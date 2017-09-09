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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.exceptions.SessionError;
import org.javabeanstack.util.Dates;
import org.javabeanstack.util.Fn;
import org.javabeanstack.util.Strings;
import static org.javabeanstack.util.Fn.iif;
import static org.javabeanstack.util.Strings.occurs;
import static org.javabeanstack.util.Strings.isNullorEmpty;

/**
 * Esta clase abstrae funcionalidades que permiten ejecutar sentencias nativas
 * sobre la base de datos.
 *
 * El test unitario se encuentra en TestProjects clase
 * py.com.oym.test.data.TestDataNativeQuery
 *
 * @author Jorge Enciso
 */
public class DataNativeQuery implements IDataNativeQuery {

    private static final Logger LOGGER = Logger.getLogger(DataNativeQuery.class);

    private boolean queryCreated;
    private String querySentence;
    private String columnExpr;
    private String[] columnList;
    private String entityExpr;
    private String fromEntity;
    private String[] entityList;
    private String filterExpr;
    private String[] filterExprList;
    private String orderExpr;
    private String[] orderList;
    private String groupExpr;
    private String[] groupList;
    private String filterGroupExpr;
    private String[] filterGroupList;
    private final LinkedList<JoinParam> joinParams = new LinkedList();
    private Map<String, Object> queryParams = new HashMap<>();
    private IDataLink dataLink;
    private final Map<String, String> queryConstants = new HashMap<>();
    private int first;
    private int maxResult;
    private IDataNativeQuery subQueryFrom;
    private String subQueryFromSentence;
    private String subQueryAlias;
    private boolean applyDBQueryFilter = true;

    public DataNativeQuery() {
    }

    /**
     * Asigna la lista de columnas
     *
     * @param columns Son las columnas de la sentencia select
     * @return objeto dataNativeQuery con la asignación de las columnas
     */
    @Override
    public IDataNativeQuery select(String columns) {
        columnExpr = columns;
        queryCreated = false;
        columnList = null;
        columnList = setColumnLabel(columnExpr);
        return this;
    }

    /**
     * Asigna la/s entidad/es desde donde se extraeran la información.
     *
     * @param entities es la lista de entidades (tablas o vistas) forman parte
     * de from, inner join
     * @return objeto dataNativeQuery con la asignación de las entidades
     */
    @Override
    public IDataNativeQuery from(String entities) {
        entityExpr = entities;        
        fromEntity = entities;
        
        subQueryAlias = null;
        subQueryFrom = null;
        entityList = null;
        queryCreated = false;
        joinParams.clear();
        filterExpr  = null;
        filterExprList = null;
        orderExpr = null;
        orderList = null;
        groupExpr = null;
        groupList = null;
        filterGroupExpr = null;
        filterGroupList = null;
        return this;
    }

    /**
     * Asigna la/s entidad/es desde donde se extraeran la información.
     *
     * @param query objeto IDataNativeQuery cuya sentencia es utilizada como
     * subquery dentro de la clausula FROM.
     * @return objeto dataNativeQuery con la asignación de las entidades
     */
    @Override
    public IDataNativeQuery from(IDataNativeQuery query, String alias) {
        subQueryAlias = alias;
        subQueryFrom = query;
        entityExpr = "";
        entityList = null;
        fromEntity = null;
        joinParams.clear();
        queryCreated = false;        
        return this;
    }

    /**
     * Asigna la/s entidad/es desde donde se extraeran la información.
     *
     * @param query objeto IDataNativeQuery cuya sentencia es utilizada como
     * subquery dentro de la clausula FROM.
     * @return objeto dataNativeQuery con la asignación de las entidades
     */
    @Override
    public IDataNativeQuery from(String query, String alias) {
        subQueryAlias = alias;
        subQueryFromSentence = query;
        entityExpr = "";
        entityList = null;
        fromEntity = null;
        joinParams.clear();
        queryCreated = false;        
        return this;
    }
    
    /**
     * Asigna la/s entidad/es que formarán parte del join
     *
     * @param entity es la lista de entidad (tabla o vista)
     * @param joinExpr la expresión del join.
     * @return objeto dataNativeQuery con la asignación de las entidades
     */
    @Override
    public IDataNativeQuery join(String entity, String joinExpr) {
        joinParams.add(new JoinParam(entity, joinExpr, "INNER JOIN"));
        queryCreated = false;
        return this;
    }

