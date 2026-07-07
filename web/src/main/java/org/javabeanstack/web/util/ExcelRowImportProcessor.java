/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2018 Jorge Enciso
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
package org.javabeanstack.web.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.javabeanstack.data.IDataRow;

/**
 * Clase base para procesar una fila de Excel e importar sus valores en un
 * objeto que implemente la interfaz IDataRow.
 *
 * <p>
 * La relación entre las columnas de Excel y los atributos del objeto destino se
 * gestiona mediante los siguientes mapas:</p>
 *
 * <ul>
 * <li><b>headToField:</b> encabezado de Excel -> atributo del objeto
 * destino.</li>
 * <li><b>fieldToHead:</b> atributo del objeto destino -> encabezado de
 * Excel.</li>
 * <li><b>headToColumnIndex:</b> encabezado de Excel -> índice de columna.</li>
 * </ul>
 *
 * @param <T> Tipo del objeto destino que debe implementar IDataRow.
 */
public abstract class ExcelRowImportProcessor<T extends IDataRow> {

    private Row row;
    private T target;
    private final Row headerRow;
    private final Map<String, String> headToField;

    private Map<String, String> fieldToHead;
    private Map<String, Integer> headToColumnIndex;

    private final DataFormatter dataFormatter;
    private final FormulaEvaluator formulaEvaluator;

    /**
     * Crea un procesador utilizando la primera fila de la hoja como fila de
     * encabezados.
     *
     * <p>
     * Los mapas fieldToHead y headToColumnIndex se generan automáticamente.</p>
     *
     * @param row Fila de Excel que será procesada.
     * @param target Objeto destino donde se asignarán los valores importados.
     * @param headToField Mapa encabezado de Excel -> atributo del objeto
     * destino.
     */
    public ExcelRowImportProcessor(
            Row row,
            T target,
            Map<String, String> headToField) {

        this(
                row,
                target,
                getDefaultHeaderRow(row),
                headToField,
                null,
                null
        );
    }

    /**
     * Crea un procesador utilizando la primera fila de la hoja como fila de
     * encabezados.
     *
     * <p>
     * Cuando fieldToHead es null, se genera automáticamente a partir de
     * headToField.</p>
     *
     * <p>
     * Cuando headToColumnIndex es null, se genera automáticamente a partir de
     * la fila de encabezados.</p>
     *
     * @param row Fila de Excel que será procesada.
     * @param target Objeto destino donde se asignarán los valores importados.
     * @param headToField Mapa encabezado de Excel -> atributo del objeto
     * destino.
     * @param fieldToHead Mapa atributo del objeto destino -> encabezado de
     * Excel.
     * @param headToColumnIndex Mapa encabezado de Excel -> índice de columna.
     */
    public ExcelRowImportProcessor(
            Row row,
            T target,
            Map<String, String> headToField,
            Map<String, String> fieldToHead,
            Map<String, Integer> headToColumnIndex) {

        this(
                row,
                target,
                getDefaultHeaderRow(row),
                headToField,
                fieldToHead,
                headToColumnIndex
        );
    }

    /**
     * Crea un procesador indicando explícitamente la fila que contiene los
     * encabezados de la planilla.
     *
     * <p>
     * Cuando fieldToHead es null, se genera automáticamente a partir de
     * headToField.</p>
     *
     * <p>
     * Cuando headToColumnIndex es null, se genera automáticamente a partir de
     * headerRow.</p>
     *
     * @param row Fila de Excel que será procesada.
     * @param target Objeto destino donde se asignarán los valores importados.
     * @param headerRow Fila de Excel que contiene los encabezados.
     * @param headToField Mapa encabezado de Excel -> atributo del objeto
     * destino.
     * @param fieldToHead Mapa atributo del objeto destino -> encabezado de
     * Excel.
     * @param headToColumnIndex Mapa encabezado de Excel -> índice de columna.
     */
    public ExcelRowImportProcessor(
            Row row,
            T target,
            Row headerRow,
            Map<String, String> headToField,
            Map<String, String> fieldToHead,
            Map<String, Integer> headToColumnIndex) {

        this.row = Objects.requireNonNull(row, "La fila de Excel no puede ser null");
        this.target = Objects.requireNonNull(target, "El objeto destino no puede ser null");
        this.headerRow = Objects.requireNonNull(headerRow, "La fila de encabezados no puede ser null");
        this.headToField = new HashMap<>(
                Objects.requireNonNull(headToField, "El mapa headToField no puede ser null")
        );

        this.dataFormatter = new DataFormatter();
        this.formulaEvaluator = row.getSheet()
                .getWorkbook()
                .getCreationHelper()
                .createFormulaEvaluator();

        this.fieldToHead = fieldToHead;
        this.headToColumnIndex = headToColumnIndex;
    }

