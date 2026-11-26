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
package org.javabeanstack.sifen.test.fase12;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.beans.EventosDE;
import org.javabeanstack.sifen.core.validation.DEValidator;
import org.javabeanstack.sifen.core.validation.EventValidator;
import org.javabeanstack.sifen.test.casos.BateriaIdentificacion;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.CasosFase12;
import org.javabeanstack.sifen.test.casos.FixturesSifen;

/**
 * Lectura de {@code casos/fase12/esperados.tsv} (plan JBSSIF, §12.1 y §12.2) y armado del documento o del
 * evento de cada fila desde su catálogo de origen.
 */
public final class EsperadosFase12 {
    /** Recurso de los esperados. */
    public static final String RECURSO = "casos/fase12/esperados.tsv";
    /** Zona de Asunción. */
    public static final ZoneId ZONA = ZoneId.of("America/Asuncion");
    /** Reloj de la validación: la firma de los casos más 5 s, como en la Fase 3. */
    public static final Clock RELOJ = Clock.fixed(CasosDePrueba.FECHA_FIRMA.plusSeconds(5).atZone(ZONA).toInstant(), ZONA);

    private EsperadosFase12() {
    }

    /** Una fila de los esperados. */
    public record Esperado(String caso, String clase, String origen, Set<String> errores, Set<String> avisos,
            String sifen, String vivo, String tanda, String nota) {
        /** @return {@code true} si la fila es una operación de §12.3 (no se arma desde un catálogo). */
        public boolean esOperacion() {
            return "operacion".equals(clase);
        }

        /** @return {@code true} si la fila es un evento. */
        public boolean esEvento() {
            return clase.startsWith("evento");
        }

        /** @return el DE de la fila, nuevo en cada llamada. */
        public DocumentoElectronico documento() {
            switch (origen) {
                case "f12":
                    Supplier<DocumentoElectronico> s = CasosFase12.todos().get(caso);
                    if (s == null) {
                        throw new IllegalArgumentException("Caso inexistente en CasosFase12: " + caso);
                    }
                    return s.get();
                case "casos":
                    return CasosFase12.leyendas(CasosDePrueba.documentos().stream().filter(k -> k.getId().equals(caso))
                            .findFirst().orElseThrow(() -> new IllegalArgumentException(caso)).construir());
                case "bateria":
                    return BateriaIdentificacion.combinaciones().stream().filter(k -> k.id().equals(caso))
                            .findFirst().orElseThrow(() -> new IllegalArgumentException(caso)).construir();
                default:
                    throw new IllegalArgumentException("Origen desconocido " + origen);
            }
        }

        /** @return el evento de la fila, nuevo en cada llamada. */
        public EventosDE evento() {
            if ("casos".equals(origen)) {
                return CasosDePrueba.eventos().stream().filter(k -> k.getId().equals(caso)).findFirst().orElseThrow().construir();
            }
            for (CasosFase12.CasoEvento e : CasosFase12.eventos()) {
                if (e.id().equals(caso)) {
                    return e.construir();
                }
            }
            Supplier<EventosDE> s = CasosFase12.eventosNegativos().get(caso);
            if (s == null) {
                throw new IllegalArgumentException("Evento inexistente: " + caso);
            }
            return s.get();
        }

        @Override
        public String toString() {
            return caso;
        }
    }

    /**
     * Lee las filas de los esperados.
     *
     * @return filas, en orden.
     */
    public static List<Esperado> leer() {
        String texto;
        try {
            texto = FixturesSifen.recurso(RECURSO);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        List<Esperado> l = new ArrayList<>();
        boolean cabecera = true;
        for (String linea : texto.split("\n")) {
            if (linea.isBlank() || linea.startsWith("#")) {
                continue;
            }
            if (cabecera) {
                cabecera = false;
                continue;
            }
            String[] c = linea.split("\t", -1);
            if (c.length != 9) {
                throw new IllegalStateException(RECURSO + ": " + c.length + " columnas en «" + linea + "»");
            }
            l.add(new Esperado(c[0], c[1], c[2], codigos(c[3]), codigos(c[4]), c[5], c[6], c[7], c[8]));
        }
        return Collections.unmodifiableList(l);
    }

    private static Set<String> codigos(String columna) {
        if ("-".equals(columna)) {
            return Set.of();
        }
        return new TreeSet<>(Arrays.asList(columna.split(",")));
    }

    /** @return validador de DE de la fase: las reglas estándar en modo estricto y con el reloj fijo. */
    public static DEValidator validador() {
        return DEValidator.builder().registerStandard().strict().clock(RELOJ).build();
    }

    /** @return validador de eventos de la fase, estricto y con el reloj fijo. */
    public static EventValidator validadorEventos() {
        return EventValidator.builder().registerStandard().strict().clock(RELOJ).build();
    }
}
