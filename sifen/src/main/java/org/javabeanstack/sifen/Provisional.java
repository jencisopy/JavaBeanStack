/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
*        jenciso@javabeanstack.org
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
package org.javabeanstack.sifen;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca lo <b>PROVISIONAL</b> del contrato de API v1: puede cambiar dentro de {@code 2.x}, con aviso en las
 * notas de la versión y en el contrato. La aplicación lo usa, pero aislado detrás de una clase propia.
 *
 * <p>Es la fuente única de qué es provisional: la foto de firmas del contrato separa lo anotado (o declarado
 * dentro de un tipo anotado) en {@code contract/api_v1_provisional.txt} y deja lo estable en
 * {@code contract/api_v1.txt}. Lo provisional se promueve a estable en una versión posterior, con evidencia de
 * uso: pierde la anotación, pasa a la foto estable y se avisa en las notas de la versión.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
public @interface Provisional {
}
