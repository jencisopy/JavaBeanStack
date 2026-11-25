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
package org.javabeanstack.sifen.internal.response;

/**
 * Un campo del XML (de un documento que se lee o de una respuesta de SIFEN) no se pudo convertir a
 * su tipo (plan JBSSIF, §3.8): lleva la ruta del nodo ({@code gDatGralOpe.gDatRec.iNatRec}) para el
 * mensaje {@code JBS-XML-05} / {@code JBS-RESP-06}. Uso interno: la fachada la traduce y nunca sale.
 */
public final class UnconvertibleFieldException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String ruta;

    /**
     * Crea la excepción.
     *
     * @param ruta  ruta del nodo.
     * @param causa la conversión que falló.
     */
    public UnconvertibleFieldException(String ruta, RuntimeException causa) {
        super(causa.getClass().getName(), causa);
        this.ruta = ruta;
    }

    /**
     * Devuelve la ruta del nodo, desde el grupo más externo que se leyó.
     *
     * @return la ruta.
     */
    public String getPath() {
        return ruta;
    }

    /**
     * Antepone el nombre del nodo padre a la ruta.
     *
     * @param padre nombre local del padre.
     * @return una excepción nueva con la ruta completa y la misma causa.
     */
    UnconvertibleFieldException conPadre(String padre) {
        return new UnconvertibleFieldException(padre + "." + ruta, (RuntimeException) getCause());
    }
}
