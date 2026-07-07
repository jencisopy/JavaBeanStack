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

package org.javabeanstack.model;

import org.javabeanstack.data.IDataRow;
import org.w3c.dom.Document;


/**
 *
 * @author Jorge Enciso
 */
public interface IAppObjectAuth extends IDataRow{
    public static int DENIED = 1;
    public static int ALLOWED = 0;
    
    Long getIdAppObjectAuth();        
    void setIdAppObjectAuth(Long IdAppObjectAuth);        
    
    Long getIduser();    
    void setIduser(Long iduser);    

    Long getIdAppObject();    
    void setIdAppObject(Long idAppObject);    
    
    String getAuth();        
    void setAuth(String auth);   

    Integer getRead();        
    void setRead(Integer read);   

    Integer getWrite();        
    void setWrite(Integer write);   

    Integer getExecute();        
    void setExecute(Integer execute);   

    Integer getInsert();        
    void setInsert(Integer insert);   

    Integer getDelete();        
    void setDelete(Integer delete);   

    Integer getUpdate();        
    void setUpdate(Integer update);   
    
    Integer getConfirm();        
    void setConfirm(Integer confirm);   
    
    Integer getCancel();        
    void setCancel(Integer cancel);   
    
    Integer getAttach();        
    void setAttach(Integer attach);   
    
    Integer getCopyFrom();        
    void setCopyFrom(Integer copyFrom);   
    
    Document getAuthXmlDom();
    void setAuthXmlDom(Document xmlDom);
}
