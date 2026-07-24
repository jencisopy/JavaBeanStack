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

package org.javabeanstack.model;

import java.io.Serializable;
import java.util.List;
import jakarta.xml.bind.annotation.XmlTransient;
import org.javabeanstack.data.IDataRow;

/**
 * Contrato de la entidad empresa: datos de identificación fiscal y de contacto,
 * la unidad de persistencia/base de datos asociada, el menú, el logo y la
 * jerarquía de empresas. Extiende {@link IDataRow}.
 *
 * @author Jorge Enciso
 */
public interface IAppCompany extends IDataRow, Serializable {
    /**
     * Devuelve el identificador de la empresa.
     * @return identificador de la empresa.
     */
    Long getIdcompany();

    /**
     * Devuelve el identificador del grupo de empresas.
     * @return identificador del grupo de empresas.
     */
    Long getIdcompanygroup();

    /**
     * Devuelve el identificador de la empresa máscara (plantilla).
     * @return identificador de la empresa máscara.
     */
    Long getIdcompanymask();

    /**
     * Devuelve el identificador del período activo de la empresa.
     * @return identificador del período.
     */
    Long getIdperiod();

    /**
     * Devuelve el nombre de la empresa.
     * @return nombre de la empresa.
     */
    String getName();

    /**
     * Devuelve el país de la empresa.
     * @return país.
     */
    String getCountry();

    /**
     * Devuelve la razón social de la empresa.
     * @return razón social.
     */
    String getSocialName();

    /**
     * Devuelve el identificador tributario (RUC/NIT) de la empresa.
     * @return identificador tributario.
     */
    String getTaxId();

    /**
     * Devuelve el número de teléfono de la empresa.
     * @return número de teléfono.
     */
    String getTelephoneNumber();

    /**
     * Devuelve la unidad de persistencia asociada a la empresa.
     * @return nombre de la unidad de persistencia.
     */
    String getPersistentUnit();

    /**
     * Devuelve la dirección de la empresa.
     * @return dirección.
     */
    String getAddress();

    /**
     * Devuelve la actividad económica de la empresa.
     * @return actividad de la empresa.
     */
    String getCompanyActivity();

    /**
     * Devuelve información adicional de la empresa.
     * @return información adicional.
     */
    String getInformation();

    /**
     * Devuelve el usuario de aplicación asociado a la empresa.
     * @return usuario de aplicación.
     */
    String getAppuser();

    /**
     * Devuelve la lista de empresas relacionadas (jerarquía).
     * @return lista de empresas.
     */
    @XmlTransient
    List<IAppCompany> getCompanyList();

    /**
     * Devuelve la ruta del sistema de archivos de la empresa.
     * @return ruta del sistema de archivos.
     */
    String getFilesystem();

    /**
     * Devuelve el logo de la empresa.
     * @return logo en bytes.
     */
    byte[] getLogo();

    /**
     * Devuelve el menú asociado a la empresa.
     * @return menú.
     */
    String getMenu();

    /**
     * Devuelve el motor de base de datos de la empresa.
     * @return motor de base de datos.
     */
    String getDbengine();

    /**
     * Asigna el identificador de la empresa.
     * @param idcompany identificador de la empresa.
     */
    void setIdcompany(Long idcompany);

    /**
     * Asigna el identificador del grupo de empresas.
     * @param idcompanygroup identificador del grupo de empresas.
     */
    void setIdcompanygroup(Long idcompanygroup);

    /**
     * Asigna el identificador de la empresa máscara.
     * @param idcompanymask identificador de la empresa máscara.
     */
    void setIdcompanymask(Long idcompanymask);

    /**
     * Asigna el identificador del período activo.
     * @param idperiod identificador del período.
     */
    void setIdperiod(Long idperiod);

    /**
     * Asigna el nombre de la empresa.
     * @param name nombre de la empresa.
     */
    void setName(String name);

    /**
     * Asigna el país de la empresa.
     * @param country país.
     */
    void setCountry(String country);

    /**
     * Asigna la razón social de la empresa.
     * @param socialName razón social.
     */
    void setSocialName(String socialName);

    /**
     * Asigna el identificador tributario de la empresa.
     * @param taxId identificador tributario.
     */
    void setTaxId(String taxId);

    /**
     * Asigna el número de teléfono de la empresa.
     * @param number número de teléfono.
     */
    void setTelephoneNumber(String number);

    /**
     * Asigna la dirección de la empresa.
     * @param address dirección.
     */
    void setAddress(String address);

    /**
     * Asigna la lista de empresas relacionadas.
     * @param companyList lista de empresas.
     */
    void setCompanyList(List<IAppCompany> companyList);

    /**
     * Asigna la actividad económica de la empresa.
     * @param empresaActivity actividad de la empresa.
     */
    void setCompanyActivity(String empresaActivity);

    /**
     * Asigna información adicional de la empresa.
     * @param information información adicional.
     */
    void setInformation(String information);

    /**
     * Asigna el usuario de aplicación asociado a la empresa.
     * @param appuser usuario de aplicación.
     */
    void setAppuser(String appuser);

    /**
     * Asigna la unidad de persistencia asociada a la empresa.
     * @param persistentUnit nombre de la unidad de persistencia.
     */
    void setPersistentUnit(String persistentUnit);

    /**
     * Asigna la ruta del sistema de archivos de la empresa.
     * @param filesystem ruta del sistema de archivos.
     */
    void setFilesystem(String filesystem);

    /**
     * Asigna el logo de la empresa.
     * @param logo logo en bytes.
     */
    void setLogo(byte[] logo);

    /**
     * Asigna el menú asociado a la empresa.
     * @param menu menú.
     */
    void setMenu(String menu);

    /**
     * Asigna el motor de base de datos de la empresa.
     * @param dbEngine motor de base de datos.
     */
    void setDbengine(String dbEngine);
}
