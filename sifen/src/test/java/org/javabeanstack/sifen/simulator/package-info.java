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
 * Simulador de SIFEN para pruebas sin red ni certificado (plan JBSSIF, §8.6): {@link
 * org.javabeanstack.sifen.simulator.SifenSimulator}, sus {@link org.javabeanstack.sifen.simulator.Scenario
 * escenarios} y los {@link org.javabeanstack.sifen.simulator.RecordedRequest pedidos registrados}. Se publica en el
 * {@code test-jar} de {@code jbs-sifen} ({@code <classifier>tests</classifier>}, {@code <scope>test</scope>}), con
 * las respuestas grabadas y los certificados desechables bajo {@code org/javabeanstack/sifen/simulator/}. No es parte
 * del contrato de API v1: es una herramienta de prueba y puede cambiar.
 */
package org.javabeanstack.sifen.simulator;
