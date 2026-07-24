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
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.javabeanstack.data.IDataRow;

/**
 * Contrato para servicios de importación de datos desde planillas Excel.
 * Define las operaciones de configuración del proceso (libro, procesador de
 * filas, opciones), la pasada de validación previa
 * ({@link #checkValidation(Sheet)}), la importación propiamente dicha
 * ({@link #importData()}) y los contadores/mensajes de resultado que consumen
 * las vistas (p.ej. el wizard de importación).
 *
 * @author jenciso
 * @param <T> tipo de la vista destino (debe implementar {@code IDataRow}).
 */
public interface IExcelImportSrv<T extends IDataRow> extends Serializable {

    /**
     * Retorna la cantidad de filas efectivamente leídas de la planilla en la
     * última pasada de lectura (tengan o no errores). Puede diferir de
     * {@link #getRowsCount()} si el procesador descarta filas.
     *
     * @return cantidad de filas leídas de la planilla.
     */
    int getRowsReadedCount();

    /**
     * Retorna el total de registros producidos por la última pasada de
     * lectura: válidos ({@link #getDataRows()}) más con errores
     * ({@link #getDataRowsError()}).
     *
     * @return total de registros a importar (válidos + con errores).
     */
    int getRowsCount();

    /**
     * Retorna la cantidad de registros grabados con éxito en la base por la
     * última importación.
     *
     * @return cantidad de registros migrados.
     */
    int getRowsMigratedCount();

    /**
     * Retorna la cantidad de registros omitidos en la última importación por
     * existir ya en la base cuando la opción de sobrescritura
     * ({@link #getOverWriteData()}) está desactivada.
     *
     * @return cantidad de registros omitidos por ya existir.
     */
    int getRowsExistCount();

    /**
     * Retorna la cantidad de registros válidos listos para importar según la
     * última pasada de lectura.
     *
     * @return cantidad de registros válidos.
     */
    int getRowsValidCount();

    /**
     * Retorna la cantidad de registros con errores de validación o conversión
     * acumulados en {@link #getDataRowsError()}.
     *
     * @return cantidad de registros con errores.
     */
    int getRowsErrorCount();

    /**
     * Retorna el total de registros procesados por la última importación:
     * migrados + omitidos por existir + con errores.
     *
     * @return total de registros procesados.
     */
    int getRowsProcessedCount();

    /**
     * Indica si el último proceso de importación se ejecutó completo, es decir
     * recorrió todos los registros aunque algunos hayan quedado con errores.
     *
     * @return {@code true} si el proceso se completó; {@code false} si abortó
     * por una excepción, por falta de datos o aún no se ejecutó.
     */
    Boolean getImportOk();

    /**
     * Retorna el mensaje descriptivo del resultado cuando el proceso se
     * completó (ver {@link #getImportOk()}).
     *
     * @return mensaje del resultado de la importación.
     */
    String getResultMessage();

    /**
     * Retorna el detalle del error cuando el proceso abortó (ver
     * {@link #getImportOk()}).
     *
     * @return detalle del error, o cadena vacía si no hubo aborto.
     */
    String getResultErrorMessage();

    /**
     * Retorna el log del último proceso de importación.
     *
     * @return log de la importación; cadena vacía si no hay detalle.
     */
    String getResultLog();

    /**
     * Indica si los errores deben revisarse antes de importar: en ese caso
     * {@link #importData()} se detiene sin grabar cuando encuentra registros
     * con errores que no pasaron previamente por
     * {@link #checkValidation(Sheet)}.
     *
     * @return {@code true} si se exige revisar los errores antes de importar
     * (valor por defecto); {@code false} para importar directo.
     */
    Boolean getCheckBeforeErrors();

    /**
     * Establece si los errores deben revisarse antes de importar. El valor
     * nulo se normaliza a {@code false}.
     *
     * @param revisarErrores {@code true} para exigir la revisión previa;
     * {@code false} para importar directo omitiendo los registros con error.
     */
    void setCheckBeforeErrors(Boolean revisarErrores);

    /**
     * Indica en qué fase del proceso se encuentra la importación, útil para que
     * los hooks distingan la pasada de revisión de la importación real: retorna
     * {@code false} durante la pasada de solo-validación de
     * {@link #checkValidation(Sheet)} y {@code true} durante {@link #importData()}
     * (desde la lectura hasta el fin de la grabación; al terminar el proceso se
     * resetea a {@code false}). También queda en {@code true} entre una
     * revisión finalizada con éxito y la importación que la consume.
     *
     * @return {@code true} si la planilla ya fue revisada o se está en la fase
     * de importación; {@code false} durante la pasada de revisión.
     */
    Boolean getErrorsReviewed();


    /**
     * Retorna el mensaje de error del último proceso de importación.
     *
     * @return mensaje de error, o {@code null} si no se registró ninguno.
     */
    String getErrorMessage();

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
    void setExcelRowProcessor(IExcelRowProcessor<T> excelRowProcessor);

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
     * Ejecuta una pasada de solo-validación sobre la planilla: lee y convierte
     * todos los registros alimentando {@link #getDataRows()} (válidos) y
     * {@link #getDataRowsError()} (con errores), sin persistir nada. Deja
     * registrada la revisión para que {@link #importData()} pueda continuar
     * cuando {@link #getCheckBeforeErrors()} está activo.
     *
     * @param sheet hoja a procesar; si es {@code null} se utiliza la primera
     * hoja del libro asignado.
     */
    void checkValidation(Sheet sheet);

    /**
     * Ejecuta el proceso de importación del archivo Excel cargado.
     * Cada implementación define la lógica específica de validación,
     * conversión y persistencia de los datos.
     *
     * @throws Exception si ocurre un error no controlado durante el proceso.
     */
    void importData() throws Exception;
}
