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
package org.javabeanstack.sifen.internal.ctx;

import org.javabeanstack.sifen.core.SifenConfig;

/**
 * Contexto de generación del XML de un DE (configuración y CDC forzado), heredado del fork.
 *
 * <p>Interno, pero lo exponen, entre otras, las sobrecargas de la fachada estática
 * {@link org.javabeanstack.sifen.Sifen}, deprecada en favor de {@link org.javabeanstack.sifen.ISifenClient} (D42):
 * por eso se mantiene sin cambios de firma en 2.x y se retira en 3.0 con ella; no recibe operaciones nuevas
 * (ESC-33, I8-28). Ninguna firma de {@code ISifenClient} lo nombra.</p>
 */
public class GenerationCtx {

    private String forzarCDC;
    private SifenConfig sifenConfig;

    public static GenerationCtx getDefaultFromConfig(SifenConfig sifenConfig) {
        GenerationCtx generationCtx = new GenerationCtx();
        generationCtx.setForzarCDC(null);
        generationCtx.setSifenConfig(sifenConfig);
        return generationCtx;

    }

    public String getForzarCDC() {
        return forzarCDC;
    }

    public void setForzarCDC(String forzarCDC) {
        this.forzarCDC = forzarCDC;
    }


    public SifenConfig getSifenConfig() {
        return sifenConfig;
    }

    public void setSifenConfig(SifenConfig sifenConfig) {
        this.sifenConfig = sifenConfig;
    }
}
