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

/**
 * Nivel de exigencia de una columna de la planilla, declarado en
 * {@link ExcelColumnSpec}. Cada nivel se valida en un momento distinto del
 * flujo de importación: la existencia de la cabecera en
 * {@link ExcelRowProcessor#checkMetaData()} (una sola vez, antes de leer
 * filas) y la presencia del valor en {@link ExcelRowProcessor#process()}
 * (fila por fila).
 *
 * @author Jorge Enciso
 */
public enum ColumnRequirement {
    /**
     * La columna puede faltar en la planilla. Si falta, el atributo destino
     * recibe el valor por defecto declarado en la especificación, o
     * {@code null} si no se declaró ninguno. Es el nivel por defecto y
     * reproduce el comportamiento histórico del procesador.
     */
    OPTIONAL,
    /**
     * La cabecera tiene que existir en la planilla. Si falta,
     * {@link ExcelRowProcessor#checkMetaData()} devuelve el mensaje «Falta la
     * columna obligatoria «X»» y la importación no llega a leer las filas. El
     * contenido de cada celda no se exige: una celda vacía se asigna como
     * {@code null} (o con el valor por defecto si se declaró
     * {@code defaultWhenBlank}).
     */
    COLUMN,
    /**
     * Además de exigir la cabecera (como {@link #COLUMN}), exige que la celda
     * traiga un valor en cada fila. Una celda vacía marca la fila con el error
     * «La columna «X» no puede estar vacía» (número 50000) y el resto de las
     * columnas se sigue procesando; la fila termina en el listado de errores
     * de la importación.
     * <p>
     * Un valor por defecto declarado con {@code defaultWhenBlank()}
     * <b>prevalece</b> sobre esta exigencia: si la columna lo declaró, la celda
     * vacía toma ese valor y la fila no se marca con error (el valor por
     * defecto es la declaración explícita de qué poner cuando el dato no
     * viene).
     */
    VALUE
}
