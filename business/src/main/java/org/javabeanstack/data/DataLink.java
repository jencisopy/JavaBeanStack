/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 Jorge Enciso
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

package org.javabeanstack.data;

import jakarta.ejb.EJB;
import org.javabeanstack.data.services.IDataService;

/**
 * Esta clase se encarga de la conexión con los datos, utiliza para el efecto la
 * clase GenericDAO. Es un wraper de GenericDAO, provee interfases que
 * permiten la recuperación, busqueda, refresco, y actualización de los
 * registros.
 *
 * @author Jorge Enciso
 */
public class DataLink extends AbstractDataLink {
    /** Es el objeto responsable del acceso a los datos  */
    @EJB
    private IGenericDAO dao;

    public DataLink() {
    }

    public <T extends IGenericDAO> DataLink(T dao) {
        this.dao = dao;
    }


    /**
     * Devuelve el objeto de acceso a datos (dao)
     * @return objeto de acceso a datos (dao)
     */
    @Override
    public IGenericDAO getDao() {
        return dao;
    }

    /**
     * Devuelve el objeto DataService (es el componente que valida y graba los datos en la base)
     * @param <T>
     * @return objeto de acceso a datos (dao)
     */
    @Override
    public <T extends IDataService> T getDataService() {
        if (dao instanceof IDataService) {
            return (T)dao;
        }
        return null;
    }

    /**
     * Asignación objeto acceso a datos. Solo se utiliza si no funciono la
     * injection en el constructor
     *
     * @param <T>
     * @param dao objeto para acceso a los datos.
     */
    @Override
    public <T extends IGenericDAO> void setDao(T dao) {
        this.dao = dao;
    }
}
