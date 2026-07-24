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

import java.math.BigInteger;
import java.time.LocalDateTime;
import org.javabeanstack.data.IDataRow;

/**
 * Contrato de la entidad recurso de aplicación: un recurso (fuente y compilado)
 * con su tipo, url, checksum y datos de procesamiento, organizado jerárquicamente
 * ({@code idparent}). Extiende {@link IDataRow}.
 *
 * @author Jorge Enciso
 */
public interface IAppResource extends IDataRow {
    /**
     * Devuelve el identificador del recurso.
     * @return identificador del recurso.
     */
    Long getIdappresource();

    /**
     * Asigna el identificador del recurso.
     * @param idappresource identificador del recurso.
     */
    void setIdappresource(Long idappresource);

    /**
     * Devuelve el identificador del recurso padre.
     * @return identificador del recurso padre.
     */
    Long getIdparent();

    /**
     * Asigna el identificador del recurso padre.
     * @param idparent identificador del recurso padre.
     */
    void setIdparent(Long idparent);

    /**
     * Devuelve el identificador del objeto de aplicación asociado.
     * @return identificador del objeto.
     */
    Long getIdAppObject();

    /**
     * Asigna el identificador del objeto de aplicación asociado.
     * @param idobject identificador del objeto.
     */
    void setIdAppObject(Long idobject);

    /**
     * Devuelve el código del recurso.
     * @return código del recurso.
     */
    String getCode();

    /**
     * Asigna el código del recurso.
     * @param code código del recurso.
     */
    void setCode(String code);

    /**
     * Devuelve el nombre del recurso.
     * @return nombre del recurso.
     */
    String getName();

    /**
     * Asigna el nombre del recurso.
     * @param name nombre del recurso.
     */
    void setName(String name);

    /**
     * Devuelve la url del recurso.
     * @return url del recurso.
     */
    String getUrl();

    /**
     * Asigna la url del recurso.
     * @param url url del recurso.
     */
    void setUrl(String url);

    /**
     * Devuelve el tipo del recurso.
     * @return tipo del recurso.
     */
    String getType();

    /**
     * Asigna el tipo del recurso.
     * @param type tipo del recurso.
     */
    void setType(String type);

    /**
     * Devuelve el juego de caracteres del recurso.
     * @return juego de caracteres.
     */
    String getCharset();

    /**
     * Asigna el juego de caracteres del recurso.
     * @param charset juego de caracteres.
     */
    void setCharset(String charset);

    /**
     * Devuelve el contenido fuente del recurso.
     * @return contenido fuente.
     */
    String getSource();

    /**
     * Asigna el contenido fuente del recurso.
     * @param source contenido fuente.
     */
    void setSource(String source);

    /**
     * Devuelve el contenido compilado del recurso.
     * @return contenido compilado.
     */
    String getCompiled();

    /**
     * Asigna el contenido compilado del recurso.
     * @param compiled contenido compilado.
     */
    void setCompiled(String compiled);

    /**
     * Devuelve la fecha y hora de procesamiento del recurso.
     * @return fecha de procesamiento.
     */
    LocalDateTime getProcesstime();

    /**
     * Asigna la fecha y hora de procesamiento del recurso.
     * @param processtime fecha de procesamiento.
     */
    void setProcesstime(LocalDateTime processtime);

    /**
     * Devuelve la fecha y hora de la última referencia al recurso.
     * @return fecha de la última referencia.
     */
    LocalDateTime getLastreference();

    /**
     * Asigna la fecha y hora de la última referencia al recurso.
     * @param lastreference fecha de la última referencia.
     */
    void setLastreference(LocalDateTime lastreference);

    /**
     * Devuelve el contador de referencias al recurso.
     * @return cantidad de referencias.
     */
    BigInteger getReferencetime();

    /**
     * Asigna el contador de referencias al recurso.
     * @param referencetime cantidad de referencias.
     */
    void setReferencetime(BigInteger referencetime);

    /**
     * Devuelve el checksum del recurso.
     * @return checksum del recurso.
     */
    String getChecksum();

    /**
     * Asigna el checksum del recurso.
     * @param checkSum checksum del recurso.
     */
    void setChecksum(String checkSum);

    /**
     * Devuelve el usuario de aplicación asociado al recurso.
     * @return usuario de aplicación.
     */
    String getAppuser();

    /**
     * Asigna el usuario de aplicación asociado al recurso.
     * @param appuser usuario de aplicación.
     */
    void setAppuser(String appuser);

    /**
     * Indica si el recurso es válido.
     * @return verdadero si es válido, falso si no.
     */
    boolean isValid();
}
