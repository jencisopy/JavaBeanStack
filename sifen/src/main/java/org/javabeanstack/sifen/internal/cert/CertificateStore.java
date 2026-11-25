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
package org.javabeanstack.sifen.internal.cert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.validation.support.LoadedCertificate;
import org.javabeanstack.sifen.core.validation.support.LoadedCertificate.Cause;

/**
 * Almacén de los certificados cargados (plan JBSSIF, §3.7, D33 Parte B; M0-13, E3-15).
 *
 * <p>Carga el PKCS#12 de la configuración <b>una vez</b> y lo guarda por clave (ruta absoluta, o
 * resumen del contenido en línea, más un resumen de la contraseña: admite varias empresas, RNF8). En
 * cada pedido compara la <b>huella</b> del archivo (existencia, tamaño y fecha de modificación): si
 * cambió o se borró, lo recarga y el resultado nuevo lo dice ({@code JBS-CERT-15}) o trae la causa
 * ({@code JBS-CERT-01} si se borró). La carga nunca lanza: devuelve un {@link LoadedCertificate} con
 * la causa, que leen las reglas {@code JBS-CERT-*}; quien firma o arma el TLS pide el
 * {@link Material} y recibe la excepción con el mensaje de la regla.</p>
 *
 * <p>Seguridad: no registra la contraseña, el contenido ni los mensajes de las excepciones del JDK
 * (pueden traer datos del almacén); los flujos se cierran siempre (E3-15).</p>
 *
 * <p>Fase 5 (plan JBSSIF, §5.2; M3-64, M0-04): el almacén es una <b>instancia</b>. Cada conexión de
 * transporte ({@code internal.http.SifenConnection}) tiene la suya, que olvida ante una falla de
 * transporte sin tocar la de otro cliente. Los métodos estáticos ({@link #get}, {@link #material},
 * {@link #invalidate}, {@link #clear}) siguen trabajando sobre un almacén compartido ({@link #shared()}),
 * que usan la validación de la configuración y la firma: esos llamadores no son del carril de
 * conexión y pasan a una instancia por cliente con la fachada (F, I5-02).</p>
 */
public final class CertificateStore {
    private static final Logger LOGGER = LogManager.getLogger(CertificateStore.class);
    /** Debajo de este tamaño un valor decodificable como Base64 se toma como ruta (un PKCS#12 real es mayor). */
    private static final int MINIMO_EN_LINEA = 100;
    /** Almacén compartido de los métodos estáticos (validación y firma). */
    private static final CertificateStore SHARED = new CertificateStore();

    private final Map<String, Entrada> entradas = new ConcurrentHashMap<>();

    /**
     * Material de la clave para firmar y para el TLS: solo dentro de la librería.
     *
     * @param info        metadatos del certificado.
     * @param keyStore    el almacén cargado.
     * @param alias       la entrada que se usa.
     * @param privateKey       su clave privada.
     * @param certificate su certificado.
     */
    public record Material(LoadedCertificate info, KeyStore keyStore, String alias, PrivateKey privateKey,
            X509Certificate certificate) {
        @Override
        public String toString() {
            return "Material{" + info + '}';
        }
    }

    private record Entrada(String huella, LoadedCertificate info, Material material) {
    }

    /**
     * Crea un almacén vacío, propio de quien lo crea (una conexión de transporte, §5.2).
     */
    public CertificateStore() {
    }

    /**
     * Devuelve el almacén compartido que usan los métodos estáticos.
     *
     * @return el almacén compartido.
     */
    public static CertificateStore shared() {
        return SHARED;
    }

    /**
     * Devuelve el certificado de la configuración, cargándolo si hace falta o si el archivo cambió.
     *
     * @param cfg configuración; con {@code usarCertificadoCliente = false} igual se intenta (la firma lo
     *            necesita).
     * @return el resultado de la carga; nunca {@code null} ni lanza.
     */
    public static LoadedCertificate get(SifenConfig cfg) {
        return SHARED.loaded(cfg);
    }

    /**
     * Devuelve el material para firmar o armar el TLS.
     *
     * @param cfg configuración.
     * @return el material, o {@code null} si el certificado no se pudo cargar (la causa está en
     *         {@link #get(SifenConfig)}).
     */
    public static Material material(SifenConfig cfg) {
        return SHARED.materialOf(cfg);
    }

