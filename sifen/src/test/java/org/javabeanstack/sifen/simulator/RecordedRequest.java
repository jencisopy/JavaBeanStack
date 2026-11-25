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
package org.javabeanstack.sifen.simulator;

import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Un pedido que recibió {@link SifenSimulator} (plan JBSSIF, §8.6): ruta, empresa, servicio, cuerpo y
 * el certificado de cliente con el que se abrió la conexión TLS. Inmutable.
 */
public final class RecordedRequest {
    private final String path;
    private final String tenant;
    private final SimulatedService service;
    private final String body;
    private final X509Certificate clientCertificate;
    private final Instant receivedAt;

    RecordedRequest(String path, String tenant, SimulatedService service, String body,
            X509Certificate clientCertificate, Instant receivedAt) {
        this.path = path;
        this.tenant = tenant;
        this.service = service;
        this.body = body;
        this.clientCertificate = clientCertificate;
        this.receivedAt = receivedAt;
    }

    /** @return la ruta del pedido, con el prefijo de empresa si lo hay. */
    public String getPath() {
        return path;
    }

    /** @return la empresa (prefijo de la ruta), {@code ""} si no hay; nunca {@code null}. */
    public String getTenant() {
        return tenant;
    }

    /** @return el servicio, o {@code null} si la ruta no es de ninguno (el simulador contestó 404). */
    public SimulatedService getService() {
        return service;
    }

    /** @return el cuerpo SOAP recibido (UTF-8). */
    public String getBody() {
        return body;
    }

    /** @return el certificado de cliente de la conexión TLS, o {@code null} si no presentó ninguno. */
    public X509Certificate getClientCertificate() {
        return clientCertificate;
    }

    /** @return el sujeto del certificado de cliente (RFC 2253, con {@code SERIALNUMBER} por nombre), o {@code ""} si no hay. */
    public String getClientSubject() {
        return clientCertificate == null ? "" : clientCertificate.getSubjectX500Principal()
                .getName(javax.security.auth.x500.X500Principal.RFC2253, java.util.Map.of("2.5.4.5", "SERIALNUMBER"));
    }

    /** @return cuándo llegó. */
    public Instant getReceivedAt() {
        return receivedAt;
    }

    /**
     * Texto del primer elemento con ese nombre local, con o sin prefijo, con las referencias de XML
     * decodificadas ({@code &amp;amp;}, {@code &amp;#13;}…), como lo leería un parser (I10-16).
     *
     * @param localName nombre local (p. ej. {@code dId}, {@code dCDC}, {@code DigestValue}).
     * @return el texto, o {@code null} si no está.
     */
    public String text(String localName) {
        String raw = rawText(localName);
        return raw == null ? null : decode(raw);
    }

    /**
     * Texto del primer elemento con ese nombre local tal como viajó, sin decodificar las referencias.
     *
     * @param localName nombre local.
     * @return el texto crudo, o {@code null} si no está.
     */
    public String rawText(String localName) {
        return text(body, localName);
    }

    /**
     * Decodifica las referencias de XML de un texto: las cinco predefinidas y las numéricas decimales y
     * hexadecimales. Una referencia que no reconoce queda tal cual.
     *
     * @param text texto con referencias.
     * @return el texto decodificado.
     */
    public static String decode(String text) {
        if (text.indexOf('&') < 0) {
            return text;
        }
        Matcher m = Pattern.compile("&(#x[0-9A-Fa-f]+|#[0-9]+|amp|lt|gt|quot|apos);").matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String r = m.group(1);
            String v;
            switch (r) {
                case "amp": v = "&"; break;
                case "lt": v = "<"; break;
                case "gt": v = ">"; break;
                case "quot": v = "\""; break;
                case "apos": v = "'"; break;
                default:
                    int cp = r.startsWith("#x") ? Integer.parseInt(r.substring(2), 16) : Integer.parseInt(r.substring(1));
                    v = new String(Character.toChars(cp));
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(v));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** @return el {@code dId} del pedido, o {@code -1} si no tiene. */
    public long getRequestId() {
        String t = text("dId");
        return t == null ? -1 : Long.parseLong(t.trim());
    }

    /** @return el CDC del DE enviado ({@code DE Id}), el consultado ({@code dCDC}) o {@code null}. */
    public String getCdc() {
        Matcher m = Pattern.compile("<(?:\\w+:)?DE\\s+Id=\"(\\d{44})\"").matcher(body);
        if (m.find()) {
            return m.group(1);
        }
        return text("dCDC");
    }

    static String text(String xml, String localName) {
        Matcher m = Pattern.compile("<(?:\\w+:)?" + Pattern.quote(localName) + "(?:\\s[^>]*)?>([^<]*)</").matcher(xml);
        return m.find() ? m.group(1) : null;
    }

    @Override
    public String toString() {
        return "RecordedRequest[" + path + ", tenant=" + tenant + ", service=" + service + ", client=" + getClientSubject() + "]";
    }
}
