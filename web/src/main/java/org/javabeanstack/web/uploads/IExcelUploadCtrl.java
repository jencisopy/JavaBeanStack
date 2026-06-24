/*
* Copyright (c) 2015-2018 OyM System Group S.A.
* Capitan Cristaldo 464, Asunción, Paraguay
* All rights reserved. 
*
* NOTICE:  All information contained herein is, and remains
* the property of OyM System Group S.A. and its suppliers,
* if any.  The intellectual and technical concepts contained
* herein are proprietary to OyM System Group S.A.
* and its suppliers and protected by trade secret or copyright law.
* Dissemination of this information or reproduction of this material
* is strictly forbidden unless prior written permission is obtained
* from OyM System Group S.A.
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
