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
package org.javabeanstack.web.rest.util;

import java.util.Collection;
import java.util.Map;

/**
 * Traducción de los parámetros de listado de los recursos REST
 * ({@code page}, {@code size} y {@code orderby}) a los valores que espera la
 * capa de datos, y validación de los mismos.
 *
 * <p>Convenciones implementadas:</p>
 * <ul>
 *   <li>{@code page} empieza en 1; ausente equivale a 1.</li>
 *   <li>{@code size} ausente equivale a {@link #DEFAULT_PAGE_SIZE} y no puede
 *       superar {@link #MAX_PAGE_SIZE}.</li>
 *   <li>{@code orderby} tiene el formato {@code <campo>[,asc|desc]} y solo
 *       admite campos declarados en la lista blanca del recurso; un campo fuera
 *       de la lista es un pedido inválido, no un pedido ignorado.</li>
 * </ul>
 *
 * <p>Los métodos de validación devuelven un valor —no lanzan excepción— porque
 * la traducción a la respuesta HTTP (400 con el cuerpo de error de la
 * aplicación) le corresponde al recurso que los usa.</p>
 *
 * @author Jorge Enciso
 */
public class RestQuery {

    /** Número de la primera página. */
    public static final int FIRST_PAGE = 1;
    /**
     * Número de página más alto que se acepta pedir. El techo no es un capricho:
     * sin él, {@code (page - 1) * size} desborda el entero y la primera fila
     * resulta negativa, lo que hace fallar a la consulta con un error interno
     * donde el contrato pide un rechazo de validación. Con este techo y el
     * tamaño máximo de página el producto entra holgado en un entero.
     */
    public static final int MAX_PAGE = 1_000_000;
    /** Cantidad de registros por página cuando el cliente no la indica. */
    public static final int DEFAULT_PAGE_SIZE = 20;
    /** Cantidad máxima de registros por página que se acepta pedir. */
    public static final int MAX_PAGE_SIZE = 100;

    private static final String ASC = "asc";
    private static final String DESC = "desc";

    private RestQuery() {
    }

    /**
     * Determina si el número de página es válido. Un valor ausente es válido:
     * significa la primera página. Un valor por encima de {@link #MAX_PAGE} no
     * lo es: el recurso debe responder 400 en lugar de dejar que la consulta
     * falle por desbordamiento.
     *
     * @param page número de página solicitado.
     * @return verdadero si es válido.
     */
    public static boolean isValidPage(Integer page) {
        return page == null || (page >= FIRST_PAGE && page <= MAX_PAGE);
    }

    /**
     * Determina si la cantidad de registros por página es válida, contra el
     * máximo por omisión. Un valor ausente es válido: significa el tamaño por
     * omisión.
     *
     * @param size cantidad de registros por página solicitada.
     * @return verdadero si es válida.
     */
    public static boolean isValidSize(Integer size) {
        return isValidSize(size, MAX_PAGE_SIZE);
    }

    /**
     * Determina si la cantidad de registros por página es válida contra el
     * máximo indicado.
     *
     * @param size cantidad de registros por página solicitada.
     * @param maxSize cantidad máxima admitida.
     * @return verdadero si es válida.
     */
    public static boolean isValidSize(Integer size, int maxSize) {
        return size == null || (size >= 1 && size <= maxSize);
    }

    /**
     * Devuelve el número de página a utilizar.
     *
     * @param page número de página solicitado.
     * @return el número solicitado, o la primera página si no se indicó o el
     * valor es inválido.
     */
    public static int getPage(Integer page) {
        if (page == null || page < FIRST_PAGE) {
            return FIRST_PAGE;
        }
        return page;
    }

