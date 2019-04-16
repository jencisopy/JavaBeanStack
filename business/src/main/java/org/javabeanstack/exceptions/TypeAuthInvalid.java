/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2018 Jorge Enciso
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
package org.javabeanstack.exceptions;

/**
 *
 * @author Jorge Enciso
 */
public class TypeAuthInvalid extends Exception {

    /**
     * Creates a new instance of <code>TypeAuthInvalid</code> without detail
     * message.
     */
    public TypeAuthInvalid() {
    }

    /**
     * Constructs an instance of <code>TypeAuthInvalid</code> with the specified
     * detail message.
     *
     * @param msg the detail message.
     */
    public TypeAuthInvalid(String msg) {
        super(msg);
    }
}
