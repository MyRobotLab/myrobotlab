package org.myrobotlab.service;

import java.io.IOException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.test.TestUtils;

@Ignore
public class InmoovScriptTest {

  @Before
  public void beforeTest() {
    TestUtils.initEnvirionment();
  }
  
  @Test
  public void testInMoovScript() throws IOException {
    
    // TODO: fetch/install the InmoovScript dependencies 
    String inmoovScript = "InmoovScript/Inmoov.py";
    // Ok. first thing to do.. we have 2 arduinos.. upload them with the latest
    
    Python python = (Python)Runtime.createAndStart("python", "Python");
    python.execFile(inmoovScript);

    // Now we need to consider some sort of squite of tests that we can perform to make sure the inmoov script is worky.
    // this is a basic smoke test.
    
    // what's our inmoov service name?  i01 ?
    
    System.out.println("Press the any key to exit.");
    System.in.read();
    
  }
}
