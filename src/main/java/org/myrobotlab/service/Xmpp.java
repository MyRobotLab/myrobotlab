package org.myrobotlab.service;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.Roster.SubscriptionMode;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.roster.packet.RosterPacket.ItemStatus;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration.Builder;
import org.myrobotlab.codec.Api;
import org.myrobotlab.codec.CodecUri;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.Connection;
import org.myrobotlab.service.interfaces.Gateway;
import org.slf4j.Logger;

/**
 * An Xmpp service which utilizes Jive's smack client library There is smack,
 * whack, and tinder
 * http://stackoverflow.com/questions/1547599/differences-between-smack-tinder-
 * and-whack
 * 
 * @author GROG
 *
 */
public class Xmpp extends Service implements Gateway, ChatManagerListener, ChatMessageListener, MessageListener, RosterListener, ConnectionListener {// ,

  public static class Contact {
    public String user;
    public String presence;
    public String type;
    public String name;
    public String status;

    public String toString() {
      return String.format("user: %s, name: %s, presence: %s, type: %s, status: %s", user, name, type, presence, status);
    }
  }

  public static class XmppMsg {
    public String from;
    public String msg;
    public String type;
    public String stanzaId;

    public XmppMsg(Chat chat, Message msg) {
      this.from = chat.getParticipant();
      this.msg = msg.getBody();
      Message.Type t = msg.getType();
      if (t != null) {
        this.type = msg.getType().toString();
      }
      stanzaId = msg.getStanzaId();
    }
  }

