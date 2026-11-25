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
package org.javabeanstack.sifen.core.validation;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.javabeanstack.sifen.core.beans.EventosDE;
import org.javabeanstack.sifen.core.fields.request.event.TrGesEve;
import org.javabeanstack.sifen.core.validation.rules.event.EventRuleSets;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;

/**
 * Validación previa de los eventos ({@code rEve}, tabla «Eventos» del catálogo): el mismo motor y el
 * mismo contrato que {@link DEValidator}, con la raíz {@link TrGesEve} (I3-06). Las rutas parten de
 * {@code rEve}; los errores internos del motor ({@code JBS-VAL-*}) también.
 *
 * <pre>{@code
 * ValidationResult r = EventValidator.standard().validate(eventosDE, generationCtx);
 * }</pre>
 */
public final class EventValidator {
    /** Raíz de las rutas de los eventos. */
    public static final String ROOT = "rEve";

    /** Conjuntos de eventos del catálogo ({@link EventRuleSets#STANDARD}, D33). */
    private static final List<GroupValidator<TrGesEve>> GRUPOS_ESTANDAR = List.copyOf(EventRuleSets.STANDARD);

    static {
        // I3-25: el catálogo verifica códigos, grupos y rutas al cargarse; se fuerza con el validador para
        // que una definición inconsistente falle en la primera carga y no solo en CatalogoReglasTest.
        RuleCatalog.all();
    }

    private static final EventValidator ESTANDAR = new EventValidator(ValidationEngine.de(GRUPOS_ESTANDAR, false,
            null, ROOT));

    private final ValidationEngine<TrGesEve> motor;

    private EventValidator(ValidationEngine<TrGesEve> motor) {
        this.motor = motor;
    }

    /**
     * Devuelve el validador con los grupos del catálogo.
     *
     * @return instancia compartida.
     */
    public static EventValidator standard() {
        return ESTANDAR;
    }

    /**
     * Crea un constructor vacío.
     *
     * @return el constructor.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Valida un evento.
     *
     * @param evento        evento a validar; {@code null} da {@link ValidationRule#JBS_VAL_01}.
     * @param generationCtx contexto de generación; {@code null} da el aviso {@code JBS-VAL-02}.
     * @return el resultado; nunca {@code null}.
     */
    public ValidationResult validate(TrGesEve evento, GenerationCtx generationCtx) {
        return motor.validar(evento, generationCtx);
    }

    /**
     * Valida todos los eventos de un envío y junta los resultados; cada ruta lleva el índice del
     * evento ({@code rGesEve[1].rEve.gGroupTiEvt…}, base 0).
     *
     * @param eventos       envío de eventos; {@code null} o sin eventos da {@code JBS-VAL-01}.
     * @param generationCtx contexto de generación.
     * @return el resultado combinado; nunca {@code null}.
     */
    public ValidationResult validate(EventosDE eventos, GenerationCtx generationCtx) {
        return validate(eventos, generationCtx, null, null);
    }

    /**
     * Valida todos los eventos de un envío suponiendo la fecha de firma y el {@code Id} con que los enviaría
     * {@code ISifenClient.sendEvents} si no los traen: es la validación previa de
     * {@code ISifenClient.validateEvents} (I8-34, decisión del usuario ESC-35). En cada evento, si trae
     * {@code dFecFirma} se valida la suya y si no las reglas de {@code dFecFirma} ({@code JBS-EVENTO-02},
     * {@code GDE004}) evalúan la supuesta; si trae {@code Id} (no nulo ni en blanco) se valida el suyo y si no
     * {@code JBS-EVENTO-01} evalúa el supuesto. Los eventos <b>no se modifican</b>.
     *
     * @param eventos              envío de eventos; {@code null} o sin eventos da {@code JBS-VAL-01}.
     * @param generationCtx        contexto de generación.
     * @param defaultSignatureDate {@code dFecFirma} supuesta si un evento no la cargó; {@code null} = ninguna.
     * @param defaultEventId       {@code Id} supuesto si un evento no lo cargó; {@code null} = ninguno (con los dos
     *                             nulos es igual que {@link #validate(EventosDE, GenerationCtx)}).
     * @return el resultado combinado; nunca {@code null}.
     */
    public ValidationResult validate(EventosDE eventos, GenerationCtx generationCtx, LocalDateTime defaultSignatureDate,
            String defaultEventId) {
        if (eventos == null || eventos.getrGesEveList() == null || eventos.getrGesEveList().isEmpty()) {
            return motor.validar(null, generationCtx);
        }
        List<ValidationResult> resultados = new ArrayList<>();
        List<TrGesEve> lista = eventos.getrGesEveList();
        for (int i = 0; i < lista.size(); i++) {
            resultados.add(motor.validar(lista.get(i), generationCtx, defaultSignatureDate, defaultEventId)
                    .withPrefix("rGesEve[" + i + "]"));
        }
        return ValidationResult.combine(resultados);
    }

