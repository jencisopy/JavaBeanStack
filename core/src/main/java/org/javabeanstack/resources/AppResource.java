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

package org.javabeanstack.resources;

import java.util.Map;
import javax.ejb.EJB;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.javabeanstack.security.ISecManager;
import org.javabeanstack.security.IUserSession;
import org.javabeanstack.xml.IXmlDom;
import org.javabeanstack.xml.IXmlManager;
import org.javabeanstack.xml.XmlDomW3c;
import org.javabeanstack.xml.XmlResourceSearcher;
import static org.javabeanstack.util.Fn.nvl;


/**
 *
 * @author Jorge Enciso
 */
public class AppResource implements IAppResource {

    @EJB private IXmlManager xmlManager;
    @EJB private ISecManager secManager;    

    @Override
    public byte[] getResourceAsBytes(IUserSession userSession, String resourcePath) {
        if (resourcePath.toLowerCase().endsWith(".jrxml")){
            String text = nvl(getResourceAsJRXml(userSession.getSessionId(), resourcePath),"");
            return text.getBytes();
        }
        return null;
    }

    @Override
    public byte[] getResourceAsBytes(String sessionId, String resourcePath) {
        if (resourcePath.toLowerCase().endsWith(".jrxml")){
            String text = nvl(getResourceAsJRXml(sessionId, resourcePath),"");
            return text.getBytes();
        }
        return null;
    }

    @Override
    public IXmlDom<Document, Element> getResourceAsXmlDom(String sessionId,
            String resourcePath, String elementPath, Map<String, String> params) {
        if (!secManager.isSesionIdValid(sessionId)){
            return null;
        }
        IXmlDom xmlDom = new XmlDomW3c();
        if (!xmlManager.getXmlSearcher().exist(resourcePath)){
            return null;
        }
        xmlDom.setXmlSearcher(xmlManager.getXmlSearcher());
        boolean result = xmlDom.config(resourcePath, "", elementPath, false, params);
        if (!result){ 
            return null;
        }
        xmlDom.setXmlSearcher(null);
        return xmlDom;
    }
    
    protected String getResourceAsJRXml(String sessionId, String resourcePath) {
        if (!secManager.isSesionIdValid(sessionId)){
            return null;
        }
        if (!xmlManager.getXmlSearcher().exist(resourcePath)){
            return null;
        }
        XmlResourceSearcher searcher = (XmlResourceSearcher)xmlManager.getXmlSearcher();
        String jrxml = searcher.search(null, resourcePath);
        if (jrxml == null){
            return null;
        }
        return jrxml;
    }
}
