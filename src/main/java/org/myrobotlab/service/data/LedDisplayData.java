package org.myrobotlab.service.data;

public class LedDisplayData {

    public String action; // fill | flash | play animation | stop | clear
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
    
    
}
