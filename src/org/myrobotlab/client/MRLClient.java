package org.myrobotlab.client;

import java.lang.reflect.Constructor;

import org.myrobotlab.cmdline.CMDLine;
import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Message;

/**
 * MRLClient
 * class used to interface with a remote running instance or MyRobotLab.  The instance of MRL
 * must be running a RemoteAdapter service.  This class can send and recieve messages
 * from the MRL instance using only a few methods.  
 * The following code is an example of creating a client, registering a MRL instance,
 * sending a subscribe message to a TestCatcher service named "catcher01".
 * Whenever catcher01's catchInteger method is invoked a message will be sent back
 * to the MRL client.  And finally a "send" which sends a message to trigger the callback
 * event.
 * 
 * Example Code:
 * 
 * 		MRLClient api = new MRLClient();
 * 		Receiver client = new Receiver();
 *
 *		api.register("localhost", 6767, client);
 *		api.subscribe("catchInteger", "catcher01", "myMsg", Integer.TYPE);
 *		api.send("catcher01", "catchInteger", 5);
 *
 */
public class MRLClient implements Receiver {

	String name;
	Communicator listener = null;
	
	public final static String COMMUNICATION_TYPE_UDP = "org.myrobotlab.client.CommObjectStreamOverUDP";
	public final static String COMMUNICATION_TYPE_TCP = "org.myrobotlab.client.CommObjectStreamOverTCP";
	
	// global
	String host = null;
	int port = -1;
 	
	public static boolean debug = false;

	public MRLClient(String name)
	{
		this(name, COMMUNICATION_TYPE_UDP);
	}
	
	public MRLClient(String name, String communicationType)
	{
		this.name = name;
		this.listener = getCommunicator(communicationType, name);
	}
	
	
	/**
	 * CommunicatorObjectFactory - should be interface
	 * 
	 * @param type - type name to create
	 * @return
	 */
	public static Communicator getCommunicator(String type, String name)
	{
		Object params[] = null;
		if (name != null) {
			params = new Object[1];
			params[0] = name;
		}

		Class<?> c;
		try {
			c = Class.forName(type);
			Constructor<?> mc = c.getConstructor(new Class[] { name.getClass() });
			return (Communicator) mc.newInstance(params); // Dynamically instantiate it

		} catch (Exception e) {
			e.printStackTrace();
		} 
		return null;
	}
	
	/**
	 * method which registers with a running MRL instance.  It does this by sending a service
	 * directory update to the MRL's RemoteAdapter.  From MRL's perspective it will appear
	 * as if this is another MRL instance with a single service.  The bogus service name
	 * is controlled by overriding Receiver.getName()
	 * 
	 * Once a MRLClient registers is may send messages and subscribe to events.
	 * 
	 * @param host - target host or ip of the running MRL instance's RemoteAdapter
	 * @param port - target port of the RemoteAdapter
	 * @param client - call back interface to receive messages
	 * @return
	 */
	final public boolean register(String host, int port, Receiver client) {
		if (host == null || host.length() == 0 || port < 0)
		{
			System.out.println("host and port need to be set for registering");
			return false;
		}
		
		this.host = host;
		this.port = port;		

		return listener.register(host, port, client);
	}

	
	/**
	 * method to send a messages to a running MRL instances. @see register(String, int, Receiver) must
	 * be called before "send" can be used.
	 * 
	 * @param name - name of the service to receive the message (message destination)
	 * @param method - method to invoke
	 * @param data - parameter data for the method
	 * @return
	 */
	final synchronized public boolean send(String name, String method, Object... data) 
	{
		return listener.send(name, method, "send", data);
	}
	
	/**
	 * Subscribes to a remote MRL service method. When the method is called on
	 * the remote system an event message with return data is sent. It is
	 * necessary to registerForServices before subscribing.
	 * 
	 * @param outMethod - the name of the remote method to hook/subscribe to
	 * @param serviceName - service name of the remote service
	 * @param inMethod - inMethod can be used as an identifier
	 * @param paramTypes
	 */
	public void subscribe(String outMethod, String serviceName, String inMethod, Class<?>... paramTypes) {
		MRLListener listener = new MRLListener(outMethod, getName(), inMethod,
				paramTypes);
		send(serviceName, "addListener", listener);
	}

	public void unsubscribe(String outMethod, String serviceName, String inMethod, Class<?>... paramTypes) {
		MRLListener listener = new MRLListener(outMethod, getName(), inMethod,
				paramTypes);
		send(serviceName, "removeListener", listener);
	}
	
	@Override
	public void receive(Message msg) {
		System.out.println("received " + msg);
	}

	public String getName() {
		return name;
	}

	public static void main(String[] args) {

		CMDLine cmdline = new CMDLine();
		cmdline.splitLine(args);

		if (cmdline.hasSwitch("-test")){
			test();
			return;
		}
		
		/*
		MRLClient api = new MRLClient();
  		//Receiver client = new Receiver();
 
 		api.register("localhost", 6767, api);
 		api.subscribe("catchInteger", "catcher01", "myMsg", Integer.TYPE);
 		api.send("catcher01", "catchInteger", 5);
		*/
		
		MRLClient client = new MRLClient("MRLClient");		
		
		client.host = cmdline.getSafeArgument("-host", 0, "localhost");
		client.port = Integer.parseInt(cmdline.getSafeArgument("-host", 0, "6767"));
		String service = cmdline.getSafeArgument("-service", 0, "myService");
		String method = cmdline.getSafeArgument("-method", 0, "doIt");
		int paramCount = cmdline.getArgumentCount("-data");
		
		Object[] data = new Object[paramCount];
		
		for (int i = 0; i < paramCount; ++i)
		{
			try {
				Integer d = Integer.parseInt(cmdline.getSafeArgument("-data", i, ""));
				data[i] = d;
			} catch (Exception e) {
				data[i] = cmdline.getSafeArgument("-data", i, "");
			}
		}
		
		client.register(client.host, client.port, client);
		client.send(service, method, data);
		if (debug)
			System.out.println("CTRL-C to quit");

	}

	static public void test()
	{
		MRLClient client = new MRLClient("MRLClient");
		client.register("localhost", 6767, client);
		client.send("catcher01", "catchInteger", 3);
		client.subscribe("catchInteger", "catcher01", "catchInteger", Integer.TYPE);
		client.send("catcher01", "catchInteger", 5);
		System.out.println("CTRL-C to quit");
	}
	
	static public String help() {
		return "java -jar MRLClient.jar -host [localhost] -port [6767] -service [myService] -method [doIt] -data \"data1\" \"data2\" \"data3\"... \n"
				+ "host: the name or ip of the instance of MyRobotLab which the message should be sent."
				+ "port: the port number which the foreign MyRobotLab is listening to."
				+ "service: the Service the message is to be sent."
				+ "method: the method to be invoked on the Service"
				+ "data: the method's parameters.";

	}
	
}