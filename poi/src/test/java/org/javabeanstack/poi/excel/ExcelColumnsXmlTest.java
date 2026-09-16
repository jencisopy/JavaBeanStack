/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
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
package org.javabeanstack.poi.excel;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.xml.DomW3cParser;
import org.javabeanstack.xml.XmlDomW3c;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Pruebas unitarias de {@link ExcelColumnsXml} y de
 * {@link ExcelColumns#edit(String)} (plan {@code XLSXML}, Fase 1).
 * <p>
 * Los fragmentos XML son constantes del test; el único caso que necesita
 * archivos ({@code fromFile} con herencia {@code clase/src}) los escribe en
 * un directorio temporal, nunca en el árbol de fuentes.
 *
 * @author Jorge Enciso
 */
public class ExcelColumnsXmlTest {

    /**
     * Bloque equivalente, columna por columna, al {@code buildColumns()} de
     * {@code ExcelCtactesubProcessor} (ejemplo del plan).
     */
    static final String CTACTESUB_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<XML>\n"
            + "    <PAGE>\n"
            + "        <DATATABLES>\n"
            + "            <CTACTESUB title=\"Sub Cuentas\"><COLUMNS><CODIGO header=\"Código\"/></COLUMNS></CTACTESUB>\n"
            + "        </DATATABLES>\n"
            + "        <EXCELIMPORT>\n"
            + "            <DEFAULT>\n"
            + "                <COLUMNS>\n"
            + "                    <!-- header = nombre del tag en minúsculas; field = header -->\n"
            + "                    <CTACTE required=\"VALUE\"/>\n"
            + "                    <CODIGO required=\"VALUE\"/>\n"
            + "                    <NOMBRE required=\"COLUMN\"/>\n"
            + "                    <RUC/>\n"
            + "                    <DIRECCION/>\n"
            + "                    <TELEFONO/>\n"
            + "                    <CONTACTO/>\n"
            + "                    <CLASIFICACION/>\n"
            + "                    <INACTIVO default=\"false\"/>\n"
            + "                    <BLOQUEARATRASO header=\"bloquearAtraso\" field=\"bloquearAtraso\" default=\"false\"/>\n"
            + "                    <BLOQUEARATRASODIAS header=\"bloquearAtrasoDias\" field=\"bloquearAtrasoDias\" default=\"0\"/>\n"
            + "                    <ESPROVEEDOR default=\"0\" overwrite=\"false\"/>\n"
            + "                    <ESCLIENTE default=\"1\" overwrite=\"false\"/>\n"
            + "                    <CTBCLIENTE overwrite=\"false\"/>\n"
            + "                    <CTBPROVEEDOR overwrite=\"false\"/>\n"
            + "                    <VENDEDOR/>\n"
            + "                    <CTACTERUTA/>\n"
            + "                    <ZONA/>\n"
            + "                    <SUBZONA/>\n"
            + "                    <CIUDAD/>\n"
            + "                    <PROVINCIA/>\n"
            + "                    <PAIS/>\n"
            + "                </COLUMNS>\n"
            + "            </DEFAULT>\n"
            + "        </EXCELIMPORT>\n"
            + "    </PAGE>\n"
            + "</XML>";

    static final String NODE_PATH = "PAGE/EXCELIMPORT/DEFAULT";

    /**
     * El mismo mapeo con la API fluida. Los valores por defecto van como
     * cadena porque el XML solo puede producir cadenas (RF5/D7); la
     * conversión al tipo del atributo ocurre al aplicarse.
     */
    static ExcelColumns buildCtactesubColumns() {
        ExcelColumns c = new ExcelColumns();
        c.add("ctacte", "ctacte").require(ColumnRequirement.VALUE);
        c.add("codigo", "codigo").require(ColumnRequirement.VALUE);
        c.add("nombre", "nombre").required();
        c.add("ruc", "ruc");
        c.add("direccion", "direccion");
        c.add("telefono", "telefono");
        c.add("contacto", "contacto");
        c.add("clasificacion", "clasificacion");
        c.add("inactivo", "inactivo").defaultValue("false");
        c.add("bloquearAtraso", "bloquearAtraso").defaultValue("false");
        c.add("bloquearAtrasoDias", "bloquearAtrasoDias").defaultValue("0");
        c.add("esproveedor", "esproveedor").defaultValue("0").noOverwrite();
        c.add("escliente", "escliente").defaultValue("1").noOverwrite();
        c.add("ctbcliente", "ctbcliente").noOverwrite();
        c.add("ctbproveedor", "ctbproveedor").noOverwrite();
        c.add("vendedor", "vendedor");
        c.add("ctacteruta", "ctacteruta");
        c.add("zona", "zona");
        c.add("subzona", "subzona");
        c.add("ciudad", "ciudad");
        c.add("provincia", "provincia");
        c.add("pais", "pais");
        return c;
    }

    /**
     * Compara dos colecciones campo por campo (CA1-02): nunca con
     * {@code equals}, que en {@link ExcelColumnSpec} solo mira la cabecera.
     */
    static void assertSameColumns(ExcelColumns expected, ExcelColumns actual) {
        assertEquals(expected.toHeadToField(), actual.toHeadToField());
        assertEquals(new ArrayList<>(expected.toHeadToField().keySet()),
                new ArrayList<>(actual.toHeadToField().keySet()), "orden de declaración");
        assertEquals(expected.size(), actual.size());
        Iterator<ExcelColumnSpec> e = expected.iterator();
        Iterator<ExcelColumnSpec> a = actual.iterator();
        while (e.hasNext()) {
            ExcelColumnSpec es = e.next();
            ExcelColumnSpec as = a.next();
            String col = "columna «" + es.getHeader() + "»";
            assertEquals(es.getHeader(), as.getHeader(), col);
            assertEquals(es.getField(), as.getField(), col);
            assertEquals(es.getRequirement(), as.getRequirement(), col);
            assertEquals(es.hasDefaultValue(), as.hasDefaultValue(), col);
            if (es.hasDefaultValue()) {
                assertEquals(es.getDefaultValue().get(), as.getDefaultValue().get(), col);
            }
            assertEquals(es.isDefaultValueFixed(), as.isDefaultValueFixed(), col);
            assertEquals(es.isDefaultWhenBlank(), as.isDefaultWhenBlank(), col);
            assertEquals(es.isOverwrite(), as.isOverwrite(), col);
            assertNull(as.getConverter(), col);
        }
        assertFalse(a.hasNext());
    }

    static IllegalArgumentException assertRejects(String expectedFragment, Runnable action) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, action::run);
        assertTrue(ex.getMessage().contains(expectedFragment),
                "se esperaba «" + expectedFragment + "» en: " + ex.getMessage());
        return ex;
    }

    static Element rootOf(String xml) throws Exception {
        return DomW3cParser.loadXml(xml).getDocumentElement();
    }

    // ----------------------------------------------------------------------
    // Equivalencia con buildColumns() y las cinco fuentes
    // ----------------------------------------------------------------------

    @Test
    public void testFromXmlConNodePathEquivaleABuildColumns() {
        ExcelColumns columns = ExcelColumnsXml.fromXml(CTACTESUB_XML, NODE_PATH);
        assertSameColumns(buildCtactesubColumns(), columns);
        assertEquals(22, columns.size());
    }

    @Test
    public void testFromXmlDomEquivaleABuildColumns() {
        XmlDomW3c dom = new XmlDomW3c();
        assertTrue(dom.config("", CTACTESUB_XML, "", true));
        ExcelColumns columns = ExcelColumnsXml.fromXmlDom(dom, NODE_PATH);
        assertSameColumns(buildCtactesubColumns(), columns);
        // ruta absoluta también
        assertSameColumns(buildCtactesubColumns(),
                ExcelColumnsXml.fromXmlDom(dom, "/XML/PAGE/EXCELIMPORT/DEFAULT"));
        // el <COLUMNS> mismo como destino de la ruta
        assertSameColumns(buildCtactesubColumns(),
                ExcelColumnsXml.fromXmlDom(dom, NODE_PATH + "/COLUMNS"));
    }

    @Test
    public void testFromNodeAceptaElColumnsOSuPadreDirecto() throws Exception {
        Document doc = DomW3cParser.loadXml(CTACTESUB_XML);
        Element parent = DomW3cParser.getElement(doc, NODE_PATH);
        Element columnsNode = DomW3cParser.getElement(doc, NODE_PATH + "/COLUMNS");
        assertSameColumns(buildCtactesubColumns(), ExcelColumnsXml.fromNode(parent));
        assertSameColumns(buildCtactesubColumns(), ExcelColumnsXml.fromNode(columnsNode));
        assertSameColumns(buildCtactesubColumns(), ExcelColumnsXml.fromNode(parent, "ctactesub"));
    }

    @Test
    public void testFromXmlSinNodePathTomaElRaiz() {
        ExcelColumns fromColumns = ExcelColumnsXml.fromXml(
                "<COLUMNS><CODIGO required=\"VALUE\"/><NOMBRE/></COLUMNS>");
        assertEquals(Arrays.asList("codigo", "nombre"), new ArrayList<>(fromColumns.toHeadToField().keySet()));
        assertEquals(ColumnRequirement.VALUE, fromColumns.byHeader("codigo").orElseThrow().getRequirement());

        ExcelColumns fromParent = ExcelColumnsXml.fromXml(
                "<DEFAULT><COLUMNS><CODIGO/></COLUMNS></DEFAULT>");
        assertEquals(1, fromParent.size());
        assertEquals("codigo", fromParent.byHeader("codigo").orElseThrow().getField());
    }

    @Test
    public void testFromFileEquivaleABuildColumns(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("ctactesub.xml");
        Files.writeString(file, CTACTESUB_XML, StandardCharsets.UTF_8);
        ExcelColumns columns = ExcelColumnsXml.fromFile(file.toString(), NODE_PATH);
        assertSameColumns(buildCtactesubColumns(), columns);
    }

    // ----------------------------------------------------------------------
    // Reglas de cada atributo (RF2, RF4)
    // ----------------------------------------------------------------------

    @Test
    public void testIdentidadHeaderYFieldExplicitos() {
        ExcelColumns c = ExcelColumnsXml.fromXml("<COLUMNS>"
                + "<CODIGO/>"
                + "<C2 header=\"RUC / Nº de Identificacion del Informado\" field=\"ruc\"/>"
                + "<C3 header=\"  con espacios  \"/>"
                + "<C4 field=\"otroAtributo\"/>"
                + "<C5 field=\"\"/>"
                + "</COLUMNS>");
        ExcelColumnSpec codigo = c.byHeader("codigo").orElseThrow();
        assertEquals("codigo", codigo.getHeader(), "tag en minúsculas");
        assertEquals("codigo", codigo.getField(), "field = header");
        assertEquals(ColumnRequirement.OPTIONAL, codigo.getRequirement());
        assertFalse(codigo.hasDefaultValue());
        assertFalse(codigo.isDefaultWhenBlank());
        assertTrue(codigo.isOverwrite());

        ExcelColumnSpec ruc = c.byHeader("RUC / Nº de Identificacion del Informado").orElseThrow();
        assertEquals("ruc", ruc.getField());
        ExcelColumnSpec espacios = c.byHeader("con espacios").orElseThrow();
        assertEquals("con espacios", espacios.getHeader(), "header con trim");
        assertEquals("con espacios", espacios.getField(), "field = header ya normalizado");
        assertEquals("otroAtributo", c.byHeader("c4").orElseThrow().getField());
        assertEquals("", c.byHeader("c5").orElseThrow().getField(), "field vacío: columna procesada a mano");
    }

    @Test
    public void testRequiredValoresYSinonimos() {
        ExcelColumns c = ExcelColumnsXml.fromXml("<COLUMNS>"
                + "<A required=\"OPTIONAL\"/><B required=\"COLUMN\"/><C required=\"VALUE\"/>"
                + "<D required=\"value\"/><E required=\"Column\"/>"
                + "<F required=\"true\"/><G required=\"false\"/><H required=\"TRUE\"/>"
                + "</COLUMNS>");
        assertEquals(ColumnRequirement.OPTIONAL, c.byHeader("a").orElseThrow().getRequirement());
        assertEquals(ColumnRequirement.COLUMN, c.byHeader("b").orElseThrow().getRequirement());
        assertEquals(ColumnRequirement.VALUE, c.byHeader("c").orElseThrow().getRequirement());
        assertEquals(ColumnRequirement.VALUE, c.byHeader("d").orElseThrow().getRequirement());
        assertEquals(ColumnRequirement.COLUMN, c.byHeader("e").orElseThrow().getRequirement());
        assertEquals(ColumnRequirement.COLUMN, c.byHeader("f").orElseThrow().getRequirement());
        assertEquals(ColumnRequirement.OPTIONAL, c.byHeader("g").orElseThrow().getRequirement());
        assertEquals(ColumnRequirement.COLUMN, c.byHeader("h").orElseThrow().getRequirement());
    }

    @Test
    public void testDefaultAusenteDistintoDeDefaultVacio() {
        ExcelColumns c = ExcelColumnsXml.fromXml("<COLUMNS>"
                + "<SIN/><VACIO default=\"\"/><CON default=\"01\"/>"
                + "</COLUMNS>");
        assertFalse(c.byHeader("sin").orElseThrow().hasDefaultValue());
        ExcelColumnSpec vacio = c.byHeader("vacio").orElseThrow();
        assertTrue(vacio.hasDefaultValue(), "default=\"\" es un default de cadena vacía");
        assertEquals("", vacio.getDefaultValue().get());
        assertTrue(vacio.isDefaultValueFixed());
        ExcelColumnSpec con = c.byHeader("con").orElseThrow();
        assertEquals("01", con.getDefaultValue().get(), "el default se guarda como cadena");
        assertTrue(con.isDefaultValueFixed());
    }

    @Test
    public void testDefaultWhenBlankYOverwrite() {
        ExcelColumns c = ExcelColumnsXml.fromXml("<COLUMNS>"
                + "<A default=\"GS.\" defaultwhenblank=\"true\"/>"
                + "<B default=\"GS.\" defaultwhenblank=\"false\"/>"
                + "<C default=\"GS.\"/>"
                + "<D overwrite=\"false\"/><E overwrite=\"true\"/><F overwrite=\"FALSE\"/><G/>"
                + "</COLUMNS>");
        assertTrue(c.byHeader("a").orElseThrow().isDefaultWhenBlank());
        assertFalse(c.byHeader("b").orElseThrow().isDefaultWhenBlank());
        assertFalse(c.byHeader("c").orElseThrow().isDefaultWhenBlank());
        assertFalse(c.byHeader("d").orElseThrow().isOverwrite());
        assertTrue(c.byHeader("e").orElseThrow().isOverwrite());
        assertFalse(c.byHeader("f").orElseThrow().isOverwrite());
        assertTrue(c.byHeader("g").orElseThrow().isOverwrite());
    }

    @Test
    public void testComentariosIgnoradosYAtributosDeHerenciaTolerados() {
        ExcelColumns c = ExcelColumnsXml.fromXml("<COLUMNS clase=\"BASE\" src=\"file://clases.xml\">"
                + "<!-- <SUCURSAL default=\"01\"/> descomentar si corresponde -->"
                + "<CODIGO clase=\"X\" src=\"file://otro.xml\" __header=\"file://otro.xml\" required=\"VALUE\"/>"
                + "<!-- otro comentario -->"
                + "<NOMBRE/>"
                + "</COLUMNS>");
        assertEquals(Arrays.asList("codigo", "nombre"), new ArrayList<>(c.toHeadToField().keySet()));
        assertEquals(ColumnRequirement.VALUE, c.byHeader("codigo").orElseThrow().getRequirement());
    }

    @Test
    public void testOrdenDelXmlSeConserva() {
        ExcelColumns c = ExcelColumnsXml.fromXml("<COLUMNS><ZETA/><ALFA required=\"COLUMN\"/><MEDIA default=\"x\"/></COLUMNS>");
        assertEquals(Arrays.asList("zeta", "alfa", "media"), new ArrayList<>(c.toHeadToField().keySet()));
    }

    // ----------------------------------------------------------------------
    // Parser estricto (RF3, CA1-03)
    // ----------------------------------------------------------------------

    @Test
    public void testAtributoDesconocidoLanzaConBloqueYColumna() {
        IllegalArgumentException ex = assertRejects("atributo desconocido «requiered»",
                () -> ExcelColumnsXml.fromXml(CTACTESUB_XML.replace("<CODIGO required=\"VALUE\"/>",
                        "<CODIGO requiered=\"VALUE\"/>"), NODE_PATH));
        assertTrue(ex.getMessage().startsWith("Bloque «PAGE/EXCELIMPORT/DEFAULT», columna «CODIGO»: "),
                ex.getMessage());
        // camelCase no es sinónimo: los atributos van en minúsculas
        assertRejects("atributo desconocido «defaultWhenBlank»",
                () -> ExcelColumnsXml.fromXml("<COLUMNS><A default=\"1\" defaultWhenBlank=\"true\"/></COLUMNS>"));
        assertRejects("atributo desconocido «column»",
                () -> ExcelColumnsXml.fromXml("<COLUMNS><A column=\"a\"/></COLUMNS>"));
    }

    @Test
    public void testValoresInvalidosLanzan() {
        assertRejects("columna «A»: valor inválido «obligatorio» para «required»",
                () -> ExcelColumnsXml.fromXml("<COLUMNS><A required=\"obligatorio\"/></COLUMNS>"));
        assertRejects("columna «A»: valor inválido «» para «required»",
                () -> ExcelColumnsXml.fromXml("<COLUMNS><A required=\"\"/></COLUMNS>"));
        assertRejects("columna «B»: valor inválido «no» para «overwrite»",
                () -> ExcelColumnsXml.fromXml("<COLUMNS><B overwrite=\"no\"/></COLUMNS>"));
        assertRejects("columna «B»: valor inválido «1» para «overwrite»",
                () -> ExcelColumnsXml.fromXml("<COLUMNS><B overwrite=\"1\"/></COLUMNS>"));
        assertRejects("columna «C»: valor inválido «si» para «defaultwhenblank»",
                () -> ExcelColumnsXml.fromXml("<COLUMNS><C default=\"x\" defaultwhenblank=\"si\"/></COLUMNS>"));
    }

    @Test
    public void testDefaultWhenBlankSinDefaultLanza() {
        assertRejects("columna «C»: «defaultwhenblank» exige declarar «default»",
                () -> ExcelColumnsXml.fromXml("<COLUMNS><C defaultwhenblank=\"true\"/></COLUMNS>"));
        assertRejects("«defaultwhenblank» exige declarar «default»",
                () -> ExcelColumnsXml.fromXml("<COLUMNS><C defaultwhenblank=\"false\"/></COLUMNS>"));
    }

    @Test
    public void testCabeceraVaciaODuplicadaLanza() {
        assertRejects("columna «A»: la cabecera («header») no puede estar vacía",
                () -> ExcelColumnsXml.fromXml("<COLUMNS><A header=\"\"/></COLUMNS>"));
        assertRejects("columna «A»: la cabecera («header») no puede estar vacía",
                () -> ExcelColumnsXml.fromXml("<COLUMNS><A header=\"   \"/></COLUMNS>"));
        // dos tags distintos que producen la misma cabecera
        IllegalArgumentException ex = assertRejects("La cabecera «codigo» ya fue declarada",
                () -> ExcelColumnsXml.fromXml("<COLUMNS><CODIGO/><COD header=\"codigo\"/></COLUMNS>"));
        assertTrue(ex.getMessage().startsWith("Bloque «fragmento XML», columna «COD»: "), ex.getMessage());
        // misma cabecera con distinto trim también es duplicada
        assertRejects("ya fue declarada",
                () -> ExcelColumnsXml.fromXml("<COLUMNS><A header=\"x\"/><B header=\" x \"/></COLUMNS>"));
    }

    @Test
    public void testBloqueSinColumnasLanza() {
        assertRejects("Bloque «fragmento XML»: no declara ninguna columna",
                () -> ExcelColumnsXml.fromXml("<COLUMNS/>"));
        assertRejects("no declara ninguna columna",
                () -> ExcelColumnsXml.fromXml("<COLUMNS><!-- solo comentarios --></COLUMNS>"));
        assertRejects("Bloque «PAGE/EXCELIMPORT/DEFAULT»: no declara ninguna columna",
                () -> ExcelColumnsXml.fromXml("<XML><PAGE><EXCELIMPORT><DEFAULT><COLUMNS/></DEFAULT></EXCELIMPORT></PAGE></XML>",
                        NODE_PATH));
    }

    @Test
    public void testPadreSinHijoColumnsLanzaYNoLeeUnaColumnaColums() {
        IllegalArgumentException ex = assertRejects("se esperaba un hijo «COLUMNS» dentro de «DEFAULT»",
                () -> ExcelColumnsXml.fromXml("<DEFAULT><COLUMS><CODIGO/></COLUMS></DEFAULT>"));
        assertTrue(ex.getMessage().contains("[COLUMS]"), ex.getMessage());
        assertRejects("se esperaba un hijo «COLUMNS» dentro de «DEFAULT»",
                () -> ExcelColumnsXml.fromXml("<DEFAULT/>"));
        // el nombre del tag es exacto
        assertRejects("se esperaba un hijo «COLUMNS»",
                () -> ExcelColumnsXml.fromXml("<DEFAULT><columns><CODIGO/></columns></DEFAULT>"));
        assertRejects("«DEFAULT» tiene 2 hijos «COLUMNS» y se esperaba uno solo",
                () -> ExcelColumnsXml.fromXml("<DEFAULT><COLUMNS><A/></COLUMNS><COLUMNS><B/></COLUMNS></DEFAULT>"));
    }

    @Test
    public void testTagDeColumnaConHijosLanza() {
        assertRejects("columna «CODIGO»: un tag de columna no admite elementos hijos (se encontró «NOMBRE»)",
                () -> ExcelColumnsXml.fromXml("<COLUMNS><CODIGO><NOMBRE/></CODIGO></COLUMNS>"));
    }

    @Test
    public void testNodoInexistenteTextoInvalidoYNulos() {
        assertRejects("Bloque «PAGE/EXCELIMPORT/PROVEEDORES»: el nodo no existe en el XML",
                () -> ExcelColumnsXml.fromXml(CTACTESUB_XML, "PAGE/EXCELIMPORT/PROVEEDORES"));
        XmlDomW3c dom = new XmlDomW3c();
        assertTrue(dom.config("", CTACTESUB_XML, "", true));
        assertRejects("Bloque «PAGE/NADA»: el nodo no existe en el XML",
                () -> ExcelColumnsXml.fromXmlDom(dom, "PAGE/NADA"));
        assertRejects("la ruta del nodo es nula o vacía",
                () -> ExcelColumnsXml.fromXmlDom(dom, " "));
        assertRejects("el recurso XML es nulo o no está cargado",
                () -> ExcelColumnsXml.fromXmlDom(null, NODE_PATH));
        assertRejects("el recurso XML es nulo o no está cargado",
                () -> ExcelColumnsXml.fromXmlDom(new XmlDomW3c(), NODE_PATH));
        assertRejects("el texto no es un XML válido",
                () -> ExcelColumnsXml.fromXml("<COLUMNS><A/>"));
        assertRejects("el texto XML es nulo o vacío",
                () -> ExcelColumnsXml.fromXml("  "));
        assertRejects("el nodo XML es nulo",
                () -> ExcelColumnsXml.fromNode(null, "x"));
    }

    @Test
    public void testFromFileArchivoInexistenteONodoInexistente(@TempDir Path dir) throws Exception {
        String missing = dir.resolve("no_existe.xml").toString();
        IllegalArgumentException ex = assertRejects("no existe",
                () -> ExcelColumnsXml.fromFile(missing, NODE_PATH));
        assertTrue(ex.getMessage().startsWith("Bloque «PAGE/EXCELIMPORT/DEFAULT» (no_existe.xml): "), ex.getMessage());

        Path file = dir.resolve("ctactesub.xml");
        Files.writeString(file, CTACTESUB_XML, StandardCharsets.UTF_8);
        ex = assertRejects("no se pudo leer el bloque",
                () -> ExcelColumnsXml.fromFile(file.toString(), "PAGE/EXCELIMPORT/PROVEEDORES"));
        assertTrue(ex.getMessage().startsWith("Bloque «PAGE/EXCELIMPORT/PROVEEDORES» (ctactesub.xml): "), ex.getMessage());
        assertRejects("la ruta del nodo es nula o vacía",
                () -> ExcelColumnsXml.fromFile(file.toString(), ""));
        assertRejects("la ruta del archivo es nula o vacía",
                () -> ExcelColumnsXml.fromFile(null, NODE_PATH));
        // error del parser con el archivo en el mensaje
        Path bad = dir.resolve("malo.xml");
        Files.writeString(bad, CTACTESUB_XML.replace("<CODIGO required=\"VALUE\"/>",
                "<CODIGO requiered=\"VALUE\"/>"), StandardCharsets.UTF_8);
        ex = assertRejects("atributo desconocido «requiered»",
                () -> ExcelColumnsXml.fromFile(bad.toString(), NODE_PATH));
        assertTrue(ex.getMessage().startsWith("Bloque «PAGE/EXCELIMPORT/DEFAULT» (malo.xml), columna «CODIGO»: "),
                ex.getMessage());
    }

    // ----------------------------------------------------------------------
    // Herencia clase/src en fromFile (RF7, RF8, D9, CA1-05)
    // ----------------------------------------------------------------------

    @Test
    public void testFromFileResuelveHerenciaRelativaALaCarpetaYHeredadasPrimero(@TempDir Path dir) throws Exception {
        // La referencia src es un nombre pelado: solo se resuelve si el parser
        // pone la carpeta del archivo como parámetro «path».
        String clases = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<XML>\n"
                + "    <IMPORTCOLUMNS_BASE>\n"
                + "        <EMPRESA default=\"01\"/>\n"
                + "        <SUCURSAL default=\"01\" overwrite=\"false\"/>\n"
                + "        <NOMBRE default=\"sin nombre\" required=\"OPTIONAL\"/>\n"
                + "    </IMPORTCOLUMNS_BASE>\n"
                + "</XML>";
        String columnas = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<XML>\n"
                + "    <PAGE>\n"
                + "        <EXCELIMPORT>\n"
                + "            <DEFAULT>\n"
                + "                <COLUMNS clase=\"IMPORTCOLUMNS_BASE\" src=\"file://clases_prueba.xml\">\n"
                + "                    <CODIGO required=\"VALUE\"/>\n"
                + "                    <NOMBRE required=\"COLUMN\"/>\n"
                + "                </COLUMNS>\n"
                + "            </DEFAULT>\n"
                + "        </EXCELIMPORT>\n"
                + "    </PAGE>\n"
                + "</XML>";
        Files.writeString(dir.resolve("clases_prueba.xml"), clases, StandardCharsets.UTF_8);
        Path file = dir.resolve("columnas_prueba.xml");
        Files.writeString(file, columnas, StandardCharsets.UTF_8);

        ExcelColumns c = ExcelColumnsXml.fromFile(file.toString(), NODE_PATH);

        assertEquals(Arrays.asList("empresa", "sucursal", "codigo", "nombre"),
                new ArrayList<>(c.toHeadToField().keySet()), "las heredadas van primero");
        assertEquals("01", c.byHeader("empresa").orElseThrow().getDefaultValue().get());
        assertFalse(c.byHeader("sucursal").orElseThrow().isOverwrite());
        ExcelColumnSpec nombre = c.byHeader("nombre").orElseThrow();
        assertEquals(ColumnRequirement.COLUMN, nombre.getRequirement(), "el atributo propio no se pisa");
        assertTrue(nombre.hasDefaultValue(), "el atributo que falta se hereda");
        assertEquals("sin nombre", nombre.getDefaultValue().get());
        assertEquals(ColumnRequirement.VALUE, c.byHeader("codigo").orElseThrow().getRequirement());
    }

    @Test
    public void testFromXmlNoAplicaHerencia() {
        // El texto trae clase/src pero fromXml no hereda: solo las columnas propias
        ExcelColumns c = ExcelColumnsXml.fromXml("<COLUMNS clase=\"IMPORTCOLUMNS_BASE\" src=\"file://clases_prueba.xml\">"
                + "<CODIGO/></COLUMNS>");
        assertEquals(Arrays.asList("codigo"), new ArrayList<>(c.toHeadToField().keySet()));
    }

    // ----------------------------------------------------------------------
    // ExcelColumns.edit (RF9, D5, CA1-06)
    // ----------------------------------------------------------------------

    @Test
    public void testEditConservaPosicionYElRestoDeLosCampos() {
        ExcelColumns c = ExcelColumnsXml.fromXml(CTACTESUB_XML, NODE_PATH);
        List<String> before = new ArrayList<>(c.toHeadToField().keySet());
        Map<String, String> headToFieldBefore = c.toHeadToField();

        c.edit("esproveedor").converter(v -> "1".equals(String.valueOf(v)) ? 1 : 0);
        c.edit(" nombre ").require(ColumnRequirement.VALUE);

        assertEquals(before, new ArrayList<>(c.toHeadToField().keySet()), "la posición no cambia");
        assertEquals(headToFieldBefore, c.toHeadToField());
        assertEquals(22, c.size());
        ExcelColumnSpec esproveedor = c.byHeader("esproveedor").orElseThrow();
        assertNotNull(esproveedor.getConverter());
        assertEquals("esproveedor", esproveedor.getField());
        assertEquals(ColumnRequirement.OPTIONAL, esproveedor.getRequirement());
        assertTrue(esproveedor.hasDefaultValue());
        assertEquals("0", esproveedor.getDefaultValue().get());
        assertTrue(esproveedor.isDefaultValueFixed(), "el default fijo sigue marcado como fijo tras edit");
        assertFalse(esproveedor.isDefaultWhenBlank());
        assertFalse(esproveedor.isOverwrite(), "overwrite=\"false\" se conserva");
        ExcelColumnSpec nombre = c.byHeader("nombre").orElseThrow();
        assertEquals(ColumnRequirement.VALUE, nombre.getRequirement());
        assertTrue(nombre.isOverwrite());
        assertNull(nombre.getConverter());
    }

    @Test
    public void testEditConservaDefaultWhenBlankYSupplier() {
        ExcelColumns c = new ExcelColumns();
        c.add("a", "a").defaultValue("x").defaultWhenBlank().noOverwrite();
        c.add("b", "b").defaultValue(() -> "calc");
        c.edit("a").converter(v -> v);
        c.edit("b").required();
        ExcelColumnSpec a = c.byHeader("a").orElseThrow();
        assertTrue(a.isDefaultWhenBlank());
        assertTrue(a.isDefaultValueFixed());
        assertFalse(a.isOverwrite());
        assertEquals("x", a.getDefaultValue().get());
        ExcelColumnSpec b = c.byHeader("b").orElseThrow();
        assertEquals(ColumnRequirement.COLUMN, b.getRequirement());
        assertTrue(b.hasDefaultValue());
        assertFalse(b.isDefaultValueFixed(), "el proveedor sigue siendo calculado");
        assertEquals("calc", b.getDefaultValue().get());
    }

    @Test
    public void testEditDeCabeceraInexistenteLanza() {
        ExcelColumns c = ExcelColumnsXml.fromXml("<COLUMNS><CODIGO/><NOMBRE/></COLUMNS>");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> c.edit("fecha"));
        assertTrue(ex.getMessage().contains("«fecha» no fue declarada"), ex.getMessage());
        assertTrue(ex.getMessage().contains("codigo"), ex.getMessage());
        assertThrows(IllegalArgumentException.class, () -> c.edit(null));
        assertEquals(2, c.size(), "un edit fallido no agrega nada");
    }

    // ----------------------------------------------------------------------
    // Integración mínima con ExcelRowProcessor (§1.3.3)
    // ----------------------------------------------------------------------

    static final String APPUSER_XML = "<COLUMNS>"
            + "<CODE required=\"VALUE\"/>"
            + "<FULLNAME header=\"fullName\" required=\"COLUMN\"/>"
            + "<DESCRIPCION header=\"Descripción\" field=\"description\" required=\"VALUE\"/>"
            + "<IDCOMPANY default=\"12\"/>"
            + "<DISABLED default=\"1\"/>"
            + "<EMAIL1 header=\"email1\" overwrite=\"false\"/>"
            + "</COLUMNS>";

    private Workbook buildWorkbook(String[] headers, Object[] values) {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("data");
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }
        Row data = sheet.createRow(1);
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (value == null) {
                data.createCell(i);
            } else if (value instanceof String) {
                data.createCell(i).setCellValue((String) value);
            } else if (value instanceof Double) {
                data.createCell(i).setCellValue((Double) value);
            }
        }
        return wb;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testProcesadorConColumnasDelXmlProcesaLaFila() throws Exception {
        try (Workbook wb = buildWorkbook(new String[]{"code", "fullName", "Descripción", "email1"},
                new Object[]{"jenciso", "Jorge Enciso", "Analista", "j@x.com"})) {
            Row row = wb.getSheetAt(0).getRow(1);
            ExcelColumns columns = ExcelColumnsXml.fromXml(APPUSER_XML);
            ExcelRowProcessor<AppUser> p = new ExcelRowProcessor<AppUser>(row, AppUser.class, columns, 0, null) {
            };
            assertEquals("", p.checkMetaData());
            assertEquals(Arrays.asList("code", "fullName", "Descripción", "idcompany", "disabled", "email1"),
                    new ArrayList<>(p.getHeadToField().keySet()));
            AppUser user = p.process();
            assertEquals("jenciso", user.getCode());
            assertEquals("Jorge Enciso", user.getFullName());
            assertEquals("Analista", user.getDescription());
            assertEquals(Long.valueOf(12L), user.getIdcompany(), "default \"12\" convertido a Long");
            assertEquals(Boolean.TRUE, user.getDisabled(), "default \"1\" convertido a Boolean");
            assertEquals("j@x.com", user.getEmail1());
            assertTrue(user.getErrors() == null || user.getErrors().isEmpty(), String.valueOf(user.getErrors()));
            Set<String> noOverwrite = (Set<String>) user.getProperties().get(ExcelRowProcessor.NO_OVERWRITE_FIELDS);
            assertEquals(Set.of("email1"), noOverwrite);
            Set<String> defaulted = (Set<String>) user.getProperties().get(ExcelRowProcessor.DEFAULTED_FIELDS);
            assertEquals(Set.of("idcompany", "disabled"), defaulted);
        }
    }

    @Test
    public void testProcesadorConColumnasDelXmlReclamaRequiredAusente() throws Exception {
        try (Workbook wb = buildWorkbook(new String[]{"code"}, new Object[]{"jenciso"})) {
            Row row = wb.getSheetAt(0).getRow(1);
            ExcelRowProcessor<AppUser> p = new ExcelRowProcessor<AppUser>(row, AppUser.class,
                    ExcelColumnsXml.fromXml(APPUSER_XML), 0, null) {
            };
            String msg = p.checkMetaData();
            assertTrue(msg.contains("Falta la columna obligatoria «fullName»"), msg);
            assertTrue(msg.contains("Falta la columna obligatoria «Descripción»"), msg);
            assertFalse(msg.contains("idcompany"), msg);
        }
    }

    @Test
    public void testProcesadorConColumnasDelXmlMarcaValueVacio() throws Exception {
        try (Workbook wb = buildWorkbook(new String[]{"code", "fullName", "Descripción"},
                new Object[]{"jenciso", "Jorge Enciso", null})) {
            Row row = wb.getSheetAt(0).getRow(1);
            ExcelRowProcessor<AppUser> p = new ExcelRowProcessor<AppUser>(row, AppUser.class,
                    ExcelColumnsXml.fromXml(APPUSER_XML), 0, null) {
            };
            assertEquals("", p.checkMetaData());
            AppUser user = p.process();
            assertNull(user.getDescription());
            Map<String, IErrorReg> errores = user.getErrors();
            assertNotNull(errores);
            IErrorReg error = errores.get("description");
            assertNotNull(error, String.valueOf(errores));
            assertEquals(Integer.valueOf(50000), error.getErrorNumber());
            assertTrue(error.getMessage().contains("«Descripción»"), error.getMessage());
            assertTrue(error.getMessage().contains("no puede estar vacía"), error.getMessage());
        }
    }

    @Test
    public void testConverterAgregadoConEditSeAplicaEnElProcesador() throws Exception {
        try (Workbook wb = buildWorkbook(new String[]{"code", "fullName", "Descripción"},
                new Object[]{"jenciso", "jorge enciso", "Analista"})) {
            Row row = wb.getSheetAt(0).getRow(1);
            ExcelColumns columns = ExcelColumnsXml.fromXml(APPUSER_XML);
            columns.edit("fullName").converter(v -> String.valueOf(v).toUpperCase());
            ExcelRowProcessor<AppUser> p = new ExcelRowProcessor<AppUser>(row, AppUser.class, columns, 0, null) {
            };
            assertEquals(Arrays.asList("code", "fullName", "Descripción", "idcompany", "disabled", "email1"),
                    new ArrayList<>(p.getHeadToField().keySet()), "edit no mueve la columna");
            assertNotNull(p.getColumns().byHeader("fullName").orElseThrow().getConverter());
            AppUser user = p.process();
            assertEquals("JORGE ENCISO", user.getFullName());
        }
    }

    @Test
    public void testDefaultDelXmlNoConvertibleLoReclamaCheckMetaData() throws Exception {
        try (Workbook wb = buildWorkbook(new String[]{"code"}, new Object[]{"jenciso"})) {
            Row row = wb.getSheetAt(0).getRow(1);
            ExcelColumns columns = ExcelColumnsXml.fromXml("<COLUMNS><CODE/><IDCOMPANY default=\"abc\"/></COLUMNS>");
            ExcelRowProcessor<AppUser> p = new ExcelRowProcessor<AppUser>(row, AppUser.class, columns, 0, null) {
            };
            String msg = p.checkMetaData();
            assertTrue(msg.contains("El valor por defecto de la columna «idcompany» no es convertible a Long"), msg);
        }
    }
}
