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

package org.myrobotlab.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.ServiceEnvironment;
// http://zerioh.tripod.com/ressources/sockets.html
class CommObjectStreamOverTCP extends Thread implements Communicator {
	
	Socket socket = null;
	// URI ??
	boolean debug = false;
	boolean isRunning = false;
	CommObjectStreamOverTCP instance = null;
	
	// inbound 
	Receiver client = null;
	ObjectInputStream in = null;
	
	String host = null;
	int port = -1;

	// out bound
	ObjectOutputStream out = null;


	public CommObjectStreamOverTCP(String n) {
		super(n);
		instance = this;
	}

	public void shutdown() {
		if ((socket != null) && (!socket.isClosed())) {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			socket = null;
		}
		
		isRunning = false;
		instance.interrupt();
		instance = null;
	}

	public void run() {
		// FIXME - Socket if socket==null || closed -> socket = getTCPSocket(); - no globals?
		try {
			System.out.println(getName() + " listenerTCP listening on "
					+ socket.getLocalAddress() + ":"
					+ socket.getLocalPort());

			isRunning = true;
			// FIXME - MAKE NOT caller must do the same and create & cache a Object stream (do not create it every message!)
			// http://www.daniweb.com/software-development/java/threads/364424/java-sockets-help
			

			while (isRunning) {
				try {
					
					if (in == null)
					{
						in = new ObjectInputStream(socket.getInputStream());
					}
					
					Message msg = (Message) in.readObject();

					// client API
					if (client != null) {
						client.receive(msg);
					}

				} catch (ClassNotFoundException e) {
					System.out.println("ClassNotFoundException - possible unknown class sent from MRL instance");
					System.out.println(e.getMessage());
				}
			} // while isRunning

		} catch (Exception e) {
			System.out.println("listenerTCP could not listen ");
			e.printStackTrace();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.myrobotlab.client.Communicator#register(java.lang.String, int, org.myrobotlab.client.Receiver)
	 */
	final public boolean register(String host, int port, Receiver client) {
		
		try {
			this.host = host;
			this.port = port;
			this.client = client;
			
			//ServiceDirectoryUpdate sdu = new ServiceDirectoryUpdate();
			//sdu.serviceEnvironment = new ServiceEnvironment(sdu.remoteURL);
			// pushing bogus Service with name into SDU
			ServiceEnvironment local = new ServiceEnvironment(null);
			// FIXME - make Proxy
			/*
			ServiceInterface sw = new ServiceInterface(getName(), null, local.accessURL);
			local.serviceDirectory.put(getName(), sw);
			*/

			send(null, "registerServices", "register", new Object[]{local});

			// start listening on the new socket
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
			System.out.println("CommObjectStreamOverTCP send message to " + name + "." + method + "()" );
		
		if (socket == null)
		{
			socket = getTCPSocket();
		}
		
		Message msg = new Message();
		msg.name = name;
		msg.method = method;
		msg.sender = getName(); 
		msg.sendingMethod = sendingMethod;
		msg.data = data;

		// send it
		try {

			if (out == null)
			{
				out = new ObjectOutputStream(socket.getOutputStream());
			}
			out.writeObject(msg);
			out.flush();

		} catch (Exception e) {
			System.out.println("threw [" + e.getMessage() + "]");
			return false;
		}
		return true;
	}
	

	/**
	 * method to initialize the necessary data components for TCP communication
	 */
	private Socket getTCPSocket()
	{		
		try {
			socket = new Socket(host, port);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}

		return socket;
	}

	
}
