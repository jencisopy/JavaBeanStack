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
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.javabeanstack.sifen.ISifenClient;
import org.javabeanstack.sifen.SifenClient;
import org.javabeanstack.sifen.api.BatchQueryResult;
import org.javabeanstack.sifen.api.BatchSendResult;
import org.javabeanstack.sifen.api.ConnectionDiagnosis;
import org.javabeanstack.sifen.api.DEQueryResult;
import org.javabeanstack.sifen.api.EventResult;
import org.javabeanstack.sifen.api.NumberRange;
import org.javabeanstack.sifen.api.RucQueryResult;
import org.javabeanstack.sifen.api.SendResult;
import org.javabeanstack.sifen.api.SignedDocument;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.beans.EventosDE;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.fields.request.de.TgCamCond;
import org.javabeanstack.sifen.core.fields.request.de.TgCamDEAsoc;
import org.javabeanstack.sifen.core.fields.request.de.TgDatRec;
import org.javabeanstack.sifen.core.fields.request.de.TgEmis;
import org.javabeanstack.sifen.core.fields.request.de.TgOblAfe;
import org.javabeanstack.sifen.core.fields.request.de.TgPaConEIni;
import org.javabeanstack.sifen.core.fields.request.event.TgGroupTiEvt;
import org.javabeanstack.sifen.core.fields.request.event.TrGeVeInu;
import org.javabeanstack.sifen.core.fields.request.event.TrGeVeNom;
import org.javabeanstack.sifen.core.fields.request.event.TrGesEve;
import org.javabeanstack.sifen.core.types.CMondT;
import org.javabeanstack.sifen.core.types.TTiDE;
import org.javabeanstack.sifen.core.types.TTipEmi;
import org.javabeanstack.sifen.core.types.TcOblAfe;
import org.javabeanstack.sifen.core.types.TiCondOpe;
import org.javabeanstack.sifen.core.types.TiTiPago;
import org.javabeanstack.sifen.core.validation.SifenValidationException;
import org.javabeanstack.sifen.core.validation.ValidationError;
import org.javabeanstack.sifen.core.validation.ValidationResult;
import org.javabeanstack.sifen.internal.http.ServidoresFase5;
import org.javabeanstack.sifen.test.SifenTestEnv;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.XmlCanonico;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Batería de aceptación del beta (plan JBSSIF, §8.7) contra SIFEN-test, toda por {@link ISifenClient} y con la
 * validación previa activa: el recorrido de la empresa del usuario (FE de contado y a crédito en PYG y USD, NC
 * asociada a una FE de la misma corrida, NR, autofactura, lote, cancelación, inutilización, {@code resolve},
 * consultas y {@code checkConnection}) más los pendientes en vivo de las Fases 4, 5, 6 y 10 (CA6-05, I6-08,
 * I10-06, I10-19).
 *
 * <p>Ambiente de PRUEBAS (D28), 001-001, numeración desde el siguiente libre de la tabla de equivalencias,
 * reservada antes de cada envío. No reintenta: un rechazo o un corte se registra y se analiza; un resultado
 * desconocido se recupera con {@code resolve}, nunca con otro número. La única excepción a la validación activa
 * es el caso de {@code iTipEmi} = 2 (CA6-05): primero se comprueba el rechazo local B002 con la validación activa
 * y después se envía el mismo documento por un segundo cliente sin validación previa, solo para ver el 1050.</p>
 *
 * <p>Uso: {@code mvn -pl sifen -am test -Psifen-live -Dtest=SifenVivoBetaTest#bateria -Djbs.sifen.test.env=<env>
 * -Djbs.sifen.test.ventas=<ventas de test8> -Djbs.sifen.test.equivalencias=<csv>
 * -Djbs.sifen.test.beta.salida=<carpeta fuera del repo>}; {@code -Djbs.sifen.test.beta.simular=true} es el
 * ensayo en seco (firma y validación local, sin red, sobre una copia temporal de la tabla).</p>
 */
@Tag("sifen-live")
public class SifenVivoBetaTest {

    private static final String MOTIVO_CANCELACION = "Cancelacion de prueba del beta de jbs-sifen (JBSSIF 8.7)";
    private static final String MOTIVO_INUTILIZACION = "Inutilizacion de prueba del beta de jbs-sifen (JBSSIF 8.7)";

    private static TgEmis emisorReal;

    private Path dir;
    private Path res;
    private boolean simular;
    private long pausa;
    private ReemisionPrueba reemision;
    private SifenTestEnv.Emisor emisor;
    private final List<String> fallas = new ArrayList<>();
    /** Documentos firmados de la corrida, por id de caso. */
    private final Map<String, SignedDocument> firmados = new LinkedHashMap<>();
    /** Casos aprobados por SIFEN en esta corrida. */
    private final Map<String, SignedDocument> aprobados = new LinkedHashMap<>();
    /** Número enviado (001-001-…) de cada caso. */
    private final Map<String, String> numeros = new LinkedHashMap<>();
    private boolean primero = true;
    /** Pasos a correr ({@code -Djbs.sifen.test.beta.solo}); vacío = todos. */
    private java.util.Set<String> solo = new java.util.HashSet<>();

    private boolean corre(String paso) {
        return solo.isEmpty() || solo.contains(paso);
    }

    /**
     * Reanudación tras un corte de la corrida ({@code -Djbs.sifen.test.beta.previos=B-01,B-02,…}): cada caso ya
     * aprobado se toma de la tabla (número y CDC) y de su pedido guardado, con {@code readSigned}; el B-13 firmado y
     * no enviado, de su {@code .firmado.xml}. Nada se vuelve a firmar ni a numerar.
     */
    private void cargarPrevios(ISifenClient client, Path tabla) throws Exception {
        String previos = System.getProperty("jbs.sifen.test.beta.previos");
        if (previos == null || previos.isBlank()) {
            return;
        }
        List<String> filas = Files.readAllLines(tabla, StandardCharsets.UTF_8);
        for (String id : previos.split(",")) {
            String numero = null;
            String cdc = null;
            boolean aprobado = false;
            for (String l : filas) {
                String[] c = l.split(";", -1);
                if (c.length > 6 && c[0].equals(id) && c[2].startsWith("001-001-")) {
                    numero = c[2];
                    cdc = c[4];
                    aprobado |= c[6].equals("ISifenClient.send") && c[8].equals("0260");
                }
            }
            Assertions.assertNotNull(cdc, "el caso previo " + id + " está en la tabla");
            Path f = dir.resolve("respuestas/" + cdc + (aprobado ? ".pedido.xml" : ".firmado.xml"));
            SignedDocument d = client.readSigned(Files.readString(f, StandardCharsets.UTF_8));
            Assertions.assertEquals(cdc, d.getCdc());
            numeros.put(id, numero);
            firmados.put(id, d);
            if (aprobado) {
                aprobados.put(id, d);
            }
            fila(numero, id + " (previo, de la corrida anterior)", "readSigned", cdc, aprobado ? "aprobado" : "firmado, sin enviar",
                    "", "", "", "", "", "", "");
        }
    }

