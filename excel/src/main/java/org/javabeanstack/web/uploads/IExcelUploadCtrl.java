/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2015 - 2027 Jorge Enciso
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
package org.javabeanstack.web.uploads;

import java.io.Serializable;
import org.apache.poi.ss.usermodel.Workbook;
import org.javabeanstack.web.util.FacesContextUtil;
import org.primefaces.event.FileUploadEvent;

/**
 * Contrato para el controlador de carga de planillas Excel.
 * Define las operaciones de recepción, acceso y limpieza de archivos Excel
 * subidos mediante PrimeFaces FileUpload.
 *
 * @author jenciso
 */
public interface IExcelUploadCtrl extends Serializable{

    /**
     * Limpia el estado interno, cerrando el workbook y eliminando los bytes
     * del archivo previamente cargado.
     */
    void clear();

    /**
     * Retorna el {@link Workbook} generado a partir del archivo Excel cargado.
     *
     * @return instancia de {@link Workbook} lista para lectura.
     * @throws IllegalStateException si el archivo no tiene hojas o la hoja 0 es nula.
     * @throws Exception si ocurre un error al leer el archivo.
     */
    Workbook getExcelWorkbook() throws Exception;

    /**
     * Evento que recibe la planilla excel a procesar.
     *
     * @param event datos del evento.
     */
    void handleFileUpload(FileUploadEvent event);

    /**
     * Determina si existe archivo a levantar.
     *
     * @return Verdadero existe y falso no existe.
     */
    boolean hasFile();

    /**
     * Retorna la instancia de {@link FacesContextUtil} configurada para
     * mostrar mensajes en la vista.
     *
     * @return instancia configurada de {@link FacesContextUtil}.
     */
    FacesContextUtil getFacesCtx();

    /**
     * Callback invocado al finalizar el procesamiento del archivo Excel.
     * Si {@code message} no es nulo ni vacío, muestra un mensaje informativo.
     * Cierra el diálogo de carga y limpia el componente de archivo.
     *
     * @param message mensaje a mostrar al usuario; si es nulo o vacío, no se muestra nada.
     */
    void onComplete(String message);
}
