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
package org.javabeanstack.poi.excel;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/**
 * Escritor <b>incremental</b> de una hoja de Excel ({@code .xlsx}): recibe las
 * filas de a una, con {@link #writeRow(Object[])}, y nunca necesita tenerlas
 * todas juntas.
 *
 * <p><b>Por qué existe.</b> {@link ExcelUtil#toExcelSheets(java.util.List)}
 * también escribe con SXSSF, pero recibe <b>todas</b> las filas en una lista:
 * para exportar un conjunto grande hay que leerlo entero a memoria antes de
 * empezar. Con este escritor quien lee por lotes escribe cada lote y lo
 * suelta; en memoria quedan solo la ventana de filas de SXSSF
 * ({@value #ROW_WINDOW}) y el lote en curso. Las filas que salen de la ventana
 * van a un temporal comprimido que {@link #close()} borra.</p>
 *
 * <p><b>Tipos de celda.</b> El estilo y el tipo de cada celda los decide el
 * <b>tipo Java declarado</b> de la columna, no el valor: así una columna de
 * texto nunca se convierte en número porque un valor «parezca» numérico
 * ({@code "001"} sigue siendo {@code "001"}). La regla:</p>
 * <table>
 * <caption>Tipo declarado de la columna y celda que se escribe</caption>
 * <tr><th>Tipo declarado</th><th>Celda</th></tr>
 * <tr><td>{@code String}, {@code Character}</td><td>texto con formato de
 * texto ({@code @}); POI conserva los espacios finales; recortado a
 * {@value #MAX_TEXT_LENGTH} caracteres (ver «Lo que Excel no puede
 * guardar»)</td></tr>
 * <tr><td>{@code Long}, {@code Integer}, {@code Short}, {@code Byte},
 * {@code BigInteger}</td><td>numérica, formato {@code 0}; texto si el valor
 * tiene más de 15 dígitos significativos (ver «Precisión»)</td></tr>
 * <tr><td>{@code BigDecimal}, {@code Double}, {@code Float}</td><td>numérica,
 * sin máscara (formato General), salvo la que se fije con
 * {@link #setColumnFormat(int, String)}; {@code NaN} e infinito, celda
 * vacía</td></tr>
 * <tr><td>{@code LocalDateTime}, {@code java.util.Date} y
 * subclases</td><td>fecha {@code dd/mm/yyyy hh:mm:ss}, el estilo de
 * {@link ExcelUtil}; fuera del rango de Excel, texto
 * {@code dd/MM/yyyy HH:mm:ss}</td></tr>
 * <tr><td>{@code LocalDate}, {@code java.sql.Date}</td><td>fecha
 * {@code dd/mm/yyyy}; fuera del rango de Excel, texto {@code dd/MM/yyyy}</td></tr>
 * <tr><td>{@code java.sql.Time}, {@code LocalTime}</td><td>hora: la fracción
 * del día, que es como Excel guarda una hora sola, con formato
 * {@code hh:mm:ss}</td></tr>
 * <tr><td>{@code Boolean}</td><td>texto {@code true} / {@code false}, como
 * {@link ExcelUtil}</td></tr>
 * <tr><td>{@code byte[]}</td><td>no admitido: el constructor lo
 * rechaza</td></tr>
 * <tr><td>{@code null} u {@code Object}</td><td>según el valor de cada celda,
 * con las mismas reglas</td></tr>
 * <tr><td>cualquier otro</td><td>texto ({@code String.valueOf})</td></tr>
 * </table>
 * <p>Un valor nulo deja la celda vacía. Un valor que no corresponde al tipo
 * declarado (un texto en una columna entera) se escribe según su propio tipo,
 * como lo haría {@link ExcelUtil}: no se descarta ni se convierte.</p>
 *
 * <p><b>Precisión.</b> Excel guarda los números como {@code double} y
 * muestra 15 dígitos significativos. Un <b>entero</b> ({@code Long},
 * {@code BigInteger}) con más de {@value #MAX_NUMERIC_DIGITS} dígitos
 * significativos (los ceros finales no cuentan) se escribe por eso como
 * <b>texto</b>, con todos sus dígitos: como número volvería redondeado. La
 * importación del framework lo lee igual, porque convierte el texto al tipo
 * entero del atributo. Un decimal más largo no vuelve igual al leerlo; si una
 * columna lo necesita, hay que declararla {@code String}.</p>
 *
 * <p><b>Lo que Excel no puede guardar.</b> Una planilla tiene límites que un
 * dato de la base puede superar; el escritor no aborta la fila por eso, sino
 * que aplica una regla fija y <b>cuenta</b> cada celda afectada, para que
 * quien exporta lo informe:</p>
 * <ul>
 * <li><b>texto de más de {@value #MAX_TEXT_LENGTH} caracteres</b> (el máximo
 * de una celda): se recorta a ese largo, sin partir un carácter de dos
 * unidades; {@link #getTruncatedCellCount()};</li>
 * <li><b>fecha u hora anterior al 1900-01-01 o posterior al 9999-12-31</b>
 * (fuera del calendario de Excel, que la guardaría como {@code -1}): se
 * escribe como texto <b>en el formato que lee la importación del
 * framework</b> ({@code LocalDates.toDateTime(String)}): {@code dd/MM/yyyy}
 * para una fecha sola ({@code LocalDate}, {@code java.sql.Date}) y
 * {@code dd/MM/yyyy HH:mm:ss} para una fecha con hora, sin fracciones de
 * segundo (una fecha sola con año de cinco cifras va con la hora
 * {@code 00:00:00}, porque la importación solo completa la hora de un texto de
 * diez caracteres); {@link #getOutOfRangeDateCount()}. Así la celda vuelve como fecha y
 * hora por {@link ExcelRowProcessor} (la fecha sola, a las 00:00:00). Los años
 * anteriores al 1 de la era común no vuelven: el formato no lleva era;</li>
 * <li><b>{@code NaN} e infinito</b> ({@code Double}, {@code Float}): Excel no
 * los representa; la celda queda vacía; {@link #getInvalidNumberCount()}.</li>
 * </ul>
 *
 * <p><b>Formato de texto.</b> Las celdas que el escritor emite como texto
 * llevan el formato de texto de Excel ({@code @}): así, si alguien edita la
 * celda en Excel y la confirma, no la convierte en número ({@code 001} no
 * pasa a {@code 1} ni un entero largo pierde dígitos).</p>
 *
 * <p><b>Encabezados.</b> Tienen que ser textos únicos y no vacíos, que son las
 * condiciones con que la importación del framework
 * ({@link ExcelRowProcessor#checkMetaData()}) acepta una planilla; el
 * constructor lo verifica. Como la importación los compara sin los espacios
 * de los extremos, el escritor también: los recorta antes de compararlos y los
 * escribe recortados ({@code "a"} y {@code "a "} son el mismo encabezado). La fila de encabezados puede no ser la primera
 * ({@link #ExcelSheetWriter(String, String[], Class[], int)}): las filas de
 * arriba quedan vacías y los datos empiezan en la siguiente.</p>
 *
 * <p><b>Anchos.</b> Se fijan al abrir la hoja, por encabezado y tipo
 * declarado, sin {@code autoSizeColumn} (que obligaría a seguir todas las
 * filas en memoria); {@link #setColumnWidth(int, int)} los cambia.</p>
 *
 * <p>Uso típico:</p>
 * <pre>
 * try (ExcelSheetWriter writer = new ExcelSheetWriter("Clientes",
 *         new String[]{"codigo", "nombre", "saldo"},
 *         new Class&lt;?&gt;[]{String.class, String.class, BigDecimal.class});
 *      OutputStream out = Files.newOutputStream(archivo)) {
 *     for (Object[] fila : lote) {
 *         writer.writeRow(fila);
 *     }
 *     writer.writeTo(out);
 * }
 * </pre>
 *
 * <p>No es seguro para hilos: una instancia escribe una hoja desde un solo
 * hilo.</p>
 *
 * @author Jorge Enciso
 */
