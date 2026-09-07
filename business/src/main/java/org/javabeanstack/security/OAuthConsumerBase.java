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

import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.security.model.UserSession;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import jakarta.ejb.EJB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.javabeanstack.crypto.CipherUtil;
import org.javabeanstack.crypto.DigestUtil;
import org.javabeanstack.data.IDBFilter;
import org.javabeanstack.data.IDataResult;
import org.javabeanstack.data.model.DataResult;
import org.javabeanstack.data.services.IAppAuthConsumerTokenSrv;
import org.javabeanstack.data.services.IAppCompanySrv;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.model.IAppAuthConsumer;
import org.javabeanstack.data.services.IDataService;
import org.javabeanstack.error.ErrorReg;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.exceptions.TokenGenericException;
import org.javabeanstack.model.IAppCompany;
import org.javabeanstack.model.IAppUser;
import org.javabeanstack.util.Fn;
import static org.javabeanstack.util.Fn.nvl;
import org.javabeanstack.model.IAppAuthConsumerToken;
import org.javabeanstack.util.LocalDates;

/**
 *
 * Implementación base de la gestión de consumidores OAuth y sus tokens
 * ({@link org.javabeanstack.security.IOAuthConsumer}): emisión, validación y
 * revocación de tokens, y resolución del usuario, la empresa y el filtro de
 * datos a partir de un token. La extiende {@code OAuthConsumer}.
 *
 * @author Jorge Enciso
 */
public abstract class OAuthConsumerBase implements IOAuthConsumer {

    private static final Logger LOGGER = LogManager.getLogger(OAuthConsumerBase.class);

    @EJB
    private IDataService dao;

    /**
     * Servicio de los tokens: el único punto por el que se graba la tabla, con
     * sus validaciones {@code @CheckMethod}.
     */
    @EJB
    private IAppAuthConsumerTokenSrv authConsumerTokenSrv;

    @EJB
    private IAppCompanySrv appCompanySrv;

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

    /**
     * Devuelve el último consumidor de autenticación resuelto.
     *
     * @return último consumidor, o {@code null} si no hay.
     */
    public IAppAuthConsumer getLastAuthConsumer() {
        return lastAuthConsumer;
    }

    /**
     * Devuelve el último token de autenticación resuelto.
     *
     * @return último token, o {@code null} si no hay.
     */
    public IAppAuthConsumerToken getLastAuthConsumerToken() {
        return lastAuthConsumerToken;
    }

