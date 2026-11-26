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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.javabeanstack.sifen.ISifenClient;
import org.javabeanstack.sifen.SifenClient;
import org.javabeanstack.sifen.api.BatchQueryResult;
import org.javabeanstack.sifen.api.BatchSendResult;
import org.javabeanstack.sifen.api.ConnectionDiagnosis;
import org.javabeanstack.sifen.api.DEQueryResult;
import org.javabeanstack.sifen.api.EventResult;
import org.javabeanstack.sifen.api.NumberRange;
import org.javabeanstack.sifen.api.ResultStatus;
import org.javabeanstack.sifen.api.RucQueryResult;
import org.javabeanstack.sifen.api.SendResult;
import org.javabeanstack.sifen.api.SignedDocument;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.beans.EventosDE;
import org.javabeanstack.sifen.core.exceptions.ErrorCode;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.fields.request.de.TgCamDEAsoc;
import org.javabeanstack.sifen.core.fields.request.de.TgCamItem;
import org.javabeanstack.sifen.core.fields.request.de.TgDatRec;
import org.javabeanstack.sifen.core.fields.request.de.TgEmis;
import org.javabeanstack.sifen.core.fields.request.de.TgPaConEIni;
import org.javabeanstack.sifen.core.fields.request.event.TgGroupTiEvt;
import org.javabeanstack.sifen.core.fields.request.event.TrGeVeConf;
import org.javabeanstack.sifen.core.fields.request.event.TrGeVeDescon;
import org.javabeanstack.sifen.core.fields.request.event.TrGeVeDisconf;
import org.javabeanstack.sifen.core.fields.request.event.TrGeVeNom;
import org.javabeanstack.sifen.core.fields.request.event.TrGeVeNotRec;
import org.javabeanstack.sifen.core.fields.request.event.TrGesEve;
import org.javabeanstack.sifen.core.types.TTiDE;
import org.javabeanstack.sifen.core.types.TiNatRec;
import org.javabeanstack.sifen.core.types.TiTipDocAso;
import org.javabeanstack.sifen.core.validation.SifenValidationException;
import org.javabeanstack.sifen.core.validation.ValidationError;
import org.javabeanstack.sifen.core.validation.ValidationResult;
import org.javabeanstack.sifen.simulator.Scenario;
import org.javabeanstack.sifen.simulator.SifenSimulator;
import org.javabeanstack.sifen.simulator.SimulatedService;
import org.javabeanstack.sifen.test.SifenTestEnv;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.CasosFase12;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.javabeanstack.sifen.test.casos.XmlCanonico;
import org.javabeanstack.sifen.test.fase12.EsperadosFase12;
import org.javabeanstack.sifen.test.fase12.PreparacionVivoFase12;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

/**
 * Envíos en vivo de la batería ampliada de la Fase 12 (plan JBSSIF, §12.3; M12-12) contra SIFEN-test, por
 * {@link ISifenClient}, con la mecánica de {@link SifenVivoBetaTest}: número reservado en la tabla de equivalencias
 * antes de cada envío, validación previa activa (los negativos de Q39 van por un cliente aparte sin validación,
 * después de mostrar lo que dice la local), reanudación tras un corte y selección de tandas.
 *
 * <p><b>Qué recorre.</b> La lista {@code envios_12_3.tsv} (una fila por caso, con su tanda, su número estimado, el
 * {@code dCodRes} esperado y la condición), en el orden de la columna {@code orden}, con 12.3.11 (inutilizaciones) y
 * 12.3.12 (casos atados al calendario) siempre al final. Cada documento sale de su catálogo por
 * {@code esperados.tsv} ({@link EsperadosFase12}); los de OP-30..OP-32, de {@link CasosFase12#cancelacionConNotas()}.</p>
 *
 * <p><b>Guardas.</b> Solo {@code DEV} (D28) y la guarda de la reemisión de {@link ReemisionPrueba}. Guarda de rango
 * (D52 (4)): si el siguiente número libre no está entre {@value #DESDE} y {@value #HASTA}, falla antes de reservar.
 * <b>Cada condición de la columna {@code condicion} se afirma antes de reservar</b> sobre un gemelo del documento con
 * los datos que le pondría la reemisión (emisor de prueba, timbrado, fecha), y otra vez sobre el documento reservado,
 * antes de firmar: transportista = emisor de prueba ({@link PreparacionVivoFase12}), procesadora = emisor de prueba,
 * receptor de prueba, CDC asociado de la corrida, totales de DE-62, ítem de ID-I06 a 22.000 Gs sobre DE-16,
 * vencimientos de DE-67, largo de DE-68, fecha de inicio del timbrado de OP-20 y OP-22, elección de R1–R3 y de la FE
 * de DE-61, datos de EV-07 tomados de R1, y el plazo de 48 h de la cancelación de OP-30..OP-32. Una condición que no
 * se cumple se anota y el caso no se envía (sin quemar número).</p>
 *
 * <p><b>Ensayo en seco</b> ({@code -Djbs.sifen.test.f12.simular=true}): el mismo camino contra {@link SifenSimulator}
 * (la URL base se afirma en {@code 127.0.0.1} antes de cualquier llamada), sobre una copia temporal de la tabla, con
 * el simulador programado fila por fila con el {@code dCodRes} esperado (0260 aprueba, 1005 aprueba con observación,
 * otro código rechaza con ese código, {@code ?} aprueba). Comprueba además que cada número reservado sea el estimado de
 * la lista y que la tabla real no cambie (md5). Deja {@code simulacion.tsv} con, por caso, el dato que cumple su
 * condición; los pedidos firmados van a {@code simulacion/}, fuera de git.</p>
 *
 * <p><b>Pendientes y estado final (E12-03, D55).</b> Al empezar cada corrida y antes de 12.3.11, cada número de la fase
 * sin estado final (solo {@code RESERVADO} con su pedido guardado, una fila {@code :excepcion}, un lote sin respuesta
 * definitiva o un estado no reconocido) se consulta por CDC ({@code queryDE}; un lote, con {@code queryBatch}) y el
 * resultado se registra en la tabla, siempre con el CDC de la tabla y nunca con el de la respuesta. En ese paso nunca se
 * reenvía: no se usa {@code resolve}, que transmite si SIFEN contesta 0420 (I12-19: {@code resolve} sí se usa <b>en el
 * momento del envío</b>, ante un resultado desconocido, en {@code resolver}). 12.3.11 inutiliza solo un rechazo
 * definitivo o una reserva que no llegó a enviarse, y falla si queda un número sin estado final. OP-21 firmado, con el
 * pedido de un envío guardado y sin registro del envío, también es un pendiente (E12-06).</p>
 *
 * <p><b>Ciclo 3 de la Fase 12.</b> Antes de la consulta de pendientes y de la primera reserva, {@code checkConnection}; si
 * falla, no se reserva nada. Una excepción de transporte en un envío o en un evento detiene la tanda antes de la reserva
 * siguiente (M12-18). En un evento, la fila de la tabla de resultados va antes que las respuestas crudas, y una
 * inutilización con excepción o rechazada no se repite sin aviso: se frena hasta confirmarla o autorizarla una vez con
 * {@code -Djbs.sifen.test.f12.reintentar=I-nnnn} (M12-19). Los reintentos de la lista van en la tanda
 * {@value #TANDA_REINTENTOS} (una fila por caso, antes de 12.3.10) y solo corren nombrados en
 * {@code -Djbs.sifen.test.f12.reintentar}; la sonda DE-56s de I12-23 reemplaza a DE-56x solo si el reintento de DE-56
 * repite el 0160.</p>
 *
 * <p>Uso (desde el módulo, con {@code -Psifen-live}): {@code -Dtest=SifenVivoFase12Test#bateria
 * -Djbs.sifen.test.env=<env> -Djbs.sifen.test.ventas=<test8/ventas> -Djbs.sifen.test.equivalencias=<csv>
 * -Djbs.sifen.test.f12.envios=<envios_12_3.tsv> -Djbs.sifen.test.f12.salida=<fase12/vivo>} y, en vivo,
 * {@code -Djbs.sifen.test.f12.vivo=SIFEN-test} (llave del modo en vivo, M12-14; sin ella no hay corrida en vivo),
 * {@code -Djbs.sifen.test.f12.tandas=12.3.2[,…]} (obligatorio), {@code -Djbs.sifen.test.f12.previos=todos|<casos>}
 * (reanudación), {@code -Djbs.sifen.test.f12.mksif=<constancia>} (D52 (7), obligatorio y no vacío para correr 12.3.9 o
 * 12.3.10; queda escrito en la tabla de resultados), {@code -Djbs.sifen.test.f12.casos=<casos>} (solo restringe),
 * {@code -Djbs.sifen.test.f12.reintentar=<ids>} (autoriza un reintento con otro número, M12-14),
 * {@code -Djbs.sifen.test.f12.op21.inutilizar=true} (cierre: 12.3.11 inutiliza el OP-21 firmado y no enviado, D55) y
 * {@code -Djbs.sifen.test.f12.de41.planb=true}. {@code #preparacion} hace solo {@code checkConnection} y
 * {@code queryRuc} del emisor de prueba (§12.3.1), sin firmar ni enviar nada.</p>
 *
 * <p>Solo en el ensayo: {@code -Djbs.sifen.test.f12.simular.tabla=<copia>} (reanudación sobre una copia),
 * {@code -Djbs.sifen.test.f12.simular.corte=<id>} (corte entre el envío del DE y su registro en la tabla) y
 * {@code -Djbs.sifen.test.f12.simular.lote=<OP-10|OP-11>} (ese lote sigue «en proceso» en cada consulta) y
 * {@code -Djbs.sifen.test.f12.simular.resultados=<id>:<dCodRes|no-enviado>[,…]} (lo que contesta el simulador a ese id en
 * vez de su esperado; {@code no-enviado} es una excepción de transporte antes de enviar: ciclo 3, para ensayar los
 * reintentos, la sonda y M12-18).</p>
 */
@Tag("sifen-live")
public class SifenVivoFase12Test {
    /** Primer número de la Fase 12 en SIFEN-test (D52 (1)). */
    static final int DESDE = 1071;
    /** Último número de la Fase 12 en SIFEN-test (D52 (1)). */
    static final int HASTA = 1249;

    private static final String P = "jbs.sifen.test.f12.";
    private static final String MOTIVO_CANCELACION = "Cancelacion de prueba de la bateria de jbs-sifen (JBSSIF 12.3)";
    private static final String MOTIVO_INUTILIZACION = "Inutilizacion de prueba de la bateria de jbs-sifen (JBSSIF 12.3)";
    private static final String RECEPTOR_DE_PRUEBA = "RECEPTOR DE PRUEBA";
    private static final String CABECERA = "hora\torden\ttanda\tcaso\tnumero estimado\tnumero\toperacion\tcdc o id\tlocal\tstatus"
            + "\tdCodRes\tesperado\tcoincide\tcondicion (dato que la cumple)\tobservaciones";
    /** Tandas que se corren al final, en este orden (12.3.11 antes que 12.3.12). */
    private static final List<String> AL_FINAL = List.of("12.3.11", "12.3.12");
    /** Candidatos a reemplazar R1–R3, en el orden de la lista (I12-11). */
    private static final List<String> REEMPLAZOS_R = List.of("DE-47", "DE-19", "DE-48", "DE-12", "DE-13", "DE-50", "DE-51", "DE-08",
            "DE-52", "DE-53", "DE-54");
    /** FE de 12.3.5 en el orden de la lista (base de DE-61 si DE-44 no aprobó: de atrás hacia adelante). */
    private static final List<String> FE_1235 = List.of("DE-44", "DE-45", "DE-46", "DE-47", "DE-19", "DE-48", "DE-12", "DE-49", "DE-13",
            "DE-50", "DE-51", "DE-08", "DE-52", "DE-53", "DE-54");
    /** Documentos que otro documento de la corrida cita por su CDC del catálogo (el CDC se cambia por el de la corrida). */
    private static final List<String> BASES = List.of("DE-09", "DE-36", "DE-37", "OP-30-FE", "OP-31-FE", "OP-32-FE");
    /** Valor exacto de {@code -Djbs.sifen.test.f12.vivo} que habilita la corrida en vivo (M12-14). */
    static final String LLAVE_VIVO = "SIFEN-test";
    /** Tandas que no se corren en vivo sin la constancia de que MKSIF no emite (D52 (7), I12-16). */
    static final Set<String> TANDAS_MKSIF = Set.of("12.3.9", "12.3.10");
    /** Tope de reintentos de la fase, en números (lista de §12.3: 20). */
    static final int TOPE_REINTENTOS = 20;
    /** Propiedades que solo valen en el ensayo en seco. */
    private static final List<String> SOLO_ENSAYO = List.of("simular.tabla", "simular.corte", "simular.lote", "simular.resultados");
    /**
     * Tanda de los reintentos decididos (ciclo 3 de la Fase 12): una fila por caso, antes de 12.3.10; cada una corre solo
     * con el caso nombrado en {@code -Djbs.sifen.test.f12.reintentar}, una vez y con otro número.
     */
    static final String TANDA_REINTENTOS = "reintentos";
    /** Resultado simulado de una excepción de transporte antes de enviar (solo en el ensayo, M12-18). */
    static final String NO_ENVIADO = "no-enviado";
    /** La sonda de I12-23 y el caso al que reemplaza. */
    static final String SONDA_56 = "DE-56s";
    /** Fuentes cuya huella (md5) se escribe en cada corrida (rutas desde el módulo). */
    static final List<String> FUENTES = List.of(
            "src/test/java/org/javabeanstack/sifen/test/vivo/SifenVivoFase12Test.java",
            "src/test/java/org/javabeanstack/sifen/test/vivo/ReemisionPrueba.java",
            "src/test/java/org/javabeanstack/sifen/test/SifenTestEnv.java");
    /** Un CDC (44 dígitos) en una línea de salida: se enmascara (en la salida no van CDC completos). */
    private static final Pattern CDC_EN_TEXTO = Pattern.compile("(?<!\\d)(\\d{2})\\d{38}(\\d{4})(?!\\d)");
    /** Un número de la tabla: establecimiento-punto-número. */
    private static final Pattern NUMERO_TABLA = Pattern.compile("\\d{3}-\\d{3}-\\d{7}");
    /** 48 h: plazo de consulta de un lote; pasado, cada CDC se consulta solo (ISifenClient#queryBatch). */
    private static final Duration PLAZO_LOTE = Duration.ofHours(48);
    /** Primera consulta de un lote y espera entre consultas (Guía de mejores prácticas: 10 min y 10 min). */
    private static final long ESPERA_LOTE_MS = 600_000L;

    /** Estado de un número de la fase según la tabla (E12-03). */
    enum Estado {
        /** aprobado o aprobado con observación. */
        APROBADO,
        /** inutilizado (evento 0600). */
        INUTILIZADO,
        /** rechazo definitivo de SIFEN, o 0420 en la consulta (no está en SIFEN). */
        RECHAZADO,
        /** reservado y nunca enviado (sin pedido guardado, o con un rechazo local o una condición no cumplida). */
        NO_ENVIADO,
        /** OP-21: firmado y guardado, sin enviar (12.3.12, D56). */
        FIRMADO_SIN_ENVIAR,
        /** sin estado final: primero consultar (D55). */
        PENDIENTE
    }

    /**
     * Estado de un número de la fase, con lo necesario para resolverlo o inutilizarlo.
     *
     * @param codigo          el {@code dCodRes} de la fila que decidió el estado ({@code ""} si ninguna).
     * @param inutilizacion   la última inutilización que no quedó en 0600: {@code excepcion} (resultado desconocido) o
     *                        {@code rechazada <código>}; {@code null} si no hubo (M12-19).
     * @param intentosInutilizacion cuántas inutilizaciones sin 0600 tiene el número.
     */
    record EstadoNumero(int numero, String completo, String id, String cdc, String lote, Estado estado, String motivo,
            LocalDateTime fecha, String codigo, String inutilizacion, int intentosInutilizacion) {
        boolean definitivo() {
            return estado != Estado.PENDIENTE && estado != Estado.FIRMADO_SIN_ENVIAR;
        }

        boolean deLote() {
            return id.matches("OP-1[01]-\\d+");
        }
    }

    /** Corte simulado del proceso (solo en el ensayo): un {@link Error} que ningún {@code catch} del test atrapa. */
    static final class CorteSimulado extends Error {
        private static final long serialVersionUID = 1L;

        CorteSimulado(String mensaje) {
            super(mensaje);
        }
    }

    /** Una fila de {@code envios_12_3.tsv}. */
    record Fila(int orden, String tanda, String caso, String vivo, String numero, String sifen, String local, String condicion,
            String nota) {
        boolean negativo() {
            return "N".equals(vivo);
        }
    }

    private static TgEmis emisorReal;

    private SifenTestEnv.Emisor emisor;
    private String email;
    private Path dir;
    private Path res;
    private Path tabla;
    private boolean simular;
    private long pausa;
    private boolean primero = true;
    private SifenSimulator sim;
    private ReemisionPrueba reemision;
    private ISifenClient client;
    private ISifenClient crudo;
    private ISifenClient lento;
    private ISifenClient crudoLento;
    private String constanciaMksif;
    private boolean planB41;
    private int rechazosSeguidos;
    private boolean detenido;
    private final List<String> fallas = new ArrayList<>();
    private final List<String> desvios = new ArrayList<>();
    /** Números estimados de la lista, por caso (OP-10 y OP-11: el rango; OP-30..OP-32: FE y NC). */
    private final Map<String, List<Integer>> estimados = new LinkedHashMap<>();
    private final Map<String, Integer> usados = new LinkedHashMap<>();
    private final Map<String, SignedDocument> firmados = new LinkedHashMap<>();
    private final Map<String, SignedDocument> aprobados = new LinkedHashMap<>();
    private final Map<String, LocalDateTime> aprobadoEn = new LinkedHashMap<>();
    private final Map<String, String> numeros = new LinkedHashMap<>();
    /** CDC del catálogo → CDC de la corrida, solo de {@link #BASES} aprobadas. */
    private final Map<String, String> cdcDeLaCorrida = new LinkedHashMap<>();
    private final Map<String, String> eventos = new LinkedHashMap<>();
    private final Map<String, String> r = new LinkedHashMap<>();
    private String base61;
    private boolean receptorSuspendido;
    private LocalDateTime recepcion;
    private boolean lote10Definitivo;
    private Map<String, EsperadosFase12.Esperado> esperados;
    /** Ids nombrados en {@code -Djbs.sifen.test.f12.reintentar}: los únicos que pueden volver a numerarse o enviarse (M12-14). */
    private Set<String> reintentar = Set.of();
    /** Ids ya reintentados en esta corrida (un reintento por id). */
    private final Set<String> reintentados = new HashSet<>();
    /** Filas de la lista, por caso (el esperado de cada caso para el ensayo de la consulta de pendientes). */
    private final Map<String, Fila> porCaso = new LinkedHashMap<>();
    /** Tandas en las que ya se escribió la constancia de MKSIF en esta corrida. */
    private final Set<String> constanciaEscrita = new HashSet<>();
    private String corteSimulado;
    private String loteEnProceso;
    /** En el ensayo, pedidos que recibió el simulador por servicio (fin del ensayo). */
    private Map<String, Integer> pedidosAlSimulador = Map.of();
    /** Veces que se envió cada evento en corridas anteriores ({@code resultados.tsv}). */
    private final Map<String, Integer> enviosDeEvento = new LinkedHashMap<>();
    /** Estado (columna status) del último envío de cada evento en corridas anteriores. */
    private final Map<String, String> statusDeEvento = new LinkedHashMap<>();
    /** Ids con un número de la fase en la tabla antes de esta corrida (reanudación: no se vuelven a reservar). */
    private final Set<String> reservadosAntes = new HashSet<>();
    /** Lotes ya enviados en una corrida anterior: OP-10/OP-11 → número de lote (no se reenvían, solo se consultan). */
    private final Map<String, String> lotesAntes = new LinkedHashMap<>();
    /** Eventos ya enviados en una corrida anterior ({@code resultados.tsv}): id → dCodRes (no se repiten). */
    private final Map<String, String> eventosAntes = new LinkedHashMap<>();
    /** Casos con una fila en la tanda {@link #TANDA_REINTENTOS}: su reintento va solo ahí (ciclo 3). */
    private final Set<String> conFilaDeReintento = new HashSet<>();
    /** Ids que reservaron un número en esta corrida (un reintento en la misma corrida se coteja contra él). */
    private final Set<String> reservadosEnLaCorrida = new HashSet<>();
    /** Números de la fase en la tabla antes de esta corrida, por clave de estimado (el índice del estimado sigue de ahí). */
    private final Map<String, Integer> antesPorClave = new LinkedHashMap<>();
    /** Solo en el ensayo: lo que contesta el simulador por id ({@code -Djbs.sifen.test.f12.simular.resultados}). */
    private final Map<String, String> simulados = new LinkedHashMap<>();
    /** Por qué se detuvo la tanda (la primera causa). */
    private String motivoDetencion;

    // ------------------------------------------------------------------ §12.3.1: conexión y RUC