public class ExcelSheetWriter implements Closeable {

    /** Filas que SXSSF mantiene en memoria antes de bajarlas al temporal. */
    public static final int ROW_WINDOW = 100;

    /**
     * Dígitos significativos que Excel conserva en un número; un entero más
     * largo se escribe como texto.
     */
    public static final int MAX_NUMERIC_DIGITS = 15;

    /**
     * Largo máximo del texto de una celda de Excel (el de
     * {@code SpreadsheetVersion.EXCEL2007.getMaxTextLength()}).
     */
    public static final int MAX_TEXT_LENGTH = 32767;

    /** Primer instante que el calendario de Excel (sistema 1900) representa. */
    private static final LocalDateTime EXCEL_MIN_DATE = LocalDateTime.of(1900, 1, 1, 0, 0);
    /** Primer instante posterior al último día que Excel representa. */
    private static final LocalDateTime EXCEL_END_DATE = LocalDateTime.of(10000, 1, 1, 0, 0);
    /**
     * Fecha sola fuera de rango, como texto: el formato que acepta la
     * importación ({@code LocalDates.toDateTime(String)} le agrega la hora
     * 00:00:00 a un texto de diez caracteres).
     */
    private static final DateTimeFormatter TEXT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /**
     * Fecha y hora fuera de rango, como texto: el mismo patrón con que la
     * importación las lee (sin fracciones de segundo).
     */
    private static final DateTimeFormatter TEXT_DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /** Nanosegundos de un día, para pasar una hora a fracción del día. */
    private static final long NANOS_PER_DAY = 86_400_000_000_000L;

