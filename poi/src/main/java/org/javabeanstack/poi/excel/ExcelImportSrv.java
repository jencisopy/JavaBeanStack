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
package org.javabeanstack.poi.excel;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.naming.InitialContext;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.Dependent;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.javabeanstack.annotation.CheckForeignkey;
import org.javabeanstack.data.DataInfo;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.config.IAppConfig;
import org.javabeanstack.data.IDataResult;
import org.javabeanstack.data.services.IDataService;
import org.javabeanstack.error.ErrorInfo;
import org.javabeanstack.error.IErrorInfo;
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
 * @param <T> tipo de la vista destino (debe implementar {@code IDataRow}).
 */
@Dependent
public abstract class ExcelImportSrv<T extends IDataRow> implements IExcelImportSrv<T> {

    @EJB
    private IAppConfig appConfig;

    private Map<String, Object> properties = new HashMap();

    /**
     * Procesador de filas y libro Excel de la corrida en curso. Son
     * {@code transient}: ni el libro de POI ni el procesador (que retiene la
     * fila y las columnas) son serializables, y el servicio suele vivir dentro
     * de un bean de vista pasivable. Si el contenedor pasiva la vista a mitad
     * del asistente, la corrida se pierde y hay que volver a cargar la
     * planilla; el resto del estado (contadores, log, filas convertidas) se
     * conserva.
     */
    private transient IExcelRowProcessor excelRowProcessor;

    private List<T> dataRowsError = new ArrayList();

    private List<T> dataRows = new ArrayList();

    private transient Workbook excelWorkbook;

    /**
     * Validador de Bean Validation de la revisión previa, resuelto de forma
     * perezosa por {@link #getValidator()}. Es {@code transient} como el libro
     * y el procesador: no es serializable, y tras una pasivación se vuelve a
     * resolver ({@link #validatorResolved} vuelve a {@code false}).
     */
    private transient Validator validator;

    /** Indica si ya se intentó resolver {@link #validator}. */
    private transient boolean validatorResolved;

    /**
     * Hoja a procesar por nombre; tiene prioridad sobre {@link #sheetIndex}.
     */
    private String sheetName;

    /**
     * Hoja a procesar por posición, <b>base 0</b> como en POI; {@code null} =
     * la primera.
     */
    private Integer sheetIndex;

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
     * Operaciones que permite la importación: altas y modificaciones por
     * omisión; bajas no. Sin altas, los registros que no existen quedan con
     * error; sin modificaciones, los existentes nunca se sobrescriben.
     */
    private boolean allowInsert = true;
    private boolean allowUpdate = true;
    private boolean allowDelete = false;

    /**
     * Columna de la planilla que marca las filas a eliminar (solo con
     * {@link #isAllowDelete()}); es opcional en la planilla: sin ella no se
     * elimina nada.
     */
    private String deleteColumn = DEFAULT_DELETE_COLUMN;

    /** Valor de {@link #deleteColumn} que marca la fila a eliminar. */
    private String deleteValue = DEFAULT_DELETE_VALUE;

    /** Registros eliminados en la última importación. */
    private int rowsDeletedCount = 0;

    /** Columna de marca de baja por omisión. */
    public static final String DEFAULT_DELETE_COLUMN = "accion";

    /** Valor de la marca de baja por omisión. */
    public static final String DEFAULT_DELETE_VALUE = "B";

    /**
     * Propiedad de la fila (en {@code getProperties()}) que la marca para
     * eliminar.
     */
    public static final String DELETE_MARK = "EXCEL_DELETE_MARK";

    /**
     * Propiedad de la fila marcada para eliminar con los errores de lectura
     * que se dejaron de lado: se informan solo si el registro no aparece.
     */
    static final String DELETE_READ_ERRORS = "EXCEL_DELETE_READ_ERRORS";

    /** Mensaje de la fila marcada sin permiso de baja. */
    static final String DELETE_NOT_AUTHORIZED = "No está autorizado para eliminar registros";

    /** Mensaje de la fila marcada cuyo registro no existe. */
    static final String DELETE_NOT_FOUND = "No existe el registro a eliminar";

