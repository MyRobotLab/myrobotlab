package org.myrobotlab.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.params.AuthPNames;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.Email;
import org.myrobotlab.pickToLight.Controller;
import org.myrobotlab.pickToLight.KitRequest;
import org.myrobotlab.pickToLight.Module;
import org.myrobotlab.pickToLight.ModuleList;
import org.myrobotlab.pickToLight.ModuleRequest;
import org.myrobotlab.pickToLight.PickEvent;
import org.myrobotlab.pickToLight.SOAPResponse;
import org.slf4j.Logger;

import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

/**
 * 
 * PickToLight - unknown service?
 * 
 * @author GroG
 * 
 *         C:\mrl\myrobotlab&gt;xjc -d src -p org.myrobotlab.pickToLight
 *         PickToLightTypes.xsd
 * 
 *         TODO - post report &amp; statistics TODO - update URI - meta data - make
 *         update.jar bin calls moduleList calls setAllBoxesLEDs (on off)
 *         setBoxesOn(String list) setBoxesOff(String list) getBesSwitchState()
 *         displayString(boxlist, str) ZOD update uri blinkOff TODO - automated
 *         registration Polling / Sensor - important - check sensor state FIXME
 *         - EROR is not being handled in non IP address &amp; no connectivity !!!!
 *         - read config in /boot/ - registration url including password - proxy
 *         ?
 * 
 */
public class PickToLight extends Service implements GpioPinListenerDigital {

  /**
   * Worker is a PickToLight level thread which operates over (potentially) all
   * of the service modules. Displays have their own
   * 
   */
  public class Worker extends Thread {

    public boolean isWorking = false;

    String task;
    Object[] data;
    int counter = 0;

    public Worker(String task, Object... data) {
      super(task);
      this.task = task;
      this.data = data;
    }

    @Override
    public void run() {
      try {

        isWorking = true;
        while (isWorking) {
          ++counter;

          switch (task) {

            case "blinkAll":
              for (Map.Entry<String, Module> o : modules.entrySet()) {
                Module m = o.getValue();
                if (counter % 2 == 0) {
                  m.ledOn();
                } else {
                  m.ledOff();
                }
              }

              // poll pause
              sleep(blinkDelayMs);
              break;

            case "blinkCycle":
              for (Map.Entry<String, Module> o : modules.entrySet()) {
                Module m = o.getValue();
                // if (counter%2 == 0){
                m.ledOn();
                // } else {
                sleep(blinkDelayMs);
                m.ledOff();
                // }
              }

              // poll pause
              sleep(blinkDelayMs);
              break;
            case "pollAll":
              for (Map.Entry<String, Module> o : modules.entrySet()) {
                Module m = o.getValue();
                // if display
                // read pause
                // sleep(30);
                m.display(Integer.toHexString(m.readSensor()));
              }

              // poll pause
              sleep(pollingDelayMs);
              break;

            case "learn":
              log.info("Worker - learn");
              for (Map.Entry<String, Module> o : modules.entrySet()) {
                Module m = o.getValue();
                log.info("sensor {} value {} ", m.getI2CAddress(), m.readSensor());
                if (m.readSensor() == 1) { // FIXME !!!! BITMASK
                  // READ !!!
                  // (m.readSensor() == 3
                  blinkOff(m.getI2CAddress());
                  // sendEvent("learn", new String[] {
                  // currentPresentationId, "" + m.getI2CAddress()
                  // });sdf\
                  sendEvent(new PickEvent(getController(), m));
                }
              }

              // poll pause
              sleep(pollingDelayMs);
              break;

            case "pollSet":
              log.info("Worker - pollSet");
              ArrayList<Module> list = ((ModuleList) data[0]).list;

              Iterator<Module> iter = list.iterator();
              while (iter.hasNext()) {
                Module m = iter.next();
                log.info("sensor {} value {} ", m.getI2CAddress(), m.readSensor());
                if (m.readSensor() == 1) { // FIXME !!!! BITMASK
                  // READ !!!
                  // (m.readSensor() == 3
                  sendEvent(new PickEvent(getController(), new Module(1, m.getI2CAddress())));
                  blinkOff(m.getI2CAddress());
                  iter.remove();
                }
              }

              if (list.size() == 0) {
                stopPolling();
                clearAll();
              }

              // poll pause
              sleep(pollingDelayMs);
              break;

            case "cycleAll":

              log.info("Worker - cycleAll");
              String msg = ("    " + (String) data[0] + "    ");

              // start with scroll on page
              for (int i = 0; i < msg.length() - 3; ++i) {
                for (Map.Entry<String, Module> o : modules.entrySet()) {
                  Module m = o.getValue();
                  m.display(msg.substring(i, i + 4));
                }
                sleep(cycleDelayMs);
              }

              sleep(cycleDelayMs); // so 0 length msgs don't peg cpu

              break;

            default:
              error(String.format("don't know how to handle task %s", task));
              break;
          }

        } // while is working
        log.info("leaving Worker");
      } catch (Exception e) {
        isWorking = false;
      }
    }

  }

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(PickToLight.class);

