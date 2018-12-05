/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 Jorge Enciso
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
 
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.javabeanstack.exceptions.SessionError;
import org.javabeanstack.model.IAppTablesRelation;
import org.javabeanstack.security.IUserSession;
import org.javabeanstack.data.services.IDataService;
import org.javabeanstack.util.Dates;
import org.javabeanstack.util.Strings;

/**
 * Es un wrapper de AbstractDao, gestiona el acceso a los datos.
 * 
 * @author Jorge Enciso
 */
public abstract class AbstractDataLink implements IDataLink, Serializable {
    /**
     * Unidad de persistencia donde esta configurado los parámetros de la
     * conexión a la base de datos
     */
    private String persistUnit = IDBManager.CATALOGO;
    /**
     * Objeto resultante del login del usuario. Para acceder al esquema de datos
     * de la base el usuario debe estar logeado
     */
    private IUserSession userSession;

    /**
     * Es el objeto responsable del acceso a los datos
     *
     * @return dao
     */
    @Override
    public abstract IGenericDAO getDao();

    @Override
    public abstract <T extends IGenericDAO> void setDao(T dao);

    /**
     * Es el objeto responsable del acceso a los datos y logica del negocio
     *
     * @param <T>
     * @return la instancia del dataService
     */
    @Override
    public abstract <T extends IDataService> T getDataService();


    /**
     * Unidad de persistencia donde esta configurado los parámetros de la
     * conexión a la base de datos
     *
     * @return unidad de persistencia
     */
    @Override
    public String getPersistUnit() {
        return persistUnit;
    }

    /**
     * Agrega un registro en la tabla
     *
     * @param <T> tipo de dato generalmente hereda de DataRow
     * @param ejb el objeto con los valores del registro
     * @throws org.javabeanstack.exceptions.SessionError
     * @return dataResult (resultado del persist)
     */
    @Override
    public <T extends IDataRow> IDataResult persist(T ejb) throws SessionError{
        ejb.setAction(IDataRow.INSERT);
        return update(ejb);
    }    

    /**
     * Agregar registros a la tabla
     *
     * @param <T> tipo de dato generalmente hereda de DataRow
     * @param ejbs lista con los objetos con los valores de los registros
     * @return dataResult (resultado del persist)
     * @throws org.javabeanstack.exceptions.SessionError
     */
    @Override
    public <T extends IDataRow> IDataResult persist(List<T> ejbs) throws SessionError{
        ejbs.forEach( ejb -> {
            ejb.setAction(IDataRow.INSERT);
        });
        return update(ejbs);
    }

    /**
     * Actualizar un registro en la tabla
     *
     * @param <T> tipo de dato generalemente hereda de DataRow
     * @param ejb el objeto con los valores del registro
     * @throws org.javabeanstack.exceptions.SessionError
     * @return dataResult (resultado del merge)
     */
    @Override
    public <T extends IDataRow> IDataResult merge(T ejb) throws SessionError{
        ejb.setAction(IDataRow.UPDATE);
        return update(ejb);
    }

    /**
     * Actualizar registros en la tabla
     *
     * @param <T> tipo de dato generalmente hereda de DataRow
     * @param ejbs lista con los objetos con los valores de los registros
     * @return dataResult (resultado del merge)
     * @throws org.javabeanstack.exceptions.SessionError
     */
    @Override
    public <T extends IDataRow> IDataResult merge(List<T> ejbs) throws SessionError{
        ejbs.forEach((ejb) -> {
            ejb.setAction(IDataRow.UPDATE);
        });
        return update(ejbs);
    }

    /**
     * Borrar un registro de la tabla
     *
     * @param <T> tipo de dato generalemente hereda de DataRow
     * @param ejb el objeto con los valores del registro
     * @throws org.javabeanstack.exceptions.SessionError
     * @return dataResult (resultado del remove)
     */
    @Override
    public <T extends IDataRow> IDataResult remove(T ejb) throws SessionError{
        ejb.setAction(IDataRow.DELETE);
        return update(ejb);
    }

