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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.fields.request.event.TgGroupTiEvt;
import org.javabeanstack.sifen.core.fields.request.event.TrGeVeDescon;
import org.javabeanstack.sifen.core.fields.request.event.TrGeVeNotRec;
import org.javabeanstack.sifen.core.fields.request.event.TrGeVeTr;
import org.javabeanstack.sifen.core.fields.request.event.TrGesEve;
import org.javabeanstack.sifen.core.types.TDepartamento;
import org.javabeanstack.sifen.core.types.TdMotEv;
import org.javabeanstack.sifen.core.types.TdTipIdenVeh;
import org.javabeanstack.sifen.core.types.TiModTrans;
import org.javabeanstack.sifen.core.types.TiNatRec;
import org.javabeanstack.sifen.core.types.TiTTrans;
import org.javabeanstack.sifen.core.types.TiTipDocRec;
import org.javabeanstack.sifen.internal.util.SifenUtil;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.FixturesSifen;

/**
 * Soporte común de {@code DeValidatorTest} y {@code NullSafetyMutationTest} (plan JBSSIF, §3.4):
 * validador de las pruebas, configuración, casos del catálogo en sus dos orígenes y detección de la
 * integración en la fachada (§3.5).
 *
 * <p>Las pruebas del catálogo usan {@code DEValidator.builder().registerStandard().strict()} (regla
 * de la revisión del contrato): una regla reportada que su grupo no declara hace lanzar al motor.</p>
 *
 * <p><b>Corrida final</b> ({@link #CORRIDA_FINAL}): desde el cierre de la Fase 3 (C:I3-42, 2026-11-24)
 * está activa <b>por omisión</b>: una regla sin negativo, una fila derivada que no se dispara, una regla
 * sin conjunto que la declare o una mutación que termina en NPE sin regla ponen el build en rojo con
 * {@code mvn test} a secas (CA3-03, CA3-04). Para desarrollar una regla nueva sin su negativo todavía se
 * puede apagar con {@code -Djbs.sifen.validacion.final=false}; nunca en la integración.</p>
 */
final class SoporteValidacion {

    /** CA3-03/CA3-04 exigidos: lo pendiente falla (por omisión desde C:I3-42; {@code =false} solo al desarrollar). */
    static final boolean CORRIDA_FINAL = Boolean.parseBoolean(System.getProperty("jbs.sifen.validacion.final", "true"));

    /** Nombre del setter de §3.5; mientras no exista, la parte (b) de la mutación se simula. */
    static final String SETTER_VALIDACION_PREVIA = "setPreValidationEnabled";

    private static final Method SETTER = buscarSetter();

    /** Zona de los casos del catálogo. */
    static final ZoneId ZONA = ZoneId.of("America/Asuncion");

    /**
     * Reloj fijo de las pruebas: la firma de los casos + 5 s. Con el del sistema, las reglas de plazo
     * (A004b a las 72 h, D002 a los 30 días) empezarían a saltar solas sobre los casos del catálogo,
     * que tienen fechas fijas.
     */
    static final Clock RELOJ = Clock.fixed(CasosDePrueba.FECHA_FIRMA.plusSeconds(5).atZone(ZONA).toInstant(), ZONA);

    private SoporteValidacion() {
    }

    /**
     * Desvíos de los casos de la Fase 2 respecto del MT que se dejan <b>como están</b> porque corregirlos
     * cambiaría un golden congelado (I3-14, decisión de la sesión principal del 2026-11-24): por caso,
     * los códigos de error que el validador debe dar y por qué. Agregado por app-developer.
     */
    static final Map<String, Map<String, String>> DESVIOS_ESPERADOS = Map.of(
            "DE-17", Map.of("D203", "B2C con país ARG: el MT pide B2F para un receptor del exterior"),
            // I4-11 (app-developer, 2026-11-24, decisión del usuario «Sí, escribir entero»): DE-18 ya no da
            // JBS-E-92 —la tasa 10.00 se guarda y se escribe 10— y deja de ser un desvío conocido.
            "DE-20", Map.of("D202a", "autofactura B2B: el MT pide B2C"),
            "DE-23", Map.of("E715", "nota de remisión sin cRelMerc (1805, DEC-06)",
                    "JBS-E-174", "dMarVeh «MARCA FICTICIA» de 14 caracteres (máximo 10)"),
            // E3-24 (app-developer, 2026-11-24): JBS-E-78 sigue al MT E719 (el ítem que aplica un anticipo
            // lleva el CDC); DE-14 aplica un anticipo por ítem sin CDC. El desvío de DE-30 (la FE de
            // anticipo) que había agregado test-developer desaparece con la corrección.
            "DE-14", Map.of("JBS-E-78", "aplica un anticipo por ítem sin dCDCAnticipo (MT E719, E3-24)"));

