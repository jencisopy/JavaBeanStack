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
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Supplier;
import org.javabeanstack.sifen.Sifen;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.beans.response.RespuestaRecepcionDE;
import org.javabeanstack.sifen.core.fields.request.de.TgCamItem;
import org.javabeanstack.sifen.core.fields.request.de.TgDatRec;
import org.javabeanstack.sifen.core.validation.DEValidator;
import org.javabeanstack.sifen.core.validation.ValidationError;
import org.javabeanstack.sifen.core.validation.ValidationResult;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.test.SifenTestEnv;
import org.javabeanstack.sifen.test.casos.BateriaIdentificacion;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Envíos en vivo de la Fase 3 (plan JBSSIF, §3.4 y §2.1.1–§2.1.5; decisiones del usuario del
 * 2026-11-24): inmobiliaria con y sin redondeo, anticipos, B2F e identificación del receptor. Ambiente de
 * PRUEBAS (D28), 001-001, numeración desde 1000 (ESC-8) reservada en la tabla de equivalencias antes de
 * cada envío. Validación previa apagada en el envío (se mide la respuesta de SIFEN a lo que genera la
 * librería); la predicción local se calcula aparte y se guarda junto a la respuesta. No reintenta.
 *
 * <p>Uso: {@code mvn -pl sifen -am test -Psifen-live -Dtest=SifenVivoFase3Test -Djbs.sifen.test.env=<env>
 * -Djbs.sifen.test.fase3.salida=<carpeta fuera del repo>}.</p>
 */
@Tag("sifen-live")
public class SifenVivoFase3Test {

    /** Consulta (sin reenviar) el estado de un CDC cuyo envío quedó cortado: {@code -Djbs.sifen.test.fase3.consultar=<cdc>}. */
    @Test
    public void consultarCortado() throws Exception {
        String cdc = System.getProperty("jbs.sifen.test.fase3.consultar");
        Assumptions.assumeTrue(cdc != null, "sin CDC a consultar");
        SifenTestEnv.Entorno entorno = SifenTestEnv.entornoOSaltar();
        SifenConfig cfg = entorno.getConfiguracion();
        Assertions.assertEquals(SifenConfig.Environment.DEV, cfg.getEnvironment(), "D28");
        org.javabeanstack.sifen.core.beans.response.RespuestaConsultaDE r = Sifen.consultaDE(cdc, cfg);
        Path dir = Paths.get(System.getProperty("jbs.sifen.test.fase3.salida"));
        Files.writeString(dir.resolve("respuestas/consulta-" + cdc + ".xml"), String.valueOf(r.getRespuestaBruta()), StandardCharsets.UTF_8);
        System.out.println("consultaDE " + cdc + " ⇒ " + r.getdCodRes() + " " + r.getdMsgRes());
    }

