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

import jakarta.ejb.ApplicationException;

/**
 * Una hoja de la exportación tiene más filas de las que el sistema admite.
 *
 * <p>Lleva <b>datos y no un texto armado</b> —qué hoja, cuántas filas y cuál
 * era el tope— a propósito: quien la recibe es el que conoce a su usuario y el
 * idioma en el que hay que hablarle. Una biblioteca no debería decidir cómo se
 * le explica un rechazo a la persona que aprieta el botón.</p>
 *
 * <p>La cantidad de filas suele ser <b>el tope más uno</b> y no el total real:
 * quien exporta pide una fila de más justamente para poder distinguir «entró
 * justo» de «se cortó», y así no paga traer un conjunto que va a descartar. Por
 * eso el mensaje que se arme debería decir «supera las N» y no «tiene N+1».</p>
 *
 * <p>Es {@code @ApplicationException(rollback = false)}: la exportación no
 * escribe nada, y dejar que marque la transacción para deshacer haría que un
 * rechazo por tamaño arrastrara consigo lo que estuviera haciendo el llamador.</p>
 *
 * @author jenciso
 */
@ApplicationException(rollback = false)
public class ExcelExportLimitException extends Exception {

    private final String sheetName;
    private final int rows;
    private final int maxRows;

    /**
     * @param sheetName nombre de la hoja que se pasó del tope.
     * @param rows filas que tiene la hoja (suele ser el tope más uno).
     * @param maxRows tope vigente al momento del rechazo.
     */
    public ExcelExportLimitException(String sheetName, int rows, int maxRows) {
        super("La hoja \"" + sheetName + "\" tiene " + rows + " filas y supera el limite de "
                + maxRows + " que admite una exportacion");
        this.sheetName = sheetName;
        this.rows = rows;
        this.maxRows = maxRows;
    }

    /**
     * @return nombre de la hoja que se pasó del tope.
     */
    public String getSheetName() {
        return sheetName;
    }

    /**
     * @return filas que tiene la hoja.
     */
    public int getRows() {
        return rows;
    }

    /**
     * @return tope vigente al momento del rechazo.
     */
    public int getMaxRows() {
        return maxRows;
    }
}
