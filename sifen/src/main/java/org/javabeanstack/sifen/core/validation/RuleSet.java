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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;

/**
 * Un subgrupo del MT con sus reglas (D33): cada regla es una <b>ficha</b> y un <b>chequeo</b> declarados
 * juntos, una debajo de otra, en el constructor de la subclase:
 *
 * <pre>{@code
 * final class IssuerRules extends RuleSet<DocumentoElectronico, TgEmis> {
 *     IssuerRules() {
 *         super("D-gEmis", RuleGroup.D, "gDatGralOpe.gEmis");
 *         declare(rule("JBS-D-07").field("dRucEm").error().omitValue(), required(TgEmis::getdRucEm));
 *         declare(rule("JBS-D-12").field("dNomEmi").error().lengthOnly(), length(TgEmis::getdNomEmi, 4, 255));
 *     }
 *
 *     protected List<Subject<DocumentoElectronico, TgEmis>> subjects(DocumentoElectronico de) {
 *         return de.getgDatGralOpe() == null ? none() : unique(de.getgDatGralOpe().getgEmis());
 *     }
 * }
 * }</pre>
 *
 * <p>{@link #subjects} dice cómo llegar al elemento: <b>ningún</b> sujeto si falta el padre (la ausencia
 * la reporta el conjunto del padre) y un sujeto con elemento {@code null} si falta el propio elemento
 * (sus reglas de presencia, {@link #groupRequiredIf}, lo reportan; las demás cumplen). En una lista, un sujeto
 * por elemento con su índice. Por cada sujeto se evalúan las reglas <b>activas</b> (vigentes en la fecha
 * de reglas y no desactivadas) en el orden declarado; un chequeo que lanza da
 * {@link ValidationRule#JBS_VAL_00} con {@code conjunto/código} y la validación sigue.</p>
 *
 * <p>Implementa {@link GroupValidator}: el motor y los validadores lo usan igual que a un grupo.</p>
 *
 * @param <T> raíz validada ({@code DocumentoElectronico} o {@code TrGesEve}).
 * @param <E> elemento del subgrupo.
 */
public abstract class RuleSet<T, E> implements GroupValidator<T> {
    private static final Logger LOGGER = LogManager.getLogger(RuleSet.class);

    private final String nombre;
    private final RuleGroup grupo;
    private final String rutaBase;
    private final List<Declaracion<T, E>> declaraciones = new ArrayList<>();

    /** Ficha y chequeo; el chequeo es {@code null} en las no evaluadas. */
    private record Declaracion<T, E>(ValidationRule ficha, Check<T, E> chequeo) {
    }

    /** Un campo (o todo el elemento, {@code ""}) que la librería no escribe cuando se cumple la condición. */
    private record NoSeEscribe<T, E>(String campo, Predicate<Subject<T, E>> cuando) {
    }

    private final List<NoSeEscribe<T, E>> noSeEscriben = new ArrayList<>();

    /**
     * Fija el dato de la ficha.
     *
     * @param nombre   nombre único del conjunto ({@code D-gEmis}); aparece en {@code JBS-VAL-00}.
     * @param grupo    grupo del MT.
     * @param rutaBase ruta canónica del subgrupo ({@code ""} = raíz del documento).
     */
    protected RuleSet(String nombre, RuleGroup grupo, String rutaBase) {
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.grupo = Objects.requireNonNull(grupo, "grupo");
        this.rutaBase = rutaBase == null ? "" : rutaBase;
    }

    /**
     * Cómo llegar al elemento del subgrupo desde la raíz.
     *
     * @param raiz raíz validada (no nula).
     * @return los sujetos; vacío si falta el padre.
     */
    protected abstract List<Subject<T, E>> subjects(T raiz);

    // ---------------------------------------------------------------- declaración

    /**
     * Empieza la ficha de una regla de este conjunto.
     *
     * @param codigo código estable.
     * @return el builder, con el grupo y la ruta base del conjunto.
     */
    protected final ValidationRule.Builder rule(String codigo) {
        return new ValidationRule.Builder(codigo, grupo, rutaBase);
    }

    /**
     * Declara una regla evaluada.
     *
     * @param ficha   ficha de la regla.
     * @param chequeo lógica.
     */
    protected final void declare(ValidationRule.Builder ficha, Check<T, E> chequeo) {
        declaraciones.add(new Declaracion<>(ficha.construir(), Objects.requireNonNull(chequeo, "chequeo")));
    }

