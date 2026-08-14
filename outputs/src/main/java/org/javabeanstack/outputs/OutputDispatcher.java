/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
*        jenciso@javabeanstack.org
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
package org.javabeanstack.outputs;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.error.ErrorReg;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.log.ILogManager;
import org.javabeanstack.model.IAppLogRecord;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.util.Fn;
import org.javabeanstack.util.Strings;

/**
 * Orquestador del subsistema de salida de documentos: encadena una
 * {@link IDocumentSource} (que produce el documento en memoria) con uno o más
 * {@link IDocumentTarget} (que lo entregan por su canal), garantizando el orden
 * <b>primero generar, después comprometer el destino</b> y generando una sola
 * vez cualquiera sea la cantidad de destinos.
 *
 * <p>Se usa por composición desde los controllers, al estilo de
 * {@code JasperReportUtil}:</p>
 *
 * <pre>
 * List&lt;IErrorReg&gt; results = new OutputDispatcher(getUserSession())
 *     .source(wordSource)                      // o jasperSource, excelSource...
 *     .fileName("transferencia_12.docx")       // opcional, pisa el de la fuente
 *     .to(new DownloadTarget())                // uno o más destinos
 *     .to(new FolderTarget("/datos/docs/"))
 *     .log(logMngr, AppLogEvent.class)         // opcional
 *     .execute();
 * </pre>
 *
 * <p>
 * {@link #execute()} nunca lanza (al estilo de {@code MailSender.send()}):
 * todo fallo —de generación o de entrega— vuelve como {@link IErrorReg} y el
 * llamador decide qué mostrar. Un fallo de generación no ejecuta ningún
 * destino; un fallo en un destino no impide los demás.
 * </p>
 *
 * <p>
 * Este orquestador no depende de ninguna tecnología concreta: las fuentes y
 * destinos con dependencias pesadas viven en el módulo dueño de cada una
 * ({@code jbs-jasper}, {@code jbs-messaging}, {@code jbs-poi},
 * {@code jbs-web}).
 * </p>
 *
 * @author Jorge Enciso
 */
public class OutputDispatcher {

    private static final Logger LOGGER = LogManager.getLogger(OutputDispatcher.class);

    /** Nombre de canal informado en el log para la impresión directa. */
    public static final String CHANNEL_PRINTER = "printer";

    private final IUserSession userSession;
    private final List<IDocumentTarget> targets = new ArrayList();
    private IDocumentSource source;
    private String fileName;
    private ILogManager logMngr;
    private Class<? extends IAppLogRecord> logType;
    private IOutputDocument document;

    /**
     * Crea el orquestador sin contexto de sesión (sin posibilidad de log).
     */
    public OutputDispatcher() {
        this(null);
    }

    /**
     * Crea el orquestador con el contexto del usuario, necesario para el
     * registro opcional en el log de la aplicación.
     *
     * @param userSession sesión del usuario, o nulo si no se va a loguear.
     */
    public OutputDispatcher(IUserSession userSession) {
        this.userSession = userSession;
    }

    /**
     * Asigna la fuente que producirá el documento.
     *
     * @param source fuente de generación.
     * @return esta instancia, para encadenar.
     */
    public OutputDispatcher source(IDocumentSource source) {
        this.source = source;
        return this;
    }

    /**
     * Asigna el nombre de archivo del documento, pisando el que la fuente
     * determine. Opcional.
     *
     * @param fileName nombre del archivo con su extensión.
     * @return esta instancia, para encadenar.
     */
    public OutputDispatcher fileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    /**
     * Agrega un destino de entrega. Puede llamarse varias veces: el documento
     * se genera una sola vez y se entrega a todos.
     *
     * @param target destino de entrega.
     * @return esta instancia, para encadenar.
     */
    public OutputDispatcher to(IDocumentTarget target) {
        if (target != null) {
            targets.add(target);
        }
        return this;
    }

    /**
     * Activa el registro de cada entrega en el log de la aplicación
     * (evento {@link IAppLogRecord#EVENT_DOCUMENT_OUTPUT}). Requiere que el
     * orquestador se haya creado con la sesión del usuario.
     *
     * @param logMngr administrador del log de la aplicación.
     * @param logType clase de la entidad de log de la aplicación (ej.
     * {@code AppLogEvent.class}); {@code dbWrite} la necesita para persistir.
     * @return esta instancia, para encadenar.
     */
    public OutputDispatcher log(ILogManager logMngr, Class<? extends IAppLogRecord> logType) {
        this.logMngr = logMngr;
        this.logType = logType;
        return this;
    }

