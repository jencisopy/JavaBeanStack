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

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.lang3.time.DateUtils;
import org.javabeanstack.crypto.DigestUtil;
import org.javabeanstack.security.model.ClientAuth;
import org.javabeanstack.security.model.ServerAuth;
import org.javabeanstack.util.Dates;
import org.javabeanstack.util.Fn;
import org.javabeanstack.util.Strings;
import static org.javabeanstack.util.Strings.*;

/**
 *
 * @author Jorge Enciso
 */
public class DigestAuth {
    private final Map<String, ServerAuth> serverAuthMap = new HashMap();
    public static String BASIC = "Basic";
    public static String DIGEST = "Digest";
    private String type = "BASIC";
    private String realm = "";
    private String qop = "";

    public DigestAuth() {
    }

    public DigestAuth(String typeAuth, String realm, String qop) {
        this.type = typeAuth;
        this.realm = realm;
        this.qop = qop;
    }
    
    public ServerAuth createResponseAuth(){
        return DigestAuth.this.createResponseAuth(type, null, realm);
    }
    
    public String getResponseHeader(ServerAuth responseAuth){
        String value = "";
        value = type;
        if (type.equals("Basic")){
            value += " realm=\"Restricted\"";
            return value;
        }
        String opaque = responseAuth.getOpaque();
        String nonce = responseAuth.getNonce();
        String nc = ((Integer)responseAuth.getNonceCount()).toString();
        nc = Strings.leftPad(nc, 10, "0");
        
        value += " realm=\""+realm
                + "\" qop=\""+qop
                + "\" nonce=\""+nonce 
                + "\" opaque=\""+opaque
                + "\" nc="+nc;

        return value;
    }
    
    public ServerAuth createResponseAuth(String typeAuth, String nonce, String realm){
        ServerAuth responseAuth = new ServerAuth();
        if (typeAuth == null){
            typeAuth = getType();
        }
        if (realm == null){
            typeAuth = getRealm();
        }
        
        responseAuth.setType(typeAuth);
        responseAuth.setRealm(Fn.nvl(realm, ""));
        if (nonce == null){
            // Crear un nonce y opaque en forma aleatoria
            String random = typeAuth+":"+Dates.now().toString()+":"+Fn.nvl(realm, "");
            String random2 = Dates.now().toString();
            nonce = DigestUtil.md5(random);
            responseAuth.setOpaque(DigestUtil.md5(random2));
        }
        responseAuth.setNonce(nonce);
        serverAuthMap.put(nonce, responseAuth);
        purgeResponseAuth();
        return responseAuth;
    }
    
    protected void purgeResponseAuth(){
        Date now = DateUtils.addMinutes(Dates.now(),-1);
        for(Iterator<Map.Entry<String, ServerAuth>> it = serverAuthMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, ServerAuth> entry = it.next();
            if(entry.getValue().getLastReference().before(now)) {
                it.remove();
            }        
        }
    }
    
    public ServerAuth getResponseAuth(String nonce){
        ServerAuth auth = serverAuthMap.get(nonce);
        if (auth != null){
            auth.setLastReference(new Date());
        }
        return auth;
    }
    

    public boolean isNonceExist(String nonce){
        ServerAuth responseAuth = serverAuthMap.get(nonce);
        if (responseAuth != null){
            responseAuth.setLastReference(new Date());
            return true;
        }
        return false;
    }
    
    public String getOpaque(String nonce){
        ServerAuth response = serverAuthMap.get(nonce);        
        if (response != null){
            return response.getOpaque();
        }
        return null;
    }

    protected boolean checkNonce(ClientAuth clientAuth){
        //Verificar existencia de nonce
        if (!isNonceExist(clientAuth.getNonce())){
            return false;
        }
        //Verificar válidez de opaque
        if (!isNullorEmpty(clientAuth.getQop())){
            if (getOpaque(clientAuth.getNonce()) == null){
                return false;
            }
        }
        return true;
    }
    
    public boolean check(ClientAuth requestAuth) {
        if (requestAuth.getType().equals(BASIC)){
            return checkBasic(requestAuth);
        }
        if (requestAuth.getType().equals(DIGEST)){
            if (checkMD5(requestAuth)) {
                return true;
            }
            if (checkMD5_Sess(requestAuth)){
                return true;
            }
            ServerAuth responseAuth = serverAuthMap.get(requestAuth.getNonce());        
            if (responseAuth != null){
                responseAuth.increment();
            }
        }
        return false;
    }

