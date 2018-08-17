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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType; 
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
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
    private static final long serialVersionUID = -7941769011539363185L;

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
    
    public List<AppUserFormViewColumn> getChild() {
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
