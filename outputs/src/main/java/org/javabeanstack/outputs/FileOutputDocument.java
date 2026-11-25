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

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implementación de {@link IOutputDocument} <b>respaldada por un archivo</b>:
 * el contenido no está en memoria sino en disco, típicamente en un temporal
 * del servidor.
 *
 * <p><b>Por qué existe.</b> {@link OutputDocument} guarda el documento como
 * {@code byte[]}; para una planilla de cientos de miles de filas eso es tener
 * el archivo entero en el heap mientras se descarga. Con esta clase quien
 * genera escribe directo al archivo y el destino lo lee por bloques con
 * {@link #writeTo(OutputStream)}. La regla del subsistema —primero generar,
 * después comprometer el destino— se mantiene: el archivo está completo antes
 * de entregarlo.</p>
 *
 * <p><b>Compatibilidad.</b> Los destinos que necesitan los bytes (el adjunto
 * de un correo, la carpeta del servidor) siguen funcionando:
 * {@link #getContent()} lee el archivo completo. Es correcto, pero vuelve a
 * cargarlo en memoria; para un documento grande conviene un destino que use
 * {@link #writeTo(OutputStream)}.</p>
 *
 * <p><b>Ciclo de vida.</b> Esta clase no borra el archivo por su cuenta: lo
 * hace {@link #delete()}, que el dueño del documento llama cuando ya no lo
 * necesita (después de la descarga, al cancelar o al descartar la
 * vista).</p>
 *
 * <p>Uso típico:</p>
 * <pre>
 * Path archivo = Files.createTempFile(carpeta, "exportacion", ".xlsx");
 * // ... escribir el archivo ...
 * IOutputDocument documento = new FileOutputDocument(archivo,
 *         "clientes_20261124.xlsx", IOutputDocument.FORMAT_XLSX);
 * </pre>
 *
 * @author Jorge Enciso
 */
public class FileOutputDocument implements IOutputDocument {

    private static final Logger LOGGER = LogManager.getLogger(FileOutputDocument.class);

    private final Path file;
    private String fileName;
    private String contentType;
    private String format;

    /**
     * Crea el documento sobre un archivo existente.
     *
     * @param file archivo con el contenido del documento.
     * @param fileName nombre con que el documento llega al destino, con su
     * extensión (no tiene por qué coincidir con el del archivo).
     * @param format formato, uno de los valores {@code FORMAT_*} de
     * {@link IOutputDocument}; el content-type se deriva con
     * {@link OutputDocument#contentTypeFor(String)}.
     * @throws IllegalArgumentException si {@code file} es nulo.
     */
    public FileOutputDocument(Path file, String fileName, String format) {
        if (file == null) {
            throw new IllegalArgumentException("El archivo del documento es nulo");
        }
        this.file = file;
        this.fileName = fileName;
        this.format = format;
        this.contentType = OutputDocument.contentTypeFor(format);
    }

    /**
     * Devuelve el archivo que respalda el documento.
     *
     * @return ruta del archivo.
     */
    public Path getFile() {
        return file;
    }

    /**
     * Devuelve el nombre con que el documento llega al destino, con su
     * extensión. No tiene por qué coincidir con el del archivo que lo respalda.
     *
     * @return nombre del documento (ej. {@code clientes_20261124.xlsx}).
     */
    @Override
    public String getFileName() {
        return fileName;
    }

    /**
     * Asigna el nombre con que el documento llega al destino. No renombra el
     * archivo que lo respalda.
     *
     * @param fileName nombre del documento, con su extensión.
     */
    @Override
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Devuelve el contenido completo, <b>leyendo el archivo entero a
     * memoria</b>. Existe por compatibilidad con los destinos que necesitan
     * los bytes; para entregar el documento por un flujo usar
     * {@link #writeTo(OutputStream)}.
     *
     * @return bytes del archivo.
     * @throws UncheckedIOException si el archivo no existe o no se puede leer.
     */
    @Override
    public byte[] getContent() {
        try {
            return Files.readAllBytes(file);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo leer el documento " + file, ex);
        }
    }

    /**
     * Reemplaza el contenido del archivo por los bytes indicados (lo crea si
     * no existe). Un contenido nulo deja el archivo vacío.
     *
     * @param content bytes del documento.
     * @throws UncheckedIOException si no se puede escribir el archivo.
     */
    @Override
    public void setContent(byte[] content) {
        try {
            Files.write(file, content == null ? new byte[0] : content);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo escribir el documento " + file, ex);
        }
    }

    /**
     * Devuelve el tipo MIME con que el documento viaja; al crearlo se deriva
     * del formato con {@link OutputDocument#contentTypeFor(String)}.
     *
     * @return tipo MIME.
     */
    @Override
    public String getContentType() {
        return contentType;
    }

    /**
     * Asigna el tipo MIME del documento (reemplaza el derivado del formato).
     *
     * @param contentType tipo MIME.
     */
    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Devuelve el formato del documento, uno de los valores {@code FORMAT_*} de
     * {@link IOutputDocument}.
     *
     * @return formato del documento.
     */
    @Override
    public String getFormat() {
        return format;
    }

    /**
     * Asigna el formato del documento. No vuelve a derivar el tipo MIME: para
     * cambiarlo, {@link #setContentType(String)}.
     *
     * @param format formato, uno de los valores {@code FORMAT_*} de
     * {@link IOutputDocument}.
     */
    @Override
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * Devuelve el tamaño del archivo en bytes.
     *
     * @return tamaño del archivo, o 0 si no existe o no se puede leer (los
     * destinos rechazan un documento de tamaño 0).
     */
    @Override
    public long getSize() {
        try {
            return Files.isRegularFile(file) ? Files.size(file) : 0L;
        } catch (IOException ex) {
            LOGGER.warn("No se pudo leer el tamaño de {}: {}", file, ex.getMessage());
            return 0L;
        }
    }

    /**
     * Copia el archivo al flujo, por bloques, sin cargarlo entero en memoria.
     *
     * @param output flujo destino; no se cierra ni se vacía.
     * @throws IOException si el archivo no existe o la copia falla.
     */
    @Override
    public void writeTo(OutputStream output) throws IOException {
        Files.copy(file, output);
    }

    /**
     * Borra el archivo que respalda el documento. Se puede llamar más de una
     * vez.
     *
     * @return {@code true} si el archivo se borró; {@code false} si ya no
     * existía o no se pudo borrar (el motivo queda en el log).
     */
    public boolean delete() {
        try {
            return Files.deleteIfExists(file);
        } catch (IOException ex) {
            LOGGER.warn("No se pudo borrar el documento {}: {}", file, ex.getMessage());
            return false;
        }
    }

    @Override
    public String toString() {
        return "FileOutputDocument{fileName=" + fileName + ", format=" + format
                + ", file=" + file + ", size=" + getSize() + "}";
    }
}
