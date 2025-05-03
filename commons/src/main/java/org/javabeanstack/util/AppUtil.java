/*
* JavaBeanStack FrameWork
*
* Copyright (C) 2017 - 2018 Jorge Enciso
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
package org.javabeanstack.util;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author Jorge Enciso
 */
public class AppUtil {

    private String appFilter = "";

    public static AppUtil getInstance() {
        return new AppUtil();
    }

    public static String stackTraceToString(String filter) {
        List<StackWalker.StackFrame> stackTrace;
        if (filter == null) {
            stackTrace = getInstance().getStackTrace();
        } else {
            stackTrace = getInstance().getStackTrace(filter);
        }
        String retornar = "";
        for (StackWalker.StackFrame frame : stackTrace) {
            if (frame.getLineNumber() > 0) {
                retornar += frame.toString() + "\n";
            }
        }
        return retornar;
    }

    public static String getStackTraceText(String filter) {
        List<StackWalker.StackFrame> stackTrace;
        if (filter == null) {
            stackTrace = getInstance().getStackTrace();
        } else {
            stackTrace = getInstance().getStackTrace(filter);
        }
        String retornar = "";
        for (StackWalker.StackFrame frame : stackTrace) {
            if (frame.getLineNumber() > 0) {
                retornar += Strings.replicate(" ", 20)+frame.toString() + "\n";
            }
        }
        return retornar;
    }
    
    public final StackWalker.StackFrame getCallerStack(){
        List<StackWalker.StackFrame> stackTrace = getStackTrace();
        StackWalker.StackFrame retornar = null;
        for (StackWalker.StackFrame frame:stackTrace){
            if (!frame.getClassName().equals(getClass().getName())){
                retornar = frame;
                break;
            }
        }
        return retornar;
    }

    public final static StackWalker.StackFrame getCallerStack(String className){
        List<StackWalker.StackFrame> stackTrace = getInstance().getStackTrace();
        StackWalker.StackFrame retornar = null;
        boolean ready = false;
        for (StackWalker.StackFrame frame:stackTrace){
            if (!frame.getClassName().equals("org.javabeanstack.util.AppUtil")){
                if (frame.getClassName().equals(className)){
                    ready = true;
                    continue;
                }
                if (ready){
                    retornar = frame;
                    break;
                }
            }
        }
        return retornar;
    }
    
    public static String getAppPackage(){
        String className = getInstance().getCallerStack().getClassName();
        int pos = Strings.findString(".", className, 2);
        String retornar = Strings.substr(className, 0, pos);
        return retornar;
    }
    
    public List<StackWalker.StackFrame> getStackTrace() {
        return getStackTrace(null);
    }

    public List<StackWalker.StackFrame> getStackTrace(String packageFilter) {
        appFilter = packageFilter;
        List<StackWalker.StackFrame> stackTrace;
        if (!Strings.isNullorEmpty(packageFilter)) {
            stackTrace = StackWalker.getInstance().walk(this::collectStackTraceFiltered);
        } else {
            stackTrace = StackWalker.getInstance().walk(this::collectStackTrace);
        }
        //Eliminar el trace de este programa
        stackTrace.remove(0);
        stackTrace.remove(0);
        //Eliminar trace no utiles
        int i = 0;
        for (StackWalker.StackFrame frame : stackTrace) {
            if (frame.getLineNumber() < 0) {
                stackTrace.remove(i);
                break;
            }
            i++;
        }
        return stackTrace;
    }

    protected List<StackWalker.StackFrame> collectStackTrace(Stream<StackWalker.StackFrame> stackFrameStream) {
        return stackFrameStream.collect(Collectors.toList());
    }

    protected List<StackWalker.StackFrame> collectStackTraceFiltered(Stream<StackWalker.StackFrame> stackFrameStream) {
        return stackFrameStream
                .filter(f -> f.getClassName().contains(appFilter) || f.getClassName().contains("org.javabeanstack") || f.getClassName().contains("py.com.oym"))
                .collect(Collectors.toList());
    }

}
