package org.myrobotlab.service;

abstract public class AbstractTest {
  
  /**
   * cached internet test value for tests
   */
  static Boolean hasInternet = null;
  
  static public void sleep(int sleepMs) {
    try {
      Thread.sleep(sleepMs);
    } catch (InterruptedException e) {
      // don't care
    }
  }
  static public boolean isHeadless() {
    return Runtime.isHeadless();
  }
  
  static public boolean hasInternet() {    
    if (hasInternet == null) {
      hasInternet = Runtime.hasInternet();
    }
    return hasInternet;
  }
  
}
