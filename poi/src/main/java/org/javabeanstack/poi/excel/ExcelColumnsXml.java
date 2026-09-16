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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.javabeanstack.xml.DomW3cParser;
import org.javabeanstack.xml.IXmlDom;
import org.javabeanstack.xml.XmlDomW3c;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * Lee una especificación de columnas de importación declarada en XML y arma
 * el {@link ExcelColumns} equivalente, en el orden en que las columnas están
 * escritas. Es la alternativa declarativa al método {@code buildColumns()}
 * de los procesadores: lo que se declara es exactamente lo mismo (cabecera,
 * atributo destino, obligatoriedad, valor por defecto y sobreescritura); la
 * transformación ({@code converter}) y el valor por defecto calculado
 * ({@code Supplier}) siguen siendo código Java y se agregan encima con
 * {@link ExcelColumns#edit(String)}.
 * <p>
 * <b>Sintaxis.</b> Un bloque {@code <COLUMNS>} con <b>un tag por columna</b>;
 * el nombre del tag es solo un identificador único (por convención en
 * mayúsculas) y los atributos van en minúsculas, como en las grillas:
 * <pre>{@code
 * <COLUMNS>
 *     <CTACTE required="VALUE"/>
 *     <CODIGO required="VALUE"/>
 *     <NOMBRE required="COLUMN"/>
 *     <RUC/>
 *     <INACTIVO default="false"/>
 *     <BLOQUEARATRASO header="bloquearAtraso" field="bloquearAtraso" default="false"/>
 *     <ESPROVEEDOR default="0" overwrite="false"/>
 *     <MONEDA default="GS." defaultwhenblank="true"/>
 * </COLUMNS>
 * }</pre>
 * <table>
 * <caption>Atributos de cada tag de columna</caption>
 * <tr><th>Atributo</th><th>Valores</th><th>Si se omite</th><th>Equivale a</th></tr>
 * <tr><td>{@code header}</td><td>texto exacto de la cabecera de la planilla
 * (se normaliza con {@code trim()})</td><td>nombre del tag en
 * minúsculas</td><td>primer argumento de {@code add}</td></tr>
 * <tr><td>{@code field}</td><td>atributo de la vista destino</td><td>igual a
 * {@code header}</td><td>segundo argumento de {@code add}</td></tr>
 * <tr><td>{@code required}</td><td>{@code OPTIONAL}, {@code COLUMN},
 * {@code VALUE} (sin distinguir mayúsculas); sinónimos {@code true} =
 * {@code COLUMN} y {@code false} = {@code OPTIONAL}</td><td>{@code OPTIONAL}</td>
 * <td>{@code required()} / {@code require(...)}</td></tr>
 * <tr><td>{@code default}</td><td>cadena; se convierte al tipo del atributo al
 * aplicarse, igual que el valor de una celda. {@code default=""} es un valor
 * por defecto vacío, distinto de no declararlo</td><td>sin valor por
 * defecto</td><td>{@code defaultValue(...)} fijo</td></tr>
 * <tr><td>{@code defaultwhenblank}</td><td>{@code true} / {@code false}; exige
 * {@code default}</td><td>{@code false}</td><td>{@code defaultWhenBlank()}</td></tr>
 * <tr><td>{@code overwrite}</td><td>{@code true} / {@code false}</td>
 * <td>{@code true}</td><td>{@code noOverwrite()}</td></tr>
 * </table>
 * <p>
 * <b>El parser es estricto.</b> Un error de tipeo en el XML no puede dejar una
 * columna opcional o sin valor por defecto en silencio, así que cualquier
 * atributo desconocido, valor inválido, {@code defaultwhenblank} sin
 * {@code default}, cabecera vacía o repetida, tag de columna con elementos
 * hijos, bloque sin ninguna columna o nodo sin un único hijo {@code COLUMNS}
 * produce una {@link IllegalArgumentException} que nombra el bloque y la
 * columna («Bloque «PAGE/EXCELIMPORT/DEFAULT» (ctactesub.xml), columna
 * «CODIGO»: atributo desconocido «requiered»»). Se toleran los atributos
 * {@code clase} y {@code src} y los que empiezan con {@code __} (los deja la
 * herencia de {@code XmlDomW3c}); los comentarios XML se ignoran. Que un bloque
 * vacío sea un error importa especialmente: el constructor base de
 * {@link ExcelRowProcessor} trata un {@code ExcelColumns} vacío como mapeo
 * identidad de todas las cabeceras de la planilla.
 * <p>
 * <b>Fuentes.</b> Un nodo DOM ya resuelto ({@link #fromNode(Element)}), el
 * recurso {@code IXmlDom} que el controller ya tiene cargado más la ruta del
 * bloque ({@link #fromXmlDom(IXmlDom, String)}), un fragmento como texto
 * ({@link #fromXml(String)}, sin herencia {@code clase/src}) o un archivo más
 * la ruta del bloque ({@link #fromFile(String, String)}, con herencia
 * {@code clase/src} resuelta relativa a la carpeta del archivo). Con herencia,
 * los tags de la clase base quedan <b>antes</b> de los propios y sus atributos
 * se heredan solo cuando faltan.
 *
 * @author Jorge Enciso
 */
public final class ExcelColumnsXml {

    /**
     * Nombre del tag que agrupa las columnas.
     */
    public static final String COLUMNS_TAG = "COLUMNS";

    /**
     * Atributos admitidos en cada tag de columna.
     */
    static final String ATTR_HEADER = "header";
    static final String ATTR_FIELD = "field";
    static final String ATTR_REQUIRED = "required";
    static final String ATTR_DEFAULT = "default";
    static final String ATTR_DEFAULT_WHEN_BLANK = "defaultwhenblank";
    static final String ATTR_OVERWRITE = "overwrite";

    private static final Set<String> KNOWN_ATTRIBUTES = Set.of(
            ATTR_HEADER, ATTR_FIELD, ATTR_REQUIRED, ATTR_DEFAULT,
            ATTR_DEFAULT_WHEN_BLANK, ATTR_OVERWRITE);

    /**
     * Atributos que deja la herencia de {@code XmlDomW3c} y que no describen
     * la columna: se ignoran en vez de rechazarse.
     */
    private static final Set<String> TOLERATED_ATTRIBUTES = Set.of("clase", "src");

    private ExcelColumnsXml() {
    }

    /**
     * Arma las columnas a partir de un nodo DOM ya resuelto. El nodo es el
     * {@code <COLUMNS>} mismo, o un nodo con <b>exactamente un</b> hijo
     * llamado {@code COLUMNS}; en cualquier otro caso lanza, sin adivinar
     * (un {@code <COLUMS>} mal escrito se leería como una columna válida y
     * silenciosa). Los mensajes nombran el bloque con el nombre del nodo.
     *
     * @param node el {@code <COLUMNS>} o su padre directo; no puede ser
     * {@code null}.
     * @return las columnas declaradas, en el orden del XML.
     * @throws IllegalArgumentException si el nodo es nulo, si no es ni
     * contiene un único {@code COLUMNS}, si no declara ninguna columna o si
     * alguna columna está mal declarada (ver la descripción de la clase).
     */
    public static ExcelColumns fromNode(Element node) {
        return fromNode(node, null);
    }

    /**
     * Arma las columnas a partir de un nodo DOM ya resuelto, nombrando el
     * origen en los mensajes de error. Mismas reglas que
     * {@link #fromNode(Element)}.
     *
     * @param node el {@code <COLUMNS>} o su padre directo; no puede ser
     * {@code null}.
     * @param sourceLabel texto con el que se identifica el bloque en los
     * mensajes («Bloque «{@code sourceLabel}», columna «X»: …»); si es
     * {@code null} o vacío se usa el nombre del nodo.
     * @return las columnas declaradas, en el orden del XML.
     * @throws IllegalArgumentException en los mismos casos que
     * {@link #fromNode(Element)}.
     */
    public static ExcelColumns fromNode(Element node, String sourceLabel) {
        if (node == null) {
            throw new IllegalArgumentException(
                    block(isBlank(sourceLabel) ? "?" : sourceLabel) + ": el nodo XML es nulo");
        }
        String label = isBlank(sourceLabel) ? node.getNodeName() : sourceLabel.trim();
        return parse(node, block(label));
    }

    /**
     * Arma las columnas a partir de un recurso XML ya cargado (típicamente
     * el {@code xml/<formulario>.xml} que el controller resolvió con la
     * herencia {@code clase/src} aplicada) y la ruta del bloque dentro de él.
     *
     * @param xmlDom recurso cargado; su documento no puede ser {@code null}.
     * @param nodePath ruta del nodo que es o contiene el {@code COLUMNS}, en
     * formato XPath relativo o absoluto (p. ej.
     * {@code "PAGE/EXCELIMPORT/DEFAULT"}); es lo que nombran los mensajes.
     * @return las columnas declaradas, en el orden del XML.
     * @throws IllegalArgumentException si el recurso no está cargado, si la
     * ruta está vacía, no es válida o no existe en el documento, o si el
     * bloque está mal declarado (ver la descripción de la clase).
     */
    public static ExcelColumns fromXmlDom(IXmlDom<Document, Element> xmlDom, String nodePath) {
        String prefix = block(nodePath);
        if (xmlDom == null || xmlDom.getDom() == null) {
            throw new IllegalArgumentException(prefix + ": el recurso XML es nulo o no está cargado");
        }
        Element node = selectNode(xmlDom.getDom(), nodePath, prefix);
        return parse(node, prefix);
    }

    /**
     * Arma las columnas a partir de un fragmento XML como texto cuyo elemento
     * raíz es el {@code <COLUMNS>} o su padre directo. El texto se interpreta
     * <b>sin</b> herencia {@code clase/src}: para heredar hay que leer desde
     * un archivo ({@link #fromFile(String, String)}) o desde un recurso ya
     * resuelto ({@link #fromXmlDom(IXmlDom, String)}).
     *
     * @param xmlText texto XML; no puede ser nulo ni vacío.
     * @return las columnas declaradas, en el orden del texto.
     * @throws IllegalArgumentException si el texto es nulo, vacío o no es un
     * XML válido, o si el bloque está mal declarado (ver la descripción de la
     * clase).
     */
    public static ExcelColumns fromXml(String xmlText) {
        return fromXml(xmlText, null);
    }

    /**
     * Arma las columnas a partir de un texto XML, ubicando el bloque por su
     * ruta dentro del texto. Sin herencia {@code clase/src}, como
     * {@link #fromXml(String)}.
     *
     * @param xmlText texto XML; no puede ser nulo ni vacío.
     * @param nodePath ruta del nodo que es o contiene el {@code COLUMNS}, en
     * formato XPath relativo o absoluto; si es {@code null} o vacío se toma
     * el elemento raíz del texto.
     * @return las columnas declaradas, en el orden del texto.
     * @throws IllegalArgumentException si el texto es nulo, vacío o no es un
     * XML válido, si la ruta no existe, o si el bloque está mal declarado
     * (ver la descripción de la clase).
     */
    public static ExcelColumns fromXml(String xmlText, String nodePath) {
        String prefix = isBlank(nodePath) ? block("fragmento XML") : block(nodePath);
        if (isBlank(xmlText)) {
            throw new IllegalArgumentException(prefix + ": el texto XML es nulo o vacío");
        }
        XmlDomW3c xmlDom = new XmlDomW3c();
        boolean loaded = xmlDom.config("", xmlText, "", true);
        if (!loaded || xmlDom.getDom() == null) {
            throw new IllegalArgumentException(prefix + ": el texto no es un XML válido"
                    + causeText(xmlDom.getException()), xmlDom.getException());
        }
        if (isBlank(nodePath)) {
            return parse(xmlDom.getDom().getDocumentElement(), prefix);
        }
        return parse(selectNode(xmlDom.getDom(), nodePath, prefix), prefix);
    }

    /**
     * Arma las columnas a partir de un archivo XML y la ruta del bloque
     * dentro de él. Se aplica la herencia {@code clase/src} del subárbol
     * pedido, con las referencias {@code src="file://otro.xml"} sin carpeta
     * resueltas <b>relativas a la carpeta del archivo</b>; los tags heredados
     * de la clase base quedan antes de los propios.
     *
     * @param path ruta del archivo XML (absoluta o relativa al directorio de
     * trabajo).
     * @param nodePath ruta del nodo que es o contiene el {@code COLUMNS}, en
     * formato XPath relativo o absoluto (p. ej.
     * {@code "PAGE/EXCELIMPORT/DEFAULT"}).
     * @return las columnas declaradas, en el orden del XML (heredadas
     * primero).
     * @throws IllegalArgumentException si la ruta o el nodo están vacíos, si
     * el archivo no existe, no se puede interpretar o no contiene el nodo, o
     * si el bloque está mal declarado (ver la descripción de la clase).
     */
    public static ExcelColumns fromFile(String path, String nodePath) {
        if (isBlank(path)) {
            throw new IllegalArgumentException(block(nodePath) + ": la ruta del archivo es nula o vacía");
        }
        File file = new File(path.trim());
        String prefix = block(nodePath) + " (" + file.getName() + ")";
        if (isBlank(nodePath)) {
            throw new IllegalArgumentException(prefix + ": la ruta del nodo es nula o vacía");
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException(prefix + ": el archivo «" + file.getPath() + "» no existe");
        }
        Map<String, String> params = new HashMap<>();
        String folder = file.getAbsoluteFile().getParent();
        params.put("path", (folder == null) ? "" : folder + File.separator);
        params.put("encoding", "UTF-8");
        XmlDomW3c xmlDom = new XmlDomW3c();
        boolean loaded = xmlDom.config(file, nodePath.trim(), false, params);
        if (!loaded || xmlDom.getDom() == null) {
            throw new IllegalArgumentException(prefix
                    + ": no se pudo leer el bloque del archivo «" + file.getPath()
                    + "» (el archivo no es un XML válido o no contiene ese nodo)"
                    + causeText(xmlDom.getException()), xmlDom.getException());
        }
        // Con nodePath, XmlDomW3c devuelve un documento cuyo raíz es el nodo pedido
        return parse(xmlDom.getDom().getDocumentElement(), prefix);
    }

    // ----------------------------------------------------------------------
    // Implementación
    // ----------------------------------------------------------------------

    /**
     * Ubica un nodo por su ruta dentro de un documento.
     */
    private static Element selectNode(Document document, String nodePath, String prefix) {
        if (isBlank(nodePath)) {
            throw new IllegalArgumentException(prefix + ": la ruta del nodo es nula o vacía");
        }
        Element node;
        try {
            node = DomW3cParser.getElement(document, nodePath.trim());
        } catch (Exception ex) {
            throw new IllegalArgumentException(prefix + ": la ruta del nodo no es válida"
                    + causeText(ex), ex);
        }
        if (node == null) {
            throw new IllegalArgumentException(prefix + ": el nodo no existe en el XML");
        }
        return node;
    }

    /**
     * Resuelve el {@code <COLUMNS>} a partir del nodo recibido y arma la
     * colección con sus hijos.
     */
    private static ExcelColumns parse(Element node, String prefix) {
        Element columnsNode = resolveColumnsNode(node, prefix);
        ExcelColumns columns = new ExcelColumns();
        for (Element column : DomW3cParser.getChildren(columnsNode)) {
            addColumn(columns, column, prefix);
        }
        if (columns.isEmpty()) {
            throw new IllegalArgumentException(prefix + ": no declara ninguna columna");
        }
        return columns;
    }

    /**
     * El nodo es el {@code COLUMNS}, o tiene exactamente un hijo
     * {@code COLUMNS}; si no, lanza.
     */
    private static Element resolveColumnsNode(Element node, String prefix) {
        if (COLUMNS_TAG.equals(node.getNodeName())) {
            return node;
        }
        List<Element> found = new ArrayList<>();
        List<String> childNames = new ArrayList<>();
        for (Element child : DomW3cParser.getChildren(node)) {
            childNames.add(child.getNodeName());
            if (COLUMNS_TAG.equals(child.getNodeName())) {
                found.add(child);
            }
        }
        if (found.size() == 1) {
            return found.get(0);
        }
        if (found.isEmpty()) {
            throw new IllegalArgumentException(prefix + ": se esperaba un hijo «" + COLUMNS_TAG
                    + "» dentro de «" + node.getNodeName() + "» (hijos encontrados: " + childNames + ")");
        }
        throw new IllegalArgumentException(prefix + ": «" + node.getNodeName() + "» tiene "
                + found.size() + " hijos «" + COLUMNS_TAG + "» y se esperaba uno solo");
    }

    /**
     * Lee un tag de columna, valida sus atributos y la declara en la
     * colección.
     */
    private static void addColumn(ExcelColumns columns, Element column, String prefix) {
        String tag = column.getNodeName();
        String where = prefix + ", columna «" + tag + "»: ";

        NamedNodeMap attributes = column.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            String name = ((Attr) attributes.item(i)).getName();
            if (name.startsWith("__") || TOLERATED_ATTRIBUTES.contains(name)) {
                continue;
            }
            if (!KNOWN_ATTRIBUTES.contains(name)) {
                throw new IllegalArgumentException(where + "atributo desconocido «" + name
                        + "» (se admiten " + KNOWN_ATTRIBUTES.stream().sorted().toList() + ")");
            }
        }
        List<Element> children = DomW3cParser.getChildren(column);
        if (!children.isEmpty()) {
            throw new IllegalArgumentException(where + "un tag de columna no admite elementos hijos"
                    + " (se encontró «" + children.get(0).getNodeName() + "»)");
        }

        String header = column.hasAttribute(ATTR_HEADER)
                ? column.getAttribute(ATTR_HEADER)
                : tag.toLowerCase(Locale.ROOT);
        if (isBlank(header)) {
            throw new IllegalArgumentException(where + "la cabecera («" + ATTR_HEADER + "») no puede estar vacía");
        }
        header = header.trim();
        String field = column.hasAttribute(ATTR_FIELD) ? column.getAttribute(ATTR_FIELD) : header;

        ColumnRequirement requirement = column.hasAttribute(ATTR_REQUIRED)
                ? parseRequirement(column.getAttribute(ATTR_REQUIRED), where)
                : ColumnRequirement.OPTIONAL;
        boolean hasDefault = column.hasAttribute(ATTR_DEFAULT);
        boolean defaultWhenBlank = false;
        if (column.hasAttribute(ATTR_DEFAULT_WHEN_BLANK)) {
            if (!hasDefault) {
                throw new IllegalArgumentException(where + "«" + ATTR_DEFAULT_WHEN_BLANK
                        + "» exige declarar «" + ATTR_DEFAULT + "»");
            }
            defaultWhenBlank = parseBoolean(column.getAttribute(ATTR_DEFAULT_WHEN_BLANK),
                    ATTR_DEFAULT_WHEN_BLANK, where);
        }
        boolean overwrite = column.hasAttribute(ATTR_OVERWRITE)
                ? parseBoolean(column.getAttribute(ATTR_OVERWRITE), ATTR_OVERWRITE, where)
                : true;

        ExcelColumnSpec.Builder builder;
        try {
            builder = columns.add(header, field);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(where + ex.getMessage(), ex);
        }
        builder.require(requirement);
        if (hasDefault) {
            builder.defaultValue(column.getAttribute(ATTR_DEFAULT));
        }
        builder.defaultWhenBlank(defaultWhenBlank);
        builder.overwrite(overwrite);
    }

    private static ColumnRequirement parseRequirement(String value, String where) {
        String normalized = (value == null) ? "" : value.trim().toUpperCase(Locale.ROOT);
        switch (normalized) {
            case "OPTIONAL", "FALSE" -> {
                return ColumnRequirement.OPTIONAL;
            }
            case "COLUMN", "TRUE" -> {
                return ColumnRequirement.COLUMN;
            }
            case "VALUE" -> {
                return ColumnRequirement.VALUE;
            }
            default ->
                throw new IllegalArgumentException(where + "valor inválido «" + value + "» para «"
                        + ATTR_REQUIRED + "» (se admite OPTIONAL, COLUMN, VALUE, true o false)");
        }
    }

    private static boolean parseBoolean(String value, String attribute, String where) {
        String normalized = (value == null) ? "" : value.trim();
        if ("true".equalsIgnoreCase(normalized)) {
            return true;
        }
        if ("false".equalsIgnoreCase(normalized)) {
            return false;
        }
        throw new IllegalArgumentException(where + "valor inválido «" + value + "» para «"
                + attribute + "» (se admite true o false)");
    }

    private static String block(String label) {
        return "Bloque «" + (isBlank(label) ? "?" : label.trim()) + "»";
    }

    private static String causeText(Exception ex) {
        return (ex == null || isBlank(ex.getMessage())) ? "" : ": " + ex.getMessage();
    }

    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}
