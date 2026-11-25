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
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.javabeanstack.sifen.api.ConnectionDiagnosis;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Diagnóstico de conexión por etapas (plan JBSSIF, §5.3; RF10, M0-09, CA5-06 en la parte de la API)
 * contra servidores locales y nombres {@code *.invalid} (el resolvedor de pruebas no consulta la red):
 * cada etapa informa resultado y duración, la primera falla corta las siguientes y nunca lanza.
 */
public class ConnectionProbeTest {
    private static HttpsServer servidor;
    private static final AtomicInteger PEDIDOS = new AtomicInteger();

    @BeforeAll
    public static void levantar() throws Exception {
        ServidoresFase5.confiarEnElAutofirmado();
        servidor = ServidoresFase5.https(0, ServidoresFase5.responde("consulta-ruc-0502.xml", PEDIDOS));
    }

    @AfterAll
    public static void bajar() {
        servidor.stop(0);
        ServidoresFase5.restaurar();
    }

    private static SifenConfig cfg() {
        SifenConfig c = FixturesSifen.configuracion();
        c.setBaseUrl("https://127.0.0.1:" + servidor.getAddress().getPort());
        return c;
    }

    private static List<String> nombres(ConnectionDiagnosis d) {
        return d.getSteps().stream().map(ConnectionDiagnosis.Step::getName).toList();
    }

    @Test
    public void todoBienPorEtapas() {
        ConnectionDiagnosis d = ConnectionProbe.diagnose(cfg(), SifenConnection.create(), "0badcafe");
        Assertions.assertTrue(d.isOk(), d.toString());
        Assertions.assertEquals(List.of("CONFIG", "DNS", "TCP", "TLS", "HTTP"), nombres(d));
        Assertions.assertEquals("0badcafe", d.getOperationId());
        Assertions.assertTrue(d.getSteps().stream().allMatch(s -> s.getDurationMs() >= 0 && s.getCode().isEmpty()), d.toString());
    }

    @Test
    public void nombreQueNoSeResuelve() {
        SifenConfig c = cfg();
        c.setBaseUrl("https://sifen-diagnostico.invalid");
        ConnectionDiagnosis d = ConnectionProbe.diagnose(c, null, "1");
        Assertions.assertFalse(d.isOk());
        Assertions.assertEquals(List.of("CONFIG", "DNS"), nombres(d));
        Assertions.assertEquals("JBS-CONEX-01", ConnectionProbe.firstFailure(d).orElseThrow());
        Assertions.assertTrue(d.getSteps().get(1).getMessage().contains("sifen-diagnostico.invalid"), d.toString());
    }

    @Test
    public void puertoCerrado() throws Exception {
        int puerto;
        try (ServerSocket s = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
            puerto = s.getLocalPort();
        }
        SifenConfig c = cfg();
        c.setBaseUrl("https://127.0.0.1:" + puerto);
        ConnectionDiagnosis d = ConnectionProbe.diagnose(c, null, "2");
        Assertions.assertEquals(List.of("CONFIG", "DNS", "TCP"), nombres(d));
        Assertions.assertEquals("JBS-CONEX-03", ConnectionProbe.firstFailure(d).orElseThrow());
    }

    @Test
    public void certificadoQueNoExiste() {
        SifenConfig c = cfg();
        c.setClientCertificate("/no/existe/empresa.pfx");
        ConnectionDiagnosis d = ConnectionProbe.diagnose(c, SifenConnection.create(), "3");
        Assertions.assertEquals(List.of("CONFIG", "DNS", "TCP", "TLS"), nombres(d));
        Assertions.assertEquals("JBS-CERT-01", ConnectionProbe.firstFailure(d).orElseThrow());
    }

    @Test
    public void porElProxyDeLaConfiguracion() throws Exception {
        try (ServidoresFase5.ProxyLocal proxy = new ServidoresFase5.ProxyLocal(null, null)) {
            SifenConfig c = cfg();
            c.setProxyHost("127.0.0.1");
            c.setProxyPort(proxy.puerto());
            ConnectionDiagnosis d = ConnectionProbe.diagnose(c, null, "4");
            Assertions.assertTrue(d.isOk(), d.toString());
            Assertions.assertEquals(List.of("CONFIG", "PROXY", "HTTP"), nombres(d));
            Assertions.assertTrue(proxy.lineas().stream().anyMatch(l -> l.startsWith("CONNECT ")), proxy.lineas().toString());
        }
    }

