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
package org.javabeanstack.sifen.core.fields.request.de;

import org.javabeanstack.sifen.core.exceptions.SifenException;
import org.javabeanstack.sifen.core.types.*;
import org.javabeanstack.sifen.internal.response.SifenObjectBase;
import org.javabeanstack.sifen.internal.util.ResponseUtil;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import java.math.BigDecimal;

/**
 * Grupo D030 {@code gOblAfe}: una obligación afectada por la operación (NT 018, imputación de las ventas al RG90).
 * Va de 0 a 11 veces dentro de {@code gOpeCom} (D010), después de sus campos; lo arma {@link TgOpeCom} desde su lista
 * ({@link TgOpeCom#getgOblAfeList()}).
 *
 * <p>Campos: {@code cOblAfe} (D031, código de la Tabla 12, {@link TcOblAfe}) y {@code dDesOblAfe} (D032, A21-65), que la
 * librería deriva del código —la validación D032 de SIFEN exige que coincidan—. Plan JBSSIF, §6.1 (I0-01).</p>
 *
 * <p>Delta del fork 2.0.0-SNAPSHOT: la clase era una copia de {@code TgOpeCom} que nadie usaba y escribía el literal
 * {@code gOpeCom} (I0-01). Sus accesores de esa copia ({@code iTipTra}, {@code iTImp}, {@code cMoneOpe},
 * {@code dCondTiCam}, {@code dTiCam}, {@code iCondAnt}) se conservan, deprecados y sin efecto en el XML, porque son parte
 * de la foto del contrato v1.</p>
 */
public class TgOblAfe extends SifenObjectBase {
    private TcOblAfe cOblAfe;

    // restos de la copia de TgOpeCom del fork (I0-01): sin efecto, se conservan por el contrato v1
    private TTipTra iTipTra;
    private TTImp iTImp;
    private CMondT cMoneOpe;
    private TdCondTiCam dCondTiCam;
    private BigDecimal dTiCam;
    private TiCondAnt iCondAnt;

    /** Crea la obligación vacía. */
    public TgOblAfe() {
    }

    /**
     * Crea la obligación con su código.
     *
     * @param cOblAfe código de la Tabla 12.
     */
    public TgOblAfe(TcOblAfe cOblAfe) {
        this.cOblAfe = cOblAfe;
    }

    /**
     * Método interno, no usar: escribe {@code gOblAfe} dentro de {@code gOpeCom}.
     *
     * @param gOpeCom nodo padre (D010).
     * @throws SOAPException si falla el armado.
     */
    public void setupSOAPElements(SOAPElement gOpeCom) throws SOAPException {
        SOAPElement gOblAfe = gOpeCom.addChildElement("gOblAfe");
        gOblAfe.addChildElement("cOblAfe").setTextContent(String.valueOf(this.cOblAfe.getVal()));
        gOblAfe.addChildElement("dDesOblAfe").setTextContent(this.cOblAfe.getDescripcion());
    }

    @Override
    public void setValueFromChildNode(Node value) throws SifenException {
        // dDesOblAfe se deriva del código (D032)
        if ("cOblAfe".equals(value.getLocalName())) {
            this.cOblAfe = TcOblAfe.getByVal(Short.parseShort(ResponseUtil.getTextValue(value)));
        }
    }

    /**
     * Devuelve el código de la obligación ({@code cOblAfe}, D031).
     *
     * @return el código, o {@code null} si no se cargó o el XML trae uno fuera de la Tabla 12.
     */
    public TcOblAfe getcOblAfe() {
        return cOblAfe;
    }

    /**
     * Fija el código de la obligación ({@code cOblAfe}, D031); la descripción ({@code dDesOblAfe}) sale de la Tabla 12.
     *
     * @param cOblAfe código.
     */
    public void setcOblAfe(TcOblAfe cOblAfe) {
        this.cOblAfe = cOblAfe;
    }

    /**
     * Devuelve la descripción que se escribe en {@code dDesOblAfe} (D032): la de la Tabla 12 para el código.
     *
     * @return la descripción, o {@code null} sin código.
     */
    public String getdDesOblAfe() {
        return cOblAfe == null ? null : cOblAfe.getDescripcion();
    }

    /** @return sin efecto en el XML. @deprecated resto de la copia de {@code TgOpeCom} del fork (I0-01). */
    @Deprecated
    public TTipTra getiTipTra() {
        return iTipTra;
    }

    /** @param iTipTra sin efecto en el XML. @deprecated resto de la copia de {@code TgOpeCom} del fork (I0-01). */
    @Deprecated
    public void setiTipTra(TTipTra iTipTra) {
        this.iTipTra = iTipTra;
    }

    /** @return sin efecto en el XML. @deprecated resto de la copia de {@code TgOpeCom} del fork (I0-01). */
    @Deprecated
    public TTImp getiTImp() {
        return iTImp;
    }

    /** @param iTImp sin efecto en el XML. @deprecated resto de la copia de {@code TgOpeCom} del fork (I0-01). */
    @Deprecated
    public void setiTImp(TTImp iTImp) {
        this.iTImp = iTImp;
    }

    /** @return sin efecto en el XML. @deprecated resto de la copia de {@code TgOpeCom} del fork (I0-01). */
    @Deprecated
    public CMondT getcMoneOpe() {
        return cMoneOpe;
    }

    /** @param cMoneOpe sin efecto en el XML. @deprecated resto de la copia de {@code TgOpeCom} del fork (I0-01). */
    @Deprecated
    public void setcMoneOpe(CMondT cMoneOpe) {
        this.cMoneOpe = cMoneOpe;
    }

    /** @return sin efecto en el XML. @deprecated resto de la copia de {@code TgOpeCom} del fork (I0-01). */
    @Deprecated
    public TdCondTiCam getdCondTiCam() {
        return dCondTiCam;
    }

    /** @param dCondTiCam sin efecto en el XML. @deprecated resto de la copia de {@code TgOpeCom} del fork (I0-01). */
    @Deprecated
    public void setdCondTiCam(TdCondTiCam dCondTiCam) {
        this.dCondTiCam = dCondTiCam;
    }

    /** @return sin efecto en el XML. @deprecated resto de la copia de {@code TgOpeCom} del fork (I0-01). */
    @Deprecated
    public BigDecimal getdTiCam() {
        return dTiCam;
    }

    /** @param dTiCam sin efecto en el XML. @deprecated resto de la copia de {@code TgOpeCom} del fork (I0-01). */
    @Deprecated
    public void setdTiCam(BigDecimal dTiCam) {
        this.dTiCam = dTiCam;
    }

    /** @return sin efecto en el XML. @deprecated resto de la copia de {@code TgOpeCom} del fork (I0-01). */
    @Deprecated
    public TiCondAnt getiCondAnt() {
        return iCondAnt;
    }

    /** @param iCondAnt sin efecto en el XML. @deprecated resto de la copia de {@code TgOpeCom} del fork (I0-01). */
    @Deprecated
    public void setiCondAnt(TiCondAnt iCondAnt) {
        this.iCondAnt = iCondAnt;
    }
}
