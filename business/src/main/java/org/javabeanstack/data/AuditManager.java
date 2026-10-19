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
package org.javabeanstack.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import jakarta.ejb.EJB;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.log.ILogManager;
import org.javabeanstack.security.ISessions;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.util.Fn;
import org.javabeanstack.util.Strings;

/**
 * Auditoría de las grabaciones (plan AUDMGR): decide si una entidad se audita,
 * con caché, y escribe su fila de auditoría. Ver {@link IAuditManager}, que
 * explica las dos reglas de transacción que esta clase respeta.
 *
 * <p>Se declara en el {@code ejb-jar.xml} de cada módulo de la aplicación, como
 * {@code LogManager}: sin esa declaración el DAO no resuelve su inyección y el
 * módulo no despliega.</p>
 *
 * @author Jorge Enciso
 */
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class AuditManager implements IAuditManager {

    private static final Logger LOGGER = LogManager.getLogger(AuditManager.class);

    /**
     * Caché de {@link #isEntityAuditAble(String, String, String)}. La clave es
     * la terna de nombres; el valor, la respuesta del catálogo.
     *
     * <p>Es estática a propósito: este bean es {@code Stateless} y el
     * contenedor lo sirve desde un pool, así que un campo de instancia daría
     * una caché por instancia y la consulta se repetiría sin necesidad.
     * Estática equivale a <b>una por despliegue</b>.</p>
     *
     * <p>Es un mapa concurrente y no una caché con vencimiento porque la tabla
     * de auditoría se toca cuando se reconfigura el sistema, no durante la
     * operación. Para verla actualizada sin reiniciar está
     * {@link #clearAuditCache()}.</p>
     */
    private static final ConcurrentMap<String, Boolean> AUDIT_CACHE = new ConcurrentHashMap<>();

    /** Consulta a {@code AppAudit} (sesión nula: el catálogo). */
    @EJB
    private IGenericDAO dao;

    /** Sesión del usuario, para la cabecera de la fila de auditoría. */
    @EJB
    private ISessions sessions;

    /** Registro de errores, como lo hacía el DAO. */
    @EJB
    private ILogManager logMngr;

    /**
     * Indica si una entidad se audita, resolviendo sus tres nombres (clase,
     * tabla y {@code getAuditEntity()}).
     *
     * <p>{@code NOT_SUPPORTED} (de la clase): entrando por el proxy, todo el
     * método corre sin transacción, también la consulta que hace por
     * {@code this}.</p>
     *
     * @param <T> tipo de la entidad.
     * @param ejb entidad.
     * @return verdadero si se audita.
     */
    @Override
    public <T extends IDataRow> boolean isAuditAble(T ejb) {
        if (ejb == null || ejb.getAuditClass() == null) {
            return false;
        }
        if (ejb.isAuditAble()) {
            return true;
        }
        //Por nombre de la clase
        String entidad1 = ejb.getClass().getSimpleName().toLowerCase();
        //Por el nombre de la anotación Table en la clase. Una entidad sin
        //@Table es legal para JPA (la tabla toma el nombre de la clase), así
        //que no puede hacer fallar toda grabación con un NullPointerException.
        Table tableAnnotation = ejb.getClass().getAnnotation(Table.class);
        String entidad2 = (tableAnnotation != null)
                ? tableAnnotation.name().toLowerCase() : entidad1;
        //Por el valor en la anotación AuditEntity o un valor personalizado
        //determinado en la clase
        String auditEntity = ejb.getAuditEntity();
        String entidad3 = "*";
        if (auditEntity != null) {
            entidad3 = auditEntity;
        }
        return isEntityAuditAble(entidad1, entidad2, entidad3);
    }

    /**
     * Indica si una entidad se audita, a partir de su clase.
     *
     * @param <T> tipo de la entidad.
     * @param clazz clase de la entidad.
     * @return verdadero si se audita; falso también si la clase no se puede
     * instanciar.
     */
    @Override
    public <T extends IDataRow> boolean isAuditAble(Class<IDataRow> clazz) {
        if (clazz == null) {
            return false;
        }
        IDataRow ejb;
        try {
            ejb = clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            //Lo único que se tolera es no poder construir la instancia: sin
            //ella no hay nada que preguntar. El fallo del catálogo se propaga.
            ErrorManager.showError(e, LOGGER, logMngr, null);
            return false;
        }
        return isAuditAble(ejb);
    }

    /**
     * Indica si una entidad se audita, por un único nombre.
     *
     * @param <T> tipo de la entidad.
     * @param entityName nombre de la entidad en {@code AppAudit}.
     * @return verdadero si se audita.
     */
    @Override
    public <T extends IDataRow> boolean isAuditAble(String entityName) {
        //Los tres nombres iguales equivalen al "entity = :entity1" anterior.
        return isEntityAuditAble(entityName, entityName, entityName);
    }

    /**
     * Indica si alguno de los tres nombres está marcado para auditar en
     * {@code AppAudit}, con caché por terna.
     *
     * @param entity1 nombre de la clase de la entidad, en minúsculas.
     * @param entity2 nombre de la tabla de la entidad, en minúsculas.
     * @param entity3 valor de {@code getAuditEntity()}, o {@code "*"}.
     * @return verdadero si alguno está marcado.
     * @throws IllegalStateException si la consulta al catálogo falla: no se
     * devuelve falso ante un error.
     */
    @Override
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
            //Deliberadamente NO se devuelve falso: con el catálogo caído eso
            //dejaba de auditar sin que nadie se entere. Que falle y se vea.
            //El error no se registra acá: al cruzar el límite de este bean el
            //contenedor ya lo deja en el log (en WildFly, WFLYEJB0034) y después
            //lo vuelve a dejar el save() del DAO que lo atrapa. Registrarlo acá
            //sería una tercera copia de lo mismo.
            throw new IllegalStateException(
                    "No se pudo determinar si la entidad se audita (" + clave + ")", e);
        }
        boolean auditable = result != null && !result.isEmpty();
        AUDIT_CACHE.put(clave, auditable);
        return auditable;
    }

    /**
     * Vacía la caché de {@link #isEntityAuditAble(String, String, String)}
     * de este despliegue.
     */
    @Override
    public void clearAuditCache() {
        AUDIT_CACHE.clear();
    }

    /**
     * Escribe la fila de auditoría del cambio hecho sobre la entidad, con el
     * {@code EntityManager} y en la transacción de la grabación. No decide si
     * la entidad se audita.
     *
     * <p>{@code MANDATORY}: se suma a la transacción de la grabación y falla
     * en voz alta si se la llama sin una. Un fallo acá (por ejemplo, del
     * {@code persist}) cruza el límite del bean: el contenedor lo envuelve en
     * {@code EJBException} y marca la transacción para rollback, igual que
     * antes hacía el {@code save()} del DAO al atraparlo.</p>
     *
     * @param <T> tipo de la entidad.
     * @param em entity manager de la grabación.
     * @param sessionId identificador de la sesión del usuario.
     * @param ejb entidad grabada, con su acción todavía puesta.
     * @throws Exception si falla el registro de auditoría.
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public <T extends IDataRow> void write(EntityManager em, String sessionId, T ejb) throws Exception {
        IDataRow auditEjb = (IDataRow) ejb.getAuditClass().getConstructor().newInstance();
        String operacion;
        switch (ejb.getAction()) {
            case IDataRow.INSERT:
                operacion = "I";
                break;
            case IDataRow.UPDATE:
                operacion = "U";
                break;
            case IDataRow.DELETE:
                operacion = "D";
                break;
            case IDataRow.READ:
                operacion = "R";
                break;
            default:
                operacion = "?";
                break;
        }
        String ip = "", deviceName = "", appUser = "";
        IUserSession session = sessions.getUserSession(sessionId);
        if (session != null) {
            IDBLinkInfo dbLinkInfo = sessions.getDBLinkInfo(sessionId);
            ip = Fn.nvl(session.getIp(), "");
            //El nombre del dispositivo solo existe en una sesión por token:
            //es el uuidDevice que declaró el cliente al pedirlo.
            if (session.getClientAuthRequestInfo() != null) {
                deviceName = Fn.nvl(dbLinkInfo.getUuidDevice(), "");
            }
            appUser = dbLinkInfo.getAppUserSeal();
        }
        auditEjb.setValue("operacion", operacion);
        //Los dos campos se escriben solo si la clase de auditoría los tiene,
        //para que el framework y el modelo de la aplicación se puedan
        //desplegar en cualquier orden (ver AuditHeader).
        if (DataInfo.isFieldExist(auditEjb.getClass(), "iprequest")) {
            auditEjb.setValue("iprequest", Strings.isNullorEmpty(ip) ? null : ip);
        }
        if (DataInfo.isFieldExist(auditEjb.getClass(), "device")) {
            auditEjb.setValue("device", AuditHeader.formatDevice(deviceName, ip));
        }
        auditEjb.setValue("sessionid", sessionId);
        auditEjb = ejb.copyTo(auditEjb);
        if (DataInfo.isFieldExist(auditEjb.getClass(), "appuser")) {
            auditEjb.setValue("appuser", appUser);
        }
        em.persist(auditEjb);
        //DEBUG y no INFO (D6): vuelca el registro completo de cada fila.
        LOGGER.debug(auditEjb);
    }
}
