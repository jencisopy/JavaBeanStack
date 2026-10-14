/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
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
package org.javabeanstack.log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import jakarta.ejb.EJB;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import org.apache.logging.log4j.Logger;
import org.javabeanstack.data.IDataResult;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.security.ISessions;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.util.Strings;
import org.javabeanstack.model.IAppMessage;
import org.javabeanstack.data.IGenericDAO;
import org.javabeanstack.model.IAppLogRecord;
import org.javabeanstack.util.Fn;
import org.javabeanstack.util.LocalDates;

/**
 * Su función es gestionar la escritura y lectura del log del sistema.
 *
 * <p>También resuelve, con caché, si una entidad debe auditarse
 * ({@link #isEntityAuditAble(String, String, String)}). Vive acá y no en la
 * configuración por una razón de arranque, explicada en el javadoc de ese
 * método: este bean es {@code Stateless} y no se lo puede reentrar mientras se
 * construye.</p>
 *
 * @author Jorge Enciso
 */
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class LogManager implements ILogManager {

    private static final Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger(LogManager.class);

    private final String LOG_TYPE_PATH = "org.javabeanstack.model.appcatalog.AppLogEvent";

    @EJB
    private IGenericDAO dao;

    @EJB
    private ISessions sessions;

    /**
     * Devuelve el nombre completo de la clase del tipo de registro de log por
     * defecto.
     *
     * @return path de la clase del tipo de log por defecto.
     */
    protected String getLogTypePath(){
        return LOG_TYPE_PATH;
    }

    /**
     * Crea una nueva instancia de registro de log del tipo indicado.
     *
     * @param <T> tipo del registro de log.
     * @param logType clase del registro de log.
     * @return nueva instancia de registro de log.
     */
    @Override
    public <T extends IAppLogRecord> IAppLogRecord getNewAppLogRecord(Class<T> logType) {
        try {
            if (logType == null) {
                Class logTypeDefault = Class.forName(getLogTypePath());
                return (IAppLogRecord) logTypeDefault.getConstructor().newInstance();
            }
            return logType.getConstructor().newInstance();
        } catch (Exception ex) {
            //continua con la busqueda
        }
        return null;
    }

    /**
     * Escribe información de un evento en una tabla de la base de datos.
     *
     * @param errorReg objeto error conteniendo la información del evento.
     * @return verdadero si tuvo exito y falso si no.
     */
    @Override
    public boolean dbWrite(IErrorReg errorReg) {
        return dbWrite(null, null, errorReg);
    }

    /**
     * Escribe información de un evento en una tabla de la base de datos.
     *
     * @param <T>
     * @param logType
     * @param error objeto error conteniendo la información del evento.
     * @param sessionId identificador de la sesión del usuario
     * @return verdadero si tuvo exito y falso si no.
     */
    @Override
    public <T extends IAppLogRecord> boolean dbWrite(Class<T> logType, String sessionId, IErrorReg error) {
        try {
            IAppLogRecord logRecord = getNewAppLogRecord(logType);
            String messageInfo = "";
            if (error.getException() != null) {
                messageInfo = ErrorManager.getStackCause(error.getException());
            }
            logRecord.setEvent(error.getEvent());
            logRecord.setLevel(error.getLevel());
            logRecord.setMessage(error.getMessage());
            logRecord.setMessageInfo(messageInfo);
            logRecord.setMessageNumber(error.getErrorNumber());
            logRecord.setAppObject(error.getEntity());
            logRecord.setIpRequestFrom(error.getIpRequest());
            return dbWrite(logRecord, sessionId);
        } catch (Exception exp) {
            ErrorManager.showError(exp, LOGGER);
        }
        return false;
    }

    /**
     * Escribe información de un evento en una tabla de la base de datos.
     *
     * @param <T>
     * @param logRecord registro del evento.
     * @param sessionId identificador de la sesión del usuario
     * @return verdadero si tuvo exito y falso si no.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Override
    public <T extends IAppLogRecord> boolean dbWrite(T logRecord, String sessionId) {
        String sesionId = "NINGUNA";
        Long idcompany = null;
        Long iduser = null;
        String ipRequestFrom = "";
        if (!Strings.isNullorEmpty(sessionId)) {
            IUserSession userSession = sessions.getUserSession(sessionId);
            if (userSession != null && !Strings.isNullorEmpty(userSession.getIp())) {
                ipRequestFrom = userSession.getIp();
            }
            if (userSession != null && userSession.getUser() != null) {
                idcompany = userSession.getIdCompany();
                iduser = userSession.getUser().getIduser();
                sesionId = Strings.localDateToString(userSession.getTimeLogin())
                        + userSession.getUser().getLogin().trim();
            }
            logRecord.setIdcompany(idcompany);
            logRecord.setIduser(iduser);
            logRecord.setSessionId(sesionId);
            logRecord.setIpRequestFrom(ipRequestFrom);
        }
        if (logRecord.getLogTimeOrigin() == null) {
            logRecord.setLogTimeOrigin(LocalDates.now());
        }
        logRecord.setMessage(Fn.nvl(logRecord.getMessage(),""));
        logRecord.setMessageInfo(Fn.nvl(logRecord.getMessageInfo(),""));
        logRecord.setMessageNumber(Fn.nvl(logRecord.getMessageNumber(),0));
        IDataResult dataResult;
        try {
            dataResult = dao.persist(null, logRecord);
            return dataResult.isSuccessFul();
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return false;
    }

    /**
     * Busca en una tabla de mensajes en la base de datos "AppMessage" el nro.
     * de mensaje y devuelve el registro
     *
     * @param msgNumber nro. de mensaje
     * @return Mensaje solicitado
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public IAppMessage getAppMessage(Integer msgNumber) {
        try {
            IAppMessage message
                    = dao.findByQuery(null,
                            "select o from AppMessage o where number = " + msgNumber.toString(),
                            null);

            return message;
        } catch (Exception exp) {
            ErrorManager.showError(exp, LOGGER);
        }
        return null;
    }

    /**
     * Busca en una tabla de mensajes en la base de datos "AppMessage" el nro.
     * de mensaje y devuelve el registro
     *
     * @return Mensaje solicitado
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<IAppMessage> getAppMessages() {
        try {
            List<IAppMessage> messages
                    = dao.findListByQuery(null,
                            "select o from AppMessage o order by number",
                            null);

            return messages;
        } catch (Exception exp) {
            ErrorManager.showError(exp, LOGGER);
        }
        return new ArrayList();
    }

    /**
     * Cache de {@link #isEntityAuditAble(String, String, String)}. La clave es
     * la terna de nombres; el valor, la respuesta del catalogo.
     *
     * <p>Es estatico a proposito: este bean es {@code Stateless} y el
     * contenedor lo sirve desde un pool, asi que un campo de instancia daria
     * un cache por instancia agrupada y la consulta se repetiria sin
     * necesidad. Estatico equivale a <b>uno por despliegue</b>, que es el mismo
     * alcance que tenia cuando vivia en el singleton de configuracion.</p>
     *
     * <p>Es un mapa concurrente y no un cache con vencimiento porque la tabla
     * de auditoria se toca cuando se reconfigura el sistema, no durante la
     * operacion. Para verlo actualizado sin reiniciar esta
     * {@link #clearAuditCache()}.</p>
     */
    private static final ConcurrentMap<String, Boolean> AUDIT_CACHE = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean isEntityAuditAble(String entity1, String entity2, String entity3) {
        String clave = entity1 + "\u0001" + entity2 + "\u0001" + entity3;
        Boolean cacheado = AUDIT_CACHE.get(clave);
        if (cacheado != null) {
            return cacheado;
        }
        List<IDataRow> result;
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("entity1", entity1);
            params.put("entity2", entity2);
            params.put("entity3", entity3);
            result = dao.findListByQuery(null, "select o from AppAudit o "
                    + "where entity in (:entity1, :entity2, :entity3) "
                    + "and audit = true", params);
        } catch (Exception e) {
            //Deliberadamente NO se devuelve falso: con el catalogo caido eso
            //dejaba de auditar sin que nadie se entere. Que falle y se vea.
            //El error no se registra aca, pero no porque asi salga una sola
            //vez: al cruzar el limite de este bean el contenedor ya la deja en
            //el log (en WildFly, WFLYEJB0034) y despues la vuelve a dejar el
            //update() del DAO que la atrapa, asi que la traza sale dos veces
            //igual. Registrarla aca seria una tercera copia de lo mismo.
            throw new IllegalStateException(
                    "No se pudo determinar si la entidad se audita (" + clave + ")", e);
        }
        boolean auditable = result != null && !result.isEmpty();
        AUDIT_CACHE.put(clave, auditable);
        return auditable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void clearAuditCache() {
        AUDIT_CACHE.clear();
    }
}
