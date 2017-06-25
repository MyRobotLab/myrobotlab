package org.myrobotlab.service;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Blender extends Service {

  /**
   * Control line - JSON over TCP/IP This is the single control communication
   * line over which virtual objects are created - and linked with Serial
   * connections
   * 
   * Typically, a virtual object is created and if it has a serial line (like an
   * Arduino) a new TCP/IP connection is created which sends and receives the
   * binary serial data
   * 
   * @author GroG
   *
   */

  // MAKE NOTE - must NOT pretty print !!! \n will break messages
  private transient static Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss.SSS").disableHtmlEscaping().create();

  public class ControlHandler extends Thread {
    Socket socket;
    DataInputStream dis;
    boolean listening = false;

    public ControlHandler(Socket socket) {
      this.socket = socket;
    }

    @Override
    public void run() {
      BufferedReader in = null;
      try {
        listening = true;
        // in = socket.getInputStream();
        dis = new DataInputStream(socket.getInputStream());
        in = new BufferedReader(new InputStreamReader(dis));
        while (listening) {
          // handle inbound control messages

          // JSONObject json = new JSONObject(in.readLine());
          String json = in.readLine();
          log.info(String.format("%s", json));
          Message msg = gson.fromJson(json, Message.class);
          log.info(String.format("msg %s", msg));
          invoke(msg);

        }
      } catch (Exception e) {
        Logging.logError(e);
      } finally {
        try {
          if (in != null)
            in.close();
        } catch (Exception e) {
        }
      }
    }
  }

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Blender.class);

  public static final String SUCCESS = "SUCCESS";
  Socket control = null;
  transient ControlHandler controlHandler = null;
  
  String host = "localhost";
  Integer controlPort = 8989;
  

  Integer serialPort = 9191;
  String blenderVersion;

  /*
   * transient HashMap<String, VirtualPort> virtualPorts = new HashMap<String,
   * VirtualPort>();
   * 
   * public class VirtualPort { public Serial serial; public
   * VirtualNullModemCable cable; }
   */

  // Socket serial = null; NO

  String expectedBlenderVersion = "0.9";

  public Blender(String n) {
    super(n);
  }

  /*
   * important "attach" method for Blender - this way MRL World notifies Blender
   * to dynamically create "virtual" counterpart for device.
   * 
   */
  public synchronized void attach(Arduino service) {
    // let Blender know we are going
    // to virtualize an Arduino
    sendMsg("attach", service.getName(), service.getSimpleName());
  }

  public boolean connect() {
    try {
      if (control != null && control.isConnected()) {
        info("already connected");
        return true;
      }

      info("connecting to Blender.py %s %d", host, controlPort);
      control = new Socket(host, controlPort);
      controlHandler = new ControlHandler(control);
      controlHandler.start();

      info("connected - goodtimes");
      return true;
    } catch (Exception e) {
      error(e);
    }
    return false;
  }

  public boolean connect(String host, Integer port) {
    this.host = host;
    this.controlPort = port;
    return connect();
  }

  public boolean disconnect() {
    try {
      if (control != null) {
        control.close();
      }
      if (controlHandler != null) {
        controlHandler.listening = false;
        controlHandler = null;
      }
      // TODO - run through all serial connections
      return true;
    } catch (Exception e) {
      error(e);
    }
    return false;
  }

  // -------- publish api end --------

  // -------- Blender.py --> callback api begin --------
  public void getVersion() {
    sendMsg("getVersion");
  }

  boolean isConnected() {
    return (control != null) && control.isConnected();
  }

  // call back from blender
  /*
   * call back from Blender when python script does an attach to a virtual
   * device - returns name of the service attached
   */
  public synchronized String onAttach(String name) {
    try {

      info("onAttach - Blender is ready to attach serial device %s", name);
      // FIXME - MUST WAIT FOR ARDUINO PORT TO BE READY
      // THIS KLUDGE PREVENTS A RACE CONDITION
      Service.sleep(3000);
      // FIXME - more general case determined by "Type"
      ServiceInterface si = Runtime.getService(name);
      if ("org.myrobotlab.service.Arduino".equals(si.getType())) {
        // FIXME - make more general - "any" Serial device !!!
        Arduino arduino = (Arduino) Runtime.getService(name);
        if (arduino != null) {

          // get handle to serial service of the Arduino
          Serial serial = arduino.getSerial();

          // connecting over tcp ip
          serial.connectTcp(String.format("tcp://%s:%d", host, serialPort));

          // int vpn = virtualPorts.size();

          /*
           * VIRTUAL PORT IS NOT NEEDED !!! - JUST A SERIAL OVER TCP/IP YAY !!!!
           * :) VirtualPort vp = new VirtualPort(); vp.serial = (Serial)
           * Runtime.start(String.format("%s.UART.%d" ,arduino.getName(), vpn),
           * "Serial"); vp.cable =
           * Serial.createNullModemCable(String.format("MRL.%d", vpn),
           * String.format("BLND.%d", vpn)); virtualPorts.put(arduino.getName(),
           * vp); vp.serial.connect(String.format("BLND.%d", vpn));
           */
          // vp.serial.addRelay(host, serialPort);
          // arduino.connect(String.format("MRL.%d", vpn));
          // add the tcp relay pipes

        } else {
          error("onAttach %s not found", name);
        }
      }

    } catch (Exception e) {
      Logging.logError(e);
    }
    return name;
  }

  public String onBlenderVersion(String version) {
    blenderVersion = version;
    if (blenderVersion.equals(expectedBlenderVersion)) {
      info("Blender.py version is %s goodtimes", version);
    } else {
      error("Blender.py version is %s goodtimes", version);
    }

    return version;
  }

  public String onGetVersion(String version) {
    info("blender returned %s", version);
    return version;
  }

  // -------- Blender.py --> callback api end --------

  public void publishConnect() {
  }

  // -------- publish api begin --------
  public void publishDisconnect() {
  }

  public void sendMsg(String method, Object... data) {
    if (isConnected()) {
      try {
        Message msg = Message.createMessage(this, "Blender.py", method, data);
        OutputStream out = control.getOutputStream();
        // FIXME - this encoder needs to
        // NOT PRETTY PRINT - delimiter is \n PRETY PRINT WILL BREAK IT !!!
        // Should be able to request a "new" named thread safe encoder !!
        // Adding newline for message delimeter
        String json = String.format("%s\n", gson.toJson(msg));
        info("sending %s", json);
        out.write(json.getBytes());
      } catch (Exception e) {
        error(e);
      }
    } else {
      error("not connected");
    }
  }

  public void toJson() {
    sendMsg("toJson");
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      // create masters
      Blender blender = (Blender) Runtime.start("blender", "Blender");
      // gui
      Runtime.start("gui", "SwingGui");

      // connect blender service
      if (!blender.connect()) {
        throw new Exception("could not connect");
      }

      // get version
      blender.getVersion();

      String vLeftPort = "vleft";
      String vRightPort = "vright";

      // Step #0 pre-create MRL Arduino & Serial - an pre connect with tcp ip
      // port
      InMoov i01 = (InMoov) Runtime.start("i01", "InMoov");

      // Serial i01_left_serial =
      // (Serial)Runtime.createAndStart("i01.left.serial", "Serial");
      // i01_left_serial.connectTCP(host, port); // is this better (more access)
      // - or bury in blender.attach(?)

      Arduino i01_left = (Arduino) Runtime.start("i01.left", "Arduino");
      Arduino i01_right = (Arduino) Runtime.start("i01.right", "Arduino");

      // Step #1 - setup virtual arduino --- NOT SURE - can be done outside
      blender.attach(i01_left);
      sleep(3);
      blender.attach(i01_right);

      // Step #2 - i01 connects
      i01.startHead(vLeftPort);
      // i01.startMouthControl(bogusLeftPort);
      i01.startLeftArm(vLeftPort);
      i01.startLeftHand(vLeftPort);

      i01.startRightArm(vRightPort);
      i01.startRightHand(vRightPort);

      // left.biceps0
      // i01.head.neck
      /*
       * Servo neck = (Servo) Runtime.start("jaw2", "Servo");
       * 
       * Service.sleep(4000); // Servo rothead = (Servo)
       * Runtime.start("i01.head.rothead", // "Servo");
       * 
       * neck.attach(arduino01, 7); // rothead.attach(arduino01, 9);
       * 
       * // rothead.moveTo(90); neck.moveTo(90); sleep(100); //
       * rothead.moveTo(120); neck.moveTo(120); sleep(100); //
       * rothead.moveTo(0); neck.moveTo(0); sleep(100); // rothead.moveTo(90);
       * neck.moveTo(90); sleep(100); // rothead.moveTo(120); neck.moveTo(120);
       * sleep(100); // rothead.moveTo(0); neck.moveTo(0); sleep(100);
       * 
       * // servo01.sweep(); // servo01.stop(); neck.detach();
       * 
       * blender.getVersion(); // blender.toJson(); // blender.toJson();
       */

      // Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      Logging.logError(e);
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

    ServiceType meta = new ServiceType(Blender.class.getCanonicalName());
    meta.addDescription("interfaces Blender for simulation and display");
    meta.addCategory("display");
    meta.addCategory("simulator");
    return meta;
  }

}
