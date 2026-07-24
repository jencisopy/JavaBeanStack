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
 * Contrato de la entidad parámetro de sistema: un valor de configuración
 * tipado (booleano, texto, fecha o número) identificado por su nombre y grupo.
 * Extiende {@link IDataRow}.
 *
 * @author Jorge Enciso
 */
public interface IAppSystemParam extends IDataRow {
    /**
     * Devuelve el identificador del parámetro.
     * @return identificador del parámetro.
     */
    Long getIdAppSystemParam();

    /**
     * Devuelve el nombre del parámetro.
     * @return nombre del parámetro.
     */
    String getParam();

    /**
     * Devuelve la descripción del parámetro.
     * @return descripción del parámetro.
     */
    String getParamDescrip();

    /**
     * Devuelve el tipo del parámetro (booleano, carácter, fecha, número).
     * @return tipo del parámetro.
     */
    Character getParamType();

    /**
     * Devuelve el grupo de sistema al que pertenece el parámetro.
     * @return grupo de sistema.
     */
    String getSystemgroup();

    /**
     * Devuelve el valor booleano del parámetro.
     * @return valor booleano.
     */
    Boolean getValueBoolean();

    /**
     * Devuelve el valor de texto del parámetro.
     * @return valor de texto.
     */
    String getValueChar();

    /**
     * Devuelve el valor de fecha del parámetro.
     * @return valor de fecha.
     */
    LocalDateTime getValueDate();

    /**
     * Devuelve el valor numérico del parámetro.
     * @return valor numérico.
     */
    Long getValueNumber();

    /**
     * Devuelve el valor del parámetro según su tipo.
     * @return valor del parámetro.
     */
    Object getValue();

    /**
     * Asigna el identificador del parámetro.
     * @param idsystemparam identificador del parámetro.
     */
    void setIdAppSystemParam(Long idsystemparam);

    /**
     * Asigna el nombre del parámetro.
     * @param param nombre del parámetro.
     */
    void setParam(String param);

    /**
     * Asigna la descripción del parámetro.
     * @param paramDescrip descripción del parámetro.
     */
    void setParamDescrip(String paramDescrip);

    /**
     * Asigna el tipo del parámetro.
     * @param paramType tipo del parámetro.
     */
    void setParamType(Character paramType);

    /**
     * Asigna el grupo de sistema del parámetro.
     * @param systemgroup grupo de sistema.
     */
    void setSystemgroup(String systemgroup);

    /**
     * Asigna el valor booleano del parámetro.
     * @param valueBoolean valor booleano.
     */
    void setValueBoolean(Boolean valueBoolean);

    /**
     * Asigna el valor de texto del parámetro.
     * @param valueChar valor de texto.
     */
    void setValueChar(String valueChar);

    /**
     * Asigna el valor de fecha del parámetro.
     * @param valueDate valor de fecha.
     */
    void setValueDate(LocalDateTime valueDate);

    /**
     * Asigna el valor numérico del parámetro.
     * @param valueNumber valor numérico.
     */
    void setValueNumber(Long valueNumber);

    /**
     * Asigna el valor del parámetro según su tipo.
     * @param value valor del parámetro.
     * @throws Exception si el valor no corresponde al tipo del parámetro.
     */
    void setValue(Object value) throws Exception;
}
