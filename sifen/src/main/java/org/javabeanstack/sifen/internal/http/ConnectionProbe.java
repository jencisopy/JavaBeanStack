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
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javabeanstack.sifen.api.ConnectionDiagnosis;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.exceptions.ErrorCode;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.validation.ValidationMessages;
import org.javabeanstack.sifen.internal.facade.ExceptionTranslator;
import org.javabeanstack.sifen.internal.util.SifenUtil;

/**
 * Diagnóstico de la conexión con SIFEN por etapas, sin enviar ningún documento (plan JBSSIF, §5.3;
 * RF10, M0-09). Es lo que {@code ISifenClient.checkConnection()} devuelve cuando la fachada lo cablea
 * (carril F); reemplaza la etapa {@code NOT_AVAILABLE} de §10.2.
 *
 * <p>Etapas, en orden; la primera que falla corta las siguientes:</p>
 * <ol>
 *   <li>{@value #CONFIG}: la URL base es absoluta y {@code https} ({@code JBS-CFG-03}/{@code JBS-CFG-04});
 *       con {@code http://} sigue, porque es una prueba local;</li>
 *   <li>{@value #PROXY}: si hay proxy (el de la configuración, o el que elige el {@code ProxySelector} de
 *       la JVM), se conecta por TCP con él ({@code JBS-CONEX-02});</li>
 *   <li>{@value #DNS} y {@value #TCP}: sin proxy, resuelve el nombre ({@code JBS-CONEX-01}) y abre la
 *       conexión TCP ({@code JBS-CONEX-03}/{@code JBS-CONEX-04});</li>
 *   <li>{@value #TLS}: sin proxy, el saludo TLS con el certificado de cliente de la conexión
 *       ({@code JBS-CERT-*} si no se carga, {@code JBS-CONEX-06} si el saludo falla);</li>
 *   <li>{@value #HTTP}: un {@code GET} del WSDL de la consulta de RUC por el mismo camino que un pedido
 *       (proxy, TLS, tiempos); cualquier respuesta HTTP cuenta como etapa superada: SIFEN respondió.</li>
 * </ol>
 * <p>Con TLS 1.3 el cliente da por terminado el saludo antes de que el servidor revise su certificado:
 * un certificado de cliente rechazado puede pasar la etapa {@value #TLS} y fallar en {@value #HTTP}.
 * Por proxy no hay etapas {@value #DNS}, {@value #TCP} ni {@value #TLS} propias (las hace el túnel):
 * las cubre {@value #HTTP}.</p>
 *
 * <p>Cada etapa informa su duración en milisegundos y, si falla, el código y el mensaje para pantalla.
 * Nunca lanza. Como un diagnóstico es para la pantalla de soporte, una falla de transporte descarta
 * además el contexto TLS de la conexión, igual que un pedido.</p>
 */
public final class ConnectionProbe {
    /** Etapa de la configuración de la URL. */
    public static final String CONFIG = "CONFIG";
    /** Etapa del proxy. */
    public static final String PROXY = "PROXY";
    /** Etapa de la resolución del nombre. */
    public static final String DNS = "DNS";
    /** Etapa de la conexión TCP. */
    public static final String TCP = "TCP";
    /** Etapa del saludo TLS. */
    public static final String TLS = "TLS";
    /** Etapa del pedido HTTP. */
    public static final String HTTP = "HTTP";

    private static final Logger LOGGER = LogManager.getLogger(ConnectionProbe.class);

    private ConnectionProbe() {
    }

    /**
     * Diagnostica la conexión de la configuración.
     *
     * @param config      configuración.
     * @param connection  conexión del cliente (su contexto TLS); {@code null} = la de la configuración.
     * @param operationId id de operación que va en el resultado.
     * @return el diagnóstico; nunca {@code null} ni lanza.
     */
    public static ConnectionDiagnosis diagnose(SifenConfig config, SifenConnection connection, String operationId) {
        List<ConnectionDiagnosis.Step> steps = new ArrayList<>();
        try {
            run(config, connection != null ? connection : SifenConnection.of(config), steps);
        } catch (RuntimeException e) {
            LOGGER.error("[{}] {} en el diagnóstico de conexión ({})", OperationContext.label(operationId),
                    ErrorCode.JBS_INT_00.getCode(), e.getClass().getName());
            steps.add(ConnectionDiagnosis.Step.failed("INTERNAL", 0, ErrorCode.JBS_INT_00.getCode(), ErrorCode.JBS_INT_00.message()));
        }
        ConnectionDiagnosis d = ConnectionDiagnosis.of(steps, operationId);
        LOGGER.info("[{}] Diagnóstico de conexión: {}", OperationContext.label(operationId), d.isOk() ? "correcto" : "con fallas");
        return d;
    }

