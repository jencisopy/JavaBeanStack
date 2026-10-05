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
package org.javabeanstack.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import jakarta.ejb.EJB;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javabeanstack.data.IDataResult;
import org.javabeanstack.data.IDataRow;
import org.w3c.dom.Document;
import org.javabeanstack.data.IGenericDAO;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.error.ErrorReg;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.io.IOUtil;
import org.javabeanstack.log.ILogManagerData;
import org.javabeanstack.model.IAppSystemParam;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.util.Fn;
import static org.javabeanstack.util.Strings.leftPad;
import org.javabeanstack.xml.DomW3cParser;

/**
 * Esta clase implementa funcionalidades que permite a la app leer y grabar
 * configuraciones que determinará el comportamiento del sistema.
 *
 * Se implementan dos tipos de almacenes de datos de configuración
 *
 * El 1ro es un {@code Map<Clave,Valor>} el cual se guarda objetos DOM conteniendo
 * información en formato xml.
 *
 * El 2do. es un objeto "AppSystemParam" mapeado a una tabla de la base de datos
 * conteniendo información de configuración del sistema.
 *
 *
 * @author Jorge Enciso
 */
@Lock(LockType.READ)
public class AppGenericConfig implements IAppConfig {

    protected static final Logger LOGGER = LogManager.getLogger(AppGenericConfig.class);

    @EJB
    protected IGenericDAO dao;
    
    @EJB
    protected ILogManagerData logMngr;

    /**
     * En este atributo se guardan objetos DOM que son accedidos por una clave
     * "groupkey". Estos objetos DOM contienen información sobre parámetros del
     * sistema, configuración de conexión a los datos, parámetros de seguridad
     * etc.
     */
    protected final Map<String, Document> config = new TreeMap(String.CASE_INSENSITIVE_ORDER);

    /**
     * Lee el objeto DOM de configuración guardado bajo una clave "groupkey"
     *
     * <p>El {@code Document} devuelto es compartido y no es seguro para hilos,
     * ni siquiera en lectura: quien lo recorra debe hacerlo dentro de
     * {@code synchronized (dom)}, el mismo monitor que usan
     * {@link #getProperty(String, String, String)} y
     * {@link #setProperty(String, String, String, String)}.</p>
     *
     * @param groupKey identificador del registro.
     * @return objeto DOM.
     */
    @Override
    public Document getConfigDOM(String groupKey) {
        groupKey = groupKey.toUpperCase();
        return config.get(groupKey);
    }

    /**
     * Lee el valor de una propiedad que se encuentra bajo una clave "groupkey"
     * y un path dentro del objeto DOM.
     *
     * @param property propiedad del objeto DOM xml
     * @param groupKey clave bajo la cual se encuentra el objeto DOM.
     * @param nodePath path del elemento donde se encuentra el property.
     * @return valor de la propiedad solicitada.
     */
    @Override
    public String getProperty(String property, String groupKey, String nodePath) {
        groupKey = groupKey.toUpperCase();
        Document dom = config.get(groupKey);
        if (dom == null) {
            return null;
        }
        String propValue;
        // El Document de Xerces no es seguro para hilos ni siquiera en lectura
        // (expansión diferida de nodos) y este bean es @Lock(READ): sin este
        // candado, varias sesiones leyendo a la vez recibían null o "" y hasta
        // dejaban el DOM corrupto. Se sincroniza sobre el propio Document para
        // que las subclases que lo lean directamente usen el mismo monitor.
        synchronized (dom) {
            try {
                propValue = DomW3cParser.getPropertyValue(dom, property, nodePath);
            } catch (Exception ex) {
                // Se devuelve null como siempre, pero ya no en silencio: sin
                // este aviso la falla se veía lejos de acá, como un null en
                // quien leyó la propiedad. Una línea por falla; la pila solo
                // en DEBUG para no inundar el log.
                LOGGER.warn("No se pudo leer la propiedad " + property + " de "
                        + groupKey + " (" + nodePath + "): " + ex);
                LOGGER.debug("Detalle del error al leer la propiedad " + property, ex);
                propValue = null;
            }
        }
        return propValue;
    }

