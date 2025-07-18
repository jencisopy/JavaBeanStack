/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import javax.annotation.PostConstruct;
import javax.crypto.BadPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.javabeanstack.crypto.CipherUtil;
import org.javabeanstack.crypto.DigestUtil;
import org.javabeanstack.data.DBLinkInfo;
import org.javabeanstack.data.IDBLinkInfo;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.error.ErrorReg;
import org.javabeanstack.data.IGenericDAO;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.exceptions.SessionError;
import org.javabeanstack.log.ILogManagerSecurity;
import org.javabeanstack.model.IAppCompany;
import org.javabeanstack.model.IAppCompanyAllowed;
import org.javabeanstack.model.IAppUser;
import org.javabeanstack.util.Fn;
import org.javabeanstack.util.Strings;
import org.javabeanstack.model.IAppAuthConsumerToken;
import static org.javabeanstack.model.IAppLogRecord.*;
import org.javabeanstack.security.model.ClientAuthRequestInfo;
import org.javabeanstack.security.model.IClientAuthRequestInfo;
import org.javabeanstack.util.LocalDates;

/**
 * Es la clase encargada de todas las sesiones de usuarios. Guarda información
 * de cada sesión y gestión la creación y expiración de la sesión.
 *
 * El test unitario se encuentra en TestProjects clase
 * py.com.oym.test.data.TestSesiones
 *
 * @author Jorge Enciso
 */
//@Singleton
@Startup
@Lock(LockType.READ)
public class Sessions implements ISessions {
    private static final Logger LOGGER = LogManager.getLogger(Sessions.class);

    protected final Map<String, Object> sessionVar = new HashMap<>();
    protected boolean oneSessionPerUser = false;
    private SecretKey secretKey;
    private final Map<SessionInfo, Object> sessionsInfo = new HashMap();

    @EJB
    protected IGenericDAO dao;

    @EJB
    private IOAuthConsumer oAuthConsumer;
    
    @EJB
    private ILogManagerSecurity logMngr;

    /**
     * Se ejecuta al instanciarse esta clase.
     *
     */
    @PostConstruct
    private void init() {
        try {
            secretKey = CipherUtil.getSecureRandomKey(CipherUtil.BLOWFISH, 128);
        } catch (NoSuchAlgorithmException ex) {
            ErrorManager.showError(ex, LOGGER);
        }
    }

    @Override
    public Object getSessionInfo(String sessionId, String key) {
        return sessionsInfo.get(new SessionInfo(sessionId, key));
    }

    @Override
    public void addSessionInfo(String sessionId, String key, Object info) {
        IUserSession userSession = getUserSession(sessionId);
        if (userSession == null || userSession.getUser() == null) {
            removeAllSessionInfo(sessionId);
            return;
        }
        sessionsInfo.put(new SessionInfo(sessionId, key), info);
    }

    @Override
    public void removeSessionInfo(String sessionId, String key) {
        this.sessionsInfo.remove(new SessionInfo(sessionId, key));
    }

    private void removeAllSessionInfo(String sessionId) {
        for (Iterator<Map.Entry<SessionInfo, Object>> it = sessionsInfo.entrySet().iterator(); it.hasNext();) {
            Map.Entry<SessionInfo, Object> entry = it.next();
            if (entry.getKey().sessionId.equals(sessionId)) {
                it.remove();
            }
        }
    }

    /**
     * Crea una sesión de usuario para acceso a la app
     *
     * @param userLogin usuario
     * @param password password
     * @param idcompany empresa que esta solicitando ingresar
     * @param idleSessionExpireInMinutes minutos sin actividad antes de cerrar
     * la sesión.
     * @param otherParams
     * @return objeto conteniendo datos del login exitoso o rechazado
     */
    @Override
    @Lock(LockType.WRITE)
    public IUserSession createSession(String userLogin, String password, Object idcompany, Integer idleSessionExpireInMinutes, Map<String, Object> otherParams) {
        LOGGER.debug("CREATESESSION IN");
        IUserSession session;
        try {
            // Verifcar si coincide usuario, contraseña y pasar resultado para ser procesado
            session = login(userLogin, password, otherParams);
            processCreateSession(session, idcompany, idleSessionExpireInMinutes);
            return session;
        } catch (Exception e) {
            ErrorManager.showError(e, LOGGER, logMngr, null);
        }
        return null;
    }

