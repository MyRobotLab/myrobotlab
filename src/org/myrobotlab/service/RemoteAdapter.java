/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceEnvironment;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.CommData;
import org.myrobotlab.net.TCPThread2;
import org.myrobotlab.service.interfaces.CommunicationInterface;
import org.myrobotlab.service.interfaces.Communicator;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;

/***
 * 
 * @author GroG
 * 
 *         This is a service which allows foreign clients to connect
 * 
 */

public class RemoteAdapter extends Service implements Communicator {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(RemoteAdapter.class);

	@Element
	public String lastHost = "127.0.0.1";
	@Element
	public String lastPort = "6767";

	
	// types of listening threads - multiple could be managed
	// when correct interfaces and base classes are done
	transient TCPListener tcpListener = null;
	transient UDPListener udpListener = null;

	private Integer udpPort = 6767;
	private Integer tcpPort = 6767;

	private int udpRx = 0;
	private int udpTx = 0;
	private int tcpTx = 0;

	transient HashMap<URI, TCPThread2> clientList = new HashMap<URI, TCPThread2>();

	public RemoteAdapter(String n) {
		super(n);
	}

	@Override
	public boolean isReady() {
		if (tcpListener.serverSocket != null) {
			return tcpListener.serverSocket.isBound();
		}
		return false;
	}

	class TCPListener extends Thread {
		RemoteAdapter myService = null;
		transient ServerSocket serverSocket = null;
		ObjectOutputStream out;
		ObjectInputStream in;
		int listeningPort;

		public TCPListener(int listeningPort, RemoteAdapter s) {
			super(String.format("%s.tcp.%d", s.getName(), listeningPort));
			this.listeningPort = listeningPort;
			myService = s;
		}

		public void shutdown() {
			if ((serverSocket != null) && (!serverSocket.isClosed())) {
				try {
					serverSocket.close();
				} catch (IOException e) {
					logException(e);
				}
			}
			serverSocket = null;
		}

		public void run() {
			try {

				serverSocket = new ServerSocket(listeningPort, 10);

				log.info(getName() + " TCPListener listening on " + serverSocket.getLocalSocketAddress());
				myService.info(String.format("listening on %s tcp", serverSocket.getLocalSocketAddress()));

				while (isRunning()) {
					// FIXME - on contact register the "environment" regardless
					// if a service registers !!!
					Socket clientSocket = serverSocket.accept(); // FIXME
																	// ENCODER
																	// SHOULD BE
																	// DOING
																	// THIS
					String clientKey = String.format("tcp://%s:%d", clientSocket.getInetAddress().getHostAddress(), clientSocket.getPort());
					// String newHostEntryKey = String.format("mrl://%s/%s",
					// myService.getName(), clientKey);
					// info(String.format("connection from %s",
					// newHostEntryKey));
					URI uri = new URI(clientKey);
					clientList.put(uri, new TCPThread2(myService, uri, clientSocket));
				}

				serverSocket.close();
			} catch (Exception e) {
				logException(e);
			}

		}
	}

	class UDPListener extends Thread {

		DatagramSocket socket = null;
		RemoteAdapter myService = null;
		int listeningPort;
		boolean isRunning = false;

		public UDPListener(int listeningPort, RemoteAdapter s) {
			super(String.format("%s.usp.%d", s.getName(), listeningPort));
			this.listeningPort = listeningPort;
			myService = s;
		}

		public void shutdown() {
			isRunning = false;
			if ((socket != null) && (!socket.isClosed())) {
				socket.close();
			}
		}

