package org.myrobotlab.framework;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class StreamGobbler extends Thread {
  public final static Logger log = LoggerFactory.getLogger(StreamGobbler.class);

  InputStream is;
  ArrayList<OutputStream> os;

  String type;

  public StreamGobbler(InputStream is, ArrayList<OutputStream> os, String type) {
    super(String.format("streamgobbler_%s_%s", type, Runtime.getPid()));
    this.is = is;
    this.os = os;
  }

  @Override
  public void run() {
    try {

      String line = null;
      int c = -1;
      char ch = '\0';
      StringBuilder sb = new StringBuilder();

      while ((c = is.read()) != -1) {

        // FIXME up/down arrow history

        ch = (char) c;

        if (ch != '\n') {
          sb.append(ch);
          continue;
        } else {
          line = sb.toString();
          sb = new StringBuilder();
        }

        // FIXME OutputStream Versus Log !!! based on - IS_AGENT ||
        // FROM_AGENT ||
        // log.info(String.format("%s%s", tag, line));
        // log.info(String.format("<<%s", line));
        for (int i = 0; i < os.size(); ++i) {
          OutputStream out = os.get(i);
          out.write(String.format("%s\n", line).getBytes());
          out.flush(); // remember always to flush !!! :)
        }
      }
    } catch (Exception e) {
      log.error("StreamGobbler threw", e);
    }/* finally {
      try {
        if (is != null) {
          is.close();
        }
      } catch (Exception ex) {
      }
      */
    }

  }
