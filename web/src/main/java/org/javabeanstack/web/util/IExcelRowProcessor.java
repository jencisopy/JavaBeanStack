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

import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.javabeanstack.data.IDataRow;

/**
 * Contrato de un procesador que transforma una fila de una planilla Excel en un
 * objeto de dominio de tipo {@code T}. La implementación de referencia es
 * {@link ExcelRowProcessor}.
 *
 * @param <T> tipo del objeto destino (debe implementar {@link IDataRow}).
 */
public interface IExcelRowProcessor<T extends IDataRow> {

    /**
     * Instancia el objeto destino y lo completa con los valores de la fila
     * actual.
     *
     * @return el objeto destino de tipo {@code T} ya instanciado y completado.
     * @throws Exception si no se puede instanciar el destino o falla la
     * asignación de algún valor.
     */
    T process() throws Exception;

    /**
     * Reemplaza la fila de Excel sobre la cual opera el procesador. Permite
     * reutilizar la misma instancia (y sus mapas ya calculados) en varias filas
     * de la misma planilla.
     *
     * @param row la nueva fila de Excel a procesar.
     */
    void setRow(Row row);

    /**
     * @return el mapeo de encabezado de columna del Excel a nombre del atributo
     * en el objeto destino.
     */
    Map<String, String> getHeadToField();

    /**
     * @return el mapeo inverso de nombre de atributo en el objeto destino a
     * encabezado de columna del Excel.
     */
    Map<String, String> getFieldToHead();

    /**
     * @return el mapeo de encabezado de columna del Excel a índice de columna.
     */
    Map<String, Integer> getHeadToIndex();

    /**
     * @return el índice (base 0) de la fila que contiene los encabezados de
     * columna dentro de la planilla.
     */
    int getHeaderRowIndex();
}
