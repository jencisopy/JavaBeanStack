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
package org.javabeanstack.sifen.internal.facade;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.javabeanstack.sifen.Sifen;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.beans.EventosDE;
import org.javabeanstack.sifen.core.beans.ValidezFirmaDigital;
import org.javabeanstack.sifen.core.exceptions.ErrorCategory;
import org.javabeanstack.sifen.core.exceptions.Operation;
import org.javabeanstack.sifen.core.exceptions.SifenConnectionException;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.fields.request.de.TgTotSub;
import org.javabeanstack.sifen.internal.http.SifenConnection;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.ThrowingSupplier;

/**
 * CA3-09 (plan JBSSIF, §3.8, D33 Parte C): inyección de fallas por operación de la fachada.
 *
 * <p>Cada fila de la matriz es una operación pública × un punto de falla simulable. Se exige que lo
 * que sale sea una {@link SifenException} (o, en {@code validarFirmaDE*}, una
 * {@link ValidezFirmaDigital} con código) con el código y la categoría esperados, mensaje no vacío,
 * id de correlación de 8 hexadecimales en el mensaje y al principio de una línea del log, y ninguna
 * {@code NullPointerException} en la cadena de causas salvo en las filas INTERNAL que la esperan (la
 * red sobre la línea base, {@code validacionPrevia=false}).</p>
 *
 * <p>Sin red real: el servidor es HTTPS en {@code 127.0.0.1} con el certificado autofirmado y
 * responde lo grabado en {@code src/test/resources/respuestas/} o una variación (vacío, HTML, 4xx,
 * 5xx, corte, demora); el puerto cerrado es uno efímero que se abre y se cierra. Nunca se contacta
 * SIFEN (D28). La matriz queda en {@code target/fallas/ca3_09_matriz.tsv}. Los puntos que no se
 * pueden provocar sin red (DNS, proxy, tiempo de conexión, algoritmo ausente) se prueban en
 * {@link TraductorExcepcionesTest} y se marcan «traductor» en la documentación.</p>
 */
public class FachadaFallasTest {
    private static final Pattern ID = Pattern.compile("[0-9a-f]{8}");
    private static final String LOGGER_FACHADA = Operations.class.getName();

    private static HttpsServer servidor;
    private static ExecutorService hilos;
    private static SifenConfig cfg;
    private static SifenConfig cfgLenta;
    private static int puertoCerrado;
    /** I3-61: socket que contesta texto plano al conectar; un cliente https falla en el handshake TLS. */
    private static ServerSocket servidorPlano;
    private static volatile int estado = 200;
    private static volatile byte[] cuerpo = new byte[0];
    private static volatile String modo = "normal";
    private static HostnameVerifier verificadorOriginal;
    private static final String[] PROPIEDADES = {"javax.net.ssl.trustStore", "javax.net.ssl.trustStorePassword", "javax.net.ssl.trustStoreType"};
    private static final String[] valoresOriginales = new String[PROPIEDADES.length];
    private static final List<String> LOG = Collections.synchronizedList(new ArrayList<>());
    private static final List<String> MATRIZ = Collections.synchronizedList(new ArrayList<>());
    private static AbstractAppender captura;

