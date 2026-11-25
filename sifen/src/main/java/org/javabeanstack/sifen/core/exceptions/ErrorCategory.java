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
package org.javabeanstack.sifen.core.exceptions;

/**
 * Categoría de un error de la librería (plan JBSSIF, §3.8, D33 Parte C): dice <b>quién</b> tiene que
 * actuar, para que la aplicación elija icono, color y a quién avisar sin interpretar el texto.
 */
public enum ErrorCategory {
    /** El contenido del documento o del evento no cumple una regla del catálogo: lo corrige el usuario. */
    DE_CONTENT("Contenido del documento", "usuario"),
    /** Un argumento de la operación o el XML que se leyó no sirve: lo corrige el usuario o la aplicación. */
    INPUT("Datos de entrada", "usuario"),
    /** La configuración de la librería está incompleta o es incoherente: la corrige el administrador. */
    CONFIGURATION("Configuración", "administrador"),
    /** El certificado digital falta, venció o no se puede usar: lo corrige el administrador. */
    CERTIFICATE("Certificado digital", "administrador"),
    /** No se pudo hablar con SIFEN (red, proxy, TLS, HTTP): reintentar o avisar a TI. */
    CONNECTION("Conexión con SIFEN", "TI"),
    /** SIFEN respondió algo que no se puede interpretar (no un rechazo, que va en la respuesta): soporte. */
    SIFEN_RESPONSE("Respuesta de SIFEN", "soporte"),
    /** Falla interna de la librería o del entorno: soporte, con el id de correlación. */
    INTERNAL("Error interno", "soporte");

    private final String descripcion;
    private final String responsable;

    ErrorCategory(String descripcion, String responsable) {
        this.descripcion = descripcion;
        this.responsable = responsable;
    }

    /**
     * Devuelve el nombre de la categoría para mostrar.
     *
     * @return el nombre en español.
     */
    public String getDescription() {
        return descripcion;
    }

    /**
     * Devuelve a quién le corresponde actuar: {@code usuario}, {@code administrador}, {@code TI} o
     * {@code soporte}.
     *
     * @return el responsable sugerido.
     */
    public String getResponsible() {
        return responsable;
    }
}
