package org.myrobotlab.framework;

import static org.junit.Assert.*;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

public class EncoderTest {

	@Test
	public void testEncoderDecodeURI() {
		Encoder e = new Encoder();
		try {
			Message m = e.decodeURI(new URI("http://www.myrobotlab.org/api/foo/bar/a/b/d?baz=bam&bap=bop"));
			assertNotNull(m);
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
}
