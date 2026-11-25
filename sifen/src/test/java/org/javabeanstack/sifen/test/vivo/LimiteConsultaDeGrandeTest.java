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
package org.javabeanstack.sifen.test.vivo;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import jakarta.xml.soap.SOAPMessage;
import org.javabeanstack.sifen.ISifenClient;
import org.javabeanstack.sifen.SifenClient;
import org.javabeanstack.sifen.api.DEQueryResult;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.internal.helpers.SoapHelper;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.javabeanstack.sifen.simulator.Scenario;
import org.javabeanstack.sifen.simulator.SifenSimulator;
import org.javabeanstack.sifen.simulator.SimulatedService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * M5-07 (plan JBSSIF, §8.7): mide, contra el simulador y sin red, cuántos ítems admite {@code queryDE} antes de que
 * el JDK 25 corte la lectura por el límite de entidades ({@code jdk.xml.maxGeneralEntitySizeLimit} y afines), con la
 * respuesta 0422 en los dos formatos: el {@code rDE} <b>escapado</b> dentro de {@code xContenDE} (lo que graba el
 * simulador) y el <b>en línea</b> (lo que devuelve SIFEN-test en todas las 0422 guardadas).
 *
 * <p>El DE es el de {@code query-de-found.xml} (DE-01 firmado) con su primer {@code gCamItem} repetido: los totales
 * y la firma no cierran, pero {@code queryDE} no los verifica; lo que se mide es la lectura. Uso:
 * {@code mvn -pl sifen -am test -Psifen-live -Dtest=LimiteConsultaDeGrandeTest -Djbs.sifen.test.m507=<carpeta>}.</p>
 */
@Tag("sifen-live")
public class LimiteConsultaDeGrandeTest {

    private static final String SOAP = "application/soap+xml; charset=utf-8";
    private static final String RECURSO = "/org/javabeanstack/sifen/simulator/responses/query-de-found.xml";

    private String prefijo;
    private String sufijo;
    private String rde;
    private String item;
    private String cdc;
    private final List<String> lineas = new ArrayList<>();

