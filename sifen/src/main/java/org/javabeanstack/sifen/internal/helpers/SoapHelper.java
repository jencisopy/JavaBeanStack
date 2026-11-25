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
package org.javabeanstack.sifen.internal.helpers;

import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.internal.SOAPResponse;
import org.javabeanstack.sifen.internal.http.SifenConnection;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import java.io.IOException;
import java.io.InputStream;

/**
 * Helper encargado de manejar las peticiones SOAP.
 *
 * <p>Delta del fork 2.0.0-SNAPSHOT (plan JBSSIF, §1.4.5; ficha 14 del informe de línea base):
 * sin cambio de código; el fork solo difiere en cómo su compilador arma la concatenación del
 * mensaje «Conectando a: ».</p>
 *
 * <p>Cambio de jbs-sifen (plan JBSSIF, §3.8, D33 Parte C): {@code makeSoapRequest} mira el protocolo
 * antes del cast (E0-10), tolera el {@code getErrorStream()} nulo (E3-12) y clasifica las fallas con
 * código ({@code JBS-CONEX-*}, {@code JBS-RESP-02}).</p>
 *
 * <p>Fase 5 (plan JBSSIF, §5.2, D37): el envío pasó a
 * {@link org.javabeanstack.sifen.internal.http.SifenConnection}, con contexto TLS, tiempos de espera
 * y proxy por instancia; {@code makeSoapRequest} queda como atajo que usa la conexión de la
 * configuración.</p>
 */
public class SoapHelper {

    public static SOAPMessage createSoapMessage() throws SOAPException {
        MessageFactory mf12 = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
        return mf12.createMessage();
    }

    public static SOAPMessage parseSoapMessage(InputStream is)
            throws SOAPException, IOException {
        MessageFactory mf12 = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
        return mf12.createMessage(null, is);
    }

    /**
     * Envía el mensaje por la conexión de esta instancia de configuración
     * ({@link SifenConnection#of(SifenConfig)}) y devuelve la respuesta; la clasificación de las fallas
     * está en {@link SifenConnection#exchange(SifenConfig, String, SOAPMessage)}.
     *
     * @param sifenConfig configuración.
     * @param urlString   URL del servicio.
     * @param soapMessage mensaje a enviar.
     * @return la respuesta con estado, mensaje SOAP y bytes recibidos.
     * @throws SifenException con el código de la falla.
     */
    public static SOAPResponse makeSoapRequest(SifenConfig sifenConfig, String urlString, SOAPMessage soapMessage) throws SifenException {
        return makeSoapRequest(sifenConfig, urlString, soapMessage, null);
    }

    /**
     * Envía el mensaje por la conexión indicada (§5.2).
     *
     * @param sifenConfig configuración.
     * @param urlString   URL del servicio.
     * @param soapMessage mensaje a enviar.
     * @param connection  conexión; {@code null} = la de la configuración.
     * @return la respuesta con estado, mensaje SOAP y bytes recibidos.
     * @throws SifenException con el código de la falla.
     */
    public static SOAPResponse makeSoapRequest(SifenConfig sifenConfig, String urlString, SOAPMessage soapMessage,
            SifenConnection connection) throws SifenException {
        SifenConnection c = connection != null ? connection : SifenConnection.of(sifenConfig);
        return c.exchange(sifenConfig, urlString, soapMessage);
    }
}