    /** Ancho por omisión, en caracteres, de una columna de texto. */
    private static final int WIDTH_TEXT = 12;
    /** Ancho de una fecha sin hora ({@code dd/mm/yyyy}). */
    private static final int WIDTH_DATE_ONLY = 10;
    /** Ancho de una hora ({@code hh:mm:ss}). */
    private static final int WIDTH_TIME = 8;
    /** Ancho de un booleano ({@code false}). */
    private static final int WIDTH_BOOLEAN = 5;

    /** Clase de celda que corresponde a un tipo Java. */
    private enum Kind {
        TEXT, INTEGER, DECIMAL, DATETIME, DATE, TIME, BOOLEAN, AUTO
    }

    private final SXSSFWorkbook workBook;
    private final SXSSFSheet sheet;
    private final ExcelUtil.SheetStyles styles;
    private final CellStyle integerStyle;
    private final CellStyle decimalStyle;
    private final CellStyle textStyle;
    private final CellStyle timeStyle;
    private final String[] headers;
    private final Kind[] kinds;
    private final CellStyle[] columnFormats;
    private final Map<String, CellStyle> formatCache = new HashMap<>();
    private final int headerRowIndex;
    private int nextRow;
    private int rowCount;
    private int truncatedCells;
    private int outOfRangeDates;
    private int invalidNumbers;
    private boolean written;
    private boolean closed;

    /**
     * Abre la hoja con los encabezados en la primera fila.
     *
     * @param sheetName nombre de la hoja; se sanea con la regla de
     * {@link ExcelUtil} (31 caracteres como máximo, sin {@code []:*?/\}) y,
     * si es nulo o queda vacío, se usa {@code DATOS}.
     * @param headers encabezados de las columnas, en orden: textos únicos y
     * no vacíos; se comparan y se escriben sin los espacios de los extremos.
     * @param types tipo Java de cada columna, en el mismo orden; puede ser
     * {@code null} (todas según el valor) y un elemento {@code null} vale lo
     * mismo para esa columna.
     * @throws IllegalArgumentException si no hay encabezados, hay uno nulo,
     * vacío o repetido (sin contar los espacios de los extremos), la cantidad de tipos no coincide con la de
     * encabezados o una columna es {@code byte[]}.
     */
    public ExcelSheetWriter(String sheetName, String[] headers, Class<?>[] types) {
        this(sheetName, headers, types, 0);
    }

