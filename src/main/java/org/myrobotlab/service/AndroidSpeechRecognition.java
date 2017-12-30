package org.myrobotlab.service;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechRecognizer;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.slf4j.Logger;

/**
 *
 * @author LunDev (github), Ma. Vo. (MyRobotlab)
 * @author Moz4r 
 *         Client temporary build :
 *         https://github.com/moz4r/SpeechRecognitionMRL/blob/master/app/release/app-release.apk?raw=true
 *         Client temporary sources :
 *         https://github.com/moz4r/SpeechRecognitionMRL
 */
public class AndroidSpeechRecognition extends AbstractSpeechRecognizer {

  public class Command {
    public String name;
    public String method;
    public Object[] params;

    Command(String name, String method, Object[] params) {
      this.name = name;
      this.method = method;
      this.params = params;
    }
  }

  HashMap<String, Command> commands = new HashMap<String, Command>();

  private static ServerSocket serverSock;
  // heartBeat is wip
  private transient Timer heartBeat;
  private transient boolean heartBeatFirstMessage = true;
  // is socked bind with success?

  public boolean runningserver;
  // is a client connected?
  boolean clientIsConnect = false;
  public int port = 5684;

  private static NewConnectionHandler nch;
  private static Socket clientSocket = null;
  public String lastThingRecognized;
  private String serverAdress;
  private boolean listening = false;
  private boolean autoListen = false;

  private class ClientHandler extends Thread {

    private boolean running;
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

    // kill client socket
    public void finish() {
      try {
        if (clientSocket != null) {
          in.close();
          out.close();
          clientSocket.close();
          clientSocket = null;
          clientIsConnect = false;
        }

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
      Object obj;
      boolean check = true;
      while (running && check) {
        try {
          obj = in.readObject();
          if (obj != null) {
            String mes = (String) obj;
            process(mes);
          }
        } catch (EOFException ex) {
          check = false;
          // clients will cause some unexpected events, sometime
          log.info("EOFException kill connection");
          if (runningserver) {
            startServer();
          }
        } catch (ClassNotFoundException e) {
          check = false;
          log.info("ClassNotFoundException kill connection");
          if (runningserver) {
            startServer();
          }
        } catch (IOException e) {
          check = false;
          log.info("IOException kill connection");
          if (runningserver) {
            startServer();
          }
        }
      }

    }
  }

  private class NewConnectionHandler extends Thread {

    public NewConnectionHandler(ServerSocket ss) {
      serverSock = ss;
    }

    @Override
    public void run() {

      while (!clientIsConnect && runningserver) {

        try {
          clientIsConnect = true;
          // Only accept one client
          log.info("waiting client...");
          clientSocket = serverSock.accept();
          // restart after disconnect
          clientIsConnect = false;
          ClientHandler ch = new ClientHandler(clientSocket);
          client = ch;
          ch.start();
        } catch (SocketException ex) {
          clientIsConnect = false;
          runningserver = false;
          log.info("socket closed");
        } catch (IOException e) {
          log.info("NewConnectionHandler error : %s", e);
          clientIsConnect = false;
        }

      }

    }
  }

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(AndroidSpeechRecognition.class);
  transient private ClientHandler client;

  private boolean speaking;

  // TODO refactor version control
  private final static String VERSION = "1.0b";

