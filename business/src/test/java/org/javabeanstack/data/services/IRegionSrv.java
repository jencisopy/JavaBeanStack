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
