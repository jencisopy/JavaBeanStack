/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
*        jenciso@javabeanstack.org
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de la cabecera {@code Content-Disposition} que arma
 * {@link DownloadTarget#contentDisposition(boolean, String)} (RFC 6266 y
 * RFC 5987): el nombre va entre comillas y escapado, con su forma UTF-8
 * codificada, y sin barras.
 */
public class DownloadTargetContentDispositionTest {

    @Test
    public void testNombreSimple() {
        assertEquals("attachment; filename=\"ctactesub_20261125_0930.xlsx\"; "
                + "filename*=UTF-8''ctactesub_20261125_0930.xlsx",
                DownloadTarget.contentDisposition(false, "ctactesub_20261125_0930.xlsx"));
    }

    @Test
    public void testInline() {
        assertTrue(DownloadTarget.contentDisposition(true, "a.pdf").startsWith("inline; filename=\"a.pdf\""));
    }

    @Test
    public void testEspaciosYBarra() {
        String header = DownloadTarget.contentDisposition(false, "Ventas por Fecha/Nro.xlsx");
        assertEquals("attachment; filename=\"Ventas por Fecha_Nro.xlsx\"; "
                + "filename*=UTF-8''Ventas%20por%20Fecha_Nro.xlsx", header);
    }

    @Test
    public void testComillasPuntoYComaYBarraInvertida() {
        String header = DownloadTarget.contentDisposition(false, "a\"b;c\\d.xlsx");
        assertEquals("attachment; filename=\"a\\\"b;c_d.xlsx\"; filename*=UTF-8''a%22b%3Bc_d.xlsx", header);
    }

    @Test
    public void testNoAscii() {
        String header = DownloadTarget.contentDisposition(false, "Año Ñandú.xlsx");
        assertEquals("attachment; filename=\"A_o _and_.xlsx\"; "
                + "filename*=UTF-8''A%C3%B1o%20%C3%91and%C3%BA.xlsx", header);
    }

    @Test
    public void testControlYSaltoDeLinea() {
        String header = DownloadTarget.contentDisposition(false, "a\r\nSet-Cookie: x.xlsx");
        assertFalse(header.contains("\r") || header.contains("\n"), header);
        assertTrue(header.contains("filename=\"a__Set-Cookie: x.xlsx\""), header);
    }

    @Test
    public void testNuloOVacio() {
        assertEquals("attachment; filename=\"documento\"; filename*=UTF-8''documento",
                DownloadTarget.contentDisposition(false, null));
        assertEquals("attachment; filename=\"documento\"; filename*=UTF-8''documento",
                DownloadTarget.contentDisposition(false, "  "));
    }
}
