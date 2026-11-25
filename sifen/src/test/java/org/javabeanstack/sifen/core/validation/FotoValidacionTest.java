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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.fields.request.event.TrGesEve;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Foto del comportamiento del validador (plan JBSSIF, §3.6, CA3-07; script de un solo uso): por cada
 * positivo del catálogo de casos, cada fila de {@code negativos_de.tsv} y cada mutación de
 * {@link GrafoDe} sobre las fuentes de la mutación anti-NPE, vuelca {@code (sección, id, código, ruta,
 * severidad)} ordenado. Se corre antes y después del refactor de D33 y los dos archivos deben ser
 * idénticos. Solo corre con {@code -Djbs.sifen.foto=<archivo de salida>}.
 */
public class FotoValidacionTest {

    @Test
    public void volcarFoto() throws Exception {
        String salida = System.getProperty("jbs.sifen.foto");
        Assumptions.assumeTrue(salida != null && !salida.isBlank(), "sin -Djbs.sifen.foto");
        DeValidatorTest.configurar();
        DEValidator de = SoporteValidacion.validador();
        EventValidator ev = SoporteValidacion.validadorEventos();
        GenerationCtx ctx = SoporteValidacion.contexto(SoporteValidacion.configuracion(false));
        TreeSet<String> lineas = new TreeSet<>();

        for (SoporteValidacion.Fuente f : SoporteValidacion.fuentes()) {
            volcar(lineas, "POS", f.id(), validar(de, ev, f.crear(), ctx));
        }
        for (SoporteValidacion.FuenteEvento f : SoporteValidacion.fuentesEvento()) {
            volcar(lineas, "POS", f.id(), validar(de, ev, f.crear(), ctx));
        }
        for (DeValidatorTest.Negativo n : DeValidatorTest.leerTabla()) {
            String id = "L" + n.linea() + ":" + n.codigo();
            boolean evento = SoporteValidacion.regla(n.codigo()).map(SoporteValidacion.Regla::evento).orElse(false);
            try {
                DeValidatorTest.Base b = DeValidatorTest.elegirBase(n, evento);
                if (b == null) {
                    lineas.add("NEG\t" + id + "\tSIN_BASE");
                    continue;
                }
                Object raiz = b.crear();
                GenerationCtx c = DeValidatorTest.aplicar(n, raiz);
                volcar(lineas, "NEG", id, validar(de, ev, raiz, c));
            } catch (Throwable t) {
                lineas.add("NEG\t" + id + "\tEXC\t" + t.getClass().getName());
            }
        }
        List<Object[]> fuentesMutacion = new ArrayList<>();
        for (SoporteValidacion.Fuente f : SoporteValidacion.fuentes()) {
            fuentesMutacion.add(new Object[] {f.id(), (java.util.function.Supplier<Object>) f::crearCorregido});
        }
        for (SoporteValidacion.FuenteEnriquecida f : SoporteValidacion.fuentesEnriquecidas()) {
            fuentesMutacion.add(new Object[] {f.id(), (java.util.function.Supplier<Object>) f::crear});
        }
        for (SoporteValidacion.FuenteEvento f : SoporteValidacion.fuentesEvento()) {
            fuentesMutacion.add(new Object[] {f.id(), (java.util.function.Supplier<Object>) f::crear});
        }
        for (Object[] fm : fuentesMutacion) {
            @SuppressWarnings("unchecked")
            java.util.function.Supplier<Object> fabrica = (java.util.function.Supplier<Object>) fm[1];
            List<GrafoDe.Punto> puntos = GrafoDe.puntos(fabrica.get());
            for (int i = 0; i < puntos.size(); i++) {
                GrafoDe.Punto p = puntos.get(i);
                String id = fm[0] + "#" + GrafoDe.rutaDe(p.camino(), p.tipo()) + ":" + p.tipo();
                try {
                    Object raiz = fabrica.get();
                    GrafoDe.aplicar(raiz, p);
                    volcar(lineas, "MUT", id, validar(de, ev, raiz, ctx));
                } catch (Throwable t) {
                    lineas.add("MUT\t" + id + "\tEXC\t" + t.getClass().getName());
                }
            }
        }
        Path destino = Paths.get(salida);
        Files.createDirectories(destino.toAbsolutePath().getParent());
        Files.write(destino, lineas, StandardCharsets.UTF_8);
        System.out.println("[FOTO] " + lineas.size() + " líneas en " + destino.toAbsolutePath());
    }

    private static ValidationResult validar(DEValidator de, EventValidator ev, Object raiz, GenerationCtx c) {
        if (raiz instanceof TrGesEve evento) {
            return ev.validate(evento, c);
        }
        return de.validate((DocumentoElectronico) raiz, c);
    }

    private static void volcar(TreeSet<String> lineas, String seccion, String id, ValidationResult r) {
        if (r.isEmpty()) {
            lineas.add(seccion + "\t" + id + "\tSIN_ERRORES");
        }
        for (ValidationError e : r.getAll()) {
            lineas.add(seccion + "\t" + id + "\t" + e.getCode() + "\t" + e.getPath() + "\t" + e.getSeverity());
        }
    }

    static void escribir(Path p, List<String> l) throws IOException {
        Files.write(p, l, StandardCharsets.UTF_8);
    }
}
