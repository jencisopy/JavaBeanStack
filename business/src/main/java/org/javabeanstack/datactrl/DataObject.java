/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2018 Jorge Enciso
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

package org.javabeanstack.datactrl;

import javax.inject.Inject;
import org.javabeanstack.data.IDataLink;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.events.IDataEvents;


/**
 * Esta clase se encarga de almacenar los registros de datos
 * recuperados de la base de datos a travéz de la clase GenericDAO. Provee interfases
 * que permiten la recuperación, busqueda, refresco, y actualización de los registros.
 * @author Jorge Enciso
 * @param <T>   tipo ejb
 */
public class DataObject <T extends IDataRow> extends AbstractDataObject{

    @Inject
    private IDataLink dao;

    @Inject
    private IDataLink daoCatalog;
    
    public DataObject(){
    }

    public DataObject(Class<T> type){
        this.setType(type);
    }    

    public DataObject(Class<T> type, IDataEvents dtEvents){
        this.setType(type);
        this.setDataEvents(dtEvents);
    }

    public DataObject(Class<T> type, IDataEvents dtEvents, IDataLink dao, IDataLink daoCatalog){
        this.setType(type);
        this.setDataEvents(dtEvents);
        if (dao != null){
            this.dao = dao;            
        }
        if (daoCatalog != null){
            this.daoCatalog = daoCatalog;            
        }
    }
    
    @Override
    public IDataLink getDAO() {
        return dao; 
    }
    
    @Override
    public IDataLink getDAOCatalog() {
        return daoCatalog; 
    }
}
