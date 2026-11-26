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
package org.javabeanstack.sifen;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.javabeanstack.sifen.api.SignedDocument;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.validation.DEValidator;
import org.javabeanstack.sifen.core.validation.ValidationContext;
import org.javabeanstack.sifen.core.validation.ValidationError;
import org.javabeanstack.sifen.core.validation.ValidationResult;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.simulator.SifenSimulator;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code validateDE} y {@code dFecFirma} (plan JBSSIF, I8-27, decisión del usuario ESC-32): sin
 * {@code dFecFirma}, {@link ISifenClient#validateDE} valida como si fuera la hora de Asunción con que la
 * completaría {@link ISifenClient#sign}, sin cargarla en el documento; con {@code dFecFirma}, valida la suya.
 * Sin red: el simulador escucha en {@code 127.0.0.1} y ninguna validación lo contacta.
 */
public class SifenClientValidateDETest {
    private static SifenSimulator sim;
    private static final AtomicInteger NUMERO = new AtomicInteger(700);

    @BeforeAll
    public static void levantar() throws Exception {
        sim = SifenSimulator.start();
    }

    @AfterAll
    public static void bajar() {
        if (sim != null) {
            sim.close();
        }
    }

    @BeforeEach
    public void limpiar() {
        sim.reset();
    }

    private static ISifenClient cliente() throws SifenException {
        return SifenClient.create(sim.config());
    }

    /** El DE del caso 4 del catálogo con un número propio, las leyendas de DEV (Q14) y sin {@code dFecFirma}. */
    private static DocumentoElectronico documentoSinFecha() {
        DocumentoElectronico de = CasosDePrueba.documentos().get(4).construir();
        de.getgDatGralOpe().getgEmis().setdNomEmi("DE generado en ambiente de prueba - sin valor comercial ni fiscal");
        de.getgDtipDE().getgCamItemList().get(0).setdDesProSer(
                "DOCUMENTO ELECTRÓNICO SIN VALOR COMERCIAL NI FISCAL - GENERADO EN AMBIENTE DE PRUEBA");
        de.getgTimb().setdNumDoc(String.format("%07d", NUMERO.incrementAndGet()));
        de.setdFecFirma(null);
        return de;
    }

    private static LocalDateTime ahoraEnAsuncion() {
        return LocalDateTime.now(ValidationContext.SIFEN_ZONE).truncatedTo(ChronoUnit.SECONDS);
    }

    @Test
    public void sinFechaDeFirmaValidaComoLaFirmariaSignYNoTocaElDocumento() throws Exception {
        ISifenClient c = cliente();
        DocumentoElectronico de = documentoSinFecha();

        ValidationResult r = c.validateDE(de);

        Assertions.assertFalse(r.contains("JBS-A-03"), "sin dFecFirma no se reporta su falta: " + r);
        Assertions.assertFalse(r.contains("A004a"), "la hora actual no está en el futuro: " + r);
        Assertions.assertFalse(r.contains("A004b"), "la hora actual no tiene más de 72 h: " + r);
        Assertions.assertTrue(r.isValid(), "el DE sin dFecFirma es válido para la validación previa: " + r);
        Assertions.assertNull(de.getdFecFirma(), "validateDE no carga dFecFirma en el documento del usuario");

        LocalDateTime antes = ahoraEnAsuncion();
        SignedDocument doc = c.sign(de);
        Assertions.assertNotNull(doc, "sign firma el mismo DE que validateDE dio por válido");
        Assertions.assertFalse(doc.getCdc().isEmpty());
        Assertions.assertNotNull(de.getdFecFirma(), "sign sí completa dFecFirma");
        Assertions.assertFalse(de.getdFecFirma().isBefore(antes.minusSeconds(1)), "sign completa con la hora actual");
        Assertions.assertTrue(sim.requests().isEmpty(), "ni validar ni firmar contactan a SIFEN");
    }

    @Test
    public void conFechaDeFirmaCargadaValidaLaSuya() throws Exception {
        ISifenClient c = cliente();

        DocumentoElectronico futura = documentoSinFecha();
        LocalDateTime enDosHoras = ahoraEnAsuncion().plusHours(2);
        futura.setdFecFirma(enDosHoras);
        ValidationResult rf = c.validateDE(futura);
        Assertions.assertTrue(rf.contains("A004a"), "una dFecFirma futura sigue avisando A004a: " + rf);
        Assertions.assertEquals(enDosHoras, futura.getdFecFirma(), "la fecha cargada no se toca");

        DocumentoElectronico vieja = documentoSinFecha();
        LocalDateTime hace100Horas = ahoraEnAsuncion().minusHours(100);
        vieja.setdFecFirma(hace100Horas);
        ValidationResult rv = c.validateDE(vieja);
        Assertions.assertTrue(rv.contains("A004b"), "una dFecFirma de más de 72 h sigue avisando A004b: " + rv);
        ValidationError aviso = rv.find("A004b").get(0);
        Assertions.assertFalse(aviso.isError(), "A004b es un aviso");
        Assertions.assertTrue(aviso.getValue().orElse("").startsWith(hace100Horas.toString()),
                "el valor mostrado es la fecha del documento: " + aviso.getValue());
        Assertions.assertEquals(hace100Horas, vieja.getdFecFirma());
    }

    /**
     * Un solo lugar (I8-27): la fecha supuesta sale del mismo reloj del cliente que usa {@code sign}. Con un reloj
     * dos horas adelantado, {@code validateDE} avisa A004a (como avisaría la validación de {@code sign}) y
     * {@code sign} completa exactamente esa hora.
     */
    @Test
    public void laFechaSupuestaSaleDelRelojDelCliente() throws Exception {
        Clock adelantado = Clock.offset(Clock.systemUTC(), Duration.ofHours(2));
        ISifenClient c = SifenClient.create(sim.config(), null, adelantado, null, null);
        DocumentoElectronico de = documentoSinFecha();

        ValidationResult r = c.validateDE(de);
        Assertions.assertTrue(r.contains("A004a"), "la hora del reloj del cliente está en el futuro: " + r);
        Assertions.assertFalse(r.contains("JBS-A-03"), r.toString());
        Assertions.assertNull(de.getdFecFirma());

        LocalDateTime esperadaMin = LocalDateTime.ofInstant(adelantado.instant(), ValidationContext.SIFEN_ZONE)
                .truncatedTo(ChronoUnit.SECONDS);
        c.sign(de);
        LocalDateTime esperadaMax = LocalDateTime.ofInstant(adelantado.instant(), ValidationContext.SIFEN_ZONE)
                .truncatedTo(ChronoUnit.SECONDS);
        Assertions.assertFalse(de.getdFecFirma().isBefore(esperadaMin), "sign completa con el reloj del cliente");
        Assertions.assertFalse(de.getdFecFirma().isAfter(esperadaMax), "sign completa con el reloj del cliente");
    }

    /** La fecha supuesta es solo de {@code validateDE}: el validador sin ella sigue exigiendo {@code dFecFirma}. */
    @Test
    public void elValidadorSinFechaSupuestaSigueExigiendoDFecFirma() throws Exception {
        DocumentoElectronico de = documentoSinFecha();
        GenerationCtx ctx = GenerationCtx.getDefaultFromConfig(sim.config());

        ValidationResult directo = DEValidator.standard().validate(de, ctx);
        Assertions.assertTrue(directo.contains("JBS-A-03"), "DEValidator.validate(de, ctx) no supone fecha: " + directo);
        Assertions.assertFalse(directo.find("JBS-A-03").isEmpty() || !directo.find("JBS-A-03").get(0).isError());

        ValidationResult sinSupuesta = DEValidator.standard().validate(de, ctx, null);
        Assertions.assertTrue(sinSupuesta.contains("JBS-A-03"), "fecha supuesta nula = sin suponer: " + sinSupuesta);

        ValidationResult conSupuesta = DEValidator.standard().validate(de, ctx, ahoraEnAsuncion());
        Assertions.assertFalse(conSupuesta.contains("JBS-A-03"), conSupuesta.toString());
        Assertions.assertNull(de.getdFecFirma(), "ningún camino de validación carga dFecFirma");
    }

    @Test
    public void unDocumentoNuloSigueSiendoJbsVal01() throws Exception {
        ValidationResult r = cliente().validateDE(null);
        Assertions.assertTrue(r.contains("JBS-VAL-01"), r.toString());
        Assertions.assertFalse(r.isValid());
    }
}
