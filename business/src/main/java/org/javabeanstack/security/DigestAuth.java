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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.time.DateUtils;
import org.javabeanstack.crypto.DigestUtil;
import org.javabeanstack.exceptions.TypeAuthInvalid;
import org.javabeanstack.security.model.ClientAuth;
import org.javabeanstack.security.model.ServerAuth;
import org.javabeanstack.util.Dates;
import org.javabeanstack.util.Fn;
import org.javabeanstack.util.Strings;
import static org.javabeanstack.util.Strings.*;

/**
 * Clase que implementa funcionalidades de autenticación basado en la
 * especificación RFC 2617.
 * Más información en https://en.wikipedia.org/wiki/Digest_access_authentication
 * https://tools.ietf.org/html/rfc2617
 *
 * @author Jorge Enciso
 */
public class DigestAuth {
    private final Map<String, ServerAuth> serverAuthMap = new HashMap();
    public final static String BASIC = "Basic";
    public final static String DIGEST = "Digest";

    private String type = "Basic";
    private String realm = "";
    private String qop = "";
    private int numberCanFail = 10;
    private int secondsIdle = 60;
    private List<String> typeAuthValids = new ArrayList();
    private List<String> algorithmValids = new ArrayList();
    private List<String> qopValids = new ArrayList();

    public DigestAuth() {
        defaultAttributes();
    }
    
    /**
     * Setea propiedades que definirá el comportamiento del componente.
     *
     * @param typeAuth tipo de autenticación ("Basic","Digest")
     * @param realm entorno, grupo o base donde se autentica.
     * @param qop quality of protection (para digest los valores posibles
     * "auth","auth-int")
     * @throws org.javabeanstack.exceptions.TypeAuthInvalid
     */
    public DigestAuth(String typeAuth, String realm, String qop) throws TypeAuthInvalid {
        defaultAttributes();
        if (!isValidTypeAuth(typeAuth)){
            throw new TypeAuthInvalid(typeAuth+" no es válido");
        }
        this.type = typeAuth;
        this.realm = realm;
        this.qop = Fn.nvl(qop,"");
        if (!this.qop.isEmpty()){
            String[] qops = this.qop.split(",");
            for (int i=0;i < qops.length;i++){
               qops[i] = qops[i].trim();
            }
            this.qopValids = Arrays.asList(qops);
        }
    }

    /**
     * Atributos por defecto
     */
    private void defaultAttributes(){
        typeAuthValids.add("Basic");
        typeAuthValids.add("Digest");
        
        algorithmValids.add("MD5");
        algorithmValids.add("MD5-sess");
        
        qopValids.add("");
        qopValids.add("auth");
        qopValids.add("auth-int");
    }
    
    protected final boolean isValidTypeAuth(String typeAuth){
        return typeAuthValids.contains(typeAuth);
    }

    protected final boolean isValidAlgoritm(String algorithm){
        return algorithmValids.contains(algorithm);
    }

    protected final boolean isValidQop(String qop){
        return qopValids.contains(qop);
    }
    
    /**
     * Devuelve el valor alfanumerico que se utilizará para enviar en la
     * variable header "www-autenticate" del paquete de respuesta del servidor.
     *
     * @param responseAuth objeto con los datos necesarios para la autenticación
     * @return valor alfanumerico que se utilizará para enviar en la variable
     * header "www-autenticate" del paquete de respuesta del servidor.
     */
    public String getResponseHeader(ServerAuth responseAuth) {
        String value = type;
        if (type.equalsIgnoreCase("Basic")) {
            value += " realm=\"Restricted\"";
            return value;
        }
        String opaque = responseAuth.getOpaque();
        String nonce = responseAuth.getNonce();
        String nc = ((Integer) responseAuth.getNonceCount()).toString();
        nc = Strings.leftPad(nc, 8, "0");

        value += " realm=\"" + realm
                + "\" qop=\"" + qop
                + "\" nonce=\"" + nonce
                + "\" opaque=\"" + opaque
                + "\" nc=" + nc;

        return value;
    }

    /**
     * Crea un objeto que se utilizará para autenticar la petición del cliente.
     *
     * @return objeto que se utilizará para autenticar la petición.
     * @throws org.javabeanstack.exceptions.TypeAuthInvalid
     */
    public ServerAuth createResponseAuth() throws TypeAuthInvalid {
        return DigestAuth.this.createResponseAuth(type, null, realm);
    }

