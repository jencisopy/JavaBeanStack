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
package org.javabeanstack.sifen.internal;

import org.javabeanstack.sifen.SifenClient;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * La versión de la librería sale del build (plan JBSSIF, M1-01): la del recurso filtrado es la del {@code pom.xml}
 * del módulo, {@code ISifenClient.getVersion()} y el {@code User-Agent} la usan, y ningún fuente de
 * {@code src/main} la escribe a mano.
 */
public class VersionLibreriaTest {

    /** {@code project/version} del pom del módulo o, si no la declara, la del padre. */
    private static String versionDelPom() throws Exception {
        Document d = org.javabeanstack.sifen.internal.util.SifenUtil.newSecureDocumentBuilder().parse(Paths.get("pom.xml").toFile());
        String propia = null;
        String padre = null;
        for (Node n = d.getDocumentElement().getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && "version".equals(e.getLocalName())) {
                propia = e.getTextContent().trim();
            } else if (n instanceof Element e && "parent".equals(e.getLocalName())) {
                for (Node m = e.getFirstChild(); m != null; m = m.getNextSibling()) {
                    if (m instanceof Element v && "version".equals(v.getLocalName())) {
                        padre = v.getTextContent().trim();
                    }
                }
            }
        }
        return propia != null ? propia : padre;
    }

    @Test
    public void laVersionEsLaDelPom() throws Exception {
        String pom = versionDelPom();
        Assertions.assertNotNull(pom);
        Assertions.assertEquals(pom, Constants.SDK_CURRENT_VERSION, "recurso filtrado jbs-sifen.properties");
        Assertions.assertNotEquals(Constants.UNKNOWN_VERSION, Constants.SDK_CURRENT_VERSION);
        Assertions.assertEquals(pom, SifenClient.create(FixturesSifen.configuracion()).getVersion(), "ISifenClient.getVersion()");
        Assertions.assertEquals("jbs-sifen/" + pom, FixturesSifen.configuracion().getUserAgent());
    }

    /** Ningún fuente de src/main lleva la versión escrita (el número del pom entre comillas). */
    @Test
    public void ningunFuenteEscribeLaVersion() throws Exception {
        String pom = versionDelPom();
        Pattern literal = Pattern.compile("\"" + Pattern.quote(pom) + "\"");
        try (Stream<Path> s = Files.walk(Paths.get("src", "main"))) {
            for (Path f : s.filter(Files::isRegularFile).toList()) {
                String texto = Files.readString(f);
                Assertions.assertFalse(literal.matcher(texto).find(), f + " escribe la versión " + pom + " a mano (M1-01)");
            }
        }
    }
}
