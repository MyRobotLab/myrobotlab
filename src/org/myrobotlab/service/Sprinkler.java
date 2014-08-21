package org.myrobotlab.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class Sprinkler extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Sprinkler.class);

	WebGUI wegui;
	Arduino arduino;
	Cron cron;
	
	String defaultPort = "/dev/ttyACM0";
	
	// TODO - memory appender
	ArrayList<String> history = new ArrayList<String>();
	
	
	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);
		peers.put("webgui", "WebGUI", "WebGUI service");
		peers.put("arduino", "Arduino", "Arduino service");
		peers.put("cron", "Cron", "Cron service");
		return peers;
	}

	public Sprinkler(String n) {
		super(n);
	}
	
	public boolean connect(){
		return arduino.connect(defaultPort);
	}
	
	public boolean connect(String port){
		defaultPort = port;
		return arduino.connect(defaultPort);
	}
	
	public void startService() {
		arduino = (Arduino)startPeer("arduino");
		if (!connect()){
			// FIXME !!!
			// send mail error !!!
			// send xmpp error !!
		}
		
		// FIXME - custom MRLComm.ino build to start with all digital pins = 1 HIGH
		// for the funky stinky nature of the relay board
		cron = (Cron)startPeer("cron");
		// FIXME - start schedule
		cron.addScheduledEvent("0 7 */3 * *", this.getName(), "onTimeToWater");
		cron.addScheduledEvent("30 7 */3 * *", this.getName(), "stop");
	}
	
	public void stop(){
		log.info("stop");
		history.add(String.format("stop %s", new Date().toString()));
		arduino.digitalWrite(5, 1);
		arduino.digitalWrite(6, 1);
		arduino.digitalWrite(7, 1);
		arduino.digitalWrite(8, 1);
		arduino.digitalWrite(9, 1);
		arduino.digitalWrite(10, 1);
		arduino.digitalWrite(11, 1);
		arduino.digitalWrite(12, 1);
	}
	
	// TODO - fix add length of watering
	public void onTimeToWater(){
		log.info("onTimeToWater");
		history.add(String.format("onTimeToWater %s", new Date().toString()));
		arduino.digitalWrite(5, 1);
		arduino.digitalWrite(6, 1);
		arduino.digitalWrite(7, 0);
		arduino.digitalWrite(8, 0);
		arduino.digitalWrite(9, 0);
		arduino.digitalWrite(10, 0);
		arduino.digitalWrite(11, 1);
		arduino.digitalWrite(12, 1);	
	}
	
	public ArrayList<String> getHistory(){
		return history;
	}
	
	public ArrayList<org.myrobotlab.service.Cron.Task> getTasks(){
		return cron.getTasks();
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
