/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2027 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
*        jenciso@javabeanstack.org
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
package org.javabeanstack.sifen.test.vivo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.javabeanstack.sifen.simulator.Scenario;
import org.javabeanstack.sifen.simulator.SifenSimulator;
import org.javabeanstack.sifen.simulator.SimulatedService;
import org.javabeanstack.sifen.test.SifenTestEnv;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.javabeanstack.sifen.test.vivo.SifenVivoFase12Test.Estado;
import org.javabeanstack.sifen.test.vivo.SifenVivoFase12Test.EstadoNumero;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.AssertionFailedError;

/**
 * Sin red ni certificado: la clasificación de los números de la Fase 12 por la tabla de equivalencias (E12-03, E12-04,
 * M12-15) y la verificación del modo de la corrida (M12-14, I12-16) de {@link SifenVivoFase12Test}, que es
 * {@code sifen-live}. Los casos de la clasificación son los de la sonda de check_0008 (plan JBSSIF, Fase 12) más el OP-21
 * firmado, la fila «—» y la consulta de un pendiente. Tabla y CDC ficticios (RUC 80123456 del simulador).
 *
 * <p>Ciclo 3 (2026-11-25): OP-21 con el pedido de un envío cortado (E12-06, la sonda S3c de check_0009), las
 * inutilizaciones sin 0600 y 12.3.11 contra el simulador con el corte al tercer rechazo y el freno de una inutilización
 * rechazada o con excepción (M12-19, la sonda S2), el aviso del {@code queryRuc} intermitente (I12-20) y la lectura de
 * los resultados simulados del ensayo.</p>
 */
public class EstadosFase12Test {
    private static final String P = "jbs.sifen.test.f12.";
    private static final String CAB = "iditemmovimiento;numero_original;numero_enviado;cdc_original;cdc_enviado;fecha_envio;operacion;"
            + "codigo_estado;dCodRes;dEstRes;dMsgRes";
    private static final String FECHA = "2026-11-25T10:00:00";

    /** CDC ficticio de 44 dígitos con el tipo y el número dados. */
    private static String cdc(String tipo, int numero) {
        return tipo + "80123456" + "5" + "001001" + String.format("%07d", numero) + "1" + "20261125" + "1" + "000000001" + "0";
    }

    private static String fila(String id, int numero, String tipo, String op, String cod, String est) {
        return String.join(";", id, "001-002-0000001", String.format("001-001-%07d", numero), "", cdc(tipo, numero), FECHA, op,
                "200", cod, est, "");
    }

    private static Map<Integer, EstadoNumero> clasificar(Set<String> conPedido, String... filas) {
        List<String> l = new ArrayList<>();
        l.add(CAB);
        l.addAll(List.of(filas));
        return SifenVivoFase12Test.clasificar(l, "001-001", (id, c) -> conPedido.contains(id));
    }

    /** Con el pedido de envío (E12-06): {@code conEnvio} son los CDC con un {@code .pedido.xml} guardado. */
    private static Map<Integer, EstadoNumero> clasificar(Set<String> conPedido, Set<String> conEnvio, String... filas) {
        List<String> l = new ArrayList<>();
        l.add(CAB);
        l.addAll(List.of(filas));
        return SifenVivoFase12Test.clasificar(l, "001-001", (id, c) -> conPedido.contains(id), conEnvio::contains);
    }

