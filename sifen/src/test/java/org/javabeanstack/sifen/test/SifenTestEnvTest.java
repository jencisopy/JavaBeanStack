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
package org.javabeanstack.sifen.test;

import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.TestAbortedException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

/**
 * Guardas de {@link SifenTestEnv} sin red (plan JBSSIF, §2.7, D28, M1-03).
 *
 * <p>Usa {@link SifenTestEnv#cargar(Path)} con archivos de {@code @TempDir} y el certificado
 * autofirmado. <b>Nunca</b> fija {@code jbs.sifen.test.env} con {@code System.setProperty}: surefire
 * comparte la JVM entre clases y eso armaría los tests en vivo.</p>
 */
public class SifenTestEnvTest {
    private static final String CLAVE_CENTINELA = "CLAVE-CENTINELA-91c2e7";
    private static final String CSC_CENTINELA = "CSCCENTINELA7f3a00000000000000AB";

    @TempDir
    Path dir;

    private Path pfxConClaveCentinela() throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(FixturesSifen.rutaPfx())) {
            ks.load(in, FixturesSifen.PFX_CLAVE.toCharArray());
        }
        String alias = ks.aliases().nextElement();
        KeyStore nuevo = KeyStore.getInstance("PKCS12");
        nuevo.load(null, null);
        nuevo.setKeyEntry(alias, ks.getKey(alias, FixturesSifen.PFX_CLAVE.toCharArray()), CLAVE_CENTINELA.toCharArray(),
                ks.getCertificateChain(alias));
        Path destino = dir.resolve("centinela.pfx");
        try (OutputStream out = Files.newOutputStream(destino)) {
            nuevo.store(out, CLAVE_CENTINELA.toCharArray());
        }
        return destino;
    }

    private Path env(String ambiente, Path certificado, boolean conCsc, boolean conEmisor, String extra) throws Exception {
        StringBuilder sb = new StringBuilder();
        if (ambiente != null) {
            sb.append("SIFEN_AMBIENTE=").append(ambiente).append('\n');
        }
        if (certificado != null) {
            sb.append("SIFEN_CERT_PATH=").append(certificado).append('\n');
        }
        sb.append("SIFEN_CERT_PASS=").append(CLAVE_CENTINELA).append('\n');
        if (conCsc) {
            sb.append("SIFEN_CSC_ID=0001\nSIFEN_CSC=").append(CSC_CENTINELA).append('\n');
        }
        if (conEmisor) {
            sb.append("SIFEN_EMISOR_RUC=80123456\nSIFEN_EMISOR_DV=5\nSIFEN_TIMBRADO=12345678\n")
                    .append("SIFEN_TIMBRADO_FECHA_INICIO=2026-01-02\nSIFEN_ESTABLECIMIENTO=001\nSIFEN_PUNTO_EXPEDICION=001\n");
        }
        if (extra != null) {
            sb.append(extra).append('\n');
        }
        Path f = Files.createTempFile(dir, "sifen", ".env");
        Files.write(f, sb.toString().getBytes(StandardCharsets.ISO_8859_1));
        return f;
    }

    private static void sinSecretos(String texto) {
        Assertions.assertFalse(texto.contains(CLAVE_CENTINELA), "aparece la clave del certificado");
        Assertions.assertFalse(texto.contains(CSC_CENTINELA), "aparece el CSC");
    }

    // ------------------------------------------------------------------ D28

    @Test
    public void ambienteProdCortaLaCorrida() throws Exception {
        Path f = env("PROD", pfxConClaveCentinela(), true, true, null);
        IllegalStateException e = Assertions.assertThrows(IllegalStateException.class, () -> SifenTestEnv.cargar(f));
        Assertions.assertTrue(e.getMessage().contains("SIFEN_AMBIENTE=PROD"));
        sinSecretos(e.getMessage());
    }

    @Test
    public void ambienteEnMinusculaTambienCorta() throws Exception {
        Path f = env("dev", pfxConClaveCentinela(), true, true, null);
        Assertions.assertThrows(IllegalStateException.class, () -> SifenTestEnv.cargar(f));
    }

    /** La guarda va antes de las asunciones: PROD sin certificado ni CSC falla, no se salta. */
    @Test
    public void ambienteProdSinCertificadoIgualFalla() throws Exception {
        Path f = env("PROD", null, false, false, null);
        Assertions.assertThrows(IllegalStateException.class, () -> SifenTestEnv.cargar(f));
    }

    @Test
    public void ambienteDevDevuelveConfiguracionDev() throws Exception {
        Path f = env("DEV", pfxConClaveCentinela(), true, true, null);
        SifenTestEnv.Entorno e = SifenTestEnv.cargar(f);
        SifenConfig c = e.getConfiguracion();
        Assertions.assertEquals(SifenConfig.Environment.DEV, c.getEnvironment());
        Assertions.assertTrue(c.getLocalBaseUrl().contains("sifen-test.set.gov.py"), "URL del ambiente de pruebas");
        Assertions.assertTrue(c.getQRQueryUrl().contains("consultas-test"), "QR de pruebas");
        Assertions.assertNull(c.getBaseUrl(), "sin URL base explícita");
        Assertions.assertTrue(c.isHabilitarNotaTecnica13());
        Assertions.assertEquals("80123456", e.getEmisor().getRuc());
        Assertions.assertEquals(12345678, e.getEmisor().getTimbrado());
        Assertions.assertEquals(SifenTestEnv.NUMERO_INICIAL_MINIMO, e.getNumeroInicial());
        Assertions.assertNull(e.getEmisor().getEmail(), "SIFEN_EMISOR_EMAIL es opcional para la configuración (I2-08)");
    }

    /** I2-08: el correo del emisor se lee si está y no condiciona al resto de los datos del emisor. */
    @Test
    public void emailDelEmisorSeLeeYEsOpcional() throws Exception {
        Path f = env("DEV", pfxConClaveCentinela(), true, true, "SIFEN_EMISOR_EMAIL=emisor.prueba@example.com");
        SifenTestEnv.Emisor emisor = SifenTestEnv.cargar(f).getEmisor();
        Assertions.assertEquals("emisor.prueba@example.com", emisor.getEmail());
        Assertions.assertEquals("emisor.prueba@example.com", SifenTestEnv.emailParaReemitirOSaltar(emisor));
        // solo el correo, sin el resto del emisor: no alcanza para tener emisor
        Assertions.assertNull(SifenTestEnv.cargar(env("DEV", pfxConClaveCentinela(), true, false,
                "SIFEN_EMISOR_EMAIL=emisor.prueba@example.com")).getEmisor());
    }

    @Test
    public void sinAmbienteValeDev() throws Exception {
        Path f = env(null, pfxConClaveCentinela(), true, false, null);
        SifenTestEnv.Entorno e = SifenTestEnv.cargar(f);
        Assertions.assertEquals(SifenConfig.Environment.DEV, e.getConfiguracion().getEnvironment());
        Assertions.assertNull(e.getEmisor(), "sin datos del emisor");
    }

    @Test
    public void sinCscSaltaSinFallar() throws Exception {
        Path f = env("DEV", pfxConClaveCentinela(), false, false, null);
        TestAbortedException e = Assertions.assertThrows(TestAbortedException.class, () -> SifenTestEnv.cargar(f));
        sinSecretos(e.getMessage());
    }

    @Test
    public void archivoInexistenteSalta() {
        Assertions.assertThrows(TestAbortedException.class, () -> SifenTestEnv.cargar(dir.resolve("no-existe.env")));
    }

    @Test
    public void numeroInicialMenorQue1000CortaLaCorrida() throws Exception {
        // ESC-8 (usuario, 2026-11-24): los DE en vivo de jbs-sifen van desde el 0001000.
        Assertions.assertEquals(1000, SifenTestEnv.NUMERO_INICIAL_MINIMO);
        Path f = env("DEV", pfxConClaveCentinela(), true, true, "SIFEN_NUMERO_INICIAL=50");
        Assertions.assertThrows(IllegalStateException.class, () -> SifenTestEnv.cargar(f));
        Path g = env("DEV", pfxConClaveCentinela(), true, true, "SIFEN_NUMERO_INICIAL=999");
        Assertions.assertThrows(IllegalStateException.class, () -> SifenTestEnv.cargar(g));
        Path h = env("DEV", pfxConClaveCentinela(), true, true, "SIFEN_NUMERO_INICIAL=1073");
        Assertions.assertEquals(1073, SifenTestEnv.cargar(h).getNumeroInicial());
    }

    // ------------------------------------------------------------------ M1-03

    /**
     * Canario de M1-03: con el log en ALL y las salidas estándar capturadas, cargar la configuración
     * y firmar un DE con ella no deja la clave ni el CSC en ningún lado.
     */
    @Test
    public void noVuelcaSecretosAlLog() throws Exception {
        List<String> eventos = new ArrayList<>();
        LoggerContext contexto = (LoggerContext) LogManager.getContext(false);
        Configuration conf = contexto.getConfiguration();
        AbstractAppender captura = new AbstractAppender("canario-m103", null, null, true, Property.EMPTY_ARRAY) {
            @Override
            public void append(LogEvent event) {
                eventos.add(event.getMessage().getFormattedMessage()
                        + (event.getThrown() != null ? " " + event.getThrown() : ""));
            }
        };
        captura.start();
        LoggerConfig raiz = conf.getRootLogger();
        Level nivelAnterior = raiz.getLevel();
        raiz.addAppender(captura, Level.ALL, null);
        raiz.setLevel(Level.ALL);
        contexto.updateLoggers();
        PrintStream out = System.out;
        PrintStream err = System.err;
        ByteArrayOutputStream estandar = new ByteArrayOutputStream();
        PrintStream captado = new PrintStream(estandar, true, StandardCharsets.UTF_8);
        System.setOut(captado);
        System.setErr(captado);
        try {
            LogManager.getLogger("org.javabeanstack.sifen").info("marca-canario-m103");
            SifenTestEnv.Entorno e = SifenTestEnv.cargar(env("DEV", pfxConClaveCentinela(), true, true, null));
            SifenConfig c = e.getConfiguracion();
            // El fixture usa un emisor ficticio (D105 lo rechazaría en DEV): este test mira el log de la
            // generación, no la validación previa (§3.5, D20).
            c.setPreValidationEnabled(false);
            GenerationCtx ctx = GenerationCtx.getDefaultFromConfig(c);
            String xml = CasosDePrueba.documentos().get(4).construir().generarXml(ctx, c);
            Assertions.assertNotNull(xml);
            sinSecretos(xml);
        } finally {
            System.setOut(out);
            System.setErr(err);
            raiz.removeAppender("canario-m103");
            raiz.setLevel(nivelAnterior);
            contexto.updateLoggers();
            captura.stop();
        }
        String todo = String.join("\n", eventos) + "\n" + estandar.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(todo.contains("marca-canario-m103"), "el canario no captura el log");
        sinSecretos(todo);
    }
}
