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
package org.javabeanstack.sifen.core;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.Set;
import org.javabeanstack.sifen.core.exceptions.ErrorCode;
import org.javabeanstack.sifen.core.exceptions.Operation;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.internal.facade.Operations;
import org.javabeanstack.sifen.internal.util.SifenUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javabeanstack.sifen.core.validation.ConfigValidator;
import org.javabeanstack.sifen.core.validation.ValidationContext;

import static org.javabeanstack.sifen.internal.Constants.SDK_CURRENT_VERSION;

/**
 * Clase de configuración de las peticiones Sifen.<br>
 * <ul>
 *      <li>ambiente (Environment): Tipo de ambiente a utilizar (desarrollo o producción). Dependiendo de esto se
 *      definen las urls a utilizar.</li>
 *
 *      <li>urlBase (String): URL a la que se hará la petición. Hay una para cada tipo de ambiente. Se puede sobreescribir.</li>
 *
 *      <li>urlRecibe, urlRecibeLote, urlEvento, urlConsultaLote, urlConsultaRUC, urlConsulta (String):
 *      URLs para las peticiones específicas. Tienen valores por defecto (obtenidos del MT), pero pueden ser sobreescritas.</li>
 *
 *      <li>usarCertificadoCliente (Boolean): Define si se utiliza o no el certificado proporcionado para la
 *      autenticación y firma de las peticiones.
 *      Se recomienda utilizar el valor por defecto (true), pero es posible sobreescribirlo en caso de estar
 *      realizando a otro nivel la autenticación por certificado (Ej.: Forward Proxy).</li>
 *
 *      <li>certificadoCliente, contrasenaCertificadoCliente (String): Certificado a utilizar (ruta del archivo o
 *      archivo codificado en Base64), junto a la contraseña.</li>
 *
 *      <li>tipoCertificadoCliente (ClientCertificateType): Tipo de archivo del certificado. Solo PFX es soportado
 *      actualmente.</li>
 * </ul>
 *
 * <p>Delta del fork 2.0.0-SNAPSHOT (plan JBSSIF, §1.4.5; ficha 16 del informe de línea base):
 * sin cambio de código; el {@code toString()} del fork solo difiere en cómo su compilador arma la
 * concatenación.</p>
 *
 * <p>Cambio de jbs-sifen (plan JBSSIF, D13): el {@code User-Agent} pasa de
 * {@code rshk-jsifenlib/0.2.4 (LVEA)} a {@code jbs-sifen/<versión>}; no forma parte del XML.</p>
 *
 * <p>Fase 5 (plan JBSSIF, §5.2 y §5.4; M0-14, D27, D37, D41): tiempos de espera configurables
 * ({@value #HTTP_CONNECT_TIMEOUT_KEY}, {@value #HTTP_READ_TIMEOUT_KEY}, en milisegundos), proxy propio
 * de la configuración con o sin usuario ({@code sifen.proxy.host/port/user/pass}; si no se indica, rige
 * el de la JVM), reintentos de las <b>consultas</b> ({@value #MAX_RETRIES_KEY},
 * {@value #RETRY_BACKOFF_KEY}; nunca de una recepción, D41) y la traza de pedido y respuesta
 * ({@value #TRACE_ENABLED_KEY}, {@value #TRACE_FOLDER_KEY}). Las claves son las del diseño de la Fase 5
 * (D42 punto 3: son datos y no se traducen). {@link #copy()} copia todo, incluidas las listas de la
 * última carga (I10-08).</p>
 */
@SuppressWarnings("deprecation")
public class SifenConfig {
    // Enums

    /**
     * Enum con los tipos de certificados disponibles para la conexión segura y la firma digital.
     */
    public enum ClientCertificateType {PFX} // Implementar PEM y DER

    /**
     * Enum con los tipos de ambientes disponibles en Sifen.
     */
    public enum Environment {DEV, PROD}

    // Atributos
    private static final String SIFEN_AMBIENTE_KEY = "sifen.ambiente";
    private Environment ambiente;


    private static final String SIFEN_URL_BASE_KEY = "sifen.url_base";
    private String urlBase;

    private String urlBaseLocal;
    private String urlConsultaQr;

    private String pathRecibe;
    private String pathRecibeLote;
    private String pathEvento;
    private String pathConsultaLote;
    private String pathConsultaRUC;
    private String pathConsulta;

    /**
     * Clave retirada (plan JBSSIF, §6.3, D25): la NT 13 rige desde 2023 y {@code dBasExe} se escribe siempre. Si un
     * archivo de configuración la trae, se ignora con un aviso en el log al cargarlo.
     */
    private static final String SIFEN_HABILITAR_NOTA_TECNICA_13_KEY = "sifen.habilitar_nota_tecnica_13";

    /** Propiedad de la validación previa del DE y de los eventos (plan JBSSIF, §3.5, D20). */
    public static final String PRE_VALIDATION_KEY = "sifen.validacion_previa";
    /**
     * Validación previa antes de generar o enviar (D20): activa por defecto; en {@code false} la
     * librería se comporta como la línea base del fork (equivalencia, consumidores que validan por su
     * cuenta).
     */
    private boolean validacionPrevia = true;

    /** Reglas desactivadas por código, separadas por coma (D33, §3.6). */
    public static final String DISABLED_RULES_KEY = "sifen.validacion_reglas_desactivadas";
    /** Fecha de referencia de las reglas, {@code AAAA-MM-DD} (D33, §3.6). */
    public static final String RULES_DATE_KEY = "sifen.validacion_fecha_reglas";
    private Set<String> validacionReglasDesactivadas = new LinkedHashSet<>();
    private LocalDate validacionFechaReglas;

