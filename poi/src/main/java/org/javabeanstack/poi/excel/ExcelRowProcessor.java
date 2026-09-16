/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2018 - 2027 Jorge Enciso
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.javabeanstack.data.DataInfo;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.util.Fn;

/**
 * Procesa una fila de una planilla Excel y la transforma en un objeto de
 * dominio de tipo {@code T}.
 * <p>
 * Esta clase NO mantiene una instancia del objeto destino: solo conserva su
 * {@link Class} en {@link #targetType}. Es la subclase, dentro de
 * {@link #process()}, la responsable de instanciar el objeto destino a partir
 * de {@link #targetType} y de asignarle los valores leídos de la fila.
 * <p>
 * El mapeo entre las columnas del Excel y los atributos del objeto destino se
 * define mediante {@code headToField}, donde:
 * <ul>
 * <li>clave = encabezado de la columna en el Excel</li>
 * <li>valor = nombre del atributo en el objeto destino</li>
 * </ul>
 * Si {@code headToField} es {@code null} o está vacío, se genera uno donde el
 * nombre del atributo coincide exactamente con el encabezado de columna (es
 * decir, se asume que los encabezados del Excel ya tienen el mismo nombre que
 * los atributos de {@code T}).
 * <p>
 * Ese mapeo es el atajo del caso simple. La fuente de verdad interna es
 * {@link ExcelColumns}: una colección ordenada de {@link ExcelColumnSpec} que
 * además de la equivalencia cabecera → atributo declara por columna el nivel
 * de exigencia ({@link ColumnRequirement}), un valor por defecto (fijo o
 * calculado por fila) y una transformación previa del valor de la celda. Los
 * constructores que reciben un {@code Map<String, String>} construyen columnas
 * {@link ColumnRequirement#OPTIONAL} sin valor por defecto, que es el
 * comportamiento histórico; los que reciben {@link ExcelColumns} habilitan la
 * validación de columnas obligatorias en {@link #checkMetaData()} y la
 * aplicación de valores por defecto en {@link #process()}.
 * <p>
 * Las subclases solo necesitan implementar {@link #process()}, leyendo los
 * valores con {@link #valueOfField(String)} o {@link #valueOfColumn(String)}, y
 * pueden validar la compatibilidad de tipos vía {@link #checkMetaData()} (que
 * delega en {@link ExcelUtil#getAssignableTypeError(Cell, Class, String)}) antes
 * de asignarlos a la instancia que ellas mismas creen a partir de
 * {@link #targetType}.
 *
 * @param <T> tipo del objeto destino
 */
public abstract class ExcelRowProcessor<T extends IDataRow> implements IExcelRowProcessor<T> {

    /**
     * Clave de {@link IDataRow#getProperties()} de la fila procesada bajo la
     * que se guarda el conjunto ({@code Set<String>}) de atributos que se
     * completaron con el valor por defecto de una columna <b>ausente</b> en la
     * planilla. La importación lo usa para no pisar con esos valores un
     * registro que ya existe (decisión del usuario, 2026-09-16: si el
     * registro existe y la columna no vino, se conserva lo que hay).
     */
    public static final String DEFAULTED_FIELDS = "EXCEL_DEFAULTED_FIELDS";

    /**
     * Clave de {@link IDataRow#getProperties()} de la fila procesada bajo la
     * que se guarda el conjunto ({@code Set<String>}) de atributos cuyas
     * columnas se declararon con {@link ExcelColumnSpec.Builder#noOverwrite()}:
     * la importación no los copia sobre un registro que ya existe (el valor se
     * graba solo en las altas).
     */
    public static final String NO_OVERWRITE_FIELDS = "EXCEL_NO_OVERWRITE_FIELDS";

    /**
     * La fila de Excel que se está procesando. Puede reasignarse con
     * {@link #setRow(Row)}.
     */
    private Row row;

    /**
     * La clase del objeto destino. {@link #process()} la usa para instanciar el
     * objeto destino que será completado con los valores de la fila.
     */
    protected final Class<T> targetType;

    /**
     * Especificación de las columnas de la planilla (cabecera, atributo
     * destino, obligatoriedad, valor por defecto y transformación). Es la
     * fuente de verdad del procesador: {@link #headToField} y
     * {@link #fieldToHead} se derivan de ella.
     */
    private final ExcelColumns columns;

    /**
     * Mapeo de encabezado de columna del Excel a nombre del atributo en target.
     * Ejemplo: {"Tax ID" -> "taxId"}. Derivado de {@link #columns}.
     */
    private final Map<String, String> headToField;

    /**
     * Inverso de {@link #headToField}: nombre del atributo en target a
     * encabezado de columna del Excel. Ejemplo: {"taxId" -> "Tax ID"}.
     */
    private final Map<String, String> fieldToHead;

