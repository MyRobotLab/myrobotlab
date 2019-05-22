package org.myrobotlab.lang;

import java.util.Date;
import java.util.List;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Instantiator;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

// service life-cycle

// 2 stage - generate meta data / meta-"code"
public class LangUtils {

  transient public final static Logger log = LoggerFactory.getLogger(LangUtils.class);

  // meta data generated seperate from string code generation ...
  // NEEDS ORDER
  enum CodePart {
    create, start, attach, subscribe;
  }

  public CodeMeta toMeta(List<ServiceInterface> si) {
    return null;
  }

  // FIXME !! - no "." dots should be allowed in names - dots should always be a
  // network delinator
  // FIXME !! - no special characters host.mydomain.com/inmoov.leftArm.shoulder
  // ?
  static public String safeRefName(ServiceInterface si) {
    return CodecUtils.getSafeReferenceName(si.getName());
  }
  
  static public String toPython() {
    return toPython("export.py");
  }

  // TODO ???? - make meta ?? seems meta is already registery and methods &
  // reflection ..
  static public String toPython(String filename) {
    StringBuilder sb = new StringBuilder();

    // if use date ..
    
    // TODO - filename 
    sb.append("##############################################################\n");
    sb.append("# MyRobotLab configuration file\n");
    sb.append("# This file is generated from a running instance of MyRobotLab.\n");
    sb.append("# It is meant to get MyRobotLab as close to that instance's state a possible.\n");
    sb.append("# This file can be generated at any time using Runtime.save(filename)\n");
    sb.append("# More information @ http://myrobotlab.org and https://github.com/myrobotlab\n");
    sb.append(String.format("# version %s\n", Runtime.getVersion()));
    sb.append(String.format("# file %s\n", filename));
    sb.append(String.format("# generated %s\n\n", (new Date()).toString()));

    sb.append("##############################################################\n");
    sb.append("## imports ####\n");
    sb.append("import org.myrobotlab.framework.Platform as Platform\n");

    
    sb.append("##############################################################\n");
    sb.append("## creating services ####\n");
    sb.append("# Platform virtual state - this virtual setting will attempt to switch all services \n");
    sb.append("# which support virtual hardware to start in a \"virtual\" state where no hardware is needed\n\n");
    
    Platform platform = Platform.getLocalInstance();
    sb.append(String.format("Platform.setVirtual(%s)\n", (platform.isVirtual()?"True":"False")));
    
    // from current running system - vs something uncreated passed in ....
    List<ServiceInterface> services = Runtime.getServices();
    
    // the creation of all services - with peers in comments
    for (ServiceInterface si : services) {
      if (si.isRuntime()) {
        continue;
      }
      sb.append(String.format("%s = Runtime.create('%s', '%s')\n", safeRefName(si), si.getName(), si.getSimpleName()));
      // do peers with comments
      // top level peers - others commented out
    }
    
    sb.append("\n");

    sb.append("##############################################################\n");
    sb.append("## starting services ####\n");
    sb.append("# Although Runtime.start(name,type) both creates and starts services it might be desirable on creation to\n");
    sb.append("# substitute peers, types or references of other sub services before the service is 'started'\n");
    sb.append("# e.g. i01 = Runtime.create('i01', 'InMoov')\n");
    sb.append("# e.g. i01_left = Runtime.create('i01.left', 'Ssc32UsbServoController')\n");
    
    // the easy start (start peers auto-magically creates peers)
    for (ServiceInterface si : services) {
      if (si.isRuntime()) {
        continue;
      }
      sb.append(String.format("%s = Runtime.start('%s', '%s')\n", safeRefName(si), si.getName(), si.getSimpleName()));
      // do peers with comments
      // top level peers - others commented out
    }
    
    sb.append("\n");

    sb.append("##############################################################\n");
    sb.append("## configuring services ####\n");
    // set config of each
    for (ServiceInterface si : services) {
      if (si.isRuntime()) {
        continue;
      }

      // FIXME - use the interface as a template for set/get/call etc..
      // check to see if a matching org.myrobotlab.lang has a generatePython
      String classname = String.format("org.myrobotlab.lang." + si.getSimpleName() + "Lang");
      try {
        Object o = Instantiator.getThrowableNewInstance(null, classname);
        String custom = null;
        custom = (String) Instantiator.invoke(o, "toPython", new Object[] { si });
        sb.append(custom);
        sb.append("\n");
      } catch (Exception e) {
        if (e instanceof ClassNotFoundException) {
          log.info("custom {} not defined", classname);
        } else {
          log.error("{}.toPython threw", classname, e);
        }
      }

      
      // do peers with comments
      // top level peers - others commented out
    }

    // attach all ????

    // subscribe all ??? (really should be attach)

    // test ?

    // run

    //

    return sb.toString();

  }

}
