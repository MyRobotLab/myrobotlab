package org.myrobotlab.fileLib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public final class FindFile { // implements FilenameFilter
	/*
	 * private String root = "."; // the starting point for the search; private
	 * Boolean recurse = true; private Boolean includeDirsInResult = false;
	 * private Pattern pattern = null;
	 */

	class RegexFilter implements FilenameFilter {
		private Pattern pattern;

		public RegexFilter(String regex) {
			pattern = Pattern.compile(regex);
		}

		@Override
		public boolean accept(File dir, String name) {
			// Strip path information, search for regex:
			return pattern.matcher(new File(name).getName()).matches();
		}
	}

	public final static Logger log = LoggerFactory.getLogger(FindFile.class.getCanonicalName());

	public static List<File> find() throws FileNotFoundException {
		return find(null, null, true, false);
	}

	public static List<File> find(String criteria) throws FileNotFoundException {
		return find(null, criteria, true, false);
	}

	public static List<File> find(String root, String criteria) throws FileNotFoundException {
		return find(root, criteria, true, false);
	}

	public static List<File> find(String root, String criteria, boolean recurse, boolean includeDirsInResult) throws FileNotFoundException {
		if (root == null) {
			root = ".";
		}

		if (criteria == null) {
			criteria = ".*";
		}

		File r = new File(root);
		validateDirectory(r);
		List<File> result = process(r, criteria, recurse, includeDirsInResult);
		Collections.sort(result);
		return result;
	}

	public static List<File> findByExtension(String extensions) throws FileNotFoundException {
		// "([^\\s]+(\\.(?i)(jpg|png|gif|bmp))$)"
		return find(null, "([^\\s]+(\\.(?i)(" + extensions + "))$)", true, false);
	}

	public static List<File> findByExtension(String root, String extensions) throws FileNotFoundException {
		return find(root, "([^\\s]+(\\.(?i)(" + extensions + "))$)", true, false);
	}

	public static void main(String... aArgs) throws FileNotFoundException {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		// TODO - there was methods to do this already in java.io
		List<File> files = FindFile.find(".ivy", "resolved.*\\.xml$");

		// List<File> files = FindFile.find("\\.(?i:)(?:xml)$");
		// List<File> files = FindFile.find(".*\\.java$");
		// List<File> files = FindFile.find(".*\\.svn$");

		// print out all file names, in the the order of File.compareTo()
		for (File file : files) {
			String name = file.getName();
			name = name.substring(name.indexOf("-") + 1);
			name = name.substring(0, name.indexOf("-"));
			System.out.println(name);
		}
	}

	// recursively go through ALL directories regardless of matching
	// need to find all files before we can filter them
	private static List<File> process(File rootPath, String criteria, boolean recurse, boolean includeDirsInResult) throws FileNotFoundException {
		List<File> result = new ArrayList<File>();
		File[] filesAndDirs = rootPath.listFiles();
		List<File> filesDirs = Arrays.asList(filesAndDirs);
		log.debug("looking at path " + rootPath + " has " + filesDirs.size() + " files");
		for (File file : filesDirs) {

			StringBuffer out = new StringBuffer();
			out.append(file.getName());

			Pattern pattern = Pattern.compile(criteria);

			Matcher matcher = pattern.matcher(file.getName());
			if (matcher.find()) {
				out.append(" matches");
				if (file.isFile() || (!file.isFile() && includeDirsInResult)) {
					out.append(" will be added");
					result.add(file);
				} else {
					out.append(" will not be added");
				}
			} else {
				out.append(" does not match");
			}

			if (!file.isFile() && recurse) {
				log.debug("decending into " + file.getName());
				List<File> deeperList = process(file, criteria, recurse, includeDirsInResult);
				result.addAll(deeperList);
			}

			log.debug(out.toString());
		}
		return result;
	}

	static private void validateDirectory(File aDirectory) throws FileNotFoundException {
		if (aDirectory == null) {
			throw new IllegalArgumentException("Directory should not be null.");
		}
		if (!aDirectory.exists()) {
			throw new FileNotFoundException("Directory does not exist: " + aDirectory);
		}
		if (!aDirectory.isDirectory()) {
			throw new IllegalArgumentException("Is not a directory: " + aDirectory);
		}
		if (!aDirectory.canRead()) {
			throw new IllegalArgumentException("Directory cannot be read: " + aDirectory);
		}
	}

	// TODO - extention filter
	// TODO - simple astrix filter

}
