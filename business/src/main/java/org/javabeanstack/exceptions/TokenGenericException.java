package org.javabeanstack.exceptions;

/**
 *
 * @author Jorge Enciso
 */
public class TokenGenericException extends RuntimeException {
    public TokenGenericException(){
        super();
    }
    
    public TokenGenericException(String message){
        super(message);
    }

    public TokenGenericException(String message, Throwable cause){
        super(message, cause);
    }

    public TokenGenericException(Throwable cause){
        super(cause);
    }
}
