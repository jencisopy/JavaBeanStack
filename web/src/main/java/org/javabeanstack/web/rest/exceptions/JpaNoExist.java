package org.javabeanstack.web.rest.exceptions;

/**
 *
 * @author Jorge Enciso
 */
public class JpaNoExist extends RuntimeException {
    public JpaNoExist(){
        super();
    }
    public JpaNoExist(String message){
        super(message);
    }

    public JpaNoExist(String message, Throwable cause){
        super(message, cause);
    }

    public JpaNoExist(Throwable cause){
        super(cause);
    }
}
