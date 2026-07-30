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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.naming.InitialContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javabeanstack.crypto.CipherUtil;
import org.javabeanstack.messaging.IMailAccount;
import org.javabeanstack.util.Fn;

/**
 * Implementación de {@link IMailSessionProvider}. Arma la sesión de correo a
 * partir de la cuenta que recibe; si la cuenta no define un servidor SMTP, cae a
 * la sesión del contenedor.
 *
 * <p><b>La configuración llega resuelta</b>: este módulo no sabe de dónde salió
 * —parámetros del sistema, una tabla por empresa o valores fijos— porque de eso
 * se ocupa la aplicación que lo usa. Acá solo se lee la {@link IMailAccount}.</p>
 *
 * <p><b>Sobre la clave</b>: {@link IMailAccount#getCipherKey()} es lo que define
 * cómo se interpreta la contraseña. Con llave, se descifra con
 * {@link CipherUtil}; sin llave, la contraseña no está cifrada y se usa tal
 * cual, dejando una advertencia en el log.</p>
 *
 * @author Jorge Enciso
 */
public class MailSessionProvider implements IMailSessionProvider {

    private static final Logger LOGGER = LogManager.getLogger(MailSessionProvider.class);

    /** Nombre JNDI por defecto de la sesión de correo del contenedor en WildFly. */
    public static final String DEFAULT_JNDI = "java:jboss/mail/Default";
    /** Puerto SMTP por defecto (submission con STARTTLS). */
    public static final int DEFAULT_SMTP_PORT = 587;

    /**
     * Milisegundos de espera para <b>establecer la conexión</b> con el servidor.
     *
     * <p><b>Sin un tiempo límite no hay envío que falle</b>: un servidor que
     * acepta la conexión y no responde deja el hilo esperando para siempre, y
     * con él a quien haya iniciado el envío. Puede ajustarse por cuenta con las
     * propiedades adicionales.</p>
     *
     * <p>Este valor es corto a propósito: un servidor que no acepta la conexión
     * en 15 segundos no la va a aceptar.</p>
     */
    public static final int DEFAULT_TIMEOUT_MS = 15000;

    /**
     * Milisegundos de espera para <b>leer o escribir</b> durante el diálogo SMTP,
     * bastante más generoso que el de conexión.
     *
     * <p>La diferencia no es cosmética: conectar es instantáneo o no ocurre,
     * pero la respuesta al {@code DATA} llega recién <b>después</b> de que el
     * servidor aceptó el mensaje completo, y del otro lado puede haber antivirus
     * o filtros que se toman su tiempo. Con un límite corto se corta la lectura
     * de una respuesta que iba a llegar, el mensaje <b>ya se entregó</b> y el
     * envío se reporta como fallido — un falso negativo verificado en
     * producción. Y como un fallo de envío puede bloquear un ingreso con segundo
     * factor, ese falso negativo deja gente afuera con el código ya enviado.</p>
     */
    public static final int DEFAULT_IO_TIMEOUT_MS = 60000;

