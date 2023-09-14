package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Message;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class RuntimeProcessTest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(RuntimeProcessTest.class);

  @Before
  public void setUp() {
    // LoggingFactory.init("WARN");
  }

  public boolean contains(ByteArrayOutputStream out, String str) {
    return new String(out.toByteArray()).contains(str);
  }

  @Test
  public void cliTest() throws Exception {

    // from ,to null=runtime, data  
    String cwd = null;
    Message msg = CodecUtils.pathToMsg(getName(), "ls");
    assertEquals("runtime", msg.getName());
    assertEquals("ls", msg.method);
    assertEquals(getName(), msg.getSrcName());

    msg = CodecUtils.pathToMsg(getName() + "@someWhere", "ls");
    assertEquals(getName(), msg.getSrcName());
    assertEquals("someWhere", msg.getSrcId());
    assertEquals(getName() + "@someWhere", msg.getSrcFullName());

    // FIXME !!!!
    // Message msg = CodecUtils.cliToMsg(null, getName(), null, "/ls /runtime");
    // FAILS

    msg = CodecUtils.pathToMsg(getName() + "@someWhere", "ls");
    assertEquals("runtime", msg.getName());
    assertEquals("runtime", msg.getFullName());
    assertEquals(getName() + "@someWhere", msg.sender);
    assertNull(msg.getId());
    assertEquals(0, msg.data.length);

    cwd = "/runtime/";
    msg = CodecUtils.pathToMsg(getName(), cwd + "ls");
    assertEquals("runtime", msg.getName());
    assertEquals("ls", msg.method);
    assertEquals(getName(), msg.getSrcName());
    assertNull(msg.data);

    cwd = "/runtime/blahmethod";
    msg = CodecUtils.pathToMsg(getName(), cwd);
    assertEquals("runtime", msg.getName());
    assertEquals("blahmethod", msg.method);
    assertEquals(getName(), msg.getSrcName());

    // make sure runtime is running
    Runtime runtime = Runtime.getInstance();
    // remove all except runtime
    Runtime.releaseAll(false, true);
    String[] services = Runtime.getServiceNames();

    assertTrue(String.format("releasedAll(false) should have 1 remaining runtime services are %s", Arrays.toString(services)), services.length == 1);
    
    // releasing "self" test
    runtime.releaseService();
    services = Runtime.getServiceNames();
    assertTrue(String.format("releasedAll(false) should have 0 remaining runtime services are %s", Arrays.toString(services)), services.length == 0);
    
    // testing re-entrant -
    runtime = Runtime.getInstance();
    assertTrue("testing re-entrant - expecting runtime service", Arrays.toString(Runtime.getServiceNames()).contains("runtime"));
    
    // removing all 
    Runtime.releaseAll();
    sleep(100);

    // better be 0
    services = Runtime.getServiceNames();
    assertTrue(String.format("releasedAll(false) should have 0 remaining runtime services are %s", Arrays.toString(services)), services.length == 0);

    // better be re-entrant
    runtime = Runtime.getInstance();
    services = Runtime.getServiceNames();
    assertTrue(String.format("releasedAll(false) should have new runtime services are %s", Arrays.toString(services)), services.length > 0);
    assertTrue("testing re-entrant again - expecting runtime service", Arrays.toString(Runtime.getServiceNames()).contains("runtime"));
    

    /**
     * cli
     * 
     * cli - / context cwd / from / to / data cli - / context cwd / from / to /
     * data / .. .params cli - / context cwd / from / to / ?q={json} ?? complex
     * param
     */

  }

}