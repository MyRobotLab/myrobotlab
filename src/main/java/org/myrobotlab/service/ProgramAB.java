package org.myrobotlab.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.alicebot.ab.AIMLMap;
import org.alicebot.ab.AIMLSet;
import org.alicebot.ab.Bot;
import org.alicebot.ab.Category;
import org.alicebot.ab.Chat;
import org.alicebot.ab.MagicBooleans;
import org.alicebot.ab.Predicates;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.logging.SimpleLogPublisher;
import org.myrobotlab.programab.ChatData;
import org.myrobotlab.programab.MrlSraixHandler;
import org.myrobotlab.programab.OOBPayload;
import org.myrobotlab.service.data.Locale;
import org.myrobotlab.service.interfaces.LocaleProvider;
import org.myrobotlab.service.interfaces.LogPublisher;
import org.myrobotlab.service.interfaces.SearchPublisher;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.slf4j.Logger;

/**
 * Program AB service for MyRobotLab Uses AIML 2.0 to create a ChatBot This is a
 * reboot of the Old AIML spec to be more 21st century.
 *
 * More Info at http://aitools.org/ProgramAB
 * 
 * The ProgramAB service is the host to many AIML based Bots. Each bot can
 * maintain a chat session with multiple users. This association between a bot
 * and a user
 * 
 * @author kwatters
 *
 */
public class ProgramAB extends Service implements TextListener, TextPublisher, LocaleProvider, LogPublisher {
  // Internal class for the program ab response.
  public static class Response {
    // FIXME - timestamps are usually longs System.currentTimeMillis()
    public Date timestamp;
    public String botName;
    public String userName;
    public String msg;
    public List<OOBPayload> payloads;

    public Response(String userName, String botName, String msg, List<OOBPayload> payloads, Date timestamp) {
      this.botName = botName;
      this.userName = userName;
      this.msg = msg;
      this.payloads = payloads;
      this.timestamp = timestamp;
    }

    public String toString() {
      StringBuilder str = new StringBuilder();
      str.append("[");
      str.append("Time:" + timestamp.getTime() + ", ");
      str.append("Bot:" + botName + ", ");
      str.append("User:" + userName + ", ");
      str.append("Msg:" + msg + ", ");
      str.append("Payloads:[");
      if (payloads != null) {
        for (OOBPayload payload : payloads) {
          str.append(payload.toString() + ", ");
        }
      }
      str.append("]]");
      return str.toString();
    }
  }

  static final long serialVersionUID = 1L;
  transient public final static Logger log = LoggerFactory.getLogger(ProgramAB.class);
  // TODO: this path is really specific to each bot that is loaded.
  // right now there is a requirement that all active bots are loaded from the
  // same directory. (defaulting to ProgramAB)
  private String path = "ProgramAB";
  // bots map is keyed off the lower case version of the bot name.
  private transient HashMap<String, Bot> bots = new HashMap<String, Bot>();
  // Mapping a bot to a username and chat session
  private HashMap<String, HashMap<String, ChatData>> sessions = new HashMap<String, HashMap<String, ChatData>>();
  // TODO: ProgramAB default bot should be Alice-en_US we should name the rest
  // of the language specific default bots.
  // initial default values for the current bot/and user
  private String currentBotName = "en-US";
  // This is the default username that is chatting with the bot.
  private String currentUserName = "default";
  public int savePredicatesInterval = 300000; // every 5 minutes
  // TODO: move the implementation from the gui to this class so it can be used
  // across web and swing gui properly.
  boolean visualDebug = true;
  // TODO: if true, AIML is written back to disk on shutdown of this service.
  public boolean writeOnExit = true;

  /**
   * list of available bots - populated on startup
   */
  HashSet<String> availableBots = new HashSet<>();
  boolean peerSearch = true;
  private Locale locale;
  
  transient SimpleLogPublisher logPublisher = null;

