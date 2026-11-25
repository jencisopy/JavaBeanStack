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
package org.javabeanstack.sifen.internal;

/**
 * Valores constantes utilizados en la librería.
 *
 * <p>{@code SDK_CURRENT_VERSION} es la versión de {@code jbs-sifen} (plan JBSSIF, D13; antes
 * {@code "0.2.4"} de rshk-jsifenlib, I0-02). Desde M1-01 sale del build: Maven filtra la versión del
 * proyecto en el recurso {@code org/javabeanstack/sifen/jbs-sifen.properties} y se lee al cargar la clase; no
 * hay literal de versión en el código. Si el recurso falta o no se filtró (un IDE que compila sin Maven), queda
 * {@link #UNKNOWN_VERSION}. {@code SIFEN_CURRENT_VERSION} ({@code dVerFor}) no cambia.</p>
 */
public class Constants {
    /** Versión cuando no se encuentra el recurso filtrado (M1-01). */
    public static final String UNKNOWN_VERSION = "desconocida";
    /** Recurso con la versión filtrada por Maven (M1-01). */
    static final String VERSION_RESOURCE = "/org/javabeanstack/sifen/jbs-sifen.properties";

    // Globals
    public static final String SDK_CURRENT_VERSION = readVersion();
    public static final String SIFEN_CURRENT_VERSION = "150";

    public static final String RSA_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

    // Namespaces
    public static final String SIFEN_NS_URI = "http://ekuatia.set.gov.py/sifen/xsd";
    public static final String SIFEN_NS_URI_RECEP_DE = SIFEN_NS_URI + " siRecepDE_v150.xsd";
    public static final String SIFEN_NS_URI_RECEP_EVENTO = SIFEN_NS_URI + " siRecepEvento_v150.xsd";

    /** Lee la versión del recurso filtrado; nunca lanza (M1-01). */
    private static String readVersion() {
        try (java.io.InputStream in = Constants.class.getResourceAsStream(VERSION_RESOURCE)) {
            if (in == null) {
                return UNKNOWN_VERSION;
            }
            java.util.Properties p = new java.util.Properties();
            p.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
            String v = p.getProperty("version", "").trim();
            return v.isEmpty() || v.startsWith("$") ? UNKNOWN_VERSION : v;
        } catch (java.io.IOException | RuntimeException e) {
            return UNKNOWN_VERSION;
        }
    }
}
