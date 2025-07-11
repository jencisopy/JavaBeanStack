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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javabeanstack.data.IGenericDAO;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.model.IAppResource;
import org.javabeanstack.util.Fn;
import static org.javabeanstack.util.Strings.isNullorEmpty;
/**
 * Clase encargada de buscar un recurso xml en una base de datos o a 
 * partir de una path de un archivo dado.
 * 
 * @author Jorge Enciso
 * @param <V>
 */
public class XmlResourceSearcher<V> extends XmlSearcher<V> {

    private static final Logger LOGGER = LogManager.getLogger(XmlResourceSearcher.class);

    private IGenericDAO dao;

    public IGenericDAO getDao() {
        return dao;
    }

    public void setDao(IGenericDAO dao) {
        this.dao = dao;
    }
    
    /**
     * Si esta disponible la busqueda en un tipo de path determinado.
     *
     * @param pathType tipo de path (file, object, http)
     * @return verdadero si esta disponible, falso si no
     */
    @Override
    public boolean isSearchAvailable(String pathType) {
        if (super.isSearchAvailable(pathType)) {
            return true;
        }
        return Fn.inList(pathType.toUpperCase(), "OBJ", "OBJ:");
    }

    /**
     * Busca el texto xml en un archivo, tabla u otra ubicación del objeto.
     *
     * @param context contexto IXmlDom de este objeto
     * @param xmlPath ubicación del archivo
     * @return el texto xml.
     */
    @Override
    public String search(IXmlDom context, String xmlPath) {
        if (Fn.inList(getPathType(xmlPath), "file", "file:", "obj", "obj:")) {
            String queryString1 = "select o from AppResource o where url = :value";
            String queryString2 = "select o from AppResource o where name = :value and type = 'XML'";
            Map<String, Object> parameters = new HashMap();
            String xmlPath2 = XmlSearcher.getJustPath(xmlPath).toLowerCase();

            parameters.put("value", xmlPath2);
            IAppResource appXmlSource;
            try {
                appXmlSource
                        = dao.findByQuery(null, queryString1, parameters);
                if (appXmlSource == null){
                    appXmlSource
                        = dao.findByQuery(null, queryString2, parameters);
                }
                if (appXmlSource != null) {
                    if (appXmlSource.isValid()) {
                        return appXmlSource.getCompiled();
                    }
                    else if (!isNullorEmpty(appXmlSource.getSource())){
                        return appXmlSource.getSource();
                    }
                }
            } catch (Exception ex) {
                ErrorManager.showError(ex, LOGGER);
            }
        }
        if (context == null){
            return null;
        }        
        return super.search(context, xmlPath);
    }

    /**
     * Determina si existe el recurso solicitado 
     * 
     * @param xmlPath path del objeto.
     * @return verdadero si existe y falso si no.
     */
    @Override
    public boolean exist(String xmlPath) {
        if (Fn.inList(getPathType(xmlPath), "file", "file:", "obj", "obj:")) {
            String queryString1 = "select o from AppResource o where url = :value";
            String queryString2 = "select o from AppResource o where name = :value and type = 'XML'";
            Map<String, Object> parameters = new HashMap();
            String xmlPath2 = XmlSearcher.getJustPath(xmlPath).toLowerCase();

            parameters.put("value", xmlPath2);
            IAppResource appXmlSource;
            try {
                appXmlSource
                        = dao.findByQuery(null, queryString1, parameters);
                if (appXmlSource == null){
                    appXmlSource
                        = dao.findByQuery(null, queryString2, parameters);
                }
                return (appXmlSource != null);
            } catch (Exception ex) {
                ErrorManager.showError(ex, LOGGER);
            }
        }
        return super.exist(xmlPath);
    }    
}
