/*
* Copyright (c) 2015-2018 OyM System Group S.A.
* Capitan Cristaldo 464, Asunción, Paraguay
* All rights reserved. 
*
* NOTICE:  All information contained herein is, and remains
* the property of OyM System Group S.A. and its suppliers,
* if any.  The intellectual and technical concepts contained
* herein are proprietary to OyM System Group S.A.
* and its suppliers and protected by trade secret or copyright law.
* Dissemination of this information or reproduction of this material
* is strictly forbidden unless prior written permission is obtained
* from OyM System Group S.A.
 */
package org.javabeanstack.datactrl.uicomponents;
import java.io.Serializable;
import java.util.Map;
/**
 *
 * @author Jorge Enciso
 * 
 * Para ser utilizado principalmente en DataTable. Busca valores de campos en
 * tablas externas o calculos expeciales.
 * 
 * 
 */
public interface IDataCollector extends Serializable {
    void setDataSource(Map<String, Object> params);
    Object getDataValue(Map<String, Object> params);
}
