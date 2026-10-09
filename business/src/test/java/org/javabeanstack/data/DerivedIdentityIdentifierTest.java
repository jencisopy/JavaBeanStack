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

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.Test;

/**
 * Verifica que la clave que usa
 * {@link AbstractDAO#removeEjb(jakarta.persistence.EntityManager, org.javabeanstack.data.IDataRow)}
 * sirva también para las entidades con <b>identidad derivada</b>, es decir con
 * {@code @Id} sobre una asociación y sin {@code @IdClass} ni {@code @MapsId}.
 *
 * <p>
 * Es el mapeo de varias entidades de la aplicación ({@code Itemproducto},
 * {@code Itemservicio}, {@code Itemgasto} e {@code ItemmovimientoSifen} sobre
 * {@code @Id @ManyToOne}; {@code Personafisica} sobre {@code @Id @OneToOne}) y
 * de él depende que el borrado por el DAO siga funcionando, porque
 * {@code removeEjb} resuelve la clave con
 * {@link jakarta.persistence.PersistenceUnitUtil#getIdentifier(Object)} y con
 * ella busca la fila administrada. La duda que cierra este test es qué devuelve
 * ese método en ese mapeo: Hibernate envuelve el {@code @Id} de una asociación
 * en una clave compuesta no agregada
 * ({@code org.hibernate.type.EmbeddedComponentType} +
 * {@code NonAggregatedIdentifierMappingImpl}) y, al no haber clase contenedora
 * ({@code @IdClass}), devuelve <b>la propia entidad</b> como clave; esa clave se
 * descompone en el único valor JDBC de la columna, que es lo que {@code find}
 * pone en el {@code where}.
 * </p>
 *
 * <p>
 * El test no necesita servidor ni base de datos: arma la unidad de persistencia
 * solo con el metamodelo (dialecto explícito y sin acceso a los metadatos JDBC),
 * que es todo lo que necesita {@code getIdentifier}.
 * </p>
 *
 * @author app-developer (plan DELHIJA, §2.1 / I1-03, 2026-10-09)
 */
public class DerivedIdentityIdentifierTest {

    @Entity
    @Table(name = "derivedid_padre")
    public static class Padre {

        @Id
        private Long idpadre;

        public Long getIdpadre() {
            return idpadre;
        }

        public void setIdpadre(Long idpadre) {
            this.idpadre = idpadre;
        }
    }

    /** Mapeo de {@code Itemproducto} / {@code ItemmovimientoSifen}. */
    @Entity
    @Table(name = "derivedid_hija_many")
    public static class HijaManyToOne {

        @Id
        @Basic(optional = false)
        @JoinColumn(name = "idpadre", referencedColumnName = "idpadre", nullable = false)
        @ManyToOne
        private Padre padre;

        public Padre getPadre() {
            return padre;
        }

        public void setPadre(Padre padre) {
            this.padre = padre;
        }
    }

    /** Mapeo de {@code Personafisica}. */
    @Entity
    @Table(name = "derivedid_hija_one")
    public static class HijaOneToOne {

        @Id
        @Basic(optional = false)
        @JoinColumn(name = "idpadre", referencedColumnName = "idpadre")
        @OneToOne
        private Padre padre;

        public Padre getPadre() {
            return padre;
        }

        public void setPadre(Padre padre) {
            this.padre = padre;
        }
    }

    @Test
    public void testIdentidadDerivada() {
        Padre padre = new Padre();
        padre.setIdpadre(77L);
        HijaManyToOne hijaMany = new HijaManyToOne();
        hijaMany.setPadre(padre);
        HijaOneToOne hijaOne = new HijaOneToOne();
        hijaOne.setPadre(padre);

        try (SessionFactory sf = buildSessionFactory()) {
            //La clave es la propia fila...
            assertSame(hijaMany, sf.getPersistenceUnitUtil().getIdentifier(hijaMany),
                    "@Id @ManyToOne: getIdentifier tiene que devolver la propia fila");
            assertSame(hijaOne, sf.getPersistenceUnitUtil().getIdentifier(hijaOne),
                    "@Id @OneToOne: getIdentifier tiene que devolver la propia fila");
            //...y se descompone en el valor de la columna que find pone en el where.
            assertEquals(List.of(77L), valoresJdbcDeLaClave(sf, HijaManyToOne.class, hijaMany),
                    "@Id @ManyToOne: la clave no se descompone en el valor de la columna");
            assertEquals(List.of(77L), valoresJdbcDeLaClave(sf, HijaOneToOne.class, hijaOne),
                    "@Id @OneToOne: la clave no se descompone en el valor de la columna");
        }
    }

    /**
     * Arma una unidad de persistencia con las tres entidades, sin conexión: con
     * el dialecto declarado y el acceso a los metadatos JDBC apagado, Hibernate
     * no pide ninguna conexión al arrancar.
     */
    private SessionFactory buildSessionFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.dialect", "org.hibernate.dialect.SQLServerDialect");
        props.put("hibernate.boot.allow_jdbc_metadata_access", "false");
        props.put("hibernate.hbm2ddl.auto", "none");
        //En el classpath de prueba no hay proveedor JSONB y sin esto el arranque
        //muere al construir el mapeador JSON por omisión. Nada se serializa acá.
        FormatMapper sinFormato = new FormatMapper() {
            @Override
            public <T> T fromString(CharSequence cs, JavaType<T> javaType, WrapperOptions opts) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> String toString(T value, JavaType<T> javaType, WrapperOptions opts) {
                throw new UnsupportedOperationException();
            }
        };
        props.put("hibernate.type.json_format_mapper", sinFormato);
        props.put("hibernate.type.xml_format_mapper", sinFormato);

        return new MetadataSources(new StandardServiceRegistryBuilder().applySettings(props).build())
                .addAnnotatedClass(Padre.class)
                .addAnnotatedClass(HijaManyToOne.class)
                .addAnnotatedClass(HijaOneToOne.class)
                .buildMetadata()
                .buildSessionFactory();
    }

    /** Valores que Hibernate pone en el {@code where} al buscar por esa clave. */
    private List<Object> valoresJdbcDeLaClave(SessionFactory sf, Class<?> entidad, Object fila) {
        Object clave = sf.getPersistenceUnitUtil().getIdentifier(fila);
        List<Object> valores = new ArrayList<>();
        ((SessionFactoryImplementor) sf).getMappingMetamodel()
                .getEntityDescriptor(entidad)
                .getIdentifierMapping()
                .forEachJdbcValue(clave, valores, null,
                        (indice, lista, nada, valor, mapping) -> lista.add(valor), null);
        return valores;
    }
}
