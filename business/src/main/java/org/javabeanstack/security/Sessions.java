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
import org.javabeanstack.util.Dates;


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
        return true;
    }
    
    
    /**
     * Devuelve un objeto sesión correspondiente a una sesión solicitada
     * @param sessionId   identificador de la sesión.
     * @return objeto con los datos de la sesión solicitada
     */
    @Override
    public UserSession getUserSession(String sessionId){
        return null;
    }
}