    /** Códigos de error esperados en un caso (vacío si no tiene desvíos). */
    static Set<String> desviosEsperados(String idCaso) {
        Map<String, String> d = DESVIOS_ESPERADOS.get(idCaso);
        return d == null ? Set.of() : d.keySet();
    }

    /** Origen del DE de un caso: armado por objeto o leído de su XML de entrada. */
    enum Origen {
        OBJ, XML;

        /**
         * Crea el DE del caso con la leyenda del ambiente de pruebas en {@code dNomEmi}: el emisor
         * ficticio de la Fase 2 no la trae y D105 la exige en DEV (I3-13, Q14). Se aplica en memoria;
         * los casos y los golden no cambian. Agregado por app-developer (2026-11-24).
         */
        DocumentoElectronico crear(CasosDePrueba.Caso caso) {
            DocumentoElectronico de = crearSinLeyenda(caso);
            if (de.getgDatGralOpe() != null && de.getgDatGralOpe().getgEmis() != null) {
                de.getgDatGralOpe().getgEmis().setdNomEmi(LEYENDA_EMISOR);
            }
            return de;
        }

        private DocumentoElectronico crearSinLeyenda(CasosDePrueba.Caso caso) {
            if (this == OBJ) {
                return caso.construir();
            }
            try {
                return caso.leer(FixturesSifen.entrada(caso));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (Exception e) {
                throw new IllegalStateException("No se pudo leer el caso " + caso.getId(), e);
            }
        }
    }

    /** Par caso + origen. */
    record Fuente(CasosDePrueba.Caso caso, Origen origen) {
        DocumentoElectronico crear() {
            return origen.crear(caso);
        }

        /** El DE con sus desvíos conocidos corregidos ({@link SoporteValidacion#CORRECCIONES}). */
        DocumentoElectronico crearCorregido() {
            DocumentoElectronico de = crear();
            corregir(caso.getId(), de);
            return de;
        }

        String id() {
            return caso.getId() + "/" + origen.name().toLowerCase();
        }

        @Override
        public String toString() {
            return id();
        }
    }

    /** Todos los casos de DE del catálogo, primero los armados por objeto y después los leídos. */
    static List<Fuente> fuentes() {
        List<Fuente> lista = new ArrayList<>();
        for (CasosDePrueba.Caso c : CasosDePrueba.documentos()) {
            if (c.tieneConstructor()) {
                lista.add(new Fuente(c, Origen.OBJ));
            }
        }
        for (CasosDePrueba.Caso c : CasosDePrueba.documentos()) {
            lista.add(new Fuente(c, Origen.XML));
        }
        return lista;
    }

