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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;

/**
 * Lo que recibe cada {@link GroupValidator}: el contexto de generación (de solo lectura) y el
 * acumulador donde reporta los incumplimientos.
 *
 * <p>Es <b>mutable</b> y vive lo que dura una llamada a {@link DEValidator#validate}: el motor crea
 * uno, se lo pasa a cada grupo en orden y al final lo congela en un {@link ValidationResult}
 * inmutable. No es seguro entre hilos ni debe guardarse.</p>
 *
 * <p><b>Null-safety</b>: el contexto de generación puede ser nulo (y también su
 * {@code SifenConfig}); en ese caso los accesores devuelven los valores por defecto de
 * {@link SifenConfig} en vez de fallar. Los ayudantes {@link #present(Object)} y
 * {@link #checkPresent(ValidationRule, String, Object, Object...)} son la forma recomendada de comprobar
 * obligatorios sin tocar un nulo.</p>
 */
public final class ValidationContext {
    /** Zona de las reglas de plazo cuando no se inyecta un reloj (M3-23): la hora de SIFEN es la de Asunción. */
    public static final ZoneId SIFEN_ZONE = ZoneId.of("America/Asuncion");
    private static final Clock RELOJ_PARAGUAY = Clock.system(SIFEN_ZONE);
    private final GenerationCtx generationCtx;
    private final LocalDateTime ahora;
    private final Set<String> desactivadas;
    private final LocalDate fechaReglas;
    private final String raiz;
    private final LocalDateTime fechaFirmaPorOmision;
    private final String idEventoPorOmision;
    private final List<ValidationError> reportados = new ArrayList<>();

    /**
     * Crea el contexto de una corrida con el reloj del sistema.
     *
     * @param generationCtx contexto de generación; puede ser {@code null}.
     */
    ValidationContext(GenerationCtx generationCtx) {
        this(generationCtx, null);
    }

    /**
     * Crea el contexto de una corrida.
     *
     * @param generationCtx contexto de generación; puede ser {@code null}.
     * @param reloj         reloj del que se toma {@link #getNow()}; {@code null} = reloj del sistema en la
     *                      zona {@link #SIFEN_ZONE} (M3-23).
     */
    ValidationContext(GenerationCtx generationCtx, Clock reloj) {
        this(generationCtx, reloj, Set.of(), null);
    }

    /**
     * Crea el contexto de una corrida con reglas desactivadas y fecha de reglas (D33).
     *
     * @param generationCtx contexto de generación; puede ser {@code null}.
     * @param reloj         reloj de {@link #getNow()}; {@code null} = el del sistema.
     * @param desactivadas  códigos desactivados por el validador (se suman los de la configuración).
     * @param fechaReglas   fecha de referencia de las reglas fijada por el validador; {@code null} = la
     *                      de la configuración o la de hoy.
     */
    ValidationContext(GenerationCtx generationCtx, Clock reloj, Set<String> desactivadas, LocalDate fechaReglas) {
        this(generationCtx, reloj, desactivadas, fechaReglas, ValidationRule.DE_ROOT);
    }

    /**
     * Crea el contexto de una corrida de un motor con su raíz (I3-21: la ruta de {@code JBS-VAL-00} sale de
     * la raíz del motor, no del tipo del objeto).
     *
     * @param raiz raíz de las rutas ({@link ValidationRule#ROOTS}).
     */
    ValidationContext(GenerationCtx generationCtx, Clock reloj, Set<String> desactivadas, LocalDate fechaReglas,
            String raiz) {
        this(generationCtx, reloj, desactivadas, fechaReglas, raiz, null, null);
    }

    /**
     * Crea el contexto de una corrida con los valores que se suponen cuando el documento o el evento no los
     * trae: la validación previa del cliente valida como firmaría {@code sign} (I8-27) o como enviaría
     * {@code sendEvents} (I8-34).
     *
     * @param fechaFirmaPorOmision {@code dFecFirma} supuesta si el documento o el evento no la cargó; {@code null} =
     *                             ninguna.
     * @param idEventoPorOmision   {@code Id} supuesto si el evento no lo cargó ({@code rEve}); {@code null} = ninguno.
     */
    ValidationContext(GenerationCtx generationCtx, Clock reloj, Set<String> desactivadas, LocalDate fechaReglas,
            String raiz, LocalDateTime fechaFirmaPorOmision, String idEventoPorOmision) {
        this.fechaFirmaPorOmision = fechaFirmaPorOmision;
        this.idEventoPorOmision = idEventoPorOmision;
        this.raiz = raiz == null ? ValidationRule.DE_ROOT : raiz;
        this.generationCtx = generationCtx;
        this.ahora = LocalDateTime.now(reloj == null ? RELOJ_PARAGUAY : reloj);
        Set<String> d = new HashSet<>(desactivadas == null ? Set.of() : desactivadas);
        SifenConfig cfg = generationCtx == null ? null : generationCtx.getSifenConfig();
        if (cfg != null && cfg.getDisabledRules() != null) {
            d.addAll(cfg.getDisabledRules());
        }
        this.desactivadas = Collections.unmodifiableSet(d);
        LocalDate f = fechaReglas;
        if (f == null && cfg != null) {
            f = cfg.getRulesDate();
        }
        this.fechaReglas = f == null ? ahora.toLocalDate() : f;
    }

