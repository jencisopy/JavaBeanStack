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
package org.javabeanstack.data;

import org.javabeanstack.model.tables.Ciudad;
import org.javabeanstack.model.tables.Pais;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cortocircuito de {@code AbstractDAO.findByUk} ante valores de clave única
 * nulos o entidades todavía sin identificador (plan {@code QUERYUK}, RF5).
 *
 * <p>Es un test PURO sobre la regla, que vive en un método estático
 * justamente para poder medirla sin {@code EntityManager}: la consulta en sí
 * necesita servidor y base, y se verifica en los escenarios funcionales.</p>
 *
 * @author Jorge Enciso
 */
@DisplayName("AbstractDAO.findByUk: cortocircuito por nulo o transitorio (QUERYUK)")
public class AbstractDAOFindByUkShortCircuitTest {

    @Test
    @DisplayName("Un valor nulo no tiene coincidencia posible")
    public void testValorNulo() {
        assertTrue(AbstractDAO.isUnresolvableUkValue(null));
        Ciudad ciudad = new Ciudad();
        assertTrue(AbstractDAO.isUnresolvableUkValue(ciudad.getValue("provincia")));
        assertTrue(AbstractDAO.isUnresolvableUkValue(ciudad.getValue("codigo")));
    }

    @Test
    @DisplayName("Una entidad asociada sin identificador es transitoria")
    public void testEntidadTransitoria() {
        Ciudad ciudad = new Ciudad();
        ciudad.setProvincia(new Pais());
        assertTrue(AbstractDAO.isUnresolvableUkValue(ciudad.getValue("provincia")));
    }

    @Test
    @DisplayName("Una entidad asociada con identificador se consulta normalmente")
    public void testEntidadPersistida() {
        Pais provincia = new Pais();
        provincia.setIdpais(7L);
        Ciudad ciudad = new Ciudad();
        ciudad.setProvincia(provincia);
        ciudad.setCodigo("ASU");
        ciudad.setNombreCorto("Asunción");

        for (String attribute : DataInfo.getUniqueKeyAttributes(Ciudad.class)) {
            assertFalse(AbstractDAO.isUnresolvableUkValue(ciudad.getValue(attribute)),
                    "no debería cortocircuitar por " + attribute);
        }
    }

    @Test
    @DisplayName("Un escalar con valor no cortocircuita")
    public void testEscalarConValor() {
        assertFalse(AbstractDAO.isUnresolvableUkValue("ASU"));
        assertFalse(AbstractDAO.isUnresolvableUkValue(0L));
        assertFalse(AbstractDAO.isUnresolvableUkValue(Boolean.FALSE));
    }
}
