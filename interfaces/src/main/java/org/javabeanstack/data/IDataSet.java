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
package org.javabeanstack.data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Jorge Enciso
  */
public interface IDataSet extends Serializable {
    public IDataSet getChanged();
    public Date getLastProcess();
    public void setLastProcess(Date date);
    public List<? extends IDataRow> get(int setNumber);
    public List<? extends IDataRow> get(String key);    
    public Map<String, List<? extends IDataRow>> getMapListSet();
    public Map<String, IDataObject> getMapDataObject();        
    public void add(String key, List<? extends IDataRow> set);
    public void add(String key, IDataRow row);    
    public void addDataObject(String key, IDataObject dataObject);        
    public int size();
}
