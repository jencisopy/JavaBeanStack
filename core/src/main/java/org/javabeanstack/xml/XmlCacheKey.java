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
import java.util.Objects;
import static org.javabeanstack.util.Strings.left;
import static org.javabeanstack.util.Strings.isNullorEmpty;

/**
 *
 * Implementación de {@link IXmlCacheKey}: clave de la caché de documentos XML
 * compuesta por el tipo de origen, la ruta del documento y la ruta del
 * elemento, con el contador de referencias.
 *
 * @author Jorge Enciso
 */
public class XmlCacheKey implements IXmlCacheKey {
    private String pathType;
    private String documentPath;
    private String elementPath="";
    private Date lastReference;
    private Integer referenceTime=0;
    
    /**
     * Constructor por defecto.
     */
    public XmlCacheKey(){
    }

    /**
     * Crea la clave a partir de la ruta del documento y del elemento; deduce el
     * tipo de origen de la ruta.
     *
     * @param documentPath ruta del documento.
     * @param elementPath ruta del elemento.
     */
    public XmlCacheKey(String documentPath, String elementPath){
        this.pathType = XmlSearcher.getPathType(documentPath);
        this.documentPath = XmlSearcher.getJustPath(documentPath).toLowerCase();
        this.elementPath = elementPath.trim();        
        if (isNullorEmpty(this.pathType)){
            this.pathType = IXmlSearcher.FILEPATH;
        }
    }   
    
    /**
     * Crea la clave con el tipo de origen, la ruta del documento y del elemento.
     *
     * @param pathType tipo de origen (archivo, objeto, http).
     * @param documentPath ruta del documento.
     * @param elementPath ruta del elemento.
     */
    public XmlCacheKey(String pathType, String documentPath, String elementPath){
        this.pathType = pathType.trim().toLowerCase();
        if (isNullorEmpty(pathType)){
            this.pathType = XmlSearcher.getPathType(documentPath);
        }
        this.documentPath = XmlSearcher.getJustPath(documentPath).toLowerCase();
        this.elementPath = elementPath.trim();
    }


    /**
     * Devuelve el tipo de origen del documento (archivo, objeto, http).
     *
     * @return tipo de origen.
     */
    @Override
    public String getPathType() {
        fixPathType();
        return pathType;
    } 

    
    /**
     * Devuelve la ruta del documento.
     *
     * @return ruta del documento.
     */
    @Override
    public String getDocumentPath() {
        return documentPath;
    }

    /**
     * Devuelve la ruta del elemento dentro del documento.
     *
     * @return ruta del elemento.
     */
    @Override
    public String getElementPath() {
        return elementPath;
    }

    /**
     * Asigna el tipo de origen del documento.
     *
     * @param pathType tipo de origen.
     */
    @Override
    public void setPathType(String pathType) {
        this.pathType = pathType.trim().toLowerCase();
        fixPathType();
    }
    
    private void fixPathType(){
        if (this.pathType.endsWith(":")){
            this.pathType = left(this.pathType,this.pathType.length()-1);
        }
    }
            
    /**
     * Asigna la ruta del documento.
     *
     * @param documentPath ruta del documento.
     */
    @Override
    public void setDocumentPath(String documentPath) {
        this.documentPath = XmlSearcher.getJustPath(documentPath).toLowerCase().trim();
    }

    /**
     * Asigna la ruta del elemento dentro del documento.
     *
     * @param elementPath ruta del elemento.
     */
    @Override
    public void setElementPath(String elementPath) {
        this.elementPath = elementPath;
    }

    /**
     * Identificador del objeto
     *
     * @return identificador del objeto
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + Objects.hashCode(this.pathType);
        hash = 89 * hash + Objects.hashCode(this.documentPath);
        hash = 89 * hash + Objects.hashCode(this.elementPath);
        return hash;
    }

    /**
     * Determina si este objeto es igual a uno que se recibe como parámetro
     *
     * @param obj objeto a comparar.
     * @return verdadero si es igual y falso si no
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final XmlCacheKey other = (XmlCacheKey) obj;
        if (!Objects.equals(this.pathType.toLowerCase(), other.pathType.toLowerCase())) {
            return false;
        }
        if (!Objects.equals(this.documentPath, other.documentPath)) {
            return false;
        }
        return Objects.equals(this.elementPath, other.elementPath);
    }

    /**
     * Devuelve la fecha y hora de la última referencia.
     *
     * @return fecha de la última referencia.
     */
    @Override
    public Date getLastReference() {
        return lastReference;
    }

    /**
     * Devuelve la cantidad de referencias hechas a la clave.
     *
     * @return cantidad de referencias.
     */
    @Override
    public Integer getReferenceTime() {
        return referenceTime;
    }

    /**
     * Asigna la fecha y hora de la última referencia.
     *
     * @param date fecha de la última referencia.
     */
    @Override
    public void setLastReference(Date date) {
        this.lastReference = date;
    }

    /**
     * Incrementa el contador de referencias a la clave.
     */
    @Override
    public void addReferenceTime() {
        referenceTime++;
    }
}
