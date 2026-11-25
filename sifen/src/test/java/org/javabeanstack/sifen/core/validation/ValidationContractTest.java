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
package org.javabeanstack.sifen.core.validation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.exceptions.SifenExceptionCodes;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contrato de la validación previa (plan JBSSIF, §3.2): acumulación, inmutabilidad, red anti-NPE del
 * motor, datos sensibles, rutas y mensajes. Usa reglas ficticias (constructor de paquete de
 * {@link ValidationRule}), sin tocar el catálogo; las reglas reales y sus tests negativos son §3.3 y
 * §3.4.
 */
public class ValidationContractTest {
    private static final String RUC = "80012345";
    private static final String NOMBRE = "Juana Benítez de Prueba";

    private static final ValidationRule TST_ERROR = new ValidationRule("TST001", RuleGroup.D,
            "gDatGralOpe.gDatRec.dRucRec", Severity.ERROR, "1234", ValuePolicy.OMIT);
    private static final ValidationRule TST_ERROR_2 = new ValidationRule("TST002", RuleGroup.D,
            "gDatGralOpe.gDatRec.dNomRec", Severity.ERROR, null, ValuePolicy.LENGTH_ONLY);
    private static final ValidationRule TST_AVISO = new ValidationRule("TST003", RuleGroup.D,
            "gDatGralOpe.gDatRec.iNatRec", Severity.WARNING, null, ValuePolicy.SHOW);
    private static final ValidationRule TST_ITEM = new ValidationRule("TST004", RuleGroup.E,
            "gDtipDE.gCamItem[].gValorItem.dPUniProSer", Severity.ERROR, null, ValuePolicy.SHOW);

    /** Grupo ficticio cuyo comportamiento se da con una lambda. */
    private static GroupValidator<DocumentoElectronico> grupo(String nombre,
            BiConsumer<DocumentoElectronico, ValidationContext> cuerpo) {
        return new GroupValidator<DocumentoElectronico>() {
            @Override
            public String getName() {
                return nombre;
            }

            @Override
            public Set<ValidationRule> getRules() {
                return Set.of(TST_ERROR, TST_ERROR_2, TST_AVISO, TST_ITEM);
            }

            @Override
            public void validate(DocumentoElectronico raiz, ValidationContext ctx) {
                cuerpo.accept(raiz, ctx);
            }
        };
    }

    private static GenerationCtx contexto() {
        GenerationCtx ctx = new GenerationCtx();
        ctx.setSifenConfig(new SifenConfig());
        return ctx;
    }

    private static List<String> codigos(ValidationResult r) {
        List<String> lista = new ArrayList<>();
        for (ValidationError e : r.getAll()) {
            lista.add(e.getCode());
        }
        return lista;
    }

    @Test
    public void acumulaTodosLosErroresDeTodosLosGruposEnOrden() {
        DEValidator v = DEValidator.builder()
                .register(grupo("uno", (de, ctx) -> {
                    ctx.report(TST_ERROR);
                    ctx.report(TST_AVISO, null, "9");
                }))
                .register(grupo("dos", (de, ctx) -> ctx.report(TST_ERROR_2, null, NOMBRE)))
                .build();
        ValidationResult r = v.validate(new DocumentoElectronico(), contexto());
        Assertions.assertEquals(List.of("TST001", "TST003", "TST002"), codigos(r));
        Assertions.assertEquals(2, r.getErrors().size());
        Assertions.assertEquals(1, r.getWarnings().size());
        Assertions.assertFalse(r.isValid());
        Assertions.assertTrue(r.contains("TST002"));
        Assertions.assertEquals("1234", r.find("TST001").get(0).getSifenResponseCode());
        Assertions.assertEquals(List.of("uno", "dos"), List.of(v.getGroups().get(0).getName(),
                v.getGroups().get(1).getName()));
    }

