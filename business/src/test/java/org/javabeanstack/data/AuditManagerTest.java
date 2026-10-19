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
import java.util.Map;
import jakarta.persistence.Column;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.javabeanstack.annotation.AuditEntity;
import org.javabeanstack.security.ISessions;
import org.javabeanstack.security.model.IUserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba pura de {@link AuditManager} (plan AUDMGR, §1.3.1): resolución de los
 * tres nombres, caché de la consulta a {@code AppAudit}, que un fallo del
 * catálogo no se convierta en «no se audita», y la fila que escribe
 * {@code write()}. El DAO, las sesiones y el {@code EntityManager} son dobles
 * en memoria.
 */
public class AuditManagerTest {

    /** Ternas consultadas al catálogo, en orden. */
    private final List<Map<String, Object>> consultas = new ArrayList<>();
    /** Entidades marcadas para auditar en el AppAudit simulado. */
    private final List<String> auditadas = new ArrayList<>();
    /** Lo que persistió el EntityManager. */
    private final List<Object> persistidos = new ArrayList<>();
    private boolean catalogoCaido;
    private IUserSession sesion;
    private AuditManager manager;

    @BeforeEach
    void setUp() throws Exception {
        consultas.clear();
        auditadas.clear();
        persistidos.clear();
        catalogoCaido = false;
        sesion = sesion("10.0.0.5", false);
        manager = new AuditManager();
        inyectar("dao", Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IGenericDAO.class},
                (proxy, method, args) -> {
                    if (!"findListByQuery".equals(method.getName())) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    if (catalogoCaido) {
                        throw new IllegalStateException("catálogo caído");
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> params = (Map<String, Object>) args[2];
                    consultas.add(params);
                    List<IDataRow> filas = new ArrayList<>();
                    if (params.values().stream().anyMatch(auditadas::contains)) {
                        filas.add(new Cliente());
                    }
                    return filas;
                }));
        inyectar("sessions", Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ISessions.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getUserSession":
                            return sesion;
                        case "getDBLinkInfo":
                            return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IDBLinkInfo.class},
                                    (p, m, a) -> switch (m.getName()) {
                                        case "getAppUserSeal" -> "SELLO";
                                        case "getUuidDevice" -> "tablet-1";
                                        default -> null;
                                    });
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                }));
        manager.clearAuditCache();
    }

    private void inyectar(String campo, Object valor) throws Exception {
        Field f = AuditManager.class.getDeclaredField(campo);
        f.setAccessible(true);
        f.set(manager, valor);
    }

    private IUserSession sesion(String ip, boolean porToken) {
        return (IUserSession) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IUserSession.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getIp" -> ip;
                    case "getClientAuthRequestInfo" -> porToken
                            ? Proxy.newProxyInstance(getClass().getClassLoader(),
                                    new Class<?>[]{org.javabeanstack.security.model.IClientAuthRequestInfo.class},
                                    (p, m, a) -> null)
                            : null;
                    default -> null;
                });
    }

    private EntityManager em() {
        return (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EntityManager.class},
                (proxy, method, args) -> {
                    if ("persist".equals(method.getName())) {
                        persistidos.add(args[0]);
                        return null;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    @Test
    @DisplayName("Resuelve clase, tabla y @AuditEntity, y responde lo del catálogo")
    void resuelveLosTresNombres() {
        auditadas.add("clientes_tabla");
        assertTrue(manager.isAuditAble(new Cliente()));
        assertEquals(1, consultas.size());
        Map<String, Object> terna = consultas.get(0);
        assertEquals("cliente", terna.get("entity1"));
        assertEquals("clientes_tabla", terna.get("entity2"));
        assertEquals("clientex", terna.get("entity3"));
    }

    @Test
    @DisplayName("La respuesta se cachea por terna y clearAuditCache la vacía")
    void cacheYLimpieza() {
        assertFalse(manager.isAuditAble(new Cliente()));
        assertFalse(manager.isAuditAble(new Cliente()));
        assertEquals(1, consultas.size());
        manager.clearAuditCache();
        auditadas.add("cliente");
        assertTrue(manager.isAuditAble(new Cliente()));
        assertEquals(2, consultas.size());
    }

    @Test
    @DisplayName("Sin clase de auditoría no se audita, y marcada con setAuditAble sí: ninguna consulta")
    void sinConsulta() {
        Cliente sinAuditoria = new Cliente();
        sinAuditoria.setAuditClass(null);
        assertFalse(manager.isAuditAble(sinAuditoria));
        Cliente marcada = new Cliente();
        marcada.setAuditAble(true);
        assertTrue(manager.isAuditAble(marcada));
        assertTrue(consultas.isEmpty());
    }

    @Test
    @DisplayName("Por nombre único: la misma palabra en los tres lugares; por clase: instancia y resuelve")
    void porNombreYPorClase() {
        auditadas.add("itemventa");
        assertTrue(manager.isAuditAble("itemventa"));
        assertEquals("itemventa", consultas.get(0).get("entity3"));
        @SuppressWarnings({"unchecked", "rawtypes"})
        Class<IDataRow> clase = (Class) Cliente.class;
        assertFalse(manager.isAuditAble(clase));
    }

    @Test
    @DisplayName("Con el catálogo caído falla: no se convierte en «no se audita»")
    void catalogoCaidoFalla() {
        catalogoCaido = true;
        assertThrows(IllegalStateException.class, () -> manager.isAuditAble(new Cliente()));
    }

    @Test
    @DisplayName("write: la fila lleva operación, datos copiados, sesión, IP, dispositivo y sello")
    void escribeLaFila() throws Exception {
        Cliente cliente = new Cliente();
        cliente.setAction(IDataRow.UPDATE);
        manager.write(em(), "S1", cliente);
        assertEquals(1, persistidos.size());
        AuditCliente fila = (AuditCliente) persistidos.get(0);
        assertEquals("U", fila.getOperacion());
        assertEquals(7L, fila.getIdcliente());
        assertEquals("Ana", fila.getNombre());
        assertEquals("S1", fila.getSessionid());
        assertEquals("10.0.0.5", fila.getIprequest());
        assertEquals("10.0.0.5", fila.getDevice());
        assertEquals("SELLO", fila.getAppuser());
        //write no decide: no consulta el catálogo
        assertTrue(consultas.isEmpty());
    }

    @Test
    @DisplayName("write por token: el dispositivo es «uuidDevice (ip)»; sin sesión, IP y dispositivo nulos")
    void escribeConTokenYSinSesion() throws Exception {
        sesion = sesion("181.91.86.240", true);
        Cliente alta = new Cliente();
        alta.setAction(IDataRow.INSERT);
        manager.write(em(), "T1", alta);
        AuditCliente fila = (AuditCliente) persistidos.get(0);
        assertEquals("I", fila.getOperacion());
        assertEquals("tablet-1 (181.91.86.240)", fila.getDevice());

        sesion = null;
        Cliente baja = new Cliente();
        baja.setAction(IDataRow.DELETE);
        manager.write(em(), "X", baja);
        AuditCliente sinSesion = (AuditCliente) persistidos.get(1);
        assertEquals("D", sinSesion.getOperacion());
        assertNull(sinSesion.getIprequest());
        assertNull(sinSesion.getDevice());
        assertEquals("", sinSesion.getAppuser());
    }

    @Test
    @DisplayName("write con la clase de auditoría vieja: IP en maquina, sin device ni appuser, sin fallar")
    void escribeConModeloViejo() throws Exception {
        ClienteViejo cliente = new ClienteViejo();
        cliente.setAction(IDataRow.INSERT);
        manager.write(em(), "S1", cliente);
        AuditClienteViejo fila = (AuditClienteViejo) persistidos.get(0);
        assertEquals("I", fila.getOperacion());
        assertEquals("10.0.0.5", fila.getIpRequest());
        assertEquals("Ana", fila.getNombre());
    }

    @Test
    @DisplayName("write: lectura = R, acción desconocida = ?")
    void operacionesRyDesconocida() throws Exception {
        Cliente leida = new Cliente();
        leida.setAction(IDataRow.READ);
        manager.write(em(), "S1", leida);
        Cliente rara = new Cliente();
        rara.setAction(99);
        manager.write(em(), "S1", rara);
        assertEquals("R", ((AuditCliente) persistidos.get(0)).getOperacion());
        assertEquals("?", ((AuditCliente) persistidos.get(1)).getOperacion());
    }

    @Test
    @DisplayName("Una clase que no se puede instanciar no se audita, sin consultar")
    void claseNoInstanciable() {
        @SuppressWarnings({"unchecked", "rawtypes"})
        Class<IDataRow> abstracta = (Class) Abstracta.class;
        assertFalse(manager.isAuditAble(abstracta));
        assertTrue(consultas.isEmpty());
    }

    /** Entidad que no se puede instanciar. */
    public abstract static class Abstracta extends DataRow {
    }

    /** Entidad con la clase de auditoría de la forma vieja. */
    public static class ClienteViejo extends Cliente {

        public ClienteViejo() {
            setAuditClass(AuditClienteViejo.class);
        }
    }

    /** Forma vieja: ipRequest sobre maquina, sin device ni appuser. */
    public static class AuditClienteViejo extends DataRow {

        @Id
        private Long idaudit = 1L;
        @Column(name = "nombre")
        private String nombre;
        @Column(name = "operacion")
        private String operacion;
        @Column(name = "sessionid")
        private String sessionid;
        @Column(name = "maquina")
        private String ipRequest;

        public Long getIdaudit() {
            return idaudit;
        }

        public void setIdaudit(Long idaudit) {
            this.idaudit = idaudit;
        }

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public String getOperacion() {
            return operacion;
        }

        public void setOperacion(String operacion) {
            this.operacion = operacion;
        }

        public String getSessionid() {
            return sessionid;
        }

        public void setSessionid(String sessionid) {
            this.sessionid = sessionid;
        }

        public String getIpRequest() {
            return ipRequest;
        }

        public void setIpRequest(String ipRequest) {
            this.ipRequest = ipRequest;
        }
    }

    /** Entidad auditada de prueba. */
    @Table(name = "CLIENTES_TABLA")
    @AuditEntity(name = "ClienteX")
    public static class Cliente extends DataRow {

        @Id
        private Long idcliente = 7L;
        @Column(name = "nombre")
        private String nombre = "Ana";

        public Cliente() {
            setAuditClass(AuditCliente.class);
        }

        public Long getIdcliente() {
            return idcliente;
        }

        public void setIdcliente(Long idcliente) {
            this.idcliente = idcliente;
        }

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }
    }

    /** Su clase de auditoría, con la cabecera nueva (iprequest + device). */
    public static class AuditCliente extends DataRow {

        @Id
        private Long idaudit = 1L;
        //Con @Column, como en las entidades reales: copyTo solo copia los
        //atributos anotados de la clase destino.
        @Column(name = "idcliente")
        private Long idcliente;
        @Column(name = "nombre")
        private String nombre;
        @Column(name = "operacion")
        private String operacion;
        @Column(name = "sessionid")
        private String sessionid;
        @Column(name = "appuser")
        private String appuser;
        @Column(name = "iprequest")
        private String iprequest;
        @Column(name = "maquina")
        private String device;

        public Long getIdaudit() {
            return idaudit;
        }

        public void setIdaudit(Long idaudit) {
            this.idaudit = idaudit;
        }

        public Long getIdcliente() {
            return idcliente;
        }

        public void setIdcliente(Long idcliente) {
            this.idcliente = idcliente;
        }

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public String getOperacion() {
            return operacion;
        }

        public void setOperacion(String operacion) {
            this.operacion = operacion;
        }

        public String getSessionid() {
            return sessionid;
        }

        public void setSessionid(String sessionid) {
            this.sessionid = sessionid;
        }

        public String getAppuser() {
            return appuser;
        }

        public void setAppuser(String appuser) {
            this.appuser = appuser;
        }

        public String getIprequest() {
            return iprequest;
        }

        public void setIprequest(String iprequest) {
            this.iprequest = iprequest;
        }

        public String getDevice() {
            return device;
        }

        public void setDevice(String device) {
            this.device = device;
        }
    }
}
