package org.myrobotlab.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class LidarVlp16 extends Service {

  private static final long serialVersionUID = 1L;

  int dataPort = 2368;
  int positionPort = 8308;

  public final static Logger log = LoggerFactory.getLogger(LidarVlp16.class);

  transient DatagramSocket dataSocket;
  transient DatagramSocket positionSocket;

  public LidarVlp16(String n) {
    super(n);
  }

  public void listen() throws SocketException, UnknownHostException {
    dataSocket = new DatagramSocket(dataPort, InetAddress.getByName("0.0.0.0"));
    dataSocket.setBroadcast(true); 
    positionSocket = new DatagramSocket(positionPort, InetAddress.getByName("0.0.0.0"));
    positionSocket.setBroadcast(true);
  }

  public void receiveData() throws IOException {    
    byte[] recvBuf = new byte[15000];
    DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
    dataSocket.receive(receivePacket);
  }

  public void receivePosition() throws IOException {
    byte[] recvBuf = new byte[15000];
    DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
    positionSocket.receive(receivePacket);
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

    ServiceType meta = new ServiceType(LidarVlp16.class);
    meta.addDescription("used as a general template");
    meta.setAvailable(true); // false if you do not want it viewable in a gui
    // add dependency if necessary
    // meta.addDependency("org.coolproject", "1.0.0");
    meta.setAvailable(false);
    meta.addCategory("sensor", "lidar");
    return meta;
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Runtime.start("lidar  ", "LidarVlp16");
      Runtime.start("servo", "Servo");
      Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
