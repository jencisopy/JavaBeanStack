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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.beans.EventosDE;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Casos borde del núcleo de D33 (plan JBSSIF, §3.6): la ficha y su builder, el sujeto, la lista de
 * sujetos, la verificación del catálogo y la igualdad de los errores.
 */
public class NucleoReglasTest {

    /** Conjunto ficticio para usar los constructores protegidos de fichas y sujetos. */
    static final class Ficticio extends RuleSet<DocumentoElectronico, String> {
        Ficticio(String base) {
            super("TST-nucleo-" + base, RuleGroup.E, base);
        }

        ValidationRule.Builder ficha(String codigo) {
            return rule(codigo);
        }

        List<Subject<DocumentoElectronico, String>> lista(List<String> l, int... externos) {
            return externos.length == 0 ? perItem(l) : perItem(l, externos);
        }

        @Override
        protected List<Subject<DocumentoElectronico, String>> subjects(DocumentoElectronico raiz) {
            return none();
        }
    }

    @Test
    public void laFichaArmaSuRutaYSusDatos() {
        Ficticio c = new Ficticio("gDtipDE.gCamItem[]");
        ValidationRule r = c.ficha("TST301").field("dCodInt").warning().dCodRes("1234").lengthOnly()
                .source(RuleSource.NT_010, RuleSource.NT_013).from(LocalDate.of(2025, 1, 1))
                .until(RuleSource.NT_013, "ficticio").replaces("D105").message("Ítem {0} con {1}").construir();
        Assertions.assertEquals("gDtipDE.gCamItem[].dCodInt", r.getPath());
        Assertions.assertEquals(Severity.WARNING, r.getSeverity());
        Assertions.assertEquals(ValuePolicy.LENGTH_ONLY, r.getValuePolicy());
        Assertions.assertEquals(RuleSource.NT_013, r.getSource());
        Assertions.assertEquals(List.of(RuleSource.NT_010, RuleSource.NT_013), r.getSources());
        Assertions.assertEquals(LocalDate.of(2023, 5, 21), r.getEffectivePeriod().until());
        Assertions.assertEquals("D105", r.getReplaces());
        Assertions.assertEquals("Ítem 3 con x", r.message(3, "x"));
        Assertions.assertEquals("Ítem {0} con {1}", r.message((Object[]) null));
        Assertions.assertEquals("gDtipDE.gCamItem[2].dCodInt", r.path(2, 9));
        Assertions.assertEquals("gDtipDE.gCamItem[].dCodInt", r.path((int[]) null));
        Assertions.assertEquals("gDtipDE.gCamItem[]", c.ficha("TST302").field("").error().construir().getPath());
        Assertions.assertEquals("otra.ruta", c.ficha("TST303").path("otra.ruta").error().showValue().construir().getPath());
        Assertions.assertEquals("dSisFact", new Ficticio("").ficha("TST304").field("dSisFact").error().omitValue().construir().getPath());
        Assertions.assertEquals("gDtipDE.gCamItem[]", c.ficha("TST305").error().construir().getPath(), "sin campo = la base");
        Assertions.assertThrows(IllegalStateException.class, () -> c.ficha("TST306").field("x").construir(), "sin severidad");
        Assertions.assertThrows(NullPointerException.class, () -> c.ficha("TST307").until(RuleSource.NT_001, "sin fecha"));
        Assertions.assertFalse(r.isInForceOn(LocalDate.of(2024, 1, 1)), "hasta 2023-05-21 y desde 2025: no rige nunca");
        Assertions.assertEquals("TST301", r.toString());
    }

