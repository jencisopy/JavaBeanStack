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
package org.javabeanstack.security;

import java.util.Base64;
import org.javabeanstack.crypto.DigestUtil;
import static org.javabeanstack.util.Strings.*;

/**
 *
 * @author Jorge Enciso
 */
public class DigestAuth {

    private ClientAuth clientAuth;
    private ServerAuth serverAuth;

    public DigestAuth() {
    }

    public DigestAuth(String method, String msgHeader, String msgBody) {
        clientAuth = new ClientAuth();
        clientAuth.setProperties(method, msgHeader, msgBody);
    }

    public void setRequestAuth(String method, String msgHeader, String msgBody) {
        clientAuth = new ClientAuth();
        clientAuth.setProperties(method, msgHeader, msgBody);
    }

    public void setServerAuth(String type, String username, String password, String realm, String nonce, String opaque) {
        serverAuth = new ServerAuth();
        serverAuth.username = username;
        serverAuth.password = password;
        serverAuth.realm = realm;
        serverAuth.nonce = nonce;
        serverAuth.opaque = opaque;
        serverAuth.type = type;
    }

    public boolean check() {
        if (checkBasic()) {
            return true;
        }
        if (checkMD5()) {
            return true;
        }
        return checkMD5_Sess();
    }

    public boolean checkBasic() {
        if (!clientAuth.username.equals(serverAuth.username)) {
            return false;
        }
        return clientAuth.password.equals(serverAuth.password);
    }

    public boolean checkMD5() {
        //Check nonce, opaque, realm, type
        if (!serverAuth.check(clientAuth)) {
            return false;
        }
        String result = "";
        //Algoritmo MD5 
        String ha1 = DigestUtil.md5(clientAuth.username + ":" + clientAuth.realm + ":" + serverAuth.password);
        String ha2 = DigestUtil.md5(clientAuth.method + ":" + clientAuth.uri);
        String qop = clientAuth.qop;
        // qop = auth-int solo si el mensaje tiene cuerpo        
        if (clientAuth.qop.equals("auth-int")) {
            if (isNullorEmpty(clientAuth.entityBody)) {
                qop = "auth";
            }
        }
        if (qop.isEmpty()) {
            result = DigestUtil.md5(ha1 + ":" + clientAuth.nonce + ":" + ha2);
        } else if (qop.equals("auth")) {
            result = DigestUtil.md5(ha1 
                                        + ":" + clientAuth.nonce 
                                        + ":" + clientAuth.nonceCount 
                                        + ":" + clientAuth.cnonce 
                                        + ":" + clientAuth.qop 
                                        + ":" + ha2);
        } else if (qop.equals("auth-int")) {
            ha2 = DigestUtil.md5(clientAuth.method + ":" + clientAuth.uri + ":" + clientAuth.entityBody);
            result = DigestUtil.md5(ha1 
                                    + ":" + clientAuth.nonce 
                                    + ":" + clientAuth.nonceCount 
                                    + ":" + clientAuth.cnonce 
                                    + ":" + clientAuth.qop 
                                    + ":" + ha2);
        }
        return clientAuth.response.equals(result);
    }

    public boolean checkMD5_Sess() {
        //Check nonce, opaque, realm, type
        if (!serverAuth.check(clientAuth)) {
            return false;
        }
        String result = "";
        //Algoritmo MD5-sess 
        String ha1 = DigestUtil.md5(DigestUtil.md5(clientAuth.username + ":" + clientAuth.realm + ":" + serverAuth.password) + ":" + clientAuth.nonce + ":" + clientAuth.cnonce);
        String ha2 = DigestUtil.md5(clientAuth.method + ":" + clientAuth.uri);
        String qop = clientAuth.qop;
        // qop = auth-int solo si el mensaje tiene cuerpo
        if (clientAuth.qop.equals("auth-int")) {
            if (isNullorEmpty(clientAuth.entityBody)) {
                qop = "auth";
            }
        }
        if (clientAuth.qop.isEmpty()) {
            result = DigestUtil.md5(ha1 + ":" + clientAuth.nonce + ":" + ha2);
        } else {
            if (qop.equals("auth-int")) {
                ha2 = DigestUtil.md5(clientAuth.method + ":" + clientAuth.uri + ":" + clientAuth.entityBody);
            }
            result = DigestUtil.md5(ha1 + ":"
                                        + clientAuth.nonce + ":"
                                        + clientAuth.nonceCount + ":"
                                        + clientAuth.cnonce + ":"
                                        + clientAuth.qop + ":" + ha2);
        }
        return result.equals(clientAuth.response);
    }

    class ClientAuth {

        String header;
        String username = "";
        String realm = "";
        String nonce = "";
        String cnonce = "";
        String nonceCount = ""; //Contador de request en hexadecimal en donde el usuario realiza la petición
        String method = "GET";
        String uri = "";
        String qop = "auth";
        String opaque = "";
        String password = "";
        String response = "";
        String type = "";
        String entityBody = "";

        void setProperties(String method, String msgHeader, String msgBody) {
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
            String[] pHeader = clientAuth.header.split(",");
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
    }

    class ServerAuth {
        String type = "";
        String username = "";
        String password = "";
        String realm = "";
        String nonce = "";
        String nonceCount = ""; //Contador de request en hexadecimal en donde el usuario realiza la petición
        String opaque = "";

        boolean check(ClientAuth requestAuth) {
            if (!type.equals(requestAuth.type)) {
                return false;
            }
            if (!realm.equals(requestAuth.realm)) {
                return false;
            }
            if (!nonce.equals(requestAuth.nonce)) {
                return false;
            }
            return opaque.equals(requestAuth.opaque);
        }
    }
}
