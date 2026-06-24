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
package org.javabeanstack.web.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.Dependent;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.config.IAppConfig;
import org.javabeanstack.util.Fn;

/**
 * Implementación base del servicio de importación de datos desde planillas
 * Excel. Provee métodos utilitarios para validar la estructura de la planilla y
 * convertir sus filas a objetos del modelo. Las subclases deben sobrescribir
 * {@link #importData()} para definir la lógica específica de cada tipo de
 * importación.
 *
 * @author jenciso
 * @param <T> tipo de la vista destino (debe implementar {@link IDataRow}).
 */
@Dependent
public class ExcelImportSrv<T extends IDataRow> implements IExcelImportSrv<T> {

    @EJB
    private IAppConfig appConfig;

    private Map<String, Object> properties = new HashMap();

    private IExcelRowProcessor excelRowProcessor;

    private List<T> dataRowsError = new ArrayList();

    private List<T> dataRows = new ArrayList();
    
    private Workbook excelWorkbook;

    /**
     * Retorna el libro Excel a procesar previamente asignado.
     *
     * @return el {@link Workbook} a procesar, o {@code null} si aún no se asignó.
     */
    @Override
    public Workbook getExcelWorkbook() {
        return excelWorkbook;
    }

    /**
     * Asigna el libro Excel a procesar. Debe invocarse antes de
     * {@link #getDataFromExcelSheet(Sheet)} cuando no se pasa una hoja explícita.
     *
     * @param workbook libro Excel a procesar.
     */
    @Override
    public void setExcelWorkbook(Workbook workbook) {
        this.excelWorkbook = workbook;
    }


    /**
     * Retorna los registros que no pudieron procesarse por contener errores de
     * validación o conversión.
     *
     * @return lista de registros con error; nunca es nula.
     */
    @Override
    public List<T> getDataRowsError() {
        return dataRowsError;
    }

    /**
     * Retorna los registros leídos y convertidos correctamente desde la
     * planilla.
     *
     * @return lista de registros válidos; nunca es nula.
     */
    @Override
    public List<T> getDataRows() {
        return dataRows;
    }

    /**
     * Retorna las propiedades de configuración del proceso de importación (por
     * ejemplo {@code allowFieldNotExist}). Inicia como un mapa vacío.
     *
     * @return mapa de propiedades; nunca es nulo.
     */
    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Retorna el procesador de filas asignado, encargado de transformar cada
     * fila de la planilla en un objeto del modelo.
     *
     * @return el procesador de filas, o {@code null} si aún no se asignó.
     */
    @Override
    public IExcelRowProcessor<T> getExcelRowProcessor() {
        return excelRowProcessor;
    }

    /**
     * Asigna el procesador de filas a utilizar durante la importación. Debe
     * invocarse antes de {@link #getDataFromExcelSheet(Sheet)}.
     *
     * @param excelRowProcessor procesador de filas a asociar.
     */
    @Override
    public void setExcelRowProcessor(IExcelRowProcessor<T> excelRowProcessor) {
        this.excelRowProcessor = excelRowProcessor;
    }

    /**
     * Reemplaza el mapa de propiedades de configuración del proceso.
     *
     * @param properties nuevo mapa de propiedades.
     */
    @Override
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    /**
     * Agrega o actualiza una propiedad de configuración del proceso,
     * inicializando el mapa si fuese nulo.
     *
     * @param key clave de la propiedad.
     * @param value valor de la propiedad.
     */
    @Override
    public void addProperty(String key, Object value) {
        if (properties == null) {
            properties = new HashMap();
        }
        this.properties.put(key, value);
    }

