package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.framework.Service;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.ProgramAB.Response;
import org.slf4j.Logger;

public class ProgramABTest extends AbstractServiceTest {

  public final static Logger log = LoggerFactory.getLogger(ProgramABTest.class);
  private String botname = "lloyd";
  // TODO: move this to test resources
  private String testResources = "src/test/resources/ProgramAB";
  private String path = null;
  private ProgramAB testService;

  private String username = "testUser";

  public void addCategoryTest() throws IOException {
    testService.addCategory("BOOG", "HOWDY");
    Response resp = testService.getResponse(username, "BOOG");
    assertTrue(resp.msg.equals("HOWDY"));
  }

  public Service createService() {
    try {
    // LoggingFactory.init("INFO");
    log.info("Setting up the Program AB Service ########################################");
    // Load the service under test
    // a test robot
    // TODO: this should probably be created by Runtime,
    // OOB tags might not know what the service name is ?!
    testService = (ProgramAB)Runtime.start(botname, "ProgramAB");
    testService.setPath(path);
    // testService = new ProgramAB("simple");
    // testService.setPath("c:/mrl/develop/ProgramAB");

    // start the service.
    testService.startService();
    // load the bot brain for the chat with the user
    testService.startSession(username, botname);
    // clean out any aimlif the bot that might
    // have been saved in a previous test run!
    String aimlIFPath = path + "/bots/" + botname + "/aimlif";
    File aimlIFPathF = new File(aimlIFPath);
    if (aimlIFPathF.isDirectory()) {
      for (File f : aimlIFPathF.listFiles()) {
        // if there's a file here.
        log.info("Deleting pre-existing AIMLIF files : {}", f.getAbsolutePath());
        f.delete();
      }
    }
    // TODO: same thing for predicates! (or other artifacts from a previous aiml
    // test run)
    } catch(Exception e) {
      log.error("createService threw", e);
    }
    return testService;
  }

  public void listPatternsTest() {
    ArrayList<String> res = testService.listPatterns(botname);
    assertTrue(res.size() > 0);
  }

  // the pannous service seems borked at the moment due to bad ssl, and maybe
  // other stuff.. kwatters: I recommend we build our own service that does this
  // stuff
  // @Test
  public void pannousTest() throws IOException {
    Response resp = testService.getResponse(username, "SHOW ME INMOOV");
    // System.out.println(resp);
    boolean contains = resp.msg.contains("http");
    assertTrue(contains);
  }

  @Before
  public void setUp() {
    // TODO: set the location for the temp folder via :
    // System.getProperty("java.io.tmpdir")
    // LoggingFactory.init("INFO");
    // testFolder.getRoot().getAbsolutePath()
    try {
      this.path = testFolder.getRoot().getAbsolutePath() + File.separator + "ProgramAB";
      FileIO.copy(testResources, path);
    } catch (IOException e) {
      log.warn("Error extracting resources for test. {}", testResources);
      Assert.assertNotNull(e);
    }
  }

  public void sraixOOBTest() throws IOException {
    // Response resp = testService.getResponse(username, "MRLSRAIX");
    // System.out.println(resp);
    // boolean contains = resp.msg.contains("foobar");
    // assertTrue(contains);
    Response resp = testService.getResponse(username, "OOBMRLSRAIX");
    // System.out.println(resp);
    boolean contains = resp.msg.contains("You are talking to lloyd");
    assertTrue(contains);
  }

  public void sraixTest() throws IOException {
    if (Runtime.hasInternet()) {
      // Response resp = testService.getResponse(username, "MRLSRAIX");
      
      Response resp = testService.getResponse(username, "Why is the sky blue?");
      System.out.println(resp);
      // System.out.println(resp);
      boolean contains = resp.msg.contains("atmosphere");
      assertTrue(contains);
    }
  }

  public void testAddEntryToSetAndMaps() throws IOException {
    // TODO: This does NOT work yet!
    Response resp = testService.getResponse(username, "Add Jabba to the starwarsnames set");
    assertEquals("Ok...", resp.msg);
    resp = testService.getResponse(username, "Add jabba equals Jabba the Hut to the starwars map");
    assertEquals("Ok...", resp.msg);
    resp = testService.getResponse(username, "DO YOU LIKE Jabba?");
    assertEquals("Jabba the Hut is awesome.", resp.msg);
    // TODO : re-enable this one?
    // now test creating a new set.
    resp = testService.getResponse(username, "Add bourbon to the whiskey set");
    assertEquals("Ok...", resp.msg);
    resp = testService.getResponse(username, "NEWSETTEST bourbon");
    // assertEquals("bourbon is a whiskey", resp.msg);
  }

  @Test
  public void testJapanese() throws IOException {

    ProgramAB pikachu = (ProgramAB)Runtime.start("pikachu", "ProgramAB");
    pikachu.setPath(path);
    // pikachu the service.
    pikachu.startService();
    // load the bot brain for the chat with the user
    pikachu.startSession(path, username, "pikachu", new Locale("ja"));
    Response resp = pikachu.getResponse("私はケビンです");
    assertEquals("あなたに会えてよかったケビン", resp.msg);
    pikachu.releaseService();
  }

  public void testLearn() throws IOException {
    // Response resp1 = testService.getResponse(session, "SET FOO BAR");
    // System.out.println(resp1.msg);
    Response resp = testService.getResponse(username, "LEARN AAA IS BBB");
    // System.out.println(resp.msg);
    resp = testService.getResponse(username, "WHAT IS AAA");
    assertEquals("BBB", resp.msg);
  }