    /**
     * Borrar registros de la tabla
     *
     * @param <T> tipo de dato generalmente hereda de DataRow
     * @param ejbs lista con los objetos con los valores de los registros
     * @return dataResult (resultado del remove)
     * @throws org.javabeanstack.exceptions.SessionError
     */
    @Override
    public <T extends IDataRow> IDataResult remove(List<T> ejbs) throws SessionError{
        ejbs.forEach((ejb) -> {
            ejb.setAction(IDataRow.DELETE);
        });
        return update(ejbs);
    }
    
    /**
     * Agrega, actualiza o borra registros de la base de datos
     *
     * @param <T> tipo de dato generalemente hereda de DataRow
     * @param ejb el objeto con los valores del registro
     * @throws org.javabeanstack.exceptions.SessionError
     * @return dataResult (resultado del update)
     */
    @Override
    public <T extends IDataRow> IDataResult update(T ejb) throws SessionError {
        // Verificar si la sesión es válida
        checkUserSession();
        IDataResult dataResult = getDao().update(getSessionId(), ejb);
        if (dataResult.isSuccessFul() && dataResult.isRemoveDeleted()){
            dataResult.setRowsUpdated(ejb);
        }
        return dataResult;
    }

    /**
     * Agregar, actualiza o borra registros de la base de datos
     *
     * @param <T> tipo de dato generalmente hereda de DataRow
     * @param ejbs lista con los objetos con los valores de los registros
     * @return dataResult (resultado del update)
     * @throws org.javabeanstack.exceptions.SessionError
     */
    @Override
    public <T extends IDataRow> IDataResult update(List<T> ejbs) throws SessionError {
        // Verificar si la sesión es válida
        checkUserSession();
        //Procesar solo los registros modificados
        List<T> ejbs2 = this.getRowsChanged(ejbs);
        IDataResult dataResult = getDao().update(getSessionId(), ejbs2);
        if (dataResult.isSuccessFul()){
            dataResult.setRowsUpdated(ejbs);            
            // Eliminar registros borrados de la lista            
            removeDeleted(ejbs);
            dataResult.setRemoveDeleted(Boolean.TRUE);            
        }
        return dataResult;
    }

    /**
     * Agregar, actualiza o borra registros de la base de datos
     *
     * @param dataSet cada elemento del dataSet contiene una lista con los objetos 
     * mapeados a los registros de cada tabla
     * @return dataResult (resultado del update)
     * @throws org.javabeanstack.exceptions.SessionError
     */
    @Override
    public IDataResult update(IDataSet dataSet) throws SessionError {
        // Verificar si la sesión es válida
        checkUserSession();
        // Procesar solo registros modificados.
        IDataSet dataSetChanged = dataSet.getChanged();
        IDataResult dataResult = getDao().update(getSessionId(), dataSetChanged);
        if (dataResult.isSuccessFul()){
            dataResult.setRowsUpdated(dataSet);            
            // Eliminar registros borrados de la lista                    
            for (Map.Entry<String, List<? extends IDataRow>> entry: dataSet.getMapListSet().entrySet()){
                List<? extends IDataRow> ejbs = entry.getValue();
                removeDeleted(ejbs);                
            }
            if (dataSet.getMapDataObject() != null && !dataSet.getMapDataObject().isEmpty()){
                dataResult.setRemoveDeleted(Boolean.TRUE);                
            }
        }
        return dataResult;
    }

    /**
     * Devuelve un registro de datos.
     *
     * @param <T> tipo de dato generalmente hereda de DataRow
     * @param entityClass tipo de dato o clase solicitada
     * @param id identificador del registro (clave primaria).
     * @return Objeto con los datos del registro.
     * @throws Exception
     * @throws org.javabeanstack.exceptions.SessionError
     */
    @Override 
    public <T extends IDataRow> T find(Class<T> entityClass, Object id) throws Exception, SessionError {
        // Verificar si la sesión es válida
        checkUserSession();
        return getDao().findById(entityClass, getSessionId(), id);
    }

    /**
     * Devuelve un registro de datos.
     *
     * @param <T> tipo de dato generalmente hereda de DataRow
     * @param ejb objeto conteniendo datos necesarios para llegar a recuperar el
     * registro a travéz de los campos de la clave unica
     * @return Objeto con los datos del registro
     * @throws Exception
     * @throws org.javabeanstack.exceptions.SessionError
     */
    @Override
    public <T extends IDataRow> T findByUk(T ejb) throws Exception, SessionError {
        // Verificar si la sesión es válida
        checkUserSession();
        return getDao().findByUk(getSessionId(), ejb);
    }

