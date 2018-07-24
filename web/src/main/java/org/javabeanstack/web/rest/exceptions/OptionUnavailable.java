package org.javabeanstack.web.rest.exceptions;

/**
 *
 * @author Jorge Enciso
 */
public class OptionUnavailable extends RuntimeException {
    public OptionUnavailable(){
        super();
    }
    
    public OptionUnavailable(String message){
        super(message);
    }

    public OptionUnavailable(String message, Throwable cause){
        super(message, cause);
    }

    public OptionUnavailable(Throwable cause){
        super(cause);
    }
}
