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
package org.javabeanstack.datactrl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.javabeanstack.annotation.FieldFilter;
import org.javabeanstack.error.ErrorManager;
import static org.javabeanstack.util.Strings.*;

/**
 * Este componente genera una expresión que se utiliza para la selección
 * de datos en los ABMs.
 * Los nombres de los metodos getters debe empezar por "get" y seguido por el 
 * nombre del atributo.
 * 
 * @author Jorge Enciso
 */
public class DataFilter {
    private String filterExpression;
    private Map<String, Object> parameters = new HashMap();

    public String getFilterExpression() {
        return filterExpression;
    }

    public void setFilterExpression(String filterExpression) {
        this.filterExpression = filterExpression;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void addParam(String key, Object value) {
        parameters.put(key, value);
    }

    public void clear() {
        parameters.clear();
        filterExpression = "";
    }

    /**
     * Selecciona datos en un controller.
     * @param <T>
     * @param context controller.
     */
    public <T extends AbstractDataObject> void execute(T context) {
        context.setFilter(filterExpression);
        context.setFilterParams(parameters);
        context.requery();
    }

    /**
     * Crea la sentencia o expresión para el filtrado, utiliza a getSentence()
     * para tal efecto.
     */
    public void createSentence() {
        Map<String, Object> values = getSentence();
        parameters = (Map<String, Object>) values.get("parameters");
        filterExpression = (String) values.get("expression");
    }

    /**
     * Genera la expresión del filtro a partir de los atributos y las anotaciones
     * en los metodos correspondientes.
     * 
     * @return un objeto con la expresión del filtro y los parámetros que se van
     * a utilizar en dicho filtro.
     */
    public final Map<String, Object> getSentence() {
        Map<String, Object> params = new HashMap();
        List<FieldFilter> fields = new ArrayList();
        Map<FieldFilter, Object> params2 = new HashMap();
        for (Method method : this.getClass().getMethods()) {
            FieldFilter annotation = method.getAnnotation(FieldFilter.class);
            if (annotation != null) {
                try {
                    method.setAccessible(true);
                    Object valor = method.invoke(this);
                    String valorStr = "no";
                    if (valor instanceof String) {
                        valorStr = (String) valor;
                    }
                    //Si es nulo y no se tiene que incluir en el filtro
                    if ((valor == null || isNullorEmpty(valorStr))
                            && annotation.nullOrEmptyExpression().isEmpty()) {
                        continue;
                    }
                    params2.put(annotation, valor);
                    //Si es nulo y se debe incluir en el filtro
                    if (valor == null || isNullorEmpty(valorStr)) {
                        params2.put(annotation, null);
                    }
                    //Si el valor del campo es caracter, configurar modo de busqueda
                    if (valor instanceof String && !isNullorEmpty((String) valor)) {
                        if (annotation.expression().toLowerCase().contains("like")) {
                            valor = ((String) valor).trim();
                            if (annotation.mode().equalsIgnoreCase("contain_ltrim")) {
                                valor = ((String) valor).trim() + "%";
                            } else if (annotation.mode().equalsIgnoreCase("contain_rtrim")) {
                                valor = "%" + ((String) valor).trim();
                            } else if (annotation.mode().isEmpty()
                                    || annotation.mode().equalsIgnoreCase("contain")
                                    || annotation.mode().equalsIgnoreCase("contain_trim")) {
                                valor = "%" + ((String) valor).trim() + "%";
                            }
                        }
                    }
                    String paramName = substr(method.getName(), 3);
                    paramName = left(paramName, 1).toLowerCase() + substr(paramName, 1);
                    params.put(paramName, valor);
                    if (!annotation.expression().isEmpty()) {
                        fields.add(annotation);
                    }
                } catch (Exception ex) {
                    ErrorManager.showError(ex, Logger.getLogger(getClass()));
                }
            }
        }
        //Se ordena la lista de acuerdo al atributo order de la anotación,
        //de tal forma a que la expresión se genere en el orden deseado.
        Collections.sort(fields, (FieldFilter p1, FieldFilter p2) -> p1.order().compareTo(p2.order()));
        String filterExpressionResult = "";
        String operator = "";
        //Contrucción de la expresión
        for (FieldFilter field : fields) {
            Object paramValue = params2.get(field);
            if (paramValue == null) {
                filterExpressionResult += operator + "(" + field.nullOrEmptyExpression() + ")";
            } else {
                filterExpressionResult += operator + "(" + field.expression() + ")";
            }
            operator = " and ";
        }
        Map<String, Object> retornar = new HashMap();
        retornar.put("expression", filterExpressionResult);
        retornar.put("parameters", params);
        return retornar;
    }
}