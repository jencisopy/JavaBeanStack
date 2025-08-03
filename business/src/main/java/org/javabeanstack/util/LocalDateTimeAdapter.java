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
package org.javabeanstack.util;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDateTime;

/**
 * Convierte en objeto LocalDateTime a partir de un string
 * Convierte un string a formato LocalDateTime
 * @author Jorge Enciso
 */
public class LocalDateTimeAdapter extends XmlAdapter<String, LocalDateTime> {

    @Override
    public LocalDateTime unmarshal(String v) throws Exception {
        if (v == null){
            return null;
        }
        //yyyy-MM-dd'T'HH:mm:ss.SSS
        if (v.length() > 20){
            return LocalDates.toDateTime(Strings.left(v,19), "yyyy-MM-dd'T'HH:mm:ss");
        }
        //yyyy-MM-dd'T'HH:mm
        if (v.length() == 16){
            return LocalDates.toDateTime(v, "yyyy-MM-dd'T'HH:mm");
        }
        //Igual a yyyy-MM-dd
        if (v.length() == 10){
            v += "T00:00:00";
        }
        return LocalDates.toDateTime(v, "yyyy-MM-dd'T'HH:mm:ss");
    }

    @Override
    public String marshal(LocalDateTime v) throws Exception {
        if (v == null){
            return null;
        }
        return Strings.left(v.toString(),19);
    }
}
