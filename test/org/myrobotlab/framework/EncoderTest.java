package org.myrobotlab.framework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;

import org.junit.Test;

public class EncoderTest {

	@Test
	public void testEncoderDecodeURI() throws Exception {
		Encoder e = new Encoder();

		// create and start a service named foo, decode a url for that service.
		org.myrobotlab.service.Test testService = new org.myrobotlab.service.Test("foo");
		testService.startService();
		Message m = e.decodeURI(new URI("http://www.myrobotlab.org/api/foo/bar/a/b/d?baz=bam&bap=bop"));
		assertNotNull(m);
		assertEquals("foo", m.getName());

	}

}
