package org.javabeanstack.data;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Jorge Enciso
 */
public class DBFilterTest {
    @Test
    public void test1(){
        System.out.println("=======DBFilter-TEST1======");
        IDBFilter dbFilter = new DBFilter();
        IDBFilterElement element = new DBFilterElement();

        // Valor numerico
        element.setFieldName("idempresa");
        element.setFieldValue(1);
        dbFilter.getFilter().add(element);
        System.out.println(dbFilter.getFilterExpr(0));        
        System.out.println(dbFilter.getFilterExpr(0,"a"));                
        assertEquals(dbFilter.getFilterExpr(0),"(idempresa=1)");
        assertEquals(dbFilter.getFilterExpr(0,"a"),"(a.idempresa=1)");        

        // Valor alfanumerico
        element = new DBFilterElement();
        element.setFieldName("iddocumento");
        element.setFieldValue("V");
        dbFilter.getFilter().add(element);
        System.out.println(dbFilter.getFilterExpr(1));
        
        assertEquals(dbFilter.getFilterExpr(1),"(iddocumento='V')");        
        assertEquals(dbFilter.getFilterExpr(1,"a"),"(a.iddocumento='V')");
        
        System.out.println(dbFilter.getAllFilterExpr());
        System.out.println(dbFilter.getAllFilterExpr("a"));
        
    }
    
    @Test
    public void test2(){
        System.out.println("=======DBFilter TEST CON LIST======");                
        IDBFilter dbFilter = new DBFilter();
        IDBFilterElement element = new DBFilterElement();
        
        element.setFieldName("idempresa");
        
        // Una lista de valores numericos
        List values = new ArrayList();
        values.add(1);
        values.add(2);
        
        element.setFieldValue(values);
        dbFilter.getFilter().add(element);
        System.out.println(dbFilter.getFilterExpr(0));        
        System.out.println(dbFilter.getFilterExpr(0,"a"));                
        
        assertEquals(dbFilter.getFilterExpr(0),"(idempresa IN(1,2))");
        assertEquals(dbFilter.getFilterExpr(0,"a"),"(a.idempresa IN(1,2))");        
        
        element = new DBFilterElement();
        element.setFieldName("iddocumento");
        
        // Lista de valores alfanumericos
        values = new ArrayList();
        values.add("V");
        values.add("C");
        
        element.setFieldValue(values);
        dbFilter.getFilter().add(element);
        System.out.println(dbFilter.getFilterExpr(1));        
        System.out.println(dbFilter.getFilterExpr(1,"a"));                
        
        assertEquals(dbFilter.getFilterExpr(1),"(iddocumento IN('V','C'))");
        assertEquals(dbFilter.getFilterExpr(1,"a"),"(a.iddocumento IN('V','C'))");
    }
    
    @Test
    public void test3(){
        System.out.println("=======DBFilter TEST CON GROUP======");        
        IDBFilter dbFilter = new DBFilter();

        // Valor numerico
        dbFilter.addFilter("idempresa", 1, 1);        
        System.out.println(dbFilter.getFilterExpr(0));        
        System.out.println(dbFilter.getFilterExpr(0,"a"));                
        assertEquals(dbFilter.getFilterExpr(0),"(idempresa=1)");
        assertEquals(dbFilter.getFilterExpr(0,"a"),"(a.idempresa=1)");        
        assertEquals(dbFilter.getFilterExpr(0,null,"a"),"(a.idempresa=1)");        
        assertEquals(dbFilter.getFilterExpr(0,1,"a"),"(a.idempresa=1)");                
        assertEquals(dbFilter.getFilterExpr(0,2,"a"),"");        
        // Valor alfanumerico
        dbFilter.addFilter("iddocumento", "V", 2);                
        System.out.println(dbFilter.getFilterExpr(1));        
        
        assertEquals(dbFilter.getFilterExpr(1),"(iddocumento='V')");        
        assertEquals(dbFilter.getFilterExpr(1,"a"),"(a.iddocumento='V')");
        assertEquals(dbFilter.getFilterExpr(1,null,"a"),"(a.iddocumento='V')");        
        assertEquals(dbFilter.getFilterExpr(1,2,"a"),"(a.iddocumento='V')");        
        assertEquals(dbFilter.getFilterExpr(1,1,"a"),"");        
        
        System.out.println(dbFilter.getAllFilterExpr());
        System.out.println(dbFilter.getAllFilterExpr("a"));
        System.out.println("=======GROUP======");
        System.out.println(dbFilter.getAllFilterExpr(1,"a"));
        System.out.println(dbFilter.getAllFilterExpr(2,"a"));        
        System.out.println(dbFilter.getAllFilterExpr(null,"a"));                
    }
}
