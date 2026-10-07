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
package org.javabeanstack.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.javabeanstack.data.IDataRow;

/**
 * Contrato de la entidad parámetro de sistema: un valor de configuración
 * tipado (booleano, texto, fecha o número) identificado por su nombre y grupo.
 * Extiende {@link IDataRow}.
 *
 * @author Jorge Enciso
 */
public interface IAppSystemParam extends IDataRow {
    /**
     * Devuelve el identificador del parámetro.
     * @return identificador del parámetro.
     */
    Long getIdAppSystemParam();

    /**
     * Devuelve el nombre del parámetro.
     * @return nombre del parámetro.
     */
    String getParam();

    /**
     * Devuelve la descripción del parámetro.
     * @return descripción del parámetro.
     */
    String getParamDescrip();

    /**
     * Devuelve el tipo del parámetro (booleano, carácter, fecha, número).
     * @return tipo del parámetro.
     */
    Character getParamType();

    /**
     * Devuelve el grupo de sistema al que pertenece el parámetro.
     * @return grupo de sistema.
     */
    String getSystemgroup();

    /**
     * Devuelve el valor booleano del parámetro.
     * @return valor booleano.
     */
    Boolean getValueBoolean();

    /**
     * Devuelve el valor de texto del parámetro.
     * @return valor de texto.
     */
    String getValueChar();

    /**
     * Devuelve el valor de fecha del parámetro.
     * @return valor de fecha.
     */
    LocalDateTime getValueDate();

    /**
     * Devuelve el valor numérico del parámetro.
     * @return valor numérico.
     */
    Long getValueNumber();

    /**
     * Devuelve el valor del parámetro según su tipo.
     * @return valor del parámetro.
     */
    Object getValue();

    /**
     * Asigna el identificador del parámetro.
     * @param idsystemparam identificador del parámetro.
     */
    void setIdAppSystemParam(Long idsystemparam);

    /**
     * Asigna el nombre del parámetro.
     * @param param nombre del parámetro.
     */
    void setParam(String param);

    /**
     * Asigna la descripción del parámetro.
     * @param paramDescrip descripción del parámetro.
     */
    void setParamDescrip(String paramDescrip);

    /**
     * Asigna el tipo del parámetro.
     * @param paramType tipo del parámetro.
     */
    void setParamType(Character paramType);

    /**
     * Asigna el grupo de sistema del parámetro.
     * @param systemgroup grupo de sistema.
     */
    void setSystemgroup(String systemgroup);

    /**
     * Asigna el valor booleano del parámetro.
     * @param valueBoolean valor booleano.
     */
    void setValueBoolean(Boolean valueBoolean);

    /**
     * Asigna el valor de texto del parámetro.
     * @param valueChar valor de texto.
     */
    void setValueChar(String valueChar);

    /**
     * Asigna el valor de fecha del parámetro.
     * @param valueDate valor de fecha.
     */
    void setValueDate(LocalDateTime valueDate);

    /**
     * Asigna el valor numérico del parámetro.
     * @param valueNumber valor numérico.
     */
    void setValueNumber(Long valueNumber);

    /**
     * Asigna el valor del parámetro según su tipo.
     * @param value valor del parámetro.
     * @throws Exception si el valor no corresponde al tipo del parámetro.
     */
    void setValue(Object value) throws Exception;

    /**
     * Alcance "solo global": el parámetro solo puede tener la fila con
     * empresa nula.
     */
    char SCOPE_GLOBAL = 'G';

    /**
     * Alcance "por empresa": además de la fila global (obligatoria), cada
     * empresa puede tener o no un valor propio que prevalece sobre el global.
     */
    char SCOPE_COMPANY = 'E';

    /**
     * Alcance "global oculto": como {@link #SCOPE_GLOBAL}, solo admite la fila
     * con empresa nula, pero es de uso interno del sistema (claves, versión de
     * la estructura): las pantallas de parámetros no lo muestran ni lo editan.
     */
    char SCOPE_HIDDEN = 'H';

    /**
     * Separador de las opciones de la lista de valores válidos
     * ({@link #getValidValues()}).
     */
    String VALID_VALUES_SEPARATOR = "|";

    /**
     * Apertura de un validador en la lista de valores válidos (ver
     * {@link #parseValidators(String)}).
     */
    String VALIDATOR_OPEN = "{";

    /**
     * Cierre de un validador en la lista de valores válidos.
     */
    String VALIDATOR_CLOSE = "}";

    /**
     * Largo máximo de la ayuda de un parámetro ({@link #getParamHelp()}).
     */
    int PARAM_HELP_MAX = 2000;

