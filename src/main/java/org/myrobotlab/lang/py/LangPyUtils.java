package org.myrobotlab.lang.py;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Instantiator;
import org.myrobotlab.framework.Peer;
import org.myrobotlab.framework.Plan;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.data.Locale;
import org.myrobotlab.service.meta.abstracts.MetaData;
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

  public String toDefaultStartPython(ServiceInterface si, boolean includeHeader) {
    StringBuilder content = new StringBuilder();

    if (includeHeader) {
      content.append("import time\n");
      content.append("import org.myrobotlab.framework.Platform as Platform\n");
      content.append("import org.myrobotlab.service.Runtime as Runtime\n");
      content.append("\n");

      // we make start methods now
      content.append("def start():\n");

      content.append(String.format("  " + "print('loading %s of type %s')", si.getName(), si.getSimpleName()));
      content.append("\n");
    }

    String safename = LangPyUtils.safeRefName(si);

    content.append(String.format("  %s = Runtime.start('%s', '%s')\n", safename, si.getName(), si.getSimpleName()));
    String localeTag = ((Service) si).getLocaleTag();
    Locale defaultLocale = Locale.getDefault();
    if (localeTag != null && !localeTag.equals(defaultLocale.getTag())) {
      content.append(String.format("  %s.setLocale('%s')\n", safename, localeTag));
    }

    return content.toString();
  }

  public String toDefaultReleasePython(ServiceInterface si, boolean includeHeader) {
    StringBuilder content = new StringBuilder();

    String safename = LangPyUtils.safeRefName(si);

    if (includeHeader) {
      content.append("\n");
      content.append("def release():\n");
      content.append(String.format("  %s = Runtime.release('%s')\n", safename, si.getName()));
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
    return toPython(null, null, null, filename, names, null, null, null, null);
  }

  public String toPython() throws IOException {
    return toPython(null, null, null, null, null, null, null, null, null);
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

  // this method is used as an entry point for generating python - everything
  // done it is done only once
  // recursive calls are confined to buildPython
  public String toPython(StringBuilder content, Boolean includeHeader, Boolean numericPrefix, String folder, String names, Integer currentDepth, Integer splitLevel,
      Boolean overwrite, Integer maxDepth) throws IOException {

    Map<Integer, String> serviceFileWritten = buildPython(content, includeHeader, numericPrefix, folder, names, currentDepth, splitLevel, overwrite, maxDepth);

    // determine order of creation for the services written to file - sort

    List<Integer> order = new ArrayList<>();
    for (Integer o : serviceFileWritten.keySet()) {
      order.add(o);
    }

    Collections.sort(order);

    // write module file
    StringBuilder initContent = new StringBuilder();
    File init = new File(folder + File.separator + "__init__.py");
    for (Integer n : order) {
      initContent.append("from . import " + CodecUtils.getSafeReferenceName(serviceFileWritten.get(n)) + "_config\n");
    }
    initContent.append("\n");
    initContent.append("def start():\n");
    for (Integer n : order) {
      initContent.append("  " + CodecUtils.getSafeReferenceName(serviceFileWritten.get(n)) + "_config.start()\n");
    }
    Files.write(init.toPath(), initContent.toString().getBytes());

    initContent.append("\n");
    initContent.append("def release():\n");
    for (Integer n : order) {
      initContent.append("  " + CodecUtils.getSafeReferenceName(serviceFileWritten.get(n)) + "_config.release()\n");
    }
    Files.write(init.toPath(), initContent.toString().getBytes());

    return null;
  }

  // options :
  // force overwrite - default do not overwrite
  // "launch.yml" is the interface - it only saves launch.yml

  public Map<Integer, String> buildPython(StringBuilder content, Boolean includeHeader, Boolean numericPrefix, String folder, String names, Integer currentDepth,
      Integer splitLevel, Boolean overwrite, Integer maxDepth) throws IOException {

    // FIXME - switch to use the default excludes
    Set<String> excludes = new HashSet<>();
    excludes.add("runtime");
    excludes.add("python");
    excludes.add("security");
    excludes.add("webgui");
    excludes.add("intro");

    // defaults
    if (currentDepth == null) {
      currentDepth = 0;
    }

    // FIXME - remove
    if (numericPrefix == null) {
      numericPrefix = false;
    }

    if (splitLevel == null) {
      splitLevel = 1; // this is desired by inmoov InMoovHead, Torso etc...
    }

    if (includeHeader == null) {
      includeHeader = true;
    }

    if (content == null) {
      content = new StringBuilder();
    }

    if (folder == null) {
      folder = Runtime.getInstance().getConfigPath() + File.separator + "default";
    }

    Map<Integer, String> serviceFileWritten = new HashMap<>();

    String check = folder.replace("\\", "/");
    String[] chkdir = check.split("/");
    for (int i = 0; i < chkdir.length; ++i) {
      String chk = CodecUtils.getSafeReferenceName(chkdir[i]);
      if (!chk.equals(chkdir[i])) {
        throw new IOException(String.format("%s not valid name, consider %s", chkdir[i], chk));
      }
    }

    File dir = new File(folder);
    dir.mkdirs();

    String filename = folder + File.separator + "launch.yml";

    if (overwrite == null) {
      overwrite = true;
    }

    Map<String, ServiceInterface> all = Runtime.getLocalServices();

    Set<String> includes = new HashSet<>();
    if (names != null) {
      String[] n = names.split(",");
      for (int i = 0; i < n.length; ++i) {
        String name = n[i];
        if (name.indexOf("@") < 0) {
          includes.add(name + "@" + Runtime.getInstance().getId());
        } else {
          includes.add(name);
        }
      }
    } else {
      includes.addAll(all.keySet());
    }

    // preconditions -
    // needs a directory path data/config/{name}/launch.yml
    File launchFile = new File(filename);
    if (launchFile.exists() && !overwrite) {
      throw new IOException("file %s already exists");
    }

    // If a single service and its peers, this doesnt make much difference
    // but for random services it may. if we process breadth first, and
    // have more than one service, then process them in the order they were
    // created
    List<ServiceInterface> list = new ArrayList<>();
    list.addAll(all.values());
    Collections.sort(list);

    // for (ServiceInterface si : all.values()) {
    for (ServiceInterface si : list) {

      // filtration of services based on includes, excludes, types, peers
      if (!includes.contains(si.getFullName())) {
        continue;
      }

      if (excludes.contains(si.getName())) {
        continue;
      }

      // check for custom exporter ...
      StringBuilder newPythonContent = new StringBuilder();
      PythonGenerator generator = getExporter(String.format("org.myrobotlab.lang.py.%sPy", si.getSimpleName()));
      if (generator != null) {
        newPythonContent.append(toDefaultStartPython(si, includeHeader));
        newPythonContent.append(generator.toPython(si));
      } else {
        newPythonContent.append(toDefaultStartPython(si, includeHeader));
      }

      content.append(newPythonContent);

      MetaData.getConfigType(filename);

      // static getDefault routes by type
      Plan plan = ServiceConfig.getDefault(Runtime.getPlan(), si.getName(), si.getSimpleName());
      ServiceConfig sc = plan.get(si.getName());
      Map<String, Peer> peers = sc.getPeers();

      // FIXME - do "indent"
      boolean firstTime = true;
      if ((maxDepth == null || maxDepth < currentDepth) && peers != null) {
        for (String peer : peers.keySet()) {
          Peer sr = peers.get(peer);
          String noWorky = "noWorky"; // had to fix
          ServiceInterface peerSi = Runtime.getService(noWorky);
          if (peerSi != null) {
            if (currentDepth >= splitLevel) {
              // concatenate content
              if (firstTime) {
                content.append(String.format("\n  # %s peers\n", si.getSimpleName()));
              }
              serviceFileWritten.putAll(buildPython(content, false, numericPrefix, folder, noWorky, currentDepth + 1, splitLevel, overwrite, maxDepth));
              content.append("\n");
            } else {
              // don't concatenate - split files
              serviceFileWritten.putAll(buildPython(null, true, numericPrefix, folder, noWorky, currentDepth + 1, splitLevel, overwrite, maxDepth));
            }
          }
          firstTime = false;
        } // for (String peer : peers.keySet())

      } // if (maxDepth == null || maxDepth < currentDepth)

      String prefix = "";
      if (numericPrefix) {
        prefix = String.format("%02d_", si.getCreationOrder());
      }

      if (si.getName().equals("i01")) {
        log.info("here");
      }

      // need to group the def release
      content.append(toDefaultReleasePython(si, currentDepth <= splitLevel));

      // if multiFile
      if (currentDepth <= splitLevel) {
        Files.write(new File(folder + File.separator + prefix + safeRefName(si) + "_config.py").toPath(), content.toString().getBytes());
        serviceFileWritten.put(si.getCreationOrder(), si.getName());
      }

      log.info("{}", si.getName());
      content = new StringBuilder();
    } // for each service

    // release

    // FIXME - should be void ?
    // conditional write launch file ...
    return serviceFileWritten;
  }

  @Override
  public String toPython(ServiceInterface si) {
    return toDefaultStartPython(si, true);
  }

}
