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
package org.javabeanstack.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Pruebas de la conversión texto a tipo de dato (plan XMLMAP, §1.3).
 *
 * @author Jorge Enciso
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class ValueConverterTest {

    /**
     * Enumeración de prueba.
     */
    public enum Color {
        ROJO, VERDE, AZUL
    }

    public ValueConverterTest() {
    }

    /**
     * El texto se devuelve tal cual cuando el destino es String.
     */
    @Test
    public void test01String() {
        System.out.println("1-ValueConverter - String");
        assertEquals("hola", ValueConverter.fromString("hola", String.class));
        assertEquals(" con espacios ", ValueConverter.fromString(" con espacios ", String.class));
        //El vacío sigue siendo vacío en String (emptyAsNull es del llamador)
        assertEquals("", ValueConverter.fromString("", String.class));
        assertNull(ValueConverter.fromString(null, String.class));
    }

    /**
     * Character toma el primer carácter; el vacío es nulo.
     */
    @Test
    public void test02Character() {
        System.out.println("2-ValueConverter - Character");
        assertEquals('S', ValueConverter.fromString("Si", Character.class));
        assertEquals('N', ValueConverter.fromString("N", char.class));
        assertNull(ValueConverter.fromString("", Character.class));
        //Recorta los extremos como todos los tipos que no son String
        assertEquals('X', ValueConverter.fromString(" X ", Character.class));
        assertNull(ValueConverter.fromString("   ", Character.class));
    }

    /**
     * Booleanos admitidos (D5): T/F, true/false, 1/0 sin distinguir
     * mayúsculas. S/N no es un valor válido.
     */
    @Test
    public void test03Boolean() {
        System.out.println("3-ValueConverter - Boolean");
        for (String verdadero : new String[]{"T", "t", "true", "TRUE", "True", "1", " T "}) {
            assertEquals(Boolean.TRUE, ValueConverter.fromString(verdadero, Boolean.class), verdadero);
        }
        for (String falso : new String[]{"F", "f", "false", "FALSE", "0"}) {
            assertEquals(Boolean.FALSE, ValueConverter.fromString(falso, Boolean.class), falso);
        }
        assertEquals(Boolean.TRUE, ValueConverter.fromString("true", boolean.class));
        assertNull(ValueConverter.fromString("", Boolean.class));
        //S/N no se acepta (D5)
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ValueConverter.fromString("S", Boolean.class));
        assertEquals("\"S\" no se puede convertir a Boolean", ex.getMessage());
        assertThrows(IllegalArgumentException.class, () -> ValueConverter.fromString("N", Boolean.class));
        assertThrows(IllegalArgumentException.class, () -> ValueConverter.fromString("si", Boolean.class));
    }

    /**
     * Tipos enteros: valor exacto, rechazo de decimales y del texto que no es
     * un número.
     */
    @Test
    public void test04Enteros() {
        System.out.println("4-ValueConverter - enteros");
        assertEquals(Short.valueOf("12"), ValueConverter.fromString("12", Short.class));
        assertEquals(12, ValueConverter.fromString("12", Integer.class));
        assertEquals(12L, ValueConverter.fromString(" 12 ", Long.class));
        assertEquals(12L, ValueConverter.fromString("12", long.class));
        assertEquals(-5, ValueConverter.fromString("-5", int.class));
        assertEquals(new BigInteger("900000000000"), ValueConverter.fromString("900000000000", BigInteger.class));
        //"12.0" es exacto, "12.5" no
        assertEquals(12L, ValueConverter.fromString("12.0", Long.class));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ValueConverter.fromString("12.5", Long.class));
        assertEquals("\"12.5\" no se puede convertir a Long", ex.getMessage());
        //Fuera de rango
        assertThrows(IllegalArgumentException.class, () -> ValueConverter.fromString("99999999", Short.class));
        //No es un número
        ex = assertThrows(IllegalArgumentException.class,
                () -> ValueConverter.fromString("doce", Integer.class));
        assertEquals("\"doce\" no se puede convertir a Integer", ex.getMessage());
        //Vacío
        assertNull(ValueConverter.fromString("", Long.class));
        assertNull(ValueConverter.fromString("   ", Integer.class));
    }

    /**
     * Tipos decimales.
     */
    @Test
    public void test05Decimales() {
        System.out.println("5-ValueConverter - decimales");
        assertEquals(new BigDecimal("1500.35"), ValueConverter.fromString("1500.35", BigDecimal.class));
        assertEquals(1500.35D, (Double) ValueConverter.fromString("1500.35", Double.class), 0.000001D);
        assertEquals(1500.35D, (Double) ValueConverter.fromString("1500.35", double.class), 0.000001D);
        assertEquals(1.5F, (Float) ValueConverter.fromString("1.5", Float.class), 0.000001F);
        assertEquals(1.5F, (Float) ValueConverter.fromString("1.5", float.class), 0.000001F);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ValueConverter.fromString("1,5", BigDecimal.class));
        assertEquals("\"1,5\" no se puede convertir a BigDecimal", ex.getMessage());
        assertNull(ValueConverter.fromString("", BigDecimal.class));
    }

    /**
     * Fechas en formato ISO.
     */
    @Test
    public void test06FechasIso() {
        System.out.println("6-ValueConverter - fechas ISO");
        assertEquals(LocalDate.of(2026, 10, 25), ValueConverter.fromString("2026-10-25", LocalDate.class));
        assertEquals(LocalDateTime.of(2026, 10, 25, 13, 45, 30),
                ValueConverter.fromString("2026-10-25T13:45:30", LocalDateTime.class));
        //Solo fecha en un LocalDateTime: medianoche
        assertEquals(LocalDateTime.of(2026, 10, 25, 0, 0),
                ValueConverter.fromString("2026-10-25", LocalDateTime.class));
        //java.util.Date pasa por LocalDateTime y la zona del sistema
        Date fecha = (Date) ValueConverter.fromString("2026-10-25T13:45:30", Date.class);
        assertEquals(LocalDateTime.of(2026, 10, 25, 13, 45, 30),
                LocalDateTime.ofInstant(fecha.toInstant(), ZoneId.systemDefault()));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ValueConverter.fromString("25/10/2026", LocalDate.class));
        assertEquals("\"25/10/2026\" no se puede convertir a LocalDate", ex.getMessage());
        //El mensaje nombra el tipo pedido, no el que se usa para parsear
        ex = assertThrows(IllegalArgumentException.class,
                () -> ValueConverter.fromString("25/10/2026", Date.class));
        assertEquals("\"25/10/2026\" no se puede convertir a Date", ex.getMessage());
        ex = assertThrows(IllegalArgumentException.class,
                () -> ValueConverter.fromString("ayer", LocalDateTime.class));
        assertEquals("\"ayer\" no se puede convertir a LocalDateTime", ex.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> ValueConverter.fromString("2026-13-01", LocalDate.class));
        assertNull(ValueConverter.fromString("", LocalDate.class));
        assertNull(ValueConverter.fromString("", LocalDateTime.class));
        assertNull(ValueConverter.fromString("", Date.class));
    }

    /**
     * Fechas con formateador propio (RF12).
     */
    @Test
    public void test07FechasConFormato() {
        System.out.println("7-ValueConverter - fechas con formato");
        DateTimeFormatter fecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter fechaHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        assertEquals(LocalDate.of(2026, 10, 25),
                ValueConverter.fromString("25/10/2026", LocalDate.class, fecha, fechaHora));
        assertEquals(LocalDateTime.of(2026, 10, 25, 13, 45, 30),
                ValueConverter.fromString("25/10/2026 13:45:30", LocalDateTime.class, fecha, fechaHora));
        //Solo fecha con el formateador de fecha
        assertEquals(LocalDateTime.of(2026, 10, 25, 0, 0),
                ValueConverter.fromString("25/10/2026", LocalDateTime.class, fecha, fechaHora));
        //Con formato propio el ISO ya no vale
        assertThrows(IllegalArgumentException.class,
                () -> ValueConverter.fromString("2026-10-25", LocalDate.class, fecha, fechaHora));
    }

    /**
     * Enumeraciones por nombre, sin distinguir mayúsculas.
     */
    @Test
    public void test08Enumeraciones() {
        System.out.println("8-ValueConverter - enumeraciones");
        assertEquals(Color.ROJO, ValueConverter.fromString("ROJO", Color.class));
        assertEquals(Color.VERDE, ValueConverter.fromString("verde", Color.class));
        assertEquals(Color.AZUL, ValueConverter.fromString(" Azul ", Color.class));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ValueConverter.fromString("amarillo", Color.class));
        assertEquals("\"amarillo\" no se puede convertir a Color", ex.getMessage());
        assertNull(ValueConverter.fromString("", Color.class));
    }

    /**
     * Tipos no soportados y argumentos inválidos.
     */
    @Test
    public void test09NoSoportado() {
        System.out.println("9-ValueConverter - tipos no soportados");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ValueConverter.fromString("algo", Object.class));
        assertEquals("\"algo\" no se puede convertir a Object", ex.getMessage());
        assertThrows(IllegalArgumentException.class, () -> ValueConverter.fromString("algo", null));
        //El nulo nunca falla, aunque el tipo no esté soportado
        assertNull(ValueConverter.fromString(null, Object.class));
    }

    /**
     * isSupported responde por todos los tipos de RF7.
     */
    @Test
    public void test10IsSupported() {
        System.out.println("10-ValueConverter - isSupported");
        Class<?>[] soportados = new Class<?>[]{String.class, Character.class, char.class,
            Boolean.class, boolean.class, Short.class, short.class, Integer.class, int.class,
            Long.class, long.class, BigInteger.class, BigDecimal.class, Double.class, double.class,
            Float.class, float.class, LocalDate.class, LocalDateTime.class, Date.class, Color.class};
        for (Class<?> tipo : soportados) {
            assertTrue(ValueConverter.isSupported(tipo), tipo.getName());
        }
        assertFalse(ValueConverter.isSupported(Object.class));
        assertFalse(ValueConverter.isSupported(byte[].class));
        assertFalse(ValueConverter.isSupported(null));
    }
}
