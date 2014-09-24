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
 * 		http://www.javaspecialists.eu/archive/Issue088.html - details of ObjectOutputStream.reset()
 * 		http://zerioh.tripod.com/ressources/sockets.html - example of Object serialization
 * 		http://www.cafeaulait.org/slides/sd2003west/sockets/Java_Socket_Programming.html nice simple resource
 * 		http://stackoverflow.com/questions/1480236/does-a-tcp-socket-connection-have-a-keep-alive
 * 
 * TCP can detect if a endpoint is "closed" - it also has the capability of using SO_KEEPALIVE
 * which will detect a broken connection - but the details are left up to the operating system (with
 * interval up to 2 hours!)
 * 	I believe a small interval keepalive with very small data-packet would be beneficial for both TCP & UDP
 *  Communicators
 *  
 *  A dead heartbeat would mean removal of all references of the dead system from the running system
 *  
 *  FIXME FIXME FIXME !!!!
 *  remove this class - RemoteAdapter should have all logic
 * 
 * */

package org.myrobotlab.net;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.WriteAbortedException;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceEnvironment;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.Communicator;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

public class CommObjectStreamOverTCP implements Communicator, Serializable {

	transient public final static Logger log = LoggerFactory.getLogger(CommObjectStreamOverTCP.class);
	private static final long serialVersionUID = 1L;
	static public transient HashMap<URI, TCPThread> clientList = new HashMap<URI, TCPThread>();
	HashMap<URI, Heart> heartbeatList = new HashMap<URI, Heart>();

	transient Service myService = null;
	boolean useHeartbeat = false;
	boolean heartbeatRunning = false;

	public class TCPThread extends Thread {

		URI url;
		transient Socket socket = null;
		CommData data = new CommData();
		ObjectInputStream in = null;
		ObjectOutputStream out = null;
		public final String name; // name of the creator, who created this
									// thread
		boolean isRunning = false;

		public TCPThread(URI url, Socket socket) throws UnknownHostException, IOException {
			super(String.format("%s_%s", myService.getName(), url));
			this.name = myService.getName();
			this.url = url;
			if (socket == null) {
				socket = new Socket(url.getHost(), url.getPort());
			}
			this.socket = socket;
			// TODO - could used buffered input / ouput - but I'm still stinging
			// from some
			// bug I don't understand where it appears a newly contructed clock
			// was sent
			// without the data being updated :( holding off any "optimizations"
			// until I figure out
			// what is going on
			// http://stackoverflow.com/questions/3365261/does-a-buffered-objectinputstream-exist
			// out = new ObjectOutputStream(new
			// BufferedOutputStream(socket.getOutputStream()));
			out = new ObjectOutputStream((socket.getOutputStream()));
			out.flush();// some flush before using :)
			// http://stackoverflow.com/questions/3365261/does-a-buffered-objectinputstream-exist
			in = new ObjectInputStream(socket.getInputStream());
			// in = new ObjectInputStream(new
			// BufferedInputStream(socket.getInputStream()));
			// in = new ObjectInputStream((socket.getInputStream()));
			this.start(); // starting listener
		}

		// listening for msgs
		@Override
		public void run() {
			try {
				isRunning = true;

				// FIXME - isRunning is a bad idea - its a single instance
				// ,however, there may be multiple
				// TCPThreads !!!
				while (socket != null && isRunning) {

					Message msg = null;
					Object o = null;
					try {
						o = in.readObject();
						msg = (Message) o;

					} catch (Exception e) {
						if (e.getClass() == NotSerializableException.class || e.getClass() == WriteAbortedException.class)
						{
							myService.error("someone tried to send something but it does not fit through the pipe");
						} else {
							myService.error(e.getMessage());
						}
						// FIXME - more intelligent ERROR handling - recover if
						// possible !!!!
						Logging.logException(e);
						msg = null;
						releaseConnect(e);
					}
					if (msg == null) {
						log.error("msg deserialized to null");
					} else {
						// FIXME - normalize to single method - check for data
						// type too ? !!!
						if (msg.method.equals("register")) {
							try {
								ServiceInterface sw = (ServiceInterface) msg.data[0];
							
								// IMPORTANT - (should be in Encoder) - create the key for foreign service environment
								String mrlURI = String.format("mrl://%s/tcp://%s:%d", myService.getName(), socket.getInetAddress().getHostAddress(), socket.getPort());
								URI uri = new URI(mrlURI);
								
								// check if the URI is already defined - if not - we will
								// send back the services which we want to export - Security will filter appropriately 
								ServiceEnvironment foreignProcess = Runtime.getServiceEnvironment(uri);
								if (foreignProcess == null){
									// not defined we will send export
									// TODO - Security filters - default export (include exclude) - mapset of name
									ServiceEnvironment localProcess = Runtime.getLocalServicesForExport();
									
									Iterator<String> it = localProcess.serviceDirectory.keySet().iterator();
									String name;
									ServiceInterface si;
									while (it.hasNext()) {
										name = it.next();
										si = localProcess.serviceDirectory.get(name);
										
										Message sendService = myService.createMessage("", "register", si);
										sendRemote(uri, sendService);
									}
									
								}
								
								// HMMM a vote for String vs URI here - since we need to
								// catch syntax !!!
								sw.setHost(uri);

								// if security ... msg within msg
								// getOutbox().add(createMessage(Runtime.getInstance().getName(),
								// "register", inboundMsg));
								Runtime.register(sw, uri);// <-- not an INVOKE !!!
								
								
								
								
							} catch (Exception e) {
								Logging.logException(e);
							}
							continue;
						}
						++data.rx;
						// myService.getInbox().add(msg);
						myService.getOutbox().add(msg);
					}
				}

				// closing connections TODO - why wouldn't you close the others?
				in.close();
				out.close();

			} catch (IOException e) {
				log.error("TCPThread threw");
				isRunning = false;
				socket = null;
				Logging.logException(e);
			}

			// connection has been broken
			// myService.invoke("connectionBroken", url); FIXME
		}