    /**
     * Devuelve la empresa dueña de este valor. Nulo indica el valor global.
     *
     * <p>Los métodos de alcance y valor por defecto son {@code default} para no
     * romper implementaciones anteriores al cambio: una implementación que no
     * los sobreescriba se comporta como un parámetro solo global y sin valor
     * por defecto.</p>
     *
     * @return identificador de la empresa, o nulo si es el valor global.
     */
    default Long getIdcompany() {
        return null;
    }

    /**
     * Asigna la empresa dueña de este valor (nulo = valor global).
     * @param idcompany identificador de la empresa.
     */
    default void setIdcompany(Long idcompany) {
        throw new UnsupportedOperationException("Esta implementación no admite valores por empresa");
    }

    /**
     * Devuelve el alcance del parámetro: {@link #SCOPE_GLOBAL},
     * {@link #SCOPE_COMPANY} o {@link #SCOPE_HIDDEN}. Rige el que figure en la
     * fila global.
     * @return alcance del parámetro.
     */
    default Character getParamScope() {
        return SCOPE_GLOBAL;
    }

    /**
     * Asigna el alcance del parámetro.
     * @param paramScope {@link #SCOPE_GLOBAL}, {@link #SCOPE_COMPANY} o
     * {@link #SCOPE_HIDDEN}.
     */
    default void setParamScope(Character paramScope) {
        throw new UnsupportedOperationException("Esta implementación no admite alcance por empresa");
    }

    /**
     * Devuelve el valor de fábrica en texto canónico (ver
     * {@link #formatValue(Character, Object)}).
     * @return valor por defecto, o nulo si no tiene.
     */
    default String getDefaultValue() {
        return null;
    }

    /**
     * Asigna el valor de fábrica en texto canónico.
     * @param defaultValue valor por defecto.
     */
    default void setDefaultValue(String defaultValue) {
        throw new UnsupportedOperationException("Esta implementación no admite valor por defecto");
    }

    /**
     * Devuelve la lista de valores válidos del parámetro, o nulo si admite
     * cualquier valor de su tipo. Formato: opciones separadas por
     * {@code |}; cada opción {@code valor} o {@code valor=etiqueta} (ver
     * {@link #parseValidValues(String)}), o un validador entre llaves como
     * {@code {FOLDER}} o {@code {>=0 and <=10}} (ver
     * {@link #parseValidators(String)}).
     *
     * @return lista de valores válidos, o nulo.
     */
    default String getValidValues() {
        return null;
    }

    /**
     * Asigna la lista de valores válidos del parámetro (ver
     * {@link #getValidValues()}).
     * @param validValues lista de valores válidos, o nulo.
     */
    default void setValidValues(String validValues) {
        throw new UnsupportedOperationException("Esta implementación no admite lista de valores válidos");
    }

    /**
     * Devuelve la ayuda extensa del parámetro: qué hace, valores válidos y
     * su significado, cuándo rige y advertencias (hasta
     * {@link #PARAM_HELP_MAX} caracteres). Las pantallas de parámetros la
     * muestran como ayuda contextual del control del valor; si es nula usan la
     * descripción.
     *
     * <p>Es {@code default} por la misma razón que el alcance y la lista de
     * valores válidos: una implementación anterior que no la sobreescriba no
     * tiene ayuda.</p>
     *
     * @return ayuda del parámetro, o nulo si no tiene.
     */
    default String getParamHelp() {
        return null;
    }

    /**
     * Asigna la ayuda extensa del parámetro (ver {@link #getParamHelp()}).
     *
     * @param paramHelp ayuda del parámetro, o nulo.
     */
    default void setParamHelp(String paramHelp) {
        throw new UnsupportedOperationException("Esta implementación no admite ayuda del parámetro");
    }

    /**
     * Indica si el parámetro admite un valor propio por empresa.
     * @return verdadero si el alcance es {@link #SCOPE_COMPANY}.
     */
    default boolean isCompanyAllowed() {
        return getParamScope() != null && getParamScope() == SCOPE_COMPANY;
    }

    /**
     * Indica si el parámetro es global oculto: se lee como cualquier global,
     * pero no se muestra ni se edita desde las pantallas de parámetros.
     * @return verdadero si el alcance es {@link #SCOPE_HIDDEN}.
     */
    default boolean isHidden() {
        return getParamScope() != null && getParamScope() == SCOPE_HIDDEN;
    }