  public static void main(String[] args) throws InterruptedException {

    LoggingFactory.init(Level.INFO);
    try {

      Runtime.start("gui", "SwingGui");
      Runtime.start("avr", "AndroidSpeechRecognition");
      Runtime.start("python", "Python");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public AndroidSpeechRecognition(String n) {
    super(n);
    // intializing variables
    // Should do something useful here in future
  }

  // TODO process more messages
  private void process(String mes) {
    log.info("received message: " + mes);
    if (mes.startsWith("version")) {
      String[] split = mes.split("=");

      boolean versionOk = false;

      // check client version
      if (VERSION.equalsIgnoreCase(split[1])) {
        versionOk = true;
      }

      if (!versionOk) {
        sendToClient("serverversion=" + VERSION);
        warn("Client version %s is different from server version  %s", split[1], VERSION);
        client.finish();
        clientIsConnect = false;
      } else {
        sendToClient("accepted");
        log.info("Client accepted !");
      }
    } else if (mes.startsWith("recognized")) {
      String[] split = mes.split("=");
      log.info("recognized: " + split[1]);
      invoke("recognized", split[1]);
    } else if (mes.startsWith("isListening")) {
      String[] split = mes.split("=");
      log.info("isListening: " + split[1]);
      invoke("listeningEvent", Boolean.parseBoolean(split[1]));
    } else {
      log.error("ERROR: " + mes);
    }
  }

  @Override
  public String publishText(String text) {
    return recognized(text);
  }

  @Override
  public void addTextListener(TextListener service) {
    addListener("publishText", service.getName(), "onText");
  }

  @Override
  public String recognized(String text) {
    if (commands.containsKey(text)) {
      // If we have a command. send it when we recognize...
      Command cmd = commands.get(text);
      send(cmd.name, cmd.method, cmd.params);
    }
    lastThingRecognized = text;
    broadcastState();
    return text;
  }

  private void sendToClient(String mes) {
    if (clientSocket != null && clientSocket.isConnected()) {
      try {
        client.getOut().writeObject(mes);
      } catch (IOException ex) {
        log.error("send error");
        Logging.logError(ex);
      }
    }
  }

  public void setPort(int p) {
    port = p;
  }

  public String getClientAddress() {
    if (clientSocket != null && clientSocket.isConnected()) {
      return "- Client : " + clientSocket.getInetAddress().toString() + " connected !";
    }
    return "- Client : not connected";

  }

  public String getServerAddress() {
    if (serverAdress == null) {
      serverAdress = Runtime.getLocalAddresses().toString();
    }
    return serverAdress;

  }

  // Server-end

  @Override
  public void startListening() {
    sendToClient("startListening");
  }

  @Deprecated
  public void startRecognition() {
    startListening();
  }

  // Server-start
  public void startServer() {

    stopServer();

    if (heartBeat != null) {
      heartBeat.cancel();
      heartBeat = null;
    }
    heartBeatFirstMessage = true;

    // waiting real heartBeat
    heartBeat = new Timer();
    heartBeat.schedule(new TimerTask() {
      @Override
      public void run() {
        if (clientSocket != null && clientSocket.isConnected()) {
          try {
            broadcastState();
            if (heartBeatFirstMessage) {
              heartBeatFirstMessage = false;
              setAutoListen(autoListen);
            }
            client.getOut().writeObject("heartBeat");
          } catch (IOException e) {
            log.info("timer error");
            // TODO Auto-generated catch block
            startServer();
          }
        }
      }
    }, 0, 5000);
    try {
      ServerSocket serverSock = new ServerSocket(port);
      serverSock.setReuseAddress(true);
      nch = new NewConnectionHandler(serverSock);
      nch.start();
      runningserver = true;
    } catch (IOException ex) {
      runningserver = false;
      error("Error binding, address already in use ? : %s", ex);
    }
    broadcastState();
  }

  public void stopServer() {

    if (client != null) {
      client.finish();
      clientIsConnect = false;
    }

    runningserver = false;
    try {
      serverSock.close();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      // e.printStackTrace();
    }
    serverSock = null;
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    // log.info(serverSock.isBound()+"isBound");
    broadcastState();
  }

  @Override
  public void startService() {
    super.startService();
    startServer();
  }

  @Override
  public void stopService() {
    stopServer();
    super.stopService();
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
    meta.addDescription("service which opens a listening port on 5684 and re-publishes incoming Android recognized speech");
    meta.addCategory("speech recognition");
    return meta;
  }

  @Override
  public void listeningEvent(Boolean event) {
    listening = event;
    broadcastState();
    return;
  }

  @Override
  public void resumeListening() {
    sendToClient("resumeListening");
  }

  @Override
  public void stopListening() {
    sendToClient("stopListening");
  }

  @Override
  public void addMouth(SpeechSynthesis mouth) {
    mouth.addEar(this);
    subscribe(mouth.getName(), "publishStartSpeaking");
    subscribe(mouth.getName(), "publishEndSpeaking");
  }

  @Override
  public void onStartSpeaking(String utterance) {
    if (getAutoListen())
    {
    pauseListening();
    }
  }

  @Override
  public void onEndSpeaking(String utterance) {
    if (getAutoListen())
    {
      resumeListening();
    }
  }

  @Override
  public void lockOutAllGrammarExcept(String lockPhrase) {
    // TODO Auto-generated method stub

  }

  @Override
  public void clearLock() {
    // TODO Auto-generated method stub

  }

  @Override
  public void pauseListening() {
    sendToClient("pauseListening");
  }

  @Override
  public void setAutoListen(boolean autoListen) {
    this.autoListen = autoListen;
    if (autoListen) {
      sendToClient("setAutoListenTrue");
    } else {
      sendToClient("setAutoListenFalse");
    }
    broadcastState();
  }

  public void setContinuous(boolean b) {
    // TODO Auto-generated method stub
  }

  @Override
  public boolean isListening() {
    return this.listening;
  }

  public boolean getAutoListen() {
    return this.autoListen;
  }

  // TODO - should this be in Service ?????
  public void addCommand(String actionPhrase, String name, String method, Object... params) {
    actionPhrase = actionPhrase.toLowerCase().trim();
    if (commands == null) {
      commands = new HashMap<String, Command>();
    }
    commands.put(actionPhrase, new Command(name, method, params));
  }

}
