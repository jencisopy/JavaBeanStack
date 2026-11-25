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
package org.javabeanstack.sifen.core.exceptions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.javabeanstack.sifen.core.validation.RuleCatalog;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.ValidationRule;
import org.javabeanstack.sifen.core.validation.ValidationMessages;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * CA3-10 (plan JBSSIF, §3.8, D33 Parte C): los 81 códigos del modelo de errores tienen mensaje.
 *
 * <p>52 de {@link ErrorCode} (ENT 14, XML 7, FIRMA 5, CONEX 11, RESP 8, INT 5, EST 2; 38 de D33 más
 * JBS-ENT-06 y JBS-ENT-07 del ciclo 2, y JBS-ENT-08, JBS-ENT-09 y JBS-INT-04 de §10.2; JBS-ENT-10, JBS-ENT-11, JBS-XML-06 y JBS-XML-07 de §10.3; JBS-ENT-12/13/14, JBS-CONEX-11 y JBS-RESP-08 de §10.7) con mensaje y
 * acción en {@code error_messages.properties}, sin claves huérfanas, y 29 reglas de configuración
 * y certificado (JBS-CFG-01..14, JBS-CERT-01..15) con mensaje en el recurso de validación.</p>
 */
public class CodigoErrorTest {
    private static final Pattern FORMA = Pattern.compile("JBS-(ENT|XML|FIRMA|CONEX|RESP|INT|EST)-\\d{2}");

    @Test
    public void cadaCodigoTieneMensajeYAccionYNingunaClaveSobra() {
        Set<String> esperadas = new TreeSet<>();
        Set<String> vistos = new HashSet<>();
        for (ErrorCode c : ErrorCode.values()) {
            Assertions.assertTrue(FORMA.matcher(c.getCode()).matches(), c.getCode());
            Assertions.assertTrue(vistos.add(c.getCode()), "código repetido " + c.getCode());
            Assertions.assertEquals(c.name(), c.getCode().replace('-', '_'), "constante y código coinciden");
            Assertions.assertNotNull(c.getCategory());
            esperadas.add(c.getCode());
            esperadas.add(c.getCode() + ErrorMessages.SUFIJO_ACCION);
            Assertions.assertFalse(c.message().equals(c.getCode()), "sin mensaje: " + c.getCode());
            Assertions.assertFalse(c.getAction().isBlank(), "sin acción: " + c.getCode());
        }
        Assertions.assertEquals(esperadas, new TreeSet<>(ErrorMessages.claves()), "claves del recurso = códigos × {mensaje, acción}");
    }

    @Test
    public void sonCincuentaYDosConstantesPorFamilia() {
        Assertions.assertEquals(52, ErrorCode.values().length);
        int[] esperado = {14, 7, 5, 11, 8, 5, 2};
        String[] familias = {"ENT", "XML", "FIRMA", "CONEX", "RESP", "INT", "EST"};
        for (int i = 0; i < familias.length; i++) {
            String f = familias[i];
            long n = java.util.Arrays.stream(ErrorCode.values()).filter(c -> c.getCode().startsWith("JBS-" + f + "-")).count();
            Assertions.assertEquals(esperado[i], n, f);
        }
    }

    @Test
    public void conLasVeintinueveDeConfiguracionYCertificadoSonOchentaYUno() {
        List<ValidationRule> reglas = RuleCatalog.all().stream()
                .filter(r -> r.getGroup() == RuleGroup.CFG || r.getGroup() == RuleGroup.CERT)
                .collect(Collectors.toList());
        Set<String> codigos = reglas.stream().map(ValidationRule::getCode).collect(Collectors.toCollection(TreeSet::new));
        for (int i = 1; i <= 14; i++) {
            Assertions.assertTrue(codigos.contains(String.format("JBS-CFG-%02d", i)), "JBS-CFG-" + i);
        }
        for (int i = 1; i <= 15; i++) {
            Assertions.assertTrue(codigos.contains(String.format("JBS-CERT-%02d", i)), "JBS-CERT-" + i);
        }
        Assertions.assertEquals(29, codigos.size(), codigos.toString());
        for (String c : codigos) {
            Assertions.assertTrue(ValidationMessages.hasMessage(c), "sin mensaje: " + c);
            Assertions.assertNotEquals(ErrorCategory.DE_CONTENT, SifenException.categoryOf(c), c);
        }
        // 67 de D33 + JBS-ENT-06 (M3-32) y JBS-ENT-07 (I3-49), §3.8 ciclo 2, + JBS-ENT-08/09 y JBS-INT-04 (§10.2)
        Assertions.assertEquals(81, ErrorCode.values().length + codigos.size());
    }

    @Test
    public void lasCategoriasDeConexionUsanSuSubclase() {
        for (ErrorCode c : ErrorCode.values()) {
            SifenException e = SifenException.of(c, "detalle", null);
            Assertions.assertEquals(c.getCode(), e.getCode(), "getCode() es el código estable (D42, §11.4)");
            Assertions.assertEquals(c.getCategory(), e.getCategory());
            Assertions.assertEquals(c.getLegacyCode(), e.getLegacyCode());
            Assertions.assertEquals(c.getCategory() == ErrorCategory.CONNECTION, e instanceof SifenConnectionException, c.getCode());
            Assertions.assertNull(e.getCorrelationId(), "sin pasar por la fachada no hay id");
            Assertions.assertEquals(e.getUserMessage(), e.getMessage());
        }
    }

    @Test
    public void elConstructorHeredadoSigue() {
        SifenException e = new SifenException("i300", "Llamada SOAP inválida");
        Assertions.assertEquals("i300", e.getCode());
        Assertions.assertEquals("i300", e.getLegacyCode());
        Assertions.assertEquals(ErrorCategory.CONNECTION, e.getCategory());
        Assertions.assertTrue(e.getErrors().isEmpty());
        Assertions.assertTrue(e.toString().contains("[i300/CONNECTION]"), e.toString());
    }
}
