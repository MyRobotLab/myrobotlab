package org.myrobotlab.document.transformer;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import au.com.bytecode.opencsv.CSVReader;

/**
 * A singleton class to load a dictionary into the jvm that can be used across
 * multiple instances of a pipeline sage.
 * 
 * @author kwatters
 *
 */
public class DictionaryLoader {
	private static DictionaryLoader instance = null;

	private HashMap<String, HashMap<String,String>> dictMap;

	protected DictionaryLoader() {
		// Exists only to defeat instantiation.
	}
	public static DictionaryLoader getInstance() {
		if(instance == null) {
			instance = new DictionaryLoader();
		}
		return instance;
	}

	public synchronized HashMap<String,String> loadDictionary(String fileName) throws IOException {
		// it's already loaded, just return it
		if (dictMap.containsKey(fileName)) {
			return dictMap.get(fileName);
		}
		// it's not loaded , load the file and put it in the dict map and return
		// assume the file is a csv file with key/value pairs on each line
		HashMap<String,String> dictionary = new HashMap<String,String>();
		CSVReader reader = new CSVReader(new FileReader(fileName));
		String [] nextLine;
		while ((nextLine = reader.readNext()) != null) {
			// nextLine[] is an array of values from the line
			dictionary.put(nextLine[0], nextLine[1]);
		}
		dictMap.put(fileName, dictionary);
		return dictionary;
	}
}
