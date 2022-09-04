package org.myrobotlab.lang.yml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

// service life-cycle

// 2 stage - generate meta data / meta-"code"
public class LangYmlUtils {

  transient public final static Logger log = LoggerFactory.getLogger(LangYmlUtils.class);

  // options :
  // force overwrite - default do not overwrite
  // "launch.yml" is the interface - it only saves launch.yml

  static public String toYml(StringBuilder content, String folder, String names, Boolean overwrite, Boolean groupByPeer) throws IOException {

    // defaults

    if (content == null) {
      content = new StringBuilder();
    }

    if (folder == null) {
      folder = Runtime.getInstance().getConfigDir() + File.separator + "default";
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
      DumperOptions dumperOptions = new DumperOptions();
      dumperOptions.setPrettyFlow(true);
      // dumperOptions.setDefaultFlowStyle(BLOCK);
      // dumperOptions.setAllowReadOnlyProperties(false);
      /*
       * You can use YAML.setBeanAccess(BeanAccess.FIELDS), but then neither
       * setter nor getter is used (only field access). It's not always
       * suitable.
       */

      Yaml yaml = new Yaml(dumperOptions);
      // yaml.setBeanAccess(BeanAccess.FIELD);
      String yml = yaml.dump(si);
      content.append(yml);

      if (groupByPeer) {
        // if running peers - append and exclude ...
      }

      // if multiFile
      Files.write(new File(folder + File.separator + si.getName() + ".yml").toPath(), content.toString().getBytes());

      log.info("{}", si.getName());
    }

    // conditional write launch file ...

    return null;
  }

}