    /**
     * Devuelve la fecha de referencia de las reglas: qué reglas rigen ({@link EffectivePeriod}). Es la fijada
     * en el validador, si no la de {@code SifenConfig.validacionFechaReglas}, si no la de hoy (D33).
     *
     * @return la fecha.
     */
    public LocalDate getRulesDate() {
        return fechaReglas;
    }

    /**
     * Devuelve los códigos desactivados en esta corrida (validador + configuración).
     *
     * @return conjunto inmodificable.
     */
    public Set<String> getDisabledRules() {
        return desactivadas;
    }

    /**
     * Indica si una regla se evalúa en esta corrida: evaluable, no desactivada y vigente en la fecha de
     * reglas.
     *
     * @param ficha la regla.
     * @return {@code true} si se evalúa.
     */
    public boolean isRuleActive(ValidationRule ficha) {
        return ficha.isEvaluable() && (!ficha.isDisableable() || !desactivadas.contains(ficha.getCode()))
                && ficha.isInForceOn(fechaReglas);
    }

    /**
     * Devuelve la raíz de las rutas de esta corrida ({@code DE}, {@code rEve} o {@code sifenConfig}).
     *
     * @return la raíz.
     */
    String getRoot() {
        return raiz;
    }

    /**
     * Devuelve el instante de la validación, sustituto de la hora de transmisión en las reglas de
     * plazo (A004a/b, D002/D002f, GEN003…: DEC-03, por eso son avisos). Se fija una vez por corrida;
     * las pruebas lo inyectan con {@code DEValidator.Builder#clock}.
     *
     * @return fecha y hora local de la validación.
     */
    public LocalDateTime getNow() {
        return ahora;
    }

    /**
     * Devuelve la fecha de firma ({@code dFecFirma}) que esta corrida supone cuando el documento o el evento no la
     * trae: la hora con que la completaría {@code ISifenClient.sign} o {@code ISifenClient.sendEvents}. La fija
     * {@link DEValidator#validate(org.javabeanstack.sifen.core.beans.DocumentoElectronico, GenerationCtx,
     * LocalDateTime)}, que es la que usa {@code ISifenClient.validateDE} (I8-27), o
     * {@link EventValidator#validate(org.javabeanstack.sifen.core.beans.EventosDE, GenerationCtx, LocalDateTime,
     * String)}, la de {@code ISifenClient.validateEvents} (I8-34); en las demás corridas es {@code null} y la
     * falta de {@code dFecFirma} se reporta ({@code JBS-A-03}, {@code JBS-EVENTO-02}). Las reglas de
     * {@code dFecFirma} usan la del documento o del evento si está cargada y esta si no.
     *
     * @return la fecha supuesta, o {@code null} si la corrida no supone ninguna.
     */
    public LocalDateTime getDefaultSignatureDate() {
        return fechaFirmaPorOmision;
    }

    /**
     * Devuelve el {@code Id} de evento ({@code rEve@Id}) que esta corrida supone cuando el evento no lo trae
     * (nulo o en blanco, el mismo criterio con que lo completa {@code ISifenClient.sendEvents}). Lo fija
     * {@link EventValidator#validate(org.javabeanstack.sifen.core.beans.EventosDE, GenerationCtx, LocalDateTime,
     * String)}, la de {@code ISifenClient.validateEvents} (I8-34); en las demás corridas es {@code null} y la falta
     * del {@code Id} se reporta ({@code JBS-EVENTO-01}).
     *
     * @return el {@code Id} supuesto, o {@code null} si la corrida no supone ninguno.
     */
    public String getDefaultEventId() {
        return idEventoPorOmision;
    }

