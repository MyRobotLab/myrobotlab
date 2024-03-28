package org.myrobotlab.process;

import java.io.IOException;

import org.myrobotlab.service.TerminalManager;

public class RosTerminal extends Terminal {

  public RosTerminal(TerminalManager service, String name) throws IOException {
    super(service, name);
  }

}
