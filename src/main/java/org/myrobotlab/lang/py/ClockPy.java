package org.myrobotlab.lang.py;

import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.service.Clock;

public class ClockPy extends LangPyUtils {


  public String toPython(ServiceInterface si) {
    
    // common stuff
    Clock clock = (Clock) si;   
    StringBuilder content = new StringBuilder();
    String name = safeRefName(si);

    Integer i = clock.getInterval();
    if (i != 1000) {
      content.append("  " + name + String.format(".setInterval(%d)\n", i));
    }
    
    content.append("  " +"# Clock Config : " + name + "\n");

    if (clock.isClockRunning()) {
      content.append("  " + name + ".startClock()\n");
    }
    
    return content.toString();
  }

}
