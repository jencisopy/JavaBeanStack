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

import java.util.LinkedHashMap;
import java.util.Map;
import org.javabeanstack.config.IAppConfig;
import org.javabeanstack.model.IAppSystemParam;

/**
 * Constructor de {@link MailAccount}.
 *
 * <p>Además del armado a mano, ofrece {@link #fromAppConfig(IAppConfig)} para
 * las aplicaciones que guardan la configuración de correo en sus parámetros de
 * sistema. <b>Es un camino de conveniencia, no el único</b>: una aplicación que
 * resuelva la configuración de otra manera —una tabla por empresa, un archivo,
 * valores fijos— arma la cuenta con los métodos de este constructor y el
 * subsistema de correo funciona igual.</p>
 *
 * <p><b>Por qué el conocimiento de los parámetros vive acá y no en el
 * proveedor de sesiones</b>: así {@link MailSessionProvider} y
 * {@link MailSender} no dependen de dónde salió la configuración. Quien no use
 * {@code IAppConfig} simplemente no llama a este método, y nada del envío
 * arrastra esa dependencia.</p>
 *
 * @author Jorge Enciso
 */
public class MailAccountBuilder {

    /** Servidor SMTP de salida. */
    public static final String PARAM_SMTP_HOST = "MAIL_SMTP_HOST";
    /** Puerto del servidor SMTP. */
    public static final String PARAM_SMTP_PORT = "MAIL_SMTP_PORT";
    /** Indica si la conexión SMTP usa STARTTLS. */
    public static final String PARAM_SMTP_STARTTLS = "MAIL_SMTP_STARTTLS";
    /** Indica si la conexión SMTP usa SSL directo. */
    public static final String PARAM_SMTP_SSL = "MAIL_SMTP_SSL";
    /** Hosts SMTP cuyo certificado se acepta aunque su nombre no coincida. */
    public static final String PARAM_SMTP_SSL_TRUST = "MAIL_SMTP_SSL_TRUST";
    /** Indica si el servidor SMTP exige autenticación. */
    public static final String PARAM_SMTP_AUTH = "MAIL_SMTP_AUTH";
    /** Usuario de autenticación SMTP. */
    public static final String PARAM_SMTP_USER = "MAIL_SMTP_USER";
    /** Contraseña de autenticación SMTP (cifrada si hay llave). */
    public static final String PARAM_SMTP_PASS = "MAIL_SMTP_PASS";
    /** Llave con la que se descifra la contraseña SMTP. */
    public static final String PARAM_CIPHER_KEY = "MAIL_CIPHER_KEY";
    /** Nombre JNDI de la sesión de correo del contenedor. */
    public static final String PARAM_SESSION_JNDI = "MAIL_SESSION_JNDI";
    /** Dirección con la que se firma el envío. */
    public static final String PARAM_FROM_ADDRESS = "MAIL_FROM_ADDRESS";
    /** Nombre visible del remitente. */
    public static final String PARAM_FROM_NAME = "MAIL_FROM_NAME";

    Long id;
    Long idcompany;
    String code;
    String name;
    String channel;
    String fromAddress;
    String fromName;
    String replyTo;
    String smtpHost;
    Integer smtpPort;
    Boolean smtpStartTls;
    Boolean smtpSsl;
    Boolean smtpAuth;
    String smtpUser;
    String smtpPass;
    String smtpSslTrust;
    String cipherKey;
    String sessionJndi;
    String protocol;
    String inHost;
    Integer inPort;
    Boolean inSsl;
    String inUser;
    String inPass;
    String inFolder;
    Boolean inDeleteOnServer;
    Boolean defaultAccount;
    Boolean active;
    Integer maxRetry;
    Map<String, String> extraProperties;

