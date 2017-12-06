package org.myrobotlab.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.codec.Api;
import org.myrobotlab.codec.ApiFactory;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * Cli - This is a command line interface to MyRobotLab. It supports some shell
 * like commands such as "cd", and "ls" Use the command "help" to display help.
 *
 * should be a singleton in a process. This does not seem to work on
 * cygwin/windows, but it does work in a command prompt. Linux/Mac can use this
 * via a Terminal / Console window.
 * 
 * @author GroG
 *
 */
public class Cli extends Service {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(Cli.class);

  public final static String cd = "cd";
  public final static String pwd = "pwd";
  public final static String ls = "ls";
  public final static String lp = "lp"; // list pipes
  public final static String help = "help";
  public final static String question = "?";

  transient ApiFactory api = null;

  /**
   * pipes from other processes - possibly added by the Agent service our stdin
   * is attached to a pipe and when we attach to that service our stdin becomes
   * that processes std - and its stdout links to our stdout
   */
  Map<String, Pipe> pipes = new HashMap<String, Pipe>();

  // my "real" std:in & std:out
  transient Decoder in;
  transient OutputStream myOutstream;

  ArrayList<String> history = new ArrayList<String>();

  String cwd = "/";
  String prompt = "#";

  /**
   * remote process's name - if attached
   */
  String attachedProcessName = null;

  /**
   * input to remote process if attached
   */
  transient OutputStream attachedIn = null;
  transient Pipe attachedPipe = null;

  public class CliOutputStream extends OutputStream {

    @Override
    public void write(int b) throws IOException {
      // lowest common denominator write .. with INTS no less !
      log.info("here");
    }

    @Override
    public void write(byte[] data) throws IOException {

      // writing to stdout - daemon
      // we don't write if its a daemon, because
      // it will block forever if forked with &
      // because System.out is borked when forked ?
      // if (!Runtime.isDaemon()){
      System.out.write(data);
      // }

      // publishing stdout
      invoke("stdout", data);
    }

  }

  // ================= Decoder Begin =================
  // FIXME - tab to autoComplete ! - up arrow
  // FIXME up/down arrow history
  // FIXME - needs refactor / merge with StreamGobbler
  // FIXME - verify you can pipe commands !
  // FIXME - THIS CONCEPT IS SOOOOOO IMPORTANT
  // - its a Central Point Controller - where input (any InputStream) can send
  // data to be decoded on a very common API e.g. (proto
  // scheme)(host)/api/inputEncoding/responseEncoding/instance/(method)/(params...)
  // Agent + (RemoteAdapter/WebGui/Netosphere) + Cli(command processor part
  // with InStream/OutStream) - is most Big-Fu !
  public class Decoder extends Thread {
    // public String cwd = "/"; CHANGED THIS - it now is GLOBAL - :P
    boolean isRunning = false;
    transient Cli cli;
    transient InputStream is;
    // TODO ecoding defaults & methods to change
    // FIXME - need reference to OutputStream to return
    String inputEncoding = CodecUtils.TYPE_URI; // REST JSON
    String outputEncoding = CodecUtils.TYPE_JSON; // JSON / JSON MSG

    public Decoder(Cli cli, InputStream is) {
      super(String.format("%s-stdin-decoder", cli.getName()));
      this.cli = cli;
      this.is = is;
    }

    @Override
    public void run() {
      isRunning = true;
      try {
        while (isRunning) {

          StringBuffer sb = new StringBuffer();
          writePrompt();
          String line = null;
          int c = 0;

          while ((c = is.read()) != -1) {

            // handle up arrow - tab autoComplet - and regular \n
            // if not one of these - then append the next character
            if (c != '\n') {
              sb.append((char) c);
              continue;
            } else {
              line = sb.toString();
              sb.setLength(0);
            }
            line = line.trim();
            cli.process(line);
          } // end while
          log.error("cli input stream is closed - exiting");
          isRunning = false;
        }
      } catch (Exception e) {
        log.info("terminating listening on input stream");
        isRunning = false;
      }
    }
  }

  // ================= Decoder End =================