    /**
     * Asigna la/s entidad/es que formarán parte del join
     *
     * @param joinExpr la expresión del join.
     * @return objeto dataNativeQuery con la asignación de las entidades
     */
    @Override
    public IDataNativeQuery join(IDataNativeQuery subquery, String alias, String joinExpr) {
        joinParams.add(new JoinParam(subquery, alias, joinExpr, "INNER JOIN"));
        queryCreated = false;
        return this;
    }
    
    /**
     * Asigna la/s entidad/es que formarán parte del join
     *
     * @param entity entidad (tabla o vista)
     * @param joinExpr la expresión del join.
     * @return objeto dataNativeQuery con la asignación de las entidades
     */
    @Override
    public IDataNativeQuery leftJoin(String entity, String joinExpr) {
        joinParams.add(new JoinParam(entity, joinExpr, "LEFT OUTER JOIN"));
        queryCreated = false;
        return this;
    }

    /**
     * Asigna los filtros en la expresión where.
     *
     * @param filterExpr son los filtros
     * @return objeto dataNativeQuery con la asignación de la expresión
     */
    @Override
    public IDataNativeQuery where(String filterExpr) {
        this.filterExpr = filterExpr;
        this.filterExpr += getDBFilterExpr();
        this.filterExprList = null;
        queryCreated = false;
        return this;
    }

    /**
     * Asigna los filtros en la expresión where.
     *
     * @param filterExpr son los filtros
     * @param params son los parametros del query
     * @return objeto dataNativeQuery con la asignación de la expresión
     */
    @Override
    public IDataNativeQuery where(String filterExpr, Map<String, Object> params) {
        this.filterExpr = filterExpr;
        this.filterExpr += getDBFilterExpr();        
        this.filterExprList = null;
        queryCreated = false;
        addParams(params);
        return this;
    }

    /**
     * Asigna los filtros en la expresión where.
     *
     * @param dataExpr son los filtros
     * @return objeto dataNativeQuery con la asignación de la expresión
     */
    @Override
    public IDataNativeQuery where(IDataExpression dataExpr) {
        this.filterExpr = dataExpr.getSentence();
        this.filterExpr += getDBFilterExpr();        
        this.filterExprList = null;
        queryCreated = false;
        if (!dataExpr.getSentenceParams().isEmpty()) {
            addParams(dataExpr.getSentenceParams());
        }
        return this;
    }

    /**
     * Asigna los filtros en la expresión where.
     *
     * @param dataExpr son los filtros
     * @param params son los parametros del query
     * @return objeto dataNativeQuery con la asignación de la expresión
     */
    @Override
    public IDataNativeQuery where(IDataExpression dataExpr, Map<String, Object> params) {
        this.filterExpr = dataExpr.getSentence();
        this.filterExpr += getDBFilterExpr();        
        this.filterExprList = null;
        queryCreated = false;
        addParams(params);
        return this;
    }

    private String getDBFilterExpr(){
        String result="";
        if (getApplyDBFilter() 
                && this.getDataLink() != null
                && this.getDataLink().getUserSession() != null
                && !this.getDataLink().getPersistUnit().equals(IDBManager.CATALOGO)){
            
            String operador = isNullorEmpty(filterExpr) ? "" : " and ";
            try {
                String entityExp = getEntityList()[0];
                int pos = entityExp.indexOf(" ") > 0 ? entityExp.indexOf(" ") : entityExp.length();
                
                String entity = entityExp.substring(0,pos);
                String entityAlias = Strings.substr(entityExp, pos+1);
                        
                IDBFilter dbFilter = getDataLink().getUserSession().getDBFilter();

                Class clazz = getClassModel(dbFilter.getModelPackagePath(), entity);
                if (clazz != null){
                    String filter = dbFilter.getFilterExpr(clazz, entityAlias.trim());
                    if (!isNullorEmpty(filter)){
                        result = operador + filter;
                    }
                }
            }
            catch (Exception exp){
                result = "";
            }
            
        }
        return result;
    }
    
    private Class getClassModel(String packagePath, String entity){
        String className = "";
        String[] partes = entity.split("_");
        for (String parte : partes) {
            className += Strings.Capitalize(parte).trim();
        }
        String[] path = packagePath.split(";");
        Class clazz=null;
        for (String packages : path) {
            String classPath = packages.trim() + "." + className;
            try{
                clazz = Class.forName(classPath);
                break;
            }
            catch (ClassNotFoundException ex){
                //continua con la busqueda
            }
        }
        return clazz;
    }
    
