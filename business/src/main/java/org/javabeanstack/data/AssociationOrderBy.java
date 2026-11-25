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
package org.javabeanstack.data;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumns;
import jakarta.persistence.Transient;

/**
 * Reescribe los términos de un {@code ORDER BY} de JPQL que navegan una
 * asociación que puede ser nula, para que <b>ordenar no descarte filas</b>.
 *
 * <p><b>El problema.</b> Un orden como {@code customer.name} navega la
 * asociación {@code customer} de forma implícita, y Hibernate (6 y 7)
 * resuelve toda navegación implícita con un {@code join} <b>interno</b>: las
 * filas cuya asociación es nula desaparecen del resultado. El conteo de la
 * misma sentencia se calcula sin el {@code ORDER BY}, así que no se entera, y
 * una grilla paginada anuncia más filas de las que puede mostrar.</p>
 *
 * <p><b>Por qué no un {@code left join}.</b> La salida obvia, agregar
 * {@code left join o.customer j} y ordenar por {@code j.name}, no sirve para
 * las sentencias del framework: sus filtros usan atributos sin calificar
 * ({@code idcompany = 6}) y, con un segundo elemento en el {@code from},
 * Hibernate rechaza todo atributo sin calificar que exista también en la
 * entidad unida («Ambiguous unqualified attribute reference»). Por eso cada
 * término se reemplaza por una <b>subconsulta escalar correlacionada por la
 * clave foránea del primer salto</b>, que no agrega nada al {@code from} de la
 * sentencia:</p>
 * <pre>
 * customer.name asc
 *   →  (select jbs_ob1.name from Customer jbs_ob1 where jbs_ob1 = o.customer) asc nulls last, o.idinvoice
 * customer.city.name desc
 *   →  (select jbs_ob1.city.name from Customer jbs_ob1 where jbs_ob1 = o.customer) desc nulls first, o.idinvoice desc
 * </pre>
 * <p>Si la raíz no tiene la asociación nula, la subconsulta busca la fila
 * destino por la clave foránea; si la tiene nula (o la clave apunta a una fila
 * que no existe), no encuentra nada y devuelve nulo: la fila de afuera se
 * conserva. Los saltos siguientes se navegan <b>dentro</b> de la subconsulta,
 * donde un {@code join} interno solo hace que devuelva nulo. Todo lo de adentro
 * va calificado con su propio alias, así que no interfiere con los atributos
 * sin calificar de la sentencia.</p>
 *
 * <p><b>Por qué por la clave foránea y no por la raíz.</b> La forma anterior,
 * {@code (select jbs_ob1.customer.name from Invoice jbs_ob1 where jbs_ob1 = o)},
 * da el mismo valor, pero obliga al motor a volver a leer la raíz por cada
 * fila: en SQL Server, con 4,96 millones de filas, una página costaba 31 s
 * contra 7,6 s de la correlación por la clave foránea. La forma vieja queda como
 * respaldo cuando el primer salto no tiene en la raíz una columna simple que
 * apunte a la clave del destino: lado inverso ({@code @OneToOne(mappedBy = ...)}),
 * unión por otra columna ({@code referencedColumnName}), columnas de unión
 * compuestas ({@code @JoinColumns}), {@code @JoinTable},
 * {@code @PrimaryKeyJoinColumn} o un destino sin una clave simple
 * ({@code @IdClass}, {@code @EmbeddedId}).</p>
 *
 * <p><b>Nulos.</b> Los términos reescritos llevan {@code nulls last} en orden
 * ascendente y {@code nulls first} en descendente (los registros sin la
 * asociación van al final, o al principio si se invierte el orden), igual en
 * todos los motores: Hibernate emula la cláusula donde el motor no la tiene.
 * Si el término ya declara {@code nulls first|last}, se respeta.</p>
 *
 * <p><b>Desempate.</b> Toda expresión de orden termina con la clave de la
 * raíz ({@code o.idinvoice}): con filas empatadas en el orden pedido (las
 * que no tienen la asociación empatan todas en nulo, pero también basta un
 * {@code customer.code} repetido) la paginación por {@code offset}/{@code fetch}
 * no garantiza un orden estable, y una página repite filas de otra mientras
 * otras no salen nunca. El desempate toma la <b>dirección del último término
 * explícito</b> del orden ({@code fecha desc} → {@code fecha desc, o.idinvoice desc};
 * {@code codigo} → {@code codigo, o.idinvoice}): un índice sobre esa columna
 * lleva la clave como sufijo ascendente, y solo con la misma dirección el motor
 * puede servir la primera página recorriéndolo hacia atrás, sin ordenar toda la
 * tabla. Cambia
 * solo el orden entre filas empatadas en todo lo pedido; sigue siendo un
 * orden total. No se agrega lo que ya figura: si el orden ya incluye
 * la clave, o alguno de sus componentes, como término propio (no dentro de
 * una expresión: {@code idinvoice % 2} no cuenta), queda donde está. Tampoco
 * se agrega una clave simple cuya <b>columna</b> ya está en el orden por otro
 * camino (un escalar mapeado a la misma columna, o {@code asociacion.<clave>}
 * unida por esa columna): SQL Server rechaza una columna repetida en el
 * {@code ORDER BY} (error 169 de SQL Server). Con clave compuesta
 * ({@code @IdClass}) se agregan todos sus atributos {@code @Id}; con
 * {@code @EmbeddedId}, los atributos de la clase incrustada
 * ({@code o.id.parte1, o.id.parte2}), salvo que el orden ya nombre el
 * {@code @EmbeddedId} entero. Con identidad derivada de una única asociación
 * a uno ({@code @Id @OneToOne purchase}), el desempate es la clave del
 * destino por esa asociación ({@code o.purchase.idpurchase}), que se lee de
 * la clave foránea sin {@code join} (ver {@code derivedKeyOf}).
 * No hay desempate si la clave no se puede
 * determinar con seguridad (ver {@code keyOf}). Una expresión vacía queda
 * vacía: agregar un orden donde no lo había cambiaría lo que hoy se ve.</p>
 *
 * <p><b>Qué se reescribe.</b> Solo un término que sea un camino de atributos
 * (opcionalmente calificado con el alias de la raíz), con dirección y
 * {@code nulls} opcionales, cuyos segmentos navegados sean <b>todos</b>
 * asociaciones a uno ({@code @ManyToOne} / {@code @OneToOne}) y al menos una
 * de ellas pueda ser nula. Ante cualquier duda el término queda como está, que
 * es el comportamiento anterior:</p>
 * <ul>
 * <li>atributos simples ({@code fecha desc}), funciones, números, paréntesis
 * o cualquier expresión que no sea un camino;</li>
 * <li>un camino que no empieza en una asociación a uno de la raíz
 * ({@code @Embedded}, {@code @EmbeddedId}, un atributo inexistente) o que pasa
 * por una colección;</li>
 * <li>un camino cuyas asociaciones son todas obligatorias, declaradas con
 * {@code optional = false} explícito en {@code @ManyToOne}/{@code @OneToOne}:
 * el {@code join} interno no descarta nada y la subconsulta solo costaría.
 * <b>{@code nullable = false} en las columnas de unión no alcanza</b>: el
 * modelo puede declararlo en columnas que en la base admiten y tienen nulos
 * (una factura sin {@code status}, por ejemplo), y ordenar por ellas
 * perdería filas;</li>
 * <li>la clave de la asociación ({@code customer.idcustomer}), cuando la
 * asociación es el lado dueño de la clave foránea y la une por la clave del
 * destino: Hibernate la lee de la columna de la clave foránea, sin
 * {@code join}. Del lado inverso ({@code @OneToOne(mappedBy = ...)}) o con una
 * unión por otra columna ({@code referencedColumnName}) sí hay {@code join}, y
 * el término se reescribe.</li>
 * </ul>
 *
 * <p>Los atributos se buscan por su nombre exacto (JPQL distingue mayúsculas)
 * en la clase y sus superclases. La clase no conoce ninguna aplicación: la usa
 * {@code AbstractDataService.getSelectCmd} y la puede usar cualquiera que arme
 * una sentencia {@code select <alias> from <Entidad> <alias> ... order by}.</p>
 *
 * @author Jorge Enciso
 */
