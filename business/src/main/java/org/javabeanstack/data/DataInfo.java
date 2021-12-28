/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 Jorge Enciso
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
import java.util.ArrayList;
import java.util.List;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.apache.log4j.Logger;

import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.util.Strings;

/**
 * Esta clase contiene metodos que brindan información sobre los modelos ejb ej.
 * si un campo es primarykey,
 *
 * @author Jorge Enciso
 */
public class DataInfo {

    private static final Logger LOGGER = Logger.getLogger(DataInfo.class);

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
                if (field.equalsIgnoreCase(fieldname.toLowerCase())) {
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
            String[] uniqueList = DataInfo.getUniqueFields(ejb.getClass());
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
        Method[] methods = classType.getMethods();
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
            if (ejb == null){
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
                // Buscar entre los atributos si coincide el nombre
                Field field = getDeclaredField(ejb.getClass(), fieldname);
                if (field != null) {
                    field.setAccessible(true);
                    field.set(ejb,value);
                    exito = true;
                } else {
                    // Si no hay atributos con el nombre solicitado buscar como metodo
                    Method method = getMethod(ejb.getClass(), "set" + Strings.capitalize(fieldname));
                    if (method != null) {
                        method.setAccessible(true);
                        method.invoke(ejb,value);
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