    private static final Logger LOGGER = LogManager.getLogger(SifenConfig.class);
    /** Claves {@code sifen.*} que la librería lee; otra con ese prefijo es {@code JBS-CFG-11}. */
    private static final Set<String> CLAVES_CONOCIDAS = Set.of(SIFEN_AMBIENTE_KEY, SIFEN_URL_BASE_KEY,
            SIFEN_HABILITAR_NOTA_TECNICA_13_KEY, PRE_VALIDATION_KEY, DISABLED_RULES_KEY,
            RULES_DATE_KEY, "sifen.certificado_cliente.usar", "sifen.certificado_cliente.tipo",
            "sifen.certificado_cliente.archivo", "sifen.certificado_cliente.contrasena", "sifen.csc.id", "sifen.csc",
            // Fase 5 (§5.2, §5.4)
            "sifen.http.connect_timeout", "sifen.http.read_timeout", "sifen.proxy.host", "sifen.proxy.port",
            "sifen.proxy.user", "sifen.proxy.pass", "sifen.reintentos.max", "sifen.reintentos.backoff_ms",
            "sifen.traza.activa", "sifen.traza.carpeta", "sifen.traza.retencion_dias");
    /** Propiedades con valor no admitido al cargar ({@code JBS-CFG-10}). */
    private final List<String> propiedadesInvalidas = new ArrayList<>();
    /** Claves {@code sifen.*} desconocidas al cargar ({@code JBS-CFG-11}). */
    private final List<String> propiedadesDesconocidas = new ArrayList<>();


    private static final String SIFEN_USAR_CERTIFICADO_CLIENTE_KEY = "sifen.certificado_cliente.usar";
    private boolean usarCertificadoCliente;
    private static final String SIFEN_TIPO_CERTIFICADO_CLIENTE_KEY = "sifen.certificado_cliente.tipo";
    private ClientCertificateType tipoCertificadoCliente;
    private static final String SIFEN_ARCHIVO_CERTIFICADO_CLIENTE_KEY = "sifen.certificado_cliente.archivo";
    private String certificadoCliente;
    private static final String SIFEN_PASSWORD_CERTIFICADO_CLIENTE_KEY = "sifen.certificado_cliente.contrasena";
    private String contrasenaCertificadoCliente;

    private static final String SIFEN_ID_CSC_KEY = "sifen.csc.id";
    private String idCSC;
    private static final String SIFEN_CSC_KEY = "sifen.csc";
    private String CSC;

    /** Tiempo de espera al conectar, en milisegundos (§5.2, M0-14). */
    public static final String HTTP_CONNECT_TIMEOUT_KEY = "sifen.http.connect_timeout";
    /** Tiempo de espera de la respuesta, en milisegundos (§5.2, M0-14). */
    public static final String HTTP_READ_TIMEOUT_KEY = "sifen.http.read_timeout";
    /** Tiempo de espera por omisión al conectar: 15 s. */
    public static final int DEFAULT_CONNECT_TIMEOUT_MS = 15 * 1000;
    /**
     * Tiempo de espera por omisión de la respuesta: 60 s (decisión del usuario, 2026-11-24): el MT fija en un
     * minuto el máximo de respuesta de SIFEN; con 45 s una respuesta lenta pero válida terminaba como
     * resultado desconocido.
     */
    public static final int DEFAULT_READ_TIMEOUT_MS = 60 * 1000;
    // Los nombres de estos dos campos los usan pruebas por reflexión: no se renombran.
    // M5-05: los campos de la Fase 5 se leen en cada intercambio desde el hilo del pedido y se pueden cambiar
    // desde otro (en caliente): son volatile para que el cambio se vea. Los demás se fijan al cargar.
    private volatile int httpConnectTimeout;
    private volatile int httpReadTimeout;
    private final String userAgent;

    /** Servidor proxy HTTP propio de la configuración (§5.2); sin él rige el de la JVM. */
    public static final String PROXY_HOST_KEY = "sifen.proxy.host";
    /** Puerto del proxy (obligatorio si hay servidor). */
    public static final String PROXY_PORT_KEY = "sifen.proxy.port";
    /** Usuario del proxy (opcional). */
    public static final String PROXY_USER_KEY = "sifen.proxy.user";
    /** Contraseña del proxy (obligatoria si hay usuario). */
    public static final String PROXY_PASSWORD_KEY = "sifen.proxy.pass";
    private volatile String proxyHost;
    private volatile int proxyPort;
    private volatile String proxyUser;
    private volatile String proxyPassword;

    /** Reintentos de una consulta ante una falla rápida de transporte (§5.2, D41): nunca en una recepción. */
    public static final String MAX_RETRIES_KEY = "sifen.reintentos.max";
    /** Espera antes del primer reintento, en milisegundos; se duplica en cada uno. */
    public static final String RETRY_BACKOFF_KEY = "sifen.reintentos.backoff_ms";
    /** Máximo de reintentos admitido. */
    public static final int MAX_RETRIES_LIMIT = 10;
    /** Espera máxima admitida antes del primer reintento: 60 s. */
    public static final int RETRY_BACKOFF_LIMIT_MS = 60 * 1000;
    private volatile int maxRetries;
    private volatile int retryBackoffMs;

    /** Traza completa de pedido y respuesta (D27, §5.4); también como propiedad del sistema, en caliente. */
    public static final String TRACE_ENABLED_KEY = "sifen.traza.activa";
    /** Carpeta de la traza; sin ella la traza va al log {@code org.javabeanstack.sifen.trace}. */
    public static final String TRACE_FOLDER_KEY = "sifen.traza.carpeta";
    /** Días que se conservan los archivos de la traza en su carpeta; 0 = no se borran (I5-07, M3-21). */
    public static final String TRACE_RETENTION_KEY = "sifen.traza.retencion_dias";
    /** Retención por omisión de la traza: 7 días. */
    public static final int DEFAULT_TRACE_RETENTION_DAYS = 7;
    /** Retención máxima admitida: 3650 días. */
    public static final int TRACE_RETENTION_LIMIT_DAYS = 3650;
    // I5-10 (decisión del usuario, 2026-11-24): en las tres claves de la traza gana la configuración de la
    // empresa; null = no la define y rige la propiedad del sistema (o el valor por omisión).
    private volatile Boolean traceEnabled;
    private volatile String traceFolder;
    private volatile Integer traceRetentionDays;

    // Valores Finales
    private final String URL_BASE_DEV = "https://sifen-test.set.gov.py";
    private final String URL_BASE_PROD = "https://sifen.set.gov.py";

    private final String URL_CONSULTA_QR_DEV = "https://ekuatia.set.gov.py/consultas-test/qr?";
    private final String URL_CONSULTA_QR_PROD = "https://ekuatia.set.gov.py/consultas/qr?";

