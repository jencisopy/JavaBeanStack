package org.javabeanstack.model;

import org.javabeanstack.data.IDataRow;
import org.w3c.dom.Document;


/**
 *
 * @author Jorge Enciso
 */
public interface IAppObjectAuth extends IDataRow{
    public static int DENIED = 1;
    public static int ALLOWED = 0;
    
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
    
    Integer getCancel();        
    void setCancel(Integer cancel);   
    
    Integer getAttach();        
    void setAttach(Integer attach);   
    
    Integer getCopyFrom();        
    void setCopyFrom(Integer copyFrom);   
    
    Document getAuthXmlDom();
    void setAuthXmlDom(Document xmlDom);
}
