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
package org.javabeanstack.sifen.test.casos;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.fields.request.de.TgDatRec;
import org.javabeanstack.sifen.core.types.*;
import org.javabeanstack.sifen.core.validation.DEValidator;
import org.javabeanstack.sifen.core.validation.ValidationError;
import org.javabeanstack.sifen.core.validation.ValidationResult;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;

/**
 * Batería de identificación del receptor (grupo D2xx; plan JBSSIF §2.1.5, pedido del usuario del
 * 2026-11-24): matriz naturaleza × operación, RUC, documentos de no contribuyente, innominado (NT 021/024),
 * extranjero (NT 023: D208g, D210 opcional en B2F), nombre y dirección. Por combinación: lo que espera
 * el MT v150 + NT 021/023/024/027 (aceptar o el código de rechazo), lo que hace la validación previa de
 * jbs-sifen y el {@code gDatRec} que genera la librería con la validación apagada.
 * Uso: {@code BateriaIdentificacion <salida.md>}.
 */
public final class BateriaIdentificacion {

    /** Una combinación: id, descripción, base, cambio, reglas que el MT exige (vacío = aceptar) y nota. */
    public record Combinacion(String id, String descripcion, Supplier<DocumentoElectronico> base,
            Consumer<DocumentoElectronico> cambio, Set<String> esperadas, String nota) {
        @Override
        public String toString() {
            return id;
        }

        public DocumentoElectronico construir() {
            DocumentoElectronico de = base.get();
            // Leyendas del ambiente de pruebas (Q14), para que D105/JBS-E-70 no se mezclen.
            de.getgDatGralOpe().getgEmis().setdNomEmi("DE generado en ambiente de prueba - sin valor comercial ni fiscal");
            de.getgDtipDE().getgCamItemList().get(0).setdDesProSer(
                    "DOCUMENTO ELECTRÓNICO SIN VALOR COMERCIAL NI FISCAL - GENERADO EN AMBIENTE DE PRUEBA");
            cambio.accept(de);
            return de;
        }
    }

    static TgDatRec r(DocumentoElectronico de) {
        return de.getgDatGralOpe().getgDatRec();
    }

    static Set<String> s(String... c) {
        return Set.of(c);
    }

    static final Supplier<DocumentoElectronico> B2B = CasosDePrueba::facturaIvaMixto;          // contribuyente, PYG
    static final Supplier<DocumentoElectronico> B2C = CasosDePrueba::facturaPygB2cCedula;      // cédula, PYG 47.789
    static final Supplier<DocumentoElectronico> B2G = CasosDePrueba::facturaB2g;
    static final Supplier<DocumentoElectronico> NC = CasosDePrueba::notaCredito;
    static final Supplier<DocumentoElectronico> EXT = CasosDePrueba::facturaExtranjero;        // USD, pasaporte
    static final Supplier<DocumentoElectronico> B2F = CasosDePrueba::facturaB2fPasaporteSinNumero;

    static void doc(DocumentoElectronico de, TiTipDocRec tipo, String numero) {
        r(de).setiTipIDRec(tipo);
        r(de).setdNumIDRec(numero);
    }

    static void innominado(DocumentoElectronico de) {
        r(de).setiTipIDRec(TiTipDocRec.INNOMINADO);
        r(de).setdNumIDRec("0");
        r(de).setdNomRec("Sin Nombre");
    }

    static void precio(DocumentoElectronico de, String precio) {
        de.getgDtipDE().getgCamItemList().get(0).getgValorItem().setdPUniProSer(new BigDecimal(precio));
        de.getgDtipDE().getgCamItemList().get(0).setdCantProSer(BigDecimal.ONE);
    }

    static void comoNoContribuyente(DocumentoElectronico de, TiTiOpe ope) {
        TgDatRec x = r(de);
        x.setiNatRec(TiNatRec.NO_CONTRIBUYENTE);
        x.setiTiOpe(ope);
        x.setiTiContRec(null);
        x.setdRucRec(null);
        x.setdDVRec((short) 0);
        x.setiTipIDRec(TiTipDocRec.CEDULA_PARAGUAYA);
        x.setdNumIDRec("1234567");
    }

