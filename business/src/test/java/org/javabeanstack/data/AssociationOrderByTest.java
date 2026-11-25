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
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import org.javabeanstack.model.tables.Ciudad;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias de {@link AssociationOrderBy}: qué términos de un
 * {@code ORDER BY} se reescriben y con qué forma. Son de texto (la clase solo
 * mira las anotaciones por reflexión); que Hibernate 7 ejecute la forma
 * reescrita sin descartar filas se comprueba aparte, contra una base.
 *
 * @author Jorge Enciso
 */
public class AssociationOrderByTest {

    /** Entidad raíz de prueba: una asociación de cada clase. */
    static class Raiz {

        @Id
        Long idraiz;
        String nombre;
        @ManyToOne
        Opcional opcional;
        @ManyToOne(optional = false)
        Obligatoria obligatoria;
        @ManyToOne
        @JoinColumn(name = "idobligatoria2", nullable = false)
        Obligatoria obligatoriaPorColumna;
        @OneToOne
        Opcional unoAUno;
        @OneToMany
        List<Opcional> lista;
        @Embedded
        Incrustado incrustado;
        @OneToOne(mappedBy = "raiz")
        Detalle detalle;
        @ManyToOne
        @JoinColumn(name = "codigoopcional", referencedColumnName = "nombre")
        Opcional porNombre;
        // Misma columna que la clave, por otros dos caminos.
        @Column(name = "idraiz", insertable = false, updatable = false)
        Long idraizCopia;
        @OneToOne
        @JoinColumn(name = "idraiz", insertable = false, updatable = false)
        Opcional mismaColumna;
    }

    /** Lado dueño de la relación uno a uno con {@link Raiz}. */
    static class Detalle {

        @Id
        Long iddetalle;
        @OneToOne
        Raiz raiz;
    }

    /** Raíz con un atributo que se llama como el alias. */
    static class RaizConAtributoO {

        @ManyToOne
        Opcional o;
    }

    /** Sin atributo {@code @Id}. */
    static class SinClave {

        @ManyToOne
        Opcional opcional;
    }

    /** Clave compuesta por dos atributos {@code @Id}. */
    static class ClaveCompuesta {

        @Id
        Long parte1;
        @Id
        Long parte2;
        @ManyToOne
        Opcional opcional;
    }

    /** Clave incrustada ({@code @EmbeddedId}). */
    static class ClaveIncrustada {

        @EmbeddedId
        ClaveDosPartes id;
        String nombre;
    }

    @Embeddable
    static class ClaveDosPartes {

        static final long serialVersionUID = 1L;
        Long parte1;
        Long parte2;
    }

    /** Identidad derivada: un componente de la clave es una asociación. */
    static class ClaveDerivada {

        @Id
        Long parte1;
        @Id
        @ManyToOne
        Opcional padre;
        String nombre;
    }

    /** Clave incrustada que contiene una asociación ({@code @MapsId}). */
    static class ClaveIncrustadaConAsociacion {

        @EmbeddedId
        ClaveConAsociacion id;
        String nombre;
    }

    @Embeddable
    static class ClaveConAsociacion {

        Long parte1;
        @ManyToOne
        Opcional padre;
    }

    /** Clave incrustada cuya clase hereda un atributo. */
    static class ClaveIncrustadaHeredada {

        @EmbeddedId
        ClaveHija id;
    }

    static class ClaveBase {

        Long parte1;
    }

    @Embeddable
    static class ClaveHija extends ClaveBase {

        Long parte2;
    }

    /** Hereda las asociaciones de {@link Raiz}. */
    static class RaizHija extends Raiz {
    }

    static class Opcional {

        @Id
        Long idopcional;
        String nombre;
        @ManyToOne
        Opcional padre;
        @ManyToOne(optional = false)
        Obligatoria obligatoria;
        String año;
    }

    static class Obligatoria {

        @Id
        Long idobligatoria;
        String nombre;
        @ManyToOne(optional = false)
        Obligatoria otra;
        @ManyToOne
        Opcional opcional;
    }

    @Embeddable
    static class Incrustado {

        String codigo;
    }

    private static String sub(String alias, String path) {
        return "(select " + alias + "." + path + " from Raiz " + alias + " where " + alias + " = o)";
    }