  transient public RasPi raspi;

  transient public WebGui webgui;
  transient public Worker cycleWorker;
  transient public Worker pollingWorker;

  transient public Worker blinkWorker;
  int cycleDelayMs = 300;
  int pollingDelayMs = 150;

  int blinkDelayMs = 150;

  String plant;

  // static HashMap<String, Module> modules = new HashMap<String, Module>();
  ConcurrentHashMap<String, Module> modules = new ConcurrentHashMap<String, Module>();
  // transient HashMap<String, Worker> workers = new HashMap<String,
  // Worker>();
  public final static String MODE_KITTING = "kitting";
  public final static String MODE_LEARNING = "learning";
  public final static String MODE_INSTALLING = "installing";

  public final static String MODE_STARTING = "starting";
  String mode = "kitting";

  String messageGoodPick = "GOOD";
  final public static String soapRegisterTemplate = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:a=\"http://schemas.microsoft.com/2003/10/Serialization/Arrays\" xmlns:tem=\"http://tempuri.org/\"><soapenv:Header/><soapenv:Body><tem:RegisterController><tem:plant>%s</tem:plant><tem:Name>%s</tem:Name><tem:MACAddress>%s</tem:MACAddress><tem:IPAddress>%s</tem:IPAddress><tem:I2CAddresses>%s</tem:I2CAddresses></tem:RegisterController></soapenv:Body></soapenv:Envelope>";

  // final public static String soapEventTemplate =
  // "<soapenv:Envelope
  // xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"
  // xmlns:tem=\"http://tempuri.org/\"><soapenv:Header/><soapenv:Body><tem:plant>%s</tem:plant><tem:Event><tem:Type>%s</tem:Type><tem:Data>%s</tem:Data></tem:Event></soapenv:Body></soapenv:Envelope>";
  final public static String soapNotifyTemplate = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:a=\"http://schemas.microsoft.com/2003/10/Serialization/Arrays\" xmlns:tem=\"http://tempuri.org/\"><soapenv:Header/><soapenv:Body><tem:NotifyMES><tem:plant>%s</tem:plant><tem:type>%s</tem:type><tem:eventData>%s</tem:eventData></tem:NotifyMES></soapenv:Body></soapenv:Envelope>";
  public final static String ERROR_CONNECTION_REFUSED = "E001";
  public final static String ERROR_CONNECTION_RESET = "E002";

  public final static String ERROR_NO_RESPONSE = "E003";
  private int rasPiBus = 1;
  // FIXME - who will update me ?
  private String updateURL;
  private int blinkNumber = 5;

  private int blinkDelay = 300;

  transient Timer timer;
  KitRequest lastKitRequest = null;
  Date lastKitRequestDate;
  int kitRequestCount;

  Properties properties = new Properties();

