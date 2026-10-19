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
package org.javabeanstack.data;

import jakarta.persistence.EntityManager;

/**
 * Auditoría de las grabaciones: decide si una entidad se audita y escribe su
 * fila de auditoría (plan AUDMGR).
 *
 * <p>Reúne lo que estaba repartido entre el administrador de log (la consulta
 * a la tabla {@code AppAudit} del catálogo y su caché) y el DAO (la resolución
 * de los nombres de la entidad y la escritura de la fila). El DAO solo
 * delega.</p>
 *
 * <p><b>Dos reglas de transacción que la implementación respeta y que quien
 * la use no debe romper:</b></p>
 * <ol>
 * <li><b>La decisión corre fuera de la transacción que graba.</b> La
 * respuesta es configuración, no un dato de la operación en curso, y el
 * catálogo es otro origen de datos: con el suyo en {@code jta="true"},
 * consultarlo desde adentro enlista un segundo recurso de una fase y Narayana
 * aborta la transacción ({@code ARJUNA012140}). Los métodos de decisión están
 * anotados {@code NOT_SUPPORTED}, y eso solo tiene efecto si la llamada cruza
 * el proxy del contenedor. Por eso {@link #write} <b>no decide</b>: pregunta
 * el llamador, a través del proxy, y después pide escribir.</li>
 * <li><b>La fila de auditoría va con la de datos.</b> {@link #write} usa el
 * {@code EntityManager} que recibe, dentro de la transacción del llamador
 * ({@code MANDATORY}): se graban las dos o ninguna.</li>
 * </ol>
 *
 * <p>Es un bean {@code Stateless} sin inicialización propia: no se lo puede
 * reentrar durante su arranque. La consulta estuvo en la configuración y hubo
 * que sacarla de ahí porque, en las aplicaciones que siembran parámetros
 * desde el {@code @PostConstruct} de ese singleton, cada grabación volvía a
 * entrar al bean mientras el contenedor lo construía ({@code WFLYEJB0132},
 * hallazgo {@code E5-07} del plan JTACAT). Después estuvo en el administrador
 * de log, que no era su oficio.</p>
 *
 * @author Jorge Enciso
 */
public interface IAuditManager {

    /**
     * Indica si una entidad se audita, resolviendo sus tres nombres: el de la
     * clase y el de la tabla, en minúsculas, y el de {@code getAuditEntity()}
     * (o {@code "*"} si no lo tiene). Una entidad sin clase de auditoría no se
     * audita, y una marcada con {@code isAuditAble()} sí, sin consultar.
     *
     * @param <T> tipo de la entidad.
     * @param ejb entidad.
     * @return verdadero si se audita.
     * @throws RuntimeException si la consulta al catálogo falla (ver
     * {@link #isEntityAuditAble}).
     */
    <T extends IDataRow> boolean isAuditAble(T ejb);

    /**
     * Indica si una entidad se audita, a partir de su clase (ver
     * {@link #isAuditAble(IDataRow)}).
     *
     * @param <T> tipo de la entidad.
     * @param clazz clase de la entidad.
     * @return verdadero si se audita; falso también si la clase no se puede
     * instanciar.
     * @throws RuntimeException si la consulta al catálogo falla: dejarlo pasar
     * equivaldría a no auditar en silencio.
     */
    <T extends IDataRow> boolean isAuditAble(Class<IDataRow> clazz);

    /**
     * Indica si una entidad se audita, por un único nombre.
     *
     * @param <T> tipo de la entidad.
     * @param entityName nombre de la entidad en {@code AppAudit}.
     * @return verdadero si se audita.
     */
    <T extends IDataRow> boolean isAuditAble(String entityName);

    /**
     * Indica si alguno de los tres nombres está marcado para auditar en la
     * tabla de auditoría del catálogo ({@code AppAudit}).
     *
     * <p>El resultado se cachea por terna, porque la tabla tiene decenas de
     * filas y cambia solo cuando se reconfigura la auditoría, mientras que la
     * pregunta se hace <b>una vez por cada fila que se graba</b>.</p>
     *
     * @param entity1 nombre de la clase de la entidad, en minúsculas.
     * @param entity2 nombre de la tabla de la entidad, en minúsculas.
     * @param entity3 valor de {@code getAuditEntity()}, o {@code "*"}.
     * @return verdadero si alguno de los tres nombres está marcado.
     * @throws RuntimeException si la consulta al catálogo falla; al cruzar el
     * contenedor el llamador la recibe envuelta en {@code EJBException}.
     * <b>No devuelve falso ante un error</b>: hacerlo dejaba registros sin
     * auditar en silencio.
     */
    boolean isEntityAuditAble(String entity1, String entity2, String entity3);

    /**
     * Vacía la caché de {@link #isEntityAuditAble}. Hay que llamarlo después
     * de modificar la tabla de auditoría del catálogo, o los cambios no se ven
     * hasta reiniciar.
     *
     * <p>La caché es una por despliegue: en una aplicación repartida en varios
     * artefactos (por ejemplo un EAR y dos WAR) hay que vaciarla en cada uno, o
     * reiniciar el servidor.</p>
     */
    void clearAuditCache();

    /**
     * Escribe la fila de auditoría del cambio hecho sobre la entidad. No
     * decide si la entidad se audita: lo decide el llamador antes, con
     * {@link #isAuditAble(IDataRow)} (ver la regla 1 de esta interfaz).
     *
     * <p>Es el <b>único</b> escritor de la cabecera de auditoría en todas las
     * capas. La cabecera separa el origen de la grabación en
     * {@code iprequest} (la IP del request) y {@code device} (columna
     * {@code maquina}: {@code AuditHeader.formatDevice(String, String)}, de {@code jbs-business}), y
     * los escribe solo si la clase de auditoría los tiene (compatibilidad hacia
     * atrás con el modelo de la aplicación; ver {@code AuditHeader}).</p>
     *
     * @param <T> tipo de la entidad.
     * @param em entity manager de la grabación: la fila de auditoría se
     * persiste con él, en la misma transacción.
     * @param sessionId identificador de la sesión del usuario.
     * @param ejb entidad grabada, con su acción todavía puesta.
     * @throws Exception si falla el registro de auditoría.
     */
    <T extends IDataRow> void write(EntityManager em, String sessionId, T ejb) throws Exception;
}