    /**
     * Abre la hoja con los encabezados en la fila indicada.
     *
     * <p>Sirve para producir una planilla que una importación lea con su fila
     * de encabezados configurada: las filas anteriores quedan vacías y los
     * datos empiezan en la fila siguiente a la de encabezados.</p>
     *
     * @param sheetName nombre de la hoja; ver
     * {@link #ExcelSheetWriter(String, String[], Class[])}.
     * @param headers encabezados de las columnas, en orden.
     * @param types tipo Java de cada columna, en el mismo orden, o
     * {@code null}.
     * @param headerRowIndex índice (base 0) de la fila de encabezados.
     * @throws IllegalArgumentException si los encabezados o los tipos no son
     * válidos (ver el otro constructor) o el índice está fuera de la hoja.
     */
    public ExcelSheetWriter(String sheetName, String[] headers, Class<?>[] types, int headerRowIndex) {
        this.kinds = validate(headers, types, headerRowIndex);
        this.headers = new String[headers.length];
        for (int j = 0; j < headers.length; j++) {
            this.headers[j] = headers[j].trim();
        }
        this.headerRowIndex = headerRowIndex;
        this.columnFormats = new CellStyle[headers.length];
        this.workBook = new SXSSFWorkbook(ROW_WINDOW);
        this.workBook.setCompressTempFiles(true);
        this.styles = new ExcelUtil.SheetStyles(workBook);
        this.integerStyle = createFormatStyle("0");
        this.decimalStyle = createFormatStyle("General");
        this.textStyle = createFormatStyle("@");
        this.timeStyle = createFormatStyle("hh:mm:ss");
        // Sin autoSizeColumn -> no hace falta trackAllColumnsForAutoSizing()
        this.sheet = workBook.createSheet(ExcelUtil.sheetName(workBook, sheetName));
        for (int j = 0; j < this.headers.length; j++) {
            sheet.setColumnWidth(j, ExcelUtil.charsToWidthUnits(initialWidth(this.headers[j], kinds[j])));
        }
        Row row = sheet.createRow(headerRowIndex);
        for (int j = 0; j < this.headers.length; j++) {
            Cell cell = row.createCell(j);
            cell.setCellStyle(styles.header);
            cell.setCellValue(this.headers[j]);
        }
        this.nextRow = headerRowIndex + 1;
    }

    /**
     * Verifica los argumentos del constructor antes de crear el libro (así un
     * argumento inválido no deja temporales) y resuelve la clase de celda de
     * cada columna.
     *
     * @param headers encabezados.
     * @param types tipos declarados, o {@code null}.
     * @param headerRowIndex índice de la fila de encabezados.
     * @return la clase de celda de cada columna.
     */
    private static Kind[] validate(String[] headers, Class<?>[] types, int headerRowIndex) {
        if (headers == null || headers.length == 0) {
            throw new IllegalArgumentException("La hoja necesita al menos una columna");
        }
        if (types != null && types.length != headers.length) {
            throw new IllegalArgumentException("Se declararon " + types.length + " tipos para "
                    + headers.length + " encabezados");
        }
        int maxColumns = SpreadsheetVersion.EXCEL2007.getMaxColumns();
        if (headers.length > maxColumns) {
            throw new IllegalArgumentException("Una hoja admite hasta " + maxColumns + " columnas");
        }
        int lastRow = SpreadsheetVersion.EXCEL2007.getLastRowIndex();
        if (headerRowIndex < 0 || headerRowIndex >= lastRow) {
            throw new IllegalArgumentException("Fila de encabezados fuera de la hoja: " + headerRowIndex);
        }
        Set<String> seen = new HashSet<>();
        Kind[] result = new Kind[headers.length];
        for (int j = 0; j < headers.length; j++) {
            String header = headers[j];
            if (header == null || header.trim().isEmpty()) {
                throw new IllegalArgumentException("El encabezado de la columna " + (j + 1) + " está vacío");
            }
            if (!seen.add(header.trim())) {
                throw new IllegalArgumentException("Encabezado repetido: «" + header + "»");
            }
            Class<?> type = (types == null) ? null : types[j];
            if (type == byte[].class) {
                throw new IllegalArgumentException("La columna «" + header
                        + "» es binaria (byte[]) y no se puede escribir en una planilla");
            }
            result[j] = kindOf(type);
        }
        return result;
    }