  boolean isConnected = false;
  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Xmpp.class);

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {
    ServiceType meta = new ServiceType(Xmpp.class.getCanonicalName());
    meta.addDescription("xmpp service to access the jabber network");
    meta.addCategory("connectivity");
    meta.addDependency("org.jivesoftware.smack", "4.1.6");
    return meta;
  }

  transient CodecUri uri = new CodecUri();

  TreeMap<String, Contact> contacts = new TreeMap<String, Contact>();
  String username;
  String password;

  String hostname = "myrobotlab.org"; // talk.myrobotlab.org
  String serviceName = "myrobotlab.org"; // xmpp.myrobotlab.org

  int port = 5222;
  transient XMPPTCPConnectionConfiguration config;
  transient XMPPTCPConnection connection;
  transient ChatManager chatManager;

  transient Roster roster = null;

  /**
   * auditors chat buddies who can see what commands are being processed and by
   * who through the Xmpp service TODO - audit full system ??? regardless of
   * message origin?
   */
  HashSet<String> auditors = new HashSet<String>();

  // HashSet<String> responseRelays = new HashSet<String>();
  HashSet<String> allowCommandsFrom = new HashSet<String>();

  transient HashMap<String, Chat> chats = new HashMap<String, Chat>();

  transient Chat chat = null;

  public Xmpp(String n) {
    super(n);
  }

  public void addBuddy(String user) throws NotLoggedInException, NoResponseException, XMPPErrorException, NotConnectedException {
    Roster roster = Roster.getInstanceFor(connection);
    roster.setSubscriptionMode(SubscriptionMode.accept_all);
    // jid: String, user: String, groups: String[]

    // null groups
    roster.createEntry("grog@myrobotlab.org", "grog", null);
  }

  @Override
  public void addConnectionListener(String name) {
    // TODO Auto-generated method stub

  }

  public void addXmppMsgListener(Service service) {
    // FIXME - implement direct callback or pub sub support ??
  }

  public void broadcast(String msg) {
    // TODO - possibly implement
    // but we should use more xmpp definitions e.g. broadcast to room
    // define a room etc...
  }

  public void chatCreated(Chat chat, boolean locallyCreated) {
    // test if locallyCreated
    if (!locallyCreated) {
      chat.addMessageListener(this);
    }
  }

  // grog@xmpp://{host}:5222 ???
  @Override
  public void connect(String uri) throws URISyntaxException {
    // TODO Auto-generated method stub

  }

  public void connect(String hostname, int port, String username, String password) throws Exception {

    purgeTask("reconnect");

    this.hostname = hostname;
    this.serviceName = hostname;
    this.port = port;
    this.username = username;
    this.password = password;

    Builder builder = XMPPTCPConnectionConfiguration.builder();
    builder.setUsernameAndPassword(username, password);
    builder.setServiceName(serviceName);
    builder.setServiceName(hostname);
    builder.setPort(port);
    builder.setSecurityMode(SecurityMode.disabled);
    builder.setDebuggerEnabled(true);

    XMPPTCPConnectionConfiguration config = builder.build();

    connection = new XMPPTCPConnection(config);
    connection.connect().login();

    roster = Roster.getInstanceFor(connection);
    chatManager = ChatManager.getInstanceFor(connection);

    roster.addRosterListener(this);

    isConnected = true;

    // not worth it - always empty right after connect
    // getContactList();

    broadcastState();
  }

  public void connect(String username, String password) throws Exception {
    connect("myrobotlab.org", 5222, username, password);
  }

  public void disconnect() {
    if (connection != null) {
      connection.disconnect();
    }
    isConnected = false;
    broadcastState();

  }

  @Override
  public void entriesAdded(Collection<String> entries) {
    log.info("entriesAdded {}", entries);
    getContactList();
  }

  @Override
  public void entriesDeleted(Collection<String> entries) {
    log.info("entriesAdded {}", entries);

  }

  @Override
  public void entriesUpdated(Collection<String> entries) {
    log.info("entriesAdded {}", entries);

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

  public Contact getContact(RosterEntry r) {
    Contact contact = new Contact();
    contact.name = r.getName();
    contact.user = r.getUser();
    contact.type = r.getType().toString();

    Type presenceType = roster.getPresence(r.getUser()).getType();
    if (presenceType != null) {
      contact.presence = presenceType.toString();
    }

    ItemStatus status = r.getStatus();
    if (status != null) {
      contact.status = status.toString();
    }

    // ItemStatus status = r.getStatus(); // null
    // log.info("roster entry {}", r.toString());
    // contact.na= r.getUser();
    log.info("getContact {}", contact.toString());

    return contact;
  }

  /*
   * Displays users (entries) in the roster
   */
  public Map<String, Contact> getContactList() {
    // Roster roster = Roster.getInstanceFor(connection);
    Collection<RosterEntry> entries = roster.getEntries();
    contacts.clear();

    log.info("\n\n" + entries.size() + " buddy(ies):");
    for (RosterEntry r : entries) {
      Contact c = getContact(r);
      contacts.put(c.user, c);
    }
    broadcastState();
    return contacts;
  }

  @Override
  public String getPrefix(URI protocolKey) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void presenceChanged(Presence presence) {
    log.info("presenceChanged {}", presence);
    // String user = presence.getFrom();

    getContactList();
    /*
     * if (contacts.containsKey(user)) { Contact c = contacts.get(user);
     * c.presence = presence.toString(); invoke("publishPresenceChanged", c); }
     */
  }

  /**
   * Process received messages
   */
  public void processMessage(Chat chat, Message message) {
    XmppMsg xmppMsg = new XmppMsg(chat, message);
    invoke("publishXmppMsg", xmppMsg);

    Message.Type type = message.getType();
    String participant = chat.getParticipant();
    String body = message.getBody();
    log.info("message of type {} from user {} - {}", type, participant, body);
    if (type == Message.Type.chat) {
      if (body.startsWith("/")) {
        // String pathInfo = String.format("/%s/service%s",
        // CodecUtils.PREFIX_API, body); FIXME - wow that was horrific
        String pathInfo = String.format("/%s%s", Api.PREFIX_API, body);
        try {
          org.myrobotlab.framework.Message msg = CodecUri.decodePathInfo(pathInfo);
          Object ret = null;
          ServiceInterface si = Runtime.getService(msg.name);
          if (si == null) {
            ret = Status.error("could not find service %s", msg.name);
          } else {
            ret = si.invoke(msg.method, msg.data);
          }

          if (ret != null && ret instanceof Serializable) {
            // configurable use log or system.out ?
            // FIXME - make getInstance configurable
            // Encoder
            // reference !!!
            sendMessage(CodecUtils.toJson(ret), participant);
          }
        } catch (Exception e) {
          try {
            Logging.logError(e);
            sendMessage(e.toString(), participant);
          } catch (Exception e2) {
            // give up
          }
        }
      }
    } else {
      log.error("don't know how to handle message of type {}", type);
    }

  }

  @Override
  public void processMessage(Message msg) {
    log.info("here");
  }

  @Override
  public Connection publishConnect(Connection keys) {
    // TODO Auto-generated method stub
    return null;
  }

  public Contact publishPresenceChanged(Contact contact) {
    return contact;
  }

  /*
   * MRL Interface to gateways .. onMsg(GatewayData d) addMsgListener(Service s)
   * publishMsg(Object..) returns gateway specific data
   */
  // FIXME - should be MessageXmpp along with all other message types under
  // org.myrobotlab.msg
  public XmppMsg publishXmppMsg(XmppMsg msg) {
    return msg;
  }

  public XmppMsg publishSentXmppMsg(XmppMsg msg) {
    return msg;
  }

  /*
   * Sends the specified text as a message to the other chat participant.
   * 
   * @param text - the message
   */
  public void sendMessage(String text, String to) throws XMPPException, NotConnectedException {
    if (chat == null) {
      Chat chat = chatManager.createChat(to, this);
      chat.addMessageListener(this);
      this.chat = chat;
    }

    Message message = new Message();
    message.setTo(chat.getParticipant());
    message.setType(Message.Type.chat); // Message.Type.groupchat
    message.setThread(chat.getThreadID());
    message.setBody(text);

    chat.sendMessage(message);

    invoke("publishSentXmppMsg", new XmppMsg(chat, message));
  }

  @Override
  public void sendRemote(String key, org.myrobotlab.framework.Message msg) throws URISyntaxException {
    // TODO Auto-generated method stub

  }

  @Override
  public void sendRemote(URI key, org.myrobotlab.framework.Message msg) {
    // TODO Auto-generated method stub

  }

  public void setStatus(boolean available, String status) {
    if (connection != null && connection.isConnected()) {
      Presence.Type type = available ? Type.available : Type.unavailable;
      Presence presence = new Presence(type);
      presence.setStatus(status);
      // connection.sendPacket(presence);
    } else {
      log.error("setStatus not connected");
    }
  }

  @Override
  public void stopService() {
    super.stopService();
    disconnect();
  }

  // FIXME - sendMsg onMsg getMsg - GLOBAL INTERFACE FOR GATEWAYS
  // FIXME - handle multiple user accounts

  // best ->
  // https://www.snip2code.com/Snippet/828300/Smack-API-example-(uses-Smack-v4-1-5)
  public static void main(String[] args) {

    // 1. get connection
    // 2. login
    // 3. set auto accept
    // 4. get roster
    // 5. list buddies

    // 6. addChatListener (add self)
    // 7. in chatCreated
    // create a Message Listener (add self)

    // more stuff

    LoggingFactory.init(Level.INFO);

    try {

      // SmackConfiguration.DEBUG = true;

      Xmpp xmpp1 = (Xmpp) Runtime.createAndStart("xmpp", "Xmpp");
      // Runtime.start(String.format("clock%d", i), "Clock");
      // Runtime.start("gui", "SwingGui");
      // Runtime.start("python", "Python");
      // HMMM is fully qualified name important ???
      // grog.robot01@myrobotlab.org vs grog.robot01 ???
      //

      xmpp1.connect("grog.robot01@myrobotlab.org", "xxxxxx");
      // xmpp1.connect("myrobotlab.org", 5222,
      // "grog.robot01@myrobotlab.org", "zardoz7");
      // xmpp1.test();
      // xmpp1.test2();
      // xmpp1.addBuddy("grog@myrobotlab.org");
      // for (int i = 0; i < 100; ++i) {
      // xmpp1.sendMessage(String.format("/runtime/getUptime/%d", i),
      // "grog@myrobotlab.org");
      // }

      Runtime.createAndStart("webgui", "WebGui");

      xmpp1.sendMessage("hello !", "grog@myrobotlab.org");
      xmpp1.getContactList();

      // xmpp1.getContactList();
      // xmpp1.connect("myrobotlab.org", 5222, "grog.robot01", "xxxxxxx");
      // xmpp1.addAuditor("Ma. Vo.");
      // xmpp1.sendMessage("Ma. Vo. - xmpp test", "Ma. Vo.");
      // xmpp1.send("Ma. Vo.", "xmpp test");
      // xmpp1.sendMessage("hello from incubator by name " +
      // System.currentTimeMillis(), "Greg Perry");
      xmpp1.sendMessage("/runtime/getUptime", "GroG@myrobotlab.org");
      xmpp1.sendMessage("msg 2", "GroG@myrobotlab.org");
      // xmpp1.sendMessage("/runtime", "grogbot@myrobotlab.org");

      // TEST CASES :
      // 1. different clients
      // 2. group chats
      // 3. URL processing must be handled in Codec !!!! and same as CLI
      // !!!
      // 4. Non-text chats ?
      // 5. Mrl Messages

      log.info("here");

    } catch (Exception e) {
      Logging.logError(e);
    }

  }

  @Override
  public void authenticated(XMPPConnection arg0, boolean arg1) {
    // TODO Auto-generated method stub

  }

  @Override
  public void connected(XMPPConnection arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void connectionClosed() {
    log.info("connectionClosed");
    addTask("reconnect", 5000, 0, "connect", hostname, port, username, password);
    isConnected = false;
    broadcastState();
  }

  @Override
  public void connectionClosedOnError(Exception arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void reconnectingIn(int arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void reconnectionFailed(Exception arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void reconnectionSuccessful() {
    // TODO Auto-generated method stub

  }

@Override
public String publishConnect() {
	// TODO Auto-generated method stub
	return null;
}

@Override
public String publishDisconnect() {
	// TODO Auto-generated method stub
	return null;
}

@Override
public Status publishError() {
	// TODO Auto-generated method stub
	return null;
}

}
