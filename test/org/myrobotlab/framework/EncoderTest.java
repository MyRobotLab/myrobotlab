package org.myrobotlab.framework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;

import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.codec.CodecUri;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;

public class EncoderTest {

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    LoggingFactory.init(Level.INFO);
  }

  @Test
  public void testEncoderDecodeURI() throws Exception {
    // Encoder e = new Encoder();

    Message msg = CodecUri.decodeURI(new URI("http://www.myrobotlab.org:7777/api/foo/getCategories"));
    assertNotNull(msg);
    assertEquals("foo", msg.getName());
    assertEquals("getCategories", msg.method);

    msg = CodecUri.decodeURI(new URI("http://www.myrobotlab.org:7777/api"));
    assertNotNull(msg);
    assertEquals("help", msg.method);
    assertEquals("", msg.getName()); // FIXME SHOULD BE NULL

    // Runtime.getService(foo) (TYPE)
    msg = CodecUri.decodeURI(new URI("http://www.myrobotlab.org:7777/api/foo"));
    assertNotNull(msg);
    assertEquals("", msg.getName());
    assertEquals("foo", msg.method);

    // Runtime.showMethods(foo) (TYPE)
    /*
     * msg = Encoder.decodeURI(new
     * URI("http://www.myrobotlab.org:7777/api/foo/")); assertNotNull(msg);
     * assertEquals("foo", msg.getName());
     */

  }

}
