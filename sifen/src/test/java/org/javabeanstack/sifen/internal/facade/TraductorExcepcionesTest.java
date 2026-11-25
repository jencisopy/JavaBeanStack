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
package org.javabeanstack.sifen.internal.facade;

import jakarta.xml.soap.SOAPException;
import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import javax.net.ssl.SSLHandshakeException;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.transform.TransformerException;
import org.javabeanstack.sifen.core.exceptions.ErrorCategory;
import org.javabeanstack.sifen.core.exceptions.ErrorCode;
import org.javabeanstack.sifen.core.exceptions.Operation;
import org.javabeanstack.sifen.core.exceptions.SifenConnectionException;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.validation.SifenValidationException;
import org.javabeanstack.sifen.core.validation.ValidationResult;
import org.javabeanstack.sifen.internal.response.UnconvertibleFieldException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Filas del traductor y reglas del envoltorio (plan JBSSIF, §3.8, CA3-09 parte «traductor»): los
 * puntos de falla que no se pueden provocar sin red real (DNS, proxy, tiempo de conexión, TLS,
 * algoritmo ausente) se ejercitan con la excepción que produce el JDK.
 */
public class TraductorExcepcionesTest {

    private static Optional<ErrorCode> red(Throwable t) {
        return ExceptionTranslator.translate(t, ExceptionTranslator.Stage.TRANSPORT);
    }

    @Test
    public void lasFallasDeRedTienenSuCodigo() {
        Assertions.assertEquals(ErrorCode.JBS_CONEX_01, red(new UnknownHostException("sifen-test.set.gov.py")).get());
        Assertions.assertEquals(ErrorCode.JBS_CONEX_02,
                red(new IOException("Unable to tunnel through proxy. Proxy returns \"HTTP/1.1 407 Proxy Authentication Required\"")).get());
        Assertions.assertEquals(ErrorCode.JBS_CONEX_04, red(new SocketTimeoutException("Connect timed out")).get());
        Assertions.assertEquals(ErrorCode.JBS_CONEX_05, red(new SocketTimeoutException("Read timed out")).get());
        Assertions.assertEquals(ErrorCode.JBS_CONEX_03, red(new ConnectException("Connection refused")).get());
        Assertions.assertEquals(ErrorCode.JBS_CONEX_06, red(new SSLHandshakeException("PKIX path building failed")).get());
        Assertions.assertEquals(ErrorCode.JBS_CONEX_07, red(new SocketException("Connection reset")).get());
        Assertions.assertEquals(ErrorCode.JBS_CONEX_07, red(new EOFException()).get());
        Assertions.assertEquals(ErrorCode.JBS_CONEX_10, red(new SOAPException("no es SOAP")).get());
        Assertions.assertEquals(ErrorCode.JBS_CONEX_07, red(new IOException("otra")).get(), "fallback: resultado desconocido");
    }

    @Test
    public void lasDeFirmaArmadoYLecturaTambien() {
        ExceptionTranslator.Stage op = ExceptionTranslator.Stage.OPERATION;
        Assertions.assertEquals(ErrorCode.JBS_FIRMA_02,
                ExceptionTranslator.translate(new RuntimeException(new NoSuchAlgorithmException("SHA-256")), op).get());
        Assertions.assertEquals(ErrorCode.JBS_FIRMA_03, ExceptionTranslator.translate(new XMLSignatureException("x"), op).get());
        Assertions.assertEquals(ErrorCode.JBS_INT_02,
                ExceptionTranslator.translate(new RuntimeException(new TransformerException("x")), op).get());
        Assertions.assertEquals(ErrorCode.JBS_INT_01, ExceptionTranslator.translate(new SOAPException("x"), op).get());
        Assertions.assertTrue(ExceptionTranslator.translate(new NullPointerException(), op).isEmpty(), "lo imprevisto no tiene fila");
        UnconvertibleFieldException campo = new UnconvertibleFieldException("DE.gDatGralOpe.dFeEmiDE",
                new DateTimeParseException("x", "23/11", 0));
        Assertions.assertEquals(ErrorCode.JBS_XML_05,
                ExceptionTranslator.translate(campo, ExceptionTranslator.Stage.XML_READ).get());
        Assertions.assertEquals(ErrorCode.JBS_RESP_06,
                ExceptionTranslator.translate(campo, ExceptionTranslator.Stage.RESPONSE).get());
        Assertions.assertTrue(ExceptionTranslator.translate(campo, op).isEmpty(), "fuera de la lectura no es del usuario");
        Assertions.assertFalse(ExceptionTranslator.describe().isEmpty());
    }

