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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Foto de las firmas públicas del contrato de API v1 (plan JBSSIF, §10.5, CA10-03, RNF9; contrato en
 * {@code Jbs_sifen_api_contrato_maker.md}, «Paquetes del contrato» y «Política de compatibilidad»).
 *
 * <p>La foto {@code src/test/resources/contract/api_v1.txt} se fijó en §10.5 (2026-11-24), después de §10.3 y
 * §10.4: {@link #laFotoNoPierdeNiCambiaLineas} falla si falta o si una de sus líneas se pierde o cambia. Corren
 * además: que los tipos del contrato existan y sean públicos, y que <b>ninguna firma del contrato nombre un
 * tipo de {@code internal.*}</b> (I10-01). La foto se regenera con {@code -Djbs.sifen.contrato=<archivo>}
 * ({@link #generarFoto}); reemplazar la fijada solo se hace con una decisión registrada en el plan (dentro de
 * {@code 2.x} solo se agregan líneas).</p>
 *
 * <p>Regla de la foto: agregar líneas es libre; quitar o cambiar una línea falla (dentro de {@code 2.x} solo
 * se agrega). Una línea por miembro, {@code <tipo> <firma genérica>}, ordenadas. Dos reglas según el tipo:</p>
 * <ul>
 * <li><b>Completa</b> (fachada, {@code api}, {@code spi}, {@code SifenConfig}, excepciones, validación y los
 * enums de {@code core.types}): cabecera del tipo (superclase e interfaces) y todos los miembros públicos y
 * protegidos declarados.</li>
 * <li><b>Modelo de entrada</b> (D35: {@code DocumentoElectronico}, {@code EventosDE} y los grupos
 * {@code Tg*}/{@code Td*}/{@code Tr*} de {@code core.fields.request.de} y {@code .event}): constructores
 * públicos, getters ({@code get*}/{@code is*} sin parámetros), setters ({@code set*} de un parámetro) y
 * constantes; la superclase queda fuera (es interna y la Fase 7 la reemplaza), y también {@code setupDE(GenerationCtx, …)},
 * que no es un setter (I10-13).</li>
 * </ul>
 * <p>En las dos quedan fuera {@code generarXml}, {@code obtenerCDC}, {@code setupSOAPElements} y
 * {@code setValueFromChildNode} (el contrato los declara «no es contrato») y los miembros sintéticos; desde
 * D43 (§10.7) también {@code getSignedXml}, {@code generateSignedXml}, {@code setupDE}, {@code markSent} y
 * {@code withContext}.</p>
 *
 * <p><b>Dos fotos</b> (D43, §10.7): lo anotado con {@link Provisional} —o declarado dentro de un tipo anotado— va
 * a {@code contract/api_v1_provisional.txt}, que tiene que coincidir <b>exactamente</b> con lo actual (un cambio
 * obliga a regenerarla en el mismo cambio y anotarlo en la bitácora, sin ser ruptura); lo demás va a
 * {@code contract/api_v1.txt}, que no pierde ni cambia una línea. Además, un método abstracto nuevo de una
 * interfaz del contrato tiene que ser {@code default} (M10-06): agregar uno abstracto rompe a quien la
 * implementa.</p>
 */
public class ApiSignaturesTest {
    private static final String FOTO = "contract/api_v1.txt";
    private static final String FOTO_PROVISIONAL = "contract/api_v1_provisional.txt";
    private static final String INTERNAL = "org.javabeanstack.sifen.internal.";
    private static final Set<String> FUERA = Set.of("generarXml", "obtenerCDC", "setupSOAPElements", "setValueFromChildNode",
            "getSignedXml", "generateSignedXml", "setupDE", "markSent", "withContext");
    private static final String R = "org.javabeanstack.sifen.";

    /** Tipos con la regla completa (tabla «Paquetes del contrato»); los anidados públicos se suman solos. */
    static final List<String> COMPLETOS = List.of(
            R + "ISifenClient", R + "SifenClient", R + "SifenClientRegistry", R + "IConfigSupplier", R + "Provisional",
            R + "api.SignedDocument", R + "api.SendResult", R + "api.BatchSendResult", R + "api.BatchQueryResult",
            R + "api.DEQueryResult", R + "api.RucQueryResult", R + "api.EventResult", R + "api.ResultStatus",
            R + "api.BatchStatus", R + "api.SifenMessage", R + "api.NumberRange", R + "api.CertificateInfo",
            R + "api.ConnectionDiagnosis", R + "api.SignatureVerification",
            // §8.8 (M3-30): conciliación, PROVISIONAL (todo en api_v1_provisional.txt)
            R + "SifenReconciler", R + "api.ReconciliationStatus", R + "api.ReconciliationItem",
            R + "api.ReconciliationResult",
            R + "spi.ISendRegistry",
            R + "core.SifenConfig",
            R + "core.exceptions.SifenException", R + "core.exceptions.SifenConnectionException",
            R + "core.exceptions.ErrorCategory", R + "core.exceptions.Operation", R + "core.exceptions.ErrorCode",
            R + "core.validation.ValidationResult", R + "core.validation.ValidationError",
            R + "core.validation.Severity", R + "core.validation.SifenValidationException");
    /** Tipos sueltos del modelo de entrada (D35). */
    static final List<String> MODELO = List.of(R + "core.beans.DocumentoElectronico", R + "core.beans.EventosDE");
    /** Paquetes enteros del modelo de entrada (D35). */
    static final List<String> PAQUETES_MODELO = List.of(R + "core.fields.request.de", R + "core.fields.request.event");
    /** Paquete entero con la regla completa: los enums del MT. */
    static final List<String> PAQUETES_COMPLETOS = List.of(R + "core.types");

    // ------------------------------------------------------------------ tests

    @Test
    public void losTiposDelContratoExistenYSonPublicos() throws Exception {
        List<String> faltan = new ArrayList<>();
        for (String n : concatenar(COMPLETOS, MODELO)) {
            try {
                Class<?> c = Class.forName(n);
                if (!Modifier.isPublic(c.getModifiers())) {
                    faltan.add(n + " (no es público)");
                }
            } catch (ClassNotFoundException e) {
                faltan.add(n + " (no existe)");
            }
        }
        for (String p : concatenar(PAQUETES_MODELO, PAQUETES_COMPLETOS)) {
            if (clasesDe(p).isEmpty()) {
                faltan.add(p + " (paquete vacío)");
            }
        }
        Assertions.assertTrue(faltan.isEmpty(), "tipos del contrato: " + faltan);
    }

    /** I10-01: ninguna firma del contrato nombra un tipo de {@code internal.*} (en parámetros, retorno, excepciones o cabecera). */
    @Test
    public void ningunaFirmaDelContratoNombraInternal() throws Exception {
        List<String> malas = new ArrayList<>();
        Set<String> todas = new TreeSet<>(foto());
        todas.addAll(fotoProvisional());
        for (String l : todas) {
            if (l.contains(INTERNAL)) {
                malas.add(l);
            }
        }
        Assertions.assertTrue(malas.isEmpty(), malas.size() + " firmas nombran internal.*:\n" + String.join("\n", malas));
    }

    /** CA10-03: ninguna línea de la foto fijada se pierde ni cambia (fijada en §10.5, 2026-11-24). */
    @Test
    public void laFotoNoPierdeNiCambiaLineas() throws Exception {
        Set<String> fijada = leerFotoFijada(FOTO);
        Assertions.assertNotNull(fijada, "falta la foto del contrato v1 " + FOTO + " (CA10-03)");
        Assertions.assertFalse(fijada.isEmpty(), "la foto del contrato v1 está vacía");
        Set<String> actual = foto();
        List<String> perdidas = new ArrayList<>();
        for (String l : fijada) {
            if (!actual.contains(l)) {
                perdidas.add(l);
            }
        }
        Assertions.assertTrue(perdidas.isEmpty(), "el contrato v1 perdió o cambió " + perdidas.size()
                + " firmas (dentro de 2.x solo se agrega):\n" + String.join("\n", perdidas));
    }

    /**
     * Ninguna firma pública nueva queda fuera de las fotos (pedido de la re-verificación de H1, 2026-11-24): agregar
     * al contrato está permitido dentro de {@code 2.x}, pero en el mismo cambio se regenera la foto (modo aditivo) y
     * se anota en el contrato; así lo detecta el test y no una revisión.
     */
    @Test
    public void ningunaFirmaNuevaQuedaFueraDeLasFotos() throws Exception {
        Set<String> estable = leerFotoFijada(FOTO);
        Assertions.assertNotNull(estable, "falta la foto del contrato v1 " + FOTO);
        Set<String> nuevas = new TreeSet<>(foto());
        nuevas.removeAll(estable);
        Assertions.assertTrue(nuevas.isEmpty(), nuevas.size() + " firmas públicas nuevas sin foto (regenerar en modo "
                + "aditivo con -Djbs.sifen.contrato y anotarlas en el contrato):\n" + String.join("\n", nuevas));
    }

    /** D43: la foto de lo provisional coincide exactamente con la fijada (una diferencia obliga a regenerarla). */
    @Test
    public void laFotoProvisionalEsLaFijada() throws Exception {
        Set<String> fijada = leerFotoFijada(FOTO_PROVISIONAL);
        Assertions.assertNotNull(fijada, "falta la foto de lo provisional " + FOTO_PROVISIONAL + " (D43)");
        Set<String> actual = fotoProvisional();
        Set<String> sobran = new TreeSet<>(actual);
        sobran.removeAll(fijada);
        Set<String> faltan = new TreeSet<>(fijada);
        faltan.removeAll(actual);
        Assertions.assertTrue(sobran.isEmpty() && faltan.isEmpty(), "lo provisional cambió (regenerar la foto y anotarlo):"
                + "\nnuevas: " + sobran + "\nperdidas: " + faltan);
    }

    /** M10-06: un método abstracto que no está en las fotos fijadas, en una interfaz del contrato, tiene que ser {@code default}. */
    @Test
    public void losMetodosNuevosDeLasInterfacesSonDefault() throws Exception {
        Set<String> fijadas = new TreeSet<>();
        Set<String> estable = leerFotoFijada(FOTO);
        Set<String> provisional = leerFotoFijada(FOTO_PROVISIONAL);
        Assertions.assertNotNull(estable);
        fijadas.addAll(estable);
        if (provisional != null) {
            fijadas.addAll(provisional);
        }
        List<String> malas = new ArrayList<>();
        for (String n : COMPLETOS) {
            Class<?> c = Class.forName(n);
            if (!c.isInterface() || c.isAnnotation()) {
                continue;
            }
            for (Method m : c.getDeclaredMethods()) {
                if (Modifier.isAbstract(m.getModifiers())) {
                    String linea = c.getTypeName() + " " + m.toGenericString().replace(c.getTypeName() + "." + m.getName() + "(", m.getName() + "(");
                    if (!fijadas.contains(linea)) {
                        malas.add(linea);
                    }
                }
            }
        }
        Assertions.assertTrue(malas.isEmpty(), "métodos abstractos nuevos (tienen que ser default):\n" + String.join("\n", malas));
    }

    /**
     * Escribe la foto actual en el archivo de {@code -Djbs.sifen.contrato} (para fijarla en §10.5 o como evidencia)
     * y la de lo provisional al lado, con el sufijo {@code _provisional}.
     */
    @Test
    @EnabledIfSystemProperty(named = "jbs.sifen.contrato", matches = ".+")
    public void generarFoto() throws Exception {
        Path destino = Paths.get(System.getProperty("jbs.sifen.contrato"));
        if (destino.getParent() != null) {
            Files.createDirectories(destino.getParent());
        }
        List<String> lineas = new ArrayList<>();
        lineas.add("# Contrato de API v1 de jbs-sifen: firmas públicas (plan JBSSIF, §10.5, CA10-03). Generado por ApiSignaturesTest.");
        lineas.add("# Agregar líneas es libre; quitar o cambiar una rompe el contrato dentro de 2.x (ApiSignaturesTest.laFotoNoPierdeNiCambiaLineas).");
        lineas.addAll(foto());
        Files.write(destino, lineas, StandardCharsets.UTF_8);
        String nombre = destino.getFileName().toString();
        Path provisional = destino.resolveSibling(nombre.endsWith(".txt")
                ? nombre.substring(0, nombre.length() - 4) + "_provisional.txt" : nombre + "_provisional");
        List<String> lp = new ArrayList<>();
        lp.add("# Contrato de API v1 de jbs-sifen: firmas PROVISIONALES (@Provisional; plan JBSSIF, D43). Generado por ApiSignaturesTest.");
        lp.add("# Sigue provisional en la 2.2.0 y puede cambiar dentro de 2.x (Provisional): una diferencia obliga a regenerarla y anotarlo (ApiSignaturesTest.laFotoProvisionalEsLaFijada).");
        lp.addAll(fotoProvisional());
        Files.write(provisional, lp, StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------ foto

    /**
     * Firmas actuales del contrato, ordenadas.
     *
     * @return una línea por miembro.
     * @throws Exception si no se puede cargar un tipo o listar un paquete.
     */
    static Set<String> foto() throws Exception {
        return fotos(false);
    }

    /**
     * Firmas actuales de lo provisional del contrato ({@link Provisional}), ordenadas.
     *
     * @return una línea por miembro.
     * @throws Exception si no se puede cargar un tipo o listar un paquete.
     */
    static Set<String> fotoProvisional() throws Exception {
        return fotos(true);
    }

    private static Set<String> fotos(boolean provisional) throws Exception {
        Set<String> estable = new TreeSet<>();
        Set<String> prov = new TreeSet<>();
        List<String> completos = new ArrayList<>(COMPLETOS);
        for (String p : PAQUETES_COMPLETOS) {
            completos.addAll(clasesDe(p));
        }
        List<String> modelo = new ArrayList<>(MODELO);
        for (String p : PAQUETES_MODELO) {
            modelo.addAll(clasesDe(p));
        }
        for (String n : completos) {
            agregar(estable, prov, Class.forName(n), false, false);
        }
        for (String n : modelo) {
            agregar(estable, prov, Class.forName(n), true, false);
        }
        return provisional ? prov : estable;
    }

    private static void agregar(Set<String> estable, Set<String> prov, Class<?> c, boolean esModelo, boolean provisionalPorTipo) {
        if (!Modifier.isPublic(c.getModifiers()) || c.isSynthetic() || c.isAnonymousClass()) {
            return;
        }
        boolean tipoProvisional = provisionalPorTipo || c.isAnnotationPresent(Provisional.class);
        Set<String> out = tipoProvisional ? prov : estable;
        String t = c.getTypeName();
        if (!esModelo) {
            StringBuilder h = new StringBuilder("type ").append(Modifier.toString(c.getModifiers() & ~Modifier.STRICT & ~Modifier.INTERFACE));
            h.append(c.isInterface() ? " interface" : c.isEnum() ? " enum" : c.isRecord() ? " record" : " class");
            if (c.getGenericSuperclass() != null && c.getSuperclass() != Object.class && !c.isEnum() && !c.isRecord()) {
                h.append(" extends ").append(c.getGenericSuperclass().getTypeName());
            }
            for (java.lang.reflect.Type i : c.getGenericInterfaces()) {
                h.append(" implements ").append(i.getTypeName());
            }
            out.add(t + " " + h);
        }
        for (Constructor<?> k : c.getDeclaredConstructors()) {
            if (visible(k, esModelo) && (!esModelo || Modifier.isPublic(k.getModifiers()))) {
                (k.isAnnotationPresent(Provisional.class) ? prov : out).add(t + " " + k.toGenericString().replace(t + "(", "<init>("));
            }
        }
        for (Method m : c.getDeclaredMethods()) {
            if (!visible(m, esModelo) || m.isBridge() || FUERA.contains(m.getName())) {
                continue;
            }
            if (esModelo && !accesor(m)) {
                continue;
            }
            (m.isAnnotationPresent(Provisional.class) ? prov : out).add(t + " " + m.toGenericString().replace(t + "." + m.getName() + "(", m.getName() + "("));
        }
        for (Field f : c.getDeclaredFields()) {
            if (!visible(f, esModelo)) {
                continue;
            }
            if (esModelo && !(Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers()))) {
                continue;
            }
            (f.isAnnotationPresent(Provisional.class) ? prov : out).add(t + " " + f.toGenericString().replace(t + "." + f.getName(), f.getName()));
        }
        for (Class<?> anidada : c.getDeclaredClasses()) {
            agregar(estable, prov, anidada, esModelo, tipoProvisional);
        }
    }

    /** Getter ({@code get*}/{@code is*} sin parámetros) o setter ({@code set*} de un parámetro, {@code void}). */
    private static boolean accesor(Method m) {
        String n = m.getName();
        if (n.startsWith("get") || n.startsWith("is")) {
            return m.getParameterCount() == 0 && m.getReturnType() != void.class;
        }
        return n.startsWith("set") && m.getParameterCount() == 1 && m.getReturnType() == void.class;
    }

    private static boolean visible(Member m, boolean esModelo) {
        int mod = m.getModifiers();
        if (m.isSynthetic()) {
            return false;
        }
        return Modifier.isPublic(mod) || (!esModelo && Modifier.isProtected(mod) && !Modifier.isFinal(m.getDeclaringClass().getModifiers()));
    }

    private static Set<String> leerFotoFijada(String recurso) throws IOException {
        try (InputStream in = ApiSignaturesTest.class.getClassLoader().getResourceAsStream(recurso)) {
            if (in == null) {
                return null;
            }
            Set<String> s = new TreeSet<>();
            for (String l : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                l = l.strip();
                if (!l.isEmpty() && !l.startsWith("#")) {
                    s.add(l);
                }
            }
            return s;
        }
    }

    /**
     * Clases de primer nivel de un paquete del classpath de {@code main} (directorio o jar).
     *
     * @param paquete nombre del paquete.
     * @return los nombres, ordenados.
     * @throws IOException si no se puede listar.
     */
    static List<String> clasesDe(String paquete) throws IOException {
        // solo el artefacto de main (donde está ISifenClient): test-classes tiene paquetes con el mismo nombre
        String ruta = paquete.replace('.', '/');
        Set<String> out = new TreeSet<>();
        Path raiz;
        try {
            raiz = Paths.get(ISifenClient.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (java.net.URISyntaxException e) {
            throw new IOException(e);
        }
        if (Files.isDirectory(raiz)) {
            File[] fs = raiz.resolve(ruta).toFile().listFiles();
            for (File f : fs == null ? new File[0] : fs) {
                String n = f.getName();
                if (n.endsWith(".class") && !n.contains("$") && !n.equals("package-info.class")) {
                    out.add(paquete + "." + n.substring(0, n.length() - 6));
                }
            }
        } else {
            try (JarFile jar = new JarFile(raiz.toFile())) {
                for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements();) {
                    String n = e.nextElement().getName();
                    if (n.startsWith(ruta + "/") && n.endsWith(".class") && n.indexOf('/', ruta.length() + 1) < 0
                            && !n.contains("$") && !n.endsWith("package-info.class")) {
                        out.add(n.substring(0, n.length() - 6).replace('/', '.'));
                    }
                }
            }
        }
        return new ArrayList<>(out);
    }

    private static List<String> concatenar(List<String> a, List<String> b) {
        List<String> l = new ArrayList<>(a);
        l.addAll(b);
        return l;
    }
}
