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
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.myrobotlab.framework.Encoder;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceEnvironment;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.Connection;
import org.myrobotlab.net.TCPThread2;
import org.myrobotlab.service.interfaces.CommunicationInterface;
import org.myrobotlab.service.interfaces.Gateway;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

public class RemoteAdapter extends Service implements Gateway {

	// TODO - global address book of discovered connection

	public static class CommOptions implements Serializable {
		private static final long serialVersionUID = 1L;
		Platform platform;
		ArrayList<Gateway> gateways;

		public CommOptions() {
		}
	}

	static public class Scanner extends Thread {

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
			while (isScanning) {
				try {
					// Open a random port to send the package
					DatagramSocket dsocket = new DatagramSocket();
					dsocket.setBroadcast(true);

					// byte[] sendData =
					// "DISCOVER_FUIFSERVER_REQUEST".getBytes();
					//
					Message msg = myService.createMessage("", "getConnections", null);
					byte[] msgBuf = Encoder.getBytes(msg);

					DatagramPacket sendPacket;
					// Try the 255.255.255.255 first
					/*
					 * try { sendPacket = new DatagramPacket(msgBuf,
					 * msgBuf.length, InetAddress.getByName("255.255.255.255"),
					 * 6767); dsocket.send(sendPacket); myService.info(
					 * ">>> Request packet sent to: 255.255.255.255 (DEFAULT)");
					 * } catch (Exception e) { Logging.logException(e); }
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
							short x = interfaceAddress.getNetworkPrefixLength();
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
							if (!retMsg.method.equals("publishNewConnection")) {
								myService.error("not an publishNewConnection message");
								continue;
							} else {
								List<Connection> conns = (List<Connection>) retMsg.data[0];
								for (int i = 0; i < conns.size(); ++i) {
									myService.invoke("publishNewConnection", conns.get(i));
								}
							}
							/*
							 * String message = new
							 * String(receivePacket.getData()).trim(); if
							 * (message.equals("DISCOVER_FUIFSERVER_RESPONSE"))
							 * { // DO SOMETHING WITH THE SERVER'S IP (for
							 * example, store it in // your controller) //
							 * Controller_Base
							 * .setServerIp(receivePacket.getAddress());
							 * log.info( String.format(
							 * "+++++++++++++FOUND MRL INSTANCE++++++++++++ %s",
							 * receivePacket.getAddress())); }
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
			}// while (isScanning)
		}
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

		public void addTCPClient(Socket clientSocket, RemoteAdapter myService) {
			try {
				String clientKey = String.format("tcp://%s:%d", clientSocket.getInetAddress().getHostAddress(), clientSocket.getPort());
				URI uri = new URI(clientKey);
				// HELP PROTOKEY VS MRL KEY ??
				TCPThread2 tcp = new TCPThread2(myService, uri, clientSocket);
				tcpClientList.put(uri, tcp);
				connections.put(uri, tcp.data);
			} catch (Exception e) {
				Logging.logError(e);
			}
		}

		@Override
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

	}

	class UDPListener extends Thread {

		DatagramSocket socket = null;
		RemoteAdapter myService = null;
		int listeningPort;
		boolean isRunning = false;

		public UDPListener(Integer listeningPort, RemoteAdapter s) {
			super(String.format("%s.udp.%d", s.getName(), listeningPort));
			this.listeningPort = listeningPort;
			myService = s;
		}

		// FIXME FIXME FIXME - large amount of changes to tcp - application
		// logic which handles the "Messaging" should be common to both
		// tcp & udp & xmpp
		@Override
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

						// FIXME - sloppy use full method signature
						// FIXME Encoder.makeMethodKey(Msg msg)
						if ("getConnections".equals(msg.method)) {

							// get connections
							List<Connection> conn = getConnections(new URI(String.format("tcp:/%s:%d", dgram.getAddress(), dgram.getPort())));
							// send them back
							for (int i = 0; i < conn.size(); ++i) {
								Message newConnMsg = createMessage("", "publishNewConnection", conn);
								byte[] msgBuf = Encoder.getBytes(newConnMsg);
								DatagramPacket dgp = new DatagramPacket(msgBuf, msgBuf.length, dgram.getAddress(), dgram.getPort());
								socket.send(dgp);
							}

							// we will have to search for them again
						} else if ("publishNewConnection".equals(msg.method)) {
							myService.invoke("onCommOptions", msg.data[0]);
						} else if ("register".equals(msg.method)) {
							// FIXME name should be "Runtime" representing the
							// static
							// BEGIN ENCAPSULATION --- ENCODER BEGIN
							// -------------
							// IMPORTANT - (should be in Encoder) - create the
							// key
							// for foreign service environment
							// Runtime.addServiceEnvironment(name, protocolKey)
							URI protocolKey = new URI(String.format("udp://%s:%d", socket.getInetAddress().getHostAddress(), socket.getPort()));
							String mrl = String.format("mrl://%s/%s", myService.getName(), protocolKey.toString());
							URI mrlURI = new URI(mrl);

							// IMPORTANT - this is an optimization and probably
							// should be in the Comm interface defintion
							CommunicationInterface cm = myService.getComm();
							cm.addRemote(mrlURI, protocolKey);

							// check if the URI is already defined - if not - we
							// will
							// send back the services which we want to export -
							// Security will filter appropriately
							ServiceEnvironment foreignProcess = Runtime.getServiceEnvironment(mrlURI);

							ServiceInterface si = (ServiceInterface) msg.data[0];
							// HMMM a vote for String vs URI here - since we
							// need to
							// catch syntax !!!
							si.setInstanceId(mrlURI);

							// if security ... msg within msg
							// getOutbox().add(createMessage(Runtime.getInstance().getName(),
							// "register", inboundMsg));
							Runtime.register(si, mrlURI);// <-- not an INVOKE
															// !!!
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
									Message outbound = myService.createMessage(myService.getName(), "sendRemote", new Object[] { protocolKey, sendService });
									myService.getInbox().add(outbound);

								}

							}

							// BEGIN ENCAPSULATION --- ENCODER END -------------
						} else {
							// ++udpRx;
							myService.getOutbox().add(msg);
						}

					} catch (Exception e) {
						error(e);
					}
					dgram.setLength(b.length); // must reset length field!
					b_in.reset(); // reset so next read is from start of byte[]
									// again
				} // while isRunning

			} catch (SocketException se) {
				error("UDPListener could not listen %s", se.getMessage());
				Logging.logError(se);
			} catch (Exception e) {
				error("wtf error");
				logException(e);
			}
		}

		public void shutdown() {
			isRunning = false;
			if ((socket != null) && (!socket.isClosed())) {
				socket.close();
			}
		}
	}

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(RemoteAdapter.class);

	public String lastProtocolKey;
	private String defaultPrefix = null;

	private HashMap<String, String> prefixMap = new HashMap<String, String>();
	private HashSet<URI> localProtocolKeys = new HashSet<URI>();

	// types of listening threads - multiple could be managed
	// when correct interfaces and base classes are done
	transient TCPListener tcpListener = null;
	transient UDPListener udpListener = null;

	private Integer udpPort;

	private Integer tcpPort;
	boolean isListening = false;

	boolean isScanning = false;

	// TODO - multiple scanners for parallel port/broadcast scanning
	transient Scanner scanner;

	transient private HashMap<URI, TCPThread2> tcpClientList = new HashMap<URI, TCPThread2>();

	/**
	 * used as a data interface to all the non-serializable network objects - it
	 * will report stats and states
	 */
	private HashMap<URI, Connection> connections = new HashMap<URI, Connection>();

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

	public RemoteAdapter(String n) {
		super(n);
		defaultPrefix = String.format("%s.", n);
		// addLocalTask(5 * 1000, "broadcastHeartbeat");
	}

	@Override
	public void addConnectionListener(String name) {
		addListener("publishNewConnection", name, "onNewConnection", Connection.class);
	}

	// FIXME - add to Gateway interfaceS
	public HashMap<URI, Connection> broadcastHeartbeat() {
		for (Map.Entry<URI, Connection> entry : connections.entrySet()) {
			URI uri = entry.getKey();
			Connection value = entry.getValue();

			// roll through send a set of transactions off & start a
			// IOCompletion like
			// array of status ...
			// if timeout is reached - write the rest with timeout (those that
			// did not
			// get an asynch response

			if (uri.getScheme().equals("tcp")) {
				TCPThread2 tcp = tcpClientList.get(uri);
				log.info("" + tcp);
				// check socket connectivity
				// attempt to re-connect if disconnected
				broadcastState();
			}
		}
		return connections;
	}

	@Override
	// TODO refactor with boolean - lower level error(problem) to put into
	// framework
	public void connect(String uri) throws URISyntaxException {
		Message msg = createMessage("", "register", null);
		sendRemote(uri, msg);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "connectivity", "framework" };
	}

	@Override
	public HashMap<URI, Connection> getClients() {
		return connections;
	}

	/**
	 * important initial communication function related to discovery a broadcast
	 * goes out and replies must include details of communication so that a
	 * viable connection can be created
	 * 
	 * @param client
	 */
	// global access keys (all gateways) - or just this gateway ???
	// if it's all then this could be a function of runtime which is probably
	// the best
	@Override
	public List<Connection> getConnections(URI clientKey) {
		ArrayList<Connection> conns = new ArrayList<Connection>();

		try {

			// FIXME - dorky - probably fix with template method
			// FIXME - do "global" next
			ArrayList<ServiceInterface> services = Runtime.getServicesFromInterface(Gateway.class);
			// ArrayList<Gateway> gateways = new ArrayList<Gateway>();

			// if GLOBAL
			for (int i = 0; i < services.size(); ++i) {
				// Gateway
				// gateways.add((Gateway) services.get(i));
			}

			// else LOCAL
			// add (this) services connections
			ArrayList<String> addr = Runtime.getLocalAddresses();
			for (int i = 0; i < addr.size(); ++i) {
				Connection tcpConn = new Connection();
				// theoretically you could advertise udp too (and others)
				// tcpConn.protocolKey = new
				// URI(String.format("mrl://%s/tcp://%s:%d", getName(),
				// addr.get(i), getTcpPort()));
				tcpConn.protocolKey = new URI(String.format("tcp://%s:%d", addr.get(i), getTcpPort()));
				// we dont fill in our own name
				// FIXME FIXME FIXME - DO THE CORRECT WAY !!!
				// / tcpConn.protocolKey = new URI(String.format("tcp://%s:%d",
				// addr.get(i), getTcpPort()));
				// tcpKey.prefix = suggestion
				// tcpKey.prefix = prefix;
				tcpConn.platform = Runtime.getInstance().getPlatform();
				tcpConn.prefix = Runtime.getInstance().getName();// calls
																	// getPrefix
																	// under
																	// hood
				conns.add(tcpConn);
			}

			// ??

			// tcpKey.uri =
		} catch (Exception e) {
			Logging.logError(e);
		}

		return conns;

	}

	@Override
	public String getDescription() {
		return "allows remote communication between applets, or remote instances of myrobotlab";
	}

	// @Override needs to be overriden - Gateway need implementation
	public Platform getPlatform() {
		return Runtime.getInstance().getPlatform();
	}

	//
	@Override
	public String getPrefix(URI protocolKey) {
		if (defaultPrefix != null) {
			return defaultPrefix;
		} else {
			return "";// important - return "" not null
		}
	}

	public Integer getTcpPort() {
		return tcpPort;
	}

	public Integer getUdpPort() {
		return udpPort;
	}

	public boolean isListening() {
		return isListening;
	}

	@Override
	public boolean isReady() {
		if (tcpListener.serverSocket != null) {
			return tcpListener.serverSocket.isBound();
		}
		return false;
	}

	public boolean isScanning() {
		return isScanning;
	}

	public Connection onHeartbeat(Connection data) {
		return data;
	}

	/**
	 * NOT USED - just left as an example of a consumer asynchronous return of
	 * access key request
	 * 
	 * @param keys
	 * @return
	 */
	public Connection onNewConnection(Connection conn) {
		return conn;
	}

	// publishing point
	@Override
	public Connection publishNewConnection(Connection conn) {
		if (!connections.containsKey(conn.protocolKey)) {
			// uri will now become my uri
			connections.put(conn.protocolKey, conn);
			broadcastState();
		} else {
			info("%d scanning no new connections", System.currentTimeMillis());
		}
		return conn;
	}

	public void scan() {
		if (scanner != null) {
			stopScanning();
		}
		scanner = new Scanner(this);
		scanner.start();
		isScanning = true;
	}

	@Override
	public void sendRemote(String uri, Message msg) throws URISyntaxException {
		sendRemote(new URI(uri), msg);
	}

	@Override
	synchronized public void sendRemote(URI uri, Message msg) {
		String scheme = uri.getScheme();
		lastProtocolKey = uri.toString();
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

				// constructor will throw if can not connect -> new Socket(host,
				// port)
				tcp = new TCPThread2(this, uri, null);
				tcpClientList.put(uri, tcp);
				connections.put(uri, tcp.data);
				broadcastState();
			}

			tcp.send(msg);
			connections.get(uri).tx++;

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public void sendRemoteUDP(URI uri, Message msg) {
		try {

			// FIXME - could use some optimization e.g. .reset()
			DatagramSocket s = new DatagramSocket();
			ByteArrayOutputStream b_out = new ByteArrayOutputStream();
			ObjectOutputStream o_out = new ObjectOutputStream(b_out);
			o_out.writeObject(msg);
			o_out.flush();
			b_out.flush();
			byte[] b = b_out.toByteArray();
			InetAddress hostAddress = InetAddress.getByName(uri.getHost());
			DatagramPacket dgram = new DatagramPacket(b, b.length, hostAddress, uri.getPort());
			s.send(dgram);
			// dgram.se
			// TODO - send the damn packet???
		} catch (Exception e) {
			Logging.logError(e);
		}

	}

	public String setDefaultPrefix(String prefix) {
		defaultPrefix = prefix;
		return prefix;
	}

	public void setPrefix(String source, String prefix) {
		prefixMap.put(source, prefix);
	}

	public void setTCPPort(Integer tcpPort) {
		this.tcpPort = tcpPort;
	}

	public void setUDPPort(Integer udpPort) {
		this.udpPort = udpPort;
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

	public void startListening(int ports) {
		startListening(ports, ports);
	}

	public void startListening(int udpPort, int tcpPort) {
		startUDP(udpPort);
		startTCP(tcpPort);
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

	public void startUDP(Integer port) {
		stopUDP();
		if (port == null) {
			port = 6767;
		}
		udpPort = port;
		udpListener = new UDPListener(udpPort, this);
		udpListener.start();
	}

	public void stopListening() {
		stopUDP();
		stopTCP();
		isListening = false;
		broadcastState();
	}

	public void stopScanning() {
		scanner.isScanning = false;
		isScanning = false;
		scanner = null;
	}

	@Override
	public void stopService() {
		stopListening();
		super.stopService();
	}

	public void stopTCP() {
		if (tcpListener != null) {
			tcpListener.interrupt();
			tcpListener.shutdown();
			tcpListener = null;
		}
	}

	public void stopUDP() {
		if (udpListener != null) {
			udpListener.interrupt();
			udpListener.shutdown();
			udpListener = null;
		}
	}

	@Override
	public Status test() {
		Status status = super.test();
		try {

			RemoteAdapter remote01 = (RemoteAdapter) Runtime.getService(getName());
			remote01.startListening();

			RemoteAdapter remote02 = (RemoteAdapter) Runtime.start("remote02", "RemoteAdapter");
			remote02.startListening(6868);
			remote02.connect("tcp://localhost:6767");

			// bounds cases

			// how to do out of process .. or is it necessary - no

		} catch (Exception e) {
			status.addError(e);
		}

		return status;
	}
	

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		try {

			int i = 0;

			// RemoteAdapter remote0 = (RemoteAdapter)
			// Runtime.start(String.format("remote%d", 0), "RemoteAdapter");
			RemoteAdapter remote1 = (RemoteAdapter) Runtime.start(String.format("remote%d", i), "RemoteAdapter");
			remote1.setTCPPort(6868);
			remote1.setUDPPort(6868);
			remote1.startListening();
			Runtime.start(String.format("gui%d", i), "GUIService");
			// remote1.startListening(6666, 6666);
			// remote1.startListening();
			// remote0.startUDP(6767);
			// remote1.startListening();

			// remote1.startUDP(6767);
			// remote1.scan();
			/*
			 * Runtime.main(new String[] { "-runtimeName", String.format("r%d",
			 * i) }); RemoteAdapter remote = (RemoteAdapter)
			 * Runtime.start(String.format("remote%d", i), "RemoteAdapter");
			 * Runtime.start(String.format("clock%d", i), "Clock");
			 * Runtime.start(String.format("gui%d", i), "GUIService");
			 * 
			 * // Security security = //
			 * (Security)Runtime.start(String.format("security", i), //
			 * "Security"); remote.startListening(); //
			 * security.allowExportByName("laptop", true); //
			 * security.allowExportByName("laptop.gui", false); //
			 * remote.connect("tcp://192.168.0.92:6767"); //
			 * Runtime.start(String.format("joystick%d", i), "Joystick"); //
			 * Runtime.start(String.format("python%d", i), "Python");
			 */
			// what if null service is passed "register()" no parameters -
			// I'm sending a registration of nothing?
			// remote.broadcastState();

			// remote.connect("tcp://127.0.0.1:6767");
			/*
			 * THIS WORKS Message msg = remote.createMessage("", "register",
			 * remote); remote.sendRemote("tcp://127.0.0.1:6868", msg);
			 */

			// FIXME - sholdn't this be sendRemote ??? or at least
			// in an interface
			// remote.sendRemote(uri, msg);
			// xmpp1.sendMessage("xmpp 2", "robot02 02");
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

}