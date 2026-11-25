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
package org.javabeanstack.sifen;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.javabeanstack.sifen.api.SendResult;
import org.javabeanstack.sifen.api.SignedDocument;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.validation.support.ValueChecks;
import org.javabeanstack.sifen.simulator.RecordedRequest;
import org.javabeanstack.sifen.simulator.SifenSimulator;
import org.javabeanstack.sifen.simulator.SimulatedService;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Tres empresas y veinte hilos a la vez contra el simulador (plan JBSSIF, §10.5, CA10-04, CA8-13): cada
 * pedido sale con <b>su</b> numeración ({@code dId}), <b>su</b> certificado de TLS y, al emitir, <b>su</b>
 * firma y su XML sin mezclarse con el de otro hilo. Sin red.
 *
 * <p>Tres partes, porque dependen de carriles distintos:</p>
 * <ul>
 * <li><b>{@code dId} por cliente</b> ({@link #cadaEmpresaNumeraSusPedidosSinHuecosNiRepetidos}): habilitado.</li>
 * <li><b>Certificado de TLS por cliente</b> ({@link #cadaPedidoSaleConElCertificadoTlsDeSuEmpresa}): habilitado desde
 * que §5.2 (carril B) dejó el contexto TLS en cada conexión. Nota de la corrida del 2026-11-24: también pasa contra
 * la línea base de §10.2 (jar con {@code SSLContextHelper._sslContext} estático), porque ese contexto se rearmaba al
 * cambiar el certificado y cada conexión se quedaba con su fábrica de sockets: la mezcla no era observable, el
 * problema era el estado compartido (I10-14). Lo cubre la parte siguiente.</li>
 * <li><b>Ningún estático mutable</b> en el camino de {@code SifenClient}
 * ({@link #ningunEstaticoMutableEnElCaminoDeSifenClient}, CA10-04 «adelanta CA7-04»): se corre con
 * {@code -Djbs.sifen.test.sin_estaticos=true}; hoy lista lo que queda (evidencia de I10-14 para B y F).</li>
 * <li><b>Firma y XML por cliente</b> ({@link #cadaEnvioLlevaLaFirmaYElXmlDeSuEmpresa}): depende de §10.3 (F:
 * {@code sign}/{@code send}) y §4.1 (A: envío del {@code rDE} firmado sin regenerarlo). Hoy {@code sign}
 * termina con {@code JBS-INT-04}. Se corre con {@code -Djbs.sifen.test.emision_por_cliente=true}.</li>
 * </ul>
 */
public class MultiCompanyClientTest {
    private static final int HILOS = 20;
    private static final int VUELTAS = 10;
    /** Empresa → {RUC, DV, recurso del certificado (null = el del simulador)}. */
    private static final Map<String, String[]> EMPRESAS = Map.of(
            "empresa-a", new String[]{"80123456", "5", null},
            "empresa-b", new String[]{"80999901", "3", "certificados/jbs-sifen-empresa-b.pfx"},
            "empresa-c", new String[]{"80999902", "1", "certificados/jbs-sifen-empresa-c.pfx"});
    private static final List<String> ORDEN = List.of("empresa-a", "empresa-b", "empresa-c");
    private static SifenSimulator sim;
    private static Map<String, ISifenClient> clientes;

    @BeforeAll
    public static void levantar() throws Exception {
        sim = SifenSimulator.start();
    }

    @AfterAll
    public static void bajar() {
        if (sim != null) {
            sim.close();
        }
    }

    /** Clientes nuevos en cada test: el contador de pedidos de cada uno arranca en 1. */
    @BeforeEach
    public void limpiar() throws Exception {
        sim.reset();
        clientes = new HashMap<>();
        for (String e : ORDEN) {
            clientes.put(e, SifenClient.create(config(e)));
        }
    }

    @AfterEach
    public void cerrar() {
        clientes.values().forEach(ISifenClient::close);
    }

    private static SifenConfig config(String empresa) throws Exception {
        String recurso = EMPRESAS.get(empresa)[2];
        if (recurso == null) {
            return sim.config(empresa);
        }
        try (InputStream in = MultiCompanyClientTest.class.getClassLoader().getResourceAsStream(recurso)) {
            Assertions.assertNotNull(in, recurso);
            return sim.config(empresa, in.readAllBytes(), SifenSimulator.PASSWORD);
        }
    }

    /** Cada hilo usa la empresa {@code i % 3} y hace {@link #VUELTAS} llamadas; todos largan juntos. */
    private static void enParalelo(Tarea tarea) throws Exception {
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(HILOS);
        try {
            List<Future<?>> futuros = new ArrayList<>();
            for (int h = 0; h < HILOS; h++) {
                final int hilo = h;
                futuros.add(pool.submit(() -> {
                    largada.await();
                    String empresa = ORDEN.get(hilo % ORDEN.size());
                    for (int v = 0; v < VUELTAS; v++) {
                        tarea.hacer(empresa, clientes.get(empresa), hilo, v);
                    }
                    return null;
                }));
            }
            largada.countDown();
            for (Future<?> f : futuros) {
                f.get(180, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @FunctionalInterface
    private interface Tarea {
        void hacer(String empresa, ISifenClient cliente, int hilo, int vuelta) throws Exception;
    }

    private static int llamadasDe(String empresa) {
        int hilos = 0;
        for (int h = 0; h < HILOS; h++) {
            if (ORDEN.get(h % ORDEN.size()).equals(empresa)) {
                hilos++;
            }
        }
        return hilos * VUELTAS;
    }

    @Test
    public void losCertificadosDeLasEmpresasSonDistintosYValidos() throws Exception {
        for (String e : ORDEN) {
            String[] d = EMPRESAS.get(e);
            Assertions.assertTrue(ValueChecks.validDV(d[0], d[1]), e + ": DV del RUC ficticio");
            Assertions.assertTrue(clientes.get(e).getCertificateInfo().getRucs().contains(d[0] + "-" + d[1]),
                    e + ": " + clientes.get(e).getCertificateInfo().getRucs());
        }
    }

    /** {@code dId} por cliente con 3 × 20 hilos: cada empresa numera 1..N, sin huecos ni repetidos. */
    @Test
    public void cadaEmpresaNumeraSusPedidosSinHuecosNiRepetidos() throws Exception {
        enParalelo((empresa, c, hilo, vuelta) -> {
            if (vuelta % 2 == 0) {
                Assertions.assertTrue(c.queryRuc("80123456").isFound());
            } else {
                Assertions.assertNotNull(c.queryDE(CasosDePrueba.CDC_FACTURA_SIMPLE).getStatus());
            }
        });
        for (String e : ORDEN) {
            List<Long> ids = new ArrayList<>();
            for (RecordedRequest p : sim.requests(e)) {
                ids.add(p.getRequestId());
            }
            Collections.sort(ids);
            List<Long> esperado = new ArrayList<>();
            for (long i = 1; i <= llamadasDe(e); i++) {
                esperado.add(i);
            }
            Assertions.assertEquals(esperado, ids, e);
        }
        Assertions.assertEquals(HILOS * VUELTAS, sim.requests().size());
    }

    /**
     * Certificado de TLS por cliente (M3-64): cada pedido de una empresa se abrió con el certificado de esa
     * empresa (TLS mutuo; el simulador registra el certificado del par).
     */
    @Test
    public void cadaPedidoSaleConElCertificadoTlsDeSuEmpresa() throws Exception {
        enParalelo((empresa, c, hilo, vuelta) -> Assertions.assertTrue(c.queryRuc("80123456").isFound()));
        Map<String, List<String>> mezclados = new ConcurrentHashMap<>();
        for (RecordedRequest p : sim.requests()) {
            String[] d = EMPRESAS.get(p.getTenant());
            if (!p.getClientSubject().contains("RUC" + d[0] + "-" + d[1])) {
                mezclados.computeIfAbsent(p.getTenant(), k -> Collections.synchronizedList(new ArrayList<>()))
                        .add(p.getClientSubject());
            }
        }
        Assertions.assertTrue(mezclados.isEmpty(), "pedidos con el certificado TLS de otra empresa: " + resumen(mezclados));
    }

    /**
     * Firma y XML por cliente: cada hilo firma un DE de su empresa (numeración propia), lo envía, y lo que
     * llegó al simulador es exactamente ese XML (mismo {@code DigestValue}, mismo {@code dRedon}, mismo
     * certificado en la firma). Depende de §10.3 (F) y §4.1 (A).
     */
    @Test
    @EnabledIfSystemProperty(named = "jbs.sifen.test.emision_por_cliente", matches = "true",
            disabledReason = "depende de §10.3 (F: sign/send) y §4.1 (A: envío del rDE firmado sin regenerarlo)")
    public void cadaEnvioLlevaLaFirmaYElXmlDeSuEmpresa() throws Exception {
        Map<String, SignedDocument> firmados = new ConcurrentHashMap<>();
        enParalelo((empresa, c, hilo, vuelta) -> {
            SignedDocument doc = c.sign(documento(empresa, hilo * VUELTAS + vuelta + 1));
            firmados.put(doc.getCdc(), doc);
            SendResult r = c.send(doc);
            Assertions.assertFalse(r.isDigestMismatch(), "el simulador devuelve el digest recibido (D43)");
        });
        Assertions.assertEquals(HILOS * VUELTAS, firmados.size(), "un CDC por documento");
        for (RecordedRequest p : sim.requests(SimulatedService.RECEIVE_DE)) {
            SignedDocument doc = firmados.get(p.getCdc());
            Assertions.assertNotNull(doc, "CDC recibido que nadie firmó: " + p.getCdc());
            String[] d = EMPRESAS.get(p.getTenant());
            Assertions.assertTrue(p.getCdc().substring(2, 11).equals(d[0] + d[1]), p.getTenant() + ": emisor del CDC");
            Assertions.assertEquals(doc.getDigestValue(), p.text("DigestValue").trim(), "DigestValue");
            // los dos lados decodificados (I10-16): el del documento por un parser, el del pedido por el simulador
            Assertions.assertEquals(org.javabeanstack.sifen.test.casos.XmlCanonico.texto(doc.getXml(), "dRedon"), p.text("dRedon"), "dRedon");
            Assertions.assertEquals(org.javabeanstack.sifen.test.casos.XmlCanonico.texto(doc.getXml(), "X509Certificate"),
                    p.text("X509Certificate"), "certificado de la firma");
        }
    }

    /**
     * Raíces del camino de {@code SifenClient} (I10-20, §10.7): el control recorre las clases de
     * {@code org.javabeanstack.sifen} alcanzables desde estas por las referencias de su bytecode (el
     * constant pool), así que un paquete o una clase nueva del camino entra sola, sin una lista a mano.
     */
    private static final List<String> RAICES = List.of("org.javabeanstack.sifen.SifenClient",
            "org.javabeanstack.sifen.SifenClientRegistry");

    /**
     * Excepciones <b>propuestas</b> por el carril F en §10.3 (I10-15), pendientes de la decisión de
     * {@code code-approver}: CA10-04 no se marca mientras haya alguna. Cada una con su motivo; cualquier otro
     * estático mutable hace fallar la prueba.
     */
    private static final Map<String, String> EXCEPCIONES_PROPUESTAS = Map.ofEntries(
            Map.entry("org.javabeanstack.sifen.internal.cert.CertificateStore.SHARED",
            "caché por contenido (ruta o resumen del PKCS#12 + resumen de la contraseña + huella del archivo) que usan la "
                    + "validación y la firma; dos empresas con certificados distintos nunca comparten entrada. Hacerla por "
                    + "cliente es I5-02 (validación, GenerationCtx y SignatureHelper)"),
            Map.entry("org.javabeanstack.sifen.internal.http.DefaultConnections.BY_CONFIG",
            "conexión por identidad de configuración solo para la fachada heredada Sifen; SifenClient pasa su "
                    + "SifenConnection a cada pedido (SifenClientEmissionTest.elClienteNoUsaLasConexionesPorConfiguracion)"),
            Map.entry("org.javabeanstack.sifen.internal.http.DefaultConnections.WITHOUT_CONFIG",
            "idem BY_CONFIG: solo para un pedido heredado sin configuración"),
            // §10.7 (I10-20): aparecieron al recorrer el camino completo
            Map.entry("org.javabeanstack.sifen.internal.facade.Operations.PROFUNDIDAD",
            "profundidad de anidamiento de la operación en curso, por hilo (ThreadLocal<int[]>): no lleva datos de "
                    + "ninguna empresa ni pasa de una operación a otra"),
            Map.entry("org.javabeanstack.sifen.Sifen.sifenConfig",
            "fachada heredada (D34, congelada): el recorrido la alcanza solo por DocumentoElectronico.generarXml(ctx, ruta), "
                    + "que no es contrato ni lo llama SifenClient"),
            Map.entry("org.javabeanstack.sifen.Sifen.dId",
            "idem Sifen.sifenConfig: contador de la fachada heredada, que SifenClient no usa (tiene el suyo por cliente)"),
            Map.entry("org.javabeanstack.sifen.internal.facade.Operations.EN_CURSO",
                    "contador global de operaciones en curso que solo lee la fachada heredada Sifen (aviso JBS-EST-01); no "
                    + "lleva datos de una empresa"));

    /**
     * Ningún campo estático mutable en el camino de {@code SifenClient}: ni estáticos no finales, ni finales de un
     * tipo mutable (colecciones, mapas, atómicos, arreglos, contextos TLS, almacenes). Lo inmutable (textos,
     * números, {@code Logger}, {@code Pattern}, enums, {@code Duration}) se acepta. Depende de §5.2 (B) y de
     * §10.3/§4.1 (F/A) para la firma.
     */
    @Test
    @EnabledIfSystemProperty(named = "jbs.sifen.test.sin_estaticos", matches = "true",
            disabledReason = "evidencia para §5.2/§10.3: todavía hay estado estático compartido (I10-14)")
    public void ningunEstaticoMutableEnElCaminoDeSifenClient() throws Exception {
        Set<String> clases = camino();
        Assertions.assertTrue(clases.contains("org.javabeanstack.sifen.internal.http.UrlSifenConnection")
                && clases.contains("org.javabeanstack.sifen.internal.facade.Operations"), "el recorrido llega al transporte y al envoltorio");
        List<String> mutables = new ArrayList<>();
        for (String n : clases) {
            Class<?> c;
            try {
                c = Class.forName(n);
            } catch (ClassNotFoundException e) {
                continue; // retirada: nada que revisar
            }
            for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                int m = f.getModifiers();
                if (!java.lang.reflect.Modifier.isStatic(m) || f.isSynthetic() || f.isEnumConstant()) {
                    continue;
                }
                if (EXCEPCIONES_PROPUESTAS.containsKey(n + "." + f.getName())) {
                    continue;
                }
                if (!java.lang.reflect.Modifier.isFinal(m) || !inmutable(f.getType()) && !valorInmutable(f)) {
                    mutables.add(n + "." + f.getName() + " : " + f.getType().getSimpleName()
                            + (java.lang.reflect.Modifier.isFinal(m) ? " (final)" : ""));
                }
            }
        }
        Assertions.assertTrue(mutables.isEmpty(), mutables.size() + " estáticos mutables:\n" + String.join("\n", mutables));
    }

    /**
     * Valor de un estático final que es inmutable aunque su tipo declarado no lo diga ({@code List.of},
     * {@code Set.of}, {@code Map.of}, {@code Collections.unmodifiable*}): se mira la clase del valor (I10-20).
     */
    private static boolean valorInmutable(java.lang.reflect.Field f) {
        try {
            f.setAccessible(true);
            Object v = f.get(null);
            if (v == null) {
                return true;
            }
            String n = v.getClass().getName();
            if (v.getClass().isArray()) {
                return java.lang.reflect.Array.getLength(v) == 0;   // un arreglo vacío no se puede modificar
            }
            return n.startsWith("java.util.ImmutableCollections$") || n.startsWith("java.util.Collections$Unmodifiable")
                    || n.startsWith("java.util.Collections$Empty") || n.startsWith("java.util.Collections$Singleton")
                    || inmutable(v.getClass()) || INMUTABLES_DE_LA_LIBRERIA.contains(n) || sinEstado(v.getClass());
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    /**
     * Tipos de la librería documentados como inmutables y seguros entre hilos (su javadoc lo dice; no guardan
     * datos de una empresa): las fichas y los resultados de la validación y los validadores estándar.
     */
    private static final Set<String> INMUTABLES_DE_LA_LIBRERIA = Set.of(
            "org.javabeanstack.sifen.core.validation.ValidationRule", "org.javabeanstack.sifen.core.validation.ValidationResult",
            "org.javabeanstack.sifen.core.validation.EffectivePeriod", "org.javabeanstack.sifen.core.validation.DEValidator",
            "org.javabeanstack.sifen.core.validation.EventValidator", "org.javabeanstack.sifen.core.validation.ConfigValidator");

    /** Un objeto sin campos de instancia (una función sin estado, como un {@code ErrorHandler} que no hace nada). */
    private static boolean sinEstado(Class<?> c) {
        for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
            for (java.lang.reflect.Field f : k.getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Clases de {@code org.javabeanstack.sifen} (de {@code main}) alcanzables desde {@link #RAICES} por su bytecode. */
    static Set<String> camino() throws java.io.IOException {
        java.util.Deque<String> pendientes = new java.util.ArrayDeque<>(RAICES);
        Set<String> vistas = new java.util.TreeSet<>();
        java.util.regex.Pattern ref = java.util.regex.Pattern.compile("L?(org/javabeanstack/sifen/[A-Za-z0-9_$/]+)");
        while (!pendientes.isEmpty()) {
            String n = pendientes.pop();
            if (!vistas.add(n)) {
                continue;
            }
            for (String u : utf8DelConstantPool(n)) {
                java.util.regex.Matcher m = ref.matcher(u);
                while (m.find()) {
                    String r = m.group(1).replace('/', '.');
                    if (!vistas.contains(r) && esDeMain(r)) {
                        pendientes.push(r);
                    }
                }
            }
        }
        return vistas;
    }

    private static boolean esDeMain(String clase) {
        java.net.URL u = MultiCompanyClientTest.class.getClassLoader().getResource(clase.replace('.', '/') + ".class");
        return u != null && !u.toString().contains("test-classes") && !u.toString().contains("-tests.jar");
    }

    /** Las cadenas UTF-8 del constant pool de una clase (nombres de clases y descriptores). */
    private static List<String> utf8DelConstantPool(String clase) throws java.io.IOException {
        List<String> out = new ArrayList<>();
        try (InputStream in = MultiCompanyClientTest.class.getClassLoader().getResourceAsStream(clase.replace('.', '/') + ".class")) {
            if (in == null) {
                return out;
            }
            java.io.DataInputStream d = new java.io.DataInputStream(in);
            d.readInt();
            d.readUnsignedShort();
            d.readUnsignedShort();
            int n = d.readUnsignedShort();
            for (int i = 1; i < n; i++) {
                int tag = d.readUnsignedByte();
                switch (tag) {
                    case 1: out.add(d.readUTF()); break;
                    case 3: case 4: d.readInt(); break;
                    case 5: case 6: d.readLong(); i++; break;
                    case 7: case 8: case 16: case 19: case 20: d.readUnsignedShort(); break;
                    case 9: case 10: case 11: case 12: case 17: case 18: d.readInt(); break;
                    case 15: d.readUnsignedByte(); d.readUnsignedShort(); break;
                    default: throw new java.io.IOException("tag de constant pool desconocido " + tag + " en " + clase);
                }
            }
        }
        return out;
    }

    private static boolean inmutable(Class<?> t) {
        return t.isPrimitive() || t.isEnum() || t == String.class || Number.class.isAssignableFrom(t) && !t.getName().contains("atomic")
                || t == Boolean.class || t == java.util.regex.Pattern.class || t == java.time.Duration.class
                || t.getName().equals("org.apache.logging.log4j.Logger") || t == Class.class
                || t == javax.xml.namespace.QName.class || t == java.time.format.DateTimeFormatter.class
                // java.time (ZoneId, Clock, LocalDateTime…) es inmutable; un ResourceBundle no se modifica por su API
                || t.getName().startsWith("java.time.") || java.util.ResourceBundle.class.isAssignableFrom(t);
    }

    /** El DE del caso 4 del catálogo con el emisor de la empresa, un número propio y las leyendas de DEV (Q14). */
    private static DocumentoElectronico documento(String empresa, int numero) {
        String[] d = EMPRESAS.get(empresa);
        DocumentoElectronico de = CasosDePrueba.documentos().get(4).construir();
        de.getgDatGralOpe().getgEmis().setdRucEm(d[0]);
        de.getgDatGralOpe().getgEmis().setdDVEmi(d[1]);
        de.getgDatGralOpe().getgEmis().setdNomEmi("DE generado en ambiente de prueba - sin valor comercial ni fiscal");
        de.getgDtipDE().getgCamItemList().get(0).setdDesProSer(
                "DOCUMENTO ELECTRÓNICO SIN VALOR COMERCIAL NI FISCAL - GENERADO EN AMBIENTE DE PRUEBA");
        de.getgTimb().setdNumDoc(String.format("%07d", numero));
        return de;
    }


    private static String resumen(Map<String, List<String>> mezclados) {
        StringBuilder sb = new StringBuilder();
        mezclados.forEach((e, l) -> sb.append(e).append(": ").append(l.size()).append(" pedidos, p. ej. ").append(l.get(0)).append("; "));
        return sb.toString();
    }
}
