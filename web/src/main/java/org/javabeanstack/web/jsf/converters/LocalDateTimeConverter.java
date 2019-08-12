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

/**
 * Convertidor de fecha local a string y vice versa
 * @author mtrinidad
 */

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import org.apache.commons.lang.StringUtils;
import org.javabeanstack.util.LocalDates;
import org.primefaces.component.calendar.Calendar;

@ApplicationScoped
@FacesConverter("localDateTimeConverter")
public class LocalDateTimeConverter implements Converter
{
    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value)
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(extractPattern(component, context));
        try
        {
            String sTime="00:00:00";
            if (value.length()>10){
                sTime=value.substring(11);
            }
            LocalDate dateLocal = LocalDate.parse(value, formatter);
            return LocalDates.toDateTime(dateLocal.toString()+"T"+sTime,"yyyy-MM-dd'T'HH:mm:ss");
        }
        catch (Exception e)
        {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value)
    {
        if (value == null || (value instanceof String && StringUtils.isBlank((String) value)))
        {
            return "";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(extractPattern(component, context));
        return formatter.format((LocalDateTime) value);
    }

    private String extractPattern(UIComponent component, FacesContext context)
    {
        // try to get infos from calendar component
        if (component instanceof Calendar)
        {
            Calendar calendarComponent = (Calendar) component;
            return calendarComponent.getPattern();
        }
        return null;
    }
}