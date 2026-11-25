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
package org.javabeanstack.sifen.internal.request;

import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.internal.helpers.SignatureHelper;
import org.javabeanstack.sifen.internal.util.SifenUtil;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Ni la validación de una firma ni el envío por lote dejan archivos en {@code java.io.tmpdir} (plan JBSSIF,
 * §4.7: E4-01 y E3-10). La línea base escribía el XML recibido en un {@code <uuid>.xml} y el lote en un
 * {@code DE_ddMMyyyy*.zip}, con datos de clientes, y no los borraba.
 *
 * <p>La carpeta temporal es compartida con otros procesos (otras corridas de la suite en la misma máquina):
 * el test no cuenta archivos sueltos sino los nuevos que <b>contienen una marca única</b> de esta corrida (un
 * comentario fuera del {@code rDE} en el XML; un {@code dCodSeg} al azar, y por lo tanto un CDC único, en el
 * lote, que se busca dentro del ZIP descomprimido), recorriendo la carpeta en profundidad (I4-08). Contra la
 * línea base los dos primeros fallan. Un barrido de los fuentes de {@code src/main} cubre los caminos que los
 * tests no ejercitan (envío, respuestas, eventos).</p>
 */
public class SinTemporalesTest {
    private static SifenConfig cfg;
    private static GenerationCtx ctx;
    private static final Path TMP = Paths.get(System.getProperty("java.io.tmpdir"));

    @BeforeAll
    public static void configurar() {
        cfg = FixturesSifen.configuracion();
        ctx = FixturesSifen.contexto(cfg);
    }

