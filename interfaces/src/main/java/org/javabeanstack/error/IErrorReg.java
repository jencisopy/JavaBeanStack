package org.javabeanstack.error;

import java.io.Serializable;

/**
 *
 * @author Jorge Enciso
 */
public interface IErrorReg extends Serializable {
    public String getEntity();    
    public String getFieldName();
    public String[] getFieldNames();    
    public String getMessage();
    public Integer getErrorNumber();
    public Exception getException();    
    public String getIpRequest();
    public String getEvent();
    public String getLevel();

    
    
    public boolean isWarning();
    public void setEntity(String entity);
    public void setFieldName(String fieldName);
    public void setFieldNames(String[] fieldNames);    
    public void setMessage(String message);
    public void setErrorNumber(int errorNumber);
    public void setException(Exception exp);
    public void setWarning(boolean warning);
    public void setIpRequest(String ip);
    public void setEvent(String event);
    public void setLevel(String level);

}