    /**
     * Devuelve la clase de celda que corresponde a un tipo Java.
     *
     * @param type tipo Java; {@code null} u {@code Object} significan «según
     * el valor».
     * @return la clase de celda.
     */
    private static Kind kindOf(Class<?> type) {
        if (type == null || type == Object.class) {
            return Kind.AUTO;
        }
        if (type == String.class || type == Character.class || type == char.class) {
            return Kind.TEXT;
        }
        if (type == Long.class || type == Integer.class || type == Short.class || type == Byte.class
                || type == BigInteger.class || type == long.class || type == int.class
                || type == short.class || type == byte.class) {
            return Kind.INTEGER;
        }
        if (type == BigDecimal.class || type == Double.class || type == Float.class
                || type == double.class || type == float.class) {
            return Kind.DECIMAL;
        }
        // java.sql.Time es un Date: se mira antes que las fechas.
        if (type == java.sql.Time.class || type == LocalTime.class) {
            return Kind.TIME;
        }
        if (type == LocalDate.class || type == java.sql.Date.class) {
            return Kind.DATE;
        }
        if (type == LocalDateTime.class || Date.class.isAssignableFrom(type)) {
            return Kind.DATETIME;
        }
        if (type == Boolean.class || type == boolean.class) {
            return Kind.BOOLEAN;
        }
        return Kind.TEXT;
    }

    /**
     * Ancho inicial de una columna, en caracteres: el mayor entre el
     * encabezado y el que pide su tipo, acotado como en {@link ExcelUtil}.
     *
     * @param header encabezado.
     * @param kind clase de celda declarada.
     * @return ancho en caracteres.
     */
    private static int initialWidth(String header, Kind kind) {
        int chars = header.length();
        switch (kind) {
            case INTEGER:
            case DECIMAL:
                chars = Math.max(chars, ExcelUtil.WIDTH_NUMBER);
                break;
            case DATETIME:
                chars = Math.max(chars, ExcelUtil.WIDTH_DATE);
                break;
            case DATE:
                chars = Math.max(chars, WIDTH_DATE_ONLY);
                break;
            case TIME:
                chars = Math.max(chars, WIDTH_TIME);
                break;
            case BOOLEAN:
                chars = Math.max(chars, WIDTH_BOOLEAN) + ExcelUtil.STRING_PAD;
                break;
            default:
                chars = Math.max(chars + ExcelUtil.STRING_PAD, WIDTH_TEXT);
        }
        return ExcelUtil.clampWidth(chars);
    }

    /**
     * Crea un estilo con la fuente de los datos y el formato indicado.
     *
     * @param format formato de Excel.
     * @return el estilo.
     */
    private CellStyle createFormatStyle(String format) {
        CellStyle style = workBook.createCellStyle();
        style.cloneStyleFrom(styles.text);
        style.setDataFormat(workBook.createDataFormat().getFormat(format));
        return style;
    }

    /**
     * Fija la máscara de presentación de una columna numérica, de fecha o de
     * hora (por
     * ejemplo {@code #,##0.00} o {@code dd/mm/yyyy}). Rige para las filas que
     * se escriban después; en las columnas de texto o booleanas no tiene
     * efecto. Solo cambia cómo se ve el número: el valor de la celda es el
     * mismo.
     *
     * <p>Los estilos se crean una vez por formato distinto, no por columna:
     * Excel admite un número limitado de estilos por libro.</p>
     *
     * @param column índice de la columna (base 0).
     * @param format formato de Excel; {@code null} o vacío vuelve al formato
     * por omisión del tipo.
     * @throws IllegalArgumentException si la columna no existe.
     * @throws IllegalStateException si el escritor ya se cerró.
     */
    public void setColumnFormat(int column, String format) {
        checkOpen();
        checkColumn(column);
        if (format == null || format.trim().isEmpty()) {
            columnFormats[column] = null;
            return;
        }
        columnFormats[column] = formatCache.computeIfAbsent(format, this::createFormatStyle);
    }

    /**
     * Cambia el ancho de una columna. Se puede llamar en cualquier momento
     * antes de {@link #writeTo(OutputStream)}.
     *
     * @param column índice de la columna (base 0).
     * @param chars ancho en caracteres; se acota como en {@link ExcelUtil}.
     * @throws IllegalArgumentException si la columna no existe.
     * @throws IllegalStateException si el escritor ya se cerró.
     */
    public void setColumnWidth(int column, int chars) {
        checkOpen();
        checkColumn(column);
        sheet.setColumnWidth(column, ExcelUtil.charsToWidthUnits(ExcelUtil.clampWidth(chars)));
    }