    /**
     * Busca un registro AppAuthConsumer dado un consumerKey
     *
     * @param consumerKey clave del consumidor
     * @return registro AppAuthConsumer
     */
    @Override
    public IAppAuthConsumer findAuthConsumer(String consumerKey) {
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
            if (auth != null) {
                LocalDateTime start = auth.getLastUsed();
                LocalDateTime end = LocalDates.now();
                if (start == null || Duration.between(start, end).getSeconds() > 2) {
                    auth.setLastUsed(LocalDates.now());
                    modificarToken(auth);
                }
            }
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
     * @param uuidOrTokenSecret clave del token o uuid del dispositivo.
     * @return registro AppAuthConsumerToken
     */
    @Override
    public IAppAuthConsumerToken findAuthToken(String consumerKey, String uuidOrTokenSecret) {
        try {
            Map<String, Object> parameters = new HashMap();
            parameters.put("consumerKey", consumerKey);
            parameters.put("uuidOrTokenSecret", uuidOrTokenSecret);
            String queryString = "select o from AppAuthConsumerToken o where appAuthConsumer.consumerKey = :consumerKey and tokenSecret = :uuidOrTokenSecret";
            //Busca por tokensecret
            IAppAuthConsumerToken auth = dao.findByQuery(null, queryString, parameters);
            if (auth == null) {
                //Busca por uuidDevice
                queryString = "select o from AppAuthConsumerToken o where appAuthConsumer.consumerKey = :consumerKey and uuidDevice = :uuidOrTokenSecret";
                auth = dao.findByQuery(null, queryString, parameters);
            }
            if (auth != null) {
                LocalDateTime start = auth.getLastUsed();
                LocalDateTime end = LocalDates.now();
                if (start == null || Duration.between(start, end).getSeconds() > 2) {
                    auth.setLastUsed(LocalDates.now());
                    modificarToken(auth);
                }
            }
            return auth;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    /**
     * Busca un registro AppAuthConsumerToken dado un consumerKey, la clave del
     * token o uuid del dispositivo y la empresa. Desde que un mismo dispositivo
     * puede tener tokens simultáneos para empresas distintas, la empresa forma
     * parte de la búsqueda; la sobrecarga de dos argumentos se conserva por
     * compatibilidad y devuelve un token cualquiera de los que coincidan.
     *
     * @param consumerKey clave del consumidor
     * @param uuidOrTokenSecret clave del token o uuid del dispositivo.
     * @param idcompany identificador de la empresa del token.
     * @return registro AppAuthConsumerToken
     */
    @Override
    public IAppAuthConsumerToken findAuthToken(String consumerKey, String uuidOrTokenSecret, Long idcompany) {
        if (idcompany == null) {
            return findAuthToken(consumerKey, uuidOrTokenSecret);
        }
        try {
            Map<String, Object> parameters = new HashMap();
            parameters.put("consumerKey", consumerKey);
            parameters.put("uuidOrTokenSecret", uuidOrTokenSecret);
            parameters.put("idcompany", idcompany);
            String queryString = "select o from AppAuthConsumerToken o where appAuthConsumer.consumerKey = :consumerKey and tokenSecret = :uuidOrTokenSecret and idcompany = :idcompany";
            //Busca por tokensecret
            IAppAuthConsumerToken auth = dao.findByQuery(null, queryString, parameters);
            if (auth == null) {
                //Busca por uuidDevice
                queryString = "select o from AppAuthConsumerToken o where appAuthConsumer.consumerKey = :consumerKey and uuidDevice = :uuidOrTokenSecret and idcompany = :idcompany";
                auth = dao.findByQuery(null, queryString, parameters);
            }
            if (auth != null) {
                LocalDateTime start = auth.getLastUsed();
                LocalDateTime end = LocalDates.now();
                if (start == null || Duration.between(start, end).getSeconds() > 2) {
                    auth.setLastUsed(LocalDates.now());
                    modificarToken(auth);
                }
            }
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
    public String createAuthConsumer(String consumerName, LocalDateTime expiredDate) {
        try {
            IAppAuthConsumer authConsumer = getAuthConsumerClass().getConstructor().newInstance();
            authConsumer.setConsumerName(consumerName);
            authConsumer.setExpiredDate(expiredDate);
            authConsumer.setBlocked(false);
            authConsumer.setConsumerKey(createConsumerKey(authConsumer));
            IDataResult dataResult = dao.persist(null, authConsumer);
            if (dataResult.isSuccessFul()) {
                lastAuthConsumer = dataResult.getRowUpdated();
                return lastAuthConsumer.getConsumerKey();
            }
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    /**
     * Crea y guarda en la base de datos el registro de AuthConsumer
     *
     * @param authConsumer datos del consumer
     * @return consumer creado
     */
    @Override
    public IAppAuthConsumer createAuthConsumer(IAppAuthConsumer authConsumer) {
        try {
            IAppAuthConsumer authConsumerNew = getAuthConsumerClass().getConstructor().newInstance();
            authConsumerNew.setConsumerName(authConsumer.getConsumerName());
            authConsumerNew.setExpiredDate(authConsumer.getExpiredDate());
            authConsumerNew.setConsumerKey(authConsumer.getConsumerKey());
            authConsumerNew.setBlocked(authConsumer.getBlocked());

            IDataResult dataResult = dao.persist(null, authConsumerNew);
            if (!dataResult.isSuccessFul()) {
                return null;
            }
            return dataResult.getRowUpdated();
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
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
     * @param uuidOrTokenSecret clave del token o uuid del dispositivo.
     * @return token
     */
    @Override
    public String getToken(String consumerKey, String uuidOrTokenSecret) {
        IAppAuthConsumerToken authConsumerToken = findAuthToken(consumerKey, uuidOrTokenSecret);
        if (authConsumerToken != null) {
            return authConsumerToken.getToken();
        }
        return "";
    }

    /**
     * Devuelve la fecha de expiración del token
     *
     * @param consumerKey clave del consumidor
     * @param uuidOrTokenSecret clave del token o uuid del dispositivo.
     * @return fecha expiración del token
     */
    @Override
    public LocalDateTime getTokenExpiredDate(String consumerKey, String uuidOrTokenSecret) {
        IAppAuthConsumerToken authConsumerToken = findAuthToken(consumerKey, uuidOrTokenSecret);
        if (authConsumerToken != null && authConsumerToken.getAppAuthConsumerKey() != null) {
            return authConsumerToken.getAppAuthConsumerKey().getExpiredDate();
        }
        if (authConsumerToken != null && authConsumerToken.getExpiredDate() != null) {
            return authConsumerToken.getExpiredDate();
        }
        return null;
    }

    /**
     * Devuelve el url de autenticación
     *
     * @param consumerKey clave del consumidor
     * @param uuidOrTokenSecret clave del token o uuid del dispositivo.
     * @return url de autenticación
     */
    @Override
    public String getTokenAuthUrl(String consumerKey, String uuidOrTokenSecret) {
        IAppAuthConsumerToken authConsumerToken = findAuthToken(consumerKey, uuidOrTokenSecret);
        if (authConsumerToken != null && authConsumerToken.getAppAuthConsumerKey() != null) {
            return authConsumerToken.getAppAuthConsumerKey().getAuthURL();
        }
        return "";
    }

    /**
     * Devuelve el url de los servicios
     *
     * @param consumerKey clave del consumidor
     * @param uuidOrTokenSecret clave del token o uuid del dispositivo.
     * @return url de los servicios
     */
    @Override
    public String getTokenCallbackUrl(String consumerKey, String uuidOrTokenSecret) {
        IAppAuthConsumerToken authConsumerToken = findAuthToken(consumerKey, uuidOrTokenSecret);
        if (authConsumerToken != null && authConsumerToken.getAppAuthConsumerKey() != null) {
            return authConsumerToken.getAppAuthConsumerKey().getCallbackURL();
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
    /**
     * Devuelve el servicio de tokens, único punto de grabación de la tabla.
     *
     * @return el servicio.
     */
    protected IAppAuthConsumerTokenSrv getAuthConsumerTokenSrv() {
        return authConsumerTokenSrv;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delega en el servicio de tokens, que es donde vive la validación que
     * después se aplica sola al grabar.</p>
     */
    @Override
    public IErrorReg checkTokenOwner(String consumerKey, String userCode, Long idcompany) {
        if (getAuthConsumerTokenSrv() == null) {
            LOGGER.error("No está declarado el servicio de tokens (AppAuthConsumerTokenSrv)");
            return null;
        }
        return getAuthConsumerTokenSrv().checkTokenOwner(consumerKey, userCode, idcompany);
    }

    /**
     * <b>Único punto de grabación</b> de {@code appauthconsumertoken}: todas
     * las altas del framework —solicitud, creación desde un dispositivo y copia
     * del servidor principal— pasan por acá.
     *
     * <p>Graba por el servicio de la entidad, de modo que sus validaciones
     * ({@code usercode}, {@code idcompany}, consumidor y {@code data}) se
     * ejecuten siempre, sin que cada método tenga que acordarse de llamarlas.
     * Si el servicio no está declarado no se graba nada: es preferible un
     * despliegue que falla ruidosamente a una tabla que se llena sin
     * control.</p>
     *
     * @param token token a grabar.
     * @return el resultado de la grabación.
     */
    protected IDataResult grabarToken(IAppAuthConsumerToken token) {
        return grabarPorElServicio(token, "grabar");
    }

    /**
     * Modifica un token existente, también por el servicio de la entidad.
     *
     * <p>Va por el mismo punto que el alta para que ningún camino escriba la
     * tabla por afuera. Lo que cambia es qué se valida: mientras el dueño no se
     * toque, el servicio deja pasar la modificación —de otro modo no se podría
     * bloquear un token viejo, grabado antes de que hubiera reglas—.</p>
     *
     * @param token token a modificar.
     * @return el resultado de la grabación.
     */
    protected IDataResult modificarToken(IAppAuthConsumerToken token) {
        return grabarPorElServicio(token, "modificar");
    }

    /**
     * Elimina un token por el servicio de la entidad.
     *
     * @param token token a eliminar.
     * @return el resultado de la operación.
     */
    protected IDataResult borrarToken(IAppAuthConsumerToken token) {
        return grabarPorElServicio(token, "borrar");
    }

    /**
     * Ejecuta una escritura de la tabla de tokens por su servicio.
     *
     * @param token token afectado.
     * @param operacion "grabar", "modificar" o "borrar".
     * @return el resultado de la operación.
     */
    private IDataResult grabarPorElServicio(IAppAuthConsumerToken token, String operacion) {
        if (getAuthConsumerTokenSrv() == null) {
            LOGGER.error("No está declarado el servicio de tokens (AppAuthConsumerTokenSrv):"
                    + " no se pudo " + operacion + " el token");
            IDataResult fallido = new DataResult();
            fallido.setSuccess(false);
            fallido.setErrorMsg("No está declarado el servicio de tokens");
            return fallido;
        }
        IDataResult resultado;
        try {
            switch (operacion) {
                case "modificar":
                    resultado = getAuthConsumerTokenSrv().merge(null, token);
                    break;
                case "borrar":
                    resultado = getAuthConsumerTokenSrv().remove(null, token);
                    break;
                default:
                    resultado = getAuthConsumerTokenSrv().persist(null, token);
                    break;
            }
        } catch (Exception exp) {
            ErrorManager.showError(exp, LOGGER);
            resultado = new DataResult();
            resultado.setSuccess(false);
            resultado.setErrorMsg(ErrorManager.getStackCause(exp));
            return resultado;
        }
        if (!resultado.isSuccessFul()) {
            LOGGER.info("Token rechazado al " + operacion + ": " + motivoDelRechazo(resultado));
        }
        return resultado;
    }

    /**
     * Arma el texto de los errores de validación de una grabación rechazada.
     *
     * @param resultado resultado de la grabación.
     * @return el motivo, en una línea.
     */
    protected String motivoDelRechazo(IDataResult resultado) {
        if (resultado == null) {
            return "";
        }
        StringBuilder motivo = new StringBuilder(nvl(resultado.getErrorMsg(), ""));
        if (resultado.getErrorsMap() != null) {
            for (Map.Entry<String, IErrorReg> entry : resultado.getErrorsMap().entrySet()) {
                if (motivo.length() > 0) {
                    motivo.append(" · ");
                }
                motivo.append(entry.getValue().getMessage());
            }
        }
        return motivo.toString();
    }

    @Deprecated
    @Override
    public boolean requestToken(String consumerKey) {
        return requestToken(consumerKey, null, null, null);
    }

    /**
     * Graba una solicitud de token, debe completarse el proceso en otro
     * programa.
     *
     * @param consumerKey clave del consumidor
     * @param uuidDevice identificador unico del dispositivo
     * @return verdadero si tuvo exito y falso si no.
     */
    /**
     * {@inheritDoc}
     *
     * @deprecated ver {@link #requestToken(String)}.
     */
    @Deprecated
    @Override
    public boolean requestToken(String consumerKey, String uuidDevice) {
        return requestToken(consumerKey, uuidDevice, null, null);
    }

    /**
     * Graba una solicitud de token, debe completarse el proceso en otro
     * programa.
     *
     * @param consumerKey clave del consumidor
     * @param uuidDevice identificador unico del dispositivo
     * @param userName
     * @param userEmail
     * @return verdadero si tuvo exito y falso si no.
     */
    /**
     * {@inheritDoc}
     *
     * @deprecated ver {@link #requestToken(String)}.
     */
    @Deprecated
    @Override
    public boolean requestToken(String consumerKey, String uuidDevice, String userName, String userEmail) {
        return requestToken(consumerKey, uuidDevice, userName, userEmail, null, null);
    }

    /**
     * Graba una solicitud de token, debe completarse el proceso en otro
     * programa.
     *
     * @param consumerKey clave del consumidor
     * @param uuidDevice identificador unico del dispositivo
     * @param userName nombre del usuario que solicita el token
     * @param userEmail correo del usuario que solicita el token
     * @param userCode código del usuario dueño del token
     * @param idcompany empresa para la cual se solicita el token
     * @return verdadero si tuvo exito y falso si no.
     */
    @Override
    public boolean requestToken(String consumerKey, String uuidDevice, String userName,
            String userEmail, String userCode, Long idcompany) {
        try {
            IAppAuthConsumerToken authConsumerToken = getAuthConsumerTokenClass().getConstructor().newInstance();
            authConsumerToken.setAppAuthConsumerKey(findAuthConsumer(consumerKey));
            authConsumerToken.setBlocked(true);
            String token = getRandomToken();
            authConsumerToken.setToken(token);
            authConsumerToken.setTokenSecret(token);
            authConsumerToken.setUuidDevice(token);
            authConsumerToken.setUserName(userName);
            authConsumerToken.setUserEmail(userEmail);
            //El dueño del token se declara desde el pedido: una solicitud
            //pendiente también identifica usuario y empresa
            //El servicio valida los dos datos y completa `data` con ellos.
            authConsumerToken.setUserCode(nvl(userCode, "").trim());
            authConsumerToken.setIdcompany(idcompany);
            if (uuidDevice != null) {
                authConsumerToken.setUuidDevice(uuidDevice);
            }
            IDataResult dataResult = grabarToken(authConsumerToken);
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
    public String createToken(String consumerKey, IOAuthConsumerData data) throws TokenGenericException {
        return createToken(consumerKey, data, null);
    }

    /**
     * Crea y graba en la base de datos el registro de un token de autorización
     *
     * @param consumerKey clave del consumidor.
     * @param data información del token.
     * @param uuidDevice identificador unico del dispositivo.
     * @return valor del token.
     */
    @Override
    public String createToken(String consumerKey, IOAuthConsumerData data, String uuidDevice) throws TokenGenericException {
        return createToken(consumerKey, data, uuidDevice, null, null);
    }

    /**
     * Crea y graba en la base de datos el registro de un token de autorización
     *
     * @param consumerKey clave del consumidor.
     * @param data información del token.
     * @param uuidDevice identificador unico del dispositivo.
     * @param userName
     * @param userEmail
     * @return valor del token.
     */
    @Override
    public String createToken(String consumerKey, IOAuthConsumerData data,
            String uuidDevice, String userName, String userEmail) throws TokenGenericException {
        if (uuidDevice != null) {
            //Verificar existencia de un token anterior generado con las mismas especificaciones
            // ConsumerKey + uuidDevice + empresa (un dispositivo puede tener tokens
            // simultáneos para empresas distintas; sólo se reemplaza el de la misma empresa)
            IAppAuthConsumerToken tokenExists = findAuthToken(consumerKey, uuidDevice, data.getIdCompany());
            if (tokenExists != null) {
                // Si ya existe un token y esta bloqueado, generar error
                if (tokenExists.getBlocked()) {
                    throw new TokenGenericException("Este token ya existe y fue bloqueado");
                }
                try {
                    // si no eliminar token para crear uno nuevo.
                    borrarToken(tokenExists);
                } catch (Exception ex) {
                    ErrorManager.showError(ex, LOGGER);
                }
            }
        }
        try {
            dao.checkAuthConsumerData(data);
            String userCode = data.getUserLogin();
            //Si no esta la información del id del usuario
            if (nvl(data.getIdAppUser(), 0L) == 0L) {
                Map<String, Object> params = new HashMap<>();
                params.put("userLogin", data.getUserLogin());
                IAppUser usuario = dao.findByQuery(null,
                        "select o from AppUserLight o where code = :userLogin",
                        params);
                data.setIdAppUser(usuario.getIduser());
                data.setAdministrator(false);
                //Si es administrador o analista
                if (usuario.isCompanyAdmin()) {
                    data.setAdministrator(true);
                }
            } else if (nvl(userCode, "").isEmpty()) {
                //Vino el id pero no el login: resolver el código del usuario
                IAppUser usuario = dao.findByQuery(null,
                        "select o from AppUserLight o where iduser = " + data.getIdAppUser(), null);
                if (usuario != null) {
                    userCode = usuario.getCode();
                }
            }
            IAppAuthConsumerToken authConsumerToken = getAuthConsumerTokenClass().getConstructor().newInstance();
            authConsumerToken.setAppAuthConsumerKey(findAuthConsumer(consumerKey));
            authConsumerToken.setBlocked(false);
            authConsumerToken.setData(data.toString());
            //Columnas propias del dueño del token (fuente de verdad desde la Fase 1
            //del plan de seguridad; `data` se sigue grabando por compatibilidad)
            authConsumerToken.setIdcompany(data.getIdCompany());
            authConsumerToken.setUserCode(userCode.trim());
            if (uuidDevice != null) {
                authConsumerToken.setUuidDevice(uuidDevice);
            }
            String token = signTokenData(authConsumerToken);
            authConsumerToken.setToken(token);
            String tokenSecret = getTokenSecret(authConsumerToken);
            authConsumerToken.setTokenSecret(tokenSecret);
            if (uuidDevice == null) {
                authConsumerToken.setUuidDevice(tokenSecret);
            }
            authConsumerToken.setUserEmail(userEmail);
            authConsumerToken.setUserName(userName);
            IDataResult dataResult = grabarToken(authConsumerToken);
            if (!dataResult.isSuccessFul()) {
                //El motivo viaja al cliente: un token que no se creó porque un
                //dato está mal no puede responderse como "no encuentra el token".
                throw new TokenGenericException(motivoDelRechazo(dataResult));
            }
            lastAuthConsumerToken = dataResult.getRowUpdated();
            return authConsumerToken.getToken();
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return "";
    }

    /**
     * Crea y graba en la base de datos el registro de un token de autorización
     *
     * @param authConsumerToken objeto token
     * @return tokenString
     */
    @Override
    public String createToken(IAppAuthConsumerToken authConsumerToken) throws TokenGenericException {
        if (authConsumerToken.getUuidDevice() != null) {
            //Verificar existencia de un token anterior generado con las mismas especificaciones
            // ConsumerKey + uuiDevice
            IAppAuthConsumerToken tokenExists
                    = findAuthToken(authConsumerToken.getConsumerKey(),
                            authConsumerToken.getUuidDevice());
            if (tokenExists != null) {
                // Si ya existe un token y esta bloqueado, generar error
                if (tokenExists.getBlocked()) {
                    throw new TokenGenericException("Este token fue bloqueado");
                }
                try {
                    // si no eliminar token para crear uno nuevo.
                    borrarToken(tokenExists);
                } catch (Exception ex) {
                    ErrorManager.showError(ex, LOGGER);
                }
            }
        }
        try {
            IAppAuthConsumer appConsumer = findAuthConsumer(authConsumerToken.getConsumerKey());
            //No existe el consumer key
            if (appConsumer == null) {
                IAppAuthConsumer authConsumerNew = authConsumerToken.getAppAuthConsumerKey();
                if (authConsumerNew == null){
                    authConsumerNew = getAuthConsumerClass().getConstructor().newInstance();
                    authConsumerNew.setConsumerName(authConsumerToken.getConsumerName());
                    authConsumerNew.setExpiredDate(authConsumerToken.getExpiredDate());
                    authConsumerNew.setConsumerKey(authConsumerToken.getConsumerKey());
                    authConsumerNew.setBlocked(authConsumerToken.getBlocked());
                }
                appConsumer = createAuthConsumer(authConsumerNew);
            }
            IAppAuthConsumerToken authConsumerTokenNew = getAuthConsumerTokenClass().getConstructor().newInstance();
            authConsumerTokenNew.setBlocked(authConsumerToken.getBlocked());
            authConsumerTokenNew.setData(authConsumerToken.getData());
            authConsumerTokenNew.setToken(authConsumerToken.getToken());
            authConsumerTokenNew.setAppAuthConsumerKey(appConsumer);
            authConsumerTokenNew.setTokenSecret(authConsumerToken.getTokenSecret());
            authConsumerTokenNew.setUuidDevice(authConsumerToken.getUuidDevice());
            authConsumerTokenNew.setUserName(authConsumerToken.getUserName());
            authConsumerTokenNew.setUserEmail(authConsumerToken.getUserEmail());
            //Columnas propias del dueño del token; si el objeto recibido no las
            //trae, se completan desde `data` (tokens armados por el consumidor)
            Long idcompany = authConsumerToken.getIdcompany();
            String userCode = authConsumerToken.getUserCode();
            if (idcompany == null && !nvl(getDataKeyValue(authConsumerToken, "idcompany"), "").isEmpty()) {
                idcompany = Long.valueOf(getDataKeyValue(authConsumerToken, "idcompany"));
            }
            if (nvl(userCode, "").isEmpty()) {
                userCode = getDataKeyValue(authConsumerToken, "userlogin");
            }
            //La copia tampoco se graba sin dueño: el servicio la valida igual
            //que a un token propio. Uno del servidor principal cuyo usuario o
            //empresa no existan acá no serviría, y quedaría una fila que solo se
            //descubre al fallar el ingreso.
            authConsumerTokenNew.setIdcompany(idcompany);
            authConsumerTokenNew.setUserCode(nvl(userCode, "").trim());

            IDataResult dataResult = grabarToken(authConsumerTokenNew);
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
     * Convierte un {@code Map<String,String>} a un formato standard string para
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
     * @param uuidOrTokenSecret identificador del dispositivo.
     * @return verdadero si tuvo exito y falso si no.
     */
    @Override
    public boolean dropToken(String consumerKey, String uuidOrTokenSecret) {
        return dropToken(consumerKey, uuidOrTokenSecret, null);
    }

    /**
     * Elimina un token de la base de datos, identificándolo también por
     * empresa. Sin la empresa, un dispositivo con tokens de varias empresas
     * resulta ambiguo y no se elimina ninguno.
     *
     * @param consumerKey clave del consumidor.
     * @param uuidOrTokenSecret identificador del dispositivo.
     * @param idcompany empresa del token.
     * @return verdadero si tuvo exito y falso si no.
     */
    @Override
    public boolean dropToken(String consumerKey, String uuidOrTokenSecret, Long idcompany) {
        IAppAuthConsumerToken authConsumerToken = findAuthToken(consumerKey, uuidOrTokenSecret, idcompany);
        if (authConsumerToken == null) {
            return false;
        }
        try {
            IDataResult dataResult = borrarToken(authConsumerToken);
            return dataResult.isSuccessFul();
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return false;
    }

    /**
     * Bloquea un token de la base de datos.
     *
     * @param consumerKey clave del consumidor.
     * @param uuidOrTokenSecret identificador del dispositivo.
     * @param status (block,unblock)
     * @return verdadero si tuvo exito y falso si no.
     */
    @Override
    public boolean changeTokenStatus(String consumerKey, String uuidOrTokenSecret, String status) {
        return changeTokenStatus(consumerKey, uuidOrTokenSecret, status, null);
    }

    /**
     * Bloquea o desbloquea un token, identificándolo también por empresa. Sin
     * la empresa, un dispositivo con tokens de varias empresas resulta ambiguo
     * y no se modifica ninguno.
     *
     * @param consumerKey clave del consumidor.
     * @param uuidOrTokenSecret identificador del dispositivo.
     * @param status (block,unblock)
     * @param idcompany empresa del token.
     * @return verdadero si tuvo exito y falso si no.
     */
    @Override
    public boolean changeTokenStatus(String consumerKey, String uuidOrTokenSecret,
            String status, Long idcompany) {
        IAppAuthConsumerToken authConsumerToken = findAuthToken(consumerKey, uuidOrTokenSecret, idcompany);
        if (authConsumerToken == null) {
            return false;
        }
        try {
            if (status.equalsIgnoreCase("block")) {
                authConsumerToken.setBlocked(true);
            }
            if (status.equalsIgnoreCase("unblock")) {
                authConsumerToken.setBlocked(false);
            }
            IDataResult dataResult = modificarToken(authConsumerToken);
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
        if (model == null || model.getAppAuthConsumerKey() == null) {
            return null;
        }
        String data;
        data = model.getAppAuthConsumerKey().getConsumerKey();
        data += ":" + model.getAppAuthConsumerKey().getExpiredDate();
        data += ":" + model.getData();
        data += ":" + Fn.nvl(model.getUuidDevice(), "");
        String algorithm = model.getAppAuthConsumerKey().getSignatureAlgorithm();

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
        if (model == null || model.getAppAuthConsumerKey() == null) {
            return null;
        }
        String data;
        data = model.getAppAuthConsumerKey().getConsumerKey();
        data += ":" + model.getAppAuthConsumerKey().getExpiredDate();
        data += ":" + model.getData();
        data += ":" + model.getToken(); // Debio calcularse antes
        String algorithm = model.getAppAuthConsumerKey().getSignatureAlgorithm();

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
     *
     * @param token token
     * @return verdadero si es válido y falso si no.
     */
    @Override
    public IErrorReg checkToken(String token) {
        return checkToken(token, false);
    }

    /**
     * Determina si un token es válido o no
     *
     * @param authToken
     * @return verdadero si es válido y falso si no.
     */
    @Override
    public IErrorReg checkToken(IAppAuthConsumerToken authToken) {
        return checkToken(authToken, false);
    }

    /**
     * Determina si un token es válido o no
     *
     * @param token token
     * @param noCheckCredentials
     * @return verdadero si es válido y falso si no.
     */
    @Override
    public IErrorReg checkToken(String token, boolean noCheckCredentials) {
        IAppAuthConsumerToken result = findAuthToken(token);
        IErrorReg errorReturn = new ErrorReg();
        if (result == null) {
            errorReturn.setErrorNumber(50000);
            errorReturn.setMessage("Token: " + Fn.nvl(token, "") + ", no existe");
            return errorReturn;
        }
        //Si el token esta bloqueado
        if (result.getBlocked()) {
            errorReturn.setErrorNumber(50000);
            errorReturn.setMessage("Token: " + Fn.nvl(token, "") + ", esta bloqueado");
            return errorReturn;
        }
        //Si el token esta marcado como eliminado
        if (Fn.toLogical(result.getDeleted())) {
            errorReturn.setErrorNumber(50000);
            errorReturn.setMessage("Token: " + Fn.nvl(token, "") + ", no existe");
            return errorReturn;
        }
        if (result.getAppAuthConsumerKey() == null) {
            errorReturn.setErrorNumber(50000);
            errorReturn.setMessage("Token: " + Fn.nvl(token, "") + ", consumer key no existe");
            return errorReturn;
        }
        //Si el consumerKey esta bloqueado
        if (result.getAppAuthConsumerKey().getBlocked()) {
            errorReturn.setErrorNumber(50000);
            errorReturn.setMessage("Token: " + Fn.nvl(token, "") + ", consumer key esta bloqueado");
            return errorReturn;
        }
        //Si expiro el customerKey
        if (!result.getAppAuthConsumerKey().getExpiredDate().isAfter(LocalDateTime.now())) {
            errorReturn.setErrorNumber(50000);
            errorReturn.setMessage("Token: " + Fn.nvl(token, "") + ", consumer key expiro");
            return errorReturn;
        }
        if (noCheckCredentials) {
            return errorReturn;
        }
        //No existe la empresa o no tiene acceso a ella
        if (getCompanyMapped(result) == null) {
            errorReturn.setErrorNumber(50000);
            errorReturn.setMessage("Token: " + Fn.nvl(token, "") + ", No existe la empresa o no tiene acceso a ella");
            return errorReturn;
        }
        //No existe el usuario
        if (getUserMapped(result) == null) {
            errorReturn.setErrorNumber(50000);
            errorReturn.setMessage("Token: " + Fn.nvl(token, "") + ", no existe el usuario");
            return errorReturn;
        }
        if (!dao.isCredentialValid(getUserMapped(result).getIduser(), getCompanyMapped(result).getIdcompany())) {
            errorReturn.setErrorNumber(50000);
            errorReturn.setMessage("Token: " + Fn.nvl(token, "") + ", credencial inválida");
            return errorReturn;
        }
        return errorReturn;
    }

    /**
     * Determina si un token es válido o no
     *
     * @param authToken
     * @param noCheckCredentials
     * @return verdadero si es válido y falso si no.
     */
    @Override
    public IErrorReg checkToken(IAppAuthConsumerToken authToken, boolean noCheckCredentials) {
        IErrorReg errorReturn = new ErrorReg();
        if (authToken == null) {
            errorReturn.setErrorNumber(50000);
            errorReturn.setMessage("Authtoken es nulo");
            return errorReturn;
        }
        //Si el token esta bloqueado
        if (authToken.getBlocked()) {
            errorReturn.setErrorNumber(50000);
            errorReturn.setMessage("Token: " + Fn.nvl(authToken.getToken(), "") + ", esta bloqueado");
            return errorReturn;
        }
        //Si el token esta marcado como eliminado
        if (Fn.toLogical(authToken.getDeleted())) {
            errorReturn.setErrorNumber(50000);
            errorReturn.setMessage("Token: " + Fn.nvl(authToken.getToken(), "") + ", no existe");
            return errorReturn;
        }
        if (authToken.getAppAuthConsumerKey() == null) {
            authToken = findAuthToken(authToken.getToken());
        }
        if (authToken.getAppAuthConsumerKey() == null) {
            errorReturn.setErrorNumber(50000);
            errorReturn.setMessage("Token: " + Fn.nvl(authToken.getToken(), "") + ", consumer key no existe");
            return errorReturn;
        }
        //Si el consumerKey esta bloqueado
        if (authToken.getAppAuthConsumerKey().getBlocked()) {
            errorReturn.setErrorNumber(50000);
            errorReturn.setMessage("Token: " + Fn.nvl(authToken.getToken(), "") + ", consumer key esta bloqueado");
            return errorReturn;
        }
        //Si expiro el customerKey
        if (!authToken.getAppAuthConsumerKey().getExpiredDate().isAfter(LocalDateTime.now())) {
            errorReturn.setErrorNumber(50000);
            errorReturn.setMessage("Token: " + Fn.nvl(authToken.getToken(), "") + ", consumer key expiro");
            return errorReturn;
        }
        if (noCheckCredentials) {
            return errorReturn;
        }
        //No existe la empresa o no tiene acceso a ella
        if (getCompanyMapped(authToken) == null) {
            errorReturn.setErrorNumber(50000);
            errorReturn.setMessage("Token: " + Fn.nvl(authToken.getToken(), "") + ", No existe la empresa o no tiene acceso a ella");
            return errorReturn;
        }
        //No existe el usuario
        if (getUserMapped(authToken) == null) {
            errorReturn.setErrorNumber(50000);
            errorReturn.setMessage("Token: " + Fn.nvl(authToken.getToken(), "") + ", no existe el usuario");
            return errorReturn;
        }
        if (!dao.isCredentialValid(getUserMapped(authToken).getIduser(), getCompanyMapped(authToken).getIdcompany())) {
            errorReturn.setErrorNumber(50000);
            errorReturn.setMessage("Token: " + Fn.nvl(authToken.getToken(), "") + ", credencial inválida");
            return errorReturn;
        }
        return errorReturn;
    }

    /**
     * Determina si un token es válido o no
     *
     * @param token token
     * @return verdadero si es válido y falso si no.
     */
    @Override
    public boolean isValidToken(String token) {
        return isValidToken(token, false);
    }

    /**
     * Determina si un token es válido o no
     *
     * @param authToken
     * @return verdadero si es válido y falso si no.
     */
    @Override
    public boolean isValidToken(IAppAuthConsumerToken authToken) {
        return isValidToken(authToken, false);
    }

    /**
     * Determina si un token es válido o no
     *
     * @param token token
     * @param noCheckCredentials
     * @return verdadero si es válido y falso si no.
     */
    @Override
    public boolean isValidToken(String token, boolean noCheckCredentials) {
        IAppAuthConsumerToken result = findAuthToken(token);
        if (result == null) {
            return false;
        }
        //Si el token esta bloqueado
        if (result.getBlocked()) {
            LOGGER.info("Token: " + Fn.nvl(token, "") + ", esta bloqueado");
            return false;
        }
        //Si el token esta marcado como eliminado
        if (Fn.toLogical(result.getDeleted())) {
            LOGGER.info("Token: " + Fn.nvl(token, "") + ", no existe");
            return false;
        }
        if (result.getAppAuthConsumerKey() == null) {
            LOGGER.info("Token: " + Fn.nvl(token, "") + ", consumer key no existe");
            return false;
        }
        //Si el consumerKey esta bloqueado
        if (result.getAppAuthConsumerKey().getBlocked()) {
            LOGGER.info("Token: " + Fn.nvl(token, "") + ", consumer key esta bloqueado");
            return false;
        }
        //Si expiro el customerKey
        if (!result.getAppAuthConsumerKey().getExpiredDate().isAfter(LocalDateTime.now())) {
            LOGGER.info("Token: " + Fn.nvl(token, "") + ", consumer key expiro");
            return false;
        }
        if (noCheckCredentials) {
            return true;
        }
        //No existe la empresa o no tiene acceso a ella
        if (getCompanyMapped(result) == null) {
            LOGGER.info("Token: " + Fn.nvl(token, "") + ", No existe la empresa o no tiene acceso a ella");
            return false;
        }
        //No existe el usuario
        if (getUserMapped(result) == null) {
            LOGGER.info("Token: " + Fn.nvl(token, "") + ", no existe el usuario");
            return false;
        }
        return dao.isCredentialValid(getUserMapped(result).getIduser(), getCompanyMapped(result).getIdcompany());
    }

    /**
     * Determina si un token es válido o no
     *
     * @param authToken
     * @param noCheckCredentials
     * @return verdadero si es válido y falso si no.
     */
    @Override
    public boolean isValidToken(IAppAuthConsumerToken authToken, boolean noCheckCredentials) {
        if (authToken == null) {
            return false;
        }
        //Si el token esta bloqueado
        if (authToken.getBlocked()) {
            return false;
        }
        //Si el token esta marcado para borrarse
        if (Fn.toLogical(authToken.getDeleted())) {
            return false;
        }
        if (authToken.getAppAuthConsumerKey() == null) {
            return false;
        }
        //Si el consumerKey esta bloqueado
        if (authToken.getAppAuthConsumerKey().getBlocked()) {
            return false;
        }
        //Si expiro el customerKey
        if (!authToken.getAppAuthConsumerKey().getExpiredDate().isAfter(LocalDateTime.now())) {
            return false;
        }
        if (noCheckCredentials) {
            return true;
        }
        //No existe la empresa o no tiene acceso a ella
        if (getCompanyMapped(authToken) == null) {
            return false;
        }
        //No existe el usuario
        if (getUserMapped(authToken) == null) {
            return false;
        }
        return dao.isCredentialValid(getUserMapped(authToken).getIduser(), getCompanyMapped(authToken).getIdcompany());
    }

    /**
     * Devuelve el valor de una propiedad que se encuentra en el campo "data" de
     * appauthconsumertoken
     *
     * @param token token
     * @param property propiedad
     * @return valor de la propiedad si existe y vacio si no.
     */
    @Override
    public String getDataKeyValue(String token, String property) {
        try {
            IAppAuthConsumerToken tokenRecord = findAuthToken(token);
            Properties prop = new Properties();
            prop.load(new StringReader(tokenRecord.getData()));
            return nvl((String) prop.getProperty(property), "");
        } catch (IOException ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return "";
    }

    /**
     * Devuelve el valor de una propiedad que se encuentra en el campo "data" de
     * appauthconsumertoken
     *
     * @param token token
     * @param property propiedad
     * @return valor de la propiedad si existe y vacio si no.
     */
    @Override
    public String getDataKeyValue(IAppAuthConsumerToken token, String property) {
        try {
            Properties prop = new Properties();
            prop.load(new StringReader(token.getData()));
            return nvl((String) prop.getProperty(property), "");
        } catch (IOException ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return "";
    }

    /**
     * Devuelve el objeto AppUser mapeado a este token
     *
     * @param token token
     * @return objeto AppUser mapeado al token o nulo si el token es inválido o
     * el user no existe.
     */
    @Override
    public IAppUser getUserMapped(String token) {
        try {
            return getUserMapped(findAuthToken(token));
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    /**
     * Devuelve el objeto AppUser mapeado a este token
     *
     * @param token token
     * @return objeto AppUser mapeado al token o nulo si el token es inválido o
     * el user no existe.
     */
    @Override
    public IAppUser getUserMapped(IAppAuthConsumerToken token) {
        try {
            if (token == null) {
                return null;
            }
            //La columna usercode es la fuente de verdad (Fase 1 del plan de
            //seguridad); `data` queda para las demás variables del token.
            Map<String, Object> params = new HashMap<>();
            params.put("code", token.getUserCode());
            IAppUser user = dao.findByQuery(null,
                    "select o from AppUserLight o where code = :code", params);
            return user;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    /**
     * Devuelve el objeto AppCompany mapeado a este token
     *
     * @param token token
     * @return objeto AppCompany mapeado al token o nulo si el token es inválido
     * o el user no existe.
     */
    @Override
    public IAppCompany getCompanyMapped(IAppAuthConsumerToken token) {
        try {
            if (token == null || token.getIdcompany() == null) {
                return null;
            }
            //La columna idcompany es la fuente de verdad (Fase 1 del plan de
            //seguridad); `data` queda para las demás variables del token.
            IAppCompany company = dao.findByQuery(null,
                    "select o from AppCompanySimple o where idcompany = " + token.getIdcompany(), null);
            return company;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    /**
     * Devuelve el objeto AppCompany mapeado a este token
     *
     * @param token token
     * @return objeto AppCompany mapeado al token o nulo si el token es inválido
     * o el user no existe.
     */
    @Override
    public IAppCompany getCompanyMapped(String token) {
        try {
            return getCompanyMapped(findAuthToken(token));
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    /**
     * Devuelve lista de empresas que el usuario puede acceder
     *
     * @param userLogin usuario
     * @return lista de empresas que el usuario puede acceder
     */
    @Override
    public List<IAppCompany> getCompaniesAllowed(String userLogin) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("userLogin", userLogin);
            // Verificar existencia del usuario
            IAppUser user = dao.findByQuery(null,
                    "select o from AppUserLight o where code = :userLogin",
                    params);

            IUserSession userSession = new UserSession();
            userSession.setUser(user);
            return getAppCompanySrv().getAppCompanySimpleList(userSession);
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    /**
     * Verifica válidez de los datos contenidos en el modelo AuthConsumerData
     * (usuario, contraseña, empresa)
     *
     * @param data modelo AuthConsumerData
     * @return verdadero o falso si pasa o no la validación.
     */
    @Override
    public boolean checkAuthConsumerData(IOAuthConsumerData data) {
        try {
            dao.checkAuthConsumerData(data);
            return true;
        } catch (Exception ex) {
            //
        }
        return false;
    }

    /**
     * Devuelve el filtro de datos aplicable según el token.
     *
     * @param token entidad token.
     * @return filtro de datos.
     */
    @Override
    public IDBFilter getDBFilter(IAppAuthConsumerToken token) {
        // Implementar
        return null;
    }

    /**
     * Devuelve el servicio de empresas utilizado por el componente.
     *
     * @return servicio de empresas.
     */
    protected IAppCompanySrv getAppCompanySrv() {
        return appCompanySrv;
    }
}
