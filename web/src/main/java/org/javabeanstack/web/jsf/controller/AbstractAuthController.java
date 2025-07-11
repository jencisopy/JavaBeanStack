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
package org.javabeanstack.web.jsf.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.servlet.RequestDispatcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.javabeanstack.data.IDataLink;
import org.javabeanstack.security.ISecManager;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.util.Strings;
import org.javabeanstack.model.IAppCompany;
import org.javabeanstack.data.services.IAppCompanySrv;
import org.javabeanstack.error.ErrorReg;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.events.IAppSystemEvents;
import org.javabeanstack.util.Parameters;

/**
 * Clase controller de autenticación, recibe peticiones de logeo del usuario lo
 * procesa utilizando los componentes necesarios
 *
 * @author Jorge Enciso
 */
public abstract class AbstractAuthController extends AbstractController {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LogManager.getLogger(AbstractAuthController.class);

    /**
     * Login o identificador del usuario
     */
    private String userLogin;
    /**
     * Variable utilizada para propósitos internos
     */
    private String userLoginOld;
    /**
     * Password
     */
    private String password;
    /**
     * Si el usuario actual esta logeado
     */
    private boolean logged;
    /**
     * Petición URL original antes de que el filtro detectara que el
     * peticionante no estaba logeado
     */
    private String originalURL;
    /**
     * Ultima empresa a la que el usuario actual se logeo
     */
    private IAppCompany lastCompanySession;
    /**
     * Empresa seleccionada
     */
    private IAppCompany company;
    /**
     * Lista de empresas a la que el usuario tiene acceso
     */
    private List<IAppCompany> userCompanyAllowedList;
    /**
     * Indica si el usuario paso por el control del login
     */
    private boolean userControlBlur = false;

    /**
     * Objeto acceso a los datos
     *
     * @return instancia del objeto acceso a la capa de datos
     */
    public abstract IDataLink getDataLink();

    /**
     * Objeto para acceder a la capa de seguridad donde se crea las sesiones
     * entre otras funcionalidades
     *
     * @return objeto para acceder a la capa de seguridad.
     */
    public abstract ISecManager getSecManager();

    /**
     * Objeto servicio de la empresa, donde esta las válidaciones y la lógica de
     * las funcionalides de la instancia AppCompany.
     *
     * @return objeto con la lógica de las funcionalidas de AppCompany
     */
    public abstract IAppCompanySrv getAppCompanySrv();

    protected abstract IAppSystemEvents getAppSysEvents();

    @PostConstruct
    public void init() {
        logged = false;
        userCompanyAllowedList = new ArrayList<>();
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

    /**
     * Identificador o login del usuario
     *
     * @return Identificador o login del usuario
     */
    public String getUserLogin() {
        return userLogin;
    }

    /**
     * Se ejecuta de la página del login en el evento blur del campo username
     */
    public void onUserBlur() {
        userControlBlur = true;
        if (!userLogin.equals(userLoginOld)) {
            this.logged = false;
            this.company = null;
        }
        userLoginOld = userLogin;
    }

    /**
     * Asigna la variable user login
     *
     * @param userLogin identificador del login del usuario.
     */
    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }

    /**
     * @return password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Asigna el password ingresado por el usuario.
     *
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Retorna si esta logeado o no el usuario
     *
     * @return verdadero o falso si esta o no logeado el usuario
     */
    public boolean isLogged() {
        return logged;
    }

    /**
     * Asigna la variable logged.
     *
     * @param logged verdadero o falso si se logeo o no el usuario.
     */
    protected void setLogged(boolean logged) {
        this.logged = logged;
    }

    /**
     * Petición URL original antes de que el filtro detectara que el
     * peticionante no estaba logeado
     *
     * @return petición URL original
     */
    public String getOriginalURL() {
        return originalURL;
    }

    /**
     * Asigna la petición original URL.
     *
     * @param originalURL petición URL original
     */
    public void setOriginalURL(String originalURL) {
        this.originalURL = originalURL;
    }

    /**
     * Empresa seleccionada
     *
     * @return objeto o registro de empresa seleccionada.
     */
    public IAppCompany getCompany() {
        return company;
    }

    /**
     * Asigna la empresa que fue seleccionada por el usuario.
     *
     * @param company objeto empresa
     */
    public void setCompany(IAppCompany company) {
        this.company = company;
    }

    /**
     * Devuelve la Lista de empresas a la que el usuario tiene acceso
     *
     * @return Lista de empresas a la que el usuario tiene acceso
     */
    public List<IAppCompany> getUserCompanyAllowedList() {
        if (userCompanyAllowedList == null || userCompanyAllowedList.isEmpty()) {
            IUserSession userSession = getUserSession();
            if (userSession == null) {
                return null;
            }
            loadCompanyList(userSession);
        }
        return userCompanyAllowedList;
    }