    /**
     * Diagnóstico local de JBS-CERT-11 (C:M3-44): muestra el documento que se lee del certificado
     * configurado y los sujetos (principal y SAN), sin contactar a SIFEN ni imprimir la clave. E3-33:
     * <b>no correrlo con el certificado real</b> salvo pedido expreso; la salida enmascara toda CI.
     */
    @Test
    public void diagnosticoCertificado() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("jbs.sifen.test.fase3.diagCert"), "sin diagnóstico de certificado");
        SifenTestEnv.Entorno entorno = SifenTestEnv.entornoOSaltar();
        SifenTestEnv.Emisor emisor = SifenTestEnv.emisorOSaltar();
        ValidationResult r = diagnostico(entorno.getConfiguracion(), emisor.getRuc(), emisor.getDv(), System.out);
        Assertions.assertFalse(r.contains("JBS-CERT-11"), r::toString);
    }

    /**
     * Cuerpo del diagnóstico, sin red (E3-33: lo ejercita {@code DiagnosticoCertificadoTest} con el
     * certificado ficticio): imprime en {@code out} el documento de cada sujeto enmascarado y el
     * resultado de JBS-CERT-11 sobre DE-05 con el emisor dado.
     *
     * @return el resultado de la coherencia.
     */
    static ValidationResult diagnostico(SifenConfig cfg, String ruc, String dv, java.io.PrintStream out) {
        org.javabeanstack.sifen.core.validation.support.LoadedCertificate c =
                org.javabeanstack.sifen.internal.cert.CertificateStore.get(cfg);
        out.println("certificado ⇒ cargado=" + c.isLoaded() + " numeroDocumento=" + enmascarar(c.getDocumentNumber()));
        if (c.getCertificate() != null) {
            for (org.javabeanstack.sifen.internal.cert.CertificateSubjects.CertificateSubject s
                    : org.javabeanstack.sifen.internal.cert.CertificateSubjects.of(c.getCertificate())) {
                out.println("sujeto ⇒ documento=" + enmascarar(s.documentNumber()));
            }
        }
        out.println("emisor ⇒ ruc=" + enmascarar(ruc) + " dv=" + dv);
        DocumentoElectronico de = CasosDePrueba.documentos().stream().filter(k -> k.getId().equals("DE-05"))
                .findFirst().orElseThrow().construir();
        de.getgDatGralOpe().getgEmis().setdRucEm(ruc);
        de.getgDatGralOpe().getgEmis().setdDVEmi(dv);
        ValidationResult r = org.javabeanstack.sifen.core.validation.ConfigValidator.standard().validateConsistency(de, cfg);
        out.println("JBS-CERT-11 ⇒ " + (r.contains("JBS-CERT-11") ? "rechaza" : "no rechaza") + " " + r);
        return r;
    }

    /** RUC de persona jurídica (80xxxxxx), con o sin prefijo y DV: lo único que se muestra (E3-33). */
    private static final java.util.regex.Pattern RUC_JURIDICA = java.util.regex.Pattern.compile("(RUC)?80[0-9]{6}(-[0-9])?");

    /**
     * Deja ver solo un RUC de persona jurídica ({@code RUC80123456-5}); toda otra cosa —una CI con o sin
     * DV, con prefijo {@code CI} o {@code RUC}— sale con las cifras ocultas (E3-33).
     */
    static String enmascarar(String documento) {
        if (documento == null || RUC_JURIDICA.matcher(documento.trim()).matches()) {
            return documento;
        }
        return documento.replaceAll("[0-9]", "*");
    }

    @Test
    public void enviarMuestra() throws Exception {
        SifenTestEnv.Entorno entorno = SifenTestEnv.entornoOSaltar();
        SifenTestEnv.Emisor emisor = SifenTestEnv.emisorOSaltar();
        String salida = System.getProperty("jbs.sifen.test.fase3.salida");
        Assumptions.assumeTrue(salida != null, "falta jbs.sifen.test.fase3.salida");
        Path dir = Paths.get(salida);
        Files.createDirectories(dir.resolve("respuestas"));
        Path tabla = Paths.get(System.getProperty(SifenVivoTest.PROP_EQUIVALENCIAS));
        SifenConfig cfg = entorno.getConfiguracion();
        Assertions.assertEquals(SifenConfig.Environment.DEV, cfg.getEnvironment(), "D28");
        // Por omisión la validación previa va apagada en el envío (se mide a SIFEN); con
        // -Djbs.sifen.test.fase3.conValidacion=true se envía por el camino de producción (C:M3-43).
        cfg.setPreValidationEnabled(Boolean.getBoolean("jbs.sifen.test.fase3.conValidacion"));
        GenerationCtx ctx = GenerationCtx.getDefaultFromConfig(cfg);
        DEValidator local = DEValidator.standard();
        boolean simular = Boolean.getBoolean("jbs.sifen.test.fase3.simular");
        if (simular) {
            // Ensayo en seco: copia temporal de la tabla, sin red.
            Path copia = Files.createTempFile("equivalencias-simulacion", ".csv");
            Files.copy(tabla, copia, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            tabla = copia;
        }
        ReemisionPrueba reemision = new ReemisionPrueba(emisor, entorno.getNumeroInicial(), tabla);

        Map<String, Supplier<DocumentoElectronico>> muestra = new LinkedHashMap<>();
        // Pedido del usuario: el B2F sin tipo de documento antes que DE-24 (B2F con pasaporte sin número).
        muestra.put("B2F-SIN-TIPO b2f sin iTipIDRec (D208g)", CasosDePrueba::facturaB2fSinTipoDocumento);
        for (CasosDePrueba.Caso c : CasosDePrueba.documentos()) {
            int n = Integer.parseInt(c.getId().substring(3));
            if (n >= 24 && n <= 32) {
                muestra.put(c.getId() + " " + c.getArchivo(), c::construir);
            }
        }
        Map<String, BateriaIdentificacion.Combinacion> combos = new LinkedHashMap<>();
        for (BateriaIdentificacion.Combinacion c : BateriaIdentificacion.combinaciones()) {
            combos.put(c.id(), c);
        }
        for (String id : new String[] {"ID-C06", "ID-C11", "ID-N06", "ID-N10", "ID-N12", "ID-I02", "ID-I03", "ID-I05",
            "ID-F02", "ID-R01"}) {
            BateriaIdentificacion.Combinacion c = combos.get(id);
            muestra.put(id + " " + c.descripcion(), c::construir);
        }

        // E3-23: «Otro» en el tipo de pago (99) con texto libre.
        muestra.put("OTRO-TIPAGO factura PYG contado con iTiPago 99 y dDesTiPag libre", () -> {
            DocumentoElectronico de = CasosDePrueba.documentos().stream().filter(c -> c.getId().equals("DE-05"))
                    .findFirst().orElseThrow().construir();
            org.javabeanstack.sifen.core.fields.request.de.TgPaConEIni pago = de.getgDtipDE().getgCamCond().getgPaConEIniList().get(0);
            pago.setiTiPago(org.javabeanstack.sifen.core.types.TiTiPago.OTRO);
            pago.setdDesTiPag("Pago con QR de billetera");
            return de;
        });
        // §2.1.6: cierre del IVA en PYG con decimales (como hoy) y con enteros (como el fork antes del ajuste).
        for (long monto : new long[] {1015839L, 1055434L, 1102948L}) {
            muestra.put("CIE-" + monto + "-DEC lote 5 % sobre el 30 % con decimales", () -> cierre(monto, false));
            muestra.put("CIE-" + monto + "-ENT lote 5 % sobre el 30 % con enteros", () -> cierre(monto, true));
        }
        // C:M3-44: varios ítems parciales con el subtotal a ±0,02/±0,03 (ejemplos del barrido), con decimales.
        muestra.put("MULTI-1 3 lotes 5 %/30 % + 10 % + exento (dSub5 −0,03)",
                () -> cierreVarios(5, new long[] {348504L, 777412L, 773559L}, 621018L, 542318L));
        muestra.put("MULTI-2 2 lotes 5 %/30 % + 10 % + exento (dSub5 +0,02)",
                () -> cierreVarios(5, new long[] {546311935L, 4649559L}, 844799572L, 837424973L));
        muestra.put("MULTI-3 3 lotes 10 %/30 % + 10 % + exento (dSub10 +0,02)",
                () -> cierreVarios(10, new long[] {513782893L, 493158916L, 358702588L}, 1926206L, 862431869L));
        String solo = System.getProperty("jbs.sifen.test.fase3.solo");
        if (solo != null) {
            java.util.Set<String> ids = new java.util.HashSet<>(java.util.Arrays.asList(solo.split(",")));
            muestra.keySet().removeIf(k -> !ids.contains(k.split(" ")[0]));
        }
        Path res = dir.resolve(simular ? "simulacion.tsv" : "resultados.tsv");
        if (!Files.exists(res)) {
            Files.write(res, List.of("numero\tcaso\tcdc\tlocal\tcodigoEstado\tdCodRes\tdEstRes\tdMsgRes"), StandardCharsets.UTF_8);
        }
        String cdcAnticipoOriginal = CasosDePrueba.cdcAnticipo();
        boolean anticipoAprobado = false;
        // Datos del emisor de prueba tal como están en una venta real de test8 (actividad económica,
        // dirección): con los del emisor ficticio SIFEN rechaza 1261 (actividad económica incorrecta).
        Path ventas = Paths.get(System.getProperty(SifenVivoTest.PROP_VENTAS));
        org.javabeanstack.sifen.core.fields.request.de.TgEmis emisorReal;
        try (java.nio.file.DirectoryStream<Path> ds = Files.newDirectoryStream(ventas, "venta-*.xml")) {
            Path primera = ds.iterator().next();
            emisorReal = new DocumentoElectronico(Files.readString(primera, StandardCharsets.UTF_8)).getgDatGralOpe().getgEmis();
        }
        EMISOR_REAL = emisorReal;
        for (Map.Entry<String, Supplier<DocumentoElectronico>> e : muestra.entrySet()) {
            String caso = e.getKey();
            try {
                if ((caso.startsWith("DE-31") || caso.startsWith("DE-32")) && !anticipoAprobado) {
                    fila(res, "—\t" + caso + "\t\t\t\t\t\tno enviado: la factura de anticipo (DE-30) no quedó aprobada en esta corrida");
                    continue;
                }
                String cod = enviar(caso, e.getValue().get(), emisor, reemision, local, ctx, cfg, dir, res, cdcAnticipoOriginal, simular);
                if (caso.startsWith("DE-30") && "0260".equals(cod)) {
                    anticipoAprobado = true;
                }
            } catch (org.javabeanstack.sifen.core.exceptions.SifenConnectionException x) {
                fila(res, "—\t" + caso + "\t\t\t\t\t\tcorte por conexión: " + x.getMessage());
                throw x; // sin reintentos
            } catch (Exception x) {
                fila(res, "—\t" + caso + "\t\t\t\t\t\terror: " + x);
            }
        }
    }

    /**
     * Camino de producción con el certificado real (C:M3-43, M3-53, E3-32): {@code validacionPrevia} en
     * su valor por omisión ({@code true}); una factura por {@code recepcionDE} y un lote de dos por
     * {@code recepcionLoteDE}, cada uno por toda la cadena de la fachada (configuración, contenido,
     * JBS-CERT-11, coherencia del lote y envío). Un rechazo local es un hallazgo: se registra y falla.
     * {@code -Djbs.sifen.test.fase3.produccion=true}.
     */
    @Test
    public void enviarConValidacionActiva() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("jbs.sifen.test.fase3.produccion"), "sin envío por el camino de producción");
        SifenTestEnv.Entorno entorno = SifenTestEnv.entornoOSaltar();
        SifenTestEnv.Emisor emisor = SifenTestEnv.emisorOSaltar();
        Path dir = Paths.get(System.getProperty("jbs.sifen.test.fase3.salida"));
        Files.createDirectories(dir.resolve("respuestas"));
        SifenConfig cfg = entorno.getConfiguracion();
        Assertions.assertEquals(SifenConfig.Environment.DEV, cfg.getEnvironment(), "D28");
        Assertions.assertTrue(new SifenConfig().isPreValidationEnabled(), "el valor por omisión de producción es con validación");
        Assertions.assertTrue(cfg.isPreValidationEnabled(), "la configuración del entorno no apaga la validación");
        ValidationResult vc = Sifen.validarConfiguracion(cfg);
        System.out.println("validarConfiguracion ⇒ " + vc);
        Assertions.assertTrue(vc.isValid(), () -> "la configuración real no pasa: " + vc);
        cargarEmisorReal();
        ReemisionPrueba reemision = new ReemisionPrueba(emisor, entorno.getNumeroInicial(),
                Paths.get(System.getProperty(SifenVivoTest.PROP_EQUIVALENCIAS)));
        Path res = dir.resolve("resultados.tsv");
        long pausa = Long.getLong("jbs.sifen.test.fase3.pausa", 3000L);

        // 1) factura simple por recepcionDE
        DocumentoElectronico simple = prepararProduccion("VALACT-1", emisor);
        ReemisionPrueba.Reemision r1 = reemision.reemitir("VALACT-1", simple, LocalDateTime.now().minusMinutes(1).truncatedTo(ChronoUnit.SECONDS));
        RespuestaRecepcionDE resp;
        try {
            resp = Sifen.recepcionDE(simple, cfg);
        } catch (org.javabeanstack.sifen.core.validation.SifenValidationException x) {
            fila(res, r1.enviado + "\tVALACT-1 factura simple, validación previa activa\t" + r1.cdc + "\trechaza local\t\t\t\t" + x.getMessage());
            throw x;
        }
        Files.writeString(dir.resolve("respuestas/" + r1.cdc + ".xml"), String.valueOf(resp.getRespuestaBruta()), StandardCharsets.UTF_8);
        String cod = resp.getxProtDE() != null && !resp.getxProtDE().getgResProc().isEmpty()
                ? resp.getxProtDE().getgResProc().get(0).getdCodRes() : resp.getdCodRes();
        String msg = resp.getxProtDE() != null && !resp.getxProtDE().getgResProc().isEmpty()
                ? resp.getxProtDE().getgResProc().get(0).getdMsgRes() : resp.getdMsgRes();
        String est = resp.getxProtDE() != null ? resp.getxProtDE().getdEstRes() : null;
        reemision.registrar("VALACT-1", r1, "recepcionDE", resp.getCodigoEstado(), cod, est, msg);
        fila(res, r1.enviado + "\tVALACT-1 factura simple, validación previa activa\t" + r1.cdc + "\tacepta\t"
                + resp.getCodigoEstado() + "\t" + cod + "\t" + est + "\t" + msg);

        // 2) lote de dos por recepcionLoteDE (coherenciaLote, JBS-CERT-11 por DE)
        Thread.sleep(pausa);
        List<DocumentoElectronico> des = new java.util.ArrayList<>();
        List<ReemisionPrueba.Reemision> rs = new java.util.ArrayList<>();
        for (String id : new String[] {"VALACT-2", "VALACT-3"}) {
            DocumentoElectronico de = prepararProduccion(id, emisor);
            rs.add(reemision.reemitir(id, de, LocalDateTime.now().minusMinutes(1).truncatedTo(ChronoUnit.SECONDS)));
            des.add(de);
        }
        org.javabeanstack.sifen.core.beans.response.RespuestaRecepcionLoteDE rl;
        try {
            rl = Sifen.recepcionLoteDE(des, cfg);
        } catch (org.javabeanstack.sifen.core.validation.SifenValidationException x) {
            for (ReemisionPrueba.Reemision r : rs) {
                fila(res, r.enviado + "\tVALACT lote, validación previa activa\t" + r.cdc + "\trechaza local\t\t\t\t" + x.getMessage());
            }
            throw x;
        }
        Files.writeString(dir.resolve("respuestas/recepcion-lote-" + rl.getdProtConsLote() + ".xml"),
                String.valueOf(rl.getRespuestaBruta()), StandardCharsets.UTF_8);
        for (int i = 0; i < rs.size(); i++) {
            reemision.registrar("VALACT-" + (i + 2), rs.get(i), "recepcionLoteDE:" + rl.getdProtConsLote(),
                    rl.getCodigoEstado(), rl.getdCodRes(), null, rl.getdMsgRes());
        }
        System.out.println("recepcionLoteDE ⇒ " + rl.getCodigoEstado() + " " + rl.getdCodRes() + " " + rl.getdMsgRes()
                + " lote " + rl.getdProtConsLote());
        Assertions.assertEquals("0300", rl.getdCodRes(), "recepcionLoteDE: " + rl.getdMsgRes());
        org.javabeanstack.sifen.core.beans.response.RespuestaConsultaLoteDE cl = null;
        for (int i = 0; i < 6; i++) {
            Thread.sleep(10_000L);
            cl = Sifen.consultaLoteDE(rl.getdProtConsLote(), cfg);
            if (!"0361".equals(cl.getdCodResLot())) {
                break;
            }
        }
        Files.writeString(dir.resolve("respuestas/consulta-lote-" + rl.getdProtConsLote() + ".xml"),
                String.valueOf(cl.getRespuestaBruta()), StandardCharsets.UTF_8);
        Map<String, org.javabeanstack.sifen.core.fields.response.batch.TgResProcLote> porCdc = new LinkedHashMap<>();
        for (org.javabeanstack.sifen.core.fields.response.batch.TgResProcLote p : cl.getgResProcLoteList()) {
            porCdc.put(p.getId(), p);
        }
        for (int i = 0; i < rs.size(); i++) {
            ReemisionPrueba.Reemision r = rs.get(i);
            org.javabeanstack.sifen.core.fields.response.batch.TgResProcLote p = porCdc.get(r.cdc);
            String c = p == null || p.getgResProc().isEmpty() ? cl.getdCodResLot() : p.getgResProc().get(0).getdCodRes();
            String m = p == null || p.getgResProc().isEmpty() ? cl.getdMsgResLot() : p.getgResProc().get(0).getdMsgRes();
            String e = p == null ? null : p.getdEstRes();
            reemision.registrar("VALACT-" + (i + 2), r, "consultaLoteDE:" + rl.getdProtConsLote(), cl.getCodigoEstado(), c, e, m);
            fila(res, r.enviado + "\tVALACT-" + (i + 2) + " lote " + rl.getdProtConsLote() + ", validación previa activa\t" + r.cdc
                    + "\tacepta\t" + cl.getCodigoEstado() + "\t" + c + "\t" + e + "\t" + m);
        }
    }

    /** DE-05 (factura PYG contado) con el emisor real de test8 y el receptor contribuyente de prueba. */
    private static DocumentoElectronico prepararProduccion(String id, SifenTestEnv.Emisor emisor) {
        DocumentoElectronico de = CasosDePrueba.documentos().stream().filter(c -> c.getId().equals("DE-05"))
                .findFirst().orElseThrow().construir();
        de.getgDatGralOpe().setgEmis(copiaEmisor());
        TgDatRec r = de.getgDatGralOpe().getgDatRec();
        if (r.getdRucRec() != null) {
            r.setdRucRec(emisor.getRuc());
            r.setdDVRec(Short.parseShort(emisor.getDv()));
        }
        de.getgTimb().setdEst("999");
        de.getgTimb().setdPunExp("999");
        return de;
    }

    /** Datos del emisor tal como están en una venta real de test8 (con el ficticio SIFEN rechaza 1261). */
    private static void cargarEmisorReal() throws java.io.IOException {
        Path ventas = Paths.get(System.getProperty(SifenVivoTest.PROP_VENTAS));
        try (java.nio.file.DirectoryStream<Path> ds = Files.newDirectoryStream(ventas, "venta-*.xml")) {
            Path primera = ds.iterator().next();
            EMISOR_REAL = new DocumentoElectronico(Files.readString(primera, StandardCharsets.UTF_8)).getgDatGralOpe().getgEmis();
        } catch (org.javabeanstack.sifen.core.exceptions.SifenException e) {
            throw new java.io.IOException(e);
        }
    }

    /** DE-26 reducido al lote (5 % sobre el 30 %, PYG, sin redondeo) con el importe dado. */
    static DocumentoElectronico cierre(long monto, boolean enteros) {
        DocumentoElectronico de = CasosDePrueba.documentos().stream().filter(c -> c.getId().equals("DE-26"))
                .findFirst().orElseThrow().construir();
        List<TgCamItem> items = de.getgDtipDE().getgCamItemList();
        TgCamItem lote = items.get(0);
        items.clear();
        items.add(lote);
        lote.getgValorItem().setdPUniProSer(java.math.BigDecimal.valueOf(monto));
        de.getgDtipDE().getgCamCond().getgPaConEIniList().get(0).setdMonTiPag(java.math.BigDecimal.valueOf(monto));
        de.setgTotSub(enteros ? new org.javabeanstack.sifen.test.calculo.TgTotSubEnteros(false)
                : new org.javabeanstack.sifen.core.fields.request.de.TgTotSub(false));
        return de;
    }

    /** DE-26 con varios lotes parciales (tasa/30 %), un ítem al 10 % pleno y uno exento; PYG, sin redondeo. */
    static DocumentoElectronico cierreVarios(int tasa, long[] parciales, long pleno10, long exento) {
        DocumentoElectronico de = CasosDePrueba.documentos().stream().filter(c -> c.getId().equals("DE-26"))
                .findFirst().orElseThrow().construir();
        List<TgCamItem> items = de.getgDtipDE().getgCamItemList();
        TgCamItem lote = items.get(0);
        TgCamItem adm = items.get(1);
        TgCamItem exe = items.get(2);
        items.clear();
        long total = 0;
        for (int i = 0; i < parciales.length; i++) {
            TgCamItem it = i == 0 ? lote : copiaLote(lote);
            it.setdCodInt("LOTE-" + (15 + i));
            it.getgValorItem().setdPUniProSer(java.math.BigDecimal.valueOf(parciales[i]));
            it.getgCamIVA().setdTasaIVA(java.math.BigDecimal.valueOf(tasa));
            items.add(it);
            total += parciales[i];
        }
        adm.getgValorItem().setdPUniProSer(java.math.BigDecimal.valueOf(pleno10));
        exe.setdCantProSer(java.math.BigDecimal.ONE);
        exe.getgValorItem().setdPUniProSer(java.math.BigDecimal.valueOf(exento));
        items.add(adm);
        items.add(exe);
        total += pleno10 + exento;
        de.getgDtipDE().getgCamCond().getgPaConEIniList().get(0).setdMonTiPag(java.math.BigDecimal.valueOf(total));
        return de;
    }

    private static TgCamItem copiaLote(TgCamItem o) {
        TgCamItem it = new TgCamItem();
        it.setdDesProSer(o.getdDesProSer());
        it.setcUniMed(o.getcUniMed());
        it.setdCantProSer(o.getdCantProSer());
        org.javabeanstack.sifen.core.fields.request.de.TgValorItem v = new org.javabeanstack.sifen.core.fields.request.de.TgValorItem();
        org.javabeanstack.sifen.core.fields.request.de.TgValorRestaItem r = new org.javabeanstack.sifen.core.fields.request.de.TgValorRestaItem();
        r.setdDescItem(java.math.BigDecimal.ZERO);
        r.setdDescGloItem(java.math.BigDecimal.ZERO);
        v.setgValorRestaItem(r);
        it.setgValorItem(v);
        org.javabeanstack.sifen.core.fields.request.de.TgCamIVA iva = new org.javabeanstack.sifen.core.fields.request.de.TgCamIVA();
        iva.setiAfecIVA(o.getgCamIVA().getiAfecIVA());
        iva.setdPropIVA(o.getgCamIVA().getdPropIVA());
        iva.setdTasaIVA(o.getgCamIVA().getdTasaIVA());
        it.setgCamIVA(iva);
        return it;
    }

    private static org.javabeanstack.sifen.core.fields.request.de.TgEmis EMISOR_REAL;

    /** Un gEmis nuevo con los datos de la venta real (la reemisión pisa RUC, DV, nombre y correo). */
    private static org.javabeanstack.sifen.core.fields.request.de.TgEmis copiaEmisor() {
        org.javabeanstack.sifen.core.fields.request.de.TgEmis e = new org.javabeanstack.sifen.core.fields.request.de.TgEmis();
        org.javabeanstack.sifen.core.fields.request.de.TgEmis o = EMISOR_REAL;
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

    private static void fila(Path res, String fila) throws java.io.IOException {
        Files.write(res, List.of(fila), StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND);
        System.out.println(fila);
    }

    private static String enviar(String caso, DocumentoElectronico de, SifenTestEnv.Emisor emisor, ReemisionPrueba reemision,
            DEValidator local, GenerationCtx ctx, SifenConfig cfg, Path dir, Path res, String cdcAnticipoOriginal,
            boolean simular) throws Exception {
        {
            de.getgDatGralOpe().setgEmis(copiaEmisor());
            TgDatRec r = de.getgDatGralOpe().getgDatRec();
            // Receptor contribuyente: el RUC del propio emisor de prueba (existe en SIFEN), salvo el
            // caso del RUC inexistente; el ficticio 80654321 daría 1306 en todos.
            if (r.getdRucRec() != null && !caso.startsWith("ID-C06")) {
                r.setdRucRec(emisor.getRuc());
                r.setdDVRec(Short.parseShort(emisor.getDv()));
            }
            if (caso.startsWith("DE-30")) {
                de.setId(cdcAnticipoOriginal); // para que la reemisión registre la equivalencia del CDC
            }
            if (caso.startsWith("DE-31") || caso.startsWith("DE-32")) {
                String nuevo = reemision.cdcReemitido(cdcAnticipoOriginal);
                for (TgCamItem it : de.getgDtipDE().getgCamItemList()) {
                    it.setdCDCAnticipo(nuevo);
                }
            }
            // DE sintético (no es una venta de producción): se marca su origen como 999-999 para que la
            // guarda D28 de la reemisión compare contra algo distinto del 001-001 de destino.
            de.getgTimb().setdEst("999");
            de.getgTimb().setdPunExp("999");
            LocalDateTime ahora = LocalDateTime.now().minusMinutes(1).truncatedTo(ChronoUnit.SECONDS);
            ReemisionPrueba.Reemision re = reemision.reemitir(caso.split(" ")[0], de, ahora);
            ValidationResult v = local.validate(de, ctx);
            TreeSet<String> codigos = new TreeSet<>();
            for (ValidationError err : v.getErrors()) {
                codigos.add(err.getCode());
            }
            String pred = codigos.isEmpty() ? "acepta" : "rechaza " + codigos;
            String pedido = de.generarXml(ctx, cfg);
            // Pausa entre envíos: diez seguidos en el mismo segundo terminaron en un corte de conexión
            // (JBS-CONEX-07) en la corrida del 2026-11-24.
            Thread.sleep(Long.getLong("jbs.sifen.test.fase3.pausa", 0L));
            if (simular) {
                Files.createDirectories(dir.resolve("simulacion"));
                Files.writeString(dir.resolve("simulacion/" + caso.split(" ")[0] + ".pedido.xml"), pedido, StandardCharsets.UTF_8);
                fila(res, re.enviado + "\t" + caso + "\t" + re.cdc + "\t" + pred + "\t\t\t\tsimulado (XML " + pedido.length() + " caracteres)");
                return null;
            }
            Files.writeString(dir.resolve("respuestas/" + re.cdc + ".pedido.xml"), pedido, StandardCharsets.UTF_8);
            RespuestaRecepcionDE resp = Sifen.recepcionDE(de, cfg);
            Files.writeString(dir.resolve("respuestas/" + re.cdc + ".xml"), String.valueOf(resp.getRespuestaBruta()), StandardCharsets.UTF_8);
            String cod = resp.getxProtDE() != null && !resp.getxProtDE().getgResProc().isEmpty()
                    ? resp.getxProtDE().getgResProc().get(0).getdCodRes() : resp.getdCodRes();
            String msg = resp.getxProtDE() != null && !resp.getxProtDE().getgResProc().isEmpty()
                    ? resp.getxProtDE().getgResProc().get(0).getdMsgRes() : resp.getdMsgRes();
            String est = resp.getxProtDE() != null ? resp.getxProtDE().getdEstRes() : null;
            reemision.registrar(caso.split(" ")[0], re, "recepcionDE", resp.getCodigoEstado(), cod, est, msg);
            fila(res, re.enviado + "\t" + caso + "\t" + re.cdc + "\t" + pred + "\t" + resp.getCodigoEstado() + "\t" + cod
                    + "\t" + est + "\t" + String.valueOf(msg).replace('\t', ' ').replace('\n', ' '));
            return cod;
        }
    }
}
