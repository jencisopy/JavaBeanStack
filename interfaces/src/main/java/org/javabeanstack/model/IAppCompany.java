package org.javabeanstack.model;

import java.io.Serializable;
import java.util.List;
import javax.xml.bind.annotation.XmlTransient;
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
    
    void setAppuser(String appuser);
    void setPersistentUnit(String persistentUnit);

    void setFilesystem(String filesystem);
    void setLogo(byte[] logo);
    void setMenu(String menu);
    void setDbengine(String dbEngine);
}
