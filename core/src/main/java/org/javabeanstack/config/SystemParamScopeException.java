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
package org.javabeanstack.config;

import jakarta.ejb.ApplicationException;
import org.javabeanstack.error.IErrorReg;

/**
 * Se intentó grabar un valor por empresa de un parámetro del sistema que no lo
 * admite: su fila global es de alcance solo global, o no existe.
 *
 * <p>Es una validación de negocio y no una falla del sistema: por eso es una
 * excepción <b>checked</b> y {@code @ApplicationException(rollback = false)}.
 * El contenedor la entrega tal cual al llamador —sin envolverla en
 * {@code EJBException} ni registrarla como error del sistema— y no deshace lo
 * que el llamador estuviera haciendo. Lleva el {@link IErrorReg} (número
 * 50000, campo {@code idcompany}) para que la capa que la reciba lo muestre
 * como cualquier otro error de validación.</p>
 *
 * @author Jorge Enciso
 */
@ApplicationException(rollback = false)
public class SystemParamScopeException extends Exception {

    private static final long serialVersionUID = 1L;

    private final IErrorReg errorReg;

    /**
     * Crea la excepción con el error de validación; su mensaje es el de la
     * excepción.
     *
     * @param errorReg error de validación (número, campo y mensaje).
     */
    public SystemParamScopeException(IErrorReg errorReg) {
        super(errorReg.getMessage());
        this.errorReg = errorReg;
    }

    /**
     * Devuelve el error de validación que originó el rechazo.
     *
     * @return el error de validación (número 50000, campo {@code idcompany}).
     */
    public IErrorReg getErrorReg() {
        return errorReg;
    }
}
