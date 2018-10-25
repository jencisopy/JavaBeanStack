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
package org.javabeanstack.services;

import java.util.List;
import java.util.Map;
import org.javabeanstack.data.IDataResult;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.data.IGenericDAO;
import org.javabeanstack.error.IErrorReg;

/**
 *
 * @author Jorge Enciso
 */
public interface IDataService extends IGenericDAO{
    <T extends IDataRow> T setFieldsToCheck(T row); 
    <T extends IDataRow> boolean checkUniqueKey(String sessionId, T row) throws Exception;        
    <T extends IDataRow> boolean checkForeignKey(String sessionId, T row, String fieldName) throws Exception;            
    <T extends IDataRow> Map<String, IErrorReg> checkDataRow(String sessionId, T row);    
    <T extends IDataRow> IDataResult save(String sessionId, T row) throws Exception;
    <T extends IDataRow> List<T> getDataRows(String sessionId, Class<T> type, String order, String filter, Map<String, Object> params, int firstRow, int maxRows) throws Exception;
    <T extends IDataRow> String getSelectCmd(String sessionId, Class<T> type, String order, String filter);
}
