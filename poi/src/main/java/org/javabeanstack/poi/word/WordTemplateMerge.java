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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;

/**
 * Reemplaza los marcadores <code>&lt;&lt;campo&gt;&gt;</code> de una plantilla
 * Word (.docx) por los valores de un mapa.
 *
 * <p>
 * El reemplazo se resuelve a nivel de <b>párrafo</b>, no de <i>run</i>. Word
 * suele partir un marcador en varios runs (por revisiones, corrector
 * ortográfico o simples cambios de formato), de modo que un run aislado
 * contiene apenas <code>'&lt;&lt;'</code>, otro el nombre del campo y otro
 * <code>'&gt;&gt;'</code>. Buscar el marcador dentro de cada run —como se hacía
 * antes— dejaba esos casos sin reemplazar y el documento salía con el marcador
 * impreso literalmente.
 * </p>
 *
 * <p>
 * El texto del párrafo se reconstruye concatenando los runs, se reemplazan los
 * marcadores sobre esa cadena y el resultado se reparte de nuevo entre los runs
 * originales: cada carácter literal vuelve al run del que salió y el valor de
 * cada marcador se escribe en el run donde ese marcador empezaba. Así se
 * conserva el formato de cada tramo del párrafo y se admite que un mismo run
 * sea el cierre de un marcador y la apertura del siguiente (caso real de la
 * plantilla de rescisión: <code>'&gt;&gt; de &lt;&lt;'</code>).
 * </p>
 *
 * <p>
 * Los campos ausentes del mapa (o con valor vacío) se reemplazan por
 * {@link #SIN_VALOR}, manteniendo el comportamiento histórico.
 * </p>
 *
 * @author Jorge Enciso
 */
public class WordTemplateMerge {

    /** Marca que se imprime cuando el campo no tiene valor. */
    public static final String SIN_VALOR = "----------";

    /** Marcador de plantilla: &lt;&lt;campo&gt;&gt;. */
    private static final Pattern MARCADOR = Pattern.compile("<<([^<>]*)>>");

    private WordTemplateMerge() {
    }

    /**
     * Reemplaza los marcadores en todo el documento: cuerpo, tablas y tablas
     * anidadas dentro de las celdas.
     *
     * @param doc documento abierto sobre la plantilla.
     * @param data valores a mergear, indexados por nombre de campo.
     */
    public static void merge(XWPFDocument doc, Map<String, String> data) {
        for (XWPFParagraph parrafo : doc.getParagraphs()) {
            mergeParagraph(parrafo, data);
        }
        mergeTables(doc.getTables(), data);
    }

