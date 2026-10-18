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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.w3c.dom.Document;

/**
 * Petición de
 * {@link DBManager#dbScriptUpdateExecute(IGenericDAO, org.javabeanstack.log.ILogManager, DbScriptUpdateRequest)}:
 * qué script ejecutar, con qué sesión corre cada sección y con qué política
 * de errores.
 *
 * <p><b>jbs no nombra secciones.</b> El atributo {@code dataconex} de cada
 * nodo {@code SQLCOMMAND} es un nombre lógico que define la aplicación (por
 * ejemplo «CATALOGO» y «DATOS»); el framework no le da significado. El
 * llamador dice, por cada valor de {@code dataconex}, con qué sesión corre el
 * nodo: {@link #getSessionByDataconex()}. Un nodo cuyo valor no está en el
 * mapa se saltea sin tocar la bitácora. Los nodos sin atributo se buscan con
 * la clave {@link #DATACONEX_UNSET}.</p>
 *
 * <p><b>Sesión nula = catálogo.</b> Tanto en el mapa como en
 * {@link #getLogSessionId()}, una sesión nula hace que el DAO use la unidad
 * de persistencia del catálogo ({@link IDBManager#CATALOGO}, «PU1»), porque
 * {@code getDBLinkInfo(null)} devuelve su vínculo.</p>
 *
 * <p>Es inmutable: se arma con {@link #builder(Document, Map)}. El mapa de
 * parámetros se copia, y el ejecutor trabaja sobre otra copia, así que el del
 * llamador nunca se modifica.</p>
 *
 * @author Jorge Enciso
 */
public final class DbScriptUpdateRequest {

    /**
     * Clave de {@link #getSessionByDataconex()} para los nodos
     * {@code SQLCOMMAND} sin atributo {@code dataconex} (o con el atributo
     * vacío): es lo que devuelve {@code Element.getAttribute} en ese caso.
     */
    public static final String DATACONEX_UNSET = "";

    private final Document script;
    private final Map<String, Object> parameters;
    private final String logSessionId;
    private final Map<String, String> sessionByDataconex;
    private final boolean continueWithError;
    private final boolean logRowOwned;
    private final String logRowOwner;

    private DbScriptUpdateRequest(Builder builder, Map<String, Object> parameters) {
        this.script = builder.script;
        this.parameters = Collections.unmodifiableMap(new HashMap<>(parameters));
        this.logSessionId = builder.logSessionId;
        this.sessionByDataconex = Collections.unmodifiableMap(new LinkedHashMap<>(builder.sessionByDataconex));
        this.continueWithError = builder.continueWithError;
        this.logRowOwned = builder.logRowOwned;
        this.logRowOwner = builder.logRowOwner;
    }

    /**
     * Crea el armador de una petición.
     *
     * @param script documento XML del paquete, con los nodos
     * {@code /ROOT/SCRIPTS/SQLCOMMAND}.
     * @param parameters parámetros SQL de la fila de la bitácora
     * ({@code dbversion}, {@code sequence}, {@code filename}, {@code script},
     * {@code appuser}) y, opcionalmente, {@code classLog} (entidad de la
     * bitácora; sin ella, {@code applogdbupdate}). Se copia.
     * @return el armador.
     * @throws NullPointerException si {@code script} o {@code parameters} son
     * nulos.
     */
    public static Builder builder(Document script, Map<String, Object> parameters) {
        return new Builder(script, parameters);
    }

    /**
     * Documento del paquete.
     *
     * @return el documento con los nodos {@code /ROOT/SCRIPTS}.
     */
    public Document getScript() {
        return script;
    }

    /**
     * Parámetros SQL de la fila de la bitácora (copia inmutable).
     *
     * @return los parámetros.
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     * Sesión con la que se escriben la bitácora ({@code {schema}} de esa
     * sesión) y los registros de error de {@code applogevent}.
     *
     * @return la sesión, o nulo para el catálogo (PU1).
     */
    public String getLogSessionId() {
        return logSessionId;
    }

    /**
     * Sesión de cada sección: valor de {@code dataconex} → sesión con la que
     * corren sus sentencias (nula = catálogo). Un nodo cuyo valor no es clave
     * del mapa se saltea. Copia inmutable; admite valores nulos.
     *
     * @return el mapa, nunca vacío.
     */
    public Map<String, String> getSessionByDataconex() {
        return sessionByDataconex;
    }

    /**
     * Política de errores, igual para todos los nodos: con falso, la primera
     * sentencia rechazada borra la fila de la bitácora, se registra y corta la
     * ejecución; con verdadero se registra y se continúa.
     *
     * @return verdadero si se toleran los errores.
     */
    public boolean isContinueWithError() {
        return continueWithError;
    }

    /**
     * Indica que el llamador ya insertó la fila de la bitácora (típicamente
     * como candado entre instancias) y que el ejecutor no debe insertarla.
     *
     * @return verdadero si la fila ya existe.
     */
    public boolean isLogRowOwned() {
        return logRowOwned;
    }

