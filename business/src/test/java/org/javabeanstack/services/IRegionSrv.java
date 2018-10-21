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
    IErrorReg checkCodigo(Region row, String sessionId);

    @CheckMethod(fieldName = "codigo", action = {IDataRow.BORRAR})
    IErrorReg checkCodigo2(Region row, String sessionId);

    @CheckMethod(fieldName = "nombre")
    IErrorReg checkNombre(Region row, String sessionId);
    
}
