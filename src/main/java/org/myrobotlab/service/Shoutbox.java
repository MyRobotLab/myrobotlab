package org.myrobotlab.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Message;
// import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.io.FindFile;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.ProgramAB.Response;
import org.myrobotlab.service.Xmpp.XmppMsg;
import org.slf4j.Logger;

// FIXME - use Peers !
public class Shoutbox extends Service {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(Shoutbox.class);

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
   * WSServer and from WSServer broadcasted to clients - therefore instead of a
   * seperate system message we will have system data components of the shout -
   * these are to display server data on the clients
   */
  public static class Shout implements Serializable {
    private static final long serialVersionUID = 1L;
    public String from;
    public String type;
    public String msg;
    public String color;
    public String ip; // TODO change to key
    public String clientId;

    public String time;

  }

  static final public String TYPE_SYSTEM = "TYPE_SYSTEM";
  static final public String TYPE_USER = "TYPE_USER";
  static final public String ORGIN_XMPP = "ORGIN_XMPP";
  static final public String ORGIN_WEB = "ORGIN_WEB";

  transient static NameProvider nameProvider = new DefaultNameProvider();

  /*
   * Core to managing the connections are the keys The keys for websockets are
   * defined as remoteIp:remotePort - unfortunately these are null on disconnect
   * so a seperate lookup needs to be utilized The keys for xmpp "buddies" are
   * simply their jabber ids
   * 
   * A Connection's UserId is a "user friendly" identification of the user using
   * that connection
   * 
   */

  static public String makeKey(String ws) {
    return String.format("%s:%s", ws, ws);
  }

  transient ProgramAB chatbot;

  transient Xmpp xmpp;
  transient ArrayList<String> xmppRelays = new ArrayList<String>();
  transient ArrayList<String> chatbotNames = new ArrayList<String>();

  transient HashMap<String, String> aliases = new HashMap<String, String>();
  int imageDefaultHeight = 200;

  int imageDefaultWidth = 200;

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

  // transient Connections conns = new Connections();

  int maxShoutsInMemory = 200;

  ArrayList<Shout> shouts = new ArrayList<Shout>();

  HashMap<String, Object> clients = new HashMap<String, Object>();
  int msgCount;

  transient FileWriter fw = null;

  transient BufferedWriter bw = null;

  int maxArchiveRecordCount = 50;

  public Shoutbox(String n) {
    super(n);
    chatbotNames.add("@mrt");
    chatbotNames.add("@mr.turing");
    chatbotNames.add("@mrturing");
  }

  public String addXMPPRelay(String user) {
    xmppRelays.add(user);
    // xmpp.sendMessage("now shoutbox relay", user); FIXME
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
        String filename = String.format("%s/shouts.%s.js", getName(), tsFormatter.format(new Date()));
        File archive = new File(filename);

        fw = new FileWriter(archive.getAbsoluteFile());
        bw = new BufferedWriter(fw);

        String d = String.format("%s", CodecUtils.toJson(shout));
        bw.write(d);
        return;
      }

      String d = String.format(",%s", CodecUtils.toJson(shout));
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

