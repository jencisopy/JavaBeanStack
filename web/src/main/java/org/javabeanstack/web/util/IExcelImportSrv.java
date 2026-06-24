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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Workbook;
import org.javabeanstack.data.IDataRow;

/**
 * Contrato para servicios de importación de datos desde planillas Excel.
 * Define las operaciones de acceso al controlador de carga, validación previa
 * y conversión de datos hacia objetos del modelo.
 *
 * @author jenciso 
 * @param <T> tipo de la vista destino (debe implementar {@link IDataRow}).
 */
public interface IExcelImportSrv<T extends IDataRow> extends Serializable {

    /**
     * Retorna el libro Excel a procesar previamente asignado.
     *
     * @return el {@link Workbook} a procesar, o {@code null} si aún no se asignó.
     */
    Workbook getExcelWorkbook();

    /**
     * Asigna el libro Excel a procesar.
     *
     * @param workbook libro Excel a procesar.
     */
    void setExcelWorkbook(Workbook workbook);
    
    /**
     * Retorna los registros que no pudieron procesarse por contener errores de
     * validación o conversión.
     *
     * @return lista de registros con error; nunca es nula.
     */
    List<T> getDataRowsError();

    /**
     * Retorna los registros leídos y convertidos correctamente desde la
     * planilla.
     *
     * @return lista de registros válidos; nunca es nula.
     */
    List<T> getDataRows();

    /**
     * Retorna las propiedades de configuración del proceso de importación (por
     * ejemplo {@code allowFieldNotExist}).
     *
     * @return mapa de propiedades; nunca es nulo.
     */
    Map<String, Object> getProperties();

    /**
     * Retorna el procesador de filas asignado, encargado de transformar cada
     * fila de la planilla en un objeto del modelo.
     *
     * @return el procesador de filas, o {@code null} si aún no se asignó.
     */
    IExcelRowProcessor<T> getExcelRowProcessor();

    /**
     * Asigna el procesador de filas a utilizar durante la importación.
     *
     * @param excelRowProcessor procesador de filas a asociar.
     */
    public void setExcelRowProcessor(IExcelRowProcessor<T> excelRowProcessor);

    /**
     * Reemplaza el mapa de propiedades de configuración del proceso.
     *
     * @param properties nuevo mapa de propiedades.
     */
    void setProperties(Map<String, Object> properties);

    /**
     * Agrega o actualiza una propiedad de configuración del proceso.
     *
     * @param key clave de la propiedad.
     * @param value valor de la propiedad.
     */
    void addProperty(String key, Object value);

    
    /**
     * Ejecuta el proceso de importación del archivo Excel cargado.
     * Cada implementación define la lógica específica de validación,
     * conversión y persistencia de los datos.
     */
    void importData() throws Exception;
}
