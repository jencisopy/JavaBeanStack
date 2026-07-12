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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.Dependent;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.config.IAppConfig;
import org.javabeanstack.data.IDataResult;
import org.javabeanstack.data.services.IDataService;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.util.Fn;

/**
 * Implementación base (abstracta) del servicio de importación de datos desde
 * planillas Excel. Define, mediante el patrón <em>template method</em>, el flujo
 * completo de importación en {@link #importData()}: validación de
 * prerrequisitos, lectura/conversión de la planilla y persistencia de cada
 * registro. Las subclases normalmente <strong>no</strong> sobrescriben
 * {@code importData()}; en su lugar:
 * <ul>
 * <li>implementan los métodos abstractos {@link #getDataService()} (servicio con
 * el que se persisten los registros), {@link #getUserSession()} (sesión del
 * usuario) y {@link #getTargetType()} (clase de la entidad a persistir), y</li>
 * <li>opcionalmente sobrescriben los métodos «gancho» (<em>hooks</em>) para
 * insertar lógica específica en cada etapa del flujo:
 * {@link #beforeImportData()}, {@link #onBeforeRowConvert(IDataRow)},
 * {@link #onAfterRowConvert(IDataRow)}, {@link #cancelError(Exception)},
 * {@link #finishWithError()} y {@link #finish()}.</li>
 * </ul>
 *
 * @author jenciso
 * @param <T> tipo de la vista destino (debe implementar {@link IDataRow}).
 */
@Dependent
public abstract class ExcelImportSrv<T extends IDataRow> implements IExcelImportSrv<T> {

    @EJB
    private IAppConfig appConfig;

    private Map<String, Object> properties = new HashMap();

    private IExcelRowProcessor excelRowProcessor;

    private List<T> dataRowsError = new ArrayList();

    private List<T> dataRows = new ArrayList();

    private Workbook excelWorkbook;

    /**
     * Indica si la importación debe sobrescribir los registros ya existentes.
     */
    private Boolean overWriteData = false;

    /**
     * Mensaje de error del último proceso de importación.
     */
    private String errorMessage;

    /**
     * Provee el servicio de datos que las subclases utilizan para persistir los
     * registros importados. Debe implementarse en cada subclase.
     *
     * @return el {@link IDataService} asociado a la entidad destino.
     */
    protected abstract IDataService getDataService();

    /**
     * Provee la sesión del usuario asociada al proceso de importación, de la que
     * se obtiene el {@code sessionId} con el que opera el servicio de datos. Debe
     * implementarse en cada subclase.
     *
     * @return la {@link IUserSession} del usuario que ejecuta la importación.
     */
    protected abstract IUserSession getUserSession();

    /**
     * Provee la clase de la entidad destino en la que se persiste cada registro.
     * Es la entidad de la base de datos (no necesariamente la misma vista
     * {@code T} leída de la planilla): cada fila se convierte hacia una instancia
     * de este tipo con {@link IDataService#copyTo} antes de grabarla. Debe
     * implementarse en cada subclase.
     *
     * @return la clase de la entidad a persistir.
     */
    protected abstract Class<? extends IDataRow> getTargetType();

