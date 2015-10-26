package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.service.ProgramAB.Response;

public class ProgramABTest {
	
	private ProgramAB testService;
	private String session = "testUser";
	private String botName = "lloyd";
	private String path = "test/ProgramAB";

	
	@Before
	public void setUp() throws Exception {
		// Load the service under test
		// a test robot
		testService = new ProgramAB("lloyd");
		// start the service.
		testService.startService();
		// load the bot brain for the chat with the user
		testService.startSession(path, session, botName);
		
		// Thread.sleep(120000);

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
		testService.reloadSession(path, session, botName);
		resp = testService.getResponse(session, "GET FOO");
		assertEquals("FOO IS " + testValue, resp.msg);		
	}
	
	@After
	public void tearDown() throws Exception {
		testService.stopService();
		testService.releaseService();
	} 
	
}
