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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.ValidationContext;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.ValidationRule;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.core.validation.support.LoadedCertificate;
import org.javabeanstack.sifen.core.validation.support.LoadedCertificate.Cause;
import org.javabeanstack.sifen.internal.cert.CertificateStore;

/**
 * Reglas del certificado de firma y de TLS ({@code JBS-CERT-*} salvo la 11; plan JBSSIF, §3.7, D33
 * Parte B), sobre el resultado de <b>intentar cargarlo</b> ({@link LoadedCertificate}): la carga no
 * lanza, cada causa de falla es una regla.
 *
 * <p>Se evalúan con {@code usarCertificadoCliente = true}; si no, ya rige {@code JBS-CFG-12}. El
 * certificado lo da {@link CertificateStore}: una carga por archivo mientras no cambie su huella.
 * Ruta base {@code sifenConfig} (no {@code sifenConfig.certificadoCliente}, como decía el diseño):
 * la contraseña y el tipo son campos hermanos del certificado.</p>
 */
final class CertificateRules extends RuleSet<SifenConfig, LoadedCertificate> {
    /** Antelación del aviso de vencimiento (CERT-09). */
    static final int DIAS_AVISO_VENCIMIENTO = 30;

    CertificateRules() {
        super("CERT-certificado", RuleGroup.CERT, ValidationRule.CONFIG_ROOT);
        declare(rule("JBS-CERT-01").field("certificadoCliente").error().source(RuleSource.JBS),
                passesWithArgs(s -> !causa(s, Cause.MISSING, Cause.NOT_FOUND), s -> new Object[] {origen(s.e())}));
        declare(rule("JBS-CERT-02").field("certificadoCliente").error().source(RuleSource.JBS),
                passesWithArgs(s -> !causa(s, Cause.UNREADABLE), s -> new Object[] {origen(s.e())}));
        declare(rule("JBS-CERT-03").field("contrasenaCertificadoCliente").error().source(RuleSource.JBS),
                passes(s -> !causa(s, Cause.WRONG_PASSWORD)));
        declare(rule("JBS-CERT-04").field("contrasenaCertificadoCliente").error().source(RuleSource.JBS),
                passes(s -> !causa(s, Cause.NO_PASSWORD)));
        declare(rule("JBS-CERT-05").field("certificadoCliente").error().source(RuleSource.JBS),
                passesWithArgs(s -> !causa(s, Cause.NOT_PKCS12), s -> new Object[] {origen(s.e())}));
        declare(rule("JBS-CERT-06").field("certificadoCliente").error().source(RuleSource.JBS),
                passes(s -> !causa(s, Cause.NO_PRIVATE_KEY)));
        declare(rule("JBS-CERT-07").field("certificadoCliente").warning().source(RuleSource.JBS),
                passesWithArgs(s -> !s.e().isLoaded() || s.e().getAliasCount() <= 1,
                        s -> new Object[] {s.e().getAliasCount()}));
        declare(rule("JBS-CERT-08").field("certificadoCliente").error().source(RuleSource.JBS),
                passesWithArgs(s -> !s.e().isLoaded() || !s.e().getNotAfter().isBefore(ahora(s.ctx())),
                        s -> new Object[] {fecha(s.e().getNotAfter())}));
        declare(rule("JBS-CERT-09").field("certificadoCliente").warning().source(RuleSource.JBS),
                passesWithArgs(s -> !venceProntoSinVencer(s.e(), ahora(s.ctx())),
                        s -> new Object[] {fecha(s.e().getNotAfter()),
                                Duration.between(ahora(s.ctx()), s.e().getNotAfter()).toDays()}));
        declare(rule("JBS-CERT-10").field("certificadoCliente").error().source(RuleSource.JBS),
                passesWithArgs(s -> !s.e().isLoaded() || !s.e().getNotBefore().isAfter(ahora(s.ctx())),
                        s -> new Object[] {fecha(s.e().getNotBefore())}));
        declare(rule("JBS-CERT-12").field("certificadoCliente").warning().source(RuleSource.JBS),
                passesWithArgs(s -> !s.e().isLoaded() || observaciones(s.e()).isEmpty(),
                        s -> new Object[] {String.join("; ", observaciones(s.e()))}));
        declare(rule("JBS-CERT-13").field("tipoCertificadoCliente").error().source(RuleSource.JBS),
                passes(s -> !causa(s, Cause.UNSUPPORTED_TYPE)));
        declare(rule("JBS-CERT-14").field("certificadoCliente").error().source(RuleSource.JBS),
                passes(s -> !causa(s, Cause.INVALID_BASE64)));
        // Informativo: se consume una vez, en la corrida que recargó el archivo (D33, §4.2).
        declare(rule("JBS-CERT-15").field("certificadoCliente").warning().source(RuleSource.JBS),
                passes(s -> !s.e().consumeChange()));
    }

    @Override
    protected List<Subject<SifenConfig, LoadedCertificate>> subjects(SifenConfig raiz) {
        return raiz.isUseClientCertificate() ? unique(CertificateStore.get(raiz)) : none();
    }

    private static boolean causa(Subject<SifenConfig, LoadedCertificate> s, Cause... causas) {
        for (Cause c : causas) {
            if (s.e().getCause() == c) {
                return true;
            }
        }
        return false;
    }

    /** Detalle del origen para el mensaje: la ruta (no es secreta) o «en línea»; nunca el contenido. */
    private static String origen(LoadedCertificate c) {
        if (c.isInline()) {
            return "contenido en línea";
        }
        return c.getPath() == null ? "no indicado" : c.getPath();
    }

    private static Instant ahora(ValidationContext ctx) {
        return ctx.getNow().atZone(ValidationContext.SIFEN_ZONE).toInstant();
    }

    private static LocalDate fecha(Instant instante) {
        return instante == null ? null : instante.atZone(ValidationContext.SIFEN_ZONE).toLocalDate();
    }

    private static boolean venceProntoSinVencer(LoadedCertificate c, Instant ahora) {
        if (!c.isLoaded() || c.getNotAfter().isBefore(ahora)) {
            return false;
        }
        return Duration.between(ahora, c.getNotAfter()).toDays() < DIAS_AVISO_VENCIMIENTO;
    }

    private static List<String> observaciones(LoadedCertificate c) {
        List<String> l = new ArrayList<>();
        if (!c.isDigitalSignature()) {
            l.add("el uso de clave no incluye la firma digital");
        }
        if (c.getDocumentNumbers().isEmpty()) {
            l.add("el sujeto no trae el RUC ni la CI del titular (SERIALNUMBER)");
        }
        return l;
    }
}