    /**
     * Retorna el mensaje de error del último proceso de importación.
     *
     * @return mensaje de error, o {@code null} si no se registró ninguno.
     */
    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Asigna el mensaje de error del proceso de importación.
     *
     * @param errorMessage mensaje de error a registrar.
     */
    @Override
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Indica si durante la importación deben sobrescribirse los registros ya
     * existentes (en lugar de únicamente insertar los nuevos). El valor nulo se
     * normaliza a {@code false} con {@link Fn#toLogical(Object)}.
     *
     * @return {@code true} si se deben sobrescribir los datos existentes;
     * {@code false} en caso contrario (valor por defecto).
     */
    @Override
    public Boolean getOverWriteData() {
        return Fn.toLogical(overWriteData);
    }

    /**
     * Establece si durante la importación deben sobrescribirse los registros ya
     * existentes. El valor recibido se normaliza con
     * {@link Fn#toLogical(Object)} (nulo se interpreta como {@code false}).
     *
     * @param overWriteData {@code true} para sobrescribir los datos existentes;
     * {@code false} para solo insertar nuevos.
     */
    @Override
    public void setOverWriteData(Boolean overWriteData) {
        this.overWriteData = Fn.toLogical(overWriteData);
    }

    /**
     * Retorna el libro Excel a procesar previamente asignado.
     *
     * @return el {@link Workbook} a procesar, o {@code null} si aún no se
     * asignó.
     */
    @Override
    public Workbook getExcelWorkbook() {
        return excelWorkbook;
    }

    /**
     * Asigna el libro Excel a procesar. Debe invocarse antes de
     * {@link #getDataFromExcelSheet(Sheet)} cuando no se pasa una hoja
     * explícita.
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
        errorMessage = "";

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
     * Valida los prerrequisitos necesarios antes de ejecutar la importación:
     * que se haya asignado el procesador de filas, la planilla Excel y que el
     * procesador tenga definido el tipo destino ({@code targetType}). Se invoca
     * al inicio de {@link #importData()}.
     *
     * @throws Exception si no se asignó el procesador, la planilla o el tipo
     * destino ({@code targetType}).
     */
    protected void checkBeforeImportData() throws Exception {
        IExcelRowProcessor<T> processor = getExcelRowProcessor();
        //Debe haberse definido el processor        
        if (processor == null) {
            throw new Exception("No se definio el processor");
        }
        //Debe haberse definido la planilla
        if (getExcelWorkbook() == null) {
            throw new Exception("No se definio la planilla a procesar o la planilla no tiene hojas");
        }
        Class<T> type = processor.getTargetType();
        //Debe haberse definido el tipo de dato destino a convertir.
        if (type == null) {
            throw new Exception("No fue definido targetType");
        }
    }

    /**
     * Ejecuta el flujo completo de importación de la planilla Excel
     * (<em>template method</em>). Las subclases no suelen sobrescribir este
     * método sino los métodos «gancho» que invoca en cada etapa. El flujo es:
     * <ol>
     * <li>{@link #beforeImportData()}: preparación previa (p.ej. asignar el
     * libro y el procesador de filas).</li>
     * <li>{@link #checkBeforeImportData()}: validación de prerrequisitos
     * (procesador, planilla y {@code targetType} del procesador).</li>
     * <li>{@link #getDataFromExcelSheet(Sheet)}: lectura y conversión de la
     * planilla a una lista de objetos {@code T}. Si no hay registros, se invoca
     * {@link #cancelError(Exception)} con {@code null} y termina.</li>
     * <li>Por cada registro válido: {@link #onBeforeRowConvert(IDataRow)},
     * conversión a la entidad {@link #getTargetType()} con
     * {@link IDataService#copyTo}, asignación de la acción
     * ({@code INSERT}/{@code UPDATE}; los {@code UPDATE} se omiten salvo que
     * {@link #getOverWriteData()} sea {@code true}),
     * {@link #onAfterRowConvert(IDataRow)}, validación con
     * {@link IDataService#checkDataRow} y persistencia con
     * {@link IDataService#update}. Los registros con error se acumulan en
     * {@link #getDataRowsError()}.</li>
     * <li>{@link #finishWithError()} si algún registro falló, o
     * {@link #finish()} si todos se procesaron correctamente.</li>
     * </ol>
     * Cualquier excepción se captura: se registra su mensaje con
     * {@link #setErrorMessage(String)} y se invoca
     * {@link #cancelError(Exception)} con la excepción.
     * <p>
     * Antes de la etapa de validación deben haberse asignado el procesador de
     * filas (vía {@link #setExcelRowProcessor(IExcelRowProcessor)}) y el libro
     * Excel (vía {@link #setExcelWorkbook(Workbook)}), típicamente dentro de
     * {@link #beforeImportData()}. Opcionalmente puede configurarse el mapa de
     * propiedades (vía {@link #addProperty(String, Object)} /
     * {@link #setProperties(Map)}), por ejemplo {@code allowFieldNotExist} para
     * controlar la validación de columnas en
     * {@link IExcelRowProcessor#checkMetaData()}.
     *
     * @throws Exception no se propaga: las excepciones del flujo se capturan
     * internamente y se derivan a {@link #cancelError(Exception)}.
     */
    @Override
    public void importData() throws Exception {
        try {
            //BeforeImportData
            beforeImportData();

            //Chequear si todos los objetos que dependen de este proceso estan asignados
            checkBeforeImportData();

            String sessionId = getUserSession().getSessionId();
            
            //Procesa planilla y convierte a una lista tipo T (entidad de la vista)
            List<T> data = getDataFromExcelSheet(null);
            if (data == null || data.isEmpty()) {
                cancelError(null);
                return;
            }
            //Aqui el proceso para mostrar registros con errores.

            //Proceso de grabación
            IDataRow target;
            boolean error = false;
            for (T source : getDataRows()) {
                //Antes de la conversion a la instancia targetType
                if (!onBeforeRowConvert(source)) {
                    continue;
                }
                //Convertir a una objeto targetType
                target = getDataService().copyTo(sessionId, source, getTargetType().getConstructor().newInstance());
                //Definir tipo de persitencia en la base
                target.setAction(IDataRow.INSERT);
                if (target.getId() != null) {
                    target.setAction(IDataRow.UPDATE);
                }
                //Si existe el registro y no se permite sobreescribir.
                if (target.getAction() == IDataRow.UPDATE && !getOverWriteData()) {
                    continue;
                }
                //Despues de convertir
                if (!onAfterRowConvert(target)) {
                    continue;
                }
                //Validar 
                Map<String, IErrorReg> errorInfo = getDataService().checkDataRow(sessionId, target);
                if (!errorInfo.isEmpty()) {
                    source.setErrors(errorInfo);
                    getDataRowsError().add(source);
                    error = true;
                    continue;
                }
                //Grabar
                IDataResult result = getDataService().update(sessionId, target);
                if (!result.isSuccessFul()) {
                    source.setErrors(result.getErrorsMap());
                    getDataRowsError().add(source);
                }
            }
            //Fin proceso
            if (error) {
                finishWithError();
                return;
            }
            finish();
        } catch (Exception e) {
            setErrorMessage(e.getMessage());
            cancelError(e);
        }
    }

    /**
     * Gancho invocado al inicio de {@link #importData()}, antes de cualquier
     * validación. La implementación base no hace nada; las subclases pueden
     * sobrescribirlo para preparar el proceso, típicamente asignar el libro
     * Excel ({@link #setExcelWorkbook(Workbook)}) y el procesador de filas
     * ({@link #setExcelRowProcessor(IExcelRowProcessor)}).
     *
     * @throws Exception si la preparación falla; la excepción es capturada por
     * {@link #importData()} y derivada a {@link #cancelError(Exception)}.
     */
    protected void beforeImportData() throws Exception{

    }

    /**
     * Gancho invocado por cada registro leído de la planilla, antes de
     * convertirlo a la entidad destino. La implementación base acepta todos los
     * registros; las subclases pueden sobrescribirlo para enriquecer o validar
     * la vista (p.ej. resolver claves foráneas) y decidir si continuar.
     *
     * @param rowView registro {@code T} leído de la planilla.
     * @return {@code true} para procesar el registro; {@code false} para
     * omitirlo.
     */
    protected boolean onBeforeRowConvert(T rowView) {
        return true;
    }

    /**
     * Gancho invocado por cada registro después de convertirlo a la entidad
     * destino ({@link #getTargetType()}) y antes de validarlo y persistirlo. La
     * implementación base acepta todos los registros; las subclases pueden
     * sobrescribirlo para ajustar la entidad antes de grabarla.
     *
     * @param rowTable entidad destino resultante de la conversión.
     * @return {@code true} para continuar con la persistencia; {@code false}
     * para omitir el registro.
     */
    protected boolean onAfterRowConvert(IDataRow rowTable) {
        return true;
    }

    /**
     * Gancho invocado cuando la importación no produce resultados o se aborta
     * por una excepción. La implementación base no hace nada; las subclases
     * pueden sobrescribirlo para notificar al usuario o registrar el error.
     *
     * @param e excepción que abortó el proceso, o {@code null} si simplemente no
     * hubo registros para importar.
     */
    protected void cancelError(Exception e) {

    }

    /**
     * Gancho invocado al finalizar la importación cuando uno o más registros
     * fallaron la validación o persistencia (ver {@link #getDataRowsError()}).
     * La implementación base no hace nada; las subclases pueden sobrescribirlo
     * para informar el resultado parcial al usuario.
     */
    protected void finishWithError() {

    }

    /**
     * Gancho invocado al finalizar la importación cuando todos los registros se
     * procesaron correctamente. La implementación base no hace nada; las
     * subclases pueden sobrescribirlo para notificar el éxito del proceso.
     */
    protected void finish() {

    }
}
