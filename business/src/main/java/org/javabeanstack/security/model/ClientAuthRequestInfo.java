/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2018 - 2027 Jorge Enciso
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

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.model.IAppAuthConsumerToken;
import static org.javabeanstack.util.Fn.nvl;
import org.javabeanstack.util.LocalDates;

/**
 *
 * Implementación de {@link IClientAuthRequestInfo}: cachea los datos de una
 * solicitud de autenticación por token (token, empresa, fecha de registro y
 * cantidad de usos) para evitar revalidar el token en cada request.
 *
 * @author Jorge Enciso
 */
public class ClientAuthRequestInfo implements IClientAuthRequestInfo{
    private static final Logger LOGGER = LogManager.getLogger(ClientAuthRequestInfo.class);    
    
    private IAppAuthConsumerToken appAuthToken;
    private Long idcompany;
    private LocalDateTime logDate;
    private int times;

    /**
     * Constructor por defecto; inicializa la fecha de registro con la fecha actual.
     */
    public ClientAuthRequestInfo(){
        logDate = LocalDates.now();
    }
    
    /**
     * Devuelve el token de la solicitud.
     *
     * @return token de acceso.
     */
    @Override
    public String getToken() {
        if (appAuthToken == null){
            return null;
        }
        times++;
        return appAuthToken.getToken();
    }

    /**
     * Devuelve el identificador de la empresa de la solicitud.
     *
     * @return identificador de la empresa.
     */
    @Override
    public Long getIdcompany() {
        return idcompany;
    }

    /**
     * Asigna el identificador de la empresa de la solicitud.
     *
     * @param idcompany identificador de la empresa.
     */
    @Override
    public void setIdcompany(Long idcompany) {
        this.idcompany = idcompany;
    }

    /**
     * Asigna la entidad token de la solicitud.
     *
     * @param appAuthToken entidad token del consumidor.
     */
    @Override
    public void setAppAuthToken(IAppAuthConsumerToken appAuthToken) {
        this.appAuthToken = appAuthToken;
    }

    /**
     * Devuelve la fecha y hora de registro de la solicitud.
     *
     * @return fecha y hora de registro.
     */
    @Override
    public LocalDateTime getLogDate() {
        return logDate;
    }

    /**
     * Devuelve la cantidad de veces que se usó la solicitud cacheada.
     *
     * @return cantidad de usos.
     */
    @Override
    public int getTimes() {
        return times;
    }

    /**
     * Devuelve el valor de una propiedad de los datos del token.
     *
     * @param property nombre de la propiedad.
     * @return valor de la propiedad.
     */
    @Override
    public String getPropertyValue(String property) {
        try {
            IAppAuthConsumerToken tokenRecord = appAuthToken;
            Properties prop = new Properties();
            prop.load(new StringReader(tokenRecord.getData()));
            return nvl((String) prop.getProperty(property), "");
        } catch (IOException ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return "";
    }    
}
