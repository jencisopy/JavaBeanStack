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
package org.javabeanstack.xml;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.javabeanstack.data.DataRow;
import org.javabeanstack.data.IDataRow;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pruebas del mapeo de datos XML al modelo (plan XMLMAP, §2.3). Son pruebas
 * puras: no requieren servidor ni base de datos.
 *
 * @author Jorge Enciso
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class XmlRowMapperTest {

    @TempDir
    Path tempDir;

    public XmlRowMapperTest() {
    }

    /**
     * Enumeración de prueba.
     */
    public enum Color {
        ROJO, VERDE, AZUL
    }

    /**
     * Entidad de prueba con un atributo de cada tipo soportado.
     */
    public static class EntidadPrueba extends DataRow {

        private static final long serialVersionUID = 1L;
        private static final String CONSTANTE = "no es un atributo";

        private Long idprueba;
        private String code;
        private String descrip;
        private Boolean activo;
        private boolean vigente;
        private long cantidad;
        private Integer orden;
        private Short tipo;
        private BigDecimal monto;
        private Double promedio;
        private Character marca;
        private LocalDate fecha;
        private LocalDateTime fechahora;
        private java.util.Date vencimiento;
        private Color color;
        private String explanation;
        private transient String temporal;

        public Long getIdprueba() {
            return idprueba;
        }

        public void setIdprueba(Long idprueba) {
            this.idprueba = idprueba;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getDescrip() {
            return descrip;
        }

        public void setDescrip(String descrip) {
            this.descrip = descrip;
        }

        public Boolean getActivo() {
            return activo;
        }

        public void setActivo(Boolean activo) {
            this.activo = activo;
        }

        public boolean isVigente() {
            return vigente;
        }

        public void setVigente(boolean vigente) {
            this.vigente = vigente;
        }

        public long getCantidad() {
            return cantidad;
        }

        public void setCantidad(long cantidad) {
            this.cantidad = cantidad;
        }

        public Integer getOrden() {
            return orden;
        }

        public void setOrden(Integer orden) {
            this.orden = orden;
        }

        public Short getTipo() {
            return tipo;
        }

        public void setTipo(Short tipo) {
            this.tipo = tipo;
        }

        public BigDecimal getMonto() {
            return monto;
        }

        public void setMonto(BigDecimal monto) {
            this.monto = monto;
        }

        public Double getPromedio() {
            return promedio;
        }

        public void setPromedio(Double promedio) {
            this.promedio = promedio;
        }

        public Character getMarca() {
            return marca;
        }

        public void setMarca(Character marca) {
            this.marca = marca;
        }

        public LocalDate getFecha() {
            return fecha;
        }

        public void setFecha(LocalDate fecha) {
            this.fecha = fecha;
        }

        public LocalDateTime getFechahora() {
            return fechahora;
        }

        public void setFechahora(LocalDateTime fechahora) {
            this.fechahora = fechahora;
        }

        public java.util.Date getVencimiento() {
            return vencimiento;
        }

        public void setVencimiento(java.util.Date vencimiento) {
            this.vencimiento = vencimiento;
        }

        public Color getColor() {
            return color;
        }

        public void setColor(Color color) {
            this.color = color;
        }

        public String getExplanation() {
            return explanation;
        }

        public void setExplanation(String explanation) {
            this.explanation = explanation;
        }

        public String getTemporal() {
            return temporal;
        }

        public void setTemporal(String temporal) {
            this.temporal = temporal;
        }

        public static String getConstante() {
            return CONSTANTE;
        }
    }

    /**
     * Entidad de prueba que hereda los atributos de otra.
     */
    public static class EntidadHija extends EntidadPrueba {

        private static final long serialVersionUID = 1L;

        private String extra;

        public String getExtra() {
            return extra;
        }

        public void setExtra(String extra) {
            this.extra = extra;
        }
    }

    /**
     * Entidad de prueba con dos atributos que solo difieren en mayúsculas y
     * sin modificadores.
     */
    public static class EntidadAmbigua extends DataRow {

        private static final long serialVersionUID = 1L;

        private Long idcliente;
        private String idCliente;

        public Long getIdclienteNumero() {
            return idcliente;
        }

        public String getIdClienteTexto() {
            return idCliente;
        }
    }

    /**
     * Entidad ambigua con los dos modificadores y del <b>mismo tipo</b>: es el
     * caso real de una entidad JPA y el que antes de E3-01 escribía en
     * silencio en el atributo equivocado. Los modificadores se declaran en
     * orden inverso al de los campos a propósito.
     */
    public static class EntidadAmbiguaSetters extends DataRow {

        private static final long serialVersionUID = 1L;

        private Long idcliente;
        private Long idCliente;

        public void setIdCliente(Long idCliente) {
            this.idCliente = idCliente;
        }

        public Long getIdClienteMayuscula() {
            return idCliente;
        }

        public void setIdcliente(Long idcliente) {
            this.idcliente = idcliente;
        }

        public Long getIdclienteMinuscula() {
            return idcliente;
        }
    }

    /**
     * Entidad ambigua con los dos modificadores y de <b>tipos distintos</b>,
     * declarados en el orden de los campos.
     */
    public static class EntidadAmbiguaTipos extends DataRow {

        private static final long serialVersionUID = 1L;

        private Long idcliente;
        private String idCliente;

        public void setIdcliente(Long idcliente) {
            this.idcliente = idcliente;
        }

        public Long getIdclienteNumero() {
            return idcliente;
        }

        public void setIdCliente(String idCliente) {
            this.idCliente = idCliente;
        }

        public String getIdClienteTexto() {
            return idCliente;
        }
    }

    /**
     * Entidad con un campo calculado anotado {@code @Transient}: no es un dato
     * que pueda venir del XML.
     */
    public static class EntidadConTransient extends DataRow {

        private static final long serialVersionUID = 1L;

        private String code;
        @jakarta.persistence.Transient
        private String calculado;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getCalculado() {
            return calculado;
        }

        public void setCalculado(String calculado) {
            this.calculado = calculado;
        }
    }

    /**
     * Devuelve el primer elemento de un texto xml.
     *
     * @param xml texto xml.
     * @return elemento raíz.
     * @throws Exception error de parseo.
     */
    private Element nodo(String xml) throws Exception {
        Document dom = DomW3cParser.loadXml(xml);
        return dom.getDocumentElement();
    }

    /**
     * Devuelve el conjunto de atributos mapeados de una fila.
     *
     * @param row fila mapeada.
     * @return conjunto de atributos asignados desde el XML.
     */
    @SuppressWarnings("unchecked")
    private Set<String> mapeados(IDataRow row) {
        return (Set<String>) row.getProperty(IXmlRowMapper.MAPPED_FIELDS);
    }

    /**
     * toRow asigna por nombre sin distinguir mayúsculas y convierte cada tipo
     * (CA2-01), deja la fila en INSERT y registra los atributos asignados
     * (RF11).
     */
    @Test
    public void test01ToRowTiposYCaja() throws Exception {
        System.out.println("1-XmlRowMapper - tipos y caja de los nombres");
        Element node = nodo("<ROW IDPRUEBA=\"10\" CODE=\"A1\" Descrip=\"Primera fila\""
                + " activo=\"T\" vigente=\"1\" cantidad=\"25\" orden=\"3\" tipo=\"2\""
                + " monto=\"1500.35\" promedio=\"7.5\" marca=\"X\" fecha=\"2026-10-25\""
                + " fechahora=\"2026-10-25T13:45:30\" vencimiento=\"2026-10-25\" color=\"verde\"/>");

        EntidadPrueba row = new XmlRowMapper().toRow(node, EntidadPrueba.class);

        assertTrue(row.getErrors().isEmpty(), row.getErrorMsg());
        assertEquals(10L, row.getIdprueba());
        assertEquals("A1", row.getCode());
        assertEquals("Primera fila", row.getDescrip());
        assertEquals(Boolean.TRUE, row.getActivo());
        assertTrue(row.isVigente());
        assertEquals(25L, row.getCantidad());
        assertEquals(3, row.getOrden());
        assertEquals(Short.valueOf("2"), row.getTipo());
        assertEquals(new BigDecimal("1500.35"), row.getMonto());
        assertEquals(7.5D, row.getPromedio(), 0.000001D);
        assertEquals('X', row.getMarca());
        assertEquals(LocalDate.of(2026, 10, 25), row.getFecha());
        assertEquals(LocalDateTime.of(2026, 10, 25, 13, 45, 30), row.getFechahora());
        assertNotNull(row.getVencimiento());
        assertEquals(Color.VERDE, row.getColor());
        //La fila nueva queda en INSERT (RF11)
        assertEquals(IDataRow.INSERT, row.getAction());
        //Los atributos asignados quedan anotados con su nombre canónico
        Set<String> mapeados = mapeados(row);
        assertTrue(mapeados.contains("idprueba"));
        assertTrue(mapeados.contains("code"));
        assertTrue(mapeados.contains("descrip"));
        assertEquals(15, mapeados.size());
    }

    /**
     * Los atributos heredados de una superclase del modelo también se asignan
     * (RF5).
     */
    @Test
    public void test02Herencia() throws Exception {
        System.out.println("2-XmlRowMapper - herencia de atributos del modelo");
        Element node = nodo("<ROW code=\"A1\" extra=\"valor\"/>");

        EntidadHija row = new XmlRowMapper().toRow(node, EntidadHija.class);

        assertTrue(row.getErrors().isEmpty(), row.getErrorMsg());
        assertEquals("A1", row.getCode());
        assertEquals("valor", row.getExtra());
    }

    /**
     * Los campos de DataRow no son alcanzables desde un atributo del XML
     * (CA2-05): ni asignan ni existen para el mapeador.
     */
    @Test
    public void test03CamposDeDataRowInalcanzables() throws Exception {
        System.out.println("3-XmlRowMapper - campos de DataRow inalcanzables");
        Element node = nodo("<ROW action=\"3\" errors=\"x\" persistMode=\"2\" signature=\"falsa\" code=\"A1\"/>");

        EntidadPrueba row = new XmlRowMapper().toRow(node, EntidadPrueba.class);

        assertEquals("A1", row.getCode());
        assertEquals(IDataRow.INSERT, row.getAction());
        assertEquals("SINFIRMA", row.getSignature());
        assertTrue(row.getErrors().isEmpty(), row.getErrorMsg());
        assertEquals(1, mapeados(row).size());

        //En modo estricto esos atributos se denuncian como no mapeados
        EntidadPrueba estricta = new XmlRowMapper().strict(true).toRow(node, EntidadPrueba.class);
        assertEquals(4, estricta.getErrors().size());
        assertNotNull(estricta.getErrors().get("action"));
        assertEquals(50000, estricta.getErrors().get("action").getErrorNumber());
    }

    /**
     * Los campos static y transient no son atributos del modelo.
     */
    @Test
    public void test04StaticYTransient() throws Exception {
        System.out.println("4-XmlRowMapper - static y transient no se asignan");
        Element node = nodo("<ROW CONSTANTE=\"otra\" temporal=\"x\" code=\"A1\"/>");

        EntidadPrueba row = new XmlRowMapper().strict(true).toRow(node, EntidadPrueba.class);

        assertNull(row.getTemporal());
        assertEquals("no es un atributo", EntidadPrueba.getConstante());
        assertEquals(2, row.getErrors().size());
        assertNotNull(row.getErrors().get("constante"));
        assertNotNull(row.getErrors().get("temporal"));
    }

    /**
     * Atributo desconocido en los tres modos: ignorado (por defecto),
     * denunciado al observador y error de la fila en modo estricto (RF6).
     */
    @Test
    public void test05AtributoDesconocido() throws Exception {
        System.out.println("5-XmlRowMapper - atributo desconocido");
        Element node = nodo("<ROW code=\"A1\" inexistente=\"valor\"/>");

        //Por defecto se ignora
        EntidadPrueba row = new XmlRowMapper().toRow(node, EntidadPrueba.class);
        assertTrue(row.getErrors().isEmpty());
        assertEquals("A1", row.getCode());

        //Con observador: avisa y no falla
        List<String> avisos = new ArrayList();
        row = new XmlRowMapper()
                .onUnmapped((nodo, nombre) -> avisos.add(nombre))
                .toRow(node, EntidadPrueba.class);
        assertTrue(row.getErrors().isEmpty());
        assertEquals(1, avisos.size());
        assertEquals("inexistente", avisos.get(0));

        //En modo estricto es un error de la fila, con número 50000
        row = new XmlRowMapper().strict(true).toRow(node, EntidadPrueba.class);
        assertEquals(1, row.getErrors().size());
        assertEquals(50000, row.getErrors().get("inexistente").getErrorNumber());
        assertEquals("inexistente", row.getErrors().get("inexistente").getFieldName());
        //El resto de los atributos se asignó igual
        assertEquals("A1", row.getCode());
    }

    /**
     * Un hijo simple es el valor de un atributo; un hijo con estructura se
     * ignora (RF4). El texto se toma <b>tal cual</b>, con sus saltos de línea
     * y su sangría (D nueva, hallazgo E3-03).
     */
    @Test
    public void test06HijosSimples() throws Exception {
        System.out.println("6-XmlRowMapper - elementos hijos simples");
        String largo = "\n   Texto largo\n   en varias lineas\n";
        String xml = "<ROW code=\"A1\">"
                + "<descrip>Texto corto</descrip>"
                + "<explanation>" + largo + "</explanation>"
                + "<detalle tipo=\"1\"><item/></detalle>"
                + "</ROW>";
        Element node = nodo(xml);

        EntidadPrueba row = new XmlRowMapper().toRow(node, EntidadPrueba.class);

        assertTrue(row.getErrors().isEmpty(), row.getErrorMsg());
        assertEquals("Texto corto", row.getDescrip());
        assertEquals(largo, row.getExplanation(),
                "el texto de un hijo simple no se altera: es el dato");
        assertTrue(mapeados(row).contains("explanation"));

        //Deshabilitado, los hijos no aportan nada
        row = new XmlRowMapper().childElementsAsFields(false).toRow(node, EntidadPrueba.class);
        assertNull(row.getDescrip());
        assertNull(row.getExplanation());

        //En modo estricto el hijo con estructura se denuncia
        row = new XmlRowMapper().strict(true).toRow(node, EntidadPrueba.class);
        assertEquals(1, row.getErrors().size());
        assertNotNull(row.getErrors().get("detalle"));
        assertEquals(largo, row.getExplanation());
    }

    /**
     * El texto de un hijo simple se conserva byte a byte —CRLF y tabuladores
     * incluidos— y solo se normaliza si se lo pide con
     * {@code normalizeChildText(true)} (hallazgo E3-03, origen
     * {@code CATXML·E4-01}).
     */
    @Test
    public void test06bTextoDelHijoTalCual() throws Exception {
        System.out.println("6b-XmlRowMapper - el texto del hijo no se normaliza");
        //Un authdef del catalogo de Maker: CRLF y tabuladores significativos.
        //El CR va como referencia de caracter porque un CR literal lo
        //normalizaria el propio parser (CRLF -> LF), que no es asunto del
        //mapeador.
        String authdef = "<AUTHORIZATIONS execute=\"\">\r\n\t<FIELDS>\r\n"
                + "\t\t<CTBCUENTA    edit=\"\" label=\"Cuenta\"/>\r\n\t</FIELDS>\r\n"
                + "</AUTHORIZATIONS>";
        String xml = "<ROW code=\"A1\"><explanation>"
                + authdef.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                        .replace("\r", "&#13;")
                + "</explanation></ROW>";
        Element node = nodo(xml);

        EntidadPrueba row = new XmlRowMapper().toRow(node, EntidadPrueba.class);
        assertTrue(row.getErrors().isEmpty(), row.getErrorMsg());
        assertEquals(authdef, row.getExplanation(),
                "el texto largo con formato tiene que volver byte a byte");

        //Con la opcion encendida se recupera la regla anterior.
        row = new XmlRowMapper().normalizeChildText(true).toRow(node, EntidadPrueba.class);
        assertEquals("<AUTHORIZATIONS execute=\"\"> <FIELDS> <CTBCUENTA edit=\"\""
                + " label=\"Cuenta\"/> </FIELDS> </AUTHORIZATIONS>", row.getExplanation(),
                "con normalizeChildText(true) se recortan y colapsan los blancos");

        //Y el fill sobre una fila existente sigue la misma regla.
        EntidadPrueba existente = new EntidadPrueba();
        new XmlRowMapper().fill(node, existente);
        assertEquals(authdef, existente.getExplanation());
    }

    /**
     * Vacíos: nulo en los tipos que no son texto, texto vacío en String y
     * nulo también en String con emptyAsNull (RF8).
     */
    @Test
    public void test07Vacios() throws Exception {
        System.out.println("7-XmlRowMapper - valores vacíos");
        Element node = nodo("<ROW idprueba=\"\" descrip=\"\" fecha=\"\" color=\"\"/>");

        EntidadPrueba row = new XmlRowMapper().toRow(node, EntidadPrueba.class);
        assertTrue(row.getErrors().isEmpty(), row.getErrorMsg());
        assertNull(row.getIdprueba());
        assertNull(row.getFecha());
        assertNull(row.getColor());
        assertEquals("", row.getDescrip());

        row = new XmlRowMapper().emptyAsNull(true).toRow(node, EntidadPrueba.class);
        assertNull(row.getDescrip());
    }

    /**
     * Una conversión fallida deja el error 50000 en la fila con el nombre del
     * atributo y no interrumpe el resto del mapeo (CA2-03).
     */
    @Test
    public void test08ConversionFallida() throws Exception {
        System.out.println("8-XmlRowMapper - conversión fallida");
        Element node = nodo("<ROW code=\"A1\" orden=\"abc\" monto=\"1500.35\" activo=\"S\"/>");

        EntidadPrueba row = new XmlRowMapper().toRow(node, EntidadPrueba.class);

        assertEquals(2, row.getErrors().size());
        assertEquals(50000, row.getErrors().get("orden").getErrorNumber());
        assertEquals("orden", row.getErrors().get("orden").getFieldName());
        assertTrue(row.getErrors().get("orden").getMessage().contains("abc"));
        //"S" no es un booleano válido (D5)
        assertNotNull(row.getErrors().get("activo"));
        //Los demás atributos se asignaron igual
        assertEquals("A1", row.getCode());
        assertEquals(new BigDecimal("1500.35"), row.getMonto());
        assertNull(row.getOrden());
        assertFalse(mapeados(row).contains("orden"));
    }

    /**
     * Resolvedor por campo: el que devuelve el valor y el que rechaza el texto
     * (RF10).
     */
    @Test
    public void test09Resolvedores() throws Exception {
        System.out.println("9-XmlRowMapper - resolvedores por campo");
        Map<String, Long> catalogo = new HashMap<>();
        catalogo.put("Carpeta", 7L);
        Element node = nodo("<ROW code=\"A1\" idprueba=\"Carpeta\"/>");

        EntidadPrueba row = new XmlRowMapper()
                .resolver("IDPRUEBA", (campo, texto, nodo, fila) -> {
                    Long id = catalogo.get(texto);
                    if (id == null) {
                        throw new Exception("no existe el tipo \"" + texto + "\"");
                    }
                    return id;
                })
                .toRow(node, EntidadPrueba.class);

        assertTrue(row.getErrors().isEmpty(), row.getErrorMsg());
        assertEquals(7L, row.getIdprueba());

        //El resolvedor que rechaza deja el error en la fila
        Element otro = nodo("<ROW code=\"A1\" idprueba=\"Inexistente\"/>");
        row = new XmlRowMapper()
                .resolver("idprueba", (campo, texto, nodo, fila) -> {
                    Long id = catalogo.get(texto);
                    if (id == null) {
                        throw new Exception("no existe el tipo \"" + texto + "\"");
                    }
                    return id;
                })
                .toRow(otro, EntidadPrueba.class);
        assertEquals(1, row.getErrors().size());
        assertEquals(50000, row.getErrors().get("idprueba").getErrorNumber());
        assertTrue(row.getErrors().get("idprueba").getMessage().contains("Inexistente"));
        assertNull(row.getIdprueba());
        assertEquals("A1", row.getCode());
    }

    /**
     * fill no toca los atributos ausentes del nodo ni los declarados en
     * ignore (CA2-04), y conserva la acción de la fila.
     */
    @Test
    public void test10FillRespetaLoAusente() throws Exception {
        System.out.println("10-XmlRowMapper - fill respeta lo ausente");
        EntidadPrueba row = new EntidadPrueba();
        row.setIdprueba(99L);
        row.setCode("VIEJO");
        row.setDescrip("Descripción anterior");
        row.setOrden(5);
        row.setAction(IDataRow.UPDATE);

        Element node = nodo("<ROW idprueba=\"1\" code=\"NUEVO\" orden=\"8\"/>");
        EntidadPrueba resultado = new XmlRowMapper().ignore("IDPRUEBA").fill(node, row);

        assertSame(row, resultado);
        //ignore: la clave primaria no se pisa
        assertEquals(99L, row.getIdprueba());
        //lo que trae el nodo se pisa
        assertEquals("NUEVO", row.getCode());
        assertEquals(8, row.getOrden());
        //lo ausente queda como estaba
        assertEquals("Descripción anterior", row.getDescrip());
        //fill conserva la acción de la fila
        assertEquals(IDataRow.UPDATE, row.getAction());
        Set<String> mapeados = mapeados(row);
        assertEquals(2, mapeados.size());
        assertFalse(mapeados.contains("idprueba"));
    }

    /**
     * Dos atributos del modelo que solo difieren en mayúsculas: gana el
     * nombre exacto y, si no lo hay, la fila recibe el error (D2, R1).
     */
    @Test
    public void test11Ambiguedad() throws Exception {
        System.out.println("11-XmlRowMapper - nombres ambiguos");
        //Un nombre ambiguo SIEMPRE es error, aunque coincida exactamente con uno
        //de los dos atributos: la capa que asigna no los distingue (E3-01).
        for (String xml : new String[]{"<ROW idcliente=\"15\"/>", "<ROW idCliente=\"15\"/>",
            "<ROW IDCLIENTE=\"15\"/>"}) {
            EntidadAmbigua row = new XmlRowMapper().toRow(nodo(xml), EntidadAmbigua.class);
            assertEquals(1, row.getErrors().size(), xml);
            assertEquals(50000, row.getErrors().get("idcliente").getErrorNumber(), xml);
            assertTrue(row.getErrors().get("idcliente").getMessage().contains("ambiguo"), xml);
            assertNull(row.getIdclienteNumero(), xml);
            assertNull(row.getIdClienteTexto(), xml);
            assertNull(mapeados(row), xml);
        }
    }

    /**
     * Regresión de E3-01: con los dos modificadores declarados —el caso de una
     * entidad JPA— el valor NO puede terminar en el atributo equivocado.
     * Antes de la corrección esto asignaba `idCliente` sin ningún error.
     */
    @Test
    public void test11bAmbiguedadConSetters() throws Exception {
        System.out.println("11b-XmlRowMapper - nombres ambiguos con modificadores");
        //Mismo tipo en los dos atributos (antes: escritura silenciosa)
        for (String xml : new String[]{"<ROW idcliente=\"15\"/>", "<ROW idCliente=\"15\"/>",
            "<ROW IDCLIENTE=\"15\"/>"}) {
            EntidadAmbiguaSetters row = new XmlRowMapper().toRow(nodo(xml), EntidadAmbiguaSetters.class);
            assertEquals(1, row.getErrors().size(), xml);
            assertEquals(50000, row.getErrors().get("idcliente").getErrorNumber(), xml);
            assertTrue(row.getErrors().get("idcliente").getMessage().contains("ambiguo"), xml);
            assertNull(row.getIdclienteMinuscula(), xml);
            assertNull(row.getIdClienteMayuscula(), xml);
            assertNull(mapeados(row), xml);
        }
        //Tipos distintos y modificadores en el otro orden
        for (String xml : new String[]{"<ROW idcliente=\"15\"/>", "<ROW idCliente=\"texto\"/>"}) {
            EntidadAmbiguaTipos row = new XmlRowMapper().toRow(nodo(xml), EntidadAmbiguaTipos.class);
            assertEquals(1, row.getErrors().size(), xml);
            assertEquals(50000, row.getErrors().get("idcliente").getErrorNumber(), xml);
            assertNull(row.getIdclienteNumero(), xml);
            assertNull(row.getIdClienteTexto(), xml);
        }
        //El mensaje nombra a los dos atributos en conflicto
        EntidadAmbiguaTipos row = new XmlRowMapper()
                .toRow(nodo("<ROW idcliente=\"15\"/>"), EntidadAmbiguaTipos.class);
        String mensaje = row.getErrors().get("idcliente").getMessage();
        assertTrue(mensaje.contains("idcliente"), mensaje);
        assertTrue(mensaje.contains("idCliente"), mensaje);
        //Y un atributo ambiguo declarado en ignore no molesta
        row = new XmlRowMapper().ignore("idcliente")
                .toRow(nodo("<ROW idcliente=\"15\"/>"), EntidadAmbiguaTipos.class);
        assertTrue(row.getErrors().isEmpty(), row.getErrorMsg());
    }

    /**
     * Formatos de fecha propios del mapeador (RF12).
     */
    @Test
    public void test12FormatoDeFecha() throws Exception {
        System.out.println("12-XmlRowMapper - formatos de fecha");
        Element node = nodo("<ROW fecha=\"25/10/2026\" fechahora=\"25/10/2026 13:45:30\"/>");

        EntidadPrueba row = new XmlRowMapper()
                .dateFormat("dd/MM/yyyy")
                .dateTimeFormat("dd/MM/yyyy HH:mm:ss")
                .toRow(node, EntidadPrueba.class);

        assertTrue(row.getErrors().isEmpty(), row.getErrorMsg());
        assertEquals(LocalDate.of(2026, 10, 25), row.getFecha());
        assertEquals(LocalDateTime.of(2026, 10, 25, 13, 45, 30), row.getFechahora());

        //Sin declarar el formato, ese texto no es una fecha ISO
        row = new XmlRowMapper().toRow(node, EntidadPrueba.class);
        assertEquals(2, row.getErrors().size());
        assertNull(row.getFecha());
    }

    /**
     * toRows por lista de nodos y por XPath devuelve el orden del documento
     * (CA2-02).
     */
    @Test
    public void test13ToRowsListaYXPath() throws Exception {
        System.out.println("13-XmlRowMapper - toRows por lista y por xpath");
        String xml = "<DATA>"
                + "<ROW code=\"A1\" orden=\"1\"/>"
                + "<ROW code=\"A2\" orden=\"2\"/>"
                + "<ROW code=\"A3\" orden=\"3\"/>"
                + "</DATA>";
        Document dom = DomW3cParser.loadXml(xml);

        List<EntidadPrueba> filas = new XmlRowMapper().toRows(dom, "/DATA/ROW", EntidadPrueba.class);
        assertEquals(3, filas.size());
        assertEquals("A1", filas.get(0).getCode());
        assertEquals("A2", filas.get(1).getCode());
        assertEquals("A3", filas.get(2).getCode());
        assertEquals(IDataRow.INSERT, filas.get(2).getAction());

        List<Element> nodos = DomW3cParser.getElements(dom, "/DATA/ROW");
        List<EntidadPrueba> porLista = new XmlRowMapper().toRows(nodos, EntidadPrueba.class);
        assertEquals(3, porLista.size());
        assertEquals("A3", porLista.get(2).getCode());

        //Una expresión que no encuentra nada devuelve una lista vacía
        assertTrue(new XmlRowMapper().toRows(dom, "/DATA/OTRO", EntidadPrueba.class).isEmpty());
    }

    /**
     * toRows sobre un IXmlDom aprovecha la herencia de textos xml
     * (clase/src) sin que el mapeador la implemente (RF3).
     */
    @Test
    public void test14ToRowsConIXmlDom() throws Exception {
        System.out.println("14-XmlRowMapper - toRows con IXmlDom y herencia");
        String baseXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<base descrip=\"heredado\" orden=\"7\"/>";
        Path baseFile = tempDir.resolve("base.xml");
        Files.write(baseFile, baseXml.getBytes(StandardCharsets.UTF_8));

        String mainXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ROW clase=\"base\" src=\"file://" + baseFile.toFile().getPath() + "\""
                + " code=\"A1\" orden=\"9\"/>";

        XmlDomW3c xmlDom = new XmlDomW3c();
        assertTrue(xmlDom.config("", mainXml, "", false));

        List<EntidadPrueba> filas = new XmlRowMapper()
                .ignore("clase", "src")
                .toRows(xmlDom, "/ROW", EntidadPrueba.class);

        assertEquals(1, filas.size());
        EntidadPrueba fila = filas.get(0);
        assertTrue(fila.getErrors().isEmpty(), fila.getErrorMsg());
        assertEquals("A1", fila.getCode());
        //El atributo heredado de la clase llega al modelo
        assertEquals("heredado", fila.getDescrip());
        //El atributo propio no lo pisa la clase
        assertEquals(9, fila.getOrden());
    }

    /**
     * Los errores de programación sí se lanzan (RF9).
     */
    @Test
    public void test15ErroresDeProgramacion() throws Exception {
        System.out.println("15-XmlRowMapper - errores de programación");
        IXmlRowMapper mapper = new XmlRowMapper();
        assertThrows(IllegalArgumentException.class, () -> mapper.toRow(null, EntidadPrueba.class));
        assertThrows(IllegalArgumentException.class, () -> mapper.toRow(nodo("<ROW/>"), null));
        assertThrows(IllegalArgumentException.class, () -> mapper.fill(nodo("<ROW/>"), null));
        assertThrows(IllegalArgumentException.class,
                () -> mapper.toRows((Document) null, "/DATA/ROW", EntidadPrueba.class));
        assertThrows(IllegalArgumentException.class,
                () -> mapper.toRows((IXmlDom<Document, Element>) null, "/DATA/ROW", EntidadPrueba.class));
        //Un xmldom sin configurar no tiene documento
        assertThrows(IllegalArgumentException.class,
                () -> mapper.toRows(new XmlDomW3c(), "/DATA/ROW", EntidadPrueba.class));
    }

    /**
     * Un mapeador configurado se reutiliza para varias filas (caché del
     * índice de atributos).
     */
    @Test
    public void test16MapeadorReutilizable() throws Exception {
        System.out.println("16-XmlRowMapper - mapeador reutilizable");
        IXmlRowMapper mapper = new XmlRowMapper().ignore("idprueba");
        EntidadPrueba primera = mapper.toRow(nodo("<ROW idprueba=\"1\" code=\"A1\"/>"), EntidadPrueba.class);
        EntidadPrueba segunda = mapper.toRow(nodo("<ROW idprueba=\"2\" code=\"A2\"/>"), EntidadPrueba.class);
        EntidadHija tercera = mapper.toRow(nodo("<ROW code=\"A3\" extra=\"x\"/>"), EntidadHija.class);

        assertNull(primera.getIdprueba());
        assertEquals("A1", primera.getCode());
        assertNull(segunda.getIdprueba());
        assertEquals("A2", segunda.getCode());
        assertEquals("A3", tercera.getCode());
        assertEquals("x", tercera.getExtra());
    }

    /**
     * Un texto vacío sobre un atributo primitivo no se asigna como nulo: la
     * fila recibe el error con la causa real (E3-02).
     */
    @Test
    public void test17VacioEnPrimitivo() throws Exception {
        System.out.println("17-XmlRowMapper - vacío sobre atributo primitivo");
        EntidadPrueba row = new EntidadPrueba();
        row.setCantidad(7L);
        row.setVigente(true);

        new XmlRowMapper().fill(nodo("<ROW cantidad=\"\" vigente=\"\" code=\"A1\"/>"), row);

        assertEquals(2, row.getErrors().size());
        assertEquals(50000, row.getErrors().get("cantidad").getErrorNumber());
        assertEquals("cantidad", row.getErrors().get("cantidad").getFieldName());
        assertTrue(row.getErrors().get("cantidad").getMessage().contains("primitivo"),
                row.getErrors().get("cantidad").getMessage());
        assertTrue(row.getErrors().get("vigente").getMessage().contains("primitivo"));
        //El valor anterior se conserva y el resto se asigna igual
        assertEquals(7L, row.getCantidad());
        assertTrue(row.isVigente());
        assertEquals("A1", row.getCode());
        assertFalse(mapeados(row).contains("cantidad"));
    }

    /**
     * Un resolvedor que devuelve un valor del tipo equivocado deja el error en
     * la fila y no interrumpe el resto (M3-05).
     */
    @Test
    public void test18ResolvedorConTipoIncompatible() throws Exception {
        System.out.println("18-XmlRowMapper - resolvedor con tipo incompatible");
        EntidadPrueba row = new XmlRowMapper()
                .resolver("idprueba", (campo, texto, nodo, fila) -> "no soy un Long")
                .toRow(nodo("<ROW idprueba=\"1\" code=\"A1\" orden=\"3\"/>"), EntidadPrueba.class);

        assertEquals(1, row.getErrors().size());
        assertEquals(50000, row.getErrors().get("idprueba").getErrorNumber());
        assertNull(row.getIdprueba());
        assertEquals("A1", row.getCode());
        assertEquals(3, row.getOrden());
    }

    /**
     * toRows con una lista nula devuelve una lista vacía (M3-05).
     */
    @Test
    public void test19ToRowsListaNula() throws Exception {
        System.out.println("19-XmlRowMapper - toRows con lista nula");
        List<EntidadPrueba> filas = new XmlRowMapper().toRows((List<Element>) null, EntidadPrueba.class);
        assertNotNull(filas);
        assertTrue(filas.isEmpty());
    }

    /**
     * emptyAsNull alcanza solo a la cadena vacía exacta: un texto de espacios
     * se conserva tal cual (M3-04e).
     */
    @Test
    public void test20EmptyAsNullSoloCadenaVacia() throws Exception {
        System.out.println("20-XmlRowMapper - emptyAsNull solo con cadena vacía");
        EntidadPrueba row = new XmlRowMapper().emptyAsNull(true)
                .toRow(nodo("<ROW descrip=\"  \" explanation=\"\"/>"), EntidadPrueba.class);
        assertTrue(row.getErrors().isEmpty(), row.getErrorMsg());
        assertEquals("  ", row.getDescrip());
        assertNull(row.getExplanation());
        //Sin emptyAsNull los dos quedan como vienen
        row = new XmlRowMapper().toRow(nodo("<ROW descrip=\"  \" explanation=\"\"/>"), EntidadPrueba.class);
        assertEquals("  ", row.getDescrip());
        assertEquals("", row.getExplanation());
    }

    /**
     * Las declaraciones de espacio de nombres no son datos: ni se asignan ni
     * se denuncian, tampoco en modo estricto (M3-05).
     */
    @Test
    public void test21DeclaracionesXmlns() throws Exception {
        System.out.println("21-XmlRowMapper - declaraciones xmlns");
        Element node = nodo("<ROW xmlns=\"http://oym.com.py/maker\""
                + " xmlns:x=\"http://oym.com.py/x\" code=\"A1\"/>");

        EntidadPrueba row = new XmlRowMapper().strict(true).toRow(node, EntidadPrueba.class);

        assertTrue(row.getErrors().isEmpty(), row.getErrorMsg());
        assertEquals("A1", row.getCode());
        assertEquals(1, mapeados(row).size());
    }

    /**
     * Atributo y elemento hijo homónimos: gana el atributo y el hijo se
     * reporta como no mapeado (M3-04a).
     */
    @Test
    public void test22AtributoEHijoHomonimos() throws Exception {
        System.out.println("22-XmlRowMapper - atributo e hijo homónimos");
        String xml = "<ROW descrip=\"del atributo\"><descrip>del hijo</descrip></ROW>";

        //Por defecto: gana el atributo, sin ruido
        EntidadPrueba row = new XmlRowMapper().toRow(nodo(xml), EntidadPrueba.class);
        assertTrue(row.getErrors().isEmpty(), row.getErrorMsg());
        assertEquals("del atributo", row.getDescrip());

        //Con observador: avisa del hijo descartado
        List<String> avisos = new ArrayList();
        row = new XmlRowMapper().onUnmapped((nodo, nombre) -> avisos.add(nombre))
                .toRow(nodo(xml), EntidadPrueba.class);
        assertEquals("del atributo", row.getDescrip());
        assertEquals(1, avisos.size());
        assertEquals("descrip", avisos.get(0));

        //En modo estricto es error de la fila, y el valor sigue siendo el del atributo
        row = new XmlRowMapper().strict(true).toRow(nodo(xml), EntidadPrueba.class);
        assertEquals(1, row.getErrors().size());
        assertTrue(row.getErrors().get("descrip").getMessage().contains("precedencia"),
                row.getErrors().get("descrip").getMessage());
        assertEquals("del atributo", row.getDescrip());
    }

    /**
     * Un campo anotado {@code @Transient} no es un atributo alcanzable desde
     * el XML (M3-04b).
     */
    @Test
    public void test23CampoTransientDeJpa() throws Exception {
        System.out.println("23-XmlRowMapper - campo @Transient de JPA");
        EntidadConTransient row = new XmlRowMapper().strict(true)
                .toRow(nodo("<ROW code=\"A1\" calculado=\"x\"/>"), EntidadConTransient.class);

        assertEquals("A1", row.getCode());
        assertNull(row.getCalculado());
        assertEquals(1, row.getErrors().size());
        assertNotNull(row.getErrors().get("calculado"));
    }

    /**
     * Un mapeador ya configurado se puede usar desde varios hilos a la vez
     * (lo único que se escribe es la caché de índices).
     */
    @Test
    public void test24LecturaConcurrente() throws Exception {
        System.out.println("24-XmlRowMapper - lectura concurrente");
        final String xml = "<DATA>"
                + "<ROW code=\"A1\" orden=\"1\" fecha=\"2026-10-25\"/>"
                + "<ROW code=\"A2\" orden=\"2\" fecha=\"2026-10-25\"/>"
                + "<ROW code=\"A3\" orden=\"3\" fecha=\"2026-10-25\"/>"
                + "</DATA>";
        final IXmlRowMapper mapper = new XmlRowMapper().ignore("idprueba");
        final List<String> fallas = Collections.synchronizedList(new ArrayList());

        Runnable tarea = () -> {
            try {
                for (int i = 0; i < 50; i++) {
                    //Cada hilo parsea su propio documento: el DOM no es compartible
                    Document dom = DomW3cParser.loadXml(xml);
                    List<EntidadPrueba> filas = mapper.toRows(dom, "/DATA/ROW", EntidadPrueba.class);
                    if (filas.size() != 3) {
                        fallas.add("cantidad de filas: " + filas.size());
                        continue;
                    }
                    if (!"A1".equals(filas.get(0).getCode()) || !"A3".equals(filas.get(2).getCode())) {
                        fallas.add("orden alterado");
                    }
                    for (EntidadPrueba fila : filas) {
                        if (!fila.getErrors().isEmpty()) {
                            fallas.add(fila.getErrorMsg());
                        }
                        if (fila.getFecha() == null) {
                            fallas.add("fecha sin asignar");
                        }
                    }
                }
            } catch (Exception ex) {
                fallas.add(ex.toString());
            }
        };

        Thread hilo1 = new Thread(tarea);
        Thread hilo2 = new Thread(tarea);
        hilo1.start();
        hilo2.start();
        hilo1.join();
        hilo2.join();

        assertTrue(fallas.isEmpty(), fallas.toString());
    }
}
