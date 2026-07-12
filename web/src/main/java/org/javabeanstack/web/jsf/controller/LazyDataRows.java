/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2018 - 2027 Jorge Enciso
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
package org.javabeanstack.web.jsf.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;
import org.primefaces.model.SortOrder;

import org.javabeanstack.data.DataInfo;
import org.javabeanstack.data.IDataRow;
import org.javabeanstack.util.Strings;
import org.javabeanstack.error.ErrorManager;
import org.javabeanstack.util.Dates;
import org.javabeanstack.util.Fn;
import org.javabeanstack.util.LocalDates;
import org.javabeanstack.web.model.IColumnModel;
import org.primefaces.model.FilterMeta;

/**
 * Modelo de datos lazy (página a página) que conecta el dataTable de
 * PrimeFaces con un {@link AbstractDataController}. La consulta, el filtrado y
 * el ordenamiento se resuelven en la base de datos a través del controller
 * (setFirstRow/setMaxRows + requery), no en memoria.
 *
 * <p><b>Ciclo de vida en PrimeFaces 15</b> (ver
 * {@code DataTable.loadLazyScrollData}):</p>
 * <ol>
 * <li>{@link #count(java.util.Map)} — se invoca <b>antes</b> de load; su valor
 * se usa en {@code calculateFirst()} para recortar el nro. de primer registro
 * si quedó fuera de rango.</li>
 * <li>{@link #load(int, int, java.util.Map, java.util.Map)} — trae la página
 * solicitada, calcula el total real y lo fija vía {@code setRowCount()}
 * (patrón "single-call" documentado en {@link LazyDataModel#count}).</li>
 * </ol>
 *
 * <p><b>Modos de filtro por columna</b> (atributo {@code filterMode} de
 * {@link IColumnModel}, aplica solo a columnas alfanuméricas):</p>
 * <ul>
 * <li>{@code exact}, {@code exact_trim}, {@code exact_ltrim} — igualdad
 * (con trim/ltrim sobre el campo).</li>
 * <li>{@code contain}, {@code contain_trim} o vacío — LIKE %valor%
 * (insensible a mayúsculas). Es el modo por defecto.</li>
 * <li>{@code contain_ltrim} — LIKE valor% (comienza con).</li>
 * <li>{@code contain_rtrim} — LIKE %valor (termina con).</li>
 * </ul>
 *
 * <p><b>Máscaras de filtro</b> (atributo {@code filterMask} de
 * {@link IColumnModel}): {@code right_blank_N} / {@code left_blank_N} rellenan
 * el valor con espacios hasta N posiciones; {@code replace('a','b')} sustituye
 * texto en el valor buscado.</p>
 *
 * <p><b>Otras banderas:</b> con {@code noCount=true} se omite el SELECT COUNT
 * y el total se estima con la página cargada (para consultas cuyo count es
 * costoso). El atributo {@code "nolazyload"} del FacesContext (fijado por
 * ej. en {@code AbstractDataController.update()}) evita re-ejecutar la
 * consulta cuando solo se refresca el componente.</p>
 *
 * @author jenciso
 * @param <T> tipo de la fila, derivado de IDataRow
 */
public class LazyDataRows<T extends IDataRow> extends LazyDataModel<T> {

    private static final Logger LOGGER = LogManager.getLogger(LazyDataRows.class);
    public AbstractDataController context;
    private boolean noCount = false;
    private int timesLoaded = 0;

    public LazyDataRows(AbstractDataController context) {
        this.context = context;
    }

    /**
     * Si es verdadero no se ejecuta el SELECT COUNT en cada carga; el total se
     * estima a partir de la página traída (ver {@link #calculateRowCount}).
     *
     * @return valor de la bandera noCount.
     */
    public boolean isNoCount() {
        return noCount;
    }

    public void setNoCount(boolean noCount) {
        this.noCount = noCount;
    }

    /**
     * Devuelve la lista de registros de la última página cargada por el
     * controller.
     *
     * @return lista de registros o nulo si no hay controller asociado.
     */
    public List<T> getRows() {
        if (context != null) {
            return context.getDataRows();
        }
        return null;
    }

