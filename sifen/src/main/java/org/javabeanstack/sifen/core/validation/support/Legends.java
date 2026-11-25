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
package org.javabeanstack.sifen.core.validation.support;

/**
 * Leyendas del ambiente de pruebas (DEC-01, decisión del usuario Q14 del 2026-11-24): D105 y
 * JBS-E-70 aceptan las dos; JBS-D-13 rechaza las dos en producción.
 */
public final class Legends {
    /** Nombre del emisor exigido en el ambiente de pruebas por el MT (12.4, D105). */
    public static final String MT = "DE generado en ambiente de prueba - sin valor comercial ni fiscal";
    /** Leyenda de la Guía de Pruebas 2026-02 (emisor y primer ítem). */
    public static final String GUIA_PRUEBAS = "DOCUMENTO ELECTRÓNICO SIN VALOR COMERCIAL NI FISCAL - GENERADO EN AMBIENTE DE PRUEBA";

    private Legends() {
    }

    /**
     * Indica si el texto es una de las dos leyendas del ambiente de pruebas.
     *
     * @param texto              texto; {@code null} da {@code false}.
     * @param ignorarMayusculas  compara sin distinguir mayúsculas y sin espacios en los extremos.
     * @return {@code true} si es una leyenda.
     */
    public static boolean isTestLegend(String texto, boolean ignorarMayusculas) {
        if (texto == null) {
            return false;
        }
        if (ignorarMayusculas) {
            String t = texto.trim();
            return MT.equalsIgnoreCase(t) || GUIA_PRUEBAS.equalsIgnoreCase(t);
        }
        return MT.equals(texto) || GUIA_PRUEBAS.equals(texto);
    }
}
