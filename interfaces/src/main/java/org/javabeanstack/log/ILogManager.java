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
package org.javabeanstack.log;

import java.util.List;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.model.IAppMessage;
import org.javabeanstack.model.IAppLogRecord;

/**
 * Contrato del administrador de log: persiste mensajes y eventos
 * ({@link IAppLogRecord}) en la base de datos y da acceso al catálogo de
 * mensajes ({@link IAppMessage}).
 *
 * <p>La implementación de referencia es {@code org.javabeanstack.log.LogManager}.</p>
 *
 * @author Jorge Enciso
 */
public interface ILogManager {
    /**
     * Devuelve un mensaje del catálogo por su número.
     *
     * @param msgNumber número del mensaje.
     * @return mensaje del catálogo.
     */
    IAppMessage getAppMessage(Integer msgNumber);

    /**
     * Devuelve todos los mensajes del catálogo.
     *
     * @return lista de mensajes.
     */
    List<IAppMessage> getAppMessages();

    /**
     * Persiste un error en el log.
     *
     * @param errorReg registro de error.
     * @return verdadero si se persistió, falso si no.
     */
    boolean dbWrite(IErrorReg errorReg);

    /**
     * Persiste un error en un tipo de log específico.
     *
     * @param <T> tipo del registro de log.
     * @param logType clase del registro de log.
     * @param sessionId identificador de la sesión del usuario.
     * @param errorReg registro de error.
     * @return verdadero si se persistió, falso si no.
     */
    <T extends IAppLogRecord> boolean dbWrite(Class<T> logType, String sessionId, IErrorReg errorReg);

    /**
     * Persiste un registro de log ya construido.
     *
     * @param <T> tipo del registro de log.
     * @param logRecord registro de log a persistir.
     * @param sessionId identificador de la sesión del usuario.
     * @return verdadero si se persistió, falso si no.
     */
    <T extends IAppLogRecord> boolean dbWrite(T logRecord, String sessionId);

    /**
     * Crea una nueva instancia de registro de log del tipo indicado.
     *
     * @param <T> tipo del registro de log.
     * @param logType clase del registro de log.
     * @return nueva instancia de registro de log.
     */
    <T extends IAppLogRecord> IAppLogRecord getNewAppLogRecord(Class<T> logType);

    /**
     * Indica si una entidad debe auditarse, segun la tabla de auditoria del
     * catalogo ({@code AppAudit}).
     *
     * <p><b>Por que vive aca y no en el DAO.</b> La respuesta es configuracion,
     * no un dato de la operacion en curso, y hay que resolverla <i>fuera</i> de
     * la transaccion que esta grabando: el catalogo es otro origen de datos y,
     * con el suyo en {@code jta="true"}, consultarlo desde adentro enlista un
     * segundo recurso de una fase y Narayana aborta la transaccion
     * ({@code ARJUNA012140}). La implementacion esta anotada
     * {@code NOT_SUPPORTED} y se la invoca <b>a traves del contenedor</b>, que
     * es la unica forma de que esa anotacion tenga efecto: una llamada entre
     * metodos del mismo objeto no pasa por el proxy y la anotacion se ignora.</p>
     *
     * <p><b>Por que vive en el administrador de log y no en la configuracion.</b>
     * Estuvo en {@code IAppConfig} y hubo que sacarla: en las aplicaciones que
     * siembran parametros desde el {@code @PostConstruct} de ese singleton,
     * cada grabacion de la siembra volvia a entrar al bean por el proxy
     * mientras el contenedor lo estaba construyendo, y WildFly lo rechaza
     * ({@code WFLYEJB0132}). Este bean es {@code Stateless}, no tiene
     * inicializacion propia y, por lo tanto, <b>no se lo puede reentrar durante
     * su arranque</b> (hallazgo {@code E5-07} del plan JTACAT).</p>
     *
     * <p>El resultado se cachea por clave, porque la tabla tiene decenas de
     * filas y cambia solo cuando se reconfigura la auditoria, mientras que la
     * pregunta se hace <b>una vez por cada fila que se graba</b>.</p>
     *
     * @param entity1 nombre de la clase de la entidad, en minusculas.
     * @param entity2 nombre de la tabla de la entidad, en minusculas.
     * @param entity3 valor de {@code getAuditEntity()}, o {@code "*"}.
     * @return verdadero si alguno de los tres nombres esta marcado para auditar.
     * @throws RuntimeException si la consulta al catalogo falla; al cruzar el
     * contenedor el llamador la recibe envuelta en {@code EJBException}.
     * <b>No devuelve falso ante un error</b>: hacerlo dejaba registros sin
     * auditar en silencio.
     */
    boolean isEntityAuditAble(String entity1, String entity2, String entity3);

    /**
     * Vacia el cache de {@link #isEntityAuditAble(String, String, String)}.
     * Hay que llamarlo despues de modificar la tabla de auditoria del catalogo,
     * o los cambios no se ven hasta reiniciar.
     *
     * <p>El cache es uno por despliegue: en una aplicacion repartida en varios
     * artefactos (por ejemplo un EAR y dos WAR) hay que vaciarlo en cada uno, o
     * reiniciar el servidor.</p>
     */
    void clearAuditCache();
}