    /**
     * Devuelve los grupos registrados, en orden.
     *
     * @return lista inmodificable.
     */
    public List<RuleSet<TrGesEve, ?>> getRuleSets() {
        List<RuleSet<TrGesEve, ?>> l = new ArrayList<>();
        for (GroupValidator<TrGesEve> g : motor.getGrupos()) {
            if (g instanceof RuleSet<TrGesEve, ?> c) {
                l.add(c);
            }
        }
        return Collections.unmodifiableList(l);
    }

    /**
     * Devuelve los grupos registrados, en orden.
     *
     * @return lista inmodificable.
     */
    public List<GroupValidator<TrGesEve>> getGroups() {
        return motor.getGrupos();
    }

    /** Constructor de {@link EventValidator} (mismas opciones que {@link DEValidator.Builder}). */
    public static final class Builder {
        private final List<GroupValidator<TrGesEve>> grupos = new ArrayList<>();
        private boolean estricto;
        private Clock reloj;
        private final Set<String> desactivadas = new LinkedHashSet<>();
        private LocalDate fechaReglas;

        private Builder() {
        }

        /**
         * Agrega los grupos del catálogo.
         *
         * @return este constructor.
         */
        public Builder registerStandard() {
            grupos.addAll(GRUPOS_ESTANDAR);
            return this;
        }

        /**
         * Agrega un grupo al final.
         *
         * @param grupo grupo de reglas; no nulo.
         * @return este constructor.
         */
        public Builder register(GroupValidator<TrGesEve> grupo) {
            grupos.add(Objects.requireNonNull(grupo, "grupo"));
            return this;
        }

        /**
         * Modo estricto, solo para pruebas (ver {@link DEValidator.Builder#strict()}).
         *
         * @return este constructor.
         */
        public Builder strict() {
            this.estricto = true;
            return this;
        }

        /**
         * Fija el reloj de las reglas de plazo (pruebas).
         *
         * @param reloj reloj; {@code null} = el del sistema.
         * @return este constructor.
         */
        public Builder clock(Clock reloj) {
            this.reloj = reloj;
            return this;
        }

        /**
         * Desactiva reglas por código (D33): no se evalúan en este validador. Se suman a las de
         * {@code SifenConfig.validacionReglasDesactivadas}. {@code JBS-VAL-00/01/02} no se pueden
         * desactivar.
         *
         * @param codigos códigos de regla.
         * @return este constructor.
         */
        public Builder disable(String... codigos) {
            for (String c : codigos) {
                desactivadas.add(Objects.requireNonNull(c, "codigo"));
            }
            return this;
        }

        /**
         * Fija la fecha de referencia de las reglas (qué reglas rigen, {@link EffectivePeriod}); por encima de
         * la de la configuración.
         *
         * @param fecha fecha; {@code null} = la de la configuración o la de hoy.
         * @return este constructor.
         */
        public Builder rulesDate(LocalDate fecha) {
            this.fechaReglas = fecha;
            return this;
        }

        /**
         * Construye el validador.
         *
         * @return validador inmutable.
         */
        public EventValidator build() {
            ValidationEngine.verificarDesactivadas(desactivadas, grupos, estricto);
            return new EventValidator(ValidationEngine.de(Collections.unmodifiableList(grupos), estricto, reloj, ROOT,
                    desactivadas, fechaReglas));
        }
    }
}
