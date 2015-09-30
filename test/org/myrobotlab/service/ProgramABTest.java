package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.myrobotlab.service.ProgramAB.Response;

public class ProgramABTest {

	@Test
	public void testProgramAB() throws Exception {

		/* DUNNO WHY IT FAILS IN BUILD ... */
		String botName = "lloyd";
		String session = "testUser";
		String path = "test/ProgramAB";
		ProgramAB testService = new ProgramAB("lloyd");
		testService.startService();
		testService.startSession(path, session, botName);
		Response resp = testService.getResponse(session, "UNIT TEST PATTERN");
		// System.out.println(resp.msg);
		assertEquals("Unit Test Pattern Passed", resp.msg);
	}
}
