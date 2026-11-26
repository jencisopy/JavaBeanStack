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

import java.util.Set;
import java.util.TreeSet;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.types.TTiDE;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.javabeanstack.sifen.test.casos.XmlCanonico;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

/**
 * {@link PreparacionVivoFase12}: el transportista de las remisiones de §12.3.6 (plan JBSSIF, I12-08).
 */
public class PreparacionVivoFase12Test {
    /** RUC y DV de otro emisor, ficticios (los del receptor de los fixtures): simulan el emisor de prueba del entorno. */
    private static final String OTRO_RUC = "80654321";
    private static final String OTRO_DV = "3";

    /**
     * Las remisiones que van en vivo son DE-57 a DE-60 y cumplen la condición con el emisor ficticio; ningún otro DE
     * que va en vivo lleva datos del transportista.
     */
    @Test
    public void lasRemisionesEnVivoLlevanElTransportistaDelEmisor() {
        Set<String> conTransportista = new TreeSet<>();
        for (EsperadosFase12.Esperado e : EsperadosFase12.leer()) {
            if (e.esEvento() || e.esOperacion() || !e.tanda().startsWith("12.3.")) {
                continue;
            }
            DocumentoElectronico de = e.documento();
            boolean remision = de.getgTimb().getiTiDE() == TTiDE.NOTA_DE_REMISION_ELECTRONICA;
            boolean lleva = de.getgDtipDE() != null && de.getgDtipDE().getgTransp() != null
                    && de.getgDtipDE().getgTransp().getgCamTrans() != null;
            if (remision || lleva) {
                conTransportista.add(e.caso());
                PreparacionVivoFase12.afirmarTransportistaDePrueba(de);
            }
        }
        Assertions.assertEquals(new TreeSet<>(Set.of("DE-57", "DE-58", "DE-59", "DE-60")), conTransportista);
    }

    /**
     * Con otro emisor (como en vivo, D58) el transportista del catálogo deja de coincidir y la comprobación falla antes
     * de firmar; tras {@link PreparacionVivoFase12#transportistaDePrueba} coincide y el XML lo lleva.
     */
    @Test
    public void conOtroEmisorElTransportistaSeAjustaYSeAfirma() throws Exception {
        SifenConfig cfg = FixturesSifen.configuracion();
        GenerationCtx ctx = FixturesSifen.contexto(cfg);
        for (String id : new String[] {"DE-57", "DE-58", "DE-59", "DE-60"}) {
            DocumentoElectronico de = EsperadosFase12.leer().stream().filter(e -> e.caso().equals(id)).findFirst().orElseThrow().documento();
            de.getgDatGralOpe().getgEmis().setdRucEm(OTRO_RUC);
            de.getgDatGralOpe().getgEmis().setdDVEmi(OTRO_DV);
            Assertions.assertThrows(AssertionFailedError.class, () -> PreparacionVivoFase12.afirmarTransportistaDePrueba(de), id);
            PreparacionVivoFase12.transportistaDePrueba(de);
            PreparacionVivoFase12.afirmarTransportistaDePrueba(de);
            String xml = de.generarXml(ctx, cfg);
            Assertions.assertEquals(OTRO_RUC, XmlCanonico.texto(xml, "dRucTrans"), id);
            Assertions.assertEquals(OTRO_DV, XmlCanonico.texto(xml, "dDVTrans"), id);
            Assertions.assertEquals(XmlCanonico.texto(xml, "dRucEm"), XmlCanonico.texto(xml, "dRucTrans"), id);
        }
    }

    /** Una NR sin datos del transportista no pasa la comprobación. */
    @Test
    public void unaRemisionSinTransportistaNoPasa() {
        DocumentoElectronico de = EsperadosFase12.leer().stream().filter(e -> e.caso().equals("DE-57")).findFirst().orElseThrow().documento();
        de.getgDtipDE().getgTransp().setgCamTrans(null);
        Assertions.assertThrows(AssertionFailedError.class, () -> PreparacionVivoFase12.afirmarTransportistaDePrueba(de));
    }
}
