/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
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
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.javabeanstack.data.IDataQueryModel;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pruebas unitarias offline de JasperReportUtil: conversión de los datos a la
 * estructura que consume el reporte y resolución del path del archivo
 * .jasper. La generación/exportación de reportes requiere recursos jrxml
 * compilados y FacesContext, se valida en el ciclo funcional.
 *
 * @author Jorge Enciso
 */
public class JasperReportUtilTest {

    public JasperReportUtilTest() {
    }

    /**
     * Crea una fila de datos de prueba sobre la interfaz IDataQueryModel.
     */
    private IDataQueryModel row(String[] columns, Object[] values) {
        return (IDataQueryModel) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{IDataQueryModel.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getColumnList":
                            return columns;
                        case "getColumnName":
                            return columns[(Integer) args[0]];
                        case "getColumn":
                            if (args[0] instanceof Integer) {
                                return values[(Integer) args[0]];
                            }
                            return null;
                        default:
                            return null;
                    }
                });
    }

    /**
     * Test of convertTo method, of class JasperReportUtil: cada fila se
     * convierte en un map columna → valor.
     */
    @Test
    public void testConvertTo() {
        System.out.println("jasperReportUtil convertTo");
        JasperReportUtil instance = new JasperReportUtil(null);
        String[] columns = {"codigo", "nombre"};
        List<IDataQueryModel> data = Arrays.asList(
                row(columns, new Object[]{1L, "Producto A"}),
                row(columns, new Object[]{2L, "Producto B"}));

        Map[] result = instance.convertTo(data);
        assertEquals(2, result.length);
        assertEquals(1L, result[0].get("codigo"));
        assertEquals("Producto A", result[0].get("nombre"));
        assertEquals(2L, result[1].get("codigo"));
        assertEquals("Producto B", result[1].get("nombre"));
    }

    /**
     * Test of getFullPathReport method, of class JasperReportUtil: nombre nulo
     * devuelve vacío; sin fileSystemPath ni recurso en la base devuelve solo
     * el nombre del archivo normalizado (minúsculas, extensión .jasper).
     */
    @Test
    public void testGetFullPathReportNormalizacion() {
        System.out.println("jasperReportUtil getFullPathReportNormalizacion");
        JasperReportUtil instance = new JasperReportUtil(null);
        assertEquals("", instance.getFullPathReport(null));
        assertEquals("informe.jasper", instance.getFullPathReport("Informe.JRXML"));
        assertEquals("informe.jasper", instance.getFullPathReport("informe"));
        assertEquals("informe.jasper", instance.getFullPathReport("/carpeta/Informe.jasper"));
    }

    /**
     * Test of getFullPathReport method, of class JasperReportUtil: si el
     * archivo existe en alguna carpeta reports del fileSystemPath devuelve el
     * path completo.
     */
    @Test
    public void testGetFullPathReportFileSystem(@TempDir Path tempDir) throws Exception {
        System.out.println("jasperReportUtil getFullPathReportFileSystem");
        Path reports = Files.createDirectories(tempDir.resolve("reports"));
        Files.createFile(reports.resolve("miinforme.jasper"));

        JasperReportUtil instance = new JasperReportUtil(null);
        //Path inexistente primero para probar la precedencia de carpetas
        instance.setFileSystemPath(tempDir.resolve("noexiste").toString()
                + "," + tempDir.toString());

        String expected = reports.toString() + File.separator + "miinforme.jasper";
        assertEquals(expected, instance.getFullPathReport("MiInforme.jasper"));
    }
}
