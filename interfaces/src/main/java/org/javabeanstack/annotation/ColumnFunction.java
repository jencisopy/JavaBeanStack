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
 * Define un campo calculado a partir de una fórmula (y, opcionalmente, de un
 * campo de otra entidad relacionada) en lugar de mapearse directamente a una
 * columna.
 *
 * @author jenciso
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface ColumnFunction {
    /**
     * Fórmula que calcula el valor del campo.
     *
     * @return fórmula del campo calculado.
     */
    String formula();

    /**
     * Clase de la entidad relacionada de la que se toma el valor, si corresponde.
     *
     * @return nombre de la clase mapeada.
     */
    String classMapped() default "";

    /**
     * Campo de la entidad relacionada del que se toma el valor, si corresponde.
     *
     * @return nombre del campo mapeado.
     */
    String fieldMapped() default "";
}