    /**
     * Encabezado de columna del Excel a su índice de columna dentro de la
     * planilla (base 0). Ejemplo: {"Tax ID" -> 2}.
     */
    private final Map<String, Integer> headToIndex;

    /**
     * Propiedades de configuración del procesador (por ejemplo,
     * {@code allowFieldNotExist}). Puede ser {@code null}; se accede de forma
     * segura vía {@link #getProperties()}.
     */
    private Map<String, Object> properties = new HashMap();

    /**
     * Índice (base 0) de la fila que contiene los encabezados de columna dentro
     * de la planilla.
     */
    private Integer headerRowIndex;

    /**
     * Evaluador de fórmulas, creado de forma perezosa la primera vez que se
     * encuentra una celda de tipo fórmula. Se cachea para reutilizarlo entre
     * celdas y filas de la misma planilla.
     */
    private FormulaEvaluator formulaEvaluator;

    /**
     * Crea un procesador que genera automáticamente {@link #fieldToHead} y
     * {@link #headToIndex} a partir de {@code headToField}, asumiendo que los
     * encabezados de columna están ubicados en la fila 0 de la planilla.
     *
     * @param row la fila de Excel desde donde se leerán los valores
     * @param targetType la clase del objeto destino a instanciar en {@link #process()}
     * @param headToField mapeo de encabezado de columna del Excel a nombre del
     * atributo en targetType; si es {@code null} o está vacío, se genera uno
     * asumiendo que cada atributo se llama igual que su encabezado de columna
     * @param properties propiedades de configuración del procesador (por
     * ejemplo, {@code allowFieldNotExist}); puede ser {@code null}
     */
    protected ExcelRowProcessor(Row row, Class<T> targetType, Map<String, String> headToField, Map<String, Object> properties) {
        this(row, targetType, headToField, 0, properties);
    }

    /**
     * Crea un procesador que genera automáticamente {@link #fieldToHead} y
     * {@link #headToIndex} a partir de {@code headToField}.
     * <p>
     * <b>Nota de implementación:</b> convive con
     * {@link #ExcelRowProcessor(Row, Class, ExcelColumns, int, Map)}, que tiene
     * la misma cantidad de argumentos: invocarlo con {@code null} literal en el
     * tercer argumento es <b>ambiguo</b> para el compilador. Para pedir el
     * mapeo identidad (cabecera == atributo) hay que decir de qué constructor
     * se trata: {@code (Map<String, String>) null} para este, o
     * {@code ExcelColumns.of(null)} para el otro (decisión {@code D10} del plan
     * {@code XLSCOL}: la API no cambia, se documenta).
     *
     * @param row la fila de Excel desde donde se leerán los valores
     * @param targetType la clase del objeto destino a instanciar en {@link #process()}
     * @param headToField mapeo de encabezado de columna del Excel a nombre del
     * atributo en targetType; si es {@code null} o está vacío, se genera uno
     * asumiendo que cada atributo se llama igual que su encabezado de columna
     * @param headerRowIndex índice (base 0) de la fila que contiene los
     * encabezados de columna dentro de la planilla
     * @param properties propiedades de configuración del procesador (por
     * ejemplo, {@code allowFieldNotExist}); puede ser {@code null}
     */
    protected ExcelRowProcessor(Row row, Class<T> targetType,
            Map<String, String> headToField,
            int headerRowIndex,
            Map<String, Object> properties) {
        this(row, targetType, ExcelColumns.of(headToField), headerRowIndex, null, null, properties);
    }

    /**
     * Crea un procesador a partir de la especificación completa de las
     * columnas, asumiendo que los encabezados están en la fila 0 de la
     * planilla y sin propiedades de configuración. Es la variante que habilita
     * columnas obligatorias, valores por defecto y transformaciones por
     * columna.
     * <p>
     * No existe la variante de <b>cuatro</b> argumentos
     * {@code (Row, Class, ExcelColumns, Map)} a propósito: sería ambigua con
     * {@link #ExcelRowProcessor(Row, Class, Map, Map)} para quien invoque el
     * constructor con {@code null} literal en el tercer argumento. Para pasar
     * propiedades usar
     * {@link #ExcelRowProcessor(Row, Class, ExcelColumns, int, Map)} con
     * {@code headerRowIndex} 0; esa variante de <b>cinco</b> argumentos sí
     * convive con la histórica {@code (Row, Class, Map, int, Map)} y con
     * {@code null} literal en el tercer argumento la llamada es ambigua (ver su
     * nota de implementación).
     *
     * @param row la fila de Excel desde donde se leerán los valores
     * @param targetType la clase del objeto destino a instanciar en {@link #process()}
     * @param columns especificación de las columnas de la planilla; si es
     * {@code null} o está vacía, se genera una identidad
     * ({@code cabecera == atributo}, todas opcionales) a partir de los
     * encabezados de la planilla
     */
    protected ExcelRowProcessor(Row row, Class<T> targetType,
            ExcelColumns columns) {
        this(row, targetType, columns, 0, null);
    }