    @Test
    public void clasificaLosCasosDeLaSonda() {
        Map<Integer, EstadoNumero> e = clasificar(Set.of("A-CORTE", "OP-21"),
                // A-CORTE: solo RESERVADO, con el pedido guardado (corte entre el envío y el registro)
                fila("A-CORTE", 1071, "01", "RESERVADO", "", ""),
                // B-SINPED: solo RESERVADO, sin pedido (nunca se envió)
                fila("B-SINPED", 1072, "01", "RESERVADO", "", ""),
                // lote enviado (0300), sin la consulta definitiva
                fila("OP-11-1", 1073, "01", "RESERVADO", "", ""),
                fila("OP-11-1", 1073, "01", "ISifenClient.sendBatch:1234567890123456", "0300", "RECEIVED"),
                // OP-21 firmado y no enviado; después, otro número de OP-21 enviado y rechazado
                fila("OP-21", 1074, "01", "RESERVADO", "", ""),
                fila("OP-21", 1074, "01", "ISifenClient.sign:firmado-sin-enviar", "", ""),
                fila("D-DESC", 1075, "01", "RESERVADO", "", ""),
                fila("D-DESC", 1075, "01", "ISifenClient.send:excepcion", "JBS-RED-03", ""),
                fila("E-RECH", 1076, "05", "RESERVADO", "", ""),
                fila("E-RECH", 1076, "05", "ISifenClient.send", "2404", "REJECTED"),
                fila("F-APROB", 1077, "01", "RESERVADO", "", ""),
                fila("F-APROB", 1077, "01", "ISifenClient.send", "0260", "APPROVED"),
                // un lote consultado con un miembro sin resultado (fila vieja con el estado vacío)
                fila("OP-10-2", 1078, "01", "ISifenClient.queryBatch:99", "", ""),
                // rechazo local y condición no cumplida: reservados y no enviados
                fila("G-LOCAL", 1079, "01", "ISifenClient.sign:rechazo-local", "JBS-VAL-01", ""),
                // un pendiente resuelto por la consulta: 0420 (no está en SIFEN) y 0422 (aprobado)
                fila("H-0420", 1080, "01", "ISifenClient.send:excepcion", "JBS-RED-03", ""),
                fila("H-0420", 1080, "01", "ISifenClient.queryDE:pendiente", "0420", "NOT_FOUND"),
                fila("I-0422", 1081, "01", "ISifenClient.send:excepcion", "JBS-RED-03", ""),
                fila("I-0422", 1081, "01", "ISifenClient.queryDE:pendiente", "0422", "APPROVED"),
                // rechazado e inutilizado
                fila("J-INU", 1082, "01", "ISifenClient.send", "1107", "REJECTED"),
                // la fila del evento de inutilización trae APPROVED (dEstRes del evento): no aprueba el DE (E12-05)
                fila("J-INU", 1082, "01", "ISifenClient.invalidateNumbers:Id 7 tipo 1", "0600", "APPROVED"),
                // estado no reconocido de SIFEN: sin estado final
                fila("K-UNREC", 1083, "01", "ISifenClient.send", "0160", "UNRECOGNIZED"),
                // filas que no son de la fase o ilegibles: se ignoran (M12-15)
                String.join(";", "—", "—", "—", "", cdc("01", 9), FECHA, "ISifenClient.queryBatch:1", "200", "0260", "APPROVED", ""),
                fila("FUERA", 1000, "01", "RESERVADO", "", ""),
                String.join(";", "OTRO", "x", "001-002-0001084", "", cdc("01", 1084), FECHA, "RESERVADO", "0", "", "", ""));
        Assertions.assertEquals(Estado.PENDIENTE, e.get(1071).estado(), e.get(1071).motivo());
        Assertions.assertEquals(Estado.NO_ENVIADO, e.get(1072).estado());
        Assertions.assertEquals(Estado.PENDIENTE, e.get(1073).estado(), "un lote sin respuesta definitiva no se inutiliza");
        Assertions.assertEquals("1234567890123456", e.get(1073).lote());
        Assertions.assertTrue(e.get(1073).deLote());
        Assertions.assertEquals(Estado.FIRMADO_SIN_ENVIAR, e.get(1074).estado());
        Assertions.assertEquals(Estado.PENDIENTE, e.get(1075).estado(), "resultado desconocido: primero consultar (D55)");
        Assertions.assertEquals(Estado.RECHAZADO, e.get(1076).estado());
        Assertions.assertEquals("05", e.get(1076).cdc().substring(0, 2));
        Assertions.assertEquals(Estado.APROBADO, e.get(1077).estado());
        Assertions.assertEquals(Estado.PENDIENTE, e.get(1078).estado(), "miembro de lote sin resultado");
        Assertions.assertEquals("99", e.get(1078).lote());
        Assertions.assertEquals(Estado.NO_ENVIADO, e.get(1079).estado());
        Assertions.assertEquals(Estado.RECHAZADO, e.get(1080).estado(), "0420 en la consulta: estado final");
        Assertions.assertEquals(Estado.APROBADO, e.get(1081).estado(), "0422 en la consulta: aprobado");
        Assertions.assertEquals(Estado.INUTILIZADO, e.get(1082).estado());
        Assertions.assertEquals(Estado.PENDIENTE, e.get(1083).estado(), "UNRECOGNIZED no es un estado final");
        Assertions.assertEquals(13, e.size(), "la fila «—», la de fuera del rango y la de otro punto no cuentan: " + e.keySet());
        Assertions.assertFalse(e.get(1074).definitivo());
        Assertions.assertTrue(e.get(1076).definitivo());
    }

