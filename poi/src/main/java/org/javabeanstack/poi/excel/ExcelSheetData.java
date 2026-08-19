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
package org.javabeanstack.poi.excel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.javabeanstack.data.IDataQueryModel;

/**
 * Una hoja de un libro de Excel: su nombre, sus cabeceras y sus filas.
 *
 * <p>Existe para que un libro pueda tener <b>varias hojas</b>
 * ({@link ExcelUtil#toExcel(java.util.List)}) y, sobre todo, para que una hoja
 * <b>sin filas</b> siga teniendo cabeceras. Ese es el motivo de que las
 * columnas se declaren aparte: {@link IDataQueryModel} las lleva adentro de
 * cada fila, así que una lista vacía no sabe cómo se llaman sus columnas y la
 * hoja saldría en blanco —indistinguible de un error—.</p>
 *
 * <p>Si no se declaran columnas se toman las de la primera fila, que es el
 * comportamiento de siempre.</p>
 *
 * @author Jorge Enciso
 */
public class ExcelSheetData implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final String[] columns;
    private final List<IDataQueryModel> rows;
    private Map<Integer, Integer> widthOverrides;

    /**
     * Crea una hoja tomando las cabeceras de la primera fila.
     *
     * @param name nombre de la hoja.
     * @param rows filas; puede ser nula o vacía.
     */
    public ExcelSheetData(String name, List<IDataQueryModel> rows) {
        this(name, null, rows);
    }

    /**
     * Crea una hoja con cabeceras declaradas.
     *
     * @param name nombre de la hoja.
     * @param columns nombres de las columnas. Si es nulo se toman los de la
     * primera fila.
     * @param rows filas; puede ser nula o vacía.
     */
    public ExcelSheetData(String name, String[] columns, List<IDataQueryModel> rows) {
        this.name = name;
        this.columns = (columns == null) ? null : columns.clone();
        this.rows = (rows == null) ? new ArrayList<>() : rows;
    }

    /**
     * Devuelve el nombre de la hoja.
     *
     * @return el nombre.
     */
    public String getName() {
        return name;
    }

    /**
     * Devuelve los nombres de las columnas.
     *
     * <p>Los declarados en el constructor o, en su defecto, los de la primera
     * fila. Un arreglo vacío significa que la hoja no tiene ni columnas
     * declaradas ni filas de dónde deducirlas.</p>
     *
     * @return los nombres; nunca nulo.
     */
    public String[] getColumns() {
        if (columns != null) {
            return columns.clone();
        }
        if (rows.isEmpty()) {
            return new String[0];
        }
        return rows.get(0).getColumnList().clone();
    }

    /**
     * Devuelve las filas de la hoja.
     *
     * @return las filas; nunca nulo.
     */
    public List<IDataQueryModel> getRows() {
        return rows;
    }

    /**
     * Devuelve los anchos de columna forzados.
     *
     * @return los anchos por índice de columna, o nulo si no hay.
     */
    public Map<Integer, Integer> getWidthOverrides() {
        return widthOverrides;
    }

    /**
     * Fuerza el ancho de algunas columnas.
     *
     * @param widthOverrides anchos en caracteres, por índice de columna.
     * @return esta misma hoja, para encadenar.
     */
    public ExcelSheetData setWidthOverrides(Map<Integer, Integer> widthOverrides) {
        this.widthOverrides = widthOverrides;
        return this;
    }
}
