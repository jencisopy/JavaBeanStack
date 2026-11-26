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
package org.javabeanstack.sifen.test.fase12;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.javabeanstack.sifen.ISifenClient;
import org.javabeanstack.sifen.SifenClient;
import org.javabeanstack.sifen.api.SignedDocument;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.validation.ValidationError;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.test.casos.CasosFase12;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.javabeanstack.sifen.test.casos.XmlCanonico;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Casos grandes de la Fase 12 (plan JBSSIF, §12.2.5; S6): facturas de 500, 999 y 1000 ítems, la de 999 con
 * textos largos y la sonda de I12-23 (500 con textos largos, DE-56s; ciclo 3). Por caso: cantidad de ítems y total, tamaño del mensaje {@code rEnviDe}, tiempo de firma,
 * digest estable (dos generaciones dan el mismo {@code DigestValue}) y relectura con {@code readSigned} sin
 * cambiar un byte. DE-55 y DE-56 no tienen XML de referencia en el repositorio (§12.1): se afirman la
 * cantidad, el total y el digest. Deja la medición en {@code target/fase12/casos_grandes.tsv} y, con
 * {@code -Djbs.sifen.f12.grandes.salida=<carpeta>}, cada mensaje en esa carpeta (para el cotejo con el XSD vigente).
 *
 * <p>El límite de SIFEN para {@code siRecepDE} es de 1000 KB (BA01 · 0200, MT v150); la librería no tiene una
 * regla local de tamaño (M12-01), y DE-56x lo muestra: la validación acepta un mensaje que SIFEN rechazaría.</p>
 */
public class CasosGrandesTest {
    /** Límite del mensaje de {@code siRecepDE} (1000 KB, en la lectura más estricta: 1000 × 1000 bytes). */
    static final int LIMITE_SIRECEPDE = 1000 * 1000;
    /**
     * Digest de DE-55 y DE-56 con el certificado autofirmado (determinista, A3). Se fijaron en §12.2.5 el
     * 2026-11-25 y cambian solo si cambia el XML de esos documentos.
     */
    static final Map<String, String> DIGEST = Map.of(
            "DE-55", "K8Khw/4K24Cq4oZvvIovzNfrBeePTPF5O6SyJV9xFMg=",
            "DE-56", "7z+al2yHQtUi9LVXs9EICpuYw8OCxjXhgiV9ltDEhzg=");
    private static final List<String> FILAS = Collections.synchronizedList(new ArrayList<>());
    private static SifenConfig cfg;
    private static GenerationCtx ctx;
    private static ISifenClient cliente;

    @BeforeAll
    public static void configurar() throws Exception {
        cfg = FixturesSifen.configuracion();
        ctx = FixturesSifen.contexto(cfg);
        cliente = SifenClient.create(FixturesSifen.configuracionConValidacion());
    }

    @AfterAll
    public static void escribir() throws Exception {
        cliente.close();
        Path destino = Paths.get("target", "fase12", "casos_grandes.tsv");
        Files.createDirectories(destino.getParent());
        List<String> l = new ArrayList<>();
        l.add("caso\titems\tbytes rEnviDe\tKB (1024)\tms generar+firmar\tDigestValue\tnota");
        List<String> f = new ArrayList<>(FILAS);
        Collections.sort(f);
        l.addAll(f);
        Files.write(destino, l, StandardCharsets.UTF_8);
    }

    private static DocumentoElectronico caso(String id) {
        return CasosFase12.todos().get(id).get();
    }

    private static String medir(String id, int items, String nota) throws Exception {
        long t0 = System.nanoTime();
        String xml = caso(id).generarXml(ctx, cfg);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        int bytes = xml.getBytes(StandardCharsets.UTF_8).length;
        String digest = XmlCanonico.texto(xml, "DigestValue");
        String salida = System.getProperty("jbs.sifen.f12.grandes.salida");
        if (salida != null) {
            // ciclo 3 (I12-23): el mensaje a una carpeta, para validarlo contra el XSD vigente con sifen_xsd_cotejo.py
            // (firmado con el autofirmado del módulo; nunca al log)
            Files.createDirectories(Paths.get(salida));
            Files.writeString(Paths.get(salida, id + ".xml"), xml, StandardCharsets.UTF_8);
        }
        FILAS.add(id + "\t" + items + "\t" + bytes + "\t" + (bytes / 1024) + "\t" + ms + "\t" + digest + "\t" + nota);
        Assertions.assertEquals(items, XmlCanonico.parsear(xml).getElementsByTagNameNS("*", "gCamItem").getLength(), id);
        return xml;
    }

