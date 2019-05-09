/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2018 Jorge Enciso
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

package org.javabeanstack.web.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;
import org.javabeanstack.resources.IAppResource;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.xml.IXmlDom;
import org.javabeanstack.xml.XmlDomW3c;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author Jorge Enciso
 */

public class AppResourceSearcher implements Serializable {
    @EJB
    private IAppResource appResource;
    
    private final FacesContextUtil facesCtx = new FacesContextUtil();

    public IXmlDom<Document, Element> getXmlDom(String resourcePath, 
                                                String elementPath, 
                                                Map<String, String> params) { 
        IXmlDom<Document, Element> xmlResource;
        xmlResource
                = appResource.getResourceAsXmlDom(getUserSession().getSessionId(),
                                                    resourcePath.trim().toLowerCase(), 
                                                    elementPath, params);

        if (xmlResource == null || xmlResource.getDom() == null) {
            // Buscar en el war carpeta /resource/
            //InputStream input = IOUtil.getResourceAsStream(getClass(), resourcePath);
            xmlResource = new XmlDomW3c();
            if (params == null){
                params = new HashMap();
            }
            params.put("encoding", "UTF-8");
            params.put("path", "/xml/");  // path alternativo de busqueda
            params.put("readFromJAR", "YES");
            xmlResource.config(resourcePath, "", elementPath, false, params);
        }
        return xmlResource;
    }

    public FacesContextUtil getFacesCtx() {
        return facesCtx;
    }

    
    protected IUserSession getUserSession() {
        return facesCtx.getUserSession();
    }
}
