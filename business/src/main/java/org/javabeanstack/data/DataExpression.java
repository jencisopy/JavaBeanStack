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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.javabeanstack.util.Fn.nvl;
import static org.javabeanstack.util.Strings.isNullorEmpty;
import static org.javabeanstack.util.Strings.textMerge;
import static org.javabeanstack.util.Fn.inList;


/**
 * Contiene metodos necesarios para generar expresiones utilizadas principalmente en los selects nativos. 
 * Es utilizado principalmente por DataNativeQuery().
 * 
 * @author Jorge Enciso
 */
public class DataExpression implements IDataExpression{

    private List<ElementExpr> exprList = new ArrayList<>();
    private boolean openparenthesis = false;
    private Map<String, Object> sentenceParams = new HashMap();

    
    public final List<ElementExpr> getExpressionList() {
        return exprList;
    }

    /**
     * Lee los parámetros de la sentencia
     *
     * @return parámetros de la sentencia
     */
    @Override
    public Map<String, Object> getSentenceParams() {
        return sentenceParams;
    }

    /**
     * Asignar los parametros de la sentencia
     *
     * @param sentenceParams
     */
    @Override
    public void setSentenceParams(Map<String, Object> sentenceParams) {
        this.sentenceParams = sentenceParams;
    }

    /**
     * Agrega un parámetro
     *
     * @param key clave del parámetro
     * @param value valor del parámetro
     */
    @Override
    public void addSentenceParam(String key, Object value) {
        this.sentenceParams.put(key, value);
    }


    public final class ElementExpr {

        String operador = "and";
        String expresion;
        String grupo = "";
        Map<String, String> params;

        public ElementExpr(String expr, Map<String, String> params) {
            this.expresion = expr;
            this.params = params;
        }

        public ElementExpr(String expr, Map<String, String> params, String operator) {
            this.expresion = expr;
            this.params = params;
            if (!isNullorEmpty(operator)) {
                this.operador = operator;
            }
        }

        public ElementExpr(String expr, Map<String, String> params, String operator, String group) {
            this.expresion = expr;
            this.params = params;
            this.grupo = group;
            if (!isNullorEmpty(operator)) {
                this.operador = operator;
            }
        }
    }

    /**
     * Limpia las expresiones
     */
    @Override
    public void clearExpressions() {
        exprList.clear();
    }

    /**
     * Inserta un parentesis a la expresión
     */
    @Override
    public void openParenthesis() {
        ElementExpr element = new ElementExpr("", null, "(");
        exprList.add(element);
        openparenthesis = true;
    }

    /**
     * Inserta un cierre de parentesis en la expresión
     */
    @Override
    public void closeParenthesis() {
        ElementExpr element = new ElementExpr("", null, ")");
        exprList.add(element);
        openparenthesis = false;
    }

    /**
     * Agrega un operador a la expresión
     *
     * @param operator operador (and, or, in)
     */
    @Override
    public void addOperator(String operator) {
        ElementExpr element = new ElementExpr("", null, operator);
        exprList.add(element);
    }

    /**
     * Agrega un operador a la expresión
     *
     * @param operator operador (and, or, in)
     * @param group
     */
    @Override
    public void addOperator(String operator, String group) {
        ElementExpr element = new ElementExpr("", null, operator, group);
        exprList.add(element);
    }

    /**
     * Agrega una expresión
     *
     * @param expr expresión
     * @param keyParams lista de parametros con sus valores.
     */
    @Override
    public void addExpression(String expr, Map<String, String> keyParams) {
        addExpression(expr, keyParams, null);
    }

    /**
     * Agrega una expresión
     *
     * @param expr expresión
     * @param keyParams lista de parametros con sus valores.
     * @param operator operador (and, or, in)
     *
     */
    @Override
    public void addExpression(String expr, Map<String, String> keyParams, String operator) {
        ElementExpr element = new ElementExpr(expr, keyParams, operator);
        if (openparenthesis && inList(element.operador.toLowerCase(), "and", "or")) {
            element.operador = "";
        }
        exprList.add(element);
        openparenthesis = false;
    }

