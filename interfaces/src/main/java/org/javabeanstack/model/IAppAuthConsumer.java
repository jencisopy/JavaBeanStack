/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
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
package org.javabeanstack.model;
import java.time.LocalDateTime;
import org.javabeanstack.data.IDataRow;
/**
 * Contrato de la entidad consumidor de autenticación (OAuth): su clave, claves
 * criptográficas, algoritmos, estado y URLs de autorización/token/callback.
 * La gestiona {@link org.javabeanstack.security.IOAuthConsumer}. Extiende
 * {@link IDataRow}.
 *
 * @author Jorge Enciso
 */
public interface IAppAuthConsumer extends IDataRow {
    /**
     * Devuelve la clave del consumidor.
     * @return clave del consumidor.
     */
    String getConsumerKey();

    /**
     * Asigna la clave del consumidor.
     * @param consumerKey clave del consumidor.
     */
    void setConsumerKey(String consumerKey);

    /**
     * Devuelve el nombre del consumidor.
     * @return nombre del consumidor.
     */
    String getConsumerName();

    /**
     * Asigna el nombre del consumidor.
     * @param consumerName nombre del consumidor.
     */
    void setConsumerName(String consumerName);

    /**
     * Devuelve la fecha de expiración del consumidor.
     * @return fecha de expiración.
     */
    LocalDateTime getExpiredDate();

    /**
     * Asigna la fecha de expiración del consumidor.
     * @param expiredDate fecha de expiración.
     */
    void setExpiredDate(LocalDateTime expiredDate);

    /**
     * Devuelve la clave pública del consumidor.
     * @return clave pública.
     */
    String getPublicKey();

    /**
     * Asigna la clave pública del consumidor.
     * @param publicKey clave pública.
     */
    void setPublicKey(String publicKey);

    /**
     * Devuelve la clave privada del consumidor.
     * @return clave privada.
     */
    String getPrivateKey();

    /**
     * Asigna la clave privada del consumidor.
     * @param privateKey clave privada.
     */
    void setPrivateKey(String privateKey);

    /**
     * Devuelve el algoritmo de firma.
     * @return algoritmo de firma.
     */
    String getSignatureAlgorithm();

    /**
     * Asigna el algoritmo de firma.
     * @param algorithm algoritmo de firma.
     */
    void setSignatureAlgorithm(String algorithm);

    /**
     * Devuelve el algoritmo de cifrado.
     * @return algoritmo de cifrado.
     */
    String getCryptoAlgorithm();

    /**
     * Asigna el algoritmo de cifrado.
     * @param algorithm algoritmo de cifrado.
     */
    void setCryptoAlgorithm(String algorithm);

    /**
     * Indica si el consumidor está bloqueado.
     * @return verdadero si está bloqueado.
     */
    Boolean getBlocked();

    /**
     * Asigna si el consumidor está bloqueado.
     * @param blocked verdadero para bloquearlo.
     */
    void setBlocked(Boolean blocked);

    /**
     * Devuelve la URL de autorización.
     * @return URL de autorización.
     */
    String getAuthURL();

    /**
     * Asigna la URL de autorización.
     * @param authURL URL de autorización.
     */
    void setAuthURL(String authURL);

    /**
     * Devuelve la URL de token.
     * @return URL de token.
     */
    String getTokenURL();

    /**
     * Asigna la URL de token.
     * @param tokenURL URL de token.
     */
    void setTokenURL(String tokenURL);

    /**
     * Devuelve la URL de callback.
     * @return URL de callback.
     */
    String getCallbackURL();

    /**
     * Asigna la URL de callback.
     * @param callbackURL URL de callback.
     */
    void setCallbackURL(String callbackURL);

    /**
     * Devuelve el alcance (scope) del consumidor.
     * @return alcance del consumidor.
     */
    String getScope();

    /**
     * Asigna el alcance (scope) del consumidor.
     * @param scope alcance del consumidor.
     */
    void setScope(String scope);
}
