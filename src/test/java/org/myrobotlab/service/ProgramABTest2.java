package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.programab.Session;
import org.slf4j.Logger;

public class ProgramABTest2 {

  transient public final static Logger log = LoggerFactory.getLogger(ProgramABTest2.class);

  private ProgramAB programab;
  public static final String LLOYD = "lloyd";
  public static final String PIKACHU = "pikachu";
  public static final String TEST_RESOURCE_ROOT = "./src/test/resources";
  public static final String BOTS_DIR = TEST_RESOURCE_ROOT + "/ProgramAB/bots";
  public static final String LLOYD_BOTDIR = BOTS_DIR + "/" + LLOYD;
  public static final String PIKACHU_BOTDIR = BOTS_DIR + "/" + PIKACHU;

  @Before
  public void setUp() {
    programab = (ProgramAB) Runtime.start("lloyd", "ProgramAB");

    // add 2 bot path references
    programab.addBot(LLOYD_BOTDIR);
    programab.addBot(PIKACHU_BOTDIR);
    List<String> botsTypes = programab.getBots();
    assertEquals("should have 6 bots now 4 (automatic) from resource and 2 from test", 6, botsTypes.size());

    // by default there should be a valid user and bot type
    String user = programab.getUsername();
    String currentBotName = programab.getBotType();

    assertEquals("human is the first default user", "human", user);
    // if valid directories there should be "some" default bot
    assertNotNull(currentBotName);

  }

  @Test
  public void testSetBotType() {
    programab.setBotType(LLOYD);
    assertEquals("x is the first default user", LLOYD, programab.getBotType());

    programab.setBotType("bogus");
    assertEquals("should not be able to set to invalid currentBotName", LLOYD, programab.getBotType());

  }

  @Test
  public void testScanForBots() {
    List<File> botFolders = programab.scanForBots(BOTS_DIR);
    assertEquals("should be 2 folderes", 2, botFolders.size());
  }

  @Test
  public void testGetMaxConversationDelay() {
    programab.clear();
    // should work for no sessions - previously threw npe
    programab.setMaxConversationDelay(7000);
    int maxDelay = programab.getMaxConversationDelay();
    assertEquals(7000, maxDelay);
    programab.setMaxConversationDelay(5000);
    maxDelay = programab.getMaxConversationDelay();
    assertEquals(5000, maxDelay);
    programab.setEnableAutoConversation(true);
    assertTrue(programab.getEnableAutoConversation());
    programab.setEnableAutoConversation(true);
    programab.setEnableAutoConversation(false);
    assertFalse(programab.getEnableAutoConversation());
    programab.getSession();
  }

  @Test
  public void testPredicateFun() {

  }

  @Test
  public void testCheckIfValid_WhenAimlDirectoryExists_ReturnsTrue() {
    boolean isValid = programab.checkIfValid(new File(LLOYD_BOTDIR));
    assertTrue(isValid);
  }

  @Test
  public void testCheckIfValid_WhenAimlDirectoryDoesNotExist_ReturnsFalse() {
    boolean isValid = programab.checkIfValid(new File("."));
    assertFalse(isValid);
  }

  @Test
  public void testSession() {
    Session session = programab.getSession();
    log.info(String.format("%s", session));
  }

}
