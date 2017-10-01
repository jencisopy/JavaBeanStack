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
package org.javabeanstack.model;

import java.util.Date;
import org.javabeanstack.data.IDataRow;

/**
 *
 * @author Jorge Enciso
 */
public interface IAppSystemParam extends IDataRow {
    Long getIdsystemparam();
    String getParam();
    String getParamDescrip();
    Character getParamType();
    String getSystemgroup();
    Boolean getValueBoolean();
    String getValueChar();
    Date getValueDate();
    Long getValueNumber();

    void setIdsystemparam(Long idsystemparam);
    void setParam(String param);
    void setParamDescrip(String paramDescrip);
    void setParamType(Character paramType);
    void setSystemgroup(String systemgroup);
    void setValueBoolean(Boolean valueBoolean);
    void setValueChar(String valueChar);
    void setValueDate(Date valueDate);
    void setValueNumber(Long valueNumber);
}
