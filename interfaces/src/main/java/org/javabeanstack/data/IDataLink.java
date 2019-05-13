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
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.data.services.IDataService;

/**
 * Esta interface abstrae funcionalidades que permitiran implementar clases que ejecuten sentencias
 * nativas sobre la base de datos.
 * 
 * @author Jorge Enciso
 */

public interface IDataLink  {
    IGenericDAO getDao();
    <T extends IDataService> T getDataService();
    
    String getPersistUnit();    
    Long getIdCompany();
    IUserSession getUserSession();
    Map<String, Object> getEntityManagerProp();
    Map<String, Object> getPersistUnitProp();
    IDataNativeQuery newDataNativeQuery();
    
    <T extends IDataRow> T find(Class<T> entityClass, Object id) throws Exception;    
    <T extends IDataRow> T findById(Class<T> entityClass, Object id) throws Exception;
    <T extends IDataRow> T findByUk(T ejb) throws Exception;    
    List<Object> findByNativeQuery(String queryString, Map<String, Object> parameters) throws Exception;
    List<Object> findByNativeQuery(String queryString, Map<String, Object> parameters, int first, int max) throws Exception;
    <T extends IDataRow> T findByNamedQuery(String namedQuery, Map<String, Object> parameters) throws Exception;
    <T extends IDataRow> T findByQuery(String queryString, Map<String, Object> parameters) throws Exception;
    <T extends IDataRow> List<T> findListByNamedQuery(String namedQuery, Map<String, Object> parameters) throws Exception;
    <T extends IDataRow> List<T> findListByNamedQuery(String namedQuery, Map<String, Object> parameters, int first, int max) throws Exception;
    <T extends IDataRow> List<T> findListByQuery(String queryString, Map<String, Object> parameters) throws Exception;
    <T extends IDataRow> List<T> findListByQuery(String queryString, Map<String, Object> parameters, int first, int max) throws Exception;

    <T extends IDataRow> T refreshRow(T row) throws Exception;
    <T extends IDataRow> IDataResult update(T ejb) throws Exception;
    <T extends IDataRow> IDataResult update(List<T> ejbs) throws Exception;
    IDataResult update(IDataSet dataSet) throws Exception;    
    
    <T extends IDataRow> IDataResult persist(T ejb) throws Exception;    
    <T extends IDataRow> IDataResult persist(List<T> ejbs) throws Exception;
    <T extends IDataRow> IDataResult merge(T ejb) throws Exception;    
    <T extends IDataRow> IDataResult merge(List<T> ejbs) throws Exception;
    <T extends IDataRow> IDataResult remove(T ejb) throws Exception;    
    <T extends IDataRow> IDataResult remove(List<T> ejbs) throws Exception;

    Long getCount(String queryString, Map<String, Object> parameters) throws Exception;
    Long getCount2(String queryString, Map<String, Object> parameters) throws Exception;    
    void setUserSession(IUserSession userSession) throws Exception;
    <T extends IGenericDAO> void setDao(T dao);
    
    String getEntitiesRelation(String entities, String typeRela, String schema) throws Exception;
    String getToken();
    void setToken(String token);
    IDBLinkInfo getDBLinkInfo();
}
