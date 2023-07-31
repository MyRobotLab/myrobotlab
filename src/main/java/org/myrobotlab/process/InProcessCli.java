package org.myrobotlab.process;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Registration;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.net.Connection;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

/**
 * 
 * @author GroG This class is a "gateway" similar to other mrl
 *         Gateways(interface) In that it exchanges messages with remote
 *         systems. In this case its stdin/stdout
 * 
 *         Purpose of this class it to provide a stream based cli interface.
 *         InputStream in - will be appropriate for the CodecUtils.cliToMsg
 *         interface, which means drive msgs to be constructed on the other side
 *         OutputStream out - the Output stream is where an potential
 *         subscriptions will resolve.
 * 
 *         cwd - current working directory state information
 *
 */
public class InProcessCli implements Runnable {
  // FIXME - make UUID - this is a connection where Runtime is the gateway !!!

  public final static Logger log = LoggerFactory.getLogger(InProcessCli.class);

  String id;
  String uuid;
  transient Thread worker = null;
  String name;
  transient InputStream in;
  transient OutputStream out;
  boolean running = false;
  String cwd = "/";
  transient ServiceInterface service = null;

  // FIXME - remove ?!?!
  List<String> remoteIdStack = new ArrayList<>();

  /**
   * the id this cli is currently remotely "attached" to
   */
  private String remoteId;

  private String contextPath = null;// TODO - make this stateful ?? -
                                    // "runtime/ls/";
  /**
   * buffer of python multi-line
   */
  private String pythonMultiLineBuffer = null;

  private Map<String, NatEntry> natTable = new HashMap<>();

  private String relayTo;

  private String relayMethod;

  public static class NatEntry {
    // FIXME msgId !
    public String method;
    public String srcFullName;

    public NatEntry(String srcFullName, String method) {
      this.srcFullName = srcFullName;
      this.method = method;
    }

    @Override
    public String toString() {
      return String.format("%s.%s", srcFullName, method);
    }
  }

  /**
   * The inProcessCli behave like a remote id - although it is in the same
   * process as the mrl instances. Its a general good model to follow, because
   * stdin/stdout is a pipe into and out of the instance, so just like
   * websockets, mqtt or xmpp it should behave the same
   * 
   * @param s
   *          service
   * @param senderName
   *          sender name
   * @param in
   *          input stream
   * @param out
   *          output stream
   * 
   */
  public InProcessCli(ServiceInterface s, String senderName, InputStream in, OutputStream out) {
    this.service = s;
    String parentId = s.getId();
    this.id = parentId + "-cli"; // this becomes a local/remote id with
                                 // prepended cli-
    this.remoteId = parentId; // remote id is the mrl instance
    this.name = senderName;
    this.in = in;
    this.out = out;
    // first session
    remoteIdStack.add(this.remoteId);
    start();
  }

  /**
   * Start InputStream consumer thread
   */
  public synchronized void start() {
    if (worker == null) {
      log.info("starting {} worker", name);
      worker = new Thread(this, String.format("%s-cli", name));
      worker.start();
    } else {
      log.info("stdin already running");
    }
  }

