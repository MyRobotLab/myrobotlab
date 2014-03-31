package org.myrobotlab.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;

import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class DiscoveryThread implements Runnable {

	public final static Logger log = LoggerFactory.getLogger(DiscoveryThread.class);

	DatagramSocket socket;

	@Override
	public void run() {
		try {
			// Keep a socket open to listen to all the UDP trafic that is
			// destined for this port
			socket = new DatagramSocket(8888, InetAddress.getByName("0.0.0.0"));
			socket.setBroadcast(true);

			while (true) {
				log.info("server >>>Ready to receive broadcast packets! on 0.0.0.0:8888");

				// Receive a packet
				byte[] recvBuf = new byte[15000];
				DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
				socket.receive(packet);

				// Packet received
				log.info("server >>>Discovery packet received from: " + packet.getAddress().getHostAddress());
				log.info("server >>>Packet received; data: " + new String(packet.getData()));

				// See if the packet holds the right command (message)
				String message = new String(packet.getData()).trim();
				if (message.equals("DISCOVER_FUIFSERVER_REQUEST")) {
					byte[] sendData = "DISCOVER_FUIFSERVER_RESPONSE".getBytes();

					// Send a response
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
					socket.send(sendPacket);

					log.info("server >>>Sent packet to: " + sendPacket.getAddress().getHostAddress());
				}
			}
		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	public static DiscoveryThread getInstance() {
		return DiscoveryThreadHolder.INSTANCE;
	}

	private static class DiscoveryThreadHolder {

		private static final DiscoveryThread INSTANCE = new DiscoveryThread();
	}

	public ArrayList<InetAddress> ping() {
		ArrayList<InetAddress> ret = new ArrayList<InetAddress>();
		// Find the server using UDP broadcast
		try {
			// Open a random port to send the package
			DatagramSocket c = new DatagramSocket();
			c.setBroadcast(true);

			byte[] sendData = "DISCOVER_FUIFSERVER_REQUEST".getBytes();

			// Try the 255.255.255.255 first
			try {
				log.info("client >>> sending request packet to: 255.255.255.255:8888 (DEFAULT)");
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), 8888);
				c.send(sendPacket);
				byte[] recvBuf = new byte[15000];
				DatagramPacket pong = new DatagramPacket(recvBuf, recvBuf.length);
				c.receive(pong);
				log.info("client >>> recieved PONG");
			} catch (Exception e) {
			}

			// Broadcast the message over all the network interfaces
			Enumeration interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface networkInterface = (NetworkInterface) interfaces.nextElement();

				if (networkInterface.isLoopback() || !networkInterface.isUp()) {
					continue; // Don't want to broadcast to the loopback
								// interface
				}

				for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
					InetAddress broadcast = interfaceAddress.getBroadcast();
					if (broadcast == null) {
						continue;
					}

					// Send the broadcast package!
					try {
						log.info("client >>> sending request packet to: 255.255.255.255:8888 (DEFAULT)");
						DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, 8888);
						c.send(sendPacket);
					} catch (Exception e) {
					}

					log.info("client >>> Request packet sent to: " + broadcast.getHostAddress() + "; Interface: " + networkInterface.getDisplayName());
				}
			}

			log.info(">>> Done looping over all network interfaces. Now waiting for a reply!");

			// Wait for a response
			byte[] recvBuf = new byte[15000];
			DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
			c.receive(receivePacket);

			// We have a response
			log.info(">>> Broadcast response from server: " + receivePacket.getAddress().getHostAddress());

			// Check if the message is correct
			String message = new String(receivePacket.getData()).trim();
			if (message.equals("DISCOVER_FUIFSERVER_RESPONSE")) {
				// DO SOMETHING WITH THE SERVER'S IP (for example, store it in
				// your controller)
				//Controller_Base.setServerIp(receivePacket.getAddress());
				ret.add(receivePacket.getAddress());
			}

			// Close the port!
			c.close();
		} catch (IOException e) {
			Logging.logException(e);
		}
		
		return ret;
	}
	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);
		
		DiscoveryThread service = DiscoveryThread.getInstance();
		// refactor - this is stupid
		Thread discovery = new Thread(service);
		discovery.start();
		ArrayList<InetAddress> clients = service.ping();
		for (int i = 0; i < clients.size(); ++i){
			log.info(String.format("client %s", clients.get(i).toString()));
		}

	}

}