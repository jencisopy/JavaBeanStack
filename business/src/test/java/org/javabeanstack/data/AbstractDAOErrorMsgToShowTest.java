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

import java.sql.SQLException;
import jakarta.ejb.EJBTransactionRolledbackException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceException;
import org.hibernate.AssertionFailure;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.GenericJDBCException;
import org.javabeanstack.exceptions.CheckException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Qué texto recibe el usuario cuando una grabación falla
 * ({@code AbstractDAO.getErrorMsgToShow}).
 *
 * <p>Es un test PURO sobre la clasificación del error: no necesita
 * {@code EntityManager} ni servidor, porque lo único que se mide es de qué
 * clase de fallo se trata y qué texto le corresponde. El fallo de
 * infraestructura se resume en un mensaje accionable —el detalle sigue yendo
 * al log y al resultado de la grabación— y todo lo demás conserva el texto de
 * siempre.</p>
 *
 * @author Jorge Enciso
 */
@DisplayName("AbstractDAO: el texto del fallo de una grabación")
public class AbstractDAOErrorMsgToShowTest {

    /** Fragmento del texto fijo que recibe el usuario ante un fallo técnico. */
    private static final String TEXTO_FIJO = "No se pudo completar la operación";

    /** DAO de prueba: la clase no tiene métodos abstractos ni estado propio. */
    private final AbstractDAO dao = new AbstractDAO() {
    };

    @Test
    @DisplayName("La transacción ya marcada para deshacerse no se le muestra al usuario")
    public void testTransaccionMarcada() {
        SQLException root = new SQLException(
                "IJ031070: Transaction cannot proceed: STATUS_MARKED_ROLLBACK");
        Exception exp = new PersistenceException(new GenericJDBCException(
                "Could not prepare statement", root));

        assertTrue(dao.isInfrastructureError(exp));
        String msg = dao.getErrorMsgToShow(exp);
        assertTrue(msg.contains(TEXTO_FIJO), msg);
        assertFalse(msg.contains("IJ031070"), "el volcado técnico llegó al usuario: " + msg);
    }

    @Test
    @DisplayName("El contexto de persistencia envenenado tampoco")
    public void testContextoEnvenenado() {
        Exception exp = new AssertionFailure("Entry for instance of 'AppLogEvent' "
                + "has a null identifier");

        assertTrue(dao.isInfrastructureError(exp));
        String msg = dao.getErrorMsgToShow(exp);
        assertTrue(msg.contains(TEXTO_FIJO), msg);
        assertFalse(msg.contains("null identifier"), "el volcado técnico llegó al usuario: " + msg);
    }

    @Test
    @DisplayName("El mensaje de un trigger de la base SÍ se le muestra: es una regla de negocio")
    public void testMensajeDeTrigger() {
        //Un error definido por el usuario (>= 50000) no lo clasifica el dialecto
        //y termina en la excepción JDBC generica, que por eso NO esta en la lista
        //de tipos de infraestructura.
        SQLException root = new SQLException(
                "TR03: No puede pasar del estado confirmado a un estado no confirmado",
                "S0001", 50000);
        Exception exp = new PersistenceException(new GenericJDBCException(
                "could not execute statement", root));

        assertFalse(dao.isInfrastructureError(exp));
        assertTrue(dao.getErrorMsgToShow(exp).contains("estado confirmado"));
    }

    @Test
    @DisplayName("Una validación envuelta por el contenedor SÍ se le muestra")
    public void testValidacionEnvueltaPorElContenedor() {
        //`CheckException` no esta anotada como excepcion de aplicacion, asi que
        //al cruzar el limite de un bean el contenedor la envuelve. Si el tipo de
        //la envoltura contara como infraestructura, se perderia el texto de la
        //validacion.
        Exception exp = new EJBTransactionRolledbackException(
                "El cliente tiene movimientos y no se puede borrar",
                new CheckException("El cliente tiene movimientos y no se puede borrar"));

        assertFalse(dao.isInfrastructureError(exp));
        assertTrue(dao.getErrorMsgToShow(exp).contains("no se puede borrar"));
    }

    @Test
    @DisplayName("La violación de integridad SÍ se le muestra: dice algo que se puede corregir")
    public void testViolacionDeIntegridad() {
        SQLException root = new SQLException("The DELETE statement conflicted with the "
                + "REFERENCE constraint \"FK_Itemmovimiento_Ciudad\"");
        Exception exp = new PersistenceException(new ConstraintViolationException(
                "could not execute statement", root, "FK_Itemmovimiento_Ciudad"));

        assertFalse(dao.isInfrastructureError(exp));
        assertTrue(dao.getErrorMsgToShow(exp).contains("REFERENCE constraint"));
    }

    @Test
    @DisplayName("La fila que no existe SÍ se le muestra, y nombra la fila (DELHIJA RF4)")
    public void testFilaInexistente() {
        Exception exp = new EntityNotFoundException(
                "AppUserFormViewColumn con id -123456789 no existe");

        assertFalse(dao.isInfrastructureError(exp));
        String msg = dao.getErrorMsgToShow(exp);
        assertTrue(msg.contains("AppUserFormViewColumn"), msg);
        assertTrue(msg.contains("-123456789"), msg);
    }

    @Test
    @DisplayName("El error de negocio llega tal cual, que es para lo que se escribió")
    public void testErrorDeNegocio() {
        Exception exp = new CheckException("Esta fecha esta fuera del periodo valido de carga");

        assertFalse(dao.isInfrastructureError(exp));
        assertTrue(dao.getErrorMsgToShow(exp)
                .contains("Esta fecha esta fuera del periodo valido de carga"));
    }

    @Test
    @DisplayName("El error de negocio con una causa técnica adentro se trata como técnico")
    public void testErrorDeNegocioConCausaTecnica() {
        //Se prioriza la causa: el texto de negocio de afuera describiría mal lo
        //que pasó, porque lo que falló es la infraestructura.
        Exception exp = new CheckException("No se pudo verificar el registro",
                new AssertionFailure("has a null identifier"));

        assertTrue(dao.isInfrastructureError(exp));
        assertTrue(dao.getErrorMsgToShow(exp).contains(TEXTO_FIJO));
    }

    @Test
    @DisplayName("Una cadena de causas cíclica no cuelga la clasificación")
    public void testCadenaCiclica() {
        Exception uno = new Exception("uno");
        Exception dos = new Exception("dos", uno);
        uno.initCause(dos);

        assertFalse(dao.isInfrastructureError(uno));
    }
}
