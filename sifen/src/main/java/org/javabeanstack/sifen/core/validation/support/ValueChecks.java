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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Pattern;
import org.javabeanstack.sifen.core.validation.ValidationContext;
import org.javabeanstack.sifen.internal.util.SifenUtil;

/**
 * Comprobaciones de formato comunes a los grupos de reglas (§3.3): largos, dígitos, números del MT
 * ({@code N e p(0-d)}), dígito verificador módulo 11, CDC y correo. Todas son <b>null-safe</b>: un
 * dato ausente cumple el formato (la ausencia la juzga la regla de presencia, si la hay), y ninguna
 * lanza.
 *
 * <p>Público para escribir reglas (D33): los conjuntos del catálogo y los propios del consumidor lo
 * usan desde sus chequeos.</p>
 */
public final class ValueChecks {
    /** Patrón {@code tEmail} del XSD (el mismo de {@code SifenTestEnv}). */
    private static final Pattern EMAIL = Pattern.compile(
            "([0-9a-zA-Z#$%]([-.\\w]*[0-9a-zA-Z#$%'\\.\\-_])*@([0-9a-zA-Z][-\\w]*[0-9a-zA-Z]\\.)+[a-zA-Z]{2,9})");
    private static final Pattern DIGITOS = Pattern.compile("[0-9]+");

    private ValueChecks() {
    }

    /**
     * Indica si un dato está presente (ver {@link ValidationContext#present(Object)}).
     *
     * @param valor el dato.
     * @return {@code true} si está presente.
     */
    public static boolean present(Object valor) {
        return ValidationContext.present(valor);
    }

    /**
     * {@code true} si el texto está ausente o tiene entre {@code min} y {@code max} caracteres.
     *
     * @param texto texto a comprobar; {@code null} cumple el formato.
     * @param min largo mínimo.
     * @param max máximo.
     * @return {@code true} si se cumple.
     */
    public static boolean length(String texto, int min, int max) {
        return texto == null || (texto.length() >= min && texto.length() <= max);
    }

    /**
     * {@code true} si el texto está ausente o son solo dígitos, entre {@code min} y {@code max}.
     *
     * @param texto texto a comprobar; {@code null} cumple el formato.
     * @param min largo mínimo.
     * @param max máximo.
     * @return {@code true} si se cumple.
     */
    public static boolean digits(String texto, int min, int max) {
        return texto == null || (DIGITOS.matcher(texto).matches() && texto.length() >= min && texto.length() <= max);
    }

    /**
     * {@code true} si el texto está presente y son solo dígitos.
     *
     * @param texto texto a comprobar; {@code null} cumple el formato.
     * @return {@code true} si se cumple.
     */
    public static boolean digitsOnly(String texto) {
        return texto != null && DIGITOS.matcher(texto).matches();
    }

    /**
     * Cantidad de cifras del valor absoluto ({@code 0} tiene 1).
     *
     * @param valor dato a comprobar.
     * @return el código del MT, o 0 si falta.
     */
    public static int digitCount(long valor) {
        return Long.toString(Math.abs(valor)).length();
    }

    /**
     * {@code true} si {@code 0 ≤ valor} y tiene a lo sumo {@code max} cifras.
     *
     * @param valor dato a comprobar.
     * @param max máximo.
     * @return {@code true} si se cumple.
     */
    public static boolean maxDigits(long valor, int max) {
        return valor >= 0 && digitCount(valor) <= max;
    }

    /**
     * Formato numérico del MT {@code N 1-e p(0-d)}: hasta {@code enteros} cifras enteras y hasta
     * {@code decimales} decimales (la escala del {@link BigDecimal}: es la que se escribe en el XML).
     *
     * @param valor     número; {@code null} cumple.
     * @param enteros   cifras enteras máximas.
     * @param decimales decimales máximos.
     * @return {@code true} si está ausente o cumple.
     */
    public static boolean decimal(BigDecimal valor, int enteros, int decimales) {
        if (valor == null) {
            return true;
        }
        int escala = Math.max(valor.scale(), 0);
        int cifrasEnteras = valor.abs().setScale(0, java.math.RoundingMode.DOWN).toPlainString().length();
        return escala <= decimales && cifrasEnteras <= enteros;
    }