    /**
     * Busca el registro correspondiente a un rowKey generado por
     * {@link IDataRow#getRowkey()}. El formato normal es
     * <code>{Tipo}valor</code> (ej. {Long}150); si el registro no tiene id
     * el rowkey es el id alternativo sin formato.
     *
     * @param rowKey clave de fila enviada por el dataTable.
     * @return el registro correspondiente o nulo si no se encuentra.
     */
    @Override
    public T getRowData(String rowKey) {
        if (context == null || rowKey == null || rowKey.isEmpty()) {
            return null;
        }
        //rowKey sin formato {Tipo}valor (id alternativo), comparar contra getRowkey()
        if (!rowKey.contains("}")) {
            if (getRows() == null) {
                return null;
            }
            for (T row : getRows()) {
                if (row != null && rowKey.equals(row.getRowkey())) {
                    return row;
                }
            }
            return null;
        }
        String rowKeyValue = rowKey.substring(rowKey.indexOf("}") + 1);
        String rowKeyType = rowKey.substring(1, rowKey.indexOf("}"));
        //Verificar si el registro actual posicionado es igual al rowKeyValue
        if (context.getRow() != null && context.getRow().getId() != null
                && rowKeyValue.equals(context.getRow().getId().toString())) {
            return (T) context.getRow();
        }
        if (getRows() == null) {
            return null;
        }
        Object id = getIdValue(rowKeyType, rowKeyValue);
        if (id == null) {
            return null;
        }
        //Buscar el registro que coincida con el id en la lista de registros
        for (T row : getRows()) {
            if (row != null && id.equals(row.getId())) {
                return row;
            }
        }
        return null;
    }