    /**
     * Escribe una fila de datos a continuación de la anterior.
     *
     * @param values valores de la fila, en el orden de los encabezados. Puede
     * traer menos valores que columnas (las que faltan quedan vacías), pero no
     * más. Un valor que Excel no puede guardar (texto demasiado largo, fecha
     * fuera de su calendario, {@code NaN}) no rechaza la fila: se aplica la
     * regla de «Lo que Excel no puede guardar» y se cuenta.
     * @throws IllegalArgumentException si {@code values} es nulo, trae más
     * valores que columnas o un valor es {@code byte[]}.
     * @throws IllegalStateException si ya se escribió el libro, el escritor
     * se cerró o la hoja llegó al máximo de filas de Excel.
     */
    public void writeRow(Object[] values) {
        checkOpen();
        if (written) {
            throw new IllegalStateException("El libro ya se escribió: no admite más filas");
        }
        if (values == null) {
            throw new IllegalArgumentException("La fila es nula");
        }
        if (values.length > headers.length) {
            throw new IllegalArgumentException("La fila trae " + values.length + " valores para "
                    + headers.length + " columnas");
        }
        if (nextRow > SpreadsheetVersion.EXCEL2007.getLastRowIndex()) {
            throw new IllegalStateException("La hoja llegó al máximo de filas de Excel ("
                    + SpreadsheetVersion.EXCEL2007.getMaxRows() + ")");
        }
        // Se revisa antes de crear la fila: una fila a medio escribir dejaría
        // ocupado su número en la hoja.
        for (int j = 0; j < values.length; j++) {
            if (values[j] instanceof byte[]) {
                throw new IllegalArgumentException("La columna «" + headers[j]
                        + "» recibió un valor binario (byte[])");
            }
        }
        Row row = sheet.createRow(nextRow);
        for (int j = 0; j < values.length; j++) {
            Object value = values[j];
            if (value != null) {
                writeCell(row.createCell(j), value, j);
            }
        }
        nextRow++;
        rowCount++;
    }

    /**
     * Escribe el valor de una celda según el tipo declarado de su columna.
     *
     * <p>Si el valor no corresponde al tipo declarado (un texto en una columna
     * entera) se escribe según su propio tipo: no se descarta ni se
     * convierte.</p>
     *
     * @param cell celda destino.
     * @param value valor, no nulo.
     * @param column índice de la columna.
     */
    private void writeCell(Cell cell, Object value, int column) {
        Kind kind = kinds[column];
        if (kind == Kind.AUTO || !accepts(kind, value)) {
            kind = kindOf(value.getClass());
        }
        switch (kind) {
            case INTEGER:
            case DECIMAL:
                writeNumber(cell, (Number) value, kind, column);
                return;
            case DATETIME:
            case DATE:
                writeDate(cell, value, column);
                return;
            case TIME:
                writeTime(cell, value, column);
                return;
            default:
                // Texto, booleano o cualquier otro tipo: su texto.
                writeText(cell, String.valueOf(value));
        }
    }

    /**
     * Indica si el valor corresponde a la clase de celda declarada.
     *
     * @param kind clase de celda declarada.
     * @param value valor, no nulo.
     * @return {@code true} si se puede escribir con esa clase de celda.
     */
    private static boolean accepts(Kind kind, Object value) {
        switch (kind) {
            case TEXT:
                return true;
            case INTEGER:
            case DECIMAL:
                return value instanceof Number;
            case DATETIME:
            case DATE:
                // java.sql.Time no es una fecha (no admite toInstant()): va como hora.
                return (value instanceof Date && !(value instanceof java.sql.Time))
                        || value instanceof LocalDateTime || value instanceof LocalDate;
            case TIME:
                return value instanceof java.sql.Time || value instanceof LocalTime;
            case BOOLEAN:
                return value instanceof Boolean;
            default:
                return false;
        }
    }

    /**
     * Escribe un texto con formato de texto, recortado al largo máximo de una
     * celda (y contado, si se recortó).
     *
     * @param cell celda destino.
     * @param text texto.
     */
    private void writeText(Cell cell, String text) {
        if (text.length() > MAX_TEXT_LENGTH) {
            int end = MAX_TEXT_LENGTH;
            // No partir un carácter de dos unidades (fuera del plano básico).
            if (Character.isHighSurrogate(text.charAt(end - 1))) {
                end--;
            }
            text = text.substring(0, end);
            truncatedCells++;
        }
        cell.setCellStyle(textStyle);
        cell.setCellValue(text);
    }