    /**
     * Vuelve el valor del parámetro al de fábrica ({@link #getDefaultValue()}).
     * Un valor por defecto nulo deja el parámetro sin valor.
     *
     * @throws Exception si el valor por defecto no corresponde al tipo.
     */
    default void restoreDefault() throws Exception {
        Object value = parseValue(getParamType(), getDefaultValue());
        Character type = getParamType();
        if (type == null) {
            return;
        }
        switch (type) {
            case 'C':
                setValueChar((String) value);
                break;
            case 'L':
                setValueBoolean((Boolean) value);
                break;
            case 'N':
                setValueNumber((Long) value);
                break;
            case 'D':
                setValueDate((LocalDateTime) value);
                break;
            default:
                throw new Exception("Tipo de dato no contemplado: " + type);
        }
    }

    /**
     * Convierte un valor al texto canónico con que se guarda el valor por
     * defecto: {@code C} tal cual, {@code N} entero, {@code L} {@code T}/{@code F},
     * {@code D} ISO {@code yyyy-MM-dd'T'HH:mm:ss}.
     *
     * <p>Acepta también el valor ya expresado como texto para cualquier tipo:
     * se valida convirtiéndolo y se devuelve normalizado.</p>
     *
     * @param paramType tipo del parámetro.
     * @param value valor a convertir.
     * @return el texto canónico, o nulo si el valor es nulo.
     * @throws IllegalArgumentException si el valor no corresponde al tipo.
     */
    static String formatValue(Character paramType, Object value) {
        if (value == null || paramType == null) {
            return null;
        }
        if (value instanceof String text && paramType != 'C') {
            try {
                value = parseValue(paramType, text);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("El valor \"" + text
                        + "\" no corresponde al tipo " + paramType, e);
            }
        }
        if (paramType == 'N' && !(value instanceof Number)
                || paramType == 'L' && !(value instanceof Boolean)
                || paramType == 'D' && !(value instanceof LocalDateTime)) {
            throw new IllegalArgumentException("El valor " + value + " ("
                    + value.getClass().getSimpleName() + ") no corresponde al tipo " + paramType);
        }
        switch (paramType) {
            case 'L':
                return Boolean.TRUE.equals(value) ? "T" : "F";
            case 'N':
                return String.valueOf(((Number) value).longValue());
            case 'D':
                return ((LocalDateTime) value).withNano(0).format(DEFAULT_DATE_FORMAT);
            default:
                return value.toString();
        }
    }