  /**
   * Default constructor for the program ab service.
   * 
   * 
   * @param n
   *          - service name
   * @param id
   *          - process id
   */
  public ProgramAB(String n, String id) {
    super(n, id);
    availableBots = getBots();
    addTask("savePredicates", savePredicatesInterval, 0, "savePredicates");
    logPublisher = new SimpleLogPublisher(this); 
    logPublisher.filterClasses(new String[]{ "org.alicebot.ab.Graphmaster", "org.alicebot.ab.MagicBooleans", "class org.myrobotlab.programab.MrlSraixHandler" });
    logPublisher.start();
  }

  public void addOOBTextListener(TextListener service) {
    addListener("publishOOBText", service.getName(), "onOOBText");
  }

  public void addResponseListener(Service service) {
    addListener("publishResponse", service.getName(), "onResponse");
  }

  @Deprecated /* use standard attachTextListener */
  public void addTextListener(TextListener service) {
    attachTextListener(service);
  }

  public void addTextListener(SpeechSynthesis service) {
    addListener("publishText", service.getName(), "onText");
  }

  public void addTextPublisher(TextPublisher service) {
    subscribe(service.getName(), "publishText");
  }

  private String createSessionPredicateFilename(String username, String botName) {
    // TODO: sanitize the session label so it can be safely used as a filename
    String predicatePath = getPath() + File.separator + "bots" + File.separator + botName + File.separator + "config";
    // just in case the directory doesn't exist.. make it.
    File predDir = new File(predicatePath);
    if (!predDir.exists()) {
      predDir.mkdirs();
    }
    predicatePath += File.separator + username + ".predicates.txt";
    return predicatePath;
  }

  public int getMaxConversationDelay() {
    return sessions.get(getCurrentBotName()).get(getCurrentUserName()).maxConversationDelay;
  }

  /**
   * This is the main method that will ask for the current bot's chat session to
   * respond to It returns a Response object.
   * 
   * @param text
   * @return
   * @throws IOException 
   */
  public Response getResponse(String text) throws IOException {
    return getResponse(getCurrentUserName(), text);
  }

  /**
   * This method has the side effect of switching which bot your are currently
   * chatting with.
   * 
   * @param username
   *          - the query string to the bot brain
   * @param text
   *          - the user that is sending the query
   * @return the response for a user from a bot given the input text.
   * @throws IOException 
   */
  public Response getResponse(String username, String text) throws IOException {
    return getResponse(username, getCurrentBotName(), text);
  }

  /**
   * Full get response method . Using this method will update the current
   * user/bot name if different from the current session.
   * 
   * @param userName
   * @param botName
   * @param text
   * @return
   * @throws IOException 
   */
  public Response getResponse(String userName, String botName, String text) throws IOException {
    return getResponse(userName, botName, text, true);
  }

