package org.myrobotlab.net;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.URI;
import java.util.Iterator;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.ServiceEnvironment;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.RemoteAdapter;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.CommunicationInterface;
import org.slf4j.Logger;

/**
 * FIXME - make abstract class Server with abstract connection info &amp;
 * serializers stop &amp; start are all the same (between UdpServer &amp; TcpServer)
 * 
 * @author grperry
 *
 *
 */
public class UdpServer implements Runnable {

	public final static Logger log = LoggerFactory.getLogger(UdpServer.class);

	DatagramSocket serverSocket;
	RemoteAdapter myService;
	Integer serverPort;
	Thread serverThread;
	boolean isRunning = false;

	public UdpServer(RemoteAdapter s) {
		myService = s;
	}

	public void start(int serverPort) {
		
		this.serverPort = serverPort;

		if (serverThread != null) {
			stop();
		}

		serverThread = new Thread(this, String.format("%s.udp.%d", myService.getName(), this.serverPort));
		serverThread.start();
	}

	public void stop() {
		if (serverThread != null) {
			serverThread.interrupt();
		}
	}

	// FIXME FIXME FIXME - large amount of changes to tcp - application
	// logic which handles the "Messaging" should be common to both
	// tcp & udp & xmpp
	@Override
	public void run() {
		isRunning = true;
		try {
			serverSocket = new DatagramSocket(serverPort);
			log.info(String.format("%s UdpServer listening on %s:%d", myService.getName(), serverSocket.getLocalAddress(), serverSocket.getLocalPort()));

			byte[] b = new byte[65507]; // max udp size 65507 + 8 byte
			// header = 65535
			ByteArrayInputStream b_in = new ByteArrayInputStream(b);
			DatagramPacket dgram = new DatagramPacket(b, b.length);

			while (isRunning) {
				serverSocket.receive(dgram); // receives all datagrams
				// FIXME - do we need o re-create???
				ObjectInputStream o_in = new ObjectInputStream(b_in);
				try {
					Message msg = (Message) o_in.readObject();
					dgram.setLength(b.length); // must reset length field!
					b_in.reset();

					/*
					 * if ("getConnections".equals(msg.method)) {
					 * 
					 * // get connections List<Connection> conn =
					 * getConnections(new URI(String.format("tcp:/%s:%d",
					 * dgram.getAddress(), dgram.getPort()))); // send them back
					 * for (int i = 0; i < conn.size(); ++i) { Message
					 * newConnMsg = createMessage(null, "publishConnection",
					 * conn); byte[] msgBuf =
					 * org.myrobotlab.codec.CodecUtils.getBytes(newConnMsg);
					 * DatagramPacket dgp = new DatagramPacket(msgBuf,
					 * msgBuf.length, dgram.getAddress(), dgram.getPort());
					 * socket.send(dgp); }
					 * 
					 * // we will have to search for them again } else if
					 * ("publishConnection".equals(msg.method)) {
					 * myService.invoke("onCommOptions", msg.data[0]); } else
					 */

					if ("register".equals(msg.method)) {
						// FIXME name should be "Runtime" representing the
						// static
						// BEGIN ENCAPSULATION --- ENCODER BEGIN
						// -------------
						// IMPORTANT - (should be in Encoder) - create the
						// key
						// for foreign service environment
						// Runtime.addServiceEnvironment(name, protocolKey)
						URI protocolKey = new URI(String.format("udp://%s:%d", serverSocket.getInetAddress().getHostAddress(), serverSocket.getPort()));
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
								Message sendService = Message.createMessage(myService, null, "register", toRegister);
								Message outbound = Message.createMessage(myService, myService.getName(), "sendRemote", new Object[] { protocolKey, sendService });
								myService.getInbox().add(outbound);

							}

						}

						// BEGIN ENCAPSULATION --- ENCODER END -------------
					} else {
						// ++udpRx;
						myService.getOutbox().add(msg);
					}

				} catch (Exception e) {
					log.error("processing msg threw", e);
				}
				dgram.setLength(b.length); // must reset length field!
				b_in.reset(); // reset so next read is from start of byte[]
				// again
			} // while isRunning

		} catch (SocketException se) {
			log.error("UdpListener could not listen", se);
		} catch (Exception e) {
			log.error("wtf error", e);
		}
	}

	public void shutdown() {
		isRunning = false;
		if ((serverSocket != null) && (!serverSocket.isClosed())) {
			serverSocket.close();
		}
	}
}
