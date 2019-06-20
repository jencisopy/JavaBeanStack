/*
* Copyright (c) 2015-2018 OyM System Group S.A.
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
package org.javabeanstack.web.jsf.converters;

/**
 *
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