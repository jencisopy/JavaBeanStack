/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2018 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
*        jenciso@javabeanstack.org
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
import java.util.List;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.datactrl.IDataObject;

/**
 *
 * @author Jorge Enciso
 * @param <T>
 */
public interface ICtrlEvents<T extends IDataObject> extends Serializable{
    void onRowSelect(T context, Object event);
    void onRowFilter(T context);
    void onChange(T context, String fieldname);
    <X extends IDataRow> List<X> onCompleteText(T context, String text);    
}
