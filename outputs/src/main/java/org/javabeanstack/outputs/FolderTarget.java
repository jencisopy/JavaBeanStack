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

import org.javabeanstack.error.ErrorReg;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.io.IOUtil;
import org.javabeanstack.util.Strings;

/**
 * Destino que guarda el documento en una <b>carpeta del servidor</b>. Escribe
 * {@code <carpeta>/<fileName>} con {@link IOUtil#writeBytesToFile}, creando los
 * directorios intermedios si no existen; si el archivo ya existe, lo
 * sobreescribe.
 *
 * <p>Uso: {@code new FolderTarget("/datos/documentos/")}.</p>
 *
 * @author Jorge Enciso
 */
public class FolderTarget implements IDocumentTarget {

    /** Nombre del canal para mensajes y log. */
    public static final String CHANNEL = "folder";

    private final String folder;

    /**
     * Crea el destino sobre la carpeta indicada.
     *
     * @param folder ruta de la carpeta destino en el servidor.
     */
    public FolderTarget(String folder) {
        this.folder = folder;
    }

    /**
     * Escribe el documento en la carpeta configurada.
     *
     * @param document documento generado.
     * @return resultado sin error si el archivo se escribió; con mensaje de
     * error si la escritura falló.
     * @throws Exception si el documento es nulo o vacío, o la carpeta no fue
     * configurada (uso inválido).
     */
    @Override
    public IErrorReg deliver(IOutputDocument document) throws Exception {
        if (document == null || document.getSize() == 0) {
            throw new Exception("El documento a guardar es nulo o está vacío");
        }
        if (Strings.isNullorEmpty(folder)) {
            throw new Exception("No se configuró la carpeta destino");
        }
        if (Strings.isNullorEmpty(document.getFileName())) {
            throw new Exception("El documento no tiene nombre de archivo");
        }
        IErrorReg result = new ErrorReg();
        String filePath = IOUtil.addbs(folder.trim()) + document.getFileName();
        if (!IOUtil.writeBytesToFile(document.getContent(), filePath)) {
            result.setErrorNumber(1);
            result.setMessage("No se pudo escribir el archivo " + filePath);
        }
        return result;
    }

    @Override
    public String getChannelName() {
        return CHANNEL;
    }
}
