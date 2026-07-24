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


import java.io.Serializable;
import org.javabeanstack.error.IErrorReg;
import java.util.Map;

/**
 * Contrato del registro base de todas las entidades (modelos EJB) mapeadas a
 * tablas o vistas de la base de datos.
 *
 * <p>Define el comportamiento común que la capa de datos ({@link IGenericDAO},
 * {@link IDataService}) espera de cada entidad: el seguimiento de la operación
 * CRUD a realizar ({@link #INSERT}, {@link #UPDATE}, {@link #DELETE}), la
 * conservación de los valores previos de los campos (para detectar cambios y
 * auditar), el registro de errores/advertencias de validación por campo, el
 * acceso genérico a los valores por nombre de campo (vía reflexión) y el
 * soporte de firma digital de la fila.</p>
 *
 * <p>La implementación de referencia es {@link org.javabeanstack.data.DataRow};
 * las entidades de aplicación la extienden en lugar de implementar directamente
 * esta interfaz.</p>
 *
 * @author Jorge Enciso
 */
public interface IDataRow extends Serializable{
    /** Modo de persistencia: la entidad se insertará ({@code persist}). */
    public static final int PERSIST   = 1;
    /** Modo de persistencia: la entidad se combinará con la existente ({@code merge}). */
    public static final int MERGE     = 2;

    /** Operación: agregar (insertar) el registro. Equivale a {@link #INSERT}. */
    public static final int AGREGAR   = 1;
    /** Operación: modificar (actualizar) el registro. Equivale a {@link #UPDATE}. */
    public static final int MODIFICAR = 2;
    /** Operación: borrar (eliminar) el registro. Equivale a {@link #DELETE}. */
    public static final int BORRAR    = 3;
    /** Operación: solo consulta, sin cambios a persistir. Equivale a {@link #READ}. */
    public static final int CONSULTAR = -1;
    /** Operación: confirmar el registro. Equivale a {@link #CONFIRM}. */
    public static final int CONFIRMAR  = 5;
    /** Operación: imprimir el registro. Equivale a {@link #PRINT}. */
    public static final int IMPRIMIR = -5;

    /** Operación: insertar el registro (alias en inglés de {@link #AGREGAR}). */
    public static final int INSERT   = 1;
    /** Operación: actualizar el registro (alias en inglés de {@link #MODIFICAR}). */
    public static final int UPDATE   = 2;
    /** Operación: eliminar el registro (alias en inglés de {@link #BORRAR}). */
    public static final int DELETE   = 3;
    /** Operación: solo lectura (alias en inglés de {@link #CONSULTAR}). */
    public static final int READ     = -1;
    /** Operación: confirmar (alias en inglés de {@link #CONFIRMAR}). */
    public static final int CONFIRM  = 5;
    /** Operación: imprimir (alias en inglés de {@link #IMPRIMIR}). */
    public static final int PRINT     = -5;

    /**
     * Devuelve una copia superficial del registro.
     *
     * @return copia del registro.
     */
    public Object    clone();

    /**
     * Devuelve la operación CRUD pendiente sobre el registro.
     *
     * @return código de operación ({@link #INSERT}, {@link #UPDATE},
     * {@link #DELETE}, {@link #READ}...); {@code 0} si no se asignó ninguna.
     */
    public int       getAction();

    /**
     * Devuelve la sentencia (condición) que localiza un registro por su clave
     * única, construida a partir de los campos que la componen.
     *
     * @return condición de la clave única.
     */
    public String    getQueryUK();

    /**
     * Devuelve el identificador de la función de búsqueda asociada al registro.
     *
     * @return identificador de la función de búsqueda.
     */
    public String    getIdFunctionFind();

    /**
     * Indica si el registro es auditable (si sus cambios deben registrarse en
     * la tabla de auditoría).
     *
     * @return verdadero si es auditable, falso si no.
     */
    public boolean   isAuditAble();

    /**
     * Indica si el registro completo fue verificado (validado) correctamente.
     *
     * @return verdadero si fue verificado, falso si no.
     */
    public boolean   isRowChecked();

