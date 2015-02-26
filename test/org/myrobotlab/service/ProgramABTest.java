package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.myrobotlab.service.ProgramAB.Response;

public class ProgramABTest {

	@Test
	public void testProgramAB() {

		String botName = "lloyd";
		String session = "testUser";
		String path = "C:/dev/workspace.kmw/myrobotlab/ProgramAB";
		ProgramAB testService = new ProgramAB("lloyd");
		testService.startService();
		testService.startSession(path, session, botName);
		Response resp = testService.getResponse(session, "Hello");
		System.out.println(resp.msg);
		assertEquals("I have no answer for that.", resp.msg);
		
	}
}