    @Test
    public void op21FirmadoSinFilaDeFirmaYRechazado() {
        Map<Integer, EstadoNumero> e = clasificar(Set.of("OP-21"),
                fila("OP-21", 1071, "01", "RESERVADO", "", ""),
                fila("OP-21", 1072, "01", "RESERVADO", "", ""));
        // corte entre guardar el firmado y registrar la firma: es el firmado sin enviar, no un pendiente
        Assertions.assertEquals(Estado.FIRMADO_SIN_ENVIAR, e.get(1071).estado());
        Map<Integer, EstadoNumero> r = clasificar(Set.of("OP-21"),
                fila("OP-21", 1071, "01", "RESERVADO", "", ""),
                fila("OP-21", 1071, "01", "ISifenClient.sign:firmado-sin-enviar", "", ""),
                fila("OP-21", 1071, "01", "ISifenClient.send", "1001", "REJECTED"));
        Assertions.assertEquals(Estado.RECHAZADO, r.get(1071).estado(), "E12-04: OP-21 rechazado va a 12.3.11");
        Map<Integer, EstadoNumero> a = clasificar(Set.of("OP-21"),
                fila("OP-21", 1071, "01", "ISifenClient.sign:firmado-sin-enviar", "", ""),
                fila("OP-21", 1071, "01", "ISifenClient.send", "1005", "APPROVED_WITH_OBSERVATION"));
        Assertions.assertEquals(Estado.APROBADO, a.get(1071).estado(), "E12-04: OP-21 aprobado no se reenvía");
    }

    /**
     * E12-06 (la sonda S3c de check_0009): OP-21 firmado, con el {@code .pedido.xml} de un envío guardado y sin la fila del
     * envío, pudo llegar a SIFEN: es un pendiente que se consulta, no un «firmado sin enviar» que 12.3.12 volvería a
     * transmitir. Sin el pedido de envío sigue «firmado sin enviar».
     */
    @Test
    public void op21ConElPedidoDeUnEnvioCortadoEsPendiente_E1206() {
        String c = cdc("01", 1071);
        String[] filas = {fila("OP-21", 1071, "01", "RESERVADO", "", ""), fila("OP-21", 1071, "01", "ISifenClient.sign:firmado-sin-enviar", "", "")};
        Map<Integer, EstadoNumero> cortado = clasificar(Set.of("OP-21"), Set.of(c), filas);
        Assertions.assertEquals(Estado.PENDIENTE, cortado.get(1071).estado(), cortado.get(1071).motivo());
        Assertions.assertTrue(cortado.get(1071).motivo().contains("E12-06"), cortado.get(1071).motivo());
        Assertions.assertFalse(cortado.get(1071).definitivo(), "un pendiente no se inutiliza ni se reenvía");
        Map<Integer, EstadoNumero> sinEnvio = clasificar(Set.of("OP-21"), Set.of(), filas);
        Assertions.assertEquals(Estado.FIRMADO_SIN_ENVIAR, sinEnvio.get(1071).estado(), "sin el pedido de envío: 12.3.12 lo envía");
        // corte entre guardar el firmado y registrar la firma, y después un envío cortado: también pendiente
        Map<Integer, EstadoNumero> soloReserva = clasificar(Set.of("OP-21"), Set.of(c), fila("OP-21", 1071, "01", "RESERVADO", "", ""));
        Assertions.assertEquals(Estado.PENDIENTE, soloReserva.get(1071).estado());
        // con el resultado del envío registrado, el pedido guardado no cambia nada (E12-04)
        Map<Integer, EstadoNumero> aprobado = clasificar(Set.of("OP-21"), Set.of(c), filas[0], filas[1],
                fila("OP-21", 1071, "01", "ISifenClient.send", "1005", "APPROVED_WITH_OBSERVATION"));
        Assertions.assertEquals(Estado.APROBADO, aprobado.get(1071).estado());
        // el pedido de envío solo cuenta para OP-21: un DE firmado sin enviar no existe para los demás
        Map<Integer, EstadoNumero> otro = clasificar(Set.of(), Set.of(cdc("01", 1072)), fila("DE-05", 1072, "01", "RESERVADO", "", ""));
        Assertions.assertEquals(Estado.NO_ENVIADO, otro.get(1072).estado());
    }

