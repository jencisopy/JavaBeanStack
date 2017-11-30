package org.javabeanstack.services;

import java.io.Serializable;
import java.util.Map;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.data.IGenericDAO;
import org.javabeanstack.error.IErrorReg;

/**
 *
 * @author Jorge Enciso
 */
public interface IDataService extends IGenericDAO, Serializable{
    <T extends IDataRow> T setListFieldCheck(T row); 
    <T extends IDataRow> boolean checkUniqueKey(String sessionId, T row) throws Exception;        
    <T extends IDataRow> boolean checkForeignKey(String sessionId, T row, String fieldName) throws Exception;            
    <T extends IDataRow> Map<String, IErrorReg> checkDataRow(String sessionId, T row) ;    
}
