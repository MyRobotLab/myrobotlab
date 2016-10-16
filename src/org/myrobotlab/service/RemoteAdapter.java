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
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
//import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.atmosphere.wasync.Client;
import org.atmosphere.wasync.ClientFactory;
import org.atmosphere.wasync.Decoder;
import org.atmosphere.wasync.Encoder;
import org.atmosphere.wasync.Event;
import org.atmosphere.wasync.Function;
import org.atmosphere.wasync.Request;
import org.atmosphere.wasync.RequestBuilder;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceEnvironment;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.Connection;
import org.myrobotlab.net.Scanner;
import org.myrobotlab.net.TCPThread2;
import org.myrobotlab.service.interfaces.CommunicationInterface;
import org.myrobotlab.service.interfaces.Gateway;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

/**
 * 
 * RemoteAdapter - Multi-node / distributed myrobotlab support.
 * 
 * A RemoteAdapter allows other instances of MyRobotLab to connect. Services and
 * resources can be shared by 2 or more joined instances. The default
 * communication listener is a UDP server listening on all addresses on port
 * 6767.
 * 
 * 
 */
public class RemoteAdapter extends Service implements Gateway {

	// TODO - global address book of discovered connection

	public static class CommOptions implements Serializable {
		private static final long serialVersionUID = 1L;
		Platform platform;
		ArrayList<Gateway> gateways;

		public CommOptions() {
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

		public void addTCPClient(java.net.Socket clientSocket, RemoteAdapter myService) {
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
					java.net.Socket clientSocket = serverSocket.accept();
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
								byte[] msgBuf = org.myrobotlab.codec.CodecUtils.getBytes(newConnMsg);
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
							ServiceEnvironment foreignProcess = Runtime.getEnvironment(mrlURI);

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
	// private HashSet<URI> localProtocolKeys = new HashSet<URI>();

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

	boolean listenOnStartup = false;

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
		addListener("publishNewConnection", name, "onNewConnection");
	}

	// FIXME - add to Gateway interfaceS
	public HashMap<URI, Connection> broadcastHeartbeat() {
		for (Map.Entry<URI, Connection> entry : connections.entrySet()) {
			URI uri = entry.getKey();
			// Connection value = entry.getValue();

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
			List<String> addr = Runtime.getLocalAddresses();
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

	// @Override needs to be overriden - Gateway need implementation
	public Platform getPlatform() {
		return Platform.getLocalInstance();
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
			// close the datagram to avoid resource leaks
			s.close();
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
		// scanner.isScanning = false;
		if (scanner != null) {
			scanner.stopScanning();
		}
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

	public void startService() {
		super.startService();
		if (listenOnStartup) {
			startListening();
		}
	}

	public void listenOnStartup(boolean b) {
		listenOnStartup = b;
	}

	public void websocket(String url) throws IOException {
		Client client = ClientFactory.getDefault().newClient();

		// "http://async-io.org"
		// http://localhost:8888/api/messages

		RequestBuilder request = client.newRequestBuilder().method(Request.METHOD.GET).uri(url).encoder(new Encoder<String, Reader>() { // Stream
																																		// the
																																		// request
																																		// body
			@Override
			public Reader encode(String s) {
				return new StringReader(s);
			}
		}).decoder(new Decoder<String, Reader>() {
			@Override
			public Reader decode(Event type, String s) {
				return new StringReader(s);
			}
		}).transport(Request.TRANSPORT.WEBSOCKET) // Try WebSocket
				.transport(Request.TRANSPORT.LONG_POLLING); // Fallback to
															// Long-Polling

		org.atmosphere.wasync.Socket socket = client.create();
		socket.on(new Function<Reader>() {
			@Override
			public void on(Reader r) {
				// Read the response
			}
		}).on(new Function<IOException>() {

			@Override
			public void on(IOException arg0) {
				// TODO Auto-generated method stub

			}

		}).open(request.build()).fire("/api/").fire("bong");
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

		ServiceType meta = new ServiceType(RemoteAdapter.class.getCanonicalName());
		meta.addDescription("allows remote communication between applets, or remote instances of myrobotlab");
		meta.addCategory("connectivity", "network", "framework");
		meta.addDependency("org.atmosphere.nettosphere", "2.3.0");

		return meta;
	}

	public static void main(String[] args) {
		LoggingFactory.init(Level.INFO);

		try {

			RemoteAdapter remote = (RemoteAdapter) Runtime.start("remote", "RemoteAdapter");
			// remote.connect("tcp://demo.myrobotlab.org:6767");
			// remote.websocket("http://demo.myrobotlab.org:8888/api/messages");
			Runtime.start("gui", "GUIService");
			remote.startListening();

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

}