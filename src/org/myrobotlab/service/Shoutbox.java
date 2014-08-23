package org.myrobotlab.service;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

import org.java_websocket.WebSocket;
import org.myrobotlab.framework.Encoder;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class Shoutbox extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Shoutbox.class);
	
	transient private WebGUI webgui;
	transient private NameProvider nameProvider = null;
	
	Integer port = 6565;

	int maxShoutsInMemory = 1000;
	ArrayList<Shout> shouts = new ArrayList<Shout>();
	
	public interface NameProvider {
		String getName(String token);
	}
	
	public static class Shout{
		public String user;
		public String type;
		public String msg;
		public String color;
	}
	
	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);
		peers.put("webgui", "WebGUI", "webgui");
		return peers;
	}

	public Shoutbox(String n) {
		super(n);
	}
	
	public void startService(){
		super.startService();
		if (webgui == null){
			webgui = (WebGUI)createPeer("webgui");
			webgui.setPort(port);
			webgui.startService();
			
			webgui.addConnectListener(this);
			webgui.addDisconnectListener(this);
		}
		
		//webgui.startWebSocketServer(port);
		// FIXME - netty websocket server
		// publishMsg --> onMsg
		//webgui.addMsgListener(this);
	}
	
	public void stopService(){
		super.stopService();
		if (webgui != null){
			webgui.stopService();
		}
	}
	
	public void onShout(String msg){
		info("%s",msg);		
		Shout shout = (Shout)Encoder.gson.fromJson(msg, Shout.class);
		shouts.add(shout);
		Message out = createMessage("shoutclient", "onShout", Encoder.gson.toJson(shout));
		webgui.sendToAll(out);
	}
	
	public void onConnect(WebSocket conn){
		log.info(conn.getRemoteSocketAddress().toString());
		Shout shout = new Shout();
		shout.type = "system";
		shout.user = conn.getRemoteSocketAddress().getHostString();
		shout.msg = String.format("%s is in the haus !", conn.getRemoteSocketAddress().getHostString());
		Message out = createMessage("shoutclient", "onShout", Encoder.gson.toJson(shout));
		webgui.sendToAll(out);
	}
	
	public void onDisconnect(WebSocket conn){
		log.info("" + conn);
	}
	
	public NameProvider setNameProvider(String classname) throws ClassNotFoundException, InstantiationException, IllegalAccessException{
		Class<?> theClass = Class.forName(classname);
		nameProvider = (NameProvider)theClass.newInstance();
		return nameProvider;
	}
	
	public void setNameProvider(NameProvider nameProvider){
		this.nameProvider = nameProvider;
	}
	
	
	@Override
	public String getDescription() {
		return "shoutbox server for myrobotlab";
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		//LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			Shoutbox shoutbox = (Shoutbox)Runtime.start("shoutbox", "Shoutbox");
			shoutbox.test();
			
			//Runtime.start("gui", "GUIService");

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

}
