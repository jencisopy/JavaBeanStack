package org.javabeanstack.services;

import org.javabeanstack.annotation.CheckMethod;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.model.tables.AppUser;

/**
 *
 * @author Jorge Enciso
 */
public interface IUsuarioSrv extends IDataService{

    @CheckMethod(fieldName = "codigo", action = {IDataRow.AGREGAR, IDataRow.MODIFICAR, IDataRow.BORRAR})
    IErrorReg checkCodigo(AppUser row, String sessionId);

    @CheckMethod(fieldName = "codigo", action = {IDataRow.BORRAR})
    IErrorReg checkCodigo2(AppUser row, String sessionId);

    @CheckMethod(fieldName = "nombre")
    IErrorReg checkNombre(AppUser row, String sessionId);
    
}
