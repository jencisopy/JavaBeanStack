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
package org.javabeanstack.messaging;

import java.util.Map;

/**
 * Contrato de la cuenta de correo de una empresa: los parámetros de salida
 * (SMTP) y de entrada (IMAP/POP3), la identidad con la que se firma el envío y
 * las credenciales. En Maker lo implementa {@code AppMsgMailCredential} sobre
 * {@code datos.AppMsgMailCredential}.
 *
 * <p><b>Seguridad</b>: {@link #getSmtpPass()} e {@link #getInPass()} devuelven el
 * valor <b>cifrado</b> tal como está en la base. El descifrado es
 * responsabilidad de la capa de servicio; la contraseña nunca se guarda ni se
 * expone en claro.</p>
 *
 * @author Jorge Enciso
 */
public interface IMailAccount {

    /** Protocolo de entrada IMAP. */
    String PROTOCOL_IMAP = "IMAP";
    /** Protocolo de entrada POP3. */
    String PROTOCOL_POP3 = "POP3";

    /**
     * Devuelve el identificador de la cuenta.
     * @return identificador de la cuenta.
     */
    Long getId();

    /**
     * Asigna el identificador de la cuenta.
     * @param id identificador de la cuenta.
     */
    void setId(Long id);

    /**
     * Devuelve el identificador de la empresa dueña de la cuenta.
     * @return identificador de la empresa.
     */
    Long getIdcompany();

    /**
     * Asigna el identificador de la empresa dueña de la cuenta.
     * @param idcompany identificador de la empresa.
     */
    void setIdcompany(Long idcompany);

    /**
     * Devuelve el código de la cuenta, único dentro de la empresa.
     * @return código de la cuenta.
     */
    String getCode();

    /**
     * Asigna el código de la cuenta.
     * @param code código de la cuenta.
     */
    void setCode(String code);

    /**
     * Devuelve el nombre descriptivo de la cuenta.
     * @return nombre de la cuenta.
     */
    String getName();

    /**
     * Asigna el nombre descriptivo de la cuenta.
     * @param name nombre de la cuenta.
     */
    void setName(String name);

    /**
     * Devuelve el canal de la cuenta.
     * @return canal ({@link IMessage#CHANNEL_MAIL} y demás).
     */
    String getChannel();

    /**
     * Asigna el canal de la cuenta.
     * @param channel canal de la cuenta.
     */
    void setChannel(String channel);

    /**
     * Devuelve la dirección con la que se firma el envío (cabecera {@code From}).
     * @return dirección de correo del remitente.
     */
    String getFromAddress();

    /**
     * Asigna la dirección con la que se firma el envío.
     * @param fromAddress dirección de correo del remitente.
     */
    void setFromAddress(String fromAddress);

    /**
     * Devuelve el nombre visible del remitente.
     * @return nombre del remitente, o nulo.
     */
    String getFromName();

    /**
     * Asigna el nombre visible del remitente.
     * @param fromName nombre del remitente.
     */
    void setFromName(String fromName);

    /**
     * Devuelve la dirección de respuesta por defecto de la cuenta.
     * @return dirección de respuesta, o nulo.
     */
    String getReplyTo();

    /**
     * Asigna la dirección de respuesta por defecto de la cuenta.
     * @param replyTo dirección de respuesta.
     */
    void setReplyTo(String replyTo);

    /**
     * Devuelve el servidor SMTP de salida.
     * @return host del servidor SMTP, o nulo si la cuenta usa la sesión del contenedor.
     */
    String getSmtpHost();

    /**
     * Asigna el servidor SMTP de salida.
     * @param smtpHost host del servidor SMTP.
     */
    void setSmtpHost(String smtpHost);

    /**
     * Devuelve el puerto del servidor SMTP.
     * @return puerto SMTP, o nulo.
     */
    Integer getSmtpPort();

    /**
     * Asigna el puerto del servidor SMTP.
     * @param smtpPort puerto SMTP.
     */
    void setSmtpPort(Integer smtpPort);

    /**
     * Indica si la conexión SMTP usa STARTTLS.
     * @return verdadero si usa STARTTLS.
     */
    Boolean getSmtpStartTls();

