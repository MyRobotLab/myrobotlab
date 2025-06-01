package org.myrobotlab.service.config;

import java.util.ArrayList;
import java.util.List;

public class FiniteStateMachineConfig extends ServiceConfig {

  public static class Transition {
    
    public Transition() {
    }
    
    public Transition(String from, String event, String to) {
      this.from = from;
      this.event = event;
      this.to = to;
    }

    public String from;
    public String event;
    public String to;
  }

  public List<Transition> transitions = new ArrayList<>();

  public String start = null;
  

}
