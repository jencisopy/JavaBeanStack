/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2018 Jorge Enciso
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

package org.javabeanstack.web.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author mtrinidad
 */
public class FileHandle {

    /**
     * Guarda un archivo obtenido de un InputStream en el path especificado
     * @param input se espera un InputStream
     * @param path es la ubicación donde se guardará el archivo
     * @return devuelve VERDADERO o FALSO
     */
    public Boolean fileUpload(InputStream input, String path) {
        Boolean result = true;
        OutputStream out = null;
        try {
            out = new FileOutputStream(new File(path));
            int read;
            byte[] bytes = new byte[1024];

            while ((read = input.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            input.close();
            out.flush();
            out.close();
        } catch (FileNotFoundException ex) {
            return false;
        } catch (IOException ex) {
            result = false;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    result = false;
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    result = false;
                }
            }
        }
        return result;
    }
    
    /**
     * Lee un archivo desde una ubicacion especifica y retorna un Streamedcontent
     * del tipo que se especifica 
     * @param sourcePath Ubicación fisica del archivo
     * @param outputType tipo de salida ej: "application/pdf" "image/jpg" etc
     * @return objeto StreamContent de un archivo solicitado.
     */
    public StreamedContent fileStreamTo(String sourcePath, String outputType){
        InputStream input=null;
        StreamedContent content;
        try {
            input = new FileInputStream(sourcePath);
        } catch (FileNotFoundException ex) {
        //  ignore
        }
        content = new DefaultStreamedContent(input, outputType);
        return content;
    }
}

