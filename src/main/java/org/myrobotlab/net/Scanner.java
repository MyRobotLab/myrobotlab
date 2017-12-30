package org.myrobotlab.net;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.util.Enumeration;
import java.util.List;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class Scanner extends Thread {
	
	public final static Logger log = LoggerFactory.getLogger(Scanner.class);

    boolean isScanning = false;
    Service myService;

    public Scanner(Service service) {
      super(String.format("%s.scanner", service.getName()));
      this.myService = service;
    }

    @Override
    public void run() {
      // Find the server using UDP broadcast
      isScanning = true;
      while (isScanning && myService.isRunning()) {
        try {
          // Open a random port to send the package
          DatagramSocket dsocket = new DatagramSocket();
          dsocket.setBroadcast(true);

          // byte[] sendData =
          // "DISCOVER_FUIFSERVER_REQUEST".getBytes();
          //
          Message msg = Message.createMessage(myService, null, "getConnections", null);
          byte[] msgBuf = org.myrobotlab.codec.CodecUtils.getBytes(msg);

          DatagramPacket sendPacket;
          // Try the 255.255.255.255 first
          /*
           * try { sendPacket = new DatagramPacket(msgBuf, msgBuf.length,
           * InetAddress.getByName("255.255.255.255"), 6767);
           * dsocket.send(sendPacket); myService.info(
           * ">>> Request packet sent to: 255.255.255.255 (DEFAULT)"); } catch
           * (Exception e) { Logging.logException(e); }
           */

          // NEEDED ?? WE ALREADY DID BROADCAST // Broadcast the
          // message over all the network interfaces
          // -------------- BEGIN ---------------------
          Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
          while (interfaces.hasMoreElements()) {
            NetworkInterface ni = interfaces.nextElement();
            // myService.info("examining interface %s %s",
            // ni.getName(), ni.getInetAddresses().toString());
            log.info(String.format("examining interface %s %s", ni.getName(), ni.getDisplayName()));
            // if (ni.isLoopback() || !ni.isUp()) {
            if (!ni.isUp()) {
              log.info(String.format("skipping %s", ni.getDisplayName()));
              continue; // Don't want tobroadcast to the loopback
              // // interface
            }

            for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
              InetAddress broadcast = interfaceAddress.getBroadcast();
              // short x = interfaceAddress.getNetworkPrefixLength();
              log.info("" + interfaceAddress.getAddress());
              if (ni.getName().equals("net4")) {
                log.info("net4");
              }
              if (broadcast == null) {
                continue;
              }

              // Send the broadcast package!
              try {
                log.info(String.format("sending to %s %s %s", ni.getName(), broadcast.getHostAddress(), ni.getDisplayName()));

                if (interfaceAddress.getNetworkPrefixLength() == -1) {
                  log.warn("jdk bug for interface %s network prefix length == -1", interfaceAddress.getAddress().getHostAddress());
                  String pre = interfaceAddress.getAddress().getHostAddress();
                  String b = pre.substring(0, pre.lastIndexOf(".")) + ".255";
                  log.warn("creating new broadcast address of %s", broadcast);
                  broadcast = InetAddress.getByName(b);
                }

                sendPacket = new DatagramPacket(msgBuf, msgBuf.length, broadcast, 6767);
                // sendPacket = new DatagramPacket(msgBuf,
                // msgBuf.length,
                // InetAddress.getByName("192.168.0.255"),
                // 6767);

                dsocket.send(sendPacket);

                myService.info(">>> Request packet sent to: " + broadcast.getHostAddress() + "; Interface: " + ni.getDisplayName());
              } catch (Exception e) {
                myService.error(e);
              }
            }
          }

          // -------------- END ---------------------

          myService.info(">>> Done looping over all network interfaces. Now waiting for a reply!");

          // multiple replies
          boolean listening = true;

          // wait and read replies - put them on the message queue
          // time out and will be done
          try {

            while (listening) {
              // Wait for a response
              byte[] recvBuf = new byte[15000];
              DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
              // how long we will wait for replies
              dsocket.setSoTimeout(2000);
              dsocket.receive(receivePacket);

              // We have a response
              myService.info(String.format("response from : %s", receivePacket.getAddress().getHostAddress()));

              // Check if the message is correct - JSON ?
              ObjectInputStream inBytes = new ObjectInputStream(new ByteArrayInputStream(receivePacket.getData()));
              Message retMsg = (Message) inBytes.readObject();
              myService.info("response from instance %s", retMsg);
              if (!retMsg.method.equals("publishConnection")) {
                myService.error("not an publishConnection message");
                continue;
              } else {
                List<Connection> conns = (List<Connection>) retMsg.data[0];
                for (int i = 0; i < conns.size(); ++i) {
                  myService.invoke("publishConnection", conns.get(i));
                }
              }
              /*
               * String message = new String(receivePacket.getData()).trim(); if
               * (message.equals("DISCOVER_FUIFSERVER_RESPONSE")) { // DO
               * SOMETHING WITH THE SERVER'S IP (for example, store it in //
               * your controller) // Controller_Base
               * .setServerIp(receivePacket.getAddress()); log.info(
               * String.format( "+++++++++++++FOUND MRL INSTANCE++++++++++++ %s"
               * , receivePacket.getAddress())); }
               */
            }

          } catch (SocketTimeoutException se) {
            myService.info("done listening for replies");
          } finally {
            dsocket.close();
          }

        } catch (Exception e) {
          myService.error(e);
        }
      } // while (isScanning)
    }

	public void stopScanning() {
		isScanning = false;
	}
  }