    /**
     * Asigna la expresión order by
     *
     * @param columnOrder lista de columnas por la que se va a ordenar el select
     * @return objeto dataNativeQuery con la asignación de la expresión
     */
    @Override
    public IDataNativeQuery orderBy(String columnOrder) {
        orderExpr = columnOrder;
        queryCreated = false;
        orderList = setColumnLabel(orderExpr);
        return this;
    }

    /**
     * Asigna la expresión group by
     *
     * @param columnGroup lista de columnas group by
     * @return objeto dataNativeQuery con la asignación de la expresión
     */
    @Override
    public IDataNativeQuery groupBy(String columnGroup) {
        groupExpr = columnGroup;
        groupList = setColumnLabel(groupExpr);
        queryCreated = false;
        return this;
    }

    /**
     * Asigna la expresión having
     *
     * @param filterExpr filtros having
     * @return objeto dataNativeQuery con la asignación de la expresión
     */
    @Override
    public IDataNativeQuery having(String filterExpr) {
        filterGroupExpr = filterExpr;
        queryCreated = false;
        return this;
    }

    /**
     * Agrega valores a los parámetros de la sentencia
     *
     * @param key nombre del parámetro
     * @param value valor del parámetro
     * @return objeto dataNativeQuery con la asignación de los parámetros
     */
    @Override
    public IDataNativeQuery addParam(String key, Object value) {
        queryParams.put(key, value);
        return this;
    }

    /**
     * Asigna los parámetros de la sentencia
     *
     * @param params Map conteniendo los parámetros
     * @return objeto dataNativeQuery con la asignación de los parámetros
     */
    @Override
    public IDataNativeQuery addParams(Map<String, Object> params) {
        queryParams = params;
        return this;
    }

    /**
     * Asigna los parámetros de la sentencia
     *
     * @param params Map conteniendo los parámetros
     * @return objeto dataNativeQuery con la asignación de los parámetros
     */
    @Override
    public IDataNativeQuery setQueryParams(Map<String, Object> params) {
        queryParams = params;
        return this;
    }

    /**
     * Crear la sentencia select a partir de todas los parámetros previos
     * recibidos
     */
    @Override
    public void createQuery() {
        querySentence = "SELECT " + columnExpr + " \r\n";

        String fromExpr = getFromExpr(); 
        querySentence += " FROM " + fromExpr + "\r\n";
        if (Strings.isNullorEmpty(filterExpr)){
            filterExpr = getDBFilterExpr();
        }
        if (!Strings.isNullorEmpty(filterExpr)) {
            querySentence += " WHERE " + filterExpr + " \r\n";
        }
        if (!Strings.isNullorEmpty(groupExpr)) {
            querySentence += " GROUP BY " + groupExpr + " \r\n";
        }
        if (!Strings.isNullorEmpty(filterGroupExpr)) {
            querySentence += " HAVING " + filterGroupExpr + " \r\n";
        }
        if (!Strings.isNullorEmpty(orderExpr)) {
            querySentence += " ORDER BY " + orderExpr + " \r\n";
        }
        setQueryConstants();
        querySentence = Strings.textMerge(querySentence, queryConstants);
        setDefaultParams();
        queryCreated = true;
    }

    @Override
    public final String getFromExpr() {
        String fromExpr = "";        
        // Generar from desde un subquery
        if (subQueryFrom != null) {
            subQueryFrom.createQuery();
            String subQuerySentence = subQueryFrom.getQuerySentence();
            fromExpr = "("+subQuerySentence+") "+subQueryAlias+ " ";
            fromExpr += getJoinExpr();
            return fromExpr;
        }
        // Generar from desde una sentencia subquery
        if (!isNullorEmpty(subQueryFromSentence)) {
            String subQuerySentence = subQueryFromSentence;
            fromExpr = "("+subQuerySentence+") "+subQueryAlias+ " ";
            fromExpr += getJoinExpr();
            return fromExpr;
        }
        
        String schema = dataLink.getDao().getSchema(dataLink.getPersistUnit());
        String joinEntityExpr = getJoinEntityExpr();
        entityExpr = fromEntity + iif(joinEntityExpr.isEmpty(), "", ", ");
        // Si no existe los parámetros de joins y la lista de entidades es mayor a 1, 
        // generar la expresión de joins en forma automática.
        if (joinParams.isEmpty() && getEntityList().length > 1) {
            try {
                fromExpr = dataLink.getEntitiesRelation(entityExpr, "", schema);
            } catch (Exception exp) {
                ErrorManager.showError(exp, LOGGER);
            }
        } // Si existe los parámetros de join crear la expresión a partir de sus parámetros.
        else if (!joinParams.isEmpty()) {
            fromExpr = getEntityWithSchema(this.fromEntity);
            fromExpr += getJoinExpr();
        } else {
            fromExpr = getEntityWithSchema(this.fromEntity);
        }
        return fromExpr;
    }

