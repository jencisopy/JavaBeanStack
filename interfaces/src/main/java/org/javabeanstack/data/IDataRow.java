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
import org.javabeanstack.error.IErrorReg;
import java.util.Map;

/**
 *
 * @author Jorge Enciso
 */
public interface IDataRow extends Serializable{
    public static final int AGREGAR   = 1;
    public static final int MODIFICAR = 2;
    public static final int BORRAR    = 3;
    public static final int CONSULTAR = 4;

    public static final int INSERT   = 1;
    public static final int UPDATE   = 2;
    public static final int DELETE   = 3;
    public static final int READ     = 4;
    
    public Object    clone();
    public int       getAction();
    public String    getQueryUK();
    public String    getIdFunctionFind();    
    public boolean   isRowChecked();
    public void      setRowChecked(boolean rowchecked);    
    public boolean   isFieldChecked(String fieldName);
    public Map       getFieldChecked();    
    public void      setFieldChecked(Map fieldChecked);        
    public void      setFieldChecked(String fieldName, boolean fieldChecked);    
    public Map<String, IErrorReg> getErrors();
    public Object    getId();    
    public Object    getRowkey();
    public Object    getValue(String fieldname);
    public Class     getFieldType(String fieldname);
    public void      setValue(String fieldname, Object value);
    public void      setAction(int action);    
    public void      setErrors(Map<String, IErrorReg> errorReg);        
    public void      setErrors(IErrorReg errorReg, String fieldName);    
    public void      setErrors(String errorMsg, String fieldname, int errorNumber);
    public boolean   delete();
    public boolean   equivalent(Object o);
    public boolean   isApplyDBFilter();
    public boolean   checkFieldIdcompany(Long idcompany);
}
