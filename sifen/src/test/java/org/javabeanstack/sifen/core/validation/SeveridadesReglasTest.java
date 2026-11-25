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
package org.javabeanstack.sifen.core.validation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Severidad de cada regla contra una tabla versionada independiente del catálogo (plan JBSSIF, §3.4;
 * C:I3-43): bajar una regla a AVISO (o subirla a ERROR) sin cambiar {@code validacion/severidades.tsv}
 * pone el build en rojo. La premisa de las reglas «no se envía» la comprueba
 * {@code DeValidatorTest.lasReglasNoSeEnviaNoLleganAlXml}.
 */
public class SeveridadesReglasTest {
    static final String TABLA = "validacion/severidades.tsv";
    static final String NO_EVALUADAS = "validacion/no_evaluadas.tsv";

    /** codigo ⇒ {severidad, motivo} de la tabla. */
    static Map<String, String[]> tabla() throws IOException {
        Map<String, String[]> m = new LinkedHashMap<>();
        try (InputStream in = SeveridadesReglasTest.class.getClassLoader().getResourceAsStream(TABLA)) {
            Assertions.assertNotNull(in, "falta " + TABLA);
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String l;
            while ((l = br.readLine()) != null) {
                if (l.isBlank() || l.startsWith("#")) {
                    continue;
                }
                String[] c = l.split("\t", -1);
                Assertions.assertNull(m.put(c[0], new String[] {c[1], c.length > 2 ? c[2] : ""}), "fila repetida: " + c[0]);
            }
        }
        return m;
    }

    @Test
    public void laSeveridadDeCadaReglaEsLaDeLaTabla() throws Exception {
        Map<String, String[]> t = tabla();
        TreeSet<String> malas = new TreeSet<>();
        for (SoporteValidacion.Regla r : SoporteValidacion.reglas()) {
            String[] fila = t.remove(r.codigo());
            if (fila == null) {
                malas.add(r.codigo() + ": sin fila en " + TABLA + " (severidad actual " + r.severidad() + ")");
            } else if (!fila[0].equals(r.severidad().name())) {
                malas.add(r.codigo() + ": la tabla dice " + fila[0] + " y el catálogo " + r.severidad());
            }
        }
        for (String sobra : t.keySet()) {
            malas.add(sobra + ": fila de una regla que no está en el catálogo");
        }
        Assertions.assertTrue(malas.isEmpty(), () -> malas.size() + " diferencias:\n" + String.join("\n", malas));
    }

    /**
     * C:I3-42: la lista explícita de exclusiones de CA3-03/CA3-04 (reglas que no se evalúan
     * localmente, con su motivo) es la de {@code validacion/no_evaluadas.tsv}, en los dos sentidos.
     */
    @Test
    public void lasNoEvaluadasSonLasDeLaLista() throws Exception {
        Map<String, String> lista = new LinkedHashMap<>();
        try (InputStream in = SeveridadesReglasTest.class.getClassLoader().getResourceAsStream(NO_EVALUADAS)) {
            Assertions.assertNotNull(in, "falta " + NO_EVALUADAS);
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String l;
            while ((l = br.readLine()) != null) {
                if (!l.isBlank() && !l.startsWith("#")) {
                    String[] c = l.split("\t", -1);
                    Assertions.assertNull(lista.put(c[0], c[1]), "fila repetida: " + c[0]);
                }
            }
        }
        TreeSet<String> malas = new TreeSet<>();
        for (Map.Entry<ValidationRule, ?> e
                : RuleCatalog.notEvaluated().entrySet()) {
            String codigo = e.getKey().getCode();
            String motivo = String.valueOf(e.getValue());
            String esperado = lista.remove(codigo);
            if (esperado == null) {
                malas.add(codigo + "\t" + motivo + "  <- no evaluada sin fila en " + NO_EVALUADAS);
            } else if (!esperado.equals(motivo)) {
                malas.add(codigo + ": la lista dice " + esperado + " y el catálogo " + motivo);
            }
        }
        for (String sobra : lista.keySet()) {
            malas.add(sobra + ": en la lista pero el catálogo la evalúa (o no existe)");
        }
        Assertions.assertTrue(malas.isEmpty(), () -> malas.size() + " diferencias:\n" + String.join("\n", malas));
    }

    @Test
    public void lasReglasNoSeEnvianSonAviso() throws Exception {
        for (Map.Entry<String, String[]> e : tabla().entrySet()) {
            if (e.getValue()[1].startsWith("no-se-envia")) {
                Assertions.assertEquals("WARNING", e.getValue()[0], e.getKey() + ": una regla «no se envía» es WARNING");
            }
        }
    }
}
