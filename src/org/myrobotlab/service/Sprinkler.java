package org.myrobotlab.service;

import java.io.IOException;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class Sprinkler extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Sprinkler.class);

	WebGUI wegui;
	Arduino arduino;
	XMPP xmpp;
	
	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);
		peers.put("webgui", "WebGUI", "WebGUI service");
		peers.put("arduino", "Arduino", "Arduino service");
		return peers;
	}

	public Sprinkler(String n) {
		super(n);
	}
	
	public boolean connect(){
		return arduino.connect("/dev/ttyACM0");
	}
	
	public void startService() {
		arduino = (Arduino)startPeer("arduino");
		if (!connect()){
			// FIXME !!!
			// send mail error !!!
			// send xmpp error !!
		}
		
		// FIXME - start schedule
	}

	public void stopService() {
		if (arduino != null){
			arduino.disconnect();
			arduino.stopService();
		}
	}
	

	@Override
	public String getDescription() {
		return "uber sprinkler system";
	}

	public static void main(String args[]) throws InterruptedException, IOException {

		
	}

}
