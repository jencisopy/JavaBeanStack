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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.javabeanstack.sifen.api.EventResult;
import org.javabeanstack.sifen.api.ResultStatus;
import org.javabeanstack.sifen.core.beans.EventosDE;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.fields.request.event.TrGesEve;
import org.javabeanstack.sifen.core.validation.EventValidator;
import org.javabeanstack.sifen.core.validation.ValidationContext;
import org.javabeanstack.sifen.core.validation.ValidationError;
import org.javabeanstack.sifen.core.validation.ValidationResult;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.simulator.SifenSimulator;
import org.javabeanstack.sifen.simulator.SimulatedService;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code validateEvents}, {@code dFecFirma} e {@code Id} (plan JBSSIF, I8-34, decisión del usuario ESC-35): un
 * evento sin {@code dFecFirma} o sin {@code Id} se valida como lo enviaría {@link ISifenClient#sendEvents}, que
 * los completa (la hora de Asunción del reloj del cliente y un {@code Id} de su numeración), sin cargarlos en el
 * evento y sin avanzar la numeración. Con los campos cargados se validan los suyos. Sin red: el simulador
 * escucha en {@code 127.0.0.1}.
 */
public class SifenClientValidateEventsTest {
    private static SifenSimulator sim;

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

    private static TrGesEve unico(EventosDE events) {
        Assertions.assertEquals(1, events.getrGesEveList().size());
        return events.getrGesEveList().get(0);
    }

    /** Un caso del catálogo de eventos con {@code Id} y {@code dFecFirma} reemplazados. */
    private static EventosDE evento(int caso, String id, LocalDateTime fechaFirma) {
        EventosDE events = CasosDePrueba.eventos().get(caso).construir();
        TrGesEve e = unico(events);
        e.setId(id);
        e.setdFecFirma(fechaFirma);
        return events;
    }

    private static TreeSet<String> codigos(ValidationResult r) {
        TreeSet<String> s = new TreeSet<>();
        for (ValidationError e : r.getAll()) {
            s.add(e.getCode() + "@" + e.getPath());
        }
        return s;
    }

    private static LocalDateTime ahoraEnAsuncion() {
        return LocalDateTime.now(ValidationContext.SIFEN_ZONE).truncatedTo(ChronoUnit.SECONDS);
    }

    /**
     * Los seis casos del catálogo, sin {@code Id} ni {@code dFecFirma} (y con {@code Id} vacío o en blanco, que
     * {@code sendEvents} también reemplaza): dan lo mismo que el evento completo y el evento no cambia.
     */
    @Test
    public void sinFechaNiIdValidaComoLoEnviariaSendEventsYNoTocaElEvento() throws Exception {
        ISifenClient c = cliente();
        int casos = CasosDePrueba.eventos().size();
        Assertions.assertEquals(6, casos, "el catálogo de eventos tiene seis casos");
        Assertions.assertTrue(c.validateEvents(CasosDePrueba.eventos().get(0).construir()).isValid(),
                "el caso 0 tal cual es válido");
        for (int i = 0; i < casos; i++) {
            ValidationResult completo = c.validateEvents(CasosDePrueba.eventos().get(i).construir());
            for (String id : new String[] {null, "", "   "}) {
                EventosDE events = evento(i, id, null);
                ValidationResult r = c.validateEvents(events);
                String donde = "caso " + i + ", Id=" + (id == null ? "null" : "'" + id + "'") + ": " + r;
                Assertions.assertFalse(r.contains("JBS-EVENTO-01"), donde);
                Assertions.assertFalse(r.contains("JBS-EVENTO-02"), donde);
                Assertions.assertFalse(r.contains("GDE004"), donde);
                Assertions.assertEquals(codigos(completo), codigos(r), "mismo resultado que el evento completo, " + donde);
                Assertions.assertEquals(id, unico(events).getId(), "validateEvents no carga el Id, " + donde);
                Assertions.assertNull(unico(events).getdFecFirma(), "validateEvents no carga dFecFirma, " + donde);
            }
        }
        Assertions.assertTrue(sim.requests().isEmpty(), "validar no contacta a SIFEN");
    }

    /** Varios eventos en un mismo {@code EventosDE}: cada uno sin {@code Id} ni fecha se valida con los supuestos. */
    @Test
    public void cadaEventoDelEnvioUsaLosSupuestos() throws Exception {
        EventosDE events = evento(0, null, null);
        TrGesEve otro = unico(evento(2, null, null));
        events.getrGesEveList().add(otro);
        ValidationResult r = cliente().validateEvents(events);
        Assertions.assertTrue(r.isValid(), r.toString());
        for (TrGesEve e : events.getrGesEveList()) {
            Assertions.assertNull(e.getId());
            Assertions.assertNull(e.getdFecFirma());
        }
    }

    /** {@code sendEvents} del mismo evento que {@code validateEvents} dio por válido sigue completando los dos campos. */
    @Test
    public void sendEventsDelMismoEventoLoCompleta() throws Exception {
        AtomicInteger llamadas = new AtomicInteger();
        Supplier<String> numeracion = () -> String.valueOf(77 + llamadas.getAndIncrement());
        ISifenClient c = SifenClient.create(sim.config(), null, Clock.systemUTC(), numeracion, null);
        EventosDE events = evento(0, null, null);

        Assertions.assertTrue(c.validateEvents(events).isValid());
        Assertions.assertNull(unico(events).getId());
        Assertions.assertNull(unico(events).getdFecFirma());

        LocalDateTime antes = ahoraEnAsuncion();
        EventResult r = c.sendEvents(events);
        Assertions.assertEquals(ResultStatus.APPROVED, r.getStatus(), r.toString());
        Assertions.assertEquals("77", unico(events).getId(), "sendEvents completa el Id con la numeración del cliente");
        Assertions.assertEquals("77", r.getEventId());
        Assertions.assertNotNull(unico(events).getdFecFirma(), "sendEvents completa dFecFirma");
        Assertions.assertFalse(unico(events).getdFecFirma().isBefore(antes.minusSeconds(1)), "con la hora actual");
        Assertions.assertEquals(1, llamadas.get(), "solo el envío tomó un Id");
        Assertions.assertEquals(1, sim.requests(SimulatedService.EVENT).size());
    }

    /** Con los campos cargados se validan tal cual: los inválidos siguen fallando. */
    @Test
    public void conValoresInvalidosSigueFallando() throws Exception {
        ISifenClient c = cliente();

        for (String id : new String[] {"abc", "12345678901", "-1"}) {
            EventosDE events = evento(0, id, CasosDePrueba.FECHA_FIRMA);
            ValidationResult r = c.validateEvents(events);
            Assertions.assertTrue(r.contains("JBS-EVENTO-01"), "Id '" + id + "': " + r);
            Assertions.assertEquals(id, r.find("JBS-EVENTO-01").get(0).getValue().orElse(null),
                    "el valor mostrado es el del evento");
            Assertions.assertFalse(r.isValid());
            Assertions.assertEquals(id, unico(events).getId());
        }

        LocalDateTime enDosHoras = ahoraEnAsuncion().plusHours(2);
        EventosDE futura = evento(0, "5", enDosHoras);
        ValidationResult rf = c.validateEvents(futura);
        Assertions.assertTrue(rf.contains("GDE004"), "una dFecFirma futura sigue avisando GDE004: " + rf);
        Assertions.assertFalse(rf.find("GDE004").get(0).isError(), "GDE004 es un aviso");
        Assertions.assertEquals(enDosHoras.toString(), rf.find("GDE004").get(0).getValue().orElse(null));
        Assertions.assertEquals(enDosHoras, unico(futura).getdFecFirma(), "la fecha cargada no se toca");
    }

    /**
     * La fecha supuesta sale del reloj del cliente, el mismo que usa {@code sendEvents}: con el reloj dos horas
     * adelantado, {@code validateEvents} avisa GDE004 con la hora supuesta, como avisaría la validación del envío.
     */
    @Test
    public void laFechaSupuestaSaleDelRelojDelCliente() throws Exception {
        Clock adelantado = Clock.offset(Clock.systemUTC(), Duration.ofHours(2));
        ISifenClient c = SifenClient.create(sim.config(), null, adelantado, null, null);
        EventosDE events = evento(0, "5", null);

        LocalDateTime min = LocalDateTime.ofInstant(adelantado.instant(), ValidationContext.SIFEN_ZONE)
                .truncatedTo(ChronoUnit.SECONDS);
        ValidationResult r = c.validateEvents(events);
        LocalDateTime max = LocalDateTime.ofInstant(adelantado.instant(), ValidationContext.SIFEN_ZONE)
                .truncatedTo(ChronoUnit.SECONDS);

        Assertions.assertTrue(r.contains("GDE004"), "la hora del reloj del cliente está en el futuro: " + r);
        Assertions.assertFalse(r.contains("JBS-EVENTO-02"), r.toString());
        LocalDateTime mostrada = LocalDateTime.parse(r.find("GDE004").get(0).getValue().orElseThrow());
        Assertions.assertFalse(mostrada.isBefore(min) || mostrada.isAfter(max), "el valor mostrado es la supuesta: " + mostrada);
        Assertions.assertNull(unico(events).getdFecFirma());
    }

    /** Validar no consume la numeración de eventos: ni la inyectada ni la compartida del cliente. */
    @Test
    public void validarNoAvanzaLaNumeracionDeEventos() throws Exception {
        AtomicInteger llamadas = new AtomicInteger();
        Supplier<String> numeracion = () -> String.valueOf(1 + llamadas.getAndIncrement());
        ISifenClient inyectada = SifenClient.create(sim.config(), null, Clock.systemUTC(), numeracion, null);
        for (int i = 0; i < 5; i++) {
            Assertions.assertTrue(inyectada.validateEvents(evento(0, null, null)).isValid());
        }
        Assertions.assertEquals(0, llamadas.get(), "validateEvents no pidió ningún Id");

        long ms = 1_795_532_400_123L;
        Clock fijo = Clock.fixed(Instant.ofEpochMilli(ms), ZoneOffset.UTC);
        SifenClientShared compartida = new SifenClientShared();
        ISifenClient c = SifenClient.create(sim.config(), null, fijo, null, compartida);
        for (int i = 0; i < 5; i++) {
            Assertions.assertTrue(c.validateEvents(evento(0, null, null)).isValid());
        }
        Assertions.assertEquals(String.valueOf(ms % 10_000_000_000L), compartida.nextEventId(fijo),
                "el primer Id que se toma después de validar es el del reloj, no uno corrido por las validaciones");
    }

    /** El {@code Id} supuesto es el que daría la numeración en ese momento, también en el borde de los diez dígitos. */
    @Test
    public void elIdSupuestoEsElSiguienteDeLaNumeracionSinConsumirlo() {
        long ms = 1_795_532_400_123L;
        Clock fijo = Clock.fixed(Instant.ofEpochMilli(ms), ZoneOffset.UTC);
        SifenClientShared sh = new SifenClientShared();
        String visto = sh.peekNextEventId(fijo);
        Assertions.assertEquals(visto, sh.peekNextEventId(fijo), "mirar dos veces da lo mismo");
        Assertions.assertEquals(visto, sh.nextEventId(fijo), "lo que se miró es lo que se toma");
        Assertions.assertEquals(String.valueOf(Long.parseLong(visto) + 1), sh.peekNextEventId(fijo));

        SifenClientShared borde = new SifenClientShared();
        Clock ultimo = Clock.fixed(Instant.ofEpochMilli(30_000_000_000L - 1), ZoneOffset.UTC);
        Assertions.assertEquals("9999999999", borde.nextEventId(ultimo));
        Assertions.assertEquals("1", borde.peekNextEventId(ultimo), "al pasar de diez dígitos vuelve a empezar");
        Assertions.assertEquals("1", borde.nextEventId(ultimo));
    }

    /** Los supuestos son solo de {@code validateEvents}: el validador sin ellos sigue exigiendo {@code Id} y fecha. */
    @Test
    public void elValidadorSinSupuestosSigueExigiendolos() throws Exception {
        EventosDE events = evento(0, null, null);
        GenerationCtx ctx = GenerationCtx.getDefaultFromConfig(sim.config());

        ValidationResult directo = EventValidator.standard().validate(events, ctx);
        Assertions.assertTrue(directo.contains("JBS-EVENTO-01"), directo.toString());
        Assertions.assertTrue(directo.contains("JBS-EVENTO-02"), directo.toString());

        ValidationResult nulos = EventValidator.standard().validate(events, ctx, null, null);
        Assertions.assertEquals(codigos(directo), codigos(nulos), "supuestos nulos = sin suponer");

        ValidationResult conSupuestos = EventValidator.standard().validate(events, ctx, ahoraEnAsuncion(), "9");
        Assertions.assertFalse(conSupuestos.contains("JBS-EVENTO-01"), conSupuestos.toString());
        Assertions.assertFalse(conSupuestos.contains("JBS-EVENTO-02"), conSupuestos.toString());

        ValidationResult supuestoInvalido = EventValidator.standard().validate(events, ctx, ahoraEnAsuncion(), "x");
        Assertions.assertTrue(supuestoInvalido.contains("JBS-EVENTO-01"), "el supuesto también cumple la regla");
        Assertions.assertNull(unico(events).getId(), "ningún camino de validación carga el Id");
        Assertions.assertNull(unico(events).getdFecFirma(), "ningún camino de validación carga dFecFirma");
    }

    @Test
    public void sinEventosSigueSiendoJbsVal01() throws Exception {
        ISifenClient c = cliente();
        for (EventosDE vacio : List.of(new EventosDE())) {
            Assertions.assertTrue(c.validateEvents(vacio).contains("JBS-VAL-01"));
        }
        Assertions.assertTrue(c.validateEvents(null).contains("JBS-VAL-01"));
    }
}
