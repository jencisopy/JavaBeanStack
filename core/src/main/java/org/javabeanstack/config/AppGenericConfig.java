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
package org.javabeanstack.config;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.javabeanstack.data.IDBManager;
import org.javabeanstack.data.IGenericDAO;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.model.IAppSystemParam;
import org.javabeanstack.xml.DomW3cParser;

/**
 *
 * @author Jorge Enciso
 */
public class AppGenericConfig implements IAppConfig {
    protected static final Logger LOGGER = Logger.getLogger(AppGenericConfig.class);

    @EJB
    protected IGenericDAO dao;

    protected final Map<String, Document> config = new TreeMap(String.CASE_INSENSITIVE_ORDER);
    
    @Override
    public Document getConfigDOM(String groupKey){
        groupKey = groupKey.toUpperCase();
        Document dom = config.get(groupKey);
        return dom;
    }

    @Override
    public String getProperty(String property, String groupKey, String nodePath) {
        groupKey = groupKey.toUpperCase();
        Document dom = config.get(groupKey);
        if (dom == null) {
            return null;
        }
        String propValue;
        try {
            propValue = DomW3cParser.getPropertyValue(dom, property, nodePath);
        } catch (Exception ex) {
            propValue = null;
        }
        return propValue;
    }

    @Override
    @Lock(LockType.WRITE)
    public boolean setProperty(String value, String property, String groupKey, String nodePath) {
        groupKey = groupKey.toUpperCase();
        Document dom = config.get(groupKey);
        if (dom == null) {
            return false;
        }
        boolean result = true;
        try {
            result = DomW3cParser.setPropertyValue(dom, value, property, nodePath);
        } catch (Exception ex) {
            result = false;
        }
        return result;
    }

    @Override
    public IAppSystemParam getSystemParam(Long id) {
        IAppSystemParam appSystemParam;
        String queryString
                = "select o from AppSystemParam o where idsystemparam = " + id;
        try {
            appSystemParam
                    = dao.findByQuery(IAppSystemParam.class,
                            IDBManager.CATALOGO,
                            queryString, null);
            return appSystemParam;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    @Override
    public IAppSystemParam getSystemParam(String param) {
        String queryString
                = "select o from AppSystemParam o where LOWER(param) = '" + param.toLowerCase() + "'";
        IAppSystemParam appSystemParam;
        try {
            appSystemParam
                    = dao.findByQuery(IAppSystemParam.class,
                            IDBManager.CATALOGO,
                            queryString, null);
            return appSystemParam;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    @Override
    public List<IAppSystemParam> getSystemParams() {
        String queryString
                = "select o from AppSystemParam o";
        try {
            List<IAppSystemParam> appSystemParams
                    = dao.findListByQuery(IDBManager.CATALOGO, queryString, null);
            return appSystemParams;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }
}
