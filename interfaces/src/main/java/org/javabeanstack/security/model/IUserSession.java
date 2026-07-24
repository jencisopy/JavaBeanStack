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
 * Contrato de la sesión de usuario autenticado: contexto de la sesión con el
 * usuario ({@link IAppUser}), la empresa activa ({@link IAppCompany}), la unidad
 * de persistencia, el filtro de datos ({@link IDBFilter}) y los datos de
 * auditoría (host, ip, fechas) que la capa de datos y de seguridad consultan en
 * cada operación.
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

    /**
     * Devuelve el identificador de la empresa activa.
     *
     * @return identificador de la empresa.
     */
    Long getIdCompany();

    /**
     * Devuelve el identificador de la empresa activa (alias {@code idempresa}).
     *
     * @return identificador de la empresa.
     */
    Long getIdEmpresa();

    /**
     * Devuelve los minutos de inactividad tras los cuales expira la sesión.
     *
     * @return minutos de expiración por inactividad.
     */
    Integer getIdleSessionExpireInMinutes();

    /**
     * Devuelve el filtro de datos aplicable a la sesión.
     *
     * @param <T> tipo del filtro.
     * @return filtro de datos.
     */
    <T extends IDBFilter> T getDBFilter();

    /**
     * Devuelve el usuario autenticado en la sesión.
     *
     * @return usuario de la sesión.
     */
    IAppUser getUser();

    /**
     * Asigna la empresa activa de la sesión.
     *
     * @param company empresa activa.
     */
    void setCompany(IAppCompany company);

    /**
     * Asigna la empresa activa de la sesión (alias {@code empresa}).
     *
     * @param empresa empresa activa.
     */
    void setEmpresa(IAppCompany empresa);

    /**
     * Asigna el identificador de la empresa activa.
     *
     * @param idcompany identificador de la empresa.
     */
    void setIdCompany(Long idcompany);

    /**
     * Asigna el identificador de la empresa activa (alias {@code idempresa}).
     *
     * @param idempresa identificador de la empresa.
     */
    void setIdEmpresa(Long idempresa);

    /**
     * Asigna el error de creación de la sesión.
     *
     * @param error registro de error.
     */
    void setError(IErrorReg error);

    /**
     * Asigna el nombre de la máquina desde la cual se creó la sesión.
     *
     * @param host nombre de la máquina.
     */
    void setHost(String host);

    /**
     * Asigna la ip de la terminal desde la cual se creó la sesión.
     *
     * @param ip dirección ip.
     */
    void setIp(String ip);

    /**
     * Asigna la última fecha y hora de referencia a la sesión.
     *
     * @param date fecha y hora de referencia.
     */
    void setLastReference(LocalDateTime date);

    /**
     * Asigna la unidad de persistencia de la sesión.
     *
     * @param persistenceUnit nombre de la unidad de persistencia.
     */
    void setPersistenceUnit(String persistenceUnit);

    /**
     * Asigna el identificador de la sesión.
     *
     * @param sessionId identificador de la sesión.
     */
    void setSessionId(String sessionId);

    /**
     * Asigna la fecha y hora de login de la sesión.
     *
     * @param timeLogin fecha y hora de login.
     */
    void setTimeLogin(LocalDateTime timeLogin);

    /**
     * Asigna el usuario autenticado de la sesión.
     *
     * @param user usuario de la sesión.
     */
    void setUser(IAppUser user);

    /**
     * Asigna los minutos de inactividad tras los cuales expira la sesión.
     *
     * @param minutes minutos de expiración por inactividad.
     */
    void setIdleSessionExpireInMinutes(Integer minutes);

    /**
     * Asigna el filtro de datos aplicable a la sesión.
     *
     * @param <T> tipo del filtro.
     * @param dbFilter filtro de datos.
     */
    <T extends IDBFilter> void setDBFilter(T dbFilter);

    /**
     * Devuelve el mapa de datos libres asociados a la sesión.
     *
     * @return mapa de datos de la sesión.
     */
    Map<String,Object> getInfo();

    /**
     * Devuelve un dato libre de la sesión por su clave.
     *
     * @param key clave del dato.
     * @return valor del dato, o {@code null} si no existe.
     */
    Object getInfo(String key);

    /**
     * Agrega o reemplaza un dato libre de la sesión.
     *
     * @param key clave del dato.
     * @param info valor del dato.
     */
    void addInfo(String key, Object info);

    /**
     * Devuelve la información de la solicitud de autenticación asociada a la sesión.
     *
     * @return información de la solicitud de autenticación.
     */
    IClientAuthRequestInfo getClientAuthRequestInfo();

    /**
     * Asigna la información de la solicitud de autenticación de la sesión.
     *
     * @param requestInfo información de la solicitud de autenticación.
     */
    void setClientAuthRequestInfo(IClientAuthRequestInfo requestInfo);
}
