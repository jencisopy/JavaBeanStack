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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.javabeanstack.data.model.DataSet;
import org.javabeanstack.model.appcatalog.AppTablesRelation;
import org.javabeanstack.model.appcatalog.AppUserFormView;
import org.javabeanstack.model.appcatalog.AppUserFormViewColumn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Reproduccion aislada del defecto de borrado de filas hijas por
 * {@code IDataSet} (plan DELHIJA, Fase 1; hallazgos E0-01 y E0-02).
 *
 * <p>La rama {@code case IDataRow.DELETE} de
 * {@link AbstractDAO#update(java.lang.String, org.javabeanstack.data.IDataSet)}
 * hace {@code em.remove(em.merge(ejb))}. El {@code merge} de una fila
 * desprendida carga el grafo con el perfil interno de fetch "merge", que
 * inicializa toda coleccion con cascada {@code MERGE} que encuentre: eso trae
 * al contexto al padre con su coleccion completa y, en el flush, la cascada
 * {@code PERSIST} desde ese padre administrado vuelve a dejar administrada a
 * la hija recien borrada (JPA 3.2, 3.2.4). Resultado: no se emite el
 * {@code DELETE} y el {@code IDataResult} devuelve exito.</p>
 *
 * <p><b>Que prueba cada caso.</b> Los cuatro patrones del plan se diferencian
 * <b>solo</b> por lo que contiene la coleccion
 * {@code appUserFormViewColumnList} del padre cuando el padre viaja en el
 * {@code IDataSet}; por eso cada metodo fija ese contenido de forma explicita
 * y lo dice en su nombre:</p>
 *
 * <table>
 * <caption>Casos</caption>
 * <tr><th>Caso</th><th>Que viaja</th><th>Resultado esperado</th></tr>
 * <tr><td>test01 (P1)</td><td>hija DELETE sola, el padre NO viaja</td><td>borra</td></tr>
 * <tr><td>test02 (P1 con padre administrado)</td><td>padre UPDATE con coleccion VACIA + hija DELETE</td><td>borra</td></tr>
 * <tr><td>test03 (P2)</td><td>hijas DELETE + padre DELETE desprendido</td><td>borra todo</td></tr>
 * <tr><td>test04 (P3)</td><td>padre UPDATE con la hija DENTRO de su coleccion + hija DELETE</td><td>error informado (D2)</td></tr>
 * <tr><td>test05 (P4)</td><td>padre UPDATE SIN la hija en su coleccion + hija DELETE</td><td>borra</td></tr>
 * <tr><td>test06</td><td>hija inexistente DELETE</td><td>error informado (RF4)</td></tr>
 * <tr><td>test07</td><td>borrado individual por {@code IGenericDAO.remove}</td><td>borra</td></tr>
 * <tr><td>test08</td><td>fila de clave compuesta sin padre (control de no regresion, R3)</td><td>borra</td></tr>
 * </table>
 *
 * <p>Las aserciones expresan el comportamiento <b>corregido</b> (criterio de
 * salida de la Fase 2). Mientras el defecto siga en el framework, los casos
 * marcados "ROJO ESPERADO HOY" fallan: ese rojo es la reproduccion.</p>
 *
 * <p><b>Evidencia.</b> Cada caso verifica el resultado contra la base con un
 * {@code select count(*)} nativo (no alcanza con {@code isSuccessFul()}) y
 * cuenta las sentencias que Hibernate escribio en {@code hibernate.log} del
 * servidor durante la operacion. El tramo de log de cada caso queda en
 * {@code business/target/delhija-logs/}. El directorio de log se toma de la
 * variable de entorno {@code WILDFLY_LOG_DIR} (por omision, el del WildFly 41
 * del asistente). El conteo de sentencias hace falta porque la clave ajena
 * {@code fk_appuserformviewcolumn_appuserformview} de la base es
 * {@code ON DELETE CASCADE}: al borrar el padre las hijas desaparecen por la
 * base, haya emitido Hibernate el {@code DELETE} de la hija o no. El
 * {@code hibernate.log} es compartido con las demas aplicaciones del servidor:
 * conviene correr este test sin otro trafico sobre el 8180.</p>
 *
 * <p><b>Datos.</b> Las filas de prueba se crean y se borran en el esquema
 * {@code catalogo} con {@code iduser = -9999}: ninguna fila real del catalogo
 * se toca. La limpieza va por sentencia nativa, para no depender del mecanismo
 * que se esta probando.</p>
 *
 * <p><b>No cubierto.</b> La identidad derivada ({@code @Id} sobre asociacion,
 * supuesto A3 del plan) no se prueba aca: ninguna entidad del
 * {@code TestProjects-ear} la usa y crear la tabla seria DDL fuera del alcance
 * de la Fase 1. El caso de clave compuesta (test08) cubre la otra mitad de R3.</p>
 *
 * @author test-developer (plan DELHIJA, 2026-10-09)
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class AbstractDAODeleteChildTest extends TestClass {

    /** Identificador de usuario de las filas de prueba; negativo a proposito. */
    private static final Long IDUSER_PRUEBA = -9999L;
    /** Nombre de vista de las filas de prueba. */
    private static final String VIEWNAME = "DELHIJA";
    /** Marca de las filas de prueba de {@code dic_tablarelacion}. */
    private static final String MARCA_RELACION = "delhija_pk";
    /** Directorio de log por omision (WildFly 41 del asistente, puerto 8180). */
    private static final String DIR_LOG_DEFECTO
            = "/home/jenciso/oym/proyectos/java/servers/wildfly-41.0.1.Final_para_ia/standalone/log";
    private static final String TABLA_PADRE = "appuserformview";
    private static final String TABLA_HIJA = "appuserformviewcolumn";

    private static IGenericDAO dao;
    private static Path dirLog;

    public AbstractDAODeleteChildTest() {
    }

    @BeforeAll
    public static void setUpClass2() throws Exception {
        //A diferencia del resto de la suite, este test NO sigue adelante en
        //silencio si no hubo conexion con el servidor de aplicaciones: un verde
        //sin servidor no prueba nada (convencion del plan DELHIJA).
        if (error != null) {
            fail("No hubo conexion con el servidor de aplicaciones: " + error
                    + " (revisar SERVER_TEST / SERVER_TEST_PORT; el arnes espera 8180)");
        }
        dao = (IGenericDAORemote) context.lookup(jndiProject + "GenericDAO!org.javabeanstack.data.IGenericDAORemote");
        assertNotNull(dao, "No se pudo obtener el GenericDAO remoto");
        String dir = System.getenv("WILDFLY_LOG_DIR");
        dirLog = Paths.get((dir == null || dir.isEmpty()) ? DIR_LOG_DEFECTO : dir);
        verificarFidelidadDelArnes();
    }

    /**
     * Comprueba que la unidad de persistencia del catalogo del servidor de
     * prueba este configurada como la de maker.
     *
     * <p>Sin {@code hibernate.max_fetch_depth=0} el arnes <b>no reproduce el
     * defecto</b>: la asociacion de la hija al padre se resuelve con join
     * fetch en el mismo plan de select, el perfil interno "merge" no llega a
     * cargar la coleccion del padre, el flush no rescata nada y el
     * {@code DELETE} se ejecuta. El resultado seria un verde que no prueba que
     * el defecto este corregido. Comprobado el 2026-10-09 corriendo los mismos
     * casos con y sin la propiedad (plan DELHIJA, Fase 1).</p>
     */
    private static void verificarFidelidadDelArnes() throws Exception {
        Map<String, Object> propiedades = dao.getPersistUnitProp("PU1");
        Object maxFetchDepth = (propiedades == null) ? null : propiedades.get("hibernate.max_fetch_depth");
        assertEquals("0", String.valueOf(maxFetchDepth),
                "La unidad de persistencia PU1 del servidor de prueba no tiene"
                + " hibernate.max_fetch_depth=0 como la de maker"
                + " (Maker-model/src/main/resources/META-INF/persistence.xml): sin esa"
                + " propiedad este test da verde sin probar nada."
                + " Agregarla en TestProject/TestProjects-ejb/src/main/resources/META-INF/persistence.xml,"
                + " reconstruir el EAR y volver a desplegarlo");
    }

    @AfterEach
    public void limpiarFilasDePrueba() throws Exception {
        if (dao == null) {
            return;
        }
        //Limpieza por sentencia: no se usa el camino de entidades, que es
        //justamente el que se esta probando.
        dao.sqlExec(null, "delete from {schemacatalog}." + TABLA_HIJA
                + " where idappuserformview in (select idappuserformview from {schemacatalog}."
                + TABLA_PADRE + " where iduser = " + IDUSER_PRUEBA + ")", null);
        dao.sqlExec(null, "delete from {schemacatalog}." + TABLA_PADRE
                + " where iduser = " + IDUSER_PRUEBA, null);
        dao.sqlExec(null, "delete from {schemacatalog}.dic_tablarelacion"
                + " where principal = '" + MARCA_RELACION + "'", null);
    }

    /**
     * P1: la hija viaja sola, marcada DELETE, y el padre no viaja en el
     * {@code IDataSet}. Es el caso de GRIDVW (E4-01): hoy no borra y no avisa.
     */
    @Test
    public void test01P1HijaSueltaSinPadreEnElDataSet() throws Exception {
        String caso = "test01-P1-hija-sola";
        Long idView = crearVistaConHijas("delhija_p1.xhtml", 3);
        List<AppUserFormViewColumn> hijas = leerHijasDesprendidas(idView);
        assertEquals(3, hijas.size(), "FIXTURE: se esperaban 3 hijas leidas por consulta");

        AppUserFormViewColumn aBorrar = hijas.get(0);
        aBorrar.setAction(IDataRow.DELETE);
        IDataSet dataSet = new DataSet();
        dataSet.add("AppUserFormViewColumn", unaLista(aBorrar));

        long marca = marcarLog();
        IDataResult resultado = dao.update(null, dataSet);
        String tramo = guardarTramoLog(caso, marca);

        long hijasAhora = contarHijas(idView);
        informar(caso, "hija DELETE sola; el padre NO viaja", resultado, 3, hijasAhora, tramo);

        assertTrue(resultado != null && Boolean.TRUE.equals(resultado.isSuccessFul()),
                "El DAO devolvio error al borrar la hija suelta: "
                + ((resultado == null) ? "null" : resultado.getErrorMsg()));
        assertTrue(contarDeletes(tramo, TABLA_HIJA) >= 1,
                "ROJO ESPERADO HOY (E0-01): no se emitio ningun DELETE de " + TABLA_HIJA
                + "; el flush rescato la hija por la cascada PERSIST del padre");
        assertEquals(2, hijasAhora,
                "ROJO ESPERADO HOY (E0-01): la hija marcada DELETE sigue en la base y el DAO devolvio exito");
    }

    /**
     * P1 con el padre ya administrado en el mismo {@code EntityManager}: el
     * padre viaja primero como UPDATE y con la coleccion de hijas
     * <b>vacia</b>, de modo que la copia que hace el {@code merge} deja la
     * coleccion administrada sin la hija a borrar.
     */
    @Test
    public void test02P1HijaSueltaConPadreUpdateYColeccionVacia() throws Exception {
        String caso = "test02-P1-padre-update-coleccion-vacia";
        Long idView = crearVistaConHijas("delhija_p1b.xhtml", 3);
        AppUserFormView padre = leerVistaDesprendida("delhija_p1b.xhtml");
        List<AppUserFormViewColumn> hijas = leerHijasDesprendidas(idView);
        assertEquals(3, hijas.size(), "FIXTURE: se esperaban 3 hijas leidas por consulta");

        padre.setAppUserFormViewColumnList(new ArrayList<AppUserFormViewColumn>());
        padre.setAction(IDataRow.UPDATE);
        AppUserFormViewColumn aBorrar = hijas.get(0);
        aBorrar.setAppUserFormView(padre);
        aBorrar.setAction(IDataRow.DELETE);

        IDataSet dataSet = new DataSet();
        dataSet.add("AppUserFormView", unaLista(padre));
        dataSet.add("AppUserFormViewColumn", unaLista(aBorrar));

        long marca = marcarLog();
        IDataResult resultado = dao.update(null, dataSet);
        String tramo = guardarTramoLog(caso, marca);

        long hijasAhora = contarHijas(idView);
        informar(caso, "padre UPDATE con coleccion VACIA + hija DELETE", resultado, 3, hijasAhora, tramo);

        assertTrue(resultado != null && Boolean.TRUE.equals(resultado.isSuccessFul()),
                "El DAO devolvio error: " + ((resultado == null) ? "null" : resultado.getErrorMsg()));
        assertTrue(contarDeletes(tramo, TABLA_HIJA) >= 1,
                "No se emitio el DELETE de la hija aunque la coleccion del padre viajaba vacia");
        assertEquals(2, hijasAhora, "La hija marcada DELETE sigue en la base");
    }

    /**
     * P2: todas las hijas marcadas DELETE y, despues, el padre desprendido
     * tambien marcado DELETE, con las hijas apuntando a esa instancia de padre
     * (es lo que hacia GRIDVW cuando aparecio E4-05, "references an unsaved
     * transient instance").
     *
     * <p>El conteo de hijas no alcanza como prueba: la clave ajena de la base
     * es {@code ON DELETE CASCADE}. Por eso se exige tambien el {@code DELETE}
     * de la tabla hija en el log.</p>
     */
    @Test
    public void test03P2HijasDeleteYPadreDeleteDesprendido() throws Exception {
        String caso = "test03-P2-hijas-y-padre-delete";
        Long idView = crearVistaConHijas("delhija_p2.xhtml", 2);
        AppUserFormView padre = leerVistaDesprendida("delhija_p2.xhtml");
        List<AppUserFormViewColumn> hijas = leerHijasDesprendidas(idView);
        assertEquals(2, hijas.size(), "FIXTURE: se esperaban 2 hijas leidas por consulta");

        padre.setAppUserFormViewColumnList(new ArrayList<AppUserFormViewColumn>());
        padre.setAction(IDataRow.DELETE);
        for (AppUserFormViewColumn hija : hijas) {
            hija.setAppUserFormView(padre);
            hija.setAction(IDataRow.DELETE);
        }

        IDataSet dataSet = new DataSet();
        dataSet.add("AppUserFormViewColumn", hijas);
        dataSet.add("AppUserFormView", unaLista(padre));

        long marca = marcarLog();
        IDataResult resultado = dao.update(null, dataSet);
        String tramo = guardarTramoLog(caso, marca);

        long hijasAhora = contarHijas(idView);
        long padreAhora = contarVista(idView);
        informar(caso, "hijas DELETE + padre DELETE desprendido", resultado, 2, hijasAhora, tramo);
        System.out.println("   filas de la vista despues: " + padreAhora);

        assertTrue(resultado != null && Boolean.TRUE.equals(resultado.isSuccessFul()),
                "ROJO ESPERADO HOY (E0-02): el DAO fallo al borrar hijas y padre en el mismo IDataSet: "
                + ((resultado == null) ? "null" : resultado.getErrorMsg()));
        assertTrue(contarDeletes(tramo, TABLA_HIJA) >= 2,
                "ROJO ESPERADO HOY (E0-02): Hibernate no emitio el DELETE de las dos hijas"
                + " (el conteo en la base no sirve de prueba: la FK es ON DELETE CASCADE)");
        assertTrue(contarDeletes(tramo, TABLA_PADRE) >= 1, "No se emitio el DELETE de la vista");
        assertEquals(0, hijasAhora, "Quedaron hijas en la base");
        assertEquals(0, padreAhora, "Quedo la fila de la vista en la base");
    }

    /**
     * P3: el padre viaja como UPDATE y la hija a borrar sigue <b>dentro</b> de
     * su coleccion. Por la semantica de JPA el borrado no es posible (el flush
     * la rescata por la cascada PERSIST); lo que se exige es que el DAO lo
     * <b>informe</b> en vez de devolver exito (decision D2, respuesta a Q1:
     * error en el {@code IDataResult}).
     */
    @Test
    public void test04P3HijaDentroDeLaColeccionDelPadre() throws Exception {
        String caso = "test04-P3-hija-dentro-de-la-coleccion";
        Long idView = crearVistaConHijas("delhija_p3.xhtml", 3);
        AppUserFormView padre = leerVistaDesprendida("delhija_p3.xhtml");
        List<AppUserFormViewColumn> hijas = leerHijasDesprendidas(idView);
        assertEquals(3, hijas.size(), "FIXTURE: se esperaban 3 hijas leidas por consulta");

        AppUserFormViewColumn aBorrar = hijas.get(0);
        for (AppUserFormViewColumn hija : hijas) {
            hija.setAppUserFormView(padre);
        }
        //La coleccion del padre lleva LAS TRES hijas, incluida la que se borra.
        padre.setAppUserFormViewColumnList(new ArrayList<>(hijas));
        padre.setAction(IDataRow.UPDATE);
        assertTrue(padre.getAppUserFormViewColumnList().contains(aBorrar),
                "FIXTURE: la hija a borrar tiene que estar dentro de la coleccion del padre");
        aBorrar.setAction(IDataRow.DELETE);

        IDataSet dataSet = new DataSet();
        dataSet.add("AppUserFormView", unaLista(padre));
        dataSet.add("AppUserFormViewColumn", unaLista(aBorrar));

        long marca = marcarLog();
        IDataResult resultado = dao.update(null, dataSet);
        String tramo = guardarTramoLog(caso, marca);

        long hijasAhora = contarHijas(idView);
        informar(caso, "padre UPDATE con la hija DENTRO de su coleccion + hija DELETE",
                resultado, 3, hijasAhora, tramo);

        assertFalse(resultado == null || Boolean.TRUE.equals(resultado.isSuccessFul()),
                "ROJO ESPERADO HOY (E0-01 / D2): el DAO devolvio exito aunque la hija"
                + " sigue en la base por estar en la coleccion del padre: no avisa nada");
        assertNotNull(resultado.getErrorMsg(), "El error no trae mensaje");
        assertFalse(resultado.getErrorMsg().isEmpty(), "El error no trae mensaje");
        //El contrato de D2 es "informa y NO borra": la hija tiene que seguir ahi.
        assertEquals(3, hijasAhora,
                "P3 informo el error pero igual se perdio alguna hija: la poscondicion de D2"
                + " es avisar, no borrar a medias");
    }

    /**
     * P4: el padre viaja como UPDATE con su coleccion <b>sin</b> la hija a
     * borrar, y la hija va despues en su propia lista. Es el patron que hoy
     * funciona ({@code ItemmovimientoSrv}, {@code CtbmovimientoSrv}): sirve de
     * control positivo del arnes y de no regresion de la correccion.
     */
    @Test
    public void test05P4HijaFueraDeLaColeccionDelPadre() throws Exception {
        String caso = "test05-P4-hija-fuera-de-la-coleccion";
        Long idView = crearVistaConHijas("delhija_p4.xhtml", 3);
        AppUserFormView padre = leerVistaDesprendida("delhija_p4.xhtml");
        List<AppUserFormViewColumn> hijas = leerHijasDesprendidas(idView);
        assertEquals(3, hijas.size(), "FIXTURE: se esperaban 3 hijas leidas por consulta");

        AppUserFormViewColumn aBorrar = hijas.get(0);
        List<AppUserFormViewColumn> quedan = new ArrayList<>(hijas);
        quedan.remove(aBorrar);
        for (AppUserFormViewColumn hija : hijas) {
            hija.setAppUserFormView(padre);
        }
        padre.setAppUserFormViewColumnList(quedan);
        padre.setAction(IDataRow.UPDATE);
        assertFalse(padre.getAppUserFormViewColumnList().contains(aBorrar),
                "FIXTURE: la hija a borrar NO tiene que estar en la coleccion del padre");
        aBorrar.setAction(IDataRow.DELETE);

        IDataSet dataSet = new DataSet();
        dataSet.add("AppUserFormView", unaLista(padre));
        dataSet.add("AppUserFormViewColumn", unaLista(aBorrar));

        long marca = marcarLog();
        IDataResult resultado = dao.update(null, dataSet);
        String tramo = guardarTramoLog(caso, marca);

        long hijasAhora = contarHijas(idView);
        informar(caso, "padre UPDATE SIN la hija en su coleccion + hija DELETE",
                resultado, 3, hijasAhora, tramo);

        assertTrue(resultado != null && Boolean.TRUE.equals(resultado.isSuccessFul()),
                "El DAO devolvio error en el patron que hoy funciona: "
                + ((resultado == null) ? "null" : resultado.getErrorMsg()));
        assertTrue(contarDeletes(tramo, TABLA_HIJA) >= 1, "No se emitio el DELETE de la hija");
        assertEquals(2, hijasAhora, "La hija marcada DELETE sigue en la base");
    }

    /**
     * Fila a borrar que no existe en la base (RF4): el DAO tiene que informar
     * error y no insertar nada.
     */
    @Test
    public void test06FilaInexistente() throws Exception {
        String caso = "test06-fila-inexistente";
        Long idView = crearVistaConHijas("delhija_inex.xhtml", 1);
        List<AppUserFormViewColumn> hijas = leerHijasDesprendidas(idView);
        assertEquals(1, hijas.size(), "FIXTURE: se esperaba 1 hija leida por consulta");

        AppUserFormViewColumn fantasma = new AppUserFormViewColumn();
        fantasma.setIdappuserformviewcolumn(-123456789L);
        fantasma.setAppUserFormView(hijas.get(0).getAppUserFormView());
        fantasma.setIdorder(99);
        fantasma.setColumnName("fantasma");
        fantasma.setVisible(true);
        fantasma.setAction(IDataRow.DELETE);

        IDataSet dataSet = new DataSet();
        dataSet.add("AppUserFormViewColumn", unaLista(fantasma));

        long marca = marcarLog();
        IDataResult resultado = dao.update(null, dataSet);
        String tramo = guardarTramoLog(caso, marca);

        long hijasAhora = contarHijas(idView);
        informar(caso, "hija inexistente marcada DELETE", resultado, 1, hijasAhora, tramo);

        assertFalse(resultado == null || Boolean.TRUE.equals(resultado.isSuccessFul()),
                "El DAO devolvio exito al borrar una fila que no existe");
        //RF4: el error tiene que decir QUE fila no se pudo borrar. Hoy lo dice el
        //StaleObjectStateException del merge; con la correccion lo tiene que decir el
        //EntityNotFoundException de removeEjb. La asercion es la misma en los dos casos.
        String mensaje = resultado.getErrorMsg();
        assertNotNull(mensaje, "El error de la fila inexistente no trae mensaje");
        assertTrue(mensaje.contains("AppUserFormViewColumn") || mensaje.contains("-123456789"),
                "El error no dice cual es la fila que no se pudo borrar: " + mensaje);
        assertEquals(1, hijasAhora,
                "El borrado de una fila inexistente altero la cantidad de hijas");
    }

    /**
     * Borrado individual por {@code IGenericDAO.remove}, que delega en la misma
     * rama DELETE de {@code update}: la hija viaja sola, sin el padre.
     */
    @Test
    public void test07RemoveIndividual() throws Exception {
        String caso = "test07-remove-individual";
        Long idView = crearVistaConHijas("delhija_rem.xhtml", 2);
        List<AppUserFormViewColumn> hijas = leerHijasDesprendidas(idView);
        assertEquals(2, hijas.size(), "FIXTURE: se esperaban 2 hijas leidas por consulta");

        long marca = marcarLog();
        IDataResult resultado = dao.remove(null, hijas.get(0));
        String tramo = guardarTramoLog(caso, marca);

        long hijasAhora = contarHijas(idView);
        informar(caso, "IGenericDAO.remove de una hija suelta", resultado, 2, hijasAhora, tramo);

        assertTrue(resultado != null && Boolean.TRUE.equals(resultado.isSuccessFul()),
                "remove devolvio error: " + ((resultado == null) ? "null" : resultado.getErrorMsg()));
        assertTrue(contarDeletes(tramo, TABLA_HIJA) >= 1,
                "ROJO ESPERADO HOY (E0-01): remove no emitio ningun DELETE");
        assertEquals(1, hijasAhora,
                "ROJO ESPERADO HOY (E0-01): remove devolvio exito y la hija sigue en la base");
    }

    /**
     * Control de no regresion para la clave compuesta (riesgo R3 / supuesto A3
     * del plan): {@code AppTablesRelation} tiene cuatro campos {@code @Id} y no
     * tiene padre con cascada. Hoy se borra bien; con la correccion (que
     * resuelve la clave con {@code PersistenceUnitUtil.getIdentifier} y busca
     * la fila con {@code em.find}) tiene que seguir borrandose igual.
     */
    @Test
    public void test08ClaveCompuestaAppTablesRelation() throws Exception {
        String caso = "test08-clave-compuesta";
        AppTablesRelation relacion = new AppTablesRelation();
        relacion.setEntityPK(MARCA_RELACION);
        relacion.setEntityFK("delhija_fk");
        relacion.setFieldsPK("id");
        relacion.setFieldsFK("id");
        relacion.setIncluded(true);
        relacion.setRelationType((short) 0);
        relacion.setAction(IDataRow.INSERT);

        IDataSet alta = new DataSet();
        alta.add("AppTablesRelation", unaLista(relacion));
        IDataResult resultadoAlta = dao.update(null, alta);
        assertTrue(resultadoAlta != null && Boolean.TRUE.equals(resultadoAlta.isSuccessFul()),
                "FIXTURE: no se pudo dar de alta la fila de clave compuesta: "
                + ((resultadoAlta == null) ? "null" : resultadoAlta.getErrorMsg()));
        assertEquals(1, contarRelaciones(), "FIXTURE: la fila de clave compuesta no quedo grabada");

        List<AppTablesRelation> leidas = dao.findListByQuery(null,
                "select o from AppTablesRelation o where o.entityPK = '" + MARCA_RELACION + "'", null);
        assertEquals(1, leidas.size(), "FIXTURE: no se pudo releer la fila de clave compuesta");
        AppTablesRelation aBorrar = leidas.get(0);
        aBorrar.setAction(IDataRow.DELETE);

        IDataSet baja = new DataSet();
        baja.add("AppTablesRelation", unaLista(aBorrar));

        long marca = marcarLog();
        IDataResult resultado = dao.update(null, baja);
        String tramo = guardarTramoLog(caso, marca);

        long ahora = contarRelaciones();
        informar(caso, "fila de clave compuesta (4 @Id) marcada DELETE", resultado, 1, ahora, tramo);

        assertTrue(resultado != null && Boolean.TRUE.equals(resultado.isSuccessFul()),
                "El DAO devolvio error al borrar la fila de clave compuesta: "
                + ((resultado == null) ? "null" : resultado.getErrorMsg()));
        assertEquals(0, ahora, "La fila de clave compuesta sigue en la base");
    }

    // ------------------------------------------------------------------
    // Fixture y verificacion contra la base
    // ------------------------------------------------------------------

    /**
     * Crea una vista de prueba con {@code cantidad} columnas hijas por el
     * camino normal del framework (un {@code IDataSet} con el padre primero y
     * las hijas despues, como hace la aplicacion al guardar una vista) y
     * verifica que quedaron grabadas.
     *
     * @param form valor de la columna {@code form} que identifica el caso.
     * @param cantidad cantidad de hijas a crear.
     * @return identificador de la vista creada.
     */
    private Long crearVistaConHijas(String form, int cantidad) throws Exception {
        AppUserFormView padre = new AppUserFormView();
        padre.setIduser(IDUSER_PRUEBA);
        padre.setForm(form);
        padre.setViewName(VIEWNAME);
        padre.setAction(IDataRow.INSERT);

        List<AppUserFormViewColumn> hijas = new ArrayList<>();
        for (int i = 1; i <= cantidad; i++) {
            AppUserFormViewColumn hija = new AppUserFormViewColumn();
            hija.setAppUserFormView(padre);
            hija.setIdorder(i);
            hija.setColumnName("col" + i);
            hija.setColumnHeader("Col " + i);
            hija.setVisible(true);
            hija.setAction(IDataRow.INSERT);
            hijas.add(hija);
        }
        IDataSet dataSet = new DataSet();
        dataSet.add("AppUserFormView", unaLista(padre));
        dataSet.add("AppUserFormViewColumn", hijas);

        IDataResult resultado = dao.update(null, dataSet);
        assertTrue(resultado != null && Boolean.TRUE.equals(resultado.isSuccessFul()),
                "FIXTURE: no se pudo crear la vista de prueba " + form + " - "
                + ((resultado == null) ? "null" : resultado.getErrorMsg()));

        AppUserFormView grabada = leerVistaDesprendida(form);
        assertNotNull(grabada, "FIXTURE: la vista de prueba " + form + " no quedo grabada");
        Long idView = grabada.getIdappuserformview();
        assertEquals(cantidad, contarHijas(idView),
                "FIXTURE: la vista de prueba " + form + " no quedo con " + cantidad + " hijas");
        return idView;
    }

    /** Relee la vista por consulta: la fila vuelve desprendida. */
    private AppUserFormView leerVistaDesprendida(String form) throws Exception {
        AppUserFormView vista = dao.findByQuery(null,
                "select o from AppUserFormView o where o.form = '" + form
                + "' and o.iduser = " + IDUSER_PRUEBA, null);
        if (vista != null && vista.getAppUserFormViewColumnList() == null) {
            //La coleccion perezosa no sobrevive a la serializacion remota.
            vista.setAppUserFormViewColumnList(new ArrayList<AppUserFormViewColumn>());
        }
        return vista;
    }

    /**
     * Relee las hijas por consulta: vuelven desprendidas, cada una con su
     * padre resuelto (la asociacion {@code @ManyToOne(optional = false)} es
     * EAGER) y con la coleccion de ese padre normalizada a una lista comun.
     */
    private List<AppUserFormViewColumn> leerHijasDesprendidas(Long idView) throws Exception {
        List<AppUserFormViewColumn> hijas = dao.findListByQuery(null,
                "select o from AppUserFormViewColumn o"
                + " where o.appUserFormView.idappuserformview = " + idView
                + " order by o.idorder", null);
        for (AppUserFormViewColumn hija : hijas) {
            AppUserFormView padre = hija.getAppUserFormView();
            if (padre != null && padre.getAppUserFormViewColumnList() == null) {
                padre.setAppUserFormViewColumnList(new ArrayList<AppUserFormViewColumn>());
            }
        }
        return hijas;
    }

    /** Cantidad de hijas de la vista segun la base (consulta nativa). */
    private long contarHijas(Long idView) throws Exception {
        return contarNativo("select count(*) from {schemacatalog}." + TABLA_HIJA
                + " where idappuserformview = " + idView);
    }

    /** Cantidad de filas de la vista segun la base (consulta nativa). */
    private long contarVista(Long idView) throws Exception {
        return contarNativo("select count(*) from {schemacatalog}." + TABLA_PADRE
                + " where idappuserformview = " + idView);
    }

    /** Cantidad de filas de prueba de {@code dic_tablarelacion}. */
    private long contarRelaciones() throws Exception {
        return contarNativo("select count(*) from {schemacatalog}.dic_tablarelacion"
                + " where principal = '" + MARCA_RELACION + "'");
    }

    private long contarNativo(String sql) throws Exception {
        List<Object> resultado = dao.findByNativeQuery(null, sql, null);
        assertFalse(resultado == null || resultado.isEmpty(), "La consulta de conteo no devolvio nada");
        return Long.parseLong(resultado.get(0).toString());
    }

    private List<IDataRow> unaLista(IDataRow row) {
        List<IDataRow> lista = new ArrayList<>();
        lista.add(row);
        return lista;
    }

    // ------------------------------------------------------------------
    // Evidencia: sentencias que Hibernate escribio en hibernate.log
    // ------------------------------------------------------------------

    /** Posicion actual de {@code hibernate.log}; -1 si no se puede leer. */
    private static long marcarLog() {
        try {
            return Files.size(dirLog.resolve("hibernate.log"));
        } catch (IOException e) {
            return -1L;
        }
    }

    /**
     * Devuelve el tramo de {@code hibernate.log} escrito desde la marca y lo
     * guarda en {@code target/delhija-logs/<caso>.log} como evidencia.
     *
     * @return el tramo, o nulo si el log no se pudo leer.
     */
    private static String guardarTramoLog(String caso, long marca) {
        String tramo = leerLogDesde(marca);
        if (tramo == null) {
            System.out.println("AVISO: no se pudo leer " + dirLog.resolve("hibernate.log")
                    + "; definir WILDFLY_LOG_DIR para tener la evidencia del log");
            return null;
        }
        try {
            Path destino = Paths.get("target", "delhija-logs");
            Files.createDirectories(destino);
            Files.write(destino.resolve(caso + ".log"), tramo.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.out.println("AVISO: no se pudo guardar el tramo de log de " + caso + ": " + e.getMessage());
        }
        return tramo;
    }

    private static String leerLogDesde(long marca) {
        if (marca < 0) {
            return null;
        }
        Path archivo = dirLog.resolve("hibernate.log");
        try (RandomAccessFile raf = new RandomAccessFile(archivo.toFile(), "r")) {
            long largo = raf.length();
            if (largo <= marca) {
                return "";
            }
            byte[] buffer = new byte[(int) (largo - marca)];
            raf.seek(marca);
            raf.readFully(buffer);
            return new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Cuenta los {@code delete from <esquema>.<tabla>} del tramo de log.
     * Hibernate escribe la sentencia formateada en varias lineas, de ahi el
     * {@code \s+} entre palabras.
     *
     * @return la cantidad, o -1 si no hubo tramo de log que revisar.
     */
    private static int contarDeletes(String tramo, String tabla) {
        return contarCoincidencias(tramo, "delete\\s+from\\s+\\w+\\." + tabla + "\\b");
    }

    /** Cuenta los select que leen la tabla como tabla principal (lleva alias). */
    private static int contarSelects(String tramo, String tabla) {
        return contarCoincidencias(tramo, "from\\s+\\w+\\." + tabla + "\\s+[a-z]");
    }

    /**
     * Cuenta los join contra la tabla. Hace falta por separado: cuando el
     * perfil interno "merge" trae la coleccion del padre en el mismo plan de
     * select (sin {@code max_fetch_depth=0}) la hija aparece como join, no
     * como select propio.
     */
    private static int contarJoins(String tramo, String tabla) {
        return contarCoincidencias(tramo, "join\\s+\\w+\\." + tabla + "\\b");
    }

    private static int contarCoincidencias(String tramo, String expresion) {
        if (tramo == null) {
            return -1;
        }
        Matcher m = Pattern.compile(expresion, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(tramo);
        int cantidad = 0;
        while (m.find()) {
            cantidad++;
        }
        return cantidad;
    }

    /** Vuelca por salida estandar lo que hay que citar en el informe. */
    private static void informar(String caso, String queViaja, IDataResult resultado,
            long hijasAntes, long hijasDespues, String tramo) {
        System.out.println("=================================================================");
        System.out.println("DELHIJA " + caso);
        System.out.println("   viaja en el IDataSet : " + queViaja);
        System.out.println("   isSuccessFul()       : " + ((resultado == null) ? "null" : resultado.isSuccessFul()));
        System.out.println("   errorMsg             : " + ((resultado == null) ? "null" : resultado.getErrorMsg()));
        System.out.println("   hijas antes/despues  : " + hijasAntes + " / " + hijasDespues);
        System.out.println("   DELETE de " + TABLA_HIJA + "  : " + contarDeletes(tramo, TABLA_HIJA));
        System.out.println("   DELETE de " + TABLA_PADRE + "      : " + contarDeletes(tramo, TABLA_PADRE));
        System.out.println("   SELECT de " + TABLA_HIJA + "  : " + contarSelects(tramo, TABLA_HIJA));
        System.out.println("   SELECT de " + TABLA_PADRE + "      : " + contarSelects(tramo, TABLA_PADRE));
        System.out.println("   JOIN   de " + TABLA_HIJA + "  : " + contarJoins(tramo, TABLA_HIJA));
        System.out.println("   JOIN   de " + TABLA_PADRE + "      : " + contarJoins(tramo, TABLA_PADRE));
        System.out.println("   lecturas de la coleccion del padre (select+join de la hija): "
                + (contarSelects(tramo, TABLA_HIJA) + contarJoins(tramo, TABLA_HIJA)));
        System.out.println("   rescate en el flush    : " + contarCoincidencias(tramo,
                "un-?schedul|deleted entity passed to persist|unScheduleDeletion"));
        System.out.println("=================================================================");
    }
}
