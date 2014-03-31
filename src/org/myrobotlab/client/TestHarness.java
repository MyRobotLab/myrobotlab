package org.myrobotlab.client;

import org.myrobotlab.framework.Message;

public class TestHarness implements Receiver {
	
	public static void main(String[] args) {

		TestHarness test = new TestHarness();
		
		// create the client with a unique name
		MRLClient mrl = new MRLClient("myApp");
		// register the client with the running MyRobotLab
		mrl.register("localhost", 6767, test);
		// subscribe to a service & method - in this case logger.log(Message)
		// mrl.subscribe("log", "logger", "receive", Message.class);
		for (int i = 0; i < 100; ++i)
		{
			// send message to logger.log(Message) 
			// since logger.log accepts all types since its input
			// is a Message
			//mrl.send("logger", "log", "Hello World " + i);
			float float0 = 1.3f;
			float float1 = 2.3f;
			float float2 = 3.3f;
			mrl.send ("jython", "input", float0 , float1 , float2);
		}
	}

	@Override
	public void receive(Message msg) {
		System.out.println(msg.data[0]);		
		System.out.println(msg.data[1]);		
		System.out.println(msg.data[2]);		
	}

}
