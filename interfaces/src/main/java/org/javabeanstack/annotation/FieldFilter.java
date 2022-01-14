/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 Jorge Enciso
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
 *
 * @author Jorge Enciso
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface FieldFilter {
    /**
     * nombre del campo
     * @return
     */
    String field() default "";
    /**
     * Expresion del filtro
     * @return
     */
    String expression() default "";
    /**
     * Parametro adicional en caso de ser string y el operador like
     * @return
     */
    String mode() default ""; //Valores equal, contain, contain_trim, contain_ltrim, contain_rtrim
    /**
     * Expresión si el valor es nulo
     * @return
     */
    String nullOrEmptyExpression() default "";
    /**
     * Orden de procesamiento
     * @return
     */
    String order() default "";
    
    String falseExpression() default "";
}
