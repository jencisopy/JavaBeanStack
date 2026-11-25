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

import java.io.IOException;

import jakarta.faces.context.FacesContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;

import org.javabeanstack.error.ErrorInfo;
import org.javabeanstack.error.IErrorInfo;
import org.javabeanstack.outputs.IDocumentTarget;
import org.javabeanstack.outputs.IOutputDocument;

/**
 * Destino del subsistema de salida ({@code org.javabeanstack.outputs}) que
 * entrega el documento como <b>descarga por la respuesta HTTP</b> del ciclo
 * JSF en curso. Es el <b>único</b> destino autorizado a tocar la respuesta.
 *
 * <p>
 * Como el documento llega ya generado y completo, las cabeceras
 * ({@code Content-Type}, {@code Content-Disposition}, {@code Content-Length})
 * se escriben con la certeza de que hay contenido que entregar: el modo de
 * falla histórico —comprometer la respuesta antes de generar y terminar
 * descargando la pantalla de error como si fuera el archivo, o reventar con
 * {@code UT010006}— queda estructuralmente eliminado.
 * </p>
 *
 * <p>
 * El contenido se entrega con {@link IOutputDocument#writeTo(java.io.OutputStream)}:
 * un documento respaldado por un archivo
 * ({@code org.javabeanstack.outputs.FileOutputDocument}) se copia a la
 * respuesta por bloques, y uno en memoria escribe sus bytes como antes.
 * </p>
 *
 * <p>Uso: {@code new DownloadTarget()} (descarga como adjunto) o
 * {@code new DownloadTarget(true)} para mostrarlo inline (ej. un PDF en el
 * navegador).</p>
 *
 * @author Jorge Enciso
 */
public class DownloadTarget implements IDocumentTarget {

    /** Nombre del canal para mensajes y log. */
    public static final String CHANNEL = "download";

    private final boolean inline;

    /**
     * Crea el destino como descarga adjunta (attachment).
     */
    public DownloadTarget() {
        this(false);
    }

    /**
     * Crea el destino indicando la disposición del contenido.
     *
     * @param inline verdadero para entregarlo inline (el navegador lo muestra,
     * típico para PDF); falso para descarga adjunta.
     */
    public DownloadTarget(boolean inline) {
        this.inline = inline;
    }

    /**
     * Escribe el documento en la respuesta HTTP del ciclo JSF en curso y marca
     * la respuesta como completa.
     *
     * <p>Si la copia falla a mitad, no lanza: devuelve el error (número 1, con
     * la excepción), como los demás destinos ante una falla de entrega. Si ya
     * salieron bytes la respuesta queda marcada como completa (está
     * comprometida y JSF no puede dibujar encima); si no, se limpia, para que
     * la pantalla pueda mostrar el error.</p>
     *
     * @param document documento generado.
     * @return resultado sin error si se escribió; con el error si la copia
     * falló.
     * @throws Exception si el documento es nulo o vacío, o no hay contexto
     * Faces activo (uso inválido).
     */
    @Override
    public IErrorInfo deliver(IOutputDocument document) throws Exception {
        if (document == null || document.getSize() == 0) {
            throw new Exception("El documento a descargar es nulo o está vacío");
        }
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            throw new Exception("No hay un contexto Faces activo para la descarga");
        }
        HttpServletResponse response =
                (HttpServletResponse) facesContext.getExternalContext().getResponse();
        response.setContentType(document.getContentType());
        response.setHeader("Content-Disposition", (inline ? "inline" : "attachment")
                + "; filename=" + document.getFileName());
        response.setContentLengthLong(document.getSize());
        //writeTo y no getContent(): un documento respaldado por un archivo se
        //copia por bloques, sin cargarlo entero en memoria; uno en memoria
        //escribe sus bytes, como siempre.
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            document.writeTo(outputStream);
            outputStream.flush();
        } catch (IOException ex) {
            //La copia fallo a mitad (el cliente corto la descarga, o el archivo
            //que respalda el documento desaparecio). Si ya salieron bytes, la
            //respuesta esta comprometida: se la marca completa para que JSF no
            //intente dibujar encima. Si no salio nada, se la limpia y JSF puede
            //mostrar el error en la pantalla.
            if (response.isCommitted()) {
                facesContext.responseComplete();
            } else {
                response.reset();
            }
            IErrorInfo result = new ErrorInfo();
            result.setErrorNumber(1);
            result.setMessage("No se pudo entregar el documento " + document.getFileName()
                    + ": " + ex.getMessage());
            result.setException(ex);
            return result;
        }
        facesContext.responseComplete();
        return new ErrorInfo();
    }

    @Override
    public String getChannelName() {
        return CHANNEL;
    }
}
