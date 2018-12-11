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

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.ejb.EJB;
import org.apache.log4j.Logger;
import org.javabeanstack.crypto.CipherUtil;
import org.javabeanstack.crypto.DigestUtil;
import org.javabeanstack.data.IDBFilter;
import org.javabeanstack.data.IDataResult;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.model.IAppAuthConsumer;
import org.javabeanstack.model.IAppAuthConsumerToken;
import org.javabeanstack.data.services.IDataService;
import org.javabeanstack.model.IAppCompany;
import org.javabeanstack.model.IAppUser;
import org.javabeanstack.util.Fn;
import static org.javabeanstack.util.Fn.nvl;

/**
 *
 * @author Jorge Enciso
 */
public abstract class OAuthConsumerBase implements IOAuthConsumer {
    private static final Logger LOGGER = Logger.getLogger(OAuthConsumerBase.class);

    @EJB
    private IDataService dao;

    private IAppAuthConsumer lastAuthConsumer;
    private IAppAuthConsumerToken lastAuthConsumerToken;

    /**
     * Asigna objeto para la conexión con la base de datos
     *
     * @param dao objeto para gestionar la consulta y grabación de los datos.
     */
    @Override
    public void setDao(IDataService dao) {
        this.dao = dao;
    }

    public IAppAuthConsumer getLastAuthConsumer() {
        return lastAuthConsumer;
    }

    public IAppAuthConsumerToken getLastAuthConsumerToken() {
        return lastAuthConsumerToken;
    }

