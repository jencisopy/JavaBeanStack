/*
* Copyright (c) 2015-2017 OyM System Group S.A.
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
package org.javabeanstack.services;

import java.util.List;
import org.javabeanstack.model.IAppCompany;
import org.javabeanstack.security.IUserSession;

/**
 *
 * @author Jorge Enciso
 */
public interface IAppCompanySrv extends IDataService {
    List<IAppCompany> getAppCompanyLight(IUserSession userSession);
    List<IAppCompany> getAppCompany(IUserSession userSession);
}
