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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.javabeanstack.sifen.core.beans.DocumentoElectronico;
import org.javabeanstack.sifen.core.beans.EventosDE;

/**
 * Recorrido y mutación por reflexión del grafo del modelo ({@code DocumentoElectronico} y sus
 * {@code Tg*}/{@code Td*}) para las pruebas de la validación previa (plan JBSSIF, §3.4).
 *
 * <p>Traduce los campos del modelo a la ruta canónica del MT: el sufijo {@code List} se quita y la
 * posición va como índice base 0 ({@code gCamItemList} ⇒ {@code gCamItem[2]}); los pocos campos que no
 * se llaman como en el MT van en {@link #ALIAS}. Sirve en los dos sentidos: enumera los puntos de
 * mutación de un DE y aplica una mutación a partir de una ruta.</p>
 */
final class GrafoDe {

    /** Campo del modelo ⇒ segmento de la ruta del MT, para los que no coinciden. */
    static final Map<String, String> ALIAS = Map.of("TgGroupTiEvt.rGeVeNom", "rGEveNom");

    /** Raíz de las rutas de los eventos ({@code EventValidator.ROOT}): el {@code TrGesEve}. */
    static final String RAIZ_EVENTO = "rEve";

    /** Campos que no son del DE (salidas de la librería o estado interno) y no se mutan. */
    static final Set<String> IGNORADOS = Set.of("DocumentoElectronico.enlaceQR");

    /** Paquetes cuyas clases se recorren (el resto son hojas). */
    private static final List<String> PAQUETES = List.of("org.javabeanstack.sifen.core.fields.",
            "org.javabeanstack.sifen.core.beans.");

    private GrafoDe() {
    }

    /** Clase de mutación. */
    enum Tipo {
        /** Objeto, texto o wrapper ⇒ {@code null}. */
        NULO,
        /** Primitivo distinto de cero ⇒ {@code 0} (E3-04: el validador toma 0 como «no informado»). */
        CERO,
        /** Lista cargada ⇒ {@code null}. */
        LISTA_NULA,
        /** Lista cargada ⇒ lista vacía. */
        LISTA_VACIA,
        /** Lista cargada ⇒ la misma lista con un {@code null} agregado al final. */
        ELEMENTO_NULO,
        /**
         * Grupo opcional que el caso no carga ⇒ instancia vacía (en una lista, una lista con un elemento
         * vacío): ejercita los grupos que ningún caso del catálogo trae («grupo presente, datos
         * faltantes»).
         */
        GRUPO_VACIO
    }

    /** Profundidad de los grupos vacíos anidados dentro de un grupo vacío. */
    static final int PROFUNDIDAD_GRUPO_VACIO = 3;

    /** Un paso del camino: campo y, si el campo es una lista, el índice del elemento (o -1). */
    record Paso(String clase, String campo, int indice) {
        String segmento() {
            String base = segmentoMt(clase, campo);
            return indice < 0 ? base : base + "[" + indice + "]";
        }
    }

    /** Punto de mutación: camino hasta el campo (el último paso sin índice) y tipo de mutación. */
    record Punto(List<Paso> camino, Tipo tipo) {
        /** Ruta concreta del MT del dato mutado (con índice base 0 en el elemento nulo agregado). */
        String ruta() {
            return rutaDe(camino, tipo);
        }

        /** Campo del modelo, para el informe ({@code TgCamItem.dCantProSer}). */
        String campoModelo() {
            Paso ultimo = camino.get(camino.size() - 1);
            return ultimo.clase() + "." + ultimo.campo();
        }

        @Override
        public String toString() {
            return ruta() + " ⇐ " + tipo;
        }
    }

    static String segmentoMt(String clase, String campo) {
        String alias = ALIAS.get(clase + "." + campo);
        if (alias != null) {
            return alias;
        }
        return campo.endsWith("List") && campo.length() > 4 ? campo.substring(0, campo.length() - 4) : campo;
    }

    static String rutaDe(List<Paso> camino, Tipo tipo) {
        StringBuilder sb = new StringBuilder();
        if (!camino.isEmpty() && camino.get(0).clase().equals("TrGesEve")) {
            sb.append(RAIZ_EVENTO).append('.');
        }
        for (int i = 0; i < camino.size(); i++) {
            if (i > 0) {
                sb.append('.');
            }
            sb.append(camino.get(i).segmento());
        }
        return sb.toString();
    }

