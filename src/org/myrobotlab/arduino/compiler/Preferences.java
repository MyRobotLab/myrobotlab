/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
 Part of the Processing project - http://processing.org

 Copyright (c) 2004-09 Ben Fry and Casey Reas
 Copyright (c) 2001-04 Massachusetts Institute of Technology

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.myrobotlab.arduino.compiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.myrobotlab.arduino.PApplet;
import org.myrobotlab.service.Arduino;

/**
 * Storage class for user preferences and environment settings.
 * <P>
 * This class no longer uses the Properties class, since properties files are
 * iso8859-1, which is highly likely to be a problem when trying to save sketch
 * folders and locations.
 * <p>
 * The GUIService portion in here is really ugly, as it uses exact layout. This was
 * done in frustration one evening (and pre-Swing), but that's long since past,
 * and it should all be moved to a proper swing layout like BoxLayout.
 * <p>
 * This is very poorly put together, that the preferences panel and the actual
 * preferences i/o is part of the same code. But there hasn't yet been a
 * compelling reason to bother with the separation aside from concern about
 * being lectured by strangers who feel that it doesn't look like what they
 * learned in CS class.
 * <p>
 * Would also be possible to change this to use the Java Preferences API. Some
 * useful articles <a
 * href="http://www.onjava.com/pub/a/onjava/synd/2001/10/17/j2se.html">here</a>
 * and <a href=
 * "http://www.particle.kth.se/~lindsey/JavaCourse/Book/Part1/Java/Chapter10/Preferences.html"
 * >here</a>. However, haven't implemented this yet for lack of time, but more
 * importantly, because it would entail writing to the registry (on Windows), or
 * an obscure file location (on Mac OS X) and make it far more difficult to find
 * the preferences to tweak them by hand (no! stay out of regedit!) or to reset
 * the preferences by simply deleting the preferences.txt file.
 * 
 * uhhh... Processing is an awesome project.. This class is an abomination...
 * Arduino is an awesome project.. it's IDE code base is a large dung pile...
 * 
 * This should use Properties again - using Resource bundles would be an
 * appropriate way to manage UTF-8 / Unicode
 * 
 */
public class Preferences implements Serializable {

	private static final long serialVersionUID = 1L;
	// final String PREFS_FILE = "preferences.txt";
	Hashtable<String, Object> defaults;
	Hashtable<String, Object> table = new Hashtable<String, Object>();;
	File preferencesFile;

	public Preferences(String filename, String commandLinePrefs) {
		// start by loading the defaults, in case something
		// important was deleted from the user prefs
		try {
			load(Arduino.getLibStream("preferences.txt"));
		} catch (Exception e) {
			Arduino.showError(null, "Could not read default settings.\n" + "You'll need to reinstall Arduino.", e);
		}

		// clone the hash table
		defaults = (Hashtable<String, Object>) table.clone();

		// other things that have to be set explicitly for the defaults

		// Load a prefs file if specified on the command line
		if (commandLinePrefs != null) {
			try {
				load(new FileInputStream(commandLinePrefs));

			} catch (Exception poe) {
				Arduino.showError("Error", "Could not read preferences from " + commandLinePrefs, poe);
			}
		} else if (!Arduino.isCommandLine()) {
			// next load user preferences file
			preferencesFile = getSettingsFile(filename);
			if (!preferencesFile.exists()) {
				// create a new preferences file if none exists
				// saves the defaults out to the file
				save();

			} else {
				// load the previous preferences file

				try {
					load(new FileInputStream(preferencesFile));

				} catch (Exception ex) {
					Arduino.showError("Error reading preferences", "Error reading the preferences file. " + "Please delete (or move)\n" + preferencesFile.getAbsolutePath()
							+ " and restart Arduino.", ex);
				}
			}
		}
	}

	public File getSettingsFile(String filename) {
		return new File(getSettingsFolder(), filename);
	}