    /**
     * Crea un procesador a partir de la especificación completa de las
     * columnas, indicando en qué fila de la planilla están los encabezados.
     * <p>
     * <b>Nota de implementación:</b> convive con la variante histórica
     * {@link #ExcelRowProcessor(Row, Class, Map, int, Map)}, que tiene la misma
     * cantidad de argumentos: invocarlo con {@code null} literal en el tercer
     * argumento es <b>ambiguo</b> para el compilador. Para pedir el mapeo
     * identidad (cabecera == atributo) hay que decir de qué constructor se
     * trata: {@code ExcelColumns.of(null)} para este, o
     * {@code (Map<String, String>) null} para el otro (decisión {@code D10} del
     * plan {@code XLSCOL}: la API no cambia, se documenta).
     *
     * @param row la fila de Excel desde donde se leerán los valores
     * @param targetType la clase del objeto destino a instanciar en {@link #process()}
     * @param columns especificación de las columnas de la planilla; si es
     * {@code null} o está vacía, se genera una identidad
     * ({@code cabecera == atributo}, todas opcionales) a partir de los
     * encabezados de la planilla
     * @param headerRowIndex índice (base 0) de la fila que contiene los
     * encabezados de columna dentro de la planilla
     * @param properties propiedades de configuración del procesador (por
     * ejemplo, {@code allowFieldNotExist}); puede ser {@code null}
     */
    protected ExcelRowProcessor(Row row, Class<T> targetType,
            ExcelColumns columns,
            int headerRowIndex,
            Map<String, Object> properties) {
        this(row, targetType, columns, headerRowIndex, null, null, properties);
    }

    /**
     * Crea un procesador que acepta los mapas {@code fieldToHead} y
     * {@code headToIndex} ya construidos, útil para evitar recalcularlos en
     * cada fila de la misma planilla. Si alguno de los dos se pasa como
     * {@code null}, se genera automáticamente (mismo comportamiento que los
     * constructores más simples).
     *
     * @param row la fila de Excel desde donde se leerán los valores
     * @param targetType la clase del objeto destino a instanciar en {@link #process()}
     * @param headToField mapeo de encabezado de columna del Excel a nombre del
     * atributo en targetType; si es {@code null} o está vacío, se genera uno
     * asumiendo que cada atributo se llama igual que su encabezado de columna
     * @param headerRowIndex índice (base 0) de la fila que contiene los
     * encabezados de columna dentro de la planilla; solo se usa si
     * {@code headToIndex} es {@code null} y necesita generarse
     * @param fieldToHead mapeo inverso ya construido (nombre del atributo en
     * targetType a encabezado de columna del Excel), o {@code null} para
     * generarlo a partir de {@code headToField}
     * @param headToIndex mapeo ya construido (encabezado de columna del Excel a
     * índice de columna), o {@code null} para generarlo leyendo la fila de
     * encabezados de la planilla
     * @param properties propiedades de configuración del procesador (por
     * ejemplo, {@code allowFieldNotExist}); puede ser {@code null}
     */
    protected ExcelRowProcessor(Row row, Class<T> targetType,
            Map<String, String> headToField,
            int headerRowIndex,
            Map<String, String> fieldToHead,
            Map<String, Integer> headToIndex,
            Map<String, Object> properties) {
        this(row, targetType, ExcelColumns.of(headToField), headerRowIndex, fieldToHead, headToIndex, properties);
    }

    /**
     * Constructor base al que delegan todos los demás: resuelve el índice de
     * encabezados, la especificación de columnas efectiva y los dos mapas
     * derivados.
     *
     * @param row la fila de Excel desde donde se leerán los valores
     * @param targetType la clase del objeto destino
     * @param columns especificación de las columnas; si es {@code null} o está
     * vacía se genera la identidad a partir de los encabezados de la planilla.
     * Se guarda una <b>copia</b>: los mapas derivados se calculan una sola vez
     * acá, así que un agregado posterior sobre la colección del llamador los
     * dejaría desactualizados
     * @param headerRowIndex índice (base 0) de la fila de encabezados
     * @param fieldToHead mapeo inverso ya construido, o {@code null} para
     * derivarlo de {@code columns}
     * @param headToIndex mapeo de cabecera a índice ya construido, o
     * {@code null} para leerlo de la planilla
     * @param properties propiedades de configuración del procesador; puede ser
     * {@code null}
     */
    private ExcelRowProcessor(Row row, Class<T> targetType,
            ExcelColumns columns,
            int headerRowIndex,
            Map<String, String> fieldToHead,
            Map<String, Integer> headToIndex,
            Map<String, Object> properties) {
        this.row = row;
        this.targetType = targetType;
        this.headToIndex = (headToIndex != null) ? headToIndex : buildHeaderIndex(row, headerRowIndex);
        this.columns = (columns == null || columns.isEmpty())
                ? ExcelColumns.of(buildDefaultHeadToField(this.headToIndex))
                : new ExcelColumns(columns);
        this.headToField = this.columns.toHeadToField();
        this.fieldToHead = (fieldToHead != null) ? fieldToHead : this.columns.toFieldToHead();
        this.headerRowIndex = headerRowIndex;
        this.properties = properties;
    }

