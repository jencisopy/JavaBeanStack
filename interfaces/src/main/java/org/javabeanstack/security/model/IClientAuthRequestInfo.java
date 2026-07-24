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
package org.javabeanstack.security.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import org.javabeanstack.model.IAppAuthConsumerToken;

/**
 * Contrato de la información cacheada de una solicitud de autenticación por
 * token: guarda el token, la empresa, la fecha de registro y la cantidad de
 * usos, para evitar revalidar el token en cada request.
 *
 * @author Jorge Enciso
 */
public interface IClientAuthRequestInfo extends Serializable{
    /**
     * Devuelve el token de la solicitud.
     *
     * @return token de acceso.
     */
    public String getToken();

    /**
     * Devuelve el identificador de la empresa de la solicitud.
     *
     * @return identificador de la empresa.
     */
    public Long getIdcompany();

    /**
     * Asigna el identificador de la empresa de la solicitud.
     *
     * @param idcompany identificador de la empresa.
     */
    public void setIdcompany(Long idcompany);

    /**
     * Asigna la entidad token de la solicitud.
     *
     * @param appAuthToken entidad token del consumidor.
     */
    public void setAppAuthToken(IAppAuthConsumerToken appAuthToken);

    /**
     * Devuelve la fecha y hora de registro de la solicitud.
     *
     * @return fecha y hora de registro.
     */
    public LocalDateTime getLogDate();

    /**
     * Devuelve la cantidad de veces que se usó la solicitud cacheada.
     *
     * @return cantidad de usos.
     */
    public int getTimes();

    /**
     * Devuelve el valor de una propiedad de los datos del token.
     *
     * @param property nombre de la propiedad.
     * @return valor de la propiedad.
     */
    public String getPropertyValue(String property);
}
