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
import jakarta.servlet.http.HttpSession;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.util.Fn;
import org.javabeanstack.web.util.FacesContextUtil;

/**
 * Esta clase se ejecuta en cada petición de un recurso. Verifica que que la
 * persona se haya logeado al sistema.
 *
 * @author Jorge Enciso
 */
public class AuthFilter implements Filter {
    private static final Logger LOGGER = LogManager.getLogger(AuthFilter.class);

    /** Extensiones de los recursos estáticos (ver {@link #hasStaticExtension(String)}). */
    private static final String[] STATIC_EXTENSIONS = {".css", ".js", ".map", ".png",
        ".jpg", ".jpeg", ".gif", ".svg", ".ico", ".webp", ".woff", ".woff2", ".ttf",
        ".eot", ".otf"};

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

    /**
     * Filtra el pedido y, al terminar, cierra la sesión que un
     * {@code FacesContextUtil.logout()} haya marcado durante el pedido.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            filtrar(request, response, chain);
        } finally {
            closeMarkedSession((HttpServletRequest) request);
        }
    }

    private void filtrar(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
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
        if (isResourceNoProtect(urlStr) || isPublicServicePath(rutaEnAplicacion(req))) {
            chain.doFilter(request, response);
            return;
        }
        // El navegador mandó una sesión que ya no existe (venció, se cerró o
        // se redesplegó la aplicación): se calcula antes de crear la nueva.
        boolean sesionPerdida = req.getRequestedSessionId() != null && !req.isRequestedSessionIdValid();
        IUserSession userSession = (IUserSession) req.getSession().getAttribute("userSession");
        // El usuario no está logueado
        if (userSession == null) {
            redirect(req, res, req.getContextPath() + getNoSessionPage(sesionPerdida));
            return;
        }
        // Ingreso a medio hacer: la contraseña se validó pero todavía no se
        // eligió la empresa, así que la sesión no tiene identificador ni está
        // en el pool. Mientras tanto solo se sirve el ingreso (que es público)
        // y los recursos; cualquier otra página va al ingreso, sin cerrar la
        // sesión HTTP para no cortar el ingreso en curso.
        if ((userSession.getSessionId() == null || userSession.getSessionId().isEmpty())
                && !isStaticResource(urlStr)) {
            redirect(req, res, req.getContextPath() + "/login.xhtml");
            return;
        }
        // La sesión del pool (Sessions) tiene su propio reloj de inactividad.
        // Se la renueva con cada pedido que no sea un recurso estático, para que
        // venza junto con la sesión HTTP; si ya venció, el pedido va a la página
        // de sesión vencida en vez de seguir y fallar al acceder a los datos.
        // Va antes del control de acceso: una sesión vencida queda sin usuario.
        if (!isStaticResource(urlStr) && !renewUserSession(req, userSession)) {
            req.getSession().invalidate();
            redirect(req, res, req.getContextPath() + getSessionExpiredPage());
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
        return urlStr.contains("/jakarta.faces.resource/");
    }

    /**
     * Indica si la ruta pedida (relativa al contexto) es la de un servicio
     * público. Reemplaza a las reglas {@code contains("webresources")} y
     * {@code contains("/upload")}, que dejaban pública cualquier dirección que
     * tuviera esos textos en cualquier parte: ahora tienen que ser el primer
     * segmento de la ruta.
     *
     * @param ruta ruta relativa al contexto, en minúsculas (servlet + path info).
     * @return verdadero si no requiere sesión.
     */
    protected boolean isPublicServicePath(String ruta) {
        return isPathUnder(ruta, "/webresources") || isPathUnder(ruta, "/upload");
    }

    /**
     * Indica si una ruta es un prefijo dado o está debajo de él.
     *
     * @param ruta ruta relativa al contexto.
     * @param prefijo prefijo, con la barra inicial y sin la final.
     * @return verdadero si la ruta es el prefijo o empieza con él y una barra.
     */
    public static boolean isPathUnder(String ruta, String prefijo) {
        return ruta != null && (ruta.equals(prefijo) || ruta.startsWith(prefijo + "/"));
    }

    /**
     * Ruta del pedido relativa al contexto (servlet más path info), en
     * minúsculas.
     *
     * @param req petición en curso.
     * @return ruta relativa al contexto.
     */
    private static String rutaEnAplicacion(HttpServletRequest req) {
        String ruta = Fn.nvl(req.getServletPath(), "") + Fn.nvl(req.getPathInfo(), "");
        return ruta.toLowerCase();
    }

    /**
     * Página (relativa al contexto) a la que va un pedido sin sesión del
     * usuario.
     *
     * @param sesionPerdida verdadero si el navegador mandó una sesión que ya
     * no existe (venció, se cerró o se redesplegó la aplicación).
     * @return dirección relativa al contexto, por omisión la del ingreso.
     */
    protected String getNoSessionPage(boolean sesionPerdida) {
        return "/login.xhtml";
    }

    /**
     * Quita del pool de sesiones la sesión que cerró un
     * {@code FacesContextUtil.logout()}. Por omisión no hace nada (el filtro
     * no conoce al administrador de seguridad); el pool la vence solo.
     *
     * @param userSession sesión del usuario que se cerró.
     */
    protected void closeUserSession(IUserSession userSession) {
    }

