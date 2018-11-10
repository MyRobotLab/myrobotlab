package org.myrobotlab.service;

abstract public class AbstractTest {
  
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
    return Runtime.hasInternet();
  }
  
}