    /**
     * Procesa la creación de la sesión del usuario al sistema.
     *
     * @param session variable creada en el metodo login que va a ser procesada.
     * @param idcompany empresa que esta solicitando ingresar
     * @param idleSessionExpireInMinutes minutos sin actividad antes de cerrar
     * la sesión.
     * @return verdadero o falso si pudo o no crear la sesión.
     * @throws Exception
     */
    @Lock(LockType.WRITE)
    protected boolean processCreateSession(IUserSession session, Object idcompany, Integer idleSessionExpireInMinutes) throws Exception {
        LOGGER.debug("PROCESSCREATESESSION IN");
        if (session == null || session.getUser() == null) {
            return false;
        }
        String sessionId = createSessionId(session);
        // Si se permite solo una sesión por usuario 
        if (oneSessionPerUser) {
            // Válidar que el usuario no este ya logueado                    
            IUserSession sessionCtrl = getUserSession(encrypt(sessionId));
            if (sessionCtrl != null && sessionCtrl.getUser() != null) {
                session.setUser(null);
                String mensaje = session.getUser().getLogin().trim().toUpperCase() + " tiene una sesión activa";
                LOGGER.debug(mensaje);
                session.setError(new ErrorReg(mensaje, 4, ""));
                session.getError().setEntity(getClass().getName());
                session.getError().setEvent(EVENT_CREATESESSION);
                session.getError().setLevel(LEVEL_ALERT);
                logMngr.dbWrite(session.getError());
                return false;
            }
        }
        // Verificar si tiene permiso para acceder a los datos de la empresa
        if (!checkCompanyAccess(((IAppUser) session.getUser()).getIduser(), (Long) idcompany)) {
            session.setUser(null);
            String mensaje = session.getUser().getLogin().trim().toUpperCase() + " no tiene autorización para acceder a esta empresa";
            LOGGER.debug(mensaje);
            session.setError(new ErrorReg(mensaje, 4, ""));
            session.getError().setEntity(getClass().getName());            
            session.getError().setEvent(EVENT_CREATESESSION);
            session.getError().setLevel(LEVEL_ALERT);
            logMngr.dbWrite(session.getError());
            return false;
        }

        Map<String, Object> parameters = new HashMap();
        parameters.put("idcompany", idcompany);

        IAppCompany company = dao.findByQuery(null,
                "select o from AppCompanyLight o "
                + " where idcompany = :idcompany", parameters);

        // Agregar atributos adicionales a la sesión
        String persistUnit = company.getPersistentUnit().trim();
        session.setPersistenceUnit(persistUnit); //Unidad de persistencia
        session.setCompany(company); // Empresa logueada
        session.setIdCompany(Long.valueOf(idcompany.toString()));

        // Id de sesión encriptada por seguridad.
        session.setSessionId(encrypt(sessionId));
        // Tiempo de expiración en minutos desde ultima actividad
        session.setIdleSessionExpireInMinutes(idleSessionExpireInMinutes);

        // Agregar sesión al pool de sesiones
        sessionVar.put(sessionId, session);
        LOGGER.debug("Sesión creada: " + sessionId);
        
        // Metodo que se ejecuta al final del proceso con el fin de que en clases derivadas
        // se pueda realizar tareas adicionales como anexar otros atributos a la sesión.
        afterCreateSession(session);
        return true;
    }

    
    /**
     * Crea una sesión de usuario para acceso a la app
     *
     * @param token token de acceso.
     * @return objeto conteniendo datos del login exitoso o rechazado
     */
    @Override
    @Lock(LockType.WRITE)
    public IUserSession createSessionFromToken(String token) {
        LOGGER.debug("CREATESESSION IN FROM TOKEN");
        try {
            //Limpiar sesion si existiese todavia
            sessionVar.remove(token);
            //Buscar el Token
            IAppAuthConsumerToken authToken = oAuthConsumer.findAuthToken(token);
            //Verificar válidez del token
            IErrorReg error = oAuthConsumer.checkToken(token);
            if (error.getErrorNumber() > 0) {
                LOGGER.error(error.getMessage());
                throw new SessionError(error.getMessage());
            }
            IClientAuthRequestInfo requestInfo = new ClientAuthRequestInfo();
            requestInfo.setAppAuthToken(authToken);
            String userLogin = Fn.nvl(requestInfo.getPropertyValue("userlogin"),"TOKEN");
            if (userLogin.isEmpty()){
                userLogin = "TOKEN";
            }
            //Crear objeto sesion.
            IUserSession session = new UserSession();
            IAppUser appUser = (IAppUser)dao.findByQuery(null, "select o from AppUserLight o where code = :userLogin", Fn.queryParams("userLogin",userLogin));
            if (appUser == null) {
                Class appUserModel = dao.findListByQuery(null, "select o from AppUserLight o where type = 1", null, 0, 1).get(0).getClass();
                appUser = (IAppUser)appUserModel.getConstructor().newInstance();
                appUser.setId(0L);
                appUser.setCode(userLogin);
                appUser.setType(IAppUser.ISUSER);
                appUser.setFullName("TOKEN");
                appUser.setDescription("Acceso via token");
                appUser.setRol(IAppUser.USUARIO);
                appUser.setExpiredDate(LocalDates.toDateTime("31/12/9999"));
                appUser.setDisabled(false);
            }
            LOGGER.debug("CREATESESSION IN FROM TOKEN con el usuario "+userLogin);
            session.setUser(appUser);
            processCreateSessionFromToken(session, authToken, 30);
            return session;
        } catch (Exception e) {
            ErrorManager.showError(e, LOGGER, logMngr, null);
        }
        return null;
    }

