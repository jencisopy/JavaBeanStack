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

import java.util.Base64;
import static org.javabeanstack.util.Strings.left;
import static org.javabeanstack.util.Strings.substr;

/**
 * Modelo conteniendo información de autenticación del request del cliente basados 
 * en las especificaciones RFC 2069, RFC 2617
 * Más información en https://en.wikipedia.org/wiki/Digest_access_authentication
 * 
 * @author Jorge Enciso
 */
public class ClientAuth {
    private String header="";
    private String username = "";
    private String realm = "";
    private String nonce = "";
    private String cnonce = "";
    private String nonceCount = ""; 
    private String method = "GET";
    private String uri = "";
    private String qop = "auth";
    private String opaque = "";
    private String password = "";
    private String response = "";
    private String type = "";
    private String entityBody = "";
    
    public ClientAuth(){
    }

    public ClientAuth(String method, String msgHeader, String msgBody){
        setProperties(method, msgHeader, msgBody);
    }

    /**
     * Asigna en los atributos los valores recibidos como parámetros.
     * 
     * @param method metodos del protocolo httpd (GET,POST etc)
     * @param msgHeader valor de la variable "Authorization" del header del request
     * @param msgBody cuerpo del resquest
     */
    public final void setProperties(String method, String msgHeader, String msgBody) {
        //Procesar header y asignar en los atributos.
        type = left(msgHeader, msgHeader.indexOf(" "));
        msgHeader = substr(msgHeader, msgHeader.indexOf(" "));
        msgHeader = msgHeader.replace("\"", "");
        msgHeader = msgHeader.replace(" ", "");
        this.header = msgHeader;
        this.method = method;
        this.entityBody = msgBody;

        if (type.equalsIgnoreCase("Digest")) {
            //Guarda los valores de cada uno en MD5 auth
            this.cnonce = getPropertyValue("cnonce");
            this.nonceCount = getPropertyValue("nc");
            this.nonce = getPropertyValue("nonce");
            this.opaque = getPropertyValue("opaque");
            this.qop = getPropertyValue("qop");
            this.realm = getPropertyValue("realm");
            this.response = getPropertyValue("response");
            this.uri = getPropertyValue("uri");
            this.username = getPropertyValue("username");
        } else if (type.equalsIgnoreCase("Basic")) {
            msgHeader = new String(Base64.getDecoder().decode(this.header));
            this.username = msgHeader.split(":")[0];
            this.password = msgHeader.split(":")[1];
        }
    }

    /**
     * Devuelve un valor de una propiedad
     * @param property nombre de la propiedad
     * @return valor de una propiedad
     */
    private String getPropertyValue(String property) {
        //Corta la mitad en vectores con una ",". Luego de la mitad se corta la mitad de = con split 
        String[] pHeader = this.header.split(",");
        //Ordenar el header 
        String result = "";
        for (String pHeader1 : pHeader) {
            //Preguntar si la propiedad = property
            if (pHeader1.split("=", 2)[0].equals(property)) {
                result = pHeader1.split("=", 2)[1];
                break;
            }
        }
        return result;
    }

    /**
     * Valor de la variable "Authorization" del header del request
     * @return Valor de la variable "Authorization" del header del request
     */
    public String getHeader() {
        return header;
    }

    /**
     * Devuelve el nombre del usuario
     * @return nombre del usuario
     */
    public String getUsername() {
        return username;
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
     * Devuelve el valor de nonce
     * @return valor de nonce
     */
    public String getNonce() {
        return nonce;
    }

    /**
     * Devuelve el valor de cnonce
     * @return valor de cnonce
     */
    public String getCnonce() {
        return cnonce;
    }

    /**
     * Devuelve el valor de noncecount
     * @return valor de noncecount
     */
    public String getNonceCount() {
        return nonceCount;
    }

    /**
     * Devuelve el valor de method
     * @return valor de method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Devuelve el valor de uri
     * @return valor de uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * Devuelve el valor de qop
     * @return valor de qop
     */
    public String getQop() {
        return qop;
    }

    /**
     * Devuelve el valor de opaque
     * @return valor de opaque
     */
    public String getOpaque() {
        return opaque;
    }

    /**
     * Devuelve el valor de password
     * @return valor de password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Devuelve el valor de response
     * @return valor de response
     */
    public String getResponse() {
        return response;
    }

    /**
     * Devuelve el valor de type
     * @return valor de type
     */
    public String getType() {
        return type;
    }

    /**
     * Devuelve el valor de entityBody
     * @return valor de entityBody
     */
    public String getEntityBody() {
        return entityBody;
    }
}
