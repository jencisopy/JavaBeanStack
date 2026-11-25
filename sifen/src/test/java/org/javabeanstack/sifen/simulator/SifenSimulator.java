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
package org.javabeanstack.sifen.simulator;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsExchange;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.javabeanstack.sifen.core.SifenConfig;

/**
 * Simulador de SIFEN para probar sin red ni certificado (plan JBSSIF, §8.6, M3-29): un servidor HTTPS
 * del JDK en {@code 127.0.0.1}, con un puerto efímero, que contesta respuestas grabadas por servicio y
 * por empresa y puede provocar las fallas de transporte (corte, 502/504, respuesta que no es SOAP,
 * demora). Se distribuye en el {@code test-jar} de {@code jbs-sifen} (clasificador {@code tests}) y no
 * depende de JUnit.
 *
 * <pre>{@code
 * try (SifenSimulator sim = SifenSimulator.start()) {
 *     ISifenClient sifen = SifenClient.create(sim.config());
 *     sim.on(SimulatedService.QUERY_RUC, Scenario.rucFound());
 *     RucQueryResult r = sifen.queryRuc("80123456");
 *     sim.once(SimulatedService.QUERY_DE, Scenario.connectionCut()); // solo el próximo pedido
 *     ...
 * }
 * }</pre>
 *
 * <p><b>Configuración lista</b>: {@link #config()} devuelve una {@link SifenConfig} de ambiente
 * {@code DEV} con la URL del simulador, el certificado autofirmado y desechable del emisor ficticio
 * {@code 80123456-5} <b>en línea</b> (Base64: sirve también con el simulador dentro de un jar), su
 * contraseña, el CSC público de desarrollo y la NT 13 activa. {@link #config(String)} agrega un prefijo de empresa a la
 * URL, para separar los pedidos de varias empresas; {@link #config(String, byte[], String)} usa otro
 * certificado de cliente.</p>
 *
 * <p><b>Estado global de la JVM</b> (mientras haya un simulador abierto): el cliente de
 * {@code jbs-sifen} confía en el almacén por omisión de la JVM, así que {@link #start()} apunta
 * {@code javax.net.ssl.trustStore*} a un archivo temporal con el certificado del servidor (SAN
 * {@code 127.0.0.1} y {@code localhost}: no hace falta un verificador de nombre propio). Desde §5.2 el
 * contexto TLS es de cada conexión de la librería (uno por instancia de {@code SifenConfig}) y se arma con
 * el almacén vigente: el simulador ya no vacía ningún estático (I5-01). El último {@link #close()}
 * restaura las propiedades y borra el archivo. Mientras {@code SifenConfig} no tenga un almacén de
 * confianza propio, esto es estado global (M8-01). Las pruebas que usan el simulador no deben correr en paralelo con otras que dependan del
 * almacén de confianza.</p>
 *
 * <p>El servidor exige certificado de cliente (TLS mutuo, como SIFEN), acepta cualquiera y lo registra
 * en cada {@link RecordedRequest}: es lo que permite comprobar qué certificado usó cada empresa.</p>
 */
public final class SifenSimulator implements AutoCloseable {
    /** Contraseña de los almacenes desechables del simulador. */
    public static final String PASSWORD = "jbs-sifen-test";
    /** RUC (sin DV) del emisor del certificado de cliente por omisión. */
    public static final String ISSUER_RUC = "80123456";
    /** DV del emisor del certificado de cliente por omisión. */
    public static final String ISSUER_DV = "5";
    /** CSC público de desarrollo (no sirve en producción). */
    public static final String DEV_CSC = "ABCD0000000000000000000000000000";
    /** Id del CSC público de desarrollo. */
    public static final String DEV_CSC_ID = "0001";

    private static final String CERTS = "org/javabeanstack/sifen/simulator/certificates/";
    private static final String[] TRUST_PROPERTIES = {"javax.net.ssl.trustStore", "javax.net.ssl.trustStorePassword",
        "javax.net.ssl.trustStoreType"};
    private static final Object GLOBAL = new Object();
    private static int open;
    private static String[] savedProperties;
    private static Path trustFile;

    private final HttpsServer server;
    private final ExecutorService threads;
    private final String baseUrl;
    private final Map<String, Scenario> defaults = new ConcurrentHashMap<>();
    private final Map<String, Queue<Scenario>> queued = new ConcurrentHashMap<>();
    private final List<RecordedRequest> requests = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean closed;

    private SifenSimulator(HttpsServer server, ExecutorService threads) {
        this.server = server;
        this.threads = threads;
        this.baseUrl = "https://127.0.0.1:" + server.getAddress().getPort();
        resetScenarios();
    }

