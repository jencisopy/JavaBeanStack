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

package org.javabeanstack.error;

import java.io.Serializable;

/**
 * Contrato de la representación unificada de un error (o advertencia) entre las
 * capas del framework: reúne el mensaje, el número de error, la entidad y campo
 * afectados, la excepción de origen y datos de contexto (evento, ip, nivel).
 *
 * <p>La implementación de referencia es {@code org.javabeanstack.error.ErrorReg}.</p>
 *
 * @author Jorge Enciso
 */
public interface IErrorReg extends Serializable {
    /**
     * Devuelve la entidad afectada por el error.
     *
     * @return nombre de la entidad.
     */
    public String getEntity();

    /**
     * Devuelve el campo afectado por el error.
     *
     * @return nombre del campo.
     */
    public String getFieldName();

    /**
     * Devuelve los campos afectados por el error (cuando involucra varios).
     *
     * @return nombres de los campos.
     */
    public String[] getFieldNames();

    /**
     * Devuelve el mensaje de error.
     *
     * @return mensaje de error.
     */
    public String getMessage();

    /**
     * Devuelve el número de error del catálogo de mensajes.
     *
     * @return número de error.
     */
    public Integer getErrorNumber();

    /**
     * Devuelve la excepción de origen del error, si la hubo.
     *
     * @return excepción, o {@code null} si no hubo.
     */
    public Exception getException();

    /**
     * Devuelve la ip de la solicitud donde ocurrió el error.
     *
     * @return dirección ip.
     */
    public String getIpRequest();

    /**
     * Devuelve el evento en el que ocurrió el error.
     *
     * @return nombre del evento.
     */
    public String getEvent();

    /**
     * Devuelve el nivel del error (p. ej. entidad, fila, campo).
     *
     * @return nivel del error.
     */
    public String getLevel();

    /**
     * Devuelve información de contexto adicional asociada al error.
     *
     * @return información adicional.
     */
    public Object getInfo();

    /**
     * Indica si el registro es una advertencia (no un error).
     *
     * @return verdadero si es advertencia, falso si es error.
     */
    public boolean isWarning();

    /**
     * Asigna la entidad afectada por el error.
     *
     * @param entity nombre de la entidad.
     */
    public void setEntity(String entity);

    /**
     * Asigna el campo afectado por el error.
     *
     * @param fieldName nombre del campo.
     */
    public void setFieldName(String fieldName);

    /**
     * Asigna los campos afectados por el error.
     *
     * @param fieldNames nombres de los campos.
     */
    public void setFieldNames(String[] fieldNames);

    /**
     * Asigna el mensaje de error.
     *
     * @param message mensaje de error.
     */
    public void setMessage(String message);

    /**
     * Asigna el número de error del catálogo de mensajes.
     *
     * @param errorNumber número de error.
     */
    public void setErrorNumber(int errorNumber);

    /**
     * Asigna la excepción de origen del error.
     *
     * @param exp excepción.
     */
    public void setException(Exception exp);

    /**
     * Asigna la condición de advertencia del registro.
     *
     * @param warning verdadero si es advertencia.
     */
    public void setWarning(boolean warning);

    /**
     * Asigna la ip de la solicitud donde ocurrió el error.
     *
     * @param ip dirección ip.
     */
    public void setIpRequest(String ip);

    /**
     * Asigna el evento en el que ocurrió el error.
     *
     * @param event nombre del evento.
     */
    public void setEvent(String event);

    /**
     * Asigna el nivel del error.
     *
     * @param level nivel del error.
     */
    public void setLevel(String level);

    /**
     * Asigna información de contexto adicional del error.
     *
     * @param info información adicional.
     */
    public void setInfo(Object info);
}
