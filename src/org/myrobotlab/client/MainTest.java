package org.myrobotlab.client;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.MessageListener;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Proxy;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.data.Pin;

public class MainTest implements MessageListener {
	
	private int cnt;

	/**
	 * "receive" is the main gatway for all MRL messages into your application
	 * you can subscribe to all the other services's methods you would like and they
	 * will come to your application through this method.
	 */
	@Override
	public void receive(Message msg) {
		System.out.println(String.format("incoming msg %s.%s --> %s.%s", msg.sender, msg.sendingMethod, msg.name, msg.method));
		if (msg.method.equals("ourData"))
		{
			Pin pin = (Pin)msg.data[0];
			System.out.println(String.format("pin event pin %d from %s is now value %d", pin.pin, pin.source, pin.value));
		}// else if (msg.method.equals("otherMethod"))
		{
			// handle other subscriptions methods here...
		}	
	}

	public MainTest() {
		
		// make and start an Arduino
		Arduino arduino = new Arduino("arduino");
		arduino.startService();
		// connect default 8 N 1 to serial device
		arduino.connect("/dev/ttyACM0"); 
		//arduino.connect("COM10");
		//arduino.setBoard("diecimila");
		arduino.connect();
		
		// make and start a Proxy
		Proxy proxy = new Proxy("myApp");
		proxy.startService();
		
		// add this class so it can receive any messages sent to it
		proxy.addMessageListener(this);
		
		// subscribe to the methods/messages we are interested in
		// arduino.publishPin(Pin) --- is sent to us with msg.method name = "ourData"
		proxy.subscribe("publishPin", arduino.getName(), "ourData", Pin.class);
		arduino.digitalWrite(13, 1); 
		
		// start a gui if you want
		GUIService gui = new GUIService("gui");
		gui.startService();
		

		while (cnt < 100) {
			++cnt;
			try {

				// ################# Works Now ;)
				// ###################
				for (Pin p : arduino.getPinList()) {
					System.out.print(p.value + " ");
				}
				System.out.println();
				arduino.digitalWrite(13, 1); // turn on LED
				arduino.digitalWrite(9, 0); // turn on LED
				Thread.sleep(500);
				arduino.digitalWrite(8, 0); // turn on LED
				arduino.digitalWrite(9, 1); // turn on LED
				Thread.sleep(500);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// when you are done
		Runtime.releaseAll();
	}

	public static void main(String[] args) {
		new MainTest();
	}

}