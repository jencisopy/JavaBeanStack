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
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.fields.request.de.TgEmis;
import org.javabeanstack.sifen.internal.response.SifenObjectFactory;
import org.javabeanstack.sifen.test.SifenTestEnv;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URL;

/**
 * Base de los tests que pegan al ambiente de test de SIFEN.
 *
 * <p>Plan JBSSIF, §1.8: la configuración ya no sale de un {@code test.properties} del classpath
 * sino de {@link SifenTestEnv}; si falta, {@code @BeforeEach} (antes {@code @BeforeClass}) deja
 * cada test saltado, no fallado (con {@code @BeforeAll} surefire no los cuenta como saltados).</p>
 *
 * <p>§2.7 (M1-03): ya no se loguea la configuración (llevaba la clave y el CSC al log de surefire).</p>
 */
public class DETestBase {

    protected static final Logger LOGGER = LogManager.getLogger(DETestBase.class);

    protected static final DocumentBuilderFactory _documentBuilderFactory = DocumentBuilderFactory.newInstance();
    protected static DocumentBuilder _documentBuilder;

    static {
        _documentBuilderFactory.setNamespaceAware(true);
        try {
            _documentBuilder = _documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            System.err.println("Error al crear el DocumentBuilder");
        }
    }


    @BeforeEach
    public void setupSifenConfig() throws SifenException {
        SifenConfig sifenConfig = SifenTestEnv.configuracionOSaltar();
        // M1-03 (plan JBSSIF, §2.7): nunca loguear la configuración; su toString() (port literal,
        // D10) incluye la clave del certificado y el CSC. Solo el ambiente, que no es secreto.
        LOGGER.info("Ambiente de SIFEN de los tests en vivo: " + sifenConfig.getEnvironment());
        Sifen.setSifenConfig(sifenConfig);
    }

    protected TgEmis cargarEmisor(String ruc) throws IOException, SAXException, SifenException {
        InputStream resource = getClass().getClassLoader().getResourceAsStream("tests/emisores/" + ruc + ".xml");
        Document document = _documentBuilder.parse(resource);
        NodeList gEmis = document.getElementsByTagName("gEmis");
        Assertions.assertEquals(1, gEmis.getLength());
        Node node = gEmis.item(0);
        Assertions.assertNotNull(node);
        TgEmis tgEmis = SifenObjectFactory.getFromNode(node, TgEmis.class);
        return tgEmis;
    }

    @Test
    @Disabled("Ignorado en el repositorio original (rshk-jsifenlib); se revisa en la Fase 2")
    public void testEmisorDelArchivo() throws Exception {
        TgEmis tgEmis = cargarEmisor("80111111-0");
        Assertions.assertNotNull(tgEmis);
    }



}
