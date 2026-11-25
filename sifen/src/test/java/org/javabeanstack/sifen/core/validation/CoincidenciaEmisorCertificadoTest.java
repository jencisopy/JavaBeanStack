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

import java.nio.file.Path;
import java.util.List;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.validation.support.LoadedCertificate;
import org.javabeanstack.sifen.internal.cert.CertificateStore;
import org.javabeanstack.sifen.internal.cert.CertificateSubjects;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Cotejo del emisor con el certificado de firma, JBS-CERT-11 (plan JBSSIF, §3.7; E3-32): el camino de
 * producción con la validación previa activa. Certificados desechables de
 * {@code generar_certs_reglas_cert.sh}: {@code jbs-sifen-apoderado.p12} (CI en el sujeto,
 * {@code RUC80123456-5} en el SAN), {@code jbs-sifen-apoderado-varios.p12} (CI en el sujeto; en el SAN
 * {@code RUC80999999-1}, {@code ruc80123456} en minúscula, que el fork no reconoce, y un {@code dirName} sin
 * {@code SERIALNUMBER}),
 * {@code jbs-sifen-sin-ruc.p12} (persona física: solo la CI, sin SAN ni {@code keyUsage}) y
 * {@code jbs-sifen-sin-firma.p12} (sin {@code SERIALNUMBER} ni SAN). En ningún caso el mensaje muestra la CI.
 *
 * <p>I3-59 (ciclo 3, app-developer): las expectativas siguen la semántica del fork, la misma de la
 * verificación de firma (DV calculado si el certificado no lo trae, prefijo con mayúsculas, sin número
 * rechaza); {@code Cert11IgualQueLaFirmaTest} comprueba que coinciden.</p>
 */
public class CoincidenciaEmisorCertificadoTest {
    private static final String CI = "1234567";
    private static final ConfigValidator V = ConfigValidator.standard();

    @BeforeEach
    @AfterEach
    public void vaciarAlmacen() {
        CertificateStore.clear();
    }

    private static SifenConfig cfg(String certificado) {
        SifenConfig c = FixturesSifen.configuracion();
        c.setBaseUrl("https://127.0.0.1:9");
        c.setClientCertificate(recurso(certificado).toString());
        return c;
    }

    private static Path recurso(String nombre) {
        return FixturesSifen.rutaPfx().resolveSibling(nombre);
    }

    private static DocumentoElectronico de(String ruc, String dv) {
        DocumentoElectronico de = CasosDePrueba.documentos().get(4).construir();
        de.getgDatGralOpe().getgEmis().setdRucEm(ruc);
        de.getgDatGralOpe().getgEmis().setdDVEmi(dv);
        return de;
    }

    /** Valida y devuelve el resultado; si hay JBS-CERT-11 exige que el mensaje no muestre la CI. */
    private static ValidationResult cotejar(String certificado, String ruc, String dv) {
        ValidationResult r = V.validateConsistency(de(ruc, dv), cfg(certificado));
        Assertions.assertFalse(r.toString().contains(CI), () -> "el mensaje muestra la CI: " + r);
        return r;
    }