    /**
     * Reemplaza los marcadores en una lista de tablas, recorriendo también las
     * tablas anidadas en sus celdas.
     *
     * @param tables tablas a procesar.
     * @param data valores a mergear.
     */
    private static void mergeTables(List<XWPFTable> tables, Map<String, String> data) {
        for (XWPFTable table : tables) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph parrafo : cell.getParagraphs()) {
                        mergeParagraph(parrafo, data);
                    }
                    mergeTables(cell.getTables(), data);
                }
            }
        }
    }

    /**
     * Reemplaza los marcadores de un párrafo repartiendo el resultado entre sus
     * runs. Solo se reescriben los runs cuyo texto cambia, para no alterar el
     * resto del párrafo.
     *
     * @param parrafo párrafo a procesar.
     * @param data valores a mergear.
     */
    private static void mergeParagraph(XWPFParagraph parrafo, Map<String, String> data) {
        List<XWPFRun> runs = parrafo.getRuns();
        if (runs == null || runs.isEmpty()) {
            return;
        }
        //Texto de cada run (concatenación de sus nodos w:t) y texto del párrafo.
        List<String> textos = new ArrayList();
        StringBuilder parrafoTexto = new StringBuilder();
        for (XWPFRun run : runs) {
            String texto = getRunText(run);
            textos.add(texto);
            parrafoTexto.append(texto);
        }
        String texto = parrafoTexto.toString();
        if (!texto.contains("<<") || !texto.contains(">>")) {
            return;
        }
        //Un acumulador por run; el texto literal vuelve a su run de origen y el
        //valor del marcador se escribe en el run donde el marcador comienza.
        StringBuilder[] nuevos = new StringBuilder[runs.size()];
        for (int i = 0; i < nuevos.length; i++) {
            nuevos[i] = new StringBuilder();
        }
        int posicion = 0;
        Matcher matcher = MARCADOR.matcher(texto);
        while (matcher.find()) {
            copyLiteral(texto, posicion, matcher.start(), textos, nuevos);
            nuevos[getRunIndex(matcher.start(), textos)].append(getValue(matcher.group(1), data));
            posicion = matcher.end();
        }
        if (posicion == 0) {
            //No había ningún marcador completo, no se toca el párrafo.
            return;
        }
        copyLiteral(texto, posicion, texto.length(), textos, nuevos);
        for (int i = 0; i < runs.size(); i++) {
            String nuevo = nuevos[i].toString();
            if (!nuevo.equals(textos.get(i))) {
                setRunText(runs.get(i), nuevo);
            }
        }
    }

    /**
     * Copia el tramo de texto sin marcadores comprendido entre dos posiciones,
     * devolviendo cada carácter al run del que proviene.
     *
     * @param texto texto completo del párrafo.
     * @param desde posición inicial (inclusive).
     * @param hasta posición final (exclusive).
     * @param textos texto original de cada run.
     * @param nuevos acumuladores por run.
     */
    private static void copyLiteral(String texto, int desde, int hasta, List<String> textos, StringBuilder[] nuevos) {
        int inicio = desde;
        while (inicio < hasta) {
            int indice = getRunIndex(inicio, textos);
            int fin = Math.min(hasta, getRunEnd(indice, textos));
            nuevos[indice].append(texto, inicio, fin);
            inicio = fin;
        }
    }

    /**
     * Devuelve el índice del run al que pertenece una posición del texto del
     * párrafo. Los runs vacíos no son propietarios de ninguna posición.
     *
     * @param posicion posición dentro del texto del párrafo.
     * @param textos texto original de cada run.
     * @return índice del run; el último run si la posición excede el texto.
     */
    private static int getRunIndex(int posicion, List<String> textos) {
        int offset = 0;
        for (int i = 0; i < textos.size(); i++) {
            offset += textos.get(i).length();
            if (posicion < offset) {
                return i;
            }
        }
        return textos.size() - 1;
    }

    /**
     * Devuelve la posición final (exclusive) del run indicado dentro del texto
     * del párrafo.
     *
     * @param indice índice del run.
     * @param textos texto original de cada run.
     * @return posición final del run.
     */
    private static int getRunEnd(int indice, List<String> textos) {
        int offset = 0;
        for (int i = 0; i <= indice; i++) {
            offset += textos.get(i).length();
        }
        return offset;
    }

    /**
     * Devuelve el valor a imprimir para un campo, o {@link #SIN_VALOR} si no
     * está en el mapa o está vacío.
     *
     * @param campo nombre del campo, sin los delimitadores.
     * @param data valores a mergear.
     * @return valor a imprimir.
     */
    private static String getValue(String campo, Map<String, String> data) {
        String valor = data.get(campo);
        if (valor == null || valor.isEmpty()) {
            return SIN_VALOR;
        }
        return valor;
    }

    /**
     * Devuelve el texto de un run concatenando todos sus nodos w:t. No se usa
     * {@link XWPFRun#text()} porque agrega tabulaciones y saltos de línea que
     * desalinearían las posiciones respecto del texto que luego se escribe.
     *
     * @param run run a leer.
     * @return texto del run.
     */
    private static String getRunText(XWPFRun run) {
        StringBuilder texto = new StringBuilder();
        CTR ctr = run.getCTR();
        for (int i = 0; i < ctr.sizeOfTArray(); i++) {
            CTText nodo = ctr.getTArray(i);
            if (nodo.getStringValue() != null) {
                texto.append(nodo.getStringValue());
            }
        }
        return texto.toString();
    }

    /**
     * Escribe el texto de un run dejándolo en un único nodo w:t (elimina los
     * nodos sobrantes para no duplicar el contenido).
     *
     * @param run run a escribir.
     * @param texto texto a dejar en el run.
     */
    private static void setRunText(XWPFRun run, String texto) {
        CTR ctr = run.getCTR();
        for (int i = ctr.sizeOfTArray() - 1; i > 0; i--) {
            ctr.removeT(i);
        }
        run.setText(texto, 0);
    }
}
