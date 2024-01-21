package org.myrobotlab.programab;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.alicebot.ab.Chat;
import org.alicebot.ab.Predicates;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.programab.handlers.oob.OobProcessor;
import org.myrobotlab.programab.models.Event;
import org.myrobotlab.programab.models.Mrl;
import org.myrobotlab.programab.models.Template;
import org.myrobotlab.service.ProgramAB;
import org.myrobotlab.service.config.ProgramABConfig;
import org.slf4j.Logger;

/**
 * Session provides ProgramAB session info and reference to Chat and Bot
 * 
 * @author GroG
 *
 */
public class Session {

  transient public final static Logger log = LoggerFactory.getLogger(ProgramAB.class);

  /**
   * name of the user that owns this session
   */
  public String userName;

  /**
   * last time the bot responded
   */
  public Date lastResponseTime = null;

  /**
   * bot will prompt users if enabled trolling is true after
   * maxConversationDelay has passed
   */
  public boolean enableTrolling = false;

  /**
   * Number of milliseconds before the robot starts talking on its own.
   */
  public int maxConversationDelay = 5000;

  /**
   * general bot information
   */
  public transient BotInfo botInfo;

  /**
   * interface to program-ab
   */
  public transient Chat chat;

  /**
   * service that manages this session
   */
  private transient ProgramAB programab;

  /**
   * current file associated with this user and session
   */
  public File predicatesFile;

  /**
   * predicate data associated with this session
   */
  protected Predicates predicates = null;

  /**
   * current topic of this session
   */
  public String currentTopic = null;

  /**
   * Session for a user and bot
   * 
   * @param programab
   *                  program ab for this session
   * @param userName
   *                  userName
   * @param botInfo
   *                  the bot for the session
   * 
   */
  public Session(ProgramAB programab, String userName, BotInfo botInfo) {
    this.programab = programab;
    this.userName = userName;
    this.botInfo = botInfo;
    this.chat = loadChat();
    predicates = chat.predicates;

    Event event = new Event(programab.getName(), userName, null, null);
    programab.invoke("publishSession", event);

    ProgramABConfig config = programab.getConfig();
    if (config.startTopic != null) {
      chat.predicates.put("topic", config.startTopic);
    }

    this.maxConversationDelay = config.maxConversationDelay;
    this.enableTrolling = config.enableTrolling;

  }

  private synchronized Chat getChat() {
    return chat;
  }

  private Chat loadChat() {
    Chat chat = new Chat(botInfo.getBot());
    // loading predefined predicates - if they exist
    File userPredicates = new File(
        FileIO.gluePaths(botInfo.path.getAbsolutePath(), String.format("config/%s.predicates.txt", userName)));
    if (userPredicates.exists()) {
      predicatesFile = userPredicates;
      chat.predicates.getPredicateDefaults(userPredicates.getAbsolutePath());
    }

    return chat;
  }

  public void savePredicates() {
    StringBuilder sb = new StringBuilder();
    TreeSet<String> sort = new TreeSet<>();
    sort.addAll(getChat().predicates.keySet());
    for (String predicate : sort) {
      String value = getChat().predicates.get(predicate);
      if (!predicate.startsWith("cfg_")) {
        sb.append(predicate + ":" + value + "\n");
      }
    }
    File predicates = new File(
        FileIO.gluePaths(botInfo.path.getAbsolutePath(), String.format("config/%s.predicates.txt", userName)));
    predicates.getParentFile().mkdirs();
    log.info("bot : {} user : {} saving predicates filename : {} ", botInfo.currentBotName, userName, predicates);
    try {
      FileWriter writer = new FileWriter(predicates, StandardCharsets.UTF_8);
      writer.write(sb.toString());
      writer.close();

    } catch (Exception e) {
      log.error("writing predicates threw", e);
    }
  }

  /**
   * Get all current predicate names and values
   * 
   * @return
   */
  public Map<String, String> getPredicates() {
    TreeMap<String, String> sort = new TreeMap<>();
    sort.putAll(getChat().predicates);
    return sort;
  }

  public Response getResponse(String inText) {
    try {
      String returnText = getChat().multisentenceRespond(inText);
      String xml = String.format("<template>%s</template>", returnText);
      Template template = XmlParser.parseTemplate(xml);

      OobProcessor handler = programab.getOobProcessor();
      handler.process(template.oob, true); // block by default

      List<Mrl> mrl = template.oob != null ? template.oob.mrl : null;
      // returned all text inside template but outside oob
      Response response = new Response(userName, botInfo.currentBotName, template.text, mrl);
      return response;
    } catch (Exception e) {
      programab.error(e);
    }
    return new Response(userName, botInfo.currentBotName, "", null);
  }

  public Chat reload() {
    botInfo.reload();
    chat = loadChat();
    return chat;
  }

  public void remove(String predicateName) {
    getChat().predicates.remove(predicateName);
  }

  public void setPredicate(String predicateName, String predicateValue) {
    getChat().predicates.put(predicateName, predicateValue);
  }

  public String getPredicate(String predicateName) {
    return getChat().predicates.get(predicateName);
  }

  public String getUsername() {
    return userName;
  }

  public Object getBotType() {
    return botInfo.currentBotName;
  }

}
