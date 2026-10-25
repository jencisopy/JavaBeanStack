/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
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
package org.javabeanstack.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Convierte un texto al tipo de dato solicitado.
 *
 * <p>Es la pieza que llena el hueco entre una fuente de datos que solo tiene
 * texto (un atributo XML, una celda de planilla declarada como texto, un
 * parámetro de configuración) y un atributo tipado del modelo: los
 * asignadores de valor del framework ({@code IDataRow.setValue}) no convierten
 * tipos, con lo cual un {@code String} sobre un atributo {@code Long} falla.</p>
 *
 * <p>Tipos soportados (ver {@link #isSupported(Class)}):</p>
 * <ul>
 * <li>{@link String} — se devuelve tal cual, sin recortar espacios.</li>
 * <li>{@link Character} y {@code char} — el primer carácter del texto, una
 * vez recortados los espacios de los extremos (como en todos los tipos que no
 * son {@link String}).</li>
 * <li>{@link Boolean} y {@code boolean} — {@code T}/{@code F},
 * {@code true}/{@code false}, {@code 1}/{@code 0}, sin distinguir mayúsculas.
 * No se aceptan {@code S}/{@code N} ni {@code yes}/{@code no}.</li>
 * <li>{@link Short}, {@link Integer}, {@link Long}, {@link BigInteger} y sus
 * primitivos — enteros exactos: un texto con decimales se rechaza.</li>
 * <li>{@link BigDecimal}, {@link Double}, {@link Float} y sus primitivos.</li>
 * <li>{@link LocalDate}, {@link LocalDateTime} y {@link Date} — en formato
 * ISO-8601 por defecto ({@code 2026-10-25}, {@code 2026-10-25T13:45:00}) o con
 * los formateadores recibidos. {@code LocalDateTime} y {@code Date} aceptan
 * además un texto de solo fecha, que se interpreta a medianoche.</li>
 * <li>Enumeraciones — por el nombre de la constante, sin distinguir
 * mayúsculas.</li>
 * </ul>
 *
 * <p>Reglas generales:</p>
 * <ul>
 * <li>Un texto nulo devuelve {@code null}.</li>
 * <li>Un texto vacío (o solo espacios) devuelve {@code null} para todo tipo
 * que no sea {@link String}; para {@link String} devuelve el texto tal cual.
 * Quien necesite que el vacío sea nulo también en texto lo resuelve en su
 * capa (por ejemplo {@code IXmlRowMapper.emptyAsNull}).</li>
 * <li>Un texto que no se puede convertir lanza
 * {@link IllegalArgumentException} con el texto y el tipo; lo mismo un tipo no
 * soportado. Quien llama decide si eso es un error de la fila o del
 * proceso.</li>
 * </ul>
 *
 * <p>Clase sin estado: todos sus métodos son estáticos y seguros entre
 * hilos.</p>
 *
 * @author Jorge Enciso
 */
public final class ValueConverter {

    /**
     * Mensaje de error cuando el texto no corresponde al tipo solicitado.
     */
    private static final String ERROR_CONVERSION = "\"%s\" no se puede convertir a %s";

    private ValueConverter() {
    }

    /**
     * Convierte un texto al tipo solicitado interpretando las fechas en
     * formato ISO-8601.
     *
     * @param text texto a convertir.
     * @param type tipo de dato esperado.
     * @return el valor convertido, o {@code null} si el texto es nulo o vacío
     * y el tipo no es {@link String}.
     * @throws IllegalArgumentException si el texto no corresponde al tipo o si
     * el tipo no está soportado.
     */
    public static Object fromString(String text, Class<?> type) {
        return fromString(text, type, null, null);
    }

    /**
     * Convierte un texto al tipo solicitado.
     *
     * @param text texto a convertir.
     * @param type tipo de dato esperado.
     * @param dateFormatter formateador de fecha; si es nulo se usa
     * {@link DateTimeFormatter#ISO_LOCAL_DATE}.
     * @param dateTimeFormatter formateador de fecha y hora; si es nulo se usa
     * {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}.
     * @return el valor convertido, o {@code null} si el texto es nulo o vacío
     * y el tipo no es {@link String}.
     * @throws IllegalArgumentException si el texto no corresponde al tipo o si
     * el tipo no está soportado.
     */
    public static Object fromString(String text, Class<?> type,
            DateTimeFormatter dateFormatter, DateTimeFormatter dateTimeFormatter) {
        if (type == null) {
            throw new IllegalArgumentException("No se indicó el tipo de dato al que se debe convertir el texto");
        }
        if (text == null) {
            return null;
        }
        Class<?> target = toWrapper(type);
        if (String.class.equals(target)) {
            return text;
        }
        String value = text.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (Character.class.equals(target)) {
            return value.charAt(0);
        }
        if (Boolean.class.equals(target)) {
            return toBoolean(text, value);
        }
        if (isNumber(target)) {
            return toNumber(text, value, target);
        }
        if (LocalDate.class.equals(target)) {
            return toLocalDate(text, value, dateFormatter);
        }
        if (LocalDateTime.class.equals(target)) {
            return toLocalDateTime(text, value, dateFormatter, dateTimeFormatter, LocalDateTime.class);
        }
        if (Date.class.equals(target)) {
            return Dates.toDateTime(toLocalDateTime(text, value, dateFormatter, dateTimeFormatter, Date.class));
        }
        if (target.isEnum()) {
            return toEnum(text, value, target);
        }
        throw error(text, type);
    }

    /**
     * Informa si el tipo dado se puede convertir desde un texto.
     *
     * @param type tipo de dato a evaluar.
     * @return verdadero si {@link #fromString(String, Class)} lo soporta.
     */
    public static boolean isSupported(Class<?> type) {
        if (type == null) {
            return false;
        }
        Class<?> target = toWrapper(type);
        return String.class.equals(target)
                || Character.class.equals(target)
                || Boolean.class.equals(target)
                || isNumber(target)
                || LocalDate.class.equals(target)
                || LocalDateTime.class.equals(target)
                || Date.class.equals(target)
                || target.isEnum();
    }

    /**
     * Devuelve la clase envoltorio de un tipo primitivo; para los demás tipos
     * devuelve el tipo recibido.
     *
     * @param type tipo de dato.
     * @return tipo envoltorio equivalente.
     */
    private static Class<?> toWrapper(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (boolean.class.equals(type)) {
            return Boolean.class;
        }
        if (char.class.equals(type)) {
            return Character.class;
        }
        if (short.class.equals(type)) {
            return Short.class;
        }
        if (int.class.equals(type)) {
            return Integer.class;
        }
        if (long.class.equals(type)) {
            return Long.class;
        }
        if (float.class.equals(type)) {
            return Float.class;
        }
        if (double.class.equals(type)) {
            return Double.class;
        }
        return type;
    }

    /**
     * Informa si el tipo (ya convertido a envoltorio) es uno de los numéricos
     * soportados.
     *
     * @param target tipo envoltorio.
     * @return verdadero si es numérico.
     */
    private static boolean isNumber(Class<?> target) {
        return Short.class.equals(target)
                || Integer.class.equals(target)
                || Long.class.equals(target)
                || BigInteger.class.equals(target)
                || BigDecimal.class.equals(target)
                || Double.class.equals(target)
                || Float.class.equals(target);
    }

    /**
     * Convierte el texto a un valor lógico.
     *
     * @param text texto original (para el mensaje de error).
     * @param value texto sin espacios al principio ni al final.
     * @return verdadero o falso.
     */
    private static Boolean toBoolean(String text, String value) {
        if ("T".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value) || "1".equals(value)) {
            return Boolean.TRUE;
        }
        if ("F".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value) || "0".equals(value)) {
            return Boolean.FALSE;
        }
        throw error(text, Boolean.class);
    }

    /**
     * Convierte el texto a un valor numérico del tipo solicitado. Los tipos
     * enteros exigen un valor exacto: un texto con decimales se rechaza.
     *
     * @param text texto original (para el mensaje de error).
     * @param value texto sin espacios al principio ni al final.
     * @param target tipo envoltorio numérico.
     * @return el número convertido.
     */
    private static Number toNumber(String text, String value, Class<?> target) {
        BigDecimal number;
        try {
            number = new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw error(text, target);
        }
        try {
            if (Short.class.equals(target)) {
                return number.shortValueExact();
            }
            if (Integer.class.equals(target)) {
                return number.intValueExact();
            }
            if (Long.class.equals(target)) {
                return number.longValueExact();
            }
            if (BigInteger.class.equals(target)) {
                return number.toBigIntegerExact();
            }
            if (Double.class.equals(target)) {
                return number.doubleValue();
            }
            if (Float.class.equals(target)) {
                return number.floatValue();
            }
        } catch (ArithmeticException ex) {
            throw error(text, target);
        }
        return number;
    }

    /**
     * Convierte el texto a una fecha.
     *
     * @param text texto original (para el mensaje de error).
     * @param value texto sin espacios al principio ni al final.
     * @param dateFormatter formateador de fecha (puede ser nulo).
     * @return la fecha convertida.
     */
    private static LocalDate toLocalDate(String text, String value, DateTimeFormatter dateFormatter) {
        try {
            return LocalDate.parse(value, Fn.nvl(dateFormatter, DateTimeFormatter.ISO_LOCAL_DATE));
        } catch (RuntimeException ex) {
            throw error(text, LocalDate.class);
        }
    }

    /**
     * Convierte el texto a una fecha y hora. Si el texto no tiene la parte
     * horaria se lo interpreta como una fecha a medianoche.
     *
     * @param text texto original (para el mensaje de error).
     * @param value texto sin espacios al principio ni al final.
     * @param dateFormatter formateador de fecha (puede ser nulo).
     * @param dateTimeFormatter formateador de fecha y hora (puede ser nulo).
     * @param reported tipo que se informa si la conversión falla (el pedido
     * por quien llama, que puede ser {@link Date}).
     * @return la fecha y hora convertida.
     */
    private static LocalDateTime toLocalDateTime(String text, String value,
            DateTimeFormatter dateFormatter, DateTimeFormatter dateTimeFormatter, Class<?> reported) {
        try {
            return LocalDateTime.parse(value, Fn.nvl(dateTimeFormatter, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } catch (RuntimeException ex) {
            //Un texto de solo fecha es válido: la hora es la medianoche.
            try {
                return LocalDate.parse(value, Fn.nvl(dateFormatter, DateTimeFormatter.ISO_LOCAL_DATE)).atStartOfDay();
            } catch (RuntimeException ex2) {
                throw error(text, reported);
            }
        }
    }

    /**
     * Convierte el texto a una constante de la enumeración, sin distinguir
     * mayúsculas.
     *
     * @param text texto original (para el mensaje de error).
     * @param value texto sin espacios al principio ni al final.
     * @param target tipo enumerado.
     * @return la constante de la enumeración.
     */
    private static Object toEnum(String text, String value, Class<?> target) {
        for (Object constant : target.getEnumConstants()) {
            if (((Enum<?>) constant).name().equalsIgnoreCase(value)) {
                return constant;
            }
        }
        throw error(text, target);
    }

    /**
     * Arma la excepción de conversión fallida.
     *
     * @param text texto que no se pudo convertir.
     * @param type tipo de dato esperado.
     * @return la excepción a lanzar.
     */
    private static IllegalArgumentException error(String text, Class<?> type) {
        return new IllegalArgumentException(String.format(ERROR_CONVERSION, text, type.getSimpleName()));
    }
}