		// FIXME - UDP must do this too? - put in common location
		public void releaseConnect(Exception e) {
			try {
				// FIXME - more intelligent ERROR handling - recover if possible
				// !!!!
				Logging.logException(e);
				log.error("removing {} from registry", url);
				Runtime.release(url);
				log.error("removing {} client from clientList", url);
				clientList.remove(url);
				log.error("shutting down thread");
				isRunning = false;
				log.error("attempting to close streams");
				in.close();
				out.close();
				log.error("attempting to close socket");
				socket.close();
			} catch (Exception f) {
				// do nothing - closing down a bad connection, I don't want
				// another thrown
			}
		}

		public Socket getSocket() {
			return socket;
		}

		// TODO implement communication switching by
		// asking for protocol preference - retrieved from Notify
		// so that subscribers request preference !
		public synchronized void send(URI url2, Message msg) {
			try {
				// --- DEBUG TRAP --
				/*
				 * if (String.format("%s.%s", msg.sender,
				 * msg.sendingMethod).equals("clock2.publishState")) {
				 * log.info("here");
				 * 
				 * //log.error("****** sending object {} ********",
				 * System.identityHashCode(c));
				 * //log.error(String.format("%s tx %s.%s -tcp-> %s.%s",
				 * myService.getName(), msg.sender, msg.sendingMethod, msg.name,
				 * msg.method)); }
				 */

				//chase network bugs 
				//log.error(String.format("%s tx %s.%s -tcp-> %s.%s", myService.getName(), msg.sender, msg.sendingMethod, msg.name, msg.method));
				out.writeObject(msg);
				out.flush();
				out.reset(); // magic line OMG - that took WAY TO LONG TO FIGURE
								// OUT !!!!!!!
				++data.tx;
			
			} catch (Exception e) {
				if (e.getClass() == NotSerializableException.class || e.getClass() == WriteAbortedException.class)
				{
					myService.error(String.format("oops tried to send a %s but it does not fit through the pipe", msg.data[0].getClass().getCanonicalName()));
				} /*else {  HIDES FIRST RELEVANT MESSAGE
					myService.setError(e.getMessage());
				}*/
				Logging.logException(e);
				releaseConnect(e);
			}
		}

	} // TCP Thread

	public CommObjectStreamOverTCP(Service service) {
		this.myService = service;
	}

	// send tcp
	@Override
	public void sendRemote(final URI url, final Message msg) {

		TCPThread phone = null;
		if (clientList.containsKey(url)) {
			phone = clientList.get(url);
		} else {
			log.info("could not find url in client list attempting new connection ");
			try {
				phone = new TCPThread(url, null);
				clientList.put(url, phone);
			} catch (Exception e) {
				Logging.logException(e);
				log.error("could not connect to " + url);
				return;
			}
		}

		phone.send(url, msg);
	}

	public synchronized void addClient(URI url, Object commData) {
		if (!clientList.containsKey(url)) {
			log.debug("adding client " + url);
			try {
				TCPThread tcp = new TCPThread(url, (Socket) commData);
				clientList.put(url, tcp);

				if (useHeartbeat) {
					Heart heart = new Heart(url, this);
					heart.start();
					heartbeatList.put(url, heart);
				}

			} catch (Exception e) {
				Logging.logException(e);
				myService.error(String.format("could not connect to %s", url.toString()));
			}
		}
	}

	// TODO - Communicator - shutdown communicator
	public void stopService() {
		// TODO Auto-generated method stub
		for (Map.Entry<URI, TCPThread> o : clientList.entrySet()) {
			// Map.Entry<String,SerializableImage> pairs = o;
			URI uri = o.getKey();
			TCPThread thread = o.getValue();
			if (thread.name.equals(myService.getName())) {
				log.warn("%s shutting down tcp thread %s", myService.getName(), o.getKey());
				thread.isRunning = false;
				thread.interrupt();
			}
		}
	}

	class Heart extends Thread {
		URI url;
		CommObjectStreamOverTCP comm;
		boolean isRunning = false;
		int heartbeatIntervalMilliSeconds = 1000;

		Heart(URI url, CommObjectStreamOverTCP comm) {
			this.url = url;
			this.comm = comm;
		}

		@Override
		public void run() {
			try {
				while (isRunning) {
					Thread.sleep(heartbeatIntervalMilliSeconds);
					Message msg = new Message();
					msg.method = "echoHeartbeat";
					Heartbeat heartbeat = new Heartbeat();
					heartbeat.sender = myService.getName();
					msg.data = new Object[] { new Heartbeat() };
					// comm.send(name, msg);
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public HashMap<URI, CommData> getClients() {

		HashMap<URI, CommData> data = new HashMap<URI, CommData>();
		for (URI key : clientList.keySet()) {
			TCPThread tcp = clientList.get(key);
			data.put(key, tcp.data);
		}

		return data;
	}

}
