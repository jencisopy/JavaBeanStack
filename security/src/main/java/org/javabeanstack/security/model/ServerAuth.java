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
package org.javabeanstack.security.model;

import java.util.Date;
import static org.javabeanstack.util.Dates.now;

/**
 * Modelo conteniendo información de que se utilizará para autenticar la petición del usuario
 * Basados en las especificaciones RFC 2069, RFC 2617
 * Más información en https://en.wikipedia.org/wiki/Digest_access_authentication
 * 
 * @author Jorge Enciso
 */
public class ServerAuth {
    private String nonce = "";
    private String opaque = "";
    private int nonceCount;
    private String realm = "";
    private Date lastReference = now();
    private String username = "";
    private String password = "";
    private String type = "Basic";

    /**
     * Devuelve el nombre del usuario
     * @return nombre del usuario
     */
    public String getUsername() {
        return username;
    }

    /**
     * Setea el atributo username
     * @param username nombre del usuario
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Devuelve el valor de password
     * @return valor de password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Asigna el password del usuario
     * @param password 
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Devuelve el valor de nonce
     * @return valor de nonce
     */
    public String getNonce() {
        return nonce;
    }

    /**
     * Asigna el atributo nonce 
     * @param nonce valor de nonce
     */
    public void setNonce(String nonce) {
        this.nonce = nonce;
    }


    /**
     * Devuelve el valor de opaque
     * @return valor de opaque
     */
    public String getOpaque() {
        return opaque;
    }

    /**
     * Asigna el atributo opaque
     * @param opaque valor de opaque
     */
    public void setOpaque(String opaque) {
        this.opaque = opaque;
    }

    /**
     * Devuelve el valor de noncecount
     * @return valor de noncecount
     */
    public int getNonceCount() {
        return nonceCount;
    }

    /**
     * Incrementa el nonceCount
     */
    public void increment() {
        this.nonceCount++;
    }

    
    /**
     * Devuelve el valor Realm 
     * Más información en https://en.wikipedia.org/wiki/Digest_access_authentication
     * @return valor del atributo realm
     */
    public String getRealm() {
        return realm;
    }

    /**
     * Asigna el atributo realm
     * @param realm valor de realm
     */
    public void setRealm(String realm) {
        this.realm = realm;
    }

    /**
     * Devuelve la ultima vez que fue referenciado.
     * @return ultima vez que fue referenciado.
     */
    public Date getLastReference() {
        return lastReference;
    }

    /**
     * Asigna el atributo lastreference
     * @param lastReference 
     */
    public void setLastReference(Date lastReference) {
        this.lastReference = lastReference;
    }

    /**
     * Devuelve tipo de autenticación (Basic, Digest)
     * @return 
     */
    public String getType() {
        return type;
    }

    /**
     * Asigna el atributo type (Basic, Digest)
     * @param type valor de type.
     */
    public void setType(String type) {
        this.type = type;
    }
}