    // Constructores
    public SifenConfig() {
        this.ambiente = Environment.DEV;
        this.urlBaseLocal = URL_BASE_DEV;
        this.urlConsultaQr = URL_CONSULTA_QR_DEV;

        this.pathRecibe = "/de/ws/sync/recibe.wsdl";
        this.pathRecibeLote = "/de/ws/async/recibe-lote.wsdl";
        this.pathEvento = "/de/ws/eventos/evento.wsdl";
        this.pathConsultaLote = "/de/ws/consultas/consulta-lote.wsdl";
        this.pathConsultaRUC = "/de/ws/consultas/consulta-ruc.wsdl";
        this.pathConsulta = "/de/ws/consultas/consulta.wsdl";
        this.usarCertificadoCliente = true;

        this.idCSC = "0002";
        this.CSC = "EFGH0000000000000000000000000000";

        this.httpConnectTimeout = DEFAULT_CONNECT_TIMEOUT_MS; // 15 Segundos
        this.httpReadTimeout = DEFAULT_READ_TIMEOUT_MS; // 60 Segundos
        this.userAgent = "jbs-sifen" + "/" + SDK_CURRENT_VERSION;

        this.maxRetries = 0;
        this.retryBackoffMs = 1000;
    }

    /**
     * Constructor de copia (§5.2, I10-08): campo por campo, sin pasar por los setters que derivan
     * valores ({@code setEnvironment}, {@code setCscId}); conserva también las listas de la última carga.
     */
    private SifenConfig(SifenConfig o) {
        this.ambiente = o.ambiente;
        this.urlBase = o.urlBase;
        this.urlBaseLocal = o.urlBaseLocal;
        this.urlConsultaQr = o.urlConsultaQr;
        this.pathRecibe = o.pathRecibe;
        this.pathRecibeLote = o.pathRecibeLote;
        this.pathEvento = o.pathEvento;
        this.pathConsultaLote = o.pathConsultaLote;
        this.pathConsultaRUC = o.pathConsultaRUC;
        this.pathConsulta = o.pathConsulta;
        this.validacionPrevia = o.validacionPrevia;
        this.validacionReglasDesactivadas = new LinkedHashSet<>(o.validacionReglasDesactivadas);
        this.validacionFechaReglas = o.validacionFechaReglas;
        this.propiedadesInvalidas.addAll(o.propiedadesInvalidas);
        this.propiedadesDesconocidas.addAll(o.propiedadesDesconocidas);
        this.usarCertificadoCliente = o.usarCertificadoCliente;
        this.tipoCertificadoCliente = o.tipoCertificadoCliente;
        this.certificadoCliente = o.certificadoCliente;
        this.contrasenaCertificadoCliente = o.contrasenaCertificadoCliente;
        this.idCSC = o.idCSC;
        this.CSC = o.CSC;
        this.httpConnectTimeout = o.httpConnectTimeout;
        this.httpReadTimeout = o.httpReadTimeout;
        this.userAgent = o.userAgent;
        this.proxyHost = o.proxyHost;
        this.proxyPort = o.proxyPort;
        this.proxyUser = o.proxyUser;
        this.proxyPassword = o.proxyPassword;
        this.maxRetries = o.maxRetries;
        this.retryBackoffMs = o.retryBackoffMs;
        this.traceEnabled = o.traceEnabled;
        this.traceFolder = o.traceFolder;
        this.traceRetentionDays = o.traceRetentionDays;
    }

    /**
     * Devuelve una copia independiente de la configuración (plan JBSSIF, §5.2, I10-08): todos los
     * campos, incluidas las listas de la última carga ({@code JBS-CFG-10}, {@code JBS-CFG-11}) y los de la
     * Fase 5; cambiar después el original no cambia la copia ni al revés.
     *
     * @return la copia.
     */
    public SifenConfig copy() {
        return new SifenConfig(this);
    }

    public SifenConfig(Environment tipoAmbiente, ClientCertificateType tipoCertificadoCliente, String certificadoCliente,
                       String contrasenaCertificadoCliente) {
        this();
        this.setEnvironment(tipoAmbiente);

        this.usarCertificadoCliente = true;
        this.certificadoCliente = certificadoCliente;
        this.contrasenaCertificadoCliente = contrasenaCertificadoCliente;
        this.tipoCertificadoCliente = tipoCertificadoCliente;
    }

    public SifenConfig(Environment tipoAmbiente, String idCSC, String CSC, ClientCertificateType tipoCertificadoCliente,
                       String certificadoCliente, String contrasenaCertificadoCliente) {
        this(tipoAmbiente, tipoCertificadoCliente, certificadoCliente, contrasenaCertificadoCliente);

        this.setCscId(idCSC);
        this.CSC = CSC;
    }

    /**
     * Carga la configuración de Sifen a ser utilizada desde un conjunto de propiedades.
     *
     * @param propiedades Properties que contengan las configuraciones a ser utilizadas.
     * @return La clase de configuración de Sifen con los datos obtenidos de las propiedades.
     * @throws SifenException Si los valores proveídos son incorrectos.
     */
    public static SifenConfig load(Properties propiedades) throws SifenException {
        return Operations.execute(Operation.LOAD_CONFIG, null, () -> {
            SifenConfig sifenConfig = leerPropiedades(propiedades);
            // §3.7 (D33 Parte B, momento 1): toda la configuración y el certificado; con errores lanza la
            // lista completa (SifenValidationException, código legado i101) en vez del primer problema.
            ConfigValidator.standard().require(sifenConfig);
            return sifenConfig;
        });
    }