    /**
     * Obtiene la primera fila de la hoja como fila de encabezados por defecto.
     *
     * @param row Fila de datos perteneciente a la hoja de Excel.
     * @return Primera fila de la hoja de Excel.
     */
    private static Row getDefaultHeaderRow(Row row) {
        if (row == null) {
            throw new IllegalArgumentException("La fila de Excel no puede ser null");
        }

        Sheet sheet = row.getSheet();

        if (sheet == null) {
            throw new IllegalArgumentException(
                    "La fila indicada no pertenece a ninguna hoja de Excel"
            );
        }

        Row headerRow = sheet.getRow(sheet.getFirstRowNum());

        if (headerRow == null) {
            throw new IllegalArgumentException(
                    "No fue posible obtener la fila de encabezados de Excel"
            );
        }

        return headerRow;
    }

    /**
     * Genera el mapa inverso atributo -> encabezado a partir del mapa
     * encabezado -> atributo.
     *
     * @param headToField Mapa encabezado de Excel -> atributo del objeto
     * destino.
     * @return Mapa atributo del objeto destino -> encabezado de Excel.
     */
    private Map<String, String> buildFieldToHead(Map<String, String> headToField) {
        Map<String, String> result = new HashMap<>();

        for (Map.Entry<String, String> entry : headToField.entrySet()) {
            String headerName = normalize(entry.getKey());
            String fieldName = normalize(entry.getValue());

            if (isBlank(headerName) || isBlank(fieldName)) {
                continue;
            }

            if (result.containsKey(fieldName)) {
                throw new IllegalArgumentException(
                        "El atributo '" + fieldName
                        + "' está relacionado con más de un encabezado de Excel"
                );
            }

            result.put(fieldName, headerName);
        }

        return result;
    }

    /**
     * Genera un mapa que relaciona cada encabezado de Excel con su índice de
     * columna correspondiente.
     *
     * @param headerRow Fila de Excel que contiene los encabezados.
     * @return Mapa encabezado de Excel -> índice de columna.
     */
    private Map<String, Integer> buildHeadToColumnIndex(Row headerRow) {
        Map<String, Integer> result = new HashMap<>();

        for (Cell cell : headerRow) {
            String headerName = normalize(dataFormatter.formatCellValue(cell));

            if (!isBlank(headerName)) {
                result.put(headerName, cell.getColumnIndex());
            }
        }

        return result;
    }

    /**
     * Asigna la fila de Excel que será procesada.
     *
     * <p>
     * La nueva fila debe pertenecer a la misma hoja que la fila de encabezados
     * configurada, para asegurar que los índices de columna y los mapeos
     * actuales sigan siendo válidos.</p>
     *
     * @param row Nueva fila de Excel a procesar.
     * @throws IllegalArgumentException Si la fila es null o pertenece a una
     * hoja diferente de la fila de encabezados.
     */
    public void setRow(Row row) {
        if (row == null) {
            throw new IllegalArgumentException("La fila de Excel no puede ser null");
        }

        if (row.getSheet() != headerRow.getSheet()) {
            throw new IllegalArgumentException(
                    "La nueva fila debe pertenecer a la misma hoja que la fila de encabezados"
            );
        }

        this.row = row;
    }

    /**
     * Asigna el objeto destino donde se cargarán los valores obtenidos desde la
     * fila de Excel.
     *
     * @param target Nuevo objeto destino.
     * @throws IllegalArgumentException Si el objeto destino es null.
     */
    public void setTarget(T target) {
        if (target == null) {
            throw new IllegalArgumentException("El objeto destino no puede ser null");
        }

        this.target = target;
    }

    /**
     * Obtiene la celda de Excel asociada a un atributo del objeto destino.
     *
     * @param fieldName Nombre del atributo del objeto destino.
     * @return Celda de Excel correspondiente o null si no existe una relación.
     */
    protected Cell getCellByField(String fieldName) {
        Integer columnIndex = getColumnIndexByField(fieldName);

        if (columnIndex == null) {
            return null;
        }

        return row.getCell(columnIndex);
    }

    /**
     * Obtiene la celda de Excel asociada a un encabezado de la planilla.
     *
     * @param headerName Nombre del encabezado de la columna de Excel.
     * @return Celda de Excel correspondiente o null si no existe el encabezado.
     */
    protected Cell getCellByHeader(String headerName) {
        Integer columnIndex = getColumnIndexByHeader(headerName);

        if (columnIndex == null) {
            return null;
        }

        return row.getCell(columnIndex);
    }