    /**
     * Devuelve un registro
     * 
     * @param <T> tipo de dato generalmente hereda de DataRow
     * @param queryString sentencia JPQL que se ejecutará para recuperar los
     * datos
     * @param parameters parametros de la sentencia.
     * @return Devuelve un registro
     * @throws Exception
     * @throws org.javabeanstack.exceptions.SessionError
     */
    @Override
    public <T extends IDataRow> T findByQuery(String queryString,
            Map<String, Object> parameters)
            throws Exception, SessionError {
        // Verificar si la sesión es válida
        checkUserSession();
        parameters = addParams(queryString, parameters);
        return getDao().findByQuery(getSessionId(), queryString, parameters);
    }

    /**
     * Devuelve una lista de registros resultado de la selección de datos en una
     * base de datos
     *
     * @param <T> tipo de dato generalmente hereda de DataRow
     * @param queryString sentencia JPQL que se ejecutará para recuperar los
     * datos
     * @param parameters parametros de la sentencia.
     * @return una lista de registros
     * @throws Exception
     * @throws org.javabeanstack.exceptions.SessionError
     */
    @Override
    public <T extends IDataRow> List<T> findListByQuery(String queryString, Map<String, Object> parameters)
            throws Exception, SessionError {
        // Verificar si la sesión es válida
        checkUserSession();
        parameters = addParams(queryString, parameters);
        return getDao().findListByQuery(getSessionId(), queryString, parameters);
    }

    /**
     *
     * Devuelve una lista de registros resultado de la selección de datos en una
     * base de datos
     *
     * @param <T> tipo de dato generalmente hereda de DataRow
     * @param queryString sentencia JPQL que se ejecutará para recuperar los
     * datos
     * @param parameters parametros de la sentencia.
     * @param first a partir de un registro determinado del conjunto
     * @param max máxima cantidad de registros.
     * @return una lista de registros
     * @throws Exception
     * @throws org.javabeanstack.exceptions.SessionError
     */
    @Override
    public <T extends IDataRow> List<T> findListByQuery(String queryString, Map<String, Object> parameters, int first, int max)
            throws Exception, SessionError {
        // Verificar si la sesión es válida
        checkUserSession();
        parameters = addParams(queryString, parameters);
        return getDao().findListByQuery(getSessionId(), queryString, parameters, first, max);
    }

    /**
     * Devuelve un registro resultado de la selección de datos en una base de
     * datos
     *
     * @param <T> tipo de dato generalmente hereda de DataRow
     * @param namedQuery nombre del query definido en el EJB
     * @param parameters parametros de la sentencia.
     * @return una lista de registros
     * @throws Exception
     * @throws org.javabeanstack.exceptions.SessionError
     */
    @Override
    public <T extends IDataRow> T findByNamedQuery(String namedQuery, Map<String, Object> parameters) throws Exception, SessionError {
        // Verificar si la sesión es válida
        checkUserSession();
        return getDao().findByNamedQuery(getSessionId(), namedQuery, parameters);
    }

    /**
     * Devuelve una lista de registros resultado de la selección de datos en una
     * base de datos
     *
     * @param <T> tipo de dato generalmente hereda de DataRow
     * @param namedQuery nombre del query definido en el EJB
     * @param parameters parametros de la sentencia.
     * @return una lista de registros
     * @throws Exception
     * @throws org.javabeanstack.exceptions.SessionError
     */
    @Override
    public <T extends IDataRow> List<T> findListByNamedQuery(String namedQuery, Map<String, Object> parameters) throws Exception, SessionError {
        // Verificar si la sesión es válida
        checkUserSession();
        return getDao().findListByNamedQuery(getSessionId(), namedQuery, parameters);
    }

    /**
     * Devuelve una lista de registros resultado de la selección de datos en una
     * base de datos
     *
     * @param <T> tipo de dato generalmente hereda de DataRow
     * @param namedQuery nombre del query definido en el EJB
     * @param parameters parametros de la sentencia.
     * @param first puntero que define el nro de registro a partir del cual va a
     * recuperar los datos
     * @param max máxima cantidad de registros.
     * @return una lista de registros
     * @throws Exception
     * @throws org.javabeanstack.exceptions.SessionError
     */
    @Override
    public <T extends IDataRow> List<T> findListByNamedQuery(String namedQuery, Map<String, Object> parameters, int first, int max) throws Exception, SessionError {
        // Verificar si la sesión es válida
        checkUserSession();
        return getDao().findListByNamedQuery(getSessionId(), namedQuery, parameters, first, max);
    }

