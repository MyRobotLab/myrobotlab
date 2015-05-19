package org.myrobotlab.framework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;

import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;

public class EncoderTest {
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);
	}


	@Test
	public void testEncoderDecodeURI() throws Exception {
		Encoder e = new Encoder();

		// create and start a service named foo, decode a url for that service.
		org.myrobotlab.service.TestCatcher testService = new org.myrobotlab.service.TestCatcher("foo");
		testService.startService();
		// NOT VALID API - can not be typed to an objects method signature
		// Message m = e.decodeURI(new URI("http://www.myrobotlab.org/api/foo/bar/a/b/d?baz=bam&bap=bop"));
		
		Message m = Encoder.decodeURI(new URI("http://www.myrobotlab.org/api/foo/getCategories"));
		assertNotNull(m);
		assertEquals("foo", m.getName());
		

	}

}
