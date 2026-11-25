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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Verificación del empaquetado (plan JBSSIF, §8.5; M3-63, D22): cada jar publicado de {@code jbs-sifen}
 * (el principal y el {@code -tests.jar} del simulador) lleva en {@code META-INF/} los tres avisos de
 * licencia, iguales byte a byte a los del repositorio: {@code LICENSE} (LGPL v3 de JavaBeanStack),
 * {@code NOTICE} (con el aviso MIT de Roshka S.A. + Taxare S.A.) y {@code LICENSE-rshk-jsifenlib.md} (la
 * licencia MIT del proyecto de origen, que exige el aviso en cada copia).
 *
 * <p>La corre el build de {@code sifen/} en la fase {@code verify} ({@code exec:java}, después de
 * {@code package} y antes de {@code install}): si un jar no trae un aviso, o trae otro texto, el build
 * falla. {@code VerificarAvisosJarTest} prueba que la verificación detecta lo que falta.</p>
 *
 * <p>Uso: {@code VerificarAvisosJar <LICENSE> <NOTICE> <LICENSE-rshk-jsifenlib.md> <jar>...}. Vive en los
 * fuentes de test y no entra en ningún jar publicado (el {@code -tests.jar} solo lleva el simulador).</p>
 */
public final class VerificarAvisosJar {
    /** Nombres de los avisos dentro de {@code META-INF/}, en el orden de los argumentos. */
    public static final List<String> AVISOS = List.of("LICENSE", "NOTICE", "LICENSE-rshk-jsifenlib.md");

    private VerificarAvisosJar() {
    }

    /**
     * Verifica los jars.
     *
     * @param args los tres avisos del repositorio y uno o más jars.
     * @throws Exception si falta un aviso, difiere del repositorio o no se puede leer un archivo.
     */
    public static void main(String[] args) throws Exception {
        if (args.length < AVISOS.size() + 1) {
            throw new IllegalArgumentException("uso: VerificarAvisosJar <LICENSE> <NOTICE> <LICENSE-rshk-jsifenlib.md> <jar>...");
        }
        Map<String, Path> fuentes = new LinkedHashMap<>();
        for (int i = 0; i < AVISOS.size(); i++) {
            fuentes.put(AVISOS.get(i), Path.of(args[i]));
        }
        List<String> fallas = new ArrayList<>();
        for (String jar : Arrays.copyOfRange(args, AVISOS.size(), args.length)) {
            fallas.addAll(verificar(Path.of(jar), fuentes));
        }
        if (!fallas.isEmpty()) {
            throw new IllegalStateException("Avisos de licencia del empaquetado (§8.5, M3-63):\n  " + String.join("\n  ", fallas));
        }
        System.out.println("[jbs-sifen] avisos de licencia presentes e iguales al repositorio en "
                + (args.length - AVISOS.size()) + " jar(s)");
    }

    /**
     * Fallas de un jar: cada aviso que falta en {@code META-INF/} o cuyo contenido difiere del repositorio.
     *
     * @param jar     jar a revisar.
     * @param fuentes aviso → archivo del repositorio.
     * @return las fallas; vacía si está completo.
     * @throws IOException si no se puede leer el jar o un aviso del repositorio.
     */
    public static List<String> verificar(Path jar, Map<String, Path> fuentes) throws IOException {
        List<String> fallas = new ArrayList<>();
        if (!Files.isRegularFile(jar)) {
            fallas.add(jar + ": no existe");
            return fallas;
        }
        try (JarFile j = new JarFile(jar.toFile())) {
            for (Map.Entry<String, Path> f : fuentes.entrySet()) {
                ZipEntry e = j.getEntry("META-INF/" + f.getKey());
                if (e == null) {
                    fallas.add(jar.getFileName() + ": falta META-INF/" + f.getKey());
                    continue;
                }
                byte[] dentro;
                try (InputStream in = j.getInputStream(e)) {
                    dentro = in.readAllBytes();
                }
                if (!Arrays.equals(dentro, Files.readAllBytes(f.getValue()))) {
                    fallas.add(jar.getFileName() + ": META-INF/" + f.getKey() + " no es igual a " + f.getValue());
                }
            }
        }
        return fallas;
    }
}
