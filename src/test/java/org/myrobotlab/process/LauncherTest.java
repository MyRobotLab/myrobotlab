package org.myrobotlab.process;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.framework.CmdOptions;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

import picocli.CommandLine;

@Ignore /* testing if the is the root of ci problem */
public class LauncherTest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(LauncherTest.class);

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  @AfterClass
  public static void lastCleanup() {
  }

  private boolean invalidArgs = false;

  static public String toString(String[] args) {
    if (args == null) {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    for (String str : args) {
      sb.append(str);
      sb.append(" ");
    }
    return sb.toString();
  }

  @Test
  public void test() throws IllegalArgumentException, IllegalAccessException, IOException, URISyntaxException, InterruptedException, ParseException {

    Launcher.main(new String[] {"-s", "runtime", "Runtime" });

    String help = Launcher.mainHelp();

    assertTrue(help.contains("-s") && help.contains("--service") && help.contains("-i"));

    // TODO - spawning a classpath (newly built source) Launcher (build src
    // spawn)
    ProcessBuilder pb = null;
    Process p = null;

    // default
    try {
      pb = Launcher.createBuilder(new CmdOptions());
    } catch (Exception e) {
      log.info("no valid myrobotlab.jar");
    }

    assertNotNull(pb);

    p = pb.start();
    assertTrue(p.isAlive());
    p.destroy();

    // FIXME validate default
    try {
      CmdOptions options = new CmdOptions();
      new CommandLine(options).parseArgs(new String[] { "-s x" });
      Launcher.createBuilder(options);
    } catch (Exception e) {
      invalidArgs = true;
    }

    assertTrue(invalidArgs);

  }

}