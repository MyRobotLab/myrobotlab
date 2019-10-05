package org.myrobotlab.client;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import org.myrobotlab.service.Runtime;
import org.junit.Test;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.lang.NameGenerator;
import org.myrobotlab.test.AbstractTest;

public class InProcessCliTest extends AbstractTest {
  
  static PipedOutputStream pipe = null;
  static PipedInputStream in = null;
  static ByteArrayOutputStream bos = null;
  
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    pipe = new PipedOutputStream();
    in = new PipedInputStream(pipe);
    bos = new ByteArrayOutputStream();
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }
  
  public void write(String str) throws IOException {
    pipe.write((str + "\n").getBytes());
    // must read it off and process the data
    sleep(300);
  }
  
  public void clear() {
    bos.reset();
  }
  
  public String getResponse() {
    String ret =  new String(bos.toByteArray());
    log.info("cd => {}", ret);
    // clear();
    return ret;
  }
  
  public String toJson(Object o) {
    return CodecUtils.toPrettyJson(o);
  }

  @Test
  public void testProcess() throws IOException {
 
    InProcessCli cli = new InProcessCli(NameGenerator.getName(), "runtime", in, bos);
    cli.start();
    
    clear();
    write("pwd");
    String ret = getResponse();
    assertEquals("/", ret);
    
    clear();
    write("ls");
    assertEquals(toJson(Runtime.getServiceNames()), getResponse());
    
    clear();
    write("route");
    assertEquals(toJson(Runtime.route()), getResponse());
    
    // cd to different directory with and without /
    
    // try a bunch of service commands
    
  }

}
