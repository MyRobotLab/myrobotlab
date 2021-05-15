package org.myrobotlab.lang;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Instantiator;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.net.Connection;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.data.Locale;
import org.slf4j.Logger;

// service life-cycle

// 2 stage - generate meta data / meta-"code"
public class LangUtils {

  transient public final static Logger log = LoggerFactory.getLogger(LangUtils.class);

  // meta data generated seperate from string code generation ...
  // NEEDS ORDER
  enum CodePart {
    create, configure, start, attach, subscribe;
  }

  Set<String> suppressExportOfTypes = new TreeSet<>();

  public CodeMeta toMeta(List<ServiceInterface> si) {
    return null;
  }

  static public String escape(String v) {
    if (v == null) {
      return "None";
    }
    return String.format("\"%s\"", v);
  }

  static public String toPython(Boolean b) {
    if (b == null) {
      return "None";
    }
    if (b) {
      return "True";
    }
    return "False";
  }

  // FIXME !! - no "." dots should be allowed in names - dots should always be a
  // network delinator
  // FIXME !! - no special characters host.mydomain.com/inmoov.leftArm.shoulder
  // ?
  static public String safeRefName(ServiceInterface si) {
    return CodecUtils.getSafeReferenceName(si.getName());
  }

  static public String safeRefName(String si) {
    return CodecUtils.getSafeReferenceName(si);
  }

  static public String toPython(String names) throws IOException {
    String[] nameFilters = names.split(",");
    return toPython(nameFilters, null, null);
  }

  static public String toPython() throws IOException {
    return toPython(null, null, null);
  }

  static public String toPython(Double value) {
    if (value == null) {
      return "None";
    }
    return value.toString();
  }

  // TODO ???? - make meta ?? seems meta is already registery and methods &
  // reflection ..
  static public String toPython(String[] nameFilters, String[] typeFilters, Boolean includeRuntime) throws IOException {
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
    // sb.append(String.format("# file %s\n", filename));
    sb.append(String.format("# generated %s\n\n", (new Date()).toString()));

    sb.append("##############################################################\n");
    sb.append("## imports ####\n");
    sb.append("import time\n");
    sb.append("import org.myrobotlab.framework.Platform as Platform\n");
    sb.append("import org.myrobotlab.service.Runtime as Runtime\n");
    sb.append("\n");
    sb.append((Platform.isVirtual() ? "Platform.setVirtual(True)\n" : "# Uncomment to use virtual hardware \n# Platform.setVirtual(True)\n"));

    // from current running system - vs something uncreated passed in ....
    Map<String, ServiceInterface> allServices = Runtime.getLocalServices();
    List<ServiceInterface> services = new ArrayList<>();
    if (nameFilters != null) {
      for (String filter : nameFilters) {
        for (ServiceInterface service : allServices.values()) {
          if (service.getName().equals(filter)) {
            services.add(service);
          }
        }
      }
    }

    if (typeFilters != null) {
      for (String filter : typeFilters) {
        for (ServiceInterface service : allServices.values()) {
          if (service.getSimpleName().equals(filter)) {
            services.add(service);
          }
        }
      }
    }

    if (nameFilters == null && typeFilters == null) {
      // no filters
      services = new ArrayList<ServiceInterface>(allServices.values());
    }

    if (includeRuntime != null && includeRuntime) {
      services.add(Runtime.getInstance());
    }
    
    sb.append("##############################################################\n");
    sb.append(String.format("## creating %d services ####\n", services.size()));
    sb.append("# Although Runtime.start(name,type) both creates and starts services it might be desirable on creation to\n");
    sb.append("# substitute peers, types or references of other sub services before the service is \"started\"\n");
    sb.append("# e.g. i01 = Runtime.create('i01', 'InMoov') # this will \"create\" the service and config could be manipulated before starting \n");
    sb.append("# e.g. i01_left = Runtime.create('i01.left', 'Ssc32UsbServoController')\n");

    sb.append("runtime = Runtime.getInstance()\n");
    
    // the easy start (start peers auto-magically creates peers)
    for (ServiceInterface si : services) {
      if (si.isRuntime()) {
        continue;
      }
      String safename = safeRefName(si);
      sb.append(String.format("%s = Runtime.start('%s', '%s')\n", safename, si.getName(), si.getSimpleName()));
      String localeTag =((Service)si).getLocaleTag();
      if (localeTag != null) {
        sb.append(String.format("%s.setLocale('%s')\n", safename, localeTag));
      }
      // do peers with comments
      // top level peers - others commented out
    }
    
    Runtime runtime = Runtime.getInstance();
    
    sb.append("##############################################################\n");
    sb.append(String.format("## creating client connections connections ####\n"));
    Map<String, Connection> connections = runtime.getConnections();
    for (Connection c : connections.values()) {
      // we can only re-attach "clients" connections - not server/listening connections
      String cType = (String)c.get("c-type");
      if ("Runtime".equals(cType)) {
        sb.append("runtime.connect(\'" +  (String)c.get("url") + "\') \n");
      }
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
    /**
     * <pre>
     * sb.append("##############################################################\n");
     * sb.append(String.format("## starting %d services ####\n",
     * services.size()));
     * 
     * for (ServiceInterface si2 : services) { if (si2.isRuntime()) { continue;
     * } sb.append(String.format("%s = Runtime.start('%s', '%s')\n",
     * safeRefName(si2), si2.getName(), si2.getSimpleName())); // do peers with
     * comments // top level peers - others commented out }
     */
    // attach all ????

    // subscribe all ??? (really should be attach)

    // test ?

    // run

    //

    // connections

    return sb.toString();

  }

}
