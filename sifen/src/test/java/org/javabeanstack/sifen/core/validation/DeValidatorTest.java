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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.fields.request.de.TgDatRec;
import org.javabeanstack.sifen.core.fields.request.de.TgEmis;
import org.javabeanstack.sifen.core.fields.request.de.TgOpeCom;
import org.javabeanstack.sifen.core.fields.request.de.TgRespDE;
import org.javabeanstack.sifen.core.fields.request.event.TgGroupTiEvt;
import org.javabeanstack.sifen.core.fields.request.event.TrGeVeDescon;
import org.javabeanstack.sifen.core.fields.request.event.TrGeVeNotRec;
import org.javabeanstack.sifen.core.fields.request.event.TrGeVeTr;
import org.javabeanstack.sifen.core.fields.request.event.TrGesEve;
import org.javabeanstack.sifen.core.types.TdMotEv;
import org.javabeanstack.sifen.core.types.TdTipIdenVeh;
import org.javabeanstack.sifen.core.types.TiModTrans;
import org.javabeanstack.sifen.core.types.TiNatRec;
import org.javabeanstack.sifen.core.types.TiTTrans;
import org.javabeanstack.sifen.core.types.TDepartamento;
import org.javabeanstack.sifen.core.types.TiTiOpe;
import org.javabeanstack.sifen.core.types.TiTipDocRec;
import org.javabeanstack.sifen.core.types.TiTipIDRespDE;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.internal.util.SifenUtil;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests positivos y negativos del catálogo de reglas de la validación previa (plan JBSSIF, §3.4;
 * CA3-02, CA3-03).
 *
 * <p><b>Positivos</b> (CA3-02): cada caso del catálogo, armado por objeto y leído de su XML, valida
 * sin más errores que sus desvíos conocidos ({@link SoporteValidacion#DESVIOS_ESPERADOS}, I3-14); los
 * avisos se listan.</p>
 *
 * <p><b>Negativos</b> (CA3-03): uno por regla del catálogo ({@link SoporteValidacion#reglas()}, DE y eventos), desde
 * la tabla {@code src/test/resources/validacion/negativos_de.tsv}: caso base, acción (o secuencia de
 * acciones) sobre rutas del MT y ruta esperada del error. El caso base es el del catálogo <b>con sus
 * desvíos conocidos corregidos</b> ({@link SoporteValidacion#CORRECCIONES}), así que ningún negativo se apoya en un
 * desvío: cada uno exige que la base no tenga ya el error, que después de la acción el resultado lo
 * tenga con su código, ruta concreta, severidad y mensaje, y que no aparezca {@code JBS-VAL-00}. Las
 * reglas se nombran por su código (nunca por la constante); las de {@code ReglasNoEvaluadas} no llevan
 * negativo.</p>
 *
 * <p>Fuera de la corrida final ({@link SoporteValidacion#CORRIDA_FINAL}) una regla sin negativo o una
 * fila derivada que no se confirma quedan saltadas con el motivo; en la corrida final fallan.</p>
 */
public class DeValidatorTest {

    /** Recurso de la tabla de negativos. */
    static final String TABLA = "validacion/negativos_de.tsv";
    /** Comentario que abre la sección de filas derivadas del catálogo. */
    static final String MARCA_DERIVADAS = "# --- derivadas del catálogo";
    /** Comentario que abre la sección de filas escritas a mano. */
    static final String MARCA_A_MANO = "# --- a mano";
    /** Separador de los pasos de una acción compuesta ({@code accion@ruta ; accion@ruta}). */
    static final String SEPARADOR_PASOS = " ; ";

    /** Fila de la tabla: código, fuente ({@code *}, {@code DE-01/obj}, {@code EV-05}), acción, ruta de la acción, ruta esperada. */
    record Negativo(int linea, String codigo, String fuente, String accion, String rutaAccion, String rutaEsperada,
            boolean derivada) {
        @Override
        public String toString() {
            return codigo + " (" + TABLA + ":" + linea + ")";
        }
    }

    /** Un paso de la acción: qué se hace y sobre qué ruta. */
    record Paso(String accion, String ruta) {
    }

    /** Caso base de un negativo: un DE del catálogo (con sus desvíos corregidos) o un evento. */
    record Base(String id, Supplier<Object> fabrica, boolean evento) {
        Object crear() {
            return fabrica.get();
        }

        @Override
        public String toString() {
            return id;
        }
    }

    /**
     * Acciones con nombre ({@code java:<nombre>}) para lo que no se arma con asignaciones: modifican la
     * raíz (DE o {@code TrGesEve}) y devuelven el contexto con que se valida.
     */
    static final Map<String, BiFunction<Object, GenerationCtx, GenerationCtx>> ACCIONES = acciones();

    private static DEValidator validador;
    private static EventValidator validadorEventos;
    private static SifenConfig cfg;
    private static GenerationCtx ctx;
    private static List<Negativo> tabla;

    @BeforeAll
    public static void configurar() throws IOException {
        validador = SoporteValidacion.validador();
        validadorEventos = SoporteValidacion.validadorEventos();
        cfg = SoporteValidacion.configuracion(false);
        ctx = SoporteValidacion.contexto(cfg);
        tabla = leerTabla();
    }

    // ------------------------------------------------------------------ positivos (CA3-02)

    static Stream<SoporteValidacion.Fuente> fuentes() {
        return SoporteValidacion.fuentes().stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fuentes")
    public void cadaCasoDelCatalogoValidaSinErrores(SoporteValidacion.Fuente fuente) {
        ValidationResult r = validador.validate(fuente.crear(), ctx);
        if (!r.getWarnings().isEmpty()) {
            System.out.println("[CA3-02] " + fuente + " avisos: " + r.getWarnings());
        }
        // I3-14: los desvíos conocidos de los casos congelados son los únicos errores admitidos.
        Set<String> codigos = new TreeSet<>();
        r.getErrors().forEach(e -> codigos.add(e.getCode()));
        Assertions.assertEquals(SoporteValidacion.desviosEsperados(fuente.caso().getId()), codigos,
                () -> fuente + " tiene errores: " + r.getErrors());
    }

    /** Las correcciones de {@link SoporteValidacion#CORRECCIONES} dejan cada caso sin errores (base limpia de los negativos). */
    @ParameterizedTest(name = "{0}")
    @MethodSource("fuentes")
    public void conSusDesviosCorregidosCadaCasoQuedaSinErrores(SoporteValidacion.Fuente fuente) {
        DocumentoElectronico de = fuente.crear();
        SoporteValidacion.corregir(fuente.caso().getId(), de);
        ValidationResult r = validador.validate(de, ctx);
        Assertions.assertTrue(r.getErrors().isEmpty(), () -> fuente + " corregido tiene errores: " + r.getErrors());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("eventosDelCatalogo")
    public void cadaEventoDelCatalogoValidaSinErrores(CasosDePrueba.CasoEvento caso) {
        TrGesEve ev = caso.construir().getrGesEveList().get(0);
        ValidationResult r = validadorEventos.validate(ev, ctx);
        Set<String> codigos = new TreeSet<>();
        r.getErrors().forEach(e -> codigos.add(e.getCode()));
        Assertions.assertEquals(SoporteValidacion.DESVIOS_EVENTOS.getOrDefault(caso.getId(), Set.of()), codigos,
                () -> caso + " tiene errores: " + r.getErrors());
        SoporteValidacion.corregirEvento(caso.getId(), ev);
        ValidationResult corregido = validadorEventos.validate(ev, ctx);
        Assertions.assertTrue(corregido.getErrors().isEmpty(), () -> caso + " corregido tiene errores: " + corregido.getErrors());
    }

    static Stream<CasosDePrueba.CasoEvento> eventosDelCatalogo() {
        return CasosDePrueba.eventos().stream();
    }

    // ------------------------------------------------------------------ negativos (CA3-03)

    @TestFactory
    public Stream<DynamicTest> unNegativoPorRegla() {
        Map<String, List<Negativo>> porCodigo = new LinkedHashMap<>();
        for (Negativo n : tabla) {
            porCodigo.computeIfAbsent(n.codigo(), k -> new ArrayList<>()).add(n);
        }
        Set<String> declaradas = SoporteValidacion.declaradasDe();
        declaradas.addAll(SoporteValidacion.declaradasEventos());
        Map<String, String> noEvaluadas = SoporteValidacion.noEvaluadas();
        List<DynamicTest> tests = new ArrayList<>();
        for (SoporteValidacion.Regla regla : SoporteValidacion.reglas()) {
            if (regla.codigo().equals(SoporteValidacion.VAL_00)) {
                continue; // su negativo usa un grupo sintético: elJbsVal00SoloLoProvocaUnGrupoQueLanza
            }
            List<Negativo> filas = porCodigo.getOrDefault(regla.codigo(), List.of());
            if (noEvaluadas.containsKey(regla.codigo())) {
                // La librería declara que no la evalúa (constante, derivada, calculada): por diseño.
                for (Negativo n : filas) {
                    tests.add(DynamicTest.dynamicTest(n.toString(), () -> Assumptions.abort(n
                            + ": no se evalúa localmente (" + noEvaluadas.get(regla.codigo()) + ")")));
                }
                continue;
            }
            if (filas.isEmpty()) {
                tests.add(DynamicTest.dynamicTest(regla.codigo() + " sin negativo",
                        () -> pendiente("CA3-03: la regla " + regla.codigo() + " no tiene negativo en " + TABLA)));
                continue;
            }
            for (Negativo n : filas) {
                tests.add(DynamicTest.dynamicTest(n.toString(), () -> ejecutar(regla, n, declaradas)));
            }
        }
        return tests.stream();
    }

    /**
     * C:I3-43: la premisa de las reglas bajadas a AVISO porque «el dato cargado no se envía a SIFEN»
     * (M3-42, I3-37; marcadas {@code no-se-envia} en {@code validacion/severidades.tsv}): con su negativo
     * aplicado y la validación apagada, el XML que genera la librería no trae la etiqueta.
     */
    @TestFactory
    public Stream<DynamicTest> lasReglasNoSeEnviaNoLleganAlXml() throws Exception {
        Map<String, List<Negativo>> porCodigo = new LinkedHashMap<>();
        for (Negativo n : tabla) {
            porCodigo.computeIfAbsent(n.codigo(), k -> new ArrayList<>()).add(n);
        }
        List<DynamicTest> tests = new ArrayList<>();
        for (Map.Entry<String, String[]> e : SeveridadesReglasTest.tabla().entrySet()) {
            if (!e.getValue()[1].startsWith("no-se-envia")) {
                continue;
            }
            String codigo = e.getKey();
            SoporteValidacion.Regla regla = SoporteValidacion.regla(codigo).orElseThrow();
            List<Negativo> filas = porCodigo.getOrDefault(codigo, List.of());
            tests.add(DynamicTest.dynamicTest(codigo, () -> {
                Assertions.assertFalse(filas.isEmpty(), codigo + ": sin negativo en " + TABLA);
                int generados = 0;
                List<String> sinGenerar = new ArrayList<>();
                for (Negativo n : filas) {
                    Base base = elegirBase(n, regla.evento());
                    Object raiz = base.crear();
                    aplicar(n, raiz);
                    String xml;
                    try {
                        xml = raiz instanceof TrGesEve ev
                                ? org.javabeanstack.sifen.internal.request.MensajesSifen.mensajeEvento(NullSafetyMutationTest.envolver(ev), cfg)
                                : ((DocumentoElectronico) raiz).generarXml(ctx, cfg);
                    } catch (Exception x) {
                        // El negativo también rompe otra regla en ERROR que la frena (p. ej. E983c sin el
                        // documento del transportista ⇒ E985): no mide la premisa; otra fila sí. Pero si
                        // la validación del mismo estado solo da AVISOs, el AVISO deja pasar un dato con el
                        // que la generación falla (como E730a con gCamIVA vacío): eso es un defecto.
                        Object mismo = base.crear();
                        GenerationCtx ctxMismo = aplicar(n, mismo);
                        ValidationResult rv = validar(mismo, ctxMismo);
                        Assertions.assertFalse(rv.isValid(),
                                () -> n + ": la generación falla (" + x + ") y la validación no da ningún ERROR que la frene: " + rv.getAll());
                        sinGenerar.add(n + ": " + x.getMessage());
                        continue;
                    }
                    generados++;
                    String[] segs = regla.ruta().replace("[]", "").split("\\.");
                    String etiqueta = segs[segs.length - 1];
                    Assertions.assertFalse(java.util.regex.Pattern.compile("<(\\w+:)?" + etiqueta + "[ >/]").matcher(xml).find(),
                            () -> n + ": la librería escribe <" + etiqueta + "> aunque la regla está en AVISO por «no se envía»");
                }
                Assertions.assertTrue(generados > 0, () -> codigo + ": ningún negativo genera XML para medir la premisa: " + sinGenerar);
            }));
        }
        return tests.stream();
    }

    private void ejecutar(SoporteValidacion.Regla regla, Negativo n, Set<String> declaradas) {
        String rutaEsperada = esperada(n);
        Assertions.assertEquals(regla.ruta(), SoporteValidacion.normalizarRuta(rutaEsperada),
                () -> n + ": la ruta esperada no corresponde a la ruta canónica de la regla");
        boolean interna = SoporteValidacion.DEL_MOTOR.contains(regla.codigo());
        if (!interna && !declaradas.contains(regla.codigo())) {
            pendiente(n + ": ningún grupo estándar declara la regla");
            return;
        }
        boolean evento = regla.evento();
        Base base = elegirBase(n, evento);
        if (base == null && n.derivada()) {
            pendiente("fila derivada sin confirmar: " + n + ": ningún caso del catálogo tiene " + n.rutaAccion());
            return;
        }
        Assertions.assertNotNull(base, () -> n + ": ningún caso del catálogo tiene cargado " + n.rutaAccion());

        ValidationResult antes = validar(base.crear(), ctx);
        Assertions.assertFalse(tiene(antes, regla.codigo(), rutaEsperada),
                () -> n + ": el caso base " + base + " ya tiene el error; el negativo no prueba nada");

        Object raiz = base.crear();
        GenerationCtx ctxUsado = aplicar(n, raiz);
        ValidationResult r = regla.codigo().equals(SoporteValidacion.VAL_01) ? validador.validate(null, ctxUsado)
                : validar(raiz, ctxUsado);
        Assertions.assertFalse(r.contains(SoporteValidacion.VAL_00),
                () -> n + ": JBS-VAL-00 en el negativo: " + r.find(SoporteValidacion.VAL_00));
        if (!tiene(r, regla.codigo(), rutaEsperada)) {
            String motivo = n + " sobre " + base + ": se esperaba " + regla.codigo() + "@" + rutaEsperada
                    + "; se obtuvo " + r.getAll();
            if (n.derivada()) {
                // Fila derivada del catálogo con el caso elegido por "*": en una regla condicional el caso
                // puede no cumplir la condición. Se confirma a mano antes de la corrida final.
                pendiente("fila derivada sin confirmar: " + motivo);
                return;
            }
            Assertions.fail(motivo);
        }
        for (ValidationError e : r.find(regla.codigo())) {
            Assertions.assertEquals(regla.severidad(), e.getSeverity(), () -> n + ": severidad");
            Assertions.assertFalse(e.getMessage().isBlank(), () -> n + ": mensaje vacío");
            Assertions.assertTrue(regla.tieneMensaje(), () -> n + ": sin mensaje en el recurso");
        }
    }

    private static ValidationResult validar(Object raiz, GenerationCtx c) {
        if (raiz instanceof TrGesEve evento) {
            return validadorEventos.validate(evento, c);
        }
        return validador.validate((DocumentoElectronico) raiz, c);
    }

    @Test
    public void elJbsVal00SoloLoProvocaUnGrupoQueLanza() {
        DEValidator v = SoporteValidacion.validadorConGrupoQueLanza("sintetico", "dato sensible que no debe salir");
        ValidationResult r = v.validate(SoporteValidacion.fuentes().get(0).crear(), ctx);
        List<ValidationError> val00 = r.find(SoporteValidacion.VAL_00);
        Assertions.assertEquals(1, val00.size(), r::toString);
        Assertions.assertEquals("DE", val00.get(0).getPath());
        Assertions.assertFalse(val00.get(0).getMessage().contains("dato sensible"));
        Assertions.assertTrue(val00.get(0).getMessage().contains("sintetico"));
    }

    // ------------------------------------------------------------------ completitud

    @Test
    public void cadaReglaDelCatalogoTieneSuNegativo() {
        Set<String> enTabla = new HashSet<>();
        for (Negativo n : tabla) {
            enTabla.add(n.codigo());
        }
        Map<String, String> noEvaluadas = SoporteValidacion.noEvaluadas();
        Set<String> faltan = new TreeSet<>();
        for (SoporteValidacion.Regla regla : SoporteValidacion.reglas()) {
            if (!regla.codigo().equals(SoporteValidacion.VAL_00) && !enTabla.contains(regla.codigo())
                    && !noEvaluadas.containsKey(regla.codigo())) {
                faltan.add(regla.codigo());
            }
        }
        if (!faltan.isEmpty()) {
            pendiente("CA3-03: " + faltan.size() + " de " + (SoporteValidacion.reglas().size() - 1 - noEvaluadas.size())
                    + " reglas evaluables sin negativo en " + TABLA + ": " + faltan);
        }
    }

    @Test
    public void laTablaSoloNombraReglasDelCatalogoConSuRutaCanonica() {
        List<String> malas = new ArrayList<>();
        Map<String, String> noEvaluadas = SoporteValidacion.noEvaluadas();
        for (Negativo n : tabla) {
            SoporteValidacion.Regla regla = SoporteValidacion.regla(n.codigo()).orElse(null);
            if (regla == null) {
                malas.add(n + ": código fuera del catálogo");
                continue;
            }
            if (!regla.ruta().equals(SoporteValidacion.normalizarRuta(esperada(n)))) {
                malas.add(n + ": ruta esperada " + esperada(n) + " ≠ canónica " + regla.ruta());
            }
            for (Paso p : pasos(n)) {
                if (p.accion().startsWith("java:")) {
                    continue;
                }
                if (!rutaDelModelo(p.ruta())) {
                    malas.add(n + ": la ruta de la acción " + p.ruta() + " no existe en el modelo");
                } else if (!campoConSetter(p.ruta()) && !SOLO_POR_XML.containsKey(n.codigo())
                        && !noEvaluadas.containsKey(n.codigo())) {
                    // Sin setter el consumidor no puede dejar el modelo en ese estado: o se prueba por la
                    // lectura de un XML (SOLO_POR_XML) o la regla es imposible de provocar y se lista.
                    malas.add(n + ": el campo de " + p.ruta() + " no tiene setter público");
                }
            }
        }
        Assertions.assertTrue(malas.isEmpty(), () -> String.join("\n", malas));
    }

    /** M3-10: lo que declaran los grupos estándar cubre todas las reglas del catálogo (salvo las del motor). */
    @Test
    public void losGruposEstandarDeclaranTodasLasReglasDelCatalogo() {
        Set<String> declaradas = SoporteValidacion.declaradasDe();
        Map<String, String> noEvaluadas = SoporteValidacion.noEvaluadas();
        Set<String> deEventos = SoporteValidacion.declaradasEventos();
        Set<String> sinGrupo = new TreeSet<>();
        for (SoporteValidacion.Regla regla : SoporteValidacion.reglas()) {
            boolean delMotor = SoporteValidacion.DEL_MOTOR.contains(regla.codigo());
            Set<String> deSuValidador = regla.evento() ? deEventos : declaradas;
            if (!delMotor && !deSuValidador.contains(regla.codigo()) && !noEvaluadas.containsKey(regla.codigo())) {
                sinGrupo.add(regla.codigo());
            }
        }
        if (!sinGrupo.isEmpty()) {
            pendiente("M3-10: " + sinGrupo.size() + " reglas sin grupo estándar (DEValidator o EventValidator) que las"
                    + " declare ni motivo de no evaluada: " + sinGrupo);
        }
    }

    // ------------------------------------------------------------------ bases

    /** Bases del tipo pedido, en orden: DE por objeto, DE leídos del XML; o los eventos del catálogo. */
    static List<Base> bases(boolean evento) {
        List<Base> l = new ArrayList<>();
        if (evento) {
            for (CasosDePrueba.CasoEvento c : CasosDePrueba.eventos()) {
                l.add(new Base(c.getId(), () -> {
                    TrGesEve ev = c.construir().getrGesEveList().get(0);
                    SoporteValidacion.corregirEvento(c.getId(), ev);
                    return ev;
                }, true));
            }
            return l;
        }
        for (SoporteValidacion.Fuente f : SoporteValidacion.fuentes()) {
            l.add(new Base(f.id(), () -> {
                DocumentoElectronico de = f.crear();
                SoporteValidacion.corregir(f.caso().getId(), de);
                return de;
            }, false));
        }
        return l;
    }

    /** La base indicada por la fila ({@code DE-01/xml}, {@code DE-05} = por objeto, {@code EV-03}). */
    static Base base(String spec, boolean evento) {
        String id = spec.contains("/") ? spec : (evento ? spec : spec + "/obj");
        for (Base b : bases(evento)) {
            if (b.id().equals(id)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Caso base inexistente: " + spec);
    }

    /**
     * Elige la base de la fila: la indicada, o con {@code *} la primera del catálogo en la que la ruta
     * del primer paso existe y, para falta/nulo/vacío/listas, tiene valor; si ninguna, la primera en la
     * que se puede crear el grupo («grupo presente, dato faltante»).
     */
    static Base elegirBase(Negativo n, boolean evento) {
        if (!"*".equals(n.fuente())) {
            return base(n.fuente(), evento);
        }
        Paso p = pasos(n).get(0);
        if (p.accion().startsWith("java:")) {
            return bases(evento).get(0);
        }
        boolean exigeValor = p.accion().equals("falta") || p.accion().equals("nulo") || p.accion().equals("vacio")
                || p.accion().equals("lista-vacia") || p.accion().equals("+nulo");
        for (Base b : bases(evento)) {
            GrafoDe.Destino d = GrafoDe.resolver(b.crear(), concreta(p.ruta()));
            if (d == null) {
                continue;
            }
            Object v = d.valor();
            if (!exigeValor || (v != null && !GrafoDe.esCero(v) && ValidationContext.present(v))) {
                return b;
            }
        }
        if (!p.accion().equals("vacio") && !p.accion().equals("+nulo")) {
            for (Base b : bases(evento)) {
                if (GrafoDe.crearCamino(b.crear(), concreta(p.ruta())) != null) {
                    return b;
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ acciones

    /** Pasos de la fila: {@code accion@ruta ; accion@ruta} en la columna de acción, o uno solo. */
    static List<Paso> pasos(Negativo n) {
        List<Paso> l = new ArrayList<>();
        if (n.accion().contains("@")) {
            for (String parte : n.accion().split(SEPARADOR_PASOS)) {
                int a = parte.lastIndexOf('@');
                l.add(new Paso(parte.substring(0, a).trim(), parte.substring(a + 1).trim()));
            }
        } else {
            l.add(new Paso(n.accion(), n.rutaAccion()));
        }
        return l;
    }

    /** Aplica los pasos de la fila sobre {@code raiz} y devuelve el contexto con que se valida. */
    static GenerationCtx aplicar(Negativo n, Object raiz) {
        GenerationCtx c = ctx;
        for (Paso p : pasos(n)) {
            c = aplicar(n, p, raiz, c);
        }
        return c;
    }

    private static GenerationCtx aplicar(Negativo n, Paso p, Object raiz, GenerationCtx c) {
        String a = p.accion();
        if (a.startsWith("java:")) {
            BiFunction<Object, GenerationCtx, GenerationCtx> f = ACCIONES.get(a.substring(5));
            Assertions.assertNotNull(f, () -> n + ": acción inexistente " + a);
            return f.apply(raiz, c);
        }
        String ruta = concreta(p.ruta());
        GrafoDe.Destino d = GrafoDe.resolver(raiz, ruta);
        if (d == null) {
            // Grupo opcional que el caso no carga: se crean vacíos los eslabones que faltan.
            d = GrafoDe.crearCamino(raiz, ruta);
        }
        Assertions.assertNotNull(d, () -> n + ": no se pudo resolver " + p.ruta());
        Class<?> tipo = d.tipo();
        if (a.equals("falta")) {
            if (d.indice() >= 0) {
                d.poner(null);
            } else if (tipo.isPrimitive()) {
                d.poner(GrafoDe.cero(tipo));
            } else if (List.class.isAssignableFrom(tipo)) {
                d.poner(new ArrayList<>());
            } else {
                d.poner(null);
            }
        } else if (a.equals("nulo")) {
            d.poner(null);
        } else if (a.equals("vacio")) {
            d.poner("");
        } else if (a.equals("lista-vacia")) {
            d.poner(new ArrayList<>());
        } else if (a.equals("+nulo")) {
            List<Object> l = new ArrayList<>((List<?>) d.valor());
            l.add(null);
            d.poner(l);
        } else if (a.equals("crear")) {
            // Grupo nuevo vacío (en una lista sin índice: una lista con un elemento vacío).
            if (d.indice() < 0 && List.class.isAssignableFrom(d.campo().getType())) {
                List<Object> l = new ArrayList<>();
                l.add(GrafoDe.nuevoGrupo(GrafoDe.elemento(d.campo())));
                d.poner(l);
            } else {
                d.poner(GrafoDe.nuevoGrupo(tipo));
            }
        } else if (a.startsWith("repetir:")) {
            // La lista crece hasta N elementos repitiendo el primero.
            int total = Integer.parseInt(a.substring(8));
            List<Object> l = new ArrayList<>((List<?>) d.valor());
            Assertions.assertFalse(l.isEmpty(), () -> n + ": repetir sobre una lista vacía");
            while (l.size() < total) {
                l.add(l.get(0));
            }
            d.poner(l);
        } else if (a.startsWith("lista:")) {
            // Lista de escalares con los valores dados (separados por coma).
            Class<?> elemento = GrafoDe.elemento(d.campo());
            List<Object> l = new ArrayList<>();
            for (String v : a.substring(6).split(",")) {
                l.add(GrafoDe.convertir(elemento, v.trim()));
            }
            d.poner(l);
        } else if (a.startsWith("dv-mal:")) {
            // DV distinto del módulo 11 del RUC que está en la ruta dada.
            GrafoDe.Destino ruc = GrafoDe.resolver(raiz, concreta(a.substring(7)));
            Assertions.assertNotNull(ruc, () -> n + ": no se pudo resolver el RUC de " + a);
            int bueno = Integer.parseInt(SifenUtil.generateDv(String.valueOf(ruc.valor())));
            d.poner(GrafoDe.convertir(tipo, String.valueOf((bueno + 1) % 10)));
        } else if (a.startsWith("largo:")) {
            d.poner("A".repeat(Integer.parseInt(a.substring(6))));
        } else if (a.startsWith("digitos:")) {
            d.poner("1".repeat(Integer.parseInt(a.substring(8))));
        } else if (a.startsWith("set:")) {
            d.poner(GrafoDe.convertir(tipo, a.substring(4)));
        } else {
            Assertions.fail(n + ": acción desconocida " + a);
        }
        return c;
    }

    private static Map<String, BiFunction<Object, GenerationCtx, GenerationCtx>> acciones() {
        Map<String, BiFunction<Object, GenerationCtx, GenerationCtx>> m = new LinkedHashMap<>();
        m.put("sinContexto", (r, c) -> null);
        // §6.3 (D25, app-developer): «sinNotaTecnica13» sale con el flag; JBS-VAL-03 está de baja
        m.put("documentoNulo", (r, c) -> c);
        m.put("ambienteProd", (r, c) -> {
            SifenConfig prod = SoporteValidacion.configuracion(false);
            prod.setEnvironment(SifenConfig.Environment.PROD);
            return FixturesSifen.contexto(prod);
        });
        m.put("prodConLeyenda", (r, c) -> {
            de(r).getgDatGralOpe().getgEmis().setdNomEmi(LEYENDA_PRUEBAS);
            SifenConfig prod = SoporteValidacion.configuracion(false);
            prod.setEnvironment(SifenConfig.Environment.PROD);
            return FixturesSifen.contexto(prod);
        });
        m.put("nreConOpeCom", (r, c) -> {
            de(r).getgDatGralOpe().setgOpeCom(new TgOpeCom());
            return c;
        });
        m.put("respDESinTipo", (r, c) -> responsable(de(r), x -> x.setiTipIDRespDE(null), c));
        m.put("respDEOtroSinTexto", (r, c) -> responsable(de(r), x -> x.setiTipIDRespDE(TiTipIDRespDE.OTRO), c));
        m.put("respDESinNumero", (r, c) -> responsable(de(r), x -> x.setdNumIDRespDE(null), c));
        m.put("respDESinNombre", (r, c) -> responsable(de(r), x -> x.setdNomRespDE(null), c));
        m.put("respDESinCargo", (r, c) -> responsable(de(r), x -> x.setdCarRespDE(null), c));
        m.put("dvReceptorIncorrecto", (r, c) -> {
            TgDatRec rec = de(r).getgDatGralOpe().getgDatRec();
            int bueno = Integer.parseInt(SifenUtil.generateDv(rec.getdRucRec()));
            rec.setdDVRec((short) ((bueno + 1) % 10));
            return c;
        });
        m.put("innominado", (r, c) -> {
            de(r).getgDatGralOpe().getgDatRec().setiTipIDRec(TiTipDocRec.INNOMINADO);
            return c;
        });
        m.put("innominadoB2B", (r, c) -> {
            de(r).getgDatGralOpe().getgDatRec().setiTipIDRec(TiTipDocRec.INNOMINADO);
            de(r).getgDatGralOpe().getgDatRec().setiTiOpe(TiTiOpe.B2B);
            return c;
        });
        m.put("b2fConDepartamento", (r, c) -> {
            de(r).getgDatGralOpe().getgDatRec().setiTiOpe(TiTiOpe.B2F);
            de(r).getgDatGralOpe().getgDatRec().setcDepRec(TDepartamento.CAPITAL);
            return c;
        });
        m.put("b2fConCiudad", (r, c) -> {
            de(r).getgDatGralOpe().getgDatRec().setiTiOpe(TiTiOpe.B2F);
            de(r).getgDatGralOpe().getgDatRec().setcCiuRec(1);
            return c;
        });
        // Eventos que ningún caso del catálogo trae: se reemplaza el grupo por uno válido de ese tipo.
        m.put("notRec1", (r, c) -> evento(r, g -> g.setrGeVeNotRec(SoporteValidacion.notificacion(true))));
        m.put("notRec2", (r, c) -> evento(r, g -> g.setrGeVeNotRec(SoporteValidacion.notificacion(false))));
        m.put("descon1", (r, c) -> evento(r, g -> g.setrGeVeDescon(SoporteValidacion.desconocimiento(true))));
        m.put("descon2", (r, c) -> evento(r, g -> g.setrGeVeDescon(SoporteValidacion.desconocimiento(false))));
        for (int motivo = 1; motivo <= 4; motivo++) {
            int mot = motivo;
            m.put("tr" + mot, (r, c) -> evento(r, g -> g.setrGeVeTr(SoporteValidacion.transporte(mot))));
        }
        m.put("dosEventos", (r, c) -> {
            ((TrGesEve) r).getgGroupTiEvt().setrGeVeNotRec(SoporteValidacion.notificacion(true));
            return c;
        });
        m.put("sinEvento", (r, c) -> {
            ((TrGesEve) r).setgGroupTiEvt(new TgGroupTiEvt());
            return c;
        });
        return Collections.unmodifiableMap(m);
    }

    /** Reemplaza el grupo del evento por uno nuevo armado por {@code armar} (devuelve el contexto de siempre). */
    private static GenerationCtx evento(Object raiz, java.util.function.Consumer<TgGroupTiEvt> armar) {
        TgGroupTiEvt g = new TgGroupTiEvt();
        armar.accept(g);
        ((TrGesEve) raiz).setgGroupTiEvt(g);
        return ctx;
    }

    private static DocumentoElectronico de(Object raiz) {
        return (DocumentoElectronico) raiz;
    }

    /** Leyenda del nombre del emisor en el ambiente de pruebas (MT 12.4, D105). */
    static final String LEYENDA_PRUEBAS = "DE generado en ambiente de prueba - sin valor comercial ni fiscal";

    /** Agrega al emisor un responsable completo y le aplica el cambio del negativo. */
    private static GenerationCtx responsable(DocumentoElectronico de, java.util.function.Consumer<TgRespDE> cambio,
            GenerationCtx c) {
        TgRespDE r = new TgRespDE();
        r.setiTipIDRespDE(TiTipIDRespDE.CEDULA_PARAGUAYA);
        r.setdNumIDRespDE("1234567");
        r.setdNomRespDE("Responsable de Prueba");
        r.setdCarRespDE("Gerente general");
        cambio.accept(r);
        TgEmis e = de.getgDatGralOpe().getgEmis();
        e.setgRespDE(r);
        return c;
    }

    // ------------------------------------------------------------------ utilitarios

    /** Ruta concreta: cada {@code []} de la canónica pasa a {@code [0]}. */
    static String concreta(String ruta) {
        return ruta.replace("[]", "[0]");
    }

    /** Ruta esperada del error: la de la columna, o con {@code =} la del (único) paso con {@code [0]}. */
    private static String esperada(Negativo n) {
        return "=".equals(n.rutaEsperada()) ? concreta(pasos(n).get(0).ruta()) : n.rutaEsperada();
    }

    private static boolean tiene(ValidationResult r, String codigo, String ruta) {
        for (ValidationError e : r.find(codigo)) {
            if (e.getPath().equals(ruta)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Negativos cuyo campo no tiene setter público: el estado solo llega por la lectura de un XML
     * ({@code setValueFromChildNode}); código ⇒ motivo. La fila lo asigna por reflexión.
     */
    static final Map<String, String> SOLO_POR_XML = Map.of();

    /** {@code true} si el campo final de la ruta tiene setter público. */
    private static boolean campoConSetter(String ruta) {
        Class<?> c = DocumentoElectronico.class;
        String r = ruta;
        if (r.startsWith(GrafoDe.RAIZ_EVENTO + ".")) {
            c = TrGesEve.class;
            r = r.substring(GrafoDe.RAIZ_EVENTO.length() + 1);
        }
        String[] segs = r.split("\\.");
        Field f = null;
        for (String seg : segs) {
            String nombre = seg.contains("[") ? seg.substring(0, seg.indexOf('[')) : seg;
            f = GrafoDe.campoMt(c, nombre);
            if (f == null) {
                return false;
            }
            c = elementoDe(f);
        }
        return f != null && GrafoDe.tieneSetter(f);
    }

    private static boolean rutaDelModelo(String ruta) {
        Class<?> c = DocumentoElectronico.class;
        String r = ruta;
        if (r.startsWith(GrafoDe.RAIZ_EVENTO + ".")) {
            c = TrGesEve.class;
            r = r.substring(GrafoDe.RAIZ_EVENTO.length() + 1);
        }
        for (String seg : r.split("\\.")) {
            String nombre = seg.contains("[") ? seg.substring(0, seg.indexOf('[')) : seg;
            if (c == null) {
                return false;
            }
            Field f = GrafoDe.campoMt(c, nombre);
            if (f == null) {
                return false;
            }
            c = elementoDe(f);
        }
        return true;
    }

    private static Class<?> elementoDe(Field f) {
        if (List.class.isAssignableFrom(f.getType())
                && f.getGenericType() instanceof java.lang.reflect.ParameterizedType pt
                && pt.getActualTypeArguments()[0] instanceof Class<?> k) {
            return GrafoDe.esModelo(k) ? k : null;
        }
        return GrafoDe.esModelo(f.getType()) ? f.getType() : null;
    }

    /** Lo pendiente de §3.3 se salta con el motivo; en la corrida final falla. */
    private static void pendiente(String motivo) {
        if (SoporteValidacion.CORRIDA_FINAL) {
            Assertions.fail(motivo);
        }
        Assumptions.abort(motivo);
    }

    static List<Negativo> leerTabla() throws IOException {
        List<Negativo> lista = new ArrayList<>();
        try (InputStream in = DeValidatorTest.class.getClassLoader().getResourceAsStream(TABLA)) {
            Assertions.assertNotNull(in, "falta el recurso " + TABLA);
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String linea;
            int nro = 0;
            boolean derivadas = false;
            while ((linea = br.readLine()) != null) {
                nro++;
                if (linea.startsWith(MARCA_DERIVADAS)) {
                    derivadas = true;
                } else if (linea.startsWith(MARCA_A_MANO)) {
                    derivadas = false;
                }
                if (linea.isBlank() || linea.startsWith("#")) {
                    continue;
                }
                String[] c = linea.split("\t", -1);
                if (c.length < 5) {
                    throw new IllegalStateException(TABLA + ":" + nro + ": se esperaban 5 columnas");
                }
                lista.add(new Negativo(nro, c[0].trim(), c[1].trim(), c[2].trim(), c[3].trim(), c[4].trim(),
                        derivadas && "*".equals(c[1].trim())));
            }
        }
        return lista;
    }
}
