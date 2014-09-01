/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
 Part of the Processing project - http://processing.org

 Copyright (c) 2004-10 Ben Fry and Casey Reas
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

 GroG - removed all Swing & Swing references - no gui components

 */

package org.myrobotlab.arduino;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.myrobotlab.arduino.compiler.AvrdudeUploader;
import org.myrobotlab.arduino.compiler.Compiler;
import org.myrobotlab.arduino.compiler.PdePreprocessor;
import org.myrobotlab.arduino.compiler.RunnerException;
import org.myrobotlab.arduino.compiler.Sizer;
import org.myrobotlab.arduino.compiler.Uploader;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Arduino;
import org.slf4j.Logger;

public class Sketch {
	Arduino myArduino;
	public final static Logger log = LoggerFactory.getLogger(Sketch.class.getCanonicalName());

	static private File tempBuildFolder;

	String program;

	/** main pde file for this sketch. */
	private File primaryFile;

	/**
	 * Name of sketch, which is the name of main file (without .pde or .java
	 * extension)
	 */
	private String name;

	/** true if any of the files have been modified. */
	private boolean modified;

	/** folder that contains this sketch */
	private File folder;

	/** data folder location for this sketch (may not exist yet) */
	private File dataFolder;

	/** code folder location for this sketch (may not exist yet) */
	private File codeFolder;

	private SketchCode2 current;
	private int currentIndex;
	/**
	 * Number of sketchCode objects (tabs) in the current sketch. Note that this
	 * will be the same as code.length, because the getCode() method returns
	 * just the code[] array, rather than a copy of it, or an array that's been
	 * resized to just the relevant files themselves.
	 * http://dev.processing.org/bugs/show_bug.cgi?id=940
	 */
	private int codeCount;
	private SketchCode2[] code;

	/** Class name for the PApplet, as determined by the preprocessor. */
	private String appletClassName;
	/** Class path determined during build. */
	private String classPath;

	/**
	 * This is *not* the "Processing" libraries path, this is the Java libraries
	 * path, as in java.library.path=BlahBlah, which identifies search paths for
	 * DLLs or JNILIBs.
	 */
	private String libraryPath;
	/**
	 * List of library folders.
	 */
	private ArrayList<File> importedLibraries;

	/**
	 * path is location of the main .pde file, because this is also simplest to
	 * use when opening the file from the finder/explorer.
	 */
	public Sketch(String path, Arduino myArduino) throws IOException {

		this.myArduino = myArduino;
		primaryFile = new File(path);

		// get the name of the sketch by chopping .pde or .java
		// off of the main file name
		String mainFilename = primaryFile.getName();
		int suffixLength = getDefaultExtension().length() + 1;
		name = mainFilename.substring(0, mainFilename.length() - suffixLength);

		tempBuildFolder = myArduino.getBuildFolder();
		folder = new File(new File(path).getParent());

		load();
	}

	/**
	 * Build the list of files.
	 * <P>
	 * Generally this is only done once, rather than each time a change is made,
	 * because otherwise it gets to be a nightmare to keep track of what files
	 * went where, because not all the data will be saved to disk.
	 * <P>
	 * This also gets called when the main sketch file is renamed, because the
	 * sketch has to be reloaded from a different folder.
	 * <P>
	 * Another exception is when an external editor is in use, in which case the
	 * load happens each time "run" is hit.
	 */
	protected void load() {
		codeFolder = new File(folder, "code");
		dataFolder = new File(folder, "data");

		// get list of files in the sketch folder
		String list[] = folder.list();

		// reset these because load() may be called after an
		// external editor event. (fix for 0099)
		codeCount = 0;

		code = new SketchCode2[list.length];

		String[] extensions = getExtensions();

		for (String filename : list) {
			// Ignoring the dot prefix files is especially important to avoid
			// files
			// with the ._ prefix on Mac OS X. (You'll see this with Mac files
			// on
			// non-HFS drives, i.e. a thumb drive formatted FAT32.)
			if (filename.startsWith("."))
				continue;

			// Don't let some wacko name a directory blah.pde or bling.java.
			if (new File(folder, filename).isDirectory())
				continue;

			// figure out the name without any extension
			String base = filename;
			// now strip off the .pde and .java extensions
			for (String extension : extensions) {
				if (base.toLowerCase().endsWith("." + extension)) {
					base = base.substring(0, base.length() - (extension.length() + 1));

					// Don't allow people to use files with invalid names, since
					// on load,
					// it would be otherwise possible to sneak in nasty
					// filenames. [0116]
					if (Sketch.isSanitaryName(base)) {
						code[codeCount++] = new SketchCode2(new File(folder, filename), extension);
					}
				}
			}
		}
		// Remove any code that wasn't proper
		code = (SketchCode2[]) PApplet.subset(code, 0, codeCount);

		// move the main class to the first tab
		// start at 1, if it's at zero, don't bother
		for (int i = 1; i < codeCount; i++) {
			// if (code[i].file.getName().equals(mainFilename)) {
			if (code[i].getFile().equals(primaryFile)) {
				SketchCode2 temp = code[0];
				code[0] = code[i];
				code[i] = temp;
				break;
			}
		}

		// sort the entries at the top
		sortCode();

		setCurrentCode(0);
	}

