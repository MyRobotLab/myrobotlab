package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.framework.Message;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.programab.BotInfo;
import org.myrobotlab.programab.Response;
import org.myrobotlab.programab.Session;
import org.myrobotlab.service.data.Locale;
import org.myrobotlab.service.data.Utterance;
import org.slf4j.Logger;

public class ProgramABTest {

  public final static Logger log = LoggerFactory.getLogger(ProgramABTest.class);

  static protected final String PIKACHU = "pikachu";

  static protected final String LLOYD = "lloyd";

  private String testResources = "src/test/resources/ProgramAB";

  static private ProgramAB lloyd;

  static private ProgramAB pikachu;

  private String username = "testUser";

  // This method runs once before any test method in the class
  @BeforeClass
  public static void setUpClass() {
    System.out.println("BeforeClass - Runs once before any test method");
    lloyd = (ProgramAB) Runtime.start(LLOYD, "ProgramAB");
    pikachu = (ProgramAB) Runtime.start(PIKACHU, "ProgramAB");

    // very first inits - all should work !
    assertTrue("4+ standard", lloyd.getBots().size() >= 4);
    // should require very little to start ! - this is a requirement !
    Response response = lloyd.getResponse("Hi");

    // expect Alice's aiml processed
    assertTrue(response.msg.startsWith("Hi"));

    Session session = lloyd.getSession();
    assertEquals("default user should be human", "human", session.getUsername());
    assertEquals("default botType should be Alice", "Alice", session.getBotType());

  }

  // This method runs once after all test methods in the class have been
  // executed
  @AfterClass
  public static void tearDownClass() {
    Runtime.release(LLOYD);
    Runtime.release(PIKACHU);
  }

  // This method runs before each test method
  @Before
  public void setUp() {
    System.out.println("Before - Runs before each test method");
    // Perform setup tasks specific to each test method

    // add a couple test bots
    List<File> bots = lloyd.scanForBots(testResources + "/bots");
    assertTrue("2+ test bots", bots.size() >= 2);
    assertTrue("6+ bots total", lloyd.getBots().size() >= 6);

    pikachu.scanForBots(testResources + "/bots");

    // validate newly created programab can by default start a session
    Session session = lloyd.getSession();
    assertNotNull(session);

    lloyd.setBotType("lloyd");
    assertEquals("lloyd", lloyd.getBotType());

    // validate error is called when invalid bot type set

    // load the bot brain for the chat with the user
    lloyd.setSession(username, LLOYD);
    assertEquals(username, lloyd.getUsername());
    // clean out any aimlif the bot that might
    // have been saved in a previous test run!
    String aimlIFPath = testResources + "/bots/" + LLOYD + "/aimlif";
    File aimlIFPathF = new File(aimlIFPath);
    if (aimlIFPathF.isDirectory()) {
      for (File f : aimlIFPathF.listFiles()) {
        // if there's a file here.
        log.info("Deleting pre-existing AIMLIF files : {}", f.getAbsolutePath());
        f.delete();
      }
    }
  }

  @Test
  public void testOnUtterance() throws Exception {

    MockGateway gateway = (MockGateway) Runtime.start("gateway", "MockGateway");
    lloyd.addListener("publishUtterance", "mocker@mockId");

    Utterance utterance = new Utterance();
    utterance.username = "human";
    utterance.text = "HELLO";
    utterance.channelBotName = "Mr.Turing";

    gateway.sendWithDelay("lloyd", "onUtterance", utterance);
    Message msg = gateway.waitForMsg("mocker", "onUtterance", 50);
    assertNotNull(msg);
    assertEquals("Passed", ((Utterance) msg.data[0]).text);
  }

  public void addCategoryTest() throws IOException {
    testService.addCategory("BOOG", "HOWDY");
    Response resp = testService.getResponse(username, "BOOG");
    assertTrue(resp.msg.equals("HOWDY"));
  }

  // This method runs after each test method
  @After
  public void tearDown() {
    System.out.println("After - Runs after each test method");
  }

  @Test
  public void testAddCategoryTest() throws IOException {
    lloyd.addCategory("ABCDEF", "ABCDEF");
    // String username = lloyd.getUsername();
    // username,
    Response resp = lloyd.getResponse("ABCDEF");
    assertTrue(resp.msg.equals("ABCDEF"));
  }

  @Test
  public void testListPatterns() {
    ArrayList<String> res = lloyd.listPatterns(LLOYD);
    assertTrue(res.size() > 0);
  }

  // the pannous service seems borked at the moment due to bad ssl, and maybe
  // other stuff.. kwatters: I recommend we build our own service that does this
  // stuff
  // @Test
  public void pannousTest() throws IOException {
    Response resp = lloyd.getResponse(username, "SHOW ME INMOOV");
    // System.out.println(resp);
    boolean contains = resp.msg.contains("http");
    assertTrue(contains);
  }

  @Test
  public void testSraixOOB() throws IOException {
    // Response resp = testService.getResponse(username, "MRLSRAIX");
    // System.out.println(resp);
    // boolean contains = resp.msg.contains("foobar");
    // assertTrue(contains);
    Response resp = lloyd.getResponse(username, "OOBMRLSRAIX");
    // System.out.println(resp);
    boolean contains = resp.msg.contains("You are talking to lloyd");
    assertTrue(contains);
  }

