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

/**
 *
 * @author Jorge Enciso
 */
public class DigestAuth {
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String[] getpHeader() {
        return pHeader;
    }

    public void setpHeader(String[] pHeader) {
        this.pHeader = pHeader;
    }

    public DigestAuth(String[] pHeader) {
        this.pHeader = pHeader;
    }
    private String[] pHeader;
    public DigestAuth() {
    }
    
    public DigestAuth(String header) {
        //Procesar header y asignar en los atributos.
        
        //Corta la mitad en vectores con una ","
        this.pHeader = header.split(",");
        //Obtiene los valores del vector cortando la mitad en el "="
        this.username = this.pHeader[0].split("=")[1];
        this.realm = this.pHeader[1].split("=")[1];
        this.nonce = this.pHeader[2].split("=")[1];
        this.uri= this.pHeader[3].split("=")[1];
        this.nonceCount = this.pHeader[5].split("=")[1];
        this.cnonce = this.pHeader[6].split("=")[1];
        this.opaque= this.pHeader[8].split("=")[1];
        
    }

    public boolean check(){
        return true;
    }
    
    public boolean checkMD5(){
        return true;
    }

    public boolean checkMD5_Sess(){
        //Verificar los siguientes casos
        return true;
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
}