    public static List<Combinacion> combinaciones() {
        List<Combinacion> l = new ArrayList<>();
        // ---- contribuyente (D201 = 1)
        l.add(new Combinacion("ID-C01", "B2B, RUC y DV correctos, persona jurídica", B2B, de -> { }, s(), ""));
        l.add(new Combinacion("ID-C02", "B2B, persona física (iTiContRec 1)", B2B, de -> r(de).setiTiContRec(TiTipCont.PERSONA_FISICA), s(), ""));
        l.add(new Combinacion("ID-C03", "B2B, RUC sin DV (dDVRec sin cargar = 0)", B2B, de -> r(de).setdDVRec((short) 0), s("D207"), "1309"));
        l.add(new Combinacion("ID-C04", "B2B, DV incorrecto", B2B, de -> r(de).setdDVRec((short) 4), s("D207"), "1309"));
        l.add(new Combinacion("ID-C05", "B2B, RUC con guion y DV («80654321-3»)", B2B, de -> r(de).setdRucRec("80654321-3"), s("JBS-D-43"), "formato A 3-8"));
        l.add(new Combinacion("ID-C06", "B2B, RUC inexistente con DV de módulo 11 correcto (80000005-6)", B2B, de -> {
            r(de).setdRucRec("80000005");
            r(de).setdDVRec(Short.parseShort(org.javabeanstack.sifen.internal.util.SifenUtil.generateDv("80000005")));
        }, s(), "solo servidor: 1306 (RUC inexistente), como el 0000055 de la Fase 2"));
        l.add(new Combinacion("ID-C07", "B2G contribuyente (gCompPub)", B2G, de -> { }, s(), ""));
        l.add(new Combinacion("ID-C08", "Contribuyente en B2C", B2B, de -> r(de).setiTiOpe(TiTiOpe.B2C), s(), "D202 no lo impide"));
        l.add(new Combinacion("ID-C09", "Contribuyente en B2F", B2B, de -> { r(de).setiTiOpe(TiTiOpe.B2F); r(de).setcPaisRec(PaisType.ARG); },
                s("D202"), "1300: B2F ⇒ no contribuyente; departamento y ciudad no van en B2F (JBS-D-50/54 son AVISO desde I3-42)"));
        l.add(new Combinacion("ID-C10", "Contribuyente con iTipIDRec informado", B2B, de -> r(de).setiTipIDRec(TiTipDocRec.CEDULA_PARAGUAYA), s(), "1311 (NT 023); desde I3-37 D208a es AVISO: la librería no escribe el dato"));
        l.add(new Combinacion("ID-C11", "Contribuyente con dNumIDRec informado", B2B, de -> r(de).setdNumIDRec("1234567"), s(), "1334 (NT 023); desde I3-37 D210b es AVISO: no se escribe y SIFEN aprueba (001-001-0001011)"));
        l.add(new Combinacion("ID-C12", "Contribuyente sin iTiContRec", B2B, de -> r(de).setiTiContRec(null), s("D205"), "1302"));
        l.add(new Combinacion("ID-C13", "Contribuyente sin RUC", B2B, de -> r(de).setdRucRec(null), s("D206"), "1304"));
        // ---- no contribuyente (D201 = 2), B2C
        l.add(new Combinacion("ID-N01", "B2C, cédula paraguaya 1234567", B2C, de -> { }, s(), ""));
        l.add(new Combinacion("ID-N02", "B2C, pasaporte", B2C, de -> doc(de, TiTipDocRec.PASAPORTE, "AB123456"), s(), ""));
        l.add(new Combinacion("ID-N03", "B2C, cédula extranjera", B2C, de -> doc(de, TiTipDocRec.CEDULA_EXTRANJERA, "12345678"), s(), ""));
        l.add(new Combinacion("ID-N04", "B2C, carnet de residencia", B2C, de -> doc(de, TiTipDocRec.CARNET_DE_RESIDENCIA, "R-12345"), s(), ""));
        l.add(new Combinacion("ID-N05", "B2C, tarjeta diplomática (6)", B2C, de -> doc(de, TiTipDocRec.TARJETA_DIPLOMATICA, "TD-001"), s(), ""));
        l.add(new Combinacion("ID-N06", "B2C, «Otro» (9) con texto libre «Registro de conducir»", B2C, de -> {
            doc(de, TiTipDocRec.OTRO, "RC-778899");
            r(de).setdDTipIDRec("Registro de conducir");
        }, s(), "D209a: con 9 el texto libre A 9-41 va en dDTipIDRec (Q17)"));
        l.add(new Combinacion("ID-N07", "B2C, «Otro» (9) sin texto libre", B2C, de -> doc(de, TiTipDocRec.OTRO, "RC-778899"), s("D209a"), "1313 (Q17)"));
        l.add(new Combinacion("ID-N08", "B2C, cédula sin número", B2C, de -> r(de).setdNumIDRec(null), s("D210"), "1314"));
        l.add(new Combinacion("ID-N09", "B2C, número con espacios «123 4567»", B2C, de -> r(de).setdNumIDRec("123 4567"), s("JBS-D-60"), "XSD tdNumDocId [0-9A-Za-z-]: espacio no admitido (E3-26)"));
        l.add(new Combinacion("ID-N10", "B2C, número con puntos «1.234.567»", B2C, de -> r(de).setdNumIDRec("1.234.567"), s("JBS-D-60"), "ídem; SIFEN 0160 en vivo (001-001-0001013, E3-26)"));
        l.add(new Combinacion("ID-N11", "B2C, número con guion «1234567-8»", B2C, de -> r(de).setdNumIDRec("1234567-8"), s(), "ídem"));
        l.add(new Combinacion("ID-N12", "B2C, número con ceros a la izquierda «0001234567»", B2C, de -> r(de).setdNumIDRec("0001234567"), s(), "ídem"));
        l.add(new Combinacion("ID-N13", "B2C, número de 21 caracteres", B2C, de -> r(de).setdNumIDRec("123456789012345678901"), s("JBS-D-44"), "A 1-20"));
        l.add(new Combinacion("ID-N14", "B2C, descripción del documento que no corresponde al código (cédula / «Pasaporte»)", B2C,
                de -> r(de).setdDTipIDRec("Pasaporte"), s(), "D209a 1313 si se enviara; la librería escribe la descripción del código"));
        l.add(new Combinacion("ID-N15", "No contribuyente en B2B", B2C, de -> r(de).setiTiOpe(TiTiOpe.B2B), s("D202"), "1300"));
        l.add(new Combinacion("ID-N16", "No contribuyente en B2G", B2C, de -> r(de).setiTiOpe(TiTiOpe.B2G), s("D202"), "1300"));
        l.add(new Combinacion("ID-N17", "No contribuyente con dRucRec", B2C, de -> r(de).setdRucRec("80654321"), s(), "1305; desde I3-37 D206a es AVISO: la librería no escribe el RUC"));
        l.add(new Combinacion("ID-N18", "No contribuyente con iTiContRec", B2C, de -> r(de).setiTiContRec(TiTipCont.PERSONA_FISICA), s(), "1303; desde I3-37 D205a es AVISO: la librería no escribe el dato"));
        l.add(new Combinacion("ID-N19", "No contribuyente sin iTipIDRec (B2C)", B2C, de -> r(de).setiTipIDRec(null), s("D208"),
                "1310; D208g (1335) solo en B2F desde M3-38, para no duplicar el mismo hecho"));
        // ---- innominado (D208 = 5)
        l.add(new Combinacion("ID-I01", "Innominado PYG, total 47.789 (< 7.000.000)", B2C, BateriaIdentificacion::innominado, s(), ""));
        l.add(new Combinacion("ID-I02", "Innominado PYG, total 6.999.999", B2C, de -> { innominado(de); precio(de, "6999999"); }, s(), "NT 024: el umbral es ≥ 7.000.000"));
        l.add(new Combinacion("ID-I03", "Innominado PYG, total 7.000.000", B2C, de -> { innominado(de); precio(de, "7000000"); }, s("D208c"), "1321 (NT 021/024)"));
        l.add(new Combinacion("ID-I04", "Innominado USD, 958,82 × 7300,25 = 6.999.625,6 Gs (< umbral por F023)", EXT, de -> {
            comoNoContribuyente(de, TiTiOpe.B2C); r(de).setcPaisRec(PaisType.PRY); innominado(de); precio(de, "958.82");
        }, s(), "el umbral se mide en guaraníes (F023)"));
        l.add(new Combinacion("ID-I05", "Innominado USD, 958,91 × 7300,25 = 7.000.282,7 Gs (≥ umbral)", EXT, de -> {
            comoNoContribuyente(de, TiTiOpe.B2C); r(de).setcPaisRec(PaisType.PRY); innominado(de); precio(de, "958.91");
        }, s("D208c"), "1321 sobre dTotalGs"));
        l.add(new Combinacion("ID-I06", "Innominado en nota de crédito", NC, de -> { comoNoContribuyente(de, TiTiOpe.B2C); innominado(de); }, s("D208e"), "1331 (NT 023)"));
        l.add(new Combinacion("ID-I07", "Innominado fuera de B2C (B2B)", B2C, de -> { innominado(de); r(de).setiTiOpe(TiTiOpe.B2B); }, s("D208f", "D202"), "1333 (NT 023) y 1300"));
        l.add(new Combinacion("ID-I08", "Innominado con número distinto de «0»", B2C, de -> { innominado(de); r(de).setdNumIDRec("1234567"); }, s("JBS-D-44"), "MT: «completar con 0»"));
        l.add(new Combinacion("ID-I09", "Innominado con nombre distinto de «Sin Nombre»", B2C, de -> { innominado(de); r(de).setdNomRec("Juan Perez"); }, s("JBS-D-46"), ""));
        l.add(new Combinacion("ID-I10", "Innominado sin número (la librería completa con «0»)", B2C, de -> { innominado(de); r(de).setdNumIDRec(null); }, s(), ""));
        // ---- extranjero (B2F)
        l.add(new Combinacion("ID-F01", "B2F, pasaporte, sin número (DE-24)", B2F, de -> { }, s(), "D210 opcional en B2F (NT 023); la librería escribe dNumIDRec «0»"));
        l.add(new Combinacion("ID-F02", "B2F, pasaporte con número", B2F, de -> r(de).setdNumIDRec("AB123456"), s(),
                "MT v150 D210a (1323) decía no informar en B2F; la NT 023 no la repite: en vivo"));
        l.add(new Combinacion("ID-F03", "B2F sin iTipIDRec", B2F, de -> r(de).setiTipIDRec(null), s("D208g"), "1335 (NT 023): ERROR desde el 2026-11-24"));
        l.add(new Combinacion("ID-F04", "B2F con país PRY", B2F, de -> r(de).setcPaisRec(PaisType.PRY), s("D203"), "1320"));
        l.add(new Combinacion("ID-F05", "B2C con país ARG (extranjero sin B2F, como DE-17)", EXT, de -> { }, s("D203"), "1320"));
        l.add(new Combinacion("ID-F06", "B2F sin dirección", B2F, de -> r(de).setdDirRec(null), s("D213"), "1318"));
        l.add(new Combinacion("ID-F07", "B2F con departamento", B2F, de -> r(de).setcDepRec(TDepartamento.CAPITAL), s(), "no informar si D202 = 4; desde I3-42 JBS-D-50 es AVISO: TgDatRec no escribe cDepRec en B2F"));
        // Fase 12 (plan JBSSIF, §12.1, grupo A; 2026-11-25): B2F con documentos de extranjero distintos del pasaporte
        l.add(new Combinacion("ID-F08", "B2F con cédula extranjera", B2F, de -> doc(de, TiTipDocRec.CEDULA_EXTRANJERA, "12345678"), s(),
                "D208 3 con D213 dDirRec (1318); sin departamento ni ciudad. SIFEN: 0260"));
        l.add(new Combinacion("ID-F09", "B2F con carnet de residencia", B2F, de -> doc(de, TiTipDocRec.CARNET_DE_RESIDENCIA, "R-12345"), s(),
                "D208 4, número con guion (tdNumDocId). SIFEN: 0260 a confirmar (residente con país distinto de PRY)"));
        // ---- nombre, dirección y otros
        l.add(new Combinacion("ID-R01", "Nombre con ñ, tildes, comillas y & («Peña & Hijos \"Ñandú\" S.Á.»)", B2B,
                de -> r(de).setdNomRec("Peña & Hijos \"Ñandú\" S.Á."), s(), "se escapa en el XML"));
        l.add(new Combinacion("ID-R02", "Nombre de 3 caracteres", B2B, de -> r(de).setdNomRec("ABC"), s("JBS-D-46"), "A 4-255"));
        l.add(new Combinacion("ID-R03", "Sin nombre (contribuyente)", B2B, de -> r(de).setdNomRec(null), s("JBS-D-45"), "la librería escribiría «Sin Nombre»"));
        l.add(new Combinacion("ID-R04", "Dirección sin departamento", B2B, de -> r(de).setcDepRec(null), s("JBS-D-49"), ""));
        l.add(new Combinacion("ID-R05", "Dirección sin número de casa (dNumCasRec 0)", B2B, de -> r(de).setdNumCasRec(0), s(), "«0 si no tiene»"));
        l.add(new Combinacion("ID-R06", "Correo inválido", B2B, de -> r(de).setdEmailRec("no-es-correo"), s("JBS-D-58"), ""));
        return l;
    }