    /**
     * Procesa la creación de la sesión del usuario al sistema.
     *
     * @param session variable creada en el metodo login que va a ser procesada.
     * @param authToken token
     * @param idleSessionExpireInMinutes minutos sin actividad antes de cerrar
     * la sesión.
     * @return verdadero o falso si pudo o no crear la sesión.
     * @throws Exception
     */
    @Lock(LockType.WRITE)
    protected boolean processCreateSessionFromToken(IUserSession session, IAppAuthConsumerToken authToken, Integer idleSessionExpireInMinutes) throws Exception {
        LOGGER.debug("PROCESSCREATESESSION IN FROM TOKEN");
        if (authToken == null || session.getUser() == null) {
            return false;
        }
        //Asignar idcompany
        Long idcompany, idcompanytoken;
        IAppCompany appCompanyToken = oAuthConsumer.getCompanyMapped(authToken);
        if (appCompanyToken == null) {
            throw new Exception("La empresa definida en el token no existe");
        }
        idcompany = appCompanyToken.getIdcompany();
        if (appCompanyToken.getIdcompanymask() != null) {
            idcompanytoken = appCompanyToken.getIdcompanymask();
        } else {
            idcompanytoken = appCompanyToken.getIdcompany();
        }

        String token = authToken.getToken();
        // Guardar los datos de autenticación en el cache.
        IClientAuthRequestInfo requestInfo = new ClientAuthRequestInfo();
        requestInfo.setIdcompany(idcompanytoken);
        requestInfo.setAppAuthToken(authToken);

        Map<String, Object> parameters = new HashMap();
        parameters.put("idcompany", idcompany);

        IAppCompany company = dao.findByQuery(null,
                "select o from AppCompanyLight o "
                + " where idcompany = :idcompany", parameters);

        // Agregar atributos adicionales a la sesión
        String persistUnit = company.getPersistentUnit().trim();
        session.setPersistenceUnit(persistUnit); //Unidad de persistencia
        session.setCompany(company); // Empresa logueada
        session.setIdCompany(idcompany);
        session.setClientAuthRequestInfo(requestInfo);

        // Id de sesión 
        session.setSessionId(token);
        // Tiempo de expiración en minutos desde ultima actividad
        session.setIdleSessionExpireInMinutes(idleSessionExpireInMinutes);

        // Agregar sesión al pool de sesiones
        sessionVar.put(token, session);
        //
        LOGGER.debug("Sesión creada: " + token);
        
        // Metodo que se ejecuta al final del proceso con el fin de que en clases derivadas
        // se pueda realizar tareas adicionales como anexar otros atributos a la sesión.
        afterCreateSession(session);
        return true;
    }

    protected void afterCreateSession(IUserSession session) throws Exception {
    }