    /** {@code true} si la clase es del modelo (se recorre por dentro). */
    static boolean esModelo(Class<?> c) {
        if (c == null || c.isEnum() || c.isPrimitive()) {
            return false;
        }
        String n = c.getName();
        for (String p : PAQUETES) {
            if (n.startsWith(p) && !n.startsWith("org.javabeanstack.sifen.core.beans.response.")) {
                return true;
            }
        }
        return false;
    }

    /** Campos de instancia de la clase y sus superclases del modelo. */
    static List<Field> campos(Class<?> c) {
        List<Field> lista = new ArrayList<>();
        for (Class<?> k = c; k != null && esModelo(k); k = k.getSuperclass()) {
            for (Field f : k.getDeclaredFields()) {
                int m = f.getModifiers();
                if (Modifier.isStatic(m) || f.isSynthetic()
                        || IGNORADOS.contains(k.getSimpleName() + "." + f.getName())) {
                    continue;
                }
                f.setAccessible(true);
                lista.add(f);
            }
        }
        return lista;
    }

    /**
     * Enumera los puntos de mutación del grafo de {@code raiz}: cada campo cargado (no nulo, o primitivo
     * distinto de cero) da una mutación; las listas cargadas dan tres. Recorre en profundidad, en el
     * orden de declaración de los campos.
     */
    static List<Punto> puntos(Object raiz) {
        List<Punto> lista = new ArrayList<>();
        recorrer(raiz, new ArrayList<>(), lista, Collections.newSetFromMap(new IdentityHashMap<>()));
        return lista;
    }

    private static void recorrer(Object obj, List<Paso> prefijo, List<Punto> salida, Set<Object> vistos) {
        if (obj == null || !vistos.add(obj)) {
            return;
        }
        for (Field f : campos(obj.getClass())) {
            if (!tieneSetter(f)) {
                // Sin setter público el consumidor no puede dejarlo nulo (TgTotSub.redondeo, que solo
                // fija el constructor): una NPE ahí no es alcanzable y no se muta.
                continue;
            }
            Object valor = leer(f, obj);
            String clase = f.getDeclaringClass().getSimpleName();
            List<Paso> camino = new ArrayList<>(prefijo);
            camino.add(new Paso(clase, f.getName(), -1));
            if (f.getType().isPrimitive()) {
                if (!esCero(valor)) {
                    salida.add(new Punto(List.copyOf(camino), Tipo.CERO));
                }
                continue;
            }
            if (valor == null || (valor instanceof List<?> vacia && vacia.isEmpty())) {
                gruposVacios(f, camino, salida, 1);
                continue;
            }
            if (valor instanceof List<?> l) {
                salida.add(new Punto(List.copyOf(camino), Tipo.LISTA_NULA));
                salida.add(new Punto(List.copyOf(camino), Tipo.LISTA_VACIA));
                salida.add(new Punto(List.copyOf(camino), Tipo.ELEMENTO_NULO));
                for (int i = 0; i < l.size(); i++) {
                    Object e = l.get(i);
                    if (e != null && esModelo(e.getClass())) {
                        List<Paso> conIndice = new ArrayList<>(prefijo);
                        conIndice.add(new Paso(clase, f.getName(), i));
                        recorrer(e, conIndice, salida, vistos);
                    }
                }
                continue;
            }
            salida.add(new Punto(List.copyOf(camino), Tipo.NULO));
            if (esModelo(valor.getClass())) {
                recorrer(valor, camino, salida, vistos);
            }
        }
    }

    /** Puntos {@link Tipo#GRUPO_VACIO} del campo (si es un grupo del modelo) y de sus grupos hijos. */
    private static void gruposVacios(Field f, List<Paso> camino, List<Punto> salida, int nivel) {
        Class<?> tipo = List.class.isAssignableFrom(f.getType()) ? elemento(f) : f.getType();
        if (tipo == null || !esModelo(tipo) || nivel > PROFUNDIDAD_GRUPO_VACIO) {
            return;
        }
        salida.add(new Punto(List.copyOf(camino), Tipo.GRUPO_VACIO));
        List<Paso> base = new ArrayList<>(camino);
        if (List.class.isAssignableFrom(f.getType())) {
            Paso ultimo = base.remove(base.size() - 1);
            base.add(new Paso(ultimo.clase(), ultimo.campo(), 0));
        }
        for (Field hijo : campos(tipo)) {
            if (!tieneSetter(hijo)) {
                continue;
            }
            List<Paso> c = new ArrayList<>(base);
            c.add(new Paso(hijo.getDeclaringClass().getSimpleName(), hijo.getName(), -1));
            gruposVacios(hijo, c, salida, nivel + 1);
        }
    }

