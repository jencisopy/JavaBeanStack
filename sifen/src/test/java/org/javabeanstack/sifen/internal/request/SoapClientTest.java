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
package org.javabeanstack.sifen.internal.request;

import org.javabeanstack.sifen.Sifen;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.exceptions.ErrorCategory;
import org.javabeanstack.sifen.core.exceptions.SifenConnectionException;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.validation.SifenValidationException;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.beans.response.RespuestaConsultaDE;
import org.javabeanstack.sifen.core.beans.response.RespuestaConsultaLoteDE;
import org.javabeanstack.sifen.core.beans.response.RespuestaConsultaRUC;
import org.javabeanstack.sifen.core.beans.response.RespuestaRecepcionDE;
import org.javabeanstack.sifen.core.beans.response.RespuestaRecepcionEvento;
import org.javabeanstack.sifen.core.beans.response.RespuestaRecepcionLoteDE;
import org.javabeanstack.sifen.internal.http.SifenConnection;
import org.javabeanstack.sifen.internal.util.SifenUtil;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.javabeanstack.sifen.test.casos.XmlCanonico;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cliente SOAP de la librería contra un servidor HTTPS embebido del JDK que responde lo grabado en
 * {@code src/test/resources/respuestas/} (plan JBSSIF, §2.6). Sin red: el servidor escucha en
 * {@code 127.0.0.1} con un puerto efímero; nunca se contacta SIFEN (D28).
 *
 * <p>Por qué HTTPS y no {@code http://}: en la línea base {@code SoapHelper.makeSoapRequest}
 * casteaba a {@code HttpsURLConnection} antes de mirar el protocolo y <b>todo</b> {@code http://}
 * fallaba con {@code ClassCastException} (E0-10, I2-04); desde §3.7 la configuración lo rechaza
 * ({@code JBS-CFG-04}) y desde §3.8 el cast ya no existe; el test por {@code http://} queda
 * {@code @Disabled} hasta la Fase 5. El servidor usa el certificado autofirmado del módulo y exige
 * certificado de cliente (TLS mutuo, como SIFEN); el cliente confía en él por el almacén de
 * confianza por omisión ({@code javax.net.ssl.trustStore*}), que es lo que usa el
 * {@code TlsContext} de cada conexión (§5.2). Las propiedades y el verificador de nombre se restauran
 * al terminar y la conexión de la configuración se libera (I5-01).</p>
 */
public class SoapClientTest {
    private static HttpsServer servidor;
    private static SifenConfig cfg;
    private static final List<String> pedidos = Collections.synchronizedList(new ArrayList<>());
    private static final List<String> rutas = Collections.synchronizedList(new ArrayList<>());
    private static final List<String> agentes = Collections.synchronizedList(new ArrayList<>());
    private static volatile int estado = 200;
    private static volatile byte[] cuerpo = new byte[0];
    private static HostnameVerifier verificadorOriginal;
    private static final String[] PROPIEDADES = {"javax.net.ssl.trustStore", "javax.net.ssl.trustStorePassword", "javax.net.ssl.trustStoreType"};
    private static final String[] valoresOriginales = new String[PROPIEDADES.length];

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
                pedidos.add(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
            rutas.add(intercambio.getRequestURI().getPath());
            agentes.add(intercambio.getRequestHeaders().getFirst("User-Agent"));
            byte[] b = cuerpo;
            intercambio.getResponseHeaders().add("Content-Type", "application/soap+xml; charset=utf-8");
            intercambio.sendResponseHeaders(estado, b.length == 0 ? -1 : b.length);
            if (b.length > 0) {
                try (OutputStream out = intercambio.getResponseBody()) {
                    out.write(b);
                }
            }
            intercambio.close();
        });
        servidor.start();

        for (int i = 0; i < PROPIEDADES.length; i++) {
            valoresOriginales[i] = System.getProperty(PROPIEDADES[i]);
        }
        System.setProperty("javax.net.ssl.trustStore", FixturesSifen.rutaPfx().toString());
        System.setProperty("javax.net.ssl.trustStorePassword", FixturesSifen.PFX_CLAVE);
        System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
        verificadorOriginal = HttpsURLConnection.getDefaultHostnameVerifier();
        // El certificado autofirmado no tiene SAN de IP: se acepta solo 127.0.0.1.
        HttpsURLConnection.setDefaultHostnameVerifier((host, sesion) -> "127.0.0.1".equals(host));

        cfg = FixturesSifen.configuracion();
        cfg.setBaseUrl("https://127.0.0.1:" + servidor.getAddress().getPort());
    }

    @AfterAll
    public static void bajar() throws Exception {
        if (servidor != null) {
            servidor.stop(0);
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
        liberarConexiones(cfg);
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

    @BeforeEach
    public void limpiar() {
        pedidos.clear();
        rutas.clear();
        agentes.clear();
        estado = 200;
        cuerpo = new byte[0];
    }

    private static void responder(int codigo, String recurso) throws Exception {
        estado = codigo;
        cuerpo = recurso == null ? new byte[0] : FixturesSifen.recursoBytes("respuestas/" + recurso);
    }

    private static DocumentoElectronico factura() {
        return CasosDePrueba.documentos().get(4).construir();
    }

    // ------------------------------------------------------------------ consultas

    @Test
    public void consultaRucEncontrado() throws Exception {
        responder(200, "consulta-ruc-0502.xml");
        RespuestaConsultaRUC r = Sifen.consultaRUC("80123456", cfg);
        Assertions.assertEquals(200, r.getCodigoEstado());
        Assertions.assertEquals("0502", r.getdCodRes());
        Assertions.assertEquals("RUC encontrado", r.getdMsgRes());
        Assertions.assertEquals("EMPRESA FICTICIA DE PRUEBA S.A.", r.getxContRUC().getdRazCons());
        Assertions.assertEquals("S", r.getxContRUC().getdRUCFactElec());
        Assertions.assertEquals("/de/ws/consultas/consulta-ruc.wsdl", rutas.get(0));
        Assertions.assertEquals("80123456", XmlCanonico.texto(pedidos.get(0), "dRUCCons"));
        Assertions.assertTrue(agentes.get(0).startsWith("jbs-sifen/"), "User-Agent de D13: " + agentes.get(0));
        Assertions.assertTrue(r.getRespuestaBruta().contains("rResEnviConsRuc"));
    }

    @Test
    public void consultaRucInexistente() throws Exception {
        responder(200, "consulta-ruc-0500.xml");
        RespuestaConsultaRUC r = Sifen.consultaRUC("80000000", cfg);
        Assertions.assertEquals("0500", r.getdCodRes());
        Assertions.assertNull(r.getxContRUC());
    }

    @Test
    public void consultaDePorCdc() throws Exception {
        responder(200, "consulta-de-0422.xml");
        RespuestaConsultaDE r = Sifen.consultaDE(CasosDePrueba.CDC_FACTURA_SIMPLE, cfg);
        Assertions.assertEquals("0422", r.getdCodRes());
        Assertions.assertNotNull(r.getdFecProc());
        Assertions.assertEquals("1000000001", r.getxContenDE().getdProtAut(), "xContenDE escapado, desescapado por la librería");
        Assertions.assertEquals(CasosDePrueba.CDC_FACTURA_SIMPLE, XmlCanonico.texto(pedidos.get(0), "dCDC"));
    }

    @Test
    public void consultaLote() throws Exception {
        responder(200, "consulta-lote-0362.xml");
        RespuestaConsultaLoteDE r = Sifen.consultaLoteDE("1234567890123456", cfg);
        Assertions.assertEquals("0362", r.getdCodResLot());
        Assertions.assertEquals(1, r.getgResProcLoteList().size());
        Assertions.assertEquals("Aprobado", r.getgResProcLoteList().get(0).getdEstRes());
        Assertions.assertEquals("/de/ws/consultas/consulta-lote.wsdl", rutas.get(0));
    }

    // ------------------------------------------------------------------ recepciones (firman con el autofirmado)

    @Test
    public void recepcionDeAprobado() throws Exception {
        responder(200, "recepcion-de-0260.xml");
        RespuestaRecepcionDE r = Sifen.recepcionDE(factura(), cfg);
        Assertions.assertEquals(200, r.getCodigoEstado());
        Assertions.assertEquals("Aprobado", r.getxProtDE().getdEstRes());
        Assertions.assertEquals("0260", r.getxProtDE().getgResProc().get(0).getdCodRes());
        Assertions.assertEquals("/de/ws/sync/recibe.wsdl", rutas.get(0));
        // Lo enviado es el DE firmado: la firma valida con la librería.
        Assertions.assertTrue(Sifen.validarFirmaDEDesdeXml(pedidos.get(0)).isValido());
        Assertions.assertNotNull(r.getRequestSent());
    }

    @Test
    public void recepcionDeRechazado() throws Exception {
        responder(200, "recepcion-de-rechazo.xml");
        RespuestaRecepcionDE r = Sifen.recepcionDE(factura(), cfg);
        Assertions.assertEquals("Rechazado", r.getxProtDE().getdEstRes());
        Assertions.assertEquals("CDC duplicado", r.getxProtDE().getgResProc().get(0).getdMsgRes());
    }

    @Test
    public void recepcionLote() throws Exception {
        responder(200, "recepcion-lote-0300.xml");
        List<DocumentoElectronico> lote = new ArrayList<>();
        lote.add(factura());
        RespuestaRecepcionLoteDE r = Sifen.recepcionLoteDE(lote, cfg);
        Assertions.assertEquals("0300", r.getdCodRes());
        Assertions.assertEquals("1234567890123456", r.getdProtConsLote());
        Assertions.assertEquals("/de/ws/async/recibe-lote.wsdl", rutas.get(0));
        String contenido = MensajesSifen.contenidoLote(pedidos.get(0));
        Assertions.assertTrue(contenido.contains("rLoteDE"), "el zip del lote trae el rLoteDE");
    }

    @Test
    public void recepcionEvento() throws Exception {
        responder(200, "evento-0600.xml");
        RespuestaRecepcionEvento r = Sifen.recepcionEvento(CasosDePrueba.eventos().get(0).construir(), cfg);
        Assertions.assertEquals(1, r.getgResProcEVe().size());
        Assertions.assertEquals("Aprobado", r.getgResProcEVe().get(0).getdEstRes());
        Assertions.assertEquals("0600", r.getgResProcEVe().get(0).getgResProc().get(0).getdCodRes());
        Assertions.assertEquals("/de/ws/eventos/evento.wsdl", rutas.get(0));
    }

    // ------------------------------------------------------------------ con la validación previa activa (C:M3-43)

    /** La configuración por omisión de producción: validación previa activa (D20), contra el servidor embebido. */
    private static SifenConfig conValidacion() {
        SifenConfig c = FixturesSifen.configuracionConValidacion();
        c.setBaseUrl(cfg.getBaseUrl());
        return c;
    }

    /** DE-05 con las leyendas del ambiente de pruebas (Q14): válido para la validación previa. */
    private static DocumentoElectronico facturaValida() {
        DocumentoElectronico de = factura();
        de.getgDatGralOpe().getgEmis().setdNomEmi("DE generado en ambiente de prueba - sin valor comercial ni fiscal");
        de.getgDtipDE().getgCamItemList().get(0).setdDesProSer(
                "DOCUMENTO ELECTRÓNICO SIN VALOR COMERCIAL NI FISCAL - GENERADO EN AMBIENTE DE PRUEBA");
        return de;
    }

    @Test
    public void recepcionDeConValidacionActivaLlegaAlServidor() throws Exception {
        responder(200, "recepcion-de-0260.xml");
        RespuestaRecepcionDE r = Sifen.recepcionDE(facturaValida(), conValidacion());
        Assertions.assertEquals("0260", r.getxProtDE().getgResProc().get(0).getdCodRes());
        Assertions.assertEquals(1, pedidos.size(), "las tres etapas pasan y el DE se envía");
        Assertions.assertTrue(Sifen.validarFirmaDEDesdeXml(pedidos.get(0)).isValido());
    }

    @Test
    public void recepcionLoteConValidacionActivaPasaCoherenciaLote() throws Exception {
        responder(200, "recepcion-lote-0300.xml");
        List<DocumentoElectronico> lote = new ArrayList<>();
        lote.add(facturaValida());
        DocumentoElectronico segundo = facturaValida();
        segundo.getgTimb().setdNumDoc("0000105");
        lote.add(segundo);
        RespuestaRecepcionLoteDE r = Sifen.recepcionLoteDE(lote, conValidacion());
        Assertions.assertEquals("0300", r.getdCodRes());
        Assertions.assertEquals(1, pedidos.size());
        Assertions.assertTrue(MensajesSifen.contenidoLote(pedidos.get(0)).contains("rLoteDE"));
    }

    @Test
    public void loteConOtroEmisorSeRechazaEnLoteUnoSinConectar() throws Exception {
        responder(200, "recepcion-lote-0300.xml");
        List<DocumentoElectronico> lote = new ArrayList<>();
        lote.add(facturaValida());
        DocumentoElectronico otro = facturaValida();
        otro.getgTimb().setdNumDoc("0000106");
        otro.getgDatGralOpe().getgEmis().setdRucEm("80000005");
        otro.getgDatGralOpe().getgEmis().setdDVEmi(SifenUtil.generateDv("80000005"));
        lote.add(otro);
        SifenValidationException e = Assertions.assertThrows(SifenValidationException.class,
                () -> Sifen.recepcionLoteDE(lote, conValidacion()));
        Assertions.assertTrue(e.getResult().contains("JBS-CERT-11"), e.getResult().toString());
        Assertions.assertTrue(e.getResult().find("JBS-CERT-11").get(0).getPath().startsWith("lote[1]."),
                e.getResult().toString());
        Assertions.assertTrue(pedidos.isEmpty(), "rechazado antes de conectar");
    }

    @Test
    public void recepcionEventoConValidacionActivaLlegaAlServidor() throws Exception {
        responder(200, "evento-0600.xml");
        RespuestaRecepcionEvento r = Sifen.recepcionEvento(CasosDePrueba.eventos().get(0).construir(), conValidacion());
        Assertions.assertEquals("0600", r.getgResProcEVe().get(0).getgResProc().get(0).getdCodRes());
        Assertions.assertEquals(1, pedidos.size());
    }

    // ------------------------------------------------------------------ respuestas reales de SIFEN-test (§2.9, M2-06)

    /** El CDC de una respuesta real anonimizada conserva un DV válido y el emisor ficticio. */
    private static void cdcAnonimizadoValido(String cdc) {
        Assertions.assertEquals(44, cdc.length(), cdc);
        Assertions.assertEquals(CasosDePrueba.RUC_EMISOR + CasosDePrueba.DV_EMISOR, cdc.substring(2, 11), cdc);
        Assertions.assertEquals(SifenUtil.generateDv(cdc.substring(0, 43)), cdc.substring(43), "DV del CDC " + cdc);
    }

    @Test
    public void recepcionDeAprobadaReal() throws Exception {
        responder(200, "real-recepcion-de-0260.xml");
        RespuestaRecepcionDE r = Sifen.recepcionDE(factura(), cfg);
        Assertions.assertEquals("Aprobado", r.getxProtDE().getdEstRes());
        Assertions.assertEquals("50452538", r.getxProtDE().getdProtAut());
        Assertions.assertEquals("0260", r.getxProtDE().getgResProc().get(0).getdCodRes());
        Assertions.assertEquals("Autorización del DE satisfactoria", r.getxProtDE().getgResProc().get(0).getdMsgRes());
        Assertions.assertNotNull(r.getxProtDE().getdFecProc(), "fecha con zona horaria (-03:00)");
        cdcAnonimizadoValido(r.getxProtDE().getId());
    }

    @Test
    public void recepcionDeRechazadaReal1107() throws Exception {
        responder(200, "real-recepcion-de-1107.xml");
        RespuestaRecepcionDE r = Sifen.recepcionDE(factura(), cfg);
        Assertions.assertEquals("Rechazado", r.getxProtDE().getdEstRes());
        Assertions.assertNull(r.getxProtDE().getdProtAut());
        Assertions.assertEquals("1107", r.getxProtDE().getgResProc().get(0).getdCodRes());
        Assertions.assertTrue(r.getxProtDE().getgResProc().get(0).getdMsgRes().contains("inicio de vigencia del timbrado"));
        cdcAnonimizadoValido(r.getxProtDE().getId());
    }

    @Test
    public void recepcionLoteReal() throws Exception {
        responder(200, "real-recepcion-lote-0300.xml");
        List<DocumentoElectronico> lote = new ArrayList<>();
        lote.add(factura());
        RespuestaRecepcionLoteDE r = Sifen.recepcionLoteDE(lote, cfg);
        Assertions.assertEquals("0300", r.getdCodRes());
        Assertions.assertEquals("Lote recibido con éxito", r.getdMsgRes());
        Assertions.assertEquals("110724783112254838", r.getdProtConsLote());
        Assertions.assertEquals(0, r.getdTpoProces());
    }

    @Test
    public void consultaLoteReal0362Con1306() throws Exception {
        responder(200, "real-consulta-lote-0362.xml");
        RespuestaConsultaLoteDE r = Sifen.consultaLoteDE("110724783112254838", cfg);
        Assertions.assertEquals("0362", r.getdCodResLot());
        Assertions.assertEquals(8, r.getgResProcLoteList().size());
        int aprobados = 0;
        for (org.javabeanstack.sifen.core.fields.response.batch.TgResProcLote p : r.getgResProcLoteList()) {
            cdcAnonimizadoValido(p.getId());
            if ("Aprobado".equals(p.getdEstRes())) {
                aprobados++;
                Assertions.assertNotNull(p.getdProtAut());
            } else {
                Assertions.assertEquals("Rechazado", p.getdEstRes());
                Assertions.assertEquals("1306", p.getgResProc().get(0).getdCodRes());
            }
        }
        Assertions.assertEquals(7, aprobados);
    }

    // ------------------------------------------------------------------ errores

    /*
     * §3.8 (D33 Parte C, ajuste de app-developer): la línea base devolvía una respuesta vacía
     * (dCodRes nulo) ante un Fault, un 500 no SOAP o un cuerpo vacío, y una NPE ante la respuesta
     * indentada (E2-04). Desde §3.8 los tres primeros son SifenException con código y la indentada se
     * interpreta. La matriz completa de fallas está en FachadaFallasTest (CA3-09).
     */

    /** HTTP 500 con SOAP Fault: {@code JBS-RESP-01} con el código y el texto del Fault en el detalle. */
    @Test
    public void soapFault() throws Exception {
        responder(500, "soap-fault.xml");
        SifenException e = Assertions.assertThrows(SifenException.class, () -> Sifen.consultaRUC("80123456", cfg));
        Assertions.assertEquals("JBS-RESP-01", e.getCode(), e.toString());
        Assertions.assertEquals(ErrorCategory.SIFEN_RESPONSE, e.getCategory());
        Assertions.assertTrue(e.getTechnicalDetail().contains("código="), e.getTechnicalDetail());
    }

    @Test
    public void http500SinSoap() throws Exception {
        responder(500, "http-500.html");
        SifenException e = Assertions.assertThrows(SifenException.class, () -> Sifen.consultaRUC("80123456", cfg));
        Assertions.assertEquals("JBS-CONEX-09", e.getCode(), e.toString());
        Assertions.assertInstanceOf(SifenConnectionException.class, e);
        Assertions.assertTrue(e.getMessage().contains("500"), e.getMessage());
        Assertions.assertTrue(e.getTechnicalDetail().contains("500 Internal Server Error"), e.getTechnicalDetail());
    }

    @Test
    public void cuerpoVacio() throws Exception {
        responder(200, null);
        SifenException e = Assertions.assertThrows(SifenException.class, () -> Sifen.consultaRUC("80123456", cfg));
        Assertions.assertEquals("JBS-RESP-02", e.getCode(), e.toString());
    }

    /** Espacios entre elementos de la respuesta: la línea base daba NPE (E2-04); desde §3.8 se interpreta. */
    @Test
    public void respuestaIndentadaSeInterpretaE204() throws Exception {
        responder(200, "consulta-ruc-0502-indentada.xml");
        RespuestaConsultaRUC r = Sifen.consultaRUC("80123456", cfg);
        Assertions.assertEquals("0502", r.getdCodRes());
        Assertions.assertEquals("RUC encontrado", r.getdMsgRes());
    }

    // ------------------------------------------------------------------ E0-10

    /**
     * {@code http://}: la línea base daba {@code ClassCastException} (E0-10); desde §3.7 la configuración
     * lo rechaza antes de conectar ({@code JBS-CFG-04}). El cast de {@code SoapHelper} ya no existe desde §3.8 (M3-39).
     */
    @Test
    public void urlHttpLaRechazaLaConfiguracionE010() {
        SifenConfig http = FixturesSifen.configuracion();
        http.setBaseUrl("http://127.0.0.1:" + servidor.getAddress().getPort());
        SifenValidationException e = Assertions.assertThrows(SifenValidationException.class,
                () -> Sifen.consultaRUC("80123456", http));
        Assertions.assertTrue(e.getResult().contains("JBS-CFG-04"), e.getMessage());
    }

    @Test
    @Disabled("El servidor embebido es HTTPS; el cast de E0-10 se corrigió en §3.8 (el protocolo se mira antes). Queda para la Fase 5 (§5.2) con un servidor http:// y JBS-CFG-04 desactivada")
    public void consultaRucPorHttp() throws Exception {
        SifenConfig http = FixturesSifen.configuracion();
        http.setBaseUrl("http://127.0.0.1:" + servidor.getAddress().getPort());
        responder(200, "consulta-ruc-0502.xml");
        Assertions.assertEquals("0502", Sifen.consultaRUC("80123456", http).getdCodRes());
    }
}
