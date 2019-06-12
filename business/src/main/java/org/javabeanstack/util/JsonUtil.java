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

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.json.JsonBuilderFactory;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import org.javabeanstack.data.IDataQueryModel;

/**
 *
 * @author Jorge Enciso
 */
public class JsonUtil {
    public static <T extends IDataQueryModel> JsonArray create(List<T> data) {
        if (data == null){
            return null;
        }
        Map<String, Object> config = new HashMap();
        config.put("javax.json.stream.JsonGenerator.prettyPrinting", true);
        JsonBuilderFactory factory = Json.createBuilderFactory(config);
        JsonArrayBuilder builder = factory.createArrayBuilder();
        data.forEach((row) -> {
            for (String columnName : row.getColumnList()) {
                Object value = row.getColumn(columnName);
                Class<?> classMember = null;
                JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
                if (row.getColumn(columnName) != null){
                    classMember = row.getColumn(columnName).getClass();
                }
                if (classMember == null || value == null) {
                    objectBuilder.add(columnName, JsonValue.NULL);
                } else if (classMember.getSimpleName().equals("String")) {
                    objectBuilder.add(columnName, ((String)value).trim());
                } else if (classMember.getSimpleName().equals("BigDecimal")) {
                    objectBuilder.add(columnName, (BigDecimal)value);
                } else if (classMember.getSimpleName().equals("Long")) {
                    objectBuilder.add(columnName, (Long)value);
                } else if (classMember.getSimpleName().equals("Date") 
                        || classMember.getSimpleName().equals("Timestamp") 
                        || classMember.getSimpleName().equals("LocalDateTime")) {
                    value = Dates.toString((Date) row.getColumn(columnName), "yyyy-MM-dd'T'HH:mm:ss");                    
                    objectBuilder.add(columnName, (String)value);
                } else if (classMember.getSimpleName().equals("Short")) {
                    objectBuilder.add(columnName, (Short)value);
                } else if (classMember.getSimpleName().equals("Integer")) {
                    objectBuilder.add(columnName, (Integer)value);
                } else if (classMember.getSimpleName().equals("Character")) {
                    objectBuilder.add(columnName, (Character)value);
                } else if (classMember.getSimpleName().equals("Boolean")) {
                    objectBuilder.add(columnName, (Boolean)value);
                } else {
                    objectBuilder.add(columnName, value.toString());
                }
                builder.add(objectBuilder.build());
            }
        });
        return builder.build();
    }
}