    /**
     * Aplica la mutación sobre {@code raiz} (que tiene que ser una instancia nueva del mismo caso: el
     * camino se resuelve otra vez sobre ella).
     */
    static void aplicar(Object raiz, Punto punto) {
        if (punto.tipo() == Tipo.GRUPO_VACIO) {
            Destino d = crearCamino(raiz, rutaDe(punto.camino(), punto.tipo()));
            if (d == null) {
                throw new IllegalStateException("Grupo vacío no reproducible: " + punto);
            }
            Object grupo = List.class.isAssignableFrom(d.campo().getType()) ? nuevo(elemento(d.campo()))
                    : nuevo(d.campo().getType());
            if (grupo == null) {
                throw new IllegalStateException("Grupo sin constructor sin argumentos: " + punto);
            }
            if (List.class.isAssignableFrom(d.campo().getType())) {
                List<Object> l = new ArrayList<>();
                l.add(grupo);
                d.poner(l);
            } else {
                d.poner(grupo);
            }
            return;
        }
        Object padre = raiz;
        List<Paso> camino = punto.camino();
        for (int i = 0; i < camino.size() - 1; i++) {
            padre = bajar(padre, camino.get(i));
            if (padre == null) {
                throw new IllegalStateException("Camino no reproducible: " + punto);
            }
        }
        Paso ultimo = camino.get(camino.size() - 1);
        Field f = campo(padre.getClass(), ultimo.campo());
        switch (punto.tipo()) {
            case NULO, LISTA_NULA -> escribir(f, padre, null);
            case CERO -> escribir(f, padre, cero(f.getType()));
            case LISTA_VACIA -> escribir(f, padre, new ArrayList<>());
            case ELEMENTO_NULO -> {
                List<Object> copia = new ArrayList<>((List<?>) leer(f, padre));
                copia.add(null);
                escribir(f, padre, copia);
            }
            default -> throw new IllegalStateException(String.valueOf(punto.tipo()));
        }
    }

    /** Ruta concreta del elemento nulo que agrega {@link Tipo#ELEMENTO_NULO}. */
    static String rutaElementoNulo(Object raiz, Punto punto) {
        Object padre = raiz;
        List<Paso> camino = punto.camino();
        for (int i = 0; i < camino.size() - 1; i++) {
            padre = bajar(padre, camino.get(i));
        }
        Paso ultimo = camino.get(camino.size() - 1);
        int tam = ((List<?>) leer(campo(padre.getClass(), ultimo.campo()), padre)).size();
        return punto.ruta() + "[" + tam + "]";
    }

    private static Object bajar(Object obj, Paso paso) {
        Object valor = leer(campo(obj.getClass(), paso.campo()), obj);
        if (paso.indice() >= 0) {
            List<?> l = (List<?>) valor;
            return l == null || l.size() <= paso.indice() ? null : l.get(paso.indice());
        }
        return valor;
    }

    // ------------------------------------------------------------------ por ruta del MT

    /** Resultado de resolver una ruta del MT: el objeto dueño y el campo final (con índice o -1). */
    record Destino(Object duenio, Field campo, int indice) {
        Object valor() {
            Object v = leer(campo, duenio);
            if (indice >= 0) {
                List<?> l = (List<?>) v;
                return l == null || l.size() <= indice ? null : l.get(indice);
            }
            return v;
        }

        /** Tipo del dato final: el del campo, o el de sus elementos si la ruta lleva índice. */
        Class<?> tipo() {
            if (indice >= 0) {
                Class<?> e = elemento(campo);
                return e == null ? Object.class : e;
            }
            return campo.getType();
        }

