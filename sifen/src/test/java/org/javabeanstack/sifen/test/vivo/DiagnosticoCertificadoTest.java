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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.validation.ValidationResult;
import org.javabeanstack.sifen.internal.cert.CertificateStore;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * E3-33: el diagnóstico de certificado de {@link SifenVivoFase3Test} no imprime la CI del titular. Sin red:
 * se corre con los certificados ficticios ({@code jbs-sifen-apoderado.p12}: CI 1234567 en el sujeto,
 * RUC80123456-5 en el SAN).
 */
public class DiagnosticoCertificadoTest {
    private static final String CI = "1234567";

    @AfterEach
    public void vaciarAlmacen() {
        CertificateStore.clear();
    }

    private static String correr(String certificado, String ruc, String dv) {
        SifenConfig cfg = FixturesSifen.configuracion();
        cfg.setClientCertificate(FixturesSifen.rutaPfx().resolveSibling(certificado).toString());
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(b, true, StandardCharsets.UTF_8)) {
            ValidationResult r = SifenVivoFase3Test.diagnostico(cfg, ruc, dv, out);
            Assertions.assertNotNull(r);
        }
        return b.toString(StandardCharsets.UTF_8);
    }

    @Test
    public void elDiagnosticoDelApoderadoNoMuestraLaCi() {
        String salida = correr("jbs-sifen-apoderado.p12", "80123456", "5");
        Assertions.assertFalse(salida.contains(CI), salida);
        Assertions.assertTrue(salida.contains("RUC80123456-5"), () -> "el RUC de la empresa sí se muestra: " + salida);
        Assertions.assertTrue(salida.contains("JBS-CERT-11 ⇒ no rechaza"), salida);
    }

    @Test
    public void niSiquieraCuandoElEmisorEsElTitular() {
        String salida = correr("jbs-sifen-apoderado.p12", CI, "9");
        Assertions.assertFalse(salida.contains(CI), salida);
        String sinRuc = correr("jbs-sifen-sin-ruc.p12", "80123456", "5");
        Assertions.assertFalse(sinRuc.contains(CI), sinRuc);
        Assertions.assertTrue(sinRuc.contains("JBS-CERT-11 ⇒ rechaza"), sinRuc);
    }

    @ParameterizedTest(name = "enmascarar({0}) = {1}")
    @CsvSource(value = {"RUC80123456-5|RUC80123456-5", "80123456-5|80123456-5", "80123456|80123456", "1234567-9|*******-*",
        "CI1234567|CI*******", "RUC1234567-9|RUC*******-*", "1234567|*******", "RUC50012345-1|RUC********-*"}, delimiter = '|')
    public void soloSeMuestraElRucDeUnaPersonaJuridica(String documento, String esperado) {
        Assertions.assertEquals(esperado, SifenVivoFase3Test.enmascarar(documento));
    }
}