    /**
     * Obtiene el índice de columna de Excel asociado a un atributo del objeto
     * destino.
     *
     * @param fieldName Nombre del atributo del objeto destino.
     * @return Índice de columna de Excel o null si no existe una relación.
     */
    protected Integer getColumnIndexByField(String fieldName) {
        String normalizedFieldName = normalize(fieldName);
        String headerName = getFieldToHead().get(normalizedFieldName);

        if (headerName == null) {
            return null;
        }

        return getColumnIndexByHeader(headerName);
    }

    /**
     * Obtiene el índice de columna asociado a un encabezado de Excel.
     *
     * @param headerName Nombre del encabezado de Excel.
     * @return Índice de columna de Excel o null si no existe.
     */
    protected Integer getColumnIndexByHeader(String headerName) {
        return getHeadToColumnIndex().get(normalize(headerName));
    }

    /**
     * Obtiene el valor de una celda como texto a partir del nombre de un
     * atributo del objeto destino.
     *
     * @param fieldName Nombre del atributo del objeto destino.
     * @return Valor de la celda convertido a texto o null si no existe.
     */
    protected String getStringValueByField(String fieldName) {
        return getStringValue(getCellByField(fieldName));
    }

    /**
     * Obtiene el valor de una celda como texto a partir del nombre del
     * encabezado de Excel.
     *
     * @param headName Nombre del encabezado de Excel.
     * @return Valor de la celda convertido a texto o null si no existe.
     */
    protected String getStringValueByHead(String headName) {
        return getStringValue(getCellByHeader(headName));
    }

    /**
     * Obtiene el valor nativo de una celda a partir del nombre de un atributo
     * del objeto destino.
     *
     * <p>
     * El resultado puede ser String, Double, Boolean, Date o null, según el
     * tipo de contenido de la celda.</p>
     *
     * @param fieldName Nombre del atributo del objeto destino.
     * @return Valor de la celda como Object o null si no existe.
     */
    protected Object getValueByField(String fieldName) {
        return getValue(getCellByField(fieldName));
    }

    /**
     * Obtiene el valor nativo de una celda a partir del nombre del encabezado
     * de Excel.
     *
     * <p>
     * El resultado puede ser String, Double, Boolean, Date o null, según el
     * tipo de contenido de la celda.</p>
     *
     * @param headName Nombre del encabezado de Excel.
     * @return Valor de la celda como Object o null si no existe.
     */
    protected Object getValueByHead(String headName) {
        return getValue(getCellByHeader(headName));
    }

