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
import javax.ejb.EJB;
import org.apache.log4j.Logger;
import org.javabeanstack.data.IGenericDAO;
import org.javabeanstack.data.IDBManager;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.model.IAppXmlSources;
import org.javabeanstack.util.Fn;

/**
 *
 * @author Jorge Enciso
 * @param <V>
 */
public class XmlFileObjectSearcher<V> extends XmlSearcher<V> {

    private static final Logger LOGGER = Logger.getLogger(XmlFileObjectSearcher.class);

    @EJB
    private IGenericDAO dao;

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
            String queryString = "select o from AppXmlSource o where xmlpath = :xmlpath";
            Map<String, Object> parameters = new HashMap();
            String xmlPath2 = XmlSearcher.getJustPath(xmlPath).toLowerCase();

            parameters.put("xmlpath", xmlPath2);
            IAppXmlSources appXmlSource;
            try {
                appXmlSource
                        = dao.findByQuery(IAppXmlSources.class,
                                            IDBManager.CATALOGO,
                                            queryString, parameters);
                if (appXmlSource != null && appXmlSource.isValid()) {
                    return appXmlSource.getXmlCompiled();
                }
            } catch (Exception ex) {
                ErrorManager.showError(ex, LOGGER);
            }
        }
        return super.search(context, xmlPath);
    }
}
