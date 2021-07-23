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


import java.util.List;
import java.util.Map;


/**
 * Esta interfase define funcionalidades para crear y ejecutar sentencias
 * select nativas sobre la base de datos.
 * 
 * @author Jorge Enciso
 */
public interface IDataNativeQuery {
    /**
     * Asigna las columnas que iran en la sentencia SELECT
     * @param columns Son las columnas de la sentencia select
     * @return objeto DataNativeQuery con los datos asignados
     */
    public IDataNativeQuery select(String columns);
    /**
     * Asigna ls lista de entidades que formarán la sentencia SELECT
     * @param entities  es la lista de entidades (tablas o vistas) forman
     *                  parte de from
     * @return objeto DataNativeQuery con los datos asignados
     */
    public IDataNativeQuery from(String entities);
    
    /**
     * Asigna lista de entidades que formarán la sentencia SELECT
     * @param subQuery  objeto IDataNativeQuery cuya sentencia es utilizada como
     * subquery dentro de la clausula FROM. 
     * @param alias alias del subquery
     * @return objeto DataNativeQuery con los datos asignados
     */
    public IDataNativeQuery from(IDataNativeQuery subQuery, String alias);

    /**
     * Asigna lista de entidades que formarán la sentencia SELECT
     * @param subQuery  subquery dentro de la clausula FROM. 
     * @param alias alias del subquery
     * @return objeto DataNativeQuery con los datos asignados
     */
    public IDataNativeQuery from(String subQuery, String alias);
    
    /**
     * Provee a la clase información para agregar una instrucción join a la sentencia
     * @param entity    es la entidad (tabla o vista)
     * @param joinExpr  expresión para unir las entidades.
     * @return objeto DataNativeQuery con los datos asignados
     */
    public IDataNativeQuery join(String entity, String joinExpr);
    
    /**
     * Provee a la clase información para agregar una instrucción join a la sentencia
     * @param subquery 
     * @param alias    alias del subquery
     * @param joinExpr  expresión para unir las entidades.
     * @return objeto DataNativeQuery con los datos asignados
     */
    public IDataNativeQuery join(IDataNativeQuery subquery, String alias, String joinExpr);
    
    /**
     * Provee a la clase información para agregar una instrucción join a la sentencia
     * @param entity    es la entidad (tabla o vista)
     * @param joinExpr  expresión para unir las entidades.
     * @return objeto DataNativeQuery con los datos asignados
     */
    public IDataNativeQuery leftJoin(String entity, String joinExpr);
    /**
     * Asigna a la sentencia los filtros en el where 
     * @param filterExpr  son los filtros 
     * @return objeto DataNativeQuery con los datos asignados
     */
    public IDataNativeQuery where(String filterExpr);
    /**
     * Asigna a la sentencia los filtros en el where 
     * @param filterExpr  son los filtros 
     * @param params  son los parametros del query
     * @return objeto DataNativeQuery con los datos asignados
     */
    public IDataNativeQuery where(String filterExpr, Map<String, Object> params);     
    /**
     * Asigna a la sentencia los filtros en el where 
     * @param dataExpr  son los filtros 
     * @return objeto DataNativeQuery con los datos asignados
     */
    public IDataNativeQuery where(IDataExpression dataExpr);    
    /**
     * Asigna a la sentencia los filtros en el where 
     * @param dataExpr  son los filtros 
     * @param params  son los parametros del query
     * @return objeto DataNativeQuery con los datos asignados
     */
    public IDataNativeQuery where(IDataExpression dataExpr, Map<String, Object> params);
    /**
     * Asigna una lista de columnas de ordenamiento a la sentencia SELECT 
     * @param columnOrder   lista de columnas por la que se va a ordenar
     *                      el select 
     * @return objeto DataNativeQuery con los datos asignados
     */
    public IDataNativeQuery orderBy(String columnOrder);
    /**
     * Asigna una lista de columnas al GROUP BY 
     * @param columnGroup   lista de columnas group by
     * @return objeto DataNativeQuery con los datos asignados
     */
    public IDataNativeQuery groupBy(String columnGroup);
    /**
     * Es la expresión del filtro del HAVING en la sentencia SELECT
     * @param filterExpr  filtros having 
     * @return objeto DataNativeQuery con los datos asignados
     */
    public IDataNativeQuery having(String filterExpr);
    /**
     * Agrega valores a los parámetros de la sentencia
     * @param key       nombre del parámetro
     * @param value     valor del parámetro
     * @return objeto DataNativeQuery con los datos asignados
     */
    public IDataNativeQuery addParam(String key, Object value);    
    /**
     * Asigna los parámetros de la sentencia
     * 
     * @param params    Map conteniendo los parámetros
     * @return objeto DataNativeQuery con los datos asignados
     */
    public IDataNativeQuery addParams(Map<String, Object> params); 
    /**
     * Asigna los parámetros de la sentencia
     * 
     * @param params    Map conteniendo los parámetros
     * @return objeto DataNativeQuery con los datos asignados
     */
    public IDataNativeQuery setQueryParams(Map<String, Object> params);
    /**
     *  Crear la sentencia select a partir de todas los parámetros previos recibidos
     */
    public void createQuery();
    /**
     *  Crear la sentencia select a partir de todas los parámetros previos recibidos
     * @return devuelve la sentencia
     */
    public String createQuerySentence();
    
