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

import java.util.Arrays;

import org.javabeanstack.model.appcatalog.AppResource;
import org.javabeanstack.model.tables.Barrio;
import org.javabeanstack.model.tables.Ciudad;
import org.javabeanstack.model.tables.Pais;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Traducción columna→atributo de la clave única (plan {@code QUERYUK},
 * hallazgo {@code I5-12}).
 *
 * <p>Es un test PURO: no necesita servidor de aplicaciones ni base de datos.
 * Cubre las cuatro familias de la clave única —columna igual al atributo,
 * columna escrita distinto, clave foránea mapeada solo como asociación y
 * columna que la entidad no mapea— más las dos trampas de precedencia y las
 * exclusiones ({@code static}, {@code @Transient}).</p>
 *
 * @author Jorge Enciso
 */
@DisplayName("DataInfo: clave única con nombres de atributo (QUERYUK)")
public class DataInfoQueryUkTest {

    @Test
    @DisplayName("Entidad cuya columna coincide con el atributo: misma sentencia de siempre")
    public void testSentenciaSinCambioCuandoColumnaEsIgualAlAtributo() {
        //Es la sentencia que fija DataInfoTest.test08: el formato no cambió.
        String expResult = "select o from AppResource o  where  o.code = :code";
        assertEquals(expResult, DataInfo.createQueryUkCommand(new AppResource()));
        assertEquals(expResult, new AppResource().getQueryUK());
    }

    @Test
    @DisplayName("Asociación y atributo renombrado: la sentencia usa los atributos")
    public void testSentenciaConAsociacionYAtributoRenombrado() {
        String expResult = "select o from Ciudad o  where  o.codigo = :codigo"
                + " and  o.provincia = :provincia and  o.nombreCorto = :nombreCorto";
        assertEquals(expResult, DataInfo.createQueryUkCommand(new Ciudad()));
        assertEquals(expResult, new Ciudad().getQueryUK());
    }

    @Test
    @DisplayName("getUniqueFields sigue devolviendo las COLUMNAS")
    public void testGetUniqueFieldsDevuelveColumnas() {
        assertArrayEquals(new String[]{"codigo", "idprovincia", "nombre_corto"},
                DataInfo.getUniqueFields(Ciudad.class));
    }

    @Test
    @DisplayName("getUniqueKeyAttributes devuelve los ATRIBUTOS, en orden")
    public void testGetUniqueKeyAttributes() {
        assertArrayEquals(new String[]{"codigo", "provincia", "nombreCorto"},
                DataInfo.getUniqueKeyAttributes(Ciudad.class));
    }

    @Test
    @DisplayName("Columna que la entidad no mapea: sin clave única y sin sentencia")
    public void testColumnaSinAtributo() {
        //La columna "zona" de Barrio solo existe como campo estático.
        assertNull(DataInfo.getAttributeByColumnName(Barrio.class, "zona"));
        assertNull(DataInfo.getUniqueKeyAttributes(Barrio.class));
        assertNull(DataInfo.createQueryUkCommand(new Barrio()));
        assertNull(new Barrio().getQueryUK());
        //La anotación sí conserva las columnas, que son las de la base.
        assertArrayEquals(new String[]{"codigo", "zona"}, DataInfo.getUniqueFields(Barrio.class));
    }

    @Test
    @DisplayName("La búsqueda no distingue mayúsculas pero respeta la grafía del atributo")
    public void testBusquedaSinDistinguirMayusculas() {
        assertEquals("nombreCorto", DataInfo.getAttributeByColumnName(Ciudad.class, "NOMBRE_CORTO"));
        assertEquals("nombreCorto", DataInfo.getAttributeByColumnName(Ciudad.class, "nombre_corto"));
        assertEquals("provincia", DataInfo.getAttributeByColumnName(Ciudad.class, "IdProvincia"));
    }

    @Test
    @DisplayName("Precedencia: el escalar anotado gana a la asociación con la misma columna")
    public void testPrecedenciaEscalarSobreAsociacion() {
        assertEquals("idpais", DataInfo.getAttributeByColumnName(Ciudad.class, "idpais"));
        //Y una columna que no mapea nadie sigue devolviendo nulo.
        assertNull(DataInfo.getAttributeByColumnName(Ciudad.class, "idpais_inexistente"));
    }

    @Test
    @DisplayName("Precedencia: @Column gana al atributo homónimo que es una asociación")
    public void testPrecedenciaColumnaSobreAtributoHomonimo() {
        //Caso Ctbivapyfrmdetalle: la columna "zonaurbana" la mapea
        //zonaurbanaCodigo, y existe ademas una asociación llamada zonaurbana.
        //zonaurbanaCodigo es de SOLO LECTURA y la asociación es escribible: aun
        //así gana el escalar, porque la asociación mapea idzonaurbana y coincide
        //solo por el nombre. Es la trampa de D16 (plan QUERYUK, cierre).
        assertEquals("zonaurbanaCodigo", DataInfo.getAttributeByColumnName(Ciudad.class, "zonaurbana"));
    }