    /**
     * Escribe un número: como número, salvo un entero de más de 15 dígitos
     * significativos (texto) o un {@code NaN} o infinito (celda vacía).
     *
     * @param cell celda destino.
     * @param number número.
     * @param kind clase de celda (entera o decimal).
     * @param column índice de la columna.
     */
    private void writeNumber(Cell cell, Number number, Kind kind, int column) {
        if ((number instanceof Double && !Double.isFinite(number.doubleValue()))
                || (number instanceof Float && !Float.isFinite(number.floatValue()))) {
            // Excel no representa NaN ni infinito: celda vacía.
            invalidNumbers++;
            return;
        }
        if (exceedsNumericPrecision(number)) {
            // Entero de más de 15 dígitos significativos: como número Excel lo
            // redondearía (ver «Precisión»).
            writeText(cell, number.toString());
            return;
        }
        CellStyle style = columnFormats[column];
        if (style == null) {
            style = (kind == Kind.INTEGER) ? integerStyle : decimalStyle;
        }
        cell.setCellStyle(style);
        // El único tipo numérico de una celda es double (ver la nota de
        // precisión de la clase).
        cell.setCellValue(number.doubleValue());
    }

    /**
     * Escribe una hora sola como lo guarda Excel: la fracción del día (0 a 1),
     * con formato {@code hh:mm:ss}, o con la máscara fijada para la columna.
     *
     * @param cell celda destino.
     * @param value {@code java.sql.Time} o {@code LocalTime}.
     * @param column índice de la columna.
     */
    private void writeTime(Cell cell, Object value, int column) {
        // java.sql.Time no admite toInstant(): se pasa por su LocalTime.
        LocalTime time = (value instanceof java.sql.Time)
                ? ((java.sql.Time) value).toLocalTime()
                : (LocalTime) value;
        cell.setCellStyle(columnFormats[column] != null ? columnFormats[column] : timeStyle);
        cell.setCellValue(time.toNanoOfDay() / (double) NANOS_PER_DAY);
    }

    /**
     * Escribe una fecha con el estilo de {@link ExcelUtil}, o como texto en el
     * formato que lee la importación
     * si cae fuera del calendario de Excel (y la cuenta).
     *
     * @param cell celda destino.
     * @param value {@code Date}, {@code LocalDateTime} o {@code LocalDate}.
     * @param column índice de la columna.
     */
    private void writeDate(Cell cell, Object value, int column) {
        LocalDateTime instant;
        String text;
        if (value instanceof LocalDate) {
            instant = ((LocalDate) value).atStartOfDay();
            text = TEXT_DATE.format((LocalDate) value);
        } else if (value instanceof java.sql.Date) {
            // java.sql.Date no admite toInstant(): se pasa por su LocalDate.
            LocalDate date = ((java.sql.Date) value).toLocalDate();
            instant = date.atStartOfDay();
            text = TEXT_DATE.format(date);
        } else if (value instanceof java.sql.Timestamp) {
            instant = ((java.sql.Timestamp) value).toLocalDateTime();
            text = TEXT_DATE_TIME.format(instant);
        } else if (value instanceof Date) {
            // Misma zona que usa POI para escribir un Date.
            instant = LocalDateTime.ofInstant(((Date) value).toInstant(), ZoneId.systemDefault());
            text = TEXT_DATE_TIME.format(instant);
        } else {
            instant = (LocalDateTime) value;
            text = TEXT_DATE_TIME.format(instant);
        }
        if (instant.isBefore(EXCEL_MIN_DATE) || !instant.isBefore(EXCEL_END_DATE)) {
            outOfRangeDates++;
            if (text.length() != 10 && (value instanceof LocalDate || value instanceof java.sql.Date)) {
                // La importación completa con 00:00:00 solo un texto de diez
                // caracteres: un año de cinco cifras va con la hora explícita.
                text = TEXT_DATE_TIME.format(instant);
            }
            writeText(cell, text);
            return;
        }
        if (value instanceof java.sql.Date) {
            cell.setCellStyle(styles.date);
            cell.setCellValue(((java.sql.Date) value).toLocalDate());
        } else {
            ExcelUtil.writeCell(cell, value, styles);
        }
        if (columnFormats[column] != null) {
            cell.setCellStyle(columnFormats[column]);
        }
    }

