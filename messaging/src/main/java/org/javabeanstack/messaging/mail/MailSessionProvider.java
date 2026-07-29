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
package org.javabeanstack.messaging.mail;

import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import java.util.Properties;
import javax.naming.InitialContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javabeanstack.config.IAppConfig;
import org.javabeanstack.crypto.CipherUtil;
import org.javabeanstack.model.IAppSystemParam;
import org.javabeanstack.util.Fn;

/**
 * Implementación de {@link IMailSessionProvider}. Arma la sesión de correo desde
 * los parámetros globales del sistema (grupo {@code Messaging}); si no están
 * configurados, cae a la sesión del contenedor.
 *
 * <p><b>Sobre la clave</b>: si existe el parámetro {@link #PARAM_CIPHER_KEY}, la
 * contraseña SMTP se descifra con {@link CipherUtil}. Dónde vive definitivamente
 * esa llave es una decisión de la Fase 3; hasta entonces, si la llave no está
 * configurada la contraseña se usa tal cual (modo de desarrollo), con una
 * advertencia en el log.</p>
 *
 * @author Jorge Enciso
 */
public class MailSessionProvider implements IMailSessionProvider {

    private static final Logger LOGGER = LogManager.getLogger(MailSessionProvider.class);

    /** Servidor SMTP de salida. Si está vacío se usa la sesión del contenedor. */
    public static final String PARAM_SMTP_HOST = "MAIL_SMTP_HOST";
    /** Puerto del servidor SMTP. */
    public static final String PARAM_SMTP_PORT = "MAIL_SMTP_PORT";
    /** Indica si la conexión SMTP usa STARTTLS. */
    public static final String PARAM_SMTP_STARTTLS = "MAIL_SMTP_STARTTLS";
    /** Indica si la conexión SMTP usa SSL directo. */
    public static final String PARAM_SMTP_SSL = "MAIL_SMTP_SSL";
    /**
     * Lista de hosts SMTP cuyo certificado se acepta aunque no coincida con el
     * nombre (separada por espacios, o {@code *} para todos). Vacío = verificación
     * de identidad estándar. Necesario cuando el servidor propio presenta un
     * certificado emitido para otro nombre (p. ej. hosting compartido).
     */
    public static final String PARAM_SMTP_SSL_TRUST = "MAIL_SMTP_SSL_TRUST";
    /** Indica si el servidor SMTP exige autenticación. */
    public static final String PARAM_SMTP_AUTH = "MAIL_SMTP_AUTH";
    /** Usuario de autenticación SMTP. */
    public static final String PARAM_SMTP_USER = "MAIL_SMTP_USER";
    /** Contraseña de autenticación SMTP (cifrada si hay llave configurada). */
    public static final String PARAM_SMTP_PASS = "MAIL_SMTP_PASS";
    /** Llave para descifrar la contraseña SMTP (provisional, ver Fase 3). */
    public static final String PARAM_CIPHER_KEY = "MAIL_CIPHER_KEY";
    /** Nombre JNDI de la sesión de correo del contenedor. */
    public static final String PARAM_JNDI = "MAIL_SESSION_JNDI";

    /** Nombre JNDI por defecto de la sesión de correo del contenedor en WildFly. */
    public static final String DEFAULT_JNDI = "java:jboss/mail/Default";
    /** Puerto SMTP por defecto (submission con STARTTLS). */
    public static final int DEFAULT_SMTP_PORT = 587;

    @Override
    public Session getSession(IAppConfig appConfig, Long idcompany) throws Exception {
        String host = str(appConfig, PARAM_SMTP_HOST);
        if (!Fn.nvl(host, "").trim().isEmpty()) {
            return buildSmtpSession(appConfig, host.trim());
        }
        return lookupContainerSession(appConfig);
    }

