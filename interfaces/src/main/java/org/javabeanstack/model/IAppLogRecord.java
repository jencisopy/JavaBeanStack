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
package org.javabeanstack.model;

import java.time.LocalDateTime;
import org.javabeanstack.data.IDataRow;

/**
 * Contrato de la entidad registro de log: un evento del sistema (sesión, login,
 * error, acceso a empresa...) con su nivel, categoría, mensaje y datos de
 * contexto. La persiste el {@link org.javabeanstack.log.ILogManager}. Extiende
 * {@link IDataRow}.
 *
 * @author Jorge Enciso
 */
public interface IAppLogRecord extends IDataRow{
    /** Evento: creación de sesión. */
    public static final String EVENT_CREATESESSION = "CREATESESSION";
    /** Evento: inicio de sesión. */
    public static final String EVENT_LOGIN = "LOGIN";
    /** Evento: cierre de sesión. */
    public static final String EVENT_LOGOUT = "LOGOUT";
    /** Evento: error. */
    public static final String EVENT_ERROR = "ERROR";
    /** Evento: acceso a empresa. */
    public static final String EVENT_COMPANY_IN = "COMPANYIN";
    /** Evento: carga de página. */
    public static final String EVENT_LOAD_PAGE = "LOADPAGE";
    /** Evento: actualización de base de datos. */
    public static final String EVENT_UPDATEDB = "UPDATEDB";
    /** Evento: salida de un documento generado (descarga, correo, carpeta, impresión). */
    public static final String EVENT_DOCUMENT_OUTPUT = "DOCUMENTOUTPUT";

    /** Nivel: error. */
    public static final String LEVEL_ERROR = "E";
    /** Nivel: alerta. */
    public static final String LEVEL_ALERT = "A";
    /** Nivel: informativo. */
    public static final String LEVEL_INFO = "I";

    /** Categoría: aplicación. */
    public static final String CATEGORY_APP = "A";
    /** Categoría: seguridad. */
    public static final String CATEGORY_SECURITY = "S";
    /** Categoría: datos. */
    public static final String CATEGORY_DATA = "D";

    /**
     * Devuelve el identificador del registro de log.
     * @return identificador del registro.
     */
    Long getIdlog();

    /**
     * Devuelve el identificador de la sesión.
     * @return identificador de la sesión.
     */
    String getSessionId();

    /**
     * Devuelve el identificador de la empresa.
     * @return identificador de la empresa.
     */
    Long getIdcompany();

    /**
     * Devuelve el identificador del usuario.
     * @return identificador del usuario.
     */
    Long getIduser();

    /**
     * Devuelve el nivel del evento ({@link #LEVEL_ERROR}, {@link #LEVEL_ALERT},
     * {@link #LEVEL_INFO}).
     * @return nivel del evento.
     */
    String getLevel();

    /**
     * Devuelve la categoría del evento ({@link #CATEGORY_APP},
     * {@link #CATEGORY_SECURITY}, {@link #CATEGORY_DATA}).
     * @return categoría del evento.
     */
    String getCategory();

    /**
     * Devuelve la ip de origen de la solicitud.
     * @return ip de origen.
     */
    String getIpRequestFrom();

    /**
     * Devuelve el evento registrado.
     * @return nombre del evento.
     */
    String getEvent();

    /**
     * Devuelve la fecha y hora de registro del log.
     * @return fecha y hora de registro.
     */
    LocalDateTime getLogTime();

    /**
     * Devuelve la fecha y hora de origen del evento.
     * @return fecha y hora de origen.
     */
    LocalDateTime getLogTimeOrigin();

    /**
     * Devuelve el mensaje del evento.
     * @return mensaje del evento.
     */
    String getMessage();

    /**
     * Devuelve información adicional del mensaje.
     * @return información adicional.
     */
    String getMessageInfo();

    /**
     * Devuelve el número de mensaje del catálogo.
     * @return número de mensaje.
     */
    Integer getMessageNumber();

    /**
     * Devuelve el objeto de aplicación asociado al evento.
     * @return objeto de aplicación.
     */
    String getAppObject();

    /**
     * Devuelve la página web asociada al evento.
     * @return página web.
     */
    String getWebPage();

    /**
     * Asigna el identificador de la sesión.
     * @param sessionId identificador de la sesión.
     */
    void setSessionId(String sessionId);

    /**
     * Asigna el identificador del registro de log.
     * @param idlog identificador del registro.
     */
    void setIdlog(Long idlog);

    /**
     * Asigna el identificador de la empresa.
     * @param idcompany identificador de la empresa.
     */
    void setIdcompany(Long idcompany);

    /**
     * Asigna el identificador del usuario.
     * @param iduser identificador del usuario.
     */
    void setIduser(Long iduser);

    /**
     * Asigna el evento registrado.
     * @param event nombre del evento.
     */
    void setEvent(String event);

    /**
     * Asigna el nivel del evento.
     * @param level nivel del evento.
     */
    void setLevel(String level);

    /**
     * Asigna la categoría del evento.
     * @param category categoría del evento.
     */
    void setCategory(String category);

    /**
     * Asigna la fecha y hora de registro del log.
     * @param dateTime fecha y hora de registro.
     */
    void setLogTime(LocalDateTime dateTime);

    /**
     * Asigna la fecha y hora de origen del evento.
     * @param dateTimeOrigin fecha y hora de origen.
     */
    void setLogTimeOrigin(LocalDateTime dateTimeOrigin);

    /**
     * Asigna la ip de origen de la solicitud.
     * @param origin ip de origen.
     */
    void setIpRequestFrom(String origin);

    /**
     * Asigna el mensaje del evento.
     * @param message mensaje del evento.
     */
    void setMessage(String message);

    /**
     * Asigna información adicional del mensaje.
     * @param messageInfo información adicional.
     */
    void setMessageInfo(String messageInfo);

    /**
     * Asigna el número de mensaje del catálogo.
     * @param messageNumber número de mensaje.
     */
    void setMessageNumber(Integer messageNumber);

    /**
     * Asigna el objeto de aplicación asociado al evento.
     * @param object objeto de aplicación.
     */
    void setAppObject(String object);

    /**
     * Asigna la página web asociada al evento.
     * @param object página web.
     */
    void setWebPage(String object);
}
