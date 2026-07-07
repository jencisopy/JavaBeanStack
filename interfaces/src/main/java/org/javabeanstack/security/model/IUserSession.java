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

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import org.javabeanstack.data.IDBFilter;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.model.IAppCompany;
import org.javabeanstack.model.IAppUser;

/**
 *
 * @author Jorge Enciso
 */
public interface IUserSession extends Serializable {

    /**
     *
     * @return Devuelve el objeto empresa a la cual se accedio en esta sesión.
     */
    IAppCompany getCompany();
    /**
     *
     * @return Devuelve el objeto empresa a la cual se accedio en esta sesión.
     */
    IAppCompany getEmpresa();

    /**
     *
     * @return Devuelve un objeto error si la creación de la sesión no fue exitosa
     */
    IErrorReg getError();

    /**
     *
     * @return Devuelve el nombre de la máquina desde la cual fue creada la sesión.
     */
    String getHost();

    /**
     *
     * @return Devuelve el ip de la terminal, desde la cual fue creada la sesión
     */
    String getIp();

    /**
     *
     * @return Devuelve la ultima fecha y hora que se hizo referencia a la sesión.
     */
    LocalDateTime getLastReference();

    /**
     *
     * @return Devuelve la unidad de persistencia donde se encuentra la configuración
     * para acceso a la base de datos donde esta la información de la empresa logeada
     */
    String getPersistenceUnit();

    /**
     *
     * @return Devuelve el identificador de la sesión
     */
    String getSessionId();

    /**
     *
     * @return Devuelve la fecha y hora que fue logeada la sesión
     */
    LocalDateTime getTimeLogin();
    Long getIdCompany();    
    Long getIdEmpresa();
    Integer getIdleSessionExpireInMinutes();
    <T extends IDBFilter> T getDBFilter();
    IAppUser getUser();
    void setCompany(IAppCompany company);    
    void setEmpresa(IAppCompany empresa);
    void setIdCompany(Long idcompany);        
    void setIdEmpresa(Long idempresa);    
    void setError(IErrorReg error);
    void setHost(String host);
    void setIp(String ip);
    void setLastReference(LocalDateTime date);
    void setPersistenceUnit(String persistenceUnit);
    void setSessionId(String sessionId);
    void setTimeLogin(LocalDateTime timeLogin);
    void setUser(IAppUser user);
    void setIdleSessionExpireInMinutes(Integer minutes);
    <T extends IDBFilter> void setDBFilter(T dbFilter);    
    Map<String,Object> getInfo();
    Object getInfo(String key);    
    void addInfo(String key, Object info);
    IClientAuthRequestInfo getClientAuthRequestInfo();
    void setClientAuthRequestInfo(IClientAuthRequestInfo requestInfo);
}
