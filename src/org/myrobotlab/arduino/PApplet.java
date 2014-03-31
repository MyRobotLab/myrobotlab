package org.myrobotlab.arduino;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class PApplet {
	/** Path to sketch folder */
	public String sketchPath; // folder;

	public static int platform;
	static final String WHITESPACE = " \t\n\r\f\u00A0";

	static protected HashMap<String, Pattern> matchPatterns;

	static public String[] loadStrings(File file) {
		InputStream is = createInput(file);
		if (is != null)
			return loadStrings(is);
		return null;
	}

	static public InputStream createInput(File file) {
		if (file == null) {
			throw new IllegalArgumentException("File passed to createInput() was null");
		}
		try {
			InputStream input = new FileInputStream(file);
			if (file.getName().toLowerCase().endsWith(".gz")) {
				return new GZIPInputStream(input);
			}
			return input;

		} catch (IOException e) {
			System.err.println("Could not createInput() for " + file);
			e.printStackTrace();
			return null;
		}
	}

	static final public String[] str(int x[]) {
		String s[] = new String[x.length];
		for (int i = 0; i < x.length; i++)
			s[i] = String.valueOf(x[i]);
		return s;
	}

	static public String join(String[] list, char separator) {
		return join(list, String.valueOf(separator));
	}

	static public String join(String[] list, String separator) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < list.length; i++) {
			if (i != 0)
				buffer.append(separator);
			buffer.append(list[i]);
		}
		return buffer.toString();
	}

	static public String[] split(String value, char delim) {
		// do this so that the exception occurs inside the user's
		// program, rather than appearing to be a bug inside split()
		if (value == null)
			return null;
		// return split(what, String.valueOf(delim)); // huh

		char chars[] = value.toCharArray();
		int splitCount = 0; // 1;
		for (int i = 0; i < chars.length; i++) {
			if (chars[i] == delim)
				splitCount++;
		}
		// make sure that there is something in the input string
		// if (chars.length > 0) {
		// if the last char is a delimeter, get rid of it..
		// if (chars[chars.length-1] == delim) splitCount--;
		// on second thought, i don't agree with this, will disable
		// }
		if (splitCount == 0) {
			String splits[] = new String[1];
			splits[0] = new String(value);
			return splits;
		}
		// int pieceCount = splitCount + 1;
		String splits[] = new String[splitCount + 1];
		int splitIndex = 0;
		int startIndex = 0;
		for (int i = 0; i < chars.length; i++) {
			if (chars[i] == delim) {
				splits[splitIndex++] = new String(chars, startIndex, i - startIndex);
				startIndex = i + 1;
			}
		}
		// if (startIndex != chars.length) {
		splits[splitIndex] = new String(chars, startIndex, chars.length - startIndex);
		// }
		return splits;
	}

	static public String[] split(String value, String delim) {
		ArrayList<String> items = new ArrayList<String>();
		int index;
		int offset = 0;
		while ((index = value.indexOf(delim, offset)) != -1) {
			items.add(value.substring(offset, index));
			offset = index + delim.length();
		}
		items.add(value.substring(offset));
		String[] outgoing = new String[items.size()];
		items.toArray(outgoing);
		return outgoing;
	}

	public String sketchPath(String where) {
		if (sketchPath == null) {
			return where;
			// throw new RuntimeException("The applet was not inited properly, "
			// +
			// "or security restrictions prevented " +
			// "it from determining its path.");
		}
		// isAbsolute() could throw an access exception, but so will writing
		// to the local disk using the sketch path, so this is safe here.
		// for 0120, added a try/catch anyways.
		try {
			if (new File(where).isAbsolute())
				return where;
		} catch (Exception e) {
		}

		return sketchPath + File.separator + where;
	}

	public String savePath(String where) {
		if (where == null)
			return null;
		String filename = sketchPath(where);
		createPath(filename);
		return filename;
	}

	/**
	 * Identical to savePath(), but returns a File object.
	 */
	public File saveFile(String where) {
		return new File(savePath(where));
	}

	public OutputStream createOutput(String filename) {
		return createOutput(saveFile(filename));
	}

	/**
	 * @nowebref
	 */
	static public OutputStream createOutput(File file) {
		try {
			createPath(file); // make sure the path exists
			FileOutputStream fos = new FileOutputStream(file);
			if (file.getName().toLowerCase().endsWith(".gz")) {
				return new GZIPOutputStream(fos);
			}
			return fos;

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	static public void createPath(String path) {
		createPath(new File(path));
	}

	static public void createPath(File file) {
		try {
			String parent = file.getParent();
			if (parent != null) {
				File unit = new File(parent);
				if (!unit.exists())
					unit.mkdirs();
			}
		} catch (SecurityException se) {
			System.err.println("You don't have permissions to create " + file.getAbsolutePath());
		}
	}

	/**
	 * ( begin auto-generated from saveStream.xml )
	 * 
	 * Save the contents of a stream to a file in the sketch folder. This is
	 * basically <b>saveBytes(blah, loadBytes())</b>, but done more efficiently
	 * (and with less confusing syntax).<br />
	 * <br />
	 * When using the <b>targetFile</b> parameter, it writes to a <b>File</b>
	 * object for greater control over the file location. (Note that unlike some
	 * other functions, this will not automatically compress or uncompress gzip
	 * files.)
	 * 
	 * ( end auto-generated )
	 * 
	 * @webref output:files
	 * @param target
	 *            name of the file to write to
	 * @param source
	 *            location to read from (a filename, path, or URL)
	 * @see PApplet#createOutput(String)
	 */
	public boolean saveStream(String target, String source) {
		return saveStream(saveFile(target), source);
	}

	/**
	 * Identical to the other saveStream(), but writes to a File object, for
	 * greater control over the file location.
	 * <p/>
	 * Note that unlike other api methods, this will not automatically compress
	 * or uncompress gzip files.
	 */
	public boolean saveStream(File target, String source) {
		return saveStream(target, createInputRaw(source));
	}

	/**
	 * @nowebref
	 */
	public boolean saveStream(String target, InputStream source) {
		return saveStream(saveFile(target), source);
	}

	/**
	 * @nowebref
	 */
	static public boolean saveStream(File target, InputStream source) {
		File tempFile = null;
		try {
			File parentDir = target.getParentFile();
			// make sure that this path actually exists before writing
			createPath(target);
			tempFile = File.createTempFile(target.getName(), null, parentDir);
			FileOutputStream targetStream = new FileOutputStream(tempFile);

			saveStream(targetStream, source);
			targetStream.close();
			targetStream = null;

			if (target.exists()) {
				if (!target.delete()) {
					System.err.println("Could not replace " + target.getAbsolutePath() + ".");
				}
			}
			if (!tempFile.renameTo(target)) {
				System.err.println("Could not rename temporary file " + tempFile.getAbsolutePath());
				return false;
			}
			return true;

		} catch (IOException e) {
			if (tempFile != null) {
				tempFile.delete();
			}
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * @nowebref
	 */
	static public void saveStream(OutputStream target, InputStream source) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(source, 16384);
		BufferedOutputStream bos = new BufferedOutputStream(target);

		byte[] buffer = new byte[8192];
		int bytesRead;
		while ((bytesRead = bis.read(buffer)) != -1) {
			bos.write(buffer, 0, bytesRead);
		}

		bos.flush();
	}

	static public String[] splitTokens(String value) {
		return splitTokens(value, WHITESPACE);
	}

	static public String[] splitTokens(String value, String delim) {
		StringTokenizer toker = new StringTokenizer(value, delim);
		String pieces[] = new String[toker.countTokens()];

		int index = 0;
		while (toker.hasMoreTokens()) {
			pieces[index++] = toker.nextToken();
		}
		return pieces;
	}

	static public final int constrain(int amt, int low, int high) {
		return (amt < low) ? low : ((amt > high) ? high : amt);
	}

	static public String[] loadStrings(InputStream input) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
			return loadStrings(reader);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	static public String[] loadStrings(BufferedReader reader) {
		try {
			String lines[] = new String[100];
			int lineCount = 0;
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (lineCount == lines.length) {
					String temp[] = new String[lineCount << 1];
					System.arraycopy(lines, 0, temp, 0, lineCount);
					lines = temp;
				}
				lines[lineCount++] = line;
			}
			reader.close();

			if (lineCount == lines.length) {
				return lines;
			}

			// resize array to appropriate amount for these lines
			String output[] = new String[lineCount];
			System.arraycopy(lines, 0, output, 0, lineCount);
			return output;

		} catch (IOException e) {
			e.printStackTrace();
			// throw new RuntimeException("Error inside loadStrings()");
		}
		return null;
	}

	public PrintWriter createWriter(String filename) {
		return createWriter(saveFile(filename));
	}

	static public PrintWriter createWriter(File file) {
		try {
			createPath(file); // make sure in-between folders exist
			OutputStream output = new FileOutputStream(file);
			if (file.getName().toLowerCase().endsWith(".gz")) {
				output = new GZIPOutputStream(output);
			}
			return createWriter(output);

		} catch (Exception e) {
			if (file == null) {
				throw new RuntimeException("File passed to createWriter() was null");
			} else {
				e.printStackTrace();
				throw new RuntimeException("Couldn't create a writer for " + file.getAbsolutePath());
			}
		}
		// return null;
	}

	static public PrintWriter createWriter(OutputStream output) {
		try {
			BufferedOutputStream bos = new BufferedOutputStream(output, 8192);
			OutputStreamWriter osw = new OutputStreamWriter(bos, "UTF-8");
			return new PrintWriter(osw);
		} catch (UnsupportedEncodingException e) {
		} // not gonna happen
		return null;
	}

	static final public String hex(byte value) {
		return hex(value, 2);
	}

	static final public String hex(char value) {
		return hex(value, 4);
	}

	static final public String hex(int value) {
		return hex(value, 8);
	}

	/**
	 * @param digits
	 *            the number of digits (maximum 8)
	 */
	static final public String hex(int value, int digits) {
		String stuff = Integer.toHexString(value).toUpperCase();
		if (digits > 8) {
			digits = 8;
		}

		int length = stuff.length();
		if (length > digits) {
			return stuff.substring(length - digits);

		} else if (length < digits) {
			return "00000000".substring(8 - (digits - length)) + stuff;
		}
		return stuff;
	}

	static public Object subset(Object list, int start) {
		int length = Array.getLength(list);
		return subset(list, start, length - start);
	}

	static public Object subset(Object list, int start, int count) {
		Class<?> type = list.getClass().getComponentType();
		Object outgoing = Array.newInstance(type, count);
		System.arraycopy(list, start, outgoing, 0, count);
		return outgoing;
	}

	static public String[] expand(String list[], int newSize) {
		String temp[] = new String[newSize];
		// in case the new size is smaller than list.length
		System.arraycopy(list, 0, temp, 0, Math.min(newSize, list.length));
		return temp;
	}

	static public String[] append(String array[], String value) {
		array = expand(array, array.length + 1);
		array[array.length - 1] = value;
		return array;
	}

	static public Object append(Object array, Object value) {
		int length = Array.getLength(array);
		array = expand(array, length + 1);
		Array.set(array, length, value);
		return array;
	}

	static public Object expand(Object array) {
		return expand(array, Array.getLength(array) << 1);
	}

	static public Object expand(Object list, int newSize) {
		Class<?> type = list.getClass().getComponentType();
		Object temp = Array.newInstance(type, newSize);
		System.arraycopy(list, 0, temp, 0, Math.min(Array.getLength(list), newSize));
		return temp;
	}

	static public Object shorten(Object list) {
		int length = Array.getLength(list);
		return subset(list, 0, length - 1);
	}

	static public String[] match(String str, String regexp) {
		Pattern p = matchPattern(regexp);
		Matcher m = p.matcher(str);
		if (m.find()) {
			int count = m.groupCount() + 1;
			String[] groups = new String[count];
			for (int i = 0; i < count; i++) {
				groups[i] = m.group(i);
			}
			return groups;
		}
		return null;
	}

	static Pattern matchPattern(String regexp) {
		Pattern p = null;
		if (matchPatterns == null) {
			matchPatterns = new HashMap<String, Pattern>();
		} else {
			p = matchPatterns.get(regexp);
		}
		if (p == null) {
			if (matchPatterns.size() == 10) {
				// Just clear out the match patterns here if more than 10 are
				// being
				// used. It's not terribly efficient, but changes that you have
				// >10
				// different match patterns are very slim, unless you're doing
				// something really tricky (like custom match() methods), in
				// which
				// case match() won't be efficient anyway. (And you should just
				// be
				// using your own Java code.) The alternative is using a queue
				// here,
				// but that's a silly amount of work for negligible benefit.
				matchPatterns.clear();
			}
			p = Pattern.compile(regexp, Pattern.MULTILINE | Pattern.DOTALL);
			matchPatterns.put(regexp, p);
		}
		return p;
	}

	static public String[][] matchAll(String what, String regexp) {
		Pattern p = matchPattern(regexp);
		Matcher m = p.matcher(what);
		ArrayList<String[]> results = new ArrayList<String[]>();
		int count = m.groupCount() + 1;
		while (m.find()) {
			String[] groups = new String[count];
			for (int i = 0; i < count; i++) {
				groups[i] = m.group(i);
			}
			results.add(groups);
		}
		if (results.isEmpty()) {
			return null;
		}
		String[][] matches = new String[results.size()][count];
		for (int i = 0; i < matches.length; i++) {
			matches[i] = results.get(i);
		}
		return matches;
	}

	public File dataFile(String where) {
		// isAbsolute() could throw an access exception, but so will writing
		// to the local disk using the sketch path, so this is safe here.
		File why = new File(where);
		if (why.isAbsolute())
			return why;

		String jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
		if (jarPath.contains("Contents/Resources/Java/")) {
			File containingFolder = new File(jarPath).getParentFile();
			File dataFolder = new File(containingFolder, "data");
			return new File(dataFolder, where);
		}
		// Windows, Linux, or when not using a Mac OS X .app file
		return new File(sketchPath + File.separator + "data" + File.separator + where);
	}

	public String dataPath(String where) {
		return dataFile(where).getAbsolutePath();
	}

	/**
	 * Call openStream() without automatic gzip decompression.
	 */
	public InputStream createInputRaw(String filename) {
		InputStream stream = null;

		if (filename == null)
			return null;

		if (filename.length() == 0) {
			// an error will be called by the parent function
			// System.err.println("The filename passed to openStream() was empty.");
			return null;
		}

		// safe to check for this as a url first. this will prevent online
		// access logs from being spammed with GET /sketchfolder/http://blahblah
		if (filename.indexOf(":") != -1) { // at least smells like URL
			try {
				URL url = new URL(filename);
				stream = url.openStream();
				return stream;

			} catch (MalformedURLException mfue) {
				// not a url, that's fine

			} catch (FileNotFoundException fnfe) {
				// Java 1.5 likes to throw this when URL not available. (fix for
				// 0119)
				// http://dev.processing.org/bugs/show_bug.cgi?id=403

			} catch (IOException e) {
				// changed for 0117, shouldn't be throwing exception
				e.printStackTrace();
				// System.err.println("Error downloading from URL " + filename);
				return null;
				// throw new RuntimeException("Error downloading from URL " +
				// filename);
			}
		}

		// Moved this earlier than the getResourceAsStream() checks, because
		// calling getResourceAsStream() on a directory lists its contents.
		// http://dev.processing.org/bugs/show_bug.cgi?id=716
		try {
			// First see if it's in a data folder. This may fail by throwing
			// a SecurityException. If so, this whole block will be skipped.
			File file = new File(dataPath(filename));
			if (!file.exists()) {
				// next see if it's just in the sketch folder
				file = new File(sketchPath, filename);
			}
			if (file.isDirectory()) {
				return null;
			}
			if (file.exists()) {
				try {
					// handle case sensitivity check
					String filePath = file.getCanonicalPath();
					String filenameActual = new File(filePath).getName();
					// make sure there isn't a subfolder prepended to the name
					String filenameShort = new File(filename).getName();
					// if the actual filename is the same, but capitalized
					// differently, warn the user.
					// if (filenameActual.equalsIgnoreCase(filenameShort) &&
					// !filenameActual.equals(filenameShort)) {
					if (!filenameActual.equals(filenameShort)) {
						throw new RuntimeException("This file is named " + filenameActual + " not " + filename + ". Rename the file " + "or change your code.");
					}
				} catch (IOException e) {
				}
			}

			// if this file is ok, may as well just load it
			stream = new FileInputStream(file);
			if (stream != null)
				return stream;

			// have to break these out because a general Exception might
			// catch the RuntimeException being thrown above
		} catch (IOException ioe) {
		} catch (SecurityException se) {
		}

		// Using getClassLoader() prevents java from converting dots
		// to slashes or requiring a slash at the beginning.
		// (a slash as a prefix means that it'll load from the root of
		// the jar, rather than trying to dig into the package location)
		ClassLoader cl = getClass().getClassLoader();

		// by default, data files are exported to the root path of the jar.
		// (not the data folder) so check there first.
		stream = cl.getResourceAsStream("data/" + filename);
		if (stream != null) {
			String cn = stream.getClass().getName();
			// this is an irritation of sun's java plug-in, which will return
			// a non-null stream for an object that doesn't exist. like all good
			// things, this is probably introduced in java 1.5. awesome!
			// http://dev.processing.org/bugs/show_bug.cgi?id=359
			if (!cn.equals("sun.plugin.cache.EmptyInputStream")) {
				return stream;
			}
		}

		// When used with an online script, also need to check without the
		// data folder, in case it's not in a subfolder called 'data'.
		// http://dev.processing.org/bugs/show_bug.cgi?id=389
		stream = cl.getResourceAsStream(filename);
		if (stream != null) {
			String cn = stream.getClass().getName();
			if (!cn.equals("sun.plugin.cache.EmptyInputStream")) {
				return stream;
			}
		}

		// Finally, something special for the Internet Explorer users. Turns out
		// that we can't get files that are part of the same folder using the
		// methods above when using IE, so we have to resort to the old skool
		// getDocumentBase() from teh applet dayz. 1996, my brotha.
		try {
			// URL base = getDocumentBase(); Not going to pull in Applet
			URL base = null;
			if (base != null) {
				URL url = new URL(base, filename);
				URLConnection conn = url.openConnection();
				return conn.getInputStream();
				// if (conn instanceof HttpURLConnection) {
				// HttpURLConnection httpConnection = (HttpURLConnection) conn;
				// // test for 401 result (HTTP only)
				// int responseCode = httpConnection.getResponseCode();
				// }
			}
		} catch (Exception e) {
		} // IO or NPE or...

		// Now try it with a 'data' subfolder. getting kinda desperate for
		// data...
		try {
			URL base = null;// getDocumentBase();
			if (base != null) {
				URL url = new URL(base, "data/" + filename);
				URLConnection conn = url.openConnection();
				return conn.getInputStream();
			}
		} catch (Exception e) {
		}

		try {
			// attempt to load from a local file, used when running as
			// an application, or as a signed applet
			try { // first try to catch any security exceptions
				try {
					stream = new FileInputStream(dataPath(filename));
					if (stream != null)
						return stream;
				} catch (IOException e2) {
				}

				try {
					stream = new FileInputStream(sketchPath(filename));
					if (stream != null)
						return stream;
				} catch (Exception e) {
				} // ignored

				try {
					stream = new FileInputStream(filename);
					if (stream != null)
						return stream;
				} catch (IOException e1) {
				}

			} catch (SecurityException se) {
			} // online, whups

		} catch (Exception e) {
			// die(e.getMessage(), e);
			e.printStackTrace();
		}

		return null;
	}

	public void saveStrings(String filename, String data[]) {
		saveStrings(saveFile(filename), data);
	}

	/**
	 * @nowebref
	 */
	static public void saveStrings(File file, String data[]) {
		saveStrings(createOutput(file), data);
	}

	/**
	 * @nowebref
	 */
	static public void saveStrings(OutputStream output, String[] data) {
		PrintWriter writer = createWriter(output);
		for (int i = 0; i < data.length; i++) {
			writer.println(data[i]);
		}
		writer.flush();
		writer.close();
	}

	/**
	 * Parse a String into an int value. Returns 0 if the value is bad.
	 */
	static final public int parseInt(String what) {
		return parseInt(what, 0);
	}

	/**
	 * Parse a String to an int, and provide an alternate value that should be
	 * used when the number is invalid.
	 */
	static final public int parseInt(String what, int otherwise) {
		try {
			int offset = what.indexOf('.');
			if (offset == -1) {
				return Integer.parseInt(what);
			} else {
				return Integer.parseInt(what.substring(0, offset));
			}
		} catch (NumberFormatException e) {
		}
		return otherwise;
	}

	// . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

	static final public int[] parseInt(boolean what[]) {
		int list[] = new int[what.length];
		for (int i = 0; i < what.length; i++) {
			list[i] = what[i] ? 1 : 0;
		}
		return list;
	}

	static public String[] nf(int num[], int digits) {
		String formatted[] = new String[num.length];
		for (int i = 0; i < formatted.length; i++) {
			formatted[i] = num[i] + "";// nf(num[i], digits);
		}
		return formatted;
	}

	/**
	 * Integer number formatter.
	 */
	static private NumberFormat int_nf;
	static private int int_nf_digits;
	static private boolean int_nf_commas;

	static public String nf(int num, int digits) {

		if ((int_nf != null) && (int_nf_digits == digits) && !int_nf_commas) {
			return int_nf.format(num);
		}

		int_nf = NumberFormat.getInstance();
		int_nf.setGroupingUsed(false); // no commas
		int_nf_commas = false;
		int_nf.setMinimumIntegerDigits(digits);
		int_nf_digits = digits;
		return int_nf.format(num);

	}

	static public int[] parseInt(String what[]) {
		return parseInt(what, 0);
	}

	static public int[] parseInt(String what[], int missing) {
		int output[] = new int[what.length];
		for (int i = 0; i < what.length; i++) {
			try {
				output[i] = Integer.parseInt(what[i]);
			} catch (NumberFormatException e) {
				output[i] = missing;
			}
		}
		return output;
	}

	static public int[] expand(int list[]) {
		return expand(list, list.length << 1);
	}

	static public int[] expand(int list[], int newSize) {
		int temp[] = new int[newSize];
		System.arraycopy(list, 0, temp, 0, Math.min(newSize, list.length));
		return temp;
	}

	static public char[] subset(char list[], int start) {
		return subset(list, start, list.length - start);
	}

	static public char[] subset(char list[], int start, int count) {
		char output[] = new char[count];
		System.arraycopy(list, start, output, 0, count);
		return output;
	}

	static final public int unhex(String value) {
		// has to parse as a Long so that it'll work for numbers bigger than
		// 2^31
		return (int) (Long.parseLong(value, 16));
	}

}