    /** Busca un caso por su id ({@code DE-01}). */
    static CasosDePrueba.Caso caso(String id) {
        for (CasosDePrueba.Caso c : CasosDePrueba.documentos()) {
            if (c.getId().equals(id)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Caso inexistente: " + id);
    }

    /** Validador de las pruebas del catálogo: las reglas estándar en modo estricto. */
    static DEValidator validador() {
        return DEValidator.builder().registerStandard().strict().clock(RELOJ).build();
    }

    // ------------------------------------------------------------------ bases corregidas y eventos armados

    /**
     * Correcciones de los desvíos conocidos de los casos congelados (I3-14), para los casos base de
     * los negativos: ruta (con {@code [*]} = cada elemento) y valor de {@code set:}. Un test exige que
     * con ellas el caso quede sin errores.
     */
    static final Map<String, List<String[]>> CORRECCIONES = Map.of(
            // Todos: el primer ítem con la leyenda de pruebas de la Guía (JBS-E-70, AVISO; Q14).
            "*", List.<String[]>of(new String[] {"gDtipDE.gCamItem[0].dDesProSer", SoporteValidacion.LEYENDA_ITEM}),
            "DE-17", List.<String[]>of(new String[] {"gDatGralOpe.gDatRec.iTiOpe", "B2F"},
                    new String[] {"gDatGralOpe.gDatRec.dDirRec", "AVENIDA FICTICIA 123"}),
            "DE-18", List.<String[]>of(new String[] {"gDtipDE.gCamItem[*].gCamIVA.dTasaIVA", "10"}),
            "DE-20", List.<String[]>of(new String[] {"gDatGralOpe.gDatRec.iTiOpe", "B2C"}),
            "DE-14", List.<String[]>of(new String[] {"gDtipDE.gCamItem[*].dCDCAnticipo", CasosDePrueba.CDC_FACTURA_SIMPLE}),
            "DE-23", List.<String[]>of(new String[] {"gDtipDE.gCamItem[*].cRelMerc", "TOLERANCIA_DE_QUIEBRA"},
                    new String[] {"gDtipDE.gCamItem[*].dCanQuiMer", "1"},
                    new String[] {"gDtipDE.gTransp.gVehTras[*].dMarVeh", "MARCA"}));

    /**
     * Desvíos conocidos de los eventos congelados de la Fase 2 (los únicos errores admitidos en su
     * positivo; se corrigen en la base de los negativos con {@link #corregirEvento}).
     */
    static final Map<String, Set<String>> DESVIOS_EVENTOS = Map.of(
            "EV-05", Set.of("GENFE002", "GENFE011"), // E8-01: sin JBS-EVENTO-43, de baja
            "EV-06", Set.of("GENFE002"));

    /**
     * Corrige los desvíos de un evento del catálogo: CDC de la nominación con su DV de módulo 11,
     * descripción del documento del receptor con la del MT y sin la ciudad que la librería escribe con
     * otra etiqueta (E0-12).
     */
    static void corregirEvento(String idCaso, TrGesEve e) {
        if (!DESVIOS_EVENTOS.containsKey(idCaso)) {
            return;
        }
        org.javabeanstack.sifen.core.fields.request.event.TrGeVeNom nom = e.getgGroupTiEvt().getrGeVeNom();
        String id = nom.getId();
        nom.setId(id.substring(0, 43) + SifenUtil.generateDv(id.substring(0, 43)));
        if ("EV-05".equals(idCaso)) {
            nom.setdDTipIDRec(TiTipDocRec.CEDULA_PARAGUAYA.getDescripcion());
            // E8-01: la ciudad se conserva; con E0-12 corregido (§6.1) sale como dDesCiuRec y la valida JBS-EVENTO-56
        }
    }

    /** Aplica a {@code de} las correcciones de sus desvíos conocidos ({@link #CORRECCIONES}). */
    static void corregir(String idCaso, DocumentoElectronico de) {
        List<String[]> todas = new ArrayList<>(CORRECCIONES.get("*"));
        todas.addAll(CORRECCIONES.getOrDefault(idCaso, List.of()));
        for (String[] c : todas) {
            for (int i = 0; ; i++) {
                String ruta = c[0].replace("[*]", "[" + i + "]");
                GrafoDe.Destino d = GrafoDe.resolver(de, ruta);
                if (d == null) {
                    break;
                }
                d.poner(GrafoDe.convertir(d.tipo(), c[1]));
                if (!c[0].contains("[*]")) {
                    break;
                }
            }
        }
    }

    /** Notificación de recepción válida, de un receptor contribuyente o no. */
    static TrGeVeNotRec notificacion(boolean contribuyente) {
        TrGeVeNotRec n = new TrGeVeNotRec();
        n.setId(CasosDePrueba.CDC_FACTURA_SIMPLE);
        n.setdFecEmi(CasosDePrueba.FECHA_EMISION);
        n.setdFecRecep(CasosDePrueba.FECHA_EMISION.plusSeconds(5));
        n.setdNomRec("CLIENTE FICTICIO S.A.");
        if (contribuyente) {
            n.setiTipRec(TiNatRec.CONTRIBUYENTE);
            n.setdRucRec(RUC_FICTICIO);
            n.setdDVRec(SifenUtil.generateDv(RUC_FICTICIO));
        } else {
            n.setiTipRec(TiNatRec.NO_CONTRIBUYENTE);
            n.setdTipIDRec(TiTipDocRec.CEDULA_PARAGUAYA);
            n.setdNumID("1234567");
        }
        n.setdTotalGs(new java.math.BigDecimal("100000"));
        return n;
    }

    /** Desconocimiento válido, de un receptor contribuyente o no. */
    static TrGeVeDescon desconocimiento(boolean contribuyente) {
        TrGeVeDescon d = new TrGeVeDescon();
        d.setId(CasosDePrueba.CDC_FACTURA_SIMPLE);
        d.setdFecEmi(CasosDePrueba.FECHA_EMISION);
        d.setdFecRecep(CasosDePrueba.FECHA_EMISION.plusSeconds(5));
        d.setdNomRec("CLIENTE FICTICIO S.A.");
        if (contribuyente) {
            d.setiTipRec(TiNatRec.CONTRIBUYENTE);
            d.setdRucRec(RUC_FICTICIO);
            d.setdDVRec(SifenUtil.generateDv(RUC_FICTICIO));
        } else {
            d.setiTipRec(TiNatRec.NO_CONTRIBUYENTE);
            d.setdTipIDRec(TiTipDocRec.CEDULA_PARAGUAYA);
            d.setdNumID("1234567");
        }
        d.setmOtEve("Desconocimiento de prueba del receptor");
        return d;
    }

    /** Actualización de datos del transporte válida para el motivo dado (1 local, 2 chofer, 3 transportista, 4 vehículo). */
    static TrGeVeTr transporte(int motivo) {
        TrGeVeTr t = new TrGeVeTr();
        t.setId(CasosDePrueba.CDC_FACTURA_SIMPLE);
        switch (motivo) {
            case 1 -> {
                t.setdMotEv(TdMotEv.CAMBIO_LOCAL_ENTREGA);
                t.setcDepEnt(TDepartamento.CAPITAL);
                t.setcCiuEnt((short) 1);
                t.setdDesCiuEnt("ASUNCION (DISTRITO)");
                t.setdDirEnt("CALLE FICTICIA 1");
                t.setdNumCas(10);
            }
            case 2 -> {
                t.setdMotEv(TdMotEv.CAMBIO_CHOFER);
                t.setdNomChof("CHOFER FICTICIO");
                t.setdNumIDChof("1234567");
            }
            case 3 -> {
                t.setdMotEv(TdMotEv.CAMBIO_TRANSPORTISTA);
                t.setiNatTrans(TiNatRec.CONTRIBUYENTE);
                t.setdRucTrans(RUC_FICTICIO);
                t.setdDVTrans(SifenUtil.generateDv(RUC_FICTICIO));
                t.setdNomTrans("TRANSPORTES FICTICIOS");
            }
            case 5 -> {
                // Transportista no contribuyente (con documento en vez de RUC).
                t.setdMotEv(TdMotEv.CAMBIO_TRANSPORTISTA);
                t.setiNatTrans(TiNatRec.NO_CONTRIBUYENTE);
                t.setiTipIDTrans(TiTipDocRec.CEDULA_PARAGUAYA);
                t.setdNumIDTrans("1234567");
                t.setdNomTrans("TRANSPORTISTA FICTICIO");
            }
            default -> {
                t.setdMotEv(TdMotEv.CAMBIO_VEHICULO);
                t.setiTipTrans(TiTTrans.PROPIO);
                t.setiModTrans(TiModTrans.TERRESTRE);
                t.setdTiVehTras("CAMION");
                t.setdMarVeh("MARCA");
                t.setdTipIdenVeh(TdTipIdenVeh.NRO_IDENTIFICACION);
                t.setdNroIDVeh("ABC123");
            }
        }
        return t;
    }

    /** RUC ficticio con DV distinto de 0 (para que una regla de DV pueda distinguir el bueno del malo). */
    static final String RUC_FICTICIO = "80012346";

    /** Leyenda del primer ítem en el ambiente de pruebas (Guía de pruebas, JBS-E-70). */
    static final String LEYENDA_ITEM = "DOCUMENTO ELECTRÓNICO SIN VALOR COMERCIAL NI FISCAL - GENERADO EN AMBIENTE DE PRUEBA";

    /**
     * DE enriquecidos para la mutación: casos del catálogo con grupos opcionales cargados que ningún
     * caso trae (tarjeta, cheque, responsable, vehículo nuevo, seguros, datos adicionales, transportista
     * no contribuyente), para anular sus datos con la condición del fork cumplida y no solo con el grupo
     * vacío. Cada entrada: id, caso base y pares ruta ⇒ valor ({@code set:} de la tabla).
     */
    static final List<Object[]> ENRIQUECIDOS = List.of(
            new Object[] {"DE-05+tarjeta", "DE-05", new String[][] {
                {"gDtipDE.gCamCond.gPaConEIni[0].iTiPago", "TARJETA_DE_CREDITO"},
                {"gDtipDE.gCamCond.gPaConEIni[0].gPagTarCD.iDenTarj", "VISA"},
                {"gDtipDE.gCamCond.gPaConEIni[0].gPagTarCD.iForProPa", "POS"},
                {"gDtipDE.gCamCond.gPaConEIni[0].gPagTarCD.dRSProTar", "PROCESADORA FICTICIA"},
                {"gDtipDE.gCamCond.gPaConEIni[0].gPagTarCD.dRUCProTar", "80012346"},
                {"gDtipDE.gCamCond.gPaConEIni[0].gPagTarCD.dDVProTar", "8"},
                {"gDtipDE.gCamCond.gPaConEIni[0].gPagTarCD.dCodAuOpe", "123456"},
                {"gDtipDE.gCamCond.gPaConEIni[0].gPagTarCD.dNomTit", "TITULAR FICTICIO"},
                {"gDtipDE.gCamCond.gPaConEIni[0].gPagTarCD.dNumTarj", "1234"}}},
            new Object[] {"DE-05+cheque", "DE-05", new String[][] {
                {"gDtipDE.gCamCond.gPaConEIni[0].iTiPago", "CHEQUE"},
                {"gDtipDE.gCamCond.gPaConEIni[0].gPagCheq.dNumCheq", "12345678"},
                {"gDtipDE.gCamCond.gPaConEIni[0].gPagCheq.dBcoEmi", "BANCO FICTICIO"}}},
            new Object[] {"DE-05+extras", "DE-05", new String[][] {
                {"gDatGralOpe.gEmis.gRespDE.iTipIDRespDE", "CEDULA_PARAGUAYA"},
                {"gDatGralOpe.gEmis.gRespDE.dNumIDRespDE", "1234567"},
                {"gDatGralOpe.gEmis.gRespDE.dNomRespDE", "RESPONSABLE FICTICIO"},
                {"gDatGralOpe.gEmis.gRespDE.dCarRespDE", "GERENTE GENERAL"},
                {"gDtipDE.gCamItem[0].gVehNuevo.iTipOpVN", "VENTA_AL_CONSUMIDOR_FINAL"},
                {"gDtipDE.gCamItem[0].gVehNuevo.iTipCom", "GASOLINA"},
                {"gDtipDE.gCamItem[0].gVehNuevo.dChasis", "CHASIS12345678901"},
                {"gDtipDE.gCamItem[0].gVehNuevo.dColor", "BLANCO"},
                {"gDtipDE.gCamItem[0].gVehNuevo.dNroMotor", "MOTOR123456"},
                {"gDtipDE.gCamItem[0].gVehNuevo.cTipVeh", "AUTOMOVIL"},
                {"gDtipDE.gCamItem[0].gVehNuevo.dCilin", "1600"},
                {"gDtipDE.gCamEsp.gGrupSeg.dCodEmpSeg", "SEG01234567890123456"},
                {"gDtipDE.gCamEsp.gGrupSeg.gGrupPolSeg[0].dPoliza", "POL-1"},
                {"gDtipDE.gCamEsp.gGrupSeg.gGrupPolSeg[0].dUnidVig", "MESES"},
                {"gDtipDE.gCamEsp.gGrupSeg.gGrupPolSeg[0].dVigencia", "12"},
                {"gDtipDE.gCamEsp.gGrupSeg.gGrupPolSeg[0].dNumPoliza", "POL-0001"},
                {"gDtipDE.gCamEsp.gGrupAdi.dCiclo", "CICLO-1"},
                {"gDtipDE.gCamEsp.gGrupAdi.dFecIniC", "2026-11-01"},
                {"gDtipDE.gCamEsp.gGrupAdi.dFecFinC", "2026-11-30"}}},
            new Object[] {"DE-23+transportista", "DE-23", new String[][] {
                {"gDtipDE.gTransp.gCamTrans.iNatTrans", "NO_CONTRIBUYENTE"},
                {"gDtipDE.gTransp.gCamTrans.dRucTrans", "null"},
                {"gDtipDE.gTransp.gCamTrans.dDVTrans", "0"},
                {"gDtipDE.gTransp.gCamTrans.iTipIDTrans", "CEDULA_PARAGUAYA"},
                {"gDtipDE.gTransp.gCamTrans.dNumIDTrans", "1234567"}}});

    /** Fuentes enriquecidas ({@link #ENRIQUECIDOS}): el caso por objeto, corregido y con los grupos cargados. */
    static List<FuenteEnriquecida> fuentesEnriquecidas() {
        List<FuenteEnriquecida> l = new ArrayList<>();
        for (Object[] e : ENRIQUECIDOS) {
            Fuente base = new Fuente(caso((String) e[1]), Origen.OBJ);
            String[][] pares = (String[][]) e[2];
            l.add(new FuenteEnriquecida((String) e[0], () -> {
                DocumentoElectronico de = base.crearCorregido();
                for (String[] par : pares) {
                    GrafoDe.Destino d = GrafoDe.resolver(de, par[0]);
                    if (d == null) {
                        d = GrafoDe.crearCamino(de, par[0]);
                    }
                    d.poner(GrafoDe.convertir(d.tipo(), par[1]));
                }
                return de;
            }));
        }
        return l;
    }

    /**
     * Casos de la Fase 12 que estrenan grupos (plan JBSSIF, §12.2.4): tarjeta y débito sin procesadora, cheque,
     * pago en otra moneda, cuotas con entrega, B2G con {@code gGrupAdi}, autofactura a un extranjero,
     * {@code gTransp} y {@code gCamCarg} en una factura (exportación), remisión a un receptor del exterior,
     * vehículo nuevo, rastreo de la mercadería y los cuatro grupos de {@code gCamEsp}. DE-55 y DE-56 quedan
     * fuera (cientos de ítems iguales: no agregan rutas y multiplican el tiempo). {@code gCompPub} ya lo muta
     * DE-15, que es de las fuentes del catálogo.
     */
    static final List<String> FASE12 = List.of("DE-44", "DE-45", "DE-46", "DE-47", "DE-48", "DE-33", "DE-42", "DE-34",
            "DE-59", "DE-63", "DE-68", "DE-64", "DE-65", "DE-66", "DE-67");

    /**
     * Hallazgos de la Fase 12 que la mutación encuentra y la fase no corrige (D54): expresión sobre
     * {@code "<ruta> <tipo>"} → código. Vacío: el único, E12-02 (un {@code null} dentro de {@code dVencPag} no lo
     * reportaba ninguna regla y {@code TgGrupAdi.setupSOAPElements} daba NPE también con la validación activa), se
     * corrigió en la fase por decisión del usuario (2026-11-25, ESC-40): lo rechaza JBS-E-195 y el armado omite el nulo.
     */
    static final Map<String, String> HALLAZGOS_FASE12 = Map.of();

    /** Fuentes en las que cada hallazgo conocido tiene que seguir apareciendo (vacío: ver {@link #HALLAZGOS_FASE12}). */
    static final Map<String, Set<String>> HALLAZGO_ESPERADO_EN = Map.of();

    /** Fuentes de la Fase 12 ({@link #FASE12}), armadas por objeto desde {@code CasosFase12}. */
    static List<FuenteEnriquecida> fuentesFase12() {
        List<FuenteEnriquecida> l = new ArrayList<>();
        for (String id : FASE12) {
            l.add(new FuenteEnriquecida(id + "/f12", org.javabeanstack.sifen.test.casos.CasosFase12.todos().get(id)));
        }
        // DE-67 trae los tres dVencPag del máximo: un nulo agregado da además JBS-E-143 (más de 3); con dos, el nulo
        // queda solo y lo tiene que reportar JBS-E-195 en su índice (E12-02)
        l.add(new FuenteEnriquecida("DE-67/f12-2venc", () -> {
            DocumentoElectronico de = org.javabeanstack.sifen.test.casos.CasosFase12.todos().get("DE-67").get();
            de.getgDtipDE().getgCamEsp().getgGrupAdi().getdVencPagList().remove(2);
            return de;
        }));
        return l;
    }

    /** DE enriquecido a mutar. */
    record FuenteEnriquecida(String id, java.util.function.Supplier<DocumentoElectronico> fabrica) {
        DocumentoElectronico crear() {
            return fabrica.get();
        }

        @Override
        public String toString() {
            return id;
        }
    }

    /** Fuentes de eventos para la mutación: los 6 del catálogo (corregidos) y los 8 que ningún caso trae. */
    static List<FuenteEvento> fuentesEvento() {
        List<FuenteEvento> l = new ArrayList<>();
        for (CasosDePrueba.CasoEvento c : CasosDePrueba.eventos()) {
            l.add(new FuenteEvento(c.getId(), () -> {
                TrGesEve ev = c.construir().getrGesEveList().get(0);
                corregirEvento(c.getId(), ev);
                return ev;
            }));
        }
        CasosDePrueba.CasoEvento primero = CasosDePrueba.eventos().get(0);
        String[] nombres = {"notRec1", "notRec2", "descon1", "descon2", "tr1", "tr2", "tr3", "tr4", "tr5"};
        for (String nombre : nombres) {
            l.add(new FuenteEvento(primero.getId() + "+" + nombre, () -> {
                TrGesEve ev = primero.construir().getrGesEveList().get(0);
                TgGroupTiEvt g = new TgGroupTiEvt();
                switch (nombre) {
                    case "notRec1" -> g.setrGeVeNotRec(notificacion(true));
                    case "notRec2" -> g.setrGeVeNotRec(notificacion(false));
                    case "descon1" -> g.setrGeVeDescon(desconocimiento(true));
                    case "descon2" -> g.setrGeVeDescon(desconocimiento(false));
                    default -> g.setrGeVeTr(transporte(nombre.charAt(2) - '0'));
                }
                ev.setgGroupTiEvt(g);
                return ev;
            }));
        }
        return l;
    }

    /** Evento a mutar: id y fábrica de instancias nuevas. */
    record FuenteEvento(String id, java.util.function.Supplier<TrGesEve> fabrica) {
        TrGesEve crear() {
            return fabrica.get();
        }

        @Override
        public String toString() {
            return id;
        }
    }

    // ------------------------------------------------------------------ catálogo de reglas: único punto
    // Todo lo que las pruebas de §3.4 saben de la implementación de las reglas pasa por acá (aviso del
    // 2026-11-24: las reglas se van a reorganizar en clases separadas con vigencia por NT). Las pruebas
    // nombran las reglas solo por su código; si cambia dónde viven, se adapta este bloque y nada más.

    /** Leyenda del nombre del emisor en el ambiente de pruebas (MT 12.4, D105; Q14). */
    static final String LEYENDA_EMISOR = "DE generado en ambiente de prueba - sin valor comercial ni fiscal";

    /** Código de la regla del motor «un grupo lanzó» (nunca debe aparecer fuera de su propio test). */
    static final String VAL_00 = "JBS-VAL-00";
    /** Código de «no se recibió el documento». */
    static final String VAL_01 = "JBS-VAL-01";
    /** Código de «sin contexto de generación». */
    static final String VAL_02 = "JBS-VAL-02";
    /** Reglas del motor: no las declara ningún grupo. */
    static final Set<String> DEL_MOTOR = Set.of(VAL_00, VAL_01, VAL_02);

    /** Lo que las pruebas necesitan saber de una regla del catálogo. */
    record Regla(String codigo, String ruta, Severity severidad, boolean evento, boolean tieneMensaje) {
        @Override
        public String toString() {
            return codigo;
        }
    }

    /** Todas las reglas del catálogo, en su orden. */
    static List<Regla> reglas() {
        List<Regla> l = new ArrayList<>();
        java.time.LocalDate hoy = java.time.LocalDate.now(RELOJ);
        for (ValidationRule r : RuleCatalog.all()) { // §3.6 (D33): el catálogo sale de los conjuntos
            // §6.2/§6.3 (app-developer): una regla dada de baja (until) no se evalúa ni tiene negativo; E8-01: tampoco la
            // que tiene fecha de baja posterior al reloj de los tests (JBS-EVENTO-43, baja de una guarda de la librería)
            if (delDocumento(r) && r.isInForceOn(hoy) && r.getEffectivePeriod().until() == null) { // §3.7: las de configuración y certificado tienen su propia prueba
                l.add(regla(r));
            }
        }
        return l;
    }

    /** La regla de ese código, si existe en el catálogo. */
    static Optional<Regla> regla(String codigo) {
        return RuleCatalog.byCode(codigo).map(SoporteValidacion::regla);
    }

    private static Regla regla(ValidationRule r) {
        return new Regla(r.getCode(), r.getPath(), r.getSeverity(), r.getGroup() == RuleGroup.EVENTO,
                ValidationMessages.hasMessage(r.getCode()));
    }

    /**
     * Regla del DE o de un evento: las {@code JBS-CFG-*} y {@code JBS-CERT-*} (§3.7) no son del documento y
     * las prueba {@code ConfiguracionValidatorTest}.
     */
    static boolean delDocumento(ValidationRule r) {
        return r.getGroup() != RuleGroup.CFG && r.getGroup() != RuleGroup.CERT;
    }

    /** Ruta canónica de una ruta concreta (índices a {@code []}). */
    static String normalizarRuta(String ruta) {
        return ValidationRule.normalizePath(ruta);
    }

    /** Códigos que declaran los grupos estándar del DE. */
    static Set<String> declaradasDe() {
        Set<String> s = new HashSet<>();
        for (GroupValidator<?> g : validador().getGroups()) {
            g.getRules().forEach(r -> s.add(r.getCode()));
        }
        return s;
    }

    /** Códigos que declaran los grupos estándar de los eventos. */
    static Set<String> declaradasEventos() {
        Set<String> s = new HashSet<>();
        for (GroupValidator<?> g : validadorEventos().getGroups()) {
            g.getRules().forEach(r -> s.add(r.getCode()));
        }
        return s;
    }

    /** Cantidad de grupos estándar (DE, eventos), para el informe. */
    static int[] cantidadGrupos() {
        return new int[] {validador().getGroups().size(), validadorEventos().getGroups().size()};
    }

    /**
     * Reglas que la librería declara como no evaluadas localmente (constantes, derivadas, calculadas):
     * código ⇒ motivo. No llevan negativo (I3-15). Desde §3.6 (D33) salen de
     * {@link RuleCatalog#notEvaluated()} (cada conjunto las declara con {@code declareNotEvaluated}).
     */
    static Map<String, String> noEvaluadas() {
        Map<String, String> m = new LinkedHashMap<>();
        for (Map.Entry<ValidationRule, NotEvaluatedReason> e : RuleCatalog.notEvaluated().entrySet()) {
            if (delDocumento(e.getKey())) {
                m.put(e.getKey().getCode(), e.getValue().name());
            }
        }
        return m;
    }

    /** Validador estándar del DE con un grupo extra que lanza una NPE con un texto sensible. */
    static DEValidator validadorConGrupoQueLanza(String nombre, String textoSensible) {
        GroupValidator<DocumentoElectronico> roto = new GroupValidator<>() {
            @Override
            public String getName() {
                return nombre;
            }

            @Override
            public Set<ValidationRule> getRules() {
                return Set.of();
            }

            @Override
            public void validate(DocumentoElectronico raiz, ValidationContext c) {
                throw new NullPointerException(textoSensible);
            }
        };
        return DEValidator.builder().registerStandard().register(roto).strict().clock(RELOJ).build();
    }

    /** Validador de eventos de las pruebas: las reglas estándar en modo estricto, con el mismo reloj. */
    static EventValidator validadorEventos() {
        return EventValidator.builder().registerStandard().strict().clock(RELOJ).build();
    }

    /** {@code true} si §3.5 ya agregó {@code SifenConfig.setPreValidationEnabled(boolean)}. */
    static boolean fachadaIntegrada() {
        return SETTER != null;
    }

    /**
     * Configuración de las pruebas con la validación previa en el valor pedido (si §3.5 todavía no
     * existe, la configuración no valida y {@code conValidacion} se ignora).
     */
    static SifenConfig configuracion(boolean conValidacion) {
        SifenConfig cfg = FixturesSifen.configuracion();
        if (SETTER != null) {
            try {
                SETTER.invoke(cfg, conValidacion);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
        return cfg;
    }

    static GenerationCtx contexto(SifenConfig cfg) {
        return FixturesSifen.contexto(cfg);
    }

    private static Method buscarSetter() {
        try {
            return SifenConfig.class.getMethod(SETTER_VALIDACION_PREVIA, boolean.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /** La primera {@link NullPointerException} de la cadena de causas, o {@code null}. */
    static NullPointerException npeEnCadena(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause() == c ? null : c.getCause()) {
            if (c instanceof NullPointerException npe) {
                return npe;
            }
        }
        return null;
    }

    /** Descripción corta de una falla: clase de la excepción (o de la NPE de la cadena) y su origen. */
    static String describir(Throwable t) {
        NullPointerException npe = npeEnCadena(t);
        Throwable foco = npe != null ? npe : t;
        String donde = "";
        for (StackTraceElement e : foco.getStackTrace()) {
            if (e.getClassName().startsWith("org.javabeanstack.sifen.")) {
                donde = " en " + e.getClassName().substring(e.getClassName().lastIndexOf('.') + 1) + "."
                        + e.getMethodName() + ":" + e.getLineNumber();
                break;
            }
        }
        return (npe != null ? "NPE" : foco.getClass().getSimpleName()) + donde;
    }
}
