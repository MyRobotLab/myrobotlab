package org.myrobotlab.service.data;
/**
 * Class to publish to specify details on how to display an led or a group of leds.
 * There is a need to "flash" LEDs in order to signal some event.  This is the
 * beginning of an easy way to publish a message to do that.
 * 
 * @author GroG
 *
 */
public class LedDisplayData {

    public String action = "flash"; // fill | flash | play animation | stop | clear
    public int red;
    public int green;
    public int blue;
    //public int white?;
    
    /**
     * number of flashes
     */
    public int count = 5;
    /**
     * interval of flash in ms
     */
    public long interval = 500;
    
    
    public LedDisplayData() {};

    public LedDisplayData(int red, int green, int blue) {
      this.red = red;
      this.green = green;
      this.blue = blue;
      
    };
    
    
}
