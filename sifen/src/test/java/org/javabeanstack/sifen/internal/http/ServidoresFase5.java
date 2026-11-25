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

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import org.javabeanstack.sifen.test.casos.FixturesSifen;

/**
 * Servidores locales de las pruebas de conexión de la Fase 5 (plan JBSSIF, §5.2–§5.5). Sin red: todo
 * escucha en {@code 127.0.0.1} y nunca se contacta SIFEN (D28).
 *
 * <ul>
 *   <li>{@link #https(int, HttpHandler)}: HTTPS del JDK con el certificado autofirmado del módulo, que
 *       exige certificado de cliente y solo confía en el autofirmado (TLS mutuo, como SIFEN);</li>
 *   <li>{@link Crudo}: socket TCP plano que decide qué hacer en cada conexión (cerrar sin responder,
 *       tardar, responder HTTP);</li>
 *   <li>{@link ProxyLocal}: proxy HTTP con {@code CONNECT} y pedidos absolutos, con o sin usuario.</li>
 * </ul>
 * <p>{@link #confiarEnElAutofirmado()} fija el almacén de confianza y el verificador de nombre de la JVM
 * como las pruebas vecinas; {@link #restaurar()} los deja como estaban.</p>
 */
public final class ServidoresFase5 {
    private static final String[] PROPIEDADES = {"javax.net.ssl.trustStore", "javax.net.ssl.trustStorePassword",
        "javax.net.ssl.trustStoreType"};
    private static final String[] ORIGINALES = new String[PROPIEDADES.length];
    private static HostnameVerifier verificadorOriginal;

    private ServidoresFase5() {
    }

    public static void confiarEnElAutofirmado() {
        for (int i = 0; i < PROPIEDADES.length; i++) {
            ORIGINALES[i] = System.getProperty(PROPIEDADES[i]);
        }
        System.setProperty("javax.net.ssl.trustStore", FixturesSifen.rutaPfx().toString());
        System.setProperty("javax.net.ssl.trustStorePassword", FixturesSifen.PFX_CLAVE);
        System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
        verificadorOriginal = HttpsURLConnection.getDefaultHostnameVerifier();
        HttpsURLConnection.setDefaultHostnameVerifier((host, sesion) -> "127.0.0.1".equals(host));
    }

    public static void restaurar() {
        for (int i = 0; i < PROPIEDADES.length; i++) {
            if (ORIGINALES[i] == null) {
                System.clearProperty(PROPIEDADES[i]);
            } else {
                System.setProperty(PROPIEDADES[i], ORIGINALES[i]);
            }
        }
        if (verificadorOriginal != null) {
            HttpsURLConnection.setDefaultHostnameVerifier(verificadorOriginal);
        }
    }

