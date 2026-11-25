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
package org.javabeanstack.sifen.internal.http;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.exceptions.ErrorCode;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.validation.ValidationMessages;
import org.javabeanstack.sifen.core.validation.support.LoadedCertificate;
import org.javabeanstack.sifen.internal.cert.CertificateStore;

/**
 * Contexto TLS de una conexión (plan JBSSIF, §5.2; E0-05, M3-64, E3-31).
 *
 * <p>Reemplaza el {@code SSLContextHelper._sslContext} estático de la línea base, que se armaba una
 * vez para toda la JVM y nunca se invalidaba. Acá cada conexión tiene <b>su</b> contexto y <b>su</b>
 * {@link CertificateStore}:</p>
 * <ul>
 *   <li>el contexto se arma con el certificado de cliente de la configuración y se reutiliza mientras el
 *       almacén entregue el mismo certificado; si el archivo cambió (huella), se arma otro;</li>
 *   <li>{@link #invalidate(Holder)} lo descarta solo si sigue siendo el que falló
 *       ({@code compareAndSet}): no tira uno que otro hilo acaba de rearmar;</li>
 *   <li>si la configuración no usa certificado de cliente, se usa el contexto por omisión de la JVM,
 *       nunca el de otra configuración (E3-31: la línea base devolvía el último que se había armado).</li>
 * </ul>
 * <p>Sin {@code synchronized}: dos hilos que arman a la vez producen dos contextos equivalentes y queda
 * el último; ninguno espera al otro (M3-64).</p>
 */
public final class TlsContext {
    private static final Logger LOGGER = LogManager.getLogger(TlsContext.class);

    /** Contexto armado y el certificado con el que se armó ({@code null} = contexto de la JVM). */
    record Holder(SSLContext context, LoadedCertificate certificate) {
    }

    private final CertificateStore store = new CertificateStore();
    private final AtomicReference<Holder> current = new AtomicReference<>();

    TlsContext() {
    }

    /**
     * Devuelve el contexto para la configuración, armándolo si hace falta.
     *
     * @param cfg configuración.
     * @return el contexto y el certificado con el que se armó.
     * @throws SifenException {@code JBS-CERT-*} si el certificado no se puede cargar, o {@code JBS-INT-00}.
     */
    Holder get(SifenConfig cfg) throws SifenException {
        Holder h = current.get();
        if (!isClientCertificateConfigured(cfg)) {
            if (h != null && h.certificate() == null) {
                return h;
            }
            Holder n;
            try {
                LOGGER.info("Se utilizará un contexto SSL por defecto.");
                n = new Holder(SSLContext.getDefault(), null);
            } catch (NoSuchAlgorithmException e) {
                throw SifenException.of(ErrorCode.JBS_INT_00, "No se puede retornar el contexto SSL por defecto", e);
            }
            current.set(n);
            return n;
        }
        CertificateStore.Material material = requireMaterial(store, cfg);
        if (h != null && h.certificate() == material.info()) {
            return h;
        }
        LOGGER.info("Contexto SSL no cargado aún o certificado distinto. Empezando carga...");
        Holder n = new Holder(build(material, cfg), material.info());
        current.set(n);
        return n;
    }

    /**
     * Descarta el contexto si sigue siendo el indicado, y olvida los certificados cargados.
     *
     * @param failed el que se usó en el pedido que falló; {@code null} no descarta nada.
     * @return {@code true} si se descartó.
     */
    boolean invalidate(Holder failed) {
        if (failed != null && current.compareAndSet(failed, null)) {
            store.forgetAll();
            return true;
        }
        return false;
    }

    /** Descarta el contexto y los certificados, sea cual sea. */
    void clear() {
        current.set(null);
        store.forgetAll();
    }

    /**
     * Indica si hay un contexto armado (para las pruebas y el diagnóstico).
     *
     * @return {@code true} si hay.
     */
    boolean isBuilt() {
        return current.get() != null;
    }

    private static SSLContext build(CertificateStore.Material material, SifenConfig cfg) throws SifenException {
        KeyManagerFactory keyManagerFactory;
        try {
            keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            throw SifenException.of(ErrorCode.JBS_INT_00, "No se puede obtener una instancia de administrador de claves de algoritmo: "
                    + KeyManagerFactory.getDefaultAlgorithm(), e);
        }
        try {
            keyManagerFactory.init(material.keyStore(), cfg.getClientCertificatePassword().toCharArray());
        } catch (UnrecoverableKeyException e) {
            // #45 del inventario: la clave privada tiene otra contraseña que el almacén
            throw SifenException.ofRule("JBS-CERT-03", ValidationMessages.message("JBS-CERT-03"),
                    "No se puede inicializar el administrador de claves: " + e.getClass().getName(), e);
        } catch (KeyStoreException | NoSuchAlgorithmException e) {
            throw SifenException.of(ErrorCode.JBS_INT_00, "No se puede inicializar el administrador de claves", e);
        }
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            // confianza: el almacén por omisión de la JVM (javax.net.ssl.trustStore), como la línea base
            sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());
            return sslContext;
        } catch (KeyManagementException e) {
            throw SifenException.of(ErrorCode.JBS_INT_00, "No se inicializar el contexto SSL", e);
        } catch (NoSuchAlgorithmException e) {
            throw SifenException.of(ErrorCode.JBS_INT_00, "No se puede obtener una instancia de contexto SSL TLS", e);
        }
    }

    /**
     * Material del certificado de la configuración en un almacén (§3.7): con la causa de la regla
     * {@code JBS-CERT-*} si no se pudo cargar, nunca con el texto de la excepción del JDK.
     *
     * @param store almacén.
     * @param cfg   configuración con el certificado.
     * @return el material.
     * @throws SifenException con el código de la regla ({@code JBS-CERT-01}…) si el certificado no se
     *                        pudo cargar.
     */
    public static CertificateStore.Material requireMaterial(CertificateStore store, SifenConfig cfg) throws SifenException {
        CertificateStore.Material material = store.materialOf(cfg);
        if (material == null) {
            LoadedCertificate info = store.loaded(cfg);
            String codigo = info.getCause().getCode();
            String mensaje = ValidationMessages.message(codigo, info.isInline() ? "en línea" : info.getPath());
            throw SifenException.ofRule(codigo, mensaje, "No se puede cargar el certificado de cliente (" + codigo + ")", null);
        }
        return material;
    }

    /**
     * Indica si la configuración usa un certificado de cliente PFX con archivo y contraseña (la condición
     * de la línea base para armar el TLS mutuo).
     *
     * @param cfg configuración.
     * @return {@code true} si hay que armar el contexto con el certificado.
     */
    public static boolean isClientCertificateConfigured(SifenConfig cfg) {
        if (cfg != null && cfg.isUseClientCertificate()) {
            if (cfg.getClientCertificateType() == SifenConfig.ClientCertificateType.PFX) {
                if (cfg.getClientCertificate() == null || cfg.getClientCertificatePassword() == null) {
                    LOGGER.warn("Configuración del certificado no establecida.");
                    return false;
                }
                return true;
            } else {
                LOGGER.warn("Tipo de certificado aún no soportado");
                return false;
            }
        }
        return false;
    }
}
