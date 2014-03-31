/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
 Part of the Processing project - http://processing.org

 Copyright (c) 2004-08 Ben Fry and Casey Reas
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

 GroG - removed all Swing, Swing references & imports of
 classes which required AWT or Swing - no gui components !

 Compiler without dependencies on imports of Graphic related components,
 such as Base, Editor, Sketch, etc..

 */

package org.myrobotlab.arduino.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.myrobotlab.arduino.PApplet;
import org.myrobotlab.service.Arduino;

public class Compiler {
	static final String BUGS_URL = "http://code.google.com/p/arduino/issues/list";
	static final String SUPER_BADNESS = "Compiler error, please submit this code to " + BUGS_URL;

	String program;
	String buildPath;
	String programName;
	boolean verbose;

	Arduino myArduino = null;

	RunnerException exception;

	/**
	 * This is *not* the "Processing" libraries path, this is the Java libraries
	 * path, as in java.library.path=BlahBlah, which identifies search paths for
	 * DLLs or JNILIBs. (bullshit) did I say bullshit? - this variable is never
	 * even read ... ever .. i mean wtf ?
	 */
	private String libraryPath;

	// preprocessor
	private ArrayList<File> importedLibraries;

	public Compiler(Arduino myArduino) {
		this.myArduino = myArduino;
	}

