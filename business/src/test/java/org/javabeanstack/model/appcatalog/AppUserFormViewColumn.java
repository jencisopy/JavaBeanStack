/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2015 - 2027 Jorge Enciso
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
package org.javabeanstack.model.appcatalog;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
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
public class AppUserFormViewColumn extends DataRow {
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
    
    @Size(max = 100)
    @Column(name = "columnOrder")
    private String columnOrder;
    
    @Size(max = 100)
    @Column(name = "columnFilter")
    private String columnFilter;
    
    @Size(max = 50)
    @Column(name = "columnType")
    private String columnType;

    @Size(max = 200)
    @Column(name = "columnTitle")
    private String columnTitle;
    
    @Size(max = 300)
    @Column(name = "datasourceparams")
    private String dataSourceParams;
    
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
        this.appUserFormView = (AppUserFormView)appUserFormView;
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

    public String getColumnOrder() {
        return columnOrder;
    }

    public void setColumnOrder(String columnOrder) {
        this.columnOrder = columnOrder;
    }

    public String getColumnFilter() {
        return columnFilter;
    }

    public void setColumnFilter(String columnFilter) {
        this.columnFilter = columnFilter;
    }

    public String getColumnType() {
        return columnType;
    }

    public void setColumnType(String columnType) {
        this.columnType = columnType;
    }

    public String getColumnTitle() {
        return columnTitle;
    }

    public void setColumnTitle(String columnTitle) {
        this.columnTitle = columnTitle;
    }

    public String getDataSourceParams() {
        return dataSourceParams;
    }

    public void setDataSourceParams(String dataSourceParams) {
        this.dataSourceParams = dataSourceParams;
    }
    
    @Override
    public String toString() {
        return "py.com.oym.model.tables.Appuserformviewcolumn[ idappuserformviewcolumn=" + idappuserformviewcolumn + " ]";
    }
}
