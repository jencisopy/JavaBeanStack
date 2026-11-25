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
package org.javabeanstack.sifen.test.vivo;

import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.fields.request.de.TgCamDEAsoc;
import org.javabeanstack.sifen.core.fields.request.de.TgEmis;
import org.javabeanstack.sifen.core.fields.request.de.TgTimb;
import org.javabeanstack.sifen.core.types.TiTipDocAso;
import org.javabeanstack.sifen.test.SifenTestEnv;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Reemisión de un DE para el ambiente de <b>pruebas</b> de SIFEN (plan JBSSIF, §2.9): toma un DE
 * leído de una venta real (extraída fuera del módulo, RNF3) y le pone el emisor, el timbrado, el
 * establecimiento, el punto y el correo ({@code dEmailE}, I2-08) de prueba de {@link SifenTestEnv}, un número de documento nuevo, fecha
 * actual y código de seguridad nuevo; el CDC, la firma y el QR los recalcula la librería al generar.
 *
 * <p>La numeración vive en una <b>tabla de equivalencias</b> CSV persistente (fuera del repo): cada
 * número se <b>reserva</b> con una fila {@code RESERVADO} en el momento de reemitir, antes de
 * cualquier llamada a SIFEN, y el resultado se agrega después en otra fila (también si la llamada
 * lanza, con la clase de la excepción). Así un número enviado nunca se reutiliza, ni si la corrida
 * se corta ni ante un timeout con resultado desconocido (M0-12). Cada corrida sigue desde el número
 * más alto registrado + 1 y nunca baja de {@code SIFEN_NUMERO_INICIAL} (≥ 1000, ESC-8). Las notas de
 * crédito/débito con documento asociado electrónico apuntan al CDC <b>reemitido</b> de su factura
 * (buscado en la tabla por el CDC original).</p>
 *
 * <p><b>Guarda D28 de la reemisión</b> (pedido del usuario, 2026-11-23: «enviar como 001-001 y no como
 * 001-002»): si la reemisión llevaría el timbrado o el establecimiento-punto del DE original, o un
 * timbrado que no sea {@code SIFEN_TIMBRADO}, la corrida se corta ({@link IllegalStateException})
 * antes de consumir y reservar el número ({@link SifenTestEnv#verificarReemisionDePrueba}).</p>
 *
 * <p>No abre conexiones: el envío lo hace {@link SifenVivoTest}. La prueba sin red está en
 * {@code ReemisionPruebaTest}.</p>
 */
public final class ReemisionPrueba {
    /**
     * Razón social que la DNIT exige al emisor en el ambiente de pruebas: literal del MT v150, regla
     * 1263. La Guía de Pruebas 2026-02 trae otro («DOCUMENTO ELECTRÓNICO SIN VALOR COMERCIAL NI FISCAL -
     * GENERADO EN AMBIENTE DE PRUEBA»); se queda el del MT y, si SIFEN-test responde 1263, se cambia (I2-11).
     */
    public static final String RAZON_SOCIAL_PRUEBA = "DE generado en ambiente de prueba - sin valor comercial ni fiscal";
    /** Cabecera de la tabla de equivalencias. */
    public static final String CABECERA = "iditemmovimiento;numero_original;numero_enviado;cdc_original;cdc_enviado;fecha_envio;operacion;codigo_estado;dCodRes;dEstRes;dMsgRes";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final SifenTestEnv.Emisor emisor;
    private final String email;
    private final Path tabla;
    private final Map<String, String> cdcPorOriginal = new LinkedHashMap<>();
    private int siguiente;
    private final SecureRandom azar = new SecureRandom();

    /**
     * @param emisor datos del emisor y timbrado de prueba.
     * @param numeroInicial número mínimo ({@code SIFEN_NUMERO_INICIAL}, ≥ 1000, ESC-8).
     * @param tabla CSV de equivalencias (se crea si no existe).
     * @throws IOException si la tabla no se puede leer.
     * @throws org.opentest4j.TestAbortedException si falta {@code SIFEN_EMISOR_EMAIL} o no cumple el
     *         patrón del XSD (I2-08): se comprueba acá, antes de leer la tabla o reservar un número.
     */
    public ReemisionPrueba(SifenTestEnv.Emisor emisor, int numeroInicial, Path tabla) throws IOException {
        this.email = SifenTestEnv.emailParaReemitirOSaltar(emisor);
        this.emisor = emisor;
        this.tabla = tabla;
        int maximo = numeroInicial - 1;
        if (Files.exists(tabla)) {
            for (String linea : Files.readAllLines(tabla, StandardCharsets.UTF_8)) {
                if (linea.isEmpty() || linea.startsWith("iditemmovimiento")) {
                    continue;
                }
                String[] c = linea.split(";", -1);
                maximo = Math.max(maximo, numero(c[2]));
                if (!c[3].isEmpty() && !c[4].isEmpty()) {
                    cdcPorOriginal.put(c[3], c[4]);
                }
            }
        }
        this.siguiente = Math.max(numeroInicial, maximo + 1);
    }

    private static int numero(String establecimientoPuntoNumero) {
        String[] p = establecimientoPuntoNumero.split("-");
        return Integer.parseInt(p[p.length - 1]);
    }

    /** @return número que usará la próxima reemisión. */
    public int getSiguiente() {
        return siguiente;
    }

    /**
     * CDC reemitido que corresponde a un CDC original (de esta corrida o de una anterior).
     *
     * @param cdcOriginal CDC de producción.
     * @return CDC enviado a pruebas, o {@code null}.
     */
    public String cdcReemitido(String cdcOriginal) {
        return cdcPorOriginal.get(cdcOriginal);
    }

    /**
     * Comprueba, sin modificar el DE, que su reemisión no se parecería al original de producción:
     * timbrado y establecimiento-punto del emisor de prueba distintos de los del DE, y timbrado igual a
     * {@code SIFEN_TIMBRADO} (D28; {@link SifenTestEnv#verificarReemisionDePrueba}). {@code SifenVivoTest}
     * la corre sobre todas las ventas antes de la primera llamada a SIFEN; {@link #reemitir} la repite.
     *
     * @param de DE leído de la venta (con la numeración original).
     * @throws IllegalStateException si la reemisión violaría la regla.
     */
    public void verificarAntesDeReemitir(DocumentoElectronico de) {
        TgTimb t = de.getgTimb();
        SifenTestEnv.verificarReemisionDePrueba(t.getdEst(), t.getdPunExp(), t.getdNumTim(),
                emisor.getEstablecimiento(), emisor.getPuntoExpedicion(), emisor.getTimbrado(), emisor.getTimbrado());
    }

    /**
     * CDC reemitidos de los DE electrónicos que asocia el DE (notas de crédito/débito), sin modificarlo.
     * Una posición {@code null} es un asociado que no fue reemitido en pruebas.
     *
     * @param de DE leído de la venta.
     * @return lista vacía si no asocia DE electrónicos.
     */
    public List<String> cdcAsociadosReemitidos(DocumentoElectronico de) {
        List<String> cdcs = new ArrayList<>();
        if (de.getgCamDEAsocList() != null) {
            for (TgCamDEAsoc asoc : de.getgCamDEAsocList()) {
                if (asoc.getiTipDocAso() == TiTipDocAso.ELECTRONICO) {
                    cdcs.add(cdcPorOriginal.get(asoc.getdCdCDERef()));
                }
            }
        }
        return cdcs;
    }

    /**
     * M2-05: decide, antes de reservar número, si una nota se puede enviar. Solo se envía si cada DE
     * electrónico que asocia fue reemitido y quedó <b>aprobado</b> en pruebas; si no, SIFEN la
     * rechazaría y se quemaría un número.
     *
     * @param de nota leída de la venta.
     * @param aprobado dice si un CDC reemitido quedó aprobado (en vivo: resultado del lote o
     *        {@code consultaDE}).
     * @return {@code null} si se puede enviar; si no, el motivo (sin CDC ni datos de la venta).
     */
    public String motivoParaNoEnviarNota(DocumentoElectronico de, Predicate<String> aprobado) {
        for (String cdc : cdcAsociadosReemitidos(de)) {
            if (cdc == null) {
                return "la factura asociada no fue reemitida en pruebas";
            }
            if (!aprobado.test(cdc)) {
                return "la factura asociada reemitida no quedó aprobada en pruebas";
            }
        }
        return null;
    }

    /**
     * Reemite el DE en el lugar y reserva su número (fila {@code RESERVADO} en la tabla).
     *
     * @param idVenta {@code iditemmovimiento} de la venta.
     * @param de DE leído de la venta (con la numeración original).
     * @param fecha fecha de emisión y de firma.
     * @return datos de la reemisión.
     * @throws SifenException si no se puede calcular el CDC.
     * @throws IOException si no se puede escribir la reserva.
     * @throws IllegalStateException si el DE asocia un DE electrónico que no fue reemitido, o si la
     *         reemisión usaría el timbrado o el establecimiento-punto del original, o un timbrado que no
     *         es {@code SIFEN_TIMBRADO} (D28, {@link SifenTestEnv#verificarReemisionDePrueba}): en ambos
     *         casos sin consumir ni reservar número.
     */
    public Reemision reemitir(String idVenta, DocumentoElectronico de, LocalDateTime fecha) throws SifenException, IOException {
        TgTimb t = de.getgTimb();
        // Guarda D28 (001-001 y timbrado de prueba, nunca los de producción): antes de tocar el DE.
        verificarAntesDeReemitir(de);
        String estOriginal = t.getdEst();
        String puntoOriginal = t.getdPunExp();
        int timbradoOriginal = t.getdNumTim();
        String original = t.getdEst() + "-" + t.getdPunExp() + "-" + t.getdNumDoc();
        String cdcOriginal = de.getId();

        if (de.getgCamDEAsocList() != null) {
            for (TgCamDEAsoc asoc : de.getgCamDEAsocList()) {
                if (asoc.getiTipDocAso() == TiTipDocAso.ELECTRONICO) {
                    String nuevo = cdcPorOriginal.get(asoc.getdCdCDERef());
                    if (nuevo == null) {
                        throw new IllegalStateException("El DE " + original + " asocia el CDC " + asoc.getdCdCDERef()
                                + ", que no fue reemitido en pruebas: enviar antes su factura");
                    }
                    asoc.setdCdCDERef(nuevo);
                }
            }
        }

        int numero = siguiente;
        t.setdNumTim(emisor.getTimbrado());
        t.setdFeIniT(emisor.getInicioTimbrado());
        t.setdEst(emisor.getEstablecimiento());
        t.setdPunExp(emisor.getPuntoExpedicion());
        t.setdNumDoc(String.format("%07d", numero));
        // Misma guarda sobre lo que quedó en el DE, antes de consumir el número y de reservarlo.
        SifenTestEnv.verificarReemisionDePrueba(estOriginal, puntoOriginal, timbradoOriginal,
                t.getdEst(), t.getdPunExp(), t.getdNumTim(), emisor.getTimbrado());
        siguiente++;
        TgEmis e = de.getgDatGralOpe().getgEmis();
        e.setdRucEm(emisor.getRuc());
        e.setdDVEmi(emisor.getDv());
        e.setdNomEmi(RAZON_SOCIAL_PRUEBA);
        // I2-08: la base no trae el correo del emisor; se pisa siempre con el del .env.
        e.setdEmailE(email);
        de.setdFecFirma(fecha);
        de.getgDatGralOpe().setdFeEmiDE(fecha);
        de.getgOpeDE().setdCodSeg(String.format("%09d", 1 + azar.nextInt(999_999_998)));
        String cdc = de.obtenerCDC();
        if (cdcOriginal != null) {
            cdcPorOriginal.put(cdcOriginal, cdc);
        }
        Reemision r = new Reemision(original, emisor.getEstablecimiento() + "-" + emisor.getPuntoExpedicion() + "-" + t.getdNumDoc(),
                cdcOriginal, cdc, fecha);
        registrar(idVenta, r, "RESERVADO", 0, null, null, null);
        return r;
    }

    /**
     * Agrega una fila de resultado a la tabla de equivalencias (antes de mirar el resultado: queda
     * aunque la aserción falle). La reserva ya la escribió {@link #reemitir}.
     *
     * @param idVenta {@code iditemmovimiento} de la venta.
     * @param r reemisión.
     * @param operacion {@code recepcionDE} o {@code recepcionLoteDE:<lote>}.
     * @param codigoEstado estado HTTP.
     * @param codRes código de resultado de SIFEN.
     * @param estRes estado del DE.
     * @param msgRes mensaje.
     * @throws IOException si no se puede escribir.
     */
    public void registrar(String idVenta, Reemision r, String operacion, int codigoEstado, String codRes, String estRes, String msgRes) throws IOException {
        List<String> lineas = new ArrayList<>();
        if (!Files.exists(tabla)) {
            Files.createDirectories(tabla.toAbsolutePath().getParent());
            lineas.add(CABECERA);
        }
        lineas.add(String.join(";", idVenta, r.original, r.enviado, n(r.cdcOriginal), r.cdc, r.fecha.format(FECHA), operacion,
                String.valueOf(codigoEstado), n(codRes), n(estRes), n(msgRes).replace(';', ',').replace('\n', ' ')));
        Files.write(tabla, lineas, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static String n(String s) {
        return s == null ? "" : s;
    }

    /** Resultado de una reemisión. */
    public static final class Reemision {
        public final String original;
        public final String enviado;
        public final String cdcOriginal;
        public final String cdc;
        public final LocalDateTime fecha;

        Reemision(String original, String enviado, String cdcOriginal, String cdc, LocalDateTime fecha) {
            this.original = original;
            this.enviado = enviado;
            this.cdcOriginal = cdcOriginal;
            this.cdc = cdc;
            this.fecha = fecha;
        }
    }
}
