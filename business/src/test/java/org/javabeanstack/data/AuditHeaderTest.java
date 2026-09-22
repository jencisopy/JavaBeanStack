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

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas puras de la composición del dispositivo de la cabecera de auditoría.
 *
 * @author Jorge Enciso
 */
public class AuditHeaderTest {

    @Test
    @DisplayName("Con nombre e IP: 'nombre (ip)'")
    public void conNombreEIp() {
        assertEquals("tablet-vendedor-guido (181.91.86.240)",
                AuditHeader.formatDevice("tablet-vendedor-guido", "181.91.86.240"));
    }

    @Test
    @DisplayName("Sin nombre (sesión por login): solo la IP")
    public void soloIp() {
        assertEquals("127.0.0.1", AuditHeader.formatDevice(null, "127.0.0.1"));
        assertEquals("127.0.0.1", AuditHeader.formatDevice("", "127.0.0.1"));
        assertEquals("127.0.0.1", AuditHeader.formatDevice("   ", "127.0.0.1"));
    }

    @Test
    @DisplayName("Sin IP: solo el nombre")
    public void soloNombre() {
        assertEquals("tablet-vendedor-guido", AuditHeader.formatDevice("tablet-vendedor-guido", null));
        assertEquals("tablet-vendedor-guido", AuditHeader.formatDevice("tablet-vendedor-guido", "  "));
    }

    @Test
    @DisplayName("Sin nombre ni IP: null, no cadena vacía")
    public void sinOrigen() {
        assertNull(AuditHeader.formatDevice(null, null));
        assertNull(AuditHeader.formatDevice("", ""));
        assertNull(AuditHeader.formatDevice("  ", "  "));
    }

    @Test
    @DisplayName("Nombre de 250 caracteres: se recorta el nombre y la IP entra entera")
    public void truncadoDelNombre() {
        String nombre = "X".repeat(250);
        String ip = "181.91.86.240";
        String device = AuditHeader.formatDevice(nombre, ip, 150);
        assertEquals(150, device.length());
        assertTrue(device.endsWith(" (" + ip + ")"), "la IP tiene que entrar completa: " + device);
        //150 - largo de " (181.91.86.240)" = 150 - 16 = 134 caracteres de nombre.
        assertEquals("X".repeat(134) + " (" + ip + ")", device);
    }

    @Test
    @DisplayName("El máximo por omisión es el largo de la columna maquina (150)")
    public void maximoPorOmision() {
        assertEquals(150, AuditHeader.DEVICE_MAX_LENGTH);
        String device = AuditHeader.formatDevice("Y".repeat(300), "10.0.0.1");
        assertEquals(150, device.length());
        assertTrue(device.endsWith(" (10.0.0.1)"));
    }

    @Test
    @DisplayName("El nombre justo en el límite no se recorta")
    public void nombreJustoEnElLimite() {
        String ip = "10.0.0.1";
        int disponible = 150 - (" (" + ip + ")").length();
        String nombre = "Z".repeat(disponible);
        assertEquals(nombre + " (" + ip + ")", AuditHeader.formatDevice(nombre, ip, 150));
        //Un caracter más y se recorta exactamente uno.
        assertEquals(nombre + " (" + ip + ")", AuditHeader.formatDevice(nombre + "Z", ip, 150));
    }

    @Test
    @DisplayName("Máximo tan chico que ni la IP entra: se devuelve la IP recortada")
    public void maximoMenorQueLaIp() {
        assertEquals("181.91", AuditHeader.formatDevice("tablet", "181.91.86.240", 6));
        assertEquals("", AuditHeader.formatDevice("tablet", "181.91.86.240", 0));
    }

    @Test
    @DisplayName("Los espacios sobrantes de los extremos no entran al valor")
    public void seRecortanEspacios() {
        assertEquals("tablet (10.0.0.1)", AuditHeader.formatDevice("  tablet  ", " 10.0.0.1 "));
    }

    @Test
    @DisplayName("Solo el nombre, más largo que el máximo: se recorta al máximo")
    public void nombreSoloRecortado() {
        String device = AuditHeader.formatDevice("W".repeat(300), null, 150);
        assertEquals(150, device.length());
        assertEquals("W".repeat(150), device);
    }
}
