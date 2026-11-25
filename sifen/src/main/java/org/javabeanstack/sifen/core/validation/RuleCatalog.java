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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.javabeanstack.sifen.core.validation.rules.de.DERuleSets;
import org.javabeanstack.sifen.core.validation.rules.event.EventRuleSets;
import org.javabeanstack.sifen.core.validation.rules.config.ConfigRuleSets;

/**
 * El catálogo de reglas en código (D33): se arma una vez desde las listas estándar
 * ({@link DERuleSets#STANDARD}, {@link EventRuleSets#STANDARD}) más las reglas del motor, y falla al
 * cargar la clase si un código está repetido o mal formado, si una regla {@code JBS-} no lleva el
 * nombre de su grupo o si su ruta no cuelga de la base de su conjunto. Es la fuente del catálogo
 * {@code .md} ({@link #exportTsv()}, que lee {@code sifen_catalogo_reglas.py --generar-md}).
 */
public final class RuleCatalog {
    /**
     * Forma de un código: ID del MT ({@code D208c}, {@code GENFE011}), {@code JBS-<grupo>-<nn>} (de la
     * librería) o {@code APP-<grupo>-<nn>} (reservado al consumidor; I3-21).
     */
    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z]{1,5}[0-9]{2,3}[a-z]?|(JBS|APP)-[A-Z]{1,6}-[0-9]{2,3}");

    private static final Map<String, ValidationRule> REGLAS;
    private static final Map<String, String> CONJUNTO_DE;

    static {
        Map<String, ValidationRule> reglas = new LinkedHashMap<>();
        Map<String, String> conjuntoDe = new LinkedHashMap<>();
        for (ValidationRule r : List.of(ValidationRule.JBS_VAL_00, ValidationRule.JBS_VAL_01, ValidationRule.JBS_VAL_02)) {
            reglas.put(r.getCode(), r);
            conjuntoDe.put(r.getCode(), "motor");
        }
        List<RuleSet<?, ?>> conjuntos = new ArrayList<>();
        conjuntos.addAll(DERuleSets.STANDARD);
        conjuntos.addAll(EventRuleSets.STANDARD);
        conjuntos.addAll(ConfigRuleSets.STANDARD);
        conjuntos.addAll(ConfigRuleSets.DE_CONSISTENCY);
        for (RuleSet<?, ?> c : conjuntos) {
            for (ValidationRule r : c.getDeclaredRules()) {
                String problema = problemaDeDefinicion(r, c, reglas);
                if (problema != null) {
                    throw new IllegalStateException("Catálogo de reglas inconsistente: " + problema);
                }
                reglas.put(r.getCode(), r);
                conjuntoDe.put(r.getCode(), c.getName());
            }
        }
        REGLAS = Collections.unmodifiableMap(reglas);
        CONJUNTO_DE = Collections.unmodifiableMap(conjuntoDe);
    }

    private RuleCatalog() {
    }

    /**
     * Comprueba una ficha de un conjunto contra lo ya registrado.
     *
     * @return {@code null} si es válida, o el motivo del rechazo.
     */
    static String problemaDeDefinicion(ValidationRule r, RuleSet<?, ?> c, Map<String, ValidationRule> registradas) {
        String codigo = r.getCode();
        if (!isWellFormedCode(codigo)) {
            return "código de regla mal formado: " + codigo;
        }
        if (codigo.startsWith("APP-")) {
            return "el código " + codigo + " está reservado a las reglas del consumidor";
        }
        if (codigo.startsWith("JBS-") && !codigo.startsWith("JBS-" + r.getGroup().name() + "-")) {
            return "la regla " + codigo + " no lleva el nombre de su grupo " + r.getGroup().name();
        }
        if (registradas.containsKey(codigo)) {
            return "código de regla repetido: " + codigo;
        }
        String base = c.getBasePath();
        if (!base.isEmpty() && !r.getPath().equals(base) && !r.getPath().startsWith(base + ".")) {
            return "la ruta de " + codigo + " (" + r.getPath() + ") no cuelga de la base de " + c.getName() + " (" + base + ")";
        }
        return null;
    }

    /**
     * Indica si un código tiene la forma de las reglas de la librería.
     *
     * @param codigo código.
     * @return {@code true} si es un ID del MT, {@code JBS-<grupo>-<nn>} o {@code APP-<grupo>-<nn>}.
     */
    public static boolean isWellFormedCode(String codigo) {
        return codigo != null && CODE_PATTERN.matcher(codigo).matches();
    }

    /**
     * Devuelve todas las reglas del catálogo (motor, DE y eventos), en orden.
     *
     * @return lista inmodificable.
     */
    public static List<ValidationRule> all() {
        return List.copyOf(REGLAS.values());
    }

    /**
     * Busca una regla por su código.
     *
     * @param codigo código; {@code null} da vacío.
     * @return la regla, o vacío.
     */
    public static Optional<ValidationRule> byCode(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(REGLAS.get(codigo));
    }

    /**
     * Devuelve las reglas que no se evalúan localmente, con su motivo (I3-15).
     *
     * @return mapa inmodificable, en orden del catálogo.
     */
    public static Map<ValidationRule, NotEvaluatedReason> notEvaluated() {
        Map<ValidationRule, NotEvaluatedReason> m = new LinkedHashMap<>();
        for (ValidationRule r : REGLAS.values()) {
            if (!r.isEvaluable()) {
                m.put(r, r.getNotEvaluatedReason());
            }
        }
        return Collections.unmodifiableMap(m);
    }

    /**
     * Devuelve las reglas vigentes en una fecha.
     *
     * @param fecha fecha de referencia.
     * @return lista inmodificable.
     */
    public static List<ValidationRule> inForceOn(LocalDate fecha) {
        List<ValidationRule> l = new ArrayList<>();
        for (ValidationRule r : REGLAS.values()) {
            if (r.isInForceOn(fecha)) {
                l.add(r);
            }
        }
        return Collections.unmodifiableList(l);
    }

    /**
     * Devuelve el nombre del conjunto que declara una regla ({@code motor} para {@code JBS-VAL-00/01/02}).
     *
     * @param codigo código.
     * @return el nombre, o {@code null}.
     */
    public static String ruleSetOf(String codigo) {
        return CONJUNTO_DE.get(codigo);
    }

    /**
     * Exporta el catálogo como TSV para {@code sifen_catalogo_reglas.py --generar-md}: una línea por
     * regla con código, grupo, conjunto, ruta, severidad ({@code R}/{@code N}), {@code dCodRes},
     * política ({@code O}/{@code L}/{@code —}), fuentes, desde, hasta, motivo de baja, motivo de no
     * evaluada y código reemplazado.
     *
     * @return el TSV con encabezado.
     */
    public static String exportTsv() {
        StringBuilder sb = new StringBuilder(
                "codigo\tgrupo\tconjunto\truta\tsev\tdcodres\tvalor\tfuentes\tdesde\thasta\tmotivo_baja\tno_evaluada\treemplaza\n");
        for (ValidationRule r : REGLAS.values()) {
            StringBuilder f = new StringBuilder();
            for (RuleSource fu : r.getSources()) {
                f.append(f.length() == 0 ? "" : ", ").append(fu.getName());
            }
            EffectivePeriod v = r.getEffectivePeriod();
            sb.append(r.getCode()).append('\t').append(r.getGroup().name()).append('\t').append(CONJUNTO_DE.get(r.getCode()))
                    .append('\t').append(r.getPath()).append('\t').append(r.getSeverity() == Severity.ERROR ? "R" : "N")
                    .append('\t').append(nulo(r.getSifenResponseCode())).append('\t').append(politica(r.getValuePolicy()))
                    .append('\t').append(f).append('\t').append(nulo(v.from())).append('\t').append(nulo(v.until()))
                    .append('\t').append(nulo(v.endReason())).append('\t')
                    .append(r.getNotEvaluatedReason() == null ? "" : r.getNotEvaluatedReason().name()).append('\t')
                    .append(nulo(r.getReplaces())).append('\n');
        }
        return sb.toString();
    }

    private static String nulo(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static String politica(ValuePolicy p) {
        switch (p) {
            case OMIT:
                return "O";
            case LENGTH_ONLY:
                return "L";
            default:
                return "—";
        }
    }
}
