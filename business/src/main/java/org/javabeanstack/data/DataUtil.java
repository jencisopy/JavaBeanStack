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

import org.javabeanstack.model.IEmpresa;
import org.javabeanstack.security.IUserSession;
import static org.javabeanstack.util.Fn.nvl;
import static org.javabeanstack.util.Strings.isNullorEmpty;
import static org.javabeanstack.util.Strings.textMerge;

/**
 *
 * @author Jorge Enciso
 */
public class DataUtil {
    public static String getEmpresaFilter(IUserSession userSession) {
        return getEmpresaFilter(userSession, "");
    }
    
    public static String getEmpresaFilter(IUserSession userSession,String alias) {
        alias = (isNullorEmpty(alias)) ? "" : alias + ".";        
        String result = "";
        Long idempresa = userSession.getIdEmpresa();
        if (nvl(idempresa, 0L) == 0L) {
            return "";
        }
        if (userSession.getEmpresa() != null && !userSession.getEmpresa().getEmpresaList().isEmpty()) {
            Long idempresaChild;
            for (IEmpresa empresa : userSession.getEmpresa().getEmpresaList()) {
                idempresaChild = empresa.getIdempresa();
                // Si existe una mascara cambiando el idempresa
                if (nvl(empresa.getIdempresamask(),0L) != 0L){
                    idempresaChild = empresa.getIdempresamask();
                }
                result += "," + idempresaChild;
            }
            if (!"".equals(nvl(result, ""))) {
                result = alias+"idempresa in (" + idempresa + result + ")";
            }
        } else {
            result = alias+"idempresa = " + idempresa;
        }
        return result;
    }

    public static String getEmpresaAndPeriodoFilter(IUserSession userSession) {    
        return getEmpresaAndPeriodoFilter(userSession,"");
    }
    
    public static String getEmpresaAndPeriodoFilter(IUserSession userSession, String alias) {
        String result = "";
        alias = (isNullorEmpty(alias)) ? "" : alias + ".";
        Long idempresa = userSession.getIdEmpresa();
        Long idperiodo = userSession.getEmpresa().getIdperiodo();
        if (nvl(idempresa, 0L) == 0L) {
            return "";
        }
        result = "(" + alias + "idempresa = " + idempresa + ")";
        if (userSession.getEmpresa() != null && !userSession.getEmpresa().getEmpresaList().isEmpty()) {
            String separador = " or ";
            for (IEmpresa empresa : userSession.getEmpresa().getEmpresaList()) {
                result += separador;

                idempresa = (nvl(empresa.getIdempresamask(),0L) != 0L ? empresa.getIdempresamask() : empresa.getIdempresa());
                // Agregar filtro de empresa si no los tiene
                result += textMerge("({alias}idempresa = {idempresa}", "alias", alias, "idempresa", idempresa);
                result += ")";
            }
            result = "(" + result + ")";
        }
        if (nvl(idperiodo, 0L) != 0L) {
            result = "("+result+" and " + alias + "idperiodo=" + idperiodo+")";
        }
        return result;
    }

    public static String getPeriodoFilter(IUserSession userSession) {
        return getPeriodoFilter(userSession,"");
    }
    
    public static String getPeriodoFilter(IUserSession userSession, String alias) {
        String result = "";
        alias = (isNullorEmpty(alias)) ? "" : alias + ".";
        Long idperiodo = userSession.getEmpresa().getIdperiodo();
        if (nvl(idperiodo, 0L) != 0L) {
            result = alias+"idperiodo = " + idperiodo;
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
