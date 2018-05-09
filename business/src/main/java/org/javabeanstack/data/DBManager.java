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

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Resource;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.SessionContext;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;

import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.util.Dates;

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
    private int entityIdStrategic = IDBManager.PERSESSION;
    private Date lastPurge=new Date();
            
    private final Map<String, Data> entityManagers = new HashMap<>();

    @Resource
    SessionContext context;

    /**
     * Devuelve la estrategia de acceso/creación de los entityManagers.
     * Los valores posibles son: un entityManager por Thread o un entityManager 
     * por sesión del usuario.
     * @return estrategia de acceso/creación de los entityManagers.
     */
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
                em = (EntityManager) entityManagers.get(key).em;
                entityManagers.get(key).lastRef = Dates.now();
                LOGGER.debug("EntityManager ya existe: " + key);
            } else {
                em = this.createEntityManager(key);
            }
            purgeEntityManager();
            return em;
        } catch (Exception ex) {
            ErrorManager.showError(ex,LOGGER);            
        }
        return null;
    }

    /**
     * Crea un entitymanager dentro de un Map utiliza la unidad de persistencia 
     * y el threadid o sessionid del usuario como clave
     * 
     * @param key  id thread o sessionid del usuario
     * @return el entity manager creado.
     */
    @Override 
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)    
    @Lock(LockType.WRITE)
    public EntityManager createEntityManager(String key) {      
        EntityManager em;
        try {
            String persistentUnit = key.substring(0,key.indexOf(':')).toLowerCase();
            em = (EntityManager) context.lookup("java:comp/env/persistence/" + persistentUnit);
            Data data = new Data();
            data.em = em;
            entityManagers.put(key, data);
            LOGGER.debug("--------- Se ha creado un nuevo EntityManager --------- " + key);
            return em;
        } catch (Exception ex) {
            ErrorManager.showError(ex,LOGGER);
        }
        return null;
    }
    
    /**
     * Elimina los entityManagers del map, a aquellos que no se esta utilizando
     * en un periodo dado.
     */
    protected void purgeEntityManager(){
        LOGGER.debug("purgeEntityManager() "+lastPurge);                        
        Date now = new Date();
        //Solo procesar si la ultima purga fue hace 5 minutos.
        if (!lastPurge.before(DateUtils.addMinutes(now, -5))){
            return;
        }
        //Purgar aquellos entityManagers que no fueron referenciados hace 5 minutos
        now = DateUtils.addMinutes(Dates.now(),-5);
        for(Iterator<Map.Entry<String, Data>> it = entityManagers.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Data> entry = it.next();
            if(entry.getValue().lastRef.before(now)) {
                LOGGER.debug("Se elimino entityManager: " + entry.getKey());                
                it.remove();
            }        
        }
        lastPurge = new Date();
        LOGGER.debug("Se proceso purgeEntityManager "+lastPurge);                                
    }
    
    /**
     *  Ejecuta rollback de una transacción 
     */    
    @Override 
    @Lock(LockType.WRITE)    
    public void rollBack(){
        context.setRollbackOnly();
    }
    
    class Data {
         EntityManager em;
         Date lastRef = Dates.now();
    }
}
