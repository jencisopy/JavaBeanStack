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
package org.javabeanstack.model;

import java.math.BigInteger;
import java.time.LocalDateTime;
import org.javabeanstack.data.IDataRow;

/**
 * Contrato de la entidad fuente XML: almacena el texto XML original y compilado
 * de un recurso, con su fecha de procesamiento y contador de referencias, para
 * el motor de documentos XML ({@link org.javabeanstack.xml.IXmlManager}).
 * Extiende {@link IDataRow}.
 *
 * @author Jorge Enciso
 */
public interface IAppXmlSource extends IDataRow {
    /**
     * Devuelve el identificador de la fuente XML.
     * @return identificador de la fuente XML.
     */
    Long getIdXmlSource();

    /**
     * Devuelve el identificador del objeto asociado.
     * @return identificador del objeto.
     */
    Long getIdObject();

    /**
     * Devuelve el nombre del XML.
     * @return nombre del XML.
     */
    String getXmlName();

    /**
     * Devuelve el texto XML original.
     * @return texto XML original.
     */
    String getXmlSource();

    /**
     * Devuelve el texto XML compilado (procesado con sus clases derivadas).
     * @return texto XML compilado.
     */
    String getXmlCompiled();

    /**
     * Devuelve la fecha y hora de procesamiento del XML.
     * @return fecha de procesamiento.
     */
    LocalDateTime getProcessTime();

    /**
     * Devuelve el contador de referencias al XML.
     * @return cantidad de referencias.
     */
    BigInteger getReferencetime();

    /**
     * Devuelve la ruta del XML.
     * @return ruta del XML.
     */
    String getXmlPath();

    /**
     * Indica si la fuente XML es válida.
     * @return verdadero si es válida, falso si no.
     */
    boolean isValid();

    /**
     * Asigna el identificador de la fuente XML.
     * @param idxmlsource identificador de la fuente XML.
     */
    void setIdXmlSource(Long idxmlsource);

    /**
     * Asigna el identificador del objeto asociado.
     * @param idobject identificador del objeto.
     */
    void setIdObject(Long idobject);

    /**
     * Asigna el nombre del XML.
     * @param xmlname nombre del XML.
     */
    void setXmlName(String xmlname);

    /**
     * Asigna el texto XML original.
     * @param xmlsource texto XML original.
     */
    void setXmlSource(String xmlsource);

    /**
     * Asigna el texto XML compilado.
     * @param xmlcompile texto XML compilado.
     */
    void setXmlCompiled(String xmlcompile);

    /**
     * Asigna la fecha y hora de procesamiento del XML.
     * @param processtime fecha de procesamiento.
     */
    void setProcessTime(LocalDateTime processtime);

    /**
     * Asigna el contador de referencias al XML.
     * @param referencetime cantidad de referencias.
     */
    void setReferencetime(BigInteger referencetime);

    /**
     * Asigna la ruta del XML.
     * @param xmlpath ruta del XML.
     */
    void setXmlPath(String xmlpath);
}
