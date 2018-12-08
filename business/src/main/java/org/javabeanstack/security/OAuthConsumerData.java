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
package org.javabeanstack.security;

import java.util.Map;
import java.util.TreeMap;
import static org.javabeanstack.util.Fn.nvl;

/**
 *
 * @author Jorge Enciso
 */
public class OAuthConsumerData implements IOAuthConsumerData{
    private Long idAppUser=0L;
    private Long idCompany=0L;
    private Map<String, String> otherData = new TreeMap();

    @Override
    public Long getIdAppUser() {
        return idAppUser;
    }

    @Override
    public void setIdAppUser(Long iduser) {
        this.idAppUser = iduser;
    }

    @Override
    public Long getIdCompany() {
        return idCompany;
    }

    @Override
    public void setIdCompany(Long idcompany) {
        this.idCompany = idcompany;
    }

    @Override
    public Map<String, String> getOtherData() {
        return otherData;
    }

    @Override
    public void setOtherData(Map<String, String> otherData) {
        this.otherData = otherData;
    }

    @Override
    public void addOtherDataValue(String key, String value) {
        this.otherData.put(key, value);
    }

    @Override
    public void removeOtherDataValue(String key) {
        this.otherData.remove(key);
    }

    @Override
    public String toString() {
        String result = "idappuser="+nvl(idAppUser,"0").toString().trim()+"\n";
        result += "idcompany="+nvl(idCompany,0).toString().trim()+"\n";
        result += getOtherDataString(otherData);
        return result;
    }
    
    
    protected String getOtherDataString(Map<String, String> data){
        if (data == null){
            return "";
        }
        String result = "";
        for (Map.Entry<String, String> entry : data.entrySet()){
            result += entry.getKey().trim()+"="+entry.getValue().trim()+"\n";
        }
        return result;
    }    
    
}
