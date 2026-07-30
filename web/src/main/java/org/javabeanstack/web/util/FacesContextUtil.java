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
package org.javabeanstack.web.util;

import java.util.Iterator;
import java.util.Map;
import jakarta.faces.application.Application;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.application.ViewHandler;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.logging.log4j.Logger;

import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.error.IErrorReg;

import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.util.Fn;
import org.javabeanstack.util.Strings;
import org.primefaces.PrimeFaces;

/**
 * Utilidad sobre el {@link jakarta.faces.context.FacesContext}: acceso
 * simplificado al contexto JSF (request, sesión, atributos), despliegue de
 * mensajes (error, info, advertencia) y navegación.
 *
 * @author Jorge Enciso
 */
public class FacesContextUtil {
    private String messageView;

    /**
     * Devuelve el mensaje asociado a la vista.
     *
     * @return mensaje de la vista.
     */
    public String getMessageView() {
        return messageView;
    }

    /**
     * Asigna el mensaje asociado a la vista.
     *
     * @param messageView mensaje de la vista.
     */
    public void setMessageView(String messageView) {
        this.messageView = messageView;
    }

    /**
     * Devuelve el {@link FacesContext} actual.
     *
     * @return contexto JSF.
     */
    public FacesContext getFacesContext() {
        return FacesContext.getCurrentInstance();
    }

    /**
     * Devuelve el objeto {@code Application} de JSF.
     *
     * @return aplicación JSF.
     */
    public Application getApplication() {
        return getFacesContext().getApplication();
    }

    /**
     * Devuelve el contexto de request de PrimeFaces.
     *
     * @return contexto de PrimeFaces.
     */
    public PrimeFaces getRequestContext() {
        return PrimeFaces.current();
    }

    /**
     * Devuelve el {@code ExternalContext} de JSF.
     *
     * @return contexto externo.
     */
    public ExternalContext getExternalContext() {
        return getFacesContext().getExternalContext();
    }

    /**
     * Devuelve la ruta de contexto del request.
     *
     * @return ruta de contexto.
     */
    public String getRequestContextPath() {
        return getExternalContext().getRequestContextPath();
    }

    /**
     * Devuelve la ruta real en el sistema de archivos de un path web.
     *
     * @param path ruta web.
     * @return ruta real en el filesystem.
     */
    public String getRealPath(String path) {
        return getExternalContext().getRealPath(path);
    }

    /**
     * Devuelve el {@code UIViewRoot} de la vista actual.
     *
     * @return raíz de la vista.
     */
    public UIViewRoot getUIViewRoot() {
        ViewHandler viewHandler = getApplication().getViewHandler();
        return viewHandler.createView(getFacesContext(), getFacesContext().getViewRoot().getViewId());
    }

