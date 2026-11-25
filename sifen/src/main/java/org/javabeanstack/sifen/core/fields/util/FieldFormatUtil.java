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
package org.javabeanstack.sifen.core.fields.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Formato de importes del DE.
 *
 * <p>Plan JBSSIF, §4.3 (E3-09, D38 e): la línea base compartía un {@code DecimalFormat} estático, que el
 * JDK no garantiza seguro entre hilos (javadoc de {@code DecimalFormat}, «Synchronization»); desde la
 * Fase 4 se crea un formateador por llamada, con la misma configuración, así que el texto no cambia.
 * Los símbolos son los de {@link Locale#ROOT} (dígitos ASCII, signo {@code -}) con el punto decimal: el
 * texto ya no depende de la configuración regional por omisión de la JVM.</p>
 */
public class FieldFormatUtil {

    /**
     * Formatea {@code dRedon}: hasta 4 decimales y 4 enteros, sin separador de miles ni punto decimal
     * si no hay decimales ({@code 0.100} → {@code "0.1"}, {@code 39} → {@code "39"}). Seguro entre hilos.
     *
     * @param val valor.
     * @return el texto del campo.
     */
    public static String formattdCRed(BigDecimal val) {
        return newTdCRedFormat().format(val);
    }

    /** Formateador de {@code dRedon}, uno nuevo por llamada (E3-09). */
    private static DecimalFormat newTdCRedFormat() {
        DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(Locale.ROOT);
        dfs.setDecimalSeparator('.');
        DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(Locale.ROOT);
        format.setDecimalSeparatorAlwaysShown(false);
        format.setGroupingUsed(false);
        format.setMaximumFractionDigits(4);
        format.setMaximumIntegerDigits(4);
        format.setDecimalFormatSymbols(dfs);
        return format;
    }

}
