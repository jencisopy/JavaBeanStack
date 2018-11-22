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

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.ejb.EJB;
import org.apache.log4j.Logger;
import org.javabeanstack.crypto.CipherUtil;
import org.javabeanstack.crypto.DigestUtil;
import org.javabeanstack.data.IDataResult;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.model.IAppAuthConsumer;
import org.javabeanstack.model.IAppAuthConsumerToken;
import org.javabeanstack.services.IDataService;
import org.javabeanstack.util.Fn;

/**
 *
 * @author Jorge Enciso
 */
public abstract class OAuthConsumer implements IOAuthConsumer {
    private static final Logger LOGGER = Logger.getLogger(OAuthConsumer.class);

    @EJB
    private IDataService dao;

    /**
     * Asigna objeto para la conexión con la base de datos
     * @param dao objeto para gestionar la consulta y grabación de los datos.
     */
    @Override
    public void setDao(IDataService dao){
        this.dao = dao;
    }
    
    /**
     * Busca un registro AppAuthConsumer dado un consumerKey
     * @param consumerKey clave del consumidor
     * @return registro AppAuthConsumer
     */
    protected final IAppAuthConsumer findAuthConsumer(String consumerKey){
        String queryString = "select o from AppConsumer where consumerKey = :consumerKey";
        Map<String, Object> parameters = new HashMap();
        parameters.put("consumerKey",consumerKey);
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
     * @param token 
     * @return registro AppAuthConsumerToken
     */
    protected final IAppAuthConsumerToken findToken(String token){
        String queryString = "select o from AppConsumerToken where token = :token";
        Map<String, Object> parameters = new HashMap();
        parameters.put("token",token);
        try {
            IAppAuthConsumerToken auth = dao.findByQuery(null, queryString, parameters);
            return auth;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }


    /**
     * Busca un registro AppAuthConsumerToken dado un consumerKey y la clave del token
     * @param consumerKey clave del consumidor
     * @param tokenSecret clave del token.
     * @return registro AppAuthConsumerToken
     */
    protected final IAppAuthConsumerToken findToken(String consumerKey, String tokenSecret){
        String queryString = "select o from AppConsumerToken where consumerKey = :consumerKey and tokenSecret = :tokenSecret";
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
     * @param consumerName nombre del consumidor
     * @param expiredDate  fecha de expiración del registro
     * @return 
     */
    @Override
    public boolean createAuthConsumer(String consumerName, Date expiredDate){
        try {
            IAppAuthConsumer authConsumer = getAuthConsumer().getClass().newInstance();            
            authConsumer.setConsumerName(consumerName);
            authConsumer.setExpiredDate(expiredDate);
            authConsumer.setConsumerKey(createConsumerKey(authConsumer));
            IDataResult dataResult = dao.persist(null, authConsumer);
            return dataResult.isSuccessFul();
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return false;
    }

    /**
     * Elimina un registro de AuthConsumer de la base de datos.
     * @param consumerKey clave del consumidor
     * @return verdadero si tuvo exito y falso si no
     */
    @Override
    public boolean dropAuthConsumer(String consumerKey){
        IAppAuthConsumer appConsumer = findAuthConsumer(consumerKey);
        if (appConsumer == null){
            return false;
        }
        try {        
            IDataResult dataResult = dao.remove(null, appConsumer);
            return dataResult.isSuccessFul();
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return false;
    }
    
    /**
     * Devuelve un token dado un consumerKey y un tokenSecret.
     * @param consumerKey clave del consumidor
     * @param tokenSecret clave del token.
     * @return token
     */
    @Override
    public final String getToken(String consumerKey, String tokenSecret){
        IAppAuthConsumerToken authConsumerToken = findToken(consumerKey, tokenSecret);
        if (authConsumerToken != null){
            return authConsumerToken.getToken();
        }
        return null;
    }
    
    @Override
    public boolean requestToken(String consumerKey){
        try {
            IAppAuthConsumerToken appConsumerToken = getAuthConsumerToken().getClass().newInstance();            
            appConsumerToken.setAppAuthConsumer(findAuthConsumer(consumerKey));
            appConsumerToken.setBlocked(true);
            //appConsumerToken.setToken(token);
            //appConsumerToken.setTokenSecret(token);
            IDataResult dataResult = dao.persist(null, appConsumerToken);
            return dataResult.isSuccessFul();
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return false;
    }
    
    @Override
    public String createToken(String consumerKey){
        return null;
    }

    /**
     * Elimina un token de la base de datos.
     * @param consumerKey clave del consumidor.
     * @param tokenSecret clave del token.
     * @return verdadero si tuvo exito y falso si no.
     */
    @Override
    public boolean dropToken(String consumerKey, String tokenSecret){
        IAppAuthConsumerToken authConsumerToken = findToken(consumerKey, tokenSecret);
        if (authConsumerToken == null){
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
     * Crea un consumerKey y configura el registro AppAuthConsumer para que valide el consumerKey
     * @param authConsumer modelo AppAuthConsumer
     * @return el consumerKey
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     * @throws InvalidKeyException 
     */
    protected String createConsumerKey(IAppAuthConsumer authConsumer) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException{
        String consumer ;
        String method = CipherUtil.BLOWFISH+"_HmacSHA256";
        authConsumer.setSignatureMethod(method);
        
        SecretKey privateKey = CipherUtil.getSecureRandomKey(CipherUtil.BLOWFISH, 256);        
        String encodedKey = Fn.bytesToBase64(privateKey.getEncoded());        
        authConsumer.setPrivateKey(encodedKey);        
        authConsumer.setPublicKey(null);        
        
        consumer = authConsumer.getConsumerName();
        consumer += ":"+authConsumer.getExpiredDate();
        consumer += ":"+authConsumer.getSignatureMethod();
        
        String consumerKey = DigestUtil.digestHmacToBase64Url("HmacSHA256", consumer, privateKey.getEncoded());
        return consumerKey;
    }
    
    /**
     * Convierte la clave en formato base64 al tipo SecretKey
     * @param encodeKey clave en formato base64
     * @param algorithm tipo de algoritmo
     * @return clave tipo SecretKey
     */
    protected SecretKey getSecretKey(String encodeKey, String algorithm){
        // decode the base64 encoded string
        byte[] decodedKey = Fn.base64ToBytes(encodeKey);
        // rebuild key using SecretKeySpec
        SecretKey originalKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, algorithm);        
        return originalKey;
    }
}
