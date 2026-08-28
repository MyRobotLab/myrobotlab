package org.myrobotlab.service.config;

public class Py4jConfig extends ServiceConfig {

  /**
   * root of python scripts - if not specified by user it will be
   *  /data/Py4j/{serviceName}
   */
  public String scriptRootDir;

  /**
   * Reserved: a bundled interpreter is not wired yet. Py4j always creates
   * {@code data/Py4j/venv} from {@code python3} or {@code python} on PATH.
   */
  public boolean useBundledPython = true;
  
  /**
   * Whether to start a new Python process with
   * stdin/stdout connected to the JVM or leave the
   * starting of the process to the user.
   */
  public boolean autostartPython = true;

}
