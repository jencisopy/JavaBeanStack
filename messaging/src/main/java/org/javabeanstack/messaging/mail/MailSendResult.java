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

import java.util.ArrayList;
import java.util.List;
import org.javabeanstack.error.ErrorReg;
import org.javabeanstack.error.IErrorReg;

/**
 * Implementación de {@link IMailSendResult}. Se construye con las fábricas
 * {@link #ok(String)}, {@link #retryable(String)} y {@link #failed(String)}.
 *
 * @author Jorge Enciso
 */
public class MailSendResult implements IMailSendResult {

    private boolean successful;
    private boolean retryable;
    private String messageId;
    private IErrorReg errorReg;
    private final List<String> invalidAddresses = new ArrayList<>();

    /**
     * Constructor por defecto.
     */
    public MailSendResult() {
    }

    /**
     * Crea un resultado exitoso.
     *
     * @param messageId {@code Message-ID} asignado por el servidor.
     * @return resultado exitoso.
     */
    public static MailSendResult ok(String messageId) {
        MailSendResult r = new MailSendResult();
        r.successful = true;
        r.messageId = messageId;
        return r;
    }

    /**
     * Crea un resultado de error recuperable (conviene reintentar).
     *
     * @param message descripción del error.
     * @return resultado de error recuperable.
     */
    public static MailSendResult retryable(String message) {
        MailSendResult r = new MailSendResult();
        r.successful = false;
        r.retryable = true;
        r.errorReg = new ErrorReg(message, 0, "");
        return r;
    }

    /**
     * Crea un resultado de error definitivo (no conviene reintentar).
     *
     * @param message descripción del error.
     * @return resultado de error definitivo.
     */
    public static MailSendResult failed(String message) {
        MailSendResult r = new MailSendResult();
        r.successful = false;
        r.retryable = false;
        r.errorReg = new ErrorReg(message, 0, "");
        return r;
    }

    @Override
    public boolean isSuccessful() {
        return successful;
    }

    @Override
    public boolean isRetryable() {
        return retryable;
    }

    @Override
    public String getMessageId() {
        return messageId;
    }

    @Override
    public IErrorReg getErrorReg() {
        return errorReg;
    }

    @Override
    public List<String> getInvalidAddresses() {
        return invalidAddresses;
    }

    /**
     * Agrega una dirección rechazada por el servidor.
     *
     * @param address dirección inválida.
     */
    public void addInvalidAddress(String address) {
        invalidAddresses.add(address);
    }
}
