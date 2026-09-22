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
package org.javabeanstack.data;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Compatibilidad de la cabecera de auditoría entre versiones del framework y
 * del modelo de la aplicación.
 *
 * <p>
 * {@code AbstractDAO.auditSave} escribe {@code iprequest} y {@code device}
 * solo si el atributo existe en la clase de auditoría. Esta prueba reproduce
 * las dos formas posibles de esa clase —la vieja, con un único atributo
 * {@code ipRequest} sobre la columna {@code maquina}, y la nueva, con
 * {@code iprequest} y {@code device} separados— y verifica que la decisión y
 * el destino del valor sean los esperados en cada una. Es lo que permite
 * desplegar el framework y la aplicación en cualquier orden.
 * </p>
 *
 * @author Jorge Enciso
 */
public class AuditHeaderCompatTest {

    @Test
    @DisplayName("Modelo viejo: existe iprequest (por el nombre sin distinguir mayúsculas) y no existe device")
    public void modeloViejo() {
        assertTrue(DataInfo.isFieldExist(AuditViejo.class, "iprequest"),
                "el atributo ipRequest tiene que resolver por 'iprequest'");
        assertFalse(DataInfo.isFieldExist(AuditViejo.class, "device"),
                "el modelo viejo no tiene atributo device");
    }

    @Test
    @DisplayName("Modelo nuevo: existen los dos atributos")
    public void modeloNuevo() {
        assertTrue(DataInfo.isFieldExist(AuditNuevo.class, "iprequest"));
        assertTrue(DataInfo.isFieldExist(AuditNuevo.class, "device"));
    }

    @Test
    @DisplayName("Modelo viejo: la IP sigue yendo a la columna maquina, como antes")
    public void modeloViejoEscribeEnMaquina() throws Exception {
        AuditViejo row = new AuditViejo();
        row.setValue("iprequest", "127.0.0.1");
        assertEquals("127.0.0.1", row.getIpRequest());
        assertEquals("127.0.0.1", row.getValue("iprequest"));
    }

    @Test
    @DisplayName("Modelo nuevo: la IP va a iprequest y el dispositivo a maquina, sin pisarse")
    public void modeloNuevoSeparaLosDos() throws Exception {
        AuditNuevo row = new AuditNuevo();
        String ip = "181.91.86.240";
        row.setValue("iprequest", ip);
        row.setValue("device", AuditHeader.formatDevice("tablet-vendedor-guido", ip));
        assertEquals(ip, row.getIprequest());
        assertEquals("tablet-vendedor-guido (181.91.86.240)", row.getDevice());
    }

    @Test
    @DisplayName("Sin origen se graba null en los dos campos, no cadena vacía")
    public void sinOrigenGrabaNull() throws Exception {
        AuditNuevo row = new AuditNuevo();
        row.setValue("iprequest", null);
        row.setValue("device", AuditHeader.formatDevice("", ""));
        assertNull(row.getIprequest());
        assertNull(row.getDevice());
    }

    /** Forma vieja de una clase de auditoría: ipRequest sobre la columna maquina. */
    public static class AuditViejo extends DataRow {

        @Id
        private Long idaudit = 1L;

        @Column(name = "maquina")
        private String ipRequest;

        public Long getIdaudit() {
            return idaudit;
        }

        public void setIdaudit(Long idaudit) {
            this.idaudit = idaudit;
        }

        public String getIpRequest() {
            return ipRequest;
        }

        public void setIpRequest(String ipRequest) {
            this.ipRequest = ipRequest;
        }
    }

    /** Forma nueva: iprequest sobre su columna y device sobre maquina. */
    public static class AuditNuevo extends DataRow {

        @Id
        private Long idaudit = 1L;

        @Column(name = "iprequest")
        private String iprequest;

        @Column(name = "maquina")
        private String device;

        public Long getIdaudit() {
            return idaudit;
        }

        public void setIdaudit(Long idaudit) {
            this.idaudit = idaudit;
        }

        public String getIprequest() {
            return iprequest;
        }

        public void setIprequest(String iprequest) {
            this.iprequest = iprequest;
        }

        public String getDevice() {
            return device;
        }

        public void setDevice(String device) {
            this.device = device;
        }
    }
}
