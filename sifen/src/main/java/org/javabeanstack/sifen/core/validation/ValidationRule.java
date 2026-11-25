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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * La <b>ficha</b> de una regla de validación (D32, D33): su identidad y sus datos, sin lógica.
 *
 * <ul>
 * <li>{@link #getCode() código} estable: el ID del MT ({@code D208c}) o {@code JBS-<grupo>-<nn>}
 * (de la librería) o {@code APP-<grupo>-<nn>} (del consumidor). Un código asignado no se reasigna;</li>
 * <li>{@link #getGroup() grupo} del MT y {@link #getPath() ruta canónica} (con {@code []} en las listas);</li>
 * <li>{@link #getSeverity() severidad}, {@link #getSifenResponseCode() dCodRes} y
 * {@link #getValuePolicy() política del valor rechazado};</li>
 * <li>{@link #getSources() fuentes} y {@link #getEffectivePeriod() vigencia} (desde/hasta/motivo de baja);</li>
 * <li>{@link #getNotEvaluatedReason() motivo} si la regla no se evalúa localmente, y el código que
 * {@link #getReplaces() reemplaza}, si la regla es la versión propia de otra.</li>
 * </ul>
 *
 * <p>La ficha se declara <b>junto a su chequeo</b> en el conjunto de su subgrupo
 * ({@link RuleSet#declare}); el catálogo completo lo arma {@link RuleCatalog}. El mensaje
 * vive en {@link ValidationMessages} con la clave del código (las reglas propias del consumidor pueden
 * traer el texto en la ficha, {@link Builder#message}). Las reglas del motor ({@link #JBS_VAL_00},
 * {@link #JBS_VAL_01}, {@link #JBS_VAL_02}) son constantes de esta clase.</p>
 */
public final class ValidationRule {
    /** Índice concreto dentro de una ruta: {@code [3]}. */
    private static final Pattern INDICE = Pattern.compile("\\[[0-9]+\\]");

    /** Raíz de las rutas del DE; nombra el documento entero en las reglas internas. */
    public static final String DE_ROOT = "DE";
    /** Raíz de las rutas de la configuración y del certificado (D33, §3.7). */
    public static final String CONFIG_ROOT = "sifenConfig";
    /** Raíces admitidas como ruta de una regla interna ({@code JBS-VAL-*}): DE, evento (I3-06) y configuración. */
    public static final Set<String> ROOTS = Set.of(DE_ROOT, "rEve", CONFIG_ROOT);

    /**
     * Una regla lanzó una excepción no prevista (por ejemplo una {@link NullPointerException}): el
     * motor la convierte en este error en vez de propagarla; la prueba de mutación exige que <b>nunca</b>
     * aparezca. Argumentos: {@code {0}} conjunto (y código de la regla), {@code {1}} clase de la
     * excepción (I3-12).
     */
    public static final ValidationRule JBS_VAL_00 = new ValidationRule("JBS-VAL-00", RuleGroup.VAL, DE_ROOT,
            Severity.ERROR, null, ValuePolicy.OMIT, false);

    /** No se recibió documento ({@code null}). */
    public static final ValidationRule JBS_VAL_01 = new ValidationRule("JBS-VAL-01", RuleGroup.VAL, DE_ROOT,
            Severity.ERROR, null, ValuePolicy.OMIT, false);

    /** Se validó con {@code GenerationCtx} nulo: valores por defecto (aviso). */
    public static final ValidationRule JBS_VAL_02 = new ValidationRule("JBS-VAL-02", RuleGroup.VAL, DE_ROOT,
            Severity.WARNING, null, ValuePolicy.OMIT, false);

    private final String codigo;
    private final RuleGroup grupo;
    private final String ruta;
    private final Severity severidad;
    private final String codigoRespuestaSifen;
    private final ValuePolicy politicaValor;
    private final List<RuleSource> fuentes;
    private final EffectivePeriod vigencia;
    private final NotEvaluatedReason motivoNoEvaluada;
    private final String reemplaza;
    private final String mensajePropio;
    private final boolean desactivable;

    /**
     * Construye una ficha sin fuente ni vigencia propias (las del motor y las reglas ficticias de las
     * pruebas del contrato). Las del catálogo se arman con {@link Builder}.
     */
    ValidationRule(String codigo, RuleGroup grupo, String ruta, Severity severidad,
            String codigoRespuestaSifen, ValuePolicy politicaValor) {
        this(codigo, grupo, ruta, severidad, codigoRespuestaSifen, politicaValor, List.of(RuleSource.MT_V150),
                EffectivePeriod.ALWAYS, null, null, null, true);
    }

    /** Las reglas del motor: sin fuente propia y no desactivables. */
    private ValidationRule(String codigo, RuleGroup grupo, String ruta, Severity severidad,
            String codigoRespuestaSifen, ValuePolicy politicaValor, boolean desactivable) {
        this(codigo, grupo, ruta, severidad, codigoRespuestaSifen, politicaValor, List.of(RuleSource.MT_V150),
                EffectivePeriod.ALWAYS, null, null, null, desactivable);
    }

    private ValidationRule(String codigo, RuleGroup grupo, String ruta, Severity severidad,
            String codigoRespuestaSifen, ValuePolicy politicaValor, List<RuleSource> fuentes, EffectivePeriod vigencia,
            NotEvaluatedReason motivoNoEvaluada, String reemplaza, String mensajePropio, boolean desactivable) {
        this.desactivable = desactivable;
        this.codigo = Objects.requireNonNull(codigo, "codigo");
        this.grupo = Objects.requireNonNull(grupo, "grupo");
        this.ruta = Objects.requireNonNull(ruta, "ruta");
        this.severidad = Objects.requireNonNull(severidad, "severidad");
        this.codigoRespuestaSifen = codigoRespuestaSifen;
        this.politicaValor = politicaValor == null ? ValuePolicy.OMIT : politicaValor;
        this.fuentes = List.copyOf(fuentes);
        this.vigencia = vigencia == null ? EffectivePeriod.ALWAYS : vigencia;
        this.motivoNoEvaluada = motivoNoEvaluada;
        this.reemplaza = reemplaza;
        this.mensajePropio = mensajePropio;
        if (ruta.isBlank()) {
            throw new IllegalArgumentException("La regla " + codigo + " no tiene ruta");
        }
    }

    /** Copia con el motivo de no evaluada (lo fija {@link RuleSet#declareNotEvaluated}). */
    ValidationRule conMotivo(NotEvaluatedReason motivo) {
        return new ValidationRule(codigo, grupo, ruta, severidad, codigoRespuestaSifen, politicaValor, fuentes,
                vigencia, motivo, reemplaza, mensajePropio, desactivable);
    }

    /**
     * Quita los índices de una ruta concreta: {@code gDtipDE.gCamItem[3].dCodInt} →
     * {@code gDtipDE.gCamItem[].dCodInt}.
     *
     * @param ruta ruta con o sin índices; {@code null} da {@code null}.
     * @return la ruta canónica.
     */
    public static String normalizePath(String ruta) {
        if (ruta == null) {
            return null;
        }
        return INDICE.matcher(ruta).replaceAll("[]");
    }

    /**
     * Arma la ruta concreta reemplazando, en orden, cada {@code []} por el índice correspondiente (base
     * 0). Si hay menos índices que {@code []}, los restantes quedan sin índice; los sobrantes se ignoran.
     *
     * @param indices índices base 0, del grupo más externo al más interno.
     * @return la ruta concreta.
     */
    public String path(int... indices) {
        if (indices == null || indices.length == 0) {
            return ruta;
        }
        StringBuilder sb = new StringBuilder(ruta.length() + 8);
        int desde = 0;
        int usados = 0;
        int pos = ruta.indexOf("[]", desde);
        while (pos >= 0 && usados < indices.length) {
            sb.append(ruta, desde, pos).append('[').append(indices[usados]).append(']');
            usados++;
            desde = pos + 2;
            pos = ruta.indexOf("[]", desde);
        }
        sb.append(ruta, desde, ruta.length());
        return sb.toString();
    }

    /**
     * Devuelve el mensaje de la regla: el propio de la ficha, si lo trae, o el de
     * {@link ValidationMessages}.
     *
     * @param args detalles para los marcadores {@code {0}}, {@code {1}}… (nunca datos del documento).
     * @return el mensaje en español; nunca {@code null}.
     */
    public String message(Object... args) {
        if (mensajePropio != null) {
            String texto = mensajePropio;
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    texto = texto.replace("{" + i + "}", String.valueOf(args[i]));
                }
            }
            return texto;
        }
        return ValidationMessages.message(codigo, args);
    }

    /**
     * Indica si la regla rige en una fecha ({@link EffectivePeriod#isInForceOn}).
     *
     * @param fecha fecha de referencia de las reglas.
     * @return {@code true} si rige.
     */
    public boolean isInForceOn(LocalDate fecha) {
        return vigencia.isInForceOn(fecha);
    }

    /**
     * Indica si la regla se evalúa localmente.
     *
     * @return {@code false} si tiene {@linkplain #getNotEvaluatedReason() motivo}.
     */
    public boolean isEvaluable() {
        return motivoNoEvaluada == null;
    }

    /**
     * Indica si la regla se puede desactivar ({@code disable(...)} del validador o
     * {@code sifen.validacion_reglas_desactivadas}). Las guardas anti-NPE no (M3-15): sin ellas la
     * librería vuelve a fallar al armar el XML.
     *
     * @return {@code false} si es una guarda o una regla del motor.
     */
    public boolean isDisableable() {
        return desactivable;
    }

    /**
     * El código estable.
     *
     * @return el código estable.
     */
    public String getCode() {
        return codigo;
    }

    /**
     * El grupo del MT.
     *
     * @return el grupo del MT.
     */
    public RuleGroup getGroup() {
        return grupo;
    }

    /**
     * La ruta canónica, p. ej. {@code gDatGralOpe.gDatRec.dRucRec}; {@code DE} para el documento.
     *
     * @return la ruta canónica, p. ej. {@code gDatGralOpe.gDatRec.dRucRec}; {@code DE} para el documento.
     */
    public String getPath() {
        return ruta;
    }

    /**
     * {@code ERROR} o {@code WARNING}.
     *
     * @return {@code ERROR} o {@code WARNING}.
     */
    public Severity getSeverity() {
        return severidad;
    }

    /**
     * El {@code dCodRes} con que SIFEN rechazaría, o {@code null} si la regla es propia.
     *
     * @return el {@code dCodRes} con que SIFEN rechazaría, o {@code null} si la regla es propia.
     */
    public String getSifenResponseCode() {
        return codigoRespuestaSifen;
    }

    /**
     * Qué parte del valor rechazado se conserva; nunca {@code null}.
     *
     * @return qué parte del valor rechazado se conserva; nunca {@code null}.
     */
    public ValuePolicy getValuePolicy() {
        return politicaValor;
    }

    /**
     * Los documentos de los que sale la regla, el principal al final; nunca vacía.
     *
     * @return los documentos de los que sale la regla, el principal al final; nunca vacía.
     */
    public List<RuleSource> getSources() {
        return fuentes;
    }

    /**
     * La fuente principal (la última que la introdujo o modificó).
     *
     * @return la fuente principal (la última que la introdujo o modificó).
     */
    public RuleSource getSource() {
        return vigencia.source();
    }

    /**
     * Desde/hasta cuándo rige; nunca {@code null}.
     *
     * @return desde/hasta cuándo rige; nunca {@code null}.
     */
    public EffectivePeriod getEffectivePeriod() {
        return vigencia;
    }

    /**
     * Por qué no se evalúa, o {@code null} si se evalúa.
     *
     * @return por qué no se evalúa, o {@code null} si se evalúa.
     */
    public NotEvaluatedReason getNotEvaluatedReason() {
        return motivoNoEvaluada;
    }

    /**
     * El código de la regla que esta reemplaza, o {@code null}.
     *
     * @return el código de la regla que esta reemplaza, o {@code null}.
     */
    public String getReplaces() {
        return reemplaza;
    }

    /**
     * El código.
     *
     * @return el código.
     */
    @Override
    public String toString() {
        return codigo;
    }

    /**
     * Constructor fluido de una ficha. Lo crea {@link RuleSet#rule(String)}, que ya conoce el
     * grupo y la ruta base del conjunto.
     */
    public static final class Builder {
        private final String codigo;
        private final RuleGroup grupo;
        private final String rutaBase;
        private String ruta;
        private Severity severidad;
        private String dCodRes;
        private ValuePolicy politica = ValuePolicy.OMIT;
        private final List<RuleSource> fuentes = new ArrayList<>();
        private LocalDate desde;
        private LocalDate hasta;
        private String motivoBaja;
        private String reemplaza;
        private String mensaje;
        private boolean desactivable = true;

        Builder(String codigo, RuleGroup grupo, String rutaBase) {
            this.codigo = Objects.requireNonNull(codigo, "codigo");
            this.grupo = grupo;
            this.rutaBase = rutaBase == null ? "" : rutaBase;
        }

        /**
         * Campo relativo a la ruta base del conjunto ({@code ""} = la propia base).
         *
         * @param campo campo, o camino de subcampos ({@code gPagTarCD.iDenTarj}).
         * @return este builder.
         */
        public Builder field(String campo) {
            if (campo == null || campo.isEmpty()) {
                this.ruta = rutaBase;
            } else {
                this.ruta = rutaBase.isEmpty() ? campo : rutaBase + "." + campo;
            }
            return this;
        }

        /**
         * Ruta canónica completa (para lo que no cuelga de la base del conjunto).
         *
         * @param rutaCompleta ruta canónica.
         * @return este builder.
         */
        public Builder path(String rutaCompleta) {
            this.ruta = rutaCompleta;
            return this;
        }

        /**
         * Este builder, con severidad {@code ERROR} (rechazo, {@code R} del MT).
         *
         * @return este builder, con severidad {@code ERROR} (rechazo, {@code R} del MT).
         */
        public Builder error() {
            this.severidad = Severity.ERROR;
            return this;
        }

        /**
         * Este builder, con severidad {@code WARNING} (notificación, {@code N} del MT).
         *
         * @return este builder, con severidad {@code WARNING} (notificación, {@code N} del MT).
         */
        public Builder warning() {
            this.severidad = Severity.WARNING;
            return this;
        }

        /**
         * Fija el dato de la ficha.
         *
         * @param codigoRespuesta {@code dCodRes} con que SIFEN rechaza.
         * @return este builder.
         */
        public Builder dCodRes(String codigoRespuesta) {
            this.dCodRes = codigoRespuesta;
            return this;
        }

        /**
         * Este builder; el valor rechazado no se conserva (por defecto).
         *
         * @return este builder; el valor rechazado no se conserva (por defecto).
         */
        public Builder omitValue() {
            this.politica = ValuePolicy.OMIT;
            return this;
        }

        /**
         * Este builder; del valor rechazado se conserva solo el largo.
         *
         * @return este builder; del valor rechazado se conserva solo el largo.
         */
        public Builder lengthOnly() {
            this.politica = ValuePolicy.LENGTH_ONLY;
            return this;
        }

        /**
         * Este builder; el valor rechazado se conserva (solo escalares).
         *
         * @return este builder; el valor rechazado se conserva (solo escalares).
         */
        public Builder showValue() {
            this.politica = ValuePolicy.SHOW;
            return this;
        }

        /**
         * Documentos que introdujeron o modificaron la regla, el principal al final.
         *
         * @param f fuentes.
         * @return este builder.
         */
        public Builder source(RuleSource... f) {
            Collections.addAll(this.fuentes, f);
            return this;
        }

        /**
         * Fija el dato de la ficha.
         *
         * @param fecha primer día en que rige la regla.
         * @return este builder.
         */
        public Builder from(LocalDate fecha) {
            this.desde = fecha;
            return this;
        }

        /**
         * Da de baja la regla en la fecha de una fuente.
         *
         * @param fuente documento que la deroga (debe tener fecha).
         * @param motivo por qué deja de regir.
         * @return este builder.
         */
        public Builder until(RuleSource fuente, String motivo) {
            return until(Objects.requireNonNull(fuente.getDate(), "la fuente no tiene fecha"), motivo);
        }

        /**
         * Da de baja la regla en una fecha.
         *
         * @param fecha  primer día en que ya no rige.
         * @param motivo por qué deja de regir.
         * @return este builder.
         */
        public Builder until(LocalDate fecha, String motivo) {
            this.hasta = fecha;
            this.motivoBaja = motivo;
            return this;
        }

        /**
         * Fija el dato de la ficha.
         *
         * @param codigoReemplazado código de la regla que esta reemplaza.
         * @return este builder.
         */
        public Builder replaces(String codigoReemplazado) {
            this.reemplaza = codigoReemplazado;
            return this;
        }

        /**
         * Texto propio (solo reglas del consumidor; las de la librería usan el {@code .properties}).
         *
         * @param texto mensaje en español, con marcadores {@code {0}}… si corresponde.
         * @return este builder.
         */
        public Builder message(String texto) {
            this.mensaje = texto;
            return this;
        }

        /**
         * Marca la regla como guarda anti-NPE: no se puede desactivar (M3-15).
         *
         * @return este builder.
         */
        public Builder nonDisableable() {
            this.desactivable = false;
            return this;
        }

        /** Arma la ficha. */
        ValidationRule construir() {
            if (ruta == null) {
                field("");
            }
            if (severidad == null) {
                throw new IllegalStateException("La regla " + codigo + " no declara su severidad");
            }
            List<RuleSource> f = fuentes.isEmpty() ? List.of(RuleSource.MT_V150) : List.copyOf(fuentes);
            EffectivePeriod v = new EffectivePeriod(f.get(f.size() - 1), desde, hasta, motivoBaja);
            return new ValidationRule(codigo, grupo, ruta, severidad, dCodRes, politica, f, v, null, reemplaza, mensaje, desactivable);
        }
    }
}
