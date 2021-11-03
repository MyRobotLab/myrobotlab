package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class DiscordBotTest extends AbstractServiceTest {

  public final static Logger log = LoggerFactory.getLogger(DiscordBotTest.class);

  @Override
  public Service createService() throws Exception {
    DiscordBot bot = (DiscordBot)Runtime.start("discord", "DiscordBot");
    return bot;
  }

  @Override
  public void testService() throws Exception {

    DiscordBot bot = (DiscordBot)service;
    bot.setBotName("Awesom-O-4000");
    bot.setToken("BOGUS_FAKE_TOKEN");
    
    assertEquals("Awesom-O-4000", bot.getBotName());
    assertEquals("BOGUS_FAKE_TOKEN", bot.getToken());
    
    // TODO: mock out the ProgramAB "brain" 
    // TODO: mock out the actual Discord server connection.
    
  }

}
