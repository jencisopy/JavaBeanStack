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
package org.javabeanstack.web.util;

import java.util.Iterator;
import java.util.Map;
import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.javabeanstack.error.IErrorReg;

import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.util.Fn;
import org.primefaces.PrimeFaces;

public class FacesContextUtil {

    public FacesContext getFacesContext() {
        return FacesContext.getCurrentInstance();
    }

    public Application getApplication() {
        return getFacesContext().getApplication();
    }

    public PrimeFaces getRequestContext() {
        return PrimeFaces.current();
    }

    public ExternalContext getExternalContext() {
        return getFacesContext().getExternalContext();
    }

    public String getRequestContextPath() {
        return getExternalContext().getRequestContextPath();
    }

    public String getRealPath(String path) {
        return getExternalContext().getRealPath(path);
    }

    public UIViewRoot getUIViewRoot() {
        ViewHandler viewHandler = getApplication().getViewHandler();
        return viewHandler.createView(getFacesContext(), getFacesContext().getViewRoot().getViewId());
    }

    public HttpSession getSession() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest req = (HttpServletRequest) context.getExternalContext().getRequest();
        HttpSession session = req.getSession();
        return session;
    }

    public Map<String, Object> getSessionMap() {
        return FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
    }

    public Map<String, Object> getRequestMap() {
        return FacesContext.getCurrentInstance().getExternalContext().getRequestMap();
    }

    public Map<String, String> getRequestParameterMap() {
        return FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
    }

    public Map<Object, Object> getAttributes(String key) {
        return FacesContext.getCurrentInstance().getAttributes();
    }

    public void setAttribute(Object key, Object value) {
        FacesContext.getCurrentInstance().getAttributes().put(key, value);
    }

    public void refreshView() {
        FacesContext context = getFacesContext();
        Application application = getApplication();
        ViewHandler viewHandler = application.getViewHandler();
        UIViewRoot viewRoot = viewHandler.createView(context, context.getViewRoot().getViewId());
        context.setViewRoot(viewRoot);
    }

    public void refreshView(String idcomponent) {
        FacesContext context = FacesContext.getCurrentInstance();
        UIComponent component = this.findComponent(idcomponent);
        if (component != null){
            idcomponent = component.getClientId();
            context.getPartialViewContext().getRenderIds().add(idcomponent);
        }
    }

    public void showError(String message) {
        showError("Error", message);
    }

    public void showError(String title, String message) {
        getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, title, message));
    }

    public void showError(String title, String message, String clientId) {
        getFacesContext().addMessage(clientId, new FacesMessage(FacesMessage.SEVERITY_ERROR, title, message));
    }

    public void showError(String title, Map<String, IErrorReg> errors) {
        if (errors != null && errors.size() > 0) {
            Iterator iterator = errors.keySet().iterator();
            String key;
            while (iterator.hasNext()) {
                key = (String) iterator.next();
                String titleShow = Fn.nvl(title,"Error ")+" en "+key;
                getFacesContext().addMessage(key, new FacesMessage(FacesMessage.SEVERITY_ERROR, titleShow, errors.get(key).getMessage()));
            }
        }
    }

    public void showInfo(String message) {
        showInfo("Información", message);
    }

    public void showInfo(String title, String message) {
        getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, title, message));
    }

    public void showWarn(String message) {
        showWarn("Aviso", message);
    }

    public void showWarn(String title, String message) {
        getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, title, message));
    }

    public String getIp() {
        HttpServletRequest request = (HttpServletRequest) getFacesContext().getExternalContext().getRequest();
        String ip = request.getHeader("X-FORWARDED-FOR");
        if (ip == null) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    public String getHost() {
        HttpServletRequest request = (HttpServletRequest) getFacesContext().getExternalContext().getRequest();
        String host = request.getRemoteHost();
        return host;
    }

    public UIComponent findComponent(String name) {
        if (Fn.nvl(name,"").isEmpty()){
            return null;
        }
        UIComponent componente = FacesContext.getCurrentInstance().getViewRoot().findComponent(name);
        return componente;
    }

    public void addCallbackParam(String param, Object value) {
        getRequestContext().ajax().addCallbackParam(param, value);
    }

    public void addMessage(String clientId, FacesMessage message) {
        FacesContext.getCurrentInstance().addMessage(clientId, message);
    }

    public Long getIdEmpresa() {
        return this.getUserSession().getIdEmpresa();
    }

    public IUserSession getUserSession() {
        IUserSession userSession = (IUserSession) getSessionMap().get("userSession");
        return userSession;
    }

    public Long getUserId() {
        IUserSession userSession = (IUserSession) getSessionMap().get("userSession");
        if (userSession == null || userSession.getUser() == null) {
            return null;
        }
        return userSession.getUser().getIduser();
    }

    public String logout() {
        getSessionMap().put("userSession", null);
        return "/login.xhtml?faces-redirect=true";
    }

    public void goHome() throws Exception {
        HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        HttpServletResponse res = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
        res.sendRedirect(req.getContextPath() + "/home.xhtml");
    }

    public void goPage(String url) throws Exception {
        HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        HttpServletResponse res = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
        res.sendRedirect(req.getContextPath() + "/" + url);
    }
}