    /**
     * Cierra, al terminar el pedido, la sesión que marcó
     * {@code FacesContextUtil.logout()}: la quita del pool
     * ({@link #closeUserSession(IUserSession)}) e invalida la sesión HTTP. Se
     * hace acá y no en el cierre mismo porque ese cierre ocurre en mitad del
     * render.
     *
     * @param req petición que terminó.
     */
    private void closeMarkedSession(HttpServletRequest req) {
        Object marca = req.getAttribute(FacesContextUtil.LOGOUT_REQUEST_ATTR);
        if (!(marca instanceof IUserSession)) {
            return;
        }
        req.removeAttribute(FacesContextUtil.LOGOUT_REQUEST_ATTR);
        try {
            closeUserSession((IUserSession) marca);
        } catch (Exception ex) {
            LOGGER.warn("No se pudo quitar la sesión del pool: " + ex.getMessage());
        }
        HttpSession session = req.getSession(false);
        if (session != null) {
            try {
                session.invalidate();
            } catch (IllegalStateException ex) {
                //Ya estaba invalidada.
            }
        }
    }

    /**
     * Renueva la sesión del pool de sesiones ({@code Sessions}) del usuario y
     * dice si sigue vigente.
     *
     * <p>La sesión HTTP se renueva sola con cada pedido; la del pool solo cuando
     * algo la pide por su identificador, que en la práctica es el acceso a
     * datos. Sin esta renovación, una seguidilla de pedidos que no tocan datos
     * mantiene viva la primera y deja vencer la segunda, y el siguiente acceso a
     * datos falla con un error de sesión en lugar de llevar al ingreso.</p>
     *
     * <p>Por omisión no hace nada y devuelve verdadero: el filtro no conoce al
     * administrador de seguridad de la aplicación. Las aplicaciones lo
     * redefinen; tiene que ser una consulta en memoria, porque corre en cada
     * pedido.</p>
     *
     * @param req petición en curso.
     * @param userSession sesión del usuario guardada en la sesión HTTP.
     * @return verdadero si la sesión sigue vigente, falso si venció.
     */
    protected boolean renewUserSession(HttpServletRequest req, IUserSession userSession) {
        return true;
    }

    /**
     * Página (relativa al contexto) a la que va un pedido cuya sesión del pool
     * venció. La sesión HTTP ya se invalidó cuando se llega acá.
     *
     * @return dirección relativa al contexto, por omisión la del ingreso.
     */
    protected String getSessionExpiredPage() {
        return "/login.xhtml";
    }

    /**
     * Indica si el recurso pedido es estático (hojas de estilo, scripts,
     * imágenes, fuentes). Los recursos estáticos siguen exigiendo sesión, pero
     * no renuevan la del pool: no son actividad del usuario.
     *
     * @param urlStr dirección del recurso pedido, en minúsculas y sin
     * parámetros de ruta.
     * @return verdadero si es un recurso estático.
     */
    protected boolean isStaticResource(String urlStr) {
        return hasStaticExtension(urlStr);
    }

    /**
     * Indica si una dirección es la de un recurso estático por su extensión,
     * o un recurso de Faces ({@code jakarta.faces.resource}).
     *
     * @param urlStr dirección en minúsculas; se ignora la consulta.
     * @return verdadero si es un recurso estático.
     */
    public static boolean hasStaticExtension(String urlStr) {
        if (urlStr == null) {
            return false;
        }
        String ruta = urlStr;
        int pos = ruta.indexOf('?');
        if (pos >= 0) {
            ruta = ruta.substring(0, pos);
        }
        if (ruta.contains("/jakarta.faces.resource/")) {
            return true;
        }
        for (String extension : STATIC_EXTENSIONS) {
            if (ruta.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Redirige la petición. Un pedido ajax de Faces no sigue un 302: recibiría
     * el HTML del destino y no podría interpretarlo, así que se le responde
     * con la redirección de Faces ({@code partial-response}).
     *
     * @param req petición en curso.
     * @param res respuesta.
     * @param url dirección de destino.
     * @throws IOException si falla la escritura de la respuesta.
     */
    protected void redirect(HttpServletRequest req, HttpServletResponse res, String url) throws IOException {
        if ("partial/ajax".equals(req.getHeader("Faces-Request"))) {
            res.setContentType("text/xml");
            res.setCharacterEncoding("UTF-8");
            res.setHeader("Cache-Control", "no-cache");
            res.getWriter().write(partialRedirect(url));
            return;
        }
        res.sendRedirect(url);
    }

    /**
     * Respuesta parcial de Faces que redirige a una dirección.
     *
     * @param url dirección de destino.
     * @return documento {@code partial-response} con la redirección.
     */
    public static String partialRedirect(String url) {
        String destino = url.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<partial-response><redirect url=\"" + destino + "\"></redirect></partial-response>";
    }

    private boolean isPageInfo(String page) {
        // AQUI LISTA DE PAGINAS DE INFORMACION (paginanoencontrada.xhtml, rolinsuficiente.xhtml, etc)
        return false;
    }
}
