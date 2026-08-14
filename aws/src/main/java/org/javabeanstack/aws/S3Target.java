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
package org.javabeanstack.aws;

import software.amazon.awssdk.services.s3.S3Client;

import org.javabeanstack.error.ErrorReg;
import org.javabeanstack.error.IErrorReg;
import org.javabeanstack.outputs.IDocumentTarget;
import org.javabeanstack.outputs.IOutputDocument;
import org.javabeanstack.util.Strings;

/**
 * Destino del subsistema de salida ({@code org.javabeanstack.outputs}) que
 * sube el documento a un <b>bucket S3</b>, directo desde la memoria (sin pasar
 * por el sistema de archivos). La clave del objeto es el nombre del documento,
 * opcionalmente con un prefijo de carpeta; si el objeto ya existe, se
 * sobreescribe.
 *
 * <p>Uso: {@code new S3Target(s3Client, "mi-bucket").prefix("documentos/")}.
 * El {@link S3Client} lo construye la aplicación (por ejemplo con
 * {@link S3Util#newS3Client}) y es suyo: este destino no lo cierra.</p>
 *
 * @author Jorge Enciso
 */
public class S3Target implements IDocumentTarget {

    /** Nombre del canal para mensajes y log. */
    public static final String CHANNEL = "s3";

    private final S3Client s3Client;
    private final String bucketName;
    private String prefix = "";

    /**
     * Crea el destino sobre el cliente y el bucket indicados.
     *
     * @param s3Client cliente S3 ya construido por la aplicación.
     * @param bucketName nombre del bucket destino.
     */
    public S3Target(S3Client s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    /**
     * Asigna un prefijo de carpeta para la clave del objeto (ej.
     * {@code "documentos/2026/"}). Opcional.
     *
     * @param prefix prefijo de la clave.
     * @return esta instancia, para encadenar.
     */
    public S3Target prefix(String prefix) {
        this.prefix = prefix == null ? "" : prefix;
        return this;
    }

    /**
     * Sube el documento al bucket con la clave {@code <prefijo><fileName>}.
     *
     * @param document documento generado.
     * @return resultado sin error si la subida fue aceptada; con el mensaje
     * del fallo si no.
     * @throws Exception si el documento es nulo o vacío, o el destino no fue
     * configurado (uso inválido).
     */
    @Override
    public IErrorReg deliver(IOutputDocument document) throws Exception {
        if (document == null || document.getSize() == 0) {
            throw new Exception("El documento a subir es nulo o está vacío");
        }
        if (s3Client == null || Strings.isNullorEmpty(bucketName)) {
            throw new Exception("No se configuró el cliente o el bucket S3 destino");
        }
        if (Strings.isNullorEmpty(document.getFileName())) {
            throw new Exception("El documento no tiene nombre de archivo");
        }
        IErrorReg result = new ErrorReg();
        try {
            S3Util.putObject(s3Client, bucketName, prefix + document.getFileName(),
                    document.getContent(), document.getContentType());
        } catch (Exception ex) {
            //Fallo del canal: vuelve como resultado, no como excepción.
            result.setErrorNumber(1);
            result.setMessage("No se pudo subir " + prefix + document.getFileName()
                    + " al bucket " + bucketName + ": " + ex.getMessage());
            result.setException(ex);
        }
        return result;
    }

    @Override
    public String getChannelName() {
        return CHANNEL;
    }
}