    @Test
    public void elResultadoEsInmutableYNoCambiaConReportesPosteriores() {
        ValidationContext ctx = new ValidationContext(contexto());
        ctx.report(TST_ERROR);
        ValidationResult r = ctx.resultado();
        ctx.report(TST_ERROR_2);
        Assertions.assertEquals(1, r.getAll().size(), "un reporte posterior no altera el resultado");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> r.getAll().add(r.getAll().get(0)));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> r.getErrors().clear());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> r.getWarnings().clear());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> r.find("TST001").clear());
    }

    @Test
    public void soloAvisosEsValidoYNoLanza() throws Exception {
        DEValidator v = DEValidator.builder()
                .register(grupo("avisos", (de, ctx) -> ctx.report(TST_AVISO)))
                .build();
        ValidationResult r = v.validate(new DocumentoElectronico(), contexto());
        Assertions.assertTrue(r.isValid());
        Assertions.assertFalse(r.isEmpty());
        r.throwIfInvalid();
    }

    @Test
    public void conErroresLanzaSifenValidationExceptionConLaListaCompleta() {
        DEValidator v = DEValidator.builder()
                .register(grupo("g", (de, ctx) -> {
                    ctx.report(TST_ERROR);
                    ctx.report(TST_AVISO);
                }))
                .build();
        ValidationResult r = v.validate(new DocumentoElectronico(), contexto());
        SifenValidationException e = Assertions.assertThrows(SifenValidationException.class, r::throwIfInvalid);
        Assertions.assertInstanceOf(SifenException.class, e);
        Assertions.assertEquals(SifenExceptionCodes.INVALID_DE_CODE, e.getLegacyCode());
        // D42, §11.4: getCode() devuelve el código estable (la primera regla con error), no el i405
        Assertions.assertEquals("TST001", e.getCode());
        Assertions.assertEquals(2, e.getErrors().size());
        Assertions.assertSame(r, e.getResult());
        Assertions.assertTrue(e.getMessage().startsWith(SifenExceptionCodes.INVALID_DE_MESSAGE + ": 1 error y 1 aviso. [TST001]"),
                e.getMessage());
    }

    @Test
    public void elResumenDeLaExcepcionSeCortaEnElMaximo() {
        ValidationContext ctx = new ValidationContext(contexto());
        for (int i = 0; i < SifenValidationException.MAX_ERRORS_IN_MESSAGE + 3; i++) {
            ctx.report(TST_ITEM, i);
        }
        SifenValidationException e = new SifenValidationException(ctx.resultado());
        Assertions.assertTrue(e.getMessage().endsWith("; y 3 más"), e.getMessage());
        Assertions.assertEquals(SifenValidationException.MAX_ERRORS_IN_MESSAGE + 3, e.getErrors().size());
    }

    @Test
    public void unGrupoQueLanzaNpeSeConvierteEnJbsVal00YLosSiguientesCorren() {
        DEValidator v = DEValidator.builder()
                .register(grupo("gDatRec", (de, ctx) -> {
                    ctx.report(TST_ERROR);
                    de.getgDatGralOpe().getgDatRec();          // NPE: gDatGralOpe es null
                }))
                .register(grupo("despues", (de, ctx) -> ctx.report(TST_ERROR_2)))
                .build();
        ValidationResult r = Assertions.assertDoesNotThrow(() -> v.validate(new DocumentoElectronico(), contexto()));
        Assertions.assertEquals(List.of("TST001", "JBS-VAL-00", "TST002"), codigos(r));
        ValidationError interno = r.find("JBS-VAL-00").get(0);
        Assertions.assertEquals("DE", interno.getPath());
        Assertions.assertTrue(interno.getMessage().contains("gDatRec (NullPointerException)"), interno.getMessage());
        Assertions.assertTrue(interno.isError());
    }

    @Test
    public void documentoNuloDaJbsVal01SinCorrerLosGrupos() {
        List<String> corridos = new ArrayList<>();
        DEValidator v = DEValidator.builder()
                .register(grupo("g", (de, ctx) -> corridos.add("g")))
                .build();
        ValidationResult r = v.validate(null, null);
        Assertions.assertEquals(List.of("JBS-VAL-01"), codigos(r));
        Assertions.assertFalse(r.isValid());
        Assertions.assertTrue(corridos.isEmpty());
    }

    @Test
    public void contextoNuloDaElAvisoJbsVal02YValoresPorDefecto() {
        // §6.3 (D25, app-developer): sin el flag de la NT 13, el valor por defecto que queda es la configuración nula
        List<Boolean> corrio = new ArrayList<>();
        DEValidator v = DEValidator.builder()
                .register(grupo("g", (de, ctx) -> {
                    corrio.add(Boolean.TRUE);
                    Assertions.assertNull(ctx.getSifenConfig());
                }))
                .build();
        ValidationResult r = v.validate(new DocumentoElectronico(), null);
        Assertions.assertEquals(List.of("JBS-VAL-02"), codigos(r));
        Assertions.assertTrue(r.isValid(), "el aviso no invalida");
        Assertions.assertEquals(List.of(Boolean.TRUE), corrio);
    }

    @Test
    public void elValorSensibleNoApareceEnNingunTexto() {
        ValidationContext ctx = new ValidationContext(contexto());
        ctx.report(TST_ERROR, null, RUC);                 // OMIT
        ctx.report(TST_ERROR_2, null, NOMBRE);            // LENGTH_ONLY
        ValidationResult r = ctx.resultado();
        ValidationError omitido = r.getAll().get(0);
        ValidationError largo = r.getAll().get(1);
        Assertions.assertTrue(omitido.getValue().isEmpty());
        Assertions.assertEquals("largo " + NOMBRE.length(), largo.getValue().orElseThrow());
        SifenValidationException e = new SifenValidationException(r);
        for (String texto : List.of(omitido.getMessage(), omitido.toString(), largo.getMessage(), largo.toString(),
                r.toString(), e.getMessage())) {
            Assertions.assertFalse(texto.contains(RUC), texto);
            Assertions.assertFalse(texto.contains("Benítez"), texto);
        }
    }

    @Test
    public void conMostrarElValorSeTruncaYTampocoVaALosTextos() {
        String largo = "1234567890".repeat(6);
        ValidationContext ctx = new ValidationContext(contexto());
        ctx.report(TST_AVISO, null, new BigDecimal("1E+3"));
        ctx.report(TST_ITEM, TST_ITEM.path(0), largo);
        ValidationResult r = ctx.resultado();
        Assertions.assertEquals("1000", r.getAll().get(0).getValue().orElseThrow());
        String truncado = r.getAll().get(1).getValue().orElseThrow();
        Assertions.assertEquals(ValidationError.MAX_VALUE_LENGTH + 1, truncado.length());
        Assertions.assertTrue(truncado.endsWith("…"));
        Assertions.assertFalse(r.getAll().get(1).toString().contains("1234567890"));
    }

    @Test
    public void rutasConIndiceBaseCeroYNormalizacion() {
        Assertions.assertEquals("gDtipDE.gCamItem[3].gValorItem.dPUniProSer", TST_ITEM.path(3));
        Assertions.assertEquals("gDtipDE.gCamItem[].gValorItem.dPUniProSer", TST_ITEM.path());
        Assertions.assertEquals(TST_ITEM.getPath(), ValidationRule.normalizePath(TST_ITEM.path(12)));
        ValidationRule doble = new ValidationRule("TST005", RuleGroup.E, "gDtipDE.gCamItem[].gRasMerc[].dNumLote",
                Severity.ERROR, null, null);
        Assertions.assertEquals("gDtipDE.gCamItem[1].gRasMerc[0].dNumLote", doble.path(1, 0));
        Assertions.assertEquals("gDtipDE.gCamItem[1].gRasMerc[].dNumLote", doble.path(1));
        Assertions.assertEquals(ValuePolicy.OMIT, doble.getValuePolicy(), "política nula = OMIT");
        Assertions.assertNull(ValidationRule.normalizePath(null));
    }

    @Test
    public void unaRutaQueNoEsLaDeLaReglaSeRechazaSinFiltrarla() {
        ValidationContext ctx = new ValidationContext(contexto());
        IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class,
                () -> ctx.report(TST_ERROR, RUC, null));
        Assertions.assertFalse(e.getMessage().contains(RUC), e.getMessage());
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> ctx.report(TST_ITEM, "gDtipDE.gCamItem[0].gValorItem.dTotBruOpeItem", null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ctx.checkPresent(TST_ERROR, RUC, null));
        ctx.report(TST_ITEM, TST_ITEM.path(1), null);
        ctx.report(TST_ITEM, 4);
        ctx.report(TST_ERROR, null, null);
        Assertions.assertEquals(List.of("gDtipDE.gCamItem[1].gValorItem.dPUniProSer",
                "gDtipDE.gCamItem[4].gValorItem.dPUniProSer", "gDatGralOpe.gDatRec.dRucRec"),
                ctx.resultado().getAll().stream().map(ValidationError::getPath).toList());
    }

    @Test
    public void dentroDeUnGrupoUnaRutaAjenaTerminaEnJbsVal00() {
        DEValidator v = DEValidator.builder()
                .register(grupo("g", (de, ctx) -> ctx.report(TST_ERROR, RUC, null)))
                .build();
        ValidationResult r = v.validate(new DocumentoElectronico(), contexto());
        Assertions.assertEquals(List.of("JBS-VAL-00"), codigos(r));
        Assertions.assertFalse(r.toString().contains(RUC));
    }

    @Test
    public void unaReglaNoDeclaradaSeAvisaYEnModoEstrictoLanza() {
        ValidationRule ajena = new ValidationRule("TST010", RuleGroup.F, "gTotSub.dTotGralOpe", Severity.ERROR,
                null, null);
        GroupValidator<DocumentoElectronico> g = grupo("g", (de, ctx) -> ctx.report(ajena));
        ValidationResult r = DEValidator.builder().register(g).build().validate(new DocumentoElectronico(), contexto());
        Assertions.assertEquals(List.of("TST010"), codigos(r), "sin modo estricto se conserva y solo se avisa");
        DEValidator estricto = DEValidator.builder().register(g).strict().build();
        IllegalStateException e = Assertions.assertThrows(IllegalStateException.class,
                () -> estricto.validate(new DocumentoElectronico(), contexto()));
        Assertions.assertTrue(e.getMessage().contains("TST010"), e.getMessage());
        DEValidator estrictoOk = DEValidator.builder().register(grupo("ok", (de, ctx) -> ctx.report(TST_ERROR)))
                .strict().build();
        Assertions.assertEquals(List.of("TST001"), codigos(estrictoOk.validate(new DocumentoElectronico(), contexto())));
    }

    @Test
    public void laTrazaDelLogNoLlevaLosMensajesDeLaExcepcionNiDeSusCausas() {
        NumberFormatException causa = new NumberFormatException("For input string: \"" + RUC + "\"");
        IllegalStateException original = new IllegalStateException("fallo con " + NOMBRE, causa);
        Throwable copia = ValidationEngine.trazaSinMensaje(original);
        Assertions.assertEquals(IllegalStateException.class.getName(), copia.getMessage());
        Assertions.assertArrayEquals(original.getStackTrace(), copia.getStackTrace());
        Assertions.assertEquals(NumberFormatException.class.getName(), copia.getCause().getMessage());
        Assertions.assertNull(copia.getCause().getCause());
        java.io.StringWriter texto = new java.io.StringWriter();
        copia.printStackTrace(new java.io.PrintWriter(texto));
        Assertions.assertFalse(texto.toString().contains(RUC), texto.toString());
        Assertions.assertFalse(texto.toString().contains("Benítez"), texto.toString());
    }

    @Test
    public void mostrarSoloConservaEscalares() {
        Object bean = new Object() {
            @Override
            public String toString() {
                return "Receptor{ruc=" + RUC + "}";
            }
        };
        ValidationContext ctx = new ValidationContext(contexto());
        ctx.report(TST_AVISO, null, bean);
        ctx.report(TST_AVISO, null, List.of(RUC));
        ctx.report(TST_AVISO, null, java.time.LocalDate.of(2026, 11, 23));
        ctx.report(TST_AVISO, null, Severity.WARNING);
        ctx.report(TST_AVISO, null, Boolean.TRUE);
        ctx.report(TST_AVISO, null, (short) 4);
        List<ValidationError> todos = ctx.resultado().getAll();
        Assertions.assertTrue(todos.get(0).getValue().isEmpty(), "un bean no se conserva");
        Assertions.assertTrue(todos.get(1).getValue().isEmpty(), "una colección no se conserva");
        Assertions.assertEquals("2026-11-23", todos.get(2).getValue().orElseThrow());
        Assertions.assertEquals("WARNING", todos.get(3).getValue().orElseThrow());
        Assertions.assertEquals("true", todos.get(4).getValue().orElseThrow());
        Assertions.assertEquals("4", todos.get(5).getValue().orElseThrow());
    }

    @Test
    public void exigirReportaLosAusentesYDejaSeguirLosPresentes() {
        ValidationContext ctx = new ValidationContext(contexto());
        Assertions.assertFalse(ctx.checkPresent(TST_ERROR, null, null));
        Assertions.assertFalse(ctx.checkPresent(TST_ERROR, null, "  "));
        Assertions.assertFalse(ctx.checkPresent(TST_ERROR, null, Collections.emptyList()));
        Assertions.assertTrue(ctx.checkPresent(TST_ERROR, null, RUC));
        Assertions.assertTrue(ctx.checkPresent(TST_ERROR, null, (short) 0), "un cero es un dato presente");
        Assertions.assertEquals(3, ctx.getReportedCount());
    }

    @Test
    public void unaReglaSinMensajeDaElTextoDeReservaSinLanzar() {
        ValidationContext ctx = new ValidationContext(contexto());
        ctx.report(TST_ERROR);
        String mensaje = ctx.resultado().getAll().get(0).getMessage();
        Assertions.assertTrue(mensaje.contains("TST001"), mensaje);
        Assertions.assertFalse(ValidationMessages.hasMessage("TST001"));
        Assertions.assertFalse(ValidationMessages.hasMessage(null));
        Assertions.assertNotNull(ValidationMessages.message(null));
    }

    @Test
    public void losMarcadoresSeReemplazanSinFormatoDeLocale() {
        Assertions.assertEquals("Error interno del validador en el grupo gTotSub (ArithmeticException): no se pudieron "
                + "evaluar sus reglas. Revise el registro de la aplicación.",
                ValidationRule.JBS_VAL_00.message("gTotSub", "ArithmeticException"));
        Assertions.assertTrue(ValidationMessages.message("JBS-VAL-00", new BigDecimal("7000000")).contains("7000000"));
    }

    @Test
    public void prefijoYCombinacionParaLotes() {
        ValidationContext uno = new ValidationContext(contexto());
        uno.report(TST_ERROR);
        ValidationContext dos = new ValidationContext(contexto());
        dos.report(TST_ITEM, 2);
        ValidationResult lote = ValidationResult.combine(List.of(uno.resultado().withPrefix("lote[0]"),
                dos.resultado().withPrefix("lote[1]")));
        Assertions.assertEquals("lote[0].gDatGralOpe.gDatRec.dRucRec", lote.getAll().get(0).getPath());
        Assertions.assertEquals("lote[1].gDtipDE.gCamItem[2].gValorItem.dPUniProSer", lote.getAll().get(1).getPath());
        Assertions.assertSame(ValidationResult.empty(), ValidationResult.combine(null));
        Assertions.assertTrue(ValidationResult.empty().isValid());
    }

    @Test
    public void elBuilderRechazaGruposRepetidosOSinNombre() {
        DEValidator.Builder b = DEValidator.builder().register(grupo("g", (de, ctx) -> { }))
                .register(grupo("g", (de, ctx) -> { }));
        Assertions.assertThrows(IllegalArgumentException.class, b::build);
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> DEValidator.builder().register(grupo(" ", (de, ctx) -> { })).build());
        Assertions.assertThrows(NullPointerException.class, () -> DEValidator.builder().register(null));
    }

    @Test
    public void laExcepcionSeSerializaConSuLista() throws Exception {
        ValidationContext ctx = new ValidationContext(contexto());
        ctx.report(TST_ERROR_2, null, NOMBRE);
        SifenValidationException original = new SifenValidationException(ctx.resultado());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }
        SifenValidationException copia;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            copia = (SifenValidationException) in.readObject();
        }
        Assertions.assertEquals(original.getErrors(), copia.getErrors());
        Assertions.assertEquals(original.getMessage(), copia.getMessage());
    }
}
