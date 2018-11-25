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
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.programab.ChatData;
import org.myrobotlab.programab.MrlSraixHandler;
import org.myrobotlab.programab.OOBPayload;
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
 * @author kwatters
 *
 */
public class ProgramAB extends Service implements TextListener, TextPublisher {

  transient public final static Logger log = LoggerFactory.getLogger(ProgramAB.class);

  public static class Response {

    public String msg;
    transient public List<OOBPayload> payloads;
    // FIXME - timestamps are usually longs System.currentTimeMillis()
    public Date timestamp;
    public String botName;
    public String userName;

    public Response(String userName, String botName, String msg, List<OOBPayload> payloads, Date timestamp) {
      this.botName = botName;
      this.userName = userName;
      this.msg = msg;
      this.payloads = payloads;
      this.timestamp = timestamp;
    }

    public String toString() {
      return String.format("%d %s %s %s", timestamp.getTime(), userName, botName, msg);
    }
  }

  transient Bot bot = null;

  private String path = "ProgramAB";

  transient HashMap<String, HashMap<String, ChatData>> sessions = new HashMap<String, HashMap<String, ChatData>>();
  // TODO: better parsing than a regex...
  transient Pattern oobPattern = Pattern.compile("<oob>.*?</oob>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
  transient Pattern mrlPattern = Pattern.compile("<mrl>.*?</mrl>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);

  // a guaranteed bot we have
  private String currentBotName = "en-US";
  // this is the username that is chatting with the bot.
  private String currentUserName = "default";

  static final long serialVersionUID = 1L;
  static int savePredicatesInterval = 60 * 1000 * 5; // every 5 minutes

  @Deprecated
  Boolean wasCleanyShutdowned = true;
  Boolean visualDebug;

  HashSet<String> availableBots = new HashSet<String>();

  public ProgramAB(String name) {
    super(name);
    getBots();
    // Tell programAB to persist it's learned predicates about people
    // every 30 seconds.
    addTask("savePredicates", savePredicatesInterval, 0, "savePredicates");
  }

  public void addOOBTextListener(TextListener service) {
    addListener("publishOOBText", service.getName(), "onOOBText");
  }

  public void addResponseListener(Service service) {
    addListener("publishResponse", service.getName(), "onResponse");
  }

  public void addTextListener(TextListener service) {
    addListener("publishText", service.getName(), "onText");
  }

  public void addTextListener(SpeechSynthesis service) {
    addListener("publishText", service.getName(), "onText");
  }

  public void addTextPublisher(TextPublisher service) {
    subscribe(service.getName(), "publishText");
  }

  /**
   * We don't use csv fiiles anymore, just a check there is no more aimlIf folder
   * Check also if there is Aiml files inside folder...
   */
  private boolean checkBrain(String botName) {
    if (botName == null || botName.isEmpty()) {
      error("checkBrain: Bot name is null !! please check..");
      return false;
    }
    String aimlPath = getPath() + File.separator + "bots" + File.separator + botName + File.separator + "aiml";
    String aimlIFPath = getPath() + File.separator + "bots" + File.separator + botName + File.separator + "aimlif";
    log.info("AIML Files:");
    File folder = new File(aimlPath);
    File folderaimlIF = new File(aimlIFPath);
    if (!folder.exists()) {
      error("{} does not exist", aimlPath);
      return false;
    }
    if (folderaimlIF.exists()) {
      folderaimlIF.renameTo(new File(folderaimlIF + ".old"));
      log.warn("Moving aimlIf folder to old, we don't need it anymore, and can cause issues !");
    }
    return true;
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

  public Response getResponse(String text) {
    return getResponse(getCurrentUserName(), text);
  }

  /**
   * 
   * @param text
   *          - the query string to the bot brain
   * @param username
   *          - the user that is sending the query
   * @param botName
   *          - the name of the bot you which to get the response from
   * @return the response for a user from a bot given the input text.
   */
  public Response getResponse(String username, String botName, String text) {
    this.setCurrentBotName(botName);
    return getResponse(username, text);
  }

  public Response getResponse(String userName, String text) {
    log.info("Get Response for : user {} bot {} : {}", userName, getCurrentBotName(), text);
    if (bot == null) {
      String error = "ERROR: Core not loaded, please load core before chatting.";
      error(error);
      return new Response(userName, getCurrentBotName(), error, null, new Date());
    }

    if (text.isEmpty()) {
      return new Response(userName, getCurrentBotName(), "", null, new Date());
    }

    if (!sessions.containsKey(getCurrentBotName()) || !sessions.get(getCurrentBotName()).containsKey(userName)) {
      startSession(getPath(), userName, getCurrentBotName());
    }

    ChatData chatData = sessions.get(getCurrentBotName()).get(userName);
    String res = getChat(userName, getCurrentBotName()).multisentenceRespond(text);
    // grab and update the time when this response came in.
    chatData.lastResponseTime = new Date();

    // Check the AIML response to see if there is OOB (out of band data)
    // If so, publish that data independent of the text response.
    List<OOBPayload> payloads = null;
    if (chatData.processOOB) {
      payloads = processOOB(res);
    }

    // OOB text should not be published as part of the response text.
    Matcher matcher = oobPattern.matcher(res);
    res = matcher.replaceAll("").trim();

    Response response = new Response(userName, getCurrentBotName(), res, payloads, chatData.lastResponseTime);
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

    // if (log.isDebugEnabled()) {
    // for (String key : sessions.get(session).predicates.keySet()) {
    // log.debug(session + " " + key + " " +
    // sessions.get(session).predicates.get(key));
    // }
    // }
    return response;
  }

  public void repetition_count(int val) {
    org.alicebot.ab.MagicNumbers.repetition_count = val;
  }

  public Chat getChat(String userName, String botName) {
    if (!sessions.containsKey(botName) || !sessions.get(botName).containsKey(userName)) {
      error("%s %S session does not exist", botName, userName);
      return null;
    } else {
      return sessions.get(botName).get(userName).chat;
    }
  }

  public void removePredicate(String userName, String predicateName) {
    removePredicate(userName, getCurrentBotName(), predicateName);
  }

  public void removePredicate(String userName, String botName, String predicateName) {
    Predicates preds = getChat(userName, botName).predicates;
    preds.remove(predicateName);
  }

  public void addToSet(String setName, String setValue) {
    // add to the set for the bot.
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

  public void addToMap(String mapName, String mapKey, String mapValue) {
    // add an entry to the map.
    AIMLMap updateMap = bot.mapMap.get(mapName);
    mapKey = mapKey.toUpperCase().trim();
    if (updateMap != null) {
      updateMap.put(mapKey, mapValue);
      // persist to disk!
      updateMap.writeAIMLMap();
    } else {
      log.info("Unknown AIML map: {}.  A new MAP will be created. ", mapName);
      // dynamically create new maps?!
      AIMLMap newMap = new AIMLMap(mapName, bot);
      newMap.put(mapKey, mapValue);
      newMap.writeAIMLMap();
    }
  }

  public void setPredicate(String predicateName, String predicateValue) {
    setPredicate(getCurrentUserName(), predicateName, predicateValue);
  }

  public void setPredicate(String username, String predicateName, String predicateValue) {
    Predicates preds = getChat(username, getCurrentBotName()).predicates;
    preds.put(predicateName, predicateValue);
  }

  public void unsetPredicate(String username, String predicateName) {
    Predicates preds = getChat(username, getCurrentBotName()).predicates;
    preds.remove(predicateName);
  }

  public String getPredicate(String predicateName) {
    return getPredicate(getCurrentUserName(), predicateName);
  }

  public String getPredicate(String username, String predicateName) {
    Predicates preds = getChat(username, getCurrentBotName()).predicates;
    return preds.get(predicateName);
  }

  /**
   * Only respond if the last response was longer than delay ms ago
   * 
   * @param username
   *          - current username
   * @param text
   *          - text to get a response for
   * @param delay
   *          - min amount of time that must have transpired since the last
   *          response.
   * @return the response
   */
  public Response getResponse(String userName, String text, Long delay) {
    ChatData chatData = sessions.get(getCurrentBotName()).get(userName);
    long delta = System.currentTimeMillis() - chatData.lastResponseTime.getTime();
    if (delta > delay) {
      return getResponse(userName, text);
    } else {
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
   * Return a list of all patterns that the AIML Bot knows to match against.
   * 
   * @param botName
   *          the bots name from which to return it's patterns.
   * @return a list of all patterns loaded into the aiml brain
   */
  public ArrayList<String> listPatterns(String botName) {
    ArrayList<String> patterns = new ArrayList<String>();
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
  public void onText(String text) {
    // What else should we do here? seems reasonable to just do this.
    // this should actually call getResponse
    // on input, get the proper response
    // Response resp = getResponse(text);
    getResponse(text);
    // push that to the next end point.
    // invoke("publishText", resp.msg);
  }

  private OOBPayload parseOOB(String oobPayload) {

    // TODO: fix the damn double encoding issue.
    // we have user entered text in the service/method
    // and params values.
    // grab the service
    Pattern servicePattern = Pattern.compile("<service>(.*?)</service>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
    Matcher serviceMatcher = servicePattern.matcher(oobPayload);
    serviceMatcher.find();
    String serviceName = serviceMatcher.group(1);

    Pattern methodPattern = Pattern.compile("<method>(.*?)</method>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
    Matcher methodMatcher = methodPattern.matcher(oobPayload);
    methodMatcher.find();
    String methodName = methodMatcher.group(1);

    Pattern paramPattern = Pattern.compile("<param>(.*?)</param>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
    Matcher paramMatcher = paramPattern.matcher(oobPayload);
    ArrayList<String> params = new ArrayList<String>();
    while (paramMatcher.find()) {
      // We found some OOB text.
      // assume only one OOB in the text?
      String param = paramMatcher.group(1);
      params.add(param);
    }
    OOBPayload payload = new OOBPayload(serviceName, methodName, params);
    // log.info(payload.toString());
    return payload;

    // JAXB stuff blows up because the response from program ab is already
    // xml decoded!
    //
    // JAXBContext jaxbContext;
    // try {
    // jaxbContext = JAXBContext.newInstance(OOBPayload.class);
    // Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    // log.info("OOB PAYLOAD :" + oobPayload);
    // Reader r = new StringReader(oobPayload);
    // OOBPayload oobMsg = (OOBPayload) jaxbUnmarshaller.unmarshal(r);
    // return oobMsg;
    // } catch (JAXBException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }

    // log.info("OOB tag found, but it's not an MRL tag. {}", oobPayload);
    // return null;
  }

  private List<OOBPayload> processOOB(String text) {
    // Find any oob tags
    ArrayList<OOBPayload> payloads = new ArrayList<OOBPayload>();
    Matcher oobMatcher = oobPattern.matcher(text);
    while (oobMatcher.find()) {
      // We found some OOB text.
      // assume only one OOB in the text?
      String oobPayload = oobMatcher.group(0);
      Matcher mrlMatcher = mrlPattern.matcher(oobPayload);
      while (mrlMatcher.find()) {
        String mrlPayload = mrlMatcher.group(0);
        OOBPayload payload = parseOOB(mrlPayload);
        payloads.add(payload);
        // TODO: maybe we dont' want this?
        // Notifiy endpoints
        invoke("publishOOBText", mrlPayload);
        // grab service and invoke method.
        ServiceInterface s = Runtime.getService(payload.getServiceName());
        if (s == null) {
          log.warn("Service name in OOB/MRL tag unknown. {}", mrlPayload);
          return null;
        }
        // TODO: should you be able to be synchronous for this
        // execution?
        Object result = null;
        if (payload.getParams() != null) {
          result = s.invoke(payload.getMethodName(), payload.getParams().toArray());
        } else {
          result = s.invoke(payload.getMethodName());
        }
        log.info("OOB PROCESSING RESULT: {}", result);
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

  /*
   * publishing method of the pub sub pair - with addResponseListener allowing
   * subscriptions pub/sub routines have the following pattern
   * 
   * publishing routine -&gt; publishX - must be invoked to provide data to
   * subscribers subscription routine -&gt; addXListener - simply adds a Service
   * listener to the notify framework any service which subscribes must
   * implement -&gt; onX(data) - this is where the data will be sent (the
   * call-back)
   * 
   */
  public Response publishResponse(Response response) {
    return response;
  }

  /*
   * Test only publishing point - for simple consumers
   */
  public String publishResponseText(Response response) {
    return response.msg;
  }

  @Override
  public String publishText(String text) {
    // clean up whitespaces & cariage return
    text = text.replaceAll("\\n", " ");
    text = text.replaceAll("\\r", " ");
    text = text.replaceAll("\\s{2,}", " ");
    return text;
  }

  public String publishRequest(String text) {
    return text;
  }

  public void reloadSession(String session, String botName) {
    reloadSession(getPath(), session, botName);
  }

  public void reloadSession(String path, String userName, String botName) {

    if (sessions.containsKey(botName) && sessions.get(botName).containsKey(userName)) {
      // TODO: will garbage collection clean up the bot now ?
      // Or are there other handles to it?
      sessions.get(botName).remove(userName);
      log.info("{} session removed", sessions);
    }
    bot = null;
    // TODO: we should make sure we keep the same path as before.
    startSession(path, userName, getCurrentBotName());
  }

  /**
   * Persist the predicates for all known sessions in the robot.
   * 
   */
  public void savePredicates() throws IOException {
    for (String botName : sessions.keySet()) {
      for (String userName : sessions.get(botName).keySet()) {
        String sessionPredicateFilename = createSessionPredicateFilename(userName, botName);
        File sessionPredFile = new File(sessionPredicateFilename);
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
      }
    }
    log.info("Done saving predicates.");
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

  public void startSession() {
    startSession(null);
  }

  public void startSession(String username) {
    startSession(username, getCurrentBotName());
  }

  /**
   * Load the AIML 2.0 Bot config and start a chat session. This must be called
   * after the service is created.
   * 
   * @param username
   *          - The new user name
   * @param botName
   *          - The name of the bot to load. (example: alice2)
   */
  public void startSession(String username, String botName) {
    startSession(getPath(), username, botName);
  }

  public void startSession(String path, String userName, String botName) {
    startSession(path, userName, botName, MagicBooleans.defaultLocale);
  }

  public void startSession(String path, String userName, String botName, Locale locale) {
    // Session is between a user and a bot. key is compound.
    if (sessions.containsKey(botName) && sessions.get(botName).containsKey(userName)) {
      warn("Session %s %s already created", botName, userName);
      return;
    }

    ready = false;
    this.setPath(path);
    info("Starting chat session path: %s username: %s botname: %s", path, userName, botName);
    this.setCurrentBotName(botName);
    this.setCurrentUserName(userName);

    // TODO: remove this completely!
    // Ignore the return value, this checkBrain method doesn't work as expected for bots defined externally.
    checkBrain(botName);

    // TODO: manage the bots in a collective pool/hash map.
    // TODO: check for corrupted aiml inside pAB code -> NPE ! ( blocking inside standalone jar )
    if (bot == null) {
      bot = new Bot(botName, path, locale);
    } else if (!botName.equalsIgnoreCase(bot.name)) {
      bot = new Bot(botName, path, locale);
    }

    // Hijack all the SRAIX requests and implement them as a synchronous call to a service to 
    // return a string response for programab...
    MrlSraixHandler sraixHandler = new MrlSraixHandler();
    bot.setSraixHandler(sraixHandler);

    Chat chat = new Chat(bot);
    // for (Category c : bot.brain.getCategories()) {
    // log.info(c.getPattern());
    // }
    //
    // String resp = chat.multisentenceRespond("hello");

    // load session specific predicates, these override the default ones.
    String sessionPredicateFilename = createSessionPredicateFilename(userName, botName);
    chat.predicates.getPredicateDefaults(sessionPredicateFilename);

    HashMap<String, ChatData> session = new HashMap<String, ChatData>();
    session.put(userName, new ChatData(chat));

    // take care of not kill other sessions...
    if (sessions.containsKey(botName)) {
      sessions.get(botName).putAll(session);
    } else {
      sessions.put(botName, session);
    }

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
      // robot surname is stored inside default.predicates, not inside system.prop
      setPredicate(userName, "botname", getPredicate("default", "botname"));
      try {
        savePredicates();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    // END TODO

    // this.currentBotName = botName;
    // String userName = chat.predicates.get("name");
    log.info("Started session for bot name:{} , username:{}", botName, userName);
    // TODO: to make sure if the start session is updated, that the button
    // updates in the gui ?
    setReady(true);
  }

  public void addCategory(Category c) {
    bot.brain.addCategory(c);
  }

  public void addCategory(String pattern, String template, String that) {
    log.info("Adding category {} to respond with {} for the that context {}", pattern, template, that);
    // TODO: expose that / topic / etc..
    /// TODO: what filename?!
    int activationCnt = 0;
    String topic = "*";
    // TODO: what is this used for?
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
   * TODO : check things using it
   */
  @Deprecated
  public boolean setUsername(String username) {
    startSession(this.getPath(), username, this.getCurrentBotName());
    return true;
  }

  public void writeAIML() {
    if (bot != null) {
      bot.writeAIMLFiles();
    }
  }

  @Deprecated
  public void writeAIMLIF() {
    if (bot != null) {
      bot.writeAIMLIFFiles();
    }
  }

  /**
   * writeAndQuit will write brain to disk
   * For learn.aiml is concerned
   */
  public void writeAndQuit() {
    if (bot == null) {
      log.info("no bot - don't need to write and quit");
      return;
    }
    try {
      savePredicates();
      //important to save learnf.aiml
      writeAIML();
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    bot.writeQuit();
  }

  // getters - setters

  public void setPath(String path) {
    if (path != null && !path.equals(this.path)) {
      this.path = path;
      // path changed, we need to update bots list
      getBots();
      broadcastState();
    }
  }

  public void setCurrentBotName(String currentBotName) {
    this.currentBotName = currentBotName;
  }

  public void setVisualDebug(Boolean visualDebug) {
    this.visualDebug = visualDebug;
    broadcastState();
  }

  public Boolean getVisualDebug() {
    if (visualDebug == null) {
      visualDebug = true;
    }
    return visualDebug;
  }

  public void setCurrentUserName(String currentUserName) {
    this.currentUserName = currentUserName;
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

  public HashSet<String> getBots() {
    availableBots.clear();
    File programAbDir = new File(String.format("%s/bots", getPath()));
    if (!programAbDir.exists() || !programAbDir.isDirectory()) {
      log.info("%s does not exist !!!");
    } else {
      File[] listOfFiles = programAbDir.listFiles();
      for (int i = 0; i < listOfFiles.length; i++) {
        if (listOfFiles[i].isFile()) {
          // System.out.println("File " + listOfFiles[i].getName());
        } else if (listOfFiles[i].isDirectory()) {
          availableBots.add(listOfFiles[i].getName());
        }
      }
    }
    return availableBots;
  }

  // Framework

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
    writeAndQuit();
    super.stopService();
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
    meta.addCategory("intelligence");
    meta.addDependency("program-ab", "program-ab-data", "1.1", "zip");
    meta.addDependency("program-ab", "program-ab-kw", "0.0.8.4");
    meta.addDependency("org.json", "json", "20090211");
    //used by FileIO
    meta.addDependency("commons-io", "commons-io", "2.5");
    // This is for CJK support in ProgramAB. 
    // TODO: move this into the published POM for ProgramAB so they are pulled in transiently.
    meta.addDependency("org.apache.lucene", "lucene-analyzers-common", "7.4.0");
    meta.addDependency("org.apache.lucene", "lucene-analyzers-kuromoji", "7.4.0");
    meta.addCategory("ai","control");
    return meta;
  }
  
  @Override
  public void startService() {
    super.startService();
    load();
  }

  public static void main(String s[]) throws IOException {
    try {
      LoggingFactory.init(Level.WARN);
      Runtime.start("gui", "SwingGui");
      //Runtime.start("webgui", "WebGui");

      ProgramAB brain = (ProgramAB) Runtime.start("brain", "ProgramAB");

      //logging.setLevel("class org.myrobotlab.service.ProgramAB", "INFO"); //org.myrobotlab.service.ProgramAB

      //WebkitSpeechRecognition ear = (WebkitSpeechRecognition) Runtime.start("ear", "WebkitSpeechRecognition");
      //MarySpeech mouth = (MarySpeech) Runtime.start("mouth", "MarySpeech");

      // mouth.attach(ear);
      // brain.attach(ear);
      //brain.attach(mouth);

      //brain.startSession("default", "en-US");
      //brain.startSession("c:\\dev\\workspace\\pyrobotlab\\home\\kwatters\\harry", "kevin", "harry");

      //brain.savePredicates();
    } catch (Exception e) {
      log.error("main threw", e);
    }

  }

}