	/**
	 * Compile with avr-gcc.
	 * 
	 * @param program
	 *            Sketch object to be compiled.
	 * @param buildPath
	 *            Where the temporary files live and will be built from.
	 * @param programName
	 *            the name of the combined sketch file w/ extension
	 * @return true if successful.
	 * @throws RunnerException
	 *             Only if there's a problem. Only then.
	 */
	public boolean compile(String programName, String program, String buildPath, boolean verbose) throws RunnerException {
		this.program = program;
		this.buildPath = buildPath;
		this.programName = programName;
		this.verbose = verbose;

		preprocess(programName, program, new PdePreprocessor(myArduino));

		String avrBasePath = Arduino.getAvrBasePath();
		Map<String, String> boardPreferences = myArduino.getBoardPreferences();
		String core = boardPreferences.get("build.core");
		if (core == null) {
			RunnerException re = new RunnerException("No board selected; please choose a board from the Tools > Board menu.");
			re.hideStackTrace();
			throw re;
		}
		String corePath;

		if (core.indexOf(':') == -1) {
			Target t = myArduino.getTarget();
			File coreFolder = new File(new File(t.getFolder(), "cores"), core);
			corePath = coreFolder.getAbsolutePath();
		} else {
			Target t = myArduino.targetsTable.get(core.substring(0, core.indexOf(':')));
			File coreFolder = new File(t.getFolder(), "cores");
			coreFolder = new File(coreFolder, core.substring(core.indexOf(':') + 1));
			corePath = coreFolder.getAbsolutePath();
		}

		String variant = boardPreferences.get("build.variant");
		String variantPath = null;

		if (variant != null) {
			if (variant.indexOf(':') == -1) {
				Target t = myArduino.getTarget();
				File variantFolder = new File(new File(t.getFolder(), "variants"), variant);
				variantPath = variantFolder.getAbsolutePath();
			} else {
				Target t = myArduino.targetsTable.get(variant.substring(0, variant.indexOf(':')));
				File variantFolder = new File(t.getFolder(), "variants");
				variantFolder = new File(variantFolder, variant.substring(variant.indexOf(':') + 1));
				variantPath = variantFolder.getAbsolutePath();
			}
		}

		List<File> objectFiles = new ArrayList<File>();

		// 0. include paths for core + all libraries

		myArduino.setCompilingProgress(20);
		List includePaths = new ArrayList();
		includePaths.add(corePath);
		if (variantPath != null)
			includePaths.add(variantPath);
		for (File file : getImportedLibraries()) {
			includePaths.add(file.getPath());
		}

		// 1. compile the sketch (already in the buildPath)

		myArduino.setCompilingProgress(30);
		objectFiles.addAll(compileFiles(avrBasePath, buildPath, includePaths, findFilesInPath(buildPath, "S", false), findFilesInPath(buildPath, "c", false),
				findFilesInPath(buildPath, "cpp", false), boardPreferences));

		// 2. compile the libraries, outputting .o files to:
		// <buildPath>/<library>/

		myArduino.setCompilingProgress(40);
		for (File libraryFolder : getImportedLibraries()) {
			File outputFolder = new File(buildPath, libraryFolder.getName());
			File utilityFolder = new File(libraryFolder, "utility");
			createFolder(outputFolder);
			// this library can use includes in its utility/ folder
			includePaths.add(utilityFolder.getAbsolutePath());
			objectFiles.addAll(compileFiles(avrBasePath, outputFolder.getAbsolutePath(), includePaths, findFilesInFolder(libraryFolder, "S", false),
					findFilesInFolder(libraryFolder, "c", false), findFilesInFolder(libraryFolder, "cpp", false), boardPreferences));
			outputFolder = new File(outputFolder, "utility");
			createFolder(outputFolder);
			objectFiles.addAll(compileFiles(avrBasePath, outputFolder.getAbsolutePath(), includePaths, findFilesInFolder(utilityFolder, "S", false),
					findFilesInFolder(utilityFolder, "c", false), findFilesInFolder(utilityFolder, "cpp", false), boardPreferences));
			// other libraries should not see this library's utility/ folder
			includePaths.remove(includePaths.size() - 1);
		}

		// 3. compile the core, outputting .o files to <buildPath> and then
		// collecting them into the core.a library file.

		myArduino.setCompilingProgress(50);
		includePaths.clear();
		includePaths.add(corePath); // include path for core only
		if (variantPath != null)
			includePaths.add(variantPath);
		List<File> coreObjectFiles = compileFiles(avrBasePath, buildPath, includePaths, findFilesInPath(corePath, "S", true), findFilesInPath(corePath, "c", true),
				findFilesInPath(corePath, "cpp", true), boardPreferences);

		String runtimeLibraryName = buildPath + File.separator + "core.a";
		List baseCommandAR = new ArrayList(Arrays.asList(new String[] { avrBasePath + "avr-ar", "rcs", runtimeLibraryName }));
		for (File file : coreObjectFiles) {
			List<String> commandAR = new ArrayList<String>(baseCommandAR);
			commandAR.add(file.getAbsolutePath());
			execAsynchronously(commandAR);
		}

		// 4. link it all together into the .elf file

		myArduino.setCompilingProgress(60);
		List<String> baseCommandLinker = new ArrayList<String>(Arrays.asList(new String[] { avrBasePath + "avr-gcc", "-Os", "-Wl,--gc-sections",
				"-mmcu=" + boardPreferences.get("build.mcu"), "-o", buildPath + File.separator + programName + ".elf" }));

		for (File file : objectFiles) {
			baseCommandLinker.add(file.getAbsolutePath());
		}

		baseCommandLinker.add(runtimeLibraryName);
		baseCommandLinker.add("-L" + buildPath);
		baseCommandLinker.add("-lm");

		execAsynchronously(baseCommandLinker);

		List<String> baseCommandObjcopy = new ArrayList<String>(Arrays.asList(new String[] { avrBasePath + "avr-objcopy", "-O", "-R", }));

		ArrayList<String> commandObjcopy;

		// 5. extract EEPROM data (from EEMEM directive) to .eep file.
		myArduino.setCompilingProgress(70);
		commandObjcopy = new ArrayList<String>(baseCommandObjcopy);
		commandObjcopy.add(2, "ihex");
		commandObjcopy.set(3, "-j");
		commandObjcopy.add(".eeprom");
		commandObjcopy.add("--set-section-flags=.eeprom=alloc,load");
		commandObjcopy.add("--no-change-warnings");
		commandObjcopy.add("--change-section-lma");
		commandObjcopy.add(".eeprom=0");
		commandObjcopy.add(buildPath + File.separator + programName + ".elf");
		commandObjcopy.add(buildPath + File.separator + programName + ".eep");
		execAsynchronously(commandObjcopy);

		// 6. build the .hex file
		myArduino.setCompilingProgress(80);
		commandObjcopy = new ArrayList(baseCommandObjcopy);
		commandObjcopy.add(2, "ihex");
		commandObjcopy.add(".eeprom"); // remove eeprom data
		commandObjcopy.add(buildPath + File.separator + programName + ".elf");
		commandObjcopy.add(buildPath + File.separator + programName + ".hex");
		execAsynchronously(commandObjcopy);

		myArduino.setCompilingProgress(90);

		myArduino.setCompilingProgress(100);
		myArduino.message("done.");

		return true;
	}