  /**
   * Gets a response and optionally update if this is the current bot session
   * that's active globally.
   * 
   * @param userName
   * @param botName
   * @param text
   * @param updateCurrentSession
   *          (specify if the currentbot/currentuser name should be updated in
   *          the programab service.)
   * @return
   * @throws IOException 
   */
  public Response getResponse(String userName, String botName, String text, boolean updateCurrentSession) throws IOException {
    // error check the input.
    log.info("Get Response for : user {} bot {} : {}", userName, botName, text);
    if (userName == null || botName == null || text == null) {
      String error = "ERROR: Username , botName or text was null. no response.";
      error(error);
      return new Response(userName, botName, error, null, new Date());
    }
    // update the current session if we want to change which bot is at
    // attention.
    if (updateCurrentSession) {
      updateCurrentSession(userName, botName);
    }

    Bot bot = bots.get(botName.toLowerCase());
    if (bot == null) {
      String error = "ERROR: Core not loaded, please load core before chatting.";
      error(error);
      return new Response(userName, botName, error, null, new Date());
    }
    // Auto start a new session from the current path that the programAB service
    // is operating out of.
    if (!sessions.containsKey(botName) || !sessions.get(botName).containsKey(userName)) {
      startSession(getPath(), userName, botName);
    }
    ChatData chatData = sessions.get(botName).get(userName);
    // Get the actual bots aiml based response.
    String res = getChat(userName, botName).multisentenceRespond(text);
    // grab and update the time when this response came in.
    chatData.lastResponseTime = new Date();
    // Check the AIML response to see if there is OOB (out of band data)
    // If so, process those oob messages.
    List<OOBPayload> payloads = null;
    if (chatData.processOOB) {
      payloads = processOOB(res);
    }
    // OOB text should not be published as part of the response text.
    if (payloads != null) {
      res = OOBPayload.removeOOBFromString(res).trim();
    }
    // create the response object to return
    Response response = new Response(userName, botName, res, payloads, chatData.lastResponseTime);
    // Now that we've said something, lets create a timer task to wait for N
    // seconds
    // and if nothing has been said.. try say something else.
    // TODO: trigger a task to respond with something again
    // if the humans get bored
    if (chatData.enableAutoConversation) {
      // schedule one future reply. (always get the last word in..)
      // int numExecutions = 1;
      // TODO: we need a way for the task to just execute one time
      // it'd be good to have access to the timer here, but it's transient
      addTask("getResponse", chatData.maxConversationDelay, 0, "getResponse", userName, text);
    }

    // EEK! clean up the API!
    invoke("publishRequest", text); // publisher used by uis
    invoke("publishResponse", response);
    invoke("publishResponseText", response);
    invoke("publishText", response.msg);
    info("to: %s - %s", userName, res);
    return response;
  }

  private void updateCurrentSession(String userName, String botName) {
    // update the current user/bot name..
    if (!botName.equals(getCurrentBotName())) {
      // update which bot is in the front.. and honestly. we should also set
      // which userName is currently talking to the bot.
      log.info("Setting {} as the current bot.", botName);
      this.setCurrentBotName(botName);
    }
    if (!userName.equals(getCurrentUserName())) {
      // update which bot is in the front.. and honestly. we should also set
      // which userName is currently talking to the bot.
      log.info("Setting {} user as the currnt user.", userName);
      this.setCurrentUserName(userName);
    }
  }

  /**
   * This method specifics how many times the robot will respond with the same
   * thing before forcing a different (default?) response instead.
   * 
   * @param val
   */
  public void repetitionCount(int val) {
    org.alicebot.ab.MagicNumbers.repetition_count = val;
  }

  public Chat getChat(String userName, String botName) {
    if (sessions.containsKey(botName) && sessions.get(botName).containsKey(userName)) {
      return sessions.get(botName).get(userName).chat;
    } else {
      warn("%s %S session does not exist", botName, userName);
      return null;
    }
  }

  public void removePredicate(String userName, String predicateName) {
    removePredicate(userName, getCurrentBotName(), predicateName);
  }

  public void removePredicate(String userName, String botName, String predicateName) {
    Predicates preds = getChat(userName, botName).predicates;
    preds.remove(predicateName);
  }

  /**
   * Add a value to a set for the current session
   * 
   * @param setName
   * @param setValue
   */
  public void addToSet(String setName, String setValue) {
    // add to the set for the bot.
    Bot bot = bots.get(getCurrentBotName().toLowerCase());
    AIMLSet updateSet = bot.setMap.get(setName);
    setValue = setValue.toUpperCase().trim();
    if (updateSet != null) {
      updateSet.add(setValue);
      // persist to disk.
      updateSet.writeAIMLSet();
    } else {
      log.info("Unknown AIML set: {}.  A new set will be created. ", setName);
      // TODO: should we create a new set ? or just log this warning?
      // The AIML Set doesn't exist. Lets create a new one
      AIMLSet newSet = new AIMLSet(setName, bot);
      newSet.add(setValue);
      newSet.writeAIMLSet();
    }
  }

