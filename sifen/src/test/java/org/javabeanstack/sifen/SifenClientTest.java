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

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import org.javabeanstack.sifen.api.CertificateInfo;
import org.javabeanstack.sifen.api.ConnectionDiagnosis;
import org.javabeanstack.sifen.api.NumberRange;
import org.javabeanstack.sifen.api.SignatureVerification;
import org.javabeanstack.sifen.api.SignedDocument;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.EventosDE;
import org.javabeanstack.sifen.core.exceptions.ErrorCategory;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.validation.SifenValidationException;
import org.javabeanstack.sifen.core.validation.ValidationResult;
import org.javabeanstack.sifen.internal.Constants;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Esqueleto de la fachada por instancia (plan JBSSIF, §10.2): construcción por empresa, validación de
 * la configuración por instancia, aislamiento entre dos empresas en hilos distintos, registro por
 * clave y que ninguna operación del contrato termine con {@code JBS-INT-04} (M10-06, §10.7). Sin red.
 *
 * <p>El aislamiento que se prueba es el del cliente (configuración, certificado leído, resultados,
 * ids). El contexto TLS y la caché de certificados siguen siendo estáticos debajo de las peticiones:
 * los independiza §5.2 (M3-64) y los prueba el test multiempresa de §10.5 (CA10-04).</p>
 */
public class SifenClientTest {
    private static final Pattern ID = Pattern.compile("[0-9a-f]{8}");
    /** Certificado de otra «empresa»: persona física apoderada (README de certificados). */
    private static final String PFX_B = "certificados/jbs-sifen-apoderado.p12";

    @TempDir
    Path temporal;

