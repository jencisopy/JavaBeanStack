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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import org.javabeanstack.io.IOUtil;
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
 * {@link #onAfterRowConvert(IDataRow, IDataRow)}, {@link #cancelError(Exception)},
 * {@link #finishWithError()} y {@link #finish()}.</li>
 * </ul>
 * Ofrece además {@link #checkValidation(Sheet)} (pasada de solo-validación que
 * alimenta las listas de válidos/errores sin persistir) y los
 * contadores/mensajes de resultado que consumen las vistas.
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
     * Indica si los errores deben revisarse antes de importar (ver la guarda
     * de revisión previa en {@link #importData()}).
     */
    private Boolean checkBeforeErrors = true;

    /** Registros grabados con éxito en la última importación. */
    private int rowsMigratedCount = 0;

    /** Registros omitidos por ya existir (sin sobrescritura) en la última importación. */
    private int rowsExistCount = 0;

    /** Filas efectivamente leídas de la planilla en la última pasada de lectura. */
    private int rowsReadedCount = 0;

    /**
     * Indica si el último proceso de importación se ejecutó completo (aunque
     * haya filas con errores). Es falso si abortó por excepción o sin datos.
     */
    private Boolean importOk = false;

    /**
     * Indica si la última pasada de validación ({@link #checkValidation})
     * finalizó correctamente, es decir el usuario ya tuvo oportunidad de
     * revisar los errores. Se consume (resetea) en cada {@link #importData()}.
     */
    private boolean errorsReviewed = false;

    /**
     * Indica si la importación debe sobrescribir los registros ya existentes.
     */
    private Boolean overWriteData = false;

    /**
     * Mensaje de error del último proceso de importación.
     */
    private String errorMessage;

    /**
     * Log del último proceso de importación (inicio, errores por registro,
     * totales y fin). Puede crecer mucho: la vista no debe volcarlo completo
     * sin límite.
     */
    private final StringBuilder resultLog = new StringBuilder();

    /**
     * Formato de fecha/hora usado en el log del proceso.
     */
    private static final DateTimeFormatter LOG_TS = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Provee el servicio de datos que las subclases utilizan para persistir los
     * registros importados. Debe implementarse en cada subclase.
     *
     * @return el {@link IDataService} asociado a la entidad destino.
     */
    protected abstract IDataService getDataService();

    /**
     * Indica si los errores deben revisarse antes de importar (ver
     * {@link #importData()}: con la opción activa el proceso se detiene si hay
     * registros con errores sin pasar antes por {@link #checkValidation}).
     *
     * @return {@code true} si se exige la revisión previa (valor por defecto);
     * {@code false} para importar directo.
     */
    @Override
    public Boolean getCheckBeforeErrors() {
        return checkBeforeErrors;
    }

    /**
     * Establece si los errores deben revisarse antes de importar. El valor
     * recibido se normaliza con {@link Fn#toLogical(Object)} (nulo se
     * interpreta como {@code false}).
     *
     * @param checkBeforeErrors {@code true} para exigir la revisión previa;
     * {@code false} para importar directo omitiendo los registros con error.
     */
    @Override
    public void setCheckBeforeErrors(Boolean checkBeforeErrors) {
        this.checkBeforeErrors = Fn.toLogical(checkBeforeErrors);
    }

    @Override
    public Boolean getErrorsReviewed() {
        return errorsReviewed;
    }
    
    /**
     * Retorna la cantidad de filas efectivamente leídas de la planilla en la
     * última pasada de {@link #getDataFromExcelSheet(Sheet)}, tengan o no
     * errores. Puede diferir de {@link #getRowsCount()} cuando el procesador
     * descarta filas (devuelve {@code null} al procesarlas).
     *
     * @return cantidad de filas leídas de la planilla.
     */
    @Override
    public int getRowsReadedCount() {
        return rowsReadedCount;
    }

    /**
     * Retorna el total de registros producidos por la última pasada de lectura:
     * válidos ({@link #getDataRows()}) más con errores
     * ({@link #getDataRowsError()}).
     *
     * @return total de registros a importar (válidos + con errores).
     */
    @Override
    public int getRowsCount() {
        int retornar = 0;
        if (getDataRows() != null) {
            retornar = getDataRows().size();
        }
        if (getDataRowsError() != null) {
            retornar += getDataRowsError().size();
        }
        return retornar;
    }

    /**
     * Retorna la cantidad de registros con errores de validación o conversión
     * acumulados en {@link #getDataRowsError()}.
     *
     * @return cantidad de registros con errores.
     */
    @Override
    public int getRowsErrorCount() {
        int retornar = 0;
        if (getDataRowsError() != null) {
            retornar = getDataRowsError().size();
        }
        return retornar;
    }

    /**
     * Retorna la cantidad de registros válidos listos para importar según la
     * última pasada de lectura.
     *
     * @return cantidad de registros válidos.
     */
    @Override
    public int getRowsValidCount() {
        return getRowsCount() - getRowsErrorCount();
    }

    /**
     * Retorna la cantidad de registros grabados con éxito en la base por la
     * última ejecución de {@link #importData()}.
     *
     * @return cantidad de registros migrados.
     */
    @Override
    public int getRowsMigratedCount() {
        return rowsMigratedCount;
    }

    /**
     * Retorna la cantidad de registros omitidos en la última importación por
     * existir ya en la base cuando {@link #getOverWriteData()} está
     * desactivado.
     *
     * @return cantidad de registros omitidos por ya existir.
     */
    @Override
    public int getRowsExistCount() {
        return rowsExistCount;
    }

    /**
     * Retorna el total de registros procesados por la última importación:
     * migrados + omitidos por existir + con errores.
     *
     * @return total de registros procesados.
     */
    @Override
    public int getRowsProcessedCount() {
        return rowsMigratedCount + rowsExistCount + getRowsErrorCount();
    }

    /**
     * Indica si el último proceso de importación se ejecutó completo (recorrió
     * todos los registros, aunque algunos hayan quedado con errores). Es falso
     * si el proceso abortó por una excepción o porque no hubo datos que
     * procesar.
     */
    @Override
    public Boolean getImportOk() {
        return importOk;
    }

    /**
     * Retorna el mensaje descriptivo del resultado del proceso: el mensaje de
     * error si lo hubo, un aviso si quedaron registros con errores, o la
     * confirmación de importación limpia.
     *
     * @return mensaje del resultado de la importación.
     */
    @Override
    public String getResultMessage() {
        if (getErrorMessage() != null && !getErrorMessage().isEmpty()) {
            return getErrorMessage();
        }
        if (!dataRowsError.isEmpty()){
            return "El proceso tuvo errores en la migración";
        }
        return "Importación sin errores";
    }

    /**
     * Retorna el detalle del error cuando el proceso abortó (ver
     * {@link #getImportOk()}).
     *
     * @return detalle del error, o cadena vacía si no hubo aborto.
     */
    @Override
    public String getResultErrorMessage() {
        if (getErrorMessage() != null && !getErrorMessage().isEmpty()) {
            return getErrorMessage();
        }
        return "";
    }

    /**
     * Retorna el log del último proceso de importación: inicio, errores de
     * migración por registro (identificador + mensajes), totales y fin.
     * Puede contener mucho texto; si se muestra en pantalla debe acotarse.
     *
     * @return log de la importación; cadena vacía si aún no se ejecutó.
     */
    @Override
    public String getResultLog() {
        return resultLog.toString();
    }

    /**
     * Agrega una línea al log del proceso de importación.
     *
     * @param texto línea a registrar.
     */
    protected void logResult(String texto) {
        resultLog.append(texto).append("\n");
    }

    /**
     * Nombre del archivo origen de los datos (la planilla Excel migrada),
     * registrado como primera línea del log del proceso. Debe implementarse
     * en cada subclase (típicamente obteniéndolo del controlador de carga);
     * si el nombre no está disponible debe retornar cadena vacía, en cuyo
     * caso la línea no se registra.
     *
     * @return nombre de la planilla origen, o cadena vacía si no se conoce.
     */
    protected abstract String getSourceFileName();

    /**
     * Identificador del registro usado en el log de errores de migración. La
     * implementación base retorna el id del registro si lo tiene; las
     * subclases pueden sobrescribirlo para usar un dato de negocio más
     * descriptivo (p.ej. el RUC o el número de documento).
     *
     * @param row registro leído de la planilla.
     * @return identificador del registro para el log; cadena vacía si no hay.
     */
    protected String getRowLogIdentifier(T row) {
        Object id = row.getId();
        return id != null ? id.toString() : "";
    }

    /**
     * Vuelca al log los registros con errores acumulados en
     * {@link #getDataRowsError()}: número secuencial, identificador
     * ({@link #getRowLogIdentifier}) y mensajes de error del registro.
     */
    private void appendErrorsToLog() {
        if (dataRowsError == null || dataRowsError.isEmpty()) {
            return;
        }
        logResult("Errores de migración (" + dataRowsError.size() + "):");
        int nro = 1;
        for (T row : dataRowsError) {
            StringBuilder msgs = new StringBuilder();
            if (row.getErrors() != null) {
                msgs.append(row.getErrorMsg());
            }
            String id = getRowLogIdentifier(row);
            logResult("  " + nro++ + ") " + (id.isEmpty() ? "" : id + ": ") + msgs);
        }
    }

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
     * Ejecuta una pasada sobre todos los registros de una hoja del excel En ese
     * proceso alimenta dataRows (registros válidos a migrar) dataRowsError
     * (registros con errores en la planilla, tanto de conversión como de la
     * validación de la lógica de negocios). Prepara antes el proceso vía
     * {@link #beforeImportData()} (asigna el libro y el procesador de filas) y
     * valida los prerrequisitos, por lo que puede invocarse en forma
     * independiente de {@link #importData()}. Si la pasada finaliza bien deja
     * marcada la revisión ({@code errorsReviewed}), con lo cual el siguiente
     * {@link #importData()} no vuelve a leer ni a validar la planilla.
     * <p>
     * Si {@link #getCheckBeforeErrors()} es {@code false} (no se pidió revisar
     * los errores antes de importar) el método no hace nada: la lectura y la
     * validación completa se realizan recién en {@link #importData()}.
     *
     * @param sheet hoja a procesar; si es {@code null} se utiliza la primera
     * hoja del libro asignado.
     */
    @Override
    public void checkValidation(Sheet sheet) {
        errorsReviewed = false;
        if (!getCheckBeforeErrors()) {
            return;
        }
        try {
            beforeImportData();
            checkBeforeImportData();
            getDataFromExcelSheet(sheet);
            errorsReviewed = true;
        } catch (Exception e) {
            errorMessage = e.getMessage();
            cancelError(e);
        }
    }

    /**
     * Devuelve una lista conteniendo los datos leídos de la planilla Excel,
     * utilizando el procesador de filas configurado. Cada fila se transforma
     * con {@link IExcelRowProcessor#process()}; los registros válidos se
     * acumulan en {@link #getDataRows()} y los que contienen errores en
     * {@link #getDataRowsError()}.
     * <p>
     * Si {@link #getCheckBeforeErrors()} está activo y aún no se revisaron los
     * errores (pasada de {@link #checkValidation(Sheet)}), cada registro sin
     * errores de conversión se somete además a la validación de la lógica de
     * negocios: se invoca {@link #onBeforeRowConvert(IDataRow)}, se convierte a
     * la entidad destino ({@link #getTargetType()}) con
     * {@link IDataService#copyTo}, se invoca
     * {@link #onAfterRowConvert(IDataRow, IDataRow)} y se valida con
     * {@link IDataService#checkDataRow}; los registros rechazados por la
     * validación se acumulan en {@link #getDataRowsError()} y los omitidos por
     * los hooks se descartan. Cuando la lectura se realiza dentro de
     * {@link #importData()} ({@code errorsReviewed} en {@code true}) este método
     * solo convierte las filas: los hooks y la validación corren después, en el
     * bucle de grabación.
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
        T source;
        int firstRow = 0;
        int lastRow = (rowCount <= 0) ? sheet.getLastRowNum() : firstRow + rowCount - 1;
        rowsReadedCount = 0;
        //Validación de la lógica de negocios solo si se pidió revisar los
        //errores antes de importar y aún no se hizo la revisión.
        boolean checkBusinessLogic = !errorsReviewed && getCheckBeforeErrors();
        IDataRow target;
        String sessionId = checkBusinessLogic ? getUserSession().getSessionId() : null;
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
            source = processor.process();
            rowsReadedCount++;
            if (source != null) {
                if (source.getErrors() != null && !source.getErrors().isEmpty()) {
                    dataRowsError.add(source);
                    continue;
                }
                // Verificación de la logica de negocios si es que asi esta seteado en la importacion
                if (checkBusinessLogic) {
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
                    //Despues de convertir
                    if (!onAfterRowConvert(source, target)) {
                        continue;
                    }
                    //Validar
                    Map<String, IErrorReg> errorInfo = getDataService().checkDataRow(sessionId, target);
                    if (!errorInfo.isEmpty()) {
                        source.setErrors(errorInfo);
                        dataRowsError.add(source);
                        continue;
                    }
                }
                dataRows.add(source);
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
     * <li>Lectura de la planilla, solo si no hubo una pasada previa de
     * {@link #checkValidation(Sheet)}: {@link #beforeImportData()} (preparación
     * previa, p.ej. asignar el libro y el procesador de filas),
     * {@link #checkBeforeImportData()} (validación de prerrequisitos:
     * procesador, planilla y {@code targetType} del procesador) y
     * {@link #getDataFromExcelSheet(Sheet)} (lectura y conversión de la
     * planilla a una lista de objetos {@code T}; en esta pasada no se ejecutan
     * los hooks ni la validación de negocio, que corren recién en el bucle de
     * grabación). Si la revisión ya se hizo se reutilizan
     * {@link #getDataRows()} y {@link #getDataRowsError()} de esa pasada.
     * Desde este punto y durante toda la grabación el proceso queda en fase de
     * importación: {@link #getErrorsReviewed()} retorna {@code true} en los
     * hooks (a diferencia de la pasada de revisión, donde retorna
     * {@code false}); el indicador se resetea al finalizar el proceso, con lo
     * cual una próxima corrida vuelve a leer y validar la planilla.</li>
     * <li>Guarda de revisión previa: si {@link #getCheckBeforeErrors()} está
     * activo, hay registros con errores y no hubo una pasada previa de
     * {@link #checkValidation(Sheet)}, el proceso se detiene sin grabar nada
     * (queda {@link #getImportOk()} en {@code false} y el detalle en
     * {@link #getErrorMessage()}). Si no hay registros válidos, se invoca
     * {@link #cancelError(Exception)} con {@code null} y termina.</li>
     * <li>Por cada registro válido: {@link #onBeforeRowConvert(IDataRow)},
     * conversión a la entidad {@link #getTargetType()} con
     * {@link IDataService#copyTo}, asignación de la acción
     * ({@code INSERT}/{@code UPDATE}; los {@code UPDATE} se omiten salvo que
     * {@link #getOverWriteData()} sea {@code true}, contabilizados en
     * {@link #getRowsExistCount()}), {@link #onAfterRowConvert(IDataRow, IDataRow)},
     * validación con {@link IDataService#checkDataRow} y persistencia con
     * {@link IDataService#update} (los grabados se contabilizan en
     * {@link #getRowsMigratedCount()}). Los registros con error se acumulan en
     * {@link #getDataRowsError()}. Si los registros ya fueron validados en la
     * pasada de {@link #checkValidation(Sheet)}, el {@code checkDataRow} no se
     * repite (se marca el registro como verificado); en ese caso los duplicados
     * dentro de la misma planilla no se detectan entre sí, dado que ninguno
     * existía en la base al momento de la revisión.</li>
     * <li>Completado el recorrido se marca {@link #getImportOk()} en
     * {@code true} y se invoca {@link #finishWithError()} si algún registro
     * falló, o {@link #finish()} si todos se procesaron correctamente.</li>
     * </ol>
     * Cualquier excepción se captura: su mensaje queda disponible en
     * {@link #getErrorMessage()} y se invoca
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
        importOk = false;
        rowsMigratedCount = 0;
        rowsExistCount = 0;
        resultLog.setLength(0);
        logResult("Inicio del proceso: " + LocalDateTime.now().format(LOG_TS));
        try {
            String sessionId = getUserSession().getSessionId();
            if (!getSourceFileName().isEmpty()) {
                logResult("Planilla: " + getSourceFileName());
            }
            logResult("Empresa: " + getUserSession().getCompany().getName());
            logResult("Usuario: " + getUserSession().getUser().getLogin());
            logResult("----------------------------------------------------------------------------------------");

            //Si se pidió revisar los errores antes de importar y aún no fueron
            //revisados (no pasó por checkValidation), se detiene la importación.
            boolean reviewed = errorsReviewed;
            
            //Procesa planilla y convierte a una lista tipo T (entidad de la vista)
            //si es que no se proceso aun.
            if (!errorsReviewed) {
                //BeforeImportData
                beforeImportData();

                //Chequear si todos los objetos que dependen de este proceso estan asignados
                checkBeforeImportData();
                //Queda en fase de importación (getErrorsReviewed() true en los
                //hooks) y evita repetir la validación de negocio en la lectura.
                errorsReviewed = true;
                getDataFromExcelSheet(null);
            }

            if (getCheckBeforeErrors() && !reviewed && !getDataRowsError().isEmpty()) {
                errorMessage = "Se encontraron " + getDataRowsError().size()
                        + " registro(s) con errores. Revise la validación antes de importar.";
                logResult("Proceso detenido: " + errorMessage);
                return;
            }
            if (dataRows == null || dataRows.isEmpty()) {
                errorMessage = "No se encontraron registros para procesar";
                logResult("Proceso detenido: " + errorMessage);
                cancelError(null);
                return;
            }
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
                    rowsExistCount++;
                    continue;
                }
                //Despues de convertir
                if (!onAfterRowConvert(source, target)) {
                    continue;
                }
                //Validar
                if (!reviewed) {
                    Map<String, IErrorReg> errorInfo = getDataService().checkDataRow(sessionId, target);
                    if (!errorInfo.isEmpty()) {
                        source.setErrors(errorInfo);
                        getDataRowsError().add(source);
                        error = true;
                        continue;
                    }
                } else {
                    target.setRowChecked(true);
                }
                //Grabar
                IDataResult result = getDataService().update(sessionId, target);
                if (!result.isSuccessFul()) {
                    source.setErrors(result.getErrorsMap());
                    getDataRowsError().add(source);
                } else {
                    rowsMigratedCount++;
                }
            }
            //Fin proceso: el recorrido se completó (haya o no filas con errores)
            importOk = true;
            if (error) {
                finishWithError();
                return;
            }
            finish();
        } catch (Exception e) {
            errorMessage = e.getMessage();
            logResult("El proceso abortó por un error: " + e.getMessage());
            cancelError(e);
        } finally {
            //Revisión consumida: una próxima corrida debe releer y revalidar la planilla.
            errorsReviewed = false;
            appendErrorsToLog();
            logResult("Registros procesados: " + getRowsProcessedCount()
                    + " (migrados: " + rowsMigratedCount
                    + ", ya existentes: " + rowsExistCount
                    + ", con errores: " + getRowsErrorCount() + ")");
            logResult("Fin del proceso: " + LocalDateTime.now().format(LOG_TS));
            saveResultLogToFile();
        }
    }

    /**
     * Persiste el log del proceso ({@link #getResultLog()}) como archivo en la
     * carpeta del servidor definida por el parámetro del sistema
     * {@code APPLOGPATH} (mismo parámetro que usa
     * {@code AbstractDataService.importFrom}); si el parámetro no está
     * definido se usa {@code jboss.server.config.dir}. El nombre del archivo
     * es {@code import_from_excel_<Entidad>_<idempresa>_<yyyyMMddHHmmss>.log}. Cualquier
     * error (carpeta inexistente, permisos, sesión no disponible) se registra
     * en el propio log sin interrumpir el proceso.
     */
    protected void saveResultLogToFile() {
        try {
            String filePath = System.getProperty("jboss.server.config.dir");
            if (appConfig != null && appConfig.getSystemParam("APPLOGPATH") != null) {
                filePath = appConfig.getSystemParam("APPLOGPATH").getValueChar();
            }
            filePath = IOUtil.addbs(filePath)
                    + "import_from_excel_" + getTargetType().getSimpleName()
                    + "_" + getUserSession().getIdCompany()
                    + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    + ".log";
            Path file = Paths.get(filePath);
            Files.writeString(file, getResultLog(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            logResult("Log guardado en: " + filePath);
        } catch (Exception e) {
            logResult("No se pudo guardar el log en disco: " + e.getMessage());
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
     * sobrescribirlo para ajustar la entidad destino antes de grabarla o
     * derivar de la fila origen información adicional.
     *
     * @param rowSource registro origen leído de la planilla (entidad de la
     * vista) a partir del cual se generó la entidad destino.
     * @param rowTarget entidad destino resultante de la conversión.
     * @return {@code true} para continuar con la persistencia; {@code false}
     * para omitir el registro.
     */
    protected boolean onAfterRowConvert(T rowSource, IDataRow rowTarget) {
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