  public void writePrompt() throws IOException {
    write(getPrompt().getBytes());
  }

  public String getPrompt() {
    return String.format("%s:%s%s ", Runtime.getInstance().getName(), cwd, prompt);
  }

  public void clear() {
    try {
      final String os = System.getProperty("os.name");

      if (os.contains("Windows")) {
        // java.lang.Runtime.getRuntime().exec("cls");
        new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
      } else {
        java.lang.Runtime.getRuntime().exec("clear");
      }
    } catch (final Exception e) {
      // Handle any exceptions.
    }
  }

  /**
   * processes input from incoming streams
   * 
   * @param line
   *          - line of data from the stream
   * @throws IOException
   *           - can throw from damaged io stream
   */
  public void process(String line) throws IOException {

    // order of precedence
    // 1. execute cli methods
    // 2. execute service methods
    // 3. execute Runtime methods

    if (line.length() == 0) {
      writePrompt();
      return;
    }

    if (attachedIn != null) {
      if ("detach".equals(line)) {
        // multiple in future mabye
        detach();
        return;
      }

      // relaying command to another remote cli process
      try {
        attachedIn.write(String.format("%s\n", line).getBytes());
        attachedIn.flush();
      } catch (Exception e) {
        log.error("std:in ---(agent)---X---> process ({})", getName());
        log.info("detaching... ");
        detach();
      }
      // writePrompt();
      return;
    }

    if (line.startsWith(cd)) {
      String path = "/";
      if (line.length() > 2) {
        // FIXME - cheesy - "look" for relative directories
        // !
        if (!line.contains("/")) {
          path = "/" + line.substring(3);
        } else {
          path = line.substring(3);
        }
      }
      cd(path);
    } else if (line.startsWith("shutdown")) {
      line = line.replace(" ", "/"); // formats for api call
      String path = String.format("/%s/%s/runtime/%s", Api.PREFIX_API, ApiFactory.API_TYPE_SERVICE, line);
      try {
        Object ret = api.process(myOutstream, path);
      } catch (Exception e) {
        log.error("shutdown threw", e);
      }
      write("\n".getBytes());
    } else if (line.startsWith("clear") || line.startsWith("cls")) {
      clear();
    } else if (line.startsWith("attach")) {
      String[] parts = line.split(" ");
      if (parts.length == 1) {
        attach();
      } else {
        attach(parts[1]);
      }
    } else if (line.startsWith(lp)) {
      write(CodecUtils.toJson(pipes).getBytes());
    } else if (line.startsWith(help)) {
      // TODO dump json command object
      // which has a map of commands
    } else if (line.startsWith(pwd)) {
      write(String.format("%s\n", cwd).getBytes());
    } else if (line.startsWith(ls)) {
      String path = cwd; // <-- path =
      if (line.length() > 3) {
        path = line.substring(3);
      }

      path = path.trim();
      // absolute path always
      ls(path);
    } else {

      String path = null;
      if (line.startsWith("/")) {
        path = String.format("/%s/%s%s", Api.PREFIX_API, ApiFactory.API_TYPE_SERVICE, line);
      } else {
        path = String.format("/%s/%s%s%s", Api.PREFIX_API, ApiFactory.API_TYPE_SERVICE, cwd, line);
      }

      log.info(path);
      try {

        Object ret = api.process(myOutstream, path);
        write("\n".getBytes());

      } catch (Exception e) {
        log.error("cli.process threw", e);
      }

    }
    writePrompt();
  }

  /**
   * Pipe between another process - its output stream is our input stream its
   * input stream is our output
   * 
   * <pre>
   * Our Process ---------- Pipe -----------  Remote Process
   * InputStream  ----- input/output ----&gt;     OutputStream (stdin)
   * OutputStream &lt;---- output/input -----     InputStream  (stdout)
   * 
   * MAKE NOTE : - you cannot interrupt a thread doing a blocking read on a processes
   * output stream !  The ONLY way to stop the thread from reading is to close the
   * stream.  So, we are going to keep the threads going until the Cli Service
   * is requested to stop - and we will stop the threads by closing the stream,
   * in stopService
   * 
   * </pre>
   * 
   * @author GroG
   *
   */
  public class Pipe implements Runnable {
    public String id;

