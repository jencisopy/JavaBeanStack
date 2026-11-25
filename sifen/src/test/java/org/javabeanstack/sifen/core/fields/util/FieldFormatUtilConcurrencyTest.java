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
package org.javabeanstack.sifen.core.fields.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@code FieldFormatUtil.formattdCRed} es seguro entre hilos (plan JBSSIF, §4.3, E3-09, CA4-06).
 *
 * <p>La línea base compartía un {@code DecimalFormat} estático, que el javadoc del JDK declara no
 * sincronizado. El test formatea los valores de {@code dRedon} del catálogo desde 16 hilos a la vez y
 * exige el mismo texto que da un solo hilo. <b>Es un test de regresión, no una reproducción</b>: en
 * Temurin 25 {@code DecimalFormat.format(BigDecimal)} sincroniza internamente sobre su
 * {@code digitList} y el estático viejo también pasa (I4-01; evidencia en
 * {@code jbs_sifen_insumos/fase4/e309_antes.txt}). El arreglo no depende de ese detalle de
 * implementación.</p>
 */
public class FieldFormatUtilConcurrencyTest {
    private static final int HILOS = 16;
    private static final int VUELTAS = 200_000;

    @Test
    public void formattdCRedEsSeguroEntreHilos() throws Exception {
        List<BigDecimal> valores = new ArrayList<>();
        List<String> esperados = new ArrayList<>();
        for (String v : new String[]{"0", "0.1", "0.100", "0.24", "0.240", "39", "49", "0.18", "1.2345", "12.5", "7", "0.05",
                "1912", "0.0001", "3.14159", "-0.5", "1234.5678", "9876.4321", "0.4999", "4321.1234"}) {
            BigDecimal b = new BigDecimal(v);
            valores.add(b);
            esperados.add(FieldFormatUtil.formattdCRed(b));   // un solo hilo: la referencia
        }
        AtomicInteger distintos = new AtomicInteger();
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(HILOS);
        try {
            List<Future<?>> tareas = new ArrayList<>();
            for (int h = 0; h < HILOS; h++) {
                final int desfase = h;
                tareas.add(pool.submit(() -> {
                    largada.await();
                    for (int i = 0; i < VUELTAS; i++) {
                        int k = (i + desfase) % valores.size();
                        if (!esperados.get(k).equals(FieldFormatUtil.formattdCRed(valores.get(k)))) {
                            distintos.incrementAndGet();
                        }
                    }
                    return null;
                }));
            }
            largada.countDown();
            for (Future<?> f : tareas) {
                f.get();
            }
        } finally {
            pool.shutdownNow();
        }
        Assertions.assertEquals(0, distintos.get(),
                "formattdCRed dio un texto distinto del de un solo hilo en " + distintos.get() + " de " + (HILOS * VUELTAS) + " llamadas (E3-09)");
    }

    /** El formateador por llamada da el mismo texto que el estático de la línea base (sin mover golden). */
    @Test
    public void mismoTextoQueLaLineaBase() {
        Assertions.assertEquals("0.1", FieldFormatUtil.formattdCRed(new BigDecimal("0.100")));
        Assertions.assertEquals("39", FieldFormatUtil.formattdCRed(new BigDecimal("39")));
        Assertions.assertEquals("0", FieldFormatUtil.formattdCRed(new BigDecimal("0.000")));
        Assertions.assertEquals("1.2346", FieldFormatUtil.formattdCRed(new BigDecimal("1.23456")));
        Assertions.assertEquals("-1912", FieldFormatUtil.formattdCRed(new BigDecimal("-1912")), "máximo 4 enteros: 1912 cabe");
        Assertions.assertEquals("2345", FieldFormatUtil.formattdCRed(new BigDecimal("12345")), "máximo 4 enteros, como la línea base");
    }
}