    private static void run(SifenConfig config, SifenConnection connection, List<ConnectionDiagnosis.Step> steps) {
        long t0 = System.nanoTime();
        if (config == null) {
            steps.add(rule(CONFIG, t0, "JBS-CFG-01"));
            return;
        }
        String base = SifenUtil.coalesce(config.getBaseUrl(), config.getLocalBaseUrl());
        URL url;
        try {
            url = URI.create(base + config.getQueryRucPath() + "?wsdl").toURL();
        } catch (IllegalArgumentException | IOException | NullPointerException e) {
            steps.add(rule(CONFIG, t0, "JBS-CFG-03"));
            return;
        }
        String protocolo = url.getProtocol();
        if (!"https".equalsIgnoreCase(protocolo) && !"http".equalsIgnoreCase(protocolo)) {
            steps.add(rule(CONFIG, t0, "JBS-CFG-04"));
            return;
        }
        int puerto = url.getPort() > 0 ? url.getPort() : url.getDefaultPort();
        steps.add(ConnectionDiagnosis.Step.ok(CONFIG, ms(t0)));

        // proxy: el de la configuración o el que elegiría la JVM para esta URL
        Proxy proxy;
        try {
            proxy = UrlSifenConnection.proxyOf(config);
        } catch (SifenException e) {
            steps.add(ConnectionDiagnosis.Step.failed(PROXY, 0, e.getCode(), e.getUserMessage()));
            return;
        }
        if (proxy == null) {
            proxy = jvmProxy(url);
        }
        int timeout = config.getHttpConnectTimeout();
        if (proxy != null && proxy.type() == Proxy.Type.HTTP) {
            long t = System.nanoTime();
            InetSocketAddress a = (InetSocketAddress) proxy.address();
            String texto = a.getHostString() + ":" + a.getPort();
            try (Socket s = new Socket()) {
                s.connect(a.isUnresolved() ? new InetSocketAddress(a.getHostString(), a.getPort()) : a, timeout);
                steps.add(ConnectionDiagnosis.Step.ok(PROXY, ms(t)));
            } catch (IOException | RuntimeException e) {
                steps.add(failed(PROXY, t, ErrorCode.JBS_CONEX_02, texto));
                return;
            }
        } else {
            long t = System.nanoTime();
            InetAddress ip;
            try {
                ip = InetAddress.getByName(url.getHost());
                steps.add(ConnectionDiagnosis.Step.ok(DNS, ms(t)));
            } catch (IOException e) {
                steps.add(failed(DNS, t, ErrorCode.JBS_CONEX_01, url.getHost()));
                return;
            }
            t = System.nanoTime();
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(ip, puerto), timeout);
                steps.add(ConnectionDiagnosis.Step.ok(TCP, ms(t)));
            } catch (IOException e) {
                ErrorCode c = ExceptionTranslator.translate(e, ExceptionTranslator.Stage.TRANSPORT).orElse(ErrorCode.JBS_CONEX_03);
                steps.add(failed(TCP, t, c, argument(c, url.getHost(), config)));
                return;
            }
            if ("https".equalsIgnoreCase(protocolo)) {
                t = System.nanoTime();
                SSLContext ctx;
                try {
                    ctx = connection.tlsContext(config);
                } catch (SifenException e) {
                    steps.add(ConnectionDiagnosis.Step.failed(TLS, ms(t), e.getCode(), e.getUserMessage()));
                    return;
                }
                try (Socket plano = new Socket()) {
                    plano.connect(new InetSocketAddress(ip, puerto), timeout);
                    plano.setSoTimeout(config.getHttpReadTimeout());
                    try (SSLSocket tls = (SSLSocket) ctx.getSocketFactory().createSocket(plano, url.getHost(), puerto, false)) {
                        tls.startHandshake();
                    }
                    steps.add(ConnectionDiagnosis.Step.ok(TLS, ms(t)));
                } catch (IOException e) {
                    connection.invalidate();
                    ErrorCode c = ExceptionTranslator.translate(e, ExceptionTranslator.Stage.TRANSPORT).orElse(ErrorCode.JBS_CONEX_06);
                    steps.add(failed(TLS, t, c, argument(c, url.getHost(), config)));
                    return;
                }
            }
        }
        steps.add(http(config, connection, url, proxy));
    }

    /**
     * {@code GET} del WSDL por el mismo camino que un pedido (E5-04): la conexión la arma
     * {@code UrlSifenConnection.open} con el proxy, la credencial, el contexto TLS y los tiempos del pedido.
     */
    private static ConnectionDiagnosis.Step http(SifenConfig config, SifenConnection connection, URL url, Proxy proxy) {
        long t = System.nanoTime();
        HttpURLConnection c = null;
        UrlSifenConnection.ProxyAuthenticator auth = null;
        try {
            Proxy configurado = UrlSifenConnection.proxyOf(config);
            auth = UrlSifenConnection.ProxyAuthenticator.of(config, configurado);
            SSLContext ctx = "https".equalsIgnoreCase(url.getProtocol()) ? connection.tlsContext(config) : null;
            c = UrlSifenConnection.open(config, url, configurado, ctx, auth);
            c.setRequestMethod("GET");
            int estado = c.getResponseCode();
            if (estado == HttpURLConnection.HTTP_PROXY_AUTH) {
                LOGGER.debug("Diagnóstico: 407 del proxy{}", UrlSifenConnection.credentialNote(auth, url));
                // §10.7 (I10-22): con credencial configurada, el mismo código que la operación (JBS-CONEX-11)
                return failed(HTTP, t, auth == null ? ErrorCode.JBS_CONEX_02 : ErrorCode.JBS_CONEX_11, proxyText(proxy));
            }
            LOGGER.debug("Diagnóstico: HTTP {} de {}", estado, url.getHost());
            return ConnectionDiagnosis.Step.ok(HTTP, ms(t));
        } catch (SifenException e) {
            return ConnectionDiagnosis.Step.failed(HTTP, ms(t), e.getCode(), e.getUserMessage());
        } catch (IOException e) {
            connection.invalidate();
            ErrorCode codigo = ExceptionTranslator.translate(e, ExceptionTranslator.Stage.TRANSPORT).orElse(ErrorCode.JBS_CONEX_07);
            if (codigo == ErrorCode.JBS_CONEX_02 && auth != null && UrlSifenConnection.proxyAuthRejected(e)) {
                codigo = ErrorCode.JBS_CONEX_11; // §10.7 (I10-22): como la operación
            }
            if (codigo == ErrorCode.JBS_CONEX_02 || codigo == ErrorCode.JBS_CONEX_11) {
                LOGGER.debug("Diagnóstico: el proxy rechazó el túnel{}", UrlSifenConnection.credentialNote(auth, url));
            }
            return failed(HTTP, t, codigo, codigo == ErrorCode.JBS_CONEX_02 || codigo == ErrorCode.JBS_CONEX_11
                    ? proxyText(proxy) : argument(codigo, url.getHost(), config));
        } finally {
            if (c != null) {
                c.disconnect();
            }
        }
    }

    private static Proxy jvmProxy(URL url) {
        return UrlSifenConnection.jvmProxy(url);
    }

    private static String proxyText(Proxy proxy) {
        if (proxy == null || !(proxy.address() instanceof InetSocketAddress a)) {
            return "de la JVM";
        }
        return a.getHostString() + ":" + a.getPort();
    }

    private static Object argument(ErrorCode c, String host, SifenConfig config) {
        switch (c) {
            case JBS_CONEX_04:
                return config.getHttpConnectTimeout() / 1000;
            case JBS_CONEX_05:
                return config.getHttpReadTimeout() / 1000;
            default:
                return host;
        }
    }

    private static ConnectionDiagnosis.Step failed(String step, long t0, ErrorCode code, Object arg) {
        return ConnectionDiagnosis.Step.failed(step, ms(t0), code.getCode(), code.message(arg));
    }

    private static ConnectionDiagnosis.Step rule(String step, long t0, String code) {
        return ConnectionDiagnosis.Step.failed(step, ms(t0), code, ValidationMessages.message(code));
    }

    private static long ms(long t0) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
    }

    /** Para las pruebas: el código de la primera etapa fallida, si hay. */
    static Optional<String> firstFailure(ConnectionDiagnosis d) {
        return d.getSteps().stream().filter(s -> !s.isOk()).map(ConnectionDiagnosis.Step::getCode).findFirst();
    }
}
