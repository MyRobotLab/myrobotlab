package org.myrobotlab.service;

import java.util.HashMap;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Invoker;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.Script;
import org.slf4j.Logger;
import py4j.GatewayServerListener;
import py4j.Py4JServerConnection;
import py4j.GatewayServer;

public class Py4j extends Service implements GatewayServerListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Py4j.class);

  private transient GatewayServer gateway = null;

  private transient Invoker handler = null;
  
  protected HashMap<String, Script> openedScripts = new HashMap<String, Script>();

  protected String activeScript = null;


  public Py4j(String n, String id) {
    super(n, id);
  }

  /**
   * start the gateway service listening on port
   */
  public void start() {
    if (gateway == null) {
      gateway = new GatewayServer(this);
      gateway.addListener(this);
      gateway.start();
      info("server started listening on %s:%d", gateway.getAddress(), gateway.getListeningPort());
      handler = (Invoker) gateway.getPythonServerEntryPoint(new Class[] { Invoker.class });
    } else {
      log.info("Py4j gateway server already started");
    }
  }

  public void newScript() {
    newScript("script.py");
  }  

  public void newScript(String scriptName) {
    if (!openedScripts.containsKey(scriptName)) {
      openScript(scriptName, "");
    }
  }

  public void openScript(String scriptName, String code) {
    activeScript = scriptName;
    openedScripts.put(scriptName, new Script(scriptName, code));
    broadcastState();
  }

  public void closeScript(String scriptName) {
    openedScripts.remove(scriptName);
    broadcastState();
  }
  

  @Override
  public boolean preProcessHook(Message msg) {
    // let the messages for this service
    // get processed normally
    if (methodSet.contains(msg.method)) {
      return true;
    }

    // will probably need to queu this
    if (handler != null) {
      // TODO - determine clients are connected .. how many clients etc..
      try {
        handler.invoke(msg.method, msg.data);
      } catch(Exception e) {
        error(e);
      }
    }
    return false;
  }

  /**
   * stop the gateway service
   */
  public void stop() {
    if (gateway != null) {
      gateway.shutdown();
      gateway = null;
    } else {
      log.info("Py4j gateway server already started");
    }
  }

  // https://stackoverflow.com/questions/23157424/py4j-how-would-i-go-about-on-calling-a-python-method-in-java
  public interface PythonInterface {
    public Message onMsg(Message msg);
    // below overloaded
    // public int doOperation(int i, int j, int k);
  }

  // TODO - now just need to set a reference of callbacks
  public void PythonCall(PythonInterface callback, Message msg) {
    callback.onMsg(msg);
    // return numbers;
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();
      Runtime.start("servo", "Servo");
      Py4j py4j = (Py4j) Runtime.start("py4j", "Py4j");
      py4j.start();

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  @Override
  public void connectionError(Exception e) {
    error(e);
  }

  @Override
  public void connectionStarted(Py4JServerConnection gatewayConnection) {
    info("connection started");
  }

  @Override
  public void connectionStopped(Py4JServerConnection gatewayConnection) {
    info("connection stopped");
  }

  @Override
  public void serverError(Exception e) {
    error("server error");
    error(e);
  }

  @Override
  public void serverPostShutdown() {
    info("%s post shutdown", getName());
  }

  @Override
  public void serverPreShutdown() {
    info("%s pre shutdown", getName());
  }

  @Override
  public void serverStarted() {
    info("%s started", getName());
  }

  @Override
  public void serverStopped() {
    info("%s stopped", getName());    
  }
  
  public void handleStdOut(String msg) {
    invoke("publishStdOut", msg);
  }

  public String publishStdOut(String data) {
    return data;
  }


}