    @BeforeAll
    public static void levantar() throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = FixturesSifen.class.getClassLoader().getResourceAsStream(FixturesSifen.PFX)) {
            ks.load(in, FixturesSifen.PFX_CLAVE.toCharArray());
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, FixturesSifen.PFX_CLAVE.toCharArray());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        SSLContext tlsServidor = SSLContext.getInstance("TLS");
        tlsServidor.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        servidor = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.setHttpsConfigurator(new HttpsConfigurator(tlsServidor) {
            @Override
            public void configure(HttpsParameters params) {
                SSLParameters p = getSSLContext().getDefaultSSLParameters();
                p.setNeedClientAuth(true);
                params.setSSLParameters(p);
            }
        });
        servidor.createContext("/", intercambio -> {
            try (InputStream in = intercambio.getRequestBody()) {
                in.readAllBytes();
            }
            String m = modo;
            if ("cerrar".equals(m)) {
                intercambio.close();
                return;
            }
            if ("tardar".equals(m)) {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] b = cuerpo;
            intercambio.getResponseHeaders().add("Content-Type", "application/soap+xml; charset=utf-8");
            intercambio.sendResponseHeaders(estado, b.length == 0 ? -1 : b.length);
            if (b.length > 0) {
                try (OutputStream out = intercambio.getResponseBody()) {
                    out.write(b);
                } catch (java.io.IOException ignorada) {
                    // el cliente cortó por tiempo
                }
            }
            intercambio.close();
        });
        hilos = Executors.newCachedThreadPool();
        servidor.setExecutor(hilos);
        servidor.start();

        for (int i = 0; i < PROPIEDADES.length; i++) {
            valoresOriginales[i] = System.getProperty(PROPIEDADES[i]);
        }
        System.setProperty("javax.net.ssl.trustStore", FixturesSifen.rutaPfx().toString());
        System.setProperty("javax.net.ssl.trustStorePassword", FixturesSifen.PFX_CLAVE);
        System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
        verificadorOriginal = HttpsURLConnection.getDefaultHostnameVerifier();
        HttpsURLConnection.setDefaultHostnameVerifier((host, sesion) -> "127.0.0.1".equals(host));

        cfg = FixturesSifen.configuracion();
        cfg.setBaseUrl("https://127.0.0.1:" + servidor.getAddress().getPort());
        cfgLenta = FixturesSifen.configuracion();
        cfgLenta.setBaseUrl(cfg.getBaseUrl());
        // §5.2: tiempo de lectura configurable (antes, reflexión sobre el campo final; I5-01)
        cfgLenta.setHttpReadTimeout(300);
        try (ServerSocket s = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
            puertoCerrado = s.getLocalPort();
        }
        servidorPlano = new ServerSocket(0, 5, java.net.InetAddress.getLoopbackAddress());
        Thread plano = new Thread(() -> {
            while (!servidorPlano.isClosed()) {
                try (java.net.Socket so = servidorPlano.accept()) {
                    so.getOutputStream().write("HTTP/1.1 400 Bad Request\r\nContent-Length: 0\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                    so.getOutputStream().flush();
                } catch (java.io.IOException e) {
                    // cerrado al bajar
                }
            }
        }, "servidor-plano-i3-61");
        plano.setDaemon(true);
        plano.start();

        // La configuración por omisión de log4j2 deja la raíz en ERROR: el logger del envoltorio se
        // configura aparte (WARN incluido) y sin aditividad, para no ensuciar la salida de la corrida.
        LoggerContext contexto = (LoggerContext) LogManager.getContext(false);
        captura = new AbstractAppender("canario-ca309", null, null, true, Property.EMPTY_ARRAY) {
            @Override
            public void append(LogEvent event) {
                LOG.add(event.getMessage().getFormattedMessage());
            }
        };
        captura.start();
        LoggerConfig propio = LoggerConfig.newBuilder().withLoggerName(LOGGER_FACHADA).withLevel(Level.ALL)
                .withAdditivity(false).withConfig(contexto.getConfiguration()).build();
        propio.addAppender(captura, Level.ALL, null);
        contexto.getConfiguration().addLogger(LOGGER_FACHADA, propio);
        contexto.updateLoggers();
    }

    @AfterAll
    public static void bajar() throws Exception {
        if (servidor != null) {
            servidor.stop(0);
        }
        if (servidorPlano != null) {
            servidorPlano.close();
        }
        if (hilos != null) {
            hilos.shutdownNow();
        }
        for (int i = 0; i < PROPIEDADES.length; i++) {
            if (valoresOriginales[i] == null) {
                System.clearProperty(PROPIEDADES[i]);
            } else {
                System.setProperty(PROPIEDADES[i], valoresOriginales[i]);
            }
        }
        if (verificadorOriginal != null) {
            HttpsURLConnection.setDefaultHostnameVerifier(verificadorOriginal);
        }
        liberarConexiones(cfg, cfgLenta);
        LoggerContext contexto = (LoggerContext) LogManager.getContext(false);
        contexto.getConfiguration().removeLogger(LOGGER_FACHADA);
        contexto.updateLoggers();
        captura.stop();
        Path destino = Paths.get("target", "fallas", "ca3_09_matriz.tsv");
        Files.createDirectories(destino.getParent());
        List<String> lineas = new ArrayList<>();
        lineas.add("operacion\tfalla\tcodigo\tcategoria\tid\tnpe_en_causas\tresultado_desconocido");
        lineas.addAll(MATRIZ);
        Files.write(destino, lineas, StandardCharsets.UTF_8);
    }

    /**
     * Cierra las conexiones de las configuraciones de esta clase (I5-01): desde §5.2 el contexto TLS es de
     * cada {@code SifenConnection} (por instancia de {@code SifenConfig}), no un estático que haya que vaciar.
     */
    private static void liberarConexiones(SifenConfig... configs) {
        for (SifenConfig c : configs) {
            if (c != null) {
                SifenConnection.release(c);
            }
        }
    }

    // ------------------------------------------------------------------ servidor

    private static void responder(int codigo, String recurso) throws Exception {
        modo = "normal";
        estado = codigo;
        cuerpo = recurso == null ? new byte[0] : FixturesSifen.recursoBytes("respuestas/" + recurso);
    }

    private static void responderTexto(int codigo, String texto) {
        modo = "normal";
        estado = codigo;
        cuerpo = texto.getBytes(StandardCharsets.UTF_8);
    }

    private static void responderModo(String m, String recurso) throws Exception {
        responder(200, recurso);
        modo = m;
    }

    private static String grabada(String recurso) throws Exception {
        return new String(FixturesSifen.recursoBytes("respuestas/" + recurso), StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------ datos

    private static DocumentoElectronico factura() {
        return CasosDePrueba.documentos().get(4).construir();
    }

    private static List<DocumentoElectronico> lote(DocumentoElectronico... des) {
        List<DocumentoElectronico> l = new ArrayList<>();
        Collections.addAll(l, des);
        return l;
    }

    private static EventosDE evento() {
        return CasosDePrueba.eventos().get(0).construir();
    }

    private static SifenConfig conCertificado(String ruta) {
        SifenConfig c = FixturesSifen.configuracion();
        c.setBaseUrl(cfg.getBaseUrl());
        c.setClientCertificate(ruta);
        return c;
    }

    /**
     * I3-61: nombre que no resuelve (RFC 6761, {@code .invalid}). M3-62: lo rechaza {@code ResolverDePrueba}
     * (resolver de los tests, JEP 418) sin consultar al del sistema; el test lo comprueba.
     */
    private static SifenConfig dnsCfg() {
        SifenConfig c = FixturesSifen.configuracion();
        c.setBaseUrl("https://sifen-jbs-prueba.invalid:443");
        return c;
    }

    /** I3-61: https contra un servidor HTTP plano local: el handshake falla antes de enviar. */
    private static SifenConfig tlsCfg() {
        SifenConfig c = FixturesSifen.configuracion();
        c.setBaseUrl("https://127.0.0.1:" + servidorPlano.getLocalPort());
        return c;
    }

    private static SifenConfig puertoCerradoCfg() {
        SifenConfig c = FixturesSifen.configuracion();
        c.setBaseUrl("https://127.0.0.1:" + puertoCerrado);
        return c;
    }

    /** Respuesta grabada con un cambio de texto (para provocar RESP-04/06/07 desde una real). */
    private static void responderVariante(String recurso, String de, String a) throws Exception {
        String xml = grabada(recurso);
        Assertions.assertTrue(xml.contains(de), "la variante de " + recurso + " tiene que cambiar algo: " + de);
        responderTexto(200, xml.replace(de, a));
    }

    // ------------------------------------------------------------------ matriz

    /** Una fila: preparación del servidor + llamada + lo esperado. */
    private record Fila(Operation op, String falla, String codigo, ErrorCategory categoria, boolean npeEsperada,
            Preparar preparar, Llamada llamada) {
    }

    @FunctionalInterface
    private interface Preparar {
        void hacer() throws Exception;
    }

    @FunctionalInterface
    private interface Llamada {
        Object hacer() throws Exception;
    }

    private static Fila f(Operation op, String falla, String codigo, ErrorCategory cat, Preparar p, Llamada l) {
        return new Fila(op, falla, codigo, cat, false, p, l);
    }

    private static Fila npe(Operation op, String falla, Preparar p, Llamada l) {
        return new Fila(op, falla, "JBS-INT-00", ErrorCategory.INTERNAL, true, p, l);
    }

    /** Fila INTERNAL diagnosticada con su código, cuya causa inyectada es una NPE. */
    private static Fila npe(Operation op, String falla, String codigo, Preparar p, Llamada l) {
        return new Fila(op, falla, codigo, ErrorCategory.INTERNAL, true, p, l);
    }

    private static final Preparar NADA = () -> responder(200, "consulta-ruc-0502.xml");

    private static List<Fila> matriz() throws Exception {
        List<Fila> m = new ArrayList<>();
        SifenConfig conVal = FixturesSifen.configuracionConValidacion();
        conVal.setBaseUrl(cfg.getBaseUrl());
        String pfxInexistente = Files.createTempDirectory("ca309").resolve("borrado.pfx").toString();

        // ---------------------------------------------------------------- GENERATE_XML (+ firma y QR)
        m.add(f(Operation.GENERATE_XML, "configuración nula", "JBS-CFG-01", ErrorCategory.CONFIGURATION, NADA,
                () -> factura().generarXml(FixturesSifen.contexto(cfg), (SifenConfig) null)));
        m.add(f(Operation.GENERATE_XML, "DE incompleto con validación", "JBS-D-39", ErrorCategory.DE_CONTENT, NADA, () -> {
            DocumentoElectronico de = factura();
            de.getgDatGralOpe().getgEmis().setdNomEmi(org.javabeanstack.sifen.core.validation.support.Legends.MT);
            de.getgDatGralOpe().setgDatRec(null);
            return de.generarXml(FixturesSifen.contexto(conVal), conVal);
        }));
        m.add(npe(Operation.GENERATE_XML, "DE incompleto sin validación (red)", NADA, () -> {
            DocumentoElectronico de = factura();
            de.getgDatGralOpe().setgDatRec(null);
            return de.generarXml(FixturesSifen.contexto(cfg), cfg);
        }));
        m.add(f(Operation.GENERATE_XML, "certificado borrado, con validación", "JBS-CERT-01", ErrorCategory.CERTIFICATE, NADA, () -> {
            SifenConfig c = conCertificado(pfxInexistente);
            c.setPreValidationEnabled(true);
            return factura().generarXml(FixturesSifen.contexto(c), c);
        }));
        m.add(f(Operation.GENERATE_XML, "firma: certificado borrado sin validación", "JBS-CERT-01", ErrorCategory.CERTIFICATE, NADA,
                () -> factura().generarXml(FixturesSifen.contexto(conCertificado(pfxInexistente)), conCertificado(pfxInexistente))));
        m.add(f(Operation.GENERATE_XML, "firma: contraseña incorrecta (mutilada por reflexión)", "JBS-CERT-03", ErrorCategory.CERTIFICATE, NADA, () -> {
            SifenConfig c = conCertificado(FixturesSifen.rutaPfx().toString());
            Field campo = SifenConfig.class.getDeclaredField("contrasenaCertificadoCliente");
            campo.setAccessible(true);
            campo.set(c, "otra-clave");
            return factura().generarXml(FixturesSifen.contexto(c), c);
        }));
        m.add(f(Operation.GENERATE_XML, "firma: sin certificado de cliente (usar=false)", "JBS-CFG-12", ErrorCategory.CONFIGURATION, NADA, () -> {
            SifenConfig c = conCertificado(FixturesSifen.rutaPfx().toString());
            c.setUseClientCertificate(false);
            return factura().generarXml(FixturesSifen.contexto(c), c);
        }));
        m.add(npe(Operation.GENERATE_XML, "QR: totales que no se pueden leer (NPE inyectada)", "JBS-FIRMA-04", NADA, () -> {
            DocumentoElectronico base = factura();
            DocumentoElectronico de = new DocumentoElectronico() {
                @Override
                public TgTotSub getgTotSub() {
                    return null; // setupDE usa el campo; el QR, el getter
                }
            };
            de.setdFecFirma(base.getdFecFirma());
            de.setdSisFact(base.getdSisFact());
            de.setgOpeDE(base.getgOpeDE());
            de.setgTimb(base.getgTimb());
            de.setgDatGralOpe(base.getgDatGralOpe());
            de.setgDtipDE(base.getgDtipDE());
            de.setgTotSub(base.getgTotSub());
            de.setgCamGen(base.getgCamGen());
            de.setgCamDEAsocList(base.getgCamDEAsocList());
            return de.generarXml(FixturesSifen.contexto(cfg), cfg);
        }));
        m.add(npe(Operation.GENERATE_XML, "contexto de generación nulo", NADA, () -> factura().generarXml((org.javabeanstack.sifen.internal.ctx.GenerationCtx) null)));

        // ---------------------------------------------------------------- COMPUTE_CDC
        m.add(f(Operation.COMPUTE_CDC, "DE vacío (I3-49: dato del documento, no error interno)", "JBS-ENT-07", ErrorCategory.DE_CONTENT, NADA,
                () -> new DocumentoElectronico().obtenerCDC()));

        // ---------------------------------------------------------------- SEND_DE
        m.add(f(Operation.SEND_DE, "DE nulo", "JBS-VAL-01", ErrorCategory.INPUT, NADA, () -> Sifen.recepcionDE(null, cfg)));
        m.add(f(Operation.SEND_DE, "configuración global no fijada", "JBS-CFG-01", ErrorCategory.CONFIGURATION, NADA,
                () -> Sifen.recepcionDE(factura(), null)));
        m.add(f(Operation.SEND_DE, "SOAP Fault (HTTP 500)", "JBS-RESP-01", ErrorCategory.SIFEN_RESPONSE,
                () -> responder(500, "soap-fault.xml"), () -> Sifen.recepcionDE(factura(), cfg)));
        m.add(f(Operation.SEND_DE, "cuerpo vacío (HTTP 200)", "JBS-RESP-02", ErrorCategory.SIFEN_RESPONSE,
                () -> responder(200, null), () -> Sifen.recepcionDE(factura(), cfg)));
        m.add(f(Operation.SEND_DE, "nodo de otra operación", "JBS-RESP-02", ErrorCategory.SIFEN_RESPONSE,
                () -> responder(200, "consulta-ruc-0502.xml"), () -> Sifen.recepcionDE(factura(), cfg)));
        m.add(f(Operation.SEND_DE, "HTML de un portal (HTTP 200)", "JBS-CONEX-10", ErrorCategory.CONNECTION,
                () -> responder(200, "http-500.html"), () -> Sifen.recepcionDE(factura(), cfg)));
        m.add(f(Operation.SEND_DE, "HTML (HTTP 500)", "JBS-CONEX-09", ErrorCategory.CONNECTION,
                () -> responder(500, "http-500.html"), () -> Sifen.recepcionDE(factura(), cfg)));
        m.add(f(Operation.SEND_DE, "HTTP 404 sin cuerpo (E3-12)", "JBS-CONEX-08", ErrorCategory.CONNECTION,
                () -> responder(404, null), () -> Sifen.recepcionDE(factura(), cfg)));
        m.add(f(Operation.SEND_DE, "HTTP 503 sin cuerpo (E3-12)", "JBS-CONEX-09", ErrorCategory.CONNECTION,
                () -> responder(503, null), () -> Sifen.recepcionDE(factura(), cfg)));
        m.add(f(Operation.SEND_DE, "sin rProtDe", "JBS-RESP-04", ErrorCategory.SIFEN_RESPONSE,
                () -> responderTexto(200, "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"><env:Body>"
                        + "<ns2:rRetEnviDe xmlns:ns2=\"http://ekuatia.set.gov.py/sifen/xsd\"/></env:Body></env:Envelope>"),
                () -> Sifen.recepcionDE(factura(), cfg)));
        m.add(f(Operation.SEND_DE, "aprobado sin dProtAut", "JBS-RESP-07", ErrorCategory.SIFEN_RESPONSE,
                () -> responderVariante("recepcion-de-0260.xml", "<ns2:dProtAut>1000000001</ns2:dProtAut>", ""),
                () -> Sifen.recepcionDE(factura(), cfg)));
        m.add(f(Operation.SEND_DE, "fecha de la respuesta ilegible", "JBS-RESP-06", ErrorCategory.SIFEN_RESPONSE,
                () -> responderVariante("recepcion-de-0260.xml", "2026-11-23T10:00:07-03:00", "23/11/2026"),
                () -> Sifen.recepcionDE(factura(), cfg)));
        m.add(f(Operation.SEND_DE, "conexión cortada sin respuesta", "JBS-CONEX-07", ErrorCategory.CONNECTION,
                () -> responderModo("cerrar", "recepcion-de-0260.xml"), () -> Sifen.recepcionDE(factura(), cfg)));
        m.add(f(Operation.SEND_DE, "SIFEN tarda (tiempo de lectura)", "JBS-CONEX-05", ErrorCategory.CONNECTION,
                () -> responderModo("tardar", "recepcion-de-0260.xml"), () -> Sifen.recepcionDE(factura(), cfgLenta)));
        m.add(f(Operation.SEND_DE, "puerto cerrado", "JBS-CONEX-03", ErrorCategory.CONNECTION, NADA,
                () -> Sifen.recepcionDE(factura(), puertoCerradoCfg())));
        // I3-61: fallas anteriores al envío (en connect()): no marcan el resultado como desconocido.
        // M3-62: el nombre .invalid lo rechaza ResolverDePrueba, no el resolver del sistema
        m.add(f(Operation.SEND_DE, "DNS: el nombre no resuelve", "JBS-CONEX-01", ErrorCategory.CONNECTION, NADA,
                () -> Sifen.recepcionDE(factura(), dnsCfg())));
        m.add(f(Operation.SEND_DE, "TLS: el servidor no habla TLS", "JBS-CONEX-06", ErrorCategory.CONNECTION, NADA,
                () -> Sifen.recepcionDE(factura(), tlsCfg())));

        // ---------------------------------------------------------------- SEND_BATCH
        m.add(f(Operation.SEND_BATCH, "lista nula", "JBS-VAL-01", ErrorCategory.INPUT, NADA, () -> Sifen.recepcionLoteDE(null, cfg)));
        m.add(f(Operation.SEND_BATCH, "más de 50 DE", "JBS-ENT-01", ErrorCategory.INPUT, NADA, () -> {
            List<DocumentoElectronico> l = new ArrayList<>();
            for (int i = 0; i < 51; i++) {
                l.add(factura());
            }
            return Sifen.recepcionLoteDE(l, cfg);
        }));
        m.add(f(Operation.SEND_BATCH, "CDC repetido", "JBS-ENT-02", ErrorCategory.INPUT, NADA,
                () -> Sifen.recepcionLoteDE(lote(factura(), factura()), cfg)));
        m.add(f(Operation.SEND_BATCH, "recibido sin dProtConsLote", "JBS-RESP-07", ErrorCategory.SIFEN_RESPONSE,
                () -> responderVariante("recepcion-lote-0300.xml", "<ns2:dProtConsLote>1234567890123456</ns2:dProtConsLote>", ""),
                () -> Sifen.recepcionLoteDE(lote(factura()), cfg)));
        m.add(f(Operation.SEND_BATCH, "SOAP Fault", "JBS-RESP-01", ErrorCategory.SIFEN_RESPONSE,
                () -> responder(500, "soap-fault.xml"), () -> Sifen.recepcionLoteDE(lote(factura()), cfg)));
        m.add(f(Operation.SEND_BATCH, "SIFEN tarda", "JBS-CONEX-05", ErrorCategory.CONNECTION,
                () -> responderModo("tardar", "recepcion-lote-0300.xml"), () -> Sifen.recepcionLoteDE(lote(factura()), cfgLenta)));

        // ---------------------------------------------------------------- SEND_EVENTS
        m.add(f(Operation.SEND_EVENTS, "eventos nulos", "JBS-VAL-01", ErrorCategory.INPUT, NADA, () -> Sifen.recepcionEvento(null, cfg)));
        m.add(f(Operation.SEND_EVENTS, "sin gResProcEVe", "JBS-RESP-04", ErrorCategory.SIFEN_RESPONSE,
                () -> responderTexto(200, "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"><env:Body>"
                        + "<ns2:rRetEnviEventoDe xmlns:ns2=\"http://ekuatia.set.gov.py/sifen/xsd\"><ns2:dFecProc>2026-11-23T10:07:00-03:00"
                        + "</ns2:dFecProc></ns2:rRetEnviEventoDe></env:Body></env:Envelope>"),
                () -> Sifen.recepcionEvento(evento(), cfg)));
        m.add(f(Operation.SEND_EVENTS, "HTTP 502 sin cuerpo", "JBS-CONEX-09", ErrorCategory.CONNECTION,
                () -> responder(502, null), () -> Sifen.recepcionEvento(evento(), cfg)));
        m.add(f(Operation.SEND_EVENTS, "conexión cortada", "JBS-CONEX-07", ErrorCategory.CONNECTION,
                () -> responderModo("cerrar", "evento-0600.xml"), () -> Sifen.recepcionEvento(evento(), cfg)));

        // ---------------------------------------------------------------- QUERY_DE
        m.add(f(Operation.QUERY_DE, "CDC nulo", "JBS-ENT-03", ErrorCategory.INPUT, NADA, () -> Sifen.consultaDE(null, cfg)));
        m.add(f(Operation.QUERY_DE, "CDC con DV incorrecto", "JBS-ENT-03", ErrorCategory.INPUT, NADA, () -> {
            String cdc = CasosDePrueba.CDC_FACTURA_SIMPLE;
            char dv = cdc.charAt(43);
            return Sifen.consultaDE(cdc.substring(0, 43) + (dv == '9' ? '0' : (char) (dv + 1)), cfg);
        }));
        m.add(f(Operation.QUERY_DE, "SOAP Fault", "JBS-RESP-01", ErrorCategory.SIFEN_RESPONSE,
                () -> responder(500, "soap-fault.xml"), () -> Sifen.consultaDE(CasosDePrueba.CDC_FACTURA_SIMPLE, cfg)));
        m.add(f(Operation.QUERY_DE, "sin dCodRes", "JBS-RESP-04", ErrorCategory.SIFEN_RESPONSE,
                () -> responderVariante("consulta-de-0422.xml", "<ns2:dCodRes>0422</ns2:dCodRes>", ""),
                () -> Sifen.consultaDE(CasosDePrueba.CDC_FACTURA_SIMPLE, cfg)));
        m.add(f(Operation.QUERY_DE, "puerto cerrado", "JBS-CONEX-03", ErrorCategory.CONNECTION, NADA,
                () -> Sifen.consultaDE(CasosDePrueba.CDC_FACTURA_SIMPLE, puertoCerradoCfg())));

        // ---------------------------------------------------------------- QUERY_BATCH
        m.add(f(Operation.QUERY_BATCH, "número de lote con letras", "JBS-ENT-05", ErrorCategory.INPUT, NADA,
                () -> Sifen.consultaLoteDE("12AB", cfg)));
        m.add(f(Operation.QUERY_BATCH, "sin dCodResLot", "JBS-RESP-04", ErrorCategory.SIFEN_RESPONSE,
                () -> responderVariante("consulta-lote-0362.xml", "<ns2:dCodResLot>0362</ns2:dCodResLot>", ""),
                () -> Sifen.consultaLoteDE("1234567890123456", cfg)));
        m.add(f(Operation.QUERY_BATCH, "HTTP 401 sin cuerpo", "JBS-CONEX-08", ErrorCategory.CONNECTION,
                () -> responder(401, null), () -> Sifen.consultaLoteDE("1234567890123456", cfg)));

        // ---------------------------------------------------------------- QUERY_RUC
        m.add(f(Operation.QUERY_RUC, "RUC nulo", "JBS-ENT-04", ErrorCategory.INPUT, NADA, () -> Sifen.consultaRUC(null, cfg)));
        m.add(f(Operation.QUERY_RUC, "RUC con guion y DV", "JBS-ENT-04", ErrorCategory.INPUT, NADA, () -> Sifen.consultaRUC("80123456-5", cfg)));
        m.add(f(Operation.QUERY_RUC, "configuración nula", "JBS-CFG-01", ErrorCategory.CONFIGURATION, NADA,
                () -> Sifen.consultaRUC("80123456", (SifenConfig) null)));
        m.add(f(Operation.QUERY_RUC, "URL http:// (E0-10)", "JBS-CFG-04", ErrorCategory.CONFIGURATION, NADA, () -> {
            SifenConfig c = FixturesSifen.configuracion();
            c.setBaseUrl("http://127.0.0.1:" + puertoCerrado);
            return Sifen.consultaRUC("80123456", c);
        }));
        m.add(f(Operation.QUERY_RUC, "certificado borrado", "JBS-CERT-01", ErrorCategory.CERTIFICATE, NADA,
                () -> Sifen.consultaRUC("80123456", conCertificado(pfxInexistente))));
        m.add(f(Operation.QUERY_RUC, "HTML (HTTP 200)", "JBS-CONEX-10", ErrorCategory.CONNECTION,
                () -> responder(200, "http-500.html"), () -> Sifen.consultaRUC("80123456", cfg)));
        m.add(f(Operation.QUERY_RUC, "texto plano (HTTP 403)", "JBS-CONEX-08", ErrorCategory.CONNECTION,
                () -> responderTexto(403, "Forbidden"), () -> Sifen.consultaRUC("80123456", cfg)));
        m.add(f(Operation.QUERY_RUC, "SIFEN tarda", "JBS-CONEX-05", ErrorCategory.CONNECTION,
                () -> responderModo("tardar", "consulta-ruc-0502.xml"), () -> Sifen.consultaRUC("80123456", cfgLenta)));
        m.add(f(Operation.QUERY_RUC, "sin dCodRes", "JBS-RESP-04", ErrorCategory.SIFEN_RESPONSE,
                () -> responderVariante("consulta-ruc-0502.xml", "<ns2:dCodRes>0502</ns2:dCodRes>", ""),
                () -> Sifen.consultaRUC("80123456", cfg)));

        // ---------------------------------------------------------------- READ_XML
        m.add(f(Operation.READ_XML, "XML nulo", "JBS-XML-01", ErrorCategory.INPUT, NADA, () -> new DocumentoElectronico((String) null)));
        m.add(f(Operation.READ_XML, "XML sin nodo DE", "JBS-XML-02", ErrorCategory.INPUT, NADA, () -> new DocumentoElectronico("<rDE><otro/></rDE>")));
        m.add(f(Operation.READ_XML, "XML mal formado", "JBS-XML-03", ErrorCategory.INPUT, NADA, () -> new DocumentoElectronico("<rDE><DE>")));
        m.add(f(Operation.READ_XML, "falta el timbrado (CDC)", "JBS-XML-04", ErrorCategory.INPUT, NADA, () -> {
            String xml = FixturesSifen.entrada(CasosDePrueba.documentos().get(0));
            return new DocumentoElectronico(xml.replaceAll("(?s)<gTimb>.*</gTimb>", ""));
        }));
        m.add(f(Operation.READ_XML, "valor no numérico", "JBS-XML-05", ErrorCategory.INPUT, NADA, () -> {
            String xml = FixturesSifen.entrada(CasosDePrueba.documentos().get(0));
            return new DocumentoElectronico(xml.replaceFirst("<iNatRec>\\d</iNatRec>", "<iNatRec>X</iNatRec>"));
        }));

        // ---------------------------------------------------------------- LOAD_CONFIG
        m.add(f(Operation.LOAD_CONFIG, "configuración nula", "JBS-CFG-01", ErrorCategory.CONFIGURATION, NADA, () -> {
            Sifen.setSifenConfig(null);
            return null;
        }));
        m.add(f(Operation.LOAD_CONFIG, "archivo inexistente", "JBS-ENT-06", ErrorCategory.CONFIGURATION, NADA,
                () -> SifenConfig.load(pfxInexistente + ".properties")));
        m.add(f(Operation.LOAD_CONFIG, "propiedades sin datos", "JBS-CFG-02", ErrorCategory.CONFIGURATION, NADA,
                () -> SifenConfig.load(new Properties())));
        m.add(f(Operation.LOAD_CONFIG, "InputStream nulo", "JBS-ENT-06", ErrorCategory.CONFIGURATION, NADA,
                () -> SifenConfig.load((InputStream) null)));
        return m;
    }

    @TestFactory
    public List<DynamicTest> cadaOperacionPorCadaFalla() throws Exception {
        List<DynamicTest> tests = new ArrayList<>();
        for (Fila fila : matriz()) {
            tests.add(DynamicTest.dynamicTest(fila.op() + " · " + fila.falla(), () -> verificar(fila)));
        }
        return tests;
    }

    private static void verificar(Fila fila) throws Exception {
        fila.preparar().hacer();
        Throwable lanzada = null;
        try {
            fila.llamada().hacer();
        } catch (Throwable e) {
            lanzada = e;
        }
        final Throwable t = lanzada;
        Assertions.assertNotNull(t, fila.falla() + ": no lanzó");
        Assertions.assertInstanceOf(SifenException.class, t, () -> fila.falla() + ": salió " + t);
        SifenException e = (SifenException) t;
        if (fila.categoria() == ErrorCategory.DE_CONTENT && !e.getErrors().isEmpty()) {
            // una excepción de validación lleva el código de su primera regla con error y la lista completa
            Assertions.assertTrue(e.getErrors().stream().anyMatch(err -> err.getCode().equals(fila.codigo())),
                    () -> fila.falla() + ": " + e);
            Assertions.assertEquals(e.getErrors().stream().filter(err -> err.isError()).findFirst().get().getCode(),
                    e.getCode());
        } else {
            Assertions.assertEquals(fila.codigo(), e.getCode(), () -> fila.falla() + ": " + e + " | " + e.getTechnicalDetail());
        }
        Assertions.assertEquals(fila.categoria(), e.getCategory(), () -> fila.falla() + ": " + e);
        Assertions.assertEquals(fila.op(), e.getOperation(), () -> fila.falla() + ": " + e);
        Assertions.assertFalse(e.getUserMessage() == null || e.getUserMessage().isBlank(), fila.falla() + ": mensaje vacío");
        Assertions.assertNotNull(e.getCorrelationId(), fila.falla() + ": sin id");
        Assertions.assertTrue(ID.matcher(e.getCorrelationId()).matches(), e.getCorrelationId());
        Assertions.assertTrue(e.getMessage().endsWith("(ref. " + e.getCorrelationId() + ")"), e.getMessage());
        boolean npe = npeEnCausas(e);
        if (fila.npeEsperada()) {
            Assertions.assertTrue(npe, fila.falla() + ": se esperaba la NPE de la línea base en la causa");
        } else {
            Assertions.assertFalse(npe, () -> fila.falla() + ": NullPointerException sin diagnosticar en las causas de " + e);
        }
        String linea = buscarLinea(e.getCorrelationId());
        Assertions.assertNotNull(linea, fila.falla() + ": el log no tiene una línea que empiece con [" + e.getCorrelationId() + "]");
        Assertions.assertTrue(linea.contains(e.getCode()), linea);
        boolean desconocido = e.isOutcomeUnknown();
        // I3-48 / I3-61: en una recepción, solo las fallas posteriores a writeTo (lectura, corte, HTTP sin
        // SOAP y las de la respuesta); DNS, proxy, puerto cerrado, tiempo al conectar y TLS (01..04, 06)
        // ocurren en connect(), antes de enviar
        boolean despuesDeEnviar = DESPUES_DE_ENVIAR.contains(fila.codigo()) || fila.codigo().startsWith("JBS-RESP-");
        Assertions.assertEquals(fila.op().isSend() && despuesDeEnviar, desconocido,
                fila.falla() + ": resultado desconocido solo en una recepción que falla después de enviar");
        MATRIZ.add(fila.op() + "\t" + fila.falla() + "\t" + e.getCode() + "\t" + e.getCategory() + "\t"
                + e.getCorrelationId() + "\t" + npe + "\t" + desconocido);
    }

    /**
     * M3-62: la fila DNS no depende del resolver del sistema: el de los tests está activo (resuelve su
     * nombre testigo, que nadie más conoce) y rechaza los {@code .invalid}.
     */
    @org.junit.jupiter.api.Test
    public void elNombreDePruebaLoRechazaElResolverDeLosTests() throws Exception {
        Assertions.assertTrue(java.net.InetAddress.getByName(org.javabeanstack.sifen.test.casos.ResolverDePrueba.TESTIGO)
                .isLoopbackAddress(), "el resolver de los tests no está registrado");
        Assertions.assertThrows(java.net.UnknownHostException.class, () -> java.net.InetAddress.getByName("sifen-jbs-prueba.invalid"));
    }

    private static final java.util.Set<String> DESPUES_DE_ENVIAR = java.util.Set.of("JBS-CONEX-05", "JBS-CONEX-07",
            "JBS-CONEX-08", "JBS-CONEX-09", "JBS-CONEX-10");

    private static boolean npeEnCausas(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause() == c ? null : c.getCause()) {
            if (c instanceof NullPointerException) {
                return true;
            }
        }
        return false;
    }

    private static String buscarLinea(String id) {
        synchronized (LOG) {
            for (String l : LOG) {
                if (l.startsWith("[" + id + "]")) {
                    return l;
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ VERIFY_SIGNATURE (no lanza)

    @TestFactory
    public List<DynamicTest> validarFirmaDevuelveLaFallaConCodigo() throws Exception {
        List<DynamicTest> tests = new ArrayList<>();
        tests.add(DynamicTest.dynamicTest("VALIDAR_FIRMA · XML nulo", () -> firma("XML nulo", "JBS-XML-01", ErrorCategory.INPUT,
                () -> Sifen.validarFirmaDEDesdeXml(null))));
        tests.add(DynamicTest.dynamicTest("VALIDAR_FIRMA · XML mal formado", () -> firma("XML mal formado", "JBS-XML-03", ErrorCategory.INPUT,
                () -> Sifen.validarFirmaDEDesdeXml("<rDE><DE>"))));
        tests.add(DynamicTest.dynamicTest("VALIDAR_FIRMA · ruta inexistente", () -> firma("ruta inexistente", "JBS-FIRMA-01", ErrorCategory.INPUT,
                () -> Sifen.validarFirmaDE("/no/existe/de.xml"))));
        tests.add(DynamicTest.dynamicTest("VALIDAR_FIRMA · archivo nulo", () -> firma("archivo nulo", "JBS-FIRMA-01", ErrorCategory.INPUT,
                () -> Sifen.validarFirmaDE((java.io.File) null))));
        tests.add(DynamicTest.dynamicTest("VALIDAR_FIRMA · certificado sin número de documento", () -> {
            SifenConfig c = conCertificado(FixturesSifen.rutaPfx().getParent().resolve("jbs-sifen-sin-firma.p12").toString());
            String xml = factura().generarXml(FixturesSifen.contexto(c), c);
            ValidezFirmaDigital v = Sifen.validarFirmaDEDesdeXml(xml);
            Assertions.assertFalse(v.isValido());
            Assertions.assertEquals("JBS-FIRMA-05", v.getCode(), v.getMotivoInvalidez());
            Assertions.assertEquals(ErrorCategory.CERTIFICATE, v.getCategory());
            MATRIZ.add(Operation.VERIFY_SIGNATURE + "\tcertificado sin número de documento (veredicto)\t" + v.getCode()
                    + "\t" + v.getCategory() + "\t—\tfalse\tfalse");
        }));
        return tests;
    }

    private static void firma(String falla, String codigo, ErrorCategory cat, ThrowingSupplier<ValidezFirmaDigital> llamada) throws Throwable {
        ValidezFirmaDigital v = llamada.get();
        Assertions.assertFalse(v.isValido(), falla);
        Assertions.assertEquals(codigo, v.getCode(), () -> falla + ": " + v.getMotivoInvalidez());
        Assertions.assertEquals(cat, v.getCategory(), falla);
        Assertions.assertNotNull(v.getCorrelationId(), falla);
        Assertions.assertTrue(v.getMotivoInvalidez().endsWith("(ref. " + v.getCorrelationId() + ")"), v.getMotivoInvalidez());
        Assertions.assertNotNull(buscarLinea(v.getCorrelationId()), falla + ": sin línea de log con el id");
        MATRIZ.add(Operation.VERIFY_SIGNATURE + "\t" + falla + "\t" + v.getCode() + "\t" + v.getCategory() + "\t"
                + v.getCorrelationId() + "\tfalse\tfalse");
    }

    // ------------------------------------------------------------------ E3-13 (parcial) y E2-04

    /**
     * E3-13: un {@code &lt;} legítimo y un {@code &#36;} ({@code $}) en {@code dMsgRes} rompían el
     * reparseo desescapado (la línea base perdía el {@code dCodRes} o lanzaba
     * {@code IllegalArgumentException}); ahora el releído falla con aviso {@code JBS-RESP-03} en el
     * log y se usa el mensaje original.
     */
    @org.junit.jupiter.api.Test
    public void consultaDeConEntidadesEnElMensajeConservaElCodigo() throws Exception {
        responderVariante("consulta-de-0422.xml", "<ns2:dMsgRes>CDC encontrado</ns2:dMsgRes>",
                "<ns2:dMsgRes>Monto &lt; 100 &#36; y &#x41;</ns2:dMsgRes>");
        org.javabeanstack.sifen.core.beans.response.RespuestaConsultaDE r = Sifen.consultaDE(CasosDePrueba.CDC_FACTURA_SIMPLE, cfg);
        Assertions.assertEquals("0422", r.getdCodRes());
        Assertions.assertEquals("Monto < 100 $ y A", r.getdMsgRes());
    }

    /** E3-13: las referencias numéricas hexadecimales, inválidas y con {@code $} no lanzan. */
    @org.junit.jupiter.api.Test
    public void elDesescapeNoLanza() {
        Assertions.assertEquals("A", org.javabeanstack.sifen.internal.util.SifenUtil.unescapeXML("&#x41;"));
        Assertions.assertEquals("$", org.javabeanstack.sifen.internal.util.SifenUtil.unescapeXML("&#36;"));
        Assertions.assertEquals("&#xZZ;", org.javabeanstack.sifen.internal.util.SifenUtil.unescapeXML("&#xZZ;"));
        Assertions.assertEquals("<a>", org.javabeanstack.sifen.internal.util.SifenUtil.unescapeXML("&lt;a&gt;"));
    }
}
