package org.myrobotlab.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.myrobotlab.framework.Encoder;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceEnvironment;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.Connection;
import org.myrobotlab.service.RemoteAdapter.CommOptions;
import org.myrobotlab.service.interfaces.Gateway;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

public class XMPP extends Service implements Gateway, MessageListener {

	// GOOD ! - bundle message in single object for event return
	public static class XMPPMsg {
		public Chat chat;
		public Message msg;

		public XMPPMsg(Chat chat, Message msg) {
			this.chat = chat;
			this.msg = msg;
		}
	}

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(XMPP.class.getCanonicalName());
	static final int packetReplyTimeout = 500; // millis

	// FIXME - sendMsg onMsg getMsg - GLOBAL INTERFACE FOR GATEWAYS
	// FIXME - handle multiple user accounts

	// not sure how to initialize requirements .. probably a register Security
	// event
	// thread safe ???

	private String defaultPrefix;

	HashMap<String, String> xmppSecurity = new HashMap<String, String>();

	String user;
	String password;
	String hostname = "talk.google.com";
	int port = 5222;
	String service = "gmail.com"; // defaulted :P

	transient ConnectionConfiguration config;
	transient XMPPConnection connection;
	transient ChatManager chatManager;

	transient Roster roster = null;

	transient HashMap<String, RosterEntry> idToEntry = new HashMap<String, RosterEntry>();

