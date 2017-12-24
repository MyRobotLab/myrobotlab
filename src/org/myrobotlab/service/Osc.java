package org.myrobotlab.service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPortOut;

public class Osc extends Service implements OSCListener {

  private static final long serialVersionUID = 1L;

  // Map<String, OSCPortOut> senders = new HashMap<String, OSCPortOut>(); 
  transient OSCPortOut sender;
  String senderHost;
  Integer senderPort;
  
  transient OSCPortIn receiver;
  Integer port;

  public final static Logger log = LoggerFactory.getLogger(Osc.class);
  
  public static class OscMessage {
	long ts;
	String address;
	List <Object> arguments;

	  public OscMessage(long ts, OSCMessage message) {
		this.ts = ts;
		this.address = message.getAddress();
		this.arguments = message.getArguments();
	  }
	  
	  public long getDate(){
		  return ts;
	  }
				
	  public List<Object> getArguments(){
		  return arguments;
	  }
	  
	  public String getAddress(){
		  return address;
	  }
	  
	  public String toString(){
		  StringBuilder sb = new StringBuilder();
		  sb.append("osc ");
		  sb.append(ts);
		  sb.append(" ");
		  sb.append(address);
		  if (arguments != null){
			  sb.append(" [");
			  for (int i = 0; i < arguments.size(); ++i){
				  sb.append(arguments.get(i));
				  if (i != arguments.size() -1){
					  sb.append(", ");
				  }
			  }
			  sb.append("]");
		  }
		  return sb.toString();
	  }
  }

  public Osc(String n) {
    super(n);
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(Osc.class.getCanonicalName());
    meta.addDescription("Service for the Open Sound Control using the JavaOsc library");
    meta.setAvailable(true); // false if you do not want it viewable in a gui
    meta.setLink("http://www.illposed.com/software/javaosc.html");
    // add dependency if necessary
    meta.addDependency("com.illposed.osc", "0.4");
    meta.addCategory("network", "music");
    return meta;
  }
  
  public void listen(Integer newPort) throws IOException{
    listen("/*", newPort);
  }

  public void listen(String filter, Integer newPort) throws IOException {

    if (port != null && port != newPort) {
      receiver.stopListening();
      receiver = null;
    }

    if (newPort == null) {
      newPort = 12000;
    }   
    port = newPort;
    receiver = new OSCPortIn(newPort);
    if (filter != null){
      receiver.addListener(filter, this);
    }
    receiver.startListening();
  }
  
  public void stopListening(){
    if (receiver != null){
      receiver.stopListening();
    }
  }
  
  public OSCPortOut connect(String host, Integer port) throws SocketException, UnknownHostException{
    if (!host.equals(senderHost) || port.equals(senderPort)){
      senderHost = host;
      senderPort = port;
      sender = new OSCPortOut(InetAddress.getByName(host), port);
      broadcastState();
    }
    return sender;
  }
  
  public void sendMsg(String topic, Object... args) throws IOException {
    if (sender == null){
      error("you must connect first - osc.connect(host, port)");
    }
    List<Object> list = new ArrayList<Object>(Arrays.asList(args));
    OSCMessage msg = new OSCMessage(topic, list);
    sender.send(msg);
  }

  
  public OSCPortIn getReceiver(){
    return receiver;
  }

  /*
   * convert and publish to an Mrl Osc Message
   * adding ts to messsage as well.
   */
  public OscMessage publishOscMessage(long ts, OSCMessage message){
	OscMessage msg = new OscMessage(ts, message);
	log.info("{}", msg);
    return msg;
  }

  @Override
  public void acceptMessage(Date date, OSCMessage message) {
	  // JavaOSC bug - date always comes in as null
    invoke("publishOscMessage", new Date().getTime(), message);
  }
  
  public void stopService(){
    stopListening();
  }
  
  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);
       
      Osc osc = (Osc)Runtime.start("osc", "Osc");
      /*
      Python python = (Python)Runtime.start("python", "Python"); 
      python.subscribe("osc", "publishOSCMessage");
      */
      osc.listen(6000);
      // osc.listen("/filter1", 6000);      
      // osc.stopListening();
      
      osc.connect("127.0.0.1", 6000);
      osc.sendMsg("/test", "to be or not to be that is the question");
      osc.sendMsg("/newTopic", 18, "hello", 4.5);
      osc.sendMsg("/somewhere/else", "this", "is", 7, 3.3, "variable arguments");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
