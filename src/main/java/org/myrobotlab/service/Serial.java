package org.myrobotlab.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.TcpSerialHub;
import org.myrobotlab.serial.Port;
import org.myrobotlab.serial.PortQueue;
import org.myrobotlab.serial.PortStream;
import org.myrobotlab.serial.SerialControl;
import org.myrobotlab.service.config.SerialConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.interfaces.PortConnector;
import org.myrobotlab.service.interfaces.PortPublisher;
import org.myrobotlab.service.interfaces.QueueSource;
import org.myrobotlab.service.interfaces.RecordControl;
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.myrobotlab.service.interfaces.SerialDevice;
import org.slf4j.Logger;

/**
 * 
 * Serial - a service that allows reading and writing to a serial port device.
 *
 */
public class Serial extends Service implements SerialControl, QueueSource, SerialDataListener, RecordControl, SerialDevice, PortPublisher, PortConnector {

  /**
   * general read timeout - 0 is infinite &gt; 0 is number of milliseconds to
   * wait up to, until data is returned timeout = null: wait forever timeout =
   * 0: non-blocking mode (return immediately on read) timeout = x: set timeout
   * to x milliseconds
   */

  private Integer timeoutMS = null;

  private static final long serialVersionUID = 1L;

  // rates
  public final static Integer BAUD_2400 = 2400;
  public final static Integer BAUD_4800 = 4800;
  public final static Integer BAUD_9600 = 9600;
  public final static Integer BAUD_19200 = 19200;
  public final static Integer BAUD_38400 = 38400;
  public final static Integer BAUD_57600 = 57600;
  public final static Integer BAUD_115200 = 115200;

  /**
   * different hardware library - hotspot only
   */
  final public static String HARDWARE_LIBRARY_JSSC = "org.myrobotlab.serial.PortJSSC";

  /**
   * Android only bluetooth library
   */
  final public static String HARDWARE_LIBRARY_ANDROID_BLUETOOTH = "android.somethin";

  transient public final static Logger log = LoggerFactory.getLogger(Serial.class);

  /**
   * cached list of portnames on the system
   */
  static HashSet<String> portNames = new HashSet<String>();

  /**
   * blocking and non-blocking publish/subscribe reading is possible at the same
   * time. If blocking is not used then the internal buffer will fill to the
   * BUFFER_SIZE and just be left - overrun data will be lost
   */
  int BUFFER_SIZE = 1024;

  /**
   * blocking queue for blocking rx read requests
   */
  transient BlockingQueue<Integer> blockingRX = new LinkedBlockingQueue<Integer>();

  /**
   * our set of ports we have access to. This is a shared resource between ALL
   * serial services. It should be possible simply to iterate through this list
   * to get all (cached) names .. operating system ports may have changed and
   * need re-querying
   * 
   * it also might be worthwhile to keep a "static" list for remote and virtual
   * ports so that remote and other services can have access to that list
   * 
   * has to be transient because many Ports are not serializable
   * 
   * remote manipulations and identification should always be done through
   * portNames
   */
  transient static HashMap<String, Port> ports = new HashMap<String, Port>();

  /**
   * all the ports we are currently connected to typically there is 0 to 1
   * connected ports - however the serial service has the ability to "fork"
   * ports where it is connected to 2 or more ports simultaneously
   */
  final transient Map<String, Port> connectedPorts = new HashMap<>();

  /**
   * used as the "default" port - now that Serial can multiplex with multiple
   * ports - the default is used for methods which are not explicit ... e.g.
   * connect(), disconnect() etc.. are now equivalent to connect(portName),
   * disconnect(portName)
   */
  String portName = null;

  /**
   * last port name which was connected to - still has name when portName is
   * null and disconnected
   */
  public String lastPortName;

  /**
   * "the" port - there is only one although we can fork and multiplex others.
   * This is the port which we determine if this Serial service is connected or
   * not
   */
  transient Port port = null;

  /**
   * we need to dynamically load our preferred hardware type, because we may
   * want to change it or possibly the platform does not support it. When it is
   * null - we will let MRL figure out what is best.
   */
  static String hardwareLibrary = null;

  transient OutputStream recordRx = null;
  transient OutputStream recordTx = null;

  static List<String> formats = null;
  static String format = "hex";

  /**
   * number of tx bytes
   */
  int txCount = 0;

  /**
   * number of received bytes
   */
  int rxCount = 0;

  /**
   * default bps
   */
  int rate = 115200;

  /**
   * default dataBits
   */
  int dataBits = 8;

  /**
   * default stopBits
   */
  int stopBits = 1;

  /**
   * default parity
   */
  int parity = 0;

  private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

  transient TcpSerialHub tcpSerialHub = new TcpSerialHub();

  /**
   * list of RX listeners - if "local" they will be immediately called back by
   * the serial device's thread when data arrives, if they are "remote" they
   * should be published to. They can subscribe to the publishRX method when a
   * lister is added. Serial is the first listener added to this map
   */
  transient Map<String, SerialDataListener> listeners = new HashMap<>();

