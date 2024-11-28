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
package org.javabeanstack.web.jsf.converters;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import org.javabeanstack.data.IDataLink;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.error.ErrorManager;

/**
 *
 * @author Jorge Enciso
 * @param <T>
 */
public abstract class AbstractDataConverter<T extends IDataRow> implements Converter, Serializable {

    private static final Logger LOGGER = Logger.getLogger(AbstractDataConverter.class);
    Class<T> clase;

    public AbstractDataConverter() {
    }

    public AbstractDataConverter(Class<T> clase) {
        this.clase = clase;
    }

    public abstract IDataLink getDAO();

    public Class<T> getClase() {
        if (clase != null) {
            return clase;
        }
        return (Class<T>) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0];
    }

    /**
     * Convierte de un valor string a objeto
     *
     * @param context facecontext
     * @param component
     * @param value valor a convertir
     * @return string a objeto
     */
    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        LOGGER.debug(value);

        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            getDAO().setUserSession(getUserSession());
            if (!StringUtils.isNumeric(value)) {
                return null;
            }
            T row = getDAO().findById(getClase(), Long.valueOf(value));
            return row;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    /**
     * Convierte de un objeto a una variable string
     *
     * @param context facecontext
     * @param component
     * @param object instancia del objeto
     * @return un objeto a una variable string
     */
    @Override
    public String getAsString(FacesContext context, UIComponent component, Object object) {
        LOGGER.debug(object);
        if (object != null) {
            if (object instanceof String) {
                return (String) object;
            } else if ((object instanceof IDataRow) && ((IDataRow) object).getId() != null) {
                String result = ((IDataRow) object).getId().toString();
                return result;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Devuelve la variable de sesión del usuario logueado
     *
     * @return variable de sesión del usuario logueado
     */
    public IUserSession getUserSession() {
        IUserSession userSession = (IUserSession) FacesContext
                .getCurrentInstance()
                .getExternalContext()
                .getSessionMap()
                .get("userSession");
        return userSession;
    }
    
    protected Map<String, Object> getValuesFrom(String value) {
        int ini = value.indexOf("{") + 1;
        int fin = value.lastIndexOf("}") - 1;
        Map<String, Object> retornar = new HashMap();
        String[] proceso = value.substring(ini, fin).split(",");
        for (String proceso1 : proceso) {
            String[] elementos = proceso1.split(":");
            if (elementos[0].trim().startsWith("\"")) {
                elementos[0] = elementos[0].replace("\"", "");
            }
            if (elementos[1].trim().startsWith("\"")) {
                elementos[1] = elementos[1].trim().replace("\"", "");
                retornar.put(elementos[0].trim(), elementos[1]);
            } else {
                retornar.put(elementos[0].trim(), Long.valueOf(elementos[1]));
            }
        }
        return retornar;
    }
}
