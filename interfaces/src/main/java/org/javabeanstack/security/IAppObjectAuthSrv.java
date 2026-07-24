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

import java.util.Map;
import org.javabeanstack.data.IDataResult;
import org.javabeanstack.data.services.IDataService;
import org.javabeanstack.model.IAppObjectAuth;
import org.javabeanstack.model.IAppUser;
import org.w3c.dom.Document;

/**
 * Contrato del servicio de autorización a nivel de objeto: resuelve, para un
 * usuario y una acción, el nivel de acceso a un objeto de aplicación
 * ({@link IAppObjectAuth}) y a sus campos.
 *
 * <p>Extiende el servicio de datos {@link IDataService} y expone además la
 * definición de autorización como documento XML.</p>
 *
 * @author Jorge Enciso
 */
public interface IAppObjectAuthSrv extends IDataService {
    /**
     * Determina el nivel de autorización de un usuario sobre un objeto para una
     * acción, considerando únicamente las reglas del usuario.
     *
     * @param sessionId identificador de la sesión del usuario.
     * @param idAppObject identificador del objeto de aplicación.
     * @param user usuario a evaluar.
     * @param action acción a evaluar.
     * @param authDenyDefault valor por defecto cuando no hay regla explícita.
     * @return nivel de autorización resultante.
     */
    Integer checkAuthUserOnly(String sessionId, Long idAppObject, IAppUser user, String action, Integer authDenyDefault);

    /**
     * Determina el nivel de autorización de un usuario (por id) sobre un objeto
     * para una acción.
     *
     * @param sessionId identificador de la sesión del usuario.
     * @param idAppObject identificador del objeto de aplicación.
     * @param iduser identificador del usuario.
     * @param action acción a evaluar.
     * @param checkResult mapa donde se acumula el detalle de la evaluación.
     * @param authDenyDefault valor por defecto cuando no hay regla explícita.
     * @return nivel de autorización resultante.
     */
    Integer checkAuth(String sessionId, Long idAppObject, Long iduser, String action, Map<String, String> checkResult, Integer authDenyDefault);

    /**
     * Determina el nivel de autorización de un usuario sobre un objeto para una
     * acción.
     *
     * @param sessionId identificador de la sesión del usuario.
     * @param idAppObject identificador del objeto de aplicación.
     * @param user usuario a evaluar.
     * @param action acción a evaluar.
     * @param checkResult mapa donde se acumula el detalle de la evaluación.
     * @param authDenyDefault valor por defecto cuando no hay regla explícita.
     * @return nivel de autorización resultante.
     */
    Integer checkAuth(String sessionId, Long idAppObject, IAppUser user, String action, Map<String, String> checkResult, Integer authDenyDefault);

    /**
     * Determina el nivel de autorización de un usuario (por id) sobre un campo
     * de un objeto para una acción.
     *
     * @param sessionId identificador de la sesión del usuario.
     * @param idAppObject identificador del objeto de aplicación.
     * @param iduser identificador del usuario.
     * @param field campo a evaluar.
     * @param action acción a evaluar.
     * @return nivel de autorización resultante.
     */
    Integer checkAuthField(String sessionId, Long idAppObject, Long iduser, String field, String action);

    /**
     * Determina el nivel de autorización de un usuario sobre un campo de un
     * objeto para una acción.
     *
     * @param sessionId identificador de la sesión del usuario.
     * @param idAppObject identificador del objeto de aplicación.
     * @param user usuario a evaluar.
     * @param field campo a evaluar.
     * @param action acción a evaluar.
     * @return nivel de autorización resultante.
     */
    Integer checkAuthField(String sessionId, Long idAppObject, IAppUser user, String field, String action);

    /**
     * Devuelve la definición de autorización de un objeto como documento XML.
     *
     * @param idAppObject identificador del objeto de aplicación.
     * @return documento XML con la definición de autorización.
     */
    Document getAuthXmlDom(Long idAppObject);

    /**
     * Devuelve la definición de autorización de un objeto para un usuario como
     * documento XML.
     *
     * @param idAppObject identificador del objeto de aplicación.
     * @param iduser identificador del usuario.
     * @return documento XML con la definición de autorización.
     */
    Document getAuthXmlDom(Long idAppObject, Long iduser);

    /**
     * Devuelve la entidad de autorización de un objeto para un usuario.
     *
     * @param idAppObject identificador del objeto de aplicación.
     * @param iduser identificador del usuario.
     * @return entidad de autorización.
     */
    IAppObjectAuth getAppObjectAuth(Long idAppObject, Long iduser);

    /**
     * Persiste la entidad de autorización de un objeto.
     *
     * @param sessionId identificador de la sesión del usuario.
     * @param ejb entidad de autorización a guardar.
     * @return resultado de la operación.
     * @throws Exception si la persistencia falla.
     */
    IDataResult saveAppObjectAuth(String sessionId, IAppObjectAuth ejb) throws Exception;
}
