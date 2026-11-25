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

import org.javabeanstack.sifen.internal.util.Traces;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;

/**
 * Motor genérico: corre los {@link GroupValidator} registrados, en orden, sobre una raíz y acumula
 * todo en un {@link ValidationResult}. Lo usa {@link DEValidator} y lo reutiliza el validador de
 * eventos (§3.5).
 *
 * <p>Garantías: nunca devuelve {@code null}; con raíz nula reporta {@link ValidationRule#JBS_VAL_01}
 * sin correr ningún grupo; con contexto nulo reporta {@link ValidationRule#JBS_VAL_02} (aviso) y
 * sigue; una {@link RuntimeException} de un grupo se convierte en {@link ValidationRule#JBS_VAL_00}
 * (con el nombre del grupo y la clase de la excepción) y los grupos siguientes corren igual. Los
 * {@link Error} de la JVM no se capturan.</p>
 *
 * <p><b>Log de {@code JBS-VAL-00}</b> (M3-04): la traza va a WARN, para poder diagnosticar en
 * producción un falso positivo que bloquea la emisión, pero <b>sin el mensaje</b> de la excepción ni
 * de sus causas (el de una excepción del JDK puede traer un dato del documento:
 * {@code NumberFormatException: For input string: …}); quedan la clase y la pila.</p>
 *
 * <p><b>Reglas declaradas</b> (M3-10): después de cada grupo el motor comprueba que todo lo que
 * reportó esté en su {@link GroupValidator#getRules()}. Una regla no declarada se acepta y se avisa
 * en el log; en modo <b>estricto</b> (solo para pruebas: {@code DEValidator.Builder#strict()}) lanza
 * {@link IllegalStateException}, lo único que este motor deja salir.</p>
 *
 * @param <T> tipo de la raíz.
 */
final class ValidationEngine<T> {
    private static final Logger LOGGER = LogManager.getLogger(ValidationEngine.class);

    private final List<GroupValidator<T>> grupos;
    private final boolean estricto;
    private final Clock reloj;
    private final String raiz;
    private final Set<String> desactivadas;
    private final LocalDate fechaReglas;

    private ValidationEngine(List<GroupValidator<T>> grupos, boolean estricto, Clock reloj, String raiz,
            Set<String> desactivadas, LocalDate fechaReglas) {
        this.grupos = List.copyOf(grupos);
        this.estricto = estricto;
        this.reloj = reloj;
        this.raiz = raiz;
        this.desactivadas = Set.copyOf(desactivadas);
        this.fechaReglas = fechaReglas;
    }

    /**
     * Crea el motor; valida los grupos una sola vez, al construirlo.
     *
     * @param grupos   grupos en el orden de ejecución.
     * @param estricto {@code true} = una regla reportada fuera de {@code getRules()} lanza
     *                 {@link IllegalStateException} (solo pruebas).
     * @return el motor.
     * @throws NullPointerException     si un grupo, su nombre o sus reglas son nulos.
     * @throws IllegalArgumentException si un nombre está en blanco o repetido.
     */
    static <T> ValidationEngine<T> de(List<GroupValidator<T>> grupos, boolean estricto) {
        return de(grupos, estricto, null);
    }

    /**
     * Crea el motor con un reloj fijo (pruebas de las reglas de plazo, DEC-03).
     *
     * @param grupos   grupos en el orden de ejecución.
     * @param estricto ver {@link #de(List, boolean)}.
     * @param reloj    reloj de {@link ValidationContext#getNow()}; {@code null} = el del sistema.
     * @return el motor.
     */
    static <T> ValidationEngine<T> de(List<GroupValidator<T>> grupos, boolean estricto, Clock reloj) {
        return de(grupos, estricto, reloj, ValidationRule.DE_ROOT);
    }

    /**
     * Crea el motor para una raíz con nombre propio (I3-06): los errores internos
     * ({@code JBS-VAL-*}) llevan esa ruta ({@code DE} o {@code rEve}).
     *
     * @param grupos   grupos en el orden de ejecución.
     * @param estricto ver {@link #de(List, boolean)}.
     * @param reloj    reloj de {@link ValidationContext#getNow()}; {@code null} = el del sistema.
     * @param raiz     una de {@link ValidationRule#ROOTS}.
     * @return el motor.
     */
    static <T> ValidationEngine<T> de(List<GroupValidator<T>> grupos, boolean estricto, Clock reloj, String raiz) {
        return de(grupos, estricto, reloj, raiz, Set.of(), null);
    }

    /**
     * Crea el motor con reglas desactivadas y fecha de reglas (D33).
     *
     * @param grupos       grupos en el orden de ejecución.
     * @param estricto     ver {@link #de(List, boolean)}.
     * @param reloj        reloj; {@code null} = el del sistema.
     * @param raiz         una de {@link ValidationRule#ROOTS}.
     * @param desactivadas códigos que no se evalúan.
     * @param fechaReglas  fecha de referencia de las reglas; {@code null} = configuración u hoy.
     * @return el motor.
     */
    static <T> ValidationEngine<T> de(List<GroupValidator<T>> grupos, boolean estricto, Clock reloj, String raiz,
            Set<String> desactivadas, LocalDate fechaReglas) {
        Map<String, GroupValidator<T>> porNombre = new LinkedHashMap<>();
        for (GroupValidator<T> grupo : grupos) {
            Objects.requireNonNull(grupo, "grupo");
            String nombre = Objects.requireNonNull(grupo.getName(), "nombre del grupo");
            Objects.requireNonNull(grupo.getRules(), "reglas del grupo " + nombre);
            if (nombre.isBlank()) {
                throw new IllegalArgumentException("Grupo de validación sin nombre: " + grupo.getClass().getName());
            }
            if (porNombre.putIfAbsent(nombre, grupo) != null) {
                throw new IllegalArgumentException("Grupo de validación repetido: " + nombre);
            }
        }
        if (!ValidationRule.ROOTS.contains(raiz)) {
            throw new IllegalArgumentException("Raíz de validación desconocida: " + raiz);
        }
        return new ValidationEngine<>(new ArrayList<>(porNombre.values()), estricto, reloj, raiz,
                desactivadas == null ? Set.of() : desactivadas, fechaReglas);
    }

