package org.myrobotlab.programab;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;

import org.alicebot.ab.Chat;
import org.alicebot.ab.Predicates;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.ProgramAB;
import org.slf4j.Logger;

/**
 * Session provides ProgramAB session info and reference to Chat and Bot
 * 
 * @author GroG
 *
 */
public class Session {

  transient public final static Logger log = LoggerFactory.getLogger(ProgramAB.class);

  public String userName;
  public boolean processOOB = true;
  public Date lastResponseTime = null;
  public boolean enableTrolling = false;
  // Number of milliseconds before the robot starts talking on its own.
  public int maxConversationDelay = 5000;

  // FIXME - could be transient ??
  transient public BotInfo botInfo;
  public transient Chat chat;

  transient ProgramAB programab;

  public File predicatesFile;

  // public Map<String,String> predicates = new TreeMap<>();
  public Predicates predicates = null;

  /**
   * Session for a user and bot
   * 
   * @param programab
   *          program ab for this session
   * @param userName
   *          username
   * @param botInfo
   *          the bot for the session
   * 
   */
  public Session(ProgramAB programab, String userName, BotInfo botInfo) {
    this.programab = programab;
    this.userName = userName;
    this.botInfo = botInfo;

  }

  /**
   * lazy loading chat
   *
   * task to save predicates and getting responses will eventually call getBot
   * we don't want initialization to create 2 when only one is needed
   * 
   * @return
   */
  private synchronized Chat getChat() {
    if (chat == null) {
      chat = new Chat(botInfo.getBot());
      // loading predefined predicates - if they exist
      File userPredicates = new File(FileIO.gluePaths(botInfo.path.getAbsolutePath(), String.format("config/%s.predicates.txt", userName)));
      if (userPredicates.exists()) {
        predicatesFile = userPredicates;
        chat.predicates.getPredicateDefaults(userPredicates.getAbsolutePath());
      }
    }
    predicates = chat.predicates;
    return chat;
  }

  public void savePredicates() {
    StringBuilder sb = new StringBuilder();
    for (String predicate : getChat().predicates.keySet()) {
      String value = getChat().predicates.get(predicate);
      sb.append(predicate + ":" + value + "\n");
    }
    File predicates = new File(FileIO.gluePaths(botInfo.path.getAbsolutePath(), String.format("config/%s.predicates.txt", userName)));
    predicates.getParentFile().mkdirs();
    log.info("Bot : {} User : {} Predicates Filename : {} ", botInfo.name, userName, predicates);
    try {
      FileOutputStream fos = new FileOutputStream(predicates);
      fos.write(sb.toString().getBytes());
      fos.close();
    } catch (Exception e) {
      log.error("writing predicates threw", e);
    }
  }

  public Response getResponse(String inText) {

    String text = getChat().multisentenceRespond(inText);

    // Find any oob tags
    ArrayList<OOBPayload> oobTags = OOBPayload.extractOOBPayloads(text, programab);

    // invoke them all if configured to do so
    if (processOOB) {
      for (OOBPayload payload : oobTags) {
        // assumption is this is non blocking invoking!
        boolean oobRes = OOBPayload.invokeOOBPayload(payload, programab.getName(), false);
        if (!oobRes) {
          // there was a failure invoking
          log.warn("Failed to invoke OOB/MRL tag : {}", OOBPayload.asOOBTag(payload));
        }
      }
    }

    // strip any oob tags if found
    if (oobTags.size() > 0) {
      text = OOBPayload.removeOOBFromString(text).trim();
    }

    Response response = new Response(userName, botInfo.name, text, oobTags);
    return response;

  }

  public Chat reload() {
    botInfo.reload();
    chat = null;
    return getChat();
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

}
