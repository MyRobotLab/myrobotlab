package org.myrobotlab.framework;
import org.junit.Ignore;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.config.ClockConfig;
import org.slf4j.Logger;

import picocli.CommandLine;
@Ignore
public class CmdOptionsTest {

  public final static Logger log = LoggerFactory.getLogger(CmdOptionsTest.class);

  static boolean contains(List<String> l, String flag) {
    for (String f : l) {
      if (f.equals(flag)) {
        return true;
      }
    }
    return false;
  }

  @Test
  public void testGetOutputCmd() throws IOException {

    CmdOptions options = new CmdOptions();
    new CommandLine(options).parseArgs(new String[] {});
    // validate defaults
    assertNull(options.config);
    assertEquals(0, options.services.size());

    new CommandLine(options).parseArgs(new String[] {  "-s", "webgui", "WebGui", "clock01", "Clock" });

    assertEquals(4, options.services.size());

    List<String> cmd = options.getOutputCmd();
    assertTrue(contains(cmd, "webgui"));
    assertTrue(contains(cmd, "clock01"));

    log.info(CmdOptions.toString(cmd));

    Runtime.releaseAll(true, true);
    // test help
    Runtime.main(new String[] { "--id", "test", "-s", "clockCmdTest", "Clock" });
    assertNotNull(Runtime.getService("clockCmdTest"));
    assertEquals("test", Runtime.getInstance().getId());

    Runtime.releaseAll(true, true);
    
    Runtime.main(new String[] { "-c", "xxx", "-s", "clockCmdTest", "Clock" });
    
    ClockConfig clock = (ClockConfig)Runtime.getInstance().readServiceConfig("xxx", "clockCmdTest");
    assertNotNull(clock);
    assertNotNull(Runtime.getService("clockCmdTest"));
    
    Runtime.releaseAll(true, true);
    
    log.info("here");

  }

}