  @Test
  public void testSraix() throws IOException {
    if (Runtime.hasInternet()) {
      Response resp = lloyd.getResponse(username, "MRLSRAIX");
      // Response resp = testService.getResponse(username, "Why is the sky
      // blue?");
      // System.out.println(resp);
      // System.out.println(resp);
      boolean contains = resp.msg.contains("information");
      assertTrue(contains);
    }
  }

  @Test
  public void testAddEntryToSetAndMaps() throws IOException {
    // TODO: This does NOT work yet!
    Response resp = lloyd.getResponse(username, "Add Jabba to the starwarsnames SET");
    assertEquals("Ok...", resp.msg);
    resp = lloyd.getResponse(username, "Add jabba equals Jabba the Hut to the starwars MAP");
    assertEquals("Ok...", resp.msg);
    resp = lloyd.getResponse(username, "DO YOU LIKE Jabba?");
    assertEquals("Jabba the Hut is awesome.", resp.msg);
    // TODO : re-enable this one?
    // now test creating a new set.
    resp = lloyd.getResponse(username, "Add bourbon to the whiskey SET");
    assertEquals("Ok...", resp.msg);
    resp = lloyd.getResponse(username, "NEWSETTEST bourbon");
    // assertEquals("bourbon is a whiskey", resp.msg);
  }

  @Test
  public void testJapanese() throws IOException {
    pikachu.scanForBots(testResources + "/bots");
    pikachu.setBotType("pikachu");
    // setting Japanese locality
    pikachu.setLocale("ja");
    // load the bot brain for the chat with the user
    pikachu.setSession(username, PIKACHU);
    Response resp = pikachu.getResponse("私はケビンです");
    assertEquals("あなたに会えてよかったケビン", resp.msg);
    Runtime.release(PIKACHU);
  }

  @Test
  public void testLearn() throws IOException {
    // Response resp1 = testService.getResponse(session, "SET FOO BAR");
    // System.out.println(resp1.msg);
    Response resp = lloyd.getResponse(username, "LEARN AAA IS BBB");
    // System.out.println(resp.msg);
    resp = lloyd.getResponse(username, "WHAT IS AAA");
    assertEquals("BBB", resp.msg);
  }

  @Test
  public void testMultiSession() throws IOException {
    ProgramAB lloyd = (ProgramAB) Runtime.start("lloyd", "ProgramAB");
    lloyd.setBotType("lloyd");
    // load the bot brain for the chat with the user
    lloyd.setSession("user1", "lloyd");
    Response res = lloyd.getResponse("My name is Kevin");
    System.out.println(res);
    lloyd.setSession("user2", "lloyd");
    res = lloyd.getResponse("My name is Grog");
    System.out.println(res);
    lloyd.setSession("user1", "lloyd");
    Response respA = lloyd.getResponse("What is my name?");
    System.out.println(respA);
    lloyd.setSession("user2", "lloyd");
    Response respB = lloyd.getResponse("What is my name?");
    System.out.println(respB);
    lloyd.setSession(username, LLOYD);
    assertEquals("Kevin", respA.msg);
    assertEquals("Grog", respB.msg);
  }

  @Test
  public void testOOBTags() throws Exception {

    // Response resp = testService.getResponse(username, "OOB TEST");

    ProgramAB lloyd = (ProgramAB) Runtime.start("lloyd", "ProgramAB");
    Response resp = lloyd.getResponse(username, "OOB TEST");

    assertEquals("OOB Tag Test", resp.msg);

    // TODO figure a mock object that can wait on a callback to let us know the
    // python service is started.
    // wait up to 5 seconds for python service to start
    long maxWait = 6000;
    int i = 0;

    while (Runtime.getService("oobclock") == null) {
      Thread.sleep(100);
      log.info("waiting for oobclock to start...");
      i++;
      if (i > maxWait) {
        Assert.assertFalse("took too long to process OOB tag", i > maxWait);
      }
    }
    Assert.assertNotNull(Runtime.getService("oobclock"));
    Runtime.release("oobclock");
  }

  @Test
  public void testPredicates() {
    // test removing the predicate if it exists
    lloyd.setPredicate(username, "name", "foo1");
    String name = lloyd.getPredicate(username, "name");
    // validate it's set properly
    assertEquals("foo1", name);
    lloyd.removePredicate(username, "name");
    // validate the predicate doesn't exist
    name = lloyd.getPredicate(username, "name");
    // TODO: is this valid? one would expect it would return null.
    assertEquals("unknown", name);
    // set a predicate
    lloyd.setPredicate(username, "name", "foo2");
    name = lloyd.getPredicate(username, "name");
    // validate it's set properly
    assertEquals("foo2", name);
  }

  @Test
  public void testProgramAB() throws Exception {
    // a response
    Response resp = lloyd.getResponse(username, "UNIT TEST PATTERN");
    // System.out.println(resp.msg);
    assertEquals("Unit Test Pattern Passed", resp.msg);
  }

