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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
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
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.javabeanstack.data.IDataQueryModel;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.data.model.DataQueryModel;
import org.javabeanstack.util.Fn;
import org.javabeanstack.util.LocalDates;

/**
 *
 * Utilidad de Excel (Apache POI): apertura de libros, exportación de datos de
 * consulta ({@link org.javabeanstack.data.IDataQueryModel}) a un libro y su
 * descarga.
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
    private static final int MAX_SHEET_NAME = 31;  // tope de Excel para el nombre de una hoja
    private static final String SHEET_DEFAULT = "DATOS";  // nombre historico de la hoja unica

    /**
     * Factor de escala por fuente. Tus celdas usan 8pt; la unidad de Excel se
     * mide contra la fuente por defecto (~11pt). 1.0 = generoso (no se corta
     * nada); ~0.78 = columnas más ajustadas al 8pt real.
     */
    private static final double FONT_SCALE = 1.0;

    private static int[] computeColumnWidths(List<IDataQueryModel> data, String[] columnNames,
            Map<Integer, Integer> overrides) {
        int columnCount = columnNames.length;
        int[] widths = new int[columnCount];
        int sampleSize = Math.min(SAMPLE_ROWS, data.size());
        for (int j = 0; j < columnCount; j++) {
            // a) Override explícito (ancho "asignado previamente"): gana siempre
            if (overrides != null && overrides.containsKey(j)) {
                widths[j] = clampWidth(overrides.get(j));
                continue;
            }
            // b) Piso: que al menos entre el nombre de la cabecera
            int chars = Fn.nvl(columnNames[j], "").length();
            // c) Detectar tipo y, solo para String, muestrear longitud
            for (int i = 0; i < sampleSize; i++) {
                Object[] fila = (Object[]) data.get(i).getRow();
                if (j >= fila.length) {
                    continue;
                }
                Object v = fila[j];
                if (v == null) {
                    continue;
                }
                if (v instanceof Date || v instanceof LocalDate || v instanceof LocalDateTime) {
                    chars = Math.max(chars, WIDTH_DATE);   // Timestamp extiende Date
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

    /**
     * Envía un libro de Excel al navegador como descarga.
     *
     * @param workBook libro a descargar.
     * @param fileName nombre del archivo.
     * @throws Exception si la descarga falla.
     */
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

    /**
     * Abre un libro de Excel desde una ruta de archivo.
     *
     * @param filePath ruta del archivo.
     * @return libro abierto.
     * @throws IOException si no se puede leer el archivo.
     */
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

    /**
     * Abre un libro de Excel desde un archivo.
     *
     * @param file archivo.
     * @return libro abierto.
     * @throws IOException si no se puede leer el archivo.
     */
    public static Workbook openWorkbook(File file) throws IOException {
        Workbook wb = WorkbookFactory.create(file);
        return wb;
    }

    /**
     * Exporta una lista de filas de consulta a un libro de Excel.
     *
     * @param toExport datos a exportar.
     * @return libro generado.
     * @throws Exception si la exportación falla.
     */
    public static Workbook toExcel(List<IDataQueryModel> toExport) throws Exception {
        return toExcel(toExport, null);
    }

    /**
     * Exporta una lista de filas de consulta a un libro de Excel, con anchos de
     * columna personalizados.
     *
     * @param toExport datos a exportar.
     * @param widthOverrides anchos por índice de columna.
     * @return libro generado, o {@code null} si no hay datos.
     * @throws Exception si la exportación falla.
     */
    public static Workbook toExcel(List<IDataQueryModel> toExport, Map<Integer, Integer> widthOverrides) throws Exception {
        if (toExport == null || toExport.isEmpty()) {
            return null;
        }
        ExcelSheetData hoja = new ExcelSheetData(SHEET_DEFAULT, toExport)
                .setWidthOverrides(widthOverrides);
        return toExcelSheets(Collections.singletonList(hoja));
    }

    /**
     * Exporta VARIAS hojas de datos a un solo libro de Excel.
     *
     * <p>Las hojas se escriben en el orden en que vienen y cada una conserva su
     * nombre. Una hoja sin filas se crea igual —con sus cabeceras, si las
     * declaró—: la alternativa, saltearla, haría que un libro de tres pestañas
     * llegara con dos y nadie supiera si falta el dato o falló la
     * exportación.</p>
     *
     * <p>Los estilos se crean UNA vez por libro y no por hoja: Excel admite
     * 64.000 estilos y crearlos por hoja es la forma conocida de agotarlos.</p>
     *
     * @param sheets hojas a exportar.
     * @return libro generado, o {@code null} si no hay ninguna hoja.
     * @throws Exception si la exportación falla.
     */
    public static Workbook toExcelSheets(List<ExcelSheetData> sheets) throws Exception {
        if (sheets == null || sheets.isEmpty()) {
            return null;
        }
        SXSSFWorkbook workBook = new SXSSFWorkbook(100);
        workBook.setCompressTempFiles(true);
        SheetStyles styles = new SheetStyles(workBook);
        for (ExcelSheetData sheet : sheets) {
            writeSheet(workBook, styles, sheet);
        }
        return workBook;
    }

    /**
     * Escribe una hoja en el libro.
     *
     * @param workBook libro destino.
     * @param styles estilos del libro.
     * @param data datos de la hoja.
     */
    private static void writeSheet(SXSSFWorkbook workBook, SheetStyles styles, ExcelSheetData data) {
        SXSSFSheet sheet = workBook.createSheet(sheetName(workBook, data.getName()));
        // Sin autoSizeColumn -> NO hace falta trackAllColumnsForAutoSizing()
        String[] columnNames = data.getColumns();
        List<IDataQueryModel> toExport = data.getRows();
        int columnCount = columnNames.length;
        if (columnCount == 0) {
            return;
        }
        // 1) Calcular y fijar anchos ANTES de escribir
        int[] widthChars = computeColumnWidths(toExport, columnNames, data.getWidthOverrides());
        for (int j = 0; j < columnCount; j++) {
            sheet.setColumnWidth(j, charsToWidthUnits(widthChars[j]));
        }

        // 2) Cabecera
        int rownum = 0;
        Row row = sheet.createRow(rownum++);
        for (int j = 0; j < columnCount; j++) {
            Cell cell = row.createCell(j);
            cell.setCellStyle(styles.header);
            cell.setCellValue(columnNames[j]);
        }

        // 3) Datos
        for (int i = 0; i < toExport.size(); i++) {
            row = sheet.createRow(rownum++);
            Object[] fila = (Object[]) toExport.get(i).getRow();
            for (int j = 0; j < columnCount && j < fila.length; j++) {
                writeCell(row.createCell(j), fila[j], styles);
            }
        }
    }

    /**
     * Escribe un valor en una celda con el estilo que le corresponde a su tipo.
     *
     * <p>Los números y las fechas se escriben COMO números y fechas, no como
     * texto: una columna de importes que llega como texto no se puede sumar en
     * la planilla, que es a lo que se exporta.</p>
     *
     * @param cell celda destino.
     * @param valor valor a escribir.
     * @param styles estilos del libro.
     */
    private static void writeCell(Cell cell, Object valor, SheetStyles styles) {
        if (valor == null) {
            cell.setCellStyle(styles.text);
        } else if (valor instanceof BigDecimal) {
            cell.setCellStyle(styles.number);
            cell.setCellValue(((BigDecimal) valor).doubleValue());
        } else if (valor instanceof Date) {
            cell.setCellStyle(styles.dateTime);
            cell.setCellValue((Date) valor);
        } else if (valor instanceof LocalDateTime) {
            cell.setCellStyle(styles.dateTime);
            cell.setCellValue((LocalDateTime) valor);
        } else if (valor instanceof LocalDate) {
            cell.setCellStyle(styles.date);
            cell.setCellValue((LocalDate) valor);
        } else {
            cell.setCellStyle(styles.text);
            cell.setCellValue(String.valueOf(valor));
        }
    }

    /**
     * Devuelve un nombre de hoja admisible y no repetido.
     *
     * <p>Excel no admite más de 31 caracteres ni los caracteres
     * {@code []:*?/\}, y rechaza el libro entero —no la hoja— si se los
     * ponen. Un nombre repetido hace que POI falle al crear la segunda.</p>
     *
     * @param workBook libro destino.
     * @param name nombre pedido.
     * @return el nombre a usar.
     */
    private static String sheetName(Workbook workBook, String name) {
        String limpio = WorkbookUtil.createSafeSheetName(Fn.nvl(name, SHEET_DEFAULT));
        if (limpio.trim().isEmpty()) {
            limpio = SHEET_DEFAULT;
        }
        String candidato = limpio;
        int sufijo = 2;
        while (workBook.getSheet(candidato) != null) {
            String numero = " (" + sufijo + ")";
            int corte = Math.min(limpio.length(), MAX_SHEET_NAME - numero.length());
            candidato = limpio.substring(0, corte) + numero;
            sufijo++;
        }
        return candidato;
    }

    /**
     * Escribe el libro en un flujo de salida y libera sus temporales.
     *
     * <p>Es la escritura NEUTRA: {@link #downLoadFile(Workbook, String)} sirve
     * a JSF y no se puede usar fuera de él —depende de {@code FacesContext}—,
     * así que un recurso REST o un proceso de fondo necesitan esta.</p>
     *
     * <p>El {@code dispose()} no es optativo: sin él, cada exportación deja en
     * el directorio temporal del servidor los archivos que SXSSF creó para no
     * cargar el libro entero en memoria.</p>
     *
     * @param workBook libro a escribir.
     * @param output flujo destino; no se cierra.
     * @throws Exception si la escritura falla.
     */
    public static void write(Workbook workBook, OutputStream output) throws Exception {
        if (workBook == null || output == null) {
            return;
        }
        try (workBook) {
            workBook.write(output);
            output.flush();
        } finally {
            if (workBook instanceof SXSSFWorkbook) {
                ((SXSSFWorkbook) workBook).dispose();
            }
        }
    }

    /**
     * Devuelve el libro como un arreglo de bytes y libera sus temporales.
     *
     * @param workBook libro a convertir.
     * @return los bytes del archivo, o {@code null} si no hay libro.
     * @throws Exception si la escritura falla.
     */
    public static byte[] toBytes(Workbook workBook) throws Exception {
        if (workBook == null) {
            return null;
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            write(workBook, output);
            return output.toByteArray();
        }
    }

    /**
     * Los estilos de un libro, creados una sola vez.
     */
    private static class SheetStyles {

        private final CellStyle text;
        private final CellStyle number;
        private final CellStyle dateTime;
        private final CellStyle date;
        private final CellStyle header;

        SheetStyles(Workbook workBook) {
            DataFormat dataFormat = workBook.createDataFormat();
            Font font8 = workBook.createFont();
            font8.setFontHeightInPoints((short) 8);
            Font fontBold = workBook.createFont();
            fontBold.setFontHeightInPoints((short) 8);
            fontBold.setBold(true);

            text = workBook.createCellStyle();
            text.setFont(font8);
            number = workBook.createCellStyle();
            number.setFont(font8);
            number.setDataFormat(dataFormat.getFormat("#,##0.00"));
            dateTime = workBook.createCellStyle();
            dateTime.setFont(font8);
            dateTime.setDataFormat(dataFormat.getFormat("dd/mm/yyyy hh:mm:ss"));
            date = workBook.createCellStyle();
            date.setFont(font8);
            date.setDataFormat(dataFormat.getFormat("dd/mm/yyyy"));
            header = workBook.createCellStyle();
            header.setFont(fontBold);
        }
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
        T dataRow;
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
            dataRow = processor.process();
            if (dataRow != null){
                retornar.add(dataRow);                
            }
        }
        return retornar;
    }


    /**
     * Convierte un valor de la celda de la planilla a un valor asignable en un
     * atributo de una clase de java.
     * <p>
     * Conversiones soportadas:
     * <ul>
     * <li>{@code null} y tipos ya coincidentes: se retornan sin cambios.</li>
     * <li>Numéricos: {@code BigDecimal}, {@code Long}, {@code Integer},
     * {@code Short} (vía {@code BigDecimal}; {@code Long/Integer/Short} de forma
     * exacta, lanza excepción si hay pérdida de información).</li>
     * <li>{@code Character}: primer caracter del texto.</li>
     * <li>{@code Boolean}: {@code true} si el valor es {@code "1"} o
     * {@code "true"} (ignorando mayúsculas).</li>
     * <li>{@code LocalDateTime}: desde {@code Date}, {@code Timestamp} o
     * {@code String} (este último con formato {@code dd/MM/yyyy HH:mm:ss},
     * ver {@link org.javabeanstack.util.LocalDates#toDateTime(String)}).</li>
     * </ul>
     * Para cualquier otra combinación el valor se retorna sin transformar (en
     * particular {@code String} hacia {@code LocalDate}/{@code Date}/
     * {@code Timestamp} aún no está contemplado).
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
                    // convertValue soporta String -> fecha (ver LocalDates.toDateTime);
                    // se valida con la misma lógica real de conversión.
                    return isParsable(text, fieldType) ? null
                            : "El texto '" + text + "' no tiene formato de fecha para el atributo '" + fieldName + "'";
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

    /**
     * Indica si el tipo es numérico (entero o decimal).
     *
     * @param type tipo a evaluar.
     * @return {@code true} si es un tipo numérico soportado.
     */
    private static boolean isNumericType(Class<?> type) {
        String n = type.getSimpleName();
        return n.equals("BigDecimal") || n.equals("BigInteger") || n.equals("Long")
                || n.equals("Integer") || n.equals("Short") || n.equals("Double")
                || n.equals("Float") || n.equals("Byte");
    }

    /**
     * Indica si el tipo es numérico entero (no admite decimales).
     *
     * @param type tipo a evaluar.
     * @return {@code true} si es un tipo entero.
     */
    private static boolean isIntegerType(Class<?> type) {
        String n = type.getSimpleName();
        return n.equals("Long") || n.equals("Integer") || n.equals("Short")
                || n.equals("Byte") || n.equals("BigInteger");
    }

    /**
     * Indica si el tipo representa una fecha u hora.
     *
     * @param type tipo a evaluar.
     * @return {@code true} si es {@code LocalDateTime}, {@code LocalDate},
     * {@code Date} o {@code Timestamp}.
     */
    private static boolean isDateType(Class<?> type) {
        String n = type.getSimpleName();
        return n.equals("LocalDateTime") || n.equals("LocalDate")
                || n.equals("Date") || n.equals("Timestamp");
    }

    /**
     * Verifica si el texto puede convertirse al tipo indicado reutilizando la
     * lógica real de {@link #convertValue(Object, Class)}. Un texto vacío se
     * considera parseable (deriva en {@code null}).
     *
     * @param text texto a evaluar.
     * @param type tipo destino.
     * @return {@code true} si la conversión no arroja excepción.
     */
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

    /**
     * Indica si el texto representa un valor booleano admitido
     * ({@code true}/{@code false}/{@code 1}/{@code 0}, o vacío).
     *
     * @param text texto a evaluar.
     * @return {@code true} si es convertible a {@code Boolean}.
     */
    private static boolean isParsableBoolean(String text) {
        String v = Fn.nvl(text, "").trim().toLowerCase();
        return v.isEmpty() || v.equals("true") || v.equals("false") || v.equals("1") || v.equals("0");
    }
}
