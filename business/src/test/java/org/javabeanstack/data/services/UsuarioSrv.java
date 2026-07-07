/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
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

package org.javabeanstack.data.services;

import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.javabeanstack.annotation.CheckMethod;
import org.javabeanstack.data.DBManager;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.error.ErrorReg;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.model.appcatalog.AppUser;


/**
 *
 * @author Jorge Enciso
 */
@TransactionManagement(value=TransactionManagementType.CONTAINER)
public class UsuarioSrv extends DataService implements IUsuarioSrv {
    private static final Logger LOGGER = LogManager.getLogger(UsuarioSrv.class);
    
    @CheckMethod(fieldName = "codigo",
                 action   = {IDataRow.AGREGAR,
                             IDataRow.MODIFICAR,
                             IDataRow.BORRAR}) 
    @Override
    public IErrorReg checkCodigo(String sessionId, AppUser row){
        IErrorReg errorReg = new ErrorReg(); 
        LOGGER.info("IN validCodigo");
        return errorReg;
    }

    @CheckMethod(fieldName = "codigo",
                 action = {IDataRow.BORRAR}) 
    @Override
    public IErrorReg checkCodigo2(String sessionId, AppUser row){
        IErrorReg errorReg = new ErrorReg(); 
        LOGGER.info("IN validCodigo2");
        return errorReg;
    }
    
    @CheckMethod(fieldName = "nombre",
                action   = {IDataRow.AGREGAR,
                            IDataRow.MODIFICAR})     
    @Override
    public IErrorReg checkNombre(String sessionId, AppUser row){
        IErrorReg errorReg = new ErrorReg();
        LOGGER.info("IN checkNombre");
        //errorReg.setMessage("prueba de error");
        return errorReg;
    }
    
    @Override
    protected String getPersistentUnit(String sessionId){
        return DBManager.CATALOGO;
    }
    
    public String hello(){
        return "UsuarioSrv";
    }

//    @PostConstruct
//    public void init() {
//        //System.out.println("Post construct");
//    }
//    
//    @PreDestroy
//    public void destroy() {
//        //System.out.println("destroy");
//    }
//    
//    @Remove
//    public void checkOut() {
//        //System.out.println("remove");
//    }
//    
//    @AfterBegin
//    private void afterBegin(){
//        System.out.println("A new transaction has started.");
//    }
//
//    @BeforeCompletion
//    private void beforeCompletion(){
//        System.out.println("A transaction is about to be committed.");
//    }
//    
//    @AfterCompletion
//    private void afterCompletion(boolean committed) {
//        System.out.println("a transaction commit protocol has completed, and tells the instance whether the transaction has been committed or rolled back , based on committed value : " + committed);
//    }
    
}