    @Test
    public void laExcepcionDeConexionLlevaSuTipo() {
        SifenException e = SifenException.of(ErrorCode.JBS_CONEX_01, "detalle", null, "sifen-test.set.gov.py");
        SifenConnectionException c = Assertions.assertInstanceOf(SifenConnectionException.class, e);
        Assertions.assertEquals(SifenConnectionException.Type.DNS, c.getType());
        Assertions.assertEquals(ErrorCategory.CONNECTION, c.getCategory());
        Assertions.assertTrue(c.getMessage().contains("sifen-test.set.gov.py"), c.getMessage());
        for (ErrorCode ce : ErrorCode.values()) {
            if (ce.getCategory() == ErrorCategory.CONNECTION) {
                Assertions.assertNotNull(SifenConnectionException.typeOf(ce), ce.getCode());
            }
        }
    }

    @Test
    public void loImprevistoSaleComoInt00ConIdYLaMismaCausa() {
        NullPointerException npe = new NullPointerException("dato del documento");
        SifenException e = Assertions.assertThrows(SifenException.class,
                () -> Operations.execute(Operation.QUERY_RUC, () -> "80123456", () -> {
                    throw npe;
                }));
        Assertions.assertEquals("JBS-INT-00", e.getCode());
        Assertions.assertSame(npe, e.getCause());
        Assertions.assertEquals(Operation.QUERY_RUC, e.getOperation());
        Assertions.assertEquals("80123456", e.getReference());
        Assertions.assertTrue(e.getMessage().endsWith("(ref. " + e.getCorrelationId() + ")"));
        Assertions.assertFalse(e.getMessage().contains("dato del documento"), "el mensaje de la causa no llega a pantalla");
        Assertions.assertTrue(e.getTechnicalDetail().contains("dato del documento"), "va al detalle técnico");
        Assertions.assertEquals("i100", e.getLegacyCode());
    }

    @Test
    public void unLinkageErrorTambienTieneCodigo() {
        SifenException e = Assertions.assertThrows(SifenException.class,
                () -> Operations.execute(Operation.GENERATE_XML, null, () -> {
                    throw new NoSuchMethodError("setIdAttributeNode");
                }));
        Assertions.assertEquals("JBS-INT-00", e.getCode());
    }

    @Test
    public void loDiagnosticadoSaleLaMismaInstanciaYLaInternaNoReasignaElId() {
        SifenValidationException original = new SifenValidationException(ValidationResult.empty());
        SifenException e = Assertions.assertThrows(SifenException.class,
                () -> Operations.execute(Operation.SEND_DE, () -> "CDC", () ->
                        Operations.execute(Operation.GENERATE_XML, () -> "interna", () -> {
                            throw original;
                        })));
        Assertions.assertSame(original, e);
        Assertions.assertEquals(Operation.SEND_DE, e.getOperation(), "la operación es la más externa");
        Assertions.assertEquals("CDC", e.getReference());
        Assertions.assertEquals("i405", e.getLegacyCode(), "contrato de §3.5; desde D42 (§11.4) por getLegacyCode()");
        String id = e.getCorrelationId();
        e.withContext(Operation.QUERY_DE, "otra", "ffffffff");
        Assertions.assertEquals(id, e.getCorrelationId(), "el id se asigna una vez");
    }

    @Test
    public void laAnidadaTraduceConSuOperacion() {
        SifenException e = Assertions.assertThrows(SifenException.class,
                () -> Operations.execute(Operation.QUERY_DE, null, () ->
                        Operations.execute(Operation.READ_XML, null, () -> {
                            throw new UnconvertibleFieldException("DE.dSisFact", new NumberFormatException("x"));
                        })));
        Assertions.assertEquals("JBS-XML-05", e.getCode());
        Assertions.assertTrue(e.getMessage().contains("DE.dSisFact"), e.getMessage());
        Assertions.assertEquals(Operation.QUERY_DE, e.getOperation());
    }

    @Test
    public void elMapaLegadoParteDeLoEmitido() {
        @SuppressWarnings("deprecation")
        List<String> i200 = org.javabeanstack.sifen.core.exceptions.SifenExceptionCodes.newCodes("i200");
        Assertions.assertTrue(i200.isEmpty(), "I3-22: i200 no se emitía");
        @SuppressWarnings("deprecation")
        List<String> i100 = org.javabeanstack.sifen.core.exceptions.SifenExceptionCodes.newCodes("i100");
        Assertions.assertTrue(i100.contains("JBS-CERT-01"), "I3-22: el certificado salía como i100");
        Assertions.assertEquals(ErrorCategory.CONFIGURATION, SifenException.categoryOf("i101"));
        Assertions.assertEquals(ErrorCategory.CERTIFICATE, SifenException.categoryOf("JBS-CERT-03"));
        Assertions.assertEquals(ErrorCategory.INPUT, SifenException.categoryOf("JBS-VAL-01"));
        Assertions.assertEquals(ErrorCategory.INTERNAL, SifenException.categoryOf("JBS-VAL-00"));
        Assertions.assertEquals(ErrorCategory.DE_CONTENT, SifenException.categoryOf("D208c"));
    }
}
