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

import com.sun.net.httpserver.HttpsServer;
import jakarta.xml.soap.SOAPMessage;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLContext;
import javax.xml.namespace.QName;
import org.javabeanstack.sifen.ISifenClient;
import org.javabeanstack.sifen.SifenClient;
import org.javabeanstack.sifen.api.RucQueryResult;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.exceptions.Operation;
import org.javabeanstack.sifen.core.exceptions.SifenConnectionException;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.internal.Constants;
import org.javabeanstack.sifen.internal.SOAPResponse;
import org.javabeanstack.sifen.internal.helpers.SSLContextHelper;
import org.javabeanstack.sifen.internal.helpers.SoapHelper;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Transporte por instancia de la Fase 5 (plan JBSSIF, §5.2; CA5-03, CA5-04; E0-05, E0-10, E3-31,
 * M0-04, M0-14, M3-64, E5-01) contra servidores locales: caída y vuelta del servidor, socket cerrado
 * sin respuesta, cambio del certificado de cliente en caliente, configuración sin certificado, tiempo
 * de lectura, proxy con y sin usuario, {@code http://}. Todo sin reiniciar la JVM y sin red.
 */
public class SifenConnectionTest {
    @TempDir
    static Path tmp;

    @BeforeAll
    public static void confiar() {
        ServidoresFase5.confiarEnElAutofirmado();
    }

    @AfterAll
    public static void restaurar() {
        ServidoresFase5.restaurar();
    }

    private static SOAPMessage mensaje() throws Exception {
        SOAPMessage m = SoapHelper.createSoapMessage();
        m.getSOAPBody().addBodyElement(new QName(Constants.SIFEN_NS_URI, "rEnviConsRUC"))
                .addChildElement("dRUCCons").setTextContent("80123456");
        return m;
    }

    private static String https(HttpsServer s) {
        return "https://127.0.0.1:" + s.getAddress().getPort() + "/de/ws/consultas/consulta-ruc.wsdl";
    }

    private static int puertoLibre() throws Exception {
        try (ServerSocket s = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
            return s.getLocalPort();
        }
    }

    private static SifenException falla(SifenConnection c, SifenConfig cfg, String url) {
        return Assertions.assertThrows(SifenException.class, () -> c.exchange(cfg, url, mensaje()));
    }

    // ------------------------------------------------------------------ recuperación sin reinicio (CA5-03)

    @Test
    public void caidaYVueltaDelServidorSinReiniciar() throws Exception {
        AtomicInteger pedidos = new AtomicInteger();
        HttpsServer s = ServidoresFase5.https(0, ServidoresFase5.responde("consulta-ruc-0502.xml", pedidos));
        int puerto = s.getAddress().getPort();
        String url = https(s);
        SifenConfig cfg = FixturesSifen.configuracion();
        SifenConnection c = SifenConnection.create();
        Assertions.assertEquals(200, c.exchange(cfg, url, mensaje()).getStatus());
        Assertions.assertTrue(((UrlSifenConnection) c).hasTlsContext());

        s.stop(0);
        SifenException e = falla(c, cfg, url);
        Assertions.assertEquals("JBS-CONEX-03", e.getCode(), e.getTechnicalDetail());
        Assertions.assertEquals(SifenConnectionException.Type.TCP, ((SifenConnectionException) e).getType());
        Assertions.assertFalse(((UrlSifenConnection) c).hasTlsContext(), "la falla de transporte descarta el contexto TLS (E0-05)");

        HttpsServer vuelta = ServidoresFase5.https(puerto, ServidoresFase5.responde("consulta-ruc-0502.xml", pedidos));
        try {
            SOAPResponse r = c.exchange(cfg, url, mensaje());
            Assertions.assertEquals(200, r.getStatus());
            Assertions.assertTrue(new String(r.getRawData(), java.nio.charset.StandardCharsets.UTF_8).contains("0502"));
            Assertions.assertEquals(2, pedidos.get());
        } finally {
            vuelta.stop(0);
        }
    }

    @Test
    public void cambioDelCertificadoDeClienteEnCaliente() throws Exception {
        AtomicInteger pedidos = new AtomicInteger();
        HttpsServer s = ServidoresFase5.https(0, ServidoresFase5.responde("consulta-ruc-0502.xml", pedidos));
        try {
            Path pfx = tmp.resolve("empresa-caliente.pfx");
            Path otro = FixturesSifen.rutaPfx().getParent().resolve("jbs-sifen-sin-firma.p12");
            Files.copy(otro, pfx, StandardCopyOption.REPLACE_EXISTING);
            SifenConfig cfg = FixturesSifen.configuracion();
            cfg.setClientCertificate(pfx.toString());
            SifenConnection c = SifenConnection.create();
            // el servidor solo confía en el autofirmado: con el otro certificado el TLS mutuo no pasa
            SifenException e = falla(c, cfg, https(s));
            Assertions.assertTrue(Set.of("JBS-CONEX-06", "JBS-CONEX-07").contains(e.getCode()), e.getCode() + " " + e.getTechnicalDetail());
            Assertions.assertEquals(0, pedidos.get());

            // se reemplaza el archivo, sin tocar la configuración ni la conexión
            Files.copy(FixturesSifen.rutaPfx(), pfx, StandardCopyOption.REPLACE_EXISTING);
            Files.setLastModifiedTime(pfx, FileTime.fromMillis(System.currentTimeMillis() + 5000));
            Assertions.assertEquals(200, c.exchange(cfg, https(s), mensaje()).getStatus());
            Assertions.assertEquals(1, pedidos.get());

            // y al revés: el certificado nuevo se usa de verdad
            Files.copy(otro, pfx, StandardCopyOption.REPLACE_EXISTING);
            Files.setLastModifiedTime(pfx, FileTime.fromMillis(System.currentTimeMillis() + 10000));
            falla(c, cfg, https(s));
            Assertions.assertEquals(1, pedidos.get());
        } finally {
            s.stop(0);
        }
    }

    @Test
    public void certificadoBorradoEnCalienteYRepuesto() throws Exception {
        AtomicInteger pedidos = new AtomicInteger();
        HttpsServer s = ServidoresFase5.https(0, ServidoresFase5.responde("consulta-ruc-0502.xml", pedidos));
        try {
            Path pfx = FixturesSifen.copiarPfx(tmp.resolve("empresa-borrada.pfx"));
            SifenConfig cfg = FixturesSifen.configuracion();
            cfg.setClientCertificate(pfx.toString());
            SifenConnection c = SifenConnection.create();
            Assertions.assertEquals(200, c.exchange(cfg, https(s), mensaje()).getStatus());
            Files.delete(pfx);
            Assertions.assertEquals("JBS-CERT-01", falla(c, cfg, https(s)).getCode());
            FixturesSifen.copiarPfx(pfx);
            Assertions.assertEquals(200, c.exchange(cfg, https(s), mensaje()).getStatus());
        } finally {
            s.stop(0);
        }
    }

    @Test
    public void sinCertificadoNoUsaElContextoDeOtraConfiguracionE331() throws Exception {
        AtomicInteger pedidos = new AtomicInteger();
        HttpsServer s = ServidoresFase5.https(0, ServidoresFase5.responde("consulta-ruc-0502.xml", pedidos));
        try {
            SifenConfig conCertificado = FixturesSifen.configuracion();
            SifenConfig sinCertificado = conCertificado.copy();
            sinCertificado.setUseClientCertificate(false);
            Assertions.assertEquals(200, SoapHelper.makeSoapRequest(conCertificado, https(s), mensaje()).getStatus());
            // la línea base devolvía acá el contexto estático de la otra configuración y el pedido pasaba
            SifenException e = Assertions.assertThrows(SifenException.class,
                    () -> SoapHelper.makeSoapRequest(sinCertificado, https(s), mensaje()));
            Assertions.assertTrue(Set.of("JBS-CONEX-06", "JBS-CONEX-07").contains(e.getCode()), e.getCode());
            Assertions.assertNotSame(SSLContextHelper.getContextFromConfig(conCertificado),
                    SSLContextHelper.getContextFromConfig(sinCertificado));
            Assertions.assertSame(SSLContext.getDefault(), SSLContextHelper.getContextFromConfig(sinCertificado));
            Assertions.assertEquals(1, pedidos.get());
            Assertions.assertEquals(200, SoapHelper.makeSoapRequest(conCertificado, https(s), mensaje()).getStatus());
        } finally {
            s.stop(0);
        }
    }

    @Test
    public void unaConexionPorInstanciaDeConfiguracion() {
        SifenConfig a = FixturesSifen.configuracion();
        SifenConfig copia = a.copy();
        Assertions.assertSame(SifenConnection.of(a), SifenConnection.of(a));
        Assertions.assertNotSame(SifenConnection.of(a), SifenConnection.of(copia), "identidad, no igualdad (E3-31)");
        Assertions.assertNotSame(SifenConnection.create(), SifenConnection.create());
        SifenConnection vieja = SifenConnection.of(a);
        SifenConnection.release(a);
        Assertions.assertNotSame(vieja, SifenConnection.of(a), "release olvida la conexión de la instancia");
    }

    @Test
    public void sifenConfigNoRedefineLaIgualdadQueUsaElMapaDebil() throws Exception {
        Assertions.assertEquals(Object.class, SifenConfig.class.getMethod("equals", Object.class).getDeclaringClass());
        Assertions.assertEquals(Object.class, SifenConfig.class.getMethod("hashCode").getDeclaringClass());
    }

    @Test
    public void cerrarLiberaYUnPedidoPosteriorVuelveAArmar() throws Exception {
        AtomicInteger pedidos = new AtomicInteger();
        HttpsServer s = ServidoresFase5.https(0, ServidoresFase5.responde("consulta-ruc-0502.xml", pedidos));
        try {
            SifenConfig cfg = FixturesSifen.configuracion();
            SifenConnection c = SifenConnection.create();
            c.exchange(cfg, https(s), mensaje());
            c.close();
            c.close();
            Assertions.assertFalse(((UrlSifenConnection) c).hasTlsContext());
            Assertions.assertEquals(200, c.exchange(cfg, https(s), mensaje()).getStatus());
        } finally {
            s.stop(0);
        }
    }

    // ------------------------------------------------------------------ socket cerrado y E5-01

    @Test
    public void socketCerradoSinRespuestaUnaSolaConexionE501() throws Exception {
        try (ServidoresFase5.Crudo crudo = new ServidoresFase5.Crudo(n -> ServidoresFase5.Accion.CERRAR,
                ServidoresFase5.http200("consulta-ruc-0502.xml"))) {
            SifenConfig cfg = FixturesSifen.configuracion();
            SifenException e = falla(SifenConnection.create(), cfg, "http://127.0.0.1:" + crudo.puerto() + "/x");
            Assertions.assertEquals("JBS-CONEX-07", e.getCode(), e.getTechnicalDetail());
            Assertions.assertTrue(e.withContext(Operation.SEND_DE, null, "x").isOutcomeUnknown(),
                    "el pedido salió: en una recepción el resultado es desconocido (I3-48)");
            Thread.sleep(200);
            // sin el envío de largo fijo el JDK reenvía el POST por su cuenta (sun.net.http.retryPost)
            Assertions.assertEquals(1, crudo.conexiones(), "el transporte no reenvía (D41, E5-01)");
        }
    }

    @Test
    public void httpPlanoContraUnServidorRealE010() throws Exception {
        try (ServidoresFase5.Crudo crudo = new ServidoresFase5.Crudo(n -> ServidoresFase5.Accion.RESPONDER,
                ServidoresFase5.http200("consulta-ruc-0502.xml"))) {
            SOAPResponse r = SifenConnection.create().exchange(FixturesSifen.configuracion(),
                    "http://127.0.0.1:" + crudo.puerto() + "/de/ws/consultas/consulta-ruc.wsdl", mensaje());
            Assertions.assertEquals(200, r.getStatus());
            Assertions.assertNotNull(r.getSoapResponse());
            Assertions.assertTrue(crudo.pedidos().get(0).contains("Content-Length:"), crudo.pedidos().get(0));
            Assertions.assertTrue(crudo.pedidos().get(0).contains("80123456"));
        }
    }

    /** Un portal cautivo que redirige: {@code JBS-CONEX-10} (no SOAP), una sola conexión y sin seguirla. */
    @Test
    public void redireccionDeUnPortalEsJbsConex10() throws Exception {
        try (ServidoresFase5.Crudo crudo = new ServidoresFase5.Crudo(n -> ServidoresFase5.Accion.REDIRIGIR_302,
                ServidoresFase5.http200("consulta-ruc-0502.xml"))) {
            SifenException e = falla(SifenConnection.create(), FixturesSifen.configuracion(), "http://127.0.0.1:" + crudo.puerto() + "/x");
            Assertions.assertEquals("JBS-CONEX-10", e.getCode(), e.getTechnicalDetail());
            Assertions.assertTrue(e.getUserMessage().contains("HTTP 302"), e.getUserMessage());
            Thread.sleep(200);
            Assertions.assertEquals(1, crudo.conexiones(), "no se sigue la redirección");
        }
    }

    // ------------------------------------------------------------------ tiempos de espera (CA5-04)

    @Test
    public void tiempoDeLecturaConfigurableSinRecrearNada() throws Exception {
        try (ServidoresFase5.Crudo crudo = new ServidoresFase5.Crudo(n -> ServidoresFase5.Accion.TARDAR,
                ServidoresFase5.http200("consulta-ruc-0502.xml"))) {
            crudo.tardanza(1500);
            SifenConfig cfg = FixturesSifen.configuracion();
            cfg.setHttpReadTimeout(200);
            String url = "http://127.0.0.1:" + crudo.puerto() + "/x";
            SifenConnection c = SifenConnection.create();
            long t0 = System.nanoTime();
            SifenException e = falla(c, cfg, url);
            long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
            Assertions.assertEquals("JBS-CONEX-05", e.getCode());
            Assertions.assertEquals(SifenConnectionException.Type.READ_TIMEOUT, ((SifenConnectionException) e).getType());
            Assertions.assertTrue(ms < 1400, "cortó por el tiempo configurado: " + ms + " ms");
            cfg.setHttpReadTimeout(5000);
            Assertions.assertEquals(200, c.exchange(cfg, url, mensaje()).getStatus());
        }
    }

    @Test
    public void tiempoDeConexionConfigurable() throws Exception {
        SifenConfig cfg = FixturesSifen.configuracion();
        cfg.setHttpConnectTimeout(1234);
        Assertions.assertEquals(1234, cfg.getHttpConnectTimeout());
        Assertions.assertEquals(1234, cfg.copy().getHttpConnectTimeout());
    }

    // ------------------------------------------------------------------ proxy (CA5-04)

    @Test
    public void proxySinUsuarioPorConnect() throws Exception {
        AtomicInteger pedidos = new AtomicInteger();
        HttpsServer s = ServidoresFase5.https(0, ServidoresFase5.responde("consulta-ruc-0502.xml", pedidos));
        try (ServidoresFase5.ProxyLocal proxy = new ServidoresFase5.ProxyLocal(null, null)) {
            SifenConfig cfg = FixturesSifen.configuracion();
            cfg.setProxyHost("127.0.0.1");
            cfg.setProxyPort(proxy.puerto());
            Assertions.assertEquals(200, SifenConnection.create().exchange(cfg, https(s), mensaje()).getStatus());
            Assertions.assertEquals(1, pedidos.get());
            Assertions.assertTrue(proxy.lineas().get(0).startsWith("CONNECT 127.0.0.1:" + s.getAddress().getPort()),
                    proxy.lineas().toString());
        } finally {
            s.stop(0);
        }
    }

    @Test
    public void proxyConUsuarioDestinoHttp() throws Exception {
        try (ServidoresFase5.ProxyLocal proxy = new ServidoresFase5.ProxyLocal("usuario-proxy", "clave proxy");
                ServidoresFase5.Crudo crudo = new ServidoresFase5.Crudo(n -> ServidoresFase5.Accion.RESPONDER,
                        ServidoresFase5.http200("consulta-ruc-0502.xml"))) {
            SifenConfig cfg = FixturesSifen.configuracion();
            cfg.setProxyHost("127.0.0.1");
            cfg.setProxyPort(proxy.puerto());
            cfg.setProxyUser("usuario-proxy");
            cfg.setProxyPassword("clave proxy");
            String url = "http://127.0.0.1:" + crudo.puerto() + "/de/ws/consultas/consulta-ruc.wsdl";
            Assertions.assertEquals(200, SifenConnection.create().exchange(cfg, url, mensaje()).getStatus());
            Assertions.assertEquals(0, proxy.rechazos407());
            Assertions.assertTrue(proxy.lineas().get(0).startsWith("POST http://127.0.0.1:" + crudo.puerto()), proxy.lineas().toString());
        }
    }

    /**
     * Con destino {@code https://} el JDK no manda {@code Basic} en el {@code CONNECT}
     * ({@code jdk.http.auth.tunneling.disabledSchemes=Basic} en {@code conf/net.properties} del JDK 25,
     * leída al arrancar la JVM): el proxy responde 407 y la falla sale como {@code JBS-CONEX-11} (desde §10.7, I5-09) con el
     * proxy en el mensaje (I5-04, M5-01).
     */
    @Test
    public void proxyConUsuarioDestinoHttpsLimiteDelJdk() throws Exception {
        AtomicInteger pedidos = new AtomicInteger();
        HttpsServer s = ServidoresFase5.https(0, ServidoresFase5.responde("consulta-ruc-0502.xml", pedidos));
        try (ServidoresFase5.ProxyLocal proxy = new ServidoresFase5.ProxyLocal("usuario-proxy", "clave")) {
            SifenConfig cfg = FixturesSifen.configuracion();
            cfg.setProxyHost("127.0.0.1");
            cfg.setProxyPort(proxy.puerto());
            cfg.setProxyUser("usuario-proxy");
            cfg.setProxyPassword("clave");
            SifenException e = falla(SifenConnection.create(), cfg, https(s));
            // §10.7 (I5-09): con credencial configurada el 407 del túnel tiene su código (antes JBS-CONEX-02)
            Assertions.assertEquals("JBS-CONEX-11", e.getCode(), e.getTechnicalDetail());
            Assertions.assertEquals(SifenConnectionException.Type.PROXY, ((SifenConnectionException) e).getType());
            Assertions.assertTrue(proxy.rechazos407() >= 1);
            Assertions.assertEquals(0, pedidos.get());
            Assertions.assertFalse(e.withContext(Operation.SEND_DE, null, "x").isOutcomeUnknown(), "no pasó del proxy");
        } finally {
            s.stop(0);
        }
    }

    @Test
    public void proxyQuePideUsuarioYNoLoTieneDestinoHttp() throws Exception {
        try (ServidoresFase5.ProxyLocal proxy = new ServidoresFase5.ProxyLocal("usuario-proxy", "clave");
                ServidoresFase5.Crudo crudo = new ServidoresFase5.Crudo(n -> ServidoresFase5.Accion.RESPONDER,
                        ServidoresFase5.http200("consulta-ruc-0502.xml"))) {
            SifenConfig cfg = FixturesSifen.configuracion();
            cfg.setProxyHost("127.0.0.1");
            cfg.setProxyPort(proxy.puerto());
            SifenException e = falla(SifenConnection.create(), cfg, "http://127.0.0.1:" + crudo.puerto() + "/x");
            Assertions.assertEquals("JBS-CONEX-02", e.getCode(), e.getTechnicalDetail());
            Assertions.assertFalse(e.withContext(Operation.SEND_DE, null, "x").isOutcomeUnknown(), "un 407 no llegó a SIFEN");
            Assertions.assertTrue(e.getUserMessage().contains("proxy"), e.getUserMessage());
            Assertions.assertEquals(0, crudo.conexiones());
        }
    }

    @Test
    public void proxyQueNoRespondeEsJbsConex02() throws Exception {
        SifenConfig cfg = FixturesSifen.configuracion();
        cfg.setProxyHost("127.0.0.1");
        cfg.setProxyPort(puertoLibre());
        SifenException e = falla(SifenConnection.create(), cfg, "https://127.0.0.1:9/x");
        Assertions.assertEquals("JBS-CONEX-02", e.getCode(), "sin el proxy de la configuración salía JBS-CONEX-03: " + e.getTechnicalDetail());
        Assertions.assertEquals(SifenConnectionException.Type.PROXY, ((SifenConnectionException) e).getType());
        Assertions.assertTrue(e.getTechnicalDetail().contains("proxy 127.0.0.1:" + cfg.getProxyPort()), e.getTechnicalDetail());
    }

    /** Selector de la JVM que manda todo por un proxy (como {@code https.proxyHost} o {@code net.properties}). */
    static java.net.ProxySelector selectorHacia(int puerto) {
        return new java.net.ProxySelector() {
            @Override
            public List<java.net.Proxy> select(java.net.URI uri) {
                return List.of(new java.net.Proxy(java.net.Proxy.Type.HTTP, new java.net.InetSocketAddress("127.0.0.1", puerto)));
            }

            @Override
            public void connectFailed(java.net.URI uri, java.net.SocketAddress sa, java.io.IOException ioe) {
            }
        };
    }

    /** E5-03 (caso del proxy caído en un cliente): el proxy de la JVM caído es {@code JBS-CONEX-02} y el error lo nombra, no culpa a SIFEN. */
    @Test
    public void proxyDeLaJvmCaidoEsJbsConex02YSeNombra() throws Exception {
        int puerto = puertoLibre();
        java.net.ProxySelector original = java.net.ProxySelector.getDefault();
        java.net.ProxySelector.setDefault(selectorHacia(puerto));
        try {
            SifenException e = falla(SifenConnection.create(), FixturesSifen.configuracion(), "https://127.0.0.1:9/x");
            Assertions.assertEquals("JBS-CONEX-02", e.getCode(), "antes: JBS-CONEX-03 contra SIFEN: " + e.getTechnicalDetail());
            Assertions.assertEquals(SifenConnectionException.Type.PROXY, ((SifenConnectionException) e).getType());
            Assertions.assertTrue(e.getTechnicalDetail().contains("proxy 127.0.0.1:" + puerto + " (de la JVM)"), e.getTechnicalDetail());
            Assertions.assertFalse(Retries.isRetryable(e), "una consulta no se reintenta contra un proxy caído");
        } finally {
            java.net.ProxySelector.setDefault(original);
        }
    }

    @Test
    public void proxyConNombreQueNoSeResuelveEsJbsConex02() throws Exception {
        SifenConfig cfg = FixturesSifen.configuracion();
        cfg.setProxyHost("proxy-inexistente.invalid");
        cfg.setProxyPort(3128);
        SifenException e = falla(SifenConnection.create(), cfg, "https://127.0.0.1:9/x");
        Assertions.assertEquals("JBS-CONEX-02", e.getCode(), e.getTechnicalDetail());
    }

    @Test
    public void proxySinPuertoEsJbsCfg10() throws Exception {
        SifenConfig cfg = FixturesSifen.configuracion();
        cfg.setProxyHost("127.0.0.1");
        SifenException e = falla(SifenConnection.create(), cfg, "https://127.0.0.1:9/x");
        Assertions.assertEquals("JBS-CFG-10", e.getCode());
        Assertions.assertTrue(e.getUserMessage().contains(SifenConfig.PROXY_PORT_KEY), e.getUserMessage());
    }

    // ------------------------------------------------------------------ varias empresas a la vez (M3-64)

    @Test
    public void dosConexionesEnParaleloSinContextoCompartido() throws Exception {
        AtomicInteger pedidos = new AtomicInteger();
        HttpsServer s = ServidoresFase5.https(0, ServidoresFase5.responde("consulta-ruc-0502.xml", pedidos));
        ExecutorService hilos = Executors.newFixedThreadPool(8);
        try {
            Path pfxA = FixturesSifen.copiarPfx(tmp.resolve("empresa-a.pfx"));
            Path pfxB = FixturesSifen.copiarPfx(tmp.resolve("empresa-b.pfx"));
            SifenConfig a = FixturesSifen.configuracion();
            a.setClientCertificate(pfxA.toString());
            SifenConfig b = FixturesSifen.configuracion();
            b.setClientCertificate(pfxB.toString());
            SifenConnection ca = SifenConnection.create();
            SifenConnection cb = SifenConnection.create();
            List<Future<Integer>> f = new ArrayList<>();
            for (int i = 0; i < 40; i++) {
                SifenConnection c = i % 2 == 0 ? ca : cb;
                SifenConfig cfg = i % 2 == 0 ? a : b;
                f.add(hilos.submit(() -> c.exchange(cfg, https(s), mensaje()).getStatus()));
            }
            for (Future<Integer> x : f) {
                Assertions.assertEquals(200, x.get(30, TimeUnit.SECONDS));
            }
            Assertions.assertNotSame(ca.tlsContext(a), cb.tlsContext(b));
            Assertions.assertEquals(40, pedidos.get());
        } finally {
            hilos.shutdownNow();
            s.stop(0);
        }
    }

    // ------------------------------------------------------------------ por la fachada nueva

    @Test
    public void clienteSeRecuperaDeLaCaidaDelServidor() throws Exception {
        AtomicInteger pedidos = new AtomicInteger();
        HttpsServer s = ServidoresFase5.https(0, ServidoresFase5.responde("consulta-ruc-0502.xml", pedidos));
        int puerto = s.getAddress().getPort();
        SifenConfig cfg = FixturesSifen.configuracion();
        cfg.setBaseUrl("https://127.0.0.1:" + puerto);
        try (ISifenClient cliente = SifenClient.create(cfg)) {
            RucQueryResult r = cliente.queryRuc("80123456");
            Assertions.assertTrue(r.isFound());
            s.stop(0);
            SifenException e = Assertions.assertThrows(SifenException.class, () -> cliente.queryRuc("80123456"));
            Assertions.assertEquals("JBS-CONEX-03", e.getCode());
            s = ServidoresFase5.https(puerto, ServidoresFase5.responde("consulta-ruc-0502.xml", pedidos));
            Assertions.assertTrue(cliente.queryRuc("80123456").isFound(), "sin recrear el cliente ni reiniciar");
        } finally {
            s.stop(0);
        }
    }
}
