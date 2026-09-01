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

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.validator.routines.EmailValidator;

/**
 * Funciones utiles
 *
 * @author Jorge Enciso
 */
public class Fn {

    private Fn() {
    }

    /**
     * Verifica si un valor "obj" se encuentra en una lista de variables
     *
     * @param obj valor buscado
     * @param list lista de valores.
     * @return verdadero si encuentra y falso si no.
     */
    public static boolean inList(Object obj, Object... list) {
        for (Object e : list) {
            if (Objects.equals(obj, e)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica si un valor "obj" se encuentra en una lista de variables
     *
     * @param obj valor buscado
     * @param list lista de valores.
     * @return verdadero si encuentra y falso si no.
     */
    public static boolean inList(String obj, String... list) {
        for (Object e : list) {
            if (Objects.equals(obj, e)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica si un valor "obj" se encuentra en una lista de variables
     *
     * @param obj valor buscado
     * @param list lista de valores.
     * @return verdadero si encuentra y falso si no.
     */
    public static boolean inArrayInteger(Integer obj, int... list) {
        for (Object e : list) {
            if (obj.equals(e)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Devuelve {@code value1} si la condición es verdadera y {@code value2} si no
     * (equivalente a un operador ternario).
     *
     * @param <T> tipo de los valores.
     * @param condition condición a evaluar.
     * @param value1 valor si la condición es verdadera.
     * @param value2 valor si la condición es falsa.
     * @return uno de los dos valores según la condición.
     */
    public static <T> T iif(Boolean condition, T value1, T value2) {
        if (condition == null || condition) {
            return value1;
        }
        return value2;
    }

    /**
     * Busca un objeto en una matriz y si encuentra devuelve el nro. de elemento
     *
     * @param matrix
     * @param search
     * @return nro. de elemento si encuentra el objeto
     */
    public static Integer findInMatrix(Object[] matrix, Object search) {
        int posicion = -1;
        for (int i = 0; i < matrix.length; i++) {
            if (matrix[i] == search) {
                posicion = i;
                break;
            }
        }
        return posicion;
    }

    /**
     * Busca un objeto en una matriz y si encuentra devuelve el nro. de elemento
     *
     * @param matrix
     * @param search
     * @param caseSensitive
     * @return nro. de elemento si encuentra el objeto
     */
    public static Integer findInMatrix(String[] matrix, String search, Boolean caseSensitive) {
        int posicion = -1;
        for (int i = 0; i < matrix.length; i++) {
            if (caseSensitive) {
                if (matrix[i].trim().equals(search)) {
                    posicion = i;
                    break;
                }
            } else {
                if (matrix[i].trim().equalsIgnoreCase(search)) {
                    posicion = i;
                    break;
                }
            }
        }
        return posicion;
    }

    /**
     * Convierte a verdadero o falso (0 falso, 1 verdadero)
     *
     * @param value puede ser una variable numerica o una cadena.
     * @return verdadero o falso.
     */
    public static Boolean toLogical(Object value) {
        if (value == null) {
            return false;
        } else if (value instanceof Boolean) {
            return (Boolean) value;
        } else if ("1".equals(value.toString())) {
            return true;
        } else if ("true".equalsIgnoreCase(value.toString())) {
            return true;
        } else if ("0".equals(value.toString())) {
            return false;
        } else if ("false".equalsIgnoreCase(value.toString())) {
            return false;
        } else if ("".equals(value.toString())) {
            return false;
        }
        return false;
    }

    /**
     * Devuelve un valor alternativo (alternateValue) si el valor dado (value)
     * es nulo.
     *
     * @param <T>
     * @param value valor que devuelve si no es nulo
     * @param alternateValue valor que devuelve si "value" es nulo
     * @return valor (value si no es nulo) valor alternativo (alternateValue si
     * value es nulo)
     */
    public static <T> T nvl(T value, T alternateValue) {
        if (value == null) {
            return alternateValue;
        }
        return value;
    }

    /**
     * Convierte de un array tipo byte a un string hexadecimal
     *
     * @param bytes valor a traducir.
     * @return array tipo byte a un string hexadecimal
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte byt : bytes) {
            result.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }

    /**
     * Convierte un texto hexadecimal a un array tipo bytes
     *
     * @param hexText texto hexadecimal
     * @return texto hexadecimal a un array tipo bytes
     */
    public static byte[] hexToByte(String hexText) {
        int len = hexText.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexText.charAt(i), 16) << 4)
                    + Character.digit(hexText.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Convierte un datos codificado en base 64 a otro equivalente de tipo byte
     *
     * @param encrypted64
     * @return
     */
    public static byte[] base64ToBytes(String encrypted64) {
        return Base64.getDecoder().decode(encrypted64);
    }

    /**
     * Códifica un dato tipo byte[] a base 64
     *
     * @param text datos tipo byte[]
     * @return tipo byte[] a base 64
     */
    public static String bytesToBase64(byte[] text) {
        return Base64.getEncoder().encodeToString(text);
    }

    /**
     * Códifica un dato tipo byte[] a base 64 tipo url
     *
     * @param text datos tipo byte[]
     * @return tipo byte[] a base 64
     */
    public static String bytesToBase64Url(byte[] text) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(text);
    }

    /**
     * Construye un mapa de parámetros a partir de pares clave-valor consecutivos.
     *
     * @param params pares (clave, valor) consecutivos.
     * @return mapa de parámetros.
     */
    public static Map<String, Object> queryParams(Object... params) {
        Map<String, Object> result = new HashMap();
        int c = 0;
        Object key = null;
        for (Object param : params) {
            c++;
            if (c % 2 != 0) {
                key = param;
            } else if (key != null && !key.toString().isEmpty()) {
                result.put(key.toString(), param);
            }
        }
        return result;
    }

    /**
     * Convierte de numero a string utilizando una mascara para el efecto
     *
     * @param value valor numerico
     * @param mask mascara (ver en la documentación de DecimalFormat)
     * @return valor convertido a string.
     */
    public static String numberToString(Object value, String mask) {
        if (value == null) {
            return "";
        }
        String result;
        if (!Fn.nvl(mask, "").isEmpty()) {
            DecimalFormat df = new DecimalFormat(mask);
            result = df.format(value);
        } else {
            DecimalFormat df = new DecimalFormat("###");
            result = df.format(value);
        }
        return result;
    }

    /**
     * Convertir un objeto a un string con un formato
     *
     * @param value valor del objeto.
     * @return objeto a un string con un formato
     */
    public static String getValueFormatted(Object value) {
        if (value == null) {
            return "";
        }
        return getValueFormatted(value, null);
    }

    /**
     * Convertir un objeto a un string con un formato
     *
     * @param value valor del objeto.
     * @param format formato
     * @return objeto a un string con un formato
     */
    public static String getValueFormatted(Object value, String format) {
        if (value == null) {
            return "";
        }
        String result = "";
        try {
            if (value instanceof Number) {
                if (format == null) {
                    format = "##,###,###,##0.00";
                }
                result = Fn.numberToString(value, format);
            } else if (value instanceof Timestamp) {
                if (Fn.nvl(format, "").isEmpty()) {
                    format = "dd/MM/yyyy";
                }
                result = Dates.toString((Timestamp) value, format);
            } else if (value instanceof LocalDateTime) {
                if (Fn.nvl(format, "").isEmpty()) {
                    format = "dd/MM/yyyy";
                }
                result = LocalDates.toString((LocalDateTime) value, format);
            } else if (value instanceof String
                    && !Strings.isNullorEmpty(format)
                    && !Strings.isNullorEmpty((String) value)) {
                //Formatear valor alfanumerico según mascara
                int c = 0;
                for (int i = 0; i < format.length(); i++) {
                    if (c >= ((String) value).length()) {
                        break;
                    }
                    if ("-,. (){}:".contains(Strings.substr(format, i, 1))) {
                        result += Strings.substr(format, i, 1);
                    } else {
                        result += Strings.substr((String) value, c, 1);
                        c++;
                    }
                }
            } else {
                result = value.toString();
            }
        } catch (Exception exp) {
            return "";
        }
        return nvl(result, "").trim();
    }

    /**
     * Indica si el valor es <b>una sola</b> dirección de correo con formato
     * válido.
     *
     * <p>A diferencia de {@link #isEmailValid(String)}, que acepta una lista
     * separada por {@code ;}, acá cualquier separador invalida el valor. Es lo
     * que corresponde cuando la dirección identifica a alguien —el correo de un
     * usuario— y no es una lista de destinatarios: con dos direcciones en el
     * campo no habría a cuál escribirle ni cuál dar por verificada.</p>
     *
     * @param emailAddress dirección de correo, puede ser nula.
     * @return verdadero si es exactamente una dirección con formato válido.
     */
    public static boolean isSingleEmailValid(String emailAddress) {
        if (emailAddress == null) {
            return false;
        }
        String value = emailAddress.trim();
        if (value.isEmpty()) {
            return false;
        }
        // Un separador o un espacio interno delatan más de una dirección, aunque
        // cada una por separado fuera válida.
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == ';' || c == ',' || Character.isWhitespace(c)) {
                return false;
            }
        }
        return EmailValidator.getInstance().isValid(value);
    }

    /**
     * Indica si una dirección de correo tiene formato válido.
     *
     * <p>Acepta <b>varias direcciones separadas por {@code ;}</b>, y es válido
     * solo si todas lo son. Para un campo que deba contener una única dirección,
     * usar {@link #isSingleEmailValid(String)}.</p>
     *
     * @param emailAdress dirección de correo.
     * @return verdadero si el formato es válido, falso si no.
     */
    public static boolean isEmailValid(String emailAdress) {
        if (!emailAdress.contains(";")) {
            return EmailValidator.getInstance().isValid(emailAdress);
        }
        boolean result = true;
        String[] mails = emailAdress.split(";");
        for (String email : mails) {
            if (!EmailValidator.getInstance().isValid(email)) {
                result = false;
                break;
            }
        }
        return result;
    }

    /**
     * Devuelve verdadero si el valor que se pasa como parametro es nulo o vacio
     *
     * @param lista lista de valor
     * @return verdadero si "string" es nulo o vacio.
     */
    public static Boolean isNullorEmpty(List lista) {
        if (lista != null) {
            return lista.isEmpty();
        }
        return lista == null;
    }

    /**
     * Devuelve verdadero si el valor que se pasa como parametro es nulo o vacio
     *
     * @param map lista de valor
     * @return verdadero si "string" es nulo o vacio.
     */
    public static Boolean isNullorEmpty(Map map) {
        if (map != null) {
            return map.isEmpty();
        }
        return map == null;
    }

    /**
     * Devuelve verdadero si una dirección IP coincide con alguna de las
     * entradas de una lista separadas por comas.
     *
     * <p>Es la semántica que el sistema ya usaba en el filtro de peticiones
     * para los parámetros {@code IP_REQUEST_ALLOWED} e
     * {@code IP_REQUEST_NOT_ALLOWED}, extraída acá para que haya una sola
     * implementación:</p>
     *
     * <ul>
     * <li>Lista nula, vacía o compuesta solo de espacios y comas ⇒ <b>coincide
     * siempre</b>. Es el equivalente de {@code 0.0.0.0}: sin restricción
     * declarada, no hay nada que restringir.</li>
     * <li>Una entrada {@code 0.0.0.0} o {@code *} ⇒ coincide siempre.</li>
     * <li>Coincidencia exacta.</li>
     * <li>Comodín por octetos: ver {@link #ipMatchPattern(String, String)}.</li>
     * </ul>
     *
     * @param ipRequest dirección IP a evaluar.
     * @param ipList lista de direcciones o patrones separados por coma.
     * @return verdadero si la dirección está comprendida en la lista.
     */
    public static boolean ipMatch(String ipRequest, String ipList) {
        if (ipList == null || ipList.trim().isEmpty()) {
            return true;
        }
        return ipMatchAny(ipRequest, ipList.split(","));
    }

    /**
     * Variante de {@link #ipMatch(String, String)} que recibe la lista ya
     * separada, para quien la tiene partida de antemano.
     *
     * <p>Un arreglo nulo, vacío o cuyos elementos estén todos en blanco
     * equivale a la lista vacía y por lo tanto coincide siempre.</p>
     *
     * @param ipRequest dirección IP a evaluar.
     * @param ipList direcciones o patrones.
     * @return verdadero si la dirección está comprendida en la lista.
     */
    public static boolean ipMatchAny(String ipRequest, String... ipList) {
        //Una lista sin ninguna entrada útil no es una restricción.
        if (isIpListEmpty(ipList)) {
            return true;
        }
        return ipListed(ipRequest, ipList);
    }

    /**
     * Devuelve verdadero si una dirección IP está nombrada por alguna de las
     * entradas de la lista.
     *
     * <p>Se diferencia de {@link #ipMatchAny(String, String...)} en <b>qué
     * significa la lista vacía</b>, que es lo contrario según para qué se use
     * la lista. En una lista de <b>permitidos</b>, no declarar nada quiere
     * decir «todos pasan»; en una de <b>denegados</b>, no declarar nada quiere
     * decir «no se deniega a nadie». Por eso acá una lista nula, vacía o en
     * blanco devuelve <b>falso</b>: nadie está nombrado.</p>
     *
     * @param ipRequest dirección IP a evaluar.
     * @param ipList direcciones o patrones.
     * @return verdadero si la dirección está nombrada en la lista.
     */
    public static boolean ipListed(String ipRequest, String... ipList) {
        if (ipList == null) {
            return false;
        }
        for (String patron : ipList) {
            if (patron == null || patron.trim().isEmpty()) {
                continue;
            }
            if (ipMatchPattern(ipRequest, patron.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Devuelve verdadero si una lista de direcciones no tiene ninguna entrada
     * util: nula, sin elementos, o con todos los elementos en blanco.
     *
     * @param ipList direcciones o patrones.
     * @return verdadero si la lista no declara nada.
     */
    private static boolean isIpListEmpty(String... ipList) {
        if (ipList == null || ipList.length == 0) {
            return true;
        }
        for (String patron : ipList) {
            if (patron != null && !patron.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Devuelve verdadero si una dirección IP coincide con un patrón.
     *
     * <p>El patrón se compara por octetos <b>de derecha a izquierda</b>: un
     * {@code *} siempre pasa, y un {@code 0} pasa mientras no se haya
     * encontrado todavía un octeto significativo. Por eso {@code 192.168.*} y
     * {@code 192.168.0.0} significan lo mismo —cualquier dirección de esa
     * red—, mientras que {@code 10.0.0.5} exige la dirección exacta: el
     * {@code 0} del medio ya no es comodín porque a su derecha hay un octeto
     * significativo.</p>
     *
     * <p>Los octetos se alinean por la <b>izquierda</b>, de manera que un
     * patrón más corto que la dirección compara solo sus primeros octetos.</p>
     *
     * <h2>Un patrón que queda todo en comodines coincide</h2>
     *
     * <p>{@code *.*.*.*}, {@code 0.*.*.*}, {@code *.168.1.5} y {@code 0.0}
     * coinciden con cualquier dirección, que es lo que dicen. <b>La versión
     * anterior de esta lógica —la que vivía duplicada dentro del filtro de
     * peticiones— no los aceptaba con ninguna dirección</b>: marcaba la
     * coincidencia solo al comparar literalmente el primer octeto, y si ese
     * octeto era comodín nunca llegaba a marcarla. O sea que esas entradas eran
     * <b>inertes</b>: no permitían ni denegaban a nadie.</p>
     *
     * <p>Se corrigió a propósito, porque una entrada que no hace nada de lo que
     * dice es peor que cualquiera de las dos alternativas. Vale saber en qué se
     * nota: en una lista de <b>permitidos</b> esas entradas pasan de inertes a
     * permisivas, y en una de <b>denegados</b>, de inertes a activas. Nadie
     * puede tenerlas hoy como única entrada de la lista de permitidos —su
     * instalación no dejaría entrar a nadie—, pero conviene mirar las listas
     * antes de actualizar.</p>
     *
     * <p><b>Nunca lanza excepción.</b> Ante una entrada mal formada, una
     * dirección desconocida o un patrón con más octetos que la dirección
     * —el caso real es un patrón IPv4 contra el bucle local IPv6
     * {@code 0:0:0:0:0:0:0:1}, que al partir por punto da un solo elemento—
     * la respuesta es <b>no coincide</b>. Es lo que corresponde en un control
     * de acceso: lo que no se puede evaluar, no se autoriza.</p>
     *
     * @param ipRequest dirección IP a evaluar.
     * @param ipPattern dirección o patrón contra el cual evaluarla.
     * @return verdadero si la dirección coincide con el patrón.
     */
    public static boolean ipMatchPattern(String ipRequest, String ipPattern) {
        if (ipPattern == null) {
            return false;
        }
        String patron = ipPattern.trim();
        //Todas las direcciones habilitadas, incluso una de origen desconocido:
        //es la forma explícita de decir "sin restricción".
        if (inList(patron, "0.0.0.0", "*")) {
            return true;
        }
        if (ipRequest == null || ipRequest.trim().isEmpty()) {
            return false;
        }
        String ip = ipRequest.trim();
        if (ip.equals(patron)) {
            return true;
        }
        String[] partesPatron = patron.split("\\.");
        String[] partesIp = ip.split("\\.");
        //Un patrón sin un solo octeto (por ejemplo "..." o ".") no es un
        //comodín: es basura, y no autoriza nada.
        if (partesPatron.length == 0) {
            return false;
        }
        boolean esComodin = true;
        for (int i = partesPatron.length - 1; i >= 0; i--) {
            if (partesPatron[i].equals("*")) {
                continue;
            }
            if (esComodin && partesPatron[i].equals("0")) {
                continue;
            }
            esComodin = false;
            //El patrón tiene más octetos que la dirección: no hay con qué
            //comparar, así que no coincide.
            if (i >= partesIp.length) {
                return false;
            }
            if (!partesPatron[i].equals(partesIp[i])) {
                return false;
            }
            if (i == 0) {
                return true;
            }
        }
        //El patrón era todo comodines (por ejemplo "*.*.*.*").
        return true;
    }
}
