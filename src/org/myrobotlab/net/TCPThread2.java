package org.myrobotlab.net;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Iterator;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceEnvironment;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.CommunicationInterface;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

public class TCPThread2 extends Thread {

	public final static Logger log = LoggerFactory.getLogger(TCPThread2.class);

	Service myService;
	public Socket socket;
	public CommData data;
	ObjectInputStream in;
	ObjectOutputStream out;
	boolean isRunning = false;

	public TCPThread2(Service service, URI uri, Socket socket) throws UnknownHostException, IOException {
		super(String.format("%s_%s", service.getName(), uri));
		this.myService = service;
		this.data = new CommData(uri);
		if (socket == null) {
			socket = new Socket(uri.getHost(), uri.getPort());
		}
		this.socket = socket;
		out = new ObjectOutputStream((socket.getOutputStream()));
		out.flush();
		in = new ObjectInputStream(socket.getInputStream());
		this.start();
	}

	/**
	 * listening for inbound messages
	 */
	@Override
	public void run() {
		try {
			isRunning = true;
			data.state = CommData.CONNECTED;
			while (socket != null && isRunning) {

				Message msg = null;
				Object o = null;

				o = in.readObject();
				msg = (Message) o;
				++data.rx;
				data.rxSender = msg.sender;
				data.rxSendingMethod = msg.sendingMethod;
				data.rxName = msg.name;
				data.rxMethod = msg.method;

				// FIXME - THIS NEEDS TO BE NORMALIZED - WILL BE THE SAME IN
				// XMPP & WEBGUI & REMOTEADAPTER
				// FIXME - normalize to single method - check for data
				// type too ? !!!
				if (msg.method.equals("register")) {
					// BEGIN ENCAPSULATION --- ENCODER BEGIN -------------
					// IMPORTANT - (should be in Encoder) - create the key
					// for foreign service environment
					URI protoKey = new URI(String.format("tcp://%s:%d", socket.getInetAddress().getHostAddress(), socket.getPort()));
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

					Object[] msgData = msg.data;
					ServiceInterface si = null;

					if (msgData != null) {
						si = (ServiceInterface) msg.data[0];
						si.setHost(uri);
					}
					// HMMM a vote for String vs URI here - since we need to
					// catch syntax !!!

					// if security ... msg within msg
					// getOutbox().add(createMessage(Runtime.getInstance().getName(),
					// "register", inboundMsg));
					Runtime.register(si, uri);// <-- not an INVOKE !!! // -
					// no security ! :P

					if (foreignProcess == null) {

						// not defined we will send export
						// TODO - Security filters - default export (include
						// exclude) - mapset of name
						ServiceEnvironment localProcess = Runtime.getLocalServicesForExport();

						Iterator<String> it = localProcess.serviceDirectory.keySet().iterator();
						String name;
						ServiceInterface toRegister;
						while (it.hasNext()) {
							name = it.next();
							toRegister = localProcess.serviceDirectory.get(name);

							// the following will wrap a message within a
							// message and send it remotely
							// This Thread CANNOT Write on The
							// ObjectOutputStream directly -
							// IT SHOULD NEVER DO ANY METHOD WHICH CAN BLOCK
							// !!!! - 3 days of bug chasing when
							// it wrote to ObjectOutputStream and oos blocked
							// when the buffer was full - causing deadlock
							// putting it on the inbox will move it to a
							// different thread
							Message sendService = myService.createMessage("", "register", toRegister);
							Message outbound = myService.createMessage(myService.getName(), "sendRemote", new Object[] { protoKey, sendService });
							myService.getInbox().add(outbound);

						}

					}

					// BEGIN ENCAPSULATION --- ENCODER END -------------
				} else {
					myService.getOutbox().add(msg);
					++data.rx;
				}
			} // while

			// closing connections TODO - why wouldn't you close the others?
			in.close();
			out.close();

		} catch (Exception e) {
			isRunning = false;
			socket = null;
			Logging.logException(e);
			data.state = CommData.DISCONNECTED;
		}

		releaseConnect();

		// connection has been broken
		// myService.invoke("connectionBroken", url); FIXME
	}

	// FIXME - prepare for re-init / or completely de-init
	// and have RA re-establish connection
	public void releaseConnect() {
		try {
			data.state = CommData.DISCONNECTED;
			log.error("removing {} from registry", data.uri);
			// FIXME - not working - are you sure you want to do this?
			// just because the connection is broken
			Runtime.release(data.uri);
			log.error("shutting down thread");
			isRunning = false;
			log.error("attempting to close streams");
			in.close();
			out.close();
			log.error("attempting to close socket");
			socket.close();
		} catch (Exception dontCare) {
		}
	}

	public synchronized void send(Message msg) {
		try {

			/* debugging logging
			if (msg.data != null && msg.data[0] != null && msg.data[0] instanceof Service) {
				String n = ((Service) msg.data[0]).getName();
				log.info(String.format("serializing %s", n));
				if (n.equals("r1")) {
					log.info("here");
					// return;
				}

			}
			*/
			out.writeObject(msg);
			out.flush();
			out.reset(); // magic line OMG - that took WAY TO LONG TO FIGURE
							// OUT !!!!!!!
			data.txSender = msg.sender;
			data.txSendingMethod = msg.sendingMethod;
			data.txName = msg.name;
			data.txMethod = msg.method;
			++data.tx;

		} catch (Exception e) {
			myService.error(e);
			releaseConnect();
		}
	}

}
