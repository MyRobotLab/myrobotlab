package org.myrobotlab.config;

import java.io.File;
import java.io.IOException;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.CmdOptions;
import org.myrobotlab.framework.StartYml;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.config.RuntimeConfig;
import org.slf4j.Logger;

/**
 * Class to process basic configuration functions and processing.
 * 
 * @author GroG
 *
 */
public class ConfigUtils {

  public final static Logger log = LoggerFactory.getLogger(Runtime.class);

  private static RuntimeConfig config;

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
    if (config == null) {
      loadRuntimeConfig(null);
    }
    return config.resource;

  }

  /**
   * Loads a runtime config based on the configName. config =
   * data/config/{configName}/runtime.yml If one does exits, it is returned, if
   * one does not exist a default one is created and saved.
   * 
   * @param configName
   * @return
   */
  static public RuntimeConfig loadRuntimeConfig(CmdOptions options) {

    if (config != null) {
      return config;
    }

    StartYml startYml = loadStartYml();
    String configName = null;

    if (startYml.enable) {
      configName = startYml.config;
    } else {
      configName = "default";
    }

    // start with default
    config = new RuntimeConfig();
    try {

      File runtimeYml = new File(Runtime.ROOT_CONFIG_DIR + File.separator + configName + File.separator + "runtime.yml");
      if (runtimeYml.exists()) {
        // parse that file look for resource: entry in file
        config = (RuntimeConfig) CodecUtils.readServiceConfig(runtimeYml.getAbsolutePath());
      } else {
        FileIO.toFile(runtimeYml, CodecUtils.toYaml(config).getBytes());
      }

    } catch (IOException e) {
      log.error("loadRuntimeConfig threw", e);
    }

    if (options != null && options.id != null) {
      config.id = options.id;
    }

    return config;
  }

  public static StartYml loadStartYml() {
    StartYml startYml = new StartYml();
    String defaultStartFile = CodecUtils.toYaml(startYml);
    File checkStartYml = new File("start.yml");
    if (!checkStartYml.exists()) {
      // save default start.yml
      startYml = new StartYml();
      try {
        FileIO.toFile("start.yml", defaultStartFile);
      } catch (IOException e) {
        log.error("could not save start.yml", e);
      }
    } else {
      // load start.yml
      try {
        String yml = FileIO.toString("start.yml");
        startYml = CodecUtils.fromYaml(yml, StartYml.class);
      } catch (Exception e) {
        log.error("could not load start.yml replacing with new start.yml", e);
        startYml = new StartYml();
        try {
          FileIO.toFile("start.yml", defaultStartFile);
        } catch (IOException ex) {
          log.error("could not save start.yml", ex);
        }
      }
    }
    log.info("start.yml exists {} {}", checkStartYml.exists(), CodecUtils.toJson(startYml));
    return startYml;
  }

  public static String getId() {
    if (config == null) {
      loadRuntimeConfig(null);
    }
    return config.id;
  }

  /**
   * If Runtime.releaseAll is called the statics here should be reset
   */
  public static void reset() {
    config = null;
  }

}