    /**
     * Devuelve el ambiente de SIFEN de la configuración (D105/JBS-D-13 dependen de él).
     *
     * @return el ambiente, o {@code null} si no hay configuración.
     */
    public SifenConfig.Environment getEnvironment() {
        SifenConfig cfg = getSifenConfig();
        return cfg == null ? null : cfg.getEnvironment();
    }

    /**
     * Devuelve el contexto de generación con que se validó.
     *
     * @return el contexto, o {@code null} si no se pasó.
     */
    public GenerationCtx getGenerationCtx() {
        return generationCtx;
    }

    /**
     * Devuelve la configuración de SIFEN del contexto de generación.
     *
     * @return la configuración, o {@code null} si no hay contexto o el contexto no la trae.
     */
    public SifenConfig getSifenConfig() {
        return generationCtx == null ? null : generationCtx.getSifenConfig();
    }

    /**
     * Indica si un dato está presente: no es {@code null}, ni un texto en blanco, ni una colección o
     * un mapa vacíos.
     *
     * @param valor el dato.
     * @return {@code true} si está presente.
     */
    public static boolean present(Object valor) {
        if (valor == null) {
            return false;
        }
        if (valor instanceof CharSequence texto) {
            return !texto.toString().isBlank();
        }
        if (valor instanceof Collection<?> coleccion) {
            return !coleccion.isEmpty();
        }
        if (valor instanceof Map<?, ?> mapa) {
            return !mapa.isEmpty();
        }
        return true;
    }

    /**
     * Reporta un incumplimiento en la ruta canónica de la regla, sin valor.
     *
     * @param regla regla incumplida.
     */
    public void report(ValidationRule regla) {
        report(regla, null, null);
    }

    /**
     * Reporta un incumplimiento sin valor, en la ruta concreta que resulta de la canónica de la regla
     * con estos índices ({@link ValidationRule#path(int...)}, base 0). No hay sobrecarga con una
     * ruta en texto y sin valor (M3-03): la ruta se arma siempre desde la regla.
     *
     * @param regla   regla incumplida.
     * @param indices índices base 0 de las listas de la ruta, del grupo más externo al más interno.
     */
    public void report(ValidationRule regla, int... indices) {
        report(regla, regla.path(indices), null);
    }

    /**
     * Reporta un incumplimiento. El valor pasa por la {@linkplain ValidationRule#getValuePolicy()
     * política} de la regla antes de guardarse.
     *
     * @param regla regla incumplida.
     * @param ruta  ruta concreta, armada con {@code regla.path(i, j)}; {@code null} = ruta canónica.
     *              Sin sus índices tiene que ser la ruta canónica de la regla.
     * @throws IllegalArgumentException si la ruta no corresponde a la regla (M3-03); dentro de un grupo
     *                                  el motor la convierte en {@code JBS-VAL-00}.
     * @param valor valor rechazado tal como está en el documento; puede ser {@code null}.
     * @param args  detalles para los marcadores del mensaje (largo máximo, dominio…), nunca datos del
     *              documento.
     */
    public void report(ValidationRule regla, String ruta, Object valor, Object... args) {
        reportados.add(ValidationError.de(regla, ruta, valor, args));
    }

    /**
     * Comprueba un obligatorio: si el dato no está {@linkplain #present(Object) presente}, reporta la
     * regla. Uso típico: {@code if (ctx.checkPresent(regla, ruta, grupo)) { ...validar los hijos... }}.
     *
     * @param rule  regla de obligatoriedad.
     * @param path  ruta concreta armada con {@code regla.path(i, j)}; {@code null} = ruta canónica.
     * @param value dato a comprobar.
     * @param args  detalles para el mensaje.
     * @return {@code true} si el dato está presente (se puede seguir hacia sus hijos).
     */
    public boolean checkPresent(ValidationRule rule, String path, Object value, Object... args) {
        if (present(value)) {
            return true;
        }
        report(rule, path, null, args);
        return false;
    }

    /**
     * Devuelve la cantidad de incumplimientos reportados hasta ahora (errores y avisos).
     *
     * @return la cantidad.
     */
    public int getReportedCount() {
        return reportados.size();
    }

    /** Códigos reportados desde la posición {@code desde} (para el control de M3-10 del motor). */
    List<String> codigosDesde(int desde) {
        List<String> codigos = new ArrayList<>();
        for (int i = desde; i < reportados.size(); i++) {
            codigos.add(reportados.get(i).getCode());
        }
        return codigos;
    }

    /** Congela lo reportado en un resultado inmutable. */
    ValidationResult resultado() {
        return new ValidationResult(reportados);
    }
}
