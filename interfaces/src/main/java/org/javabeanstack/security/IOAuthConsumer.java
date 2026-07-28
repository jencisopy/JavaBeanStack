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
package org.javabeanstack.security;

import java.time.LocalDateTime;
import java.util.List;
import org.javabeanstack.data.IDBFilter;
import org.javabeanstack.model.IAppAuthConsumer;
import org.javabeanstack.data.services.IDataService;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.model.IAppCompany;
import org.javabeanstack.model.IAppUser;
import org.javabeanstack.model.IAppAuthConsumerToken;

/**
 * Contrato de la gestión de consumidores OAuth y sus tokens de acceso.
 *
 * <p>Permite crear y dar de baja consumidores ({@link IAppAuthConsumer}),
 * emitir, validar y revocar tokens ({@link IAppAuthConsumerToken}), y resolver
 * a partir de un token el usuario, la empresa, las empresas habilitadas y el
 * filtro de datos aplicable. La implementación de referencia es
 * {@code org.javabeanstack.security.OAuthConsumer}.</p>
 *
 * @author Jorge Enciso
 */
public interface IOAuthConsumer {
    /**
     * Crea un consumidor de autenticación y devuelve su clave.
     *
     * @param consumerName nombre del consumidor.
     * @param expiredDate fecha de expiración del consumidor.
     * @return clave del consumidor creada.
     */
    String createAuthConsumer(String consumerName, LocalDateTime expiredDate);

    /**
     * Persiste un consumidor de autenticación.
     *
     * @param authConsumer consumidor a crear.
     * @return consumidor persistido.
     */
    IAppAuthConsumer createAuthConsumer(IAppAuthConsumer authConsumer);

    /**
     * Emite un token para el consumidor indicado.
     *
     * @param consumerKey clave del consumidor.
     * @param data datos de autenticación a incluir en el token.
     * @return token emitido.
     * @throws Exception si la emisión falla.
     */
    String createToken(String consumerKey, IOAuthConsumerData data)  throws Exception ;

    /**
     * Emite un token para el consumidor y dispositivo indicados.
     *
     * @param consumerKey clave del consumidor.
     * @param data datos de autenticación.
     * @param uuidDevice identificador del dispositivo.
     * @return token emitido.
     * @throws Exception si la emisión falla.
     */
    String createToken(String consumerKey, IOAuthConsumerData data, String uuidDevice)  throws Exception;

    /**
     * Emite un token asociando también el nombre y correo del usuario.
     *
     * @param consumerKey clave del consumidor.
     * @param data datos de autenticación.
     * @param uuidDevice identificador del dispositivo.
     * @param userName nombre del usuario.
     * @param userEmail correo del usuario.
     * @return token emitido.
     * @throws Exception si la emisión falla.
     */
    String createToken(String consumerKey, IOAuthConsumerData data, String uuidDevice, String userName, String userEmail)  throws Exception;

    /**
     * Emite un token a partir de una entidad token ya construida.
     *
     * @param authConsumerToken entidad token del consumidor.
     * @return token emitido.
     * @throws Exception si la emisión falla.
     */
    String createToken(IAppAuthConsumerToken authConsumerToken) throws Exception;

    /**
     * Busca un consumidor por su clave.
     *
     * @param consumerKey clave del consumidor.
     * @return consumidor, o {@code null} si no existe.
     */
    IAppAuthConsumer findAuthConsumer(String consumerKey);

    /**
     * Busca la entidad token asociada a un token.
     *
     * @param token token de acceso.
     * @return entidad token, o {@code null} si no existe.
     */
    IAppAuthConsumerToken findAuthToken(String token);

    /**
     * Busca la entidad token por consumidor y uuid/secreto.
     *
     * @param consumerKey clave del consumidor.
     * @param uuidOrTokenSecret uuid del dispositivo o secreto del token.
     * @return entidad token, o {@code null} si no existe.
     */
    IAppAuthConsumerToken findAuthToken(String consumerKey, String uuidOrTokenSecret);

    /**
     * Busca la entidad token por consumidor, uuid/secreto y empresa. Un mismo
     * dispositivo puede tener tokens simultáneos para empresas distintas; con
     * {@code idcompany} nulo delega en la sobrecarga de dos argumentos.
     *
     * @param consumerKey clave del consumidor.
     * @param uuidOrTokenSecret uuid del dispositivo o secreto del token.
     * @param idcompany identificador de la empresa del token.
     * @return entidad token, o {@code null} si no existe.
     */
    IAppAuthConsumerToken findAuthToken(String consumerKey, String uuidOrTokenSecret, Long idcompany);

    /**
     * Da de baja un consumidor.
     *
     * @param consumerKey clave del consumidor.
     * @return verdadero si se dio de baja, falso si no.
     */
    boolean dropAuthConsumer(String consumerKey);

