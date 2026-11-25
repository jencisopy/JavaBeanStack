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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;
import org.javabeanstack.sifen.core.SifenConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * La copia de la configuración de un cliente cubre todos los getters de {@link SifenConfig} (plan
 * JBSSIF, §10.2): un campo nuevo (p. ej. los de la Fase 5, carril B) que {@link SifenClientConfigCopy}
 * no copie hace fallar {@link #ningunGetterQuedaSinCubrir()}.
 */
public class SifenClientConfigCopyTest {

    /** Getters que la copia reproduce y esta prueba compara. */
    private static final Set<String> CUBIERTOS = Set.of("getEnvironment", "getBaseUrl", "getLocalBaseUrl",
            "getQRQueryUrl", "getSendPath", "getSendBatchPath", "getEventPath", "getQueryBatchPath",
            "getQueryRucPath", "getQueryDEPath", "isUseClientCertificate", "getClientCertificate",
            "getClientCertificatePassword", "getClientCertificateType", "getHttpConnectTimeout",
            "getHttpReadTimeout", "getUserAgent", "getCscId", "getCsc", "isHabilitarNotaTecnica13",
            "isPreValidationEnabled", "getDisabledRules", "getRulesDate",
            // §5.2 (carril B, I10-08): SifenConfig.copy() copia también las listas de la carga y la Fase 5
            "getInvalidProperties", "getUnknownProperties", "getProxyHost", "getProxyPort", "getProxyUser",
            "getProxyPassword", "getMaxRetries", "getRetryBackoffMs", "isTraceEnabled", "getTraceFolder",
            "getTraceRetentionDays", "getTraceEnabledSetting", "getTraceRetentionSetting");
    /** Getters que la copia no reproduce: ninguno desde {@code SifenConfig.copy()} (§5.2, I10-08). */
    private static final Set<String> SIN_COPIA = Set.of();

    /** Una configuración con todos los setters en un valor distinto del de omisión. */
    private static SifenConfig distinta() {
        SifenConfig c = new SifenConfig();
        c.setEnvironment(SifenConfig.Environment.PROD);
        c.setBaseUrl("https://127.0.0.1:9999");
        c.setSendPath("/a/recibe");
        c.setSendBatchPath("/a/recibe-lote");
        c.setEventPath("/a/evento");
        c.setQueryDEPath("/a/consulta");
        c.setQueryBatchPath("/a/consulta-lote");
        c.setQueryRucPath("/a/consulta-ruc");
        c.setUseClientCertificate(false);
        c.setClientCertificateType(SifenConfig.ClientCertificateType.PFX);
        c.setClientCertificate("/no/existe/empresa-a.pfx");
        c.setClientCertificatePassword("clave-a");
        c.setCscId("7");
        c.setCsc("ZZZZ0000000000000000000000000007");
        c.setHabilitarNotaTecnica13(true);
        c.setPreValidationEnabled(false);
        c.setDisabledRules(Set.of("D208c", "E002"));
        c.setRulesDate(LocalDate.of(2026, 12, 31));
        return c;
    }

    private static Set<String> getters() {
        Set<String> nombres = new TreeSet<>();
        for (Method m : SifenConfig.class.getMethods()) {
            if (m.getDeclaringClass() == Object.class || Modifier.isStatic(m.getModifiers()) || m.getParameterCount() > 0) {
                continue;
            }
            String n = m.getName();
            if ((n.startsWith("get") || n.startsWith("is")) && m.getReturnType() != void.class) {
                nombres.add(n);
            }
        }
        return nombres;
    }

    @Test
    public void ningunGetterQuedaSinCubrir() {
        Set<String> sobran = new TreeSet<>(getters());
        sobran.removeAll(CUBIERTOS);
        sobran.removeAll(SIN_COPIA);
        Assertions.assertTrue(sobran.isEmpty(), "getters de SifenConfig que SifenClientConfigCopy no cubre: " + sobran);
        Set<String> faltan = new TreeSet<>(CUBIERTOS);
        faltan.removeAll(getters());
        Assertions.assertTrue(faltan.isEmpty(), "getters que ya no existen: " + faltan);
    }

    @Test
    public void laCopiaDevuelveLoMismoEnCadaGetter() throws Exception {
        SifenConfig original = distinta();
        SifenConfig copia = SifenClientConfigCopy.of(original);
        Assertions.assertNotSame(original, copia);
        for (String n : CUBIERTOS) {
            Method m = SifenConfig.class.getMethod(n);
            Assertions.assertEquals(m.invoke(original), m.invoke(copia), n);
        }
        // y con los valores de omisión
        SifenConfig omision = new SifenConfig();
        SifenConfig copiaOmision = SifenClientConfigCopy.of(omision);
        for (String n : CUBIERTOS) {
            Method m = SifenConfig.class.getMethod(n);
            Assertions.assertEquals(m.invoke(omision), m.invoke(copiaOmision), n + " (omisión)");
        }
    }

    @Test
    public void cambiarLaOriginalNoCambiaLaCopia() {
        SifenConfig original = distinta();
        SifenConfig copia = SifenClientConfigCopy.of(original);
        original.setEnvironment(SifenConfig.Environment.DEV);
        original.setClientCertificate("/otro.pfx");
        original.setCsc("AAAA0000000000000000000000000000");
        original.setDisabledRules(Set.of());
        Assertions.assertEquals(SifenConfig.Environment.PROD, copia.getEnvironment());
        Assertions.assertEquals("/no/existe/empresa-a.pfx", copia.getClientCertificate());
        Assertions.assertEquals("ZZZZ0000000000000000000000000007", copia.getCsc());
        Assertions.assertEquals(Set.of("D208c", "E002"), copia.getDisabledRules());
    }

    @Test
    public void nulaDaNula() {
        Assertions.assertNull(SifenClientConfigCopy.of(null));
    }
}
