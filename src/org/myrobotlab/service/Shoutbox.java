package org.myrobotlab.service;

import java.util.ArrayList;

import org.myrobotlab.fileLib.FileIO;
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
	Integer port = 6565;

	int maxShoutsInMemory = 1000;
	ArrayList<Shout> shouts = new ArrayList<Shout>();
	
	public static class Shout{
		String user;
		String msg;
	}
	
	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);
		peers.put("webgui", "WebGUI", "webgui");
		
		return peers;
	}

	public Shoutbox(String n) {
		super(n);
	}
	
	public void start(Integer port){
		if (webgui == null){
			webgui = (WebGUI)startPeer("webgui");
		}
		
		webgui.startWebSocketServer(port);
		// FIXME - netty websocket server
		webgui.addMsgListener(this);
	}

	@Override
	public String getDescription() {
		return "shoutbox server for myrobotlab";
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		//LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			Shoutbox template = (Shoutbox)Runtime.start("template", "_TemplateService");
			template.test();
			
			Runtime.start("gui", "GUIService");

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

}
