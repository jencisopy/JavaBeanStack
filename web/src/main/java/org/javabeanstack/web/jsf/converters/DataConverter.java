/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2018 Jorge Enciso
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

package org.javabeanstack.web.jsf.converters;

import javax.inject.Inject;
import org.javabeanstack.data.DataLink;
import org.javabeanstack.data.IDataLink;
import org.javabeanstack.data.IDataRow;


/**
 *
 * @author Jorge Enciso
 * @param <T>
 */
public class DataConverter<T extends IDataRow> extends AbstractDataConverter<T> {
    @Inject
    private DataLink dao;

    public DataConverter(){
    }
    
    public DataConverter(Class<T> clase){
        super(clase);
    }

    @Override
    public IDataLink getDAO() {
        return dao;
    }
}