    /**
     * Asigna si la conexión SMTP usa STARTTLS.
     * @param smtpStartTls verdadero si usa STARTTLS.
     */
    void setSmtpStartTls(Boolean smtpStartTls);

    /**
     * Indica si la conexión SMTP usa SSL directo.
     * @return verdadero si usa SSL.
     */
    Boolean getSmtpSsl();

    /**
     * Asigna si la conexión SMTP usa SSL directo.
     * @param smtpSsl verdadero si usa SSL.
     */
    void setSmtpSsl(Boolean smtpSsl);

    /**
     * Indica si el servidor SMTP exige autenticación.
     * @return verdadero si exige autenticación.
     */
    Boolean getSmtpAuth();

    /**
     * Asigna si el servidor SMTP exige autenticación.
     * @param smtpAuth verdadero si exige autenticación.
     */
    void setSmtpAuth(Boolean smtpAuth);

    /**
     * Devuelve el usuario de autenticación SMTP.
     * @return usuario SMTP, o nulo.
     */
    String getSmtpUser();

    /**
     * Asigna el usuario de autenticación SMTP.
     * @param smtpUser usuario SMTP.
     */
    void setSmtpUser(String smtpUser);

    /**
     * Devuelve la contraseña de autenticación SMTP, <b>cifrada</b>.
     * @return contraseña SMTP cifrada, o nulo.
     */
    String getSmtpPass();

    /**
     * Asigna la contraseña de autenticación SMTP (debe recibirse cifrada).
     * @param smtpPass contraseña SMTP cifrada.
     */
    void setSmtpPass(String smtpPass);

    /**
     * Devuelve los servidores SMTP cuyo certificado se acepta aunque su nombre no
     * coincida con el host. Separados por espacios, o {@code *} para todos.
     *
     * <p>Hace falta cuando el servidor está en un hosting compartido y presenta
     * un certificado a nombre de otro host.</p>
     *
     * @return lista de hosts de confianza, o nulo.
     */
    String getSmtpSslTrust();

    /**
     * Asigna los servidores SMTP cuyo certificado se acepta aunque su nombre no
     * coincida con el host.
     * @param smtpSslTrust lista de hosts separados por espacios, o {@code *}.
     */
    void setSmtpSslTrust(String smtpSslTrust);

    /**
     * Devuelve la llave con la que se descifran {@link #getSmtpPass()} e
     * {@link #getInPass()}.
     *
     * <p><b>Su ausencia es significativa</b>: si es nula o vacía, las
     * contraseñas <b>no están cifradas</b> y se usan tal cual.</p>
     *
     * @return llave de cifrado, o nulo si las contraseñas están en claro.
     */
    String getCipherKey();

    /**
     * Asigna la llave con la que se descifran las contraseñas de la cuenta.
     * @param cipherKey llave de cifrado, o nulo si están en claro.
     */
    void setCipherKey(String cipherKey);

    /**
     * Devuelve el nombre JNDI de la sesión de correo del contenedor a usar
     * cuando la cuenta no define {@link #getSmtpHost()}.
     *
     * <p><b>Es configuración del despliegue, no de la empresa</b>: viaja en la
     * cuenta para que el proveedor de sesiones reciba todo lo que necesita en un
     * solo objeto, pero su valor no depende de quién sea el dueño de la cuenta.
     * Quien prefiera resolver el recurso por su cuenta puede omitirlo y usar
     * directamente el envío con una {@code Session} ya construida.</p>
     *
     * @return nombre JNDI de la sesión de correo, o nulo.
     */
    String getSessionJndi();

    /**
     * Asigna el nombre JNDI de la sesión de correo del contenedor.
     * @param sessionJndi nombre JNDI, o nulo.
     */
    void setSessionJndi(String sessionJndi);

    /**
     * Devuelve propiedades adicionales del transporte, que se aplican tal cual
     * sobre la sesión de correo.
     *
     * <p>Es la vía para usar cualquier propiedad del proveedor de correo que no
     * tenga un método propio en este contrato —un tiempo de espera, un proxy—
     * sin tener que ampliar la interfaz. Las claves son las del proveedor
     * ({@code mail.smtp.*}).</p>
     *
     * @return propiedades adicionales, o nulo si no hay.
     */
    Map<String, String> getExtraProperties();

