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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.Sifen;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.beans.EventosDE;
import org.javabeanstack.sifen.core.fields.request.event.TrGesEve;
import org.javabeanstack.sifen.internal.request.MensajesSifen;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Prueba de mutación anti-NPE de la validación previa (plan JBSSIF, §3.4; CA3-04, RF7).
 *
 * <p>Para cada caso del catálogo ({@code CasosDePrueba}, armado por objeto y leído de su XML) recorre
 * por reflexión el grafo del DE ({@link GrafoDe}) y, sobre una instancia nueva cada vez, anula un dato
 * cargado: objeto o texto a {@code null}, primitivo a {@code 0}, lista a {@code null}, lista vacía y
 * lista con un {@code null} agregado; y cada grupo opcional que el caso no carga se agrega vacío
 * (hasta {@link GrafoDe#PROFUNDIDAD_GRUPO_VACIO} niveles), para ejercitar también los grupos que
 * ningún caso trae. Solo se mutan los campos con setter público (lo que el consumidor puede dejar
 * así). Por cada mutación:</p>
 * <ol>
 *   <li><b>(a)</b> {@code validarDE} no lanza, nunca reporta {@code JBS-VAL-00} y, si detecta el dato
 *       faltante, lo reporta como ERROR con una regla del catálogo y la ruta concreta del dato
 *       (índice base 0 incluido; un {@code []} literal en la ruta de un elemento es un defecto).</li>
 *   <li><b>(b)</b> si lo detecta, {@code generarXml} con {@code validacionPrevia=true} lanza
 *       {@link SifenValidationException} y no una NPE. Mientras §3.5 no exista se simula: que el
 *       validador lo rechace es lo que impide llegar a la NPE.</li>
 * </ol>
 *
 * <p>El oráculo de «campo obligatorio» es el propio fork: si el validador no reporta nada y la
 * generación con la validación apagada falla (NPE u otra excepción, buscando en la cadena de causas,
 * M0-10), el campo es <b>obligatorio sin regla</b> y con la validación activa terminaría en esa
 * excepción. Si la generación no falla, la mutación es informativa (dato opcional o que SIFEN rechaza
 * sin romper la librería: {@code vacío}, {@code null}, {@code 0} de la columna «Hoy» del catálogo).</p>
 *
 * <p>Mientras §3.3 avanza, «obligatorio sin regla» y «ruta distinta» solo se informan (salida estándar
 * y {@code target/validacion/mutacion_anti_npe.txt}); {@code JBS-VAL-00}, una excepción del validador y
 * una falla de (b) fallan siempre. En la corrida final ({@link SoporteValidacion#CORRIDA_FINAL}) todo
 * falla: CA3-04 exige 0 NPE.</p>
 */
public class NullSafetyMutationTest {

    /** Resultado de una mutación. */
    enum Clase {
        /** El validador lo reporta como ERROR en la ruta del dato (y (b) se cumple). */
        CUBIERTO,
        /** El validador reporta un ERROR nuevo, pero en ninguna ruta del dato mutado. */
        RUTA_DISTINTA,
        /** El validador no lo reporta y la generación sin validación falla: obligatorio sin regla. */
        SIN_REGLA,
        /** El validador no lo reporta y la generación sin validación no falla (opcional o SIFEN). */
        INFORMATIVO,
        /** {@code JBS-VAL-00}, excepción del validador o falla de (b): defecto de una regla. */
        DEFECTO
    }

    /** Fila del informe. */
    record Hallazgo(String fuente, String ruta, String campo, GrafoDe.Tipo tipo, Clase clase, String detalle) {
    }

    private static final List<Hallazgo> INFORME = Collections.synchronizedList(new ArrayList<>());

    private static DEValidator validador;
    private static EventValidator validadorEventos;
    private static SifenConfig cfgSinValidacion;
    private static GenerationCtx ctxSinValidacion;
    private static SifenConfig cfgConValidacion;
    private static GenerationCtx ctxConValidacion;

    @BeforeAll
    public static void configurar() {
        validador = SoporteValidacion.validador();
        validadorEventos = SoporteValidacion.validadorEventos();
        cfgSinValidacion = SoporteValidacion.configuracion(false);
        ctxSinValidacion = SoporteValidacion.contexto(cfgSinValidacion);
        cfgConValidacion = SoporteValidacion.configuracion(true);
        ctxConValidacion = SoporteValidacion.contexto(cfgConValidacion);
    }

    /**
     * Lo que se muta: un DE del catálogo o un evento ({@code TrGesEve}), con su validador, la generación
     * del fork sin validación (el oráculo) y la comprobación (b) contra la fachada.
     */
    interface Sujeto {
        String id();

        /** Raíz nueva del grafo a mutar (instancia nueva en cada llamada). */
        Object crear();

        ValidationResult validar(Object raiz);

        /** Genera con la validación apagada; lanza si la librería falla. */
        void generarSinValidacion(Object raiz) throws Exception;

        /** (b): {@code null} si la fachada con la validación activa rechaza con los errores de la mutación. */
        String fachada(Object raiz, List<ValidationError> nuevos);
    }

    /** DE del catálogo: {@code DEValidator} y {@code generarXml}. */
    static Sujeto sujeto(SoporteValidacion.Fuente fuente) {
        return new Sujeto() {
            @Override
            public String id() {
                return fuente.id();
            }

            @Override
            public Object crear() {
                // Base corregida: un dato que en el caso congelado ya tiene su desvío también se prueba.
                return fuente.crearCorregido();
            }

            @Override
            public ValidationResult validar(Object raiz) {
                return validador.validate((DocumentoElectronico) raiz, ctxSinValidacion);
            }

            @Override
            public void generarSinValidacion(Object raiz) throws Exception {
                if (((DocumentoElectronico) raiz).generarXml(ctxSinValidacion, cfgSinValidacion) == null) {
                    throw new IllegalStateException("generarXml devolvió null (SOAPException)");
                }
            }

            @Override
            public String fachada(Object raiz, List<ValidationError> nuevos) {
                if (!SoporteValidacion.fachadaIntegrada()) {
                    return null;
                }
                try {
                    ((DocumentoElectronico) raiz).generarXml(ctxConValidacion, cfgConValidacion);
                    return "generarXml con validacionPrevia=true no lanzó SifenValidationException";
                } catch (SifenValidationException e) {
                    return conLosErrores(e, nuevos, "generarXml");
                } catch (Exception e) {
                    return "generarXml con validacionPrevia=true lanzó " + SoporteValidacion.describir(e);
                }
            }
        };
    }

    /**
     * Evento del catálogo: {@code EventValidator} sobre el {@code TrGesEve} y el mensaje de recepción
     * de eventos. (b) va contra {@code Sifen.validarEvento}, la misma validación que
     * {@code recepcionEvento} hace antes de enviar, para no abrir nunca una conexión.
     */
    static Sujeto sujeto(SoporteValidacion.FuenteEvento caso) {
        return new Sujeto() {
            @Override
            public String id() {
                return caso.id();
            }

            @Override
            public Object crear() {
                return caso.crear();
            }

            @Override
            public ValidationResult validar(Object raiz) {
                return validadorEventos.validate((TrGesEve) raiz, ctxSinValidacion);
            }

            @Override
            public void generarSinValidacion(Object raiz) throws Exception {
                if (MensajesSifen.mensajeEvento(envolver((TrGesEve) raiz), cfgSinValidacion) == null) {
                    throw new IllegalStateException("el mensaje de eventos salió nulo");
                }
            }

            @Override
            public String fachada(Object raiz, List<ValidationError> nuevos) {
                try {
                    Sifen.validarEvento(envolver((TrGesEve) raiz), ctxConValidacion).throwIfInvalid();
                    return "Sifen.validarEvento no rechazó el evento";
                } catch (SifenValidationException e) {
                    return conLosErrores(e, nuevos, "Sifen.validarEvento");
                } catch (RuntimeException e) {
                    return "Sifen.validarEvento lanzó " + SoporteValidacion.describir(e);
                }
            }
        };
    }

    static EventosDE envolver(TrGesEve evento) {
        EventosDE ev = new EventosDE();
        List<TrGesEve> l = new ArrayList<>();
        l.add(evento);
        ev.setrGesEveList(l);
        return ev;
    }

    private static String conLosErrores(SifenValidationException e, List<ValidationError> nuevos, String quien) {
        // La excepción tiene que deberse a la mutación, no solo a un error previo del caso.
        for (ValidationError err : nuevos) {
            if (e.getResult().contains(err.getCode())) {
                return null;
            }
        }
        return "la SifenValidationException de " + quien + " no trae los errores de la mutación: " + resumen(nuevos);
    }

    static Stream<SoporteValidacion.Fuente> fuentes() {
        return SoporteValidacion.fuentes().stream();
    }

    static Stream<SoporteValidacion.FuenteEvento> eventos() {
        return SoporteValidacion.fuentesEvento().stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fuentes")
    public void cadaDatoAnuladoSeReportaYNuncaTerminaEnNpe(SoporteValidacion.Fuente fuente) {
        mutarTodo(sujeto(fuente));
    }

    static Stream<SoporteValidacion.FuenteEnriquecida> enriquecidos() {
        return SoporteValidacion.fuentesEnriquecidas().stream();
    }

    /** Los DE enriquecidos validan sin errores (si no, sus errores esconderían los de la mutación). */
    @ParameterizedTest(name = "{0}")
    @MethodSource("enriquecidos")
    public void cadaDatoAnuladoDeUnDeEnriquecidoSeReportaYNuncaTerminaEnNpe(SoporteValidacion.FuenteEnriquecida fuente) {
        ValidationResult r = validador.validate(fuente.crear(), ctxSinValidacion);
        Assertions.assertTrue(r.getErrors().isEmpty(), () -> fuente + " tiene errores: " + r.getErrors());
        SoporteValidacion.Fuente base = SoporteValidacion.fuentes().get(0);
        mutarTodo(new Sujeto() {
            private final Sujeto de = sujeto(base);

            @Override
            public String id() {
                return fuente.id();
            }

            @Override
            public Object crear() {
                return fuente.crear();
            }

            @Override
            public ValidationResult validar(Object raiz) {
                return de.validar(raiz);
            }

            @Override
            public void generarSinValidacion(Object raiz) throws Exception {
                de.generarSinValidacion(raiz);
            }

            @Override
            public String fachada(Object raiz, List<ValidationError> nuevos) {
                return de.fachada(raiz, nuevos);
            }
        });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("eventos")
    public void cadaDatoAnuladoDeUnEventoSeReportaYNuncaTerminaEnNpe(SoporteValidacion.FuenteEvento caso) {
        mutarTodo(sujeto(caso));
    }

    private void mutarTodo(Sujeto sujeto) {
        Object base = sujeto.crear();
        ValidationResult rBase = sujeto.validar(base);
        Assertions.assertFalse(rBase.contains(SoporteValidacion.VAL_00),
                () -> sujeto.id() + ": JBS-VAL-00 sobre el caso sin mutar: " + rBase);
        Set<String> previos = claves(rBase.getErrors());
        // C:M3-45: la base de cada fuente genera (sin esto, una fuente cuyas mutaciones se reportan todas
        // nunca ejercita su setupSOAPElements: pasaba con la notificación y el desconocimiento).
        Object paraGenerar = sujeto.crear();
        Assertions.assertDoesNotThrow(() -> sujeto.generarSinValidacion(paraGenerar), () -> sujeto.id() + ": la base no genera");

        List<String> violaciones = new ArrayList<>();
        for (GrafoDe.Punto punto : GrafoDe.puntos(base)) {
            Hallazgo h = mutar(sujeto, punto, previos);
            INFORME.add(h);
            if (h.clase() == Clase.DEFECTO
                    || (SoporteValidacion.CORRIDA_FINAL && (h.clase() == Clase.SIN_REGLA || h.clase() == Clase.RUTA_DISTINTA))) {
                violaciones.add(h.clase() + " " + h.ruta() + " ⇐ " + h.tipo() + ": " + h.detalle());
            }
        }
        Assertions.assertTrue(violaciones.isEmpty(), () -> sujeto.id() + ": " + violaciones.size()
                + " mutaciones fallan:\n  " + String.join("\n  ", violaciones.subList(0, Math.min(40, violaciones.size()))));
    }

    /**
     * M3-55: el mensaje que se arma desde la base de las fuentes de notificación de recepción y de
     * desconocimiento trae su grupo de evento (no basta con que no lance).
     */
    @Test
    public void lasBasesDeNotificacionYDesconocimientoGeneranSuGrupo() throws Exception {
        int vistas = 0;
        for (SoporteValidacion.FuenteEvento f : SoporteValidacion.fuentesEvento()) {
            String etiqueta = f.id().contains("+notRec") ? "rGeVeNotRec" : f.id().contains("+descon") ? "rGeVeDescon" : null;
            if (etiqueta == null) {
                continue;
            }
            vistas++;
            String xml = MensajesSifen.mensajeEvento(envolver(f.crear()), cfgSinValidacion);
            Assertions.assertTrue(java.util.regex.Pattern.compile("<(\\w+:)?" + etiqueta + "[ >]").matcher(xml).find(),
                    () -> f.id() + ": el mensaje no trae <" + etiqueta + ">");
        }
        Assertions.assertEquals(4, vistas, "dos fuentes de notificación y dos de desconocimiento");
    }

    @Test
    public void laValidacionPreviaDeLaFachadaEstaIntegrada() {
        // I3-26: con §3.5 hecha es una aserción plena (antes era assume + assert del mismo valor y no
        // podía fallar). Sin la fachada, la parte (b) de la mutación no se ejercitaría.
        Assertions.assertTrue(SoporteValidacion.fachadaIntegrada(),
                "SifenConfig.setPreValidationEnabled no existe: la parte (b) de la mutación no se ejercita");
    }

    private Hallazgo mutar(Sujeto sujeto, GrafoDe.Punto punto, Set<String> previos) {
        Object de = sujeto.crear();
        String ruta = punto.tipo() == GrafoDe.Tipo.ELEMENTO_NULO ? GrafoDe.rutaElementoNulo(de, punto) : punto.ruta();
        GrafoDe.aplicar(de, punto);

        ValidationResult r;
        try {
            r = sujeto.validar(de);
        } catch (RuntimeException e) {
            // IllegalStateException del modo estricto (regla no declarada) u otra fuga del motor.
            return hallazgo(sujeto, punto, ruta, Clase.DEFECTO, "el validador lanzó " + SoporteValidacion.describir(e)
                    + (e instanceof IllegalStateException ? " (" + e.getMessage() + ")" : ""));
        }
        if (r.contains(SoporteValidacion.VAL_00)) {
            return hallazgo(sujeto, punto, ruta, Clase.DEFECTO, "JBS-VAL-00: " + r.find(SoporteValidacion.VAL_00));
        }
        List<ValidationError> nuevos = new ArrayList<>();
        for (ValidationError e : r.getErrors()) {
            if (!previos.contains(clave(e))) {
                nuevos.add(e);
            }
        }
        if (!nuevos.isEmpty()) {
            List<ValidationError> enRuta = new ArrayList<>();
            List<String> sinIndice = new ArrayList<>();
            for (ValidationError e : nuevos) {
                if (SoporteValidacion.regla(e.getCode()).isEmpty()) {
                    return hallazgo(sujeto, punto, ruta, Clase.DEFECTO, "regla fuera del catálogo: " + e.getCode());
                }
                if (rutaDelDato(e.getPath(), ruta, punto.tipo())) {
                    enRuta.add(e);
                } else if (punto.tipo() == GrafoDe.Tipo.ELEMENTO_NULO && e.getPath().startsWith(punto.ruta() + "[].")) {
                    // "lista[]" a secas es una regla de la lista entera (cantidad, repetidos): válida;
                    // "lista[].campo" es una regla del elemento reportada sin su índice: defecto.
                    sinIndice.add(e.getCode() + "@" + e.getPath());
                }
            }
            if (!sinIndice.isEmpty()) {
                return hallazgo(sujeto, punto, ruta, Clase.DEFECTO, "ruta de un elemento sin índice: " + sinIndice);
            }
            // Otra instancia con la misma mutación: la fachada no reutiliza la que vio el validador.
            Object otro = sujeto.crear();
            GrafoDe.aplicar(otro, punto);
            String fallaB = sujeto.fachada(otro, nuevos);
            if (fallaB != null) {
                return hallazgo(sujeto, punto, ruta, Clase.DEFECTO, "(b) " + fallaB);
            }
            if (enRuta.isEmpty()) {
                // Un dato opcional del que dependen otros (dCiclo ⇒ dFecIniC, E822a) se rechaza, con razón,
                // en el dependiente: si el fork genera igual, el dato no era obligatorio y no hay ruta que exigir.
                Object base = sujeto.crear();
                GrafoDe.aplicar(base, punto);
                try {
                    sujeto.generarSinValidacion(base);
                    return hallazgo(sujeto, punto, ruta, Clase.INFORMATIVO, "opcional; coherencia en otra ruta: " + resumen(nuevos));
                } catch (Exception e) {
                    return hallazgo(sujeto, punto, ruta, Clase.RUTA_DISTINTA, "reportado en otra ruta: " + resumen(nuevos)
                            + " (el fork falla: " + SoporteValidacion.describir(e) + ")");
                }
            }
            return hallazgo(sujeto, punto, ruta, Clase.CUBIERTO, resumen(enRuta));
        }
        // Sin error nuevo: el oráculo es la generación del fork con la validación apagada.
        Object otro = sujeto.crear();
        GrafoDe.aplicar(otro, punto);
        try {
            sujeto.generarSinValidacion(otro);
            return hallazgo(sujeto, punto, ruta, Clase.INFORMATIVO, "el fork genera el XML");
        } catch (Exception | Error e) {
            if (e instanceof Error err && !(e instanceof AssertionError) && !(e instanceof StackOverflowError)) {
                throw err;
            }
            return hallazgo(sujeto, punto, ruta, Clase.SIN_REGLA, SoporteValidacion.describir(e));
        }
    }

    private Hallazgo hallazgo(Sujeto sujeto, GrafoDe.Punto punto, String ruta, Clase clase, String detalle) {
        return new Hallazgo(sujeto.id(), ruta, punto.campoModelo(), punto.tipo(), clase, detalle);
    }

    /** La ruta del error corresponde al dato mutado (el dato mismo o algo debajo de él). */
    /**
     * Grupos de elección (uno y solo uno de sus hijos): anular el único hijo cargado se reporta, con
     * razón, en el grupo (JBS-EVENTO-03 «exactamente un evento»).
     */
    static final Set<String> GRUPOS_DE_ELECCION = Set.of("rEve.gGroupTiEvt");

    static boolean rutaDelDato(String rutaError, String rutaDato, GrafoDe.Tipo tipo) {
        if (GRUPOS_DE_ELECCION.contains(rutaError) && rutaDato.startsWith(rutaError + ".")
                && rutaDato.indexOf('.', rutaError.length() + 1) < 0) {
            return true;
        }
        if (tipo == GrafoDe.Tipo.GRUPO_VACIO && (rutaDato.startsWith(rutaError + ".") || rutaDato.startsWith(rutaError + "["))) {
            // El grupo vacío se colgó de eslabones que el caso no tenía y que la mutación creó también
            // vacíos: el error del eslabón creado es la respuesta correcta.
            return true;
        }
        if (rutaError.equals(rutaDato) || rutaError.startsWith(rutaDato + ".") || rutaError.startsWith(rutaDato + "[")) {
            // Para el elemento nulo, "lista[]…" no es la ruta del elemento (se trata aparte).
            return !(tipo == GrafoDe.Tipo.ELEMENTO_NULO && rutaError.startsWith(rutaDato + "[]"));
        }
        return false;
    }

    private static Set<String> claves(List<ValidationError> errores) {
        Set<String> s = new HashSet<>();
        for (ValidationError e : errores) {
            s.add(clave(e));
        }
        return s;
    }

    private static String clave(ValidationError e) {
        return e.getCode() + "@" + e.getPath();
    }

    private static String resumen(List<ValidationError> errores) {
        List<String> l = new ArrayList<>();
        for (ValidationError e : errores) {
            l.add(e.getCode() + "@" + e.getPath());
            if (l.size() == 5) {
                l.add("…");
                break;
            }
        }
        return String.join(", ", l);
    }

    @AfterAll
    public static void informe() throws IOException {
        Map<Clase, Integer> porClase = new EnumMap<>(Clase.class);
        // Campos obligatorios sin regla, agrupados por ruta canónica: qué falta en §3.3.
        Map<String, Set<String>> sinRegla = new TreeMap<>();
        Map<String, Set<String>> rutaDistinta = new TreeMap<>();
        List<String> defectos = new ArrayList<>();
        Set<String> fuentes = new TreeSet<>();
        synchronized (INFORME) {
            for (Hallazgo h : INFORME) {
                porClase.merge(h.clase(), 1, Integer::sum);
                fuentes.add(h.fuente());
                String canonica = SoporteValidacion.normalizarRuta(h.ruta()) + " ⇐ " + h.tipo();
                switch (h.clase()) {
                    case SIN_REGLA -> sinRegla.computeIfAbsent(canonica, k -> new TreeSet<>()).add(h.detalle() + " [" + h.fuente() + "]");
                    case RUTA_DISTINTA -> rutaDistinta.computeIfAbsent(canonica, k -> new TreeSet<>()).add(h.detalle());
                    case DEFECTO -> defectos.add(h.fuente() + " " + h.ruta() + " ⇐ " + h.tipo() + ": " + h.detalle());
                    default -> {
                    }
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("# Prueba de mutación anti-NPE (JBSSIF §3.4, CA3-04)\n");
        sb.append("reglas en el catálogo: ").append(SoporteValidacion.reglas().size())
                .append(" · grupos estándar: ").append(SoporteValidacion.cantidadGrupos()[0])
                .append(" DE + ").append(SoporteValidacion.cantidadGrupos()[1]).append(" eventos")
                .append(" · fachada §3.5: ").append(SoporteValidacion.fachadaIntegrada() ? "sí" : "pendiente (b simulada)")
                .append(" · corrida final: ").append(SoporteValidacion.CORRIDA_FINAL).append('\n');
        sb.append("fuentes: ").append(fuentes.size()).append(" · mutaciones: ").append(INFORME.size()).append('\n');
        for (Clase c : Clase.values()) {
            sb.append("  ").append(c).append(": ").append(porClase.getOrDefault(c, 0)).append('\n');
        }
        sb.append("\n## Obligatorios sin regla (").append(sinRegla.size()).append(" rutas)\n");
        sinRegla.forEach((k, v) -> sb.append(k).append("  —  ").append(primeros(v, 3)).append('\n'));
        sb.append("\n## Reportados en otra ruta (").append(rutaDistinta.size()).append(" rutas)\n");
        rutaDistinta.forEach((k, v) -> sb.append(k).append("  —  ").append(primeros(v, 2)).append('\n'));
        sb.append("\n## Defectos (").append(defectos.size()).append(")\n");
        defectos.forEach(d -> sb.append(d).append('\n'));
        Set<String> mutadas = new TreeSet<>();
        synchronized (INFORME) {
            for (Hallazgo h : INFORME) {
                mutadas.add(SoporteValidacion.normalizarRuta(h.ruta()) + "\t" + h.tipo() + "\t" + h.clase());
            }
        }
        Path salida = Paths.get("target", "validacion", "mutacion_anti_npe.txt");
        Files.createDirectories(salida.getParent());
        Files.write(salida.resolveSibling("rutas_mutadas.tsv"), mutadas, StandardCharsets.UTF_8);
        Files.createDirectories(salida.getParent());
        Files.writeString(salida, sb.toString(), StandardCharsets.UTF_8);
        String[] lineas = sb.toString().split("\n");
        System.out.println(String.join("\n", java.util.Arrays.copyOf(lineas, Math.min(lineas.length, 12)))
                + "\n(informe completo: " + salida.toAbsolutePath() + ")");
    }

    private static String primeros(Set<String> s, int n) {
        List<String> l = new ArrayList<>(s);
        return l.size() <= n ? String.join("; ", l) : String.join("; ", l.subList(0, n)) + "; … (" + l.size() + ")";
    }
}
