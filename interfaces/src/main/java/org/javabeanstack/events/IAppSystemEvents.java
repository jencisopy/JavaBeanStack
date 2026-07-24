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
package org.javabeanstack.events;

import java.io.Serializable;

import org.apache.logging.log4j.Logger;
import org.javabeanstack.error.IErrorReg;

/**
 * Contrato de los eventos de sistema de la aplicación: puntos de extensión que
 * se disparan en el ciclo de vida de la sesión (creación, login/logout, carga
 * de página, acceso a empresa) y ante errores.
 *
 * @author Jorge Enciso
 */
public interface IAppSystemEvents extends Serializable{
    /**
     * Se ejecuta al crear una sesión.
     */
    void onCreateSession();

    /**
     * Se ejecuta al crear una sesión que finalizó con error.
     *
     * @param error registro del error de creación.
     */
    void onCreateSession(IErrorReg error);

    /**
     * Se ejecuta al iniciar sesión (login).
     */
    void onLogin();

    /**
     * Se ejecuta al cerrar sesión (logout).
     */
    void onLogout();

    /**
     * Se ejecuta al cargar una página.
     *
     * @param page identificador de la página cargada.
     */
    void onLoadPage(String page);

    /**
     * Se ejecuta al acceder a una empresa.
     */
    void onCompanyAccess();

    /**
     * Se ejecuta ante un error.
     *
     * @param e excepción ocurrida.
     * @param logger logger sobre el que registrar el error.
     */
    void onError(Exception e, Logger logger);
}
