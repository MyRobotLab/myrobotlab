package org.myrobotlab.lang.py;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Instantiator;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.data.Locale;
import org.slf4j.Logger;

// service life-cycle

// 2 stage - generate meta data / meta-"code"
public class LangPyUtils implements PythonGenerator {

  transient public final static Logger log = LoggerFactory.getLogger(LangPyUtils.class);


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
  
  public String toDefaultPython(ServiceInterface si) {
    StringBuilder content = new StringBuilder();
    String safename = LangPyUtils.safeRefName(si);
    content.append(String.format("%s = Runtime.start('%s', '%s')\n", safename, si.getName(), si.getSimpleName()));
    String localeTag = ((Service) si).getLocaleTag();
    Locale defaultLocale = Locale.getDefault();
    if (localeTag != null && !localeTag.equals(defaultLocale.getTag())) {
      content.append(String.format("%s.setLocale('%s')\n", safename, localeTag));
    }
    return content.toString();
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

  public String toPython(String filename, String names) throws IOException {
    return toPython(null, filename, names, null, null);
  }

  public String toPython() throws IOException {
    return toPython(null, null, null, null, null);
  }

  static public String toPython(Double value) {
    if (value == null) {
      return "None";
    }
    return value.toString();
  }

  public static final String capitalize(final String line) {
    return Character.toUpperCase(line.charAt(0)) + line.substring(1);
  }

  public static PythonGenerator getExporter(String clazz) {
    try {
      return (PythonGenerator) Instantiator.getThrowableNewInstance(null, clazz);
    } catch (Exception e) {
      if (e instanceof ClassNotFoundException) {
        log.info("custom {} not defined", clazz);
      } else {
        log.error("{}.toPython threw", clazz, e);
      }
    }
    return null;
  }

  /**
   * <pre>
   * static public String toPython(StringBuilder sb, Boolean asDef, String filename, Integer splitFileDepth, Integer level, Boolean traversePeers, Boolean includeDate,
   *     Boolean includeVersion, String[] includeNames, String[] includeTypes, Boolean includeRuntime, Boolean includeHeader) throws IOException {
   * 
   *   // defaults
   *   if (asDef == null) {
   *     asDef = true;
   *   }
   * 
   *   if (level == null) {
   *     level = 0;
   *   }
   * 
   *   if (splitFileDepth == null) {
   *     splitFileDepth = 1;
   *   }
   * 
   *   // if (splitFileDepth > level) {
   *   // // find parent of file specified
   *   // File parentDir = new File(filename).getParentFile();
   *   // parentDir.mkdirs();
   *   // String ret = exportServices(filename, splitFileDepth, level,
   *   // traversePeers, includeDate, includeVersion, includeNames,
   *   // includeTypes, includeRuntime, includeHeader);
   *   // Files.write(Paths.get(filename), ret.getBytes());
   *   // return ret;
   *   // } else {
   *   // return exportServices(filename, splitFileDepth, level, traversePeers,
   *   // includeDate, includeVersion, includeNames, includeTypes,
   *   // includeRuntime, includeHeader);
   *   // }
   * 
   *   if (sb == null) {
   *     sb = new StringBuilder();
   *   }
   * 
   *   if (traversePeers == null) {
   *     traversePeers = true;
   *   }
   * 
   *   if (includeDate == null) {
   *     includeDate = true;
   *   }
   * 
   *   if (includeVersion == null) {
   *     includeVersion = true;
   *   }
   * 
   *   if (includeHeader == null) {
   *     includeHeader = false;
   *   }
   * 
   *   int indentLevel = 0;
   *   String indent = "";
   * 
   *   // TODO - filename
   *   if (includeHeader) {
   *     sb.append("##############################################################\n");
   *     sb.append("# MyRobotLab configuration file\n");
   *     sb.append("# This file is generated from a running instance of MyRobotLab.\n");
   *     sb.append("# It is meant to get MyRobotLab as close to that instance's state a possible.\n");
   *     sb.append("# This file can be generated at any time using Runtime.save(filename)\n");
   *     sb.append("# More information @ http://myrobotlab.org and https://github.com/myrobotlab\n");
   *     if (includeVersion) {
   *       sb.append(String.format("# version %s\n", Runtime.getVersion()));
   *     }
   *     // sb.append(String.format("# file %s\n", filename));
   *     if (includeDate) {
   *       sb.append(String.format("# generated %s\n\n", (new Date()).toString()));
   *     }
   * 
   *     sb.append("##############################################################\n");
   *     sb.append("## imports ####\n");
   *   }
   *   sb.append("import time\n");
   *   sb.append("import org.myrobotlab.framework.Platform as Platform\n");
   *   sb.append("import org.myrobotlab.service.Runtime as Runtime\n");
   *   sb.append("\n");
   *   sb.append((Platform.isVirtual() ? "Platform.setVirtual(True)\n" : "# Uncomment to use virtual hardware \n# Platform.setVirtual(True)\n"));
   * 
   *   // from current running system - vs something uncreated passed in ....
   *   Map<String, ServiceInterface> allServices = Runtime.getLocalServices();
   *   List<ServiceInterface> services = new ArrayList<>();
   *   if (includeNames != null) {
   *     for (String filter : includeNames) {
   *       for (ServiceInterface service : allServices.values()) {
   *         if (service.getName().equals(filter)) {
   *           services.add(service);
   *         }
   *       }
   *     }
   *   }
   * 
   *   // FIXME - includePeers !!!
   * 
   *   if (includeTypes != null) {
   *     for (String filter : includeTypes) {
   *       for (ServiceInterface service : allServices.values()) {
   *         if (service.getSimpleName().equals(filter)) {
   *           services.add(service);
   *         }
   *       }
   *     }
   *   }
   * 
   *   if (includeNames == null && includeTypes == null) {
   *     // no filters
   *     services = new ArrayList<ServiceInterface>(allServices.values());
   *   }
   * 
   *   if (includeRuntime != null && includeRuntime) {
   *     services.add(Runtime.getInstance());
   *   }
   * 
   *   if (includeHeader) {
   *     sb.append("##############################################################\n");
   *     sb.append(String.format("## creating %d services ####\n", services.size()));
   *     sb.append("# Although Runtime.start(name,type) both creates and starts services it might be desirable on creation to\n");
   *     sb.append("# substitute peers, types or references of other sub services before the service is \"started\"\n");
   *     sb.append("# e.g. i01 = Runtime.create('i01', 'InMoov') # this will \"create\" the service and config could be manipulated before starting \n");
   *     sb.append("# e.g. i01_left = Runtime.create('i01.left', 'Ssc32UsbServoController')\n");
   *   }
   *   // sb.append("runtime = Runtime.getInstance()\n");
   * 
   *   // FIXME create and start vs start option !
   *   // FIXME - order service based on creation order
   * 
   *   if (asDef) {
   *     indentLevel = 2;
   *     indent = "  ";
   *   }
   * 
   *   // the easy start (start peers auto-magically creates peers)
   *   for (ServiceInterface si : services) {
   *     if (si.isRuntime()) {
   *       continue;
   *     }
   *     String safename = safeRefName(si);
   *     sb.append(String.format("%s = Runtime.start('%s', '%s')\n", safename, si.getName(), si.getSimpleName()));
   *     String localeTag = ((Service) si).getLocaleTag();
   *     Locale defaultLocale = Locale.getDefault();
   *     if (localeTag != null && !localeTag.equals(defaultLocale.getTag())) {
   *       sb.append(String.format("%s.setLocale('%s')\n", safename, localeTag));
   *     }
   *     // do peers with comments
   *     // top level peers - others commented out
   *   }
   * 
   *   Runtime runtime = Runtime.getInstance();
   *   boolean firstTime = true;
   * 
   *   Map<String, Connection> connections = runtime.getConnections();
   *   for (Connection c : connections.values()) {
   *     if (firstTime && includeHeader) {
   *       sb.append("##############################################################\n");
   *       sb.append(String.format("## creating client connections connections ####\n"));
   *     }
   *     // we can only re-attach "clients" connections - not server/listening
   *     // connections
   *     String cType = (String) c.get("c-type");
   *     if ("Runtime".equals(cType)) {
   *       sb.append("runtime.connect(\'" + (String) c.get("url") + "\') \n");
   *     }
   *   }
   * 
   *   sb.append("\n");
   * 
   *   if (includeHeader) {
   *     sb.append("##############################################################\n");
   *     sb.append("## configuring services ####\n");
   *   }
   *   // set config of each
   *   for (ServiceInterface si : services) {
   *     if (si.isRuntime()) {
   *       continue;
   *     }
   * 
   *     // FIXME - use the interface as a template for set/get/call etc..
   *     // check to see if a matching org.myrobotlab.lang has a generatePython
   *     String classname = String.format("org.myrobotlab.lang.py." + si.getSimpleName() + "Py");
   * 
   *     try {
   *       Object o = Instantiator.getThrowableNewInstance(null, classname);
   *       String custom = null;
   *       custom = (String) Instantiator.invoke(o, "toPython", new Object[] { si });
   *       sb.append(custom);
   *       sb.append("\n");
   *     } catch (Exception e) {
   *       if (e instanceof ClassNotFoundException || e instanceof InvocationTargetException) {
   *         log.info("custom {} not defined", classname);
   *       } else {
   *         log.error("{}.toPython threw", classname, e);
   *       }
   *     }
   * 
   *     firstTime = false;
   *     MetaData serviceType = ServiceData.getMetaData(si.getName(), si.getSimpleName());
   *     Map<String, ServiceReservation> peers = serviceType.getPeers();
   *     if (peers.size() > 0 && includeHeader) {
   *       sb.append("## peers ####\n");
   *     }
   * 
   *     // FIXME - do "indent"
   *     for (String peer : peers.keySet()) {
   *       ServiceReservation sr = peers.get(peer);
   *       String safename = safeRefName(sr.actualName);
   *       ServiceInterface peerSi = Runtime.getService(sr.actualName);
   * 
   *       if (asDef) {
   *         sb.append(String.format("def start%s()", capitalize(sr.key)));
   *       }
   * 
   *       // TODO - check state of peer created/started
   *       // peer is not currently running
   *       if (peerSi == null) {
   *         sb.append(String.format(indent + "%s = Runtime.start('%s', '%s')\n", safename, sr.actualName, sr.type));
   *       } else {
   *         // peer is currently running
   *         sb.append(String.format(indent + "%s = Runtime.start('%s', '%s')\n", safename, sr.actualName, sr.type));
   * 
   *         String peerFile = new File(filename).getParent() + File.separator + sr.actualName + ".py";
   * 
   *         if (splitFileDepth > level) {
   *           // write individual py files at this level
   *           // find parent of file specified
   *           File parentDir = new File(filename).getParentFile();
   *           parentDir.mkdirs();
   *           // FIXME - is splitFileDepth or level a different depth because
   *           // they are roots of their own files ?
   *           String ret = toPython(null, true, peerFile, splitFileDepth, level + 1, traversePeers, includeDate, includeVersion, new String[] { sr.actualName }, includeTypes,
   *               includeRuntime, includeHeader);
   *           Files.write(Paths.get(filename), ret.getBytes());
   *           return ret;
   *         } else {
   *           // concatenate the file contents with sub peers
   *           return sb.append(toPython(sb, true, filename, splitFileDepth, level + 1, traversePeers, includeDate, includeVersion, new String[] { sr.actualName }, includeTypes,
   *               includeRuntime, includeHeader)).toString();
   *         }
   *       }
   *     } // for all peers
   *   }
   *   return sb.toString();
   * }
   * </pre>
   */

  // options :
  // force overwrite - default do not overwrite
  // "launch.yml" is the interface - it only saves launch.yml

  public String toPython(StringBuilder content, String folder, String names, Boolean overwrite, Boolean groupByPeer) throws IOException {

    // defaults

    if (content == null) {
      content = new StringBuilder();
    }

    if (folder == null) {
      folder = "data" + File.separator + "config" + File.separator + "default";
    }

    File dir = new File(folder);
    dir.mkdirs();

    String filename = folder + File.separator + "launch.yml";

    if (overwrite == null) {
      overwrite = true;
    }

    if (groupByPeer == null) {
      groupByPeer = true;
    }

    String[] includes = null;
    if (names != null) {
      includes = names.split(",");
    }

    // preconditions -
    // needs a directory path data/config/{name}/launch.yml
    File launchFile = new File(filename);
    if (launchFile.exists() && !overwrite) {
      throw new IOException("file %s already exists");
    }

    List<ServiceInterface> all = Runtime.getServices();
    Collections.sort(all);

    for (ServiceInterface si : all) {

      // filtration of services based on includes, excludes, types, peers
      boolean match = false;
      if (includes != null) {

        for (int i = 0; i < includes.length; ++i) {
          if (si.getName().equals(includes[i])) {
            match = true;
            break;
          }
        }
      } else {
        match = true;
      }

      if (!match) {
        continue;
      }

      // if groupByPeers and a running service matches a defined peer of a match
      // - then export

      // split at peer level == x
      if (groupByPeer) {
        // if running peers - append and exclude ...
      }

      // write module file
      File init = new File(folder + File.separator + "__init__.py");
      if (!init.exists()) {
        Files.write(init.toPath(), "".getBytes());
      }

      // check for custom exporter ...
      String newPythonContent = null;
      PythonGenerator generator = getExporter(String.format("org.myrobotlab.lang.py.%sPy", si.getSimpleName()));
      if (generator != null) {
        newPythonContent = generator.toPython(si);
      } else {
        newPythonContent = toDefaultPython(si);
      }

      content.append(newPythonContent);
      // if multiFile
      Files.write(new File(folder + File.separator + si.getName() + ".py").toPath(), content.toString().getBytes());

      log.info("{}", si.getName());
    }

    // conditional write launch file ...

    return null;
  }

  @Override
  public String toPython(ServiceInterface si) {
   return toDefaultPython(si);
  }

}
