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
package org.javabeanstack.xml;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.w3c.dom.Document;
import org.javabeanstack.data.IGenericDAO;

/**
 *
 * @author Jorge Enciso
 * 
 */
//@Singleton
@Startup
@Lock(LockType.WRITE)
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class XmlManager implements IXmlManager {
    private static final Logger LOGGER = LogManager.getLogger(XmlManager.class);
    
    @EJB
    private IGenericDAO dao;
    
    private final IXmlSearcher<Document> xmlSearcher = new XmlResourceSearcher();
    
    private Map<String, IXmlCache> cache = new HashMap();

    @PostConstruct
    protected void init(){
        ((XmlResourceSearcher)xmlSearcher).setDao(dao);
    }
            
    @Override
    @Lock(LockType.READ)
    public IXmlSearcher<Document> getXmlSearcher() {
        LOGGER.debug(Thread.currentThread().getId());
        return xmlSearcher;
    }

    @Override
    @Lock(LockType.READ)    
    public Map<String, IXmlCache> getCache() {
        return cache;
    }

    @Override
    public void setCache(Map<String, IXmlCache> cache) {
        this.cache = cache;
    }

    @Override
    public void addToCache(String key, IXmlCache value) {
        this.cache.put(key, value);
    }

    @Override
    public void removeFromCache(String key) {
        this.cache.remove(key);
    }

    @Override
    public void clearCache() {
        this.cache.clear();
    }

    @Override
    public void processObjects() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void purgeObjects() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