    /**
     * Lee las propiedades sin validar (§3.7): un valor no admitido o una clave {@code sifen.*}
     * desconocida no lanza, queda registrado para {@code JBS-CFG-10} / {@code JBS-CFG-11}; una clave
     * obligatoria ausente deja el campo nulo para su regla (E3-14: la línea base daba
     * {@code NullPointerException}).
     *
     * @param propiedades propiedades; {@code null} = vacías.
     * @return la configuración leída.
     */
    static SifenConfig leerPropiedades(Properties propiedades) {
        Properties p = propiedades == null ? new Properties() : propiedades;
        SifenConfig sifenConfig = new SifenConfig();

        for (String clave : p.stringPropertyNames()) {
            if (clave.startsWith("sifen.") && !CLAVES_CONOCIDAS.contains(clave)) {
                sifenConfig.propiedadesDesconocidas.add(clave);
            }
        }

        String ambiente = valor(p, SIFEN_AMBIENTE_KEY);
        if (ambiente == null) {
            sifenConfig.ambiente = null;
        } else {
            try {
                sifenConfig.setEnvironment(Environment.valueOf(ambiente));
            } catch (IllegalArgumentException e) {
                sifenConfig.ambiente = null;
                sifenConfig.propiedadesInvalidas.add(SIFEN_AMBIENTE_KEY);
            }
        }

        if (p.containsKey(SIFEN_URL_BASE_KEY)) {
            sifenConfig.setBaseUrl(p.getProperty(SIFEN_URL_BASE_KEY));
        }

        if (p.containsKey(SIFEN_USAR_CERTIFICADO_CLIENTE_KEY)) {
            sifenConfig.setUseClientCertificate(Boolean.parseBoolean(p.getProperty(SIFEN_USAR_CERTIFICADO_CLIENTE_KEY)));
        }

        String tipo = valor(p, SIFEN_TIPO_CERTIFICADO_CLIENTE_KEY);
        if (tipo != null) {
            try {
                sifenConfig.setClientCertificateType(ClientCertificateType.valueOf(tipo));
            } catch (IllegalArgumentException e) {
                sifenConfig.propiedadesInvalidas.add(SIFEN_TIPO_CERTIFICADO_CLIENTE_KEY);
            }
        }

        sifenConfig.setClientCertificate(p.getProperty(SIFEN_ARCHIVO_CERTIFICADO_CLIENTE_KEY));
        sifenConfig.setClientCertificatePassword(p.getProperty(SIFEN_PASSWORD_CERTIFICADO_CLIENTE_KEY));

        if (p.containsKey(SIFEN_CSC_KEY)) {
            sifenConfig.setCsc(p.getProperty(SIFEN_CSC_KEY));
        }

        if (p.containsKey(SIFEN_ID_CSC_KEY)) {
            sifenConfig.setCscId(p.getProperty(SIFEN_ID_CSC_KEY));
        }

        // §6.3 (D25): la clave se ignora; un valor false cambiaba el XML (sin dBasExe), contra la NT 13
        if (p.containsKey(SIFEN_HABILITAR_NOTA_TECNICA_13_KEY)) {
            LOGGER.warn("{} = {} se ignora: la NT 13 rige desde 2023 y dBasExe se escribe siempre (D25)",
                    SIFEN_HABILITAR_NOTA_TECNICA_13_KEY, p.getProperty(SIFEN_HABILITAR_NOTA_TECNICA_13_KEY));
        }

        // §3.5 (D20): ausente = activa; solo «true» o «false», sin distinguir mayúsculas.
        String previa = valor(p, PRE_VALIDATION_KEY);
        if (previa != null) {
            if ("true".equalsIgnoreCase(previa) || "false".equalsIgnoreCase(previa)) {
                sifenConfig.validacionPrevia = Boolean.parseBoolean(previa);
            } else {
                sifenConfig.propiedadesInvalidas.add(PRE_VALIDATION_KEY);
            }
        }

        // §3.6 (D33): códigos desactivados y fecha de referencia de las reglas.
        if (p.containsKey(DISABLED_RULES_KEY)) {
            for (String codigo : p.getProperty(DISABLED_RULES_KEY).split(",")) {
                if (!codigo.isBlank()) {
                    sifenConfig.validacionReglasDesactivadas.add(codigo.trim());
                }
            }
        }
        String fecha = valor(p, RULES_DATE_KEY);
        if (fecha != null) {
            try {
                sifenConfig.validacionFechaReglas = LocalDate.parse(fecha);
                if (sifenConfig.validacionFechaReglas.isBefore(LocalDate.now(ValidationContext.SIFEN_ZONE))) {
                    // M3-23: una fecha fija no caduca; se avisa para que no quede olvidada.
                    LOGGER.warn("{} = {} es anterior a hoy: la validación aplica las reglas vigentes en esa fecha",
                            RULES_DATE_KEY, sifenConfig.validacionFechaReglas);
                }
            } catch (DateTimeParseException e) {
                sifenConfig.propiedadesInvalidas.add(RULES_DATE_KEY);
            }
        }

        leerPropiedadesConexion(p, sifenConfig);
        return sifenConfig;
    }

    /**
     * Fase 5 (§5.2, §5.4): tiempos, proxy, reintentos y traza. Un valor que no se admite queda en
     * {@code JBS-CFG-10} y el campo conserva su valor por omisión.
     */
    private static void leerPropiedadesConexion(Properties p, SifenConfig c) {
        List<String> invalidas = c.propiedadesInvalidas;
        c.httpConnectTimeout = entero(p, HTTP_CONNECT_TIMEOUT_KEY, c.httpConnectTimeout, 1, Integer.MAX_VALUE, invalidas);
        c.httpReadTimeout = entero(p, HTTP_READ_TIMEOUT_KEY, c.httpReadTimeout, 1, Integer.MAX_VALUE, invalidas);

        String host = valor(p, PROXY_HOST_KEY);
        if (host != null) {
            c.proxyHost = host;
            if (valor(p, PROXY_PORT_KEY) == null) {
                invalidas.add(PROXY_PORT_KEY);
            }
        }
        c.proxyPort = entero(p, PROXY_PORT_KEY, c.proxyPort, 1, 65535, invalidas);
        String usuario = valor(p, PROXY_USER_KEY);
        if (usuario != null) {
            c.proxyUser = usuario;
            // la contraseña no se recorta: se toma tal cual (puede tener espacios)
            c.proxyPassword = p.getProperty(PROXY_PASSWORD_KEY);
            if (c.proxyPassword == null || c.proxyPassword.isEmpty()) {
                invalidas.add(PROXY_PASSWORD_KEY);
            }
        }

        c.maxRetries = entero(p, MAX_RETRIES_KEY, c.maxRetries, 0, MAX_RETRIES_LIMIT, invalidas);
        c.retryBackoffMs = entero(p, RETRY_BACKOFF_KEY, c.retryBackoffMs, 0, RETRY_BACKOFF_LIMIT_MS, invalidas);

        String traza = valor(p, TRACE_ENABLED_KEY);
        if (traza != null) {
            if ("true".equalsIgnoreCase(traza) || "false".equalsIgnoreCase(traza)) {
                c.traceEnabled = Boolean.parseBoolean(traza);
            } else {
                invalidas.add(TRACE_ENABLED_KEY);
            }
        }
        c.traceFolder = valor(p, TRACE_FOLDER_KEY);
        if (valor(p, TRACE_RETENTION_KEY) != null) {
            int antes = c.propiedadesInvalidas.size();
            int dias = entero(p, TRACE_RETENTION_KEY, DEFAULT_TRACE_RETENTION_DAYS, 0, TRACE_RETENTION_LIMIT_DAYS, invalidas);
            // un valor no admitido es JBS-CFG-10 y la configuración queda sin definirla
            c.traceRetentionDays = invalidas.size() > antes ? null : dias;
        }
    }

