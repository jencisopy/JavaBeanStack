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

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Startup;
import org.apache.log4j.Logger;

import org.javabeanstack.data.DBManager;
import org.javabeanstack.data.DataLink;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.error.ErrorReg;
import org.javabeanstack.util.Fn;
import org.javabeanstack.model.IDicPermisoEmpresa;
import org.javabeanstack.model.IEmpresa;

import org.javabeanstack.model.IUser;
import org.javabeanstack.data.IGenericDAO;


/**
 * Es la clase encargada de todas las sesiones de usuarios.
 * Guarda información de cada sesión y gestión la creación y expiración de la sesión.
 * 
 * El test unitario se encuentra en TestProjects clase
 *     py.com.oym.test.data.TestSesiones
 * 
 * @author Jorge Enciso
 */
//@Singleton
@Startup
@Lock(LockType.READ)
public class Sessions implements ISessions, ISessionsLocal, ISessionsRemote{
    private static final Logger   LOGGER = Logger.getLogger(Sessions.class);
    Map<String, Object> sessionVar = new HashMap<>();    
    
    @EJB private IGenericDAO dao;
    
    public Sessions(){
        
    }
    
    /**
     * Crea una sesión de usuario para acceso a la app
     * 
     * @param userLogin     usuario
     * @param password      password
     * @param idempresa     empresa que esta solicitando ingresar
     * @param idleSessionExpireInMinutes    minutos sin actividad antes de cerrar la sesión.    
     * @return objeto conteniendo datos del login exitoso o rechazado
     */    

    /* TODO analizar que no pueda ingresar el mismo usuario más de una vez */
    @Override
    @Lock(LockType.WRITE)
    public IUserSession createSession(String userLogin, String password, Object idempresa, Integer idleSessionExpireInMinutes){
        try{
            IUserSession session;      
            // Verifcar si coincide usuario y contraseña, si esta o no activo
            session = login(userLogin, password);
            if (session != null && session.getUser() != null){
                Date fecha = new Date();
                String usuarioCod = userLogin.toUpperCase().trim();
                String usuarioRol = ((IUser)session.getUser()).getRol().trim();
                String md5   = usuarioCod + usuarioRol + password.trim() + idempresa + fecha;        
                // Verificar si tiene permiso para acceder a los datos de la empresa
                if (!checkEmpresaPermision(((IUser)session.getUser()).getIdusuario(),(Long)idempresa)){
                    session.setUser(null);
                    String mensaje = "No tiene autorización para acceder a esta empresa";
                    LOGGER.debug(mensaje);
                    session.setError(new ErrorReg(mensaje,4,""));
                    return session;
                }
                
                DataLink dataLink = new DataLink(dao);  
                
                Map<String, Object> parameters = new HashMap();
                parameters.put("idempresa", idempresa);
                
                IEmpresa empresa= dataLink.findByQuery(IEmpresa.class,
                                  "select o from Empresa o "
                                + " where idempresa = :idempresa",parameters);

                String token = idempresa+"-"+Fn.getMD5(md5);
                
                String persistUnit = empresa.getDatos().trim();
                session.setPersistenceUnit(persistUnit);
                session.setEmpresa(empresa);
                idempresa = session.getIdEmpresa();
                session.setIdEmpresa(Long.parseLong(idempresa.toString()));

                session.setSessionId(token);
                session.setIdleSessionExpireInMinutes(idleSessionExpireInMinutes);
                sessionVar.put(token, session);
            }
            return session;
        }
        catch (Exception exp){
            ErrorManager.showError(exp, LOGGER);
        }
        return null;
    }
    
