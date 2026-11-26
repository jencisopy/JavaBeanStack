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
package org.javabeanstack.sifen.test.fase12;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.validation.DEValidator;
import org.javabeanstack.sifen.core.validation.EventValidator;
import org.javabeanstack.sifen.core.validation.SifenValidationException;
import org.javabeanstack.sifen.core.validation.ValidationError;
import org.javabeanstack.sifen.core.validation.ValidationResult;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.test.casos.CasosFase12;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Validación previa de la batería de la Fase 12 (plan JBSSIF, §12.2.3; CA12-03): cada fila de
 * {@code casos/fase12/esperados.tsv} da <b>exactamente</b> los errores y los avisos de la fila; si no tiene
 * errores, la librería genera el XML con la validación activa, y si los tiene, la fachada rechaza con
 * {@link SifenValidationException} y esos mismos códigos (sin NPE).
 *
 * <p>Deja en {@code target/fase12/validacion.tsv} lo obtenido por fila (evidencia de §12.2).</p>
 */
public class BateriaFacturacionTest {
    private static final List<String> FILAS = Collections.synchronizedList(new ArrayList<>());
    private static DEValidator validador;
    private static EventValidator validadorEventos;
    private static SifenConfig cfg;
    private static GenerationCtx ctx;
    private static SifenConfig cfgConValidacion;
    private static GenerationCtx ctxConValidacion;

    @BeforeAll
    public static void configurar() {
        validador = EsperadosFase12.validador();
        validadorEventos = EsperadosFase12.validadorEventos();
        cfg = FixturesSifen.configuracion();
        ctx = FixturesSifen.contexto(cfg);
        cfgConValidacion = FixturesSifen.configuracionConValidacion();
        ctxConValidacion = FixturesSifen.contexto(cfgConValidacion);
    }

    @AfterAll
    public static void escribir() throws Exception {
        Path destino = Paths.get("target", "fase12", "validacion.tsv");
        Files.createDirectories(destino.getParent());
        List<String> l = new ArrayList<>();
        l.add("caso\tclase\terrores obtenidos\tavisos obtenidos\tcoincide\tmensajes");
        List<String> ordenadas = new ArrayList<>(FILAS);
        List<String> orden = EsperadosFase12.leer().stream().map(EsperadosFase12.Esperado::caso).toList();
        ordenadas.sort((a, b) -> Integer.compare(orden.indexOf(a.split("\t")[0]), orden.indexOf(b.split("\t")[0])));
        l.addAll(ordenadas);
        Files.write(destino, l, StandardCharsets.UTF_8);
    }

    static Stream<EsperadosFase12.Esperado> documentos() {
        return EsperadosFase12.leer().stream().filter(e -> !e.esEvento() && !e.esOperacion());
    }

    static Stream<EsperadosFase12.Esperado> eventos() {
        return EsperadosFase12.leer().stream().filter(EsperadosFase12.Esperado::esEvento);
    }

    private static Set<String> codigos(List<ValidationError> l) {
        return l.stream().map(ValidationError::getCode).collect(Collectors.toCollection(TreeSet::new));
    }

    private static void registrar(EsperadosFase12.Esperado e, ValidationResult r, boolean coincide) {
        String mensajes = r.getAll().stream().map(v -> v.getCode() + " " + v.getPath() + ": " + v.getMessage())
                .collect(Collectors.joining(" | ")).replace("\t", " ").replace("\n", " ");
        FILAS.add(e.caso() + "\t" + e.clase() + "\t" + vacio(codigos(r.getErrors())) + "\t" + vacio(codigos(r.getWarnings()))
                + "\t" + (coincide ? "sí" : "NO") + "\t" + mensajes);
    }

