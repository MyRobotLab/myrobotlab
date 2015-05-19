package org.myrobotlab.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import org.jivesoftware.smack.Roster;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.fileLib.FindFile;
import org.myrobotlab.framework.Encoder;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.ProgramAB.Response;
import org.myrobotlab.service.WebGUI.WebMsg;
import org.myrobotlab.service.XMPP.XMPPMsg;
import org.slf4j.Logger;

public class Shoutbox extends Service {
	public static class Connection implements Serializable {
		private static final long serialVersionUID = 1L;
		public String user;
		public String ip; // used ???
		public String port;
		public String color;

		// connectivity
		transient public String clientID; // FIXME - this is most recent websocket
										// - loses other connections
		public String xmpp;
		public boolean xmppSystemMsgs = false;
	}

	// key to ---> User???
	/*
	static public class Connections implements Serializable {
		private static final long serialVersionUID = 1L;
		HashMap<WebSocket, String> wsToKey = new HashMap<WebSocket, String>();
		HashMap<String, Connection> keyToConn = new HashMap<String, Connection>();

		// HashMap<String, Integer> userConnCnts = new HashMap<String,
		// Integer>();

		// handles xmpp - our reference is always a jabberID
		public Connection addConnection(String jabberID, String userid) {
			if (!keyToConn.containsKey(jabberID)) {
				Connection conn = new Connection();
				conn.user = userid;
				conn.xmpp = jabberID;
				keyToConn.put(jabberID, conn);
				return conn;
			} else {
				return keyToConn.get(jabberID);
			}
		}

		public Connection addConnection(WebSocket ws) {
			// "real" ip address Yay! - no reverse host lookup
			String ip = ws.getRemoteSocketAddress().getAddress().getHostAddress();
			String port = ws.getRemoteSocketAddress().getPort() + "";
			// new socket - might be a user who already has a session
			// doubt if DrupalNameProvider is returning appropriate information

			// return HashMap of properties userid user# email etc ...
			String userid = nameProvider.getName(ip);
			Connection conn = new Connection();

			if (keyToConn.containsKey(ws)) {
				log.error("adding Websocket which is already in index %s", ws);
			}

			// populate user with new data on the "connect"
			// FIXME - change user.ip & port to user.key

			conn.ip = ip;
			conn.port = port;
			conn.clientID = ws;
			conn.user = userid;

			keyToConn.put(makeKey(ws), conn);
			wsToKey.put(ws, makeKey(ws));
			if (!ip.equals(userid)) {// / ??
				// userToUser.put(userid, user);
			}
			return conn;
		}

		public Connection getConnection(String key) {
			// String key = makeKey(ws);
			if (keyToConn.containsKey(key)) {
				return keyToConn.get(key);
			}
			return null;
		}

		public int getConnectionCount() {
			return keyToConn.size();
		}

		public int getGuestCount() {
			return getConnectionCount() - getUserCount();
		}

		// FIXME - finish
		public HashMap<String, Integer> getUserConnCnts() {
			HashMap<String, Integer> ret = new HashMap<String, Integer>();

			for (Map.Entry<String, Connection> entry : keyToConn.entrySet()) {
				// WebSocket ws = entry.getKey();
				Connection conn = entry.getValue();

				// user is in key - get it out and count it
				if (conn.clientID != null) {

				} else if (conn.xmpp != null) {

				} else {
					// mr.turing .. ???
				}
			}

			return ret;

		}

		public int getUserCount() {
			// FIXME
			// should do a "reduce"
			// but if that is the case - normalize users accross ws & xmpp :P
			// return userToUser.size();
			return 7;
		}

		public String[] listConnections() {
			// INFO - not thread safe if wsToUser changes
			String[] conns = new String[keyToConn.entrySet().size()];
			int i = 0;

			for (Map.Entry<String, Connection> entry : keyToConn.entrySet()) {
				// WebSocket ws = entry.getKey();
				Connection conn = entry.getValue();
				conns[i] = String.format("%s@%s", conn.user, entry.getKey());
				++i;
			}
			Arrays.sort(conns);
			return conns;
		}

		public void remove(String jabberID) {
			log.info(String.format("remove %s", jabberID));
			String match = null;
			for (Map.Entry<String, Connection> o : keyToConn.entrySet()) {
				if (o.getKey().startsWith(jabberID)) {
					match = o.getKey();
				}
			}
			if (match != null) {
				keyToConn.remove(match);
				log.info(String.format("removed %s", match));
			} else {
				log.error(String.format("remove %s not found", jabberID));
			}
		}

		public void remove(WebSocket ws) {
			String key = wsToKey.get(ws);
			keyToConn.remove(key);
			// userToUser.remove(user.user);
		}

	} // end class Connections
	
	*/