  @Test
  public void testSavePredicates() throws IOException {
    long uniqueVal = System.currentTimeMillis();
    String testValue = String.valueOf(uniqueVal);
    Response resp = lloyd.getResponse(username, "SET FOO " + testValue);
    assertEquals(testValue, resp.msg);
    lloyd.savePredicates();
    lloyd.reloadSession(username, LLOYD);
    resp = lloyd.getResponse(username, "GET FOO");
    assertEquals("FOO IS " + testValue, resp.msg);
  }

  @Test
  public void testUppercase() {
    // test a category where the aiml tag is uppercased.
    Response resp = lloyd.getResponse(username, "UPPERCASE");
    assertEquals("Passed", resp.msg);
  }

  @Test
  public void testSets() throws IOException {
    Response resp = lloyd.getResponse(username, "SETTEST CAT");
    assertEquals("An Animal.", resp.msg);
    resp = lloyd.getResponse(username, "SETTEST MOUSE");
    assertEquals("An Animal.", resp.msg);
    resp = lloyd.getResponse(username, "SETTEST DOG");
    // System.out.println(resp.msg);
    assertEquals("An Animal.", resp.msg);
  }

  @Test
  public void testSetsAndMaps() throws IOException {
    Response resp = lloyd.getResponse(username, "DO YOU LIKE Leah?");
    assertEquals("Princess Leia Organa is awesome.", resp.msg);
    resp = lloyd.getResponse(username, "DO YOU LIKE Princess Leah?");
    assertEquals("Princess Leia Organa is awesome.", resp.msg);
  }

  @Test
  public void testTopicCategories() throws IOException {
    lloyd.removePredicate(username, "topic");
    String topic = lloyd.getTopic();
    assertEquals("unknown", topic);
    // Top level definition
    Response resp = lloyd.getResponse(username, "TESTTOPICTEST");
    assertEquals("TOPIC IS unknown", resp.msg);
    resp = lloyd.getResponse(username, "SET TOPIC TEST");
    resp = lloyd.getResponse(username, "TESTTOPICTEST");
    assertEquals("TEST TOPIC RESPONSE", resp.msg);
    // maybe we can still fallback to non-topic responses.
    resp = lloyd.getResponse(username, "HI");
    assertEquals("Hello user!", resp.msg);
    // TODO: how the heck do we unset a predicate from AIML?
    lloyd.removePredicate(username, "topic");
    resp = lloyd.getResponse(username, "TESTTOPICTEST");
    assertEquals("TOPIC IS unknown", resp.msg);
  }

  @Test
  public void testUmlaut() throws IOException {
    Response resp = lloyd.getResponse(username, "Lars Ümlaüt");
    // @GroG says - "this is not working"
    assertEquals("He's a character from Guitar Hero!", resp.msg);
  }

  @Test
  public void testLocales() {
    ProgramAB lloyd = (ProgramAB) Runtime.start("pikachu", "ProgramAB");
    lloyd.addBots(testResources + "/" + "bots");
    lloyd.setBotType("pikachu");
    Map<String, Locale> locales = lloyd.getLocales();
    assertTrue(locales.containsKey("ja"));
    // release the service we created in this method.
    pikachu.releaseService();
  }

  @Test
  public void testReload() throws IOException {
    lloyd.getResponse("my name is george");
    Response response = lloyd.getResponse("what is my name?");

    BotInfo botInfo = lloyd.getBotInfo();

    String newFile = botInfo.path.getAbsolutePath() + File.separator + "aiml" + File.separator + "newFileCategory.aiml";
    String newFileCategory = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><aiml><category><pattern>RELOAD</pattern><template>I have reloaded</template></category></aiml>";
    FileIO.toFile(newFile, newFileCategory);

    lloyd.reload();

    response = lloyd.getResponse("RELOAD");
    assertTrue(response.msg.contains("I have reloaded"));

    response = lloyd.getResponse("what is my name?");
    assertTrue(response.msg.contains("george"));

    // clean out file
    new File(newFile).delete();

  }

  @Test
  public void testDefaultSession() throws IOException {
    // minimal startup - create the service get a response
    ProgramAB lloyd = (ProgramAB) Runtime.start("lloyd", "ProgramAB");
    lloyd.setBotType("lloyd");
    assertTrue(lloyd.getBots().size() > 0);
    // test for a response
    Response response = lloyd.getResponse("Hello");
    assertTrue(!response.msg.startsWith("I have no"));

    // not sure if this is worth testing - there might be more
    // assertEquals("Alice", alice.getCurrentBotName());
    // assertEquals("default", alice.getCurrentUserName());
    lloyd.releaseService();

  }

  // TODO - tests
  // ProgramAB starts - it should find its own bot info's
  // set username = default
  // set LLOYD = what is available if NOT set
  // getResponse() -> if current session doesn't exist - get bot
  // if current bot doesn't exist - attempt to activate it
  // test - absolute minimal setup and getResponse ... 2 lines ? 1?
  // test - setting direct location addBotInfo(path)
  // test adding new bots from new locations
  // test mrl.properties - and the lack of those properties
  // verify conversation starter
  // verify inactivity conversation trolling

}