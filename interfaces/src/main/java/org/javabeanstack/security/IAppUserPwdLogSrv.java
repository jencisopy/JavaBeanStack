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
package org.javabeanstack.security;

import java.time.LocalDateTime;
import org.javabeanstack.data.services.IDataService;
import org.javabeanstack.model.IAppUser;

/**
 * Contrato del servicio de bitácora de contraseñas: registra el historial de
 * contraseñas de un usuario y permite consultarlo (para impedir la reutilización
 * de contraseñas anteriores). Extiende el servicio de datos {@link IDataService}.
 *
 * @author Jorge Enciso
 */
public interface IAppUserPwdLogSrv extends IDataService {
    /**
     * Indica si existe bitácora de contraseñas para el usuario de la sesión.
     *
     * @param sessionId identificador de la sesión del usuario.
     * @return verdadero si existe bitácora, falso si no.
     */
    boolean isExistUserPwdLog(String sessionId);

    /**
     * Indica si existe bitácora de contraseñas para el usuario indicado.
     *
     * @param appUser usuario a consultar.
     * @return verdadero si existe bitácora, falso si no.
     */
    boolean isExistUserPwdLog(IAppUser appUser);

    /**
     * Registra la contraseña actual del usuario de la sesión en la bitácora.
     *
     * @param sessionId identificador de la sesión del usuario.
     */
    void insertUserPwdLog(String sessionId);

    /**
     * Registra la contraseña actual del usuario indicado en la bitácora.
     *
     * @param appUser usuario cuya contraseña se registra.
     */
    void insertUserPwdLog(IAppUser appUser);

    /**
     * Devuelve el identificador del usuario cuya bitácora contiene la contraseña
     * indicada.
     *
     * @param pwd contraseña a buscar.
     * @return identificador del usuario, o {@code null} si no se encuentra.
     */
    Long getIdUserFromPwdLog(String pwd);

    /**
     * Reemplaza la contraseña de un usuario dejando el rastro que exige la
     * auditoría.
     *
     * <p>El valor de {@code appuser.pass} no es solo una credencial: es el sello
     * con el que quedan marcadas todas las filas que ese usuario graba. Por eso
     * el hash que se retira se registra en la bitácora <b>antes</b> de
     * reescribirlo, y el nuevo después de grabarlo: sin ese registro, las filas
     * selladas con el hash viejo quedarían sin usuario resoluble de forma
     * irreversible.</p>
     *
     * <p>Si la grabación falla, se deshace el cambio en memoria para que el
     * objeto siga reflejando lo que realmente está en la base.</p>
     *
     * @param appUser usuario cuya contraseña se reemplaza.
     * @param hashNuevo contraseña ya cifrada con la fórmula que corresponda.
     * @return verdadero si la contraseña quedó grabada.
     */
    boolean replaceUserPass(IAppUser appUser, String hashNuevo);

    /**
     * Devuelve la fecha del último cambio de la contraseña <b>vigente</b> del
     * usuario.
     *
     * <p>Es la fecha de alta, en la bitácora, de la fila cuyo hash es el que el
     * usuario tiene hoy, y no la de la última fila del usuario: la bitácora
     * guarda también el hash que se retira en cada cambio, y una fila posterior
     * con otro hash no dice nada sobre la contraseña en uso.</p>
     *
     * <p>Por omisión devuelve nulo, que la política de vencimiento interpreta
     * como «no se puede evaluar»: así un implementador que no lo redefina no
     * vence a nadie.</p>
     *
     * @param appUser usuario a consultar.
     * @return fecha y hora del último cambio de la contraseña vigente, o nulo
     * si no hay registro o no se pudo consultar.
     */
    default LocalDateTime getLastPasswordChange(IAppUser appUser) {
        return null;
    }
}
