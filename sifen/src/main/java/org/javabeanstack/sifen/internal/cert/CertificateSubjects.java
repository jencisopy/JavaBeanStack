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
package org.javabeanstack.sifen.internal.cert;

import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;
import org.javabeanstack.sifen.internal.util.SifenUtil;

/**
 * Lectura de los datos del sujeto de un certificado (plan JBSSIF, §3.7; E2-05): el
 * {@code SERIALNUMBER} con el RUC o la CI del titular y su nombre, del sujeto o del
 * {@code dirName} del SAN.
 *
 * <p>El JDK entrega el {@code dirName} en RFC 2253 con el {@code SERIALNUMBER} como OID
 * {@code 2.5.4.5} y el valor en hexadecimal DER ({@code #130d…}); se reinterpreta con
 * {@link X500Principal} y un mapa de OID, que devuelve el texto.</p>
 */
public final class CertificateSubjects {
    private static final Map<String, String> OID = Map.of("2.5.4.5", "SERIALNUMBER");

    /**
     * Un sujeto del certificado.
     *
     * @param documentNumber número de documento con prefijo ({@code RUC80123456-5}), o {@code null}.
     * @param name    {@code CN} u {@code O}, o {@code null}.
     */
    public record CertificateSubject(String documentNumber, String name) {
    }

    private CertificateSubjects() {
    }

    /**
     * Devuelve los sujetos del certificado: el principal y los {@code dirName} del SAN, en ese orden.
     *
     * @param certificate el certificado; {@code null} da lista vacía.
     * @return lista modificable; nunca {@code null}.
     */
    public static List<CertificateSubject> of(X509Certificate certificate) {
        List<CertificateSubject> l = new ArrayList<>();
        if (certificate == null) {
            return l;
        }
        l.add(sujeto(certificate.getSubjectX500Principal().getName(X500Principal.RFC2253, OID)));
        l.addAll(ofSan(certificate));
        return l;
    }

    /**
     * Devuelve los sujetos de los {@code dirName} del SAN.
     *
     * @param certificate el certificado.
     * @return lista modificable; vacía si no hay SAN o no se puede leer.
     */
    public static List<CertificateSubject> ofSan(X509Certificate certificate) {
        List<CertificateSubject> l = new ArrayList<>();
        Collection<List<?>> alternativos;
        try {
            alternativos = certificate.getSubjectAlternativeNames();
        } catch (CertificateParsingException e) {
            return l;
        }
        if (alternativos == null) {
            return l;
        }
        for (List<?> alternativo : alternativos) {
            if (alternativo != null && alternativo.size() >= 2 && Integer.valueOf(4).equals(alternativo.get(0))
                    && alternativo.get(1) instanceof String dn) {
                try {
                    l.add(sujeto(new X500Principal(dn).getName(X500Principal.RFC2253, OID)));
                } catch (IllegalArgumentException e) {
                    // dirName mal formado: se ignora, como cualquier otro nombre alternativo ilegible.
                }
            }
        }
        return l;
    }

    /**
     * Devuelve el número de documento de un {@code SERIALNUMBER}, con la semántica de
     * {@code ValidezFirmaDigital.SujetoCertificado.create} del fork, que es la que valida la firma
     * (I3-59, ESC-13): prefijo {@code RUC} o {@code CI} <b>distinguiendo mayúsculas</b>, el resto sin
     * los espacios de los extremos y, si no trae DV, con el DV <b>calculado</b> ({@code SifenUtil.generateDv}).
     *
     * <p>Único desvío deliberado: un número con caracteres que no son dígitos y sin DV da {@code null}
     * en vez de la {@code NumberFormatException} del fork (un sujeto sin número, que no coincide).</p>
     *
     * @param serialNumber el valor del {@code SERIALNUMBER}; puede ser {@code null}.
     * @return el número con DV ({@code 80123456-5}), o {@code null} si no trae prefijo conocido.
     */
    public static String normalizeDocumentNumber(String serialNumber) {
        if (serialNumber == null || serialNumber.isBlank()) {
            return null;
        }
        String n;
        if (serialNumber.startsWith("CI")) {
            n = serialNumber.substring(2).trim();
        } else if (serialNumber.startsWith("RUC")) {
            n = serialNumber.substring(3).trim();
        } else {
            return null;
        }
        if (!n.contains("-")) {
            try {
                n = n + "-" + SifenUtil.generateDv(n);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return n;
    }

    /**
     * Devuelve un atributo de un DN en RFC 2253 con nombres de atributo ({@code SERIALNUMBER}, {@code CN}).
     *
     * @param dn   el DN.
     * @param tipo el atributo, sin distinguir mayúsculas.
     * @return el valor, o {@code null}.
     */
    public static String attribute(String dn, String tipo) {
        try {
            for (Rdn rdn : new LdapName(dn).getRdns()) {
                if (rdn.getType().equalsIgnoreCase(tipo) && rdn.getValue() instanceof String valor) {
                    return valor;
                }
            }
        } catch (InvalidNameException | IllegalArgumentException e) {
            return null;
        }
        return null;
    }

    /**
     * Compara el número de documento del certificado con el emisor (§3.7, E2-05, JBS-CERT-11), como
     * {@code SignatureHelper.checkDocumentIssuer} del fork (I3-59): igualdad exacta con
     * {@code dRucEm + "-" + dDVEmi}. Null-safe (E0-07): un dato nulo no coincide.
     *
     * @param numero número del certificado, el que devuelve {@link #normalizeDocumentNumber(String)}; {@code null} no coincide.
     * @param ruc    {@code dRucEm}.
     * @param dv     {@code dDVEmi}.
     * @return {@code true} si coinciden.
     */
    public static boolean matchesIssuer(String numero, String ruc, String dv) {
        return numero != null && ruc != null && dv != null && numero.equals(ruc + "-" + dv);
    }

    private static CertificateSubject sujeto(String dn) {
        String cn = attribute(dn, "CN");
        return new CertificateSubject(attribute(dn, "SERIALNUMBER"), cn != null ? cn : attribute(dn, "O"));
    }
}
