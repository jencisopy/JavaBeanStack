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
package org.javabeanstack.web.util;

import java.io.File;
import java.util.List;
import org.apache.poi.ss.usermodel.Workbook;
import org.javabeanstack.data.IDataQueryModel;
import static org.javabeanstack.web.util.ExcelUtil.*;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 *
 * @author Jorge Enciso
 */
public class ExcelUtilTest {
    @Test
    public void testDownLoadFile() throws Exception {
    }

    @Test
    public void testOpenWorkbook_String() throws Exception {
        
    }

    @Test
    public void testOpenWorkbook_File() throws Exception {
        String path = "./src/test/java/org/javabeanstack/web/util/prueba.xlsx";
        File file = new File(path);
        Workbook wb = openWorkbook(file);
        assertNotNull(wb);
    }

    @Test
    public void testToExcel() throws Exception {
    }

    @Test
    public void testFromExcelToDataQueryModel() throws Exception {
        String path = "./src/test/java/org/javabeanstack/web/util/prueba.xlsx";
        File file = new File(path);
        Workbook wb = ExcelUtil.openWorkbook(file);
        List<IDataQueryModel> result = fromExcelToDataQueryModel(wb.getSheetAt(0));
        assertTrue(!result.isEmpty());
        //Sin cabecera
        path = "./src/test/java/org/javabeanstack/web/util/prueba2.xlsx";
        file = new File(path);
        wb = ExcelUtil.openWorkbook(file);
        try {
            fromExcelToDataQueryModel(wb.getSheetAt(0));
        } catch (Exception e) {
            assertTrue(e.getMessage().equals("No esta  especificado los nombres de las columnas en la primera fila"));
        }
    }

    @Test
    public void testGetColumnNames() {
    }

}