    @Override
    public MailChannelStatus checkConfig(IMailAccount account) {
        List<String> falta = new ArrayList<>();
        if (account == null) {
            return new MailChannelStatus(MailChannelStatus.Mode.NONE,
                    Collections.singletonList("cuenta"), "No se recibio ninguna cuenta de correo");
        }
        // Una cuenta sin remitente no esta en condiciones de enviar. El mensaje
        // puede traer el suyo, pero si la cuenta no declara ninguno lo que hay
        // es una configuracion a medio cargar, y sin este control el fallo
        // aparece recien al enviar: el canal se habria declarado operativo.
        if (Fn.nvl(account.getFromAddress(), "").trim().isEmpty()) {
            falta.add("remitente");
        }
        String host = Fn.nvl(account.getSmtpHost(), "").trim();
        if (!host.isEmpty()) {
            if (Fn.nvl(account.getSmtpAuth(), false)) {
                if (Fn.nvl(account.getSmtpUser(), "").trim().isEmpty()) {
                    falta.add("usuario SMTP");
                }
                if (Fn.nvl(account.getSmtpPass(), "").trim().isEmpty()) {
                    falta.add("contrasenia SMTP");
                }
            }
            String detalle = "Servidor SMTP " + host + ":"
                    + Fn.nvl(account.getSmtpPort(), DEFAULT_SMTP_PORT)
                    + (falta.isEmpty() ? "" : ", falta: " + falta);
            return new MailChannelStatus(MailChannelStatus.Mode.PARAMS, falta, detalle);
        }
        // Modo JNDI. El valor por defecto del contenedor NO cuenta como
        // configuracion: existe siempre y apunta a un servidor local que
        // normalmente no hay, de modo que un descuido pareceria estar
        // configurado hasta que el primer envio falla.
        String jndi = Fn.nvl(account.getSessionJndi(), "").trim();
        if (jndi.isEmpty()) {
            return new MailChannelStatus(MailChannelStatus.Mode.NONE,
                    Arrays.asList("servidor SMTP", "sesion de correo"),
                    "La cuenta no declara servidor SMTP ni sesion de correo del contenedor");
        }
        // Declarar EXPLICITAMENTE el recurso por defecto del contenedor tampoco
        // cuenta como configuracion, y hay que decirlo aparte: el lookup de
        // java:jboss/mail/Default TIENE EXITO --existe siempre-- asi que sin
        // este control el canal se declararia operativo y el fallo recien
        // aparecería al enviar, contra localhost:25. Es el mismo descuido que
        // el campo vacio, escrito de otra manera.
        if (DEFAULT_JNDI.equalsIgnoreCase(jndi)) {
            return new MailChannelStatus(MailChannelStatus.Mode.NONE,
                    Arrays.asList("servidor SMTP", "sesion de correo"),
                    "La cuenta apunta a " + DEFAULT_JNDI + ", que es el recurso por defecto "
                    + "del contenedor y no una configuracion de correo: apunta a un servidor "
                    + "local que normalmente no existe");
        }
        try {
            Object obj = new InitialContext().lookup(jndi);
            if (!(obj instanceof Session)) {
                return new MailChannelStatus(MailChannelStatus.Mode.NONE,
                        Collections.singletonList("sesion de correo"),
                        "El recurso " + jndi + " existe pero no es una sesion de correo");
            }
        } catch (Exception e) {
            return new MailChannelStatus(MailChannelStatus.Mode.NONE,
                    Collections.singletonList("sesion de correo"),
                    "No se pudo resolver la sesion de correo " + jndi + ": " + e.getMessage());
        }
        return new MailChannelStatus(MailChannelStatus.Mode.JNDI, falta,
                "Sesion de correo del contenedor " + jndi
                + (falta.isEmpty() ? "" : ", falta: " + falta));
    }

    @Override
    public Session getSession(IMailAccount account) throws Exception {
        if (account == null) {
            throw new IllegalArgumentException("No se recibio la cuenta de correo");
        }
        String host = Fn.nvl(account.getSmtpHost(), "").trim();
        if (!host.isEmpty()) {
            return buildSmtpSession(account, host);
        }
        return lookupContainerSession(account);
    }

