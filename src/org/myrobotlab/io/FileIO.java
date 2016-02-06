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
package org.myrobotlab.io;

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
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
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

	static public void byteArrayToFile(File dst, byte[] data) throws IOException {
		FileOutputStream fos = null;
		fos = new FileOutputStream(dst);
		fos.write(data);
		fos.close();

	}

	static public void deleteRecursive(File path) {
		File[] c = path.listFiles();
		log.info("clearing dir " + path.toString());
		for (File file : c) {
			if (file.isDirectory()) {
				deleteRecursive(file);
				log.info("deleting dir " + file.toString());
				file.delete();
			} else {
				log.info("deleting file " + file.toString());
				file.delete();
			}
		}

		path.delete();
	}


	/**
	 * compares two files - throws if they are not identical, good to use in
	 * testing
	 * 
	 * @param filename1
	 * @param filename2
	 * @throws FileComparisonException
	 * @throws IOException
	 */
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


	

	// --- string interface end --------------------

	// --- byte[] interface begin ------------------
	// rename getBytes getResourceBytes / String File InputStream

	//
	static public boolean copyResource(File src, File dst) {
		try {
			byte[] b = resourceToByteArray(src);
			String path = dst.getParent();
			File dir = new File(path);
			if (!dir.exists()) {
				dir.mkdirs();
			}

			byteArrayToFile(dst, b);
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

	/*
	 * public static File[] getPackageContent(String packageName) throws
	 * IOException { ArrayList<File> list = new ArrayList<File>();
	 * Enumeration<URL> urls =
	 * Thread.currentThread().getContextClassLoader().getResources(packageName);
	 * while (urls.hasMoreElements()) { URL url = urls.nextElement(); File dir =
	 * new File(url.getFile()); if (dir != null) { for (File f :
	 * dir.listFiles()) { list.add(f); } } } return list.toArray(new File[] {});
	 * }
	 */

	// getBytes end ------------------

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

	static public byte[] getURL(URL url) {
		try {
			URLConnection conn = url.openConnection();
			return toByteArray(conn.getInputStream());
		} catch (Exception e) {
			Logging.logError(e);
		}
		return null;
	}

	static public String getResouceLocation() {
		URL url = File.class.getResource("/resource");

		// FIXME - DALVIK issue !
		if (url == null) {
			return null; // FIXME DALVIK issue
		} else {
			return url.toString();
		}
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

	public static String getJarName() {
		String nm = getSource();
		if (!nm.endsWith(".jar")) {
			log.error("mrl is not in a jar!");
			return null;
		}
		return nm;
	}

	public static final String getSource() {
		try {
			// return
			// URLDecoder.decode(Bootstrap.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath(),
			// "UTF-8");
			String source = FileIO.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			log.info("getSource {}", source);
			return source;
		} catch (Exception e) {
			log.error("getSource threw");
			Logging.logError(e);
			return null;
		}
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

	public static final byte[] resourceToByteArray(File src) {
		String filename = String.format("/resource/%s", src);

		log.info(String.format("looking for %s", filename));
		InputStream isr = null;
		if (isJar()) {
			isr = FileIO.class.getResourceAsStream(filename);
		} else {
			try {
				isr = new FileInputStream(String.format("%sresource/%s", getSource(), src));
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

	public final static String resourceToString(String src) {
		return resourceToString(new File(src));
	}
	
	public final static String resourceToString(File src) {
		byte[] bytes = resourceToByteArray(src);
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
	static public void savePartFile(File file, byte[] data) throws IOException {
		// first delete any part or filename file currently there

		String filename = file.getName();
		
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

		byteArrayToFile(new File(partFilename), data);

		if (!partFile.renameTo(new File(filename))) {
			throw new IOException(String.format("could not rename %s to %s ..  don't know why.. :(", partFilename, filename));
		}

	}

	public static void stringToFile(String filename, String data) throws IOException {
		byteArrayToFile(new File(filename), data.getBytes());
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

	static public void copy(File src, File dst) throws IOException {		
		byte[] b = fileToByteArray(src);
		byteArrayToFile(dst, b);
	}

	static public void copyFile(InputStream is, long size, String outFile) throws IOException {
		String[] parts = outFile.split("/");
		if (parts.length > 1) {
			if (parts.length > 1) {
				File d = new File(outFile.substring(0, outFile.lastIndexOf("/")));
				if (!d.exists()) {
					d.mkdirs();
				}
			}
		}

		FileOutputStream fos = new FileOutputStream(new File(outFile));
		/*
		 * apparently size is not correct or is compressed size ? dunno
		 * something aint right ! byte[] buffer = new byte[(int) size];
		 * is.read(buffer); FileOutputStream fos = new FileOutputStream(new
		 * File(outFile)); fos.write(buffer); fos.close(); is.close();
		 */

		// some files are big - nice to have a big buffer
		byte[] byteArray = new byte[262144];
		int i;

		while ((i = is.read(byteArray)) > 0) {
			fos.write(byteArray, 0, i);
		}
		is.close();
		fos.close();
	}

	static public final boolean extractResources() {
		try {
			return extractResources(false);
		} catch (Exception e) {
			Logging.logError(e);
		}
		return false;
	}

	static public final boolean extractResources(boolean overwrite) throws IOException {
		String resourceName = "resource";
		File check = new File(resourceName);
		if (check.exists() && !overwrite) {
			log.warn("{} aleady exists - not extracting", resourceName);
			return false;
		}

		if (!inJar() && !overwrite) {
			log.warn("mrl is not operating in a jar - not extracting");
			return false;
		}

		return extract(getJarName(), resourceName, "");
	}

	static public final boolean extract(String jarFile, String from, String to) throws IOException {
		// extract(/C:/mrl/myrobotlab/dist/myrobotlab.jar, resource, )
		log.info(String.format("extract(%s, %s, %s)", jarFile, from, to));

		boolean contents = false;
		boolean found = false;
		boolean firstMatch = true;

		JarFile jar = new JarFile(jarFile);
		Enumeration<JarEntry> enumEntries = jar.entries();

		// normalize slash
		if (from != null) {
			from = from.replace("\\", "\\\\");
		}

		if (to != null) {
			to = to.replace("\\", "\\\\");
		}

		// normalize [from | from/ | from/*]
		String fromRoot = null;
		if (from != null && (from.endsWith("/") || from.endsWith("/*"))) {
			fromRoot = from.substring(0, from.lastIndexOf("/"));
			contents = true;
		} else {
			fromRoot = from;
		}

		// normalize [to , to/]
		if (to == null || to.equals("") || to.equals("./")) {
			to = ".";
		}

		while (enumEntries.hasMoreElements()) {
			JarEntry file = (JarEntry) enumEntries.nextElement();
			// log.debug(file.getName());

			// spin through resrouces until a match
			if (fromRoot != null && !file.getName().startsWith(fromRoot)) {
				// log.info(String.format("skipping %s", file.getName()));
				continue;
			}

			found = true;

			// our first match !
			if (!file.isDirectory()) {
				// not a directory
				String name = null;
				if (contents) {
					name = String.format("%s/%s", to, file.getName().substring((fromRoot.length() + 1)));
				} else {
					if (firstMatch) {
						// file to file
						name = to;
					} else {
						// dirFile to file
						name = String.format("%s/%s", to, file.getName());
					}
				}

				log.info("extracting {} to {}", file.getName(), name);
				copyFile(jar.getInputStream(file), file.getSize(), name);
				// file to file copy ... done
				if (firstMatch) {
					break;
				}
			} else {
				// df
				// if (conti)
				String name = null;
				if (contents) {
					name = String.format("%s/%s", to, file.getName().substring((fromRoot.length() + 1)));
				} else {
					name = String.format("%s/%s", to, file.getName());
				}
				File d = new File(name);
				d.mkdirs();
			}

			firstMatch = false;
		}

		if (!found) {
			log.error("could not find {}", from);
		}

		jar.close();

		return found;
	}

	/**
	 * 
	 * Yet Another Way
	 * 
	 * public void extractFromJar(String jarFile, String fileToExtract, String
	 * dest) { try {
	 * 
	 * 
	 * String home = getClass().getProtectionDomain().
	 * getCodeSource().getLocation().toString(). substring(6);
	 * 
	 * JarFile jar = new JarFile(jarFile); ZipEntry entry =
	 * jar.getEntry(fileToExtract); File efile = new File(dest,
	 * entry.getName());
	 * 
	 * InputStream in = new BufferedInputStream(jar.getInputStream(entry));
	 * OutputStream out = new BufferedOutputStream(new FileOutputStream(efile));
	 * byte[] buffer = new byte[2048]; for (;;) { int nBytes = in.read(buffer);
	 * if (nBytes <= 0) break; out.write(buffer, 0, nBytes); } out.flush();
	 * out.close(); in.close(); } catch (Exception e) { e.printStackTrace(); } }
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */

	public static List<File> getPackageContent(String packageName) throws IOException {
		return getPackageContent(packageName, false, null, null);
	}

	/** 
	 * The "goal" of this method is to get the list of contents from a package REGARDLESS of the packaging :P
	 * Regrettably, the implementation depends greatly on if the classes are on the file system vs if they are in a jar file
	 * 
	 * Step 1: find out if our application is running in a jar (runtime release) or running on classes on the file system (debug)
	 * 
	 * Step 2: if we are running from the file system with .class files - it becomes very simple file operations 
	 * 
	 * @param packageName
	 * @param recurse
	 * @param include
	 * @param exclude
	 * @return
	 * @throws IOException
	 */
	public static List<File> getPackageContent(String packageName, boolean recurse, String[] include, String[] exclude) throws IOException {
		
		// FIXME - "if inJar()" must be done in a very different way !
		
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		assert classLoader != null;
		String path = packageName.replace('.', '/');
		Enumeration<URL> resources = classLoader.getResources(path);
		List<File> dirs = new ArrayList<File>();
		log.info("resources.hassMoreElements {}", resources.hasMoreElements());
		
		while (resources.hasMoreElements()) {
			URL resource = resources.nextElement();
			log.info("resources.nextElement {}", resource);
			dirs.add(new File(resource.getFile()));
		}
		
		ArrayList<File> files = new ArrayList<File>();
		// if (recurse) {
		for (File directory : dirs) {
			files.addAll(findPackageContents(directory, packageName, recurse, include, exclude));
		}
		// }
		return files;// .toArray(new Class[classes.size()]);
	}

	/**
	 * Recursive method used to find all classes in a given directory and
	 * subdirs.
	 *
	 * @param directory
	 *            The base directory
	 * @param packageName
	 *            The package name for classes found inside the base directory
	 * @return The classes
	 * @throws ClassNotFoundException
	 */
	public static List<File> findPackageContents(File directory, String packageName, boolean recurse, String[] include, String[] exclude) {
		List<File> classes = new ArrayList<File>();
		if (!directory.exists()) {
			return classes;
		}
		File[] files = directory.listFiles();
		for (File file : files) {
			if (file.isDirectory() && recurse) {
				assert !file.getName().contains(".");
				classes.addAll(findPackageContents(file, packageName + "." + file.getName(), recurse, include, exclude));
			} else { // if (file.getName().endsWith(".class")) {
				String filename = file.getName();
				boolean add = false;
				if (include != null) {
					for (int i = 0; i < include.length; ++i) {
						if (filename.matches(include[i])) {
							add = true;
							break;
						}
					}
				}

				if (exclude != null) {
					for (int i = 0; i < exclude.length; ++i) {
						if (filename.matches(exclude[i])) {
							add = false;
							break;
						}
					}
				}

				if (include == null && exclude == null) {
					add = true;
				}

				if (add) {
					classes.add(file);
				}
			}
		}
		return classes;
	}

	// FIXME - UNIT TESTS !!!
	public static void main(String[] args) throws ZipException, IOException {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			/*
			final URL jarUrl = new URL("jar:file:/C:/mrl/myrobotlab/dist/myrobotlab.jar!/resource");
			final JarURLConnection connection = (JarURLConnection) jarUrl.openConnection();
			final URL url = connection.getJarFileURL();

			System.out.println(url.getFile());
			*/

			List<File> c = getPackageContent("org.myrobotlab.service");

			// File[] files = getPackageContent("org.myrobotlab.service");

//			extract("develop/myrobotlab.jar", "resource/version.txt", "./version.txt");

			// extract("/C:/mrl/myrobotlab/dist/myrobotlab.jar", "resource",
			// "");
//			extract("dist/myrobotlab.jar", "resource", "");
			// extractResources();
			/*
			 * // extract directory to a non existent directory // result should
			 * be test7 extract("dist/myrobotlab.jar",
			 * "resource/AdafruitMotorShield/*", "test66");
			 * 
			 * // file to file extract("dist/myrobotlab.jar",
			 * "module.properties", "module.txt");
			 * 
			 * // file to file extract("dist/myrobotlab.jar",
			 * "resource/ACEduinoMotorShield.png", "ACEduinoMotorShield.png");
			 * 
			 * // file to file extract("dist/myrobotlab.jar",
			 * "resource/ACEduinoMotorShield.png",
			 * "test2/deeper/ACEduinoMotorShield.png");
			 * 
			 * // extract directory to a non existent directory // result should
			 * be test7 extract("dist/myrobotlab.jar", "resource/*", "test7");
			 * 
			 * // extract directory to a non existent directory // result should
			 * be test8/testdeeper/(contents of resource)
			 * extract("dist/myrobotlab.jar", "resource/", "test8/testdeeper");
			 * 
			 * // extract directory to a non existent directory // result should
			 * be test3/deep/deeper/resource extract("dist/myrobotlab.jar",
			 * "resource", "test3/deep/deeper");
			 * 
			 * String t = "this is a test"; FileIO.savePartFile("save.txt",
			 * t.getBytes()); byte[] data = FileIO.loadPartFile("save.txt",
			 * 10000); if (data != null) { log.info(new String(data)); }
			 */

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

	public static List<String> getPackageClassNames(String packageName) throws ClassNotFoundException, IOException {
		List<File> files = getPackageContent(packageName, false, new String[] { ".*\\.class" }, new String[] { ".*Test\\.class", ".*\\$.*" });
		ArrayList<String> classes = new ArrayList<String>();
		String path = packageName.replace('.', '/');
		for (File file : files) {
			String filename = file.getName();
			String classname = String.format("%s.%s", packageName, filename.substring(0, filename.length() - 6));
			classes.add(classname);
		}
		return classes;
	}

}