    /** Archivos de la carpeta temporal, en profundidad (I4-08: también los de una subcarpeta nueva). */
    private static Set<Path> archivos() throws IOException {
        Set<Path> todos = new HashSet<>();
        Files.walkFileTree(TMP, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (attrs.isRegularFile()) {
                    todos.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE; // de otro usuario o borrado mientras se recorre
            }
        });
        return todos;
    }

    /** Archivos nuevos de la carpeta temporal cuyo contenido (o el de sus entradas, si es un ZIP) lleva la marca. */
    private static List<Path> nuevosConMarca(Set<Path> antes, String marca) throws IOException {
        List<Path> con = new ArrayList<>();
        for (Path p : archivos()) {
            if (antes.contains(p)) {
                continue;
            }
            byte[] b;
            try {
                b = Files.readAllBytes(p);
            } catch (IOException e) {
                continue; // de otro proceso, sin permiso o ya borrado
            }
            if (new String(b, StandardCharsets.ISO_8859_1).contains(marca) || zipConMarca(p, marca)) {
                con.add(p);
            }
        }
        return con;
    }

    private static boolean zipConMarca(Path p, String marca) {
        try (InputStream in = Files.newInputStream(p); ZipInputStream z = new ZipInputStream(in)) {
            for (ZipEntry e = z.getNextEntry(); e != null; e = z.getNextEntry()) {
                if (new String(z.readAllBytes(), StandardCharsets.UTF_8).contains(marca)) {
                    return true;
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            // no es un ZIP
        }
        return false;
    }

    /** E4-01: validar la firma de un XML en texto no lo escribe en un temporal. */
    @Test
    public void validarLaFirmaNoDejaTemporales() throws Exception {
        String marca = "jbs-sifen-E4-01-" + UUID.randomUUID();
        String xml = CasosDePrueba.documentos().get(4).construir().generateSignedXml(cfg) + "<!-- " + marca + " -->";
        Set<Path> antes = archivos();
        Assertions.assertTrue(SignatureHelper.validateSignature(xml, "XML").isValido(), "la marca está fuera del rDE: la firma valida");
        Assertions.assertTrue(SignatureHelper.validateSignatureOrThrow(xml, "XML").isValido());
        Assertions.assertEquals(List.of(), nuevosConMarca(antes, marca), "temporales con el XML validado (E4-01)");
    }

    /** E3-10: armar el pedido de lote (por el camino heredado y con los firmados) no deja el ZIP en un temporal. */
    @Test
    public void elLoteNoDejaTemporales() throws Exception {
        String codSeg = String.format("%09d", (long) (Math.random() * 1_000_000_000L));
        DocumentoElectronico de = CasosDePrueba.documentos().get(4).construir();
        de.getgOpeDE().setdCodSeg(codSeg);
        String cdc = de.obtenerCDC();
        Set<Path> antes = archivos();

        ReqRecLoteDe heredado = new ReqRecLoteDe(1L, cfg);
        heredado.setDEList(List.of(de));
        heredado.setupSoapMessage(ctx);

        DocumentoElectronico otro = CasosDePrueba.documentos().get(4).construir();
        otro.getgOpeDE().setdCodSeg(codSeg);
        ReqRecLoteDe firmados = new ReqRecLoteDe(2L, cfg);
        firmados.setSignedXmlList(List.of(otro.generateSignedXml(cfg)));
        firmados.setupSoapMessage(ctx);

        SifenUtil.compressXmlToZip("<rLoteDE>" + cdc + "</rLoteDE>");
        Assertions.assertEquals(List.of(), nuevosConMarca(antes, cdc), "temporales con el lote (E3-10)");
    }

    /** I4-08: el recorrido ve un archivo con la marca dentro de una subcarpeta nueva de {@code java.io.tmpdir}. */
    @Test
    public void detectaUnTemporalEnUnaSubcarpeta() throws Exception {
        String marca = "jbs-sifen-I4-08-" + UUID.randomUUID();
        Set<Path> antes = archivos();
        Path dir = Files.createTempDirectory("jbs-sifen-i408");
        try {
            Path f = Files.writeString(dir.resolve("lote.xml"), "<rLoteDE>" + marca + "</rLoteDE>");
            Assertions.assertEquals(List.of(f), nuevosConMarca(antes, marca));
        } finally {
            try (Stream<Path> s = Files.walk(dir)) {
                for (Path q : s.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(q);
                }
            }
        }
    }

    /**
     * I4-08 (CA4-06 sin {@code grep} a mano): ningún fuente de {@code src/main} escribe archivos salvo los de la
     * lista blanca —la traza activable de la conexión ({@code ExchangeTrace}, §5.4) y
     * {@code DocumentoElectronico.generarXml(…, ruta)}, que escribe donde pide la aplicación—.
     */
    @Test
    public void ningunFuenteEscribeArchivosFueraDeLaListaBlanca() throws Exception {
        java.util.regex.Pattern escritura = java.util.regex.Pattern.compile(
                "createTempFile|createTempDirectory|new FileOutputStream|Files\\.write|Files\\.newOutputStream|new FileWriter|Files\\.copy|new RandomAccessFile");
        Set<String> listaBlanca = Set.of("ExchangeTrace.java", "DocumentoElectronico.java");
        Path main = Paths.get("src", "main", "java");
        Assertions.assertTrue(Files.isDirectory(main), "se corre desde el módulo sifen: " + main.toAbsolutePath());
        List<String> fuera = new ArrayList<>();
        try (Stream<Path> s = Files.walk(main)) {
            for (Path f : s.filter(q -> q.toString().endsWith(".java")).toList()) {
                List<String> lineas = Files.readAllLines(f, StandardCharsets.UTF_8);
                for (int n = 0; n < lineas.size(); n++) {
                    String l = lineas.get(n).trim();
                    if (!l.startsWith("*") && !l.startsWith("//") && escritura.matcher(l).find()
                            && !listaBlanca.contains(f.getFileName().toString())) {
                        fuera.add(f + ":" + (n + 1) + ": " + l);
                    }
                }
            }
        }
        Assertions.assertEquals(List.of(), fuera, "escrituras a archivo fuera de la lista blanca (E3-10, E4-01)");
    }
}
