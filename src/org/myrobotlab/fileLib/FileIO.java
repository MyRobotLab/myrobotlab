/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */
package org.myrobotlab.fileLib;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Set;
import java.util.zip.ZipException;

import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * class of useful utility functions we do not use nio - for portability reasons
 * e.g. Android
 * 
 * @author GroG
 *
 */
public class FileIO {

	public static class FileComparisonException extends Exception {
		private static final long serialVersionUID = 1L;

		public FileComparisonException(String msg) {
			super(msg);
		}
	}

	public final static Logger log = LoggerFactory.getLogger(FileIO.class);

	static public void byteArrayToFile(String filename, byte[] data) throws IOException {
		FileOutputStream fos = null;
		fos = new FileOutputStream(filename);
		fos.write(data);
		fos.close();

	}

	static final public void close(InputStream in, OutputStream out) {
		closeStream(in);
		close(out);
	}

	static final public void close(OutputStream is) {
		try {
			if (is != null) {
				is.close();
			}
		} catch (Exception e) {
		}
	}

	/**
	 * general purpose stream closer for single line closing
	 * 
	 * @param is
	 */
	static final public void closeStream(InputStream is) {
		try {
			if (is != null) {
				is.close();
			}
		} catch (Exception e) {
		}
	}

	public static void compareFiles(String filename1, String filename2) throws FileComparisonException, IOException {
		File file1 = new File(filename1);
		File file2 = new File(filename2);
		if (file1.length() != file2.length()) {
			throw new FileComparisonException(String.format("%s size is %d adn %s is size %d", filename1, file1.length(), filename2, file2.length()));
		}

		byte[] a1 = fileToByteArray(new File(filename1));
		byte[] a2 = fileToByteArray(new File(filename2));

		for (int i = 0; i < a1.length; ++i) {
			if (a1[i] != a2[i]) {
				throw new FileComparisonException(String.format("files differ at position %d", i));
			}
		}
	}

