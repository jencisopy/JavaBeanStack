/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
*        jenciso@javabeanstack.org
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 3 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
* MA 02110-1301  USA
 */
package org.javabeanstack.sifen.internal.http;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javabeanstack.sifen.core.SifenConfig;

/**
 * Traza completa de pedido y respuesta para soporte (plan JBSSIF, §5.4, D27; I5-07, M3-21).
 *
 * <p>Apagada por omisión. Se mira <b>en cada intercambio</b>, así que se enciende y se apaga sin
 * reiniciar ni recrear nada. En las tres claves (activa, carpeta y retención) <b>gana la configuración de
 * la empresa</b>; la propiedad del sistema de la misma clave solo rige cuando la configuración no la
 * define (I5-10, decisión del usuario del 2026-11-24). Dos vías:</p>
 * <ul>
 *   <li>{@code SifenConfig.setTraceEnabled(true)} (o {@value SifenConfig#TRACE_ENABLED_KEY}{@code =true}
 *       en las propiedades): rige solo para esa configuración, es decir, para <b>una empresa</b>;</li>
 *   <li>la propiedad del sistema {@value SifenConfig#TRACE_ENABLED_KEY}{@code =true}: rige para
 *       <b>todas</b> las configuraciones de la JVM que no la definen, y es la que llega en caliente a un
 *       {@code SifenClient}, que trabaja con su copia de la configuración. El aviso de encendido lo dice.</li>
 * </ul>
 * <p>Lo que escribe son los bytes que salieron y los que llegaron, con datos de clientes (nombre, RUC,
 * dirección, importes; el CDC va en el nombre del archivo). Por eso:</p>
 * <ul>
 *   <li>al encenderse (y cada vez que cambia la vía o el destino) deja una línea WARN con la vía, el
 *       destino y la retención; una por conexión, sin estado estático ({@link State});</li>
 *   <li>en la carpeta ({@value SifenConfig#TRACE_FOLDER_KEY} de la configuración o de la propiedad del
 *       sistema) crea la carpeta {@code rwx------} y cada archivo {@code rw-------} donde el sistema de
 *       archivos es POSIX; una carpeta que ya existía no se toca, pero si la pueden leer otros usuarios
 *       se avisa;</li>
 *   <li>borra sus propios archivos más viejos que la retención ({@value SifenConfig#TRACE_RETENTION_KEY},
 *       7 días por omisión; 0 = no borrar), a lo sumo una vez por hora por conexión.</li>
 * </ul>
 * <p>Nombres: {@code <AAAAMMDD-HHMMSS.mmm>_<nanoTime en hexadecimal>_<id>_<CDC|RUC|lote>_pedido.xml},
 * {@code ..._respuesta.xml} y, si no hubo respuesta interpretable, {@code ..._error.txt}. Sin carpeta va
 * al log {@value #TRACE_LOGGER}: es la única salida del XML completo fuera de DEBUG (D27), y la enciende
 * el operador a propósito. Una falla al escribir la traza nunca corta la operación. La credencial del
 * proxy no llega a la traza: no se escriben cabeceras.</p>
 */
public final class ExchangeTrace {
    /** Nombre del log de la traza cuando no hay carpeta. */
    public static final String TRACE_LOGGER = "org.javabeanstack.sifen.trace";
    private static final Logger TRACE = LogManager.getLogger(TRACE_LOGGER);
    private static final Logger LOGGER = LogManager.getLogger(ExchangeTrace.class);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss.SSS");
    private static final Pattern CDC = Pattern.compile("(?:<(?:\\w+:)?dCDC>|Id=\")(\\d{44})");
    private static final Pattern REFERENCIA = Pattern.compile("<(?:\\w+:)?(dRUCCons|dProtConsLote)>([0-9]{1,20})<");
    /** Archivos que escribe la traza (los únicos que borra la retención). */
    private static final Pattern PROPIO = Pattern.compile("\\d{8}-\\d{6}\\.\\d{3}_[0-9a-f]+_[^_]+_[^_]+_(pedido\\.xml|respuesta\\.xml|error\\.txt)");
    private static final Set<PosixFilePermission> CARPETA = java.util.Collections.unmodifiableSet(PosixFilePermissions.fromString("rwx------"));
    private static final Set<PosixFilePermission> ARCHIVO = java.util.Collections.unmodifiableSet(PosixFilePermissions.fromString("rw-------"));
    /** Cada cuánto se revisa la retención, por conexión. */
    static final Duration CLEANUP_INTERVAL = Duration.ofHours(1);

    /**
     * Estado de la traza de una conexión: el último aviso de encendido y la última limpieza. Lo guarda la
     * conexión (una por cliente), así que la traza no tiene estado estático mutable (I10-14).
     */
    public static final class State {
        private final AtomicReference<String> announced = new AtomicReference<>();
        private final AtomicLong lastCleanupNanos = new AtomicLong(Long.MIN_VALUE);
    }