    /**
     * M12-19 e I12-23: la clasificación guarda la última inutilización sin 0600 (con excepción o rechazada) y cuántas hubo,
     * y el {@code dCodRes} de la fila que decidió el estado (el reintento de DE-56 con 0160 decide entre DE-56x y la sonda).
     */
    @Test
    public void inutilizacionSinCeroSeiscientosYCodigoDelEstado_M1219() {
        Map<Integer, EstadoNumero> e = clasificar(Set.of(),
                fila("A-EXC", 1071, "01", "ISifenClient.send", "0160", "REJECTED"),
                fila("A-EXC", 1071, "01", "ISifenClient.invalidateNumbers:excepcion", "", ""),
                fila("B-RECH", 1072, "01", "ISifenClient.send", "1862", "REJECTED"),
                fila("B-RECH", 1072, "01", "ISifenClient.invalidateNumbers:Id 9 tipo 1", "4066", "REJECTED"),
                fila("C-OK", 1073, "01", "ISifenClient.send", "0160", "REJECTED"),
                fila("C-OK", 1073, "01", "ISifenClient.invalidateNumbers:excepcion", "", ""),
                fila("C-OK", 1073, "01", "ISifenClient.invalidateNumbers:Id 10 tipo 1", "0600", "APPROVED"),
                fila("DE-56", 1074, "01", "ISifenClient.send", "0160", "REJECTED"),
                fila("DE-56", 1075, "01", "RESERVADO", "", ""),
                fila("DE-56", 1075, "01", "ISifenClient.send", "0160", "REJECTED"),
                fila("D-0420", 1076, "01", "ISifenClient.send", "0100", "UNRECOGNIZED"),
                fila("D-0420", 1076, "01", "ISifenClient.queryDE:pendiente", "0420", "NOT_FOUND"));
        Assertions.assertEquals(Estado.RECHAZADO, e.get(1071).estado(), "la inutilización con excepción no cambia el estado del DE");
        Assertions.assertEquals("excepcion", e.get(1071).inutilizacion());
        Assertions.assertEquals(1, e.get(1071).intentosInutilizacion());
        Assertions.assertEquals("rechazada 4066", e.get(1072).inutilizacion());
        Assertions.assertEquals(Estado.INUTILIZADO, e.get(1073).estado());
        Assertions.assertNull(e.get(1073).inutilizacion(), "con el 0600 ya no hay nada que frenar");
        Assertions.assertEquals("0160", e.get(1075).codigo(), "el código del reintento de DE-56");
        Assertions.assertEquals(Estado.RECHAZADO, e.get(1075).estado());
        Assertions.assertEquals("0420", e.get(1076).codigo());
        Assertions.assertNull(e.get(1076).inutilizacion());
    }

    /** I12-20: el 0160 intermitente de {@code queryRuc} es un aviso con {@code checkConnection} ok; otra cosa decide. */
    @Test
    public void elQueryRucIntermitenteEsUnAvisoConLaConexionOk_I1220() {
        Assertions.assertNotNull(SifenVivoFase12Test.avisoDeQueryRuc(true, "JBS-RESP-02"));
        Assertions.assertNull(SifenVivoFase12Test.avisoDeQueryRuc(false, "JBS-RESP-02"), "sin conexión decide");
        Assertions.assertNull(SifenVivoFase12Test.avisoDeQueryRuc(true, "JBS-CONEX-01"), "otra excepción decide");
    }