	private List<File> compileFiles(String avrBasePath, String buildPath, List<File> includePaths, List<File> sSources, List<File> cSources, List<File> cppSources,
			Map<String, String> boardPreferences) throws RunnerException {

		List<File> objectPaths = new ArrayList<File>();

		for (File file : sSources) {
			String objectPath = buildPath + File.separator + file.getName() + ".o";
			objectPaths.add(new File(objectPath));
			execAsynchronously(getCommandCompilerS(avrBasePath, includePaths, file.getAbsolutePath(), objectPath, boardPreferences));
		}

		for (File file : cSources) {
			String objectPath = buildPath + File.separator + file.getName() + ".o";
			objectPaths.add(new File(objectPath));
			execAsynchronously(getCommandCompilerC(avrBasePath, includePaths, file.getAbsolutePath(), objectPath, boardPreferences));
		}

		for (File file : cppSources) {
			String objectPath = buildPath + File.separator + file.getName() + ".o";
			objectPaths.add(new File(objectPath));
			execAsynchronously(getCommandCompilerCPP(avrBasePath, includePaths, file.getAbsolutePath(), objectPath, boardPreferences));
		}

		return objectPaths;
	}

	boolean firstErrorFound;
	boolean secondErrorFound;

	/**
	 * Either succeeds or throws a RunnerException fit for public consumption.
	 */
	private void execAsynchronously(List commandList) throws RunnerException {
		String[] command = new String[commandList.size()];
		commandList.toArray(command);
		int result = 0;

		if (verbose || myArduino.preferences.getBoolean("build.verbose")) {
			String avrCmd = "";
			for (int j = 0; j < command.length; j++) {
				avrCmd += command[j] + " ";
			}
			myArduino.message(String.format("%s\n", avrCmd));
		}

		firstErrorFound = false; // haven't found any errors yet
		secondErrorFound = false;

		Process process;

		// command = new String[]{"ls.exe"};
		try {
			process = Runtime.getRuntime().exec(command);
		} catch (IOException e) {
			RunnerException re = new RunnerException(e.getMessage());
			re.hideStackTrace();
			throw re;
		}

		MessageSiphon in = new MessageSiphon(process.getInputStream(), myArduino);
		MessageSiphon err = new MessageSiphon(process.getErrorStream(), myArduino);

		// wait for the process to finish. if interrupted
		// before waitFor returns, continue waiting
		boolean compiling = true;
		while (compiling) {
			try {
				if (in.thread != null)
					in.thread.join();
				if (err.thread != null)
					err.thread.join();
				result = process.waitFor();
				// myArduino.message("result is " + result);
				compiling = false;
			} catch (InterruptedException ignored) {
			}
		}

		// FIXME - garbage - inducing program control via re-throwing exceptions
		// - very ugly !

		// an error was queued up by message(), barf this back to compile(),
		// which will barf it back to Editor. if you're having trouble
		// discerning the imagery, consider how cows regurgitate their food
		// to digest it, and the fact that they have five stomaches.
		//
		// System.out.println("throwing up " + exception);
		if (exception != null) {
			throw exception;
		}

		if (result > 1) {
			// a failure in the tool (e.g. unable to locate a sub-executable)
			System.err.println(command[0] + " returned " + result);
		}

		if (result != 0) {
			RunnerException re = new RunnerException("Error compiling.");
			re.hideStackTrace();
			throw re;
		}
	}

