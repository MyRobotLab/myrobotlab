package org.myrobotlab.inmoov;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/*
 * InMoov Language default en-EN language vars
 * Override by load
 */
public class LanguagePack {
  public final static Logger log = LoggerFactory.getLogger(LanguagePack.class);
  public static LinkedHashMap<String, String> lpVars = new LinkedHashMap<String, String>();

  public static void load(String locale, String intanceName) throws IOException {

    load("InMoov2" + File.separator + "languagePack", locale, intanceName);
  }

  public static void load(String directory, String locale, String intanceName) throws IOException {

    String file = directory + File.separator + locale + File.separator + "system.txt";

    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));

    for (String line = br.readLine(); line != null; line = br.readLine()) {
      log.info("Inmoov languagePack load : {}", line);
      String[] parts = line.split("=");
      lpVars.put(parts[0], parts[1]);
    }

  }

  public static String get(String param) {
    if (lpVars.containsKey(param)) {
      return lpVars.get(param);
    }
    return "not translated";

  }

}
