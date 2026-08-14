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
 * Contrato de una <b>fuente</b> de documentos: la pieza que sabe producir un
 * {@link IOutputDocument} completo en memoria a partir de su tecnología de
 * generación. Cada tecnología es una implementación:
 *
 * <ul>
 * <li>una plantilla Word (.docx) cuyos marcadores {@code <<campo>>} se
 * reemplazan con un mapa de datos;</li>
 * <li>un reporte JasperReports que se llena con datos y parámetros y se
 * exporta a PDF, XLSX o HTML;</li>
 * <li>cualquier generador futuro (una planilla armada con POI, un HTML de una
 * plantilla de texto, etc.).</li>
 * </ul>
 *
 * <p>
 * La fuente es <b>independiente del destino</b>: no sabe si el documento se
 * descargará, se enviará por correo o se guardará en una carpeta. Esa
 * separación es la que permite generar una sola vez y entregar a N destinos.
 * </p>
 *
 * <p>
 * Las implementaciones se configuran con su API propia (fluida o por setters)
 * antes de llamar a {@link #generate()}: la plantilla y su mapa, el reporte y
 * sus parámetros. La validación del insumo (¿existe la plantilla? ¿existe el
 * reporte?) es responsabilidad de la fuente y debe ocurrir dentro de
 * {@code generate()}, antes de producir nada.
 * </p>
 *
 * @author Jorge Enciso
 */
public interface IDocumentSource {

    /**
     * Produce el documento completo en memoria: valida que el insumo exista
     * (plantilla, reporte), genera los bytes y los devuelve envueltos con su
     * nombre, tipo MIME y formato. No toca la respuesta HTTP ni ningún otro
     * destino.
     *
     * @return documento generado, nunca nulo.
     * @throws Exception si el insumo no existe o la generación falla; el
     * mensaje debe identificar el insumo (ej. el nombre de la plantilla que no
     * se encontró) para que el llamador pueda mostrarlo al usuario.
     */
    IOutputDocument generate() throws Exception;
}
