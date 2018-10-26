package org.javabeanstack.services;

import org.javabeanstack.annotation.CheckMethod;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.model.tables.Region;

/**
 *
 * @author Jorge Enciso
 */
public interface IRegionSrv extends IDataService{

    @CheckMethod(fieldName = "codigo", action = {IDataRow.AGREGAR, IDataRow.MODIFICAR, IDataRow.BORRAR})
    IErrorReg checkCodigo(String sessionId, Region row);

    @CheckMethod(fieldName = "codigo", action = {IDataRow.BORRAR})
    IErrorReg checkCodigo2(String sessionId, Region row);

    @CheckMethod(fieldName = "nombre",  action = {IDataRow.AGREGAR, IDataRow.MODIFICAR})
    IErrorReg checkNombre(String sessionId, Region row);
    
}
