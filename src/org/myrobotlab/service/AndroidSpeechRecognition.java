package org.myrobotlab.service;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.slf4j.Logger;

/**
 * based on _TemplateService
 */
/**
 *
 * @author LunDev (github), Ma. Vo. (MyRobotlab)
 */
public class AndroidSpeechRecognition extends Service implements TextPublisher {

  private class ClientHandler extends Thread {

    private boolean running;
    private Socket clientSocket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ClientHandler(Socket socket) {
      try {
        clientSocket = socket;
        in = new ObjectInputStream(clientSocket.getInputStream());
        out = new ObjectOutputStream(clientSocket.getOutputStream());
      } catch (Exception ex) {
        Logging.logError(ex);
      }
      running = true;
    }

    public void finish() {
      try {
        in.close();
        out.close();
        clientSocket.close();
      } catch (IOException e) {
        Logging.logError(e);
      }
      running = false;
    }

    public ObjectOutputStream getOut() {
      return out;
    }

    @Override
    public void run() {
      try {
        Object obj;
        while (running && (obj = in.readObject()) != null) {
          String mes = (String) obj;
          process(mes);
        }
      } catch (Exception ex) {
        Logging.logError(ex);
      }

    }
  }

  private class NewConnectionHandler extends Thread {

    private final ServerSocket serverSock;

    public NewConnectionHandler(ServerSocket ss) {
      serverSock = ss;
    }

    @Override
    public void run() {
      while (runningserver) {
        try {
          Socket clientSocket = serverSock.accept();

          ClientHandler ch = new ClientHandler(clientSocket);
          client = ch;
          ch.start();

          log.debug("Client connected");
          // Only accept one client
          runningserver = false;
        } catch (IOException ex) {
          Logging.logError(ex);
        }
      }
    }
  }

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(AndroidSpeechRecognition.class);
  transient private ClientHandler client;
  private int port = 5684;

  private final static String VERSION = "2015.01.01";

  private boolean runningserver;

  public static void main(String[] args) throws InterruptedException {

    LoggingFactory.init(Level.INFO);
    try {

      Runtime.start("gui", "SwingGui");
      Runtime.start("avr", "AndroidSpeechRecognition");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public AndroidSpeechRecognition(String n) {
    super(n);
    // intializing variables
    // Should do something useful here in future
  }

  private void process(String mes) {
    log.debug("received message: " + mes);
    if (mes.startsWith("version")) {
      String[] split = mes.split("=");

      boolean versionneuer = false;

      String aktversion2 = VERSION.replace(".", "~");
      String[] aktversionsplit = aktversion2.split("~");
      int[] aktversionsplitint = new int[aktversionsplit.length];
      for (int i = 0; i < aktversionsplit.length; i++) {
        aktversionsplitint[i] = Integer.parseInt(aktversionsplit[i]);
      }

      String runversion2 = split[1].replace(".", "~");
      String[] runversionsplit = runversion2.split("~");
      int[] runversionsplitint = new int[runversionsplit.length];
      for (int i = 0; i < runversionsplit.length; i++) {
        runversionsplitint[i] = Integer.parseInt(runversionsplit[i]);
      }

      for (int i = 0; i < 3; i++) {
        if (aktversionsplitint[i] < runversionsplitint[i]) {
          // eigener Versions-Teil ist NEUER wie der aktuelleste
          // Versions-Teil
          break;
        } else if (aktversionsplitint[i] > runversionsplitint[i]) {
          // eigener Versions-Teil ist AELTER wie der aktuelleste
          // Versions-Teil
          versionneuer = true;
          break;
        } else if (aktversionsplitint[i] > runversionsplitint[i]) {
          // eigener Versions-Teil ist GLEICH wie der aktuelleste
          // Versions-Teil
        }
      }

      if (versionneuer) {
        send("serverversion=" + VERSION);
        log.debug("Client has an old version");
        client.finish();
      } else {
        send("accepted");
        log.debug("Client accepted");
      }
    } else if (mes.startsWith("recognized")) {
      String[] split = mes.split("=");
      log.debug("recognized: " + split[1]);
      invoke("recognized", split[1]);
    } else {
      log.error("ERROR: " + mes);
    }
  }

  @Override
  public String publishText(String text) {
    return text;
  }

  @Override
  public void addTextListener(TextListener service) {
    addListener("publishText", service.getName(), "onText");
  }

  public String recognized(String text) {
    return text;
  }

  private void send(String mes) {
    try {
      client.getOut().writeObject(mes);
    } catch (IOException ex) {
      Logging.logError(ex);
    }

  }

  public void sendToClient(String mes) {
    send("fromServer=" + mes);
  }

  public void setPort(int p) {
    port = p;
  }

  // Server-end

  public void startRecognition() {
    send("startrecognition");
  }

  // Server-start
  private void startServer() {
    runningserver = true;
    try {
      ServerSocket serverSock = new ServerSocket(port);
      NewConnectionHandler nch = new NewConnectionHandler(serverSock);
      nch.start();
    } catch (IOException ex) {
      Logging.logError(ex);
    }
  }

  @Override
  public void startService() {
    super.startService();
    startServer();
  }

  @Override
  public void stopService() {
    super.stopService();
    if (runningserver) {
      runningserver = false;
    }
    if (client != null) {
      client.finish();
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

    ServiceType meta = new ServiceType(AndroidSpeechRecognition.class.getCanonicalName());
    meta.addDescription("utilizing Android's Speech Recognition");
    meta.addCategory("speech recognition");
    return meta;
  }

}