    /**
     * Busca un registro AppAuthConsumer dado un consumerKey
     *
     * @param consumerKey clave del consumidor
     * @return registro AppAuthConsumer
     */
    protected IAppAuthConsumer findAuthConsumer(String consumerKey) {
        String queryString = "select o from AppAuthConsumer o where consumerKey = :consumerKey";
        Map<String, Object> parameters = new HashMap();
        parameters.put("consumerKey", consumerKey);
        try {
            IAppAuthConsumer auth = dao.findByQuery(null, queryString, parameters);
            return auth;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    /**
     * Busca un registro AppAuthConsumerToken dado un token
     *
     * @param token
     * @return registro AppAuthConsumerToken
     */
    @Override
    public IAppAuthConsumerToken findAuthToken(String token) {
        String queryString = "select o from AppAuthConsumerToken o where token = :token";
        Map<String, Object> parameters = new HashMap();
        parameters.put("token", token);
        try {
            IAppAuthConsumerToken auth = dao.findByQuery(null, queryString, parameters);
            return auth;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    /**
     * Busca un registro AppAuthConsumerToken dado un consumerKey y la clave del
     * token
     *
     * @param consumerKey clave del consumidor
     * @param tokenSecret clave del token.
     * @return registro AppAuthConsumerToken
     */
    protected IAppAuthConsumerToken findAuthToken(String consumerKey, String tokenSecret) {
        String queryString = "select o from AppAuthConsumerToken o where appAuthConsumer.consumerKey = :consumerKey and tokenSecret = :tokenSecret";
        Map<String, Object> parameters = new HashMap();
        parameters.put("consumerKey", consumerKey);
        parameters.put("tokenSecret", tokenSecret);
        try {
            IAppAuthConsumerToken auth = dao.findByQuery(null, queryString, parameters);
            return auth;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    /**
     * Crea y guarda en la base de datos el registro de AuthConsumer
     *
     * @param consumerName nombre del consumidor
     * @param expiredDate fecha de expiración del registro
     * @return
     */
    @Override
    public boolean createAuthConsumer(String consumerName, Date expiredDate) {
        try {
            IAppAuthConsumer authConsumer = getAuthConsumerClass().newInstance();
            authConsumer.setConsumerName(consumerName);
            authConsumer.setExpiredDate(expiredDate);
            authConsumer.setConsumerKey(createConsumerKey(authConsumer));
            IDataResult dataResult = dao.persist(null, authConsumer);
            if (dataResult.isSuccessFul()) {
                lastAuthConsumer = dataResult.getRowUpdated();
            }
            return dataResult.isSuccessFul();
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return false;
    }

    /**
     * Elimina un registro de AuthConsumer de la base de datos.
     *
     * @param consumerKey clave del consumidor
     * @return verdadero si tuvo exito y falso si no
     */
    @Override
    public boolean dropAuthConsumer(String consumerKey) {
        IAppAuthConsumer authConsumer = findAuthConsumer(consumerKey);
        if (authConsumer == null) {
            return false;
        }
        try {
            IDataResult dataResult = dao.remove(null, authConsumer);
            return dataResult.isSuccessFul();
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return false;
    }

    /**
     * Devuelve un token dado un consumerKey y un tokenSecret.
     *
     * @param consumerKey clave del consumidor
     * @param tokenSecret clave del token.
     * @return token
     */
    @Override
    public final String getToken(String consumerKey, String tokenSecret) {
        IAppAuthConsumerToken authConsumerToken = findAuthToken(consumerKey, tokenSecret);
        if (authConsumerToken != null) {
            return authConsumerToken.getToken();
        }
        return "";
    }

    /**
     * Graba una solicitud de token, debe completarse el proceso en otro
     * programa.
     *
     * @param consumerKey clave del consumidor
     * @return verdadero si tuvo exito y falso si no.
     */
    @Override
    public boolean requestToken(String consumerKey) {
        try {
            IAppAuthConsumerToken authConsumerToken = getAuthConsumerTokenClass().newInstance();
            authConsumerToken.setAppAuthConsumer(findAuthConsumer(consumerKey));
            authConsumerToken.setBlocked(true);
            String token = getRandomToken();
            authConsumerToken.setToken(token);
            authConsumerToken.setTokenSecret(token);
            IDataResult dataResult = dao.persist(null, authConsumerToken);
            return dataResult.isSuccessFul();
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return false;
    }

    /**
     * Crea y graba en la base de datos el registro de un token de autorización
     *
     * @param consumerKey clave del consumidor.
     * @param data información del token.
     * @return valor del token.
     */
    @Override
    public String createToken(String consumerKey, IOAuthConsumerData data) {
        try {
            IAppAuthConsumerToken authConsumerToken = getAuthConsumerTokenClass().newInstance();
            authConsumerToken.setAppAuthConsumer(findAuthConsumer(consumerKey));
            authConsumerToken.setBlocked(false);
            authConsumerToken.setData(data.toString());
            String token = signTokenData(authConsumerToken);
            authConsumerToken.setToken(token);
            String tokenSecret = getTokenSecret(authConsumerToken);
            authConsumerToken.setTokenSecret(tokenSecret);
            IDataResult dataResult = dao.persist(null, authConsumerToken);
            if (!dataResult.isSuccessFul()) {
                return "";
            }
            lastAuthConsumerToken = dataResult.getRowUpdated();
            return authConsumerToken.getToken();
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return "";
    }

    /**
     * Crea un token válido.
     *
     * @param consumerKey clave del consumidor.
     * @param data datos del token.
     * @return valor del token si tuvo exito.
     */
    @Override
    public String createToken(String consumerKey, Map<String, String> data) {
        try {
            IAppAuthConsumerToken authConsumerToken = getAuthConsumerTokenClass().newInstance();
            authConsumerToken.setAppAuthConsumer(findAuthConsumer(consumerKey));
            authConsumerToken.setBlocked(false);
            authConsumerToken.setData(getDataString(data));
            String token = signTokenData(authConsumerToken);
            authConsumerToken.setToken(token);
            authConsumerToken.setTokenSecret(token);
            IDataResult dataResult = dao.persist(null, authConsumerToken);
            if (!dataResult.isSuccessFul()) {
                return null;
            }
            lastAuthConsumerToken = dataResult.getRowUpdated();
            return authConsumerToken.getToken();
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    /**
     * Convierte una map<String,String> a un formato standard string para
     * guardar en la base
     *
     * @param data map con los datos.
     * @return dato convertido.
     */
    protected String getDataString(Map<String, String> data) {
        String result = "";
        for (Map.Entry<String, String> entry : data.entrySet()) {
            result += entry.getKey().trim() + "=" + entry.getValue().trim() + "\n";
        }
        return result;
    }

    /**
     * Elimina un token de la base de datos.
     *
     * @param consumerKey clave del consumidor.
     * @param tokenSecret clave del token.
     * @return verdadero si tuvo exito y falso si no.
     */
    @Override
    public boolean dropToken(String consumerKey, String tokenSecret) {
        IAppAuthConsumerToken authConsumerToken = findAuthToken(consumerKey, tokenSecret);
        if (authConsumerToken == null) {
            return false;
        }
        try {
            IDataResult dataResult = dao.remove(null, authConsumerToken);
            return dataResult.isSuccessFul();
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return false;
    }

    /**
     * Crea un consumerKey y configura el registro AppAuthConsumer para que
     * valide el consumerKey
     *
     * @param authConsumer modelo AppAuthConsumer
     * @return el consumerKey
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     * @throws InvalidKeyException
     */
    protected String createConsumerKey(IAppAuthConsumer authConsumer) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
        String consumer;
        // Seleccionar tipo de algoritmo para firmar y para encriptar
        authConsumer.setCryptoAlgorithm(CipherUtil.BLOWFISH);
        authConsumer.setSignatureAlgorithm(DigestUtil.SHA1);
        // Generación de la clave en forma aleatoria
        SecretKey privateKey = CipherUtil.getSecureRandomKey(CipherUtil.BLOWFISH, 256);
        String encodedKey = Fn.bytesToBase64(privateKey.getEncoded());
        authConsumer.setPrivateKey(encodedKey);
        authConsumer.setPublicKey(null);

        consumer = authConsumer.getConsumerName();
        consumer += ":" + authConsumer.getExpiredDate();
        consumer += ":" + authConsumer.getSignatureAlgorithm();

        MessageDigest digest = MessageDigest.getInstance(DigestUtil.SHA1);
        byte[] digestMessage = digest.digest(consumer.getBytes("UTF-8"));
        return Fn.bytesToBase64Url(digestMessage);
    }

    /**
     * Convierte la clave en formato base64 al tipo SecretKey
     *
     * @param encodeKey clave en formato base64
     * @param algorithm tipo de algoritmo
     * @return clave tipo SecretKey
     */
    protected SecretKey getSecretKey(String encodeKey, String algorithm) {
        // decode the base64 encoded string
        byte[] decodedKey = Fn.base64ToBytes(encodeKey);
        // rebuild key using SecretKeySpec
        SecretKey originalKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, algorithm);
        return originalKey;
    }

    /**
     * Calcula el valor del token
     *
     * @param model detalle de la información del token.
     * @return token
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     * @throws InvalidKeyException
     */
    protected String signTokenData(IAppAuthConsumerToken model) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
        if (model == null || model.getAppAuthConsumer() == null) {
            return null;
        }
        String data;
        data = model.getAppAuthConsumer().getConsumerKey();
        data += ":" + model.getAppAuthConsumer().getExpiredDate();
        data += ":" + model.getData();
        String algorithm = model.getAppAuthConsumer().getSignatureAlgorithm();

        MessageDigest digest = MessageDigest.getInstance(algorithm);
        byte[] digestMessage = digest.digest(data.getBytes("UTF-8"));
        return Fn.bytesToBase64Url(digestMessage);
    }

    /**
     * Calcula el valor de la clave del token
     *
     * @param model detalle de la información del token.
     * @return clavel del token
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     * @throws InvalidKeyException
     */
    protected String getTokenSecret(IAppAuthConsumerToken model) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
        if (model == null || model.getAppAuthConsumer() == null) {
            return null;
        }
        String data;
        data = model.getAppAuthConsumer().getConsumerKey();
        data += ":" + model.getAppAuthConsumer().getExpiredDate();
        data += ":" + model.getData();
        data += ":" + model.getToken(); // Debio calcularse antes
        String algorithm = model.getAppAuthConsumer().getSignatureAlgorithm();

        MessageDigest digest = MessageDigest.getInstance(algorithm);
        byte[] digestMessage = digest.digest(data.getBytes("UTF-8"));
        return Fn.bytesToBase64Url(digestMessage);
    }

    /**
     * Genera un valor aleatorio para un token temporal.
     *
     * @return valor aleatorio para un token temporal.
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    protected String getRandomToken() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        Integer random = (int) (Math.random() * 1000000 + 1);
        MessageDigest digest = MessageDigest.getInstance(DigestUtil.SHA1);
        byte[] digestMessage = digest.digest(random.toString().getBytes());
        return Fn.bytesToBase64Url(digestMessage);
    }

    /**
     * Determina si un token es válido o no
     * @param token token
     * @return verdadero si es válido y falso si no.
     */
    @Override
    public boolean isValidToken(String token) {
        IAppAuthConsumerToken result = findAuthToken(token);
        if (result == null){
            return false;
        }
        return !result.getBlocked();
    }

    /**
     * Devuelve el valor de una propiedad que se encuentra en el campo "data" de 
     * appauthconsumertoken
     * 
     * @param token  token
     * @param property propiedad
     * @return valor de la propiedad si existe y vacio si no.
     */
    @Override
    public String getDataKeyValue(String token, String property) {
        try {
            IAppAuthConsumerToken tokenRecord = findAuthToken(token);
            Properties prop = new Properties();
            prop.load(new StringReader(tokenRecord.getData()));
            return nvl((String)prop.getProperty(property),"");
        } catch (IOException ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return "";
    }

    /**
     * Devuelve el valor de una propiedad que se encuentra en el campo "data" de 
     * appauthconsumertoken
     * 
     * @param token  token
     * @param property propiedad
     * @return valor de la propiedad si existe y vacio si no.
     */
    @Override
    public String getDataKeyValue(IAppAuthConsumerToken token, String property) {
        try {
            Properties prop = new Properties();
            prop.load(new StringReader(token.getData()));
            return nvl((String)prop.getProperty(property),"");
        } catch (IOException ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return "";
    }
    
    /**
     * Devuelve el objeto AppUser mapeado a este token
     * @param token token
     * @return objeto AppUser mapeado al token o nulo si el token es inválido o
     * el user no existe.
     */
    @Override
    public IAppUser getUserMapped(String token) {
        if (!isValidToken(token)){
            return null;
        }
        try{
            Long iduser = Long.parseLong(getDataKeyValue(token,"idappuser"));            
            IAppUser user = dao.findByQuery(null,"select o from AppUser o where iduser = "+iduser,null);
            return user;
        }
        catch (Exception exp){
            ErrorManager.showError(exp, LOGGER);
        }
        return null;
    }

    /**
     * Devuelve el objeto AppCompany mapeado a este token
     * @param token token
     * @return objeto AppCompany mapeado al token o nulo si el token es inválido o
     * el user no existe.
     */
    @Override
    public IAppCompany getCompanyMapped(IAppAuthConsumerToken token) {
        try{
            if (!isValidToken(token.getToken())){
                return null;
            }
            Long idcompany = Long.parseLong(getDataKeyValue(token,"idcompany"));
            IAppCompany company = dao.findByQuery(null,"select o from AppCompany o where idcompany = "+idcompany,null);
            return company;
        }
        catch (Exception exp){
            ErrorManager.showError(exp, LOGGER);
        }
        return null;
    }

    /**
     * Devuelve el objeto AppCompany mapeado a este token
     * @param token token
     * @return objeto AppCompany mapeado al token o nulo si el token es inválido o
     * el user no existe.
     */
    @Override
    public IAppCompany getCompanyMapped(String token) {
        try{
            if (!isValidToken(token)){
                return null;
            }
            IAppAuthConsumerToken tokenRecord = findAuthToken(token);
            Long idcompany = Long.parseLong(getDataKeyValue(tokenRecord,"idcompany"));
            IAppCompany company = dao.findByQuery(null,"select o from AppCompany o where idcompany = "+idcompany,null);
            return company;
        }
        catch (Exception exp){
            ErrorManager.showError(exp, LOGGER);
        }
        return null;
    }
    
    @Override
    public IDBFilter getDBFilter(IAppAuthConsumerToken token){
        // Implementar
        return null;
    }
}