	/**
	 * Part of the MessageConsumer interface, this is called whenever a piece
	 * (usually a line) of error message is spewed out from the compiler. The
	 * errors are parsed for their contents and line number, which is then
	 * reported back to Editor.
	 */
	public void message(String s) {
		int i;

		// remove the build path so people only see the filename
		// can't use replaceAll() because the path may have characters in it
		// which
		// have meaning in a regular expression.
		if (!verbose) {
			while ((i = s.indexOf(buildPath + File.separator)) != -1) {
				s = s.substring(0, i) + s.substring(i + (buildPath + File.separator).length());
			}
		}

		// look for error line, which contains file name, line number,
		// and at least the first line of the error message
		String errorFormat = "([\\w\\d_]+.\\w+):(\\d+):\\s*error:\\s*(.*)\\s*";
		String[] pieces = PApplet.match(s, errorFormat);

		// if (pieces != null && exception == null) {
		// exception = sketch.placeException(pieces[3], pieces[1],
		// PApplet.parseInt(pieces[2]) - 1);
		// if (exception != null) exception.hideStackTrace();
		// }

		if (pieces != null) {
			String error = pieces[3], msg = "";

			if (pieces[3].trim().equals("SPI.h: No such file or directory")) {
				error = "Please import the SPI library from the Sketch > Import Library menu.";
				msg = "\nAs of Arduino 0019, the Ethernet library depends on the SPI library."
						+ "\nYou appear to be using it or another library that depends on the SPI library.\n\n";
			}

			if (pieces[3].trim().equals("'BYTE' was not declared in this scope")) {
				error = "The 'BYTE' keyword is no longer supported.";
				msg = "\nAs of Arduino 1.0, the 'BYTE' keyword is no longer supported." + "\nPlease use Serial.write() instead.\n\n";
			}

			if (pieces[3].trim().equals("no matching function for call to 'Server::Server(int)'")) {
				error = "The Server class has been renamed EthernetServer.";
				msg = "\nAs of Arduino 1.0, the Server class in the Ethernet library " + "has been renamed to EthernetServer.\n\n";
			}

			if (pieces[3].trim().equals("no matching function for call to 'Client::Client(byte [4], int)'")) {
				error = "The Client class has been renamed EthernetClient.";
				msg = "\nAs of Arduino 1.0, the Client class in the Ethernet library " + "has been renamed to EthernetClient.\n\n";
			}

			if (pieces[3].trim().equals("'Udp' was not declared in this scope")) {
				error = "The Udp class has been renamed EthernetUdp.";
				msg = "\nAs of Arduino 1.0, the Udp class in the Ethernet library " + "has been renamed to EthernetClient.\n\n";
			}

			if (pieces[3].trim().equals("'class TwoWire' has no member named 'send'")) {
				error = "Wire.send() has been renamed Wire.write().";
				msg = "\nAs of Arduino 1.0, the Wire.send() function was renamed " + "to Wire.write() for consistency with other libraries.\n\n";
			}

			if (pieces[3].trim().equals("'class TwoWire' has no member named 'receive'")) {
				error = "Wire.receive() has been renamed Wire.read().";
				msg = "\nAs of Arduino 1.0, the Wire.receive() function was renamed " + "to Wire.read() for consistency with other libraries.\n\n";
			}

			// RunnerException e = program.placeException(error, pieces[1],
			// PApplet.parseInt(pieces[2]) - 1);

			// replace full file path with the name of the sketch tab (unless
			// we're
			// in verbose mode, in which case don't modify the compiler output)
			/*
			 * if (e != null && !verbose) { SketchCode code =
			 * sketch.getCode(e.getCodeIndex()); String fileName =
			 * code.isExtension(sketch.getDefaultExtension()) ?
			 * code.getPrettyName() : code.getFileName(); s = fileName + ":" +
			 * e.getCodeLine() + ": error: " + pieces[3] + msg; }
			 */
			/*
			 * if (exception == null && e != null) { exception = e;
			 * exception.hideStackTrace(); }
			 */
		}

		myArduino.invoke("compilerError", s);
		System.err.print(s);
	}

