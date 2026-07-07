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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType; 
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.javabeanstack.data.DataRow;

/**
 *
 * @author Jorge Enciso
 */
@Entity
@Table(name = "appuserformview")
@NamedQueries({
    @NamedQuery(name = "AppUserFormView.findAll", query = "SELECT a FROM AppUserFormView a")})
public class AppUserFormView extends DataRow {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "idappuserformview")
    private Long idappuserformview;
    
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "form")
    private String form;
    
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "viewname")
    private String viewName;
    
    @Column(name = "iduser")
    private Long iduser;
    
    @Size(max = 2147483647)
    @Column(name = "filtertext")
    private String filtertext;
    
    
    @OneToMany(cascade = {CascadeType.PERSIST,CascadeType.MERGE}, mappedBy = "appUserFormView")
    @OrderBy("idorder ASC")
    private List<AppUserFormViewColumn> appUserFormViewColumnList = new ArrayList();

    public AppUserFormView() {
    }

    public AppUserFormView(Long idappuserformview) {
        this.idappuserformview = idappuserformview;
    }

    public AppUserFormView(Long idappuserformview, String form, String viewname) {
        this.idappuserformview = idappuserformview;
        this.form = form;
        this.viewName = viewname;
    }

    public Long getIdappuserformview() {
        return idappuserformview;
    }

    public void setIdappuserformview(Long idappuserformview) {
        this.idappuserformview = idappuserformview;
    }

    public String getForm() {
        return form;
    }

    public void setForm(String form) {
        this.form = form;
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public Long getIduser() {
        return iduser;
    }

    public void setIduser(Long iduser) {
        this.iduser = iduser;
    }

    public String getFiltertext() {
        return filtertext;
    }

    public void setFiltertext(String filtertext) {
        this.filtertext = filtertext;
    }

    @Override
    public boolean equivalent(Object o) {
        if (!(o instanceof AppUserFormView)) {
            return false;
        }
        AppUserFormView other = (AppUserFormView) o;
        return (Objects.equals(getForm(), other.getForm())
                && Objects.equals(getViewName(), other.getViewName())
                && Objects.equals(getIduser(), other.getIduser()));
    }

    public List<AppUserFormViewColumn> getAppUserFormViewColumnList() {
        return appUserFormViewColumnList;
    }
    
    public List<AppUserFormViewColumn> getChildren() {
        return appUserFormViewColumnList;
    }
    
    public void setAppUserFormViewColumnList(List<AppUserFormViewColumn> appUserFormViewColumnList) {
        this.appUserFormViewColumnList = appUserFormViewColumnList;
    }

    @Override
    public String toString() {
        return "py.com.oym.model.tables.Appuserformview[ idappuserformview=" + idappuserformview + " ]";
    }
}