    private static void estableYReleible(String id, String xml) throws Exception {
        String otra = caso(id).generarXml(ctx, cfg);
        Assertions.assertEquals(XmlCanonico.texto(xml, "DigestValue"), XmlCanonico.texto(otra, "DigestValue"), id + ": digest estable");
        String esperado = DIGEST.get(id);
        if (esperado != null) {
            Assertions.assertEquals(esperado, XmlCanonico.texto(xml, "DigestValue"), id + ": digest fijado en §12.2.5");
        }
        SignedDocument firmado = cliente.sign(caso(id));
        SignedDocument releido = cliente.readSigned(firmado.getXml());
        Assertions.assertEquals(firmado.getDigestValue(), releido.getDigestValue(), id + ": readSigned conserva el digest");
        Assertions.assertArrayEquals(XmlCanonico.canonico(firmado.getXml()), XmlCanonico.canonico(releido.getXml()), id + ": readSigned no cambia un byte");
    }

    @Test
    public void facturaDe500Items() throws Exception {
        String xml = medir("DE-55", 500, "acepta; 0260 en vivo (§12.3.7)");
        Assertions.assertEquals("500000", XmlCanonico.texto(xml, "dTotGralOpe"));
        Assertions.assertTrue(xml.getBytes(StandardCharsets.UTF_8).length < LIMITE_SIRECEPDE, "DE-55 entra en 1000 KB");
        estableYReleible("DE-55", xml);
    }

    @Test
    public void facturaDe999ItemsElMaximo() throws Exception {
        String xml = medir("DE-56", 999, "acepta; 0260 en vivo (§12.3.7); queryDE con el mismo C14N (M5-07, I8-06)");
        Assertions.assertEquals("999000", XmlCanonico.texto(xml, "dTotGralOpe"));
        Assertions.assertTrue(xml.getBytes(StandardCharsets.UTF_8).length < LIMITE_SIRECEPDE, "DE-56 entra en 1000 KB");
        estableYReleible("DE-56", xml);
    }

    @Test
    public void facturaDe1000ItemsSeRechazaSinLlegarAFirmar() {
        SifenException e = Assertions.assertThrows(SifenException.class, () -> cliente.sign(caso("DE-56n")));
        Assertions.assertTrue(e.getErrors().stream().map(ValidationError::getCode).anyMatch("JBS-E-57"::equals), e.toString());
        FILAS.add("DE-56n\t1000\t-\t-\t-\t-\trechazo local JBS-E-57 en sign (" + e.getCode() + ")");
    }

    /**
     * I12-23 (decisión del usuario, 2026-11-25): la sonda de 500 ítems con textos largos queda entre DE-55 (349 KB, aprobada)
     * y DE-56 (691 KB, 0160 genérico) por bytes y es igual a DE-55 por cantidad de ítems; entra en 1000 KB, la validación
     * previa la acepta y se firma y relee sin cambiar un byte. Solo se envía si el reintento de DE-56 repite el 0160.
     */
    @Test
    public void sondaDe500ItemsLargos_I12_23() throws Exception {
        String xml = medir("DE-56s", 500, "acepta; sonda de I12-23 en 12.3.10, solo si el reintento de DE-56 repite el 0160 genérico");
        int bytes = xml.getBytes(StandardCharsets.UTF_8).length;
        Assertions.assertEquals("500000", XmlCanonico.texto(xml, "dTotGralOpe"));
        Assertions.assertTrue(bytes > 600 * 1024 && bytes < 700 * 1024, "DE-56s pesa unos 650 KB: " + bytes + " bytes");
        Assertions.assertTrue(bytes < LIMITE_SIRECEPDE, "DE-56s entra en 1000 KB");
        Assertions.assertTrue(cliente.validateDE(caso("DE-56s")).getErrors().isEmpty(), "la validación previa acepta la sonda");
        estableYReleible("DE-56s", xml);
    }

    /** M12-01: 999 ítems con textos largos pasan los 1000 KB y la validación previa no lo ve (SIFEN: 0200). */
    @Test
    public void facturaDeMasDe1000KbPasaLaValidacionLocal_M12_01() throws Exception {
        String xml = medir("DE-56x", 999, "acepta local, M12-01; SIFEN 0200 (exploratorio, §12.3.10)");
        Assertions.assertTrue(xml.getBytes(StandardCharsets.UTF_8).length > 1000 * 1024, "DE-56x pasa los 1000 KB");
        Assertions.assertTrue(cliente.validateDE(caso("DE-56x")).getErrors().isEmpty(), "ninguna regla local frena el tamaño (M12-01)");
    }
}
