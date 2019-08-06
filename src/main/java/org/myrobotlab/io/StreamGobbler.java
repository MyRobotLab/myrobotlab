package org.myrobotlab.io;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class StreamGobbler extends Thread {
  InputStream in;
  String name;
  boolean relay = true;

  public StreamGobbler(String name, InputStream is) {
    super(name);
    this.in = is;
    this.name = name;
  }

  public void run() {
    try {
      InputStreamReader isr = new InputStreamReader(in);
      BufferedReader br = new BufferedReader(isr);
      String line = null;
      while ((line = br.readLine()) != null)
        if (relay) {
          System.out.println(line);
        } else {
          // dev null
        }
    } catch (IOException ioe) {
      System.out.println("gobbler leaving");
      ioe.printStackTrace();
    }
  }
}