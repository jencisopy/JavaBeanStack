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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.validation.RuleCatalog;
import org.javabeanstack.sifen.core.validation.RuleSet;
import org.javabeanstack.sifen.core.validation.RuleSource;
import org.javabeanstack.sifen.core.validation.RuleGroup;
import org.javabeanstack.sifen.core.validation.NotEvaluatedReason;
import org.javabeanstack.sifen.core.validation.ValidationRule;
import org.javabeanstack.sifen.core.validation.Subject;
import org.javabeanstack.sifen.internal.util.SifenUtil;

/**
 * Reglas de la configuración de la librería ({@code JBS-CFG-*}; plan JBSSIF, §3.7, D33 Parte B):
 * reemplazan los 19 {@code if} de {@code Sifen.validateConfiguration} y suman lo que la línea base no
 * miraba (protocolo, host contra ambiente, formato del CSC, propiedades mal escritas).
 *
 * <p>Sin E/S: corre antes de cada operación. Los secretos ({@code CSC}, contraseña) nunca se muestran
 * (política {@code OMIT}); los detalles de los mensajes son nombres de campo o de propiedad.</p>
 */
final class ConfigRules extends RuleSet<SifenConfig, SifenConfig> {
    /** Host de SIFEN de pruebas y de producción (CFG-05; refuerza D28). */
    static final String HOST_DEV = "sifen-test.set.gov.py";
    static final String HOST_PROD = "sifen.set.gov.py";
    /** Pares de CSC genéricos de pruebas que publica la SET. */
    private static final List<String[]> CSC_GENERICOS = List.of(
            new String[] {"0001", "ABCD0000000000000000000000000000"},
            new String[] {"0002", "EFGH0000000000000000000000000000"});

    ConfigRules() {
        super("CFG-configuracion", RuleGroup.CFG, ValidationRule.CONFIG_ROOT);
        // Lo reporta ConfigValidator con la configuración nula; acá nunca falla.
        declare(rule("JBS-CFG-01").field("").error().source(RuleSource.JBS), passes(s -> s.root() != null));
        declare(rule("JBS-CFG-02").field("ambiente").error().source(RuleSource.JBS),
                requiredIf(s -> !s.e().getInvalidProperties().contains("sifen.ambiente"), SifenConfig::getEnvironment));
        // La URL no se muestra (podría traer credenciales de un proxy): el mensaje basta.
        declare(rule("JBS-CFG-03").field("urlBase").error().source(RuleSource.JBS), passes(s -> uri(s.e()).isPresent()));
        declare(rule("JBS-CFG-04").field("urlBase").error().source(RuleSource.JBS),
                passes(s -> uri(s.e()).map(u -> "https".equalsIgnoreCase(u.getScheme())).orElse(true)));
        declare(rule("JBS-CFG-05").field("urlBase").error().source(RuleSource.JBS),
                passesWithArgs(s -> hostCoherente(s.e()), s -> new Object[] {
                        HOST_DEV.equalsIgnoreCase(host(s.e())) ? "de pruebas" : "de producción",
                        String.valueOf(s.e().getEnvironment())}));
        declare(rule("JBS-CFG-06").field("").error().source(RuleSource.JBS),
                passesWithArgs(s -> serviciosFaltantes(s.e()).isEmpty(),
                        s -> new Object[] {String.join(", ", serviciosFaltantes(s.e()))}));
        declare(rule("JBS-CFG-07").field("CSC").error().source(RuleSource.JBS),
                passesWithArgs(s -> cscFaltantes(s.e()).isEmpty(), s -> new Object[] {String.join(", ", cscFaltantes(s.e()))}));
        declare(rule("JBS-CFG-08").field("CSC").error().source(RuleSource.JBS), passes(s -> !cscGenericoEnProduccion(s.e())));
        declare(rule("JBS-CFG-09").field("CSC").error().source(RuleSource.JBS),
                passesWithArgs(s -> formatoCsc(s.e()) == null, s -> new Object[] {formatoCsc(s.e())}));
        declare(rule("JBS-CFG-10").field("").error().source(RuleSource.JBS),
                passesWithArgs(s -> s.e().getInvalidProperties().isEmpty(),
                        s -> new Object[] {String.join(", ", s.e().getInvalidProperties())}));
        declare(rule("JBS-CFG-11").field("").warning().source(RuleSource.JBS),
                passesWithArgs(s -> s.e().getUnknownProperties().isEmpty(),
                        s -> new Object[] {String.join(", ", s.e().getUnknownProperties())}));
        declare(rule("JBS-CFG-12").field("usarCertificadoCliente").error().showValue().source(RuleSource.JBS),
                passes(s -> s.e().isUseClientCertificate(), s -> s.e().isUseClientCertificate()));
        // Los tiempos de espera son configurables desde §5.2 (sifen.http.*) y un tiempo <= 0 ya lo rechaza
        // SifenConfig (el setter lanza; load lo deja como clave inválida, JBS-CFG-10): la regla no se evalúa (I8-36).
        declareNotEvaluated(rule("JBS-CFG-13").field("httpConnectTimeout").error().source(RuleSource.JBS), NotEvaluatedReason.CONSTANT);
        declare(rule("JBS-CFG-14").field("validacionReglasDesactivadas").warning().source(RuleSource.JBS),
                passesWithArgs(s -> desactivadasInvalidas(s.e()).isEmpty(),
                        s -> new Object[] {String.join(", ", desactivadasInvalidas(s.e()))}));
    }

