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
 * Contratos del subsistema de salida de documentos. Separa tres conceptos
 * ortogonales que suelen aparecer fundidos en los controllers:
 *
 * <pre>
 *    FUENTE  ──genera──►  DOCUMENTO  ──entrega──►  DESTINOS (1..N)
 * IDocumentSource        IOutputDocument         IDocumentTarget
 * </pre>
 *
 * <ul>
 * <li>{@link org.javabeanstack.outputs.IDocumentSource} — la <b>fuente</b> sabe
 * <i>producir</i> un documento: una plantilla Word con sus marcadores, un
 * reporte JasperReports con sus datos y parámetros. Cada tecnología de
 * generación es una implementación nueva, sin tocar el resto.</li>
 * <li>{@link org.javabeanstack.outputs.IOutputDocument} — el <b>documento</b>
 * generado en memoria (bytes + nombre + content-type + formato). Se produce
 * una sola vez, cualquiera sea la cantidad de destinos.</li>
 * <li>{@link org.javabeanstack.outputs.IDocumentTarget} — el <b>destino</b>
 * sabe <i>entregar</i> un documento ya generado: descarga por la respuesta
 * HTTP, correo, carpeta del servidor y, a futuro, base de datos, gestión
 * documental, S3, WhatsApp. Cada canal es una implementación nueva, sin tocar
 * el resto.</li>
 * <li>{@link org.javabeanstack.outputs.IPrintableSource} — capacidad opcional
 * de una fuente de <i>imprimir directo</i> en una impresora del servidor, sin
 * producir un documento descargable.</li>
 * </ul>
 *
 * <p>
 * El orden que el orquestador de la salida debe garantizar: <b>primero generar
 * en memoria, después comprometer el destino</b>. Escribir cabeceras o tomar el
 * stream de la respuesta HTTP antes de terminar la generación entrega al
 * navegador una descarga corrupta cuando algo falla, e impide renderizar el
 * aviso de error ({@code UT010006: Cannot call getWriter(), getOutputStream()
 * already called}).
 * </p>
 *
 * <p>
 * Son contratos puros: no dependen de POI, de JasperReports, de la capa web ni
 * de la mensajería. Las implementaciones viven en el módulo dueño de cada
 * dependencia — el orquestador y la salida a carpeta en {@code jbs-core}, la
 * fuente de reportes en {@code jbs-jasper} (sobre
 * {@code org.javabeanstack.web.util.JasperReportUtil}), el destino de correo en
 * {@code jbs-messaging} (sobre {@code MailSender}), el de descarga en
 * {@code jbs-web}, y las fuentes de plantillas propias de cada aplicación.
 * </p>
 *
 * <p>
 * {@code IOutputDocument} calza campo a campo con
 * {@link org.javabeanstack.messaging.IMessageAttachment} (nombre, contenido,
 * content-type): convertir el documento en adjunto de un
 * {@link org.javabeanstack.messaging.IMailMessage} es una copia directa de
 * valores. La entrega puede registrarse opcionalmente en el log de la
 * aplicación con el evento
 * {@link org.javabeanstack.model.IAppLogRecord#EVENT_DOCUMENT_OUTPUT}.
 * </p>
 *
 * @author Jorge Enciso
 */
package org.javabeanstack.outputs;
