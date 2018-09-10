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
 *
 * @author Jorge Enciso
 */
public class ClientAuth {
    private String header="";
    private String username = "";
    private String realm = "";
    private String nonce = "";
    private String cnonce = "";
    private String nonceCount = ""; //Contador de request en hexadecimal en donde el usuario realiza la petición
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

    public String getHeader() {
        return header;
    }

    public String getUsername() {
        return username;
    }

    public String getRealm() {
        return realm;
    }

    public String getNonce() {
        return nonce;
    }

    public String getCnonce() {
        return cnonce;
    }

    public String getNonceCount() {
        return nonceCount;
    }

    public String getMethod() {
        return method;
    }

    public String getUri() {
        return uri;
    }

    public String getQop() {
        return qop;
    }

    public String getOpaque() {
        return opaque;
    }

    public String getPassword() {
        return password;
    }

    public String getResponse() {
        return response;
    }

    public String getType() {
        return type;
    }

    public String getEntityBody() {
        return entityBody;
    }
}
