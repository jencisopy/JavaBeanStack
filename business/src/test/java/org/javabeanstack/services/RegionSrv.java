package org.javabeanstack.services;

import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import org.apache.log4j.Logger;
import org.javabeanstack.annotation.CheckMethod;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.error.ErrorReg;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.model.tables.Region;


/**
 *
 * @author Jorge Enciso
 */
@TransactionManagement(value=TransactionManagementType.CONTAINER)
public class RegionSrv extends DataService implements IRegionSrv {
    private static final Logger LOGGER = Logger.getLogger(RegionSrv.class);
    
    @CheckMethod(fieldName = "codigo",
                 action   = {IDataRow.AGREGAR,
                             IDataRow.MODIFICAR,
                             IDataRow.BORRAR}) 
    @Override
    public IErrorReg checkCodigo(Region row, String sessionId){
        IErrorReg errorReg = new ErrorReg(); 
        LOGGER.info("IN validCodigo");
        return errorReg;
    }

    @CheckMethod(fieldName = "codigo",
                 action = {IDataRow.BORRAR}) 
    @Override
    public IErrorReg checkCodigo2(Region row, String sessionId){
        IErrorReg errorReg = new ErrorReg(); 
        LOGGER.info("IN validCodigo2");
        return errorReg;
    }
    
    @CheckMethod(fieldName = "nombre")     
    @Override
    public IErrorReg checkNombre(Region row, String sessionId){
        IErrorReg errorReg = new ErrorReg();
        LOGGER.info("IN checkNombre");
        //errorReg.setMessage("prueba de error");
        return errorReg;
    }
    
    
    public String hello(){
        return "RegionSrv";
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