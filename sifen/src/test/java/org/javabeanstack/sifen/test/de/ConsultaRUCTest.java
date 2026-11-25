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
import org.javabeanstack.sifen.core.beans.response.RespuestaConsultaRUC;
import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.fields.response.ruc.TxContRuc;
import org.javabeanstack.sifen.test.SifenTestEnv;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Test en vivo contra el ambiente de test de SIFEN (plan JBSSIF, §1.8): requiere la configuración
 * de {@link SifenTestEnv}; sin ella la clase queda saltada.
 */
@Tag("sifen-live")
public class ConsultaRUCTest extends DETestBase {

    @Test
    @Disabled("I1-08: afirmaba la razón social de un RUC de un tercero (datos ficticios desde I3-64); la consulta del RUC propio está en SifenVivoRucTest")
    public void testConsultaRUC() throws SifenException {
        RespuestaConsultaRUC ret = Sifen.consultaRUC("80555555");
        assert ret.getCodigoEstado() == 200;
        assert ret.getdCodRes() != null && ret.getdCodRes().equals("0502");
        TxContRuc txContRuc = ret.getxContRUC();
        assert txContRuc != null;
        assert txContRuc.getdRazCons().contains("EMPRESA");
        assert txContRuc.getdRazCons().contains("FICTICIA");
        assert txContRuc.getdRazCons().contains("S.A.");
    }

}
