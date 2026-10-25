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
package org.javabeanstack.xml;

import org.javabeanstack.data.IDataRow;
import org.w3c.dom.Element;

/**
 * Convierte el texto de un atributo XML al valor que se va a asignar en el
 * modelo, cuando la conversión genérica por tipo de dato no alcanza.
 *
 * <p>Es el punto de extensión de {@link IXmlRowMapper} para los valores que
 * solo la aplicación sabe interpretar; el caso típico es una clave foránea
 * expresada por su clave natural ({@code idappobjecttype="Carpeta"} en vez del
 * identificador numérico): el framework no conoce el catálogo de la
 * aplicación, pero la aplicación sí.</p>
 *
 * <pre>{@code
 * mapper.resolver("idappobjecttype", (campo, texto, nodo, fila) -> tipos.idPorNombre(texto));
 * }</pre>
 *
 * <p>Lanzar una excepción es la forma de rechazar el valor: el mapeador la
 * convierte en un error de la fila ({@code setErrors}, número 50000) y sigue
 * con el atributo siguiente.</p>
 *
 * @author Jorge Enciso
 */
@FunctionalInterface
public interface IXmlFieldResolver {

    /**
     * Devuelve el valor tipado que corresponde al texto recibido.
     *
     * @param field nombre del atributo del modelo que se está asignando.
     * @param text texto leído del nodo XML.
     * @param node nodo XML que se está mapeando (por si hace falta otro dato
     * del mismo nodo).
     * @param row fila que se está completando.
     * @return el valor a asignar, ya del tipo del atributo (puede ser nulo).
     * @throws Exception si el texto no se puede resolver; el mapeador lo
     * registra como error de la fila.
     */
    Object resolve(String field, String text, Element node, IDataRow row) throws Exception;
}
