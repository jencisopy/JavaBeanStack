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
package org.javabeanstack.poi.word;

import java.util.HashMap;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del reemplazo de marcadores en plantillas Word. Los casos reproducen
 * cómo Word parte los marcadores en varios runs en las plantillas del ERP: el
 * texto de cada caso está tomado de las plantillas reales de la línea GILOTE.
 *
 * @author jenciso
 */
public class WordTemplateMergeTest {

    /**
     * Arma un párrafo con un run por cada texto recibido, ejecuta el merge y
     * devuelve el texto resultante del párrafo.
     *
     * @param data valores a mergear.
     * @param textos texto de cada run de la plantilla.
     * @return texto del párrafo ya mergeado.
     */
    private String merge(Map<String, String> data, String... textos) {
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph parrafo = doc.createParagraph();
            for (String texto : textos) {
                XWPFRun run = parrafo.createRun();
                run.setText(texto, 0);
            }
            WordTemplateMerge.merge(doc, data);
            StringBuilder resultado = new StringBuilder();
            for (XWPFRun run : doc.getParagraphs().get(0).getRuns()) {
                resultado.append(run.getText(0) == null ? "" : run.getText(0));
            }
            return resultado.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<String, String> datos() {
        Map<String, String> data = new HashMap();
        data.put("cicliente", "1.234.567");
        data.put("diahoy", "12");
        data.put("meshoy", "agosto");
        data.put("añohoy", "2026");
        data.put("manzana", "15");
        data.put("importesena", "500.000");
        data.put("cliente", "JUAN PEREZ");
        data.put("fraccion", "SANTA MARIA");
        return data;
    }

    @Test
    @DisplayName("Reemplaza un marcador contenido en un solo run")
    public void marcadorEnUnSoloRun() {
        assertEquals("C.I. Nro. 1.234.567",
                merge(datos(), "C.I. Nro. <<cicliente>>"));
    }

    @Test
    @DisplayName("Reemplaza un marcador partido en tres runs")
    public void marcadorPartidoEnTresRuns() {
        assertEquals("1.234.567",
                merge(datos(), "<<", "cicliente", ">>"));
    }

    @Test
    @DisplayName("Reemplaza un marcador cuyo nombre está partido en dos runs")
    public void nombreDeCampoPartido() {
        //Caso real de GILOTE_CONSTANCIASYS: ['<<', 'año', 'hoy', '>>']
        assertEquals("2026",
                merge(datos(), "<<", "año", "hoy", ">>"));
    }

    @Test
    @DisplayName("Reemplaza cuando los delimitadores están partidos por la mitad")
    public void delimitadoresPartidos() {
        //Casos reales de GILOTE_CONTRATOSYS: ['<<manzana>', '>'] y ['<', '<', 'importesena', '>>']
        assertEquals("15", merge(datos(), "<<manzana>", ">"));
        assertEquals("500.000", merge(datos(), "<", "<", "importesena", ">>"));
    }

    @Test
    @DisplayName("Reemplaza marcadores encadenados que comparten un run")
    public void marcadoresQueCompartenRun() {
        //Caso real de RESCICIONSYS, donde un mismo run cierra un marcador y abre el siguiente.
        assertEquals("Asunción 12 de agosto del 2026",
                merge(datos(), "Asunción <<", "diahoy", ">> de <<", "meshoy", ">> del <<", "añohoy", ">>"));
    }

    @Test
    @DisplayName("Conserva el texto de los runs que no participan del marcador")
    public void conservaElRestoDelParrafo() {
        assertEquals("El comprador JUAN PEREZ acepta.",
                merge(datos(), "El comprador ", "<<", "cliente", ">>", " acepta."));
    }

    @Test
    @DisplayName("Reparte el resultado dejando el valor en el run donde empieza el marcador")
    public void valorEnElRunDeApertura() {
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph parrafo = doc.createParagraph();
            for (String texto : new String[]{"Asunción <<", "diahoy", ">> de <<", "meshoy", ">>"}) {
                XWPFRun run = parrafo.createRun();
                run.setText(texto, 0);
            }
            WordTemplateMerge.merge(doc, datos());
            java.util.List<XWPFRun> runs = doc.getParagraphs().get(0).getRuns();
            assertEquals("Asunción 12", runs.get(0).getText(0));
            assertEquals("", runs.get(1).getText(0));
            assertEquals(" de agosto", runs.get(2).getText(0));
            assertEquals("", runs.get(3).getText(0));
            assertEquals("", runs.get(4).getText(0));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("Imprime la marca de campo vacío cuando el dato no está")
    public void campoSinValor() {
        assertEquals("Finca Nº " + WordTemplateMerge.SIN_VALOR,
                merge(datos(), "Finca Nº <<", "finca", ">>"));
        Map<String, String> data = datos();
        data.put("cliente", "");
        assertEquals(WordTemplateMerge.SIN_VALOR, merge(data, "<<cliente>>"));
    }

    @Test
    @DisplayName("No altera un párrafo sin marcadores ni uno con delimitadores sueltos")
    public void parrafoSinMarcadores() {
        assertEquals("Texto sin marcadores", merge(datos(), "Texto sin marcadores"));
        //'>>' antes de '<<': no hay marcador y el texto debe quedar intacto.
        assertEquals(">> texto <<", merge(datos(), ">> texto <<"));
    }

    @Test
    @DisplayName("Reemplaza dentro de las tablas y de las tablas anidadas")
    public void marcadoresEnTablas() {
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFTable tabla = doc.createTable(1, 1);
            XWPFTableCell celda = tabla.getRow(0).getCell(0);
            XWPFParagraph parrafo = celda.getParagraphs().get(0);
            parrafo.createRun().setText("<<", 0);
            parrafo.createRun().setText("cliente", 0);
            parrafo.createRun().setText(">>", 0);

            XWPFTable anidada = celda.insertNewTbl(parrafo.getCTP().newCursor());
            XWPFTableCell celdaAnidada = anidada.createRow().createCell();
            XWPFParagraph interno = celdaAnidada.addParagraph();
            interno.createRun().setText("<<", 0);
            interno.createRun().setText("fraccion", 0);
            interno.createRun().setText(">>", 0);

            WordTemplateMerge.merge(doc, datos());

            assertEquals("JUAN PEREZ", celda.getParagraphs().get(0).getRuns().get(0).getText(0));
            assertEquals("SANTA MARIA", interno.getRuns().get(0).getText(0));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
