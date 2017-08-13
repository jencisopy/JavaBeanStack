/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 3 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
* MA 02110-1301  USA
*/

package org.javabeanstack.data;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Resource;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.SessionContext;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import org.apache.log4j.Logger;

import org.javabeanstack.error.ErrorManager;

/**
 * Contiene metodos para gestionar el acceso a los datos, es utilizado
 *  por GenericDAO 
 * 
 * @author Jorge Enciso
 */
@Startup
@Lock(LockType.READ)
public class DBManager implements IDBManager, IDBManagerLocal, IDBManagerRemote{
    private static final Logger LOGGER = Logger.getLogger(DBManager.class);    
    
    private int entityIdStrategic = IDBManager.PERTHREAD;
            
    Map<String, EntityManager> entityManagers = new HashMap<>();

    @Resource
    SessionContext context;

    @Override
    public int getEntityIdStrategic() {
        return entityIdStrategic;
    }

    
    /**
     * Devuelve un entityManager, lo crea si no existe en la unidad de persistencia solicitada
     * @param key  id thread
     * @return Devuelve un entityManager
     */    
    @Override 
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public EntityManager getEntityManager(String key) {
        try {
            if (key == null || "".equals(key)) {
                return null;
            }
            EntityManager em;
            if (entityManagers.containsKey(key)) {
                em = (EntityManager) entityManagers.get(key);
                LOGGER.debug("--------- EntityManager ya existe --------- " + key);
            } else {
                em = this.createEntityManager(key);
            }
            return em;
        } catch (Exception ex) {
            ErrorManager.showError(ex,LOGGER);            
        }
        return null;
    }

    /**
     * Crea un entitymanager dentro de un Map utiliza la unidad de persistencia y el 
     *    threadid como clave
     * @param key  id thread
     * @return          el entity manager creado.
     */
    @Override 
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)    
    @Lock(LockType.WRITE)
    public EntityManager createEntityManager(String key) {      
        EntityManager em;
        try {
            String persistentUnit = key.substring(0,key.indexOf(":")).toLowerCase();
            em = (EntityManager) context.lookup("java:comp/env/persistence/" + persistentUnit);
            entityManagers.put(key, em);
            LOGGER.debug("--------- Se ha creado un nuevo EntityManager --------- " + key);
            return em;
        } catch (Exception ex) {
            ErrorManager.showError(ex,LOGGER);
        }
        return null;
    }
    
    /**
     *  Ejecuta rollback de una transacción 
     */    
    @Override 
    @Lock(LockType.WRITE)    
    public void rollBack(){
        context.setRollbackOnly();
    }
}
