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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.fields.request.event.TrGesEve;
import org.javabeanstack.sifen.core.validation.rules.de.DERuleSets;
import org.javabeanstack.sifen.core.validation.rules.event.EventRuleSets;
import org.javabeanstack.sifen.core.validation.rules.config.ConfigRuleSets;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Consistencia del catálogo de reglas en código (plan JBSSIF, §3.6, D33; reemplaza a
 * {@code ReglaValidacionTest} y {@code ReglasEstandarTest}): códigos únicos y bien formados, prefijo
 * {@code JBS-} del grupo de su conjunto, ruta bajo la base del conjunto, un mensaje por regla y ninguno
 * huérfano (M3-11), cada regla con chequeo <b>o</b> motivo, todo conjunto del paquete registrado en su
 * lista, y la exportación del TSV que lee {@code sifen_catalogo_reglas.py --generar-md}.
 */
public class CatalogoReglasTest {
    /** Las reglas del motor no las declara ningún conjunto. */
    private static final Set<String> MOTOR = Set.of("JBS-VAL-00", "JBS-VAL-01", "JBS-VAL-02");

    private static List<RuleSet<?, ?>> conjuntos() {
        List<RuleSet<?, ?>> l = new ArrayList<>(DERuleSets.STANDARD);
        l.addAll(EventRuleSets.STANDARD);
        l.addAll(ConfigRuleSets.STANDARD);
        l.addAll(ConfigRuleSets.DE_CONSISTENCY);
        return l;
    }

