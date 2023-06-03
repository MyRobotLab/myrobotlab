package org.myrobotlab.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;
/**
 * A general stream gobbler, useful when starting processes and handling their streams
 * @author GroG
 *
 */
public class StreamGobbler extends Thread {
  protected transient InputStream processOut;
  protected transient OutputStream processIn;
  protected String name;
  public final static Logger log = LoggerFactory.getLogger(StreamGobbler.class);

  /**
   * When we do not need to redirect the stream to another stream. E.g.
   * when we create a process, and are already redirecting std:out and std:err
   * 
   * @param name
   * @param processOut
   */
  public StreamGobbler(String name, InputStream processOut) {
    this(name, processOut, null);
  }

  /**
   * This is a general stream gobbler that will consume and input stream and move 
   * the data to an output stream.
   * 
   * @param name
   * @param processOut
   * @param processIn
   */
  public StreamGobbler(String name, InputStream processOut, OutputStream processIn) {
    super(name);
    this.processOut = processOut;
    this.processIn = processIn;
    this.name = name;
  }

  @Override
  public void run() {
    try {
      InputStreamReader isr = new InputStreamReader(processOut);
      BufferedReader br = new BufferedReader(isr);
      String line = null;
      while ((line = br.readLine()) != null) {
        log.info("gobbler - {}", line);
        if (line != null && processIn != null) {
          processIn.write(String.format("%s\n", line).getBytes());
          processIn.flush();
        }}
    } catch (IOException ioe) {
      log.info("{} gobbler leaving", name);
    }
  }
}