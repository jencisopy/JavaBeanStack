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
package org.javabeanstack.resources;

import java.io.Serializable;
import java.util.Map;
import org.javabeanstack.model.IAppCompany;
import org.javabeanstack.model.IAppUser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.javabeanstack.security.model.IUserSession;
import org.javabeanstack.xml.IXmlDom;

/**
 * Contrato de acceso a los recursos de la aplicación (archivos, XML, avatares,
 * logos) resolviéndolos según la sesión de usuario y la empresa activa.
 *
 * @author Jorge Enciso
 */
public interface IAppResource extends Serializable{
    /**
     * Devuelve un recurso como arreglo de bytes, resuelto según la sesión.
     *
     * @param userSession sesión del usuario.
     * @param resourcePath ruta del recurso.
     * @return contenido del recurso.
     */
    byte[] getResourceAsBytes(IUserSession userSession, String resourcePath);

    /**
     * Devuelve un recurso como arreglo de bytes, resuelto según el identificador
     * de sesión.
     *
     * @param sessionId identificador de la sesión.
     * @param resourcePath ruta del recurso.
     * @return contenido del recurso.
     */
    byte[] getResourceAsBytes(String sessionId, String resourcePath);

    /**
     * Devuelve un recurso XML como DOM, tomando un nodo y aplicando parámetros.
     *
     * @param sessionId identificador de la sesión.
     * @param resourcePath ruta del recurso.
     * @param elementPath ruta del elemento dentro del XML.
     * @param params parámetros de sustitución.
     * @return DOM del recurso XML.
     */
    IXmlDom<Document, Element> getResourceAsXmlDom(String sessionId, String resourcePath, String elementPath, Map<String, String> params);

    /**
     * Devuelve el avatar de un usuario como arreglo de bytes.
     *
     * @param <T> tipo de la entidad usuario.
     * @param entityClass clase de la entidad usuario.
     * @param userId identificador del usuario.
     * @return contenido del avatar.
     */
    <T extends IAppUser> byte[] getUserAvatar(Class<T> entityClass, Long userId);

    /**
     * Devuelve el logo de una empresa como arreglo de bytes.
     *
     * @param <T> tipo de la entidad empresa.
     * @param entityClass clase de la entidad empresa.
     * @param companyId identificador de la empresa.
     * @return contenido del logo.
     */
    <T extends IAppCompany> byte[] getCompanyLogo(Class<T> entityClass, Long companyId);
}