    /**
     * Asigna la lista de empresas a la que el usuario tiene acceso
     *
     * @param userCompanyAllowedList lista de empresas a la que el usuario tiene
     * acceso
     */
    public void setUserCompanyAllowedList(List<IAppCompany> userCompanyAllowedList) {
        this.userCompanyAllowedList = userCompanyAllowedList;
    }

    /**
     * Ultima empresa a la que acceso el usuario actual.
     *
     * @return ultima empresa accesada.
     */
    public IAppCompany getLastCompanySession() {
        return this.lastCompanySession;
    }

    /**
     * Asigna en un atributo de la instancia la empresa que se esta ingresando
     *
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
            getFacesCtx().showError(ex, LOGGER);
        }
    }

    /**
     * Verifica si el usuario y contraseña proveida para ingresar al sistema es
     * válida.
     *
     * @return verdadero o falso si el usuario y contraseña son válidos.
     */
    public Boolean login() {
        if (!userControlBlur) {
            return false;
        }
        boolean result = false;
        if (!Strings.isNullorEmpty(userLogin)) {
            try {
                // Chequea válidez de los datos ingresados.
                Map<String, Object> otherParams = new Parameters()
                        .put("ipRequestFrom", getFacesCtx().getIp())
                        .getParams();

                IUserSession userSession = getSecManager().login2(userLogin, password, otherParams);
                if (userSession.getUser() != null) {
                    result = true;
                    if (getFacesCtx().getSessionMap().get("userSession") == null) {
                        getFacesCtx().getSessionMap().put("userSession", userSession);
                    }
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
                getFacesCtx().showError(ex, LOGGER);
            }
        } else {
            getFacesCtx().showError("", "Ingrese el usuario y la contraseña");
        }
        getFacesCtx().addCallbackParam("result", result);
        return result;
    }

    /**
     * Verifica los datos del usuario, contraseña y empresa para luego crear la
     * sesión de entrada si los datos proveidos fuerón correctos.
     *
     * @return verdadero o falso si tuvo exito o no al crear la sesión de
     * entrada.
     */
    public Boolean createSession() {
        boolean result = false;
        if (!userControlBlur) {
            return false;
        }
        if (company != null) {
            result = true;

            Map<String, Object> otherParams = new Parameters()
                    .put("ipRequestFrom", getFacesCtx().getIp())
                    .getParams();
            IUserSession userSession = getSecManager().createSession(userLogin, password, company.getIdcompany(), null, otherParams);
            userSession.setIp(getFacesCtx().getIp());
            userSession.setHost(getFacesCtx().getHost());
            if (userSession.getError() != null) {
                result = false;
                //Error en evento de creación de sessión
                getFacesCtx().showError("", userSession.getError().getMessage());
                userSession.getError().setIpRequest(userSession.getIp());
                getAppSysEvents().onCreateSession(userSession.getError());
            } else {
                getFacesCtx().getSessionMap().put("userSession", userSession);
                userSession.setCompany(company);
                //Evento de creación de sessión
                getAppSysEvents().onCreateSession();
            }
        } else {
            getFacesCtx().showError("", "Empresa no válida.");
            //Error en evento de creación de sessión
            IErrorReg error = new ErrorReg();
            error.setErrorNumber(50000);
            error.setMessage("Empresa no válida");
            error.setFieldName("idcompany");
            error.setEntity("USUARIO");
            error.setIpRequest(getFacesCtx().getIp());
            getAppSysEvents().onCreateSession(error);
        }
        if (result) {
            getFacesCtx().addCallbackParam("originalURL", originalURL);
        }
        getFacesCtx().addCallbackParam("result", result);
        return result;
    }

    /**
     * Puebla un array con la lista de empresas a la que el usuario puede
     * ingresar
     *
     * @param userSession datos del usuario.
     */
    private void loadCompanyList(IUserSession userSession) {
        if (!userCompanyAllowedList.isEmpty()) {
            userCompanyAllowedList.clear();
        }
        try {
            List<IAppCompany> query = getAppCompanySrv().getAppCompanyLight(userSession);
            query.forEach((empresa) -> {
                userCompanyAllowedList.add(empresa);
            });
        } catch (Exception ex) {
            getFacesCtx().showError(ex, LOGGER);
        }
    }

    /**
     * Este metodo gestiona el cambio de la empresa en la sesión del usuario
     *
     * @param idcompany
     * @return nueva instancia del objeto sesión.
     */
    public IUserSession reCreateSession(Long idcompany) {
        IUserSession sessionOld = getUserSession();
        if (sessionOld != null) {
            IUserSession session = getSecManager().reCreateSession(sessionOld.getSessionId(), idcompany);
            //Evento de CrearSesion
            getAppSysEvents().onCreateSession();
            return session;
        }
        return null;
    }

    /**
     * Cierra la sesión del usuario
     *
     * @return link para redireccionar a la página de logeo
     */
    @Override
    public String logout() {
        logged = false;
        return super.logout();
    }
}