    /**
     * Devuelve la cantidad de registros por página a utilizar, con el tamaño y
     * el máximo por omisión.
     *
     * @param size cantidad solicitada.
     * @return cantidad a utilizar.
     */
    public static int getSize(Integer size) {
        return getSize(size, DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
    }

    /**
     * Devuelve la cantidad de registros por página a utilizar. Un valor ausente
     * o inválido cae al tamaño por omisión y uno excesivo se recorta al máximo,
     * de modo que este método nunca devuelva un valor peligroso para la base;
     * el rechazo del pedido inválido (400) lo decide el recurso con
     * {@link #isValidSize(Integer, int)}.
     *
     * @param size cantidad solicitada.
     * @param defaultSize cantidad por omisión.
     * @param maxSize cantidad máxima admitida.
     * @return cantidad a utilizar.
     */
    public static int getSize(Integer size, int defaultSize, int maxSize) {
        if (size == null || size < 1) {
            return defaultSize;
        }
        if (size > maxSize) {
            return maxSize;
        }
        return size;
    }

    /**
     * Devuelve el número de la primera fila a recuperar (base 0), tal como lo
     * espera la capa de datos.
     *
     * <p>El cálculo se hace en aritmética larga y se recorta: aunque el recurso
     * omitiera la validación de {@link #isValidPage(Integer)}, el resultado
     * nunca es negativo. Una primera fila negativa hace fallar a la consulta con
     * un error interno, que es exactamente lo que no debe pasar con un parámetro
     * que llega de afuera.</p>
     *
     * @param page número de página solicitado.
     * @param size cantidad de registros por página solicitada.
     * @return número de la primera fila de la página.
     */
    public static int getFirstRow(Integer page, Integer size) {
        long firstRow = (long) (getPage(page) - 1) * getSize(size);
        if (firstRow > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) firstRow;
    }

    /**
     * Devuelve la cantidad máxima de filas a recuperar, tal como la espera la
     * capa de datos.
     *
     * @param size cantidad de registros por página solicitada.
     * @return cantidad máxima de filas.
     */
    public static int getMaxRows(Integer size) {
        return getSize(size);
    }

    /**
     * Valida el parámetro {@code orderby} contra una lista blanca de campos y
     * devuelve la expresión de ordenamiento a pasar a la capa de datos.
     *
     * <p>La lista blanca es la garantía de que el ordenamiento no se convierta
     * en un punto de inyección ni exponga campos que el recurso no publica: solo
     * se ordena por lo que el recurso declaró.</p>
     *
     * @param orderBy valor recibido, con formato {@code <campo>[,asc|desc]}.
     * @param whitelist campos por los que el recurso admite ordenar (la
     * comparación no distingue mayúsculas de minúsculas).
     * @return la expresión de ordenamiento (por ejemplo {@code "codigo desc"}),
     * cadena vacía si no se pidió ordenamiento, o {@code null} si el pedido es
     * inválido —campo fuera de la lista blanca o dirección desconocida—, caso en
     * el que el recurso debe responder 400.
     */
    public static String parseOrderBy(String orderBy, Collection<String> whitelist) {
        if (orderBy == null || orderBy.trim().isEmpty()) {
            return "";
        }
        if (whitelist == null || whitelist.isEmpty()) {
            return null;
        }
        String[] parts = orderBy.trim().split("[,\\s]+");
        if (parts.length > 2) {
            return null;
        }
        String field = parts[0].trim();
        String direction = (parts.length == 2) ? parts[1].trim().toLowerCase() : ASC;
        if (!ASC.equals(direction) && !DESC.equals(direction)) {
            return null;
        }
        for (String allowed : whitelist) {
            if (allowed != null && allowed.trim().equalsIgnoreCase(field)) {
                return allowed.trim() + " " + direction;
            }
        }
        return null;
    }

    /**
     * Variante de {@link #parseOrderBy(String, Collection)} para los recursos
     * cuyo campo publicado no se llama igual que el campo de la entidad (por
     * ejemplo {@code persona} contra {@code persona.nombre}).
     *
     * @param orderBy valor recibido, con formato {@code <campo>[,asc|desc]}.
     * @param whitelist mapa de campo publicado a expresión de la entidad (las
     * claves se comparan sin distinguir mayúsculas de minúsculas).
     * @return la expresión de ordenamiento, cadena vacía si no se pidió
     * ordenamiento, o {@code null} si el pedido es inválido.
     */
    public static String parseOrderBy(String orderBy, Map<String, String> whitelist) {
        if (orderBy == null || orderBy.trim().isEmpty()) {
            return "";
        }
        if (whitelist == null || whitelist.isEmpty()) {
            return null;
        }
        String[] parts = orderBy.trim().split("[,\\s]+");
        if (parts.length > 2) {
            return null;
        }
        String field = parts[0].trim();
        String direction = (parts.length == 2) ? parts[1].trim().toLowerCase() : ASC;
        if (!ASC.equals(direction) && !DESC.equals(direction)) {
            return null;
        }
        for (Map.Entry<String, String> entry : whitelist.entrySet()) {
            if (entry.getKey() != null && entry.getKey().trim().equalsIgnoreCase(field)) {
                String expression = entry.getValue();
                if (expression == null || expression.trim().isEmpty()) {
                    return null;
                }
                return expression.trim() + " " + direction;
            }
        }
        return null;
    }
}
