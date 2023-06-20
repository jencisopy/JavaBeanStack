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
package org.javabeanstack.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.javabeanstack.data.model.DataQueryModel;
import org.javabeanstack.data.IDataExpression;
import org.javabeanstack.data.IDataLink;
import org.javabeanstack.data.IDataNativeQuery;
import org.javabeanstack.data.IDataQueryModel;
import org.javabeanstack.error.ErrorManager;

import static org.javabeanstack.util.Strings.*;
import static org.javabeanstack.util.Fn.iif;
import static org.javabeanstack.util.Fn.nvl;


/**
 * Crea y ejecuta una sentencia sql devolviendo los datos solicitados.<br>
 * Se estructura de la siguiente manera. <br><br>
 * <pre>
 * select {columnsGroup1}, {columnsGroup2}, {columnsGroup3}, {columns}
 *    from {instrucción generada a partir de entityRoot y entityToJoin
 *          en la innerclass EntitiesToRelation y EntitiesRelation}
 *    where {whereFilter}
 *    order by {orderBy}
 *    group by {groupBy}
 *
 * </pre>
 *
 * @author Jorge Enciso
 */
public class DataReport {
    private static final Logger LOGGER = Logger.getLogger(DataReport.class);

    /**
     * Objeto manejador de datos
     */
    private IDataLink dataLink;
    
    /**
     * Lista de entidades desde donde se buscará la información
     */
    private String entitiesToJoin = "";
    /**
     * Lista de entidades con sus alias personalizados
     */
    private String entitiesAlias = "";
    /**
     * Entidad raiz de la sentencia sql a generarse
     */
    private String entityRoot;
    /**
     * Primer grupo de columnas
     */
    private String columnsGroup1;
    /**
     * Segundo grupo de columnas
     */
    private String columnsGroup2;
    /**
     * Tercer grupo de columnas
     */
    private String columnsGroup3;

    /**
     * Lista de columnas
     */
    private String columns;
    /**
     * Condición where
     */
    private IDataExpression whereFilter;
    /**
     * Expresión order by
     */
    private String orderBy;
    /**
     * Expresión group by
     */
    private String groupBy;
    /**
     * Codigo del documento que se utilizará para filtrar información
     */
    private String document;
    /**
     * Nro de reporte generado, para propsitos de auditoria
     */
    private Integer reportNumber = 0;
    /**
     * Objeto query responsable de ejecutar la instrucción sql y exponer el
     * resultado
     */
    private IDataNativeQuery query;
    /**
     * Sentencia sql generada en el metodo sqlSentenciaCreate()
     */
    private String sqlSentence;

    protected Map<String, Object> preference1 = new HashMap();
    protected Map<String, Object> preference2 = new HashMap();
    
    private int dateGroup = 0; // 0 && Ninguno, 1 año, 2 año y mes

    public DataReport() {
        // Implementar en clases heredadas
    }

    public DataReport(IDataLink dao) {
        this.dataLink = dao;
    }

    public int getDateGroup() {
        return dateGroup;
    }

    public void setDateGroup(int dateGroup) {
        this.dateGroup = dateGroup;
    }

    public void init(IDataLink dao, Map<String, Object> preference1, Map<String, Object> preference2) {
        throw new UnsupportedOperationException("Debe implementar el metodo " + getClass().getName());
    }
    
    public void init(Map<String, Object> preference1, Map<String, Object> preference2) {
        throw new UnsupportedOperationException("Debe implementar el metodo " + getClass().getName());
    }

    /**
     * Devuelve objeto para acceso a la base de datos
     *
     * @return objeto para acceso a la base de datos
     */
    public final IDataLink getDataLink() {
        return dataLink;
    }

    public IDataNativeQuery getQuery() {
        return query;
    }

    protected void setQuery(IDataNativeQuery query) {
        this.query = query;
    }
    
    /**
     * Asigna el objeto para acceso a la base de datos
     *
     * @param dataLink objeto manejador de datos.
     */
    public final void setDataLink(IDataLink dataLink) {
        this.dataLink = dataLink;
    }

