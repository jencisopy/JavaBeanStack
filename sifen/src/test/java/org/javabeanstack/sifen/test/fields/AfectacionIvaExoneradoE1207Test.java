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
package org.javabeanstack.sifen.test.fields;

import java.util.Arrays;
import java.util.List;
import org.javabeanstack.sifen.ISifenClient;
import org.javabeanstack.sifen.SifenClient;
import org.javabeanstack.sifen.api.SignedDocument;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.types.TiAfecIVA;
import org.javabeanstack.sifen.internal.ctx.GenerationCtx;
import org.javabeanstack.sifen.test.casos.CasosDePrueba;
import org.javabeanstack.sifen.test.casos.FixturesSifen;
import org.javabeanstack.sifen.test.casos.XmlCanonico;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * E12-07 (plan JBSSIF, Fase 12; decisión del usuario del 2026-11-25, ESC-43): el texto de {@code dDesAfecIVA} para el
 * ítem exonerado es el del XSD vigente de la DNIT, «Exonerado (Art. 100 - Ley 6380/2019)» ({@code tdDesAfecIVA} de
 * {@code DE_Types_v150.xsd}, descargado el 2026-11-25). Hasta la corrección la librería escribía el del XSD de 2019,
 * «Exonerado (Art. 83- Ley 125/91)», y SIFEN-test rechazaba con 0160 todo DE con un ítem exonerado (DE-12, 0001100).
 *
 * <p>Compatibilidad al releer: la lectura del modelo toma {@code iAfecIVA} por código e ignora {@code dDesAfecIVA}, así
 * que un XML con el texto viejo (un DE emitido antes de la corrección) se lee como {@link TiAfecIVA#EXONERADO}; si se
 * vuelve a generar sale con el texto vigente. {@code readSigned} no regenera: conserva el XML firmado tal cual.</p>
 */
public class AfectacionIvaExoneradoE1207Test {
    /** Texto vigente (XSD de la DNIT del 2026-11-25, {@code tdDesAfecIVA}). */
    static final String VIGENTE = "Exonerado (Art. 100 - Ley 6380/2019)";
    /** Texto del XSD de 2019, que SIFEN-test ya no acepta. */
    static final String VIEJO = "Exonerado (Art. 83- Ley 125/91)";

    private static SifenConfig cfg;
    private static GenerationCtx ctx;

    @BeforeAll
    public static void configurar() {
        cfg = FixturesSifen.configuracion();
        ctx = FixturesSifen.contexto(cfg);
    }

    /** Las cuatro descripciones del enum son, en orden, la enumeración vigente de {@code tdDesAfecIVA}. */
    @Test
    public void lasDescripcionesSonLasDelXsdVigente() {
        Assertions.assertEquals(VIGENTE, TiAfecIVA.EXONERADO.getDescripcion());
        Assertions.assertEquals(List.of("Gravado IVA", VIGENTE, "Exento", "Gravado parcial (Grav- Exento)"),
                Arrays.stream(TiAfecIVA.values()).map(TiAfecIVA::getDescripcion).toList());
        Assertions.assertEquals(TiAfecIVA.EXONERADO, TiAfecIVA.getByVal((short) 2), "el código no cambia");
    }

    /** DE-12 (factura con un ítem exonerado), armado por objeto, escribe el texto vigente y no el viejo. */
    @Test
    public void elDe12EscribeElTextoVigente() throws Exception {
        String xml = CasosDePrueba.documentos().stream().filter(k -> k.getId().equals("DE-12")).findFirst().orElseThrow()
                .construir().generarXml(ctx, cfg);
        Assertions.assertEquals("2", XmlCanonico.texto(xml, "iAfecIVA"));
        Assertions.assertEquals(VIGENTE, XmlCanonico.texto(xml, "dDesAfecIVA"));
        Assertions.assertFalse(xml.contains("Ley 125/91"), "E12-07: el texto del XSD de 2019 no se escribe");
    }

    /**
     * La entrada de DE-12 ({@code casos/factura-exonerado.xml}, salida del fork) trae el texto viejo: se lee como
     * exonerado y, regenerada, sale con el vigente (la compatibilidad con un DE emitido antes de la corrección).
     */
    @Test
    public void unXmlConElTextoViejoSeLeeComoExoneradoYSeRegeneraConElVigente() throws Exception {
        String entrada = FixturesSifen.recurso("casos/factura-exonerado.xml");
        Assertions.assertTrue(entrada.contains(VIEJO), "precondición: la entrada conserva el texto viejo");
        DocumentoElectronico leido = new DocumentoElectronico(entrada);
        Assertions.assertEquals(TiAfecIVA.EXONERADO, leido.getgDtipDE().getgCamItemList().get(0).getgCamIVA().getiAfecIVA());
        String xml = leido.generarXml(ctx, cfg);
        Assertions.assertEquals(VIGENTE, XmlCanonico.texto(xml, "dDesAfecIVA"));
    }

    /**
     * {@code readSigned} sobre un XML firmado con el texto viejo no lo reescribe: lo devuelve con el texto con el que se
     * firmó, y el modelo leído es exonerado. (El XML viejo se arma reemplazando el texto en uno firmado ahora; readSigned
     * no verifica la firma criptográficamente.)
     */
    @Test
    public void readSignedConservaElTextoConElQueSeFirmo() throws Exception {
        String firmado = CasosDePrueba.documentos().stream().filter(k -> k.getId().equals("DE-12")).findFirst().orElseThrow()
                .construir().generarXml(ctx, cfg);
        // sin assertEquals sobre el XML: un fallo no debe volcar el XML firmado en el log
        Assertions.assertTrue(firmado.contains(VIGENTE), "precondición: el XML firmado trae el texto vigente");
        String viejo = firmado.replace(VIGENTE, VIEJO);
        try (ISifenClient c = SifenClient.create(cfg)) {
            SignedDocument doc = c.readSigned(viejo);
            Assertions.assertEquals(VIEJO, XmlCanonico.texto(doc.getXml(), "dDesAfecIVA"), "readSigned no regenera");
            Assertions.assertEquals(TiAfecIVA.EXONERADO,
                    new DocumentoElectronico(doc.getXml()).getgDtipDE().getgCamItemList().get(0).getgCamIVA().getiAfecIVA());
        }
    }
}