    /**
     * Instancia un objeto destino a partir de {@link #targetType} y lo completa
     * con los valores de la fila delegando en {@link #applyColumns(IDataRow)}.
     * <p>
     * Las subclases pueden sobrescribir este método si requieren reglas de
     * negocio adicionales; lo habitual es llamar primero a
     * {@code super.process()} (que deja el objeto con las columnas ya
     * asignadas) y completar después los valores derivados.
     *
     * @return el objeto destino de tipo {@code T} ya instanciado y completado,
     * o {@code null} si {@link #isMigrable()} descartó la fila
     * @throws Exception si no se puede instanciar {@link #targetType} o si falla
     * la asignación de algún valor
     */
    @Override
    public T process() throws Exception {
        if (!isMigrable()){
            return null;
        }
        T target = targetType.getDeclaredConstructor().newInstance();
        applyColumns(target);
        return target;
    }

    /**
     * Recorre las columnas declaradas en {@link #getColumns()}, en orden, y
     * asigna en {@code target} el valor de cada una mediante
     * {@code IDataRow.setValue}, convirtiendo previamente el valor al tipo
     * declarado del atributo con {@link ExcelUtil#convertValue(Object, Class)}
     * (ya que {@code setValue} no realiza conversión de tipos). Por cada
     * columna:
     * <ul>
     * <li>si el atributo destino está vacío o no existe en {@code T}, la
     * columna se omite silenciosamente (ver {@code allowFieldNotExist} en
     * {@link #checkMetaData()});</li>
     * <li>si la cabecera no está en la planilla, se asigna el valor por
     * defecto de la columna (o {@code null} si no declaró ninguno). Si el
     * proveedor del valor por defecto o su conversión al tipo del atributo
     * fallan, se marca el registro con el error «El valor por defecto de la
     * columna «X» no se pudo aplicar: …» (número 50000) en vez de abortar la
     * importación entera en la primera fila;</li>
     * <li>si la celda está vacía y la columna declaró
     * {@code defaultWhenBlank} con valor por defecto, se asigna ese valor; si
     * la columna es {@link ColumnRequirement#VALUE}, se marca el registro con
     * el error «La columna «X» no puede estar vacía» (número 50000) y se sigue
     * con la columna siguiente;</li>
     * <li>si la celda trae valor, se aplica la transformación de la columna
     * (si declaró una) y se asigna el resultado convertido. Una excepción de
     * la transformación marca el registro con el error «Error al convertir la
     * columna «X»: …» (número 50000) y no asigna el atributo.</li>
     * </ul>
     * El registro marcado con error no interrumpe el procesamiento: la
     * importación lo deriva al listado de filas con error.
     *
     * @param target objeto destino a completar, ya instanciado.
     * @throws Exception si falla la asignación de algún valor.
     */
    protected void applyColumns(T target) throws Exception {
        for (ExcelColumnSpec spec : columns) {
            String header = spec.getHeader();
            String fieldName = spec.getField();
            if (fieldName == null || fieldName.isEmpty()) {
                continue;
            }
            if (!DataInfo.isFieldExist(targetType, fieldName)) {
                continue;
            }
            if (!spec.isOverwrite()) {
                registerField(target, NO_OVERWRITE_FIELDS, fieldName);
            }
            Class<?> fieldType = DataInfo.getFieldType(targetType, fieldName);
            //La columna no existe en la planilla: se aplica el valor por defecto
            if (!headToIndex.containsKey(header)) {
                if (!spec.hasDefaultValue()) {
                    target.setValue(fieldName, null);
                    continue;
                }
                applyDefaultValue(target, spec, fieldName, fieldType);
                registerField(target, DEFAULTED_FIELDS, fieldName);
                continue;
            }
            Object value = valueOfColumn(header);
            if (isBlank(value)) {
                //El valor por defecto declarado con `defaultWhenBlank` prevalece
                //sobre la exigencia de valor de una columna VALUE.
                if (spec.isDefaultWhenBlank() && spec.hasDefaultValue()) {
                    applyDefaultValue(target, spec, fieldName, fieldType);
                    continue;
                }
                if (spec.getRequirement() == ColumnRequirement.VALUE) {
                    target.setErrors("La columna «" + header + "» no puede estar vacía",
                            fieldName, 50000);
                    continue;
                }
                target.setValue(fieldName, ExcelUtil.convertValue(value, fieldType));
                continue;
            }
            if (spec.getConverter() != null) {
                try {
                    value = spec.getConverter().apply(value);
                } catch (Exception exp) {
                    target.setErrors("Error al convertir la columna «" + header + "»: "
                            + Fn.nvl(exp.getMessage(), exp.getClass().getSimpleName()), fieldName, 50000);
                    continue;
                }
            }
            target.setValue(fieldName, ExcelUtil.convertValue(value, fieldType));
        }
    }

