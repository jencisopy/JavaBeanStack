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

import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.exceptions.ErrorCode;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.validation.ValidationMessages;
import org.javabeanstack.sifen.internal.SOAPResponse;
import org.javabeanstack.sifen.internal.facade.ExceptionTranslator;
import org.javabeanstack.sifen.internal.helpers.SoapHelper;
import org.javabeanstack.sifen.internal.util.SifenUtil;

/**
 * {@link SifenConnection} sobre {@code HttpURLConnection}/{@code HttpsURLConnection} del JDK (plan
 * JBSSIF, §5.2, D37): el transporte con el que SIFEN-test aprobó los envíos del plan, ahora por
 * instancia.
 *
 * <p>Por pedido: abre la conexión con {@link #open} —el mismo armado que usa el diagnóstico de
 * {@link ConnectionProbe}, para que no puedan divergir (E5-04)—: el proxy de la configuración si hay
 * ({@code openConnection(Proxy)}; si no, el que elija la JVM), el {@code SSLSocketFactory} de su
 * {@link TlsContext}, los tiempos de espera y, si el proxy pide usuario, un {@code Authenticator} propio
 * de la conexión (no el global de la JVM). Ante una falla de entrada/salida descarta el contexto TLS: el
 * pedido siguiente arma uno nuevo, y como el socket factory cambia, la JVM tampoco reutiliza un socket
 * viejo de su caché keep-alive.</p>
 *
 * <p>El cuerpo se serializa antes y se envía con {@code setFixedLengthStreamingMode} (E5-01): sin eso
 * {@code HttpURLConnection} <b>reenvía por su cuenta</b> un POST cuando el servidor cierra la conexión
 * sin responder ({@code sun.net.http.retryPost}, {@code true} por omisión), un reintento oculto de la
 * recepción que contradice D41. En la red sale lo mismo (el JDK ya mandaba {@code Content-Length}). Con
 * ese modo el JDK tampoco sigue una redirección: un 3xx sale como {@code JBS-CONEX-10} (antes se seguía como
 * {@code GET} y la página del portal daba el mismo código). Los mismos bytes van a la traza
 * ({@link ExchangeTrace}).</p>
 *
 * <p>Proxy efectivo (E5-03): si la configuración no declara proxy, la falla al conectar se compara con
 * el que elegiría el {@code ProxySelector} de la JVM ({@code https.proxyHost}, {@code net.properties}): si
 * hay uno, la falla es {@code JBS-CONEX-02} y el error nombra ese proxy, en vez de culpar a SIFEN. Con el
 * proxy de la JVM el JDK intenta conectar <b>dos veces</b> con el último proxy de la lista: el tiempo real
 * es 2 × {@code connect_timeout} (los 30 s del informe del 2026-11-19, dato para §5.1); con el de la
 * configuración hay un solo intento.</p>
 *
 * <p>Usuario del proxy: con un destino {@code http://} se manda {@code Proxy-Authorization: Basic}
 * de entrada (el modo de envío de largo fijo no admite responder a un 407); con un destino
 * {@code https://} lo maneja el {@code Authenticator} en el {@code CONNECT}, que entrega la credencial
 * <b>una sola vez</b> por intercambio (E5-02: el JDK la vuelve a pedir ante cada 407 y repetía el
 * {@code CONNECT} hasta 20 veces, lo que bloquea la cuenta en un proxy atado al directorio). Límite
 * conocido del JDK (I5-04, M5-01): con un destino {@code https://} la credencial viaja en el
 * {@code CONNECT} solo si el esquema no está en {@code jdk.http.auth.tunneling.disabledSchemes}, que por
 * omisión ({@code conf/net.properties}) deshabilita {@code Basic}; la propiedad se lee al arrancar la JVM.
 * En los dos casos la falla sale como {@code JBS-CONEX-02} y el detalle técnico dice si el proxy rechazó
 * la credencial o si el JDK no la envió.</p>
 */
final class UrlSifenConnection implements SifenConnection {
    private static final Logger LOGGER = LogManager.getLogger(UrlSifenConnection.class);

    private final TlsContext tls = new TlsContext();
    private final ExchangeTrace.State trace = new ExchangeTrace.State();

    UrlSifenConnection() {
    }

    /** Proxy por el que sale el pedido: el de la configuración, el de la JVM o ninguno (directo). */
    record Route(Proxy configured, Proxy effective, String text, boolean jvm) {
        boolean proxied() {
            return effective != null;
        }

