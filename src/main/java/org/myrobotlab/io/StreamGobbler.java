package org.myrobotlab.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class StreamGobbler extends Thread {
  protected transient InputStream processOut;
  protected transient OutputStream processIn;
  protected String name;

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
      while ((line = br.readLine()) != null)
        if (line != null) {
          processIn.write(String.format("%s\n", line).getBytes());
          processIn.flush();
        }
    } catch (IOException ioe) {
      System.out.println("gobbler leaving");
      ioe.printStackTrace();
    }
  }
}