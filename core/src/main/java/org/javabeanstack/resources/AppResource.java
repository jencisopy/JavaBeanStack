/*
* Copyright (c) 2015-2017 OyM System Group S.A.
* Capitan Cristaldo 464, Asunción, Paraguay
* All rights reserved. 
*
* NOTICE:  All information contained herein is, and remains
* the property of OyM System Group S.A. and its suppliers,
* if any.  The intellectual and technical concepts contained
* herein are proprietary to OyM System Group S.A.
* and its suppliers and protected by trade secret or copyright law.
* Dissemination of this information or reproduction of this material
* is strictly forbidden unless prior written permission is obtained
* from OyM System Group S.A.
 */
package org.javabeanstack.resources;

import java.io.InputStream;
import java.util.Map;
import javax.ejb.EJB;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.javabeanstack.security.ISecManager;
import org.javabeanstack.security.IUserSession;
import org.javabeanstack.xml.IXmlDom;
import org.javabeanstack.xml.IXmlManager;
import org.javabeanstack.xml.XmlDomW3c;

/**
 *
 * @author Jorge Enciso
 */
public class AppResource implements IAppResource {

    @EJB private IXmlManager xmlManager;
    @EJB private ISecManager secManager;    

    @Override
    public InputStream getResourceAsStream(IUserSession userSession, String resourcePath) {
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String sessionId, String resourcePath) {
        return null;
    }

    @Override
    public IXmlDom<Document, Element> getResourceAsXmlDom(String sessionId,
            String resourcePath, String elementPath, Map<String, String> params) {
        if (!secManager.isSesionIdValid(sessionId)){
            return null;
        }
        IXmlDom xmlDom = new XmlDomW3c();
        xmlDom.setXmlSearcher(xmlManager.getXmlSearcher());
        boolean result = xmlDom.config(resourcePath, "", elementPath, false, params);
        if (!result){
            return null;
        }
        xmlDom.setXmlSearcher(null);
        return xmlDom;
    }
}