    public static void main(String[] args) throws Exception {
        SifenConfig cfg = FixturesSifen.configuracion();
        GenerationCtx ctx = FixturesSifen.contexto(cfg);
        ZoneId zona = ZoneId.of("America/Asuncion");
        DEValidator v = DEValidator.builder().registerStandard()
                .clock(Clock.fixed(CasosDePrueba.FECHA_FIRMA.plusSeconds(5).atZone(zona).toInstant(), zona)).build();
        List<String> out = new ArrayList<>();
        out.add("| ID | Combinación | MT/NT espera | jbs-sifen (validación previa) | ¿coincide? | gDatRec que genera la librería | Nota |");
        out.add("|---|---|---|---|---|---|---|");
        int coinciden = 0;
        for (Combinacion c : combinaciones()) {
            ValidationResult r = v.validate(c.construir(), ctx);
            Set<String> err = new TreeSet<>();
            for (ValidationError e : r.getErrors()) {
                err.add(e.getCode());
            }
            boolean ok = err.equals(new TreeSet<>(c.esperadas()));
            if (ok) {
                coinciden++;
            }
            String xml;
            try {
                xml = gDatRec(c.construir().generarXml(ctx, cfg));
            } catch (Throwable t) {
                xml = "lanza " + t.getClass().getSimpleName();
            }
            out.add("| " + c.id() + " | " + c.descripcion().replace("|", "\\|") + " | "
                    + (c.esperadas().isEmpty() ? "acepta" : "rechaza " + new TreeSet<>(c.esperadas())) + " | "
                    + (err.isEmpty() ? "acepta" : "rechaza " + err) + " | " + (ok ? "sí" : "**NO**") + " | `"
                    + xml.replace("|", "\\|") + "` | " + c.nota().replace("|", "\\|") + " |");
        }
        out.add("");
        out.add("Combinaciones: " + combinaciones().size() + " · coinciden con el MT: " + coinciden);
        Files.write(Paths.get(args[0]), out, StandardCharsets.UTF_8);
        System.out.println("combinaciones " + combinaciones().size() + ", coinciden " + coinciden);
    }

    /** El gDatRec del XML, sin espacios de nombres, campos de identificación. */
    static String gDatRec(String xml) {
        Matcher m = Pattern.compile("<(?:\\w+:)?gDatRec>(.*?)</(?:\\w+:)?gDatRec>", Pattern.DOTALL).matcher(xml);
        if (!m.find()) {
            return "(sin gDatRec)";
        }
        String g = m.group(1);
        StringBuilder sb = new StringBuilder();
        Matcher f = Pattern.compile("<(?:\\w+:)?(iNatRec|iTiOpe|cPaisRec|iTiContRec|dRucRec|dDVRec|iTipIDRec|dDTipIDRec|dNumIDRec|dNomRec)>([^<]*)<").matcher(g);
        while (f.find()) {
            sb.append(f.group(1)).append('=').append(f.group(2)).append(' ');
        }
        return sb.toString().trim();
    }
}
