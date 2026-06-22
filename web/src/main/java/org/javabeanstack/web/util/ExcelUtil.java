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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.faces.context.FacesContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.OLE2NotOfficeXmlFileException;
import org.apache.poi.poifs.filesystem.NotOLE2FileException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import static org.apache.poi.ss.usermodel.CellType.BOOLEAN;
import static org.apache.poi.ss.usermodel.CellType.FORMULA;
import static org.apache.poi.ss.usermodel.CellType.NUMERIC;
import static org.apache.poi.ss.usermodel.CellType.STRING;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.javabeanstack.data.IDataQueryModel;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.data.model.DataQueryModel;
import org.javabeanstack.util.Fn;
import org.javabeanstack.util.LocalDates;

/**
 *
 * @author Jorge Enciso
 */
public class ExcelUtil {

    private static final int SAMPLE_ROWS = 200;  // filas que se muestrean para medir String
    private static final int WIDTH_MIN = 8;    // ancho mínimo en caracteres
    private static final int WIDTH_MAX = 60;   // tope para String largos (evita columnas enormes)
    private static final int WIDTH_DATE = 19;   // "dd/mm/yyyy hh:mm:ss" = 19 fijo
    private static final int WIDTH_NUMBER = 15;   // "#,##0.00" con signo y separadores de miles
    private static final int STRING_PAD = 2;    // margen visual para texto

    /**
     * Factor de escala por fuente. Tus celdas usan 8pt; la unidad de Excel se
     * mide contra la fuente por defecto (~11pt). 1.0 = generoso (no se corta
     * nada); ~0.78 = columnas más ajustadas al 8pt real.
     */
    private static final double FONT_SCALE = 1.0;

    private static int[] computeColumnWidths(List<IDataQueryModel> data, int columnCount,
            Map<Integer, Integer> overrides) {
        int[] widths = new int[columnCount];
        int sampleSize = Math.min(SAMPLE_ROWS, data.size());
        for (int j = 0; j < columnCount; j++) {
            // a) Override explícito (ancho "asignado previamente"): gana siempre
            if (overrides != null && overrides.containsKey(j)) {
                widths[j] = clampWidth(overrides.get(j));
                continue;
            }
            // b) Piso: que al menos entre el nombre de la cabecera
            int chars = data.get(0).getColumnName(j).length();
            // c) Detectar tipo y, solo para String, muestrear longitud
            for (int i = 0; i < sampleSize; i++) {
                Object v = ((Object[]) data.get(i).getRow())[j];
                if (v == null) {
                    continue;
                }
                if (v instanceof Date) {                 // Timestamp extiende Date
                    chars = Math.max(chars, WIDTH_DATE);
                } else if (v instanceof Number) {        // BigDecimal, Long, Integer, Double...
                    chars = Math.max(chars, WIDTH_NUMBER);
                } else {                                 // String, Boolean ("true"/"false"), etc.
                    chars = Math.max(chars, v.toString().trim().length() + STRING_PAD);
                }
            }
            widths[j] = clampWidth(chars);
        }
        return widths;
    }

    private static int clampWidth(int chars) {
        return Math.max(WIDTH_MIN, Math.min(WIDTH_MAX, chars));
    }

    /**
     * Convierte un ancho en caracteres a las unidades de POI (1/256 de
     * caracter).
     */
    private static int charsToWidthUnits(int chars) {
        return (int) Math.round((chars + 1) * 256 * FONT_SCALE);  // +1 caracter de padding
    }

    public static void downLoadFile(Workbook workBook, String fileName) throws Exception {
        if (workBook == null) {
            return;
        }
        boolean isXls = (workBook instanceof HSSFWorkbook);   // solo HSSF es .xls; XSSF y SXSSF son .xlsx
        String extension = isXls ? ".xls" : ".xlsx";
        String contentType = isXls
                ? "application/vnd.ms-excel"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

        HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance()
                .getExternalContext().getResponse();
        response.setContentType(contentType);
        response.addHeader("Content-disposition", "attachment; filename=" + fileName + extension);

        try (ServletOutputStream out = response.getOutputStream()) {
            workBook.write(out);
            out.flush();
        } finally {
            // Borra los archivos temporales que SXSSF creó en disco
            try (workBook) {
                // Borra los archivos temporales que SXSSF creó en disco
                if (workBook instanceof SXSSFWorkbook) {
                    ((SXSSFWorkbook) workBook).dispose();
                }
            }
        }
        FacesContext.getCurrentInstance().responseComplete();
    }