  @Test
  public void testMultiSession() throws IOException {
    ProgramAB lloyd = (ProgramAB)Runtime.start("lloyd", "ProgramAB");
    lloyd.setPath(path);
    // pikachu the service.
    lloyd.startService();
    // load the bot brain for the chat with the user
    lloyd.startSession(path, "user1", "lloyd");
    Response res = lloyd.getResponse("My name is Kevin");
    System.out.println(res);
    lloyd.startSession(path, "user2", "lloyd");
    res = lloyd.getResponse("My name is Grog");
    System.out.println(res);
    lloyd.startSession(path, "user1", "lloyd");
    Response respA = lloyd.getResponse("What is my name?");
    System.out.println(respA);
    lloyd.startSession(path, "user2", "lloyd");
    Response respB = lloyd.getResponse("What is my name?");
    System.out.println(respB);

    assertEquals("Kevin", respA.msg);
    assertEquals("Grog", respB.msg);

    // release this service.
    lloyd.releaseService();

  }

  public void testOOBTags() throws Exception {
    Response resp = testService.getResponse(username, "OOB TEST");
    assertEquals("OOB Tag Test", resp.msg);

    // TODO figure a mock object that can wait on a callback to let us know the
    // python service is started.
    // wait up to 5 seconds for python service to start
    long maxWait = 6000;
    int i = 0;
    Runtime.start("python", "Python");
    while (Runtime.getService("python") == null) {
      Thread.sleep(100);
      log.info("Waiting for python to start...");
      i++;
      if (i > maxWait) {
        Assert.assertFalse("Took too long to process OOB tag", i > maxWait);
      }
    }
    Assert.assertNotNull(Runtime.getService("python"));

  }

  public void testPredicates() {
    // test removing the predicate if it exists
    testService.setPredicate(username, "name", "foo1");
    String name = testService.getPredicate(username, "name");
    // validate it's set properly
    assertEquals("foo1", name);
    testService.removePredicate(username, "name");
    // validate the predicate doesn't exist
    name = testService.getPredicate(username, "name");
    // TODO: is this valid? one would expect it would return null.
    assertEquals("unknown", name);
    // set a predicate
    testService.setPredicate(username, "name", "foo2");
    name = testService.getPredicate(username, "name");
    // validate it's set properly
    assertEquals("foo2", name);
  }

  public void testProgramAB() throws Exception {
    // a response
    Response resp = testService.getResponse(username, "UNIT TEST PATTERN");
    // System.out.println(resp.msg);
    assertEquals("Unit Test Pattern Passed", resp.msg);
  }

  public void testSavePredicates() throws IOException {
    long uniqueVal = System.currentTimeMillis();
    String testValue = String.valueOf(uniqueVal);
    Response resp = testService.getResponse(username, "SET FOO " + testValue);
    assertEquals(testValue, resp.msg);
    testService.savePredicates();
    testService.reloadSession(username, botname);
    resp = testService.getResponse(username, "GET FOO");
    assertEquals("FOO IS " + testValue, resp.msg);
  }

  @Override
  public void testService() throws Exception {
    // run each of the test methods.
    testProgramAB();
    testOOBTags();
    testSavePredicates();
    testPredicates();
    testLearn();
    testSets();
    testSetsAndMaps();
    testAddEntryToSetAndMaps();
    testTopicCategories();
    umlautTest();
    listPatternsTest();
    // This following test is known to be busted..
    // pannousTest();
    addCategoryTest();
    sraixOOBTest();
    // sraixTest();  sraix doesnt appear to work - wikipedia interface is borked on pannous bots
  }

  public void testSets() throws IOException {
    Response resp = testService.getResponse(username, "SETTEST CAT");
    assertEquals("An Animal.", resp.msg);
    resp = testService.getResponse(username, "SETTEST MOUSE");
    assertEquals("An Animal.", resp.msg);
    resp = testService.getResponse(username, "SETTEST DOG");
    // System.out.println(resp.msg);
    assertEquals("An Animal.", resp.msg);
  }

  public void testSetsAndMaps() throws IOException {
    Response resp = testService.getResponse(username, "DO YOU LIKE Leah?");
    assertEquals("Princess Leia Organa is awesome.", resp.msg);
    resp = testService.getResponse(username, "DO YOU LIKE Princess Leah?");
    assertEquals("Princess Leia Organa is awesome.", resp.msg);
  }

  public void testTopicCategories() throws IOException {
    // Top level definition
    Response resp = testService.getResponse(username, "TESTTOPICTEST");
    assertEquals("TOPIC IS unknown", resp.msg);
    resp = testService.getResponse(username, "SET TOPIC TEST");
    resp = testService.getResponse(username, "TESTTOPICTEST");
    assertEquals("TEST TOPIC RESPONSE", resp.msg);
    // maybe we can still fallback to non-topic responses.
    resp = testService.getResponse(username, "HI");
    assertEquals("Hello user!", resp.msg);
    // TODO: how the heck do we unset a predicate from AIML?
    testService.unsetPredicate(username, "topic");
    resp = testService.getResponse(username, "TESTTOPICTEST");
    assertEquals("TOPIC IS unknown", resp.msg);
  }

  public void umlautTest() throws IOException {
    Response resp = testService.getResponse(username, "Lars Ümlaüt");
    // @GroG says - "this is not working"
    assertEquals("He's a character from Guitar Hero!", resp.msg);
  }

}