    /**
     * Devuelve una lista de registros resultado de la selección de datos en una
     * base de datos, utilizando una sentencia nativa.
     *
     * @param queryString sentencia select
     * @param parameters parametros de la sentencia
     * @return una lista de registros
     * @throws Exception
     * @throws org.javabeanstack.exceptions.SessionError
     */
    @Override
    public List<Object> findByNativeQuery(String queryString, Map<String, Object> parameters) throws Exception, SessionError {
        // Verificar si la sesión es válida
        checkUserSession();
        parameters = addParams(queryString, parameters);
        return getDao().findByNativeQuery(getSessionId(), queryString, parameters);
    }

    /**
     * Devuelve una lista de registros resultado de la selección de datos en una
     * base de datos, utilizando una sentencia nativa.
     *
     * @param queryString sentencia select
     * @param parameters parametros de la sentencia
     * @param first puntero que define el nro de registro a partir del cual va a
     * recuperar los datos
     * @param max máxima cantidad de registros.
     * @return una lista de registros
     * @throws Exception
     * @throws org.javabeanstack.exceptions.SessionError
     */
    @Override
    public List<Object> findByNativeQuery(String queryString, Map<String, Object> parameters, int first, int max) throws Exception, SessionError {
        // Verificar si la sesión es válida
        checkUserSession();
        parameters = addParams(queryString, parameters);
        return getDao().findByNativeQuery(getSessionId(), queryString, parameters, first, max);
    }

    /**
     * Refresca los datos de un registro en la base de datos.
     *
     * @param <T>
     * @param row registro a refrescar de la base
     * @return registro con los datos actualizados desde la base.
     * @throws Exception
     * @throws org.javabeanstack.exceptions.SessionError
     */
    @Override
    public <T extends IDataRow> T refreshRow(T row) throws Exception, SessionError {
        // Verificar si la sesión es válida
        checkUserSession();
        return getDao().refreshRow(getSessionId(), row);
    }


    /**
     * Devuelve la cantidad de registros que resultaria de una sentencia JPQL
     *
     * @param queryString sentencia JPQL
     * @param parameters parametros de la sentencia
     * @return la cantidad de registros
     * @throws Exception
     * @throws org.javabeanstack.exceptions.SessionError
     */
    @Override
    public Long getCount(String queryString, Map<String, Object> parameters) throws Exception, SessionError {
        // Verificar si la sesión es válida
        checkUserSession();
        parameters = addParams(queryString, parameters);
        return getDao().getCount(getSessionId(), queryString, parameters);
    }

    /**
     * Devuelve la cantidad de registros que resultaria de una sentencia NATIVE
     *
     * @param queryString sentencia JPQL
     * @param parameters parametros de la sentencia
     * @return la cantidad de registros 
     * @throws Exception
     * @throws org.javabeanstack.exceptions.SessionError
     */
    @Override
    public Long getCount2(String queryString, Map<String, Object> parameters) throws Exception, SessionError {
        // Verificar si la sesión es válida
        checkUserSession();
        parameters = addParams(queryString, parameters);
        return getDao().getCount2(getSessionId(), queryString, parameters);
    }

    /**
     * Devuelve un objeto Map con las propiedades del entity manager del dao
     *
     * @return objeto Map con las propiedades del entity manager
     */
    @Override
    public Map<String, Object> getEntityManagerProp() {
        return getDao().getEntityManagerProp(persistUnit);
    }

    /**
     * Devuelve un objeto Map con las propiedades de la conexión de datos.
     *
     * @return objeto Map con las propiedades de la conexión de datos
     */
    @Override
    public Map<String, Object> getPersistUnitProp() {
        return getDao().getPersistUnitProp(persistUnit);
    }

    /**
     * Devuelve el objeto userSession resultante del login del usuario
     *
     * @return userSession
     */
    @Override
    public IUserSession getUserSession() {
        return userSession;
    }

