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

package org.javabeanstack.error;

import java.io.Serializable;

/**
 *
 * @author Jorge Enciso
 */
public interface IErrorReg extends Serializable {
    public String getEntity();    
    public String getFieldName();
    public String[] getFieldNames();    
    public String getMessage();
    public Integer getErrorNumber();
    public Exception getException();    
    public String getIpRequest();
    public String getEvent();
    public String getLevel();
    public Object getInfo();

    
    
    public boolean isWarning();
    public void setEntity(String entity);
    public void setFieldName(String fieldName);
    public void setFieldNames(String[] fieldNames);    
    public void setMessage(String message);
    public void setErrorNumber(int errorNumber);
    public void setException(Exception exp);
    public void setWarning(boolean warning);
    public void setIpRequest(String ip);
    public void setEvent(String event);
    public void setLevel(String level);
    public void setInfo(Object info);
}
