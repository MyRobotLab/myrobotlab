package org.myrobotlab.client;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.myrobotlab.codec.ApiCli;
import org.myrobotlab.service.Runtime;
import org.python.jline.internal.Log;

public class InProcessCli implements Runnable {
  Thread myThread = null;

  InputStream in;
  OutputStream out;
  boolean running = false;

  public InProcessCli(InputStream in, OutputStream out) {
    this.in = in;
    this.out = out;
  }

  public void start() {
    if (myThread == null) {
      myThread = new Thread(this, "client-stdin-worker");
      myThread.start();
    } else {
      Log.info("stdin already running");
    }
  }

  @Override
  public void run() {

    try {
      running = true;
      Random random = new Random();
      
      String uuid = String.format("stdin-%s-%d", Runtime.getId(), random.nextInt(10000));
      ApiCli cli = new ApiCli();// (ApiCli)ApiFactory.getApiProcessor("cli");
      // cli.addClient(null, "cli", null, uuid);
      Map<String, Object> attributes = new HashMap<>();
      attributes.put("gateway", "runtime");
      attributes.put("uuid", uuid);
      attributes.put("id", "cli");
      attributes.put("User-Agent", "stdin-client");
      attributes.put("cwd", "/");
      attributes.put("uri", "/api/cli");
      attributes.put("user", "root");
      attributes.put("host", "local");
      Runtime.getInstance().addConnection(uuid, attributes);

      int c = '\n';
      String readLine = "";
      // FIXME - check .available() every 300ms so we don't block forever !
      while (running && (c = in.read()) != 0x04 /* ctrl-d 0x04 ctrl-c 0x03 '\n' */) {

        // out.write((char) c);
        readLine += (char) c;
        if (c == '\n') {
          try {
            // cli.process(webgui, apiKey, r);
            cli.process(null, "cli", "/api/cli", uuid, out, readLine);
          } catch (Exception e) {
            e.printStackTrace();
          }
          readLine = "";
        }

      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    running = false;
    myThread = null;
  }

  public void stop() {
    if (myThread != null) {
      myThread.interrupt();
    }
  }

  public static void main(String[] args) {
    try {
      // Logger logger = (Logger)
      // LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
      // logger.setLevel(Level.INFO);

      // Logger.getRootLogger().setLevel(Level.INFO);

      Runtime.getInstance();

      InProcessCli client = new InProcessCli(System.in, System.out);
      client.start();

      // if interactive vs non-interactive which will pretty much be curl ;P BUT
      // BLOCKING !!! (ie useful)

      // client.startInteractiveMode();

      // System.out.println("password {}", password);

    } catch (Exception e) {
      // log.error("main threw", e);
      e.printStackTrace();
    }
  }

}
