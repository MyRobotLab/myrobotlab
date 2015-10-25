package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.service.ProgramAB.Response;

public class ProgramABTest {
	
	private ProgramAB testService;
	private String session;
	
	@Before
	public void setUp() throws Exception {
		// Load the service under test
		// a test robot
		String botName = "lloyd";
		// the username that is going to chat with the bot
		session = "testUser";
		// directory to the "bots" aiml folders are kept.
		String path = "test/ProgramAB";
		testService = new ProgramAB("lloyd");
		// start the service.
		testService.startService();
		// load the bot brain for the chat with the user
		testService.startSession(path, session, botName);

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
	
	@After
	public void tearDown() throws Exception {
		testService.stopService();
		testService.releaseService();
	} 
	
}
