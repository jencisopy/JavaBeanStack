/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
*        jenciso@javabeanstack.org
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
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.javabeanstack.data.IDataQueryModel;
import org.javabeanstack.outputs.IDocumentSource;
import org.javabeanstack.outputs.IOutputDocument;
import org.javabeanstack.outputs.OutputDocument;
import org.javabeanstack.util.Fn;
import org.javabeanstack.util.Strings;

/**
 * Fuente de documentos del subsistema de salida
 * ({@code org.javabeanstack.outputs}) para <b>planillas de datos tabulares</b>
 * (.xlsx): vuelca una lista de filas a una planilla y la devuelve como
 * documento en memoria.
 *
 * <p>
 * El motor es {@link ExcelUtil#toExcel(java.util.List, java.util.Map)}, que
 * aporta los estilos, los anchos de columna calculados y el manejo de
 * volúmenes grandes; esta clase solo agrega la conversión a bytes y el
 * empaquetado como {@link IOutputDocument}. Es el <b>único</b> camino del
 * subsistema para el formato de planilla: el reporte Jasper no se exporta a
 * Excel (los datos tabulares se generan desde acá, el diseño del informe es
 * de Jasper).
 * </p>
 *
 * <p>Uso típico:</p>
 * <pre>
 * IDocumentSource source = new ExcelDataSource()
 *     .data(resultList)
 *     .fileName("comisiones_202608.xlsx");
 * </pre>
 *
 * @author Jorge Enciso
 */
public class ExcelDataSource implements IDocumentSource {

    private List<IDataQueryModel> data;
    private Map<Integer, Integer> widths;
    private String sheetName;
    private String fileName;

    /**
     * Asigna las filas de datos a volcar en la planilla.
     *
     * @param data filas de datos.
     * @return esta instancia, para encadenar.
     */
    public ExcelDataSource data(List<IDataQueryModel> data) {
        this.data = data;
        return this;
    }

    /**
     * Asigna anchos de columna puntuales, pisando los calculados por el motor.
     * Opcional.
     *
     * @param widths mapa índice de columna → ancho.
     * @return esta instancia, para encadenar.
     */
    public ExcelDataSource widths(Map<Integer, Integer> widths) {
        this.widths = widths;
        return this;
    }

    /**
     * Asigna el nombre de la hoja de la planilla. Opcional.
     *
     * @param sheetName nombre de la hoja.
     * @return esta instancia, para encadenar.
     */
    public ExcelDataSource sheetName(String sheetName) {
        this.sheetName = sheetName;
        return this;
    }

    /**
     * Asigna el nombre de archivo del documento; por omisión
     * {@code datos.xlsx}.
     *
     * @param fileName nombre del archivo con su extensión.
     * @return esta instancia, para encadenar.
     */
    public ExcelDataSource fileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    /**
     * Genera la planilla y la devuelve como documento en memoria. No toca la
     * respuesta HTTP.
     *
     * @return documento generado, en formato
     * {@link IOutputDocument#FORMAT_XLSX}.
     * @throws Exception si no hay datos o la generación falla.
     */
    @Override
    public IOutputDocument generate() throws Exception {
        if (data == null || data.isEmpty()) {
            throw new Exception("No hay datos para generar la planilla");
        }
        Workbook workBook = ExcelUtil.toExcel(data, widths);
        try {
            if (!Strings.isNullorEmpty(sheetName)) {
                workBook.setSheetName(0, sheetName);
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            workBook.write(buffer);
            return new OutputDocument(Fn.nvl(fileName, "datos.xlsx"),
                    buffer.toByteArray(), IOutputDocument.FORMAT_XLSX);
        } finally {
            //Cierra el workbook y borra los temporales que SXSSF crea en disco
            try (workBook) {
                if (workBook instanceof SXSSFWorkbook) {
                    ((SXSSFWorkbook) workBook).dispose();
                }
            }
        }
    }
}