  /**
   * conversion utility TODO - support endianess
   * 
   * @param bytes
   *          - bytes to convert
   * @param offset
   *          - offset into bytes
   * @param length
   *          - length of data to convert
   * @return the integer that represents the bytes
   */
  public static int bytesToInt(int[] bytes, int offset, int length) {
    return (int) bytesToLong(bytes, offset, length);
  }

  /**
   * conversion utility TODO - support endianess
   * 
   * @param bytes
   *          the input array
   * @param offset
   *          where to start
   * @param length
   *          how many bytes
   * @return the decoded long
   * 
   */
  public static long bytesToLong(int[] bytes, int offset, int length) {

    long retVal = 0;

    for (int i = 0; i < length; ++i) {
      retVal |= (bytes[offset + i] & 0xff);
      if (i != length - 1) {
        retVal <<= 8;
      }
    }

    return retVal;
  }

  /**
   * Static list of third party dependencies for this service. The list will be
   * consumed by Ivy to download and manage the appropriate resources
   * 
   * @param n
   *          name
   * @param id
   *          instance ide
   */

  public Serial(String n, String id) {
    super(n, id);
    listeners.put(n, this);
    if (formats == null) {
      formats = new ArrayList<String>();
      formats.add("bin");
      formats.add("hex");
      formats.add("dec");
    }
    getPortNames();
  }

  public void addByteListener(SerialDataListener listener) {
    log.info("Adding a Byte listener... {} publishes to {}", getName(), listener.getName());
    listeners.put(listener.getName(), listener);
  }

  /**
   * Method which either sets up the pub/sub remote or assigns a local reference
   * from the publishing thread
   * 
   * FIXME - this is no longer a "Good" pattern - the framework now does this
   * auto-magically, where local pub/sub listeners will "directly" be invoked,
   * and remote pub/sub listeners will be sent remotely
   * 
   * FIXME - this now violates the good pattern. A good pattern simply lets the
   * framework handle the details of local/remote and this function should
   * definitely NOT get a direct reference to the service
   * 
   * @param name
   *          the name of the listener
   * 
   */
  public void addByteListener(String name) {
    log.info("Add Byte Listener for Name {}", name);
    ServiceInterface si = Runtime.getService(name);

    if (si == null) {
      log.error("{} service interface is null!");
      return;
    }

    if (si != null && SerialDataListener.class.isAssignableFrom(si.getClass()) && si.isLocal()) {
      // local optimization
      addByteListener((SerialDataListener) si);
    } else {
      // TODO: review this section here.. we might have double publishing going
      // on.
      // pub sub
      addListener("publishRX", name, "onByte");
      addListener("publishBytes", name, "onBytes");
      addListener("publishConnect", name, "onConnect");
      addListener("publishDisconnect", name, "onDisconnect");
    }
  }

  public void addPortListener(String name) {
    addListener("publishConnect", name, "onConnect");
    addListener("publishDisconnect", name, "onDisconnect");
  }

  /**
   * method similar to InputStream's
   */
  public int available() {
    return blockingRX.size();
  }

  /**
   * clears the rx buffer
   */
  public void clear() {
    blockingRX.clear();
  }

  /**
   * for backwards compatibility
   * 
   */
  public void connect(String name) throws IOException {
    open(name);
  }

