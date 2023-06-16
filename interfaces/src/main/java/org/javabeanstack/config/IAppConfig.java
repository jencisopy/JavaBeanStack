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
package org.javabeanstack.config;

import java.util.List;
import org.javabeanstack.data.IDataResult;
import org.w3c.dom.Document;
import org.javabeanstack.model.IAppSystemParam;

/**
 *
 * @author Jorge Enciso
 */
public interface IAppConfig {
    IAppSystemParam getSystemParam(Long id);        
    IAppSystemParam getSystemParam(String param);    
    List<IAppSystemParam> getSystemParams();        
    Document getConfigDOM(String groupKey);
    String getProperty(String property, String groupKey, String nodePath);
    boolean setProperty(String value, String property, String groupKey, String nodePath);
    String getFileSystemPath(String sessionId);
    IDataResult setSystemParam(IAppSystemParam param) throws Exception;
    void setSystemParams(List<IAppSystemParam> params) throws Exception;
}
