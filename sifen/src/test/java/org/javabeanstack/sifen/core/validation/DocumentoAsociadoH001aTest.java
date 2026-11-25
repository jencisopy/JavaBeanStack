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

import java.util.stream.Stream;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.fields.request.de.TgCamDEAsoc;
import org.javabeanstack.sifen.core.types.TTiDE;
import org.javabeanstack.sifen.core.types.TiTIpoDoc;
import org.javabeanstack.sifen.core.types.TiTipDocAso;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Matriz de H001a (tipo de documento admitido como asociado, MT v150 H001a / dCodRes 2414): NC, ND y
 * NR se asocian a una factura (CDC con prefijo {@code 01} o impreso de tipo 1); una factura, a una
 * factura ({@code 01}) o a una nota de remisión ({@code 07} / impreso 4). Cubre las ramas que la
 * corrida de negativos no recorre (plan JBSSIF, §3.4; cobertura del ciclo de corrección 2).
 */
public class DocumentoAsociadoH001aTest {
    private static final DEValidator V = DEValidator.standard();

    static Stream<Arguments> matriz() {
        return Stream.of(
                // caso base, tipo de DE, tipo de asociado, prefijo del CDC o tipo de impreso, ¿H001a?
                Arguments.of("DE-21", TTiDE.NOTA_DE_CREDITO_ELECTRONICA, 1, "01", false),
                Arguments.of("DE-21", TTiDE.NOTA_DE_CREDITO_ELECTRONICA, 1, "07", true),
                Arguments.of("DE-21", TTiDE.NOTA_DE_DEBITO_ELECTRONICA, 1, "01", false),
                Arguments.of("DE-21", TTiDE.NOTA_DE_DEBITO_ELECTRONICA, 1, "05", true),
                Arguments.of("DE-21", TTiDE.NOTA_DE_REMISION_ELECTRONICA, 1, "01", false),
                Arguments.of("DE-21", TTiDE.NOTA_DE_REMISION_ELECTRONICA, 1, "04", true),
                Arguments.of("DE-21", TTiDE.FACTURA_ELECTRONICA, 1, "01", false),
                Arguments.of("DE-21", TTiDE.FACTURA_ELECTRONICA, 1, "07", false),
                Arguments.of("DE-21", TTiDE.FACTURA_ELECTRONICA, 1, "05", true),
                Arguments.of("DE-21", TTiDE.AUTOFACTURA_ELECTRONICA, 1, "05", false),
                Arguments.of("DE-22", TTiDE.NOTA_DE_DEBITO_ELECTRONICA, 2, "1", false),
                Arguments.of("DE-22", TTiDE.NOTA_DE_DEBITO_ELECTRONICA, 2, "4", true),
                Arguments.of("DE-22", TTiDE.NOTA_DE_CREDITO_ELECTRONICA, 2, "2", true),
                Arguments.of("DE-22", TTiDE.NOTA_DE_REMISION_ELECTRONICA, 2, "1", false),
                Arguments.of("DE-22", TTiDE.FACTURA_ELECTRONICA, 2, "1", false),
                Arguments.of("DE-22", TTiDE.FACTURA_ELECTRONICA, 2, "4", false),
                Arguments.of("DE-22", TTiDE.FACTURA_ELECTRONICA, 2, "3", true),
                Arguments.of("DE-22", TTiDE.AUTOFACTURA_ELECTRONICA, 2, "3", false));
    }

    @ParameterizedTest(name = "{0} como {1}, asociado {2}/{3} ⇒ H001a {4}")
    @MethodSource("matriz")
    public void elTipoDeDocumentoAsociadoSeAdmiteSegunElMt(String caso, TTiDE tipo, int asociado, String valor, boolean error) {
        DocumentoElectronico de = CasosDePrueba.documentos().stream().filter(c -> c.getId().equals(caso))
                .findFirst().orElseThrow().construir();
        de.getgTimb().setiTiDE(tipo);
        TgCamDEAsoc a = de.getgCamDEAsocList().get(0);
        Assertions.assertEquals(asociado, a.getiTipDocAso().getVal(), "el caso base trae el tipo de asociado esperado");
        if (asociado == 1) {
            a.setdCdCDERef(valor + a.getdCdCDERef().substring(2));
        } else {
            a.setiTipoDocAso(TiTIpoDoc.getByVal(Short.parseShort(valor)));
        }
        GenerationCtx ctx = FixturesSifen.contexto(FixturesSifen.configuracion());
        ValidationResult r = V.validate(de, ctx);
        Assertions.assertEquals(error, r.contains("H001a"), r::toString);
    }

    @ParameterizedTest(name = "{0} sin dato que mirar ⇒ sin H001a")
    @MethodSource("sinDato")
    public void sinCdcOSinTipoDeImpresoNoSeJuzgaElTipo(String caso) {
        DocumentoElectronico de = CasosDePrueba.documentos().stream().filter(c -> c.getId().equals(caso))
                .findFirst().orElseThrow().construir();
        TgCamDEAsoc a = de.getgCamDEAsocList().get(0);
        if (a.getiTipDocAso() == TiTipDocAso.ELECTRONICO) {
            a.setdCdCDERef("0");
        } else {
            a.setiTipoDocAso(null);
        }
        ValidationResult r = V.validate(de, FixturesSifen.contexto(FixturesSifen.configuracion()));
        Assertions.assertFalse(r.contains("H001a"), r::toString);
    }

    static Stream<String> sinDato() {
        return Stream.of("DE-21", "DE-22");
    }
}
