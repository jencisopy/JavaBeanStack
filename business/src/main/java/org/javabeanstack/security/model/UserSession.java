/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
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


import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.javabeanstack.data.IDBFilter;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.util.Fn;
import org.javabeanstack.model.IAppCompany;
import org.javabeanstack.model.IAppUser;
import org.javabeanstack.util.LocalDates;

/**
 * Esta clase guarda información de la sesión de un usuario.
 * Es utilizado en SecManager y en Sessions principalmente.
 * 
 * @author Jorge Enciso
 */
public class UserSession implements IUserSession{ 
    private IAppUser user;
    private LocalDateTime timeLogin = LocalDates.now();
    private LocalDateTime lastReference = LocalDates.now();
    private IAppCompany company;
    private Long idcompany;
    private String ip;
    private String host;
    private String persistenceUnit;
    private String sessionId;
    private IErrorReg error;
    private Integer idleSessionExpireInMinutes;
    private IDBFilter dbFilter;
    private Map<String, Object> info = new HashMap();
    private IClientAuthRequestInfo clientAuthRequestInfo;

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
    public LocalDateTime getTimeLogin() {
        return timeLogin;
    }

    /**
     * Asigna la fecha y hora que fue iniciada la sesión.
     * @param timeLogin fecha y hora.
     */
    @Override
    public void setTimeLogin(LocalDateTime timeLogin) {
        this.timeLogin = timeLogin;
    }

    /**
     * Devuelve la ultima fecha y hora que se hizo referencia a la sesión.
     * 
     * @return Devuelve la ultima fecha y hora que se hizo referencia a la sesión.
     */
    @Override
    public LocalDateTime getLastReference() {
        return lastReference;
    }

    /**
     * Asigna la ultima vez que se hizo referencia a la sesión.
     * @param date fecha y hora.
     */
    @Override
    public void setLastReference(LocalDateTime date) {
        this.lastReference = date;
    }
    
    /**
     * Empresa seleccionada
     * 
     * @return Devuelve el objeto company a la cual se accedio en esta sesión.
     */
    @Override
    public IAppCompany getCompany() {
        return company;
    }

    /**
     * Devuelve la empresa activa de la sesión (alias de {@link #getCompany()}).
     * 
     * @return Devuelve el objeto company a la cual se accedio en esta sesión.
     */
    @Override
    public IAppCompany getEmpresa() {
        return getCompany();
    }
    
    /**
     * Asigna la empresa activa de la sesión.
     *
     * @param company empresa activa.
     */
    @Override
    public void setCompany(IAppCompany company) {
        this.company = company;
    }

    /**
     * Asigna la empresa activa de la sesión (alias {@code empresa}).
     *
     * @param empresa empresa activa.
     */
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
     * Devuelve el host del cliente.
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
     * Devuelve la unidad de persistencia asociada a la sesión.
     * 
     * @return Devuelve la unidad de persistencia donde se encuentra la configuración
 para acceso a la base de datos donde esta la información de la company logeada
     */
    @Override
    public String getPersistenceUnit() {
        return persistenceUnit;
    }

    /**
     * Asigna la unidad de persistencia de la sesión.
     *
     * @param persistenceUnit nombre de la unidad de persistencia.
     */
    @Override
    public void setPersistenceUnit(String persistenceUnit) {
        this.persistenceUnit = persistenceUnit;
    }

    /**
     * Devuelve el identificador de la sesión.
     * 
     * @return Devuelve el identificador de la sesión
     */
    @Override
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Asigna el identificador de la sesión.
     *
     * @param sessionId identificador de la sesión.
     */
    @Override
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }    

    /**
     * Devuelve el error de creación de la sesión, si la creación no fue exitosa.
     * 
     * @return Devuelve un objeto error si la creación de la sesión no fue exitosa
     */
    @Override
    public IErrorReg getError() {
        return error;
    }

    /**
     * Asigna el error de creación de la sesión.
     *
     * @param error registro de error.
     */
    @Override
    public void setError(IErrorReg error) {
        this.error = error;
    }


    /**
     * Devuelve el identificador de la empresa activa.
     *
     * @return identificador de la empresa.
     */
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

    /**
     * Devuelve el identificador de la empresa activa (alias {@code idempresa}).
     *
     * @return identificador de la empresa.
     */
    @Override
    public Long getIdEmpresa() {
        return getIdCompany();
    }

    /**
     * Asigna el identificador de la empresa activa.
     *
     * @param idcompany identificador de la empresa.
     */
    @Override
    public void setIdCompany(Long idcompany) {
        this.idcompany = idcompany;
    }

    /**
     * Asigna el identificador de la empresa activa (alias {@code idempresa}).
     *
     * @param idempresa identificador de la empresa.
     */
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
    
    /**
     * Devuelve el filtro de datos aplicable a la sesión.
     *
     * @return filtro de datos.
     */
    @Override
    public IDBFilter getDBFilter() {
        return dbFilter;
    }

    /**
     * Asigna el filtro de datos aplicable a la sesión.
     *
     * @param dbFilter filtro de datos.
     */
    @Override
    public void setDBFilter(IDBFilter dbFilter) {
        this.dbFilter = dbFilter;
    }

    /**
     * Devuelve el mapa de datos libres asociados a la sesión.
     *
     * @return mapa de datos de la sesión.
     */
    @Override
    public Map<String, Object> getInfo() {
        return info;
    }

    /**
     * Devuelve un dato libre de la sesión por su clave.
     *
     * @param key clave del dato.
     * @return valor del dato, o {@code null} si no existe.
     */
    @Override
    public  Object getInfo(String key) {
        return info.get(key);
    }
    
    /**
     * Agrega o reemplaza un dato libre de la sesión.
     *
     * @param key clave del dato.
     * @param info valor del dato.
     */
    @Override
    public void addInfo(String key, Object info) {
        this.info.put(key, info);
    }

    /**
     * Devuelve la información de la solicitud de autenticación asociada a la sesión.
     *
     * @return información de la solicitud de autenticación.
     */
    @Override
    public IClientAuthRequestInfo getClientAuthRequestInfo() {
        return this.clientAuthRequestInfo;
    }

    /**
     * Asigna la información de la solicitud de autenticación de la sesión.
     *
     * @param requestInfo información de la solicitud de autenticación.
     */
    @Override
    public void setClientAuthRequestInfo(IClientAuthRequestInfo requestInfo) {
        this.clientAuthRequestInfo = requestInfo;
    }
}
