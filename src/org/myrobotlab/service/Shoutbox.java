package org.myrobotlab.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.java_websocket.WebSocket;
import org.jivesoftware.smack.Roster;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.fileLib.FindFile;
import org.myrobotlab.framework.Encoder;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.ProgramAB.Response;
import org.myrobotlab.service.XMPP.XMPPMsg;
import org.myrobotlab.webgui.WSServer.WSMsg;
import org.slf4j.Logger;

public class Shoutbox extends Service {

	
	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Shoutbox.class);

	transient WebGUI webgui;
	transient ProgramAB chatbot;
	
	transient XMPP xmpp;
	ArrayList<String> xmppRelays = new ArrayList<String>();

	ArrayList<String> chatbotNames = new ArrayList<String>();
	
	int imageDefaultHeight = 200;
	int imageDefaultWidth = 200;

	transient private static NameProvider nameProvider = new DefaultNameProvider();

	Integer port = 6565;

	int maxShoutsInMemory = 200;
	ArrayList<Shout> shouts = new ArrayList<Shout>();
	Users users = new Users();

	int msgCount;
	FileWriter fw = null;
	BufferedWriter bw = null;

	int maxArchiveRecordCount = 50;

	// FIXME xmpp - no system messages -> way to shutoff or turn on system messages
	// FIXME xmpp - no taling or recieving messages from mr.turing (refactor "shouts") from everyone... (anywhere shout) -> invoke "onShout" ??/
	// FIXME - way to reset user use nickname or force someone to have name
	// FIXME - Authorization & Authentication
	// FIXME - 
	// FIXME make userShout userShoutAll systemShout systemShoutAll
	// FIXME - push all security into Security !!!
	// FIXME - security WebGUI REST interace / web interface - shutdown && allow
	// only methods "onShout" allow only service "shoutbox" allow only service
	// types
	// TODO - system commands - refresh / clear / reload / history /
	// resize-format / stats / show times / set my color
	// FIXME - define client & server - system and user commands
	// TODO - number of sessions / authenticated / guests - query deeper on each
	// user - stats - geo-location
	// FIXME - permissions - erase my chat - moderate others
	// scrollable - non scrollable - set wrap - menu display - Angular.js /
	// jquery
	// levels of authorization / admin
	// hover over - display - time other (user) info
	// TODO - auto resize images
	// TODO - add modify or delete own shout
	// TODO - days alive ! - stats (poll thread - only pushes on changes)
	// TODO - force logout command
	// FIXME - color options

	public static class Users {

		HashMap<WebSocket, User> wsToUser = new HashMap<WebSocket, User>();
		HashMap<String, User> userToUser = new HashMap<String, User>();
		
		public int getConnectionCount() {
			return wsToUser.size();
		}

		public int getUserCount() {
			return userToUser.size();
		}
		
		public String[] listConnections(){			
			// INFO - not thread safe if wsToUser changes
			String [] conns = new String[wsToUser.entrySet().size()];
			int i = 0;
			
			for (Map.Entry<WebSocket, User> entry : wsToUser.entrySet()) {				
			    WebSocket ws = entry.getKey();
				User user = entry.getValue();
				conns[i] = String.format("%s@%s:%d", user.user ,ws.getRemoteSocketAddress().getAddress().getHostAddress(),ws.getRemoteSocketAddress().getPort()); 
				++i;
			}
			Arrays.sort(conns);
			return conns;
		}

		public User addUser(WebSocket ws) {
			// "real" ip address Yay! - no reverse host lookup
			String ip = ws.getRemoteSocketAddress().getAddress().getHostAddress();
			String port = ws.getRemoteSocketAddress().getPort() + "";
			// new socket - might be a user who already has a session
			// doubt if DrupalNameProvider is returning appropriate information

			// return HashMap of properties userid user# email etc ...
			String userid = nameProvider.getName(ip);
			User user = new User();

			if (wsToUser.containsKey(ws)) {
				log.error("adding Websocket which is already in index %s", ws);
			}

			// populate user with new data on the "connect"
			user.ip = ip;
			user.port = port;
			user.socket = ws;
			user.user = userid;

			wsToUser.put(ws, user);
			if (!ip.equals(userid)) {
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
			userToUser.remove(user.user);
		}

	}

	public interface NameProvider {
		String getName(String token);
	}

	/**
	 * Shout is the most common message structure being sent from client to
	 * WSServer and from WSServer broadcasted to clients - therefore instead of
	 * a seperate system message we will have system data components of the
	 * shout - these are to display server data on the clients
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
		chatbotNames.add("@mrt");
		chatbotNames.add("@mr.turing");
		chatbotNames.add("@mrturing");
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

			if (fw == null) {
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

			if (msgCount % maxArchiveRecordCount == 0) {
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
				if (latest == null) {
					latest = f;
				}
				if (f.lastModified() > latest.lastModified()) {
					latest = f;
				}
			}

			if (latest == null) {
				log.info("no files found to restore");
				return;
			}

			info("loading latest file %s", latest);

			String json = String.format("[%s]", FileIO.fileToString(latest.getAbsoluteFile()));

			Shout[] saved = Encoder.gson.fromJson(json, Shout[].class);

			for (int i = 0; i < saved.length; ++i) {
				shouts.add(saved[i]);
			}

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	// WTFU - onShout does not use this .. why??
	public Shout createShout(String type, String msg) {
		Shout shout = new Shout();
		shout.type = type;
		shout.msg = msg;

		updateSystemInfo(shout);
		return shout;
	}

	public Shout createSysetmShout(String msg) {
		return createShout("system", msg);
	}

	/**
	 * send system message to everyone
	 * 
	 * @param msg
	 */
	public void systemShout(String msg) {
		Shout shout = createShout("system", msg);
		Message onShout = createMessage("shoutclient", "onShout", Encoder.gson.toJson(shout));
		// out.sender = shout.user;
		webgui.sendToAll(onShout);
	}

	// TODO Create User INFO & INDEXES HERE
	public void onConnect(WebSocket conn) {
		log.info(conn.getRemoteSocketAddress().toString());

		// set javascript user object for this connection
		User user = users.addUser(conn);
		Message onConnect = createMessage("shoutclient", "onConnect", Encoder.gson.toJson(user));
		conn.send(Encoder.gson.toJson(onConnect));// .sendToAll(onConnect);

		// BROADCAST ARRIVAL
		// TODO - broadcast to others new connection of user - (this mean's user
		// has established new connection,
		// this could be refreshing the page, going to a different page, opening
		// a new tab or
		// actually arriving on the site - how to tell the difference between
		// all these activities?
		systemShout(String.format("[%s]@[%s] is in the haus !", user.user, user.ip));

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
	 * fabulous new "pub"lish method from WSServer sends Websocket + Message no
	 * more need to do a preProcessHook if you "subscribe" to inbound messages
	 * 
	 * @param msg
	 */
	public void onWSMsg(WSMsg wsmsg) {
		++msgCount;
		// msg types individually routed here - this by design
		// in this way this service (Shoutbox) handles specific
		// routing - anything else sent by the client will be dumped
		// - direct messaging from client is not allowed
		// (webgui.allowDirectMessaging(false))
		// Shoutbox subscibes to onWSMsg and dumps any message we don't want to
		// handle
		if ("onShout".equals(wsmsg.msg.method)) {
			onShout(wsmsg);
		} else {
			Message msg = wsmsg.msg;
			error("unAuthorized message !!! %s.%s from sender %s", msg.name, msg.method, msg.sender);
		}
	}

	private Shout updateSystemInfo(Shout shout) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		// or SimpleDateFormat sdf = new SimpleDateFormat(
		// "MM/dd/yyyy KK:mm:ss a Z" );
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		shout.time = sdf.format(new Date());

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

	public void onShout(WSMsg wsmsg) {
		// webgui assigns client ip to Message.sender
		Message m = wsmsg.msg;
		info("%s", m);

		String msg = (String) m.data[0];
		// sender is put in by WebGUI / WSServer
		// shout.ip = m.sender;
		User user = users.getUser(wsmsg.socket);

		Shout shout = (Shout) Encoder.gson.fromJson(msg, Shout.class);

		if (user == null) {
			error("%s shouting but not found in ipToUser", m.sender);
		}

		// transfer data - transfer personal properties
		shout.user = user.user;
		shout.ip = user.ip;

		updateSystemInfo(shout);

		// starts with "/" is a system message
		if (shout.msg.startsWith("/")) {
			log.info("system message");

			// Object ret = null;
			
			String [] params =  shout.msg.split("/");

			if (shout.msg.startsWith("/startChatBot")) {
				invoke("startChatBot");
				return;
			}
			
			if (shout.msg.startsWith("/listConnections")) {
				invoke("listConnections", wsmsg);
				return;
			}
			
			if (shout.msg.startsWith("/startXMPP")) {
				invoke("startXMPP", params[2], params[3]);
				return;
			}
			
			if (shout.msg.startsWith("/addXMPPRelay")) {
				invoke("addXMPPRelay", params[2]);
				return;
			}
			
			if (shout.msg.startsWith("/i")){
				String src = params[2];
				shout.msg = String.format("<a href=\"%s\"><img src=\"%s\" width=\"%d\" height=\"%d\"/></a>", src, src, imageDefaultWidth, imageDefaultHeight);
			}
		}
		
		// more general contains 
		

		String foundName = findChatBotName(shout.msg);
		if (foundName != null) {
			chatWithChatbot(foundName, shout);
		}

		shouts.add(shout);
		Message out = createMessage("shoutclient", "onShout", Encoder.gson.toJson(shout));
		webgui.sendToAll(out);
		
		if (xmpp != null){
			for (int i = 0; i < xmppRelays.size(); ++i){
				log.info(String.format("sending xmpp client %s %s",shout.user, shout.msg));
				xmpp.sendMessage(String.format("%s:%s", shout.user, shout.msg), xmppRelays.get(i));
			}
		}

		archive(shout);
	}
	
	public void listConnections(WSMsg wsmsg){
		log.info("listConnections");
		
		Shout shout = createShout("system", Encoder.gson.toJson(users.listConnections()));
		Message listUsers = createMessage("shoutclient", "onShout", Encoder.gson.toJson(shout));
				
		//Message catchup = createMessage("shoutclient", "onShout", Encoder.gson.toJson(users.listConnections()));
		wsmsg.socket.send(Encoder.gson.toJson(listUsers));
	}
	

	public String findChatBotName(String msg) {
		for (String name : chatbotNames) {
			if (msg.contains(name)) {
				return name;
			}
		}
		return null;
	}

	private void chatWithChatbot(String foundName, Shout shout) {
		// clean found name - we don't want to send @mrt etc to Alice 2.0 
		String msg = shout.msg.replace(foundName, "");
		chatbot.getResponse(shout.user, msg);
	}

	public void startChatBot() {
		if (chatbot != null){
			error("chatbot already started");
			return;
		}
		chatbot = (ProgramAB) Runtime.start("chatbot", "ProgramAB");
		chatbot.startSession("ProgramAB", "alice2");
		chatbot.addResponseListener(this);
	}
	
	private String resizeImage(String shout){
		int x = shout.indexOf("<img");
		if (x > 0){
			int space = shout.indexOf(" ", x);
			int endTag = shout.indexOf(">", x);
			int insert = (space < endTag)?space:endTag;
			String r = String.format("%s width=%d height=%d %s", shout.substring(0, insert), imageDefaultWidth, imageDefaultHeight, shout.substring(insert));
			log.info(String.format("=========== RESIZE ============ %s", r));
		}
		
		return shout;
	}

	// FIXME - refactor ---(all msgs from non websockets e.g. chatbot | xmpp | other --to--> websockets
	// FIXME - onChatBotResponse
	// onProgramAB response - onChatBotResponse ???
	public Response onResponse(Response response) {
		log.info("chatbot shouting");
		
		String r = resizeImage(response.msg);
		
		Shout shout = createShout("usermsg", r);
		shout.user = "mr.turing";

		shouts.add(shout);
		Message out = createMessage("shoutclient", "onShout", Encoder.gson.toJson(shout));
		webgui.sendToAll(out);
		archive(shout);

		return response;
	}
	
	// --------- XMPP BEGIN ------------
	public boolean startXMPP(String user, String password){
		if (xmpp == null){
			xmpp = (XMPP)Runtime.start("xmpp", "XMPP");
		}
		xmpp.connect(user, password);
		if (xmpp.connect(user, password)){
			xmpp.addXMPPMsgListener(this);
			return true;
		} else { 
			return false;
		}
		
	}
	
	public void onXMPPMsg(XMPPMsg xmppMsg){
		log.info(String.format("XMPP - %s %s", xmppMsg.msg.getFrom(), xmppMsg.msg.getBody()));
		
		//String r = resizeImage(response.msg);
		
		Shout shout = createShout("usermsg", xmppMsg.msg.getBody());
		shout.user = "mr.turing";

		shouts.add(shout);
		Message out = createMessage("shoutclient", "onShout", Encoder.gson.toJson(shout));
		webgui.sendToAll(out);
		archive(shout);
	}
	
	public boolean stopXMPP(){
		if (xmpp != null){
			xmpp.disconnect();
			xmpp.releaseService();
			xmpp = null;
			return true;
		}
		return false;
	}
	
	public Roster getXMPPRoster(){
		return xmpp.getRoster();
	}
	
	public String addXMPPRelay(String user){
		xmppRelays.add(user);
		xmpp.sendMessage("now shoutbox relay", user);
		return user;
	}
	
	public String removeXMPPRelay(String user){
		xmppRelays.remove(user);
		return user;
	}
	
	public ArrayList<String> getXMPPRelays(){
		return xmppRelays;
	}
	
	// add relay - is a subset of the XMPP roster
	/*
	public boolean removeXMPPClient(String user){
		if (xmppClients.containsKey(user)){
			XMPP xmpp = xmppClients.get(user);
			xmpp.releaseService();
			xmppClients.remove(user);
			return true;
		}
		return false;
	}
	*/
	// --------- XMPP END ------------
	public void test() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		Shoutbox shoutbox = (Shoutbox) Runtime.create(getName(), "Shoutbox");
		shoutbox.startService();
		shoutbox.setNameProvider("org.myrobotlab.client.DrupalNameProvider");
		webgui.allowREST(true);
		//shoutbox.startXMPP("incubator@myrobotlab.org", "xxxxxx");
		//shoutbox.addXMPPRelay("Greg Perry");
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