    /**
     * Asigna en {@code target} el valor por defecto declarado por la columna,
     * convertido al tipo del atributo.
     * <p>
     * Un valor por defecto que no se puede aplicar (el proveedor lanza, o el
     * valor no es convertible al tipo del atributo) es un error de programación
     * de la subclase, no un dato malo de la planilla; pero se reporta como
     * error de la fila (número 50000, nombrando la columna) en vez de dejar que
     * la excepción suba por {@link #process()} y corte la importación en la
     * primera fila con una traza que no dice de qué columna se trata.
     * {@link #checkMetaData()} adelanta el diagnóstico cuando el valor por
     * defecto es fijo.
     *
     * @param target objeto destino a completar.
     * @param spec especificación de la columna, con el valor por defecto ya
     * declarado.
     * @param fieldName nombre del atributo destino.
     * @param fieldType tipo declarado del atributo destino.
     */
    /**
     * Anota {@code fieldName} en el conjunto de atributos guardado en las
     * propiedades de la fila bajo {@code key} ({@link #DEFAULTED_FIELDS} o
     * {@link #NO_OVERWRITE_FIELDS}), creándolo si no existe.
     *
     * @param target fila procesada.
     * @param key clave del conjunto en las propiedades de la fila.
     * @param fieldName atributo a anotar.
     */
    @SuppressWarnings("unchecked")
    private void registerField(T target, String key, String fieldName) {
        Map<String, Object> properties = target.getProperties();
        Set<String> fields = (Set<String>) properties.get(key);
        if (fields == null) {
            fields = new HashSet<>();
            properties.put(key, fields);
        }
        fields.add(fieldName);
    }

    private void applyDefaultValue(T target, ExcelColumnSpec spec, String fieldName, Class<?> fieldType) {
        try {
            target.setValue(fieldName, ExcelUtil.convertValue(spec.getDefaultValue().get(), fieldType));
        } catch (Exception exp) {
            target.setErrors("El valor por defecto de la columna «" + spec.getHeader()
                    + "» no se pudo aplicar: "
                    + Fn.nvl(exp.getMessage(), exp.getClass().getSimpleName()),
                    fieldName, 50000);
        }
    }

    /**
     * Provee la clase de la entidad destino en la que se persiste cada registro.
     * Es la entidad de la base de datos (no necesariamente la misma vista
     * {@code T} leída de la planilla): cada fila se convierte hacia una instancia
     * de este tipo con {@code IDataService.copyTo} antes de grabarla. Debe
     * implementarse en cada subclase.
     * @return la clase del objeto destino, usada por {@link #process()} para
     * instanciarlo
     */
    @Override
    public Class<T> getTargetType(){
        return targetType;
    }
    
    /**
     * Devuelve el mapa de propiedades libres asociadas al registro.
     * @return las propiedades de configuración del procesador (por ejemplo,
     * {@code allowFieldNotExist}); nunca {@code null} (si no se establecieron,
     * retorna un mapa vacío)
     */
    protected Map<String, Object> getProperties(){
        return Fn.nvl(properties, new HashMap());
    }
    
    /**
     * Retorna el valor de la celda en {@link #row} que corresponde al ATRIBUTO
     * de target indicado. Resuelve primero el encabezado de columna equivalente
     * vía {@link #fieldToHead}, y luego delega en
     * {@link #valueOfColumn(String)} para leer la celda en sí.
     *
     * @param fieldName nombre del atributo en {@link #targetType} (por ejemplo,
     * "taxId")
     * @return el valor nativo de la celda (String, Double, Boolean o
     * LocalDateTime), o {@code null} si {@code fieldName} no tiene mapeo en
     * {@link #fieldToHead}, la columna no existe en la fila, o la celda está
     * vacía
     */
    protected Object valueOfField(String fieldName) {
        String header = fieldToHead.get(fieldName);
        return header == null ? null : valueOfColumn(header);
    }