    /**
     * Asigna el estado de verificación del registro completo.
     *
     * @param rowchecked verdadero si el registro fue verificado.
     */
    public void      setRowChecked(boolean rowchecked);

    /**
     * Indica si un campo específico fue verificado. Si no hay información de
     * verificación por campo, se considera verificado.
     *
     * @param fieldName nombre del campo.
     * @return verdadero si el campo fue verificado, falso si no.
     */
    public boolean   isFieldChecked(String fieldName);

    /**
     * Devuelve el mapa con el estado de verificación de cada campo.
     *
     * @return mapa campo → verificado.
     */
    public Map<String, Boolean> getFieldsChecked();

    /**
     * Asigna el mapa con el estado de verificación de cada campo.
     *
     * @param fieldsChecked mapa campo → verificado.
     */
    public void      setFieldsChecked(Map fieldsChecked);

    /**
     * Asigna el estado de verificación de un campo. Si el campo pasa a no
     * verificado, el registro completo queda marcado como no verificado.
     *
     * @param fieldName nombre del campo.
     * @param fieldChecked verdadero si el campo fue verificado.
     */
    public void      setFieldChecked(String fieldName, boolean fieldChecked);

    /**
     * Devuelve los errores de validación registrados por campo (excluye las
     * advertencias).
     *
     * @return mapa campo → error.
     */
    public Map<String, IErrorReg> getErrors();

    /**
     * Devuelve las advertencias de validación registradas por campo.
     *
     * @return mapa campo → advertencia.
     */
    public Map<String, IErrorReg> getWarnings();

    /**
     * Devuelve el valor identificador (clave primaria) del registro.
     *
     * @return identificador del registro.
     */
    public Object    getId();

    /**
     * Devuelve el identificador alternativo del registro (usado cuando aún no
     * tiene identidad persistente, p. ej. el hash de identidad del objeto).
     *
     * @return identificador alternativo.
     */
    public Object    getIdAlternative();

    /**
     * Devuelve una clave de fila legible que combina el tipo y el identificador
     * del registro; útil como {@code rowKey} en las tablas de la vista.
     *
     * @return clave de fila.
     */
    public String    getRowkey();

    /**
     * Devuelve el valor de un campo o atributo por su nombre (vía reflexión).
     *
     * @param fieldname nombre del campo o atributo.
     * @return valor del campo.
     */
    public Object    getValue(String fieldname);

    /**
     * Devuelve el valor original de un campo antes de la primera modificación
     * de la operación en curso.
     *
     * @param fieldname nombre del campo.
     * @return valor original del campo.
     */
    public Object    getOldValue(String fieldname);

    /**
     * Devuelve el valor inmediatamente anterior de un campo (el previo a la
     * última asignación).
     *
     * @param fieldname nombre del campo.
     * @return valor anterior del campo.
     */
    public Object    getBeforeValue(String fieldname);

    /**
     * Devuelve el tipo de dato de un campo o atributo.
     *
     * @param fieldname nombre del campo.
     * @return clase del tipo de dato del campo.
     */
    public Class     getFieldType(String fieldname);

    /**
     * Devuelve el texto concatenado de todos los mensajes de error (no
     * advertencias) registrados en el registro.
     *
     * @return mensajes de error, separados por salto de línea.
     */
    public String    getErrorMsg();

    /**
     * Asigna el valor identificador (clave primaria) del registro.
     *
     * @param id valor identificador.
     */
    public void      setId(Object id);

    /**
     * Asigna el identificador alternativo del registro.
     *
     * @param id identificador alternativo.
     */
    public void      setIdAlternative(Object id);

    /**
     * Asigna el valor de un campo o atributo (vía reflexión). Conserva el valor
     * anterior y, si el registro no tenía operación asignada, lo marca como
     * {@link #UPDATE}.
     *
     * @param fieldname nombre del campo o atributo.
     * @param value valor a asignar.
     * @throws Exception si el campo no existe o el tipo del valor es incorrecto.
     */
    public void      setValue(String fieldname, Object value) throws Exception;