  public static void main(String[] args) {
    LoggingFactory.init(Level.DEBUG);

    try {
      // Runtime.getStartInfo();

      PickToLight pick = new PickToLight("pick.1");
      String response = pick.getServerTime();
      log.info("Response: {}", response);
      // pick.sendSoap(soapAction, soapEnv);

      pick.sendEvent(new PickEvent(pick.getController(), new Module(1, 13)));

      pick.startService();
      pick.systemCheck();

      boolean stopHere = true;
      if (stopHere) {
        return;
      }

      pick.register();

      pick.autoRefreshI2CDisplay(1);

      boolean ret = true;
      if (ret) {
        return;
      }

      pick.register();
      pick.createModules();

      // Controller controller = pick.getController();
      pick.startService();

      int selector = 0x83; // IR selected - LED OFF

      /*
       * int MASK_DISPLAY = 0x01; int MASK_LED = 0x02; int MASK_SENSOR = 0x80;
       */

      log.info(String.format("0x%s", Integer.toHexString(selector)));
      selector &= ~Module.MASK_LED; // LED ON
      log.info(String.format("0x%s", Integer.toHexString(selector)));
      selector |= Module.MASK_LED; // LED OFF
      log.info(String.format("0x%s", Integer.toHexString(selector)));

      List<String> ips = Runtime.getLocalAddresses();

      for (int i = 0; i < ips.size(); ++i) {
        log.info(ips.get(i));
      }

      ips = Runtime.getLocalHardwareAddresses();

      for (int i = 0; i < ips.size(); ++i) {
        log.info(ips.get(i));
      }

      // Controller2 c = pick.getController();
      // log.info("{}", c);

      // String binList = pickToLight.getBoxList();
      // pickToLight.display(binList, "helo");
      /*
       * pickToLight.display("01", "1234"); pickToLight.display(" 01 02 03 ",
       * "1234  1"); pickToLight.display("01 03", " 1234"); //
       * pickToLight.display(binList, "1234 ");
       */

      // Runtime.createAndStart("web", "WebGui");

      // Runtime.createAndStart("webgui", "WebGui");
      /*
       * SwingGui gui = new SwingGui("gui"); gui.startService();
       */
    } catch (Exception e) {
      Logging.logError(e);
    }

  }

  public PickToLight(String n) {
    super(n);
    webgui = (WebGui) createPeer("webgui");
    webgui.autoStartBrowser(false);
    webgui.useLocalResources(true);
    raspi = (RasPi) createPeer("raspi");
    loadProperties();
  }

  public void autoCheckForUpdates(int seconds) {
    addTask(seconds * 1000, "checkForUpdates");
  }

  public void autoRefreshI2CDisplay(int seconds) {
    addTask(seconds * 1000, "refreshI2CDisplay");
  }

  public void autoRegister(int seconds) {
    addTask(seconds * 1000, "register");
  }

  public void blinkAllOn() {
    log.info("blinkAllOn");
    blinkStop();

    blinkWorker = new Worker("blinkAll");
    blinkWorker.start();
  }

  public void blinkAllOn(String msg) {
    for (Map.Entry<String, Module> o : modules.entrySet()) {
      blinkOn(o.getValue().getI2CAddress(), msg);
    }
  }

  public void blinkCycle() {
    log.info("blinkAllOn");
    blinkStop();

    blinkWorker = new Worker("blinkCycle");
    blinkWorker.start();
  }

  public void blinkOff(Integer address) {
    blinkOff(address, null);
  }

  public void blinkOff(Integer address, String msg) {
    blinkOff(address, msg, blinkNumber, blinkDelay);
  }

  public void blinkOff(Integer address, String msg, int blinkNumber, int blinkDelay) {
    getModule(address).blinkOff(msg, blinkNumber, blinkDelay);
  }

  // public final
  // IN MEMORY ERROR LIST !!!!!!
  // getErrors( Error - key - detail - time )

  public void blinkOn(Integer address, String msg) {
    blinkOn(address, msg, blinkNumber, blinkDelay);
  }

  public void blinkOn(Integer address, String msg, int blinkNumber, int blinkDelay) {
    getModule(address).blinkOn(msg, blinkNumber, blinkDelay);
  }

  public void blinkStop() {
    if (blinkWorker != null) {
      blinkWorker.interrupt();
      blinkWorker.isWorking = false;
      blinkWorker = null;
    }
  }

  public void clearAll() {
    for (Map.Entry<String, Module> o : modules.entrySet()) {
      o.getValue().clear();
    }

    stopPolling();
  }

