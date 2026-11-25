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
package org.javabeanstack.sifen.core.validation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resultado inmutable de una validación previa: <b>todos</b> los incumplimientos juntos, en el orden
 * en que se reportaron (grupos en el orden de registro y, dentro de cada grupo, el de sus reglas).
 *
 * <p>Es {@linkplain #isValid() válido} si no tiene ningún {@link Severity#ERROR}; los
 * {@link Severity#WARNING} no impiden generar ni enviar. {@link #throwIfInvalid()} convierte el
 * resultado en la {@link SifenValidationException} que lanzan la fachada y {@code generarXml} (§3.5).</p>
 *
 * <p>Ninguna operación modifica la instancia: {@link #withPrefix(String)} y {@link #combine(List)}
 * devuelven resultados nuevos.</p>
 */
public final class ValidationResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final ValidationResult VACIO = new ValidationResult(Collections.emptyList());

    /** Errores y avisos en orden de reporte (lista inmodificable). */
    private final List<ValidationError> todos;

    ValidationResult(List<ValidationError> errores) {
        List<ValidationError> copia = new ArrayList<>();
        if (errores != null) {
            for (ValidationError error : errores) {
                if (error != null) {
                    copia.add(error);
                }
            }
        }
        this.todos = List.copyOf(copia);
    }

    /**
     * Devuelve un resultado sin errores ni avisos.
     *
     * @return el resultado vacío (válido).
     */
    public static ValidationResult empty() {
        return VACIO;
    }

    /**
     * Junta varios resultados en uno, en el orden dado (p. ej. los DE de un lote, cada uno ya
     * {@linkplain #withPrefix(String) prefijado}).
     *
     * @param resultados resultados a juntar; {@code null} y los elementos nulos se ignoran.
     * @return el resultado combinado.
     */
    public static ValidationResult combine(List<ValidationResult> resultados) {
        if (resultados == null || resultados.isEmpty()) {
            return VACIO;
        }
        List<ValidationError> lista = new ArrayList<>();
        for (ValidationResult resultado : resultados) {
            if (resultado != null) {
                lista.addAll(resultado.todos);
            }
        }
        return new ValidationResult(lista);
    }

    /**
     * Devuelve todos los incumplimientos: errores y avisos.
     *
     * @return lista inmodificable, en orden de reporte.
     */
    public List<ValidationError> getAll() {
        return todos;
    }

    /**
     * Devuelve solo los de severidad {@link Severity#ERROR}.
     *
     * @return lista inmodificable.
     */
    public List<ValidationError> getErrors() {
        return filtrar(Severity.ERROR);
    }

    /**
     * Devuelve solo los de severidad {@link Severity#WARNING}.
     *
     * @return lista inmodificable.
     */
    public List<ValidationError> getWarnings() {
        return filtrar(Severity.WARNING);
    }

    /**
     * Devuelve los incumplimientos de una regla.
     *
     * @param codigo código de la regla.
     * @return lista inmodificable; vacía si no hay o si {@code codigo} es {@code null}.
     */
    public List<ValidationError> find(String codigo) {
        List<ValidationError> lista = new ArrayList<>();
        if (codigo != null) {
            for (ValidationError error : todos) {
                if (codigo.equals(error.getCode())) {
                    lista.add(error);
                }
            }
        }
        return Collections.unmodifiableList(lista);
    }

    /**
     * Indica si alguna regla con ese código se incumplió.
     *
     * @param codigo código de la regla.
     * @return {@code true} si hay al menos un incumplimiento.
     */
    public boolean contains(String codigo) {
        return !find(codigo).isEmpty();
    }

    /**
     * Indica si el documento puede generarse y enviarse.
     *
     * @return {@code true} si no hay ningún {@link Severity#ERROR} (puede haber avisos).
     */
    public boolean isValid() {
        for (ValidationError error : todos) {
            if (error.isError()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Indica si no hay ningún incumplimiento, ni siquiera avisos.
     *
     * @return {@code true} si la lista está vacía.
     */
    public boolean isEmpty() {
        return todos.isEmpty();
    }

    /**
     * Devuelve un resultado con las rutas precedidas por un prefijo, para acumular varios documentos:
     * con {@code "lote[2]"}, {@code gDatGralOpe.gDatRec.dRucRec} pasa a
     * {@code lote[2].gDatGralOpe.gDatRec.dRucRec}.
     *
     * @param prefijo prefijo de las rutas; {@code null} o en blanco devuelve esta instancia.
     * @return el resultado prefijado.
     */
    public ValidationResult withPrefix(String prefijo) {
        if (prefijo == null || prefijo.isBlank() || todos.isEmpty()) {
            return this;
        }
        List<ValidationError> lista = new ArrayList<>(todos.size());
        for (ValidationError error : todos) {
            lista.add(error.conPrefijo(prefijo));
        }
        return new ValidationResult(lista);
    }

    /**
     * Lanza {@link SifenValidationException} si hay al menos un {@link Severity#ERROR}.
     *
     * @throws SifenValidationException con este resultado completo (errores y avisos).
     */
    public void throwIfInvalid() throws SifenValidationException {
        if (!isValid()) {
            throw new SifenValidationException(this);
        }
    }

    private List<ValidationError> filtrar(Severity severidad) {
        List<ValidationError> lista = new ArrayList<>();
        for (ValidationError error : todos) {
            if (error.getSeverity() == severidad) {
                lista.add(error);
            }
        }
        return Collections.unmodifiableList(lista);
    }

    /**
     * Devuelve la cantidad de errores y avisos y la lista, sin valores rechazados.
     *
     * @return el texto para el log.
     */
    @Override
    public String toString() {
        return "ValidationResult{errores=" + getErrors().size() + ", avisos=" + getWarnings().size()
                + ", detalle=" + todos + "}";
    }
}