    public boolean checkBasic(ClientAuth clientAuth) {
        ServerAuth serverAuth = getResponseAuth("basic");
        if (serverAuth == null){
            return false;
        }
        if (!clientAuth.getUsername().equals(serverAuth.getUsername())) {
            return false;
        }
        return clientAuth.getPassword().equals(serverAuth.getPassword());
    }

    public boolean checkMD5(ClientAuth clientAuth) {
        ServerAuth serverAuth = getResponseAuth(clientAuth.getNonce());        
        //Check nonce, opaque, realm, type
        if (!compareServerAndClientAuth(clientAuth, serverAuth)) {
            return false;
        }
        String result = "";
        //Algoritmo MD5 
        String ha1 = DigestUtil.md5(clientAuth.getUsername() + ":" + clientAuth.getRealm() + ":" + serverAuth.getPassword());
        String ha2 = DigestUtil.md5(clientAuth.getMethod() + ":" + clientAuth.getUri());
        String qop = clientAuth.getQop();
        // qop = auth-int solo si el mensaje tiene cuerpo        
        if (clientAuth.getQop().equals("auth-int")) {
            if (isNullorEmpty(clientAuth.getEntityBody())) {
                qop = "auth";
            }
        }
        if (qop.isEmpty()) {
            result = DigestUtil.md5(ha1 + ":" + clientAuth.getNonce() + ":" + ha2);
        } else if (qop.equals("auth")) {
            result = DigestUtil.md5(ha1 
                                        + ":" + clientAuth.getNonce()
                                        + ":" + clientAuth.getNonceCount()
                                        + ":" + clientAuth.getCnonce()
                                        + ":" + clientAuth.getQop()
                                        + ":" + ha2);
        } else if (qop.equals("auth-int")) {
            ha2 = DigestUtil.md5(clientAuth.getMethod() + ":" + clientAuth.getUri() + ":" + clientAuth.getEntityBody());
            result = DigestUtil.md5(ha1 
                                    + ":" + clientAuth.getNonce()
                                    + ":" + clientAuth.getNonceCount() 
                                    + ":" + clientAuth.getCnonce()
                                    + ":" + clientAuth.getQop() 
                                    + ":" + ha2);
        }
        return clientAuth.getResponse().equals(result);
    }

    public boolean checkMD5_Sess(ClientAuth clientAuth) {
        ServerAuth serverAuth = getResponseAuth(clientAuth.getNonce());
        //Check nonce, opaque, realm, type
        if (!compareServerAndClientAuth(clientAuth, serverAuth)) {
            return false;
        }
        String result = "";
        //Algoritmo MD5-sess 
        String ha1 = DigestUtil.md5(DigestUtil.md5(clientAuth.getUsername() + ":" + clientAuth.getRealm() + ":" + serverAuth.getPassword()) + ":" + clientAuth.getNonce() + ":" + clientAuth.getCnonce());
        String ha2 = DigestUtil.md5(clientAuth.getMethod() + ":" + clientAuth.getUri());
        String qop = clientAuth.getQop();
        // qop = auth-int solo si el mensaje tiene cuerpo
        if (clientAuth.getQop().equals("auth-int")) {
            if (isNullorEmpty(clientAuth.getEntityBody())) {
                qop = "auth";
            }
        }
        if (clientAuth.getQop().isEmpty()) {
            result = DigestUtil.md5(ha1 + ":" + clientAuth.getNonce() + ":" + ha2);
        } else {
            if (qop.equals("auth-int")) {
                ha2 = DigestUtil.md5(clientAuth.getMethod() + ":" + clientAuth.getUri() + ":" + clientAuth.getEntityBody());
            }
            result = DigestUtil.md5(ha1 + ":"
                                        + clientAuth.getNonce() + ":"
                                        + clientAuth.getNonceCount() + ":"
                                        + clientAuth.getCnonce() + ":"
                                        + clientAuth.getQop() + ":" + ha2);
        }
        return result.equals(clientAuth.getResponse());
    }
    
    public boolean compareServerAndClientAuth(ClientAuth requestAuth, ServerAuth responseAuth) {
        if (!responseAuth.getType().equals(requestAuth.getType())) {
            return false;
        }
        if (!responseAuth.getRealm().equals(requestAuth.getRealm())) {
            return false;
        }
        return responseAuth.getNonce().equals(requestAuth.getNonce());
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getQop() {
        return this.qop;
    }
    
    public void setQop(String qop) {
        this.qop = qop;
    }
}
