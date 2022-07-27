package org.javabeanstack.model;

import java.time.LocalDateTime;
import org.javabeanstack.data.IDataRow;


/**
 *
 * @author Jorge Enciso
 */
public interface IAppUserPwdLog extends IDataRow{
    Long getIdAppUserPwdLog();        
    void setIdAppUserPwdLog(Long IdAppUserPwdLog);        
    
    Long getIduser();    
    void setIduser(Long iduser);    

    String getPwd();        
    void setPwd(String pwd);   

    String getRol();        
    void setRol(String rol);   
    
    LocalDateTime getDateTimeLog();    
}
