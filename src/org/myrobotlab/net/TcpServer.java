package org.myrobotlab.net;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.framework.Message;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.RemoteAdapter;
import org.slf4j.Logger;

public class TcpServer implements Runnable {
	public final static Logger log = LoggerFactory.getLogger(TcpServer.class);

	/**
	 * list of active tcp connections tcp connections need a new thread for a
	 * listener - to recv messages and maintain the connection udp does not - it
	 * requires a single server on a single port
	 */
	transient private HashMap<URI, TcpThread> tcpClientList = new HashMap<URI, TcpThread>();

	transient RemoteAdapter myService = null;
	transient ServerSocket serverSocket = null;
	transient ObjectOutputStream out;
	transient ObjectInputStream in;
	Integer serverPort;
	boolean isRunning = false;
	transient Thread serverThread = null;

	public TcpServer(RemoteAdapter s) {
		myService = s;
	}

	public void start(int serverPort){
		
		this.serverPort = serverPort;
		
		if (serverThread != null){
			stop();
		}
		
		serverThread = new Thread(this, String.format("%s.tcp.%d", myService.getName(), this.serverPort));
		serverThread.start();
	}

	public void stop() {
		if (serverThread != null) {
			serverThread.interrupt();
		}
	}

	@Override
	public void run() {
		try {

			serverSocket = new ServerSocket(serverPort, 10);
			myService.info(String.format("TcpServer listening on %s", serverSocket.getLocalSocketAddress()));

			isRunning = true;

			while (isRunning) {
				// FIXME - on contact register the "environment" regardless
				// if a service registers !!!
				java.net.Socket clientSocket = serverSocket.accept();
				// inbound connection FIXME - all keys constructed in
				// Encoder
				// TODO reduce these 2 methods to an interface
				// addTcpClient(clientSocket, myService);
				
				
				// ADD TCP CLIENT BEGIN ! - probably should not be in RemoteAdapter - as this is a detail for tcp
				
				String clientKey = String.format("tcp://%s:%d", clientSocket.getInetAddress().getHostAddress(), clientSocket.getPort());
				URI uri = new URI(clientKey);
				// HELP PROTOKEY VS MRL KEY ??
				TcpThread tcp = new TcpThread(myService, uri, clientSocket);
				tcpClientList.put(uri, tcp);
				myService.connections.put(uri, tcp.data);
				
				// ADD TCP CLIENT END ! - probably should not be in RemoteAdapter - as this is a detail for tcp
				
				myService.broadcastState();
			}

			serverSocket.close();
		} catch (Exception e) {
			log.error("tcp server socket threw", e);
		}
		isRunning = false;
	}

	public void shutdown() {
		if ((serverSocket != null) && (!serverSocket.isClosed())) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				// dont care
			}
		}
		serverSocket = null;
	}

	public boolean isReady() {
		if (serverSocket != null) {
			return serverSocket.isBound();
		}
		return false;
	}

	// FIXME - add to Gateway interfaceS
	public HashMap<URI, Connection> broadcastHeartbeat() {
		for (Map.Entry<URI, Connection> entry : myService.connections.entrySet()) {
			URI uri = entry.getKey();
			// Connection value = entry.getValue();

			// roll through send a set of transactions off & start a
			// IOCompletion like
			// array of status ...
			// if timeout is reached - write the rest with timeout (those that
			// did not
			// get an asynch response

			if (uri.getScheme().equals("tcp")) {
				TcpThread tcp = tcpClientList.get(uri);
				log.info("" + tcp);
				// check socket connectivity
				// attempt to re-connect if disconnected
				myService.broadcastState();
			}
		}
		// FIXME - refactor out
		return myService.connections;
	}

	public void sendTcp(URI uri, Message msg) {
		TcpThread tcp = null;
		try {
			if (tcpClientList.containsKey(uri)) {
				tcp = tcpClientList.get(uri);
			} else {

				// constructor will throw if can not connect -> new Socket(host,
				// port)
				tcp = new TcpThread(myService, uri, null);
				tcpClientList.put(uri, tcp);
				// FIXME - refactor out
				myService.connections.put(uri, tcp.data);
				myService.broadcastState();
			}

			tcp.send(msg);
			myService.connections.get(uri).tx++;

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

}