  /**
   * Digests incoming InputStream. For all newlines, the loop sends the command
   * to be processed.
   */
  @Override
  public void run() {

    try {
      running = true;

      int c = '\n';
      String readLine = "";

      writePrompt();
      while (running) {

        if (in.available() > 0) {
          c = in.read();
        } else {
          try {
            Thread.sleep(50);
          } catch (InterruptedException e) {
          }
          continue;
        }

        log.info("c = {}", c);
        // != 0x04 /* ctrl-d 0x04 ctrl-c 0x03 '\n' */

        readLine += (char) c;
        if (c == '\n') {
          // python interpreter
          if (relayTo != null) {

            // multi line
            if (pythonMultiLineBuffer != null) {
              // found end of multi-line - process
              pythonMultiLineBuffer += readLine;

              if (pythonMultiLineBuffer.endsWith("\n\n")) {
                Message msg = Message.createMessage(name + '@' + id, relayTo, relayMethod, pythonMultiLineBuffer);
                service.out(msg);
                pythonMultiLineBuffer = null;
              }
              writePrompt();
              readLine = "";
              continue;
            }

            // beginning of multi-line
            if (readLine.startsWith("def") || readLine.startsWith("for") || readLine.startsWith("if")) {
              pythonMultiLineBuffer = readLine;
              writePrompt();
              readLine = "";
              continue;
            }

            // exiting python
            if ("exit()\n".equals(readLine)) {
              relayTo = null;
              pythonMultiLineBuffer = null;
              writePrompt();
              readLine = "";
              // re-initialize logging to previous state
              Runtime.initLog();
              continue;
            }
            // default case single line process
            Message msg = Message.createMessage(name + '@' + id, relayTo, relayMethod, readLine);
            service.out(msg);
            writePrompt();
            readLine = "";
            continue;
          }

          // service interpreter
          if (readLine.length() > 1 && relayTo == null) {
            try {
              process(null, readLine);
            } catch (Exception e) {
              log.error("cli process threw", e);
            }
          }
          readLine = "";
          writePrompt();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    running = false;
    worker = null;
  }

  // FIXME determine if GET notation should use encoded ? (probably) - ie should
  // it "always" be decoded ?
  public void process(String srcFullName, String cmd) {
    try {

      cmd = cmd.trim();

      Message cliMsg = null;

      if (!cmd.startsWith("/")) {
        // FIXME - THIS IS LOCAL ONLY !!!!
        // cd is a "local" command
        if (cmd.startsWith("cd")) {
          String[] parts = cmd.split(" ");
          if (parts.length == 2) {
            // cd xxx
            if (parts[1].startsWith("/")) {
              cwd = parts[1];
            } else {
              cwd += parts[1];
            }
          }
          // cmd = String.format("/runtime/cd %s", cwd); LOCAL !
          writeToJson(cwd);
          return;
        } else if (cmd.startsWith("pwd")) {
          // FIXME - THIS IS LOCAL ONLY !!!!
          writeToJson(cwd);
          return;
        } else if (cmd.startsWith("ls")) {
          // "ls" is a special query
          // FIXME - ls with params !
          String[] parts = cmd.split(" ");
          String lsPath = cwd;
          if (parts.length == 2) {
            // cd xxx
            if (parts[1].startsWith("/")) {
              lsPath = parts[1];
            } else {
              lsPath = cwd + parts[1];
            }
          }
          cliMsg = cliToMsg(cmd);
          cliMsg.method = "ls";
          cliMsg.data = new Object[] { lsPath };// {"\"" + lsPath + "\""};
          // cmd = String.format("/runtime/ls %s", lsPath);
        } else if (cwd.equals("/")) {
          cmd = "/runtime/" + cmd;
        } else {
          cmd = cwd + cmd;
        }
      } // else - must start with "/" - its a complete command

      if ("".equals(cmd)) {
        return; // FIXME RETURN ERROR ???
      }

      // "create" cli specific msgs
      if (cliMsg == null) {
        cliMsg = cliToMsg(cmd);
      }

      // fully address destination
      if (!cliMsg.name.contains("@")) {
        cliMsg.name += "@" + remoteId;
      }

      if ("exit".equals(cliMsg.method)) {
        String ret = "exiting " + remoteId + "...";
        String id = null;
        if (remoteIdStack.size() > 0) {
          int x = remoteIdStack.size() - 1;
          id = remoteIdStack.get(x);
          remoteIdStack.remove(x);
        } else {
          id = remoteId; // parent
        }
        setRemote(id);
        System.out.println(ret);
        return;
      }

      // subscribe - setup subscription
      // MRLListener listener = new MRLListener(cliMsg.method, name + '@' + id,
      // CodecUtils.getCallbackTopicName(cliMsg.method));
      // Message subscription = Message.createMessage(name + '@' + id,
      // cliMsg.getFullName(), "addListener", listener);

      String cliFullName = name + '@' + id;

      /*
       * if (srcFullName == null) { srcFullName = name + '@' + id; }
       */

      // setup cli subscription
      MRLListener listener = new MRLListener(cliMsg.method, cliFullName, CodecUtils.getCallbackTopicName(cliMsg.method));
      Message subscription = Message.createMessage(cliFullName, cliMsg.getFullName(), "addListener", listener);

      // send out subscription
      service.out(subscription);

      // setup NAT - translation
      natTable.put(String.format("%s.%s", cliFullName, CodecUtils.getCallbackTopicName(cliMsg.method)), new NatEntry(srcFullName, cliMsg.method));

      // send command msg
      service.out(cliMsg);

      // when you get the callback - print the data & remove the subscription
      // ...

    } catch (Exception e) {
      log.error("cli threw", e);
      return;
    }
  }

  /**
   * This is the Cli encoder - it takes a line of text and generates the
   * appropriate msg from it to either invoke (locally) or sendBlockingRemote
   * (remotely)
   * 
   * @param data
   *          data
   * @return message
   */
  public Message cliToMsg(String data) {

    if (contextPath != null) {
      data = contextPath + data;
    }

    // FIXME - is this "blocking?"
    return CodecUtils.pathToMsg("runtime@" + id, data);
  }

  public void writeToJson(Object o) {
    try {
      if (o != null) {
        out.write(CodecUtils.toPrettyJson(o).getBytes());
      } else {
        out.write("null".getBytes());
      }
    } catch (Exception e) {
      log.error("writeToJson threw", e);
    }
  }

  /**
   * get context specific path
   * 
   * @param uuid
   *          uuid
   * @return string representing cli prompt
   * 
   */
  public String getPrompt(String uuid) {
    return String.format("[%s@%s %s]%s", name, remoteId, cwd, "#");
  }

  /**
   * stop the thread - close the stream
   */
  public synchronized void stop() {
    running = false;

    if (worker != null) {
      // interrupt will not work
      // on an infinite blocked read
      worker.interrupt();
    }

    if (in != null && !System.in.equals(in)) {
      try {
        in.close();
      } catch (Exception e) {
        log.info("sdin error");
      }
    }

    if (out != null && !System.out.equals(out)) {
      try {
        out.close();
      } catch (Exception e) {
      }
    }
  }

  public void setRemote(String remoteId) {
    this.remoteId = remoteId;
  }

  public String getId() {
    return id;
  }

  public void writePrompt() {
    try {
      // pause a little - wait for async callbacks (stdout stderr) to process
      Service.sleep(50);
      if ("python".equals(relayTo)) {
        if (pythonMultiLineBuffer != null) {
          out.write("... ".getBytes());
        } else
          out.write(">>> ".getBytes());
      } else {
        out.write("\n".getBytes());
        out.write(getPrompt(id).getBytes());
        out.write(" ".getBytes());
      }
    } catch (Exception e) {
      log.error("writePrompt threw", e);
    }
  }

  /**
   * @param msg
   *          Incoming Message - likely from local/remote runtime
   */
  public void onMsg(Message msg) {

    log.info("{}@{} <== {}.{}", name, id, msg.getFullName(), msg.getMethod());

    // if NAT match - remove subscription
    String key = String.format("%s.%s", msg.getFullName(), msg.method);
    if (natTable.containsKey(key)) {
      NatEntry entry = natTable.get(key);

      Message subscription = Message.createMessage(name + "@" + id, entry.srcFullName, "removeListener", new Object[] { entry.method, name + "@" + id });
      // send out subscription
      service.out(subscription);

      // send back to original requester with changed address
      msg.setName(entry.srcFullName);

      // replacement of callback too - will always be onCli
      // so multiplexed methods will have single callback
      msg.method = "onCli";

      // was nat'd - needs to be sent back
      if (entry.srcFullName != null) {
        service.out(msg);
      } // else came from cli

      natTable.remove(key);
    }
    // do NAT replacement and send msg to originator

    if (msg.data == null) {
      writeToJson(null);
    } else {
      for (Object o : msg.data) {
        if ("onRegistered".equals(msg.getMethod())) {
          if (msg.data != null & msg.data.length == 1) {
            // FIXME check dataEncoding ???
            Registration registration = (Registration) msg.data[0];// CodecUtils.fromJson((String)
                                                                   // msg.data[0],
                                                                   // Registration.class);
            try {
              out.write("\n\n".getBytes());
              out.write(String.format("%s <- registered %s %s\n\n", name + '@' + id, registration.getFullName(), registration.getTypeKey()).getBytes());
            } catch (Exception e) {
              log.error("writing registration threw", e);
            }
          }
        } else {
          if ("json".equals(msg.encoding) || "onStdOut".equals(msg.getMethod()) || "onStdError".equals(msg.getMethod())) {
            // python interpreter
            try {
              out.write(o.toString().getBytes());
            } catch (Exception e) {
              log.error("write threw", e);
            }
          } else {
            writeToJson(o);
          }
        }
      }
    }
  }

  public void relay(String name, String method, String pubMethod) {
    relayTo = name;
    relayMethod = method;
    String cliFullName = name + '@' + id;

    // setup cli subscription
    MRLListener listener = new MRLListener(pubMethod, cliFullName, CodecUtils.getCallbackTopicName(pubMethod));
    Message subscription = Message.createMessage(cliFullName, relayTo, "addListener", listener);

    // send out subscription
    service.out(subscription);

  }

  public static void main(String[] args) {
    try {

      // Logger.getRootLogger().setLevel(Level.INFO);

      InProcessCli client = new InProcessCli(Runtime.getInstance(), "test", System.in, System.out);
      client.start();

      // if interactive vs non-interactive which will pretty much be curl ;P BUT
      // BLOCKING !!! (ie useful)

      // client.startInteractiveMode();

      // System.out.println("password {}", password);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public Connection getConnection() {
    String cliId = getId();
    String uuid = java.util.UUID.randomUUID().toString();
    Connection connection = new Connection(uuid, cliId, "runtime");
    connection.put("header-User-Agent", "stdin-client");
    connection.put("cwd", "/");
    connection.put("uri", "/api/cli");
    connection.put("user", "root");
    connection.put("host", "local");
    connection.put("c-type", "Cli");
    connection.put("cli", this);
    return connection;

  }

}