    /**
     * Indica si un entero tiene más dígitos significativos de los que Excel
     * conserva. Solo mira los enteros ({@code Long}, {@code BigInteger}; los
     * {@code Integer}, {@code Short} y {@code Byte} no llegan a 15 dígitos):
     * los decimales siguen siempre como número.
     *
     * @param number número a escribir.
     * @return {@code true} si hay que escribirlo como texto.
     */
    static boolean exceedsNumericPrecision(Number number) {
        BigInteger integer;
        if (number instanceof Long) {
            integer = BigInteger.valueOf(number.longValue());
        } else if (number instanceof BigInteger) {
            integer = (BigInteger) number;
        } else {
            return false;
        }
        if (integer.signum() == 0) {
            return false;
        }
        return new BigDecimal(integer).stripTrailingZeros().precision() > MAX_NUMERIC_DIGITS;
    }

    /**
     * Escribe el libro completo en un flujo. Se llama una sola vez, después de
     * la última fila: a partir de ahí el escritor no admite más filas.
     *
     * <p>No cierra el flujo ni el escritor: los temporales de SXSSF se borran
     * en {@link #close()}, que hay que llamar igual (lo hace el
     * try-with-resources).</p>
     *
     * @param output flujo destino; no se cierra.
     * @throws IOException si la escritura falla.
     * @throws IllegalStateException si el libro ya se escribió o el escritor
     * se cerró.
     */
    public void writeTo(OutputStream output) throws IOException {
        checkOpen();
        if (written) {
            throw new IllegalStateException("El libro ya se escribió");
        }
        if (output == null) {
            throw new IllegalArgumentException("El flujo de salida es nulo");
        }
        written = true;
        workBook.write(output);
        output.flush();
    }

    /**
     * Libera el libro y borra los temporales que SXSSF dejó en disco. Se
     * puede llamar más de una vez; las llamadas siguientes no hacen nada.
     *
     * @throws IOException si no se pudo cerrar el libro.
     */
    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        workBook.close();
    }

    /**
     * Devuelve la cantidad de filas de datos escritas (sin la de encabezados).
     *
     * @return filas de datos escritas.
     */
    public int getRowCount() {
        return rowCount;
    }

    /**
     * Devuelve la cantidad de celdas de texto que se recortaron a
     * {@value #MAX_TEXT_LENGTH} caracteres.
     *
     * @return celdas recortadas.
     */
    public int getTruncatedCellCount() {
        return truncatedCells;
    }

    /**
     * Devuelve la cantidad de fechas que caían fuera del calendario de Excel y
     * se escribieron como texto.
     *
     * @return fechas escritas como texto.
     */
    public int getOutOfRangeDateCount() {
        return outOfRangeDates;
    }

    /**
     * Devuelve la cantidad de números {@code NaN} o infinitos que quedaron
     * como celda vacía.
     *
     * @return números no representables.
     */
    public int getInvalidNumberCount() {
        return invalidNumbers;
    }

    /**
     * Devuelve la cantidad de columnas de la hoja.
     *
     * @return cantidad de columnas.
     */
    public int getColumnCount() {
        return headers.length;
    }

    /**
     * Devuelve el nombre con que quedó la hoja, ya saneado.
     *
     * @return nombre de la hoja.
     */
    public String getSheetName() {
        return sheet.getSheetName();
    }

    /**
     * Devuelve el índice (base 0) de la fila de encabezados.
     *
     * @return índice de la fila de encabezados.
     */
    public int getHeaderRowIndex() {
        return headerRowIndex;
    }

    /**
     * Devuelve una copia de los encabezados, tal como se escribieron (sin los
     * espacios de los extremos).
     *
     * @return encabezados, en orden.
     */
    public String[] getHeaders() {
        return headers.clone();
    }

    /**
     * Verifica que el escritor no esté cerrado.
     */
    private void checkOpen() {
        if (closed) {
            throw new IllegalStateException("El escritor de la hoja ya se cerró");
        }
    }

    /**
     * Verifica que el índice de columna exista.
     *
     * @param column índice de la columna.
     */
    private void checkColumn(int column) {
        if (column < 0 || column >= headers.length) {
            throw new IllegalArgumentException("Columna inexistente: " + column);
        }
    }
}
