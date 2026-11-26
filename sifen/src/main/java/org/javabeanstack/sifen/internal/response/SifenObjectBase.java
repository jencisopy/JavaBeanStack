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

import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.types.CMondT;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import java.math.BigDecimal;

/**
 * Clase abstracta heredada por las clases utilizadas como parte de las respuestas a las peticiones, y también
 * por el modelo de entrada ({@code DocumentoElectronico}, {@code EventosDE} y los grupos {@code Tg*}/{@code Tr*}).
 *
 * <p>Es de un paquete interno ({@code internal.*}: «no utilizar»), pero es la superclase de los
 * {@code Respuesta*} deprecados en favor de {@link org.javabeanstack.sifen.ISifenClient} (D42) y de los tipos de
 * respuesta que ellos exponen. Por eso, como excepción a lo interno, su firma pública
 * ({@link #setValueFromChildNode(Node)}) se mantiene sin cambios en toda la línea 2.x, sin {@code @Deprecated}
 * (decisión del usuario ESC-36, I8-35). No es contrato v1 ni se retira con lo deprecado: el modelo de entrada,
 * que sí es contrato, también la extiende.</p>
 */
public abstract class SifenObjectBase {
    public abstract void setValueFromChildNode(Node value) throws SifenException;
}