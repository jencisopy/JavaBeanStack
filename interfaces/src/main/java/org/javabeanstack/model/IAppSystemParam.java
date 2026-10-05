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
     * Devuelve el alcance del parámetro: {@link #SCOPE_GLOBAL} o
     * {@link #SCOPE_COMPANY}. Rige el que figure en la fila global.
     * @return alcance del parámetro.
     */
    default Character getParamScope() {
        return SCOPE_GLOBAL;
    }

    /**
     * Asigna el alcance del parámetro.
     * @param paramScope {@link #SCOPE_GLOBAL} o {@link #SCOPE_COMPANY}.
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
     * Indica si el parámetro admite un valor propio por empresa.
     * @return verdadero si el alcance es {@link #SCOPE_COMPANY}.
     */
    default boolean isCompanyAllowed() {
        return getParamScope() != null && getParamScope() == SCOPE_COMPANY;
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
     * Formato del texto canónico de las fechas en el valor por defecto.
     */
    DateTimeFormatter DEFAULT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
}
