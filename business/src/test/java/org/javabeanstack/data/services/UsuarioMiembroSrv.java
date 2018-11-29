/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javabeanstack.data.services;

import org.javabeanstack.data.services.DataService;
import org.javabeanstack.data.DBManager;

/**
 *
 * @author Jorge Enciso
 */
public class UsuarioMiembroSrv extends DataService{
    @Override
    protected String getPersistentUnit(String sessionId){
        return DBManager.CATALOGO;
    }    
}