    /** Ciclo 3: los resultados simulados del ensayo ({@code id:código}, en orden) y la clave del estimado de cada id. */
    @Test
    public void resultadosSimuladosYClaveDelEstimado() {
        Map<String, String> m = SifenVivoFase12Test.simulados("DE-12:0160, DE-13:1862,DE-44:no-enviado");
        Assertions.assertEquals(List.of("DE-12", "DE-13", "DE-44"), List.copyOf(m.keySet()));
        Assertions.assertEquals("no-enviado", m.get("DE-44"));
        Assertions.assertTrue(SifenVivoFase12Test.simulados(null).isEmpty());
        Assertions.assertThrows(AssertionFailedError.class, () -> SifenVivoFase12Test.simulados("DE-12"));
        Assertions.assertEquals("OP-10", SifenVivoFase12Test.clave("OP-10-4"));
        Assertions.assertEquals("OP-31", SifenVivoFase12Test.clave("OP-31-NC"));
        Assertions.assertEquals("DE-56s", SifenVivoFase12Test.clave("DE-56s"));
        // simular.resultados es solo del ensayo
        falla(props("vivo", "SIFEN-test", "tandas", "reintentos", "simular.resultados", "DE-12:0160"), "solo vale en el ensayo");
    }

    // ------------------------------------------------------------------ 12.3.11 contra el simulador (M12-18, M12-19)

    @TempDir
    Path dir;

    /**
     * Entorno de un {@code .env} temporal (datos ficticios; como {@code ReemisionPruebaTest}): de él sale solo el emisor de
     * prueba; el cliente usa {@link FixturesSifen#configuracion()} (el autofirmado del módulo).
     */
    private SifenTestEnv.Entorno entorno() throws Exception {
        Path pfx = FixturesSifen.copiarPfx(dir.resolve("a.pfx"));
        Path env = dir.resolve("prueba.env");
        Files.write(env, ("SIFEN_AMBIENTE=DEV\nSIFEN_CERT_PATH=" + pfx + "\nSIFEN_CERT_PASS=x\nSIFEN_CSC_ID=0001\nSIFEN_CSC=y\n"
                + "SIFEN_EMISOR_RUC=80123456\nSIFEN_EMISOR_DV=5\nSIFEN_TIMBRADO=80123456\nSIFEN_TIMBRADO_FECHA_INICIO=2024-10-31\n"
                + "SIFEN_ESTABLECIMIENTO=001\nSIFEN_PUNTO_EXPEDICION=001\nSIFEN_EMISOR_EMAIL=emisor.prueba@example.com\n")
                .getBytes(StandardCharsets.ISO_8859_1));
        return SifenTestEnv.cargar(env);
    }

    /** Tabla con seis números rechazados (0001071–0001076), sin inutilizar. */
    private Path tablaConSeisRechazados() throws Exception {
        List<String> l = new ArrayList<>();
        l.add(CAB);
        for (int n = 1071; n <= 1076; n++) {
            l.add(fila("R-" + n, n, "01", "RESERVADO", "", ""));
            l.add(fila("R-" + n, n, "01", "ISifenClient.send", "0160", "REJECTED"));
        }
        Path t = dir.resolve("equivalencias.csv");
        Files.write(t, l, StandardCharsets.UTF_8);
        return t;
    }

    private static long filas(Path salida, String texto) throws Exception {
        return Files.readAllLines(salida.resolve("simulacion.tsv"), StandardCharsets.UTF_8).stream().filter(x -> x.contains(texto)).count();
    }

