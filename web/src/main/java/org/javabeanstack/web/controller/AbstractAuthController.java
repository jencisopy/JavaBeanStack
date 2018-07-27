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
package org.javabeanstack.web.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.servlet.RequestDispatcher;
import org.apache.log4j.Logger;

import org.javabeanstack.data.IDataLink;
import org.javabeanstack.security.ISecManager;
import org.javabeanstack.security.IUserSession;
import org.javabeanstack.util.Strings;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.model.IAppCompany;
import org.javabeanstack.services.IAppCompanySrv;

/**
 * Clase controller de autenticación, recibe peticiones de logeo del usuario lo
 * procesa utilizando los componentes necesarios
 *
 * @author Jorge Enciso
 */
public abstract class AbstractAuthController extends AbstractController {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(AbstractAuthController.class);

    private String username;
    private String usernameold;
    private String password;
    private boolean logged;
    private String originalURL;
    private IAppCompany lastCompanySession;
    private IAppCompany company;
    private List<IAppCompany> userCompanyList;
    private boolean userControlBlur = false;

    public abstract IDataLink getDataLink();

    public abstract ISecManager getSecManager();

    public abstract IAppCompanySrv getAppCompanySrv();

    @PostConstruct
    public void init() {
        logged = false;
        userCompanyList = new ArrayList<>();
        originalURL = (String) getFacesCtx().getRequestMap().get(RequestDispatcher.FORWARD_REQUEST_URI);

        if (originalURL == null) {
            originalURL = getFacesCtx().getRequestContextPath() + "/index.xhtml";
        } else {
            String originalQuery = (String) getFacesCtx().getRequestMap().get(RequestDispatcher.FORWARD_QUERY_STRING);
            if (originalQuery != null) {
                originalURL += "?" + originalQuery;
            }
        }
    }

    public String getUsername() {
        return username;
    }

    /**
     * Se ejecuta de la página del login en el evento blur del campo username
     */
    public void onUserBlur() {
        userControlBlur = true;
        if (!username.equals(usernameold)) {
            this.logged = false;
            this.company = null;
        }
        usernameold = username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isLogged() {
        return logged;
    }

    public void setLogged(boolean logged) {
        this.logged = logged;
    }

    public String getOriginalURL() {
        return originalURL;
    }

    public void setOriginalURL(String originalURL) {
        this.originalURL = originalURL;
    }

    public IAppCompany getCompany() {
        return company;
    }

    public void setCompany(IAppCompany company) {
        this.company = company;
    }

    public List<IAppCompany> getUserCompanyList() {
        return userCompanyList;
    }

    public void setUserCompanyList(List<IAppCompany> userCompanyList) {
        this.userCompanyList = userCompanyList;
    }

    public IAppCompany getLastCompanySession() {
        return this.lastCompanySession;
    }

    /**
     * Asigna en un atributo de la instancia la empresa que se esta ingresando
     * @param idempresa identificador de la empresa
     */
    protected void setLastCompanySession(Long idempresa) {
        if (!userControlBlur) {
            return;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("idempresa", idempresa);
        IAppCompany emp;
        try {
            emp = getDataLink().findByQuery(
                    "select o from AppCompanyLight o where idcompany = :idempresa",
                    params);
            this.lastCompanySession = emp;
            this.company = lastCompanySession;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
    }

    /**
     * Verifica si el usuario y contraseña proveida para ingresar al sistema es 
     * válida.
     * @return verdadero o falso si el usuario y contraseña son válidos. 
     */
    public Boolean login() {
        if (!userControlBlur) {
            return false;
        }
        boolean result = false;
        if (!Strings.isNullorEmpty(username)) {
            try {
                // Chequea válidez de los datos ingresados.
                IUserSession userSession = getSecManager().login2(username, password);
                if (userSession.getUser() != null) {
                    result = true;
                    userSession.setIp(getFacesCtx().getIp());
                    userSession.setHost(getFacesCtx().getHost());
                    getFacesCtx().getSessionMap().put("userSession", userSession);
                    if (company == null) {
                        // Traer lista de empresas a la que el usuario esta permitido ingresar
                        loadCompanyList(userSession);
                        // Asignar idempresa por defecto al que el usuario se logeo en su
                        // sesión anterior
                        setLastCompanySession(userSession.getUser().getIdcompany());
                    }
                    logged = true;
                    getFacesCtx().showInfo("", "Usuario activo: " + userSession.getUser().getFullName());
                } else {
                    getFacesCtx().showError("", userSession.getError().getMessage());
                }
            } catch (Exception ex) {
                ErrorManager.showError(ex, LOGGER);
            }
        } else {
            getFacesCtx().showError("", "Ingrese el usuario y la contraseña");
        }
        getFacesCtx().addCallbackParam("result", result);
        return result;
    }

    /**
     * Se ejecuta en el evento submit del formulario de logeo
     * @return verdadero o falso si tuvo exito o no al crear la sesión de entrada.
     */
    public Boolean onSubmit() {
        return createSession();
    }

    /**
     * Verifica los datos del usuario, contraseña y empresa para luego crear
     * la sesión de entrada si los datos proveidos fuerón correctos.
     * @return verdadero o falso si tuvo exito o no al crear la sesión de entrada.
     */
    public Boolean createSession() {
        boolean result = false;
        if (!userControlBlur) {
            return false;
        }
        if (company != null) {
            result = true;
            IUserSession userSession = getSecManager().createSession(username, password, company.getIdcompany(), null);
            if (userSession.getError() != null) {
                result = false;
                getFacesCtx().showError("", userSession.getError().getMessage());
            } else {
                getFacesCtx().getSessionMap().put("userSession", userSession);
            }
            userSession.setCompany(company);
        } else {
            getFacesCtx().showError("", "Empresa no válida.");
        }
        if (result) {
            getFacesCtx().addCallbackParam("originalURL", originalURL);
        }
        getFacesCtx().addCallbackParam("result", result);
        return result;
    }

    /**
     * Puebla un array con la lista de empresas a la que el usuario puede ingresar
     * @param userSession datos del usuario.
     */
    private void loadCompanyList(IUserSession userSession) {
        if (userCompanyList.size() > 0) {
            userCompanyList.clear();
        }
        try {
            List<IAppCompany> query = getAppCompanySrv().getAppCompanyLight(userSession);
            query.forEach((empresa) -> {
                userCompanyList.add(empresa);
            });
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
    }

    public List<IAppCompany> getCompanyList() {
        IUserSession userSession = getUserSession();
        if (userSession == null) {
            return null;
        }
        loadCompanyList(userSession);
        return userCompanyList;
    }

    @Override
    public String logout() {
        logged = false;
        return super.logout();
    }
}
