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
package org.javabeanstack.sifen.core.validation;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Properties;
import java.util.Set;
import org.javabeanstack.sifen.Sifen;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.exceptions.SifenExceptionCodes;
import org.javabeanstack.sifen.core.validation.support.LoadedCertificate;
import org.javabeanstack.sifen.internal.cert.CertificateStore;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Reglas de la configuración y del certificado (plan JBSSIF, §3.7, CA3-08): un caso por regla
 * {@code JBS-CFG-*} / {@code JBS-CERT-*}, la invalidación de la caché por huella (el {@code .pfx}
 * borrado en caliente da {@code JBS-CERT-01} en la operación siguiente), la carga con la lista completa
 * y que ningún secreto llegue al mensaje.
 *
 * <p>Sin red (D28): las operaciones que se ejercitan fallan en la validación, antes de conectar, y
 * además apuntan a {@code 127.0.0.1}.</p>
 */
public class ConfiguracionValidatorTest {
    /** Dentro de la vigencia del autofirmado (2026-11-23 → 2036-11-20). */
    private static final Clock HOY = Clock.fixed(Instant.parse("2026-12-01T12:00:00Z"), ZoneOffset.UTC);
    private static final ConfigValidator V = ConfigValidator.withClock(HOY);

    @TempDir
    Path tmp;

    @BeforeEach
    @AfterEach
    public void vaciarAlmacen() {
        CertificateStore.clear();
    }

    private static SifenConfig cfg() {
        SifenConfig c = FixturesSifen.configuracion();
        c.setBaseUrl("https://127.0.0.1:9");
        return c;
    }

    private static Path recurso(String nombre) {
        return FixturesSifen.rutaPfx().resolveSibling(nombre);
    }

    private static void contiene(ValidationResult r, String codigo, boolean error) {
        Assertions.assertTrue(r.contains(codigo), codigo + " ausente: " + r);
        Assertions.assertEquals(error, r.find(codigo).get(0).isError(), codigo + ": severidad");
        Assertions.assertEquals(!error || !r.isValid(), true);
        Assertions.assertEquals(ValidationRule.normalizePath(r.find(codigo).get(0).getPath()),
                RuleCatalog.byCode(codigo).orElseThrow().getPath(), codigo + ": ruta canónica");
    }

    // ------------------------------------------------------------------ CFG

    @Test
    public void laConfiguracionDeLasPruebasNoTieneObservaciones() {
        ValidationResult r = V.validate(cfg());
        Assertions.assertTrue(r.isEmpty(), r.toString());
    }

    @Test
    public void cfg01ConfiguracionNula() {
        ValidationResult r = V.validate(null);
        contiene(r, "JBS-CFG-01", true);
        Assertions.assertEquals("sifenConfig", r.find("JBS-CFG-01").get(0).getPath());
        SifenException e = Assertions.assertThrows(SifenException.class, () -> Sifen.setSifenConfig(null));
        Assertions.assertEquals(SifenExceptionCodes.INVALID_CONFIGURATION_CODE, e.getLegacyCode());
        // D42, §11.4: getCode() devuelve el código estable (la primera regla con error), no el iNNN
        Assertions.assertEquals(((SifenValidationException) e).getResult().getErrors().get(0).getCode(), e.getCode());
    }

    @Test
    public void cfg02SinAmbiente() {
        SifenConfig c = cfg();
        c.setEnvironment(null);
        contiene(V.validate(c), "JBS-CFG-02", true);
    }

    @Test
    public void cfg03UrlNoAbsoluta() {
        SifenConfig c = cfg();
        c.setBaseUrl("sifen test");
        contiene(V.validate(c), "JBS-CFG-03", true);
        c.setBaseUrl("/de/ws");
        contiene(V.validate(c), "JBS-CFG-03", true);
    }

    @Test
    public void cfg04SinHttps() {
        SifenConfig c = cfg();
        c.setBaseUrl("http://127.0.0.1:9");
        contiene(V.validate(c), "JBS-CFG-04", true);
    }

