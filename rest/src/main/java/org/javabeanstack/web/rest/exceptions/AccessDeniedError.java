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
package org.javabeanstack.web.rest.exceptions;

/**
 * La sesión es válida pero no alcanza para la operación pedida (403): el rol
 * no corresponde, el alcance es ambiguo, falta el encabezado CSRF, etc. Lleva
 * un código corto para que el cliente lo distinga sin leer el mensaje.
 *
 * <p>Se diferencia de {@link TokenError} (401), que significa «no sé quién
 * sos».</p>
 */
public class AccessDeniedError extends RuntimeException {

    /** Alcance ambiguo: la persona tiene más de una cuenta o rol aplicable. */
    public static final String SCOPE_AMBIGUOUS = "SCOPE_AMBIGUOUS";
    /** El rol pedido no le corresponde a la persona en esta empresa. */
    public static final String ROLE_MISMATCH = "ROL_NO_CORRESPONDE";
    /** Petición mutadora por cookie sin el encabezado CSRF. */
    public static final String CSRF_REQUIRED = "CSRF_REQUIRED";
    /** La operación exige ser administrador del sistema. */
    public static final String ADMIN_REQUIRED = "ADMIN_REQUIRED";

    private final String code;

    public AccessDeniedError(String code, String message) {
        super(message);
        this.code = code;
    }

    public AccessDeniedError(String message) {
        this(null, message);
    }

    /** @return código corto del rechazo, o nulo. */
    public String getCode() {
        return code;
    }
}
