package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RuntimeTest {

	@Test
	public void testRuntime() throws Exception {
		System.out.println("This is a junit test... woot!");
		Runtime testService = new Runtime("testruntime");
		// try to start the service
		testService.startService();
		
		// make sure the service knows it's name...
		assertEquals( "testruntime" , testService.getIntanceName());
		
		// try to stop the service.
		testService.stopService();
		// we assume we get here. if not runtime didn't start...
	}
}
