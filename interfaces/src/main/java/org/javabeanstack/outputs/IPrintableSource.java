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
package org.javabeanstack.outputs;

/**
 * Capacidad opcional de una {@link IDocumentSource}: enviar la salida
 * <b>directo a una impresora</b>, sin producir un documento descargable.
 *
 * <p>
 * La impresión directa es el único destino que no encaja en el modelo
 * {@code generar → } {@link IOutputDocument} {@code → entregar}: no hay
 * archivo, y quien sabe imprimir es la propia tecnología de generación (en
 * JasperReports, {@code JasperPrintManager} imprime el {@code JasperPrint} ya
 * llenado). Por eso se modela como capacidad de la fuente y no como un
 * {@link IDocumentTarget}.
 * </p>
 *
 * <p>
 * <b>Advertencia</b>: la impresión ocurre en la JVM <b>del servidor</b> — sale
 * por una impresora visible desde el servidor de aplicaciones, no por la del
 * cliente. Para imprimir en el puesto del usuario, el camino es entregar el
 * PDF por descarga y que lo imprima el navegador.
 * </p>
 *
 * @author Jorge Enciso
 */
public interface IPrintableSource {

    /**
     * Imprime la salida directamente en la impresora del servidor (la
     * impresora por omisión, o la que la implementación tenga configurada).
     * No produce ningún {@link IOutputDocument} y no toca la respuesta HTTP.
     *
     * @throws Exception si el insumo no existe, la generación falla o la
     * impresora no está disponible.
     */
    void printDirect() throws Exception;
}