    @Test
    public void bateria() throws Exception {
        SifenTestEnv.Entorno entorno = SifenTestEnv.entornoOSaltar();
        emisor = SifenTestEnv.emisorOSaltar();
        String salida = System.getProperty("jbs.sifen.test.beta.salida");
        Assumptions.assumeTrue(salida != null, "falta jbs.sifen.test.beta.salida");
        simular = Boolean.getBoolean("jbs.sifen.test.beta.simular");
        pausa = Long.getLong("jbs.sifen.test.beta.pausa", 3000L);
        long horasDiferida = Long.getLong("jbs.sifen.test.beta.diferida.horas", 4L);
        dir = Paths.get(salida);
        Files.createDirectories(dir.resolve(simular ? "simulacion" : "respuestas"));

        SifenConfig cfg = entorno.getConfiguracion();
        Assertions.assertEquals(SifenConfig.Environment.DEV, cfg.getEnvironment(), "D28");
        Assertions.assertTrue(new SifenConfig().isPreValidationEnabled(), "el valor por omisión es con validación");
        Assertions.assertTrue(cfg.isPreValidationEnabled(), "la configuración del entorno no apaga la validación");

        Path tabla = Paths.get(System.getProperty(SifenVivoTest.PROP_EQUIVALENCIAS));
        if (simular) {
            Path copia = Files.createTempFile("equivalencias-simulacion-beta", ".csv");
            Files.copy(tabla, copia, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            tabla = copia;
        }
        reemision = new ReemisionPrueba(emisor, entorno.getNumeroInicial(), tabla);
        cargarEmisorReal();
        res = dir.resolve(simular ? "simulacion.tsv" : "resultados.tsv");
        if (!Files.exists(res)) {
            Files.write(res, List.of("hora\tnumero\tcaso\toperacion\tcdc o id\tlocal\tstatus\tdCodRes\tdMsgRes\tdDigVal=local"
                    + "\tconsulta\tC14N=local\tobservaciones"), StandardCharsets.UTF_8);
        }
        System.out.println("Siguiente número libre: " + reemision.getSiguiente());
        String s = System.getProperty("jbs.sifen.test.beta.solo");
        if (s != null && !s.isBlank()) {
            solo = new java.util.HashSet<>(Arrays.asList(s.split(",")));
        }

        try (ISifenClient client = SifenClient.create(cfg)) {
            Assertions.assertTrue(client.getConfig().isPreValidationEnabled(), "el cliente valida antes de enviar");
            Assertions.assertTrue(client.validateConfig().isValid(), "la configuración del entorno no pasa la validación");
            fila("—", "jar", "getVersion", "", "", client.getVersion(), "", "", "", "", "", "");
            cargarPrevios(client, tabla);

            // 0) conexión y RUC (CA8-10), sin número
            if (!simular) {
                diagnostico(client, "B-00 checkConnection inicial");
                consultarRuc(client, "B-00 queryRuc del emisor de prueba");
            }

            // 1) emisión diferida (CA6-05): firmado ahora con dFecFirma de horas antes, guardado; se envía al final
            LocalDateTime antes = LocalDateTime.now().minusHours(horasDiferida).truncatedTo(ChronoUnit.SECONDS);
            SignedDocument diferido = firmados.containsKey("B-13") ? null : !corre("B-13") ? null
                    : firmar(client, "B-13", "emisión diferida: venta 372925, dFecFirma " + horasDiferida
                    + " h antes, guardado y enviado al final sin refirmar (CA6-05)", preparada(venta("372925")), antes);
            Path archivoDiferido = null;
            if (firmados.containsKey("B-13") && corre("B-13") && diferido == null) {
                archivoDiferido = dir.resolve("respuestas/" + firmados.get("B-13").getCdc() + ".firmado.xml");
            }
            if (diferido != null) {
                archivoDiferido = dir.resolve((simular ? "simulacion/" : "respuestas/") + diferido.getCdc() + ".firmado.xml");
                Files.writeString(archivoDiferido, diferido.getXml(), StandardCharsets.UTF_8);
            }

            // 2) envíos individuales
            if (corre("B-01")) individual(client, "B-01", "FE PYG contado (venta 372265)", venta("372265"));
            if (corre("B-02")) individual(client, "B-02", "FE PYG crédito (venta 372706, la factura de la NC)", venta("372706"));
            if (corre("B-03")) individual(client, "B-03", "FE USD contado (venta 373240 pasada a contado, derivada)", usdContado());
            if (corre("B-04")) individual(client, "B-04", "FE USD crédito (venta 373240)", venta("373240"));
            if (!corre("B-05")) {
                // paso no pedido
            } else if (aprobados.containsKey("B-02") || simular) {
                DocumentoElectronico nc = venta("372723");
                individualNc(client, "B-05", "NC (venta 372723) asociada a B-02", nc, "B-02");
            } else {
                fila("—", "B-05 NC", "sin envío", "", "", "", "", "B-02 no quedó aprobada", "", "", "", "");
            }
            if (corre("B-06")) individual(client, "B-06", "NR (DE-23, datos de prueba completados: cRelMerc, dCanQuiMer, dPorQuiMer, dMarVeh)", remision());
            if (corre("B-07")) individual(client, "B-07", "Autofactura (DE-20, datos de prueba con iTiOpe B2C)", autofactura());
            if (corre("B-09")) individual(client, "B-09", "FE con dos gOblAfe 211 y 700 al final de gOpeCom (I6-08, CA6-02)", conObligaciones());
            if (!corre("B-10")) {
                // paso no pedido
            } else if (aprobados.containsKey("B-01") || simular) {
                DocumentoElectronico nc = venta("372723");
                TgCamDEAsoc a = nc.getgCamDEAsocList().get(0);
                a.setdCdCDERef(venta("372265").getId());     // el CDC de producción: la reemisión lo cambia por el de B-01
                a.setdRucFus(emisor.getRuc());
                individualNc(client, "B-10", "NC con dRucFus = RUC del emisor, asociada a B-01 (I6-08)", nc, "B-01");
            } else {
                fila("—", "B-10 NC dRucFus", "sin envío", "", "", "", "", "B-01 no quedó aprobada", "", "", "", "");
            }
            if (corre("B-11")) individual(client, "B-11", "FE a receptor innominado (DE-16), para la nominación", armado("DE-16"));
            if (corre("B-12")) {
                contingencia(client, cfg);
            }

            // 3) resolve sobre un CDC aprobado: 0422 y sin reenvío (proxy contador)
            if (corre("B-08") && aprobados.containsKey("B-04") && !simular) {
                pausar();
                resolverAprobado(aprobados.get("B-04"));
            }

            // 4) eventos sobre los aprobados
            if (corre("E")) {
                eventos(client);
            }
            if (corre("C")) {
                // 0100 «Error Inesperado» en E-01/E-02 a los 3 min de la aprobación: un solo intento más, más tarde
                cancelar(client, "E-01r", "cancel de B-03 (FE USD contado), segundo intento a más de 10 min de la aprobación", "B-03");
                cancelar(client, "E-02r", "cancel de B-05 (NC), segundo intento a más de 10 min de la aprobación", "B-05");
            }

            // 5) lote
            if (corre("L")) {
                lote(client);
            }

            // 6) la emisión diferida: readSigned del archivo y send, sin refirmar
            if (archivoDiferido != null) {
                pausar();
                enviarDiferido(client, archivoDiferido);
            }

            // 7) inutilización de un rango libre y el Id de evento repetido (I10-19); se reservan al final
            if (corre("I")) {
                inutilizaciones(client);
            }

            if (!simular) {
                consultarRuc(client, "B-98 queryRuc del emisor de prueba (final)");
                diagnostico(client, "B-99 checkConnection final");
            }
        }
        Assertions.assertTrue(fallas.isEmpty(), () -> "casos que no dieron lo esperado: " + fallas);
    }

    /**
     * Diagnóstico de la consulta de RUC sin enviar ningún DE: {@code queryRuc} con la traza activa en
     * {@code -Djbs.sifen.test.beta.traza=<carpeta fuera del repo>} y la fachada heredada para comparar.
     */
    @Test
    public void diagnosticarRuc() throws Exception {
        String traza = System.getProperty("jbs.sifen.test.beta.traza");
        Assumptions.assumeTrue(traza != null, "sin diagnóstico de RUC");
        SifenTestEnv.Emisor em = SifenTestEnv.emisorOSaltar();
        SifenConfig cfg = SifenTestEnv.entornoOSaltar().getConfiguracion();
        Assertions.assertEquals(SifenConfig.Environment.DEV, cfg.getEnvironment(), "D28");
        cfg.setTraceEnabled(true);
        cfg.setTraceFolder(traza);
        try (ISifenClient client = SifenClient.create(cfg)) {
            try {
                RucQueryResult r = client.queryRuc(em.getRuc());
                System.out.println("queryRuc ⇒ " + r.getResponseCode() + " " + r.getMessage());
            } catch (SifenException x) {
                System.out.println("queryRuc ⇒ " + x.getCode() + " · detalle: " + x.getTechnicalDetail());
            }
        }
        SifenConfig cfg2 = SifenTestEnv.entornoOSaltar().getConfiguracion();
        org.javabeanstack.sifen.core.beans.response.RespuestaConsultaRUC r2 = org.javabeanstack.sifen.Sifen.consultaRUC(em.getRuc(), cfg2);
        System.out.println("Sifen.consultaRUC ⇒ " + r2.getdCodRes() + " " + r2.getdMsgRes());
        Files.writeString(Paths.get(traza, "consultaRUC-heredada.xml"), String.valueOf(r2.getRespuestaBruta()), StandardCharsets.UTF_8);
    }

    /** Serie de consultas de RUC (sin DE ni número): {@code -Djbs.sifen.test.beta.ruc.serie=<n>}, pausa de 3 s. */
    @Test
    public void serieRuc() throws Exception {
        int n = Integer.getInteger("jbs.sifen.test.beta.ruc.serie", 0);
        Assumptions.assumeTrue(n > 0, "sin serie de RUC");
        SifenTestEnv.Emisor em = SifenTestEnv.emisorOSaltar();
        String otros = System.getProperty("jbs.sifen.test.beta.ruc.otros");
        if (otros != null) {
            SifenConfig cfg = SifenTestEnv.entornoOSaltar().getConfiguracion();
            Assertions.assertEquals(SifenConfig.Environment.DEV, cfg.getEnvironment(), "D28");
            try (ISifenClient client = SifenClient.create(cfg)) {
                for (String ruc : otros.split(",")) {
                    for (int i = 0; i < n; i++) {
                        try {
                            RucQueryResult r = client.queryRuc(ruc);
                            System.out.println("RUC " + ruc + " ⇒ " + r.getResponseCode() + " " + r.getMessage() + " · estado "
                                    + r.getTaxpayerStatusCode());
                            break;
                        } catch (SifenException x) {
                            System.out.println("RUC " + ruc + " ⇒ " + x.getCode() + " · " + x.getTechnicalDetail());
                        }
                        Thread.sleep(3000L);
                    }
                }
            }
            return;
        }
        for (int k = 0; k < 2; k++) {
            SifenConfig cfg = SifenTestEnv.entornoOSaltar().getConfiguracion();
            Assertions.assertEquals(SifenConfig.Environment.DEV, cfg.getEnvironment(), "D28");
            try (ISifenClient client = SifenClient.create(cfg)) {
                for (int i = 0; i < n; i++) {
                    try {
                        RucQueryResult r = client.queryRuc(em.getRuc());
                        System.out.println("cliente " + k + " consulta " + i + " ⇒ " + r.getResponseCode());
                    } catch (SifenException x) {
                        System.out.println("cliente " + k + " consulta " + i + " ⇒ " + x.getCode() + " · " + x.getTechnicalDetail());
                    }
                    Thread.sleep(3000L);
                }
            }
        }
    }

    /**
     * Inutilización de los números rechazados de §8.7 (decisión del usuario, 2026-11-25): un evento por número, con el
     * tipo de documento de cada uno. {@code -Djbs.sifen.test.beta.inutilizar=0001053:7,0001055:4,0001059:1}; con
     * {@code -Djbs.sifen.test.beta.simular=true}, solo la validación local.
     */
    @Test
    public void inutilizarRechazados() throws Exception {
        String lista = System.getProperty("jbs.sifen.test.beta.inutilizar");
        Assumptions.assumeTrue(lista != null, "sin números a inutilizar");
        simular = Boolean.getBoolean("jbs.sifen.test.beta.simular");
        emisor = SifenTestEnv.emisorOSaltar();
        SifenTestEnv.Entorno entorno = SifenTestEnv.entornoOSaltar();
        SifenConfig cfg = entorno.getConfiguracion();
        Assertions.assertEquals(SifenConfig.Environment.DEV, cfg.getEnvironment(), "D28");
        Assertions.assertTrue(cfg.isPreValidationEnabled(), "validación activa");
        dir = Paths.get(System.getProperty("jbs.sifen.test.beta.salida"));
        Files.createDirectories(dir.resolve("respuestas"));
        res = dir.resolve(simular ? "simulacion.tsv" : "resultados.tsv");
        if (!Files.exists(res)) {
            Files.write(res, List.of("hora\tnumero\tcaso\toperacion\tcdc o id\tlocal\tstatus\tdCodRes\tdMsgRes\tdDigVal=local"
                    + "\tconsulta\tC14N=local\tobservaciones"), StandardCharsets.UTF_8);
        }
        Path tabla = Paths.get(System.getProperty(SifenVivoTest.PROP_EQUIVALENCIAS));
        if (simular) {
            Path copia = Files.createTempFile("equivalencias-simulacion-inu", ".csv");
            Files.copy(tabla, copia, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            tabla = copia;
        }
        reemision = new ReemisionPrueba(emisor, entorno.getNumeroInicial(), tabla);
        List<String> filas = Files.readAllLines(tabla, StandardCharsets.UTF_8);
        try (ISifenClient client = SifenClient.create(cfg)) {
            for (String par : lista.split(",")) {
                String num = par.substring(0, par.indexOf(':'));
                TTiDE tipo = TTiDE.getByVal(Short.parseShort(par.substring(par.indexOf(':') + 1)));
                String completo = emisor.getEstablecimiento() + "-" + emisor.getPuntoExpedicion() + "-" + num;
                // el número tiene que estar en la tabla, rechazado y sin aprobación, con el CDC del tipo indicado
                boolean aprobado = false;
                String cdc = null;
                for (String l : filas) {
                    String[] c = l.split(";", -1);
                    if (c.length > 8 && c[2].equals(completo)) {
                        cdc = c[4].isEmpty() ? cdc : c[4];
                        aprobado |= "0260".equals(c[8]);
                    }
                }
                Assertions.assertNotNull(cdc, completo + " está en la tabla");
                Assertions.assertFalse(aprobado, completo + " no tiene aprobación");
                Assertions.assertEquals(String.format("%02d", tipo.getVal()), cdc.substring(0, 2), "el tipo coincide con el CDC");
                String id = "I-" + num.substring(3);
                NumberRange rango = NumberRange.builder().timbrado(String.valueOf(emisor.getTimbrado()))
                        .establishment(emisor.getEstablecimiento()).issuePoint(emisor.getPuntoExpedicion())
                        .documentType(tipo).from(num).to(num).reason(MOTIVO_INUTILIZACION + " - numero rechazado").build();
                if (simular) {
                    fila(completo, id + " inutilización del rechazado (tipo " + tipo.getVal() + ")", "invalidateNumbers (simulado)",
                            "", "", "", "", "", "", "", "", "");
                    continue;
                }
                pausar();
                ReemisionPrueba.Reemision re = new ReemisionPrueba.Reemision("—", completo, null, "",
                        LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
                EventResult e;
                try {
                    e = client.invalidateNumbers(rango);
                } catch (SifenException x) {
                    fila(completo, id + " inutilización del rechazado (tipo " + tipo.getVal() + ")", "invalidateNumbers", "", "",
                            "excepción", x.getCode(), limpio(x.getMessage()), "", "", "", "resultado desconocido: " + x.isOutcomeUnknown());
                    reemision.registrar(id, re, "ISifenClient.invalidateNumbers:excepcion", 0, x.getCode(), null, x.getMessage());
                    fallas.add(id + " " + x.getCode());
                    continue;
                }
                guardar("evento-" + id + "-" + e.getEventId() + ".xml", e.getRawResponse());
                guardar("evento-" + id + "-" + e.getEventId() + ".pedido.xml", e.getEventXml());
                reemision.registrar(id, re, "ISifenClient.invalidateNumbers:Id " + e.getEventId() + " tipo " + tipo.getVal(), 200,
                        codigo(e), String.valueOf(e.getStatus()), mensaje(e));
                fila(completo, id + " inutilización del rechazado (tipo " + tipo.getVal() + ", desde = hasta)", "invalidateNumbers",
                        "Id " + e.getEventId(), "acepta", String.valueOf(e.getStatus()), codigo(e), limpio(mensaje(e)), "", "", "",
                        "protocolo " + e.getAuthorizationProtocol());
                if (!"0600".equals(codigo(e))) {
                    fallas.add(id + " " + codigo(e) + " " + mensaje(e));
                }
            }
        }
        Assertions.assertTrue(fallas.isEmpty(), () -> "inutilizaciones que no dieron 0600: " + fallas);
    }

    /**
     * Humo con el jar final antes de H2 (§8.7, revisión de cierre de las Fases 6 y 8): una FE de una venta de test8 con
     * el receptor de prueba ({@code sign} → {@code send} → {@code queryDE}), una FE a innominado y su nominación con
     * ciudad con la validación <b>activa</b> (E8-01), la cancelación de la primera FE (I8-08) y una serie de
     * {@code queryRuc} con {@code sifen.reintentos.max} = 2 y la traza activa (I8-03, CA8-10).
     * {@code -Djbs.sifen.test.beta.humo=<n consultas de RUC>}.
     */
    @Test
    public void humoFinal() throws Exception {
        int consultas = Integer.getInteger("jbs.sifen.test.beta.humo", 0);
        Assumptions.assumeTrue(consultas > 0, "sin humo final");
        simular = Boolean.getBoolean("jbs.sifen.test.beta.simular");
        pausa = Long.getLong("jbs.sifen.test.beta.pausa", 3000L);
        emisor = SifenTestEnv.emisorOSaltar();
        SifenTestEnv.Entorno entorno = SifenTestEnv.entornoOSaltar();
        dir = Paths.get(System.getProperty("jbs.sifen.test.beta.salida"));
        Files.createDirectories(dir.resolve(simular ? "simulacion" : "respuestas"));
        res = dir.resolve(simular ? "simulacion_humo.tsv" : "resultados_humo.tsv");
        if (!Files.exists(res)) {
            Files.write(res, List.of("hora\tnumero\tcaso\toperacion\tcdc o id\tlocal\tstatus\tdCodRes\tdMsgRes\tdDigVal=local"
                    + "\tconsulta\tC14N=local\tobservaciones"), StandardCharsets.UTF_8);
        }
        Path tabla = Paths.get(System.getProperty(SifenVivoTest.PROP_EQUIVALENCIAS));
        if (simular) {
            Path copia = Files.createTempFile("equivalencias-simulacion-humo", ".csv");
            Files.copy(tabla, copia, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            tabla = copia;
        }
        reemision = new ReemisionPrueba(emisor, entorno.getNumeroInicial(), tabla);
        cargarEmisorReal();
        SifenConfig cfg = entorno.getConfiguracion();
        Assertions.assertEquals(SifenConfig.Environment.DEV, cfg.getEnvironment(), "D28");
        Assertions.assertTrue(cfg.isPreValidationEnabled(), "validación activa");
        Path trazaDe = dir.resolve("respuestas/traza_humo_de");
        Path trazaRuc = dir.resolve("respuestas/traza_humo_ruc");
        if (!simular) {
            Files.createDirectories(trazaDe);
            Files.createDirectories(trazaRuc);
            cfg.setTraceEnabled(true);
            cfg.setTraceFolder(trazaDe.toString());
        }
        try (ISifenClient client = SifenClient.create(cfg)) {
            Assertions.assertTrue(client.getConfig().isPreValidationEnabled(), "el cliente valida antes de enviar");
            fila("—", "jar", "getVersion", "", "", client.getVersion(), "", "", "", "", "", "");
            // 1) humo: FE PYG contado de la venta 372265, receptor de prueba
            DocumentoElectronico fe = venta("372265");
            individual(client, "H-01", "humo: FE PYG contado (venta 372265, receptor de prueba)", fe);
            Assertions.assertEquals(emisor.getRuc(), XmlCanonico.texto(firmados.get("H-01").getXml(), "dRucRec"), "I8-05: receptor de prueba");
            Assertions.assertEquals("RECEPTOR DE PRUEBA", XmlCanonico.texto(firmados.get("H-01").getXml(), "dNomRec"), "I8-05");
            // 2) E8-01: nominación con ciudad, validación activa
            individual(client, "H-02", "FE a receptor innominado (DE-16), para la nominación", armado("DE-16"));
            SignedDocument inn = simular ? firmados.get("H-02") : aprobados.get("H-02");
            if (inn != null) {
                EventosDE ev = nominacion(inn);
                ValidationResult v = client.validateEvents(ev);
                boolean soloIdFecha = v.getErrors().stream().allMatch(e -> e.getCode().equals("JBS-EVENTO-01") || e.getCode().equals("JBS-EVENTO-02"));
                fila(numeros.get("H-02"), "H-03 nominación con dDesCiuRec (E8-01)", "validateEvents (validación activa)", inn.getCdc(),
                        soloIdFecha ? "acepta (salvo Id y dFecFirma, que asigna sendEvents)" : "rechaza local " + codigos(v.getErrors()),
                        "", "", "", "", "", "", "");
                if (!soloIdFecha) {
                    fallas.add("H-03 la validación activa sigue frenando la nominación: " + codigos(v.getErrors()));
                } else if (!simular) {
                    pausar();
                    evento(client, "H-03", "nominación con dDesCiuRec, validación activa (E8-01)", inn, () -> client.sendEvents(nominacion(inn)));
                }
            }
            // 3) I8-08: cancelación de la FE del humo
            SignedDocument h1 = simular ? firmados.get("H-01") : aprobados.get("H-01");
            if (h1 != null && !simular) {
                pausar();
                EventResult c = evento(client, "H-04", "cancel de H-01 (I8-08)", h1, () -> client.cancel(h1.getCdc(), MOTIVO_CANCELACION));
                if (c != null && "0100".equals(codigo(c)) && c.getStatus() != org.javabeanstack.sifen.api.ResultStatus.UNRECOGNIZED) {
                    fallas.add("H-04 0100 sin UNRECOGNIZED: " + c.getStatus());
                }
            }
        }
        // 4) I8-03 / CA8-10: serie de queryRuc con reintentos = 2 y la traza de cada intento
        if (!simular) {
            SifenConfig cr = SifenTestEnv.entornoOSaltar().getConfiguracion();
            Assertions.assertEquals(SifenConfig.Environment.DEV, cr.getEnvironment(), "D28");
            cr.setMaxRetries(2);
            cr.setTraceEnabled(true);
            cr.setTraceFolder(trazaRuc.toString());
            int bien = 0;
            try (ISifenClient client = SifenClient.create(cr)) {
                for (int i = 1; i <= consultas; i++) {
                    String op;
                    String cod;
                    String obs;
                    try {
                        RucQueryResult r = client.queryRuc(emisor.getRuc());
                        op = r.getOperationId();
                        cod = r.getResponseCode();
                        obs = r.isFound() ? "encontrado" : "NO encontrado";
                        if ("0502".equals(cod)) {
                            bien++;
                        } else {
                            fallas.add("R-" + i + " " + cod);
                        }
                    } catch (SifenException x) {
                        op = x.getCorrelationId();
                        cod = x.getCode() + " (SIFEN " + x.getSifenResponseCode() + ")";
                        obs = limpio(x.getTechnicalDetail());
                        fallas.add("R-" + i + " " + cod);
                    }
                    int intentos = 0;
                    int c0160 = 0;
                    try (java.nio.file.DirectoryStream<Path> ds = Files.newDirectoryStream(trazaRuc, "*_" + op + "_*_respuesta.xml")) {
                        for (Path f : ds) {
                            intentos++;
                            if (Files.readString(f, StandardCharsets.UTF_8).contains("<ns2:dCodRes>0160<")) {
                                c0160++;
                            }
                        }
                    }
                    fila("—", "R-" + i + " queryRuc con reintentos = 2 (I8-03, CA8-10)", "queryRuc", op, "", obs, cod, "", "", "", "",
                            "intentos " + intentos + " · 0160 recibidos " + c0160);
                    Thread.sleep(pausa);
                }
            }
            fila("—", "R resumen", "queryRuc ×" + consultas, "", "", "0502: " + bien, "", "", "", "", "", "");
        }
        Assertions.assertTrue(fallas.isEmpty(), () -> "casos que no dieron lo esperado: " + fallas);
    }

    /**
     * Cancelación de un caso aprobado de una corrida anterior ({@code -Djbs.sifen.test.beta.cancelar=H-01}): lo relee de
     * su pedido guardado con {@code readSigned} y muestra la hora de aprobación que informó SIFEN antes de cancelar.
     */
    @Test
    public void cancelarPrevio() throws Exception {
        String id = System.getProperty("jbs.sifen.test.beta.cancelar");
        Assumptions.assumeTrue(id != null, "sin cancelación");
        emisor = SifenTestEnv.emisorOSaltar();
        SifenConfig cfg = SifenTestEnv.entornoOSaltar().getConfiguracion();
        Assertions.assertEquals(SifenConfig.Environment.DEV, cfg.getEnvironment(), "D28");
        Assertions.assertTrue(cfg.isPreValidationEnabled(), "validación activa");
        dir = Paths.get(System.getProperty("jbs.sifen.test.beta.salida"));
        res = dir.resolve(System.getProperty("jbs.sifen.test.beta.tabla", "resultados_humo.tsv"));
        Path tabla = Paths.get(System.getProperty(SifenVivoTest.PROP_EQUIVALENCIAS));
        System.setProperty("jbs.sifen.test.beta.previos", id);
        try (ISifenClient client = SifenClient.create(cfg)) {
            cargarPrevios(client, tabla);
            SignedDocument d = aprobados.get(id);
            Assertions.assertNotNull(d, id + " aprobado");
            String aprobacion = "";
            Path r = dir.resolve("respuestas/" + d.getCdc() + ".xml");
            if (Files.exists(r)) {
                aprobacion = String.valueOf(XmlCanonico.texto(Files.readString(r, StandardCharsets.UTF_8), "dFecProc"));
            }
            fila(numeros.get(id), id + " dFecProc de la aprobación (SIFEN) " + aprobacion + " · ahora " + LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                    "—", d.getCdc(), "", "", "", "", "", "", "", "");
            EventResult c = evento(client, id + "-cancel", "cancel de " + id + " después de la hora de aprobación de SIFEN", d,
                    () -> client.cancel(d.getCdc(), MOTIVO_CANCELACION));
            Assertions.assertNotNull(c);
        }
        Assertions.assertTrue(fallas.isEmpty(), () -> "casos que no dieron lo esperado: " + fallas);
    }

    /**
     * Inutilización repetida de un rango ya inutilizado (pedido de MKSIF, §8.7): sin reservar números,
     * {@code -Djbs.sifen.test.beta.reinutilizar=<desde>:<hasta>:<tipo>}; se espera GEI005a / 4066 y {@code REJECTED}.
     */
    @Test
    public void reinutilizarRango() throws Exception {
        String arg = System.getProperty("jbs.sifen.test.beta.reinutilizar");
        Assumptions.assumeTrue(arg != null, "sin rango a reinutilizar");
        String[] a = arg.split(":");
        emisor = SifenTestEnv.emisorOSaltar();
        SifenTestEnv.Entorno entorno = SifenTestEnv.entornoOSaltar();
        SifenConfig cfg = entorno.getConfiguracion();
        Assertions.assertEquals(SifenConfig.Environment.DEV, cfg.getEnvironment(), "D28");
        Assertions.assertTrue(cfg.isPreValidationEnabled(), "validación activa");
        dir = Paths.get(System.getProperty("jbs.sifen.test.beta.salida"));
        res = dir.resolve("resultados.tsv");
        Path tabla = Paths.get(System.getProperty(SifenVivoTest.PROP_EQUIVALENCIAS));
        // el rango tiene que estar ya inutilizado (0600) en la tabla: no se reserva nada
        for (int n = Integer.parseInt(a[0]); n <= Integer.parseInt(a[1]); n++) {
            String completo = emisor.getEstablecimiento() + "-" + emisor.getPuntoExpedicion() + "-" + String.format("%07d", n);
            boolean inutilizado = Files.readAllLines(tabla, StandardCharsets.UTF_8).stream()
                    .map(l -> l.split(";", -1)).anyMatch(c -> c.length > 8 && c[2].equals(completo)
                            && c[6].startsWith("ISifenClient.invalidateNumbers") && "0600".equals(c[8]));
            Assertions.assertTrue(inutilizado, completo + " ya inutilizado en la tabla");
        }
        reemision = new ReemisionPrueba(emisor, entorno.getNumeroInicial(), tabla);
        TTiDE tipo = TTiDE.getByVal(Short.parseShort(a[2]));
        NumberRange rango = NumberRange.builder().timbrado(String.valueOf(emisor.getTimbrado()))
                .establishment(emisor.getEstablecimiento()).issuePoint(emisor.getPuntoExpedicion())
                .documentType(tipo).from(a[0]).to(a[1]).reason(MOTIVO_INUTILIZACION + " - rango ya inutilizado").build();
        String nums = a[0] + "…" + a[1];
        try (ISifenClient client = SifenClient.create(cfg)) {
            EventResult e;
            try {
                e = client.invalidateNumbers(rango);
            } catch (SifenException x) {
                fila(nums, "I-03 inutilización repetida de un rango ya inutilizado (MKSIF)", "invalidateNumbers", "", "", "excepción",
                        x.getCode(), limpio(x.getMessage()), "", "", "", "resultado desconocido: " + x.isOutcomeUnknown());
                throw x;
            }
            guardar("evento-I-03-" + e.getEventId() + ".xml", e.getRawResponse());
            guardar("evento-I-03-" + e.getEventId() + ".pedido.xml", e.getEventXml());
            StringBuilder m = new StringBuilder();
            e.getMessages().forEach(s -> m.append(m.length() == 0 ? "" : " | ").append(s.getCode()).append(' ').append(s.getText()));
            for (int n = Integer.parseInt(a[0]); n <= Integer.parseInt(a[1]); n++) {
                reemision.registrar("I-03", new ReemisionPrueba.Reemision("—", emisor.getEstablecimiento() + "-" + emisor.getPuntoExpedicion()
                        + "-" + String.format("%07d", n), null, "", LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)),
                        "ISifenClient.invalidateNumbers(repetido):Id " + e.getEventId(), 200, codigo(e), String.valueOf(e.getStatus()), mensaje(e));
            }
            fila(nums, "I-03 inutilización repetida de un rango ya inutilizado (MKSIF; tipo " + tipo.getVal() + ")", "invalidateNumbers",
                    "Id " + e.getEventId(), "acepta", String.valueOf(e.getStatus()), codigo(e), limpio(mensaje(e)), "", "", "",
                    "mensajes: " + limpio(m.toString()));
            System.out.println("I-03 ⇒ " + e.getStatus() + " " + m);
        }
    }

    // ------------------------------------------------------------------ envíos individuales

    private SignedDocument firmar(ISifenClient client, String id, String descripcion, DocumentoElectronico de, LocalDateTime fecha)
            throws Exception {
        ReemisionPrueba.Reemision re = reemision.reemitir(id, de, fecha);
        numeros.put(id, re.enviado);
        try {
            SignedDocument doc = client.sign(de);
            Assertions.assertEquals(re.cdc, doc.getCdc(), "el CDC firmado es el reservado");
            firmados.put(id, doc);
            fila(re.enviado, id + " " + descripcion, "sign", doc.getCdc(), "acepta · " + resumen(doc.getXml()), "", "", "", "", "", "", "");
            return doc;
        } catch (SifenValidationException x) {
            TreeSet<String> codigos = codigos(x.getErrors());
            fila(re.enviado, id + " " + descripcion, "sign", re.cdc, "rechaza local " + codigos, "", x.getCode(), "", "", "", "", "");
            if (!simular) {
                reemision.registrar(id, re, "ISifenClient.sign:rechazo-local", 0, x.getCode(), null, codigos.toString());
            }
            fallas.add(id + " rechazo local " + codigos);
            return null;
        }
    }

    private void individual(ISifenClient client, String id, String descripcion, DocumentoElectronico de) throws Exception {
        preparar(de);
        LocalDateTime ahora = LocalDateTime.now().minusMinutes(1).truncatedTo(ChronoUnit.SECONDS);
        SignedDocument doc = firmar(client, id, descripcion, de, ahora);
        if (doc != null) {
            enviar(client, id, descripcion, doc);
        }
    }

    private void individualNc(ISifenClient client, String id, String descripcion, DocumentoElectronico nc, String factura) throws Exception {
        preparar(nc);
        LocalDateTime ahora = LocalDateTime.now().minusMinutes(1).truncatedTo(ChronoUnit.SECONDS);
        SignedDocument doc = firmar(client, id, descripcion, nc, ahora);
        if (doc == null) {
            return;
        }
        String asociado = XmlCanonico.texto(doc.getXml(), "dCdCDERef");
        SignedDocument fe = simular ? firmados.get(factura) : aprobados.get(factura);
        Assertions.assertNotNull(fe, "la factura asociada es de esta corrida");
        Assertions.assertEquals(fe.getCdc(), asociado, "la NC asocia el CDC de " + factura + " de esta corrida");
        enviar(client, id, descripcion + " (dCdCDERef = " + factura + ")", doc);
    }

    /** {@code send} y, si aprueba, {@code queryDE} con la comparación del rDE de SIFEN contra el local. */
    private SendResult enviar(ISifenClient client, String id, String descripcion, SignedDocument doc) throws Exception {
        String num = numeros.get(id);
        if (simular) {
            Files.writeString(dir.resolve("simulacion/" + id + ".pedido.xml"), doc.getXml(), StandardCharsets.UTF_8);
            return null;
        }
        pausar();
        Files.writeString(dir.resolve("respuestas/" + doc.getCdc() + ".pedido.xml"), doc.getXml(), StandardCharsets.UTF_8);
        ReemisionPrueba.Reemision re = reserva(id, num, doc.getCdc());
        SendResult r;
        try {
            r = client.send(doc);
        } catch (SifenException x) {
            fila(num, id + " " + descripcion, "send", doc.getCdc(), "", "excepción", x.getCode(), limpio(x.getMessage()), "", "", "",
                    "resultado desconocido: " + x.isOutcomeUnknown());
            reemision.registrar(id, re, "ISifenClient.send:excepcion", 0, x.getCode(), null, x.getMessage());
            fallas.add(id + " " + x.getCode() + " (resultado desconocido: " + x.isOutcomeUnknown() + ")");
            return null;
        }
        guardar(doc.getCdc() + ".xml", r.getRawResponse());
        reemision.registrar(id, re, "ISifenClient.send", 200, r.getResponseCode(), r.getStatus().toString(), r.getMessage());
        String digest = digest(r, doc);
        String[] q = "0260".equals(r.getResponseCode()) ? consultar(client, id, re, doc) : new String[] {"", "", ""};
        fila(num, id + " " + descripcion, "send", doc.getCdc(), "acepta", r.getStatus().toString(), r.getResponseCode(),
                limpio(r.getMessage()), digest, q[0], q[1], q[2]);
        if ("0260".equals(r.getResponseCode())) {
            aprobados.put(id, doc);
        } else {
            fallas.add(id + " " + r.getResponseCode() + " " + r.getMessage());
        }
        return r;
    }

    /** queryDE tras un 0260: [consulta, C14N, observaciones]. */
    private String[] consultar(ISifenClient client, String id, ReemisionPrueba.Reemision re, SignedDocument doc) throws Exception {
        Thread.sleep(2000L);
        DEQueryResult q = client.queryDE(doc.getCdc());
        guardar("consulta-" + doc.getCdc() + ".xml", q.getRawResponse());
        if (re != null) {
            reemision.registrar(id, re, "ISifenClient.queryDE", 200, q.getResponseCode(), q.getStatus().toString(), q.getMessage());
        }
        String c14n = "";
        String obs = "escapes &lt; en la 0422: " + contar(q.getRawResponse(), "&lt;");
        if (q.getXml() != null && !q.getXml().isBlank()) {
            c14n = Arrays.equals(XmlCanonico.canonico(doc.getXml()), XmlCanonico.canonico(q.getXml())) ? "sí" : "NO";
            if (!doc.getDigestValue().equals(q.getDigestValue())) {
                c14n += " (DigestValue de la consulta distinto)";
            }
        } else {
            c14n = "sin rDE en la consulta";
        }
        return new String[] {q.getResponseCode() + " " + q.getStatus(), c14n, obs};
    }

    // ------------------------------------------------------------------ CA6-05: iTipEmi = 2

    private void contingencia(ISifenClient client, SifenConfig cfg) throws Exception {
        String id = "B-12";
        DocumentoElectronico de = armado("DE-05");
        de.getgOpeDE().setiTipEmi(TTipEmi.CONTINGENCIA);
        preparar(de);
        LocalDateTime ahora = LocalDateTime.now().minusMinutes(1).truncatedTo(ChronoUnit.SECONDS);
        ReemisionPrueba.Reemision re = reemision.reemitir(id, de, ahora);
        numeros.put(id, re.enviado);
        String desc = id + " iTipEmi = 2 (contingencia), DE-05 (CA6-05)";
        // 1) con la validación activa: B002 local
        try {
            client.sign(de);
            fila(re.enviado, desc, "sign (validación activa)", re.cdc, "ACEPTA (se esperaba B002)", "", "", "", "", "", "", "");
            fallas.add(id + " la validación activa no frenó iTipEmi = 2");
            return;
        } catch (SifenValidationException x) {
            TreeSet<String> c = codigos(x.getErrors());
            fila(re.enviado, desc, "sign (validación activa)", re.cdc, "rechaza local " + c, "", x.getCode(), "", "", "", "",
                    c.stream().anyMatch(s -> s.startsWith("B002")) ? "B002 confirmado" : "sin B002");
            if (!simular) {
                reemision.registrar(id, re, "ISifenClient.sign:rechazo-local", 0, x.getCode(), null, c.toString());
            }
            if (c.stream().noneMatch(s -> s.startsWith("B002"))) {
                fallas.add(id + " sin B002: " + c);
            }
        }
        // 2) única excepción a la validación activa (CA6-05): otro cliente, sin validación previa, solo este caso
        SifenConfig sinValidar = SifenTestEnv.entornoOSaltar().getConfiguracion();
        Assertions.assertEquals(SifenConfig.Environment.DEV, sinValidar.getEnvironment(), "D28");
        sinValidar.setPreValidationEnabled(false);
        Assertions.assertTrue(cfg.isPreValidationEnabled(), "la configuración compartida sigue validando");
        try (ISifenClient crudo = SifenClient.create(sinValidar)) {
            SignedDocument doc = crudo.sign(de);
            Assertions.assertEquals(re.cdc, doc.getCdc());
            Assertions.assertEquals("2", XmlCanonico.texto(doc.getXml(), "iTipEmi"));
            if (simular) {
                Files.writeString(dir.resolve("simulacion/" + id + ".pedido.xml"), doc.getXml(), StandardCharsets.UTF_8);
                fila(re.enviado, desc, "sign (sin validación, cliente aparte)", doc.getCdc(), "firmado", "", "", "", "", "", "", "");
                return;
            }
            pausar();
            Files.writeString(dir.resolve("respuestas/" + doc.getCdc() + ".pedido.xml"), doc.getXml(), StandardCharsets.UTF_8);
            SendResult r;
            try {
                r = crudo.send(doc);
            } catch (SifenException x) {
                fila(re.enviado, desc, "send (sin validación, cliente aparte)", doc.getCdc(), "", "excepción", x.getCode(),
                        limpio(x.getMessage()), "", "", "", "resultado desconocido: " + x.isOutcomeUnknown());
                reemision.registrar(id, re, "ISifenClient.send(sin-validacion):excepcion", 0, x.getCode(), null, x.getMessage());
                fallas.add(id + " " + x.getCode());
                return;
            }
            guardar(doc.getCdc() + ".xml", r.getRawResponse());
            reemision.registrar(id, re, "ISifenClient.send(sin-validacion)", 200, r.getResponseCode(), r.getStatus().toString(), r.getMessage());
            fila(re.enviado, desc, "send (sin validación, cliente aparte)", doc.getCdc(), "B002 apagado", r.getStatus().toString(),
                    r.getResponseCode(), limpio(r.getMessage()), "", "", "", "1050".equals(r.getResponseCode()) ? "1050 confirmado" : "NO es 1050");
            if (!"1050".equals(r.getResponseCode())) {
                fallas.add(id + " SIFEN devolvió " + r.getResponseCode() + " (se esperaba 1050)");
            }
        }
    }

    // ------------------------------------------------------------------ resolve sobre un aprobado

    private void resolverAprobado(SignedDocument doc) throws Exception {
        String id = "B-08";
        try (ServidoresFase5.ProxyLocal proxy = new ServidoresFase5.ProxyLocal(null, null)) {
            SifenConfig conProxy = SifenTestEnv.entornoOSaltar().getConfiguracion();
            Assertions.assertEquals(SifenConfig.Environment.DEV, conProxy.getEnvironment(), "D28");
            Assertions.assertTrue(conProxy.isPreValidationEnabled(), "validación activa");
            conProxy.setProxyHost("127.0.0.1");
            conProxy.setProxyPort(proxy.puerto());
            try (ISifenClient porProxy = SifenClient.create(conProxy)) {
                SignedDocument leido = porProxy.readSigned(doc.getXml());
                long antes = proxy.connects();
                SendResult r;
                try {
                    r = porProxy.resolve(leido);
                } catch (SifenException x) {
                    fila(numeros.get("B-04"), id + " resolve de B-04 aprobado", "readSigned → resolve (proxy)", doc.getCdc(), "",
                            "excepción", x.getCode(), limpio(x.getMessage()), "", "", "", "túneles: " + (proxy.connects() - antes));
                    fallas.add(id + " " + x.getCode());
                    return;
                }
                long tuneles = proxy.connects() - antes;
                guardar(doc.getCdc() + ".resolve.xml", r.getRawResponse());
                boolean consulta = String.valueOf(r.getRawResponse()).contains("rEnviConsDeResponse");
                fila(numeros.get("B-04"), id + " resolve de B-04 ya aprobado", "readSigned → resolve (proxy contador)", doc.getCdc(), "",
                        r.getStatus() + (r.isAlreadyRegistered() ? " (ya registrado)" : ""), r.getResponseCode(), limpio(r.getMessage()),
                        digest(r, doc), "", "", "túneles: " + tuneles + " · respuesta de consulta: " + consulta);
                if (!r.isAlreadyRegistered() || tuneles != 1 || !consulta) {
                    fallas.add(id + " resolve: ya registrado " + r.isAlreadyRegistered() + ", túneles " + tuneles + ", consulta " + consulta);
                }
            }
        }
    }

    // ------------------------------------------------------------------ eventos

    private void eventos(ISifenClient client) throws Exception {
        // cancelación de una FE sin NC asociada (B-03) y de la NC (B-05)
        cancelar(client, "E-01", "cancel de B-03 (FE USD contado)", "B-03");
        cancelar(client, "E-02", "cancel de B-05 (NC)", "B-05");
        // nominación de B-11 con dDesCiuRec (I6-08)
        SignedDocument inn = simular ? firmados.get("B-11") : aprobados.get("B-11");
        if (inn == null) {
            fila("—", "E-03 nominación de B-11", "sin envío", "", "", "", "", "B-11 no quedó aprobada", "", "", "", "");
            return;
        }
        EventosDE ev = nominacion(inn);
        ValidationResult v = client.validateEvents(ev);
        fila(numeros.get("B-11"), "E-03 nominación de B-11 (dDesCiuRec, I6-08)", "validateEvents (validación activa)", inn.getCdc(),
                v.isValid() ? "acepta" : "rechaza local " + codigos(v.getErrors()), "", "", "", "", "", "",
                "Id y dFecFirma los asigna sendEvents");
        if (simular) {
            return;
        }
        boolean soloIdFecha = v.getErrors().stream().allMatch(e -> e.getCode().equals("JBS-EVENTO-01") || e.getCode().equals("JBS-EVENTO-02"));
        if (soloIdFecha) {
            pausar();
            evento(client, "E-03", "nominación de B-11 (dDesCiuRec, I6-08)", inn, () -> client.sendEvents(nominacion(inn)));
            return;
        }
        // 1) con la validación activa, por sendEvents: el rechazo local que vería Maker
        try {
            client.sendEvents(nominacion(inn));
            fila(numeros.get("B-11"), "E-03 nominación (validación activa)", "sendEvents", inn.getCdc(), "ENVIADO", "", "", "", "", "", "", "");
        } catch (SifenValidationException x) {
            fila(numeros.get("B-11"), "E-03 nominación (validación activa)", "sendEvents", inn.getCdc(), "rechaza local " + codigos(x.getErrors()),
                    "", x.getCode(), "", "", "", "", "sin contactar a SIFEN");
            fallas.add("E-03 la validación activa frena la nominación con dDesCiuRec: " + codigos(x.getErrors()));
        }
        // 2) I6-08 en vivo: el mismo evento por un cliente aparte sin validación previa (como CA6-05)
        SifenConfig sinValidar = SifenTestEnv.entornoOSaltar().getConfiguracion();
        Assertions.assertEquals(SifenConfig.Environment.DEV, sinValidar.getEnvironment(), "D28");
        sinValidar.setPreValidationEnabled(false);
        try (ISifenClient crudo = SifenClient.create(sinValidar)) {
            pausar();
            evento(crudo, "E-03b", "nominación de B-11 con dDesCiuRec, cliente aparte sin validación (I6-08)", inn,
                    () -> crudo.sendEvents(nominacion(inn)));
        }
    }

    /** Nominación de un receptor no contribuyente con dirección y ciudad (EV-05) sobre el CDC dado. */
    private static EventosDE nominacion(SignedDocument inn) {
        EventosDE ev = CasosDePrueba.eventoNominacionConDireccion((short) 1);
        TrGesEve ges = ev.getrGesEveList().get(0);
        ges.setId(null);               // la librería asigna el Id
        ges.setdFecFirma(null);        // y la hora de Asunción
        TrGeVeNom nom = ges.getgGroupTiEvt().getrGeVeNom();
        nom.setId(inn.getCdc());
        nom.setdDTipIDRec("Cédula paraguaya");   // GENFE011: la descripción de la Tabla (el catálogo la trae sin tilde)
        return ev;
    }

    private void cancelar(ISifenClient client, String id, String desc, String caso) throws Exception {
        SignedDocument d = simular ? firmados.get(caso) : aprobados.get(caso);
        if (d == null) {
            fila("—", id + " " + desc, "sin envío", "", "", "", "", caso + " no quedó aprobada", "", "", "", "");
            return;
        }
        if (simular) {
            fila(numeros.get(caso), id + " " + desc, "cancel (simulado)", d.getCdc(), "", "", "", "", "", "", "", "");
            return;
        }
        pausar();
        evento(client, id, desc, d, () -> client.cancel(d.getCdc(), MOTIVO_CANCELACION));
    }

    @FunctionalInterface
    private interface Llamada<T> {
        T call() throws Exception;
    }

    private EventResult evento(ISifenClient client, String id, String desc, SignedDocument d, Llamada<EventResult> llamada) throws Exception {
        EventResult r;
        try {
            r = llamada.call();
        } catch (SifenException x) {
            fila(d == null ? "—" : numeroDe(d), id + " " + desc, "evento", d == null ? "" : d.getCdc(), "", "excepción", x.getCode(),
                    limpio(x.getMessage()), "", "", "", "resultado desconocido: " + x.isOutcomeUnknown());
            fallas.add(id + " " + x.getCode());
            return null;
        }
        guardar("evento-" + id + "-" + r.getEventId() + ".xml", r.getRawResponse());
        guardar("evento-" + id + "-" + r.getEventId() + ".pedido.xml", r.getEventXml());
        String consulta = "";
        if (d != null) {
            Thread.sleep(2000L);
            DEQueryResult q = client.queryDE(d.getCdc());
            guardar("consulta-evento-" + id + "-" + d.getCdc() + ".xml", q.getRawResponse());
            consulta = q.getResponseCode() + " " + q.getStatus() + " · eventos " + (q.getEvents() == null ? 0 : q.getEvents().size());
        }
        fila(d == null ? "—" : numeroDe(d), id + " " + desc, "evento", (d == null ? "" : d.getCdc() + " · ") + "Id " + r.getEventId(), "acepta",
                String.valueOf(r.getStatus()), codigo(r), limpio(mensaje(r)), "", consulta, "", "");
        if (!"0600".equals(codigo(r))) {
            fallas.add(id + " " + codigo(r) + " " + mensaje(r));
        }
        return r;
    }

    private static String codigo(EventResult r) {
        return r.getMessages() == null || r.getMessages().isEmpty() ? "" : r.getMessages().get(0).getCode();
    }

    private static String mensaje(EventResult r) {
        return r.getMessages() == null || r.getMessages().isEmpty() ? "" : r.getMessages().get(0).getText();
    }

    // ------------------------------------------------------------------ lote

    private void lote(ISifenClient client) throws Exception {
        List<SignedDocument> docs = new ArrayList<>();
        String[][] casos = {
            {"L-01", "venta 372231 (B2B crédito)"},
            {"L-02", "venta 372444 (12 ítems)"},
            {"L-03", "venta 372266 (transacción mixta)"},
            {"L-04", "DE-09 (IVA 10 %, 5 % y exento)"}};
        for (String[] c : casos) {
            DocumentoElectronico de = c[0].equals("L-04") ? armado("DE-09") : venta(c[1].substring(6, 12));
            preparar(de);
            LocalDateTime ahora = LocalDateTime.now().minusMinutes(1).truncatedTo(ChronoUnit.SECONDS);
            SignedDocument d = firmar(client, c[0], "lote: " + c[1], de, ahora);
            if (d != null) {
                docs.add(d);
            }
        }
        if (docs.size() != casos.length) {
            fila("—", "L lote", "sin envío", "", "", "", "", "algún DE del lote no pasó la validación local", "", "", "", "");
            return;
        }
        if (simular) {
            for (int i = 0; i < docs.size(); i++) {
                Files.writeString(dir.resolve("simulacion/" + casos[i][0] + ".pedido.xml"), docs.get(i).getXml(), StandardCharsets.UTF_8);
            }
            return;
        }
        pausar();
        List<ReemisionPrueba.Reemision> res = new ArrayList<>();
        for (int i = 0; i < docs.size(); i++) {
            Files.writeString(dir.resolve("respuestas/" + docs.get(i).getCdc() + ".pedido.xml"), docs.get(i).getXml(), StandardCharsets.UTF_8);
            res.add(reserva(casos[i][0], numeros.get(casos[i][0]), docs.get(i).getCdc()));
        }
        BatchSendResult b;
        try {
            b = client.sendBatch(docs);
        } catch (SifenException x) {
            fila("—", "L sendBatch de 4 FE", "sendBatch", "", "", "excepción", x.getCode(), limpio(x.getMessage()), "", "", "",
                    "resultado desconocido: " + x.isOutcomeUnknown());
            for (int i = 0; i < docs.size(); i++) {
                reemision.registrar(casos[i][0], res.get(i), "ISifenClient.sendBatch:excepcion", 0, x.getCode(), null, x.getMessage());
            }
            fallas.add("lote " + x.getCode());
            return;
        }
        guardar("lote-" + b.getBatchNumber() + ".xml", b.getRawResponse());
        for (int i = 0; i < docs.size(); i++) {
            reemision.registrar(casos[i][0], res.get(i), "ISifenClient.sendBatch:" + b.getBatchNumber(), 200, b.getResponseCode(),
                    String.valueOf(b.getStatus()), b.getMessage());
        }
        fila(numeros.get("L-01") + "…" + numeros.get("L-04"), "L sendBatch de 4 FE", "sendBatch", "lote " + b.getBatchNumber(), "acepta",
                String.valueOf(b.getStatus()), b.getResponseCode(), limpio(b.getMessage()), "", "", "",
                "CDC en orden: " + b.getCdcs().equals(docs.stream().map(SignedDocument::getCdc).toList())
                + " · estimado " + b.getEstimatedProcessingTime());
        if (!"0300".equals(b.getResponseCode())) {
            fallas.add("lote " + b.getResponseCode() + " " + b.getMessage());
            return;
        }
        // I10-06: el estado de un DE del lote por CDC, con el lote recién recibido
        Thread.sleep(5000L);
        DEQueryResult q0 = client.queryDE(docs.get(0).getCdc());
        guardar("consulta-lote-temprana-" + docs.get(0).getCdc() + ".xml", q0.getRawResponse());
        fila(numeros.get("L-01"), "L I10-06 queryDE de L-01 a los 5 s del sendBatch", "queryDE", docs.get(0).getCdc(), "",
                String.valueOf(q0.getStatus()), q0.getResponseCode(), limpio(q0.getMessage()), "", "", "", "");

        // ciclo de vida: una consulta temprana (60 s) y después la espera sugerida, con tope
        int intentos = Integer.getInteger("jbs.sifen.test.beta.lote.intentos", 8);
        long esperaMax = Long.getLong("jbs.sifen.test.beta.lote.espera.max", 600_000L);
        long espera = 60_000L;
        BatchQueryResult q = null;
        for (int i = 1; i <= intentos; i++) {
            Thread.sleep(espera);
            q = client.queryBatch(b.getBatchNumber());
            guardar("consulta-lote-" + b.getBatchNumber() + "-" + i + ".xml", q.getRawResponse());
            fila("—", "L queryBatch intento " + i, "queryBatch", "lote " + b.getBatchNumber(), "", String.valueOf(q.getStatus()),
                    q.getResponseCode(), limpio(q.getMessage()), "", "", "", "espera sugerida " + q.getSuggestedWait() + " · resultados "
                    + (q.getResults() == null ? 0 : q.getResults().size()));
            if (!"0361".equals(q.getResponseCode())) {
                break;
            }
            if (i == 1) {
                // I10-06 otra vez, con el lote en proceso confirmado
                DEQueryResult qp = client.queryDE(docs.get(1).getCdc());
                guardar("consulta-lote-en-proceso-" + docs.get(1).getCdc() + ".xml", qp.getRawResponse());
                fila(numeros.get("L-02"), "L I10-06 queryDE de L-02 con el lote en 0361", "queryDE", docs.get(1).getCdc(), "",
                        String.valueOf(qp.getStatus()), qp.getResponseCode(), limpio(qp.getMessage()), "", "", "", "");
            }
            Duration s = q.getSuggestedWait();
            espera = Math.min(esperaMax, s == null || s.isZero() ? 120_000L : s.toMillis());
        }
        if (q == null || !"0362".equals(q.getResponseCode())) {
            fallas.add("lote no concluyó: " + (q == null ? "sin consulta" : q.getResponseCode()));
            return;
        }
        Map<String, SendResult> porCdc = new LinkedHashMap<>();
        for (SendResult r : q.getResults()) {
            porCdc.put(r.getCdc(), r);
        }
        for (int i = 0; i < docs.size(); i++) {
            SignedDocument d = docs.get(i);
            SendResult r = porCdc.get(d.getCdc());
            if (r == null) {
                fila(numeros.get(casos[i][0]), casos[i][0] + " resultado del lote", "queryBatch", d.getCdc(), "", "sin resultado", "", "", "", "", "", "");
                fallas.add(casos[i][0] + " sin resultado en el lote");
                continue;
            }
            reemision.registrar(casos[i][0], res.get(i), "ISifenClient.queryBatch:" + b.getBatchNumber(), 200, r.getResponseCode(),
                    String.valueOf(r.getStatus()), r.getMessage());
            String[] qq = {"", "", ""};
            if (r.getStatus() == org.javabeanstack.sifen.api.ResultStatus.APPROVED) {
                qq = consultar(client, casos[i][0], res.get(i), d);
                aprobados.put(casos[i][0], d);
            } else {
                fallas.add(casos[i][0] + " " + r.getResponseCode() + " " + r.getMessage());
            }
            fila(numeros.get(casos[i][0]), casos[i][0] + " resultado del lote: " + casos[i][1], "queryBatch → queryDE", d.getCdc(), "acepta",
                    String.valueOf(r.getStatus()), r.getResponseCode(), limpio(r.getMessage()), digest(r, d), qq[0], qq[1], qq[2]);
        }
    }

    // ------------------------------------------------------------------ emisión diferida

    private void enviarDiferido(ISifenClient client, Path archivo) throws Exception {
        String id = "B-13";
        String xml = Files.readString(archivo, StandardCharsets.UTF_8);
        SignedDocument d = client.readSigned(xml);
        SignedDocument original = firmados.get(id);
        Assertions.assertEquals(original.getXml(), d.getXml(), "readSigned devuelve el XML byte a byte");
        Assertions.assertEquals(original.getDigestValue(), d.getDigestValue(), "sin refirmar");
        long horas = Duration.between(d.getSignatureDate(), LocalDateTime.now()).toMinutes();
        ValidationResult v = client.validateDE(d.getDocument());
        String aviso = v.getWarnings().stream().filter(w -> w.getCode().startsWith("A004")).map(ValidationError::getMessage)
                .findFirst().orElse("sin A004b (menos de 72 h)");
        String desc = "emisión diferida: readSigned del archivo y send sin refirmar; dFecFirma " + d.getSignatureDate()
                + " (" + horas + " min antes del envío)";
        if (simular) {
            fila(numeros.get(id), id + " " + desc, "readSigned → send (simulado)", d.getCdc(), "acepta · " + aviso, "", "", "", "", "", "", "");
            return;
        }
        Files.writeString(dir.resolve("respuestas/" + d.getCdc() + ".pedido.xml"), d.getXml(), StandardCharsets.UTF_8);
        ReemisionPrueba.Reemision re = reserva(id, numeros.get(id), d.getCdc());
        SendResult r;
        try {
            r = client.send(d);
        } catch (SifenException x) {
            fila(numeros.get(id), id + " " + desc, "readSigned → send", d.getCdc(), "", "excepción", x.getCode(), limpio(x.getMessage()),
                    "", "", "", "resultado desconocido: " + x.isOutcomeUnknown());
            reemision.registrar(id, re, "ISifenClient.send:excepcion", 0, x.getCode(), null, x.getMessage());
            fallas.add(id + " " + x.getCode());
            return;
        }
        guardar(d.getCdc() + ".xml", r.getRawResponse());
        reemision.registrar(id, re, "ISifenClient.send", 200, r.getResponseCode(), r.getStatus().toString(), r.getMessage());
        String[] q = "0260".equals(r.getResponseCode()) ? consultar(client, id, re, d) : new String[] {"", "", ""};
        fila(numeros.get(id), id + " " + desc, "readSigned → send", d.getCdc(), "acepta · " + aviso, r.getStatus().toString(),
                r.getResponseCode(), limpio(r.getMessage()), digest(r, d), q[0], q[1], q[2]);
        if ("0260".equals(r.getResponseCode())) {
            aprobados.put(id, d);
        } else {
            fallas.add(id + " " + r.getResponseCode() + " " + r.getMessage());
        }
    }

    // ------------------------------------------------------------------ inutilización e Id repetido (I10-19)

    private void inutilizaciones(ISifenClient client) throws Exception {
        int desde1 = reemision.getSiguiente();
        String[] r1 = reservarRango("I-01", desde1, desde1 + 1);
        String[] r2 = reservarRango("I-02", desde1 + 2, desde1 + 3);
        NumberRange rango = NumberRange.builder().timbrado(String.valueOf(emisor.getTimbrado()))
                .establishment(emisor.getEstablecimiento()).issuePoint(emisor.getPuntoExpedicion())
                .documentType(TTiDE.FACTURA_ELECTRONICA).from(r1[0]).to(r1[1]).reason(MOTIVO_INUTILIZACION).build();
        if (simular) {
            fila(r1[0] + "…" + r1[1], "I-01 invalidateNumbers de un rango libre reservado", "invalidateNumbers (simulado)", "", "", "", "", "", "", "", "", "");
            fila(r2[0] + "…" + r2[1], "I-02 inutilización por sendEvents con el Id de I-01 (I10-19)", "sendEvents (simulado)", "", "", "", "", "", "", "", "", "");
            return;
        }
        pausar();
        EventResult e1 = evento(client, "I-01", "invalidateNumbers de " + r1[0] + "…" + r1[1] + " (rango libre reservado)", null,
                () -> client.invalidateNumbers(rango));
        registrarRango("I-01", r1, e1);
        if (e1 == null || e1.getEventId() == null) {
            return;
        }
        // I10-19: segundo evento con el mismo Id, sobre otro rango
        TrGeVeInu inu = new TrGeVeInu();
        inu.setdNumTim(emisor.getTimbrado());
        inu.setdEst(emisor.getEstablecimiento());
        inu.setdPunExp(emisor.getPuntoExpedicion());
        inu.setdNumIn(r2[0]);
        inu.setdNumFin(r2[1]);
        inu.setiTiDE(TTiDE.FACTURA_ELECTRONICA);
        inu.setmOtEve(MOTIVO_INUTILIZACION + " - Id repetido");
        TgGroupTiEvt g = new TgGroupTiEvt();
        g.setrGeVeInu(inu);
        TrGesEve ges = new TrGesEve();
        ges.setId(e1.getEventId());
        ges.setgGroupTiEvt(g);
        EventosDE ev = new EventosDE();
        List<TrGesEve> l = new ArrayList<>();
        l.add(ges);
        ev.setrGesEveList(l);
        pausar();
        EventResult e2;
        try {
            e2 = client.sendEvents(ev);
        } catch (SifenException x) {
            fila(r2[0] + "…" + r2[1], "I-02 inutilización con el Id de I-01 (I10-19)", "sendEvents", "Id " + e1.getEventId(), "",
                    "excepción", x.getCode(), limpio(x.getMessage()), "", "", "", "resultado desconocido: " + x.isOutcomeUnknown());
            registrarRango("I-02", r2, null);
            return;
        }
        guardar("evento-I-02-" + e2.getEventId() + ".xml", e2.getRawResponse());
        guardar("evento-I-02-" + e2.getEventId() + ".pedido.xml", e2.getEventXml());
        registrarRango("I-02", r2, e2);
        fila(r2[0] + "…" + r2[1], "I-02 inutilización por sendEvents con el Id de I-01 (I10-19)", "sendEvents", "Id " + e2.getEventId()
                + (e2.getEventId().equals(e1.getEventId()) ? " (= I-01)" : " (≠ I-01)"), "acepta", String.valueOf(e2.getStatus()), codigo(e2),
                limpio(mensaje(e2)), "", "", "", "protocolo I-01 " + e1.getAuthorizationProtocol() + " · I-02 " + e2.getAuthorizationProtocol());
    }

    /** Reserva dos números consecutivos en la tabla para la inutilización (al final: no mueve el contador de los DE). */
    private String[] reservarRango(String id, int desde, int hasta) throws Exception {
        String a = String.format("%07d", desde);
        String b = String.format("%07d", hasta);
        if (!simular) {
            for (int n = desde; n <= hasta; n++) {
                reemision.registrar(id, new ReemisionPrueba.Reemision("—", emisor.getEstablecimiento() + "-" + emisor.getPuntoExpedicion()
                        + "-" + String.format("%07d", n), null, "", LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)),
                        "RESERVADO-INUTILIZACION", 0, null, null, null);
            }
        }
        return new String[] {a, b};
    }

    private void registrarRango(String id, String[] r, EventResult e) throws Exception {
        for (int n = Integer.parseInt(r[0]); n <= Integer.parseInt(r[1]); n++) {
            reemision.registrar(id, new ReemisionPrueba.Reemision("—", emisor.getEstablecimiento() + "-" + emisor.getPuntoExpedicion()
                    + "-" + String.format("%07d", n), null, "", LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)),
                    "ISifenClient." + (id.equals("I-01") ? "invalidateNumbers" : "sendEvents(Id repetido)") + (e == null ? ":excepcion" : ":Id " + e.getEventId()),
                    e == null ? 0 : 200, e == null ? null : codigo(e), e == null ? null : String.valueOf(e.getStatus()), e == null ? null : mensaje(e));
        }
    }

    // ------------------------------------------------------------------ armado de los DE

    private DocumentoElectronico venta(String iditem) throws Exception {
        Path ventas = Paths.get(System.getProperty(SifenVivoTest.PROP_VENTAS));
        return new DocumentoElectronico(Files.readString(ventas.resolve("venta-" + iditem + ".xml"), StandardCharsets.UTF_8));
    }

    /** La venta USD 373240 pasada a contado, con el pago en efectivo en USD por el total (derivada: test8 no tiene USD de contado). */
    private DocumentoElectronico usdContado() throws Exception {
        DocumentoElectronico de = venta("373240");
        TgCamCond c = de.getgDtipDE().getgCamCond();
        c.setiCondOpe(TiCondOpe.CONTADO);
        c.setgPagCred(null);
        TgPaConEIni p = new TgPaConEIni();
        p.setiTiPago(TiTiPago.EFECTIVO);
        p.setdMonTiPag(new BigDecimal("1575"));
        p.setcMoneTiPag(CMondT.USD);
        p.setdTiCamTiPag(de.getgDatGralOpe().getgOpeCom().getdTiCam());
        List<TgPaConEIni> l = new ArrayList<>();
        l.add(p);
        c.setgPaConEIniList(l);
        return de;
    }

    /** DE-23 con lo que la validación previa exige a una NR (E715, E717, JBS-E-174): el catálogo es del fork. */
    private DocumentoElectronico remision() {
        DocumentoElectronico de = armado("DE-23");
        for (org.javabeanstack.sifen.core.fields.request.de.TgCamItem i : de.getgDtipDE().getgCamItemList()) {
            i.setcRelMerc(org.javabeanstack.sifen.core.types.TcRelMerc.TOLERANCIA_DE_QUIEBRA);
            i.setdCanQuiMer(BigDecimal.ZERO);
            i.setdPorQuiMer(BigDecimal.ZERO);
        }
        for (org.javabeanstack.sifen.core.fields.request.de.TgVehTras v : de.getgDtipDE().getgTransp().getgVehTrasList()) {
            v.setdMarVeh("TOYOTA");      // el catálogo trae «MARCA FICTICIA» (14 caracteres; A 1-10)
        }
        // 2303 en 0001053: el RUC del transportista del catálogo (80123456) no existe en SIFEN-test
        de.getgDtipDE().getgTransp().getgCamTrans().setdRucTrans(emisor.getRuc());
        de.getgDtipDE().getgTransp().getgCamTrans().setdDVTrans(Short.parseShort(emisor.getDv()));
        return de;
    }

    /**
     * DE-20 con el receptor B2C que exige D202a (1316) a la autofactura y, con {@code -Djbs.sifen.test.beta.af.ci},
     * otra cédula del vendedor: la del catálogo (2345678) es un RUC activo en SIFEN-test (2562 en 0001055).
     */
    private static DocumentoElectronico autofactura() {
        DocumentoElectronico de = armado("DE-20");
        de.getgDatGralOpe().getgDatRec().setiTiOpe(org.javabeanstack.sifen.core.types.TiTiOpe.B2C);
        String ci = System.getProperty("jbs.sifen.test.beta.af.ci");
        if (ci != null) {
            de.getgDtipDE().getgCamAE().setdNumIDVen(ci);
        }
        return de;
    }

    private static DocumentoElectronico conObligaciones() {
        DocumentoElectronico de = armado("DE-05");
        List<TgOblAfe> l = new ArrayList<>();
        l.add(new TgOblAfe(TcOblAfe.IVA_GRAVADAS_EXONERADAS_EXPORTADORES));
        l.add(new TgOblAfe(TcOblAfe.IRE_REGIMEN_GENERAL));
        de.getgDatGralOpe().getgOpeCom().setgOblAfeList(l);
        return de;
    }

    private static DocumentoElectronico armado(String id) {
        return CasosDePrueba.documentos().stream().filter(k -> k.getId().equals(id)).findFirst().orElseThrow().construir();
    }

    /**
     * Los ajustes de las Fases 3 y 4: para un caso del catálogo, el {@code gEmis} de una venta real de test8 (si no,
     * 1261) y el origen 999-999 (guarda D28 de la reemisión); para todos, el receptor contribuyente con el RUC del
     * propio emisor de prueba (si no, 1306) y, en las ventas, sin los datos de contacto del cliente real.
     */
    private void preparar(DocumentoElectronico de) {
        boolean deVenta = "001".equals(de.getgTimb().getdEst()) && "002".equals(de.getgTimb().getdPunExp());
        if (!deVenta) {
            de.getgDatGralOpe().setgEmis(copiaEmisor());
            de.getgTimb().setdEst("999");
            de.getgTimb().setdPunExp("999");
        }
        TgDatRec r = de.getgDatGralOpe().getgDatRec();
        if (r.getdRucRec() != null) {
            r.setdRucRec(emisor.getRuc());
            r.setdDVRec(Short.parseShort(emisor.getDv()));
        }
        if (deVenta) {
            r.setdNomRec("RECEPTOR DE PRUEBA");
            r.setdNomFanRec(null);
            if (r.getdDirRec() != null) {
                r.setdDirRec("CALLE FICTICIA 1");
            }
            r.setdTelRec(null);
            r.setdCelRec(null);
            r.setdEmailRec(null);
            r.setdCodCliente(null);
        }
    }

    /** {@link #preparar} y devuelve el mismo DE (I8-05: el B-13 de 0001047 se firmó sin el receptor de prueba). */
    private DocumentoElectronico preparada(DocumentoElectronico de) {
        preparar(de);
        return de;
    }

    private static void cargarEmisorReal() throws Exception {
        Path ventas = Paths.get(System.getProperty(SifenVivoTest.PROP_VENTAS));
        try (java.nio.file.DirectoryStream<Path> ds = Files.newDirectoryStream(ventas, "venta-*.xml")) {
            Path primera = ds.iterator().next();
            emisorReal = new DocumentoElectronico(Files.readString(primera, StandardCharsets.UTF_8)).getgDatGralOpe().getgEmis();
        }
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

    // ------------------------------------------------------------------ utilidades

    /** La fila de reserva del CDC (ya escrita por {@code reemitir}), para registrar el resultado. */
    private ReemisionPrueba.Reemision reserva(String id, String numero, String cdc) {
        return new ReemisionPrueba.Reemision("—", numero, null, cdc, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    private String numeroDe(SignedDocument d) {
        for (Map.Entry<String, SignedDocument> e : firmados.entrySet()) {
            if (e.getValue().getCdc().equals(d.getCdc())) {
                return numeros.get(e.getKey());
            }
        }
        return "—";
    }

    /** queryRuc sin cortar la corrida: SIFEN-test contesta 0160 en forma intermitente a la consulta de RUC (§8.7). */
    private void consultarRuc(ISifenClient client, String caso) throws Exception {
        try {
            RucQueryResult ruc = client.queryRuc(emisor.getRuc());
            guardar("ruc-" + emisor.getRuc() + ".xml", ruc.getRawResponse());
            fila("—", caso, "queryRuc", emisor.getRuc(), "", ruc.isFound() ? "encontrado" : "no encontrado", ruc.getResponseCode(),
                    limpio(ruc.getMessage()), "", "", "", "facturador electrónico: " + ruc.isElectronicInvoicer());
        } catch (SifenException x) {
            fila("—", caso, "queryRuc", emisor.getRuc(), "", "excepción", x.getCode(), limpio(x.getMessage()), "", "", "",
                    limpio(x.getTechnicalDetail()));
            fallas.add(caso + " " + x.getCode());
        }
    }

    private void diagnostico(ISifenClient client, String caso) throws Exception {
        ConnectionDiagnosis d = client.checkConnection();
        fila("—", caso, "checkConnection", d.getOperationId(), "", d.isOk() ? "ok" : "FALLA", "", limpio(d.toString()), "", "", "", "");
        if (!d.isOk()) {
            fallas.add(caso + " " + d);
        }
    }

    private void pausar() throws InterruptedException {
        if (!primero && !simular) {
            Thread.sleep(pausa);
        }
        primero = false;
    }

    private static String digest(SendResult r, SignedDocument d) {
        return r.getDigestValue() == null || r.getDigestValue().isEmpty() ? "sin dDigVal"
                : (r.getDigestValue().equals(d.getDigestValue()) && !r.isDigestMismatch() ? "sí" : "NO");
    }

    private static TreeSet<String> codigos(List<ValidationError> errores) {
        TreeSet<String> c = new TreeSet<>();
        for (ValidationError e : errores) {
            c.add(e.getCode() + (e.getPath() == null ? "" : "@" + e.getPath()));
        }
        return c;
    }

    private static String resumen(String xml) throws Exception {
        StringBuilder b = new StringBuilder();
        for (String campo : new String[] {"iTiDE", "iTipEmi", "cMoneOpe", "iCondOpe", "dRedon", "dTotGralOpe", "dTotalGs"}) {
            String v = XmlCanonico.texto(xml, campo);
            if (v != null) {
                b.append(b.length() == 0 ? "" : " ").append(campo).append('=').append(v);
            }
        }
        return b.toString();
    }

    private static int contar(String s, String sub) {
        if (s == null) {
            return 0;
        }
        int n = 0;
        for (int i = s.indexOf(sub); i >= 0; i = s.indexOf(sub, i + sub.length())) {
            n++;
        }
        return n;
    }

    private void guardar(String nombre, String contenido) throws java.io.IOException {
        if (!simular) {
            Files.writeString(dir.resolve("respuestas/" + nombre), String.valueOf(contenido), StandardCharsets.UTF_8);
        }
    }

    private static String limpio(String s) {
        return String.valueOf(s).replace('\t', ' ').replace('\n', ' ');
    }

    private void fila(String numero, String caso, String op, String cdc, String local, String status, String cod, String msg,
            String digest, String consulta, String c14n, String obs) throws java.io.IOException {
        String f = String.join("\t", LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString(), numero, caso, op, cdc, local,
                status, cod, msg, digest, consulta, c14n, obs);
        Files.write(res, List.of(f), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        System.out.println(f);
    }
}
