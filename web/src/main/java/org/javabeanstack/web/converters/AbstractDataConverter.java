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

package org.javabeanstack.web.converters;

import java.io.Serializable;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import org.apache.log4j.Logger;

import org.javabeanstack.data.IDataLink;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.security.IUserSession;
import org.javabeanstack.error.ErrorManager;

/**
 *
 * @author Jorge Enciso
 * @param <T>
 */
public abstract class AbstractDataConverter<T extends IDataRow> implements Converter, Serializable   {
    private static final Logger LOGGER = Logger.getLogger(AbstractDataConverter.class);
    Class<T> clase;

    public AbstractDataConverter(){
    }
    
    public AbstractDataConverter(Class<T> clase){
        this.clase = clase;
    }
    
    public abstract IDataLink getDAO();
    
    
    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        LOGGER.debug(value);

        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            getDAO().setUserSession(getUserSession());
            T row = getDAO().find(clase,Long.parseLong(value));
            return row;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    
    @Override
    public String getAsString(FacesContext context, UIComponent component, Object object) {
        LOGGER.debug(object);

        if (object != null) {
            if (!(object instanceof String)) {
                String result = ((IDataRow) object).getId().toString();
                return result;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public IUserSession getUserSession() {
        IUserSession userSession = (IUserSession)FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("userSession");
        return userSession;
    }    
}
