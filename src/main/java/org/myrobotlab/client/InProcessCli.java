package org.myrobotlab.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.myrobotlab.codec.ApiCli;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.Gateway;
import org.python.jline.internal.Log;
import org.slf4j.Logger;

/**
 * 
 * @author GroG This class is a "gateway" similar to other mrl
 *         Gateways(interface) In that it exchanges messages with remote
 *         systems. In this case its stdin/stdout
 *
 */
public class InProcessCli implements Runnable {
  // FIXME - make UUID - this is a connection where Runtime is the gateway !!!

  public final static Logger log = LoggerFactory.getLogger(InProcessCli.class);

  String uuid;
  Thread myThread = null;
  String name;
  InputStream in;
  OutputStream out;
  boolean running = false;
  String prefix = "/";

  public InProcessCli(String senderName, InputStream in, OutputStream out) {
    this.name = senderName;
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

      String id = "cli";
      String uuid = String.format("stdin-%s-%d", Runtime.getId(), random.nextInt(10000));
      ApiCli cli = new ApiCli();// (ApiCli)ApiFactory.getApiProcessor("cli");
      // cli.addClient(null, "cli", null, uuid);
      Map<String, Object> attributes = new HashMap<>();
      attributes.put("gateway", "runtime");
      attributes.put("uuid", uuid);
      attributes.put("id", id);
      attributes.put("User-Agent", "stdin-client");
      attributes.put("cwd", "/");
      attributes.put("uri", "/api/cli");
      attributes.put("user", "root");
      attributes.put("host", "local");
      Runtime.getInstance().addConnection(id, uuid, attributes);

      int c = '\n';
      String readLine = "";
      // FIXME - check .available() every 300ms so we don't block forever !
      while (running
          && (c = in.read()) != 0x04 /* ctrl-d 0x04 ctrl-c 0x03 '\n' */) {

        readLine += (char) c;
        if (c == '\n') {
          try {
            process(readLine);

            // if remote send --msg--> sendBlocking ????

            // stdin -> msg ! -> invoke or sendBlockingRemote
            // cli.process(null, "cli", "/api/cli", uuid, out, readLine);
          } catch (Exception e) {
            log.error("cli process threw", e);
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

  public void process(String data) {
    try {

      /* what the hell is the prefix ?
      if (prefix != null) {
        data = prefix + data;
      }
      */
      data = data.trim();

      if ("".equals(data)) {
        writePrompt(out, uuid); // <-- should be id no ?
        return;
      }

      // parse line for /{serviceName}/{method}/jsonEncoded? parms ?? ...
      // parse line for /{serviceName@id}/{method}/jsonEncoded? parms ?? ...

      // "create" cli specific msgs
      Message cliMsg = msgFromCli(data);

      Object ret = null;

      if (cliMsg.isLocal()) {
        // invoke locally
        ServiceInterface si = Runtime.getService(cliMsg.name);
        ret = si.invoke(cliMsg);
      } else {
        // send remotely
        // get gateway
        // send blocking remote
        // return result
        Gateway gateway = Runtime.getGatway(cliMsg.getRemoteId());
        ret = gateway.sendBlockingRemote(cliMsg, 3000); // I ASSUME THIS RETURNS DATA AND NOT THE RETURNING MSG ?
      }

      if (ret == null || ret.getClass().equals(String.class)) {
        write((String)ret);
      } else if (ret != null) {
        writeToJson(ret);
      }
      
      writePrompt(out, data);

    } catch (Exception e) {
      log.error("cli threw", e);
    }
  }

  /**
   * This is the Cli encoder - it takes a line of text and generates the
   * appropriate msg from it to either invoke (locally) or sendBlockingRemote
   * (remotely)
   * 
   * @param data
   * @return
   */
  public Message msgFromCli(String data) {

    // default msg
    Message msg = Message.createMessage("runtime", "runtime", "pwd", null);
    String[] parts = data.split(" ");

    if (parts.length > 1) {
      // at least 1 space in command meaning at least 1 parameter
      if (data.startsWith("cd")) {
        msg.method = "cd";
        msg.data = new Object[] { parts[1] };

      } else if (data.startsWith("ls")) {
        msg.method = "ls";
        msg.data = new Object[] { parts[1] };

      } else if (data.startsWith("attach")) {
        msg.method = "attach";
        msg.data = new Object[] { parts[1] };
      }
    } else {
      // 0 spaces in command
      if ("pwd".equals(data)) {
        msg.method = "pwd";
      } else if ("lc".equals(data)) {
        msg.method = "lc";
      } else if ("whoami".equals(data)) {
        msg.method = "whoami";
      } else if ("route".equals(data)) {
        msg.method = "getRouteTable";
      } else if ("ls".equals(data)) {
        msg.method = "ls";
      } else {
        // we try to do a service call ?
        String cmd = null;
        if (!data.startsWith("/")){
          cmd = Runtime.getInstance().pwd() + parts[0];
        } else {
          cmd = parts[0];
        }
        String[] cmdParts = cmd.split("/");
        if (cmdParts.length > 2) {
          msg.name = cmdParts[1];
          msg.method = cmdParts[2];
          // add the parameters
          if (cmdParts.length > 3) {
            String[] params = new String[cmdParts.length - 3];
            for (int i = 3; i < cmdParts.length; ++i) {
              params[i - 3] = cmdParts[i];  
            }
            msg.data = params;
          }
        }
      }
    }

    return msg;
  }
  
  
  public void write(String o) throws IOException {
    if (o == null) {
      out.write("null".getBytes());
    } else {
      out.write(o.getBytes());
    }
  }

  public void writeToJson(Object o) throws IOException {
    out.write(CodecUtils.toPrettyJson(o).getBytes());
  }

  public void writePrompt(OutputStream out, String id) throws IOException {
    out.write("\n".getBytes());
    out.write(getPrompt(id).getBytes());
    out.write(" ".getBytes());
  }

  public String getPrompt(String uuid) {
    // Map<String, Object> gateway = Runtime.getConnection(uuid);
    // String prompt = "root".equals(gateway.get("user")) ? "#" : "$";
    // return String.format("[%s@%s %s]%s", gateway.get("user"), gateway.get("host"), gateway.get("cwd"), prompt);
    Runtime runtime = Runtime.getInstance();
    return String.format("[%s@%s %s]%s", name, Runtime.getId(), runtime.pwd(), "#");
  }

  // FIXME - interrupt does not work on a infinite blocked read
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

      InProcessCli client = new InProcessCli("test", System.in, System.out);
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
