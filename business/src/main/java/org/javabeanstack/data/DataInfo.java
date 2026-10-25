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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.util.Strings;

/**
 * Esta clase contiene metodos que brindan información sobre los modelos ejb ej.
 * si un campo es primarykey,
 *
 * @author Jorge Enciso
 */
public class DataInfo {

    private static final Logger LOGGER = LogManager.getLogger(DataInfo.class);

    /**
     * Atributos de la clave única ya resueltos, por clase de entidad (plan
     * {@code QUERYUK}). Las anotaciones no cambian en tiempo de ejecución, así
     * que la traducción columna→atributo se hace una sola vez por clase y la
     * caché vive con el cargador de clases del despliegue.
     */
    private static final Map<Class<?>, String[]> UK_ATTRIBUTES = new ConcurrentHashMap<>();

    /**
     * Centinela de la caché: la entidad no declara clave única o alguna de sus
     * columnas no se pudo traducir ({@code ConcurrentHashMap} no admite nulos).
     */
    private static final String[] SIN_CLAVE_UNICA = new String[0];

    private DataInfo() {
    }

    /**
     * Determina si el campo o miembro dado corresponde a la clave primaria.
     *
     * @param <T>
     * @param classType clase DataRow dado
     * @param fieldname nombre del campo
     * @return Verdadero si es la clave primaria.
     */
    public static <T extends IDataRow> boolean isPrimaryKey(Class<T> classType, String fieldname) {
        try {
            Field field = DataInfo.getDeclaredField(classType, fieldname);
            Id id = field.getAnnotation(Id.class);
            return id != null;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Determina si un campo o miembro dado es o forma parte de una clave unica.
     *
     * <p>Responde verdadero tanto si el nombre dado es una <b>columna</b> de la
     * clave única —lo que declara {@code @UniqueConstraint}, ver
     * {@link #getUniqueFields(Class)}— como si es uno de los <b>atributos</b>
     * que la mapean ({@link #getUniqueKeyAttributes(Class)}). Los llamadores
     * son pantallas y servicios que hablan en atributos (es el nombre que
     * conoce el formulario), y la columna puede escribirse distinto:
     * preguntarle por {@code nombreCorto} cuando la anotación dice
     * {@code nombre_corto} devolvía falso (plan {@code QUERYUK}, hallazgo
     * {@code M0-03}). La comparación sigue sin distinguir mayúsculas.</p>
     *
     * @param <T>
     * @param classType clase DataRow dado
     * @param fieldname nombre del campo
     * @return Verdadero si forma parte de una clave unica.
     */
    public static <T extends IDataRow> boolean isUniqueKey(Class<T> classType, String fieldname) {
        try {
            String[] fields = DataInfo.getUniqueFields(classType);
            if (fields == null) {
                return false;
            }
            for (String field : fields) {
                if (field.equalsIgnoreCase(fieldname)) {
                    return true;
                }
            }
            String[] attributes = DataInfo.getUniqueKeyAttributes(classType);
            if (attributes == null) {
                return false;
            }
            for (String attribute : attributes) {
                if (attribute.equalsIgnoreCase(fieldname)) {
                    return true;
                }
            }
            return false;
        } catch (Exception exp) {
            return false;
        }
    }

    /**
     * Determina si un campo o miembro es una clave foranea.
     *
     * @param <T>
     * @param classType clase DataRow dado
     * @param fieldname nombre del campo
     * @return Verdadero si es una clave foranea.
     */
    public static <T extends IDataRow> boolean isForeignKey(Class<T> classType, String fieldname) {
        try {
            Field field = DataInfo.getDeclaredField(classType, fieldname);
            JoinColumn idforeignkey = field.getAnnotation(JoinColumn.class);
            return idforeignkey != null;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Determina si un campo existe en la clase.
     *
     * @param <T>
     * @param classType clase DataRow dado
     * @param fieldname nombre del campo
     * @return Verdadero si existe.
     */
    public static <T extends IDataRow> boolean isFieldExist(Class<T> classType, String fieldname) {
        try {
            Field field = DataInfo.getDeclaredField(classType, fieldname);
            if (field != null) {
                return true;
            }
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return false;
    }
    
    /**
     * Determina si un campo existe en la clase.
     *
     * @param <T>
     * @param classType clase DataRow dado
     * @param fieldname nombre del campo
     * @return Verdadero si existe.
     */
    public static <T extends IDataRow> String getFieldDB(Class<T> classType, String fieldname) {
        try {
            Field field = DataInfo.getDeclaredField(classType, fieldname);
            if (field == null) {
                return "";
            }
            Column column = field.getAnnotation(Column.class);
            if (column == null){
                return "";
            }
            return column.name();
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return "";
    }
    

    /**
     * Determina si un campo existe en la clase.
     *
     * @param <T>
     * @param classType clase DataRow dado
     * @param methodName nombre del metodo
     * @return Verdadero si existe.
     */
    public static <T extends IDataRow> boolean isMethodExist(Class<T> classType, String methodName) {
        try {
            Method method = DataInfo.getMethod(classType, methodName);
            if (method != null) {
                return true;
            }
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return false;
    }

    /**
     * Determina si un campo o miembro es una clave foranea cuyo fetch = Lazy
     *
     * @param <T>
     * @param classType clase DataRow dado
     * @param fieldname nombre del campo
     * @return Verdadero si fetch = Lazy
     */
    public static <T extends IDataRow> boolean isLazyFetch(Class<T> classType, String fieldname) {
        try {
            Field field = DataInfo.getDeclaredField(classType, fieldname);
            OneToMany annotation = field.getAnnotation(OneToMany.class);
            return annotation.fetch() == FetchType.LAZY;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Devuelve los miembros lazy
     *
     * @param <T>
     * @param classType clase DataRow dado
     * @return una lista con los campos o miembros cuyo fetch = Lazy
     */
    public static <T extends IDataRow> List<Field> getLazyMembers(Class<T> classType) {
        try {
            Field[] fields = classType.getDeclaredFields();
            List<Field> lazyFields = new ArrayList();
            for (Field field : fields) {
                OneToMany annotation = field.getAnnotation(OneToMany.class);
                if (annotation != null) {
                    if (annotation.fetch() == FetchType.LAZY) {
                        lazyFields.add(field);
                    }
                }
            }
            return lazyFields;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return new ArrayList();
    }

    /**
     * Devuelve el nombre de la tabla o vista a la que esta mapeada
     *
     * @param <T>
     * @param classType clase DataRow
     * @return nombre de la tabla
     */
    public static <T extends IDataRow> String getTableName(Class<T> classType) {
        try {
            Table entity = classType.getAnnotation(Table.class);
            return entity.name();
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return "";
    }

    /**
     * Crea una sentencia para buscar un registro especifico utilizando los
     * valores de los campos unicos.
     *
     * <p>La sentencia es HQL, de modo que se escribe con los nombres de los
     * <b>atributos</b> de la entidad; la anotación {@code @UniqueConstraint}
     * declara nombres de <b>columna</b> de la base. La traducción de una cosa a
     * la otra la hace {@link #getUniqueKeyAttributes(Class)} (plan
     * {@code QUERYUK}, hallazgo {@code I5-12}). Si alguna columna de la clave
     * única no corresponde a ningún atributo ni asociación devuelve nulo, que
     * los consumidores tratan como «esta entidad no tiene clave única».</p>
     *
     * @param ejb
     * @return sentencia con la expresión de filtro de/los campos unicos.
     */
    public static String createQueryUkCommand(IDataRow ejb) {
        try {
            // Crear sentencia para buscar el registro
            String command = "select o from " + ejb.getClass().getSimpleName() + " o ";
            if (!ejb.getIdFunctionFind().equals("")) {
                command = command + " where " + "o." + DataInfo.getIdFieldName(ejb.getClass()) + " = :" + ejb.getIdFunctionFind();
                return command;
            }
            String uniqueFilter = "";
            String separador = "";
            String[] uniqueList = DataInfo.getUniqueKeyAttributes(ejb.getClass());
            if (uniqueList == null) {
                return null;
            }
            for (String fieldName : uniqueList) {
                uniqueFilter = uniqueFilter + separador + " o." + fieldName + " = :" + fieldName;
                separador = " and ";
            }
            command = command + " where " + uniqueFilter;
            return command;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    /**
     * Busca y devuelve el nombre del campo clave
     *
     * @param <T>
     * @param classType
     * @return el nombre del campo que conforma la clave primaria, nulo si no
     * pudo conseguirla.
     */
    public static <T extends IDataRow> String getIdFieldName(Class<T> classType) {
        try {
            Field[] fields = classType.getDeclaredFields();
            for (Field field : fields) {
                Id id = field.getAnnotation(Id.class);
                if (id != null) {
                    return field.getName();
                }
            }
            return null;
        } catch (SecurityException ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    /**
     * Devuelve una lista conteniendo los campos que conforman la clave unica.
     *
     * <p><b>Son nombres de COLUMNA de la base</b>, tal cual los declara la
     * primera {@code @UniqueConstraint} de la anotación {@code @Table}: es lo
     * que necesitan el DDL y las comprobaciones que hablan de columnas
     * ({@link #isUniqueKey(Class, String)}). Para escribir una consulta HQL
     * hace falta el nombre del <b>atributo</b>, que devuelve
     * {@link #getUniqueKeyAttributes(Class)}.</p>
     *
     * @param <T>
     * @param classType
     * @return lista conteniendo los campos que conforman la clave unica.
     */
    public static <T extends IDataRow> String[] getUniqueFields(Class<T> classType) {
        try {
            if (classType.getAnnotation(Table.class).uniqueConstraints() == null
                    || classType.getAnnotation(Table.class).uniqueConstraints().length == 0) {
                return null;
            }
            String[] uniqueConst = classType.getAnnotation(Table.class).uniqueConstraints()[0].columnNames();
            if (uniqueConst != null) {
                return uniqueConst;
            }
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    /**
     * Devuelve los ATRIBUTOS de la entidad que conforman la clave única.
     *
     * <p>Traduce cada columna que declara la primera {@code @UniqueConstraint}
     * (ver {@link #getUniqueFields(Class)}) al nombre del atributo o de la
     * asociación que la mapea, con
     * {@link #getAttributeByColumnName(Class, String)}. Es lo que permite que
     * la consulta de clave única sea HQL válido también cuando la columna y el
     * atributo se escriben distinto o cuando la columna es una clave foránea
     * mapeada únicamente como asociación (plan {@code QUERYUK}, hallazgo
     * {@code I5-12}).</p>
     *
     * <p>Dos columnas que caen en la misma asociación (caso de
     * {@code @JoinColumns}) producen un solo atributo; se conserva el orden de
     * aparición. Si alguna columna no se puede traducir devuelve <b>nulo</b> y
     * deja una advertencia en el log <b>una sola vez por clase</b>: el control
     * de duplicados queda sin efecto para esa entidad, que es lo que ya ocurría
     * antes, pero declarado y sin excepción.</p>
     *
     * <p>El resultado se calcula una sola vez por clase (las anotaciones no
     * cambian en tiempo de ejecución) y se guarda en una caché estática.</p>
     *
     * @param <T>
     * @param classType clase de la entidad.
     * @return atributos que conforman la clave única, nulo si la entidad no
     * declara clave única o si alguna de sus columnas no se pudo traducir.
     */
    public static <T extends IDataRow> String[] getUniqueKeyAttributes(Class<T> classType) {
        if (classType == null) {
            return null;
        }
        String[] attributes = UK_ATTRIBUTES.computeIfAbsent(classType, clase -> {
            String[] columns = getUniqueFields((Class<? extends IDataRow>) clase);
            if (columns == null || columns.length == 0) {
                return SIN_CLAVE_UNICA;
            }
            List<String> result = new ArrayList<>();
            for (String column : columns) {
                String attribute = getAttributeByColumnName(clase, column);
                if (attribute == null) {
                    LOGGER.warn("Clave única de {}: la columna '{}' no corresponde a ningún "
                            + "atributo ni asociación; el control de duplicados queda sin "
                            + "efecto para esta entidad", clase.getSimpleName(), column);
                    return SIN_CLAVE_UNICA;
                }
                if (!result.contains(attribute)) {
                    result.add(attribute);
                }
            }
            return result.toArray(new String[0]);
        });
        if (attributes.length == 0) {
            return null;
        }
        //Copia: el arreglo del caché lo comparten todos los hilos del servidor
        //y basta que un llamador lo ordene o lo pise para corromper la clave
        //única de esa entidad para todos (M4-02).
        return attributes.clone();
    }

    /**
     * Devuelve el nombre del atributo de la clase que mapea una columna dada.
     *
     * <p>Recorre los campos declarados de la clase y de sus superclases hasta
     * {@link DataRow} (exclusive), ignorando los campos {@code static}, los
     * {@code transient} y los anotados {@code @Transient}. La búsqueda no
     * distingue mayúsculas de minúsculas, pero el nombre devuelto conserva la
     * grafía del atributo, que es la que entiende HQL.</p>
     *
     * <p>Orden de precedencia (plan {@code QUERYUK}, decisión D5):</p>
     * <ol>
     * <li>campo con {@code @Column(name = columna)};</li>
     * <li>campo <b>sin</b> {@code @Column(name)} cuyo nombre coincide con la
     * columna (mapeo por defecto de JPA);</li>
     * <li>campo con {@code @JoinColumn(name = columna)} —suelto o dentro de un
     * {@code @JoinColumns}—, es decir la <b>asociación</b>.</li>
     * </ol>
     *
     * <p>El orden importa: una entidad puede tener a la vez el escalar
     * {@code @Column(name = "iddocumento")} y la asociación
     * {@code @JoinColumn(name = "iddocumento")}, y también una asociación
     * llamada igual que la columna de otro atributo (por ejemplo
     * {@code Ctbivapyfrmdetalle}, donde la columna {@code ctbivapyfrm} la mapea
     * el atributo {@code ctbivapyfrmCodigo} y existe además la asociación
     * {@code ctbivapyfrm}).</p>
     *
     * <p>Decisión D16 (cierre del plan {@code QUERYUK}, hallazgo
     * {@code M4-01}): cuando la columna la mapea <b>más de un</b> campo, antes
     * de aplicar D5 gana el que <b>puede escribirla</b>, es decir el que no
     * está anotado {@code insertable = false}. El caso real es
     * {@code Documentoctrl}, donde la columna {@code iddocumento} la mapean a
     * la vez el escalar de solo lectura {@code iddocumento} y la asociación
     * escribible {@code documento}: con D5 a secas la consulta de clave única
     * comparaba el escalar, que en el alta todavía viene nulo, y el control de
     * duplicados quedaba inerte. Si ninguno o más de uno puede escribir la
     * columna rige D5, que es lo que ocurre cuando los dos son de solo
     * lectura.</p>
     *
     * <p>Al filtrar por escribibilidad solo se toman en cuenta los campos que
     * mapean la columna de forma <b>explícita</b> (regla 1 y regla 3). Un
     * campo de la regla 2 que además sea una asociación mapea en realidad
     * <b>otra</b> columna —la de su {@code @JoinColumn}— y coincide solo por
     * el nombre, así que no puede ganarle a un mapeo explícito; se lo conserva
     * como candidato de D5 para no cambiar el resultado de las entidades donde
     * hoy es el único.</p>
     *
     * @param classType clase de la entidad.
     * @param columnName nombre de la columna en la base de datos.
     * @return nombre del atributo o de la asociación, nulo si no lo mapea
     * ninguno.
     */
    public static String getAttributeByColumnName(Class<?> classType, String columnName) {
        if (classType == null || Strings.isNullorEmpty(columnName)) {
            return null;
        }
        List<Candidato> candidatos = getColumnCandidates(classType, columnName);
        if (candidatos.isEmpty()) {
            return null;
        }
        if (candidatos.size() > 1) {
            //D16: si uno solo puede escribir la columna, es ese.
            String escribible = null;
            int cuantos = 0;
            for (Candidato candidato : candidatos) {
                if (candidato.mapeoExplicito() && candidato.escribible()) {
                    escribible = candidato.nombre();
                    cuantos++;
                }
            }
            if (cuantos == 1) {
                return escribible;
            }
        }
        //D5: la recolección ya devuelve los candidatos en orden de precedencia.
        return candidatos.get(0).nombre();
    }

    /**
     * Recolecta todos los campos que mapean una columna, en el orden de
     * precedencia de D5.
     *
     * <p>Un mismo campo puede coincidir por dos reglas (nombre igual a la
     * columna y {@code @JoinColumn} con ese nombre); se lo deja una sola vez,
     * en la posición de la primera coincidencia, y se conserva la marca de
     * mapeo explícito si alguna de las dos la tiene.</p>
     *
     * @param classType clase de la entidad.
     * @param columnName nombre de la columna en la base de datos.
     * @return candidatos en orden de precedencia, vacío si ninguno la mapea.
     */
    private static List<Candidato> getColumnCandidates(Class<?> classType, String columnName) {
        List<Candidato> candidatos = new ArrayList<>();
        List<Field> fields = getMappedFields(classType);
        // 1) Atributo con @Column(name = columna)
        for (Field field : fields) {
            Column column = field.getAnnotation(Column.class);
            if (column != null && columnName.equalsIgnoreCase(column.name())) {
                agregarCandidato(candidatos,
                        new Candidato(field.getName(), column.insertable(), true));
            }
        }
        // 2) Atributo homónimo sin @Column(name) (mapeo por defecto de JPA)
        for (Field field : fields) {
            Column column = field.getAnnotation(Column.class);
            if ((column == null || Strings.isNullorEmpty(column.name()))
                    && columnName.equalsIgnoreCase(field.getName())) {
                boolean asociacion = field.getAnnotation(JoinColumn.class) != null
                        || field.getAnnotation(JoinColumns.class) != null;
                agregarCandidato(candidatos,
                        new Candidato(field.getName(),
                                column == null || column.insertable(), !asociacion));
            }
        }
        // 3) Asociación con @JoinColumn(name = columna) o @JoinColumns
        for (Field field : fields) {
            JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
            if (joinColumn != null && columnName.equalsIgnoreCase(joinColumn.name())) {
                agregarCandidato(candidatos,
                        new Candidato(field.getName(), joinColumn.insertable(), true));
                continue;
            }
            JoinColumns joinColumns = field.getAnnotation(JoinColumns.class);
            if (joinColumns != null) {
                for (JoinColumn item : joinColumns.value()) {
                    if (columnName.equalsIgnoreCase(item.name())) {
                        agregarCandidato(candidatos,
                                new Candidato(field.getName(), item.insertable(), true));
                        break;
                    }
                }
            }
        }
        return candidatos;
    }

    /**
     * Agrega un candidato a la lista cuidando de no repetir el mismo campo.
     *
     * @param candidatos lista en construcción.
     * @param nuevo candidato a agregar.
     */
    private static void agregarCandidato(List<Candidato> candidatos, Candidato nuevo) {
        for (int i = 0; i < candidatos.size(); i++) {
            Candidato existente = candidatos.get(i);
            if (!existente.nombre().equals(nuevo.nombre())) {
                continue;
            }
            if (!existente.mapeoExplicito() && nuevo.mapeoExplicito()) {
                candidatos.set(i, new Candidato(existente.nombre(),
                        nuevo.escribible(), true));
            }
            return;
        }
        candidatos.add(nuevo);
    }

    /**
     * Campo que mapea una columna de la base, con lo que D16 necesita saber de
     * él.
     *
     * @param nombre nombre del atributo o de la asociación.
     * @param escribible verdadero si puede escribir la columna
     * ({@code insertable} de su {@code @Column} o {@code @JoinColumn}).
     * @param mapeoExplicito verdadero si la anotación nombra la columna; falso
     * si la coincidencia es solo por el nombre del atributo.
     */
    private record Candidato(String nombre, boolean escribible, boolean mapeoExplicito) {
    }

    /**
     * Devuelve los campos de la clase que pueden estar mapeados a una columna.
     *
     * <p>Son los campos declarados por la clase y por sus superclases hasta
     * {@link DataRow} (exclusive), sin los {@code static}, los
     * {@code transient} ni los anotados {@code @Transient}.</p>
     *
     * @param classType clase de la entidad.
     * @return campos candidatos, de la clase primero y después los heredados.
     */
    private static List<Field> getMappedFields(Class<?> classType) {
        List<Field> fields = new ArrayList<>();
        Class<?> clase = classType;
        while (clase != null && !clase.equals(DataRow.class) && !clase.equals(Object.class)) {
            for (Field field : clase.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())
                        || Modifier.isTransient(field.getModifiers())
                        || field.getAnnotation(Transient.class) != null) {
                    continue;
                }
                fields.add(field);
            }
            clase = clase.getSuperclass();
        }
        return fields;
    }

    /**
     * Devuelve el valor de la clave primaria.
     *
     * @param ejb Objeto row.
     * @return valor de la clave primaria.
     */
    public static Object getIdvalue(IDataRow ejb) {
        try {
            String field = DataInfo.getIdFieldName(ejb.getClass());
            if (field != null) {
                return ejb.getValue(field);
            }
            return null;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    /**
     * Asigna un valor a la clave primaria.
     *
     * @param ejb instancia del objeto row.
     * @param value valor.
     * @return verdadero si tuvo exito o falso si no.
     */
    public static boolean setIdvalue(IDataRow ejb, Object value) {
        try {
            String field = DataInfo.getIdFieldName(ejb.getClass());
            if (field != null) {
                ejb.setValue(field, value);
                return true;
            }
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return false;
    }

    /**
     * Devuelve el valor por defecto de un atributo de la instancia de un objeto
     * row
     *
     * @param <T>
     * @param ejb instancia del objeto DataRow
     * @param fieldname nombre del campo
     * @return valor por defecto oara el campo o atributo dado.
     */
    public static <T extends IDataRow> Object getDefaultValue(T ejb, String fieldname) {
        Object valor = null;
        Class<?> classObj = ejb.getClass();
        try {
            Field field = DataInfo.getDeclaredField(classObj, fieldname + "_default");
            field.setAccessible(true);
            valor = field.get(ejb);
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException | NullPointerException ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return valor;
    }

    /**
     * Asigna el valor por defecto a un atributo de la instancia de un objeto
     * row
     *
     * @param <T>
     * @param ejb instancia del objeto DataRow
     * @param fieldname nombre del campo
     */
    public static <T extends IDataRow> void setDefaultValue(T ejb, String fieldname) {
        Object valor;
        Class<?> classObj = ejb.getClass();
        try {
            Field field = DataInfo.getDeclaredField(classObj, fieldname);
            field.setAccessible(true);
            valor = DataInfo.getDefaultValue(ejb, fieldname);
            if (valor != null) {
                field.set(ejb, valor);
                if (ejb.getAction() == 0) {
                    ejb.setAction(IDataRow.UPDATE);
                }
            }
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
    }

    /**
     * Devuelve el valor de un atributo que hace referencia a un objeto
     * relacionado
     *
     * @param <T>
     * @param ejb Objeto row.
     * @param objrelaName Nombre del atributo.
     * @return el valor de un atributo que hace referencia a un objeto
     * relacionado
     */
    public static <T extends IDataRow> T getObjFk(T ejb, String objrelaName) {
        try {
            Field field;
            try {
                field = DataInfo.getDeclaredField(ejb.getClass(), objrelaName.toLowerCase());
            } catch (Exception ex) {
                field = null;
            }
            if (field == null) {
                field = DataInfo.getDeclaredField(ejb.getClass(), "id" + objrelaName.toLowerCase());
            }
            field.setAccessible(true);
            Object obj = field.get(ejb);
            if (obj instanceof IDataRow) {
                return (T) obj;
            }
            return null;
        } catch (IllegalAccessException | IllegalArgumentException | SecurityException | NullPointerException ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    /**
     * Devuelve un campo o miembro solicitado de una clase sin importar si el
     * nombre esta en mayuscula o minuscula.
     *
     * @param classType clase
     * @param fieldname Nombre del campo o atributo.
     * @return campo o miembro solicitado.
     */
    public static Field getDeclaredField(Class classType, String fieldname) {
        Field field = null;
        try {
            if (fieldname.contains(".")) {
                String className = fieldname.substring(0, Strings.findString(".", fieldname));
                Class classMember = classType.getDeclaredField(className).getType();
                fieldname = fieldname.substring(Strings.findString(".", fieldname) + 1);
                field = getDeclaredField(classMember, fieldname);
            } else {
                field = classType.getDeclaredField(fieldname);
            }
        } catch (NoSuchFieldException | NullPointerException ex) {
            field = null;
        } catch (SecurityException ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        if (field != null) {
            return field;
        }
        Field[] fields = classType.getDeclaredFields();
        for (Field field1 : fields) {
            if (field1.getName().equalsIgnoreCase(fieldname.toLowerCase())) {
                return field1;
            }
        }
        return null;
    }

    /**
     * Devuelve un campo o miembro de una clase buscandolo en toda su jerarquia
     * de herencia sin distinguir mayusculas de minusculas.
     *
     * <p>Se diferencia de {@link #getDeclaredField(Class, String)} en que
     * recorre tambien las superclases: aquel solo mira los campos declarados
     * en la clase recibida. Si hay dos campos cuyos nombres solo difieren en
     * mayusculas gana el que coincide exactamente, en cualquier nivel de la
     * jerarquia; si ninguno coincide exacto se devuelve el primero que
     * coincida sin distinguir mayusculas, empezando por la clase recibida.</p>
     *
     * <p>Admite la notacion con punto para llegar a un campo de un miembro
     * ({@code persona.idpersona}), igual que {@code getDeclaredField}.</p>
     *
     * @param classType clase
     * @param fieldname nombre del campo o atributo.
     * @return campo o miembro solicitado, nulo si no existe.
     */
    public static Field getDeclaredFieldIgnoreCase(Class classType, String fieldname) {
        if (classType == null || fieldname == null) {
            return null;
        }
        if (fieldname.contains(".")) {
            String memberName = fieldname.substring(0, Strings.findString(".", fieldname));
            Field member = getDeclaredFieldIgnoreCase(classType, memberName);
            if (member == null) {
                return null;
            }
            return getDeclaredFieldIgnoreCase(member.getType(),
                    fieldname.substring(Strings.findString(".", fieldname) + 1));
        }
        Field found = null;
        Class classSearch = classType;
        while (classSearch != null && !Object.class.equals(classSearch)) {
            for (Field field : getDeclaredFields(classSearch)) {
                if (field.getName().equals(fieldname)) {
                    //El nombre exacto gana sobre cualquier coincidencia anterior
                    return field;
                }
                if (found == null && field.getName().equalsIgnoreCase(fieldname)) {
                    found = field;
                }
            }
            classSearch = classSearch.getSuperclass();
        }
        return found;
    }

    /**
     * Devuelve la lista de campos/atributos de una clase
     *
     * @param classType clase DataRow
     * @return lista de campos
     */
    public static Field[] getDeclaredFields(Class classType) {
        Field[] fields;
        try {
            fields = classType.getDeclaredFields();
        } catch (SecurityException ex) {
            return new Field[0];
        }
        return fields;
    }

    /**
     * Devuelve un metodo solicitado de una clase sin importar si el nombre esta
     * en mayuscula o minuscula.
     *
     * <p>El nombre <b>exacto</b> tiene prioridad: primero se busca el metodo
     * que se llame igual y no reciba argumentos (el caso de un lector
     * {@code get}) y, si no existe, el que se llame igual y reciba uno (el de
     * un modificador {@code set}); recien entonces se acepta una coincidencia
     * que no distinga mayusculas, como se hacia siempre.</p>
     *
     * <p>Sin esa prioridad el metodo quedaba a merced del orden en que
     * {@code getMethods()} devuelve los metodos, que la maquina virtual no
     * especifica: en una clase con dos atributos que solo difieren en la caja
     * ({@code idcliente} e {@code idCliente}) el valor podia terminar
     * asignandose al atributo equivocado y sin error (plan {@code XMLMAP},
     * hallazgo {@code M3-01}).</p>
     *
     * @param classType clase
     * @param methodName Nombre del metodo
     * @return objeto metodo
     */
    public static Method getMethod(Class classType, String methodName) {
        Method method = null;
        try {
            method = classType.getMethod(methodName);
        } catch (NoSuchMethodException | NullPointerException ex) {
            method = null;
        } catch (SecurityException ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        if (method != null) {
            return method;
        }
        if (classType == null || methodName == null) {
            return null;
        }
        Method[] methods = classType.getMethods();
        // Nombre exacto. El de aridad 0 ya se busco arriba, asi que entre las
        // sobrecargas gana el de un solo argumento (un modificador "set").
        Method exact = null;
        for (Method method1 : methods) {
            if (method1.getName().equals(methodName)) {
                if (method1.getParameterCount() == 1) {
                    return method1;
                }
                if (exact == null) {
                    exact = method1;
                }
            }
        }
        if (exact != null) {
            return exact;
        }
        // Sin coincidencia exacta se acepta la que no distingue mayusculas
        for (Method method1 : methods) {
            if (method1.getName().equalsIgnoreCase(methodName.toLowerCase())) {
                return method1;
            }
        }
        return null;
    }

    /**
     * Devuelve el tipo de dato de un campo o atributo solicitado
     *
     * @param classType clase DataRow
     * @param fieldname nombre del campo
     * @return tipo de dato de un campo o atributo solicitado
     */
    public static Class getFieldType(Class classType, String fieldname) {
        Class cls = null;
        try {
            if (fieldname.contains(".")) {
                String className = fieldname.substring(0, Strings.findString(".", fieldname));
                Class classMember = classType.getDeclaredField(className).getType();
                fieldname = fieldname.substring(Strings.findString(".", fieldname) + 1);
                cls = getFieldType(classMember, fieldname);
            } else {
                cls = getDeclaredField(classType, fieldname).getType();
            }
        } catch (NoSuchFieldException | NullPointerException ex) {
            cls = null;
        } catch (SecurityException ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return cls;
    }

    /**
     * Devuelve el tipo de dato que devuelve un metodo
     *
     * @param classType clase DataRow
     * @param methodname nombre del metodo
     * @return tipo de dato que devuelve un metodo
     */
    public static Class getMethodReturnType(Class classType, String methodname) {
        return getMethodReturnType(classType, methodname, false);
    }

    /**
     * Devuelve el tipo de dato que devuelve un metodo
     *
     * @param classType clase DataRow
     * @param methodname nombre del metodo
     * @param isGetter se agrega el prefijo "get" al metodo
     * @return tipo de dato que devuelve un metodo
     */
    public static Class getMethodReturnType(Class classType, String methodname, Boolean isGetter) {
        Class cls = null;
        try {
            if (methodname.contains(".")) {
                String memberName = methodname.substring(0, Strings.findString(".", methodname));
                methodname = methodname.substring(Strings.findString(".", methodname) + 1);
                Class classMember;
                Field field = getDeclaredField(classType, memberName);
                if (field != null) {
                    classMember = field.getType();
                    cls = getMethodReturnType(classMember, methodname, isGetter);
                } else {
                    if (!"get".equals(Strings.left(memberName, 3).toLowerCase())) {
                        memberName = "get" + Strings.capitalize(memberName);
                    }
                    Method method = getMethod(classType, memberName);
                    classMember = method.getReturnType();
                    cls = getMethodReturnType(classMember, methodname, isGetter);
                }
            } else {
                if (!"get".equals(Strings.left(methodname, 3).toLowerCase())) {
                    methodname = "get" + Strings.capitalize(methodname);
                }
                cls = classType.getMethod(methodname).getReturnType();
            }
        } catch (NoSuchMethodException | NullPointerException ex) {
            cls = null;
        } catch (SecurityException ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return cls;
    }

    /**
     * Devuelve el valor de un campo o atributo
     *
     * @param ejb objeto DataRow
     * @param fieldname nombre del campo
     * @return valor del campo solicitado.
     */
    public static Object getFieldValue(Object ejb, String fieldname) {
        Object value = null;
        try {
            if (ejb == null) {
                return null;
            }
            // Si contiene un punto significa que el campo esta en uno de sus miembros
            if (fieldname.contains(".")) {
                String memberName = fieldname.substring(0, Strings.findString(".", fieldname));
                fieldname = fieldname.substring(Strings.findString(".", fieldname) + 1);

                Field field = getDeclaredField(ejb.getClass(), memberName);
                if (field != null) {
                    field.setAccessible(true);
                    Object parentValue = field.get(ejb);
                    value = getFieldValue(parentValue, fieldname);
                } else {
                    // Si no hay atributos con el nombre solicitado buscar como metodo
                    Method method = getMethod(ejb.getClass(), "get" + Strings.capitalize(memberName));
                    if (method != null) {
                        method.setAccessible(true);
                        Object parentValue = method.invoke(ejb);
                        value = getFieldValue(parentValue, fieldname);
                    }
                }
            } else {
                // Buscar entre los atributos si coincide el nombre
                Field field = getDeclaredField(ejb.getClass(), fieldname);
                if (field != null) {
                    field.setAccessible(true);
                    value = field.get(ejb);
                } else {
                    // Si no hay atributos con el nombre solicitado buscar como metodo
                    Method method = getMethod(ejb.getClass(), "get" + Strings.capitalize(fieldname));
                    if (method != null) {
                        method.setAccessible(true);
                        value = method.invoke(ejb);
                    }
                }
            }
        } catch (Exception ex) {
            return null;
        }
        return value;
    }

    /**
     * Setea un valor a un miembro de un objeto
     *
     * @param ejb objeto DataRow
     * @param fieldname nombre del campo
     * @param value
     * @return verdadero o falso si tuvo exito o no
     */
    public static Boolean setFieldValue(Object ejb, String fieldname, Object value) {
        Boolean exito = true;
        try {
            // Si contiene un punto significa que el campo esta en uno de sus miembros
            if (fieldname.contains(".")) {
                String memberName = fieldname.substring(0, Strings.findString(".", fieldname));
                fieldname = fieldname.substring(Strings.findString(".", fieldname) + 1);

                Field field = getDeclaredField(ejb.getClass(), memberName);
                if (field != null) {
                    field.setAccessible(true);
                    Object parentValue = field.get(ejb);
                    exito = setFieldValue(parentValue, fieldname, value);
                } else {
                    // Si no hay atributos con el nombre solicitado buscar como metodo
                    Method method = getMethod(ejb.getClass(), "get" + Strings.capitalize(memberName));
                    if (method != null) {
                        method.setAccessible(true);
                        Object parentValue = method.invoke(ejb);
                        exito = setFieldValue(parentValue, fieldname, value);
                    }
                }
            } else {
                exito = false;
                // Si no hay atributos con el nombre solicitado buscar como metodo
                Method method = getMethod(ejb.getClass(), "set" + Strings.capitalize(fieldname));
                if (method != null) {
                    method.setAccessible(true);
                    method.invoke(ejb, value);
                    exito = true;
                } else {
                    // Buscar entre los atributos si coincide el nombre
                    Field field = getDeclaredField(ejb.getClass(), fieldname);
                    if (field != null) {
                        field.setAccessible(true);
                        field.set(ejb, value);
                        exito = true;
                    }
                }
            }
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
            return false;
        }
        return exito;
    }
}
