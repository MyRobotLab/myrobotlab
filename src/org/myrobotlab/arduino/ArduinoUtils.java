package org.myrobotlab.arduino;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Arduino;
import org.slf4j.Logger;

import com.sun.jna.Platform;

public class ArduinoUtils {

	public transient final static Logger log = LoggerFactory.getLogger(ArduinoUtils.class);

	// TODO: auto-discover?
	public static String arduinoPath = "c:\\dev\\arduino-1.6.8\\";
	// TODO: fix this. a temp directory so we can upload the mrlcomm properly.

	public static StringBuilder outputBuilder;

	// not needed ?
	private static String commandPath = "";
	private static String additionalEnv = "";
	public static int exitValue;

	static public String getExeName() {
		if (Platform.isMac()) {
			return "Arduino";
		}

		return "arduino_debug";
	}

	public static boolean uploadSketch(String port, String board) throws IOException, InterruptedException {
		if (!(board.equalsIgnoreCase("uno") || board.equalsIgnoreCase("mega") || board.equalsIgnoreCase("megaADK") || board.equalsIgnoreCase("nano"))) {
			// TODO: validate the proper set of values.
			System.out.println(String.format("Invalid board type:%s", board));
			exitValue = 1;
			return false;
		}
		// Assume this is mrlcomm resource!
		// G-say: FIXME - this will ONLY work in eclipse !!! - should extract it
		// from /resource
		//not working
	  FileIO.extractResources();
		String sketchFilename = "resource/Arduino/MRLComm/MRLComm.ino";
		File sketch = new File(sketchFilename);
		if (!sketch.exists()){
		  sketchFilename = "src/resource/Arduino/MRLComm/MRLComm.ino";
		  sketch = new File(sketchFilename);
		}
		// Create the command to run (and it's args.)
		String arduinoExe = arduinoPath + getExeName();
		ArrayList<String> args = new ArrayList<String>();
		args.add("--upload");
		args.add("--port");
		args.add(port);
		args.add("--board");
		if (board.equalsIgnoreCase("nano")) {
			args.add("arduino:avr:" + board + ":cpu=atmega328");
		}
		else {
			args.add("arduino:avr:" + board);
		}
		args.add(sketch.getAbsolutePath());
		// args.add("--verbose-upload");
		//args.add("--preserve-temp-files");
		// run the command.
		String result = runCommand(arduinoExe, args);
		// print stdout/err from running the command
		log.info("Result..." + result);

		log.info("Uploaded Sketch.");

		// take a breath... We think it probably worked? but not sure..
		
		
		Thread.sleep(2000);
		
		// G-says : the following does not return correctly
		// if it correctly compiles but fails to upload

		if (result.trim().endsWith(" bytes.")) {
			return true;
		} else {
			return false;
		}

	}

	/**
	 * Helper function to run a system command and return the stdout / stderr as
	 * a string
	 * 
	 * @param program
	 * @param args
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	protected static String runCommand(String program, ArrayList<String> args) throws InterruptedException {

		ArrayList<String> command = new ArrayList<String>();
		command.add(program);
		if (args != null) {
			for (String arg : args) {
				command.add(arg);
			}
		}
		System.out.println("RUNNING COMMAND :" + join(command, " "));

		ProcessBuilder builder = new ProcessBuilder(command);
		// we need to specify environment variables
		Map<String, String> environment = builder.environment();

		String ldLibPath = commandPath;
		if (additionalEnv.length() > 0) {
			ldLibPath += ":" + additionalEnv;
		}

		environment.put("LD_LIBRARY_PATH", ldLibPath);
		try {
			Process handle = builder.start();

			InputStream stdErr = handle.getErrorStream();
			InputStream stdOut = handle.getInputStream();

			// TODO: we likely don't need this
			// OutputStream stdIn = handle.getOutputStream();

			outputBuilder = new StringBuilder();
			byte[] buff = new byte[4096];

			// TODO: should we read both of these streams?
			// if we break out of the first loop is the process terminated?

			// read stderr
			for (int n; (n = stdErr.read(buff)) != -1;) {
				outputBuilder.append(new String(buff, 0, n));
			}
			// read stdout
			for (int n; (n = stdOut.read(buff)) != -1;) {
				outputBuilder.append(new String(buff, 0, n));
			}

			stdOut.close();
			stdErr.close();

			// TODO: stdin if we use it.
			// stdIn.close();

			// the process should be closed by now?

			handle.waitFor();

			handle.destroy();

			exitValue = handle.exitValue();
			// print the output from the command
			System.out.println(outputBuilder.toString());
			System.out.println("Exit Value : " + exitValue);
			outputBuilder.append("Exit Value : " + exitValue);

			return outputBuilder.toString();
		} catch (IOException e) {
			exitValue = 5;
			return e.getMessage();
			// throw e;
		}
	}

	/**
	 * Helper function to run a program , return the stderr / stdout as a string
	 * and to catch any exceptions that occur
	 * 
	 * @param cmd
	 * @param args
	 * @return
	 */
	protected String RunAndCatch(String cmd, ArrayList<String> args) {
		String returnValue;
		try {
			returnValue = runCommand(cmd, args);
			// }
			// catch (IOException e) {
			// // TODO Auto-generated catch block
			// returnValue = e.getMessage();
			// e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			returnValue = e.getMessage();
			e.printStackTrace();
		}
		return returnValue;
	}

	// TODO: this should be on a string utils static class.
	private static String join(ArrayList<String> list, String joinChar) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		int size = list.size();
		for (String part : list) {
			i++;
			sb.append(part);
			if (i != size) {
				sb.append(joinChar);
			}
		}
		return sb.toString();
	}

	public static String getOutput() {
		return outputBuilder.toString();
	}

}
