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

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba pura del envoltorio {@code AbstractDAO.auditSave} (plan AUDMGR, D3):
 * sus tres caminos y que la decisión la pida a {@link IAuditManager} y no la
 * resuelva él. {@code AuditManager} es un doble que registra las llamadas.
 */
public class AbstractDAOAuditSaveTest {

    private final List<String> llamadas = new ArrayList<>();
    private boolean respuesta;
    private final AbstractDAO dao = new AbstractDAO() {
    };

    @BeforeEach
    void setUp() throws Exception {
        llamadas.clear();
        IAuditManager doble = (IAuditManager) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IAuditManager.class}, (proxy, method, args) -> {
                    llamadas.add(method.getName());
                    return "isAuditAble".equals(method.getName()) ? respuesta : null;
                });
        Field campo = AbstractDAO.class.getDeclaredField("auditManager");
        campo.setAccessible(true);
        campo.set(dao, doble);
    }

    @Test
    @DisplayName("Con el flag puesto escribe sin preguntar")
    void conFlagEscribeSinDecidir() throws Exception {
        dao.auditSave(null, "S", new DataRow(), true);
        assertEquals(List.of("write"), llamadas);
    }

    @Test
    @DisplayName("Sin flag y no auditable: pregunta y no escribe")
    void sinFlagNoAuditable() throws Exception {
        respuesta = false;
        dao.auditSave(null, "S", new DataRow(), false);
        assertEquals(List.of("isAuditAble"), llamadas);
    }

    @Test
    @DisplayName("Sin flag y auditable: pregunta y después escribe")
    void sinFlagAuditable() throws Exception {
        respuesta = true;
        dao.auditSave((EntityManager) null, "S", new DataRow(), false);
        assertEquals(List.of("isAuditAble", "write"), llamadas);
    }

    @Test
    @DisplayName("Los isAuditAble del DAO delegan en AuditManager")
    void isAuditAbleDelega() {
        respuesta = true;
        assertTrue(dao.isAuditAble(new DataRow()));
        assertTrue(dao.isAuditAble("itemventa"));
        assertEquals(List.of("isAuditAble", "isAuditAble"), llamadas);
    }
}