    /**
     * Asigna el objeto userSession a esta clase lo que permitira acceder al
     * esquema de datos de la base de datos.
     *
     * @param userSession objeto resultante del login del usuario
     * @throws org.javabeanstack.exceptions.SessionError
     */
    @Override
    public void setUserSession(IUserSession userSession) throws SessionError {
        if (userSession ==  null){
            this.userSession = null;
            this.persistUnit = IDBManager.CATALOGO;
            return;
        }
        checkUserSession();
        this.userSession = userSession;
        this.persistUnit = userSession.getPersistenceUnit();
    }

    /**
     * Devuelve un objeto DataNativeQuery que permitira ejecutar
     * @return instancia de un objeto DataNativeQuery
     */
    @Override
    public IDataNativeQuery newDataNativeQuery() {
        DataNativeQuery result = new DataNativeQuery();
        result.setDataLink(this);
        return result;
    }

    /**
     * Genera una expresión con las relaciones de las entidades solicitadas.
     * @param entities lista de entidades (tablas, vistas)
     * @param typeRela tipo de relacion (inner, left, right, full) es opcional
     * @param schema nombre del schema que se agregará en la instrucción.
     * @return expresión join.
     * @throws Exception
     * @throws SessionError 
     */
    @Override
    public String getEntitiesRelation(String entities, String typeRela, String schema) throws Exception, SessionError {
        // Verificar lista de entidades
        if (Strings.isNullorEmpty(entities)) {
            return "";
        }
        if (Strings.isNullorEmpty(schema)){
            schema = this.getDao().getSchema(persistUnit);
        }
        entities = entities.toLowerCase().replace("{schema}.", "");
        // Verificar valor de tipo de relación
        if (Strings.isNullorEmpty(typeRela)) {
            typeRela = "INNER";
        }
        typeRela = typeRela.toUpperCase();
        if (("FULL LEFT RIGHT".contains(typeRela)) && !("OUTER".contains(typeRela))) {
            typeRela += " OUTER ";
        }
        typeRela = typeRela.trim() + " ";
        String[] analizar = entities.split("\\,");
        int len = analizar.length;
        String[][] entidades = new String[len][3];
        for (int i = 0; i < analizar.length; i++) {
            String expr = analizar[i].trim();
            int xSpace = expr.indexOf(' ');
            if (xSpace >= 0) {
                entidades[i][0] = expr.substring(0, xSpace).trim();
                entidades[i][1] = expr.substring(xSpace).trim();
            } else {
                entidades[i][0] = expr;
                entidades[i][1] = expr;
            }
        }
        String expresion = schema + "." + entidades[0][0] + " " + entidades[0][1];
        String leftEntidad, leftAlias, rightEntidad, rightAlias;
        IDataLink dao;        
        if (!this.getPersistUnit().equals(IDBManager.CATALOGO)){
            dao = new DataLink(this.getDao());                
        }
        else{
            dao = this;
        } 
        
        for (int i = 0; i < analizar.length; i++) {
            if (i + 1 == analizar.length) {
                continue;
            }
            leftEntidad = entidades[i][0].toLowerCase();
            leftAlias = entidades[i][1].toLowerCase();
            for (int j = i + 1; j < analizar.length; j++) {
                rightEntidad = entidades[j][0].toLowerCase();
                rightAlias = entidades[j][1].toLowerCase();
                
                Map<String, Object> params = new HashMap();
                params.put("entityPK", rightEntidad );
                params.put("entityFK", leftEntidad );
                
                List<IAppTablesRelation> data = dao.findListByNamedQuery("AppTablesRelation.findByEntity", params);
                if (data.isEmpty()){
                    params.put("entityPK", leftEntidad);
                    params.put("entityFK", rightEntidad);
                    data = dao.findListByNamedQuery("AppTablesRelation.findByEntity", params);
                }
                    
                if (data.isEmpty()) {
                    continue;
                }
                String expr1, expr2;
                IAppTablesRelation row = data.get(0);
                if (row.getFieldsFK().equals(leftEntidad)) {
                    expr1 = row.getFieldsFK().trim();
                    expr2 = row.getFieldsPK().trim();
                } else {
                    expr1 = row.getFieldsPK().trim();
                    expr2 = row.getFieldsFK().trim();
                }
                boolean circular = false;
                String variable = " JOIN " + schema + "." + rightEntidad.trim() + " ";
                if (expresion.contains(variable)) {
                    circular = true;
                }
                switch (row.getRelationType()) {
                    case 0:
                        typeRela = "INNER";
                        break;
                    case 1:
                        typeRela = "LEFT OUTER";
                        break;
                    case 2:
                        typeRela = "RIGHT OUTER";
                        break;
                    case 3:
                        typeRela = "FULL OUTER";
                        break;
                    default:
                        typeRela = "INNER";
                        break;
                }
                if (!circular) {
                    expresion += " " + typeRela + " JOIN " + schema + "." + rightEntidad + " " + rightAlias
                            + " ON " + leftAlias + "." + expr1.trim() + " = " + rightAlias + "." + expr2.trim() + " ";
                }
            }
        }
        return expresion;
    }