    /**
     * {@code appuser} dueño de la fila. Si no es nulo, el cierre
     * ({@code finished}) y la reversión ({@code DELETE}) de la fila exigen
     * {@code appuser = :logRowOwner}: una fila que otro tomó entretanto no se
     * cierra ni se borra.
     *
     * <p>Es el mismo dato que el {@code appuser} de {@link #getParameters()}
     * (el que escribe el {@code INSERT} de la fila): el armador los ata, de
     * modo que la fila que inserta el ejecutor siempre se puede cerrar
     * (M6-08).</p>
     *
     * @return el dueño, o nulo para no exigirlo.
     */
    public String getLogRowOwner() {
        return logRowOwner;
    }

    /**
     * Armador de {@link DbScriptUpdateRequest}.
     */
    public static final class Builder {

        private final Document script;
        private final Map<String, Object> parameters;
        private String logSessionId;
        private final Map<String, String> sessionByDataconex = new LinkedHashMap<>();
        private boolean continueWithError;
        private boolean logRowOwned;
        private String logRowOwner;

        private Builder(Document script, Map<String, Object> parameters) {
            this.script = Objects.requireNonNull(script, "script");
            this.parameters = Objects.requireNonNull(parameters, "parameters");
        }

        /**
         * Sesión de la bitácora; por omisión nula (catálogo).
         *
         * @param logSessionId sesión, o nulo para el catálogo.
         * @return este armador.
         */
        public Builder logSessionId(String logSessionId) {
            this.logSessionId = logSessionId;
            return this;
        }

        /**
         * Agrega una sección: los nodos con ese valor de {@code dataconex}
         * corren con esa sesión.
         *
         * @param dataconex valor del atributo ({@link #DATACONEX_UNSET} para
         * los nodos sin atributo).
         * @param sessionId sesión de sus sentencias, o nulo para el catálogo.
         * @return este armador.
         * @throws NullPointerException si {@code dataconex} es nulo.
         */
        public Builder section(String dataconex, String sessionId) {
            sessionByDataconex.put(Objects.requireNonNull(dataconex, "dataconex"), sessionId);
            return this;
        }

        /**
         * Agrega varias secciones de una vez (ver {@link #section(String, String)}).
         *
         * @param sessions valor de {@code dataconex} → sesión.
         * @return este armador.
         * @throws NullPointerException si el mapa o alguna clave son nulos.
         */
        public Builder sessionByDataconex(Map<String, String> sessions) {
            for (Map.Entry<String, String> entry : sessions.entrySet()) {
                section(entry.getKey(), entry.getValue());
            }
            return this;
        }

        /**
         * Política de errores; por omisión falso (no se toleran).
         *
         * @param continueWithError verdadero para registrar y continuar.
         * @return este armador.
         */
        public Builder continueWithError(boolean continueWithError) {
            this.continueWithError = continueWithError;
            return this;
        }

        /**
         * Fila de la bitácora ya insertada por el llamador; por omisión falso.
         *
         * @param logRowOwned verdadero para que no se inserte.
         * @return este armador.
         */
        public Builder logRowOwned(boolean logRowOwned) {
            this.logRowOwned = logRowOwned;
            return this;
        }

        /**
         * Dueño de la fila que exigen el cierre y la reversión; por omisión
         * nulo (no se exige). Debe coincidir con el {@code appuser} de los
         * parámetros, o este faltar: en ese caso la petición lo toma del
         * dueño (ver {@link #build()}).
         *
         * @param logRowOwner {@code appuser} dueño, o nulo.
         * @return este armador.
         */
        public Builder logRowOwner(String logRowOwner) {
            this.logRowOwner = logRowOwner;
            return this;
        }

        /**
         * Arma la petición.
         *
         * <p><b>Dueño y {@code appuser}.</b> Con dueño
         * ({@link #logRowOwner(String)}), el {@code appuser} de los
         * parámetros es el que escribe el {@code INSERT} de la fila y el dueño
         * es el que exigen su cierre y su reversión: si fueran distintos, el
         * ejecutor insertaría una fila que después no puede cerrar ni borrar.
         * Por eso, si los parámetros no traen {@code appuser} (o lo traen
         * nulo) la petición usa el dueño; si lo traen, debe coincidir con el
         * dueño, sin contar los espacios de los extremos (como el {@code =}
         * de SQL con una columna {@code char(n)}). El mapa del llamador no se
         * modifica.</p>
         *
         * @return la petición inmutable.
         * @throws IllegalArgumentException si no se agregó ninguna sección (la
         * fila se insertaría y se cerraría sin ejecutar nada), o si el dueño y
         * el {@code appuser} de los parámetros difieren.
         */
        public DbScriptUpdateRequest build() {
            if (sessionByDataconex.isEmpty()) {
                throw new IllegalArgumentException("Se requiere al menos una sección (dataconex -> sesión)");
            }
            Map<String, Object> effective = parameters;
            if (logRowOwner != null) {
                Object appuser = parameters.get("appuser");
                if (appuser == null) {
                    effective = new HashMap<>(parameters);
                    effective.put("appuser", logRowOwner);
                } else if (!logRowOwner.trim().equals(appuser.toString().trim())) {
                    throw new IllegalArgumentException("El dueño de la fila (logRowOwner = " + logRowOwner
                            + ") difiere del appuser de los parámetros (" + appuser + ")");
                }
            }
            return new DbScriptUpdateRequest(this, effective);
        }
    }
}
