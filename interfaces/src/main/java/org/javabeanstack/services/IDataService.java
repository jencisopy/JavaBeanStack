package org.javabeanstack.services;

import java.io.Serializable;
import java.util.Map;
import org.javabeanstack.data.IDataResult;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.data.IGenericDAO;
import org.javabeanstack.error.IErrorReg;

/**
 *
 * @author Jorge Enciso
 */
public interface IDataService extends IGenericDAO, Serializable{
    <T extends IDataRow> T setListFieldCheck(T row); 
    <T extends IDataRow> boolean checkUniqueKey(T row, String sessionId) ;        
    <T extends IDataRow> boolean checkForeignKey(T row, String fieldName, String sessionId) ;            
    <T extends IDataRow> Map<String, IErrorReg> checkDataRow(T row, String sessionId) ;    

    <T extends IDataRow> IDataResult create(T row, String sessionId) throws Exception;
    <T extends IDataRow> IDataResult edit(T row, String sessionId) throws Exception;
    <T extends IDataRow> IDataResult remove(T row, String sessionId) throws Exception;
}
