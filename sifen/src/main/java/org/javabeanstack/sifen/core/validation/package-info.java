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
/**
 * Validación previa del Documento Electrónico: comprueba el DE completo <b>antes</b> de armar el XML
 * y de firmar, y devuelve todos los incumplimientos juntos, con el código de la regla, la ruta del
 * campo según el Manual Técnico y un mensaje en español (plan JBSSIF, Fase 3).
 *
 * <h2>Piezas</h2>
 * <ul>
 * <li>{@link org.javabeanstack.sifen.core.validation.DEValidator} y
 * {@link org.javabeanstack.sifen.core.validation.EventValidator}: puntos de entrada; con su
 * {@code Builder} se registran conjuntos, se desactivan reglas por código y se fija la fecha de reglas.</li>
 * <li>{@link org.javabeanstack.sifen.core.validation.RuleSet}: un subgrupo del MT con sus reglas;
 * cada regla es una <b>ficha</b> ({@link org.javabeanstack.sifen.core.validation.ValidationRule}) y un
 * <b>chequeo</b> ({@link org.javabeanstack.sifen.core.validation.Check}) declarados juntos (D33). Los
 * del catálogo están en {@code rules.de} y {@code rules.event}; la única lista a mano es
 * {@code DERuleSets.STANDARD} / {@code EventRuleSets.STANDARD}.</li>
 * <li>{@link org.javabeanstack.sifen.core.validation.RuleCatalog}: el catálogo en código, armado desde
 * esas listas; exporta el TSV del que se deriva el catálogo {@code .md}.</li>
 * <li>{@link org.javabeanstack.sifen.core.validation.RuleSource},
 * {@link org.javabeanstack.sifen.core.validation.EffectivePeriod},
 * {@link org.javabeanstack.sifen.core.validation.NotEvaluatedReason}: de dónde sale una regla, desde y hasta
 * cuándo rige y por qué no se evalúa.</li>
 * <li>{@link org.javabeanstack.sifen.core.validation.ValidationMessages} y el recurso
 * {@code validation_messages.properties}: <b>todos</b> los textos, en un solo lugar.</li>
 * <li>{@code support}: {@code ValueChecks} (predicados null-safe), {@code DEData} (variables de condición
 * del catálogo) y {@code Legends}; públicos para escribir reglas.</li>
 * <li>{@link org.javabeanstack.sifen.core.validation.ValidationError},
 * {@link org.javabeanstack.sifen.core.validation.ValidationResult} y
 * {@link org.javabeanstack.sifen.core.validation.SifenValidationException}: la salida, inmutable.</li>
 * </ul>
 *
 * <h2>Cómo es una regla (D33)</h2>
 * <pre>{@code
 * declare(rule("JBS-D-08").field("dRucEm").error().omitValue(),
 *         passes(s -> !present(s.e().getdRucEm()) || ValueChecks.length(s.e().getdRucEm(), 3, 8), s -> s.e().getdRucEm()));
 * declare(rule("D017").field("dCondTiCam").error().dCodRes("1207"),
 *         requiredIf(s -> !DEData.inGuaranies(s.root()), TgOpeCom::getdCondTiCam));
 * declareNotEvaluated(rule("D016").field("dDesMoneOpe").error().dCodRes("1206"), NotEvaluatedReason.DERIVED);
 * }</pre>
 * <p>La ficha va con el código, el campo (relativo a la base del conjunto), la severidad, el
 * {@code dCodRes}, la política del valor y, si corresponde, {@code source(RuleSource.NT_0xx)},
 * {@code from(fecha)} y {@code until(fecha, motivo)}. El chequeo devuelve {@code true} si se cumple; con
 * el elemento ausente cumple (la presencia es otra regla: {@code groupRequiredIf}).</p>
 *
 * <h2>Qué hacer ante una nota técnica nueva</h2>
 * <table>
 * <caption>Procedimiento (los demás pasos los reclama un test o el {@code .py} si faltan)</caption>
 * <tr><th>Caso</th><th>Pasos</th></tr>
 * <tr><td>Toda NT</td><td>Una constante en {@link org.javabeanstack.sifen.core.validation.RuleSource}
 * ({@code NT_028("NT 028", fecha)}).</td></tr>
 * <tr><td>Agregar una regla</td><td>(a) {@code declare(rule("JBS-E-192")…source(RuleSource.NT_028), …)} en el
 * conjunto de su subgrupo; (b) su clave en {@code validation_messages.properties}; (c) su negativo en
 * {@code negativos_de.tsv}; (d) {@code mvn -pl sifen test} y {@code sifen_catalogo_reglas.py --generar-md}
 * (la fila nueva sale con «(describir)» en el chequeo).</td></tr>
 * <tr><td>Modificar una regla</td><td>Cambiar el chequeo en su declaración y sumar la fuente; ajustar el
 * mensaje y el negativo si cambian; regenerar el {@code .md}. Mismo código.</td></tr>
 * <tr><td>Dar de baja una regla</td><td>{@code .until(RuleSource.NT_028, "motivo")} en la declaración
 * existente; nada más. No se borra ni se reasigna el código: el {@code .md} la muestra tachada.</td></tr>
 * <tr><td>Cambio de semántica con fecha de corte</td><td>Regla nueva con código nuevo,
 * {@code .from(fecha)} y {@code .replaces("X")}; la vieja con {@code .until(fecha, motivo)}.</td></tr>
 * </table>
 * <p>Sin recompilar: {@code sifen.validacion_reglas_desactivadas=D105,JBS-E-70} y
 * {@code sifen.validacion_fecha_reglas=2027-03-01} (o {@code Builder.disable}/{@code rulesDate}).</p>
 *
 * <h2>Decisión: reglas en código por grupo; identidad y mensajes como datos (D32, completada por D33)</h2>
 * <p>Se evaluaron dos formas (plan JBSSIF, §3.2):</p>
 * <ol>
 * <li><b>Tabla declarativa + motor</b>: cada regla como dato (campo, condición, formato, dominio) y
 * un motor genérico que la interpreta.</li>
 * <li><b>Reglas en código</b>: una clase {@code GroupValidator} por grupo del MT que navega el
 * modelo con sus getters tipados.</li>
 * </ol>
 * <p>Se elige un <b>híbrido</b>: la <b>lógica</b> va en código (opción 2) y la <b>identidad</b> de
 * cada regla y su <b>mensaje</b>, como datos ({@code ValidationRule} + {@code ValidationMessages}).
 * Criterio:</p>
 * <ul>
 * <li>Las condiciones del MT dependen de hasta nueve discriminantes ({@code iTiDE}, {@code iTipTra},
 * {@code iTImp}, {@code cMoneOpe}, {@code iNatRec}, {@code iTiOpe}, {@code iCondOpe},
 * {@code iIndPres}, {@code iTipEmi}), cruzan grupos y la coherencia es aritmética (suma de ítems contra
 * {@code gTotSub}, CDC contra {@code gTimb}). Una tabla declarativa obligaría a inventar un lenguaje
 * de expresiones sin tipos ni compilador, que nadie más mantiene; en código las condiciones compilan
 * contra el modelo, se depuran y las cubre JaCoCo (≥ 90 %, §3.4).</li>
 * <li>Lo que sí es dato —el ID estable de la fila del catálogo, la severidad, el {@code dCodRes} de
 * SIFEN, la ruta y el texto— queda declarado en un solo lugar y es verificable: cada regla tiene su
 * mensaje y no hay mensajes huérfanos ({@code ReglaValidacionTest}); cada grupo declara qué reglas
 * reporta ({@link org.javabeanstack.sifen.core.validation.GroupValidator#getRules()}), lo que
 * permite cruzar catálogo, código y tests negativos (CA3-03).</li>
 * <li>El modelo actual ({@code Tg*}) cambia en la Fase 7: las reglas en código migran con el
 * compilador marcando cada getter que cambió; una tabla con nombres de campo en texto se rompería en
 * silencio.</li>
 * </ul>
 *
 * <h2>Contrato de null-safety</h2>
 * <p>Un DE incompleto nunca termina en {@link NullPointerException} (RF7; prueba de mutación de §3.4,
 * que anula uno a uno los campos obligatorios). Dos capas:</p>
 * <ol>
 * <li><b>Por construcción</b>: cada grupo navega con
 * {@link org.javabeanstack.sifen.core.validation.ValidationContext#checkPresent} /
 * {@link org.javabeanstack.sifen.core.validation.ValidationContext#present}; el grupo padre ausente
 * se reporta una vez y sus hijos no se evalúan. El documento nulo da {@code JBS-VAL-01} y el
 * contexto de generación nulo, el aviso {@code JBS-VAL-02}.</li>
 * <li><b>Red del motor</b>: si una regla igual lanza una {@link RuntimeException}, el motor la
 * convierte en {@code JBS-VAL-00} (con el nombre del grupo y la clase de la excepción), deja la traza
 * a WARN <b>sin el mensaje</b> de la excepción y sigue con el grupo siguiente. La prueba de mutación
 * exige que {@code JBS-VAL-00} <b>no aparezca nunca</b>: así un descuido de una regla se ve en la
 * prueba sin romper la garantía de «no lanza» en producción.</li>
 * </ol>
 * <p><b>Trade-off aceptado</b> (D32, M3-04): un defecto de una regla sobre un DE válido bloquea la
 * emisión con {@code JBS-VAL-00}, un texto poco explicativo para el usuario; el diagnóstico está en la
 * traza del log y la salida de emergencia es {@code validacionPrevia=false} (§3.5).</p>
 *
 * <h2>Rutas verificadas y reglas declaradas</h2>
 * <ul>
 * <li>La ruta de un error se arma siempre desde la regla ({@code ctx.report(regla, i, j)} o
 * {@code regla.path(i, j)}); una ruta que, sin sus índices, no es la canónica de la regla se rechaza
 * con {@link IllegalArgumentException} (M3-03), para que ningún dato del documento viaje como
 * ruta.</li>
 * <li>Después de cada grupo, el motor comprueba que lo reportado esté en su {@code getRules()}
 * (M3-10): aviso en el log, o {@link IllegalStateException} con
 * {@code DEValidator.builder().strict()}, que es como deben correr las pruebas.</li>
 * <li>El catálogo de {@code ValidationRule} se cierra al terminar de cargar la clase (M3-08): no
 * cambia en tiempo de ejecución.</li>
 * </ul>
 *
 * <h2>Datos sensibles</h2>
 * <p>El valor rechazado se conserva solo si la regla lo permite
 * ({@link org.javabeanstack.sifen.core.validation.ValuePolicy}: por defecto nada; el largo para
 * campos con datos personales; el valor truncado solo para códigos, importes y fechas, y solo si es
 * un escalar: un bean o una colección se descartan aunque la regla diga {@code SHOW}). Ningún texto
 * que arme la librería —mensaje, {@code toString()}, {@code getMessage()} de la excepción, log—
 * incluye el valor.</p>
 *
 * <h2>Rutas y códigos</h2>
 * <ul>
 * <li>Ruta: grupos y campo del MT en notación de objeto, desde el elemento {@code DE} (que no se
 * nombra): {@code gDatGralOpe.gDatRec.dRucRec}; en las listas, índice <b>base 0</b>
 * ({@code gDtipDE.gCamItem[3].gValorItem.dPUniProSer} es el cuarto ítem), como las rutas de
 * propiedad de Bean Validation. {@code DE} sola designa el documento entero.</li>
 * <li>Código: el ID de la regla en el MT cuando existe ({@code D208c}); si la regla es de la
 * librería, {@code JBS-<grupo>-<nn>} con el nombre del grupo ({@code JBS-F-01},
 * {@code JBS-EVENTO-01}). Es el ID de la fila del catálogo —una fila por chequeo, no por campo— y
 * no se reasigna.</li>
 * </ul>
 *
 * <h2>Hallazgos que cubre</h2>
 * <p>M0-10 (no había validación previa); las guardas anti-NPE de E0-14 ({@code gCompPub} /
 * {@code dDncpG} en B2G), E0-16 ({@code dBasExe} sin NT 013: JBS-VAL-03, dada de baja en §6.3 porque desde la
 * Fase 6 {@code dBasExe} se escribe siempre, D25),
 * E0-19 (escala de {@code dBasExe}), E2-01 (autofactura sin {@code gCamDEAsoc}) y E2-03
 * ({@code dDescItem}/{@code dDescGloItem} nulos) son reglas de §3.3 sobre este contrato; E0-18
 * ({@code dDirRec} sin {@code cDisRec} en la nominación) es una regla de eventos con el mismo
 * {@link org.javabeanstack.sifen.core.validation.GroupValidator} sobre la raíz del evento. E2-05 no:
 * la NPE ocurre al validar la firma de un XML, no al validar el DE.</p>
 *
 * <p>La integración en la fachada ({@code Sifen.validarDE}, la llamada automática en
 * {@code generarXml}/{@code recepcion*} y {@code SifenConfig.validacionPrevia}) es §3.5; este paquete
 * no cambia el XML generado.</p>
 */
package org.javabeanstack.sifen.core.validation;