	// ///////////////////////////////////////////////////////////////////////////

	static private List getCommandCompilerS(String avrBasePath, List includePaths, String sourceName, String objectName, Map<String, String> boardPreferences) {
		List baseCommandCompiler = new ArrayList(Arrays.asList(new String[] { avrBasePath + "avr-gcc", "-c", // compile,
																												// don't
																												// link
				"-g", // include debugging info (so errors include line numbers)
				"-assembler-with-cpp", "-mmcu=" + boardPreferences.get("build.mcu"), "-DF_CPU=" + boardPreferences.get("build.f_cpu"), "-DARDUINO=" + Arduino.REVISION, }));

		for (int i = 0; i < includePaths.size(); i++) {
			baseCommandCompiler.add("-I" + (String) includePaths.get(i));
		}

		baseCommandCompiler.add(sourceName);
		baseCommandCompiler.add("-o" + objectName);

		return baseCommandCompiler;
	}

	private List getCommandCompilerC(String avrBasePath, List includePaths, String sourceName, String objectName, Map<String, String> boardPreferences) {

		List baseCommandCompiler = new ArrayList(Arrays.asList(new String[] { avrBasePath + "avr-gcc", "-c", // compile,
																												// don't
																												// link
				"-g", // include debugging info (so errors include line numbers)
				"-Os", // optimize for size
				myArduino.preferences.getBoolean("build.verbose") ? "-Wall" : "-w", // show
				// warnings
				// if
				// verbose
				"-ffunction-sections", // place each function in its own section
				"-fdata-sections", "-mmcu=" + boardPreferences.get("build.mcu"), "-DF_CPU=" + boardPreferences.get("build.f_cpu"), "-DARDUINO=" + Arduino.REVISION, }));

		for (int i = 0; i < includePaths.size(); i++) {
			baseCommandCompiler.add("-I" + (String) includePaths.get(i));
		}

		baseCommandCompiler.add(sourceName);
		baseCommandCompiler.add("-o" + objectName);

		return baseCommandCompiler;
	}

	private List getCommandCompilerCPP(String avrBasePath, List includePaths, String sourceName, String objectName, Map<String, String> boardPreferences) {

		List baseCommandCompilerCPP = new ArrayList(Arrays.asList(new String[] { avrBasePath + "avr-g++", "-c", // compile,
																												// don't
																												// link
				"-g", // include debugging info (so errors include line numbers)
				"-Os", // optimize for size
				myArduino.preferences.getBoolean("build.verbose") ? "-Wall" : "-w", // show
				// warnings
				// if
				// verbose
				"-fno-exceptions", "-ffunction-sections", // place each function
															// in its own
															// section
				"-fdata-sections", "-mmcu=" + boardPreferences.get("build.mcu"), "-DF_CPU=" + boardPreferences.get("build.f_cpu"), "-DARDUINO=" + Arduino.REVISION, }));

		for (int i = 0; i < includePaths.size(); i++) {
			baseCommandCompilerCPP.add("-I" + (String) includePaths.get(i));
		}

		baseCommandCompilerCPP.add(sourceName);
		baseCommandCompilerCPP.add("-o" + objectName);

		return baseCommandCompilerCPP;
	}

	// ///////////////////////////////////////////////////////////////////////////