    @Test
    public void medir() throws Exception {
        String salida = System.getProperty("jbs.sifen.test.m507");
        Assumptions.assumeTrue(salida != null, "sin medición de M5-07");
        String t;
        try (InputStream in = getClass().getResourceAsStream(RECURSO)) {
            t = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        int i = t.indexOf("<ns2:xContenDE>") + "<ns2:xContenDE>".length();
        int j = t.indexOf("</ns2:xContenDE>");
        prefijo = t.substring(0, i);
        sufijo = t.substring(j);
        rde = desescapar(t.substring(i, j));
        int a = rde.indexOf("<gCamItem>");
        item = rde.substring(a, rde.indexOf("</gCamItem>") + "</gCamItem>".length());
        int d = rde.indexOf("<DE Id=\"") + "<DE Id=\"".length();
        cdc = rde.substring(d, d + 44);
        lineas.add("ítem base: " + item.length() + " caracteres, " + contar(item, "<") + " etiquetas");
        lineas.add("formato\tn ítems\tbytes de la respuesta\treferencias de entidad\tresultado");

        try (SifenSimulator sim = SifenSimulator.start(); ISifenClient client = SifenClient.create(sim.config())) {
            // 1) escapado: búsqueda del máximo
            int ok = 1;
            int falla = -1;
            for (int n = 100; n <= 20000; n *= 2) {
                if (probar(sim, client, n, true, false)) {
                    ok = n;
                } else {
                    falla = n;
                    break;
                }
            }
            while (falla > 0 && falla - ok > 1) {
                int m = (ok + falla) / 2;
                if (probar(sim, client, m, true, false)) {
                    ok = m;
                } else {
                    falla = m;
                }
            }
            lineas.add("MÁXIMO escapado: " + ok + " ítems pasan, " + falla + " fallan");
            // 2) en línea: tamaños grandes
            for (int n : new int[] {ok, falla, 2000, 5000}) {
                if (n > 0) {
                    probar(sim, client, n, false, false);
                }
            }
            // 3) en línea con descripciones llenas de '&' (120 por ítem, el peor caso de un DE real)
            int okAmp = 1;
            int fallaAmp = -1;
            for (int n = 100; n <= 20000; n *= 2) {
                if (probar(sim, client, n, false, true)) {
                    okAmp = n;
                } else {
                    fallaAmp = n;
                    break;
                }
            }
            while (fallaAmp > 0 && fallaAmp - okAmp > 1) {
                int m = (okAmp + fallaAmp) / 2;
                if (probar(sim, client, m, false, true)) {
                    okAmp = m;
                } else {
                    fallaAmp = m;
                }
            }
            lineas.add("MÁXIMO en línea con 120 '&' por ítem: " + okAmp + " pasan, " + fallaAmp + " fallan");
        }
        // 4) la relectura desescapada de ResponseUtil con una 0422 en línea (el &amp; del dCarQR)
        String enLinea = prefijo + rde + sufijo;
        SOAPMessage m = SoapHelper.parseSoapMessage(new ByteArrayInputStream(enLinea.getBytes(StandardCharsets.UTF_8)));
        SOAPMessage r = ResponseUtil.parseSoapMessage(m);
        lineas.add("ResponseUtil.parseSoapMessage con la 0422 en línea: " + (r == m
                ? "no pudo releer la versión desescapada y siguió con la original (JBS-RESP-03 en el log)"
                : "releyó la versión desescapada"));
        Path out = Paths.get(salida);
        Files.createDirectories(out);
        Files.write(out.resolve("m5_07_medicion.txt"), lineas, StandardCharsets.UTF_8);
        lineas.forEach(System.out::println);
    }

    private boolean probar(SifenSimulator sim, ISifenClient client, int n, boolean escapado, boolean ampersand) throws Exception {
        String it = ampersand ? item.replaceFirst("<dDesProSer>[^<]*</dDesProSer>", "<dDesProSer>" + "&amp;".repeat(120) + "</dDesProSer>") : item;
        StringBuilder items = new StringBuilder(it.length() * n);
        for (int k = 0; k < n; k++) {
            items.append(it);
        }
        // el DE-01 trae 2 ítems: se reemplazan los dos por los n
        int a = rde.indexOf("<gCamItem>");
        int b = rde.lastIndexOf("</gCamItem>") + "</gCamItem>".length();
        String grande = rde.substring(0, a) + items + rde.substring(b);
        String cuerpo = prefijo + (escapado ? escapar(grande) : grande) + sufijo;
        int refs = contar(cuerpo, "&");
        sim.on(SimulatedService.QUERY_DE, Scenario.body(200, SOAP, cuerpo));
        String resultado;
        boolean bien;
        try {
            DEQueryResult q = client.queryDE(cdc);
            int leidos = q.getXml() == null ? -1 : contar(q.getXml(), "<gCamItem>");
            bien = "0422".equals(q.getResponseCode()) && leidos == n;
            resultado = q.getResponseCode() + " · ítems leídos " + leidos;
        } catch (SifenException x) {
            bien = false;
            resultado = x.getCode() + " · " + x.getTechnicalDetail();
        }
        lineas.add((escapado ? "escapado" : ampersand ? "en línea +&" : "en línea") + "\t" + n + "\t"
                + cuerpo.getBytes(StandardCharsets.UTF_8).length + "\t" + refs + "\t" + resultado);
        return bien;
    }

    private static String desescapar(String s) {
        return s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&amp;", "&");
    }

    private static String escapar(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static int contar(String s, String sub) {
        int n = 0;
        for (int i = s.indexOf(sub); i >= 0; i = s.indexOf(sub, i + sub.length())) {
            n++;
        }
        return n;
    }
}
