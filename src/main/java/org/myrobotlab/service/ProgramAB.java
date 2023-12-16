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
import org.alicebot.ab.ProgramABListener;
import org.apache.commons.lang3.StringUtils;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.generics.SlidingWindowList;
import org.myrobotlab.image.Util;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.logging.SimpleLogPublisher;
import org.myrobotlab.programab.BotInfo;
import org.myrobotlab.programab.Response;
import org.myrobotlab.programab.Session;
import org.myrobotlab.programab.models.Event;
import org.myrobotlab.service.config.ProgramABConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.data.Locale;
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
    implements TextListener, TextPublisher, LocaleProvider, LogPublisher, ProgramABListener, UtterancePublisher,
    UtteranceListener, ResponsePublisher {

  /**
   * default file name that aiml categories comfing from matching a learnf tag
   * will be written to.
   */
  private static final String LEARNF_AIML_FILE = "learnf.aiml";

  private static final long serialVersionUID = 1L;

  /**
   * history of topic changes
   */
  protected List<Event> topicHistory = new SlidingWindowList<>(100);

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
   * Mapping a bot to a currentUserName and chat session
   */
  Map<String, Session> sessions = new TreeMap<>();

  /**
   * start GoogleSearch (a peer) instead of sraix web service which is down or
   * problematic much of the time
   */
  boolean peerSearch = true;

  transient SimpleLogPublisher logPublisher = null;

  /**
   * Default constructor for the program ab service.
   * 
   * 
   * @param n
   *           - service name
   * @param id
   *           - process id
   */
  public ProgramAB(String n, String id) {
    super(n, id);
  }

  public String getBotName(File file) {
    return file.getName();
  }

  /**
   * function to scan the parent directory for bot directories, and return a
   * list of valid bots to be added with addBot(path)
   * 
   * @param path
   *             path to search
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
        addBot(file.getAbsolutePath());
      }
    }
    return botDirs;
  }

  /**
   * @param botDir
   *               checks to see if valid bot dir
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
    return getConfig().maxConversationDelay;
  }

  /**
   * This is the main method that will ask for the current bot's chat session to
   * respond to It returns a Response object.
   * 
   * @param text
   *             the input utterance
   * @return the programab response
   * 
   */
  public Response getResponse(String text) {
    return getResponse(getUsername(), text);
  }

  /**
   * This method has the side effect of switching which bot your are currently
   * chatting with.
   * 
   * @param currentUserName
   *                        - the query string to the bot brain
   * @param text
   *                        - the user that is sending the query
   * @return the response for a user from a bot given the input text.
   */
  public Response getResponse(String currentUserName, String text) {
    return getResponse(currentUserName, getBotType(), text);
  }

  /**
   * Full get response method . Using this method will update the current
   * user/bot name if different from the current session.
   * 
   * @param currentUserName
   *                        currentUserName
   * @param currentBotName
   *                        bot name
   * @param text
   *                        utterace
   * @return programab response to utterance
   * 
   */
  public Response getResponse(String currentUserName, String currentBotName, String text) {
    return getResponse(currentUserName, currentBotName, text, true);
  }

  /**
   * Gets a response and optionally update if this is the current bot session
   * that's active globally.
   * 
   * @param currentUserName
   *                             - user request a response
   * 
   * @param currentBotName
   *                             - bot type providing the response
   * 
   * @param text
   *                             - query
   * 
   * @param updateCurrentSession
   *                             - switch the current focus, so that the current
   *                             session is the
   *                             currentUserName and bot type in the parameter,
   *                             publishSession will
   *                             publish the new session if different
   * 
   * @return the response
   * 
   */
  public Response getResponse(String currentUserName, String currentBotName, String text,
      boolean updateCurrentSession) {
    Session session = getSession(currentUserName, currentBotName);

    // if a session with this user and bot does not exist
    // attempt to create it
    if (session == null) {
      session = startSession(currentUserName, currentBotName, updateCurrentSession);
      if (session == null) {
        error("currentUserName or bot name not valid %s %s", currentUserName, currentBotName);
        return null;
      }
    }

    if (updateCurrentSession && (!getUsername().equals(currentUserName) || !getBotType().equals(currentBotName))) {
      setUsername(currentUserName);
      setBotType(currentBotName);
    }

    log.info("getResponse({})", text);
    Response response = session.getResponse(text);

    invoke("publishRequest", text);
    invoke("publishResponse", response);
    invoke("publishText", response.msg);

    return response;
  }

  private Bot getBot(String currentBotName) {
    return bots.get(currentBotName).getBot();
  }

  private BotInfo getBotInfo(String currentBotName) {
    if (currentBotName == null) {
      error("getBotinfo(null) not valid");
      return null;
    }
    BotInfo botInfo = bots.get(currentBotName);
    if (botInfo == null) {
      error("botInfo(%s) is null", currentBotName);
      return null;
    }

    return botInfo;
  }

  /**
   * This method specifics how many times the robot will respond with the same
   * thing before forcing a different (default?) response instead.
   * 
   * @param val
   *            foo
   */
  public void repetitionCount(int val) {
    org.alicebot.ab.MagicNumbers.repetition_count = val;
  }

  /**
   * get the "current" session if it exists
   * 
   * @return
   */
  public Session getSession() {
    return getSession(getUsername(), getBotType());
  }

  /**
   * get a specific user & currentBotName session
   * 
   * @param user
   * @param currentBotName
   * @return
   */
  public Session getSession(String user, String currentBotName) {
    String sessionKey = getSessionKey(user, currentBotName);
    if (sessions.containsKey(sessionKey)) {
      return sessions.get(sessionKey);
    } else {
      return null;
    }
  }

  /**
   * remove a specific user and current bot types predicate
   */
  public void removePredicate(String user, String predicateName) {
    removePredicate(user, getBotType(), predicateName);
  }

  /**
   * remove an explicit user and currentBotName's predicate
   * 
   * @param user
   * @param currentBotName
   * @param name
   */
  public void removePredicate(String user, String currentBotName, String name) {
    Session session = getSession(user, currentBotName);
    if (session != null) {
      session.remove(name);
    } else {
      error("could not remove predicate %s from session %s<->%s session does not exist", user, currentBotName, name);
    }
  }

  /**
   * Add a value to a set for the current session
   * 
   * @param setName
   *                 name of the set
   * @param setValue
   *                 value to add to the set
   */
  public void addToSet(String setName, String setValue) {
    if (setName == null || setValue == null) {
      error("addToSet(%s,%s) cannot have name or value null", setName, setValue);
      return;
    }
    setName = setName.toLowerCase().trim();
    setValue = setValue.trim();

    // add to the set for the bot.
    Bot bot = getBot(getBotType());
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
   *                - the map name
   * @param key
   *                - the key
   * @param value
   *                - the value
   */
  public void addToMap(String mapName, String key, String value) {

    if (mapName == null || key == null || value == null) {
      error("addToMap(%s,%s,%s) mapname, key or value cannot be null", mapName, key, value);
      return;
    }
    mapName = mapName.toLowerCase().trim();
    key = key.toUpperCase().trim();

    // add an entry to the map.
    Bot bot = getBot(getBotType());
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

  public void setPredicate(String name, String value) {
    setPredicate(getUsername(), name, value);
  }

  /**
   * Sets a specific user and current bot predicate to a value. Useful when
   * setting predicate values of a session, when the user previously was an
   * unknown human to a new or previously known user.
   * 
   * @param currentUserName
   * @param name
   * @param value
   */
  public void setPredicate(String currentUserName, String name, String value) {
    setPredicate(currentUserName, getBotType(), name, value);
  }

  /**
   * Sets a predicate for a session keyed by currentUserName and bottype. If the
   * session does not currently exist, it will make a new session for that user.
   * 
   * @param currentUserName
   * @param currentBotName
   * @param name
   * @param value
   */
  public void setPredicate(String currentUserName, String currentBotName, String name, String value) {
    Session session = getSession(currentUserName, currentBotName);
    if (session != null) {
      session.setPredicate(name, value);
    } else {
      // attempt to create a session if it doesn't exist
      session = startSession(currentUserName, currentBotName, false);
      if (session != null) {
        session.setPredicate(name, value);
      } else {
        error("could not create session");
      }
    }
  }

  @Deprecated /* use removePredicate */
  public void unsetPredicate(String currentUserName, String predicateName) {
    removePredicate(currentUserName, getBotType(), predicateName);
  }

  /**
   * Get a predicate's value for the current session
   * 
   * @param predicateName
   * @return
   */
  public String getPredicate(String predicateName) {
    return getPredicate(getUsername(), predicateName);
  }

  /**
   * get a specified users's predicate value for the current currentBotName
   * session
   * 
   * @param currentUserName
   * @param predicateName
   * @return
   */
  public String getPredicate(String currentUserName, String predicateName) {
    return getPredicate(currentUserName, getBotType(), predicateName);
  }

  /**
   * With a session key, get a specific predicate value
   * 
   * @param currentUserName
   * @param currentBotName
   * @param predicateName
   * @return
   */
  public String getPredicate(String currentUserName, String currentBotName, String predicateName) {
    Session s = getSession(currentUserName, currentBotName);
    if (s == null) {
      s = startSession(currentUserName, currentBotName, false);
      if (s == null) {
        log.warn("Error starting programAB session between bot {} and user {}", currentUserName, currentBotName);
        return null;
      }
    }
    return s.getPredicate(predicateName);
  }

  /**
   * Only respond if the last response was longer than delay ms ago
   * 
   * @param currentUserName
   *                        - current currentUserName
   * @param text
   *                        - text to get a response
   * @param delay
   *                        - min amount of time that must have transpired since
   *                        the last
   * @return the response
   * @throws IOException
   *                     boom
   */
  public Response troll(String currentUserName, String text, Long delay) throws IOException {
    Session session = getSession(currentUserName, getBotType());
    long delta = System.currentTimeMillis() - session.lastResponseTime.getTime();
    if (delta > delay) {
      return getResponse(currentUserName, text);
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
   * @param currentBotName
   *                       the bots name from which to return it's patterns.
   * @return a list of all patterns loaded into the aiml brain
   */
  public ArrayList<String> listPatterns(String currentBotName) {
    ArrayList<String> patterns = new ArrayList<String>();
    Bot bot = getBot(currentBotName);
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
   *                 publish a response generated from a session in the programAB
   *                 service.
   * @return the response
   * 
   */
  @Override
  public Response publishResponse(Response response) {
    return response;
  }

  @Override
  public String publishText(String text) {
    if (text == null || text.length() == 0) {
      return "";
    }
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
   *                the out of band text to publish
   * @return oobtext
   * 
   */
  public String publishOOBText(String oobText) {
    return oobText;
  }

  /**
   * This method will close the current bot, and reload it from AIML It then
   * will then re-establish only the session associated with currentUserName.
   * 
   * @param currentUserName
   *                        currentUserName for the session
   * @param currentBotName
   *                        the bot name being chatted with
   * @throws IOException
   *                     boom
   * 
   */
  public void reloadSession(String currentUserName, String currentBotName) throws IOException {
    Session session = getSession(currentUserName, currentBotName);
    if (session != null) {
      session.reload();
      info("reloaded session %s <-> %s ", currentUserName, currentBotName);
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
  public Map<String, String> getPredicates(String currentUserName, String currentBotName) {
    Session session = getSession(currentUserName, currentBotName);
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
    getConfig().enableTrolling = enableAutoConversation;
  }

  public boolean getEnableAutoConversation() {
    return getConfig().enableTrolling;
  }

  public void setMaxConversationDelay(int maxConversationDelay) {
    getConfig().maxConversationDelay = maxConversationDelay;
  }

  /**
   * set a bot property - the result will be serialized to config/properties.txt
   * 
   * @param name
   *              property name to set for current bot/session
   * @param value
   *              value to set for current bot/session
   */
  public void setBotProperty(String name, String value) {
    setBotProperty(getBotType(), name, value);
  }

  /**
   * set a bot property - the result will be serialized to config/properties.txt
   * 
   * @param currentBotName
   *                       bot name
   * @param name
   *                       bot property name
   * @param value
   *                       value to set the property too
   */
  public void setBotProperty(String currentBotName, String name, String value) {
    info("setting %s property %s:%s", getBotType(), name, value);
    BotInfo botInfo = getBotInfo(currentBotName);
    name = name.trim();
    value = value.trim();
    botInfo.setProperty(name, value);
  }

  public void removeBotProperty(String name) {
    removeBotProperty(getBotType(), name);
  }

  public void removeBotProperty(String currentBotName, String name) {
    info("removing %s property %s", getBotType(), name);
    BotInfo botInfo = getBotInfo(currentBotName);
    botInfo.removeProperty(name);
  }

  /**
   * Setting a session is only setting a key, to the active user and bot, its
   * not starting a session, which is a different process done threw
   * startSession.
   * 
   * Sets currentUserName and currentBotName. The session will be started if it
   * can be
   * when a
   * getResponse is processed. "Active" session is just where the session key
   * exists and is currently set via currentUserName and currentBotName
   * 
   * @param currentUserName
   * @param currentBotName
   * @return
   */
  public void setSession(String currentUserName, String currentBotName) {
    // replacing "focus" so
    // current name and bottype is the
    // one that will be used
    setUsername(currentUserName);
    setBotType(currentBotName);
  }
  
  public Session startSession(String currentUserName, String currentBotName) {
    return startSession(currentUserName, currentBotName, true);
  }

  /**
   * Load the AIML 2.0 Bot config and start a chat session. This must be called
   * after the service is created. If the session does not exist it will be
   * created. If the session does exist then that session will be used.
   * 
   * config.currentUserName and config.currentBotName will be set in memory the
   * specified
   * values. The "current" session will be this session.
   * 
   * @param currentUserName
   *                        - The new user name
   * @param currentBotName
   *                        - The name of the bot to load. (example: alice2)
   * 
   * @return the session that is started
   */

  public Session startSession(String currentUserName, String currentBotName, boolean setAsCurrent) {

    if (currentUserName == null || currentBotName == null) {
      error("currentUserName nor bot type can be null");
      return null;
    }

    if (!bots.containsKey(currentBotName)) {
      error("bot type %s is not valid, list of possible types are %s", currentBotName, bots.keySet());
      return null;
    }

    if (setAsCurrent) {
      // really sets the key of the active session currentUserName <-> currentBotName
      // but next getResponse will use this session
      setSession(currentUserName, currentBotName);
    }

    String sessionKey = getSessionKey(currentUserName, currentBotName);
    if (sessions.containsKey(sessionKey)) {
      log.info("session exists returning existing");
      return sessions.get(sessionKey);
    }

    log.info("creating new session {}<->{} replacing {}", currentUserName, currentBotName, setAsCurrent);
    BotInfo botInfo = getBotInfo(currentBotName);
    Session session = new Session(this, currentUserName, botInfo);
    sessions.put(sessionKey, session);

    // get session
    return getSession();
  }

  /**
   * A category sent "to" program-ab - there is a callback onAddCategory which
   * should hook to the event when program-ab adds a category
   * 
   * @param c
   */
  public void addCategory(Category c) {
    Bot bot = getBot(getBotType());
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
   * Verifies and adds a new path to the search directories for bots. Bots of
   * aiml live in directories which represent their "type" The directory names
   * must be unique.
   * 
   * @param path
   *             the path to add a bot from
   * @return the path if successful. o/w null
   * 
   */
  public String addBot(String path) {
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

      if (bots.containsKey(botInfo.currentBotName)) {
        log.info("replacing bot %s with new bot definition", botInfo.currentBotName);
      }

      bots.put(botInfo.currentBotName, botInfo);
      botInfo.img = getBotImage(botInfo.currentBotName);

      broadcastState();
    } else {
      error("invalid bot path %s - a bot must be a directory with a subdirectory named \"aiml\"", path);
      return null;
    }
    return path;
  }

  @Deprecated /* use setBotType */
  public void setCurrentBotName(String currentBotName) {
    setBotType(currentBotName);
  }

  /**
   * Sets the current bot type to a set of aiml folders previously added via
   * configuration or through the addBot(path) function.
   * 
   * You can get a list of possible configured bot types through the method
   * getBots()
   * 
   * @param currentBotName
   */
  public void setBotType(String currentBotName) {
    if (currentBotName == null) {
      error("bot type cannot be null");
      return;
    }

    if (bots.size() == 0) {
      error("bot paths must be set before a bot type is set");
    }

    if (!bots.containsKey(currentBotName)) {
      error("cannot set bot %s, no valid type found, possible values are %s", currentBotName, bots.keySet());
      return;
    }
    String prev = config.currentBotName;
    config.currentBotName = currentBotName;
    if (!currentBotName.equals(prev)) {
      invoke("getBotImage", currentBotName);
      broadcastState();
    }
  }

  public void setUsername(String currentUserName) {
    if (currentUserName == null) {
      error("currentUserName cannot be null");
      return;
    }
    String prev = config.currentUserName;
    config.currentUserName = currentUserName;
    if (!currentUserName.equals(prev)) {
      broadcastState();
    }
  }

  public String getSessionKey(String currentUserName, String currentBotName) {
    return String.format("%s <-> %s", currentUserName, currentBotName);
  }

  /**
   * Simple preferred way to get the user's name
   * 
   * @return
   */
  public String getUsername() {
    return config.currentUserName;
  }

  @Deprecated /* of course it will be "current" - use getUser() */
  public String getCurrentUserName() {
    return getUsername();
  }

  @Deprecated /* use getBotType() */
  public String getCurrentBotName() {
    return getBotType();
  }

  public String getBotType() {
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
    try {
      super.startService();

      logPublisher = new SimpleLogPublisher(this);
      logPublisher.filterClasses(new String[] { "org.alicebot.ab.Graphmaster", "org.alicebot.ab.MagicBooleans",
          "class org.myrobotlab.programab.MrlSraixHandler" });
      Logging logging = LoggingFactory.getInstance();
      logging.setLevel("org.alicebot.ab.Graphmaster", "DEBUG");
      logging.setLevel("org.alicebot.ab.MagicBooleans", "DEBUG");
      logging.setLevel("class org.myrobotlab.programab.MrlSraixHandler", "DEBUG");
      logPublisher.start();

    } catch (Exception e) {
      error(e);
    }

  }
  
  @Override
  public ProgramABConfig apply(ProgramABConfig c) {
    super.apply(c);
    // scan for bots
    if (config.botDir != null) {
      scanForBots(config.botDir);
    }

    // explicitly setting bots overrides scans
    if (config.bots != null && config.bots.size() > 0) {
      for (String botPath : config.bots) {
        addBot(botPath);
      }
    }

    if (config.currentUserName != null) {
      setUsername(config.currentUserName);
    }

    if (config.currentBotName != null) {
      setBotType(config.currentBotName);
    }

    if (config.startTopic != null) {
      setTopic(config.startTopic);
    }
    return c;    
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
   *                     boom
   * 
   */
  public void reload() {
    try {
      reloadSession(getUsername(), getBotType());
    } catch (Exception e) {
      error(e);
    }
  }

  public String getBotImage() {
    return getBotImage(getBotType());
  }

  public String getBotImage(String currentBotName) {
    BotInfo botInfo = null;
    String path = null;

    try {

      botInfo = getBotInfo(currentBotName);
      if (botInfo != null) {
        path = FileIO.gluePaths(botInfo.path.getAbsolutePath(), "bot.png");
        File check = new File(path);
        if (check.exists()) {
          return Util.getImageAsBase64(path);
        }
      }

    } catch (Exception e) {
      info("image for %s cannot be found %s", currentBotName, e.getMessage());
    }

    return getResourceImage("default.png");
  }

  public String getAimlFile(String currentBotName, String name) {
    BotInfo botInfo = getBotInfo(currentBotName);
    if (botInfo == null) {
      error("cannot get bot %s", currentBotName);
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

  public void saveAimlFile(String currentBotName, String filename, String data) {
    BotInfo botInfo = getBotInfo(currentBotName);
    if (botInfo == null) {
      error("cannot get bot %s", currentBotName);
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

  public static void main(String args[]) {
    try {
      LoggingFactory.init("INFO");
      Runtime.startConfig("dev");
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

  public void addBots(String path) {

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
        addBot(f.getAbsolutePath());
      }
    }
  }

  @Override
  synchronized public void onChangePredicate(Chat chat, String predicateName, String value) {
    log.info("{} on predicate change {}={}", chat.bot.name, predicateName, value);

    // a little janky because program-ab doesn't know the predicate filename,
    // because it does know the "user"
    // but ProgramAB saves predicates in a {currentUserName}.predicates.txt format
    // in
    // the bot directory

    // so we find the session by matching the chat in the callback
    for (Session s : sessions.values()) {
      if (s.chat == chat) {
        // found session saving predicates
        invoke("publishPredicate", s, predicateName, value);
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
   *                - session where the predicate change occurred
   * @param name
   *                - name of predicate
   * @param value
   *                - new value of predicate
   * @return
   */
  public Event publishPredicate(Session session, String name, String value) {
    Event event = new Event();
    event.id = String.format("%s<->%s", session.userName, session.botInfo.currentBotName);
    event.user = session.userName;
    event.botname = session.botInfo.currentBotName;
    event.name = name;
    event.value = value;

    if ("topic".equals(name) && value != null && !value.equals(session.currentTopic)) {
      Event topicChange = new Event(getName(), session.userName, session.botInfo.currentBotName, value);
      invoke("publishTopic", topicChange);
      session.currentTopic = value;
      topicHistory.add(topicChange);
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
        sb.append(
            "<!-- DO NOT EDIT THIS FILE - \n\tIT IS OVERWRITTEN WHEN CATEGORIES ARE ADDED FROM LEARN AND LEARNF TAGS -->\n");
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

    log.info("utterance received {}", utterance);

    boolean talkToBots = false;
    // TODO: reconcile having different name between the discord bot currentUserName
    // and the programab bot name. Mr. Turing is not actually Alice.. and vice
    // versa.
    String currentBotName = utterance.channelBotName;

    // prevent bots going off the rails
    if (utterance.isBot && talkToBots) {
      log.info("Not responding to bots.");
      return;
    }

    // Don't talk to myself, though I should be a bot..
    if (utterance.username != null && utterance.username.contentEquals(currentBotName)) {
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
        config.sleep = (config.sleep || utterance.text.contains("@")) && !utterance.text.contains(currentBotName);
        if (!config.sleep) {
          shouldIRespond = true;
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
      utteranceDisp = utteranceDisp.replace("@" + currentBotName, "");
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

  /**
   * New topic published when it changes
   * 
   * @param topicChange
   * @return
   */
  public Event publishTopic(Event topicChange) {
    return topicChange;
  }

  public String getTopic() {
    return getPredicate(getUsername(), "topic");
  }

  public String getTopic(String currentUserName) {
    return getPredicate(currentUserName, "topic");
  }

  public void setTopic(String currentUserName, String topic) {
    setPredicate(currentUserName, "topic", topic);
  }

  public void setTopic(String topic) {
    setPredicate(getUsername(), "topic", topic);
  }

  /**
   * Published when a new session is created
   * 
   * @param session
   * @return
   */
  public Event publishSession(Event session) {
    return session;
  }

  /**
   * clear all sessions
   */
  public void clear() {
    log.info("clearing sessions");
    sessions.clear();
  }

  /**
   * <pre>
   * A mechanism to publish a message directly from aiml.
   * The subscriber can interpret the message and do something with it.
   * In the case of InMoov for example, the unaddressed messages are processed
   * as python method calls. This remove direct addressing from the aiml!
   * And allows a great amount of flexibility on how the messages are
   * interpreted, without polluting the aiml or ProgramAB.
   * 
   * The oob syntax is:
   *  &lt;oob&gt;
   *    &lt;mrljson&gt;
   *        [{method:on_new_user, data:[{&quot;name&quot;:&quot;&lt;star/&gt;&quot;}]}]
   *    &lt;/mrljson&gt;
   * &lt;/oob&gt;
   * 
   * 
   * Full typed parameters are supported without conversions.
   * 
   * </pre>
   * 
   * @param msg
   * @return
   */
  public Message publishMessage(Message msg) {
    return msg;
  }

}
