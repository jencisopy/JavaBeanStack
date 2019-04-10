package org.javabeanstack.web.rest.model;

import javax.xml.bind.annotation.XmlRootElement;
import org.javabeanstack.ws.model.IMessageResponse;

/**
 *
 * @author Jorge Enciso
 */
@XmlRootElement
public class MessageResponse implements IMessageResponse {
    private String id;
    private String link;    
    private String message;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getLink() {
        return link;
    }

    @Override
    public void setLink(String link) {
        this.link = link;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}