    public static Workbook openWorkbook(String filePath) throws IOException {
        String lowerPath = filePath.toLowerCase();
        boolean isXlsx = lowerPath.endsWith("xlsx");
        boolean isXls = lowerPath.endsWith("xls");
        if (!isXlsx && !isXls) {
            throw new IllegalArgumentException("The specified file is not an Excel file");
        }
        try (InputStream fileInputStream = new FileInputStream(filePath)) {
            if (isXlsx) {
                return new XSSFWorkbook(fileInputStream);
            }
            return new HSSFWorkbook(fileInputStream);
        } catch (OLE2NotOfficeXmlFileException | NotOLE2FileException e) {
            throw new IllegalArgumentException(
                    "The file format is not supported. Ensure the file is a valid Excel file.", e);
        }
    }

    public static Workbook openWorkbook(File file) throws IOException {
        Workbook wb = WorkbookFactory.create(file);
        return wb;
    }

    public static Workbook toExcel(List<IDataQueryModel> toExport) throws Exception {
        return toExcel(toExport, null);
    }

    public static Workbook toExcel(List<IDataQueryModel> toExport, Map<Integer, Integer> widthOverrides) throws Exception {
        if (toExport == null || toExport.isEmpty()) {
            return null;
        }
        SXSSFWorkbook workBook = new SXSSFWorkbook(100);
        workBook.setCompressTempFiles(true);
        SXSSFSheet sheet = workBook.createSheet("DATOS");
        // Sin autoSizeColumn -> NO hace falta trackAllColumnsForAutoSizing()

        DataFormat dataFormat = workBook.createDataFormat();
        Font font8 = workBook.createFont();
        font8.setFontHeightInPoints((short) 8);
        Font fontBold = workBook.createFont();
        fontBold.setFontHeightInPoints((short) 8);
        fontBold.setBold(true);

        CellStyle defaultCellStyle = workBook.createCellStyle();
        defaultCellStyle.setFont(font8);
        CellStyle numberCellStyle = workBook.createCellStyle();
        numberCellStyle.setFont(font8);
        numberCellStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));
        CellStyle dateCellStyle = workBook.createCellStyle();
        dateCellStyle.setFont(font8);
        dateCellStyle.setDataFormat(dataFormat.getFormat("dd/mm/yyyy hh:mm:ss"));
        CellStyle textBoldCellStyle = workBook.createCellStyle();
        textBoldCellStyle.setFont(fontBold);

        int columnCount = toExport.get(0).getColumnList().length;

        // 1) Calcular y fijar anchos ANTES de escribir
        int[] widthChars = computeColumnWidths(toExport, columnCount, widthOverrides);
        for (int j = 0; j < columnCount; j++) {
            sheet.setColumnWidth(j, charsToWidthUnits(widthChars[j]));
        }

        // 2) Cabecera
        int rownum = 0;
        Row row = sheet.createRow(rownum++);
        for (int j = 0; j < columnCount; j++) {
            Cell cell = row.createCell(j);
            cell.setCellStyle(textBoldCellStyle);
            cell.setCellValue(toExport.get(0).getColumnName(j));
        }

        // 3) Datos
        for (int i = 0; i < toExport.size(); i++) {
            row = sheet.createRow(rownum++);
            Object[] fila = (Object[]) toExport.get(i).getRow();
            for (int j = 0; j < columnCount; j++) {
                Cell cell = row.createCell(j);
                Object valor = fila[j];
                if (valor == null) {
                    cell.setCellStyle(defaultCellStyle);
                } else if (valor instanceof BigDecimal) {
                    cell.setCellStyle(numberCellStyle);
                    cell.setCellValue(((BigDecimal) valor).doubleValue());
                } else if (valor instanceof Date) {
                    cell.setCellStyle(dateCellStyle);
                    cell.setCellValue((Date) valor);
                } else {
                    cell.setCellStyle(defaultCellStyle);
                    cell.setCellValue(String.valueOf(valor));
                }
            }
        }
        return workBook;
    }

    /**
     * Lee los datos de una planilla excel y lo convierte a una List a tipo
     * IDataQueryModel
     *
     * @param sheet planilla excel.
     * @return Lista de registros tipo IDataQueryModel
     * @throws Exception
     */
    public static List<IDataQueryModel> fromExcelToDataQueryModel(Sheet sheet) throws Exception {
        if (sheet == null) {
            return null;
        }
        List<String> headerNames = getHeaderNames(sheet);
        if (headerNames.isEmpty()) {
            throw new Exception("Verifique que en la primera fila tenga nombres de columnas válidas, tipo caracter sin espacios");
        }
        List<IDataQueryModel> retornar = new ArrayList();
        String[] columnNames = headerNames.toArray(String[]::new);
        //Recorrer las filas de la hoja
        for (Row row : sheet) {
            Cell cell = null;
            try {
                if (row.getRowNum() == 0) {
                    continue;
                }
                IDataQueryModel data = new DataQueryModel();
                Object[] dataRow = new Object[columnNames.length];
                data.setRow(dataRow);
                data.setColumnList(columnNames);
                //Recorrer las celdas
                for (int i = 0; i < headerNames.size(); i++) {
                    String columnName = sheet.getRow(0).getCell(i).getStringCellValue();
                    if (Fn.nvl(columnName, "").isEmpty()) {
                        continue;
                    }
                    cell = row.getCell(i);
                    if (cell == null) {
                        continue;
                    }
                    switch (cell.getCellType()) {
                        case NUMERIC:
                            if (DateUtil.isCellDateFormatted(cell)) {
                                data.setColumn(columnName, cell.getLocalDateTimeCellValue());
                            } else {
                                data.setColumn(columnName, cell.getNumericCellValue());
                            }
                            break;
                        case BOOLEAN:
                            data.setColumn(columnName, cell.getBooleanCellValue());
                            break;
                        case STRING:
                            data.setColumn(columnName, cell.getStringCellValue());
                            break;
                        case FORMULA:
                            switch (cell.getCachedFormulaResultType()) {
                                case NUMERIC:
                                    data.setColumn(columnName, cell.getNumericCellValue());
                                    break;
                                case STRING:
                                    data.setColumn(columnName, cell.getStringCellValue());
                                    break;
                                case BOOLEAN:
                                    data.setColumn(columnName, cell.getBooleanCellValue());
                                    break;
                                default:
                                    data.setColumn(columnName, null);
                            }
                            break;
                    }
                }
                retornar.add(data);
            } catch (Exception e) {
                String errorMsg = "ERROR EN LA FILA " + row.getRowNum();
                if (cell != null) {
                    errorMsg += ", CELDA " + cell.getAddress().formatAsString();
                }
                errorMsg += ", " + e.getMessage();
                throw new Exception(errorMsg);
            }
        }
        return retornar;
    }

    /**
     * Puebla una lista de instancias del objeto destino ({@code T}) procesando
     * las filas de una planilla excel mediante un {@link IExcelRowProcessor} ya
     * configurado.
     * <p>
     * El procesador se reutiliza para cada fila (vía
     * {@link IExcelRowProcessor#setRow(Row)}), por lo que conserva los mapas de
     * encabezados ya calculados al construirlo.
     *
     * @param <T> tipo del objeto destino (IDataRow).
     * @param sheet planilla excel.
     * @param processor procesador de filas ya configurado con el mapeo de
     * columnas y la fila de encabezados.
     * @param firstRow índice (base 0) de la primera fila de datos a procesar.
     * @param rowCount cantidad de filas a procesar; si es menor o igual a 0 se
     * procesan todas las filas desde {@code firstRow} hasta el final de la
     * planilla. Las filas vacías (inexistentes) dentro del rango se omiten.
     * @return Lista con las instancias de {@code T} pobladas a partir de las
     * filas procesadas.
     * @throws Exception
     */
    public static <T extends IDataRow> List<T> fromExcelToDataRow (
            Sheet sheet, IExcelRowProcessor<T> processor, int firstRow, int rowCount) throws Exception {
        List<T> retornar = new ArrayList();
        if (sheet == null || processor == null) {
            return retornar;
        }
        int lastRow = (rowCount <= 0) ? sheet.getLastRowNum() : firstRow + rowCount - 1;
        for (int r = firstRow; r <= lastRow; r++) {
            // No se procesa la fila de encabezados
            if (r == processor.getHeaderRowIndex()) {
                continue;
            }
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            processor.setRow(row);
            retornar.add(processor.process());
        }
        return retornar;
    }


    /**
     * Convierte un valor de la celda de la planilla a un valor asignable en un
     * atributo de una clase de java.
     *
     * @param value valor de la celda.
     * @param type tipo a convertir.
     * @return valor con el tipo convertido.
     */
    public static Object convertValue(Object value, Class type) {
        if (value == null) {
            return null;
        } else if (type.getName().equals(value.getClass().getName())) {
            return value;
        } else if (type.getSimpleName().equals("BigDecimal") && !(value instanceof BigDecimal)) {
            return new BigDecimal(value.toString());
        } else if (type.getSimpleName().equals("Long") && !(value instanceof Long)) {
            return new BigDecimal(value.toString().trim()).longValueExact();
        } else if (type.getSimpleName().equals("Integer") && !(value instanceof Integer)) {
            return new BigDecimal(value.toString().trim()).intValueExact();
        } else if (type.getSimpleName().equals("Short") && !(value instanceof Short)) {
            return new BigDecimal(value.toString().trim()).shortValueExact();
        } else if (type.getSimpleName().equals("Character") && !(value instanceof Character)) {
            String s = value.toString();
            return s.isEmpty() ? null : s.charAt(0);
        } else if (type.getSimpleName().equals("Boolean") && !(value instanceof Boolean)) {
            return (value.toString().trim().equals("1") || value.toString().trim().equalsIgnoreCase("true"));
        } else if (type.getSimpleName().equals("LocalDateTime") && (value instanceof Date)) {
            return LocalDates.toDateTime((Date) value);
        } else if (type.getSimpleName().equals("LocalDateTime") && (value instanceof Timestamp)) {
            return ((Timestamp) value).toLocalDateTime();
        } else if (type.getSimpleName().equals("LocalDateTime") && (value instanceof String)) {
            return LocalDates.toDateTime((String)value);
        }
        return value;
    }

    /**
     * Devuelve un valor BigDecimal de una celda
     *
     * @param cell celda
     * @return valor BigDecimal.
     * @throws Exception
     */
    public static BigDecimal getBigDecimal(Cell cell) throws Exception {
        BigDecimal retornar = null;
        switch (cell.getCellType()) {
            case NUMERIC:
                retornar = BigDecimal.valueOf(cell.getNumericCellValue());
                break;
            case BOOLEAN:
                throw new Exception("Imposible convertir de tipo boolean a bigdecimal");
            case STRING:
                retornar = new BigDecimal(cell.getStringCellValue());
                break;
            case FORMULA:
                retornar = BigDecimal.valueOf(cell.getNumericCellValue());
                break;
        }
        return retornar;
    }

    /**
     * Extrae los valores de la fila 1 para usarlas como nombres de las
     * columnas.
     *
     * @param sheet planilla excel.
     * @return Lista con los nombres de columnas.
     */
    public static List<String> getHeaderNames(Sheet sheet) {
        if (sheet == null) {
            return new ArrayList();
        }
        List<String> retornar = new ArrayList();
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            return Collections.EMPTY_LIST;
        }
        //Todas las celdas debe ser string
        short last = headerRow.getLastCellNum();
        for (int c = 0; c < last; c++) {
            Cell cell = headerRow.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            // Una cabecera vacía (definida pero en blanco) marca el fin de las columnas útiles
            if (cell.getCellType() == CellType.BLANK
                    || (cell.getCellType() == STRING && cell.getStringCellValue().trim().isEmpty())) {
                break;
            }
            // Si llegó hasta acá y no es texto, la cabecera es inválida
            if (cell.getCellType() != STRING) {
                return new ArrayList();
            }
            retornar.add(cell.getStringCellValue().trim());
        }
        return retornar;
    }

    /**
     * Verifica si el valor de la celda es asignable (o convertible) al tipo del
     * atributo de la clase destino.
     *
     * @param cell celda del excel.
     * @param fieldType tipo del atributo destino (DataInfo.getFieldType).
     * @param fieldName nombre del atributo destino (para el mensaje).
     * @return null si es asignable, o el detalle del inconveniente.
     */
    public static String getAssignableTypeError(Cell cell, Class<?> fieldType, String fieldName) {
        // El atributo no existe en la clase / no hay correlación válida
        if (fieldType == null) {
            return "La columna no corresponde a ningún atributo de la clase ('" + fieldName + "')";
        }
        String target = fieldType.getSimpleName();
        switch (cell.getCellType()) {
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return isDateType(fieldType) ? null
                            : "El valor es una fecha y el atributo '" + fieldName + "' es de tipo " + target;
                }
                double number = cell.getNumericCellValue();
                if (isNumericType(fieldType) || "String".equals(target)) {
                    if (isIntegerType(fieldType) && number != Math.floor(number)) {
                        return "El valor " + number + " tiene decimales y el atributo '"
                                + fieldName + "' es de tipo entero (" + target + ")";
                    }
                    return null;
                }
                if ("Boolean".equals(target)) {
                    return (number == 0 || number == 1) ? null
                            : "El valor numérico " + number + " no es convertible a Boolean en '" + fieldName + "'";
                }
                return "El valor numérico no es asignable al atributo '" + fieldName + "' de tipo " + target;

            case STRING:
                String text = cell.getStringCellValue();
                if ("String".equals(target)) {
                    return null;
                }
                if ("Character".equals(target)) {
                    return (text != null && text.length() == 1) ? null
                            : "El texto '" + text + "' no es asignable a Character en '" + fieldName + "'";
                }
                if (isNumericType(fieldType)) {
                    return isParsable(text, fieldType) ? null
                            : "El texto '" + text + "' no es convertible a " + target + " en '" + fieldName + "'";
                }
                if ("Boolean".equals(target)) {
                    return isParsableBoolean(text) ? null
                            : "El texto '" + text + "' no es convertible a Boolean en '" + fieldName + "'";
                }
                if (isDateType(fieldType)) {
                    return "El texto '" + text + "' no tiene formato de fecha para el atributo '" + fieldName + "'";
                }
                return null;

            case BOOLEAN:
                return ("Boolean".equals(target) || "String".equals(target)) ? null
                        : "El valor es booleano y el atributo '" + fieldName + "' es de tipo " + target;

            case FORMULA:
                // En el flujo actual la fórmula se evalúa como numérica
                return (isNumericType(fieldType) || "String".equals(target)) ? null
                        : "El resultado de la fórmula no es asignable al atributo '" + fieldName + "' de tipo " + target;

            default:
                return null; // BLANK / ERROR: lo maneja el flujo normal
        }
    }

    private static boolean isNumericType(Class<?> type) {
        String n = type.getSimpleName();
        return n.equals("BigDecimal") || n.equals("BigInteger") || n.equals("Long")
                || n.equals("Integer") || n.equals("Short") || n.equals("Double")
                || n.equals("Float") || n.equals("Byte");
    }

    private static boolean isIntegerType(Class<?> type) {
        String n = type.getSimpleName();
        return n.equals("Long") || n.equals("Integer") || n.equals("Short")
                || n.equals("Byte") || n.equals("BigInteger");
    }

    private static boolean isDateType(Class<?> type) {
        String n = type.getSimpleName();
        return n.equals("LocalDateTime") || n.equals("LocalDate")
                || n.equals("Date") || n.equals("Timestamp");
    }

    private static boolean isParsable(String text, Class<?> type) {
        if (Fn.nvl(text, "").trim().isEmpty()) {
            return true; // celda vacía -> null, lo maneja convertValue
        }
        try {
            convertValue(text.trim(), type); // reutiliza la lógica real de conversión
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isParsableBoolean(String text) {
        String v = Fn.nvl(text, "").trim().toLowerCase();
        return v.isEmpty() || v.equals("true") || v.equals("false") || v.equals("1") || v.equals("0");
    }
}
