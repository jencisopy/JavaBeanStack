/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2018 - 2027 Jorge Enciso
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

import org.javabeanstack.security.AppAccessPolicy;
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

    /**
     * Constructor por defecto.
     */
    public AuthFilter() {
        this.ipRequestAllowed = new String[]{"0.0.0.0"};
    }

    /**
     * Inicializa el filtro de autenticación.
     */
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
        String urlStr = quitarParametrosDeRuta(req.getRequestURL().toString().toLowerCase());
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

        // Registrar el nombre de la aplicación en la sesión: acá se conoce el
        // context path, y la capa de datos lo necesita para evaluar el WRITE.
        userSession.addInfo(AppAccessPolicy.APPNAME, req.getContextPath());

        // Verificar que el rol del usuario tenga acceso a esta aplicación.
        // Es el único punto por el que pasan todas las peticiones, con y sin
        // token, de modo que la política no depende del camino de autenticación.
        if (!AppAccessPolicy.isAllowed(appConfig, req.getContextPath(),
                AppAccessPolicy.ACCESS, userSession.getUser())) {
            LOGGER.info("Acceso denegado a " + req.getContextPath()
                    + " para el usuario " + userSession.getUser().getLogin()
                    + " (rol " + userSession.getUser().getRol() + ")");
            req.getSession().invalidate();
            res.sendRedirect(req.getContextPath() + "/noautorizado.xhtml");
            return;
        }

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
    
    

    /**
     * Quita los parametros de ruta de una direccion, es decir todo lo que sigue
     * al primer punto y coma.
     *
     * <p>Hace falta porque la lista blanca de recursos publicos compara el final
     * de la direccion, y {@code getRequestURL()} devuelve los parametros de ruta
     * pegados al nombre del archivo. El caso real es la PRIMERA visita: mientras
     * el servidor no recibio ninguna cookie de vuelta no sabe si el navegador
     * las acepta, asi que ademas de mandarla reescribe las direcciones que la
     * aplicacion genera y el formulario sale con
     * {@code action="login.xhtml;jsessionid=..."}. Contra esa direccion
     * {@code endsWith("login.xhtml")} da falso, la pagina publica se toma por
     * protegida y la peticion termina redirigida al login: el ingreso no
     * reacciona hasta que se recarga la pagina, y lo mismo les pasa a
     * verificarcorreo, recuperarclave y restablecerclave, que se abren desde un
     * enlace del correo y son justamente las que llegan sin sesion previa.
     *
     * <p>Normalizar antes de comparar cierra ademas la puerta a que una
     * direccion decorada cambie una decision de autorizacion, que es algo que
     * no deberia depender de un adorno de la ruta.
     *
     * @param urlStr direccion pedida.
     * @return la direccion sin parametros de ruta.
     */
    private String quitarParametrosDeRuta(String urlStr) {
        int pos = urlStr.indexOf(';');
        return (pos < 0) ? urlStr : urlStr.substring(0, pos);
    }

    /**
     * Verifica si la dirección desde donde llega la petición tiene
     * autorización, según los parámetros {@code IP_REQUEST_ALLOWED} e
     * {@code IP_REQUEST_NOT_ALLOWED} del sistema.
     *
     * <p>La lógica de coincidencia —comodines, listas y ceros a la derecha—
     * vive en {@link Fn#ipMatchPattern(String, String)}. Estaba escrita acá,
     * duplicada palabra por palabra entre las dos listas, y de esa forma no
     * podía reutilizarse; el control de ingreso por usuario necesitaba la
     * misma regla y habría sido una tercera copia.</p>
     *
     * <p>Las dos listas se consultan con métodos distintos <b>a propósito</b>,
     * porque la lista vacía significa lo contrario en cada una: sin permitidos
     * declarados pasan todos, sin denegados declarados no se rechaza a nadie.</p>
     *
     * @param ipRequest dirección desde donde llega la petición.
     * @return verdadero si la petición está autorizada.
     */
    private boolean requestAllowed(String ipRequest) {
        //IPs allowed (si no hay ninguna declarada, todas estan permitidas)
        if (!Fn.ipMatchAny(ipRequest, ipRequestAllowed)) {
            logAccessNoAllowed(ipRequest);
            return false;
        }
        //IPs not allowed (si no hay ninguna declarada, no se deniega a nadie)
        if (Fn.ipListed(ipRequest, ipRequestNotAllowed)) {
            logAccessNoAllowed(ipRequest);
            return false;
        }
        return true;
    }

    //Implementar en las clases derivadas.
    /**
     * Registra en el log un intento de acceso no autorizado.
     *
     * @param remoteIP dirección IP del cliente.
     */
    protected void logAccessNoAllowed(String remoteIP){
    }
    
    @Override
    public void destroy() {

    }

    /**
     * Indica si el recurso pedido puede servirse sin sesión iniciada.
     *
     * <p>Se evalúa <b>antes</b> de exigir la sesión, así que lo que devuelva
     * verdadero queda fuera del control de acceso. Las aplicaciones que tengan
     * páginas públicas propias —una activación por enlace, una recuperación de
     * contraseña— redefinen este método y llaman a {@code super} para conservar
     * las de acá.</p>
     *
     * @param urlStr dirección del recurso pedido.
     * @return verdadero si el recurso no requiere sesión.
     */
    protected boolean isResourceNoProtect(String urlStr) {
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
