package org.myrobotlab.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import org.java_websocket.WebSocket;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.fileLib.FindFile;
import org.myrobotlab.framework.Encoder;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.webgui.WSServer.WSMsg;
import org.slf4j.Logger;

public class Shoutbox extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Shoutbox.class);

	transient private WebGUI webgui;
	transient private static NameProvider nameProvider = new DefaultNameProvider();

	Integer port = 6565;

	int maxShoutsInMemory = 200;
	ArrayList<Shout> shouts = new ArrayList<Shout>();
	Users users = new Users();

	int msgCount;
	FileWriter fw = null;
	BufferedWriter bw = null;

	int maxArchiveRecordCount = 50;
	
	// FIXME - push all security into Security !!!
	// FIXME - security WebGUI REST interace / web interface - shutdown && allow
	// only methods "onShout" allow only service "shoutbox" allow only service
	// types
	// TODO - system commands - refresh / clear / reload / history /
	// resize-format / stats / show times / set my color
	// FIXME - hyperlink urls - colors fixed - emoticons
	// FIXME - define client & server - system and user commands
	// TODO - number of sessions / authenticated / guests - query deeper on each
	// user - stats - geo-location
	// FIXME - permissions - erase my chat - moderate others
	// scrollable - non scrollable - set wrap - menu display - Angular.js /
	// jquery
	// levels of authorization / admin
	// hover over - display - time other info
	// TODO - auto resize images
	// TODO - add modify delete own shout
	// TODO - days alive ! - stats (poll thread - only pushes on changes)
	// TODO - force logout command

	
	public static class Users {
		
		HashMap<WebSocket, User> wsToUser = new HashMap<WebSocket, User>();
		HashMap<String, User> userToUser = new HashMap<String, User>();
		
		public int getConnectionCount(){
			return wsToUser.size();
		}
		
		public int getUserCount(){
			return userToUser.size();
		}
		
		public User addUser(WebSocket ws){
			// "real" ip address Yay! - no reverse host lookup
			String ip = ws.getRemoteSocketAddress().getAddress().getHostAddress();
			String port = ws.getRemoteSocketAddress().getPort() + "";
			// new socket - might be a user who already has a session
			// doubt if  DrupalNameProvider is returning appropriate information
			
			// return HashMap of properties userid user# email etc ...
			String userid = nameProvider.getName(ip);
			User user = new User();
			
			if (wsToUser.containsKey(ws)){
				log.error("adding Websocket which is already in index %s", ws);
			}
			
			// populate user with new data on the "connect"
			user.ip = ip;
			user.port = port;
			user.socket = ws;
			user.user = userid;
			
			wsToUser.put(ws, user);
			if (!ip.equals(userid)){
				userToUser.put(userid, user);
			}
			return user;
		}

		public User getUser(WebSocket ws) {
			if (wsToUser.containsKey(ws)) {
				return wsToUser.get(ws);				
			}
			return null;
		}

		public int getGuestCount() {
			return getConnectionCount() - getUserCount();
		}

		public void remove(User user) {
			wsToUser.remove(user.socket);
			userToUser.remove(user);
		}
		
	}

	
	public interface NameProvider {
		String getName(String token);
	}

	/**
	 * Shout is the most common message structure being sent
	 * from client to WSServer and from WSServer broadcasted to
	 * clients - therefore instead of a seperate system message we
	 * will have system data components of the shout - these are to
	 * display server data on the clients
	 */
	public static class Shout {
		public String user;
		public String type;
		public String msg;
		public String color;
		public String ip;
		
		public String time;
		
		// system related
		public int connectionCount;
		public int userCount;
		public int guestCount;
		public int msgCount;
		
	}

	public static class User {
		public String user;
		public String ip;
		public String port;
		public String color;
		transient public WebSocket socket;
	}

	// if new socket & recently closed socket of user - then
	// "dwilli is on the move !"

	public static class DefaultNameProvider implements NameProvider {
		@Override
		public String getName(String token) {
			return token;
		}
	}

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);
		peers.put("webgui", "WebGUI", "webgui");
		return peers;
	}

	public Shoutbox(String n) {
		super(n);
	}

	public void startService() {
		super.startService();
		if (webgui == null) {
			webgui = (WebGUI) createPeer("webgui");
			webgui.setPort(port);
			webgui.startService();

			// subscribe to events
			webgui.addConnectListener(this);
			webgui.addDisconnectListener(this);
			webgui.addWSMsgListener(this);
		}

		// security allows the following method
		// webgui.allowMethod("onShout");

		try {
			String provider = "org.myrobotlab.client.DrupalNameProvider";
			log.info(String.format("attempting to set name provider - %s", provider));
			setNameProvider(provider);
		} catch (Exception e) {
			error(e);
		}

		// no REST - for security
		webgui.allowREST(false);
		// no direct messaging - for security 
		webgui.allowDirectMessaging(false);
		// FIXME - resource processor is not necessary either !!

		// webgui.startWebSocketServer(port);
		// FIXME - netty websocket server
		// publishMsg --> onMsg
		// webgui.addMsgListener(this);
		restore();
	}

	public void stopService() {
		super.stopService();
		if (webgui != null) {
			webgui.stopService();
		}
	}
	
	public void archive(Shout shout) {
		
		try {
			File dir = new File(getName());
			// archive chats
			if (!dir.exists()) {
				dir.mkdir();
			}

			if (fw == null){
				String filename = String.format("%s/shouts.%s.js", getName(), TSFormatter.format(new Date()));
				File archive = new File(filename);

				fw = new FileWriter(archive.getAbsoluteFile());
				bw = new BufferedWriter(fw);

				String d = String.format("%s", Encoder.gson.toJson(shout));
				bw.write(d);
				return;
			}
			
			String d = String.format(",%s", Encoder.gson.toJson(shout));
			bw.write(d);
			bw.flush();
			
			if (msgCount%maxArchiveRecordCount  == 0){
				close(bw);
				fw = null;
				bw = null;
			}

		} catch (Exception e) {
			Logging.logException(e);
		} 
	}

	public void restore() {
		try {
			File latest = null;
			// restore the last file back into memory
			List<File> files = FindFile.find(getName(), "shouts.*.js", false, false);

			for (int i = 0; i < files.size(); ++i) {
				File f = files.get(i);
				if (latest == null){
					latest = f;
				}
				if (f.lastModified() > latest.lastModified()) {
					latest = f;
				}
			}
			
			if (latest == null){
				log.info("no files found to restore");
				return;
			}
			
			info("loading latest file %s", latest);
			
			String json = String.format("[%s]", FileIO.fileToString(latest.getAbsoluteFile()));
			
			Shout[] saved = Encoder.gson.fromJson(json, Shout[].class);
			
			for (int i = 0; i < saved.length; ++i){
				shouts.add(saved[i]);
			}
			
		} catch (Exception e) {
			Logging.logException(e);
		}
	}
	
	// WTFU - onShout does not use this .. why??
	public Shout createShout(String type, String msg){
		Shout shout = new Shout();
		shout.type = type;
		shout.msg = msg;
		
		updateSystemInfo(shout);
		return shout;
	}

	// TODO Create User INFO & INDEXES HERE
	public void onConnect(WebSocket conn) {
		log.info(conn.getRemoteSocketAddress().toString());
		
		// send the user's definiton - back to his client ?
		User user = users.addUser(conn);
		Message onConnect = createMessage("shoutclient", "onConnect", Encoder.gson.toJson(user));
		conn.send(Encoder.gson.toJson(onConnect));//.sendToAll(onConnect);
		
		// BROADCAST ARRIVAL
		// TODO - broadcast to others new connection of user - (this mean's user has established new connection,
		// this could be refreshing the page, going to a different page, opening a new tab or
		// actually arriving on the site - how to tell the difference between all these activities?
		Shout shout = createShout("system", String.format("[%s]@[%s] is in the haus !", user.user, user.ip));
		Message onShout = createMessage("shoutclient", "onShout", Encoder.gson.toJson(shout));
		// out.sender = shout.user;
		webgui.sendToAll(onShout);

		// FIXME onShout which takes ARRAY of shouts !!! - send the whole thing
		// in one shot
		// UPDATE NEW CONNECTION'S DISPLAY
		for (int i = 0; i < shouts.size(); ++i) {
			Shout s = shouts.get(i);
			String ss = Encoder.gson.toJson(s);
			Message catchup = createMessage("shoutclient", "onShout", ss);
			conn.send(Encoder.gson.toJson(catchup));
		}
	}

	public void onDisconnect(WebSocket conn) {
		User user = users.getUser(conn);
		users.remove(user);
		// log.info(conn.getRemoteSocketAddress().toString()); - always null
		log.info("" + conn);
	}

	public NameProvider setNameProvider(String classname) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		Class<?> theClass = Class.forName(classname);
		nameProvider = (NameProvider) theClass.newInstance();
		return nameProvider;
	}

	public void setNameProvider(NameProvider nameProvider2) {
		nameProvider = nameProvider2;
	}

	/**
	 * fabulous new "pub"lish method from WSServer sends Websocket + Message
	 * no more need to do a preProcessHook if you "subscribe" to inbound messages
	 * @param msg
	 */
	public void onWSMsg(WSMsg wsmsg){
		++msgCount;
		// msg types individually routed here - this by design
		// in this way this service (Shoutbox) handles specific
		// routing - anything else sent by the client will be dumped
		// - direct messaging from client is not allowed (webgui.allowDirectMessaging(false))
		// Shoutbox subscibes to onWSMsg and dumps any message we don't want to handle
		if ("onShout".equals(wsmsg.msg.method)){
			onShout(wsmsg);
		} else {
			Message msg = wsmsg.msg;
			error("unAuthorized message !!! %s.%s from sender %s",msg.name, msg.method, msg.sender);
		}
	}
	
	public void onShout(WSMsg wsmsg) {
		// webgui assigns client ip to Message.sender
		Message m = wsmsg.msg;
		info("%s", m);

		String msg = (String) m.data[0];
		// sender is put in by WebGUI / WSServer
		// shout.ip = m.sender;
		User user = users.getUser(wsmsg.socket);
		
		Shout shout = (Shout) Encoder.gson.fromJson(msg, Shout.class);
		
		if (user == null){
			error("%s shouting but not found in ipToUser", m.sender);
		}
		
		// transfer data - transfer personal properties
		shout.user = user.user;
		shout.ip = user.ip;
		
		updateSystemInfo(shout);
		
		shouts.add(shout);
		Message out = createMessage("shoutclient", "onShout", Encoder.gson.toJson(shout));
		webgui.sendToAll(out);
	
		archive(shout);
	}

	private Shout updateSystemInfo(Shout shout) {
		SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
		// or SimpleDateFormat sdf = new SimpleDateFormat( "MM/dd/yyyy KK:mm:ss a Z" );
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		shout.time = sdf.format(new Date()) ;
		
		shout.connectionCount = users.getConnectionCount();
		shout.userCount = users.getUserCount();
		shout.guestCount = users.getGuestCount();
		shout.msgCount = msgCount;
		return shout;
	}

	@Override
	public String getDescription() {
		return "shoutbox server for myrobotlab";
	}

	public void test() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		Shoutbox shoutbox = (Shoutbox) Runtime.create(getName(), "Shoutbox");
		shoutbox.startService();
		shoutbox.setNameProvider("org.myrobotlab.client.DrupalNameProvider");
		webgui.allowREST(true);
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		try {

			Shoutbox shoutbox = (Shoutbox) Runtime.create("shoutbox", "Shoutbox");
			shoutbox.test();

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

}
