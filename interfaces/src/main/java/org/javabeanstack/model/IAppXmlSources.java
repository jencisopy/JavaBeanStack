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
package org.javabeanstack.model;

import java.math.BigInteger;
import java.util.Date;
import org.javabeanstack.data.IDataRow;

/**
 *
 * @author Jorge Enciso
 */
public interface IAppXmlSources extends IDataRow {
    Long getIdXmlSource();
    Long getIdObject();
    String getXmlName();
    String getXmlSource();
    String getXmlCompiled();
    Date getProcessTime();
    BigInteger getReferencetime();
    String getXmlPath();
    boolean isValid();

    void setIdXmlSource(Long idxmlsource);    
    void setIdObject(Long idobject);  
    void setXmlName(String xmlname);
    void setXmlSource(String xmlsource);    
    void setXmlCompiled(String xmlcompile);    
    void setProcessTime(Date processtime);    
    void setReferencetime(BigInteger referencetime);
    void setXmlPath(String xmlpath);
}
