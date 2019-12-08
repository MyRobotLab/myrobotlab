package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.client.InProcessCli;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.service.Runtime;
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
  public void testProcess() throws IOException, InterruptedException {
    
    Runtime runtime = Runtime.getInstance();
    // runtime.startInteractiveMode();
 
    InProcessCli cli = new InProcessCli(runtime.getId(), "runtime", in, bos);
    Runtime.stdInClient = cli;
    cli.start();
    
   
    clear();
    write("pwd");
    Thread.sleep(1000);
    String ret = getResponse();    
    assertTrue(ret.startsWith(toJson("/")));
    
    
    clear();
    write("ls");
    Thread.sleep(1000);
    assertTrue(getResponse().contains(toJson(Runtime.getServiceNames())));
    
    
    clear();
    write("route");
    Thread.sleep(1000);
    assertTrue(getResponse().contains(toJson(Runtime.route())));
    
    // cd to different directory with and without /
    
    // try a bunch of service commands
    
  }

}
