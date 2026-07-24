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

import org.javabeanstack.data.IDataRow;
import org.w3c.dom.Document;


/**
 * Contrato de la entidad autorización de objeto: define, para un usuario y un
 * objeto de aplicación, el nivel de permiso por acción (lectura, escritura,
 * inserción, borrado, etc.). La usa el servicio de autorización
 * {@link org.javabeanstack.security.IAppObjectAuthSrv}. Extiende {@link IDataRow}.
 *
 * @author Jorge Enciso
 */
public interface IAppObjectAuth extends IDataRow{
    /** Nivel de autorización: denegado. */
    public static int DENIED = 1;
    /** Nivel de autorización: permitido. */
    public static int ALLOWED = 0;

    /**
     * Devuelve el identificador de la autorización.
     * @return identificador de la autorización.
     */
    Long getIdAppObjectAuth();

    /**
     * Asigna el identificador de la autorización.
     * @param IdAppObjectAuth identificador de la autorización.
     */
    void setIdAppObjectAuth(Long IdAppObjectAuth);

    /**
     * Devuelve el identificador del usuario.
     * @return identificador del usuario.
     */
    Long getIduser();

    /**
     * Asigna el identificador del usuario.
     * @param iduser identificador del usuario.
     */
    void setIduser(Long iduser);

    /**
     * Devuelve el identificador del objeto de aplicación.
     * @return identificador del objeto.
     */
    Long getIdAppObject();

    /**
     * Asigna el identificador del objeto de aplicación.
     * @param idAppObject identificador del objeto.
     */
    void setIdAppObject(Long idAppObject);

    /**
     * Devuelve la definición de autorización (texto).
     * @return definición de autorización.
     */
    String getAuth();

    /**
     * Asigna la definición de autorización (texto).
     * @param auth definición de autorización.
     */
    void setAuth(String auth);

    /**
     * Devuelve el permiso de lectura ({@link #ALLOWED}/{@link #DENIED}).
     * @return permiso de lectura.
     */
    Integer getRead();

    /**
     * Asigna el permiso de lectura.
     * @param read permiso de lectura.
     */
    void setRead(Integer read);

    /**
     * Devuelve el permiso de escritura.
     * @return permiso de escritura.
     */
    Integer getWrite();

    /**
     * Asigna el permiso de escritura.
     * @param write permiso de escritura.
     */
    void setWrite(Integer write);

    /**
     * Devuelve el permiso de ejecución.
     * @return permiso de ejecución.
     */
    Integer getExecute();

    /**
     * Asigna el permiso de ejecución.
     * @param execute permiso de ejecución.
     */
    void setExecute(Integer execute);

    /**
     * Devuelve el permiso de inserción.
     * @return permiso de inserción.
     */
    Integer getInsert();

    /**
     * Asigna el permiso de inserción.
     * @param insert permiso de inserción.
     */
    void setInsert(Integer insert);

    /**
     * Devuelve el permiso de borrado.
     * @return permiso de borrado.
     */
    Integer getDelete();

    /**
     * Asigna el permiso de borrado.
     * @param delete permiso de borrado.
     */
    void setDelete(Integer delete);

    /**
     * Devuelve el permiso de actualización.
     * @return permiso de actualización.
     */
    Integer getUpdate();

    /**
     * Asigna el permiso de actualización.
     * @param update permiso de actualización.
     */
    void setUpdate(Integer update);

    /**
     * Devuelve el permiso de confirmación.
     * @return permiso de confirmación.
     */
    Integer getConfirm();

    /**
     * Asigna el permiso de confirmación.
     * @param confirm permiso de confirmación.
     */
    void setConfirm(Integer confirm);

    /**
     * Devuelve el permiso de cancelación.
     * @return permiso de cancelación.
     */
    Integer getCancel();

    /**
     * Asigna el permiso de cancelación.
     * @param cancel permiso de cancelación.
     */
    void setCancel(Integer cancel);

    /**
     * Devuelve el permiso de adjuntar.
     * @return permiso de adjuntar.
     */
    Integer getAttach();

    /**
     * Asigna el permiso de adjuntar.
     * @param attach permiso de adjuntar.
     */
    void setAttach(Integer attach);

    /**
     * Devuelve el permiso de copiar desde.
     * @return permiso de copiar desde.
     */
    Integer getCopyFrom();

    /**
     * Asigna el permiso de copiar desde.
     * @param copyFrom permiso de copiar desde.
     */
    void setCopyFrom(Integer copyFrom);

    /**
     * Devuelve la definición de autorización como documento XML.
     * @return documento XML de autorización.
     */
    Document getAuthXmlDom();

    /**
     * Asigna la definición de autorización como documento XML.
     * @param xmlDom documento XML de autorización.
     */
    void setAuthXmlDom(Document xmlDom);
}
