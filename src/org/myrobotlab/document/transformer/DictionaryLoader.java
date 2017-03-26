package org.myrobotlab.document.transformer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import au.com.bytecode.opencsv.CSVReader;

/**
 * A singleton class to load a dictionary into the jvm that can be used across
 * multiple instances of a pipeline sage.
 * 
 * @author kwatters
 *
 */
public class DictionaryLoader {

  public final static Logger log = LoggerFactory.getLogger(DictionaryLoader.class);
  private static DictionaryLoader instance = null;

  // csvFile -> map-of-values
  private HashMap<String, HashMap<String, List<String>>> dictMap;

  protected DictionaryLoader() {
    // Exists only to defeat instantiation.
    dictMap = new HashMap<String, HashMap<String, List<String>>>();
  }

  public static DictionaryLoader getInstance() {
    if (instance == null) {
      instance = new DictionaryLoader();
    }
    return instance;
  }

  public synchronized HashMap<String, List<String>> loadDictionary(String fileName) throws IOException {
    // it's already loaded, just return it
    if (dictMap.containsKey(fileName)) {
      return dictMap.get(fileName);
    }
    // It's not loaded, load the file and put it in the dict map and return.
    // Assume the file is a csv file with key/value pairs on each line.
    HashMap<String, List<String>> dictionary = new HashMap<String, List<String>>();
    File dictFile = new File(fileName);
    if (!dictFile.exists()) {
      log.warn("Dictionary file not found {}", dictFile.getAbsolutePath());
      return null;
    }
    CSVReader reader = new CSVReader(new FileReader(fileName));
    String[] line;
    while ((line = reader.readNext()) != null) {
      // line[] is an array of values from the line
      List<String> listVal = dictionary.get(line[0]);
      if (listVal == null) {
        listVal = new ArrayList<String>();
        dictionary.put(line[0], listVal);
      }
      for (int i = 1; i < line.length; i++) {
        listVal.add(line[i]);
      }
    }
    dictMap.put(fileName, dictionary);
    reader.close();
    return dictionary;
  }
}
