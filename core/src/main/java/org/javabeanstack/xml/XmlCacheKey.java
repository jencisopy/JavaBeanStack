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
import java.util.Objects;
import static org.javabeanstack.util.Strings.left;
import static org.javabeanstack.util.Strings.isNullorEmpty;

/**
 *
 * @author Jorge Enciso
 */
public class XmlCacheKey implements IXmlCacheKey {
    private String pathType;
    private String documentPath;
    private String elementPath="";
    private Date lastReference;
    private Integer referenceTime=0;
    
    public XmlCacheKey(){
    }

    public XmlCacheKey(String documentPath, String elementPath){
        this.pathType = XmlSearcher.getPathType(documentPath);
        this.documentPath = XmlSearcher.getJustPath(documentPath).toLowerCase();
        this.elementPath = elementPath.trim();        
        if (isNullorEmpty(this.pathType)){
            this.pathType = IXmlSearcher.FILEPATH;
        }
    }   
    
    public XmlCacheKey(String pathType, String documentPath, String elementPath){
        this.pathType = pathType.trim().toLowerCase();
        if (isNullorEmpty(pathType)){
            this.pathType = XmlSearcher.getPathType(documentPath);
        }
        this.documentPath = XmlSearcher.getJustPath(documentPath).toLowerCase();
        this.elementPath = elementPath.trim();
    }


    @Override
    public String getPathType() {
        fixPathType();
        return pathType;
    } 

    
    @Override
    public String getDocumentPath() {
        return documentPath;
    }

    @Override
    public String getElementPath() {
        return elementPath;
    }

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
            
    @Override
    public void setDocumentPath(String documentPath) {
        this.documentPath = XmlSearcher.getJustPath(documentPath).toLowerCase().trim();
    }

    @Override
    public void setElementPath(String elementPath) {
        this.elementPath = elementPath;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + Objects.hashCode(this.pathType);
        hash = 89 * hash + Objects.hashCode(this.documentPath);
        hash = 89 * hash + Objects.hashCode(this.elementPath);
        return hash;
    }

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

    @Override
    public Date getLastReference() {
        return lastReference;
    }

    @Override
    public Integer getReferenceTime() {
        return referenceTime;
    }

    @Override
    public void setLastReference(Date date) {
        this.lastReference = date;
    }

    @Override
    public void addReferenceTime() {
        referenceTime++;
    }
}
