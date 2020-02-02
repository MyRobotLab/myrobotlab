package org.myrobotlab.inmoov;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;

import org.apache.commons.io.FilenameUtils;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/*
 * InMoov Language pack
 */
public class LanguagePack {
  public final static Logger log = LoggerFactory.getLogger(LanguagePack.class);
  public static LinkedHashMap<String, String> lpVars = new LinkedHashMap<String, String>();

  /*
   * iterate over each txt files in the directory
   */
  public void load(String locale) {
    String extension = "lang";
    File dir = Utils.makeDirectory("InMoov" + File.separator + "system" + File.separator + "languagePack" + File.separator + locale);
    if (dir.exists()) { 
      lpVars.clear();
      for (File f : dir.listFiles()) {
        if (f.isDirectory()) {
          continue;
        }
        if (FilenameUtils.getExtension(f.getAbsolutePath()).equalsIgnoreCase(extension)) {
          log.info("Inmoov languagePack load : {}", f.getName());
          try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
            for (String line = br.readLine(); line != null; line = br.readLine()) {
              String[] parts = line.split("::");
              if (parts.length > 1) {
                lpVars.put(parts[0].toUpperCase(), parts[1]);
              }
            }
          } catch (IOException e) {
            log.error("LanguagePack : {}", e);
          }
        } else {
          log.warn("{} is not a {} file", f.getAbsolutePath(), extension);
        }
      }
    }
  }

  public String get(String param) {
    if (lpVars.containsKey(param.toUpperCase())) {
      return lpVars.get(param.toUpperCase());
    }
    return "not yet translated";

  }
}
