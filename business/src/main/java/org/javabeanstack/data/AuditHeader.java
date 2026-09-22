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
package org.javabeanstack.data;

import org.javabeanstack.util.Fn;
import org.javabeanstack.util.Strings;

/**
 * Reglas de armado de la cabecera de las tablas de auditoría (<code>aud_*</code>).
 *
 * <p>
 * Existe para que la composición del <b>dispositivo</b> viva en un solo lugar.
 * La regla del sello de auditoría (<code>appuser</code>) llegó a estar
 * triplicada entre el framework, la capa intermedia y la aplicación, y costó
 * una corrección en los tres repositorios; esta se define acá y la consume
 * únicamente {@link AbstractDAO#auditSave}, que es el único escritor de la
 * cabecera.
 * </p>
 *
 * <p>
 * La cabecera separa dos cosas que históricamente compartían la columna
 * <code>maquina</code>:
 * </p>
 * <ul>
 * <li><b><code>iprequest</code></b> (50): la IP del request que originó la
 * grabación, tal cual, sin adornos.</li>
 * <li><b><code>maquina</code></b> (150): el <i>dispositivo</i>, un texto
 * legible por sí solo que arma {@link #formatDevice(String, String, int)}.</li>
 * </ul>
 *
 * @author Jorge Enciso
 */
public class AuditHeader {

    /**
     * Longitud de la columna <code>maquina</code> a partir de la migración
     * V038 del plan AUDDEV. Es el largo máximo del texto que devuelve
     * {@link #formatDevice(String, String, int)} en su versión de dos
     * argumentos.
     */
    public static final int DEVICE_MAX_LENGTH = 150;

    /**
     * Constructor privado: la clase es un contenedor de métodos estáticos.
     */
    private AuditHeader() {
    }

    /**
     * Arma el valor del dispositivo con el largo de la columna
     * <code>maquina</code> ({@link #DEVICE_MAX_LENGTH}).
     *
     * @param deviceName nombre del dispositivo (el <code>uuidDevice</code> del
     * token) o nulo/vacío si la sesión no viene de un token.
     * @param ip IP del request, o nulo/vacío si no se conoce.
     * @return el dispositivo formateado, o nulo si no hay nombre ni IP.
     */
    public static String formatDevice(String deviceName, String ip) {
        return formatDevice(deviceName, ip, DEVICE_MAX_LENGTH);
    }

    /**
     * Arma el valor que se guarda en la columna <code>maquina</code> de la
     * auditoría a partir del nombre del dispositivo y de la IP del request.
     *
     * <p>
     * Formato:
     * </p>
     * <table border="1">
     * <caption>Casos</caption>
     * <tr><th>nombre</th><th>ip</th><th>resultado</th></tr>
     * <tr><td><code>tablet-vendedor</code></td><td><code>10.0.0.1</code></td>
     * <td><code>tablet-vendedor (10.0.0.1)</code></td></tr>
     * <tr><td>vacío (sesión por login)</td><td><code>10.0.0.1</code></td>
     * <td><code>10.0.0.1</code></td></tr>
     * <tr><td><code>tablet-vendedor</code></td><td>vacío</td>
     * <td><code>tablet-vendedor</code></td></tr>
     * <tr><td>vacío</td><td>vacío</td><td><code>null</code></td></tr>
     * </table>
     *
     * <p>
     * <b>Sin origen se devuelve <code>null</code>, no cadena vacía</b>: en la
     * auditoría no es lo mismo "no se sabe" que "vacío" (decisión D9 del plan
     * AUDDEV).
     * </p>
     *
     * <p>
     * <b>Qué se trunca.</b> El <code>uuidDevice</code> admite hasta 250
     * caracteres, así que el texto compuesto puede pasarse de largo. Se
     * recorta <b>el nombre</b>, nunca la cadena armada: la IP tiene que
     * entrar entera o el dato pierde todo su valor. Si ni siquiera la IP
     * entra en el largo disponible, se devuelve el texto recortado al
     * máximo, que es lo único que la columna puede recibir.
     * </p>
     *
     * @param deviceName nombre del dispositivo (el <code>uuidDevice</code> del
     * token) o nulo/vacío si la sesión no viene de un token.
     * @param ip IP del request, o nulo/vacío si no se conoce.
     * @param maxLength largo máximo del resultado (el de la columna).
     * @return el dispositivo formateado, o nulo si no hay nombre ni IP.
     */
    public static String formatDevice(String deviceName, String ip, int maxLength) {
        String name = Fn.nvl(deviceName, "").trim();
        String address = Fn.nvl(ip, "").trim();
        if (Strings.isNullorEmpty(name) && Strings.isNullorEmpty(address)) {
            return null;
        }
        if (Strings.isNullorEmpty(name)) {
            return recortar(address, maxLength);
        }
        if (Strings.isNullorEmpty(address)) {
            return recortar(name, maxLength);
        }
        //" (" + ip + ")" es lo que hay que reservar para que la IP entre entera.
        String sufijo = " (" + address + ")";
        int disponible = maxLength - sufijo.length();
        if (disponible <= 0) {
            //Ni la IP sola entra: no hay nada que componer.
            return recortar(address, maxLength);
        }
        if (name.length() > disponible) {
            name = name.substring(0, disponible);
        }
        return name + sufijo;
    }

    /**
     * Recorta un texto al largo máximo indicado.
     *
     * @param valor texto.
     * @param maxLength largo máximo.
     * @return el texto, recortado si excedía el máximo.
     */
    private static String recortar(String valor, int maxLength) {
        if (maxLength <= 0) {
            return "";
        }
        if (valor.length() <= maxLength) {
            return valor;
        }
        return valor.substring(0, maxLength);
    }
}
