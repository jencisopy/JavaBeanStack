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
import java.util.Date;

/**
 * Contrato de la clave de la caché de documentos XML ({@link IXmlCache}):
 * identifica un documento cacheado por su tipo de origen, ruta de documento y
 * ruta de elemento, y lleva la cuenta de referencias.
 *
 * @author Jorge Enciso
 */
public interface IXmlCacheKey {
    /**
     * Devuelve el tipo de origen del documento (archivo, objeto, http).
     *
     * @return tipo de origen.
     */
    String getPathType();

    /**
     * Devuelve la ruta del documento.
     *
     * @return ruta del documento.
     */
    String getDocumentPath();

    /**
     * Devuelve la ruta del elemento dentro del documento.
     *
     * @return ruta del elemento.
     */
    String getElementPath();

    /**
     * Devuelve la fecha y hora de la última referencia.
     *
     * @return fecha de la última referencia.
     */
    Date getLastReference();

    /**
     * Devuelve la cantidad de referencias hechas a la clave.
     *
     * @return cantidad de referencias.
     */
    Integer getReferenceTime();

    /**
     * Asigna la ruta del documento.
     *
     * @param documentPath ruta del documento.
     */
    void setDocumentPath(String documentPath);

    /**
     * Asigna la ruta del elemento dentro del documento.
     *
     * @param elementPath ruta del elemento.
     */
    void setElementPath(String elementPath);

    /**
     * Asigna el tipo de origen del documento.
     *
     * @param pathType tipo de origen.
     */
    void setPathType(String pathType);

    /**
     * Asigna la fecha y hora de la última referencia.
     *
     * @param date fecha de la última referencia.
     */
    void setLastReference(Date date);

    /**
     * Incrementa el contador de referencias a la clave.
     */
    void addReferenceTime();
}