    @Test
    public void cfg05HostDeOtroAmbiente() {
        SifenConfig c = cfg();
        c.setBaseUrl("https://sifen.set.gov.py");
        contiene(V.validate(c), "JBS-CFG-05", true);
        c.setBaseUrl("https://sifen-test.set.gov.py");
        Assertions.assertFalse(V.validate(c).contains("JBS-CFG-05"));
        c.setBaseUrl("https://localhost:8443");
        Assertions.assertFalse(V.validate(c).contains("JBS-CFG-05"), "un host local o un proxy no se juzga");
    }

    @Test
    public void cfg06FaltaUnServicio() {
        SifenConfig c = cfg();
        c.setSendPath(" ");
        c.setQueryDEPath(null);
        ValidationResult r = V.validate(c);
        contiene(r, "JBS-CFG-06", true);
        Assertions.assertTrue(r.find("JBS-CFG-06").get(0).getMessage().contains("pathRecibe, pathConsulta"), r.toString());
    }

    @Test
    public void cfg07FaltaElCsc() {
        SifenConfig c = cfg();
        c.setCsc(null);
        contiene(V.validate(c), "JBS-CFG-07", true);
    }

    @Test
    public void cfg08CscGenericoEnProduccion() {
        SifenConfig c = cfg();
        c.setEnvironment(SifenConfig.Environment.PROD);
        ValidationResult r = V.validate(c);
        contiene(r, "JBS-CFG-08", true);
        Assertions.assertFalse(r.toString().contains("ABCD0000"), "el CSC no se muestra");
    }

    @Test
    public void cfg09FormatoDelCsc() {
        SifenConfig c = cfg();
        c.setCscId(null); // M3-19: queda «null»
        contiene(V.validate(c), "JBS-CFG-09", true);
        c.setCscId("1");
        c.setCsc("corto");
        ValidationResult r = V.validate(c);
        contiene(r, "JBS-CFG-09", true);
        Assertions.assertFalse(r.toString().contains("corto"), "el CSC no se muestra");
    }

    @Test
    public void cfg10ValoresNoAdmitidosConLaListaCompleta() throws Exception {
        Properties p = propiedades("sifen.ambiente=DESARROLLO\nsifen.certificado_cliente.tipo=PEM\nsifen.validacion_previa=si\n");
        SifenValidationException e = Assertions.assertThrows(SifenValidationException.class, () -> SifenConfig.load(p));
        Assertions.assertEquals(SifenExceptionCodes.INVALID_CONFIGURATION_CODE, e.getLegacyCode());
        // D42, §11.4: getCode() devuelve el código estable (la primera regla con error), no el iNNN
        Assertions.assertEquals(((SifenValidationException) e).getResult().getErrors().get(0).getCode(), e.getCode());
        String m = e.getResult().find("JBS-CFG-10").get(0).getMessage();
        Assertions.assertTrue(m.contains("sifen.ambiente") && m.contains("sifen.certificado_cliente.tipo")
                && m.contains("sifen.validacion_previa"), m);
        Assertions.assertFalse(e.getResult().contains("JBS-CFG-02"), "el ambiente inválido no se informa dos veces");
        Assertions.assertTrue(e.getResult().contains("JBS-CERT-13"), "toda la lista, no el primero: " + e.getResult());
    }

    @Test
    public void e314ClavesAusentesNoDanNullPointer() throws Exception {
        SifenValidationException e = Assertions.assertThrows(SifenValidationException.class,
                () -> SifenConfig.load(new Properties()));
        contiene(e.getResult(), "JBS-CFG-02", true);
        contiene(e.getResult(), "JBS-CERT-13", true);
    }

    @Test
    public void cfg11ClaveDesconocidaEsAviso() throws Exception {
        SifenConfig c = SifenConfig.load(propiedades("sifen.ambiente=DEV\nsifen.ambeinte=DEV\nsifen.certificado_cliente.tipo=PFX\n"));
        ValidationResult r = V.validate(c);
        contiene(r, "JBS-CFG-11", false);
        Assertions.assertTrue(r.isValid());
    }

