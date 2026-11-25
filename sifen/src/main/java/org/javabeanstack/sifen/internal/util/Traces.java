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
package org.javabeanstack.sifen.internal.util;

/**
 * Trazas para el log sin los mensajes de las excepciones (M3-04, D27): un mensaje puede traer datos
 * del documento o de la configuración, la clase y la pila no.
 */
public final class Traces {
    /** Profundidad máxima de la cadena de causas que se copia. */
    static final int MAXIMO_CAUSAS = 10;

    private Traces() {
    }

    /**
     * Copia la excepción y sus causas (hasta {@value #MAXIMO_CAUSAS}) conservando solo la clase y la
     * pila: ningún mensaje original llega al log.
     *
     * @param original excepción capturada.
     * @return la copia sin mensajes.
     */
    public static Throwable withoutMessage(Throwable original) {
        TrazaSinMensaje raiz = new TrazaSinMensaje(original);
        TrazaSinMensaje actual = raiz;
        Throwable causa = original.getCause();
        int profundidad = 0;
        while (causa != null && causa != original && profundidad < MAXIMO_CAUSAS) {
            TrazaSinMensaje siguiente = new TrazaSinMensaje(causa);
            actual.initCause(siguiente);
            actual = siguiente;
            causa = causa.getCause() == causa ? null : causa.getCause();
            profundidad++;
        }
        return raiz;
    }

    /**
     * Sustituto de una excepción en el log: su «mensaje» es el nombre de la clase original y su pila
     * la original; no copia el mensaje ni las suprimidas.
     */
    static final class TrazaSinMensaje extends RuntimeException {
        private static final long serialVersionUID = 1L;

        TrazaSinMensaje(Throwable original) {
            super(original.getClass().getName());
            setStackTrace(original.getStackTrace());
        }
    }
}