    /**
     * §12.3.1 sin firmar ni enviar ningún DE ni evento: {@code checkConnection} y {@code queryRuc} del emisor de prueba,
     * con la guarda de ambiente. Respuestas crudas a {@code respuestas/} (fuera de git); el resumen a {@code resultados.tsv}.
     */
    @Test
    public void preparacion() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean(P + "preparacion"), "sin -D" + P + "preparacion=true");
        Assertions.assertFalse(Boolean.getBoolean(P + "simular"), "la preparación no tiene ensayo: no envía nada");
        SifenTestEnv.Entorno entorno = SifenTestEnv.entornoOSaltar();
        emisor = SifenTestEnv.emisorOSaltar();
        salida();
        SifenConfig cfg = entorno.getConfiguracion();
        Assertions.assertEquals(SifenConfig.Environment.DEV, cfg.getEnvironment(), "D28");
        Assertions.assertTrue(cfg.getBaseUrl() == null || !cfg.getBaseUrl().contains("127.0.0.1"), "en vivo, la URL de SIFEN-test");
        Fila f = new Fila(1, "12.3.1", "(preparación)", "—", "—", "—", "—", "—", "");
        try (ISifenClient c = SifenClient.create(cfg)) {
            Assertions.assertTrue(c.validateConfig().isValid(), "la configuración del entorno no pasa la validación");
            fila(f, "—", "getVersion", "", "", c.getVersion(), "", "", huellaDelCodigo(), huellaDeLasFuentes());
            ConnectionDiagnosis d = c.checkConnection();
            fila(f, "—", "checkConnection", d.getOperationId(), "", d.isOk() ? "ok" : "FALLA", "", "", "", limpio(d.toString()));
            if (!d.isOk()) {
                fallas.add("checkConnection " + d);
            }
            try {
                RucQueryResult q = c.queryRuc(emisor.getRuc());
                guardar("ruc-emisor-de-prueba.xml", q.getRawResponse());
                fila(f, "—", "queryRuc (emisor de prueba)", "SIFEN_EMISOR_RUC", "", q.isFound() ? "encontrado" : "no encontrado",
                        q.getResponseCode(), "0502", "facturador electrónico: " + q.isElectronicInvoicer(), limpio(q.getMessage()));
                if (!q.isFound()) {
                    fallas.add("queryRuc " + q.getResponseCode());
                }
            } catch (SifenException x) {
                // SIFEN-test contesta 0160 en forma intermitente a la consulta de RUC (§8.7, I8-03): con checkConnection ok
                // es un aviso y no decide (I12-20; el control de la regla de operación es checkConnection)
                String aviso = avisoDeQueryRuc(d.isOk(), x.getCode());
                fila(f, "—", "queryRuc (emisor de prueba)", "SIFEN_EMISOR_RUC", "", aviso != null ? "aviso" : "excepción", x.getCode(), "0502",
                        aviso != null ? aviso : "", limpio(x.getTechnicalDetail()));
                if (aviso == null) {
                    fallas.add("queryRuc " + x.getCode());
                }
            }
        }
        Assertions.assertTrue(fallas.isEmpty(), () -> enmascarar("preparación: " + fallas));
    }

    /**
     * I12-20: el {@code queryRuc} de la preparación con el 0160 intermitente de SIFEN-test (I8-03: {@code JBS-RESP-02},
     * {@code rRetEnviDe} en vez de {@code rResEnviConsRuc}) no hace fallar la preparación si {@code checkConnection} dio ok.
     *
     * @return el texto del aviso, o {@code null} si la excepción decide (falla).
     */
    static String avisoDeQueryRuc(boolean conexionOk, String codigo) {
        if (conexionOk && "JBS-RESP-02".equals(codigo)) {
            return "aviso: 0160 intermitente de SIFEN-test en la consulta de RUC (I8-03); checkConnection ok: no decide (I12-20)";
        }
        return null;
    }

    // ------------------------------------------------------------------ la batería

    @Test
    public void bateria() throws Exception {
        String lista = System.getProperty(P + "envios");
        Assumptions.assumeTrue(lista != null, "falta -D" + P + "envios");
        // M12-14 e I12-16: el modo (ensayo o vivo, con su llave), la constancia de MKSIF y los reintentos, antes de nada
        verificarModo(System.getProperties());
        SifenTestEnv.Entorno entorno = SifenTestEnv.entornoOSaltar();
        emisor = SifenTestEnv.emisorOSaltar();
        simular = Boolean.getBoolean(P + "simular");
        pausa = Long.getLong(P + "pausa", 3000L);
        planB41 = Boolean.getBoolean(P + "de41.planb");
        constanciaMksif = System.getProperty(P + "mksif") == null ? null : System.getProperty(P + "mksif").strip();
        corteSimulado = simular ? System.getProperty(P + "simular.corte") : null;
        loteEnProceso = simular ? System.getProperty(P + "simular.lote") : null;
        salida();
        Set<String> tandas = conjunto(System.getProperty(P + "tandas"));
        Set<String> casos = conjunto(System.getProperty(P + "casos"));
        reintentar = conjunto(System.getProperty(P + "reintentar"));

        List<Fila> filas = leerLista(Paths.get(lista));
        for (Fila f : filas) {
            porCaso.putIfAbsent(f.caso(), f);
            if (TANDA_REINTENTOS.equals(f.tanda())) {
                conFilaDeReintento.add(f.caso());
            }
        }
        if (simular) {
            simulados.putAll(simulados(System.getProperty(P + "simular.resultados")));
        }
        esperados = new LinkedHashMap<>();
        for (EsperadosFase12.Esperado e : EsperadosFase12.leer()) {
            esperados.put(e.caso(), e);
        }
        verificarLista(filas);

        // D28 y validación activa
        SifenConfig cfg = configuracion(true, false);
        Assertions.assertEquals(SifenConfig.Environment.DEV, cfg.getEnvironment(), "D28");
        Assertions.assertTrue(new SifenConfig().isPreValidationEnabled(), "el valor por omisión es con validación");
        Assertions.assertTrue(cfg.isPreValidationEnabled(), "la configuración del entorno no apaga la validación");
        if (!simular) {
            // M12-14: en vivo la URL sale solo del ambiente (DEV) y la salida es la de la evidencia
            Assertions.assertNull(cfg.getBaseUrl(), "en vivo no se fija una URL base: la da el ambiente DEV");
            Assertions.assertTrue(dir.toAbsolutePath().normalize().endsWith(Paths.get("fase12", "vivo")),
                    "en vivo la salida es la carpeta fase12/vivo de la evidencia");
        }

        Path real = Paths.get(System.getProperty(SifenVivoTest.PROP_EQUIVALENCIAS));
        String md5Real = md5(real);
        tabla = real;
        if (simular) {
            // ensayo de la reanudación: dos corridas sobre la misma copia (-D...simular.tabla), nunca sobre la tabla real
            String copia = System.getProperty(P + "simular.tabla");
            if (copia != null) {
                tabla = Paths.get(copia);
                Assertions.assertNotEquals(real.toRealPath(), Files.exists(tabla) ? tabla.toRealPath() : tabla.toAbsolutePath(),
                        "el ensayo nunca usa la tabla real");
                if (!Files.exists(tabla)) {
                    Files.copy(real, tabla);
                }
            } else {
                tabla = Files.createTempFile("equivalencias-simulacion-f12", ".csv");
                Files.copy(real, tabla, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        reemision = new ReemisionPrueba(emisor, entorno.getNumeroInicial(), tabla);
        email = SifenTestEnv.emailParaReemitirOSaltar(emisor);
        guardaDeRango("al empezar");
        cargarEmisorReal();
        Fila inicio = new Fila(0, "—", "(inicio)", "—", "—", "—", "—", "—", "");
        try {
            client = SifenClient.create(cfg);
            Assertions.assertTrue(client.getConfig().isPreValidationEnabled(), "el cliente valida antes de enviar");
            fila(inicio, "—", "getVersion", "", "", client.getVersion(), "", "", huellaDelCodigo(),
                    "siguiente libre " + reemision.getSiguiente() + (simular ? " · ENSAYO EN SECO contra el simulador " + cfg.getBaseUrl()
                    + (System.getProperty(P + "simular.tabla") != null ? " (tabla: la copia de -D" + P + "simular.tabla)" : " (tabla: copia temporal)")
                    + (simulados.isEmpty() ? "" : " · resultados simulados " + simulados) : " · EN VIVO (llave -D" + P + "vivo)")
                    + " · tandas " + (tandas.isEmpty() ? "todas" : tandas)
                    + (casos.isEmpty() ? "" : " · casos " + casos) + (reintentar.isEmpty() ? "" : " · reintentar " + reintentar));
            fila(inicio, "—", "huella de las fuentes", "", "", "", "", "", huellaDeLasFuentes(), "");
            // M12-18: la conexión, antes de la consulta de pendientes y de la primera reserva; si falla no se reserva nada
            ConnectionDiagnosis d = client.checkConnection();
            fila(inicio, "—", "checkConnection (antes de la primera reserva)", d.getOperationId(), "", d.isOk() ? "ok" : "FALLA", "", "", "",
                    limpio(d.toString()));
            if (!d.isOk()) {
                detener("checkConnection falló antes de la primera reserva: no se reserva ningún número (M12-18)");
            }
            cargarPrevios();
            cargarLoHechoAntes();
            // E12-03: lo que quedó sin estado final en una corrida anterior se consulta antes de seguir
            if (!detenido) {
                resolverPendientes("al empezar");
            }
            List<Fila> orden = new ArrayList<>(filas);
            orden.sort((a, b) -> {
                int x = AL_FINAL.indexOf(a.tanda());
                int y = AL_FINAL.indexOf(b.tanda());
                return x != y ? Integer.compare(x, y) : Integer.compare(a.orden(), b.orden());
            });
            for (Fila f : orden) {
                if (detenido) {
                    break;
                }
                if (!tandas.isEmpty() && !tandas.contains(f.tanda()) || !casos.isEmpty() && !casos.contains(f.caso())) {
                    continue;
                }
                if ("espera".equals(f.tanda())) {
                    fila(f, "—", "sin envío", "", "", "", "", f.sifen(), "", "tanda «espera»: " + f.condicion());
                    continue;
                }
                if (!constanciaDeMksif(f)) {
                    continue;
                }
                correr(f);
            }
        } finally {
            for (ISifenClient c : Arrays.asList(client, crudo, lento, crudoLento)) {
                if (c != null) {
                    c.close();
                }
            }
            if (sim != null) {
                pedidosAlSimulador = new LinkedHashMap<>();
                for (SimulatedService s : SimulatedService.values()) {
                    pedidosAlSimulador.put(s.name(), sim.requests(s).size());
                }
                sim.close();
            }
        }
        if (simular) {
            Assertions.assertEquals(md5Real, md5(real), "el ensayo en seco no toca la tabla real");
            int numerosReservados = usados.values().stream().mapToInt(Integer::intValue).sum();
            fila(inicio, "—", "fin del ensayo", "", "", "", "", "", "tabla real sin cambios (md5 " + md5Real.substring(0, 8) + "…)",
                    "números reservados en esta corrida: " + numerosReservados + " (en " + usados.size() + " casos) · distintos del estimado: "
                    + desvios + " · pedidos al simulador: " + pedidosAlSimulador);
            Assertions.assertTrue(desvios.isEmpty(), () -> "números reservados distintos del estimado: " + desvios);
        }
        // I12-19: los mensajes de falla también van sin CDC completos (la salida de surefire)
        Assertions.assertTrue(fallas.isEmpty(), () -> enmascarar("casos que no dieron lo esperado o no cumplieron su condición: " + fallas));
    }

    /**
     * Lee {@code -Djbs.sifen.test.f12.simular.resultados} ({@code id:código} separados por coma; solo en el ensayo).
     *
     * @return id → código ({@value #NO_ENVIADO} para una excepción de transporte antes de enviar).
     */
    static Map<String, String> simulados(String valor) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String par : conjuntoOrdenado(valor)) {
            int i = par.lastIndexOf(':');
            Assertions.assertTrue(i > 0 && i < par.length() - 1, "simular.resultados: «" + par + "» no es id:código");
            m.put(par.substring(0, i).strip(), par.substring(i + 1).strip());
        }
        return m;
    }

    /** Detiene la tanda antes de la reserva siguiente; la primera causa queda en {@link #motivoDetencion}. */
    private void detener(String motivo) {
        if (motivoDetencion == null) {
            motivoDetencion = motivo;
        }
        detenido = true;
        fallas.add(motivo);
    }

    private void correr(Fila f) throws Exception {
        switch (f.caso()) {
            case "OP-21" -> {
                if ("12.3.1".equals(f.tanda())) {
                    op21Firmar(f);
                } else {
                    op21Enviar(f);
                }
            }
            case "EV-07", "EV-03", "EV-09", "EV-04", "EV-04c", "EV-08", "EV-08c" -> eventoDelReceptor(f);
            case "OP-06" -> op06(f);
            case "OP-03" -> op03(f);
            case "OP-04", "OP-01", "OP-02" -> cancelarAnterior(f);
            case "OP-10", "OP-11" -> lote(f);
            case "OP-30", "OP-31", "OP-32" -> cancelacionConNota(f);
            case "(inutilizaciones)" -> inutilizaciones(f);
            case "DE-41" -> {
                if (TANDA_REINTENTOS.equals(f.tanda())) {
                    reintento(f);
                    return;
                }
                if (planB41) {
                    // plan B (lista, fila de DE-41): FE innominada nueva y su nominación, un número más
                    SignedDocument inn = documento(f, "DE-41-FE", () -> esperados.get("DE-16").documento(), "0260");
                    if (inn != null) {
                        EventosDE nom = nominacion(inn);
                        EventResult e = evento(f, "DE-41-nom", inn, "nominación de DE-41-FE con el receptor de EV-05 (plan B)", "0600",
                                () -> client.sendEvents(nom), numeros.get("DE-41-FE"), inn.getCdc());
                        if (e != null) {
                            eventos.put("DE-41-nom", codigo(e));
                        }
                    }
                }
                documento(f, f.caso(), () -> esperados.get(f.caso()).documento(), f.sifen());
            }
            default -> {
                if (TANDA_REINTENTOS.equals(f.tanda())) {
                    reintento(f);
                } else {
                    documento(f, f.caso(), () -> esperados.get(f.caso()).documento(), f.sifen());
                }
            }
        }
    }

    /**
     * Fila de la tanda {@link #TANDA_REINTENTOS} (ciclo 3): el caso se vuelve a armar desde su catálogo, con los mismos
     * datos y otro número. Corre solo si está nombrado en {@code -Djbs.sifen.test.f12.reintentar} y tiene un número
     * anterior; lo demás (una vez, anterior con estado final y no aprobado, tope de 20) lo decide {@link #yaReservado}.
     */
    private void reintento(Fila f) throws Exception {
        String id = f.caso();
        String no = null;
        if (!reintentar.contains(id)) {
            no = "reintento preparado en la lista (ciclo 3): solo corre nombrado en -D" + P + "reintentar=" + id;
        } else if (!reservadosAntes.contains(id) && !reservadosEnLaCorrida.contains(id)) {
            no = "no hay un número anterior de " + id + " que reintentar";
        }
        if (no != null) {
            fila(f, "—", "sin envío", "", "", "", "", f.sifen(), "", no);
            return;
        }
        documento(f, id, () -> esperados.get(id).documento(), f.sifen());
    }

    // ------------------------------------------------------------------ documentos

    /**
     * Arma, prepara, afirma la condición (en el gemelo y en el reservado), reserva, firma y envía un DE.
     *
     * @return el documento aprobado, o {@code null}.
     */
    private SignedDocument documento(Fila fila, String id, Supplier<DocumentoElectronico> catalogo, String esperado) throws Exception {
        Fila f = sub(fila, id);
        LocalDateTime ahora = LocalDateTime.now().minusMinutes(1).truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime fecha = fechaDeEmision(id, ahora);
        // 1) lo que cada caso necesita de la corrida, antes de armar nada
        if (yaReservado(f, id)) {
            fila(f, "—", "sin reservar", aprobados.containsKey(id) ? aprobados.get(id).getCdc() : "", "", "", "", esperado, "",
                    "ya tiene número de una corrida anterior (" + (aprobados.containsKey(id) ? "aprobado, tomado de previos" : "sin aprobar: "
                    + "va a 12.3.11, o se reintenta una vez con otro número nombrándolo en -D" + P + "reintentar") + ")");
            return aprobados.get(id);
        }
        String motivo = motivoParaNoEnviar(f, id);
        if (motivo != null) {
            fila(f, "—", "sin envío", "", "", "", "", esperado, "", motivo);
            return null;
        }
        if (f.negativo() && !simular && constanciaMksif == null) {
            // defensa: verificarModo ya exige la constancia para 12.3.9 y 12.3.10 (I12-16)
            fila(f, "—", "sin envío", "", "", "", "", esperado, "", "falta -D" + P + "mksif (D52 (7): no correr mientras MKSIF emite)");
            fallas.add(id + " sin la constancia de MKSIF");
            return null;
        }
        Supplier<DocumentoElectronico> armado = () -> armar(id, catalogo.get());
        // 2) gemelo con lo que le pondría la reemisión: validación local y condición, antes de reservar
        DocumentoElectronico gemelo = armado.get();
        comoReemitido(gemelo, fecha, reemision.getSiguiente());
        ajustesTrasReemitir(id, gemelo, ahora);
        String dato;
        try {
            dato = condicion(id, gemelo, true);
        } catch (AssertionFailedError | IllegalStateException x) {
            fila(f, "—", "sin envío", "", "", "", "", esperado, "NO CUMPLE (antes de reservar): " + limpio(x.getMessage()), "");
            fallas.add(id + " condición: " + x.getMessage());
            return null;
        }
        ValidationResult v = client.validateDE(gemelo);
        EsperadosFase12.Esperado e = esperados.get(id);
        String local = local(v);
        Set<String> errEsp = e != null ? e.errores() : Set.of();
        Set<String> avEsp = e != null ? e.avisos() : Set.of();
        if (!(codigos(v.getErrors()).equals(errEsp) && codigos(v.getWarnings()).equals(avEsp))) {
            fila(f, "—", "validateDE (gemelo)", "", local, "", "", esperado, dato, "la validación local no da lo esperado (errores "
                    + errEsp + ", avisos " + avEsp + "): no se reserva");
            fallas.add(id + " validación local " + local);
            return null;
        }
        if (f.negativo()) {
            fila(f, "—", "validateDE (validación activa, lo que vería la aplicación)", "", local, "", "", esperado, dato,
                    "se envía por el cliente aparte sin validación previa (Q39, D55)");
        }
        // 3) reservar (guarda de rango) y volver a afirmar sobre el reservado
        DocumentoElectronico de = armado.get();
        ReemisionPrueba.Reemision re = reservar(id, de, fecha);
        ajustesTrasReemitir(id, de, ahora);
        try {
            condicion(id, de, false);
        } catch (AssertionFailedError | IllegalStateException x) {
            fila(f, re.enviado, "sin envío", re.cdc, "", "", "", esperado, "NO CUMPLE (reservado, antes de firmar): " + limpio(x.getMessage()),
                    "número reservado y no enviado: va a 12.3.11");
            reemision.registrar(id, re, "ISifenClient.sign:condicion-no-cumplida", 0, null, null, limpio(x.getMessage()));
            fallas.add(id + " condición tras reservar: " + x.getMessage());
            return null;
        }
        ISifenClient c = cliente(f.negativo(), lento(id));
        SignedDocument doc;
        try {
            doc = c.sign(de);
        } catch (SifenValidationException x) {
            fila(f, re.enviado, "sign", re.cdc, "rechaza local " + codigos(x.getErrors()), "", x.getCode(), esperado, dato,
                    "número reservado y no enviado: va a 12.3.11");
            reemision.registrar(id, re, "ISifenClient.sign:rechazo-local", 0, x.getCode(), null, codigos(x.getErrors()).toString());
            fallas.add(id + " rechazo local " + codigos(x.getErrors()));
            return null;
        }
        // I12-19: sin assertEquals de CDC (el mensaje de la falla los volcaría enteros en la salida de surefire)
        Assertions.assertTrue(re.cdc.equals(doc.getCdc()), () -> id + ": el CDC firmado no es el reservado (" + enmascarar(doc.getCdc()) + ")");
        firmados.put(id, doc);
        numeros.put(id, re.enviado);
        // DE-62: los cuatro totales otra vez, en el XML firmado
        if ("DE-62".equals(id)) {
            dato += " · en el firmado: " + totalesDe62(doc.getXml());
        }
        return enviar(f, id, doc, re, local, esperado, dato);
    }

    /** Envía un DE firmado y registra el resultado; si aprueba, queda en {@link #aprobados}. */
    private SignedDocument enviar(Fila f, String id, SignedDocument doc, ReemisionPrueba.Reemision re, String local, String esperado,
            String dato) throws Exception {
        String simulado = simulado(id, esperado);
        programar(SimulatedService.RECEIVE_DE, simulado, false);
        pausar();
        guardarPedido(id, doc);
        ISifenClient c = cliente(f.negativo(), lento(id));
        SendResult s;
        try {
            if (NO_ENVIADO.equals(simulado)) {
                throw SifenException.of(ErrorCode.JBS_CONEX_01, "ensayo: excepción de transporte antes de enviar (M12-18)", null);
            }
            s = c.send(doc);
        } catch (SifenException x) {
            fila(f, re.enviado, "send", doc.getCdc(), local, "excepción", x.getCode(), esperado, dato,
                    "resultado desconocido: " + x.isOutcomeUnknown());
            reemision.registrar(id, re, "ISifenClient.send:excepcion", 0, x.getCode(), null, x.getMessage());
            if (x.isOutcomeUnknown()) {
                return resolver(f, id, doc, re, local, esperado, dato);
            }
            // M12-18: sin enviar por una falla de transporte, el caso siguiente fallaría igual y quemaría otro número
            detener(id + " " + x.getCode() + ": excepción de transporte sin enviar; la tanda se detiene antes de la reserva siguiente (M12-18)");
            return null;
        }
        return resultado(f, id, doc, re, s, local, esperado, dato, "send");
    }

    /** Un resultado desconocido se resuelve con {@code resolve}, nunca con otro número (§12.3). */
    private SignedDocument resolver(Fila f, String id, SignedDocument doc, ReemisionPrueba.Reemision re, String local, String esperado,
            String dato) throws Exception {
        pausar();
        try {
            SendResult s = cliente(f.negativo(), lento(id)).resolve(doc);
            return resultado(f, id, doc, re, s, local, esperado, dato, "resolve");
        } catch (SifenException x) {
            fila(f, re.enviado, "resolve", doc.getCdc(), local, "excepción", x.getCode(), esperado, dato,
                    "sigue desconocido: no se inutiliza (D55); resolver antes de seguir");
            reemision.registrar(id, re, "ISifenClient.resolve:excepcion", 0, x.getCode(), null, x.getMessage());
            fallas.add(id + " resolve " + x.getCode());
            detenido = true;
            return null;
        }
    }

    private SignedDocument resultado(Fila f, String id, SignedDocument doc, ReemisionPrueba.Reemision re, SendResult s, String local,
            String esperado, String dato, String op) throws Exception {
        if (simular && id.equals(corteSimulado)) {
            // ensayo del corte entre el envío y el registro (M12-15): el proceso «muere» con el pedido guardado y la tabla en RESERVADO
            throw new CorteSimulado("corte simulado de " + id + " entre el envío y el registro en la tabla");
        }
        // E12-03: primero la tabla (lo que decide el estado) y después la respuesta cruda; un corte entre los dos deja
        // el número con su resultado y, si se corta antes, solo RESERVADO con el pedido guardado: lo resuelve la consulta
        reemision.registrar(id, re, "ISifenClient." + op, 200, s.getResponseCode(), String.valueOf(s.getStatus()), s.getMessage());
        guardar(doc.getCdc() + ".xml", s.getRawResponse());
        boolean aprobado = s.getStatus() == ResultStatus.APPROVED || s.getStatus() == ResultStatus.APPROVED_WITH_OBSERVATION;
        String obs = "dDigVal=local " + digest(s, doc);
        if (aprobado) {
            aprobados.put(id, doc);
            aprobadoEn.put(id, simular || s.getProcessingDate() == null ? LocalDateTime.now() : s.getProcessingDate());
            if (BASES.contains(id)) {
                cdcDeLaCorrida.put(cdcDelCatalogo(id), doc.getCdc());
            }
            if ("DE-56".equals(id) || "DE-55".equals(id) || SONDA_56.equals(id)) {
                obs += " · " + consultar(id, doc);
            }
        }
        coincide(f, id, s.getResponseCode(), esperado);
        fila(f, re.enviado, op, doc.getCdc(), local, String.valueOf(s.getStatus()), s.getResponseCode(), esperado, dato,
                obs + " · " + limpio(s.getMessage()));
        return aprobado ? doc : null;
    }

    /** queryDE tras un aprobado (DE-55/DE-56: el mismo C14N, CA12-08). */
    private String consultar(String id, SignedDocument doc) throws Exception {
        if (!simular) {
            Thread.sleep(2000L);
        }
        DEQueryResult q = client.queryDE(doc.getCdc());
        guardar("consulta-" + doc.getCdc() + ".xml", q.getRawResponse());
        if (simular) {
            return "queryDE " + q.getResponseCode() + " (simulador: rDE ficticio, sin comparar el C14N)";
        }
        if (q.getXml() == null || q.getXml().isBlank()) {
            return "queryDE " + q.getResponseCode() + " sin rDE";
        }
        boolean igual = Arrays.equals(XmlCanonico.canonico(doc.getXml()), XmlCanonico.canonico(q.getXml()));
        if (!igual) {
            fallas.add(id + " C14N distinto en la consulta");
        }
        return "queryDE " + q.getResponseCode() + " C14N=local " + (igual ? "sí" : "NO");
    }

    /** Si el caso depende de otro de la corrida que no quedó aprobado (o de la hora), el motivo para no enviarlo. */
    private String motivoParaNoEnviar(Fila f, String id) {
        switch (id) {
            case "DE-39", "DE-39b" -> {
                return aprobados.containsKey("DE-09") ? null : "DE-09 no quedó aprobada";
            }
            case "DE-39c" -> {
                return aprobados.containsKey("DE-39") && aprobados.containsKey("DE-39b") ? null
                        : "DE-39 y DE-39b no quedaron aprobadas (con una sola, SIFEN la aprobaría)";
            }
            case "DE-37", "DE-37n" -> {
                return aprobados.containsKey("DE-36") ? null : "DE-36 no quedó aprobada";
            }
            case "DE-38" -> {
                return aprobados.containsKey("DE-37") ? null : "DE-37 no quedó aprobada";
            }
            case "ID-I06" -> {
                return aprobados.containsKey("DE-16") ? null : "DE-16 no quedó aprobada (I12-09)";
            }
            case "DE-61" -> {
                base61 = elegirBase61();
                return base61 != null ? null : "ninguna FE de 12.3.5 sirve de base (I12-11)";
            }
            case "DE-41" -> {
                if (planB41) {
                    return aprobados.containsKey("DE-41-FE") && "0600".equals(eventos.get("DE-41-nom")) ? null
                            : "plan B: la FE innominada nueva o su nominación no quedaron aprobadas";
                }
                return cdcDeLaTabla("001-001-0001070") != null ? null : "001-001-0001070 no está aprobada en la tabla: plan B";
            }
            case "DE-56x", SONDA_56 -> {
                return motivoSegunReintentoDe56(id);
            }
            default -> {
                if (id.endsWith("-NC")) {
                    String fe = id.replace("-NC", "-FE");
                    return aprobados.containsKey(fe) ? null : fe + " no quedó aprobada";
                }
                return null;
            }
        }
    }

    /**
     * I12-23 (decisión del usuario, 2026-11-25): DE-56 se reintenta una vez; si aprueba, DE-56x corre como está; si repite
     * el 0160, DE-56x no se envía y va la sonda {@value #SONDA_56} en su lugar (el mismo número estimado). Sin el reintento
     * hecho, ninguno de los dos; con otro resultado, tampoco (se anota: decide el usuario).
     *
     * @return el motivo para no enviar {@code id}, o {@code null}.
     */
    private String motivoSegunReintentoDe56(String id) {
        boolean sonda = SONDA_56.equals(id);
        EstadoNumero re = reintentoDe56();
        if (re == null) {
            return "I12-23: espera el reintento de DE-56 (tanda «" + TANDA_REINTENTOS + "»), que decide entre DE-56x y la sonda";
        }
        if (re.estado() == Estado.APROBADO) {
            return sonda ? "I12-23: DE-56 aprobó en su reintento (" + re.completo() + "): corre DE-56x y la sonda no se envía" : null;
        }
        if (re.estado() == Estado.RECHAZADO && "0160".equals(re.codigo())) {
            return sonda ? null : "I12-23: DE-56 repitió el 0160 en su reintento (" + re.completo() + "): DE-56x no se envía, va la sonda " + SONDA_56;
        }
        if (sonda) {
            fallas.add("I12-23: el reintento de DE-56 (" + re.completo() + ") quedó «" + re.motivo() + "»: ni DE-56x ni la sonda (decide el usuario)");
        }
        return "I12-23: el reintento de DE-56 (" + re.completo() + ") quedó «" + re.motivo() + "», ni aprobado ni 0160: ni DE-56x ni la sonda"
                + " (decide el usuario)";
    }

    /** El segundo número de la fase de DE-56 (su reintento) con su estado; {@code null} si todavía no se reintentó. */
    private EstadoNumero reintentoDe56() {
        try {
            List<EstadoNumero> l = clasificarTabla().values().stream().filter(e -> "DE-56".equals(e.id())).toList();
            return l.size() < 2 ? null : l.get(l.size() - 1);
        } catch (Exception x) {
            throw new IllegalStateException(x);
        }
    }

    /** El DE del catálogo con los datos de prueba de la corrida (sin el timbrado ni el número: eso es de la reserva). */
    private DocumentoElectronico armar(String id, DocumentoElectronico de) {
        CasosFase12.leyendas(de);
        // emisor: el gEmis de una venta de test8 (si no, 1261) con el RUC del emisor de prueba (antes de comparar nada)
        TgEmis em = copiaEmisor();
        em.setdRucEm(emisor.getRuc());
        em.setdDVEmi(emisor.getDv());
        de.getgDatGralOpe().setgEmis(em);
        // guarda D28 de la reemisión: el original no puede llevar el establecimiento-punto de prueba
        de.getgTimb().setdEst("999");
        de.getgTimb().setdPunExp("999");
        // receptor contribuyente = el de prueba (RUC del emisor de prueba, M8-04)
        TgDatRec rec = de.getgDatGralOpe().getgDatRec();
        if (rec != null && rec.getdRucRec() != null) {
            rec.setdRucRec(emisor.getRuc());
            rec.setdDVRec(Short.parseShort(emisor.getDv()));
            rec.setdNomRec(RECEPTOR_DE_PRUEBA);
        }
        // transportista y procesadora = emisor de prueba (I12-08, I8-09, D58; E623 · 1603)
        PreparacionVivoFase12.transportistaDePrueba(de);
        for (TgPaConEIni p : pagos(de)) {
            if (p.getgPagTarCD() != null && p.getgPagTarCD().getdRUCProTar() != null) {
                p.getgPagTarCD().setdRUCProTar(emisor.getRuc());
                p.getgPagTarCD().setdDVProTar(Short.parseShort(emisor.getDv()));
            }
        }
        // ID-I06: el único ítem a 1 × 22.000 Gs (la FE de DE-16 es de 35.000: H004d · 2417)
        if ("ID-I06".equals(id)) {
            TgCamItem it = de.getgDtipDE().getgCamItemList().get(0);
            it.setdCantProSer(BigDecimal.ONE);
            it.getgValorItem().setdPUniProSer(new BigDecimal("22000"));
        }
        // documentos asociados: el CDC de la corrida (explícito o por el CDC del catálogo de su base)
        String explicito = asociadoExplicito(id);
        if (de.getgCamDEAsocList() != null) {
            for (TgCamDEAsoc a : de.getgCamDEAsocList()) {
                if (a.getiTipDocAso() == TiTipDocAso.ELECTRONICO) {
                    a.setdCdCDERef(explicito != null ? explicito : cdcDeLaCorrida.getOrDefault(a.getdCdCDERef(), a.getdCdCDERef()));
                }
            }
        }
        if (de.getgDtipDE().getgCamItemList() != null) {
            for (TgCamItem it : de.getgDtipDE().getgCamItemList()) {
                if (it.getdCDCAnticipo() != null) {
                    it.setdCDCAnticipo(cdcDeLaCorrida.getOrDefault(it.getdCDCAnticipo(), it.getdCDCAnticipo()));
                }
            }
        }
        return de;
    }

    private String asociadoExplicito(String id) {
        switch (id) {
            case "ID-I06":
                return cdc(aprobados.get("DE-16"));
            case "DE-61":
                return cdc(aprobados.get(base61));
            case "DE-41":
                return planB41 ? cdc(aprobados.get("DE-41-FE")) : cdcDeLaTabla("001-001-0001070");
            default:
                return null;
        }
    }

    /** Lo que {@link ReemisionPrueba#reemitir} le pone al DE, sobre el gemelo (sin reservar). */
    private void comoReemitido(DocumentoElectronico de, LocalDateTime fecha, int numero) {
        de.getgTimb().setdNumTim(emisor.getTimbrado());
        de.getgTimb().setdFeIniT(emisor.getInicioTimbrado());
        de.getgTimb().setdEst(emisor.getEstablecimiento());
        de.getgTimb().setdPunExp(emisor.getPuntoExpedicion());
        de.getgTimb().setdNumDoc(String.format("%07d", numero));
        TgEmis e = de.getgDatGralOpe().getgEmis();
        e.setdRucEm(emisor.getRuc());
        e.setdDVEmi(emisor.getDv());
        e.setdNomEmi(ReemisionPrueba.RAZON_SOCIAL_PRUEBA);
        e.setdEmailE(email);
        de.setdFecFirma(fecha);
        de.getgDatGralOpe().setdFeEmiDE(fecha);
    }

    /** OP-20: firma y emisión 73 h atrás (D56); OP-22: emisión 31 días atrás (la firma, ahora). */
    private static LocalDateTime fechaDeEmision(String id, LocalDateTime ahora) {
        switch (id) {
            case "OP-20":
                return ahora.minusHours(73);
            case "OP-22":
                return ahora.minusDays(31);
            default:
                return ahora;
        }
    }

    /** Lo que se cambia después de la reserva sin tocar el CDC (la firma no entra en el CDC). */
    private static void ajustesTrasReemitir(String id, DocumentoElectronico de, LocalDateTime ahora) {
        if ("OP-22".equals(id)) {
            de.setdFecFirma(ahora);
        }
    }

    /**
     * Afirma la condición del caso y devuelve el dato que la cumple (para {@code simulacion.tsv}). Las comprobaciones
     * comunes valen para todos: emisor, receptor y transportista de prueba, procesadora, leyenda y CDC asociados de la
     * corrida.
     *
     * @param gemelo {@code true} antes de reservar.
     */
    private String condicion(String id, DocumentoElectronico de, boolean gemelo) throws Exception {
        List<String> d = new ArrayList<>();
        TgEmis em = de.getgDatGralOpe().getgEmis();
        Assertions.assertEquals(emisor.getRuc(), em.getdRucEm(), "emisor = SIFEN_EMISOR_RUC");
        TgDatRec rec = de.getgDatGralOpe().getgDatRec();
        if (rec != null && rec.getdRucRec() != null) {
            Assertions.assertEquals(emisor.getRuc(), rec.getdRucRec(), "receptor contribuyente = emisor de prueba (M8-04)");
            Assertions.assertEquals(RECEPTOR_DE_PRUEBA, rec.getdNomRec());
            d.add("receptor de prueba (dRucRec = SIFEN_EMISOR_RUC)");
        }
        PreparacionVivoFase12.afirmarTransportistaDePrueba(de);
        if (de.getgDtipDE().getgTransp() != null && de.getgDtipDE().getgTransp().getgCamTrans() != null) {
            d.add("transportista dRucTrans/dDVTrans = dRucEm/dDVEmi = SIFEN_EMISOR_RUC (I12-08)");
        }
        for (TgPaConEIni p : pagos(de)) {
            if (p.getgPagTarCD() != null && p.getgPagTarCD().getdRUCProTar() != null) {
                Assertions.assertEquals(emisor.getRuc(), p.getgPagTarCD().getdRUCProTar(), "procesadora = emisor de prueba (1603)");
                Assertions.assertEquals(emisor.getDv(), String.valueOf(p.getgPagTarCD().getdDVProTar()));
                d.add("procesadora dRUCProTar/dDVProTar = SIFEN_EMISOR_RUC (E623 · 1603)");
            }
        }
        if (de.getgDtipDE().getgCamItemList() != null && !de.getgDtipDE().getgCamItemList().isEmpty()) {
            Assertions.assertEquals(CasosFase12.LEYENDA_ITEM, de.getgDtipDE().getgCamItemList().get(0).getdDesProSer(), "leyenda (Q14)");
        }
        // asociados: todo CDC electrónico es de esta corrida o uno aprobado de antes que la condición nombra
        if (de.getgCamDEAsocList() != null) {
            for (TgCamDEAsoc a : de.getgCamDEAsocList()) {
                if (a.getiTipDocAso() == TiTipDocAso.ELECTRONICO) {
                    String base = baseDe(a.getdCdCDERef());
                    Assertions.assertNotNull(base, id + ": el CDC asociado no es de un documento aprobado de esta corrida ni de la condición");
                    if (id.startsWith("OP-3") && id.endsWith("-NC")) {
                        // M12-16: la NC de OP-30..OP-32 cita su propia FE, no cualquier aprobado de la corrida
                        String fe = id.replace("-NC", "-FE");
                        Assertions.assertNotNull(aprobados.get(fe), id + ": " + fe + " no está aprobada");
                        // I12-19: sin assertEquals de CDC (volcaría los dos CDC enteros en el mensaje de la falla)
                        Assertions.assertTrue(aprobados.get(fe).getCdc().equals(a.getdCdCDERef()), id + ": la NC no cita su propia FE (" + fe
                                + "; cita la de " + base + ")");
                    }
                    d.add("dCdCDERef = CDC de " + base);
                }
            }
        }
        for (TgCamItem it : items(de)) {
            if (it.getdCDCAnticipo() != null) {
                Assertions.assertEquals(cdc(aprobados.get("DE-36")), it.getdCDCAnticipo(), "dCDCAnticipo = DE-36 de la corrida (E719)");
                d.add("dCDCAnticipo = CDC de DE-36");
            }
        }
        switch (id) {
            case "DE-41" -> {
                TgDatRec n = receptorNominado();
                Assertions.assertEquals(n.getiNatRec(), rec.getiNatRec(), "DE-41: receptor de la nominación (H004h · 2441)");
                Assertions.assertEquals(n.getiTiOpe(), rec.getiTiOpe());
                Assertions.assertEquals(n.getiTipIDRec(), rec.getiTipIDRec());
                Assertions.assertEquals(n.getdNumIDRec(), rec.getdNumIDRec());
                Assertions.assertEquals(n.getdNomRec(), rec.getdNomRec());
                d.add(planB41 ? "plan B: FE innominada nueva DE-41-FE y su nominación" : "sobre 001-001-0001070 (H-02 de §8.7, nominada con EV-05 en H-03)");
                d.add("receptor = el de la nominación (cédula " + n.getdNumIDRec() + ", " + n.getdNomRec() + ")");
            }
            case "DE-16" -> d.add("FE innominada exclusiva de ID-I06 (no se nomina, no se cancela, ninguna otra NC la cita)");
            case "ID-I06" -> {
                BigDecimal total = new BigDecimal(XmlCanonico.texto(aprobados.get("DE-16").getXml(), "dTotGralOpe"));
                TgCamItem it = de.getgDtipDE().getgCamItemList().get(0);
                Assertions.assertEquals(1, de.getgDtipDE().getgCamItemList().size(), "ID-I06: un solo ítem");
                Assertions.assertEquals(0, it.getgValorItem().getdPUniProSer().multiply(it.getdCantProSer()).compareTo(new BigDecimal("22000")),
                        "ID-I06: 1 × 22.000");
                Assertions.assertTrue(new BigDecimal("22000").compareTo(total) < 0, "ID-I06: 22.000 < total de DE-16 (H004d · 2417)");
                d.add("ítem 1 × 22000 < " + total.toPlainString() + " de DE-16 (" + numeros.get("DE-16") + ")");
            }
            case "DE-62" -> {
                if (gemelo) {
                    d.add(totalesDe62(de.generarXml(FixturesSifen.contexto(FixturesSifen.configuracion()), FixturesSifen.configuracion())));
                }
            }
            case "DE-67" -> {
                LocalDate emision = de.getgDatGralOpe().getdFeEmiDE().toLocalDate();
                for (LocalDate v : de.getgDtipDE().getgCamEsp().getgGrupAdi().getdVencPagList()) {
                    Assertions.assertFalse(v.isBefore(emision), "DE-67: vencimiento " + v + " anterior a la emisión (E824 · 2055)");
                }
                d.add("vencimientos " + de.getgDtipDE().getgCamEsp().getgGrupAdi().getdVencPagList() + " ≥ emisión " + emision);
            }
            case "DE-68" -> {
                String reg = items(de).get(0).getgRasMerc().getdNumReg();
                String regCom = items(de).get(0).getgRasMerc().getdNumRegEntCom();
                Assertions.assertEquals(20, reg.length(), "DE-68: dNumReg de 20 (I12-05)");
                Assertions.assertEquals(20, regCom.length(), "DE-68: dNumRegEntCom de 20 (I12-05)");
                d.add("dNumReg y dNumRegEntCom de 20 caracteres");
            }
            case "OP-20", "OP-22" -> {
                LocalDate emision = de.getgDatGralOpe().getdFeEmiDE().toLocalDate();
                Assertions.assertTrue(emisor.getInicioTimbrado().isBefore(emision),
                        id + ": dFeIniT del timbrado de prueba (" + emisor.getInicioTimbrado() + ") no es anterior a la emisión " + emision
                        + " (C004b · 1103)");
                d.add("dFeIniT " + emisor.getInicioTimbrado() + " < emisión " + emision + ("OP-20".equals(id) ? " (firma y emisión −73 h)"
                        : " (emisión −31 días, firma ahora)"));
            }
            case "DE-61" -> {
                String recBase = XmlCanonico.texto(aprobados.get(base61).getXml(), "dRucRec");
                Assertions.assertEquals(recBase, rec.getdRucRec(), "DE-61: el mismo receptor que su FE");
                Assertions.assertFalse(r.containsValue(base61) || "DE-09".equals(base61), "DE-61: nunca R1–R3 ni DE-09");
                d.add("base = " + base61 + " (" + numeros.get(base61) + "), mismo receptor");
            }
            case "DE-34" -> {
                Assertions.assertEquals(CasosFase12.INFO_FISCAL_EXPORTACION, de.getgOpeDE().getdInfoFisc(), "DE-34: dInfoFisc de la NT 007 corregido");
                d.add("dInfoFisc de la NT 007 con el inciso i) (dato corregido de §12.1.3)");
            }
            case "DE-53" -> d.add("cambio de pago ponderado (M12-02): sin el aviso JBS-E-30");
            case "DE-55", "DE-56", "DE-56x" -> d.add(items(de).size() + " ítems; tiempo de lectura ampliado");
            case SONDA_56 -> {
                Assertions.assertEquals(500, items(de).size(), SONDA_56 + ": 500 ítems (I12-23)");
                d.add("500 ítems con los textos largos de DE-56x (sonda de I12-23, en lugar de DE-56x); tiempo de lectura ampliado");
            }
            default -> {
            }
        }
        if (id.endsWith("-FE") && id.startsWith("OP-3")) {
            d.add("FE propia y exclusiva de " + id.substring(0, 5) + " (ningún otro caso la cita)");
        }
        if (id.endsWith("-NC") && id.startsWith("OP-3")) {
            String fe = id.replace("-NC", "-FE");
            Assertions.assertTrue(de.getgCamDEAsocList() != null && de.getgCamDEAsocList().stream()
                    .anyMatch(a -> a.getiTipDocAso() == TiTipDocAso.ELECTRONICO && cdc(aprobados.get(fe)).equals(a.getdCdCDERef())),
                    id + ": la NC asocia el CDC de " + fe + " de esta corrida (M12-16)");
            BigDecimal totalFe = new BigDecimal(XmlCanonico.texto(aprobados.get(fe).getXml(), "dTotGralOpe"));
            BigDecimal totalNc = new BigDecimal(XmlCanonico.texto(de.generarXml(FixturesSifen.contexto(FixturesSifen.configuracion()),
                    FixturesSifen.configuracion()), "dTotGralOpe"));
            if (id.startsWith("OP-30")) {
                Assertions.assertTrue(totalNc.compareTo(totalFe) < 0, "OP-30: NC parcial");
            } else if (id.startsWith("OP-31")) {
                Assertions.assertEquals(0, totalNc.compareTo(totalFe), "OP-31: NC por el total");
            }
            d.add("NC " + totalNc.toPlainString() + " de " + totalFe.toPlainString() + " de " + fe + " (" + numeros.get(fe) + ")");
        }
        return String.join(" · ", d);
    }

    private static String totalesDe62(String xml) throws Exception {
        String[][] esperado = {{"dComi", "110000"}, {"dIVAComi", "10000.00"}, {"dTotIVA", "110000.00"}, {"dTotGralOpe", "1210000"}};
        List<String> l = new ArrayList<>();
        for (String[] e : esperado) {
            String v = XmlCanonico.texto(xml, e[0]);
            Assertions.assertEquals(e[1], v, "DE-62: " + e[0]);
            l.add(e[0] + " " + v);
        }
        return String.join(", ", l);
    }

    /** El documento de la corrida (o de la condición) al que corresponde un CDC asociado; {@code null} si ninguno. */
    private String baseDe(String cdc) {
        for (Map.Entry<String, SignedDocument> e : aprobados.entrySet()) {
            if (e.getValue().getCdc().equals(cdc)) {
                return e.getKey() + " (" + numeros.get(e.getKey()) + ")";
            }
        }
        if (cdc != null && cdc.equals(cdcDeLaTabla("001-001-0001070"))) {
            return "001-001-0001070";
        }
        return null;
    }

    /** Nominación de EV-05 sobre el CDC dado, como la de H-03 (§8.7, {@code SifenVivoBetaTest#nominacion}). */
    private static EventosDE nominacion(SignedDocument inn) {
        EventosDE ev = CasosDePrueba.eventoNominacionConDireccion((short) 1);
        TrGesEve ges = ev.getrGesEveList().get(0);
        ges.setId(null);
        ges.setdFecFirma(null);
        TrGeVeNom nom = ges.getgGroupTiEvt().getrGeVeNom();
        nom.setId(inn.getCdc());
        nom.setdDTipIDRec("Cédula paraguaya");   // GENFE011: la descripción de la Tabla
        return ev;
    }

    /** El receptor que nominó EV-05 en H-03 (§8.7), como lo armó {@code SifenVivoBetaTest#nominacion}. */
    private static TgDatRec receptorNominado() {
        TrGeVeNom n = CasosDePrueba.eventoNominacionConDireccion((short) 1).getrGesEveList().get(0).getgGroupTiEvt().getrGeVeNom();
        TgDatRec rec = new TgDatRec();
        rec.setiNatRec(TiNatRec.getByVal(n.getiNatRec()));
        rec.setiTiOpe(org.javabeanstack.sifen.core.types.TiTiOpe.getByVal(n.getiTiOpe()));
        rec.setiTipIDRec(org.javabeanstack.sifen.core.types.TiTipDocRec.getByVal(n.getiTipIDRec()));
        rec.setdNumIDRec(n.getdNumIDRec());
        rec.setdNomRec(n.getdNomRec());
        return rec;
    }

    // ------------------------------------------------------------------ OP-21: firmar ahora, enviar 72 h después

    private void op21Firmar(Fila f) throws Exception {
        String id = "OP-21";
        if (yaReservado(f, id)) {
            fila(f, "—", "sin reservar", "", "", "", "", f.sifen(), "", "OP-21 ya está firmado y reservado de una corrida anterior");
            return;
        }
        LocalDateTime ahora = LocalDateTime.now().minusMinutes(1).truncatedTo(ChronoUnit.SECONDS);
        // M12-15: gemelo validado y condición antes de reservar, y condición otra vez sobre el reservado
        DocumentoElectronico gemelo = armar(id, CasosFase12.leyendas(caso("DE-05")));
        comoReemitido(gemelo, ahora, reemision.getSiguiente());
        String problema = problemaDelGemelo(id, gemelo);
        if (problema != null) {
            fila(f, "—", "sin envío", "", "", "", "", f.sifen(), "NO CUMPLE (antes de reservar): " + problema, "no se reserva");
            fallas.add(id + " antes de reservar: " + problema);
            return;
        }
        DocumentoElectronico de = armar(id, CasosFase12.leyendas(caso("DE-05")));
        ReemisionPrueba.Reemision re = reservar(id, de, ahora);
        String dato;
        try {
            dato = condicion(id, de, false);
        } catch (AssertionFailedError | IllegalStateException x) {
            reemision.registrar(id, re, "ISifenClient.sign:condicion-no-cumplida", 0, null, null, limpio(x.getMessage()));
            fila(f, re.enviado, "sin envío", re.cdc, "", "", "", f.sifen(), "NO CUMPLE (reservado, antes de firmar): " + limpio(x.getMessage()),
                    "número reservado y no firmado: va a 12.3.11");
            fallas.add(id + " condición tras reservar: " + x.getMessage());
            return;
        }
        SignedDocument doc = client.sign(de);
        Assertions.assertTrue(re.cdc.equals(doc.getCdc()), "OP-21: el CDC firmado no es el reservado");
        Path archivo = firmadoOp21(doc.getCdc());
        Files.writeString(archivo, doc.getXml(), StandardCharsets.UTF_8);
        reemision.registrar(id, re, "ISifenClient.sign:firmado-sin-enviar", 0, null, null, "se envía desde " + ahora.plusHours(72));
        firmados.put(id, doc);
        numeros.put(id, re.enviado);
        fila(f, re.enviado, "sign (sin enviar)", doc.getCdc(), "acepta", "firmado", "", f.sifen(), (dato.isEmpty() ? "" : dato + " · ") + "dFecFirma " + ahora
                + "; se envía desde " + ahora.plusHours(72) + " con readSigned (12.3.12)", "guardado " + archivo.getFileName());
    }

    /** El archivo del OP-21 firmado y sin enviar (fuera de git). */
    private Path firmadoOp21(String cdc) {
        return dir.resolve((simular ? "simulacion/" : "respuestas/") + "OP-21-" + cdc + ".firmado.xml");
    }

    /**
     * 12.3.12: envía el OP-21 firmado en 12.3.1 con {@code readSigned}, sin refirmar, <b>una sola vez</b> (E12-04): solo
     * si su último estado en la tabla es {@code firmado-sin-enviar}. Con un resultado definitivo no se reenvía; sin estado
     * final, lo resuelve la consulta de pendientes (nunca otro envío).
     */
    private void op21Enviar(Fila f) throws Exception {
        String id = "OP-21";
        EstadoNumero en = estadoDe(id);
        if (en == null) {
            fila(f, "—", "sin envío", "", "", "", "", f.sifen(), "", "no hay un OP-21 reservado en 12.3.1");
            return;
        }
        if (en.estado() != Estado.FIRMADO_SIN_ENVIAR) {
            String obs = switch (en.estado()) {
                case APROBADO -> "ya aprobado en una corrida anterior: no se reenvía (E12-04)";
                case PENDIENTE -> "sin estado final (" + en.motivo() + "): se resuelve por consulta, no se reenvía (D55)";
                case RECHAZADO, NO_ENVIADO -> "ya tiene resultado (" + en.motivo() + "): no se reenvía; va a 12.3.11 (D55)";
                default -> "estado " + en.estado() + ": no se envía";
            };
            fila(f, en.completo(), "sin envío", en.cdc(), "", String.valueOf(en.estado()), "", f.sifen(), "", obs);
            if (en.estado() == Estado.PENDIENTE) {
                fallas.add(id + " sin estado final: " + en.motivo());
            }
            return;
        }
        Path archivo = firmadoOp21(en.cdc());
        if (!Files.exists(archivo)) {
            fila(f, en.completo(), "sin envío", en.cdc(), "", "", "", f.sifen(), "", "falta el firmado de 12.3.1: no se refirma (va a 12.3.11)");
            fallas.add(id + " sin el firmado guardado");
            return;
        }
        SignedDocument doc = client.readSigned(Files.readString(archivo, StandardCharsets.UTF_8));
        Assertions.assertTrue(en.cdc().equals(doc.getCdc()), "OP-21: el firmado guardado no es el de la tabla");
        LocalDateTime desde = doc.getSignatureDate().plusHours(72);
        if (LocalDateTime.now().isBefore(desde)) {
            if (!simular) {
                fila(f, en.completo(), "sin envío", doc.getCdc(), "", "", "", f.sifen(), "NO CUMPLE todavía: se envía desde " + desde,
                        "no bloquea (D56); si al cerrar sigue sin enviar, 12.3.11 lo inutiliza con -D" + P + "op21.inutilizar=true");
                return;
            }
        }
        ReemisionPrueba.Reemision re = new ReemisionPrueba.Reemision("—", en.completo(), null, doc.getCdc(),
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        firmados.put(id, doc);
        numeros.put(id, en.completo());
        SignedDocument ok = enviar(f, id, doc, re, "acepta (readSigned, sin refirmar)", f.sifen(), "dFecFirma " + doc.getSignatureDate()
                + "; desde " + desde + (simular && LocalDateTime.now().isBefore(desde) ? " (ensayo: sin esperar las 72 h)" : ""));
        if (ok == null) {
            fila(f, en.completo(), "después del envío", doc.getCdc(), "", "", "", f.sifen(), "",
                    "OP-21 no quedó aprobado: correr 12.3.11 otra vez para inutilizarlo (12.3.11 va antes que 12.3.12 en cada corrida, D55)");
        }
    }

    // ------------------------------------------------------------------ eventos del receptor (12.3.8)

    private void eventoDelReceptor(Fila f) throws Exception {
        asignarR();
        String id = f.caso();
        String rx = switch (id) {
            case "EV-07", "EV-03", "EV-09" -> "R1";
            case "EV-04", "EV-04c" -> "R2";
            default -> "R3";
        };
        String caso = r.get(rx);
        if (caso == null) {
            fila(f, "—", "sin envío", "", "", "", "", f.sifen(), "", rx + " sin asignar: ninguna FE aprobada de 12.3.5 sirve (I12-11)");
            return;
        }
        if (receptorSuspendido) {
            fila(f, numeros.get(caso), "sin envío", "", "", "", "", f.sifen(), "", "eventos del receptor suspendidos por EV-07 (0143 o 4006): escalar");
            return;
        }
        String previo = switch (id) {
            case "EV-03" -> "EV-07";
            case "EV-09" -> "EV-03";
            case "EV-04c" -> "EV-04";
            case "EV-08c" -> "EV-08";
            default -> null;
        };
        if (previo != null && !eventos.containsKey(previo)) {
            fila(f, numeros.get(caso), "sin envío", "", "", "", "", f.sifen(), "", "va después de " + previo + ", que no se envió");
            return;
        }
        SignedDocument fe = aprobados.get(caso);
        String xml = fe.getXml();
        LocalDateTime emision = LocalDateTime.parse(XmlCanonico.texto(xml, "dFeEmiDE"));
        if (recepcion == null) {
            recepcion = LocalDateTime.now().minusSeconds(30).truncatedTo(ChronoUnit.SECONDS);
        }
        LocalDateTime rcp = recepcion.isBefore(emision) ? emision : recepcion;
        Assertions.assertFalse(rcp.isBefore(emision) || rcp.isAfter(LocalDateTime.now()), "dFecRecep no anterior a dFecEmi ni futura (I12-11)");
        EventosDE ev;
        String dato = rx + " = " + caso + " (" + numeros.get(caso) + ")";
        switch (id) {
            case "EV-07" -> {
                ev = evento12("EV-07");
                TrGeVeNotRec n = ev.getrGesEveList().get(0).getgGroupTiEvt().getrGeVeNotRec();
                n.setId(fe.getCdc());
                n.setdFecEmi(emision);
                n.setdFecRecep(rcp);
                n.setiTipRec(TiNatRec.getByVal(Short.parseShort(XmlCanonico.texto(xml, "iNatRec"))));
                n.setdNomRec(XmlCanonico.texto(xml, "dNomRec"));
                n.setdRucRec(XmlCanonico.texto(xml, "dRucRec"));
                n.setdDVRec(XmlCanonico.texto(xml, "dDVRec"));
                String gs = XmlCanonico.texto(xml, "dTotGralOpeGs");
                String total = gs != null ? gs : XmlCanonico.texto(xml, "dTotGralOpe");
                n.setdTotalGs(new BigDecimal(total));
                Assertions.assertEquals(emisor.getRuc(), n.getdRucRec(), "EV-07: el receptor es el de prueba");
                dato += " · Id, dFecEmi " + emision + ", dTotalGs " + total + (gs != null ? " (dTotGralOpeGs)" : " (PYG: dTotGralOpe)")
                        + ", iTipRec " + n.getiTipRec().getVal() + ", dNomRec " + n.getdNomRec() + ", dRucRec = SIFEN_EMISOR_RUC, de R1 aprobada"
                        + " · dFecRecep " + rcp;
            }
            case "EV-03", "EV-04" -> {
                ev = CasosDePrueba.eventos().stream().filter(k -> k.getId().equals(id)).findFirst().orElseThrow().construir();
                TgGroupTiEvt g = ev.getrGesEveList().get(0).getgGroupTiEvt();
                if (g.getrGeVeConf() != null) {
                    g.getrGeVeConf().setId(fe.getCdc());
                    g.getrGeVeConf().setdFecRecep(rcp);
                } else {
                    TrGeVeDisconf dis = g.getrGeVeDisconf();
                    dis.setId(fe.getCdc());
                }
                dato += " · dFecRecep " + rcp;
            }
            case "EV-08" -> {
                ev = evento12("EV-08");
                TrGeVeDescon n = ev.getrGesEveList().get(0).getgGroupTiEvt().getrGeVeDescon();
                n.setId(fe.getCdc());
                n.setdFecEmi(emision);
                n.setdFecRecep(rcp);
                n.setiTipRec(TiNatRec.getByVal(Short.parseShort(XmlCanonico.texto(xml, "iNatRec"))));
                n.setdNomRec(XmlCanonico.texto(xml, "dNomRec"));
                n.setdRucRec(XmlCanonico.texto(xml, "dRucRec"));
                n.setdDVRec(XmlCanonico.texto(xml, "dDVRec"));
                dato += " · receptor de R3 aprobada · dFecRecep " + rcp;
            }
            default -> {
                // EV-09, EV-04c, EV-08c: conformidad total
                ev = evento12("EV-09");
                TrGeVeConf conf = ev.getrGesEveList().get(0).getgGroupTiEvt().getrGeVeConf();
                conf.setId(fe.getCdc());
                conf.setdFecRecep(rcp);
                dato += " · conformidad total después de " + previo + " · dFecRecep " + rcp;
            }
        }
        TrGesEve ges = ev.getrGesEveList().get(0);
        ges.setId(null);          // la librería asigna el Id (I10-19: uno repetido da 4066)
        ges.setdFecFirma(null);   // y la hora de Asunción (I8-34)
        final EventosDE aEnviar = ev;
        EventResult res = evento(f, id, fe, dato, () -> client.sendEvents(aEnviar));
        if (res != null) {
            eventos.put(id, codigo(res));
            if ("EV-07".equals(id) && ("0143".equals(codigo(res)) || "4006".equals(codigo(res)))) {
                receptorSuspendido = true;
            }
        }
    }

    /** R1–R3 (I12-11): DE-45, DE-46 y DE-49, o la primera FE aprobada de la lista de reemplazos. */
    private void asignarR() throws Exception {
        if (!r.isEmpty()) {
            return;
        }
        if (base61 == null) {
            // I12-24: en una corrida aparte (12.3.8 sin 12.3.6) la FE de DE-61 no está en memoria: se repone
            base61 = reponerBase61();
        }
        String[][] titulares = {{"R1", "DE-45"}, {"R2", "DE-46"}, {"R3", "DE-49"}};
        for (String[] t : titulares) {
            if (aprobados.containsKey(t[1]) && conReceptorDePrueba(t[1])) {
                r.put(t[0], t[1]);
            }
        }
        for (String[] t : titulares) {
            if (r.containsKey(t[0])) {
                continue;
            }
            for (String c : REEMPLAZOS_R) {
                if (aprobados.containsKey(c) && conReceptorDePrueba(c) && !r.containsValue(c) && !c.equals(base61)) {
                    r.put(t[0], c);
                    break;
                }
            }
        }
        Map<String, String> ordenado = new TreeMap<>(r);
        r.clear();
        r.putAll(ordenado);
        fila(new Fila(51, "12.3.8", "(R1–R3)", "—", "—", "—", "—", "—", ""), "—", "asignación", "", "", "", "", "",
                "R1–R3 = " + r + " (FE de DE-61: " + base61 + ")", "");
    }

    private boolean conReceptorDePrueba(String caso) throws Exception {
        return emisor.getRuc().equals(XmlCanonico.texto(aprobados.get(caso).getXml(), "dRucRec"));
    }

    /**
     * I12-24: la FE de DE-61 de una corrida anterior, por el {@code dCdCDERef} de DE-61 aprobado (de los previos); si DE-61
     * no está aprobado, la que elegiría {@link #elegirBase61()}.
     */
    private String reponerBase61() throws Exception {
        SignedDocument d61 = aprobados.get("DE-61");
        if (d61 != null) {
            String ref = XmlCanonico.texto(d61.getXml(), "dCdCDERef");
            for (Map.Entry<String, SignedDocument> e : aprobados.entrySet()) {
                if (!"DE-61".equals(e.getKey()) && e.getValue().getCdc().equals(ref)) {
                    return e.getKey();
                }
            }
        }
        return elegirBase61();
    }

    /** FE de DE-61 (I12-11): DE-44, o la última aprobada de 12.3.5 que no sea DE-45, DE-46, DE-49 ni la cite otro. */
    private String elegirBase61() {
        if (aprobados.containsKey("DE-44")) {
            return "DE-44";
        }
        List<String> l = new ArrayList<>(FE_1235);
        Collections.reverse(l);
        for (String c : l) {
            if (aprobados.containsKey(c) && !List.of("DE-45", "DE-46", "DE-49", "DE-44").contains(c) && !r.containsValue(c)) {
                return c;
            }
        }
        return null;
    }

    private static EventosDE evento12(String id) {
        return CasosFase12.eventos().stream().filter(k -> k.id().equals(id)).findFirst().orElseThrow().construir();
    }

    // ------------------------------------------------------------------ eventos del emisor (12.3.8 y 12.3.12)

    /** OP-06: el emisor cancela R1 conformada (la NT 025 quitó 4004). */
    private void op06(Fila f) throws Exception {
        asignarR();
        String caso = r.get("R1");
        if (caso == null || receptorSuspendido || !"0600".equals(eventos.get("EV-03"))) {
            fila(f, "—", "sin envío", "", "", "", "", f.sifen(), "", "sin R1, eventos del receptor suspendidos o EV-03 no aprobado: "
                    + "sin la conformidad no prueba lo de la NT 025 (I12-11)");
            return;
        }
        SignedDocument fe = aprobados.get(caso);
        String ventana = esperarVentana(caso);
        evento(f, "OP-06", fe, "R1 = " + caso + " conformada (EV-03 0600) · " + ventana,
                () -> client.cancel(fe.getCdc(), MOTIVO_CANCELACION));
    }

    /** OP-03: inutilizar el número de un aprobado de la corrida (ID-C08): 4065. */
    private void op03(Fila f) throws Exception {
        SignedDocument d = aprobados.get("ID-C08");
        if (d == null) {
            fila(f, "—", "sin envío", "", "", "", "", f.sifen(), "", "ID-C08 no quedó aprobada");
            return;
        }
        String num = d.getCdc().substring(17, 24);
        NumberRange rango = NumberRange.builder().timbrado(String.valueOf(emisor.getTimbrado())).establishment(emisor.getEstablecimiento())
                .issuePoint(emisor.getPuntoExpedicion()).documentType(TTiDE.FACTURA_ELECTRONICA).from(num).to(num)
                .reason(MOTIVO_INUTILIZACION + " - numero aprobado").build();
        evento(f, "OP-03", d, "número de ID-C08 (" + numeros.get("ID-C08") + "), aprobado en esta corrida",
                () -> client.invalidateNumbers(rango));
    }

    /** OP-04, OP-01 y OP-02: cancelar un documento aprobado en fases anteriores (por su número en la tabla). */
    private void cancelarAnterior(Fila f) throws Exception {
        LocalDateTime ahora = LocalDateTime.now();
        String numero;
        String dato;
        String esperado = f.sifen();
        switch (f.caso()) {
            case "OP-04" -> {
                numero = "001-001-0001050";
                boolean pasadas = ahora.isAfter(LocalDateTime.of(2026, 11, 27, 1, 21));
                dato = "cancelar otra vez 0001050 (cancelada en §8.7)" + (pasadas ? "; pasadas las 48 h: 4009 también es coherente" : "");
                if (pasadas) {
                    // I12-17: lo que la lista dice coherente no se marca como falla (se anota cuál salió)
                    esperado = f.sifen() + "+4009";
                }
            }
            case "OP-01" -> {
                LocalDateTime a = LocalDateTime.of(2026, 11, 26, 9, 15);
                LocalDateTime b = LocalDateTime.of(2026, 11, 25, 21, 35);
                if (!ahora.isBefore(a)) {
                    numero = "001-001-0001001";
                    dato = "0001001: más de 48 h desde dFecProc (desde " + a + ")";
                } else if (!ahora.isBefore(b)) {
                    numero = "001-001-0000051";
                    dato = "0000051: más de 48 h desde dFecProc (desde " + b + ")";
                } else if (simular) {
                    numero = "001-001-0000051";
                    dato = "NO CUMPLE a la hora del ensayo (" + ahora.truncatedTo(ChronoUnit.MINUTES) + "): 0000051 desde " + b
                            + ", 0001001 desde " + a + " (ensayo: se recorre igual)";
                } else {
                    fila(f, "—", "sin envío", "", "", "", "", f.sifen(), "NO CUMPLE todavía: 0000051 desde " + b + ", 0001001 desde " + a,
                            "no se envía en esta corrida: volver a correr la tanda 12.3.8 (o -D" + P + "casos=OP-01) desde " + b
                            + "; lo ya hecho no se repite (I12-17)");
                    return;
                }
            }
            default -> {
                LocalDateTime desde = LocalDateTime.of(2026, 11, 30, 21, 35);
                numero = "001-001-0000060";
                if (ahora.isBefore(desde) && !simular) {
                    fila(f, "—", "sin envío", "", "", "", "", f.sifen(), "NO CUMPLE todavía: desde " + desde, "no bloquea (D56)");
                    return;
                }
                dato = "NC 0000060: 168 h desde " + desde + (ahora.isBefore(desde) ? " (ensayo: todavía no; se recorre igual)" : "");
            }
        }
        String cdc = cdcDeLaTabla(numero);
        if (cdc == null) {
            fila(f, numero, "sin envío", "", "", "", "", f.sifen(), "", numero + " no está aprobada en la tabla");
            fallas.add(f.caso() + " sin " + numero);
            return;
        }
        evento(f, f.caso(), null, dato + " · CDC de la tabla", esperado, () -> client.cancel(cdc, MOTIVO_CANCELACION), numero, cdc);
    }

    // ------------------------------------------------------------------ OP-30 a OP-32 (12.3.9, antes de OP-10: I12-15 (b))

    private void cancelacionConNota(Fila f) throws Exception {
        String op = f.caso();
        Supplier<DocumentoElectronico> feCat = CasosFase12.cancelacionConNotas().get(op + "-FE");
        Supplier<DocumentoElectronico> ncCat = CasosFase12.cancelacionConNotas().get(op + "-NC");
        SignedDocument fe = documento(f, op + "-FE", feCat, "0260");
        if (fe == null) {
            fila(f, "—", "sin envío", "", "", "", "", f.sifen(), "", op + "-FE no quedó aprobada: no van ni la NC ni el cancel");
            return;
        }
        if ("OP-32".equals(op)) {
            String ventana = esperarVentana(op + "-FE");
            EventResult can = evento(f, op + " cancel", fe, "cancel de OP-32-FE antes de la NC · " + ventana, "0600",
                    () -> client.cancel(fe.getCdc(), MOTIVO_CANCELACION), numeros.get(op + "-FE"), fe.getCdc());
            if (!"0600".equals(can != null ? codigo(can) : eventos.get(op + " cancel"))) {
                fila(f, "—", "sin envío", "", "", "", "", "2404", "", "el cancel no dio 0600: la NC no se envía ni se reserva");
                return;
            }
            String nc = op + "-NC";
            boolean ncDeAntes = reservadosAntes.contains(nc) && !reintentar.contains(nc);
            if (!ncDeAntes && !canceladaEnSifen(f, op + "-FE", fe)) {
                return;
            }
            documento(f, op + "-NC", ncCat, "2404");
            return;
        }
        SignedDocument nc = documento(f, op + "-NC", ncCat, "0260");
        if (nc == null) {
            fila(f, "—", "sin envío", "", "", "", "", f.sifen(), "", op + "-NC no quedó aprobada: el cancel no prueba lo pedido");
            return;
        }
        String ventana = esperarVentana(op + "-FE");
        evento(f, op + " cancel", fe, "cancel de " + op + "-FE con su NC (" + numeros.get(op + "-NC") + ") aprobada, sin cancelar la NC · "
                + ventana, f.sifen(), () -> client.cancel(fe.getCdc(), MOTIVO_CANCELACION), numeros.get(op + "-FE"), fe.getCdc());
    }

    /**
     * M12-16: antes de reservar la NC de OP-32, {@code queryDE} de su FE tiene que mostrar el evento de cancelación (un
     * resultado ambiguo no gasta el número). En el ensayo, el simulador contesta su 0422 fijo, que trae un cancel.
     */
    private boolean canceladaEnSifen(Fila f, String feId, SignedDocument fe) throws Exception {
        String motivo = null;
        for (int i = 1; i <= 3; i++) {
            if (simular) {
                sim.once(SimulatedService.QUERY_DE, Scenario.deFound());
            } else {
                Thread.sleep(10_000L);
            }
            pausar();
            try {
                DEQueryResult q = client.queryDE(fe.getCdc());
                guardar("consulta-" + fe.getCdc() + "-cancelada-" + i + ".xml", q.getRawResponse());
                boolean cancelada = "0422".equals(q.getResponseCode()) && q.getEvents() != null
                        && q.getEvents().stream().anyMatch(e -> e != null && e.contains("rGeVeCan"));
                if (cancelada) {
                    fila(sub(f, feId), numeros.get(feId), "queryDE (cancelación confirmada antes de reservar la NC)", fe.getCdc(), "",
                            String.valueOf(q.getStatus()), q.getResponseCode(), "0422", "la FE tiene su evento de cancelación en SIFEN"
                            + (simular ? " (ensayo: 0422 fijo del simulador, con un cancel)" : ""), "intento " + i);
                    return true;
                }
                motivo = "queryDE " + q.getResponseCode() + ", eventos " + (q.getEvents() == null ? 0 : q.getEvents().size())
                        + ": SIFEN no muestra la cancelación";
            } catch (SifenException x) {
                motivo = "queryDE excepción " + x.getCode();
            }
        }
        fila(sub(f, feId), numeros.get(feId), "sin envío", fe.getCdc(), "", "", "", "0422", "", motivo + ": la NC no se reserva (M12-16)");
        fallas.add(feId + " cancelación sin confirmar: " + motivo);
        return false;
    }

    /**
     * Ventana de la cancelación: dFecProc + 1 min (I8-13) ≤ firma ≤ dFecProc + 48 h (MT v150 §11.1.2, Tabla J, GDE004a · 4009).
     * Espera hasta el piso (con un tope de 10 min) y falla si ya pasó el techo.
     */
    private String esperarVentana(String caso) throws Exception {
        LocalDateTime aprob = aprobadoEn.get(caso);
        Assertions.assertNotNull(aprob, caso + ": sin hora de aprobación");
        LocalDateTime piso = aprob.plusMinutes(1);
        LocalDateTime techo = aprob.plusHours(48);
        if (!simular) {
            long espera = Duration.between(LocalDateTime.now(), piso).toMillis();
            Assertions.assertTrue(espera < 600_000L, caso + ": dFecProc más de 10 min adelantado (I8-13)");
            if (espera > 0) {
                Thread.sleep(espera + 1000L);
            }
        }
        LocalDateTime firma = LocalDateTime.now();
        Assertions.assertFalse(firma.isAfter(techo), caso + ": pasaron las 48 h desde la aprobación (4009)");
        return "firma " + firma.truncatedTo(ChronoUnit.SECONDS) + " entre dFecProc+1 min y dFecProc+48 h (" + techo.truncatedTo(ChronoUnit.MINUTES)
                + (simular ? "; ensayo: dFecProc = hora local, sin esperar" : "") + ")";
    }

    // ------------------------------------------------------------------ lotes (12.3.9)

    private void lote(Fila f) throws Exception {
        String op = f.caso();
        if (!simular && constanciaMksif == null) {
            // defensa: verificarModo ya exige la constancia para 12.3.9 (I12-16)
            fila(f, "—", "sin envío", "", "", "", "", f.sifen(), "", "falta -D" + P + "mksif (D52 (7))");
            fallas.add(op + " sin la constancia de MKSIF");
            return;
        }
        if (lotesAntes.containsKey(op)) {
            loteAnterior(f, op, lotesAntes.get(op));
            return;
        }
        if (reservadosAntes.contains(op + "-1")) {
            fila(f, "—", "sin envío", "", "", "", "", f.sifen(), "", "los números del lote ya están reservados de una corrida anterior y no "
                    + "hay registro del envío: no se vuelven a reservar ni se envía el lote; los consulta el paso de pendientes y, con estado "
                    + "final, van a 12.3.11");
            return;
        }
        if ("OP-11".equals(op) && !lote10Definitivo) {
            fila(f, "—", "sin envío", "", "", "", "", f.sifen(), "", "sin la respuesta definitiva de OP-10 (no se reenvía un lote sin la del anterior)");
            return;
        }
        int n = "OP-10".equals(op) ? 5 : 50;
        // M12-15: el lote entero dentro del rango de D52, antes de reservar el primero
        int primero = reemision.getSiguiente();
        if (primero < DESDE || primero + n - 1 > HASTA) {
            fila(f, "—", "sin envío", "", "", "", "", f.sifen(), "", "D52 (4): el lote de " + n + " (" + primero + "–" + (primero + n - 1)
                    + ") no entra entero entre " + DESDE + " y " + HASTA + ": no se reserva ningún número");
            fallas.add(op + " fuera del rango de D52 (" + primero + "–" + (primero + n - 1) + ")");
            detenido = true;
            return;
        }
        LocalDateTime ahora = LocalDateTime.now().minusMinutes(1).truncatedTo(ChronoUnit.SECONDS);
        // M12-15: el gemelo de cada FE, validado y con su condición, antes de reservar ninguna
        for (int i = 1; i <= n; i++) {
            String id = op + "-" + i;
            DocumentoElectronico gemelo = armar(id, CasosFase12.leyendas(caso("DE-05")));
            comoReemitido(gemelo, ahora, primero + i - 1);
            String problema = problemaDelGemelo(id, gemelo);
            if (problema != null) {
                fila(f, "—", "sin envío", "", "", "", "", f.sifen(), "NO CUMPLE (antes de reservar): " + id + ": " + problema,
                        "el lote no se reserva ni se envía");
                fallas.add(id + " antes de reservar: " + problema);
                return;
            }
        }
        List<SignedDocument> docs = new ArrayList<>();
        List<ReemisionPrueba.Reemision> res = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            String id = op + "-" + i;
            DocumentoElectronico de = armar(id, CasosFase12.leyendas(caso("DE-05")));
            ReemisionPrueba.Reemision re = reservar(id, de, ahora);
            if ("OP-10".equals(op) && i == 4) {
                // C008 · 1107: la fecha de inicio del timbrado cambiada (la validación local no la ve; no entra en el CDC)
                de.getgTimb().setdFeIniT(emisor.getInicioTimbrado().minusDays(1));
            }
            String falla = null;
            try {
                condicion(id, de, false);
            } catch (AssertionFailedError | IllegalStateException x) {
                falla = "condición tras reservar: " + limpio(x.getMessage());
                reemision.registrar(id, re, "ISifenClient.sign:condicion-no-cumplida", 0, null, null, limpio(x.getMessage()));
            }
            SignedDocument d = null;
            if (falla == null) {
                try {
                    d = client.sign(de);
                } catch (SifenValidationException x) {
                    falla = "rechazo local " + codigos(x.getErrors());
                    reemision.registrar(id, re, "ISifenClient.sign:rechazo-local", 0, x.getCode(), null, codigos(x.getErrors()).toString());
                }
            }
            if (falla != null) {
                // el lote no se envía: los ya reservados quedan «no enviado» en la tabla (van a 12.3.11), no pendientes
                for (int k = 0; k < ids.size(); k++) {
                    reemision.registrar(ids.get(k), res.get(k), "ISifenClient.sign:lote-no-enviado", 0, null, null, id + ": " + falla);
                }
                fila(f, re.enviado, "sign", re.cdc, falla, "", "", f.sifen(), "", "el lote no se envía: números reservados a 12.3.11");
                fallas.add(id + " " + falla);
                return;
            }
            docs.add(d);
            res.add(re);
            ids.add(id);
            firmados.put(id, d);
            numeros.put(id, re.enviado);
        }
        for (int k = 0; k < docs.size(); k++) {
            guardarPedido(ids.get(k), docs.get(k));
        }
        if (simular) {
            sim.once(SimulatedService.RECEIVE_BATCH, Scenario.batchReceived());
        }
        pausar();
        BatchSendResult b;
        try {
            b = client.sendBatch(docs);
        } catch (SifenException x) {
            for (int i = 0; i < docs.size(); i++) {
                reemision.registrar(ids.get(i), res.get(i), "ISifenClient.sendBatch:excepcion", 0, x.getCode(), null, x.getMessage());
            }
            fila(f, res.get(0).enviado + "…" + res.get(res.size() - 1).enviado, "sendBatch", "", "acepta", "excepción", x.getCode(), f.sifen(),
                    "", "resultado desconocido: " + x.isOutcomeUnknown() + "; no se reenvía el lote: lo consulta el paso de pendientes");
            fallas.add(op + " " + x.getCode());
            detenido = true;
            return;
        }
        // E12-03: primero la tabla (el número de lote), después la respuesta cruda
        for (int i = 0; i < docs.size(); i++) {
            reemision.registrar(ids.get(i), res.get(i), "ISifenClient.sendBatch:" + b.getBatchNumber(), 200, b.getResponseCode(),
                    String.valueOf(b.getStatus()), b.getMessage());
        }
        guardar("lote-" + b.getBatchNumber() + ".xml", b.getRawResponse());
        String rango = res.get(0).enviado + "…" + res.get(res.size() - 1).enviado;
        String dato = n + " FE mínimas (DE-05) en un solo envío" + ("OP-10".equals(op) ? "; la 4.ª con dFeIniT cambiada (C008 · 1107)" : "");
        fila(f, rango, "sendBatch", "lote " + b.getBatchNumber(), "acepta", String.valueOf(b.getStatus()), b.getResponseCode(), "0300", dato,
                "CDC en orden: " + b.getCdcs().equals(docs.stream().map(SignedDocument::getCdc).toList()));
        if (!"0300".equals(b.getResponseCode())) {
            fallas.add(op + " " + b.getResponseCode());
            detenido = true;
            return;
        }
        BatchQueryResult q = null;
        // M12-15: primera consulta a los 10 minutos y después cada 10 minutos o más (ISifenClient#queryBatch, Guía de la DNIT)
        long espera = ESPERA_LOTE_MS;
        for (int i = 1; i <= Integer.getInteger(P + "lote.intentos", 8); i++) {
            if (!simular) {
                Thread.sleep(espera);
            }
            programarConsultaDeLote(op, ids, res);
            q = client.queryBatch(b.getBatchNumber());
            guardar("consulta-lote-" + b.getBatchNumber() + "-" + i + ".xml", q.getRawResponse());
            fila(f, rango, "queryBatch intento " + i, "lote " + b.getBatchNumber(), "", String.valueOf(q.getStatus()), q.getResponseCode(), "0362",
                    "", "resultados " + (q.getResults() == null ? 0 : q.getResults().size()));
            if (!"0361".equals(q.getResponseCode())) {
                break;
            }
            Duration s = q.getSuggestedWait();
            espera = Math.min(1_800_000L, Math.max(ESPERA_LOTE_MS, s == null ? 0L : s.toMillis()));
        }
        if (q == null || !"0362".equals(q.getResponseCode())) {
            fallas.add(op + " sin respuesta definitiva del lote: consultar, no reenviar (los números quedan pendientes; no van a 12.3.11)");
            detenido = true;
            return;
        }
        int pendientes = registrarResultadoLote(f, op, b.getBatchNumber(), ids, res, docs, q, rango, dato);
        if ("OP-10".equals(op) && pendientes == 0) {
            lote10Definitivo = true;
        }
        if ("OP-11".equals(op)) {
            // queryDE de cinco al azar con el mismo digest
            List<Integer> idx = new ArrayList<>();
            for (int i = 0; i < docs.size(); i++) {
                idx.add(i);
            }
            Collections.shuffle(idx, new Random());
            List<String> l = new ArrayList<>();
            for (int k = 0; k < 5; k++) {
                SignedDocument d = docs.get(idx.get(k));
                DEQueryResult dq = client.queryDE(d.getCdc());
                guardar("consulta-" + d.getCdc() + ".xml", dq.getRawResponse());
                l.add(ids.get(idx.get(k)) + " " + dq.getResponseCode() + (simular ? "" : " digest " + (d.getDigestValue().equals(dq.getDigestValue()) ? "=" : "≠")));
            }
            fila(f, rango, "queryDE de cinco al azar", "lote " + b.getBatchNumber(), "", "", "", "", "", String.join(", ", l));
        }
    }

    /** En el ensayo, lo que contesta la consulta del lote: cada CDC de la tabla con su esperado, o «en proceso» (M12-15). */
    private void programarConsultaDeLote(String op, List<String> ids, List<ReemisionPrueba.Reemision> res) {
        if (!simular) {
            return;
        }
        if (op.equals(loteEnProceso)) {
            sim.once(SimulatedService.QUERY_BATCH, Scenario.batchInProcess());
            return;
        }
        Map<String, String> codigosLote = new LinkedHashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            codigosLote.put(res.get(i).cdc, "OP-10-4".equals(ids.get(i)) ? "1107" : Scenario.APPROVED_CODE);
        }
        sim.once(SimulatedService.QUERY_BATCH, Scenario.batchCompletedWith(codigosLote));
    }

    /**
     * Registra la respuesta definitiva (0362) de un lote <b>por el CDC de la tabla</b>: un CDC de la respuesta que no está
     * en la tabla no se registra (M12-15: la fila «—» dejaba la tabla ilegible) y un miembro sin resultado queda pendiente.
     *
     * @param docs los firmados, o {@code null} al reanudar (se leen del pedido guardado).
     * @return cuántos miembros quedaron sin resultado.
     */
    private int registrarResultadoLote(Fila f, String op, String lote, List<String> ids, List<ReemisionPrueba.Reemision> res,
            List<SignedDocument> docs, BatchQueryResult q, String rango, String dato) throws Exception {
        Map<String, SendResult> porCdc = new LinkedHashMap<>();
        for (SendResult s : q.getResults() == null ? List.<SendResult>of() : q.getResults()) {
            porCdc.put(s.getCdc(), s);
        }
        Set<String> nuestros = res.stream().map(x -> x.cdc).collect(Collectors.toSet());
        List<String> ajenos = porCdc.keySet().stream().filter(c -> !nuestros.contains(c)).toList();
        if (!ajenos.isEmpty()) {
            fallas.add(op + " la respuesta del lote trae " + ajenos.size() + " CDC que no están en la tabla (no se registran)");
        }
        int aprob = 0;
        int sinResultado = 0;
        List<String> rechazados = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            ReemisionPrueba.Reemision re = res.get(i);
            SendResult s = porCdc.get(re.cdc);
            if (s == null) {
                sinResultado++;
                rechazados.add(id + " sin resultado (pendiente)");
                fallas.add(id + " sin resultado en la respuesta del lote: queda pendiente");
                continue;
            }
            String code = s.getResponseCode();
            reemision.registrar(id, re, "ISifenClient.queryBatch:" + lote, 200, code, String.valueOf(s.getStatus()), s.getMessage());
            boolean ok = s.getStatus() == ResultStatus.APPROVED || s.getStatus() == ResultStatus.APPROVED_WITH_OBSERVATION;
            boolean esperadoOk = !"OP-10-4".equals(id);
            if (ok) {
                aprob++;
                SignedDocument d = docs != null ? docs.get(i) : leerPedido(id, re.cdc);
                if (d != null) {
                    aprobados.put(id, d);
                    firmados.put(id, d);
                }
                numeros.put(id, re.enviado);
                aprobadoEn.put(id, LocalDateTime.now());
            } else {
                rechazados.add(id + " " + code);
            }
            if (ok != esperadoOk || !esperadoOk && !"1107".equals(code)) {
                fallas.add(id + " " + code + " (esperado " + (esperadoOk ? "0260" : "1107") + ")");
            }
        }
        fila(f, rango, "resultado del lote", "lote " + lote, "acepta", "aprobados " + aprob + "/" + ids.size(), q.getResponseCode(),
                f.sifen(), dato, "registrado por el CDC de la tabla · rechazados " + rechazados + (ajenos.isEmpty() ? "" : " · CDC ajenos a la tabla: "
                + ajenos.size()));
        return sinResultado;
    }

    /**
     * Un lote enviado en una corrida anterior: nunca otro {@code sendBatch}. Si sus números ya tienen estado final (lo
     * resolvió la consulta de pendientes), se toman los aprobados; si no, sigue pendiente y la corrida se detiene.
     */
    private void loteAnterior(Fila f, String op, String lote) throws Exception {
        List<EstadoNumero> miembros = clasificarTabla().values().stream().filter(e -> e.id().matches(Pattern.quote(op) + "-\\d+")).toList();
        long pendientes = miembros.stream().filter(e -> !e.definitivo()).count();
        if (pendientes > 0) {
            fila(f, "—", "sin envío", "lote " + lote, "", "", "", "0362", "", "lote de una corrida anterior con " + pendientes + " números sin "
                    + "estado final: no se reenvía; volver a correr más tarde (la consulta de pendientes lo consulta al empezar)");
            fallas.add(op + " lote " + lote + " sin respuesta definitiva todavía: volver a consultar, no reenviar");
            detenido = true;
            return;
        }
        int aprob = 0;
        for (EstadoNumero e : miembros) {
            if (e.estado() == Estado.APROBADO) {
                aprob++;
                if (!aprobados.containsKey(e.id())) {
                    SignedDocument d = leerPedido(e.id(), e.cdc());
                    if (d != null) {
                        aprobados.put(e.id(), d);
                        firmados.put(e.id(), d);
                        aprobadoEn.put(e.id(), e.fecha());
                    }
                }
                numeros.put(e.id(), e.completo());
            }
        }
        if ("OP-10".equals(op)) {
            lote10Definitivo = true;
        }
        fila(f, "—", "sin envío", "lote " + lote, "", "aprobados " + aprob + "/" + miembros.size(), "", "", "",
                "lote de una corrida anterior, ya con estado final en la tabla: no se reenvía");
    }

    // ------------------------------------------------------------------ pendientes y estado final (E12-03)

    /**
     * Clasifica cada número de la fase por sus filas de la tabla (pura: la prueba sin red está en
     * {@code EstadosFase12Test}). Lee solo filas con un número {@code est-pun-nnnnnnn} del emisor de prueba y del rango de
     * D52; ignora cualquier otra (M12-15).
     *
     * @param lineas      líneas de la tabla de equivalencias.
     * @param estPun      establecimiento y punto del emisor de prueba ({@code 001-001}).
     * @param tienePedido si hay un pedido firmado guardado para el id y el CDC (para OP-21, su firmado de 12.3.1).
     * @return estado por número.
     */
    static Map<Integer, EstadoNumero> clasificar(List<String> lineas, String estPun, BiPredicate<String, String> tienePedido) {
        return clasificar(lineas, estPun, tienePedido, c -> false);
    }

    /**
     * Como {@link #clasificar(List, String, BiPredicate)}, con el pedido de <b>envío</b> de OP-21 (E12-06): OP-21 firmado,
     * sin registro del envío y con su {@code .pedido.xml} guardado pudo haberse enviado (corte entre el envío y la fila de
     * la tabla), así que es un pendiente que se consulta, no un «firmado sin enviar» que 12.3.12 volvería a transmitir.
     *
     * @param pedidoDeEnvio si hay un pedido de envío guardado para el CDC.
     */
    static Map<Integer, EstadoNumero> clasificar(List<String> lineas, String estPun, BiPredicate<String, String> tienePedido,
            Predicate<String> pedidoDeEnvio) {
        Map<Integer, List<String[]>> porNumero = new TreeMap<>();
        for (String l : lineas) {
            if (l.isEmpty() || l.startsWith("iditemmovimiento")) {
                continue;
            }
            String[] c = l.split(";", -1);
            if (c.length < 11 || !NUMERO_TABLA.matcher(c[2]).matches() || !c[2].startsWith(estPun + "-")) {
                continue;
            }
            int n = Integer.parseInt(c[2].substring(c[2].lastIndexOf('-') + 1));
            if (n < DESDE || n > HASTA || c[6].startsWith("RESERVADO-INUTILIZACION")) {
                continue;
            }
            porNumero.computeIfAbsent(n, k -> new ArrayList<>()).add(c);
        }
        Map<Integer, EstadoNumero> out = new TreeMap<>();
        for (Map.Entry<Integer, List<String[]>> e : porNumero.entrySet()) {
            String id = null;
            String cdc = "";
            String lote = null;
            String completo = null;
            LocalDateTime fecha = null;
            Estado estado = null;
            String motivo = "";
            String codigo = "";
            boolean aprobado = false;
            boolean inutilizado = false;
            String inutilizacion = null;
            int intentos = 0;
            for (String[] c : e.getValue()) {
                id = id == null ? c[0] : id;
                completo = completo == null ? c[2] : completo;
                cdc = cdc.isEmpty() ? c[4] : cdc;
                if (fecha == null) {
                    try {
                        fecha = LocalDateTime.parse(c[5]);
                    } catch (RuntimeException x) {
                        // fecha ilegible: queda sin fecha
                    }
                }
                String op = c[6];
                if (op.startsWith("ISifenClient.invalidateNumbers")) {
                    // E12-05: el evento de inutilización registrado trae dEstRes APPROVED; no es la aprobación del DE
                    if ("0600".equals(c[8])) {
                        inutilizado = true;
                    } else {
                        // M12-19: una inutilización con excepción (resultado desconocido) o rechazada no se repite sin aviso
                        intentos++;
                        inutilizacion = op.endsWith(":excepcion") ? "excepcion" : "rechazada " + c[8];
                    }
                    continue;
                }
                if ("APPROVED".equals(c[9]) || "APPROVED_WITH_OBSERVATION".equals(c[9])) {
                    aprobado = true;
                }
                if ("RESERVADO".equals(op)) {
                    continue;
                }
                if ("ISifenClient.sign:firmado-sin-enviar".equals(op)) {
                    estado = Estado.FIRMADO_SIN_ENVIAR;
                    motivo = "firmado sin enviar (12.3.12)";
                } else if (op.startsWith("ISifenClient.sign:")) {
                    estado = Estado.NO_ENVIADO;
                    motivo = "no enviado (" + op.substring("ISifenClient.sign:".length()) + ")";
                } else if (op.endsWith(":excepcion")) {
                    estado = Estado.PENDIENTE;
                    motivo = "excepción en " + op.substring("ISifenClient.".length(), op.length() - ":excepcion".length());
                } else if (op.startsWith("ISifenClient.sendBatch:")) {
                    lote = op.substring("ISifenClient.sendBatch:".length());
                    estado = Estado.PENDIENTE;
                    motivo = "lote " + lote + " sin respuesta definitiva";
                } else if (op.startsWith("ISifenClient.")) {
                    if (op.startsWith("ISifenClient.queryBatch:")) {
                        lote = op.substring("ISifenClient.queryBatch:".length());
                    }
                    codigo = c[8];
                    switch (c[9]) {
                        case "APPROVED", "APPROVED_WITH_OBSERVATION" -> {
                            estado = Estado.APROBADO;
                            motivo = "aprobado";
                        }
                        case "REJECTED" -> {
                            estado = Estado.RECHAZADO;
                            motivo = "rechazado " + c[8];
                        }
                        case "NOT_FOUND" -> {
                            estado = Estado.RECHAZADO;
                            motivo = c[8] + " en la consulta: no está en SIFEN";
                        }
                        default -> {
                            estado = Estado.PENDIENTE;
                            motivo = "estado «" + c[9] + "» en " + op.substring("ISifenClient.".length());
                        }
                    }
                }
            }
            if ("OP-21".equals(id) && (estado == null || estado == Estado.FIRMADO_SIN_ENVIAR) && !cdc.isEmpty() && pedidoDeEnvio.test(cdc)) {
                // E12-06: firmado y con el pedido de un envío guardado, sin la fila del envío: pudo haber llegado a SIFEN
                estado = Estado.PENDIENTE;
                motivo = "OP-21 firmado, con el pedido de un envío guardado y sin su registro: pudo enviarse (corte entre el envío y la"
                        + " fila de la tabla, E12-06)";
            }
            if (estado == null) {
                // solo la reserva: con el pedido guardado pudo haberse enviado (corte entre el envío y el registro)
                if (tienePedido.test(id, cdc)) {
                    boolean op21 = "OP-21".equals(id);
                    estado = op21 ? Estado.FIRMADO_SIN_ENVIAR : Estado.PENDIENTE;
                    motivo = op21 ? "firmado y guardado (corte antes de registrar la firma)"
                            : "reservado con el pedido guardado: pudo enviarse (corte entre el envío y el registro)";
                } else {
                    estado = Estado.NO_ENVIADO;
                    motivo = "reservado y sin pedido guardado: no se envió";
                }
            }
            if (aprobado) {
                estado = Estado.APROBADO;
                motivo = "aprobado";
            } else if (inutilizado) {
                estado = Estado.INUTILIZADO;
                motivo = "inutilizado (0600)";
            }
            out.put(e.getKey(), new EstadoNumero(e.getKey(), completo, id, cdc, lote, estado, motivo, fecha, codigo,
                    inutilizado ? null : inutilizacion, intentos));
        }
        return out;
    }

    private Map<Integer, EstadoNumero> clasificarTabla() throws Exception {
        return clasificar(Files.readAllLines(tabla, StandardCharsets.UTF_8), emisor.getEstablecimiento() + "-" + emisor.getPuntoExpedicion(),
                (id, cdc) -> !cdc.isEmpty() && Files.exists("OP-21".equals(id) ? firmadoOp21(cdc) : pedidoDe(cdc)),
                cdc -> Files.exists(pedidoDe(cdc)));
    }

    /** El último número de la fase de un id, con su estado; {@code null} si no tiene. */
    private EstadoNumero estadoDe(String id) throws Exception {
        EstadoNumero ultimo = null;
        for (EstadoNumero e : clasificarTabla().values()) {
            if (e.id().equals(id)) {
                ultimo = e;
            }
        }
        return ultimo;
    }

    /**
     * E12-03: cada número de la fase sin estado final se consulta y su resultado se registra en la tabla (por el CDC de
     * la tabla). Un lote con número, con {@code queryBatch}; lo demás, con {@code queryDE}. Nunca reenvía.
     */
    private void resolverPendientes(String momento) throws Exception {
        List<EstadoNumero> pend = clasificarTabla().values().stream().filter(e -> e.estado() == Estado.PENDIENTE).toList();
        Fila g = new Fila(0, "—", "(pendientes " + momento + ")", "—", "—", "—", "—", "—", "");
        if (pend.isEmpty()) {
            fila(g, "—", "consulta de pendientes", "", "", "", "", "", "", "ningún número de la fase sin estado final");
            return;
        }
        fila(g, "—", "consulta de pendientes", "", "", "", "", "", "", pend.size() + " números sin estado final: "
                + pend.stream().map(e -> e.completo() + " " + e.id() + " (" + e.motivo() + ")").collect(Collectors.joining(", ")));
        Map<String, List<EstadoNumero>> porLote = new LinkedHashMap<>();
        for (EstadoNumero e : pend) {
            if (e.deLote() && e.lote() != null) {
                porLote.computeIfAbsent(e.lote(), k -> new ArrayList<>()).add(e);
            } else {
                resolverPorConsulta(g, e);
            }
        }
        for (Map.Entry<String, List<EstadoNumero>> l : porLote.entrySet()) {
            resolverLote(g, l.getKey(), l.getValue());
        }
    }

    private void resolverLote(Fila g, String lote, List<EstadoNumero> miembros) throws Exception {
        String op = miembros.get(0).id().replaceAll("-\\d+$", "");
        LocalDateTime enviado = miembros.stream().map(EstadoNumero::fecha).filter(x -> x != null).min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
        if (!simular && LocalDateTime.now().isBefore(enviado.plusMinutes(10))) {
            fila(sub(g, op), "—", "sin consultar", "lote " + lote, "", "", "", "", "", "primera consulta del lote desde " + enviado.plusMinutes(10)
                    + " (Guía de la DNIT: 10 min): sigue pendiente");
            return;
        }
        List<String> ids = miembros.stream().map(EstadoNumero::id).toList();
        List<ReemisionPrueba.Reemision> res = miembros.stream().map(e -> new ReemisionPrueba.Reemision("—", e.completo(), null, e.cdc(),
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))).toList();
        programarConsultaDeLote(op, ids, res);
        pausar();
        BatchQueryResult q;
        try {
            q = client.queryBatch(lote);
        } catch (SifenException x) {
            fila(sub(g, op), "—", "queryBatch (pendiente)", "lote " + lote, "", "excepción", x.getCode(), "0362", "", "sigue pendiente");
            return;
        }
        guardar("consulta-lote-" + lote + "-pendiente.xml", q.getRawResponse());
        String rango = miembros.get(0).completo() + "…" + miembros.get(miembros.size() - 1).completo();
        if ("0362".equals(q.getResponseCode())) {
            Fila gl = new Fila(g.orden(), g.tanda(), g.caso() + " · " + op, g.vivo(), g.numero(), "0362", g.local(), g.condicion(), g.nota());
            int sin = registrarResultadoLote(gl, op, lote, ids, res, null, q, rango, "consulta de pendientes del lote");
            if ("OP-10".equals(op) && sin == 0) {
                lote10Definitivo = true;
            }
            return;
        }
        fila(sub(g, op), rango, "queryBatch (pendiente)", "lote " + lote, "", String.valueOf(q.getStatus()), q.getResponseCode(), "0362", "",
                "sin respuesta definitiva: no se reenvía");
        if (LocalDateTime.now().isAfter(enviado.plus(PLAZO_LOTE))) {
            // pasadas las 48 h la consulta del lote ya no sirve: cada CDC por su cuenta (ISifenClient#queryBatch)
            for (EstadoNumero e : miembros) {
                resolverPorConsulta(g, e);
            }
        }
    }

    /** {@code queryDE} del CDC de la tabla; 0422 aprobado, 0420 no está en SIFEN (para un miembro de lote, solo pasadas 48 h). */
    private void resolverPorConsulta(Fila g, EstadoNumero e) throws Exception {
        Fila f = sub(g, e.id());
        if (simular) {
            sim.once(SimulatedService.QUERY_DE, aprobadoEnElEnsayo(e.id()) ? Scenario.deFound() : Scenario.deNotFound());
        }
        pausar();
        DEQueryResult q;
        try {
            q = client.queryDE(e.cdc());
        } catch (SifenException x) {
            fila(f, e.completo(), "queryDE (pendiente)", e.cdc(), "", "excepción", x.getCode(), "", "", "sigue sin estado final (" + e.motivo() + ")");
            return;
        }
        guardar("consulta-pendiente-" + e.cdc() + ".xml", q.getRawResponse());
        ReemisionPrueba.Reemision re = new ReemisionPrueba.Reemision("—", e.completo(), null, e.cdc(), LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        String code = q.getResponseCode();
        if ("0422".equals(code)) {
            reemision.registrar(e.id(), re, "ISifenClient.queryDE:pendiente", 200, code, String.valueOf(ResultStatus.APPROVED), q.getMessage());
            if (!"OP-21".equals(e.id())) {
                SignedDocument d = leerPedido(e.id(), e.cdc());
                if (d != null) {
                    aprobados.put(e.id(), d);
                    firmados.put(e.id(), d);
                    numeros.put(e.id(), e.completo());
                    aprobadoEn.put(e.id(), e.fecha() != null ? e.fecha() : LocalDateTime.now());
                    if (BASES.contains(e.id())) {
                        cdcDeLaCorrida.put(cdcDelCatalogo(e.id()), d.getCdc());
                    }
                }
            }
            fila(f, e.completo(), "queryDE (pendiente)", e.cdc(), "", String.valueOf(q.getStatus()), code, "", "",
                    "estaba: " + e.motivo() + " · aprobado en SIFEN: registrado (no se reenvía ni se inutiliza)");
            return;
        }
        if ("0420".equals(code)) {
            if (e.deLote() && (e.fecha() == null || LocalDateTime.now().isBefore(e.fecha().plus(PLAZO_LOTE)))) {
                fila(f, e.completo(), "queryDE (pendiente)", e.cdc(), "", String.valueOf(q.getStatus()), code, "", "",
                        "0420 de un número de lote: no es definitivo mientras el lote pueda estar en proceso (48 h): sigue pendiente");
                return;
            }
            reemision.registrar(e.id(), re, "ISifenClient.queryDE:pendiente", 200, code, String.valueOf(ResultStatus.NOT_FOUND), q.getMessage());
            fila(f, e.completo(), "queryDE (pendiente)", e.cdc(), "", String.valueOf(q.getStatus()), code, "", "",
                    "estaba: " + e.motivo() + " · no está en SIFEN (rechazado o no recibido): estado final, va a 12.3.11");
            return;
        }
        fila(f, e.completo(), "queryDE (pendiente)", e.cdc(), "", String.valueOf(q.getStatus()), code, "", "", "sigue sin estado final");
    }

    /** En el ensayo, si el esperado del id aprueba (lo que contestaría SIFEN a la consulta de un pendiente). */
    private boolean aprobadoEnElEnsayo(String id) {
        String e;
        if (simulados.containsKey(id)) {
            e = simulados.get(id);
        } else if (id.matches("OP-1[01]-\\d+")) {
            e = "OP-10-4".equals(id) ? "1107" : "0260";
        } else if (id.startsWith("OP-3")) {
            e = "OP-32-NC".equals(id) ? "2404" : "0260";
        } else if ("DE-41-FE".equals(id)) {
            e = "0260";
        } else if (porCaso.containsKey(id)) {
            e = porCaso.get(id).sifen();
        } else {
            e = esperados.containsKey(id) ? esperados.get(id).sifen() : "?";
        }
        e = e.endsWith("?") ? e.substring(0, e.length() - 1) : e;
        return e.isEmpty() || "-".equals(e) || "0260".equals(e) || "1005".equals(e);
    }

    // ------------------------------------------------------------------ 12.3.11: inutilizaciones

    /**
     * Cada número de la fase con un rechazo definitivo o reservado sin enviar, un evento por número y con su tipo de
     * documento cotejado contra el CDC de la tabla (D55). Antes, la consulta de pendientes; un número que sigue sin estado
     * final no se inutiliza y hace fallar la tanda (E12-03). El OP-21 firmado y sin enviar queda para 12.3.12 (D56), salvo
     * al cerrar ({@code -Djbs.sifen.test.f12.op21.inutilizar=true}). Respeta el corte por tres rechazos seguidos.
     */
    private void inutilizaciones(Fila f) throws Exception {
        resolverPendientes("antes de 12.3.11");
        boolean cerrarOp21 = Boolean.getBoolean(P + "op21.inutilizar");
        List<String> hechos = new ArrayList<>();
        List<String> rechazadas = new ArrayList<>();
        List<String> desconocidas = new ArrayList<>();
        List<String> frenadas = new ArrayList<>();
        List<String> pendientes = new ArrayList<>();
        for (EstadoNumero en : clasificarTabla().values()) {
            if (detenido) {
                fila(f, "—", "corte", "", "", "", "", "", "", "12.3.11 se detiene: " + (motivoDetencion != null ? motivoDetencion : "la tanda quedó detenida") + " (§12.3)");
                break;
            }
            String num = String.format("%07d", en.numero());
            if (en.estado() == Estado.APROBADO || en.estado() == Estado.INUTILIZADO) {
                continue;
            }
            if (en.estado() == Estado.PENDIENTE) {
                fila(f, en.completo(), "sin inutilizar", en.cdc(), "", "PENDIENTE", "", "", "", en.id() + ": sin estado final (" + en.motivo()
                        + "): no se inutiliza (D55)");
                pendientes.add(num + " (" + en.id() + ")");
                continue;
            }
            if (en.estado() == Estado.FIRMADO_SIN_ENVIAR && !cerrarOp21) {
                fila(f, en.completo(), "sin inutilizar", en.cdc(), "", "FIRMADO_SIN_ENVIAR", "", "", "", en.id()
                        + ": firmado sin enviar, se envía en 12.3.12 (D56, no bloquea); al cerrar sin enviarlo, -D" + P + "op21.inutilizar=true");
                continue;
            }
            String idInu = "I-" + num.substring(3);
            if (en.inutilizacion() != null) {
                // M12-19: una inutilización anterior con excepción o rechazada no se repite sin aviso
                String antes = "excepcion".equals(en.inutilizacion()) ? "con excepción (resultado desconocido: pudo quedar registrada)"
                        : en.inutilizacion();
                boolean autorizada = reintentar.contains(idInu) && en.intentosInutilizacion() == 1 && reintentados.add(idInu);
                if (!autorizada) {
                    fila(f, en.completo(), "sin inutilizar", en.cdc(), "", String.valueOf(en.estado()), "", "0600", "", en.id()
                            + ": la inutilización anterior quedó " + antes + "; no se repite sin confirmarla (punto 4 de la regla: cotejar y "
                            + "registrar a mano) o autorizarla una vez con -D" + P + "reintentar=" + idInu + " (M12-19)"
                            + (en.intentosInutilizacion() > 1 ? "; ya se repitió una vez" : ""));
                    frenadas.add(num + " (" + en.id() + ", " + antes + ")");
                    continue;
                }
                fila(f, en.completo(), "reintento autorizado", en.cdc(), "", "", "", "0600", "", "-D" + P + "reintentar=" + idInu
                        + ": la inutilización anterior quedó " + antes + "; se repite una sola vez (M12-19)");
            }
            TTiDE tipo = TTiDE.getByVal(Short.parseShort(en.cdc().substring(0, 2)));
            SignedDocument firmado = firmados.get(en.id());
            if (firmado != null && firmado.getCdc().equals(en.cdc())) {
                Assertions.assertEquals(firmado.getDocumentType(), tipo, en.id() + ": el tipo de la inutilización es el del documento firmado");
            }
            NumberRange rango = NumberRange.builder().timbrado(String.valueOf(emisor.getTimbrado())).establishment(emisor.getEstablecimiento())
                    .issuePoint(emisor.getPuntoExpedicion()).documentType(tipo).from(num).to(num)
                    .reason(MOTIVO_INUTILIZACION + " - numero no aprobado").build();
            Fila g = new Fila(f.orden(), f.tanda(), "I-" + num.substring(3) + " (" + en.id() + ")", f.vivo(), f.numero(), "0600", f.local(),
                    f.condicion(), "");
            ReemisionPrueba.Reemision re = new ReemisionPrueba.Reemision("—", en.completo(), null, "", LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
            String id = en.id();
            boolean[] registrado = {false};
            EventResult ev = evento(g, "I-" + num.substring(3), null, "tipo " + tipo.getVal() + " del CDC de la tabla (" + en.id() + ", "
                    + en.motivo() + "), desde = hasta", "0600", () -> {
                        EventResult r = client.invalidateNumbers(rango);
                        // E12-03: primero la fila de la tabla (el 0600 decide el estado del número), después la respuesta cruda
                        reemision.registrar(id, re, "ISifenClient.invalidateNumbers:Id " + r.getEventId() + " tipo " + tipo.getVal(), 200,
                                codigo(r), String.valueOf(r.getStatus()), mensaje(r));
                        registrado[0] = true;
                        return r;
                    }, en.completo(), en.cdc());
            if (ev == null && !registrado[0]) {
                reemision.registrar(id, re, "ISifenClient.invalidateNumbers:excepcion", 0, null, null, null);
            }
            String intento = num + ":" + tipo.getVal() + " (" + en.id() + ")";
            if (ev == null) {
                desconocidas.add(intento);
            } else if ("0600".equals(codigo(ev))) {
                hechos.add(intento);
            } else {
                rechazadas.add(intento + " " + codigo(ev));
            }
        }
        // I12-19: inutilizados son solo los 0600; los intentos rechazados o con excepción se cuentan aparte
        fila(f, "—", "resumen 12.3.11", "", "", "", "", "0600", "números inutilizados (0600): " + hechos.size() + " · rechazados: "
                + rechazadas.size() + " · con excepción: " + desconocidas.size() + " · frenados (M12-19): " + frenadas.size()
                + " · sin estado final: " + pendientes.size(), "inutilizados " + hechos + (rechazadas.isEmpty() ? "" : " · rechazados " + rechazadas)
                + (desconocidas.isEmpty() ? "" : " · con excepción " + desconocidas) + (frenadas.isEmpty() ? "" : " · frenados " + frenadas)
                + (pendientes.isEmpty() ? "" : " · pendientes " + pendientes));
        if (!pendientes.isEmpty()) {
            fallas.add("12.3.11: números sin estado final, no inutilizados: " + pendientes + " (consultar antes de inutilizar, D55)");
        }
        if (!frenadas.isEmpty()) {
            fallas.add("12.3.11: inutilizaciones anteriores sin 0600, no repetidas: " + frenadas + " (confirmar o autorizar, M12-19)");
        }
    }

    // ------------------------------------------------------------------ eventos: llamada común

    @FunctionalInterface
    private interface Llamada<T> {
        T call() throws Exception;
    }

    private EventResult evento(Fila f, String id, SignedDocument d, String dato, Llamada<EventResult> llamada) throws Exception {
        return evento(f, id, d, dato, f.sifen(), llamada, d == null ? "—" : numeroDe(d.getCdc()), d == null ? "" : d.getCdc());
    }

    private EventResult evento(Fila f, String id, SignedDocument d, String dato, Llamada<EventResult> llamada, String numero, String cdc)
            throws Exception {
        return evento(f, id, d, dato, f.sifen(), llamada, numero, cdc);
    }

    private EventResult evento(Fila f, String id, SignedDocument d, String dato, String esperado, Llamada<EventResult> llamada, String numero,
            String cdc) throws Exception {
        f = id.equals(f.caso()) || f.caso().startsWith("I-") ? f : sub(f, id);
        if (eventosAntes.containsKey(id)) {
            String no = motivoParaNoRepetirEvento(id);
            if (no != null) {
                fila(f, numero, "sin envío", cdc, "", "", eventosAntes.get(id), esperado, dato, no);
                return null;
            }
            fila(f, numero, "reintento autorizado", cdc, "", "", eventosAntes.get(id), esperado, dato,
                    "-D" + P + "reintentar: el anterior no quedó registrado (" + eventosAntes.get(id) + "); se repite una sola vez");
        }
        String simulado = simulado(id, esperado);
        programar(SimulatedService.EVENT, simulado, true);
        pausar();
        EventResult res;
        try {
            if (NO_ENVIADO.equals(simulado)) {
                throw SifenException.of(ErrorCode.JBS_CONEX_01, "ensayo: excepción de transporte antes de enviar (M12-18)", null);
            }
            res = llamada.call();
        } catch (SifenValidationException x) {
            fila(f, numero, "evento", cdc, "rechaza local " + codigos(x.getErrors()), "", x.getCode(), esperado, dato, "sin contactar a SIFEN");
            fallas.add(id + " rechazo local " + codigos(x.getErrors()));
            return null;
        } catch (SifenException x) {
            fila(f, numero, "evento", cdc, "", "excepción", x.getCode(), esperado, dato, "resultado desconocido: " + x.isOutcomeUnknown());
            // M12-18: una excepción de transporte detiene la tanda (con resultado desconocido, además, el evento no se repite)
            detener(id + " " + x.getCode() + ": excepción en el evento (resultado desconocido: " + x.isOutcomeUnknown()
                    + "); la tanda se detiene (M12-18)");
            return null;
        }
        eventos.put(id, codigo(res));
        coincide(f, id, codigo(res), esperado);
        // M12-19: primero la fila (lo que dice que el evento ya se envió) y después las dos respuestas crudas
        fila(f, numero, "evento", (cdc.isEmpty() ? "" : cdc + " · ") + "Id " + res.getEventId(), "acepta", String.valueOf(res.getStatus()),
                codigo(res), esperado, dato, limpio(mensaje(res)));
        guardar("evento-" + id.replace(' ', '_') + "-" + res.getEventId() + ".xml", res.getRawResponse());
        guardar("evento-" + id.replace(' ', '_') + "-" + res.getEventId() + ".pedido.xml", res.getEventXml());
        return res;
    }

    /** En el ensayo, lo que contesta el simulador a {@code id}: el de {@code simular.resultados} o el esperado de la fila. */
    private String simulado(String id, String esperado) {
        return simular && simulados.containsKey(id) ? simulados.get(id) : esperado;
    }

    /** La fila de un documento o evento que es parte de un caso (OP-30-FE de OP-30, el plan B de DE-41). */
    private static Fila sub(Fila f, String id) {
        return id.equals(f.caso()) ? f : new Fila(f.orden(), f.tanda(), f.caso() + " · " + id, f.vivo(), f.numero(), f.sifen(), f.local(),
                f.condicion(), f.nota());
    }

    private static String codigo(EventResult r) {
        if (r.getMessages() != null && !r.getMessages().isEmpty()) {
            return r.getMessages().get(0).getCode();
        }
        return r.getResponseCode();
    }

    private static String mensaje(EventResult r) {
        return r.getMessages() == null || r.getMessages().isEmpty() ? String.valueOf(r.getMessage()) : r.getMessages().get(0).getText();
    }

    // ------------------------------------------------------------------ reserva, simulador y clientes

    /** Guarda de rango (D52 (4)) y reserva; en el ensayo, el número tiene que ser el estimado de la lista. */
    private ReemisionPrueba.Reemision reservar(String id, DocumentoElectronico de, LocalDateTime fecha) throws Exception {
        guardaDeRango(id);
        int n = reemision.getSiguiente();
        String clave = clave(id);
        List<Integer> est = estimados.get(clave);
        // ciclo 3: un caso con reintento tiene dos estimados; el índice sigue desde los números que ya tenía en la tabla
        int k = antesPorClave.getOrDefault(clave, 0) + usados.merge(clave, 1, Integer::sum) - 1;
        reservadosEnLaCorrida.add(id);
        if (est != null && k < est.size() && est.get(k) != n) {
            desvios.add(id + ": " + n + " en vez de " + est.get(k));
        }
        List<TgCamDEAsoc> asoc = de.getgCamDEAsocList();
        de.setgCamDEAsocList(null);   // los CDC asociados ya son los de la corrida: la reemisión no los remapea
        ReemisionPrueba.Reemision re;
        try {
            re = reemision.reemitir(id, de, fecha);
        } finally {
            de.setgCamDEAsocList(asoc);
        }
        return re;
    }

    /** Clave del estimado de un id: OP-10/OP-11 y OP-30..OP-32 por operación; el resto, el id. */
    static String clave(String id) {
        return id.matches("OP-1[01]-\\d+") || id.startsWith("OP-3") ? id.substring(0, 5) : id;
    }

    private void guardaDeRango(String id) {
        int n = reemision.getSiguiente();
        Assertions.assertTrue(n >= DESDE && n <= HASTA, "D52 (4): el siguiente número libre (" + n + ") no está entre " + DESDE + " y " + HASTA
                + " (" + id + "): se detiene antes de reservar");
    }

    /** En el ensayo, el simulador contesta lo esperado de la fila (0260 aprueba, 1005 con observación, otro código rechaza). */
    private void programar(SimulatedService servicio, String esperado, boolean evento) {
        if (!simular || esperado == null || NO_ENVIADO.equals(esperado)) {
            return;
        }
        String e = esperado.endsWith("?") ? esperado.substring(0, esperado.length() - 1) : esperado;
        if (e.contains("+") && evento) {
            e = e.split("\\+")[0];   // un evento con dos códigos coherentes (OP-04): el ensayo contesta el primero
        }
        if (e.isEmpty() || "-".equals(e) || "0260".equals(e) || "0600".equals(e) || e.contains("+")) {
            return;
        }
        if ("1005".equals(e)) {
            sim.once(servicio, Scenario.approvedWithObservation());
        } else if (evento) {
            sim.once(servicio, Scenario.eventRejected(e, "Rechazo simulado (ensayo en seco de la Fase 12)"));
        } else {
            sim.once(servicio, Scenario.rejected(e, "Rechazo simulado (ensayo en seco de la Fase 12)"));
        }
    }

    private void coincide(Fila f, String id, String obtenido, String esperado) {
        boolean ok = coincide(esperado, obtenido);
        if (ok) {
            rechazosSeguidos = 0;
            return;
        }
        rechazosSeguidos++;
        if (!esperado.endsWith("?")) {
            fallas.add(id + " " + obtenido + " (esperado " + esperado + ")");
        }
        if (rechazosSeguidos >= 3) {
            detener("tres resultados seguidos distintos de lo esperado: se detiene la tanda y se escala (§12.3)");
        }
    }

    static boolean coincide(String esperado, String obtenido) {
        if (esperado == null || esperado.isEmpty() || "-".equals(esperado) || "?".equals(esperado)) {
            return true;
        }
        if (esperado.contains("+")) {
            return Arrays.asList(esperado.split("\\+")).contains(obtenido);   // lote: 0300 al enviar, 0362 al consultar
        }
        String e = esperado.endsWith("?") ? esperado.substring(0, esperado.length() - 1) : esperado;
        return e.equals(obtenido);
    }

    private SifenConfig configuracion(boolean validar, boolean lectura) throws Exception {
        SifenConfig cfg = SifenTestEnv.entornoOSaltar().getConfiguracion();
        Assertions.assertEquals(SifenConfig.Environment.DEV, cfg.getEnvironment(), "D28");
        if (simular) {
            if (sim == null) {
                sim = SifenSimulator.start();
            }
            cfg.setBaseUrl(sim.getBaseUrl());
            Assertions.assertTrue(cfg.getBaseUrl().startsWith("https://127.0.0.1:"), "el ensayo va solo contra el simulador local");
        }
        cfg.setPreValidationEnabled(validar);
        if (lectura) {
            cfg.setHttpReadTimeout(Integer.getInteger(P + "lectura.ms", 180_000));
        }
        return cfg;
    }

    private ISifenClient cliente(boolean sinValidacion, boolean lectura) throws Exception {
        if (!sinValidacion && !lectura) {
            return client;
        }
        if (sinValidacion && lectura) {
            if (crudoLento == null) {
                crudoLento = SifenClient.create(configuracion(false, true));
            }
            return crudoLento;
        }
        if (sinValidacion) {
            if (crudo == null) {
                crudo = SifenClient.create(configuracion(false, false));
            }
            return crudo;
        }
        if (lento == null) {
            lento = SifenClient.create(configuracion(true, true));
        }
        return lento;
    }

    private static boolean lento(String id) {
        return "DE-55".equals(id) || "DE-56".equals(id) || "DE-56x".equals(id) || SONDA_56.equals(id);
    }

    // ------------------------------------------------------------------ lista, tabla y reanudación

    static List<Fila> leerLista(Path archivo) throws Exception {
        List<Fila> l = new ArrayList<>();
        for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
            if (linea.isBlank() || linea.startsWith("orden\t")) {
                continue;
            }
            String[] c = linea.split("\t", -1);
            Assertions.assertEquals(9, c.length, "envios_12_3.tsv: 9 columnas en «" + c[0] + "»");
            l.add(new Fila(Integer.parseInt(c[0]), c[1], c[2], c[3], c[4], c[5], c[6], c[7], c[8]));
        }
        return l;
    }

    /** Cada caso de la lista tiene su fila en los esperados y su manejo; arma los números estimados. */
    private void verificarLista(List<Fila> filas) {
        Set<String> especiales = Set.of("(inutilizaciones)");
        for (Fila f : filas) {
            if (!especiales.contains(f.caso())) {
                Assertions.assertTrue(esperados.containsKey(f.caso()), "sin fila en esperados.tsv: " + f.caso());
            }
            if (f.numero().matches("\\d{7}(–\\d{7})?")) {
                String[] p = f.numero().split("–");
                int a = Integer.parseInt(p[0]);
                int b = p.length > 1 ? Integer.parseInt(p[1]) : a;
                List<Integer> l = new ArrayList<>();
                for (int n = a; n <= b; n++) {
                    l.add(n);
                }
                Assertions.assertTrue(a >= DESDE && b <= HASTA, "D52: " + f.caso() + " fuera del rango");
                // ciclo 3: un caso con fila de reintento suma su segundo estimado (en el orden de la lista)
                estimados.computeIfAbsent(f.caso(), k -> new ArrayList<>()).addAll(l);
            }
        }
        if (planB41) {
            estimados.clear();   // el plan B corre todo lo que sigue: los estimados dejan de valer
        }
    }

    /**
     * Reanudación ({@code -Djbs.sifen.test.f12.previos=todos|<casos>}): cada caso de la fase aprobado en una corrida
     * anterior se toma de la tabla (número y CDC) y de su pedido guardado con {@code readSigned}. Nada se refirma ni se
     * renumera. La hora de aprobación es el {@code dFecProc} de la respuesta guardada si está (M12-16); si no, la fecha de
     * emisión de la tabla.
     */
    private void cargarPrevios() throws Exception {
        String previos = System.getProperty(P + "previos");
        if (previos == null || previos.isBlank()) {
            return;
        }
        Assertions.assertTrue(!simular || System.getProperty(P + "simular.tabla") != null,
                "en el ensayo, la reanudación solo sobre la copia de -D" + P + "simular.tabla");
        Set<String> pedidos = "todos".equals(previos) ? null : conjunto(previos);
        Map<String, String[]> ok = new LinkedHashMap<>();
        for (String l : Files.readAllLines(tabla, StandardCharsets.UTF_8)) {
            String[] c = l.split(";", -1);
            if (c.length < 11 || !NUMERO_TABLA.matcher(c[2]).matches()) {
                continue;
            }
            int n = Integer.parseInt(c[2].substring(c[2].lastIndexOf('-') + 1));
            // E12-05: la fila del evento de inutilización trae APPROVED (el del evento) y no es una aprobación del DE
            boolean aprob = ("APPROVED".equals(c[9]) || "APPROVED_WITH_OBSERVATION".equals(c[9]))
                    && !c[6].startsWith("ISifenClient.invalidateNumbers");
            if (n >= DESDE && n <= HASTA && aprob && !c[4].isEmpty() && !"OP-21".equals(c[0]) && (pedidos == null || pedidos.contains(c[0]))) {
                ok.put(c[0], c);
            }
        }
        for (Map.Entry<String, String[]> e : ok.entrySet()) {
            String[] c = e.getValue();
            SignedDocument d = leerPedido(e.getKey(), c[4]);
            Assertions.assertNotNull(d, e.getKey() + ": aprobado sin su pedido guardado");
            aprobados.put(e.getKey(), d);
            firmados.put(e.getKey(), d);
            numeros.put(e.getKey(), c[2]);
            aprobadoEn.put(e.getKey(), horaDeAprobacion(c[4], c[5]));
            if (BASES.contains(e.getKey())) {
                cdcDeLaCorrida.put(cdcDelCatalogo(e.getKey()), d.getCdc());
            }
            if (e.getKey().startsWith("OP-10-")) {
                lote10Definitivo = true;
            }
        }
        fila(new Fila(0, "—", "(previos)", "—", "—", "—", "—", "—", ""), "—", "readSigned", "", "", "", "", "", "",
                "aprobados de corridas anteriores: " + ok.keySet());
    }

    /** {@code dFecProc} de la respuesta cruda guardada del CDC (en vivo); si no está, la fecha de la tabla. */
    private LocalDateTime horaDeAprobacion(String cdc, String fechaTabla) {
        Path r = dir.resolve("respuestas/" + cdc + ".xml");
        if (!simular && Files.exists(r)) {
            try {
                String fp = XmlCanonico.texto(Files.readString(r, StandardCharsets.UTF_8), "dFecProc");
                if (fp != null && !fp.isBlank()) {
                    return OffsetDateTime.parse(fp.strip()).toLocalDateTime();
                }
            } catch (Exception x) {
                // respuesta ilegible: la fecha de la tabla
            }
        }
        return LocalDateTime.parse(fechaTabla);
    }

    /**
     * Lo hecho en corridas anteriores, para no repetirlo al reanudar: los ids con un número de la fase en la tabla, los
     * lotes enviados y los eventos de {@code resultados.tsv}. En el ensayo la tabla es una copia sin números de la fase
     * y no hay {@code resultados.tsv} propio: no cambia nada.
     */
    private void cargarLoHechoAntes() throws Exception {
        for (String l : Files.readAllLines(tabla, StandardCharsets.UTF_8)) {
            String[] c = l.split(";", -1);
            if (c.length < 11 || !NUMERO_TABLA.matcher(c[2]).matches()) {
                continue;
            }
            int n = Integer.parseInt(c[2].substring(c[2].lastIndexOf('-') + 1));
            if (n < DESDE || n > HASTA || c[6].startsWith("RESERVADO-INUTILIZACION") || c[6].startsWith("ISifenClient.invalidateNumbers")) {
                continue;
            }
            if ("RESERVADO".equals(c[6])) {
                reservadosAntes.add(c[0]);
                antesPorClave.merge(clave(c[0]), 1, Integer::sum);
            }
            if (c[6].startsWith("ISifenClient.sendBatch:") && !c[6].endsWith(":excepcion")) {
                lotesAntes.putIfAbsent(c[0].replaceAll("-\\d+$", ""), c[6].substring("ISifenClient.sendBatch:".length()));
            }
        }
        Path previos = res;
        if ((!simular || System.getProperty(P + "simular.tabla") != null) && Files.exists(previos)) {
            for (String l : Files.readAllLines(previos, StandardCharsets.UTF_8)) {
                String[] c = l.split("\t", -1);
                if (c.length > 10 && "evento".equals(c[6]) && !c[10].isEmpty() && !c[3].startsWith("I-")) {
                    String id = c[3].contains(" · ") ? c[3].substring(c[3].lastIndexOf(" · ") + 3) : c[3];
                    eventosAntes.put(id, c[10]);
                    eventos.put(id, c[10]);
                    enviosDeEvento.merge(id, 1, Integer::sum);
                    statusDeEvento.put(id, c[9]);
                }
            }
        }
        if ("0143".equals(eventos.get("EV-07")) || "4006".equals(eventos.get("EV-07"))) {
            receptorSuspendido = true;
        }
        if (!reservadosAntes.isEmpty() || !eventosAntes.isEmpty()) {
            fila(new Fila(0, "—", "(reanudación)", "—", "—", "—", "—", "—", ""), "—", "tabla y resultados.tsv", "", "", "", "", "", "",
                    "con número de antes: " + new TreeSet<>(reservadosAntes) + " · lotes: " + lotesAntes + " · eventos: " + eventosAntes.keySet());
        }
    }

    /**
     * {@code true} si el id ya tiene un número de la fase de una corrida anterior y no se autorizó reintentarlo (M12-14).
     * El reintento ({@code -Djbs.sifen.test.f12.reintentar}) se niega si el número anterior está aprobado o sin estado
     * final, si el id ya se reintentó una vez, si es de un lote o si se pasaría el tope de la fase.
     */
    private boolean yaReservado(Fila f, String id) throws Exception {
        if (!reservadosAntes.contains(id) && !reservadosEnLaCorrida.contains(id)) {
            return false;
        }
        if (!reintentar.contains(id)) {
            return true;
        }
        if (conFilaDeReintento.contains(id) && !TANDA_REINTENTOS.equals(f.tanda())) {
            // ciclo 3: el reintento de un caso con fila propia va solo en esa fila (antes de 12.3.10)
            fila(sub(f, id), "—", "reintento en su fila", "", "", "", "", "", "", "el reintento de " + id + " va en la tanda «"
                    + TANDA_REINTENTOS + "» de la lista, no en esta");
            return true;
        }
        if (reintentados.contains(id)) {
            // un reintento por id y por corrida: un segundo pedido no reserva otro número (antes devolvía false y reservaba)
            fila(sub(f, id), "—", "reintento negado", "", "", "", "", "", "", "ya se reintentó en esta corrida");
            return true;
        }
        String negado = motivoParaNoReintentar(id);
        if (negado != null) {
            fila(sub(f, id), "—", "reintento negado", "", "", "", "", "", "", negado);
            fallas.add(id + " reintento negado: " + negado);
            return true;
        }
        reintentados.add(id);
        fila(sub(f, id), "—", "reintento autorizado", "", "", "", "", "", "", "-D" + P + "reintentar: una vez, con otro número; el anterior "
                + "va a 12.3.11");
        return false;
    }

    private String motivoParaNoReintentar(String id) throws Exception {
        if (id.matches("OP-1[01]-\\d+")) {
            return "un número de lote no se reintenta";
        }
        Map<String, Integer> porId = new LinkedHashMap<>();
        EstadoNumero ultimo = null;
        for (EstadoNumero e : clasificarTabla().values()) {
            porId.merge(e.id(), 1, Integer::sum);
            if (e.id().equals(id)) {
                ultimo = e;
            }
        }
        if (ultimo == null) {
            return null;
        }
        if (porId.get(id) > 1) {
            return "ya se reintentó una vez (" + porId.get(id) + " números)";
        }
        if (ultimo.estado() == Estado.APROBADO) {
            return "el número anterior (" + ultimo.completo() + ") está aprobado";
        }
        if (!ultimo.definitivo()) {
            return "el número anterior (" + ultimo.completo() + ") no tiene estado final: " + ultimo.motivo();
        }
        // ciclo 3: los reintentos de esta corrida ya tienen su número en la tabla (porId los cuenta); sumar reintentados los
        // contaba dos veces (y los eventos no gastan número)
        int hechos = porId.values().stream().mapToInt(k -> k - 1).sum();
        if (hechos + 1 > TOPE_REINTENTOS) {
            return "se pasaría el tope de " + TOPE_REINTENTOS + " reintentos de la fase (" + hechos + " hechos)";
        }
        return null;
    }

    private String motivoParaNoRepetirEvento(String id) {
        if (!reintentar.contains(id)) {
            return "ya enviado en una corrida anterior (resultados.tsv): no se repite; para repetirlo una vez, -D" + P + "reintentar=" + id;
        }
        if (reintentados.contains(id) || enviosDeEvento.getOrDefault(id, 0) > 1) {
            return "ya se repitió una vez";
        }
        if ("0600".equals(eventosAntes.get(id))) {
            return "el anterior quedó registrado (0600): no se repite";
        }
        if ("excepción".equals(statusDeEvento.get(id))) {
            return "el anterior tiene resultado desconocido: no se repite (D55)";
        }
        reintentados.add(id);
        return null;
    }

    /**
     * M12-14 e I12-16, antes de nada: el ensayo no lleva la llave de vivo; en vivo hacen falta la llave
     * ({@code -Djbs.sifen.test.f12.vivo=SIFEN-test}), la tanda, ninguna propiedad del ensayo y, para 12.3.9 o 12.3.10, la
     * constancia de MKSIF no vacía; un lote no se reintenta. La prueba sin red está en {@code EstadosFase12Test}.
     *
     * @param p propiedades del sistema.
     */
    static void verificarModo(Properties p) {
        boolean ensayo = Boolean.parseBoolean(p.getProperty(P + "simular"));
        String vivo = p.getProperty(P + "vivo");
        String mksif = p.getProperty(P + "mksif");
        if (mksif != null) {
            Assertions.assertFalse(mksif.isBlank(), "I12-16: -D" + P + "mksif vacío no es una constancia de que MKSIF no emite");
        }
        for (String id : conjunto(p.getProperty(P + "reintentar"))) {
            Assertions.assertFalse(id.matches("OP-1[01](-\\d+)?"), "M12-14: un lote no se reintenta (" + id + ")");
        }
        if (ensayo) {
            Assertions.assertNull(vivo, "M12-14: el ensayo en seco no lleva la llave del modo en vivo (-D" + P + "vivo)");
            return;
        }
        Assertions.assertEquals(LLAVE_VIVO, vivo, "M12-14: sin -D" + P + "vivo=" + LLAVE_VIVO + " no hay corrida en vivo (y sin -D" + P
                + "simular=true no hay ensayo)");
        for (String k : SOLO_ENSAYO) {
            Assertions.assertNull(p.getProperty(P + k), "-D" + P + k + " solo vale en el ensayo en seco");
        }
        Set<String> tandas = conjunto(p.getProperty(P + "tandas"));
        Assertions.assertFalse(tandas.isEmpty(), "en vivo se elige la tanda (-D" + P + "tandas): la batería no se corre de una vez");
        for (String t : tandas) {
            if (TANDAS_MKSIF.contains(t)) {
                Assertions.assertNotNull(mksif, "D52 (7), I12-16: la tanda " + t + " no se corre en vivo sin -D" + P
                        + "mksif=<constancia de que MKSIF no emite>");
            }
        }
    }

    /**
     * D52 (7), I12-16: al empezar 12.3.9 o 12.3.10 la constancia de MKSIF queda escrita en una fila de la tabla de
     * resultados; en vivo, sin ella, la fila no se corre.
     *
     * @return {@code true} si la fila se puede correr.
     */
    private boolean constanciaDeMksif(Fila f) throws Exception {
        if (!TANDAS_MKSIF.contains(f.tanda())) {
            return true;
        }
        Fila c = new Fila(f.orden(), f.tanda(), "(constancia MKSIF)", "—", "—", "—", "—", "—", "");
        if (constanciaMksif == null) {
            if (simular) {
                if (constanciaEscrita.add(f.tanda())) {
                    fila(c, "—", "constancia de MKSIF (D52 (7))", "", "", "", "", "", "ensayo en seco: sin constancia (en vivo es obligatoria)", "");
                }
                return true;
            }
            fila(f, "—", "sin envío", "", "", "", "", f.sifen(), "", "falta -D" + P + "mksif: la tanda " + f.tanda() + " no se corre en vivo (D52 (7))");
            fallas.add(f.caso() + " sin la constancia de MKSIF");
            return false;
        }
        if (constanciaEscrita.add(f.tanda())) {
            fila(c, "—", "constancia de MKSIF (D52 (7))", "", "", "", "", "", "MKSIF no emite: " + constanciaMksif,
                    "escrita al empezar la tanda " + f.tanda());
        }
        return true;
    }

    /**
     * Validación local y condición del gemelo de un documento sin catálogo de esperados (OP-21, lotes): se espera que
     * acepte sin avisos (M12-15).
     *
     * @return {@code null} si cumple; si no, el motivo.
     */
    private String problemaDelGemelo(String id, DocumentoElectronico gemelo) throws Exception {
        try {
            condicion(id, gemelo, true);
        } catch (AssertionFailedError | IllegalStateException x) {
            return "condición: " + limpio(x.getMessage());
        }
        ValidationResult v = client.validateDE(gemelo);
        if (!v.getErrors().isEmpty() || !v.getWarnings().isEmpty()) {
            return "validación local del gemelo: " + local(v) + " (se espera «acepta», sin avisos)";
        }
        return null;
    }

    /** Pedido firmado guardado de un CDC (fuera de git). */
    private Path pedidoDe(String cdc) {
        return dir.resolve((simular ? "simulacion/" : "respuestas/") + cdc + ".pedido.xml");
    }

    /** El pedido guardado leído con {@code readSigned} (sin refirmar); {@code null} si no está. */
    private SignedDocument leerPedido(String id, String cdc) throws Exception {
        Path p = "OP-21".equals(id) ? firmadoOp21(cdc) : pedidoDe(cdc);
        if (!Files.exists(p)) {
            return null;
        }
        SignedDocument d = client.readSigned(Files.readString(p, StandardCharsets.UTF_8));
        Assertions.assertTrue(cdc.equals(d.getCdc()), id + ": el pedido guardado no es el del CDC de la tabla");
        return d;
    }

    /** CDC aprobado (0260) de un número de la tabla. */
    private String cdcDeLaTabla(String numero) {
        try {
            String cdc = null;
            for (String l : Files.readAllLines(tabla, StandardCharsets.UTF_8)) {
                String[] c = l.split(";", -1);
                if (c.length > 8 && c[2].equals(numero) && "0260".equals(c[8]) && !c[4].isEmpty()) {
                    cdc = c[4];
                }
            }
            return cdc;
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private String numeroDe(String cdc) {
        for (Map.Entry<String, SignedDocument> e : firmados.entrySet()) {
            if (e.getValue().getCdc().equals(cdc)) {
                return numeros.get(e.getKey());
            }
        }
        try {
            for (String l : Files.readAllLines(tabla, StandardCharsets.UTF_8)) {
                String[] c = l.split(";", -1);
                if (c.length > 4 && c[4].equals(cdc)) {
                    return c[2];
                }
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        return "—";
    }

    private String cdcDelCatalogo(String id) {
        try {
            if (id.startsWith("OP-3")) {
                return CasosFase12.cancelacionConNotas().get(id).get().obtenerCDC();
            }
            return esperados.get(id).documento().obtenerCDC();
        } catch (SifenException e) {
            throw new IllegalStateException(e);
        }
    }

    // ------------------------------------------------------------------ sin red (EstadosFase12Test, M12-19)

    /**
     * Sin red: una instancia lista para correr 12.3.11 contra el simulador, sobre la tabla y la salida dadas (M12-19: el
     * corte al tercer rechazo y el freno de una inutilización sin 0600). Nunca en vivo: la URL base es la del simulador.
     *
     * @param emisor     emisor de prueba (de un {@code .env} temporal).
     * @param cfg        configuración del cliente (la del autofirmado; la URL base pasa a ser la del simulador).
     * @param simulador  simulador ya levantado.
     * @param tabla      tabla de equivalencias de la prueba.
     * @param salida     carpeta de salida ({@code simulacion.tsv}).
     * @param reintentos ids de {@code -Djbs.sifen.test.f12.reintentar}.
     * @return la instancia.
     */
    static SifenVivoFase12Test paraInutilizacionesSinRed(SifenTestEnv.Emisor emisor, SifenConfig cfg, SifenSimulator simulador, Path tabla,
            Path salida, Set<String> reintentos) throws Exception {
        SifenVivoFase12Test t = new SifenVivoFase12Test();
        t.simular = true;
        t.sim = simulador;
        t.emisor = emisor;
        t.dir = salida;
        Files.createDirectories(salida.resolve("simulacion"));
        t.res = salida.resolve("simulacion.tsv");
        if (!Files.exists(t.res)) {
            Files.write(t.res, List.of(CABECERA), StandardCharsets.UTF_8);
        }
        t.tabla = tabla;
        t.reemision = new ReemisionPrueba(t.emisor, SifenTestEnv.NUMERO_INICIAL_MINIMO, tabla);
        cfg.setBaseUrl(simulador.getBaseUrl());
        Assertions.assertTrue(cfg.getBaseUrl().startsWith("https://127.0.0.1:"), "sin red: solo contra el simulador local");
        t.client = SifenClient.create(cfg);
        t.reintentar = reintentos;
        t.esperados = new LinkedHashMap<>();
        return t;
    }

    /** Sin red: 12.3.11 (con su consulta de pendientes) y cierra el cliente. */
    void inutilizacionesSinRed() throws Exception {
        try {
            inutilizaciones(new Fila(78, "12.3.11", "(inutilizaciones)", "V0", "—", "0600", "—", "", ""));
        } finally {
            client.close();
        }
    }

    /** @return las fallas anotadas (sin red). */
    List<String> fallasSinRed() {
        return fallas;
    }

    /** @return si la tanda quedó detenida (sin red). */
    boolean detenidoSinRed() {
        return detenido;
    }

    // ------------------------------------------------------------------ utilidades

    private void salida() throws Exception {
        String s = System.getProperty(P + "salida");
        Assumptions.assumeTrue(s != null, "falta -D" + P + "salida");
        simular = Boolean.getBoolean(P + "simular");
        dir = Paths.get(s);
        Files.createDirectories(dir.resolve(simular ? "simulacion" : "respuestas"));
        res = dir.resolve(simular ? "simulacion.tsv" : "resultados.tsv");
        if (!Files.exists(res)) {
            Files.write(res, List.of(CABECERA), StandardCharsets.UTF_8);
        }
    }

    /**
     * D58 (I12-17): el {@code gEmis} base de la corrida. RUC, DV, timbrado, establecimiento, punto, correo y razón social
     * de prueba salen del archivo de entorno ({@link SifenTestEnv}); el resto (domicilio, actividad económica, responsable)
     * sale de la <b>primera venta por nombre</b> de {@code test8/ventas} ({@code venta-*.xml} en orden, determinista), como
     * en §8.7.
     */
    private static void cargarEmisorReal() throws Exception {
        Path ventas = Paths.get(System.getProperty(SifenVivoTest.PROP_VENTAS));
        Path primera;
        try (Stream<Path> s = Files.list(ventas)) {
            primera = s.filter(p -> p.getFileName().toString().matches("venta-.*\\.xml")).sorted().findFirst()
                    .orElseThrow(() -> new IllegalStateException("sin venta-*.xml en " + ventas));
        }
        emisorReal = new DocumentoElectronico(Files.readString(primera, StandardCharsets.UTF_8)).getgDatGralOpe().getgEmis();
    }

    private static TgEmis copiaEmisor() {
        TgEmis e = new TgEmis();
        TgEmis o = emisorReal;
        e.setdRucEm(o.getdRucEm());
        e.setdDVEmi(o.getdDVEmi());
        e.setiTipCont(o.getiTipCont());
        e.setcTipReg(o.getcTipReg());
        e.setdNomEmi(o.getdNomEmi());
        e.setdNomFanEmi(o.getdNomFanEmi());
        e.setdDirEmi(o.getdDirEmi());
        e.setdNumCas(o.getdNumCas());
        e.setdCompDir1(o.getdCompDir1());
        e.setdCompDir2(o.getdCompDir2());
        e.setcDepEmi(o.getcDepEmi());
        e.setcDisEmi(o.getcDisEmi());
        e.setdDesDisEmi(o.getdDesDisEmi());
        e.setcCiuEmi(o.getcCiuEmi());
        e.setdDesCiuEmi(o.getdDesCiuEmi());
        e.setdTelEmi(o.getdTelEmi());
        e.setdEmailE(o.getdEmailE());
        e.setdDenSuc(o.getdDenSuc());
        e.setgActEcoList(o.getgActEcoList());
        e.setgRespDE(o.getgRespDE());
        return e;
    }

    private static DocumentoElectronico caso(String id) {
        return CasosDePrueba.documentos().stream().filter(k -> k.getId().equals(id)).findFirst().orElseThrow().construir();
    }

    private static List<TgPaConEIni> pagos(DocumentoElectronico de) {
        if (de.getgDtipDE() == null || de.getgDtipDE().getgCamCond() == null || de.getgDtipDE().getgCamCond().getgPaConEIniList() == null) {
            return List.of();
        }
        return de.getgDtipDE().getgCamCond().getgPaConEIniList();
    }

    private static List<TgCamItem> items(DocumentoElectronico de) {
        return de.getgDtipDE().getgCamItemList() == null ? List.of() : de.getgDtipDE().getgCamItemList();
    }

    private static String cdc(SignedDocument d) {
        return d == null ? null : d.getCdc();
    }

    /** Como {@link #conjunto(String)}, en el orden escrito. */
    private static List<String> conjuntoOrdenado(String s) {
        if (s == null || s.isBlank()) {
            return List.of();
        }
        return Arrays.stream(s.split(",")).map(String::strip).filter(x -> !x.isEmpty()).toList();
    }

    private static Set<String> conjunto(String s) {
        if (s == null || s.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(s.split(",")).map(String::strip).filter(x -> !x.isEmpty()).collect(Collectors.toCollection(HashSet::new));
    }

    private static Set<String> codigos(List<ValidationError> errores) {
        return errores.stream().map(ValidationError::getCode).collect(Collectors.toCollection(TreeSet::new));
    }

    private static String local(ValidationResult v) {
        Set<String> e = codigos(v.getErrors());
        Set<String> a = codigos(v.getWarnings());
        String s = e.isEmpty() ? "acepta" : "rechaza " + String.join(", ", e);
        return a.isEmpty() ? s : s + " · aviso " + String.join(", ", a);
    }

    private static String digest(SendResult r, SignedDocument d) {
        return r.getDigestValue() == null || r.getDigestValue().isEmpty() ? "sin dDigVal"
                : (r.getDigestValue().equals(d.getDigestValue()) && !r.isDigestMismatch() ? "sí" : "NO");
    }

    /**
     * Huella del código que firma (§12.3.1): md5 de las clases compiladas de {@code src/main} que usa esta corrida. El
     * {@code getVersion()} no distingue el jar instalado de las correcciones sin commit (E12-01, E12-02).
     */
    static String huellaDelCodigo() throws Exception {
        Path clases = Paths.get("target", "classes");
        if (!Files.isDirectory(clases)) {
            return "huella: sin target/classes";
        }
        MessageDigest md = MessageDigest.getInstance("MD5");
        List<Path> l;
        try (Stream<Path> s = Files.walk(clases)) {
            l = s.filter(p -> p.toString().endsWith(".class")).sorted().toList();
        }
        for (Path p : l) {
            md.update(clases.relativize(p).toString().getBytes(StandardCharsets.UTF_8));
            md.update(Files.readAllBytes(p));
        }
        return "huella de target/classes: md5 " + HexFormat.of().formatHex(md.digest()) + " (" + l.size() + " clases)";
    }

    /**
     * Huella de las fuentes de la corrida (md5 de {@link #FUENTES}, desde el módulo): se escribe en cada corrida, para
     * cotejarla con la anotada en la evidencia.
     */
    static String huellaDeLasFuentes() throws Exception {
        List<String> l = new ArrayList<>();
        for (String f : FUENTES) {
            Path p = Paths.get(f);
            l.add(p.getFileName() + " " + (Files.exists(p) ? md5(p) : "(no está)"));
        }
        return "md5 de las fuentes: " + String.join(" · ", l);
    }

    private static String md5(Path p) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(Files.readAllBytes(p)));
    }

    private void pausar() throws InterruptedException {
        if (!primero && !simular) {
            Thread.sleep(pausa);
        }
        primero = false;
    }

    /** Guarda el pedido firmado por su CDC (en el ensayo y en vivo), antes de enviarlo. */
    private void guardarPedido(String id, SignedDocument doc) throws Exception {
        Files.writeString(pedidoDe(doc.getCdc()), doc.getXml(), StandardCharsets.UTF_8);
    }

    private void guardar(String nombre, String contenido) throws Exception {
        if (!simular) {
            Files.writeString(dir.resolve("respuestas/" + nombre), String.valueOf(contenido), StandardCharsets.UTF_8);
        }
    }

    private static String limpio(String s) {
        return String.valueOf(s).replace('\t', ' ').replace('\n', ' ');
    }

    /** En la salida no van CDC completos: el tipo y los cuatro últimos dígitos (sin el RUC del emisor de prueba). */
    static String enmascarar(String linea) {
        return CDC_EN_TEXTO.matcher(linea).replaceAll("$1…$2");
    }

    private void fila(Fila f, String numero, String op, String cdc, String local, String status, String cod, String esperado, String dato,
            String obs) throws Exception {
        String coincide = cod == null || cod.isEmpty() || esperado == null || esperado.isEmpty() ? "" : coincide(esperado, cod) ? "sí" : "NO";
        String linea = String.join("\t", LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString(), String.valueOf(f.orden()), f.tanda(),
                f.caso(), f.numero(), numero, op, cdc, local, status, cod == null ? "" : cod, esperado == null ? "" : esperado, coincide,
                limpio(dato), limpio(obs));
        linea = enmascarar(linea);
        Files.write(res, List.of(linea), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        System.out.println(linea);
    }
}
