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
package org.javabeanstack.sifen;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.javabeanstack.sifen.api.BatchQueryResult;
import org.javabeanstack.sifen.api.BatchSendResult;
import org.javabeanstack.sifen.api.BatchStatus;
import org.javabeanstack.sifen.api.ConnectionDiagnosis;
import org.javabeanstack.sifen.api.EventResult;
import org.javabeanstack.sifen.api.NumberRange;
import org.javabeanstack.sifen.api.ResultStatus;
import org.javabeanstack.sifen.api.SendResult;
import org.javabeanstack.sifen.api.SignedDocument;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.exceptions.ErrorCategory;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.fields.request.de.TgCamItem;
import org.javabeanstack.sifen.core.fields.request.de.TgVehTras;
import org.javabeanstack.sifen.core.types.TTiDE;
import org.javabeanstack.sifen.core.types.TcRelMerc;
import org.javabeanstack.sifen.core.types.TiTiOpe;
import org.javabeanstack.sifen.simulator.RecordedRequest;
import org.javabeanstack.sifen.simulator.Scenario;
import org.javabeanstack.sifen.simulator.SifenSimulator;
import org.javabeanstack.sifen.simulator.SimulatedService;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * El recorrido del beta por {@link ISifenClient} contra el simulador, sin red (plan JBSSIF, §8.7, CA8-11): la
 * misma batería que §8.7 corrió en vivo contra SIFEN-test, para que Maker la tenga sin red ni certificado.
 *
 * <p>Un solo test, en orden y repetible (cada corrida levanta su simulador y numera desde cero): diagnóstico de
 * conexión; FE, NC asociada a esa FE, NR y autofactura en dos pasos (firmar, «grabar» el XML, releerlo y
 * enviarlo); lote de dos FE hasta concluir (0300 → 0361 → 0362 con un resultado por CDC); cancelación e
 * inutilización; un envío de resultado desconocido resuelto con {@code resolve} sin duplicar (0422: no reenvía) y
 * otro reenviado una sola vez (0420); y la <b>recuperación sin reiniciar la JVM</b>: caída y vuelta del
 * servidor en el mismo puerto con el mismo cliente, certificado borrado y cambiado en caliente. Cada paso deja una
 * línea en {@code target/beta/ca8_11_recorrido.tsv}.</p>
 */
public class BetaJourneySimulatorTest {
    private static final String MOTIVO = "Cancelacion de prueba del documento";
    private final AtomicInteger numero = new AtomicInteger(800);
    private final List<String> pasos = new ArrayList<>();
    private SifenSimulator sim;

    @TempDir
    Path temporal;

    @AfterEach
    public void cerrar() throws Exception {
        if (sim != null) {
            sim.close();
        }
        Path destino = Paths.get("target", "beta", "ca8_11_recorrido.tsv");
        Files.createDirectories(destino.getParent());
        List<String> l = new ArrayList<>();
        l.add("paso\toperacion\tresultado\tdetalle");
        l.addAll(pasos);
        Files.write(destino, l, StandardCharsets.UTF_8);
    }

    private void paso(String id, String operacion, Object resultado, String detalle) {
        pasos.add(id + "\t" + operacion + "\t" + resultado + "\t" + detalle);
    }

