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
package org.javabeanstack.sifen.core.validation;

/**
 * Grupo de campos del Manual Técnico de SIFEN (v150) al que pertenece una regla.
 *
 * <p>Las letras son las del MT (sección de la estructura del DE y tabla de reglas de validación):
 * el catálogo de reglas tiene una tabla por grupo y cada {@link ValidationRule} declara el suyo.
 * Las reglas de coherencia entre grupos (suma de ítems contra {@code gTotSub}, CDC contra
 * {@code gTimb}…) van en el grupo del campo que se rechaza, como hace el MT.</p>
 */
public enum RuleGroup {
    /** A. Campos firmados del DE (A001–A099): {@code DE}, {@code Id}, {@code dDVId}, {@code dFecFirma}… */
    A("Campos firmados del Documento Electrónico"),
    /** B. Campos inherentes a la operación del DE (B001–B099): {@code gOpeDE}. */
    B("Campos inherentes a la operación del Documento Electrónico"),
    /** C. Campos de datos del timbrado (C001–C099): {@code gTimb}. */
    C("Campos de datos del timbrado"),
    /** D. Campos generales del DE (D001–D299): {@code gDatGralOpe}, {@code gOpeCom}, {@code gEmis}, {@code gDatRec}. */
    D("Campos generales del Documento Electrónico"),
    /** E. Campos específicos por tipo de DE (E001–E009 y los subgrupos E010 en adelante): {@code gDtipDE}. */
    E("Campos específicos por tipo de Documento Electrónico"),
    /** F. Subtotales y totales (F001–F099): {@code gTotSub}. */
    F("Subtotales y totales"),
    /** G. Campos complementarios comerciales de uso general (G001–G049): {@code gCamGen}. */
    G("Campos complementarios comerciales de uso general"),
    /** H. Documento asociado (H001–H049): {@code gCamDEAsoc}. */
    H("Campos que identifican al documento asociado"),
    /** I. Firma digital (I001–I049). */
    I("Información de la firma digital"),
    /** J. Campos fuera de la firma (J001–J049): {@code gCamFuFD}. */
    J("Campos fuera de la firma digital"),
    /** Eventos ({@code rGesEve}): cancelación, inutilización, nominación… */
    EVENTO("Eventos"),
    /** Configuración de la librería ({@code SifenConfig}, código {@code JBS-CFG-<nn>}; D33, §3.7). */
    CFG("Configuración de la librería"),
    /** Certificado de firma y de TLS (código {@code JBS-CERT-<nn>}; D33, §3.7). */
    CERT("Certificado digital"),
    /** Reglas del propio validador (código {@code JBS-VAL-<nn>}): no vienen del MT. */
    VAL("Reglas internas del validador");

    private final String descripcion;

    RuleGroup(String descripcion) {
        this.descripcion = descripcion;
    }

    /**
     * Devuelve el nombre del grupo como lo titula el MT.
     *
     * @return descripción en español.
     */
    public String getDescription() {
        return descripcion;
    }
}
