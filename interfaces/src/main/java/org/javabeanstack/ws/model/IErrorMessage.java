package org.javabeanstack.ws.model;

/**
 *
 * @author Jorge Enciso
 */
public interface IErrorMessage {

    String getDocumentation();

    int getErrorCode();

    String getErrorMessage();

    void setDocumentation(String documentation);

    void setErrorCode(int errorCode);

    void setErrorMessage(String errorMessage);
    
}