    /**
     * Devuelve la {@code HttpSession} actual.
     *
     * @return sesión HTTP.
     */
    public HttpSession getSession() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest req = (HttpServletRequest) context.getExternalContext().getRequest();
        HttpSession session = req.getSession();
        return session;
    }

    /**
     * Devuelve el mapa de atributos de sesión.
     *
     * @return mapa de sesión.
     */
    public Map<String, Object> getSessionMap() {
        return FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
    }

    /**
     * Devuelve el mapa de atributos del request.
     *
     * @return mapa de request.
     */
    public Map<String, Object> getRequestMap() {
        return FacesContext.getCurrentInstance().getExternalContext().getRequestMap();
    }

    /**
     * Devuelve el mapa de parámetros del request.
     *
     * @return parámetros del request.
     */
    public Map<String, String> getRequestParameterMap() {
        return FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
    }

    /**
     * Devuelve el mapa de atributos del {@link FacesContext} actual.
     *
     * @param key parámetro no utilizado (se conserva por compatibilidad de firma).
     * @return mapa de atributos del contexto.
     */
    public Map<Object, Object> getAttributes(String key) {
        return FacesContext.getCurrentInstance().getAttributes();
    }

    /**
     * Asigna un atributo en el contexto.
     *
     * @param key clave.
     * @param value valor.
     */
    public void setAttribute(Object key, Object value) {
        FacesContext.getCurrentInstance().getAttributes().put(key, value);
    }

    /**
     * Refresca (recarga) la vista actual.
     */
    public void refreshView() {
        FacesContext context = getFacesContext();
        Application application = getApplication();
        ViewHandler viewHandler = application.getViewHandler();
        UIViewRoot viewRoot = viewHandler.createView(context, context.getViewRoot().getViewId());
        context.setViewRoot(viewRoot);
    }

    /**
     * Refresca un componente de la vista.
     *
     * @param idcomponent identificador del componente.
     */
    public void refreshView(String idcomponent) {
        FacesContext context = FacesContext.getCurrentInstance();
        UIComponent component = this.findComponent(idcomponent);
        if (component != null) {
            idcomponent = component.getClientId();
            context.getPartialViewContext().getRenderIds().add(idcomponent);
        }
    }

    /**
     * Muestra un mensaje de error a partir de una excepción y la registra en el log.
     *
     * @param exception excepción.
     * @param logger logger donde registrar.
     */
    public void showError(Exception exception, Logger logger) {
        ErrorManager.showError(exception, logger);
        //Mostrar la causa raíz (ej. el error SQL) y no el wrapper EJB/JPA
        String mensaje = ErrorManager.getRootCauseMessage(exception);
        if (mensaje.isEmpty()) {
            mensaje = exception.getMessage();
        }
        showError("Error", mensaje);
    }
    
    /**
     * Muestra un mensaje de error.
     *
     * @param message mensaje de error.
     */
    public void showError(String message) {
        showError("Error", message);
    }

    /**
     * Muestra un mensaje de error con título.
     *
     * @param title título.
     * @param message mensaje de error.
     */
    public void showError(String title, String message) {
        getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, title, message));
        if (!Strings.isNullorEmpty(messageView)) {
            refreshView(messageView);
        }
    }

    /**
     * Muestra un mensaje de error asociado a un componente.
     *
     * @param title título.
     * @param message mensaje de error.
     * @param clientId identificador del componente.
     */
    public void showError(String title, String message, String clientId) {
        getFacesContext().addMessage(clientId, new FacesMessage(FacesMessage.SEVERITY_ERROR, title, message));
        if (!Strings.isNullorEmpty(messageView)) {
            refreshView(messageView);
        }
    }

    /**
     * Muestra los errores contenidos en un mapa.
     *
     * @param title título.
     * @param errors mapa de errores por campo.
     */
    public void showError(String title, Map<String, IErrorReg> errors) {
        if (errors != null && !errors.isEmpty()) {
            Iterator iterator = errors.keySet().iterator();
            String key;
            while (iterator.hasNext()) {
                key = (String) iterator.next();
                if (!errors.get(key).isWarning()) {
                    String titleShow = Fn.nvl(title, "Error ") + " en " + key;
                    getFacesContext().addMessage(key, new FacesMessage(FacesMessage.SEVERITY_ERROR, titleShow, errors.get(key).getMessage()));
                }
            }
            if (!Strings.isNullorEmpty(messageView)) {
                refreshView(messageView);
            }
        }
    }

    /**
     * Muestra un mensaje informativo.
     *
     * @param message mensaje.
     */
    public void showInfo(String message) {
        showInfo("Información", message);
    }

    /**
     * Muestra un mensaje informativo con título.
     *
     * @param title título.
     * @param message mensaje.
     */
    public void showInfo(String title, String message) {
        getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, title, message));
        if (!Strings.isNullorEmpty(messageView)) {
            refreshView(messageView);
        }
    }

    /**
     * Muestra un mensaje de advertencia.
     *
     * @param message mensaje.
     */
    public void showWarn(String message) {
        showWarn("Aviso", message);
    }

    /**
     * Muestra un mensaje de advertencia con título.
     *
     * @param title título.
     * @param message mensaje.
     */
    public void showWarn(String title, String message) {
        getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, title, message));
        if (!Strings.isNullorEmpty(messageView)) {
            refreshView(messageView);
        }
    }

    /**
     * Muestra un mensaje de advertencia asociado a un componente.
     *
     * @param title título.
     * @param message mensaje.
     * @param clientId identificador del componente.
     */
    public void showWarn(String title, String message, String clientId) {
        getFacesContext().addMessage(clientId, new FacesMessage(FacesMessage.SEVERITY_WARN, title, message));
        if (!Strings.isNullorEmpty(messageView)) {
            refreshView(messageView);
        }
    }

    /**
     * Muestra las advertencias contenidas en un mapa.
     *
     * @param title título.
     * @param errors mapa de advertencias por campo.
     */
    public void showWarn(String title, Map<String, IErrorReg> errors) {
        if (errors != null && !errors.isEmpty()) {
            Iterator iterator = errors.keySet().iterator();
            String key;
            while (iterator.hasNext()) {
                key = (String) iterator.next();
                if (errors.get(key).isWarning()) {
                    String titleShow = Fn.nvl(title, "Advertencia ") + " en " + key;
                    getFacesContext().addMessage(key, new FacesMessage(FacesMessage.SEVERITY_WARN, titleShow, errors.get(key).getMessage()));
                }
            }
            if (!Strings.isNullorEmpty(messageView)) {
                refreshView(messageView);
            }
        }
    }

    /**
     * Devuelve la dirección IP del cliente.
     *
     * @return dirección IP.
     */
    public String getIp() {
        HttpServletRequest request = (HttpServletRequest) getFacesContext().getExternalContext().getRequest();
        String ip = request.getHeader("X-FORWARDED-FOR");
        if (ip == null) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * Devuelve la URL base de la aplicación derivada del pedido en curso, por
     * ejemplo {@code https://erp.cliente.com.py/Maker-web}.
     *
     * <p><b>ADVERTENCIA DE SEGURIDAD.</b> El host sale del encabezado
     * {@code Host}, que lo controla <b>quien envía la petición</b>. Una URL
     * armada con esto solo es confiable en flujos <b>autenticados</b>, donde
     * quien la dispara ya es el dueño de la cuenta y no gana nada envenenando
     * su propio enlace. <b>Nunca</b> debe usarse para armar enlaces de flujos
     * anónimos —recuperación de contraseña, reenvíos públicos—: ahí permitiría
     * que un tercero haga llegar a la casilla legítima de la víctima un enlace
     * que apunta a su propio servidor, con un token válido.</p>
     *
     * <p>Detrás de un proxy inverso el pedido trae el host interno, así que
     * esta derivación es un <b>último recurso</b>: lo correcto es configurar la
     * URL base de la instalación.</p>
     *
     * @return URL base sin barra final, o nulo si no hay pedido web en curso.
     */
    public String getBaseUrl() {
        FacesContext context = getFacesContext();
        if (context == null || context.getExternalContext() == null) {
            return null;
        }
        Object pedido = context.getExternalContext().getRequest();
        if (!(pedido instanceof HttpServletRequest)) {
            return null;
        }
        HttpServletRequest request = (HttpServletRequest) pedido;
        String esquema = request.getScheme();
        int puerto = request.getServerPort();
        StringBuilder url = new StringBuilder(esquema).append("://").append(request.getServerName());
        boolean puertoImplicito = ("http".equals(esquema) && puerto == 80)
                || ("https".equals(esquema) && puerto == 443);
        if (!puertoImplicito) {
            url.append(":").append(puerto);
        }
        return url.append(request.getContextPath()).toString();
    }

    /**
     * Devuelve el host del cliente.
     *
     * @return host.
     */
    public String getHost() {
        HttpServletRequest request = (HttpServletRequest) getFacesContext().getExternalContext().getRequest();
        String host = request.getRemoteHost();
        return host;
    }

    /**
     * Busca un componente de la vista por su nombre.
     *
     * @param name nombre del componente.
     * @return componente encontrado, o {@code null} si no existe.
     */
    public UIComponent findComponent(String name) {
        if (Fn.nvl(name, "").isEmpty()) {
            return null;
        }
        UIComponent componente = FacesContext.getCurrentInstance().getViewRoot().findComponent(name);
        return componente;
    }

    /**
     * Agrega un parámetro de callback (PrimeFaces) para la respuesta Ajax.
     *
     * @param param nombre del parámetro.
     * @param value valor.
     */
    public void addCallbackParam(String param, Object value) {
        getRequestContext().ajax().addCallbackParam(param, value);
    }

    /**
     * Agrega un {@code FacesMessage} a un componente.
     *
     * @param clientId identificador del componente.
     * @param message mensaje JSF.
     */
    public void addMessage(String clientId, FacesMessage message) {
        FacesContext.getCurrentInstance().addMessage(clientId, message);
    }

    /**
     * Devuelve el identificador de la empresa activa en la sesión.
     *
     * @return identificador de la empresa.
     */
    public Long getIdEmpresa() {
        return this.getUserSession().getIdEmpresa();
    }

    /**
     * Devuelve la sesión de usuario actual.
     *
     * @return sesión de usuario.
     */
    public IUserSession getUserSession() {
        IUserSession userSession = (IUserSession) getSessionMap().get("userSession");
        return userSession;
    }

    /**
     * Devuelve el identificador del usuario actual.
     *
     * @return identificador del usuario.
     */
    public Long getUserId() {
        IUserSession userSession = (IUserSession) getSessionMap().get("userSession");
        if (userSession == null || userSession.getUser() == null) {
            return null;
        }
        return userSession.getUser().getIduser();
    }

    /**
     * Cierra la sesión y devuelve la navegación resultante.
     *
     * @return destino de navegación tras el logout.
     */
    public String logout() {
        getSessionMap().put("userSession", null);
        return "/login.xhtml?faces-redirect=true";
    }

    /**
     * Navega a la página de inicio.
     *
     * @throws Exception si la navegación falla.
     */
    public void goHome() throws Exception {
        HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        HttpServletResponse res = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
        res.sendRedirect(req.getContextPath() + "/home.xhtml");
    }

    /**
     * Navega a la página indicada.
     *
     * @param url destino de navegación.
     * @throws Exception si la navegación falla.
     */
    public void goPage(String url) throws Exception {
        HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        HttpServletResponse res = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
        res.sendRedirect(req.getContextPath() + "/" + url);
    }
}
