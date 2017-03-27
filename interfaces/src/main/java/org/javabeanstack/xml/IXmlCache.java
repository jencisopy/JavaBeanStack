/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 Jorge Enciso
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
 * Clase que almacena un objeto DOM o texto xml con el fin de utilizarlos
 * posteriormente para no volver a procesarlos.
 *
 * @author Jorge Enciso
 * @param <T> es el tipo DOM clase org.jdom2.Document or org.w3c.dom.Document
 */
public interface IXmlCache<T> {
    /**
     * Es la fecha y hora del procesamiento del objeto DOM.
     *
     * @return fecha y hora del procesamiento del objeto DOM.
     */
    Date getProcessTime();
    
    /**
     * Devuelve si el objeto fue procesado con sus clases derivadas.
     * 
     * @return verdadero si fue compilado, falso si no.
     */
    boolean isCompiled();

    /**
     * Es la cantidad de veces que se hizo referencia a este elemento
     *
     * @return cantidad de referencias hechas a este elemento.
     */
    Long getReferenceTimes();

    /**
     * Es la ultima fecha y hora que se hizo referencia a este objeto
     *
     * @return fecha y hora de la ultima referencia
     */
    Date getLastReference();

    /**
     * Devuelve el texto xml si lo hubiera.
     *
     * @return texto xml
     */
    String getXmlText();

    /**
     * Devuelve el objeto DOM de este elemento
     *
     * @return objeto DOM.
     */
    T getDom();

    /**
     * Devuelve el objeto DOM de este elemento.
     *
     * @param date debe ser menor a la fecha de procesamiento o de lo contrario
     * se considera el DOM invalido u obsoleto.
     * @return objeto DOM.
     */
    T getDom(Date date);

    /**
     * Asignación manual de la fecha de procesamiento del objeto DOM.
     * Normalmente se asigna en el constructor de esta clase.
     *
     * @param date fecha de procesamiento del DOM.
     */
    void setProcessTime(Date date);

    /**
     * Asignación manual de un objeto DOM
     *
     * @param object objeto DOM.
     */
    void setDom(T object);

    /**
     * Asignación manual del texto xml
     *
     * @param xmlText texto xml
     */
    void setXmlText(String xmlText);
    
    /**
     * Asigna verdadero si el elemento fue procesado con sus clases derivadas.
     * @param isCompiled 
     */
    void setCompiled(boolean isCompiled);

    /**
     * Verifica la válidez del elemento, lastModified debe ser menor a la fecha
     * de procesamiento del DOM.
     *
     * @param lastModified fecha de modificación del archivo/objeto desde donde
     * fue procesado el dom.
     * @return verdadero es válido y falso es invalido.
     */
    boolean isValid(Date lastModified);
}
