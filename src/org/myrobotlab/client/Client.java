package org.myrobotlab.client;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.MessageService;
import org.myrobotlab.framework.Outbox;
import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

public class Client extends MessageService implements NameProvider {

  public final static Logger log = LoggerFactory.getLogger(Client.class);

  URI uri = null;

  // static private Recorder recorder = null; TO BE USED IN FUTURE
  String name;

  boolean isRunning = true;

  ObjectOutputStream outObject;
  Socket socket = null;
  OutputStream outStream = null;

  public class ReceiveThread extends Thread {
    public void run() {
      while (socket != null && isRunning) {
        // Message msg = null;
        // Object o = null;

        /*
         * o = in.readObject(); msg = (Message) o;
         */
      }

    }
  }

  public Client(String uri, String name) throws URISyntaxException {
    this(new URI(uri), name);
  }

  public Client(URI uri, String name) {
    super(name);
    this.name = name;
    this.uri = uri;

    // this.inbox = new Inbox(name);
    this.outbox = new Outbox(this);
    // cm = new CommunicationManager(name);
    this.outbox.setCommunicationManager(cm);
  }

  // FIXME - make MessageService parent of Service - inherit it here with all
  // messaging possible
  // Messaging is already an interface ! Good start there
  public void send(String name, String method, Object... data) throws UnknownHostException, IOException {
    try {
      Message msg = createMessage(name, method, data);
      msg.sender = this.getName();
      // All methods which are invoked will
      // get the correct sendingMethod
      // here its hardcoded
      msg.sendingMethod = "send";

      if (socket == null) {
        socket = new Socket(uri.getHost(), uri.getPort());
        outStream = socket.getOutputStream();
        outObject = new ObjectOutputStream(outStream);
      }

      outObject.writeObject(msg);
      outObject.flush();
      // MAKE NOTE !!! :
      // a reset is necessary after every object !
      outObject.reset();
    } catch (Exception e) {
      Logging.logError(e);
      try {
        socket.close();
      } catch (Exception e2) {
      }
    }

  }

  // FIXME - should be in messenger service
  /*
   * public Message createMessage(String name, String method, Object data) { if
   * (data == null) { return createMessage(name, method, null); } Object[] d =
   * new Object[1]; d[0] = data; return createMessage(name, method, d); }
   */

  // TODO - remove or reconcile - RemoteAdapter and Service are the only ones
  // using this
  public Message createMessage(String name, String method, Object data) {
    if (data == null) {
      return createMessage(name, method, null);
    }
    Object[] d = new Object[1];
    d[0] = data;
    return createMessage(name, method, d);
  }

  // FIXME All parameter constructor
  // TODO - Probably simplify to take array of object
  public Message createMessage(String name, String method, Object[] data) {
    Message msg = new Message();
    msg.name = name; // destination instance name
    msg.sender = this.getName();
    msg.data = data;
    msg.method = method;

    return msg;
  }

  @Override
  public String getName() {
    return name;
  }

  public static void main(String[] args) {
    try {
      Client client = new Client("tcp://localhost:6767", "client");
      // call a runtime method
      client.send("runtime", "getUptime");
      client.send("runtime", "start", "arduino", "Arduino");
      client.send("runtime", "start", "servo01", "Servo");
      // call a method you made in python
      client.send("python", "foo");
      client.send("arduino", "connect", "COM4");
      client.send("servo01", "moveTo", 5);
      log.info("here");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }
}
