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


import java.util.Date;
import org.javabeanstack.data.DBLinkInfo;
import org.javabeanstack.data.IDBLinkInfo;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.model.IEmpresa;
import org.javabeanstack.model.IUser;
import org.javabeanstack.util.Fn;

/**
 * Esta clase guarda información de la sesión de un usuario.
 * Es utilizado en SecManager y en Sessions principalmente.
 * 
 * @author Jorge Enciso
 */
public class UserSession implements IUserSession{
    private IUser user;
    private Date timeLogin = new Date();
    private Date lastReference = new Date();
    private IEmpresa empresa;
    private Long idempresa;
    private String ip;
    private String host;
    private String persistenceUnit;
    private String sessionId;
    private IErrorReg error;
    private Integer idleSessionExpireInMinutes;

    public UserSession() {
    }

    /**
     * Devuelve el objeto usuario
     * 
     * @return Devuelve el objeto usuario
     */
    @Override
    public IUser getUser() {
        return user;
    }

    /**
     * Asigna el objeto usuario a la clase
     * @param user objeto usuario
     */
    @Override
    public void setUser(IUser user) {
        this.user = user;
    }

    /**
     * Devuelve la fecha y hora que fue iniciada la sesión
     * 
     * @return Devuelve la fecha y hora que fue iniciada la sesión
     */
    @Override
    public Date getTimeLogin() {
        return timeLogin;
    }

    /**
     * Asigna la fecha y hora que fue iniciada la sesión.
     * @param timeLogin fecha y hora.
     */
    @Override
    public void setTimeLogin(Date timeLogin) {
        this.timeLogin = timeLogin;
    }

    /**
     * Devuelve la ultima fecha y hora que se hizo referencia a la sesión.
     * 
     * @return Devuelve la ultima fecha y hora que se hizo referencia a la sesión.
     */
    @Override
    public Date getLastReference() {
        return lastReference;
    }

    /**
     * Asigna la ultima vez que se hizo referencia a la sesión.
     * @param date fecha y hora.
     */
    @Override
    public void setLastReference(Date date) {
        this.lastReference = date;
    }
    
    /**
     * 
     * @return Devuelve el objeto empresa a la cual se accedio en esta sesión.
     */
    @Override
    public IEmpresa getEmpresa() {
        return empresa;
    }

    @Override
    public void setEmpresa(IEmpresa empresa) {
        this.empresa = empresa;
    }

    
    /**
     * Devuelve el ip de la terminal, desde la cual fue creada la sesión.
     * @return Devuelve el ip de la terminal, desde la cual fue creada la sesión
     */
    @Override
    public String getIp() {
        return ip;
    }

    /**
     * Asigna el ip de la terminal, desde la cual fue creada la sesión.    
     * 
     * @param ip    ip del cliente.
    */
    @Override
    public void setIp(String ip) {
        this.ip = ip;
    }

    /**
     * 
     * @return Devuelve el nombre de la máquina desde la cual fue creada la sesión.
     */
    @Override
    public String getHost() {
        return host;
    }

    /**
     * Asigna el nombre de la máquina desde la cual fue creada la sesión.
     * @param host 
     */
    @Override
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * 
     * @return Devuelve la unidad de persistencia donde se encuentra la configuración
     * para acceso a la base de datos donde esta la información de la empresa logeada
     */
    @Override
    public String getPersistenceUnit() {
        return persistenceUnit;
    }

    @Override
    public void setPersistenceUnit(String persistenceUnit) {
        this.persistenceUnit = persistenceUnit;
    }

    /**
     * 
     * @return Devuelve el identificador de la sesión
     */
    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }    

    /**
     * 
     * @return Devuelve un objeto error si la creación de la sesión no fue exitosa
     */
    @Override
    public IErrorReg getError() {
        return error;
    }

    @Override
    public void setError(IErrorReg error) {
        this.error = error;
    }

    @Override
    public Long getIdEmpresa() {
        if (getEmpresa() != null){
            if (Fn.nvl(getEmpresa().getIdempresamask(),0L) != 0L){
                return getEmpresa().getIdempresamask();
            }
            else{
                return getEmpresa().getIdempresa();
            }
        }
        return idempresa;
    }

    @Override
    public void setIdEmpresa(Long idempresa) {
        this.idempresa = idempresa;
    }

    /**
     * Devuelve minutos que la sesión puede estar inactiva antes de que sea cerrada.
     * @return minutos inactivos antes de cerrarse la sesión.
     */
    @Override
    public Integer getIdleSessionExpireInMinutes() {
        return idleSessionExpireInMinutes;
    }

    /**
     * Setea la cantidad de minutos inactivos que debe estar la sesión antes de cerrarse.
     * @param minutes minutos inactivos.
     */
    @Override
    public void setIdleSessionExpireInMinutes(Integer minutes) {
        this.idleSessionExpireInMinutes = minutes;
    }
    
    @Override
    public IDBLinkInfo getDbLinkInfo(){
        IDBLinkInfo dbInfo = new DBLinkInfo();
        dbInfo.setUserSession(this);
        return dbInfo;
    }
}
