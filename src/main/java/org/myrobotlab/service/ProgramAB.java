package org.myrobotlab.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alicebot.ab.AIMLMap;
import org.alicebot.ab.AIMLSet;
import org.alicebot.ab.Bot;
import org.alicebot.ab.Category;
import org.alicebot.ab.Chat;
import org.alicebot.ab.Predicates;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.programab.ChatData;
import org.myrobotlab.programab.MrlSraixHandler;
import org.myrobotlab.programab.OOBPayload;
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

  HashSet<String> bots = new HashSet<String>();

  private String path = "ProgramAB";
  public boolean aimlError = false;

  /**
   * botName - is un-initialized to preserve serialization stickyness
   */
  // String botName;
  // This is the username that is chatting with the bot.
  // String currentSession = "default";
  // Session is a user and a bot. so the key to the session should be the
  // username, and the bot name.
  transient HashMap<String, HashMap<String, ChatData>> sessions = new HashMap<String, HashMap<String, ChatData>>();
  // TODO: better parsing than a regex...
  transient Pattern oobPattern = Pattern.compile("<oob>.*?</oob>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
  transient Pattern mrlPattern = Pattern.compile("<mrl>.*?</mrl>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);

  // a guaranteed bot we have
  private String currentBotName = "en-US";
  // this is the username that is chatting with the bot.
  private String currentUserName = "default";
  public boolean loading = false;

  static final long serialVersionUID = 1L;
  static int savePredicatesInterval = 60 * 1000 * 5; // every 5 minutes
  public String wasCleanyShutdowned;

  public ProgramAB(String name) {
    super(name);
    // Tell programAB to persist it's learned predicates about people
    // every 30 seconds.
    addTask("savePredicates", savePredicatesInterval, 0, "savePredicates");
    // TODO: Lazy load this!
    // look for local bots defined
    File programAbDir = new File(String.format("%s/bots", getPath()));
    if (!programAbDir.exists() || !programAbDir.isDirectory()) {
      log.info("%s does not exist !!!");
    } else {
      File[] listOfFiles = programAbDir.listFiles();
      for (int i = 0; i < listOfFiles.length; i++) {
        if (listOfFiles[i].isFile()) {
          // System.out.println("File " + listOfFiles[i].getName());
        } else if (listOfFiles[i].isDirectory()) {
          bots.add(listOfFiles[i].getName());
        }
      }
    }
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

  public void addTextPublisher(TextPublisher service) {
    subscribe(service.getName(), "publishText");
  }

  private void cleanOutOfDateAimlIFFiles(String botName) {
    String aimlPath = getPath() + File.separator + "bots" + File.separator + botName + File.separator + "aiml";
    String aimlIFPath = getPath() + File.separator + "bots" + File.separator + botName + File.separator + "aimlif";
    aimlError = false;
    log.info("AIML FILES:");
    File folder = new File(aimlPath);
    File folderaimlIF = new File(aimlIFPath);
    if (!folder.exists()) {
      log.error("{} does not exist", aimlPath);
      aimlError = true;
      return;
    }
    if (wasCleanyShutdowned == null || wasCleanyShutdowned.isEmpty()) {
      wasCleanyShutdowned = "firstStart";
    }
    if (wasCleanyShutdowned.equals("nok")) {
      if (folderaimlIF.exists()) {
        // warn("Bad previous shutdown, ProgramAB need to recompile AimlIf
        // files. Don't worry.");
        log.info("Bad previous shutdown, ProgramAB need to recompile AimlIf files. Don't worry.");
        for (File f : folderaimlIF.listFiles()) {
          f.delete();
        }
      }
    }

    log.info(folder.getAbsolutePath());
    HashMap<String, Long> modifiedDates = new HashMap<String, Long>();
    for (File f : folder.listFiles()) {
      log.info(f.getAbsolutePath());
      // TODO: better stripping of the file extension
      String aiml = f.getName().replace(".aiml", "");
      modifiedDates.put(aiml, f.lastModified());
    }
    log.info("AIMLIF FILES:");
    folderaimlIF = new File(aimlIFPath);
    if (!folderaimlIF.exists()) {
      // TODO: throw an exception warn / log ?
      log.info("aimlif directory missing,creating it. " + folderaimlIF.getAbsolutePath());
      folderaimlIF.mkdirs();
      return;
    }
    for (File f : folderaimlIF.listFiles()) {
      log.info(f.getAbsolutePath());
      // TODO: better stripping of the file extension
      String aimlIF = f.getName().replace(".aiml.csv", "");
      Long lastMod = modifiedDates.get(aimlIF);
      if (lastMod != null) {
        if (f.lastModified() < lastMod) {
          // the AIMLIF file is newer than the AIML file.
          // delete the AIMLIF file so ProgramAB recompiles it
          // properly.
          log.info("Deleteing AIMLIF file because the original AIML file was modified. {}", aimlIF);
          f.delete();
          // edit moz4r : we need to change the last modification date to aiml
          // folder for recompilation
          sleep(1000);
          String fil = aimlPath + File.separator + "folder_updated";
          File file = new File(fil);
          file.delete();
          try {
            PrintWriter writer = new PrintWriter(fil, "UTF-8");
            writer.println(lastMod.toString());
            writer.close();
          } catch (IOException e) {
            // do something
          }
        }
      }
    }
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

    // TODO: wire this in so the gui updates properly. ??
    // broadcastState();

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

  public void setPredicate(String username, String predicateName, String predicateValue) {
    Predicates preds = getChat(username, getCurrentBotName()).predicates;
    preds.put(predicateName, predicateValue);
  }

  public void unsetPredicate(String username, String predicateName) {
    Predicates preds = getChat(username, getCurrentBotName()).predicates;
    preds.remove(predicateName);
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

  public void reloadSession(String session, String botName) {
    reloadSession(getPath(), session, botName);
  }

  public void reloadSession(String path, String userName, String botName) {
    reloadSession(path, userName, botName, false);
  }

  public void reloadSession(String path, String userName, String botName, Boolean killAimlIf) {
    loading = true;
    broadcastState();
    // kill the bot
    writeAndQuit(killAimlIf);
    // kill the session

    if (sessions.containsKey(botName) && sessions.get(botName).containsKey(userName)) {
      // TODO: will garbage collection clean up the bot now ?
      // Or are there other handles to it?
      sessions.remove(sessions.get(botName).get(userName));
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
        FileWriter predWriter = new FileWriter(sessionPredFile, false);
        for (String predicate : chat.predicates.keySet()) {
          String value = chat.predicates.get(predicate);
          predWriter.write(predicate + ":" + value + "\n");
        }
        predWriter.close();
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
    loading = true;
    this.setPath(path);
    info("Starting chat session path: %s username: %s botname: %s", path, userName, botName);
    this.setCurrentBotName(botName);
    this.setCurrentUserName(userName);

    broadcastState();
    // Session is between a user and a bot. key is compound.

    if (sessions.containsKey(botName) && sessions.get(botName).containsKey(userName)) {
      warn("Session %s %s already created", botName, userName);
      return;
    }
    if (!sessions.isEmpty()) {
      wasCleanyShutdowned = "ok";
    }
    cleanOutOfDateAimlIFFiles(botName);
    wasCleanyShutdowned = "nok";

    // TODO: manage the bots in a collective pool/hash map.
    if (bot == null) {
      bot = new Bot(botName, path);
    } else if (!botName.equalsIgnoreCase(bot.name)) {
      bot = new Bot(botName, path);
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

    if (sessions.containsKey(botName)) {
      session = sessions.get(botName);
    }
    session.put(userName, new ChatData(chat));
    sessions.put(botName, session);
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
    // this.currentBotName = botName;
    // String userName = chat.predicates.get("name");
    log.info("Started session for bot name:{} , username:{}", botName, userName);
    // TODO: to make sure if the start session is updated, that the button
    // updates in the gui ?
    this.save();
    loading = false;
    broadcastState();
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

  public void setPath(String path) {
    this.path = path;
  }

  public void setCurrentBotName(String currentBotName) {
    this.currentBotName = currentBotName;
  }

  /**
   * TODO : maybe merge / check with startsession directly
   * setUsername will check if username correspond to current session If no, a
   * new session is started
   * 
   * @param username
   *          - The new username
   * @return boolean - True if username changed
   * @throws IOException
   */
  public boolean setUsername(String username) {
    if (username.isEmpty()) {
      log.error("chatbot username is empty");
      return false;
    }
    if (sessions.isEmpty()) {
      log.info(username + " first session started");
      startSession(username);
      return false;
    }
    try {
      savePredicates();
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    if (username.equalsIgnoreCase(this.getCurrentUserName())) {
      log.info(username + " already connected");
      return false;
    }
    if (!username.equalsIgnoreCase(this.getCurrentUserName())) {
      startSession(this.getPath(), username, this.getCurrentBotName());
      setPredicate(username, "name", username);
      setPredicate("default", "lastUsername", username);
      // robot name is stored inside default.predicates, not inside system.prop
      setPredicate(username, "botname", getPredicate("default", "botname"));
      try {
        savePredicates();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      log.info(username + " session started");
      return true;
    }
    return false;
  }

  public void writeAIML() {
    bot.writeAIMLFiles();
  }

  public void writeAIMLIF() {
    bot.writeAIMLIFFiles();
  }

  public void writeAndQuit() {
    writeAndQuit(false);
  }

  /**
   * writeAndQuit will clean shutdown BOT so next stratup is sync & faaaast
   * 
   * @param killAimlIf
   *          - Delete aimlif and restart bot from aiml - Useful to push aiml
   *          modifications in realtime - Without restart whole script
   */
  public void writeAndQuit(Boolean killAimlIf) {
    if (bot == null) {
      log.info("no bot - don't need to write and quit");
      return;
    }
    try {
      savePredicates();
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    String fil = bot.aimlif_path + File.separator + "folder_updated";
    if (!killAimlIf) {
      bot.writeQuit();
      wasCleanyShutdowned = "ok";

      // edit moz4r : we need to change the last modification date to aimlif
      // folder because at this time all is compilated.
      // so programAb don't need to load AIML at startup
      sleep(1000);
      File folder = new File(bot.aimlif_path);

      for (File f : folder.listFiles()) {
        f.setLastModified(System.currentTimeMillis());
      }

    } else {
      fil = bot.aiml_path + File.separator + "folder_updated";
    }
    File file = new File(fil);
    file.delete();
    try {
      PrintWriter writer = new PrintWriter(fil, "UTF-8");
      writer.println("");
      writer.close();
    } catch (IOException e) {
      log.error("PrintWriter error");
    }
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
    try {
      savePredicates();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
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
    meta.addDependency("program-ab", "program-ab-kw", "0.0.7-SNAPSHOT");
    meta.addDependency("org.json", "json", "20090211");
    //used by FileIO
    meta.addDependency("commons-io", "commons-io", "2.5");
    //needed if we dont "install all" > HttpClient used by sraix
    meta.addDependency("org.apache.httpcomponents", "httpclient", "4.5.2");
    meta.addDependency("org.apache.httpcomponents", "httpcore", "4.4.6");
    return meta;
  }

  public static void main(String s[]) throws IOException {
    try {
      LoggingFactory.init("INFO");

      Runtime.start("gui", "SwingGui");
      // Runtime.start("webgui", "WebGui");

      ProgramAB brain = (ProgramAB) Runtime.start("brain", "ProgramAB");
      // brain.startSession("default", "alice2");
      WebkitSpeechRecognition ear = (WebkitSpeechRecognition) Runtime.start("ear", "WebkitSpeechRecognition");
      MarySpeech mouth = (MarySpeech) Runtime.start("mouth", "MarySpeech");

      // mouth.attach(ear);
      ear.attach(mouth);
      // ear.addMouth(mouth);
      brain.attach(ear);
      mouth.attach(brain);

      brain.startSession("default", "en-US");
      // ear.startListening();

      // FIXME - make this work
      // brain.attach(mouth);

      // ear.attach(mouth);
      // FIXME !!! - make this work
      // ear.attach(mouth);

      // brain.addTextPublisher(service);
      // ear.attach(brain);

      /*
       * log.info(brain.getResponse("hi there").toString());
       * log.info(brain.getResponse("こんにちは").toString());
       * log.info(brain.getResponse("test").toString());
       * log.info(brain.getResponse("").toString()); brain.setUsername("test");
       */

      brain.savePredicates();
    } catch (Exception e) {
      log.error("main threw", e);
    }

  }

  public String getPath() {
    return path;
  }

  public String getCurrentUserName() {
    return currentUserName;
  }

  public void setCurrentUserName(String currentUserName) {
    this.currentUserName = currentUserName;
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

}
