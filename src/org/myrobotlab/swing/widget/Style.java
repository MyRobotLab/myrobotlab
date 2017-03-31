package org.myrobotlab.swing.widget;

import java.awt.Color;

/**
 * style for swing gui objects
 * 
 * reference - http://www.realdealmarketing.net/docs/css-coding-style.php
 */
public class Style {

  public static Color base = Color.decode("0x" + "00B270"); // green
  public static Color background = Color.decode("0x" + "00B270");
  public static Color foreground = Color.decode("0x" + "222222"); // near
  // black

  public static Color listHighlight = Color.decode("0x" + "00EE22"); // bright
  // green
  public static Color listForeground = Color.decode("0x" + "222222");
  public static Color listBackground = Color.decode("0x" + "FFFFFF");

  public static Color possibleServicesLatest = Color.decode("0x" + "E5FFE5");
  public static Color possibleServicesUpdate = Color.decode("0x" + "FFFFCC");
  public static Color possibleServicesStable = Color.decode("0x" + "E5FFE5");
  public static Color possibleServicesDev = Color.decode("0x" + "E5FFE5");
  public static Color possibleServicesNotInstalled = Color.decode("0x" + "D9D9D9");

  public static Color fatal = Color.decode("0x" + "E5FFE5");
  public static Color error = Color.decode("0x" + "E5FFE5");
  public static Color warning = Color.decode("0x" + "E5FFE5");
  public static Color info = Color.decode("0x" + "E5FFE5");

  public static Color remoteColorTab = Color.decode("0x" + "007000");
  public static Color remoteFont = Color.decode("0x" + "FFFFFF");

  public static Color remoteBase = Color.decode("0x" + "000000");
  public static Color remoteBackground = Color.decode("0x" + "99DD66");
  public static Color remoteHighlight = Color.decode("0x" + "000000");

  private Style INSTANCE = new Style();

  public Style getInstance() {
    return INSTANCE;
  }

}
