package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.net.Connection;
import org.myrobotlab.process.InProcessCli;
import org.myrobotlab.test.AbstractTest;

public class InProcessCliTest extends AbstractTest {

  static PipedOutputStream pipe = null;
  static PipedInputStream in = null;
  static ByteArrayOutputStream bos = null;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    // set config to this tests name - so will not conflict with other
    // tests
    Runtime.setConfig("InProcessCliTest");
    pipe = new PipedOutputStream();
    in = new PipedInputStream(pipe);
    bos = new ByteArrayOutputStream();
  }

  public void write(String str) throws IOException {
    pipe.write((str + "\n").getBytes());
    pipe.flush();
    // must read it off and process the data
    sleep(50);
  }

  public void clear() {
    bos.reset();
  }

  public String getResponse() {
    String ret = new String(bos.toByteArray());
    log.info("cd => {}", ret);
    // clear();
    return ret;
  }

  public String toJson(Object o) {
    return CodecUtils.toPrettyJson(o);
  }

  @Test
  public void testProcess() throws IOException, InterruptedException {
    try {
      Runtime runtime = Runtime.getInstance();
      // runtime.stopInteractiveMode();
      runtime.stopInteractiveMode();

      InProcessCli proc = new InProcessCli(runtime, "proc-cli-testz", in, bos);

      // FIXME - adding route should be automagic

      // add the route !
      Connection c = proc.getConnection();
      String stdCliUuid = (String) c.get("uuid");

      // addRoute(".*", getName(), 100);
      runtime.addConnection(stdCliUuid, proc.getId(), c);

      // wait for pipe to clear
      Thread.sleep(300);
      clear();
      write("pwd");
      Thread.sleep(300);
      String ret = getResponse();
      log.warn("pwd expected {} got {}", "/", ret);
      assertTrue("expecting to start with \"/\"", ret.startsWith("\"/\""));

      clear();
      write("ls");
      Thread.sleep(300);
      String response = getResponse();
      log.warn("ls response is {}", response);
      String services = toJson(Runtime.getServiceNames());
      log.warn("ls expected {} got {}", services, response);
      assertTrue(String.format("expecting a list of service names expecting %s got %s", services, response), response.contains(services));

      boolean virtual = runtime.isVirtual();

      // boolean conversion
      clear();
      write("/runtime/setVirtual/false");
      Thread.sleep(300);
      ret = getResponse();
      assertFalse("virtual better be false", runtime.isVirtual());
      write("/runtime/setVirtual/true");
      Thread.sleep(300);
      assertTrue("this better be virtual", runtime.isVirtual());
      // replace with original value
      runtime.setVirtual(virtual);

      // integer conversion
      Clock clockCli = (Clock) Runtime.start("clockCli", "Clock");
      write("/clockCli/setInterval/1234");
      Thread.sleep(300);
      Integer check = 1234;
      log.warn("/clockCli/setInterval/ expected 1234 got {}", ret);
      assertEquals(check, clockCli.getInterval());
      proc.stop();
    } catch (Exception e) {
      log.error("InProcessCliTest threw", e);
    }
  }

}