    /**
     * Crea un objeto que se utilizará para autenticar la petición del cliente.
     *
     * @return objeto que se utilizará para autenticar la petición.
     * @throws org.javabeanstack.exceptions.TypeAuthInvalid
     *
     * @param typeAuth tipo de autenticación ("Basic","Digest")
     * @param nonce codigo identificador del objeto autenticador. Si es nulo se
     * generará el identificador de forma aleatoria
     * @param realm grupo, entorno o base donde se autenticará la petición.
     */
    public ServerAuth createResponseAuth(String typeAuth, String nonce, String realm) throws TypeAuthInvalid {
        ServerAuth responseAuth = new ServerAuth();
        if (typeAuth == null) {
            typeAuth = getType();
        }
        // Validar tipos de autenticaciones permitidas
        if (!isValidTypeAuth(typeAuth)) {
            throw new TypeAuthInvalid(typeAuth + " no esta permitido");
        }
        if (realm == null) {
            typeAuth = getRealm();
        }

        responseAuth.setType(typeAuth);
        responseAuth.setRealm(Fn.nvl(realm, ""));
        if (nonce == null) {
            // Crear un nonce y opaque en forma aleatoria
            String random = typeAuth + ":" + Dates.now().toString() + ":" + Fn.nvl(realm, "");
            String random2 = Dates.now().toString();
            nonce = DigestUtil.md5(random);
            responseAuth.setOpaque(DigestUtil.md5(random2));
        }
        responseAuth.setNonce(nonce);
        serverAuthMap.put(nonce, responseAuth);
        // Purgar autenticadores viejos, si hay acumuladas más de 500 peticiones
        if (serverAuthMap.size() > 500) {
            purgeResponseAuth();
        }
        return responseAuth;
    }

    /**
     * Elimina todos los objeto de autenticación que ya fuerón utilizados o no
     * se hicierón referencia en un tiempo definido en el atributo secondsIdle
     */
    public void purgeResponseAuth() {
        Date now = DateUtils.addSeconds(Dates.now(), secondsIdle * -1);
        for (Iterator<Map.Entry<String, ServerAuth>> it = serverAuthMap.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, ServerAuth> entry = it.next();
            if (entry.getValue().getLastReference().before(now)) {
                it.remove();
            }
        }
    }

    /**
     * Devuelve el objeto creado con los datos necesarios para autenticar una
     * petición.
     *
     * @param nonce identificador del objeto.
     * @return objeto creado con los datos necesarios para autenticar una
     * petición.
     */
    public ServerAuth getResponseAuth(String nonce) {
        ServerAuth auth = serverAuthMap.get(nonce);
        if (auth != null) {
            auth.setLastReference(new Date());
        }
        return auth;
    }

    /**
     * Devuelve verdadero o falso si existe o no un objeto responseAuth
     *
     * @param nonce identificador del objeto.
     * @return verdadero o falso si existe o no un objeto responseAuth
     */
    public boolean isNonceExist(String nonce) {
        ServerAuth responseAuth = serverAuthMap.get(nonce);
        if (responseAuth != null) {
            responseAuth.setLastReference(new Date());
            return true;
        }
        return false;
    }

    /**
     * Devuelve el valor opaque del objeto autenticador.
     *
     * @param nonce identificador del objeto autenticador.
     * @return el valor opaque del objeto autenticador solicitado.
     */
    public String getOpaque(String nonce) {
        ServerAuth response = serverAuthMap.get(nonce);
        if (response != null) {
            return response.getOpaque();
        }
        return null;
    }

