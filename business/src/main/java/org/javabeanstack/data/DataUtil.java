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

package org.javabeanstack.data;

import java.util.ArrayList;
import java.util.List;
import org.javabeanstack.security.IUserSession;
import org.javabeanstack.model.IAppCompany;
import static org.javabeanstack.util.Fn.nvl;
import static org.javabeanstack.util.Strings.isNullorEmpty;
import static org.javabeanstack.util.Strings.textMerge;


/**
 * Clases utiles para filtrado de datos y otros.
 * 
 * @author Jorge Enciso
 */
public class DataUtil {
    private DataUtil(){
    }
   
    /**
     * Devuelve un filtro correspondiente a la empresa logueada que será utilizado
     * en los selects de datos.
     * 
     * @param userSession sesión del usuario
     * @return un filtro correspondiente a la empresa logueada
     */
    public static String getCompanyFilter(IUserSession userSession) {
        return getCompanyFilter(userSession, "");
    }
    
    /**
     * Devuelve un filtro correspondiente a la empresa logueada que será utilizado
     * en los selects de datos.
     * 
     * @param userSession sesión del usuario
     * @param alias alias de la tabla
     * @return un filtro correspondiente a la empresa logueada
     */
    public static String getCompanyFilter(IUserSession userSession,String alias) {
        alias = (isNullorEmpty(alias)) ? "" : alias + ".";        
        String result = "";
        Long idcompany = userSession.getIdCompany();
        if (nvl(idcompany, 0L) == 0L) {
            return "";
        }
        if (userSession.getCompany() != null && !userSession.getCompany().getCompanyList().isEmpty()) {
            Long idcompanyChild;
            for (IAppCompany company : userSession.getCompany().getCompanyList()) {
                idcompanyChild = company.getIdcompany();
                // Si existe una mascara cambiando el idcompany
                if (nvl(company.getIdcompanymask(),0L) != 0L){
                    idcompanyChild = company.getIdcompanymask();
                }
                result += "," + idcompanyChild;
            }
            if (!"".equals(nvl(result, ""))) {
                result = alias+"idcompany in (" + idcompany + result + ")";
            }
        } else {
            result = alias+"idcompany = " + idcompany;
        }
        return result;
    }

    /**
     * En caso de que la empresa a la que se logueo el usuario sea el padre
     * de un grupo de empresas, devuelve una lista de empresas pertenecientes a
     * ese grupo.
     * @param userSession sesión del usuario.
     * @return lista de empresas pertenecientes al grupo
     */
    public static List<Long> getCompanyList(IUserSession userSession) {
        List<Long> result = new ArrayList();
        Long idcompany = userSession.getIdCompany();
        if (nvl(idcompany, 0L) == 0L) {
            return null;
        }
        result.add(idcompany);
        if (userSession.getCompany() != null && !userSession.getCompany().getCompanyList().isEmpty()) {
            Long idcompanyChild;
            for (IAppCompany company : userSession.getCompany().getCompanyList()) {
                idcompanyChild = company.getIdcompany();
                // Si existe una mascara cambiando el idcompany
                if (nvl(company.getIdcompanymask(),0L) != 0L){
                    idcompanyChild = company.getIdcompanymask();
                }
                result.add(idcompanyChild);
            }
        }
        return result;
    }

    /**
     * Filtro de empresa logueada más el periodo activo, datos a ser utilizado en los selects
     * @param userSession sesión del usuario.
     * @return Filtro de empresa logueada más el periodo activo
     */
    public static String getCompanyAndPeriodFilter(IUserSession userSession) {    
        return getCompanyAndPeriodFilter(userSession,"");
    }
    
    /**
     * Filtro de empresa logueada más el periodo activo, datos a ser utilizado en los selects
     * @param userSession sesión del usuario.
     * @param alias alias de la tabla
     * @return Filtro de empresa logueada más el periodo activo
     */
    public static String getCompanyAndPeriodFilter(IUserSession userSession, String alias) {
        String result = "";
        alias = (isNullorEmpty(alias)) ? "" : alias + ".";
        Long idcompany = userSession.getIdCompany();
        Long idperiodo = userSession.getCompany().getIdperiod();
        if (nvl(idcompany, 0L) == 0L) {
            return "";
        }
        result = "(" + alias + "idcompany = " + idcompany + ")";
        if (userSession.getCompany() != null && !userSession.getCompany().getCompanyList().isEmpty()) {
            String separador = " or ";
            for (IAppCompany company : userSession.getCompany().getCompanyList()) {
                result += separador;

                idcompany = (nvl(company.getIdcompanymask(),0L) != 0L ? company.getIdcompanymask() : company.getIdcompany());
                // Agregar filtro de empresa si no los tiene
                result += textMerge("({alias}idcompany = {idempresa}", "alias", alias, "idempresa", idcompany);
                result += ")";
            }
            result = "(" + result + ")";
        }
        if (nvl(idperiodo, 0L) != 0L) {
            result = "("+result+" and " + alias + "idperiod=" + idperiodo+")";
        }
        return result;
    }

    /**
     * Devuelve filtro del periodo activo de la empresa logueada
     * @param userSession sesión del usuario
     * @return filtro del periodo activo de la empresa logueada
     */
    public static String getPeriodFilter(IUserSession userSession) {
        return getPeriodFilter(userSession,"");
    }

    /**
     * Devuelve filtro del periodo activo de la empresa logueada
     * @param userSession sesión del usuario
     * @param alias alias de la tabla
     * @return filtro del periodo activo de la empresa logueada
     */
    public static String getPeriodFilter(IUserSession userSession, String alias) {
        String result;
        alias = (isNullorEmpty(alias)) ? "" : alias + ".";
        Long idperiodo = userSession.getCompany().getIdperiod();
        if (nvl(idperiodo, 0L) == 0L) {
            idperiodo = 1L;
        }
        result = alias+"idperiod = " + idperiodo;        
        return result;
    }

    /**
     * Devuelve el id de periodo activo de la empresa logueada.
     * @param userSession sesión del usuario.
     * @return id de periodo activo de la empresa logueada.
     */
    public static Long getCompanyIdPeriod(IUserSession userSession) {
        Long result = 1L;
        Long idperiodo = userSession.getCompany().getIdperiod();
        if (nvl(idperiodo, 0L) != 0L) {
            result = idperiodo;
        }
        return result;
    }
    
    public static String getDateTimeType(String engine){
        if (engine.equals("ORACLE")){
            return "DATE";
        }
        return "DATETIME";
    }
}
