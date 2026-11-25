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

import java.time.DateTimeException;
import org.javabeanstack.sifen.core.exceptions.ErrorCode;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Clase encargada de leer los Nodos XML y sus hijos y generar las respuestas formateadas.
 */
public class SifenObjectFactory {
    private static final Logger LOGGER = LogManager.getLogger(SifenObjectFactory.class);

    public static <T extends SifenObjectBase> T getFromNode(Node mainNode, Class<T> sifenObjectBase) throws SifenException {
        try {
            T object = sifenObjectBase.newInstance();
            getFromNode(mainNode, object);
            return object;
        } catch (InstantiationException | IllegalAccessException e) {
            LOGGER.throwing(Level.DEBUG, e);
            throw SifenException.of(ErrorCode.JBS_INT_00,
                    "Error de instanciación al intentar crear un objeto de clase: " + sifenObjectBase.getCanonicalName() + " -> " + e.getLocalizedMessage(), e
            );
        }
    }

    public static <T extends SifenObjectBase> void getFromNode(Node mainNode, T object) throws SifenException {
        NodeList childNodes = mainNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            // §3.8 (E2-04): un nodo de texto (indentación) o un comentario no es un campo; la línea
            // base lo pasaba igual y terminaba en NullPointerException sobre getLocalName().
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            try {
                object.setValueFromChildNode(node);
            } catch (UnconvertibleFieldException e) {
                throw e.conPadre(nombre(mainNode));
            } catch (IllegalArgumentException | DateTimeException e) {
                // §3.8: número, fecha o código fuera de la tabla: JBS-XML-05 / JBS-RESP-06 con la ruta
                throw new UnconvertibleFieldException(nombre(mainNode) + "." + nombre(node), e);
            }
        }
    }

    private static String nombre(Node n) {
        return n.getLocalName() != null ? n.getLocalName() : n.getNodeName();
    }
}