    @Test
    @DisplayName("D16: la asociación escribible gana al escalar de solo lectura")
    public void testPrecedenciaAsociacionEscribibleSobreEscalarSoloLectura() {
        //Caso Documentoctrl: la columna la mapean el escalar RO idregion y la
        //asociación escribible region; en el alta el escalar viene nulo y el
        //control de duplicados quedaba inerte (M4-01).
        assertEquals("region", DataInfo.getAttributeByColumnName(Ciudad.class, "idregion"));
    }

    @Test
    @DisplayName("D16: el escalar escribible gana a la asociación de solo lectura")
    public void testPrecedenciaEscalarEscribibleSobreAsociacionSoloLectura() {
        assertEquals("idzona", DataInfo.getAttributeByColumnName(Ciudad.class, "idzona"));
    }

    @Test
    @DisplayName("isUniqueKey responde por columna y por atributo")
    public void testIsUniqueKeyPorColumnaYPorAtributo() {
        //Columnas, como siempre (DataInfoTest.test10).
        assertTrue(DataInfo.isUniqueKey(Ciudad.class, "nombre_corto"));
        assertTrue(DataInfo.isUniqueKey(Ciudad.class, "NOMBRE_CORTO"));
        assertTrue(DataInfo.isUniqueKey(Ciudad.class, "idprovincia"));
        //Atributos, que es el nombre que conoce el formulario (M0-03).
        assertTrue(DataInfo.isUniqueKey(Ciudad.class, "nombreCorto"));
        assertTrue(DataInfo.isUniqueKey(Ciudad.class, "provincia"));
        assertTrue(DataInfo.isUniqueKey(Ciudad.class, "codigo"));
        //Lo que no forma parte de la clave sigue dando falso.
        assertFalse(DataInfo.isUniqueKey(Ciudad.class, "descripcion"));
        assertFalse(DataInfo.isUniqueKey(Ciudad.class, "idpais"));
    }

    @Test
    @DisplayName("Los campos static y @Transient no mapean columnas")
    public void testCamposExcluidos() {
        assertNull(DataInfo.getAttributeByColumnName(Ciudad.class, "descripcion"));
        assertNull(DataInfo.getAttributeByColumnName(Barrio.class, "zona"));
    }

    @Test
    @DisplayName("El resultado se calcula una sola vez por clase y no expone la caché")
    public void testCache() {
        String[] primera = DataInfo.getUniqueKeyAttributes(Ciudad.class);
        String[] segunda = DataInfo.getUniqueKeyAttributes(Ciudad.class);
        assertNotNull(primera);
        //El plan pedía la MISMA instancia (CA1-02 e) como prueba de que la
        //segunda llamada sale de la caché. Se cambió a copia (M4-02): el
        //arreglo cacheado lo comparten todos los hilos del servidor y bastaba
        //que un llamador lo ordenara o lo pisara para corromper la clave única
        //de esa entidad para todos. La caché se sigue verificando —mismo
        //contenido— y además se comprueba que mutar lo devuelto no la toque.
        assertArrayEquals(primera, segunda, "la segunda llamada tiene que venir de la caché");
        assertNotSame(primera, segunda, "no se devuelve el arreglo interno de la caché");
        primera[0] = "CORROMPIDO";
        assertArrayEquals(segunda, DataInfo.getUniqueKeyAttributes(Ciudad.class),
                "mutar lo devuelto no puede afectar a la caché");
    }

    @Test
    @DisplayName("El valor del parámetro de una asociación es el objeto asociado")
    public void testValorDeLaAsociacion() {
        Ciudad ciudad = new Ciudad();
        Pais provincia = new Pais();
        provincia.setIdpais(10L);
        ciudad.setProvincia(provincia);
        ciudad.setCodigo("ASU");
        ciudad.setNombreCorto("Asunción");

        for (String attribute : DataInfo.getUniqueKeyAttributes(Ciudad.class)) {
            assertNotNull(ciudad.getValue(attribute), "sin valor para " + attribute);
        }
        assertSame(provincia, ciudad.getValue("provincia"));
    }

    @Test
    @DisplayName("Entidad sin clave única: sin sentencia")
    public void testEntidadSinClaveUnica() {
        assertNull(DataInfo.getUniqueKeyAttributes(SinClaveUnica.class));
    }

    /**
     * Entidad mínima sin {@code uniqueConstraints}, para el caso «no declara
     * clave única» (que es el de 436 entidades de Maker).
     */
    @jakarta.persistence.Entity
    @jakarta.persistence.Table(name = "sinclaveunica")
    public static class SinClaveUnica extends DataRow {

        @jakarta.persistence.Id
        @jakarta.persistence.Column(name = "idsinclaveunica")
        private Long idsinclaveunica;

        public Long getIdsinclaveunica() {
            return idsinclaveunica;
        }

        public void setIdsinclaveunica(Long idsinclaveunica) {
            this.idsinclaveunica = idsinclaveunica;
        }
    }

    @Test
    @DisplayName("Las columnas de la clave única no se repiten en la sentencia")
    public void testSinColumnasRepetidas() {
        String hql = DataInfo.createQueryUkCommand(new Ciudad());
        long veces = Arrays.stream(hql.split(" and ")).count();
        assertEquals(3, veces, hql);
    }
}
