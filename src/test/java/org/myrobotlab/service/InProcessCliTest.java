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

    Runtime runtime = Runtime.getInstance();
    runtime.startInteractiveMode();
    runtime.stopInteractiveMode();
    Thread.sleep(300);
    runtime.startInteractiveMode(in, bos);

    // wait for pipe to clear
    Thread.sleep(300);
    clear();
    write("pwd");
    String ret = getResponse();
    Thread.sleep(200);
    assertTrue(ret.startsWith("\"/\""));

    clear();
    write("ls");
    Thread.sleep(200);
    assertTrue(getResponse().contains(toJson(Runtime.getServiceNames())));

    boolean virtual = runtime.isVirtual();

    // boolean conversion
    clear();
    write("/runtime/setVirtual/false");
    ret = getResponse();
    Thread.sleep(200);
    assertFalse(runtime.isVirtual());
    write("/runtime/setVirtual/true");
    Thread.sleep(200);
    assertTrue(runtime.isVirtual());
    // replace with original value
    runtime.setVirtual(virtual);

    // integer conversion
    Clock clockCli = (Clock) Runtime.start("clockCli", "Clock");
    write("/clockCli/setInterval/1234");
    Integer check = 1234;
    Thread.sleep(200);
    assertEquals(check, clockCli.getInterval());

  }

}
