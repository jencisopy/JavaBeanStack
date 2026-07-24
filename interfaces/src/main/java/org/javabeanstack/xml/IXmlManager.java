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
package org.javabeanstack.xml;

import java.util.Map;

/**
 * Contrato del administrador de documentos XML: coordina el buscador de textos
 * XML ({@link IXmlSearcher}) y la caché de documentos ya procesados
 * ({@link IXmlCache}) para reutilizarlos y purgarlos.
 *
 * @author Jorge Enciso
 */
public interface IXmlManager {
    /**
     * Devuelve el buscador de textos XML asociado al administrador.
     *
     * @param <V> tipo del objeto DOM.
     * @return buscador de textos XML.
     */
    <V> IXmlSearcher<V> getXmlSearcher();

    /**
     * Devuelve la caché de documentos XML procesados.
     *
     * @return mapa clave → documento cacheado.
     */
    Map<String, IXmlCache> getCache();

    /**
     * Asigna la caché de documentos XML procesados.
     *
     * @param cache mapa clave → documento cacheado.
     */
    void setCache(Map<String, IXmlCache> cache);

    /**
     * Agrega un documento a la caché.
     *
     * @param key clave del documento.
     * @param value documento cacheado.
     */
    void addToCache(String key, IXmlCache value);

    /**
     * Elimina un documento de la caché.
     *
     * @param key clave del documento.
     */
    void removeFromCache(String key);

    /**
     * Vacía la caché de documentos.
     */
    void clearCache();

    /**
     * Procesa los documentos pendientes del administrador.
     */
    void processObjects();

    /**
     * Purga los documentos obsoletos o sin uso de la caché.
     */
    void purgeObjects();
}
