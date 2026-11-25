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
package org.javabeanstack.sifen.internal.http;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Proxy {@code Basic} con usuario y destino {@code https://} (plan JBSSIF, §5.2; AB:E5-02, AB:M5-06,
 * CA5-04). La bandera {@code -Djdk.http.auth.tunneling.disabledSchemes=} se lee al arrancar la JVM, así
 * que el caso corre en una JVM aparte ({@link TunelConUsuarioMain}) con el classpath de esta prueba:
 * con la clave correcta el pedido pasa (2 {@code CONNECT}: el primero sin credencial y el segundo con
 * ella); con una incorrecta sale {@code JBS-CONEX-11} (desde §10.7, I5-09; antes {@code JBS-CONEX-02}), el detalle dice que el proxy rechazó la credencial
 * y hay a lo sumo 2 {@code CONNECT} (antes eran 20, uno por cada 407 hasta {@code http.maxRedirects}:
 * bloquea la cuenta de un proxy atado al directorio).
 */
public class ProxyConUsuarioHttpsTest {
    private static List<String> salida;

    @BeforeAll
    public static void correr() throws Exception {
        String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        ProcessBuilder pb = new ProcessBuilder(java, "-Djdk.http.auth.tunneling.disabledSchemes=",
                "-cp", System.getProperty("java.class.path"), TunelConUsuarioMain.class.getName());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        List<String> l = new ArrayList<>();
        try (java.io.BufferedReader r = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String x;
            while ((x = r.readLine()) != null) {
                l.add(x);
            }
        }
        Assertions.assertTrue(p.waitFor(60, TimeUnit.SECONDS), "la JVM de la prueba no terminó");
        salida = l;
    }

    private static String linea(String caso) {
        return salida.stream().filter(x -> x.startsWith(caso + " ")).findFirst()
                .orElseThrow(() -> new AssertionError("sin la línea " + caso + ": " + salida));
    }

    private static long connects(String l) {
        return Long.parseLong(l.replaceAll(".* connects=(\\d+) .*", "$1"));
    }

    @Test
    public void conLaClaveCorrectaElPedidoPasa() {
        String l = linea("correcta");
        Assertions.assertTrue(l.contains("resultado=200"), l);
        Assertions.assertEquals(2, connects(l), l);
    }

    @Test
    public void conLaClaveIncorrectaComoMuchoDosConnectYLaCausaClara() {
        String l = linea("incorrecta");
        Assertions.assertTrue(l.contains("resultado=JBS-CONEX-11"), "§10.7 (I5-09): la credencial rechazada tiene su código: " + l);
        Assertions.assertTrue(connects(l) <= 2, "la credencial rechazada se entrega una sola vez (E5-02): " + l);
        Assertions.assertTrue(l.contains("rechazó la credencial del usuario usuario-proxy"), l);
        Assertions.assertFalse(l.contains("detalle=null"), l);
        Assertions.assertFalse(l.contains("otra"), "la clave no aparece en el detalle: " + l);
    }
}
