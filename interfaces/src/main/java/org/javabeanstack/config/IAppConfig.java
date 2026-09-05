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
     * Versión de estructura "no determinada".
     *
     * <p>Devuelto por {@link #getDBVersionForThisApp()} desactiva la
     * verificación de versión; devuelto por {@link #getDBVersion(String)} NO la
     * desactiva: entra a la comparación y hace que la base parezca
     * adelantada.</p>
     */
    String DBVERSION_UNDEFINED = "999999";

    /**
     * Clave con la que {@link #updateDatabase(String)} deja en la sesión del
     * usuario ({@code IUserSession.addInfo}) el motivo por el cual no pudo
     * actualizar la estructura de la base.
     *
     * <p>Es un canal de una sola lectura: la actualización corre dentro de la
     * petición del ingreso, cuando la vista activa es todavía la de login, así
     * que un mensaje encolado ahí no sobrevive a la navegación. La primera
     * pantalla que se dibuja lo lee, lo muestra y lo borra.</p>
     */
    String DBUPDATE_ERROR = "dbUpdateError";

    /**
     * Clave con la que {@link #updateDatabase(String)} deja en la sesión del
     * usuario el aviso de que la estructura de la base se actualizó bien, con
     * la versión que quedó vigente.
     *
     * <p>Viaja por el mismo canal de una sola lectura que
     * {@link #DBUPDATE_ERROR}, y por el mismo motivo. Los dos pueden convivir:
     * con la actualización configurada para continuar ante un error, un
     * paquete puede fallar y los anteriores quedar aplicados.</p>
     */
    String DBUPDATE_INFO = "dbUpdateInfo";

    /**
     * Devuelve la versión de estructura de base de datos que espera esta
     * aplicación, con el formato {@code <version>.<secuencia>} (por ejemplo
     * {@code "10.001"}).
     *
     * <p>Las dos partes son de ancho fijo —dos dígitos la versión, tres la
     * secuencia— porque {@code checkDatabase} las compara como texto: sin el
     * relleno, {@code "9.001"} resultaría mayor que {@code "10.001"}.</p>
     *
     * @return versión de estructura esperada, o {@code "999999"} si no está
     * definida, valor con el que la verificación queda desactivada.
     */
    String getDBVersionForThisApp();

    /**
     * Devuelve la versión de estructura actualmente instalada en la base, con
     * el mismo formato que {@link #getDBVersionForThisApp()}.
     *
     * @param sessionId identificador de la sesión.
     * @return versión de estructura instalada, o {@code "999999"} si no pudo
     * determinarse. Ese valor NO desactiva la verificación por este lado: entra
     * a la comparación y hace que la base parezca adelantada.
     */
    String getDBVersion(String sessionId);
}