    /**
     * Asigna un valor a una propiedad del objeto DOM de configuración que se
     * encuentra bajo una clave "groupkey" y un path al nodo donde se encuentra
     * la propiedad.
     *
     * @param value valor a asignar.
     * @param property nombre de la propiedad.
     * @param groupKey clave con la cual se identifica al DOM xml.
     * @param nodePath path dentro del objeto DOM donde se encuentra la
     * propiedad.
     * @return verdadero si tuvo exito y falso si no.
     */
    @Override
    @Lock(LockType.WRITE)
    public boolean setProperty(String value, String property, String groupKey, String nodePath) {
        groupKey = groupKey.toUpperCase();
        Document dom = config.get(groupKey);
        if (dom == null) {
            return false;
        }
        boolean result;
        // Mismo candado que getProperty: @Lock(WRITE) solo excluye a las
        // llamadas que pasan por el proxy del contenedor, no a los accesos
        // internos del propio bean.
        synchronized (dom) {
            try {
                result = DomW3cParser.setPropertyValue(dom, value, property, nodePath);
            } catch (Exception ex) {
                // Mismo criterio que getProperty: se devuelve false, pero dejando rastro
                LOGGER.warn("No se pudo asignar la propiedad " + property + " de "
                        + groupKey + " (" + nodePath + "): " + ex);
                LOGGER.debug("Detalle del error al asignar la propiedad " + property, ex);
                result = false;
            }
        }
        return result;
    }

