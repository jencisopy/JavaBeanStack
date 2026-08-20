/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2018 - 2027 Jorge Enciso
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
package org.javabeanstack.poi.excel;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Workbook;
import org.javabeanstack.data.IDataQueryModel;
import org.javabeanstack.data.model.DataQueryModel;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del servicio de exportación.
 *
 * <p>Sin contenedor no hay {@code IAppConfig} inyectado, así que lo que se mide
 * acá es el comportamiento <b>sin configuración</b> —que tiene que ser el valor
 * por omisión y no una exportación caída— y el control del tope, que se fuerza
 * sobreescribiendo {@code getMaxRows}. Que el parámetro se lea de verdad se
 * mide contra la aplicación desplegada; eso no es un unitario.</p>
 *
 * @author jenciso
 */
public class ExcelExportSrvTest {

    /** Servicio con un tope fijo, para no tener que fabricar 10.001 filas. */
    private ExcelExportSrv conTope(final int maxRows) {
        return new ExcelExportSrv() {
            @Override
            public int getMaxRows(int sheetCount) {
                return maxRows;
            }
        };
    }

    private List<IDataQueryModel> filas(int cantidad) {
        List<IDataQueryModel> rows = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            DataQueryModel model = new DataQueryModel();
            model.setColumnList(new String[]{"codigo", "nombre"});
            model.setRow(new Object[]{"COD" + i, "Fila " + i});
            rows.add(model);
        }
        return rows;
    }

    @Test
    public void testTopePorOmisionSegunCantidadDeHojas() {
        ExcelExportSrv srv = new ExcelExportSrv();
        //Sin configuración a mano: los valores por omisión, y el de varias hojas
        //MENOR que el de una sola (una planilla de tres solapas pesa por la
        //suma, no por la hoja más grande).
        assertEquals(IExcelExportSrv.DEFAULT_MAX_ROWS, srv.getMaxRows(1));
        assertEquals(IExcelExportSrv.DEFAULT_MAX_ROWS, srv.getMaxRows(0));
        assertEquals(IExcelExportSrv.DEFAULT_MAX_ROWS_PER_SHEET, srv.getMaxRows(2));
        assertEquals(IExcelExportSrv.DEFAULT_MAX_ROWS_PER_SHEET, srv.getMaxRows(5));
        assertTrue(IExcelExportSrv.DEFAULT_MAX_ROWS_PER_SHEET < IExcelExportSrv.DEFAULT_MAX_ROWS);
    }

    @Test
    public void testHojaQueSePasaSeRechazaYSeNombra() throws Exception {
        ExcelExportSrv srv = conTope(3);
        List<ExcelSheetData> hojas = List.of(
                new ExcelSheetData("Cabecera", filas(3)),
                new ExcelSheetData("Detalle", filas(4)));
        ExcelExportLimitException exp = assertThrows(ExcelExportLimitException.class,
                () -> srv.toBytes(hojas));
        //El rechazo dice CUÁL hoja: con un mensaje genérico sería imposible
        //saber qué acotar.
        assertEquals("Detalle", exp.getSheetName());
        assertEquals(4, exp.getRows());
        assertEquals(3, exp.getMaxRows());
    }

    @Test
    public void testHojaQueEntraJustoNoSeRechaza() throws Exception {
        //El límite es inclusivo: quien exporta pide una fila MÁS que el tope
        //para distinguir «entró justo» de «se cortó», y esa de más es la que
        //dispara el rechazo. Si acá se rechazara, el tope real sería N-1.
        ExcelExportSrv srv = conTope(3);
        byte[] libro = srv.toBytes(List.of(new ExcelSheetData("Cabecera", filas(3))));
        assertNotNull(libro);
        assertEquals('P', (char) libro[0]);
        assertEquals('K', (char) libro[1]);
    }

    @Test
    public void testLibroSinHojasNoRompe() throws Exception {
        ExcelExportSrv srv = conTope(3);
        srv.checkMaxRows(null);
        srv.checkMaxRows(new ArrayList<>());
    }

    @Test
    public void testHojaSinFilasNoSePasa() throws Exception {
        //Un período sin movimiento se exporta igual, con sus encabezados: una
        //hoja vacía es una respuesta, no un error.
        ExcelExportSrv srv = conTope(1);
        List<ExcelSheetData> hojas = List.of(
                new ExcelSheetData("Vacia", new String[]{"codigo", "nombre"}, new ArrayList<>()));
        try (Workbook libro = srv.toWorkbook(hojas)) {
            assertNotNull(libro);
            assertEquals("Vacia", libro.getSheetAt(0).getSheetName());
            //La fila de encabezados sigue estando.
            assertEquals(0, libro.getSheetAt(0).getLastRowNum());
        }
    }
}
