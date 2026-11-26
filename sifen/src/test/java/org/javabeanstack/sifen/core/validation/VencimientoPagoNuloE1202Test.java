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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.test.casos.CasosFase12;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.javabeanstack.sifen.test.casos.XmlCanonico;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * E12-02 (plan JBSSIF, Fase 12; decisión del usuario del 2026-11-25, ESC-40, excepción a D54): un {@code null}
 * dentro de {@code gGrupAdi.dVencPagList}.
 *
 * <p>En la línea base ninguna regla lo reportaba y {@code TgGrupAdi.setupSOAPElements} daba
 * {@code NullPointerException} también con la validación activa (CA3-04, CA12-03). Ahora <b>con la validación
 * previa</b> lo rechaza JBS-E-195 en la ruta del elemento ({@code dVencPag[i]}), y <b>sin la validación</b> el
 * armado omite el elemento nulo, como {@code TgOpeCom} con {@code gOblAfe} (D031): el XML sale con los
 * vencimientos informados.</p>
 *
 * <p>Base: DE-67 de {@code CasosFase12} (servicio cíclico con tres vencimientos), con el del medio en
 * {@code null}: con tres elementos JBS-E-143 (más de 3) no tapa el nulo.</p>
 */
public class VencimientoPagoNuloE1202Test {

    private static final String RUTA = "gDtipDE.gCamEsp.gGrupAdi.dVencPag";

    /** DE-67 con {@code [2026-12-10, null, 2026-12-20]}. */
    private static DocumentoElectronico conVencimientoNulo() {
        DocumentoElectronico de = CasosFase12.todos().get("DE-67").get();
        List<LocalDate> venc = de.getgDtipDE().getgCamEsp().getgGrupAdi().getdVencPagList();
        venc.remove(2);
        venc.add(1, null);
        return de;
    }

    /** DE-67 con los dos vencimientos no nulos de {@link #conVencimientoNulo()}. */
    private static DocumentoElectronico sinElNulo() {
        DocumentoElectronico de = CasosFase12.todos().get("DE-67").get();
        de.getgDtipDE().getgCamEsp().getgGrupAdi().getdVencPagList().remove(2);
        return de;
    }

    private static int cuantos(String xml, String etiqueta) {
        Matcher m = Pattern.compile("<(\\w+:)?" + etiqueta + "[ >]").matcher(xml);
        int n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }

    @Test
    public void conValidacionElNuloSeRechazaConJbsE195EnSuIndice() {
        SifenConfig cfg = FixturesSifen.configuracionConValidacion();
        ValidationResult base = DEValidator.standard().validate(sinElNulo(), FixturesSifen.contexto(cfg));
        Assertions.assertTrue(base.getErrors().isEmpty(), () -> "DE-67 con dos vencimientos: " + base.getErrors());

        DocumentoElectronico de = conVencimientoNulo();
        ValidationResult r = DEValidator.standard().validate(de, FixturesSifen.contexto(cfg));
        Assertions.assertEquals(1, r.getErrors().size(), r.toString());
        ValidationError e = r.find("JBS-E-195").get(0);
        Assertions.assertEquals(Severity.ERROR, e.getSeverity());
        Assertions.assertEquals(RUTA + "[1]", e.getPath());
        Assertions.assertEquals("Falta una fecha de vencimiento del pago: la lista tiene un elemento nulo.", e.getMessage());
        ValidationRule regla = RuleCatalog.all().stream().filter(x -> x.getCode().equals("JBS-E-195")).findFirst().orElseThrow();
        Assertions.assertEquals(RUTA + "[]", regla.getPath(), "ruta canónica de la regla");
        Assertions.assertEquals(Severity.ERROR, regla.getSeverity());
        Assertions.assertFalse(r.contains("JBS-E-143"), "tres elementos: no supera el máximo");

        SifenValidationException x = Assertions.assertThrows(SifenValidationException.class,
                () -> de.generarXml(FixturesSifen.contexto(cfg), cfg), "con la validación no llega a armar (ni NPE)");
        Assertions.assertEquals("JBS-E-195", x.getCode());
    }

    @Test
    public void sinValidacionElArmadoOmiteElNuloSinNpe() throws Exception {
        SifenConfig cfg = FixturesSifen.configuracion();
        Assertions.assertFalse(cfg.isPreValidationEnabled());
        DocumentoElectronico de = conVencimientoNulo();
        String xml = de.generarXml(FixturesSifen.contexto(cfg), cfg);
        Assertions.assertEquals(2, cuantos(xml, "dVencPag"), "solo los dos vencimientos informados");
        Assertions.assertEquals("2026-12-10", XmlCanonico.texto(xml, "dVencPag"));
        String esperado = sinElNulo().generarXml(FixturesSifen.contexto(cfg), cfg);
        Assertions.assertArrayEquals(XmlCanonico.canonico(esperado), XmlCanonico.canonico(xml),
                "el mismo DE que sin el elemento nulo");
        Assertions.assertEquals(3, de.getgDtipDE().getgCamEsp().getgGrupAdi().getdVencPagList().size(),
                "el armado no cambia la lista del llamador");
    }
}