  // WTFU - publishShout does not use this .. why??
  public Shout createShout(String type, String msg) {
    Shout shout = new Shout();
    shout.type = type;
    shout.msg = msg;
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

  public void getXMPPRelays() {
    Shout shout = createShout(TYPE_USER, Arrays.toString(xmppRelays.toArray()));
    shout.from = "mr.turing";
    invoke("publishShout", shout);
  }

  /**
   * archiving restores last json file back into newly started shoutbox
   */
  public void loadShouts() {
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

      String json = String.format("[%s]", FileIO.toString(latest.getAbsoluteFile()));

      Shout[] saved = CodecUtils.fromJson(json, Shout[].class);

      for (int i = 0; i < saved.length; ++i) {
        shouts.add(saved[i]);
      }

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public void setNickName(String nickname) {
    // WebGui web
    log.info("setNickName {}", nickname);
  }

  public void mimicTuring(String msg) {
    Shout shout = createShout(TYPE_USER, msg);
    shout.from = "mr.turing";
    invoke("publishShout", shout);
  }

  // FIXME - refactor ---(all msgs from non websockets e.g. chatbot | xmpp |
  // other --to--> websockets
  // FIXME - onChatBotResponse
  // onProgramAB response - onChatBotResponse ???
  public Response onResponse(Response response) {
    log.info("chatbot shouting");

    // String r = resizeImage(response.msg);
    String r = response.msg;

    // conns.addConnection("mr.turing", "mr.turing");

    Shout shout = createShout(TYPE_USER, r);
    shout.from = "mr.turing";
    invoke("publishShout", shout);
    return response;
  }

  // FIXME FIXME FIXME - not normalized with publishShout(WebSocket) :PPPP
  // FIXME - must fill in your name - "Greg Perry" somewhere..
  public void onXMPPMsg(XmppMsg xmppMsg) {
    log.info(String.format("Xmpp - %s %s", xmppMsg.from, xmppMsg.msg));

    // not exactly the same model as onConnect - so we try to add each time
    String user = "me";// FIXME
    // xmpp.getEntry(xmppMsg.msg.getFrom()).getName();
    // conns.addConnection(xmppMsg.msg.getFrom(), user);

    Shout shout = createShout(TYPE_USER, xmppMsg.msg);
    shout.from = user;

    invoke("publishShout", shout);
  }

  /*
   * shout of minimal complexity
   * 
   */
  public void shout(String msg) {
    // an optimized shout - there is client id & auth stuff which should be
    // supplied at the service level
    // a client should simply shout('my text') and all the other parts be
    // filled in on overloaded methods
    shout("test", msg);
  }

  /*
   * max complexity shout
   * 
   */
  public void shout(String clientId, String msg) {
    Shout shout = createShout(TYPE_USER, msg);
    shout.clientId = clientId;
    shout.from = clientId; // ????
    invoke("publishShout", shout);
  }

  // EXCHANGE need "session-key" to do a - connection/session-key for user
  // FIXME NOT NORMALIZED with onXMPPMsg() !!!!
  // public void publishShout(WSMsg wsmsg) { is Message necessary here?
  public Shout publishShout(Shout shout) throws NotConnectedException, XMPPException {
    log.info(String.format("publishShout %s %s", shout.from, shout.msg));

    String foundName = findChatBotName(shout.msg);
    if (foundName != null) {
      chatWithChatbot(foundName, shout);
    }

    shouts.add(shout);
    // Message out = createMessage("shoutclient", "publishShout",
    // CodecUtils.toJson(shout));
    // TODO: what do we do with the result of this method?
    Message.createMessage(this, "shoutclient", "publishShout", CodecUtils.toJson(shout));
    // webgui.sendToAll(out);

    if (xmpp != null && !TYPE_SYSTEM.equals(shout.type)) {
      for (int i = 0; i < xmppRelays.size(); ++i) {
        String relayName = xmppRelays.get(i);
        String jabberID = null;// FIXME xmpp.getJabberID(relayName);
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

    return shout;
  }

  public void quickStart(String xmpp, String password) throws Exception {
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

    addTask(30 * 60 * 1000, "savePredicates");
  }

  public String removeXMPPRelay(String user) {
    xmppRelays.remove(user);
    // conns.remove(xmpp.getJabberID(user));
    return user;
  }

  // TODO: when this gets used we could add it back in
  // private String resizeImage(String shout) {
  // int x = shout.indexOf("<img");
  // if (x > 0) {
  // int space = shout.indexOf(" ", x);
  // int endTag = shout.indexOf(">", x);
  // int insert = (space < endTag) ? space : endTag;
  // String r = String.format("%s width=%d height=%d %s", shout.substring(0,
  // insert), imageDefaultWidth, imageDefaultHeight, shout.substring(insert));
  // log.info(String.format("=========== RESIZE ============ %s", r));
  // }
  //
  // return shout;
  // }

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

  // --------- Xmpp END ------------

  // String lastShoutMsg = null;

  public void sendTo(String type, String key, Object data) {
    Shout shout = createShout(TYPE_SYSTEM, CodecUtils.toJson(data));
    String msgString = CodecUtils.toJson(shout);
    // TODO: do something with the "sendTo" message?
    // Message sendTo = createMessage("shoutclient", "publishShout", msgString);
    Message.createMessage(this, "shoutclient", "publishShout", msgString);

  }

  public void setNameProvider(NameProvider nameProvider2) {
    nameProvider = nameProvider2;
  }

  public NameProvider setNameProvider(String classname) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    Class<?> theClass = Class.forName(classname);
    nameProvider = (NameProvider) theClass.newInstance();
    return nameProvider;
  }

  // TO PEER OR NOT TO PEER THAT IS THE QUESTION...
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

    try {
      // TODO FIGURE THIS OUT :P OATH ?
      String provider = "org.myrobotlab.client.DrupalNameProvider";
      log.info(String.format("attempting to set name provider - %s", provider));
      setNameProvider(provider);
    } catch (Exception e) {
      error(e);
    }

    loadShouts();
  }

  // ---- outbound ---->

  /*
   * 
   * 
   * CONCEPTS systemBroadcast - system needs to send to all system message list
   * - system sends to a list of users system message channel -
   * 
   * channel - a group of recievers &amp; senders
   * 
   * Authenticaiton &amp; Authorization - OATH query to Drupal?
   * 
   * DATA timezone - set time zode - use UTC for all server data
   * 
   * // system related public int connectionCount; public int userCount; public
   * int guestCount; public int msgCount;
   * 
   * getVersion
   */

  // --------- Xmpp BEGIN ------------
  public void startXMPP(String user, String password) throws Exception {
    if (xmpp == null) {
      xmpp = (Xmpp) Runtime.start("xmpp", "Xmpp");
    }

    xmpp.connect("myrobotlab.org", 5222, user, password);
    xmpp.addXmppMsgListener(this);

  }

  public static void main(String args[]) {
    LoggingFactory.init(Level.INFO);

    try {
      Shoutbox shoutbox = (Shoutbox) Runtime.start("shoutbox", "Shoutbox");
      Shout shout = new Shout();
      shout.from = "Fred";
      shout.msg = "Hello I'm Fred";

      shoutbox.shouts.add(shout);

      shout = new Shout();
      shout.from = "George";
      shout.msg = "Hi I'm George";
      shoutbox.shouts.add(shout);

      shoutbox.createShout(TYPE_SYSTEM, "this is a test shout");
      shoutbox.createShout(TYPE_SYSTEM, "this is another test shout");
      shoutbox.createShout(TYPE_SYSTEM, "more test shouting");

      Runtime.start("cli", "Cli");

      Runtime.start("webgui", "WebGui");

    } catch (Exception e) {
      Logging.logError(e);
    }

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

    ServiceType meta = new ServiceType(Shoutbox.class.getCanonicalName());
    meta.addDescription("shoutbox server");
    meta.addCategory("connectivity");

    return meta;
  }

}
