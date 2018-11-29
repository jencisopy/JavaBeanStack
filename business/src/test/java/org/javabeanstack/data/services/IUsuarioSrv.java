package org.javabeanstack.data.services;

import org.javabeanstack.data.services.IDataService;
import org.javabeanstack.annotation.CheckMethod;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.model.appcatalog.AppUser;

/**
 *
 * @author Jorge Enciso
 */
public interface IUsuarioSrv extends IDataService{

    @CheckMethod(fieldName = "codigo", action = {IDataRow.AGREGAR, IDataRow.MODIFICAR, IDataRow.BORRAR})
    IErrorReg checkCodigo(String sessionId, AppUser row);

    @CheckMethod(fieldName = "codigo", action = {IDataRow.BORRAR})
    IErrorReg checkCodigo2(String sessionId, AppUser row);

    @CheckMethod(fieldName = "nombre", action = {IDataRow.AGREGAR, IDataRow.MODIFICAR})
    IErrorReg checkNombre(String sessionId, AppUser row);
    
}
