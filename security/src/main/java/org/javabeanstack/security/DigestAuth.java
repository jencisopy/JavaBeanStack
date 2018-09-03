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

import org.javabeanstack.crypto.DigestUtil;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

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
    private String response_esp = "";

    

    public DigestAuth(String header) {
        //Procesar header y asignar en los atributos.
        
        //Corta la mitad en vectores con una ",". Luego de la mitad se corta la mitad de = con split 
        // y luego se quita el las comillas dobles de cada valor con replace
       header = header.replace("\"", "");
       header = header.replace(" ", "");
       String[] pHeader = header.split(",");

        //Ordenar el header 
       ArrayList<String> arrayHeader = new ArrayList<>();
       arrayHeader.addAll(Arrays.asList(pHeader));
       Collections.sort(arrayHeader);
      
       //Guarda los valores de cada uno
       
       this.cnonce = arrayHeader.get(0).split("=",2)[1];
       this.nonceCount = arrayHeader.get(1).split("=",2)[1];
       this.nonce = arrayHeader.get(2).split("=",2)[1];
       this.opaque = arrayHeader.get(3).split("=",2)[1];
       this.qop = arrayHeader.get(4).split("=",2)[1];
       this.realm = arrayHeader.get(5).split("=",2)[1];
       this.response_esp = arrayHeader.get(6).split("=",2)[1];
       this.uri = arrayHeader.get(7).split("=",2)[1];
       this.username = arrayHeader.get(8).split("=",2)[1];
       
    }

    public boolean check() {
        return true;
    }
    public boolean checkMD5(){
        String ha1 = DigestUtil.md5(this.username + ":" + this.realm + ":password");
        String ha2 = DigestUtil.md5(this.method + ":" + this.uri);
        String response = DigestUtil.md5(ha1 + ":" + this.nonce + ":" + ha2);
        return response.equals(response_esp);
    }
    public boolean checkMD5_auth(){
        //Algoritmo MD5
        String ha1 = DigestUtil.md5(this.username + ":" + this.realm + ":password");
        String ha2 = DigestUtil.md5(this.method + ":" + this.uri);
        String response = DigestUtil.md5(ha1 + ":" + this.nonce + ":" + this.nonceCount + ":"
         + this.cnonce + ":" + this.qop + ":" + ha2);
        return response.equals(response_esp);
    }
     public boolean checkMD5_sess_auth(){
        //Algoritmo MD5-sess
        String ha1 = DigestUtil.md5(this.username + ":" + this.realm + ":password");
        String ha2 = DigestUtil.md5(this.method + ":" + this.uri);
        String response = DigestUtil.md5(ha1 + ":" + this.nonce + ":" + this.nonceCount + ":"
         + this.cnonce + ":" + this.qop + ":" + ha2);
        return response.equals(response_esp);
    }

    public boolean checkMD5_Sess() {
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

    

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    public String getResponse_esp() {
        return response_esp;
    }

    public void setResponse_esp(String response_esp) {
        this.response_esp = response_esp;
    }
}