    /**
     * Vuelve a crear la sesión con el acceso a una nueva empresa
     *
     * @param sessionIdEncrypted identificador de la sesión.
     * @param idcompany identificador de la empresa a la que se solicita el
     * nuevo acceso.
     * @return objeto sesión
     */
    @Override
    @Lock(LockType.WRITE)
    public IUserSession reCreateSession(String sessionIdEncrypted, Object idcompany) {
        LOGGER.debug("RECREATESESSION IN");
        String sessionId = decrypt(sessionIdEncrypted);
        // Verificar válides de la sesión
        UserSession session = getUserSession(sessionIdEncrypted);
        if (session == null || session.getError() != null) {
            if (session == null) {
                return null;
            }
            LOGGER.debug(session.getError().getMessage());
            return session;
        }
        // Eliminar sesión 
        sessionVar.remove(sessionId);
        try {
            //Crear nueva sesión
            processCreateSession(session, idcompany, null);
            return session;
        } catch (Exception e) {
            ErrorManager.showError(e, LOGGER, logMngr, null);
        }
        return null;
    }

    /**
     * Verifica si el iduser proporcionado es válido
     *
     * @param iduser id del usuario
     * @return una variable ErrorReg si es nulo es válido si no hubo algún error
     * @throws java.lang.Exception
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public boolean isUserValid(Long iduser) throws Exception {
        return (checkUser(iduser) == null);
    }

    /**
     * Verifica si el iduser proporcionado es válido
     *
     * @param iduser id del usuario
     * @return una variable ErrorReg si es nulo es válido si no hubo algún error
     * @throws Exception
     */
    protected ErrorReg checkUser(Long iduser) throws Exception {
        LOGGER.debug("IsUserValid IN");
        String mensaje;
        Map<String, Object> params = new HashMap<>();
        params.put("iduser", iduser);
        if (Fn.nvl(iduser, 0L) != 0L) {
            // Verificar existencia del usuario
            IAppUser usuario = dao.findByQuery(null,
                    "select o from AppUserLight o where iduser = :iduser",
                    params);

            // Verificar que exista el usuario
            if (usuario == null) {
                mensaje = "Este usuario " + iduser + " no existe";
                LOGGER.debug(mensaje);
                return new ErrorReg(mensaje, 1, "");
            }
            // Verificar que el usuario este activo.
            if (usuario.getDisabled()) {
                mensaje = "La cuenta " + usuario.getLogin().trim() + " esta inactivo";
                LOGGER.info(mensaje);
                return new ErrorReg(mensaje, 2, "");
            }
            // Verificar que no expiro la cuenta
            if (usuario.getExpiredDate().isBefore(LocalDates.now())) {
                mensaje = "La cuenta " + usuario.getLogin() + " expiro";
                LOGGER.debug(mensaje);
                return new ErrorReg(mensaje, 2, "");
            }
            return null;
        }
        mensaje = "Este usuario " + iduser + " no existe";
        LOGGER.debug(mensaje);
        return new ErrorReg(mensaje, 1, "");
    }

    /**
     * Devuelve verdadero si sus credenciales para el logeo son válidas o falso
     * si no
     *
     * @param userLogin usuario
     * @param password contraseña.
     * @param otherParams
     * @return userSession conteniendo información de la sesión del usuario
     * @throws Exception
     */
    @Override
    public IUserSession login(String userLogin, String password, Map<String, Object> otherParams) throws Exception {
        LOGGER.debug("LOGIN IN");
        String mensaje;
        Map<String, Object> params = new HashMap<>();
        params.put("userLogin", userLogin);
        UserSession userSession;
        if (userLogin != null) {
            // Verificar existencia del usuario
            IAppUser usuario = dao.findByQuery(null,
                    "select o from AppUserLight o where code = :userLogin",
                    params);

            userSession = new UserSession();
            // Verificar que exista el usuario
            if (usuario == null) {
                mensaje = "Este usuario " + userLogin + " no existe";
                LOGGER.debug(mensaje);
                userSession.setError(new ErrorReg(mensaje, 1, ""));
                return userSession;
            }
            // Verificar que el usuario este activo.
            if (usuario.getDisabled()) {
                mensaje = "La cuenta " + usuario.getLogin().trim() + " esta inactivo";
                LOGGER.info(mensaje);
                userSession.setError(new ErrorReg(mensaje, 2, ""));
                return userSession;
            }
            // Verificar que no expiro la cuenta
            if (usuario.getExpiredDate().isBefore(LocalDates.now())) {
                mensaje = "La cuenta " + usuario.getLogin() + " expiro";
                LOGGER.debug(mensaje);
                userSession.setError(new ErrorReg(mensaje, 2, ""));
                return userSession;
            }
            // Verificar que la contraseña sea correcta
            String claveEncriptada = getEncryptedPass(usuario, password);
            if (!claveEncriptada.equals(usuario.getPass())) {
                userSession.setError(new ErrorReg("Contraseña incorrecta", 3, ""));
                return userSession;
            }
            userSession.setUser(usuario);
            return userSession;
        }
        return null;
    }

