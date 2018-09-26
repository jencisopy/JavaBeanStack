/*
* Copyright (c) 2015-2017 OyM System Group S.A.
* Capitan Cristaldo 464, Asunción, Paraguay
* All rights reserved. 
*
* NOTICE:  All information contained herein is, and remains
* the property of OyM System Group S.A. and its suppliers,
* if any.  The intellectual and technical concepts contained
* herein are proprietary to OyM System Group S.A.
* and its suppliers and protected by trade secret or copyright law.
* Dissemination of this information or reproduction of this material
* is strictly forbidden unless prior written permission is obtained
* from OyM System Group S.A.
 */
package org.javabeanstack.model.tables;

import java.util.Objects;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import org.javabeanstack.data.DataRow;

/**
 *
 * @author Jorge Enciso
 */
@Entity
@Table(name = "appuserformviewcolumn") 
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "AppUserFormViewColumn.findAll", query = "SELECT a FROM AppUserFormViewColumn a")})
public class AppUserFormViewColumn extends DataRow{
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "idappuserformviewcolumn")
    private Long idappuserformviewcolumn; 
    


    @JoinColumn(name = "idappuserformview", referencedColumnName = "idappuserformview")
    @ManyToOne(optional = false)
    private AppUserFormView appUserFormView;

    @Basic(optional = false)
    @NotNull
    @Column(name = "idorder")
    private Integer idorder;

    @NotNull
    @Size(max = 50)
    @Column(name = "columnname")
    private String columnName;
    
    @Size(max = 50)
    @Column(name = "columnheader")
    private String columnHeader;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "visible")
    private boolean visible;
    
    @Size(max = 100)
    @Column(name = "link")
    private String link;
    
    @Size(max = 50)
    @Column(name = "style")
    private String style;

    @Size(max = 50)
    @Column(name = "filterfunction")
    private String filterFunction;

    @Size(max = 50)
    @Column(name = "mask")
    private String mask;

    @Column(name = "width")
    private Integer width;
    

    public AppUserFormViewColumn() {
    }

    public AppUserFormViewColumn(Long idappuserformviewcolumn) {
        this.idappuserformviewcolumn = idappuserformviewcolumn;
    }

    public AppUserFormViewColumn(Long idappuserformviewcolumn, boolean visible) {
        this.idappuserformviewcolumn = idappuserformviewcolumn;
        this.visible = visible;
    }

    public Long getIdappuserformviewcolumn() {
        return idappuserformviewcolumn;
    }

    public void setIdappuserformviewcolumn(Long idappuserformviewcolumn) {
        this.idappuserformviewcolumn = idappuserformviewcolumn;
    }


    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getColumnHeader() {
        return columnHeader;
    }

    public void setColumnHeader(String header) {
        this.columnHeader = header;
    }

    public boolean getVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    @Override
    public boolean equivalent(Object o) {
        return equals(o);
    }


    public AppUserFormView getAppUserFormView() {
        return appUserFormView;
    }

    public void setAppUserFormView(AppUserFormView appUserFormView) {
        this.appUserFormView = appUserFormView;
    }
    

    public Integer getIdorder() {
        return idorder;
    }

    public void setIdorder(Integer idorder) {
        this.idorder = idorder;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getFilterFunction() {
        return filterFunction;
    }

    public void setFilterFunction(String filterFunction) {
        this.filterFunction = filterFunction;
    }

    public String getMask() {
        return mask;
    }

    public void setMask(String mask) {
        this.mask = mask;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.idappuserformviewcolumn);
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
        final AppUserFormViewColumn other = (AppUserFormViewColumn) obj;
        if (!Objects.equals(this.idappuserformviewcolumn, other.idappuserformviewcolumn)) {
            return false;
        }
        return true;
    }
    
    @Override
    public String toString() {
        return "py.com.oym.model.tables.Appuserformviewcolumn[ idappuserformviewcolumn=" + idappuserformviewcolumn + " ]";
    }
}
