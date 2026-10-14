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

import java.util.ArrayList;
import java.util.List;

import org.javabeanstack.model.tables.Ciudad;
import org.javabeanstack.model.tables.Pais;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cortocircuito de {@code AbstractDAO.findByUk} ante valores de clave única
 * nulos o entidades todavía sin identificador (plan {@code QUERYUK}, RF5).
 *
 * <p>Es un test PURO sobre la regla, que vive en un método estático
 * justamente para poder medirla sin {@code EntityManager}: la consulta en sí
 * necesita servidor y base, y se verifica en los escenarios funcionales.</p>
 *
 * <p>Cubre también la elección de fila cuando la base tiene duplicados
 * ({@code pickUkMatch}, hallazgo {@code M0-02}), que es la otra mitad de lo
 * que {@code findByUk} decide sin tocar la base.</p>
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

    @Test
    @DisplayName("Identidad derivada: el padre sin grabar hace transitoria a la hija")
    public void testIdentidadDerivada() {
        //El @Id está sobre una asociación (caso Ctacteretencion): getId() no
        //devuelve nulo (el atributo viene inicializado con new) y sin mirar el
        //padre se ligaría una entidad transitoria como parámetro.
        ConIdentidadDerivada hija = new ConIdentidadDerivada();
        assertTrue(AbstractDAO.isUnresolvableUkValue(hija));
        hija.getPais().setIdpais(7L);
        assertFalse(AbstractDAO.isUnresolvableUkValue(hija));
    }

    /**
     * Entidad de prueba con identidad derivada: su clave primaria es la
     * asociación, no un escalar.
     */
    @jakarta.persistence.Entity
    @jakarta.persistence.Table(name = "conidentidadderivada")
    public static class ConIdentidadDerivada extends DataRow {

        @jakarta.persistence.Id
        @jakarta.persistence.JoinColumn(name = "idpais", referencedColumnName = "idpais")
        @jakarta.persistence.OneToOne
        private Pais pais = new Pais();

        public Pais getPais() {
            return pais;
        }

        public void setPais(Pais pais) {
            this.pais = pais;
        }
    }

    @Test
    @DisplayName("Sin coincidencias no hay fila")
    public void testSinCoincidencias() {
        assertNull(AbstractDAO.pickUkMatch(new ArrayList<Ciudad>(), new Ciudad()));
        assertNull(AbstractDAO.pickUkMatch(null, new Ciudad()));
    }

    @Test
    @DisplayName("Una sola coincidencia: esa")
    public void testUnaCoincidencia() {
        Ciudad encontrada = ciudad(10L);
        assertSame(encontrada, AbstractDAO.pickUkMatch(List.of(encontrada), ciudad(null)));
    }

    @Test
    @DisplayName("Duplicados en el alta: cualquiera sirve, se devuelve la primera")
    public void testDuplicadosEnElAlta() {
        Ciudad primera = ciudad(10L);
        Ciudad segunda = ciudad(11L);
        assertSame(primera,
                AbstractDAO.pickUkMatch(List.of(primera, segunda), ciudad(null)));
    }

    @Test
    @DisplayName("Duplicados al modificar una de las repetidas: se devuelve la OTRA")
    public void testDuplicadosAlModificarUnaDeLasRepetidas() {
        Ciudad primera = ciudad(10L);
        Ciudad segunda = ciudad(11L);
        //Si devolviera la fila que se está modificando, checkUniqueKey la daría
        //por buena (es el mismo registro) y el duplicado pasaría.
        assertSame(segunda,
                AbstractDAO.pickUkMatch(List.of(primera, segunda), ciudad(10L)));
    }

    @Test
    @DisplayName("Duplicados al modificar una fila ajena: se devuelve la primera")
    public void testDuplicadosAlModificarUnaFilaAjena() {
        Ciudad primera = ciudad(10L);
        Ciudad segunda = ciudad(11L);
        assertSame(primera,
                AbstractDAO.pickUkMatch(List.of(primera, segunda), ciudad(99L)));
    }

    /**
     * Arma una ciudad con la misma clave única y el identificador dado.
     *
     * @param id valor de la clave primaria, nulo para una fila todavía sin
     * grabar.
     * @return la ciudad.
     */
    private Ciudad ciudad(Long id) {
        Pais provincia = new Pais();
        provincia.setIdpais(7L);
        Ciudad ciudad = new Ciudad();
        ciudad.setIdciudad(id);
        ciudad.setProvincia(provincia);
        ciudad.setCodigo("ASU");
        ciudad.setNombreCorto("Asunción");
        return ciudad;
    }
}
