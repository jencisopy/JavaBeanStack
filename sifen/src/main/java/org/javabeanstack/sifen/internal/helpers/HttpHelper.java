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
package org.javabeanstack.sifen.internal.helpers;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Helper encargado de manejar algunas peticiones HTTP.
 */
public class HttpHelper {
    public static void request(SSLSocketFactory sslSocketFactory, String url) {
        try {
            URL actualURL = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) actualURL.openConnection();
            connection.setInstanceFollowRedirects(false);
            if (connection instanceof HttpsURLConnection) {
                ((HttpsURLConnection) connection).setSSLSocketFactory(sslSocketFactory);
            }
            connection.setRequestMethod("GET");
            DataInputStream in = new DataInputStream(connection.getInputStream());
            connection.connect();
            byte[] buff = new byte[1024];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            int count;
            while ((count = in.read(buff)) != -1) {
                baos.write(buff, 0, count);
            }

            System.out.println("Headers");
            System.out.println("-------");

            Map<String, List<String>> headerFields = connection.getHeaderFields();
            if (headerFields != null) {
                for (String key : headerFields.keySet()) {
                    List<String> strings = headerFields.get(key);
                    System.out.println(key + ": [" + String.join(",", strings) + "]");
                }
            }

            System.out.println(baos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}