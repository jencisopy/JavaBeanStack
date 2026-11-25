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
package org.javabeanstack.sifen.internal.request;

import jakarta.xml.soap.SOAPMessage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import javax.xml.namespace.QName;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.response.RespuestaConsultaRUC;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.internal.Constants;
import org.javabeanstack.sifen.internal.SOAPResponse;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.internal.helpers.SoapHelper;
import org.javabeanstack.sifen.internal.http.ExchangeTrace;
import org.javabeanstack.sifen.internal.http.OperationContext;
import org.javabeanstack.sifen.internal.http.ServidoresFase5;
import org.javabeanstack.sifen.internal.http.ServidoresFase5.Accion;
import org.javabeanstack.sifen.internal.http.SifenConnection;
import org.javabeanstack.sifen.internal.response.BaseResponse;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Reintentos de las consultas y nada más (plan JBSSIF, §5.2; D41, CA5-05), id de operación en las
 * líneas del transporte (I10-09), XML del pedido solo a DEBUG (§5.4, M0-03, D27) y traza activable en
 * caliente (§5.4, D27). Contra un servidor TCP local con fallas programadas ({@code http://}: las
 * peticiones se arman directo, sin la validación de la configuración que exige {@code https}).
 */
public class ConsultaReintentosTest {
    private static final List<LogEvent> LOG = Collections.synchronizedList(new ArrayList<>());
    private static final String[] LOGGERS = {"org.javabeanstack.sifen.internal.request.BaseRequest",
        "org.javabeanstack.sifen.internal.http.UrlSifenConnection", ExchangeTrace.TRACE_LOGGER,
        "org.javabeanstack.sifen.internal.http.ExchangeTrace"};
    private static AbstractAppender captura;

    @TempDir
    Path tmp;

    /** Petición que no se reintenta, como una recepción (la de por omisión de {@code BaseRequest}). */
    static final class RecepcionDePrueba extends BaseRequest {
        RecepcionDePrueba(SifenConfig cfg) {
            super(7, cfg);
        }

        @Override
        SOAPMessage setupSoapMessage(GenerationCtx generationCtx) throws SifenException {
            try {
                SOAPMessage m = SoapHelper.createSoapMessage();
                m.getSOAPBody().addBodyElement(new QName(Constants.SIFEN_NS_URI, "rEnviDe")).addChildElement("dId")
                        .setTextContent("7");
                return m;
            } catch (jakarta.xml.soap.SOAPException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        BaseResponse processResponse(SOAPResponse soapResponse) {
            return new RespuestaConsultaRUC();
        }
    }

    @BeforeAll
    public static void capturar() {
        LoggerContext contexto = (LoggerContext) LogManager.getContext(false);
        captura = new AbstractAppender("fase5-reintentos", null, null, true, Property.EMPTY_ARRAY) {
            @Override
            public void append(LogEvent event) {
                LOG.add(event.toImmutable());
            }
        };
        captura.start();
        for (String nombre : LOGGERS) {
            LoggerConfig propio = LoggerConfig.newBuilder().withLoggerName(nombre).withLevel(Level.INFO)
                    .withAdditivity(false).withConfig(contexto.getConfiguration()).build();
            propio.addAppender(captura, Level.ALL, null);
            contexto.getConfiguration().addLogger(nombre, propio);
        }
        contexto.updateLoggers();
    }

    @AfterAll
    public static void soltar() {
        LoggerContext contexto = (LoggerContext) LogManager.getContext(false);
        for (String nombre : LOGGERS) {
            contexto.getConfiguration().removeLogger(nombre);
        }
        contexto.updateLoggers();
        captura.stop();
    }

    @BeforeEach
    public void limpiar() {
        LOG.clear();
    }

    @AfterEach
    public void sinPropiedades() {
        System.clearProperty(SifenConfig.TRACE_ENABLED_KEY);
        System.clearProperty(SifenConfig.TRACE_FOLDER_KEY);
        System.clearProperty(SifenConfig.TRACE_RETENTION_KEY);
    }

    private static SifenConfig cfg(ServidoresFase5.Crudo crudo) {
        SifenConfig c = FixturesSifen.configuracion();
        c.setBaseUrl("http://127.0.0.1:" + crudo.puerto());
        c.setRetryBackoffMs(10);
        return c;
    }

    private static RespuestaConsultaRUC consulta(SifenConfig c, SifenConnection conexion) throws SifenException {
        ReqConsRuc r = new ReqConsRuc(1, c);
        r.setdRUCCons("80123456");
        return (RespuestaConsultaRUC) r.makeRequest(GenerationCtx.getDefaultFromConfig(c), c.getQueryRucPath(), conexion);
    }

    private static ServidoresFase5.Crudo crudo(java.util.function.IntFunction<Accion> plan) throws Exception {
        return new ServidoresFase5.Crudo(plan, ServidoresFase5.http200("consulta-ruc-0502.xml"));
    }

    // ------------------------------------------------------------------ reintentos (D41)

    @Test
    public void laConsultaSeReintentaTrasUnCorteYSeRecupera() throws Exception {
        try (ServidoresFase5.Crudo s = crudo(n -> n <= 2 ? Accion.CERRAR : Accion.RESPONDER)) {
            SifenConfig c = cfg(s);
            c.setMaxRetries(2);
            Assertions.assertEquals("0502", consulta(c, SifenConnection.create()).getdCodRes());
            Assertions.assertEquals(3, s.conexiones());
            Assertions.assertEquals(2, LOG.stream().filter(e -> e.getMessage().getFormattedMessage().contains("reintento")).count());
        }
    }

    @Test
    public void laConsultaSeReintentaTrasUn503() throws Exception {
        try (ServidoresFase5.Crudo s = crudo(n -> n == 1 ? Accion.RESPONDER_503 : Accion.RESPONDER)) {
            SifenConfig c = cfg(s);
            c.setMaxRetries(1);
            Assertions.assertEquals("0502", consulta(c, null).getdCodRes());
            Assertions.assertEquals(2, s.conexiones());
        }
    }

    @Test
    public void sinReintentosPorOmision() throws Exception {
        try (ServidoresFase5.Crudo s = crudo(n -> n == 1 ? Accion.CERRAR : Accion.RESPONDER)) {
            SifenConfig c = cfg(s);
            Assertions.assertEquals(0, c.getMaxRetries());
            SifenException e = Assertions.assertThrows(SifenException.class, () -> consulta(c, SifenConnection.create()));
            Assertions.assertEquals("JBS-CONEX-07", e.getCode());
            Assertions.assertEquals(1, s.conexiones());
        }
    }

    @Test
    public void unaRecepcionNuncaSeReintentaD41() throws Exception {
        try (ServidoresFase5.Crudo s = crudo(n -> n == 1 ? Accion.CERRAR : Accion.RESPONDER)) {
            SifenConfig c = cfg(s);
            c.setMaxRetries(5);
            SifenException e = Assertions.assertThrows(SifenException.class,
                    () -> new RecepcionDePrueba(c).makeRequest(null, "/de/ws/sync/recibe.wsdl", SifenConnection.create()));
            Assertions.assertEquals("JBS-CONEX-07", e.getCode());
            Assertions.assertTrue(e.withContext(org.javabeanstack.sifen.core.exceptions.Operation.SEND_DE, null, "x")
                    .isOutcomeUnknown());
            Thread.sleep(200);
            Assertions.assertEquals(1, s.conexiones(), "ni la librería ni el JDK reenvían una recepción (D41, E5-01)");
        }
    }

    @Test
    public void unTiempoAgotadoNoSeReintenta() throws Exception {
        try (ServidoresFase5.Crudo s = crudo(n -> Accion.TARDAR)) {
            s.tardanza(1000);
            SifenConfig c = cfg(s);
            c.setMaxRetries(3);
            c.setHttpReadTimeout(150);
            SifenException e = Assertions.assertThrows(SifenException.class, () -> consulta(c, null));
            Assertions.assertEquals("JBS-CONEX-05", e.getCode());
            Assertions.assertEquals(1, s.conexiones());
        }
    }

    @Test
    public void unaRedireccionNoSeReintenta() throws Exception {
        try (ServidoresFase5.Crudo s = crudo(n -> n == 1 ? Accion.REDIRIGIR_302 : Accion.RESPONDER)) {
            SifenConfig c = cfg(s);
            c.setMaxRetries(3);
            SifenException e = Assertions.assertThrows(SifenException.class, () -> consulta(c, null));
            Assertions.assertEquals("JBS-CONEX-10", e.getCode());
            Assertions.assertEquals(1, s.conexiones());
        }
    }

    @Test
    public void lasConsultasSonReintentablesYLaBaseNo() {
        SifenConfig c = FixturesSifen.configuracion();
        Assertions.assertTrue(new ReqConsRuc(1, c).isRetryable());
        Assertions.assertTrue(new ReqConsDe(1, c).isRetryable());
        Assertions.assertTrue(new ReqConsLoteDe(1, c).isRetryable());
        Assertions.assertFalse(new RecepcionDePrueba(c).isRetryable());
        Assertions.assertFalse(new ReqRecDe(1, c).isRetryable());
        Assertions.assertFalse(new ReqRecLoteDe(1, c).isRetryable());
        Assertions.assertFalse(new ReqRecEventoDe(1, c).isRetryable());
    }

    @Test
    public void unHiloInterrumpidoNoSigueReintentando() throws Exception {
        try (ServidoresFase5.Crudo s = crudo(n -> Accion.CERRAR)) {
            SifenConfig c = cfg(s);
            c.setMaxRetries(5);
            c.setRetryBackoffMs(5000);
            Thread.currentThread().interrupt();
            try {
                Assertions.assertThrows(SifenException.class, () -> consulta(c, null));
                Assertions.assertTrue(Thread.currentThread().isInterrupted(), "la marca se conserva");
            } finally {
                Thread.interrupted();
            }
            Assertions.assertEquals(1, s.conexiones());
        }
    }

    // ------------------------------------------------------------------ log (I10-09, M0-03)

    @Test
    public void lasLineasLlevanElIdDeOperacionYElXmlNoVaAInfo() throws Exception {
        try (ServidoresFase5.Crudo s = crudo(n -> Accion.RESPONDER);
                OperationContext.Scope scope = OperationContext.open("abcd1234")) {
            consulta(cfg(s), null);
        }
        Assertions.assertFalse(LOG.isEmpty());
        for (LogEvent e : LOG) {
            String m = e.getMessage().getFormattedMessage();
            Assertions.assertTrue(m.startsWith("[abcd1234]"), m);
            Assertions.assertFalse(m.contains("dRUCCons") || m.contains("Envelope"), "XML a " + e.getLevel() + ": " + m);
        }
        Assertions.assertNull(OperationContext.current(), "el ámbito se cerró");
    }

    @Test
    public void elIdDeLaPeticionTienePrioridad() throws Exception {
        try (ServidoresFase5.Crudo s = crudo(n -> Accion.RESPONDER);
                OperationContext.Scope scope = OperationContext.open("ffff0000")) {
            SifenConfig c = cfg(s);
            ReqConsRuc r = new ReqConsRuc(1, c);
            r.setdRUCCons("80123456");
            r.setOperationId("1234abcd");
            r.makeRequest(GenerationCtx.getDefaultFromConfig(c), c.getQueryRucPath());
        }
        Assertions.assertTrue(LOG.stream().anyMatch(e -> e.getMessage().getFormattedMessage().startsWith("[1234abcd]")));
    }

    // ------------------------------------------------------------------ traza (D27)

    private List<Path> archivos(Path dir) throws Exception {
        if (!Files.exists(dir)) {
            return List.of();
        }
        try (Stream<Path> s = Files.list(dir)) {
            return s.sorted().toList();
        }
    }

    @Test
    public void trazaApagadaNoEscribeYEncendidaEnCalienteSi() throws Exception {
        Path dir = tmp.resolve("traza");
        try (ServidoresFase5.Crudo s = crudo(n -> Accion.RESPONDER)) {
            SifenConfig c = cfg(s);
            c.setTraceFolder(dir.toString());
            consulta(c, null);
            Assertions.assertTrue(archivos(dir).isEmpty(), "apagada no escribe nada");
            c.setTraceEnabled(true); // la misma instancia, sin recrear la configuración
            consulta(c, null);
            List<Path> a = archivos(dir);
            Assertions.assertEquals(2, a.size(), a.toString());
            Path pedido = a.stream().filter(p -> p.toString().endsWith("_pedido.xml")).findFirst().orElseThrow();
            Path respuesta = a.stream().filter(p -> p.toString().endsWith("_respuesta.xml")).findFirst().orElseThrow();
            Assertions.assertTrue(pedido.getFileName().toString().contains("80123456"), "referencia en el nombre: " + pedido);
            Assertions.assertTrue(Files.readString(pedido, StandardCharsets.UTF_8).contains("<dRUCCons>80123456</dRUCCons>"));
            Assertions.assertTrue(Files.readString(respuesta, StandardCharsets.UTF_8).contains("0502"));
        }
    }

    @Test
    public void trazaPorPropiedadDelSistemaParaUnaCopiaDeLaConfiguracion() throws Exception {
        Path dir = tmp.resolve("traza-sistema");
        try (ServidoresFase5.Crudo s = crudo(n -> Accion.RESPONDER)) {
            SifenConfig c = cfg(s).copy(); // como la copia de un SifenClient: el setter de la original no le llega
            consulta(c, null);
            Assertions.assertTrue(archivos(dir).isEmpty());
            System.setProperty(SifenConfig.TRACE_ENABLED_KEY, "true");
            System.setProperty(SifenConfig.TRACE_FOLDER_KEY, dir.toString());
            consulta(c, null);
            Assertions.assertEquals(2, archivos(dir).size());
            System.setProperty(SifenConfig.TRACE_ENABLED_KEY, "false");
            consulta(c, null);
            Assertions.assertEquals(2, archivos(dir).size(), "apagada de nuevo en caliente");
        }
    }

    @Test
    public void trazaDeUnaFallaEscribePedidoYError() throws Exception {
        Path dir = tmp.resolve("traza-falla");
        try (ServidoresFase5.Crudo s = crudo(n -> Accion.CERRAR)) {
            SifenConfig c = cfg(s);
            c.setTraceEnabled(true);
            c.setTraceFolder(dir.toString());
            Assertions.assertThrows(SifenException.class, () -> consulta(c, null));
            List<Path> a = archivos(dir);
            Assertions.assertEquals(2, a.size(), a.toString());
            Path error = a.stream().filter(p -> p.toString().endsWith("_error.txt")).findFirst().orElseThrow();
            Assertions.assertTrue(Files.readString(error).contains("JBS-CONEX-07"));
        }
    }

    private static boolean posix(Path p) {
        return p.getFileSystem().supportedFileAttributeViews().contains("posix");
    }

    private static List<String> avisos() {
        return LOG.stream().filter(e -> e.getLoggerName().endsWith(".ExchangeTrace") && e.getLevel() == Level.WARN)
                .map(e -> e.getMessage().getFormattedMessage()).toList();
    }

    /** I5-07: carpeta 700, archivos 600 y un solo WARN al encenderse, que dice la vía y el destino. */
    @Test
    public void trazaConPermisosRestringidosYUnAvisoAlEncenderse() throws Exception {
        Path dir = tmp.resolve("traza-permisos");
        try (ServidoresFase5.Crudo s = crudo(n -> Accion.RESPONDER)) {
            SifenConfig c = cfg(s);
            c.setTraceEnabled(true);
            c.setTraceFolder(dir.toString());
            SifenConnection conexion = SifenConnection.create();
            consulta(c, conexion);
            consulta(c, conexion);
            Assertions.assertEquals(4, archivos(dir).size());
            if (posix(dir)) {
                Assertions.assertEquals("rwx------", java.nio.file.attribute.PosixFilePermissions.toString(
                        Files.getPosixFilePermissions(dir)));
                for (Path f : archivos(dir)) {
                    Assertions.assertEquals("rw-------", java.nio.file.attribute.PosixFilePermissions.toString(
                            Files.getPosixFilePermissions(f)), f.toString());
                }
            }
            List<String> a = avisos();
            Assertions.assertEquals(1, a.size(), "un aviso por encendido: " + a);
            Assertions.assertTrue(a.get(0).contains("por la configuración de esta empresa") && a.get(0).contains(dir.toString())
                    && a.get(0).contains("7 días"), a.get(0));
            Assertions.assertFalse(a.get(0).contains("dRUCCons"), "el aviso no lleva datos");
            c.setTraceEnabled(false);
            consulta(c, conexion);
            c.setTraceEnabled(true);
            consulta(c, conexion);
            Assertions.assertEquals(2, avisos().size(), "apagada y encendida otra vez: avisa de nuevo");
        }
    }

    @Test
    public void elAvisoDeLaPropiedadDelSistemaDiceQueAlcanzaATodas() throws Exception {
        try (ServidoresFase5.Crudo s = crudo(n -> Accion.RESPONDER)) {
            System.setProperty(SifenConfig.TRACE_ENABLED_KEY, "true");
            System.setProperty(SifenConfig.TRACE_FOLDER_KEY, tmp.resolve("traza-global").toString());
            consulta(cfg(s), null);
        }
        Assertions.assertTrue(avisos().stream().anyMatch(a -> a.contains("todas las empresas de la JVM")), avisos().toString());
    }

    /** I5-10: la configuración que apaga la traza gana sobre la propiedad del sistema que la enciende. */
    @Test
    public void laConfiguracionQueApagaGanaALaPropiedadDelSistema() throws Exception {
        Path dir = tmp.resolve("traza-precedencia-activa");
        try (ServidoresFase5.Crudo s = crudo(n -> Accion.RESPONDER)) {
            System.setProperty(SifenConfig.TRACE_ENABLED_KEY, "true");
            System.setProperty(SifenConfig.TRACE_FOLDER_KEY, dir.toString());
            SifenConfig apagada = cfg(s);
            apagada.setTraceEnabled(false);
            consulta(apagada, null);
            Assertions.assertTrue(archivos(dir).isEmpty(), "la empresa que la apaga no escribe");
            consulta(cfg(s), null);
            Assertions.assertEquals(2, archivos(dir).size(), "la que no la define sigue la propiedad del sistema");
        }
    }

    /** I5-10: carpeta y retención de la configuración ganan sobre las de la propiedad del sistema. */
    @Test
    public void carpetaYRetencionDeLaConfiguracionGanan() throws Exception {
        Path global = tmp.resolve("traza-global-precedencia");
        Path propia = Files.createDirectories(tmp.resolve("traza-propia-precedencia"));
        java.nio.file.attribute.FileTime vieja = java.nio.file.attribute.FileTime.fromMillis(
                System.currentTimeMillis() - java.time.Duration.ofDays(3).toMillis());
        Path viejo = Files.writeString(propia.resolve("20261120-101010.123_1a2b_abcd1234_80123456_pedido.xml"), "x");
        Files.setLastModifiedTime(viejo, vieja);
        try (ServidoresFase5.Crudo s = crudo(n -> Accion.RESPONDER)) {
            System.setProperty(SifenConfig.TRACE_FOLDER_KEY, global.toString());
            System.setProperty(SifenConfig.TRACE_RETENTION_KEY, "1");
            SifenConfig c = cfg(s);
            c.setTraceEnabled(true);
            c.setTraceFolder(propia.toString());
            c.setTraceRetentionDays(30);
            consulta(c, null);
        }
        Assertions.assertFalse(Files.exists(global), "la carpeta de la propiedad del sistema no se usa");
        Assertions.assertTrue(Files.exists(viejo), "rige la retención de 30 días de la configuración, no la de 1 día");
        Assertions.assertEquals(3, archivos(propia).size());
    }

    @Test
    public void sinRetencionEnLaConfiguracionRigeLaDelSistema() throws Exception {
        Path dir = Files.createDirectories(tmp.resolve("traza-retencion-sistema"));
        Path viejo = Files.writeString(dir.resolve("20261120-101010.123_1a2b_abcd1234_80123456_pedido.xml"), "x");
        Files.setLastModifiedTime(viejo, java.nio.file.attribute.FileTime.fromMillis(
                System.currentTimeMillis() - java.time.Duration.ofDays(3).toMillis()));
        try (ServidoresFase5.Crudo s = crudo(n -> Accion.RESPONDER)) {
            System.setProperty(SifenConfig.TRACE_RETENTION_KEY, "1");
            SifenConfig c = cfg(s);
            c.setTraceEnabled(true);
            c.setTraceFolder(dir.toString());
            consulta(c, null);
        }
        Assertions.assertFalse(Files.exists(viejo), "sin valor propio rige la propiedad del sistema (1 día)");
    }

    @Test
    public void unaCarpetaQueYaExistiaYEsAbiertaSeAvisaYNoSeToca() throws Exception {
        Path dir = Files.createDirectory(tmp.resolve("traza-abierta"));
        org.junit.jupiter.api.Assumptions.assumeTrue(posix(dir));
        Files.setPosixFilePermissions(dir, java.nio.file.attribute.PosixFilePermissions.fromString("rwxr-xr-x"));
        try (ServidoresFase5.Crudo s = crudo(n -> Accion.RESPONDER)) {
            SifenConfig c = cfg(s);
            c.setTraceEnabled(true);
            c.setTraceFolder(dir.toString());
            consulta(c, null);
        }
        Assertions.assertEquals("rwxr-xr-x", java.nio.file.attribute.PosixFilePermissions.toString(Files.getPosixFilePermissions(dir)));
        Assertions.assertTrue(avisos().stream().anyMatch(a -> a.contains("ATENCIÓN") && a.contains("rwxr-xr-x")), avisos().toString());
    }

    /** I5-07: la retención borra solo los archivos propios más viejos que el plazo. */
    @Test
    public void laRetencionBorraSoloLosPropiosViejos() throws Exception {
        Path dir = tmp.resolve("traza-retencion");
        Files.createDirectories(dir);
        java.nio.file.attribute.FileTime vieja = java.nio.file.attribute.FileTime.fromMillis(
                System.currentTimeMillis() - java.time.Duration.ofDays(3).toMillis());
        Path propioViejo = Files.writeString(dir.resolve("20261120-101010.123_1a2b_abcd1234_80123456_pedido.xml"), "x");
        Path propioNuevo = Files.writeString(dir.resolve("20261124-101010.123_1a2c_abcd1234_80123456_respuesta.xml"), "x");
        Path ajeno = Files.writeString(dir.resolve("notas.txt"), "x");
        Files.setLastModifiedTime(propioViejo, vieja);
        Files.setLastModifiedTime(ajeno, vieja);
        try (ServidoresFase5.Crudo s = crudo(n -> Accion.RESPONDER)) {
            SifenConfig c = cfg(s);
            c.setTraceEnabled(true);
            c.setTraceFolder(dir.toString());
            c.setTraceRetentionDays(2);
            consulta(c, null);
        }
        Assertions.assertFalse(Files.exists(propioViejo), "propio de hace 3 días con retención de 2: se borra");
        Assertions.assertTrue(Files.exists(propioNuevo));
        Assertions.assertTrue(Files.exists(ajeno), "un archivo que la traza no nombró no se toca");
        Assertions.assertEquals(4, archivos(dir).size(), "propio nuevo + ajeno + pedido y respuesta de esta consulta");
    }

    @Test
    public void conRetencionCeroNoSeBorraNada() throws Exception {
        Path dir = tmp.resolve("traza-sin-retencion");
        Files.createDirectories(dir);
        Path viejo = Files.writeString(dir.resolve("20200101-000000.000_1_-_sin-referencia_error.txt"), "x");
        Files.setLastModifiedTime(viejo, java.nio.file.attribute.FileTime.fromMillis(0));
        try (ServidoresFase5.Crudo s = crudo(n -> Accion.RESPONDER)) {
            SifenConfig c = cfg(s);
            c.setTraceEnabled(true);
            c.setTraceFolder(dir.toString());
            c.setTraceRetentionDays(0);
            consulta(c, null);
        }
        Assertions.assertTrue(Files.exists(viejo));
    }

    @Test
    public void trazaSinCarpetaVaAlLogDeLaTraza() throws Exception {
        try (ServidoresFase5.Crudo s = crudo(n -> Accion.RESPONDER)) {
            SifenConfig c = cfg(s);
            c.setTraceEnabled(true);
            consulta(c, null);
        }
        Assertions.assertTrue(LOG.stream().anyMatch(e -> ExchangeTrace.TRACE_LOGGER.equals(e.getLoggerName())
                && e.getMessage().getFormattedMessage().contains("<dRUCCons>80123456</dRUCCons>")));
        Assertions.assertTrue(LOG.stream().filter(e -> !ExchangeTrace.TRACE_LOGGER.equals(e.getLoggerName()))
                .noneMatch(e -> e.getMessage().getFormattedMessage().contains("dRUCCons")), "fuera de la traza, sin XML");
    }
}
