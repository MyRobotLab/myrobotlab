package org.myrobotlab.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.ServiceEnvironment;

class CommObjectStreamOverUDP extends Thread implements Communicator {
	
	DatagramSocket datagramSocket = null;

	boolean debug = false;
	boolean isRunning = false;
	CommObjectStreamOverUDP instance = null;
	
	// inbound 
	byte[] inBuffer = new byte[65535]; // datagram max size
	ByteArrayInputStream inByteStream = null;
	DatagramPacket inDataGram = null;
	Receiver client = null;
	
	String host = null;
	int port = -1;

	// out bound
	ByteArrayOutputStream outByteStream = null;
	ObjectOutputStream outObjectStream = null;


	public CommObjectStreamOverUDP(String n) {
		super(n);
		instance = this;
	}

	public void shutdown() {
		if ((datagramSocket != null) && (!datagramSocket.isClosed())) {
			datagramSocket.close();
			datagramSocket = null;
		}
		
		isRunning = false;
		instance.interrupt();
		instance = null;
	}

	public void run() {

		try {
			System.out.println(getName() + " listenerUDP listening on "
					+ datagramSocket.getLocalAddress() + ":"
					+ datagramSocket.getLocalPort());

			isRunning = true;

			while (isRunning) {
				datagramSocket.receive(inDataGram); // blocks
				ObjectInputStream o_in = new ObjectInputStream(inByteStream);
				try {
					Message msg = (Message) o_in.readObject();
					// must reset length field!
					inDataGram.setLength(inBuffer.length); 
					// reset so next read is from start of byte[] again
					inByteStream.reset(); 

					if (msg == null) {
						System.out.println("UDP null message");
					} else {

						// client API
						if (client != null) {
							client.receive(msg);
						}
					}

				} catch (ClassNotFoundException e) {
					System.out.println("ClassNotFoundException - possible unknown class sent from MRL instance");
					System.out.println(e.getMessage());
				}
				inDataGram.setLength(inBuffer.length); // must reset length
														// field!
				inByteStream.reset(); // reset so next read is from start of
										// byte[]
				// again
			} // while isRunning

		} catch (Exception e) {
			System.out.println("listenerUDP could not listen");
		}
	}
	
	/* (non-Javadoc)
	 * @see org.myrobotlab.client.Communicator#register(java.lang.String, int, org.myrobotlab.client.Receiver)
	 */
	@Override
	final public boolean register(String host, int port, Receiver client) {
		
		try {
			this.host = host;
			this.port = port;
			this.client = client;
			
			//ServiceDirectoryUpdate sdu = new ServiceDirectoryUpdate();
			//sdu.serviceEnvironment = new ServiceEnvironment(sdu.remoteURL);
			// pushing bogus Service with name into SDU
			ServiceEnvironment local = new ServiceEnvironment(null);
			/* FIXME - make proxy
			ServiceInterface sw = new ServiceInterface(getName(), null, local.accessURL);
			local.serviceDirectory.put(getName(), sw);
			*/

			send(null, "registerServices", "registerUDP", new Object[]{local});

			// start listening on the new datagramSocket
			// listenerUDP = new CommObjectStreamOverUDP("udp_" + host + "_" + port);
			start(); 
					
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return false;
		}

		return true;
	}

	/* (non-Javadoc)
	 * @see org.myrobotlab.client.Communicator#send(java.lang.String, java.lang.String, java.lang.String, java.lang.Object)
	 */
	@Override
	final synchronized public boolean send(String name, String method, String sendingMethod, Object... data) {
		
		if (debug)
			System.out.println("sendFromUDP message to " + name + "." + method + "()" );
		
		if (datagramSocket == null)
		{
			datagramSocket = getUDPSocket();
		}
		
		Message msg = new Message();
		msg.name = name;
		msg.method = method;
		msg.sender = getName(); 
		msg.sendingMethod = sendingMethod;
		msg.data = data;

		// send it
		try {
			// ObjectStreams must be recreated
			outByteStream.reset();
			outObjectStream = new ObjectOutputStream(outByteStream); 
			outObjectStream.writeObject(msg);
			outObjectStream.flush();
			byte[] b = outByteStream.toByteArray();

			DatagramPacket packet = new DatagramPacket(b, b.length, InetAddress.getByName(host), port);

			datagramSocket.send(packet);

		} catch (Exception e) {
			System.out.println("threw [" + e.getMessage() + "]");
			return false;
		}
		return true;
	}
	

	/**
	 * method to initialize the necessary data components for UDP communication
	 */
	private DatagramSocket getUDPSocket()
	{		
		try {
			datagramSocket = new DatagramSocket();
			// inbound
			inByteStream = new ByteArrayInputStream(inBuffer);
			inDataGram = new DatagramPacket(inBuffer, inBuffer.length);
	
			// outbound
			outByteStream = new ByteArrayOutputStream();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}

		return datagramSocket;
	}

	
}
