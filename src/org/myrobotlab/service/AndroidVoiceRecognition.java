package org.myrobotlab.service;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.slf4j.Logger;

/**
 * based on _TemplateService
 */
/**
 *
 * @author LunDev (github), Ma. Vo. (MyRobotlab)
 */
public class AndroidVoiceRecognition extends Service implements TextPublisher {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory
			.getLogger(AndroidVoiceRecognition.class);

	private ClientHandler client;
	private int port = 5684;
	private final static String VERSION = "2015.01.01";

	public AndroidVoiceRecognition(String n) {
		super(n);
		// intializing variables
		// Should do something useful here in future
	}

	@Override
	public void startService() {
		super.startService();
		startServer();
	}

	@Override
	public void stopService() {
		super.stopService();
	}

	@Override
	public String getDescription() {
		return "utilizing Android's Voice Recognition";
	}

	public void setPort(int p) {
		port = p;
	}

	public void sendToClient(String mes) {
		send("fromServer=" + mes);
	}
	
	public void startRecognition() {
		send("startrecognition");
	}

	// Server-start
	private void startServer() {
		try {
			ServerSocket serverSock = new ServerSocket(port);
			NewConnectionHandler nch = new NewConnectionHandler(serverSock);
			nch.start();
		} catch (IOException ex) {
			System.out.println(ex);
		}
	}

	private void send(String mes) {

		try {
			client.getOut().writeObject(mes);
		} catch (IOException ex) {
			System.out.println(ex);
		}

	}

	private void process(String mes) {
		System.out.println(mes);
		if (mes.startsWith("version")) {
			String[] split = mes.split("=");

			boolean versionneuer = false;

			String aktversion2 = VERSION.replace(".", "~");
			String[] aktversionsplit = aktversion2.split("~");
			int[] aktversionsplitint = new int[aktversionsplit.length];
			for (int i = 0; i < aktversionsplit.length; i++) {
				aktversionsplitint[i] = Integer.parseInt(aktversionsplit[i]);
			}

			String runversion2 = split[1].replace(".", "~");
			String[] runversionsplit = runversion2.split("~");
			int[] runversionsplitint = new int[runversionsplit.length];
			for (int i = 0; i < runversionsplit.length; i++) {
				runversionsplitint[i] = Integer.parseInt(runversionsplit[i]);
			}

			for (int i = 0; i < 3; i++) {
				if (aktversionsplitint[i] < runversionsplitint[i]) {
					// eigener Versions-Teil ist NEUER wie der aktuelleste
					// Versions-Teil
					break;
				} else if (aktversionsplitint[i] > runversionsplitint[i]) {
					// eigener Versions-Teil ist AELTER wie der aktuelleste
					// Versions-Teil
					versionneuer = true;
					break;
				} else if (aktversionsplitint[i] > runversionsplitint[i]) {
					// eigener Versions-Teil ist GLEICH wie der aktuelleste
					// Versions-Teil
				}
			}

			if (versionneuer) {
				send("serverversion=" + VERSION);
				System.out.println("Client has an old version");
				client.finish();
			} else {
				send("accepted");
				System.out.println("Client accepted");
			}
		} else if (mes.startsWith("recognized")) {
			String[] split = mes.split("=");
			//TODO - (Python) callback
			invoke("recognized",split[1]);
		} else {
			System.out.println("ERROR: " + mes);
		}
		// sendToAll(mes);
	}

	private class NewConnectionHandler extends Thread {

		private final ServerSocket serverSock;

		public NewConnectionHandler(ServerSocket ss) {
			serverSock = ss;
		}

		@Override
		public void run() {
			while (true) {
				try {
					Socket clientSocket = serverSock.accept();

					ClientHandler ch = new ClientHandler(clientSocket);
					client = ch;
					ch.start();

					System.out.println("Client connected");
					// Only accept one client
					break;
				} catch (IOException ex) {
					System.out.println(ex);
				}
			}
		}
	}

	private class ClientHandler extends Thread {

		private boolean running;
		private Socket clientSocket;
		private ObjectInputStream in;
		private ObjectOutputStream out;

		public ClientHandler(Socket socket) {
			try {
				clientSocket = socket;
				in = new ObjectInputStream(clientSocket.getInputStream());
				out = new ObjectOutputStream(clientSocket.getOutputStream());
			} catch (Exception ex) {
				System.out.println(ex);
			}
			running = true;
		}

		public ObjectOutputStream getOut() {
			return out;
		}

		public void finish() {
			try {
				in.close();
				out.close();
				clientSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			running = false;
		}

		@Override
		public void run() {
			try {
				Object obj;
				while (running && (obj = in.readObject()) != null) {
					// System.out.println("got a message!");
					String mes = (String) obj;
					process(mes);
				}
			} catch (IOException | ClassNotFoundException ex) {
				System.out.println(ex);
			}

		}
	}

	// Server-end

	public static void main(String[] args) throws InterruptedException {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);
		try {

			Runtime.start("gui", "GUIService");
			Runtime.start("avr", "AndroidVoiceRecognition");

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	@Override
	public String publishText(String text) {
		return text;
	}
	
	public String recognized(String text) {
		return text;
	}
}
