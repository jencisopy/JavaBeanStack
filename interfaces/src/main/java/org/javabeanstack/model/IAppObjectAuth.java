package org.javabeanstack.model;

import org.javabeanstack.data.IDataRow;


/**
 *
 * @author Jorge Enciso
 */
public interface IAppObjectAuth extends IDataRow{
    Long getIdAppObjectAuth();        
    void setIdAppObjectAuth(Long IdAppObjectAuth);        
    
    Long getIduser();    
    void setIduser(Long iduser);    

    Long getIdAppObject();    
    void setIdAppObject(Long idAppObject);    
    
    String getAuth();        
    void setAuth(String auth);   

    Integer getRead();        
    void setRead(Integer read);   

    Integer getWrite();        
    void setWrite(Integer write);   

    Integer getExecute();        
    void setExecute(Integer execute);   

    Integer getInsert();        
    void setInsert(Integer insert);   

    Integer getDelete();        
    void setDelete(Integer delete);   

    Integer getUpdate();        
    void setUpdate(Integer update);   
    
    Integer getConfirm();        
    void setConfirm(Integer confirm);   
    
    Integer getAttach();        
    void setAttach(Integer attach);   
    
    Integer getCopyFrom();        
    void setCopyFrom(Integer copyFrom);   
}
