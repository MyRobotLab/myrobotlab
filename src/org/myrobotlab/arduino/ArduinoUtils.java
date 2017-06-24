package org.myrobotlab.arduino;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class ArduinoUtils {

  public transient final static Logger log = LoggerFactory.getLogger(ArduinoUtils.class);

  // TODO: auto-discover?
  public static String arduinoPath = "C:\\Program Files (x86)\\Arduino\\";
  // TODO: fix this. a temp directory so we can upload the mrlcomm properly.

  public static StringBuilder outputBuilder;

  // not needed ?
  private static String commandPath = "";
  private static String additionalEnv = "";
  public static int exitValue;

  static public String getExeName() {
    Platform platform = Platform.getLocalInstance();
    if (platform.isMac()) {
      return "Arduino"; // really it's capitalized ?
    }
    /*
     * if (platform.isLinux()) { return "arduino"; }
     */

    return "arduino";
  }

  /**
   * Upload the MrlComm.ino with an arduino ide (version 1.6.8) installed in the default windows location of 
   * C:\Program Files (x86)\Arduino\
   * 
   * @param port - the com port for the arduino
   * @param boardKey - the board type mega/uno
   * @return true if the upload was successful, otherwise false.
   * @throws IOException - if there is an error talking to the com port
   * @throws InterruptedException - if interrupted by a thread
   */
  public static boolean uploadSketch(String port, String boardKey) throws IOException, InterruptedException {
    return uploadSketch(port, boardKey, arduinoPath);
  }
  
  
  /**
   * Upload the MrlComm.ino sketch to the using an arduino IDE  (only version 1.6.8 has been tested for this method.)
   * 
   * @param port the com port  (COM4,  /dev/ttyAMA0  ...)
   * @param boardKey - the board type mega / uno
   * @param arduinoPath - path to the arduino ide installation
   * @return true if successful, false otherwise
   * @throws IOException - if there's a problem reading the source ino sketch or talking to the com port
   * @throws InterruptedException - if interrupted.
   */
  public static boolean uploadSketch(String port, String boardKey, String arduinoPath) throws IOException, InterruptedException {
    FileIO.extractResources();
    String sketchFilename = "resource/Arduino/MRLComm/MRLComm.ino";
    File sketch = new File(sketchFilename);
    if (!sketch.exists()) {
      // trying to use development version
      sketchFilename = "src/resource/Arduino/MRLComm/MRLComm.ino";
      sketch = new File(sketchFilename);
    }
    // Create the command to run (and it's args.)
    String arduinoExe = arduinoPath + getExeName();
    ArrayList<String> args = new ArrayList<String>();
    args.add("--upload");
    // args.add("--verbose-upload");
    // args.add("--verbose");
    args.add("--port");
    args.add(port);
    args.add("--board");
    
    String[] parts = boardKey.split("\\.");
    if (parts.length > 1) {
      args.add("arduino:avr:" + parts[0] + ":cpu=" + parts[1]);
    } else {
      args.add("arduino:avr:" + parts[0]);
    }
    
    args.add(sketch.getAbsolutePath());
    // args.add("--verbose-upload");
    // args.add("--preserve-temp-files");
    // run the command.
    String result = runCommand(arduinoExe, args);
    // print stdout/err from running the command
    log.info("Result..." + result);

    log.info("Uploaded Sketch.");

    // take a breath... We think it probably worked? but not sure..

    Thread.sleep(2000);

    // G-says : the following does not return correctly
    // if it correctly compiles but fails to upload

    // if (result.trim().endsWith(" bytes.")) {
    if (result.contains("Sketch uses")){
      return true;
    } else {
      return false;
    }

  }

  /**
   * Helper function to run a system command and return the stdout / stderr as a
   * string
   * 
   * @param program - the path to the executible to run
   * @param args - the list of arguments to be passed to the program
   * @return - returns the stdout and stderr as a string
   * @throws InterruptedException - if interrupted
   */
  public static String runCommand(String program, ArrayList<String> args) throws InterruptedException {

    ArrayList<String> command = new ArrayList<String>();
    command.add(program);
    if (args != null) {
      for (String arg : args) {
        command.add(arg);
      }
    }
    log.info("RUNNING COMMAND : {}" , StringUtils.join(command, " "));
    System.out.println();

    ProcessBuilder builder = new ProcessBuilder(command);
    // we need to specify environment variables
    Map<String, String> environment = builder.environment();

    String ldLibPath = commandPath;
    if (additionalEnv.length() > 0) {
      ldLibPath += ":" + additionalEnv;
    }

    environment.put("LD_LIBRARY_PATH", ldLibPath);
    // on windows i need to append to the path

    // TODO: move run command into it's own util class
    // TODO: allow people to pass in environment variables in the method sig
    String path = environment.get("Path");
    if (path != null) {
      path += ";.\\mimic";
      environment.put("Path", path);
    }

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
   * @param cmd - the path to the program to run
   * @param args - a list of args to pass in
   * @return - the stdout/stderr from the underlying process
   */
  public static String RunAndCatch(String cmd, ArrayList<String> args) {
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

  public static String getOutput() {
    return outputBuilder.toString();
  }

}
