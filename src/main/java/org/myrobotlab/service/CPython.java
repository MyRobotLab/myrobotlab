package org.myrobotlab.service;

import java.io.IOException;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;
import org.bytedeco.javacpp.*;
// import org.bytedeco.cpython.*;
import static org.bytedeco.cpython.global.python.*;



/**
 * 
 * CPython - This is work in progress to borg in the native CPython interpreter
 * via the javacpp presets.  It works at a python 3.7.3 level.
 *  * 
 */
public class CPython extends Service {

  private static final long serialVersionUID = 1L;

  public CPython(String reservedKey, String inId) {
    super(reservedKey, inId);
  }

  public void execScript(String script) throws IOException {
    // TODO: adhere to a full interface, but for now. let's do a poc of running a script
    // in the c python interpreter.
    Py_SetPath(cachePackages());
    // TODO: maybe this should be based on the runtime id?
    Pointer program = Py_DecodeLocale(CPython.class.getSimpleName(), null);
    if (program == null) {
        log.warn("Fatal error: cannot get class name");
        // System.exit(1);
    }
    Py_SetProgramName(program);  /* optional but recommended */
    Py_Initialize();
    PyRun_SimpleStringFlags(script, null);
    if (Py_FinalizeEx() < 0) {
      log.warn("Ouch.. python process said to run a system exit! That's harsh.. nope.");
      //  System.exit(120);
    }
    PyMem_RawFree(program);
  }
  
  static public ServiceType getMetaData() {
    ServiceType meta = new ServiceType(CPython.class.getCanonicalName());
    meta.addDescription("The native C based python binding via JavaCPP Presets. Python version 3.7.3");
    meta.addCategory("programming", "control");
    meta.addDependency("org.bytedeco", "cpython-platform", "3.7.3-1.5.1");
    return meta;
  }
  
  public static void main(String[] args) throws IOException {
    LoggingFactory.init(Level.INFO);
    CPython python = (CPython)Runtime.start("cpython", "CPython");
    String script = "print(\"hello world\")";
    // String script = "from time import time,ctime\nprint('Today is', ctime(time()))\n";
    python.execScript(script);
  }
}