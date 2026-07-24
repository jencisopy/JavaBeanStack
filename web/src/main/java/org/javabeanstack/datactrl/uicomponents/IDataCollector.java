/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2015 - 2027 Jorge Enciso
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
package org.javabeanstack.datactrl.uicomponents;
import java.io.Serializable;
import java.util.Map;
/**
 * Contrato del recolector de valores para componentes de UI (principalmente
 * {@code DataTable}): obtiene valores de campos desde tablas externas o
 * mediante cálculos especiales.
 *
 * @author Jorge Enciso
 */
public interface IDataCollector extends Serializable {
    /**
     * Asigna la fuente de datos del recolector.
     *
     * @param params parámetros de la fuente de datos.
     */
    void setDataSource(Map<String, Object> params);

    /**
     * Obtiene el valor (externo o calculado) según los parámetros indicados.
     *
     * @param params parámetros para resolver el valor.
     * @return valor obtenido.
     */
    Object getDataValue(Map<String, Object> params);
}