    /**
     * Convierte el texto canónico del valor por defecto al tipo del parámetro.
     *
     * @param paramType tipo del parámetro.
     * @param text texto canónico.
     * @return el valor tipado, o nulo si el texto es nulo.
     */
    static Object parseValue(Character paramType, String text) {
        if (text == null || paramType == null) {
            return null;
        }
        switch (paramType) {
            case 'L':
                return "T".equalsIgnoreCase(text.trim());
            case 'N':
                return Long.valueOf(text.trim());
            case 'D':
                return LocalDateTime.parse(text.trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            default:
                return text;
        }
    }

    /**
     * Interpreta una lista de valores válidos: opciones separadas por
     * {@code |}; cada opción {@code valor} o {@code valor=etiqueta} (se corta
     * en el primer {@code =}, así la etiqueta puede llevar comas o signos
     * igual). Se recortan los espacios, las opciones vacías se omiten y, si
     * una opción no trae etiqueta, la etiqueta es el propio valor. Un valor
     * repetido conserva la primera etiqueta.
     *
     * <p>Ejemplos: {@code GRAY|WHITE|BLUE};
     * {@code 1=Permitido salvo negación explícita|2=Negado salvo permiso explícito}.</p>
     *
     * <p>Las opciones que empiezan con llave son <b>validadores</b> (ver
     * {@link #parseValidators(String)}) y no forman parte de la lista: se
     * omiten aunque la llave no esté cerrada. Un texto con solo validadores
     * devuelve un mapa vacío (el parámetro no tiene lista cerrada).</p>
     *
     * @param validValues texto de la lista.
     * @return mapa valor → etiqueta en el orden de la lista; vacío si el texto
     * es nulo o no trae opciones.
     */
    static Map<String, String> parseValidValues(String validValues) {
        Map<String, String> result = new LinkedHashMap<>();
        if (validValues == null || validValues.isBlank()) {
            return result;
        }
        for (String option : splitValidValues(validValues)) {
            if (option.trim().startsWith(VALIDATOR_OPEN)) {
                continue;
            }
            int eq = option.indexOf('=');
            String value = (eq < 0 ? option : option.substring(0, eq)).trim();
            if (value.isEmpty()) {
                continue;
            }
            String label = eq < 0 ? "" : option.substring(eq + 1).trim();
            result.putIfAbsent(value, label.isEmpty() ? value : label);
        }
        return result;
    }

    /**
     * Devuelve los validadores declarados en la lista de valores válidos: las
     * opciones escritas entre llaves, como {@code {EMAIL}}, {@code {FOLDER}} o
     * {@code {>=0 and <=10}}. Cada uno se toma <b>completo</b> antes de
     * separar {@code valor=etiqueta}, así el {@code =} de un rango no se
     * confunde con el de una etiqueta, y un {@code |} entre llaves no corta la
     * opción.
     *
     * <p>Se devuelve el texto de adentro de las llaves, recortado y sin
     * cambiar mayúsculas (interpretarlo es tarea de quien valida; un
     * validador desconocido se ignora). Una llave que no se cierra abarca solo
     * hasta el primer {@code |} que le sigue —el resto de la lista se
     * conserva— y ese tramo se devuelve <b>con</b> la llave de apertura
     * (<code>{EMAIL</code> se devuelve como <code>"{EMAIL"</code>), así quien valida lo reconoce como
     * un validador mal escrito y no como uno válido; nunca es una opción de la
     * lista. Los vacíos ({@code {}}) se omiten y un repetido se devuelve una
     * sola vez.</p>
     *
     * <p>Ejemplo: {@code {>=1 and <=65535}} devuelve
     * {@code [">=1 and <=65535"]}; {@code GRAY|WHITE} devuelve una lista
     * vacía.</p>
     *
     * @param validValues texto de la lista.
     * @return validadores en el orden de la lista; vacía si no hay.
     */
    static List<String> parseValidators(String validValues) {
        List<String> result = new ArrayList<>();
        if (validValues == null || validValues.isBlank()) {
            return result;
        }
        for (String option : splitValidValues(validValues)) {
            String token = option.trim();
            if (!token.startsWith(VALIDATOR_OPEN)) {
                continue;
            }
            if (token.length() > VALIDATOR_OPEN.length() && token.endsWith(VALIDATOR_CLOSE)) {
                token = token.substring(VALIDATOR_OPEN.length(), token.length() - VALIDATOR_CLOSE.length()).trim();
            } else {
                // Sin cerrar: se conserva la llave para que no pase por un validador válido
                token = VALIDATOR_OPEN + token.substring(VALIDATOR_OPEN.length()).trim();
                if (token.equals(VALIDATOR_OPEN)) {
                    token = "";
                }
            }
            if (!token.isEmpty() && !result.contains(token)) {
                result.add(token);
            }
        }
        return result;
    }

    /**
     * Parte la lista de valores válidos en opciones por {@code |}, sin cortar
     * dentro de las llaves de un validador. Una llave que llega al final sin
     * cerrarse no se traga el resto de la lista: su tramo termina en el primer
     * {@code |} que le sigue y lo que viene después se vuelve a partir
     * (SYSPARUI M3-10).
     *
     * @param validValues texto de la lista (no nulo).
     * @return opciones sin recortar, en orden.
     */
    private static List<String> splitValidValues(String validValues) {
        char open = VALIDATOR_OPEN.charAt(0);
        char close = VALIDATOR_CLOSE.charAt(0);
        char separator = VALID_VALUES_SEPARATOR.charAt(0);
        List<String> options = new ArrayList<>();
        int start = 0;
        int depth = 0;
        int firstSeparatorInBrace = -1;
        int i = 0;
        while (i < validValues.length()) {
            char c = validValues.charAt(i);
            if (c == open) {
                if (depth == 0) {
                    firstSeparatorInBrace = -1;
                }
                depth++;
            } else if (c == close && depth > 0) {
                depth--;
            } else if (c == separator) {
                if (depth == 0) {
                    options.add(validValues.substring(start, i));
                    start = i + 1;
                } else if (firstSeparatorInBrace < 0) {
                    firstSeparatorInBrace = i;
                }
            }
            i++;
            if (i == validValues.length() && depth > 0 && firstSeparatorInBrace >= 0) {
                // Llave sin cerrar: el tramo termina en el primer | dentro de ella
                options.add(validValues.substring(start, firstSeparatorInBrace));
                start = firstSeparatorInBrace + 1;
                i = start;
                depth = 0;
                firstSeparatorInBrace = -1;
            }
        }
        options.add(validValues.substring(start));
        return options;
    }

    /**
     * Formato del texto canónico de las fechas en el valor por defecto.
     */
    DateTimeFormatter DEFAULT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
}