	static public void copy(File src, File dst) throws IOException {
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	static public boolean copy(String from, String to) {
		try {
			byte[] b = fileToByteArray(new File(from));
			byteArrayToFile(to, b);
			return true;
		} catch (Exception e) {
			Logging.logError(e);
		}
		return false;
	}

	// --- string interface end --------------------

	// --- byte[] interface begin ------------------
	// rename getBytes getResourceBytes / String File InputStream

	//
	static public boolean copyResource(String fromFilename, String toFilename) {
		try {
			byte[] b = resourceToByteArray(fromFilename);
			File f = new File(toFilename);
			String path = f.getParent();
			File dir = new File(path);
			if (!dir.exists()) {
				dir.mkdirs();
			}

			byteArrayToFile(toFilename, b);
			return true;
		} catch (Exception e) {
			Logging.logError(e);
		}
		return false;
	}

	public final static byte[] fileToByteArray(File file) throws IOException {

		FileInputStream fis = null;
		byte[] data = null;

		fis = new FileInputStream(file);
		data = toByteArray(fis);

		fis.close();

		return data;
	}

	// --- string interface begin ---
	public final static String fileToString(File file) throws IOException {
		byte[] bytes = fileToByteArray(file);
		if (bytes == null) {
			return null;
		}
		return new String(bytes);
	}

	public final static String fileToString(String filename) throws IOException {
		return fileToString(new File(filename));
	}

	static public String getBinaryPath() {
		return ClassLoader.getSystemClassLoader().getResource(".").getPath();
	}

	/**
	 * get configuration directory
	 * 
	 * @return
	 */
	static public String getCfgDir() {
		try {
			String dirName = String.format("%s%s.myrobotlab", System.getProperty("user.dir"), File.separator);

			File dir = new File(dirName);

			if (!dir.exists()) {
				boolean ret = dir.mkdirs();
				if (!ret) {
					log.error("could not create {}", dirName);
				}
			}

			if (!dir.isDirectory()) {
				log.error("{} is not a file", dirName);
			}

			return dirName;

		} catch (Exception e) {
			Logging.logError(e);
		}
		return null;
	}

	public static File[] getPackageContent(String packageName) throws IOException {
		ArrayList<File> list = new ArrayList<File>();
		Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources(packageName);
		while (urls.hasMoreElements()) {
			URL url = urls.nextElement();
			File dir = new File(url.getFile());
			for (File f : dir.listFiles()) {
				list.add(f);
			}
		}
		return list.toArray(new File[] {});
	}

	// getBytes end ------------------

	static public String getResouceLocation() {
		URL url = File.class.getResource("/resource");

		// FIXME - DALVIK issue !
		if (url == null) {
			return null; // FIXME DALVIK issue
		} else {
			return url.toString();
		}
	}

	static public String getResourceJarPath() {

		if (!inJar()) {
			log.info("resource is not in jar");
			return null;
		}

		String full = getResouceLocation();
		String jarPath = full.substring(full.indexOf("jar:file:/") + 10, full.lastIndexOf("!"));
		return jarPath;
	}

	// --- object interface end --------

	// jar pathing begin ---------------

	static public String getRootLocation() {
		URL url = File.class.getResource("/");
		return url.toString();
	}

	public static final String getSource() {
		try {
			// return
			// URLDecoder.decode(Bootstrap.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath(),
			// "UTF-8");
			return FileIO.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
		} catch (Exception e) {
			Logging.logError(e);
			return null;
		}
	}

	static public byte[] getURL(URL url) {
		try {
			URLConnection conn = url.openConnection();
			return toByteArray(conn.getInputStream());
		} catch (Exception e) {
			Logging.logError(e);
		}
		return null;
	}

	static public boolean inJar() {
		String location = getResouceLocation();
		if (location != null) {
			return getResouceLocation().startsWith("jar:");
		} else {
			return false;
		}
	}

	public static final boolean isJar() {
		String source = getSource();
		if (source != null) {
			return source.endsWith(".jar");
		}
		return false;
	}

	/**
	 * similar to ls - when running in jar form will return contents of path in
	 * an array list of files when running in the ide will return the contents
	 * of /bin + path
	 * 
	 * @param path
	 * @return
	 */
	static public ArrayList<File> listInternalContents(String path) {
		ArrayList<File> ret = new ArrayList<File>();
		if (!inJar()) {
			// get listing if in debug mode or classes are unzipped
			String rp = getRootLocation();
			String targetDir = rp.substring(rp.indexOf("file:/") + 6);
			String fullPath = targetDir + path;
			File dir = new File(fullPath);
			if (!dir.exists()) {
				log.error(String.format("%s does not exist", fullPath));
				return ret;
			}

			if (!dir.isDirectory()) {
				ret.add(dir);
			}

			String[] tmp = dir.list();
			for (int i = 0; i < tmp.length; ++i) {
				File file = new File(targetDir + path + "/" + tmp[i]);
				ret.add(file);
				/*
				 * if (dirCheck.isDirectory()) { ret.add(tmp[i] + "/"); } else {
				 * ret.add(tmp[i]); }
				 */
			}
			dir.list();
			return ret;
		} else {
			// gets compiled to the "bin" directory in eclipse
			File bin = new File(String.format("./bin%s", path));
			if (!bin.exists()) {
				log.error("{} does not exist", bin);
				return ret;
			}

			log.error("implement");
			// if ()
			return null;
		}
	}

	static public ArrayList<File> listResourceContents(String path) {
		return listInternalContents("/resource" + path);
	}

	/**
	 * inter process file communication - default is to wait and attempt to load
	 * a file in the next second - it comes from savePartFile - then the writing
	 * of the file from a different process should be an atomic move regardless
	 * of file size
	 * 
	 * @param filename
	 * @throws IOException
	 */
	static public byte[] loadPartFile(String filename) throws IOException {
		return loadPartFile(filename, 1000);
	}

	static public byte[] loadPartFile(String filename, long timeoutMs) throws IOException {
		long startTs = System.currentTimeMillis();
		try {
			while (System.currentTimeMillis() - startTs < timeoutMs) {

				File file = new File(filename);
				if (file.exists()) {
					return fileToByteArray(file);
				}
				Thread.sleep(30);
			}

		} catch (Exception e) {
			new IOException("interrupted while waiting for file to arrive");
		}
		return null;
	}

	// jar pathing end ---------------
	// -- os primitives begin -------

	public static void main(String[] args) throws ZipException, IOException {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {
			String t = "this is a test";
			FileIO.savePartFile("save.txt", t.getBytes());
			byte[] data = FileIO.loadPartFile("save.txt", 10000);
			if (data != null) {
				log.info(new String(data));
			}

			/*
			 * String data = resourceToString("version.txt"); data =
			 * resourceToString("framework/ivychain.xml"); data =
			 * resourceToString("framework/serviceData.xml");
			 * 
			 * byte[] ba = resourceToByteArray("version.txt"); ba =
			 * resourceToByteArray("framework/version.txt"); ba =
			 * resourceToByteArray("framework/serviceData.xml");
			 * 
			 * String hello = resourceToString("blah.txt");
			 * 
			 * copyResource("mrl_logo.jpg", "mrl_logo.jpg");
			 * 
			 * byte[] b = resourceToByteArray("mrl_logo.jpg");
			 * 
			 * File[] files = getPackageContent("");
			 * 
			 * log.info(getBinaryPath());
			 * 
			 * log.info("{}", b);
			 * 
			 * log.info("done");
			 */
		} catch (Exception e) {
			Logging.logError(e);
		}

	}

	public final static Object readBinary(String filename) {
		try {
			InputStream file = new FileInputStream(filename);
			InputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream(buffer);
			try {
				return input.readObject();
			} finally {
				input.close();
			}
		} catch (Exception e) {
			Logging.logError(e);
			return null;
		}
	}

	public static final byte[] resourceToByteArray(String resourceName) {
		String filename = String.format("/resource/%s", resourceName);

		log.info(String.format("looking for %s", filename));
		InputStream isr = null;
		if (isJar()) {
			isr = FileIO.class.getResourceAsStream(filename);
		} else {
			try {
				isr = new FileInputStream(String.format("%sresource/%s", getSource(), resourceName));
			} catch (Exception e) {
				Logging.logError(e);
				return null;
			}
		}
		byte[] data = null;
		try {
			if (isr == null) {
				log.error(String.format("can not find resource [%s]", filename));
				return null;
			}
			data = toByteArray(isr);
		} finally {
			try {
				if (isr != null) {
					isr.close();
				}
			} catch (Exception e) {

			}
		}
		return data;
	}

	public final static String resourceToString(String filename) {
		byte[] bytes = resourceToByteArray(filename);
		if (bytes == null) {
			return null;
		}
		return new String(bytes);
	}

	public static boolean rmDir(File directory, Set<File> exclude) {
		if (directory.exists()) {
			File[] files = directory.listFiles();
			if (null != files) {
				for (int i = 0; i < files.length; i++) {
					if (files[i].isDirectory()) {
						rmDir(files[i], exclude);
					} else {
						if (exclude != null && exclude.contains(files[i])) {
							log.info("skipping exluded file {}", files[i].getName());
						} else {
							log.info("removing file {}", files[i].getName());
							files[i].delete();
						}
					}
				}
			}
		}

		boolean ret = (exclude != null) ? true : (directory.delete());
		return ret;
	}

	/**
	 * for inter process file writting & locking ..
	 * 
	 * @param filename
	 * @param data
	 * @throws IOException
	 */
	static public void savePartFile(String filename, byte[] data) throws IOException {
		// first delete any part or filename file currently there
		File file = new File(filename);
		if (file.exists()) {
			if (!file.delete()) {
				throw new IOException(String.format("%s exists but could not delete", filename));
			}
		}
		String partFilename = String.format("%s.part", filename);
		File partFile = new File(partFilename);
		if (partFile.exists()) {
			if (!partFile.delete()) {
				throw new IOException(String.format("%s exists but could not delete", partFilename));
			}
		}

		byteArrayToFile(partFilename, data);

		if (!partFile.renameTo(new File(filename))) {
			throw new IOException(String.format("could not rename %s to %s ..  don't know why.. :(", partFilename, filename));
		}

	}

	public static void stringToFile(String filename, String data) throws IOException {
		byteArrayToFile(filename, data.getBytes());
	}

	public static byte[] toByteArray(InputStream is) {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		// DataInputStream input = new DataInputStream(isr);
		try {

			int nRead;
			byte[] data = new byte[16384];

			while ((nRead = is.read(data, 0, data.length)) != -1) {
				baos.write(data, 0, nRead);
			}

			baos.flush();
			baos.close();
			return baos.toByteArray();
		} catch (Exception e) {
			Logging.logError(e);
		}

		return null;
	}

	// --- object interface begin ------
	public final static boolean writeBinary(String filename, Object toSave) {
		try {
			// use buffering
			OutputStream file = new FileOutputStream(filename);
			OutputStream buffer = new BufferedOutputStream(file);
			ObjectOutput output = new ObjectOutputStream(buffer);
			try {
				output.writeObject(toSave);
				output.flush();
			} finally {
				output.close();
			}
		} catch (IOException e) {
			Logging.logError(e);
			return false;
		}
		return true;
	}

}
