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
package org.javabeanstack.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declara la seguridad a nivel de fila de una entidad: restringe qué registros
 * son visibles/accesibles según una entidad de control y un parámetro de la
 * sesión.
 *
 * @author jenciso
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface EntityRowSecurity {
    /**
     * Campo identificador de la entidad sobre el que se aplica la restricción.
     *
     * @return nombre del campo identificador.
     */
    String idField();

    /**
     * Entidad de control que define los registros permitidos.
     *
     * @return nombre de la entidad de control.
     */
    String entity();

    /**
     * Parámetro (de la sesión/contexto) que acota los registros permitidos.
     *
     * @return nombre del parámetro.
     */
    String paramField();
}
