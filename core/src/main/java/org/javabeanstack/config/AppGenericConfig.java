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
import org.javabeanstack.data.IGenericDAO;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.model.IAppSystemParam;
import org.javabeanstack.xml.DomW3cParser;

/**
 * Esta clase implementa funcionalidades que permite a la app leer y grabar
 * configuraciones que determinará el comportamiento del sistema.
 * 
 * Se implementan dos tipos de almacenes de datos de configuración
 *
 * El 1ro es un Map<Clave,Valor> el cual se guarda objetos DOM conteniendo
 * información en formato xml.
 * 
 * El 2do. es un objeto "AppSystemParam" mapeado a una tabla de la base de datos
 * conteniendo información de configuración del sistema.
 * 
 * 
 * @author Jorge Enciso
 */
@Lock(LockType.READ)
public class AppGenericConfig implements IAppConfig {
    protected static final Logger LOGGER = Logger.getLogger(AppGenericConfig.class);

    @EJB
    protected IGenericDAO dao;

    /** 
     * En este atributo se guardan objetos DOM que son accedidos por una 
     * clave "groupkey". Estos objetos DOM contienen información sobre 
     * parámetros del sistema, configuración de conexión a los datos, 
     * parámetros de seguridad etc.
     */
    protected final Map<String, Document> config = new TreeMap(String.CASE_INSENSITIVE_ORDER);
    
    /**
     * Lee el objeto DOM de configuración guardado bajo una clave "groupkey"
     * @param groupKey  identificador del registro.
     * @return objeto DOM.
     */
    @Override
    public Document getConfigDOM(String groupKey){
        groupKey = groupKey.toUpperCase();
        Document dom = config.get(groupKey);
        return dom;
    }

    /**
     * Lee el valor de una propiedad que se encuentra bajo una clave "groupkey"
     * y un path dentro del objeto DOM.
     * 
     * @param property  propiedad del objeto DOM xml
     * @param groupKey  clave bajo la cual se encuentra el objeto DOM. 
     * @param nodePath  path del elemento donde se encuentra el property.
     * @return valor de la propiedad solicitada.
     */
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

    /**
     * Asigna un valor a una propiedad del objeto DOM de configuración que se
     * encuentra bajo una clave "groupkey" y un path al nodo donde se encuentra
     * la propiedad. 
     * @param value     valor a asignar.
     * @param property  nombre de la propiedad.
     * @param groupKey  clave con la cual se identifica al DOM xml.
     * @param nodePath  path dentro del objeto DOM donde se encuentra la propiedad.
     * @return verdadero si tuvo exito y falso si no.
     */
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

    /**
     * Lee de una tabla "appSystemParam" un registro mediante un identificador
     * 
     * @param id  identificador del registro.
     * @return el registro AppSystemParam solicitado 
     */
    @Override
    public IAppSystemParam getSystemParam(Long id) {
        IAppSystemParam appSystemParam;
        String queryString
                = "select o from AppSystemParam o where idsystemparam = " + id;
        try {
            appSystemParam
                    = dao.findByQuery(IAppSystemParam.class,
                            null,
                            queryString, null);
            return appSystemParam;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    /**
     * Lee de una tabla "appSystemParam" un registro utilizando el nombre
     * de un parámetro como identificador solicitado.
     * 
     * @param param  nombre del parametro.
     * @return registro AppSystemParam solicitado.
     */
    @Override
    public IAppSystemParam getSystemParam(String param) {
        String queryString
                = "select o from AppSystemParam o where LOWER(param) = '" + param.toLowerCase() + "'";
        IAppSystemParam appSystemParam;
        try {
            appSystemParam
                    = dao.findByQuery(IAppSystemParam.class,
                            null,
                            queryString, null);
            return appSystemParam;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    /**
     * Devuelve una lista conteniendo los registros de "appSystemParam"
     * @return lista de registros "AppSystemParam"
     */
    @Override
    public List<IAppSystemParam> getSystemParams() {
        String queryString
                = "select o from AppSystemParam o";
        try {
            List<IAppSystemParam> appSystemParams
                    = dao.findListByQuery(null, queryString, null);
            return appSystemParams;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }
}