  /**
   * Add a map / value for the current session
   * 
   * @param mapName
   *          - the map name
   * @param key
   *          - the key
   * @param value
   *          - the value
   */
  public void addToMap(String mapName, String key, String value) {
    // add an entry to the map.
    Bot bot = bots.get(getCurrentBotName().toLowerCase());
    AIMLMap updateMap = bot.mapMap.get(mapName);
    key = key.toUpperCase().trim();
    if (updateMap != null) {
      updateMap.put(key, value);
      // persist to disk!
      updateMap.writeAIMLMap();
    } else {
      log.info("Unknown AIML map: {}.  A new MAP will be created. ", mapName);
      // dynamically create new maps?!
      AIMLMap newMap = new AIMLMap(mapName, bot);
      newMap.put(key, value);
      newMap.writeAIMLMap();
    }
  }

  public void setPredicate(String predicateName, String predicateValue) {
    setPredicate(getCurrentUserName(), predicateName, predicateValue);
  }

  public void setPredicate(String username, String predicateName, String predicateValue) {
    setPredicate(username, getCurrentBotName(), predicateName, predicateValue);
  }

  public void setPredicate(String username, String botName, String predicateName, String predicateValue) {
    Predicates preds = getChat(username, botName).predicates;
    preds.put(predicateName, predicateValue);
  }

  @Deprecated
  public void unsetPredicate(String username, String predicateName) {
    removePredicate(username, getCurrentBotName(), predicateName);
  }

  public String getPredicate(String predicateName) {
    return getPredicate(getCurrentUserName(), predicateName);
  }

  public String getPredicate(String username, String predicateName) {
    return getPredicate(username, getCurrentBotName(), predicateName);
  }

  public String getPredicate(String username, String botName, String predicateName) {
    Predicates preds = getChat(username, botName).predicates;
    return preds.get(predicateName);
  }

  /**
   * Only respond if the last response was longer than delay ms ago
   * 
   * @param userName
   *          - current username
   * @param text
   *          - text to get a response
   * @param delay
   *          - min amount of time that must have transpired since the last
   * @return the response
   * @throws IOException 
   */
  public Response getResponse(String userName, String text, Long delay) throws IOException {
    ChatData chatData = sessions.get(getCurrentBotName()).get(userName);
    long delta = System.currentTimeMillis() - chatData.lastResponseTime.getTime();
    if (delta > delay) {
      return getResponse(userName, text);
    } else {
      log.info("Skipping response, minimum delay since previous response not reached.");
      return null;
    }
  }

  public boolean isEnableAutoConversation() {
    return sessions.get(getCurrentBotName()).get(getCurrentUserName()).enableAutoConversation;
  }

  public boolean isProcessOOB() {
    return sessions.get(getCurrentBotName()).get(getCurrentUserName()).processOOB;
  }

  /**
   * Return a list of all patterns that the current AIML Bot knows to match
   * against.
   * 
   * @param botName
   *          the bots name from which to return it's patterns.
   * @return a list of all patterns loaded into the aiml brain
   */
  public ArrayList<String> listPatterns(String botName) {
    ArrayList<String> patterns = new ArrayList<String>();
    Bot bot = bots.get(botName.toLowerCase());
    for (Category c : bot.brain.getCategories()) {
      patterns.add(c.getPattern());
    }
    return patterns;
  }

  /**
   * Return the number of milliseconds since the last response was given -1 if a
   * response has never been given.
   * 
   * @return milliseconds
   */
  public long millisecondsSinceLastResponse() {
    ChatData chatData = sessions.get(getCurrentBotName()).get(getCurrentUserName());
    if (chatData.lastResponseTime == null) {
      return -1;
    }
    long delta = System.currentTimeMillis() - chatData.lastResponseTime.getTime();
    return delta;
  }

  @Override
  public void onText(String text) throws IOException {
    getResponse(text);
    // TODO: should we publish the response here?
  }

