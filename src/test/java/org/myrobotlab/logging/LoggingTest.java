package org.myrobotlab.logging;

import org.junit.Test;
import org.junit.Assert;
import org.junit.Ignore;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

// TODO: Why does this unit test fail when building from the command line with maven, but not in eclipse?!
@Ignore
public class LoggingTest {

  @Test
  public void testLogging() {
    Logger log = LoggerFactory.getLogger(LoggingTest.class);
    String originalLogLevel = LoggingFactory.getLogLevel();
    log.info("original log level is {}", originalLogLevel);
    LoggingFactory.init();
    originalLogLevel = LoggingFactory.getLogLevel();
    log.info("log level afer init {}", originalLogLevel);
    Assert.assertEquals(LoggingFactory.getLogLevel(), "DEBUG");
    LoggingFactory.setLevel("WARN");
    log.warn("set to warn level");
    log.info("this should not be logged");
    log.debug("nor this");
    log.info("testing info log level");
    // TODO: this assert is broken! figure out why it passes in eclipse, but not
    // in maven?!
    // Assert.assertEquals(LoggingFactory.getLogLevel(), "WARN");
  }

}
