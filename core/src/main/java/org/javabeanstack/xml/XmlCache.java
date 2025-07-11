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

import java.io.IOException;
import java.util.Date;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;
import org.javabeanstack.error.ErrorManager;

/**
 * Clase que almacena un objeto DOM o texto xml con el fin de utilizarlos
 * posteriormente para no volver a procesarlos.
 *
 * @author Jorge Enciso
 * @param <T> es el tipo DOM clase org.jdom2.Document or org.w3c.dom.Document
 */
public class XmlCache<T> implements IXmlCache<T> {

    private static final Logger LOGGER = LogManager.getLogger(XmlCache.class);

    private Date processTime;
    private T domObject;
    private String xmlText;
    private Long referenceTimes = 0L;
    private Date lastReference;
    private boolean compiled=false;

    public XmlCache() {
    }

    public XmlCache(T dom) {
        domObject = dom;
        processTime = new Date();
    }

    public XmlCache(String xmlText) {
        this.xmlText = xmlText;
        processTime = new Date();
    }

    public XmlCache(T dom, Date processTime) {
        this.domObject = dom;
        this.processTime = processTime;
    }

    public XmlCache(String xmlText, Date processTime) {
        this.xmlText = xmlText;
        this.processTime = processTime;
    }

    /**
     * Es la cantidad de veces que se hizo referencia a este elemento
     *
     * @return cantidad de referencias hechas a este elemento.
     */
    @Override
    public Long getReferenceTimes() {
        return referenceTimes;
    }

    /**
     * Es la ultima fecha y hora que se hizo referencia a este objeto
     *
     * @return fecha y hora de la ultima referencia
     */
    @Override
    public Date getLastReference() {
        return lastReference;
    }

    /**
     * Es la fecha y hora del procesamiento del objeto DOM.
     *
     * @return fecha y hora del procesamiento del objeto DOM.
     */
    @Override
    public Date getProcessTime() {
        return processTime;
    }

    /**
     * Asignación manual de la fecha de procesamiento del objeto DOM.
     * Normalmente se asigna en el constructor de esta clase.
     *
     * @param date fecha de procesamiento del DOM.
     */
    @Override
    public void setProcessTime(Date date) {
        this.processTime = date;
    }

    /**
     * Devuelve el objeto DOM de este elemento
     *
     * @return objeto DOM.
     */
    @Override
    public T getDom() {
        if (domObject == null) {
            try {
                DomW3cParser.loadXml(xmlText);
            } catch (IOException | ParserConfigurationException | SAXException ex) {
                ErrorManager.showError(ex, LOGGER);
            }
        }
        lastReference = new Date();
        referenceTimes++;
        return domObject;
    }

    /**
     * Devuelve el objeto DOM de este elemento.
     *
     * @param date debe ser menor a la fecha de procesamiento o de lo contrario
     * se considera el DOM invalido u obsoleto.
     * @return objeto DOM.
     */
    @Override
    public T getDom(Date date) {
        if (!isValid(date)) {
            return null;
        }
        if (domObject == null) {
            try {
                DomW3cParser.loadXml(xmlText);
            } catch (IOException | ParserConfigurationException | SAXException ex) {
                ErrorManager.showError(ex, LOGGER);
            }
        }
        lastReference = new Date();
        referenceTimes++;
        return domObject;
    }

    /**
     * Asignación manual de un objeto DOM
     *
     * @param dom objeto DOM.
     */
    @Override
    public void setDom(T dom) {
        this.domObject = dom;
        processTime = new Date();
        referenceTimes = 0L;
        lastReference = null;
        xmlText = "";
    }

    /**
     * Devuelve el texto xml si lo hubiera.
     *
     * @return texto xml
     */
    @Override
    public String getXmlText() {
        return xmlText;
    }

    /**
     * Asignación manual del texto xml
     *
     * @param xmlText texto xml
     */
    @Override
    public void setXmlText(String xmlText) {
        this.xmlText = xmlText;
        domObject = null;
        processTime = new Date();
        referenceTimes = 0L;
        lastReference = null;
    }

    /**
     * Verifica la válidez del elemento, lastModified debe ser menor a la fecha
     * de procesamiento del DOM.
     *
     * @param lastModified fecha de modificación del archivo/objeto desde donde
     * fue procesado el dom.
     * @return verdadero es válido y falso es invalido.
     */
    @Override
    public boolean isValid(Date lastModified) {
        return !getProcessTime().before(lastModified);
    }


    /**
     * Devuelve si el objeto fue procesado con sus clases derivadas.
     * 
     * @return verdadero si fue compilado, falso si no.
     */
    @Override
    public boolean isCompiled() {
        return compiled;
    }

    /**
     * Asigna verdadero si el elemento fue procesado con sus clases derivadas.
     * @param isCompiled 
     */
    @Override
    public void setCompiled(boolean isCompiled) {
        this.compiled = isCompiled;
    }
}
