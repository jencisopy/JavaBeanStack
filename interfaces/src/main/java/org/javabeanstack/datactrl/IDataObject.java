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
package org.javabeanstack.datactrl;

import org.javabeanstack.error.IErrorReg;
import java.util.List;
import java.util.Map;
import org.javabeanstack.data.IDataLink;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.data.IDataSet;
import org.javabeanstack.events.IDataEvents;

/**
 *
 * @author Jorge Enciso
 * @param <T> 
  */
public interface IDataObject <T extends IDataRow>{
    public boolean      isReadwrite();
    public IDataLink    getDAO();
    public IDataLink    getDAOCatalog();
   
    public Class<T>     getType();
    public String       getFilter();
    public String       getFilterExtra();     
    public String       getOrder();
    public Exception    getErrorApp();
    public String       getErrorMsg(boolean all);
    public String       getErrorMsg(String fieldName);
    public List<T>      getDataRows();
    public Map<Integer, T> getDataRowsChanged();    
    public T            getRow();
    public String       getSelectCmd();
    public String       getLastQuery();    
    public int          getRecno();
    public int          getFirstRow();
    public int          getMaxRows();    
    public int          getRowCount();
    public int          getRecStatus();
    public Long         getIdcompany();
    public Long         getIdempresa();    
    public IDataEvents  getDataEvents();        
    public boolean      isShowDeletedRow();
    public void         setShowDeletedRow(boolean showDeletedRow);
    public void         setReadWrite(boolean readWrite);
    public void         setType(Class<T> type);    
    public void         setFilter(String filter);
    public void         setOrder(String order);
    public void         setFirstRow(int first);
    public void         setMaxRows(int maxrow);    
    public boolean      open();
    public boolean      open(String order, String filter,boolean readwrite, int maxrows);
    public boolean      requery();
    public boolean      requery(String filterExtra, Map<String, Object> filterParams);    
    public boolean      goTo(int rownumber);
    public boolean      goTo(int rownumber, int offset);
    public boolean      moveFirst();
    public boolean      moveNext();
    public boolean      movePreviews();
    public boolean      moveLast();
    public boolean      find(String field, Object value, int begin, int end);
    public boolean      find(String field, Object value);
    public boolean      findNext();
    public boolean      isEof();
    public Object       getNewValue(String fieldname);
    public Object       getFieldDefaultValue(String fieldname);
    public Object       getField(String fieldname);
    public Object       getField(String objname, String fieldname);
    public Object       getFieldOld(String fieldname);
    public IDataRow     getFieldObjFK(String fieldname);
    public Map<String, Object> getFilterParams(); 
    public boolean      setField(String fieldname, Object value);
    public boolean      setField(String fieldname, Map<String, Object> param);    
    public boolean      setField(String fieldname, Object value, boolean noAfterSetField);
    public void         setFilterExtra(String filterExtra);     
    public void         setFilterParams(Map<String, Object> filterParams); 
    public boolean      isFieldExist(String fieldname);
    public boolean      isForeingKey(String fieldname);
    public boolean      isOpen();
    public boolean      allowOperation(int operation);
    public Object       getPrimaryKeyValue();
    public boolean      setPrimaryKeyValue(Object value);
    public boolean      refreshRow();
    public boolean      insertRow();
    public boolean      insertRowFrom();
    public boolean      deleteRow();
    public void         copyFrom(String idcompany, String companyName, String xmlTag, String tableCopy);
    public boolean      isExists();
    public Map<String, IErrorReg> checkDataRow() throws Exception;
    public boolean      checkData(boolean allRows);   
    public boolean      update(boolean allRows);
    public boolean      revert(Boolean allRows);
    public boolean      update(IDataSet dataSet);
    public boolean      revert(IDataSet dataSet);
    public boolean      revert();
    public void         close();
}
