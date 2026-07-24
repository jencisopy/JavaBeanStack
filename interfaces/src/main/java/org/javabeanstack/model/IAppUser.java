/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
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

package org.javabeanstack.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import org.javabeanstack.data.IDataRow;


/**
 * Contrato de la entidad usuario de aplicación: credenciales, rol, empresas
 * habilitadas ({@link IAppCompanyAllowed}) y pertenencia a grupos
 * ({@link IAppUserMember}). Extiende {@link IDataRow}.
 *
 * @author Jorge Enciso
 */
public interface IAppUser extends IDataRow, Serializable {
    /** Rol: analista. */
    public static final String ANALISTA = "00";
    /** Rol: superusuario. */
    public static final String SUPERUSER = "01";
    /** Rol: administrador del sistema. */
    public static final String ADMINISTRADOR = "20";
    /** Rol: administrador del sistema (alias de {@link #ADMINISTRADOR}). */
    public static final String ADMINISTRADORSYSTEM = "20";
    /** Rol: administrador de empresa. */
    public static final String ADMINCOMPANY = "21";
    /** Rol: usuario de tipo token (acceso por token). */
    public static final String TOKEN = "25";
    /** Rol: usuario común. */
    public static final String USUARIO = "30";

    /** Tipo de registro: usuario. */
    public static final Short ISUSER = 1;
    /** Tipo de registro: grupo de usuarios. */
    public static final Short ISUSERGROUP = 2;

    /**
     * Devuelve el identificador del usuario.
     * @return identificador del usuario.
     */
    Long getIduser();

    /**
     * Devuelve el login del usuario.
     * @return login del usuario.
     */
    String getLogin();

    /**
     * Devuelve el código del usuario.
     * @return código del usuario.
     */
    String getCode();

    /**
     * Devuelve el nombre completo del usuario.
     * @return nombre completo.
     */
    String getFullName();

    /**
     * Devuelve la descripción del usuario.
     * @return descripción.
     */
    String getDescription();

    /**
     * Devuelve la contraseña (hash) del usuario.
     * @return contraseña.
     */
    String getPass();

    /**
     * Devuelve la copia de respaldo de la contraseña.
     * @return contraseña de respaldo.
     */
    String getPassBackup();

    /**
     * Devuelve la confirmación de la contraseña.
     * @return confirmación de la contraseña.
     */
    String getPassConfirm();

    /**
     * Devuelve la segunda confirmación de la contraseña.
     * @return segunda confirmación de la contraseña.
     */
    String getPassConfirm2();

    /**
     * Indica si el usuario está deshabilitado.
     * @return verdadero si está deshabilitado, falso si no.
     */
    Boolean getDisabled();

    /**
     * Devuelve la fecha de expiración del usuario.
     * @return fecha de expiración.
     */
    LocalDateTime getExpiredDate();

    /**
     * Devuelve el identificador de la empresa por defecto del usuario.
     * @return identificador de la empresa.
     */
    Long getIdcompany();

    /**
     * Devuelve la lista de empresas habilitadas para el usuario.
     * @return lista de empresas habilitadas.
     */
    List<IAppCompanyAllowed> getAppCompanyAllowedList();

    /**
     * Devuelve la lista de grupos a los que pertenece el usuario.
     * @return lista de membresías.
     */
    List<IAppUserMember> getUserMemberList();

    /**
     * Devuelve el rol del usuario.
     * @return rol del usuario.
     */
    String getRol();

    /**
     * Devuelve el rol de mayor jerarquía del usuario.
     * @return rol de mayor jerarquía.
     */
    String getHighRol();

    /**
     * Devuelve todos los roles del usuario (propios y heredados de sus grupos).
     * @return roles del usuario.
     */
    String getAllRoles();

    /**
     * Devuelve el rol de aplicación del usuario.
     * @return rol de aplicación.
     */
    String getAppRol();

    /**
     * Devuelve el tipo de registro ({@link #ISUSER} o {@link #ISUSERGROUP}).
     * @return tipo de registro.
     */
    Short getType();

    /**
     * Devuelve el avatar del usuario.
     * @return avatar en bytes.
     */
    byte[] getAvatar();

    /**
     * Asigna el identificador del usuario.
     * @param iduser identificador del usuario.
     */
    void setIduser(Long iduser);

    /**
     * Asigna el login del usuario.
     * @param loginName login del usuario.
     */
    void setLogin(String loginName);

    /**
     * Asigna el código del usuario.
     * @param code código del usuario.
     */
    void setCode(String code);

    /**
     * Asigna el nombre completo del usuario.
     * @param name nombre completo.
     */
    void setFullName(String name);

    /**
     * Asigna la descripción del usuario.
     * @param description descripción.
     */
    void setDescription(String description);

    /**
     * Asigna la contraseña del usuario.
     * @param password contraseña.
     */
    void setPass(String password);

    /**
     * Asigna la confirmación de la contraseña.
     * @param passwordConfirm confirmación de la contraseña.
     */
    void setPassConfirm(String passwordConfirm);

    /**
     * Asigna la segunda confirmación de la contraseña.
     * @param passwordConfirm2 segunda confirmación de la contraseña.
     */
    void setPassConfirm2(String passwordConfirm2);

    /**
     * Asigna el identificador de la empresa por defecto del usuario.
     * @param idcompany identificador de la empresa.
     */
    void setIdcompany(Long idcompany);

    /**
     * Asigna la lista de empresas habilitadas para el usuario.
     * @param appCompanyAllowedList lista de empresas habilitadas.
     */
    void setAppCompanyAllowedList(List<IAppCompanyAllowed> appCompanyAllowedList);

    /**
     * Asigna la lista de grupos a los que pertenece el usuario.
     * @param userMemberList lista de membresías.
     */
    void setUserMemberList(List<IAppUserMember> userMemberList);

    /**
     * Asigna si el usuario está deshabilitado.
     * @param disable verdadero para deshabilitar.
     */
    void setDisabled(Boolean disable);

    /**
     * Asigna la fecha de expiración del usuario.
     * @param expira fecha de expiración.
     */
    void setExpiredDate(LocalDateTime expira);

    /**
     * Asigna el rol del usuario.
     * @param rol rol del usuario.
     */
    void setRol(String rol);

    /**
     * Asigna el rol de aplicación del usuario.
     * @param appRol rol de aplicación.
     */
    void setAppRol(String appRol);

    /**
     * Asigna el tipo de registro.
     * @param tipo tipo de registro ({@link #ISUSER} o {@link #ISUSERGROUP}).
     */
    void setType(Short tipo);

    /**
     * Asigna el avatar del usuario.
     * @param avatar avatar en bytes.
     */
    void setAvatar(byte[] avatar);

    /**
     * Indica si el usuario es administrador del sistema.
     * @return verdadero si es administrador del sistema.
     */
    boolean isSysAdmin();

    /**
     * Indica si el usuario es administrador de empresa.
     * @return verdadero si es administrador de empresa.
     */
    boolean isCompanyAdmin();

    /**
     * Indica si el usuario es superusuario.
     * @return verdadero si es superusuario.
     */
    boolean isSuperUser();
}
