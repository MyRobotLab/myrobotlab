package org.myrobotlab.config;

import java.io.File;
import java.io.IOException;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.StartYml;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.config.RuntimeConfig;

public class ConfigUtils {

  /**
   * This gets the current resource root without starting a Runtime instance if
   * not already started. The resource root depends on config, if Runtime is
   * running the logic and current config name is already available. If Runtime
   * is not running, we need to go through a series of steps to deterime where
   * the resource root is configured.
   * 
   * @return
   */
  public static String getResourceRoot() {

    String resource = "resource";

    // check if runtime is running
    if (!Runtime.isAvailable()) {
      // check for start.yml

      File checkStartYml = new File("start.yml");
      StartYml startYml = new StartYml();
      if (checkStartYml.exists()) {
        String yml;
        try {
          yml = FileIO.toString("start.yml");
          startYml = CodecUtils.fromYaml(yml, StartYml.class);

          // see if autostart is on with a config
          if (startYml.enable) {
            // use that config to find runtime.yml

            File runtimeYml = new File(Runtime.ROOT_CONFIG_DIR + File.separator + startYml.config + File.separator + "runtime.yml");
            if (runtimeYml.exists()) {
              // parse that file look for resource: entry in file
              RuntimeConfig config = (RuntimeConfig) CodecUtils.readServiceConfig(runtimeYml.getAbsolutePath());
              resource = config.resource;
            }

          } else {
            // start.yml enable = false / so we'll use default config
            File runtimeYml = new File(Runtime.ROOT_CONFIG_DIR + File.separator + "default" + File.separator + "runtime.yml");
            if (runtimeYml.exists()) {
              // parse that file look for resource: entry in file
              RuntimeConfig config = (RuntimeConfig) CodecUtils.readServiceConfig(runtimeYml.getAbsolutePath());
              resource = config.resource;
            }
          }

        } catch (IOException e) {
          // problem getting or parsing
          // going to assume default "resource"
        }
      } // no startYml
      return resource;
    } else {
      // Runtime is available - ask it
      return Runtime.getInstance().getConfig().resource;
    }
  }
}