    /**
     * Primer grupo de columnas
     *
     * @return Primer grupo de columnas
     */
    public final String getColumnsGroup1() {
        return columnsGroup1;
    }

    /**
     * Asigna el primer grupo de columnas
     *
     * @param columnsGroup1 primer grupo de columnas
     */
    public final void setColumnsGroup1(String columnsGroup1) {
        this.columnsGroup1 = columnsGroup1;
    }

    /**
     * Segundo grupo de columnas
     *
     * @return Segundo grupo de columnas
     */
    public final String getColumnsGroup2() {
        return columnsGroup2;
    }

    /**
     * Asigna el segundo grupo de columnas
     *
     * @param columnsGroup2 segundo grupo de columnas
     */
    public final void setColumnsGroup2(String columnsGroup2) {
        this.columnsGroup2 = columnsGroup2;
    }

    /**
     * Tercer grupo de columnas
     *
     * @return Tercer grupo de columnas
     */
    public final String getColumnsGroup3() {
        return columnsGroup3;
    }

    /**
     * Asigna el tercer grupo de columnas
     *
     * @param columnsGroup3 primer grupo de columnas
     */
    public final void setColumnsGroup3(String columnsGroup3) {
        this.columnsGroup3 = columnsGroup3;
    }

    /**
     * Lista de columnas
     *
     * @return lista de columnas
     */
    public final String getColumns() {
        return columns;
    }

    /**
     * Asigna la lista de columnas
     *
     * @param columns
     */
    public final void setColumns(String columns) {
        this.columns = columns;
    }

    /**
     * Lee la lista de entidades (tablas, vistas) que se utilizarán en la
     * consulta
     *
     * @return lista de entidades
     */
    public final String getEntitiesToJoin() {
        return entitiesToJoin;
    }

    /**
     * Asigna la lista de entidades (tablas, vistas) que se utilizarán en la
     * sentencia
     *
     * @param entitiesToJoin
     */
    public final void setEntitiesToJoin(String entitiesToJoin) {
        this.entitiesToJoin = entitiesToJoin;
    }

    /**
     * Devuelve la entidad raiz a partir de la cual se realizarán los joins para
     * construir la sentencia sql.
     *
     * @return entidad raiz.
     */
    public final String getEntityRoot() {
        return entityRoot;
    }

    /**
     * Asigna la entidad raiz de la sentencia sql.
     *
     * @param entityRoot
     */
    public final void setEntityRoot(String entityRoot) {
        this.entityRoot = entityRoot;
    }

    /**
     * EntitiesAlias es la una lista de entidades (tablas, vistas) que tienen
     * asignado un alias, por la cual serán referenciados en la sentencia sql
     *
     * @return lista separada con coma de entidades con sus alias
     */
    public final String getEntitiesAlias() {
        return entitiesAlias;
    }

    /**
     * Asigna la lista de entidades <br>
     * ej. item a, vendedor b
     *
     * @param entitiesAlias (
     */
    public final void setEntitiesAlias(String entitiesAlias) {
        this.entitiesAlias = entitiesAlias;
    }

    public final String getDocument() {
        return document;
    }

    public final void setDocument(String document) {
        this.document = document;
    }

    /**
     * Devuelve la condición de filtros de la sentencia sql.
     *
     * @return condición where.
     */
    public final IDataExpression getWhereFilter() {
        return whereFilter;
    }

    /**
     * Setea la condición where de la sentencia sql.
     *
     * @param whereFilter
     */
    public void setWhereFilter(IDataExpression whereFilter) {
        this.whereFilter = whereFilter;
    }

    /**
     * Devuelve lista de campos del group by si lo tuviere
     *
     * @return lista de campos del group by
     */
    public final String getGroupBy() {
        return groupBy;
    }

    /**
     * Asigna la expresión group by para la sentencia sql.
     *
     * @param groupBy
     */
    public final void setGroupBy(String groupBy) {
        this.groupBy = groupBy;
    }

    /**
     * Devuelve la expresión order by asignada
     *
     * @return expresión order by
     */
    public final String getOrderBy() {
        return orderBy;
    }