    /**
     * M12-19 (la sonda S2 de check_0009): SIFEN rechaza toda inutilización; 12.3.11 corta al tercer rechazo seguido (tres
     * eventos de seis), y el resumen no cuenta los rechazados como inutilizados (I12-19). La 12.3.11 siguiente no repite
     * los tres rechazados (se frenan y la tanda falla), inutiliza los otros tres y, con {@code reintentar=I-1071}, repite
     * ese una sola vez.
     */
    @Test
    public void corteAlTercerRechazoYFrenoDeLasRechazadas_M1219() throws Exception {
        SifenTestEnv.Entorno env = entorno();
        Path tabla = tablaConSeisRechazados();
        Path salida = dir.resolve("salida");
        try (SifenSimulator sim = SifenSimulator.start()) {
            sim.on(SimulatedService.EVENT, Scenario.eventRejected("4066", "Rechazo simulado de la inutilizacion"));
            SifenVivoFase12Test t = SifenVivoFase12Test.paraInutilizacionesSinRed(env.getEmisor(), FixturesSifen.configuracion(), sim, tabla, salida, Set.of());
            t.inutilizacionesSinRed();
            Assertions.assertEquals(3, sim.requests(SimulatedService.EVENT).size(), "corta al tercer rechazo seguido");
            Assertions.assertTrue(t.detenidoSinRed());
            Assertions.assertTrue(t.fallasSinRed().stream().anyMatch(x -> x.contains("tres resultados seguidos")), t.fallasSinRed().toString());
            Assertions.assertEquals(1, filas(salida, "\tcorte\t"), "una fila «corte»");
            Assertions.assertEquals(1, filas(salida, "números inutilizados (0600): 0 · rechazados: 3"), "I12-19: los rechazados no son inutilizados");
            Map<Integer, EstadoNumero> e = clasificarTabla(tabla);
            Assertions.assertEquals("rechazada 4066", e.get(1071).inutilizacion());
            Assertions.assertNull(e.get(1074).inutilizacion(), "el cuarto no se intentó");

            sim.on(SimulatedService.EVENT, Scenario.eventRegistered());
            SifenVivoFase12Test t2 = SifenVivoFase12Test.paraInutilizacionesSinRed(env.getEmisor(), FixturesSifen.configuracion(), sim, tabla, salida, Set.of());
            t2.inutilizacionesSinRed();
            Assertions.assertEquals(6, sim.requests(SimulatedService.EVENT).size(), "solo los tres que no se intentaron");
            Assertions.assertFalse(t2.detenidoSinRed());
            Assertions.assertTrue(t2.fallasSinRed().stream().anyMatch(x -> x.contains("sin 0600, no repetidas")), t2.fallasSinRed().toString());
            Assertions.assertEquals(1, filas(salida, "números inutilizados (0600): 3 · rechazados: 0 · con excepción: 0 · frenados (M12-19): 3"));
            Map<Integer, EstadoNumero> e2 = clasificarTabla(tabla);
            Assertions.assertEquals(Estado.INUTILIZADO, e2.get(1074).estado());
            Assertions.assertEquals(Estado.RECHAZADO, e2.get(1071).estado());

            SifenVivoFase12Test t3 = SifenVivoFase12Test.paraInutilizacionesSinRed(env.getEmisor(), FixturesSifen.configuracion(), sim, tabla, salida, Set.of("I-1071"));
            t3.inutilizacionesSinRed();
            Assertions.assertEquals(7, sim.requests(SimulatedService.EVENT).size(), "I-1071 autorizado: una vez");
            Assertions.assertEquals(Estado.INUTILIZADO, clasificarTabla(tabla).get(1071).estado());
            Assertions.assertEquals(1, filas(salida, "\treintento autorizado\t"));
        }
    }

    /**
     * M12-18 y M12-19: una inutilización con excepción (la conexión se corta: resultado desconocido) detiene 12.3.11 y queda
     * escrita; la 12.3.11 siguiente no la repite sin aviso (resultado desconocido: pudo quedar registrada).
     */
    @Test
    public void inutilizacionConExcepcionDetieneYNoSeRepiteSinAviso_M1218() throws Exception {
        SifenTestEnv.Entorno env = entorno();
        Path tabla = tablaConSeisRechazados();
        Path salida = dir.resolve("salida");
        try (SifenSimulator sim = SifenSimulator.start()) {
            sim.once(SimulatedService.EVENT, Scenario.connectionCut());
            SifenVivoFase12Test t = SifenVivoFase12Test.paraInutilizacionesSinRed(env.getEmisor(), FixturesSifen.configuracion(), sim, tabla, salida, Set.of());
            t.inutilizacionesSinRed();
            Assertions.assertEquals(1, sim.requests(SimulatedService.EVENT).size(), "la primera excepción detiene la tanda");
            Assertions.assertTrue(t.detenidoSinRed());
            Assertions.assertTrue(t.fallasSinRed().stream().anyMatch(x -> x.contains("M12-18")), t.fallasSinRed().toString());
            Assertions.assertEquals("excepcion", clasificarTabla(tabla).get(1071).inutilizacion());

            SifenVivoFase12Test t2 = SifenVivoFase12Test.paraInutilizacionesSinRed(env.getEmisor(), FixturesSifen.configuracion(), sim, tabla, salida, Set.of());
            t2.inutilizacionesSinRed();
            Assertions.assertEquals(6, sim.requests(SimulatedService.EVENT).size(), "los otros cinco; el de la excepción no se repite");
            Assertions.assertEquals(1, filas(salida, "quedó con excepción (resultado desconocido: pudo quedar registrada); no se repite"));
            Assertions.assertEquals(1, filas(salida, "números inutilizados (0600): 5 · rechazados: 0 · con excepción: 0 · frenados (M12-19): 1"));
            Assertions.assertEquals(Estado.RECHAZADO, clasificarTabla(tabla).get(1071).estado());
        }
    }