    public static SSLContext contextoServidor() throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = FixturesSifen.class.getClassLoader().getResourceAsStream(FixturesSifen.PFX)) {
            ks.load(in, FixturesSifen.PFX_CLAVE.toCharArray());
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, FixturesSifen.PFX_CLAVE.toCharArray());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        SSLContext c = SSLContext.getInstance("TLS");
        c.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return c;
    }

    /** HTTPS con TLS mutuo en el puerto indicado (0 = efímero). */
    public static HttpsServer https(int puerto, HttpHandler handler) throws Exception {
        HttpsServer s = HttpsServer.create(new InetSocketAddress("127.0.0.1", puerto), 0);
        s.setHttpsConfigurator(new HttpsConfigurator(contextoServidor()) {
            @Override
            public void configure(HttpsParameters params) {
                SSLParameters p = getSSLContext().getDefaultSSLParameters();
                p.setNeedClientAuth(true);
                params.setSSLParameters(p);
            }
        });
        s.createContext("/", handler);
        s.start();
        return s;
    }

    /** Manejador que responde siempre el mismo recurso de {@code respuestas/} y cuenta los pedidos. */
    public static HttpHandler responde(String recurso, AtomicInteger pedidos) throws IOException {
        byte[] b = FixturesSifen.recursoBytes("respuestas/" + recurso);
        return x -> {
            try (InputStream in = x.getRequestBody()) {
                in.readAllBytes();
            }
            pedidos.incrementAndGet();
            x.getResponseHeaders().add("Content-Type", "application/soap+xml; charset=utf-8");
            x.sendResponseHeaders(200, b.length);
            try (OutputStream out = x.getResponseBody()) {
                out.write(b);
            }
        };
    }

    /** Respuesta HTTP 200 completa con un cuerpo SOAP, para {@link Crudo}. */
    public static byte[] http200(String recurso) throws IOException {
        byte[] b = FixturesSifen.recursoBytes("respuestas/" + recurso);
        byte[] cab = ("HTTP/1.1 200 OK\r\nContent-Type: application/soap+xml; charset=utf-8\r\nContent-Length: " + b.length
                + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII);
        byte[] r = new byte[cab.length + b.length];
        System.arraycopy(cab, 0, r, 0, cab.length);
        System.arraycopy(b, 0, r, cab.length, b.length);
        return r;
    }

    /** Lee las cabeceras de un pedido HTTP (hasta la línea vacía) y, si hay {@code Content-Length}, el cuerpo. */
    public static String leerPedido(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        int c;
        int fin = 0;
        while ((c = in.read()) != -1) {
            b.write(c);
            fin = (c == '\r' || c == '\n') ? fin + 1 : 0;
            if (fin == 4) {
                break;
            }
        }
        String cabeceras = b.toString(StandardCharsets.ISO_8859_1);
        for (String l : cabeceras.split("\r\n")) {
            if (l.toLowerCase().startsWith("content-length:")) {
                int n = Integer.parseInt(l.substring(15).trim());
                byte[] cuerpo = in.readNBytes(n);
                return cabeceras + new String(cuerpo, StandardCharsets.UTF_8);
            }
        }
        return cabeceras;
    }

    /** Qué hace {@link Crudo} con la conexión número {@code n} (base 1). */
    public enum Accion { CERRAR, TARDAR, RESPONDER, RESPONDER_503, REDIRIGIR_302 }

    /** Servidor TCP plano con un comportamiento por conexión. */
    public static final class Crudo implements AutoCloseable {
        private final ServerSocket socket;
        private final AtomicInteger conexiones = new AtomicInteger();
        private final List<String> pedidos = Collections.synchronizedList(new ArrayList<>());
        private volatile IntFunction<Accion> plan;
        private volatile byte[] respuesta;
        private volatile long tardanzaMs = 2000;

        public Crudo(IntFunction<Accion> plan, byte[] respuesta) throws IOException {
            this.plan = plan;
            this.respuesta = respuesta;
            this.socket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
            Thread t = new Thread(this::atender, "fase5-crudo");
            t.setDaemon(true);
            t.start();
        }

        private void atender() {
            while (!socket.isClosed()) {
                try {
                    Socket s = socket.accept();
                    int n = conexiones.incrementAndGet();
                    Thread h = new Thread(() -> manejar(s, n), "fase5-crudo-" + n);
                    h.setDaemon(true);
                    h.start();
                } catch (IOException e) {
                    // cerrado
                }
            }
        }

        private void manejar(Socket s, int n) {
            try (Socket so = s) {
                Accion a = plan.apply(n);
                pedidos.add(leerPedido(so.getInputStream()));
                if (a == Accion.CERRAR) {
                    return;
                }
                if (a == Accion.TARDAR) {
                    Thread.sleep(tardanzaMs);
                }
                if (a == Accion.REDIRIGIR_302) {
                    so.getOutputStream().write(("HTTP/1.1 302 Found\r\nLocation: http://127.0.0.1:" + puerto()
                            + "/portal\r\nContent-Length: 0\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                    so.getOutputStream().flush();
                    return;
                }
                so.getOutputStream().write(a == Accion.RESPONDER_503
                        ? "HTTP/1.1 503 Service Unavailable\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                                .getBytes(StandardCharsets.US_ASCII)
                        : respuesta);
                so.getOutputStream().flush();
            } catch (IOException | InterruptedException e) {
                // el cliente cortó
            }
        }

        public int puerto() {
            return socket.getLocalPort();
        }

        public int conexiones() {
            return conexiones.get();
        }

        public List<String> pedidos() {
            return pedidos;
        }

        public void plan(IntFunction<Accion> p) {
            this.plan = p;
        }

        public void tardanza(long ms) {
            this.tardanzaMs = ms;
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }

    /** Proxy HTTP local: {@code CONNECT} (túnel) y pedidos absolutos {@code http://}, con usuario opcional. */
    public static final class ProxyLocal implements AutoCloseable {
        private final ServerSocket socket;
        private final String credencial;
        private final List<String> lineas = Collections.synchronizedList(new ArrayList<>());
        private final AtomicInteger rechazos407 = new AtomicInteger();

        /** @param usuario {@code null} = sin autenticación. */
        public ProxyLocal(String usuario, String clave) throws IOException {
            this.credencial = usuario == null ? null
                    : "Basic " + Base64.getEncoder().encodeToString((usuario + ":" + clave).getBytes(StandardCharsets.UTF_8));
            this.socket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
            Thread t = new Thread(this::atender, "fase5-proxy");
            t.setDaemon(true);
            t.start();
        }

        private void atender() {
            while (!socket.isClosed()) {
                try {
                    Socket s = socket.accept();
                    Thread h = new Thread(() -> manejar(s), "fase5-proxy-conexion");
                    h.setDaemon(true);
                    h.start();
                } catch (IOException e) {
                    // cerrado
                }
            }
        }

        private void manejar(Socket cliente) {
            try (Socket c = cliente) {
                InputStream in = c.getInputStream();
                String cabeceras = leerCabeceras(in);
                if (!cabeceras.contains("\r\n")) {
                    return; // solo abrió la conexión (la etapa PROXY del diagnóstico)
                }
                String primera = cabeceras.substring(0, cabeceras.indexOf("\r\n"));
                lineas.add(primera);
                if (credencial != null && !credencialCorrecta(cabeceras)) {
                    rechazos407.incrementAndGet();
                    c.getOutputStream().write(("HTTP/1.1 407 Proxy Authentication Required\r\n"
                            + "Proxy-Authenticate: Basic realm=\"fase5\"\r\nContent-Length: 0\r\nConnection: close\r\n\r\n")
                            .getBytes(StandardCharsets.US_ASCII));
                    return;
                }
                String[] partes = primera.split(" ");
                if ("CONNECT".equals(partes[0])) {
                    String[] hp = partes[1].split(":");
                    try (Socket destino = new Socket(hp[0], Integer.parseInt(hp[1]))) {
                        c.getOutputStream().write("HTTP/1.1 200 Connection established\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                        tubos(c, destino);
                    }
                } else {
                    java.net.URI u = java.net.URI.create(partes[1]);
                    try (Socket destino = new Socket(u.getHost(), u.getPort())) {
                        // como un proxy real: Proxy-Authorization es de este salto y no pasa al destino
                        String reescrita = partes[0] + " " + u.getRawPath() + " " + partes[2]
                                + cabeceras.substring(cabeceras.indexOf("\r\n")).replaceAll("(?i)\r\nProxy-Authorization:[^\r]*", "");
                        destino.getOutputStream().write(reescrita.getBytes(StandardCharsets.ISO_8859_1));
                        tubos(c, destino);
                    }
                }
            } catch (IOException e) {
                // corte
            }
        }

        /** M5-06: el nombre de la cabecera sin distinguir mayúsculas (en el CONNECT el JDK manda «Proxy-authorization»). */
        private boolean credencialCorrecta(String cabeceras) {
            for (String l : cabeceras.split("\r\n")) {
                int dos = l.indexOf(':');
                if (dos > 0 && l.substring(0, dos).trim().equalsIgnoreCase("Proxy-Authorization")
                        && l.substring(dos + 1).trim().equals(credencial)) {
                    return true;
                }
            }
            return false;
        }

        /** Cantidad de {@code CONNECT} que recibió. */
        public long connects() {
            return lineas.stream().filter(x -> x.startsWith("CONNECT ")).count();
        }

        private static String leerCabeceras(InputStream in) throws IOException {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            int c;
            int fin = 0;
            while ((c = in.read()) != -1) {
                b.write(c);
                fin = (c == '\r' || c == '\n') ? fin + 1 : 0;
                if (fin == 4) {
                    break;
                }
            }
            return b.toString(StandardCharsets.ISO_8859_1);
        }

        /** Copia en los dos sentidos hasta que uno cierra. */
        private static void tubos(Socket a, Socket b) throws IOException {
            Thread ida = new Thread(() -> copiar(a, b), "fase5-proxy-ida");
            ida.setDaemon(true);
            ida.start();
            copiar(b, a);
            try {
                ida.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private static void copiar(Socket desde, Socket hacia) {
            try {
                desde.getInputStream().transferTo(hacia.getOutputStream());
                hacia.shutdownOutput();
            } catch (IOException e) {
                // corte
            }
        }

        public int puerto() {
            return socket.getLocalPort();
        }

        public List<String> lineas() {
            return lineas;
        }

        public int rechazos407() {
            return rechazos407.get();
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}