    /**
     * Revoca un token.
     *
     * @param consumerKey clave del consumidor.
     * @param uuidOrTokenSecret uuid del dispositivo o secreto del token.
     * @return verdadero si se revocó, falso si no.
     */
    boolean dropToken(String consumerKey, String uuidOrTokenSecret);

    /**
     * Elimina un token identificándolo también por empresa. Un dispositivo
     * puede tener tokens simultáneos para empresas distintas: sin la empresa
     * la búsqueda es ambigua y no se elimina ninguno.
     *
     * @param consumerKey clave del consumidor.
     * @param uuidOrTokenSecret uuid del dispositivo o secreto del token.
     * @param idcompany empresa del token.
     * @return verdadero si lo eliminó, falso si no.
     */
    boolean dropToken(String consumerKey, String uuidOrTokenSecret, Long idcompany);

    /**
     * Cambia el estado de un token.
     *
     * @param consumerKey clave del consumidor.
     * @param uuidOrTokenSecret uuid del dispositivo o secreto del token.
     * @param status nuevo estado del token.
     * @return verdadero si se cambió, falso si no.
     */
    boolean changeTokenStatus(String consumerKey, String uuidOrTokenSecret, String status);

    /**
     * Cambia el estado de un token identificándolo también por empresa. Un
     * dispositivo puede tener tokens simultáneos para empresas distintas: sin
     * la empresa la búsqueda es ambigua y no se modifica ninguno.
     *
     * @param consumerKey clave del consumidor.
     * @param uuidOrTokenSecret uuid del dispositivo o secreto del token.
     * @param status nuevo estado (block, unblock).
     * @param idcompany empresa del token.
     * @return verdadero si lo cambió, falso si no.
     */
    boolean changeTokenStatus(String consumerKey, String uuidOrTokenSecret, String status, Long idcompany);

    /**
     * Devuelve la clase de entidad de consumidor utilizada.
     *
     * @return clase del consumidor.
     */
    Class<IAppAuthConsumer> getAuthConsumerClass();

    /**
     * Devuelve la clase de entidad de token utilizada.
     *
     * @return clase del token.
     */
    Class<IAppAuthConsumerToken> getAuthConsumerTokenClass();

    /**
     * Devuelve el token vigente para un consumidor y uuid/secreto.
     *
     * @param consumerKey clave del consumidor.
     * @param uuidOrTokenSecret uuid del dispositivo o secreto del token.
     * @return token vigente.
     */
    String getToken(String consumerKey, String uuidOrTokenSecret);

    /**
     * Devuelve la fecha de expiración de un token.
     *
     * @param consumerKey clave del consumidor.
     * @param uuidOrTokenSecret uuid del dispositivo o secreto del token.
     * @return fecha de expiración.
     */
    LocalDateTime getTokenExpiredDate(String consumerKey, String uuidOrTokenSecret);

    /**
     * Devuelve la URL de autenticación asociada a un token.
     *
     * @param consumerKey clave del consumidor.
     * @param uuidOrTokenSecret uuid del dispositivo o secreto del token.
     * @return URL de autenticación.
     */
    String getTokenAuthUrl(String consumerKey, String uuidOrTokenSecret);

    /**
     * Devuelve la URL de callback asociada a un token.
     *
     * @param consumerKey clave del consumidor.
     * @param uuidOrTokenSecret uuid del dispositivo o secreto del token.
     * @return URL de callback.
     */
    String getTokenCallbackUrl(String consumerKey, String uuidOrTokenSecret);

    /**
     * Solicita un token para el consumidor indicado.
     *
     * @param consumerKey clave del consumidor.
     * @return verdadero si la solicitud fue aceptada, falso si no.
     */
    boolean requestToken(String consumerKey);

    /**
     * Solicita un token para el consumidor y dispositivo indicados.
     *
     * @param consumerKey clave del consumidor.
     * @param uuidDevice identificador del dispositivo.
     * @return verdadero si la solicitud fue aceptada, falso si no.
     */
    boolean requestToken(String consumerKey, String uuidDevice);

    /**
     * Solicita un token asociando el nombre y correo del usuario.
     *
     * @param consumerKey clave del consumidor.
     * @param uuidDevice identificador del dispositivo.
     * @param userName nombre del usuario.
     * @param userEmail correo del usuario.
     * @return verdadero si la solicitud fue aceptada, falso si no.
     */
    boolean requestToken(String consumerKey, String uuidDevice, String userName, String userEmail);