  private List<OOBPayload> processOOB(String text) {
    // Find any oob tags
    ArrayList<OOBPayload> payloads = OOBPayload.extractOOBPayloads(text, this);
    // invoke them all.
    for (OOBPayload payload : payloads) {
      // assumption is this is non blocking invoking!
      boolean oobRes = OOBPayload.invokeOOBPayload(payload, getName(), false);
      if (!oobRes) {
        // there was a failure invoking
        log.warn("Failed to invoke OOB/MRL tag : {}", OOBPayload.asOOBTag(payload));
      }
    }
    if (payloads.size() > 0) {
      return payloads;
    } else {
      return null;
    }
  }

  /*
   * If a response comes back that has an OOB Message, publish that separately
   */
  public String publishOOBText(String oobText) {
    return oobText;
  }

  /**
   * publish a response generated from a session in the programAB service.
   * 
   * @param response
   * @return
   */
  public Response publishResponse(Response response) {
    return response;
  }

  /**
   * Test only publishing point - for simple consumers
   */
  public String publishResponseText(Response response) {
    return response.msg;
  }

  @Override
  public String publishText(String text) {
    // TODO: this should not be done here.
    // clean up whitespaces & cariage return
    text = text.replaceAll("\\n", " ");
    text = text.replaceAll("\\r", " ");
    text = text.replaceAll("\\s{2,}", " ");
    return text;
  }

  public String publishRequest(String text) {
    return text;
  }

  public void reloadSession(String userName, String botName) throws IOException {
    reloadSession(getPath(), userName, botName);
  }

  /**
   * This method will close the current bot, and reload it from AIML It then
   * will then re-establish only the session associated with userName.
   * 
   * @param path
   * @param userName
   * @param botName
   * @throws IOException 
   */
  public void reloadSession(String path, String userName, String botName) throws IOException {

    if (sessions.containsKey(botName) && sessions.get(botName).containsKey(userName)) {
      // TODO: will garbage collection clean up the bot now ?
      // Or are there other handles to it?
      sessions.get(botName).remove(userName);
      log.info("{} session removed", sessions);
    }
    // reloading a session means to remove restart the bot and then start the
    // session.
    bots.remove(botName.toLowerCase());
    startSession(path, userName, botName);
    // Set<String> userSessions = sessions.get(botName).keySet();
    // TODO: we should make sure we keep the same path as before.
    // for (String user : userSessions ) {
    // startSession(path, user, getCurrentBotName());
    // }
  }

  /**
   * Persist the predicates for all known sessions in the robot.
   * 
   */
  public void savePredicates() throws IOException {
    for (String botName : sessions.keySet()) {
      // TODO: gael is seeing an exception here.. only was is if botName doesn't
      // have any sessions.
      if (sessions.containsKey(botName)) {
        for (String userName : sessions.get(botName).keySet()) {
          savePredicates(botName, userName);
        }
      } else {
        log.warn("Bot {} had no sessions to save predicates for.", botName);
      }
    }
    log.info("Done saving predicates.");
  }

  private void savePredicates(String botName, String userName) throws IOException {
    String sessionPredicateFilename = createSessionPredicateFilename(userName, botName);
    log.info("Bot : {} User : {} Predicates Filename : {} ", botName, userName, sessionPredicateFilename);
    File sessionPredFile = new File(sessionPredicateFilename);
    // if the file doesn't exist.. we should create it.. (and make the
    // directories for it.)
    if (!sessionPredFile.getParentFile().exists()) {
      // create the directory.
      log.info("Creating the directory {}", sessionPredFile.getParentFile());
      sessionPredFile.getParentFile().mkdirs();
    }
    Chat chat = getChat(userName, botName);
    // overwrite the original file , this should always be a full set.
    log.info("Writing predicate file for session {} {}", botName, userName);
    StringBuilder sb = new StringBuilder();
    for (String predicate : chat.predicates.keySet()) {
      String value = chat.predicates.get(predicate);
      sb.append(predicate + ":" + value + "\n");
    }
    FileWriter predWriter = new FileWriter(sessionPredFile, false);
    BufferedWriter bw = new BufferedWriter(predWriter);
    bw.write(sb.toString());
    bw.close();
    log.info("Saved predicates to file {}", sessionPredFile.getAbsolutePath());
  }

