package org.myrobotlab.logging;

import org.myrobotlab.framework.Platform;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public class LoggerFactory {

  public static Logger getLogger(Class<?> clazz) {
    return getLogger(clazz.toString());
  }

  public static Logger getLogger(String name) {
    Platform platform = Platform.getLocalInstance();
    if (platform.isDalvik()) {
      String android = name.substring(name.lastIndexOf(".") + 1);
      if (android.length() > 23)
        android = android.substring(0, 23);
      // http://slf4j.42922.n3.nabble.com/
      // Bug-173-New-slf4j-android-Android-throws-an-IllegalArgumentException-when-Log-Tag-length-exceeds-23-s-td443886.html
      return org.slf4j.LoggerFactory.getLogger(android);
    } else {
      return org.slf4j.LoggerFactory.getLogger(name);
    }
  }

  public static ILoggerFactory getILoggerFactory() {
    return org.slf4j.LoggerFactory.getILoggerFactory();
  }

}
