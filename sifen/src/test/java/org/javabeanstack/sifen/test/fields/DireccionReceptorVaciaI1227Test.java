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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.validation.DEValidator;
import org.javabeanstack.sifen.core.validation.ValidationError;
import org.javabeanstack.sifen.core.validation.ValidationResult;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.test.casos.BateriaIdentificacion;
import org.javabeanstack.sifen.test.casos.CasosFase12;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.javabeanstack.sifen.test.casos.XmlCanonico;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * I12-27 (plan JBSSIF, Fase 12; decisión del usuario del 2026-11-25, ESC-47 opción (a), excepción a D54): sin
 * dirección del receptor, el armado no escribe {@code dDirRec}. Hasta la corrección, en B2F ({@code iTiOpe} 4) y en la
 * nota de remisión ({@code iTiDE} 7) escribía {@code <dDirRec>} vacío ({@code TgDatRec}), que el XSD vigente
 * ({@code tdDirec}: largo mínimo 1) no admite: SIFEN contestaría 0160 (esquema) en vez de 1318, el código de D213 que
 * busca ID-F06. El XSD hace el elemento opcional ({@code minOccurs="0"}); la exigencia es de D213, que la validación
 * previa sigue aplicando.
 *
 * <p>Con {@code -Djbs.sifen.i1227.volcar=<carpeta>} escribe los XML generados (ID-F06 y la NR sin dirección) para
 * validarlos contra el XSD vigente con {@code sifen_xsd_cotejo.py}. Ningún test compara el XML entero con
 * {@code assertEquals}: un fallo no debe volcar el XML firmado en el log.</p>
 */
public class DireccionReceptorVaciaI1227Test {
    private static final String VOLCAR = System.getProperty("jbs.sifen.i1227.volcar");
    private static SifenConfig cfg;
    private static GenerationCtx ctx;

    @BeforeAll
    public static void configurar() {
        cfg = FixturesSifen.configuracion();
        ctx = FixturesSifen.contexto(cfg);
    }

    /** ID-F06 (B2F sin dirección): el XML no lleva {@code dDirRec} ni ningún elemento vacío en {@code gDatRec}. */
    @Test
    public void b2fSinDireccionNoEscribeDDirRec() throws Exception {
        String xml = idF06().generarXml(ctx, cfg);
        volcar("ID-F06.xml", xml);
        afirmarSinDDirRecNiVacios(xml, "ID-F06");
    }

    /** ID-F06: la validación previa sigue dando D213 con el dCodRes 1318. */
    @Test
    public void b2fSinDireccionSigueDandoD213Con1318() {
        afirmarD213(DEValidator.standard().validate(idF06(), ctx), "ID-F06");
    }

    /** Nota de remisión (DE-57) sin dirección: el XML no lleva {@code dDirRec}; la validación previa da D213 · 1318. */
    @Test
    public void notaDeRemisionSinDireccionNoEscribeDDirRec() throws Exception {
        DocumentoElectronico nr = nrSinDireccion();
        Assertions.assertEquals(7, nr.getgTimb().getiTiDE().getVal(), "precondición: nota de remisión");
        String xml = nr.generarXml(ctx, cfg);
        volcar("DE-57-sin-direccion.xml", xml);
        afirmarSinDDirRecNiVacios(xml, "NR sin dirección");
        afirmarD213(DEValidator.standard().validate(nrSinDireccion(), ctx), "NR sin dirección");
    }

    /** Con dirección, B2F la sigue escribiendo (ID-F01, la base de ID-F06). */
    @Test
    public void b2fConDireccionLaSigueEscribiendo() throws Exception {
        DocumentoElectronico de = combinacion("ID-F01").construir();
        String direccion = de.getgDatGralOpe().getgDatRec().getdDirRec();
        Assertions.assertNotNull(direccion, "precondición: la base de ID-F06 tiene dirección");
        Assertions.assertEquals(direccion, XmlCanonico.texto(de.generarXml(ctx, cfg), "dDirRec"));
    }

    // ------------------------------------------------------------------ apoyo

    private static DocumentoElectronico idF06() {
        DocumentoElectronico de = combinacion("ID-F06").construir();
        Assertions.assertNull(de.getgDatGralOpe().getgDatRec().getdDirRec(), "precondición: ID-F06 sin dirección");
        Assertions.assertEquals(4, de.getgDatGralOpe().getgDatRec().getiTiOpe().getVal(), "precondición: B2F");
        return de;
    }

    private static DocumentoElectronico nrSinDireccion() {
        DocumentoElectronico de = CasosFase12.todos().get("DE-57").get();
        de.getgDatGralOpe().getgDatRec().setdDirRec(null);
        return de;
    }

    private static BateriaIdentificacion.Combinacion combinacion(String id) {
        return BateriaIdentificacion.combinaciones().stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow();
    }

    private static void afirmarSinDDirRecNiVacios(String xml, String caso) throws Exception {
        Node g = XmlCanonico.parsear(xml).getElementsByTagNameNS("*", "gDatRec").item(0);
        Assertions.assertNotNull(g, caso + ": el XML tiene gDatRec");
        List<String> vacios = new ArrayList<>();
        boolean conDireccion = false;
        for (Node n = g.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e) {
                conDireccion |= "dDirRec".equals(e.getLocalName());
                if (e.getTextContent() == null || e.getTextContent().isBlank()) {
                    vacios.add(e.getLocalName());
                }
            }
        }
        Assertions.assertFalse(conDireccion, caso + ": I12-27, sin dirección no se escribe dDirRec");
        Assertions.assertTrue(vacios.isEmpty(), caso + ": elementos vacíos en gDatRec " + vacios);
    }

    private static void afirmarD213(ValidationResult r, String caso) {
        List<ValidationError> d213 = r.find("D213");
        Assertions.assertEquals(1, d213.size(), () -> caso + ": se espera D213, hay " + r.getErrors());
        Assertions.assertEquals("1318", d213.get(0).getSifenResponseCode(), caso);
        Assertions.assertFalse(r.isValid(), caso + ": D213 es un error");
    }

    private static void volcar(String archivo, String xml) throws Exception {
        if (VOLCAR != null && !VOLCAR.isBlank()) {
            Path destino = Paths.get(VOLCAR);
            Files.createDirectories(destino);
            Files.write(destino.resolve(archivo), xml.getBytes(StandardCharsets.UTF_8));
        }
    }
}