    /**
     * Arma una cuenta leyendo los parámetros de sistema de la aplicación.
     *
     * <p>La contraseña y la llave se copian <b>tal como están</b>: si la
     * contraseña está cifrada, sigue cifrada. Descifrarla es tarea del
     * proveedor de sesiones, al momento de usarla.</p>
     *
     * @param appConfig configuración de la aplicación.
     * @param idcompany empresa a la que corresponde la cuenta, o nulo. No se usa
     * para leer —estos parámetros son globales— pero queda registrado en la
     * cuenta.
     * @return constructor con los valores tomados de la configuración.
     */
    public static MailAccountBuilder fromAppConfig(IAppConfig appConfig, Long idcompany) {
        MailAccountBuilder b = new MailAccountBuilder();
        b.idcompany = idcompany;
        b.smtpHost = str(appConfig, PARAM_SMTP_HOST);
        b.smtpPort = num(appConfig, PARAM_SMTP_PORT);
        b.smtpStartTls = bool(appConfig, PARAM_SMTP_STARTTLS);
        b.smtpSsl = bool(appConfig, PARAM_SMTP_SSL);
        b.smtpSslTrust = str(appConfig, PARAM_SMTP_SSL_TRUST);
        b.smtpAuth = bool(appConfig, PARAM_SMTP_AUTH);
        b.smtpUser = str(appConfig, PARAM_SMTP_USER);
        b.smtpPass = str(appConfig, PARAM_SMTP_PASS);
        b.cipherKey = str(appConfig, PARAM_CIPHER_KEY);
        b.sessionJndi = str(appConfig, PARAM_SESSION_JNDI);
        b.fromAddress = str(appConfig, PARAM_FROM_ADDRESS);
        b.fromName = str(appConfig, PARAM_FROM_NAME);
        return b;
    }

    /**
     * Construye la cuenta con los valores acumulados.
     *
     * @return la cuenta de correo.
     */
    public MailAccount build() {
        return new MailAccount(this);
    }

    /**
     * Asigna el identificador de la cuenta.
     * @param value identificador.
     * @return este constructor.
     */
    public MailAccountBuilder id(Long value) {
        this.id = value;
        return this;
    }

    /**
     * Asigna la empresa dueña de la cuenta.
     * @param value identificador de la empresa.
     * @return este constructor.
     */
    public MailAccountBuilder idcompany(Long value) {
        this.idcompany = value;
        return this;
    }

    /**
     * Asigna el código de la cuenta.
     * @param value código.
     * @return este constructor.
     */
    public MailAccountBuilder code(String value) {
        this.code = value;
        return this;
    }

    /**
     * Asigna el nombre descriptivo de la cuenta.
     * @param value nombre.
     * @return este constructor.
     */
    public MailAccountBuilder name(String value) {
        this.name = value;
        return this;
    }

    /**
     * Asigna el canal de la cuenta.
     * @param value canal.
     * @return este constructor.
     */
    public MailAccountBuilder channel(String value) {
        this.channel = value;
        return this;
    }

    /**
     * Asigna el remitente del envío.
     * @param address dirección del remitente.
     * @param name nombre visible del remitente.
     * @return este constructor.
     */
    public MailAccountBuilder from(String address, String name) {
        this.fromAddress = address;
        this.fromName = name;
        return this;
    }

    /**
     * Asigna la dirección de respuesta.
     * @param value dirección de respuesta.
     * @return este constructor.
     */
    public MailAccountBuilder replyTo(String value) {
        this.replyTo = value;
        return this;
    }

    /**
     * Asigna el servidor SMTP y su puerto.
     * @param host servidor SMTP.
     * @param port puerto, o nulo para el valor por defecto.
     * @return este constructor.
     */
    public MailAccountBuilder smtp(String host, Integer port) {
        this.smtpHost = host;
        this.smtpPort = port;
        return this;
    }

    /**
     * Asigna el modo de cifrado del transporte.
     * @param startTls verdadero si eleva la conexión con STARTTLS.
     * @param ssl verdadero si usa SSL desde el inicio.
     * @return este constructor.
     */
    public MailAccountBuilder security(Boolean startTls, Boolean ssl) {
        this.smtpStartTls = startTls;
        this.smtpSsl = ssl;
        return this;
    }

    /**
     * Asigna los hosts cuyo certificado se acepta aunque el nombre no coincida.
     * @param value lista separada por espacios, o {@code *}.
     * @return este constructor.
     */
    public MailAccountBuilder smtpSslTrust(String value) {
        this.smtpSslTrust = value;
        return this;
    }

