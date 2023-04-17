package org.myrobotlab.framework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import picocli.CommandLine;

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
    assertEquals(false, options.autoUpdate);
    assertNull(options.config);
    assertNull(options.connect);
    assertEquals(0, options.services.size());

    new CommandLine(options).parseArgs(new String[] { "--id", "raspi", "-s", "webgui", "WebGui", "clock01", "Clock" });

    assertEquals("raspi", options.id);
    assertEquals(4, options.services.size());

    List<String> cmd = options.getOutputCmd();
    assertTrue(contains(cmd, "webgui"));
    assertTrue(contains(cmd, "raspi"));

    log.info(CmdOptions.toString(cmd));

    options = new CmdOptions();
    new CommandLine(options).parseArgs(new String[] { "-a" });
    assertEquals(true, options.autoUpdate);

    // test help

    // test unmatched option

  }

}
