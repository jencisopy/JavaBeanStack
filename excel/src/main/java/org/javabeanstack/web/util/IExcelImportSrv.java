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
     * Retorna el mensaje de error del último proceso de importación.
     *
     * @return mensaje de error, o {@code null} si no se registró ninguno.
     */
    String getErrorMessage();

    /**
     * Asigna el mensaje de error del proceso de importación.
     *
     * @param errorMessage mensaje de error a registrar.
     */
    void setErrorMessage(String errorMessage);

    /**
     * Indica si durante la importación deben sobrescribirse los registros ya
     * existentes (en lugar de únicamente insertar los nuevos).
     *
     * @return {@code true} si se deben sobrescribir los datos existentes;
     * {@code false} en caso contrario (valor por defecto).
     */
    Boolean getOverWriteData();

    /**
     * Establece si durante la importación deben sobrescribirse los registros ya
     * existentes.
     *
     * @param overWriteData {@code true} para sobrescribir los datos existentes;
     * {@code false} para solo insertar nuevos.
     */
    void setOverWriteData(Boolean overWriteData);

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
