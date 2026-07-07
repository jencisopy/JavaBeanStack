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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.javabeanstack.util.Strings;
import org.javabeanstack.web.util.FacesContextUtil;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;

/**
 *
 * @author Alberto Amarilla
 */
@Named("excelUploadBean")
@Dependent
public class ExcelUploadCtrl implements IExcelUploadCtrl {

    private static final Logger LOGGER = LogManager.getLogger(ExcelUploadCtrl.class);

    private byte[] excelBytes;

    private Workbook excelWorkbook = null;

    private FacesContextUtil facesCtx = new FacesContextUtil();
    
    /**
     * Retorna la instancia de {@link FacesContextUtil} para mostrar mensajes en la vista.
     * Si la instancia es nula, la inicializa y configura la vista de mensajes
     * con {@code AppMkRootCtrl.MENSAJES}.
     *
     * @return instancia configurada de {@link FacesContextUtil}.
     */
    @Override
    public FacesContextUtil getFacesCtx() {
        if (facesCtx == null) {
            facesCtx = new FacesContextUtil();
        }
        return facesCtx;
    }
    
    /**
     * Evento que recibe la planilla excel a procesar.
     *
     * @param event datos del evento.
     */
    @Override
    public void handleFileUpload(FileUploadEvent event) {
        try {
            this.excelBytes = event.getFile().getContent();
        } catch (Exception e) {
            LOGGER.error("Error al recibir el archivo Excel", e);
            this.excelBytes = null;
        }
    }

    /**
     * Retorna el {@link Workbook} generado a partir de los bytes del archivo Excel cargado.
     * Si el workbook aún no fue creado, lo inicializa desde {@code excelBytes}.
     * Valida que el archivo tenga al menos una hoja y que la hoja 0 no sea nula.
     *
     * @return instancia de {@link Workbook} lista para lectura.
     * @throws IllegalStateException si el archivo no tiene hojas o la hoja 0 es nula.
     * @throws Exception si ocurre un error al leer el stream del archivo.
     */
    @Override
    public Workbook getExcelWorkbook() throws Exception {
        if (excelWorkbook == null) {
            InputStream is = new ByteArrayInputStream(excelBytes);
            excelWorkbook = WorkbookFactory.create(is);
            if (excelWorkbook.getNumberOfSheets() == 0) {
                throw new IllegalStateException("El archivo Excel no tiene hojas.");
            }
            if (excelWorkbook.getSheetAt(0) == null) {
                throw new IllegalStateException("La hoja 0 del Excel vino null.");
            }
        }
        return excelWorkbook;
    }


    /**
     * Determina si existe archivo a levantar.
     *
     * @return Verdadero existe y falso no existe.
     */
    @Override
    public boolean hasFile() {
        return excelBytes != null && excelBytes.length > 0;
    }

    /**
     * Limpia el estado interno del componente, eliminando el workbook y los bytes
     * del archivo Excel previamente cargado. Permite reutilizar la instancia para
     * una nueva carga.
     */
    @Override
    public void clear() {
        if (excelWorkbook != null) {
            try { excelWorkbook.close(); } catch (Exception ignored) {}
            excelWorkbook = null;
        }
        this.excelBytes = null;
    }
    
    /**
     * Callback invocado al finalizar el procesamiento del archivo Excel.
     * Si {@code message} no es nulo ni vacío, muestra un mensaje informativo al usuario.
     * Cierra el diálogo de carga y ejecuta el script de limpieza del componente de archivo.
     *
     * @param message mensaje a mostrar al usuario; si es nulo o vacío, no se muestra nada.
     */
    @Override
    public void onComplete(String message){
        if (!Strings.isNullorEmpty(message)){
            getFacesCtx().showInfo(message);            
        }
        clear();        
        PrimeFaces.current().executeScript("PF('wdlg_excel_upload').hide(); clearFile();");
    }
}
