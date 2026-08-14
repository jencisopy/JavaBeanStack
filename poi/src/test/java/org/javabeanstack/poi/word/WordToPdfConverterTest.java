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
package org.javabeanstack.poi.word;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests del conversor Word a PDF: un documento con marcadores mergeados
 * convertido en memoria debe producir un PDF válido.
 *
 * @author Jorge Enciso
 */
public class WordToPdfConverterTest {

    /**
     * Arma un docx en memoria con un marcador, lo mergea y lo convierte:
     * el resultado debe arrancar con la firma %PDF-.
     */
    @Test
    public void testMergeYConversion() throws Exception {
        byte[] docx;
        try (XWPFDocument doc = new XWPFDocument()) {
            //Los .docx reales de Word siempre traen la parte de estilos; uno
            //creado desde cero con POI no, y el conversor la exige.
            doc.createStyles();
            //Sección con tamaño de página A4: un .docx real de Word siempre
            //la trae; el conversor la exige.
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr sectPr =
                    doc.getDocument().getBody().addNewSectPr();
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz pageSz =
                    sectPr.addNewPgSz();
            pageSz.setW(java.math.BigInteger.valueOf(11906));
            pageSz.setH(java.math.BigInteger.valueOf(16838));
            XWPFParagraph p = doc.createParagraph();
            p.createRun().setText("Contrato de <<cliente>> por <<monto>> guaranies.");
            Map<String, String> data = new HashMap();
            data.put("cliente", "PRUEBA S.A.");
            data.put("monto", "1.500.000");
            WordTemplateMerge.merge(doc, data);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            doc.write(buffer);
            docx = buffer.toByteArray();
        }
        byte[] pdf = WordToPdfConverter.convert(docx);
        assertTrue(pdf.length > 500, "el pdf debe tener contenido");
        assertArrayEquals("%PDF-".getBytes(), java.util.Arrays.copyOf(pdf, 5),
                "debe arrancar con la firma PDF");
    }
}