        void poner(Object nuevo) {
            if (indice >= 0) {
                @SuppressWarnings("unchecked")
                List<Object> l = new ArrayList<>((List<Object>) leer(campo, duenio));
                l.set(indice, nuevo);
                escribir(campo, duenio, l);
            } else {
                escribir(campo, duenio, nuevo);
            }
        }
    }

    /**
     * Resuelve una ruta concreta del MT ({@code gDtipDE.gCamItem[0].gValorItem.dPUniProSer}) sobre
     * {@code raiz}. Un segmento sin índice sobre una lista se toma como la lista entera.
     *
     * @return el destino, o {@code null} si algún eslabón intermedio es nulo o la ruta no existe en
     *         el modelo.
     */
    static Destino resolver(Object raiz, String ruta) {
        String[] segs = sinRaiz(raiz, ruta).split("\\.");
        Object actual = raiz;
        for (int i = 0; i < segs.length; i++) {
            String seg = segs[i];
            int indice = -1;
            int c = seg.indexOf('[');
            if (c >= 0) {
                indice = Integer.parseInt(seg.substring(c + 1, seg.length() - 1));
                seg = seg.substring(0, c);
            }
            Field f = campoMt(actual.getClass(), seg);
            if (f == null) {
                return null;
            }
            if (i == segs.length - 1) {
                return new Destino(actual, f, indice);
            }
            Object v = leer(f, actual);
            if (indice >= 0) {
                List<?> l = (List<?>) v;
                v = l == null || l.size() <= indice ? null : l.get(indice);
            }
            if (v == null) {
                return null;
            }
            actual = v;
        }
        return null;
    }

    /**
     * Como {@link #resolver}, pero crea los eslabones intermedios que falten (objetos del modelo con su
     * constructor sin argumentos; en una lista, los elementos hasta el índice pedido): sirve para el
     * negativo de un dato de un grupo opcional que ningún caso del catálogo carga («grupo presente y
     * dato faltante»). El dato final queda como esté (nulo en un grupo recién creado).
     *
     * @return el destino, o {@code null} si la ruta no existe en el modelo o un eslabón no se puede crear.
     */
    static Destino crearCamino(Object raiz, String ruta) {
        String[] segs = sinRaiz(raiz, ruta).split("\\.");
        Object actual = raiz;
        for (int i = 0; i < segs.length; i++) {
            String seg = segs[i];
            int indice = -1;
            int c = seg.indexOf('[');
            if (c >= 0) {
                indice = Integer.parseInt(seg.substring(c + 1, seg.length() - 1));
                seg = seg.substring(0, c);
            }
            Field f = campoMt(actual.getClass(), seg);
            if (f == null) {
                return null;
            }
            if (i == segs.length - 1) {
                return new Destino(actual, f, indice);
            }
            Object v = leer(f, actual);
            if (indice >= 0 || List.class.isAssignableFrom(f.getType())) {
                int pos = Math.max(indice, 0);
                @SuppressWarnings("unchecked")
                List<Object> l = v == null ? new ArrayList<>() : new ArrayList<>((List<Object>) v);
                Class<?> tipoElemento = elemento(f);
                while (l.size() <= pos) {
                    Object nuevo = nuevo(tipoElemento);
                    if (nuevo == null) {
                        return null;
                    }
                    l.add(nuevo);
                }
                if (l.get(pos) == null) {
                    Object nuevo = nuevo(tipoElemento);
                    if (nuevo == null) {
                        return null;
                    }
                    l.set(pos, nuevo);
                }
                escribir(f, actual, l);
                v = l.get(pos);
            } else if (v == null) {
                v = nuevo(f.getType());
                if (v == null) {
                    return null;
                }
                escribir(f, actual, v);
            }
            actual = v;
        }
        return null;
    }

    /** Tipo de los elementos de un campo lista (o {@code null}). */
    static Class<?> elemento(Field f) {
        if (f.getGenericType() instanceof java.lang.reflect.ParameterizedType pt
                && pt.getActualTypeArguments()[0] instanceof Class<?> k) {
            return k;
        }
        return null;
    }

    /** Instancia vacía de un grupo del modelo (constructor sin argumentos), o {@code null}. */
    static Object nuevoGrupo(Class<?> tipo) {
        return nuevo(tipo);
    }