    @Test
    public void sujetosYListas() {
        Ficticio c = new Ficticio("x[]");
        Assertions.assertTrue(c.lista(null).isEmpty());
        List<String> l = new ArrayList<>(Arrays.asList("a", null));
        List<Subject<DocumentoElectronico, String>> s = c.lista(l, 4);
        Assertions.assertEquals(2, s.size());
        Assertions.assertEquals(4, s.get(1).index(0));
        Assertions.assertEquals(1, s.get(1).index(1));
        Assertions.assertEquals(-1, s.get(1).index(2));
        Assertions.assertNull(s.get(1).e());
        Assertions.assertEquals(0, Subject.of("z", (int[]) null).indexes().length);
        Assertions.assertEquals(List.of(), c.getDeclaredRules());
        Assertions.assertEquals("x[]", c.getBasePath());
    }

    @Test
    public void elCatalogoRechazaFichasInconsistentes() {
        Ficticio c = new Ficticio("gTimb");
        Map<String, ValidationRule> ya = new HashMap<>();
        Assertions.assertTrue(RuleCatalog.problemaDeDefinicion(c.ficha("d1").field("x").error().construir(), c, ya).contains("mal formado"));
        Assertions.assertTrue(RuleCatalog.problemaDeDefinicion(c.ficha("JBS-D-01").field("x").error().construir(), c, ya).contains("grupo"));
        Assertions.assertTrue(RuleCatalog.problemaDeDefinicion(c.ficha("C099").path("otra.cosa").error().construir(), c, ya).contains("no cuelga"));
        ValidationRule ok = c.ficha("C098").field("dEst").error().construir();
        Assertions.assertNull(RuleCatalog.problemaDeDefinicion(ok, c, ya));
        ya.put("C098", ok);
        Assertions.assertTrue(RuleCatalog.problemaDeDefinicion(ok, c, ya).contains("repetido"));
        Assertions.assertNull(RuleCatalog.ruleSetOf("NO-EXISTE"));
        Assertions.assertEquals("motor", RuleCatalog.ruleSetOf("JBS-VAL-00"));
    }

    @Test
    public void igualdadDeLosErroresYResultados() {
        ValidationRule a = new ValidationRule("TST401", RuleGroup.D, "x.y", Severity.ERROR, "1", ValuePolicy.SHOW);
        ValidationRule b = new ValidationRule("TST402", RuleGroup.D, "x.y", Severity.WARNING, null, ValuePolicy.SHOW);
        ValidationError e1 = ValidationError.de(a, null, "v");
        Assertions.assertEquals(e1, ValidationError.de(a, null, "v"));
        Assertions.assertEquals(e1.hashCode(), ValidationError.de(a, null, "v").hashCode());
        Assertions.assertNotEquals(e1, ValidationError.de(a, null, "w"));
        Assertions.assertNotEquals(e1, ValidationError.de(b, null, "v"));
        Assertions.assertNotEquals(e1, "texto");
        Assertions.assertEquals(e1, e1);
        Assertions.assertNotEquals(e1, null);
        ValidationResult r = ValidationResult.combine(Arrays.asList(new ValidationResult(List.of(e1)), null));
        Assertions.assertEquals(1, r.getAll().size());
        Assertions.assertSame(r, r.withPrefix(" "));
        Assertions.assertTrue(new ValidationResult(Arrays.asList(e1, null)).getAll().size() == 1);
        Assertions.assertTrue(new ValidationResult(null).isEmpty());
        Assertions.assertTrue(new SifenValidationException(null).getResult().isEmpty());
        Assertions.assertTrue(ValidationContext.present(Map.of("k", 1)));
        Assertions.assertFalse(ValidationContext.present(Map.of()));
    }

    @Test
    public void validadorDeEventosConEnvioVacioYRegistroDeVarios() {
        Assertions.assertTrue(EventValidator.standard().validate(new EventosDE(), null).contains("JBS-VAL-01"));
        Assertions.assertTrue(EventValidator.standard().validate((EventosDE) null, null).contains("JBS-VAL-01"));
        DEValidator v = DEValidator.builder().registerAll(List.of(new Ficticio("a"), new Ficticio("b"))).build();
        Assertions.assertEquals(2, v.getGroups().size());
        Assertions.assertEquals(2, v.getRuleSets().size());
    }
}