    /**
     * Agrega una expresión
     *
     * @param expr expresión
     * @param keyParams lista de parametros con sus valores.
     * @param operator operador (and, or, in)
     * @param group
     *
     */
    @Override
    public void addExpression(String expr, Map<String, String> keyParams, String operator, String group) {
        ElementExpr element = new ElementExpr(expr, keyParams, operator, group);
        if (openparenthesis && inList(element.operador.toLowerCase(), "and", "or")) {
            element.operador = "";
        }
        exprList.add(element);
        openparenthesis = false;
    }

    /**
     * Agrega una expresión
     *
     * @param expr expresión
     * @param keyParamList lista de parametros
     *
     */
    @Override
    public void addExpression(String expr, Object... keyParamList) {
        Map<String, String> params = new HashMap<>();
        int i = 0;
        String expr1, expr2;
        if (keyParamList != null && keyParamList.length >= 1) {
            while (true) {
                expr1 = keyParamList[i].toString();
                if (i + 1 < keyParamList.length) {
                    expr2 = keyParamList[i + 1].toString();
                } else {
                    expr2 = null;
                }
                params.put(expr1, expr2);
                i = i + 2;
                if (i >= keyParamList.length) {
                    break;
                }
            }
        }
        addExpression(expr, params);
    }

    /**
     * Agrega una expresión
     *
     * @param dataExpression expresión
     */
    @Override
    public void addExpressions(IDataExpression dataExpression) {
        ((DataExpression) dataExpression).getExpressionList().forEach( expr -> {
            exprList.add(expr);
        });
        dataExpression.getSentenceParams().entrySet().forEach( element -> {
            this.getSentenceParams().put(element.getKey(), element.getValue());
        });
    }

    /**
     * Genera la sentencia y lo devuelve
     *
     * @return la expresión resultante
     */
    @Override
    public String getSentence() {
        return getSentence("");
    }

    /**
     * Genera la sentencia y lo devuelve
     *
     * @param group
     * @return la expresión resultante
     */
    @Override
    public String getSentence(String group) {
        String devolver = "";
        String expr;
        String linefeed = "";
        for (int i = 0; i < exprList.size(); i++) {
            ElementExpr e = exprList.get(i);
            // Si se pasa un grupo solo procesar ese grupo
            if (!group.isEmpty() && !nvl(e.grupo, "").equals(group)) {
                continue;
            }
            if (i == 0 && inList(e.operador.toLowerCase(), "and", "or")) {
                e.operador = "";
            }
            expr = e.expresion;
            if (e.params != null) {
                expr = textMerge(expr, e.params);
            }
            if (!isNullorEmpty(e.expresion)) {
                expr = " (" + expr + ") ";
            }
            devolver += linefeed + e.operador + expr;
            linefeed = "\n";
        }
        return devolver;
    }
    
    /**
     * Remueve una expresión 
     * @param elementNumber nro de elemento en la lista que va a ser eliminada.
     * @return IDataExpression resultante.
     */
    @Override
    public IDataExpression removeExpression(int elementNumber) {
        IDataExpression dataExpr = new DataExpression();
        ElementExpr element;
        for (int i=0; i < exprList.size();i++){
            if (i != elementNumber){
                element = exprList.get(i);                            
                dataExpr.addExpression(element.expresion, 
                                        element.params,
                                        element.operador,
                                        element.grupo);
            }
        }
        return dataExpr;
    }

    /**
     * Remueve un grupo de expresiones de la lista.
     * @param group grupo.
     * @return IDataExpression resultante.
     */
    @Override
    public IDataExpression removeExpression(String group) {
        IDataExpression dataExpr = new DataExpression();
        for (ElementExpr element: exprList){
            if (!element.grupo.equalsIgnoreCase(group)){
                dataExpr.addExpression(element.expresion, 
                                        element.params,
                                        element.operador,
                                        element.grupo);
            }
        }
        return dataExpr;
    }
}
