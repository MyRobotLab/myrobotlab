package org.myrobotlab.logging;

import org.junit.Test;
import org.slf4j.Logger;
import org.junit.Assert;

public class LoggingTest {

	@Test
	public void testLogging() {
		Logger log = LoggerFactory.getLogger(LoggingTest.class);
		
		String originalLogLevel = LoggingFactory.getLogLevel();
		log.info("original log level is {}", originalLogLevel);
		LoggingFactory.init();
		originalLogLevel = LoggingFactory.getLogLevel();
		log.info("log level afer init {}", originalLogLevel);
		Assert.assertEquals("DEBUG", LoggingFactory.getLogLevel());
		LoggingFactory.setLevel("warn");
		log.warn("set to warn level");
		log.info("this should not be logged");
		log.debug("nor this");
		log.info("testing info log level");
		Assert.assertEquals("WARN", LoggingFactory.getLogLevel());
		
	}

}
