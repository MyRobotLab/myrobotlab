package org.myrobotlab.service.config;

public class RasPiConfig extends ServiceConfig {
    /**
     * reading poll rate for all enabled GPIO pins
     * this "should" not be an int but a float, but at this time
     * its better to follow the PinDefinition pollRateHz type
     */
    public int pollRateHz = 1;
    
    // TODO - config which starts pins in a mode (read/write) and if write a value 0/1

}
