package org.myrobotlab.framework;

import java.io.Serializable;
import java.util.ArrayList;

import org.myrobotlab.cmdline.CmdLine;
import org.myrobotlab.framework.interfaces.Invoker;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * Simple class representing an operating system mrl process
 * 
 * @author GroG
 *
 */
public class ProcessData implements Serializable {
  
  public final static Logger log = LoggerFactory.getLogger(ProcessData.class);

  private static final long serialVersionUID = 1L;

  // public String name;
  public String id;
  public String branch;
  public String version;
  public Long startTs;
  public Long stopTs;
  public String jarPath = null;
  public String javaExe = null;
  public Long lastUpdatedTs;

  public boolean autoUpdate = false;

  // TODO - default more memory ? also mebbe have a jvmAppend flag vs replace ?
  public String jvm[];

  // FIXME - remove
  public boolean userDefinedServices = false;

  /**
   * current state of this process
   */
  public stateType state = stateType.stopped;

  // is this really used for anything ?
  @Deprecated
  public String fromAgent = null;

  @Deprecated /* just temp */
  public ArrayList<String> in = null;

  /**
   * actual java process
   */
  transient public Process process;

  /**
   * monitor on the process - if it dies agent will know
   */
  transient public Monitor monitor;

  /**
   * agent for callbacks when terminated or started
   */
  static transient public Invoker agent;

  public enum stateType {
    stopped, running, sleeping, restarting, updating, unknown
  }

  public static class Monitor extends Thread {
    ProcessData data;

    public Monitor(ProcessData pd) {
      super(String.format("%s.monitor", pd.id));
      this.data = pd;
    }

    @Override
    public void run() {
      try {
        if (data.process != null) {
          // data.isRunning = true;
          data.state = stateType.running;
          // don't wait if there is no agent
          if (agent != null) {
            data.process.waitFor();
          }
        }
      } catch (Exception e) {
      }

      data.state = stateType.stopped;

      if (agent != null) {
        agent.invoke("publishTerminated", data.id);
      }
    }
  }

  public ProcessData() {
  }

  /**
   * copy of a ProcessData - threaded data will not be copied
   * 
   * @param pd
   *          the process data
   */
  public ProcessData(ProcessData pd) {
    this.id = pd.id;
    this.branch = pd.branch;
    this.version = pd.version;
    this.version = pd.version;
    this.javaExe = pd.javaExe;
    this.jarPath = pd.jarPath;
    this.jvm = pd.jvm;

    this.userDefinedServices = pd.userDefinedServices;

    if (pd.in != null) {
      this.in = new ArrayList<String>();
      in.addAll(pd.in);
    }
  }

  public boolean isRunning() {
    return state == stateType.running;
  }

  public void setRestarting() {
    state = stateType.restarting;
  }

  public boolean isRestarting() {
    return state == stateType.restarting;
  }
  
  public String toString() {
    return String.format("id:%s branch:%s version:%s", id, branch, version);
  }

}
