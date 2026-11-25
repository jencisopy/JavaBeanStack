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
package org.javabeanstack.sifen.test.fase4;

import com.sun.net.httpserver.HttpServer;
import org.javabeanstack.sifen.ISifenClient;
import org.javabeanstack.sifen.SifenClient;
import org.javabeanstack.sifen.api.SignatureVerification;
import org.javabeanstack.sifen.api.SignedDocument;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.internal.helpers.SignatureHelper;
import org.javabeanstack.sifen.internal.helpers.SoapHelper;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.javabeanstack.sifen.internal.util.SifenUtil;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.xml.soap.SOAPMessage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Ningún punto de entrada de XML resuelve entidades externas ni DTD (plan JBSSIF, E4-02 = F:E10-03, E0-09; M4-02).
 *
 * <p>Por cada XML malicioso —entidad {@code file://} a un archivo con un secreto, entidad {@code http://} y DTD
 * externa contra un servidor local, y «billion laughs»— y por cada entrada —{@code verifySignature} (y
 * {@code SignatureHelper} por texto y por archivo), {@code readSigned}, {@code SignedDocument.getDocument()},
 * {@code new DocumentoElectronico(xml)} y la lectura de respuestas ({@code SoapHelper.parseSoapMessage} y la
 * relectura de {@code ResponseUtil})— se exige: rechazo ({@code JBS-XML-03}, veredicto de falla o excepción de
 * SOAP), <b>ningún</b> pedido al servidor local, el secreto en ningún lado y, en los parsers de la librería,
 * nada escrito en {@code System.err} (M4-02; SAAJ registra su propio {@code SAAJ0511}, M4-03). SAAJ ya rechaza el
 * DOCTYPE en las respuestas: esos dos casos fijan que siga así. La línea base hacía el pedido HTTP y leía el archivo en {@code verifySignature} y en
 * {@code getDocument()}.</p>
 */
public class XxeTest {
    private static HttpServer servidor;
    private static final AtomicInteger pedidos = new AtomicInteger();
    private static String base;
    private static SifenConfig cfg;
    private static ISifenClient cliente;
    private static String secreto;
    private static Path archivoSecreto;
    @TempDir
    static Path carpeta;

    @BeforeAll
    public static void preparar() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", x -> {
            pedidos.incrementAndGet();
            byte[] b = "<!ENTITY y \"z\">".getBytes(StandardCharsets.UTF_8);
            x.sendResponseHeaders(200, b.length);
            x.getResponseBody().write(b);
            x.close();
        });
        servidor.start();
        base = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/";
        cfg = FixturesSifen.configuracion();
        cliente = SifenClient.create(cfg);
        secreto = "SECRETO-" + UUID.randomUUID();
        archivoSecreto = Files.writeString(carpeta.resolve("secreto.txt"), secreto);
    }

    @AfterAll
    public static void bajar() {
        servidor.stop(0);
    }

    @BeforeEach
    public void reiniciar() {
        pedidos.set(0);
    }

    /** XML maliciosos sobre un {@code rDE} firmado válido: el DOCTYPE va adelante y la entidad dentro de un campo. */
    static Stream<String[]> maliciosos() throws Exception {
        String rde = CasosDePrueba.documentos().get(4).construir().generateSignedXml(FixturesSifen.configuracion());
        String conEntidad = rde.replace("<dSisFact>1</dSisFact>", "<dSisFact>&x;</dSisFact>");
        String lol = "<!DOCTYPE rDE [<!ENTITY l \"lol\">"
                + "<!ENTITY l1 \"&l;&l;&l;&l;&l;&l;&l;&l;&l;&l;\"><!ENTITY l2 \"&l1;&l1;&l1;&l1;&l1;&l1;&l1;&l1;&l1;&l1;\">"
                + "<!ENTITY l3 \"&l2;&l2;&l2;&l2;&l2;&l2;&l2;&l2;&l2;&l2;\"><!ENTITY l4 \"&l3;&l3;&l3;&l3;&l3;&l3;&l3;&l3;&l3;&l3;\">"
                + "<!ENTITY l5 \"&l4;&l4;&l4;&l4;&l4;&l4;&l4;&l4;&l4;&l4;\"><!ENTITY l6 \"&l5;&l5;&l5;&l5;&l5;&l5;&l5;&l5;&l5;&l5;\">"
                + "<!ENTITY l7 \"&l6;&l6;&l6;&l6;&l6;&l6;&l6;&l6;&l6;&l6;\"><!ENTITY l8 \"&l7;&l7;&l7;&l7;&l7;&l7;&l7;&l7;&l7;&l7;\">"
                + "<!ENTITY x \"&l8;&l8;&l8;&l8;&l8;&l8;&l8;&l8;&l8;&l8;\">]>";
        return Stream.of(
                new String[]{"entidad file://", "<!DOCTYPE rDE [<!ENTITY x SYSTEM \"" + archivoSecreto.toUri() + "\">]>" + conEntidad},
                new String[]{"entidad http://", "<!DOCTYPE rDE [<!ENTITY x SYSTEM \"" + base + "entidad\">]>" + conEntidad},
                new String[]{"DTD externa", "<!DOCTYPE rDE SYSTEM \"" + base + "dtd\">" + rde},
                new String[]{"entidad de parámetro", "<!DOCTYPE rDE [<!ENTITY % p SYSTEM \"" + base + "param\"> %p;]>" + rde},
                new String[]{"billion laughs", lol + conEntidad});
    }

    /** Corre la entrada capturando {@code System.err} (M4-02) y verifica que no hubo pedido ni fuga del secreto. */
    private static void sinEfectos(String caso, Ejecutable e) throws Exception {
        sinEfectos(caso, true, e);
    }

    /**
     * Como {@link #sinEfectos(String, Ejecutable)}; con {@code revisarErr} falso no mira {@code System.err}: SAAJ
     * registra por JUL su propio {@code SAAJ0511} al rechazar el DOCTYPE (M4-03, fuera del parser de la librería).
     */
    private static void sinEfectos(String caso, boolean revisarErr, Ejecutable e) throws Exception {
        PrintStream original = System.err;
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        String resultado;
        try {
            System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
            resultado = Assertions.assertTimeoutPreemptively(Duration.ofSeconds(10), e::correr, caso);
        } finally {
            System.setErr(original);
        }
        Assertions.assertEquals(0, pedidos.get(), caso + ": pedidos al servidor local");
        Assertions.assertFalse(String.valueOf(resultado).contains(secreto), caso + ": el secreto salió en el resultado");
        if (revisarErr) {
            Assertions.assertEquals("", err.toString(StandardCharsets.UTF_8), caso + ": escribió en System.err");
        }
    }

    @FunctionalInterface
    interface Ejecutable {
        String correr() throws Exception;
    }

    /** {@code JBS-XML-03} sin el secreto en el mensaje ni en el detalle técnico. */
    private static String rechazo(String caso, Ejecutable e) {
        SifenException x = Assertions.assertThrows(SifenException.class, e::correr, caso);
        Assertions.assertEquals("JBS-XML-03", x.getCode(), caso + ": " + x.getMessage());
        return x.getMessage() + " " + x.getTechnicalDetail();
    }

    @ParameterizedTest(name = "verifySignature: {0}")
    @MethodSource("maliciosos")
    public void verifySignature(String caso, String xml) throws Exception {
        sinEfectos(caso, () -> {
            SignatureVerification v = cliente.verifySignature(xml);
            Assertions.assertFalse(v.isValid(), caso);
            Assertions.assertEquals("JBS-XML-03", v.getCode(), caso + ": " + v);
            return v + " " + v.getReason();
        });
    }

    @ParameterizedTest(name = "SignatureHelper texto y archivo: {0}")
    @MethodSource("maliciosos")
    public void signatureHelper(String caso, String xml) throws Exception {
        Path f = Files.writeString(carpeta.resolve(UUID.randomUUID() + ".xml"), xml);
        sinEfectos(caso, () -> rechazo(caso, () -> SignatureHelper.validateSignatureOrThrow(xml, "XML").toString())
                + rechazo(caso, () -> SignatureHelper.validateSignatureOrThrow(f.toFile()).toString()));
    }

    @ParameterizedTest(name = "readSigned: {0}")
    @MethodSource("maliciosos")
    public void readSigned(String caso, String xml) throws Exception {
        sinEfectos(caso, () -> rechazo(caso, () -> cliente.readSigned(xml).toString())
                + rechazo(caso, () -> SifenUtil.readSignedRDE(xml).toString()));
    }

    @ParameterizedTest(name = "getDocument: {0}")
    @MethodSource("maliciosos")
    public void getDocument(String caso, String xml) throws Exception {
        sinEfectos(caso, () -> rechazo(caso, () -> SignedDocument.builder().xml(xml).build().getDocument().getId()));
    }

    @ParameterizedTest(name = "new DocumentoElectronico: {0}")
    @MethodSource("maliciosos")
    public void documentoElectronico(String caso, String xml) throws Exception {
        sinEfectos(caso, () -> rechazo(caso, () -> new DocumentoElectronico(xml).getId()));
    }

    /** Respuesta de SIFEN con DOCTYPE: SAAJ la rechaza sin resolver nada. */
    @ParameterizedTest(name = "respuesta SOAP: {0}")
    @MethodSource("maliciosos")
    public void respuestaSoap(String caso, String xml) throws Exception {
        String doctype = xml.substring(0, xml.indexOf("]>") > 0 && xml.startsWith("<!DOCTYPE rDE [") ? xml.indexOf("]>") + 2 : xml.indexOf('>') + 1)
                .replace("DOCTYPE rDE", "DOCTYPE env:Envelope");
        String sobre = doctype + "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"><env:Body>"
                + "<ns2:rRetEnviDe xmlns:ns2=\"http://ekuatia.set.gov.py/sifen/xsd\"><ns2:dMsgRes>&x;</ns2:dMsgRes></ns2:rRetEnviDe>"
                + "</env:Body></env:Envelope>";
        sinEfectos(caso, false, () -> {
            Exception x = Assertions.assertThrows(Exception.class, () -> {
                SOAPMessage m = SoapHelper.parseSoapMessage(new ByteArrayInputStream(sobre.getBytes(StandardCharsets.UTF_8)));
                m.getSOAPBody();
            }, caso);
            return String.valueOf(x.getMessage());
        });
    }

    /**
     * La relectura de la consulta de DE desescapa el contenido y lo vuelve a leer: un DOCTYPE escapado dentro de
     * la respuesta no se resuelve (queda la respuesta original).
     */
    @ParameterizedTest(name = "relectura de la respuesta: {0}")
    @MethodSource("maliciosos")
    public void relecturaDeLaRespuesta(String caso, String xml) throws Exception {
        String escapado = xml.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        String sobre = "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"><env:Body>"
                + "<ns2:rEnviConsDeResponse xmlns:ns2=\"http://ekuatia.set.gov.py/sifen/xsd\"><ns2:dCodRes>0422</ns2:dCodRes>"
                + "<ns2:xContenDE>" + escapado + "</ns2:xContenDE></ns2:rEnviConsDeResponse></env:Body></env:Envelope>";
        SOAPMessage original = SoapHelper.parseSoapMessage(new ByteArrayInputStream(sobre.getBytes(StandardCharsets.UTF_8)));
        original.getSOAPBody();
        sinEfectos(caso, false, () -> ResponseUtil.getXmlFromMessage(ResponseUtil.parseSoapMessage(original), false));
    }
}
