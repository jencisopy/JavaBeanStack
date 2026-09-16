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
package org.javabeanstack.poi.excel;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Colección ordenada de {@link ExcelColumnSpec} que describe el mapeo completo
 * de una planilla: qué columnas se leen, a qué atributos van, cuáles son
 * obligatorias y qué valor toman si faltan. Es la fuente de verdad que consume
 * {@link ExcelRowProcessor}; los mapas {@code headToField} / {@code fieldToHead}
 * de la interfaz {@link IExcelRowProcessor} se derivan de ella.
 * <p>
 * Conserva el <b>orden de declaración</b> de las columnas (los generadores de
 * importaciones y sus informes dependen de ese orden) y rechaza la declaración
 * de una cabecera repetida, porque una planilla con cabeceras duplicadas es
 * ambigua. Una columna ya declarada se retoca con {@link #edit(String)}, que
 * conserva su posición.
 * <p>
 * Uso típico dentro de una subclase de {@link ExcelRowProcessor}:
 * <pre>{@code
 * private static ExcelColumns buildColumns() {
 *     ExcelColumns c = new ExcelColumns();
 *     c.add("RUC / Nº de Identificacion del Informado", "ruc").require(ColumnRequirement.VALUE);
 *     c.add("Fecha de Emisión", "fecha").required();
 *     c.add("sucursal", "sucursal").defaultValue("01");
 *     c.add("moneda", "moneda").defaultValue("GS.").defaultWhenBlank();
 *     c.add("Concepto", "concepto");
 *     return c;
 * }
 * }</pre>
 *
 * @author Jorge Enciso
 */
public final class ExcelColumns implements Iterable<ExcelColumnSpec> {

    /**
     * Especificaciones por cabecera, en orden de declaración.
     */
    private final Map<String, ExcelColumnSpec> specs = new LinkedHashMap<>();

    /**
     * Crea una colección vacía, a completar con
     * {@link #add(String, String)}.
     */
    public ExcelColumns() {
    }

    /**
     * Crea una copia de otra colección, conservando el orden de declaración.
     * Las especificaciones son inmutables, así que la copia comparte las
     * instancias pero no se ve afectada por los {@link #add(String, String)}
     * posteriores sobre el original.
     * <p>
     * Es la copia defensiva que hace {@link ExcelRowProcessor} al recibir la
     * colección: una vez construido el procesador, sus mapas derivados
     * ({@code headToField} / {@code fieldToHead}) ya están calculados y un
     * agregado posterior los dejaría desactualizados.
     *
     * @param other colección a copiar; {@code null} produce una colección
     * vacía.
     */
    public ExcelColumns(ExcelColumns other) {
        if (other != null) {
            this.specs.putAll(other.specs);
        }
    }

    /**
     * Construye una colección a partir del mapeo histórico cabecera →
     * atributo: todas las columnas quedan {@link ColumnRequirement#OPTIONAL} y
     * sin valor por defecto, que es exactamente el comportamiento de los
     * constructores de {@link ExcelRowProcessor} que reciben un
     * {@code Map<String, String>}. Se conserva el orden de iteración del mapa
     * recibido. Las claves nulas o en blanco se ignoran y las demás se
     * normalizan con {@code trim()} (ver {@link #add(String, String)}).
     *
     * @param headToField mapeo de cabecera de columna a nombre del atributo
     * destino; puede ser {@code null} (devuelve una colección vacía).
     * @return la colección equivalente al mapeo recibido.
     */
    public static ExcelColumns of(Map<String, String> headToField) {
        ExcelColumns columns = new ExcelColumns();
        if (headToField == null) {
            return columns;
        }
        for (Map.Entry<String, String> entry : headToField.entrySet()) {
            String header = entry.getKey();
            if (header == null || header.trim().isEmpty()) {
                continue;
            }
            columns.add(header, entry.getValue());
        }
        return columns;
    }

    /**
     * Declara una columna y devuelve el constructor fluido para completar su
     * nivel de exigencia, valor por defecto y transformación. La columna ya
     * queda registrada al volver de este método: cada método del constructor
     * fluido reemplaza la especificación dentro de la colección, sin alterar
     * su posición.
     *
     * @param header texto exacto de la cabecera en la planilla; se normaliza
     * con {@code trim()}, porque el índice de columnas de la planilla también
     * se arma recortando los espacios de cada encabezado: sin esa
     * normalización una cabecera declarada con espacios al borde no casaría
     * nunca con su columna.
     * @param field nombre del atributo destino; puede ser {@code null} o vacío
     * para una columna que la subclase procesa a mano.
     * @return el constructor fluido de la columna declarada.
     * @throws IllegalArgumentException si la cabecera es nula o en blanco, o si
     * ya fue declarada en esta colección (comparando las cabeceras
     * normalizadas).
     */
    public ExcelColumnSpec.Builder add(String header, String field) {
        if (header == null || header.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "La cabecera de la columna no puede estar vacía (atributo destino: " + field + ")");
        }
        String normalized = header.trim();
        if (specs.containsKey(normalized)) {
            throw new IllegalArgumentException(
                    "La cabecera «" + normalized + "» ya fue declarada en las columnas de la planilla");
        }
        ExcelColumnSpec.Builder builder
                = new ExcelColumnSpec.Builder(normalized, field, spec -> specs.put(spec.getHeader(), spec));
        builder.build();
        return builder;
    }

    /**
     * Devuelve el constructor fluido de una columna <b>ya declarada</b>,
     * cargado con su especificación vigente, para completarla o corregirla
     * sin volver a declararla. Cada método del constructor reemplaza la
     * especificación dentro de la colección <b>en la misma posición</b> (el
     * reemplazo de una clave existente no altera el orden de inserción del
     * mapa): el orden de declaración se conserva.
     * <p>
     * Es el punto de enganche para lo que una declaración externa no puede
     * expresar, como las columnas leídas de un XML con
     * {@link ExcelColumnsXml}: la transformación ({@code converter}) y el valor
     * por defecto calculado ({@code Supplier}) siguen siendo código Java y se
     * agregan encima de lo declarado.
     * <pre>{@code
     * ExcelColumns c = ExcelColumnsXml.fromXmlDom(xml, "PAGE/EXCELIMPORT/DEFAULT");
     * c.edit("fecha").converter(v -> LocalDates.toDateTime(v.toString()));
     * c.edit("secuencia").defaultValue(() -> next());
     * }</pre>
     *
     * @param header cabecera de la columna a retocar; se normaliza con
     * {@code trim()}, igual que al declararla.
     * @return el constructor fluido cargado con la especificación vigente de
     * esa columna.
     * @throws IllegalArgumentException si la cabecera no fue declarada en
     * esta colección: un retoque sobre una columna inexistente sería un error
     * de tipeo que pasaría en silencio.
     */
    public ExcelColumnSpec.Builder edit(String header) {
        String normalized = (header == null) ? null : header.trim();
        ExcelColumnSpec current = (normalized == null) ? null : specs.get(normalized);
        if (current == null) {
            throw new IllegalArgumentException(
                    "La cabecera «" + header + "» no fue declarada en las columnas de la planilla"
                    + "; las declaradas son " + specs.keySet());
        }
        return new ExcelColumnSpec.Builder(current, spec -> specs.put(spec.getHeader(), spec));
    }

    /**
     * Devuelve el mapeo cabecera → atributo derivado de las
     * especificaciones, en el orden de declaración.
     *
     * @return un {@code LinkedHashMap} nuevo con cabecera → atributo destino.
     */
    public Map<String, String> toHeadToField() {
        Map<String, String> map = new LinkedHashMap<>();
        for (ExcelColumnSpec spec : specs.values()) {
            map.put(spec.getHeader(), spec.getField());
        }
        return map;
    }

    /**
     * Devuelve el mapeo inverso atributo → cabecera derivado de las
     * especificaciones, en el orden de declaración. Si dos columnas apuntan al
     * mismo atributo, gana la última declarada (igual que la inversión del
     * mapa histórico).
     *
     * @return un {@code LinkedHashMap} nuevo con atributo destino → cabecera.
     */
    public Map<String, String> toFieldToHead() {
        Map<String, String> map = new LinkedHashMap<>();
        for (ExcelColumnSpec spec : specs.values()) {
            map.put(spec.getField(), spec.getHeader());
        }
        return map;
    }

    /**
     * Busca la especificación de una cabecera.
     *
     * @param header texto de la cabecera buscada; se normaliza con
     * {@code trim()}, igual que al declararla.
     * @return la especificación de esa columna, o vacío si no fue declarada.
     */
    public Optional<ExcelColumnSpec> byHeader(String header) {
        return Optional.ofNullable((header == null) ? null : specs.get(header.trim()));
    }

    /**
     * Busca la especificación que apunta a un atributo destino. Si más de una
     * columna apunta al mismo atributo, devuelve la primera declarada.
     *
     * @param field nombre del atributo destino buscado.
     * @return la especificación de la columna que alimenta ese atributo, o
     * vacío si ninguna lo hace.
     */
    public Optional<ExcelColumnSpec> byField(String field) {
        if (field == null) {
            return Optional.empty();
        }
        for (ExcelColumnSpec spec : specs.values()) {
            if (field.equals(spec.getField())) {
                return Optional.of(spec);
            }
        }
        return Optional.empty();
    }

    /**
     * Devuelve la cantidad de columnas declaradas.
     *
     * @return la cantidad de especificaciones de la colección.
     */
    public int size() {
        return specs.size();
    }

    /**
     * Indica si no hay ninguna columna declarada.
     *
     * @return {@code true} si la colección está vacía.
     */
    public boolean isEmpty() {
        return specs.isEmpty();
    }

    /**
     * Recorre las especificaciones en el orden en que fueron declaradas. El
     * iterador es de solo lectura.
     *
     * @return iterador de solo lectura sobre las especificaciones.
     */
    @Override
    public Iterator<ExcelColumnSpec> iterator() {
        return Collections.unmodifiableCollection(specs.values()).iterator();
    }

    /**
     * Devuelve una representación legible de la colección, útil en logs y
     * mensajes de diagnóstico.
     *
     * @return texto con las especificaciones en orden de declaración.
     */
    @Override
    public String toString() {
        return "ExcelColumns" + specs.values();
    }
}
