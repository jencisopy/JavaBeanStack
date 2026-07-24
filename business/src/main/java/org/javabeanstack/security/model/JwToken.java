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
package org.javabeanstack.security.model;

import java.util.Map;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Modelo de las reivindicaciones (claims) de un JSON Web Token (JWT): emisor,
 * sujeto, audiencia, identificador, tiempos de validez y datos adicionales.
 *
 * @author Jorge Enciso
 */
@XmlRootElement
public class JwToken {
    private String iss;
    private String sub;
    private String aud;
    private Long jti;
    private Long exp;
    private Long nbf;
    private String apiKey;
    private Map<String,String> data;

    /**
     * Devuelve el emisor del token (claim {@code iss}).
     * @return emisor del token.
     */
    public String getIss() {
        return iss;
    }

    /**
     * Asigna el emisor del token (claim {@code iss}).
     * @param iss emisor del token.
     */
    public void setIss(String iss) {
        this.iss = iss;
    }

    /**
     * Devuelve el sujeto del token (claim {@code sub}).
     * @return sujeto del token.
     */
    public String getSub() {
        return sub;
    }

    /**
     * Asigna el sujeto del token (claim {@code sub}).
     * @param sub sujeto del token.
     */
    public void setSub(String sub) {
        this.sub = sub;
    }

    /**
     * Devuelve la audiencia del token (claim {@code aud}).
     * @return audiencia del token.
     */
    public String getAud() {
        return aud;
    }

    /**
     * Asigna la audiencia del token (claim {@code aud}).
     * @param aud audiencia del token.
     */
    public void setAud(String aud) {
        this.aud = aud;
    }

    /**
     * Devuelve el identificador del token (claim {@code jti}).
     * @return identificador del token.
     */
    public Long getJti() {
        return jti;
    }

    /**
     * Asigna el identificador del token (claim {@code jti}).
     * @param jti identificador del token.
     */
    public void setJti(Long jti) {
        this.jti = jti;
    }

    /**
     * Devuelve el tiempo de expiración del token (claim {@code exp}).
     * @return tiempo de expiración.
     */
    public Long getExp() {
        return exp;
    }

    /**
     * Asigna el tiempo de expiración del token (claim {@code exp}).
     * @param exp tiempo de expiración.
     */
    public void setExp(Long exp) {
        this.exp = exp;
    }

    /**
     * Devuelve el tiempo a partir del cual el token es válido (claim {@code nbf}).
     * @return tiempo de inicio de validez.
     */
    public Long getNbf() {
        return nbf;
    }

    /**
     * Asigna el tiempo a partir del cual el token es válido (claim {@code nbf}).
     * @param nbf tiempo de inicio de validez.
     */
    public void setNbf(Long nbf) {
        this.nbf = nbf;
    }

    /**
     * Devuelve la clave de API asociada al token.
     * @return clave de API.
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Asigna la clave de API asociada al token.
     * @param apiKey clave de API.
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Devuelve los datos adicionales incluidos en el token.
     * @return mapa clave → valor de datos adicionales.
     */
    public Map<String, String> getData() {
        return data;
    }

    /**
     * Asigna los datos adicionales incluidos en el token.
     * @param data mapa clave → valor de datos adicionales.
     */
    public void setData(Map<String, String> data) {
        this.data = data;
    }
}