    /** Entero entre {@code min} y {@code max}; ausente = {@code actual}; mal formado o fuera de rango = inválido. */
    private static int entero(Properties p, String clave, int actual, int min, int max, List<String> invalidas) {
        String v = valor(p, clave);
        if (v == null) {
            return actual;
        }
        try {
            int n = Integer.parseInt(v);
            if (n >= min && n <= max) {
                return n;
            }
        } catch (NumberFormatException e) {
            // cae al registro de la clave
        }
        if (!invalidas.contains(clave)) {
            invalidas.add(clave);
        }
        return actual;
    }

    /** Valor recortado de una propiedad; {@code null} si falta o está en blanco. */
    private static String valor(Properties p, String clave) {
        String v = p.getProperty(clave);
        return v == null || v.isBlank() ? null : v.trim();
    }

    /**
     * Carga la configuración de Sifen a ser utilizada desde un archivo de propiedades.
     *
     * @param archivo File referenciando al archivo de propiedades a ser utilizado como configuración.
     * @return La clase de configuración de Sifen con los datos obtenidos del archivo de propiedades.
     * @throws SifenException Si el archivo utilizado no existe, no pudo ser abierto o es un directorio.
     *                        Si los valores proveídos son incorrectos.
     */
    public static SifenConfig load(File archivo) throws SifenException {
        return Operations.execute(Operation.LOAD_CONFIG, null, () -> {
            // E3-29 / M3-32 (§3.8 ciclo 2): mensaje propio por causa (JBS-ENT-06) y la misma decodificación
            // que la sobrecarga con InputStream; nunca «no existe» para un archivo que existe.
            if (archivo == null) {
                throw SifenException.of(ErrorCode.JBS_ENT_06, "archivo nulo", null, "no se indicó el archivo");
            }
            if (!archivo.exists()) {
                throw SifenException.of(ErrorCode.JBS_ENT_06, "no existe: " + archivo, null, "el archivo no existe (" + archivo + ")");
            }
            if (archivo.isDirectory()) {
                throw SifenException.of(ErrorCode.JBS_ENT_06, "es un directorio: " + archivo, null, "la ruta es un directorio (" + archivo + ")");
            }
            byte[] contenido;
            try {
                contenido = Files.readAllBytes(archivo.toPath());
            } catch (IOException | RuntimeException e) {
                throw SifenException.of(ErrorCode.JBS_ENT_06, e.getClass().getName(), e,
                        "no se pudo leer el archivo (" + archivo + "): permiso o acceso");
            }
            return SifenConfig.load(propiedades(contenido));
        });
    }

    /**
     * Carga la configuración de Sifen desde un flujo de propiedades, con la misma decodificación que
     * {@link #load(File)}: UTF-8 si el contenido es UTF-8 válido, si no ISO-8859-1 (la de
     * {@code Properties.load(InputStream)} y la que aceptaba la línea base; E3-29).
     *
     * @param inputStream flujo; no se cierra.
     * @return la configuración.
     * @throws SifenException {@code JBS-ENT-06} si no se puede leer; la lista de reglas si no es válida.
     */
    public static SifenConfig load(InputStream inputStream) throws SifenException {
        return Operations.execute(Operation.LOAD_CONFIG, null, () -> {
            if (inputStream == null) {
                throw SifenException.of(ErrorCode.JBS_ENT_06, "InputStream nulo", null, "no se recibió el flujo de propiedades");
            }
            byte[] contenido;
            try {
                contenido = inputStream.readAllBytes();
            } catch (IOException | RuntimeException e) {
                throw SifenException.of(ErrorCode.JBS_ENT_06, e.getClass().getName(), e, "no se pudo leer el flujo de propiedades");
            }
            return SifenConfig.load(propiedades(contenido));
        });
    }

