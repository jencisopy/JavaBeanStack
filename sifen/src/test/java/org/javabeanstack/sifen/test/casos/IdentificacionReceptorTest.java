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
package org.javabeanstack.sifen.test.casos;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.validation.DEValidator;
import org.javabeanstack.sifen.core.validation.ValidationError;
import org.javabeanstack.sifen.core.validation.ValidationResult;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Matriz de identificación del receptor (grupo D2xx; plan JBSSIF §2.1.5): por cada combinación de
 * {@link BateriaIdentificacion}, la validación previa da exactamente los rechazos que espera el MT/NT
 * (vacío = acepta) y, si acepta, la librería genera el XML.
 */
public class IdentificacionReceptorTest {
    static final ZoneId ZONA = ZoneId.of("America/Asuncion");

    static Stream<BateriaIdentificacion.Combinacion> combinaciones() {
        return BateriaIdentificacion.combinaciones().stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("combinaciones")
    public void laValidacionDaLoQueEsperaElMt(BateriaIdentificacion.Combinacion c) throws Exception {
        SifenConfig cfg = FixturesSifen.configuracion();
        GenerationCtx ctx = FixturesSifen.contexto(cfg);
        DEValidator v = DEValidator.builder().registerStandard().strict()
                .clock(Clock.fixed(CasosDePrueba.FECHA_FIRMA.plusSeconds(5).atZone(ZONA).toInstant(), ZONA)).build();
        ValidationResult r = v.validate(c.construir(), ctx);
        Set<String> errores = new TreeSet<>();
        for (ValidationError e : r.getErrors()) {
            errores.add(e.getCode());
        }
        Assertions.assertFalse(r.contains("JBS-VAL-00"), r::toString);
        Assertions.assertEquals(new TreeSet<>(c.esperadas()), errores, () -> c.id() + " " + c.descripcion() + ": " + r);
        if (errores.isEmpty()) {
            DocumentoElectronico de = c.construir();
            Assertions.assertNotNull(de.generarXml(ctx, cfg), c.id());
        }
    }
}
