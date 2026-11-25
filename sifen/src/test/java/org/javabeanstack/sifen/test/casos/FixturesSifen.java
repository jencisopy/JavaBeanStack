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

import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Soporte de los tests sin red (plan JBSSIF, Fase 2): configuración con el certificado
 * autofirmado del módulo (D11), contexto de generación y lectura de recursos.
 *
 * <p>Es la misma configuración que usa el arnés A/B ({@code Generador.configuracion}): mismo
 * certificado, CSC público de desarrollo y NT 13 activa (E0-16). Ambiente DEV, sin URL de producción
 * (D28); nada de esto abre conexiones.</p>
 */
public final class FixturesSifen {
    /** Recurso del certificado autofirmado. */
    public static final String PFX = "certificados/jbs-sifen-autofirmado.pfx";
    /** Clave del almacén autofirmado (documentada en su README; no protege nada real). */
    public static final String PFX_CLAVE = "jbs-sifen-test";

    private FixturesSifen() {
    }

    /**
     * Ruta en disco del certificado autofirmado (en {@code target/test-classes}).
     *
     * @return ruta absoluta.
     */
    public static Path rutaPfx() {
        URL url = FixturesSifen.class.getClassLoader().getResource(PFX);
        if (url == null) {
            throw new IllegalStateException("Falta el recurso " + PFX);
        }
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Configuración local: DEV, certificado autofirmado, CSC público de desarrollo, NT 13 activa y
     * <b>sin validación previa</b>: los golden, el round-trip y el arnés A/B comparan contra la línea
     * base del fork, que no valida (D20, §3.5). Los tests de la validación usan
     * {@link #configuracionConValidacion()}.
     *
     * @return configuración nueva.
     */
    public static SifenConfig configuracion() {
        SifenConfig cfg = new SifenConfig();
        cfg.setEnvironment(SifenConfig.Environment.DEV);
        cfg.setUseClientCertificate(true);
        cfg.setClientCertificateType(SifenConfig.ClientCertificateType.PFX);
        cfg.setClientCertificate(rutaPfx().toString());
        cfg.setClientCertificatePassword(PFX_CLAVE);
        cfg.setCscId("0001");
        cfg.setCsc("ABCD0000000000000000000000000000");
        cfg.setHabilitarNotaTecnica13(true);
        cfg.setPreValidationEnabled(false);
        return cfg;
    }

    /**
     * La misma configuración local con la validación previa activa (el valor por defecto de
     * {@link SifenConfig}, D20).
     *
     * @return configuración nueva.
     */
    public static SifenConfig configuracionConValidacion() {
        SifenConfig cfg = configuracion();
        cfg.setPreValidationEnabled(true);
        return cfg;
    }

    /**
     * Contexto de generación por defecto de la configuración (NT 13 según la configuración).
     *
     * @param cfg configuración.
     * @return contexto.
     */
    public static GenerationCtx contexto(SifenConfig cfg) {
        return GenerationCtx.getDefaultFromConfig(cfg);
    }

    /**
     * Lee un recurso de texto UTF-8 del classpath de tests.
     *
     * @param ruta ruta relativa (p. ej. {@code casos/factura-simple-00.xml}).
     * @return contenido.
     * @throws IOException si no existe.
     */
    public static String recurso(String ruta) throws IOException {
        return new String(recursoBytes(ruta), StandardCharsets.UTF_8);
    }

    /**
     * Lee un recurso binario del classpath de tests.
     *
     * @param ruta ruta relativa.
     * @return bytes.
     * @throws IOException si no existe.
     */
    public static byte[] recursoBytes(String ruta) throws IOException {
        try (InputStream in = FixturesSifen.class.getClassLoader().getResourceAsStream(ruta)) {
            if (in == null) {
                throw new IOException("Falta el recurso " + ruta);
            }
            return in.readAllBytes();
        }
    }

    /**
     * Indica si existe un recurso del classpath de tests.
     *
     * @param ruta ruta relativa.
     * @return {@code true} si existe.
     */
    public static boolean existe(String ruta) {
        return FixturesSifen.class.getClassLoader().getResource(ruta) != null;
    }

    /**
     * XML de entrada de un caso del catálogo.
     *
     * @param caso caso.
     * @return contenido de {@code casos/<archivo>.xml}.
     * @throws IOException si falta.
     */
    public static String entrada(CasosDePrueba.Caso caso) throws IOException {
        return recurso("casos/" + caso.getArchivo() + ".xml");
    }

    /**
     * Copia el certificado autofirmado a un archivo (para tests que necesitan una ruta propia).
     *
     * @param destino archivo destino.
     * @return el destino.
     * @throws IOException si no se puede escribir.
     */
    public static Path copiarPfx(Path destino) throws IOException {
        Files.write(destino, recursoBytes(PFX));
        return destino;
    }
}
