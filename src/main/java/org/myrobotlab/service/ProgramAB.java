package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.alicebot.ab.AIMLMap;
import org.alicebot.ab.AIMLSet;
import org.alicebot.ab.Bot;
import org.alicebot.ab.Category;
import org.alicebot.ab.MagicBooleans;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.logging.SimpleLogPublisher;
import org.myrobotlab.programab.BotInfo;
import org.myrobotlab.programab.Response;
import org.myrobotlab.programab.Session;
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

  private static final long serialVersionUID = 1L;

  transient public final static Logger log = LoggerFactory.getLogger(ProgramAB.class);

  /**
   * the Bots !
   */
  Map<String, BotInfo> bots = new TreeMap<>();

  /**
   * Mapping a bot to a userName and chat session
   */
  Map<String, Session> sessions = new TreeMap<>();

  /**
   * initial bot name - this bot comes with ProgramAB this will be the result of
   * whatever is scanned in the constructor
   */
  String currentBotName = null;

  /**
   * default user name chatting with the bot
   */
  String currentUserName = "default";

  /**
   * save predicates - default every 5 minutes
   */
  public int savePredicatesInterval = 300000;

  /**
   * display processing and logging
   */
  boolean visualDebug = true;

  /**
   * start GoogleSearch (a peer) instead of sraix web service which is down or
   * problematic much of the time
   */
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
    // botPaths = initBotPaths();

    // 1. scan "resource/ProgramAB" for valid bot directories
    List<File> resourceBots = scanForBots("resource/ProgramAB");

    // 2. copy any bot in "resource/ProgramAB/{botName}" not found in
    // "data/ProgramAB/{botName}"
    for (File file : resourceBots) {
      String botName = getBotName(file);
      File dataBotDir = new File(FileIO.gluePaths("data/ProgramAB", botName));
      if (dataBotDir.exists()) {
        log.info("found data/ProgramAB/{} not copying", botName);
      } else {
        log.info("will copy new data/ProgramAB/{}", botName);
        try {
          FileIO.copy(file, dataBotDir);
        } catch (Exception e) {
          error(e);
        }
      }
    }

    // 3. addPath for all bots found in "data/ProgramAB/"
    List<File> dataBots = scanForBots("data/ProgramAB");
    for (File file : dataBots) {
      addBotPath(file.getAbsolutePath());
    }

    addTask("savePredicates", savePredicatesInterval, 0, "savePredicates");
    logPublisher = new SimpleLogPublisher(this);
    logPublisher.filterClasses(new String[] { "org.alicebot.ab.Graphmaster", "org.alicebot.ab.MagicBooleans", "class org.myrobotlab.programab.MrlSraixHandler" });
    logPublisher.start();

  }

  public String getBotName(File file) {
    return file.getName();
  }

  /**
   * function to scan the parent directory for bot directories, and return a
   * list of valid bots to be added with addBot(path)
   * 
   * @param path
   * @return
   */
  public List<File> scanForBots(String path) {
    List<File> botDirs = new ArrayList<>();
    File parent = new File(path);
    if (!parent.exists()) {
      warn("cannot scan for bots %s does not exist");
    }
    if (!parent.isDirectory()) {
      warn("%s is not a valid directory");
    }

    File[] files = parent.listFiles();
    for (File file : files) {
      if (checkIfValid(file)) {
        info("found %s bot directory", file.getName());
        botDirs.add(file);
      }
    }
    return botDirs;
  }

  /**
   * checks to see if valid bot dir
   * 
   * @param botDir
   * @return
   */
  public boolean checkIfValid(File botDir) {
    if (botDir.exists() && botDir.isDirectory()) {
      return true;
    }
    return false;
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

  public int getMaxConversationDelay() {
    return getCurrentSession().maxConversationDelay;
  }

  /**
   * This is the main method that will ask for the current bot's chat session to
   * respond to It returns a Response object.
   * 
   * @param text
   * @return
   * @throws IOException
   */
  public Response getResponse(String text) {
    return getResponse(getCurrentUserName(), text);
  }

  /**
   * This method has the side effect of switching which bot your are currently
   * chatting with.
   * 
   * @param userName
   *          - the query string to the bot brain
   * @param text
   *          - the user that is sending the query
   * @return the response for a user from a bot given the input text.
   * @throws IOException
   */
  public Response getResponse(String userName, String text) {
    return getResponse(userName, getCurrentBotName(), text);
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
  public Response getResponse(String userName, String botName, String text) {
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
   * 
   *           TODO - no one cares about starting sessions, starting a new
   *           session could be as simple as providing a different username, or
   *           botname in getResponse and a necessary session could be created
   * 
   */
  public Response getResponse(String userName, String botName, String text, boolean updateCurrentSession) {
    Session session = getSession(userName, botName);

    // if a session with this user and bot does not exist
    // attempt to create it
    if (session == null) {
      session = startSession(userName, botName);
    }

    // update the current session if we want to change which bot is at
    // attention.
    if (updateCurrentSession) {
      setCurrentUserName(userName);
      setCurrentBotName(botName);
    }

    // Get the actual bots aiml based response for this session
    Response response = session.getResponse(text);

    // EEK! clean up the API!
    invoke("publishRequest", text); // publisher used by uis
    invoke("publishResponse", response);
    invoke("publishText", response.msg);
    info("to: %s - %s", userName, response);
    return response;
  }

  @Deprecated /* should not be needed */
  private Bot getBot(String botName) {
    return bots.get(botName).getBot();
  }

  private BotInfo getBotInfo(String botName) {
    return bots.get(botName);
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

  public Session getSession() {
    return getSession(getCurrentUserName(), getCurrentBotName());
  }

  public Session getSession(String userName, String botName) {
    String sessionKey = getSessionKey(userName, botName);
    if (sessions.containsKey(sessionKey)) {
      return sessions.get(sessionKey);
    } else {
      warn("%s session does not exist", sessionKey);
      return null;
    }
  }

  public void removePredicate(String userName, String predicateName) {
    removePredicate(userName, getCurrentBotName(), predicateName);
  }

  public void removePredicate(String userName, String botName, String predicateName) {
    getSession(userName, botName).remove(predicateName);
  }

  /**
   * Add a value to a set for the current session
   * 
   * @param setName
   * @param setValue
   */
  public void addToSet(String setName, String setValue) {
    // add to the set for the bot.
    Bot bot = getBot(getCurrentBotName());
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
    Bot bot = getBot(getCurrentBotName());
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

  public void setPredicate(String userName, String predicateName, String predicateValue) {
    setPredicate(userName, getCurrentBotName(), predicateName, predicateValue);
  }

  public void setPredicate(String userName, String botName, String predicateName, String predicateValue) {
    getSession(userName, botName).setPredicate(predicateName, predicateValue);
  }

  @Deprecated
  public void unsetPredicate(String userName, String predicateName) {
    removePredicate(userName, getCurrentBotName(), predicateName);
  }

  public String getPredicate(String predicateName) {
    return getPredicate(getCurrentUserName(), predicateName);
  }

  public String getPredicate(String userName, String predicateName) {
    return getPredicate(userName, getCurrentBotName(), predicateName);
  }

  public String getPredicate(String userName, String botName, String predicateName) {
    return getSession(userName, botName).getPredicate(predicateName);
  }

  /**
   * Only respond if the last response was longer than delay ms ago
   * 
   * @param userName
   *          - current userName
   * @param text
   *          - text to get a response
   * @param delay
   *          - min amount of time that must have transpired since the last
   * @return the response
   * @throws IOException
   */
  public Response troll(String userName, String text, Long delay) throws IOException {
    Session session = getSession(userName, getCurrentBotName());
    long delta = System.currentTimeMillis() - session.lastResponseTime.getTime();
    if (delta > delay) {
      return getResponse(userName, text);
    } else {
      log.info("Skipping response, minimum delay since previous response not reached.");
      return null;
    }
  }

  public boolean isEnableAutoConversation() {
    return getSession().enableAutoConversation;
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
    Bot bot = getBot(botName);
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
    Session session = getSession();
    if (session.lastResponseTime == null) {
      return -1;
    }
    long delta = System.currentTimeMillis() - session.lastResponseTime.getTime();
    return delta;
  }

  @Override
  public void onText(String text) throws IOException {
    getResponse(text);
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

  /**
   * This method will close the current bot, and reload it from AIML It then
   * will then re-establish only the session associated with userName.
   * 
   * @param path
   * @param userName
   * @param botName
   * @throws IOException
   */
  public void reloadSession(String userName, String botName) throws IOException {
    Session session = getSession(userName, botName);
    session.reload();
  }

  /**
   * Save all the predicates for all known sessions.
   */
  public void savePredicates() throws IOException {
    for (Session session : sessions.values()) {
      session.savePredicates();
    }
  }

  public void setEnableAutoConversation(boolean enableAutoConversation) {
    getSession().enableAutoConversation = enableAutoConversation;
  }

  public void setMaxConversationDelay(int maxConversationDelay) {
    getSession().maxConversationDelay = maxConversationDelay;
  }

  public void setProcessOOB(boolean processOOB) {
    getSession().processOOB = processOOB;
  }

  public Session startSession() throws IOException {
    return startSession(currentUserName);
  }

  // FIXME - it should just set the current userName only
  public Session startSession(String userName) throws IOException {
    return startSession(userName, getCurrentBotName());
  }

  public Session startSession(String userName, String botName) {
    return startSession(null, userName, botName, MagicBooleans.defaultLocale);
  }

  @Deprecated /* path included for legacy */
  public Session startSession(String path, String userName, String botName) {
    return startSession(path, userName, botName, MagicBooleans.defaultLocale);
  }

  /**
   * Load the AIML 2.0 Bot config and start a chat session. This must be called
   * after the service is created.
   * 
   * @param path
   *          - he path to the ProgramAB directory where the bots aiml resides
   *          FIXME - path is not needed
   * @param userName
   *          - The new user name
   * @param botName
   *          - The name of the bot to load. (example: alice2)
   * @param locale
   *          - The locale of the bot to ensure the aiml is loaded (mostly for
   *          Japanese support)
   *          FIXME - local is defined in the bot, specifically config/mrl.properties
   * @throws IOException
   */
  
  @Deprecated /* 1. I question the need to expose this externally at all - if the user uses getResponse(username, botname, text)
  then a session can be auto-started - there is really no reason not to auto-start.  2. path is completely invalid here 3.
  Locale is completely invalid - it is now part of the bot description in mrl.properties and shouldn't be defined externally,
  unles its pulled from Runtime*/
  public Session startSession(@Deprecated String path, String userName, String botName, @Deprecated java.util.Locale locale) {

    /* not wanted or needed 
    if (path != null) {
      addBotPath(path);
    }
    */

    Session session = getSession(userName, botName);

    if (session != null) {
      log.info("session {} already exists - will use it", getSessionKey(userName, botName));
      setCurrentSession(userName, botName);
      return session;
    }

    // create a new session
    log.info("creating new sessions");
    BotInfo botInfo = getBotInfo(botName);
    if (botInfo == null) {
      error("cannot create session %s is not a valid botName", botName);
      return null;
    }

    session = new Session(this, userName, botInfo);
    sessions.put(getSessionKey(userName, botName), session);

    log.info("Started session for bot botName:{} , userName:{}", botName, userName);
    setCurrentSession(userName, botName);
    return session;
  }

  /**
   * setting the current session is equivalent to setting current 
   * user name and current bot name
   * 
   * @param userName
   * @param botName
   */
  public void setCurrentSession(String userName, String botName) {
    setCurrentUserName(userName);
    setCurrentBotName(botName);    
  }

  public void addCategory(Category c) {
    Bot bot = getBot(getCurrentBotName());
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

  public void writeAIML() {
    // TODO: revisit this method to make sure
    for (BotInfo bot : bots.values()) {
      if (bot.isActive()) {
        bot.writeAIMLFiles();
      }
    }
  }

  /**
   * writeAndQuit will write brain to disk For learn.aiml is concerned
   */
  public void writeAndQuit() {
    // write out all bots aiml & save all predicates for all sessions?
    for (BotInfo bot : bots.values()) {
      if (bot.isActive()) {
        try {
          savePredicates();
          // important to save learnf.aiml
          writeAIML();
          bot.writeQuit();
        } catch (IOException e1) {
          log.error("saving predicates threw", e1);
        }
      }
    }
  }

  /**
   * Verifies and adds a new path to the search directories for bots
   * 
   * @param path
   * @return
   */
  public String addBotPath(String path) {
    // verify the path is valid
    File botPath = new File(path);
    File verifyAiml = new File(FileIO.gluePaths(path, "aiml"));
    if (botPath.exists() && botPath.isDirectory() && verifyAiml.exists() && verifyAiml.isDirectory()) {
      BotInfo botInfo = new BotInfo(this, botPath);
      // key'ing on "path" probably would be better and only displaying "name"
      // then there would be no put/collisions only duplicate names
      // (preferrable)
      bots.put(botInfo.name, botInfo);
      setCurrentBotName(botInfo.name);
      broadcastState();
    } else {
      error("invalid bot path - a bot must be a directory with a subdirectory named \"aiml\"");
      return null;
    }
    return path;
  }

  @Deprecated /* for legacy - use addBotPath */
  public String setPath(String path) {

    if (path == null) {
      error("set path can not be null");
      return null;
    }
    
    File check = new File(path);
    if (!check.exists() || !check.isDirectory()) {
      error("invalid directory %s", path);
      return null;
    }
    
    check = new File(FileIO.gluePaths(path, "bots"));
    
    if (check.exists() && check.isDirectory()) {      
      for (File f : check.listFiles()) {
        addBotPath(f.getAbsolutePath());
      }
      return path;
    }


    return addBotPath(path);
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

  public Session getCurrentSession() {
    return sessions.get(getSessionKey(getCurrentUserName(), getCurrentBotName()));
  }

  public String getSessionKey(String userName, String botName) {
    return String.format("%s <-> %s", userName, botName);
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
  public Map<String, Session> getSessions() {
    return sessions;
  }

  /**
   * Initialize all known paths of a bot. Each path is "named" by the filename
   * of the directory. This is placed in a map, so there can be collisions.
   * Collisions are resolved by the following priority.
   * 
   * <pre>
   *  /resource/ProgramAB is lowest priorty
   *  /data/ProgramAB is higher priority
   *  /../ProgramAB/
   * </pre>
   * 
   * @return
   */
  public Set<String> initBotPaths() {

    Set<String> paths = new TreeSet<>();

    // paths are added in reverse priority order, since newly added paths
    // replace
    // lower priority ones

    // check for resource bots in /data/ProgramAB dir
    File resourceBots = new File(getResourceDir());

    if (!resourceBots.exists() || !resourceBots.isDirectory()) {
      log.info("{} does not exist !!!", resourceBots);
      log.info("you can add a bot directory with programab.addBot(\"path/to/bot\")", resourceBots);
    } else {
      File[] listOfFiles = resourceBots.listFiles();
      for (int i = 0; i < listOfFiles.length; i++) {
        if (listOfFiles[i].isFile()) {
        } else if (listOfFiles[i].isDirectory()) {
          paths.add(listOfFiles[i].getAbsolutePath());
        }
      }
    }

    // check for 'local' bots in /data/ProgramAB dir

    // check for dev bots
    if (getResourceDir().startsWith("src")) {
      log.info("in dev mode resourceDir starts with src");
      // automatically look in ../ProgramAB for the cloned repo
      // look for dev paths in ../ProgramAB
      File devRepoCheck = new File("../ProgramAB/resource/ProgramAB/bots");
      if (devRepoCheck.exists() && devRepoCheck.isDirectory()) {
        log.info("found repo {} adding bot paths", devRepoCheck.getAbsoluteFile());
        File[] listOfFiles = devRepoCheck.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {
          if (listOfFiles[i].isFile()) {
          } else if (listOfFiles[i].isDirectory()) {
            paths.add(listOfFiles[i].getAbsolutePath());
          }
        }
      } else {
        log.error("ProgramAB is a service module clone it at the same level as myrobotlab");
      }
    }

    return paths;
  }

  /**
   * This method can be used to get a listing of all bots available in the bots
   * directory.
   * 
   * @return
   */
  public List<String> getBots() {

    List<String> names = new ArrayList<String>();
    for (String name : bots.keySet()) {
      names.add(name);
    }
    return names;
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
    super.stopService();
    writeAndQuit();
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
    ServiceType meta = new ServiceType(ProgramAB.class);
    meta.addDescription("AIML 2.0 Reference interpreter based on Program AB");
    meta.addCategory("ai");

    // FIXME - add Wikipedia local search !!
    meta.addPeer("search", "GoogleSearch", "replacement for handling pannous sriax requests");

    // TODO: renamed the bots in the program-ab-data folder to prefix them so we
    // know they are different than the inmoov bots.
    // each bot should have their own name, it's confusing that the inmoov bots
    // are named en-US and so are the program ab bots.

    // meta.addDependency("program-ab", "program-ab-data", "1.2", "zip");
    // meta.addDependency("program-ab", "program-ab-kw", "0.0.8.5");

    meta.addDependency("program-ab", "program-ab-data", null, "zip");
    meta.addDependency("program-ab", "program-ab-kw", "0.0.8.5");

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
    // Runtime.start("gui", "SwingGui");
    ProgramAB brain = (ProgramAB) Runtime.start("brain", "ProgramAB");
    Response response = brain.getResponse("Hi, How are you?");
    log.info(response.toString());
    response = brain.getResponse("what's new?");
    log.info(response.toString());
    /*
     * WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
     * webgui.autoStartBrowser(false); webgui.startService();
     */
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
    return (SearchPublisher) getPeer("search");
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
    return Locale.getLocaleMap("en-US", "fr-FR", "es-ES", "de-DE", "nl-NL", "ru-RU", "hi-IN", "it-IT", "fi-FI", "pt-PT");
  }

  @Override
  public String publishLog(String msg) {
    return msg;
  }

  public BotInfo getBotInfo() {
    return getBotInfo(currentBotName);
  }

}