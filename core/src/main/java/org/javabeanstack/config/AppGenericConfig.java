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
@Lock(LockType.READ)
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