    @Override
    protected List<Subject<SifenConfig, SifenConfig>> subjects(SifenConfig raiz) {
        return unique(raiz);
    }

    /** URL base efectiva: la propia o la del ambiente (la que usa {@code BaseRequest}). */
    static String urlEfectiva(SifenConfig c) {
        return SifenUtil.isNotBlank(c.getBaseUrl()) ? c.getBaseUrl().trim() : c.getLocalBaseUrl();
    }

    /** La URL base como URI absoluta con host, si lo es. */
    static Optional<URI> uri(SifenConfig c) {
        String url = urlEfectiva(c);
        if (SifenUtil.isBlank(url)) {
            return Optional.empty();
        }
        try {
            URI u = new URI(url);
            return u.isAbsolute() && u.getHost() != null ? Optional.of(u) : Optional.empty();
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }

    private static String host(SifenConfig c) {
        return uri(c).map(URI::getHost).orElse(null);
    }

    /** Solo se juzgan los hosts conocidos de SIFEN; un servidor local o un proxy no. */
    private static boolean hostCoherente(SifenConfig c) {
        String h = host(c);
        if (h == null || c.getEnvironment() == null) {
            return true;
        }
        if (HOST_DEV.equalsIgnoreCase(h)) {
            return c.getEnvironment() == SifenConfig.Environment.DEV;
        }
        if (HOST_PROD.equalsIgnoreCase(h)) {
            return c.getEnvironment() == SifenConfig.Environment.PROD;
        }
        return true;
    }

    private static List<String> serviciosFaltantes(SifenConfig c) {
        List<String> l = new ArrayList<>();
        agregarSiFalta(l, "pathRecibe", c.getSendPath());
        agregarSiFalta(l, "pathRecibeLote", c.getSendBatchPath());
        agregarSiFalta(l, "pathEvento", c.getEventPath());
        agregarSiFalta(l, "pathConsultaLote", c.getQueryBatchPath());
        agregarSiFalta(l, "pathConsultaRUC", c.getQueryRucPath());
        agregarSiFalta(l, "pathConsulta", c.getQueryDEPath());
        agregarSiFalta(l, "urlConsultaQr", c.getQRQueryUrl());
        return l;
    }

    private static List<String> cscFaltantes(SifenConfig c) {
        List<String> l = new ArrayList<>();
        agregarSiFalta(l, "idCSC", c.getCscId());
        agregarSiFalta(l, "CSC", c.getCsc());
        return l;
    }

    private static void agregarSiFalta(List<String> l, String nombre, String valor) {
        if (SifenUtil.isBlank(valor)) {
            l.add(nombre);
        }
    }

    private static boolean cscGenericoEnProduccion(SifenConfig c) {
        if (c.getEnvironment() != SifenConfig.Environment.PROD) {
            return false;
        }
        for (String[] par : CSC_GENERICOS) {
            if (par[0].equals(c.getCscId()) && par[1].equals(c.getCsc())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Qué falla del formato del CSC, sin mostrarlo: {@code idCSC} de 4 dígitos ({@code setCscId(null)}
     * deja {@code "null"}, M3-19) y {@code CSC} de 32 caracteres alfanuméricos.
     *
     * @return la descripción, o {@code null} si el formato es correcto o falta (lo informa CFG-07).
     */
    private static String formatoCsc(SifenConfig c) {
        List<String> l = new ArrayList<>();
        if (SifenUtil.isNotBlank(c.getCscId()) && !c.getCscId().matches("[0-9]{4}")) {
            l.add("idCSC debe tener 4 dígitos");
        }
        if (SifenUtil.isNotBlank(c.getCsc()) && !c.getCsc().matches("[A-Za-z0-9]{32}")) {
            l.add("CSC debe tener 32 caracteres alfanuméricos");
        }
        return l.isEmpty() ? null : String.join("; ", l);
    }

    /** Códigos desactivados en la configuración que no están en el catálogo o no se pueden desactivar (I3-24, M3-15). */
    private static List<String> desactivadasInvalidas(SifenConfig c) {
        List<String> l = new ArrayList<>();
        for (String codigo : c.getDisabledRules()) {
            if (codigo.startsWith("APP-")) {
                continue; // reglas propias del consumidor: no están en el catálogo de la librería
            }
            Optional<ValidationRule> r = RuleCatalog.byCode(codigo);
            if (r.isEmpty() || !r.get().isDisableable()) {
                l.add(codigo);
            }
        }
        return l;
    }
}
