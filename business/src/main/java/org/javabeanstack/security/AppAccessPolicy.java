/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
*        jenciso@javabeanstack.org
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
package org.javabeanstack.security;

import org.javabeanstack.config.IAppConfig;
import org.javabeanstack.model.IAppSystemParam;
import org.javabeanstack.model.IAppUser;

/**
 * Política de acceso de un rol a una aplicación. Resuelve, contra los
 * parámetros del sistema, si un rol puede ingresar a una aplicación
 * ({@link #ACCESS}) y si puede modificar datos en ella ({@link #WRITE}).
 *
 * <p>El nombre del parámetro se construye por concatenación:</p>
 * <pre>
 *     &lt;NOMBRE-APLICACION-EN-MAYUSCULAS&gt;_&lt;PERMISO&gt;_&lt;ROL&gt;
 *     MAKER-WEB_ACCESS_30 · MAKER-REST_WRITE_40
 * </pre>
 * <p>El nombre de la aplicación es el <em>context path</em> del despliegue, de
 * modo que la misma política sirve para cualquier aplicación sin declarar
 * constantes por cada una. El nombre nunca se parte: solo se arma.</p>
 *
 * <p>Reglas:</p>
 * <ol>
 *   <li>Si {@code ACCESS} es falso, {@code WRITE} se considera falso.</li>
 *   <li>Un {@code WRITE} verdadero no habilita implícitamente el acceso.</li>
 *   <li>{@code ACCESS} habilita el ingreso y las operaciones de lectura.</li>
 *   <li>{@code WRITE} habilita altas, modificaciones y bajas.</li>
 *   <li>Un parámetro ausente significa <b>permitido</b>: los roles sin política
 *       declarada operan como siempre.</li>
 * </ol>
 *
 * <p>Esta clase no reemplaza a los demás controles de autorización (acceso a la
 * empresa, permisos por objeto, por campo o por registro): es un piso de
 * plataforma que se evalúa además de ellos.</p>
 *
 * @author Jorge Enciso
 */
public class AppAccessPolicy {

    /** Permiso de ingreso y lectura. */
    public static final String ACCESS = "ACCESS";
    /** Permiso de modificación de datos (altas, cambios y bajas). */
    public static final String WRITE = "WRITE";
    /**
     * Clave con la que el nombre de la aplicación queda registrado en la
     * sesión ({@code IUserSession.addInfo}). Lo asienta la capa de entrada
     * —que es la que conoce el context path— para que las capas siguientes,
     * como la de datos, puedan evaluar la política sin depender del transporte.
     */
    public static final String APPNAME = "APPNAME";

    private AppAccessPolicy() {
    }

    /**
     * Devuelve el nombre del parámetro de sistema que gobierna un permiso.
     *
     * @param appName nombre de la aplicación (context path del despliegue).
     * @param permission {@link #ACCESS} o {@link #WRITE}.
     * @param rol código del rol.
     * @return nombre del parámetro, o nulo si falta algún dato.
     */
    public static String getParamName(String appName, String permission, String rol) {
        if (appName == null || permission == null || rol == null) {
            return null;
        }
        String app = appName.trim();
        //El context path llega como "/maker-web"; se descarta la barra inicial
        while (app.startsWith("/")) {
            app = app.substring(1);
        }
        if (app.isEmpty() || rol.trim().isEmpty()) {
            return null;
        }
        return app.toUpperCase() + "_" + permission + "_" + rol.trim();
    }

    /**
     * Determina si un rol tiene concedido un permiso sobre una aplicación.
     * Aplica la regla 1: sin {@code ACCESS} no hay {@code WRITE}.
     *
     * @param appConfig configuración de la aplicación, de donde se leen los
     * parámetros del sistema.
     * @param appName nombre de la aplicación (context path del despliegue).
     * @param permission {@link #ACCESS} o {@link #WRITE}.
     * @param rol código del rol.
     * @return verdadero si el permiso está concedido; verdadero también si no
     * hay parámetro que lo regule.
     */
    public static boolean isAllowed(IAppConfig appConfig, String appName, String permission, String rol) {
        if (appConfig == null) {
            return true;
        }
        //Regla 1: sin acceso a la aplicación tampoco hay escritura
        if (WRITE.equals(permission) && !isParamAllowed(appConfig, appName, ACCESS, rol)) {
            return false;
        }
        return isParamAllowed(appConfig, appName, permission, rol);
    }

    /**
     * Determina si un usuario tiene concedido un permiso sobre una aplicación.
     *
     * <p>El rol evaluado es el <b>asignado al propio usuario</b>
     * ({@link IAppUser#getRol()}), no el de mayor privilegio entre él y los de
     * sus grupos: la política de acceso a una aplicación es una propiedad del
     * usuario, y hacerla depender de los grupos permitiría que un usuario de
     * portal ganara acceso con solo integrar un grupo. Los permisos heredados
     * de los grupos siguen operando en las demás capas de autorización.</p>
     *
     * @param appConfig configuración de la aplicación.
     * @param appName nombre de la aplicación (context path del despliegue).
     * @param permission {@link #ACCESS} o {@link #WRITE}.
     * @param user usuario a evaluar.
     * @return verdadero si el permiso está concedido.
     */
    public static boolean isAllowed(IAppConfig appConfig, String appName, String permission, IAppUser user) {
        if (user == null) {
            return false;
        }
        return isAllowed(appConfig, appName, permission, user.getRol());
    }

    /**
     * Lee el valor de un parámetro de política. Un parámetro ausente, sin valor
     * o ilegible se interpreta como permitido (regla 5), de modo que la
     * ausencia de configuración nunca deja a nadie afuera.
     *
     * @param appConfig configuración de la aplicación.
     * @param appName nombre de la aplicación.
     * @param permission permiso a consultar.
     * @param rol código del rol.
     * @return valor del parámetro, o verdadero si no está definido.
     */
    private static boolean isParamAllowed(IAppConfig appConfig, String appName, String permission, String rol) {
        String paramName = getParamName(appName, permission, rol);
        if (paramName == null) {
            return true;
        }
        IAppSystemParam param = appConfig.getSystemParam(paramName);
        if (param == null || param.getValueBoolean() == null) {
            return true;
        }
        return param.getValueBoolean();
    }
}
