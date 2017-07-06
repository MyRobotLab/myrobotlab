package org.myrobotlab.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class CliTest {

  public final static Logger log = LoggerFactory.getLogger(CliTest.class);

  static transient Cli cli = null;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    cli = (Cli)Runtime.start("cli", "Cli");
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    Runtime.exit();
  }

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testAttachString() {
    log.error("Not yet implemented");
  }

  @Test
  public void testWritePrompt() {
    log.error("Not yet implemented");
  }

  @Test
  public void testProcess() throws IOException {
    // cli.processStdIn("stdin", null);
    byte[] buffer = new byte[2048];
    
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ByteArrayInputStream bis = new ByteArrayInputStream(buffer);
    
    cli.add("test", bis, bos);
    
    //System.in.
    
  }

  @Test
  public void testAdd() {
    log.error("Not yet implemented");
  }

  @Test
  public void testAttach() {
    log.error("Not yet implemented");
  }

  @Test
  public void testAttachStdIO() {
    log.error("Not yet implemented");
  }

  @Test
  public void testCd() {
    log.error("Not yet implemented");
  }

  @Test
  public void testDetachStdIO() {
    log.error("Not yet implemented");
  }

  @Test
  public void testEcho() {
    log.error("Not yet implemented");
  }

  @Test
  public void testLs() {
    log.error("Not yet implemented");
  }

  @Test
  public void testOutByteArray() {
    log.error("Not yet implemented");
  }

  @Test
  public void testStdout() {
    log.error("Not yet implemented");
  }

  @Test
  public void testOutString() {
    log.error("Not yet implemented");
  }

}
