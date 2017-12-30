package org.myrobotlab.service;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class JFugueTest {
  public final static Logger log = LoggerFactory.getLogger(JFugueTest.class);
  static JFugue jfugue;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    LoggingFactory.init(Level.DEBUG);

    jfugue = (JFugue) Runtime.start("jfugue", "JFugue");
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    jfugue.releaseService();
  }

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testGetCategories() {
    // fail("Not yet implemented");
  }

  @Test
  public final void testGetDescription() {
    // fail("Not yet implemented");
  }

  @Test
  public final void testStopService() {
    // fail("Not yet implemented");
  }

  @Test
  public final void testMain() {
    // fail("Not yet implemented");
  }

  @Test
  public final void testJFugue() {
    // fail("Not yet implemented");
  }

  @Test
  public final void testPlayInteger() {
    // fail("Not yet implemented");
  }

  @Test
  public final void testPlayRhythm() {
    // fail("Not yet implemented");
  }

  @Test
  public final void testPlayString() {
    // fail("Not yet implemented");
  }

  @Test
  public final void testPlayRythm() {
    // fail("Not yet implemented");
  }

  @Test
  public final void test() {
    // jfugue.playRythm();
    /*
     * FIXME - jfugue noWorky in Ant on Travis-CI jfugue.play("C");
     * jfugue.play("C7h");
     * 
     * jfugue.play("C5maj7w"); jfugue.play("G5h+B5h+C6q_D6q"); jfugue.play(
     * "G5q G5q F5q E5q D5h"); jfugue.play("T[Allegro] V0 I0 G6q A5q V1 A5q G6q"
     * ); jfugue.play("V0 Cmajw V1 I[Flute] G4q E4q C4q E4q"); jfugue.play(
     * "T120 V0 I[Piano] G5q G5q V9 [Hand_Clap]q Rq");
     * 
     * jfugue.play("C3w D6h E3q F#5i Rs Ab7q Bb2i"); jfugue.play(
     * "I[Piano] C5q D5q I[Flute] G5q F5q"); jfugue.play(
     * "V0 A3q B3q C3q B3q V1 A2h C2h"); jfugue.play("Cmaj5q F#min2h Bbmin13^^^"
     * );
     * 
     * jfugue.play(30); jfugue.play(32); jfugue.play(44); jfugue.play(90);
     * jfugue.play("A");
     * 
     * jfugue.playRythm();
     * 
     * jfugue.play("C D E F G A B"); jfugue.play("A A A B B B");
     * jfugue.playRythm(); jfugue.play(30); jfugue.play(31); jfugue.play(40);
     * jfugue.play(55); jfugue.play(
     * "E5s A5s C6s B5s E5s B5s D6s C6i E6i G#5i E6i | A5s E5s A5s C6s B5s E5s B5s D6s C6i A5i Ri"
     * ); jfugue.play(55);
     */

  }

  public static void main(String[] args) {
    try {

      setUpBeforeClass();

    } catch (Exception e) {
      Logging.logError(e);
    }

    System.exit(0);
  }

}
