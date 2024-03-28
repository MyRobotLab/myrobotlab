package org.myrobotlab.process;

import java.io.IOException;

import org.myrobotlab.service.TerminalManager;

public class NodeTerminal extends Terminal {

  public NodeTerminal(TerminalManager service, String name) throws IOException {
    super(service, name);
  }

}