    @Test
    public void recorridoDelBetaSinRed() throws Exception {
        sim = SifenSimulator.start();
        SifenConfig cfg = sim.config();
        cfg.setHttpReadTimeout(500);
        ISifenClient c = SifenClient.create(cfg);

        // ---------------------------------------------------------------- diagnóstico
        ConnectionDiagnosis d = c.checkConnection();
        Assertions.assertTrue(d.isOk(), d.toString());
        paso("D-01", "checkConnection", d.isOk(), d.getSteps().size() + " etapas");

        // ---------------------------------------------------------------- FE, NC, NR y autofactura en dos pasos
        SendResult fe = emitir(c, "B-01", "FE (DE-05)", dev(armado("DE-05")));
        DocumentoElectronico nc = dev(armado("DE-21"));
        nc.getgCamDEAsocList().get(0).setdCdCDERef(fe.getCdc());
        emitir(c, "B-02", "NC asociada a la FE (DE-21)", nc);
        emitir(c, "B-03", "NR (DE-23 con los datos que la validación exige)", dev(remision()));
        DocumentoElectronico af = dev(armado("DE-20"));
        af.getgDatGralOpe().getgDatRec().setiTiOpe(TiTiOpe.B2C);
        emitir(c, "B-04", "Autofactura (DE-20, B2C)", af);

        // ---------------------------------------------------------------- lote hasta concluir
        SignedDocument l1 = c.sign(dev(armado("DE-05")));
        SignedDocument l2 = c.sign(dev(armado("DE-05")));
        BatchSendResult lote = c.sendBatch(List.of(l1, l2));
        Assertions.assertEquals(BatchStatus.RECEIVED, lote.getStatus());
        paso("L-01", "sendBatch", lote.getStatus(), lote.getBatchNumber() + " · " + lote.getCdcs().size() + " CDC");
        sim.once(SimulatedService.QUERY_BATCH, Scenario.batchInProcess());
        BatchQueryResult enProceso = c.queryBatch(lote.getBatchNumber());
        Assertions.assertEquals(BatchStatus.IN_PROCESS, enProceso.getStatus());
        Assertions.assertTrue(enProceso.getSuggestedWait().compareTo(Duration.ZERO) > 0);
        paso("L-02", "queryBatch", enProceso.getStatus(), "espera sugerida " + enProceso.getSuggestedWait());
        sim.on(SimulatedService.QUERY_BATCH, Scenario.batchCompleted(lote.getCdcs()));
        BatchQueryResult concluido = c.queryBatch(lote.getBatchNumber());
        Assertions.assertEquals(BatchStatus.COMPLETED, concluido.getStatus());
        Assertions.assertEquals(List.of(l1.getCdc(), l2.getCdc()), concluido.getResults().stream().map(SendResult::getCdc).toList());
        Assertions.assertTrue(concluido.getResults().stream().allMatch(r -> r.getStatus() == ResultStatus.APPROVED));
        paso("L-03", "queryBatch", concluido.getStatus(), concluido.getResults().size() + " aprobados por CDC");

        // ---------------------------------------------------------------- eventos
        EventResult can = c.cancel(fe.getCdc(), MOTIVO);
        Assertions.assertEquals(ResultStatus.APPROVED, can.getStatus(), can.toString());
        paso("E-01", "cancel", can.getStatus(), "Id " + can.getEventId() + " · " + can.getAuthorizationProtocol());
        NumberRange rango = NumberRange.builder().timbrado(String.valueOf(CasosDePrueba.TIMBRADO)).establishment("1")
                .issuePoint("1").documentType(TTiDE.FACTURA_ELECTRONICA).from("50").to("55")
                .reason("Inutilizacion de prueba de numeracion").build();
        EventResult inu = c.invalidateNumbers(rango);
        Assertions.assertEquals(ResultStatus.APPROVED, inu.getStatus(), inu.toString());
        paso("E-02", "invalidateNumbers", inu.getStatus(), "50..55 · Id " + inu.getEventId());

        // ---------------------------------------------------------------- resultado desconocido y resolve
        SignedDocument duda = c.sign(dev(armado("DE-05")));
        sim.once(SimulatedService.RECEIVE_DE, Scenario.delayed(Duration.ofMillis(2000), Scenario.approved()));
        SifenException e = Assertions.assertThrows(SifenException.class, () -> c.send(duda));
        Assertions.assertTrue(e.isOutcomeUnknown(), e.getCode());
        Assertions.assertEquals("JBS-CONEX-05", e.getCode());
        SendResult resuelto = c.resolve(c.readSigned(duda.getXml()));
        Assertions.assertTrue(resuelto.isAlreadyRegistered(), "0422: SIFEN lo tiene");
        Assertions.assertEquals(1, enviosDe(duda.getCdc()), "resolve no reenvía lo que SIFEN ya tiene");
        paso("R-01", "send + resolve (0422)", resuelto.getStatus(), e.getCode() + " → sin reenvío");
        SignedDocument perdido = c.sign(dev(armado("DE-05")));
        sim.once(SimulatedService.RECEIVE_DE, Scenario.connectionCut());
        Assertions.assertTrue(Assertions.assertThrows(SifenException.class, () -> c.send(perdido)).isOutcomeUnknown());
        sim.once(SimulatedService.QUERY_DE, Scenario.deNotFound());
        SendResult reenviado = c.resolve(perdido);
        Assertions.assertEquals(ResultStatus.APPROVED, reenviado.getStatus());
        Assertions.assertEquals(2, enviosDe(perdido.getCdc()), "el corte y un solo reenvío de resolve");
        paso("R-02", "send + resolve (0420)", reenviado.getStatus(), "corte → un reenvío");

        // ---------------------------------------------------------------- caída y vuelta del servidor
        int puerto = sim.getPort();
        sim.close();
        SifenException caido = Assertions.assertThrows(SifenException.class, () -> c.queryRuc("80123456"));
        Assertions.assertEquals(ErrorCategory.CONNECTION, caido.getCategory(), caido.getCode());
        paso("C-01", "queryRuc con SIFEN caído", caido.getCode(), caido.getCategory().name());
        sim = SifenSimulator.start(puerto);
        Assertions.assertTrue(c.queryRuc("80123456").isFound(), "el mismo cliente, sin reiniciar la JVM");
        Assertions.assertEquals(ResultStatus.APPROVED, c.send(c.sign(dev(armado("DE-05")))).getStatus());
        paso("C-02", "queryRuc y send tras la vuelta", "OK", "mismo cliente, puerto " + puerto);

        // ---------------------------------------------------------------- certificado borrado y cambiado en caliente
        Path pfx = temporal.resolve("empresa.pfx");
        Files.write(pfx, SifenSimulator.defaultClientCertificate());
        SifenConfig enDisco = sim.config();
        enDisco.setClientCertificate(pfx.toString());
        ISifenClient k = SifenClient.create(enDisco);
        Assertions.assertTrue(k.queryRuc("80123456").isFound());
        Files.delete(pfx);
        SifenException borrado = Assertions.assertThrows(SifenException.class, () -> k.sign(dev(armado("DE-05"))));
        Assertions.assertEquals("JBS-CERT-01", borrado.getCode());
        Assertions.assertEquals("JBS-CERT-01", k.getCertificateInfo().getFailureCode());
        paso("K-01", "sign con el certificado borrado", borrado.getCode(), borrado.getCategory().name());
        Files.write(pfx, recurso("certificados/jbs-sifen-empresa-b.pfx"));
        sim.reset();
        Assertions.assertTrue(k.queryRuc("80123456").isFound());
        Assertions.assertTrue(sim.requests().get(0).getClientSubject().contains("RUC80999901-3"),
                "el TLS ya sale con el certificado nuevo: " + sim.requests().get(0).getClientSubject());
        SifenException otro = Assertions.assertThrows(SifenException.class, () -> k.sign(dev(armado("DE-05"))));
        Assertions.assertEquals("JBS-CERT-11", otro.getCode(), "el certificado nuevo es de otro RUC que el emisor");
        paso("K-02", "certificado cambiado en caliente", otro.getCode(), "TLS con RUC80999901-3");
        Files.write(pfx, SifenSimulator.defaultClientCertificate());
        Assertions.assertEquals(ResultStatus.APPROVED, k.send(k.sign(dev(armado("DE-05")))).getStatus());
        paso("K-03", "certificado repuesto", "APPROVED", "sin invalidar ni reiniciar");
    }

