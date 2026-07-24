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
package org.javabeanstack.web.util;

import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.javabeanstack.data.IDataRow;

/**
 * Contrato de un procesador que transforma una fila de una planilla Excel en un
 * objeto de dominio de tipo {@code T}. La implementación de referencia es
 * {@link ExcelRowProcessor}.
 *
 * @param <T> tipo del objeto destino (debe implementar {@code IDataRow}).
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
     * Obtiene el mapa encabezado de Excel -> atributo del objeto destino.
     * @return el mapeo de encabezado de columna del Excel a nombre del atributo
     * en el objeto destino.
     */
    Map<String, String> getHeadToField();

    /**
     * Obtiene el mapa atributo del objeto destino -> encabezado de Excel.
     *
     * <p>
     * Si el atributo se encuentra null, se genera automáticamente a partir de
     * headToField.</p>
     * @return el mapeo inverso de nombre de atributo en el objeto destino a
     * encabezado de columna del Excel.
     */
    Map<String, String> getFieldToHead();

    /**
     * Devuelve el mapeo de encabezado de columna del Excel a índice de columna.
     * @return el mapeo de encabezado de columna del Excel a índice de columna.
     */
    Map<String, Integer> getHeadToIndex();

    /**
     * Devuelve el índice (base 0) de la fila de encabezados de la planilla.
     * @return el índice (base 0) de la fila que contiene los encabezados de
     * columna dentro de la planilla.
     */
    int getHeaderRowIndex();

    /**
     * Provee la clase de la entidad destino en la que se persiste cada registro.
     * Es la entidad de la base de datos (no necesariamente la misma vista
     * {@code T} leída de la planilla): cada fila se convierte hacia una instancia
     * de este tipo con {@code IDataService.copyTo} antes de grabarla. Debe
     * implementarse en cada subclase.
     * @return la clase del objeto destino, usada por {@link #process()} para
     * instanciarlo.
     */
    Class<T> getTargetType();

    /**
     * Valida la estructura de la planilla (encabezados de texto, sin duplicados
     * y, según la propiedad {@code allowFieldNotExist}, compatibilidad de los
     * tipos de columna con los atributos destino) antes de procesar sus filas.
     *
     * @return mensaje de error si hubiere inconvenientes, o cadena vacía si todo
     * es válido.
     */
    String checkMetaData();

    /**
     * Devuelve la hoja a la que pertenece la fila en curso.
     * @return la hoja a la que pertenece la fila en curso, o {@code null} si
     * aún no se asignó ninguna fila.
     */
    Sheet getSheet();
}
