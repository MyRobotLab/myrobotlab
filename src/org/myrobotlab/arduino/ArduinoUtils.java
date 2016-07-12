package org.myrobotlab.arduino;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

public class ArduinoUtils {

  // TODO: auto-discover?
  public static String arduinoPath = "c:\\dev\\arduino-1.6.8\\";
  // TODO: fix this. a temp directory so we can upload the mrlcomm properly.
  private static String arduinoExecutable = "arduino";
  // not needed ?
  private static String commandPath = "";
  private static String additionalEnv = "";

  public static boolean uploadSketch(String port, String board) throws IOException, InterruptedException {
    if (!(board.equalsIgnoreCase("uno") || board.equalsIgnoreCase("mega"))) {
      // TODO: validate the proper set of values.
      System.out.println(String.format("Invalid board type:%s",board));
      return false;
    }
    // Assume this is mrlcomm resource!
    String sketchFilename = "src\\resource\\Arduino\\MRLComm\\MRLComm.ino";
    File sketch = new File(sketchFilename);
    // Create the command to run (and it's args.)
    String arduinoExe = arduinoPath + arduinoExecutable;
    ArrayList<String> args = new ArrayList<String>();
    // args.add("--verbose");
    args.add("--upload");
    args.add("--port");
    args.add(port);
    args.add("--board");
    args.add("arduino:avr:" + board.toLowerCase());
    args.add(sketch.getAbsolutePath());
    // run the command.
    String result = runCommand(arduinoExe, args);
    // print stdout/err from running the command
    System.out.println("Result..." + result);

    System.out.println("Uploaded Sketch.");
    System.out.flush();
    // take a breath...  We think it probably worked?  but not sure..
    Thread.sleep(2000);
    
    if (result.trim().endsWith(" bytes.")) {
      return true;
    } else {
      return false;
    }
    
    
  }



  /**
   * Helper function to run a system command and return the stdout / stderr as a string
   * 
   * @param program
   * @param args
   * @return
   * @throws IOException
   * @throws InterruptedException
   */
  protected static String runCommand(String program , ArrayList<String> args) throws IOException, InterruptedException {

    ArrayList<String> command = new ArrayList<String>();
    command.add(program);
    if (args != null) {
      for (String arg : args) {
        command.add(arg);
      }
    }
    System.out.println("RUNNING COMMAND :" + join(command," "));

    ProcessBuilder builder = new ProcessBuilder(command);
    // we need to specify environment variables
    Map<String, String> environment = builder.environment();

    String ldLibPath = commandPath;
    if (additionalEnv.length() >0) {
      ldLibPath += ":" + additionalEnv;
    }

    environment.put("LD_LIBRARY_PATH", ldLibPath);
    Process handle = builder.start();

    InputStream stdErr = handle.getErrorStream();
    InputStream stdOut = handle.getInputStream();

    // TODO: we likely don't need this
    // OutputStream stdIn = handle.getOutputStream();

    StringBuilder outputBuilder = new StringBuilder();
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

    int exitValue = handle.exitValue();
    // print the output from the command
    System.out.println(outputBuilder.toString());
    System.out.println("Exit Value : " + exitValue);

    return outputBuilder.toString();
  }

  /**
   * Helper function to run a program , return the stderr / stdout as a string and 
   * to catch any exceptions that occur
   * 
   * @param cmd
   * @param args
   * @return
   */
  protected String RunAndCatch(String cmd, ArrayList<String> args) {
    String returnValue;
    try {
      returnValue = runCommand(cmd, args);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      returnValue = e.getMessage();
      e.printStackTrace();
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


}