  public void connect(String name, int baudRate, int dataBits, int stopBits, int parity) throws IOException {
    open(name);
    setParams(baudRate, dataBits, stopBits, parity);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.myrobotlab.service.SerialDevice#open(java.lang.String)
   */
  @Override
  public void open(String name) throws IOException {
    open(name, rate, dataBits, stopBits, parity);
  }

  public boolean setParams(int baudRate, int dataBits, int stopBits, int parity) throws IOException {
    try {
      log.info("setParams {} {} {} {}", baudRate, dataBits, stopBits, parity);
      if (port == null || !port.isOpen()) {
        log.error("port is null or not opened");
        return false;
      }

      if (port.setParams(baudRate, dataBits, stopBits, parity)) {
        this.rate = baudRate;
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  /*
   * The main simple connect - it attempts to connect to one of the known ports
   * in memory if that fails it will try to connect to a hardware port
   * 
   * TODO - make "connecting" to pre-existing ports re-entrant !!!
   * 
   * connect - optimized to have a SerialDataListener passed in - which will
   * optimize data streaming back from the port.
   * 
   * preference is to have this local optimizaton
   * 
   * connect = open + listen
   * 
   */
  public void open(String inPortName, int rate, int dataBits, int stopBits, int parity) throws IOException {

    info("connect to port %s %d|%d|%d|%d", inPortName, rate, dataBits, stopBits, parity);
    this.rate = rate;
    this.dataBits = dataBits;
    this.stopBits = stopBits;
    this.parity = parity;

    // two possible logics to see if we are connected - look at the
    // state of the port
    // on the static resource - or just check to see if its on the
    // "connectedPort" set

    // #1 check to see if were already connected a port
    // if (this.portName != null && this.portName.equals(portName) &&
    // ports.containsKey(portName)) {
    if (this.portName != null) {
      info("#1 already connected");
      Port port = ports.get(portName);
      if (port.isOpen() && port.isListening() && !portName.equals(inPortName)) {
        info("already connected to %s - disconnect first to reconnect", portName);
        return;
      }
    }

    // #2 connect to a pre-existing
    if (ports.containsKey(inPortName)) {
      info("#2 connect to a pre-existing port");
      connectPort(ports.get(inPortName), null);
      return;
    }

    if (inPortName != null && inPortName.toLowerCase().startsWith("tcp://")) {
      try {
        info("connecting tcp");
        connectTcp(inPortName);
        return;
      } catch (Exception e) {
        // not a big fan of re-throwing exceptions,
        // but I'll make an exception here
        error("connect tcp threw");
        throw new IOException(e);
      }
    }

    // #2.5 - Platform is in virtual mode - create a virtual uart

    if (isVirtual()) {
      connectVirtualUart(inPortName);
      connect(inPortName);
      return;
    }

    // #3 we dont have an existing port - so we'll try a hardware port
    // connect at default parameters - if you need custom parameters
    // create the hardware port first
    info("#3 we don't have an pre-existing port - so we'll try a hardware port");
    Port port = createHardwarePort(inPortName, rate, dataBits, stopBits, parity);
    if (port == null) {
      return;
    }

    connectPort(port, null);

    // even when the JNI says it is connected
    // rarely is everything ready to go
    // give us half a second for all the buffers
    // & hardware to be ready
    // sleep(1500);
  }

  public Port connectPort(Port newPort, SerialDataListener listener) throws IOException {
    port = newPort;
    // portName = port.getName();
    ports.put(port.getName(), port);
    portNames.add(port.getName());
    // invoke("getPortNames"); why ?

    if (listener != null) {
      listeners.put(listener.getName(), listener);
    }
    if (!port.isOpen()) {
      port.open();
    }
    port.listen(listeners);
    connectedPorts.put(port.getName(), newPort);

    // FYI !!!
    // give us a second before we advertise the port
    // is open - often the hardware or JNI buffers
    // are not ready even though we have "opened" it
    // sleep(1000);

    // invoking remote & local onConnect
    invoke("publishConnect", port.getName());
    for (String key : listeners.keySet()) {
      // NOT A GOOD OPTIMIZATION - AS THE "EVENT" IS MUCH MORE IMPORTANT
      // THAN THE SPEED OF THE DATA
      // listeners.get(key).onConnect(portName);
      send(listeners.get(key).getName(), "onConnect", port.getName());
    }

    // we have a portName and we are connected
    portName = port.getName();
    lastPortName = portName;

    // save(); why?
    broadcastState();
    return port;
  }

  public boolean connectTcp(String url) throws IOException {
    Port tcpPort = createTCPPort(url, this);
    connectPort(tcpPort, this);
    return true;
  }

  /*
   * Dynamically create a hardware port - this method is needed to abtract away
   * the specific hardware library. Its advantageous to have abstraction when
   * interfacing with a specific implementation (JNI/JNA - other?). The
   * abstraction allows the service to attempt to choose the correct library
   * depending on platform or personal user choice.
   * 
   * We don't want the whole Serial service to explode because of an Import of
   * an implementation which does not exist on a specific platform. I know this
   * from experience :)
   */

  public Port createHardwarePort(String name, int rate, int dataBits, int stopBits, int parity) {
    log.info("createHardwarePort {} port {} {}|{}|{}|{}", hardwareLibrary, name, rate, dataBits, stopBits, parity);
    try {

      hardwareLibrary = getHardwareLibrary();

      Class<?> c = Class.forName(hardwareLibrary);
      Constructor<?> constructor = c.getConstructor(new Class<?>[] { String.class, int.class, int.class, int.class, int.class });
      Port hardwarePort = (Port) constructor.newInstance(name, rate, dataBits, stopBits, parity);

      info("created  port %s %d|%d|%d|%d - goodtimes", name, rate, dataBits, stopBits, parity);
      ports.put(name, hardwarePort);
      return hardwarePort;

    } catch (Exception e) {
      error(e);
      log.error("connectPort threw", e);
    }

    return null;
  }

  public Port createTCPPort(String url, SerialDataListener listener) throws IOException {
    info("connectTCP %s", url);
    URI uri = null;
    try {
      uri = new URI(url);
    } catch (Exception e) {
      throw new IOException(e);
    }
    String scheme = uri.getScheme();
    if (!scheme.toLowerCase().equals("tcp")) {
      throw new IOException(String.format("tcp:// only supported - requested %s", scheme));
    }
    @SuppressWarnings("resource")
    Socket socket = new Socket(uri.getHost(), uri.getPort());
    // String portName = String.format("%s.%s", getName(),
    // socket.getRemoteSocketAddress().toString());
    Port socketPort = new PortStream(url, socket.getInputStream(), socket.getOutputStream());
    ports.put(portName, socketPort);
    return socketPort;
  }

  public PortQueue createVirtualPort(String name) {
    BlockingQueue<Integer> rx = new LinkedBlockingQueue<Integer>();
    BlockingQueue<Integer> tx = new LinkedBlockingQueue<Integer>();
    PortQueue portQueue = new PortQueue(name, rx, tx);
    ports.put(name, portQueue);
    return portQueue;
  }

  static public Serial connectVirtualUart(String myPort) throws IOException {
    return connectVirtualUart(null, myPort, String.format("%s.UART", myPort));
  }

  static public Serial connectVirtualUart(String myPort, String uartPort) throws IOException {
    return connectVirtualUart(null, myPort, uartPort);
  }

  static public Serial connectVirtualUart(Serial uart, String myPort, String uartPort) throws IOException {

    BlockingQueue<Integer> left = new LinkedBlockingQueue<Integer>();
    BlockingQueue<Integer> right = new LinkedBlockingQueue<Integer>();

    // add our virtual port
    PortQueue vPort = new PortQueue(myPort, left, right);
    Serial.ports.put(myPort, vPort);

    PortQueue uPort = new PortQueue(uartPort, right, left);
    if (uart == null) {
      uart = (Serial) Runtime.start(String.format("%s.UART", myPort.replace("/", "_")), "Serial");
    }
    uart.connectPort(uPort, uart);
    log.info("connectToVirtualUart - creating uart {} <--> {}", myPort, uartPort);
    // returning the serial service that is connected to the DCE side of the
    // virtual port.
    return uart;
  }

  /**
   * disconnect = close + remove listeners all ports on serial network
   */
  public void disconnect() {
    if (portName != null && !connectedPorts.containsKey(portName)) {
      info("disconnect unknown port %s", portName);
      return;
    }

    if (portName == null) {
      info("already disconnected");
      return;
    }

    // remote published disconnect
    invoke("publishDisconnect", port.getName());

    // local disconnect
    for (String key : listeners.keySet()) {
      // DUMB OPTIMIZATION - THE EVENT IS FAR MORE IMPORTANT THAN THE
      // SPEED OF THE DATA
      // listeners.get(key).onDisconnect(portName);
      send(listeners.get(key).getName(), "onDisconnect", port.getName());
    }

    info("disconnecting all ports");
    // forked ports
    for (String portName : connectedPorts.keySet()) {
      Port port = connectedPorts.get(portName);
      port.close();
    }

    connectedPorts.clear();

    portName = null;
    port = null;
    broadcastState();
  }

  static public String getHardwareLibrary() {
    // if user has forced a specific library
    // use it - customer is always right !!!
    if (hardwareLibrary != null) {
      return hardwareLibrary;
    }

    // otherwise make a "best" guess
    Platform platform = Platform.getLocalInstance();
    if (platform.isDalvik()) {
      return HARDWARE_LIBRARY_ANDROID_BLUETOOTH;
    } else {
      return HARDWARE_LIBRARY_JSSC;
    }
  }

  public Map<String, SerialDataListener> getListeners() {
    return listeners;
  }

  public Port getPort() {
    return port;
  }

  /**
   * get the port name this serial service is currently attached to
   */
  public String getPortName() {
    return portName;
  }

  static public List<String> listPorts() {
    // all current ports
    portNames.addAll(ports.keySet());

    // plus hardware ports
    SerialControl portSource = getPortSource();
    if (portSource != null) {
      List<String> osPortNames = portSource.getPortNames();
      for (int i = 0; i < osPortNames.size(); ++i) {
        portNames.add(osPortNames.get(i));
      }
    }
    List<String> ports = new ArrayList<String>(portNames);
    log.info("ports: {}", Arrays.toString(ports.toArray()));
    return ports;
  }

  static public String getPorts() {
    StringBuffer sb = new StringBuffer("[");
    List<String> ports = listPorts();
    for (int i = 0; i < ports.size(); ++i) {
      sb.append("\"" + ports.get(i) + "\"");
      if (i != ports.size() - 1) {
        sb.append(",");
      }
    }
    sb.append("]");
    log.info("ports: {}", sb);
    return sb.toString();
  }

  /**
   * "all" currently known ports - if something is missing refresh ports should
   * be called to force hardware search
   */
  @Override
  public List<String> getPortNames() {
    // refresh(); - endless loop with webgui if placed here
    // original -> return new ArrayList<String>(portNames);

    // all current ports
    portNames.addAll(ports.keySet());

    // plus hardware ports
    SerialControl portSource = getPortSource();
    if (portSource != null) {
      List<String> osPortNames = portSource.getPortNames();
      for (int i = 0; i < osPortNames.size(); ++i) {
        portNames.add(osPortNames.get(i));
      }
    }
    List<String> ports = new ArrayList<String>(portNames);

    invoke("publishPortNames", ports);
    // broadcastState(); // FIXME - REMOVE !!! publishPortNames should be used !
    return ports;
  }

  static SerialControl getPortSource() {
    try {
      hardwareLibrary = getHardwareLibrary();
      log.info("loading class: {}", hardwareLibrary);
      Class<?> c = Class.forName(getHardwareLibrary());
      return (SerialControl) c.newInstance();
    } catch (Exception e) {
      log.error("getPortSource", e);
    }

    return null;
  }

  @Override
  public BlockingQueue<?> getQueue() {
    return blockingRX;
  }

  public int getRXCount() {
    return rxCount;
  }

  public int getTimeout() {
    return timeoutMS;
  }

  public boolean isConnected() {
    // really? shouldn't this be something like...
    // if the port is actually connected?
    return portName != null;
  }

  /**
   * onByte is typically the functions clients of the Serial service use when
   * they want to consume serial data.
   * 
   * The serial service implements this function primarily so it can test itself
   * 
   * readFromPublishedByte is a catch mechanism to verify tests
   * 
   */
  @Override
  public void onBytes(byte[] bytes) {
    if (bytes == null) {
      return;
    }
    if (listeners.size() == 0) {
      log.warn("No Listeners !!!  we are invoking publishBytes.. data is likely getting dropped? ");
    }
    invoke("publishBytes", bytes);
    // String byteIntString = StringUtil.byteArrayToIntString(bytes);
    // log.info("On bytes called len: {} data: {}" , bytes.length,
    // byteIntString);
    for (int i = 0; i < bytes.length; i++) {
      Integer newByte = bytes[i] & 0xff;
      ++rxCount;
      // publish the rx byte !
      invoke("publishRX", newByte);
      if (blockingRX.size() < BUFFER_SIZE) {
        blockingRX.add(newByte);
      }
      try {
        tcpSerialHub.broadcast(newByte);
      } catch (IOException e) {
        log.warn("Error broadcasting to tcp serial hub", e);
      }
      if (recordRx != null) {
        // potentially variety of formats can be supported here
        try {
          recordRx.write(String.format(" %02X", newByte).getBytes());
        } catch (IOException e) {
          log.warn("Error writing to recordRx", e);
        }
      }
    }
  }

  @Override
  public void onConnect(String portName) {
    info("%s connected to %s", getName(), portName);
  }

  @Override
  public void onDisconnect(String portName) {
    info("%s disconnected from %s", getName(), portName);
  }

  /**
   * successful connection event
   * 
   */
  public String publishConnect(String portName) {
    info("%s publishConnect %s", getName(), portName);
    return portName;
  }

  /**
   * disconnect event
   * 
   */
  public String publishDisconnect(String portName) {
    return portName;
  }

  /*
   * event to return list of ports of all ports this serial service can see
   * 
   */
  public List<String> publishPortNames(ArrayList<String> portNames) {
    return portNames;
  }

  /**
   * publish a byte array of data that was read from the serial port.
   * 
   * @param bytes
   *          in
   * @return out
   * 
   */
  public byte[] publishBytes(byte[] bytes) {
    // log.info("Serial Port {} Publish Bytes: {}", getPortName() , bytes);
    return bytes;
  }

  /**
   * Publishing receive data to and end point
   * 
   * @param data
   *          in
   * @return out
   * 
   */
  public int publishRX(Integer data) {
    return data;
  }

  /**
   * Publishing transmit data to a publishing point
   * 
   * @param data
   *          in
   * @return out
   * 
   */
  public Integer publishTX(Integer data) {
    return data;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.myrobotlab.service.SerialDevice#read()
   */
  @Override
  synchronized public int read() throws IOException, InterruptedException {
    if (timeoutMS == null) {
      return blockingRX.take();
    }
    Integer newByte = blockingRX.poll(timeoutMS, TimeUnit.MILLISECONDS);
    if (newByte == null) {
      String error = String.format("%d ms timeout was reached - no data", timeoutMS);
      error(error);
      throw new IOException(error);
    }
    return newByte;
  }

  /**
   * return a byte array represending all the input pending data at the time
   * it's called. If there is no input data, null is returned.
   * 
   * @return byte array
   * @throws IOException
   *           boom
   * @throws InterruptedException
   *           boom
   * 
   */
  synchronized public byte[] readBytes() throws IOException, InterruptedException {
    int size = blockingRX.size();
    if (size == 0) {
      return null;
    } else {
      byte[] data = new byte[size];
      for (int i = 0; i < size; i++) {
        data[i] = blockingRX.take().byteValue();
      }
      return data;
    }
  }

  // FIXME add timeout parameter (with default)
  synchronized public int read(byte[] data) throws IOException, InterruptedException {
    for (int i = 0; i < data.length; ++i) {
      data[i] = (byte) read();
    }
    return data.length;
  }

  /**
   * Read size bytes from the serial port. If a timeout is set it may return
   * less characters as requested. With no timeout it will block until the
   * requested number of bytes is read.
   * 
   * @param length
   *          l
   * @return bytes
   * @throws InterruptedException
   *           e
   * 
   */
  synchronized public byte[] read(int length) throws InterruptedException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    int count = 0;
    Integer newByte = null;
    while (count < length) {
      if (timeoutMS == null) {
        newByte = blockingRX.take();
      } else {
        newByte = blockingRX.poll(timeoutMS, TimeUnit.MILLISECONDS);
      }
      if (newByte == null) {
        if (count == 0) {
          error("got nothing!");
          return null;
        } else {
          error("expecting %d bytes got %d", length, count);
          break;
        }
      }
      bytes.write(newByte.intValue() & 0xff);
      ++count;
    }
    return bytes.toByteArray();
  }

  public int read(int[] data) throws InterruptedException {
    int count = 0;
    Integer newByte = null;
    while (count < data.length) {
      if (timeoutMS == null) {
        newByte = blockingRX.take();
      } else {
        newByte = blockingRX.poll(timeoutMS, TimeUnit.MILLISECONDS);
      }
      if (newByte == null) {
        error("expecting %d bytes got %d", data.length, count);
        return count;
      }
      data[count] = newByte;
      ++count;
    }
    return count;
  }

  public byte[] readLine() throws InterruptedException {
    return readLine('\n');
  }

  public byte[] readLine(char deliminater) throws InterruptedException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    Integer newByte = null;
    while (newByte == null || newByte != deliminater) {
      if (timeoutMS == null) {
        newByte = blockingRX.take();
      } else {
        newByte = blockingRX.poll(timeoutMS, TimeUnit.MILLISECONDS);
      }
      if (newByte == null) {
        info("non blocking got nothing");
        return bytes.toByteArray();
      }
      bytes.write(newByte.intValue() & 0xff);
    }
    return bytes.toByteArray();
  }

  public String readString() throws InterruptedException {
    byte[] bytes = readLine('\n');
    return new String(bytes);
  }

  public String readString(char delimiter) throws InterruptedException {
    byte[] bytes = readLine(delimiter);
    return new String(bytes);
  }

  /**
   * read a string back from the serial port
   * 
   * @param length
   *          - the number of bytes to read back - the amount of time to wait
   *          blocking until we return. 0 ms means the reading thread will
   *          potentially block forever.
   * @return String form of the bytes read
   * @throws InterruptedException
   *           e
   */
  public String readString(int length) throws InterruptedException {
    byte[] bytes = read(length);
    return new String(bytes);
  }

  // FIXME remove blocking public
  // FIXME overload with timeouts etc - remove exposed blocking
  // FIXME - implement
  public byte[] readToDelimiter(String delimeter) {
    return null;
  }

  public void record() throws FileNotFoundException {
    recordRx = new FileOutputStream(String.format("%s.rx.%s", getName(), Serial.format));
    recordTx = new FileOutputStream(String.format("%s.tx.%s", getName(), Serial.format));
  }

  public void setFormat(String format) throws Exception {
    Serial.format = format;
  }

  public List<String> getFormats() {
    return formats;
  }

  public void removeByteListener(SerialDataListener listener) {
    removeByteListener(listener.getName());
  }

  public void removeByteListener(String name) {
    ServiceInterface si = Runtime.getService(name);

    // if (si instanceof SerialDataListener && si.isLocal()){
    if (SerialDataListener.class.isAssignableFrom(si.getClass()) && si.isLocal()) {
      // direct callback
      listeners.remove(si.getName());
    } else {
      // pub sub
      removeListener("publishRX", si.getName(), "onByte");
      removeListener("publishConnect", si.getName(), "onConnect");
      removeListener("publishDisconnect", si.getName(), "onDisconnect");
    }
  }

  public void reset() {
    clear();
    // setTimeout(null);
    rxCount = 0;
    txCount = 0;
  }

  public void setBufferSize(int size) {
    BUFFER_SIZE = size;
  }

  public void setDTR(boolean state) {
    port.setDTR(state);
  }

  public String setHardwareLibrary(String clazz) {
    hardwareLibrary = clazz;
    return hardwareLibrary;
  }

  /**
   * default timeout for all reads 0 = infinity &gt; 0 - will wait for the
   * number in milliseconds if the data has not arrived then an IOError will be
   * thrown
   */
  @Override
  public void setTimeout(int timeout) {
    timeoutMS = timeout;
  }

  public void stopRecording() {
    try {
      if (recordRx != null) {
        recordRx.close();
        recordRx = null;
      }

      if (recordTx != null) {
        recordTx.close();
        recordTx = null;
      }
      broadcastState();
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  @Override
  public void stopService() {
    super.stopService();
    disconnect();
    stopRecording();
  }

  @Override
  public String toString() {
    return String.format("%s->%s", getName(), portName);
  }

  // write(byte[] b) IOException
  /*
   * (non-Javadoc)
   * 
   * @see org.myrobotlab.service.SerialDevice#write(byte[])
   */
  @Override
  public void write(byte[] data) throws Exception {

    if (data == null) {
      return;
    }

    for (String portName : connectedPorts.keySet()) {
      Port writePort = connectedPorts.get(portName);
      // log.info("Writing data to port {} data:{} -- WritePort:{}", portName,
      // data, writePort);
      writePort.write(data);
    }

    // TODO: invoke publishTX with the array?
    for (int i = 0; i < data.length; i++) {
      invoke("publishTX", (int) data[i]);
    }

  }

  public void writeInt(int b) throws Exception {
    write(b);
  }

  // TODO: remove this method use write(int[] b) instead
  synchronized public void write(int b) throws Exception {

    if (connectedPorts.size() == 0) {
      error("can not write to a closed port!");
      return;
    }

    for (String portName : connectedPorts.keySet()) {
      Port writePort = connectedPorts.get(portName);
      writePort.write(b);
    }

    // main line TX
    invoke("publishTX", b);

    ++txCount;
    if (recordTx != null) {
      recordTx.write(String.format(" %02X", b).getBytes());
    }
  }

  // ============= write methods begin ====================
  // write(String data) not in OutputStream
  public void write(String data) throws Exception {
    write(data.getBytes());
  }

  public void writeString(String data) throws Exception {
    write(data.getBytes());
  }

  // FIXME - change Codec based on file extension !!!
  // file (formatter/parser) --to--> tx
  public void writeFile(String filename) {
    try {

      byte[] fileData = FileIO.toByteArray(new File(filename));

      /*
       * TODO - ENCODING !!! if (txCodec != null) { // FIXME parse the incoming
       * file for (int i = 0; i < fileData.length; ++i) { // FIXME - determine
       * what is needed / expected to parse //
       * write(txFormatter.parse(fileData[i])); } } else {
       */
      for (int i = 0; i < fileData.length; ++i) {
        write(fileData[i]);
      }
      // }

    } catch (Exception e) {
      error(e);
    }
  }

  public void connect() throws IOException {
    connect(lastPortName);
  }

  public boolean isRecording() {
    return (recordRx != null) || (recordTx != null);
  }

  public String getLastPortName() {
    return lastPortName;
  }

  @Override
  public void flush() {
  }

  public int getRate() {
    return rate;
  }

  public int getDataBits() {
    return dataBits;
  }

  public int getStopBits() {
    return stopBits;
  }

  public int parity() {
    return parity;
  }

  @Override
  public void connect(String port, int rate) throws Exception {
    connect(port, rate, 8, 1, 0);
  }

  @Override
  public void close() throws IOException {
    info("disconnecting all ports");
    // forked ports
    for (String portName : connectedPorts.keySet()) {
      Port port = connectedPorts.get(portName);
      port.close();
    }
  }

  public void logRecv(Boolean b) {
    if (b) {
      recordRx = System.out;
    } else {
      recordRx = null;
    }
  }

  public void startTcpServer() throws IOException {
    startTcpServer(null); // default port
  }

  public void startTcpServer(Integer port) throws IOException {
    tcpSerialHub.start(port);
    tcpSerialHub.attach(this);
  }

  public void stopTcpServer() throws IOException {
    tcpSerialHub.stop();
  }

  @Override
  public ServiceConfig getConfig() {
    SerialConfig config = new SerialConfig();
    config.port = lastPortName;
    return config;
  }

  public ServiceConfig apply(ServiceConfig c) {
    SerialConfig config = (SerialConfig) c;

    if (config.port != null) {
      try {
        if (isConnected()) {
          connect(config.port);
        }
      } catch (Exception e) {
        log.error("load connecting threw", e);
      }
    }
    return c;
  }

  public static void main(String[] args) {

    LoggingFactory.init(Level.INFO);

    // TODO - test blocking / non blocking / time-out blocking / reading an
    // array (or don't bother?) or do with length? num bytes to block or
    // timeout
    // TODO - if I am connected to a different serial port
    // get that name - disconnect - and then reconnect when done
    // FIXME - very little functionality for a combined tx rx file
    // TODO - test sendFile & record
    // TODO - speed test
    // TODO use utility methods to help parse read data types
    // because we should not assume we know the details of ints longs etc
    // nor
    // the endianess
    // utility methods - ascii
    // FIXME - // test case write(-1) as display becomes -1 ! - file is
    // different than gui !?!?!

    try {

      Platform.setVirtual(true);

      Serial s = (Serial) Runtime.start("s1", "Serial");
      String vport1 = "vport1";
      s.connect(vport1);
      Serial uart = (Serial) Runtime.getService(vport1 + ".UART");

      Runtime.start("webgui", "WebGui");

      Runtime.start("gui", "SwingGui");
      VirtualArduino hub = (VirtualArduino) Runtime.start("varduino", "VirtualArduino");
      // Serial serial = (Serial) Runtime.start("serial", "Serial");
      Serial serial = hub.getSerial();
      serial.startTcpServer();
      // serial.connect("COM88");
      hub.connect("COM888");

      boolean done = true;
      if (done) {
        return;
      }

      List<String> ports = new ArrayList<String>(portNames);
      serial.invoke("publishPortNames", ports);

      Runtime.start("python", "Python");
      Runtime.start("webgui", "WebGui");

      int timeout = 500;// 500 ms serial timeout

      // Runtime.start("gui", "SwingGui");
      // Runtime.start("webgui", "WebGui");

      // get serial handle and creates a uart & virtual null modem cable
      // Serial serial = (Serial) Runtime.start("serial", "Serial");
      serial.setTimeout(timeout);

      String port = "COM15";

      // EASY VIRTUAL SWITCH

      uart.setTimeout(300);
      // ---- Virtual End -----

      serial.open(port);

      // verify the null modem cable is connected
      if (!serial.isConnected()) {
        throw new IOException(String.format("%s not connected", serial.getName()));
      }

      if (!uart.isConnected()) {
        throw new IOException(String.format("%s not connected", uart.getName()));
      }

      // start binary recording
      serial.record();
      uart.record();

      // test blocking on exact size
      serial.write(10);
      serial.write(20);
      serial.write(30);
      serial.write(40);
      serial.write(50);
      serial.write(60);
      serial.write(70);
      uart.write("000D\r");
      // read back
      log.info(serial.readString(5));

      // blocking read with timeout
      String data = "HELLO";
      uart.write(data);
      String hello = serial.readString(data.length());

      if (!data.equals(hello)) {
        throw new IOException("data not equal");
      }

      serial.info("read back [%s]", hello);

      serial.info("array write");
      serial.write(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 127, (byte) 128, (byte) 254, (byte) 255 });
      uart.clear();
      serial.write("this is the end of the line \n");
      serial.clear();
      // TODO: why are we doing this? burn the first line.
      serial.readLine();
      byte[] readBackArray = uart.readLine();
      log.info(Arrays.toString(readBackArray));

      // FIXME !!! - bug - we wrote a big array to serial -
      // then immediately cleared the uart buffer
      // in fact we cleared it so fast - that the serial data ---going
      // to----> uart
      // has not reached uart (because there is some overhead in moving,
      // reading and formatting the incoming data)
      // then we start checking values in "test blocking" this by that
      // time the serial data above has hit the
      // uart

      // with a virtual null modem cable I could "cheat" and flush() could
      // look at the serial's tx buffer size
      // and block until its cleared - but this would not be typical of
      // "real" serial ports
      // but it could stabilize the test

      // in the real world we don't know when the sender to
      // our receiver is done - so we'll sleep here
      sleep(300);
      serial.info("clear buffers");
      serial.clear();
      uart.clear();

      if (serial.available() != 0) {
        throw new IOException("available data after clear");
      }

      // support write(int) kill pill or not ?
      // I say yes
      serial.info("testing blocking");
      for (int i = 255; i > -1; --i) {
        serial.write(i);
        int readBack = uart.read();
        log.info("written {} read back {}", i, readBack);
        if (i < 256 && i > -1) {
          if (readBack != i) {
            throw new IOException(String.format("read back not the same as written for value %d %d !", i, readBack));
          }
        }
      }

      // FIXME - test the -1 write(int) kill pill
      // serial.write(-1) -> should close port !!!

      // in the real world we don't know when the sender to
      // our receiver is done - so we'll sleep here
      sleep(300);
      serial.info("clear buffers");
      serial.clear();
      uart.clear();

      // test publish/subscribe nonblocking
      serial.addByteListener(serial); // <-- FIXME CREATES INFINITE LOOP
      // BUG
      uart.write(64);

      // TODO - low level details of strings & timeouts
      // TODO - filename
      serial.clear();

      // basic record
      String inRecord = "this is a short ascii row\n";
      uart.write(inRecord);
      // String record = serial.readString();

      serial.clear();
      uart.clear();

      serial.stopRecording();
      uart.stopRecording();

      // ======= decimal format begin ===========

      // default non-binary format is ascii decimal

      // uart.record("test/Serial/uart.2");
      serial.write(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, (byte) 255 });
      // we have to pause here momentarily
      // so the data can be written and read from the virtual null modem
      // cable (on different threads)
      // before we close the file streams
      sleep(30);
      // uart.stopRecording();
      serial.stopRecording();
      // ======= decimal format end ===========

      // ======= hex format begin ===========
      // serial.setCodec("hex");
      // serial.record("hex.3");
      // uart.record("test/Serial/uart.3");
      serial.write(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, (byte) 255 });
      sleep(30);
      serial.broadcastState();
      serial.stopRecording();
      // uart.stopRecording();
      // ======= hex format begin ===========

      // parsing of files based on extension check

      // TODO flush & close tests ?
      // serial.disconnect();
      // uart.disconnect();

      // log.info(status.flatten().toString());

    } catch (Exception e) {
      Logging.logError(e);
    }

  }

}
