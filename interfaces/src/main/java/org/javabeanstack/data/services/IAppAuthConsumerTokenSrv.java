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
package org.javabeanstack.data.services;

import org.javabeanstack.error.IErrorReg;

/**
 * Servicio de datos de los <b>tokens de acceso</b>
 * ({@code appauthconsumertoken}).
 *
 * <p>Es el <b>único punto</b> por el que se graba la tabla, venga la grabación
 * de donde venga: la solicitud de token, la creación desde un dispositivo, la
 * copia que trae el servidor principal o el mantenimiento por pantalla. Sus
 * validaciones ({@code @CheckMethod}) las ejecuta {@code checkDataRow} antes de
 * cada alta o modificación, de modo que ningún camino puede saltearlas.</p>
 *
 * <p>Lo que garantiza: un token siempre queda con <b>dueño</b> —las columnas
 * {@code usercode} e {@code idcompany}, y el mismo dato en {@code data}— y ese
 * dueño existe y tiene acceso concedido a la empresa.</p>
 *
 * <p>La <b>implementación la pone la aplicación</b> y se declara en su
 * {@code ejb-jar.xml}, igual que {@link IAppCompanySrv}: qué es un dueño
 * válido —qué cuenta está activa, quién tiene acceso a qué empresa— es una
 * decisión de cada producto, no del framework. {@code OAuthConsumerBase} la
 * toma por {@code @EJB} y no graba ningún token si no está declarada.</p>
 *
 * @author Jorge Enciso
 */
public interface IAppAuthConsumerTokenSrv extends IDataService {

    /**
     * Comprueba que el dueño con el que se quiere grabar un token —consumidor,
     * usuario y empresa— exista y pueda usarlo.
     *
     * <p>Es la misma comprobación que aplican las validaciones del servicio;
     * se expone para que quien va a pedir un token pueda anticipar el rechazo
     * y decir cuál de los tres datos está mal.</p>
     *
     * <p><b>Se dicen dos veces a propósito</b> —acá y en los
     * {@code @CheckMethod}— porque cumplen cosas distintas: esto nombra el
     * campo antes de intentar grabar, aquello impide la grabación. Toda regla
     * nueva del dueño hay que agregarla en los <b>dos</b> lugares, o el
     * anticipo dejará pasar lo que la grabación después rechaza.</p>
     *
     * @param consumerKey clave del consumidor.
     * @param userCode código del usuario dueño del token.
     * @param idcompany empresa del token; el {@code idcompany} de
     * {@code appcompany}, no el {@code idcompanymask}.
     * @return el error encontrado, o un error con número 0 si los tres datos
     * son correctos.
     */
    IErrorReg checkTokenOwner(String consumerKey, String userCode, Long idcompany);
}
