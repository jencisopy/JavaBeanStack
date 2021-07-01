/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 Jorge Enciso
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
package org.javabeanstack.events;

import java.io.Serializable;
import org.javabeanstack.datactrl.IDataObject;
import org.javabeanstack.data.IDataRow;

/**
 *
 * @author Jorge Enciso
 * @param <T>
 * @param <O>
 */
public interface IDataEvents<O extends IDataObject,T extends IDataRow> extends Serializable{
    public O getContext();
    public void setContext(O context);
    public boolean onAllowAction();
    public boolean beforeRowMove(T curRow);
    public void    afterRowMove(T newRow);
    public void    beforeOpen(String order, String filter,boolean readwrite, int maxrows);
    public void    beforeDataFill();
    public void    afterDataFill();
    public void    afterOpen(String order, String filter,boolean readwrite, int maxrows);
    public void    beforeRequery();
    public void    afterRequery();
    public void    beforeRefreshRow(T row);
    public void    afterRefreshRow(T row);
    public boolean beforeInsertRow(T newRow);
    public void    afterInsertRow(T row);
    public boolean beforeDeleteRow(T row); 
    public void    afterDeleteRow();
    public boolean beforeSetField(T row, String fieldname, Object oldValue, Object newValue);
    public boolean afterSetField(T row, String fieldname, Object oldValue, Object newValue);
    public boolean beforeUpdate(boolean allRows);
    public void    beforeCheckData(boolean allRows);
    public void    afterCheckData(boolean allRows);
    public void    afterUpdate(boolean allRows);
    public void    beforeClose();
    public void    afterClose();    
}
