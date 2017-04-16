/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 Jorge Enciso
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

import java.util.List;
import java.util.Objects;
import org.javabeanstack.util.Fn;

/**
 * Clase que ofrece funcionalidades para acceder de varias maneras al registro
 * de datos (row) resultante de un nativequery. Es utilizado principalmente en 
 * la clase DataNativeQuery
 * 
 * El test unitario se encuentra en TestProjects clase
 *     py.com.oym.test.data.TestDataQueryModel.
 * 
 * @author Jorge Enciso 
 */
public class DataQueryModel implements IDataQueryModel{
    private Object   columnId;
    private String[] columnList;
    private Object   row;

    /**
     * 
     * @return id del registro
     */
    @Override
    public Object getColumnId() {
        if (row == null){
            return null;
        }
        if (columnId == null){
            return getColumn(0);
        }
        return columnId;
    }

    /**
     * 
     * @param index nro de columna
     * @return valor de la columna
     */
    @Override
    public Object getColumn(int index) {
        if (row == null){
            return null;
        }
        if (row instanceof Object[]){
            Object[] colList = (Object[])row;
            if (colList.length <= index){
                return null;
            }
            return colList[index];
        }    
        return row;
    }

    /**
     * 
     * @param columnName nombre de la columna
     * @return valor de la columna
     */
    @Override
    public Object getColumn(String columnName) {
        if (!IsColumnMetaDataExist()){
            return null;
        }
        // Buscar un nombre de columna en la matriz
        int index = Fn.findInMatrix(columnList, columnName, false);
        if (index < 0){
            return null;
        }
        return getColumn(index);
    }

    /**
     * 
     * @param columnName nombre de la columna
     * @return valor de la columna como caracter.
     */
    @Override
    public String getColumnStr(String columnName) {
        if (!IsColumnMetaDataExist()){
            return null;
        }
        // Buscar un nombre de columna en la matriz
        int index = Fn.findInMatrix(columnList, columnName, false);
        if (index < 0){
            return null;
        }
        return getColumn(index).toString();
    }
    
    /**
     * 
     * @param index  nro. de la columna
     * @return el nombre de la columna
     */
    @Override
    public String getColumnName(int index) {
        if (!IsColumnMetaDataExist()){
            return null;
        }
        if (columnList.length <= index){
            return null;
        }
        return columnList[index].trim();
    }

    /**
     * Devuelve la lista de columnas tipo String[]
     * @return lista de columnas.
     */
    @Override
    public String[] getColumnList() {
        return columnList;
    }

    
    /**
     * Asigna la lista de columnas. Debe coincidir con el resultado del nativequery
     * @param columnList lista de columnas
     */
    @Override
    public void setColumnList(String[] columnList) {
        this.columnList = columnList;
    }
    

    /**
     * Asigna cual es la columna que identifica al registro.
     * @param index 
     */
    @Override
    public void setColumnId(int index) {
        columnId = getColumn(index);
    }
    
    /**
     * 
     * @return registro resultante del nativequery query
     */
    @Override
    public Object getRow() {
        return row;
    }

    /**
     * Sobreescribe la fila actual.
     * @param row 
     */
    @Override
    public void setRow(Object row) {
        this.row = row;
    }

    @Override
    public void setColumn(int index, Object value) {
        if (row instanceof Object[]){
            Object[] colList = (Object[])row;
            if (colList.length > index){
                colList[index] = value;
                return;
            }
        }
        row = value;
    }

    @Override
    public void setColumn(String columnName, Object value) {
        if (!IsColumnMetaDataExist()){
            return;
        }
        // Buscar un nombre de columna en la matriz
        int index = Fn.findInMatrix(columnList, columnName, false);
        if (index < 0){
            return;
        }
        setColumn(index, value);        
    }
    
    
    
    protected Boolean IsColumnMetaDataExist(){
        if (columnList == null){
            return false;
        }
        if (columnList.length == 0){
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 47 * hash + Objects.hashCode(this.getColumnId());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DataQueryModel other = (DataQueryModel) obj;
        return Objects.equals(this.getColumnId(), other.getColumnId());
    }

    /**
     * Busca un registro o elemento en la lista 
     * @param dataList  lista de registros.
     * @param searchId  identificador del registro.
     * @return nro. de elemento donde encuentra el registro (elemento = 0 es la primera fila)
     */
    public static int searchById(List<IDataQueryModel> dataList, Object searchId) {
        //Si esta vacio la lista
        if (dataList.isEmpty()){
            return -1;
        }
        //Los ids deben ser del mismo tipo
        boolean castToString = false;
        if (searchId.getClass() != dataList.get(0).getColumnId().getClass()){
            castToString = true;
        }
        // Buscar
        for (int i=0;i < dataList.size();i++){
            IDataQueryModel element = dataList.get(i);
            if (castToString){
                String expr1 = element.getColumnId().toString();
                String expr2 = searchId.toString();
                if (expr1.equals(expr2)){
                    return i;
                }
            }
            else if (Objects.equals(element.getColumnId(), searchId)){
                return i;
            }
        }
        return -1;
    }
}