    private static Path recurso(String ruta) {
        URL url = SifenClientTest.class.getClassLoader().getResource(ruta);
        Assertions.assertNotNull(url, ruta);
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static SifenConfig empresaA() {
        return FixturesSifen.configuracionConValidacion();
    }

    private static SifenConfig empresaB() {
        SifenConfig c = FixturesSifen.configuracionConValidacion();
        c.setClientCertificate(recurso(PFX_B).toString());
        return c;
    }

    // ------------------------------------------------------------------ construcción y validación por instancia

    @Test
    public void creaUnClientePorEmpresaConSuPropiaConfiguracion() throws Exception {
        try (ISifenClient a = SifenClient.create(empresaA()); ISifenClient b = SifenClient.create(empresaB())) {
            Assertions.assertNotSame(a, b);
            Assertions.assertEquals(Constants.SDK_CURRENT_VERSION, a.getVersion());
            Assertions.assertEquals(FixturesSifen.rutaPfx().toString(), a.getConfig().getClientCertificate());
            Assertions.assertEquals(recurso(PFX_B).toString(), b.getConfig().getClientCertificate());
            Assertions.assertTrue(a.validateConfig().isValid(), a.validateConfig().toString());
            Assertions.assertTrue(b.validateConfig().isValid(), b.validateConfig().toString());
            CertificateInfo ia = a.getCertificateInfo();
            CertificateInfo ib = b.getCertificateInfo();
            Assertions.assertTrue(ia.isLoaded() && ib.isLoaded());
            Assertions.assertNotEquals(ia.getDocumentNumber(), ib.getDocumentNumber(), "cada cliente lee su certificado");
            Assertions.assertTrue(ia.getDaysRemaining() > 0);
            Assertions.assertNotNull(ia.getNotAfter());
            Assertions.assertEquals("", ia.getFailureCode());
        }
    }

    @Test
    public void laConfiguracionSeCopiaAlCrearYEnCadaGetConfig() throws Exception {
        SifenConfig cfg = empresaA();
        ISifenClient cliente = SifenClient.create(cfg);
        cfg.setClientCertificate("/no/existe.pfx");
        cfg.setEnvironment(SifenConfig.Environment.PROD);
        Assertions.assertTrue(cliente.validateConfig().isValid(), "el cliente no ve el cambio de la original");
        Assertions.assertEquals(SifenConfig.Environment.DEV, cliente.getConfig().getEnvironment());
        SifenConfig copia = cliente.getConfig();
        copia.setClientCertificate("/no/existe.pfx");
        Assertions.assertNotSame(copia, cliente.getConfig());
        Assertions.assertTrue(cliente.validateConfig().isValid(), "ni el de la copia devuelta");
    }

    @Test
    public void unaConfiguracionInvalidaNoCreaClienteYTraeLaListaCompleta() {
        SifenConfig mala = empresaA();
        mala.setClientCertificate(temporal.resolve("no-existe.pfx").toString());
        mala.setCsc(null);
        SifenValidationException e = Assertions.assertThrows(SifenValidationException.class, () -> SifenClient.create(mala));
        Set<String> codigos = new HashSet<>();
        e.getErrors().forEach(v -> codigos.add(v.getCode()));
        Assertions.assertTrue(codigos.contains("JBS-CERT-01"), codigos.toString());
        Assertions.assertTrue(codigos.contains("JBS-CFG-07"), codigos.toString());
        Assertions.assertTrue(ID.matcher(e.getCorrelationId()).matches(), e.getCorrelationId());
        Assertions.assertTrue(e.getMessage().endsWith("(ref. " + e.getCorrelationId() + ")"));

        SifenException nula = Assertions.assertThrows(SifenException.class, () -> SifenClient.create(null));
        Assertions.assertEquals("JBS-CFG-01", nula.getCode());
        Assertions.assertEquals(ErrorCategory.CONFIGURATION, nula.getCategory());
    }

    @Test
    public void validateConfigEsPorInstanciaYDetectaElCertificadoBorrado() throws Exception {
        Path pfx = temporal.resolve("empresa-b.pfx");
        Files.copy(FixturesSifen.rutaPfx(), pfx);
        SifenConfig cfgB = empresaA();
        cfgB.setClientCertificate(pfx.toString());
        ISifenClient a = SifenClient.create(empresaA());
        ISifenClient b = SifenClient.create(cfgB);
        Files.delete(pfx);
        ValidationResult rb = b.validateConfig();
        Assertions.assertFalse(rb.isValid());
        Assertions.assertTrue(rb.contains("JBS-CERT-01"), rb.toString());
        Assertions.assertFalse(b.getCertificateInfo().isLoaded());
        Assertions.assertEquals("JBS-CERT-01", b.getCertificateInfo().getFailureCode());
        Assertions.assertTrue(a.validateConfig().isValid(), "la otra empresa no se entera");
        Assertions.assertTrue(a.getCertificateInfo().isLoaded());
    }

    // ------------------------------------------------------------------ aislamiento entre hilos

    @Test
    public void dosEmpresasEnHilosDistintosNoSeMezclan() throws Exception {
        ISifenClient a = SifenClient.create(empresaA());
        ISifenClient b = SifenClient.create(empresaB());
        String docA = a.getCertificateInfo().getDocumentNumber();
        String docB = b.getCertificateInfo().getDocumentNumber();
        String pfxA = a.getConfig().getClientCertificate();
        String pfxB = b.getConfig().getClientCertificate();
        int vueltas = 100;
        CountDownLatch largada = new CountDownLatch(1);
        Set<String> ids = Collections.synchronizedSet(new HashSet<>());
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<Integer>> futuros = new ArrayList<>();
            for (int h = 0; h < 4; h++) {
                ISifenClient c = h % 2 == 0 ? a : b;
                String doc = h % 2 == 0 ? docA : docB;
                String pfx = h % 2 == 0 ? pfxA : pfxB;
                futuros.add(pool.submit(() -> {
                    largada.await();
                    int ok = 0;
                    for (int i = 0; i < vueltas; i++) {
                        Assertions.assertEquals(doc, c.getCertificateInfo().getDocumentNumber());
                        Assertions.assertEquals(pfx, c.getConfig().getClientCertificate());
                        Assertions.assertTrue(c.validateConfig().isValid());
                        Assertions.assertNotNull(c.validateDE(CasosDePrueba.documentos().get(4).construir()));
                        ConnectionDiagnosis d = c.checkConnection();
                        Assertions.assertTrue(ids.add(d.getOperationId()), "id de operación repetido");
                        ok++;
                    }
                    return ok;
                }));
            }
            largada.countDown();
            for (Future<Integer> f : futuros) {
                Assertions.assertEquals(vueltas, f.get(60, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }
        Assertions.assertEquals(4 * vueltas, ids.size());
    }

    // ------------------------------------------------------------------ registro por clave

    @Test
    public void elRegistroLlamaAlProveedorUnaVezPorClave() throws Exception {
        SifenClientRegistry registro = new SifenClientRegistry();
        AtomicInteger llamadas = new AtomicInteger();
        int hilos = 16;
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        try {
            List<Future<ISifenClient>> futuros = new ArrayList<>();
            for (int i = 0; i < hilos; i++) {
                Callable<ISifenClient> tarea = () -> {
                    largada.await();
                    return registro.get("empresa-1", () -> {
                        llamadas.incrementAndGet();
                        return empresaA();
                    });
                };
                futuros.add(pool.submit(tarea));
            }
            largada.countDown();
            ISifenClient primero = futuros.get(0).get(60, TimeUnit.SECONDS);
            for (Future<ISifenClient> f : futuros) {
                Assertions.assertSame(primero, f.get(60, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }
        Assertions.assertEquals(1, llamadas.get());
        Assertions.assertEquals(Set.of("empresa-1"), registro.keys());
    }

    @Test
    public void unaFallaNoQuedaGuardadaEInvalidarReleeLaConfiguracion() throws Exception {
        SifenClientRegistry registro = new SifenClientRegistry();
        SifenException e = Assertions.assertThrows(SifenException.class, () -> registro.get("e", () -> null));
        Assertions.assertEquals("JBS-CFG-01", e.getCode());
        SifenException propia = Assertions.assertThrows(SifenException.class, () -> registro.get("e", () -> {
            throw new IllegalStateException("falla del proveedor");
        }));
        Assertions.assertEquals("JBS-INT-00", propia.getCode());
        Assertions.assertTrue(registro.keys().isEmpty(), "la falla no se guarda");

        ISifenClient a = registro.get("e", SifenClientTest::empresaA);
        Assertions.assertSame(a, registro.get("e", () -> {
            throw new AssertionError("no se vuelve a llamar");
        }));
        registro.invalidate("e");
        Assertions.assertTrue(registro.keys().isEmpty());
        ISifenClient b = registro.get("e", SifenClientTest::empresaB);
        Assertions.assertNotSame(a, b);
        Assertions.assertTrue(a.validateConfig().isValid(), "el cliente viejo sigue sirviendo");
        Assertions.assertEquals(recurso(PFX_B).toString(), b.getConfig().getClientCertificate());

        registro.get("otra", SifenClientTest::empresaA);
        Assertions.assertEquals(List.of("e", "otra"), new ArrayList<>(registro.keys()));
        registro.closeAll();
        Assertions.assertTrue(registro.keys().isEmpty());
        registro.closeAll();
        Assertions.assertThrows(NullPointerException.class, () -> registro.get(null, SifenClientTest::empresaA));
    }

    // ------------------------------------------------------------------ operaciones sin lanzar

    @Test
    public void lasValidacionesNoLanzan() throws Exception {
        ISifenClient c = SifenClient.create(empresaA());
        Assertions.assertTrue(c.validateDE(null).contains("JBS-VAL-01"));
        Assertions.assertTrue(c.validateEvents(null).contains("JBS-VAL-01"));
        Assertions.assertTrue(c.validateEvents(new EventosDE()).contains("JBS-VAL-01"));
        Assertions.assertNotNull(c.validateDE(CasosDePrueba.documentos().get(4).construir()));
    }

    @Test
    public void verifySignatureDaVeredictoOFallaSinLanzar() throws Exception {
        ISifenClient c = SifenClient.create(empresaA());
        SignatureVerification vacia = c.verifySignature("");
        Assertions.assertTrue(vacia.isFailure());
        Assertions.assertFalse(vacia.isValid());
        Assertions.assertEquals("JBS-XML-01", vacia.getCode());
        Assertions.assertEquals(ErrorCategory.INPUT, vacia.getCategory());
        Assertions.assertTrue(ID.matcher(vacia.getCorrelationId()).matches(), vacia.getCorrelationId());

        SignatureVerification firmada = c.verifySignature(FixturesSifen.recurso("golden/DE-01.xml"));
        Assertions.assertFalse(firmada.isFailure(), firmada.toString());
        Assertions.assertTrue(firmada.isValid(), firmada.getReason());
        Assertions.assertFalse(firmada.getSubjects().isEmpty());
        Assertions.assertEquals("", firmada.getReason());
        Assertions.assertNull(firmada.getCategory());
        Assertions.assertEquals("", firmada.getCorrelationId());
    }

    /**
     * {@code checkConnection} no lanza: contra un nombre que no resuelve (sin red, M3-62) da sus etapas y falla,
     * sin {@code JBS-INT-04} (§5.3 implementada; nombre al día por M10-06).
     */
    @Test
    public void checkConnectionDiagnosticaSinLanzarContraUnNombreQueNoResuelve() throws Exception {
        ConnectionDiagnosis d = SifenClient.create(sinRed(empresaA())).checkConnection();
        Assertions.assertNotNull(d);
        Assertions.assertFalse(d.isOk(), "el nombre .invalid no resuelve");
        Assertions.assertFalse(d.getSteps().isEmpty());
        Assertions.assertTrue(ID.matcher(d.getOperationId()).matches());
        for (ConnectionDiagnosis.Step step : d.getSteps()) {
            Assertions.assertNotEquals("JBS-INT-04", step.getCode());
        }
    }

    /** La misma configuración contra un nombre que el resolver de los tests rechaza sin consultar (M3-62). */
    private static SifenConfig sinRed(SifenConfig c) {
        c.setBaseUrl("https://sifen-jbs-prueba.invalid:443");
        return c;
    }

    // ------------------------------------------------------------------ contrato de las operaciones (M10-06)

    /**
     * Todas las operaciones de emisión, lote y eventos cumplen el contrato contra un nombre que no resuelve:
     * nunca {@code null} ni otra excepción que {@code SifenException}, con código, categoría e id, y ninguna
     * termina con {@code JBS-INT-04} (§10.3/§10.4 implementadas; nombre al día por M10-06). Sin red: una
     * implementación que llegue a la red falla sin salir de la máquina.
     */
    @Test
    public void lasOperacionesDelContratoNuncaDevuelvenNullNiJbsInt04() throws Exception {
        ISifenClient c = SifenClient.create(sinRed(empresaA()));
        SignedDocument doc = SignedDocument.builder().cdc(CasosDePrueba.CDC_FACTURA_SIMPLE).build();
        List<Callable<Object>> operaciones = List.of(
            () -> c.sign(CasosDePrueba.documentos().get(4).construir()),
            () -> c.readSigned("<rDE/>"),
            () -> c.send(doc),
            () -> c.resolve(doc),
            () -> c.sendBatch(List.of(doc)),
            () -> c.cancel(CasosDePrueba.CDC_FACTURA_SIMPLE, "motivo de prueba"),
            () -> c.invalidateNumbers(NumberRange.builder().timbrado("12345678").build()),
            () -> c.sendEvents(new EventosDE()));
        String[] metodos = {"sign", "readSigned", "send", "resolve", "sendBatch", "cancel", "invalidateNumbers", "sendEvents"};
        for (int i = 0; i < operaciones.size(); i++) {
            Object resultado;
            try {
                resultado = operaciones.get(i).call();
            } catch (Throwable t) {
                Assertions.assertInstanceOf(SifenException.class, t, metodos[i] + ": solo SifenException");
                SifenException e = (SifenException) t;
                Assertions.assertNotNull(e.getCode(), metodos[i]);
                Assertions.assertNotNull(e.getCategory(), metodos[i]);
                Assertions.assertTrue(ID.matcher(e.getCorrelationId()).matches(), metodos[i]);
                Assertions.assertNotEquals("JBS-INT-04", e.getCode(), metodos[i] + ": ya está implementada");
                continue;
            }
            // ya implementada (p. ej. sign de un DE válido, que es local): nunca null
            Assertions.assertNotNull(resultado, metodos[i]);
        }
    }

    @Test
    public void closeEsIdempotente() throws Exception {
        ISifenClient c = SifenClient.create(empresaA());
        c.close();
        c.close();
        Assertions.assertTrue(c.validateConfig().isValid(), "las operaciones en curso o posteriores no fallan por el cierre");
    }
}