    @Test
    public void elCatalogoSeArmaDeLosConjuntosSinRepetidos() {
        List<ValidationRule> todas = RuleCatalog.all();
        Assertions.assertEquals(699, todas.size(), "649 del catálogo de §3.1 + JBS-VAL-00..02 + 14 JBS-CFG + 15 JBS-CERT (§3.7) + D208g (I3-31) + 6 de formato del número de documento (E3-26) + 2 en eventos (I3-43) + 7 de la Fase 6 (D031, D031a, D032, JBS-D-62, JBS-H-12, H018, H018a) + JBS-E-194 (I6-09) + JBS-EVENTO-56 (E8-01)");
        Set<String> codigos = new HashSet<>();
        for (ValidationRule r : todas) {
            Assertions.assertTrue(codigos.add(r.getCode()), "repetido: " + r);
            Assertions.assertTrue(RuleCatalog.isWellFormedCode(r.getCode()), r.getCode());
        }
        int fichas = 0;
        for (RuleSet<?, ?> c : conjuntos()) {
            fichas += c.getDeclaredRules().size();
        }
        Assertions.assertEquals(todas.size() - MOTOR.size(), fichas);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> RuleCatalog.all().clear());
    }

    @Test
    public void cadaReglaLlevaElGrupoYLaRutaDeSuConjunto() {
        for (RuleSet<?, ?> c : conjuntos()) {
            for (ValidationRule r : c.getDeclaredRules()) {
                Assertions.assertEquals(c.getGroup(), r.getGroup(), c.getName() + ": " + r);
                if (r.getCode().startsWith("JBS-")) {
                    Assertions.assertTrue(r.getCode().startsWith("JBS-" + c.getGroup().name() + "-"), r.getCode());
                }
                String base = c.getBasePath();
                Assertions.assertTrue(base.isEmpty() || r.getPath().equals(base) || r.getPath().startsWith(base + "."),
                        r + " (" + r.getPath() + ") fuera de " + base);
                Assertions.assertEquals(c.getName(), RuleCatalog.ruleSetOf(r.getCode()));
            }
        }
        for (ValidationRule r : RuleCatalog.all()) {
            if (r.getGroup() == RuleGroup.EVENTO) {
                Assertions.assertTrue(r.getPath().startsWith("rEve"), r + " " + r.getPath());
            }
        }
    }

    /** Toda regla tiene chequeo o motivo de no evaluada, nunca los dos ni ninguno; los conjuntos no se mezclan. */
    @Test
    public void cadaReglaSeEvaluaONoConSuMotivo() {
        Set<ValidationRule> evaluadas = new HashSet<>();
        for (RuleSet<?, ?> c : conjuntos()) {
            for (ValidationRule r : c.getDeclaredRules()) {
                boolean evaluada = c.getRules().contains(r);
                Assertions.assertTrue(evaluada ^ !r.isEvaluable(), r + ": evaluada=" + evaluada + ", motivo=" + r.getNotEvaluatedReason());
                if (evaluada) {
                    Assertions.assertTrue(evaluadas.add(r), r + " la declaran dos conjuntos");
                }
            }
        }
        Map<ValidationRule, NotEvaluatedReason> noEvaluadas = RuleCatalog.notEvaluated();
        // §6.3 (D25, app-developer): JBS-VAL-03 de baja pasa a constante (dBasExe se escribe siempre)
        Assertions.assertEquals(96, noEvaluadas.size(), "I3-15: 41 derivadas, 47 calculadas, 2 constantes, 3 de tipo; §3.7: JBS-CFG-13 constante; §6.3: JBS-VAL-03 constante; §6.1: D032 derivada");
        Assertions.assertEquals(RuleCatalog.all().size() - MOTOR.size(), evaluadas.size() + noEvaluadas.size());
        Map<NotEvaluatedReason, Integer> porMotivo = new HashMap<>();
        noEvaluadas.values().forEach(m -> porMotivo.merge(m, 1, Integer::sum));
        Assertions.assertEquals(Map.of(NotEvaluatedReason.DERIVED, 42, NotEvaluatedReason.COMPUTED, 47, NotEvaluatedReason.CONSTANT, 4, NotEvaluatedReason.TYPE, 3), porMotivo);
    }

    @Test
    public void cadaReglaTieneMensajeYNoHayMensajesHuerfanos() {
        Set<String> codigos = new TreeSet<>();
        for (ValidationRule regla : RuleCatalog.all()) {
            codigos.add(regla.getCode());
            Assertions.assertTrue(ValidationMessages.hasMessage(regla.getCode()), "sin mensaje: " + regla);
            Assertions.assertFalse(regla.message().isBlank(), "mensaje vacío: " + regla);
        }
        Set<String> deReglas = new TreeSet<>();
        for (String clave : ValidationMessages.keys()) {
            if (!ValidationMessages.isAuxiliary(clave)) {
                deReglas.add(clave);
            }
        }
        Assertions.assertEquals(codigos, deReglas, "claves de regla del recurso = códigos del catálogo");
    }

    @Test
    public void losTextosAuxiliaresEstanEnElRecurso() {
        Set<String> auxiliaresDelRecurso = new TreeSet<>();
        for (String clave : ValidationMessages.keys()) {
            if (ValidationMessages.isAuxiliary(clave)) {
                auxiliaresDelRecurso.add(clave);
            }
        }
        // M3-11: ni faltan ni sobran claves "aux.".
        Assertions.assertEquals(new TreeSet<>(ValidationMessages.CLAVES_AUXILIARES), auxiliaresDelRecurso);
        Assertions.assertEquals("largo 7", ValidationMessages.text("aux.valor.largo", 7));
        Assertions.assertEquals("aux.no.existe 3", ValidationMessages.text("aux.no.existe", 3), "sin lanzar");
    }

    @Test
    public void busquedaPorCodigoYFormaDeLosCodigos() {
        Assertions.assertSame(ValidationRule.JBS_VAL_01, RuleCatalog.byCode("JBS-VAL-01").orElseThrow());
        Assertions.assertTrue(RuleCatalog.byCode("NO-EXISTE").isEmpty());
        Assertions.assertTrue(RuleCatalog.byCode(null).isEmpty());
        for (String ok : List.of("D208", "D208c", "F023b", "GDE004a", "GEC00", "JBS-F-01", "JBS-EVENTO-01", "GENFE011",
                "JBS-E-100", "EA009a")) {
            Assertions.assertTrue(RuleCatalog.isWellFormedCode(ok), ok);
        }
        for (String mal : List.of("d208", "D2", "D208cc", "JBS-F-1", "JBS-EVENTOS-01", "JBS_F_01", "", "JBS-E-1000")) {
            Assertions.assertFalse(RuleCatalog.isWellFormedCode(mal), mal);
        }
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ValidationRule("TST009", RuleGroup.D, " ",
                Severity.ERROR, null, null));
    }

    /** La única lista a mano: toda clase del paquete {@code reglas} que extiende RuleSet está registrada. */
    @Test
    public void todoConjuntoDelPaqueteEstaRegistrado() throws IOException, ClassNotFoundException {
        Path raiz = Paths.get("src/main/java/org/javabeanstack/sifen/core/validation/rules");
        Set<Class<?>> registrados = new HashSet<>();
        conjuntos().forEach(c -> registrados.add(c.getClass()));
        List<String> faltan = new ArrayList<>();
        try (Stream<Path> archivos = Files.walk(raiz)) {
            for (Path p : (Iterable<Path>) archivos.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String rel = raiz.relativize(p).toString().replace('/', '.').replace(".java", "");
                Class<?> c = Class.forName("org.javabeanstack.sifen.core.validation.rules." + rel);
                if (RuleSet.class.isAssignableFrom(c) && !registrados.contains(c)) {
                    faltan.add(c.getName());
                }
            }
        }
        Assertions.assertTrue(faltan.isEmpty(), "conjuntos sin registrar en DERuleSets/EventRuleSets/ConfigRuleSets: " + faltan);
        Assertions.assertEquals(DERuleSets.STANDARD.size(), DEValidator.standard().getRuleSets().size());
        Assertions.assertEquals(EventRuleSets.STANDARD.size(), EventValidator.standard().getRuleSets().size());
    }

    /** Exporta el TSV del catálogo (lo lee {@code sifen_catalogo_reglas.py --generar-md / --verificar}). */
    @Test
    public void exportaElTsvDelCatalogo() throws IOException {
        String tsv = RuleCatalog.exportTsv();
        String[] lineas = tsv.split("\n");
        Assertions.assertEquals(RuleCatalog.all().size() + 1, lineas.length);
        Assertions.assertTrue(tsv.contains("\nD208c\tD\tD-gDatRec\tgDatGralOpe.gDatRec.iTipIDRec\tR\t1321\t—\tNT 021, NT 024\t"), tsv.substring(0, 300));
        Path destino = Paths.get("target/validacion/catalogo.tsv");
        Files.createDirectories(destino.getParent());
        Files.write(destino, tsv.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void fuentesYVigencia() {
        ValidationRule d105 = RuleCatalog.byCode("D105").orElseThrow();
        Assertions.assertEquals(List.of(RuleSource.MT_V150), d105.getSources());
        Assertions.assertTrue(d105.isInForceOn(LocalDate.of(2000, 1, 1)));
        ValidationRule e70 = RuleCatalog.byCode("JBS-E-70").orElseThrow();
        Assertions.assertEquals(RuleSource.GUIA_PRUEBAS_2026, e70.getSource());
        Assertions.assertEquals(List.of(RuleSource.NT_013, RuleSource.JBS), RuleCatalog.byCode("JBS-VAL-03").orElseThrow().getSources());
        EffectivePeriod v = new EffectivePeriod(RuleSource.NT_013, LocalDate.of(2027, 3, 1), LocalDate.of(2028, 1, 1), "baja");
        Assertions.assertFalse(v.isInForceOn(LocalDate.of(2027, 2, 28)));
        Assertions.assertTrue(v.isInForceOn(LocalDate.of(2027, 3, 1)));
        Assertions.assertFalse(v.isInForceOn(LocalDate.of(2028, 1, 1)));
        // §6.3 (D25), §6.2 (NT 026) y E8-01 (app-developer): JBS-VAL-03, JBS-E-64 y JBS-EVENTO-43 están de baja
        Assertions.assertEquals(RuleCatalog.all().size() - 3, RuleCatalog.inForceOn(LocalDate.of(2026, 11, 24)).size());
        Assertions.assertFalse(RuleCatalog.byCode("JBS-EVENTO-43").orElseThrow().isInForceOn(LocalDate.of(2026, 11, 24)));
        Assertions.assertFalse(RuleCatalog.byCode("JBS-E-64").orElseThrow().isInForceOn(LocalDate.of(2025, 6, 16)));
        Assertions.assertFalse(RuleCatalog.byCode("JBS-VAL-03").orElseThrow().isInForceOn(LocalDate.of(2026, 11, 24)));
    }

    // ------------------------------------------------------------------ desactivar y fecha de reglas

    /** Conjunto ficticio con una regla vigente desde 2027 y otra dada de baja en 2026. */
    static final class ConjuntoConVigencia extends RuleSet<DocumentoElectronico, DocumentoElectronico> {
        ConjuntoConVigencia() {
            super("TST-vigencia", RuleGroup.A, "");
            declare(rule("TST101").field("Id").error().source(RuleSource.NT_027).from(LocalDate.of(2027, 1, 1)),
                    onNull(s -> false, s -> null));
            declare(rule("TST102").field("Id").error().until(LocalDate.of(2026, 1, 1), "derogada"),
                    onNull(s -> false, s -> null));
        }

        @Override
        protected List<Subject<DocumentoElectronico, DocumentoElectronico>> subjects(DocumentoElectronico raiz) {
            return unique(raiz);
        }
    }

    private static Set<String> codigos(ValidationResult r) {
        Set<String> s = new TreeSet<>();
        r.getAll().forEach(e -> s.add(e.getCode()));
        return s;
    }

    @Test
    public void unaReglaRigeSoloDentroDeSuVigencia() {
        DEValidator v = DEValidator.builder().register(new ConjuntoConVigencia()).build();
        GenerationCtx ctx = new GenerationCtx();
        ctx.setSifenConfig(new SifenConfig());
        DocumentoElectronico de = new DocumentoElectronico();
        Assertions.assertEquals(Set.of("TST102"), codigos(DEValidator.builder().register(new ConjuntoConVigencia())
                .rulesDate(LocalDate.of(2025, 6, 1)).build().validate(de, ctx)));
        Assertions.assertEquals(Set.of(), codigos(DEValidator.builder().register(new ConjuntoConVigencia())
                .rulesDate(LocalDate.of(2026, 6, 1)).build().validate(de, ctx)));
        Assertions.assertEquals(Set.of("TST101"), codigos(DEValidator.builder().register(new ConjuntoConVigencia())
                .rulesDate(LocalDate.of(2027, 1, 1)).build().validate(de, ctx)));
        ctx.getSifenConfig().setRulesDate(LocalDate.of(2027, 5, 1));
        Assertions.assertEquals(Set.of("TST101"), codigos(v.validate(de, ctx)), "la fecha de la configuración adelanta la NT");
    }

    @Test
    public void unaReglaSeDesactivaPorBuilderYPorConfiguracion() {
        GenerationCtx ctx = new GenerationCtx();
        ctx.setSifenConfig(new SifenConfig());
        DocumentoElectronico de = new DocumentoElectronico();
        Assertions.assertTrue(DEValidator.standard().validate(de, ctx).contains("JBS-B-01"));
        Assertions.assertFalse(DEValidator.builder().registerStandard().disable("JBS-B-01").build().validate(de, ctx)
                .contains("JBS-B-01"));
        ctx.getSifenConfig().setDisabledRules(Set.of("JBS-B-01", "JBS-C-01"));
        ValidationResult r = DEValidator.standard().validate(de, ctx);
        Assertions.assertFalse(r.contains("JBS-B-01"));
        Assertions.assertFalse(r.contains("JBS-C-01"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> DEValidator.builder().registerStandard().disable("JBS-VAL-00").build());
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> DEValidator.builder().registerStandard().disable("NO-EXISTE").strict().build());
        Assertions.assertDoesNotThrow(() -> DEValidator.builder().registerStandard().disable("NO-EXISTE").build());
        Assertions.assertFalse(EventValidator.builder().registerStandard().disable("JBS-EVENTO-01").build()
                .validate(new TrGesEve(), ctx).contains("JBS-EVENTO-01"));
    }

    @Test
    public void lasPropiedadesDeDesactivacionYFechaSeLeenDelArchivo() throws Exception {
        java.util.Properties p = new java.util.Properties();
        p.setProperty("sifen.ambiente", "DEV");
        p.setProperty("sifen.certificado_cliente.tipo", "PFX");
        p.setProperty("sifen.certificado_cliente.archivo", FixturesSifen.rutaPfx().toString());
        p.setProperty("sifen.certificado_cliente.contrasena", FixturesSifen.PFX_CLAVE);
        p.setProperty("sifen.validacion_reglas_desactivadas", " D105 , JBS-E-70,,");
        p.setProperty("sifen.validacion_fecha_reglas", "2027-03-01");
        SifenConfig c = SifenConfig.load(p);
        Assertions.assertEquals(Set.of("D105", "JBS-E-70"), c.getDisabledRules());
        Assertions.assertEquals(LocalDate.of(2027, 3, 1), c.getRulesDate());
        p.setProperty("sifen.validacion_fecha_reglas", "marzo");
        Assertions.assertThrows(org.javabeanstack.sifen.core.exceptions.SifenException.class, () -> SifenConfig.load(p));
    }

    /** Una regla que lanza da JBS-VAL-00 con conjunto/código y no corta las demás del conjunto. */
    @Test
    public void unChequeoQueLanzaNoCortaElConjunto() {
        RuleSet<DocumentoElectronico, DocumentoElectronico> roto = new RuleSet<>("TST-roto", RuleGroup.A, "") {
            {
                declare(rule("TST201").field("Id").error(), onNull(s -> {
                    throw new IllegalStateException("dato sensible");
                }, s -> null));
                declare(rule("TST202").field("Id").error(), onNull(s -> false, s -> null));
            }

            @Override
            protected List<Subject<DocumentoElectronico, DocumentoElectronico>> subjects(DocumentoElectronico raiz) {
                return unique(raiz);
            }
        };
        ValidationResult r = DEValidator.builder().register(roto).build().validate(new DocumentoElectronico(), null);
        Assertions.assertTrue(r.contains("TST202"), r::toString);
        ValidationError val00 = r.find("JBS-VAL-00").get(0);
        Assertions.assertTrue(val00.getMessage().contains("TST-roto/TST201"), val00.getMessage());
        Assertions.assertFalse(r.toString().contains("dato sensible"));
    }
}
