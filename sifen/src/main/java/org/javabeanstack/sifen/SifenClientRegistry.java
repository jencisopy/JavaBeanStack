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
package org.javabeanstack.sifen;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.exceptions.Operation;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.internal.facade.Operations;
import org.javabeanstack.sifen.spi.ISendRegistry;

/**
 * Registro de clientes de SIFEN por clave de empresa (contrato v1, plan JBSSIF, §10.2, D34; M3-34).
 * Es un objeto <b>de la aplicación</b>, no un estático de la librería: se crea una vez en un bean de
 * aplicación ({@code @ApplicationScoped} o {@code @Singleton}) y se cierra en su {@code @PreDestroy}.
 *
 * <pre>
 * SifenClientRegistry registry = new SifenClientRegistry();
 * ISifenClient sifen = registry.get(key, () -&gt; SifenConfig.load(companyProperties));
 * registry.invalidate(key);   // al cambiar la configuración o el certificado de la empresa
 * registry.closeAll();        // en el @PreDestroy
 * </pre>
 *
 * <p>Seguro entre hilos. Para una clave, el proveedor se llama <b>una sola vez</b> aunque varios hilos
 * pidan el cliente a la vez (los demás esperan a esa creación, sin bloquear las otras claves); si la
 * creación falla no queda nada guardado y el próximo {@code get} vuelve a intentar. Con el jar en
 * varios WAR, cada WAR tiene su registro: el control contra el doble envío entre WAR es de la
 * aplicación (su estado en la base).</p>
 *
 * <p>Sus clientes comparten el bloqueo de envíos en curso por CDC y la secuencia de {@code Id} de evento
 * (M10-03, I10-19): el cliente nuevo de una clave invalidada no transmite a la vez el mismo CDC que el viejo
 * ni repite sus {@code Id}. Un cliente invalidado o reemplazado se cierra cuando termina su última operación
 * en curso.</p>
 */
public final class SifenClientRegistry {
    private static final Logger LOGGER = LogManager.getLogger(SifenClientRegistry.class);

    private final ConcurrentMap<String, Slot> slots = new ConcurrentHashMap<>();
    private final ISendRegistry sendRegistry;
    private final SifenClientShared shared = new SifenClientShared();

    /** Lugar de una clave: crea su cliente una vez, bajo su propio monitor. */
    private static final class Slot {
        private volatile SifenClient client;

        synchronized SifenClient get(IConfigSupplier supplier, ISendRegistry sendRegistry, SifenClientShared shared)
                throws SifenException {
            if (client == null) {
                SifenConfig config = Operations.execute(Operation.LOAD_CONFIG, null, supplier::get);
                client = SifenClient.create(config, sendRegistry, null, null, shared);
            }
            return client;
        }

        SifenClient current() {
            return client;
        }
    }

    /**
     * Crea un registro vacío, sin registro de envíos.
     */
    public SifenClientRegistry() {
        this(null);
    }

    /**
     * Crea un registro vacío cuyos clientes consultan el registro de envíos de la aplicación antes de
     * transmitir (PROVISIONAL, como {@link ISendRegistry}). El mismo registro de envíos sirve a todas las
     * claves: el CDC identifica al emisor.
     *
     * @param sendRegistry registro de envíos; {@code null} = sin registro.
     */
    @Provisional
    public SifenClientRegistry(ISendRegistry sendRegistry) {
        this.sendRegistry = sendRegistry;
    }

    /**
     * Devuelve el cliente de la clave; si no existe llama al proveedor <b>una vez</b>, valida la
     * configuración y el certificado y lo crea.
     *
     * @param key      clave que elige la aplicación (p. ej. el id de la empresa); no {@code null}.
     * @param supplier proveedor de la configuración de esa clave; no {@code null}.
     * @return el cliente de la clave.
     * @throws SifenException la del proveedor, o {@code SifenValidationException} con todos los errores
     *                        de configuración y certificado; nada queda guardado.
     * @throws NullPointerException si {@code key} o {@code supplier} es {@code null} (error de
     *                              programación).
     */
    public ISifenClient get(String key, IConfigSupplier supplier) throws SifenException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(supplier, "supplier");
        Slot slot = slots.computeIfAbsent(key, k -> new Slot());
        SifenClient client = slot.current();
        if (client != null) {
            return client;
        }
        client = slot.get(supplier, sendRegistry, shared);
        if (slots.get(key) != slot) {
            // M10-03: la clave se invalidó mientras se creaba; el cliente sirve a esta llamada y se cierra después
            client.retire();
        }
        return client;
    }

    /**
     * Olvida el cliente de la clave: la próxima llamada a {@link #get(String, IConfigSupplier)} vuelve a
     * leer la configuración. Las operaciones en curso terminan con el cliente viejo, que se cierra cuando
     * termina la última (M10-03).
     *
     * @param key clave; una que no existe no hace nada.
     */
    public void invalidate(String key) {
        Slot slot = key == null ? null : slots.remove(key);
        if (slot != null) {
            SifenClient viejo = slot.current();
            if (viejo != null) {
                viejo.retire();
            }
            LOGGER.info("Cliente SIFEN invalidado: la próxima operación vuelve a leer la configuración");
        }
    }

    /**
     * Devuelve las claves con cliente creado (para una pantalla de diagnóstico).
     *
     * @return conjunto inmodificable, en orden alfabético.
     */
    public Set<String> keys() {
        Set<String> keys = new TreeSet<>();
        for (Map.Entry<String, Slot> e : slots.entrySet()) {
            if (e.getValue().current() != null) {
                keys.add(e.getKey());
            }
        }
        return Collections.unmodifiableSet(keys);
    }

    /**
     * Cierra todos los clientes y vacía el registro. No lanza.
     */
    public void closeAll() {
        for (String key : slots.keySet()) {
            Slot slot = slots.remove(key);
            SifenClient client = slot == null ? null : slot.current();
            if (client != null) {
                client.close();
            }
        }
    }
}
