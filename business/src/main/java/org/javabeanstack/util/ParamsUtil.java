/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2018 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
*        jenciso@javabeanstack.org
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

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.javabeanstack.data.IDataRow;

/**
 * 
 * @author Jorge Enciso
 */
public class ParamsUtil {

    /**
     * Pasa los datos que se encuentran en un objeto derivado de DataRow a un 
     * Map<String, Object>
     * @param <T>  tipo de objeto derivado de DataRow
     * @param source objeto DataRow
     * @return Map<String, Object> desde el origen
     * @throws IllegalArgumentException
     * @throws IllegalAccessException 
     */
    public static <T extends IDataRow> Map<String, Object> DataRowToMap(T source) throws IllegalArgumentException, IllegalAccessException {
        if (source == null) {
            return null;
        }
        Map<String, Object> params = new HashMap();
        Field[] fields = source.getClass().getDeclaredFields();
        //Recorrer atributos del origen para pasar al Map
        for (Field field : fields) {
            String fieldName = field.getName();
            Class clazz = source.getFieldType(fieldName);
            if (Number.class.isAssignableFrom(clazz) 
                    || clazz.isAssignableFrom(String.class) 
                    || clazz.isAssignableFrom(LocalDateTime.class) 
                    || clazz.isAssignableFrom(LocalDate.class) 
                    || clazz.isAssignableFrom(Date.class)
                    || clazz.isAssignableFrom(Boolean.class)){
                params.put(fieldName, source.getValue(field.getName()));                
            }
        }
        return params;
    }
}