    /**
     * Convierte el valor texto del rowkey al tipo de dato del id de la
     * entidad. Si el tipo es una clase DataRow (id compuesto por otra
     * entidad) recupera la entidad desde la base de datos.
     *
     * @param type nombre del tipo (Long, Integer, Short, String o nombre de
     * clase DataRow).
     * @param value valor del id en formato texto.
     * @return el id convertido o nulo si no fue posible convertirlo.
     */
    protected Object getIdValue(String type, String value) {
        try {
            if (type.equals("Long")) {
                return Long.valueOf(value);
            }
            if (type.equals("Integer")) {
                return Integer.valueOf(value);
            }
            if (type.equals("Short")) {
                return Short.valueOf(value);
            }
            if (type.equals("String")) {
                return value;
            }
            //Tipo DataRow
            Class clazz = Class.forName(type);
            Long id = Long.valueOf(value);
            return context.getDAO().findById(clazz, id);
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return null;
    }

    @Override
    public String getRowKey(T row) {
        if (row == null) {
            return "";
        }
        return row.getRowkey();
    }

    /**
     * Devuelve la clase de la entidad manejada por el controller.
     *
     * @return clase de la entidad o nulo si no hay controller asociado.
     */
    public Class<T> getEntityClass() {
        if (context != null) {
            return context.getType();
        }
        return null;
    }

    /**
     * Invocado por el dataTable en cada cambio de página, filtro u orden.
     * Convierte el sortBy de PrimeFaces a una expresión ORDER BY y delega la
     * carga en {@link #load(int, int, String, java.util.Map)}.
     *
     * @param first primer registro de la página (base 0).
     * @param pageSize cantidad de registros por página.
     * @param sortBy información de ordenamiento del dataTable.
     * @param filterBy filtros activos del dataTable.
     * @return los registros de la página solicitada.
     */
    @Override
    public List<T> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
        if (context == null) {
            return new ArrayList();
        }
        try {
            String order = "";
            if (sortBy != null) {
                for (SortMeta sortMeta : sortBy.values()) {
                    final SortOrder orders = sortMeta.getOrder();
                    if (orders == SortOrder.ASCENDING || orders == SortOrder.DESCENDING) {
                        order = order + " " + sortMeta.getField() + ((orders == SortOrder.ASCENDING) ? " asc" : " desc") + ",";
                    }
                }
                if (!Strings.isNullorEmpty(order)) {
                    order = order.substring(0, order.length() - 1);
                }
            } else {
                order = context.getOrder();
            }
            return load(first, pageSize, order, filterBy);
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return new ArrayList<>();
    }

    /**
     * Realiza la carga de la página: aplica los filtros del dataTable sobre el
     * controller (filtro "borrar"), ejecuta la consulta, calcula el total de
     * registros y reposiciona la página si quedó fuera de rango.
     *
     * @param first primer registro de la página (base 0).
     * @param pageSize cantidad de registros por página.
     * @param order expresión ORDER BY; si es vacía se conserva el orden ya
     * definido en el controller.
     * @param filters filtros activos del dataTable.
     * @return los registros de la página solicitada.
     */
    private List<T> load(int first, int pageSize, String order, Map<String, FilterMeta> filters) {
        if (context == null) {
            return new ArrayList();
        }
        try {
            if (!context.beforeLazyRowsLoad() || !context.isOpen()) {
                return new ArrayList<>();
            }
            if (context.getNoLazyRowsLoad()) {
                return context.getDataRows();
            }
            //Con el atributo "nolazyload" se evita re-ejecutar la consulta cuando
            //solo se refresca el componente (ver AbstractDataController.update)
            if (context.getFacesCtx().getFacesContext().getAttributes().get("nolazyload") != null) {
                Boolean noLazyLoad = (Boolean) context.getFacesCtx().getFacesContext().getAttributes().get("nolazyload");
                if (timesLoaded > 0 && noLazyLoad) {
                    return context.getDataRows();
                }
                context.getFacesCtx().getFacesContext().getAttributes().put("nolazyload", false);
            }
            if (filters == null) {
                filters = new HashMap<>();
            }
            String extraFilter = "";
            Map<String, Object> params = getParams(filters);
            if (!filters.isEmpty()) {
                extraFilter = getFilterExpression(filters);
            }
            context.setFirstRow(first);
            context.setMaxRows(pageSize);

            context.removeFilter("borrar");
            context.addFilter("borrar", extraFilter);
            if (!params.isEmpty()) {
                context.addFilterParams(params);
            }
            if (!Strings.isNullorEmpty(order)) {
                context.setOrder(order);
            }
            context.requery();
            List<T> rows = context.getDataRows();
            timesLoaded++;

            int rowCount = calculateRowCount(first, pageSize, rows);
            setRowCount(rowCount);
            //Si la página solicitada quedó fuera de rango (ej. un filtro redujo
            //el total) reposicionar en la última página válida y volver a consultar
            int firstAdjusted = recalculateFirst(first, pageSize, rowCount);
            if (firstAdjusted != first) {
                context.setFirstRow(firstAdjusted);
                context.requery();
                rows = context.getDataRows();
            }
            context.setRowSelected(null);
            context.afterLazyRowsLoad();
            return rows;
        } catch (Exception ex) {
            ErrorManager.showError(ex, LOGGER);
        }
        return new ArrayList<>();
    }

    /**
     * Calcula el total de registros de la consulta. Con noCount en falso
     * ejecuta el SELECT COUNT sobre la última consulta generada; caso
     * contrario estima el total con la página traída (si la página vino
     * completa se declara un registro más para habilitar la página siguiente
     * en el paginador).
     *
     * @param first primer registro de la página (base 0).
     * @param pageSize cantidad de registros por página.
     * @param rows registros traídos por la consulta.
     * @return total de registros (real o estimado).
     * @throws Exception error al ejecutar el count en la base.
     */
    private int calculateRowCount(int first, int pageSize, List<T> rows) throws Exception {
        if (!noCount) {
            return context.getDAO().getCount(context.getLastQuery(), context.getFilterParams()).intValue();
        }
        int loaded = (rows != null) ? rows.size() : 0;
        if (loaded < pageSize) {
            return first + loaded;
        }
        return first + pageSize + 1;
    }

    /**
     * Determina si un filtro del dataTable debe procesarse. Se ignoran los
     * filtros sin valor y el filtro global (no mapea a una columna).
     *
     * @param e entrada del map de filtros.
     * @return verdadero si el filtro se traduce a la consulta.
     */
    private boolean isFilterable(Map.Entry<String, FilterMeta> e) {
        return e.getValue() != null
                && e.getValue().getFilterValue() != null
                && !FilterMeta.GLOBAL_FILTER_KEY.equals(e.getKey());
    }

    /**
     * Utilizado por el metodo load, devuelve un map con el nombre del campo
     * como clave (sin puntos) y el valor del filtro convertido al tipo de dato
     * de la columna. En los campos alfanuméricos aplica la máscara de búsqueda
     * y los comodines según el filterMode de la columna.
     *
     * @param filters filtros activos del dataTable (campo, FilterMeta).
     * @return map de parámetros para la consulta.
     */
    private Map<String, Object> getParams(Map<String, FilterMeta> filters) {
        Map<String, Object> params = new HashMap<>();
        for (Map.Entry<String, FilterMeta> e : filters.entrySet()) {
            if (!isFilterable(e)) {
                continue;
            }
            String fieldName = e.getKey();
            String key = fieldName.replace(".", "");
            Object value = e.getValue().getFilterValue();
            Class clase = DataInfo.getFieldType(getEntityClass(), fieldName);
            if (clase == null) {
                //Campo no encontrado en la entidad, pasar el valor sin convertir
                params.put(key, value);
            } else if (String.class.isAssignableFrom(clase)) {
                //Si es alfanumerico, ver si tiene mascara de busqueda
                String texto = getValueWithFilterMask(value.toString(), fieldName);
                switch (getFilterMode(fieldName).toLowerCase()) {
                    case "exact":
                    case "exact_trim":
                    case "exact_ltrim":
                        params.put(key, texto);
                        break;
                    case "contain_ltrim":
                        //Busqueda contenida en la porción izquierda
                        params.put(key, texto + "%");
                        break;
                    case "contain_rtrim":
                        //Busqueda contenida en la porción derecha
                        params.put(key, "%" + texto);
                        break;
                    default:
                        //Busqueda contenida dentro del campo
                        params.put(key, "%" + texto + "%");
                        break;
                }
            } else if (clase.isInstance(value)) {
                //El componente ya convirtió el valor al tipo de la columna
                params.put(key, value);
            } else if (Long.class.isAssignableFrom(clase)) {
                params.put(key, Long.valueOf(value.toString().trim()));
            } else if (Integer.class.isAssignableFrom(clase)) {
                params.put(key, Integer.valueOf(value.toString().trim()));
            } else if (Short.class.isAssignableFrom(clase)) {
                params.put(key, Short.valueOf(value.toString().trim()));
            } else if (BigDecimal.class.isAssignableFrom(clase)) {
                params.put(key, new BigDecimal(value.toString().trim()));
            } else if (LocalDateTime.class.isAssignableFrom(clase)) {
                if (value instanceof LocalDate) {
                    params.put(key, ((LocalDate) value).atStartOfDay());
                } else if (value instanceof Date) {
                    params.put(key, LocalDates.toDateTime((Date) value));
                } else {
                    params.put(key, LocalDates.toDateTime(value.toString()));
                }
            } else if (Date.class.isAssignableFrom(clase)) {
                params.put(key, Dates.toDate(value.toString()));
            } else if (Boolean.class.isAssignableFrom(clase)) {
                params.put(key, Boolean.valueOf(value.toString()));
            } else {
                params.put(key, value);
            }
        }
        return params;
    }

    /**
     * Utilizado en el metodo load, devuelve la sentencia WHERE que filtra los
     * datos de la lista. Si el controller implementa
     * {@code onGetFilterString} se usa esa expresión; caso contrario se genera
     * una condición por columna coherente con los parámetros de
     * {@link #getParams}.
     *
     * @param filters filtros activos del dataTable (campo, FilterMeta).
     * @return expresión WHERE a agregar a la consulta.
     */
    private String getFilterExpression(Map<String, FilterMeta> filters) {
        if (context == null) {
            return "";
        }
        String queryWhere;
        // Si en el controller se define los filtros
        queryWhere = context.onGetFilterString(filters);
        if (!queryWhere.isEmpty()) {
            return queryWhere;
        }
        String filter;
        String separador = "";
        for (Map.Entry<String, FilterMeta> e : filters.entrySet()) {
            if (!isFilterable(e)) {
                continue;
            }
            String fieldName = e.getKey();
            String key = fieldName.replace(".", "");
            Class clase = DataInfo.getFieldType(getEntityClass(), fieldName);
            if (clase != null && String.class.isAssignableFrom(clase)) {
                switch (getFilterMode(fieldName).toLowerCase()) {
                    case "exact":
                        // Si el tipo de busqueda es exacta
                        filter = separador + " o." + fieldName + " = :" + key;
                        break;
                    case "exact_trim":
                        filter = separador + " trim(o." + fieldName + ") = :" + key;
                        break;
                    case "exact_ltrim":
                        filter = separador + " ltrim(o." + fieldName + ") = :" + key;
                        break;
                    case "contain_ltrim":
                        filter = separador + " ltrim(upper(o." + fieldName + ")) like upper(:" + key + ")";
                        break;
                    case "contain_rtrim":
                        filter = separador + " rtrim(upper(o." + fieldName + ")) like upper(:" + key + ")";
                        break;
                    default:
                        // Si el campo es string buscar un valor contenido en el campo
                        filter = separador + " upper(o." + fieldName + ") like upper(:" + key + ")";
                        break;
                }
            } else {
                filter = separador + " o." + fieldName + " = :" + key;
            }
            queryWhere += filter;
            separador = " and ";
        }
        return queryWhere;
    }

    /**
     * Busca la definición de la columna en el modelo del dataTable, primero
     * por el atributo filter y luego por el nombre de la columna.
     *
     * @param columnName nombre del campo filtrado.
     * @return el modelo de la columna o nulo si no existe.
     */
    private IColumnModel findColumn(String columnName) {
        if (context == null || context.getDataTable() == null
                || context.getDataTable().getColumns() == null) {
            return null;
        }
        List<IColumnModel> columns = context.getDataTable().getColumns();
        // Buscar por el campo filter
        for (IColumnModel column : columns) {
            if (Fn.nvl(column.getFilter(), "").equalsIgnoreCase(columnName)) {
                return column;
            }
        }
        // Buscar por el nombre de la columna
        for (IColumnModel column : columns) {
            if (Fn.nvl(column.getName(), "").equalsIgnoreCase(columnName)) {
                return column;
            }
        }
        return null;
    }

    /**
     * Devuelve el modo de filtrado definido en la columna (exact, exact_trim,
     * exact_ltrim, contain, contain_trim, contain_ltrim, contain_rtrim).
     *
     * @param columnName nombre del campo filtrado.
     * @return el modo de filtrado o vacío si no fue definido.
     */
    private String getFilterMode(String columnName) {
        IColumnModel column = findColumn(columnName);
        if (column == null) {
            return "";
        }
        return Fn.nvl(column.getFilterMode(), "");
    }

    /**
     * Devuelve la máscara de búsqueda definida en la columna
     * (right_blank_N, left_blank_N o replace('a','b')).
     *
     * @param columnName nombre del campo filtrado.
     * @return la máscara o vacío si no fue definida.
     */
    private String getFilterMask(String columnName) {
        IColumnModel column = findColumn(columnName);
        if (column == null) {
            return "";
        }
        return Fn.nvl(column.getFilterMask(), "");
    }

    /**
     * Aplica la máscara de búsqueda de la columna sobre el valor filtrado.
     * Con right_blank_N/left_blank_N rellena con espacios hasta N posiciones;
     * con replace('a','b') sustituye texto. Sin máscara devuelve el valor
     * recortado (trim).
     *
     * @param value valor del filtro.
     * @param columnName nombre del campo filtrado.
     * @return el valor con la máscara aplicada.
     */
    private String getValueWithFilterMask(String value, String columnName) {
        if (Fn.nvl(value, "").isEmpty()) {
            return value;
        }
        String filterMask = getFilterMask(columnName);
        if (Fn.nvl(filterMask, "").isEmpty()) {
            return value.trim();
        }
        try {
            if (filterMask.toLowerCase().contains("_blank_")) {
                String[] parts = filterMask.split("_");
                Integer size = Integer.valueOf(parts[2]);
                if (parts[0].equalsIgnoreCase("right")) {
                    value = Strings.rightPad(value.trim(), size, " ");
                } else if (parts[0].equalsIgnoreCase("left")) {
                    value = Strings.leftPad(value.trim(), size, " ");
                }
            } else if (filterMask.toLowerCase().contains("replace")) {
                String[] parts = filterMask.split(",");
                parts[0] = Strings.substr(parts[0], 8).replace("'", "");
                parts[1] = Strings.substr(parts[1], 0, parts[1].trim().length() - 1).replace("'", "");
                value = value.replace(parts[0], parts[1]);
            }
            return value;
        } catch (Exception exp) {
            //Mascara mal formada, se ignora y se filtra con el valor original
            LOGGER.debug("Máscara de filtro inválida en columna " + columnName + ": " + filterMask);
        }
        return value.trim();
    }

    /**
     * En PrimeFaces 11+ este metodo se invoca <b>antes</b> de load (ver
     * DataTable.loadLazyScrollData) para fijar el rowCount con el que se
     * recalcula el primer registro de la página. Este modelo calcula el total
     * real dentro de load junto con los datos (una sola consulta a la base,
     * respetando la bandera noCount y los filtros propios del controller),
     * por lo que aquí se devuelve el último total conocido y load lo corrige
     * luego vía setRowCount/recalculateFirst. No se devuelve 0 porque los
     * caminos que reutilizan la página cacheada (noLazyRowsLoad, atributo
     * "nolazyload") retornan sin recalcular el total y vaciarían el paginador.
     *
     * @param filterBy filtros activos del dataTable (no se utiliza).
     * @return el último total de registros conocido.
     */
    @Override
    public int count(Map<String, FilterMeta> filterBy) {
        return getRowCount();
    }
}