    /**
     * Olvida la carga del certificado de la configuración: el próximo pedido lo relee.
     *
     * @param cfg configuración; {@code null} no hace nada.
     */
    public static void invalidate(SifenConfig cfg) {
        SHARED.forget(cfg);
    }

    /** Olvida todas las cargas del almacén compartido (pruebas). */
    public static void clear() {
        SHARED.forgetAll();
    }

    /**
     * Devuelve el certificado de la configuración en este almacén (como {@link #get(SifenConfig)}).
     *
     * @param cfg configuración.
     * @return el resultado de la carga; nunca {@code null} ni lanza.
     */
    public LoadedCertificate loaded(SifenConfig cfg) {
        return entrada(cfg).info();
    }

    /**
     * Devuelve el material de la configuración en este almacén (como {@link #material(SifenConfig)}).
     *
     * @param cfg configuración.
     * @return el material, o {@code null} si el certificado no se pudo cargar.
     */
    public Material materialOf(SifenConfig cfg) {
        return entrada(cfg).material();
    }

    /**
     * Olvida en este almacén la carga del certificado de la configuración.
     *
     * @param cfg configuración; {@code null} no hace nada.
     */
    public void forget(SifenConfig cfg) {
        if (cfg != null) {
            entradas.remove(clave(cfg));
        }
    }

    /** Olvida todas las cargas de este almacén. */
    public void forgetAll() {
        entradas.clear();
    }

    private Entrada entrada(SifenConfig cfg) {
        String clave = clave(cfg);
        String huella = huella(cfg);
        Entrada actual = entradas.get(clave);
        if (actual != null && actual.huella().equals(huella)) {
            return actual;
        }
        return entradas.compute(clave, (k, previa) -> {
            if (previa != null && previa.huella().equals(huella)) {
                return previa;
            }
            boolean cambio = previa != null && previa.info().isLoaded();
            Entrada nueva = cargar(cfg, huella, cambio);
            if (cambio) {
                LOGGER.info("El certificado de cliente cambió desde la última carga; se recargó ({})", nueva.info().getCause());
            }
            return nueva;
        });
    }

    private static Entrada cargar(SifenConfig cfg, String huella, boolean cambio) {
        String valor = cfg.getClientCertificate();
        if (cfg.getClientCertificateType() != SifenConfig.ClientCertificateType.PFX) {
            return fallida(huella, Cause.UNSUPPORTED_TYPE, null, false);
        }
        if (valor == null || valor.isBlank()) {
            return fallida(huella, Cause.MISSING, null, false);
        }
        Path ruta = ruta(valor);
        byte[] contenido = null;
        String rutaTexto = null;
        if (ruta != null && Files.exists(ruta)) {
            rutaTexto = ruta.toString();
            if (Files.isDirectory(ruta) || !Files.isReadable(ruta)) {
                return fallida(huella, Cause.UNREADABLE, rutaTexto, false);
            }
        } else {
            // Una ruta inexistente nunca se toma como contenido en línea: el decodificador MIME ignora los
            // caracteres ajenos al alfabeto y una ruta larga «decodificaría» (el .pfx borrado es JBS-CERT-01).
            contenido = pareceRuta(valor) ? null : enLinea(valor);
            if (contenido == null) {
                return ruta != null && pareceRuta(valor) ? fallida(huella, Cause.NOT_FOUND, ruta.toString(), false)
                        : fallida(huella, Cause.INVALID_BASE64, null, true);
            }
        }
        String contrasena = cfg.getClientCertificatePassword();
        if (contrasena == null || contrasena.isBlank()) {
            return fallida(huella, Cause.NO_PASSWORD, rutaTexto, contenido != null);
        }
        char[] clave = contrasena.toCharArray();
        KeyStore ks;
        try (InputStream in = contenido != null ? new ByteArrayInputStream(contenido) : Files.newInputStream(ruta)) {
            ks = KeyStore.getInstance("PKCS12");
            ks.load(in, clave);
        } catch (IOException e) {
            Cause causa = contrasenaIncorrecta(e) ? Cause.WRONG_PASSWORD
                    : contenido == null && !Files.isReadable(ruta) ? Cause.UNREADABLE : Cause.NOT_PKCS12;
            return fallida(huella, causa, rutaTexto, contenido != null);
        } catch (GeneralSecurityException | RuntimeException e) {
            return fallida(huella, Cause.NOT_PKCS12, rutaTexto, contenido != null);
        }
        try {
            List<String> alias = Collections.list(ks.aliases());
            for (String a : alias) {
                if (!ks.isKeyEntry(a)) {
                    continue;
                }
                Key k = ks.getKey(a, clave);
                Certificate c = ks.getCertificate(a);
                if (k instanceof PrivateKey privada && c instanceof X509Certificate x509) {
                    String documento = null;
                    String titular = null;
                    for (CertificateSubjects.CertificateSubject s : CertificateSubjects.of(x509)) {
                        if (documento == null) {
                            documento = CertificateSubjects.normalizeDocumentNumber(s.documentNumber());
                        }
                        if (titular == null) {
                            titular = s.name();
                        }
                    }
                    LoadedCertificate info = LoadedCertificate.loaded(rutaTexto, contenido != null, alias.size(), x509,
                            documento, titular, cambio);
                    return new Entrada(huella, info, new Material(info, ks, a, privada, x509));
                }
            }
            return fallida(huella, Cause.NO_PRIVATE_KEY, rutaTexto, contenido != null);
        } catch (UnrecoverableKeyException e) {
            return fallida(huella, Cause.WRONG_PASSWORD, rutaTexto, contenido != null);
        } catch (GeneralSecurityException e) {
            return fallida(huella, Cause.NO_PRIVATE_KEY, rutaTexto, contenido != null);
        }
    }