    @ParameterizedTest(name = "{0}: emisor {1}-{2} ⇒ JBS-CERT-11 {3}")
    @CsvSource({
        // RUC en el SAN con DV: coincide con el mismo DV, no con otro
        "jbs-sifen-apoderado.p12, 80123456, 5, false",
        "jbs-sifen-apoderado.p12, 80123456, 4, true",
        // titular persona física: RUC = CI del sujeto, con el DV calculado (1234567-9); otro DV no coincide (I3-59)
        "jbs-sifen-apoderado.p12, 1234567, 9, false",
        "jbs-sifen-apoderado.p12, 1234567, 0, true",
        // varios dirName: el de otra empresa y «ruc80123456», que el fork no reconoce (prefijo en minúscula, I3-59)
        "jbs-sifen-apoderado-varios.p12, 80123456, 5, true",
        "jbs-sifen-apoderado-varios.p12, 80123456, 9, true",
        "jbs-sifen-apoderado-varios.p12, 80999999, 1, false",
        "jbs-sifen-apoderado-varios.p12, 80999999, 2, true",
        // RUC de otra empresa: ni el sujeto ni ningún dirName
        "jbs-sifen-apoderado-varios.p12, 80654321, 3, true",
        "jbs-sifen-apoderado.p12, 80654321, 3, true",
        // certificado sin ningún RUC (persona física): solo coincide su CI con el DV calculado
        "jbs-sifen-sin-ruc.p12, 80123456, 5, true",
        "jbs-sifen-sin-ruc.p12, 1234567, 9, false",
        "jbs-sifen-sin-ruc.p12, 1234567, 0, true",
        // sujeto sin SERIALNUMBER y sin SAN: sin número rechaza, como JBS-FIRMA-05 en la firma (I3-59)
        "jbs-sifen-sin-firma.p12, 80123456, 5, true"
    })
    public void elEmisorSeCotejaConTodosLosSujetosDelCertificado(String certificado, String ruc, String dv, boolean error) {
        ValidationResult r = cotejar(certificado, ruc, dv);
        Assertions.assertEquals(error, r.contains("JBS-CERT-11"), r::toString);
    }

    @Test
    public void elMensajeListaSoloLosRucDelCertificado() {
        ValidationResult varios = cotejar("jbs-sifen-apoderado-varios.p12", "80654321", "3");
        Assertions.assertTrue(varios.toString().contains("80999999-1") && !varios.toString().contains("80123456"),
                () -> "«ruc80123456» no es un RUC para el fork (I3-59): " + varios);
        ValidationResult sinRuc = cotejar("jbs-sifen-sin-ruc.p12", "80123456", "5");
        Assertions.assertTrue(sinRuc.toString().contains("el certificado no trae RUC"), sinRuc::toString);
        ValidationResult sinNumero = cotejar("jbs-sifen-sin-firma.p12", "80123456", "5");
        Assertions.assertTrue(sinNumero.toString().contains("el certificado no trae ningún número de documento"), sinNumero::toString);
    }

    @Test
    public void sinCertificadoDeClienteOSinEmisorNoSeCoteja() {
        SifenConfig sinCliente = cfg("jbs-sifen-sin-ruc.p12");
        sinCliente.setUseClientCertificate(false);
        Assertions.assertFalse(V.validateConsistency(de("80123456", "5"), sinCliente).contains("JBS-CERT-11"));
        SifenConfig c = cfg("jbs-sifen-sin-ruc.p12");
        Assertions.assertFalse(V.validateConsistency(de(null, null), c).contains("JBS-CERT-11"), "sin dRucEm");
        DocumentoElectronico sinEmisor = de("80123456", "5");
        sinEmisor.getgDatGralOpe().setgEmis(null);
        Assertions.assertFalse(V.validateConsistency(sinEmisor, c).contains("JBS-CERT-11"), "sin gEmis");
        DocumentoElectronico sinDatos = de("80123456", "5");
        sinDatos.setgDatGralOpe(null);
        Assertions.assertFalse(V.validateConsistency(sinDatos, c).contains("JBS-CERT-11"), "sin gDatGralOpe");
        ValidationResult sinCfg = V.validateConsistency(de("80123456", "5"), null);
        // sin configuración no hay certificado que cotejar: solo el aviso de contexto ausente, nunca JBS-VAL-00
        Assertions.assertTrue(sinCfg.isValid() && sinCfg.contains("JBS-VAL-02") && !sinCfg.contains("JBS-VAL-00")
                && !sinCfg.contains("JBS-CERT-11"), sinCfg::toString);
        SifenConfig noCarga = cfg("no-existe.p12");
        Assertions.assertFalse(V.validateConsistency(de("80123456", "5"), noCarga).contains("JBS-CERT-11"),
                "certificado que no carga: lo informan las JBS-CERT de configuración");
    }

