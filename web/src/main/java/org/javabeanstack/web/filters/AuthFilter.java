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
package org.javabeanstack.web.filters;

import java.io.IOException;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javabeanstack.config.IAppConfig;
import org.javabeanstack.io.IOUtil;

import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.util.Fn;

/**
 * Esta clase se ejecuta en cada petición de un recurso. Verifica que que la
 * persona se haya logeado al sistema.
 *
 * @author Jorge Enciso
 */
public class AuthFilter implements Filter {
    private static final Logger LOGGER = LogManager.getLogger(AuthFilter.class);

    @EJB
    private IAppConfig appConfig;
    
    private String[] ipRequestAllowed;
    private String[] ipRequestNotAllowed;

    public AuthFilter() {
        this.ipRequestAllowed = new String[]{"0.0.0.0"};
    }

    @PostConstruct
    public void init() {
        //Lista de IPs permitidos (si el valor es 0.0.0.0 todos estan permitidos)
        if (appConfig.getSystemParam("IP_REQUEST_ALLOWED") != null) {
            String value = Fn.nvl((String) appConfig.getSystemParam("IP_REQUEST_ALLOWED").getValue(), "0.0.0.0");
            if (!value.isEmpty()) {
                ipRequestAllowed = Fn.nvl((String) appConfig.getSystemParam("IP_REQUEST_ALLOWED").getValue(), "0.0.0.0").split(",");
            }
        }
        //Lista de IPs no permitidos
        if (appConfig.getSystemParam("IP_REQUEST_NOT_ALLOWED") != null) {
            String value = Fn.nvl((String) appConfig.getSystemParam("IP_REQUEST_NOT_ALLOWED").getValue(), "");
            if (!value.isEmpty()) {
                ipRequestNotAllowed = Fn.nvl((String) appConfig.getSystemParam("IP_REQUEST_NOT_ALLOWED").getValue(), "").split(",");
            }
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        LOGGER.debug("IN");

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String remoteAddr = request.getRemoteAddr();
        String urlStr = req.getRequestURL().toString().toLowerCase();
        //Si el recurso solicitado es la pagina noautorizado
        if (urlStr.endsWith("noautorizado.xhtml")) {
            chain.doFilter(request, response);
            return;
        }
        //Verificar si el IP que hace la petición tiene autorización 
        if (!requestAllowed(remoteAddr)) {
            res.sendRedirect(req.getContextPath() + "/noautorizado.xhtml");
            return;
        }
        if (isResourceNoProtect(urlStr)) {
            chain.doFilter(request, response);
            return;
        }
        IUserSession userSession = (IUserSession) req.getSession().getAttribute("userSession");
        // El usuario no está logueado 
        if (userSession == null) {
            res.sendRedirect(req.getContextPath() + "/login.xhtml");
            return;
        }
        //--  A partir de aca se ejecuta sólo si el usuario ya esta logueado 

        // Verificar si la página que se abre es una página de información 
        if (isPageInfo(urlStr)) {
            chain.doFilter(request, response);
            return;
        }

        String currentPage = IOUtil.getFileBaseName(req.getServletPath().toLowerCase());

        if (!currentPage.isEmpty()) {
            // Seguir con el flujo normal de la petición
            chain.doFilter(request, response);
        } else {
            res.sendRedirect(req.getContextPath() + "/404.xhtml");
        }
    }
    
    

    private boolean requestAllowed(String ipRequest) {
        //IPs allowed
        boolean allowed = false;
        for (String ip : ipRequestAllowed) {
            //Si el ip del cliente es igual a una de las lista de ips habilitados
            if (ipRequest.equals(ip.trim())) {
                allowed = true;
                break;
            }
            //Todos los ips estan habilitados.
            if (Fn.inList(ip.trim(), "0.0.0.0", "*")) {
                allowed = true;
                break;
            }
            String[] partes1 = ip.trim().split("\\.");
            String[] partes2 = ipRequest.split("\\.");
            boolean esComodin = true;
            for (int i = partes1.length - 1; i >= 0; i--) {
                if (partes1[i].equals("*")) {
                    continue;
                }
                if (esComodin && partes1[i].equals("0")) {
                    continue;
                }
                esComodin = false;
                if (!partes1[i].equals(partes2[i])) {
                    break;
                }
                if (i == 0) {
                    allowed = true;
                }
            }
            if (allowed) {
                break;
            }
        }
        //Si no esta permido registrar en el log del sistema
        if (!allowed) {
            logAccessNoAllowed(ipRequest);
            return false;
        }
        //IPs not allowed.
        if (ipRequestNotAllowed != null) {
            for (String ip : ipRequestNotAllowed) {
                //Si el ip del cliente es igual a una de las lista de ips no habilitada
                if (ipRequest.equals(ip.trim())) {
                    allowed = false;
                    break;
                }
                //Todos los ips no estan habilitados.
                if (Fn.inList(ip.trim(), "0.0.0.0", "*")) {
                    allowed = false;
                    break;
                }
                String[] partes1 = ip.trim().split("\\.");
                String[] partes2 = ipRequest.split("\\.");
                boolean esComodin = true;
                for (int i = partes1.length - 1; i >= 0; i--) {
                    if (partes1[i].equals("*")) {
                        continue;
                    }
                    if (esComodin && partes1[i].equals("0")) {
                        continue;
                    }
                    esComodin = false;
                    if (!partes1[i].equals(partes2[i])) {
                        break;
                    }
                    if (i == 0) {
                        allowed = false;
                    }
                }
                if (!allowed) {
                    break;
                }
            }
        }
        //Si no esta permido registrar en el log del sistema
        if (!allowed){
            logAccessNoAllowed(ipRequest);            
        }
        return allowed;
    }

    //Implementar en las clases derivadas.
    protected void logAccessNoAllowed(String remoteIP){
    }
    
    @Override
    public void destroy() {

    }

    private boolean isResourceNoProtect(String urlStr) {
        // AQUI LISTA DE RECURSO QUE NO REQUIERE PROTECCION 
        if (urlStr.endsWith("login.xhtml")) {
            return true;
        }
        if (urlStr.endsWith("noautorizado.xhtml")) {
            return true;
        }
        if (urlStr.endsWith("404.xhtml")) {
            return true;
        }
        if (urlStr.endsWith("cambiarclave.xhtml")) {
            return true;
        }
        if (urlStr.contains("webresources")) {
            return true;
        }
        if (urlStr.contains("/upload")) {
            return true;
        }
        return urlStr.contains("/jakarta.faces.resource/");
    }

    private boolean isPageInfo(String page) {
        // AQUI LISTA DE PAGINAS DE INFORMACION (paginanoencontrada.xhtml, rolinsuficiente.xhtml, etc)
        return false;
    }
}