    /**
     * Devuelve verdadero si sus credenciales para el logeo son válidas o falso si no
     * 
     * @param userLogin usuario
     * @param password  contraseña.
     * @return  userSession conteniendo información de la sesión del usuario
     * @throws Exception 
     */
    @Override
    public IUserSession login(String userLogin, String password) throws Exception {
        LOGGER.debug("LOGIN IN");
        String mensaje;
        Map<String,Object> params = new HashMap<>();
        params.put("userLogin", userLogin);
        UserSession userSession;
        if (userLogin != null) {
            // Verificar existencia del usuario
            IUser usuario = dao.findByQuery(IUser.class,DBManager.CATALOGO,
                        "select o from Usuario o where codigo = :userLogin",
                        params);
            
            userSession = new UserSession();
            // Verificar que exista el usuario
            if (usuario == null) {
                mensaje = "Este usuario "+userLogin+" no existe";
                LOGGER.debug(mensaje);                
                userSession.setError(new ErrorReg(mensaje,1,""));
                return userSession;
            }
            // Verificar que el usuario este activo.
            if (usuario.getDisable()){
                mensaje = "La cuenta "+usuario.getCodigo().trim()+" esta inactivo";                
                LOGGER.info(mensaje);
                userSession.setError(new ErrorReg(mensaje,2,""));
                return userSession;
            }
            // Verificar que no expiro la cuenta
            if (usuario.getExpira().before(Fn.now())){
                mensaje = "La cuenta "+usuario.getCodigo()+" expiro";
                LOGGER.debug(mensaje);
                userSession.setError(new ErrorReg(mensaje,2,""));
                return userSession;
            }
            // Verificar que la contraseña sea correcta
            String md5 = usuario.getCodigo().toUpperCase().trim() + 
                    usuario.getRol().trim() + 
                    password.trim();
                
            String claveEncriptada = Fn.getMD5(md5);
            if (!claveEncriptada.equals(usuario.getClave())){
                userSession.setError(new ErrorReg("Contraseña incorrecta",3,""));
                return userSession;
            }
            userSession.setUser(usuario);
            return userSession;
        }
        return null;
    }

    /**
     * Cierra una sesión
     * @param sessionId  identificador de la sesión a cerrar
     */
    @Override
    @Lock(LockType.WRITE)
    public void logout(String sessionId){
        try {
            sessionVar.remove(sessionId);
        }
        catch (Exception exp){
            //
        }
    }
    
    /**
     *  Chequea si un usuario tiene permiso a acceder a una empresa determinada
     * 
     * @param idusuario identificador del usuario
     * @param idempresa identificador de la empresa
     * @return verdadero si tiene permiso el usuario y falso si no
     * @throws Exception 
     */
    @Override
    public Boolean checkEmpresaPermision(Long idusuario, Long idempresa) throws Exception{
        Map<String, Object> params = new HashMap<>();
        params.put("idusuario", idusuario);
        params.put("idempresa", idempresa);  
        IDicPermisoEmpresa row = dao.findByQuery(IDicPermisoEmpresa.class, DBManager.CATALOGO, 
                                                "select o from DicPermisoEmpresa o "
                                              + "where idusuario = :idusuario  and idempresa = :idempresa", 
                                                params);
        if (row != null){
            return !row.getNegar();
        }
        return true;
    }
    
    
    /**
     * Devuelve un objeto sesión correspondiente a una sesión solicitada
     * @param sessionId   identificador de la sesión.
     * @return objeto con los datos de la sesión solicitada
     */
    @Override
    public UserSession getUserSession(String sessionId){
        UserSession sesion = (UserSession)sessionVar.get(sessionId);
        if (sesion != null){
            Integer expireInMinutes = sesion.getIdleSessionExpireInMinutes();
            if (expireInMinutes == null){
                expireInMinutes = 30;
            }
            // Verificar si ya expiro su sesión
            Calendar cal1 = Calendar.getInstance();
            Calendar cal2 = Calendar.getInstance();
            cal1.setTime(sesion.getLastReference());
            cal2.setTime(new Date());
            long time1 = cal1.getTimeInMillis();
            long time2 = cal2.getTimeInMillis();
            // Diferencias en minutos desde la ultima vez que se hizo referencia a esta sesión.        
            long idleInMinutes = (time2 - time1)/(60 * 1000);
            if (idleInMinutes >= expireInMinutes){
                sessionVar.remove(sessionId);
                sesion.setUser(null);
                String mensaje = "La sesión expiro";
                sesion.setError(new ErrorReg(mensaje, 6, ""));
                return sesion;
            }
            sesion.setLastReference(new Date());
        }
        return sesion;
    }
}