    /**
     * Lee de una tabla "appSystemParam" un registro mediante un identificador
     *
     * @param id identificador del registro.
     * @return el registro AppSystemParam solicitado
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)    
    public IAppSystemParam getSystemParam(Long id) {
        IAppSystemParam appSystemParam;
        String queryString
                = "select o from AppSystemParam o where idAppSystemParam = :id";
        try {
            appSystemParam
                    = dao.findByQuery(null, queryString, Fn.queryParams("id", id));
            return appSystemParam;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER, logMngr, null);
        }
        return null;
    }

    /**
     * Lee de una tabla "appSystemParam" el valor global (empresa nula) de un
     * parámetro, utilizando su nombre como identificador.
     *
     * <p>Filtra la empresa a propósito: con valores por empresa puede haber
     * varias filas del mismo nombre, y sin el filtro la consulta de un único
     * resultado fallaba y devolvía nulo, como si el parámetro no existiera.</p>
     *
     * @param param nombre del parametro.
     * @return registro AppSystemParam solicitado.
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)        
    public IAppSystemParam getSystemParam(String param) {
        return findSystemParam(param, null);
    }

    /**
     * Devuelve el valor de un parámetro que rige para una empresa: el propio de
     * la empresa si existe y el global lo admite; si no, el global.
     *
     * @param param nombre del parámetro.
     * @param idcompany empresa real ({@code appcompany.idcompany}), o nulo.
     * @return parámetro que rige, o nulo si no existe el global.
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public IAppSystemParam getSystemParam(String param, Long idcompany) {
        IAppSystemParam global = findSystemParam(param, null);
        // Un parámetro de alcance global ignora cualquier valor por empresa,
        // aunque alguien lo haya cargado por script salteando la validación.
        if (idcompany == null || global == null || !global.isCompanyAllowed()) {
            return global;
        }
        IAppSystemParam propio = findSystemParam(param, idcompany);
        return (propio != null) ? propio : global;
    }

    /**
     * Busca la fila exacta de un parámetro: la global si la empresa es nula o
     * la propia de la empresa en caso contrario. No aplica la cascada.
     *
     * @param param nombre del parámetro.
     * @param idcompany empresa, o nulo para la fila global.
     * @return la fila, o nulo si no existe.
     */
    protected IAppSystemParam findSystemParam(String param, Long idcompany) {
        if (param == null) {
            return null;
        }
        String queryString;
        Map<String, Object> params;
        if (idcompany == null) {
            queryString = "select o from AppSystemParam o where LOWER(param) = :param"
                    + " and idcompany is null";
            params = Fn.queryParams("param", param.toLowerCase());
        } else {
            queryString = "select o from AppSystemParam o where LOWER(param) = :param"
                    + " and idcompany = :idcompany";
            params = Fn.queryParams("param", param.toLowerCase(), "idcompany", idcompany);
        }
        try {
            return dao.findByQuery(null, queryString, params);
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER, logMngr, null);
        }
        return null;
    }

    /**
     * Devuelve una lista conteniendo los registros globales de "appSystemParam"
     * (sin los valores propios de las empresas).
     *
     * @return lista de registros "AppSystemParam"
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)        
    public List<IAppSystemParam> getSystemParams() {
        String queryString
                = "select o from AppSystemParam o where idcompany is null";
        try {
            return dao.findListByQuery(null, queryString, null);
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER, logMngr, null);
        }
        return new ArrayList();
    }

    /**
     * Devuelve los parámetros tal como rigen para una empresa: los globales,
     * reemplazados por el valor propio de la empresa en los que lo admiten.
     *
     * @param idcompany empresa real, o nulo para obtener solo los globales.
     * @return lista de parámetros vigentes para la empresa.
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<IAppSystemParam> getSystemParams(Long idcompany) {
        List<IAppSystemParam> globales = getSystemParams();
        if (idcompany == null) {
            return globales;
        }
        List<IAppSystemParam> propios;
        try {
            propios = dao.findListByQuery(null,
                    "select o from AppSystemParam o where idcompany = :idcompany",
                    Fn.queryParams("idcompany", idcompany));
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER, logMngr, null);
            return globales;
        }
        Map<String, IAppSystemParam> porNombre = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (IAppSystemParam propio : propios) {
            porNombre.put(propio.getParam(), propio);
        }
        List<IAppSystemParam> result = new ArrayList<>(globales.size());
        for (IAppSystemParam global : globales) {
            IAppSystemParam propio = porNombre.get(global.getParam());
            result.add(propio != null && global.isCompanyAllowed() ? propio : global);
        }
        return result;
    }

    /**
     * Devuelve la ruta del sistema de archivos configurada para la sesión.
     *
     * @param sessionId identificador de la sesión.
     * @return ruta del sistema de archivos.
     */
    @Override
    public String getFileSystemPath(String sessionId) {
        String path = "";
        String separador = "";
        if (!Fn.nvl(sessionId, "").isEmpty()) {
            IUserSession userSession = dao.getUserSession(sessionId);
            if (userSession == null){
                return "";
            }
            //Path resource por empresa
            if (getSystemParam("APPCOMPANY_RESOURCE_PATH") != null
                    && getSystemParam("APPCOMPANY_RESOURCE_PATH").getValueChar() != null) {
                path = IOUtil.addbs(getSystemParam("APPCOMPANY_RESOURCE_PATH").getValueChar().trim())
                        + leftPad(userSession.getCompany().getIdcompany().toString(), 2, "0");
                separador = ",";
            }
        }
        //Path resource sistema global
        if (getSystemParam("APPRESOURCEPATH") != null
                && getSystemParam("APPRESOURCEPATH").getValueChar() != null) {
            path += separador + getSystemParam("APPRESOURCEPATH").getValueChar().trim();
        }
        return Fn.nvl(path, "");
    }

    /**
     * Persiste un parámetro de sistema. La fila se identifica por el par
     * (nombre, empresa).
     *
     * <p><b>Autorización</b>: este método no verifica quién graba. Es API
     * interna de la configuración (siembra, procesos del sistema); el permiso
     * de administración de la empresa para crear o modificar valores por
     * empresa se resuelve en el servicio que exponga esa edición a los
     * usuarios (plan SYSPAR, D14).</p>
     *
     * @param param parámetro a guardar.
     * @return resultado de la operación.
     * @throws SystemParamScopeException si es un valor por empresa de un
     * parámetro que no lo admite.
     * @throws Exception si la persistencia falla.
     */
    @Override
    public IDataResult setSystemParam(IAppSystemParam param) throws Exception {
        checkScope(param);
        IDataResult result;
        if (param.getId() != null && param.getIdAppSystemParam() != 0L) {
            result = dao.merge(null, param);
        } else {
            //Verificar si existe la misma fila (nombre + empresa): sin la
            //empresa, un valor por empresa pisaría al global.
            IAppSystemParam paramMerge = findSystemParam(param.getParam(), param.getIdcompany());
            if (paramMerge != null) {
                //Si existe actualizar el registro
                param.setId(paramMerge.getId());
                result = dao.merge(null, param);
            } else {
                //Sino existe agregar en la tabla.
                //El id va en null y el modo es PERSIST: la clave de
                //appsystemparam es IDENTITY, así que la asigna la base. Con id
                //en 0 Hibernate considera la entidad "detached" y rechaza el
                //persist; con MERGE intenta actualizar una fila inexistente.
                param.setIdAppSystemParam(null);
                param.setPersistMode(IDataRow.PERSIST);
                result = dao.persist(null, param);
            }
        }
        if (!result.isSuccessFul()){
            LOGGER.error(result.getErrorMsg());
        }
        return result;
    }

    /**
     * Rechaza un valor por empresa sobre un parámetro que no lo admite. El
     * alcance lo define la fila global: si es solo global, o si el global no
     * existe, no puede haber fila con empresa.
     *
     * <p>El error (número 50000, campo {@code idcompany}) queda registrado en
     * el propio parámetro y viaja en la excepción, que es de aplicación: no
     * marca la transacción ni se registra como falla del sistema.</p>
     *
     * @param param parámetro a grabar.
     * @throws SystemParamScopeException si el parámetro no admite valor por empresa.
     */
    protected void checkScope(IAppSystemParam param) throws SystemParamScopeException {
        if (param.getIdcompany() == null) {
            return;
        }
        IAppSystemParam global = findSystemParam(param.getParam(), null);
        if (global == null || !global.isCompanyAllowed()) {
            String msg = "El parámetro " + param.getParam()
                    + " no admite un valor por empresa (alcance global)";
            IErrorReg error = new ErrorReg();
            error.setErrorNumber(50000);
            error.setFieldName("idcompany");
            error.setMessage(msg);
            param.setErrors(error, "idcompany");
            throw new SystemParamScopeException(error);
        }
    }

    /**
     * Restablece un parámetro: con empresa nula vuelve el global a su valor
     * de fábrica; con empresa, borra el valor propio para que rija el global.
     *
     * @param param nombre del parámetro.
     * @param idcompany empresa, o nulo para el global.
     * @return resultado de la operación, o nulo si no había nada que restablecer.
     * @throws Exception si la persistencia falla.
     */
    @Override
    public IDataResult restoreSystemParam(String param, Long idcompany) throws Exception {
        if (idcompany != null) {
            return deleteSystemParam(param, idcompany);
        }
        IAppSystemParam global = findSystemParam(param, null);
        if (global == null) {
            return null;
        }
        global.restoreDefault();
        return setSystemParam(global);
    }

    /**
     * Borra el valor propio de una empresa para un parámetro. Nunca borra el
     * global.
     *
     * @param param nombre del parámetro.
     * @param idcompany empresa (obligatoria).
     * @return resultado de la operación, o nulo si la empresa no tenía valor propio.
     * @throws Exception si la persistencia falla.
     */
    @Override
    public IDataResult deleteSystemParam(String param, Long idcompany) throws Exception {
        if (idcompany == null) {
            throw new IllegalArgumentException("deleteSystemParam no borra el valor global de " + param);
        }
        IAppSystemParam propio = findSystemParam(param, idcompany);
        if (propio == null) {
            return null;
        }
        IDataResult result = dao.remove(null, propio);
        if (!result.isSuccessFul()) {
            LOGGER.error(result.getErrorMsg());
        }
        return result;
    }

    /**
     * Persiste una lista de parámetros de sistema.
     *
     * @param params lista de parámetros a guardar.
     * @throws Exception si la persistencia falla.
     */
    @Override
    public void setSystemParams(List<IAppSystemParam> params) throws Exception {
        //Implementar en clases derivadas
        for (IAppSystemParam param : params) {
            setSystemParam(param);
        }
    }

    /**
     * Verifica que el esquema de la base de datos sea compatible con la aplicación.
     *
     * @param sessionId identificador de la sesión.
     * @return registro de error si no es compatible, o {@code null} si lo es.
     * @throws Exception si ocurre un error durante la verificación.
     */
    @Override
    public IErrorReg checkDatabase(String sessionId) throws Exception {
        return null;
    }

    /**
     * Actualiza el esquema de la base de datos a la versión de la aplicación.
     *
     * @param sessionId identificador de la sesión.
     * @return registro de error si la actualización falla, o {@code null} si tuvo éxito.
     * @throws Exception si ocurre un error durante la actualización.
     */
    @Override
    public IErrorReg updateDatabase(String sessionId) throws Exception {
        return null;
    }
    
    /**
     * Devuelve la versión de estructura de base de datos que espera esta
     * aplicación, con el formato {@code <version>.<secuencia>}.
     *
     * @return {@code "999999"}, valor con el que la verificación de versión
     * queda desactivada. Las aplicaciones que versionan su estructura
     * sobreescriben este método.
     */
    @Override
    public String getDBVersionForThisApp(){
        return DBVERSION_UNDEFINED;
    }
            
    /**
     * Devuelve la versión de estructura actualmente instalada en la base, con
     * el mismo formato que {@link #getDBVersionForThisApp()}.
     *
     * @param sessionId identificador de la sesión.
     * @return nulo en la implementación genérica.
     */
    @Override
    public String getDBVersion(String sessionId){
        return null;
    }
}
