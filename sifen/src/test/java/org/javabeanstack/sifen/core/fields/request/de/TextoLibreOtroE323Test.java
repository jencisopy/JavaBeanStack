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
package org.javabeanstack.sifen.core.fields.request.de;

import jakarta.xml.soap.SOAPElement;
import java.math.BigDecimal;
import org.javabeanstack.sifen.core.types.CMondT;
import org.javabeanstack.sifen.core.types.TTiDE;
import org.javabeanstack.sifen.core.types.TiCarCarga;
import org.javabeanstack.sifen.core.types.TiDenTarj;
import org.javabeanstack.sifen.core.types.TiForProPa;
import org.javabeanstack.sifen.core.types.TiIndPres;
import org.javabeanstack.sifen.core.types.TiNatRec;
import org.javabeanstack.sifen.core.types.TiTiOpe;
import org.javabeanstack.sifen.core.types.TiTiPago;
import org.javabeanstack.sifen.core.types.TiTipCom;
import org.javabeanstack.sifen.core.types.TiTipDocRec;
import org.javabeanstack.sifen.core.types.TiTipIDRespDE;
import org.javabeanstack.sifen.core.types.TiTipOpVN;
import org.javabeanstack.sifen.core.types.PaisType;
import org.javabeanstack.sifen.internal.helpers.SoapHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.NodeList;

/**
 * E3-23 (plan JBSSIF, §3.8; caso real del usuario, 2026-11-24): con el código «Otro» la librería
 * escribe el texto libre que cargó el consumidor, como pide el MT; con los demás códigos, la
 * descripción del código (un texto distinto cargado por el consumidor se ignora: la librería escribe
 * la correcta, así que D209a y sus pares no tienen por qué rechazarlo). Siete campos: {@code dDTipIDRec},
 * {@code dDTipIDRespDE}, {@code dDesIndPres}, {@code dDesTiPag}, {@code dDesDenTarj},
 * {@code dDesTipCom} y {@code dDesCarCarga}. El motivo «Otro» de la remisión ({@code dDesMotEmiNR})
 * no tiene campo en el modelo (I3-17, §6.2).
 */
public class TextoLibreOtroE323Test {
    private static final String LIBRE = "Texto libre del consumidor";

    private static SOAPElement padre() throws Exception {
        return SoapHelper.createSoapMessage().getSOAPBody().addChildElement("padre");
    }

    private static String texto(SOAPElement raiz, String nombre) {
        NodeList l = raiz.getElementsByTagName(nombre);
        return l.getLength() == 0 ? null : l.item(0).getTextContent();
    }

    @Test
    public void receptorConOtroEscribeElTextoLibre() throws Exception {
        TgDatRec r = new TgDatRec();
        r.setiNatRec(TiNatRec.NO_CONTRIBUYENTE);
        r.setiTiOpe(TiTiOpe.B2C);
        r.setcPaisRec(PaisType.PRY);
        r.setiTipIDRec(TiTipDocRec.OTRO);
        r.setdDTipIDRec(LIBRE);
        r.setdNumIDRec("123");
        SOAPElement p = padre();
        r.setupSOAPElements(p, TTiDE.FACTURA_ELECTRONICA);
        Assertions.assertEquals(LIBRE, texto(p, "dDTipIDRec"), "antes salía «Otro documento» (I0-09)");

        r.setiTipIDRec(TiTipDocRec.CEDULA_PARAGUAYA);
        SOAPElement q = padre();
        r.setupSOAPElements(q, TTiDE.FACTURA_ELECTRONICA);
        Assertions.assertEquals(TiTipDocRec.CEDULA_PARAGUAYA.getDescripcion(), texto(q, "dDTipIDRec"),
                "con otro código, la descripción del MT aunque el consumidor cargue otra");

        r.setiTipIDRec(TiTipDocRec.OTRO);
        r.setdDTipIDRec(null);
        SOAPElement s = padre();
        r.setupSOAPElements(s, TTiDE.FACTURA_ELECTRONICA);
        Assertions.assertEquals(TiTipDocRec.OTRO.getDescripcion(), texto(s, "dDTipIDRec"), "sin texto, la línea base (D209a lo rechaza)");
    }

    @Test
    public void losDemasCamposOtroTambien() throws Exception {
        TgRespDE resp = new TgRespDE();
        resp.setiTipIDRespDE(TiTipIDRespDE.OTRO);
        resp.setdDTipIDRespDE(LIBRE);
        SOAPElement p1 = padre();
        resp.setupSOAPElements(p1);
        Assertions.assertEquals(LIBRE, texto(p1, "dDTipIDRespDE"));

        TgCamFE fe = new TgCamFE();
        fe.setiIndPres(TiIndPres.OTRO);
        fe.setdDesIndPres(LIBRE);
        SOAPElement p2 = padre();
        fe.setupSOAPElements(p2, TiTiOpe.B2C);
        Assertions.assertEquals(LIBRE, texto(p2, "dDesIndPres"));

        TgPaConEIni pago = new TgPaConEIni();
        pago.setiTiPago(TiTiPago.OTRO);
        pago.setdDesTiPag(LIBRE);
        pago.setdMonTiPag(BigDecimal.TEN);
        pago.setcMoneTiPag(CMondT.PYG);
        SOAPElement p3 = padre();
        pago.setupSOAPElements(p3);
        Assertions.assertEquals(LIBRE, texto(p3, "dDesTiPag"), "antes se ignoraba");

        TgPagTarCD tarjeta = new TgPagTarCD();
        tarjeta.setiDenTarj(TiDenTarj.OTRO);
        tarjeta.setdDesDenTarj(LIBRE);
        tarjeta.setiForProPa(TiForProPa.POS);
        SOAPElement p4 = padre();
        tarjeta.setupSOAPElements(p4);
        Assertions.assertEquals(LIBRE, texto(p4, "dDesDenTarj"), "antes se ignoraba");

        TgVehNuevo veh = new TgVehNuevo();
        veh.setiTipOpVN(TiTipOpVN.VENTA_A_REPRESENTANTE);
        veh.setiTipCom(TiTipCom.OTRO);
        veh.setdDesTipCom(LIBRE);
        SOAPElement p5 = padre();
        veh.setupSOAPElements(p5);
        Assertions.assertEquals(LIBRE, texto(p5, "dDesTipCom"));

        TgCamCarg carga = new TgCamCarg();
        carga.setiCarCarga(TiCarCarga.OTRO);
        carga.setdDesCarCarga(LIBRE);
        SOAPElement p6 = padre();
        carga.setupSOAPElements(p6);
        Assertions.assertEquals(LIBRE, texto(p6, "dDesCarCarga"));

        // con un código que no es «Otro», la descripción del código
        pago.setiTiPago(TiTiPago.values()[0]);
        SOAPElement p7 = padre();
        pago.setupSOAPElements(p7);
        Assertions.assertEquals(TiTiPago.values()[0].getDescripcion(), texto(p7, "dDesTiPag"));
    }
}
