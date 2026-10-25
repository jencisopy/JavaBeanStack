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

import java.util.List;
import java.util.function.BiConsumer;
import org.javabeanstack.data.IDataRow;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Lleva los datos de un nodo XML a un registro del modelo
 * ({@link org.javabeanstack.data.IDataRow}).
 *
 * <p>Cada atributo del nodo se asigna al atributo del modelo que tenga el
 * mismo nombre <b>sin distinguir mayúsculas</b>, convirtiendo el texto al tipo
 * del atributo. Opcionalmente (ver {@link #childElementsAsFields(boolean)}) un
 * elemento hijo simple —sin atributos ni hijos propios— se trata como un
 * atributo cuyo valor es su texto, que es la forma natural de llevar textos
 * largos. Ese texto se toma <b>tal cual</b>: los saltos de línea, los
 * tabuladores y la sangría son parte del dato (ver
 * {@link #normalizeChildText(boolean)}).</p>
 *
 * <pre>{@code
 * IXmlRowMapper mapper = new XmlRowMapper()
 *         .ignore("idappobject")                     //en un UPDATE la clave primaria no se pisa
 *         .resolver("idappobjecttype", (f, texto, nodo, fila) -> tipos.idPorNombre(texto));
 *
 * List<AppObject> filas = mapper.toRows(dom, "/DATA/ROW", AppObject.class);
 *
 * AppObject existente = ...;         //leída de la base por su clave natural
 * mapper.fill(nodo, existente);      //solo pisa lo que el nodo trae
 * if (!existente.getErrors().isEmpty()) {
 *     //la fila se reporta y no se graba
 * }
 * }</pre>
 *
 * <p>Qué <b>no</b> hace: no lee archivos (recibe nodos ya parseados; la
 * lectura sigue pasando por el parser del framework) y no graba nada (devuelve
 * filas; quién las persiste y en qué transacción es asunto del que llama).</p>
 *
 * <p>Un dato que no se puede convertir no interrumpe el proceso: queda como
 * error de la fila ({@code setErrors} con número 50000 y el nombre del
 * atributo) y el mapeo sigue con el atributo siguiente. Las excepciones se
 * reservan para los errores de programación (nodo nulo, clase sin constructor
 * sin argumentos).</p>
 *
 * <p>Reglas que resuelven los casos dudosos: dos atributos del modelo que solo
 * difieren en mayúsculas hacen que el nombre sea <b>ambiguo</b> y ninguno se
 * asigna (error de la fila); entre un atributo y un elemento hijo
 * <b>homónimos</b> gana el atributo; un texto vacío sobre un atributo
 * <b>primitivo</b> es error de la fila, no un nulo.</p>
 *
 * <p>Los métodos de configuración devuelven el mismo mapeador, de forma que se
 * encadenen; un mapeador ya configurado se puede reutilizar para muchas
 * filas.</p>
 *
 * @author Jorge Enciso
 */
public interface IXmlRowMapper {

    /**
     * Clave de {@link IDataRow#getProperties()} bajo la que queda el conjunto
     * ({@code Set<String>}) de atributos del modelo que el mapeador asignó con
     * datos del nodo. Permite al consumidor saber qué trajo el XML y qué no
     * (por ejemplo para no pisar en un UPDATE lo que el archivo no declara).
     */
    String MAPPED_FIELDS = "XML_MAPPED_FIELDS";

    /**
     * Crea un registro del tipo solicitado y le asigna los datos del nodo.
     *
     * @param <T> tipo del registro.
     * @param node nodo XML origen de los datos.
     * @param type clase del registro a crear (debe tener constructor sin
     * argumentos).
     * @return el registro creado, con acción {@link IDataRow#INSERT} y, si
     * hubo datos inválidos, con los errores correspondientes.
     * @throws Exception si el nodo es nulo o la clase no se puede instanciar.
     */
    <T extends IDataRow> T toRow(Element node, Class<T> type) throws Exception;

    /**
     * Asigna los datos del nodo sobre un registro ya existente. Solo toca los
     * atributos presentes en el nodo; los demás quedan como estaban.
     *
     * @param <T> tipo del registro.
     * @param node nodo XML origen de los datos.
     * @param target registro a completar.
     * @return el mismo registro recibido.
     * @throws Exception si el nodo o el registro son nulos.
     */
    <T extends IDataRow> T fill(Element node, T target) throws Exception;

    /**
     * Convierte una lista de nodos en una lista de registros.
     *
     * @param <T> tipo de los registros.
     * @param nodes nodos XML origen de los datos.
     * @param type clase de los registros a crear.
     * @return lista de registros en el orden de los nodos recibidos.
     * @throws Exception si algún nodo es nulo o la clase no se puede
     * instanciar.
     */
    <T extends IDataRow> List<T> toRows(List<Element> nodes, Class<T> type) throws Exception;

    /**
     * Convierte en registros los nodos de un documento que cumplan con la
     * expresión XPath dada.
     *
     * @param <T> tipo de los registros.
     * @param dom documento XML.
     * @param nodePath expresión XPath de los nodos fila (ej. {@code /DATA/ROW}).
     * @param type clase de los registros a crear.
     * @return lista de registros en el orden del documento.
     * @throws Exception si el documento es nulo, la expresión es inválida o la
     * clase no se puede instanciar.
     */
    <T extends IDataRow> List<T> toRows(Document dom, String nodePath, Class<T> type) throws Exception;

    /**
     * Convierte en registros los nodos de un objeto XMLDOM ya configurado. Es
     * la variante que aprovecha la herencia de textos XML
     * ({@code clase}/{@code src}) sin que el mapeador la implemente.
     *
     * @param <T> tipo de los registros.
     * @param xmlDom objeto XMLDOM ya procesado.
     * @param nodePath expresión XPath de los nodos fila.
     * @param type clase de los registros a crear.
     * @return lista de registros en el orden del documento.
     * @throws Exception si el objeto XMLDOM es nulo o no tiene documento.
     */
    <T extends IDataRow> List<T> toRows(IXmlDom<Document, Element> xmlDom, String nodePath, Class<T> type) throws Exception;

    /**
     * Declara el resolvedor del atributo indicado: el texto se convierte con
     * él en vez de con la conversión genérica por tipo de dato.
     *
     * @param field nombre del atributo del modelo (sin distinguir mayúsculas).
     * @param resolver resolvedor a aplicar.
     * @return este mismo mapeador.
     */
    IXmlRowMapper resolver(String field, IXmlFieldResolver resolver);

    /**
     * Declara atributos que nunca se asignan aunque vengan en el nodo (claves
     * primarias, columnas protegidas).
     *
     * <p>Es <b>acumulativo y sin reversa</b>: lo declarado no se puede quitar.
     * Un cargador con dos políticas (tomar del XML el identificador en el alta
     * y no pisarlo en la modificación) usa <b>dos mapeadores</b>.</p>
     *
     * @param fields nombres de los atributos (sin distinguir mayúsculas).
     * @return este mismo mapeador.
     */
    IXmlRowMapper ignore(String... fields);

    /**
     * Indica qué hacer con un atributo del nodo que no corresponde a ningún
     * atributo del modelo: falso (por defecto) lo ignora, verdadero lo
     * registra como error de la fila.
     *
     * @param strict verdadero para el modo estricto.
     * @return este mismo mapeador.
     */
    IXmlRowMapper strict(boolean strict);

    /**
     * Habilita o deshabilita el tratamiento de los elementos hijos simples
     * como atributos. Habilitado por defecto.
     *
     * @param enabled verdadero para considerarlos.
     * @return este mismo mapeador.
     */
    IXmlRowMapper childElementsAsFields(boolean enabled);

    /**
     * Indica si el texto de un elemento hijo simple se normaliza —recortarlo y
     * colapsar cada tanda de blancos en un espacio— antes de asignarlo.
     * <b>Deshabilitado por defecto</b>: el texto se asigna tal cual.
     *
     * <p>El valor por omisión cambió a falso porque normalizar <b>altera
     * datos</b>: un texto largo con formato —un XML de autorizaciones, una
     * plantilla, un cuerpo de correo— pierde sus saltos de línea y su sangría,
     * y la primera carga lo reescribe en toda instalación sin que nadie lo
     * haya cambiado (hallazgo {@code CATXML·E4-01}). Solo quien lleva un texto
     * de una sola línea escrito en varias por comodidad del archivo —una ayuda,
     * una descripción— lo habilita.</p>
     *
     * @param enabled verdadero para normalizar los blancos del texto.
     * @return este mismo mapeador.
     */
    IXmlRowMapper normalizeChildText(boolean enabled);

    /**
     * Indica si un texto vacío debe asignarse como nulo también en los
     * atributos de tipo texto. Deshabilitado por defecto.
     *
     * <p>Alcanza <b>solo a la cadena vacía exacta</b> ({@code campo=""}): un
     * atributo de texto nunca se altera, de modo que {@code campo="  "} se
     * asigna con sus espacios. En los demás tipos el vacío siempre es nulo
     * (salvo en un atributo primitivo, que da error de la fila).</p>
     *
     * @param enabled verdadero para que el vacío sea nulo.
     * @return este mismo mapeador.
     */
    IXmlRowMapper emptyAsNull(boolean enabled);

    /**
     * Declara el formato con el que se interpretan las fechas de este
     * mapeador, en reemplazo del ISO-8601.
     *
     * @param pattern patrón de fecha (ej. {@code dd/MM/yyyy}).
     * @return este mismo mapeador.
     */
    IXmlRowMapper dateFormat(String pattern);

    /**
     * Declara el formato con el que se interpretan las fechas y horas de este
     * mapeador, en reemplazo del ISO-8601.
     *
     * @param pattern patrón de fecha y hora (ej. {@code dd/MM/yyyy HH:mm:ss}).
     * @return este mismo mapeador.
     */
    IXmlRowMapper dateTimeFormat(String pattern);

    /**
     * Declara un observador al que se avisa de cada atributo del nodo que no
     * corresponde a ningún atributo del modelo, sin interrumpir el mapeo.
     *
     * @param listener recibe el nodo y el nombre del atributo no mapeado.
     * @return este mismo mapeador.
     */
    IXmlRowMapper onUnmapped(BiConsumer<Element, String> listener);
}
