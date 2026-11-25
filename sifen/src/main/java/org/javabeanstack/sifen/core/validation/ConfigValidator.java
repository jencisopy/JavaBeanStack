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
package org.javabeanstack.sifen.core.validation;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.validation.rules.config.ConfigRuleSets;
import org.javabeanstack.sifen.core.validation.support.LoadedCertificate;
import org.javabeanstack.sifen.internal.cert.CertificateStore;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;

/**
 * Validación de la configuración y del certificado (plan JBSSIF, §3.7, D33 Parte B): reglas
 * {@code JBS-CFG-*} y {@code JBS-CERT-*}, con el mismo motor que el DE.
 *
 * <p>Cuándo corre (D33, §4.2):</p>
 * <ol>
 * <li>al cargar o fijar la configuración ({@code SifenConfig.load},
 * {@code Sifen.setSifenConfig}): con errores lanza {@link SifenValidationException} (código
 * legado {@code i101}) con <b>toda</b> la lista;</li>
 * <li>antes de cada operación ({@code recepcion*}, {@code consulta*}; {@code generarXml} con la
 * validación previa activa): el certificado sale de la caché del almacén, que compara la huella del
 * archivo (un {@code stat}) y lo recarga si cambió o se borró;</li>
 * <li>a pedido, {@code Sifen.validarConfiguracion(cfg)}: recarga forzada, devuelve el resultado sin
 * lanzar (pantalla de diagnóstico del consumidor). Los datos del certificado (titular, vigencia) se
 * leen con {@link #certificate(SifenConfig)}.</li>
 * </ol>
 *
 * <p>{@link #validateConsistency(DocumentoElectronico, SifenConfig)} es la tercera etapa de la validación
 * previa del DE ({@code JBS-CERT-11}): después del contenido y antes de firmar, sin mezclarse con el
 * resultado del {@code DEValidator}.</p>
 *
 * <p>Inmutable y seguro entre hilos; los códigos desactivados salen de la configuración
 * ({@code sifen.validacion_reglas_desactivadas}).</p>
 */
public final class ConfigValidator {
    private static final ConfigValidator ESTANDAR = new ConfigValidator(null);
    /** Ficha de la configuración nula, la única que no reporta un conjunto. */
    private static final ValidationRule JBS_CFG_01 = RuleCatalog.byCode("JBS-CFG-01").orElseThrow();

    private final ValidationEngine<SifenConfig> motor;
    private final ValidationEngine<DocumentoElectronico> coherencia;
    private final Clock reloj;

    @SuppressWarnings("unchecked")
    private ConfigValidator(Clock reloj) {
        this.reloj = reloj;
        this.motor = ValidationEngine.de(new ArrayList<GroupValidator<SifenConfig>>(
                (List<? extends GroupValidator<SifenConfig>>) (List<?>) ConfigRuleSets.STANDARD), false, reloj,
                ValidationRule.CONFIG_ROOT, Set.of(), null);
        this.coherencia = ValidationEngine.de(new ArrayList<GroupValidator<DocumentoElectronico>>(
                (List<? extends GroupValidator<DocumentoElectronico>>) (List<?>) ConfigRuleSets.DE_CONSISTENCY),
                false, reloj, ValidationRule.DE_ROOT, Set.of(), null);
    }

    /**
     * El validador estándar, con el reloj del sistema en la hora de Asunción.
     *
     * @return el validador compartido.
     */
    public static ConfigValidator standard() {
        return ESTANDAR;
    }

    /**
     * Un validador con otro reloj (vigencia del certificado en pruebas o en un diagnóstico).
     *
     * @param reloj reloj; {@code null} = el del estándar.
     * @return el validador.
     */
    public static ConfigValidator withClock(Clock reloj) {
        return reloj == null ? ESTANDAR : new ConfigValidator(reloj);
    }

    /**
     * Valida la configuración con el certificado de la caché (recargado si cambió el archivo).
     *
     * @param cfg configuración; {@code null} da {@code JBS-CFG-01}.
     * @return el resultado; nunca {@code null} ni lanza.
     */
    public ValidationResult validate(SifenConfig cfg) {
        return validate(cfg, false);
    }

    /**
     * Valida la configuración.
     *
     * @param cfg                 configuración; {@code null} da {@code JBS-CFG-01}.
     * @param recargarCertificado {@code true} relee el certificado aunque el archivo no haya cambiado.
     * @return el resultado; nunca {@code null} ni lanza.
     */
    public ValidationResult validate(SifenConfig cfg, boolean recargarCertificado) {
        if (cfg == null) {
            ValidationContext ctx = new ValidationContext(null, reloj, Set.of(), null, ValidationRule.CONFIG_ROOT);
            ctx.report(JBS_CFG_01, ValidationRule.CONFIG_ROOT, null);
            return ctx.resultado();
        }
        if (recargarCertificado) {
            CertificateStore.invalidate(cfg);
        }
        return motor.validar(cfg, GenerationCtx.getDefaultFromConfig(cfg));
    }

    /**
     * Valida y lanza si hay errores.
     *
     * @param cfg configuración.
     * @throws SifenValidationException código legado {@code i101} ({@code getLegacyCode()}; {@code getCode()} es la
     *         primera regla con error), con todos los errores y avisos.
     */
    public void require(SifenConfig cfg) throws SifenValidationException {
        ValidationResult r = validate(cfg);
        if (!r.isValid()) {
            throw SifenValidationException.ofConfiguration(r);
        }
    }

    /**
     * Coherencia del DE con el certificado de la configuración ({@code JBS-CERT-11}).
     *
     * @param de  documento.
     * @param cfg configuración (de ella salen el certificado y los códigos desactivados).
     * @return el resultado; nunca {@code null} ni lanza.
     */
    public ValidationResult validateConsistency(DocumentoElectronico de, SifenConfig cfg) {
        return coherencia.validar(de, cfg == null ? null : GenerationCtx.getDefaultFromConfig(cfg));
    }

    /**
     * Valida la coherencia y lanza si hay errores.
     *
     * @param de  documento.
     * @param cfg configuración.
     * @throws SifenValidationException código legado {@code i405} ({@code getLegacyCode()}; {@code getCode()} es la
     *         primera regla con error), como la validación del contenido.
     */
    public void requireConsistency(DocumentoElectronico de, SifenConfig cfg) throws SifenValidationException {
        validateConsistency(de, cfg).throwIfInvalid();
    }

    /**
     * Devuelve los datos del certificado de la configuración (titular, número de documento, vigencia),
     * o la causa por la que no se pudo cargar.
     *
     * @param cfg configuración; {@code null} da {@code null}.
     * @return el resultado de la carga (de la caché si el archivo no cambió).
     */
    public static LoadedCertificate certificate(SifenConfig cfg) {
        return cfg == null ? null : CertificateStore.get(cfg);
    }

    /**
     * Devuelve los conjuntos de la configuración, en orden.
     *
     * @return lista inmodificable.
     */
    public List<RuleSet<SifenConfig, ?>> getRuleSets() {
        return ConfigRuleSets.STANDARD;
    }
}