    /**
     * Arma la sesión SMTP a partir de los datos de la cuenta.
     *
     * @param account cuenta de correo con la configuración resuelta.
     * @param host servidor SMTP.
     * @return la sesión de correo.
     */
    protected Session buildSmtpSession(IMailAccount account, String host) {
        int port = Fn.nvl(account.getSmtpPort(), DEFAULT_SMTP_PORT);
        boolean starttls = Fn.nvl(account.getSmtpStartTls(), false);
        boolean ssl = Fn.nvl(account.getSmtpSsl(), false);
        boolean auth = Fn.nvl(account.getSmtpAuth(), false);

        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        if (ssl) {
            props.put("mail.smtp.ssl.enable", "true");
        }
        String sslTrust = Fn.nvl(account.getSmtpSslTrust(), "").trim();
        if (!sslTrust.isEmpty()) {
            props.put("mail.smtp.ssl.trust", sslTrust);
            // El trust explícito implica aceptar el certificado aunque su nombre no
            // coincida con el host (Angus Mail verifica la identidad por separado y
            // por defecto): sin esto el handshake falla con "No subject alternative
            // DNS name matching ... found".
            props.put("mail.smtp.ssl.checkserveridentity", "false");
        }
        props.put("mail.smtp.auth", String.valueOf(auth));
        // Tiempos límite del diálogo SMTP. Van antes de las propiedades
        // adicionales para que una cuenta pueda ajustarlos, pero nunca quedan
        // sin definir: un servidor que acepta la conexión y no contesta dejaría
        // el envío esperando indefinidamente.
        props.put("mail.smtp.connectiontimeout", String.valueOf(DEFAULT_TIMEOUT_MS));
        props.put("mail.smtp.timeout", String.valueOf(DEFAULT_IO_TIMEOUT_MS));
        props.put("mail.smtp.writetimeout", String.valueOf(DEFAULT_IO_TIMEOUT_MS));
        // Las propiedades adicionales se aplican al final, de modo que la cuenta
        // pueda ajustar cualquier valor del proveedor sin ampliar el contrato.
        Map<String, String> extra = account.getExtraProperties();
        if (extra != null) {
            for (Map.Entry<String, String> e : extra.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    props.put(e.getKey(), e.getValue());
                }
            }
        }

        if (auth) {
            final String user = account.getSmtpUser();
            final String pass = resolvePassword(account);
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
     * @param account cuenta de correo con la configuración resuelta.
     * @return la sesión del contenedor.
     * @throws Exception si no se encuentra la sesión en el JNDI.
     */
    protected Session lookupContainerSession(IMailAccount account) throws Exception {
        String jndi = Fn.nvl(account.getSessionJndi(), DEFAULT_JNDI);
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
     * <p>La presencia de {@link IMailAccount#getCipherKey()} es la que define
     * cómo se interpreta {@link IMailAccount#getSmtpPass()}:</p>
     *
     * <ul>
     * <li><b>Sin llave</b> (ausente o vacía): la contraseña <b>no está
     * cifrada</b> y se usa tal cual. Es una configuración válida, pero deja una
     * advertencia en el log, porque en una instalación real no es deseable.</li>
     * <li><b>Con llave</b>: la contraseña está cifrada y se descifra. Si el
     * descifrado falla, es un <b>error de configuración</b> y se informa como
     * tal.</li>
     * </ul>
     *
     * @param account cuenta de correo con la configuración resuelta.
     * @return contraseña en claro, o cadena vacía.
     * @throws IllegalStateException si hay llave y la contraseña no se puede
     * descifrar con ella.
     */
    protected String resolvePassword(IMailAccount account) {
        String pass = Fn.nvl(account.getSmtpPass(), "");
        String key = Fn.nvl(account.getCipherKey(), "").trim();
        if (key.isEmpty()) {
            LOGGER.warn("La cuenta de correo no tiene llave de cifrado: "
                    + "se usa la contrasenia SMTP sin descifrar.");
            return pass;
        }
        try {
            return CipherUtil.decryptAES_FromHex(pass, key);
        } catch (Exception e) {
            // No se vuelve a la contrasenia cifrada como si fuera clara: la
            // autenticacion fallaria igual, pero el error visible seria
            // "credencial rechazada", que apunta al lugar equivocado. Con llave
            // presente, un descifrado fallido es un error de configuracion.
            throw new IllegalStateException("No se pudo descifrar la contrasenia SMTP "
                    + "de la cuenta con la llave configurada: " + e.getMessage(), e);
        }
    }

}
