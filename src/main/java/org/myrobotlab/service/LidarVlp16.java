package org.myrobotlab.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.LidarConfig;
import org.slf4j.Logger;

public class LidarVlp16 extends Service<LidarConfig> {

  private static final long serialVersionUID = 1L;

  int dataPort = 2368;
  int positionPort = 8308;

  public final static Logger log = LoggerFactory.getLogger(LidarVlp16.class);

  transient DatagramSocket dataSocket;
  transient DatagramSocket positionSocket;

  public LidarVlp16(String n, String id) {
    super(n, id);
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
