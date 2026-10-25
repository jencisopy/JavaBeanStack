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
/**
 * Contratos del motor de documentos XML: administrador, buscador, DOM y caché
 * ({@link org.javabeanstack.xml.IXmlManager},
 * {@link org.javabeanstack.xml.IXmlSearcher}, {@link org.javabeanstack.xml.IXmlDom}).
 *
 * <p>Incluye además el contrato del mapeador de datos XML al modelo
 * ({@link org.javabeanstack.xml.IXmlRowMapper} y su punto de extensión
 * {@link org.javabeanstack.xml.IXmlFieldResolver}), que lleva los atributos de
 * un nodo a los atributos del mismo nombre de un
 * {@link org.javabeanstack.data.IDataRow}. La implementación vive en el módulo
 * {@code jbs-business} ({@code org.javabeanstack.xml.XmlRowMapper}), que es
 * donde están la reflexión sobre el modelo y la clase base de las
 * entidades.</p>
 */
package org.javabeanstack.xml;
