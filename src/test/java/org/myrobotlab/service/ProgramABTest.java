package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.Assert;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.ProgramAB.Response;
import org.slf4j.Logger;

public class ProgramABTest extends AbstractServiceTest {

  private ProgramAB testService;
  private String username = "testUser";
  private String botname = "lloyd";
  // TODO: move this to test resources
  private String path = "src/test/resources/ProgramAB";

  public final static Logger log = LoggerFactory.getLogger(ProgramABTest.class);

  public Service createService() {

    log.info("Setting up the Program AB Service ########################################");
    // Load the service under test
    // a test robot
    // TODO: this should probably be created by Runtime,
    // OOB tags might not know what the service name is ?!
    testService = new ProgramAB(botname);
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
    return testService;
  }

  public void testProgramAB() throws Exception {
    // a response
    Response resp = testService.getResponse(username, "UNIT TEST PATTERN");
    // System.out.println(resp.msg);
    assertEquals("Unit Test Pattern Passed", resp.msg);
  }

  public void testOOBTags() throws Exception {
    Response resp = testService.getResponse(username, "OOB TEST");
    assertEquals("OOB Tag Test", resp.msg);
    // Thread.sleep(1000);
    Assert.assertNotNull(Runtime.getService("python"));

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

  public void testLearn() throws IOException {
    // Response resp1 = testService.getResponse(session, "SET FOO BAR");
    // System.out.println(resp1.msg);
    Response resp = testService.getResponse(username, "LEARN AAA IS BBB");
    // System.out.println(resp.msg);
    resp = testService.getResponse(username, "WHAT IS AAA");
    assertEquals("BBB", resp.msg);
  }

  public void testSets() {
    Response resp = testService.getResponse(username, "SETTEST CAT");
    assertEquals("An Animal.", resp.msg);
    resp = testService.getResponse(username, "SETTEST MOUSE");
    assertEquals("An Animal.", resp.msg);
    resp = testService.getResponse(username, "SETTEST DOG");
    // System.out.println(resp.msg);
    assertEquals("An Animal.", resp.msg);
  }

  public void testSetsAndMaps() {
    Response resp = testService.getResponse(username, "DO YOU LIKE Leah?");
    assertEquals("Princess Leia Organa is awesome.", resp.msg);
    resp = testService.getResponse(username, "DO YOU LIKE Princess Leah?");
    assertEquals("Princess Leia Organa is awesome.", resp.msg);
  }

  public void testAddEntryToSetAndMaps() {
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

  public void testTopicCategories() {
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

  public void umlautTest() {
    Response resp = testService.getResponse(username, "Lars Ümlaüt");
    // @GroG says - "this is not working"
    assertEquals("He's a character from Guitar Hero!", resp.msg);
  }

  public void listPatternsTest() {
    ArrayList<String> res = testService.listPatterns(botname);
    assertTrue(res.size() > 0);
  }

  public void addCategoryTest() {
    testService.addCategory("BOOG", "HOWDY");
    Response resp = testService.getResponse(username, "BOOG");
    assertTrue(resp.msg.equals("HOWDY"));
  }

  // the pannous service seems borked at the moment due to bad ssl, and maybe
  // other stuff.. kwatters: I recommend we build our own service that does this
  // stuff
  // @Test
  public void pannousTest() {
    Response resp = testService.getResponse(username, "SHOW ME INMOOV");
    // System.out.println(resp);
    boolean contains = resp.msg.contains("http");
    assertTrue(contains);
  }

  public void sraixTest() {
    Response resp = testService.getResponse(username, "MRLSRAIX");
    // System.out.println(resp);
    boolean contains = resp.msg.contains("foobar");
    assertTrue(contains);
    resp = testService.getResponse(username, "OOBMRLSRAIX");
    // System.out.println(resp);
    contains = resp.msg.contains("You are talking to lloyd");
    assertTrue(contains);
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
    sraixTest();
  }

}
