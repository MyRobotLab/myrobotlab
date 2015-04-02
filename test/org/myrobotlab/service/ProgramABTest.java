package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.myrobotlab.service.ProgramAB.Response;

public class ProgramABTest {

	@Test
	public void testProgramAB() throws Exception {

		String botName = "lloyd";
		String session = "testUser";
		String path = "test/ProgramAB";
		ProgramAB testService = new ProgramAB("lloyd");
		testService.startService();
		testService.startSession(path, session, botName);
		Response resp = testService.getResponse(session, "time test");

		Thread.sleep(1000);

		resp = testService.getResponse(session, "BORING TIME");
		System.out.println(resp.msg);
		// assertEquals("My Default Response 3.", resp.msg);

	}
}