    @Test
    public void testSimpleAttributesAreUntouched() {
        String order = "fecha desc, idraiz";
        assertSame(order, AssociationOrderBy.rewrite(Raiz.class, "o", order));
        assertFalse(AssociationOrderBy.needsRewrite(Raiz.class, "o", order));
        // Sin la clave, se agrega como desempate aunque no se reescriba nada.
        assertEquals("fecha desc, o.idraiz", AssociationOrderBy.rewrite(Raiz.class, "o", "fecha desc"));
        assertFalse(AssociationOrderBy.needsRewrite(Raiz.class, "o", "fecha desc"));
    }

    @Test
    public void testOptionalAssociationAscending() {
        assertEquals(sub("jbs_ob1", "opcional.nombre") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "opcional.nombre"));
        assertEquals(sub("jbs_ob1", "opcional.nombre") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", " opcional.nombre ASC "));
        assertTrue(AssociationOrderBy.needsRewrite(Raiz.class, "o", "opcional.nombre"));
    }

    @Test
    public void testOptionalAssociationDescending() {
        assertEquals(sub("jbs_ob1", "opcional.nombre") + " desc nulls first, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "opcional.nombre desc"));
    }

    @Test
    public void testExplicitNullsIsKept() {
        assertEquals(sub("jbs_ob1", "opcional.nombre") + " desc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "opcional.nombre desc nulls last"));
        assertEquals(sub("jbs_ob1", "opcional.nombre") + " asc nulls first, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "opcional.nombre NULLS FIRST"));
    }

    @Test
    public void testQualifiedWithRootAlias() {
        assertEquals(sub("jbs_ob1", "opcional.nombre") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "o.opcional.nombre asc"));
        // La raíz tiene un atributo llamado como el alias: «o» es el atributo.
        assertEquals("(select jbs_ob1.o.nombre from RaizConAtributoO jbs_ob1 where jbs_ob1 = o) asc nulls last",
                AssociationOrderBy.rewrite(RaizConAtributoO.class, "o", "o.nombre"));
    }

    @Test
    public void testNestedPath() {
        assertEquals(sub("jbs_ob1", "opcional.padre.nombre") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "opcional.padre.nombre"));
        // Una obligatoria seguida de una opcional también puede descartar filas.
        assertEquals(sub("jbs_ob1", "obligatoria.opcional.nombre") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "obligatoria.opcional.nombre"));
        // Una opcional seguida de una obligatoria, también.
        assertEquals(sub("jbs_ob1", "opcional.obligatoria.nombre") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "opcional.obligatoria.nombre"));
        // Pero opcional.padre.<clave> sí se reescribe: navega dos asociaciones.
        assertEquals(sub("jbs_ob1", "opcional.padre.idopcional") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "opcional.padre.idopcional"));
    }

    @Test
    public void testTieBreakByRootId() {
        // Ya ordena por la clave (con o sin alias, en cualquier dirección): no se repite.
        assertEquals(sub("jbs_ob1", "opcional.nombre") + " asc nulls last, o.idraiz desc",
                AssociationOrderBy.rewrite(Raiz.class, "o", "opcional.nombre, o.idraiz desc"));
        assertEquals("idraiz, " + sub("jbs_ob1", "opcional.nombre") + " asc nulls last",
                AssociationOrderBy.rewrite(Raiz.class, "o", "idraiz, opcional.nombre"));
        // La clave en el medio también cuenta: no se repite al final.
        assertEquals("nombre, idraiz desc, fecha", AssociationOrderBy.rewrite(Raiz.class, "o", "nombre, idraiz desc, fecha"));
        // Sin @Id no hay desempate que agregar.
        assertEquals("(select jbs_ob1.opcional.nombre from SinClave jbs_ob1 where jbs_ob1 = o) asc nulls last",
                AssociationOrderBy.rewrite(SinClave.class, "SinClave", "o", "opcional.nombre"));
        assertEquals("nombre", AssociationOrderBy.rewrite(SinClave.class, "o", "nombre"));
    }

    @Test
    public void testTieBreakWithCompositeKeys() {
        // @IdClass (varios @Id): todos los componentes, en orden de declaración.
        assertEquals("(select jbs_ob1.opcional.nombre from ClaveCompuesta jbs_ob1 where jbs_ob1 = o) asc nulls last,"
                + " o.parte1, o.parte2",
                AssociationOrderBy.rewrite(ClaveCompuesta.class, "o", "opcional.nombre"));
        // Los que ya figuran no se repiten.
        assertEquals("parte2 desc, nombre, o.parte1",
                AssociationOrderBy.rewrite(ClaveCompuesta.class, "o", "parte2 desc, nombre"));
        // @EmbeddedId: los atributos de la clase incrustada.
        assertEquals("nombre, o.id.parte1, o.id.parte2",
                AssociationOrderBy.rewrite(ClaveIncrustada.class, "o", "nombre"));
        assertEquals("nombre, id.parte2, o.id.parte1",
                AssociationOrderBy.rewrite(ClaveIncrustada.class, "o", "nombre, id.parte2"));
        // El @EmbeddedId entero ya ordena por todos sus componentes.
        assertEquals("o.id desc, nombre", AssociationOrderBy.rewrite(ClaveIncrustada.class, "o", "o.id desc, nombre"));
        // Los atributos heredados de la clase incrustada también son componentes.
        assertEquals("nombre, o.id.parte1, o.id.parte2",
                AssociationOrderBy.rewrite(ClaveIncrustadaHeredada.class, "o", "nombre"));
        // No es seguro: un componente que es asociación (identidad derivada) o una
        // clase incrustada con una asociación. No se agrega nada.
        assertEquals("nombre", AssociationOrderBy.rewrite(ClaveDerivada.class, "o", "nombre"));
        assertEquals("nombre", AssociationOrderBy.rewrite(ClaveIncrustadaConAsociacion.class, "o", "nombre"));
    }

    @Test
    public void testAddKeyTieBreakDoesNotRewrite() {
        assertEquals("opcional.nombre, o.idraiz",
                AssociationOrderBy.addKeyTieBreak(Raiz.class, "o", "opcional.nombre"));
        assertEquals("idraiz", AssociationOrderBy.addKeyTieBreak(Raiz.class, "o", "idraiz"));
        assertEquals("", AssociationOrderBy.addKeyTieBreak(Raiz.class, "o", ""));
        assertNull(AssociationOrderBy.addKeyTieBreak(Raiz.class, "o", null));
        assertEquals("a, (b", AssociationOrderBy.addKeyTieBreak(Raiz.class, "o", "a, (b"));
    }

    @Test
    public void testForeignKeyIdOnlyFromOwningSide() {
        // Lado inverso (mappedBy): no hay clave foránea en la raíz, hay join.
        assertEquals(sub("jbs_ob1", "detalle.iddetalle") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "detalle.iddetalle"));
        // Unida por otra columna del destino: tampoco se lee de la clave foránea.
        assertEquals(sub("jbs_ob1", "porNombre.idopcional") + " desc nulls first, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "porNombre.idopcional desc"));
    }

    @Test
    public void testNonAsciiIdentifiers() {
        assertEquals(sub("jbs_ob1", "opcional.año") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "opcional.año"));
        assertTrue(AssociationOrderBy.needsRewrite(Raiz.class, "o", "o.opcional.año desc"));
    }

    @Test
    public void testKeyInsideAnExpressionDoesNotCount() {
        assertEquals("idraiz % 2, o.idraiz", AssociationOrderBy.rewrite(Raiz.class, "o", "idraiz % 2"));
        assertEquals("abs(o.idraiz) desc, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "abs(o.idraiz) desc"));
        assertEquals("o.idraiz desc nulls last, nombre",
                AssociationOrderBy.rewrite(Raiz.class, "o", "o.idraiz desc nulls last, nombre"));
    }

    @Test
    public void testKeyColumnAlreadyOrderedByAnotherPath() {
        // Repetir la columna de la clave en el ORDER BY es el error 169 de SQL Server.
        assertEquals("idraizCopia desc", AssociationOrderBy.rewrite(Raiz.class, "o", "idraizCopia desc"));
        assertEquals("o.mismaColumna.idopcional",
                AssociationOrderBy.rewrite(Raiz.class, "o", "o.mismaColumna.idopcional"));
        // Otra columna de la misma asociación no la cubre.
        assertEquals("opcional.idopcional, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "opcional.idopcional"));
    }

    @Test
    public void testOneToOneIsRewritten() {
        assertEquals(sub("jbs_ob1", "unoAUno.nombre") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "unoAUno.nombre"));
    }

    @Test
    public void testMandatoryAssociationsAreUntouched() {
        String order = "obligatoria.nombre, obligatoriaPorColumna.nombre desc, obligatoria.otra.nombre";
        assertEquals(order + ", o.idraiz", AssociationOrderBy.rewrite(Raiz.class, "o", order));
        assertFalse(AssociationOrderBy.needsRewrite(Raiz.class, "o", order));
    }

    @Test
    public void testForeignKeyIdIsUntouched() {
        String order = "opcional.idopcional desc";
        assertEquals(order + ", o.idraiz", AssociationOrderBy.rewrite(Raiz.class, "o", order));
    }

    @Test
    public void testNonAssociationPathsAreUntouched() {
        for (String order : new String[]{"incrustado.codigo", "lista.nombre", "noexiste.nombre",
            // JPQL distingue mayúsculas: «Opcional» no es el atributo «opcional».
            "Opcional.nombre"}) {
            assertEquals(order + ", o.idraiz", AssociationOrderBy.rewrite(Raiz.class, "o", order), order);
            assertFalse(AssociationOrderBy.needsRewrite(Raiz.class, "o", order), order);
        }
        // El atributo final se deja a Hibernate: si no existe, que lo diga él.
        assertEquals(sub("jbs_ob1", "opcional.noexiste") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "opcional.noexiste"));
    }

    @Test
    public void testExpressionsAreUntouched() {
        String[] orders = {"upper(opcional.nombre)", "1", "opcional", "o.opcional",
            "coalesce(opcional.nombre, nombre) desc", "opcional.nombre collate x"};
        for (String order : orders) {
            assertEquals(order + ", o.idraiz", AssociationOrderBy.rewrite(Raiz.class, "o", order), order);
        }
    }

    @Test
    public void testSeveralTerms() {
        assertEquals("fecha desc, " + sub("jbs_ob1", "opcional.nombre") + " asc nulls last, "
                + "obligatoria.nombre, " + sub("jbs_ob2", "opcional.padre.nombre") + " desc nulls first, idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o",
                        "fecha desc, opcional.nombre,obligatoria.nombre , opcional.padre.nombre desc, idraiz"));
        // Una función con comas no se parte.
        assertEquals("coalesce(nombre, 'a,b'), " + sub("jbs_ob1", "opcional.nombre") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "coalesce(nombre, 'a,b'), opcional.nombre"));
    }

    @Test
    public void testInheritedAssociation() {
        assertEquals("(select jbs_ob1.opcional.nombre from RaizHija jbs_ob1 where jbs_ob1 = o) asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(RaizHija.class, "o", "opcional.nombre"));
    }

    @Test
    public void testExplicitEntityNameAndAlias() {
        assertEquals("(select jbs_ob1.opcional.nombre from MiEntidad jbs_ob1 where jbs_ob1 = r) desc nulls first, r.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "MiEntidad", "r", "r.opcional.nombre desc"));
    }

    @Test
    public void testFrameworkTestEntity() {
        // Entidad de prueba del módulo: pais y region son @ManyToOne sin optional.
        assertEquals("(select jbs_ob1.pais.nombre from Ciudad jbs_ob1 where jbs_ob1 = o) asc nulls last, o.idciudad",
                AssociationOrderBy.rewrite(Ciudad.class, "o", "pais.nombre"));
        assertEquals("codigo, nombreCorto, o.idciudad",
                AssociationOrderBy.rewrite(Ciudad.class, "o", "codigo, nombreCorto"));
    }

    @Test
    public void testNullAndEmptyArguments() {
        assertNull(AssociationOrderBy.rewrite(Raiz.class, "o", null));
        assertEquals("", AssociationOrderBy.rewrite(Raiz.class, "o", ""));
        assertEquals("  ", AssociationOrderBy.rewrite(Raiz.class, "o", "  "));
        assertEquals("opcional.nombre", AssociationOrderBy.rewrite(null, "o", "opcional.nombre"));
        assertEquals("opcional.nombre", AssociationOrderBy.rewrite(Raiz.class, "", "opcional.nombre"));
        assertEquals("opcional.nombre", AssociationOrderBy.rewrite(Raiz.class, null, "o", "opcional.nombre"));
        // Paréntesis o comillas sin cerrar: queda como está.
        assertEquals("opcional.nombre, (x", AssociationOrderBy.rewrite(Raiz.class, "o", "opcional.nombre, (x"));
    }

    @Test
    public void testSplitTopLevel() {
        assertEquals(Arrays.asList("a", " f(b, c)", " 'x,y'"), AssociationOrderBy.splitTopLevel("a, f(b, c), 'x,y'"));
        assertNull(AssociationOrderBy.splitTopLevel("a)"));
        assertNull(AssociationOrderBy.splitTopLevel("'a"));
    }
}
