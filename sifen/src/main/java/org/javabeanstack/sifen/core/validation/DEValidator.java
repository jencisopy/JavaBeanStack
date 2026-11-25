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
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.validation.rules.de.DERuleSets;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;

/**
 * Validación previa de un {@link DocumentoElectronico}: corre los grupos de reglas registrados y
 * devuelve <b>todos</b> los incumplimientos juntos, antes de armar el XML y de firmar.
 *
 * <p>Inmutable y seguro entre hilos (si sus grupos cumplen el contrato de {@link GroupValidator}):
 * se construye una vez y se reutiliza. {@link #validate} nunca lanza por un dato del documento ni
 * devuelve {@code null}; el detalle de esa garantía está en el javadoc del paquete.</p>
 *
 * <p>Uso:</p>
 * <pre>{@code
 * DEValidator validador = DEValidator.standard();
 * ValidationResult resultado = validador.validate(de, generationCtx);
 * resultado.throwIfInvalid();                // SifenValidationException con la lista
 * }</pre>
 *
 * <p>Registro de grupos (orden = orden de ejecución y de los errores; nombres únicos):</p>
 * <pre>{@code
 * DEValidator validador = DEValidator.builder()
 *         .registerStandard()                // los grupos del catálogo (§3.3)
 *         .register(new MiGrupoValidator())  // GroupValidator<DocumentoElectronico>
 *         .build();
 * }</pre>
 */
public final class DEValidator {

    /** Conjuntos del catálogo ({@link DERuleSets#STANDARD}), en el orden del MT (D33). */
    private static final List<GroupValidator<DocumentoElectronico>> GRUPOS_ESTANDAR = List.copyOf(DERuleSets.STANDARD);

    static {
        // I3-25: el catálogo verifica códigos, grupos y rutas al cargarse; se fuerza con el validador para
        // que una definición inconsistente falle en la primera carga y no solo en CatalogoReglasTest.
        RuleCatalog.all();
    }

    private static final DEValidator ESTANDAR = new DEValidator(ValidationEngine.de(GRUPOS_ESTANDAR, false));

    private final ValidationEngine<DocumentoElectronico> motor;

    private DEValidator(ValidationEngine<DocumentoElectronico> motor) {
        this.motor = motor;
    }

    /**
     * Devuelve el validador con los grupos del catálogo de reglas.
     *
     * @return instancia compartida.
     */
    public static DEValidator standard() {
        return ESTANDAR;
    }

    /**
     * Crea un constructor vacío de validadores.
     *
     * @return el constructor.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Valida un documento electrónico.
     *
     * @param de            documento a validar; {@code null} da {@link ValidationRule#JBS_VAL_01}.
     * @param generationCtx contexto de generación (configuración, NT 013); {@code null} da el aviso
     *                      {@link ValidationRule#JBS_VAL_02} y valores por defecto.
     * @return el resultado con todos los incumplimientos; nunca {@code null}.
     * @throws IllegalStateException solo si el validador se construyó con
     *                               {@link Builder#strict()} y un grupo reporta una regla que no
     *                               declara (defecto del grupo, nunca del documento).
     */
    public ValidationResult validate(DocumentoElectronico de, GenerationCtx generationCtx) {
        return motor.validar(de, generationCtx);
    }

    /**
     * Valida un documento electrónico suponiendo la fecha de firma con que se lo firmaría si no la trae: es la
     * validación previa de {@code ISifenClient.validateDE} (I8-27), que valida como firmaría {@code sign}, que
     * completa {@code dFecFirma} con la hora de Asunción. Si el documento trae {@code dFecFirma}, se valida la
     * suya; si no, las reglas de {@code dFecFirma} ({@code JBS-A-03}, {@code A004a}, {@code A004b}) evalúan la
     * supuesta. El documento <b>no se modifica</b>: sigue sin {@code dFecFirma}.
     *
     * @param de                    documento a validar; {@code null} da {@link ValidationRule#JBS_VAL_01}.
     * @param generationCtx         contexto de generación, como en {@link #validate(DocumentoElectronico, GenerationCtx)}.
     * @param defaultSignatureDate  {@code dFecFirma} supuesta si el documento no la cargó; {@code null} = ninguna
     *                              (igual que {@link #validate(DocumentoElectronico, GenerationCtx)}).
     * @return el resultado con todos los incumplimientos; nunca {@code null}.
     * @throws IllegalStateException como {@link #validate(DocumentoElectronico, GenerationCtx)}.
     */
    public ValidationResult validate(DocumentoElectronico de, GenerationCtx generationCtx,
            LocalDateTime defaultSignatureDate) {
        return motor.validar(de, generationCtx, defaultSignatureDate);
    }

    /**
     * Devuelve los grupos registrados, en orden de ejecución.
     *
     * @return lista inmodificable.
     */
    public List<RuleSet<DocumentoElectronico, ?>> getRuleSets() {
        List<RuleSet<DocumentoElectronico, ?>> l = new ArrayList<>();
        for (GroupValidator<DocumentoElectronico> g : motor.getGrupos()) {
            if (g instanceof RuleSet<DocumentoElectronico, ?> c) {
                l.add(c);
            }
        }
        return Collections.unmodifiableList(l);
    }

    /**
     * Devuelve los grupos registrados (conjuntos del catálogo y grupos propios), en orden.
     *
     * @return lista inmodificable.
     */
    public List<GroupValidator<DocumentoElectronico>> getGroups() {
        return motor.getGrupos();
    }

    /**
     * Constructor de {@link DEValidator}. No es seguro entre hilos; el validador que construye sí.
     */
    public static final class Builder {
        private final List<GroupValidator<DocumentoElectronico>> grupos = new ArrayList<>();
        private boolean estricto;
        private Clock reloj;
        private final Set<String> desactivadas = new LinkedHashSet<>();
        private LocalDate fechaReglas;

        private Builder() {
        }

        /**
         * Agrega los grupos del catálogo, en su orden.
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
        public Builder register(GroupValidator<DocumentoElectronico> grupo) {
            grupos.add(Objects.requireNonNull(grupo, "grupo"));
            return this;
        }

        /**
         * Agrega varios grupos al final, en el orden dado.
         *
         * @param lista grupos de reglas; ni la lista ni sus elementos pueden ser nulos.
         * @return este constructor.
         */
        public Builder registerAll(List<? extends GroupValidator<DocumentoElectronico>> lista) {
            for (GroupValidator<DocumentoElectronico> grupo : Objects.requireNonNull(lista, "lista")) {
                register(grupo);
            }
            return this;
        }

        /**
         * Modo estricto, <b>solo para pruebas</b> (M3-10): si un grupo reporta una regla que no
         * declara en {@link GroupValidator#getRules()}, {@link DEValidator#validate} lanza
         * {@link IllegalStateException} en vez de solo avisar en el log. La prueba de mutación de
         * §3.4 debe usarlo.
         *
         * @return este constructor.
         */
        public Builder strict() {
            this.estricto = true;
            return this;
        }

        /**
         * Fija el reloj del que sale {@link ValidationContext#getNow()} (reglas de plazo, DEC-03);
         * para las pruebas.
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
         * @throws IllegalArgumentException si dos grupos tienen el mismo nombre o uno no tiene nombre.
         * @throws NullPointerException     si un grupo devuelve nombre o reglas nulos.
         */
        public DEValidator build() {
            ValidationEngine.verificarDesactivadas(desactivadas, grupos, estricto);
            return new DEValidator(ValidationEngine.de(Collections.unmodifiableList(grupos), estricto, reloj,
                    ValidationRule.DE_ROOT, desactivadas, fechaReglas));
        }
    }
}