    /**
     * Retorna el valor de la celda en {@link #row} ubicada bajo el ENCABEZADO
     * de columna indicado, preservando el tipo nativo de Java según el tipo de
     * la celda en Excel:
     * <ul>
     * <li>celdas {@code STRING} retornan {@link String}</li>
     * <li>celdas {@code NUMERIC} retornan {@link Double}, o
     * {@link LocalDateTime} si la celda tiene formato de fecha</li>
     * <li>celdas {@code BOOLEAN} retornan {@link Boolean}</li>
     * <li>celdas {@code BLANK} o con tipos no soportados retornan
     * {@code null}</li>
     * </ul>
     *
     * @param columnName el encabezado de columna exactamente como aparece en la
     * fila de encabezados del Excel (por ejemplo, "Tax ID")
     * @return el valor nativo de la celda, o {@code null} si {@code columnName}
     * no se encuentra en {@link #headToIndex} o la celda no existe
     */
    protected Object valueOfColumn(String columnName) {
        Integer index = headToIndex.get(columnName);
        if (index == null) {
            return null;
        }
        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }
        return extractNativeValue(cell);
    }

    /**
     * Valida los datos de origen de la planilla Excel antes de procesarlos.
     * Verifica que las cabeceras sean de tipo String, que no haya nombres de
     * columna duplicados, que estén en la planilla las columnas declaradas
     * obligatorias ({@link ColumnRequirement#COLUMN} o
     * {@link ColumnRequirement#VALUE}), que los valores por defecto <b>fijos</b>
     * que se van a aplicar sean convertibles al tipo del atributo destino y que
     * los nombres de columna correspondan a atributos existentes en la clase
     * {@code type}.
     * <p>
     * Los valores por defecto declarados como proveedor
     * ({@code defaultValue(Supplier)}) no se validan acá: pueden tener efecto
     * (una secuencia, la hora del momento) y se evalúan una vez por fila; si
     * fallan, la fila queda marcada con el error número 50000 que nombra la
     * columna (ver {@link #applyColumns(IDataRow)}).
     * <p>
     * La propiedad {@code allowFieldNotExist} (en {@link #getProperties()})
     * controla qué ocurre cuando un atributo mapeado no existe en {@code T}. Por
     * defecto vale {@code true} de forma intencional: se permite que existan
     * columnas mapeadas a atributos inexistentes (no se reporta error), de modo
     * que la subclase pueda procesarlas manualmente en {@link #process()} (por
     * ejemplo, columnas auxiliares o derivadas). Solo cuando se la establece
     * explícitamente en {@code false} se exige que todo atributo mapeado exista
     * en {@code T}, reportándose como error en caso contrario.
     *
     * @return mensaje de error si hubiere inconvenientes, o cadena vacía si
     * todo es válido.
     */
    @Override
    public String checkMetaData() {
        StringBuilder mensaje = new StringBuilder();
        Row headerRow = row.getSheet().getRow(headerRowIndex);
        short last = headerRow.getLastCellNum();
        //Verificar datos de la cabecera
        Set<String> seenHeaders = new HashSet<>();
        for (int c = 0; c < last; c++) {
            Cell cell = headerRow.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            // Una cabecera vacía (definida pero en blanco) marca el fin de las columnas útiles
            if (cell.getCellType() == CellType.BLANK
                    || (cell.getCellType() == CellType.STRING && cell.getStringCellValue().trim().isEmpty())) {
                break;
            }
            // Si llegó hasta acá y no es texto, la cabecera es inválida
            if (cell.getCellType() != CellType.STRING) {
                mensaje.append("En la columna ").append(c).append(" no tiene nombre válido\n");
                continue;
            }
            // Las cabeceras duplicadas son ambiguas: buildHeaderIndex conservaría
            // solo la última, por lo que se reportan como error.
            String headerName = cell.getStringCellValue().trim();
            if (!seenHeaders.add(headerName)) {
                mensaje.append("El nombre de la columna ").append(headerName).append(" está duplicado\n");
            }
        }
        if (mensaje.length() > 0) {
            return mensaje.toString();
        }
        //Verificar que estén en la planilla las columnas declaradas obligatorias.
        //Es una afirmación sobre la planilla, independiente de que el atributo
        //destino exista o lo procese la subclase a mano.
        for (ExcelColumnSpec spec : columns) {
            if (spec.getRequirement() == ColumnRequirement.OPTIONAL) {
                continue;
            }
            if (headToIndex.get(spec.getHeader()) == null) {
                mensaje.append("Falta la columna obligatoria «")
                        .append(spec.getHeader())
                        .append("»\n");
            }
        }
        //Verificar que los valores por defecto que se van a aplicar sean
        //convertibles al tipo del atributo destino: sin esto el error recién
        //aparece al procesar la primera fila, con un mensaje que no nombra la
        //columna. Solo se controlan los valores FIJOS: un proveedor puede tener
        //efecto (una secuencia, la hora del momento) y se evalúa una vez por
        //fila, no acá.
        for (ExcelColumnSpec spec : columns) {
            if (!spec.hasDefaultValue() || !spec.isDefaultValueFixed()) {
                continue;
            }
            //Si la columna está en la planilla y no se pidió `defaultWhenBlank`,
            //el valor por defecto nunca se aplica: reclamarlo sería ruido.
            if (headToIndex.get(spec.getHeader()) != null && !spec.isDefaultWhenBlank()) {
                continue;
            }
            String fieldName = spec.getField();
            if (fieldName == null || fieldName.isEmpty()
                    || !DataInfo.isFieldExist(targetType, fieldName)) {
                continue;
            }
            Class<?> fieldType = DataInfo.getFieldType(targetType, fieldName);
            try {
                ExcelUtil.convertValue(spec.getDefaultValue().get(), fieldType);
            } catch (Exception exp) {
                mensaje.append("El valor por defecto de la columna «")
                        .append(spec.getHeader())
                        .append("» no es convertible a ")
                        .append(fieldType.getSimpleName())
                        .append(": ")
                        .append(Fn.nvl(exp.getMessage(), exp.getClass().getSimpleName()))
                        .append("\n");
            }
        }
        Row firstRow = row.getSheet().getRow(headerRowIndex + 1);
        for (ExcelColumnSpec spec : columns) {
            String header = spec.getHeader();
            String fieldName = spec.getField();
            if (fieldName != null && fieldName.isEmpty()) {
                continue;
            }
            //Si se definio que la columna de la planilla debe tener si o si un
            //equivalente en la clase destino
            if (!Fn.nvl((Boolean) getProperties().get("allowFieldNotExist"), true)) {
                if (!DataInfo.isFieldExist(targetType, fieldName)) {
                    mensaje.append("El nombre de la columna ")
                            .append(header)
                            .append(" es incorrecta\n");
                    continue;
                }
            }
            //Si existe la equivalencia verificar si los tipos de datos de origen son convertibles a los tipos destinos
            if (DataInfo.isFieldExist(targetType, fieldName)) {
                Integer index = headToIndex.get(header);
                Cell cell = (index == null) ? null : firstRow.getCell(index);
                if (cell != null) {
                    Class<?> fieldType = DataInfo.getFieldType(targetType, fieldName);
                    String error = ExcelUtil.getAssignableTypeError(cell, fieldType, fieldName);
                    if (error != null) {
                        mensaje.append(error).append("\n");
                    }
                }
            }
        }
        return mensaje.toString();
    }

    /**
     * Devuelve la fila de Excel que se está procesando actualmente.
     *
     * @return la fila de Excel que se está procesando actualmente
     */
    protected Row getRow() {
        return row;
    }
    
    /**
     * Devuelve la hoja a la que pertenece la fila en curso.
     * @return la hoja a la que pertenece la fila en curso, o {@code null} si
     * aún no se asignó ninguna fila
     */
    @Override
    public Sheet getSheet() {
        if (row == null){
            return null;
        }
        return row.getSheet();
    }

    /**
     * Reemplaza la fila de Excel sobre la cual operan
     * {@link #valueOfField(String)} y {@link #valueOfColumn(String)}. Útil para
     * reutilizar la misma instancia de {@link ExcelRowProcessor} (y sus mapas
     * ya calculados) en varias filas de la misma planilla.
     *
     * @param row la nueva fila de Excel a procesar
     */
    @Override
    public void setRow(Row row) {
        this.row = row;
    }


    /**
     * Devuelve la especificación de las columnas de la planilla, fuente de
     * verdad del procesador.
     * @return la colección ordenada de especificaciones de columna (cabecera,
     * atributo destino, obligatoriedad, valor por defecto y transformación);
     * nunca {@code null}. Es la <b>copia</b> que hizo el constructor y se
     * devuelve para consulta: agregarle columnas no actualiza los mapas
     * derivados del procesador
     */
    @Override
    public ExcelColumns getColumns() {
        return columns;
    }

    /**
     * Obtiene el mapa encabezado de Excel -> atributo del objeto destino.
     * @return el mapeo de encabezado de columna del Excel a nombre del atributo
     * en targetType, derivado de {@link #getColumns()} y en el mismo orden
     */
    @Override
    public Map<String, String> getHeadToField() {
        return headToField;
    }

    /**
     * Obtiene el mapa atributo del objeto destino -> encabezado de Excel.
     *
     * <p>
     * Si el atributo se encuentra null, se genera automáticamente a partir de
     * headToField.</p>
     * @return el mapeo inverso de nombre de atributo en targetType a encabezado
     * de columna del Excel
     */
    @Override
    public Map<String, String> getFieldToHead() {
        return fieldToHead;
    }

    /**
     * Devuelve el mapeo de encabezado de columna del Excel a índice de columna.
     * @return el mapeo de encabezado de columna del Excel a índice de columna
     */
    @Override
    public Map<String, Integer> getHeadToIndex() {
        return headToIndex;
    }

    /**
     * Devuelve el índice (base 0) de la fila de encabezados de la planilla.
     * @return el índice (base 0) de la fila que contiene los encabezados de
     * columna dentro de la planilla
     */
    @Override
    public int getHeaderRowIndex() {
        return Fn.nvl(headerRowIndex, 0);
    }

    // --- utilitarios privados ---
    /**
     * Indica si un valor leído de una celda debe considerarse vacío: celda sin
     * contenido ({@code null}) o texto en blanco.
     *
     * @param value valor nativo de la celda.
     * @return {@code true} si el valor es {@code null} o un texto sin
     * caracteres visibles.
     */
    private static boolean isBlank(Object value) {
        if (value == null) {
            return true;
        }
        return (value instanceof String) && ((String) value).trim().isEmpty();
    }

    /**
     * Genera un {@code headToField} por defecto a partir de los encabezados
     * detectados en la planilla, asumiendo que cada encabezado coincide
     * exactamente con el nombre del atributo correspondiente en target. Se usa
     * cuando el {@code headToField} recibido por constructor es {@code null} o
     * está vacío.
     *
     * @param headToIndex mapeo de encabezado de columna a índice, ya construido
     * @return mapeo de encabezado de columna a nombre de atributo, donde ambos
     * valores son idénticos
     */
    private static Map<String, String> buildDefaultHeadToField(Map<String, Integer> headToIndex) {
        Map<String, String> defaultMap = new HashMap<>();
        for (String header : headToIndex.keySet()) {
            defaultMap.put(header, header);
        }
        return defaultMap;
    }

    /**
     * Lee la fila de encabezados de la planilla que contiene a {@code row}, y
     * construye un mapeo del texto de cada encabezado no vacío a su índice de
     * columna.
     *
     * @param row cualquier fila perteneciente a la planilla a indexar (se usa
     * únicamente para acceder a {@code row.getSheet()})
     * @param headerRowIndex índice (base 0) de la fila que contiene los
     * encabezados
     * @return mapeo de texto de encabezado a índice de columna; vacío si la
     * fila de encabezados no existe
     */
    private Map<String, Integer> buildHeaderIndex(Row row, int headerRowIndex) {
        Map<String, Integer> index = new HashMap<>();
        Row headerRow = row.getSheet().getRow(headerRowIndex);
        if (headerRow == null) {
            return index;
        }
        for (Cell cell : headerRow) {
            Object value = extractNativeValue(cell);
            if (value != null) {
                String text = value.toString().trim();
                if (!text.isEmpty()) {
                    index.put(text, cell.getColumnIndex());
                }
            }
        }
        return index;
    }

    /**
     * Retorna el evaluador de fórmulas de la planilla, creándolo de forma
     * perezosa la primera vez. Se reutiliza para todas las celdas y filas.
     *
     * @return el {@link FormulaEvaluator} asociado al libro de la fila actual
     */
    private FormulaEvaluator getFormulaEvaluator() {
        if (formulaEvaluator == null) {
            formulaEvaluator = row.getSheet().getWorkbook()
                    .getCreationHelper().createFormulaEvaluator();
        }
        return formulaEvaluator;
    }

    /**
     * Lee el valor de una celda preservando su tipo nativo de Excel. Las celdas
     * de tipo fórmula se evalúan en el momento con {@link FormulaEvaluator}
     * (sin depender del resultado en caché de la planilla), y se resuelve el
     * tipo a partir del valor evaluado.
     *
     * @param cell la celda a leer
     * @return el valor de la celda como String, Double, Boolean o LocalDateTime
     * (para celdas numéricas con formato de fecha); {@code null} para celdas
     * vacías o con tipos no soportados
     */
    private Object extractNativeValue(Cell cell) {
        CellType type = cell.getCellType();
        CellValue evaluated = null;
        if (type == CellType.FORMULA) {
            evaluated = getFormulaEvaluator().evaluate(cell);
            if (evaluated == null) {
                return null;
            }
            type = evaluated.getCellType();
        }
        switch (type) {
            case STRING:
                return evaluated != null ? evaluated.getStringValue() : cell.getStringCellValue();
            case NUMERIC:
                double numericValue = evaluated != null
                        ? evaluated.getNumberValue()
                        : cell.getNumericCellValue();
                if (DateUtil.isCellDateFormatted(cell)) {
                    return DateUtil.getLocalDateTime(numericValue);
                }
                return numericValue;
            case BOOLEAN:
                return evaluated != null ? evaluated.getBooleanValue() : cell.getBooleanCellValue();
            case BLANK:
                return null;
            default:
                return null;
        }
    }

    /**
     * Determina si la fila actual ({@link #getRow()}) debe procesarse y
     * convertirse en un objeto de tipo {@code T}. Es invocada al inicio de
     * {@link #process()}: si retorna {@code false}, la fila se omite
     * (process() retorna {@code null}).
     * <p>
     * La implementación base acepta todas las filas. Las subclases pueden
     * sobrescribirla para filtrar filas según reglas de negocio (por ejemplo,
     * descartar filas que no correspondan migrar).
     *
     * @return {@code true} si la fila debe procesarse; {@code false} para
     * omitirla.
     */
    protected boolean isMigrable(){
        return true;
    }
}
