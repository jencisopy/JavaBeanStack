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
import java.time.LocalDateTime;
import org.javabeanstack.data.IDataRow;

/**
 *
 * @author Jorge Enciso
 */
public interface IAppResource extends IDataRow {
    Long getIdappresource();
    void setIdappresource(Long idappresource);    
    
    Long getIdparent();
    void setIdparent(Long idparent);

    Long getIdAppObject();
    void setIdAppObject(Long idobject);
    
    String getCode();    
    void setCode(String code);    
    
    String getName();
    void setName(String name);    
    
    String getUrl();    
    void setUrl(String url);    
    
    String getType();    
    void setType(String type);
    
    String getCharset();    
    void setCharset(String charset);
    
    String getSource();    
    void setSource(String source);    
    
    String getCompiled();
    void setCompiled(String compiled);    
    
    LocalDateTime getProcesstime();    
    void setProcesstime(LocalDateTime processtime);    
    
    LocalDateTime getLastreference();
    void setLastreference(LocalDateTime lastreference);    

    BigInteger getReferencetime();
    void setReferencetime(BigInteger referencetime);    

    String getChecksum();
    void setChecksum(String checkSum);
    
    String getAppuser();
    void setAppuser(String appuser);

    boolean isValid();
}
