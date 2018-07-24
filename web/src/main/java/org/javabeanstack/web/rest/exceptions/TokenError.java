package org.javabeanstack.web.rest.exceptions;

/**
 *
 * @author Jorge Enciso
 */
public class TokenError extends RuntimeException {
    public TokenError(){
        super();
    }
    
    public TokenError(String message){
        super(message);
    }

    public TokenError(String message, Throwable cause){
        super(message, cause);
    }

    public TokenError(Throwable cause){
        super(cause);
    }
}