	static private void createFolder(File folder) throws RunnerException {
		if (folder.isDirectory())
			return;
		if (!folder.mkdir())
			throw new RunnerException("Couldn't create: " + folder);
	}

	/**
	 * Given a folder, return a list of the header files in that folder (but not
	 * the header files in its sub-folders, as those should be included from
	 * within the header files at the top-level).
	 */
	static public String[] headerListFromIncludePath(String path) {
		FilenameFilter onlyHFiles = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".h");
			}
		};

		return (new File(path)).list(onlyHFiles);
	}

	static public ArrayList<File> findFilesInPath(String path, String extension, boolean recurse) {
		return findFilesInFolder(new File(path), extension, recurse);
	}

	static public ArrayList<File> findFilesInFolder(File folder, String extension, boolean recurse) {
		ArrayList<File> files = new ArrayList<File>();

		if (folder.listFiles() == null)
			return files;

		for (File file : folder.listFiles()) {
			if (file.getName().startsWith("."))
				continue; // skip hidden files

			if (file.getName().endsWith("." + extension))
				files.add(file);

			if (recurse && file.isDirectory()) {
				files.addAll(findFilesInFolder(file, extension, true));
			}
		}

		return files;
	}

	public String preprocess(String programName, String program, PdePreprocessor preprocessor) throws RunnerException {

		String[] codeFolderPackages = null;
		// classPath = buildPath;

		// 1. concatenate all .pde files to the 'main' pde
		// store line number for starting point of each code bit
		// Note that the headerOffset isn't applied until compile and run,
		// because
		// it only applies to the code after it's been written to the .java
		// file.
		int headerOffset = 0;

		try {
			headerOffset = preprocessor.writePrefix(program, buildPath, programName, codeFolderPackages);
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
			String msg = "Build folder disappeared or could not be written";
			throw new RunnerException(msg);
		}

		// 2. run preproc on that code using the sugg class name
		// to create a single .java file and write to buildpath

		String CPPFilename = null;

		try {

			preprocessor.write();
			CPPFilename = programName + ".cpp";

		} catch (FileNotFoundException fnfe) { // FIXME - simple exception
												// handling + feedback to gui
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
		libraryPath = "";

		for (String item : preprocessor.getExtraImports()) {
			File libFolder = (File) Arduino.importToLibraryTable.get(item);

			if (libFolder != null && !importedLibraries.contains(libFolder)) {
				importedLibraries.add(libFolder);
				// classPath += Compiler.contentsToClassPath(libFolder);
				libraryPath += File.pathSeparator + libFolder.getAbsolutePath();
			}
		}

		// 3. then loop over the code[] and save each .java file

		/*
		 * try { Arduino.saveFile(program, new File(buildPath,
		 * primaryClassName)); } catch (IOException e1) { // TODO Auto-generated
		 * catch block e1.printStackTrace(); }
		 */

		/*
		 * for (SketchCode2 sc : code) { if (sc.isExtension("c") ||
		 * sc.isExtension("cpp") || sc.isExtension("h")) { // no pre-processing
		 * services necessary for java files // just write the the contents of
		 * 'program' to a .java file // into the build directory. uses byte
		 * stream and reader/writer // shtuff so that unicode bunk is properly
		 * handled String filename = sc.getFileName(); // code[i].name +
		 * ".java"; try { Arduino.saveFile(sc.getProgram(), new File(buildPath,
		 * filename)); } catch (IOException e) { e.printStackTrace(); throw new
		 * RunnerException("Problem moving " + filename +
		 * " to the build folder"); } // sc.setPreprocName(filename);
		 * 
		 * } else if (sc.isExtension("ino") || sc.isExtension("pde")) { // The
		 * compiler and runner will need this to have a proper // offset
		 * sc.addPreprocOffset(headerOffset); } }
		 */
		return CPPFilename;
	}

	public ArrayList<File> getImportedLibraries() {
		return importedLibraries;
	}
}
