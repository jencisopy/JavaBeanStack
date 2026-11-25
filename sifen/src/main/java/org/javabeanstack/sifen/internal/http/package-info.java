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
/**
 * Transporte de los pedidos a SIFEN (plan JBSSIF, Fase 5, §5.2–§5.5; D37).
 *
 * <p>{@link org.javabeanstack.sifen.internal.http.SifenConnection} es la interfaz interna del
 * transporte (D34 y D37 la llamaban {@code ConexionSifen}; nombre en inglés por D42, I10-10). La
 * implementación del beta conserva {@code HttpsURLConnection}, pero <b>por instancia</b>: cada conexión
 * tiene su contexto TLS y su almacén de certificados, que se descartan y se rearman ante una falla de
 * transporte o un cambio del certificado, sin reiniciar la JVM (E0-05, M0-04, M3-64, E3-31). Tiempos de
 * espera, proxy y reintentos se leen de la {@code SifenConfig} en cada pedido.</p>
 *
 * <p>Piezas para la fachada (carril F): {@link org.javabeanstack.sifen.internal.http.SifenConnection#create()}
 * (una por cliente, se pasa a {@code BaseRequest.makeRequest} y se cierra en {@code close()}),
 * {@link org.javabeanstack.sifen.internal.http.OperationContext} (id de operación en las líneas del
 * transporte, I10-09), {@link org.javabeanstack.sifen.internal.http.ConnectionProbe} (diagnóstico por
 * etapas de {@code checkConnection}, §5.3) y {@link org.javabeanstack.sifen.internal.http.BatchChecks}
 * (lote de un solo tipo y emisor, {@code JBS-ENT-09}, §5.5).</p>
 */
package org.javabeanstack.sifen.internal.http;