  public KitRequest createKitRequest() {
    KitRequest kit = new KitRequest();
    kit.vin = "8374756";
    kit.kitId = "324";

    kit.list = new ModuleRequest[modules.size()];
    int i = 0;
    for (Map.Entry<String, Module> o : modules.entrySet()) {

      ModuleRequest mr = new ModuleRequest();
      mr.i2c = o.getValue().getI2CAddress();
      mr.quantity = "" + (int) ((Math.random() * 5) + 1);

      kit.list[i] = mr;
      ++i;
    }
    return kit;
  }

  public boolean createModule(int bus, int address) {
    String key = makeKey(address);
    log.info(String.format("create module key %s (bus %d address %d)", key, bus, address));
    Module box = new Module(bus, address);
    modules.put(key, box);
    return true;
  }

  public void createModules() {

    Integer[] devices = scanI2CDevices();

    // rather heavy handed no?
    modules.clear();

    log.info(String.format("found %d devices", devices.length));

    for (int i = 0; i < devices.length; ++i) {
      int deviceAddress = devices[i];
      // FIXME - kludge to work with our prototype
      // addresses of displays are above 100
      /*
       * if (deviceAddress > 100) { createModule(rasPiBus, deviceAddress); }
       */

      createModule(rasPiBus, deviceAddress);
    }
  }

  // ---- cycling message on individual module begin ----
  public void cycle(Integer address, String msg) {
    cycle(address, msg, 300);
  }

  public void cycle(Integer address, String msg, Integer delay) {
    getModule(address).cycle(msg, delay);
  }

  public void cycleAll(String msg) {
    log.info("cycleAll");
    cycleAllStop();

    cycleWorker = new Worker("cycleAll", msg);
    cycleWorker.start();
  }

  public void cycleAllStop() {
    if (cycleWorker != null) {
      cycleWorker.interrupt();
      cycleWorker.isWorking = false;
      cycleWorker = null;
    }
  }

  public boolean cycleIPAddress() {
    Controller c = getController();
    String ip = c.getIpAddress();
    if (ip == null || ip.length() == 0) {
      error("could not get ip");
      return false;
    }
    cycleAll(ip);
    return true;
  }

  public String display(Integer address, String msg) {
    String key = makeKey(address);
    if (modules.containsKey(key)) {
      modules.get(key).display(msg);
      return msg;
    } else {
      String err = String.format("display could not find module %d", key);
      error(err);
      return err;
    }
  }

  // FIXME normalize splitting code
  public String display(String moduleList, String value) {
    if (moduleList == null) {
      error("box list is null");
      return "box list is null";
    }
    String[] list = moduleList.split(" ");
    for (int i = 0; i < list.length; ++i) {
      try {
        String strKey = list[i].trim();
        if (strKey.length() > 0) {

          String key = makeKey(Integer.parseInt(strKey));
          if (modules.containsKey(key)) {
            modules.get(key).display(value);
          } else {
            error(String.format("display could not find module %s", strKey));
          }
        }
      } catch (Exception e) {
        Logging.logError(e);
      }
    }
    return moduleList;
  }

  public String displayAll(String msg) {

    for (Map.Entry<String, Module> o : modules.entrySet()) {
      Module mc = o.getValue();
      mc.display(msg);
    }

    return msg;
  }

  public void displayI2CAddresses() {
    for (Map.Entry<String, Module> o : modules.entrySet()) {
      o.getValue().display(o.getKey());
    }
  }

  public void drawColon(Integer bus, Integer address, boolean draw) {

  }

  public int getBlinkDelayMs() {
    return blinkDelayMs;
  }

  public Controller getController() {

    try {

      // WARNING WARNING WARNING WARNING WARNING WARNING WARNING
      // register calls getController at regular interval
      // so modules are re-recated at that interval
      // createModules();

      Controller controller = new Controller();

      controller.setVersion(Runtime.getVersion());
      controller.setName(getName());

      String ip = "";
      String mac = "";

      List<String> addresses = Runtime.getLocalAddresses();
      if (addresses.size() != 1) {
        error(String.format("incorrect number of ip addresses %d", addresses.size()));
      }

      if (!addresses.isEmpty()) {
        ip = addresses.get(0);
      }

      List<String> macs = Runtime.getLocalHardwareAddresses();
      if (macs.size() != 1) {
        error(String.format("incorrect number of mac addresses %d", addresses.size()));
      }
      if (!macs.isEmpty()) {
        mac = macs.get(0);
      }

      controller.setIpAddress(ip);
      controller.setMacAddress(mac);

      controller.setModules(modules);

      return controller;
    } catch (Exception e) {
      Logging.logError(e);
    }
    return null;
  }

