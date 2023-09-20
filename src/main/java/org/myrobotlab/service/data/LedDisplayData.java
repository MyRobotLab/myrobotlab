package org.myrobotlab.service.data;

/**
 * Class to publish to specify details on how to display an led or a group of
 * leds. There is a need to "flash" LEDs in order to signal some event. This is
 * the beginning of an easy way to publish a message to do that.
 * 
 * @author GroG
 *
 */
public class LedDisplayData {

  public String action; // fill | flash | play animation | stop | clear

  public int red;

  public int green;

  public int blue;

  // public int white?;

  /**
   * number of flashes
   */
  public int count = 1;

  /**
   * interval of flash on in ms
   */
  public long timeOn = 500;

  /**
   * interval of flas off in ms
   */
  public long timeOff = 500;

  public LedDisplayData() {
  }

  public LedDisplayData(int red, int green, int blue, int count, int timeOn, int timeOff) {
    this.red = red;
    this.green = green;
    this.blue = blue;
    this.count = count;
    this.timeOn = timeOn;
    this.timeOff = timeOff;
  }

  
  public LedDisplayData(String hexColor, int count, int timeOn, int timeOff) {

    // remove "#" or "0x" prefix if present
    hexColor = hexColor.replace("#", "").replace("0x", "");

    this.count = count;
    this.timeOn = timeOn;
    this.timeOff = timeOff;
    this.red = Integer.parseInt(hexColor.substring(0, 2), 16);
    this.green = Integer.parseInt(hexColor.substring(2, 4), 16);
    this.blue = Integer.parseInt(hexColor.substring(4, 6), 16);

  }

}