    protected final String getJoinExpr(){
        String result="";
        String entity; 
        for (JoinParam param : joinParams) {
            if (param.joinSubquery != null){
                param.joinSubquery.createQuery();
                result += " \r\n" + param.joinType 
                        + " (" + param.joinSubquery.getQuerySentence() + ") " 
                        + param.joinSubqueryAlias
                        + " on " + param.joinExpr;
            }
            else{
                entity = getEntityWithSchema(param.joinEntity);
                result += " \r\n" + param.joinType + " " + entity + " on " + param.joinExpr;
            }
        }
        return result;
    }
    
    protected final String getEntityWithSchema(String entity) {
        if (occurs(".", entity) == 0) {
            entity = "{schema}." + entity;
        }
        return entity;
    }

    /**
     * Crear la sentencia select a partir de todas los parámetros previos
     * recibidos
     */
    @Override
    public String createQuerySentence() {
        if (queryCreated) {
            return querySentence;
        }
        this.createQuery();
        return querySentence;
    }

    /**
     * Agrega valores de parametros constantes (ej. :true=true, :false=false
     * etc, :idempresa)
     */
    protected void setDefaultParams() {
        if (Strings.findString(":true", querySentence.toLowerCase()) >= 0) {
            queryParams.put("true", true);
        }
        if (Strings.findString(":false", querySentence.toLowerCase()) >= 0) {
            queryParams.put("false", false);
        }
        if (!queryParams.containsKey("idempresa")) {
            if (Strings.findString(":idempresa", querySentence.toLowerCase()) >= 0) {
                if (getDataLink().getUserSession() != null) {
                    queryParams.put("idempresa", getDataLink().getUserSession().getIdEmpresa());
                }
            }
        }
        if (!queryParams.containsKey("today")) {
            if (Strings.findString(":today", querySentence.toLowerCase()) >= 0) {
                queryParams.put("today", Dates.today());
            }
        }
        if (!queryParams.containsKey("now")) {
            if (Strings.findString(":now", querySentence.toLowerCase()) >= 0) {
                queryParams.put("now", Dates.now());
            }
        }
    }

    /**
     * Asigna valores constantes a ciertos parámetros que serán reemplazadas al
     * crear la sentencia.
     */
    private void setQueryConstants() {
        String schema = (String) dataLink.getPersistUnitProp().get("hibernate.default_schema");
        queryConstants.put("schema", schema);
        String schemaCat = (String) dataLink.getDao().getPersistUnitProp(IDBManager.CATALOGO).get("hibernate.default_schema");
        queryConstants.put("schemacatalog", schemaCat);
        String motordatos = (String) dataLink.getPersistUnitProp().get("oym.motordatos");
        if ("POSTGRES".equals(motordatos)) {
            queryConstants.put("true", "true");
            queryConstants.put("false", "false");
        } else {
            queryConstants.put("true", "1");
            queryConstants.put("false", "0");
        }
        // Asignar las constantes de fecha y hora
        if (Fn.inList(motordatos, "SQLSERVER", "Microsoft SQL Server", "SYBASE")) {
            queryConstants.put("now", "getdate()");
            queryConstants.put("today", "CONVERT(date, GETDATE())");
        } else if (Fn.inList(motordatos, "ORACLE", "ORACLE8")) {
            queryConstants.put("now", "sysdate");
            queryConstants.put("today", "TRUNC(sysdate)");
        } else if ("POSTGRES".equals(motordatos)) {
            queryConstants.put("now", "now()");
            queryConstants.put("today", "date_trunc('day', now())");
        } else if ("DB2".equals(motordatos)) {
            queryConstants.put("now", "CURRENT_TIMESTAMP");
            queryConstants.put("today", "CURRENT DATE");
        }
    }

    /**
     * Ejecuta la sentencia y devuelve una lista de registros
     *
     * @return devuelve una lista de registros
     * @throws org.javabeanstack.exceptions.SessionError
     */
    @Override
    public List<IDataQueryModel> execQuery() throws SessionError {
        return execQuery(this.dataLink);
    }