    private static Entrada fallida(String huella, Cause causa, String ruta, boolean enLinea) {
        LOGGER.debug("No se pudo cargar el certificado de cliente: {}", causa);
        return new Entrada(huella, LoadedCertificate.failed(causa, ruta, enLinea), null);
    }

    /** El JDK informa la contraseña incorrecta de un PKCS#12 como IOException con causa UnrecoverableKeyException. */
    private static boolean contrasenaIncorrecta(IOException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof UnrecoverableKeyException) {
                return true;
            }
        }
        String m = e.getMessage();
        return m != null && m.toLowerCase().contains("password");
    }

    private static Path ruta(String valor) {
        try {
            return Path.of(valor.trim()).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            return null;
        }
    }

    /** Extensión de PKCS#12 o algún carácter que no puede estar en un Base64 (separador, punto, dos puntos…). */
    private static boolean pareceRuta(String valor) {
        String v = valor.trim().toLowerCase();
        return v.endsWith(".pfx") || v.endsWith(".p12") || v.contains("\\") || !v.matches("[a-z0-9+/=\\s]*")
                || (v.startsWith("/") && v.length() < 1024);
    }

    private static byte[] enLinea(String valor) {
        try {
            byte[] b = Base64.getMimeDecoder().decode(valor.trim());
            return b.length < MINIMO_EN_LINEA ? null : b;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Clave de la entrada: ruta absoluta o resumen del contenido, tipo y resumen de la contraseña. */
    private static String clave(SifenConfig cfg) {
        String valor = cfg.getClientCertificate();
        String origen;
        if (valor == null) {
            origen = "-";
        } else {
            Path p = ruta(valor);
            origen = p != null && (valor.length() < 1024) ? "archivo:" + p : "en-linea:" + resumen(valor);
        }
        return origen + "|" + cfg.getClientCertificateType() + "|" + resumen(cfg.getClientCertificatePassword());
    }

    /** Huella del origen: existencia, tamaño y fecha de modificación del archivo; constante en línea. */
    private static String huella(SifenConfig cfg) {
        String valor = cfg.getClientCertificate();
        if (valor == null || valor.isBlank()) {
            return "sin-valor";
        }
        Path p = ruta(valor);
        if (p == null) {
            return "en-linea";
        }
        try {
            BasicFileAttributes a = Files.readAttributes(p, BasicFileAttributes.class);
            return "archivo:" + a.size() + ":" + a.lastModifiedTime().toMillis() + ":" + a.isDirectory()
                    + ":" + Files.isReadable(p);
        } catch (IOException | SecurityException e) {
            return "no-existe";
        }
    }

    private static String resumen(String texto) {
        if (texto == null) {
            return "-";
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