        /** Para el log y el detalle técnico. */
        String describe() {
            return text == null ? null : text + (jvm ? " (de la JVM)" : "");
        }
    }

    @Override
    public SOAPResponse exchange(SifenConfig config, String urlString, SOAPMessage soapMessage) throws SifenException {
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw SifenException.ofRule("JBS-CFG-03", ValidationMessages.message("JBS-CFG-03"),
                    "El URL " + urlString + " es inválido: " + e.getLocalizedMessage(), e);
        }
        String protocolo = url.getProtocol();
        if (!"https".equalsIgnoreCase(protocolo) && !"http".equalsIgnoreCase(protocolo)) {
            throw SifenException.ofRule("JBS-CFG-04", ValidationMessages.message("JBS-CFG-04"),
                    "El protocolo " + protocolo + " es inválido", null);
        }
        byte[] bytes;
        try {
            ByteArrayOutputStream cuerpo = new ByteArrayOutputStream();
            soapMessage.writeTo(cuerpo);
            bytes = cuerpo.toByteArray();
        } catch (SOAPException | IOException e) {
            throw SifenException.of(ErrorCode.JBS_INT_01, "Excepción de mensajería SOAP: " + e.getLocalizedMessage(), e);
        }
        SOAPResponse soapResponse = new SOAPResponse();
        try {
            exchange(config, url, bytes, soapResponse);
            ExchangeTrace.record(config, trace, urlString, bytes, soapResponse.getRawData(), soapResponse.getStatus(), null);
            return soapResponse;
        } catch (SifenException e) {
            ExchangeTrace.record(config, trace, urlString, bytes, soapResponse.getRawData(), soapResponse.getStatus(), e.getCode());
            throw e;
        }
    }

    private void exchange(SifenConfig config, URL url, byte[] bytes, SOAPResponse soapResponse) throws SifenException {
        String id = OperationContext.label(null);
        Route route = route(config, url);
        ProxyAuthenticator auth = ProxyAuthenticator.of(config, route.configured());
        HttpURLConnection connection = null;
        TlsContext.Holder tlsUsado = null;
        boolean conectado = false;
        boolean enviado = false;
        try {
            if ("https".equalsIgnoreCase(url.getProtocol())) {
                tlsUsado = tls.get(config);
            }
            connection = open(config, url, route.configured(), tlsUsado == null ? null : tlsUsado.context(), auth);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            // E5-01: sin reenvío oculto del POST por parte del JDK
            connection.setFixedLengthStreamingMode(bytes.length);
            connection.setRequestProperty("Content-Type", "application/xml; charset=utf-8");

            // Conexión
            if (route.proxied()) {
                LOGGER.info("[{}] Conectando a: {} por el proxy {}", id, url, route.describe());
            } else {
                LOGGER.info("[{}] Conectando a: {}", id, url);
            }
            connection.connect();
            conectado = true;

            // Petición
            LOGGER.info("[{}] Enviando mensaje SOAP", id);
            // I3-48 (§3.8 ciclo 2): desde acá toda falla de una recepción deja el resultado desconocido. Con el
            // envío de largo fijo los bytes salen al escribir (no en getResponseCode): se marca antes.
            enviado = true;
            try (OutputStream out = connection.getOutputStream()) {
                out.write(bytes);
            }

            // Respuesta
            int estado = connection.getResponseCode();
            soapResponse.setStatus(estado);
            if (estado == HttpURLConnection.HTTP_PROXY_AUTH) {
                // §5.4: el proxy (el de la configuración o el de la JVM) pide credenciales o las rechazó;
                // el pedido no pasó del proxy, así que el resultado no es desconocido
                enviado = false;
                String quien = route.text() == null ? "de la JVM" : route.describe();
                // §10.7 (I5-09): con credencial configurada, el rechazo tiene su código (JBS-CONEX-11)
                throw SifenException.of(auth == null ? ErrorCode.JBS_CONEX_02 : ErrorCode.JBS_CONEX_11,
                        "HTTP 407 del proxy " + quien + credentialNote(auth, url), null, quien);
            }
            if (estado >= 300 && estado < 400) {
                // E5-01: la línea base seguía la redirección como GET y terminaba en el portal (JBS-CONEX-10); con
                // el envío de largo fijo llega el 3xx tal cual: SIFEN no redirige, es un proxy o un portal cautivo
                throw SifenException.of(ErrorCode.JBS_CONEX_10, "HTTP " + estado + " hacia "
                        + connection.getHeaderField("Location"), null, estado);
            }
            InputStream inputStream = soapResponse.isRequestSuccessful() ? connection.getInputStream()
                    : connection.getErrorStream();
            // E3-12: getErrorStream() es nulo en un 4xx/5xx sin cuerpo.
            byte[] readData = inputStream == null ? new byte[0] : SifenUtil.getByteArrayFromInputStream(inputStream);
            soapResponse.setRawData(readData);

            if (readData.length == 0) {
                if (estado >= 400) {
                    throw SifenException.of(estado >= 500 ? ErrorCode.JBS_CONEX_09 : ErrorCode.JBS_CONEX_08,
                            "HTTP " + estado + " sin cuerpo", null, estado);
                }
                throw SifenException.ofResponse(ErrorCode.JBS_RESP_02, "HTTP " + estado + " sin cuerpo", null);
            }
            SOAPMessage recibido;
            try {
                recibido = SoapHelper.parseSoapMessage(new ByteArrayInputStream(readData));
                recibido.getSOAPPart().getEnvelope(); // SAAJ parsea en diferido: se fuerza acá
            } catch (SOAPException | RuntimeException e) {
                ErrorCode codigo = estado >= 500 ? ErrorCode.JBS_CONEX_09
                        : estado >= 400 ? ErrorCode.JBS_CONEX_08 : ErrorCode.JBS_CONEX_10;
                throw SifenException.of(codigo, "HTTP " + estado + ", cuerpo no SOAP: " + inicio(readData), e, estado);
            }
            soapResponse.setSoapResponse(recibido);
        } catch (SifenException e) {
            throw enviado ? e.markSent() : e;
        } catch (IOException e) {
            // §5.2 (E0-05): el contexto que se usó se descarta; el pedido siguiente arma uno nuevo
            if (tls.invalidate(tlsUsado)) {
                LOGGER.info("[{}] Contexto TLS descartado tras una falla de transporte ({})", id, e.getClass().getName());
            }
            ErrorCode codigo = classify(e, route, conectado);
            if (codigo == ErrorCode.JBS_CONEX_02 && auth != null && proxyAuthRejected(e)) {
                // §10.7 (I5-09): el túnel cayó con un 407 teniendo credencial: el proxy la rechazó o el JDK no la envió
                codigo = ErrorCode.JBS_CONEX_11;
            }
            String donde = route.proxied() ? url + " (proxy " + route.describe() + ")" : url.toString();
            String nota = codigo == ErrorCode.JBS_CONEX_11 ? credentialNote(auth, url) : "";
            SifenException x = SifenException.of(codigo, donde + ": " + e.getClass().getName() + ": " + e.getLocalizedMessage() + nota,
                    e, argument(codigo, url, route.describe(), config));
            LOGGER.debug("[{}] Falla de transporte {}: {}", id, codigo.getCode(), e.getClass().getName());
            throw enviado ? x.markSent() : x;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Código de una falla de entrada/salida: con un proxy (de la configuración o de la JVM) y antes de
     * haber conectado, primero las filas del proxy ({@code JBS-CONEX-02}, E5-03); si no, las del transporte.
     */
    static ErrorCode classify(IOException e, Route route, boolean conectado) {
        Optional<ErrorCode> deProxy = route.proxied() && !conectado
                ? ExceptionTranslator.translate(e, ExceptionTranslator.Stage.PROXY_CONNECT) : Optional.empty();
        return deProxy.or(() -> ExceptionTranslator.translate(e, ExceptionTranslator.Stage.TRANSPORT))
                .orElse(ErrorCode.JBS_CONEX_07);
    }

    /**
     * Abre y arma la conexión HTTP(S) del pedido; la comparte el diagnóstico (E5-04). No fija método ni
     * cuerpo.
     *
     * @param config configuración (tiempos, usuario del proxy, {@code User-Agent}).
     * @param url    URL.
     * @param proxy  proxy de la configuración; {@code null} = el que elija la JVM.
     * @param tls    contexto TLS de la conexión; {@code null} = el de la JVM (solo {@code http://}).
     * @param auth   credencial del proxy de la configuración; {@code null} = sin usuario.
     * @return la conexión, sin conectar.
     * @throws IOException    si no se puede abrir.
     * @throws SifenException {@code JBS-CFG-04} si la URL no es HTTP.
     */
    static HttpURLConnection open(SifenConfig config, URL url, Proxy proxy, SSLContext tls, ProxyAuthenticator auth)
            throws IOException, SifenException {
        URLConnection abierta = proxy == null ? url.openConnection() : url.openConnection(proxy);
        if (!(abierta instanceof HttpURLConnection)) {
            throw SifenException.ofRule("JBS-CFG-04", ValidationMessages.message("JBS-CFG-04"),
                    "La conexión de " + url.getProtocol() + " no es HTTP: " + abierta.getClass().getName(), null);
        }
        HttpURLConnection c = (HttpURLConnection) abierta;
        if (c instanceof HttpsURLConnection https && tls != null) {
            https.setSSLSocketFactory(tls.getSocketFactory());
        }
        if (auth != null) {
            c.setAuthenticator(auth);
            if (!(c instanceof HttpsURLConnection)) {
                // destino http:// por el proxy: el pedido va al proxy tal cual y la credencial viaja en él
                c.setRequestProperty("Proxy-Authorization", basic(auth.user, auth.password));
            }
        }
        c.setUseCaches(false);
        // un 3xx lo informa el transporte (JBS-CONEX-10); con el envío de largo fijo el JDK tampoco lo sigue
        c.setInstanceFollowRedirects(false);
        c.setConnectTimeout(config.getHttpConnectTimeout());
        c.setReadTimeout(config.getHttpReadTimeout());
        c.setRequestProperty("User-Agent", config.getUserAgent());
        return c;
    }

    /**
     * Proxy por el que sale el pedido (E5-03): el de la configuración o, si no hay, el primero de tipo
     * HTTP que elige el {@code ProxySelector} de la JVM para esa URL.
     *
     * @param config configuración.
     * @param url    URL del pedido.
     * @return la ruta; {@code proxied()} en {@code false} si sale directo.
     * @throws SifenException {@code JBS-CFG-10} si el proxy de la configuración no tiene puerto válido.
     */
    static Route route(SifenConfig config, URL url) throws SifenException {
        Proxy configured = proxyOf(config);
        if (configured != null) {
            return new Route(configured, configured, config.getProxyHost() + ":" + config.getProxyPort(), false);
        }
        Proxy jvm = jvmProxy(url);
        if (jvm != null && jvm.address() instanceof InetSocketAddress a) {
            return new Route(null, jvm, a.getHostString() + ":" + a.getPort(), true);
        }
        return new Route(null, null, null, false);
    }

    /** El primer proxy HTTP que el {@code ProxySelector} de la JVM elige para la URL, o {@code null}. */
    static Proxy jvmProxy(URL url) {
        try {
            ProxySelector selector = ProxySelector.getDefault();
            if (selector == null) {
                return null;
            }
            for (Proxy p : selector.select(url.toURI())) {
                if (p.type() == Proxy.Type.HTTP) {
                    return p;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("No se pudo consultar el proxy de la JVM ({})", e.getClass().getName());
        }
        return null;
    }

    /** Estado 407 en la línea de estado HTTP que el JDK copia en el mensaje («… HTTP/1.1 407 …»). */
    private static final java.util.regex.Pattern ESTADO_407 = java.util.regex.Pattern.compile("HTTP/\\d(?:\\.\\d)? 407\\b");

    /**
     * Indica si la falla del túnel es un 407 del proxy. Busca el código en la línea de estado HTTP, no en
     * cualquier parte del mensaje (I10-23: un puerto {@code :8407} o un nombre de proxy no lo confunden).
     *
     * @param e falla.
     * @return {@code true} si alguna causa informa un {@code HTTP/x.y 407}.
     */
    static boolean proxyAuthRejected(Throwable e) {
        for (Throwable c = e; c != null; c = c.getCause() == c ? null : c.getCause()) {
            if (c.getMessage() != null && ESTADO_407.matcher(c.getMessage()).find()) {
                return true;
            }
        }
        return false;
    }

    /** Qué pasó con la credencial del proxy, para el detalle técnico de un {@code JBS-CONEX-11}. */
    static String credentialNote(ProxyAuthenticator auth, URL url) {
        if (auth == null) {
            return "";
        }
        if (auth.wasUsed() || !"https".equalsIgnoreCase(url.getProtocol())) {
            return "; el proxy rechazó la credencial del usuario " + auth.user + " (se entregó una sola vez)";
        }
        return "; el JDK no envió la credencial en el CONNECT (jdk.http.auth.tunneling.disabledSchemes, I5-04)";
    }

    @Override
    public SSLContext tlsContext(SifenConfig config) throws SifenException {
        return tls.get(config).context();
    }

    @Override
    public void invalidate() {
        tls.clear();
    }

    @Override
    public void close() {
        tls.clear();
    }

    /**
     * Indica si la conexión tiene un contexto TLS armado (pruebas).
     *
     * @return {@code true} si hay.
     */
    boolean hasTlsContext() {
        return tls.isBuilt();
    }

    @Override
    public String toString() {
        return "UrlSifenConnection[tls=" + (tls.isBuilt() ? "armado" : "sin armar") + "]";
    }

    /**
     * Proxy de la configuración, o {@code null} si no tiene (rige el de la JVM).
     *
     * @throws SifenException {@code JBS-CFG-10} si hay servidor y el puerto no sirve.
     */
    static Proxy proxyOf(SifenConfig config) throws SifenException {
        if (config == null || config.getProxyHost() == null) {
            return null;
        }
        int port = config.getProxyPort();
        if (port < 1 || port > 65535) {
            throw SifenException.ofRule("JBS-CFG-10", ValidationMessages.message("JBS-CFG-10", SifenConfig.PROXY_PORT_KEY),
                    "proxy " + config.getProxyHost() + " sin puerto válido (" + port + ")", null);
        }
        // sin resolver: el nombre del proxy se resuelve al conectar, y su falla es JBS-CONEX-02
        return new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(config.getProxyHost(), port));
    }

    /** Argumento del mensaje según el código (contrato de argumentos en {@code SifenConnectionException}). */
    static Object argument(ErrorCode codigo, URL url, String proxyText, SifenConfig config) {
        switch (codigo) {
            case JBS_CONEX_02:
            case JBS_CONEX_11:
                return proxyText == null ? "de la JVM" : proxyText;
            case JBS_CONEX_04:
                return config.getHttpConnectTimeout() / 1000;
            case JBS_CONEX_05:
                return config.getHttpReadTimeout() / 1000;
            default:
                return url.getHost();
        }
    }

    /** Primeros 200 caracteres del cuerpo, sin etiquetas HTML (para el detalle técnico). */
    static String inicio(byte[] datos) {
        String texto = new String(datos, 0, Math.min(datos.length, 2000), StandardCharsets.UTF_8)
                .replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        return texto.length() > 200 ? texto.substring(0, 200) : texto;
    }

    private static String basic(String user, String password) {
        String par = user + ":" + (password == null ? "" : password);
        return "Basic " + Base64.getEncoder().encodeToString(par.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Credencial del proxy de la configuración para <b>un</b> intercambio (E5-02): la entrega una sola vez;
     * ante el segundo 407 devuelve {@code null} y el JDK corta el túnel en vez de repetir el {@code CONNECT}
     * con la misma credencial rechazada. Solo responde a pedidos de proxy.
     */
    static final class ProxyAuthenticator extends Authenticator {
        private final String user;
        private final String password;
        private final AtomicBoolean used = new AtomicBoolean();

        private ProxyAuthenticator(String user, String password) {
            this.user = user;
            this.password = password;
        }

        /**
         * Credencial para un intercambio, o {@code null} si la configuración no tiene proxy con usuario.
         *
         * @param config configuración.
         * @param proxy  proxy de la configuración ({@code null} = sin proxy propio: no hay credencial).
         * @return el autenticador nuevo, o {@code null}.
         */
        static ProxyAuthenticator of(SifenConfig config, Proxy proxy) {
            return proxy == null || config.getProxyUser() == null ? null
                    : new ProxyAuthenticator(config.getProxyUser(), config.getProxyPassword());
        }

        /**
         * Indica si la credencial ya se entregó al JDK.
         *
         * @return {@code true} si se entregó.
         */
        boolean wasUsed() {
            return used.get();
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            if (getRequestorType() != RequestorType.PROXY || !used.compareAndSet(false, true)) {
                return null;
            }
            return new PasswordAuthentication(user, password == null ? new char[0] : password.toCharArray());
        }
    }
}