    /**
     * Convierte una celda de Excel a su representación de texto.
     *
     * <p>
     * Cuando la celda contiene una fórmula, se evalúa antes de devolver su
     * valor formateado.</p>
     *
     * @param cell Celda de Excel a convertir.
     * @return Valor formateado como texto o null si la celda es null.
     */
    private String getStringValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        return dataFormatter.formatCellValue(cell, formulaEvaluator);
    }

    /**
     * Obtiene el valor nativo almacenado en una celda de Excel.
     *
     * <p>
     * Las celdas numéricas con formato de fecha se devuelven como Date. Las
     * celdas con fórmula se evalúan antes de devolver su resultado.</p>
     *
     * @param cell Celda de Excel a procesar.
     * @return String, Double, Boolean, Date, mensaje de error o null.
     */
    private Object getValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == CellType.FORMULA) {
            return getFormulaValue(cell);
        }

        return getValueByCellType(cell, cell.getCellType(), null);
    }

    /**
     * Evalúa una fórmula de Excel y devuelve su resultado con el tipo de dato
     * correspondiente.
     *
     * @param cell Celda de Excel que contiene una fórmula.
     * @return Resultado evaluado de la fórmula o null si no pudo evaluarse.
     */
    private Object getFormulaValue(Cell cell) {
        CellValue evaluatedValue = formulaEvaluator.evaluate(cell);

        if (evaluatedValue == null) {
            return null;
        }

        return getValueByCellType(
                cell,
                evaluatedValue.getCellType(),
                evaluatedValue
        );
    }

    /**
     * Convierte el contenido de una celda según su tipo de dato.
     *
     * @param cell Celda original de Excel.
     * @param cellType Tipo de dato que será procesado.
     * @param evaluatedValue Resultado de una fórmula evaluada. Puede ser null.
     * @return Valor convertido al tipo Java correspondiente.
     */
    private Object getValueByCellType(
            Cell cell,
            CellType cellType,
            CellValue evaluatedValue) {

        switch (cellType) {
            case STRING:
                return evaluatedValue != null
                        ? evaluatedValue.getStringValue()
                        : cell.getStringCellValue();

            case NUMERIC:
                double numericValue = evaluatedValue != null
                        ? evaluatedValue.getNumberValue()
                        : cell.getNumericCellValue();

                if (DateUtil.isCellDateFormatted(cell)) {
                    return DateUtil.getJavaDate(numericValue);
                }

                return numericValue;

            case BOOLEAN:
                return evaluatedValue != null
                        ? evaluatedValue.getBooleanValue()
                        : cell.getBooleanCellValue();

            case ERROR:
                byte errorCode = evaluatedValue != null
                        ? evaluatedValue.getErrorValue()
                        : cell.getErrorCellValue();

                return FormulaError.forInt(errorCode).getString();

            case BLANK:
            case _NONE:
            default:
                return null;
        }
    }

    /**
     * Obtiene la fila de Excel que está siendo procesada.
     *
     * @return Fila de Excel actual.
     */
    protected Row getRow() {
        return row;
    }

    /**
     * Obtiene la fila configurada como fila de encabezados.
     *
     * @return Fila de encabezados de Excel.
     */
    protected Row getHeaderRow() {
        return headerRow;
    }

    /**
     * Obtiene el objeto destino que será cargado con los datos de Excel.
     *
     * @return Objeto destino.
     */
    protected T getTarget() {
        return target;
    }

    /**
     * Obtiene el mapa encabezado de Excel -> atributo del objeto destino.
     *
     * @return Mapa de relación entre encabezados y atributos.
     */
    protected Map<String, String> getHeadToField() {
        return headToField;
    }

    /**
     * Obtiene el mapa atributo del objeto destino -> encabezado de Excel.
     *
     * <p>
     * Si el atributo se encuentra null, se genera automáticamente a partir de
     * headToField.</p>
     *
     * @return Mapa atributo del objeto destino -> encabezado de Excel.
     */
    public Map<String, String> getFieldToHead() {
        if (fieldToHead == null) {
            fieldToHead = buildFieldToHead(headToField);
        }

        return fieldToHead;
    }

    /**
     * Asigna el mapa atributo del objeto destino -> encabezado de Excel.
     *
     * <p>
     * Cuando se recibe null, el mapa se genera automáticamente a partir de
     * headToField.</p>
     *
     * @param fieldToHead Mapa atributo del objeto destino -> encabezado de
     * Excel.
     */
    public void setFieldToHead(Map<String, String> fieldToHead) {
        if (fieldToHead == null) {
            this.fieldToHead = buildFieldToHead(headToField);
            return;
        }

        this.fieldToHead = new HashMap<>(fieldToHead);
    }

    /**
     * Obtiene el mapa encabezado de Excel -> índice de columna.
     *
     * <p>
     * Si el atributo se encuentra null, se genera automáticamente a partir de
     * la fila de encabezados.</p>
     *
     * @return Mapa encabezado de Excel -> índice de columna.
     */
    public Map<String, Integer> getHeadToColumnIndex() {
        if (headToColumnIndex == null) {
            headToColumnIndex = buildHeadToColumnIndex(headerRow);
        }

        return headToColumnIndex;
    }

    /**
     * Asigna el mapa encabezado de Excel -> índice de columna.
     *
     * <p>
     * Cuando se recibe null, el mapa se genera automáticamente a partir de la
     * fila de encabezados.</p>
     *
     * @param headToColumnIndex Mapa encabezado de Excel -> índice de columna.
     */
    public void setHeadToColumnIndex(
            Map<String, Integer> headToColumnIndex) {

        if (headToColumnIndex == null) {
            this.headToColumnIndex = buildHeadToColumnIndex(headerRow);
            return;
        }

        this.headToColumnIndex = new HashMap<>(headToColumnIndex);
    }

    /**
     * Elimina los espacios al inicio y al final de un texto.
     *
     * @param value Texto a normalizar.
     * @return Texto sin espacios laterales o null si el valor recibido es null.
     */
    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * Verifica si un texto es null, vacío o contiene solo espacios.
     *
     * @param value Texto a evaluar.
     * @return True si el texto es null, vacío o contiene solo espacios.
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Procesa la fila de Excel y devuelve el objeto destino cargado con los
     * valores importados.
     *
     * @return Objeto destino procesado.
     */
    public abstract T process();
}