    private ExchangeTrace() {
    }

    /**
     * Indica si la traza está encendida para esta configuración.
     *
     * @param config configuración; puede ser {@code null}.
     * @return el valor de la configuración si lo define (también {@code false}); si no, el de la propiedad
     *         del sistema (I5-10).
     */
    public static boolean isActive(SifenConfig config) {
        Boolean propia = config == null ? null : config.getTraceEnabledSetting();
        return propia != null ? propia : bySystemProperty();
    }

    private static boolean bySystemProperty() {
        return Boolean.parseBoolean(System.getProperty(SifenConfig.TRACE_ENABLED_KEY));
    }

    /**
     * Escribe un intercambio, si la traza está encendida.
     *
     * @param config     configuración.
     * @param state      estado de la conexión que intercambió.
     * @param url        URL del servicio.
     * @param request    bytes enviados (o que se iban a enviar).
     * @param response   bytes de la respuesta; {@code null} o vacío si no la hubo.
     * @param httpStatus estado HTTP; 0 si no lo hubo.
     * @param errorCode  código de la falla; {@code null} si no hubo.
     */
    public static void record(SifenConfig config, State state, String url, byte[] request, byte[] response,
            int httpStatus, String errorCode) {
        if (!isActive(config)) {
            state.announced.set(null); // apagada: el próximo encendido vuelve a avisar
            return;
        }
        try {
            String id = OperationContext.label(null);
            String pedido = request == null ? "" : new String(request, StandardCharsets.UTF_8);
            String referencia = referencia(pedido);
            String carpeta = folder(config);
            int retencion = retentionDays(config);
            String cabecera = "url=" + url + " http=" + httpStatus + (errorCode == null ? "" : " error=" + errorCode);
            announce(config, state, carpeta, retencion);
            boolean hayRespuesta = response != null && response.length > 0;
            if (carpeta == null) {
                TRACE.info("[{}] {} {} pedido:\n{}", id, referencia, cabecera, pedido);
                if (hayRespuesta) {
                    TRACE.info("[{}] {} respuesta:\n{}", id, referencia, new String(response, StandardCharsets.UTF_8));
                }
                return;
            }
            Path dir = Path.of(carpeta);
            boolean posix = createFolder(dir);
            // §10.3 (carril F, I10-14): sin secuencia estática; dos intercambios del mismo milisegundo no se
            // pisan por System.nanoTime() más el id de operación (uno por operación del cliente).
            String base = LocalDateTime.now().format(STAMP) + "_" + Long.toHexString(System.nanoTime()) + "_" + id + "_" + referencia;
            write(dir.resolve(base + "_pedido.xml"), request == null ? new byte[0] : request, posix);
            if (hayRespuesta) {
                write(dir.resolve(base + "_respuesta.xml"), response, posix);
            }
            if (errorCode != null || !hayRespuesta) {
                write(dir.resolve(base + "_error.txt"), (cabecera + System.lineSeparator()).getBytes(StandardCharsets.UTF_8), posix);
            }
            cleanup(dir, retencion, state);
        } catch (IOException | RuntimeException e) {
            // la traza es de soporte: su falla no corta la operación ni lleva el mensaje (puede traer la ruta)
            LOGGER.warn("No se pudo escribir la traza de SIFEN ({})", e.getClass().getName());
        }
    }

    /** Carpeta de la configuración o, si no tiene, la de la propiedad del sistema; {@code null} = al log. */
    static String folder(SifenConfig config) {
        String c = config != null && config.getTraceFolder() != null ? config.getTraceFolder()
                : System.getProperty(SifenConfig.TRACE_FOLDER_KEY);
        return c == null || c.isBlank() ? null : c.trim();
    }

    /**
     * Retención (I5-10): la de la configuración si la define; si no, la propiedad del sistema si es válida;
     * si no, el valor por omisión.
     */
    static int retentionDays(SifenConfig config) {
        Integer propia = config == null ? null : config.getTraceRetentionSetting();
        if (propia != null) {
            return propia;
        }
        String v = System.getProperty(SifenConfig.TRACE_RETENTION_KEY);
        if (v != null) {
            try {
                int n = Integer.parseInt(v.trim());
                if (n >= 0 && n <= SifenConfig.TRACE_RETENTION_LIMIT_DAYS) {
                    return n;
                }
            } catch (NumberFormatException e) {
                // cae al valor por omisión
            }
        }
        return SifenConfig.DEFAULT_TRACE_RETENTION_DAYS;
    }

