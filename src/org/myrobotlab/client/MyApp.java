package org.myrobotlab.client;

import org.myrobotlab.framework.Message;

public class MyApp implements Receiver {
	static int msgCount;
	public static void main(String[] args) throws InterruptedException {

		MyApp myApp = new MyApp();
		
		// create the client with a unique name
		MRLClient mrl = new MRLClient("myApp", MRLClient.COMMUNICATION_TYPE_TCP);
		// register the client with the running MyRobotLab
		mrl.register("localhost", 6767, myApp);
		// subscribe to a service & method - in this case logger.log(Message)
		// mrl.subscribe("log", "logger", "receive", Message.class);
		for (int i = 0; i < 100; ++i)
		{
			// send message to logger.log(Message) 
			// since logger.log accepts all types since its input
			// is a Message
			//mrl.send("logger", "log", "Hello World " + i);
			mrl.send ("controller", "input", i);
			//Thread.sleep(5);
		}
	}

	@Override
	public void receive(Message msg) {
		System.out.println(msg.sender + "." + msg.sendingMethod + "->" + msg.name + "." + msg.method + " " + msg.data[0] + "," + msg.data[1]);
		++msgCount;
		System.out.println("msgCount " + msgCount);
	}

}
