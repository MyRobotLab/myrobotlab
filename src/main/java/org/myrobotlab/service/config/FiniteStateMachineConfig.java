package org.myrobotlab.service.config;

import java.util.ArrayList;
import java.util.List;

public class FiniteStateMachineConfig extends ServiceConfig {

  public static class Transition {
    
    public Transition() {
    }
    
    public Transition(String from, String on, String to) {
      this.from = from;
      this.on = on;
      this.to = to;
    }

    public String from;
    public String on;
    public String to;
  }

  public List<Transition> transitions = new ArrayList<>();
  // public Set<Transition> transitions = new LinkedHashSet<>();

}
