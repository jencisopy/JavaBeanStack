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
 * Clase que implementa funcionalidades de autenticación basados en las especificaciones
 * RFC 2069, RFC 2617
 * Más información en https://en.wikipedia.org/wiki/Digest_access_authentication
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
    private int numberCanFail = 10;
    private int secondsIdle=60;

    public DigestAuth() {
    }

    /**
     * Setea propiedades que definirá el comportamiento del componente.
     * 
     * @param typeAuth tipo de autenticación ("Basic","Digest")
     * @param realm entorno, grupo o base donde se autentica.
     * @param qop quality of protection (para digest los valores posibles "auth","auth-int")
     */
    public DigestAuth(String typeAuth, String realm, String qop) {
        this.type = typeAuth;
        this.realm = realm;
        this.qop = qop;
    }
    
    
    /**
     * Devuelve el valor alfanumerico que se utilizará para enviar en la variable
     * header "www-autenticate" del paquete de respuesta del servidor.
     * @param responseAuth objeto con los datos necesarios para la autenticación
     * @return valor alfanumerico que se utilizará para enviar en la variable
     * header "www-autenticate" del paquete de respuesta del servidor.
     */
    public String getResponseHeader(ServerAuth responseAuth){
        String value = "";
        value = type;
        if (type.equalsIgnoreCase("Basic")){
            value += " realm=\"Restricted\"";
            return value;
        }
        String opaque = responseAuth.getOpaque();
        String nonce = responseAuth.getNonce();
        String nc = ((Integer)responseAuth.getNonceCount()).toString();
        nc = Strings.leftPad(nc, 8, "0");
        
        value += " realm=\""+realm
                + "\" qop=\""+qop
                + "\" nonce=\""+nonce 
                + "\" opaque=\""+opaque
                + "\" nc="+nc;

        return value;
    }

    /**
     * Crea un objeto para autenticar que envia al cliente
     * @return objeto que se utilizará para autenticar la petición.
     */
    public ServerAuth createResponseAuth(){
        return DigestAuth.this.createResponseAuth(type, null, realm);
    }
    
    /**
     * Devuelve el valor alfanumerico que se utilizará para enviar en la variable
     * header "www-autenticate" del paquete de respuesta del servidor.
     * La ejecución directa de este metodo es para casos excepcionales en la cual
     * se creará typeAuth, nonce o realm personalizados.
     * 
     * @param typeAuth tipo de autenticación ("Basic","Digest")
     * @param nonce codigo identificador del objeto autenticador.
     * @param realm grupo, entorno o base donde se autenticará la petición.
     * @return valor alfanumerico que se utilizará para enviar en la variable
     * header "www-autenticate" del paquete de respuesta del servidor.
     */
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
        // Purgar autenticadores viejos, si hay acumuladas más de 500 peticiones
        if (serverAuthMap.size() > 500){
            purgeResponseAuth();
        }
        return responseAuth;
    }
    
    /**
     * Elimina todos los objeto de autenticación que ya fuerón utilizados o
     * no se hicierón en un tiempo definido en el atributo secondsIdle
     */
    public void purgeResponseAuth(){
        Date now = DateUtils.addSeconds(Dates.now(),secondsIdle*-1);
        for(Iterator<Map.Entry<String, ServerAuth>> it = serverAuthMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, ServerAuth> entry = it.next();
            if(entry.getValue().getLastReference().before(now)) {
                it.remove();
            }        
        }
    }
    
    /**
     * Devuelve el objeto creado con los datos necesarios para autenticar una
     * petición.
     * @param nonce identificador del objeto.
     * @return objeto creado con los datos necesarios para autenticar una petición.
     */
    public ServerAuth getResponseAuth(String nonce){
        ServerAuth auth = serverAuthMap.get(nonce);
        if (auth != null){
            auth.setLastReference(new Date());
        }
        return auth;
    }
    

    /**
     * Devuelve verdadero o falso si existe o no un objeto responseAuth
     * @param nonce identificador del objeto.
     * @return verdadero o falso si existe o no un objeto responseAuth
     */
    public boolean isNonceExist(String nonce){
        ServerAuth responseAuth = serverAuthMap.get(nonce);
        if (responseAuth != null){
            responseAuth.setLastReference(new Date());
            return true;
        }
        return false;
    }
    
    /**
     * Devuelve el valor opaque del objeto autenticador.
     * @param nonce identificador del objeto autenticador.
     * @return el valor opaque del objeto autenticador solicitado.
     */
    public String getOpaque(String nonce){
        ServerAuth response = serverAuthMap.get(nonce);        
        if (response != null){
            return response.getOpaque();
        }
        return null;
    }

    /**
     * Chequea válidez de los datos que vienen en el header del request
     * @param clientAuth objeto request enviado en la petición del usuario.
     * @return verdadero o falso si los datos que vienen en la header del request
     * es válido o no.
     */
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
    
    /**
     * Autentica la petición. Válida todas las opciones.
     * @param requestAuth datos de autenticación enviado por el usuario.
     * @return verdadero o falso si tuvo exito o fracaso la autenticación.
     */
    public boolean check(ClientAuth requestAuth) {
        if (requestAuth.getType().equals(BASIC)){
            return checkBasic(requestAuth);
        }
        if (requestAuth.getType().equals(DIGEST)){
            if (checkMD5(requestAuth)) {
                // Eliminar serverAuth si tuvo exito
                serverAuthMap.remove(requestAuth.getNonce());
                return true;
            }
            if (checkMD5_Sess(requestAuth)){
                // Eliminar serverAuth si tuvo exito
                serverAuthMap.remove(requestAuth.getNonce());
                return true;
            }
            ServerAuth responseAuth = serverAuthMap.get(requestAuth.getNonce());        
            if (responseAuth != null){
                responseAuth.increment();
                // Si sobrepasa los 10 intentos eliminar el objeto serverAuth
                if (responseAuth.getNonceCount() > getNumberCanFail()){
                    serverAuthMap.remove(requestAuth.getNonce());
                }
            }
        }
        return false;
    }

    /**
     * Autentica la petición utilizando la modalidad "Basic"
     * @param clientAuth datos de autenticación enviado por el usuario.
     * @return verdadero o falso si tuvo exito o fracaso la autenticación.
     */
    public boolean checkBasic(ClientAuth clientAuth) {
        ServerAuth serverAuth = getResponseAuth("basic "+clientAuth.getUsername());
        //Usuario incorrecto o no se seteo el autenticador
        if (serverAuth == null){
            return false;
        }
        //Verificar cantidad de intentos fallidos.
        if (serverAuth.getNonceCount() >= numberCanFail){
            //Eliminar autenticador cuando sobrepasa cantidad de fallos permitidos
            serverAuthMap.remove("basic "+clientAuth.getUsername());
            return false;
        }
        // Verificar password
        if (!clientAuth.getPassword().equals(serverAuth.getPassword())){
            serverAuth.increment();
            return false;
        }
        //Tuvo exito eliminar autenticador
        serverAuthMap.remove("basic "+clientAuth.getUsername());        
        return true;
    }


    /**
     * Autentica la petición utilizando la modalidad "Digest" MD5
     * @param clientAuth datos de autenticación enviado por el usuario.
     * @return verdadero o falso si tuvo exito o fracaso la autenticación.
     */
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

    /**
     * Autentica la petición utilizando la modalidad "Digest" MD5-sess
     * @param clientAuth datos de autenticación enviado por el usuario.
     * @return verdadero o falso si tuvo exito o fracaso la autenticación.
     */
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
    
    /**
     * Compara valores del objeto autenticador generado en el server y el objeto enviado por el cliente.
     * @param requestAuth objeto autenticador enviado desde el cliente.
     * @param responseAuth objeto autenticador generado en el servidor.
     * @return verdadero o falso si son iguales o difieren en algún dato.
     */
    public boolean compareServerAndClientAuth(ClientAuth requestAuth, ServerAuth responseAuth) {
        if (responseAuth == null){
            return false;
        }
        if (!responseAuth.getType().equals(requestAuth.getType())) {
            return false;
        }
        if (!responseAuth.getRealm().equals(requestAuth.getRealm())) {
            return false;
        }
        if (responseAuth.getNonceCount() != 0 || !isNullorEmpty(requestAuth.getNonceCount())){
            if (!requestAuth.getQop().isEmpty() && responseAuth.getNonceCount() != Integer.parseInt(requestAuth.getNonceCount())){
                return false;
            }
        }
        return responseAuth.getNonce().equals(requestAuth.getNonce());
    }

    /**
     * Tipo de autenticación ("Basic", "Digest")
     * @return tipo de autenticación.
     */
    public String getType() {
        return type;
    }

    /**
     * Setea tipo de autenticación. Valores válidos positbles "Basic" o "Digest"
     * @param type tipo de autenticación.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Entorno, grupo o base donde autenticar la petición.
     * @return Entorno, grupo o base donde autenticar la petición.
     */
    public String getRealm() {
        return realm;
    }

    /**
     * Setea la propiedad realm
     * @param realm Entorno, grupo o base donde autenticar la petición.
     */
    public void setRealm(String realm) {
        this.realm = realm;
    }

    /**
     * Devuelve la propiedad qop (quality of protection)
     * @return propiedad qop
     */
    public String getQop() {
        return this.qop;
    }
    
    /**
     * Setea la propiedad qoq (valores posibles "","auth","auth-int")
     * @param qop quality of protection
     */
    public void setQop(String qop) {
        this.qop = qop;
    }

    /**
     * Devuelve el numero de veces que se puede fallar en la autenticación
     * 
     * @return numero de veces que se puede fallar en la autenticación
     */
    public int getNumberCanFail() {
        return numberCanFail;
    }

    /**
     * Setea al numero de veces que se puede fallar en la autenticación.
     * @param numberCanFail 
     */
    public void setNumberCanFail(int numberCanFail) {
        this.numberCanFail = numberCanFail;
    }

    /**
     * Devuelve Cantidad de segundos antes de considerarse el autenticador como obsoleto
     * @return  Cantidad de segundos antes de considerarse el autenticador como obsoleto
     */
    public int getSecondsIdle() {
        return secondsIdle;
    }

    /**
     * Setea cantidad de segundos la cual se considera válido el autenticador creado.
     * @param secondsIdle 
     */
    public void setSecondsIdle(int secondsIdle) {
        this.secondsIdle = secondsIdle;
    }
}
