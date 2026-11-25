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
import org.javabeanstack.sifen.core.validation.RuleSet;

/**
 * Conjuntos de la configuración y del certificado (plan JBSSIF, §3.7, D33 Parte B), en el orden en que
 * corren. Los usa {@code ConfigValidator}; {@code RuleCatalog} los incluye en el catálogo.
 */
public final class ConfigRuleSets {
    /** Configuración ({@code JBS-CFG-*}) y certificado ({@code JBS-CERT-*} salvo la 11). */
    public static final List<RuleSet<SifenConfig, ?>> STANDARD = List.of(
            new ConfigRules(),
            new CertificateRules());

    /** Coherencia del DE con el certificado ({@code JBS-CERT-11}). */
    public static final List<RuleSet<DocumentoElectronico, ?>> DE_CONSISTENCY = List.of(
            new DECertificateRules());

    private ConfigRuleSets() {
    }
}