  public int getCycleDelayMs() {
    return cycleDelayMs;
  }

  public String getMode() {
    return mode;
  }

  public Module getModule(Integer address) {
    return getModule(rasPiBus, address);
  }

  public Module getModule(Integer bus, Integer address) {
    String key = makeKey(bus, address);
    if (!modules.containsKey(key)) {
      error(String.format("get module - could not find module with key %s", key));
      return null;
    }
    return modules.get(key);
  }

  public int getPollingDelayMs() {
    return pollingDelayMs;
  }

  public String getServerTime() {
    String soapAction = "http://tempuri.org/SoapService/GetServerTime";
    String soapEnv = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tem=\"http://tempuri.org/\">   <soapenv:Header/>   <soapenv:Body>      <tem:GetServerTime/>   </soapenv:Body></soapenv:Envelope>";
    log.info(String.format("sendSoap - action %s [%s]", soapAction, soapEnv));
    String mesEndpoint = properties.getProperty("mes.endpoint");
    String mesUser = properties.getProperty("mes.user");
    String mesDomain = properties.getProperty("mes.domain");
    String mesPassword = properties.getProperty("mes.password");

    log.info(String.format("mesEndpoint %s", mesEndpoint));
    log.info(String.format("mesUser %s", mesUser));
    log.info(String.format("mesDomain %s", mesDomain));
    log.info(String.format("mesPassword %s", mesPassword));

    String ret = "";

    try {
      CloseableHttpClient client = HttpClients.createDefault();
      List<String> authpref = new ArrayList<String>();
      /* ALL DEPRECATED
      authpref.add(AuthPolicy.NTLM);
      client.getParams().setParameter(AuthPNames.TARGET_AUTH_PREF, authpref);
      NTCredentials creds = new NTCredentials(mesUser, mesPassword, "", mesDomain);
      client.getCredentialsProvider().setCredentials(AuthScope.ANY, creds);
	  */
      
      HttpContext localContext = new BasicHttpContext();
      HttpPost post = new HttpPost(mesEndpoint);

      // ,"utf-8"
      StringEntity stringentity = new StringEntity(soapEnv);
      stringentity.setChunked(true);
      post.setEntity(stringentity);
      post.addHeader("Accept", "text/xml");
      post.addHeader("SOAPAction", soapAction);
      post.addHeader("Content-Type", "text/xml; charset=utf-8");

      HttpResponse response = client.execute(post, localContext);
      HttpEntity entity = response.getEntity();
      ret = EntityUtils.toString(entity);

      // parse the response - check
    } catch (Exception e) {
      error("endpoint %s user %s domain %s password %s", mesEndpoint, mesUser, mesDomain, mesPassword);
      Logging.logError(e);
      ret = e.getMessage();
    }

    log.info(String.format("soap response [%s]", ret));
    return ret;

  }

  public String getVersion() {
    return Runtime.getVersion();
  }

  // DEPRECATE ???
  @Override
  public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
    // display pin state on console

    System.out.println(" --> GPIO PIN STATE CHANGE: " + event.getPin() + " [" + event.getPin().getName() + "]" + " = " + event.getState());
    GpioPin pin = event.getPin();
    log.info("GPIOPin: {}", pin);
    /*
     * if (pin.getName().equals("GPIO 0")) { modules.get("01").blinkOff("ok"); }
     * else if (pin.getName().equals("GPIO 1")) {
     * modules.get("02").blinkOff("ok"); } else if (pin.getName().equals(
     * "GPIO 2")) { modules.get("03").blinkOff("ok"); } else if
     * (pin.getName().equals("GPIO 3")) { modules.get("04").blinkOff("ok"); }
     */

