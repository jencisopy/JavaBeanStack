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
package org.javabeanstack.sifen.test.de;

import org.javabeanstack.sifen.Sifen;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.beans.EventosDE;
import org.javabeanstack.sifen.core.beans.ValidezFirmaDigital;
import org.javabeanstack.sifen.core.beans.response.*;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.fields.request.de.*;
import org.javabeanstack.sifen.core.fields.request.event.TgGroupTiEvt;
import org.javabeanstack.sifen.core.fields.request.event.TrGeVeDisconf;
import org.javabeanstack.sifen.core.fields.request.event.TrGesEve;
import org.javabeanstack.sifen.core.types.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.javabeanstack.sifen.test.SifenTestEnv;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test en vivo contra el ambiente de test de SIFEN (plan JBSSIF, §1.8): requiere la configuración
 * de {@link SifenTestEnv}; sin ella la clase queda saltada.
 */
@Tag("sifen-live")
public class DETest extends DETestBase {
    @Test
    @Disabled("I1-08: emisor y timbrado de un tercero, de 2021 (datos ficticios desde I3-64); lo reemplaza SifenVivoTest con el emisor de SifenTestEnv")
    public void testRecepcionDE() throws SifenException {
        LocalDateTime currentDate = LocalDateTime.now();

        // Grupo A
        DocumentoElectronico DE = new DocumentoElectronico();
        DE.setdFecFirma(currentDate);
        DE.setdSisFact((short) 1);

        // Grupo B
        TgOpeDE gOpeDE = new TgOpeDE();
        gOpeDE.setiTipEmi(TTipEmi.NORMAL);
        DE.setgOpeDE(gOpeDE);

        // Grupo C
        TgTimb gTimb = new TgTimb();
        gTimb.setiTiDE(TTiDE.FACTURA_ELECTRONICA);
        gTimb.setdNumTim(12345678);
        gTimb.setdEst("001");
        gTimb.setdPunExp("002");
        gTimb.setdNumDoc("0000007");
        gTimb.setdFeIniT(LocalDate.parse("2019-07-31"));
        DE.setgTimb(gTimb);

        // Grupo D
        TdDatGralOpe dDatGralOpe = new TdDatGralOpe();
        dDatGralOpe.setdFeEmiDE(currentDate);

        TgOpeCom gOpeCom = new TgOpeCom();
        gOpeCom.setiTipTra(TTipTra.PRESTACION_SERVICIOS);
        gOpeCom.setiTImp(TTImp.IVA);
        gOpeCom.setcMoneOpe(CMondT.PYG);
        dDatGralOpe.setgOpeCom(gOpeCom);

        TgEmis gEmis = new TgEmis();
        gEmis.setdRucEm("80222222");
        gEmis.setdDVEmi("6");
        gEmis.setiTipCont(TiTipCont.PERSONA_JURIDICA);
        gEmis.setdNomEmi("DE generado en ambiente de prueba - sin valor comercial ni fiscal");
        gEmis.setdDirEmi("CALLE FICTICIA");
        gEmis.setdNumCas("123");
        gEmis.setcDepEmi(TDepartamento.CAPITAL);
        gEmis.setcCiuEmi(1);
        gEmis.setdDesCiuEmi("ASUNCION (DISTRITO)");
        gEmis.setdTelEmi("0210000000");
        gEmis.setdEmailE("facturacion@example.com");

        List<TgActEco> gActEcoList = new ArrayList<>();
        TgActEco gActEco = new TgActEco();
        gActEco.setcActEco("69209");
        gActEco.setdDesActEco("ACTIVIDADES DE CONTABILIDAD, TENEDURÍA DE LIBROS, AUDITORIA Y ASESORIA FISCAL N.C.P.");
        gActEcoList.add(gActEco);

        TgActEco gActEco2 = new TgActEco();
        gActEco2.setcActEco("62090");
        gActEco2.setdDesActEco("OTRAS ACTIVIDADES DE TECNOLOGÍA DE LA INFORMACIÓN Y SERVICIOS INFORMÁTICOS");
        gActEcoList.add(gActEco2);

        gEmis.setgActEcoList(gActEcoList);
        dDatGralOpe.setgEmis(gEmis);

        TgDatRec gDatRec = new TgDatRec();
        gDatRec.setiNatRec(TiNatRec.NO_CONTRIBUYENTE);
        gDatRec.setiTiOpe(TiTiOpe.B2C);
        gDatRec.setcPaisRec(PaisType.PRY);
        gDatRec.setiTipIDRec(TiTipDocRec.CEDULA_PARAGUAYA);
        gDatRec.setdNumIDRec("1234567");
        gDatRec.setdNomRec("PERSONA FICTICIA");
        dDatGralOpe.setgDatRec(gDatRec);
        DE.setgDatGralOpe(dDatGralOpe);

        // Grupo E
        TgDtipDE gDtipDE = new TgDtipDE();

        TgCamFE gCamFE = new TgCamFE();
        gCamFE.setiIndPres(TiIndPres.OPERACION_ELECTRONICA);
        gDtipDE.setgCamFE(gCamFE);

        TgCamCond gCamCond = new TgCamCond();
        gCamCond.setiCondOpe(TiCondOpe.CREDITO);

        TgPagCred gPagCred = new TgPagCred();
        gPagCred.setiCondCred(TiCondCred.PLAZO);
        gPagCred.setdPlazoCre("60 días");

        gCamCond.setgPagCred(gPagCred);
        gDtipDE.setgCamCond(gCamCond);

        List<TgCamItem> gCamItemList = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            TgCamItem gCamItem = new TgCamItem();
            gCamItem.setdCodInt(i == 0 ? "001" : "002");
            gCamItem.setdDesProSer(i == 0 ? "Servicio de Liquidación de IVA" : "Servicio de Liquidación de IRP");
            gCamItem.setcUniMed(TcUniMed.UNI);
            gCamItem.setdCantProSer(BigDecimal.valueOf(1));

            TgValorItem gValorItem = new TgValorItem();
            gValorItem.setdPUniProSer(BigDecimal.valueOf(120000));

            TgValorRestaItem gValorRestaItem = new TgValorRestaItem();
            gValorItem.setgValorRestaItem(gValorRestaItem);
            gCamItem.setgValorItem(gValorItem);

            TgCamIVA gCamIVA = new TgCamIVA();
            gCamIVA.setiAfecIVA(TiAfecIVA.GRAVADO);
            gCamIVA.setdPropIVA(BigDecimal.valueOf(100));
            gCamIVA.setdTasaIVA(BigDecimal.valueOf(10));
            gCamItem.setgCamIVA(gCamIVA);

            gCamItemList.add(gCamItem);
        }