    /**
     * Ejecuta la sentencia y devuelve una lista de registros
     *
     * @param first
     * @param maxResult
     * @return devuelve una lista de registros
     * @throws org.javabeanstack.exceptions.SessionError
     */
    @Override
    public List<IDataQueryModel> execQuery(int first, int maxResult) throws SessionError {
        this.first = first;
        this.maxResult = maxResult;
        return execQuery(this.dataLink);
    }

    /**
     * Ejecuta la sentencia y devuelve una lista de registros
     *
     * @param dataLink objeto acceso a los datos.
     * @return devuelve una lista de registros
     * @throws org.javabeanstack.exceptions.SessionError
     */
    @Override
    public List<IDataQueryModel> execQuery(IDataLink dataLink) throws SessionError {
        List<IDataQueryModel> result = new ArrayList<>();
        try {
            if (!queryCreated) {
                createQuery();
            }
            List<Object> list;
            if (this.first > 0 || this.maxResult > 0) {
                list = dataLink.findByNativeQuery(querySentence, queryParams, first, maxResult);
            } else {
                list = dataLink.findByNativeQuery(querySentence, queryParams);
            }
            result = converToNativeQuery(list, this.columnExpr);
            this.first = 0;
            this.maxResult = 0;
        } catch (SessionError ex) {
            throw new SessionError("La sesión expiró o es inválida");
        } catch (Exception ex) {
            ErrorManager.showError(ex, Logger.getLogger(DataNativeQuery.class));
        }
        return result;
    }

    /**
     * Convierte una lista de objetos al tipo IDataQueryModel
     *
     * @param source lista de registros
     * @param columns lista de las etiquetas de las columnas
     * @return devuelve misma lista de registros pero como tipo IDataQueryModel
     */
    public static final List<IDataQueryModel> converToNativeQuery(List<Object> source, String columns) {
        if (source == null) {
            return null;
        }
        List<IDataQueryModel> target = new ArrayList<>(source.size());
        String[] columnsLabel = setColumnLabel(columns);
        for (int i = 0; i < source.size(); i++) {
            IDataQueryModel row = new DataQueryModel();
            row.setRow(source.get(i));
            row.setColumnList(columnsLabel);
            target.add(row);
        }
        return target;
    }

    /**
     * Devuelve una matriz conteniendo los nombres de las columnas del cursor
     * resultante del query
     *
     * @param columns
     * @return
     */
    protected static String[] setColumnLabel(String columns) {
        /* Reemplazar texto que se encuentra entre parentesis */
        String regex = "\\(.*?\\)";
        String expr = columns;
        expr = Strings.varReplace(expr, "()");
        expr = expr.replaceAll(regex, "()");
        String[] matrix = convertToMatrix(expr, ",");
        int pos;
        for (int i = 0; i < matrix.length; i++) {
            // Buscar nombre de la columna ejemplo fn_iditem() as item, item es el nombre
            pos = matrix[i].toLowerCase().indexOf(" as ");
            if (pos >= 0) {
                matrix[i] = matrix[i].substring(pos + 4).toLowerCase();
                continue;
            }
            // Buscar nombre de la columna ejemplo b.vendedor, vendedor es el nombre
            pos = matrix[i].toLowerCase().lastIndexOf(".");
            if (pos >= 0) {
                matrix[i] = matrix[i].substring(pos + 1);
            }
            matrix[i] = matrix[i].toLowerCase();
        }
        return matrix;
    }

    /**
     * Convierte una expresión en una matriz
     *
     * @param expr expresión
     * @param separator separadores normalmente comas.
     * @return devuelve una matriz
     */
    protected static String[] convertToMatrix(String expr, String separator) {
        String[] exprList = expr.split("\\" + separator);
        for (int i = 0; i < exprList.length; i++) {
            exprList[i] = exprList[i].trim();
        }
        return exprList;
    }

    protected void populateQueryParams(Map<String, Object> params) {
        params.entrySet().forEach((param) -> {
            queryParams.put(param.getKey(), param.getValue());
        });
    }

    /**
     *
     * @return devuelve las columnas (porción select) del la sentencia
     */
    @Override
    public String getColumnExpr() {
        return columnExpr;
    }

    /**
     * Lista de columnas
     *
     * @return
     */
    @Override
    public String[] getColumnList() {
        return columnList;
    }

    /**
     *
     * @return entidades (tablas y/o vistas)
     */
    @Override
    public String getEntityExpr() {
        return entityExpr;
    }

