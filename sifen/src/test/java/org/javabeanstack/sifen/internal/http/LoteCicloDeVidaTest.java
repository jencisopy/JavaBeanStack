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
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.javabeanstack.sifen.ISifenClient;
import org.javabeanstack.sifen.Sifen;
import org.javabeanstack.sifen.SifenClient;
import org.javabeanstack.sifen.api.BatchQueryResult;
import org.javabeanstack.sifen.api.BatchStatus;
import org.javabeanstack.sifen.api.ResultStatus;
import org.javabeanstack.sifen.api.SendResult;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.beans.response.RespuestaRecepcionLoteDE;
import org.javabeanstack.sifen.core.exceptions.ErrorCategory;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Ciclo de vida del lote (plan JBSSIF, §5.5; M3-27, M3-16, CA5-08): control local de un solo tipo y un
 * solo emisor ({@code JBS-ENT-09}) y la respuesta grabada de cada estado (recepción 0300/0301; consulta
 * 0360/0361/0362/0364) por el servidor HTTPS embebido, con el resultado repartido por CDC.
 *
 * <p>Respuestas sintéticas propias de esta prueba (MT v150, con el emisor ficticio {@code 80123456-5}),
 * en {@code src/test/resources/respuestas/}: {@code fase5-consulta-lote-0360.xml},
 * {@code fase5-consulta-lote-0361.xml}, {@code fase5-consulta-lote-0362-dos.xml} (un aprobado y un
 * rechazado 1001), {@code fase5-consulta-lote-0364.xml} y {@code fase5-recepcion-lote-0301.xml}. La
 * consulta de un lote del que no llegó el número (I10-06) necesita SIFEN-test y queda abierta.</p>
 */
public class LoteCicloDeVidaTest {
    private static final String CDC_1 = "01801234565001001000000122026112310000000010";
    private static final String CDC_2 = "01801234565001001000001322026112310000000018";
    private static HttpsServer servidor;
    private static volatile byte[] cuerpo = new byte[0];
    private static SifenConfig cfg;

    @BeforeAll
    public static void levantar() throws Exception {
        ServidoresFase5.confiarEnElAutofirmado();
        servidor = ServidoresFase5.https(0, x -> {
            try (InputStream in = x.getRequestBody()) {
                in.readAllBytes();
            }
            byte[] b = cuerpo;
            x.getResponseHeaders().add("Content-Type", "application/soap+xml; charset=utf-8");
            x.sendResponseHeaders(200, b.length);
            try (OutputStream out = x.getResponseBody()) {
                out.write(b);
            }
        });
        cfg = FixturesSifen.configuracion();
        cfg.setBaseUrl("https://127.0.0.1:" + servidor.getAddress().getPort());
    }

    @AfterAll
    public static void bajar() {
        if (servidor != null) {
            servidor.stop(0);
        }
        ServidoresFase5.restaurar();
    }

    private static void responder(String recurso) throws Exception {
        cuerpo = FixturesSifen.recursoBytes("respuestas/" + recurso);
    }

    // ------------------------------------------------------------------ JBS-ENT-09

    @Test
    public void unSoloTipoYUnSoloEmisorPasa() throws Exception {
        BatchChecks.requireSingleTypeAndIssuer(List.of(CDC_1, CDC_2));
        BatchChecks.requireSingleTypeAndIssuer(null);
        BatchChecks.requireSingleTypeAndIssuer(List.of());
        List<String> conNulo = new ArrayList<>();
        conNulo.add(CDC_1);
        conNulo.add(null);
        conNulo.add("corto");
        conNulo.add(CDC_2);
        BatchChecks.requireSingleTypeAndIssuer(conNulo);
        Assertions.assertEquals("01", BatchChecks.typeOf(CDC_1));
        Assertions.assertEquals("80123456", BatchChecks.issuerOf(CDC_1));
    }

    @Test
    public void tiposMezcladosEsJbsEnt09() {
        String notaCredito = "05" + CDC_2.substring(2);
        SifenException e = Assertions.assertThrows(SifenException.class,
                () -> BatchChecks.requireSingleTypeAndIssuer(List.of(CDC_1, CDC_2, notaCredito)));
        Assertions.assertEquals("JBS-ENT-09", e.getCode());
        Assertions.assertEquals(ErrorCategory.INPUT, e.getCategory());
        Assertions.assertTrue(e.getUserMessage().contains("documentos 1 y 3: tipo 01 y 05"), e.getUserMessage());
    }

