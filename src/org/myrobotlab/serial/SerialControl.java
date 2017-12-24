package org.myrobotlab.serial;

import java.util.List;

public interface SerialControl {
  public List<String> getPortNames();
  
  /**
   * FIXME - need a common Serial interface
   * this has been defined many times before
   * 
   *  it "should" have open(port, baudrate)
   *  and open(port) overload
   *  
   *  p4j has a nice interface - similar would be nice too
   *  
   *  for some reason we have .connect(port) &amp; .connect(port, baud ...)
   *  what is the difference between "connect" and open - "open" is correct
   *  in the sense they all are InputStream or OutpuStream .. both are "open"
   *  where did connect come from ?
   *  
   *  Perhaps its the idea of a Microcontroller service 'connecting' to its actual microcontroller
   *  - if thats the case it should be in Microcontroller not in Serial
   *
   */
  
}
