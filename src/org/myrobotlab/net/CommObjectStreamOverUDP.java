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
 * References :
 * 	http://systembash.com/content/a-simple-java-udp-server-and-udp-client/
 * 
 * */

package org.myrobotlab.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.interfaces.Gateway;
import org.slf4j.Logger;

// FIXME remove communication interface
public class CommObjectStreamOverUDP implements Gateway, Serializable {

	public final static Logger log = LoggerFactory.getLogger(CommObjectStreamOverUDP.class.getCanonicalName());
	private static final long serialVersionUID = 1L;
	boolean isRunning = false;
	static public transient HashMap<URI, UDPThread> clientList = new HashMap<URI, UDPThread>();
	
	Service myService = null;

	public class UDPThread extends Thread {
		URI url;
		transient DatagramSocket socket = null;
		CommData data;

		ObjectInputStream in = null;
		ObjectOutputStream out = null;

		byte[] buffer = new byte[65535];
		ByteArrayInputStream b_in = new ByteArrayInputStream(buffer);
		DatagramPacket dgram = new DatagramPacket(buffer, buffer.length);

		public UDPThread(URI url, DatagramSocket socket) {
			super("udp " + url);
			try {
				this.url = url;
				if (socket == null) {
					socket = new DatagramSocket();
				}

				this.socket = socket;
				this.start();
			} catch (SocketException e) {
				Logging.logException(e);
			}
		}

		synchronized public void send(final URI url, final Message msg) throws IOException {

			String host = url.getHost();
			int port = url.getPort();

			log.info("sending udp msg to " + host + ":" + port + "/" + msg.getName());

			try {
				// FIXME - determine which can be re-used & what has to be
				// created/re-created new
				ByteArrayOutputStream b_out = new ByteArrayOutputStream();
				ObjectOutputStream out = new ObjectOutputStream(b_out);
				out.writeObject(msg);
				out.flush();
				byte[] buffer = b_out.toByteArray();

				// log.info("send " + msg.getParameterSignature());

				if (buffer.length > 65535) {
					log.error("udp datagram can not exceed 65535 msg size is " + buffer.length + " !");
				}

				DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(url.getHost()), url.getPort());
				socket.send(packet);

				out.reset();

			} catch (NotSerializableException e) {
				log.error("could not serialize [" + e.getMessage() + "]");
			}
		}

		@Override
		public void run() {
			try {
				isRunning = true; // this is GLOBAL !

				while (socket != null && isRunning) {

					Message msg = null;

					try {
						socket.receive(dgram); // blocks
						in = new ObjectInputStream(b_in);

						Object o = in.readObject();

						dgram.setLength(buffer.length); // must reset length
														// field!
						b_in.reset(); // reset so next read is from start of
										// byte[] again
						msg = (Message) o;

						// client's side -
						// "I connected to a listener and it replied with registerService"
						if (msg.method.equals("registerServices")) {
							myService.invoke("registerServices", dgram.getAddress().getHostAddress(), dgram.getPort(), msg);

							// addClient(socket, dgram.getAddress(),
							// dgram.getPort());
							continue;
						}

						myService.getInbox().add(msg);

					} catch (Exception e) {
						msg = null;
						Logging.logException(e);
					}

				}

				// closing connections TODO - why wouldn't you close the others?
				in.close();
				out.close();

			} catch (Exception e) {
				log.error("UDPThread threw");
				isRunning = false;
				socket = null;
				Logging.logException(e);
			}
		}// run

		public DatagramSocket getSocket() {
			return socket;
		}

	} // UDPThread

	public CommObjectStreamOverUDP(Service service) {
		this.myService = service;
	}
	
	@Override
	public void sendRemote(final String uri, final Message msg) throws URISyntaxException {
		sendRemote(new URI(uri), msg);
	}

	@Override
	public void sendRemote(final URI url, final Message msg) {

		UDPThread phone = null;

		try {

			if (clientList.containsKey(url)) {
				phone = clientList.get(url);
			} else {
				phone = new UDPThread(url, null);
				clientList.put(url, phone);
			}

			phone.send(url, msg);

		} catch (Exception e) {
			Logging.logException(e);
			return;
		}
	}

	public void addClient(URI url, Object commData) {
		log.info("adding tcp client ");

		UDPThread phone = new UDPThread(url, (DatagramSocket) commData);
		clientList.put(url, phone);
	}

	// TODO shutdown Communicator
	public void stopService() {

		if (clientList != null) {
			for (int i = 0; i < clientList.size(); ++i) {
				UDPThread r = clientList.get(i);
				if (r != null) {
					r.interrupt();
				}
				r = null;
			}

		}
		clientList.clear();
		clientList = new HashMap<URI, UDPThread>();
		isRunning = false;
	}

	@Override
	public HashMap<URI, CommData> getClients() {
		
		HashMap<URI, CommData> data = new HashMap<URI, CommData>();
		for (URI key : clientList.keySet()) {
			UDPThread tcp = clientList.get(key);
			data.put(key, tcp.data);
		}

		return data;
	}

	@Override
	public void connect(String uri) throws URISyntaxException {
		Message msg = myService.createMessage("", "register", null);
		sendRemote(uri, msg);
	}

}