    @Test
    public void emisoresMezcladosEsJbsEnt09() {
        String otroEmisor = CDC_2.substring(0, 2) + "80000005" + CDC_2.substring(10);
        SifenException e = Assertions.assertThrows(SifenException.class,
                () -> BatchChecks.requireSingleTypeAndIssuer(List.of(CDC_1, otroEmisor)));
        Assertions.assertEquals("JBS-ENT-09", e.getCode());
        Assertions.assertTrue(e.getUserMessage().contains("RUC 80123456 y 80000005"), e.getUserMessage());
    }

    // ------------------------------------------------------------------ estados de la consulta

    @Test
    public void loteInexistente0360() throws Exception {
        responder("fase5-consulta-lote-0360.xml");
        try (ISifenClient c = SifenClient.create(cfg)) {
            BatchQueryResult r = c.queryBatch("1234567890123456");
            Assertions.assertEquals(BatchStatus.NOT_FOUND, r.getStatus());
            Assertions.assertEquals("0360", r.getResponseCode());
            Assertions.assertEquals(Duration.ZERO, r.getSuggestedWait());
            Assertions.assertTrue(r.getResults().isEmpty());
        }
    }

    @Test
    public void loteEnProceso0361SugiereDiezMinutos() throws Exception {
        responder("fase5-consulta-lote-0361.xml");
        try (ISifenClient c = SifenClient.create(cfg)) {
            BatchQueryResult r = c.queryBatch("1234567890123456");
            Assertions.assertEquals(BatchStatus.IN_PROCESS, r.getStatus());
            Assertions.assertEquals(Duration.ofMinutes(10), r.getSuggestedWait(), "Guía de Mejores Prácticas");
            Assertions.assertTrue(r.getResults().isEmpty());
        }
    }

    @Test
    public void loteConcluido0362RepartidoPorCdc() throws Exception {
        responder("fase5-consulta-lote-0362-dos.xml");
        try (ISifenClient c = SifenClient.create(cfg)) {
            BatchQueryResult r = c.queryBatch("1234567890123456");
            Assertions.assertEquals(BatchStatus.COMPLETED, r.getStatus());
            Assertions.assertEquals(Duration.ZERO, r.getSuggestedWait());
            Assertions.assertEquals(2, r.getResults().size());
            SendResult a = r.getResults().get(0);
            SendResult b = r.getResults().get(1);
            Assertions.assertEquals(CDC_1, a.getCdc());
            Assertions.assertEquals(ResultStatus.APPROVED, a.getStatus());
            Assertions.assertEquals("1000000002", a.getAuthorizationProtocol());
            Assertions.assertEquals(CDC_2, b.getCdc());
            Assertions.assertEquals(ResultStatus.REJECTED, b.getStatus());
            Assertions.assertEquals("1001", b.getMessages().get(0).getCode());
            BatchChecks.requireSingleTypeAndIssuer(List.of(a.getCdc(), b.getCdc()));
        }
    }

    @Test
    public void loteRealConcluido0362ConOchoDocumentos() throws Exception {
        responder("real-consulta-lote-0362.xml");
        try (ISifenClient c = SifenClient.create(cfg)) {
            BatchQueryResult r = c.queryBatch("110724783112254838");
            Assertions.assertEquals(BatchStatus.COMPLETED, r.getStatus());
            Assertions.assertEquals(8, r.getResults().size());
            Assertions.assertEquals(7, r.getResults().stream().filter(x -> x.getStatus() == ResultStatus.APPROVED).count());
        }
    }

    @Test
    public void consultaExtemporanea0364() throws Exception {
        responder("fase5-consulta-lote-0364.xml");
        try (ISifenClient c = SifenClient.create(cfg)) {
            BatchQueryResult r = c.queryBatch("1234567890123456");
            Assertions.assertEquals(BatchStatus.EXPIRED, r.getStatus());
            Assertions.assertEquals(Duration.ZERO, r.getSuggestedWait(), "pasadas las 48 h se resuelve cada CDC");
        }
    }

    // ------------------------------------------------------------------ estados de la recepción

    @Test
    public void loteRecibido0300YNoEncolado0301() throws Exception {
        List<DocumentoElectronico> lote = new ArrayList<>();
        lote.add(CasosDePrueba.documentos().get(4).construir());
        responder("recepcion-lote-0300.xml");
        RespuestaRecepcionLoteDE recibido = Sifen.recepcionLoteDE(lote, cfg);
        Assertions.assertEquals("0300", recibido.getdCodRes());
        Assertions.assertEquals("1234567890123456", recibido.getdProtConsLote());
        responder("fase5-recepcion-lote-0301.xml");
        RespuestaRecepcionLoteDE noEncolado = Sifen.recepcionLoteDE(lote, cfg);
        Assertions.assertEquals("0301", noEncolado.getdCodRes());
        Assertions.assertEquals("Lote no encolado para procesamiento", noEncolado.getdMsgRes());
    }
}
