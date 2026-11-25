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
package org.javabeanstack.sifen.test.fase4;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * El módulo es público (LGPL): ningún fuente ni recurso de {@code src/}, ni ningún {@code *.md} del módulo
 * ({@code README.md}, licencias, los README de los recursos), nombra a un cliente real ni lleva un
 * dato de un tercero (plan JBSSIF, §2.1; I4-13, I3-65 y, desde E8-03, el RUC real del emisor de E3-34). Las cadenas buscadas se arman por partes para que este
 * test no se encuentre a sí mismo.
 */
public class SinDatosDeClientesTest {
    private static final List<String> PROHIBIDOS = List.of(
            ("tetra" + "pak"),     // cliente del caso de redondeo en USD (I4-13)
            ("1255" + "7605"),     // timbrado de un tercero (I3-65)
            ("8006" + "6026"),     // RUC real del emisor de prueba (E3-34, ESC-19; regresión E8-03)
            ("80.06" + "6.026"));  // el mismo, con puntos

    @Test
    public void ningunArchivoDeSrcNombraAUnCliente() throws Exception {
        Path src = Paths.get("src");
        Assertions.assertTrue(Files.isDirectory(src.resolve("main")), "se corre desde el módulo sifen: " + src.toAbsolutePath());
        List<String> hallados = new ArrayList<>();
        List<Path> archivos = new ArrayList<>();
        try (Stream<Path> s = Files.walk(src)) {
            archivos.addAll(s.filter(Files::isRegularFile).toList());
        }
        // I4-13: también los *.md del módulo fuera de src/ (README.md, LICENSE-rshk-jsifenlib.md), sin target/
        try (Stream<Path> s = Files.walk(Paths.get("."))) {
            archivos.addAll(s.filter(q -> Files.isRegularFile(q) && q.toString().endsWith(".md")
                    && !q.startsWith(Paths.get(".", "target")) && !q.startsWith(Paths.get(".", "src"))).toList());
        }
        Assertions.assertTrue(archivos.stream().anyMatch(q -> q.getFileName().toString().equals("README.md")
                && q.getParent() != null && q.getParent().equals(Paths.get("."))), "recorre sifen/README.md");
        {
            for (Path f : archivos) {
                String texto = new String(Files.readAllBytes(f), StandardCharsets.ISO_8859_1).toLowerCase(Locale.ROOT);
                for (String p : PROHIBIDOS) {
                    if (texto.contains(p)) {
                        hallados.add(f + ": " + p.charAt(0) + "…");
                    }
                }
            }
        }
        Assertions.assertEquals(List.of(), hallados, "datos de clientes en src/ (§2.1, I4-13)");
    }
}