  public void setEnableAutoConversation(boolean enableAutoConversation) {
    sessions.get(getCurrentBotName()).get(getCurrentUserName()).enableAutoConversation = enableAutoConversation;
  }

  public void setMaxConversationDelay(int maxConversationDelay) {
    sessions.get(getCurrentBotName()).get(getCurrentUserName()).maxConversationDelay = maxConversationDelay;
  }

  public void setProcessOOB(boolean processOOB) {
    sessions.get(getCurrentBotName()).get(getCurrentUserName()).processOOB = processOOB;
  }

  public void startSession() throws IOException {
    startSession(currentUserName);
  }

  public void startSession(String username) throws IOException {
    startSession(username, getCurrentBotName());
  }

  public void startSession(String username, String botName) throws IOException {
    startSession(getPath(), username, botName);
  }

  public void startSession(String path, String userName, String botName) throws IOException {
    startSession(path, userName, botName, MagicBooleans.defaultLocale);
  }

  /**
   * Load the AIML 2.0 Bot config and start a chat session. This must be called
   * after the service is created.
   * 
   * @param path
   *          - he path to the ProgramAB directory where the bots aiml resides
   * @param userName
   *          - The new user name
   * @param botName
   *          - The name of the bot to load. (example: alice2)
   * @param locale
   *          - The locale of the bot to ensure the aiml is loaded (mostly for
   *          Japanese support)
   * @throws IOException 
   */
  public void startSession(String path, String userName, String botName, java.util.Locale locale) throws IOException {
    // if update the current user/bot name globally. (bring this bot/user
    // session to attention.)
    log.info("Start Session Path: {} User: {} Bot: {} Locale: {}", path, userName, botName, locale);
    
    File check = new File(path + fs + "bots" + fs + botName);
    if (!check.exists() || !check.isDirectory()) {
      String invalid = String.format("%s could not load aiml. %s is not a valid bot directory", getName(), check.getAbsolutePath());
      error(invalid);
      throw new IOException(invalid);
    }
    
    updateCurrentSession(userName, botName);
    // Session is between a user and a bot. key is compound.
    if (sessions.containsKey(botName) && sessions.get(botName).containsKey(userName)) {
      info("Session %s %s already created", botName, userName);
      return;
    }
    setReady(false);
    info("Starting chat session path: %s username: %s botname: %s", path, userName, botName);
    setPath(path);
    setCurrentBotName(botName);
    setCurrentUserName(userName);
    // check if we've already started this bot.
    Bot bot = bots.get(botName.toLowerCase());
    if (bot == null) {
      // create a new bot if we haven't started it yet.
      bot = new Bot(botName, path, locale);
      // Hijack all the SRAIX requests and implement them as a synchronous call
      // to
      // a service to return a string response for programab...
      bot.setSraixHandler(new MrlSraixHandler(this));
      // put the bot into the bots map/cache referenced by it's lowercase
      // botName.
      bots.put(botName.toLowerCase(), bot);
    }

    // create a chat session from the bot.
    Chat chat = new Chat(bot);
    // load session specific predicates, these override the default ones.
    String sessionPredicateFilename = createSessionPredicateFilename(userName, botName);
    chat.predicates.getPredicateDefaults(sessionPredicateFilename);

    if (!sessions.containsKey(botName)) {
      // initialize the sessions for this bot
      HashMap<String, ChatData> newSet = new HashMap<String, ChatData>();
      sessions.put(botName, newSet);
    }
    // put the current user session in the sessions map (overwrite if it was
    // already there.
    sessions.get(botName).put(userName, new ChatData(chat));
    initializeChatSession(userName, botName, chat);
    // this.currentBotName = botName;
    log.info("Started session for bot name:{} , username:{}", botName, userName);
    setReady(true);
  }