    /**
     *
     * @return lista de entidades
     */
    @Override
    public String[] getEntityList() {
        if (entityList == null) {
            entityList = convertToMatrix(entityExpr, ",");
        }
        return entityList;
    }

    /**
     *
     * @return filtros (expresión where)
     */
    @Override
    public String getFilterExpr() {
        return filterExpr;
    }

    /**
     *
     * @return lista de filtros (expresión where)
     */
    @Override
    public String[] getFilterExprList() {
        if (filterExprList == null) {
            filterExprList = convertToMatrix(filterExpr, ",");
        }
        return filterExprList;
    }

    /**
     *
     * @return expresión order by
     */
    @Override
    public String getOrderExpr() {
        return orderExpr;
    }

    /**
     *
     * @return lista de columnas order by
     */
    @Override
    public String[] getOrderList() {
        return orderList;
    }

    /**
     *
     * @return expresión group by
     */
    @Override
    public String getGroupExpr() {
        return groupExpr;
    }

    /**
     *
     * @return lista de columnas del group by
     */
    @Override
    public String[] getGroupList() {
        if (groupList == null) {
            groupList = convertToMatrix(groupExpr, ",");
        }
        return groupList;
    }

    /**
     *
     * @return expresión having
     */
    @Override
    public String getFilterGroupExpr() {
        return filterGroupExpr;
    }

    /**
     * Devuelve la lista de filtros en la expresion having
     *
     * @return lista de filtros en la expresion having
     */
    @Override
    public String[] getFilterGroupExprList() {
        if (filterGroupList == null) {
            filterGroupList = convertToMatrix(filterGroupExpr, ",");
        }
        return filterGroupList;
    }

    /**
     *
     * @return la sentencia resultante de los parámetros recibidos.
     */
    @Override
    public String getQuerySentence() {
        return querySentence;
    }

    /**
     *
     * @return parámetros
     */
    @Override
    public Map<String, Object> getQueryParams() {
        return queryParams;
    }

    /**
     *
     * @return devuelve el objeto para acceso a datos.
     */
    @Override
    public IDataLink getDataLink() {
        return dataLink;
    }

    /**
     * Asigna el objeto de acceso a los datos.
     *
     * @param dao objeto para acceder a los datos.
     */
    @Override
    public void setDataLink(IDataLink dao) {
        dataLink = dao;
    }

    /**
     * Retorna la cantidad de registros que resultaria de ejecutar la sentencia
     * SELECT generada por esta clase.
     *
     * @return
     * @throws java.lang.Exception
     */
    @Override
    public Long getCount() throws Exception {
        String query = getQueryCount();
        return this.dataLink.getCount2(query, queryParams);
    }

    protected String getQueryCount() {
        String query;
        query = "select 1 as xx " + "from   " + entityExpr + " \n";
        if (!Strings.isNullorEmpty(filterExpr)) {
            query += " where " + filterExpr + " \n";
        }
        if (!Strings.isNullorEmpty(groupExpr)) {
            query += " group by " + groupExpr + " \n";
        }
        if (!Strings.isNullorEmpty(filterGroupExpr)) {
            query += " having " + filterGroupExpr + " \n";
        }
        return query;
    }

    protected String getJoinEntityExpr() {
        if (joinParams.isEmpty()) {
            return "";
        }
        String devolver = "";
        for (JoinParam element : joinParams) {
            if (element.joinSubquery != null){
                continue;
            }
            devolver += element.joinEntity + ", ";                
        }
        devolver = devolver.trim();
        if (devolver.endsWith(",")) {
            devolver = devolver.substring(0, devolver.length() - 1);
        }
        return devolver;
    }

    @Override
    public boolean getApplyDBFilter() {
        return applyDBQueryFilter;
    }

    @Override
    public void setApplyDBFilter(boolean apply) {
        this.applyDBQueryFilter = apply;
    }

    class JoinParam {
        String joinEntity;
        String joinExpr;
        String joinType;
        IDataNativeQuery joinSubquery;
        String joinSubqueryAlias;

        JoinParam(String joinEntity, String joinExpr, String joinType) {
            this.joinEntity = joinEntity;
            this.joinExpr = joinExpr;
            this.joinType = joinType;
        }
        
        JoinParam(IDataNativeQuery joinSubquery, String alias, String joinExpr, String joinType) {
            this.joinSubquery = joinSubquery;
            this.joinSubqueryAlias = alias;
            this.joinEntity = "";
            this.joinExpr = joinExpr;
            this.joinType = joinType;
        }
    }
}
