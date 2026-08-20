/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2018 - 2027 Jorge Enciso
* Email: jorge.enciso.r@gmail.com
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
package org.javabeanstack.poi.excel;

import java.io.OutputStream;
import java.util.List;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.Dependent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Workbook;
import org.javabeanstack.config.IAppConfig;
import org.javabeanstack.model.IAppSystemParam;

/**
 * Servicio de exportación a planillas Excel: el único lugar donde el tope de
 * tamaño se lee y se hace cumplir.
 *
 * <p>Antes de existir, cada aplicación que exportaba repetía el control —y sus
 * valores— en el punto donde armaba el archivo. Dos copias ya bastan para que
 * empiecen a divergir: una sube el tope, la otra no, y nadie se entera hasta
 * que un usuario compara.</p>
 *
 * <p>Es una capa <b>fina</b> sobre {@link ExcelUtil}: el armado del libro
 * —estilos, anchos, nombres de hoja, tipos de celda— sigue siendo de aquél y
 * acá no se duplica nada. Lo que este servicio agrega es lo que
 * {@code ExcelUtil} no puede saber, porque es estático y no conoce la
 * configuración de la aplicación: <b>cuánto es demasiado</b>.</p>
 *
 * <p>Es {@code @Dependent} y no un EJB de sesión a propósito: no tiene estado
 * ni necesita transacción, y un bean de sesión nuevo obliga a declararlo en los
 * descriptores de cada aplicación que lo empaquete.</p>
 *
 * @author jenciso
 */
@Dependent
public class ExcelExportSrv implements IExcelExportSrv {

    private static final Logger LOGGER = LogManager.getLogger(ExcelExportSrv.class);

    @EJB
    private IAppConfig appConfig;

    @Override
    public int getMaxRows(int sheetCount) {
        boolean onlyOne = sheetCount <= 1;
        String param = onlyOne ? PARAM_MAX_ROWS : PARAM_MAX_ROWS_PER_SHEET;
        int byDefault = onlyOne ? DEFAULT_MAX_ROWS : DEFAULT_MAX_ROWS_PER_SHEET;
        if (appConfig == null) {
            //Sin configuración a mano —una prueba, o un contexto sin inyección—
            //el servicio sigue funcionando con los valores por omisión en vez de
            //dejar la exportación inservible.
            return byDefault;
        }
        try {
            IAppSystemParam config = appConfig.getSystemParam(param);
            if (config == null) {
                return byDefault;
            }
            //El valor se guarda ANTES de convertirlo: `getValueNumber()` puede
            //venir nulo —el parámetro existe pero nadie le cargó el número— y
            //encadenar el `.intValue()` haría estallar la exportación entera por
            //una fila mal cargada del catálogo.
            Long value = config.getValueNumber();
            if (value == null || value < 1L) {
                return byDefault;
            }
            return value.intValue();
        } catch (Exception exp) {
            LOGGER.error("No se pudo leer el parametro " + param + ", se usa " + byDefault, exp);
            return byDefault;
        }
    }

    @Override
    public void checkMaxRows(List<ExcelSheetData> sheets) throws ExcelExportLimitException {
        if (sheets == null || sheets.isEmpty()) {
            return;
        }
        //El tope se resuelve UNA vez con la cantidad real de hojas: pedirlo por
        //hoja haría que una exportación cambiara de regla a mitad de camino.
        int maxRows = getMaxRows(sheets.size());
        for (ExcelSheetData sheet : sheets) {
            int rows = (sheet == null || sheet.getRows() == null) ? 0 : sheet.getRows().size();
            if (rows > maxRows) {
                throw new ExcelExportLimitException(sheet.getName(), rows, maxRows);
            }
        }
    }

    @Override
    public Workbook toWorkbook(List<ExcelSheetData> sheets) throws Exception {
        checkMaxRows(sheets);
        return ExcelUtil.toExcelSheets(sheets);
    }

    @Override
    public byte[] toBytes(List<ExcelSheetData> sheets) throws Exception {
        return ExcelUtil.toBytes(toWorkbook(sheets));
    }

    @Override
    public void write(List<ExcelSheetData> sheets, OutputStream output) throws Exception {
        ExcelUtil.write(toWorkbook(sheets), output);
    }
}