    /**
     * Verifica la sesión del usuario
     * @return id de la sesión.
     * @throws SessionError 
     */
    private String checkUserSession() throws SessionError {
        if (getUserSession() != null) {
            IUserSession sesion = getDao().getUserSession(getUserSession().getSessionId());
            if (sesion == null) {
                throw new SessionError("El identificador de la sesión es inválido");
            }
            if (sesion.getError() != null) {
                throw new SessionError(sesion.getError().getMessage());
            }
            return getUserSession().getSessionId();
        }
        return "";
    }

    /**
     * Agrega valores de parametros constantes (ej. :true=true, :false=false etc, :idempresa)
     * @param queryString  sentencia
     * @param parameters objeto parametros
     * @return parametros con los valores adicionales.
     */
    private Map<String, Object> addParams(String queryString, Map<String, Object> parameters) {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        if (Strings.findString(":true", queryString.toLowerCase()) >= 0) {
            parameters.put("true", true);
        }
        if (Strings.findString(":false", queryString.toLowerCase()) >= 0) {
            parameters.put("false", false);
        }
        if (Strings.findString(":idempresa", queryString.toLowerCase()) >= 0 
                && getUserSession() != null) {
            parameters.put("idempresa", getUserSession().getIdEmpresa());
        }
        if (Strings.findString(":idcompany", queryString.toLowerCase()) >= 0
                && getUserSession() != null) {                
            parameters.put("idcompany", getUserSession().getIdCompany());
        }
        if (Strings.findString(":today", queryString.toLowerCase()) >= 0) {
            parameters.put("today", Dates.today());
        }
        if (Strings.findString(":now", queryString.toLowerCase()) >= 0) {
            parameters.put("now", Dates.now());
        }
        return parameters;
    }

    /**
     * Devuelve la lista de filas que fuerón modificadas pasibles de actualizar
     * en la base de datos.
     * @param <T>
     * @param dataRows lista de objetos
     * @return lista de filas modificadas.
     */
    private <T extends IDataRow> List<T> getRowsChanged(List<T> dataRows) {
        List<T> dataRowsChanged = new LinkedList();
        for (int i = 0; i < dataRows.size(); i++) {
            if (dataRows.get(i).getAction() != 0) {
                dataRowsChanged.add(dataRows.get(i));
            }
        }
        return dataRowsChanged;
    }

    /**
     * Elimina los registros marcados para borrar de la lista de objetos
     * @param <T>
     * @param dataRows lista de objetos.
     */
    private <T extends IDataRow> void removeDeleted(List<T> dataRows) {
        // Eliminar de la lista local todos los registros marcados para borrar
        Iterator<? extends IDataRow> i = dataRows.iterator();
        while (i.hasNext()) {
            IDataRow r = i.next();
            if (r.getAction() == IDataRow.DELETE) {
                i.remove();
            }
        }
    }
    
    /**
     * Objeto con la información necesaria para acceder a la base de datos.
     * (persistunit, session del usuario)
     * @return DBLinkInfo()
     */
    protected final IDBLinkInfo getDBLinkInfo(){
        IDBLinkInfo dbInfo = new DBLinkInfo();
        //TODO variante token
        dbInfo.setUserSession(userSession);
        return dbInfo;
    }
    
    protected final String getSessionId(){
        if (userSession == null){
            return null;
        }
        return userSession.getSessionId();
    }
}
