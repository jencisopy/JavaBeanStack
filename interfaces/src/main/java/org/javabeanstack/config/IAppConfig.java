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
package org.javabeanstack.config;

import java.util.List;
import org.javabeanstack.data.IDataResult;
import org.javabeanstack.error.IErrorReg;
import org.w3c.dom.Document;
import org.javabeanstack.model.IAppSystemParam;

/**
 * Contrato de la configuración de la aplicación: acceso a los parámetros de
 * sistema ({@link IAppSystemParam}), a las propiedades de configuración
 * almacenadas como XML y al control de versión del esquema de base de datos.
 *
 * @author Jorge Enciso
 */
public interface IAppConfig {
    /**
     * Devuelve un parámetro de sistema por su identificador.
     *
     * @param id identificador del parámetro.
     * @return parámetro de sistema.
     */
    IAppSystemParam getSystemParam(Long id);

    /**
     * Devuelve un parámetro de sistema por su nombre.
     *
     * @param param nombre del parámetro.
     * @return parámetro de sistema.
     */
    IAppSystemParam getSystemParam(String param);

    /**
     * Devuelve la lista de todos los parámetros de sistema.
     *
     * @return lista de parámetros de sistema.
     */
    List<IAppSystemParam> getSystemParams();

    /**
     * Devuelve la configuración de un grupo como documento XML.
     *
     * @param groupKey clave del grupo de configuración.
     * @return documento XML de configuración.
     */
    Document getConfigDOM(String groupKey);

    /**
     * Devuelve el valor de una propiedad de configuración.
     *
     * @param property nombre de la propiedad.
     * @param groupKey clave del grupo.
     * @param nodePath ruta del nodo dentro del XML.
     * @return valor de la propiedad.
     */
    String getProperty(String property, String groupKey, String nodePath);

    /**
     * Asigna el valor de una propiedad de configuración.
     *
     * @param value valor a asignar.
     * @param property nombre de la propiedad.
     * @param groupKey clave del grupo.
     * @param nodePath ruta del nodo dentro del XML.
     * @return verdadero si se asignó, falso si no.
     */
    boolean setProperty(String value, String property, String groupKey, String nodePath);

    /**
     * Devuelve la ruta del sistema de archivos configurada para la sesión.
     *
     * @param sessionId identificador de la sesión.
     * @return ruta del sistema de archivos.
     */
    String getFileSystemPath(String sessionId);

    /**
     * Persiste un parámetro de sistema.
     *
     * @param param parámetro a guardar.
     * @return resultado de la operación.
     * @throws Exception si la persistencia falla.
     */
    IDataResult setSystemParam(IAppSystemParam param) throws Exception;

    /**
     * Persiste una lista de parámetros de sistema.
     *
     * @param params lista de parámetros a guardar.
     * @throws Exception si la persistencia falla.
     */
    void setSystemParams(List<IAppSystemParam> params) throws Exception;

    /**
     * Actualiza el esquema de la base de datos a la versión de la aplicación.
     *
     * @param sessionId identificador de la sesión.
     * @return registro de error si la actualización falla, o {@code null} si tuvo éxito.
     * @throws Exception si ocurre un error durante la actualización.
     */
    IErrorReg updateDatabase(String sessionId) throws Exception;

    /**
     * Verifica que el esquema de la base de datos sea compatible con la aplicación.
     *
     * @param sessionId identificador de la sesión.
     * @return registro de error si no es compatible, o {@code null} si lo es.
     * @throws Exception si ocurre un error durante la verificación.
     */
    IErrorReg checkDatabase(String sessionId) throws Exception;

    /**
     * Devuelve la versión de base de datos esperada por esta aplicación.
     *
     * @return versión de base de datos de la aplicación.
     */
    Integer getDBVersionForThisApp();

    /**
     * Devuelve la versión de base de datos actualmente instalada.
     *
     * @param sessionId identificador de la sesión.
     * @return versión de base de datos instalada.
     */
    Integer getDBVersion(String sessionId);
}