    /**
     * Devuelve los grupos registrados, en orden de ejecución.
     *
     * @return lista inmodificable.
     */
    List<GroupValidator<T>> getGrupos() {
        return Collections.unmodifiableList(grupos);
    }

    /**
     * Comprueba los códigos desactivados de un validador: las reglas del motor no se pueden desactivar;
     * un código que ningún grupo registrado declara es un error de configuración del validador (en modo
     * estricto lanza, si no avisa en el log).
     *
     * @param codigos  códigos desactivados.
     * @param grupos   grupos registrados.
     * @param estricto modo de pruebas.
     * @throws IllegalArgumentException si se desactiva una regla del motor, o un código desconocido en
     *                                  modo estricto.
     */
    static void verificarDesactivadas(Set<String> codigos, List<? extends GroupValidator<?>> grupos, boolean estricto) {
        Set<String> conocidos = new HashSet<>();
        Set<String> guardas = new HashSet<>();
        for (GroupValidator<?> g : grupos) {
            for (ValidationRule f : g instanceof RuleSet<?, ?> c ? c.getDeclaredRules() : g.getRules()) {
                conocidos.add(f.getCode());
                if (!f.isDisableable()) {
                    guardas.add(f.getCode());
                }
            }
        }
        for (String codigo : codigos) {
            if (ValidationRule.JBS_VAL_00.getCode().equals(codigo) || ValidationRule.JBS_VAL_01.getCode().equals(codigo)
                    || ValidationRule.JBS_VAL_02.getCode().equals(codigo)) {
                throw new IllegalArgumentException("La regla del motor " + codigo + " no se puede desactivar");
            }
            if (guardas.contains(codigo)) {
                throw new IllegalArgumentException("La regla " + codigo
                        + " es una guarda de la librería (sin ella el XML no se puede armar) y no se puede desactivar");
            }
            if (!conocidos.contains(codigo)) {
                if (estricto) {
                    throw new IllegalArgumentException("Se desactiva una regla que no existe: " + codigo);
                }
                LOGGER.warn("Se desactiva la regla {}, que ningún grupo registrado declara", codigo);
            }
        }
    }

    /**
     * Indica si el motor corre en modo estricto.
     *
     * @return {@code true} si una regla no declarada lanza.
     */
    boolean isEstricto() {
        return estricto;
    }

    /**
     * Valida una raíz.
     *
     * @param raiz          raíz a validar; puede ser {@code null}.
     * @param generationCtx contexto de generación; puede ser {@code null}.
     * @return el resultado; nunca {@code null}.
     * @throws IllegalStateException solo en modo estricto, si un grupo reporta una regla que no
     *                               declara.
     */
    ValidationResult validar(T raizValidada, GenerationCtx generationCtx) {
        T raiz = raizValidada;
        ValidationContext ctx = new ValidationContext(generationCtx, reloj, desactivadas, fechaReglas, this.raiz);
        if (raiz == null) {
            ctx.report(ValidationRule.JBS_VAL_01, this.raiz, null);
            return ctx.resultado();
        }
        if (generationCtx == null) {
            ctx.report(ValidationRule.JBS_VAL_02, this.raiz, null);
        }
        for (GroupValidator<T> grupo : grupos) {
            int desde = ctx.getReportedCount();
            try {
                grupo.validate(raiz, ctx);
            } catch (RuntimeException e) {
                LOGGER.warn("El grupo de validación {} lanzó {}; se reporta {}", grupo.getName(),
                        e.getClass().getName(), ValidationRule.JBS_VAL_00.getCode(), trazaSinMensaje(e));
                ctx.report(ValidationRule.JBS_VAL_00, this.raiz, null, grupo.getName(), e.getClass().getSimpleName());
            }
            verificarDeclaradas(grupo, ctx.codigosDesde(desde));
        }
        return ctx.resultado();
    }

    /** M3-10: lo reportado por un grupo tiene que estar en su {@code getRules()}. */
    private void verificarDeclaradas(GroupValidator<T> grupo, List<String> reportados) {
        if (reportados.isEmpty()) {
            return;
        }
        Set<String> declarados = new HashSet<>();
        for (ValidationRule regla : grupo.getRules()) {
            if (regla != null) {
                declarados.add(regla.getCode());
            }
        }
        declarados.add(ValidationRule.JBS_VAL_00.getCode());
        for (String codigo : reportados) {
            if (!declarados.contains(codigo)) {
                if (estricto) {
                    throw new IllegalStateException("El grupo de validación " + grupo.getName()
                            + " reportó la regla " + codigo + ", que no declara en getRules()");
                }
                LOGGER.warn("El grupo de validación {} reportó la regla {}, que no declara en getRules()",
                        grupo.getName(), codigo);
            }
        }
    }

    /**
     * Copia la excepción y sus causas conservando solo la clase y la pila: ningún mensaje original
     * llega al log (M3-04). Desde §3.8 la implementación es {@link Traces#withoutMessage(Throwable)},
     * compartida con la fachada.
     *
     * @param original excepción capturada.
     * @return la copia sin mensajes.
     */
    static Throwable trazaSinMensaje(Throwable original) {
        return Traces.withoutMessage(original);
    }
}
