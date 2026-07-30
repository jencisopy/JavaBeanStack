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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de la comprobación de configuración de una cuenta de correo.
 *
 * <p>Son unitarias puras: {@code checkConfig} no abre conexiones salvo en el
 * modo JNDI, que acá no se ejercita justamente para que no dependan de un
 * contenedor.</p>
 *
 * @author Jorge Enciso
 */
public class MailSessionProviderTest {

    private final MailSessionProvider provider = new MailSessionProvider();

    /**
     * Devuelve una cuenta con el transporte completo, para que cada prueba le
     * quite lo que quiere ejercitar.
     *
     * @return cuenta con servidor, credencial y remitente.
     */
    private MailAccount cuentaCompleta() {
        MailAccount cuenta = new MailAccount();
        cuenta.setSmtpHost("smtp.ejemplo.com");
        cuenta.setSmtpPort(587);
        cuenta.setSmtpAuth(true);
        cuenta.setSmtpUser("usuario");
        cuenta.setSmtpPass("clave");
        cuenta.setFromAddress("no-reply@ejemplo.com");
        cuenta.setFromName("Ejemplo");
        return cuenta;
    }

    /**
     * Una cuenta con servidor, credencial y remitente está en condiciones de
     * enviar.
     */
    @Test
    public void testCuentaCompleta() {
        MailChannelStatus estado = provider.checkConfig(cuentaCompleta());
        assertTrue(estado.isReady());
        assertEquals(MailChannelStatus.Mode.PARAMS, estado.getMode());
        assertTrue(estado.getMissing().isEmpty());
    }

    /**
     * Sin remitente la cuenta no está operativa, aunque el transporte esté
     * completo. Es lo que evita declarar operativo un canal cuyo primer envío
     * va a fallar.
     */
    @Test
    public void testFaltaRemitente() {
        MailAccount cuenta = cuentaCompleta();
        cuenta.setFromAddress(null);
        MailChannelStatus estado = provider.checkConfig(cuenta);
        assertFalse(estado.isReady());
        assertTrue(estado.getMissing().contains("remitente"));
    }

    /**
     * Si el servidor exige autenticación y falta la credencial, la cuenta no
     * está operativa y se dice exactamente qué falta.
     */
    @Test
    public void testFaltaCredencial() {
        MailAccount cuenta = cuentaCompleta();
        cuenta.setSmtpUser("");
        cuenta.setSmtpPass("");
        MailChannelStatus estado = provider.checkConfig(cuenta);
        assertFalse(estado.isReady());
        assertTrue(estado.getMissing().contains("usuario SMTP"));
        assertTrue(estado.getMissing().contains("contrasenia SMTP"));
    }

    /**
     * Una cuenta que no declara ni servidor ni sesión del contenedor no puede
     * obtener ninguna sesión. El valor por defecto del contenedor no se asume:
     * si se asumiera, una instalación sin configurar parecería operativa.
     */
    @Test
    public void testSinTransporte() {
        MailAccount cuenta = cuentaCompleta();
        cuenta.setSmtpHost(null);
        MailChannelStatus estado = provider.checkConfig(cuenta);
        assertFalse(estado.isReady());
        assertEquals(MailChannelStatus.Mode.NONE, estado.getMode());
    }

    /**
     * Una cuenta nula no rompe la comprobación.
     */
    @Test
    public void testCuentaNula() {
        MailChannelStatus estado = provider.checkConfig(null);
        assertFalse(estado.isReady());
        assertEquals(MailChannelStatus.Mode.NONE, estado.getMode());
    }

    /**
     * El recurso por defecto del contenedor NO cuenta como configuración,
     * aunque se lo declare explícitamente.
     *
     * <p>Es <b>la regla capital</b> de la decisión sobre prerrequisitos del
     * canal, y la que más fácil se pierde: el lookup de
     * {@code java:jboss/mail/Default} <b>tiene éxito</b> —ese recurso existe
     * siempre en WildFly—, así que sin este control el canal se declararía
     * operativo y el fallo aparecería recién al enviar, contra un
     * {@code localhost:25} que normalmente no existe. Un "correo operativo"
     * falso es peor que no informar nada.</p>
     */
    @Test
    public void testJndiPorDefectoDelContenedorNoCuenta() {
        MailAccount cuenta = cuentaCompleta();
        cuenta.setSmtpHost(null);
        cuenta.setSessionJndi(MailSessionProvider.DEFAULT_JNDI);
        MailChannelStatus estado = provider.checkConfig(cuenta);
        assertEquals(MailChannelStatus.Mode.NONE, estado.getMode());
        assertFalse(estado.isReady());
        assertTrue(estado.getMissing().contains("servidor SMTP"));
    }

    /**
     * La comparación del recurso por defecto no distingue mayúsculas: un
     * descuido de tipeo no debe convertirlo en configuración válida.
     */
    @Test
    public void testJndiPorDefectoEnOtrasMayusculas() {
        MailAccount cuenta = cuentaCompleta();
        cuenta.setSmtpHost("");
        cuenta.setSessionJndi(MailSessionProvider.DEFAULT_JNDI.toUpperCase());
        assertEquals(MailChannelStatus.Mode.NONE, provider.checkConfig(cuenta).getMode());
    }
}