    /**
     * Cierra una sesión
     *
     * @param sessionIdEncrypted identificador de la sesión a cerrar
     */
    @Override
    @Lock(LockType.WRITE)
    public void logout(String sessionIdEncrypted) {
        LOGGER.debug("LOGOUT IN");
        try {
            String sessionId = decrypt(sessionIdEncrypted);
            sessionVar.remove(sessionId);
        } catch (Exception ex) {
            //
        }
    }

    /**
     * Chequea si un usuario tiene permiso a acceder a una empresa determinada
     *
     * @param iduser identificador del usuario
     * @param idcompany identificador de la empresa
     * @return verdadero si tiene permiso el usuario y falso si no
     * @throws Exception
     */
    @Override
    public Boolean checkCompanyAccess(Long iduser, Long idcompany) throws Exception {
        LOGGER.debug("CHECKCOMPANYACCESS IN");
        Map<String, Object> params = new HashMap<>();
        params.put("iduser", iduser);
        params.put("idcompany", idcompany);
        IAppCompanyAllowed row = dao.findByQuery(null,
                "select o from AppCompanyAllowed o "
                + "where iduser = :iduser  and idcompany = :idcompany",
                params);
        if (row != null) {
            return !row.getDeny();
        }
        return true;
    }

    /**
     * Devuelve un objeto sesión correspondiente a una sesión solicitada
     *
     * @param sessionIdEncrypted identificador de la sesión encriptada.
     * @return objeto con los datos de la sesión solicitada
     */
    @Override
    public UserSession getUserSession(String sessionIdEncrypted) {
        LOGGER.debug("GETUSERSESSION IN ");
        if (sessionIdEncrypted == null) {
            return null;
        }
        String sessionId = sessionIdEncrypted;
        UserSession sesion = (UserSession) sessionVar.get(sessionId);
        if (sesion == null) {
            try {
                sessionId = decrypt(sessionIdEncrypted);
                LOGGER.debug("SESSION ENCRYPTADA: " + sessionIdEncrypted);
            } catch (Exception exp) {
                return null;
            }
            sesion = (UserSession) sessionVar.get(sessionId);
        }
        LOGGER.debug("SESSION : " + sessionId);
        if (sesion != null) {
            Integer expireInMinutes = sesion.getIdleSessionExpireInMinutes();
            if (expireInMinutes == null) {
                expireInMinutes = 30;
            }
            // Verificar si ya expiro su sesión
            LocalDateTime lastReference = sesion.getLastReference();
            
            long idleInMinutes = LocalDates.minutesInterval(lastReference, LocalDates.now());
            // Diferencias en minutos desde la ultima vez que se hizo referencia a esta sesión.        
            if (idleInMinutes >= expireInMinutes) {
                sessionVar.remove(sessionId);
                sesion.setUser(null);
                sesion.setCompany(null);
                sesion.setClientAuthRequestInfo(null);
                sesion.setEmpresa(null);
                String mensaje = "La sesión expiro";
                sesion.setError(new ErrorReg(mensaje, 6, ""));
                return sesion;
            }
            sesion.setLastReference(LocalDates.now());
        }
        return sesion;
    }

