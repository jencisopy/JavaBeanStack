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
package org.javabeanstack.sifen.core;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Configuración de la Fase 5 (plan JBSSIF, §5.2 y §5.4; M0-14, I10-08, CA5-04): claves
 * {@code sifen.*} de tiempos, proxy, reintentos y traza (las del diseño, D42 punto 3), sus valores no
 * admitidos ({@code JBS-CFG-10}), {@link SifenConfig#copy()} completa y la contraseña del proxy fuera de
 * {@code toString()}.
 */
public class SifenConfigConexionTest {

    private static Properties completas() {
        Properties p = new Properties();
        p.setProperty("sifen.ambiente", "DEV");
        p.setProperty("sifen.http.connect_timeout", "5000");
        p.setProperty("sifen.http.read_timeout", "60000");
        p.setProperty("sifen.proxy.host", "proxy.empresa.local");
        p.setProperty("sifen.proxy.port", "3128");
        p.setProperty("sifen.proxy.user", "facturacion");
        p.setProperty("sifen.proxy.pass", "clave del proxy");
        p.setProperty("sifen.reintentos.max", "2");
        p.setProperty("sifen.reintentos.backoff_ms", "750");
        p.setProperty("sifen.traza.activa", "TRUE");
        p.setProperty("sifen.traza.carpeta", "/var/tmp/sifen-traza");
        p.setProperty("sifen.traza.retencion_dias", "30");
        return p;
    }

    @Test
    public void lasClavesDeLaFase5SeLeen() {
        SifenConfig c = SifenConfig.leerPropiedades(completas());
        Assertions.assertEquals(List.of(), c.getUnknownProperties(), "las claves nuevas son conocidas (JBS-CFG-11)");
        Assertions.assertEquals(List.of(), c.getInvalidProperties());
        Assertions.assertEquals(5000, c.getHttpConnectTimeout());
        Assertions.assertEquals(60000, c.getHttpReadTimeout());
        Assertions.assertEquals("proxy.empresa.local", c.getProxyHost());
        Assertions.assertEquals(3128, c.getProxyPort());
        Assertions.assertEquals("facturacion", c.getProxyUser());
        Assertions.assertEquals("clave del proxy", c.getProxyPassword());
        Assertions.assertEquals(2, c.getMaxRetries());
        Assertions.assertEquals(750, c.getRetryBackoffMs());
        Assertions.assertTrue(c.isTraceEnabled());
        Assertions.assertEquals("/var/tmp/sifen-traza", c.getTraceFolder());
        Assertions.assertEquals(30, c.getTraceRetentionDays());
        Assertions.assertEquals("sifen.http.connect_timeout", SifenConfig.HTTP_CONNECT_TIMEOUT_KEY);
        Assertions.assertEquals("sifen.reintentos.max", SifenConfig.MAX_RETRIES_KEY);
        Assertions.assertEquals("sifen.traza.activa", SifenConfig.TRACE_ENABLED_KEY);
        Assertions.assertEquals("sifen.proxy.pass", SifenConfig.PROXY_PASSWORD_KEY);
    }

    @Test
    public void valoresPorOmision() {
        SifenConfig c = new SifenConfig();
        Assertions.assertEquals(15000, c.getHttpConnectTimeout());
        Assertions.assertEquals(60000, c.getHttpReadTimeout());
        Assertions.assertNull(c.getProxyHost());
        Assertions.assertEquals(0, c.getProxyPort());
        Assertions.assertEquals(0, c.getMaxRetries(), "sin reintentos por omisión");
        Assertions.assertEquals(1000, c.getRetryBackoffMs());
        Assertions.assertFalse(c.isTraceEnabled());
        Assertions.assertNull(c.getTraceFolder());
    }

    @Test
    public void valoresNoAdmitidosVanAJbsCfg10YConservanElDeOmision() {
        Properties p = new Properties();
        p.setProperty("sifen.http.connect_timeout", "0");
        p.setProperty("sifen.http.read_timeout", "cuarenta");
        p.setProperty("sifen.proxy.port", "70000");
        p.setProperty("sifen.reintentos.max", "11");
        p.setProperty("sifen.reintentos.backoff_ms", "-1");
        p.setProperty("sifen.traza.activa", "si");
        p.setProperty("sifen.traza.retencion_dias", "-3");
        SifenConfig c = SifenConfig.leerPropiedades(p);
        Assertions.assertEquals(Set.of("sifen.http.connect_timeout", "sifen.http.read_timeout", "sifen.proxy.port",
                "sifen.reintentos.max", "sifen.reintentos.backoff_ms", "sifen.traza.activa", "sifen.traza.retencion_dias"),
                Set.copyOf(c.getInvalidProperties()));
        Assertions.assertEquals(7, c.getTraceRetentionDays());
        Assertions.assertEquals(15000, c.getHttpConnectTimeout());
        Assertions.assertEquals(60000, c.getHttpReadTimeout());
        Assertions.assertEquals(0, c.getMaxRetries());
        Assertions.assertFalse(c.isTraceEnabled());
    }

    @Test
    public void proxySinPuertoOUsuarioSinClaveSonNoAdmitidos() {
        Properties p = new Properties();
        p.setProperty("sifen.proxy.host", "proxy");
        p.setProperty("sifen.proxy.user", "u");
        SifenConfig c = SifenConfig.leerPropiedades(p);
        Assertions.assertEquals(List.of("sifen.proxy.port", "sifen.proxy.pass"), c.getInvalidProperties());
    }

    @Test
    public void lasSetterRechazanValoresSinSentido() {
        SifenConfig c = new SifenConfig();
        Assertions.assertThrows(IllegalArgumentException.class, () -> c.setHttpConnectTimeout(0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> c.setHttpReadTimeout(-5));
        Assertions.assertThrows(IllegalArgumentException.class, () -> c.setProxyPort(65536));
        Assertions.assertThrows(IllegalArgumentException.class, () -> c.setMaxRetries(11));
        Assertions.assertThrows(IllegalArgumentException.class, () -> c.setRetryBackoffMs(60001));
        c.setProxyHost("  ");
        Assertions.assertNull(c.getProxyHost(), "en blanco vuelve al proxy de la JVM");
    }

    /** 60 s por omisión (decisión del usuario, 2026-11-24): el máximo de respuesta de SIFEN del MT. */
    @Test
    public void elTiempoDeLecturaPorOmisionEsElMaximoDelMt() {
        Assertions.assertEquals(60_000, SifenConfig.DEFAULT_READ_TIMEOUT_MS);
        Assertions.assertEquals(60_000, SifenConfig.leerPropiedades(new Properties()).getHttpReadTimeout());
    }

    /** I5-10: la traza distingue «no definida» (rige la propiedad del sistema) de un valor de la configuración. */
    @Test
    public void laTrazaDistingueNoDefinidaDeApagada() {
        SifenConfig c = new SifenConfig();
        Assertions.assertNull(c.getTraceEnabledSetting());
        Assertions.assertNull(c.getTraceRetentionSetting());
        Assertions.assertEquals(7, c.getTraceRetentionDays());
        c.setTraceEnabled(false);
        c.setTraceRetentionDays(3);
        Assertions.assertEquals(Boolean.FALSE, c.getTraceEnabledSetting());
        Assertions.assertEquals(3, c.getTraceRetentionSetting());
        SifenConfig copia = c.copy();
        Assertions.assertEquals(Boolean.FALSE, copia.getTraceEnabledSetting());
        Assertions.assertEquals(3, copia.getTraceRetentionSetting());
        Properties p = new Properties();
        p.setProperty("sifen.traza.activa", "false");
        SifenConfig cargada = SifenConfig.leerPropiedades(p);
        Assertions.assertEquals(Boolean.FALSE, cargada.getTraceEnabledSetting(), "definida en las propiedades");
        Assertions.assertNull(cargada.getTraceRetentionSetting(), "no definida");
        Assertions.assertNull(SifenConfig.leerPropiedades(new Properties()).getTraceEnabledSetting());
    }

    /** M5-05: lo que se lee en cada intercambio y se cambia en caliente es volatile. */
    @Test
    public void losCamposQueCambianEnCalienteSonVolatile() throws Exception {
        for (String campo : new String[] {"httpConnectTimeout", "httpReadTimeout", "proxyHost", "proxyPort", "proxyUser",
            "proxyPassword", "maxRetries", "retryBackoffMs", "traceEnabled", "traceFolder", "traceRetentionDays"}) {
            Assertions.assertTrue(java.lang.reflect.Modifier.isVolatile(SifenConfig.class.getDeclaredField(campo).getModifiers()), campo);
        }
    }

    @Test
    public void toStringNoMuestraLaClaveDelProxy() {
        SifenConfig c = SifenConfig.leerPropiedades(completas());
        String t = c.toString();
        Assertions.assertFalse(t.contains("clave del proxy"), t);
        Assertions.assertTrue(t.contains("proxyPassword='****'"), t);
        Assertions.assertTrue(t.contains("proxyHost='proxy.empresa.local'"), t);
    }

    // ------------------------------------------------------------------ copy() (I10-08)

    private static SifenConfig distinta() {
        Properties p = completas();
        p.setProperty("sifen.ambiente", "PROD");
        p.setProperty("sifen.url_base", "https://127.0.0.1:9999");
        p.setProperty("sifen.desconocida", "x"); // JBS-CFG-11
        p.setProperty("sifen.validacion_previa", "talvez"); // JBS-CFG-10
        p.setProperty("sifen.certificado_cliente.archivo", "/no/existe.pfx");
        p.setProperty("sifen.certificado_cliente.contrasena", "c");
        p.setProperty("sifen.certificado_cliente.tipo", "PFX");
        p.setProperty("sifen.csc.id", "7");
        p.setProperty("sifen.csc", "ZZZZ0000000000000000000000000007");
        p.setProperty("sifen.habilitar_nota_tecnica_13", "true");
        p.setProperty("sifen.validacion_reglas_desactivadas", "D208c,E002");
        p.setProperty("sifen.validacion_fecha_reglas", "2099-12-31");
        SifenConfig c = SifenConfig.leerPropiedades(p);
        c.setSendPath("/a/recibe");
        c.setQueryDEPath("/a/consulta");
        c.setUseClientCertificate(false);
        return c;
    }

    @Test
    public void copyConservaCadaGetterIncluidasLasListasDeLaCarga() throws Exception {
        SifenConfig o = distinta();
        Assertions.assertEquals(List.of("sifen.desconocida"), o.getUnknownProperties());
        Assertions.assertEquals(List.of("sifen.validacion_previa"), o.getInvalidProperties());
        SifenConfig c = o.copy();
        Assertions.assertNotSame(o, c);
        int n = 0;
        for (Method m : SifenConfig.class.getMethods()) {
            if (m.getDeclaringClass() != SifenConfig.class || Modifier.isStatic(m.getModifiers()) || m.getParameterCount() > 0) {
                continue;
            }
            if ((m.getName().startsWith("get") || m.getName().startsWith("is")) && m.getReturnType() != void.class) {
                Assertions.assertEquals(m.invoke(o), m.invoke(c), m.getName());
                n++;
            }
        }
        Assertions.assertTrue(n >= 33, "getters comparados: " + n);
        Assertions.assertEquals(o.toString(), c.toString());
        Assertions.assertEquals(LocalDate.of(2099, 12, 31), c.getRulesDate());
    }

    @Test
    public void copyEsIndependiente() {
        SifenConfig o = distinta();
        SifenConfig c = o.copy();
        o.setDisabledRules(Set.of());
        o.setProxyHost("otro");
        o.setHttpReadTimeout(1);
        o.setTraceEnabled(false);
        o.setEnvironment(SifenConfig.Environment.DEV);
        Assertions.assertEquals(Set.of("D208c", "E002"), c.getDisabledRules());
        Assertions.assertEquals("proxy.empresa.local", c.getProxyHost());
        Assertions.assertEquals(60000, c.getHttpReadTimeout());
        Assertions.assertTrue(c.isTraceEnabled());
        Assertions.assertEquals(SifenConfig.Environment.PROD, c.getEnvironment());
        Assertions.assertEquals("https://sifen.set.gov.py", c.getLocalBaseUrl(), "la URL derivada no se recalcula al copiar");
    }
}