    /**
     * Asigna la expresión order by
     *
     * @param orderBy
     */
    public final void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }


    /**
     * Devuelve el nro de reporte generado.
     *
     * @return nro de reporte.
     */
    public final Integer getReportNumber() {
        return reportNumber;
    }

    /**
     * Devuelve la sentencia generada, previamente debio ejecutarse el metodo
     * createSqlSentence().
     *
     * @return sentencia sql
     */
    public String getSqlSentence() {
        return sqlSentence;
    }

    /**
     * Asigna la propiedad sqlSentence
     *
     * @param sqlSentence
     */
    protected void setSqlSentence(String sqlSentence) {
        this.sqlSentence = sqlSentence;
    }
    
    public Map<String, Object> getJasperParams() {
        throw new UnsupportedOperationException("Debe implementar el metodo getJasperParams " + getClass().getName());
    }
    
    /**
     * Acomoda los valores previo a ejecutar el metodo sqlSentenciaCreate()
     */
    protected void setProperties() {
        columnsGroup1 = nvl(columnsGroup1, "").trim();
        if (columnsGroup1.endsWith(",")) {
            columnsGroup1 = left(columnsGroup1, columnsGroup1.length() - 1);
        }
        columnsGroup2 = nvl(columnsGroup2, "").trim();
        if (columnsGroup2.endsWith(",")) {
            columnsGroup2 = left(columnsGroup2, columnsGroup2.length() - 1);
        }
        columnsGroup3 = nvl(columnsGroup3, "").trim();
        if (columnsGroup3.endsWith(",")) {
            columnsGroup3 = left(columnsGroup3, columnsGroup3.length() - 1);
        }
        columns = nvl(columns, "").trim();
        if (columns.endsWith(",")) {
            columns = left(columns, columns.length() - 1);
        }
        groupBy = nvl(groupBy, "");
        orderBy = nvl(orderBy, "");
    }

    protected void beforeCreateSentence(){
        // Implementar en clases heredadas
    }
    
    /**
     * Genera la sentencia sql a partir de los parámetros asignados
     * (columnsGroup1, columnsGroup2, columns, entityRoot, entitiesToJoin,
     * entitiesAlias, whereFilter, orderBy, groupBy), el valor resultante asigna
     * en el atributo sqlSentence y en el objeto query que luego será utilizado
     * en sqlSentenceExecute.
     */
    public void createSqlSentence() {
        beforeCreateSentence();
        setProperties();

        String select = iif(!isNullorEmpty(columnsGroup1), columnsGroup1 + "," , "")
                + iif(!isNullorEmpty(columnsGroup2), columnsGroup2 + ",", "")
                + iif(!isNullorEmpty(columnsGroup3), columnsGroup3 + ",", "")
                + iif(!isNullorEmpty(columns),  columns, "");
        
        select = select.trim();
        if (select.endsWith(",")){
            select = select.substring(0, select.length()-1);
        }

        EntitiesToRelation entityListRelations = new EntitiesToRelation();
        String sentenceSearch = select + " " + whereFilter.getSentence() + " " + orderBy;
        String entityExpr = entityListRelations.get(entityRoot, entitiesToJoin,
                entitiesAlias, sentenceSearch);

        query = dataLink.newDataNativeQuery();
        query.setApplyDBFilter(false);
        query.select(select)
                .from(entityExpr)
                .where(whereFilter)
                .groupBy(groupBy)
                .orderBy(orderBy);

        sqlSentence = query.createQuerySentence();
        LOGGER.debug(sqlSentence);
        afterCreateSentence();
    }
    
    protected void afterCreateSentence(){
        // Implementar en clases heredadas.
    }

    /**
     * Genera la sentencia sql a partir de los parámetros asignados
     * (columnsGroup1, columnsGroup2, columns, entityRoot, entitiesToJoin,
     * entitiesAlias, whereFilter, orderBy, groupBy), el valor resultante asigna
     * en el atributo sqlSentence y en el objeto query que luego será utilizado
     * en executeSqlSentence.
     *
     * @return instancia de un objeto IDataNativeQuery
     */
    public IDataNativeQuery createDataNativeQuery() {
        IDataNativeQuery queryResult;
        setProperties();
        String select = iif(!isNullorEmpty(columnsGroup1), columnsGroup1 + ",", "")
                + iif(!isNullorEmpty(columnsGroup2), columnsGroup2 + ",", "")
                + iif(!isNullorEmpty(columnsGroup3), columnsGroup3 + ",", "")
                + iif(!isNullorEmpty(columns), columns, "");

        EntitiesToRelation entityListRelations = new EntitiesToRelation();
        String sentenceSearch = select + " " + whereFilter.getSentence() + " " + orderBy;
        String entityExpr = entityListRelations.get(entityRoot, entitiesToJoin,
                entitiesAlias, sentenceSearch);

        queryResult = dataLink.newDataNativeQuery();
        queryResult.setApplyDBFilter(false);        
        queryResult.select(select)
                .from(entityExpr)
                .where(whereFilter)
                .groupBy(groupBy)
                .orderBy(orderBy);

        queryResult.createQuerySentence();
        LOGGER.debug(queryResult.getQuerySentence());
        return queryResult;
    }


    protected void beforeExecuteSqlSentence(){
        // Implementar en clases heredadas
    }
    
    /**
     * Ejecuta la sentencia sql generada previamente en el metodo
     * createSqlSentence.
     *
     * @return el resultado de la ejecución de la sentencia sql.
     * @throws Exception
     */
    public List<IDataQueryModel> executeSqlSentence() throws Exception {
        beforeExecuteSqlSentence(); 
        List<IDataQueryModel> result = query.execQuery();
        result = afterExecuteSqlSentence(result);
        return result;
    }
    
    protected List<IDataQueryModel> afterExecuteSqlSentence(List<IDataQueryModel> result){
        return result;
    }
    

    /**
     * Se encarga de generar la lista de entidades de donde se buscarán extraer
     * la información deseada.
     */
    class EntitiesToRelation {

        String entityList = "";
        String track = "";

        private String get(String mainEntity, String entities,
                String entityAlias, String sentence) {
            this.entityList = entities;
            return get(mainEntity, entityAlias, sentence, 0, "");

        }

        private String get(String mainEntity, String entityAlias, String sentence,
                int level, String processed) {
            if (level == 0) {
                track = "";
            }
            if (level == 10) {
                return entityList;
            }
            if (isNullorEmpty(processed)) {
                processed = "";
            }
            mainEntity = mainEntity.toLowerCase().trim();
            sentence = sentence.toLowerCase();
            track += mainEntity + ",";
            if (!isNullorEmpty(entityAlias)) {
                // Generar expresiones separadas por coma
                List<String> tokenList = convertToList(entityAlias, ",");
                for (String token : tokenList) {
                    // Determinar valor de la entidad y del alias
                    String entity = left(token, token.indexOf(' ')).toLowerCase().trim();
                    String alias = substring(token, token.indexOf(' ') + 1).toLowerCase().trim();
                    // Ver si existe el alias en la expresion de sentencia
                    if (inString(",( ", alias, ".", sentence)) {
                        // Agregar a la lista de entidades si no se encuentra ya
                        if (!inString(",( ", entity, "., *)", this.entityList)) {
                            this.entityList
                                    += iif(!this.entityList.isEmpty(), ", ", "") + entity + " " + alias;
                        }
                    }
                }
            }
            if (level == 0 && isNullorEmpty(entityList)) {
                entityList = mainEntity;
            }
            try {
                // Buscar las relaciones 
                EntitiesRelation entityRela = new EntitiesRelation();
                List<IDataQueryModel> data;
                if (level > 0) {
                    data = entityRela.get(mainEntity, "*-1");
                } else {
                    data = entityRela.get(mainEntity, "");
                }
                String entity;
                // Procesar primero el nivel actual
                for (IDataQueryModel row : data) {
                    entity = ((String) row.getColumn("entity")).toLowerCase().trim();
                    String xml = "<TABLA>" + entity + "</TABLA>";
                    // Determinar si ya fue procesado anteriormente
                    if (findString(entity + ",", track) >= 0 || (occurs(xml, processed) > 0)) {
                        continue;
                    }
                    // Si no fue procesado antes (no esta en entityList ni en processed)
                    if (inString(",( ", entity, ".", sentence)) {
                        if (!inString(", ", entity, ", *)", entityList)
                                && occurs(xml, processed) == 0) {
                            entityList += "," + entity;
                        }
                    }
                }
                boolean recorrer;
                boolean incluirEntity;
                for (IDataQueryModel row : data) {
                    entity = ((String) row.getColumn("entity")).toLowerCase().trim();
                    incluirEntity = true;
                    recorrer = true;
                    String xml = "<TABLA>" + entity + "</TABLA>";
                    // Determinar si ya fue procesado anteriormente
                    if (findString(entity + ",", track) >= 0 || (occurs(xml, processed) > 0)) {
                        recorrer = false;
                    }
                    // Si no fue procesado antes (no esta en entityList ni en processed)
                    if (inString(",( ", entity, ".", sentence)) {
                        if (!inString(", ", entity, ", *)", entityList)
                                && occurs(xml, processed) == 0) {
                            incluirEntity = false;
                        }
                    }
                    int lenLista = entityList.length();
                    // Si es una relacion a si misma o ya se proceso esta entidad
                    if (!mainEntity.equals(entity) && recorrer) {
                        this.get(entity, "", sentence, level + 1, processed + xml);
                    }
                    // Si existe una referencia a una tabla asociada y no sido incluido la tabla actual
                    if (lenLista != entityList.length() && incluirEntity) {
                        if (!inString(", ", entity, ", *", entityList)) {
                            entityList = left(entityList, lenLista) + ", " + entity + "," + substr(entityList, lenLista + 1);
                        }
                    }
                }
            } catch (Exception exp) {
                ErrorManager.showError(exp, LOGGER);
            }
            return entityList;
        }
    }

    /**
     * Se encarga de generar la instrucción que relaciona las entidades para
     * formar la sentencia sql completa.
     */
    class EntitiesRelation {

        List<IDataQueryModel> get(String entity, String relationType) throws Exception {
            Map<String, Object> params = new HashMap();
            params.put("entity", entity);
            List<IDataQueryModel> result = new ArrayList();
            if (isNullorEmpty(relationType) || "*-1".equals(relationType)) {
                String select1;
                select1 = "select entityPK as entity,"
                        + " fieldsPK as expr1,"
                        + " fieldsFK as expr2 "
                        + " from AppTablesRelation "
                        + " where entityFK = :entity"
                        + " and included = true";
                List list = 
                        getDataLink().getDao().findListByQuery(null,select1, params);
                result.addAll(DataQueryModel.convertToDataQueryModel(list, "entity, expr1, expr2"));
            } 
            if (isNullorEmpty(relationType) || "1-*".equals(relationType)) {
                String select2;                
                select2 = "select entityFK as entity,"
                        + " fieldsFK as expr1,"
                        + " fieldsPK as expr2 "
                        + " from AppTablesRelation "
                        + " where entityPK = :entity"
                        + " and included = true";
                List list = 
                        getDataLink().getDao().findListByQuery(null,select2, params);
                result.addAll(DataQueryModel.convertToDataQueryModel(list, "entity, expr1, expr2"));
            }
            return result;
        }
    }
    

    public final String createGroupBy(String columns) {
        List<String> campos = stringToList(columns);
        String[] groupFunction = {"sum(", "count(", "max(", "min(", "avg("};
        String fieldsGroupBy = "";
        boolean existGroupFunction;
        for (String campo : campos) {
            existGroupFunction = false;
            for (String element : groupFunction) {
                if (campo.toLowerCase().contains(element)) {
                    existGroupFunction = true;
                    break;
                }
            }
            if (!existGroupFunction && !campo.contains("0.00000") && !"".equals(campo)) {
                if (campo.toLowerCase().contains(" as ")) {
                    fieldsGroupBy = fieldsGroupBy + ("".equals(fieldsGroupBy) ? "" : ",") + substring(campo, 0, campo.toLowerCase().indexOf(" as "));
                } else {
                    fieldsGroupBy = fieldsGroupBy + ("".equals(fieldsGroupBy) ? "" : ",") + campo;
                }
            }
        }
        return fieldsGroupBy;
    }
    
}