    /**
     * Solicita un token identificando al dueño: además de los datos de
     * contacto, la solicitud declara el código de usuario y la empresa para la
     * cual se pide, de modo que un token —aun pendiente de aprobación— nunca
     * queda sin dueño.
     *
     * @param consumerKey clave del consumidor.
     * @param uuidDevice uuid del dispositivo.
     * @param userName nombre del usuario.
     * @param userEmail correo del usuario.
     * @param userCode código del usuario dueño del token.
     * @param idcompany empresa para la cual se solicita el token.
     * @return verdadero si la solicitud fue aceptada, falso si no.
     */
    boolean requestToken(String consumerKey, String uuidDevice, String userName,
            String userEmail, String userCode, Long idcompany);

    /**
     * Indica si un token es válido.
     *
     * @param token token de acceso.
     * @return verdadero si es válido, falso si no.
     */
    boolean isValidToken(String token);

    /**
     * Indica si una entidad token es válida.
     *
     * @param authToken entidad token.
     * @return verdadero si es válida, falso si no.
     */
    boolean isValidToken(IAppAuthConsumerToken authToken);

    /**
     * Indica si un token es válido, con opción de omitir la verificación de
     * credenciales.
     *
     * @param token token de acceso.
     * @param noCheckCredentials verdadero para no verificar las credenciales.
     * @return verdadero si es válido, falso si no.
     */
    boolean isValidToken(String token, boolean noCheckCredentials);

    /**
     * Indica si una entidad token es válida, con opción de omitir la
     * verificación de credenciales.
     *
     * @param authToken entidad token.
     * @param noCheckCredentials verdadero para no verificar las credenciales.
     * @return verdadero si es válida, falso si no.
     */
    boolean isValidToken(IAppAuthConsumerToken authToken, boolean noCheckCredentials);

    /**
     * Verifica un token y devuelve el error correspondiente si no es válido.
     *
     * @param token token de acceso.
     * @return registro de error, o {@code null} si es válido.
     */
    IErrorReg checkToken(String token);

    /**
     * Verifica una entidad token y devuelve el error correspondiente si no es válida.
     *
     * @param authToken entidad token.
     * @return registro de error, o {@code null} si es válida.
     */
    IErrorReg checkToken(IAppAuthConsumerToken authToken);

    /**
     * Verifica un token, con opción de omitir la verificación de credenciales.
     *
     * @param token token de acceso.
     * @param noCheckCredentials verdadero para no verificar las credenciales.
     * @return registro de error, o {@code null} si es válido.
     */
    IErrorReg checkToken(String token, boolean noCheckCredentials);

    /**
     * Verifica una entidad token, con opción de omitir la verificación de
     * credenciales.
     *
     * @param authToken entidad token.
     * @param noCheckCredentials verdadero para no verificar las credenciales.
     * @return registro de error, o {@code null} si es válida.
     */
    IErrorReg checkToken(IAppAuthConsumerToken authToken, boolean noCheckCredentials);

    /**
     * Devuelve el valor de una propiedad de los datos incluidos en un token.
     *
     * @param token token de acceso.
     * @param property nombre de la propiedad.
     * @return valor de la propiedad.
     */
    String getDataKeyValue(String token, String property);

    /**
     * Devuelve el valor de una propiedad de los datos incluidos en una entidad token.
     *
     * @param token entidad token.
     * @param property nombre de la propiedad.
     * @return valor de la propiedad.
     */
    String getDataKeyValue(IAppAuthConsumerToken token, String property);

    /**
     * Devuelve el usuario mapeado por una entidad token.
     *
     * @param token entidad token.
     * @return usuario mapeado.
     */
    IAppUser getUserMapped(IAppAuthConsumerToken token);

    /**
     * Devuelve el usuario mapeado por un token.
     *
     * @param token token de acceso.
     * @return usuario mapeado.
     */
    IAppUser getUserMapped(String token);

    /**
     * Devuelve la empresa mapeada por una entidad token.
     *
     * @param token entidad token.
     * @return empresa mapeada.
     */
    IAppCompany getCompanyMapped(IAppAuthConsumerToken token);

    /**
     * Devuelve la empresa mapeada por un token.
     *
     * @param token token de acceso.
     * @return empresa mapeada.
     */
    IAppCompany getCompanyMapped(String token);

    /**
     * Devuelve las empresas habilitadas para un usuario.
     *
     * @param userLogin login del usuario.
     * @return lista de empresas habilitadas.
     */
    List<IAppCompany> getCompaniesAllowed(String userLogin);

    /**
     * Devuelve el filtro de datos aplicable según el token.
     *
     * @param token entidad token.
     * @return filtro de datos.
     */
    IDBFilter getDBFilter(IAppAuthConsumerToken token);

    /**
     * Valida los datos de autenticación de un consumidor.
     *
     * @param data datos de autenticación.
     * @return verdadero si son válidos, falso si no.
     */
    boolean checkAuthConsumerData(IOAuthConsumerData data);

    /**
     * Asigna el servicio de datos utilizado para persistir consumidores y tokens.
     *
     * @param dao servicio de datos.
     */
    void setDao(IDataService dao);
}
