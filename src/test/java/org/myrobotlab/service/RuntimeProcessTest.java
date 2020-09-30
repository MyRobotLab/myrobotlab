package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
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
    
                                      // from  ,to null=runtime,  data
    String cwd = null;
    String to = null;
    Message msg = CodecUtils.cliToMsg(cwd, getName(), null, "ls");
    assertEquals("runtime", msg.getName());
    assertEquals("ls", msg.method);
    assertEquals(getName(), msg.getSrcName());
    
    msg = CodecUtils.cliToMsg(null, getName() + "@someWhere", null, "ls");
    assertEquals(getName(), msg.getSrcName());
    assertEquals("someWhere", msg.getSrcId());
    assertEquals(getName() + "@someWhere", msg.getSrcFullName());

    // FIXME !!!!
    //     Message msg = CodecUtils.cliToMsg(null, getName(), null, "/ls /runtime"); FAILS
    
    msg = CodecUtils.cliToMsg(cwd, getName() + "@someWhere", "blah@far", "ls");
    assertEquals("blah", msg.getName());
    assertEquals("blah@far", msg.getFullName());
    assertEquals("far", msg.getId());
    
    cwd = "/";
    msg = CodecUtils.cliToMsg(cwd, getName(), null, "ls");
    assertEquals("runtime", msg.getName());
    assertEquals("ls", msg.method);
    assertEquals(getName(), msg.getSrcName());

    cwd = "/blah";
    msg = CodecUtils.cliToMsg(cwd, getName(), null, "method");
    assertEquals("runtime", msg.getName());
    assertEquals("ls", msg.method);
    assertEquals(getName(), msg.getSrcName());
    
    /**
     * cli
     * 
     * cli - / context cwd / from  / to /  data
     * cli - / context cwd / from  / to /  data / .. .params
     * cli - / context cwd / from  / to /  ?q={json}  ??  complex param
     */
    
    
    // null context
    // CodecUtils.cliToMsg(contextPath, from, to, cmd);
    
    // cli test
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    buffer.write("ls\n".getBytes());
    ByteArrayInputStream in = new ByteArrayInputStream(buffer.toByteArray());
    Runtime runtime = Runtime.getInstance();
    runtime.startInteractiveMode(in, out);
    
    Service.sleep(100);
    assertTrue(contains(out, "\"runtime\""));
    
    // add cli loopback connection
    // String[] args = new String[]{""};
    // Runtime.main(args);
  }


}