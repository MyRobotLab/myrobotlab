package org.myrobotlab.process;

import java.io.IOException;

import org.myrobotlab.service.TerminalManager;

public class Ros2Terminal extends Terminal {

  public Ros2Terminal(TerminalManager service, String name) throws IOException {
    super(service, name);
  }

}
