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
package org.javabeanstack.sifen.internal.http;

import com.sun.net.httpserver.HttpsServer;
import jakarta.xml.soap.SOAPMessage;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.namespace.QName;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.internal.Constants;
import org.javabeanstack.sifen.internal.helpers.SoapHelper;
import org.javabeanstack.sifen.test.casos.FixturesSifen;

/**
 * Programa que {@link ProxyConUsuarioHttpsTest} corre en una JVM aparte, arrancada con
 * {@code -Djdk.http.auth.tunneling.disabledSchemes=} (la propiedad que el JDK lee al arrancar y que un
 * proxy {@code Basic} con destino {@code https://} necesita, I5-04). Hace un pedido con la clave correcta
 * y otro con una incorrecta contra un proxy local que exige {@code Basic}, y escribe una línea por caso:
 * {@code <caso> resultado=<200|código> connects=<n> detalle=<texto>}. Sin red: todo en {@code 127.0.0.1}.
 */
public final class TunelConUsuarioMain {

    private TunelConUsuarioMain() {
    }

    /**
     * Corre los dos casos.
     *
     * @param args sin uso.
     * @throws Exception si el armado falla.
     */
    public static void main(String[] args) throws Exception {
        ServidoresFase5.confiarEnElAutofirmado();
        AtomicInteger pedidos = new AtomicInteger();
        HttpsServer s = ServidoresFase5.https(0, ServidoresFase5.responde("consulta-ruc-0502.xml", pedidos));
        try {
            caso("correcta", "clave", s, pedidos);
            caso("incorrecta", "otra", s, pedidos);
        } finally {
            s.stop(0);
        }
        System.exit(0);
    }

    private static void caso(String nombre, String clave, HttpsServer s, AtomicInteger pedidos) throws Exception {
        try (ServidoresFase5.ProxyLocal proxy = new ServidoresFase5.ProxyLocal("usuario-proxy", "clave")) {
            SifenConfig cfg = FixturesSifen.configuracion();
            cfg.setProxyHost("127.0.0.1");
            cfg.setProxyPort(proxy.puerto());
            cfg.setProxyUser("usuario-proxy");
            cfg.setProxyPassword(clave);
            SOAPMessage m = SoapHelper.createSoapMessage();
            m.getSOAPBody().addBodyElement(new QName(Constants.SIFEN_NS_URI, "rEnviConsRUC"))
                    .addChildElement("dRUCCons").setTextContent("80123456");
            String url = "https://127.0.0.1:" + s.getAddress().getPort() + "/de/ws/consultas/consulta-ruc.wsdl";
            String resultado;
            String detalle = "";
            try {
                resultado = String.valueOf(SifenConnection.create().exchange(cfg, url, m).getStatus());
            } catch (SifenException e) {
                resultado = e.getCode();
                detalle = String.valueOf(e.getTechnicalDetail()).replace('\n', ' ');
            }
            System.out.println(nombre + " resultado=" + resultado + " connects=" + proxy.connects() + " pedidos="
                    + pedidos.get() + " detalle=" + detalle);
        }
    }
}