    /** Mensaje de la fila marcada cuyo registro es de otra empresa. */
    static final String DELETE_OTHER_COMPANY = "El registro a eliminar pertenece a otra empresa";

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
     * @return el {@code IDataService} asociado a la entidad destino.
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
     * recibido se normaliza con {@code Fn.toLogical} (nulo se
     * interpreta como {@code false}).
     *
     * @param checkBeforeErrors {@code true} para exigir la revisión previa;
     * {@code false} para importar directo omitiendo los registros con error.
     */
    @Override
    public void setCheckBeforeErrors(Boolean checkBeforeErrors) {
        this.checkBeforeErrors = Fn.toLogical(checkBeforeErrors);
    }

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
        return rowsMigratedCount + rowsExistCount + rowsDeletedCount + getRowsErrorCount();
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
            String id = getRowLogIdentifier(row);
            logResult("  " + nro++ + ") " + (id.isEmpty() ? "" : id + ": ") + formatRowErrors(row));
        }
    }

    /**
     * Arma el texto de los errores de una fila para el log: cada mensaje con
     * su número al final, con el formato {@code mensaje - (nro)} que usa la
     * aplicación en pantalla (el número se omite si es 0), separados por
     * {@code "; "}. Los avisos no se incluyen.
     *
     * @param row fila con errores.
     * @return los mensajes de error de la fila; cadena vacía si no tiene.
     */
    static String formatRowErrors(IDataRow row) {
        if (row.getErrors() == null) {
            return "";
        }
        StringBuilder msgs = new StringBuilder();
        for (IErrorInfo error : row.getErrors().values()) {
            if (error == null || error.isWarning()) {
                continue;
            }
            String message = Fn.nvl(error.getMessage(), "").trim();
            if (message.isEmpty()) {
                continue;
            }
            if (msgs.length() > 0) {
                msgs.append("; ");
            }
            msgs.append(message);
            Integer number = error.getErrorNumber();
            if (number != null && number != 0) {
                msgs.append(" - (").append(number).append(")");
            }
        }
        return msgs.toString();
    }

    /**
     * Provee la sesión del usuario asociada al proceso de importación, de la que
     * se obtiene el {@code sessionId} con el que opera el servicio de datos. Debe
     * implementarse en cada subclase.
     *
     * @return la {@code IUserSession} del usuario que ejecuta la importación.
     */
    protected abstract IUserSession getUserSession();

    /**
     * Provee la clase de la entidad destino en la que se persiste cada registro.
     * Es la entidad de la base de datos (no necesariamente la misma vista
     * {@code T} leída de la planilla): cada fila se convierte hacia una instancia
     * de este tipo con {@code IDataService.copyTo} antes de grabarla. Debe
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
     * normaliza a {@code false} con {@code Fn.toLogical}.
     *
     * @return {@code true} si se deben sobrescribir los datos existentes;
     * {@code false} en caso contrario (valor por defecto).
     */
    @Override
    public Boolean getOverWriteData() {
        return effectiveOverWrite();
    }

    /**
     * Valor efectivo de «sobrescribir» según las operaciones permitidas y la
     * opción del usuario. Lo usan {@link #getOverWriteData()} y los setters
     * que descartan la revisión previa cuando cambia: una subclase puede
     * sobrescribir {@code getOverWriteData()} por fila (E5-07 del plan CTBPRC
     * de Maker) y eso no tiene que ocultar el cambio de la opción.
     */
    private boolean effectiveOverWrite() {
        //Las operaciones permitidas sí van por sus métodos: una subclase las
        //restringe por los permisos del usuario.
        if (!isAllowUpdate()) {
            return false;
        }
        if (!isAllowInsert()) {
            //Sin altas, sobrescribir es lo único que tiene sentido.
            return true;
        }
        return Fn.toLogical(overWriteData);
    }

    /**
     * Indica si la opción «sobrescribir» está fijada por las operaciones
     * permitidas (sin modificaciones: siempre {@code false}; sin altas:
     * siempre {@code true}) y el usuario no puede cambiarla.
     *
     * @return {@code true} si la opción está fijada.
     */
    public boolean isOverWriteLocked() {
        return !isAllowUpdate() || !isAllowInsert();
    }

    /**
     * Indica si la importación da de alta los registros que no existen.
     *
     * @return {@code true} por omisión.
     */
    public boolean isAllowInsert() {
        return allowInsert;
    }

    /**
     * Establece si la importación da de alta los registros que no existen. Si
     * cambia el valor efectivo de «sobrescribir», descarta la revisión previa
     * (I5-25 del plan CTBPRC de Maker).
     *
     * @param allowInsert {@code true} para permitir las altas.
     */
    public void setAllowInsert(boolean allowInsert) {
        boolean previous = effectiveOverWrite();
        this.allowInsert = allowInsert;
        if (previous != effectiveOverWrite()) {
            invalidateReview();
        }
    }

    /**
     * Indica si la importación puede modificar (sobrescribir) registros
     * existentes.
     *
     * @return {@code true} por omisión.
     */
    public boolean isAllowUpdate() {
        return allowUpdate;
    }

    /**
     * Establece si la importación puede modificar registros existentes. Si
     * cambia el valor efectivo de «sobrescribir», descarta la revisión previa
     * (I5-25 del plan CTBPRC de Maker).
     *
     * @param allowUpdate {@code true} para permitir las modificaciones.
     */
    public void setAllowUpdate(boolean allowUpdate) {
        boolean previous = effectiveOverWrite();
        this.allowUpdate = allowUpdate;
        if (previous != effectiveOverWrite()) {
            invalidateReview();
        }
    }

    /**
     * Indica si la importación elimina las filas marcadas en la columna
     * {@link #getDeleteColumn()} con {@link #getDeleteValue()}.
     *
     * @return {@code false} por omisión.
     */
    public boolean isAllowDelete() {
        return allowDelete;
    }

    public void setAllowDelete(boolean allowDelete) {
        this.allowDelete = allowDelete;
    }

    /**
     * Cabecera de la columna que marca las filas a eliminar (sin distinguir
     * mayúsculas). Opcional en la planilla.
     *
     * @return la cabecera; por omisión {@value #DEFAULT_DELETE_COLUMN}.
     */
    public String getDeleteColumn() {
        return deleteColumn;
    }

    public void setDeleteColumn(String deleteColumn) {
        this.deleteColumn = deleteColumn;
    }

    /**
     * Valor que marca una fila a eliminar (sin distinguir mayúsculas ni
     * espacios).
     *
     * @return el valor; por omisión {@value #DEFAULT_DELETE_VALUE}.
     */
    public String getDeleteValue() {
        return deleteValue;
    }

    public void setDeleteValue(String deleteValue) {
        this.deleteValue = deleteValue;
    }

    /**
     * Registros eliminados en la última importación.
     *
     * @return la cantidad.
     */
    public int getRowsDeletedCount() {
        return rowsDeletedCount;
    }

    /**
     * Indica si la fila leída de la planilla está marcada para eliminar.
     *
     * @param row fila de la vista.
     * @return {@code true} si se la marcó con la columna de baja.
     */
    public static boolean isMarkedForDelete(IDataRow row) {
        return row != null && row.getProperties() != null
                && Boolean.TRUE.equals(row.getProperties().get(DELETE_MARK));
    }

    /**
     * Gancho de autorización de las bajas: se consulta una vez por pasada,
     * antes de la primera fila marcada para eliminar. La base las permite;
     * las subclases contrastan el permiso de eliminación.
     *
     * @return {@code true} si se pueden eliminar registros.
     */
    protected boolean checkDeleteAuthorization() {
        return true;
    }

    /**
     * Verificación propia del registro que se va a eliminar, además de las
     * reglas del servicio de datos. La base rechaza el registro de <b>otra
     * empresa</b>: el que resuelve por id la fórmula de la vista puede venir
     * de un catálogo compartido ({@code fn_empresashared}) y {@code findById}
     * no filtra por empresa.
     * <ul>
     * <li>Si la entidad tiene {@code idempresa}, se compara con
     * {@code getIdCompany()} de la sesión (la empresa de los datos).</li>
     * <li>Si tiene {@code idcompany} (tablas del catálogo), con
     * {@code getCompany().getIdcompany()}: {@code getIdCompany()} devuelve la
     * máscara, que no es un id del catálogo.</li>
     * <li>Si no tiene ninguno (un detalle, como la cotización de una moneda),
     * se mira el {@code idempresa} de sus asociaciones {@code @ManyToOne}, un
     * nivel: basta una de otra empresa para rechazar la baja.</li>
     * </ul>
     * Un valor nulo o que no sea numérico no se puede comparar y no frena la
     * baja; las demás reglas las aplica el servicio al grabar. No se usa
     * {@link IDataRow#checkFieldIdcompany}: con uno solo de los dos atributos
     * da por buena cualquier empresa (el que falta vale 0).
     *
     * @param sessionId identificador de la sesión.
     * @param existing registro existente que se eliminaría.
     * @return el mensaje de error, o {@code null} si se puede eliminar.
     */
    protected String checkDeleteRow(String sessionId, IDataRow existing) {
        IUserSession session = getUserSession();
        if (session == null) {
            return null;
        }
        Long idempresa = session.getIdCompany();
        Long idcompany = session.getCompany() == null ? null : session.getCompany().getIdcompany();
        Class<?> type = existing.getClass();
        if (DataInfo.isFieldExist((Class) type, "idempresa")) {
            return isOtherCompany(existing.getValue("idempresa"), idempresa) ? DELETE_OTHER_COMPANY : null;
        }
        if (DataInfo.isFieldExist((Class) type, "idcompany")) {
            return isOtherCompany(existing.getValue("idcompany"), idcompany) ? DELETE_OTHER_COMPANY : null;
        }
        for (Field field : DataInfo.getDeclaredFields(type)) {
            if (field.getAnnotation(jakarta.persistence.ManyToOne.class) == null) {
                continue;
            }
            Object parent = existing.getValue(field.getName());
            if (parent instanceof IDataRow
                    && DataInfo.isFieldExist((Class) parent.getClass(), "idempresa")
                    && isOtherCompany(((IDataRow) parent).getValue("idempresa"), idempresa)) {
                return DELETE_OTHER_COMPANY;
            }
        }
        return null;
    }

    /**
     * Indica si el valor de empresa de un registro es distinto del de la
     * sesión; un valor ausente o no numérico no se puede comparar.
     */
    private static boolean isOtherCompany(Object value, Long sessionCompany) {
        return value instanceof Number && sessionCompany != null
                && ((Number) value).longValue() != sessionCompany;
    }

    /**
     * Resuelve el registro que elimina una fila marcada: lo busca por id o por
     * clave única, aplica {@link #checkDeleteRow} y, si se pide, lo valida con
     * la acción {@code DELETE} ({@code checkDataRow}), para que la revisión
     * previa muestre las bajas que el servicio rechazaría al grabar.
     *
     * @param sessionId identificador de la sesión.
     * @param source fila marcada de la vista.
     * @param check {@code true} para validar con {@code checkDataRow}.
     * @return el registro con acción {@code DELETE}, o {@code null} si la fila
     * no se puede eliminar (el error queda en {@code source}).
     * @throws Exception si falla la conversión de la fila.
     */
    @SuppressWarnings("unchecked")
    private IDataRow resolveDeleteRow(String sessionId, T source, boolean check) throws Exception {
        IDataRow target = getDataService().copyTo(sessionId, source, getTargetType().getConstructor().newInstance());
        IDataRow existing = findExistingRow(sessionId, target);
        if (existing == null) {
            //Sin registro: se informan también los errores de lectura, que
            //pueden explicarlo (p. ej. una columna de la clave vacía).
            Map<String, IErrorInfo> readErrors = (Map<String, IErrorInfo>) source.getProperties().get(DELETE_READ_ERRORS);
            source.setErrors(readErrors != null ? new HashMap<>(readErrors) : new HashMap<>());
            source.setErrors(DELETE_NOT_FOUND, deleteColumn, 50000);
            return null;
        }
        String error = checkDeleteRow(sessionId, existing);
        if (error != null) {
            source.setErrors(error, deleteColumn, 50000);
            return null;
        }
        existing.setAction(IDataRow.DELETE);
        if (check) {
            Map<String, IErrorInfo> errorInfo = getDataService().checkDataRow(sessionId, existing);
            if (!errorInfo.isEmpty()) {
                source.setErrors(errorInfo);
                return null;
            }
        }
        return existing;
    }

    /**
     * Establece si durante la importación deben sobrescribirse los registros ya
     * existentes. El valor recibido se normaliza con
     * {@code Fn.toLogical} (nulo se interpreta como {@code false}).
     * <p>
     * Si cambia el valor efectivo de la opción, se descarta la
     * revisión previa ({@link #invalidateReview()}): la revisión no valida los
     * registros existentes que no se van a sobrescribir, y encenderlo después
     * de revisar los grabaría sin validar (E5-06 del plan CTBPRC de Maker).
     *
     * @param overWriteData {@code true} para sobrescribir los datos existentes;
     * {@code false} para solo insertar nuevos.
     */
    @Override
    public void setOverWriteData(Boolean overWriteData) {
        boolean previous = effectiveOverWrite();
        this.overWriteData = Fn.toLogical(overWriteData);
        if (previous != effectiveOverWrite()) {
            invalidateReview();
        }
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
     * Nombre de la hoja a procesar.
     *
     * @return el nombre, o {@code null} si la hoja se elige por posición
     * ({@link #getSheetIndex()}).
     */
    public String getSheetName() {
        return sheetName;
    }

    /**
     * Asigna la hoja a procesar por su nombre; tiene prioridad sobre
     * {@link #setSheetIndex(Integer)}.
     *
     * @param sheetName nombre de la hoja; {@code null} o vacío para elegirla
     * por posición.
     */
    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    /**
     * Posición de la hoja a procesar, <b>base 0</b> (la convención de POI).
     *
     * @return la posición, o {@code null} para la primera hoja.
     */
    public Integer getSheetIndex() {
        return sheetIndex;
    }

    /**
     * Asigna la hoja a procesar por su posición, <b>base 0</b>. Solo se usa si
     * no hay nombre de hoja ({@link #getSheetName()}).
     *
     * @param sheetIndex posición base 0; {@code null} para la primera hoja.
     */
    public void setSheetIndex(Integer sheetIndex) {
        this.sheetIndex = sheetIndex;
    }

    /**
     * Ubica en el libro asignado la hoja a procesar: por nombre
     * ({@link #getSheetName()}) si se indicó, si no por posición
     * ({@link #getSheetIndex()}, base 0) y, sin ninguna de las dos, la
     * primera. Es el único punto en que se elige la hoja: lo usan las dos
     * pasadas ({@link #checkValidation(Sheet)} e {@link #importData()}) y las
     * subclases para leer la fila de encabezados, de modo que validan e
     * importan siempre la misma hoja. Las subclases pueden sobrescribir los
     * getters para tomar la hoja de otra fuente (p. ej. la configuración de
     * la pantalla).
     *
     * @return la hoja; nunca {@code null}.
     * @throws IllegalStateException si no se asignó el libro o no tiene hojas.
     * @throws IllegalArgumentException si la hoja pedida no existe; el mensaje
     * nombra la hoja pedida y las disponibles.
     */
    public Sheet resolveSheet() {
        return findSheet(getExcelWorkbook(), getSheetName(), getSheetIndex());
    }

    /**
     * Ubica una hoja en un libro con las reglas de {@link #resolveSheet()}:
     * por nombre (sin distinguir mayúsculas, como Excel y
     * {@link Workbook#getSheet(String)}) si se indica, si no por posición
     * base 0 y, sin ninguna, la primera. Es la misma regla y los mismos
     * mensajes para quien muestre la hoja antes de procesar (p. ej. el
     * asistente de carga).
     *
     * @param workbook libro Excel.
     * @param name nombre de la hoja; {@code null} o vacío para usar la
     * posición.
     * @param index posición base 0; {@code null} para la primera.
     * @return la hoja; nunca {@code null}.
     * @throws IllegalStateException si el libro es nulo o no tiene hojas.
     * @throws IllegalArgumentException si la hoja pedida no existe; el mensaje
     * nombra la hoja pedida y las disponibles.
     */
    public static Sheet findSheet(Workbook workbook, String name, Integer index) {
        if (workbook == null) {
            throw new IllegalStateException("No se definio la planilla a procesar");
        }
        int sheets = workbook.getNumberOfSheets();
        if (sheets == 0) {
            throw new IllegalStateException("La planilla no tiene hojas para procesar");
        }
        if (name != null && !name.trim().isEmpty()) {
            Sheet sheet = workbook.getSheet(name.trim());
            if (sheet == null) {
                throw new IllegalArgumentException("La planilla no tiene la hoja «" + name.trim()
                        + "». Hojas: " + String.join(", ", getSheetNames(workbook)));
            }
            return sheet;
        }
        int position = Fn.nvl(index, 0);
        if (position < 0 || position >= sheets) {
            throw new IllegalArgumentException("La planilla tiene " + sheets + " hoja(s); se pidió la "
                    + (position + 1) + ". Hojas: " + String.join(", ", getSheetNames(workbook)));
        }
        return workbook.getSheetAt(position);
    }

    /**
     * Descarta la revisión previa ({@link #checkValidation(Sheet)}): la
     * siguiente {@link #importData()} vuelve a leer y validar la planilla. Se
     * llama cuando cambia lo que se va a importar después de validar (otra
     * hoja u otro archivo); si no, la importación grabaría las filas leídas
     * de la hoja anterior.
     */
    public void invalidateReview() {
        errorsReviewed = false;
        dataRows = new ArrayList();
        dataRowsError = new ArrayList();
    }

    /**
     * Nombres de las hojas de un libro, en orden.
     *
     * @param workbook libro Excel.
     * @return lista de nombres; vacía si el libro es nulo.
     */
    public static List<String> getSheetNames(Workbook workbook) {
        List<String> names = new ArrayList<>();
        if (workbook != null) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                names.add(workbook.getSheetName(i));
            }
        }
        return names;
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
     * @param sheet hoja a procesar; si es {@code null} se utiliza la de
     * {@link #resolveSheet()} (la primera, si no se indicó otra).
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
     * {@code IDataService.copyTo}, se invoca
     * {@link #onAfterRowConvert(IDataRow, IDataRow)}, se validan con Bean
     * Validation los atributos de las columnas
     * ({@link #checkBeanValidation(IExcelRowProcessor, IDataRow)}) y se valida
     * con {@code IDataService.checkDataRow}; los registros rechazados por
     * alguna de las dos validaciones (se informan juntos; si las dos marcan el
     * mismo atributo queda el error de {@code checkDataRow}) se acumulan en
     * {@link #getDataRowsError()} y los omitidos por los hooks se descartan.
     * Un registro que ya existe y no se va a sobreescribir
     * ({@link #getOverWriteData()} en {@code false}) no se valida: queda entre
     * los válidos y la grabación lo cuenta como «ya existe», igual que sin
     * revisión previa. Cuando la lectura se realiza dentro de
     * {@link #importData()} ({@code errorsReviewed} en {@code true}) este método
     * solo convierte las filas: los hooks y la validación corren después, en el
     * bucle de grabación.
     * <p>
     * La entidad destino de esta pasada se descarta: lo que {@code checkDataRow}
     * le completa (los valores por defecto de los {@code @NotNull} nulos) no
     * llega a la grabación, que vuelve a convertir la fila y completa esos
     * valores con {@link #completeNotNullDefaults(IExcelRowProcessor, IDataRow)}
     * (E4-23).
     *
     * <p>
     * Se leen las filas <b>posteriores</b> a la de encabezados
     * ({@code getHeaderRowIndex()}): las de arriba (títulos, logos) no son
     * datos, y el límite {@code EXCEL_IMPORT_ROWS_LIMIT} cuenta desde la
     * primera fila de datos.
     *
     * @param sheet hoja a procesar; si es {@code null} se utiliza la de
     * {@link #resolveSheet()} sobre el libro asignado con
     * {@link #setExcelWorkbook(Workbook)}.
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
        //Planilla a procesar (si no se indica, la que resuelve la configuración)
        if (sheet == null) {
            sheet = resolveSheet();
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
        //Las filas de datos empiezan después de la de encabezados (E0-01 de XLSSEQ).
        int firstRow = processor.getHeaderRowIndex() + 1;
        int lastRow = (rowCount <= 0) ? sheet.getLastRowNum()
                : Math.min(sheet.getLastRowNum(), firstRow + rowCount - 1);
        rowsReadedCount = 0;
        //Validación de la lógica de negocios solo si se pidió revisar los
        //errores antes de importar y aún no se hizo la revisión.
        boolean checkBusinessLogic = !errorsReviewed && getCheckBeforeErrors();
        IDataRow target;
        String sessionId = checkBusinessLogic ? getUserSession().getSessionId() : null;
        //Columna de marca de baja: opcional en la planilla; sin ella no se elimina nada.
        int deleteIndex = isAllowDelete() ? findDeleteColumn(sheet.getRow(processor.getHeaderRowIndex())) : -1;
        Boolean deleteAllowed = null;
        DataFormatter formatter = new DataFormatter();
        for (int r = firstRow; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            processor.setRow(row);
            source = processor.process();
            rowsReadedCount++;
            if (source != null) {
                if (deleteIndex >= 0 && isDeleteMark(row, deleteIndex, formatter)) {
                    //Una baja solo necesita los valores que ubican el registro:
                    //los errores de lectura (p. ej. una columna obligatoria vacía
                    //que no hace a la clave) no la frenan; se guardan por si el
                    //registro no aparece.
                    source.getProperties().put(DELETE_MARK, Boolean.TRUE);
                    source.getProperties().put(DELETE_READ_ERRORS, new HashMap<>(source.getErrors()));
                    source.setErrors(new HashMap<>());
                    if (checkBusinessLogic) {
                        if (deleteAllowed == null) {
                            deleteAllowed = checkDeleteAuthorization();
                        }
                        if (!deleteAllowed) {
                            source.setErrors(DELETE_NOT_AUTHORIZED, deleteColumn, 50000);
                            dataRowsError.add(source);
                            continue;
                        }
                        if (!onBeforeRowConvert(source)) {
                            continue;
                        }
                        if (resolveDeleteRow(sessionId, source, true) == null) {
                            dataRowsError.add(source);
                            continue;
                        }
                    }
                    dataRows.add(source);
                    continue;
                }
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
                    //Definir tipo de persitencia en la base: si el registro ya
                    //existe (por id o por clave única) se valida como UPDATE
                    //sobre el existente, para que la revisión no lo rechace
                    //como duplicado.
                    target = resolveExistingRow(sessionId, source, target);
                    if (target.getAction() == IDataRow.INSERT && !isAllowInsert()) {
                        source.setErrors(INSERT_NOT_ALLOWED, "", 50000);
                        dataRowsError.add(source);
                        continue;
                    }
                    //Despues de convertir
                    if (!onAfterRowConvert(source, target)) {
                        continue;
                    }
                    //Registro existente que no se va a grabar (sin «sobreescribir»):
                    //la grabación lo cuenta como «ya existe» sin validarlo, y la
                    //revisión hace lo mismo, para no rechazarlo por las reglas de
                    //modificación cuando nadie pidió cambiarlo (E5-05 del plan
                    //CTBPRC de Maker). Los hooks corren igual que antes.
                    if (target.getAction() == IDataRow.UPDATE && !getOverWriteData()) {
                        dataRows.add(source);
                        continue;
                    }
                    //Bean Validation de las columnas de la planilla, ANTES de
                    //checkDataRow: este completa con su valor por defecto los
                    //@NotNull nulos de la entidad descartable de esta pasada, y
                    //la grabación de una revisión previa no lo repite (E4-21).
                    Map<String, IErrorInfo> beanErrors = checkBeanValidation(processor, target);
                    //Validar
                    Map<String, IErrorInfo> errorInfo = getDataService().checkDataRow(sessionId, target);
                    if (!beanErrors.isEmpty()) {
                        //Con el mismo atributo gana el error de checkDataRow:
                        //es más específico (clave foránea, regla numerada).
                        Map<String, IErrorInfo> merged = new LinkedHashMap<>(errorInfo);
                        beanErrors.forEach(merged::putIfAbsent);
                        errorInfo = merged;
                    }
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
     * Valida con Bean Validation ({@code @NotNull}, {@code @Size}, etc.) los
     * atributos de la entidad destino que corresponden a las columnas de la
     * importación ({@link IExcelRowProcessor#getColumns()}), para que la
     * revisión previa rechace la fila que la grabación rechazaría al
     * persistirla (E4-21 del plan XLSEXP de Maker).
     * <p>
     * Se validan <b>solo</b> los atributos de las columnas, con
     * {@link Validator#validateProperty}, y no la entidad entera: muchas
     * entidades completan atributos obligatorios recién al grabar (la fecha
     * de modificación en {@code @PrePersist}/{@code @PreUpdate}, el usuario
     * en el DAO) y validarlas ahora rechazaría filas buenas. Queda fuera, por
     * lo mismo, un atributo obligatorio que no es columna de la planilla: lo
     * sigue juzgando la grabación, y el motivo del rechazo llega al log
     * ({@link #getResultErrors(IDataResult)}). Igual que en
     * {@code resolveExistingRow}, el atributo de la entidad es el que se
     * llama como el de la vista; uno que solo existe en la vista no se valida.
     * <p>
     * No se valida el registro existente que no se va a grabar (sin
     * «sobreescribir») ni, si no hay implementación de Bean Validation
     * ({@link #getValidator()} nulo), nada.
     *
     * @param processor procesador de la corrida, con las columnas.
     * @param target entidad a grabar, ya resuelta (alta o existente).
     * @return los errores por atributo (clave en minúsculas, número 50000,
     * mensaje que nombra la columna); vacío si no hay.
     */
    protected Map<String, IErrorInfo> checkBeanValidation(IExcelRowProcessor<T> processor, IDataRow target) {
        Map<String, IErrorInfo> errors = new LinkedHashMap<>();
        if (processor == null || target == null) {
            return errors;
        }
        if (target.getAction() == IDataRow.UPDATE && !getOverWriteData()) {
            return errors;
        }
        Validator beanValidator = getValidator();
        if (beanValidator == null) {
            return errors;
        }
        Class<?> type = target.getClass();
        for (ExcelColumnSpec spec : processor.getColumns()) {
            String fieldName = spec.getField();
            if (fieldName == null || fieldName.isEmpty()
                    || !DataInfo.isFieldExist((Class) type, fieldName)) {
                continue;
            }
            StringBuilder message = new StringBuilder();
            for (ConstraintViolation<IDataRow> violation : beanValidator.validateProperty(target, fieldName)) {
                if (message.length() > 0) {
                    message.append(", ");
                }
                message.append(violation.getMessage());
            }
            if (message.length() > 0) {
                errors.put(fieldName.toLowerCase(),
                        new ErrorInfo("La columna «" + spec.getHeader() + "» no es válida: " + message,
                                50000, fieldName));
            }
        }
        return errors;
    }

    /**
     * Completa con su valor por defecto los atributos {@code @NotNull} nulos
     * de la entidad a grabar que <b>no</b> son columnas de la planilla, igual
     * que lo hace {@code checkDataRow}. Se usa en la grabación de una
     * importación con revisión previa, que no repite {@code checkDataRow}: la
     * revisión los completó sobre una entidad que después se descartó, y la
     * fila que pasó la revisión se rechazaba al grabar (E4-23 del plan XLSEXP
     * de Maker; decisión del usuario D60).
     * <p>
     * Reproduce, sin repetir el resto de las validaciones ni sus consultas, el
     * bloque de {@code AbstractDataService.checkDataRow} que llama a
     * {@code AbstractDataService.setDefault} (módulo {@code jbs-business}); si
     * ese bloque cambia, hay que cambiar este método con él:
     * <ul>
     * <li>solo en altas y modificaciones, y no si la entidad tiene la
     * propiedad {@code FOREIGNKEY_NOCHECK} (la puede poner un gancho como
     * {@link #onAfterRowConvert(IDataRow, IDataRow)});</li>
     * <li>sobre los atributos declarados en la clase de la entidad
     * ({@code DataInfo.getDeclaredFields}, sin las superclases), salteando los
     * que llevan {@code @CheckForeignkey(check = false)};</li>
     * <li>con el valor del tipo del atributo: 0 para {@code BigDecimal},
     * {@code Long}, {@code Integer} y {@code Short}, {@code false} para
     * {@code Boolean}; los demás tipos quedan nulos.</li>
     * </ul>
     * Los atributos de las columnas ({@link IExcelRowProcessor#getColumns()})
     * no se tocan: los decide la planilla, y la revisión ya los controló con
     * Bean Validation ({@link #checkBeanValidation(IExcelRowProcessor, IDataRow)}).
     * Así una celda vacía con {@code defaultWhenBlank} que al sobreescribir
     * conserva lo guardado (D59, {@link ExcelRowProcessor#DEFAULTED_FIELDS})
     * sigue igual. No informa errores: un atributo que no se puede completar
     * queda como estaba y lo juzga la grabación.
     *
     * @param processor procesador de la corrida, con las columnas; puede ser
     * {@code null} (no se excluye ninguna).
     * @param target entidad a grabar, ya resuelta (alta o existente).
     */
    protected void completeNotNullDefaults(IExcelRowProcessor<T> processor, IDataRow target) {
        if (target == null || !Fn.inList(target.getAction(), IDataRow.INSERT, IDataRow.UPDATE)) {
            return;
        }
        if (target.getProperties() != null
                && Fn.toLogical(target.getProperties().get("FOREIGNKEY_NOCHECK"))) {
            return;
        }
        Set<String> columnFields = new HashSet<>();
        if (processor != null && processor.getColumns() != null) {
            for (ExcelColumnSpec spec : processor.getColumns()) {
                if (spec.getField() != null && !spec.getField().isEmpty()) {
                    columnFields.add(spec.getField().toLowerCase());
                }
            }
        }
        for (Field field : DataInfo.getDeclaredFields(target.getClass())) {
            String fieldName = field.getName();
            if (columnFields.contains(fieldName.toLowerCase())) {
                continue;
            }
            CheckForeignkey foreignKey = field.getAnnotation(CheckForeignkey.class);
            if (foreignKey != null && !foreignKey.check()) {
                continue;
            }
            if (field.getAnnotation(NotNull.class) == null) {
                continue;
            }
            try {
                if (target.getValue(fieldName) != null) {
                    continue;
                }
                Object value = notNullDefaultValue(target.getFieldType(fieldName));
                if (value != null) {
                    target.setValue(fieldName, value);
                }
            } catch (Exception e) {
                //Como en checkDataRow, no corta la fila: queda como estaba y,
                //si es un problema, la grabación lo rechaza con su motivo.
            }
        }
    }

    /**
     * Valor por defecto de {@code AbstractDataService.setDefault} para un
     * atributo {@code @NotNull} nulo, según su tipo.
     *
     * @param type tipo del atributo.
     * @return 0 o {@code false} según el tipo; {@code null} si el tipo no
     * tiene valor por defecto.
     */
    private static Object notNullDefaultValue(Class<?> type) {
        if (type == BigDecimal.class) {
            return BigDecimal.ZERO;
        } else if (type == Boolean.class) {
            return Boolean.FALSE;
        } else if (type == Long.class) {
            return 0L;
        } else if (type == Integer.class) {
            return 0;
        } else if (type == Short.class) {
            return Short.valueOf("0");
        }
        return null;
    }

    /**
     * Devuelve el validador de Bean Validation con el que la revisión previa
     * controla las columnas ({@link #checkBeanValidation}). Lo busca una sola
     * vez: primero el del contenedor ({@code java:comp/Validator}) y, si no
     * está, el de la implementación por omisión; sin implementación disponible
     * (p. ej. en una prueba unitaria) devuelve {@code null} y la validación se
     * omite. Las subclases o las pruebas pueden sobrescribirlo.
     *
     * @return el validador, o {@code null} si no hay implementación.
     */
    protected Validator getValidator() {
        if (!validatorResolved) {
            validatorResolved = true;
            try {
                validator = (Validator) new InitialContext().lookup("java:comp/Validator");
            } catch (Exception | LinkageError e) {
                validator = null;
            }
            if (validator == null) {
                try {
                    validator = Validation.buildDefaultValidatorFactory().getValidator();
                } catch (Exception | LinkageError e) {
                    validator = null;
                }
            }
        }
        return validator;
    }

    /**
     * Errores de una grabación fallida, para dejarlos en la fila de la
     * planilla (y en el log). Si el resultado trae el mapa de errores se usa
     * tal cual. Si no —la grabación cortó por una excepción y el DAO solo dejó
     * el texto—, se arman a partir de la excepción: una violación de Bean
     * Validation da un error por atributo (número 50000); cualquier otra, un
     * error con el texto del resultado y el número 1 (error que viene de una
     * excepción). Antes la fila quedaba sin motivo (E4-21 del plan XLSEXP de
     * Maker).
     *
     * @param result resultado de la grabación fallida.
     * @return los errores; nunca {@code null}.
     */
    protected Map<String, IErrorInfo> getResultErrors(IDataResult result) {
        Map<String, IErrorInfo> errors = new LinkedHashMap<>();
        if (result == null) {
            return errors;
        }
        if (result.getErrorsMap() != null && !result.getErrorsMap().isEmpty()) {
            return result.getErrorsMap();
        }
        ConstraintViolationException violations = findConstraintViolation(result.getException());
        if (violations != null && violations.getConstraintViolations() != null) {
            //Un error por atributo, con todos sus motivos.
            Map<String, StringBuilder> byProperty = new LinkedHashMap<>();
            for (ConstraintViolation<?> violation : violations.getConstraintViolations()) {
                String property = String.valueOf(violation.getPropertyPath());
                StringBuilder message = byProperty.get(property);
                if (message == null) {
                    byProperty.put(property, new StringBuilder(Fn.nvl(violation.getMessage(), "")));
                } else {
                    message.append(", ").append(violation.getMessage());
                }
            }
            byProperty.forEach((property, message) -> errors.put(property.toLowerCase(),
                    new ErrorInfo("«" + property + "» " + message, 50000, property)));
            if (!errors.isEmpty()) {
                return errors;
            }
        }
        String message = Fn.nvl(result.getErrorMsg(), "").trim();
        if (message.isEmpty() && result.getException() != null) {
            message = Fn.nvl(result.getException().getMessage(), result.getException().getClass().getSimpleName());
        }
        if (message.isEmpty()) {
            message = "No se pudo grabar el registro";
        }
        errors.put("datageneric", new ErrorInfo(message, 1, ""));
        return errors;
    }

    /**
     * Busca una violación de Bean Validation en la cadena de causas.
     *
     * @param exp excepción de la grabación; puede ser {@code null}.
     * @return la violación, o {@code null} si no hay.
     */
    private static ConstraintViolationException findConstraintViolation(Throwable exp) {
        Throwable cause = exp;
        int depth = 0;
        while (cause != null && depth++ < 20) {
            if (cause instanceof ConstraintViolationException) {
                return (ConstraintViolationException) cause;
            }
            cause = cause.getCause();
        }
        return null;
    }

    /** Mensaje de la fila que no existe cuando la importación no permite altas. */
    static final String INSERT_NOT_ALLOWED = "No existe el registro y la importación no permite altas";

    /**
     * Ubica en la fila de cabeceras la columna de marca de baja.
     *
     * @return el índice de la columna, o {@code -1} si la planilla no la trae.
     */
    private int findDeleteColumn(Row header) {
        if (header == null || deleteColumn == null || deleteColumn.trim().isEmpty()) {
            return -1;
        }
        //Mismo criterio que checkMetaData: la primera cabecera en blanco
        //marca el fin de las columnas útiles.
        short last = header.getLastCellNum();
        for (int c = 0; c < last; c++) {
            Cell cell = header.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            if (cell.getCellType() == CellType.BLANK
                    || (cell.getCellType() == CellType.STRING && cell.getStringCellValue().trim().isEmpty())) {
                break;
            }
            if (cell.getCellType() == CellType.STRING
                    && deleteColumn.trim().equalsIgnoreCase(cell.getStringCellValue().trim())) {
                return c;
            }
        }
        return -1;
    }

    /**
     * Indica si la celda de la columna de marca tiene el valor de baja.
     */
    private boolean isDeleteMark(Row row, int index, DataFormatter formatter) {
        Cell cell = row.getCell(index);
        if (cell == null) {
            return false;
        }
        String value = formatter.formatCellValue(cell);
        return value != null && deleteValue != null && deleteValue.trim().equalsIgnoreCase(value.trim());
    }

    /**
     * Busca el registro existente de la entidad convertida, por id o por clave
     * única, sin modificarlo.
     *
     * @param sessionId identificador de la sesión.
     * @param target entidad convertida desde la fila.
     * @return el registro existente, o {@code null} si no existe o no se pudo
     * buscar.
     */
    protected IDataRow findExistingRow(String sessionId, IDataRow target) {
        try {
            if (target.getId() != null) {
                return getDataService().findById(getTargetType(), sessionId, target.getId());
            }
            return getDataService().findByUk(sessionId, target);
        } catch (Exception e) {
            logResult("  No se pudo buscar el registro existente ("
                    + getTargetType().getSimpleName() + "): " + e.getMessage());
            return null;
        }
    }

    /**
     * Resuelve si la entidad convertida ya existe en la base y deja definida la
     * acción de persistencia. Primero por el identificador que haya resuelto la
     * vista ({@code getId()}, típicamente con la fórmula {@code fn_idxxx} de su
     * {@code @Id}), cargando el registro por id; si no lo trae, por la <b>clave
     * única</b> de la entidad ({@link IDataService#findByUk}, la misma búsqueda que hace
     * {@code importFrom} en la capa de servicios), que ya puede evaluarse
     * porque {@code copyTo} resolvió las asociaciones que la componen.
     * <p>
     * Si existe, devuelve el <b>registro existente</b> con acción
     * {@code UPDATE}: cuando {@link #getOverWriteData()} está activo se le
     * copian encima los valores no nulos que trajo la planilla (las columnas
     * que la planilla no trae conservan su valor: es una actualización, no un
     * reemplazo); si no está activo se devuelve tal cual, y el llamador lo
     * cuenta como «ya existente» sin grabarlo. Si no existe, o la búsqueda por
     * clave única falla (entidad sin clave única declarada, o consulta que no se
     * pudo ejecutar), devuelve la entidad convertida con acción {@code INSERT},
     * que es el comportamiento histórico; en ese caso un duplicado real lo
     * rechaza después {@code checkDataRow} con el error 50001.
     *
     * <p>
     * Esta sobrecarga no conoce la fila de la vista, así que <b>no</b> aplica
     * la protección de los valores por defecto ni la de las columnas de solo
     * alta (ver {@link #resolveExistingRow(String, IDataRow, IDataRow)}): una
     * subclase que la llame directamente obtiene la copia «solo no nulos»
     * completa. El flujo de la importación usa siempre la de tres argumentos.
     *
     * @param sessionId identificador de la sesión del usuario.
     * @param target entidad destino recién convertida desde la fila de la vista.
     * @return la entidad a validar y persistir, con su acción definida.
     * @throws Exception si falla la copia sobre el registro existente.
     */
    protected IDataRow resolveExistingRow(String sessionId, IDataRow target) throws Exception {
        return resolveExistingRow(sessionId, null, target);
    }

    /**
     * Variante de {@link #resolveExistingRow(String, IDataRow)} que conoce la
     * fila de la vista de la que salió {@code target}: si el registro existe y
     * se sobreescribe, los atributos que la vista completó con el <b>valor por
     * defecto de la columna</b> —columna ausente, o celda vacía con
     * {@code defaultWhenBlank}— ({@link ExcelRowProcessor#DEFAULTED_FIELDS})
     * y los de las columnas declaradas con {@code noOverwrite()}
     * ({@link ExcelRowProcessor#NO_OVERWRITE_FIELDS}) <b>no</b> se copian sobre
     * el existente, que conserva su valor (decisión del usuario, 2026-09-16: lo
     * que la planilla no trae no pisa lo que hay, y una columna puede declararse
     * como de solo alta; la celda vacía se sumó el 2026-11-25, D59 del plan
     * XLSEXP de Maker).
     *
     * <p>
     * Excepción para el NULL obligatorio: si la vista completó el atributo con
     * el valor por defecto de la columna ({@link ExcelRowProcessor#DEFAULTED_FIELDS}:
     * columna ausente, o celda vacía con {@code defaultWhenBlank}) y el
     * existente tiene NULL en ese atributo {@code @NotNull}, el valor por
     * defecto <b>sí</b> se copia, para que el dato quede válido, aunque la
     * columna sea {@code noOverwrite()} (D61 y D63 del plan XLSEXP de Maker,
     * hallazgo E4-24). Con lo guardado no nulo rigen D59 y {@code noOverwrite()},
     * y un valor que vino en la celda de una columna {@code noOverwrite()} no
     * se copia nunca. Como la revisión previa y la grabación resuelven el
     * existente con este mismo método, la revisión ya valida la entidad con
     * el default y no la rechaza, y con revisión o sin ella se graba lo mismo.
     *
     * <p>
     * Las dos protecciones actúan sobre el atributo de la <b>entidad destino</b>
     * que se llama igual que el de la vista (la anotación guarda el nombre del
     * atributo de la vista y la anulación exige que exista con ese nombre en
     * {@code target}). Un atributo que solo existe en la vista —por ejemplo un
     * parámetro de una {@code fn_id*} que no es columna de la tabla— no tiene
     * qué anular: ahí {@code overwrite="false"} no protege nada y el valor
     * sigue participando en la resolución de la clave.
     *
     * <p>
     * La búsqueda del existente es tolerante: si falla (entidad sin clave
     * única, consulta que no se pudo ejecutar) se sigue como alta y lo decide
     * {@code checkDataRow}. La copia sobre el existente <b>no</b>: un error en
     * esa fase es de programación (setter que lanza, tipo no asignable) y se
     * propaga, para no grabar una entidad a medio anular como actualización.
     *
     * <p>
     * El registro resuelto por id puede pertenecer a <b>otra empresa</b> cuando
     * la función {@code fn_id*} de la vista reintenta con
     * {@code fn_empresashared} (catálogos compartidos entre empresas) y
     * {@code findById} no filtra por empresa: se acepta tal cual (decisión del
     * usuario, 2026-09-16; caso a analizar por separado).
     *
     * @param sessionId identificador de la sesión del usuario.
     * @param source fila de la vista leída de la planilla; puede ser
     * {@code null}.
     * @param target entidad destino recién convertida desde la fila de la vista.
     * @return la entidad a validar y persistir, con su acción definida.
     * @throws Exception si falla la copia de la planilla sobre el registro
     * existente (la búsqueda del existente no lanza: se registra y se sigue
     * como alta).
     */
    @SuppressWarnings("unchecked")
    protected IDataRow resolveExistingRow(String sessionId, IDataRow source, IDataRow target) throws Exception {
        IDataRow existing = null;
        try {
            if (target.getId() != null) {
                //La vista ya resolvió el identificador (su @Id lleva la fórmula
                //fn_idxxx del diccionario): se carga el registro para actualizarlo,
                //no para reemplazarlo. Si no se lo encuentra, se graba el convertido
                //con su id (comportamiento histórico).
                existing = getDataService().findById(getTargetType(), sessionId, target.getId());
                if (existing == null) {
                    target.setAction(IDataRow.UPDATE);
                    return target;
                }
            } else {
                existing = getDataService().findByUk(sessionId, target);
            }
        } catch (Exception e) {
            //Sin clave única evaluable: se sigue como alta y decide checkDataRow.
            logResult("  No se pudo buscar el registro existente ("
                    + getTargetType().getSimpleName() + "): " + e.getMessage());
            existing = null;
        }
        if (existing == null) {
            target.setAction(target.getId() != null ? IDataRow.UPDATE : IDataRow.INSERT);
            return target;
        }
        //Fase de copia, fuera del try: un fallo acá no es «sin clave única» y
        //no debe devolver la entidad a medio anular como actualización.
        if (getOverWriteData()) {
            //Solo los valores que trajo la planilla; el resto se conserva.
            //Un atributo completado por el default de una columna AUSENTE,
            //o de una celda vacía con defaultWhenBlank (D59), no vino en la
            //planilla: se anula en la copia para no pisar el valor del
            //existente (copyTo con onlyFieldsNotNulls).
            //Lo mismo para las columnas declaradas noOverwrite(): se graban
            //solo en las altas.
            //Excepción (D61, D63): el default de la columna sobre un NULL de
            //un atributo @NotNull del existente sí se graba, también en una
            //columna noOverwrite().
            if (source != null) {
                clearProtectedFields(target, source, existing);
            }
            //Las colecciones hijas (@OneToMany) de la entidad convertida
            //no vienen de la planilla: si la entidad las inicializa con
            //una lista vacía, copyTo las copiaría (no son nulas) y el
            //existente perdería sus hijos cargados.
            clearCollections(target);
            target.copyTo(existing, true);
        }
        existing.setAction(IDataRow.UPDATE);
        return existing;
    }

    /**
     * Anula en {@code target} los atributos que no deben pisar al existente:
     * los que la vista completó con el valor por defecto
     * ({@link ExcelRowProcessor#DEFAULTED_FIELDS}, D59) y los de las columnas
     * {@code noOverwrite()} ({@link ExcelRowProcessor#NO_OVERWRITE_FIELDS}),
     * para que la copia «solo no nulos» no los toque. Excepción: un atributo
     * completado con el valor por defecto que es {@code @NotNull} y está NULL
     * en el existente conserva el default y se copia, aunque su columna sea
     * {@code noOverwrite()} (D61 y D63, E4-24). Un valor que vino en la celda
     * de una columna {@code noOverwrite()} no está en {@code DEFAULTED_FIELDS}
     * y se anula siempre.
     *
     * @param target entidad convertida que se va a copiar sobre el existente.
     * @param source fila de la vista con las anotaciones del procesador.
     * @param existing registro existente sobre el que se copia.
     * @throws Exception si falla la asignación.
     */
    @SuppressWarnings("unchecked")
    private void clearProtectedFields(IDataRow target, IDataRow source, IDataRow existing) throws Exception {
        Set<String> defaulted = (Set<String>) source.getProperties().get(ExcelRowProcessor.DEFAULTED_FIELDS);
        Set<String> noOverwrite = (Set<String>) source.getProperties().get(ExcelRowProcessor.NO_OVERWRITE_FIELDS);
        Set<String> fields = new LinkedHashSet<>();
        if (defaulted != null) {
            fields.addAll(defaulted);
        }
        if (noOverwrite != null) {
            fields.addAll(noOverwrite);
        }
        for (String fieldName : fields) {
            if (!DataInfo.isFieldExist(target.getClass(), fieldName)) {
                continue;
            }
            if (defaulted != null && defaulted.contains(fieldName)
                    && isNullNotNull(existing, fieldName)) {
                continue;
            }
            target.setValue(fieldName, null);
        }
    }

    /**
     * Indica si el atributo de la entidad es {@code @NotNull} y está NULL.
     *
     * @param row entidad (el registro existente).
     * @param fieldName atributo.
     * @return {@code true} si el atributo es obligatorio y no tiene valor.
     */
    private static boolean isNullNotNull(IDataRow row, String fieldName) {
        Field field = DataInfo.getDeclaredField(row.getClass(), fieldName);
        return field != null && field.getAnnotation(NotNull.class) != null
                && row.getValue(fieldName) == null;
    }

    /**
     * Anula en la entidad convertida todo atributo persistente cuyo tipo es una
     * colección ({@link Collection}; es el caso de las asociaciones
     * {@code @OneToMany}), para que {@code copyTo(existing, true)} no lo copie
     * sobre el registro existente. Una entidad suele inicializar esas listas
     * con {@code new ArrayList<>()}: no son nulas, así que la copia «solo no
     * nulos» las pasaba igual y el existente perdía los hijos que tenía
     * cargados (su servicio de datos los reinsertaba después con la misma
     * clave). Los hijos nunca salen de una celda de la planilla, así que en
     * una actualización se conservan siempre los del existente. Se recorren
     * los mismos atributos que {@code DataRow.copyTo}: los declarados en la
     * clase de la entidad que llevan alguna anotación.
     *
     * @param target entidad convertida desde la fila de la planilla.
     * @throws Exception si falla la asignación de algún atributo.
     */
    private void clearCollections(IDataRow target) throws Exception {
        for (Field field : target.getClass().getDeclaredFields()) {
            if (field.getAnnotations().length == 0) {
                continue;
            }
            if (Collection.class.isAssignableFrom(field.getType())) {
                target.setValue(field.getName(), null);
            }
        }
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
     * {@code IDataService.copyTo}, asignación de la acción
     * ({@code INSERT}/{@code UPDATE}; los {@code UPDATE} se omiten salvo que
     * {@link #getOverWriteData()} sea {@code true}, contabilizados en
     * {@link #getRowsExistCount()}), {@link #onAfterRowConvert(IDataRow, IDataRow)},
     * validación con {@code IDataService.checkDataRow} y persistencia con
     * {@code IDataService.save} (los grabados se contabilizan en
     * {@link #getRowsMigratedCount()}). Los registros con error se acumulan en
     * {@link #getDataRowsError()}, con el motivo del rechazo aunque la
     * grabación haya cortado por una excepción
     * ({@link #getResultErrors(IDataResult)}), y el log los lista con su
     * número de mensaje. Si los registros ya fueron validados en la
     * pasada de {@link #checkValidation(Sheet)}, el {@code checkDataRow} no se
     * repite (se marca el registro como verificado); en ese caso los duplicados
     * dentro de la misma planilla no se detectan entre sí, dado que ninguno
     * existía en la base al momento de la revisión. Lo que sí se repite es la
     * parte de {@code checkDataRow} que completa los valores por defecto de los
     * atributos {@code @NotNull} que no son columnas de la planilla
     * ({@link #completeNotNullDefaults(IExcelRowProcessor, IDataRow)}): la
     * revisión los completó sobre una entidad que se descartó (E4-23).</li>
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
        rowsDeletedCount = 0;
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
            //Filas con error que ya venían de la revisión previa: lo que se agregue
            //durante la grabación (fila descartada por onBeforeRowConvert /
            //onAfterRowConvert, o update fallido) también cuenta como error del
            //proceso para el cierre.
            int erroresPrevios = getDataRowsError().size();
            Boolean deleteAllowed = null;
            for (T source : getDataRows()) {
                if (isAllowDelete() && isMarkedForDelete(source)) {
                    //Baja de una fila marcada.
                    if (deleteAllowed == null) {
                        deleteAllowed = checkDeleteAuthorization();
                    }
                    if (!deleteAllowed) {
                        source.setErrors(DELETE_NOT_AUTHORIZED, deleteColumn, 50000);
                        getDataRowsError().add(source);
                        continue;
                    }
                    if (!onBeforeRowConvert(source)) {
                        continue;
                    }
                    //checkAndSave valida con la acción DELETE: no hace falta antes.
                    IDataRow existing = resolveDeleteRow(sessionId, source, false);
                    if (existing == null) {
                        getDataRowsError().add(source);
                        continue;
                    }
                    IDataResult deleted = getDataService().checkAndSave(sessionId, existing);
                    if (!deleted.isSuccessFul()) {
                        source.setErrors(getResultErrors(deleted));
                        getDataRowsError().add(source);
                    } else {
                        rowsDeletedCount++;
                        logResult("  Eliminado: " + getRowLogIdentifier(source));
                    }
                    continue;
                }
                //Antes de la conversion a la instancia targetType
                if (!onBeforeRowConvert(source)) {
                    continue;
                }
                //Convertir a una objeto targetType
                target = getDataService().copyTo(sessionId, source, getTargetType().getConstructor().newInstance());
                //Definir tipo de persitencia en la base (INSERT, o UPDATE sobre
                //el registro existente resuelto por id o por clave única).
                target = resolveExistingRow(sessionId, source, target);
                //Si no existe y no se permiten altas.
                if (target.getAction() == IDataRow.INSERT && !isAllowInsert()) {
                    source.setErrors(INSERT_NOT_ALLOWED, "", 50000);
                    getDataRowsError().add(source);
                    continue;
                }
                //Si existe el registro y no se permite sobreescribir.
                if (target.getAction() == IDataRow.UPDATE && !getOverWriteData()) {
                    rowsExistCount++;
                    logResult("  Ya existe, no se sobreescribe: " + getRowLogIdentifier(source));
                    continue;
                }
                //Despues de convertir
                if (!onAfterRowConvert(source, target)) {
                    continue;
                }
                //Validar
                if (!reviewed) {
                    Map<String, IErrorInfo> errorInfo = getDataService().checkDataRow(sessionId, target);
                    if (!errorInfo.isEmpty()) {
                        source.setErrors(errorInfo);
                        getDataRowsError().add(source);
                        error = true;
                        continue;
                    }
                } else {
                    //La revisión previa ya validó, pero los valores por defecto
                    //que checkDataRow puso en los @NotNull fuera de la planilla
                    //quedaron en la entidad descartada de esa pasada (E4-23).
                    completeNotNullDefaults(getExcelRowProcessor(), target);
                    target.setRowChecked(true);
                }
                //Grabar
                IDataResult result = getDataService().save(sessionId, target);
                if (!result.isSuccessFul()) {
                    source.setErrors(getResultErrors(result));
                    getDataRowsError().add(source);
                } else {
                    rowsMigratedCount++;
                }
            }
            //Fin proceso: el recorrido se completó (haya o no filas con errores)
            importOk = true;
            if (error || getDataRowsError().size() > erroresPrevios) {
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
                    + (rowsDeletedCount > 0 || isAllowDelete() ? ", eliminados: " + rowsDeletedCount : "")
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
