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
package org.javabeanstack.sifen.test;

import org.javabeanstack.sifen.core.SifenConfig;
import org.junit.jupiter.api.Assumptions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Configuración de los tests que pegan al ambiente de pruebas de SIFEN (plan JBSSIF, §1.8 y §2.7,
 * D16, D28).
 *
 * <p>Lee el archivo {@code KEY=VALOR} indicado por la propiedad del sistema {@value #PROPIEDAD} o,
 * si no está, por la variable de entorno {@value #VARIABLE} (formato de
 * {@code .claude/tools/sifen-credentials.env}; plantilla sin secretos en
 * {@code sifen-credentials.env.example}). Claves:</p>
 * <ul>
 *     <li>{@code SIFEN_AMBIENTE}: solo {@code DEV} (o ausente, que vale {@code DEV}).</li>
 *     <li>{@code SIFEN_CERT_PATH}, {@code SIFEN_CERT_PASS}, {@code SIFEN_CSC_ID}, {@code SIFEN_CSC}:
 *     obligatorias para cualquier test en vivo.</li>
 *     <li>{@code SIFEN_EMISOR_RUC}, {@code SIFEN_EMISOR_DV}, {@code SIFEN_TIMBRADO},
 *     {@code SIFEN_TIMBRADO_FECHA_INICIO} ({@code AAAA-MM-DD}), {@code SIFEN_ESTABLECIMIENTO},
 *     {@code SIFEN_PUNTO_EXPEDICION}: datos del emisor de prueba; solo los exigen los tests que
 *     emiten documentos ({@link #emisorOSaltar()}).</li>
 *     <li>{@code SIFEN_EMISOR_EMAIL}: correo del emisor ({@code dEmailE}, obligatorio en el DE; I2-08).
 *     Opcional para la configuración y para {@link #emisorOSaltar()} (la consulta del RUC no lo
 *     necesita), <b>obligatorio para reemitir</b>: sin él {@code ReemisionPrueba} deja el test
 *     saltado, y también si no cumple el patrón {@code tEmail} del XSD
 *     ({@link #emailParaReemitirOSaltar(Emisor)}; el mensaje nombra la clave, nunca el valor).</li>
 *     <li>{@code SIFEN_NUMERO_INICIAL} (opcional, por omisión {@value #NUMERO_INICIAL_MINIMO}): primer
 *     {@code dNumDoc} que pueden usar los DE enviados en vivo. Nunca menor que
 *     {@value #NUMERO_INICIAL_MINIMO}: los tests de {@code jbs-sifen} emiten en el timbrado de prueba,
 *     punto 001-001, desde el 0001000 (decisión del usuario ESC-8, 2026-11-24; Maker sigue su propia
 *     numeración desde el 62); un valor menor corta la corrida.</li>
 * </ul>
 *
 * <p>Reglas:</p>
 * <ul>
 *     <li><b>D28 — nunca producción.</b> Si {@code SIFEN_AMBIENTE} tiene un valor distinto de
 *     {@code DEV} (incluido {@code dev} o {@code PROD}) la corrida <b>falla</b> con
 *     {@link IllegalStateException}, antes de mirar el certificado o el CSC: una configuración
 *     equivocada no puede pasar como «test saltado». La configuración devuelta fija siempre
 *     {@link SifenConfig.Environment#DEV}.</li>
 *     <li><b>Reemisión distinta de producción</b> ({@link #verificarReemisionDePrueba}): un DE
 *     reemitido con el timbrado o el establecimiento-punto de su original, o con un timbrado que no
 *     sea {@code SIFEN_TIMBRADO}, también corta la corrida, antes de reservar el número.</li>
 *     <li>Si falta el archivo, el certificado o el CSC, el test queda <em>saltado</em>
 *     ({@link Assumptions}), nunca fallado. La asunción se evalúa desde {@code @BeforeEach}
 *     (I1-05).</li>
 *     <li><b>M1-03 — sin secretos en el log.</b> Esta clase no escribe nada al log y ningún mensaje
 *     de asunción o de excepción incluye la clave del certificado ni el CSC. Nunca concatenar la
 *     {@link SifenConfig} devuelta en un mensaje: su {@code toString()} (port literal, D10) incluye
 *     la clave y el CSC.</li>
 * </ul>
 *
 * <p>Los tests en vivo llevan {@code @Tag("sifen-live")} y el build por omisión los excluye
 * ({@code excludedGroups} en {@code sifen/pom.xml}); se corren solo con el perfil
 * {@code sifen-live}: {@code mvn -pl sifen -am test -Psifen-live -Djbs.sifen.test.env=<ruta>}.</p>
 */
public final class SifenTestEnv {
    /** Propiedad del sistema con la ruta del archivo de configuración. */
    public static final String PROPIEDAD = "jbs.sifen.test.env";
    /** Variable de entorno alternativa a {@link #PROPIEDAD}. */
    public static final String VARIABLE = "JBS_SIFEN_TEST_ENV";
    /** Único valor admitido de {@code SIFEN_AMBIENTE} (D28). */
    public static final String AMBIENTE_PERMITIDO = "DEV";
    /** Número de documento mínimo de los DE enviados en vivo (ver {@code SIFEN_NUMERO_INICIAL}). */
    public static final int NUMERO_INICIAL_MINIMO = 1000;
    /** Clave del correo del emisor ({@code dEmailE}; I2-08). */
    public static final String CLAVE_EMAIL = "SIFEN_EMISOR_EMAIL";
    /** Patrón {@code tEmail} de {@code DE_Types_v150.xsd} (los patrones XSD van anclados: se usa {@code matches()}). */
    private static final Pattern EMAIL = Pattern.compile(
            "([0-9a-zA-Z#$%]([-.\\w]*[0-9a-zA-Z#$%'\\.\\-_])*@([0-9a-zA-Z][-\\w]*[0-9a-zA-Z]\\.)+[a-zA-Z]{2,9})");

    private SifenTestEnv() {
    }

    /**
     * Devuelve la configuración de SIFEN para los tests en vivo o deja el test saltado si no está
     * disponible.
     *
     * @return configuración lista para {@code Sifen.setSifenConfig}, siempre en ambiente DEV.
     * @throws IllegalStateException si {@code SIFEN_AMBIENTE} no es {@code DEV} (D28).
     */
    public static SifenConfig configuracionOSaltar() {
        return entornoOSaltar().getConfiguracion();
    }

    /**
     * Devuelve los datos del emisor de prueba (RUC, timbrado, establecimiento, punto) o deja el test
     * saltado si falta alguno.
     *
     * @return datos del emisor del archivo de configuración.
     */
    public static Emisor emisorOSaltar() {
        Entorno entorno = entornoOSaltar();
        Assumptions.assumeTrue(entorno.getEmisor() != null,
                "Faltan los datos del emisor de prueba (SIFEN_EMISOR_RUC, SIFEN_EMISOR_DV, SIFEN_TIMBRADO, "
                        + "SIFEN_TIMBRADO_FECHA_INICIO, SIFEN_ESTABLECIMIENTO, SIFEN_PUNTO_EXPEDICION)");
        return entorno.getEmisor();
    }

    /**
     * Correo del emisor para completar {@code dEmailE} al reemitir (I2-08), o deja el test saltado si
     * {@value #CLAVE_EMAIL} falta o no cumple el patrón {@code tEmail} del XSD (un DE así lo rechaza
     * SIFEN y quema un número). Los mensajes nombran la clave, nunca el valor.
     *
     * @param emisor datos del emisor ya cargados.
     * @return correo configurado.
     */
    public static String emailParaReemitirOSaltar(Emisor emisor) {
        String email = emisor.getEmail();
        Assumptions.assumeTrue(email != null,
                "Falta " + CLAVE_EMAIL + " (correo del emisor, dEmailE): los tests que reemiten DE quedan saltados");
        Assumptions.assumeTrue(EMAIL.matcher(email).matches(),
                CLAVE_EMAIL + " no cumple el patrón tEmail del XSD: los tests que reemiten DE quedan saltados");
        return email;
    }

    /**
     * Guarda de la reemisión (refuerza D28; pedido del usuario, 2026-11-23: «enviar como 001-001 y no
     * como 001-002»): ningún DE reemitido para pruebas puede parecerse al de producción del que sale.
     * <b>Corta la corrida</b> ({@link IllegalStateException}, no salta) si:
     * <ol>
     *     <li>el timbrado de la reemisión no es el {@code SIFEN_TIMBRADO} del archivo de configuración;</li>
     *     <li>el timbrado de la reemisión es el del DE original (el de producción);</li>
     *     <li>el establecimiento-punto de la reemisión es el del DE original (hoy 001-002 en
     *     producción), o el original no trae establecimiento o punto con que comparar.</li>
     * </ol>
     * Los mensajes nombran la regla y las claves, nunca valores, CDC ni datos de la venta.
     *
     * @param estOriginal {@code dEst} del DE original.
     * @param puntoOriginal {@code dPunExp} del DE original.
     * @param timbradoOriginal {@code dNumTim} del DE original.
     * @param estReemision {@code dEst} de la reemisión.
     * @param puntoReemision {@code dPunExp} de la reemisión.
     * @param timbradoReemision {@code dNumTim} de la reemisión.
     * @param timbradoEnv {@code SIFEN_TIMBRADO} del archivo de configuración.
     * @throws IllegalStateException si la reemisión viola alguna de las tres reglas.
     */
    public static void verificarReemisionDePrueba(String estOriginal, String puntoOriginal, int timbradoOriginal,
                                                  String estReemision, String puntoReemision, int timbradoReemision,
                                                  int timbradoEnv) {
        if (timbradoReemision != timbradoEnv) {
            throw new IllegalStateException("El timbrado del DE reemitido no es el SIFEN_TIMBRADO del archivo de configuración: "
                    + "los DE de prueba solo llevan el timbrado de prueba (D28)");
        }
        if (timbradoReemision == timbradoOriginal) {
            throw new IllegalStateException("SIFEN_TIMBRADO es el mismo timbrado del DE original (el de producción): "
                    + "ningún DE reemitido puede parecerse al de producción (D28)");
        }
        if (estOriginal == null || puntoOriginal == null) {
            throw new IllegalStateException("El DE original no trae establecimiento o punto de expedición: "
                    + "no se puede comprobar que la reemisión no use el de producción (D28)");
        }
        if (mismoCodigo(estOriginal, estReemision) && mismoCodigo(puntoOriginal, puntoReemision)) {
            throw new IllegalStateException("SIFEN_ESTABLECIMIENTO-SIFEN_PUNTO_EXPEDICION es el mismo establecimiento-punto "
                    + "del DE original (el de producción): los DE de prueba se envían con otro (001-001), "
                    + "ninguno puede parecerse al de producción (D28)");
        }
    }

    /** Compara dos códigos de establecimiento o punto sin depender de los ceros a la izquierda. */
    private static boolean mismoCodigo(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        String x = a.trim();
        String y = b.trim();
        try {
            return Integer.parseInt(x) == Integer.parseInt(y);
        } catch (NumberFormatException e) {
            return x.equals(y);
        }
    }

    /**
     * Resuelve el archivo de {@link #PROPIEDAD} / {@link #VARIABLE} y lo carga con
     * {@link #cargar(Path)}; sin ruta configurada deja el test saltado.
     *
     * @return entorno cargado.
     */
    public static Entorno entornoOSaltar() {
        String ruta = System.getProperty(PROPIEDAD);
        if (ruta == null || ruta.isEmpty()) {
            ruta = System.getenv(VARIABLE);
        }
        Assumptions.assumeTrue(ruta != null && !ruta.isEmpty(),
                "Sin configuración de SIFEN (" + PROPIEDAD + " / " + VARIABLE + "): test en vivo saltado");
        return cargar(Paths.get(ruta));
    }

    /**
     * Carga un archivo de configuración. Separado de {@link #entornoOSaltar()} para poder probar la
     * guarda D28 con un archivo temporal sin tocar las propiedades del sistema (que comparten todos
     * los tests de la JVM de surefire).
     *
     * @param archivo archivo {@code KEY=VALOR}.
     * @return entorno cargado.
     * @throws IllegalStateException si {@code SIFEN_AMBIENTE} no es {@code DEV} (D28).
     */
    public static Entorno cargar(Path archivo) {
        Assumptions.assumeTrue(Files.isRegularFile(archivo), "No existe el archivo de configuración " + archivo);

        Properties valores = new Properties();
        try (InputStream entrada = Files.newInputStream(archivo)) {
            valores.load(entrada);
        } catch (IOException e) {
            Assumptions.abort("No se pudo leer " + archivo + ": " + e.getClass().getSimpleName());
        }

        // D28: los tests nunca pegan al ambiente de producción. No se salta: se corta la corrida, para
        // que una configuración equivocada no pase inadvertida. Va antes de cualquier asunción.
        String ambiente = valor(valores, "SIFEN_AMBIENTE");
        if (ambiente != null && !AMBIENTE_PERMITIDO.equals(ambiente)) {
            throw new IllegalStateException("SIFEN_AMBIENTE=" + ambiente + " en " + archivo
                    + ": los tests de jbs-sifen solo corren contra el ambiente de pruebas (DEV)");
        }

        String certificado = valor(valores, "SIFEN_CERT_PATH");
        String contrasena = valor(valores, "SIFEN_CERT_PASS");
        String idCsc = valor(valores, "SIFEN_CSC_ID");
        String csc = valor(valores, "SIFEN_CSC");
        Assumptions.assumeTrue(certificado != null && Files.isRegularFile(Paths.get(certificado)),
                "Falta el certificado (SIFEN_CERT_PATH)");
        Assumptions.assumeTrue(contrasena != null && idCsc != null && csc != null,
                "Faltan SIFEN_CERT_PASS, SIFEN_CSC_ID o SIFEN_CSC");

        SifenConfig config = new SifenConfig();
        config.setEnvironment(SifenConfig.Environment.DEV);
        config.setUseClientCertificate(true);
        config.setClientCertificateType(SifenConfig.ClientCertificateType.PFX);
        config.setClientCertificate(certificado);
        config.setClientCertificatePassword(contrasena);
        config.setCscId(idCsc);
        config.setCsc(csc);
        // El fork exige NT 13 activa con ítems gravados (E0-16).
        config.setHabilitarNotaTecnica13(true);

        int numeroInicial = NUMERO_INICIAL_MINIMO;
        String inicial = valor(valores, "SIFEN_NUMERO_INICIAL");
        if (inicial != null) {
            try {
                numeroInicial = Integer.parseInt(inicial);
            } catch (NumberFormatException e) {
                throw new IllegalStateException("SIFEN_NUMERO_INICIAL no es un número en " + archivo);
            }
            if (numeroInicial < NUMERO_INICIAL_MINIMO) {
                throw new IllegalStateException("SIFEN_NUMERO_INICIAL=" + numeroInicial + " en " + archivo
                        + ": los DE de prueba usan dNumDoc >= " + NUMERO_INICIAL_MINIMO);
            }
        }
        return new Entorno(config, leerEmisor(valores), numeroInicial);
    }

    private static Emisor leerEmisor(Properties valores) {
        String ruc = valor(valores, "SIFEN_EMISOR_RUC");
        String dv = valor(valores, "SIFEN_EMISOR_DV");
        String timbrado = valor(valores, "SIFEN_TIMBRADO");
        String inicio = valor(valores, "SIFEN_TIMBRADO_FECHA_INICIO");
        String establecimiento = valor(valores, "SIFEN_ESTABLECIMIENTO");
        String punto = valor(valores, "SIFEN_PUNTO_EXPEDICION");
        // Opcional: no entra en la condición de abajo, para que la consulta del RUC no se salte; lo
        // exige emailParaReemitirOSaltar (I2-08).
        String email = valor(valores, CLAVE_EMAIL);
        if (ruc == null || dv == null || timbrado == null || inicio == null || establecimiento == null || punto == null) {
            return null;
        }
        try {
            return new Emisor(ruc, dv, Integer.parseInt(timbrado), LocalDate.parse(inicio), establecimiento, punto, email);
        } catch (NumberFormatException | DateTimeParseException e) {
            // Sin el valor en el mensaje: no es secreto, pero el archivo lo es.
            Assumptions.abort("SIFEN_TIMBRADO o SIFEN_TIMBRADO_FECHA_INICIO con formato inválido");
            return null;
        }
    }

    private static String valor(Properties valores, String clave) {
        String v = valores.getProperty(clave);
        if (v == null) {
            return null;
        }
        v = v.trim();
        if (v.length() >= 2 && (v.startsWith("\"") && v.endsWith("\"") || v.startsWith("'") && v.endsWith("'"))) {
            v = v.substring(1, v.length() - 1);
        }
        return v.isEmpty() ? null : v;
    }

    /**
     * Resultado de la carga: configuración y, si están completos, los datos del emisor.
     */
    public static final class Entorno {
        private final SifenConfig configuracion;
        private final Emisor emisor;
        private final int numeroInicial;

        private Entorno(SifenConfig configuracion, Emisor emisor, int numeroInicial) {
            this.configuracion = configuracion;
            this.emisor = emisor;
            this.numeroInicial = numeroInicial;
        }

        /**
         * @return primer {@code dNumDoc} admitido para los DE enviados en vivo (≥ {@value SifenTestEnv#NUMERO_INICIAL_MINIMO}).
         */
        public int getNumeroInicial() {
            return numeroInicial;
        }

        public SifenConfig getConfiguracion() {
            return configuracion;
        }

        /**
         * @return datos del emisor, o {@code null} si el archivo no los trae completos.
         */
        public Emisor getEmisor() {
            return emisor;
        }
    }

    /**
     * Datos del emisor y del timbrado de prueba (no son secretos; el archivo que los trae sí).
     */
    public static final class Emisor {
        private final String ruc;
        private final String dv;
        private final int timbrado;
        private final LocalDate inicioTimbrado;
        private final String establecimiento;
        private final String puntoExpedicion;
        private final String email;

        Emisor(String ruc, String dv, int timbrado, LocalDate inicioTimbrado, String establecimiento, String puntoExpedicion,
               String email) {
            this.ruc = ruc;
            this.dv = dv;
            this.timbrado = timbrado;
            this.inicioTimbrado = inicioTimbrado;
            this.establecimiento = establecimiento;
            this.puntoExpedicion = puntoExpedicion;
            this.email = email;
        }

        public String getRuc() {
            return ruc;
        }

        public String getDv() {
            return dv;
        }

        public int getTimbrado() {
            return timbrado;
        }

        public LocalDate getInicioTimbrado() {
            return inicioTimbrado;
        }

        public String getEstablecimiento() {
            return establecimiento;
        }

        public String getPuntoExpedicion() {
            return puntoExpedicion;
        }

        /**
         * @return correo del emisor ({@code SIFEN_EMISOR_EMAIL}), o {@code null} si el archivo no lo trae.
         */
        public String getEmail() {
            return email;
        }
    }
}
