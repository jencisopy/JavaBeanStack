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
package org.javabeanstack.sifen.core.validation.rules.config;

import java.util.List;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.fields.request.de.TgEmis;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.LoadedCertificate;
import org.javabeanstack.sifen.internal.cert.CertificateStore;

/**
 * Coherencia del DE con el certificado de firma: {@code JBS-CERT-11} (plan JBSSIF, §3.7, D33 Parte B).
 *
 * <p>No es parte del {@code DEValidator} estándar (la validación del contenido no hace E/S ni depende
 * del certificado): {@code ConfigValidator.validateConsistency} la corre en {@code generarXml} y en
 * las recepciones, después del contenido y antes de firmar. El diseño la llamaba
 * {@code DEConfigRules}; ese nombre ya lo usa el conjunto de {@code JBS-VAL-03} y el código
 * pide el grupo {@code CERT} (I3-21 b).</p>
 *
 * <p>Misma semántica que la verificación de firma ({@code SignatureHelper.checkDocumentIssuer} del fork;
 * I3-59, ESC-13): prefijo {@code RUC}/{@code CI} con mayúsculas, el DV calculado si el certificado no lo
 * trae y comparación exacta de RUC y DV. Un certificado cargado <b>sin ningún número</b> rechaza, como
 * {@code JBS-FIRMA-05} en la firma. Sin certificado cargado no evalúa: eso lo informan las
 * {@code JBS-CERT-*} de configuración. Nunca muestra una CI: el mensaje lista solo los RUC.</p>
 */
final class DECertificateRules extends RuleSet<DocumentoElectronico, DocumentoElectronico> {

    DECertificateRules() {
        super("CERT-coherencia-DE", RuleGroup.CERT, "");
        // E3-32: el emisor puede estar en el sujeto o en cualquier dirName del SAN (persona física apoderada)
        declare(rule("JBS-CERT-11").field("gDatGralOpe.gEmis.dRucEm").error().source(RuleSource.JBS),
                passesWithArgs(DECertificateRules::coincide, DECertificateRules::rucsDelCertificado));
    }

    @Override
    protected List<Subject<DocumentoElectronico, DocumentoElectronico>> subjects(DocumentoElectronico raiz) {
        return unique(raiz);
    }

    private static boolean coincide(Subject<DocumentoElectronico, DocumentoElectronico> s) {
        SifenConfig cfg = s.ctx().getSifenConfig();
        TgEmis emisor = s.e().getgDatGralOpe() == null ? null : s.e().getgDatGralOpe().getgEmis();
        if (cfg == null || !cfg.isUseClientCertificate() || emisor == null || emisor.getdRucEm() == null) {
            return true;
        }
        LoadedCertificate c = CertificateStore.get(cfg);
        if (!c.isLoaded()) {
            return true;
        }
        // I3-59: sin ningún número no hay emisor que coincida (la firma lo invalida con JBS-FIRMA-05)
        return c.matchesIssuer(emisor.getdRucEm(), emisor.getdDVEmi());
    }

    /** Solo los RUC del certificado para el mensaje: una CI de persona física no se muestra (dato personal). */
    private static Object[] rucsDelCertificado(Subject<DocumentoElectronico, DocumentoElectronico> s) {
        SifenConfig cfg = s.ctx().getSifenConfig();
        if (cfg == null) {
            return new Object[] {"—"};
        }
        LoadedCertificate c = CertificateStore.get(cfg);
        java.util.List<String> rucs = c.getRucs();
        if (rucs.isEmpty()) {
            return new Object[] {c.getDocumentNumbers().isEmpty() ? "el certificado no trae ningún número de documento"
                    : "el certificado no trae RUC"};
        }
        return new Object[] {"RUC del certificado: " + String.join(", ", rucs)};
    }
}
