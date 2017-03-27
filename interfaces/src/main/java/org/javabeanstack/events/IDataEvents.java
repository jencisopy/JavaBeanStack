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

import org.javabeanstack.data.IDataRow;

/**
 *
 * @author Jorge Enciso
 */
public interface IDataEvents {
    public boolean onAllowOperation();
    public boolean beforeRowMove(IDataRow curRow);
    public void    afterRowMove(IDataRow newRow);
    public void    beforeOpen(String order, String filter,boolean readwrite, int maxrows);
    public void    beforeDataFill();
    public void    afterDataFill();
    public void    afterOpen(String order, String filter,boolean readwrite, int maxrows);
    public void    beforeRequery();
    public void    afterRequery();
    public void    beforeRefreshRow(IDataRow row);
    public void    afterRefreshRow(IDataRow row);
    public boolean beforeInsertRow(IDataRow newRow);
    public void    afterInsertRow(IDataRow row);
    public boolean beforeDeleteRow(IDataRow row); 
    public void    afterDeleteRow();
    public boolean beforeSetField(IDataRow row, String fieldname, Object oldValue, Object newValue);
    public boolean afterSetField(IDataRow row, String fieldname, Object oldValue, Object newValue);
    public boolean beforeUpdate(boolean allRows);
    public void    beforeCheckData(boolean allRows);
    public void    afterCheckData(boolean allRows);
    public void    afterUpdate(boolean allRows);
    public void    beforeClose();
    public void    afterClose();    
}
