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
package org.javabeanstack.sifen.test.vivo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import org.javabeanstack.sifen.ISifenClient;
import org.javabeanstack.sifen.SifenClient;
import org.javabeanstack.sifen.api.DEQueryResult;
import org.javabeanstack.sifen.api.SendResult;
import org.javabeanstack.sifen.api.SignedDocument;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.fields.request.de.TgDatRec;
import org.javabeanstack.sifen.core.fields.request.de.TgEmis;
import org.javabeanstack.sifen.core.validation.SifenValidationException;
import org.javabeanstack.sifen.core.validation.ValidationError;
import org.javabeanstack.sifen.internal.http.ServidoresFase5;
import org.javabeanstack.sifen.test.SifenTestEnv;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.javabeanstack.sifen.test.casos.XmlCanonico;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Verificación en vivo de la Fase 4 (plan JBSSIF, §4.6): los casos que cambiaron por la Fase 4 enviados
 * por la API nueva {@link ISifenClient} ({@code sign} → {@code send}) con la validación previa en su valor
 * por omisión (activa). Ambiente de PRUEBAS (D28), 001-001, numeración desde 1000 (ESC-8) reservada en la
 * tabla de equivalencias antes de cada envío. No reintenta: un rechazo o un corte se registra y se analiza.
 *
 * <p>Caminos: {@code sign → send}; releído del firmado y vuelto a firmar (E0-01); firmado guardado en
 * archivo, releído con {@code readSigned} y enviado tal cual (CA4-01); envío por un proxy local que cuenta
 * las conexiones (E5-01). Tras cada 0260, {@code queryDE} compara el {@code rDE} de SIFEN con el local.</p>
 *
 * <p>Uso: {@code mvn -pl sifen -am test -Psifen-live -Dtest=SifenVivoFase4Test -Djbs.sifen.test.env=<env>
 * -Djbs.sifen.test.ventas=<ventas de test8> -Djbs.sifen.test.equivalencias=<csv>
 * -Djbs.sifen.test.fase4.salida=<carpeta fuera del repo>}; {@code -Djbs.sifen.test.fase4.simular=true} hace
 * el ensayo en seco (firma y validación local, sin red, sobre una copia temporal de la tabla);
 * {@code -Djbs.sifen.test.fase4.solo=V4-01,V4-03} limita los casos.</p>
 */
@Tag("sifen-live")
public class SifenVivoFase4Test {

    /** Cómo llega el documento a {@code send}. */
    enum Camino {
        /** {@code sign(de)} → {@code send}. */
        FIRMA_ENVIO("sign → send"),
        /** {@code sign(de)} → {@code new DocumentoElectronico(xml)} → {@code sign} → {@code send} (E0-01). */
        RELEIDO_REFIRMADO("sign → releer → sign → send"),
        /** {@code sign(de)} → archivo → {@code readSigned} → {@code send} (CA4-01). */
        GUARDADO_READSIGNED("sign → archivo → readSigned → send"),
        /** {@code sign(de)} → {@code send} por un proxy local que cuenta los túneles (E5-01). */
        PROXY_CONTADO("sign → send (proxy contador)");

        final String texto;

        Camino(String texto) {
            this.texto = texto;
        }
    }

    /** Un caso de la verificación. */
    private record Caso(String id, String descripcion, Camino camino, Supplier<DocumentoElectronico> de) {
    }

    private static TgEmis emisorReal;