	// FIXME - do not allow double entries on quickStart - make re-entrant
	// FIXME if link = youtube.com - then embedd (at least with hyperlink &
	// video splash
	// FIXME - refactor these are ugly

	public static class DefaultNameProvider implements NameProvider {
		@Override
		public String getName(String token) {
			return token;
		}
	}

	public interface NameProvider {
		String getName(String token);
	}

	/**
	 * POJO Shout is the most common message structure being sent from client to
	 * WSServer and from WSServer broadcasted to clients - therefore instead of
	 * a seperate system message we will have system data components of the
	 * shout - these are to display server data on the clients
	 */
	public static class Shout implements Serializable {
		private static final long serialVersionUID = 1L;
		public String from;
		public String type;
		public String msg;
		public String color;
		public String ip; // TODO change to key

		public String time;

		// system related
		public int connectionCount;
		public int userCount;
		public int guestCount;
		public int msgCount;

	}

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Shoutbox.class);

	static final public String TYPE_SYSTEM = "TYPE_SYSTEM";

	static final public String TYPE_USER = "TYPE_USER";

	static final public String ORGIN_XMPP = "ORGIN_XMPP";
	static final public String ORGIN_WEB = "ORGIN_WEB";

	transient WebGUI webgui;

	transient ProgramAB chatbot;
	transient XMPP xmpp;

	transient ArrayList<String> xmppRelays = new ArrayList<String>();

	transient ArrayList<String> chatbotNames = new ArrayList<String>();

	transient HashMap<String, String> aliases = new HashMap<String, String>();
	int imageDefaultHeight = 200;
	int imageDefaultWidth = 200;

	transient static NameProvider nameProvider = new DefaultNameProvider();
	Integer port = 6565;
	int maxShoutsInMemory = 200;

	ArrayList<Shout> shouts = new ArrayList<Shout>();

	// FIXME - the amount of methods you DONT want exposed will be dwarfed by
	// the number you do - So, Security
	// will need to wildcard or list a filter of excludes

	// FIXME - standard interfaces for all GATEWAY SERVICES - onMsg()
	// addListener()
	// FIXME - Ma. Vo. name link on shoutbox
	// FIXME - decoding or encoing on specific GATEWAY Interface - e.g. ws or
	// xmpp
	// FIXME - login, Security, Authentication & Authorization done through the
	// Security service - restrictions only at Gateway
	// FIXME - impersonate mr.turing ?

	// FIXME make userShout userShoutAll systemShout systemShoutAll

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

	//transient Connections conns = new Connections();
	
	HashMap<String, Object> clients = new HashMap<String, Object>();

	int msgCount;

	transient FileWriter fw = null;

	transient BufferedWriter bw = null;

	int maxArchiveRecordCount = 50;

	// if new socket & recently closed socket of user - then
	// "dwilli is on the move !"

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);
		peers.put("webgui", "WebGUI", "webgui");
		return peers;
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		try {

			Shoutbox shoutbox = (Shoutbox) Runtime.create("shoutbox", "Shoutbox");
			shoutbox.test();

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	/**
	 * Core to managing the connections are the keys The keys for websockets are
	 * defined as remoteIp:remotePort - unfortunately these are null on
	 * disconnect so a seperate lookup needs to be utilized The keys for xmpp
	 * "buddies" are simply their jabber ids
	 * 
	 * A Connection's UserId is a "user friendly" identification of the user
	 * using that connection
	 * 
	 * @param ws
	 * @return
	 */

	static public String makeKey(String ws) {
		return String.format("%s:%s", ws, ws);
	}

	public Shoutbox(String n) {
		super(n);
		chatbotNames.add("@mrt");
		chatbotNames.add("@mr.turing");
		chatbotNames.add("@mrturing");
	}

	public String addXMPPRelay(String user) {
		xmppRelays.add(user);
		xmpp.sendMessage("now shoutbox relay", user);
		return user;
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

				String d = String.format("%s", Encoder.toJson(shout));
				bw.write(d);
				return;
			}

			String d = String.format(",%s", Encoder.toJson(shout));
			bw.write(d);
			bw.flush();

			if (msgCount % maxArchiveRecordCount == 0) {
				close(bw);
				fw = null;
				bw = null;
			}

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	private void chatWithChatbot(String foundName, Shout shout) {
		// clean found name - we don't want to send @mrt etc to Alice 2.0
		String msg = shout.msg.replace(foundName, "");
		chatbot.getResponse(shout.from, msg);
	}

	// WTFU - onShout does not use this .. why??
	public Shout createShout(String type, String msg) {
		Shout shout = new Shout();
		shout.type = type;
		shout.msg = msg;

		updateSystemInfo(shout);
		return shout;
	}

	public String findChatBotName(String msg) {
		for (String name : chatbotNames) {
			if (msg.contains(name)) {
				return name;
			}
		}
		return null;
	}

	@Override
	public String[] getCategories() {
		return new String[] { "connectivity" };
	}

	@Override
	public String getDescription() {
		return "shoutbox server for myrobotlab";
	}

	public void getXMPPRelays() {
		Shout shout = createShout(TYPE_USER, Arrays.toString(xmppRelays.toArray()));
		shout.from = "mr.turing";
		Message out = createMessage("shoutclient", "onShout", Encoder.toJson(shout));
		onShout("mr.turing", out);
	}

	public Roster getXMPPRoster() {
		return xmpp.getRoster();
	}

	/*
	public void listConnections(String key) {
		log.info("listConnections");
		sendTo(TYPE_SYSTEM, key, conns.listConnections());
	}
	*/

	public void mimicTuring(String msg) {
		Shout shout = createShout(TYPE_USER, msg);
		shout.from = "mr.turing";
		Message out = createMessage("shoutclient", "onShout", Encoder.toJson(shout));
		onShout("mr.turing", out);
	}
	
	/** FIXME - DON'T KNOW IF WE CAN GET THE onConnect event in Nettosphere - probably can

	// TODO Create User INFO & INDEXES HERE
	public void onConnect(WebSocket ws) {
		try {
			if (ws == null || ws.getRemoteSocketAddress() == null) {
				error("ws or getRemoteSocketAddress() == null");
				return;
			}
			log.info(ws.getRemoteSocketAddress().toString());

			// set javascript user object for this connection
			Connection conn = conns.addConnection(ws);
			Message onConnect = createMessage("shoutclient", "onConnect", Encoder.toJson(conn));
			ws.send(Encoder.toJson(onConnect));

			// BROADCAST ARRIVAL
			// TODO - broadcast to others new connection of user - (this mean's
			// user
			// has established new connection,
			// this could be refreshing the page, going to a different page,
			// opening
			// a new tab or
			// actually arriving on the site - how to tell the difference
			// between
			// all these activities?
			systemBroadcast(String.format("[%s]@[%s] is in the haus !", conn.user, conn.ip));

			// FIXME onShout which takes ARRAY of shouts !!! - send the whole
			// thing
			// in one shot
			// UPDATE NEW CONNECTION'S DISPLAY
			for (int i = 0; i < shouts.size(); ++i) {
				Shout s = shouts.get(i);
				String ss = Encoder.toJson(s);
				Message catchup = createMessage("shoutclient", "onShout", ss);
				ws.send(Encoder.toJson(catchup));
			}
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public void onDisconnect(WebSocket ws) {
		conns.remove(ws);
		info("onDisconnect %s", ws);
	}
	*/

	// FIXME - refactor ---(all msgs from non websockets e.g. chatbot | xmpp |
	// other --to--> websockets
	// FIXME - onChatBotResponse
	// onProgramAB response - onChatBotResponse ???
	public Response onResponse(Response response) {
		log.info("chatbot shouting");

		// String r = resizeImage(response.msg);
		String r = response.msg;

		//conns.addConnection("mr.turing", "mr.turing");

		Shout shout = createShout(TYPE_USER, r);
		shout.from = "mr.turing";
		Message out = createMessage("shoutclient", "onShout", Encoder.toJson(shout));
		onShout("mr.turing", out);
		return response;
	}

	// EXCHANGE need "session-key" to do a - connection/session-key for user
	// FIXME NOT NORMALIZED with onXMPPMsg() !!!!
	// public void onShout(WSMsg wsmsg) { is Message necessary here?
	public void onShout(String key, Message m) {
		log.info(String.format("onShout %s %s", key, m));

		String msg = (String) m.data[0];

		// Could cause errors in control or monitoring
		// but in xmpp you can't "overwrite" a message
		// as you can in the ws shoutbox
		// correct way would be to establish a session monitoring thread
		/*
		 * if (msg.equals(lastShoutMsg)){ log.info("we don't like to repeat"); }
		 */
		// sender is put in by WebGUI / WSServer
		// shout.ip = m.sender;
		Connection conn = null;// = conns.getConnection(key);

		Shout shout = Encoder.fromJson(msg, Shout.class);

		if (conn == null) {
			info("conn/User is null - better be a system msg");
		} else {
			// transfer data - transfer personal properties
			shout.from = conn.user;
			shout.ip = conn.ip;
		}

		updateSystemInfo(shout);

		// starts with "/" is a system message
		if (shout.msg.startsWith("/")) {
			log.info("system message");

			// Object ret = null;

			String[] params = shout.msg.split("/");

			// FIXME - reply with string "system msgs are now off/on"
			if (shout.msg.startsWith("/system")) {
				String onOff = params[2];
				if ("on".startsWith(onOff.toLowerCase())) {
					conn.xmppSystemMsgs = true;
				} else if ("off".startsWith(onOff.toLowerCase())) {
					conn.xmppSystemMsgs = false;
				} else {
					log.error("unkown param {}", onOff);
				}
				return;
			}

			if (shout.msg.startsWith("/startChatBot")) {
				invoke("startChatBot");
				return;
			}

			if (shout.msg.startsWith("/listConnections")) {
				invoke("listConnections", key);
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

			if (shout.msg.startsWith("/removeXMPPRelay")) {
				invoke("removeXMPPRelay", params[2]);
				return;
			}

			if (shout.msg.startsWith("/getXMPPRoster")) {
				invoke("getXMPPRoster");
				return;
			}

			if (shout.msg.startsWith("/getXMPPRelays")) {
				invoke("getXMPPRelays");
				return;
			}

			if (shout.msg.startsWith("/quickStart")) {
				invoke("quickStart", params[2], params[3]);
				return;
			}

			if (shout.msg.startsWith("/getUptime")) {
				systemBroadcast(Runtime.getUptime());
				return;
			}

			if (shout.msg.startsWith("/t")) {
				invoke("mimicTuring", params[2]);
				return;
			}

			if (shout.msg.startsWith("/v")) {
				// invoke("version");
				// FIXME - since your filtering specifically which functions to
				// do
				// we should call directly so compiling will flush out method
				// signature mis-matches
				// FIXME - BUT CALLING DIRECTLY DOES NOT PUT IT ON THE "PUB"
				// SIDE OF FRAMEWORK !!!
				// IS ONSHOUT THE ONLY PUB/SUB ALLOWED ?
				version(params[2]);
				return;
			}

			/*
			 * YOU THINK THIS WILL WORK > HAHAHAHA !
			 * /i/http://crossfitlosgatos.com
			 * /wp-content/uploads/2013/05/coffee-black.jpg
			 * 
			 * if (shout.msg.startsWith("/i")){ String src = params[2];
			 * shout.msg = String.format(
			 * "<a href=\"%s\"><img src=\"%s\" width=\"%d\" height=\"%d\"/></a>"
			 * , src, src, imageDefaultWidth, imageDefaultHeight); }
			 */
		}

		// more general contains

		String foundName = findChatBotName(shout.msg);
		if (foundName != null) {
			chatWithChatbot(foundName, shout);
		}

		shouts.add(shout);
		Message out = createMessage("shoutclient", "onShout", Encoder.toJson(shout));
		//webgui.sendToAll(out);

		if (xmpp != null && !TYPE_SYSTEM.equals(shout.type)) {
			for (int i = 0; i < xmppRelays.size(); ++i) {
				String relayName = xmppRelays.get(i);
				String jabberID = xmpp.getJabberID(relayName);
				// don't echo to self
				// if (!key.startsWith(jabberID)) { filter took out mrt and
				// other activity !
				log.info(String.format("sending from %s %s -> to xmpp client - relayName [%s] jabberID [%s] shout.msg [%s]", Thread.currentThread().getId(), shout.from, relayName,
						jabberID, shout.msg));
				xmpp.sendMessage(String.format("%s: %s", shout.from, shout.msg), jabberID);
				// }
			}
		}

		archive(shout);
	}

	/**
	 * fabulous new "pub"lish method from WSServer sends Websocket + Message no
	 * more need to do a preProcessHook if you "subscribe" to inbound messages
	 * 
	 * @param msg
	 */
	public void onWSMsg(WebMsg wsmsg) {
		++msgCount;
		// msg types individually routed here - this by design
		// in this way this service (Shoutbox) handles specific
		// routing - anything else sent by the client will be dumped
		// - direct messaging from client is not allowed
		// (webgui.allowDirectMessaging(false))
		// Shoutbox subscibes to onWSMsg and dumps any message we don't want to
		// handle
		if ("onShout".equals(wsmsg.msg.method)) {
			onShout(makeKey(wsmsg.clientid), wsmsg.msg);
		} else {
			Message msg = wsmsg.msg;
			error("unAuthorized message !!! %s.%s from sender %s", msg.name, msg.method, msg.sender);
		}
	}

	// FIXME FIXME FIXME - not normalized with onShout(WebSocket) :PPPP
	// FIXME - must fill in your name - "Greg Perry" somewhere..
	public void onXMPPMsg(XMPPMsg xmppMsg) {
		log.info(String.format("XMPP - %s %s", xmppMsg.msg.getFrom(), xmppMsg.msg.getBody()));

		// not exactly the same model as onConnect - so we try to add each time
		String user = xmpp.getEntry(xmppMsg.msg.getFrom()).getName();
		//conns.addConnection(xmppMsg.msg.getFrom(), user);

		Shout shout = createShout(TYPE_USER, xmppMsg.msg.getBody());
		shout.from = user;

		// shouts.add(shout);
		Message out = createMessage("shoutclient", "onShout", Encoder.toJson(shout));

		onShout(xmppMsg.msg.getFrom(), out);

		/*
		 * Shout shout = createShout(TYPE_USER, xmppMsg.msg.getBody());
		 * shout.user = user;
		 * 
		 * shouts.add(shout); Message out = createMessage("shoutclient",
		 * "onShout", Encoder.toJson(shout)); webgui.sendToAll(out); if (xmpp !=
		 * null){ for (int i = 0; i < xmppRelays.size(); ++i){
		 * log.info(String.format("sending xmpp client %s %s",shout.user,
		 * shout.msg)); xmpp.sendMessage(String.format("%s:%s", shout.user,
		 * shout.msg), xmppRelays.get(i)); } } archive(shout);
		 */
	}

	public void quickStart(String xmpp, String password) {
		startXMPP(xmpp, password);
		startChatBot();

		addXMPPRelay("Keith McGerald");
		aliases.put("Keith McGerald", "kmcgerald");
		// addXMPPRelay("Orbous Mundus");
		// addXMPPRelay("Alessandro Didonna");
		// addXMPPRelay("Dwayne Williams");
		// addXMPPRelay("Aatur Mehta");
		addXMPPRelay("Greg Perry");
		aliases.put("Greg Perry", "GroG");

		addLocalTask(30 * 60 * 1000, "savePredicates");
	}

	public String removeXMPPRelay(String user) {
		xmppRelays.remove(user);
		//conns.remove(xmpp.getJabberID(user));
		return user;
	}

	private String resizeImage(String shout) {
		int x = shout.indexOf("<img");
		if (x > 0) {
			int space = shout.indexOf(" ", x);
			int endTag = shout.indexOf(">", x);
			int insert = (space < endTag) ? space : endTag;
			String r = String.format("%s width=%d height=%d %s", shout.substring(0, insert), imageDefaultWidth, imageDefaultHeight, shout.substring(insert));
			log.info(String.format("=========== RESIZE ============ %s", r));
		}

		return shout;
	}

	/**
	 * archiving restores last json file back into newly started shoutbox
	 */
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

			Shout[] saved = Encoder.fromJson(json, Shout[].class);

			for (int i = 0; i < saved.length; ++i) {
				shouts.add(saved[i]);
			}

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public void savePredicates() {
		try {
			log.info("saving Predicates");
			if (chatbot != null) {
				chatbot.savePredicates();
			}
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public void sendTo(String type, String key, Object data) {
		Shout shout = createShout(TYPE_SYSTEM, Encoder.toJson(data));
		String msgString = Encoder.toJson(shout);
		Message sendTo = createMessage("shoutclient", "onShout", msgString);

		//Connection conn = conns.getConnection(key);
		Connection conn = null;
		if (conn == null) {
			error("sendTo conn key %s - conn not found", key);
			return;
		}

		if (conn.clientID != null) {
			// specialized formatting here
			//conn.clientID.send(Encoder.toJson(sendTo));
		} else if (conn.xmpp != null) {
			// specialized formatting here
			xmpp.sendMessage(msgString, conn.xmpp);
		}
		// Message catchup = createMessage("shoutclient", "onShout",
		// Encoder.toJson(users.listConnections()));

	}

	// --------- XMPP END ------------

	// String lastShoutMsg = null;

	public void setNameProvider(NameProvider nameProvider2) {
		nameProvider = nameProvider2;
	}

	public NameProvider setNameProvider(String classname) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		Class<?> theClass = Class.forName(classname);
		nameProvider = (NameProvider) theClass.newInstance();
		return nameProvider;
	}

	public void startChatBot() {
		if (chatbot != null) {
			error("chatbot already started");
			return;
		}
		chatbot = (ProgramAB) Runtime.start("chatbot", "ProgramAB");
		chatbot.startSession("ProgramAB", "alice2");
		chatbot.addResponseListener(this);
	}

	@Override
	public void startService() {
		super.startService();
		if (webgui == null) {
			webgui = (WebGUI) createPeer("webgui");
			//webgui.setPort(port);
			webgui.startService();

			// subscribe to events
			//webgui.addConnectListener(this);
			//webgui.addDisconnectListener(this);
			//webgui.addWSMsgListener(this);
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
		//webgui.allowREST(false);
		// no direct messaging - for security
		//webgui.allowDirectMessaging(false);
		// FIXME - resource processor is not necessary either !!

		// webgui.startWebSocketServer(port);
		// FIXME - netty websocket server
		// publishMsg --> onMsg
		// webgui.addMsgListener(this);
		restore();
	}

	// --------- XMPP BEGIN ------------
	public boolean startXMPP(String user, String password) {
		if (xmpp == null) {
			xmpp = (XMPP) Runtime.start("xmpp", "XMPP");
		}
		xmpp.connect(user, password);
		if (xmpp.connect(user, password)) {
			xmpp.addXMPPMsgListener(this);
			return true;
		} else {
			return false;
		}

	}

	// ---- outbound ---->

	@Override
	public void stopService() {
		super.stopService();
		if (webgui != null) {
			webgui.stopService();
		}
	}

	public boolean stopXMPP() {
		if (xmpp != null) {
			xmpp.disconnect();
			xmpp.releaseService();
			xmpp = null;
			return true;
		}
		return false;
	}

	// fixme (from whom) ?? - websocket xmpp other ??
	public void systemBroadcast(Object inData) {
		String data = Encoder.toJson(inData);
		Shout shout = createShout(TYPE_SYSTEM, data);
		Message onShout = createMessage("shoutclient", "onShout", Encoder.toJson(shout));
		onShout(null, onShout);
	}

	@Override
	public Status test() {
		Status status = super.test();
		try {
			Shoutbox shoutbox = (Shoutbox) Runtime.create(getName(), "Shoutbox");
			shoutbox.startService();
			shoutbox.setNameProvider("org.myrobotlab.client.DrupalNameProvider");
			//webgui.allowREST(true);
			// shoutbox.startXMPP("incubator@myrobotlab.org", "xxxxxx");
			// shoutbox.addXMPPRelay("Greg Perry");
		} catch (Exception e) {
			status.addError(e);
		}
		return status;
	}

	private Shout updateSystemInfo(Shout shout) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		// or SimpleDateFormat sdf = new SimpleDateFormat(
		// "MM/dd/yyyy KK:mm:ss a Z" );
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		shout.time = sdf.format(new Date());

		// shout.connectionCount = conns.getConnectionCount();
		// shout.userCount = conns.getUserCount();
		// shout.guestCount = conns.getGuestCount();
		shout.msgCount = msgCount;
		return shout;
	}

	public void version(String connId) {
		//sendTo(TYPE_SYSTEM, connId, conns.listConnections());
		Shout shout = createShout(TYPE_USER, Runtime.getVersion());
		shout.from = "mr.turing";
		Message out = createMessage("shoutclient", "onShout", Encoder.toJson(shout));
		onShout("mr.turing", out);
	}

}