public final class AssociationOrderBy {

    /** Prefijo de los alias de las subconsultas; se numeran por término. */
    static final String ALIAS_PREFIX = "jbs_ob";

    /** Identificador de Java (admite letras no ASCII, como {@code año}). */
    private static final String IDENTIFIER = "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";

    /** Camino de atributos, dirección y precedencia de nulos opcionales. */
    private static final Pattern TERM = Pattern.compile(
            "^(" + IDENTIFIER + "(?:\\." + IDENTIFIER + ")+)"
            + "(?:\\s+(asc|desc))?"
            + "(?:\\s+nulls\\s+(first|last))?$",
            Pattern.CASE_INSENSITIVE);

    /** Dirección (y nulos) al final de un término cualquiera: {@code ... desc nulls last}. */
    private static final Pattern TRAILING_DIRECTION = Pattern.compile(
            "\\s(asc|desc)(?:\\s+nulls\\s+(?:first|last))?$", Pattern.CASE_INSENSITIVE);

    /** Un camino de atributos (uno o más segmentos), con dirección y nulos opcionales. */
    private static final Pattern TERM_PATH = Pattern.compile(
            "^(" + IDENTIFIER + "(?:\\." + IDENTIFIER + ")*)"
            + "(?:\\s+(asc|desc))?"
            + "(?:\\s+nulls\\s+(first|last))?$",
            Pattern.CASE_INSENSITIVE);