    // if (pin.getName().equals(anObject))
  }

  // need a complex type
  public KitRequest kitToLight(KitRequest kit) {
    log.info("KitRequest");
    clearAll();
    if (kit == null) {
      error("kitToLight - kit is null");
      return null;
    }

    lastKitRequest = kit;
    lastKitRequestDate = new Date();
    ++kitRequestCount;

    info("kitToLight vin %s kit %s", kit.vin, kit.kitId);

    if (kit.list == null) {
      error("kit list is null");
    }

    log.info("found {} ModuleRequests", kit.list.length);

    ModuleList pollingList = new ModuleList();
    for (int i = 0; i < kit.list.length; ++i) {
      ModuleRequest mr = kit.list[i];
      String key = makeKey(mr.i2c);
      if (modules.containsKey(key)) {
        Module m = modules.get(key);

        m.display(mr.quantity);
        m.ledOn();

        pollingList.list.add(m);

      } else {
        error("could not find i2c address %d for vin %s kitId %s", mr.i2c, kit.vin, kit.kitId);
      }
    }

    pollSet(pollingList);

    return kit;
  }

  public void learn(String presentationId) {
    log.info(String.format("learn %s", presentationId));
    stopPolling();

    pollingWorker = new Worker("learn");
    pollingWorker.start();
  }

  public void ledOff(Integer address) {
    String key = makeKey(address);
    log.info("ledOff address {}", key);
    if (modules.containsKey(key)) {
      modules.get(key).ledOff();
    } else {
      // FIXME - Service Error Cache !!!! - IN GLOBAL getModule !
      error("ledOff could not find module %d", key);
    }
  }

  // ---- cycling message on individual module end ----

  public void ledOn(Integer address) {
    String key = makeKey(address);
    log.info("ledOn address {}", key);
    if (modules.containsKey(key)) {
      modules.get(key).ledOn();
    } else {
      // FIXME - Service Error Cache !!!! - IN GLOBAL getModule !
      error("ledOn could not find module %d", key);
    }
  }

  public void ledsAllOff() {
    TreeMap<String, Module> sorted = new TreeMap<String, Module>(modules);
    for (Map.Entry<String, Module> o : sorted.entrySet()) {
      o.getValue().ledOff();
    }
  }

  public void ledsAllOn() {
    TreeMap<String, Module> sorted = new TreeMap<String, Module>(modules);
    for (Map.Entry<String, Module> o : sorted.entrySet()) {
      o.getValue().ledOn();
    }
  }

  public Properties loadProperties() {
    InputStream input = null;

    try {

      log.info("loading default properties");
      properties.load(PickToLight.class.getResourceAsStream("/resource/PickToLight/pickToLight.properties"));

      log.info("loading mes properties");
      input = new FileInputStream("/boot/pickToLight.properties");
      properties.load(input);

      if ("true".equalsIgnoreCase(properties.getProperty("xmpp.enabled"))) {
        // Xmpp xmpp = (Xmpp) Runtime.createAndStart("xmpp", "Xmpp");
        Runtime.createAndStart("xmpp", "Xmpp");
        // xmpp.connect(properties.getProperty("xmpp.user"),
        // properties.getProperty("xmpp.password"));
        // FIXME - xmpp.addAuditor("Greg Perry");
      }

    } catch (Exception ex) {
      Logging.logError(ex);
    } finally {
      if (input != null) {
        try {
          input.close();
        } catch (IOException e) {
          // dont' care
        }
      }
    }

    plant = properties.getProperty("mes.plant");
    log.info(String.format("operating in plant [%s]", plant));
    if (plant == null) {
      log.error("invalid plant");
    }
    return properties;
  }

  /*
   * single location for key generation - in case other parts are add in a
   * composite key
   */
  public String makeKey(Integer address) {
    return makeKey(rasPiBus, address);
  }

  public String makeKey(Integer bus, Integer address) {
    // return String.format("%d.%d", bus, address);
    return String.format("%d", address);
  }

  public void pollAll() {
    log.info("pollAll");
    stopPolling();

    pollingWorker = new Worker("pollAll");
    pollingWorker.start();
  }

  public void pollSet(ModuleList moduleList) {
    log.info("pollSet");
    stopPolling();

    pollingWorker = new Worker("pollSet", moduleList);
    pollingWorker.start();
  }

  public void refreshI2CDisplay() {
    createModules();
    displayI2CAddresses();
  }

  public SOAPResponse register() {
    Controller controller = getController();
    StringBuffer sb = new StringBuffer();

    for (Map.Entry<String, Module> o : modules.entrySet()) {
      Module m = o.getValue();
      sb.append(String.format("<a:string>%s</a:string>", m.getI2CAddress()));
    }

    String body = String.format(soapRegisterTemplate, plant, "name", controller.getMacAddress(), controller.getIpAddress(), sb.toString());
    String soapResponse = sendSoap("http://tempuri.org/SoapService/RegisterController", body);

    SOAPResponse ret = new SOAPResponse();

    if (soapResponse == null || soapResponse.length() == 0) {
      ret.setError(ERROR_NO_RESPONSE);
    } else if (soapResponse.contains("reset")) {
      ret.setError(ERROR_CONNECTION_RESET);
    } else if (soapResponse.contains("refused")) {
      ret.setError(ERROR_CONNECTION_RESET);
    }

    log.info(soapResponse);

    return ret;
  }

  public Integer[] scanI2CDevices() {
    ArrayList<Integer> ret = new ArrayList<Integer>();

    // our modules don't have addresses above 56
    Integer[] all = raspi.scanI2CDevices(rasPiBus);
    for (int i = 0; i < all.length; ++i) {
      Integer address = all[i];
      if (address > 56) {
        continue;
      }

      ret.add(all[i]);
    }

    return ret.toArray(new Integer[ret.size()]);
  }

  public void sendEmail() {
    try {
      String host = properties.getProperty("mail.smtp.host", "mail.us164.corpintra.net");
      String port = properties.getProperty("mail.smtp.port", "25");
      Email email = new Email();
      email.setEmailServer(host, Integer.parseInt(port));
      Controller c = getController();
      String[] to = properties.getProperty("mail.smtp.to", "greg.perry@daimler.com,brett.hutton@daimler.com").split(",");
      email.sendEmail(to, String.format("Hello from Controller %s %s", c.getMacAddress(), c.getIpAddress()), CodecUtils.toJson(c));
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public String sendEvent(Object event) {
    return sendEvent(event.getClass().getSimpleName(), event);
  }

  public String sendEvent(String eventType, Object data) {
    String body = String.format(soapNotifyTemplate, plant, eventType, CodecUtils.toJson(data));
    return sendSoap("http://tempuri.org/SoapService/NotifyMES", body);
  }

  public String sendSoap(String soapAction, String soapEnv) {
    log.info(String.format("sendSoap - action %s [%s]", soapAction, soapEnv));
    String mesEndpoint = properties.getProperty("mes.endpoint");
    String mesUser = properties.getProperty("mes.user");
    String mesDomain = properties.getProperty("mes.domain");
    String mesPassword = properties.getProperty("mes.password");

    log.info(String.format("mesEndpoint %s", mesEndpoint));
    log.info(String.format("mesUser %s", mesUser));
    log.info(String.format("mesDomain %s", mesDomain));
    log.info(String.format("mesPassword %s", mesPassword));

    String ret = "";

    try {
      DefaultHttpClient client = new DefaultHttpClient();
      List<String> authpref = new ArrayList<String>();
      authpref.add(AuthPolicy.NTLM);
      client.getParams().setParameter(AuthPNames.TARGET_AUTH_PREF, authpref);
      NTCredentials creds = new NTCredentials(mesUser, mesPassword, "", mesDomain);
      client.getCredentialsProvider().setCredentials(AuthScope.ANY, creds);

      HttpContext localContext = new BasicHttpContext();
      HttpPost post = new HttpPost(mesEndpoint);

      // ,"utf-8"
      StringEntity stringentity = new StringEntity(soapEnv);
      stringentity.setChunked(true);
      post.setEntity(stringentity);
      post.addHeader("Accept", "text/xml");
      post.addHeader("SOAPAction", soapAction);
      post.addHeader("Content-Type", "text/xml; charset=utf-8");

      HttpResponse response = client.execute(post, localContext);
      HttpEntity entity = response.getEntity();
      ret = EntityUtils.toString(entity);

      // parse the response - check
    } catch (Exception e) {
      error("endpoint %s user %s domain %s password %s", mesEndpoint, mesUser, mesDomain, mesPassword);
      Logging.logError(e);
      ret = e.getMessage();
    }

    log.info(String.format("soap response [%s]", ret));
    return ret;

  }

  public int setBlinkDelayMs(int blinkDelayMs) {
    this.blinkDelayMs = blinkDelayMs;
    return blinkDelayMs;
  }

  public int setBrightness(Integer address, Integer level) {
    return level;
  }

  public int setCycleDelayMs(int cycleDelayMs) {
    this.cycleDelayMs = cycleDelayMs;
    return cycleDelayMs;
  }

  public void setMode(String mode) {
    if (MODE_LEARNING.equalsIgnoreCase(mode)) {
      ledsAllOn();
      displayI2CAddresses();
    }
    this.mode = mode;
  }

  public int setPollingDelayMs(int pollingDelayMs) {
    this.pollingDelayMs = pollingDelayMs;
    return pollingDelayMs;
  }

  public void start() {
    createModules();

    systemCheck();
    autoRegister(1800);
  }

  @Override
  public void startService() {
    super.startService();

    raspi.startService();
    webgui.startService();

  }

  public void stopPolling() {
    if (pollingWorker != null) {
      pollingWorker.interrupt();
      pollingWorker.isWorking = false;
      pollingWorker = null;
    }
  }

  public void systemCheck() {
    // TODO put into state mode - system check
    // check current mode see if its possible

    if (properties.getProperty("splashscreen") != null) {
      cycleAll(properties.getProperty("splashscreen"));
      sleep(100000);
    }

    blinkAllOn("test");
    Controller c = getController();
    sleep(3000);

    // ip address
    displayAll("ip  ");
    sleep(1000);
    cycleAll(c.getIpAddress());
    sleep(6000);
    cycleAllStop();

    // mac address
    displayAll("mac ");
    sleep(1000);
    cycleAll(c.getMacAddress());
    sleep(5000);
    cycleAllStop();

    // i2c
    displayAll("i2c ");
    sleep(1000);
    displayAll(String.format("%d", modules.size()));
    sleep(3000);
    displayI2CAddresses();
    sleep(4000);

    // conn address
    displayAll("conn");
    sleep(1000);
    SOAPResponse sr = register();
    if (sr.isError()) {
      blinkAllOn(sr.getError());
    } else {
      displayAll("good");
    }

    sendEmail();

    displayAll("done");

    sleep(5000);
    clearAll();
  }

  public String update() {
    return update(updateURL);
  }

  // ------------ TODO - IMPLEMENT - END ----------------------

  // ------------ TODO - IMPLEMENT - BEGIN ----------------------
  public String update(String url) {
    // TODO - auto-update
    return "TODO - auto update";
  }

  public void writeToDisplay(int address, byte b0, byte b1, byte b2, byte b3) {
    try {

      I2CBus i2cbus = I2CFactory.getInstance(rasPiBus);
      I2CDevice device = i2cbus.getDevice(address);
      device.write(address, (byte) 0x80);

      I2CDevice display = i2cbus.getDevice(0x38);
      display.write(new byte[] { 0, 0x17, b0, b1, b2, b3 }, 0, 6);

      device.write(address, (byte) 0x83);

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

    ServiceType meta = new ServiceType(PickToLight.class.getCanonicalName());
    meta.addDescription("Pick to light system");
    meta.addCategory("industrial");
    meta.addPeer("raspi", "RasPi", "raspi");
    meta.addPeer("webgui", "WebGui", "web server interface");
    // FIXME - should use static methos from HttpClient
    meta.addDependency("org.apache.commons.httpclient", "4.5.2");
    // FIXME - to specific to a partical hardware setup
    // good idea though
    meta.setAvailable(false);

    return meta;
  }

}
