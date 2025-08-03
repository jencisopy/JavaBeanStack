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
package org.javabeanstack.security.model;

import java.util.Map;
import java.util.TreeMap;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.javabeanstack.security.IOAuthConsumerData;
import static org.javabeanstack.util.Fn.nvl;

/**
 * Modelo que transporta datos relacionados a una autenticación o token de tipo oAuth
 * @author Jorge Enciso
 */
@XmlRootElement
public class OAuthConsumerData implements IOAuthConsumerData{
    private String userLogin = "";    
    private String userPass = "";
    private Long idAppUser = 0L;
    private Long idCompany = 0L;
    private boolean administrator = false;
    private Map<String, String> otherData = new TreeMap();

    public OAuthConsumerData(){
    }
    
    /**
     * Devuelve el identificador del usuario
     * @return identificador del usuario
     */
    @Override
    public Long getIdAppUser() {
        return idAppUser;
    }

    /**
     * Asigna el identificador del usuario
     * @param iduser identificador del usuario
     */
    @Override
    public void setIdAppUser(Long iduser) {
        this.idAppUser = iduser;
    }

    /**
     * Devuelve el identificador de la empresa a la que puede acceder
     * @return identificador de la empresa a la que puede acceder
     */
    @Override
    public Long getIdCompany() {
        return idCompany;
    }

    /**
     * Asigna el identificador de la empresa.
     * @param idcompany identificador de la empresa.
     */
    @Override
    public void setIdCompany(Long idcompany) {
        this.idCompany = idcompany;
    }

    /**
     * Devuelve información adicional de autenticación
     * @return información adicional de autenticación
     */
    @Override
    public Map<String, String> getOtherData() {
        return otherData;
    }

    /**
     * Asigna valores adicionales al objeto
     * @param otherData valores adicionales tipo map
     */
    @Override
    public void setOtherData(Map<String, String> otherData) {
        this.otherData = otherData;
    }

    /**
     * Agrega información adicional 
     * @param key clave
     * @param value valor
     */
    @Override
    public void addOtherDataValue(String key, String value) {
        this.otherData.put(key, value);
    }

    /**
     * Remueve una clave del campo de valores adicionales.
     * @param key clave
     */
    @Override
    public void removeOtherDataValue(String key) {
        this.otherData.remove(key);
    }

    @Override
    public String toString() {
        String result = "idappuser="+nvl(idAppUser,"0").toString().trim()+"\n";
        result += "idcompany="+nvl(idCompany,0).toString().trim()+"\n";
        result += "administrator="+nvl(administrator,false).toString().trim()+"\n";
        result += "userlogin="+nvl(userLogin,"").trim()+"\n";
        result += getOtherDataString(otherData);
        return result;
    }

    /**
     * Devuelve el login o codigo del usuario.
     * @return login del usuario
     */
    @Override    
    public String getUserLogin() {
        return userLogin;
    }

    /**
     * Asigna el login o codigo del usuario.
     * @param userLogin codigo o login del usuario.
     */
    @Override    
    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }

    /**
     * Devuelve el password del usuario
     * @return password del usuario
     */
    @Override    
    public String getUserPass() {
        return userPass;
    }

    /**
     * Asigna el password del usuario
     * @param userPass password del usuario
     */
    @Override    
    public void setUserPass(String userPass) {
        this.userPass = userPass;
    }
    
    /**
     * Devuelve datos adicionales en formato String
     * @param data map con los datos adicionales que se convertiran a string.
     * @return datos adicionales en formato String
     */
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

    /**
     * Devuelve si esta autenticación permite realizar tareas administrativas
     * @return si esta autenticación permite realizar tareas administrativas
     */
    @Override
    public boolean isAdministrator() {
        return administrator;
    }

    /**
     * Asigna verdadero o falso si este token permitira permisos de tipo administrativo.
     * @param value verdadero o falso.
     */
    @Override
    public void setAdministrator(boolean value) {
        administrator = value;
    }
}
