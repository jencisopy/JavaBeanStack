package org.javabeanstack.data.model;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.javabeanstack.data.IDataQueryModel;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Jorge Enciso
 */
public class DataQueryModelTest {
    
    public DataQueryModelTest() {
    }
    
    @Test
    public void test() {
        System.out.println("DataQueryModel test1");
        IDataQueryModel data = new DataQueryModel(); 
        Object[] row = new Object[2];
        row[0] = "001";
        row[1] = "PEPE GONZALEZ";
        
        String[] columnList = new String[2];
        columnList[0] = "codigo";
        columnList[1] = "nombre";
        
        data.setRow(row);
        data.setColumnList(columnList);
        
        assertEquals("codigo", data.getColumnName(0));
        assertEquals("nombre", data.getColumnName(1));
        
        assertEquals("001", data.getColumn("codigo"));
        assertEquals("PEPE GONZALEZ", data.getColumn("NOMBRE"));        
        
        assertEquals("001",data.getColumn(0));
        assertEquals("001",data.getColumnId());
        
        data.setColumnId(1);
        assertEquals("PEPE GONZALEZ",data.getColumnId());
    }
    
    @Test
    public void test2() {
        System.out.println("DataQueryModel test2");
        IDataQueryModel data = new DataQueryModel(); 
        Object[] row = new Object[2];
        row[0] = "001";
        row[1] = "PEPE GONZALEZ";
        
        String[] columnList = new String[2];
        columnList[0] = "codigo";
        columnList[1] = "nombre";
        
        data.setRow(row);
        data.setColumnList(columnList);
        
        assertEquals("codigo", data.getColumnName(0));
        assertEquals("nombre", data.getColumnName(1));
        
        assertEquals("001", data.getColumn("codigo"));
        assertEquals("PEPE GONZALEZ", data.getColumn("NOMBRE"));        
        
        assertEquals("001",data.getColumn(0));
        assertEquals("001",data.getColumnId());
        
        data.setColumnId(1);
        assertEquals("PEPE GONZALEZ",data.getColumnId());
        
        data.setColumn(0, "002");
        System.out.println(data.getColumn("codigo"));
        assertEquals("002", data.getColumn("codigo"));        
        data.setColumn("nombre", "JUAN PEREZ");
        assertEquals("JUAN PEREZ", data.getColumn("NOMBRE"));        
        
    }
    
    @Test
    public void test3() {
        System.out.println("DataQueryModel test3");
        IDataQueryModel data = new DataQueryModel(); 
        Object[] row = new Object[3];
        row[0] = "001";
        row[1] = "PEPE GONZALEZ";
        row[2] = BigDecimal.ZERO;
        
        String[] columnList = new String[3];
        columnList[0] = "codigo";
        columnList[1] = "nombre";
        columnList[2] = "total";
        
        data.setRow(row);
        data.setColumnList(columnList);
        
        assertEquals("codigo", data.getColumnName(0));
        assertEquals("nombre", data.getColumnName(1));
        assertEquals("total",  data.getColumnName(2));
        
        assertEquals("001", data.getColumn("codigo"));
        assertEquals("PEPE GONZALEZ", data.getColumn("NOMBRE"));
        
        assertEquals("001",data.getColumn(0));
        assertEquals("001",data.getColumnId());

        assertEquals(BigDecimal.ZERO,data.getColumn(2));
        
        data.setColumnId(1);
        assertEquals("PEPE GONZALEZ",data.getColumnId());
        
        data.setColumn(0, "002");
        System.out.println(data.getColumn("codigo"));
        assertEquals("002", data.getColumn("codigo"));        
        data.setColumn("nombre", "JUAN PEREZ");
        assertEquals("JUAN PEREZ", data.getColumn("NOMBRE"));        
        
    }
    
    @Test
    public void test4() {
        System.out.println("DataQueryModel-test4");
        List<IDataQueryModel> lista = new ArrayList();
        String[] columnList = {"id","nombre","apellido"};        
        IDataQueryModel r1 = new DataQueryModel();
        Object[] row =  {1L,"María", "Rojas"};
        r1.setRow(row);
        r1.setColumnList(columnList);
        lista.add(r1);
        IDataQueryModel r2 = new DataQueryModel();        
        Object[] row2 =  {2L,"Jose", "Perez"};
        r2.setRow(row2);
        r2.setColumnList(columnList);        
        lista.add(r2);        
        
        System.out.println(r1.getColumn(0));
        System.out.println(lista.indexOf(r2));
        assertTrue(lista.indexOf(r2) == 1);
        
        Integer searchId = 2;
        int result = DataQueryModel.searchById(lista, searchId);
        System.out.println(result);
        assertTrue(result == 1);
    }
    
}
