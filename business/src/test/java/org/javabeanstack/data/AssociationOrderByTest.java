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
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumns;

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
        // Solo nullable = false en la columna: cuenta como opcional.
        @ManyToOne
        @JoinColumn(name = "idobligatoria2", nullable = false)
        Obligatoria obligatoriaPorColumna;
        @ManyToOne(optional = false)
        @JoinColumn(name = "idobligatoria3", nullable = false)
        Obligatoria obligatoriaExplicita;
        @OneToOne
        @JoinColumn(name = "idobligatoria4", nullable = false)
        Obligatoria unoAUnoPorColumna;
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
        // Casos que no se correlacionan por la clave foránea (vuelven a la raíz).
        @ManyToOne
        @JoinColumns({
            @JoinColumn(name = "idparte1", referencedColumnName = "parte1"),
            @JoinColumn(name = "idparte2", referencedColumnName = "parte2")})
        ClaveCompuesta compuesta;
        @ManyToOne
        @JoinTable(name = "raiz_opcional")
        Opcional porTabla;
        @OneToOne
        @PrimaryKeyJoinColumn
        Opcional porClavePrimaria;
        @ManyToOne
        ClaveIncrustada incrustada;
        // Destino con nombre de entidad propio.
        @ManyToOne
        Nombrada nombrada;
        // targetEntity: el destino no es el tipo del atributo.
        @ManyToOne(targetEntity = Opcional.class)
        Object porTipo;
    }

    /** Entidad destino con {@code @Entity(name)}: la subconsulta usa ese nombre. */
    @Entity(name = "EntidadNombrada")
    static class Nombrada {

        @Id
        Long idnombrada;
        String nombre;
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

    /** Identidad derivada de una única asociación a uno. */
    static class ClaveUnoAUno {

        @Id
        @OneToOne
        @JoinColumn(name = "idorigen", referencedColumnName = "idopcional")
        Opcional origen;
        String monto;
        // Misma columna que la clave, por otro camino (error 169).
        @Column(name = "idorigen", insertable = false, updatable = false)
        Long idorigenCopia;
    }

    /** Identidad derivada por un {@code @ManyToOne}. */
    static class ClaveMuchosAUno {

        @Id
        @ManyToOne
        @JoinColumn(name = "idpadre")
        Opcional padre;
        String nombre;
    }

    /** Identidad derivada sin {@code @JoinColumn}: no se conoce la columna. */
    static class ClaveUnoAUnoSinColumna {

        @Id
        @OneToOne
        Opcional origen;
        String monto;
    }

    /** Identidad derivada unida por otra columna del destino. */
    static class ClaveUnoAUnoPorOtraColumna {

        @Id
        @OneToOne
        @JoinColumn(name = "codigo", referencedColumnName = "nombre")
        Opcional origen;
        String monto;
    }

    /** Destino de la entrega: la compra. */
    static class Purchase {

        @Id
        @Column(name = "idpurchase")
        Long idpurchase;
        String number;
    }

    /** Entrega cuya clave es la compra ({@code @Id @OneToOne purchase}). */
    static class Delivery {

        @Id
        @JoinColumn(name = "idpurchase", referencedColumnName = "idpurchase", nullable = false)
        @OneToOne()
        Purchase purchase;
        String monto;
        String fecha;
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

    /**
     * Raíz para las guardas de la correlación por la clave foránea: una
     * asociación por cada motivo para volver a la raíz, cada una con un solo
     * motivo, y una de control que sí se correlaciona.
     */
    static class RaizGuardas {

        @Id
        Long idraiz;
        // Control: columna simple hacia la clave simple del destino.
        @ManyToOne
        Opcional opcional;
        // Destino con la clave anotada en el método, no en el atributo.
        @ManyToOne
        ClaveEnMetodo porMetodo;
        // Destinos con identidad derivada: dos @Id con una asociación, o una sola asociación como clave.
        @ManyToOne
        ClaveDerivada derivada;
        @ManyToOne
        ClaveMuchosAUno derivadaUnaAsociacion;
        // Destino sin ningún @Id.
        @ManyToOne
        SinClave sinClave;
        // Destino con @IdClass unido por una sola columna: solo falla la clave del destino.
        @ManyToOne
        @JoinColumn(name = "idparte1", referencedColumnName = "parte1")
        ConIdClass conIdClass;
        // Dos columnas de unión hacia un destino de clave simple, sin referencedColumnName: solo
        // falla la cantidad de columnas.
        @ManyToOne
        @JoinColumns({
            @JoinColumn(name = "idopcional1"),
            @JoinColumn(name = "idopcional2")})
        Opcional dosColumnas;
        // targetEntity en un @OneToOne: el tipo del atributo tiene clave simple, pero el destino es otro.
        @OneToOne(targetEntity = Obligatoria.class)
        Opcional unoAUnoPorTipo;
        @ManyToOne(targetEntity = Obligatoria.class)
        Opcional muchosAUnoPorTipo;
        // Clave primaria compartida declarada con la forma plural.
        @OneToOne
        @PrimaryKeyJoinColumns({
            @PrimaryKeyJoinColumn(name = "idraiz", referencedColumnName = "idopcional")})
        Opcional porClavesPrimarias;
    }

    /** Destino con la clave en el método (acceso por propiedad). */
    static class ClaveEnMetodo {

        Long idclave;
        String nombre;

        @Id
        public Long getIdclave() {
            return idclave;
        }
    }

    /** Destino con {@code @IdClass}: dos atributos {@code @Id} simples. */
    @IdClass(ClaveDosPartes.class)
    static class ConIdClass {

        @Id
        Long parte1;
        @Id
        Long parte2;
        String nombre;
    }

    /** Forma de respaldo: subconsulta correlacionada sobre la raíz. */
    private static String sub(String alias, String path) {
        return sub("Raiz", alias, path);
    }

    /** Forma de respaldo sobre una raíz dada. */
    private static String sub(String root, String alias, String path) {
        return "(select " + alias + "." + path + " from " + root + " " + alias + " where " + alias + " = o)";
    }

    /** Forma por la clave foránea del primer salto. */
    private static String fk(String alias, String target, String path, String association) {
        return "(select " + alias + "." + path + " from " + target + " " + alias + " where " + alias
                + " = o." + association + ")";
    }

    @Test
    public void testSimpleAttributesAreUntouched() {
        String order = "fecha desc, idraiz";
        assertSame(order, AssociationOrderBy.rewrite(Raiz.class, "o", order));
        assertFalse(AssociationOrderBy.needsRewrite(Raiz.class, "o", order));
        // Sin la clave, se agrega como desempate aunque no se reescriba nada.
        assertEquals("fecha desc, o.idraiz desc", AssociationOrderBy.rewrite(Raiz.class, "o", "fecha desc"));
        assertFalse(AssociationOrderBy.needsRewrite(Raiz.class, "o", "fecha desc"));
    }

    @Test
    public void testOptionalAssociationAscending() {
        assertEquals(fk("jbs_ob1", "Opcional", "nombre", "opcional") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "opcional.nombre"));
        assertEquals(fk("jbs_ob1", "Opcional", "nombre", "opcional") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", " opcional.nombre ASC "));
        assertTrue(AssociationOrderBy.needsRewrite(Raiz.class, "o", "opcional.nombre"));
    }

    @Test
    public void testOptionalAssociationDescending() {
        assertEquals(fk("jbs_ob1", "Opcional", "nombre", "opcional") + " desc nulls first, o.idraiz desc",
                AssociationOrderBy.rewrite(Raiz.class, "o", "opcional.nombre desc"));
    }

    @Test
    public void testExplicitNullsIsKept() {
        assertEquals(fk("jbs_ob1", "Opcional", "nombre", "opcional") + " desc nulls last, o.idraiz desc",
                AssociationOrderBy.rewrite(Raiz.class, "o", "opcional.nombre desc nulls last"));
        assertEquals(fk("jbs_ob1", "Opcional", "nombre", "opcional") + " asc nulls first, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "opcional.nombre NULLS FIRST"));
    }

    @Test
    public void testQualifiedWithRootAlias() {
        assertEquals(fk("jbs_ob1", "Opcional", "nombre", "opcional") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "o.opcional.nombre asc"));
        // La raíz tiene un atributo llamado como el alias: «o» es el atributo.
        assertEquals("(select jbs_ob1.nombre from Opcional jbs_ob1 where jbs_ob1 = o.o) asc nulls last",
                AssociationOrderBy.rewrite(RaizConAtributoO.class, "o", "o.nombre"));
    }

    @Test
    public void testNestedPath() {
        // Por la clave foránea del primer salto; el resto se navega adentro.
        assertEquals(fk("jbs_ob1", "Opcional", "padre.nombre", "opcional") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "opcional.padre.nombre"));
        // Una obligatoria seguida de una opcional también puede descartar filas.
        assertEquals(fk("jbs_ob1", "Obligatoria", "opcional.nombre", "obligatoria") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "obligatoria.opcional.nombre"));
        // Una opcional seguida de una obligatoria, también.
        assertEquals(fk("jbs_ob1", "Opcional", "obligatoria.nombre", "opcional") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "opcional.obligatoria.nombre"));
        // Pero opcional.padre.<clave> sí se reescribe: navega dos asociaciones.
        assertEquals(fk("jbs_ob1", "Opcional", "padre.idopcional", "opcional") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "opcional.padre.idopcional"));
    }

    @Test
    public void testTieBreakByRootId() {
        // Ya ordena por la clave (con o sin alias, en cualquier dirección): no se repite.
        assertEquals(fk("jbs_ob1", "Opcional", "nombre", "opcional") + " asc nulls last, o.idraiz desc",
                AssociationOrderBy.rewrite(Raiz.class, "o", "opcional.nombre, o.idraiz desc"));
        assertEquals("idraiz, " + fk("jbs_ob1", "Opcional", "nombre", "opcional") + " asc nulls last",
                AssociationOrderBy.rewrite(Raiz.class, "o", "idraiz, opcional.nombre"));
        // La clave en el medio también cuenta: no se repite al final.
        assertEquals("nombre, idraiz desc, fecha", AssociationOrderBy.rewrite(Raiz.class, "o", "nombre, idraiz desc, fecha"));
        // Sin @Id no hay desempate que agregar.
        assertEquals(fk("jbs_ob1", "Opcional", "nombre", "opcional") + " asc nulls last",
                AssociationOrderBy.rewrite(SinClave.class, "SinClave", "o", "opcional.nombre"));
        assertEquals("nombre", AssociationOrderBy.rewrite(SinClave.class, "o", "nombre"));
    }

    @Test
    public void testTieBreakWithCompositeKeys() {
        // @IdClass (varios @Id): todos los componentes, en orden de declaración.
        assertEquals(fk("jbs_ob1", "Opcional", "nombre", "opcional") + " asc nulls last, o.parte1, o.parte2",
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

    /**
     * Con identidad derivada de una sola asociación a uno, el
     * desempate es la clave del destino por esa asociación, en la dirección
     * del último término.
     */
    @Test
    public void testDerivedIdentityTieBreak() {
        assertEquals("monto, o.origen.idopcional", AssociationOrderBy.rewrite(ClaveUnoAUno.class, "o", "monto"));
        assertEquals("monto desc, o.origen.idopcional desc",
                AssociationOrderBy.rewrite(ClaveUnoAUno.class, "o", "monto desc"));
        assertEquals("nombre, o.padre.idopcional", AssociationOrderBy.rewrite(ClaveMuchosAUno.class, "o", "nombre"));
        // Ya figura: por el mismo camino o por otra columna mapeada igual (error 169).
        assertEquals("origen.idopcional desc", AssociationOrderBy.rewrite(ClaveUnoAUno.class, "o", "origen.idopcional desc"));
        assertEquals("o.origen.idopcional", AssociationOrderBy.rewrite(ClaveUnoAUno.class, "o", "o.origen.idopcional"));
        assertEquals("idorigenCopia desc", AssociationOrderBy.rewrite(ClaveUnoAUno.class, "o", "idorigenCopia desc"));
        // Sin la columna de unión declarada o unida por otra columna: como antes, sin desempate.
        assertEquals("monto", AssociationOrderBy.rewrite(ClaveUnoAUnoSinColumna.class, "o", "monto"));
        assertEquals("monto", AssociationOrderBy.rewrite(ClaveUnoAUnoPorOtraColumna.class, "o", "monto"));
        // Dos @Id con una asociación (ClaveDerivada) sigue sin desempate.
        assertEquals("nombre", AssociationOrderBy.rewrite(ClaveDerivada.class, "o", "nombre"));
        assertFalse(AssociationOrderBy.needsRewrite(ClaveUnoAUno.class, "o", "monto"));
    }

    /** Entrega con clave {@code @Id @OneToOne purchase}. */
    @Test
    public void testDeliveryTieBreak() {
        assertEquals("monto desc, o.purchase.idpurchase desc",
                AssociationOrderBy.rewrite(Delivery.class, "o", "monto desc"));
        assertEquals("fecha, monto, o.purchase.idpurchase",
                AssociationOrderBy.rewrite(Delivery.class, "o", "fecha, monto"));
        // Ordenar por el número de la compra navega la clave: se reescribe y lleva el desempate.
        String rewritten = AssociationOrderBy.rewrite(Delivery.class, "o", "purchase.number desc");
        assertTrue(rewritten.endsWith(", o.purchase.idpurchase desc"), rewritten);
        assertEquals(1, rewritten.split("purchase.idpurchase", -1).length - 1, rewritten);
    }

    /**
     * Ordenar por la asociación que es la clave ya ordena por su columna de
     * unión: no se agrega el desempate, que repetiría la columna (error 169).
     */
    @Test
    public void testDerivedKeyAssociationTermIsTieBreak() {
        assertEquals("purchase desc", AssociationOrderBy.rewrite(Delivery.class, "o", "purchase desc"));
        assertEquals("fecha, o.purchase", AssociationOrderBy.rewrite(Delivery.class, "o", "fecha, o.purchase"));
        assertEquals("origen", AssociationOrderBy.rewrite(ClaveUnoAUno.class, "o", "origen"));
        assertEquals("padre desc", AssociationOrderBy.rewrite(ClaveMuchosAUno.class, "o", "padre desc"));
        assertEquals("padre desc", AssociationOrderBy.addKeyTieBreak(ClaveMuchosAUno.class, "o", "padre desc"));
        // Sin columna de unión declarada no se sabe: sigue sin desempate, como antes.
        assertEquals("origen", AssociationOrderBy.rewrite(ClaveUnoAUnoSinColumna.class, "o", "origen"));
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
        assertEquals(sub("jbs_ob1", "porNombre.idopcional") + " desc nulls first, o.idraiz desc",
                AssociationOrderBy.rewrite(Raiz.class, "o", "porNombre.idopcional desc"));
    }

    @Test
    public void testNonAsciiIdentifiers() {
        assertEquals(fk("jbs_ob1", "Opcional", "año", "opcional") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "opcional.año"));
        assertTrue(AssociationOrderBy.needsRewrite(Raiz.class, "o", "o.opcional.año desc"));
    }

    @Test
    public void testKeyInsideAnExpressionDoesNotCount() {
        assertEquals("idraiz % 2, o.idraiz", AssociationOrderBy.rewrite(Raiz.class, "o", "idraiz % 2"));
        assertEquals("abs(o.idraiz) desc, o.idraiz desc",
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
        // La asociación entera ordena por su columna de unión: si es la de la clave, la cubre.
        assertEquals("mismaColumna desc", AssociationOrderBy.rewrite(Raiz.class, "o", "mismaColumna desc"));
        assertEquals("opcional desc, o.idraiz desc", AssociationOrderBy.rewrite(Raiz.class, "o", "opcional desc"));
        // Otra columna de la misma asociación no la cubre.
        assertEquals("opcional.idopcional, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "opcional.idopcional"));
    }

    @Test
    public void testOneToOneIsRewritten() {
        assertEquals(fk("jbs_ob1", "Opcional", "nombre", "unoAUno") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "unoAUno.nombre"));
    }

    @Test
    public void testMandatoryAssociationsAreUntouched() {
        String order = "obligatoria.nombre, obligatoriaExplicita.nombre desc, obligatoria.otra.nombre";
        assertEquals(order + ", o.idraiz", AssociationOrderBy.rewrite(Raiz.class, "o", order));
        assertFalse(AssociationOrderBy.needsRewrite(Raiz.class, "o", order));
    }

    /**
     * {@code nullable = false} en la columna de unión, sin
     * {@code optional = false}, no hace obligatoria la asociación: la base
     * puede tener nulos (una factura sin {@code status}) y el orden se reescribe.
     */
    @Test
    public void testNullableFalseWithoutOptionalIsRewritten() {
        assertEquals(fk("jbs_ob1", "Obligatoria", "nombre", "obligatoriaPorColumna") + " desc nulls first, o.idraiz desc",
                AssociationOrderBy.rewrite(Raiz.class, "o", "obligatoriaPorColumna.nombre desc"));
        assertTrue(AssociationOrderBy.needsRewrite(Raiz.class, "o", "obligatoriaPorColumna.nombre"));
        assertEquals(fk("jbs_ob1", "Obligatoria", "nombre", "unoAUnoPorColumna") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "unoAUnoPorColumna.nombre"));
    }

    /**
     * Con {@code optional = false} explícito sigue sin reescribirse,
     * aunque la columna también diga {@code nullable = false}.
     */
    @Test
    public void testOptionalFalseIsUntouchedWithNullableFalse() {
        assertEquals("obligatoriaExplicita.nombre, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "obligatoriaExplicita.nombre"));
        assertFalse(AssociationOrderBy.needsRewrite(Raiz.class, "o", "obligatoriaExplicita.nombre"));
    }

    /**
     * La opcionalidad no toca la regla de la clave foránea: {@code asociacion.<id>} se
     * lee de la columna, sin {@code join}, sea o no obligatoria.
     */
    @Test
    public void testForeignKeyIdOfNullableFalseIsUntouched() {
        String order = "obligatoriaPorColumna.idobligatoria desc";
        assertEquals(order + ", o.idraiz desc", AssociationOrderBy.rewrite(Raiz.class, "o", order));
        assertFalse(AssociationOrderBy.needsRewrite(Raiz.class, "o", order));
    }

    @Test
    public void testForeignKeyIdIsUntouched() {
        String order = "opcional.idopcional desc";
        assertEquals(order + ", o.idraiz desc", AssociationOrderBy.rewrite(Raiz.class, "o", order));
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
        assertEquals(fk("jbs_ob1", "Opcional", "noexiste", "opcional") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "opcional.noexiste"));
    }

    @Test
    public void testExpressionsAreUntouched() {
        String[] orders = {"upper(opcional.nombre)", "1", "opcional", "o.opcional",
            "coalesce(opcional.nombre, nombre) desc", "opcional.nombre collate x"};
        for (String order : orders) {
            String key = order.endsWith(" desc") ? ", o.idraiz desc" : ", o.idraiz";
            assertEquals(order + key, AssociationOrderBy.rewrite(Raiz.class, "o", order), order);
        }
    }

    @Test
    public void testSeveralTerms() {
        assertEquals("fecha desc, " + fk("jbs_ob1", "Opcional", "nombre", "opcional") + " asc nulls last, "
                + "obligatoria.nombre, " + fk("jbs_ob2", "Opcional", "padre.nombre", "opcional")
                + " desc nulls first, idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o",
                        "fecha desc, opcional.nombre,obligatoria.nombre , opcional.padre.nombre desc, idraiz"));
        // Una función con comas no se parte.
        assertEquals("coalesce(nombre, 'a,b'), " + fk("jbs_ob1", "Opcional", "nombre", "opcional")
                + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "coalesce(nombre, 'a,b'), opcional.nombre"));
    }

    @Test
    public void testInheritedAssociation() {
        assertEquals(fk("jbs_ob1", "Opcional", "nombre", "opcional") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(RaizHija.class, "o", "opcional.nombre"));
    }

    @Test
    public void testExplicitEntityNameAndAlias() {
        assertEquals("(select jbs_ob1.nombre from Opcional jbs_ob1 where jbs_ob1 = r.opcional) desc nulls first, r.idraiz desc",
                AssociationOrderBy.rewrite(Raiz.class, "MiEntidad", "r", "r.opcional.nombre desc"));
        // El nombre de la entidad raíz sigue sirviendo en la forma de respaldo.
        assertEquals("(select jbs_ob1.detalle.nombre from MiEntidad jbs_ob1 where jbs_ob1 = r) asc nulls last, r.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "MiEntidad", "r", "r.detalle.nombre"));
    }

    @Test
    public void testFrameworkTestEntity() {
        // Entidad de prueba del módulo: pais y region son @ManyToOne sin optional.
        assertEquals("(select jbs_ob1.nombre from Pais jbs_ob1 where jbs_ob1 = o.pais) asc nulls last, o.idciudad",
                AssociationOrderBy.rewrite(Ciudad.class, "o", "pais.nombre"));
        assertEquals("codigo, nombreCorto, o.idciudad",
                AssociationOrderBy.rewrite(Ciudad.class, "o", "codigo, nombreCorto"));
    }

    /**
     * La subconsulta se correlaciona por la clave foránea
     * del primer salto ({@code where x = o.asociacion}), sin volver a leer la
     * raíz; los saltos siguientes van dentro. Conserva la ubicación de los
     * nulos y el desempate.
     */
    @Test
    public void testCorrelatedByForeignKey() {
        assertEquals("(select jbs_ob1.nombre from Opcional jbs_ob1 where jbs_ob1 = o.opcional) asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "opcional.nombre"));
        assertEquals("(select jbs_ob1.padre.obligatoria.nombre from Opcional jbs_ob1 where jbs_ob1 = o.opcional)"
                + " desc nulls first, o.idraiz desc",
                AssociationOrderBy.rewrite(Raiz.class, "o", "o.opcional.padre.obligatoria.nombre desc"));
        // Destino con @Entity(name): se usa ese nombre.
        assertEquals(fk("jbs_ob1", "EntidadNombrada", "nombre", "nombrada") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "nombrada.nombre"));
        // Una columna de unión simple aunque comparta la columna de la clave de la raíz.
        assertEquals(fk("jbs_ob1", "Opcional", "nombre", "mismaColumna") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "mismaColumna.nombre"));
        // Cada término lleva su alias.
        assertEquals(fk("jbs_ob1", "Opcional", "nombre", "opcional") + " asc nulls last, "
                + fk("jbs_ob2", "Obligatoria", "nombre", "obligatoriaPorColumna") + " desc nulls first, o.idraiz desc",
                AssociationOrderBy.rewrite(Raiz.class, "o", "opcional.nombre, obligatoriaPorColumna.nombre desc"));
    }

    /**
     * Sin una columna simple en la raíz que apunte a la
     * clave simple del destino, se queda la forma sobre la raíz.
     */
    @Test
    public void testFallsBackToRootCorrelation() {
        // Lado inverso (mappedBy): la clave foránea está en el destino.
        assertEquals(sub("jbs_ob1", "detalle.nombre") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "detalle.nombre"));
        // Unida por otra columna del destino (referencedColumnName).
        assertEquals(sub("jbs_ob1", "porNombre.nombre") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "porNombre.nombre"));
        // Columnas de unión compuestas, tabla de unión y clave primaria compartida.
        assertEquals(sub("jbs_ob1", "compuesta.opcional.nombre") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "compuesta.opcional.nombre"));
        assertEquals(sub("jbs_ob1", "porTabla.nombre") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "porTabla.nombre"));
        assertEquals(sub("jbs_ob1", "porClavePrimaria.nombre") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "porClavePrimaria.nombre"));
        // targetEntity explícito: no se adivina el destino.
        assertEquals(sub("jbs_ob1", "porTipo.nombre") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "porTipo.nombre"));
        // Destino con @EmbeddedId: no tiene una clave simple.
        assertEquals(sub("jbs_ob1", "incrustada.nombre") + " desc nulls first, o.idraiz desc",
                AssociationOrderBy.rewrite(Raiz.class, "o", "incrustada.nombre desc"));
    }

    /**
     * Una prueba por cada guarda de la vuelta a la raíz, con la asociación
     * de control al lado: si una guarda se quitara, ese término pasaría a la
     * forma por la clave foránea y la prueba fallaría.
     */
    @Test
    public void testEachGuardFallsBackToRoot() {
        // Control: la misma forma de camino se correlaciona por la clave foránea.
        assertEquals(fk("jbs_ob1", "Opcional", "nombre", "opcional") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(RaizGuardas.class, "o", "opcional.nombre"));
        // Destino con la clave en el método: no se encuentra por los atributos.
        assertEquals(sub("RaizGuardas", "jbs_ob1", "porMetodo.nombre") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(RaizGuardas.class, "o", "porMetodo.nombre"));
        // Destino con identidad derivada: dos @Id con una asociación, o la asociación sola como clave.
        assertEquals(sub("RaizGuardas", "jbs_ob1", "derivada.nombre") + " desc nulls first, o.idraiz desc",
                AssociationOrderBy.rewrite(RaizGuardas.class, "o", "derivada.nombre desc"));
        assertEquals(sub("RaizGuardas", "jbs_ob1", "derivadaUnaAsociacion.nombre") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(RaizGuardas.class, "o", "derivadaUnaAsociacion.nombre"));
        // Destino sin clave.
        assertEquals(sub("RaizGuardas", "jbs_ob1", "sinClave.opcional.nombre") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(RaizGuardas.class, "o", "sinClave.opcional.nombre"));
        // targetEntity (en @OneToOne y en @ManyToOne) y @PrimaryKeyJoinColumns.
        assertEquals(sub("RaizGuardas", "jbs_ob1", "muchosAUnoPorTipo.nombre") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(RaizGuardas.class, "o", "muchosAUnoPorTipo.nombre"));
        assertEquals(sub("RaizGuardas", "jbs_ob1", "unoAUnoPorTipo.nombre") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(RaizGuardas.class, "o", "unoAUnoPorTipo.nombre"));
        assertEquals(sub("RaizGuardas", "jbs_ob1", "porClavesPrimarias.nombre") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(RaizGuardas.class, "o", "porClavesPrimarias.nombre"));
    }

    /**
     * {@code @IdClass} y {@code @JoinColumns} por separado: cada regla basta
     * sola para volver a la raíz (la prueba de arriba de la clase las junta).
     */
    @Test
    public void testIdClassAndJoinColumnsAreIndependent() {
        // Una sola columna de unión, pero el destino tiene dos @Id.
        assertEquals(sub("RaizGuardas", "jbs_ob1", "conIdClass.nombre") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(RaizGuardas.class, "o", "conIdClass.nombre"));
        // Destino de clave simple, pero unido por dos columnas.
        assertEquals(sub("RaizGuardas", "jbs_ob1", "dosColumnas.nombre") + " desc nulls first, o.idraiz desc",
                AssociationOrderBy.rewrite(RaizGuardas.class, "o", "dosColumnas.nombre desc"));
        // Los dos términos en el mismo orden: cada uno con su alias y su forma.
        assertEquals(sub("RaizGuardas", "jbs_ob1", "conIdClass.nombre") + " asc nulls last, "
                + fk("jbs_ob2", "Opcional", "nombre", "opcional") + " asc nulls last, o.idraiz",
                AssociationOrderBy.rewrite(RaizGuardas.class, "o", "conIdClass.nombre, opcional.nombre"));
    }

    /**
     * Primer salto que es la clave de la raíz ({@code @Id @OneToOne} con su
     * columna de unión): se correlaciona por esa columna y el desempate es la
     * clave derivada, una sola vez.
     */
    @Test
    public void testFirstHopIsTheDerivedKey() {
        assertEquals(fk("jbs_ob1", "Purchase", "number", "purchase") + " desc nulls first, o.purchase.idpurchase desc",
                AssociationOrderBy.rewrite(Delivery.class, "o", "purchase.number desc"));
        assertEquals(fk("jbs_ob1", "Purchase", "number", "purchase") + " asc nulls last, o.purchase.idpurchase",
                AssociationOrderBy.rewrite(Delivery.class, "o", "o.purchase.number"));
        // Sin la columna de unión declarada: la unión por omisión apunta a la clave del destino, así que
        // se correlaciona igual; pero sin columna conocida no hay desempate por la clave derivada.
        assertEquals(fk("jbs_ob1", "Opcional", "nombre", "origen") + " asc nulls last",
                AssociationOrderBy.rewrite(ClaveUnoAUnoSinColumna.class, "o", "origen.nombre"));
    }

    /**
     * El desempate toma la dirección del último término
     * explícito, para que {@code fecha desc} pueda usar su índice hacia atrás.
     */
    @Test
    public void testTieBreakFollowsLastTermDirection() {
        assertEquals("fecha desc, o.idraiz desc", AssociationOrderBy.rewrite(Raiz.class, "o", "fecha desc"));
        assertEquals("fecha DESC, o.idraiz desc", AssociationOrderBy.rewrite(Raiz.class, "o", "fecha DESC"));
        assertEquals("fecha desc, o.idraiz desc", AssociationOrderBy.addKeyTieBreak(Raiz.class, "o", "fecha desc"));
        // Manda el último término, no el primero.
        assertEquals("fecha desc, secuencia, o.idraiz",
                AssociationOrderBy.rewrite(Raiz.class, "o", "fecha desc, secuencia"));
        assertEquals("fecha desc, secuencia, nro desc, o.idraiz desc",
                AssociationOrderBy.rewrite(Raiz.class, "o", "fecha desc, secuencia, nro desc"));
        assertEquals("fecha asc, o.idraiz", AssociationOrderBy.rewrite(Raiz.class, "o", "fecha asc"));
        // Con nulls explícito y en una expresión.
        assertEquals("fecha desc nulls last, o.idraiz desc",
                AssociationOrderBy.rewrite(Raiz.class, "o", "fecha desc nulls last"));
        assertEquals("upper(nombre) desc, o.idraiz desc",
                AssociationOrderBy.rewrite(Raiz.class, "o", "upper(nombre) desc"));
        // «desc» dentro de un nombre no cuenta.
        assertEquals("descripcion, o.idraiz", AssociationOrderBy.rewrite(Raiz.class, "o", "descripcion"));
        // Clave compuesta: todos los componentes en esa dirección.
        assertEquals("nombre desc, o.parte1 desc, o.parte2 desc",
                AssociationOrderBy.rewrite(ClaveCompuesta.class, "o", "nombre desc"));
        assertEquals("nombre desc, o.id.parte1 desc, o.id.parte2 desc",
                AssociationOrderBy.rewrite(ClaveIncrustada.class, "o", "nombre desc"));
        // Lo que ya figura no se agrega, ni hay desempate sin clave segura.
        assertEquals("idraiz desc", AssociationOrderBy.rewrite(Raiz.class, "o", "idraiz desc"));
        assertEquals("nombre desc", AssociationOrderBy.rewrite(ClaveDerivada.class, "o", "nombre desc"));
        assertEquals("idraizCopia desc", AssociationOrderBy.rewrite(Raiz.class, "o", "idraizCopia desc"));
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
