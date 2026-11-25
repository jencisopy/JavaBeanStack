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
import org.javabeanstack.sifen.core.beans.response.RespuestaRecepcionLoteDE;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.fields.request.de.*;
import org.javabeanstack.sifen.core.types.*;
import org.junit.jupiter.api.Assertions;
import org.javabeanstack.sifen.test.SifenTestEnv;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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
public class DEMonedaExtranjeraTest extends DETestBase {

    @Test
    @Disabled("I1-08: emisor y timbrado de un tercero, de 2021 (datos ficticios desde I3-64 e I3-65); lo reemplaza SifenVivoTest con el emisor de SifenTestEnv")
    public void testRecepcionLoteDE() throws SifenException {
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
        gTimb.setdFeIniT(LocalDate.parse("2019-07-24"));
        de.setgTimb(gTimb);

        // Grupo D
        TdDatGralOpe dDatGralOpe = new TdDatGralOpe();
        dDatGralOpe.setdFeEmiDE(currentDate);

        TgOpeCom gOpeCom = new TgOpeCom();
        gOpeCom.setiTipTra(TTipTra.PRESTACION_SERVICIOS);
        gOpeCom.setiTImp(TTImp.IVA);
        gOpeCom.setcMoneOpe(CMondT.USD);

        // Tipo de Cambio

        gOpeCom.setdCondTiCam(TdCondTiCam.GLOBAL);
        gOpeCom.setdTiCam(BigDecimal.valueOf(7135.0));

        dDatGralOpe.setgOpeCom(gOpeCom);



        TgEmis gEmis = new TgEmis();
        gEmis.setdRucEm("80111111");
        gEmis.setdDVEmi("0");
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
        gActEco.setcActEco("62010");
        gActEco.setdDesActEco("ACTIVIDADES de PROGRAMACIÓN INFORMÁTICA");
        gActEcoList.add(gActEco);

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

        List<TgCamItem> gCamItemList = new ArrayList<>();

        TgCamItem gCamItem = new TgCamItem();
        gCamItem.setdCodInt("001");
        gCamItem.setdDesProSer("Horas de Programación de Software");
        gCamItem.setcUniMed(TcUniMed.M2);
        gCamItem.setdCantProSer(BigDecimal.valueOf(13.6647));

        TgValorItem gValorItem = new TgValorItem();
        gValorItem.setdPUniProSer(BigDecimal.valueOf(95.847));

        TgValorRestaItem gValorRestaItem = new TgValorRestaItem();
        gValorRestaItem.setdDescItem(BigDecimal.valueOf(5.3));
        gValorItem.setgValorRestaItem(gValorRestaItem);
        gCamItem.setgValorItem(gValorItem);

        TgCamIVA gCamIVA = new TgCamIVA();
        gCamIVA.setiAfecIVA(TiAfecIVA.GRAVADO);
        gCamIVA.setdPropIVA(BigDecimal.valueOf(100));
        gCamIVA.setdTasaIVA(BigDecimal.valueOf(10));
        gCamItem.setgCamIVA(gCamIVA);

        gCamItemList.add(gCamItem);

        gDtipDE.setgCamItemList(gCamItemList);
        de.setgDtipDE(gDtipDE);

        // Grupo E
        TgTotSub tgTotSub = new TgTotSub();
        de.setgTotSub(tgTotSub);

        RespuestaRecepcionLoteDE ret = Sifen.recepcionLoteDE(Collections.singletonList(de));
        Assertions.assertEquals(200, ret.getCodigoEstado());
        Assertions.assertEquals("0300", ret.getdCodRes());
        Assertions.assertEquals("Lote recibido con éxito", ret.getdMsgRes());
        LOGGER.info(ret.toString());
    }

}