    /**
     * Toma una instantánea de los valores actuales del registro como valores
     * originales, para poder detectar cambios posteriores.
     */
    public void      setOldValues();

    /**
     * Asigna la operación CRUD a realizar sobre el registro. Al pasar a una
     * operación de modificación conserva los valores originales.
     *
     * @param action código de operación ({@link #INSERT}, {@link #UPDATE},
     * {@link #DELETE}...).
     */
    public void      setAction(int action);

    /**
     * Reemplaza el mapa completo de errores por campo.
     *
     * @param errorReg mapa campo → error.
     */
    public void      setErrors(Map<String, IErrorReg> errorReg);

    /**
     * Agrega un error asociado a un campo.
     *
     * @param errorReg objeto con los datos del error.
     * @param fieldName campo sobre el cual se generó el error.
     */
    public void      setErrors(IErrorReg errorReg, String fieldName);

    /**
     * Agrega un error a un campo a partir de sus datos.
     *
     * @param errorMsg mensaje de error.
     * @param fieldname campo sobre el cual ocurrió el error.
     * @param errorNumber número de error.
     */
    public void      setErrors(String errorMsg, String fieldname, int errorNumber);

    /**
     * Asigna si el registro es auditable.
     *
     * @param auditable verdadero si el registro debe auditarse.
     */
    public void      setAuditAble(boolean auditable);

    /**
     * Devuelve la clase de la entidad de auditoría asociada al registro.
     *
     * @return clase de auditoría, o {@code null} si no aplica.
     */
    public Class<? extends IDataRow> getAuditClass();

    /**
     * Asigna la clase de la entidad de auditoría asociada al registro.
     *
     * @param clazz clase de auditoría.
     */
    public void setAuditClass(Class<? extends IDataRow> clazz);

    /**
     * Marca el registro para ser borrado (asigna la operación {@link #DELETE}).
     *
     * @return verdadero.
     */
    public boolean   delete();

    /**
     * Determina si este registro es equivalente a otro según un subconjunto de
     * atributos definido por la entidad. Debe redefinirse en las clases
     * derivadas que lo requieran.
     *
     * @param o objeto a comparar.
     * @return verdadero si son equivalentes, falso si no.
     */
    public boolean   equivalent(Object o);

    /**
     * Indica si sobre este registro debe aplicarse el filtro por defecto de la
     * capa de datos (filtro por empresa/permisos).
     *
     * @return verdadero si se aplica el filtro por defecto, falso si no.
     */
    public boolean   isApplyDBFilter();

    /**
     * Verifica que el registro pertenezca a la empresa indicada (cuando la
     * entidad tiene el campo {@code idcompany}/{@code idempresa}).
     *
     * @param idcompany identificador de la empresa.
     * @return verdadero si la validación es correcta o no aplica, falso si no.
     */
    public boolean   checkFieldIdcompany(Long idcompany);

    /**
     * Punto de extensión ejecutado al leer un campo (getter). Por defecto no
     * hace nada; se redefine en las clases derivadas.
     *
     * @param fieldName nombre del campo.
     */
    public void onGetter(String fieldName);

    /**
     * Punto de extensión ejecutado tras asignar un campo (setter). Conserva el
     * valor anterior; se redefine en las clases derivadas.
     *
     * @param fieldName nombre del campo.
     * @param fieldValueOld valor anterior.
     * @param fieldValueNew valor nuevo.
     */
    public void onSetter(String fieldName, Object fieldValueOld, Object fieldValueNew);

    /**
     * Asigna valores por defecto a los campos no nulos ({@code @NotNull}) que
     * tengan valor nulo (0, {@code false}, cadena vacía según el tipo).
     *
     * @throws Exception si falla la asignación de algún valor por defecto.
     */
    public void setDefaults() throws Exception;

    /**
     * Devuelve el modo de persistencia del registro.
     *
     * @return {@link #PERSIST} o {@link #MERGE}.
     */
    public int getPersistMode();

    /**
     * Asigna el modo de persistencia del registro.
     *
     * @param persistMode {@link #PERSIST} o {@link #MERGE}.
     */
    public void setPersistMode(int persistMode);

