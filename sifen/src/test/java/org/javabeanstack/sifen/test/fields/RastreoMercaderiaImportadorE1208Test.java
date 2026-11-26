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
package org.javabeanstack.sifen.test.fields;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.fields.request.de.TgRasMerc;
import org.javabeanstack.sifen.core.validation.DEValidator;
import org.javabeanstack.sifen.core.validation.RuleCatalog;
import org.javabeanstack.sifen.core.validation.ValidationError;
import org.javabeanstack.sifen.core.validation.ValidationResult;
import org.javabeanstack.sifen.core.validation.ValidationRule;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.test.casos.CasosFase12;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.javabeanstack.sifen.test.casos.XmlCanonico;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * E12-08 (plan JBSSIF, Fase 12; decisión del usuario del 2026-11-25, ESC-44, opción A1): el grupo {@code gRasMerc}
 * sale como el {@code tgRasMerc} del XSD vigente de la DNIT ({@code DE_v150.xsd}, descargado el 2026-11-25), sin los
 * campos del importador ({@code dNomImp} E756, {@code dDirImp} E757, {@code dNumFir} E758) que la NT 010 (04/02/2022,
 * apartado 1.4) eliminó. Hasta la corrección la librería los escribía después de {@code dNumSegui} y SIFEN-test
 * rechazaba con 0160 el DE que los traía (DE-68, 0001117: «El elemento esperado es: dNumReg en lugar de: dNomImp»).
 *
 * <p>Los seis accesores quedan {@code @Deprecated} y sin efecto en el XML: guardan y devuelven el valor, se siguen
 * leyendo de un XML viejo, y sus reglas (JBS-E-99, JBS-E-100 y JBS-E-101) están de baja, así que un valor cargado no
 * frena la validación previa. {@code dNomPro} (E761) no se agrega acá: va a la Fase 7 (I6-04).</p>
 *
 * <p>Ningún test compara el XML entero con {@code assertEquals}: un fallo no debe volcar el XML firmado en el log.</p>
 */
@SuppressWarnings("deprecation")
public class RastreoMercaderiaImportadorE1208Test {
    /** Secuencia de {@code tgRasMerc} del XSD vigente ({@code DE_v150.xsd:430-498}). */
    static final List<String> SECUENCIA_VIGENTE = List.of("dNumLote", "dVencMerc", "dNSerie", "dNumPedi", "dNumSegui",
            "dNumReg", "dNumRegEntCom", "dNomPro");
    /** Lo que escribe la librería con todos los campos vigentes cargados (sin {@code dNomPro}, Fase 7). */
    static final List<String> ESCRITOS = SECUENCIA_VIGENTE.subList(0, 7);
    /** Campos eliminados por la NT 010. */
    static final List<String> IMPORTADOR = List.of("dNomImp", "dDirImp", "dNumFir");
    /** Reglas de largo de los tres campos, de baja. */
    static final List<String> REGLAS_DE_BAJA = List.of("JBS-E-99", "JBS-E-100", "JBS-E-101");

    private static SifenConfig cfg;
    private static GenerationCtx ctx;

    @BeforeAll
    public static void configurar() {
        cfg = FixturesSifen.configuracion();
        ctx = FixturesSifen.contexto(cfg);
    }

    /** DE-68 con el importador cargado: el grupo sale en el orden vigente, sin ninguno de los tres campos. */
    @Test
    public void conImportadorCargadoElGrupoSaleSinElImportadorYEnElOrdenVigente() throws Exception {
        DocumentoElectronico de = conImportador("IMPORTADORA FICTICIA S.A.", "CALLE FICTICIA 456", "FIRMA000000000000001");
        String xml = de.generarXml(ctx, cfg);
        List<String> hijos = hijosDeGRasMerc(xml);
        Assertions.assertEquals(ESCRITOS, hijos, "E12-08: gRasMerc = tgRasMerc vigente, sin el importador");
        for (String campo : IMPORTADOR) {
            Assertions.assertEquals(0, XmlCanonico.parsear(xml).getElementsByTagNameNS("*", campo).getLength(),
                    campo + " no se escribe (NT 010)");
        }
        Assertions.assertTrue(esSubsecuencia(hijos, SECUENCIA_VIGENTE), "orden del XSD vigente: " + hijos);
    }

    /** Lo deprecado sigue funcionando en la línea 2.x: los setters guardan y los getters devuelven el valor. */
    @Test
    public void losAccesoresDeprecadosGuardanYDevuelvenElValor() {
        TgRasMerc r = new TgRasMerc();
        r.setdNomImp("IMPORTADORA FICTICIA S.A.");
        r.setdDirImp("CALLE FICTICIA 456");
        r.setdNumFir("FIRMA000000000000001");
        Assertions.assertEquals("IMPORTADORA FICTICIA S.A.", r.getdNomImp());
        Assertions.assertEquals("CALLE FICTICIA 456", r.getdDirImp());
        Assertions.assertEquals("FIRMA000000000000001", r.getdNumFir());
    }

