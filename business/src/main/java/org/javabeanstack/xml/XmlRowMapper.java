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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import jakarta.persistence.Transient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javabeanstack.data.DataInfo;
import org.javabeanstack.data.DataRow;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.util.ValueConverter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Lleva los datos de un nodo XML a un registro del modelo.
 *
 * <p>Por cada atributo del nodo busca el atributo del modelo que se llame
 * igual <b>sin distinguir mayúsculas</b>, convierte el texto al tipo de ese
 * atributo ({@link ValueConverter}) y lo asigna con
 * {@link IDataRow#setValue(String, Object)}. Si
 * {@link #childElementsAsFields(boolean)} está habilitado (lo está por
 * defecto) hace lo mismo con los elementos hijos simples —sin atributos ni
 * hijos propios—, tomando su texto como valor: es la forma natural de llevar
 * textos largos sin meterlos en un atributo.</p>
 *
 * <pre>{@code
 * IXmlRowMapper mapper = new XmlRowMapper()
 *         .ignore("idappobject")                     //en un UPDATE la clave primaria no se pisa
 *         .resolver("idappobjecttype", (f, texto, nodo, fila) -> tipos.idPorNombre(texto));
 *
 * Document dom = DomW3cParser.loadXml(new File("catalogo.xml"));
 * List<AppObject> filas = mapper.toRows(dom, "/DATA/ROW", AppObject.class);
 * for (AppObject fila : filas) {
 *     if (fila.getErrors().isEmpty()) {
 *         dao.save(sessionId, fila);
 *     }
 * }
 * }</pre>
 *
 * <p>Atributos del modelo considerados: los declarados en la clase y en sus
 * superclases <b>hasta {@link DataRow} exclusive</b> (los campos propios de
 * {@code DataRow} —{@code action}, {@code errors}, {@code persistMode}…— no
 * son datos del modelo y no se pueden alcanzar desde el XML), sin los
 * {@code static} ni los {@code transient}. El índice de nombres se arma una
 * vez por clase y queda en caché.</p>
 *
 * <p>Un dato inválido no interrumpe el proceso: la fila recibe el error
 * ({@code setErrors} con número 50000 y el nombre del atributo) y el mapeo
 * sigue con el atributo siguiente. Solo se lanza excepción ante un error de
 * programación: nodo nulo, clase sin constructor sin argumentos, expresión
 * XPath inválida.</p>
 *
 * <p>Qué no hace: no lee archivos (recibe nodos ya parseados por
 * {@code DomW3cParser}, que es quien tiene las protecciones del parser) y no
 * graba nada. Tampoco es un conversor del modelo a XML.</p>
 *
 * <p>Nota sobre la asignación: {@code setValue} prueba primero el método
 * {@code set} del atributo y recién después el campo, de modo que un
 * modificador con lógica propia se ejecuta igual que en el resto de la
 * aplicación.</p>
 *
 * <p><b>Nombres que solo difieren en mayúsculas</b>: el mapeador <b>rechaza
 * siempre</b> la asignación con un error de la fila, aunque uno de los dos
 * atributos coincida exactamente con el nombre del XML. No es exceso de celo:
 * {@code setValue} no asigna el campo que el mapeador eligió, sino el que
 * resuelve {@code DataInfo.setFieldValue}, que busca el método {@code set}
 * <b>sin distinguir mayúsculas</b> y devuelve el primero que encuentra en un
 * orden que la máquina virtual no especifica; con dos modificadores de tipos
 * compatibles el valor terminaría en el atributo equivocado y sin error
 * (plan {@code XMLMAP}, hallazgo {@code E3-01}). Mientras esa resolución no
 * distinga la caja, un par así no se puede mapear: hay que renombrar el
 * atributo del modelo o resolverlo a mano con un
 * {@link IXmlFieldResolver}.</p>
 *
 * <p>Otros dos bordes resueltos: un atributo y un elemento hijo
 * <b>homónimos</b> no compiten —gana el atributo y el hijo se reporta como no
 * mapeado—, y un texto vacío sobre un atributo <b>primitivo</b>
 * ({@code long}, {@code boolean}…) no se asigna como nulo: queda como error de
 * la fila, porque un primitivo no admite el vacío.</p>
 *
 * <p>Los nombres se comparan tal como vienen en el documento
 * ({@code getNodeName()}): un nombre con prefijo de espacio de nombres
 * ({@code ns:code}) no coincide con {@code code}. Las declaraciones
 * {@code xmlns} y {@code xmlns:*} no se consideran datos y se saltean.</p>
 *
 * <p>Puntos de extensión para una subclase: {@link #applyValue} (el paso
 * completo nombre/texto → atributo) y {@link #convert} (la conversión del
 * texto al tipo del atributo). El resto de los métodos internos son privados a
 * propósito, para no comprometer como API lo que es detalle de
 * implementación; por lo mismo {@code applyValue} no recibe el índice de
 * atributos: lo resuelve por su cuenta.</p>
 *
 * <p>Uso entre hilos: <b>configurar antes de publicar</b>. Es decir,
 * terminar la configuración (resolvedores, {@code ignore}, formatos) en el
 * hilo que crea el mapeador y recién entonces compartirlo; a partir de ahí el
 * mapeo es seguro para hilos concurrentes, porque lo único que se escribe es
 * la caché de índices ({@link ConcurrentHashMap}). Los indicadores de
 * configuración no son {@code volatile}: cambiar la configuración de un
 * mapeador que otro hilo ya está usando es una carrera de datos.</p>
 *
 * @author Jorge Enciso
 */
public class XmlRowMapper implements IXmlRowMapper {

    private static final Logger LOGGER = LogManager.getLogger(XmlRowMapper.class);

    /**
     * Número de mensaje de los errores de validación de la aplicación.
     */
    private static final int ERROR_NUMBER = 50000;

    /**
     * Índice de atributos del modelo por clase: nombre en minúsculas a la
     * lista de campos que coinciden con ese nombre (más de uno solo si
     * difieren en mayúsculas).
     */
    private final Map<Class<?>, Map<String, List<Field>>> fieldIndexCache = new ConcurrentHashMap<>();

    /**
     * Resolvedores declarados, por nombre de atributo en minúsculas.
     */
    private final Map<String, IXmlFieldResolver> resolvers = new HashMap<>();

    /**
     * Nombres de atributos que nunca se asignan, en minúsculas.
     */
    private final Set<String> ignored = new LinkedHashSet<>();

    private boolean strict = false;
    private boolean childElementsAsFields = true;
    private boolean emptyAsNull = false;
    private boolean normalizeChildText = false;
    private DateTimeFormatter dateFormatter;
    private DateTimeFormatter dateTimeFormatter;
    private BiConsumer<Element, String> onUnmapped;

    /**
     * Constructor por defecto; el mapeador queda con la configuración
     * estándar: no estricto, elementos hijos simples habilitados con su texto
     * tal cual, vacío de texto como texto vacío y fechas en formato
     * ISO-8601.
     */
    public XmlRowMapper() {
    }

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
    @Override
    public <T extends IDataRow> T toRow(Element node, Class<T> type) throws Exception {
        if (type == null) {
            throw new IllegalArgumentException("No se indicó la clase del registro a crear");
        }
        T row = newInstance(type);
        fill(node, row);
        row.setAction(IDataRow.INSERT);
        return row;
    }

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
    @Override
    public <T extends IDataRow> T fill(Element node, T target) throws Exception {
        if (node == null) {
            throw new IllegalArgumentException("El nodo xml no puede ser nulo");
        }
        if (target == null) {
            throw new IllegalArgumentException("El registro a completar no puede ser nulo");
        }
        Set<String> fromAttributes = new LinkedHashSet<>();
        NamedNodeMap attributes = node.getAttributes();
        if (attributes != null) {
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attribute = attributes.item(i);
                String name = attribute.getNodeName();
                //Las declaraciones de espacio de nombres no son datos
                if (name == null || "xmlns".equals(name) || name.startsWith("xmlns:")) {
                    continue;
                }
                fromAttributes.add(toKey(name));
                applyValue(node, target, name, attribute.getNodeValue());
            }
        }
        if (childElementsAsFields) {
            for (Element child : getChildren(node)) {
                if (!isSimpleElement(child)) {
                    //Un hijo con estructura no es el valor de un atributo
                    reportUnmapped(node, target, child.getNodeName(),
                            "el elemento hijo tiene atributos o hijos propios, no es el valor de un atributo");
                    continue;
                }
                if (fromAttributes.contains(toKey(child.getNodeName()))) {
                    //El mismo nombre ya vino como atributo del nodo: el atributo manda
                    reportUnmapped(node, target, child.getNodeName(),
                            "el nodo ya trae un atributo con ese nombre y el atributo tiene precedencia");
                    continue;
                }
                applyValue(node, target, child.getNodeName(), getElementText(child));
            }
        }
        return target;
    }

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
    @Override
    public <T extends IDataRow> List<T> toRows(List<Element> nodes, Class<T> type) throws Exception {
        List<T> rows = new ArrayList();
        if (nodes == null) {
            return rows;
        }
        for (Element node : nodes) {
            rows.add(toRow(node, type));
        }
        return rows;
    }

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
    @Override
    public <T extends IDataRow> List<T> toRows(Document dom, String nodePath, Class<T> type) throws Exception {
        if (dom == null) {
            throw new IllegalArgumentException("El documento xml no puede ser nulo");
        }
        return toRows(DomW3cParser.getElements(dom, nodePath), type);
    }

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
    @Override
    public <T extends IDataRow> List<T> toRows(IXmlDom<Document, Element> xmlDom, String nodePath, Class<T> type) throws Exception {
        if (xmlDom == null) {
            throw new IllegalArgumentException("El objeto xmldom no puede ser nulo");
        }
        Document dom = xmlDom.getDom();
        if (dom == null) {
            throw new IllegalArgumentException("El objeto xmldom no tiene un documento procesado, ver el método config");
        }
        return toRows(dom, nodePath, type);
    }

    /**
     * Declara el resolvedor del atributo indicado: el texto se convierte con
     * él en vez de con la conversión genérica por tipo de dato. Un resolvedor
     * nulo quita el que estuviera declarado.
     *
     * @param field nombre del atributo del modelo (sin distinguir mayúsculas).
     * @param resolver resolvedor a aplicar.
     * @return este mismo mapeador.
     */
    @Override
    public IXmlRowMapper resolver(String field, IXmlFieldResolver resolver) {
        if (field == null) {
            throw new IllegalArgumentException("No se indicó el atributo del resolvedor");
        }
        if (resolver == null) {
            resolvers.remove(toKey(field));
        } else {
            resolvers.put(toKey(field), resolver);
        }
        return this;
    }

    /**
     * Declara atributos que nunca se asignan aunque vengan en el nodo (claves
     * primarias, columnas protegidas).
     *
     * <p>Es <b>acumulativo y sin reversa</b>: no hay forma de quitar un nombre
     * declarado. Un cargador que necesite dos políticas —por ejemplo tomar del
     * XML el identificador de fábrica en el alta y no pisarlo en la
     * modificación— usa <b>dos mapeadores</b>, no uno que se reconfigura.</p>
     *
     * @param fields nombres de los atributos (sin distinguir mayúsculas).
     * @return este mismo mapeador.
     */
    @Override
    public IXmlRowMapper ignore(String... fields) {
        if (fields != null) {
            for (String field : fields) {
                if (field != null) {
                    ignored.add(toKey(field));
                }
            }
        }
        return this;
    }

    /**
     * Indica qué hacer con un atributo del nodo que no corresponde a ningún
     * atributo del modelo: falso (por defecto) lo ignora, verdadero lo
     * registra como error de la fila.
     *
     * @param strict verdadero para el modo estricto.
     * @return este mismo mapeador.
     */
    @Override
    public IXmlRowMapper strict(boolean strict) {
        this.strict = strict;
        return this;
    }

    /**
     * Habilita o deshabilita el tratamiento de los elementos hijos simples
     * como atributos. Habilitado por defecto.
     *
     * @param enabled verdadero para considerarlos.
     * @return este mismo mapeador.
     */
    @Override
    public IXmlRowMapper childElementsAsFields(boolean enabled) {
        this.childElementsAsFields = enabled;
        return this;
    }

    /**
     * Indica si un texto vacío debe asignarse como nulo también en los
     * atributos de tipo texto. Deshabilitado por defecto.
     *
     * <p>Alcanza <b>solo a la cadena vacía exacta</b> ({@code campo=""}): un
     * atributo de texto nunca se altera, de modo que {@code campo="  "} se
     * asigna con sus espacios. En los atributos que no son de texto el vacío
     * (y el texto de solo espacios) siempre es nulo, salvo que el atributo sea
     * primitivo, en cuyo caso la fila recibe un error.</p>
     *
     * @param enabled verdadero para que el vacío sea nulo.
     * @return este mismo mapeador.
     */
    @Override
    public IXmlRowMapper emptyAsNull(boolean enabled) {
        this.emptyAsNull = enabled;
        return this;
    }

    /**
     * Indica si el texto de un elemento hijo simple se normaliza —recortarlo y
     * colapsar cada tanda de blancos en un espacio— antes de asignarlo.
     * Deshabilitado por defecto.
     *
     * @param enabled verdadero para normalizar los blancos del texto.
     * @return este mismo mapeador.
     */
    @Override
    public IXmlRowMapper normalizeChildText(boolean enabled) {
        this.normalizeChildText = enabled;
        return this;
    }

    /**
     * Declara el formato con el que se interpretan las fechas de este
     * mapeador, en reemplazo del ISO-8601.
     *
     * @param pattern patrón de fecha (ej. {@code dd/MM/yyyy}); nulo vuelve al
     * formato ISO.
     * @return este mismo mapeador.
     */
    @Override
    public IXmlRowMapper dateFormat(String pattern) {
        this.dateFormatter = (pattern == null) ? null : DateTimeFormatter.ofPattern(pattern);
        return this;
    }

    /**
     * Declara el formato con el que se interpretan las fechas y horas de este
     * mapeador, en reemplazo del ISO-8601.
     *
     * @param pattern patrón de fecha y hora (ej. {@code dd/MM/yyyy HH:mm:ss});
     * nulo vuelve al formato ISO.
     * @return este mismo mapeador.
     */
    @Override
    public IXmlRowMapper dateTimeFormat(String pattern) {
        this.dateTimeFormatter = (pattern == null) ? null : DateTimeFormatter.ofPattern(pattern);
        return this;
    }

    /**
     * Declara un observador al que se avisa de cada nombre del nodo que no
     * corresponde a ningún atributo del modelo, sin interrumpir el mapeo.
     *
     * @param listener recibe el nodo y el nombre no mapeado.
     * @return este mismo mapeador.
     */
    @Override
    public IXmlRowMapper onUnmapped(BiConsumer<Element, String> listener) {
        this.onUnmapped = listener;
        return this;
    }

    /**
     * Asigna en la fila el valor de un par nombre/texto leído del nodo.
     *
     * @param node nodo que se está mapeando.
     * @param row fila a completar.
     * @param name nombre leído del XML (atributo o elemento hijo).
     * @param text texto leído del XML.
     */
    protected void applyValue(Element node, IDataRow row, String name, String text) {
        String key = toKey(name);
        if (ignored.contains(key)) {
            return;
        }
        List<Field> candidates = getFieldIndex(row.getClass()).get(key);
        if (candidates == null) {
            reportUnmapped(node, row, name, "no corresponde a ningún atributo de " + row.getClass().getSimpleName());
            return;
        }
        Field field = selectField(candidates, name);
        if (field == null) {
            //Dos o más atributos del modelo que solo difieren en mayúsculas: no se
            //asigna ninguno, porque la capa que asigna (DataInfo.setFieldValue) no
            //los distingue y el valor podría terminar en el equivocado (E3-01).
            row.setErrors(name + ": atributo ambiguo, coincide con " + candidateNames(candidates)
                    + " en " + row.getClass().getSimpleName()
                    + "; renombrá el atributo del modelo o resolvelo con un resolvedor",
                    name, ERROR_NUMBER);
            return;
        }
        String fieldName = field.getName();
        try {
            Object value;
            IXmlFieldResolver resolver = resolvers.get(key);
            if (resolver != null) {
                value = resolver.resolve(fieldName, text, node, row);
            } else {
                value = convert(text, field.getType());
            }
            if (value == null && field.getType().isPrimitive()) {
                //Un primitivo no admite nulo: se avisa con la causa real en vez de
                //dejar que la asignación falle con un mensaje genérico (E3-02).
                row.setErrors(fieldName + ": el atributo es primitivo ("
                        + field.getType().getSimpleName() + ") y no admite un valor vacío",
                        fieldName, ERROR_NUMBER);
                return;
            }
            row.setValue(fieldName, value);
            registerMapped(row, fieldName);
        } catch (Exception ex) {
            LOGGER.debug("No se pudo asignar el atributo " + fieldName + ": " + ex.getMessage());
            row.setErrors(fieldName + ": " + getMessage(ex), fieldName, ERROR_NUMBER);
        }
    }

    /**
     * Devuelve los nombres de los campos candidatos, para el mensaje de
     * ambigüedad.
     *
     * @param candidates campos que coinciden sin distinguir mayúsculas.
     * @return nombres separados por " y ".
     */
    private String candidateNames(List<Field> candidates) {
        StringBuilder nombres = new StringBuilder();
        for (Field field : candidates) {
            if (nombres.length() > 0) {
                nombres.append(" y ");
            }
            nombres.append(field.getName());
        }
        return nombres.toString();
    }

    /**
     * Convierte el texto al tipo del atributo del modelo.
     *
     * @param text texto leído del XML.
     * @param type tipo del atributo.
     * @return valor convertido.
     */
    protected Object convert(String text, Class<?> type) {
        if (String.class.equals(type)) {
            if (text == null) {
                return null;
            }
            return (emptyAsNull && text.isEmpty()) ? null : text;
        }
        return ValueConverter.fromString(text, type, dateFormatter, dateTimeFormatter);
    }

    /**
     * Avisa de un nombre del XML que no se pudo mapear: notifica al observador
     * declarado con {@link #onUnmapped(BiConsumer)} y, en modo estricto, lo
     * registra como error de la fila.
     *
     * @param node nodo que se está mapeando.
     * @param row fila que se está completando.
     * @param name nombre leído del XML.
     * @param reason motivo por el que no se mapeó.
     */
    private void reportUnmapped(Element node, IDataRow row, String name, String reason) {
        if (onUnmapped != null) {
            onUnmapped.accept(node, name);
        }
        if (strict) {
            row.setErrors(name + ": " + reason, name, ERROR_NUMBER);
        }
    }

    /**
     * Elige el campo a asignar entre los que coinciden sin distinguir
     * mayúsculas.
     *
     * <p>Solo hay campo si el candidato es <b>uno</b>. Con dos o más —dos
     * atributos del modelo que solo difieren en la caja— se rechaza la
     * asignación <b>aunque uno coincida exactamente</b> con el nombre leído
     * del XML: elegir bien el campo no alcanza, porque
     * {@code DataInfo.setFieldValue} resuelve el modificador sin distinguir
     * mayúsculas y podría escribir en el otro atributo sin avisar
     * ({@code XMLMAP}, E3-01).</p>
     *
     * @param candidates campos candidatos.
     * @param name nombre leído del XML.
     * @return campo a asignar, nulo si la coincidencia es ambigua.
     */
    private Field selectField(List<Field> candidates, String name) {
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        return null;
    }

    /**
     * Devuelve el índice de atributos del modelo de una clase, armándolo la
     * primera vez.
     *
     * @param type clase del registro.
     * @return índice de nombre en minúsculas a campos.
     */
    private Map<String, List<Field>> getFieldIndex(Class<?> type) {
        return fieldIndexCache.computeIfAbsent(type, clase -> buildFieldIndex(clase));
    }

    /**
     * Arma el índice de atributos del modelo recorriendo la clase y sus
     * superclases hasta {@link DataRow} exclusive, sin los campos
     * {@code static}, {@code transient}, sintéticos ni anotados con
     * {@link jakarta.persistence.Transient} (un campo calculado de una entidad
     * no es un dato que pueda venir del XML).
     *
     * @param type clase del registro.
     * @return índice de nombre en minúsculas a campos.
     */
    private Map<String, List<Field>> buildFieldIndex(Class<?> type) {
        Map<String, List<Field>> index = new HashMap<>();
        Class<?> classSearch = type;
        while (classSearch != null
                && !DataRow.class.equals(classSearch)
                && !Object.class.equals(classSearch)) {
            for (Field field : DataInfo.getDeclaredFields(classSearch)) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)
                        || field.isSynthetic() || field.getAnnotation(Transient.class) != null) {
                    continue;
                }
                List<Field> candidates = index.get(toKey(field.getName()));
                if (candidates == null) {
                    candidates = new ArrayList();
                    index.put(toKey(field.getName()), candidates);
                }
                //Un campo de la superclase redefinido en la clase hija no se agrega dos veces
                boolean exists = false;
                for (Field candidate : candidates) {
                    if (candidate.getName().equals(field.getName())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    candidates.add(field);
                }
            }
            classSearch = classSearch.getSuperclass();
        }
        return index;
    }

    /**
     * Registra el atributo asignado en las propiedades de la fila bajo
     * {@link IXmlRowMapper#MAPPED_FIELDS}.
     *
     * @param row fila que se está completando.
     * @param fieldName nombre del atributo asignado.
     */
    @SuppressWarnings("unchecked")
    private void registerMapped(IDataRow row, String fieldName) {
        Map<String, Object> properties = row.getProperties();
        Set<String> fields = (Set<String>) properties.get(MAPPED_FIELDS);
        if (fields == null) {
            fields = new LinkedHashSet<>();
            properties.put(MAPPED_FIELDS, fields);
        }
        fields.add(fieldName);
    }

    /**
     * Crea una instancia de la clase del registro por su constructor sin
     * argumentos (no hace falta que sea público).
     *
     * @param <T> tipo del registro.
     * @param type clase del registro.
     * @return instancia creada.
     */
    private <T extends IDataRow> T newInstance(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException | SecurityException ex) {
            throw new IllegalArgumentException("No se pudo crear una instancia de " + type.getName()
                    + ", se requiere un constructor sin argumentos", ex);
        }
    }

    /**
     * Devuelve los elementos hijos de un nodo.
     *
     * @param node nodo padre.
     * @return lista de elementos hijos.
     */
    private List<Element> getChildren(Element node) {
        return DomW3cParser.getChildren(node);
    }

    /**
     * Informa si el elemento es un hijo simple, es decir si no tiene
     * atributos ni elementos hijos propios y por lo tanto su texto se puede
     * tratar como el valor de un atributo.
     *
     * @param element elemento a evaluar.
     * @return verdadero si es un hijo simple.
     */
    private boolean isSimpleElement(Element element) {
        NamedNodeMap attributes = element.getAttributes();
        if (attributes != null && attributes.getLength() > 0) {
            return false;
        }
        return getChildren(element).isEmpty();
    }

    /**
     * Devuelve el texto de un elemento hijo, <b>tal cual</b>: los saltos de
     * línea, los tabuladores y la sangría son parte del dato.
     *
     * <p>Hasta {@code CATXML·E4-01} un texto escrito en varias líneas se
     * devolvía en una sola —recortado y con los blancos colapsados—, por
     * herencia de la ayuda de los parámetros de sistema, donde el salto de
     * línea es comodidad del archivo. Pero el mapeador lleva <b>datos</b>: con
     * esa regla, los 352 {@code authdef} del catálogo de Maker —un XML de
     * autorizaciones con CRLF y tabuladores— se grababan colapsados, de modo
     * que la primera carga los reescribía en toda instalación sin que nadie
     * los hubiera cambiado y la verificación contra la base nunca cerraba en
     * cero. Quien quiera la regla anterior la pide con
     * {@link #normalizeChildText(boolean)}.</p>
     *
     * @param element elemento hijo.
     * @return texto del elemento.
     */
    private String getElementText(Element element) {
        String text = element.getTextContent();
        if (text == null) {
            return "";
        }
        if (normalizeChildText && (text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0)) {
            return text.replaceAll("\\s+", " ").trim();
        }
        return text;
    }

    /**
     * Devuelve el mensaje de una excepción, o el nombre de su clase si no
     * tiene mensaje.
     *
     * @param ex excepción.
     * @return mensaje para el error de la fila.
     */
    private String getMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isEmpty()) {
            return ex.getClass().getSimpleName();
        }
        return message;
    }

    /**
     * Devuelve la forma canónica (minúsculas) con la que se comparan los
     * nombres.
     *
     * @param name nombre de un atributo.
     * @return nombre en minúsculas.
     */
    private String toKey(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
