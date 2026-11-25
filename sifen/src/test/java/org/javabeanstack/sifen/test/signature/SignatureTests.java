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
package org.javabeanstack.sifen.test.signature;

import org.javabeanstack.sifen.Sifen;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.beans.ValidezFirmaDigital;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Firma y validación de firma <b>a través de la librería</b> (plan JBSSIF, §2.5, M1-04): la firma la
 * pone {@code SignatureHelper.signDocument} dentro de {@code generarXml} y la valida
 * {@code Sifen.validarFirmaDE*} ({@code SignatureHelper.validateSignature}), con el certificado
 * autofirmado del módulo (D11, emisor ficticio {@code 80123456-5}).
 *
 * <p>En la Fase 1 este test firmaba con {@code XMLSignatureFactory} directo y no ejercitaba la
 * librería (M1-04). {@code validarFirmaDEDesdeXml} escribe un temporal que nunca borra (E0-06): se
 * usa una vez; el resto valida un archivo de {@code @TempDir} con {@code validarFirmaDE(File)}.</p>
 */
public class SignatureTests {
    private static final String RUC_DV = CasosDePrueba.RUC_EMISOR + "-" + CasosDePrueba.DV_EMISOR;
    private static SifenConfig cfg;
    private static GenerationCtx ctx;

    @TempDir
    Path dir;

    @BeforeAll
    public static void configurar() {
        cfg = FixturesSifen.configuracion();
        ctx = FixturesSifen.contexto(cfg);
    }

    private static String facturaFirmada() throws Exception {
        CasosDePrueba.Caso c = CasosDePrueba.documentos().get(0);
        return c.leer(FixturesSifen.entrada(c)).generarXml(ctx, cfg);
    }

    private ValidezFirmaDigital validarArchivo(String xml) throws Exception {
        Path f = Files.createTempFile(dir, "de", ".xml");
        Files.write(f, xml.getBytes(StandardCharsets.UTF_8));
        return Sifen.validarFirmaDE(f.toFile());
    }

    private static boolean tieneSujeto(ValidezFirmaDigital v, String numero) {
        for (ValidezFirmaDigital.SujetoCertificado s : v.getSujetosCertificado()) {
            if (numero.equals(s.getNumeroDocumento())) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void firmaDeLaLibreriaValidaDesdeXml() throws Exception {
        ValidezFirmaDigital v = Sifen.validarFirmaDEDesdeXml(facturaFirmada());
        Assertions.assertTrue(v.isValido(), "motivo: " + v.getMotivoInvalidez());
        Assertions.assertNull(v.getMotivoInvalidez());
        Assertions.assertTrue(tieneSujeto(v, RUC_DV), "el certificado autofirmado declara RUC" + RUC_DV);
    }

    @Test
    public void firmaDeLaLibreriaValidaDesdeArchivo() throws Exception {
        ValidezFirmaDigital v = validarArchivo(facturaFirmada());
        Assertions.assertTrue(v.isValido(), "motivo: " + v.getMotivoInvalidez());
        boolean razon = false;
        for (ValidezFirmaDigital.SujetoCertificado s : v.getSujetosCertificado()) {
            razon |= "EMPRESA FICTICIA DE PRUEBA S.A.".equals(s.getRazonSocial());
        }
        Assertions.assertTrue(razon, "razón social del sujeto del certificado");
    }

    /** Un importe alterado después de firmar invalida la firma; el motivo viene en español. */
    @Test
    public void documentoAlteradoEsInvalido() throws Exception {
        String firmado = facturaFirmada();
        Assertions.assertTrue(firmado.contains("<dTotGralOpe>143000</dTotGralOpe>"));
        String alterado = firmado.replace("<dTotGralOpe>143000</dTotGralOpe>", "<dTotGralOpe>143001</dTotGralOpe>");
        ValidezFirmaDigital v = validarArchivo(alterado);
        Assertions.assertFalse(v.isValido());
        Assertions.assertEquals("La firma digital es inválida.", v.getMotivoInvalidez());
        Assertions.assertTrue(tieneSujeto(v, RUC_DV), "aun inválida, devuelve el sujeto leído del certificado");
    }

    @Test
    public void documentoSinFirma() throws Exception {
        ValidezFirmaDigital v = validarArchivo(FixturesSifen.recurso("casos/factura-pyg-b2c-cedula.xml"));
        Assertions.assertFalse(v.isValido());
        Assertions.assertEquals("No se encontró la firma digital en el Documento Electrónico.", v.getMotivoInvalidez());
    }

    /**
     * Firma válida pero de un certificado de otro RUC que el emisor del DE: inválida con el mensaje
     * previsto. La línea base daba {@code NullPointerException} (E2-05, corregido en §3.7: el sujeto del
     * {@code dirName} del SAN ya trae su número y {@code checkDocumentIssuer} es null-safe).
     */
    @Test
    public void emisorDistintoDelCertificadoEsInvalidoE205() throws Exception {
        DocumentoElectronico de = CasosDePrueba.documentos().get(4).construir();
        de.getgDatGralOpe().getgEmis().setdRucEm("80654321");
        de.getgDatGralOpe().getgEmis().setdDVEmi("3");
        String xml = de.generarXml(ctx, cfg);
        ValidezFirmaDigital v = validarArchivo(xml);
        Assertions.assertFalse(v.isValido());
        Assertions.assertTrue(v.getMotivoInvalidez().contains("no coincide"), v.getMotivoInvalidez());
    }

    /** El sujeto del SAN trae el número de documento (E2-05: el OID 2.5.4.5 del RFC 2253). */
    @Test
    public void sujetoDelSanConNumeroE205() throws Exception {
        ValidezFirmaDigital v = validarArchivo(facturaFirmada());
        Assertions.assertEquals(2, v.getSujetosCertificado().size());
        Assertions.assertEquals("80123456-5", v.getSujetosCertificado().get(1).getNumeroDocumento());
        Assertions.assertEquals("EMPRESA FICTICIA DE PRUEBA S.A.", v.getSujetosCertificado().get(1).getRazonSocial());
    }
}