    /**
     * Chequea válidez de los datos que vienen en el header del request
     *
     * @param clientAuth objeto request enviado en la petición del usuario.
     * @return verdadero o falso si los datos que vienen en la header del
     * request es válido o no.
     */
    protected boolean checkNonce(ClientAuth clientAuth) {
        //Verificar existencia de nonce
        if (!isNonceExist(clientAuth.getNonce())) {
            return false;
        }
        //Verificar válidez de opaque
        if (!isNullorEmpty(clientAuth.getQop())) {
            if (getOpaque(clientAuth.getNonce()) == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Autentica la petición. Válida todas las opciones.
     *
     * @param requestAuth datos de autenticación enviado por el usuario.
     * @return verdadero o falso si tuvo exito o fracaso la autenticación.
     */
    public boolean check(ClientAuth requestAuth) {
        if (requestAuth.getType().equals(BASIC) && isValidTypeAuth(BASIC)) {
            return checkBasic(requestAuth);
        }
        if (requestAuth.getType().equals(DIGEST) && isValidTypeAuth(DIGEST)) {
            if (checkMD5(requestAuth)) {
                // Eliminar serverAuth si tuvo exito
                serverAuthMap.remove(requestAuth.getNonce());
                return true;
            }
            if (checkMD5_Sess(requestAuth)) {
                // Eliminar serverAuth si tuvo exito
                serverAuthMap.remove(requestAuth.getNonce());
                return true;
            }
            ServerAuth responseAuth = serverAuthMap.get(requestAuth.getNonce());
            if (responseAuth != null) {
                responseAuth.increment();
                // Si sobrepasa los n intentos permitidos eliminar el objeto serverAuth
                if (responseAuth.getNonceCount() > getNumberCanFail()) {
                    serverAuthMap.remove(requestAuth.getNonce());
                }
            }
        }
        return false;
    }

    /**
     * Autentica la petición utilizando el tipo de autenticación "Basic"
     *
     * @param clientAuth datos de autenticación enviado por el usuario.
     * @return verdadero o falso si tuvo exito o fracaso la autenticación.
     */
    public boolean checkBasic(ClientAuth clientAuth) {
        // Validar si esta permitido el tipo de autenticación "Basic"
        if (!isValidTypeAuth(BASIC)){
            return false;
        }
        ServerAuth serverAuth = getResponseAuth(clientAuth.getUsername());
        //Usuario incorrecto o no se seteo el autenticador
        if (serverAuth == null) {
            return false;
        }
        //Verificar cantidad de intentos fallidos.
        if (serverAuth.getNonceCount() >= numberCanFail) {
            //Eliminar autenticador cuando sobrepasa cantidad de fallos permitidos
            serverAuthMap.remove(clientAuth.getUsername());
            return false;
        }
        // Verificar password
        if (!clientAuth.getPassword().equals(serverAuth.getPassword())) {
            serverAuth.increment();
            return false;
        }
        //Tuvo exito eliminar autenticador
        serverAuthMap.remove(clientAuth.getUsername());
        return true;
    }

    /**
     * Autentica la petición utilizando la modalidad "Digest" MD5
     *
     * @param clientAuth datos de autenticación enviado por el usuario.
     * @return verdadero o falso si tuvo exito o fracaso la autenticación.
     */
    public boolean checkMD5(ClientAuth clientAuth) {
        // Validar si esta permitido el tipo de autenticación "Digest"        
        if (!isValidTypeAuth(DIGEST)){
            return false;
        }
        // Validar si esta permitido el algoritmo "MD5"
        if (!isValidAlgoritm("MD5")){
            return false;
        }
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
            ha2 = DigestUtil.md5(clientAuth.getMethod() + ":" + clientAuth.getUri() + ":" + DigestUtil.md5(clientAuth.getEntityBody()));
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
     *
     * @param clientAuth datos de autenticación enviado por el usuario.
     * @return verdadero o falso si tuvo exito o fracaso la autenticación.
     */
    public boolean checkMD5_Sess(ClientAuth clientAuth) {
        // Validar si esta permitido el tipo de autenticación "Digest"        
        if (!isValidTypeAuth(DIGEST)){
            return false;
        }
        // Validar si esta permitido el algoritmo "MD5-sess"        
        if (!isValidAlgoritm("MD5-sess")){
            return false;
        }
        ServerAuth serverAuth = getResponseAuth(clientAuth.getNonce());
        //Check nonce, opaque, realm, type
        if (!compareServerAndClientAuth(clientAuth, serverAuth)) {
            return false;
        }
        String result;
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
                ha2 = DigestUtil.md5(clientAuth.getMethod() + ":" + clientAuth.getUri() + ":" + DigestUtil.md5(clientAuth.getEntityBody()));
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
     * Compara valores del objeto autenticador generado en el server y el objeto
     * enviado por el cliente.
     *
     * @param requestAuth objeto autenticador enviado desde el cliente.
     * @param responseAuth objeto autenticador generado en el servidor.
     * @return verdadero o falso si son iguales o difieren en algún dato.
     */
    public boolean compareServerAndClientAuth(ClientAuth requestAuth, ServerAuth responseAuth) {
        if (responseAuth == null) {
            return false;
        }
        if (!isValidQop(requestAuth.getQop())){
            return false;
        }
        if (!responseAuth.getType().equals(requestAuth.getType())) {
            return false;
        }
        if (!responseAuth.getRealm().equals(requestAuth.getRealm())) {
            return false;
        }
        if (responseAuth.getNonceCount() != 0 || !isNullorEmpty(requestAuth.getNonceCount())) {
            if (!requestAuth.getQop().isEmpty() && responseAuth.getNonceCount() != Integer.parseInt(requestAuth.getNonceCount())) {
                return false;
            }
        }
        return responseAuth.getNonce().equals(requestAuth.getNonce());
    }

    /**
     * Tipo de autenticación ("Basic", "Digest")
     *
     * @return tipo de autenticación.
     */
    public String getType() {
        return type;
    }

    /**
     * Setea tipo de autenticación. Valores válidos positbles "Basic" o "Digest"
     *
     * @param type tipo de autenticación.
     * @throws org.javabeanstack.exceptions.TypeAuthInvalid
     */
    public void setType(String type) throws TypeAuthInvalid {
        // Validar si esta permitido el tipo de autenticación "Digest"                
        this.type = type;
        if (!isValidTypeAuth(type)){
            throw new TypeAuthInvalid();
        }
    }

    /**
     * Entorno, grupo o base donde autenticar la petición.
     *
     * @return Entorno, grupo o base donde autenticar la petición.
     */
    public String getRealm() {
        return realm;
    }

    /**
     * Setea la propiedad realm
     *
     * @param realm Entorno, grupo o base donde autenticar la petición.
     */
    public void setRealm(String realm) {
        this.realm = realm;
    }

    /**
     * Devuelve la propiedad qop (quality of protection)
     *
     * @return propiedad qop
     */
    public String getQop() {
        return this.qop;
    }

    /**
     * Setea la propiedad qoq (valores posibles "","auth","auth-int")
     *
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
     *
     * @param numberCanFail
     */
    public void setNumberCanFail(int numberCanFail) {
        this.numberCanFail = numberCanFail;
    }

    /**
     * Devuelve Cantidad de segundos antes de considerarse el autenticador como
     * obsoleto
     *
     * @return Cantidad de segundos antes de considerarse el autenticador como
     * obsoleto
     */
    public int getSecondsIdle() {
        return secondsIdle;
    }

    /**
     * Setea cantidad de segundos la cual se considera válido el autenticador
     * creado.
     *
     * @param secondsIdle
     */
    public void setSecondsIdle(int secondsIdle) {
        this.secondsIdle = secondsIdle;
    }

    /**
     * Devuelve los tipos de autenticaciones válidos (Basic,Digest)
     *
     * @return tipos de autenticaciones válidas
     */
    public List<String> getTypeAuthValids() {
        return typeAuthValids;
    }

    /**
     * Setea los tipos de Autenticaciones válidos
     *
     * @param typeAuthValid tipos de autenticaciones (Basic, Digest)
     */
    public void setTypeAuthValids(List<String> typeAuthValid) {
        this.typeAuthValids = typeAuthValid;
    }

    /**
     * Devuelve los algoritmos de autenticaciones válidos (MD5, MD5-sess)
     *
     * @return algoritmos de autenticaciones válidos
     */
    public List<String> getAlgorithmValids() {
        return algorithmValids;
    }

    /**
     * Setea los algoritmos válidos
     *
     * @param algorithmValids algoritmos válidos (MD5, MD5-sess)
     */
    public void setAlgorithmValids(List<String> algorithmValids) {
        this.algorithmValids = algorithmValids;
    }

    /**
     * Devuelve los qops válidos ("","auth","auth-int")
     *
     * @return qops válidos
     */
    public List<String> getQopValids() {
        return qopValids;
    }

    /**
     * Setea los qops válidos
     *
     * @param qopValids qops válidos ("","auth","auth-int")
     */
    public void setQopValids(List<String> qopValids) {
        this.qopValids = qopValids;
    }
}