    private AssociationOrderBy() {
    }

    /**
     * Reescribe la expresión de orden usando el nombre simple de la clase como
     * nombre de la entidad (la convención de las sentencias del framework).
     *
     * @param rootType clase de la entidad raíz de la sentencia.
     * @param rootAlias alias de la raíz en la sentencia (por ejemplo {@code o}).
     * @param order expresión de orden, sin la palabra {@code order by}.
     * @return la expresión reescrita y con la clave de la raíz como
     * desempate; la misma si no hay nada que cambiar o si algún argumento es
     * nulo o vacío.
     */
    public static String rewrite(Class<?> rootType, String rootAlias, String order) {
        return rewrite(rootType, rootType == null ? null : rootType.getSimpleName(), rootAlias, order);
    }

    /**
     * Reescribe los términos de la expresión de orden que navegan una
     * asociación que puede ser nula y agrega la clave de la raíz como
     * desempate. Ver la descripción de la clase.
     *
     * @param rootType clase de la entidad raíz de la sentencia.
     * @param entityName nombre de la entidad raíz tal como figura en el
     * {@code from} de la sentencia.
     * @param rootAlias alias de la raíz en la sentencia.
     * @param order expresión de orden, sin la palabra {@code order by}.
     * @return la expresión reescrita y con el desempate; la misma si no hay
     * nada que cambiar o si algún argumento es nulo o vacío.
     */
    public static String rewrite(Class<?> rootType, String entityName, String rootAlias, String order) {
        if (rootType == null || isBlank(entityName) || isBlank(rootAlias) || isBlank(order)) {
            return order;
        }
        List<String> terms = splitTopLevel(order);
        if (terms == null) {
            return order;
        }
        String rewritten = rewriteTerms(rootType, entityName.trim(), rootAlias.trim(), terms);
        return appendKey(rootType, rootAlias.trim(), rewritten != null ? rewritten : order, terms);
    }

    /**
     * Agrega al final de la expresión de orden la clave de la raíz como
     * desempate, sin reescribir ningún término. Ver «Desempate» en la
     * descripción de la clase.
     *
     * @param rootType clase de la entidad raíz de la sentencia.
     * @param rootAlias alias de la raíz en la sentencia.
     * @param order expresión de orden, sin la palabra {@code order by}.
     * @return la expresión con el desempate; la misma si ya ordena por la
     * clave, si la clave no se puede determinar con seguridad o si algún
     * argumento es nulo o vacío.
     */
    public static String addKeyTieBreak(Class<?> rootType, String rootAlias, String order) {
        if (rootType == null || isBlank(rootAlias) || isBlank(order)) {
            return order;
        }
        List<String> terms = splitTopLevel(order);
        if (terms == null) {
            return order;
        }
        return appendKey(rootType, rootAlias.trim(), order, terms);
    }

    /**
     * Reescribe los términos que navegan una asociación que puede ser nula.
     *
     * @param rootType clase raíz.
     * @param entityName nombre de la entidad raíz.
     * @param rootAlias alias de la raíz.
     * @param terms términos de la expresión original.
     * @return la expresión con los términos reescritos, o {@code null} si
     * ninguno cambió.
     */
    private static String rewriteTerms(Class<?> rootType, String entityName, String rootAlias, List<String> terms) {
        StringBuilder result = new StringBuilder();
        boolean changed = false;
        int aliasNumber = 0;
        for (String term : terms) {
            String rewritten = rewriteTerm(rootType, entityName, rootAlias, term.trim(),
                    ALIAS_PREFIX + (aliasNumber + 1));
            if (rewritten != null) {
                changed = true;
                aliasNumber++;
            }
            if (result.length() > 0) {
                result.append(", ");
            }
            result.append(rewritten != null ? rewritten : term.trim());
        }
        return changed ? result.toString() : null;
    }

