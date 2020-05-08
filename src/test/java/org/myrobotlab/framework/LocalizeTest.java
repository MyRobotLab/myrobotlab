package org.myrobotlab.framework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.InMoov2;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class LocalizeTest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(TaskTest.class);
  
  static Runtime runtime = null;
  static InMoov2 i01 = null;
    
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    runtime = Runtime.getInstance();
    i01 = (InMoov2)Runtime.start("i01", "InMoov2");
  }

  @AfterClass
  public static void lastCleanup() {
    Runtime.releaseService("i01");
  }


  @Test
  public void caseInsensitive() {
    // case insensitive
    String SHUTDOWN = runtime.localize("shutdown");
    assertEquals("the system is shutting down", SHUTDOWN);
  }
  
  @Test
  public void basicTest() {
        
    // runtime en - default
    runtime.setLocale("en");
    String SHUTDOWN = runtime.localize("SHUTDOWN");
    assertEquals("the system is shutting down", SHUTDOWN);
    
    String SERVICECOUNT = runtime.localize("SERVICECOUNT", 7);
    assertEquals("there are currently 7 services running", SERVICECOUNT);
    
    
    log.info("here");

  }
  
  @Test
  public void switchingLocalesTest() {
    // start with english
    runtime.setLocale("en");
    
    // runtime fr
    runtime.setLocale("fr");
    String SHUTDOWN = runtime.localize("SHUTDOWN");
    assertEquals("extinction de mon systaime", SHUTDOWN);
    
    // query non existent key
    String BLAH = runtime.localize("BLAH");
    assertNull(BLAH);
    
    
    String STARTINGLEFTONLY = i01.localize("STARTINGLEFTONLY");
    assertEquals("arduino coté gauche uniquement sélectionné", STARTINGLEFTONLY);
    runtime.setLocale("it");
    STARTINGLEFTONLY = i01.localize("STARTINGLEFTONLY");
    assertEquals("Soltanto la configurazione della parte sinistra  del robot è stata abilitata", STARTINGLEFTONLY);
    BLAH = i01.localize("BLAH");
    assertNull(BLAH);
    
    // test to see if runtime en is default when all else fails
    STARTINGLEFTONLY = i01.localize("RELEASESERVICE");
    assertEquals("releasing service", STARTINGLEFTONLY);

  }

}