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
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
import org.myrobotlab.service.interfaces.Gateway;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;

public class RemoteAdapter extends Service implements Gateway {

	// WARNING ALL NON-TRANSIENT MEMBERS BETTER BE SERIALIZABLE !!!!

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(RemoteAdapter.class);

	@Element(required = false)
	public String lastProtoKey;
	
	private String defaultPrefix = null; 
	private HashMap<String,String> prefixMap = new HashMap<String,String>();

	// types of listening threads - multiple could be managed
	// when correct interfaces and base classes are done
	transient TCPListener tcpListener = null;
	transient UDPListener udpListener = null;

	@Element(required = false)
	private Integer udpPort;
	@Element(required = false)
	private Integer tcpPort;

	boolean isListening = false;

	transient private HashMap<URI, TCPThread2> tcpClientList = new HashMap<URI, TCPThread2>();
	/**
	 * used as a data interface to all the non-serializable network objects - it will
	 * report stats and states
	 */
	private HashMap<URI, CommData> clientList = new HashMap<URI, CommData>();

	public RemoteAdapter(String n) {
		super(n);
		defaultPrefix = n;
		addLocalTask(5 * 1000, "broadcastHeartbeat");
	}

	public boolean isListening() {
		return isListening;
	}

	public String setDefaultPrefix(String prefix){
		defaultPrefix = prefix;
		return prefix;
	}
	
	public void setPrefix(String source, String prefix){
		prefixMap.put(source, prefix);
	}
	
	@Override
	public boolean isReady() {
		if (tcpListener.serverSocket != null) {
			return tcpListener.serverSocket.isBound();
		}
		return false;
	}
	
	// FIXME  - add to Gateway interfaceS
	public HashMap<URI, CommData> broadcastHeartbeat(){
		for (Map.Entry<URI, CommData> entry : clientList.entrySet()) {
		    URI uri = entry.getKey();
		    CommData value = entry.getValue();
		    
		    // roll through send a set of transactions off & start a IOCompletion like
		    // array of status ...
		    // if timeout is reached - write the rest with timeout (those that did not 
		    // get an asynch response
		    
		    if (uri.getScheme().equals("tcp")){
		    	TCPThread2 tcp = tcpClientList.get(uri);
		    	log.info("" + tcp);
		    	// check socket connectivity
		    	// attempt to re-connect if disconnected
		    	broadcastState();
		    }
		}
		return clientList;
	}
	
	public CommData onHeartbeat(CommData data){
		return data;
	}

	class TCPListener extends Thread {
		int rxCount = 0;
		int txCount = 0;
		RemoteAdapter myService = null;
		transient ServerSocket serverSocket = null;
		ObjectOutputStream out;
		ObjectInputStream in;
		int remotePort;

		public TCPListener(int remotePort, RemoteAdapter s) {
			super(String.format("%s.tcp.%d", s.getName(), remotePort));
			this.remotePort = remotePort;
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

				serverSocket = new ServerSocket(remotePort, 10);

				log.info(getName() + " TCPListener listening on " + serverSocket.getLocalSocketAddress());
				myService.info(String.format("listening on %s tcp", serverSocket.getLocalSocketAddress()));

				while (isRunning()) {
					// FIXME - on contact register the "environment" regardless
					// if a service registers !!!
					Socket clientSocket = serverSocket.accept();
					// inbound connection FIXME - all keys constructed in
					// Encoder
					addTCPClient(clientSocket, myService);
					broadcastState();
				}

				serverSocket.close();
			} catch (Exception e) {
				logException(e);
			}
		}