	protected void replaceCode(SketchCode2 newCode) {
		for (int i = 0; i < codeCount; i++) {
			if (code[i].getFileName().equals(newCode.getFileName())) {
				code[i] = newCode;
				break;
			}
		}
	}

	protected void insertCode(SketchCode2 newCode) {
		// make sure the user didn't hide the sketch folder
		ensureExistence();

		code = (SketchCode2[]) PApplet.append(code, newCode);
		codeCount++;
	}

	protected void sortCode() {
		// cheap-ass sort of the rest of the files
		// it's a dumb, slow sort, but there shouldn't be more than ~5 files
		for (int i = 1; i < codeCount; i++) {
			int who = i;
			for (int j = i + 1; j < codeCount; j++) {
				if (code[j].getFileName().compareTo(code[who].getFileName()) < 0) {
					who = j; // this guy is earlier in the alphabet
				}
			}
			if (who != i) { // swap with someone if changes made
				SketchCode2 temp = code[who];
				code[who] = code[i];
				code[i] = temp;
			}
		}
	}

	boolean renamingCode;

	/**
	 * Handler for the New Code menu option.
	 */
	public void handleNewCode() {
		// make sure the user didn't hide the sketch folder
		ensureExistence();
	}

	public String showWarning(String warning, String desc, Exception e) {
		return warning;
	}

	protected void removeCode(SketchCode2 which) {
		// remove it from the internal list of files
		// resort internal list of files
		for (int i = 0; i < codeCount; i++) {
			if (code[i] == which) {
				for (int j = i; j < codeCount - 1; j++) {
					code[j] = code[j + 1];
				}
				codeCount--;
				code = (SketchCode2[]) PApplet.shorten(code);
				return;
			}
		}
		System.err.println("removeCode: internal error.. could not find code");
	}

	/**
	 * Move to the previous tab.
	 */
	public void handlePrevCode() {
		int prev = currentIndex - 1;
		if (prev < 0)
			prev = codeCount - 1;
		setCurrentCode(prev);
	}

	/**
	 * Move to the next tab.
	 */
	public void handleNextCode() {
		setCurrentCode((currentIndex + 1) % codeCount);
	}

	/**
	 * Sets the modified value for the code in the frontmost tab.
	 */
	public void setModified(boolean state) {
		// System.out.println("setting modified to " + state);
		// new Exception().printStackTrace();
		current.setModified(state);
		calcModified();
	}

	protected void calcModified() {
		modified = false;
		for (int i = 0; i < codeCount; i++) {
			if (code[i].isModified()) {
				modified = true;
				break;
			}
		}
	}

	public boolean isModified() {
		return modified;
	}

	protected boolean renameCodeToInoExtension(File pdeFile) {
		for (SketchCode2 c : code) {
			if (!c.getFile().equals(pdeFile))
				continue;

			String pdeName = pdeFile.getPath();
			pdeName = pdeName.substring(0, pdeName.length() - 4) + ".ino";
			return c.renameTo(new File(pdeName), "ino");
		}
		return false;
	}

