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

import org.javabeanstack.sifen.Sifen;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.types.TiNatRec;
import org.javabeanstack.sifen.core.types.TiTiOpe;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.javabeanstack.sifen.test.casos.XmlCanonico;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * E3-07 e I3-31 (plan JBSSIF, §3.8): un receptor no contribuyente en B2F sin tipo de documento.
 *
 * <p>La tabla de campos de la NT 023 exime a B2F de D208/D210, pero su validación D208g (1335) exige el
 * tipo de documento a todo no contribuyente. Decisión del usuario (2026-11-24, I3-31): D208g es
 * ERROR, así que <b>con la validación previa se rechaza localmente</b>; el número (D210) sigue
 * opcional en B2F. <b>Sin la validación</b>, la guarda de {@code TgDatRec} (E3-07) evita la
 * {@code NullPointerException} de la línea base y omite {@code iTipIDRec}/{@code dDTipIDRec}.</p>
 *
 * <p>Base: DE-17 con sus desvíos corregidos ({@link SoporteValidacion#CORRECCIONES}: B2F). El caso
 * DE-24 del catálogo (ahora negativo de D208g) lo integra test-developer.</p>
 */
public class ReceptorB2fSinDocumentoE307Test {

    private static DocumentoElectronico b2fSinDocumento() {
        SoporteValidacion.Fuente fuente = SoporteValidacion.fuentes().stream()
                .filter(f -> f.caso().getId().equals("DE-17"))
                .findFirst().orElseThrow();
        DocumentoElectronico de = fuente.crearCorregido();
        Assertions.assertEquals(TiNatRec.NO_CONTRIBUYENTE, de.getgDatGralOpe().getgDatRec().getiNatRec());
        Assertions.assertEquals(TiTiOpe.B2F, de.getgDatGralOpe().getgDatRec().getiTiOpe());
        de.getgDatGralOpe().getgDatRec().setiTipIDRec(null);
        de.getgDatGralOpe().getgDatRec().setdDTipIDRec(null);
        de.getgDatGralOpe().getgDatRec().setdNumIDRec(null);
        return de;
    }

    @Test
    public void conValidacionSeRechazaConD208gYSinExigirElNumero() {
        SifenConfig cfg = FixturesSifen.configuracionConValidacion();
        DocumentoElectronico de = b2fSinDocumento();
        ValidationResult r = DEValidator.standard().validate(de, FixturesSifen.contexto(cfg));
        Assertions.assertEquals(1, r.getErrors().size(), r.toString());
        ValidationError e = r.find("D208g").get(0);
        Assertions.assertEquals(Severity.ERROR, e.getSeverity());
        Assertions.assertEquals("gDatGralOpe.gDatRec.iTipIDRec", e.getPath());
        Assertions.assertEquals("1335", e.getSifenResponseCode());
        Assertions.assertFalse(r.contains("D210"), "D210: el número sigue opcional en B2F");
        SifenValidationException x = Assertions.assertThrows(SifenValidationException.class,
                () -> de.generarXml(FixturesSifen.contexto(cfg), cfg));
        Assertions.assertEquals("D208g", x.getCode());
    }

    @Test
    public void sinValidacionLaGuardaEvitaLaNpe() throws Exception {
        SifenConfig cfg = FixturesSifen.configuracion();
        DocumentoElectronico de = b2fSinDocumento();
        String xml = de.generarXml(FixturesSifen.contexto(cfg), cfg);
        Assertions.assertNull(XmlCanonico.texto(xml, "iTipIDRec"), "sin iTipIDRec");
        Assertions.assertNull(XmlCanonico.texto(xml, "dDTipIDRec"), "sin dDTipIDRec");
        Assertions.assertNull(XmlCanonico.texto(xml, "dNumIDRec"), "sin el «0» del innominado");
        Assertions.assertEquals("4", XmlCanonico.texto(xml, "iTiOpe"));
        Assertions.assertTrue(Sifen.validarFirmaDEDesdeXml(xml).isValido());

        DocumentoElectronico conNumero = b2fSinDocumento();
        conNumero.getgDatGralOpe().getgDatRec().setdNumIDRec("AB123");
        Assertions.assertEquals("AB123", XmlCanonico.texto(conNumero.generarXml(FixturesSifen.contexto(cfg), cfg), "dNumIDRec"));
    }
}
