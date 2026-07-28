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
 * Contrato de la entidad token de autenticación: el token emitido a un
 * dispositivo/usuario por un consumidor ({@link IAppAuthConsumer}), con su
 * secreto, datos, estado y datos del usuario asociado. Extiende {@link IDataRow}.
 *
 * @author Jorge Enciso
 */
public interface IAppAuthConsumerToken extends IDataRow {
    /**
     * Devuelve el identificador del dispositivo (uuid).
     * @return uuid del dispositivo.
     */
    String getUuidDevice();

    /**
     * Asigna el identificador del dispositivo (uuid).
     * @param uuidDevice uuid del dispositivo.
     */
    void setUuidDevice(String uuidDevice);

    /**
     * Devuelve el token.
     * @return token.
     */
    String getToken();

    /**
     * Asigna el token.
     * @param tokenKey token.
     */
    void setToken(String tokenKey);

    /**
     * Devuelve el secreto del token.
     * @return secreto del token.
     */
    String getTokenSecret();

    /**
     * Asigna el secreto del token.
     * @param tokenSecret secreto del token.
     */
    void setTokenSecret(String tokenSecret);

    /**
     * Devuelve los datos incluidos en el token.
     * @return datos del token.
     */
    String getData();

    /**
     * Asigna los datos incluidos en el token.
     * @param data datos del token.
     */
    void setData(String data);

    /**
     * Indica si el token está bloqueado.
     * @return verdadero si está bloqueado.
     */
    Boolean getBlocked();

    /**
     * Asigna si el token está bloqueado.
     * @param blocked verdadero para bloquearlo.
     */
    void setBlocked(Boolean blocked);

    /**
     * Indica si el token está marcado como eliminado.
     * @return verdadero si está eliminado.
     */
    Boolean getDeleted();

    /**
     * Asigna si el token está marcado como eliminado.
     * @param deleted verdadero para marcarlo como eliminado.
     */
    void setDeleted(Boolean deleted);

    /**
     * Devuelve el nombre del usuario asociado al token.
     * @return nombre del usuario.
     */
    String getUserName();

    /**
     * Asigna el nombre del usuario asociado al token.
     * @param userName nombre del usuario.
     */
    void setUserName(String userName);

    /**
     * Devuelve el correo del usuario asociado al token.
     * @return correo del usuario.
     */
    String getUserEmail();

    /**
     * Asigna el correo del usuario asociado al token.
     * @param userEmail correo del usuario.
     */
    void setUserEmail(String userEmail);

    /**
     * Devuelve la empresa para la cual se emitió el token.
     * @return identificador de la empresa.
     */
    Long getIdcompany();

    /**
     * Asigna la empresa para la cual se emitió el token.
     * @param idcompany identificador de la empresa.
     */
    void setIdcompany(Long idcompany);

    /**
     * Devuelve el código del usuario dueño del token (relación lógica con
     * el atributo code de la entidad usuario).
     * @return código del usuario.
     */
    String getUserCode();

    /**
     * Asigna el código del usuario dueño del token.
     * @param userCode código del usuario.
     */
    void setUserCode(String userCode);

    /**
     * Devuelve la fecha y hora del último uso del token.
     * @return fecha del último uso.
     */
    LocalDateTime getLastUsed();

    /**
     * Asigna la fecha y hora del último uso del token.
     * @param lastUsed fecha del último uso.
     */
    void setLastUsed(LocalDateTime lastUsed);

    /**
     * Devuelve la clave del consumidor que emitió el token.
     * @return clave del consumidor.
     */
    String getConsumerKey();

    /**
     * Devuelve el nombre del consumidor que emitió el token.
     * @return nombre del consumidor.
     */
    String getConsumerName();

    /**
     * Devuelve la fecha de expiración del token.
     * @return fecha de expiración.
     */
    LocalDateTime getExpiredDate();

    /**
     * Devuelve el consumidor que emitió el token.
     * @return consumidor emisor.
     */
    IAppAuthConsumer getAppAuthConsumerKey();

    /**
     * Asigna el consumidor que emitió el token.
     * @param authConsumer consumidor emisor.
     */
    void setAppAuthConsumerKey(IAppAuthConsumer authConsumer);
}
