package org.javabeanstack.data;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Jorge Enciso
 */
public class DataExpressionTest {
    
    public DataExpressionTest() {
    }
    
    @Test
    public void test() {
        System.out.println("DataExpression-test1");
        DataExpression sentence = new DataExpression();
        Map<String, String> params = new HashMap<>();
        params.put("var1", "pp");
        sentence.addExpression("pedro = {var1}", params);
        params.put("var1", "pp");
        params.put("var2", "pp");
        sentence.addExpression("juan between '{var1}' and '{var2}'", params);
        String result = sentence.getSentence().trim();
        String expResult = "(pedro = pp) \nand (juan between 'pp' and 'pp')";
        assertEquals(expResult, result);
        System.out.println(result);
    }
    
    @Test
    public void test2() {
        System.out.println("DataExpression-test2");
        DataExpression sentence = new DataExpression();
        sentence.addExpression("pedro = {var1}", "var1","pp2");
        sentence.addExpression("juan between '{var1}' and '{var2}'", "var1","pp2","var2","pp2");
        String result = sentence.getSentence().trim();
        String expResult = "(pedro = pp2) \nand (juan between 'pp2' and 'pp2')";
        assertEquals(expResult, result);
        System.out.println(result);
    }    
    
    @Test
    public void test3() {
        System.out.println("DataExpression-test3");        
        DataExpression sentence = new DataExpression();
        sentence.openParenthesis();                
        sentence.addExpression("select '' from {schema}.moneda where idmoneda = item.idmoneda", null, "IN");
        sentence.addExpression("juan between '{var1}' and '{var2}'", "var1","pp2","var2","pp2");
        sentence.addExpression("juan like '{var1}%'", "var1","pp2");        
        sentence.closeParenthesis(); 
        
        sentence.addOperator("AND");        
        
        sentence.openParenthesis();        
        sentence.addExpression("fecha = :fecha");
        sentence.addExpression("logico = {true}");                        
        sentence.closeParenthesis();      
        
        String result = sentence.getSentence().trim();
        String expResult = "(\n" +
                            "IN (select '' from {schema}.moneda where idmoneda = item.idmoneda) \n" +
                            "and (juan between 'pp2' and 'pp2') \n" +
                            "and (juan like 'pp2%') \n" +
                            ")\n" +
                            "AND\n" +
                            "(\n" +
                            " (fecha = :fecha) \n" +
                            "and (logico = {true}) \n" +
                            ")";
        assertEquals(expResult, result);        
        System.out.println(result);
    }        

    @Test
    public void test3b() {
        System.out.println("DataExpression-test3b");        
        DataExpression sentence = new DataExpression();
        sentence.openParenthesis();                
        sentence.addExpression("juan between '{var1}' and '{var2}'", "var1","pp2","var2","pp2");
        sentence.addExpression("juan like '{var1}%'", "var1","pp2");        
        sentence.closeParenthesis(); 
        
        sentence.addOperator("AND");        
        
        sentence.openParenthesis();        
        sentence.addExpression("fecha = :fecha");
        sentence.addExpression("logico = {true}");                        
        sentence.closeParenthesis();      
        
        String result = sentence.getSentence().trim();
        String expResult = "(\n" +
                            " (juan between 'pp2' and 'pp2') \n" +
                            "and (juan like 'pp2%') \n" +
                            ")\n" +
                            "AND\n" +
                            "(\n" +
                            " (fecha = :fecha) \n" +
                            "and (logico = {true}) \n" +
                            ")";
        assertEquals(expResult, result);
        System.out.println(result);
    }        
    
    @Test
    public void test4() {
        System.out.println("DataExpression-test4");
        DataExpression sentence = new DataExpression();
        sentence.addExpression("pedro = {var1}",null, "", "1");
        sentence.addExpression("juan between '{var1}' and '{var2}'", "var1","pp2","var2","pp2");
        String expResult = "(pedro = {var1}) \n" +
                            "and (juan between 'pp2' and 'pp2')";
        String result = sentence.getSentence().trim();
        assertEquals(expResult, result);
        
        System.out.println("==============");
        System.out.println(result);
        System.out.println("==============");        
        result = sentence.getSentence("1").trim();
        expResult = "(pedro = {var1})";
        assertEquals(expResult, result);
        
        System.out.println(result);
        System.out.println("==============");
    }    

    @Test
    public void test5() {
        System.out.println("DataExpression-test5");                        
        DataExpression sentence = new DataExpression();
        //sentence.openParenthesis();
        sentence.addExpression("pedro1 = {var1}",null, "", "1");
        sentence.addExpression("pedro1b = {var1}",null, "", "1");
        sentence.addExpression("juan1 between '{var1}' and '{var2}'", "var1","pp2","var2","pp2");
        //sentence.closeParenthesis();

        DataExpression sentence2 = new DataExpression();
        //sentence2.openParenthesis();        
        sentence2.addExpression("pedro2 = {var1}",null, "", "1");
        sentence2.addExpression("pedro2b = {var1}",null, "", "1");        
        sentence2.addExpression("juan2 between '{var1}' and '{var2}'", "var1","pp2","var2","pp2");
        //sentence2.closeParenthesis();        
        
        sentence.addExpressions(sentence2);
        String result = sentence.getSentence().trim();
        String expResult = "(pedro1 = {var1}) \n" +
                            "and (pedro1b = {var1}) \n" +
                            "and (juan1 between 'pp2' and 'pp2') \n" +
                            "and (pedro2 = {var1}) \n" +
                            "and (pedro2b = {var1}) \n" +
                            "and (juan2 between 'pp2' and 'pp2')";
        assertEquals(expResult, result);
        
        System.out.println("==============");
        System.out.println(result);
        System.out.println("==============");        
        result = sentence.getSentence("1").trim();
        expResult = "(pedro1 = {var1}) \n" +
                    "and (pedro1b = {var1}) \n" +
                    "and (pedro2 = {var1}) \n" +
                    "and (pedro2b = {var1})";
        assertEquals(expResult, result);
        System.out.println(result);
        System.out.println("==============");
    }    
}