    private static Map<Integer, EstadoNumero> clasificarTabla(Path tabla) throws Exception {
        return SifenVivoFase12Test.clasificar(Files.readAllLines(tabla, StandardCharsets.UTF_8), "001-001", (id, c) -> false);
    }

    // ------------------------------------------------------------------ modo de la corrida (M12-14, I12-16)

    private static Properties props(String... kv) {
        Properties p = new Properties();
        for (int i = 0; i < kv.length; i += 2) {
            p.setProperty(P + kv[i], kv[i + 1]);
        }
        return p;
    }

    private static void falla(Properties p, String texto) {
        AssertionFailedError x = Assertions.assertThrows(AssertionFailedError.class, () -> SifenVivoFase12Test.verificarModo(p));
        Assertions.assertTrue(x.getMessage().contains(texto), x.getMessage());
    }

    @Test
    public void elVivoNoEstaAUnBooleanoDelEnsayo() {
        // sin simular=true y sin la llave: ni ensayo ni vivo
        falla(props("tandas", "12.3.2"), "vivo=SIFEN-test");
        falla(props("tandas", "12.3.2", "vivo", "true"), "vivo=SIFEN-test");
        // el ensayo no lleva la llave
        falla(props("simular", "true", "vivo", "SIFEN-test"), "el ensayo en seco no lleva la llave");
        // en vivo, las propiedades del ensayo no valen
        falla(props("vivo", "SIFEN-test", "tandas", "12.3.2", "simular.tabla", "/x"), "solo vale en el ensayo");
        falla(props("vivo", "SIFEN-test", "tandas", "12.3.2", "simular.corte", "DE-40"), "solo vale en el ensayo");
        falla(props("vivo", "SIFEN-test"), "se elige la tanda");
        SifenVivoFase12Test.verificarModo(props("vivo", "SIFEN-test", "tandas", "12.3.2"));
        SifenVivoFase12Test.verificarModo(props("simular", "true", "simular.corte", "DE-40"));
    }

    @Test
    public void constanciaDeMksifYReintentos() {
        falla(props("vivo", "SIFEN-test", "tandas", "12.3.9"), "mksif=<constancia");
        falla(props("vivo", "SIFEN-test", "tandas", "12.3.8,12.3.10"), "mksif=<constancia");
        falla(props("vivo", "SIFEN-test", "tandas", "12.3.9", "mksif", "   "), "vacío no es una constancia");
        falla(props("simular", "true", "mksif", ""), "vacío no es una constancia");
        SifenVivoFase12Test.verificarModo(props("vivo", "SIFEN-test", "tandas", "12.3.9,12.3.10", "mksif", "MKSIF sin emitir: X"));
        // un lote no se reintenta
        falla(props("vivo", "SIFEN-test", "tandas", "12.3.9", "mksif", "x", "reintentar", "OP-10"), "un lote no se reintenta");
        falla(props("simular", "true", "reintentar", "DE-09,OP-11-3"), "un lote no se reintenta");
        SifenVivoFase12Test.verificarModo(props("vivo", "SIFEN-test", "tandas", "12.3.2", "reintentar", "ID-F09"));
    }

    @Test
    public void laSalidaNoLlevaCdcCompletos() {
        String c = cdc("05", 1192);
        String l = SifenVivoFase12Test.enmascarar("NC\t" + c + " · dCdCDERef = " + cdc("01", 1191) + "\tlote 1234567890123456");
        Assertions.assertFalse(l.contains(c), l);
        Assertions.assertFalse(l.contains("80123456"), "el RUC del emisor no queda a la vista: " + l);
        Assertions.assertTrue(l.contains("05…" + c.substring(40)), l);
        Assertions.assertTrue(l.contains("1234567890123456"), "un número de lote (16 dígitos) no se toca");
    }
}