    private static Object nuevo(Class<?> tipo) {
        if (tipo == null || !esModelo(tipo)) {
            return null;
        }
        try {
            java.lang.reflect.Constructor<?> k = tipo.getDeclaredConstructor();
            k.setAccessible(true);
            return k.newInstance();
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    /** Quita el {@code rEve.} de una ruta de evento cuando la raíz es el {@code TrGesEve}. */
    static String sinRaiz(Object raiz, String ruta) {
        String prefijo = RAIZ_EVENTO + ".";
        if (raiz != null && raiz.getClass().getSimpleName().equals("TrGesEve") && ruta.startsWith(prefijo)) {
            return ruta.substring(prefijo.length());
        }
        return ruta;
    }

    /** Campo de la clase cuyo segmento del MT es {@code segmento}. */
    static Field campoMt(Class<?> c, String segmento) {
        for (Field f : campos(c)) {
            if (segmentoMt(f.getDeclaringClass().getSimpleName(), f.getName()).equals(segmento)) {
                return f;
            }
        }
        return null;
    }

    /**
     * Convierte el texto de una acción {@code set:} al tipo del campo: texto, números, fechas ISO,
     * booleanos y enums (por nombre de la constante).
     */
    static Object convertir(Class<?> tipo, String texto) {
        if (texto.equals("null")) {
            return null;
        }
        if (tipo == String.class) {
            return texto;
        }
        if (tipo == BigDecimal.class) {
            return new BigDecimal(texto);
        }
        if (tipo == java.math.BigInteger.class) {
            return new java.math.BigInteger(texto);
        }
        if (tipo == int.class || tipo == Integer.class) {
            return Integer.valueOf(texto);
        }
        if (tipo == short.class || tipo == Short.class) {
            return Short.valueOf(texto);
        }
        if (tipo == long.class || tipo == Long.class) {
            return Long.valueOf(texto);
        }
        if (tipo == boolean.class || tipo == Boolean.class) {
            return Boolean.valueOf(texto);
        }
        if (tipo == LocalDate.class) {
            return LocalDate.parse(texto);
        }
        if (tipo == LocalDateTime.class) {
            return LocalDateTime.parse(texto);
        }
        if (tipo.isEnum()) {
            for (Object constante : tipo.getEnumConstants()) {
                if (((Enum<?>) constante).name().equals(texto)) {
                    return constante;
                }
            }
            throw new IllegalArgumentException("Constante " + texto + " inexistente en " + tipo.getSimpleName());
        }
        throw new IllegalArgumentException("Tipo no soportado por set: " + tipo.getName());
    }

    // ------------------------------------------------------------------ utilitarios

    static Field campo(Class<?> c, String nombre) {
        for (Field f : campos(c)) {
            if (f.getName().equals(nombre)) {
                return f;
            }
        }
        throw new IllegalStateException("Campo inexistente: " + c.getSimpleName() + "." + nombre);
    }

    static Object leer(Field f, Object obj) {
        try {
            return f.get(obj);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    static void escribir(Field f, Object obj, Object valor) {
        try {
            f.set(obj, valor);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    static boolean esCero(Object v) {
        if (v instanceof Number n) {
            return n.longValue() == 0L;
        }
        if (v instanceof Character ch) {
            return ch == 0;
        }
        return v instanceof Boolean b && !b;
    }

    static Object cero(Class<?> tipo) {
        if (tipo == int.class) {
            return 0;
        }
        if (tipo == short.class) {
            return (short) 0;
        }
        if (tipo == long.class) {
            return 0L;
        }
        if (tipo == byte.class) {
            return (byte) 0;
        }
        if (tipo == double.class) {
            return 0d;
        }
        if (tipo == float.class) {
            return 0f;
        }
        if (tipo == char.class) {
            return (char) 0;
        }
        return Boolean.FALSE;
    }

    /** {@code true} si la clase dueña tiene un {@code set<Campo>} público de un argumento (sin distinguir caja). */
    static boolean tieneSetter(Field f) {
        String buscado = "set" + f.getName();
        for (java.lang.reflect.Method m : f.getDeclaringClass().getMethods()) {
            if (m.getParameterCount() == 1 && m.getName().equalsIgnoreCase(buscado)) {
                return true;
            }
        }
        return false;
    }

    /** Tipos raíz que el recorrido admite (documento y evento). */
    static boolean esRaiz(Object o) {
        return o instanceof DocumentoElectronico || o instanceof EventosDE;
    }
}
