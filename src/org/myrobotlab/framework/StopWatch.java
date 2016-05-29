/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.framework;

import java.util.Calendar;
import java.util.TimeZone;

public class StopWatch {
  Calendar startCal;

  Calendar endCal;
  TimeZone tz = TimeZone.getTimeZone("PST");

  public static void main(String[] args) {
    StopWatch sw = new StopWatch();
    sw.start(); // capture start time

    try {
      Thread.sleep(5000); // sleep for 5 seconds
    } catch (Exception e) {
      System.out.println(e);
      System.exit(1);
    }

    sw.end(); // capture end time

    System.out.println("Elapsed time in minutes: " + sw.elapsedMinutes());
    System.out.println("Elapsed time in seconds: " + sw.elapsedSeconds());
    System.out.println("Elapsed time in milliseconds: " + sw.elapsedMillis());
  }

  /** Creates a new instance of StopWatch */
  public StopWatch() {
  }

  public StopWatch(String tzoneStr) {
    tz = TimeZone.getTimeZone(tzoneStr);
  }

  public long elapsedMillis() {
    return endCal.getTimeInMillis() - startCal.getTimeInMillis();
  }

  public double elapsedMinutes() {
    return (endCal.getTimeInMillis() - startCal.getTimeInMillis()) / (1000.0 * 60.0);
  }

  // Measure the elapsed time in different units
  public double elapsedSeconds() {
    return (endCal.getTimeInMillis() - startCal.getTimeInMillis()) / 1000.0;
  }

  // Stop the stopwatch
  public void end() {
    endCal = Calendar.getInstance(tz);
  }

  // Start the stopwatch
  public void start() {
    startCal = Calendar.getInstance(tz);
  }
} // end of StopWatch class