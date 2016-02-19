package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import java.io.File;
import java.io.IOException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.ProgramAB.Response;
import org.slf4j.Logger;

public class ProgramABTest {
	
	private ProgramAB testService;
	private String session = "testUser";
	private String botName = "lloyd";
	private String path = "test/ProgramAB";

	public final static Logger log = LoggerFactory.getLogger(ProgramABTest.class);
	
	@Before
	public void setUp() throws Exception {
		// Load the service under test
		// a test robot
		// TODO: this should probably be created by Runtime,
		// OOB tags might not know what the service name is ?!
		testService = new ProgramAB(botName);
		testService.setPath(path);
		
		// start the service.
		testService.startService();
		// load the bot brain for the chat with the user
		testService.startSession(session, botName);
		// clean out any aimlif the bot that might
		// have been saved in a previous test run!
		String aimlIFPath = path + "/bots/"+botName+"/aimlif";
		File aimlIFPathF = new File(aimlIFPath);
		if (aimlIFPathF.isDirectory()) {
			for (File f : aimlIFPathF.listFiles()) {
				// if there's a file here.
				log.info("Deleting pre-existing AIMLIF files : {}", f.getAbsolutePath());
				f.delete();
				
			}
		}
		// TODO: same thing for predicates! (or other artifacts from a previous aiml test run)
		
		

	}

	@Test
	public void testProgramAB() throws Exception {
		// a response
		Response resp = testService.getResponse(session, "UNIT TEST PATTERN");
		// System.out.println(resp.msg);
		assertEquals("Unit Test Pattern Passed", resp.msg);
	}
	
	@Test 
	public void testOOBTags() throws Exception {
		Response resp = testService.getResponse(session, "OOB TEST");
		assertEquals("OOB Tag Test", resp.msg);		
		// Thread.sleep(1000);
		Assert.assertNotNull(Runtime.getService("python"));

	}
	
	@Test
	public void testSavePredicates() throws IOException {
		long uniqueVal = System.currentTimeMillis();
		String testValue = String.valueOf(uniqueVal);
		Response resp = testService.getResponse(session, "SET FOO " + testValue);
		assertEquals(testValue, resp.msg);		
		testService.savePredicates();
		testService.reloadSession(session, botName);
		resp = testService.getResponse(session, "GET FOO");
		assertEquals("FOO IS " + testValue, resp.msg);	
		
	}
	
	@Test
	public void testPredicates() {
		// test removing the predicate if it exists
		testService.setPredicate(session, "name", "foo1");
		String name = testService.getPredicate(session, "name");
		// validate it's set properly
		assertEquals("foo1", name);
		testService.removePredicate(session, "name");
		// validate the predicate doesn't exist
		name = testService.getPredicate(session, "name");
		// TODO: is this valid?  one would expect it would return null.
		assertEquals("unknown",name);
		// set a predicate
		testService.setPredicate(session, "name", "foo2");
		name = testService.getPredicate(session, "name");
		// validate it's set properly
		assertEquals("foo2", name);
	}
	
	@Test
	public void testLearn() throws IOException {
		//Response resp1 = testService.getResponse(session, "SET FOO BAR");
		//System.out.println(resp1.msg);
		Response resp = testService.getResponse(session, "LEARN AAA IS BBB");
		System.out.println(resp.msg);
		resp = testService.getResponse(session, "WHAT IS AAA");
		assertEquals("BBB", resp.msg);		
	}
	
	@Test
	public void testSets() {
		Response resp = testService.getResponse(session, "SETTEST CAT");
		assertEquals("An Animal.", resp.msg);
		resp = testService.getResponse(session, "SETTEST MOUSE");
		assertEquals("An Animal.", resp.msg);
		resp = testService.getResponse(session, "SETTEST DOG");
		System.out.println(resp.msg);
		assertEquals("An Animal.", resp.msg);
	}
	
	@Test
	public void testSetsAndMaps() {
		Response resp = testService.getResponse(session, "DO YOU LIKE Leah?");
		assertEquals("Princess Leia Organa is awesome.", resp.msg);
		resp = testService.getResponse(session, "DO YOU LIKE Princess Leah?");
		assertEquals("Princess Leia Organa is awesome.", resp.msg);
	}
	
	@Test
	public void testAddEntryToSetAndMaps() {
		// TODO: This does NOT work yet!
		Response resp = testService.getResponse(session, "Add Jabba to the starwarsnames set");
		assertEquals("Ok...", resp.msg);
		resp = testService.getResponse(session, "Add jabba equals Jabba the Hut to the starwars map");
		assertEquals("Ok...", resp.msg);
		resp = testService.getResponse(session, "DO YOU LIKE Jabba?");
		assertEquals("Jabba the Hut is awesome.", resp.msg);
		
		
		// TODO : re-enable this one?
		// now test creating a new set.
		//resp = testService.getResponse(session, "Add bourbon to the whiskey set");
		// assertEquals("Ok...", resp.msg);
		//resp = testService.getResponse(session, "NEWSETTEST bourbon");
		//assertEquals("bourbon is a whiskey", resp.msg);
		
		
	}
	
	@After
	public void tearDown() throws Exception {
		testService.stopService();
		testService.releaseService();
	} 
	
}
