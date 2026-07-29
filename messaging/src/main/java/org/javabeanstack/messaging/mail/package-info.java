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
 * Implementación del canal de correo del subsistema de mensajería. Contiene el
 * objeto de mensaje en memoria ({@link org.javabeanstack.messaging.mail.MailMessage}
 * y sus partes), el proveedor de la sesión de correo, el conversor a MIME y el
 * emisor SMTP síncrono. Con estas piezas se puede enviar un correo sin base de
 * datos (Fase 2). La recepción y el almacenamiento llegan en la Fase 3.
 *
 * @author Jorge Enciso
 */
package org.javabeanstack.messaging.mail;