    /**
     * Objeto con la información necesaria para acceder a la base de datos.
     * (persistunit, session del usuario)
     *
     * @param sessionId identificador de la sesión o el token
     * @return DBLinkInfo
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @Override
    public IDBLinkInfo getDBLinkInfo(String sessionId) {
        IDBLinkInfo dbLinkInfo = new DBLinkInfo();
        if (!Strings.isNullorEmpty(sessionId)) {
            IUserSession userSession = getUserSession(sessionId);
            if (userSession != null && userSession.getUser() != null) {
                dbLinkInfo.setUserSession(userSession);
                //Si la conexión fue hecha via token
                if (userSession.getClientAuthRequestInfo() != null) {
                    IAppAuthConsumerToken token = oAuthConsumer.findAuthToken(sessionId);
                    try {
                        dbLinkInfo.setToken(token, oAuthConsumer, true);
                    } catch (Exception exp) {
                        ErrorManager.showError(exp, LOGGER);
                    }
                }
            }
        }
        return dbLinkInfo;
    }

    /**
     * Verifica consistencia de el modelo AuthConsumerData
     *
     * @param data modelo OAuthConsumerData
     * @return verdadero si es válido o falso si no
     */
    @Override
    public boolean checkAuthConsumerData(IOAuthConsumerData data) {
        if (data == null) {
            return false;
        }
        try {
            Long iduser = data.getIdAppUser();
            Long idcompany = data.getIdCompany();
            String userLogin = data.getUserLogin();
            String userPass = data.getUserPass();
            if (!Fn.nvl(userLogin, "").isEmpty()) {
                IUserSession session = login(userLogin, userPass, null);
                if (session == null || session.getError() != null) {
                    if (session != null && session.getError() != null){
                        LOGGER.info("Consumer data ERROR: "+session.getError().getMessage());
                    }
                    return false;
                }
                iduser = session.getUser().getIduser();
            } else {
                if (checkUser(iduser) != null) {
                    return false;
                }
            }
            if (idcompany != 0 && !checkCompanyAccess(iduser, idcompany)){
                LOGGER.info("Consumer data ERROR: "+"El usuario no tiene acceso a la empresa");
                return false;
            }
            return true;
        } catch (Exception exp) {
            ErrorManager.showError(exp, LOGGER);
        }
        return false;
    }

    /**
     * Devuelve un texto que identificará a la sesión de forma unica.
     *
     * @param userSession sesión creada desde el metodo login()
     * @return identificador de sesión.
     */
    protected String createSessionId(IUserSession userSession) {
        if (userSession == null || userSession.getUser() == null) {
            return null;
        }
        // Esto puede ser modificado en clases derivadas.
        String sessionId = userSession.getUser().getId() + ":" + userSession.getUser().getPass().toUpperCase().trim();
        Date fecha = new Date();
        if (!oneSessionPerUser) {
            sessionId += ":" + fecha.getTime();
        }
        return sessionId;
    }

    /**
     * Password cifrado que se utilizará para comparar con el password
     * almacenado en la base de datos.
     *
     * @param user usuario
     * @param password password
     * @return Password cifrado que se utilizará para comparar con el dato
     * almacenado en la base de datos.
     */
    protected String getEncryptedPass(IAppUser user, String password) {
        // Esto puede ser modificado en clases derivadas
        String md5 = user.getLogin().toUpperCase().trim() + ":" + password.trim();
        return DigestUtil.md5(md5).toUpperCase();
    }

    /**
     * Encripta un mensaje (en este caso el sesionId)
     *
     * @param msg mensaje
     * @return mensaje cifrado
     */
    protected final String encrypt(String msg) {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(CipherUtil.BLOWFISH);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(msg.getBytes());
            return Fn.bytesToHex(encrypted);
        } catch (InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    /**
     * Desencripta un mensaje (en este caso el sessionId)
     *
     * @param msgEncrypted mensaje encriptado.
     * @return mensaje descifrado
     */
    protected final String decrypt(String msgEncrypted) {
        try {
            byte[] encrypted = Fn.hexToByte(msgEncrypted);
            Cipher cipher = Cipher.getInstance(CipherUtil.BLOWFISH);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(encrypted));
        } catch (InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    @Override
    public IClientAuthRequestInfo getClientAuthRequestCache(String token) {
        IUserSession session = getUserSession(token);
        if (session == null || session.getError() != null) {
            return null;
        }
        IClientAuthRequestInfo info = session.getClientAuthRequestInfo();
        //Si no existe
        if (info == null) {
            return null;
        }
        //Si se registro en el cache hace más de un día eliminar del cache
        if (LocalDates.daysInterval(info.getLogDate(), LocalDates.now()) > 1) {
            sessionVar.remove(token);
            return null;
        }
        return info;
    }

    public class SessionInfo {

        String sessionId;
        String key;

        public SessionInfo() {
        }

        public SessionInfo(String sessionId, String key) {
            this.sessionId = sessionId;
            this.key = key;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SessionInfo other = (SessionInfo) obj;
            if (!Objects.equals(this.sessionId, other.sessionId)) {
                return false;
            }
            if (!Objects.equals(this.key, other.key)) {
                return false;
            }
            return true;
        }
    }
}
