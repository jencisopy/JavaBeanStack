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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.javabeanstack.messaging.IMailAccount;

/**
 * Implementación simple de {@link IMailAccount} para quien necesite armar una
 * cuenta de correo sin tener una entidad que la represente.
 *
 * <p>Se construye con {@link MailAccountBuilder} y, una vez construida, no se
 * modifica: los {@code set} del contrato están implementados pero lanzan. La
 * razón es práctica — una cuenta que cambia mientras se está armando una sesión
 * de correo es una fuente de errores difíciles de rastrear.</p>
 *
 * <p><b>Las contraseñas se guardan tal como llegan.</b> Si vienen cifradas, acá
 * siguen cifradas; el descifrado ocurre más adelante, en
 * {@link MailSessionProvider}, usando {@link #getCipherKey()}.</p>
 *
 * @author Jorge Enciso
 */
public class MailAccount implements IMailAccount {

    private final Long id;
    private final Long idcompany;
    private final String code;
    private final String name;
    private final String channel;
    private final String fromAddress;
    private final String fromName;
    private final String replyTo;
    private final String smtpHost;
    private final Integer smtpPort;
    private final Boolean smtpStartTls;
    private final Boolean smtpSsl;
    private final Boolean smtpAuth;
    private final String smtpUser;
    private final String smtpPass;
    private final String smtpSslTrust;
    private final String cipherKey;
    private final String sessionJndi;
    private final String protocol;
    private final String inHost;
    private final Integer inPort;
    private final Boolean inSsl;
    private final String inUser;
    private final String inPass;
    private final String inFolder;
    private final Boolean inDeleteOnServer;
    private final Boolean defaultAccount;
    private final Boolean active;
    private final Integer maxRetry;
    private final Map<String, String> extraProperties;

    /**
     * Construye la cuenta a partir de los valores acumulados en el
     * constructor fluido.
     *
     * @param b constructor con los valores de la cuenta.
     */
    MailAccount(MailAccountBuilder b) {
        this.id = b.id;
        this.idcompany = b.idcompany;
        this.code = b.code;
        this.name = b.name;
        this.channel = b.channel;
        this.fromAddress = b.fromAddress;
        this.fromName = b.fromName;
        this.replyTo = b.replyTo;
        this.smtpHost = b.smtpHost;
        this.smtpPort = b.smtpPort;
        this.smtpStartTls = b.smtpStartTls;
        this.smtpSsl = b.smtpSsl;
        this.smtpAuth = b.smtpAuth;
        this.smtpUser = b.smtpUser;
        this.smtpPass = b.smtpPass;
        this.smtpSslTrust = b.smtpSslTrust;
        this.cipherKey = b.cipherKey;
        this.sessionJndi = b.sessionJndi;
        this.protocol = b.protocol;
        this.inHost = b.inHost;
        this.inPort = b.inPort;
        this.inSsl = b.inSsl;
        this.inUser = b.inUser;
        this.inPass = b.inPass;
        this.inFolder = b.inFolder;
        this.inDeleteOnServer = b.inDeleteOnServer;
        this.defaultAccount = b.defaultAccount;
        this.active = b.active;
        this.maxRetry = b.maxRetry;
        this.extraProperties = (b.extraProperties == null || b.extraProperties.isEmpty())
                ? null : Collections.unmodifiableMap(new LinkedHashMap<>(b.extraProperties));
    }

