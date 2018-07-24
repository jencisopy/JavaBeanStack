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
 *
 * @author Jorge Enciso
 */
public class DataUtil {
    private DataUtil(){
    }
    
    public static String getCompanyFilter(IUserSession userSession) {
        return getCompanyFilter(userSession, "");
    }
    
    public static String getCompanyFilter(IUserSession userSession,String alias) {
        alias = (isNullorEmpty(alias)) ? "" : alias + ".";        
        String result = "";
        Long idempresa = userSession.getIdCompany();
        if (nvl(idempresa, 0L) == 0L) {
            return "";
        }
        if (userSession.getCompany() != null && !userSession.getCompany().getCompanyList().isEmpty()) {
            Long idempresaChild;
            for (IAppCompany empresa : userSession.getCompany().getCompanyList()) {
                idempresaChild = empresa.getIdcompany();
                // Si existe una mascara cambiando el idempresa
                if (nvl(empresa.getIdcompanymask(),0L) != 0L){
                    idempresaChild = empresa.getIdcompanymask();
                }
                result += "," + idempresaChild;
            }
            if (!"".equals(nvl(result, ""))) {
                result = alias+"idcompany in (" + idempresa + result + ")";
            }
        } else {
            result = alias+"idcompany = " + idempresa;
        }
        return result;
    }

    public static List<Long> getCompanyList(IUserSession userSession) {
        List<Long> result = new ArrayList();
        Long idempresa = userSession.getIdCompany();
        if (nvl(idempresa, 0L) == 0L) {
            return null;
        }
        result.add(idempresa);
        if (userSession.getCompany() != null && !userSession.getCompany().getCompanyList().isEmpty()) {
            Long idempresaChild;
            for (IAppCompany empresa : userSession.getCompany().getCompanyList()) {
                idempresaChild = empresa.getIdcompany();
                // Si existe una mascara cambiando el idempresa
                if (nvl(empresa.getIdcompanymask(),0L) != 0L){
                    idempresaChild = empresa.getIdcompanymask();
                }
                result.add(idempresaChild);
            }
        }
        return result;
    }

    public static String getCompanyAndPeriodFilter(IUserSession userSession) {    
        return getCompanyAndPeriodFilter(userSession,"");
    }
    
    public static String getCompanyAndPeriodFilter(IUserSession userSession, String alias) {
        String result = "";
        alias = (isNullorEmpty(alias)) ? "" : alias + ".";
        Long idempresa = userSession.getIdCompany();
        Long idperiodo = userSession.getCompany().getIdperiod();
        if (nvl(idempresa, 0L) == 0L) {
            return "";
        }
        result = "(" + alias + "idcompany = " + idempresa + ")";
        if (userSession.getCompany() != null && !userSession.getCompany().getCompanyList().isEmpty()) {
            String separador = " or ";
            for (IAppCompany empresa : userSession.getCompany().getCompanyList()) {
                result += separador;

                idempresa = (nvl(empresa.getIdcompanymask(),0L) != 0L ? empresa.getIdcompanymask() : empresa.getIdcompany());
                // Agregar filtro de empresa si no los tiene
                result += textMerge("({alias}idcompany = {idempresa}", "alias", alias, "idempresa", idempresa);
                result += ")";
            }
            result = "(" + result + ")";
        }
        if (nvl(idperiodo, 0L) != 0L) {
            result = "("+result+" and " + alias + "idperiod=" + idperiodo+")";
        }
        return result;
    }

    public static String getPeriodFilter(IUserSession userSession) {
        return getPeriodFilter(userSession,"");
    }
    
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
