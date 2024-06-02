package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.alicebot.ab.Chat;
import org.alicebot.ab.MagicBooleans;
import org.alicebot.ab.ProgramABListener;
import org.apache.commons.lang3.StringUtils;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.image.Util;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.logging.SimpleLogPublisher;
import org.myrobotlab.programab.BotInfo;
import org.myrobotlab.programab.PredicateEvent;
import org.myrobotlab.programab.Response;
import org.myrobotlab.programab.Session;
import org.myrobotlab.programab.handlers.oob.OobProcessor;
import org.myrobotlab.service.config.ProgramABConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.data.Locale;
import org.myrobotlab.service.data.TopicChange;
import org.myrobotlab.service.data.Utterance;
import org.myrobotlab.service.interfaces.LocaleProvider;
import org.myrobotlab.service.interfaces.LogPublisher;
import org.myrobotlab.service.interfaces.ResponsePublisher;
import org.myrobotlab.service.interfaces.SearchPublisher;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.myrobotlab.service.interfaces.UtteranceListener;
import org.myrobotlab.service.interfaces.UtterancePublisher;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

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
public class ProgramAB extends Service<ProgramABConfig>
    implements TextListener, TextPublisher, LocaleProvider, LogPublisher, ProgramABListener, UtterancePublisher, UtteranceListener, ResponsePublisher {

  /**
   * default file name that aiml categories comfing from matching a learnf tag
   * will be written to.
   */
  private static final String LEARNF_AIML_FILE = "learnf.aiml";

  private static final long serialVersionUID = 1L;

  /**
   * useGlobalSession true will allow the sleep member to control session focus
   */
  protected boolean useGlobalSession = false;

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
   * start GoogleSearch (a peer) instead of sraix web service which is down or
   * problematic much of the time
   */
  boolean peerSearch = true;

  transient SimpleLogPublisher logPublisher = null;

  final transient private OobProcessor oobProcessor;

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
    oobProcessor = new OobProcessor(this);
  }

  public String getBotName(File file) {
    return file.getName();
  }

  /**
   * function to scan the parent directory for bot directories, and return a
   * list of valid bots to be added with addBot(path)
   * 
   * @param path
   *          path to search
   * @return list of bot dirs
   */
  public List<File> scanForBots(String path) {
    List<File> botDirs = new ArrayList<>();
    File parent = new File(path);
    if (!parent.exists()) {
      warn("cannot scan for bots %s does not exist", path);
      return botDirs;
    }

    if (!parent.isDirectory()) {
      warn("%s is not a valid directory", parent);
      return botDirs;
    }

    File[] files = parent.listFiles();
    for (File file : files) {
      if (checkIfValid(file)) {
        info("found %s bot directory", file.getName());
        botDirs.add(file);
        addBotPath(file.getAbsolutePath());
      }
    }
    return botDirs;
  }

  /**
   * @param botDir
   *          checks to see if valid bot dir
   * @return true/false
   */
  public boolean checkIfValid(File botDir) {
    File aiml = new File(FileIO.gluePaths(botDir.getAbsolutePath(), "aiml"));
    if (aiml.exists() && aiml.isDirectory()) {
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

  @Deprecated /* use standard attachTextListener */
  public void addTextListener(SpeechSynthesis service) {
    addListener("publishText", service.getName(), "onText");
  }

  @Deprecated /* use standard attachTextPublisher */
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
   *          the input utterance
   * @return the programab response
   * 
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
   */
  public Response getResponse(String userName, String text) {
    return getResponse(userName, getCurrentBotName(), text);
  }

  /**
   * Full get response method . Using this method will update the current
   * user/bot name if different from the current session.
   * 
   * @param userName
   *          username
   * @param botName
   *          bot name
   * @param text
   *          utterace
   * @return programab response to utterance
   * 
   */
  public Response getResponse(String userName, String botName, String text) {
    return getResponse(userName, botName, text, true);
  }

  /**
   * Gets a response and optionally update if this is the current bot session
   * that's active globally.
   * 
   * @param userName
   *          username
   * @param botName
   *          botname
   * @param text
   *          utterance
   * 
   * @param updateCurrentSession
   *          (specify if the currentbot/currentuser name should be updated in
   *          the programab service.)
   * @return the response
   * 
   *         TODO - no one cares about starting sessions, starting a new session
   *         could be as simple as providing a different username, or botname in
   *         getResponse and a necessary session could be created
   * 
   */
  public Response getResponse(String userName, String botName, String text, boolean updateCurrentSession) {
    Session session = getSession(userName, botName);

    // if a session with this user and bot does not exist
    // attempt to create it
    if (session == null) {
      session = startSession(userName, botName);
      if (session == null) {
        error("username or bot name not valid %s %s", userName, botName);
        return null;
      }
    }

    // update the current session if we want to change which bot is at
    // attention.
    if (updateCurrentSession) {
      
      boolean sessionChanged =  (!userName.equals(config.currentUserName) || !botName.equals(config.currentBotName));
      
      setCurrentUserName(userName);
      setCurrentBotName(botName);
      
      if (sessionChanged) {
        invoke("publishSession", getSessionKey(userName, botName));
      }
    }

    // Get the actual bots aiml based response for this session
    log.info("getResponse({})", text);
    Response response = session.getResponse(text);

    // EEK! clean up the API!
    invoke("publishRequest", text); // publisher used by uis
    invoke("publishResponse", response);
    invoke("publishText", response.msg);

    return response;
  }

  private Bot getBot(String botName) {
    return bots.get(botName).getBot();
  }

  private BotInfo getBotInfo(String botName) {
    if (botName == null) {
      error("getBotinfo(null) not valid");
      return null;
    }
    BotInfo botInfo = bots.get(botName);
    if (botInfo == null) {
      error("botInfo(%s) is null", botName);
      return null;
    }

    return botInfo;
  }

  /**
   * This method specifics how many times the robot will respond with the same
   * thing before forcing a different (default?) response instead.
   * 
   * @param val
   *          foo
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
   *          name of the set
   * @param setValue
   *          value to add to the set
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
    Session session = getSession(userName, botName);
    if (session != null) {
      session.setPredicate(predicateName, predicateValue);
    }
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
    Session s = getSession(userName, botName);
    if (s == null) {
      // If that session doesn't currently exist, let's start it.
      s = startSession(userName, botName);
      if (s == null) {
        log.warn("Error starting programAB session between bot {} and user {}", userName, botName);
        return null;
      }
    }
    return s.getPredicate(predicateName);
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
   *           boom
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
    return getSession().enableTrolling;
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
   * @param response
   *          publish a response generated from a session in the programAB
   *          service.
   * @return the response
   * 
   */
  @Override
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
   * publish the contents of the mrl tag from an oob message in the aiml. The
   * result of this is displayed in the chatbot debug console.
   * 
   * @param oobText
   *          the out of band text to publish
   * @return oobtext
   * 
   */
  public String publishOOBText(String oobText) {
    return oobText;
  }

  /**
   * This method will close the current bot, and reload it from AIML It then
   * will then re-establish only the session associated with userName.
   * 
   * @param userName
   *          username for the session
   * @param botName
   *          the bot name being chatted with
   * @throws IOException
   *           boom
   * 
   */
  public void reloadSession(String userName, String botName) throws IOException {
    Session session = getSession(userName, botName);
    if (session != null) {
      session.reload();
      info("reloaded session %s <-> %s ", userName, botName);
    }
  }

  /**
   * Get the current session predicates
   * 
   * @return
   */
  public Map<String, String> getPredicates() {
    return getPredicates(config.currentUserName, config.currentBotName);
  }

  /**
   * Get all current predicates names and their values for the current session
   * 
   * @return
   */
  public Map<String, String> getPredicates(String userName, String botName) {
    Session session = getSession(userName, botName);
    if (session != null) {
      return session.getPredicates();
    }
    return new TreeMap<>();
  }

  /**
   * Save all the predicates for all known sessions.
   */
  public void savePredicates() {
    for (Session session : sessions.values()) {
      session.savePredicates();
    }
  }

  public void setEnableAutoConversation(boolean enableAutoConversation) {
    getSession().enableTrolling = enableAutoConversation;
  }

  public void setMaxConversationDelay(int maxConversationDelay) {
    getSession().maxConversationDelay = maxConversationDelay;
  }

  public void setProcessOOB(boolean processOOB) {
    getSession().processOOB = processOOB;
  }

  /**
   * set a bot property - the result will be serialized to config/properties.txt
   * 
   * @param name
   *          property name to set for current bot/session
   * @param value
   *          value to set for current bot/session
   */
  public void setBotProperty(String name, String value) {
    setBotProperty(getCurrentBotName(), name, value);
  }

  /**
   * set a bot property - the result will be serialized to config/properties.txt
   * 
   * @param botName
   *          bot name
   * @param name
   *          bot property name
   * @param value
   *          value to set the property too
   */
  public void setBotProperty(String botName, String name, String value) {
    info("setting %s property %s:%s", getCurrentBotName(), name, value);
    BotInfo botInfo = getBotInfo(botName);
    name = name.trim();
    value = value.trim();
    botInfo.setProperty(name, value);
  }

  public void removeBotProperty(String name) {
    removeBotProperty(getCurrentBotName(), name);
  }

  public void removeBotProperty(String botName, String name) {
    info("removing %s property %s", getCurrentBotName(), name);
    BotInfo botInfo = getBotInfo(botName);
    botInfo.removeProperty(name);
  }

  public Session startSession() throws IOException {
    return startSession(config.currentUserName);
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
   *          - the path to the ProgramAB directory where the bots aiml and
   *          config reside
   * @param userName
   *          - The new user name
   * @param botName
   *          - The name of the bot to load. (example: alice2)
   * @param locale
   *          - The locale of the bot to ensure the aiml is loaded (mostly for
   *          Japanese support) FIXME - local is defined in the bot,
   *          specifically config/mrl.properties
   * 
   *          reasons to deprecate:
   * 
   *          1. I question the need to expose this externally at all - if the
   *          user uses getResponse(username, botname, text) then a session can
   *          be auto-started - there is really no reason not to auto-start.
   * 
   *          2. path is completely invalid here
   * 
   *          3. Locale is completely invalid - it is now part of the bot
   *          description in mrl.properties and shouldn't be defined externally,
   *          unles its pulled from Runtime
   * @return the session that is started
   */

  public Session startSession(String path, String userName, String botName, java.util.Locale locale) {

    /*
     * not wanted or needed if (path != null) { addBotPath(path); }
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
    String sessionKey = getSessionKey(userName, botName);
    sessions.put(sessionKey, session);

    log.info("Started session for bot botName:{} , userName:{}", botName, userName);
    setCurrentSession(userName, botName);
    
    invoke("publishSession", sessionKey);
    
    return session;
  }
  
  /**
   * When a new session is started this event is published with the session's key
   * @param sessionKey of new Session
   * @return sessionKey
   */
  public String publishSession(String sessionKey) {
    return sessionKey;
  }

  /**
   * setting the current session is equivalent to setting current user name and
   * current bot name
   * 
   * @param userName
   *          username
   * @param botName
   *          botname
   * 
   */
  public void setCurrentSession(String userName, String botName) {
    setCurrentUserName(userName);
    setCurrentBotName(botName);
  }

  /**
   * A category sent "to" program-ab - there is a callback onAddCategory which
   * should hook to the event when program-ab adds a category
   * 
   * @param c
   */
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

  /**
   * Verifies and adds a new path to the search directories for bots
   * 
   * @param path
   *          the path to add a bot from
   * @return the path if successful. o/w null
   * 
   */
  public String addBotPath(String path) {
    // verify the path is valid
    File botPath = new File(path);
    File verifyAiml = new File(FileIO.gluePaths(path, "aiml"));
    if (botPath.exists() && botPath.isDirectory() && verifyAiml.exists() && verifyAiml.isDirectory()) {

      for (BotInfo bi : bots.values()) {
        // check relative & absolute ???
        if (bi.path.equals(botPath)) {
          log.info("already loaded bot at {}", path);
          return path;
        }
      }

      BotInfo botInfo = new BotInfo(this, botPath);

      // key'ing on "path" probably would be better and only displaying "name"
      // then there would be no put/collisions only duplicate names
      // (preferrable)

      bots.put(botInfo.name, botInfo);
      botInfo.img = getBotImage(botInfo.name);
    } else {
      error("invalid bot path %s - a bot must be a directory with a subdirectory named \"aiml\"", path);
      return null;
    }
    return path;
  }

  @Deprecated /* for legacy - use addBotsDir */
  public String setPath(String path) {
    // This method is not good, because it doesn't take the full path
    // from input and there is a buried "hardcoded" value which no one knows
    // about
    addBotsDir(path + File.separator + "bots");

    return path;
  }

  public void setCurrentBotName(String botName) {
    config.currentBotName = botName;
    invoke("getBotImage", botName);
    broadcastState();
  }

  public void setCurrentUserName(String currentUserName) {
    config.currentUserName = currentUserName;
    broadcastState();
  }

  public Session getCurrentSession() {
    return sessions.get(getSessionKey(getCurrentUserName(), getCurrentBotName()));
  }

  public String getSessionKey(String userName, String botName) {
    return String.format("%s <-> %s", userName, botName);
  }

  public String getCurrentUserName() {
    return config.currentUserName;
  }

  public String getCurrentBotName() {
    return config.currentBotName;
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
   * @return bot paths
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
    return paths;
  }

  /**
   * This method can be used to get a listing of all bots available in the bots
   * directory.
   * 
   * @return list of botnames
   * 
   */
  public List<String> getBots() {

    List<String> names = new ArrayList<String>();
    for (String name : bots.keySet()) {
      names.add(name);
    }
    return names;
  }

  // FIXME - should be String name - and inside should querry
  // type NOT by instanceof but by Runtime.getType(name)
  @Override
  public void attach(Attachable attachable) {

    /*
     * if (attachable instanceof ResponseListener) { // this one is done
     * correctly attachResponseListener(attachable.getName()); } else
     */
    if (attachable instanceof TextPublisher) {
      attachTextPublisher((TextPublisher) attachable);
    } else if (attachable instanceof TextListener) {
      addListener("publishText", attachable.getName(), "onText");
    } else if (attachable instanceof UtteranceListener) {
      attachUtteranceListener(attachable.getName());
    } else {
      log.error("don't know how to attach a {}", attachable.getName());
    }
  }

  @Override
  public void stopService() {
    super.stopService();
    savePredicates();
  }

  public boolean setPeerSearch(boolean b) {
    peerSearch = b;
    return peerSearch;
  }

  @Override
  public void startService() {
    super.startService();

    logPublisher = new SimpleLogPublisher(this);
    logPublisher.filterClasses(new String[] { "org.alicebot.ab.Graphmaster", "org.alicebot.ab.MagicBooleans", "class org.myrobotlab.programab.MrlSraixHandler" });
    Logging logging = LoggingFactory.getInstance();
    logging.setLevel("org.alicebot.ab.Graphmaster", "DEBUG");
    logging.setLevel("org.alicebot.ab.MagicBooleans", "DEBUG");
    logging.setLevel("class org.myrobotlab.programab.MrlSraixHandler", "DEBUG");
    logPublisher.start();
  }

  @Override /* FIXME - just do this once in abstract */
  public void attachTextListener(TextListener service) {
    if (service == null) {
      log.warn("{}.attachTextListener(null)", getName());
      return;
    }
    attachTextListener(service.getName());
  }

  @Override
  public void attachTextListener(String name) {
    addListener("publishText", name);
  }

  @Override
  public void attachTextPublisher(TextPublisher service) {
    if (service == null) {
      log.warn("{}.attachTextPublisher(null)", getName());
      return;
    }
    subscribe(service.getName(), "publishText");
  }

  public SearchPublisher getSearch() {
    return (SearchPublisher) getPeer("search");
  }

  @Override
  public Map<String, Locale> getLocales() {

    Map<String, Locale> ret = new TreeMap<>();
    for (BotInfo botInfo : bots.values()) {
      if (botInfo.properties.containsKey("locale")) {
        locale = new Locale((String) botInfo.properties.get("locale"));
        ret.put(locale.getTag(), locale);
      }
    }
    // return Locale.getLocaleMap("en-US", "fr-FR", "es-ES", "de-DE", "nl-NL",
    // "ru-RU", "hi-IN", "it-IT", "fi-FI", "pt-PT");
    return ret;
  }

  @Override
  public String publishLog(String msg) {
    return msg;
  }

  public BotInfo getBotInfo() {
    return getBotInfo(config.currentBotName);
  }

  /**
   * reload current session
   * 
   * @throws IOException
   *           boom
   * 
   */
  public void reload() throws IOException {
    reloadSession(getCurrentUserName(), getCurrentBotName());
  }

  public String getBotImage() {
    return getBotImage(getCurrentBotName());
  }

  public String getBotImage(String botName) {
    BotInfo botInfo = null;
    String path = null;

    try {

      botInfo = getBotInfo(botName);
      if (botInfo != null) {
        path = FileIO.gluePaths(botInfo.path.getAbsolutePath(), "bot.png");
        File check = new File(path);
        if (check.exists()) {
          return Util.getImageAsBase64(path);
        }
      }

    } catch (Exception e) {
      info("image for %s cannot be found %s", botName, e.getMessage());
    }

    return getResourceImage("default.png");
  }

  public String getAimlFile(String botName, String name) {
    BotInfo botInfo = getBotInfo(botName);
    if (botInfo == null) {
      error("cannot get bot %s", botName);
      return null;
    }

    File f = new File(FileIO.gluePaths(botInfo.path.getAbsolutePath(), "aiml" + fs + name));
    if (!f.exists()) {
      error("cannot find file %s", f.getAbsolutePath());
      return null;
    }
    String ret = null;
    try {
      ret = FileIO.toString(f);
    } catch (IOException e) {
      log.error("getAimlFile threw", e);
    }
    return ret;
  }

  public void saveAimlFile(String botName, String filename, String data) {
    BotInfo botInfo = getBotInfo(botName);
    if (botInfo == null) {
      error("cannot get bot %s", botName);
      return;
    }

    File f = new File(FileIO.gluePaths(botInfo.path.getAbsolutePath(), "aiml" + fs + filename));

    try {
      FileIO.toFile(f, data.getBytes("UTF8"));
      info("saved %s", f);
    } catch (IOException e) {
      log.error("getAimlFile threw", e);
    }
  }

  @Override
  public ProgramABConfig getConfig() {
    super.getConfig();
    if (config.bots == null) {
      config.bots = new ArrayList<>();
    }

    config.bots.clear();
    for (BotInfo bot : bots.values()) {

      Path pathAbsolute = Paths.get(bot.path.getAbsolutePath());
      Path pathBase = Paths.get(System.getProperty("user.dir"));
      Path pathRelative = pathBase.relativize(pathAbsolute);
      config.bots.add(pathRelative.toString());

    }

    return config;
  }

  @Override
  public ProgramABConfig apply(ProgramABConfig c) {
    super.apply(c);
    if (c.bots != null && c.bots.size() > 0) {
      bots.clear();
      for (String botPath : c.bots) {
        addBotPath(botPath);
      }
      broadcastState();
    }

    if (c.currentUserName != null) {
      setCurrentUserName(c.currentUserName);
    }

    if (c.currentBotName != null) {
      setCurrentBotName(c.currentBotName);
    }

    if (c.startTopic != null) {
      setTopic(c.startTopic);
    }

    return c;
  }

  /**
   * Set the current locale for this service. In ProgramAB's case if a bot
   * matches the local then set the bot
   * 
   */
  @Override
  public void setLocale(String code) {
    if (code == null) {
      error("locale cannot be null");
      return;
    }
    locale = new Locale(code);
    log.info("{} new locale is {}", getName(), code);

    for (String bot : bots.keySet()) {
      if (code.equals(bot)) {
        setCurrentBotName(bot);
      }
    }
    broadcastState();
  }

  public static void main(String args[]) {
    try {
      LoggingFactory.init("INFO");
      // Runtime.start("gui", "SwingGui");

      Runtime runtime = Runtime.getInstance();
      // runtime.setLocale("it");
      /*
       * InMoov2 i01 = (InMoov2)Runtime.start("i01", "InMoov2"); String
       * startLeft = i01.localize("STARTINGLEFTONLY"); log.info(startLeft);
       */

      ProgramAB brain = (ProgramAB) Runtime.start("brain", "ProgramAB");
      Runtime.start("bot", "DiscordBot");
      Runtime.start("python", "Python");
      // Polly polly = (Polly) Runtime.start("polly", "Polly");

      // brain.attach("polly");

      // brain.localize(key);

      // String x = brain.getResourceImage("human.png");
      // log.info(x);

      /*
       * String x = brain.getBotImage("Alice"); log.info(x); Response response =
       * brain.getResponse("Hi, How are you?"); log.info(response.toString());
       * response = brain.getResponse("what's new?");
       * log.info(response.toString());
       */

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  public void addBotsDir(String path) {

    if (path == null) {
      error("set path can not be null");
      return;
    }

    File check = new File(path);
    if (!check.exists() || !check.isDirectory()) {
      error("invalid directory %s", path);
      return;
    }

    // check = new File(FileIO.gluePaths(path, "bots"));

    if (check.exists() && check.isDirectory()) {
      log.info("found %d possible bot directories", check.listFiles().length);
      for (File f : check.listFiles()) {
        addBotPath(f.getAbsolutePath());
      }
    }
  }

  @Override
  synchronized public void onChangePredicate(Chat chat, String predicateName, String result) {
    log.info("{} on predicate change {}={}", chat.bot.name, predicateName, result);

    // a little janky because program-ab doesn't know the predicate filename,
    // because it does know the "user"
    // but ProgramAB saves predicates in a {username}.predicates.txt format in
    // the bot directory

    // so we find the session by matching the chat in the callback
    for (Session s : sessions.values()) {
      if (s.chat == chat) {
        // found session saving predicates
        invoke("publishPredicate", s, predicateName, result);
        s.savePredicates();
        return;
      }
    }
    error("could not find session to save predicates");
  }

  /**
   * Predicate updates are published here. Topic (one of the most important
   * predicate change) is also published when it changes. Session is needed to
   * extract current user and bot this is relevant to.
   * 
   * @param session
   *          - session where the predicate change occurred
   * @param name
   *          - name of predicate
   * @param value
   *          - new value of predicate
   * @return
   */
  public PredicateEvent publishPredicate(Session session, String name, String value) {
    PredicateEvent event = new PredicateEvent();
    event.id = String.format("%s<->%s", session.userName, session.botInfo.name);
    event.userName = session.userName;
    event.botName = session.botInfo.name;
    event.name = name;
    event.value = value;

    if ("topic".equals(name) && value != null && !value.equals(session.currentTopic)) {
      invoke("publishTopic", new TopicChange(session.userName, session.botInfo.name, value, session.currentTopic));
      session.currentTopic = value;
    }

    return event;
  }

  /**
   * From program-ab - this gets called whenever a new category is added from a
   * learnf tag
   */
  @Override
  public void onLearnF(Chat chat, Category c) {
    log.info("{} onLearnF({})", chat, c);
    addCategoryToFile(chat.bot, c);
  }

  /**
   * From program-ab - this gets called whenever a new category is added from a
   * learnf tag
   */
  @Override
  public void onLearn(Chat chat, Category c) {
    log.info("{} onLearn({})", chat, c);
    addCategoryToFile(chat.bot, c);
  }

  synchronized public void addCategoryToFile(Bot bot, Category c) {
    try {
      File learnfFile = new File(bot.aiml_path + fs + LEARNF_AIML_FILE);

      if (!learnfFile.exists()) {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!-- DO NOT EDIT THIS FILE - \n\tIT IS OVERWRITTEN WHEN CATEGORIES ARE ADDED FROM LEARN AND LEARNF TAGS -->\n");
        sb.append("<aiml>\n");
        sb.append("</aiml>\n");
        FileIO.toFile(learnfFile, sb.toString().getBytes());
      }

      String learnf = FileIO.toString(learnfFile);
      int pos = learnf.indexOf("</aiml>");

      if (pos < 0) {
        error("could not find </aiml> tag in file %s", learnfFile.getAbsolutePath());
        return;
      }

      String out = learnf.substring(0, pos) + Category.categoryToAIML(c) + "\n" + learnf.substring(pos);

      FileIO.toFile(learnfFile, out.getBytes());

    } catch (Exception e) {
      error(e);
    }
  }

  /**
   * wakes the global session up
   */
  public void wake() {
    config.sleep = false;
  }

  /**
   * sleeps the global session
   */
  public void sleep() {
    config.sleep = true;
  }

  @Override
  public void onUtterance(Utterance utterance) throws Exception {

    log.info("Utterance Received " + utterance);

    boolean talkToBots = false;
    // TODO: reconcile having different name between the discord bot username
    // and the programab bot name. Mr. Turing is not actually Alice.. and vice
    // versa.
    String botName = utterance.channelBotName;

    // prevent bots going off the rails
    if (utterance.isBot && talkToBots) {
      log.info("Not responding to bots.");
      return;
    }

    // Don't talk to myself, though I should be a bot..
    if (utterance.username.contentEquals(botName)) {
      log.info("Don't talk to myself.");
      return;
    }

    boolean shouldIRespond = false;
    // always respond to direct messages.
    if ("PRIVATE".equals(utterance.channelType)) {
      shouldIRespond = true;
    } else {
      if (!utterance.isBot) {
        // TODO: don't talk to bots.. it won't go well..
        // TODO: the discord api can provide use the list of mentioned users.
        // for now.. we'll just see if we see Mr. Turing as a substring.
        config.sleep = (config.sleep || utterance.text.contains("@")) && !utterance.text.contains(botName);
        if (!config.sleep) {
          shouldIRespond = true;
        }
        
        if (config.channels != null && config.channels.size() > 0) {
          // assume false
          shouldIRespond = false;
          for (String channelName : config.channels) {
            if (channelName.equals(utterance.channelName)) {
              shouldIRespond = true;
            }
          }
        }
      }
    }

    // TODO: is there a better way to test for this?
    if (shouldIRespond) {
      log.info("I should respond!");
      // let's respond to the user to their utterance.
      String utteranceDisp = utterance.text;
      // let's strip the @+botname from the beginning of the utterance i guess.
      // Strip the botname from the utterance passed to programab.
      utteranceDisp = utteranceDisp.replace("@" + botName, "");
      Response resp = getResponse(utterance.username, utteranceDisp);
      if (resp != null && !StringUtils.isEmpty(resp.msg)) {
        // Ok.. now what? respond to the user ...
        Utterance response = new Utterance();
        response.username = resp.botName;
        response.text = resp.msg;
        response.isBot = true;
        // Copy these from the utterance we received
        response.channel = utterance.channel;
        response.channelType = utterance.channelType;
        response.channelBotName = utterance.channelBotName;
        // send the message back to all utterance listeners
        // TODO: selectively only send this message back to the
        // discordbot (utterance listener ) that sent the message.
        invoke("publishUtterance", response);
      } else {
        log.info("No Response from the chatbot brain... now what?");
      }
    }
  }

  /**
   * This receiver can take a config published by another service and sync
   * predicates from it
   * 
   * @param cfg
   */
  public void onConfig(ServiceConfig cfg) {
    Yaml yaml = new Yaml();
    String yml = yaml.dumpAsMap(cfg);
    Map<String, Object> cfgMap = yaml.load(yml);

    for (Map.Entry<String, Object> entry : cfgMap.entrySet()) {
      if (entry.getValue() == null) {
        setPredicate("cfg_" + entry.getKey(), null);
      } else {
        setPredicate("cfg_" + entry.getKey(), entry.getValue().toString());
      }
    }

    invoke("getPredicates");
  }

  @Override
  public Utterance publishUtterance(Utterance utterance) {
    return utterance;
  }

  public TopicChange publishTopic(TopicChange topicChange) {
    return topicChange;
  }

  public String getTopic() {
    return getPredicate(getCurrentUserName(), "topic");
  }

  public String getTopic(String username) {
    return getPredicate(username, "topic");
  }

  public void setTopic(String username, String topic) {
    setPredicate(username, "topic", topic);
  }

  public void setTopic(String topic) {
    setPredicate(getCurrentUserName(), "topic", topic);
  }

  public OobProcessor getOobProcessor() {
    return oobProcessor;
  }

  public Object getBotProperty(String key) {
    BotInfo botInfo = getBotInfo();
    if (botInfo != null) {
      org.alicebot.ab.Properties props = botInfo.getBotProperties();
      if (props != null) {
        return props.get(key);
      }
    }
    return null;
  }
  
  public Object getBotProperties() {
    BotInfo botInfo = getBotInfo();
    if (botInfo != null) {
      org.alicebot.ab.Properties props = botInfo.getBotProperties();
      return props;
    }
    return null;
  }
  
}