    /**
     * Agrega a la expresión los componentes de la clave que todavía no figuran
     * entre los términos originales.
     *
     * @param rootType clase raíz.
     * @param rootAlias alias de la raíz.
     * @param order expresión (ya reescrita, si correspondía).
     * @param terms términos de la expresión original, para saber qué ya figura.
     * @return la expresión con el desempate.
     */
    private static String appendKey(Class<?> rootType, String rootAlias, String order, List<String> terms) {
        Key key = keyOf(rootType);
        if (key == null) {
            key = derivedKeyOf(rootType);
        }
        if (key == null) {
            return order;
        }
        if (key.embeddedField != null && containsTerm(terms, rootAlias, key.embeddedField)) {
            return order;
        }
        String direction = isDescending(terms.get(terms.size() - 1)) ? " desc" : "";
        StringBuilder result = new StringBuilder(order);
        boolean appended = false;
        for (String component : key.components) {
            if (!containsTerm(terms, rootAlias, component)
                    && (key.embeddedField != null || !containsKeyColumn(rootType, rootAlias, terms, component))) {
                result.append(", ").append(rootAlias).append('.').append(component).append(direction);
                appended = true;
            }
        }
        return appended ? result.toString() : order;
    }

    /**
     * Indica si un término del orden es descendente: termina en {@code desc},
     * con o sin {@code nulls first|last}. Vale para cualquier expresión
     * ({@code abs(x) desc}), no solo para un camino de atributos.
     *
     * @param term término de la expresión original.
     * @return {@code true} si es descendente.
     */
    private static boolean isDescending(String term) {
        Matcher matcher = TRAILING_DIRECTION.matcher(term.trim());
        return matcher.find() && "desc".equalsIgnoreCase(matcher.group(1));
    }

    /** La clave de una entidad: sus componentes, como caminos desde la raíz. */
    private static final class Key {

        /** Atributo {@code @EmbeddedId}, o {@code null} si la clave no es incrustada. */
        private final String embeddedField;
        /** Componentes en orden de declaración ({@code idinvoice}, {@code id.parte1}). */
        private final List<String> components;

        private Key(String embeddedField, List<String> components) {
            this.embeddedField = embeddedField;
            this.components = components;
        }
    }

