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
 * Define un filtro dinámico asociado a un método (getter) de una entidad, usado
 * por la capa de datos para construir la condición de selección según el valor
 * del campo.
 *
 * @author Jorge Enciso
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface FieldFilter {
    /**
     * Nombre del campo sobre el que se aplica el filtro.
     *
     * @return nombre del campo.
     */
    String field() default "";
    /**
     * Expresión del filtro.
     *
     * @return expresión del filtro.
     */
    String expression() default "";
    /**
     * Modo de comparación cuando el valor es texto y el operador es {@code like}.
     *
     * @return modo de comparación ({@code equal}, {@code contain},
     * {@code contain_trim}, {@code contain_ltrim}, {@code contain_rtrim}).
     */
    String mode() default ""; //Valores equal, contain, contain_trim, contain_ltrim, contain_rtrim
    /**
     * Expresión a aplicar cuando el valor del campo es nulo o vacío.
     *
     * @return expresión para valor nulo o vacío.
     */
    String nullOrEmptyExpression() default "";
    /**
     * Orden de procesamiento del filtro.
     *
     * @return orden de procesamiento.
     */
    String order() default "";

    /**
     * Expresión a aplicar cuando el filtro no debe activarse.
     *
     * @return expresión "falsa" del filtro.
     */
    String falseExpression() default "false";

    /**
     * Indica si el valor cero se incluye en el filtro.
     *
     * @return {@code "true"} para incluir el cero, {@code "false"} para excluirlo.
     */
    String ceroInclude() default "false";
}
