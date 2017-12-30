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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.atmosphere.wasync.Client;
import org.atmosphere.wasync.ClientFactory;
import org.atmosphere.wasync.Decoder;
import org.atmosphere.wasync.Encoder;
import org.atmosphere.wasync.Event;
import org.atmosphere.wasync.Function;
import org.atmosphere.wasync.Request;
import org.atmosphere.wasync.RequestBuilder;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.Connection;
import org.myrobotlab.net.Scanner;
import org.myrobotlab.net.TcpServer;
import org.myrobotlab.net.UdpServer;
import org.myrobotlab.service.interfaces.Gateway;
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

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(RemoteAdapter.class);

	public String lastProtocolKey;
	private String defaultPrefix = null;

	private HashMap<String, String> prefixMap = new HashMap<String, String>();

	// FIXME - needs to be self contained !! - to have multiple servers
	// FIXME !!! - Server interface send - onMsg onConnect onDisconnect(nop for
	// udp) || websockets
	transient TcpServer tcpServer = null;
	transient UdpServer udpServer = null;

	private Integer udpPort;
	private Integer tcpPort;

	boolean isListening = false;
	boolean isScanning = false;

	/**
	 * scanners to scan for other mrl instances TODO - multiple scanners for
	 * parallel port/broadcast scanning
	 */
	transient Scanner scanner;

	/**
	 * used as a data interface to all the non-serializable network objects - it
	 * will report stats and states
	 */
	public HashMap<URI, Connection> connections = new HashMap<URI, Connection>();

	public RemoteAdapter(String n) {
		super(n);
		defaultPrefix = String.format("%s.", n);
		tcpServer = new TcpServer(this);
		udpServer = new UdpServer(this);
		// addLocalTask(5 * 1000, "broadcastHeartbeat");
	}

	@Override
	public void addConnectionListener(String name) {
		addListener("publishConnection", name, "onNewConnection");
	}

	@Override
	// TODO refactor with boolean - lower level error(problem) to put into
	// framework
	/**
	 * connects and sends register message to remote system
	 * connection depends on url schema
	 */
	public void connect(String uri) throws URISyntaxException {
		log.info("{}.connecting {}", getName(), uri);
		Message msg = Message.createMessage(this, null, "register", null);
		sendRemote(uri, msg);
	}

	@Override
	public HashMap<URI, Connection> getClients() {
		return connections;
	}

	/*
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
		return tcpServer.isReady();
	}

	public boolean isScanning() {
		return isScanning;
	}

	public Connection onHeartbeat(Connection data) {
		return data;
	}

	/*
	 * NOT USED - just left as an example of a consumer asynchronous return of
	 * access key request
	 */
	public Connection onNewConnection(Connection conn) {
		return conn;
	}

	// publishing point
	@Override
	public Connection publishConnect(Connection conn) {
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

	/**
	 * TODO - support
	 * <pre>
	 * SCHEMES
	 * 	tcp tcps
	 * 	upd dtls 
	 * 	ws  wss 
	 * 
	 * SERIALIZATIONS
	 * 	JSON
	 * 	binary - native
	 * 	Protobuff
	 * </pre>
	 */
	@Override
	synchronized public void sendRemote(URI uri, Message msg) {
		log.info("sendRemote {}", uri);
		String scheme = uri.getScheme();
		lastProtocolKey = uri.toString();
		if ("tcp".equals(scheme)) {
			sendRemoteTCP(uri, msg);
		} else if ("udp".equals(scheme)) {
			sendRemoteUdp(uri, msg);
		} else {
			error(String.format("%s not supported", uri.toString()));
			return;
		}
	}

	public void sendRemoteTCP(URI uri, Message msg) {
		tcpServer.sendTcp(uri, msg);
	}

	public void sendRemoteUdp(URI uri, Message msg) {
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

	public void setTcpPort(Integer tcpPort) {
		this.tcpPort = tcpPort;
	}

	public void setUdpPort(Integer udpPort) {
		this.udpPort = udpPort;
	}

	public void startListening() {
		startListening(6767);
	}

	public void startListening(int port) {
		udpPort = tcpPort = port;

		udpServer.start(port);
		tcpServer.start(port);

		isListening = true;
		broadcastState();
	}

	public void stopListening() {
		udpServer.stop();
		tcpServer.stop();
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
		super.stopService();
		stopListening();
	}

	public void startService() {
		super.startService();
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
		try {
			LoggingFactory.init(Level.WARN);

			RemoteAdapter remote = (RemoteAdapter) Runtime.start("remote", "RemoteAdapter");
			// remote.connect("tcp://demo.myrobotlab.org:6767");
			// remote.websocket("http://demo.myrobotlab.org:8888/api/messages");
			Runtime.start("gui", "SwingGui");
			// remote.startListening();

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	@Override
	public String publishConnect() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String publishDisconnect() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Status publishError() {
		// TODO Auto-generated method stub
		return null;
	}

}