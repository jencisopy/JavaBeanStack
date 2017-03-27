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

import java.util.Map;

/**
 * Contiene metodos necesarios para generar expresiones utilizadas principalemente en los selects nativos. 
 * Es utilizado principalmente en DataNativeQuery()
 *   
 * @author Jorge Enciso
 */
public interface IDataExpression {
    /**
     *  Limpia las expresiones
     */
    public void clearExpressions();
    /**
     *  Inserta un parentesis en la expresión
     */
    public void openParenthesis();    
    /**
     *  Inserta un cierre de parentesis en la expresión
     */
    public void closeParenthesis();    
    /**
     *  Agrega un operador a la expresión 
     * @param operator operador  (and, or, in)
     */
    public void addOperator(String operator);
    /**
     * Agrega un operador a la expresión 
     * @param operator operador  (and, or, in)
     * @param group
     */
    public void addOperator(String operator,String group);
    /**
     * Agrega una expresión
     * @param expr      expresión
     * @param keyParams    lista de parametros con sus valores.
     */
    public void addExpression(String expr, Map<String, String> keyParams);
    /**
     * Agrega una expresión
     * @param expr      expresión
     * @param keyParams    lista de parametros con sus valores.
     * @param operator  operador (and, or, in)
     * 
     */
    public void addExpression(String expr, Map<String, String> keyParams, String operator);
    /**
     * Agrega una expresión
     * @param expr      expresión
     * @param keyParams    lista de parametros con sus valores.
     * @param operator  operador (and, or, in)
     * @param group
     * 
     */
    public void addExpression(String expr, Map<String, String> keyParams, String operator, String group);
    /**
     * Agrega una expresión
     * @param expr      expresión
     * @param keyParamList lista de parametros
     * 
     */
    public void addExpression(String expr, Object... keyParamList);
    /**
     * Genera la sentencia y lo devuelve
     * @return  la sentencia select.
     */
    public String getSentence();
    /**
     * Genera la sentencia y lo devuelve
     * @param group
     * @return  la sentencia select.
     */
    public String getSentence(String group);
    /**
     * Devuelve el objeto params asignado en setSentenceParams()
     * @return  
     */
    public Map<String,Object> getSentenceParams();
    /**
     * Asigna los valores de los parametros de la sentencia
     * @param sentenceParams
     */
    public void setSentenceParams(Map<String,Object> sentenceParams);
    /**
     * Agrega un valor de parámetro al objeto sentenceParams
     * @param key       
     * @param value
     */
    public void addSentenceParam(String key, Object value);
}
