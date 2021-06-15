package org.myrobotlab.lang.py;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Instantiator;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceReservation;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.framework.repo.ServiceData;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
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

  public String toDefaultPython(ServiceInterface si, boolean includeHeader) {
    StringBuilder content = new StringBuilder();

    if (includeHeader) {
      content.append("import time\n");
      content.append("import org.myrobotlab.framework.Platform as Platform\n");
      content.append("import org.myrobotlab.service.Runtime as Runtime\n");
      content.append("\n");
      content.append(String.format("print('loading %s of type %s')", si.getName(), si.getSimpleName()));
      content.append("\n");
    }

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


  // options :
  // force overwrite - default do not overwrite
  // "launch.yml" is the interface - it only saves launch.yml

  public String toPython(StringBuilder content, Boolean includeHeader, Boolean numericPrefix, String folder, String names, Integer currentDepth, Integer splitLevel, Boolean overwrite, Integer maxDepth)
      throws IOException {

    // defaults
    if (currentDepth == null) {
      currentDepth = 0;
    }
    
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
      folder = "data" + File.separator + "config" + File.separator + "default";
    }
    
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

      // write module file
      File init = new File(folder + File.separator + "__init__.py");
      if (!init.exists()) {
        Files.write(init.toPath(), "".getBytes());
      }

      // check for custom exporter ...
      StringBuilder newPythonContent = new StringBuilder();
      PythonGenerator generator = getExporter(String.format("org.myrobotlab.lang.py.%sPy", si.getSimpleName()));
      if (generator != null) {
        newPythonContent.append(toDefaultPython(si, includeHeader));
        newPythonContent.append(generator.toPython(si));
      } else {
        newPythonContent.append(toDefaultPython(si, includeHeader));
      }

      content.append(newPythonContent);

      MetaData serviceType = ServiceData.getMetaData(si.getName(), si.getSimpleName());
      Map<String, ServiceReservation> peers = serviceType.getPeers();

      // FIXME - do "indent"
      boolean firstTime = true;
      if (maxDepth == null || maxDepth < currentDepth) {
        for (String peer : peers.keySet()) {
          ServiceReservation sr = peers.get(peer);
          ServiceInterface peerSi = Runtime.getService(sr.actualName);
          if (peerSi != null) {
            if (currentDepth >= splitLevel) {
              // concatenate content
              if (firstTime) {
                content.append(String.format("\n# %s peers\n", si.getSimpleName()));
              }
              toPython(content, false, numericPrefix, folder, sr.actualName, currentDepth + 1, splitLevel, overwrite, maxDepth);
              content.append("\n");
            } else {
              // don't concatenate - split files
              toPython(null, true, numericPrefix, folder, sr.actualName, currentDepth + 1, splitLevel, overwrite, maxDepth);
            }
          }
          firstTime = false;
        } // for (String peer : peers.keySet())

      } // if (maxDepth == null || maxDepth < currentDepth)

      String prefix = "";
      if (numericPrefix) {
        prefix = String.format("%02d_", si.getCreationOrder());
      }
      
      // if multiFile
      if (currentDepth <= splitLevel) {
        Files.write(new File(folder + File.separator + prefix + safeRefName(si) + ".py").toPath(), content.toString().getBytes());
      }

      log.info("{}", si.getName());
      content = new StringBuilder();
    } // for each service

    // conditional write launch file ...

    return null;
  }

  @Override
  public String toPython(ServiceInterface si) {
    return toDefaultPython(si, true);
  }

}