    /**
     * Devuelve una lista conteniendo los datos leídos de la planilla Excel,
     * utilizando el procesador de filas configurado. Cada fila se transforma
     * con {@link IExcelRowProcessor#process()}; los registros válidos se
     * acumulan en {@link #getDataRows()} y los que contienen errores en
     * {@link #getDataRowsError()}.
     *
     * @param sheet hoja a procesar; si es {@code null} se utiliza la primera
     * hoja del libro asignado con {@link #setExcelWorkbook(Workbook)}.
     * @return registros de datos convertidos; lista vacía si la planilla no
     * tiene filas de datos.
     * @throws Exception si no se asignó el procesador o la planilla, o si la
     * estructura de la planilla no supera la validación de
     * {@link IExcelRowProcessor#checkMetaData()}.
     */
    protected List<T> getDataFromExcelSheet(Sheet sheet) throws Exception {
        dataRows = new ArrayList();
        dataRowsError = new ArrayList();
        //Debe haberse definido el processor
        IExcelRowProcessor<T> processor = getExcelRowProcessor();
        if (processor == null) {
            throw new Exception("No se definio el processor");
        }
        //Planilla a procesar (si no se indica, se toma la primera hoja del libro)
        if (sheet == null) {
            if (getExcelWorkbook() == null) {
                throw new Exception("No se definio la planilla a procesar");
            }
            sheet = getExcelWorkbook().getSheetAt(0);
        }
        if (sheet == null) {
            throw new Exception("La planilla no tiene hojas para procesar");
        }
        //Posiciona el processor en la fila de encabezados y valida la estructura
        processor.setRow(sheet.getRow(processor.getHeaderRowIndex()));
        String errorMsg = processor.checkMetaData();
        if (!Fn.nvl(errorMsg, "").isEmpty()) {
            throw new Exception(errorMsg);
        }
        //Por defecto hasta 5000 registros se puede procesar.
        Integer rowCount = 5000;
        if (appConfig != null && appConfig.getSystemParam("EXCEL_IMPORT_ROWS_LIMIT") != null) {
            rowCount = Fn.nvl(appConfig.getSystemParam("EXCEL_IMPORT_ROWS_LIMIT").getValueNumber().intValue(), 5000);
        }
        T dataRow;
        int firstRow = 0;
        int lastRow = (rowCount <= 0) ? sheet.getLastRowNum() : firstRow + rowCount - 1;
        for (int r = firstRow; r <= lastRow; r++) {
            // No se procesa la fila de encabezados
            if (r == processor.getHeaderRowIndex()) {
                continue;
            }
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            processor.setRow(row);
            dataRow = processor.process();
            if (dataRow != null) {
                if (dataRow.getErrors() == null || dataRow.getErrors().isEmpty()) {
                    dataRows.add(dataRow);
                } else {
                    dataRowsError.add(dataRow);
                }
            }
        }
        return dataRows;
    }

    /**
     * Ejecuta la importación de la planilla Excel. Esta implementación base sólo
     * valida que estén definidos los atributos necesarios; las subclases deben
     * sobrescribirla (invocando {@code super.importData()} primero) para
     * implementar la lógica de conversión y persistencia, normalmente apoyándose
     * en {@link #getDataFromExcelSheet(Sheet)}.
     * <p>
     * Antes de invocar este método deben haberse asignado los siguientes
     * atributos para que el proceso funcione correctamente:
     * <ul>
     * <li>{@code excelRowProcessor} (vía {@link #setExcelRowProcessor(IExcelRowProcessor)}):
     * el procesador de filas que define el mapeo cabecera→atributo y la
     * conversión de cada fila. No puede ser {@code null}.</li>
     * <li>{@code targetType} del procesador (que {@link IExcelRowProcessor#getTargetType()}
     * retorne la clase destino, no {@code null}): es la vista del modelo en la
     * que se instancia cada fila.</li>
     * <li>{@code excelWorkbook} (vía {@link #setExcelWorkbook(Workbook)}): el
     * libro Excel a procesar. No puede ser {@code null}.</li>
     * </ul>
     * Opcionalmente puede configurarse el mapa de propiedades (vía
     * {@link #addProperty(String, Object)} / {@link #setProperties(Map)}), por
     * ejemplo {@code allowFieldNotExist} para controlar la validación de
     * columnas en {@link IExcelRowProcessor#checkMetaData()}.
     *
     * @throws Exception si no se asignó el procesador, la planilla o el tipo
     * destino ({@code targetType}).
     */
    @Override
    public void importData() throws Exception {
        IExcelRowProcessor<T> processor = getExcelRowProcessor();
        //Debe haberse definido el processor        
        if (processor == null) {
            throw new Exception("No se definio el processor");
        }
        //Debe haberse definido la planilla
        if (getExcelWorkbook() == null){
            throw new Exception("No se definio la planilla a procesar o la planilla no tiene hojas");
        }
        Class<T> type = processor.getTargetType();
        //Debe haberse definido el tipo de dato destino a convertir.
        if (type == null) {
            throw new Exception("No fue definido targetType");
        }
        //Aqui implementar logica de importación.

        // Ejemplo de implementación en una subclase (sobrescribiendo importData):
        //
        // @Override
        // public void importData() throws Exception {
        //     // 1) Asignar la planilla a procesar (p.ej. desde el controlador de carga)
        //     setExcelWorkbook(getExcelUploadCtrl().getExcelWorkbook());
        //
        //     // 2) Asignar el procesador de filas (define el mapeo y la conversión).
        //     //    La fila de encabezados se usa para construir el índice de columnas.
        //     Sheet sheet = getExcelWorkbook().getSheetAt(0);
        //     Row headerRow = sheet.getRow(0);
        //     setExcelRowProcessor(new MiProcessor(headerRow, MiView.class));
        //
        //     // 3) (Opcional) configurar propiedades de validación.
        //     addProperty("allowFieldNotExist", false);
        //
        //     // 4) Validar prerrequisitos definidos en esta clase base.
        //     super.importData();
        //
        //     // 5) Convertir las filas; lanza Exception si checkMetaData() falla.
        //     List<MiView> data = getDataFromExcelSheet(null);
        //
        //     // 6) Persistir / informar resultados.
        //     if (data == null || data.isEmpty()) {
        //         // ... informar que no se procesaron registros
        //         return;
        //     }
        //     // dao.persist(data); ... y revisar getDataRowsError() para los rechazados
        // }
    }
}