    /**
     * Ejecuta la sentencia y devuelve una lista de registros
     * @return devuelve una lista de registros
     * @throws java.lang.Exception (SessionError)
     */
    public List<IDataQueryModel> execQuery() throws Exception;    
    /**
     * Ejecuta la sentencia y devuelve una lista de registros
     * @param dataLink  objeto acceso a los datos.
     * @return devuelve una lista de registros
     * @throws java.lang.Exception (SessionError)
     */
    public List<IDataQueryModel> execQuery(IDataLink dataLink)  throws Exception;
    /**
     * Ejecuta la sentencia y devuelve una lista de registros
     * @param first
     * @param maxResult
     * @return devuelve una lista de registros
     * @throws java.lang.Exception (SessionError)
     */
    public List<IDataQueryModel> execQuery(int first, int maxResult) throws Exception;    

    /**
     * Ejecuta la sentencia y devuelve un registro
     * @return devuelve un registro.
     * @throws java.lang.Exception (SessionError)
     */
    public IDataQueryModel execQuerySingle() throws Exception;        
    /**
     * Devuelve la sentencia SELECT resultante de los parámetros recibidos.
     * @return la sentencia resultante de los parámetros recibidos.
     */
    public String   getQuerySentence();
    /**
     * Devuelve las columnas (porción select) del la sentencia
     * @return  retorna las columnas (porción select) del la sentencia
     */
    public String   getColumnExpr();
    /**
     * Lista de columnas
     * @return lista de columnas
     */
    public String[] getColumnList();
    /**
     * Expresión con la lista de entidades (tablas y/o vistas)
     * @return entidades (tablas y/o vistas)
     */
    public String   getEntityExpr();
    /**
     * Lista de entidades (tablas y/o vistas)
     * @return entidades (tablas y/o vistas)
     */
    public String[] getEntityList();
    /**
     * Expresión FROM de la sentencia
     * @return expresión FROM.
     */
    public String   getFromExpr();
    
    /**
     * Devuelve la expresión where
     * @return filtros (expresión where)
     */
    
    public String   getFilterExpr();
    /**
     * Devuelve la lista de filtros de la expresión where
     * @return lista de filtros (expresión where)
     */
    public String[] getFilterExprList();    
    /**
     * Devuelve la expresion ORDER BY
     * @return expresión order by
     */
    public String   getOrderExpr();
    /**
     * Devuelve la lista de columnas del ORDER BY
     * @return lista de columnas order by
     */
    public String[] getOrderList();
    /**
     * Devuelve la expresion GROUP BY
     * @return expresión group by
     */
    public String   getGroupExpr();
    /**
     * Devuelve la lista de columnas que forma la expresion GROUP BY
     * @return lista de columnas del group by
     */
    public String[] getGroupList();

    /**
     * Devuelve la expresión HAVING
     * @return expresión having 
     */
    public String   getFilterGroupExpr();
    /**
     * Devuelve la lista de filtros en la expresion having
     * @return lista de filtros en la expresion having
     */
    public String[] getFilterGroupExprList();
    /**
     * Devuelve un Map con los parametros ingresados en la expresión
     * @return parámetros
     */
    public Map<String,Object> getQueryParams();
    /**
     * Devuelve el objeto para acceso a datos.
     * @return devuelve el objeto para acceso a datos.
     */
    public IDataLink getDataLink();    
    /**
     * Asigna el objeto de acceso a los datos.
     * @param dao objeto para acceder a los datos.
     */
    public void setDataLink(IDataLink dao);    
    /**
     * Retorna la cantidad de registros que resultaria de ejecutar la sentencia SELECT generada por esta clase.
     * 
     * @return cantidad de registros.
     * @throws java.lang.Exception 
     */
    public Long getCount() throws Exception; 
   
    public boolean getApplyDBFilter();
    
    public void setApplyDBFilter(boolean apply);
}
