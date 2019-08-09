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


package org.javabeanstack.security.model;


import java.util.Date;
import org.javabeanstack.data.IDBFilter;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.util.Fn;
import org.javabeanstack.model.IAppCompany;
import org.javabeanstack.model.IAppUser;

/**
 * Esta clase guarda información de la sesión de un usuario.
 * Es utilizado en SecManager y en Sessions principalmente.
 * 
 * @author Jorge Enciso
 */
public class UserSession implements IUserSession{ 
    private IAppUser user;
    private Date timeLogin = new Date();
    private Date lastReference = new Date();
    private IAppCompany company;
    private Long idcompany;
    private String ip;
    private String host;
    private String persistenceUnit;
    private String sessionId;
    private IErrorReg error;
    private Integer idleSessionExpireInMinutes;
    private IDBFilter dbFilter;

    /**
     * Devuelve el objeto usuario
     * 
     * @return Devuelve el objeto usuario
     */
    @Override
    public IAppUser getUser() {
        return user;
    }

    
    /**
     * Asigna el objeto usuario a la clase
     * @param user objeto usuario
     */
    @Override
    public void setUser(IAppUser user) {
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
     * @return Devuelve el objeto company a la cual se accedio en esta sesión.
     */
    @Override
    public IAppCompany getCompany() {
        return company;
    }

    /**
     * 
     * @return Devuelve el objeto company a la cual se accedio en esta sesión.
     */
    @Override
    public IAppCompany getEmpresa() {
        return getCompany();
    }
    
    @Override
    public void setCompany(IAppCompany company) {
        this.company = company;
    }

    @Override
    public void setEmpresa(IAppCompany empresa) {
        this.company = empresa;
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
 para acceso a la base de datos donde esta la información de la company logeada
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
    public Long getIdCompany() {
        if (getCompany() != null){
            if (Fn.nvl(getCompany().getIdcompanymask(),0L) != 0L){
                return getCompany().getIdcompanymask();
            }
            else{
                return getCompany().getIdcompany();
            }
        }
        return idcompany;
    }

    @Override
    public Long getIdEmpresa() {
        return getIdCompany();
    }

    @Override
    public void setIdCompany(Long idcompany) {
        this.idcompany = idcompany;
    }

    @Override
    public void setIdEmpresa(Long idempresa) {
        this.idcompany = idempresa;
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
    public IDBFilter getDBFilter() {
        return dbFilter;
    }

    @Override
    public void setDBFilter(IDBFilter dbFilter) {
        this.dbFilter = dbFilter;
    }
    
    @Override
    public final boolean isAdministrator(){
        if (user == null){
            return false;
        }
        return user.getRol().contains("20") || user.getRol().contains("00");
    }
}