    /**
     * Declara que la librería <b>no escribe</b> un campo del elemento, o el elemento entero, cuando se
     * cumple la condición (plan JBSSIF, §3.8, I3-42; criterio de I3-37 y M3-42): en ese caso las reglas
     * de severidad ERROR sobre ese campo, o sobre lo que cuelga de él, no se evalúan, porque el dato no
     * llega a SIFEN; las de AVISO sí (entre ellas la «no informar …» del grupo, que es la que avisa).
     * Cada declaración tiene que corresponder a una condición del {@code setupSOAPElements} del grupo.
     *
     * @param campo  campo relativo a la ruta base del conjunto ({@code "gPagTarCD"}, {@code "cDepRec"});
     *               {@code ""} = todo el elemento.
     * @param cuando condición en la que no se escribe.
     */
    protected final void notWrittenIf(String campo, Predicate<Subject<T, E>> cuando) {
        noSeEscriben.add(new NoSeEscribe<>(campo == null ? "" : campo, Objects.requireNonNull(cuando, "cuando")));
    }

    /** I3-42: la regla es de ERROR sobre un dato que, para este sujeto, la librería no escribe. */
    private boolean sobreDatoNoEscrito(ValidationRule ficha, Subject<T, E> s) {
        if (noSeEscriben.isEmpty() || ficha.getSeverity() != Severity.ERROR) {
            return false;
        }
        String ruta = ficha.getPath();
        String relativa = ruta.equals(rutaBase) ? "" : rutaBase.isEmpty() ? ruta
                : ruta.startsWith(rutaBase + ".") ? ruta.substring(rutaBase.length() + 1) : ruta;
        for (NoSeEscribe<T, E> n : noSeEscriben) {
            boolean debajo = n.campo().isEmpty() || relativa.equals(n.campo()) || relativa.startsWith(n.campo() + ".")
                    || relativa.startsWith(n.campo() + "[");
            if (!debajo || (s.element() == null && !n.campo().isEmpty())) {
                continue; // sin elemento, la condición de un campo no se puede mirar: se evalúa como siempre
            }
            if (n.cuando().test(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Declara una regla del catálogo que no se evalúa localmente (I3-15).
     *
     * @param ficha  ficha de la regla.
     * @param motivo por qué no se evalúa.
     */
    protected final void declareNotEvaluated(ValidationRule.Builder ficha, NotEvaluatedReason motivo) {
        declaraciones.add(new Declaracion<>(ficha.construir().conMotivo(Objects.requireNonNull(motivo, "motivo")), null));
    }

    // ---------------------------------------------------------------- evaluación

    @Override
    public final void validate(T raiz, ValidationContext ctx) {
        for (Subject<T, E> parcial : subjects(raiz)) {
            Subject<T, E> s = parcial.with(raiz, ctx);
            for (Declaracion<T, E> d : declaraciones) {
                if (d.chequeo() == null || !ctx.isRuleActive(d.ficha())) {
                    continue;
                }
                try {
                    if (sobreDatoNoEscrito(d.ficha(), s)) {
                        continue;
                    }
                    if (!d.chequeo().passes(s)) {
                        Object[] args = d.chequeo().args(s);
                        ctx.report(d.ficha(), d.ficha().path(s.indexes()), d.chequeo().value(s),
                                args == null ? new Object[0] : args);
                    }
                } catch (RuntimeException e) {
                    String quien = nombre + "/" + d.ficha().getCode();
                    LOGGER.warn("La regla {} lanzó {}; se reporta {}", quien, e.getClass().getName(),
                            ValidationRule.JBS_VAL_00.getCode(), ValidationEngine.trazaSinMensaje(e));
                    ctx.report(ValidationRule.JBS_VAL_00, ctx.getRoot(), null, quien, e.getClass().getSimpleName());
                }
            }
        }
    }

    @Override
    public final String getName() {
        return nombre;
    }

    /**
     * Reglas que este conjunto puede reportar (las evaluadas), en orden de declaración.
     *
     * @return conjunto ordenado inmodificable.
     */
    @Override
    public final Set<ValidationRule> getRules() {
        Set<ValidationRule> s = new LinkedHashSet<>();
        for (Declaracion<T, E> d : declaraciones) {
            if (d.chequeo() != null) {
                s.add(d.ficha());
            }
        }
        return Collections.unmodifiableSet(s);
    }

    /**
     * Todas las fichas del conjunto, evaluadas o no, en orden de declaración.
     *
     * @return lista inmodificable.
     */
    public final List<ValidationRule> getDeclaredRules() {
        List<ValidationRule> l = new ArrayList<>();
        for (Declaracion<T, E> d : declaraciones) {
            l.add(d.ficha());
        }
        return Collections.unmodifiableList(l);
    }

    /**
     * El grupo del MT.
     *
     * @return el grupo del MT.
     */
    public final RuleGroup getGroup() {
        return grupo;
    }

    /**
     * La ruta base del subgrupo.
     *
     * @return la ruta base del subgrupo.
     */
    public final String getBasePath() {
        return rutaBase;
    }

    // ---------------------------------------------------------------- sujetos

    /**
     * Ningún sujeto (falta el padre).
     *
     * @return ningún sujeto (falta el padre).
     */
    protected final List<Subject<T, E>> none() {
        return Collections.emptyList();
    }

    /**
     * Fija el dato de la ficha.
     *
     * @param elemento elemento; puede ser {@code null} (lo juzgan las reglas de presencia).
     * @return un sujeto, sin índices.
     */
    protected final List<Subject<T, E>> unique(E elemento) {
        return List.of(Subject.of(elemento));
    }

    /**
     * Un sujeto por elemento de la lista, con su índice; los elementos nulos también.
     *
     * @param lista lista; {@code null} = ningún sujeto.
     * @return los sujetos.
     */
    protected final List<Subject<T, E>> perItem(List<? extends E> lista) {
        return perItem(lista, new int[0]);
    }

    /**
     * Un sujeto por elemento de la lista, con los índices externos y el propio.
     *
     * @param lista    lista; {@code null} = ningún sujeto.
     * @param externos índices de las listas que la contienen.
     * @return los sujetos.
     */
    protected final List<Subject<T, E>> perItem(List<? extends E> lista, int[] externos) {
        if (lista == null) {
            return Collections.emptyList();
        }
        List<Subject<T, E>> l = new ArrayList<>(lista.size());
        for (int i = 0; i < lista.size(); i++) {
            int[] ind = java.util.Arrays.copyOf(externos, externos.length + 1);
            ind[externos.length] = i;
            l.add(Subject.of(lista.get(i), ind));
        }
        return l;
    }

    /**
     * Un sujeto por elemento <b>no nulo</b> de la lista, con el subelemento que da {@code getter}.
     *
     * @param lista  lista de padres; {@code null} = ningún sujeto.
     * @param getter del padre al subelemento (puede devolver {@code null}).
     * @param <P>    tipo del padre.
     * @return los sujetos.
     */
    protected final <P> List<Subject<T, E>> perItem(List<P> lista, Function<P, E> getter) {
        if (lista == null) {
            return Collections.emptyList();
        }
        List<Subject<T, E>> l = new ArrayList<>(lista.size());
        for (int i = 0; i < lista.size(); i++) {
            P padre = lista.get(i);
            if (padre != null) {
                l.add(Subject.of(getter.apply(padre), i));
            }
        }
        return l;
    }

    // ---------------------------------------------------------------- chequeos
    //
    // Vocabulario (M3-43, §3.8 ciclo 2): cada verbo nombra lo que la regla EXIGE, nunca el error.
    //   required(dato)                     el dato tiene que estar
    //   requiredIf(cond, dato)             el dato tiene que estar si se cumple cond
    //   forbiddenIf(cond, dato)            el dato NO tiene que estar si se cumple cond
    //   groupRequiredIf(cond)              el elemento del conjunto tiene que estar si se cumple cond
    //   groupForbiddenIf(cond)             el elemento del conjunto NO tiene que estar si se cumple cond
    //   requiredWithLength(dato, m, n)     el texto tiene que estar y medir entre m y n
    // Antes de D42 (Fase 11): obligatorio, obligatorioSi, noInformarSi, grupoObligatorioSi,
    // grupoNoInformarSi y obligatorioConLargo; antes de §3.8: falta, faltaSi, informadoSi, presenteSi,
    // ausenteSi y faltaOLargo.

    /**
     * El elemento del conjunto (el grupo) tiene que estar si se cumple la condición; se evalúa también
     * con el elemento nulo. Antes {@code presenteSi}.
     *
     * @param obligatorio condición en que el elemento es obligatorio.
     * @return el chequeo.
     */
    protected final Check<T, E> groupRequiredIf(Predicate<Subject<T, E>> obligatorio) {
        return s -> !obligatorio.test(s) || ValueChecks.present(s.element());
    }

    /**
     * El elemento del conjunto (el grupo) <b>no</b> tiene que informarse si se cumple la condición.
     * Antes {@code ausenteSi}.
     *
     * @param noDebe condición en que el elemento no debe informarse.
     * @return el chequeo.
     */
    protected final Check<T, E> groupForbiddenIf(Predicate<Subject<T, E>> noDebe) {
        return conValor(s -> !(noDebe.test(s) && ValueChecks.present(s.element())), s -> s.element());
    }

    /**
     * Chequeo general que se evalúa también con el elemento nulo.
     *
     * @param condicion {@code true} si se cumple.
     * @param valor     valor rechazado; puede ser {@code null}.
     * @return el chequeo.
     */
    protected final Check<T, E> onNull(Predicate<Subject<T, E>> condicion, Function<Subject<T, E>, ?> valor) {
        return conValor(condicion, valor);
    }

    /**
     * Chequeo general sobre un elemento presente (con el elemento nulo cumple).
     *
     * @param condicion {@code true} si se cumple; el elemento no es nulo.
     * @return el chequeo.
     */
    protected final Check<T, E> passes(Predicate<Subject<T, E>> condicion) {
        return passes(condicion, null);
    }

    /**
     * Chequeo general sobre un elemento presente (con el elemento nulo cumple), con valor rechazado.
     *
     * @param condicion {@code true} si se cumple; el elemento no es nulo.
     * @param valor     valor rechazado; {@code null} = ninguno.
     * @return el chequeo.
     */
    protected final Check<T, E> passes(Predicate<Subject<T, E>> condicion, Function<Subject<T, E>, ?> valor) {
        return conValor(s -> s.element() == null || condicion.test(s), valor);
    }

    /**
     * El dato tiene que estar. Antes {@code falta}.
     *
     * @param getter del elemento al dato.
     * @return el chequeo.
     */
    protected final Check<T, E> required(Function<E, ?> getter) {
        return passes(s -> ValueChecks.present(getter.apply(s.element())));
    }

    /**
     * El dato tiene que estar si se cumple la condición. Antes {@code faltaSi}.
     *
     * @param obligatorio condición.
     * @param getter      del elemento al dato.
     * @return el chequeo.
     */
    protected final Check<T, E> requiredIf(Predicate<Subject<T, E>> obligatorio, Function<E, ?> getter) {
        return passes(s -> !obligatorio.test(s) || ValueChecks.present(getter.apply(s.element())));
    }

    /**
     * El dato <b>no</b> tiene que informarse si se cumple la condición. Antes {@code informadoSi}, que
     * se leía al revés de lo que exigía (A:M3-43).
     *
     * @param noDebe condición.
     * @param getter del elemento al dato.
     * @return el chequeo.
     */
    protected final Check<T, E> forbiddenIf(Predicate<Subject<T, E>> noDebe, Function<E, ?> getter) {
        return passes(s -> !(noDebe.test(s) && ValueChecks.present(getter.apply(s.element()))),
                s -> getter.apply(s.element()));
    }

    /**
     * Largo del texto, si está presente.
     *
     * @param getter del elemento al texto.
     * @param min    mínimo.
     * @param max    máximo.
     * @return el chequeo.
     */
    protected final Check<T, E> length(Function<E, String> getter, int min, int max) {
        return passes(s -> ValueChecks.length(getter.apply(s.element()), min, max), s -> getter.apply(s.element()));
    }

    /**
     * El texto tiene que estar y tener el largo pedido. Antes {@code faltaOLargo}.
     *
     * @param getter del elemento al texto.
     * @param min    mínimo.
     * @param max    máximo.
     * @return el chequeo.
     */
    protected final Check<T, E> requiredWithLength(Function<E, String> getter, int min, int max) {
        return passes(s -> ValueChecks.present(getter.apply(s.element())) && ValueChecks.length(getter.apply(s.element()), min, max),
                s -> getter.apply(s.element()));
    }

    /**
     * Número del MT {@code N 1-e p(0-d)}, si está presente.
     *
     * @param getter       del elemento al número.
     * @param enteros      cifras enteras máximas.
     * @param decimales    decimales máximos.
     * @param mayorQueCero {@code true} = mayor que cero; {@code false} = no negativo.
     * @return el chequeo.
     */
    protected final Check<T, E> decimal(Function<E, BigDecimal> getter, int enteros, int decimales, boolean mayorQueCero) {
        return passes(s -> {
            BigDecimal v = getter.apply(s.element());
            return ValueChecks.decimal(v, enteros, decimales)
                    && (mayorQueCero ? ValueChecks.positive(v) : ValueChecks.nonNegative(v));
        }, s -> getter.apply(s.element()));
    }

    /**
     * Chequeo general sobre un elemento presente, con detalles para el mensaje.
     *
     * @param condicion {@code true} si se cumple; el elemento no es nulo.
     * @param args      detalles para los marcadores del mensaje (nunca datos personales ni secretos).
     * @return el chequeo.
     */
    protected final Check<T, E> passesWithArgs(Predicate<Subject<T, E>> condicion, Function<Subject<T, E>, Object[]> args) {
        return new Check<T, E>() {
            @Override
            public boolean passes(Subject<T, E> s) {
                return s.element() == null || condicion.test(s);
            }

            @Override
            public Object[] args(Subject<T, E> s) {
                return args.apply(s);
            }
        };
    }

    private static <T, E> Check<T, E> conValor(Predicate<Subject<T, E>> condicion, Function<Subject<T, E>, ?> valor) {
        return new Check<T, E>() {
            @Override
            public boolean passes(Subject<T, E> s) {
                return condicion.test(s);
            }

            @Override
            public Object value(Subject<T, E> s) {
                return valor == null ? null : valor.apply(s);
            }
        };
    }
}