    /**
     * Asigna las credenciales de autenticación SMTP.
     * @param user usuario.
     * @param pass contraseña, cifrada si hay llave.
     * @return este constructor.
     */
    public MailAccountBuilder auth(String user, String pass) {
        this.smtpAuth = Boolean.TRUE;
        this.smtpUser = user;
        this.smtpPass = pass;
        return this;
    }

    /**
     * Asigna la llave con la que se descifran las contraseñas.
     * @param value llave, o nulo si están en claro.
     * @return este constructor.
     */
    public MailAccountBuilder cipherKey(String value) {
        this.cipherKey = value;
        return this;
    }

    /**
     * Asigna la sesión de correo del contenedor a usar.
     * @param value nombre JNDI.
     * @return este constructor.
     */
    public MailAccountBuilder sessionJndi(String value) {
        this.sessionJndi = value;
        return this;
    }

    /**
     * Asigna los datos del servidor de entrada.
     * @param protocol protocolo ({@code IMAP} o {@code POP3}).
     * @param host servidor de entrada.
     * @param port puerto de entrada.
     * @param ssl verdadero si usa SSL.
     * @return este constructor.
     */
    public MailAccountBuilder in(String protocol, String host, Integer port, Boolean ssl) {
        this.protocol = protocol;
        this.inHost = host;
        this.inPort = port;
        this.inSsl = ssl;
        return this;
    }

    /**
     * Asigna las credenciales del servidor de entrada.
     * @param user usuario.
     * @param pass contraseña, cifrada si hay llave.
     * @return este constructor.
     */
    public MailAccountBuilder inAuth(String user, String pass) {
        this.inUser = user;
        this.inPass = pass;
        return this;
    }

    /**
     * Asigna la carpeta a sondear y si los mensajes se borran del servidor.
     * @param folder carpeta de entrada.
     * @param deleteOnServer verdadero si se borran tras descargarlos.
     * @return este constructor.
     */
    public MailAccountBuilder inFolder(String folder, Boolean deleteOnServer) {
        this.inFolder = folder;
        this.inDeleteOnServer = deleteOnServer;
        return this;
    }

    /**
     * Asigna si la cuenta es la usada por defecto en su canal.
     * @param value verdadero si es la cuenta por defecto.
     * @return este constructor.
     */
    public MailAccountBuilder defaultAccount(Boolean value) {
        this.defaultAccount = value;
        return this;
    }

    /**
     * Asigna si la cuenta está activa.
     * @param value verdadero si está activa.
     * @return este constructor.
     */
    public MailAccountBuilder active(Boolean value) {
        this.active = value;
        return this;
    }

    /**
     * Asigna la cantidad máxima de intentos de envío.
     * @param value cantidad máxima de intentos.
     * @return este constructor.
     */
    public MailAccountBuilder maxRetry(Integer value) {
        this.maxRetry = value;
        return this;
    }

    /**
     * Agrega una propiedad adicional del transporte.
     * @param key clave de la propiedad ({@code mail.smtp.*}).
     * @param value valor de la propiedad.
     * @return este constructor.
     */
    public MailAccountBuilder property(String key, String value) {
        if (extraProperties == null) {
            extraProperties = new LinkedHashMap<>();
        }
        extraProperties.put(key, value);
        return this;
    }

    private static String str(IAppConfig appConfig, String param) {
        IAppSystemParam p = appConfig.getSystemParam(param);
        return (p == null) ? null : p.getValueChar();
    }

    private static Integer num(IAppConfig appConfig, String param) {
        IAppSystemParam p = appConfig.getSystemParam(param);
        if (p == null || p.getValueNumber() == null) {
            return null;
        }
        return p.getValueNumber().intValue();
    }

    private static Boolean bool(IAppConfig appConfig, String param) {
        IAppSystemParam p = appConfig.getSystemParam(param);
        return (p == null) ? null : Boolean.TRUE.equals(p.getValueBoolean());
    }
}