    /**
     * Asigna propiedades adicionales del transporte.
     * @param extraProperties propiedades adicionales, o nulo.
     */
    void setExtraProperties(Map<String, String> extraProperties);

    /**
     * Devuelve el protocolo de entrada.
     * @return {@link #PROTOCOL_IMAP} o {@link #PROTOCOL_POP3}, o nulo si la cuenta
     *         no recibe correo.
     */
    String getProtocol();

    /**
     * Asigna el protocolo de entrada.
     * @param protocol protocolo de entrada.
     */
    void setProtocol(String protocol);

    /**
     * Devuelve el servidor de entrada (IMAP/POP3).
     * @return host del servidor de entrada, o nulo.
     */
    String getInHost();

    /**
     * Asigna el servidor de entrada.
     * @param inHost host del servidor de entrada.
     */
    void setInHost(String inHost);

    /**
     * Devuelve el puerto del servidor de entrada.
     * @return puerto de entrada, o nulo.
     */
    Integer getInPort();

    /**
     * Asigna el puerto del servidor de entrada.
     * @param inPort puerto de entrada.
     */
    void setInPort(Integer inPort);

    /**
     * Indica si la conexión de entrada usa SSL.
     * @return verdadero si usa SSL.
     */
    Boolean getInSsl();

    /**
     * Asigna si la conexión de entrada usa SSL.
     * @param inSsl verdadero si usa SSL.
     */
    void setInSsl(Boolean inSsl);

    /**
     * Devuelve el usuario de autenticación de entrada.
     * @return usuario de entrada, o nulo.
     */
    String getInUser();

    /**
     * Asigna el usuario de autenticación de entrada.
     * @param inUser usuario de entrada.
     */
    void setInUser(String inUser);

    /**
     * Devuelve la contraseña de autenticación de entrada, <b>cifrada</b>.
     * @return contraseña de entrada cifrada, o nulo.
     */
    String getInPass();

    /**
     * Asigna la contraseña de autenticación de entrada (debe recibirse cifrada).
     * @param inPass contraseña de entrada cifrada.
     */
    void setInPass(String inPass);

    /**
     * Devuelve la carpeta del servidor de entrada a sondear.
     * @return carpeta de entrada (por ejemplo {@code INBOX}), o nulo.
     */
    String getInFolder();

    /**
     * Asigna la carpeta del servidor de entrada a sondear.
     * @param inFolder carpeta de entrada.
     */
    void setInFolder(String inFolder);

    /**
     * Indica si los mensajes se borran del servidor tras descargarlos.
     * @return verdadero si se borran del servidor.
     */
    Boolean getInDeleteOnServer();

    /**
     * Asigna si los mensajes se borran del servidor tras descargarlos.
     * @param inDeleteOnServer verdadero si se borran del servidor.
     */
    void setInDeleteOnServer(Boolean inDeleteOnServer);

    /**
     * Indica si la cuenta es la usada por defecto en su canal para la empresa.
     * @return verdadero si es la cuenta por defecto.
     */
    Boolean getDefaultAccount();

    /**
     * Asigna si la cuenta es la usada por defecto.
     * @param defaultAccount verdadero si es la cuenta por defecto.
     */
    void setDefaultAccount(Boolean defaultAccount);

    /**
     * Indica si la cuenta está activa.
     * @return verdadero si está activa.
     */
    Boolean getActive();

    /**
     * Asigna si la cuenta está activa.
     * @param active verdadero si está activa.
     */
    void setActive(Boolean active);

    /**
     * Devuelve la cantidad máxima de intentos de envío antes de dar un mensaje
     * por fallado.
     * @return cantidad máxima de intentos.
     */
    Integer getMaxRetry();

    /**
     * Asigna la cantidad máxima de intentos de envío.
     * @param maxRetry cantidad máxima de intentos.
     */
    void setMaxRetry(Integer maxRetry);
}
