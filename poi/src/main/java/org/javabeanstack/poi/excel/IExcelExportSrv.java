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

import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Contrato del servicio de exportación a planillas Excel.
 *
 * <p>Es la contraparte de {@link IExcelImportSrv} y existe por el mismo motivo:
 * el tamaño que una instalación aguanta no es una constante del programa. El
 * límite <b>no es de Excel</b> —que admite un millón de filas por hoja— sino
 * del servidor, que arma el libro en memoria; dónde empieza a doler depende del
 * hardware y de cuántos exporten a la vez, así que vive en la configuración.</p>
 *
 * <h2>Cómo se usa: el tope se pide ANTES de consultar</h2>
 *
 * <p>Validar recién cuando las hojas están armadas llega tarde: para armarlas,
 * quien llama ya trajo todas las filas a memoria, que es justamente lo que el
 * tope quiere evitar. Por eso el servicio hace dos cosas y el orden importa:</p>
 *
 * <ol>
 *   <li>{@link #getMaxRows(int)} <b>antes de consultar</b>, para que quien
 *       llama pida <b>una fila más</b> que el tope. Con eso el peor caso que
 *       llega a memoria es el tope más uno, y no el conjunto entero.</li>
 *   <li>{@link #toBytes(List)} al final, que vuelve a controlar y rechaza. Esa
 *       segunda revisión es la red para el llamador que se olvide del paso
 *       anterior.</li>
 * </ol>
 *
 * <p>Cuál de los dos parámetros rige lo decide <b>la cantidad de hojas</b>: con
 * una sola, el tope es del archivo entero; con varias se aplica a cada una, y
 * por eso es más bajo —una planilla de tres solapas pesa por la suma, no por la
 * hoja más grande—.</p>
 *
 * @author jenciso
 */
public interface IExcelExportSrv extends Serializable {

    /** Parámetro con el tope de una exportación de una sola hoja. */
    String PARAM_MAX_ROWS = "EXCEL_EXPORT_MAX_ROWS";

    /** Parámetro con el tope de cada hoja de una exportación de varias. */
    String PARAM_MAX_ROWS_PER_SHEET = "EXCEL_EXPORT_MAX_ROWS_PER_SHEET";

    /** Tope de una exportación de una sola hoja si el parámetro no está. */
    int DEFAULT_MAX_ROWS = 20000;

    /** Tope de cada hoja de una exportación de varias si el parámetro no está. */
    int DEFAULT_MAX_ROWS_PER_SHEET = 10000;

    /**
     * Devuelve el tope de filas que admite una exportación.
     *
     * <p>Se pide <b>antes de consultar</b>: quien llama tiene que traer a lo
     * sumo este valor más uno.</p>
     *
     * <p>Un parámetro ausente, sin número o menor a uno cae al valor por
     * omisión —dejar la exportación en cero por una fila mal cargada del
     * catálogo sería peor que ignorarla—, y lo mismo un fallo al leerlo.</p>
     *
     * @param sheetCount cantidad de hojas que la exportación va a armar.
     * @return el tope de filas a aplicar.
     */
    int getMaxRows(int sheetCount);

    /**
     * Controla que ninguna hoja pase del tope.
     *
     * @param sheets hojas de la exportación.
     * @throws ExcelExportLimitException si alguna hoja se pasa; nombra cuál.
     */
    void checkMaxRows(List<ExcelSheetData> sheets) throws ExcelExportLimitException;

    /**
     * Arma el libro con las hojas indicadas, después de controlar el tope.
     *
     * @param sheets hojas de la exportación.
     * @return el libro armado.
     * @throws ExcelExportLimitException si alguna hoja se pasa del tope.
     * @throws Exception si falla el armado.
     */
    Workbook toWorkbook(List<ExcelSheetData> sheets) throws Exception;

    /**
     * Arma el libro y lo devuelve como bytes, listo para entregar.
     *
     * <p>Es lo que necesita una descarga por http: sin archivo temporal de por
     * medio.</p>
     *
     * @param sheets hojas de la exportación.
     * @return el contenido del archivo.
     * @throws ExcelExportLimitException si alguna hoja se pasa del tope.
     * @throws Exception si falla el armado.
     */
    byte[] toBytes(List<ExcelSheetData> sheets) throws Exception;

    /**
     * Arma el libro y lo escribe en la salida indicada.
     *
     * @param sheets hojas de la exportación.
     * @param output destino del archivo.
     * @throws ExcelExportLimitException si alguna hoja se pasa del tope.
     * @throws Exception si falla el armado o la escritura.
     */
    void write(List<ExcelSheetData> sheets, OutputStream output) throws Exception;
}