	public File getSettingsFolder() {
		File settingsFolder = null;

		String preferencesPath = get("settings.path");
		if (preferencesPath != null) {
			settingsFolder = new File(preferencesPath);

		} else {
			try {
				settingsFolder = new File(".myrobotlab");// platform.getSettingsFolder();
			} catch (Exception e) {
				Arduino.showError("Problem getting data folder", "Error getting the Arduino data folder.", e);
			}
		}

		// create the folder if it doesn't exist already
		if (!settingsFolder.exists()) {
			if (!settingsFolder.mkdirs()) {
				Arduino.showError("Settings issues", "Arduino cannot run because it could not\n" + "create a folder to store your settings.", null);
			}
		}
		return settingsFolder;
	}

	/**
	 * Change internal settings based on what was chosen in the prefs, then send
	 * a message to the editor saying that it's time to do the same.
	 */
	protected void applyFrame() {
		// put each of the settings into the table
		setBoolean("build.verbose", true);
		setBoolean("upload.verbose", true);

		String oldPath = get("sketchbook.path");
		set("sketchbook.path", ".myrobotlab");

		setBoolean("editor.external", false);
		setBoolean("update.check", false);

		setBoolean("editor.update_extension", true);

	}

	protected void load(InputStream input) throws IOException {
		load(input, table);
	}

	public void load(InputStream input, Map table) throws IOException {
		String[] lines = PApplet.loadStrings(input); // Reads as UTF-8
		for (String line : lines) {
			if ((line.length() == 0) || (line.charAt(0) == '#'))
				continue;

			// this won't properly handle = signs being in the text
			int equals = line.indexOf('=');
			if (equals != -1) {
				String key = line.substring(0, equals).trim();
				String value = line.substring(equals + 1).trim();
				table.put(key, value);
			}
		}
	}

	// .................................................................

	public void save() {

		if (preferencesFile == null)
			return;

		PrintWriter writer = PApplet.createWriter(preferencesFile);

		Enumeration e = table.keys();
		while (e.hasMoreElements()) {
			String key = (String) e.nextElement();
			writer.println(key + "=" + ((String) table.get(key)));
		}

		writer.flush();
		writer.close();

	}

	// .................................................................

	public String get(String attribute /* , String defaultValue */) {
		return (String) table.get(attribute);

	}

	public String getDefault(String attribute) {
		return (String) defaults.get(attribute);
	}

	public void set(String attribute, String value) {
		table.put(attribute, value);
	}

	public void unset(String attribute) {
		table.remove(attribute);
	}

	public boolean getBoolean(String attribute) {
		String value = get(attribute); // , null);
		return (new Boolean(value)).booleanValue();

	}

	public void setBoolean(String attribute, boolean value) {
		set(attribute, value ? "true" : "false");
	}

	public int getInteger(String attribute /* , int defaultValue */) {
		return Integer.parseInt(get(attribute));

	}

	public void setInteger(String key, int value) {
		set(key, String.valueOf(value));
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		// Properties props = new Properties();
		// props.load(new FileInputStream("arduino/lib/preferences.txt"));
		// props.save(new FileOutputStream("arduino.properties"),
		// "latest and greatest");

		String propFile = "arduino/lib/preferences.txt";
		Properties props = new Properties();
		/* set some properties here */
		props.load(new FileInputStream("arduino/lib/preferences.txt"));

		// new TreeMap(props);

		FileOutputStream fos = new FileOutputStream("sorted.properties");
		OutputStreamWriter out = new OutputStreamWriter(fos);

		List<String> n = new ArrayList(props.keySet());
		Collections.sort(n);
		for (int i = 0; i < n.size(); ++i) {
			out.write(String.format("%s=%s\n" + "", n.get(i), props.get(n.get(i))));
		}
		out.close();

		// new TreeSet<Object>(super.keySet())

		Properties tmp = new Properties() {

			@Override
			public Set<Object> keySet() {
				return Collections.unmodifiableSet(new TreeSet<Object>(super.keySet()));
			}

		};
		tmp.putAll(props);
		try {
			FileOutputStream xmlStream = new FileOutputStream("arduino.props.xml");
			/* this comes out SORTED! */
			tmp.storeToXML(xmlStream, "");
			tmp.store(new FileOutputStream("arduino.properties"), "latest and greatest");
			// tmp.save(new FileOutputStream("arduino.properties"),
			// "latest and greatest");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
