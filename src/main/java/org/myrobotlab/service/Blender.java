package org.myrobotlab.service;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.ServiceConfig;
import org.slf4j.Logger;

public class Blender extends Service<ServiceConfig> {

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
          log.info("{}", json);
          Message msg = CodecUtils.fromJson(json, Message.class);
          log.info("msg {}", msg);
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

  transient public final static Logger log = LoggerFactory.getLogger(Blender.class);

  public static final String SUCCESS = "SUCCESS";

  transient Socket control = null;

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

  public Blender(String n, String id) {
    super(n, id);
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
      if ("org.myrobotlab.service.Arduino".equals(si.getTypeKey())) {
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
        Message msg = Message.createMessage(getName(), "Blender.py", method, data);
        OutputStream out = control.getOutputStream();
        // FIXME - this encoder needs to
        // NOT PRETTY PRINT - delimiter is \n PRETY PRINT WILL BREAK IT !!!
        // Should be able to request a "new" named thread safe encoder !!
        // Adding newline for message delimeter
        String json = String.format("%s\n", CodecUtils.toJson(msg));
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

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
