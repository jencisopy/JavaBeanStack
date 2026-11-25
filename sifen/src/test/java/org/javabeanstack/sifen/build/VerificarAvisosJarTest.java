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
package org.javabeanstack.sifen.build;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * La verificación del empaquetado detecta un aviso que falta o que cambió (plan JBSSIF, §8.5, M3-63). La
 * verificación de los jars reales la hace el build en {@code verify} con {@link VerificarAvisosJar}.
 */
public class VerificarAvisosJarTest {
    @TempDir
    Path tmp;

    private Map<String, Path> fuentes() throws Exception {
        Map<String, Path> m = new LinkedHashMap<>();
        for (String a : VerificarAvisosJar.AVISOS) {
            m.put(a, Files.writeString(tmp.resolve("repo-" + a), "texto de " + a));
        }
        return m;
    }

    private Path jar(String nombre, Map<String, String> entradas) throws Exception {
        Path p = tmp.resolve(nombre);
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(p))) {
            for (Map.Entry<String, String> e : entradas.entrySet()) {
                out.putNextEntry(new JarEntry(e.getKey()));
                out.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        return p;
    }

    @Test
    public void unJarCompletoPasa() throws Exception {
        Map<String, String> e = new LinkedHashMap<>();
        for (String a : VerificarAvisosJar.AVISOS) {
            e.put("META-INF/" + a, "texto de " + a);
        }
        Assertions.assertEquals(List.of(), VerificarAvisosJar.verificar(jar("completo.jar", e), fuentes()));
    }

    @Test
    public void faltanAvisosOCambiaElTexto() throws Exception {
        Map<String, String> e = new LinkedHashMap<>();
        e.put("META-INF/LICENSE", "texto de LICENSE");
        e.put("META-INF/NOTICE", "otro texto");
        List<String> f = VerificarAvisosJar.verificar(jar("incompleto.jar", e), fuentes());
        Assertions.assertEquals(2, f.size(), f.toString());
        Assertions.assertTrue(f.get(0).contains("META-INF/NOTICE no es igual"), f.toString());
        Assertions.assertTrue(f.get(1).contains("falta META-INF/LICENSE-rshk-jsifenlib.md"), f.toString());
    }

    @Test
    public void elMainFallaSiFaltaUnAviso() throws Exception {
        Map<String, Path> fu = fuentes();
        Path sinAvisos = jar("vacio.jar", Map.of("org/x/A.class", "x"));
        String[] args = {fu.get("LICENSE").toString(), fu.get("NOTICE").toString(),
            fu.get("LICENSE-rshk-jsifenlib.md").toString(), sinAvisos.toString()};
        IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class, () -> VerificarAvisosJar.main(args));
        Assertions.assertTrue(ex.getMessage().contains("falta META-INF/LICENSE"), ex.getMessage());
        Assertions.assertTrue(VerificarAvisosJar.verificar(tmp.resolve("no-existe.jar"), fu).get(0).contains("no existe"));
    }
}
