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
     * Mapeo de encabezado de columna del Excel a nombre del atributo en target.
     * Ejemplo: {"Tax ID" -> "taxId"}.
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
        this.row = row;
        this.targetType = targetType;
        this.headToIndex = buildHeaderIndex(row, headerRowIndex);
        this.headToField = (headToField == null || headToField.isEmpty())
                ? buildDefaultHeadToField(this.headToIndex)
                : headToField;
        this.fieldToHead = invert(this.headToField);
        this.headerRowIndex = headerRowIndex;
        this.properties = properties;
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
        this.row = row;
        this.targetType = targetType;
        this.headToIndex = (headToIndex != null) ? headToIndex : buildHeaderIndex(row, headerRowIndex);
        this.headToField = (headToField == null || headToField.isEmpty())
                ? buildDefaultHeadToField(this.headToIndex)
                : headToField;
        this.fieldToHead = (fieldToHead != null) ? fieldToHead : invert(this.headToField);
        this.headerRowIndex = headerRowIndex;
        this.properties = properties;
    }

    /**
     * Instancia un objeto destino a partir de {@link #targetType}, recorre el
     * mapeo {@link #getHeadToField()} y asigna en el destino el valor de cada
     * celda mediante {@code IDataRow.setValue}, convirtiendo
     * previamente el valor nativo de la celda al tipo declarado del atributo
     * con {@link ExcelUtil#convertValue(Object, Class)} (ya que {@code setValue}
     * no realiza conversión de tipos).
     * <p>
     * Los atributos cuyo nombre esté vacío, o que no existan en {@code T}, se
     * omiten silenciosamente (ver {@code allowFieldNotExist} en
     * {@link #checkMetaData()}). Las subclases pueden sobrescribir este método
     * si requieren reglas de negocio adicionales.
     *
     * @return el objeto destino de tipo {@code T} ya instanciado y completado
     * @throws Exception si no se puede instanciar {@link #targetType} o si falla
     * la asignación de algún valor
     */
    @Override
    public T process() throws Exception {
        if (!isMigrable()){
            return null;
        }
        T target = targetType.getDeclaredConstructor().newInstance();
        for (Map.Entry<String, String> entry : headToField.entrySet()) {
            String header = entry.getKey();
            String fieldName = entry.getValue();
            if (fieldName == null || fieldName.isEmpty()) {
                continue;
            }
            if (!DataInfo.isFieldExist(targetType, fieldName)) {
                continue;
            }
            Object value = valueOfColumn(header);
            Object converted = ExcelUtil.convertValue(value, DataInfo.getFieldType(targetType, fieldName));
            target.setValue(fieldName, converted);
        }
        return target;
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
     * columna duplicados y que los nombres de columna correspondan a atributos
     * existentes en la clase {@code type}.
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
        Row firstRow = row.getSheet().getRow(headerRowIndex + 1);
        for (Map.Entry<String, String> entry : headToField.entrySet()) {
            String header = entry.getKey();
            String fieldName = entry.getValue();
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
     * Obtiene el mapa encabezado de Excel -> atributo del objeto destino.
     * @return el mapeo de encabezado de columna del Excel a nombre del atributo
     * en targetType
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
     * Construye el inverso de un mapa encabezado-a-atributo.
     *
     * @param map mapeo de encabezado de columna del Excel a nombre del atributo
     * en targetType
     * @return mapeo de nombre del atributo en targetType a encabezado de
     * columna del Excel
     */
    private static Map<String, String> invert(Map<String, String> map) {
        Map<String, String> inverted = new HashMap<>();
        for (Map.Entry<String, String> e : map.entrySet()) {
            inverted.put(e.getValue(), e.getKey());
        }
        return inverted;
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
