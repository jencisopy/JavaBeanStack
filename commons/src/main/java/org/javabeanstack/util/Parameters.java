/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
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

import java.util.HashMap;
import java.util.Map;

/**
 *
 * Contenedor fluido de parámetros nombre→valor (patrón builder) para pasar
 * parámetros a consultas y servicios.
 *
 * @author Jorge Enciso
 */
public class Parameters {
    private final Map<String, Object> params = new HashMap();
            
    /**
     * Agrega un parámetro y devuelve esta instancia (invocación fluida).
     *
     * @param key clave del parámetro.
     * @param value valor del parámetro.
     * @return esta instancia.
     */
    public Parameters put(String key, Object value){
        params.put(key, value);
        return this;
    }
    
    /**
     * Devuelve el mapa de parámetros acumulados.
     *
     * @return mapa de parámetros.
     */
    public Map<String, Object> getParams(){
        return params;
    }
    
    /**
     * Devuelve el valor de un parámetro por su clave.
     *
     * @param key clave del parámetro.
     * @return valor del parámetro.
     */
    public Object getParam(String key){
        return params.get(key);
    }
}