    /**
     * Indica si el punto de extensión {@link #onGetter(String)} está activo.
     *
     * @return verdadero si está activo.
     */
    public Boolean getOnGetterActivated();

    /**
     * Indica si el punto de extensión {@link #onSetter(String, Object, Object)}
     * está activo.
     *
     * @return verdadero si está activo.
     */
    public Boolean getOnSetterActivated();

    /**
     * Activa o desactiva el punto de extensión {@link #onGetter(String)}.
     *
     * @param onGetter verdadero para activarlo.
     */
    public void setOnGetterActivated(boolean onGetter);

    /**
     * Activa o desactiva el punto de extensión
     * {@link #onSetter(String, Object, Object)}.
     *
     * @param onSetter verdadero para activarlo.
     */
    public void setOnSetterActivated(boolean onSetter);

    /**
     * Copia los valores de los campos de este registro al registro destino.
     *
     * @param <X> tipo del registro destino.
     * @param target registro destino.
     * @return el registro destino con los valores copiados.
     * @throws Exception si falla la asignación de algún campo.
     */
    public <X extends IDataRow> X copyTo(X target) throws Exception;

    /**
     * Copia los valores de los campos de este registro al registro destino,
     * opcionalmente solo los campos no nulos.
     *
     * @param <X> tipo del registro destino.
     * @param target registro destino.
     * @param onlyFieldsNotNulls verdadero para copiar solo los campos no nulos.
     * @return el registro destino con los valores copiados.
     * @throws Exception si falla la asignación de algún campo.
     */
    public <X extends IDataRow> X copyTo(X target, boolean onlyFieldsNotNulls) throws Exception;

    /**
     * Copia los valores de los campos del registro origen a este registro.
     *
     * @param <X> tipo del registro origen.
     * @param source registro origen.
     */
    public <X extends IDataRow> void copyFrom(X source);

    /**
     * Copia los valores de los campos del registro origen a este registro,
     * opcionalmente solo los campos no nulos.
     *
     * @param <X> tipo del registro origen.
     * @param source registro origen.
     * @param onlyFieldsNotNulls verdadero para copiar solo los campos no nulos.
     * @throws Exception si falla la asignación de algún campo.
     */
    public <X extends IDataRow> void copyFrom(X source, boolean onlyFieldsNotNulls) throws Exception;

    /**
     * Devuelve el nombre de la entidad de auditoría declarado en la anotación
     * {@code @AuditEntity} de la clase.
     *
     * @return nombre de la entidad de auditoría, o {@code null} si no está anotada.
     */
    public String getAuditEntity();

    /**
     * Devuelve el mapa de propiedades libres asociadas al registro.
     *
     * @return mapa de propiedades.
     */
    public Map<String, Object> getProperties();

    /**
     * Devuelve el valor de una propiedad libre por su clave.
     *
     * @param key clave de la propiedad.
     * @return valor de la propiedad, o {@code null} si no existe.
     */
    public Object getProperty(String key);

    /**
     * Reemplaza el mapa de propiedades libres del registro.
     *
     * @param properties mapa de propiedades.
     */
    public void setProperties(Map<String, Object> properties);

    /**
     * Agrega o reemplaza una propiedad libre.
     *
     * @param key clave de la propiedad.
     * @param value valor de la propiedad.
     */
    public void addProperty(String key, Object value);

    /**
     * Agrega en bloque un conjunto de propiedades libres.
     *
     * @param properties mapa de propiedades a agregar.
     */
    public void addProperties(Map<String, Object> properties);

    /**
     * Devuelve el texto a firmar, compuesto por los campos anotados con
     * {@code @SignatureField} y la firma actual.
     *
     * @return texto a firmar.
     */
    public String getTextToSign();

    /**
     * Devuelve la firma digital actual del registro.
     *
     * @return firma del registro ({@code "SINFIRMA"} si no fue firmado).
     */
    public String getSignature();

    /**
     * Asigna la firma digital del registro.
     *
     * @param signature firma a asignar.
     */
    public void setSignature(String signature);
}
