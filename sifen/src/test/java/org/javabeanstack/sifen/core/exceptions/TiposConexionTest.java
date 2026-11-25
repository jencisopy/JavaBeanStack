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
package org.javabeanstack.sifen.core.exceptions;

import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.EnumSet;
import java.util.Set;
import javax.net.ssl.SSLHandshakeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Cada {@link SifenConnectionException.Type} con su código, su mensaje en español y su argumento
 * (plan JBSSIF, §5.4; RF12, CA5-07, D37). Las fallas reales de cada tipo las provocan
 * {@code SifenConnectionTest} (TCP, PROXY, READ_TIMEOUT, TLS/CERTIFICATE), {@code FachadaFallasTest}
 * (DNS, HTTP_4XX, HTTP_5XX) y {@code TraductorExcepcionesTest} (la tabla excepción → código).
 */
public class TiposConexionTest {

    private static SifenConnectionException conexion(ErrorCode c, Throwable causa, Object arg) {
        return (SifenConnectionException) SifenException.of(c, "detalle", causa, arg);
    }

    @Test
    public void cadaTipoTieneCodigoYMensaje() {
        Object[][] casos = {
            {ErrorCode.JBS_CONEX_01, new UnknownHostException("sifen.set.gov.py"), "sifen.set.gov.py", SifenConnectionException.Type.DNS, "resolver el nombre"},
            {ErrorCode.JBS_CONEX_02, new ConnectException("refused"), "proxy.local:3128", SifenConnectionException.Type.PROXY, "proxy"},
            {ErrorCode.JBS_CONEX_03, new ConnectException("refused"), "sifen.set.gov.py", SifenConnectionException.Type.TCP, "No se pudo conectar"},
            {ErrorCode.JBS_CONEX_04, new SocketTimeoutException("Connect timed out"), 15, SifenConnectionException.Type.CONNECT_TIMEOUT, "tiempo de espera"},
            {ErrorCode.JBS_CONEX_05, new SocketTimeoutException("Read timed out"), 45, SifenConnectionException.Type.READ_TIMEOUT, "no respondió"},
            {ErrorCode.JBS_CONEX_06, new SSLHandshakeException("PKIX path building failed"), "h", SifenConnectionException.Type.TLS, "TLS"},
            {ErrorCode.JBS_CONEX_06, new SSLHandshakeException("Received fatal alert: bad_certificate"), "h", SifenConnectionException.Type.CERTIFICATE, "certificado de cliente"},
            {ErrorCode.JBS_CONEX_07, new SocketException("Connection reset"), "h", SifenConnectionException.Type.TCP, "se cortó"},
            {ErrorCode.JBS_CONEX_08, null, 403, SifenConnectionException.Type.HTTP_4XX, "HTTP 403"},
            {ErrorCode.JBS_CONEX_09, null, 503, SifenConnectionException.Type.HTTP_5XX, "HTTP 503"},
        };
        Set<SifenConnectionException.Type> vistos = EnumSet.noneOf(SifenConnectionException.Type.class);
        for (Object[] c : casos) {
            SifenConnectionException e = conexion((ErrorCode) c[0], (Throwable) c[1], c[2]);
            Assertions.assertEquals(c[3], e.getType(), c[0] + " " + c[1]);
            Assertions.assertEquals(ErrorCategory.CONNECTION, e.getCategory());
            Assertions.assertTrue(e.getUserMessage().contains((String) c[4]), e.getUserMessage());
            vistos.add(e.getType());
        }
        // SOAP_FAULT no es de conexión: es JBS-RESP-01 (se conserva en el enum por RF12)
        Assertions.assertEquals(EnumSet.complementOf(EnumSet.of(SifenConnectionException.Type.SOAP_FAULT)), vistos);
    }

    @Test
    public void elCertificadoRechazadoSeDistingueDelTlsEnLaCadenaDeCausas() {
        Exception envuelta = new SocketException("x", new SSLHandshakeException("Received fatal alert: certificate_required"));
        Assertions.assertEquals(SifenConnectionException.Type.CERTIFICATE,
                SifenConnectionException.typeOf(ErrorCode.JBS_CONEX_06, envuelta));
        Assertions.assertEquals(SifenConnectionException.Type.TLS,
                SifenConnectionException.typeOf(ErrorCode.JBS_CONEX_06, new SSLHandshakeException("Received fatal alert: handshake_failure")));
        Assertions.assertEquals(SifenConnectionException.Type.TLS, SifenConnectionException.typeOf(ErrorCode.JBS_CONEX_06));
        Assertions.assertEquals(SifenConnectionException.Type.TCP,
                SifenConnectionException.typeOf(ErrorCode.JBS_CONEX_07, new SSLHandshakeException("Received fatal alert: bad_certificate")),
                "solo dentro de JBS-CONEX-06");
    }

    @Test
    public void elMensajeDelDnsNombraElServidor() {
        SifenConnectionException e = conexion(ErrorCode.JBS_CONEX_01, new UnknownHostException("sifen.set.gov.py"), "sifen.set.gov.py");
        Assertions.assertTrue(e.getUserMessage().contains("sifen.set.gov.py"), e.getUserMessage());
    }
}