    /**
     * Levanta un simulador en {@code 127.0.0.1} con un puerto libre, con los escenarios por omisión: todo
     * aprobado o encontrado (recepción 0260, lote 0300, evento 0600, consulta de DE 0422, de RUC 0502 y de
     * lote 0362).
     *
     * @return el simulador, ya escuchando.
     * @throws IOException si no se puede abrir el puerto o leer los certificados del simulador.
     */
    public static SifenSimulator start() throws IOException {
        return start(0);
    }

    /**
     * Levanta un simulador en un puerto dado: sirve para simular la caída y la vuelta de SIFEN (cerrar uno y
     * levantar otro en el mismo puerto, con los clientes ya creados apuntando a esa URL).
     *
     * @param port puerto de {@code 127.0.0.1}; {@code 0} = uno libre.
     * @return el simulador, ya escuchando.
     * @throws IOException si el puerto está ocupado o no se pueden leer los certificados del simulador.
     */
    public static SifenSimulator start(int port) throws IOException {
        SSLContext tls;
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            try (InputStream in = resourceStream(CERTS + "simulator-server.p12")) {
                ks.load(in, PASSWORD.toCharArray());
            }
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, PASSWORD.toCharArray());
            tls = SSLContext.getInstance("TLS");
            tls.init(kmf.getKeyManagers(), new TrustManager[]{new AnyClientCertificate()}, null);
        } catch (GeneralSecurityException e) {
            throw new IOException("No se pudo armar el TLS del simulador", e);
        }
        HttpsServer server = HttpsServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(tls) {
            @Override
            public void configure(HttpsParameters params) {
                SSLParameters p = getSSLContext().getDefaultSSLParameters();
                p.setNeedClientAuth(true);
                params.setSSLParameters(p);
            }
        });
        AtomicInteger n = new AtomicInteger();
        ExecutorService threads = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "sifen-simulator-" + n.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
        server.setExecutor(threads);
        SifenSimulator sim = new SifenSimulator(server, threads);
        server.createContext("/", sim::handle);
        acquireGlobalTrust();
        try {
            server.start();
        } catch (RuntimeException e) {
            releaseGlobalTrust();
            throw e;
        }
        return sim;
    }

    // ------------------------------------------------------------------ configuración

    /** @return la URL base ({@code https://127.0.0.1:<puerto>}), sin barra final. */
    public String getBaseUrl() {
        return baseUrl;
    }

    /** @return el puerto en el que escucha. */
    public int getPort() {
        return server.getAddress().getPort();
    }

    /**
     * Configuración lista para {@code SifenClient.create}: {@code DEV}, URL del simulador, certificado
     * desechable del emisor {@code 80123456-5} en línea, CSC de desarrollo y validación previa activa
     * (el valor por omisión).
     *
     * @return una configuración nueva.
     */
    public SifenConfig config() {
        return config("");
    }

    /**
     * Configuración de una empresa: como {@link #config()}, con {@code /<tenant>} al final de la URL base.
     *
     * @param tenant prefijo de la empresa (letras, dígitos, guiones); {@code ""} = sin prefijo.
     * @return una configuración nueva.
     */
    public SifenConfig config(String tenant) {
        return config(tenant, defaultClientCertificate(), PASSWORD);
    }

    /**
     * Configuración de una empresa con su propio certificado de cliente (PKCS#12).
     *
     * @param tenant   prefijo de la empresa; {@code ""} = sin prefijo.
     * @param pkcs12   bytes del almacén PKCS#12 (va en línea, en Base64).
     * @param password contraseña del almacén.
     * @return una configuración nueva.
     */
    public SifenConfig config(String tenant, byte[] pkcs12, String password) {
        Objects.requireNonNull(tenant, "tenant");
        if (!tenant.matches("[A-Za-z0-9_-]*")) {
            throw new IllegalArgumentException("Prefijo de empresa no válido: " + tenant);
        }
        SifenConfig cfg = new SifenConfig();
        cfg.setEnvironment(SifenConfig.Environment.DEV);
        cfg.setBaseUrl(tenant.isEmpty() ? baseUrl : baseUrl + "/" + tenant);
        cfg.setUseClientCertificate(true);
        cfg.setClientCertificateType(SifenConfig.ClientCertificateType.PFX);
        cfg.setClientCertificate(Base64.getEncoder().encodeToString(pkcs12));
        cfg.setClientCertificatePassword(password);
        cfg.setCscId(DEV_CSC_ID);
        cfg.setCsc(DEV_CSC);
        enableTechnicalNote13(cfg);
        return cfg;
    }

    /**
     * NT 13 activa ({@code dBasExe}), como el ambiente de pruebas de SIFEN y los fixtures del módulo. El
     * flag se retira en §6.3 (D25: se escribe siempre); por reflexión, para que el simulador siga
     * compilando cuando desaparezca.
     */
    private static void enableTechnicalNote13(SifenConfig cfg) {
        try {
            SifenConfig.class.getMethod("setHabilitarNotaTecnica13", boolean.class).invoke(cfg, true);
        } catch (ReflectiveOperationException e) {
            // §6.3: ya no existe, dBasExe se escribe siempre
        }
    }

    /** @return los bytes del certificado de cliente por omisión (autofirmado, emisor {@code 80123456-5}). */
    public static byte[] defaultClientCertificate() {
        return resourceBytes(CERTS + "client.pfx");
    }

    // ------------------------------------------------------------------ escenarios

    /**
     * Fija lo que contesta un servicio, para todas las empresas, hasta que se cambie.
     *
     * @param service  servicio.
     * @param scenario respuesta.
     * @return este simulador.
     */
    public SifenSimulator on(SimulatedService service, Scenario scenario) {
        return on("", service, scenario);
    }

    /**
     * Fija lo que contesta un servicio a una empresa (gana sobre lo fijado para todas).
     *
     * @param tenant   empresa ({@code ""} = todas).
     * @param service  servicio.
     * @param scenario respuesta.
     * @return este simulador.
     */
    public SifenSimulator on(String tenant, SimulatedService service, Scenario scenario) {
        defaults.put(key(tenant, service), Objects.requireNonNull(scenario, "scenario"));
        return this;
    }

    /**
     * Encola una respuesta para el próximo pedido del servicio (de cualquier empresa); después vuelve a la
     * fijada con {@link #on}. Se pueden encolar varias.
     *
     * @param service  servicio.
     * @param scenario respuesta.
     * @return este simulador.
     */
    public SifenSimulator once(SimulatedService service, Scenario scenario) {
        queued.computeIfAbsent(key("", service), k -> new ConcurrentLinkedQueue<>())
                .add(Objects.requireNonNull(scenario, "scenario"));
        return this;
    }

    /** Vuelve a los escenarios por omisión, vacía la cola y olvida los pedidos recibidos. */
    public void reset() {
        resetScenarios();
        requests.clear();
    }

    private void resetScenarios() {
        defaults.clear();
        queued.clear();
        defaults.put(key("", SimulatedService.RECEIVE_DE), Scenario.approved());
        defaults.put(key("", SimulatedService.RECEIVE_BATCH), Scenario.batchReceived());
        defaults.put(key("", SimulatedService.EVENT), Scenario.eventRegistered());
        defaults.put(key("", SimulatedService.QUERY_DE), Scenario.deFound());
        defaults.put(key("", SimulatedService.QUERY_RUC), Scenario.rucFound());
        defaults.put(key("", SimulatedService.QUERY_BATCH), Scenario.batchCompleted());
    }

    // ------------------------------------------------------------------ pedidos recibidos

    /** @return copia de los pedidos recibidos, en orden de llegada. */
    public List<RecordedRequest> requests() {
        synchronized (requests) {
            return List.copyOf(requests);
        }
    }

    /**
     * Pedidos de una empresa.
     *
     * @param tenant empresa ({@code ""} = los que vinieron sin prefijo).
     * @return copia, en orden de llegada.
     */
    public List<RecordedRequest> requests(String tenant) {
        List<RecordedRequest> out = new ArrayList<>();
        for (RecordedRequest r : requests()) {
            if (r.getTenant().equals(tenant)) {
                out.add(r);
            }
        }
        return out;
    }

    /**
     * Pedidos de un servicio.
     *
     * @param service servicio.
     * @return copia, en orden de llegada.
     */
    public List<RecordedRequest> requests(SimulatedService service) {
        List<RecordedRequest> out = new ArrayList<>();
        for (RecordedRequest r : requests()) {
            if (r.getService() == service) {
                out.add(r);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ cierre

    /** Baja el servidor, corta las demoras pendientes y, si es el último abierto, restaura la JVM. Idempotente. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        server.stop(0);
        threads.shutdownNow();
        releaseGlobalTrust();
    }

    // ------------------------------------------------------------------ atención de pedidos

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String body;
            try (InputStream in = exchange.getRequestBody()) {
                body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            String path = exchange.getRequestURI().getPath();
            SimulatedService service = SimulatedService.of(path);
            String tenant = service == null ? "" : service.tenantOf(path);
            RecordedRequest request = new RecordedRequest(path, tenant, service, body, peer(exchange), Instant.now());
            requests.add(request);
            if (service == null) {
                send(exchange, 404, "text/plain; charset=utf-8", ("Ruta desconocida para el simulador: " + path)
                        .getBytes(StandardCharsets.UTF_8));
                return;
            }
            reply(exchange, scenarioFor(tenant, service), request);
        } finally {
            exchange.close();
        }
    }

    private Scenario scenarioFor(String tenant, SimulatedService service) {
        Queue<Scenario> q = queued.get(key("", service));
        Scenario s = q == null ? null : q.poll();
        if (s != null) {
            return s;
        }
        s = defaults.get(key(tenant, service));
        return s != null ? s : defaults.get(key("", service));
    }

    private void reply(HttpExchange exchange, Scenario scenario, RecordedRequest request) throws IOException {
        switch (scenario.kind()) {
            case CUT:
                // se cierra sin cabeceras: el cliente ve el fin del flujo sin respuesta
                return;
            case DELAY:
                try {
                    Thread.sleep(scenario.delay().toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                reply(exchange, scenario.then(), request);
                return;
            default:
                send(exchange, scenario.status(), scenario.contentType(), scenario.bodyFor(request));
        }
    }

    private static void send(HttpExchange exchange, int status, String contentType, byte[] body) {
        try {
            exchange.getResponseHeaders().add("Content-Type", contentType);
            exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
            if (body.length > 0) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            }
        } catch (IOException e) {
            // el cliente se fue (tiempo de lectura agotado): no hay a quién contestar
        }
    }

    private static X509Certificate peer(HttpExchange exchange) {
        if (!(exchange instanceof HttpsExchange)) {
            return null;
        }
        try {
            Certificate[] chain = ((HttpsExchange) exchange).getSSLSession().getPeerCertificates();
            return chain.length > 0 && chain[0] instanceof X509Certificate ? (X509Certificate) chain[0] : null;
        } catch (SSLPeerUnverifiedException e) {
            return null;
        }
    }

    private static String key(String tenant, SimulatedService service) {
        return Objects.requireNonNull(tenant, "tenant") + "|" + Objects.requireNonNull(service, "service").name();
    }

    // ------------------------------------------------------------------ estado global de la JVM

    private static void acquireGlobalTrust() throws IOException {
        synchronized (GLOBAL) {
            if (open++ == 0) {
                try {
                    trustFile = Files.createTempFile("jbs-sifen-simulator-trust", ".p12");
                    Files.write(trustFile, resourceBytes(CERTS + "simulator-trust.p12"));
                    trustFile.toFile().deleteOnExit();
                } catch (IOException | RuntimeException e) {
                    open--;
                    throw e;
                }
                savedProperties = new String[TRUST_PROPERTIES.length];
                for (int i = 0; i < TRUST_PROPERTIES.length; i++) {
                    savedProperties[i] = System.getProperty(TRUST_PROPERTIES[i]);
                }
                System.setProperty("javax.net.ssl.trustStore", trustFile.toString());
                System.setProperty("javax.net.ssl.trustStorePassword", PASSWORD);
                System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
            }
        }
    }

    private static void releaseGlobalTrust() {
        synchronized (GLOBAL) {
            if (open == 0 || --open > 0) {
                return;
            }
            for (int i = 0; i < TRUST_PROPERTIES.length; i++) {
                if (savedProperties[i] == null) {
                    System.clearProperty(TRUST_PROPERTIES[i]);
                } else {
                    System.setProperty(TRUST_PROPERTIES[i], savedProperties[i]);
                }
            }
            try {
                Files.deleteIfExists(trustFile);
            } catch (IOException e) {
                // queda para deleteOnExit
            }
            trustFile = null;
        }
    }

    private static InputStream resourceStream(String resource) throws IOException {
        InputStream in = SifenSimulator.class.getClassLoader().getResourceAsStream(resource);
        if (in == null) {
            throw new IOException("Falta el recurso del simulador " + resource);
        }
        return in;
    }

    private static byte[] resourceBytes(String resource) {
        try (InputStream in = resourceStream(resource)) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Acepta cualquier certificado de cliente: el simulador lo registra, no lo juzga. */
    private static final class AnyClientCertificate implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            // se acepta: queda en RecordedRequest
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            throw new UnsupportedOperationException("el simulador no es cliente");
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