    /** E5-04: con el usuario del proxy el diagnóstico hace lo mismo que el pedido real. */
    @Test
    public void porUnProxyConUsuarioComoElPedido() throws Exception {
        try (ServidoresFase5.ProxyLocal proxy = new ServidoresFase5.ProxyLocal("usuario-proxy", "clave");
                ServidoresFase5.Crudo destino = new ServidoresFase5.Crudo(n -> ServidoresFase5.Accion.RESPONDER,
                        ServidoresFase5.http200("consulta-ruc-0502.xml"))) {
            SifenConfig c = cfg();
            c.setBaseUrl("http://127.0.0.1:" + destino.puerto());
            c.setProxyHost("127.0.0.1");
            c.setProxyPort(proxy.puerto());
            c.setProxyUser("usuario-proxy");
            c.setProxyPassword("clave");
            ConnectionDiagnosis d = ConnectionProbe.diagnose(c, null, "8");
            Assertions.assertTrue(d.isOk(), "antes: HTTP JBS-CONEX-02 aunque el pedido pasaba: " + d);
            Assertions.assertEquals(List.of("CONFIG", "PROXY", "HTTP"), nombres(d));
            Assertions.assertEquals(0, proxy.rechazos407());
            c.setProxyPassword("otra");
            ConnectionDiagnosis mala = ConnectionProbe.diagnose(c, null, "9");
            // §10.7 (I10-22): la credencial rechazada da el mismo código que la operación
            Assertions.assertEquals("JBS-CONEX-11", ConnectionProbe.firstFailure(mala).orElseThrow(), mala.toString());
            Assertions.assertEquals("HTTP", mala.getSteps().get(mala.getSteps().size() - 1).getName());
        }
    }

    @Test
    public void proxyDeLaJvmQueNoResponde() throws Exception {
        int puerto;
        try (ServerSocket s = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
            puerto = s.getLocalPort();
        }
        java.net.ProxySelector original = java.net.ProxySelector.getDefault();
        java.net.ProxySelector.setDefault(SifenConnectionTest.selectorHacia(puerto));
        try {
            ConnectionDiagnosis d = ConnectionProbe.diagnose(cfg(), null, "10");
            Assertions.assertEquals(List.of("CONFIG", "PROXY"), nombres(d));
            Assertions.assertEquals("JBS-CONEX-02", ConnectionProbe.firstFailure(d).orElseThrow());
        } finally {
            java.net.ProxySelector.setDefault(original);
        }
    }

    @Test
    public void proxyQueNoResponde() throws Exception {
        int puerto;
        try (ServerSocket s = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
            puerto = s.getLocalPort();
        }
        SifenConfig c = cfg();
        c.setProxyHost("127.0.0.1");
        c.setProxyPort(puerto);
        ConnectionDiagnosis d = ConnectionProbe.diagnose(c, null, "5");
        Assertions.assertEquals(List.of("CONFIG", "PROXY"), nombres(d));
        Assertions.assertEquals("JBS-CONEX-02", ConnectionProbe.firstFailure(d).orElseThrow());
    }

    @Test
    public void sinConfiguracionNoLanza() {
        ConnectionDiagnosis d = ConnectionProbe.diagnose(null, null, "6");
        Assertions.assertFalse(d.isOk());
        Assertions.assertEquals("JBS-CFG-01", ConnectionProbe.firstFailure(d).orElseThrow());
    }

    @Test
    public void urlConOtroProtocolo() {
        SifenConfig c = cfg();
        c.setBaseUrl("ftp://127.0.0.1");
        ConnectionDiagnosis d = ConnectionProbe.diagnose(c, null, "7");
        Assertions.assertEquals("JBS-CFG-04", ConnectionProbe.firstFailure(d).orElseThrow());
    }
}