    /**
     * Ejecuta la salida: genera el documento con la fuente y lo entrega a cada
     * destino en el orden en que fueron agregados.
     *
     * <p>
     * Nunca lanza: los fallos vuelven como {@link IErrorReg}. Si la generación
     * falla, devuelve un único resultado con ese error y ningún destino se
     * ejecuta. Si un destino falla, los siguientes se ejecutan igual.
     * </p>
     *
     * @return un resultado por destino ejecutado (o uno solo si falló la
     * generación o la configuración); sin error los exitosos.
     */
    public List<IErrorReg> execute() {
        List<IErrorReg> results = new ArrayList();
        if (source == null) {
            results.add(error("No se asignó la fuente del documento (source)"));
            return results;
        }
        if (targets.isEmpty()) {
            results.add(error("No se asignó ningún destino (to)"));
            return results;
        }
        //1) Generar una sola vez, sin comprometer ningún destino.
        try {
            document = source.generate();
            if (document == null || document.getSize() == 0) {
                throw new Exception("La fuente no produjo ningún documento");
            }
            if (!Strings.isNullorEmpty(fileName)) {
                document.setFileName(fileName);
            }
        } catch (Exception ex) {
            //Fallo manejado: vuelve como IErrorReg; en el log técnico va como WARN.
            LOGGER.warn("Falló la generación del documento", ex);
            IErrorReg generationError = error(ex.getMessage());
            generationError.setException(ex);
            writeLog("generate", generationError);
            results.add(generationError);
            return results;
        }
        //2) Entregar a cada destino; el fallo de uno no detiene a los demás.
        for (IDocumentTarget target : targets) {
            IErrorReg result;
            try {
                result = target.deliver(document);
                if (result == null) {
                    result = new ErrorReg();
                }
            } catch (Exception ex) {
                //Fallo manejado: vuelve como IErrorReg; en el log técnico va como WARN.
                LOGGER.warn("Falló la entrega por el canal " + target.getChannelName(), ex);
                result = error(ex.getMessage());
                result.setException(ex);
            }
            writeLog(target.getChannelName(), result);
            results.add(result);
        }
        return results;
    }

    /**
     * Imprime la salida directo en la impresora del servidor. Solo es posible
     * si la fuente implementa {@link IPrintableSource} (hoy, los reportes
     * Jasper); no produce ningún documento y no toca la respuesta HTTP.
     *
     * @return resultado de la impresión; sin error si fue exitosa.
     */
    public IErrorReg toPrinter() {
        IErrorReg result;
        if (source == null) {
            return error("No se asignó la fuente del documento (source)");
        }
        if (!(source instanceof IPrintableSource)) {
            return error("La fuente " + source.getClass().getSimpleName()
                    + " no soporta impresión directa (IPrintableSource)");
        }
        try {
            ((IPrintableSource) source).printDirect();
            result = new ErrorReg();
        } catch (Exception ex) {
            //Fallo manejado: vuelve como IErrorReg; en el log técnico va como WARN.
            LOGGER.warn("Falló la impresión directa", ex);
            result = error(ex.getMessage());
            result.setException(ex);
        }
        writeLog(CHANNEL_PRINTER, result);
        return result;
    }

    /**
     * Devuelve el documento generado por el último {@link #execute()}, por si
     * el llamador quiere reutilizarlo (por ejemplo, ofrecer la descarga después
     * de un envío por correo).
     *
     * @return documento generado, o nulo si aún no se generó o falló.
     */
    public IOutputDocument getDocument() {
        return document;
    }

    /**
     * Arma un resultado de error con el mensaje indicado.
     *
     * @param message mensaje del error.
     * @return registro de error.
     */
    private IErrorReg error(String message) {
        IErrorReg errorReg = new ErrorReg();
        errorReg.setErrorNumber(1);
        errorReg.setMessage(Strings.isNullorEmpty(message) ? "Error no especificado" : message);
        return errorReg;
    }

    /**
     * Registra la entrega en el log de la aplicación, si el log fue activado
     * con {@link #log(ILogManager, Class)}. Un fallo del propio log no
     * interrumpe la salida: se registra en el log técnico y se sigue.
     *
     * @param channel canal de la entrega (o "generate" si falló la generación).
     * @param result resultado de la entrega.
     */
    private void writeLog(String channel, IErrorReg result) {
        if (logMngr == null || logType == null || userSession == null) {
            return;
        }
        try {
            boolean success = result.getErrorNumber() == null || result.getErrorNumber() == 0;
            String docName = document != null ? document.getFileName() : Fn.nvl(fileName, "");
            IAppLogRecord record = logMngr.getNewAppLogRecord(logType);
            record.setEvent(IAppLogRecord.EVENT_DOCUMENT_OUTPUT);
            record.setLevel(success ? IAppLogRecord.LEVEL_INFO : IAppLogRecord.LEVEL_ERROR);
            record.setCategory(IAppLogRecord.CATEGORY_APP);
            record.setMessage("Documento " + docName + " -> " + channel + ": "
                    + (success ? "OK" : Fn.nvl(result.getMessage(), "error")));
            record.setMessageInfo(document == null ? ""
                    : "formato: " + document.getFormat() + ", " + document.getSize() + " bytes");
            record.setMessageNumber(0);
            record.setAppObject(source != null ? source.getClass().getSimpleName() : "");
            logMngr.dbWrite(record, userSession.getSessionId());
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
    }
}
