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
package org.javabeanstack.sifen.internal.request;

import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.beans.EventosDE;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.internal.util.ResponseUtil;

import jakarta.xml.soap.SOAPMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Arma sin red los mensajes SOAP de recepción de eventos y de lotes, con las mismas clases que usa
 * la fachada (plan JBSSIF, §2.2/§2.3).
 *
 * <p>Está en el paquete de {@code ReqRecEventoDe}/{@code ReqRecLoteDe} porque
 * {@code setupSoapMessage} es de paquete. Solo usa API que existe igual en el fork y en
 * {@code jbs-sifen}: la compilan los dos lados del arnés A/B.</p>
 */
public final class MensajesSifen {
    private static final Pattern XDE = Pattern.compile("<(?:[A-Za-z0-9_]+:)?xDE>([^<]*)</(?:[A-Za-z0-9_]+:)?xDE>");

    private MensajesSifen() {
    }

    /**
     * Mensaje {@code rEnviEventoDe} (firmado) tal como lo enviaría {@code Sifen.recepcionEvento}.
     *
     * @param eventos eventos a firmar.
     * @param config configuración (certificado, ambiente).
     * @return sobre SOAP serializado como lo hace {@code generarXml}.
     * @throws SifenException si falla el armado o la firma.
     */
    public static String mensajeEvento(EventosDE eventos, SifenConfig config) throws SifenException {
        ReqRecEventoDe req = new ReqRecEventoDe(1L, config);
        req.setEventoDE(eventos);
        SOAPMessage mensaje = req.setupSoapMessage(GenerationCtx.getDefaultFromConfig(config));
        return ResponseUtil.getXmlFromMessage(mensaje, true);
    }

    /**
     * Mensaje {@code rEnvioLote} tal como lo enviaría {@code Sifen.recepcionLoteDE}. El contenido
     * {@code xDE} es un zip en base64 con nombre de entrada y fecha variables: para comparar usar
     * {@link #contenidoLote(String)}.
     *
     * @param documentos DE del lote.
     * @param ctx contexto de generación.
     * @param config configuración.
     * @return sobre SOAP serializado.
     * @throws SifenException si falla el armado o la firma.
     */
    public static String mensajeLote(List<DocumentoElectronico> documentos, GenerationCtx ctx, SifenConfig config) throws SifenException {
        ReqRecLoteDe req = new ReqRecLoteDe(1L, config);
        req.setDEList(documentos);
        SOAPMessage mensaje = req.setupSoapMessage(ctx);
        return ResponseUtil.getXmlFromMessage(mensaje, true);
    }

    /**
     * Extrae el XML del {@code rLoteDE} del zip en base64 del mensaje de lote (la única entrada del
     * zip; su nombre y su fecha se descartan porque cambian en cada corrida).
     *
     * @param mensajeLote salida de {@link #mensajeLote}.
     * @return XML del {@code rLoteDE}.
     * @throws IOException si el zip no se puede leer.
     */
    public static String contenidoLote(String mensajeLote) throws IOException {
        Matcher m = XDE.matcher(mensajeLote);
        if (!m.find()) {
            throw new IOException("El mensaje de lote no tiene xDE");
        }
        byte[] zip = Base64.getDecoder().decode(m.group(1).trim());
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entrada = zin.getNextEntry();
            if (entrada == null) {
                throw new IOException("El zip del lote está vacío");
            }
            return new String(zin.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
