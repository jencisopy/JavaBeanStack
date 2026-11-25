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

import org.javabeanstack.sifen.core.types.CMondT;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class RedondeoUtil {



    //
    // reglas de redon

    /**
     * En consideración a la Resolución 314 del 2014 (Secretaría de Defensa del Consumidor-SEDECO). Las
     * reglas de redondeo aplican a múltiplos de 50 guaraníes de la siguiente manera:
     * Guaraníes 107.437  | Redondeo | 37 |  107.400
     * Guaraníes  47.789  | Redondeo | 39 |   47.750
     *
     * Observación: Para monedas extranjeras o cualquier otro cálculo que contenga decimales, las reglas
     * de validación aceptarán redondeos de 50 céntimos (por encima o por debajo).
     *
     * @param cMondT
     * @param valor
     * @return
     */
    public static BigDecimal redondeoOficialSET(CMondT cMondT, BigDecimal valor) {

        if (cMondT == CMondT.PYG) {
            return redondeoOficialGuaranies(valor);
        } else {
            return redondeoOficialOtrasMonedas(valor);
        }
    }

    public static BigDecimal redondeoOficialGuaranies(BigDecimal valor) {
        return valor.remainder(BigDecimal.valueOf(50));
    }

    /**
     * Redondeo de moneda extranjera a múltiplos de 0,50 (hacia arriba o hacia abajo, HALF_UP; I3-34): la
     * diferencia {@code valor − redondeado}, con escala fija 2 (plan JBSSIF, §4.3, E0-03).
     *
     * <p>La línea base devolvía escala 3 ({@code 0.100}, {@code 0.000}) y la arrastraba a
     * {@code dTotGralOpe} ({@code 8517.500}) y a {@code dTotalGs}; además, con un total múltiplo de 0,50
     * daba {@code 0.000} y el DE releído —sin redondeo porque {@code dRedon} es 0, §4.2— salía con otro
     * {@code dTotGralOpe} ({@code 3803.500} → {@code 3803.50}) y otro digest.</p>
     *
     * @param value total de la operación ({@code dTotOpe}, 2 decimales).
     * @return el redondeo con signo, escala 2 (positivo: se resta del total).
     */
    public static BigDecimal redondeoOficialOtrasMonedas(BigDecimal value) {
        BigDecimal fiftyCents = new BigDecimal("0.5");
        BigDecimal dividedValue = value.divide(fiftyCents, 0, RoundingMode.HALF_UP);
        BigDecimal roundedValue = dividedValue.multiply(fiftyCents);
        return roundedValue.subtract(value).negate().setScale(2, RoundingMode.HALF_UP);
    }
}