    /**
     * OutputStream of remote process
     */
    public transient InputStream out;

    /**
     * InputStream of remote process
     */
    public transient OutputStream in;

    /**
     * if "myStreams" are currently connected to remote process
     */
    boolean attached = false;

    transient Thread worker = null;

    /**
     * 
     * @param id
     *          - id of the pipe
     * @param out
     *          - output stream of the remote process
     * @param in
     *          - input stream of the remote process
     */
    // process output - process input !!
    public Pipe(String id, InputStream out, OutputStream in) {
      this.id = id;
      this.out = out;
      this.in = in;
    }

    @Override
    public void run() {
      try {

        String line = null;
        int c = -1;
        char ch = '\0';
        StringBuilder sb = new StringBuilder();

        while ((c = out.read()) != -1) {

          ch = (char) c;

          if (ch != '\n') {
            sb.append(ch);
            continue;
          } else {
            line = sb.toString();
            sb = new StringBuilder();
          }

          if (attached) {
            // write back to cli & the cli will send it to stdout and publish it
            // when this one is active it "double" posts and adds crap
            // write(String.format("%s\n", line).getBytes());

            myOutstream.write(String.format("%s\n", line).getBytes());
            myOutstream.flush();

          } else {
            // noop - write to /dev/null
          }
        }
      } catch (Exception e) {
        log.error("Pipe threw", e);
      } /*
         * finally { try { if (is != null) { is.close(); } } catch (Exception
         * ex) { }
         */
    }

    /**
     * attaches myOutputstream to the remote output stream relaying/piping the
     * data back
     * 
     * @param b
     *          - if true we will send to output stream
     */
    public void attach(boolean b) {
      this.attached = b;
    }

    public void close() {
      attached = false;
      try {
        in.close();
      } catch (Exception e) {
      }
      try {
        out.close();
      } catch (Exception e) {
      }
    }

    public void stop() {
      if (worker != null) {
        worker.interrupt();
      }
    }

    public void start() {
      if (worker == null) {
        worker = new Thread(this, String.format("pipe-%s", id));
        worker.start();
      }
    }

  }

  /*
   * Command Line Interpreter - used for processing encoded (default RESTful)
   * commands from std in and returning results in (default JSON) encoded return
   * messages.
   * 
   * Has the ability to pipe to another process - if attached to another process
   * handle, and the ability to switch between many processes
   * 
   */
  public Cli(String n) {
    super(n);
    api = ApiFactory.getInstance(this);
    myOutstream = new CliOutputStream();
  }

  /**
   * add an i/o pair to this cli for the possible purpose attaching this is a
   * remote process's input and output stream, hence from this side they are
   * inverted - ie out is an inputstream and in is an output stream
   *
   * @param name
   *          - name of pipe
   * @param out
   *          - out stream to the remote process
   * @param in
   *          - in stream from the remote process
   */
  public void add(String name, InputStream out, OutputStream in) {
    pipes.put(name, new Pipe(name, out, in));
  }

  public void attach() {
    attach((String) null);
  }

  /**
   * Pipe or Attach to another processes' Cli different level cli.attach(process
   * id)
   */
  public void attach(String id) {

    if (pipes.size() == 1) {
      // only 1 choice
      for (String key : pipes.keySet()) {
        id = key;
      }
    }

    if (!pipes.containsKey(id)) {
      error("pipe %s could not be found", id);
      return;
    }

    Pipe pipe = pipes.get(id);
    attachedProcessName = id;
    // our input stream will be connected to remote input stream
    // stdin will now be relayed and not interpreted
    attachedIn = pipe.in;
    pipe.attach(true); // attaches our outputstream with remote
    attachedPipe = pipe;
    attachedPipe.start(); // starts our listener

  }