	/**
	 * auditors chat buddies who can see what commands are being processed and
	 * by who through the XMPP service TODO - audit full system ??? regardless
	 * of message origin?
	 */
	// FIXME ?? - change to HashMap<String, RosterEntry>
	HashSet<String> auditors = new HashSet<String>();
	// HashSet<String> responseRelays = new HashSet<String>();
	HashSet<String> allowCommandsFrom = new HashSet<String>();
	transient HashMap<String, Chat> chats = new HashMap<String, Chat>();

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			int i = 1;
			Runtime.main(new String[] { "-runtimeName", String.format("r%d", i) });
			XMPP xmpp1 = (XMPP) Runtime.createAndStart(String.format("xmpp%d", i), "XMPP");
			Runtime.createAndStart(String.format("clock%d", i), "Clock");
			Runtime.createAndStart(String.format("gui%d", i), "GUIService");
			xmpp1.connect("talk.google.com", 5222, "incubator@myrobotlab.org", "xxxxxxx");
			xmpp1.addAuditor("Ma. Vo.");
			xmpp1.sendMessage("Ma. Vo. - xmpp test", "Ma. Vo.");
			// xmpp1.send("Ma. Vo.", "xmpp test");
			// xmpp1.sendMessage("hello from incubator by name " +
			// System.currentTimeMillis(), "Greg Perry");
			xmpp1.sendMessage("xmpp 2", "robot02 02");
			if (true) {
				return;
			}

		} catch (Exception e) {
			Logging.logError(e);
		}

	}

	public XMPP(String n) {
		super(n);
		// defaultPrefix = n;
	}

	public boolean addAuditor(String id) {
		RosterEntry entry = getEntry(id);
		if (entry == null) {
			error("can not add auditor %s", id);
			return false;
		}
		String jabberID = entry.getUser();
		auditors.add(jabberID);
		broadcast(String.format("added buddy %s", entry.getName()));
		return true;
	}

	// FIXME normalize with all gateways?
	@Override
	public void addConnectionListener(String name) {
		// TODO Auto-generated method stub

	}

	public void addXMPPMsgListener(Service service) {
		addListener("publishXMPPMsg", service.getName(), "onXMPPMsg", XMPPMsg.class);
	}

	/**
	 * broadcast a chat message to all buddies in the relay
	 * 
	 * @param text
	 *            - text to broadcast
	 */
	public void broadcast(String text) {
		for (String buddy : auditors) {
			sendMessage(text, buddy);
		}
	}

	public boolean connect() {

		try {

			if (config == null) {
				SASLAuthentication.supportSASLMechanism("PLAIN");
				// SASLAuthentication.registerSASLMechanism("DIGEST-MD5",
				// SASLDigestMD5Mechanism.class);
				// SASLAuthentication.supportSASLMechanism("DIGEST-MD5", 0);
				// WTF is a service name ?
				// ConnectionConfiguration config = new
				// ConnectionConfiguration(SERVER_HOST, SERVER_PORT);
				// ConnectionConfiguration config = new
				// ConnectionConfiguration("talk.google.com", 5222,
				// "gmail.com");
				// config.setTruststoreType("BKS");
				// TODO - look for security keys "myName" user & myName password
				config = new ConnectionConfiguration(hostname, 5222, "gmail.com");
			}

			if (connection == null || !connection.isConnected()) {

				log.info(String.format("%s new connection to %s:%d", getName(), hostname, port));
				connection = new XMPPConnection(config);
				connection.connect();
				log.info(String.format("%s connected %s", getName(), connection.isConnected()));
				chatManager = connection.getChatManager();

				log.info(String.format("%s is connected - logging in", getName()));
				if (!login(user, password)) {
					disconnect();
				}

				getRoster();

			}

			return connection.isConnected();

		} catch (Exception e) {
			Logging.logError(e);
		}

		return false;
	}

	@Override
	public void connect(String uri) throws URISyntaxException {
		org.myrobotlab.framework.Message msg = createMessage("", "register", null);
		sendRemote(uri, msg);
	}

	public boolean connect(String host, int port, String user, String password) {
		return connect(host, port, user, password, service);
	}

	public boolean connect(String host, int port, String user, String password, String service) {
		this.hostname = host;
		this.port = port;
		this.user = user;
		this.password = password;
		this.service = service;
		return connect();
	}

	// FIXME - user name and password - default the host and port (duh)
	public boolean connect(String user, String password) {
		this.user = user;
		this.password = password;
		return connect(hostname, port, user, password);
	}

	public void createEntry(String user, String name) throws Exception {
		log.info(String.format("Creating entry for buddy '%1$s' with name %2$s", user, name));
		connect();
		Roster roster = connection.getRoster();
		roster.createEntry(user, name, null);
	}

	public void disconnect() {
		log.info(String.format("%s disconnecting from %s:%d", getName(), hostname, port));
		if (connection != null && connection.isConnected()) {
			connection.disconnect();
			connection = null;
		}

		config = null;
		chatManager = null;
		chats.clear();
	}

	@Override
	public String[] getCategories() {
		return new String[] { "control", "connectivity" };
	}

	@Override
	public HashMap<URI, Connection> getClients() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Connection> getConnections(URI clientKey) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDescription() {
		return "xmpp service to access the jabber network";
	}

	public RosterEntry getEntry(String userOrBuddyId) {
		RosterEntry entry = null;
		String id = null;
		int pos = userOrBuddyId.indexOf("/");
		if (pos > 0) {
			id = userOrBuddyId.substring(0, pos);
		} else {
			id = userOrBuddyId;
		}

		entry = roster.getEntry(id);
		if (entry != null) {
			return entry;
		}

		if (idToEntry.containsKey(id)) {
			return idToEntry.get(id);
		}

		return null;

	}

	public String getJabberID(String id) {
		RosterEntry entry = getEntry(id);
		String jabberID;
		if (entry == null) {
			// error("could not get entry for id - using %s", id);
			jabberID = id;
		} else {
			jabberID = entry.getUser();
		}
		return jabberID;
	}

	public CommOptions getOptions() {
		return null;
	}

	// @Override
	public Platform getPlatform() {
		return Runtime.getInstance().getPlatform();
	}

	@Override
	public String getPrefix(URI protocolKey) {
		if (defaultPrefix != null) {
			return defaultPrefix;
		} else {
			return "";// important - return "" not null
		}
	}

	public Roster getRoster() {
		roster = connection.getRoster();
		for (RosterEntry entry : roster.getEntries()) {
			log.info(String.format("User: %s %s ", entry.getName(), entry.getUser()));
			idToEntry.put(entry.getName(), entry);
		}
		return roster;
	}

	// FIXME - should be in runtime
	public String listServices() {
		StringBuffer sb = new StringBuffer();
		List<ServiceInterface> services = Runtime.getServices();
		for (int i = 0; i < services.size(); ++i) {
			ServiceInterface sw = services.get(i);
			sb.append(String.format("/%s\n", sw.getName()));
		}
		return sb.toString();
	}

	public boolean login(String username, String password) {
		log.info(String.format("login %s xxxxxxxx", username));
		if (connection == null || !connection.isConnected()) {
			return connect(hostname, port, username, password);
		} else {
			try {
				connection.login(username, password);
				// getRoster();
			} catch (Exception e) {
				Logging.logError(e);
				return false;
			}
		}
		return true;

	}

	/**
	 * processMessage is the XMPP / Smack API override which handles incoming
	 * chat messages - XMPP comes with well defined and extendable capabilities,
	 * however, Google Talk does not support much more than text messages with
	 * started open xmpp .. sad :(
	 * 
	 * So we'd like to send binary mrl messages - since google doesn't support
	 * any binary extentions .. we will base64 encode our messages and send them
	 * as regular chats ;)
	 * 
	 */

	// FIXME - get clear about different levels of authorization -
	// Security/Framework to handle at message/method level
	@Override
	public void processMessage(Chat chat, Message msg) {

		Message.Type type = msg.getType();
		String from = msg.getFrom();
		String body = msg.getBody();
		if (type.equals(Message.Type.error) || body == null || body.length() == 0) {
			// log.error("{} processMessage returned error {}", from, body);
			// TODO error count ?
			return;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("Received %s message [%s] from [%s]", type, body, from));
		}

		// Security HERE !
		// check each message here ??? versus CommunicationManager
		// someone wants to do a instance.method call
		// isAuthorized(id, name, method)
		// "internally" Message can be broken up into id = security header,
		// msg.name, msg.method
		// "externally" there is an incoming id (which could map to an internal
		// id?), msg.name, msg.method

		if (body.startsWith(Encoder.SCHEME_BASE64)) {
			// BASE 64 Messages
			org.myrobotlab.framework.Message inboundMsg = Encoder.base64ToMsg(body);

			log.info(String.format("********* remote inbound message from %s -to-> %s.%s *********", inboundMsg.sender, inboundMsg.name, inboundMsg.method));

			// broadcast - then msg gets sent to restricted service issue !
			// xmpp has its own security - how to integrate this with central
			// security ??
			xmppSecurity.put("user", getEntry(from).getName());
			if (security != null && !security.isAuthorized(xmppSecurity, inboundMsg.name, inboundMsg.method)) {
				log.error("Security does not allow processing of %s message", inboundMsg);
				return;
			}

			// must add key for registration ??? - foreign system has no
			// idea what my runtime's name is - I will wrap it in a my
			// own message and send it
			if (inboundMsg.method.equals("register")) {
				try {

					// BEGIN ENCAPSULATION --- ENCODER BEGIN -------------
					// IMPORTANT - (should be in Encoder) - create the key for
					// foreign service environment
					URI protocolKey = new URI(String.format("xmpp://%s", from));
					String mrlURI = String.format("mrl://%s/%s", getName(), protocolKey.toString());
					URI uri = new URI(mrlURI);

					// IMPORTANT - this is an optimization and probably should
					// be in the Comm interface defintion
					cm.addRemote(uri, protocolKey);

					// check if the URI is already defined - if not - we will
					// send back the services which we want to export - Security
					// will filter appropriately
					ServiceEnvironment foreignProcess = Runtime.getServiceEnvironment(uri);
					if (foreignProcess == null) {
						// not defined we will send export
						// TODO - Security filters - default export (include
						// exclude) - mapset of name
						ServiceEnvironment localProcess = Runtime.getLocalServicesForExport();

						Iterator<String> it = localProcess.serviceDirectory.keySet().iterator();
						String name;
						ServiceInterface si;
						while (it.hasNext()) {
							name = it.next();
							si = localProcess.serviceDirectory.get(name);

							org.myrobotlab.framework.Message sendService = createMessage("", "register", si);
							String base64 = Encoder.msgToBase64(sendService);
							sendMessage(base64, from);
						}

					}

					ServiceInterface si = (ServiceInterface) inboundMsg.data[0];
					// HMMM a vote for String vs URI here - since we need to
					// catch syntax !!!
					si.setInstanceId(uri);

					// if security ... msg within msg
					// getOutbox().add(createMessage(Runtime.getInstance().getName(),
					// "register", inboundMsg));
					Runtime.register(si, uri);// <-- not an INVOKE !!! // -
					// no security ! :P
					// BEGIN ENCAPSULATION --- ENCODER END -------------

				} catch (Exception e) {
					Logging.logError(e);
				}
			} else {
				// just route it
				getOutbox().add(inboundMsg);
			}

			return;
		}

		// chat client interface
		if (body.charAt(0) == '/') {
			// chat command - from chat client
			try {
				processRESTChatMessage(msg);
			} catch (Exception e) {
				broadcast(String.format("sorry sir, I do not understand your command %s", e.getMessage()));
				Logging.logError(e);
			}
		}

		/*
		 * CUSTOS SPECIFIC - REMOVE else if (body != null && body.length() > 0
		 * && body.charAt(0) != '/') { broadcast(
		 * "sorry sir, I do not understand! I await your orders but,\n they must start with / for more information go to http://myrobotlab.org/service/XMPP"
		 * ); broadcast("*HAIL BEPSL!*"); broadcast(String.format(
		 * "for a list of possible commands please type /%s/help", getName()));
		 * broadcast
		 * (String.format("current roster of active units is as follows\n\n %s",
		 * listServices()));
		 * broadcast(String.format("you may query any unit for help *HAIL BEPSL!*"
		 * )); // sendMessage(String.format("<b>hello</b>"), //
		 * "supertick@gmail.com"); }
		 */

		invoke("publishXMPPMsg", chat, msg);
		//

		// FIXME - decide if its a publishing point
		// or do we directly invoke and expect a response type
		// invoke("publishMessage", chat, msg);
	}

	public org.myrobotlab.framework.Message processMyRobotLabRESTMessage(Message msg) {

		return null;
	}

	// FIXME move to codec package
	public Object processRESTChatMessage(Message msg) {
		String body = msg.getBody();
		log.info(String.format("processRESTChatMessage [%s]", body));

		if (auditors.size() > 0) {
			for (String auditor : auditors) {
				RosterEntry re = getEntry(auditor);
				sendMessage(String.format("%s %s", re.getName(), msg.getBody()), msg.getFrom());
			}
		}

		if (body == null || body.length() < 1) {
			log.info("invalid");
			return null;
		}

		// TODO - allow to be in middle of message
		// pre-processing begin --------
		int pos0 = body.indexOf('/');
		if (pos0 != 0) {
			log.info("command must start with /");
			return null;
		}

		int pos1 = body.indexOf("\n");
		if (pos1 == -1) {
			pos1 = body.length();
		}

		String uri = "";
		if (pos1 > 0) {
			uri = body.substring(pos0, pos1);
		}

		uri = uri.trim();

		log.info(String.format("[%s]", uri));

		// pre-processing end --------

		// Message msg = Encoder.decodePathInfo(path);
		Object o = null;
		
		try {
			o = Encoder.invoke(uri);
		} catch (Exception e) {
			error(e);
		}
		// Object o = RESTProcessor.invoke(uri);

		// FIXME - encoding is that input uri before call ?
		// or config ?
		// FIXME - echo
		// FIXME - choose type of encoding based on input ? part of the URI init
		// call ?
		// e.g. /api/gson/runtime/getLocalIPAdddresses [/api/gson/ .. is assumed
		// (non-explicit) and pre-pended

		if (o != null) {
			broadcast(Encoder.toJson(o, o.getClass()));
			// broadcast(o.toString());
		} else {
			broadcast(null);
		}

		return o;
	}

	/**
	 * publishing point for XMPP messages
	 * 
	 * @param message
	 * @return
	 */
	public Message publishMessage(Chat chat, Message msg) {
		log.info(String.format("%s sent msg %s", msg.getFrom(), msg.getBody()));
		return msg;
	}

	@Override
	public Connection publishNewConnection(Connection conn) {
		return conn;
	}

	/**
	 * MRL Interface to gateways .. onMsg(GatewayData d) addMsgListener(Service
	 * s) publishMsg(Object..) returns gateway specific data
	 */

	public XMPPMsg publishXMPPMsg(Chat chat, Message msg) {
		return new XMPPMsg(chat, msg);
	}

	public boolean removeAuditor(String id) {
		RosterEntry entry = getEntry(id);
		if (entry == null) {
			error("can not remove auditor %s", id);
			return false;
		}
		String jabberID = entry.getUser();
		auditors.remove(jabberID);
		return true;
	}

	// FIXME synchronized not needed?
	synchronized public void sendMessage(String text, String id) {
		try {

			connect();

			String jabberID = getJabberID(id);

			// FIXME FIXME FIXME !!! - if
			// "just connected - ie just connected and this is the first chat of the connection then "create
			// chat" otherwise use existing chat !"
			Chat chat = null;
			if (chats.containsKey(jabberID)) {
				chat = chats.get(jabberID);
			} else {
				chat = chatManager.createChat(jabberID, this);
				chats.put(jabberID, chat);
			}

			log.info("chat threadid {} hashcode {}", chat.getThreadID(), chat.hashCode());

			if (text == null) {
				text = "null"; // dangerous converson?
			}

			// log.info(String.format("sending %s (%s) %s", entry.getName(),
			// jabberID, text));
			if (log.isDebugEnabled()) {
				log.info(String.format("sending %s %s", jabberID, (text.length() > 32) ? String.format("%s...", text.substring(0, 32)) : text));
			}
			chat.sendMessage(text);

		} catch (Exception e) {
			// currentChats.remove(jabberID);
			Logging.logError(e);
		}
	}

	// FIXME - create Resistrar interface sendMRLMessage(Message msg, URI/String
	// key)
	public void sendMRLMessage(org.myrobotlab.framework.Message msg, String id) {
		// Base64.enc
	}

	// TODO implement lower level messaging
	public void sendMyRobotLabJSONMessage(org.myrobotlab.framework.Message msg) {

	}

	public void sendMyRobotLabRESTMessage(org.myrobotlab.framework.Message msg) {

	}

	@Override
	public void sendRemote(String uri, org.myrobotlab.framework.Message msg) throws URISyntaxException {
		sendRemote(new URI(uri), msg);
	}

	/**
	 * sending remotely - need uri key data to send to client adds to history
	 * list as a hop - to "hopefully" prevent infinite routing problems
	 */
	@Override
	public void sendRemote(URI uri, org.myrobotlab.framework.Message msg) {
		// decompose uri or use as key (mmm specified encoding???)
		// FIXME - Encoder should do this !!!
		// String remoteURI = uri.getPath().substring(1 + "xmpp://".length());
		// // remove
		String remoteURI = uri.toString().substring("xmpp://".length()); // remove
																			// the
																			// root
																			// "/"
		// log.info(remoteURI);
		msg.historyList.add(getName());
		String base64 = Encoder.msgToBase64(msg);
		sendMessage(base64, remoteURI);
	}

	public void setStatus(boolean available, String status) {
		connect();
		if (connection != null && connection.isConnected()) {
			Presence.Type type = available ? Type.available : Type.unavailable;
			Presence presence = new Presence(type);
			presence.setStatus(status);
			connection.sendPacket(presence);
		} else {
			log.error("setStatus not connected");
		}
	}

	@Override
	public void stopService() {
		super.stopService();
		disconnect();
	}

}
