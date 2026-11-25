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

import org.javabeanstack.sifen.Sifen;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.beans.response.RespuestaConsultaDE;
import org.javabeanstack.sifen.core.beans.response.RespuestaConsultaLoteDE;
import org.javabeanstack.sifen.core.beans.response.RespuestaRecepcionDE;
import org.javabeanstack.sifen.core.beans.response.RespuestaRecepcionLoteDE;
import org.javabeanstack.sifen.core.fields.response.batch.TgResProcLote;
import org.javabeanstack.sifen.core.types.TTiDE;
import org.javabeanstack.sifen.test.SifenTestEnv;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Prueba en vivo encadenada contra el ambiente de <b>pruebas</b> de SIFEN (plan JBSSIF, §2.9; I1-08).
 * Nunca producción: {@link SifenTestEnv} corta la corrida si {@code SIFEN_AMBIENTE} no es DEV (D28).
 *
 * <p>Los DE <b>no</b> salen del catálogo sintético sino de ventas reales de {@code maker950test8}
 * extraídas <b>fuera del módulo</b> (RNF3) como {@code venta-<iditemmovimiento>.xml} ({@code rDE} sin
 * firma, con la numeración original) en la carpeta de {@value #PROP_VENTAS}. Cada venta se reemite
 * con {@link ReemisionPrueba} (emisor, timbrado, 001-001 y número ≥ 1000 del {@code .env} (ESC-8); tabla de
 * equivalencias persistente, el número se reserva antes de llamar a SIFEN). La consulta del RUC
 * propio, que no necesita ventas, está en {@link SifenVivoRucTest}. Orden: la primera factura por
 * {@code recepcionDE} → {@code consultaDE} de ese CDC → el resto de las facturas en un lote →
 * {@code consultaLoteDE} de ese lote → notas de crédito/débito por {@code recepcionDE}, asociadas al
 * CDC reemitido de su factura y solo si esa factura quedó <b>aprobada</b> (M2-05: si no, la nota se
 * omite sin reservar número). Antes de cualquier llamada, todas las ventas pasan por la guarda D28 de
 * la reemisión ({@link ReemisionPrueba#verificarAntesDeReemitir}). Todo se consulta en la misma corrida; nada depende de CDC o lotes
 * fijos.</p>
 *
 * <p>Pedidos y respuestas brutas van a {@value #PROP_RESPUESTAS} (por omisión la carpeta
 * {@code respuestas} junto a la de ventas): traen datos de clientes reales y no se copian al repo
 * sin anonimizar. Nada de esto se loguea con la configuración (M1-03).</p>
 *
 * <p>Comando (ver el plan, §2.9): {@code mvn -s maven-settings.xml -pl sifen -am test -Psifen-live
 * -Djbs.sifen.test.env=<sifen-credentials.env> -Djbs.sifen.test.ventas=<…/test8/ventas>}.</p>
 */
@Tag("sifen-live")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SifenVivoTest {
    /** Carpeta con los {@code venta-*.xml}. */
    public static final String PROP_VENTAS = "jbs.sifen.test.ventas";
    /** Carpeta de pedidos y respuestas brutas. */
    public static final String PROP_RESPUESTAS = "jbs.sifen.test.respuestas";
    /** Tabla de equivalencias de numeración. */
    public static final String PROP_EQUIVALENCIAS = "jbs.sifen.test.equivalencias";
    /** Consultas del lote antes de darlo por en proceso (cada 10 s). */
    public static final String PROP_CONSULTAS_LOTE = "jbs.sifen.test.lote.consultas";

    private static final Logger LOGGER = LogManager.getLogger(SifenVivoTest.class);

    private static SifenConfig cfg;
    private static SifenTestEnv.Emisor emisor;
    private static ReemisionPrueba reemision;
    private static Path respuestas;
    private static final List<Path> facturas = new ArrayList<>();
    private static final List<Path> notas = new ArrayList<>();
    private static String cdcSincrono;
    /** CDC reemitidos que quedaron aprobados en esta corrida (recepcionDE y consultaLoteDE; M2-05). */
    private static final Set<String> aprobados = new HashSet<>();
    private static String lote;

    @BeforeEach
    public void preparar() throws Exception {
        SifenTestEnv.Entorno entorno = SifenTestEnv.entornoOSaltar();
        if (cfg != null) {
            return;
        }
        emisor = SifenTestEnv.emisorOSaltar();
        // I2-08: sin SIFEN_EMISOR_EMAIL no se reemite; se salta antes de crear carpetas o tocar la tabla.
        SifenTestEnv.emailParaReemitirOSaltar(emisor);
        String ventas = System.getProperty(PROP_VENTAS);
        Assumptions.assumeTrue(ventas != null && Files.isDirectory(Paths.get(ventas)),
                "Sin carpeta de ventas extraídas (" + PROP_VENTAS + ")");
        Path dirVentas = Paths.get(ventas);
        respuestas = Paths.get(System.getProperty(PROP_RESPUESTAS, dirVentas.resolveSibling("respuestas").toString()));
        Files.createDirectories(respuestas);
        Path tabla = Paths.get(System.getProperty(PROP_EQUIVALENCIAS, dirVentas.resolveSibling("equivalencias_numeracion.csv").toString()));
        reemision = new ReemisionPrueba(emisor, entorno.getNumeroInicial(), tabla);
        facturas.clear();
        notas.clear();
        aprobados.clear();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dirVentas, "venta-*.xml")) {
            List<Path> todas = new ArrayList<>();
            ds.forEach(todas::add);
            todas.sort(null);
            for (Path p : todas) {
                DocumentoElectronico de = leer(p);
                // Guarda D28 sobre TODAS las ventas antes de la primera llamada a SIFEN: ningún DE
                // reemitido con el timbrado o el establecimiento-punto de producción; corta, no salta.
                reemision.verificarAntesDeReemitir(de);
                TTiDE tipo = de.getgTimb().getiTiDE();
                (tipo == TTiDE.FACTURA_ELECTRONICA ? facturas : notas).add(p);
            }
        }
        Assumptions.assumeFalse(facturas.isEmpty(), "No hay facturas en " + dirVentas);
        cfg = entorno.getConfiguracion();
        Sifen.setSifenConfig(cfg);
        LOGGER.info("Ambiente " + cfg.getEnvironment() + "; " + facturas.size() + " facturas y " + notas.size()
                + " notas; primer número " + reemision.getSiguiente());
    }

    private static DocumentoElectronico leer(Path venta) throws Exception {
        return new DocumentoElectronico(new String(Files.readAllBytes(venta), StandardCharsets.UTF_8));
    }

    private static String idVenta(Path venta) {
        String n = venta.getFileName().toString();
        return n.substring("venta-".length(), n.length() - ".xml".length());
    }

    private static LocalDateTime ahora() {
        return LocalDateTime.now().minusMinutes(1).truncatedTo(ChronoUnit.SECONDS);
    }

    private static void guardar(String nombre, String contenido) throws Exception {
        if (contenido != null) {
            Files.write(respuestas.resolve(nombre), contenido.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Envía un DE por {@code recepcionDE} y registra el resultado en la tabla aunque la llamada
     * lance (con la clase de la excepción): el número ya quedó reservado.
     */
    private static RespuestaRecepcionDE enviar(String idVenta, DocumentoElectronico de, ReemisionPrueba.Reemision re) throws Exception {
        RespuestaRecepcionDE r = null;
        String error = null;
        try {
            r = Sifen.recepcionDE(de, cfg);
            return r;
        } catch (Exception | Error e) {
            error = e.getClass().getName();
            throw e;
        } finally {
            if (r == null) {
                reemision.registrar(idVenta, re, "recepcionDE:EXCEPCION", 0, null, null, error);
            } else {
                String estado = r.getxProtDE() != null ? r.getxProtDE().getdEstRes() : null;
                reemision.registrar(idVenta, re, "recepcionDE", r.getCodigoEstado(), codigo(r), estado, mensaje(r));
                guardar("recepcion-de-" + re.cdc + ".pedido.xml", r.getRequestSent());
                guardar("recepcion-de-" + re.cdc + ".xml", r.getRespuestaBruta());
            }
        }
    }

    private static String codigo(RespuestaRecepcionDE r) {
        return r.getxProtDE() != null && !r.getxProtDE().getgResProc().isEmpty() ? r.getxProtDE().getgResProc().get(0).getdCodRes() : r.getdCodRes();
    }

    private static String mensaje(RespuestaRecepcionDE r) {
        return r.getxProtDE() != null && !r.getxProtDE().getgResProc().isEmpty() ? r.getxProtDE().getgResProc().get(0).getdMsgRes() : r.getdMsgRes();
    }

    @Test
    @Order(2)
    public void recepcionDeDeLaPrimeraFactura() throws Exception {
        Path venta = facturas.get(0);
        DocumentoElectronico de = leer(venta);
        ReemisionPrueba.Reemision re = reemision.reemitir(idVenta(venta), de, ahora());
        RespuestaRecepcionDE r = enviar(idVenta(venta), de, re);
        String estado = r.getxProtDE() != null ? r.getxProtDE().getdEstRes() : null;
        if (r.getCodigoEstado() == 200 && estado != null && estado.startsWith("Aprobado")) {
            aprobados.add(re.cdc);
        }
        Assertions.assertEquals(200, r.getCodigoEstado());
        Assertions.assertEquals("Aprobado", estado, "recepcionDE " + re.enviado + ": " + codigo(r) + " " + mensaje(r));
        cdcSincrono = re.cdc;
    }

    @Test
    @Order(3)
    public void consultaDeDelCdcRecibido() throws Exception {
        Assumptions.assumeTrue(cdcSincrono != null, "La recepción sincrónica no quedó aprobada");
        RespuestaConsultaDE r = Sifen.consultaDE(cdcSincrono, cfg);
        guardar("consulta-de-" + cdcSincrono + ".xml", r.getRespuestaBruta());
        Assertions.assertEquals(200, r.getCodigoEstado());
        Assertions.assertEquals("0422", r.getdCodRes(), "consultaDE: " + r.getdMsgRes());
    }

    @Test
    @Order(4)
    public void recepcionLoteDelRestoDeLasFacturas() throws Exception {
        Assumptions.assumeTrue(facturas.size() > 1, "Una sola factura: no hay lote");
        List<DocumentoElectronico> des = new ArrayList<>();
        List<ReemisionPrueba.Reemision> res = new ArrayList<>();
        for (Path venta : facturas.subList(1, facturas.size())) {
            DocumentoElectronico de = leer(venta);
            res.add(reemision.reemitir(idVenta(venta), de, ahora()));
            des.add(de);
        }
        RespuestaRecepcionLoteDE r = null;
        String error = null;
        try {
            r = Sifen.recepcionLoteDE(des, cfg);
        } catch (Exception | Error e) {
            error = e.getClass().getName();
            throw e;
        } finally {
            for (int i = 0; i < des.size(); i++) {
                if (r == null) {
                    reemision.registrar(idVenta(facturas.get(i + 1)), res.get(i), "recepcionLoteDE:EXCEPCION", 0, null, null, error);
                } else {
                    reemision.registrar(idVenta(facturas.get(i + 1)), res.get(i), "recepcionLoteDE:" + r.getdProtConsLote(),
                            r.getCodigoEstado(), r.getdCodRes(), null, r.getdMsgRes());
                }
            }
        }
        guardar("recepcion-lote-" + r.getdProtConsLote() + ".xml", r.getRespuestaBruta());
        Assertions.assertEquals(200, r.getCodigoEstado());
        Assertions.assertEquals("0300", r.getdCodRes(), "recepcionLoteDE: " + r.getdMsgRes());
        lote = r.getdProtConsLote();
    }

    @Test
    @Order(5)
    public void consultaLoteDelLoteRecibido() throws Exception {
        Assumptions.assumeTrue(lote != null, "No hay lote recibido en esta corrida");
        int intentos = Integer.parseInt(System.getProperty(PROP_CONSULTAS_LOTE, "6"));
        RespuestaConsultaLoteDE r = null;
        for (int i = 0; i < intentos; i++) {
            Thread.sleep(10_000L);
            r = Sifen.consultaLoteDE(lote, cfg);
            if (!"0361".equals(r.getdCodResLot())) {
                break;
            }
        }
        guardar("consulta-lote-" + lote + ".xml", r.getRespuestaBruta());
        // M2-05: los aprobados del lote se anotan antes de las aserciones (el id de gResProcLote es el CDC).
        if (r.getCodigoEstado() == 200) {
            for (TgResProcLote p : r.getgResProcLoteList()) {
                if (p.getId() != null && p.getdEstRes() != null && p.getdEstRes().startsWith("Aprobado")) {
                    aprobados.add(p.getId());
                }
            }
        }
        Assertions.assertEquals(200, r.getCodigoEstado());
        Assertions.assertEquals("0362", r.getdCodResLot(), "consultaLoteDE: " + r.getdMsgResLot());
    }

    /**
     * M2-05: un CDC reemitido está aprobado si lo aprobó esta corrida o, para facturas de corridas
     * anteriores, si {@code consultaDE} lo encuentra (0422) con protocolo de autorización. No reserva
     * números; una excepción cuenta como no aprobado.
     */
    private static boolean aprobadoEnPruebas(String cdc) {
        if (aprobados.contains(cdc)) {
            return true;
        }
        try {
            RespuestaConsultaDE r = Sifen.consultaDE(cdc, cfg);
            guardar("consulta-de-" + cdc + ".xml", r.getRespuestaBruta());
            boolean ok = r.getCodigoEstado() == 200 && "0422".equals(r.getdCodRes())
                    && r.getxContenDE() != null && r.getxContenDE().getdProtAut() != null;
            if (ok) {
                aprobados.add(cdc);
            }
            return ok;
        } catch (Exception e) {
            LOGGER.warn("consultaDE de la factura asociada falló: " + e.getClass().getName());
            return false;
        }
    }

    @Test
    @Order(6)
    public void recepcionDeNotasAsociadas() throws Exception {
        Assumptions.assumeFalse(notas.isEmpty(), "No hay notas de crédito/débito");
        // M2-05: antes de reservar número, solo las notas cuya factura asociada quedó aprobada.
        List<Path> enviables = new ArrayList<>();
        List<String> omitidas = new ArrayList<>();
        for (Path venta : notas) {
            String motivo = reemision.motivoParaNoEnviarNota(leer(venta), SifenVivoTest::aprobadoEnPruebas);
            if (motivo == null) {
                enviables.add(venta);
            } else {
                omitidas.add("venta " + idVenta(venta) + ": " + motivo);
            }
        }
        Assumptions.assumeTrue(!enviables.isEmpty(), "Ninguna nota se envía (M2-05, sin reservar número): " + omitidas);
        List<String> fallas = new ArrayList<>();
        for (Path venta : enviables) {
            DocumentoElectronico de = leer(venta);
            ReemisionPrueba.Reemision re = reemision.reemitir(idVenta(venta), de, ahora());
            RespuestaRecepcionDE r = enviar(idVenta(venta), de, re);
            String estado = r.getxProtDE() != null ? r.getxProtDE().getdEstRes() : null;
            if (!"Aprobado".equals(estado)) {
                fallas.add(re.enviado + ": " + codigo(r) + " " + mensaje(r));
            }
        }
        Assertions.assertTrue(fallas.isEmpty(), "Notas no aprobadas: " + fallas);
        Assumptions.assumeTrue(omitidas.isEmpty(), "Notas sin enviar (M2-05, sin reservar número): " + omitidas);
    }
}