    /**
     * Diagnóstico sin enviar ningún DE: {@code checkConnection()} y, con
     * {@code -Djbs.sifen.test.fase4.consultar=<cdc>[,<cdc>]}, {@code queryDE} de cada CDC (para un envío de
     * resultado desconocido: se consulta, nunca se reenvía a ciegas). No imprime la configuración.
     */
    @Test
    public void diagnosticar() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("jbs.sifen.test.fase4.diagnostico"), "sin diagnóstico");
        SifenConfig cfg = SifenTestEnv.entornoOSaltar().getConfiguracion();
        Assertions.assertEquals(SifenConfig.Environment.DEV, cfg.getEnvironment(), "D28");
        Path dir = Paths.get(System.getProperty("jbs.sifen.test.fase4.salida"));
        Files.createDirectories(dir.resolve("respuestas"));
        try (ISifenClient client = SifenClient.create(cfg)) {
            org.javabeanstack.sifen.api.ConnectionDiagnosis d = client.checkConnection();
            System.out.println("checkConnection ⇒ " + d);
            String cdcs = System.getProperty("jbs.sifen.test.fase4.consultar");
            if (cdcs != null) {
                for (String cdc : cdcs.split(",")) {
                    DEQueryResult q = client.queryDE(cdc);
                    Files.writeString(dir.resolve("respuestas/consulta-" + cdc + ".xml"), String.valueOf(q.getRawResponse()),
                            StandardCharsets.UTF_8);
                    System.out.println("queryDE " + cdc + " ⇒ " + q.getResponseCode() + " " + q.getStatus() + " " + q.getMessage());
                }
            }
        }
    }

    /**
     * Recupera un envío de resultado desconocido por el camino del contrato (D43): {@code readSigned} del
     * {@code rDE} guardado antes del envío y {@code resolve} (consulta; con 0420 lo envía una vez, con 0422
     * devuelve el estado). {@code -Djbs.sifen.test.fase4.resolver=<id>:<archivo .pedido.xml>}.
     */
    @Test
    public void resolver() throws Exception {
        String arg = System.getProperty("jbs.sifen.test.fase4.resolver");
        Assumptions.assumeTrue(arg != null, "sin envío a resolver");
        SifenTestEnv.Entorno entorno = SifenTestEnv.entornoOSaltar();
        SifenConfig cfg = entorno.getConfiguracion();
        Assertions.assertEquals(SifenConfig.Environment.DEV, cfg.getEnvironment(), "D28");
        Assertions.assertTrue(cfg.isPreValidationEnabled(), "validación activa");
        String id = arg.substring(0, arg.indexOf(':'));
        Path pedido = Paths.get(arg.substring(arg.indexOf(':') + 1));
        Path dir = Paths.get(System.getProperty("jbs.sifen.test.fase4.salida"));
        Path res = dir.resolve("resultados.tsv");
        Path tabla = Paths.get(System.getProperty(SifenVivoTest.PROP_EQUIVALENCIAS));
        String xml = Files.readString(pedido, StandardCharsets.UTF_8);
        try (ISifenClient client = SifenClient.create(cfg)) {
            SignedDocument doc = client.readSigned(xml);
            SendResult r = client.resolve(doc);
            Files.writeString(dir.resolve("respuestas/" + doc.getCdc() + ".resolve.xml"), String.valueOf(r.getRawResponse()),
                    StandardCharsets.UTF_8);
            // fila de la tabla de equivalencias con los datos de la reserva (la reserva ya está)
            String reserva = null;
            for (String l : Files.readAllLines(tabla, StandardCharsets.UTF_8)) {
                if (l.contains(doc.getCdc()) && l.contains(";RESERVADO;")) {
                    reserva = l;
                }
            }
            Assertions.assertNotNull(reserva, "el CDC tiene su reserva en la tabla");
            String[] c = reserva.split(";", -1);
            Files.write(tabla, List.of(String.join(";", c[0], c[1], c[2], c[3], c[4], c[5],
                    "ISifenClient.resolve" + (r.isAlreadyRegistered() ? ":0422" : ":0420+send"), "200", r.getResponseCode(),
                    r.getStatus().toString(), limpio(r.getMessage()).replace(';', ','))), StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.APPEND);
            String digest = r.getDigestValue() == null || r.getDigestValue().isEmpty() ? "sin dDigVal"
                    : (r.getDigestValue().equals(doc.getDigestValue()) && !r.isDigestMismatch() ? "sí" : "NO");
            String consulta = "";
            String c14n = "";
            String totales = "";
            if ("0260".equals(r.getResponseCode())) {
                Thread.sleep(2000L);
                DEQueryResult q = client.queryDE(doc.getCdc());
                Files.writeString(dir.resolve("respuestas/consulta-" + doc.getCdc() + ".xml"), String.valueOf(q.getRawResponse()),
                        StandardCharsets.UTF_8);
                consulta = q.getResponseCode() + " " + q.getStatus();
                if (q.getXml() != null) {
                    c14n = Arrays.equals(XmlCanonico.canonico(xml), XmlCanonico.canonico(q.getXml())) ? "sí" : "NO";
                    totales = XmlCanonico.texto(xml, "dTotGralOpe") + "/" + XmlCanonico.texto(q.getXml(), "dTotGralOpe");
                }
            }
            fila(res, c[2] + "\t" + id + " (recuperación del corte)\treadSigned → resolve\t" + doc.getCdc() + "\tacepta · "
                    + resumenLocal(xml) + "\t" + r.getStatus() + (r.isAlreadyRegistered() ? " (ya registrado)" : "") + "\t"
                    + r.getResponseCode() + "\t" + limpio(r.getMessage()) + "\t" + digest + "\t" + consulta + "\t" + c14n + "\t"
                    + totales + "\t");
            Assertions.assertEquals("0260", r.getResponseCode(), r::getMessage);
        }
    }

    @Test
    public void verificarFase4() throws Exception {
        SifenTestEnv.Entorno entorno = SifenTestEnv.entornoOSaltar();
        SifenTestEnv.Emisor emisor = SifenTestEnv.emisorOSaltar();
        String salida = System.getProperty("jbs.sifen.test.fase4.salida");
        Assumptions.assumeTrue(salida != null, "falta jbs.sifen.test.fase4.salida");
        boolean simular = Boolean.getBoolean("jbs.sifen.test.fase4.simular");
        long pausa = Long.getLong("jbs.sifen.test.fase4.pausa", 3000L);
        Path dir = Paths.get(salida);
        Files.createDirectories(dir.resolve(simular ? "simulacion" : "respuestas"));

        SifenConfig cfg = entorno.getConfiguracion();
        Assertions.assertEquals(SifenConfig.Environment.DEV, cfg.getEnvironment(), "D28");
        // Validación previa activa en todos los envíos: el valor por omisión, sin tocarlo.
        Assertions.assertTrue(new SifenConfig().isPreValidationEnabled(), "el valor por omisión es con validación");
        Assertions.assertTrue(cfg.isPreValidationEnabled(), "la configuración del entorno no apaga la validación");

        Path tabla = Paths.get(System.getProperty(SifenVivoTest.PROP_EQUIVALENCIAS));
        if (simular) {
            Path copia = Files.createTempFile("equivalencias-simulacion-f4", ".csv");
            Files.copy(tabla, copia, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            tabla = copia;
        }
        ReemisionPrueba reemision = new ReemisionPrueba(emisor, entorno.getNumeroInicial(), tabla);
        cargarEmisorReal();

        Map<String, Caso> casos = new LinkedHashMap<>();
        agregar(casos, new Caso("V4-01", "DE-02 USD sin redondeo leído del XML (E0-01, E0-03, I4-04)",
                Camino.FIRMA_ENVIO, () -> leido("DE-02")));
        agregar(casos, new Caso("V4-02", "DE-06 PYG B2C sin redondeo, releído del firmado y vuelto a firmar (E0-01)",
                Camino.RELEIDO_REFIRMADO, () -> armado("DE-06")));
        agregar(casos, new Caso("V4-03", "DE-27 inmobiliaria USD redondeo hacia abajo, 1680.00 (E0-03)",
                Camino.FIRMA_ENVIO, () -> armado("DE-27")));
        agregar(casos, new Caso("V4-04", "DE-18 tasa 10.00 (E0-17)",
                Camino.FIRMA_ENVIO, () -> armado("DE-18")));
        agregar(casos, new Caso("V4-05", "DE-04 USD con redondeo aprobado (8517.50) guardado y releído (CA4-01, E0-03, I4-04)",
                Camino.GUARDADO_READSIGNED, () -> leido("DE-04")));
        agregar(casos, new Caso("V4-06", "MULTI-1 PYG 3 lotes 5 %/30 % + 10 % + exento, IVA con decimales (I3-41, D38 a)",
                Camino.FIRMA_ENVIO, () -> SifenVivoFase3Test.cierreVarios(5, new long[] {348504L, 777412L, 773559L}, 621018L, 542318L)));
        agregar(casos, new Caso("V4-07", "DE-05 PYG B2C con redondeo, una conexión por envío (E5-01)",
                Camino.PROXY_CONTADO, () -> armado("DE-05")));
        String solo = System.getProperty("jbs.sifen.test.fase4.solo");
        if (solo != null) {
            Set<String> ids = new HashSet<>(Arrays.asList(solo.split(",")));
            casos.keySet().removeIf(k -> !ids.contains(k));
        }

        Path res = dir.resolve(simular ? "simulacion.tsv" : "resultados.tsv");
        if (!Files.exists(res)) {
            Files.write(res, List.of("numero\tcaso\tapi\tcdc\tlocal\tstatus\tdCodRes\tdMsgRes\tdDigVal=local\tconsulta\tC14N=local"
                    + "\tdTotGralOpe local/SIFEN\tconexiones"), StandardCharsets.UTF_8);
        }
        List<String> fallas = new ArrayList<>();
        try (ISifenClient client = SifenClient.create(cfg)) {
            Assertions.assertTrue(client.getConfig().isPreValidationEnabled(), "el cliente valida antes de enviar");
            Assertions.assertTrue(client.validateConfig().isValid(), "la configuración del entorno no pasa la validación");
            boolean primero = true;
            for (Caso c : casos.values()) {
                if (!primero && !simular) {
                    Thread.sleep(pausa);
                }
                primero = false;
                String f = ejecutar(c, client, emisor, reemision, dir, res, simular);
                if (f != null) {
                    fallas.add(f);
                }
            }
        }
        Assertions.assertTrue(fallas.isEmpty(), () -> "casos que no dieron lo esperado: " + fallas);
    }

    private static void agregar(Map<String, Caso> casos, Caso c) {
        casos.put(c.id(), c);
    }

    /** @return {@code null} si el caso dio lo esperado; si no, el motivo. */
    private static String ejecutar(Caso c, ISifenClient client, SifenTestEnv.Emisor emisor, ReemisionPrueba reemision,
            Path dir, Path res, boolean simular) throws Exception {
        DocumentoElectronico de = c.de().get();
        preparar(de, emisor);
        LocalDateTime ahora = LocalDateTime.now().minusMinutes(1).truncatedTo(ChronoUnit.SECONDS);
        ReemisionPrueba.Reemision re = reemision.reemitir(c.id(), de, ahora);
        String prefijo = re.enviado + "\t" + c.id() + " " + c.descripcion() + "\t" + c.camino().texto + "\t" + re.cdc;

        // 1) firma (con la validación previa activa)
        SignedDocument doc;
        try {
            doc = client.sign(de);
        } catch (SifenValidationException x) {
            TreeSet<String> codigos = new TreeSet<>();
            for (ValidationError e : x.getErrors()) {
                codigos.add(e.getCode() + (e.getPath() == null ? "" : "@" + e.getPath()));
            }
            fila(res, prefijo + "\trechaza local " + codigos + "\t\t\t\t\t\t\t\t");
            if (!simular) {
                reemision.registrar(c.id(), re, "ISifenClient.sign:rechazo-local", 0, x.getCode(), null, codigos.toString());
            }
            return c.id() + " rechazo local " + codigos;
        }
        Assertions.assertEquals(re.cdc, doc.getCdc(), "el CDC firmado es el reservado");
        String local = resumenLocal(doc.getXml());

        // 2) el camino hasta send
        SignedDocument aEnviar = doc;
        switch (c.camino()) {
            case RELEIDO_REFIRMADO -> {
                DocumentoElectronico releido = new DocumentoElectronico(doc.getXml());
                SignedDocument otra = client.sign(releido);
                Assertions.assertEquals(doc.getCdc(), otra.getCdc(), "al regenerar se conserva el CDC");
                Assertions.assertEquals(importes(doc.getXml()), importes(otra.getXml()), "al regenerar se conservan los importes");
                local += " · releído: mismo CDC e importes, firma " + (doc.getDigestValue().equals(otra.getDigestValue()) ? "igual" : "nueva");
                aEnviar = otra;
            }
            case GUARDADO_READSIGNED -> {
                Path guardado = dir.resolve((simular ? "simulacion/" : "respuestas/") + doc.getCdc() + ".firmado.xml");
                Files.writeString(guardado, doc.getXml(), StandardCharsets.UTF_8);
                String leido = Files.readString(guardado, StandardCharsets.UTF_8);
                SignedDocument r = client.readSigned(leido);
                Assertions.assertEquals(leido, r.getXml(), "readSigned devuelve el XML byte a byte");
                Assertions.assertEquals(doc.getDigestValue(), r.getDigestValue(), "readSigned conserva la firma");
                Assertions.assertEquals(doc.getQRLink(), r.getQRLink(), "readSigned conserva el QR");
                Assertions.assertTrue(client.verifySignature(leido).isValid(), "la firma del guardado valida");
                local += " · guardado y releído: mismo XML, DigestValue y QR";
                aEnviar = r;
            }
            default -> {
            }
        }
        String pedido = aEnviar.getXml();
        if (simular) {
            Files.writeString(dir.resolve("simulacion/" + c.id() + ".pedido.xml"), pedido, StandardCharsets.UTF_8);
            fila(res, prefijo + "\tacepta · " + local + "\t\t\t\tsimulado (rDE " + pedido.length() + " caracteres)\t\t\t\t");
            return null;
        }
        Files.writeString(dir.resolve("respuestas/" + re.cdc + ".pedido.xml"), pedido, StandardCharsets.UTF_8);

        // 3) envío
        SendResult r;
        String conexiones = "";
        try {
            if (c.camino() == Camino.PROXY_CONTADO) {
                try (ServidoresFase5.ProxyLocal proxy = new ServidoresFase5.ProxyLocal(null, null)) {
                    SifenConfig conProxy = SifenTestEnv.entornoOSaltar().getConfiguracion();   // otra instancia, no la compartida
                    Assertions.assertEquals(SifenConfig.Environment.DEV, conProxy.getEnvironment(), "D28");
                    Assertions.assertTrue(conProxy.isPreValidationEnabled(), "validación activa");
                    conProxy.setProxyHost("127.0.0.1");
                    conProxy.setProxyPort(proxy.puerto());
                    try (ISifenClient porProxy = SifenClient.create(conProxy)) {
                        long antes = proxy.connects();
                        r = porProxy.send(aEnviar);
                        long tuneles = proxy.connects() - antes;   // antes de cualquier consulta
                        conexiones = String.valueOf(tuneles);
                        Assertions.assertEquals(0, antes, "el proxy no tenía túneles antes del envío");
                        if (tuneles != 1) {
                            fila(res, prefijo + "\tacepta · " + local + "\t" + r.getStatus() + "\t" + r.getResponseCode() + "\t"
                                    + limpio(r.getMessage()) + "\t\t\t\t\t" + conexiones);
                            return c.id() + " abrió " + tuneles + " conexiones (E5-01)";
                        }
                    }
                }
            } else {
                r = client.send(aEnviar);
            }
        } catch (SifenException x) {
            fila(res, prefijo + "\tacepta · " + local + "\texcepción\t" + x.getCode() + "\t" + limpio(x.getMessage())
                    + " (resultado desconocido: " + x.isOutcomeUnknown() + ")\t\t\t\t\t" + conexiones);
            reemision.registrar(c.id(), re, "ISifenClient.send:excepcion", 0, x.getCode(), null, x.getMessage());
            throw x;   // sin reintentos: se analiza
        }
        Files.writeString(dir.resolve("respuestas/" + re.cdc + ".xml"), String.valueOf(r.getRawResponse()), StandardCharsets.UTF_8);
        // una respuesta interpretada es un HTTP 200 (todo otro estado sale como excepción, I3-48)
        reemision.registrar(c.id(), re, "ISifenClient.send", 200, r.getResponseCode(), r.getStatus().toString(), r.getMessage());
        String digest = r.getDigestValue() == null || r.getDigestValue().isEmpty() ? "sin dDigVal"
                : (r.getDigestValue().equals(aEnviar.getDigestValue()) && !r.isDigestMismatch() ? "sí" : "NO");

        // 4) consulta del DE aprobado: el rDE de SIFEN contra el local
        String consulta = "";
        String c14n = "";
        String totales = "";
        if ("0260".equals(r.getResponseCode())) {
            Thread.sleep(2000L);
            DEQueryResult q = client.queryDE(aEnviar.getCdc());
            Files.writeString(dir.resolve("respuestas/consulta-" + re.cdc + ".xml"), String.valueOf(q.getRawResponse()), StandardCharsets.UTF_8);
            reemision.registrar(c.id(), re, "ISifenClient.queryDE", 200, q.getResponseCode(), q.getStatus().toString(), q.getMessage());
            consulta = q.getResponseCode() + " " + q.getStatus();
            if (q.getXml() != null) {
                c14n = Arrays.equals(XmlCanonico.canonico(pedido), XmlCanonico.canonico(q.getXml())) ? "sí" : "NO";
                totales = XmlCanonico.texto(pedido, "dTotGralOpe") + "/" + XmlCanonico.texto(q.getXml(), "dTotGralOpe");
                if (!aEnviar.getDigestValue().equals(q.getDigestValue())) {
                    c14n += " (DigestValue de la consulta distinto)";
                }
            } else {
                c14n = "sin rDE en la consulta";
            }
        }
        fila(res, prefijo + "\tacepta · " + local + "\t" + r.getStatus() + "\t" + r.getResponseCode() + "\t" + limpio(r.getMessage())
                + "\t" + digest + "\t" + consulta + "\t" + c14n + "\t" + totales + "\t" + conexiones);
        if (!"0260".equals(r.getResponseCode())) {
            return c.id() + " " + r.getResponseCode() + " " + r.getMessage();
        }
        if (!"sí".equals(digest)) {
            return c.id() + " dDigVal de SIFEN ≠ DigestValue local";
        }
        return null;
    }

    /** dRedon, dTotOpe, dTotGralOpe, dTotalGs y dTiCam del XML firmado (lo que cambió en la Fase 4). */
    private static String resumenLocal(String xml) throws Exception {
        StringBuilder b = new StringBuilder();
        for (String campo : new String[] {"dTasaIVA", "dRedon", "dTotOpe", "dTotGralOpe", "dTiCam", "dTotalGs"}) {
            String v = XmlCanonico.texto(xml, campo);
            if (v != null) {
                b.append(b.length() == 0 ? "" : " ").append(campo).append('=').append(v);
            }
        }
        return b.toString();
    }

    /** Todos los importes de gTotSub (y los del ítem que mira la Fase 4), por texto. */
    private static List<String> importes(String xml) throws Exception {
        List<String> l = new ArrayList<>();
        org.w3c.dom.Document d = XmlCanonico.parsear(xml);
        for (String grupo : new String[] {"gTotSub", "gCamIVA", "gValorItem"}) {
            org.w3c.dom.NodeList ns = d.getElementsByTagNameNS("*", grupo);
            for (int i = 0; i < ns.getLength(); i++) {
                org.w3c.dom.NodeList hijos = ns.item(i).getChildNodes();
                for (int j = 0; j < hijos.getLength(); j++) {
                    if (hijos.item(j) instanceof org.w3c.dom.Element e && e.getElementsByTagNameNS("*", "*").getLength() == 0) {
                        l.add(grupo + "." + e.getLocalName() + "=" + e.getTextContent());
                    }
                }
            }
        }
        return l;
    }

    private static DocumentoElectronico armado(String id) {
        return caso(id).construir();
    }

    private static DocumentoElectronico leido(String id) {
        try {
            CasosDePrueba.Caso c = caso(id);
            return c.leer(FixturesSifen.entrada(c));
        } catch (Exception e) {
            throw new IllegalStateException(id, e);
        }
    }

    private static CasosDePrueba.Caso caso(String id) {
        return CasosDePrueba.documentos().stream().filter(k -> k.getId().equals(id)).findFirst().orElseThrow();
    }

    /**
     * Los ajustes de la Fase 3 para que SIFEN-test no rechace por los datos y no por la librería: el
     * {@code gEmis} de una venta real de test8 (si no, 1261), el receptor contribuyente con el RUC del
     * propio emisor de prueba (si no, 1306) y el origen 999-999 para la guarda D28 de la reemisión.
     */
    private static void preparar(DocumentoElectronico de, SifenTestEnv.Emisor emisor) {
        de.getgDatGralOpe().setgEmis(copiaEmisor());
        TgDatRec r = de.getgDatGralOpe().getgDatRec();
        if (r.getdRucRec() != null) {
            r.setdRucRec(emisor.getRuc());
            r.setdDVRec(Short.parseShort(emisor.getDv()));
        }
        de.getgTimb().setdEst("999");
        de.getgTimb().setdPunExp("999");
    }

    private static void cargarEmisorReal() throws Exception {
        Path ventas = Paths.get(System.getProperty(SifenVivoTest.PROP_VENTAS));
        try (java.nio.file.DirectoryStream<Path> ds = Files.newDirectoryStream(ventas, "venta-*.xml")) {
            Path primera = ds.iterator().next();
            emisorReal = new DocumentoElectronico(Files.readString(primera, StandardCharsets.UTF_8)).getgDatGralOpe().getgEmis();
        }
    }

    /** Un gEmis nuevo con los datos de la venta real (la reemisión pisa RUC, DV, nombre y correo). */
    private static TgEmis copiaEmisor() {
        TgEmis e = new TgEmis();
        TgEmis o = emisorReal;
        e.setdRucEm(o.getdRucEm());
        e.setdDVEmi(o.getdDVEmi());
        e.setiTipCont(o.getiTipCont());
        e.setcTipReg(o.getcTipReg());
        e.setdNomEmi(o.getdNomEmi());
        e.setdNomFanEmi(o.getdNomFanEmi());
        e.setdDirEmi(o.getdDirEmi());
        e.setdNumCas(o.getdNumCas());
        e.setdCompDir1(o.getdCompDir1());
        e.setdCompDir2(o.getdCompDir2());
        e.setcDepEmi(o.getcDepEmi());
        e.setcDisEmi(o.getcDisEmi());
        e.setdDesDisEmi(o.getdDesDisEmi());
        e.setcCiuEmi(o.getcCiuEmi());
        e.setdDesCiuEmi(o.getdDesCiuEmi());
        e.setdTelEmi(o.getdTelEmi());
        e.setdEmailE(o.getdEmailE());
        e.setdDenSuc(o.getdDenSuc());
        e.setgActEcoList(o.getgActEcoList());
        e.setgRespDE(o.getgRespDE());
        return e;
    }

    private static String limpio(String s) {
        return String.valueOf(s).replace('\t', ' ').replace('\n', ' ');
    }

    private static void fila(Path res, String fila) throws java.io.IOException {
        Files.write(res, List.of(fila), StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND);
        System.out.println(fila);
    }
}
