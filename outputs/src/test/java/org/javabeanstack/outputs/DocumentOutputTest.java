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
package org.javabeanstack.outputs;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.javabeanstack.error.IErrorReg;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests unitarios del orquestador {@link DocumentOutput} y del destino
 * {@link FolderTarget}: son las únicas piezas del subsistema sin dependencias
 * externas (los demás adapters necesitan servidor SMTP, contexto Faces o
 * reportes compilados).
 *
 * @author Jorge Enciso
 */
public class DocumentOutputTest {

    private static final byte[] CONTENIDO = "contenido de prueba".getBytes();

    /** Fuente de prueba que produce un documento fijo. */
    private static class FuenteOk implements IDocumentSource {

        @Override
        public IOutputDocument generate() {
            return new OutputDocument("prueba.pdf", CONTENIDO, IOutputDocument.FORMAT_PDF);
        }
    }

    /** Fuente de prueba que falla al generar. */
    private static class FuenteRota implements IDocumentSource {

        @Override
        public IOutputDocument generate() throws Exception {
            throw new Exception("No encontro la plantilla de prueba");
        }
    }

    /**
     * Camino feliz: genera una vez y entrega a dos carpetas; ambos resultados
     * sin error y los dos archivos escritos con el contenido exacto.
     */
    @Test
    public void testExecuteEntregaMultiple(@TempDir File carpeta1, @TempDir File carpeta2) throws Exception {
        List<IErrorReg> results = new DocumentOutput()
                .source(new FuenteOk())
                .to(new FolderTarget(carpeta1.getAbsolutePath()))
                .to(new FolderTarget(carpeta2.getAbsolutePath()))
                .execute();

        assertEquals(2, results.size());
        for (IErrorReg result : results) {
            assertEquals(0, result.getErrorNumber());
        }
        assertArrayEquals(CONTENIDO, Files.readAllBytes(new File(carpeta1, "prueba.pdf").toPath()));
        assertArrayEquals(CONTENIDO, Files.readAllBytes(new File(carpeta2, "prueba.pdf").toPath()));
    }

    /**
     * fileName() pisa el nombre que trae la fuente.
     */
    @Test
    public void testFileNamePisaElDeLaFuente(@TempDir File carpeta) throws Exception {
        new DocumentOutput()
                .source(new FuenteOk())
                .fileName("otro_nombre.pdf")
                .to(new FolderTarget(carpeta.getAbsolutePath()))
                .execute();

        assertTrue(new File(carpeta, "otro_nombre.pdf").exists());
        assertFalse(new File(carpeta, "prueba.pdf").exists());
    }

    /**
     * Si la generación falla: un único resultado con el error, ningún destino
     * ejecutado (la carpeta queda vacía) y ninguna excepción propagada.
     */
    @Test
    public void testFalloDeGeneracionNoEjecutaDestinos(@TempDir File carpeta) {
        DocumentOutput salida = new DocumentOutput()
                .source(new FuenteRota())
                .to(new FolderTarget(carpeta.getAbsolutePath()));
        List<IErrorReg> results = salida.execute();

        assertEquals(1, results.size());
        assertNotEquals(0, results.get(0).getErrorNumber());
        assertTrue(results.get(0).getMessage().contains("plantilla de prueba"));
        assertEquals(0, carpeta.list().length);
        assertNull(salida.getDocument());
    }

    /**
     * Sin fuente o sin destinos: error de configuración, sin excepción.
     */
    @Test
    public void testConfiguracionIncompleta() {
        List<IErrorReg> sinFuente = new DocumentOutput()
                .to(new FolderTarget("/tmp")).execute();
        assertEquals(1, sinFuente.size());
        assertNotEquals(0, sinFuente.get(0).getErrorNumber());

        List<IErrorReg> sinDestino = new DocumentOutput()
                .source(new FuenteOk()).execute();
        assertEquals(1, sinDestino.size());
        assertNotEquals(0, sinDestino.get(0).getErrorNumber());
    }

    /**
     * toPrinter() con una fuente que no implementa IPrintableSource devuelve
     * un error claro, sin excepción.
     */
    @Test
    public void testToPrinterConFuenteNoImprimible() {
        IErrorReg result = new DocumentOutput()
                .source(new FuenteOk())
                .toPrinter();
        assertNotEquals(0, result.getErrorNumber());
        assertTrue(result.getMessage().contains("IPrintableSource"));
    }

    /**
     * El fallo de un destino no impide la entrega en los demás.
     */
    @Test
    public void testFalloDeUnDestinoNoDetieneLosDemas(@TempDir File carpeta) throws Exception {
        IDocumentTarget roto = new IDocumentTarget() {
            @Override
            public IErrorReg deliver(IOutputDocument document) throws Exception {
                throw new Exception("canal caído");
            }

            @Override
            public String getChannelName() {
                return "roto";
            }
        };
        List<IErrorReg> results = new DocumentOutput()
                .source(new FuenteOk())
                .to(roto)
                .to(new FolderTarget(carpeta.getAbsolutePath()))
                .execute();

        assertEquals(2, results.size());
        assertNotEquals(0, results.get(0).getErrorNumber());
        assertEquals(0, results.get(1).getErrorNumber());
        assertTrue(new File(carpeta, "prueba.pdf").exists());
    }
}
