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
 * Tipos del contrato de API v1 de {@code jbs-sifen} (plan JBSSIF, Fase 10, D34): lo que devuelven y
 * reciben las operaciones de {@code org.javabeanstack.sifen.ISifenClient}, y los resultados de la conciliación
 * de {@code org.javabeanstack.sifen.SifenReconciler} ({@code Reconciliation*}, PROVISIONALES; plan JBSSIF, §8.8).
 *
 * <p>Reglas comunes:</p>
 * <ul>
 *   <li>Inmutables y serializables; se arman con su {@code builder()} (o {@code of(...)} los valores
 *       chicos). La aplicación puede armarlos en sus pruebas.</li>
 *   <li>Los textos nunca son {@code null} (vacíos si SIFEN no los informó) y las listas son
 *       inmodificables. Las fechas e instantes son {@code null} solo donde el javadoc lo dice.</li>
 *   <li>Ningún tipo de este paquete nombra {@code org.javabeanstack.sifen.internal.*}.</li>
 *   <li>Los enums ({@link org.javabeanstack.sifen.api.ResultStatus},
 *       {@link org.javabeanstack.sifen.api.BatchStatus}) son abiertos: un valor que la aplicación no
 *       conoce se trata como {@code UNRECOGNIZED}; en
 *       {@link org.javabeanstack.sifen.api.ReconciliationStatus}, como {@code UNDETERMINED} (nunca como
 *       {@code NOT_FOUND}).</li>
 * </ul>
 */
package org.javabeanstack.sifen.api;
