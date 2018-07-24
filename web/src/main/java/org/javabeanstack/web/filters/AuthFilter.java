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
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

import org.javabeanstack.security.IUserSession;


/**
 * Esta clase se ejecuta en cada petición de un recurso. Verifica que 
 * que la persona se haya logeado al sistema.
 * @author Jorge Enciso
 */
public class AuthFilter implements Filter { 
    private static final Logger LOGGER = Logger.getLogger(AuthFilter.class); 
      
    @Override
    public void init(FilterConfig filterConfig) throws ServletException { 
    } 
  
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException { 
        LOGGER.debug("IN"); 
                  
        HttpServletRequest req = (HttpServletRequest) request; 
        HttpServletResponse res = (HttpServletResponse) response; 
  
        IUserSession userSession = (IUserSession)req.getSession().getAttribute("userSession"); 
        String urlStr = req.getRequestURL().toString().toLowerCase(); 
          
        if (isResourceNoProtect(urlStr)) { 
            chain.doFilter(request, response); 
            return; 
        } 
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
          
        String currentPage = req.getServletPath().toLowerCase(); 
          
        // Eliminar la barra del comienzo (viene en formato /index.xhtml)  
        if(currentPage.startsWith("/")) { 
            currentPage = currentPage.replace("/", ""); 
        } 
          
        // Substraer hasta el punto (index.xhtml => index) 
        int idx = currentPage.indexOf("."); 
        if(idx != -1) { 
            currentPage = currentPage.substring(0, idx); 
        } 
  
        if(!currentPage.isEmpty()) { 
            // Seguir con el flujo normal de la petición
            chain.doFilter(request, response);
        } else {
            res.sendRedirect(req.getContextPath() + "/paginanoencontrada.xhtml"); 
        }
 
    } 
  
    @Override
    public void destroy() { 
          
    } 
  
    private boolean isResourceNoProtect(String urlStr) { 
        // AQUI LISTA DE RECURSO QUE NO REQUIERE PROTECCION 
        if (urlStr.endsWith("login.xhtml")) { 
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
        return urlStr.contains("/javax.faces.resource/"); 
    } 
      
    private boolean isPageInfo(String page){ 
          // AQUI LISTA DE PAGINAS DE INFORMACION (paginanoencontrada.xhtml, rolinsuficiente.xhtml, etc)
        return false;
    } 
} 