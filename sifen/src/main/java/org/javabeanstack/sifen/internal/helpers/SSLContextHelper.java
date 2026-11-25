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
package org.javabeanstack.sifen.internal.helpers;

import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.validation.ConfigValidator;

import javax.net.ssl.SSLContext;
import java.security.*;
import org.javabeanstack.sifen.core.validation.ValidationMessages;
import org.javabeanstack.sifen.internal.cert.CertificateStore;
import org.javabeanstack.sifen.internal.http.SifenConnection;
import org.javabeanstack.sifen.internal.http.TlsContext;

/**
 * Helper encargado de manejar la conexión SSL y los certificados.
 *
 * <p>Cambio de jbs-sifen (plan JBSSIF, §3.7; M0-13, E3-15): el PKCS#12 ya no se relee en cada firma
 * ni deja el archivo abierto; lo carga {@link CertificateStore}.</p>
 *
 * <p>§3.8 (D33 Parte C, I3-22): las fallas salen con el código de su regla ({@code JBS-CERT-*},
 * {@code JBS-CFG-12}) o {@code JBS-INT-00}; la línea base las emitía todas como {@code i100}
 * (aunque el método se llamaba {@code invalidSSLContext}, el {@code i200} no se construía nunca).</p>
 *
 * <p>Fase 5 (§5.2; E0-05, M3-64, E3-31): el contexto TLS ya no es estático ni se arma bajo
 * {@code synchronized}: es de cada {@link SifenConnection} ({@link TlsContext}), se rearma cuando cambia
 * el certificado y se descarta ante una falla de transporte. {@link #getContextFromConfig(SifenConfig)}
 * devuelve el de la conexión de esa configuración. El material para <b>firmar</b> sigue saliendo del
 * almacén compartido ({@link CertificateStore#shared()}): la firma no es del carril de conexión
 * (I5-02).</p>
 */
public class SSLContextHelper {
    // §10.3 (carril F, I10-14): el campo estático _sslContext, sin uso desde §5.2 (el contexto TLS es de
    // cada conexión), se retiró; las pruebas ya no lo vacían por reflexión (I5-01).

    public static SSLContext getContextFromConfig(SifenConfig sifenConfig) throws SifenException {
        return SifenConnection.of(sifenConfig).tlsContext(sifenConfig);
    }

    public static KeyStore getCertificateKeyStore(SifenConfig sifenConfig) throws SifenException {
        if (!isCertificateConfigurationValid(sifenConfig)) {
            // §3.8: la lista completa de la configuración (i101) con el código de cada regla; si las
            // reglas pertinentes están desactivadas, la que explica por qué no hay clave para firmar.
            ConfigValidator.standard().require(sifenConfig);
            String codigo = !sifenConfig.isUseClientCertificate() ? "JBS-CFG-12"
                    : sifenConfig.getClientCertificateType() != SifenConfig.ClientCertificateType.PFX ? "JBS-CERT-13"
                    : sifenConfig.getClientCertificate() == null ? "JBS-CERT-01" : "JBS-CERT-04";
            throw SifenException.ofRule(codigo, ValidationMessages.message(codigo, "—"),
                    "Configuración del certificado no establecida. No se puede obtener la clave para la firma.", null);
        }
        return material(sifenConfig).keyStore();
    }

    /**
     * Material del certificado de la configuración (§3.7), del almacén compartido: con la causa de la
     * regla {@code JBS-CERT-*} si no se pudo cargar, nunca con el texto de la excepción del JDK.
     *
     * @param sifenConfig configuración con el certificado.
     * @return el material.
     * @throws SifenException con el código de la regla ({@code JBS-CERT-01}…) si el certificado no se
     *                        pudo cargar.
     */
    public static CertificateStore.Material material(SifenConfig sifenConfig) throws SifenException {
        return TlsContext.requireMaterial(CertificateStore.shared(), sifenConfig);
    }

    private static boolean isCertificateConfigurationValid(SifenConfig sifenConfig) {
        return TlsContext.isClientCertificateConfigured(sifenConfig);
    }
}