    /**
     * {@code true} si está ausente o es mayor que cero.
     *
     * @param valor dato a comprobar.
     * @return {@code true} si se cumple.
     */
    public static boolean positive(BigDecimal valor) {
        return valor == null || valor.signum() > 0;
    }

    /**
     * {@code true} si está ausente o no es negativo.
     *
     * @param valor dato a comprobar.
     * @return {@code true} si se cumple.
     */
    public static boolean nonNegative(BigDecimal valor) {
        return valor == null || valor.signum() >= 0;
    }

    /**
     * {@code true} si está ausente o tiene a lo sumo {@code max} cifras y no es negativo.
     *
     * @param valor dato a comprobar.
     * @param max máximo.
     * @return {@code true} si se cumple.
     */
    public static boolean maxDigits(BigInteger valor, int max) {
        return valor == null || (valor.signum() >= 0 && valor.toString().length() <= max);
    }

    /**
     * Dígito verificador módulo 11 de un RUC o CDC ({@code SifenUtil.generateDv}).
     *
     * @param numero RUC o los 43 primeros dígitos del CDC.
     * @return el DV, o {@code null} si el número no es solo dígitos (no se puede calcular).
     */
    public static String computeDV(String numero) {
        if (!digitsOnly(numero)) {
            return null;
        }
        return SifenUtil.generateDv(numero);
    }

    /**
     * {@code true} si el DV corresponde al RUC, o si no se puede juzgar (RUC ausente o con letras, DV ausente: otras reglas lo reportan). /
     *
     * @param ruc RUC sin DV.
     * @param dvInformado dígito verificador informado.
     * @return {@code true} si se cumple.
     */
    public static boolean validDV(String ruc, String dvInformado) {
        String esperado = computeDV(ruc);
        if (esperado == null || dvInformado == null || dvInformado.isBlank()) {
            return true;
        }
        return esperado.equals(dvInformado.trim());
    }

    /**
     * {@code true} si es un CDC: 44 dígitos con el DV módulo 11 de los 43 primeros.
     *
     * @param cdc código de control.
     * @return {@code true} si se cumple.
     */
    public static boolean validCdc(String cdc) {
        if (cdc == null || cdc.length() != 44 || !digitsOnly(cdc)) {
            return false;
        }
        return SifenUtil.generateDv(cdc.substring(0, 43)).equals(cdc.substring(43));
    }

    /**
     * Caracteres del tipo {@code tdNumDocId} del XSD de SIFEN (v150, {@code [0-9A-Za-z\-]{1,20}}): letras,
     * dígitos y guion. El largo lo controla la regla de largo de cada campo.
     */
    private static final java.util.regex.Pattern NUMERO_DOCUMENTO = java.util.regex.Pattern.compile("[0-9A-Za-z\\-]*");

    /**
     * Indica si un número de documento de identidad tiene el formato del XSD ({@code tdNumDocId}:
     * {@code [0-9A-Za-z\-]{1,20}}; plan JBSSIF, E3-26): SIFEN rechaza con 0160 un punto, un espacio o
     * cualquier otro carácter (probado en vivo: «1.234.567» rechazado, ceros a la izquierda y letras
     * aprobados).
     *
     * @param numero número; {@code null} se acepta (la falta la informa otra regla).
     * @return {@code true} si es nulo o solo tiene caracteres admitidos (el largo lo mira otra regla).
     */
    public static boolean validDocumentNumber(String numero) {
        return numero == null || NUMERO_DOCUMENTO.matcher(numero).matches();
    }

    /**
     * {@code true} si está ausente o tiene formato de correo y entre 3 y 80 caracteres.
     *
     * @param correo correo electrónico.
     * @return {@code true} si se cumple.
     */
    public static boolean validEmail(String correo) {
        return correo == null || (correo.length() >= 3 && correo.length() <= 80 && EMAIL.matcher(correo).matches());
    }
}