    /**
     * Devuelve un constructor vacío.
     *
     * @return constructor de cuentas de correo.
     */
    public static MailAccountBuilder create() {
        return new MailAccountBuilder();
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public Long getIdcompany() {
        return idcompany;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getChannel() {
        return channel;
    }

    @Override
    public String getFromAddress() {
        return fromAddress;
    }

    @Override
    public String getFromName() {
        return fromName;
    }

    @Override
    public String getReplyTo() {
        return replyTo;
    }

    @Override
    public String getSmtpHost() {
        return smtpHost;
    }

    @Override
    public Integer getSmtpPort() {
        return smtpPort;
    }

    @Override
    public Boolean getSmtpStartTls() {
        return smtpStartTls;
    }

    @Override
    public Boolean getSmtpSsl() {
        return smtpSsl;
    }

    @Override
    public Boolean getSmtpAuth() {
        return smtpAuth;
    }

    @Override
    public String getSmtpUser() {
        return smtpUser;
    }

    @Override
    public String getSmtpPass() {
        return smtpPass;
    }

    @Override
    public String getSmtpSslTrust() {
        return smtpSslTrust;
    }

    @Override
    public String getCipherKey() {
        return cipherKey;
    }

    @Override
    public String getSessionJndi() {
        return sessionJndi;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public String getInHost() {
        return inHost;
    }

    @Override
    public Integer getInPort() {
        return inPort;
    }

    @Override
    public Boolean getInSsl() {
        return inSsl;
    }

    @Override
    public String getInUser() {
        return inUser;
    }

    @Override
    public String getInPass() {
        return inPass;
    }

    @Override
    public String getInFolder() {
        return inFolder;
    }

    @Override
    public Boolean getInDeleteOnServer() {
        return inDeleteOnServer;
    }

    @Override
    public Boolean getDefaultAccount() {
        return defaultAccount;
    }

    @Override
    public Boolean getActive() {
        return active;
    }

    @Override
    public Integer getMaxRetry() {
        return maxRetry;
    }

    @Override
    public Map<String, String> getExtraProperties() {
        return extraProperties;
    }

    @Override
    public void setId(Long id) {
        throw inmutable();
    }

    @Override
    public void setIdcompany(Long idcompany) {
        throw inmutable();
    }

    @Override
    public void setCode(String code) {
        throw inmutable();
    }

    @Override
    public void setName(String name) {
        throw inmutable();
    }

    @Override
    public void setChannel(String channel) {
        throw inmutable();
    }

    @Override
    public void setFromAddress(String fromAddress) {
        throw inmutable();
    }

    @Override
    public void setFromName(String fromName) {
        throw inmutable();
    }

    @Override
    public void setReplyTo(String replyTo) {
        throw inmutable();
    }

    @Override
    public void setSmtpHost(String smtpHost) {
        throw inmutable();
    }

    @Override
    public void setSmtpPort(Integer smtpPort) {
        throw inmutable();
    }

    @Override
    public void setSmtpStartTls(Boolean smtpStartTls) {
        throw inmutable();
    }

    @Override
    public void setSmtpSsl(Boolean smtpSsl) {
        throw inmutable();
    }

    @Override
    public void setSmtpAuth(Boolean smtpAuth) {
        throw inmutable();
    }

    @Override
    public void setSmtpUser(String smtpUser) {
        throw inmutable();
    }

    @Override
    public void setSmtpPass(String smtpPass) {
        throw inmutable();
    }

    @Override
    public void setSmtpSslTrust(String smtpSslTrust) {
        throw inmutable();
    }

    @Override
    public void setCipherKey(String cipherKey) {
        throw inmutable();
    }

    @Override
    public void setSessionJndi(String sessionJndi) {
        throw inmutable();
    }

    @Override
    public void setProtocol(String protocol) {
        throw inmutable();
    }

    @Override
    public void setInHost(String inHost) {
        throw inmutable();
    }

    @Override
    public void setInPort(Integer inPort) {
        throw inmutable();
    }

    @Override
    public void setInSsl(Boolean inSsl) {
        throw inmutable();
    }

    @Override
    public void setInUser(String inUser) {
        throw inmutable();
    }

    @Override
    public void setInPass(String inPass) {
        throw inmutable();
    }

    @Override
    public void setInFolder(String inFolder) {
        throw inmutable();
    }

    @Override
    public void setInDeleteOnServer(Boolean inDeleteOnServer) {
        throw inmutable();
    }

    @Override
    public void setDefaultAccount(Boolean defaultAccount) {
        throw inmutable();
    }

    @Override
    public void setActive(Boolean active) {
        throw inmutable();
    }

    @Override
    public void setMaxRetry(Integer maxRetry) {
        throw inmutable();
    }

    @Override
    public void setExtraProperties(Map<String, String> extraProperties) {
        throw inmutable();
    }

    private UnsupportedOperationException inmutable() {
        return new UnsupportedOperationException(
                "MailAccount no se modifica una vez construida: use MailAccount.create()");
    }
}
