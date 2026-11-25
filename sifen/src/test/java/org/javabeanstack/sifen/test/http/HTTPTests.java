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
package org.javabeanstack.sifen.test.http;

import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.internal.helpers.HttpHelper;
import org.javabeanstack.sifen.internal.helpers.SSLContextHelper;
import org.javabeanstack.sifen.test.SifenTestEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Test en vivo contra el ambiente de pruebas de SIFEN (plan JBSSIF, §1.8): requiere la configuración
 * de {@link SifenTestEnv}; sin ella la clase queda saltada.
 *
 * <p>§2.5 (M1-04): el test original no afirmaba nada y, con {@code null} como fábrica de sockets,
 * {@code HttpsURLConnection.setSSLSocketFactory} lanzaba {@code IllegalArgumentException} que
 * {@code HttpHelper} no captura. Ahora usa el contexto TLS de la librería (con el certificado de
 * cliente) y afirma que la respuesta es el WSDL. {@code HttpHelper.request} no devuelve nada y
 * escribe en {@code System.out} (M0-05): se captura esa salida.</p>
 */
@Tag("sifen-live")
public class HTTPTests {
    private SifenConfig config;

    @BeforeEach
    public void verificarConfiguracion() {
        config = SifenTestEnv.configuracionOSaltar();
    }

    @Test
    public void testWSDLConnection() throws Exception {
        String url = "https://sifen-test.set.gov.py/de/ws/consultas/consulta-ruc.wsdl?wsdl";
        ByteArrayOutputStream capturado = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(capturado, true, StandardCharsets.UTF_8));
        try {
            HttpHelper.request(SSLContextHelper.getContextFromConfig(config).getSocketFactory(), url);
        } finally {
            System.setOut(original);
        }
        String salida = capturado.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(salida.contains("definitions"),
                "La respuesta de " + url + " no es un WSDL (primeros 300 caracteres): "
                        + salida.substring(0, Math.min(300, salida.length())));
    }
}