    private static String vacio(Set<String> s) {
        return s.isEmpty() ? "-" : String.join(",", s);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("documentos")
    public void elDocumentoDaExactamenteLoEsperado(EsperadosFase12.Esperado e) throws Exception {
        ValidationResult r = validador.validate(e.documento(), ctx);
        Set<String> errores = codigos(r.getErrors());
        Set<String> avisos = codigos(r.getWarnings());
        boolean coincide = errores.equals(e.errores()) && avisos.equals(e.avisos());
        registrar(e, r, coincide);
        Assertions.assertFalse(r.contains("JBS-VAL-00"), r::toString);
        Assertions.assertEquals(e.errores(), errores, () -> e.caso() + " (" + e.nota() + ") errores: " + r);
        Assertions.assertEquals(e.avisos(), avisos, () -> e.caso() + " (" + e.nota() + ") avisos: " + r);
        DocumentoElectronico de = e.documento();
        if (errores.isEmpty()) {
            Assertions.assertNotNull(de.generarXml(ctxConValidacion, cfgConValidacion), e.caso());
        } else {
            SifenValidationException x = Assertions.assertThrows(SifenValidationException.class,
                    () -> de.generarXml(ctxConValidacion, cfgConValidacion), e.caso());
            Assertions.assertEquals(errores, codigos(x.getResult().getErrors()), e.caso() + ": la fachada rechaza con los mismos códigos");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("eventos")
    public void elEventoDaExactamenteLoEsperado(EsperadosFase12.Esperado e) {
        ValidationResult r = validadorEventos.validate(e.evento(), ctx);
        Set<String> errores = codigos(r.getErrors());
        Set<String> avisos = codigos(r.getWarnings());
        registrar(e, r, errores.equals(e.errores()) && avisos.equals(e.avisos()));
        Assertions.assertFalse(r.contains("JBS-VAL-00"), r::toString);
        Assertions.assertEquals(e.errores(), errores, () -> e.caso() + " errores: " + r);
        Assertions.assertEquals(e.avisos(), avisos, () -> e.caso() + " avisos: " + r);
    }

    /** El catálogo y los esperados se cubren en los dos sentidos: ningún caso sin fila ni fila sin caso. */
    @Test
    public void elCatalogoYLosEsperadosCoinciden() {
        Set<String> filas = new HashSet<>();
        for (EsperadosFase12.Esperado e : EsperadosFase12.leer()) {
            Assertions.assertTrue(filas.add(e.caso()), "fila repetida: " + e.caso());
            if ("f12".equals(e.origen())) {
                Assertions.assertTrue(e.esEvento() || CasosFase12.todos().containsKey(e.caso()), "sin caso: " + e.caso());
            }
        }
        for (String id : CasosFase12.todos().keySet()) {
            Assertions.assertTrue(filas.contains(id), "caso sin fila en los esperados: " + id);
        }
        for (CasosFase12.CasoEvento ev : CasosFase12.eventos()) {
            Assertions.assertTrue(filas.contains(ev.id()), "evento sin fila: " + ev.id());
        }
        for (String id : CasosFase12.eventosNegativos().keySet()) {
            Assertions.assertTrue(filas.contains(id), "evento sin fila: " + id);
        }
    }

    /** Todo código esperado existe en el catálogo de reglas (un código mal escrito haría pasar un negativo vacío). */
    @Test
    public void cadaCodigoEsperadoEstaEnElCatalogo() {
        Set<String> catalogo = new TreeSet<>();
        validador.getGroups().forEach(g -> g.getRules().forEach(r -> catalogo.add(r.getCode())));
        validadorEventos.getGroups().forEach(g -> g.getRules().forEach(r -> catalogo.add(r.getCode())));
        TreeMap<String, String> faltan = new TreeMap<>();
        for (EsperadosFase12.Esperado e : EsperadosFase12.leer()) {
            for (String c : e.errores()) {
                if (!catalogo.contains(c)) {
                    faltan.put(e.caso(), c);
                }
            }
            for (String c : e.avisos()) {
                if (!catalogo.contains(c)) {
                    faltan.put(e.caso(), c);
                }
            }
        }
        Assertions.assertEquals(new TreeMap<>(), faltan);
    }
}
