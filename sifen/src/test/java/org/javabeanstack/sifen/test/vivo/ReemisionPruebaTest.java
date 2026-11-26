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
package org.javabeanstack.sifen.test.vivo;

import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.test.SifenTestEnv;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.javabeanstack.sifen.test.casos.XmlCanonico;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.TestAbortedException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Reemisión para el ambiente de pruebas, sin red (plan JBSSIF, §2.9): numeración ≥ 1000 (ESC-8) que sigue
 * desde la tabla de equivalencias y no repite, nota de crédito asociada al CDC reemitido, CDC
 * recalculado con el timbrado de prueba, correo del emisor ({@code dEmailE}) tomado del {@code .env}
 * y test saltado si falta (I2-08), guarda D28 de la reemisión (corta si la reemisión llevaría el punto o el
 * timbrado del original de producción, o un timbrado que no es {@code SIFEN_TIMBRADO}) y nota que solo
 * se envía con su factura aprobada (M2-05, respuestas simuladas con un predicado).
 */
public class ReemisionPruebaTest {
    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 23, 15, 30, 0);
    /** Correo ficticio del .env temporal (cumple el patrón tEmail del XSD). */
    private static final String EMAIL = "emisor.prueba@example.com";
    /** Timbrado de prueba del .env temporal (ficticio). */
    private static final int TIMBRADO_PRUEBA = 80123456;
    /** Timbrado y punto de «producción» de las ventas simuladas (ficticios; los reales no entran al repo). */
    private static final int TIMBRADO_PRODUCCION = CasosDePrueba.TIMBRADO;
    private static final String PUNTO_PRODUCCION = "002";
    /** iditemmovimiento ficticio, para comprobar que los mensajes no lo nombran. */
    private static final String ID_VENTA = "987654";

    @TempDir
    Path dir;

    private SifenTestEnv.Emisor emisor() throws Exception {
        return emisor("SIFEN_EMISOR_EMAIL=" + EMAIL + "\n");
    }

    /** Emisor de un .env temporal con el autofirmado; {@code lineaEmail} va tal cual (vacía = sin la clave). */
    private SifenTestEnv.Emisor emisor(String lineaEmail) throws Exception {
        return emisor("001", TIMBRADO_PRUEBA, lineaEmail);
    }

    /** Emisor de un .env temporal con el autofirmado, establecimiento 001 y el punto y el timbrado dados. */
    private SifenTestEnv.Emisor emisor(String punto, int timbrado, String lineaEmail) throws Exception {
        Path pfx = FixturesSifen.copiarPfx(dir.resolve("a.pfx"));
        Path env = dir.resolve("prueba.env");
        Files.write(env, ("SIFEN_AMBIENTE=DEV\nSIFEN_CERT_PATH=" + pfx + "\nSIFEN_CERT_PASS=x\nSIFEN_CSC_ID=0001\nSIFEN_CSC=y\n"
                + "SIFEN_EMISOR_RUC=80123456\nSIFEN_EMISOR_DV=5\nSIFEN_TIMBRADO=" + timbrado + "\nSIFEN_TIMBRADO_FECHA_INICIO=2024-10-31\n"
                + "SIFEN_ESTABLECIMIENTO=001\nSIFEN_PUNTO_EXPEDICION=" + punto + "\n" + lineaEmail).getBytes(StandardCharsets.ISO_8859_1));
        return SifenTestEnv.cargar(env).getEmisor();
    }

    /**
     * Pone el DE del catálogo (001-001, timbrado {@link CasosDePrueba#TIMBRADO}) como una venta de
     * producción: punto 002, timbrado ficticio de «producción» distinto del de prueba (guarda D28).
     * No toca el {@code Id}: el CDC original sigue siendo el del caso.
     */
    private static DocumentoElectronico produccion(DocumentoElectronico de) {
        de.getgTimb().setdEst("001");
        de.getgTimb().setdPunExp(PUNTO_PRODUCCION);
        de.getgTimb().setdNumTim(TIMBRADO_PRODUCCION);
        return de;
    }

    @Test
    public void numeracionDesde1000SinRepetirYContinuaEnLaSiguienteCorrida() throws Exception {
        Path tabla = dir.resolve("equivalencias.csv");
        ReemisionPrueba r = new ReemisionPrueba(emisor(), SifenTestEnv.NUMERO_INICIAL_MINIMO, tabla);
        Assertions.assertEquals(1000, r.getSiguiente(), "ESC-8: desde el 0001000");
        CasosDePrueba.Caso simple = CasosDePrueba.documentos().get(0);
        DocumentoElectronico a = produccion(simple.leer(FixturesSifen.entrada(simple)));
        DocumentoElectronico b = produccion(CasosDePrueba.documentos().get(6).construir());
        ReemisionPrueba.Reemision ra = r.reemitir("1", a, FECHA);
        ReemisionPrueba.Reemision rb = r.reemitir("2", b, FECHA);
        Assertions.assertEquals("001-001-0001000", ra.enviado);
        Assertions.assertEquals("001-001-0001001", rb.enviado);
        Assertions.assertEquals("001-002-0000001", ra.original);
        Assertions.assertEquals(CasosDePrueba.CDC_FACTURA_SIMPLE, ra.cdcOriginal);
        r.registrar("1", ra, "recepcionDE", 200, "0260", "Aprobado", "ok");
        r.registrar("2", rb, "recepcionDE", 200, "1001", "Rechazado", "con; punto y coma");
        // otra corrida: sigue desde el máximo registrado
        ReemisionPrueba otra = new ReemisionPrueba(emisor(), SifenTestEnv.NUMERO_INICIAL_MINIMO, tabla);
        Assertions.assertEquals(1002, otra.getSiguiente());
        Assertions.assertEquals(ra.cdc, otra.cdcReemitido(ra.cdcOriginal));
        List<String> lineas = Files.readAllLines(tabla, StandardCharsets.UTF_8);
        Assertions.assertEquals(ReemisionPrueba.CABECERA, lineas.get(0));
        Assertions.assertEquals(5, lineas.size(), "cabecera + 2 reservas + 2 resultados");
        Assertions.assertTrue(lineas.get(1).contains(";RESERVADO;"), lineas.get(1));
        // un inicial más alto manda sobre la tabla
        Assertions.assertEquals(1080, new ReemisionPrueba(emisor(), 1080, tabla).getSiguiente());
    }

    /** Un número reservado sin resultado (llamada que lanzó o corrida cortada) no se reutiliza. */
    @Test
    public void reservaSinResultadoNoSeReutiliza() throws Exception {
        Path tabla = dir.resolve("equivalencias.csv");
        ReemisionPrueba r = new ReemisionPrueba(emisor(), 50, tabla);
        r.reemitir("7", produccion(CasosDePrueba.documentos().get(4).construir()), FECHA);
        // sin registrar(): la corrida se cortó o recepcionDE lanzó
        Assertions.assertEquals(51, new ReemisionPrueba(emisor(), 50, tabla).getSiguiente());
    }

    /**
     * M12-15 (JBSSIF): una fila con el número «—» (la que dejaba la consulta de un lote con un CDC ajeno a la tabla) no
     * deja la tabla ilegible ni cuenta para la numeración.
     */
    @Test
    public void filaSinNumeroLegibleNoRompeLaTabla() throws Exception {
        Path tabla = dir.resolve("equivalencias.csv");
        ReemisionPrueba r = new ReemisionPrueba(emisor(), 50, tabla);
        ReemisionPrueba.Reemision ra = r.reemitir("7", produccion(CasosDePrueba.documentos().get(4).construir()), FECHA);
        r.registrar("—", new ReemisionPrueba.Reemision("—", "—", null, "01" + "0".repeat(42), FECHA), "ISifenClient.queryBatch:1", 200,
                "0260", "APPROVED", "ok");
        Assertions.assertEquals("001-001-0000050", ra.enviado);
        Assertions.assertEquals(51, new ReemisionPrueba(emisor(), 50, tabla).getSiguiente(), "la fila «—» se ignora");
    }

    @Test
    public void cdcYEmisorDePruebaRecalculados() throws Exception {
        ReemisionPrueba r = new ReemisionPrueba(emisor(), 50, dir.resolve("t.csv"));
        DocumentoElectronico de = produccion(CasosDePrueba.documentos().get(4).construir());
        ReemisionPrueba.Reemision re = r.reemitir("5", de, FECHA);
        Assertions.assertEquals(44, re.cdc.length());
        Assertions.assertTrue(re.cdc.startsWith("01" + "80123456" + "5" + "001" + "001" + "0000050"), re.cdc);
        Assertions.assertTrue(re.cdc.contains("20261123"), "fecha de emisión nueva en el CDC");
        Assertions.assertEquals(80123456, de.getgTimb().getdNumTim());
        Assertions.assertEquals(ReemisionPrueba.RAZON_SOCIAL_PRUEBA, de.getgDatGralOpe().getgEmis().getdNomEmi());
        // se genera y firma sin red con el autofirmado; el CDC del XML es el calculado
        SifenConfig cfg = FixturesSifen.configuracion();
        String xml = de.generarXml(GenerationCtx.getDefaultFromConfig(cfg), cfg);
        Assertions.assertTrue(xml.contains("Id=\"" + re.cdc + "\""));
        Assertions.assertEquals("0000050", XmlCanonico.texto(xml, "dNumDoc"));
        Assertions.assertEquals("2024-10-31", XmlCanonico.texto(xml, "dFeIniT"));
    }

    @Test
    public void notaDeCreditoApuntaAlCdcReemitidoDeSuFactura() throws Exception {
        ReemisionPrueba r = new ReemisionPrueba(emisor(), 50, dir.resolve("t.csv"));
        CasosDePrueba.Caso factura = CasosDePrueba.documentos().get(0);
        ReemisionPrueba.Reemision rf = r.reemitir("1", produccion(factura.leer(FixturesSifen.entrada(factura))), FECHA);
        Assertions.assertEquals(CasosDePrueba.CDC_FACTURA_SIMPLE, rf.cdcOriginal);
        DocumentoElectronico nc = produccion(CasosDePrueba.documentos().get(20).construir());
        r.reemitir("21", nc, FECHA);
        Assertions.assertEquals(rf.cdc, nc.getgCamDEAsocList().get(0).getdCdCDERef());
    }

    @Test
    public void notaSinSuFacturaReemitidaNoSeEnvia() throws Exception {
        ReemisionPrueba r = new ReemisionPrueba(emisor(), 50, dir.resolve("t.csv"));
        DocumentoElectronico nc = produccion(CasosDePrueba.documentos().get(20).construir());
        Assertions.assertThrows(IllegalStateException.class, () -> r.reemitir("21", nc, FECHA));
        Assertions.assertEquals(50, r.getSiguiente(), "no consume número");
    }

    // ------------------------------------------------------------------ I2-08 (dEmailE)

    /** La venta llega sin dEmailE (la base no lo tiene): el DE reemitido lleva el correo del .env. */
    @Test
    public void dEmailEVacioSeCompletaConElCorreoDelEnv() throws Exception {
        ReemisionPrueba r = new ReemisionPrueba(emisor(), 50, dir.resolve("t.csv"));
        DocumentoElectronico de = produccion(CasosDePrueba.documentos().get(4).construir());
        de.getgDatGralOpe().getgEmis().setdEmailE(null);
        r.reemitir("5", de, FECHA);
        Assertions.assertEquals(EMAIL, de.getgDatGralOpe().getgEmis().getdEmailE());
        SifenConfig cfg = FixturesSifen.configuracion();
        String xml = de.generarXml(GenerationCtx.getDefaultFromConfig(cfg), cfg);
        Assertions.assertEquals(EMAIL, XmlCanonico.texto(xml, "dEmailE"));
    }

    /** Un correo que ya traiga el DE se pisa: el emisor de pruebas es el del .env, no el de la venta. */
    @Test
    public void dEmailEDeLaVentaSePisaConElDelEnv() throws Exception {
        ReemisionPrueba r = new ReemisionPrueba(emisor(), 50, dir.resolve("t.csv"));
        CasosDePrueba.Caso simple = CasosDePrueba.documentos().get(0);
        DocumentoElectronico de = produccion(simple.leer(FixturesSifen.entrada(simple)));
        Assertions.assertNotNull(de.getgDatGralOpe().getgEmis().getdEmailE(), "el caso trae un correo propio");
        Assertions.assertNotEquals(EMAIL, de.getgDatGralOpe().getgEmis().getdEmailE());
        r.reemitir("1", de, FECHA);
        SifenConfig cfg = FixturesSifen.configuracion();
        Assertions.assertEquals(EMAIL, XmlCanonico.texto(de.generarXml(GenerationCtx.getDefaultFromConfig(cfg), cfg), "dEmailE"));
    }

    /** Sin SIFEN_EMISOR_EMAIL no se reemite: test saltado, mensaje con la clave y sin valores, sin reservar número. */
    @Test
    public void sinEmailDelEmisorSeSaltaSinReservar() throws Exception {
        SifenTestEnv.Emisor sinEmail = emisor("");
        Assertions.assertNotNull(sinEmail, "el emisor carga igual: el correo es opcional para la configuración");
        Assertions.assertNull(sinEmail.getEmail());
        Path tabla = dir.resolve("t.csv");
        TestAbortedException e = Assertions.assertThrows(TestAbortedException.class, () -> new ReemisionPrueba(sinEmail, 50, tabla));
        Assertions.assertTrue(e.getMessage().contains(SifenTestEnv.CLAVE_EMAIL), e.getMessage());
        Assertions.assertFalse(e.getMessage().contains("80123456"), "sin valores del .env en el mensaje");
        Assertions.assertFalse(Files.exists(tabla), "no reserva ningún número");
        // clave presente pero vacía: igual que ausente
        Assertions.assertThrows(TestAbortedException.class, () -> new ReemisionPrueba(emisor("SIFEN_EMISOR_EMAIL=\n"), 50, tabla));
    }

    /** Un correo que el XSD rechazaría (SIFEN lo rechaza y quema el número) también salta, sin mostrar el valor. */
    @Test
    public void emailConFormatoInvalidoSeSaltaSinMostrarlo() throws Exception {
        String malo = "correo-sin-arroba.example.com";
        TestAbortedException e = Assertions.assertThrows(TestAbortedException.class,
                () -> new ReemisionPrueba(emisor("SIFEN_EMISOR_EMAIL=" + malo + "\n"), 50, dir.resolve("t.csv")));
        Assertions.assertTrue(e.getMessage().contains(SifenTestEnv.CLAVE_EMAIL), e.getMessage());
        Assertions.assertFalse(e.getMessage().contains(malo), "sin el valor en el mensaje");
    }

    // ------------------------------------------------------------------ guarda D28 de la reemisión (001-001, timbrado de prueba)

    /** Mensaje de la guarda: nombra la regla, sin valores del .env, CDC ni datos de la venta. */
    private static void sinDatos(String mensaje) {
        Assertions.assertTrue(mensaje.contains("D28"), mensaje);
        for (String dato : new String[]{"80123456", String.valueOf(TIMBRADO_PRODUCCION), "001-002", ID_VENTA,
                CasosDePrueba.CDC_FACTURA_SIMPLE}) {
            Assertions.assertFalse(mensaje.contains(dato), "el mensaje no lleva el dato " + dato + ": " + mensaje);
        }
    }

    /** Reemisión con el mismo establecimiento-punto del original (001-002): corta, sin consumir ni reservar número. */
    @Test
    public void conElPuntoDelOriginalCortaSinReservar() throws Exception {
        Path tabla = dir.resolve("t.csv");
        ReemisionPrueba r = new ReemisionPrueba(emisor(PUNTO_PRODUCCION, TIMBRADO_PRUEBA, "SIFEN_EMISOR_EMAIL=" + EMAIL + "\n"), 50, tabla);
        CasosDePrueba.Caso simple = CasosDePrueba.documentos().get(0);
        DocumentoElectronico de = produccion(simple.leer(FixturesSifen.entrada(simple)));
        IllegalStateException e = Assertions.assertThrows(IllegalStateException.class, () -> r.reemitir(ID_VENTA, de, FECHA));
        Assertions.assertTrue(e.getMessage().contains("SIFEN_PUNTO_EXPEDICION"), e.getMessage());
        sinDatos(e.getMessage());
        Assertions.assertEquals(50, r.getSiguiente(), "no consume número");
        Assertions.assertFalse(Files.exists(tabla), "no reserva número");
        Assertions.assertEquals(TIMBRADO_PRODUCCION, de.getgTimb().getdNumTim(), "el DE queda sin tocar");
        Assertions.assertEquals("0000001", de.getgTimb().getdNumDoc());
        // la verificación previa (la que corre SifenVivoTest sobre todas las ventas) corta igual
        Assertions.assertThrows(IllegalStateException.class, () -> r.verificarAntesDeReemitir(de));
    }

    /** Reemisión con el timbrado del original (el de producción): corta, sin consumir ni reservar número. */
    @Test
    public void conElTimbradoDeProduccionCortaSinReservar() throws Exception {
        Path tabla = dir.resolve("t.csv");
        ReemisionPrueba r = new ReemisionPrueba(emisor("001", TIMBRADO_PRODUCCION, "SIFEN_EMISOR_EMAIL=" + EMAIL + "\n"), 50, tabla);
        DocumentoElectronico de = produccion(CasosDePrueba.documentos().get(4).construir());
        IllegalStateException e = Assertions.assertThrows(IllegalStateException.class, () -> r.reemitir(ID_VENTA, de, FECHA));
        Assertions.assertTrue(e.getMessage().contains("SIFEN_TIMBRADO"), e.getMessage());
        Assertions.assertTrue(e.getMessage().contains("producción"), e.getMessage());
        sinDatos(e.getMessage());
        Assertions.assertEquals(50, r.getSiguiente(), "no consume número");
        Assertions.assertFalse(Files.exists(tabla), "no reserva número");
        Assertions.assertEquals(PUNTO_PRODUCCION, de.getgTimb().getdPunExp(), "el DE queda sin tocar");
    }

    /** Venta de producción 001-002 → 001-001 con el timbrado de prueba: reemite y reserva. */
    @Test
    public void en001001ConElTimbradoDePruebaReemite() throws Exception {
        Path tabla = dir.resolve("t.csv");
        ReemisionPrueba r = new ReemisionPrueba(emisor(), 50, tabla);
        DocumentoElectronico de = produccion(CasosDePrueba.documentos().get(4).construir());
        r.verificarAntesDeReemitir(de);
        ReemisionPrueba.Reemision re = r.reemitir(ID_VENTA, de, FECHA);
        Assertions.assertTrue(re.original.startsWith("001-002-"), re.original);
        Assertions.assertEquals("001-001-0000050", re.enviado);
        Assertions.assertEquals(TIMBRADO_PRUEBA, de.getgTimb().getdNumTim());
        Assertions.assertEquals(51, r.getSiguiente());
        Assertions.assertTrue(Files.readAllLines(tabla, StandardCharsets.UTF_8).get(1).contains(";RESERVADO;"));
    }

    /** Las tres reglas de la guarda, directo sobre el método (incluida la del timbrado del .env). */
    @Test
    public void guardaDeLaReemisionPorRegla() {
        // válido: 001-002 / producción → 001-001 / prueba = .env
        SifenTestEnv.verificarReemisionDePrueba("001", "002", 11111111, "001", "001", 22222222, 22222222);
        // el timbrado que quedó en el DE no es SIFEN_TIMBRADO
        IllegalStateException e = Assertions.assertThrows(IllegalStateException.class,
                () -> SifenTestEnv.verificarReemisionDePrueba("001", "002", 11111111, "001", "001", 33333333, 22222222));
        Assertions.assertTrue(e.getMessage().contains("SIFEN_TIMBRADO"), e.getMessage());
        Assertions.assertFalse(e.getMessage().contains("33333333") || e.getMessage().contains("22222222"), e.getMessage());
        // mismo timbrado que el original
        Assertions.assertThrows(IllegalStateException.class,
                () -> SifenTestEnv.verificarReemisionDePrueba("001", "002", 22222222, "001", "001", 22222222, 22222222));
        // mismo establecimiento-punto, también con otro relleno de ceros
        Assertions.assertThrows(IllegalStateException.class,
                () -> SifenTestEnv.verificarReemisionDePrueba("001", "002", 11111111, "1", "2", 22222222, 22222222));
        // otro establecimiento con el mismo punto: válido (difiere el par)
        SifenTestEnv.verificarReemisionDePrueba("002", "001", 11111111, "001", "001", 22222222, 22222222);
        // original sin establecimiento o punto: no se puede comprobar, corta
        Assertions.assertThrows(IllegalStateException.class,
                () -> SifenTestEnv.verificarReemisionDePrueba("001", null, 11111111, "001", "001", 22222222, 22222222));
    }

    // ------------------------------------------------------------------ M2-05 (nota solo con su factura aprobada)

    /** Factura asociada reemitida y aprobada (respuesta simulada): la nota se envía. */
    @Test
    public void notaConSuFacturaAprobadaSeEnvia() throws Exception {
        ReemisionPrueba r = new ReemisionPrueba(emisor(), 50, dir.resolve("t.csv"));
        CasosDePrueba.Caso factura = CasosDePrueba.documentos().get(0);
        ReemisionPrueba.Reemision rf = r.reemitir("1", produccion(factura.leer(FixturesSifen.entrada(factura))), FECHA);
        DocumentoElectronico nc = produccion(CasosDePrueba.documentos().get(20).construir());
        Assertions.assertEquals(List.of(rf.cdc), r.cdcAsociadosReemitidos(nc));
        Assertions.assertNull(r.motivoParaNoEnviarNota(nc, Set.of(rf.cdc)::contains));
    }

    /** Factura asociada reemitida pero rechazada (respuesta simulada): la nota no se envía ni reserva número. */
    @Test
    public void notaConSuFacturaNoAprobadaNoReservaNumero() throws Exception {
        Path tabla = dir.resolve("t.csv");
        ReemisionPrueba r = new ReemisionPrueba(emisor(), 50, tabla);
        CasosDePrueba.Caso factura = CasosDePrueba.documentos().get(0);
        ReemisionPrueba.Reemision rf = r.reemitir("1", produccion(factura.leer(FixturesSifen.entrada(factura))), FECHA);
        DocumentoElectronico nc = produccion(CasosDePrueba.documentos().get(20).construir());
        List<String> consultados = new ArrayList<>();
        String motivo = r.motivoParaNoEnviarNota(nc, cdc -> {
            consultados.add(cdc);
            return false;
        });
        Assertions.assertNotNull(motivo);
        Assertions.assertTrue(motivo.contains("no quedó aprobada"), motivo);
        Assertions.assertFalse(motivo.contains(rf.cdc) || motivo.contains(CasosDePrueba.CDC_FACTURA_SIMPLE), "sin CDC: " + motivo);
        Assertions.assertEquals(List.of(rf.cdc), consultados, "se pregunta por el CDC reemitido, no por el de producción");
        Assertions.assertEquals(51, r.getSiguiente(), "la nota no consume número");
        Assertions.assertEquals(2, Files.readAllLines(tabla, StandardCharsets.UTF_8).size(), "cabecera + la reserva de la factura");
        Assertions.assertEquals(CasosDePrueba.CDC_FACTURA_SIMPLE, nc.getgCamDEAsocList().get(0).getdCdCDERef(), "la nota queda sin tocar");
    }

    /** Factura asociada nunca reemitida: se omite sin consultar a SIFEN. */
    @Test
    public void notaSinSuFacturaReemitidaSeOmiteSinConsultar() throws Exception {
        ReemisionPrueba r = new ReemisionPrueba(emisor(), 50, dir.resolve("t.csv"));
        DocumentoElectronico nc = produccion(CasosDePrueba.documentos().get(20).construir());
        Assertions.assertEquals(Collections.singletonList(null), r.cdcAsociadosReemitidos(nc));
        String motivo = r.motivoParaNoEnviarNota(nc, cdc -> {
            throw new AssertionError("no debe consultar");
        });
        Assertions.assertTrue(motivo.contains("no fue reemitida"), motivo);
        Assertions.assertEquals(50, r.getSiguiente());
        // una factura sin asociados no tiene impedimento
        Assertions.assertNull(r.motivoParaNoEnviarNota(produccion(CasosDePrueba.documentos().get(4).construir()), cdc -> false));
    }
}