    /** E3-29: decodifica en UTF-8 estricto y, si no es UTF-8 válido, en ISO-8859-1 (nunca lanza por la codificación). */
    static Properties propiedades(byte[] contenido) throws SifenException {
        String texto;
        try {
            texto = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(contenido)).toString();
        } catch (java.nio.charset.CharacterCodingException e) {
            texto = new String(contenido, StandardCharsets.ISO_8859_1);
        }
        Properties props = new Properties();
        try {
            props.load(new java.io.StringReader(texto));
        } catch (IOException | IllegalArgumentException e) {
            // IllegalArgumentException: secuencia de escape Unicode mal formada
            throw SifenException.of(ErrorCode.JBS_ENT_06, e.getClass().getName(), e, "el contenido no es un archivo de propiedades válido");
        }
        return props;
    }

    /**
     * Carga la configuración de Sifen a ser utilizada desde la ruta de un archivo de propiedades.
     *
     * @param rutaArchivo Ruta del archivo de propiedades a ser utilizado como configuración.
     * @return La clase de configuración de Sifen con los datos obtenidos del archivo de propiedades.
     * @throws SifenException Si el archivo utilizado no existe, no pudo ser abierto o es un directorio.
     *                        Si los valores proveídos son incorrectos.
     */
    public static SifenConfig load(String rutaArchivo) throws SifenException {
        return Operations.execute(Operation.LOAD_CONFIG, null,
                () -> SifenConfig.load(rutaArchivo == null ? null : new File(rutaArchivo)));
    }

    /**
     * Carga la configuración de Sifen a ser utilizada desde un archivo de propiedades ubicado en la ruta Path.
     *
     * @param path Path referenciando al archivo de propiedades a ser utilizado como configuración.
     * @return La clase de configuración de Sifen con los datos obtenidos del archivo de propiedades.
     * @throws SifenException Si el archivo utilizado no existe, no pudo ser abierto o es un directorio.
     *                        Si los valores proveídos son incorrectos.
     */
    public static SifenConfig load(Path path) throws SifenException {
        return Operations.execute(Operation.LOAD_CONFIG, null,
                () -> SifenConfig.load(path == null ? null : path.toFile()));
    }

    @Override
    public String toString() {
        return "SifenConfig{" +
                "ambiente=" + ambiente +
                ", urlBase='" + urlBase + '\'' +
                ", urlBaseLocal='" + urlBaseLocal + '\'' +
                ", urlConsultaQr='" + urlConsultaQr + '\'' +
                ", pathRecibe='" + pathRecibe + '\'' +
                ", pathRecibeLote='" + pathRecibeLote + '\'' +
                ", pathEvento='" + pathEvento + '\'' +
                ", pathConsultaLote='" + pathConsultaLote + '\'' +
                ", pathConsultaRUC='" + pathConsultaRUC + '\'' +
                ", pathConsulta='" + pathConsulta + '\'' +
                ", usarCertificadoCliente=" + usarCertificadoCliente +
                ", tipoCertificadoCliente=" + tipoCertificadoCliente +
                ", certificadoCliente=" + certificadoParaMostrar() + // M3-60: el PKCS#12 en línea no se muestra
                // M1-03 (adelantado a §3.8 ciclo 2): sin la clave del PFX ni el CSC
                ", contrasenaCertificadoCliente=" + (contrasenaCertificadoCliente == null ? "null" : "'****'") +
                ", idCSC='" + idCSC + '\'' +
                ", CSC=" + (CSC == null ? "null" : "'****'") +
                ", httpConnectTimeout=" + httpConnectTimeout +
                ", httpReadTimeout=" + httpReadTimeout +
                ", proxyHost=" + (proxyHost == null ? "null" : "'" + proxyHost + "'") +
                ", proxyPort=" + proxyPort +
                ", proxyUser=" + (proxyUser == null ? "null" : "'" + proxyUser + "'") +
                ", proxyPassword=" + (proxyPassword == null ? "null" : "'****'") +
                ", maxRetries=" + maxRetries +
                ", retryBackoffMs=" + retryBackoffMs +
                ", traceEnabled=" + traceEnabled +
                ", traceFolder=" + (traceFolder == null ? "null" : "'" + traceFolder + "'") +
                ", traceRetentionDays=" + traceRetentionDays +
                ", userAgent='" + userAgent + '\'' +
                ", validacionPrevia=" + validacionPrevia +
                ", validacionReglasDesactivadas=" + validacionReglasDesactivadas +
                ", validacionFechaReglas=" + validacionFechaReglas +
                ", URL_BASE_DEV='" + URL_BASE_DEV + '\'' +
                ", URL_BASE_PROD='" + URL_BASE_PROD + '\'' +
                ", URL_CONSULTA_QR_DEV='" + URL_CONSULTA_QR_DEV + '\'' +
                ", URL_CONSULTA_QR_PROD='" + URL_CONSULTA_QR_PROD + '\'' +
                '}';
    }

    /**
     * El certificado de cliente para {@link #toString()} (M3-60): una ruta se muestra; un contenido en
     * línea (PKCS#12 en Base64, con la clave privada adentro) no, solo su largo. En la duda se oculta.
     */
    private String certificadoParaMostrar() {
        if (certificadoCliente == null) {
            return "null";
        }
        String v = certificadoCliente.trim();
        String m = v.toLowerCase(java.util.Locale.ROOT);
        boolean ruta = v.length() < 1024 && (m.endsWith(".pfx") || m.endsWith(".p12") || v.startsWith("/")
                || !v.matches("[A-Za-z0-9+/=\\s]*"));
        return ruta ? "'" + certificadoCliente + "'" : "<en línea, " + certificadoCliente.length() + " caracteres>";
    }

    // Getters y Setters
    public void setEnvironment(Environment ambiente) {
        this.ambiente = ambiente;

        if (this.ambiente == Environment.DEV) {
            this.urlBaseLocal = URL_BASE_DEV;
            this.urlConsultaQr = URL_CONSULTA_QR_DEV;
        } else if (this.ambiente == Environment.PROD) {
            this.urlBaseLocal = URL_BASE_PROD;
            this.urlConsultaQr = URL_CONSULTA_QR_PROD;
        }
    }

    public Environment getEnvironment() {
        return ambiente;
    }

    public String getBaseUrl() {
        return urlBase;
    }

    public void setBaseUrl(String urlBase) {
        this.urlBase = urlBase;
    }

    public String getLocalBaseUrl() {
        return urlBaseLocal;
    }

    public String getQRQueryUrl() {
        return urlConsultaQr;
    }

    public String getSendPath() {
        return pathRecibe;
    }

    public void setSendPath(String pathRecibe) {
        this.pathRecibe = pathRecibe;
    }

    public String getSendBatchPath() {
        return pathRecibeLote;
    }

    public void setSendBatchPath(String pathRecibeLote) {
        this.pathRecibeLote = pathRecibeLote;
    }

    public String getEventPath() {
        return pathEvento;
    }

    public void setEventPath(String pathEvento) {
        this.pathEvento = pathEvento;
    }

    public String getQueryBatchPath() {
        return pathConsultaLote;
    }

    public void setQueryBatchPath(String pathConsultaLote) {
        this.pathConsultaLote = pathConsultaLote;
    }

    public String getQueryRucPath() {
        return pathConsultaRUC;
    }

    public void setQueryRucPath(String pathConsultaRUC) {
        this.pathConsultaRUC = pathConsultaRUC;
    }

    public String getQueryDEPath() {
        return pathConsulta;
    }

    public void setQueryDEPath(String pathConsulta) {
        this.pathConsulta = pathConsulta;
    }

    public boolean isUseClientCertificate() {
        return usarCertificadoCliente;
    }

    public void setUseClientCertificate(boolean usarCertificadoCliente) {
        this.usarCertificadoCliente = usarCertificadoCliente;
    }

    public String getClientCertificate() {
        return certificadoCliente;
    }

    public void setClientCertificate(String certificadoCliente) {
        this.certificadoCliente = certificadoCliente;
    }

    public String getClientCertificatePassword() {
        return contrasenaCertificadoCliente;
    }

    public void setClientCertificatePassword(String contrasenaCertificadoCliente) {
        this.contrasenaCertificadoCliente = contrasenaCertificadoCliente;
    }

    public ClientCertificateType getClientCertificateType() {
        return tipoCertificadoCliente;
    }

    public void setClientCertificateType(ClientCertificateType tipoCertificadoCliente) {
        this.tipoCertificadoCliente = tipoCertificadoCliente;
    }

    /**
     * Tiempo de espera al conectar con SIFEN (o con el proxy).
     *
     * @return milisegundos; 15 000 por omisión.
     */
    public int getHttpConnectTimeout() {
        return httpConnectTimeout;
    }

    /**
     * Fija el tiempo de espera al conectar (§5.2, M0-14); rige desde la operación siguiente.
     *
     * @param milliseconds milisegundos, mayor que cero (cero sería esperar sin límite).
     * @throws IllegalArgumentException si no es mayor que cero.
     */
    public void setHttpConnectTimeout(int milliseconds) {
        this.httpConnectTimeout = positivo(milliseconds, "httpConnectTimeout");
    }

    /**
     * Tiempo de espera de la respuesta de SIFEN.
     *
     * @return milisegundos; 60 000 por omisión (el máximo de respuesta de SIFEN según el MT).
     */
    public int getHttpReadTimeout() {
        return httpReadTimeout;
    }

    /**
     * Fija el tiempo de espera de la respuesta (§5.2, M0-14); rige desde la operación siguiente.
     *
     * @param milliseconds milisegundos, mayor que cero.
     * @throws IllegalArgumentException si no es mayor que cero.
     */
    public void setHttpReadTimeout(int milliseconds) {
        this.httpReadTimeout = positivo(milliseconds, "httpReadTimeout");
    }

    private static int positivo(int valor, String campo) {
        if (valor <= 0) {
            throw new IllegalArgumentException(campo + " debe ser mayor que cero: " + valor);
        }
        return valor;
    }

    /**
     * Servidor proxy HTTP propio de la configuración (§5.2). Si es {@code null} se usa el que elija la
     * JVM ({@code https.proxyHost}, {@code ProxySelector}), como en la línea base.
     *
     * @return el nombre o la dirección del proxy, o {@code null}.
     */
    public String getProxyHost() {
        return proxyHost;
    }

    /**
     * Fija el servidor proxy; {@code null} o en blanco vuelve al de la JVM.
     *
     * @param host nombre o dirección.
     */
    public void setProxyHost(String host) {
        this.proxyHost = host == null || host.isBlank() ? null : host.trim();
    }

    /**
     * Puerto del proxy.
     *
     * @return el puerto; 0 si no se indicó.
     */
    public int getProxyPort() {
        return proxyPort;
    }

    /**
     * Fija el puerto del proxy.
     *
     * @param port puerto, de 1 a 65535 (0 = sin indicar).
     * @throws IllegalArgumentException fuera de 0..65535.
     */
    public void setProxyPort(int port) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("proxyPort fuera de rango: " + port);
        }
        this.proxyPort = port;
    }

    /**
     * Usuario del proxy, si exige autenticación.
     *
     * @return el usuario, o {@code null}.
     */
    public String getProxyUser() {
        return proxyUser;
    }

    /**
     * Fija el usuario del proxy.
     *
     * @param user usuario; {@code null} o en blanco = sin autenticación.
     */
    public void setProxyUser(String user) {
        this.proxyUser = user == null || user.isBlank() ? null : user.trim();
    }

    /**
     * Contraseña del proxy (no sale en {@link #toString()}).
     *
     * @return la contraseña, o {@code null}.
     */
    public String getProxyPassword() {
        return proxyPassword;
    }

    /**
     * Fija la contraseña del proxy.
     *
     * @param password contraseña.
     */
    public void setProxyPassword(String password) {
        this.proxyPassword = password;
    }

    /**
     * Reintentos de una <b>consulta</b> (RUC, DE, lote) ante una falla rápida de transporte: conexión
     * rechazada, TLS, corte o HTTP 5xx sin respuesta de SIFEN (§5.2). Una recepción nunca se reintenta
     * (D41) y un tiempo agotado tampoco (ya consumió la espera).
     *
     * @return la cantidad; 0 por omisión (sin reintentos).
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Fija los reintentos de las consultas.
     *
     * @param retries de 0 a {@value #MAX_RETRIES_LIMIT}.
     * @throws IllegalArgumentException fuera de rango.
     */
    public void setMaxRetries(int retries) {
        if (retries < 0 || retries > MAX_RETRIES_LIMIT) {
            throw new IllegalArgumentException("maxRetries fuera de rango: " + retries);
        }
        this.maxRetries = retries;
    }

    /**
     * Espera antes del primer reintento; se duplica en cada reintento siguiente.
     *
     * @return milisegundos; 1000 por omisión.
     */
    public int getRetryBackoffMs() {
        return retryBackoffMs;
    }

    /**
     * Fija la espera antes del primer reintento.
     *
     * @param milliseconds de 0 a {@value #RETRY_BACKOFF_LIMIT_MS}.
     * @throws IllegalArgumentException fuera de rango.
     */
    public void setRetryBackoffMs(int milliseconds) {
        if (milliseconds < 0 || milliseconds > RETRY_BACKOFF_LIMIT_MS) {
            throw new IllegalArgumentException("retryBackoffMs fuera de rango: " + milliseconds);
        }
        this.retryBackoffMs = milliseconds;
    }

    /**
     * Traza completa de pedido y respuesta (D27, §5.4) pedida por <b>esta</b> configuración. Se lee en cada
     * operación. Precedencia (I5-10, decisión del usuario del 2026-11-24): si la configuración la define
     * ({@link #getTraceEnabledSetting()} distinto de {@code null}), manda la configuración, también para
     * apagarla; si no la define, rige la propiedad del sistema {@value #TRACE_ENABLED_KEY}{@code =true}, que
     * sirve en caliente para un cliente que trabaja con su copia de la configuración y alcanza a todas las
     * empresas cuya configuración no la define. La traza escribe pedido y respuesta completos, con datos de
     * clientes: avisa a WARN al encenderse y dónde escribe.
     *
     * @return {@code true} si esta configuración la enciende; {@code false} si la apaga o no la define.
     */
    public boolean isTraceEnabled() {
        return Boolean.TRUE.equals(traceEnabled);
    }

    /**
     * Valor de la traza tal como lo define la configuración (I5-10).
     *
     * @return {@code true} o {@code false} si la configuración lo define; {@code null} si no (rige la
     *         propiedad del sistema).
     */
    public Boolean getTraceEnabledSetting() {
        return traceEnabled;
    }

    /**
     * Enciende o apaga la traza de esta configuración; desde entonces la configuración manda sobre la
     * propiedad del sistema (I5-10).
     *
     * @param enabled {@code true} para escribir pedido y respuesta brutos.
     */
    public void setTraceEnabled(boolean enabled) {
        this.traceEnabled = enabled;
    }

    /**
     * Carpeta donde se escribe la traza (un archivo por intercambio). Si es {@code null} se usa la
     * propiedad del sistema {@value #TRACE_FOLDER_KEY} y, si tampoco está, el log
     * {@code org.javabeanstack.sifen.trace} (I5-10: la configuración manda).
     *
     * @return la carpeta, o {@code null}.
     */
    public String getTraceFolder() {
        return traceFolder;
    }

    /**
     * Fija la carpeta de la traza.
     *
     * @param folder ruta; {@code null} o en blanco = sin carpeta.
     */
    public void setTraceFolder(String folder) {
        this.traceFolder = folder == null || folder.isBlank() ? null : folder.trim();
    }

    /**
     * Días que se conservan los archivos de la traza en su carpeta (I5-07): al escribir, la traza borra
     * los suyos más viejos (solo los que ella nombra). Si la configuración no la define, rige la
     * propiedad del sistema {@value #TRACE_RETENTION_KEY} y, si tampoco está, el valor por omisión
     * (I5-10: la configuración manda, como en la carpeta).
     *
     * @return días que define la configuración o, si no los define, {@value #DEFAULT_TRACE_RETENTION_DAYS};
     *         0 = no se borran.
     */
    public int getTraceRetentionDays() {
        Integer d = traceRetentionDays;
        return d == null ? DEFAULT_TRACE_RETENTION_DAYS : d;
    }

    /**
     * Retención de la traza tal como la define la configuración (I5-10).
     *
     * @return días, o {@code null} si la configuración no la define (rige la propiedad del sistema).
     */
    public Integer getTraceRetentionSetting() {
        return traceRetentionDays;
    }

    /**
     * Fija la retención de la traza.
     *
     * @param days de 0 a {@value #TRACE_RETENTION_LIMIT_DAYS}; 0 = no se borran.
     * @throws IllegalArgumentException fuera de rango.
     */
    public void setTraceRetentionDays(int days) {
        if (days < 0 || days > TRACE_RETENTION_LIMIT_DAYS) {
            throw new IllegalArgumentException("traceRetentionDays fuera de rango: " + days);
        }
        this.traceRetentionDays = days;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getCscId() {
        return idCSC;
    }

    public void setCscId(String idCSC) {
        this.idCSC = SifenUtil.leftPad(idCSC, '0', 4);
    }

    public String getCsc() {
        return CSC;
    }

    public void setCsc(String CSC) {
        this.CSC = CSC;
    }

    /**
     * Devuelve siempre {@code true}: la NT 13 rige desde 2023 y {@code dBasExe} se escribe siempre (plan JBSSIF,
     * §6.3, D25). Se conserva por el contrato v1.
     *
     * @return {@code true}.
     * @deprecated sin efecto desde la Fase 6 (D25); se retira en una versión mayor.
     */
    @Deprecated
    public boolean isHabilitarNotaTecnica13() {
        return true;
    }

    /**
     * No hace nada (plan JBSSIF, §6.3, D25): la NT 13 ya no se puede apagar. Quien lo llamaba con {@code false}
     * recibe ahora el XML con {@code dBasExe}, el que exige la NT 13. Se conserva por el contrato v1.
     *
     * @param habilitarNotaTecnica13 se ignora.
     * @deprecated sin efecto desde la Fase 6 (D25); se retira en una versión mayor.
     */
    @Deprecated
    public void setHabilitarNotaTecnica13(boolean habilitarNotaTecnica13) {
        // D25: sin efecto (sin aviso: SifenClientConfigCopy copia la configuración por los setters)
    }

    /**
     * Indica si {@code generarXml}, {@code recepcionDE}, {@code recepcionLoteDE} y {@code recepcionEvento}
     * validan el documento antes de armarlo (plan JBSSIF, §3.5, D20).
     *
     * @return {@code true} por defecto.
     */
    public boolean isPreValidationEnabled() {
        return validacionPrevia;
    }

    /**
     * Activa o desactiva la validación previa. Con {@code false} la librería se comporta como la línea
     * base (sin validar; un dato faltante puede terminar en {@code NullPointerException}).
     *
     * @param validacionPrevia {@code true} para validar antes de generar o enviar.
     */
    public void setPreValidationEnabled(boolean validacionPrevia) {
        this.validacionPrevia = validacionPrevia;
    }

    /**
     * Códigos de reglas desactivadas sin recompilar (D33, §3.6). Las internas {@code JBS-VAL-00/01/02}
     * no se pueden desactivar.
     *
     * <p>Las guardas anti-NPE tampoco (M3-15) y un código que no está en el catálogo da el aviso
     * {@code JBS-CFG-14} (I3-24).</p>
     *
     * @return copia inmodificable (vacía por defecto); se cambia con
     *         {@link #setDisabledRules(Set)} (I3-24).
     */
    public Set<String> getDisabledRules() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(validacionReglasDesactivadas));
    }

    /**
     * Propiedades con un valor no admitido en la última carga ({@code JBS-CFG-10}).
     *
     * @return lista inmodificable de claves (nunca los valores).
     */
    public List<String> getInvalidProperties() {
        return Collections.unmodifiableList(propiedadesInvalidas);
    }

    /**
     * Claves {@code sifen.*} que la librería no conoce, en la última carga ({@code JBS-CFG-11}).
     *
     * @return lista inmodificable de claves.
     */
    public List<String> getUnknownProperties() {
        return Collections.unmodifiableList(propiedadesDesconocidas);
    }

    /**
     * Fija los códigos de reglas desactivadas.
     *
     * @param codigos códigos; {@code null} = ninguno.
     */
    public void setDisabledRules(Set<String> codigos) {
        this.validacionReglasDesactivadas = codigos == null ? new LinkedHashSet<>() : new LinkedHashSet<>(codigos);
    }

    /**
     * Fecha de referencia de las reglas: qué reglas rigen (adelantar una NT con fecha futura o reproducir
     * lo que se rechazaba en una fecha). {@code null} = hoy (D33, §3.6).
     *
     * @return la fecha, o {@code null}.
     */
    public LocalDate getRulesDate() {
        return validacionFechaReglas;
    }

    /**
     * Fija la fecha de referencia de las reglas.
     *
     * @param fecha fecha; {@code null} = hoy.
     */
    public void setRulesDate(LocalDate fecha) {
        this.validacionFechaReglas = fecha;
    }
}
