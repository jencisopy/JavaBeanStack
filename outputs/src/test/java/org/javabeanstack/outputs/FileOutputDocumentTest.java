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
package org.javabeanstack.outputs;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.javabeanstack.error.IErrorInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias de {@link FileOutputDocument} y del método por omisión
 * {@link IOutputDocument#writeTo(java.io.OutputStream)}: tamaño, contenido,
 * copia al flujo, borrado y entrega a {@link FolderTarget} y por
 * {@link OutputDispatcher}.
 *
 * @author Jorge Enciso
 */
public class FileOutputDocumentTest {

    private static final byte[] CONTENIDO = "contenido de un documento en disco".getBytes();

    @TempDir
    Path carpeta;

    private Path archivo() throws IOException {
        Path file = carpeta.resolve("documento.tmp");
        Files.write(file, CONTENIDO);
        return file;
    }

    @Test
    public void testPropertiesAndSize() throws Exception {
        FileOutputDocument doc = new FileOutputDocument(archivo(), "planilla.xlsx",
                IOutputDocument.FORMAT_XLSX);
        assertEquals("planilla.xlsx", doc.getFileName());
        assertEquals(IOutputDocument.FORMAT_XLSX, doc.getFormat());
        assertEquals(OutputDocument.contentTypeFor(IOutputDocument.FORMAT_XLSX), doc.getContentType());
        assertEquals(CONTENIDO.length, doc.getSize());
        assertArrayEquals(CONTENIDO, doc.getContent());
    }

    @Test
    public void testWriteToCopiesTheFile() throws Exception {
        FileOutputDocument doc = new FileOutputDocument(archivo(), "a.pdf", IOutputDocument.FORMAT_PDF);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.writeTo(out);
        assertArrayEquals(CONTENIDO, out.toByteArray());
    }

    /** Un archivo varias veces más grande que el bloque de copia sale entero y en orden. */
    @Test
    public void testWriteToLargerThanCopyBuffer() throws Exception {
        byte[] grande = new byte[3 * 1024 * 1024 + 17];
        new java.util.Random(42).nextBytes(grande);
        Path file = carpeta.resolve("grande.tmp");
        Files.write(file, grande);
        FileOutputDocument doc = new FileOutputDocument(file, "grande.xlsx", IOutputDocument.FORMAT_XLSX);
        assertEquals(grande.length, doc.getSize());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.writeTo(out);
        assertArrayEquals(grande, out.toByteArray());
    }

    @Test
    public void testDefaultWriteToOfInMemoryDocument() throws Exception {
        OutputDocument doc = new OutputDocument("a.pdf", CONTENIDO, IOutputDocument.FORMAT_PDF);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.writeTo(out);
        assertArrayEquals(CONTENIDO, out.toByteArray());
        // Sin contenido no escribe nada (y no falla).
        ByteArrayOutputStream vacio = new ByteArrayOutputStream();
        new OutputDocument().writeTo(vacio);
        assertEquals(0, vacio.size());
    }

    @Test
    public void testSetContentRewritesTheFile() throws Exception {
        Path file = archivo();
        FileOutputDocument doc = new FileOutputDocument(file, "a.pdf", IOutputDocument.FORMAT_PDF);
        doc.setContent("otro".getBytes());
        assertArrayEquals("otro".getBytes(), Files.readAllBytes(file));
        assertEquals(4, doc.getSize());
    }

    @Test
    public void testDelete() throws Exception {
        Path file = archivo();
        FileOutputDocument doc = new FileOutputDocument(file, "a.pdf", IOutputDocument.FORMAT_PDF);
        assertTrue(doc.delete());
        assertFalse(Files.exists(file));
        // Segunda vez: no hay nada que borrar, y no falla.
        assertFalse(doc.delete());
        assertEquals(0, doc.getSize());
        assertThrows(UncheckedIOException.class, doc::getContent);
        assertThrows(IOException.class, () -> doc.writeTo(new ByteArrayOutputStream()));
    }

    @Test
    public void testNullFileIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new FileOutputDocument(null, "a.pdf", IOutputDocument.FORMAT_PDF));
    }

    /** La carpeta del servidor recibe un documento por archivo igual que uno en memoria. */
    @Test
    public void testFolderTargetAcceptsFileDocument() throws Exception {
        FileOutputDocument doc = new FileOutputDocument(archivo(), "entregado.pdf",
                IOutputDocument.FORMAT_PDF);
        File destino = carpeta.resolve("destino").toFile();
        IErrorInfo result = new FolderTarget(destino.getPath()).deliver(doc);
        assertEquals(0, result.getErrorNumber());
        assertArrayEquals(CONTENIDO, Files.readAllBytes(destino.toPath().resolve("entregado.pdf")));
    }

    /** El orquestador entrega un documento por archivo sin tratarlo distinto. */
    @Test
    public void testDispatcherWithFileDocument() throws Exception {
        Path file = archivo();
        IDocumentSource source = () -> new FileOutputDocument(file, "orquestado.pdf",
                IOutputDocument.FORMAT_PDF);
        File destino = carpeta.resolve("orquestado").toFile();
        List<IErrorInfo> results = new OutputDispatcher()
                .source(source)
                .to(new FolderTarget(destino.getPath()))
                .execute();
        assertEquals(1, results.size());
        assertEquals(0, results.get(0).getErrorNumber());
        assertArrayEquals(CONTENIDO, Files.readAllBytes(destino.toPath().resolve("orquestado.pdf")));
    }
}
