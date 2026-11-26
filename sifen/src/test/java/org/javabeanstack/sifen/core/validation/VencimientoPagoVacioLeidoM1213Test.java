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
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.javabeanstack.sifen.test.casos.XmlCanonico;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * M12-13 (plan JBSSIF, Fase 12; check_0007): el camino de <b>lectura</b> de un {@code <dVencPag/>} vacío. E12-02 se
 * probó armando el DE por objeto ({@link VencimientoPagoNuloE1202Test}); acá el nulo entra por el XML, que es como
 * llega un DE que la aplicación relee de lo que grabó.
 *
 * <p>Base: el XML de referencia candidato de DE-67 ({@code golden/candidatos/DE-67.xml}, tres vencimientos), con el
 * segundo vaciado. Al leerlo, {@code TgGrupAdi} agrega un {@code null} a {@code dVencPagList} (ResponseUtil: un nodo
 * sin texto da {@code null}); con la validación previa lo informa JBS-E-195 en su índice; sin ella, el armado lo
 * omite y sale el mismo DE que con dos vencimientos.</p>
 */
public class VencimientoPagoVacioLeidoM1213Test {

    private static final String RUTA = "gDtipDE.gCamEsp.gGrupAdi.dVencPag";
    private static final Pattern SEGUNDO = Pattern.compile("<((?:\\w+:)?)dVencPag>2026-12-20</(?:\\w+:)?dVencPag>");

    /** El candidato de DE-67 con el segundo vencimiento vacío, en la forma pedida. */
    private static String conVencimientoVacio(String forma) throws Exception {
        String xml = FixturesSifen.recurso("golden/candidatos/DE-67.xml");
        Matcher m = SEGUNDO.matcher(xml);
        Assertions.assertTrue(m.find(), "el candidato de DE-67 trae el vencimiento 2026-12-20");
        String p = m.group(1);
        String vacio = "autocerrado".equals(forma) ? "<" + p + "dVencPag/>" : "<" + p + "dVencPag></" + p + "dVencPag>";
        String r = xml.substring(0, m.start()) + vacio + xml.substring(m.end());
        Assertions.assertFalse(SEGUNDO.matcher(r).find(), "un solo reemplazo");
        return r;
    }

    private static int cuantos(String xml, String etiqueta) {
        Matcher m = Pattern.compile("<(\\w+:)?" + etiqueta + "[ >]").matcher(xml);
        int n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"autocerrado", "sin texto"})
    public void elVencimientoVacioEntraComoNulo(String forma) throws Exception {
        DocumentoElectronico de = new DocumentoElectronico(conVencimientoVacio(forma));
        List<LocalDate> venc = de.getgDtipDE().getgCamEsp().getgGrupAdi().getdVencPagList();
        Assertions.assertEquals(Arrays.asList(LocalDate.of(2026, 12, 10), null, LocalDate.of(2026, 12, 30)), venc,
                "el vacío se lee como null en su posición, sin excepción");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"autocerrado", "sin texto"})
    public void conValidacionLoReportaJbsE195EnSuIndice(String forma) throws Exception {
        SifenConfig cfg = FixturesSifen.configuracionConValidacion();
        DocumentoElectronico de = new DocumentoElectronico(conVencimientoVacio(forma));
        ValidationResult r = DEValidator.standard().validate(de, FixturesSifen.contexto(cfg));
        Assertions.assertEquals(List.of("JBS-E-195"), r.getErrors().stream().map(ValidationError::getCode).toList(), r.toString());
        ValidationError e = r.find("JBS-E-195").get(0);
        Assertions.assertEquals(RUTA + "[1]", e.getPath());
        Assertions.assertEquals(Severity.ERROR, e.getSeverity());
        SifenValidationException x = Assertions.assertThrows(SifenValidationException.class,
                () -> de.generarXml(FixturesSifen.contexto(cfg), cfg), "con la validación no llega a armar");
        Assertions.assertEquals("JBS-E-195", x.getCode());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"autocerrado", "sin texto"})
    public void sinValidacionElArmadoLoOmite(String forma) throws Exception {
        SifenConfig cfg = FixturesSifen.configuracion();
        Assertions.assertFalse(cfg.isPreValidationEnabled());
        DocumentoElectronico de = new DocumentoElectronico(conVencimientoVacio(forma));
        String xml = de.generarXml(FixturesSifen.contexto(cfg), cfg);
        Assertions.assertEquals(2, cuantos(xml, "dVencPag"), "solo los dos vencimientos informados");
        DocumentoElectronico dos = new DocumentoElectronico(FixturesSifen.recurso("golden/candidatos/DE-67.xml"));
        dos.getgDtipDE().getgCamEsp().getgGrupAdi().getdVencPagList().remove(1);
        Assertions.assertArrayEquals(XmlCanonico.canonico(dos.generarXml(FixturesSifen.contexto(cfg), cfg)), XmlCanonico.canonico(xml),
                "el mismo DE que con los dos vencimientos");
        Assertions.assertEquals(3, de.getgDtipDE().getgCamEsp().getgGrupAdi().getdVencPagList().size(), "el armado no cambia la lista leída");
    }
}
