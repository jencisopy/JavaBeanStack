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

import java.time.LocalDate;

/**
 * Documento normativo del que sale una regla (D33): el Manual Técnico, cada nota técnica, la Guía de
 * Pruebas o la propia librería. <b>Una NT nueva es una constante nueva acá</b> (ver el procedimiento en
 * el javadoc del paquete).
 *
 * <p>La fecha es la de vigencia obligatoria del documento, cuando se conoce: es informativa (catálogo,
 * {@link EffectivePeriod}); una regla entra en vigor por su propio {@code from}, no por esta fecha.</p>
 */
public enum RuleSource {
    /** Manual Técnico SIFEN v150 (por defecto de toda regla). */
    MT_V150("MT v150", null),
    /** Nota técnica 001 del MT v150. */
    NT_001("NT 001", null),
    /** Nota técnica 002 del MT v150. */
    NT_002("NT 002", null),
    /** Nota técnica 003 del MT v150. */
    NT_003("NT 003", null),
    /** Nota técnica 004 del MT v150. */
    NT_004("NT 004", null),
    /** Nota técnica 005 del MT v150. */
    NT_005("NT 005", null),
    /** Nota técnica 006 del MT v150. */
    NT_006("NT 006", null),
    /** Nota técnica 007 del MT v150. */
    NT_007("NT 007", null),
    /** Nota técnica 008 del MT v150. */
    NT_008("NT 008", null),
    /** Nota técnica 009 del MT v150. */
    NT_009("NT 009", null),
    /** Nota técnica 010 del MT v150. */
    NT_010("NT 010", null),
    /** Nota técnica 011 del MT v150. */
    NT_011("NT 011", null),
    /** Nota técnica 012 del MT v150. */
    NT_012("NT 012", null),
    /** NT 013 (base exenta por ítem, {@code dBasExe}): obligatoria en producción desde el 2023-05-21. */
    NT_013("NT 013", LocalDate.of(2023, 5, 21)),
    /** Nota técnica 014 del MT v150. */
    NT_014("NT 014", null),
    /** Nota técnica 015 del MT v150. */
    NT_015("NT 015", null),
    /** Nota técnica 016 del MT v150. */
    NT_016("NT 016", null),
    /** Nota técnica 017 del MT v150. */
    NT_017("NT 017", null),
    /** Nota técnica 018 del MT v150. */
    NT_018("NT 018", null),
    /** Nota técnica 019 del MT v150. */
    NT_019("NT 019", null),
    /** Nota técnica 020 del MT v150. */
    NT_020("NT 020", null),
    /** Nota técnica 021 del MT v150. */
    NT_021("NT 021", null),
    /** Nota técnica 022 del MT v150. */
    NT_022("NT 022", null),
    /** Nota técnica 023 del MT v150. */
    NT_023("NT 023", null),
    /** Nota técnica 024 del MT v150. */
    NT_024("NT 024", null),
    /** Nota técnica 025 del MT v150. */
    NT_025("NT 025", null),
    /** Nota técnica 026 del MT v150. */
    NT_026("NT 026", null),
    /** Nota técnica 027 del MT v150. */
    NT_027("NT 027", null),
    /** Guía de Pruebas de e-Kuatia, febrero de 2026 (leyenda del primer ítem en pruebas). */
    GUIA_PRUEBAS_2026("Guía de Pruebas 2026-02", null),
    /** Regla propia de la librería: guarda de un defecto del modelo o de la generación (E0-16, E2-03…). */
    JBS("Regla propia de la librería", null);

    private final String nombre;
    private final LocalDate fecha;

    RuleSource(String nombre, LocalDate fecha) {
        this.nombre = nombre;
        this.fecha = fecha;
    }

    /**
     * Devuelve el nombre para el catálogo ({@code NT 023}).
     *
     * @return el nombre.
     */
    public String getName() {
        return nombre;
    }

    /**
     * Devuelve la fecha de vigencia obligatoria del documento, si se conoce.
     *
     * @return la fecha, o {@code null}.
     */
    public LocalDate getDate() {
        return fecha;
    }
}