		// FIXME FIXME FIXME - large amount of changes to tcp - application
		// logic which handles the "Messaging" should be common to both
		// tcp & udp & xmpp
		public void run() {
			isRunning = true;
			try {
				socket = new DatagramSocket(listeningPort);
				log.info(String.format("%s listening on udp %s:%d", getName(), socket.getLocalAddress(), socket.getLocalPort()));

				byte[] b = new byte[65507]; // max udp size 65507 + 8 byte
											// header = 65535
				ByteArrayInputStream b_in = new ByteArrayInputStream(b);
				DatagramPacket dgram = new DatagramPacket(b, b.length);

				while (isRunning) {
					socket.receive(dgram); // receives all datagrams
					// FIXME - do we need o re-create???
					ObjectInputStream o_in = new ObjectInputStream(b_in);
					try {
						Message msg = (Message) o_in.readObject();
						dgram.setLength(b.length); // must reset length field!
						b_in.reset();
						// FIXME name should be "Runtime" representing the
						// static
						if ("register".equals(msg.method)) {
							// BEGIN ENCAPSULATION --- ENCODER BEGIN
							// -------------
							// IMPORTANT - (should be in Encoder) - create the
							// key
							// for foreign service environment
							// Runtime.addServiceEnvironment(name, protoKey)
							URI protoKey = new URI(String.format("udp://%s:%d", socket.getInetAddress().getHostAddress(), socket.getPort()));
							String mrlURI = String.format("mrl://%s/%s", myService.getName(), protoKey.toString());
							URI uri = new URI(mrlURI);

							// IMPORTANT - this is an optimization and probably
							// should be in the Comm interface defintion
							CommunicationInterface cm = myService.getComm();
							cm.addRemote(uri, protoKey);

							// check if the URI is already defined - if not - we
							// will
							// send back the services which we want to export -
							// Security will filter appropriately
							ServiceEnvironment foreignProcess = Runtime.getServiceEnvironment(uri);

							ServiceInterface si = (ServiceInterface) msg.data[0];
							// HMMM a vote for String vs URI here - since we
							// need to
							// catch syntax !!!
							si.setHost(uri);

							// if security ... msg within msg
							// getOutbox().add(createMessage(Runtime.getInstance().getName(),
							// "register", inboundMsg));
							Runtime.register(si, uri);// <-- not an INVOKE !!!
														// // -
							// no security ! :P

							if (foreignProcess == null) {

								// not defined we will send export
								// TODO - Security filters - default export
								// (include
								// exclude) - mapset of name
								ServiceEnvironment localProcess = Runtime.getLocalServicesForExport();

								Iterator<String> it = localProcess.serviceDirectory.keySet().iterator();
								String name;
								ServiceInterface toRegister;
								while (it.hasNext()) {
									name = it.next();
									toRegister = localProcess.serviceDirectory.get(name);

									// the following will wrap a message within
									// a message and send it remotely
									// This Thread CANNOT Write on The
									// ObjectOutputStream directly -
									// IT SHOULD NEVER DO ANY METHOD WHICH CAN
									// BLOCK !!!! - 3 days of bug chasing when
									// it wrote to ObjectOutputStream and oos
									// blocked when the buffer was full -
									// causing deadlock
									// putting it on the inbox will move it to a
									// different thread
									Message sendService = myService.createMessage("", "register", toRegister);
									Message outbound = myService.createMessage(myService.getName(), "sendRemote", new Object[] { protoKey, sendService });
									myService.getInbox().add(outbound);

								}

							}

							// BEGIN ENCAPSULATION --- ENCODER END -------------
						} else {
							++udpRx;
							myService.getOutbox().add(msg);
						}

					} catch (Exception e) {
						logException(e);
						error("udp datagram dumping bad msg");
					}
					dgram.setLength(b.length); // must reset length field!
					b_in.reset(); // reset so next read is from start of byte[]
									// again
				} // while isRunning

			} catch (Exception e) {
				error("UDPListener could not listen");
				logException(e);
			}
		}
	}

	// FIXME - remove or change to be more general
	public HashMap<URI, CommData> getClients() {
		// ArrayList<U>
		return null;
	}

	@Override
	public void startService() {
		super.startService();
		startListening();
	}

	public void startListening() {
		startListening(udpPort, tcpPort);
	}