    /**
     * La entrada de DE-68 ({@code casos/fase12/f12-rastreo-mercaderia.xml}, generada antes de la corrección) trae el
     * importador: se lee (los getters tienen el valor) y, regenerada, sale sin él (un XML viejo se sigue leyendo).
     */
    @Test
    public void unXmlViejoConImportadorSeLeeYSeRegeneraSinEl() throws Exception {
        String entrada = FixturesSifen.recurso("casos/fase12/f12-rastreo-mercaderia.xml");
        Assertions.assertTrue(entrada.contains("<dNomImp>"), "precondición: la entrada conserva el importador");
        DocumentoElectronico leido = new DocumentoElectronico(entrada);
        TgRasMerc r = leido.getgDtipDE().getgCamItemList().get(0).getgRasMerc();
        Assertions.assertEquals("IMPORTADORA FICTICIA S.A.", r.getdNomImp());
        Assertions.assertEquals("CALLE FICTICIA 456", r.getdDirImp());
        Assertions.assertEquals("FIRMA000000000000001", r.getdNumFir());
        Assertions.assertEquals(ESCRITOS, hijosDeGRasMerc(leido.generarXml(ctx, cfg)));
    }

    /**
     * Con el importador cargado y fuera de los largos que pedían JBS-E-99..101 (4-60, 1-255 y 20), la validación previa
     * no lo rechaza: ni con la fecha de hoy ni con una fecha de reglas anterior a la baja (el campo no viaja).
     */
    @Test
    public void conImportadorCargadoLaValidacionNoLoRechaza() {
        DocumentoElectronico de = conImportador("AB", "X".repeat(256), "CORTO");
        afirmarSinRechazoDelImportador(DEValidator.standard().validate(de, ctx), "hoy");
        afirmarSinRechazoDelImportador(DEValidator.builder().registerStandard().rulesDate(LocalDate.of(2021, 1, 1)).build()
                .validate(de, ctx), "fecha de reglas 2021-01-01");
    }

    /** Las tres reglas están de baja (D33): siguen en el catálogo con su código, no rigen y tienen el motivo. */
    @Test
    public void lasReglasDelImportadorEstanDeBaja() {
        for (String codigo : REGLAS_DE_BAJA) {
            ValidationRule regla = RuleCatalog.byCode(codigo).orElseThrow();
            Assertions.assertEquals(LocalDate.of(2022, 2, 4), regla.getEffectivePeriod().until(), codigo);
            Assertions.assertFalse(regla.isInForceOn(LocalDate.of(2026, 11, 25)), codigo + " de baja");
            Assertions.assertTrue(regla.isInForceOn(LocalDate.of(2022, 2, 3)), codigo + " regía antes de la NT 010");
        }
        Assertions.assertTrue(RuleCatalog.byCode("JBS-E-102").orElseThrow().isInForceOn(LocalDate.of(2026, 11, 25)), "dNumReg sigue");
        Assertions.assertTrue(RuleCatalog.byCode("JBS-E-103").orElseThrow().isInForceOn(LocalDate.of(2026, 11, 25)), "dNumRegEntCom sigue");
    }

    // ------------------------------------------------------------------ apoyo

    private static DocumentoElectronico conImportador(String nombre, String direccion, String firma) {
        DocumentoElectronico de = CasosFase12.todos().get("DE-68").get();
        TgRasMerc r = de.getgDtipDE().getgCamItemList().get(0).getgRasMerc();
        r.setdNomImp(nombre);
        r.setdDirImp(direccion);
        r.setdNumFir(firma);
        return de;
    }

    private static void afirmarSinRechazoDelImportador(ValidationResult r, String cuando) {
        List<String> malos = new ArrayList<>();
        for (ValidationError e : r.getAll()) {
            if (REGLAS_DE_BAJA.contains(e.getCode()) || IMPORTADOR.stream().anyMatch(c -> String.valueOf(e.getPath()).endsWith("." + c))) {
                malos.add(e.getCode() + " " + e.getPath());
            }
        }
        Assertions.assertTrue(malos.isEmpty(), () -> cuando + ": " + malos);
        Assertions.assertTrue(r.getErrors().stream().noneMatch(e -> String.valueOf(e.getPath()).contains("gRasMerc")),
                () -> cuando + ": error en gRasMerc " + r.getErrors());
    }

    private static List<String> hijosDeGRasMerc(String xml) throws Exception {
        Node g = XmlCanonico.parsear(xml).getElementsByTagNameNS("*", "gRasMerc").item(0);
        Assertions.assertNotNull(g, "el XML tiene gRasMerc");
        List<String> l = new ArrayList<>();
        for (Node n = g.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e) {
                l.add(e.getLocalName());
            }
        }
        return l;
    }

    private static boolean esSubsecuencia(List<String> l, List<String> secuencia) {
        int i = 0;
        for (String s : secuencia) {
            if (i < l.size() && l.get(i).equals(s)) {
                i++;
            }
        }
        return i == l.size();
    }
}
