package org.javabeanstack.ws.resources;

import org.javabeanstack.security.ISecManager;
import org.javabeanstack.services.IDataService;

/**
 *
 * @author Jorge Enciso
 */
public interface IWebResource{
    <T extends IDataService> T getDataService();
    ISecManager getSecManager();
    ISecManager getSecManager(String jndi);
    Long getIdempresa();
    Long getIdPerson();
    String getPersonRol();
    String getIpClient();     
}