    /**
     * Determina la clave de la entidad para usarla como desempate.
     *
     * <ul>
     * <li>un atributo {@code @Id} simple: ese atributo;</li>
     * <li>varios {@code @Id} ({@code @IdClass}): todos, en orden de
     * declaración;</li>
     * <li>un {@code @EmbeddedId}: los atributos de la clase incrustada
     * ({@code id.parte1}, {@code id.parte2}).</li>
     * </ul>
     * <p>No es «seguro» —y no hay desempate— si la clave no se encuentra por
     * reflexión sobre los atributos (anotaciones en los métodos), si algún
     * componente es una asociación (identidad derivada, {@code @MapsId}) o si
     * la clase incrustada anida otra o no tiene atributos: ordenar por una
     * asociación podría volver a agregar un {@code join}.</p>
     *
     * @param type clase de la entidad.
     * @return la clave, o {@code null} si no se puede determinar con seguridad.
     */
    private static Key keyOf(Class<?> type) {
        List<String> ids = new ArrayList<>();
        Field embedded = null;
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(EmbeddedId.class)) {
                    if (embedded != null) {
                        return null;
                    }
                    embedded = field;
                } else if (field.isAnnotationPresent(Id.class)) {
                    if (isAssociation(field)) {
                        return null;
                    }
                    ids.add(field.getName());
                }
            }
            current = current.getSuperclass();
        }
        if (embedded != null) {
            if (!ids.isEmpty()) {
                return null;
            }
            List<String> components = new ArrayList<>();
            // Incluye los atributos heredados (una superclase @MappedSuperclass).
            List<Class<?>> hierarchy = new ArrayList<>();
            for (Class<?> c = embedded.getType(); c != null && c != Object.class; c = c.getSuperclass()) {
                hierarchy.add(0, c);
            }
            for (Class<?> c : hierarchy) {
                for (Field field : c.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())
                            || field.isAnnotationPresent(Transient.class)) {
                        continue;
                    }
                    if (isAssociation(field) || field.isAnnotationPresent(Embedded.class)
                            || field.isAnnotationPresent(EmbeddedId.class)) {
                        return null;
                    }
                    components.add(embedded.getName() + "." + field.getName());
                }
            }
            return components.isEmpty() ? null : new Key(embedded.getName(), components);
        }
        return ids.isEmpty() ? null : new Key(null, ids);
    }

    /**
     * Clave para el desempate de una entidad con <b>identidad derivada de una
     * única asociación a uno</b>: un solo {@code @Id}, que es un
     * {@code @OneToOne} o un {@code @ManyToOne} (por ejemplo una entrega cuya
     * clave es la compra: {@code @Id @OneToOne purchase}). El
     * desempate es {@code o.<asociacion>.<clave del destino>}, que Hibernate
     * lee de la columna de la clave foránea, sin {@code join}.
     *
     * <p>Solo con la misma regla de {@link #readsForeignKey}: lado dueño, una
     * sola {@code @JoinColumn} (declarada, para conocer su columna y controlar
     * el error 169), sin {@code @JoinTable} ni {@code @PrimaryKeyJoinColumn} ni
     * {@code targetEntity}, y un destino con una clave simple que no sea a su
     * vez una asociación. En cualquier otro caso, como antes, no hay
     * desempate.</p>
     *
     * @param type clase de la entidad.
     * @return la clave derivada, o {@code null} si no aplica.
     */
    private static Key derivedKeyOf(Class<?> type) {
        Field association = null;
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(EmbeddedId.class)) {
                    return null;
                }
                if (field.isAnnotationPresent(Id.class)) {
                    if (association != null || !isToOne(field)) {
                        return null;
                    }
                    association = field;
                }
            }
        }
        Field targetId = derivedTargetId(association);
        return targetId == null ? null : new Key(null, List.of(association.getName() + "." + targetId.getName()));
    }

    /**
     * Clave del destino de una asociación de identidad derivada, si la columna
     * de unión se puede leer sin {@code join} (ver {@link #derivedKeyOf}).
     *
     * @param association asociación que es la clave de la raíz.
     * @return el {@code @Id} simple del destino, o {@code null}.
     */
    private static Field derivedTargetId(Field association) {
        if (association == null) {
            return null;
        }
        OneToOne oneToOne = association.getAnnotation(OneToOne.class);
        ManyToOne manyToOne = association.getAnnotation(ManyToOne.class);
        if ((oneToOne != null && (!oneToOne.mappedBy().isEmpty() || oneToOne.targetEntity() != void.class))
                || (manyToOne != null && manyToOne.targetEntity() != void.class)
                || association.isAnnotationPresent(JoinTable.class)
                || association.isAnnotationPresent(PrimaryKeyJoinColumn.class)
                || association.isAnnotationPresent(PrimaryKeyJoinColumns.class)
                || joinColumns(association).size() != 1) {
            return null;
        }
        Key targetKey = keyOf(association.getType());
        if (targetKey == null || targetKey.embeddedField != null || targetKey.components.size() != 1) {
            return null;
        }
        Field targetId = findField(association.getType(), targetKey.components.get(0));
        return (targetId != null && readsForeignKey(association, targetId)) ? targetId : null;
    }

    /**
     * Indica si el atributo es una asociación (a uno o a muchos).
     *
     * @param field atributo.
     * @return {@code true} si lleva una anotación de asociación.
     */
    private static boolean isAssociation(Field field) {
        return isToOne(field) || field.isAnnotationPresent(OneToMany.class)
                || field.isAnnotationPresent(ManyToMany.class);
    }

    /**
     * Indica si algún término ordena ya por el atributo indicado de la raíz,
     * con o sin el alias y en cualquier dirección.
     *
     * @param terms términos de la expresión.
     * @param rootAlias alias de la raíz.
     * @param field atributo (o camino, como {@code id.parte1}).
     * @return {@code true} si ya figura.
     */
    private static boolean containsTerm(List<String> terms, String rootAlias, String field) {
        Pattern pattern = Pattern.compile("^(?:" + Pattern.quote(rootAlias + ".") + ")?" + Pattern.quote(field)
                + "(?:\\s+(?i:asc|desc))?(?:\\s+(?i:nulls)\\s+(?i:first|last))?$");
        for (String term : terms) {
            if (pattern.matcher(term.trim()).matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Indica si algún término ya ordena por la <b>columna</b> de un atributo
     * de la clave por otro camino: un atributo escalar mapeado a la misma
     * columna, una asociación a uno entera cuya columna de unión es esa, o
     * {@code asociacion.<clave>} cuya columna de unión es esa. Si
     * se agregara igual, SQL Server rechazaría la sentencia por tener la
     * misma columna dos veces en el {@code ORDER BY} (error 169).
     *
     * @param rootType clase raíz.
     * @param rootAlias alias de la raíz.
     * @param terms términos de la expresión.
     * @param keyField atributo de la clave.
     * @return {@code true} si la columna ya figura.
     */
    private static boolean containsKeyColumn(Class<?> rootType, String rootAlias, List<String> terms, String keyField) {
        String keyColumn;
        int dot = keyField.indexOf('.');
        if (dot > 0) {
            //Clave derivada: asociacion.<clave del destino>; su columna es la de unión.
            Field association = findField(rootType, keyField.substring(0, dot));
            if (association == null || derivedTargetId(association) == null) {
                return false;
            }
            keyColumn = joinColumns(association).get(0).name();
        } else {
            Field key = findField(rootType, keyField);
            if (key == null) {
                return false;
            }
            keyColumn = columnName(key);
        }
        for (String term : terms) {
            Matcher matcher = TERM_PATH.matcher(term.trim());
            if (!matcher.matches()) {
                continue;
            }
            String[] segments = matcher.group(1).split("\\.");
            int first = (segments.length > 1 && segments[0].equals(rootAlias)
                    && findField(rootType, rootAlias) == null) ? 1 : 0;
            int length = segments.length - first;
            if (length == 1) {
                Field field = findField(rootType, segments[first]);
                if (field != null && !isAssociation(field) && columnName(field).equalsIgnoreCase(keyColumn)) {
                    return true;
                }
                // La asociación entera como término ({@code purchase desc}) ordena por su
                // columna de unión: con identidad derivada es la misma columna de la clave.
                if (field != null && isToOne(field)) {
                    List<JoinColumn> columns = joinColumns(field);
                    if (columns.size() == 1 && columns.get(0).name().equalsIgnoreCase(keyColumn)) {
                        return true;
                    }
                }
            } else if (length == 2) {
                Field association = findField(rootType, segments[first]);
                if (association == null || !isToOne(association)) {
                    continue;
                }
                Field target = findField(association.getType(), segments[first + 1]);
                List<JoinColumn> columns = joinColumns(association);
                if (target != null && target.isAnnotationPresent(Id.class) && readsForeignKey(association, target)
                        && columns.size() == 1 && columns.get(0).name().equalsIgnoreCase(keyColumn)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Indica si la expresión de orden tiene algún término que
     * {@link #rewrite(Class, String, String, String)} reescribiría (sin
     * contar el desempate, que se agrega siempre).
     *
     * @param rootType clase de la entidad raíz.
     * @param rootAlias alias de la raíz.
     * @param order expresión de orden.
     * @return {@code true} si algún término navega una asociación que puede
     * ser nula.
     */
    public static boolean needsRewrite(Class<?> rootType, String rootAlias, String order) {
        if (rootType == null || isBlank(rootAlias) || isBlank(order)) {
            return false;
        }
        List<String> terms = splitTopLevel(order);
        return terms != null
                && rewriteTerms(rootType, rootType.getSimpleName(), rootAlias.trim(), terms) != null;
    }

    /**
     * Reescribe un término, o devuelve {@code null} si queda como está.
     *
     * @param rootType clase raíz.
     * @param entityName nombre de la entidad raíz.
     * @param rootAlias alias de la raíz.
     * @param term término, sin espacios en los extremos.
     * @param alias alias de la subconsulta.
     * @return el término reescrito, o {@code null}.
     */
    private static String rewriteTerm(Class<?> rootType, String entityName, String rootAlias,
            String term, String alias) {
        Matcher matcher = TERM.matcher(term);
        if (!matcher.matches()) {
            return null;
        }
        String[] segments = matcher.group(1).split("\\.");
        int first = 0;
        // Camino calificado con el alias de la raíz (o.customer.name), salvo
        // que la raíz tenga un atributo con ese mismo nombre.
        if (segments[0].equals(rootAlias) && findField(rootType, rootAlias) == null) {
            first = 1;
        }
        if (segments.length - first < 2) {
            return null;
        }
        if (!navigatesOptionalAssociation(rootType, segments, first)) {
            return null;
        }
        String direction = matcher.group(2) == null ? "asc" : matcher.group(2).toLowerCase();
        String nulls = matcher.group(3) != null
                ? matcher.group(3).toLowerCase()
                : ("asc".equals(direction) ? "last" : "first");
        Field firstHop = findField(rootType, segments[first]);
        String targetEntity = foreignKeyTarget(firstHop);
        String subquery;
        if (targetEntity != null) {
            // Por la clave foránea del primer salto: el resto del camino se
            // navega dentro de la subconsulta.
            subquery = "(select " + alias + "." + join(segments, first + 1) + " from " + targetEntity + " " + alias
                    + " where " + alias + " = " + rootAlias + "." + segments[first] + ")";
        } else {
            // Respaldo: sobre la raíz, con el camino entero.
            subquery = "(select " + alias + "." + join(segments, first) + " from " + entityName + " " + alias
                    + " where " + alias + " = " + rootAlias + ")";
        }
        return subquery + " " + direction + " nulls " + nulls;
    }

    /**
     * Une los segmentos desde {@code from} con puntos.
     *
     * @param segments segmentos del camino.
     * @param from índice del primero que se incluye.
     * @return el camino.
     */
    private static String join(String[] segments, int from) {
        StringBuilder path = new StringBuilder();
        for (int i = from; i < segments.length; i++) {
            if (path.length() > 0) {
                path.append('.');
            }
            path.append(segments[i]);
        }
        return path.toString();
    }

    /**
     * Decide si la subconsulta se puede correlacionar por la clave foránea de
     * la asociación ({@code where x = o.<asociacion>}) en lugar de volver a leer
     * la raíz, y devuelve el nombre de la entidad destino.
     * <p>Solo cuando la raíz tiene una columna simple que apunta a la clave
     * simple del destino: es el lado dueño, con a lo sumo un
     * {@code @JoinColumn} cuyo {@code referencedColumnName} (si lo declara) es
     * la columna de la clave del destino, sin {@code @JoinTable} ni
     * {@code @PrimaryKeyJoinColumn} ni {@code targetEntity}, y el destino tiene un único {@code @Id}
     * que no es una asociación. En cualquier otro caso devuelve {@code null} y
     * se usa la forma sobre la raíz, que es la de antes.</p>
     *
     * @param association primer salto del camino (asociación a uno).
     * @return nombre de la entidad destino ({@code @Entity(name)} o el nombre
     * simple de la clase), o {@code null} si no se puede correlacionar por la
     * clave foránea.
     */
    private static String foreignKeyTarget(Field association) {
        if (association == null || !isToOne(association)) {
            return null;
        }
        OneToOne oneToOne = association.getAnnotation(OneToOne.class);
        if (oneToOne != null && (!oneToOne.mappedBy().isEmpty() || oneToOne.targetEntity() != void.class)) {
            return null;
        }
        // Con targetEntity el destino no es el tipo del atributo: no se adivina.
        ManyToOne manyToOne = association.getAnnotation(ManyToOne.class);
        if (manyToOne != null && manyToOne.targetEntity() != void.class) {
            return null;
        }
        if (association.isAnnotationPresent(JoinTable.class)
                || association.isAnnotationPresent(PrimaryKeyJoinColumn.class)
                || association.isAnnotationPresent(PrimaryKeyJoinColumns.class)
                || joinColumns(association).size() > 1) {
            return null;
        }
        Class<?> target = association.getType();
        if (target.isInterface() || target.isPrimitive() || target.isArray()) {
            return null;
        }
        Key key = keyOf(target);
        if (key == null || key.embeddedField != null || key.components.size() != 1) {
            return null;
        }
        Field targetId = findField(target, key.components.get(0));
        if (targetId == null || !readsForeignKey(association, targetId)) {
            return null;
        }
        Entity entity = target.getAnnotation(Entity.class);
        return (entity != null && !entity.name().isEmpty()) ? entity.name() : target.getSimpleName();
    }

    /**
     * Recorre los segmentos del camino (salvo el último, que es el atributo
     * por el que se ordena) y decide si el orden puede descartar filas.
     *
     * @param rootType clase raíz.
     * @param segments segmentos del camino.
     * @param first índice del primer segmento (después del alias).
     * @return {@code true} si todos los segmentos navegados son asociaciones
     * a uno, al menos una puede ser nula y el camino no termina en la clave de
     * la última asociación.
     */
    private static boolean navigatesOptionalAssociation(Class<?> rootType, String[] segments, int first) {
        Class<?> current = rootType;
        Field association = null;
        boolean optional = false;
        int last = segments.length - 1;
        for (int i = first; i < last; i++) {
            Field field = findField(current, segments[i]);
            if (field == null || !isToOne(field)) {
                return false;
            }
            optional = optional || isOptional(field);
            association = field;
            current = field.getType();
        }
        if (!optional) {
            return false;
        }
        // asociacion.<clave>: Hibernate la lee de la clave foránea, sin join,
        // solo si es la única asociación navegada y es el lado dueño de esa
        // clave, unido por la clave del destino.
        if (last - first == 1) {
            Field target = findField(current, segments[last]);
            if (target != null && target.isAnnotationPresent(Id.class) && readsForeignKey(association, target)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Indica si {@code asociacion.<clave>} se resuelve con la columna de la
     * clave foránea de la raíz, sin {@code join}: la asociación es el lado
     * dueño (no un {@code @OneToOne(mappedBy = ...)}) y sus columnas de unión
     * apuntan a la clave del destino.
     *
     * @param association asociación a uno.
     * @param targetId atributo {@code @Id} del destino.
     * @return {@code true} si Hibernate la lee de la clave foránea.
     */
    private static boolean readsForeignKey(Field association, Field targetId) {
        OneToOne oneToOne = association.getAnnotation(OneToOne.class);
        if (oneToOne != null && !oneToOne.mappedBy().isEmpty()) {
            return false;
        }
        String idColumn = columnName(targetId);
        for (JoinColumn column : joinColumns(association)) {
            if (!column.referencedColumnName().isEmpty()
                    && !column.referencedColumnName().equalsIgnoreCase(idColumn)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Devuelve el nombre de la columna de un atributo: el de {@code @Column}
     * o, si no lo declara, el del atributo.
     *
     * @param field atributo.
     * @return nombre de la columna.
     */
    private static String columnName(Field field) {
        Column column = field.getAnnotation(Column.class);
        return (column != null && !column.name().isEmpty()) ? column.name() : field.getName();
    }

    /**
     * Devuelve las columnas de unión de una asociación ({@code @JoinColumn} o
     * {@code @JoinColumns}).
     *
     * @param field asociación.
     * @return las columnas de unión; vacía si no declara ninguna.
     */
    private static List<JoinColumn> joinColumns(Field field) {
        List<JoinColumn> result = new ArrayList<>();
        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
        if (joinColumn != null) {
            result.add(joinColumn);
        }
        JoinColumns joinColumnsAnnotation = field.getAnnotation(JoinColumns.class);
        if (joinColumnsAnnotation != null) {
            result.addAll(List.of(joinColumnsAnnotation.value()));
        }
        return result;
    }

    /**
     * Indica si el atributo es una asociación a uno.
     *
     * @param field atributo.
     * @return {@code true} si lleva {@code @ManyToOne} o {@code @OneToOne}.
     */
    private static boolean isToOne(Field field) {
        return field.isAnnotationPresent(ManyToOne.class) || field.isAnnotationPresent(OneToOne.class);
    }

    /**
     * Indica si la asociación puede ser nula: lo es salvo que se la declare
     * {@code optional = false} explícito en su {@code @ManyToOne} o
     * {@code @OneToOne}.
     * <p>{@code nullable = false} en las columnas de unión ({@code @JoinColumn}
     * o {@code @JoinColumns}) <b>no</b> la hace obligatoria: es una
     * declaración del esquema que el modelo no siempre cumple —hay columnas
     * así que en la base admiten y tienen nulos—, y tomarla por cierta dejaba
     * el {@code join} interno que descarta esas filas. Ante la duda se reescribe: el costo es una
     * subconsulta, no filas perdidas.</p>
     *
     * @param field atributo de una asociación a uno.
     * @return {@code true} si puede ser nula.
     */
    private static boolean isOptional(Field field) {
        ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
        if (manyToOne != null && !manyToOne.optional()) {
            return false;
        }
        OneToOne oneToOne = field.getAnnotation(OneToOne.class);
        return oneToOne == null || oneToOne.optional();
    }

    /**
     * Busca un atributo por su nombre exacto en la clase y sus superclases.
     *
     * @param type clase.
     * @param name nombre del atributo.
     * @return el atributo, o {@code null} si no existe.
     */
    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getName().equals(name)) {
                    return field;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    /**
     * Parte la expresión por las comas que no están dentro de paréntesis ni
     * de literales de texto.
     *
     * @param order expresión de orden.
     * @return los términos, o {@code null} si los paréntesis o las comillas
     * no cierran (la expresión queda como está).
     */
    static List<String> splitTopLevel(String order) {
        List<String> terms = new ArrayList<>();
        int depth = 0;
        boolean inQuote = false;
        int start = 0;
        for (int i = 0; i < order.length(); i++) {
            char c = order.charAt(i);
            if (c == '\'') {
                inQuote = !inQuote;
            } else if (!inQuote && c == '(') {
                depth++;
            } else if (!inQuote && c == ')') {
                depth--;
                if (depth < 0) {
                    return null;
                }
            } else if (!inQuote && depth == 0 && c == ',') {
                terms.add(order.substring(start, i));
                start = i + 1;
            }
        }
        if (depth != 0 || inQuote) {
            return null;
        }
        terms.add(order.substring(start));
        return terms;
    }

    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}
