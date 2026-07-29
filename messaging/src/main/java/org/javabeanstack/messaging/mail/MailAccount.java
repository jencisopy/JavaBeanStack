/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
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

import java.util.Map;
import org.javabeanstack.messaging.IMailAccount;

/**
 * Cuenta de correo: los datos que el subsistema necesita para enviar.
 *
 * <p>Es un <b>portador de datos</b> y nada más: no sabe de dónde salen los
 * valores. Eso es deliberado — hoy la aplicación los lee de sus parámetros del
 * sistema y mañana puede leerlos de sus propias tablas, sin que el subsistema se
 * entere ni cambie.</p>
 *
 * <p>Los atributos son <b>modificables</b>, como declara el contrato. Una
 * versión inmutable que hiciera fallar sus propios setters cumpliría la interfaz
 * solo de nombre, y quien programe contra {@link IMailAccount} tiene derecho a
 * usarla completa.</p>
 *
 * @author Jorge Enciso
 */
public class MailAccount implements IMailAccount {

    private Long id;
    private Long idcompany;
    private String code;
    private String name;
    private String channel;
    private String fromAddress;
    private String fromName;
    private String replyTo;
    private String smtpHost;
    private Integer smtpPort;
    private Boolean smtpStartTls;
    private Boolean smtpSsl;
    private Boolean smtpAuth;
    private String smtpUser;
    private String smtpPass;
    private String smtpSslTrust;
    private String cipherKey;
    private String sessionJndi;
    private Map<String, String> extraProperties;
    private String protocol;
    private String inHost;
    private Integer inPort;
    private Boolean inSsl;
    private String inUser;
    private String inPass;
    private String inFolder;
    private Boolean inDeleteOnServer;
    private Boolean defaultAccount;
    private Boolean active;
    private Integer maxRetry;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long value) {
        this.id = value;
    }

    @Override
    public Long getIdcompany() {
        return idcompany;
    }

    @Override
    public void setIdcompany(Long value) {
        this.idcompany = value;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public void setCode(String value) {
        this.code = value;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String value) {
        this.name = value;
    }

    @Override
    public String getChannel() {
        return channel;
    }

    @Override
    public void setChannel(String value) {
        this.channel = value;
    }

    @Override
    public String getFromAddress() {
        return fromAddress;
    }

    @Override
    public void setFromAddress(String value) {
        this.fromAddress = value;
    }

    @Override
    public String getFromName() {
        return fromName;
    }

    @Override
    public void setFromName(String value) {
        this.fromName = value;
    }

    @Override
    public String getReplyTo() {
        return replyTo;
    }

    @Override
    public void setReplyTo(String value) {
        this.replyTo = value;
    }

    @Override
    public String getSmtpHost() {
        return smtpHost;
    }

    @Override
    public void setSmtpHost(String value) {
        this.smtpHost = value;
    }

    @Override
    public Integer getSmtpPort() {
        return smtpPort;
    }

    @Override
    public void setSmtpPort(Integer value) {
        this.smtpPort = value;
    }

    @Override
    public Boolean getSmtpStartTls() {
        return smtpStartTls;
    }

    @Override
    public void setSmtpStartTls(Boolean value) {
        this.smtpStartTls = value;
    }

    @Override
    public Boolean getSmtpSsl() {
        return smtpSsl;
    }

    @Override
    public void setSmtpSsl(Boolean value) {
        this.smtpSsl = value;
    }

    @Override
    public Boolean getSmtpAuth() {
        return smtpAuth;
    }

    @Override
    public void setSmtpAuth(Boolean value) {
        this.smtpAuth = value;
    }

    @Override
    public String getSmtpUser() {
        return smtpUser;
    }

    @Override
    public void setSmtpUser(String value) {
        this.smtpUser = value;
    }

    @Override
    public String getSmtpPass() {
        return smtpPass;
    }

    @Override
    public void setSmtpPass(String value) {
        this.smtpPass = value;
    }

    @Override
    public String getSmtpSslTrust() {
        return smtpSslTrust;
    }

    @Override
    public void setSmtpSslTrust(String value) {
        this.smtpSslTrust = value;
    }

    @Override
    public String getCipherKey() {
        return cipherKey;
    }

    @Override
    public void setCipherKey(String value) {
        this.cipherKey = value;
    }

    @Override
    public String getSessionJndi() {
        return sessionJndi;
    }

    @Override
    public void setSessionJndi(String value) {
        this.sessionJndi = value;
    }

    @Override
    public Map<String, String> getExtraProperties() {
        return extraProperties;
    }

    @Override
    public void setExtraProperties(Map<String, String> value) {
        this.extraProperties = value;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public void setProtocol(String value) {
        this.protocol = value;
    }

    @Override
    public String getInHost() {
        return inHost;
    }

    @Override
    public void setInHost(String value) {
        this.inHost = value;
    }

    @Override
    public Integer getInPort() {
        return inPort;
    }

    @Override
    public void setInPort(Integer value) {
        this.inPort = value;
    }

    @Override
    public Boolean getInSsl() {
        return inSsl;
    }

    @Override
    public void setInSsl(Boolean value) {
        this.inSsl = value;
    }

    @Override
    public String getInUser() {
        return inUser;
    }

    @Override
    public void setInUser(String value) {
        this.inUser = value;
    }

    @Override
    public String getInPass() {
        return inPass;
    }

    @Override
    public void setInPass(String value) {
        this.inPass = value;
    }

    @Override
    public String getInFolder() {
        return inFolder;
    }

    @Override
    public void setInFolder(String value) {
        this.inFolder = value;
    }

    @Override
    public Boolean getInDeleteOnServer() {
        return inDeleteOnServer;
    }

    @Override
    public void setInDeleteOnServer(Boolean value) {
        this.inDeleteOnServer = value;
    }

    @Override
    public Boolean getDefaultAccount() {
        return defaultAccount;
    }

    @Override
    public void setDefaultAccount(Boolean value) {
        this.defaultAccount = value;
    }

    @Override
    public Boolean getActive() {
        return active;
    }

    @Override
    public void setActive(Boolean value) {
        this.active = value;
    }

    @Override
    public Integer getMaxRetry() {
        return maxRetry;
    }

    @Override
    public void setMaxRetry(Integer value) {
        this.maxRetry = value;
    }

}
