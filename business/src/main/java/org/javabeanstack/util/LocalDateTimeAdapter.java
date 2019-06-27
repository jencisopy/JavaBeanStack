/*
* Copyright (c) 2015-2018 OyM System Group S.A.
* Capitan Cristaldo 464, Asunción, Paraguay
* All rights reserved. 
*
* NOTICE:  All information contained herein is, and remains
* the property of OyM System Group S.A. and its suppliers,
* if any.  The intellectual and technical concepts contained
* herein are proprietary to OyM System Group S.A.
* and its suppliers and protected by trade secret or copyright law.
* Dissemination of this information or reproduction of this material
* is strictly forbidden unless prior written permission is obtained
* from OyM System Group S.A.
 */
package org.javabeanstack.util;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDateTime;

/**
 * Convierte en objeto LocalDateTime a partir de un string
 * Convierte un string a formato LocalDateTime
 * @author Jorge Enciso
 */
public class LocalDateTimeAdapter extends XmlAdapter<String, LocalDateTime> {

    @Override
    public LocalDateTime unmarshal(String v) throws Exception {
        if (v == null){
            return null;
        }
        if (v.length() > 20){
            return LocalDates.toDateTime(v, "yyyy-MM-dd'T'HH:mm:ss.SSS");
        }
        return LocalDates.toDateTime(v, "yyyy-MM-dd'T'HH:mm:ss");
    }

    @Override
    public String marshal(LocalDateTime v) throws Exception {
        if (v == null){
            return null;
        }
        return v.toString();
    }
}
