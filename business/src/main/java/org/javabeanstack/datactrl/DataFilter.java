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
package org.javabeanstack.datactrl;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Jorge Enciso
 */
public class DataFilter {
    private String filterExpression;
    private final Map<String, Object> parameters = new HashMap();

    public String getFilterExpression() {
        return filterExpression;
    }

    public void setFilterExpression(String filterExpression) {
        this.filterExpression = filterExpression;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void addParam(String key, Object value){
        parameters.put(key, value);
    }

    public void clear(){
        parameters.clear();
        filterExpression = "";
    }
    
    public <T extends AbstractDataObject> void execute(T context){
        context.setFilter(filterExpression);
        context.setFilterParams(parameters);
        context.requery();
    }
}
