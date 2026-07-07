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

import java.io.Serializable;
import java.util.List;
import jakarta.xml.bind.annotation.XmlTransient;
import org.javabeanstack.data.IDataRow;

/**
 *
 * @author Jorge Enciso
 * 
 */
public interface IAppCompany extends IDataRow, Serializable {
    Long getIdcompany();
    Long getIdcompanygroup();
    Long getIdcompanymask();
    Long getIdperiod();

    String getName();
    String getCountry();
    String getSocialName();
    String getTaxId();
    String getTelephoneNumber();
    
    String getPersistentUnit();
    String getAddress();
    String getCompanyActivity();    
    String getInformation();

    String getAppuser();    
    @XmlTransient
    List<IAppCompany> getCompanyList();
    

    String getFilesystem();
    byte[] getLogo();
    
    String getMenu();
    String getDbengine();

    void setIdcompany(Long idcompany);
    void setIdcompanygroup(Long idcompanygroup);
    void setIdcompanymask(Long idcompanymask);
    void setIdperiod(Long idperiod);

    void setName(String name);
    void setCountry(String country);
    void setSocialName(String socialName);
    void setTaxId(String taxId);
    void setTelephoneNumber(String number);
    
    void setAddress(String address);
    void setCompanyList(List<IAppCompany> companyList);
    void setCompanyActivity(String empresaActivity);
    void setInformation(String information);
    
    void setAppuser(String appuser);
    void setPersistentUnit(String persistentUnit);

    void setFilesystem(String filesystem);
    void setLogo(byte[] logo);
    void setMenu(String menu);
    void setDbengine(String dbEngine);
}