    @Test
    public void cfg12SinCertificadoDeClienteYSuDesactivacion() {
        SifenConfig c = cfg();
        c.setUseClientCertificate(false);
        contiene(V.validate(c), "JBS-CFG-12", true);
        c.setDisabledRules(Set.of("JBS-CFG-12"));
        Assertions.assertTrue(V.validate(c).isValid(), "con un proxy que autentica se desactiva");
    }

    @Test
    public void cfg13TiemposDeEsperaConstantes() {
        ValidationRule r = RuleCatalog.byCode("JBS-CFG-13").orElseThrow();
        Assertions.assertFalse(r.isEvaluable());
        Assertions.assertEquals(NotEvaluatedReason.CONSTANT, r.getNotEvaluatedReason());
    }

    @Test
    public void cfg14DesactivarLoQueNoExisteOUnaGuarda() {
        SifenConfig c = cfg();
        c.setDisabledRules(Set.of("NO-EXISTE", "E605", "D105"));
        ValidationResult r = V.validate(c);
        contiene(r, "JBS-CFG-14", false);
        String m = r.find("JBS-CFG-14").get(0).getMessage();
        Assertions.assertTrue(m.contains("NO-EXISTE") && m.contains("E605") && !m.contains("D105"), m);
        c.setDisabledRules(Set.of("APP-D-01"));
        Assertions.assertFalse(V.validate(c).contains("JBS-CFG-14"), "las reglas propias del consumidor no están en el catálogo");
    }

