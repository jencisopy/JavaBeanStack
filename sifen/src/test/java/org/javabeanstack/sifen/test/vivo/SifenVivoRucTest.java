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
package org.javabeanstack.sifen.test.vivo;

import org.javabeanstack.sifen.Sifen;
import org.javabeanstack.sifen.core.SifenConfig;
import org.javabeanstack.sifen.core.beans.response.RespuestaConsultaRUC;
import org.javabeanstack.sifen.test.SifenTestEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Verificación de §2.7 en vivo (plan JBSSIF): con el archivo de {@link SifenTestEnv}, la consulta del
 * RUC del propio emisor de prueba responde. No necesita ventas extraídas (M2-01) ni envía nada: es
 * solo lectura, en el ambiente de pruebas (D28). Reemplaza la expectativa de {@code ConsultaRUCTest}
 * sobre un RUC de un tercero (I1-08).
 */
@Tag("sifen-live")
public class SifenVivoRucTest {
    private SifenConfig cfg;
    private SifenTestEnv.Emisor emisor;

    @BeforeEach
    public void preparar() {
        cfg = SifenTestEnv.configuracionOSaltar();
        emisor = SifenTestEnv.emisorOSaltar();
    }

    @Test
    public void consultaRucDelEmisorDePrueba() throws Exception {
        RespuestaConsultaRUC r = Sifen.consultaRUC(emisor.getRuc(), cfg);
        Assertions.assertEquals(200, r.getCodigoEstado());
        Assertions.assertEquals("0502", r.getdCodRes(), "consultaRUC: " + r.getdMsgRes());
        Assertions.assertNotNull(r.getxContRUC());
        Assertions.assertEquals(emisor.getRuc(), r.getxContRUC().getdRUCCons());
    }
}
