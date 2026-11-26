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

import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.fields.request.de.TgCamTrans;
import org.javabeanstack.sifen.core.fields.request.de.TgEmis;
import org.javabeanstack.sifen.core.types.TTiDE;
import org.junit.jupiter.api.Assertions;

/**
 * Ajustes y comprobaciones que un DE de la batería de la Fase 12 necesita antes de firmarse para §12.3 (plan
 * JBSSIF, I12-08). Los usa {@link BateriaFacturacionSimulatorTest} y los va a usar {@code SifenVivoFase12Test}
 * después de poner el emisor de prueba del archivo de entorno (D58).
 *
 * <p><b>Transportista (I12-08, I8-09, D58).</b> SIFEN-test rechazó con 2303 («RUC del transportista inexistente»)
 * la remisión 0001053 de §8.7 por el transportista del catálogo, y aprobó la 0001054 con el transportista igual al
 * emisor de prueba. En los fixtures el transportista ya es el emisor ficticio; en vivo el emisor cambia y el
 * transportista tiene que cambiar con él. Por eso la comprobación es relacional (transportista = emisor del mismo
 * DE) y no compara contra ningún RUC escrito acá: el RUC del emisor de prueba entra solo en tiempo de
 * ejecución.</p>
 */
public final class PreparacionVivoFase12 {
    private PreparacionVivoFase12() {
    }

    /**
     * Pone como transportista el emisor del propio DE ({@code dRucTrans}/{@code dDVTrans} = {@code dRucEm}/{@code dDVEmi}).
     * Se llama después de cargar el emisor de prueba; si el DE no tiene datos del transportista, no hace nada.
     *
     * @param de documento ya con su emisor definitivo.
     * @return el mismo documento.
     */
    public static DocumentoElectronico transportistaDePrueba(DocumentoElectronico de) {
        TgCamTrans t = transportista(de);
        if (t != null) {
            TgEmis em = de.getgDatGralOpe().getgEmis();
            t.setdRucTrans(em.getdRucEm());
            t.setdDVTrans(Short.parseShort(em.getdDVEmi()));
        }
        return de;
    }

    /**
     * Afirma, antes de firmar, que el transportista es el emisor del propio DE (I12-08). Una NR sin datos del
     * transportista también falla: las cuatro remisiones de §12.3.6 los llevan.
     *
     * @param de documento listo para firmar.
     */
    public static void afirmarTransportistaDePrueba(DocumentoElectronico de) {
        TgCamTrans t = transportista(de);
        boolean remision = de.getgTimb().getiTiDE() == TTiDE.NOTA_DE_REMISION_ELECTRONICA;
        if (remision) {
            Assertions.assertNotNull(t, "I12-08: la NR lleva los datos del transportista (gCamTrans)");
        }
        if (t == null) {
            return;
        }
        TgEmis em = de.getgDatGralOpe().getgEmis();
        Assertions.assertEquals(em.getdRucEm(), t.getdRucTrans(),
                "I12-08: transportista = RUC del emisor de prueba (I8-09, D58); otro RUC dio 2303 en §8.7");
        Assertions.assertEquals(em.getdDVEmi(), String.valueOf(t.getdDVTrans()), "I12-08: DV del transportista = DV del emisor");
    }

    private static TgCamTrans transportista(DocumentoElectronico de) {
        if (de.getgDtipDE() == null || de.getgDtipDE().getgTransp() == null) {
            return null;
        }
        return de.getgDtipDE().getgTransp().getgCamTrans();
    }
}
