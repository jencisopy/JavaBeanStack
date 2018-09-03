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

    private String header;
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

    public DigestAuth(String header) {
        //Procesar header y asignar en los atributos.
        // y luego se quita el las comillas dobles de cada valor con replace
        type = left(header, header.indexOf(" "));
        header = substr(header, header.indexOf(" "));
        header = header.replace("\"", "");
        header = header.replace(" ", "");
        this.header = header;

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
            //Implementar
            this.response = new String(Base64.getDecoder().decode(this.header));
            this.username = "";
            this.password = "";
        }
    }

    private String getPropertyValue(String property) {
        //Corta la mitad en vectores con una ",". Luego de la mitad se corta la mitad de = con split 
        String[] pHeader = header.split(",");
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

    public boolean check() {
        if (checkBasic()){
            return true;
        }
        if (checkMD5()) {
            return true;
        }
        return checkMD5_Sess();
    }

    public boolean checkBasic() {
        // Implementar
        //String result = Base64.getEncoder().encode(this.username.getBytes()+":".getBytes()+this.password.getBytes());
        //byte[] bPassword = Base64.getEncoder().encode(this.password.getBytes());
        //String result = Arrays.toString(bUser) + Arrays.toString(Base64.getEncoder().encode(":".getBytes())) + Arrays.toString(bPassword);
               // +":"+this.password).getBytes()));
        //String user = "password";
        //return user.equals(this.password);
        String result = this.username +":"+ this.password;
        return this.response.equals(result);
    }

    public boolean checkMD5() {
        String result = "";
        //Algoritmo MD5 
        if (qop.isEmpty()) {
            String ha1 = DigestUtil.md5(this.username + ":" + this.realm + ":" + getPassword());
            String ha2 = DigestUtil.md5(this.method + ":" + this.uri);
            result = DigestUtil.md5(ha1 + ":" + this.nonce + ":" + ha2);
        } else if (qop.equals("auth")) {
            String ha1 = DigestUtil.md5(this.username + ":" + this.realm + ":" + getPassword());
            String ha2 = DigestUtil.md5(this.method + ":" + this.uri);
            result = DigestUtil.md5(ha1 + ":" + this.nonce + ":" + this.nonceCount + ":"
                    + this.cnonce + ":" + this.qop + ":" + ha2);
        } else if (qop.equals("auth-int")) {
            //implementar
        }
        return this.response.equals(result);
    }

    public boolean checkMD5_Sess() {
        String result = "";
        //Algoritmo MD5-sess 
        if (qop.equals("auth")) {
            String ha1 = DigestUtil.md5(DigestUtil.md5(this.username + ":" + this.realm + ":" + getPassword()) + ":" + this.nonce + ":" + this.cnonce);
            String ha2 = DigestUtil.md5(this.method + ":" + this.uri);
            result = DigestUtil.md5(ha1 + ":" + this.nonce + ":" + this.nonceCount + ":"
                    + this.cnonce + ":" + this.qop + ":" + ha2);
        } else if (qop.equals("auth-int")) {
            //implementar
        }
        return result.equals(this.response);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getCnonce() {
        return cnonce;
    }

    public void setCnonce(String cnonce) {
        this.cnonce = cnonce;
    }

    public String getNonceCount() {
        return nonceCount;
    }

    public void setNonceCount(String nonceCount) {
        this.nonceCount = nonceCount;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getQop() {
        return qop;
    }

    public void setQop(String qop) {
        this.qop = qop;
    }

    public String getOpaque() {
        return opaque;
    }

    public void setOpaque(String opaque) {
        this.opaque = opaque;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