	/**
	 * Add import statements to the current tab for all of packages inside the
	 * specified jar file.
	 */
	public void importLibrary(String jarPath) {
		// make sure the user didn't hide the sketch folder
		ensureExistence();

		String list[] = Compiler.headerListFromIncludePath(jarPath);

		// import statements into the main sketch file (code[0])
		// if the current code is a .java file, insert into current
		// if (current.flavor == PDE) {
		if (hasDefaultExtension(current)) {
			setCurrentCode(0);
		}
		// could also scan the text in the file to see if each import
		// statement is already in there, but if the user has the import
		// commented out, then this will be a problem.
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < list.length; i++) {
			buffer.append("#include <");
			buffer.append(list[i]);
			buffer.append(">\n");
		}
		buffer.append('\n');
		buffer.append(program);
		program = new String(buffer.toString());
		// editor.setSelection(0, 0); // scroll to start
		setModified(true);
	}

	/**
	 * Change what file is currently being edited. Changes the current tab
	 * index.
	 * <OL>
	 * <LI>store the String for the text of the current file.
	 * <LI>retrieve the String for the text of the new file.
	 * <LI>change the text that's visible in the text area
	 * </OL>
	 */
	public void setCurrentCode(int which) {
		// if current is null, then this is the first setCurrent(0)
		if ((currentIndex == which) && (current != null)) {
			return;
		}

		current = code[which];
		currentIndex = which;

	}

	/**
	 * Internal helper function to set the current tab based on a name.
	 * 
	 * @param findName
	 *            the file name (not pretty name) to be shown
	 */
	protected void setCurrentCode(String findName) {
		for (int i = 0; i < codeCount; i++) {
			if (findName.equals(code[i].getFileName()) || findName.equals(code[i].getPrettyName())) {
				setCurrentCode(i);
				return;
			}
		}
	}

	/**
	 * Cleanup temporary files used during a build/run.
	 */
	protected void cleanup() {
		// if the java runtime is holding onto any files in the build dir, we
		// won't be able to delete them, so we need to force a gc here
		System.gc();

		// note that we can't remove the builddir itself, otherwise
		// the next time we start up, internal runs using Runner won't
		// work because the build dir won't exist at startup, so the classloader
		// will ignore the fact that that dir is in the CLASSPATH in run.sh
		myArduino.removeDescendants(tempBuildFolder);
	}

	/**
	 * Preprocess, Compile, and Run the current code.
	 * <P>
	 * There are three main parts to this process:
	 * 
	 * <PRE>
	 *   (0. if not java, then use another 'engine'.. i.e. python)
	 * 
	 *    1. do the p5 language preprocessing
	 *       this creates a working .java file in a specific location
	 *       better yet, just takes a chunk of java code and returns a
	 *       new/better string editor can take care of saving this to a
	 *       file location
	 * 
	 *    2. compile the code from that location
	 *       catching errors along the way
	 *       placing it in a ready classpath, or .. ?
	 * 
	 *    3. run the code
	 *       needs to communicate location for window
	 *       and maybe setup presentation space as well
	 *       run externally if a code folder exists,
	 *       or if more than one file is in the project
	 * 
	 *    X. afterwards, some of these steps need a cleanup function
	 * </PRE>
	 */
	// protected String compile() throws RunnerException {

	/**
	 * When running from the editor, take care of preparations before running
	 * the build.
	 */
	public void prepare() {
		// make sure the user didn't hide the sketch folder
		ensureExistence();

		current.setProgram(program);

		// TODO record history here
		// current.history.record(program, SketchHistory.RUN);

		// if an external editor is being used, need to grab the
		// latest version of the code from the file.
		if (myArduino.preferences.getBoolean("editor.external")) {
			current = null;
			load();
		}

		// in case there were any boogers left behind
		// do this here instead of after exiting, since the exit
		// can happen so many different ways.. and this will be
		// better connected to the dataFolder stuff below.
		cleanup();

		// // handle preprocessing the main file's code
		// return build(tempBuildFolder.getAbsolutePath());
	}

	/**
	 * Build all the code for this sketch.
	 * 
	 * In an advanced program, the returned class name could be different, which
	 * is why the className is set based on the return value. A compilation
	 * error will burp up a RunnerException.
	 * 
	 * Setting purty to 'true' will cause exception line numbers to be
	 * incorrect. Unless you know the code compiles, you should first run the
	 * preprocessor with purty set to false to make sure there are no errors,
	 * then once successful, re-export with purty set to true.
	 * 
	 * @param buildPath
	 *            Location to copy all the .java files
	 * @return null if compilation failed, main class name if not
	 */
	public String preprocess(String buildPath) throws RunnerException {
		return preprocess(buildPath, new PdePreprocessor(myArduino));
	}

	public String preprocess(String buildPath, PdePreprocessor preprocessor) throws RunnerException {
		// make sure the user didn't hide the sketch folder
		ensureExistence();

		String[] codeFolderPackages = null;
		classPath = buildPath;

		// 1. concatenate all .pde files to the 'main' pde
		// store line number for starting point of each code bit

		StringBuffer bigCode = new StringBuffer();
		int bigCount = 0;
		for (SketchCode2 sc : code) {
			if (sc.isExtension("ino") || sc.isExtension("pde")) {
				sc.setPreprocOffset(bigCount);
				bigCode.append(sc.getProgram());
				bigCode.append('\n');
				bigCount += sc.getLineCount();
			}
		}

		// Note that the headerOffset isn't applied until compile and run,
		// because
		// it only applies to the code after it's been written to the .java
		// file.
		int headerOffset = 0;
		// PdePreprocessor preprocessor = new PdePreprocessor();
		try {
			headerOffset = preprocessor.writePrefix(bigCode.toString(), buildPath, name, codeFolderPackages);
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
			String msg = "Build folder disappeared or could not be written";
			throw new RunnerException(msg);
		}

		// 2. run preproc on that code using the sugg class name
		// to create a single .java file and write to buildpath

		String primaryClassName = null;

		try {
			// if (i != 0) preproc will fail if a pde file is not
			// java mode, since that's required
			String className = preprocessor.write();

			if (className == null) {
				throw new RunnerException("Could not find main class");
				// this situation might be perfectly fine,
				// (i.e. if the file is empty)
				// System.out.println("No class found in " + code[i].name);
				// System.out.println("(any code in that file will be ignored)");
				// System.out.println();

				// } else {
				// code[0].setPreprocName(className + ".java");
			}

			// store this for the compiler and the runtime
			primaryClassName = className + ".cpp";

		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
			String msg = "Build folder disappeared or could not be written";
			throw new RunnerException(msg);
		} catch (RunnerException pe) {
			// RunnerExceptions are caught here and re-thrown, so that they
			// don't
			// get lost in the more general "Exception" handler below.
			throw pe;

		} catch (Exception ex) {
			// TODO better method for handling this?
			System.err.println("Uncaught exception type:" + ex.getClass());
			ex.printStackTrace();
			throw new RunnerException(ex.toString());
		}

		// grab the imports from the code just preproc'd

		importedLibraries = new ArrayList<File>();

		for (String item : preprocessor.getExtraImports()) {
			File libFolder = (File) Arduino.importToLibraryTable.get(item);

			if (libFolder != null && !importedLibraries.contains(libFolder)) {
				importedLibraries.add(libFolder);
				// classPath += Compiler.contentsToClassPath(libFolder);
				libraryPath += File.pathSeparator + libFolder.getAbsolutePath();
			}
		}

		// 3. then loop over the code[] and save each .java file

		for (SketchCode2 sc : code) {
			if (sc.isExtension("c") || sc.isExtension("cpp") || sc.isExtension("h")) {
				// no pre-processing services necessary for java files
				// just write the the contents of 'program' to a .java file
				// into the build directory. uses byte stream and reader/writer
				// shtuff so that unicode bunk is properly handled
				String filename = sc.getFileName(); // code[i].name + ".java";
				try {
					Arduino.saveFile(sc.getProgram(), new File(buildPath, filename));
				} catch (IOException e) {
					e.printStackTrace();
					throw new RunnerException("Problem moving " + filename + " to the build folder");
				}
				// sc.setPreprocName(filename);

			} else if (sc.isExtension("ino") || sc.isExtension("pde")) {
				// The compiler and runner will need this to have a proper
				// offset
				sc.addPreprocOffset(headerOffset);
			}
		}
		return primaryClassName;
	}

	public ArrayList<File> getImportedLibraries() {
		return importedLibraries;
	}

	/**
	 * Map an error from a set of processed .java files back to its location in
	 * the actual sketch.
	 * 
	 * @param message
	 *            The error message.
	 * @param filename
	 *            The .java file where the exception was found.
	 * @param line
	 *            Line number of the .java file for the exception (1-indexed)
	 * @return A RunnerException to be sent to the editor, or null if it wasn't
	 *         possible to place the exception to the sketch code.
	 */
	// public RunnerException placeExceptionAlt(String message,
	// String filename, int line) {
	// String appletJavaFile = appletClassName + ".java";
	// SketchCode2 errorCode = null;
	// if (filename.equals(appletJavaFile)) {
	// for (SketchCode2 code : getCode()) {
	// if (code.isExtension("ino")) {
	// if (line >= code.getPreprocOffset()) {
	// errorCode = code;
	// }
	// }
	// }
	// } else {
	// for (SketchCode2 code : getCode()) {
	// if (code.isExtension("java")) {
	// if (filename.equals(code.getFileName())) {
	// errorCode = code;
	// }
	// }
	// }
	// }
	// int codeIndex = getCodeIndex(errorCode);
	//
	// if (codeIndex != -1) {
	// //System.out.println("got line num " + lineNumber);
	// // in case this was a tab that got embedded into the main .java
	// line -= getCode(codeIndex).getPreprocOffset();
	//
	// // lineNumber is 1-indexed, but editor wants zero-indexed
	// line--;
	//
	// // getMessage() will be what's shown in the editor
	// RunnerException exception =
	// new RunnerException(message, codeIndex, line, -1);
	// exception.hideStackTrace();
	// return exception;
	// }
	// return null;
	// }

	/**
	 * Map an error from a set of processed .java files back to its location in
	 * the actual sketch.
	 * 
	 * @param message
	 *            The error message.
	 * @param filename
	 *            The .java file where the exception was found.
	 * @param line
	 *            Line number of the .java file for the exception (0-indexed!)
	 * @return A RunnerException to be sent to the editor, or null if it wasn't
	 *         possible to place the exception to the sketch code.
	 */
	public RunnerException placeException(String message, String dotJavaFilename, int dotJavaLine) {
		int codeIndex = 0; // -1;
		int codeLine = -1;

		// System.out.println("placing " + dotJavaFilename + " " + dotJavaLine);
		// System.out.println("code count is " + getCodeCount());

		// first check to see if it's a .java file
		for (int i = 0; i < getCodeCount(); i++) {
			SketchCode2 code = getCode(i);
			if (!code.isExtension(getDefaultExtension())) {
				if (dotJavaFilename.equals(code.getFileName())) {
					codeIndex = i;
					codeLine = dotJavaLine;
					return new RunnerException(message, codeIndex, codeLine);
				}
			}
		}

		// If not the preprocessed file at this point, then need to get out
		if (!dotJavaFilename.equals(name + ".cpp")) {
			return null;
		}

		// if it's not a .java file, codeIndex will still be 0
		// this section searches through the list of .pde files
		codeIndex = 0;
		for (int i = 0; i < getCodeCount(); i++) {
			SketchCode2 code = getCode(i);

			if (code.isExtension(getDefaultExtension())) {
				// System.out.println("preproc offset is " +
				// code.getPreprocOffset());
				// System.out.println("looking for line " + dotJavaLine);
				if (code.getPreprocOffset() <= dotJavaLine) {
					codeIndex = i;
					// System.out.println("i'm thinkin file " + i);
					codeLine = dotJavaLine - code.getPreprocOffset();
				}
			}
		}
		// could not find a proper line number, so deal with this differently.
		// but if it was in fact the .java file we're looking for, though,
		// send the error message through.
		// this is necessary because 'import' statements will be at a line
		// that has a lower number than the preproc offset, for instance.
		// if (codeLine == -1 && !dotJavaFilename.equals(name + ".java")) {
		// return null;
		// }
		return new RunnerException(message, codeIndex, codeLine);
	}

	/**
	 * Run the build inside the temporary build folder.
	 * 
	 * @return null if compilation failed, main class name if not
	 * @throws RunnerException
	 */
	public String build(boolean verbose) throws RunnerException {
		return build(tempBuildFolder.getAbsolutePath(), verbose);
	}

	/**
	 * Preprocess and compile all the code for this sketch.
	 * 
	 * In an advanced program, the returned class name could be different, which
	 * is why the className is set based on the return value. A compilation
	 * error will burp up a RunnerException.
	 * 
	 * @return null if compilation failed, main class name if not
	 */
	public String build(String buildPath, boolean verbose) throws RunnerException {

		// run the preprocessor
		progressUpdate(20);
		String primaryClassName = preprocess(buildPath);

		// compile the program. errors will happen as a RunnerException
		// that will bubble up to whomever called build().
		Compiler compiler = new Compiler(myArduino);
		if (compiler.compile(this.program, buildPath, primaryClassName, verbose)) {
			size(buildPath, primaryClassName);
			return primaryClassName;
		}
		return null;
	}

	/**
	 * Remove all files in a directory and the directory itself.
	 */
	public void removeDir(File dir) {
		if (dir.exists()) {
			removeDescendants(dir);
			if (!dir.delete()) {
				System.err.println("Could not delete " + dir);
			}
		}
	}

	/**
	 * Recursively remove all files within a directory, used with removeDir(),
	 * or when the contents of a dir should be removed, but not the directory
	 * itself. (i.e. when cleaning temp files from lib/build)
	 */
	public void removeDescendants(File dir) {
		if (!dir.exists())
			return;

		String files[] = dir.list();
		for (int i = 0; i < files.length; i++) {
			if (files[i].equals(".") || files[i].equals(".."))
				continue;
			File dead = new File(dir, files[i]);
			if (!dead.isDirectory()) {
				if (!myArduino.preferences.getBoolean("compiler.save_build_files")) {
					if (!dead.delete()) {
						// temporarily disabled
						System.err.println("Could not delete " + dead);
					}
				}
			} else {
				removeDir(dead);
				// dead.delete();
			}
		}
	}

	public boolean exportApplet(boolean usingProgrammer) throws Throwable {
		return exportApplet(tempBuildFolder.getAbsolutePath(), usingProgrammer);
	}

	/**
	 * Handle export to applet.
	 * 
	 * @throws Throwable
	 */
	public boolean exportApplet(String appletPath, boolean usingProgrammer) throws Throwable {

		// Make sure the user didn't hide the sketch folder
		ensureExistence();

		String code = FileIO.fileToString(primaryFile);

		current.setProgram(code);

		// Reload the code when an external editor is being used
		// if (Preferences.getBoolean("editor.external")) {
		// current = null;
		// nuke previous files and settings
		// load();
		// }

		File appletFolder = new File(appletPath);
		// Nuke the old applet folder because it can cause trouble
		// if (Preferences.getBoolean("export.delete_target_folder")) {
		removeDir(appletFolder);
		// }
		// Create a fresh applet folder (needed before preproc is run below)
		appletFolder.mkdirs();

		// build the sketch
		progressNotice("Compiling sketch...");
		String foundName = build(appletFolder.getPath(), false);
		// (already reported) error during export, exit this function
		if (foundName == null)
			return false;

		progressNotice("Uploading...");
		upload(appletFolder.getPath(), foundName, usingProgrammer);
		progressUpdate(100);
		return true;
	}

	public String progressNotice(String s) {
		log.info(s);
		return s;
	}

	public Integer progressUpdate(Integer i) {
		log.info("progressUpdate " + i + "%");
		return i;
	}

	public void setCompilingProgress(int percent) {
		progressUpdate(percent);
	}

	protected void size(String buildPath, String suggestedClassName) throws RunnerException {
		long size = 0;
		String maxsizeString = myArduino.getBoardPreferences().get("upload.maximum_size");
		if (maxsizeString == null)
			return;
		long maxsize = Integer.parseInt(maxsizeString);
		Sizer sizer = new Sizer(buildPath, suggestedClassName);
		try {
			size = sizer.computeSize();
			System.out.println("Binary sketch size: " + size + " bytes (of a " + maxsize + " byte maximum)");
		} catch (RunnerException e) {
			System.err.println("Couldn't determine program size: " + e.getMessage());
		}

		if (size > maxsize)
			throw new RunnerException("Sketch too big; see http://www.arduino.cc/en/Guide/Troubleshooting#size for tips on reducing it.");
	}

	protected String upload(String buildPath, String suggestedClassName, boolean usingProgrammer) throws Throwable {

		Uploader uploader;

		// download the program
		//
		uploader = new AvrdudeUploader(myArduino);
		boolean success = uploader.uploadUsingPreferences(buildPath, suggestedClassName, usingProgrammer);

		return success ? suggestedClassName : null;
	}

	/**
	 * Replace all commented portions of a given String as spaces. Utility
	 * function used here and in the preprocessor.
	 */
	static public String scrubComments(String what) {
		char p[] = what.toCharArray();

		int index = 0;
		while (index < p.length) {
			// for any double slash comments, ignore until the end of the line
			if ((p[index] == '/') && (index < p.length - 1) && (p[index + 1] == '/')) {
				p[index++] = ' ';
				p[index++] = ' ';
				while ((index < p.length) && (p[index] != '\n')) {
					p[index++] = ' ';
				}

				// check to see if this is the start of a new multiline comment.
				// if it is, then make sure it's actually terminated somewhere.
			} else if ((p[index] == '/') && (index < p.length - 1) && (p[index + 1] == '*')) {
				p[index++] = ' ';
				p[index++] = ' ';
				boolean endOfRainbow = false;
				while (index < p.length - 1) {
					if ((p[index] == '*') && (p[index + 1] == '/')) {
						p[index++] = ' ';
						p[index++] = ' ';
						endOfRainbow = true;
						break;

					} else {
						// continue blanking this area
						p[index++] = ' ';
					}
				}
				if (!endOfRainbow) {
					throw new RuntimeException("Missing the */ from the end of a " + "/* comment */");
				}
			} else { // any old character, move along
				index++;
			}
		}
		return new String(p);
	}

	public boolean exportApplicationPrompt() throws IOException, RunnerException {
		return false;
	}

	/**
	 * Export to application via GUIService.
	 */
	protected boolean exportApplication() throws IOException, RunnerException {
		return false;
	}

	/**
	 * Export to application without GUIService.
	 */
	public boolean exportApplication(String destPath, int exportPlatform) throws IOException, RunnerException {
		return false;
	}

	/**
	 * Make sure the sketch hasn't been moved or deleted by some nefarious user.
	 * If they did, try to re-create it and save. Only checks to see if the main
	 * folder is still around, but not its contents.
	 */
	protected void ensureExistence() {
		if (folder.exists())
			return;

		showWarning("Sketch Disappeared", "The sketch folder has disappeared.\n " + "Will attempt to re-save in the same location,\n"
				+ "but anything besides the code will be lost.", null);
		try {
			folder.mkdirs();
			modified = true;

			for (int i = 0; i < codeCount; i++) {
				code[i].save(); // this will force a save
			}
			calcModified();

		} catch (Exception e) {
			showWarning("Could not re-save sketch", "Could not properly re-save the sketch. " + "You may be in trouble at this point,\n"
					+ "and it might be time to copy and paste " + "your code to another text editor.", e);
		}
	}

	// . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

	// Breaking out extension types in order to clean up the code, and make it
	// easier for other environments (like Arduino) to incorporate changes.

	/**
	 * True if the specified extension should be hidden when shown on a tab. For
	 * Processing, this is true for .pde files. (Broken out for subclasses.)
	 */
	public boolean hideExtension(String what) {
		return getHiddenExtensions().contains(what);
	}

	/**
	 * True if the specified code has the default file extension.
	 */
	public boolean hasDefaultExtension(SketchCode2 code) {
		return code.getExtension().equals(getDefaultExtension());
	}

	/**
	 * True if the specified extension is the default file extension.
	 */
	public boolean isDefaultExtension(String what) {
		return what.equals(getDefaultExtension());
	}

	/**
	 * Check this extension (no dots, please) against the list of valid
	 * extensions.
	 */
	public boolean validExtension(String what) {
		String[] ext = getExtensions();
		for (int i = 0; i < ext.length; i++) {
			if (ext[i].equals(what))
				return true;
		}
		return false;
	}

	/**
	 * Returns the default extension for this editor setup.
	 */
	public String getDefaultExtension() {
		return "ino";
	}

	static private List<String> hiddenExtensions = Arrays.asList("ino", "pde");

	public List<String> getHiddenExtensions() {
		return hiddenExtensions;
	}

	/**
	 * Returns a String[] array of proper extensions.
	 */
	public String[] getExtensions() {
		return new String[] { "ino", "pde", "c", "cpp", "h" };
	}

	// . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

	// Additional accessors added in 0136 because of package work.
	// These will also be helpful for tool developers.

	/**
	 * Returns the name of this sketch. (The pretty name of the main tab.)
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns a file object for the primary .pde of this sketch.
	 */
	public File getPrimaryFile() {
		return primaryFile;
	}

	/**
	 * Returns path to the main .pde file for this sketch.
	 */
	public String getMainFilePath() {
		return primaryFile.getAbsolutePath();
		// return code[0].file.getAbsolutePath();
	}

	/**
	 * Returns the sketch folder.
	 */
	public File getFolder() {
		return folder;
	}

	/**
	 * Returns the location of the sketch's data folder. (It may not exist yet.)
	 */
	public File getDataFolder() {
		return dataFolder;
	}

	/**
	 * Create the data folder if it does not exist already. As a convenience, it
	 * also returns the data folder, since it's likely about to be used.
	 */
	public File prepareDataFolder() {
		if (!dataFolder.exists()) {
			dataFolder.mkdirs();
		}
		return dataFolder;
	}

	/**
	 * Returns the location of the sketch's code folder. (It may not exist yet.)
	 */
	public File getCodeFolder() {
		return codeFolder;
	}

	/**
	 * Create the code folder if it does not exist already. As a convenience, it
	 * also returns the code folder, since it's likely about to be used.
	 */
	public File prepareCodeFolder() {
		if (!codeFolder.exists()) {
			codeFolder.mkdirs();
		}
		return codeFolder;
	}

	public String getClassPath() {
		return classPath;
	}

	public String getLibraryPath() {
		return libraryPath;
	}

	public SketchCode2[] getCode() {
		return code;
	}

	public int getCodeCount() {
		return codeCount;
	}

	public SketchCode2 getCode(int index) {
		return code[index];
	}

	public int getCodeIndex(SketchCode2 who) {
		for (int i = 0; i < codeCount; i++) {
			if (who == code[i]) {
				return i;
			}
		}
		return -1;
	}

	public SketchCode2 getCurrentCode() {
		return current;
	}

	public String getAppletClassName2() {
		return appletClassName;
	}

	// .................................................................

	/**
	 * Convert to sanitized name and alert the user if changes were made.
	 */
	static public String checkName(String origName) {
		String newName = sanitizeName(origName);

		if (!newName.equals(origName)) {
			String msg = "The sketch name had to be modified. Sketch names can only consist\n" + "of ASCII characters and numbers (but cannot start with a number).\n"
					+ "They should also be less less than 64 characters long.";
			System.out.println(msg);
		}
		return newName;
	}

	/**
	 * Return true if the name is valid for a Processing sketch.
	 */
	static public boolean isSanitaryName(String name) {
		return sanitizeName(name).equals(name);
	}

	/**
	 * Produce a sanitized name that fits our standards for likely to work.
	 * <p/>
	 * Java classes have a wider range of names that are technically allowed
	 * (supposedly any Unicode name) than what we support. The reason for going
	 * more narrow is to avoid situations with text encodings and converting
	 * during the process of moving files between operating systems, i.e.
	 * uploading from a Windows machine to a Linux server, or reading a FAT32
	 * partition in OS X and using a thumb drive.
	 * <p/>
	 * This helper function replaces everything but A-Z, a-z, and 0-9 with
	 * underscores. Also disallows starting the sketch name with a digit.
	 */
	static public String sanitizeName(String origName) {
		char c[] = origName.toCharArray();
		StringBuffer buffer = new StringBuffer();

		// can't lead with a digit, so start with an underscore
		if ((c[0] >= '0') && (c[0] <= '9')) {
			buffer.append('_');
		}
		for (int i = 0; i < c.length; i++) {
			if (((c[i] >= '0') && (c[i] <= '9')) || ((c[i] >= 'a') && (c[i] <= 'z')) || ((c[i] >= 'A') && (c[i] <= 'Z'))) {
				buffer.append(c[i]);

			} else {
				buffer.append('_');
			}
		}
		// let's not be ridiculous about the length of filenames.
		// in fact, Mac OS 9 can handle 255 chars, though it can't really
		// deal with filenames longer than 31 chars in the Finder.
		// but limiting to that for sketches would mean setting the
		// upper-bound on the character limit here to 25 characters
		// (to handle the base name + ".class")
		if (buffer.length() > 63) {
			buffer.setLength(63);
		}
		return buffer.toString();
	}
}