	public void startListening(int udpPort, int tcpPort) {
		startUDP(udpPort);
		startTCP(tcpPort);
	}

	public void startUDP(int port) {
		stopUDP();
		udpPort = port;
		udpListener = new UDPListener(udpPort, this);
		udpListener.start();
	}

	public void startTCP(int port) {
		stopTCP();
		tcpPort = port;
		tcpListener = new TCPListener(tcpPort, this);
		tcpListener.start();

	}

	public void stopUDP() {
		if (udpListener != null) {
			udpListener.interrupt();
			udpListener.shutdown();
			udpListener = null;
		}
	}

	public void stopTCP() {
		if (tcpListener != null) {
			tcpListener.interrupt();
			tcpListener.shutdown();
			tcpListener = null;
		}
	}

	public void stopListening() {
		stopUDP();
		stopTCP();
	}

	@Override
	public void stopService() {
		stopListening();
		super.stopService();
	}

	@Override
	public String getDescription() {
		return "allows remote communication between applets, or remote instances of myrobotlab";
	}

	static public ArrayList<InetAddress> getLocalAddresses() {
		ArrayList<InetAddress> ret = new ArrayList<InetAddress>();
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					ret.add(inetAddress);
				}
			}
		} catch (Exception e) {
			logException(e);
		}
		return ret;
	}

	@Override
	synchronized public void sendRemote(URI uri, Message msg) {
		String scheme = uri.getScheme();
		if ("tcp".equals(scheme)) {
			sendRemoteTCP(uri, msg);
		} else if ("udp".equals(scheme)) {
			sendRemoteUDP(uri, msg);
		} else {
			error(String.format("%s not supported", uri.toString()));
		}
	}

	public void sendRemoteTCP(URI uri, Message msg) {
		TCPThread2 t = null;
		try {
			if (clientList.containsKey(uri)) {
				t = clientList.get(uri);
			} else {

				t = new TCPThread2(this, uri, null);
				// clientList.put(new URI(String.format("mrl://%s/%s",
				// getName(), uri.toString())), t);
				clientList.put(uri, t);
			}

			if (t == null) {
				log.info("here");
			}

			t.send(msg);

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	public void sendRemoteUDP(URI uri, Message msg) {
		try {
			
			// FIXME - could use some optimization e.g. .reset()
			ByteArrayOutputStream b_out = new ByteArrayOutputStream();
			ObjectOutputStream o_out = new ObjectOutputStream(b_out);
			o_out.writeObject(msg);
			o_out.flush();
			b_out.flush();
			byte[] b = b_out.toByteArray();
			DatagramPacket dgram = new DatagramPacket(b, b.length);
			
		} catch (Exception e) {
			Logging.logException(e);
		}

	}

	// FIXME - remote
	@Override
	public void addClient(URI uri, Object commData) {
		// TODO Auto-generated method stub
		log.info("add client");
	}

	public Integer getUdpPort() {
		return udpPort;
	}

	public void setUDPPort(Integer udpPort) {
		this.udpPort = udpPort;
	}

	public Integer getTcpPort() {
		return tcpPort;
	}

	public void setTCPPort(Integer tcpPort) {
		this.tcpPort = tcpPort;
	}
	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.ERROR);

		try {

			int i = 1;
			Runtime.main(new String[] { "-runtimeName", String.format("r%d", i) });
			RemoteAdapter remote = (RemoteAdapter) Runtime.start(String.format("remote%d", i), "RemoteAdapter");
			Runtime.createAndStart(String.format("clock%d", i), "Clock");
			Runtime.createAndStart(String.format("gui%d", i), "GUIService");
			remote.stopService();

			// FIXME - sholdn't this be sendRemote ??? or at least
			// in an interface
			// remote.sendRemote(uri, msg);
			// xmpp1.sendMessage("xmpp 2", "robot02 02");
		} catch (Exception e) {
			Logging.logException(e);
		}
	}

}