  private void initializeChatSession(String userName, String botName, Chat chat) {
    // lets test if the robot knows the name of the person in the session
    String name = chat.predicates.get("name").trim();
    // TODO: this implies that the default value for "name" is default
    // "Friend"
    if (name == null || "Friend".equalsIgnoreCase(name) || "unknown".equalsIgnoreCase(name)) {
      // TODO: find another interface that's simpler to use for this
      // create a string that represents the predicates file
      String inputPredicateStream = "name:" + userName;
      // load those predicates
      chat.predicates.getPredicateDefaultsFromInputStream(FileIO.toInputStream(inputPredicateStream));
    }
    // TODO move this inside high level :
    // it is used to know the last username...
    if (sessions.get(botName).containsKey("default") && !userName.equals("default")) {
      setPredicate("default", "lastUsername", userName);
      // robot surname is stored inside default.predicates, not inside
      // system.prop
      setPredicate(userName, "botname", getPredicate("default", "botname"));
      try {
        savePredicates();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  public void addCategory(Category c) {
    Bot bot = bots.get(getCurrentBotName().toLowerCase());
    bot.brain.addCategory(c);
  }

  public void addCategory(String pattern, String template, String that) {
    log.info("Adding category {} to respond with {} for the that context {}", pattern, template, that);
    // TODO: expose that / topic / filename?!
    int activationCnt = 0;
    String topic = "*";
    // TODO: what is this used for? can we tell the bot to only write out
    // certain aiml files and leave the rest as
    // immutable
    String filename = "mrl_added.aiml";
    // clean the pattern
    pattern = pattern.trim().toUpperCase();
    Category c = new Category(activationCnt, pattern, that, topic, template, filename);
    addCategory(c);
  }

  public void addCategory(String pattern, String template) {
    addCategory(pattern, template, "*");
  }

  /**
   * Use startSession instead.
   * @throws IOException 
   */
  @Deprecated
  public boolean setUsername(String username) throws IOException {
    startSession(getPath(), username, getCurrentBotName());
    return true;
  }

  public void writeAIML() {
    // TODO: revisit this method to make sure
    for (Bot bot : bots.values()) {
      if (bot != null) {
        bot.writeAIMLFiles();
      }
    }
  }

  /**
   * writeAndQuit will write brain to disk For learn.aiml is concerned
   */
  public void writeAndQuit() {
    // write out all bots aiml & save all predicates for all sessions?
    for (Bot bot : bots.values()) {
      if (bot == null) {
        log.info("no bot - don't need to write and quit");
        continue;
      }
      try {
        savePredicates();
        // important to save learnf.aiml
        writeAIML();
      } catch (IOException e1) {
        log.error("saving predicates threw", e1);
      }
      bot.writeQuit();
    }
  }

  public String setPath(String path) {
    if (path != null && !path.equals(this.path)) {
      this.path = path;
      broadcastState();
    }
    return this.path;
  }

  public void setCurrentBotName(String currentBotName) {
    this.currentBotName = currentBotName;
    broadcastState();
  }

  public void setVisualDebug(Boolean visualDebug) {
    this.visualDebug = visualDebug;
    broadcastState();
  }

  public Boolean getVisualDebug() {
    return visualDebug;
  }

  public void setCurrentUserName(String currentUserName) {
    this.currentUserName = currentUserName;
    broadcastState();
  }

  public String getPath() {
    return path;
  }

  public String getCurrentUserName() {
    return currentUserName;
  }

  public String getCurrentBotName() {
    return currentBotName;
  }

  /**
   * @return the sessions
   */
  public HashMap<String, HashMap<String, ChatData>> getSessions() {
    return sessions;
  }

  /**
   * This method can be used to get a listing of all bots available in the bots
   * directory.
   * 
   * @return
   */
  public HashSet<String> getBots() {
    HashSet<String> availableBots = new HashSet<String>();
    File programAbDir = new File(String.format("%s%sbots", getPath(), File.separator));
    if (!programAbDir.exists() || !programAbDir.isDirectory()) {
      log.info("%s does not exist !!!");
    } else {
      File[] listOfFiles = programAbDir.listFiles();
      for (int i = 0; i < listOfFiles.length; i++) {
        if (listOfFiles[i].isFile()) {
        } else if (listOfFiles[i].isDirectory()) {
          availableBots.add(listOfFiles[i].getName());
        }
      }
    }
    return availableBots;
  }

  public void attach(Attachable attachable) {
    if (attachable instanceof TextPublisher) {
      addTextPublisher((TextPublisher) attachable);
    } else if (attachable instanceof TextListener) {
      addListener("publishText", attachable.getName(), "onText");
    } else {
      log.error("don't know how to attach a {}", attachable.getName());
    }
  }

  @Override
  public void stopService() {
    if (writeOnExit) {
      writeAndQuit();
    }
    super.stopService();
  }

  public boolean setPeerSearch(boolean b) {
    peerSearch = b;
    return peerSearch;
  }

  @Override
  public void startService() {
    super.startService();
    if (peerSearch) {
      startPeer("search");
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
    ServiceType meta = new ServiceType(ProgramAB.class.getCanonicalName());
    meta.addDescription("AIML 2.0 Reference interpreter based on Program AB");
    meta.addCategory("ai");

    // FIXME - add Wikipedia local search !!
    meta.addPeer("search", "GoogleSearch", "replacement for handling pannous sriax requests");

    // TODO: renamed the bots in the program-ab-data folder to prefix them so we
    // know they are different than the inmoov bots.
    // each bot should have their own name, it's confusing that the inmoov bots
    // are named en-US and so are the program ab bots.
    meta.addDependency("program-ab", "program-ab-data", "1.1", "zip");
    meta.addDependency("program-ab", "program-ab-kw", "0.0.8.4");
    meta.addDependency("org.json", "json", "20090211");
    // used by FileIO
    meta.addDependency("commons-io", "commons-io", "2.5");
    // TODO: This is for CJK support in ProgramAB move this into the published
    // POM for ProgramAB so they are pulled in transiently.
    meta.addDependency("org.apache.lucene", "lucene-analyzers-common", "8.4.1");
    meta.addDependency("org.apache.lucene", "lucene-analyzers-kuromoji", "8.4.1");
    meta.addCategory("ai", "control");
    return meta;
  }

  public static void main(String args[]) {
    LoggingFactory.init("INFO");
    Runtime.start("gui", "SwingGui");
    Runtime.start("brain", "ProgramAB");
    WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
    webgui.autoStartBrowser(false);
    webgui.startService();
  }

  @Override /* FIXME - just do this once in abstract */
  public void attachTextListener(TextListener service) {
    if (service == null) {
      log.warn("{}.attachTextListener(null)");
      return;
    }
    addListener("publishText", service.getName());
  }

  @Override
  public void attachTextPublisher(TextPublisher service) {
    if (service == null) {
      log.warn("{}.attachTextPublisher(null)");
      return;
    }
    subscribe(service.getName(), "publishText");
  }

  public SearchPublisher getSearch() {
    return (SearchPublisher)getPeer("search");
  }

  @Override
  public void setLocale(String code) {
   this.locale = new Locale(code);
  }

  @Override
  public String getLanguage() {
   return locale.getLanguage();
  }

  @Override
  public Locale getLocale() {
    return locale;
  }

  @Override
  public Map<String, Locale> getLocales() {
	  // FIXME should be based on bots found ???
   return Locale.getLocaleMap("en-US", "fr-FR", "es-ES", "de-DE", "nl-NL", "ru-RU", "hi-IN","it-IT", "fi-FI","pt-PT");
  }

@Override
public String publishLog(String msg) {
	return msg;
}

}