    /** M3-15: las guardas anti-NPE no se desactivan ni por configuración ni por el builder. */
    @Test
    public void lasGuardasNoSeDesactivan() {
        SifenConfig c = cfg();
        c.setDisabledRules(Set.of("E605", "D105"));
        ValidationContext ctx = new ValidationContext(FixturesSifen.contexto(c), HOY, Set.of(), null);
        // E3-27 (§3.8, ciclo 2): suman JBS-E-83, JBS-E-85 (E2-03) y JBS-EVENTO-40 (E0-18)
        for (String guarda : new String[] {"E605", "E605b", "E640", "JBS-D-03", "H001", "JBS-VAL-03", "JBS-E-83", "JBS-E-85"}) {
            ValidationRule r = RuleCatalog.byCode(guarda).orElseThrow();
            Assertions.assertFalse(r.isDisableable(), guarda);
            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> DEValidator.builder().registerStandard().disable(guarda).build(), guarda);
        }
        Assertions.assertFalse(RuleCatalog.byCode("JBS-EVENTO-40").orElseThrow().isDisableable());
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> EventValidator.builder().registerStandard().disable("JBS-EVENTO-40").build());
        // I3-60: la lista de guardas es cerrada (igualdad de conjuntos contra validacion/no_desactivables.tsv,
        // con el motivo de cada una): una guarda nueva sin registrar, o una registrada que vuelve a ser
        // desactivable, pone el test en rojo
        java.util.Set<String> noDesactivables = new java.util.TreeSet<>();
        for (ValidationRule r : RuleCatalog.all()) {
            if (!r.isDisableable()) {
                noDesactivables.add(r.getCode());
            }
        }
        Assertions.assertEquals(guardasRegistradas(), noDesactivables);
        Assertions.assertTrue(ctx.isRuleActive(RuleCatalog.byCode("E605").orElseThrow()));
        Assertions.assertFalse(ctx.isRuleActive(RuleCatalog.byCode("D105").orElseThrow()));
    }

    /** Códigos de {@code validacion/no_desactivables.tsv}; cada fila con su motivo. */
    private static java.util.Set<String> guardasRegistradas() {
        java.util.Set<String> l = new java.util.TreeSet<>();
        try (java.io.InputStream in = ConfiguracionValidatorTest.class.getClassLoader()
                .getResourceAsStream("validacion/no_desactivables.tsv")) {
            Assertions.assertNotNull(in, "falta validacion/no_desactivables.tsv");
            for (String fila : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                if (fila.isBlank() || fila.startsWith("#")) {
                    continue;
                }
                String[] c = fila.split("\t", -1);
                Assertions.assertTrue(c.length == 2 && !c[1].isBlank(), "fila sin motivo: " + fila);
                Assertions.assertTrue(l.add(c[0]), "fila repetida: " + c[0]);
            }
        } catch (java.io.IOException e) {
            Assertions.fail(e);
        }
        return l;
    }

    /** I3-24: el getter no expone el conjunto interno. */
    @Test
    public void lasDesactivadasDeLaConfiguracionNoSeModificanPorElGetter() {
        SifenConfig c = cfg();
        c.setDisabledRules(Set.of("D105"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> c.getDisabledRules().add("X"));
    }

    // ------------------------------------------------------------------ CERT

    @Test
    public void cert01ArchivoInexistenteONoIndicado() {
        SifenConfig c = cfg();
        c.setClientCertificate(tmp.resolve("no-existe.pfx").toString());
        ValidationResult r = V.validate(c);
        contiene(r, "JBS-CERT-01", true);
        Assertions.assertTrue(r.find("JBS-CERT-01").get(0).getMessage().contains("no-existe.pfx"), r.toString());
        c.setClientCertificate(null);
        contiene(V.validate(c), "JBS-CERT-01", true);
        // una ruta larga borrada no se confunde con contenido en línea (el MIME ignora «.», «_», «-»)
        String larga = tmp.resolve("carpeta_de_certificados_de_la_empresa_" + "x".repeat(150) + "/firma-2026.pfx").toString();
        c.setClientCertificate(larga);
        contiene(V.validate(c), "JBS-CERT-01", true);
        c.setClientCertificate("C:\\Certificados\\empresa_sin_extension");
        contiene(V.validate(c), "JBS-CERT-01", true);
    }

    @Test
    public void cert02EsUnDirectorio() {
        SifenConfig c = cfg();
        c.setClientCertificate(tmp.toString());
        contiene(V.validate(c), "JBS-CERT-02", true);
    }

    @Test
    public void cert03ContrasenaIncorrecta() {
        SifenConfig c = cfg();
        c.setClientCertificatePassword("otra-clave-secreta");
        ValidationResult r = V.validate(c);
        contiene(r, "JBS-CERT-03", true);
        Assertions.assertFalse(r.toString().contains("otra-clave-secreta"));
    }

    @Test
    public void cert04SinContrasena() {
        SifenConfig c = cfg();
        c.setClientCertificatePassword(" ");
        contiene(V.validate(c), "JBS-CERT-04", true);
    }

    @Test
    public void cert05NoEsPkcs12() {
        SifenConfig c = cfg();
        c.setClientCertificate(recurso("jbs-sifen-no-pkcs12.pem").toString());
        contiene(V.validate(c), "JBS-CERT-05", true);
    }

    @Test
    public void cert06SinClavePrivada() {
        SifenConfig c = cfg();
        c.setClientCertificate(recurso("jbs-sifen-sin-clave.p12").toString());
        contiene(V.validate(c), "JBS-CERT-06", true);
    }

    @Test
    public void cert07DosEntradasEsAviso() {
        SifenConfig c = cfg();
        c.setClientCertificate(recurso("jbs-sifen-dos-alias.p12").toString());
        ValidationResult r = V.validate(c);
        contiene(r, "JBS-CERT-07", false);
        Assertions.assertTrue(r.isValid(), r.toString());
    }

    @Test
    public void cert08Vencido() {
        contiene(ConfigValidator.withClock(reloj("2040-01-01T00:00:00Z")).validate(cfg()), "JBS-CERT-08", true);
    }

    @Test
    public void cert09PorVencerEsAviso() {
        ValidationResult r = ConfigValidator.withClock(reloj("2036-11-01T00:00:00Z")).validate(cfg());
        contiene(r, "JBS-CERT-09", false);
        Assertions.assertTrue(r.isValid());
    }

    @Test
    public void cert10TodaviaNoVigente() {
        contiene(ConfigValidator.withClock(reloj("2020-01-01T00:00:00Z")).validate(cfg()), "JBS-CERT-10", true);
    }

    @Test
    public void cert11RucDelEmisorDistintoDelCertificado() {
        SifenConfig c = cfg();
        DocumentoElectronico de = CasosDePrueba.documentos().get(4).construir();
        Assertions.assertTrue(V.validateConsistency(de, c).isEmpty(), "el emisor de los fixtures es el del certificado");
        de.getgDatGralOpe().getgEmis().setdRucEm("80654321");
        de.getgDatGralOpe().getgEmis().setdDVEmi("3");
        ValidationResult r = V.validateConsistency(de, c);
        contiene(r, "JBS-CERT-11", true);
        // E3-32 (§3.8 ciclo 2, orden del coordinador): el mensaje lista los RUC del certificado (dato de la
        // empresa, no personal) para que el usuario vea con qué RUC firma; una CI nunca se muestra
        Assertions.assertTrue(r.find("JBS-CERT-11").get(0).getMessage().contains("RUC del certificado: 80123456-5"), r.toString());
        // generarXml con la validación previa: tercera etapa, después del contenido
        SifenConfig con = FixturesSifen.configuracionConValidacion();
        con.setDisabledRules(Set.of("D105", "JBS-E-70")); // la leyenda de pruebas no es lo que se mira
        SifenValidationException e = Assertions.assertThrows(SifenValidationException.class,
                () -> de.generarXml(FixturesSifen.contexto(con), con));
        Assertions.assertTrue(e.getResult().contains("JBS-CERT-11"), e.getMessage());
        con.setDisabledRules(Set.of("D105", "JBS-E-70", "JBS-CERT-11"));
        Assertions.assertDoesNotThrow(() -> de.generarXml(FixturesSifen.contexto(con), con));
    }

    /**
     * Certificado de una persona física apoderada (C:M3-44, como el cualificado tributario F1 real):
     * el sujeto lleva la CI del titular y el RUC de la empresa viaja en el {@code dirName} del SAN.
     * El emisor coincide con el RUC del SAN, así que JBS-CERT-11 no aplica y el XML se genera.
     */
    @Test
    public void cert11ApoderadoConElRucEnElSanCoincide() {
        SifenConfig c = cfg();
        c.setClientCertificate(recurso("jbs-sifen-apoderado.p12").toString());
        DocumentoElectronico de = CasosDePrueba.documentos().get(4).construir();
        ValidationResult r = V.validateConsistency(de, c);
        Assertions.assertFalse(r.contains("JBS-CERT-11"), r.toString());
        SifenConfig con = FixturesSifen.configuracionConValidacion();
        con.setClientCertificate(c.getClientCertificate());
        con.setDisabledRules(Set.of("D105", "JBS-E-70"));
        Assertions.assertDoesNotThrow(() -> de.generarXml(FixturesSifen.contexto(con), con));
        // el titular persona física (RUC = CI del sujeto, sin DV en el certificado) también coincide:
        // se compara contra todos los sujetos, no se prefiere el del SAN
        de.getgDatGralOpe().getgEmis().setdRucEm("1234567");
        de.getgDatGralOpe().getgEmis().setdDVEmi("9"); // I3-59: DV calculado de la CI, como la firma
        Assertions.assertFalse(V.validateConsistency(de, c).contains("JBS-CERT-11"), "titular persona física");
        de.getgDatGralOpe().getgEmis().setdDVEmi("0");
        Assertions.assertTrue(V.validateConsistency(de, c).contains("JBS-CERT-11"), "titular persona física con otro DV");
        // otro emisor sigue sin coincidir: ni la CI del sujeto ni el RUC del SAN
        de.getgDatGralOpe().getgEmis().setdRucEm("80654321");
        de.getgDatGralOpe().getgEmis().setdDVEmi("3");
        contiene(V.validateConsistency(de, c), "JBS-CERT-11", true);
        // E3-32 (app-developer): el mensaje lista los RUC del certificado y nunca la CI del titular
        String mensaje = V.validateConsistency(de, c).find("JBS-CERT-11").get(0).getMessage();
        Assertions.assertTrue(mensaje.contains("RUC del certificado: 80123456-5"), mensaje);
        Assertions.assertFalse(mensaje.contains("1234567"), "la CI del titular no se muestra: " + mensaje);
        LoadedCertificate info = ConfigValidator.certificate(c);
        Assertions.assertEquals(java.util.List.of("80123456-5"), info.getRucs());
        Assertions.assertFalse(info.toString().contains("1234567"), info.toString());
    }

    @Test
    public void cert12SinFirmaDigitalNiNumeroEsAviso() {
        SifenConfig c = cfg();
        c.setClientCertificate(recurso("jbs-sifen-sin-firma.p12").toString());
        ValidationResult r = V.validate(c);
        contiene(r, "JBS-CERT-12", false);
        Assertions.assertTrue(r.isValid());
    }

    @Test
    public void cert13TipoNoIndicado() {
        SifenConfig c = cfg();
        c.setClientCertificateType(null);
        contiene(V.validate(c), "JBS-CERT-13", true);
    }

    @Test
    public void cert14ContenidoEnLinea() throws Exception {
        SifenConfig c = cfg();
        c.setClientCertificate("QUJDRA=="); // Base64 que no es un PKCS#12 (4 bytes)
        contiene(V.validate(c), "JBS-CERT-14", true);
        c.setClientCertificate("certificadoempresa");
        contiene(V.validate(c), "JBS-CERT-14", true);
        String enLinea = Base64.getEncoder().encodeToString(Files.readAllBytes(FixturesSifen.rutaPfx()));
        c.setClientCertificate(enLinea);
        ValidationResult r = V.validate(c);
        Assertions.assertTrue(r.isEmpty(), r.toString());
        Assertions.assertTrue(ConfigValidator.certificate(c).isInline());
        Assertions.assertFalse(ConfigValidator.certificate(c).toString().contains(enLinea.substring(0, 40)));
    }

    /** CA3-08: el archivo que cambia se recarga (aviso una vez) y el que se borra da JBS-CERT-01 en la operación siguiente. */
    @Test
    public void cert15YElPfxBorradoEnCaliente() throws Exception {
        Path pfx = FixturesSifen.copiarPfx(tmp.resolve("empresa.pfx"));
        SifenConfig c = cfg();
        c.setClientCertificate(pfx.toString());
        Assertions.assertTrue(V.validate(c).isEmpty());
        LoadedCertificate primero = ConfigValidator.certificate(c);
        Assertions.assertSame(primero, ConfigValidator.certificate(c), "sin cambios, la misma carga");

        Files.copy(recurso("jbs-sifen-dos-alias.p12"), pfx, StandardCopyOption.REPLACE_EXISTING);
        Files.setLastModifiedTime(pfx, java.nio.file.attribute.FileTime.from(Instant.parse("2030-01-01T00:00:00Z")));
        ValidationResult r = V.validate(c);
        contiene(r, "JBS-CERT-15", false);
        contiene(r, "JBS-CERT-07", false);
        Assertions.assertFalse(V.validate(c).contains("JBS-CERT-15"), "el aviso de cambio sale una vez");

        Files.delete(pfx);
        SifenValidationException e = Assertions.assertThrows(SifenValidationException.class, () -> Sifen.consultaRUC("80123456", c));
        Assertions.assertTrue(e.getResult().contains("JBS-CERT-01"), e.getMessage());
        Assertions.assertEquals(SifenExceptionCodes.INVALID_CONFIGURATION_CODE, e.getLegacyCode());
        // D42, §11.4: getCode() devuelve el código estable (la primera regla con error), no el iNNN
        Assertions.assertEquals(((SifenValidationException) e).getResult().getErrors().get(0).getCode(), e.getCode());
        // sin validación previa, la firma tampoco da NullPointerException: desde §3.8 sale con el código de
        // la regla (la línea base emitía i100, I3-22), su categoría y el id de correlación
        SifenException firma = Assertions.assertThrows(SifenException.class,
                () -> CasosDePrueba.documentos().get(4).construir().generarXml(FixturesSifen.contexto(c), c));
        Assertions.assertEquals("JBS-CERT-01", firma.getCode(), firma.toString());
        Assertions.assertEquals(org.javabeanstack.sifen.core.exceptions.ErrorCategory.CERTIFICATE, firma.getCategory());
        Assertions.assertNotNull(firma.getCorrelationId());
        Assertions.assertTrue(firma.getMessage().startsWith(ValidationMessages.message("JBS-CERT-01", pfx.toString())), firma.getMessage());
        Assertions.assertTrue(e.getMessage().endsWith("(ref. " + e.getCorrelationId() + ")"), e.getMessage());
        Assertions.assertEquals("JBS-CERT-01", e.getCode(), "la configuración inválida lleva el código de su primera regla");
    }

    @Test
    public void elDiagnosticoNoLanzaYRecarga() {
        SifenConfig c = cfg();
        c.setClientCertificatePassword("mal");
        ValidationResult r = Sifen.validarConfiguracion(c);
        Assertions.assertTrue(r.contains("JBS-CERT-03"));
        LoadedCertificate ok = ConfigValidator.certificate(cfg());
        Assertions.assertTrue(ok.isLoaded());
        Assertions.assertEquals("80123456-5", ok.getDocumentNumber());
        Assertions.assertEquals("EMPRESA FICTICIA DE PRUEBA S.A.", ok.getHolder());
    }

    @Test
    public void ningunSecretoLlegaAlMensaje() {
        SifenConfig c = cfg();
        c.setEnvironment(SifenConfig.Environment.PROD);
        c.setBaseUrl("http://usuario:clave-proxy@127.0.0.1:9");
        c.setClientCertificatePassword("clave-del-pfx");
        SifenValidationException e = Assertions.assertThrows(SifenValidationException.class, () -> V.require(c));
        for (String secreto : new String[] {"clave-del-pfx", "ABCD0000000000000000000000000000", "clave-proxy"}) {
            Assertions.assertFalse(e.getMessage().contains(secreto), secreto);
            Assertions.assertFalse(e.getResult().toString().contains(secreto), secreto);
        }
    }

    /** I3-21 (a): la ruta de JBS-VAL-00 sale de la raíz del motor, no del tipo del objeto validado. */
    @Test
    public void unConjuntoQueLanzaReportaEnLaRaizDeSuMotor() {
        RuleSet<org.javabeanstack.sifen.core.fields.request.event.TrGesEve, Object> evento =
                new RuleSet<>("TST-evento-roto", RuleGroup.EVENTO, "rEve") {
                    {
                        declare(rule("TST301").field("").error(), passes(s -> {
                            throw new IllegalStateException("x");
                        }));
                    }

                    @Override
                    protected java.util.List<Subject<org.javabeanstack.sifen.core.fields.request.event.TrGesEve, Object>> subjects(
                            org.javabeanstack.sifen.core.fields.request.event.TrGesEve raiz) {
                        return unique(raiz);
                    }
                };
        ValidationResult re = EventValidator.builder().register(evento).build()
                .validate(new org.javabeanstack.sifen.core.fields.request.event.TrGesEve(), FixturesSifen.contexto(cfg()));
        Assertions.assertEquals("rEve", re.find("JBS-VAL-00").get(0).getPath(), re.toString());
        RuleSet<SifenConfig, SifenConfig> config = new RuleSet<>("TST-config-roto", RuleGroup.CFG, "sifenConfig") {
            {
                declare(rule("TST302").field("").error(), passes(s -> {
                    throw new IllegalStateException("x");
                }));
            }

            @Override
            protected java.util.List<Subject<SifenConfig, SifenConfig>> subjects(SifenConfig raiz) {
                return unique(raiz);
            }
        };
        ValidationResult rc = ValidationEngine.de(java.util.List.<GroupValidator<SifenConfig>>of(config), false, HOY,
                ValidationRule.CONFIG_ROOT).validar(cfg(), FixturesSifen.contexto(cfg()));
        Assertions.assertEquals("sifenConfig", rc.find("JBS-VAL-00").get(0).getPath(), rc.toString());
        Assertions.assertTrue(RuleCatalog.isWellFormedCode("APP-D-01"));
    }

    /**
     * E3-29 (§3.8 ciclo 2): un {@code .properties} en ISO-8859-1 (comentario con tilde) carga por archivo
     * y por flujo con el mismo resultado; un archivo que no existe o un directorio dan {@code JBS-ENT-06}
     * con la causa correcta, nunca «no existe» para uno que existe.
     */
    @Test
    public void elArchivoDeConfiguracionEnIso88591CargaIgualQueElFlujo() throws Exception {
        String texto = "# Configuración de producción ñandú\nsifen.ambiente=DEV\nsifen.certificado_cliente.tipo=PFX\n"
                + "sifen.certificado_cliente.archivo=" + FixturesSifen.rutaPfx().toString().replace("\\", "/") + "\n"
                + "sifen.certificado_cliente.contrasena=" + FixturesSifen.PFX_CLAVE + "\n";
        Path archivo = tmp.resolve("sifen-iso.properties");
        Files.write(archivo, texto.getBytes(StandardCharsets.ISO_8859_1));
        SifenConfig porArchivo = SifenConfig.load(archivo.toFile());
        SifenConfig porFlujo = SifenConfig.load(new ByteArrayInputStream(texto.getBytes(StandardCharsets.ISO_8859_1)));
        Assertions.assertEquals(porFlujo.toString(), porArchivo.toString());
        Path utf8 = tmp.resolve("sifen-utf8.properties");
        Files.write(utf8, texto.getBytes(StandardCharsets.UTF_8));
        Assertions.assertEquals(porArchivo.toString(), SifenConfig.load(utf8.toFile()).toString());

        SifenException noExiste = Assertions.assertThrows(SifenException.class,
                () -> SifenConfig.load(tmp.resolve("no-esta.properties").toFile()));
        Assertions.assertEquals("JBS-ENT-06", noExiste.getCode());
        Assertions.assertTrue(noExiste.getMessage().contains("no existe"), noExiste.getMessage());
        SifenException directorio = Assertions.assertThrows(SifenException.class,
                () -> SifenConfig.load(tmp.toFile()));
        Assertions.assertEquals("JBS-ENT-06", directorio.getCode());
        Assertions.assertTrue(directorio.getMessage().contains("directorio"), directorio.getMessage());
        Assertions.assertFalse(directorio.getMessage().contains("no existe"), directorio.getMessage());
    }

    /** M1-03 (adelantado a §3.8 ciclo 2): {@code SifenConfig.toString()} no muestra la clave del PFX ni el CSC. */
    @Test
    public void elToStringNoMuestraSecretos() {
        SifenConfig c = cfg();
        String t = c.toString();
        Assertions.assertFalse(t.contains(FixturesSifen.PFX_CLAVE), t);
        Assertions.assertFalse(t.contains(c.getCsc()), t);
        Assertions.assertTrue(t.contains("contrasenaCertificadoCliente='****'"), t);
        Assertions.assertTrue(t.contains("certificadoCliente='" + c.getClientCertificate() + "'"), "una ruta se muestra: " + t);
        // M3-60: el PKCS#12 en línea (Base64) lleva la clave privada: solo se muestra su largo
        String base64 = java.util.Base64.getEncoder().encodeToString(new byte[900]);
        c.setClientCertificate(base64);
        String enLinea = c.toString();
        Assertions.assertFalse(enLinea.contains(base64.substring(0, 40)), enLinea);
        Assertions.assertTrue(enLinea.contains("certificadoCliente=<en línea, " + base64.length() + " caracteres>"), enLinea);
        c.setClientCertificate(java.util.Base64.getMimeEncoder().encodeToString(new byte[900]));
        Assertions.assertTrue(c.toString().contains("<en línea, "), "con saltos de línea MIME: " + c);
    }

    private static Clock reloj(String instante) {
        return Clock.fixed(Instant.parse(instante), ZoneOffset.UTC);
    }

    private static Properties propiedades(String texto) throws Exception {
        Properties p = new Properties();
        p.load(new ByteArrayInputStream(texto.getBytes(StandardCharsets.UTF_8)));
        if (p.getProperty("sifen.certificado_cliente.archivo") == null) {
            p.setProperty("sifen.certificado_cliente.archivo", FixturesSifen.rutaPfx().toString());
            p.setProperty("sifen.certificado_cliente.contrasena", FixturesSifen.PFX_CLAVE);
        }
        return p;
    }
}