    /** Una línea WARN al encenderse o al cambiar vía, destino o retención. */
    private static void announce(SifenConfig config, State state, String carpeta, int retencion) {
        boolean global = config == null || config.getTraceEnabledSetting() == null;
        String clave = (global ? "sistema" : "configuracion") + "|" + carpeta + "|" + retencion;
        String previa = state.announced.getAndSet(clave);
        if (clave.equals(previa)) {
            return;
        }
        String via = global ? "por la propiedad del sistema " + SifenConfig.TRACE_ENABLED_KEY
                + " (alcanza a todas las empresas de la JVM cuya configuración no la define)" : "por la configuración de esta empresa";
        String destino = carpeta == null ? "el log " + TRACE_LOGGER : "la carpeta " + carpeta;
        LOGGER.warn("Traza de SIFEN encendida {}: escribe pedido y respuesta completos, con datos de clientes, en {}{}{}",
                via, destino, carpeta == null ? "" : retencion == 0 ? " (sin borrado automático)"
                : " (se borran los archivos de más de " + retencion + " días)", carpeta == null ? "" : abierta(Path.of(carpeta)));
    }

    /** Aviso si la carpeta ya existe y la pueden leer otros usuarios (no se le cambian los permisos). */
    private static String abierta(Path dir) {
        try {
            if (Files.isDirectory(dir) && dir.getFileSystem().supportedFileAttributeViews().contains("posix")) {
                Set<PosixFilePermission> p = Files.getPosixFilePermissions(dir);
                if (p.contains(PosixFilePermission.GROUP_READ) || p.contains(PosixFilePermission.OTHERS_READ)
                        || p.contains(PosixFilePermission.GROUP_EXECUTE) || p.contains(PosixFilePermission.OTHERS_EXECUTE)) {
                    return "; ATENCIÓN: la carpeta ya existía y la pueden leer otros usuarios ("
                            + PosixFilePermissions.toString(p) + "), conviene rwx------";
                }
            }
        } catch (IOException | RuntimeException e) {
            return "";
        }
        return "";
    }

    /** Crea la carpeta en 700 si es POSIX; si ya existía y la leen otros, avisa. Devuelve si es POSIX. */
    private static boolean createFolder(Path dir) throws IOException {
        boolean posix = dir.getFileSystem().supportedFileAttributeViews().contains("posix");
        if (Files.isDirectory(dir)) {
            return posix; // una carpeta que ya existía no se toca (el aviso de encendido dice si es abierta)
        }
        if (posix) {
            Path padre = dir.toAbsolutePath().getParent();
            if (padre != null) {
                Files.createDirectories(padre);
            }
            try {
                Files.createDirectory(dir, PosixFilePermissions.asFileAttribute(CARPETA));
            } catch (FileAlreadyExistsException e) {
                // otro hilo la creó
            }
            // umask puede quitar bits pero no agregarlos: se fija igual por si el sistema los ignoró
            Files.setPosixFilePermissions(dir, CARPETA);
        } else {
            Files.createDirectories(dir);
        }
        return posix;
    }

    /** Escribe un archivo nuevo en 600 (POSIX). */
    private static void write(Path file, byte[] data, boolean posix) throws IOException {
        Set<StandardOpenOption> opciones = EnumSet.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        FileAttribute<?>[] attrs = posix ? new FileAttribute<?>[] {PosixFilePermissions.asFileAttribute(ARCHIVO)}
                : new FileAttribute<?>[0];
        try (SeekableByteChannel ch = Files.newByteChannel(file, opciones, attrs);
                OutputStream out = Channels.newOutputStream(ch)) {
            out.write(data);
        }
    }

    /** Borra los archivos propios más viejos que la retención, a lo sumo una vez por intervalo. */
    private static void cleanup(Path dir, int dias, State state) {
        if (dias <= 0) {
            return;
        }
        long ahora = System.nanoTime();
        long ultima = state.lastCleanupNanos.get();
        if (ultima != Long.MIN_VALUE && ahora - ultima < CLEANUP_INTERVAL.toNanos()) {
            return;
        }
        if (!state.lastCleanupNanos.compareAndSet(ultima, ahora)) {
            return; // otro hilo de la misma conexión está limpiando
        }
        long limite = System.currentTimeMillis() - Duration.ofDays(dias).toMillis();
        int borrados = 0;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path f : ds) {
                if (PROPIO.matcher(f.getFileName().toString()).matches() && Files.isRegularFile(f)
                        && Files.getLastModifiedTime(f).toMillis() < limite) {
                    Files.deleteIfExists(f);
                    borrados++;
                }
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("No se pudo aplicar la retención de la traza de SIFEN ({})", e.getClass().getName());
            return;
        }
        if (borrados > 0) {
            LOGGER.info("Traza de SIFEN: {} archivos de más de {} días borrados", borrados, dias);
        }
    }

    /** CDC del pedido, o el RUC o el lote consultado; {@code sin-referencia} si no hay. */
    static String referencia(String request) {
        if (request == null) {
            return "sin-referencia";
        }
        Matcher m = CDC.matcher(request);
        if (m.find()) {
            return m.group(1);
        }
        Matcher r = REFERENCIA.matcher(request);
        return r.find() ? r.group(2) : "sin-referencia";
    }
}
