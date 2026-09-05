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
/**
 * Autenticación de los recursos JAX-RS por filtro: un mismo recurso acepta un
 * token de acceso ({@code Authorization: Bearer <token>}) o una sesión de login
 * (el mismo encabezado con el identificador de sesión, o la cookie
 * {@code JbsSessionId}), sin que el código del recurso sepa cuál fue.
 *
 * <p>Piezas: {@link org.javabeanstack.web.rest.security.SessionRequired} marca
 * la clase o el método que exige sesión;
 * {@link org.javabeanstack.web.rest.security.SessionRequiredFilter} corre antes
 * del método, resuelve la sesión con
 * {@link org.javabeanstack.web.rest.security.SessionResolver} y la deja como
 * atributo del {@code HttpServletRequest}, o corta con 401/403;
 * {@link org.javabeanstack.web.rest.security.CsrfRules} reúne las reglas CSRF
 * que antes vivían en {@code SessionWebResource}.</p>
 */
package org.javabeanstack.web.rest.security;