    @Test
    public void losDocumentosYRucDelCertificadoCargado() {
        LoadedCertificate varios = CertificateStore.get(cfg("jbs-sifen-apoderado-varios.p12"));
        Assertions.assertTrue(varios.isLoaded());
        Assertions.assertNotNull(varios.getCertificate());
        Assertions.assertEquals(List.of(CI + "-9", "80999999-1"), varios.getDocumentNumbers(),
                "sujeto y dirName con número y prefijo del fork, el DV calculado (I3-59)");
        Assertions.assertEquals(List.of("80999999-1"), varios.getRucs(), "la CI no es un RUC; «ruc…» tampoco (I3-59)");
        Assertions.assertTrue(varios.isDigitalSignature());
        Assertions.assertFalse(varios.toString().contains(CI), varios::toString);

        LoadedCertificate sinRuc = CertificateStore.get(cfg("jbs-sifen-sin-ruc.p12"));
        Assertions.assertEquals(List.of(CI + "-9"), sinRuc.getDocumentNumbers());
        Assertions.assertEquals(List.of(), sinRuc.getRucs());
        Assertions.assertTrue(sinRuc.isDigitalSignature(), "sin keyUsage se admite firmar");
        Assertions.assertFalse(sinRuc.toString().contains(CI), sinRuc::toString);

        LoadedCertificate sinNumero = CertificateStore.get(cfg("jbs-sifen-sin-firma.p12"));
        Assertions.assertEquals(List.of(), sinNumero.getDocumentNumbers());
        Assertions.assertFalse(sinNumero.isDigitalSignature(), "keyUsage sin digitalSignature");

        LoadedCertificate fallido = CertificateStore.get(cfg("no-existe.p12"));
        Assertions.assertFalse(fallido.isLoaded());
        Assertions.assertNull(fallido.getCertificate());
        Assertions.assertNull(fallido.getNotBefore());
        Assertions.assertNull(fallido.getNotAfter());
        Assertions.assertEquals(List.of(), fallido.getDocumentNumbers());
        Assertions.assertEquals(List.of(), fallido.getRucs());
        Assertions.assertFalse(fallido.isDigitalSignature());
        Assertions.assertFalse(fallido.matchesIssuer("80123456", "5"));
        Assertions.assertFalse(fallido.toString().contains("rucs="), fallido::toString);
    }

    @ParameterizedTest(name = "numero({0}) = {1}")
    @CsvSource(value = {"RUC80123456-5|80123456-5", "ruc80123456|null", "RUC 80123456-5|80123456-5", "CI1234567|1234567-9",
        "ci1234567|null", "RUC80123456|80123456-5", "CI12A45|null", "PAS123|null", "'   '|null", "null|null"},
        delimiter = '|', nullValues = "null")
    public void elPrefijoYElDvDelDocumentoComoElFork(String documento, String numero) { // I3-59
        Assertions.assertEquals(numero, CertificateSubjects.normalizeDocumentNumber(documento));
    }

    @ParameterizedTest(name = "coincideConEmisor({0}, {1}, {2}) = {3}")
    @CsvSource(value = {"80123456-5|80123456|5|true", "80123456-5|80123456|4|false", "80123456-5|80123456|null|false",
        "80123456|80123456|7|false", "80123456|80123457|7|false", "null|80123456|5|false", "80123456-5|null|5|false",
        "' 80123456-5 '|' 80123456 '|' 5 '|false"}, delimiter = '|', nullValues = "null") // I3-59: igualdad exacta, como el fork
    public void elCotejoDeUnNumeroConElEmisor(String numero, String ruc, String dv, boolean coincide) {
        Assertions.assertEquals(coincide, CertificateSubjects.matchesIssuer(numero, ruc, dv));
    }

    @Test
    public void sinCertificadoNoHaySujetos() {
        Assertions.assertEquals(List.of(), CertificateSubjects.of(null));
        Assertions.assertNull(CertificateSubjects.attribute("esto no es un DN", "CN"));
    }
}