    /**
     * Arma la sesión SMTP a partir de los parámetros globales.
     *
     * @param appConfig configuración de la aplicación.
     * @param host servidor SMTP.
     * @return la sesión de correo.
     */
    protected Session buildSmtpSession(IAppConfig appConfig, String host) {
        Integer port = Fn.nvl(num(appConfig, PARAM_SMTP_PORT), DEFAULT_SMTP_PORT);
        boolean starttls = bool(appConfig, PARAM_SMTP_STARTTLS);
        boolean ssl = bool(appConfig, PARAM_SMTP_SSL);
        boolean auth = bool(appConfig, PARAM_SMTP_AUTH);

        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        if (ssl) {
            props.put("mail.smtp.ssl.enable", "true");
        }
        String sslTrust = str(appConfig, PARAM_SMTP_SSL_TRUST);
        if (!Fn.nvl(sslTrust, "").trim().isEmpty()) {
            props.put("mail.smtp.ssl.trust", sslTrust.trim());
            // El trust explícito implica aceptar el certificado aunque su nombre no
            // coincida con el host (Angus Mail verifica la identidad por separado y
            // por defecto): sin esto el handshake falla con "No subject alternative
            // DNS name matching ... found".
            props.put("mail.smtp.ssl.checkserveridentity", "false");
        }
        props.put("mail.smtp.auth", String.valueOf(auth));

        if (auth) {
            final String user = str(appConfig, PARAM_SMTP_USER);
            final String pass = resolvePassword(appConfig);
            return Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            });
        }
        return Session.getInstance(props);
    }

    /**
     * Resuelve la sesión de correo del contenedor por JNDI.
     *
     * @param appConfig configuración de la aplicación.
     * @return la sesión del contenedor.
     * @throws Exception si no se encuentra la sesión en el JNDI.
     */
    protected Session lookupContainerSession(IAppConfig appConfig) throws Exception {
        String jndi = Fn.nvl(str(appConfig, PARAM_JNDI), DEFAULT_JNDI);
        Object obj = new InitialContext().lookup(jndi);
        if (!(obj instanceof Session)) {
            throw new IllegalStateException("El recurso JNDI " + jndi
                    + " no es una sesion de correo (jakarta.mail.Session)");
        }
        return (Session) obj;
    }

    /**
     * Devuelve la contraseña SMTP en claro.
     *
     * <p>La presencia de la llave {@link #PARAM_CIPHER_KEY} es la que define
     * cómo se interpreta {@link #PARAM_SMTP_PASS}:</p>
     *
     * <ul>
     * <li><b>Sin llave</b> (ausente o vacía): la contraseña <b>no está
     * cifrada</b> y se usa tal cual. Es una configuración válida —y la única
     * posible mientras no se defina dónde custodiar la llave—, pero deja una
     * advertencia en el log, porque en una instalación real no es deseable.</li>
     * <li><b>Con llave</b>: la contraseña está cifrada y se descifra. Si el
     * descifrado falla, es un <b>error de configuración</b> y se informa como
     * tal.</li>
     * </ul>
     *
     * @param appConfig configuración de la aplicación.
     * @return contraseña en claro, o cadena vacía.
     * @throws IllegalStateException si hay llave y la contraseña no se puede
     * descifrar con ella.
     */
    protected String resolvePassword(IAppConfig appConfig) {
        String pass = Fn.nvl(str(appConfig, PARAM_SMTP_PASS), "");
        String key = str(appConfig, PARAM_CIPHER_KEY);
        if (Fn.nvl(key, "").trim().isEmpty()) {
            LOGGER.warn("No hay llave de cifrado (" + PARAM_CIPHER_KEY
                    + "): se usa la contrasenia SMTP sin descifrar.");
            return pass;
        }
        try {
            return CipherUtil.decryptAES_FromHex(pass, key);
        } catch (Exception e) {
            // No se vuelve a la contrasenia cifrada como si fuera clara: la
            // autenticacion fallaria igual, pero el error visible seria
            // "credencial rechazada", que apunta al lugar equivocado. Con llave
            // presente, un descifrado fallido es un error de configuracion.
            throw new IllegalStateException("No se pudo descifrar la contrasenia SMTP con la llave "
                    + PARAM_CIPHER_KEY + ": " + e.getMessage(), e);
        }
    }

    private String str(IAppConfig appConfig, String param) {
        IAppSystemParam p = appConfig.getSystemParam(param);
        return (p == null) ? null : p.getValueChar();
    }

    private Integer num(IAppConfig appConfig, String param) {
        IAppSystemParam p = appConfig.getSystemParam(param);
        if (p == null || p.getValueNumber() == null) {
            return null;
        }
        return p.getValueNumber().intValue();
    }

    private boolean bool(IAppConfig appConfig, String param) {
        IAppSystemParam p = appConfig.getSystemParam(param);
        return p != null && Boolean.TRUE.equals(p.getValueBoolean());
    }
}