		public void addTCPClient(Socket clientSocket, RemoteAdapter myService) {
			try {
				String clientKey = String.format("tcp://%s:%d", clientSocket.getInetAddress().getHostAddress(), clientSocket.getPort());
				URI uri = new URI(clientKey);
				// HELP PROTOKEY VS MRL KEY ??
				TCPThread2 tcp = new TCPThread2(myService, uri, clientSocket);
				tcpClientList.put(uri, tcp);
				clientList.put(uri, tcp.data);
			} catch (Exception e) {
				Logging.logException(e);
			}
		}

	}

	class UDPListener extends Thread {
		

		DatagramSocket socket = null;
		RemoteAdapter myService = null;
		int listeningPort;
		boolean isRunning = false;

		public UDPListener(Integer listeningPort, RemoteAdapter s) {
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
							// ++udpRx;
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

			} catch (SocketException se) {
				log.warn("socket exception - possible close");
			} catch (Exception e) {
				error("UDPListener could not listen");
				logException(e);
			}
		}
	}

	public HashMap<URI, CommData> getClients() {
		return clientList;
	}

	public void startListening() {
		if (udpPort == null) {
			udpPort = 6767;
		}
		if (tcpPort == null) {
			tcpPort = 6767;
		}
		startListening(udpPort, tcpPort);
		isListening = true;
		broadcastState();
	}

	public void startListening(int udpPort, int tcpPort) {
		startUDP(udpPort);
		startTCP(tcpPort);
	}

	public void startUDP(Integer port) {
		stopUDP();
		if (port == null) {
			port = 6767;
		}
		udpPort = port;
		udpListener = new UDPListener(udpPort, this);
		udpListener.start();
	}

	public void startTCP(Integer port) {
		stopTCP();
		if (port == null) {
			port = 6767;
		}
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
		isListening = false;
		broadcastState();
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
	synchronized public void sendRemote(String uri, Message msg) throws URISyntaxException {
		sendRemote(new URI(uri), msg);
	}

	@Override
	synchronized public void sendRemote(URI uri, Message msg) {
		String scheme = uri.getScheme();
		lastProtoKey = uri.toString();
		if ("tcp".equals(scheme)) {
			sendRemoteTCP(uri, msg);
		} else if ("udp".equals(scheme)) {
			sendRemoteUDP(uri, msg);
		} else {
			error(String.format("%s not supported", uri.toString()));
			return;
		}

	}

	public void sendRemoteTCP(URI uri, Message msg) {
		TCPThread2 tcp = null;
		try {
			if (tcpClientList.containsKey(uri)) {
				tcp = tcpClientList.get(uri);
			} else {

				// constructor will throw if can not connect -> new Socket(host, port)
				tcp = new TCPThread2(this, uri, null);
				tcpClientList.put(uri, tcp);
				clientList.put(uri, tcp.data);
				broadcastState();
			}

			tcp.send(msg);
			clientList.get(uri).tx++;

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
			// TODO - send the damn packet???
		} catch (Exception e) {
			Logging.logException(e);
		}

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

	@Override
	public void connect(String uri) throws URISyntaxException {
		Message msg = createMessage("", "register", null);
		sendRemote(uri, msg);
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		try {

			int i = 0;
			
			Runtime.main(new String[] { "-runtimeName", String.format("r%d", i) });
			RemoteAdapter remote = (RemoteAdapter) Runtime.start(String.format("remote%d", i), "RemoteAdapter");
			//Runtime.start(String.format("clock%d", i), "Clock");
			Runtime.start(String.format("gui%d", i), "GUIService");
			//Security security = (Security)Runtime.start(String.format("security", i), "Security");
			remote.startListening();
			//security.allowExportByName("laptop", true);
			//security.allowExportByName("laptop.gui", false);
			//remote.connect("tcp://192.168.0.92:6767");
			//Runtime.start(String.format("joystick%d", i), "Joystick");
			//Runtime.start(String.format("python%d", i), "Python");

			// what if null service is passed "register()" no parameters -
			// I'm sending a registration of nothing?
			//remote.broadcastState();

			//remote.connect("tcp://127.0.0.1:6767");
			/*
			 * THIS WORKS Message msg = remote.createMessage("", "register",
			 * remote); remote.sendRemote("tcp://127.0.0.1:6868", msg);
			 */

			// FIXME - sholdn't this be sendRemote ??? or at least
			// in an interface
			// remote.sendRemote(uri, msg);
			// xmpp1.sendMessage("xmpp 2", "robot02 02");
		} catch (Exception e) {
			Logging.logException(e);
		}
	}

}