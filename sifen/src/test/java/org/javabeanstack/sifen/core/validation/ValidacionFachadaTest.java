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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import org.javabeanstack.sifen.Sifen;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.beans.EventosDE;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.FallasFachada;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Integración de la validación previa en la fachada (plan JBSSIF, §3.5, D20): con la validación
 * activa los casos del catálogo generan el mismo XML que sin ella; con un dato faltante
 * {@code generarXml} y los {@code recepcion*} lanzan {@link SifenValidationException} antes de armar
 * el XML o de abrir una conexión (D28: nada sale a la red en estas pruebas).
 *
 * <p>Los fixtures de la Fase 2 usan un emisor ficticio: en DEV D105 exige la leyenda de pruebas, así
 * que cada caso se prepara con {@link #conLeyenda}. Los que se desvían del MT a propósito o por
 * construcción quedan fijados en {@link #desviosConocidos()}.</p>
 */
public class ValidacionFachadaTest {
    /** Instante fijo, cinco minutos después de la emisión de los fixtures (reglas de plazo, DEC-03). */
    private static final Clock RELOJ = Clock.fixed(CasosDePrueba.FECHA_EMISION.plusMinutes(5)
            .atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

    /** Pone el nombre del emisor exigido en DEV (D105). */
    private static DocumentoElectronico conLeyenda(DocumentoElectronico de) {
        de.getgDatGralOpe().getgEmis().setdNomEmi(org.javabeanstack.sifen.core.validation.support.Legends.MT);
        return de;
    }

    private static DocumentoElectronico preparar(CasosDePrueba.Caso c, boolean porObjeto) throws Exception {
        return conLeyenda(porObjeto ? c.construir() : c.leer(FixturesSifen.entrada(c)));
    }

    private static Set<String> errores(ValidationResult r) {
        Set<String> codigos = new TreeSet<>();
        for (ValidationError e : r.getErrors()) {
            codigos.add(e.getCode());
        }
        return codigos;
    }

    /** Errores que los casos congelados traen hoy: la lista única de {@link SoporteValidacion#DESVIOS_ESPERADOS} (I3-14). */
    private static Set<String> desviosConocidos(String id) {
        return SoporteValidacion.desviosEsperados(id);
    }

    @Test
    public void conValidacionLosCasosGeneranElMismoXmlQueSinElla() throws Exception {
        SifenConfig sin = FixturesSifen.configuracion();
        SifenConfig con = FixturesSifen.configuracionConValidacion();
        Assertions.assertTrue(con.isPreValidationEnabled());
        Assertions.assertFalse(sin.isPreValidationEnabled());
        int comparados = 0;
        for (CasosDePrueba.Caso c : CasosDePrueba.documentos()) {
            for (boolean porObjeto : new boolean[]{true, false}) {
                if (porObjeto && !c.tieneConstructor()) {
                    continue;
                }
                String via = c.getId() + (porObjeto ? "/obj" : "/xml");
                ValidationResult r = DEValidator.builder().registerStandard().strict().clock(RELOJ).build()
                        .validate(preparar(c, porObjeto), FixturesSifen.contexto(con));
                Assertions.assertFalse(r.contains("JBS-VAL-00"), via + ": " + r);
                Assertions.assertEquals(desviosConocidos(c.getId()), errores(r), via + ": " + r);
                if (!r.isValid()) {
                    Assertions.assertThrows(SifenValidationException.class,
                            () -> preparar(c, porObjeto).generarXml(FixturesSifen.contexto(con), con), via);
                    continue;
                }
                String xmlSin = preparar(c, porObjeto).generarXml(FixturesSifen.contexto(sin), sin);
                String xmlCon = preparar(c, porObjeto).generarXml(FixturesSifen.contexto(con), con);
                Assertions.assertEquals(xmlSin, xmlCon, via);
                comparados++;
            }
        }
        // test-developer (2026-11-24): DE-24…DE-32 suman 18 vías; DE-30 trae el desvío de E3-24 (2 vías).
        // app-developer (2026-11-24, I4-11): DE-18 deja de ser desvío (la tasa 10.00 se escribe 10): +2 vías.
        Assertions.assertEquals(52, comparados, "casos comparados (59 vías − 7 con desvíos conocidos)");
    }

    @Test
    public void unDeIncompletoNoLlegaAGenerarseNiDaNullPointer() throws Exception {
        SifenConfig con = FixturesSifen.configuracionConValidacion();
        DocumentoElectronico de = conLeyenda(CasosDePrueba.documentos().get(4).construir());
        de.getgDatGralOpe().setgDatRec(null);
        de.getgDtipDE().getgCamItemList().get(0).getgValorItem().getgValorRestaItem().setdDescItem(null);
        SifenValidationException e = Assertions.assertThrows(SifenValidationException.class,
                () -> de.generarXml(FixturesSifen.contexto(con), con));
        Assertions.assertTrue(e.getResult().contains("JBS-D-39"), e.getMessage());
        Assertions.assertTrue(e.getResult().contains("JBS-E-83"), e.getMessage());
        Assertions.assertInstanceOf(SifenException.class, e);
        // sin validación previa, la línea base: NullPointerException en la generación, que desde §3.8
        // sale de la fachada como JBS-INT-00 con id de correlación
        SifenConfig sin = FixturesSifen.configuracion();
        FallasFachada.npeInterna(() -> de.generarXml(FixturesSifen.contexto(sin), sin));
    }

    @Test
    public void lasRecepcionesValidanAntesDeConectar() throws Exception {
        SifenConfig con = FixturesSifen.configuracionConValidacion();
        GenerationCtx ctx = FixturesSifen.contexto(con);
        DocumentoElectronico malo = conLeyenda(CasosDePrueba.documentos().get(4).construir());
        malo.getgDatGralOpe().getgEmis().setdEmailE(null);   // el rechazo real de I2-08
        SifenValidationException uno = Assertions.assertThrows(SifenValidationException.class,
                () -> Sifen.recepcionDE(malo, con, ctx));
        Assertions.assertTrue(uno.getResult().contains("JBS-D-28"), uno.getMessage());

        List<DocumentoElectronico> lote = new ArrayList<>();
        lote.add(conLeyenda(CasosDePrueba.documentos().get(4).construir()));
        lote.add(malo);
        SifenValidationException deLote = Assertions.assertThrows(SifenValidationException.class,
                () -> Sifen.recepcionLoteDE(lote, con, ctx));
        Assertions.assertEquals("lote[1].gDatGralOpe.gEmis.dEmailE", deLote.getResult().find("JBS-D-28").get(0).getPath());
        Assertions.assertTrue(deLote.getResult().getErrors().stream().allMatch(err -> err.getPath().startsWith("lote[1].")),
                deLote.getMessage());

        EventosDE ev = CasosDePrueba.eventos().get(0).construir();
        ev.getrGesEveList().get(0).getgGroupTiEvt().getrGeVeCan().setmOtEve(null);
        SifenValidationException deEvento = Assertions.assertThrows(SifenValidationException.class,
                () -> Sifen.recepcionEvento(ev, con, ctx));
        Assertions.assertEquals("rGesEve[0].rEve.gGroupTiEvt.rGeVeCan.mOtEve",
                deEvento.getResult().find("JBS-EVENTO-04").get(0).getPath());
    }

    @Test
    public void validarDeYValidarEventoNoLanzan() throws Exception {
        GenerationCtx ctx = FixturesSifen.contexto(FixturesSifen.configuracionConValidacion());
        Assertions.assertTrue(Sifen.validarDE(conLeyenda(CasosDePrueba.documentos().get(4).construir()), ctx).isValid());
        Assertions.assertTrue(Sifen.validarDE(null, ctx).contains("JBS-VAL-01"));
        Assertions.assertTrue(Sifen.validarDE(new DocumentoElectronico(), null).contains("JBS-VAL-02"));
        Assertions.assertTrue(Sifen.validarEvento(CasosDePrueba.eventos().get(0).construir(), ctx).isValid());
        ValidationResult vacio = Sifen.validarEvento(null, ctx);
        Assertions.assertEquals("rEve", vacio.find("JBS-VAL-01").get(0).getPath(), "I3-06: la raíz del evento");
    }

    @Test
    public void losEventosDelCatalogoSoloTraenSusDesviosConocidos() {
        GenerationCtx ctx = FixturesSifen.contexto(FixturesSifen.configuracionConValidacion());
        EventValidator v = EventValidator.builder().registerStandard().strict().clock(RELOJ).build();
        for (CasosDePrueba.CasoEvento c : CasosDePrueba.eventos()) {
            Set<String> esperados;
            switch (c.getId()) {
                case "EV-05":
                    esperados = Set.of("GENFE002", "GENFE011"); // DV del CDC, «Cedula» (E8-01: JBS-EVENTO-43 de baja)
                    break;
                case "EV-06":
                    esperados = Set.of("GENFE002");
                    break;
                default:
                    esperados = Set.of();
            }
            ValidationResult r = v.validate(c.construir(), ctx);
            Assertions.assertEquals(esperados, errores(r), c.getId() + ": " + r);
        }
    }

    @Test
    public void laPropiedadValidacionPreviaSeLeeDelArchivo() throws Exception {
        Assertions.assertTrue(new SifenConfig().isPreValidationEnabled(), "D20: activa por defecto");
        Assertions.assertFalse(cargar("sifen.validacion_previa=false\n").isPreValidationEnabled());
        Assertions.assertTrue(cargar("sifen.validacion_previa= TRUE \n").isPreValidationEnabled());
        Assertions.assertTrue(cargar("").isPreValidationEnabled(), "ausente = activa");
        SifenException e = Assertions.assertThrows(SifenException.class, () -> cargar("sifen.validacion_previa=si\n"));
        Assertions.assertTrue(e.getMessage().contains(SifenConfig.PRE_VALIDATION_KEY));
        Assertions.assertTrue(new SifenConfig().toString().contains("validacionPrevia=true"));
    }

    private static SifenConfig cargar(String texto) throws Exception {
        Properties p = new Properties();
        p.load(new ByteArrayInputStream(("sifen.ambiente=DEV\nsifen.certificado_cliente.tipo=PFX\n" + texto).getBytes(StandardCharsets.UTF_8)));
        // §3.7: la carga valida también el certificado (JBS-CFG-12 / JBS-CERT-*).
        p.setProperty("sifen.certificado_cliente.archivo", FixturesSifen.rutaPfx().toString());
        p.setProperty("sifen.certificado_cliente.contrasena", FixturesSifen.PFX_CLAVE);
        return SifenConfig.load(p);
    }
}
