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
import java.util.List;

/**
 * Resultado de comprobar si una cuenta de correo está en condiciones de enviar.
 *
 * <p>Responde una pregunta distinta de la que responde un envío fallido:
 * <b>si la cuenta está configurada</b>, no si el servidor contesta. Una cuenta
 * sin servidor ni sesión declarada nunca va a poder enviar y conviene saberlo
 * antes de ofrecerle al usuario una función que no existe; una cuenta bien
 * configurada contra un servidor caído es otro problema, que se ve al
 * intentar.</p>
 *
 * <p>La comprobación <b>no abre ninguna conexión de red</b> salvo, en el modo
 * {@link Mode#JNDI}, la resolución del recurso.</p>
 *
 * @author Jorge Enciso
 */
public class MailChannelStatus {

    /** Forma en que la cuenta obtiene la sesión de correo. */
    public enum Mode {
        /** Servidor SMTP declarado en la cuenta. */
        PARAMS,
        /** Sesión de correo del contenedor, declarada explícitamente. */
        JNDI,
        /** La cuenta no permite obtener ninguna sesión. */
        NONE
    }

    private final Mode mode;
    private final List<String> missing;
    private final String detail;

    /**
     * Construye el resultado.
     *
     * @param mode modo de obtención de la sesión.
     * @param missing datos que faltan para poder enviar.
     * @param detail explicación legible del estado.
     */
    MailChannelStatus(Mode mode, List<String> missing, String detail) {
        this.mode = mode;
        this.missing = (missing == null) ? Collections.emptyList()
                : Collections.unmodifiableList(missing);
        this.detail = detail;
    }

    /**
     * Indica si la cuenta está en condiciones de enviar.
     *
     * @return verdadero si no falta nada.
     */
    public boolean isReady() {
        return mode != Mode.NONE && missing.isEmpty();
    }

    /**
     * Devuelve cómo obtiene la sesión esta cuenta.
     *
     * @return el modo, o {@link Mode#NONE} si no puede obtener ninguna.
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Devuelve los datos que faltan para poder enviar.
     *
     * @return lista de nombres, vacía si no falta nada.
     */
    public List<String> getMissing() {
        return missing;
    }

    /**
     * Devuelve una explicación legible del estado, apta para un log o para
     * mostrarle a quien administra la instalación.
     *
     * @return el detalle del estado.
     */
    public String getDetail() {
        return detail;
    }

    @Override
    public String toString() {
        return "MailChannelStatus{ready=" + isReady() + ", mode=" + mode
                + ", missing=" + missing + ", detail=" + detail + "}";
    }
}
