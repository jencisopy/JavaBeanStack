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

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Especificación inmutable de una columna de planilla Excel: qué cabecera se
 * lee, a qué atributo del objeto destino se asigna, con qué nivel de
 * exigencia ({@link ColumnRequirement}), con qué valor por defecto y con qué
 * transformación previa.
 * <p>
 * Es la unidad de información que consume {@link ExcelRowProcessor} (reunida
 * en {@link ExcelColumns}) y reemplaza al par cabecera/atributo del
 * {@code Map<String, String>} histórico, que equivale a una especificación
 * {@link ColumnRequirement#OPTIONAL} sin valor por defecto.
 * <p>
 * La identidad de una especificación es su {@link #getHeader() cabecera}:
 * {@link #equals(Object)} y {@link #hashCode()} solo la consideran, porque una
 * planilla no puede tener dos columnas con el mismo nombre.
 * <p>
 * Se construye con {@link #builder(String, String)}, o —lo habitual— con
 * {@link ExcelColumns#add(String, String)}, que devuelve un {@link Builder}
 * encadenado a la colección:
 * <pre>{@code
 * ExcelColumns columnas = new ExcelColumns();
 * columnas.add("RUC del Informado", "ruc").require(ColumnRequirement.VALUE);
 * columnas.add("sucursal", "sucursal").defaultValue("01");
 * }</pre>
 *
 * @author Jorge Enciso
 */
public final class ExcelColumnSpec {

    private final String header;
    private final String field;
    private final ColumnRequirement requirement;
    private final Supplier<Object> defaultValue;
    private final boolean defaultValueFixed;
    private final boolean defaultWhenBlank;
    private final Function<Object, Object> converter;
    private final boolean overwrite;

    /**
     * Crea la especificación a partir de los datos acumulados en el
     * constructor fluido.
     *
     * @param builder constructor fluido con los valores ya definidos.
     */
    private ExcelColumnSpec(Builder builder) {
        this.header = builder.header;
        this.field = builder.field;
        this.requirement = builder.requirement;
        this.defaultValue = builder.defaultValue;
        this.defaultValueFixed = builder.defaultValueFixed;
        this.defaultWhenBlank = builder.defaultWhenBlank;
        this.converter = builder.converter;
        this.overwrite = builder.overwrite;
    }

    /**
     * Crea un constructor fluido independiente de toda colección, útil para
     * armar una especificación suelta.
     *
     * @param header texto exacto de la cabecera en la planilla; se le aplica
     * {@code trim()}, igual que a las cabeceras leídas de la planilla.
     * @param field nombre del atributo destino en el {@code IDataRow}; puede
     * ser {@code null} o vacío para una columna que la subclase procesa a mano.
     * @return el constructor fluido; la especificación se obtiene con
     * {@link Builder#build()}.
     * @throws IllegalArgumentException si la cabecera es nula o en blanco, la
     * misma validación que hace {@link ExcelColumns#add(String, String)}.
     */
    public static Builder builder(String header, String field) {
        return new Builder(header, field, null);
    }

    /**
     * Devuelve el texto de la cabecera tal como debe aparecer en la fila de
     * encabezados de la planilla, sin espacios al principio ni al final (el
     * índice de columnas de la planilla también se arma con {@code trim()}).
     *
     * @return el texto de la cabecera; nunca {@code null} ni en blanco, y es la
     * identidad de la especificación.
     */
    public String getHeader() {
        return header;
    }

    /**
     * Devuelve el atributo destino al que se asigna el valor de la columna.
     *
     * @return el nombre del atributo en el objeto destino, o {@code null} /
     * cadena vacía si la columna no se autoasigna (la subclase la procesa en
     * {@code process()}).
     */
    public String getField() {
        return field;
    }

    /**
     * Devuelve el nivel de exigencia declarado para la columna.
     *
     * @return el nivel de exigencia; nunca {@code null}
     * ({@link ColumnRequirement#OPTIONAL} si no se declaró otro).
     */
    public ColumnRequirement getRequirement() {
        return requirement;
    }

    /**
     * Devuelve el proveedor del valor por defecto. Se evalúa una vez por fila,
     * por lo que puede devolver valores dependientes del momento o de la
     * sesión (fecha del día, usuario conectado).
     *
     * @return el proveedor del valor por defecto, o {@code null} si la columna
     * no declaró ninguno.
     */
    public Supplier<Object> getDefaultValue() {
        return defaultValue;
    }

    /**
     * Indica si la columna declaró un valor por defecto.
     *
     * @return {@code true} si hay valor por defecto que aplicar cuando la
     * columna falta (o cuando la celda está vacía y
     * {@link #isDefaultWhenBlank()} es {@code true}).
     */
    public boolean hasDefaultValue() {
        return defaultValue != null;
    }

    /**
     * Indica si el valor por defecto es un valor <b>fijo</b>, declarado con
     * {@link Builder#defaultValue(Object)}, en lugar de un proveedor declarado
     * con {@link Builder#defaultValue(Supplier)}.
     * <p>
     * Lo consulta {@link ExcelRowProcessor#checkMetaData()} para validar por
     * anticipado que el valor por defecto sea convertible al tipo del atributo:
     * un valor fijo se puede evaluar cuantas veces haga falta, mientras que un
     * proveedor puede tener efecto (una secuencia, la hora del momento) y por
     * eso solo se lo evalúa una vez por fila procesada.
     *
     * @return {@code true} si la columna declaró un valor por defecto y es
     * fijo; {@code false} si no declaró ninguno o si lo declaró como proveedor.
     */
    public boolean isDefaultValueFixed() {
        return defaultValueFixed;
    }

    /**
     * Indica si el valor por defecto también se aplica cuando la columna
     * existe pero la celda está vacía. Apagado por defecto: una celda vacía en
     * una columna presente suele ser un dato y no una omisión.
     * <p>
     * <b>Prevalece sobre {@link ColumnRequirement#VALUE}</b>: si la columna es
     * {@code VALUE} y declaró {@code defaultWhenBlank}, una celda vacía toma el
     * valor por defecto y la fila <b>no</b> se marca con error, porque el valor
     * por defecto es la declaración explícita de qué poner cuando el dato no
     * viene.
     *
     * @return {@code true} si el valor por defecto reemplaza a la celda vacía.
     */
    public boolean isDefaultWhenBlank() {
        return defaultWhenBlank;
    }

    /**
     * Devuelve la transformación que se aplica al valor nativo de la celda
     * antes de convertirlo al tipo del atributo y asignarlo. No se aplica al
     * valor por defecto ni a las celdas vacías.
     *
     * @return la transformación de la columna, o {@code null} si no se declaró
     * ninguna.
     */
    public Function<Object, Object> getConverter() {
        return converter;
    }

    /**
     * Indica si el valor de esta columna puede <b>sobreescribir</b> el de un
     * registro que ya existe en la base cuando la importación corre con la
     * opción de sobreescritura. Por defecto {@code true}: toda columna presente
     * en la planilla actualiza el existente. Con {@code false} la columna se
     * graba solo en las altas y, en una actualización, el registro conserva su
     * valor aunque la planilla traiga otro. No afecta a la validación ni a la
     * conversión de la celda.
     *
     * @return {@code true} si la columna actualiza registros existentes.
     */
    public boolean isOverwrite() {
        return overwrite;
    }

    /**
     * Compara por cabecera: dos especificaciones de la misma columna de
     * planilla son iguales aunque difieran el atributo destino, el nivel de
     * exigencia o el valor por defecto.
     *
     * @param obj objeto a comparar.
     * @return {@code true} si {@code obj} es una especificación con la misma
     * cabecera.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ExcelColumnSpec)) {
            return false;
        }
        ExcelColumnSpec other = (ExcelColumnSpec) obj;
        return header.equals(other.header);
    }

    /**
     * Devuelve el hash de la cabecera, coherente con {@link #equals(Object)}.
     *
     * @return el hash de la cabecera.
     */
    @Override
    public int hashCode() {
        return header.hashCode();
    }

    /**
     * Devuelve una representación legible de la especificación, útil en logs y
     * mensajes de diagnóstico.
     *
     * @return texto con la cabecera, el atributo destino y el nivel de
     * exigencia.
     */
    @Override
    public String toString() {
        return "ExcelColumnSpec{header=" + header + ", field=" + field
                + ", requirement=" + requirement
                + ", hasDefaultValue=" + hasDefaultValue()
                + ", defaultWhenBlank=" + defaultWhenBlank
                + ", overwrite=" + overwrite + "}";
    }

    /**
     * Constructor fluido de {@link ExcelColumnSpec}. Cuando lo devuelve
     * {@link ExcelColumns#add(String, String)}, cada método de configuración
     * publica de inmediato la especificación resultante en la colección de
     * origen: por eso la línea
     * {@code columnas.add("sucursal", "sucursal").required().defaultValue("01");}
     * queda registrada sin necesidad de invocar {@link #build()}.
     */
    public static final class Builder {

        private final String header;
        private final String field;
        private ColumnRequirement requirement = ColumnRequirement.OPTIONAL;
        private Supplier<Object> defaultValue;
        private boolean defaultValueFixed;
        private boolean defaultWhenBlank;
        private Function<Object, Object> converter;
        private boolean overwrite = true;
        private final Consumer<ExcelColumnSpec> sink;

        /**
         * Crea el constructor fluido de una columna.
         *
         * @param header texto exacto de la cabecera en la planilla; se
         * normaliza con {@code trim()}.
         * @param field nombre del atributo destino.
         * @param sink destino donde publicar la especificación cada vez que
         * cambia (la colección {@link ExcelColumns} que creó el constructor),
         * o {@code null} si la especificación es suelta.
         * @throws IllegalArgumentException si la cabecera es nula o en blanco:
         * una especificación sin cabecera no puede casar con ninguna columna de
         * la planilla.
         */
        Builder(String header, String field, Consumer<ExcelColumnSpec> sink) {
            if (header == null || header.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "La cabecera de la columna no puede estar vacía (atributo destino: "
                        + field + ")");
            }
            this.header = header.trim();
            this.field = field;
            this.sink = sink;
        }

        /**
         * Crea un constructor fluido cargado con una especificación ya
         * existente, para retocarla sin volver a declararla: es el que
         * devuelve {@link ExcelColumns#edit(String)}. Copia los ocho campos
         * tal cual están (incluido {@code defaultValueFixed}, que
         * {@code ExcelRowProcessor.checkMetaData()} usa para validar los
         * valores por defecto fijos antes de leer filas), sin pasar por los
         * métodos fluidos: {@link #defaultValue(Object)} tomaría el
         * {@code Supplier} guardado como proveedor calculado y perdería esa
         * marca en silencio.
         *
         * @param base especificación de partida; no puede ser {@code null}.
         * @param sink destino donde publicar la especificación cada vez que
         * cambia, o {@code null} si la especificación es suelta.
         */
        Builder(ExcelColumnSpec base, Consumer<ExcelColumnSpec> sink) {
            this.header = base.header;
            this.field = base.field;
            this.requirement = base.requirement;
            this.defaultValue = base.defaultValue;
            this.defaultValueFixed = base.defaultValueFixed;
            this.defaultWhenBlank = base.defaultWhenBlank;
            this.converter = base.converter;
            this.overwrite = base.overwrite;
            this.sink = sink;
        }

        /**
         * Declara el nivel de exigencia de la columna.
         *
         * @param requirement nivel de exigencia; {@code null} equivale a
         * {@link ColumnRequirement#OPTIONAL}.
         * @return este mismo constructor, para seguir encadenando.
         */
        public Builder require(ColumnRequirement requirement) {
            this.requirement = (requirement == null) ? ColumnRequirement.OPTIONAL : requirement;
            return publish();
        }

        /**
         * Declara la columna como obligatoria a nivel de cabecera (equivale a
         * {@code require(ColumnRequirement.COLUMN)}).
         *
         * @return este mismo constructor, para seguir encadenando.
         */
        public Builder required() {
            return require(ColumnRequirement.COLUMN);
        }

        /**
         * Declara un valor por defecto fijo, que se convierte al tipo del
         * atributo destino igual que el valor de una celda.
         * <p>
         * Si el valor recibido <b>es</b> un {@link Supplier}, se deriva a
         * {@link #defaultValue(Supplier)} en vez de guardarlo como constante:
         * es la red contra la trampa de la invarianza de los genéricos. Una
         * variable declarada {@code Supplier<String>} (o un {@code Supplier}
         * de cualquier tipo que no sea {@code Object}) <b>no</b> es subtipo de
         * {@code Supplier<Object>}, así que antes ligaba con esta sobrecarga y
         * quedaba guardada como valor: la importación fallaba en la primera
         * fila al intentar asignar la lambda al atributo. Como consecuencia, un
         * {@code Supplier} no se puede declarar como valor por defecto literal.
         *
         * @param value valor por defecto; {@code null} deja la columna sin
         * valor por defecto. Un {@link Supplier} se trata como proveedor.
         * @return este mismo constructor, para seguir encadenando.
         */
        public Builder defaultValue(Object value) {
            if (value instanceof Supplier<?> supplier) {
                return defaultValue(supplier);
            }
            this.defaultValue = (value == null) ? null : () -> value;
            this.defaultValueFixed = (value != null);
            return publish();
        }

        /**
         * Declara un valor por defecto calculado, evaluado una vez por fila
         * procesada (fecha del día, dato de la sesión, secuencia).
         * <p>
         * El parámetro es {@code Supplier<?>} y no {@code Supplier<Object>}
         * justamente por la invarianza de los genéricos: con
         * {@code Supplier<Object>} una variable declarada
         * {@code Supplier<String>} no ligaba con esta sobrecarga y se iba a
         * {@link #defaultValue(Object)} sin ningún aviso del compilador. Una
         * lambda escrita en el lugar ({@code .defaultValue(() -> "01")}) liga
         * siempre acá, porque {@code Object} no es una interfaz funcional.
         *
         * @param supplier proveedor del valor por defecto; {@code null} deja la
         * columna sin valor por defecto.
         * @return este mismo constructor, para seguir encadenando.
         */
        public Builder defaultValue(Supplier<?> supplier) {
            this.defaultValue = (supplier == null) ? null : () -> supplier.get();
            this.defaultValueFixed = false;
            return publish();
        }

        /**
         * Pide que el valor por defecto se aplique también cuando la columna
         * existe pero la celda está vacía.
         * <p>
         * Declarado junto con {@link ColumnRequirement#VALUE}, <b>prevalece</b>
         * sobre la exigencia de valor: la celda vacía toma el valor por defecto
         * y la fila no se marca con error.
         *
         * @return este mismo constructor, para seguir encadenando.
         */
        public Builder defaultWhenBlank() {
            return defaultWhenBlank(true);
        }

        /**
         * Define si el valor por defecto se aplica también con la celda vacía.
         *
         * @param defaultWhenBlank {@code true} para aplicarlo con celda vacía.
         * @return este mismo constructor, para seguir encadenando.
         */
        public Builder defaultWhenBlank(boolean defaultWhenBlank) {
            this.defaultWhenBlank = defaultWhenBlank;
            return publish();
        }

        /**
         * Declara una transformación del valor nativo de la celda, aplicada
         * antes de convertirlo al tipo del atributo (normalizar texto, quitar
         * separadores, recortar códigos). Una excepción de la transformación
         * marca la fila con un error que nombra la columna.
         *
         * @param converter transformación a aplicar, o {@code null} para
         * ninguna.
         * @return este mismo constructor, para seguir encadenando.
         */
        public Builder converter(Function<Object, Object> converter) {
            this.converter = converter;
            return publish();
        }

        /**
         * Declara que esta columna <b>no</b> sobreescribe el valor de un
         * registro existente: se graba solo en las altas y, cuando la
         * importación actualiza (opción de sobreescritura encendida), el
         * registro conserva lo que tenía aunque la planilla traiga otro valor.
         * Equivale a {@code overwrite(false)}.
         *
         * @return este mismo constructor, para seguir encadenando.
         */
        public Builder noOverwrite() {
            return overwrite(false);
        }

        /**
         * Define si la columna sobreescribe registros existentes (por defecto
         * {@code true}). Ver {@link ExcelColumnSpec#isOverwrite()}.
         *
         * @param overwrite {@code false} para conservar el valor existente.
         * @return este mismo constructor, para seguir encadenando.
         */
        public Builder overwrite(boolean overwrite) {
            this.overwrite = overwrite;
            return publish();
        }

        /**
         * Construye la especificación con los valores declarados hasta el
         * momento (y la publica en la colección de origen, si la hay).
         *
         * @return la especificación inmutable de la columna.
         */
        public ExcelColumnSpec build() {
            ExcelColumnSpec spec = new ExcelColumnSpec(this);
            if (sink != null) {
                sink.accept(spec);
            }
            return spec;
        }

        /**
         * Publica en la colección de origen la especificación con el estado
         * actual del constructor.
         *
         * @return este mismo constructor, para seguir encadenando.
         */
        private Builder publish() {
            build();
            return this;
        }
    }
}