    // ------------------------------------------------------------------ apoyo

    /** Firmar, «grabar» el XML, releerlo y enviarlo: llega el mismo XML y SIFEN informa el mismo digest. */
    private SendResult emitir(ISifenClient c, String id, String nombre, DocumentoElectronico de) throws Exception {
        SignedDocument doc = c.sign(de);
        String guardado = doc.getXml();
        SendResult r = c.send(c.readSigned(guardado));
        Assertions.assertEquals(ResultStatus.APPROVED, r.getStatus(), nombre + ": " + r.getMessage());
        Assertions.assertFalse(r.isDigestMismatch(), nombre);
        Assertions.assertEquals(doc.getCdc(), r.getCdc());
        List<RecordedRequest> p = sim.requests(SimulatedService.RECEIVE_DE);
        Assertions.assertEquals(doc.getDigestValue(), p.get(p.size() - 1).text("DigestValue").trim(), nombre + ": mismo XML");
        paso(id, "sign + readSigned + send · " + nombre, r.getStatus(), doc.getCdc());
        return r;
    }

    private long enviosDe(String cdc) {
        return sim.requests(SimulatedService.RECEIVE_DE).stream().filter(p -> cdc.equals(p.getCdc())).count();
    }

    private static DocumentoElectronico armado(String id) {
        return CasosDePrueba.documentos().stream().filter(k -> k.getId().equals(id)).findFirst().orElseThrow().construir();
    }

    /** Leyendas del ambiente de pruebas (Q14) y un número propio. */
    private DocumentoElectronico dev(DocumentoElectronico de) {
        de.getgDatGralOpe().getgEmis().setdNomEmi("DE generado en ambiente de prueba - sin valor comercial ni fiscal");
        de.getgDtipDE().getgCamItemList().get(0).setdDesProSer(
                "DOCUMENTO ELECTRÓNICO SIN VALOR COMERCIAL NI FISCAL - GENERADO EN AMBIENTE DE PRUEBA");
        de.getgTimb().setdNumDoc(String.format("%07d", numero.incrementAndGet()));
        return de;
    }

    /** DE-23 con lo que la validación previa exige a una NR (E715, E717, JBS-E-174; I8-09), como §8.7. */
    private static DocumentoElectronico remision() {
        DocumentoElectronico de = armado("DE-23");
        for (TgCamItem i : de.getgDtipDE().getgCamItemList()) {
            i.setcRelMerc(TcRelMerc.TOLERANCIA_DE_QUIEBRA);
            i.setdCanQuiMer(BigDecimal.ZERO);
            i.setdPorQuiMer(BigDecimal.ZERO);
        }
        for (TgVehTras v : de.getgDtipDE().getgTransp().getgVehTrasList()) {
            v.setdMarVeh("TOYOTA");
        }
        return de;
    }

    private static byte[] recurso(String ruta) throws Exception {
        try (InputStream in = BetaJourneySimulatorTest.class.getClassLoader().getResourceAsStream(ruta)) {
            Assertions.assertNotNull(in, ruta);
            return in.readAllBytes();
        }
    }
}