        gDtipDE.setgCamItemList(gCamItemList);
        DE.setgDtipDE(gDtipDE);

        // Grupo E
        DE.setgTotSub(new TgTotSub());

        LOGGER.info("CDC del Documento Electrónico -> " + DE.obtenerCDC());

        RespuestaRecepcionDE ret = Sifen.recepcionDE(DE);
        LOGGER.info(ret.toString());
    }

    @Test
    @Disabled("I1-08: CDC fijo de 2021 de un tercero; SifenVivoTest consulta el CDC que recibe en la misma corrida")
    public void testConsultaDE() throws SifenException {
        RespuestaConsultaDE ret = Sifen.consultaDE("01802222226001002000000722021040613265708132");
        LOGGER.info(ret.toString());
    }

    @Test
    @Disabled("I1-08: registra un evento (disconformidad) sobre un CDC de un tercero de 2021; no se envía un evento sobre un DE ajeno")
    public void testRecepcionEvento() throws SifenException {
        LocalDateTime currentDate = LocalDateTime.now();

        // Evento de Cancelación
        /*TrGeVeCan trGeVeCan = new TrGeVeCan();
        trGeVeCan.setId("01802222226001002000000722021040613265708132");
        trGeVeCan.setmOtEve("Prueba de cancelación de documento electrónico");

        TgGroupTiEvt tgGroupTiEvt = new TgGroupTiEvt();
        tgGroupTiEvt.setrGeVeCan(trGeVeCan);*/

        // Evento de Conformidad
        /*TrGeVeConf trGeVeConf = new TrGeVeConf();
        trGeVeConf.setId("01802222226001002000000722021040613265708132");
        trGeVeConf.setiTipConf(TiTipConf.CONFORMIDAD_PARCIAL);
        trGeVeConf.setdFecRecep(currentDate);

        TgGroupTiEvt tgGroupTiEvt = new TgGroupTiEvt();
        tgGroupTiEvt.setrGeVeConf(trGeVeConf);*/

        // Evento de Disconformidad
        TrGeVeDisconf trGeVeDisconf = new TrGeVeDisconf();
        trGeVeDisconf.setId("01802222226001002000000722021040613265708132");
        trGeVeDisconf.setmOtEve("Prueba de disconformidad de documento electrónico");

        TgGroupTiEvt tgGroupTiEvt = new TgGroupTiEvt();
        tgGroupTiEvt.setrGeVeDisconf(trGeVeDisconf);

        TrGesEve rGesEve1 = new TrGesEve();
        rGesEve1.setId("1");
        rGesEve1.setdFecFirma(currentDate);
        rGesEve1.setgGroupTiEvt(tgGroupTiEvt);

        EventosDE eventosDE = new EventosDE();
        eventosDE.setrGesEveList(Collections.singletonList(rGesEve1));

        RespuestaRecepcionEvento ret = Sifen.recepcionEvento(eventosDE);
        LOGGER.info(ret.toString());
    }

    @Test
    @Disabled("Lee un archivo de una ruta local fija del autor original")
    public void testConversionXml() throws SifenException, IOException {
        String xml = new String(Files.readAllBytes(Paths.get("C:\\Users\\mzarz\\Desktop\\de.xml")), StandardCharsets.UTF_8);
        DocumentoElectronico DE = new DocumentoElectronico(xml);
        LOGGER.info(DE.toString());
    }

    private DocumentoElectronico setupDocumentoElectronico() {
        LocalDateTime currentDate = LocalDateTime.now();

        // Grupo A
        DocumentoElectronico de = new DocumentoElectronico();
        de.setdFecFirma(currentDate);
        de.setdSisFact((short) 1);

        // Grupo B
        TgOpeDE gOpeDE = new TgOpeDE();
        gOpeDE.setiTipEmi(TTipEmi.NORMAL);
        de.setgOpeDE(gOpeDE);

        // Grupo C
        TgTimb gTimb = new TgTimb();
        gTimb.setiTiDE(TTiDE.FACTURA_ELECTRONICA);
        gTimb.setdNumTim(12345678);
        gTimb.setdEst("001");
        gTimb.setdPunExp("002");
        gTimb.setdNumDoc("0000008");
        gTimb.setdFeIniT(LocalDate.parse("2019-07-31"));
        de.setgTimb(gTimb);

        // Grupo D
        TdDatGralOpe dDatGralOpe = new TdDatGralOpe();
        dDatGralOpe.setdFeEmiDE(currentDate);

        TgOpeCom gOpeCom = new TgOpeCom();
        gOpeCom.setiTipTra(TTipTra.PRESTACION_SERVICIOS);
        gOpeCom.setiTImp(TTImp.IVA);
        gOpeCom.setcMoneOpe(CMondT.PYG);
        dDatGralOpe.setgOpeCom(gOpeCom);

        TgEmis gEmis = new TgEmis();
        gEmis.setdRucEm("80222222");
        gEmis.setdDVEmi("6");
        gEmis.setiTipCont(TiTipCont.PERSONA_JURIDICA);
        gEmis.setdNomEmi("de generado en ambiente de prueba - sin valor comercial ni fiscal");
        gEmis.setdDirEmi("CALLE FICTICIA");
        gEmis.setdNumCas("123");
        gEmis.setcDepEmi(TDepartamento.CAPITAL);
        gEmis.setcCiuEmi(1);
        gEmis.setdDesCiuEmi("ASUNCION (DISTRITO)");
        gEmis.setdTelEmi("0210000000");
        gEmis.setdEmailE("facturacion@example.com");

        List<TgActEco> gActEcoList = new ArrayList<>();
        TgActEco gActEco = new TgActEco();
        gActEco.setcActEco("69209");
        gActEco.setdDesActEco("ACTIVIDADES de CONTABILIDAD, TENEDURÍA de LIBROS, AUDITORIA Y ASESORIA FISCAL N.C.P.");
        gActEcoList.add(gActEco);

        TgActEco gActEco2 = new TgActEco();
        gActEco2.setcActEco("62090");
        gActEco2.setdDesActEco("OTRAS ACTIVIDADES de TECNOLOGÍA de LA INFORMACIÓN Y SERVICIOS INFORMÁTICOS");
        gActEcoList.add(gActEco2);

        gEmis.setgActEcoList(gActEcoList);
        dDatGralOpe.setgEmis(gEmis);

        TgDatRec gDatRec = new TgDatRec();
        gDatRec.setiNatRec(TiNatRec.NO_CONTRIBUYENTE);
        gDatRec.setiTiOpe(TiTiOpe.B2C);
        gDatRec.setcPaisRec(PaisType.PRY);
        gDatRec.setiTipIDRec(TiTipDocRec.CEDULA_PARAGUAYA);
        gDatRec.setdNumIDRec("1234567");
        gDatRec.setdNomRec("PERSONA FICTICIA");
        dDatGralOpe.setgDatRec(gDatRec);
        de.setgDatGralOpe(dDatGralOpe);

        // Grupo E
        TgDtipDE gDtipDE = new TgDtipDE();

        TgCamFE gCamFE = new TgCamFE();
        gCamFE.setiIndPres(TiIndPres.OPERACION_ELECTRONICA);
        gDtipDE.setgCamFE(gCamFE);

        TgCamCond gCamCond = new TgCamCond();
        gCamCond.setiCondOpe(TiCondOpe.CREDITO);

        TgPagCred gPagCred = new TgPagCred();
        gPagCred.setiCondCred(TiCondCred.PLAZO);
        gPagCred.setdPlazoCre("60 días");

        gCamCond.setgPagCred(gPagCred);
        gDtipDE.setgCamCond(gCamCond);


        gDtipDE.setgCamItemList(new ArrayList<>());
        de.setgDtipDE(gDtipDE);

        // Grupo E
        de.setgTotSub(new TgTotSub());

        return de;

    }

    private TgCamItem createTgCamItem(
            String codigo,
            String descripcion,
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            TcUniMed uniMed,
            BigDecimal pctDescuento
    ) {
        TgCamItem gCamItem = new TgCamItem();

        gCamItem.setdCodInt(codigo);
        gCamItem.setdDesProSer(descripcion);
        gCamItem.setcUniMed(uniMed);
        gCamItem.setdCantProSer(cantidad);

        TgValorItem gValorItem = new TgValorItem();
        gValorItem.setdPUniProSer(precioUnitario);

        TgValorRestaItem gValorRestaItem = new TgValorRestaItem();
        gValorItem.setgValorRestaItem(gValorRestaItem);
        gValorRestaItem.setdDescItem(pctDescuento);
        gCamItem.setgValorItem(gValorItem);

        TgCamIVA gCamIVA = new TgCamIVA();
        gCamIVA.setiAfecIVA(TiAfecIVA.GRAVADO);
        gCamIVA.setdPropIVA(BigDecimal.valueOf(100));
        gCamIVA.setdTasaIVA(BigDecimal.valueOf(10));
        gCamItem.setgCamIVA(gCamIVA);

        return gCamItem;
    }

    @Test
    @Disabled("I1-08: emisor y timbrado de un tercero, de 2021; lo reemplaza SifenVivoTest con el emisor de SifenTestEnv")
    public void testRecepcionLoteDE() throws SifenException {

        DocumentoElectronico de = setupDocumentoElectronico();

        TgCamItem tgCamItem00 = createTgCamItem(
                "001",
                "Servicio de Liquidación de IVA",
                BigDecimal.valueOf(1),
                BigDecimal.valueOf(120000),
                TcUniMed.UNI,
                BigDecimal.valueOf(0)
        );

        TgCamItem tgCamItem01 = createTgCamItem(
                "002",
                "Servicio de Liquidación de IRP",
                BigDecimal.valueOf(1),
                BigDecimal.valueOf(88000),
                TcUniMed.UNI,
                BigDecimal.valueOf(1.3)
        );

        TgDtipDE tgDtipDE = de.getgDtipDE();
        tgDtipDE.getgCamItemList().add(tgCamItem00);
        tgDtipDE.getgCamItemList().add(tgCamItem01);

        RespuestaRecepcionLoteDE ret = Sifen.recepcionLoteDE(Collections.singletonList(de));
        Assertions.assertEquals(200, ret.getCodigoEstado());
        Assertions.assertEquals("0300", ret.getdCodRes());
        Assertions.assertEquals("Lote recibido con éxito", ret.getdMsgRes());
        LOGGER.info(ret.toString());
    }

    @Test
    @Disabled("I1-08: número de lote fijo de 2021; SifenVivoTest consulta el lote que recibe en la misma corrida")
    public void testConsultaLoteDe() throws SifenException {
        RespuestaConsultaLoteDE ret = Sifen.consultaLoteDE("100000000000001");
        LOGGER.info(ret.toString());
    }

    @Test
    @Disabled("Lee un archivo de una ruta local fija del autor original")
    public void testValidacionDE() {
        ValidezFirmaDigital validity = Sifen.validarFirmaDE("D:\\de.xml");
        LOGGER.info(validity.isValido() ? "Firma digital válida" : "Firma digital inválida");
    }

}
