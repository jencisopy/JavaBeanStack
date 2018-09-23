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
import java.util.List;
import java.util.Map;
import org.javabeanstack.error.IErrorReg;

/**
 *
 * @author Jorge Enciso
 */
public interface IDataResult extends Serializable{
    public void put(String key, List<IDataRow> listEjb);
    public Map<String, List<IDataRow>> getMapResult();
    public List<IDataRow> getListEjb(String key);
    public Boolean isSuccessFul();
    public Boolean isRemoveDeleted();
    public void setSuccess(Boolean success);
    public void setRowsUpdated(IDataRow row);        
    public void setRowsUpdated(List<? extends IDataRow> rows);    
    public <T extends IDataRow> void setRowsUpdated(IDataSet dataSet);    
    public String getErrorMsg();
    public Map<String, IErrorReg> getErrorsMap();
    public Exception getException();
    public void setException(Exception ex);    
    public void setErrorMsg(String error);
    public void setErrorsMap(Map<String, IErrorReg> error);    
    public void setRemoveDeleted(Boolean remove);
    public <T extends IDataRow> T getRowResult();
}