  // FIXME - remove - do in constructor or "start"
  public void attachStdIO() {
    if (in == null) {
      in = new Decoder(this, System.in);
      in.start();
    } else {
      log.info("stdin already attached");
    }

    // if I'm not an agent then just writing to System.out is fine
    // because all of it will be relayed to an Agent if I'm spawned
    // from an Agent.. or
    // If I'm without an Agent I'll just do the logging I was directed
    // to on the command line
    /*
     * if (myOutstream == null) { myOutstream = System.out; } else {
     * log.info("stdout already attached"); }
     */
  }

  public String cd(String path) {
    cwd = path;
    return path;
  }

  /**
   * unfortunate collision of names :(
   * this detach() does not detach from services - but detaches from the 
   * std i/o of another process
   */
  public void detach() {
    try {
      write(String.format("detaching from %s\n", attachedProcessName).getBytes());
      attachedProcessName = null;
      attachedIn = null;
      if (attachedPipe != null) {
        // attachedOut.close();
        attachedPipe.attach(false);
        // attachedPipe.stop(); NO CANNOT STOP THREAD !!!
      }
      attachedPipe = null;
    } catch (Exception e) {
      log.error("cli detach threw", e);
    }
  }

  public void detachStdIO() {
    if (in != null) {
      in.interrupt();
    }
  }

  public String echo(String msg) {
    return msg;
  }

  /**
   * FIXME !!! return Object[] and let Cli command processor handle encoding for
   * return
   * 
   * path is always absolute never relative
   * 
   * @param path
   *          p
   * @throws IOException
   *           e
   * 
   */
  public void ls(String path) throws IOException {
    String[] parts = path.split("/");

    if (path.equals("/")) {
      // FIXME don't do this here !!!
      write(String.format("%s\n", CodecUtils.toJson(Runtime.getServiceNames()).toString()).getBytes());
    } else if (parts.length == 2 && !path.endsWith("/")) {
      // FIXME don't do this here !!!
      write(String.format("%s\n", CodecUtils.toJson(Runtime.getService(parts[1])).toString()).getBytes());
    } else if (parts.length == 2 && path.endsWith("/")) {
      ServiceInterface si = Runtime.getService(parts[1]);
      // FIXME don't do this here !!!
      write(String.format("%s\n", CodecUtils.toJson(si.getDeclaredMethodNames()).toString()).getBytes());
    }

    // if path == /serviceName - json return ? Cool !
    // if path /serviceName/ - method return
  }

  public void write(byte[] data) throws IOException {

    // if (Runtime.isAgent()) {
    if (myOutstream != null) {
      myOutstream.write(data);
      myOutstream.flush();
    }

    /*
     * if (fos != null) { fos.write(data); fos.flush(); }
     */
    // }

  }

  /**
   * this method publishes returning data
   * 
   * @param data
   *          - byte array to be published from stdout stream
   * @return - the byte array in string form
   */
  public String stdout(byte[] data) {
    if (data != null)
      return new String(data);
    else {
      return "";
    }
  }

  @Override
  public void startService() {
    super.startService();
    attachStdIO();
  }

  @Override
  public void stopService() {
    super.stopService();
    try {

      // shutdown my i/o
      if (in != null) {
        in.interrupt();
      }

      in = null;
      if (myOutstream != null) {
        myOutstream.close();
      }
      myOutstream = null;

      // shutdown all remote io
      for (Pipe pipe : pipes.values()) {
        // closing stream the only way to stop the thread
        pipe.close();
        pipe.stop();
      }

      /*
       * if (fos != null) { fos.close(); } fos = null;
       */
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {
    ServiceType meta = new ServiceType(Cli.class.getCanonicalName());
    meta.addDescription("command line interpreter interface for myrobotlab");
    meta.addCategory("framework");
    return meta;
  }
  
  public void releaseService(){
    super.releaseService();
    if (in != null){
      in.isRunning = false;
      // in.interrupt();
      in = null;
    }
  }

  public static void main(String[] args) {
    LoggingFactory.init("ERROR");

    try {

      Runtime.start("gui", "SwingGui");
      Cli cli = (Cli) Runtime.start("cli", "Cli");

      // cli.processInput("test", null);

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
