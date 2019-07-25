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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import javax.annotation.PostConstruct;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.javabeanstack.config.IAppConfig;
import org.javabeanstack.resources.IAppResource;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.io.IOUtil;
import org.javabeanstack.security.model.IUserSession;
import static org.javabeanstack.data.DataNativeQuery.getClassModel;

/**
 *
 * @author Jorge Enciso
 */
public abstract class AbstractUserEnvironment extends AbstractController {

    private static final Logger LOGGER = Logger.getLogger(AbstractUserEnvironment.class);

    private byte[] userAvatar;
    private byte[] companyLogo;
    private Long userIdPrevious = 0L;
    private Long companyIdPrevious = 0L;
    private String modelPackagePath;

    public abstract IAppConfig getAppConfig();

    public abstract IAppResource getAppResource();

    @PostConstruct
    public void init() {
        //TODO analizar este código
        modelPackagePath = getAppConfig().getProperty("packagepathmodel",
                "SYSTEM", "/Configuration/SystemSetting/ClassInfo");
    }

    public byte[] getUserAvatar() {
        IUserSession userSession = getUserSession();
        // Si no esta logueado
        if (userSession == null || userSession.getUser() == null) {
            return null;
        }
        // Si ya se asigno la variable en una lectura previa
        if (userSession.getUser().getId().equals(userIdPrevious)) {
            return userAvatar;
        }
        try {
            Long id = userSession.getUser().getIduser();
            Class clazz = getClassModel(modelPackagePath, "AppUser");
            userAvatar = getAppResource().getUserAvatar(clazz, id);
            if (userAvatar == null) {
                userAvatar = getDefaultUserAvatar();
            }
            userIdPrevious = id;
            return userAvatar;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    public byte[] getCompanyLogo() {
        IUserSession userSession = getUserSession();
        if (userSession == null || userSession.getCompany() == null) {
            return null;
        }
        // Si ya se asigno la variable en una lectura previa
        if (userSession.getCompany().getIdcompany().equals(companyIdPrevious)) {
            return companyLogo;
        }
        try {
            Long id = userSession.getCompany().getIdcompany();
            Class clazz = getClassModel(modelPackagePath, "AppCompany");
            companyLogo = getAppResource().getCompanyLogo(clazz, id);
            if (companyLogo == null) {
                companyLogo = getDefaultCompanyLogo();
            }
            companyIdPrevious = id;
            return companyLogo;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    public byte[] getDefaultUserAvatar() {
        // Reading a Image file from file system            
        try (InputStream imageData = IOUtil.getResourceAsStream(getClass(), "/images/userlogo.png")) {
            return IOUtils.toByteArray(imageData);
        } catch (FileNotFoundException e) {
            System.out.println("Image not found" + e);
        } catch (IOException ioe) {
            System.out.println("Exception while reading the Image " + ioe);
        }
        return null;
    }

    public byte[] getDefaultCompanyLogo() {
        // Reading a Image file from file system
        try (InputStream imageData = IOUtil.getResourceAsStream(getClass(), "/images/companylogo.png")) {
            return IOUtils.toByteArray(imageData);
        } catch (FileNotFoundException e) {
            System.out.println("Image not found" + e);
        } catch (IOException ioe) {
            System.out.println("Exception while reading the Image " + ioe);
        }
        return null;
    }

    public String getUserAvatarAsString() {
        byte[] avatar = getUserAvatar();
        return getByteAsString(avatar);
    }

    public String getCompanyLogoAsString() {
        byte[] logo = getCompanyLogo();
        return getByteAsString(logo);
    }

    protected String getByteAsString(byte[] avatar) {
        if (avatar == null) {
            return null;
        }
        String avatarAsString;
        avatarAsString = Base64.getEncoder().encodeToString(avatar);
        return avatarAsString;
    }
    
    public String getProjectContextName(){
      return  getFacesCtx().getRequestContextPath();
    }
}
