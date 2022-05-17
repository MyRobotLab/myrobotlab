package org.myrobotlab.service.config;

import java.util.ArrayList;
import java.util.List;

public class FiniteStateMachineConfig extends ServiceConfig {

  public static class Transition {
    
    public Transition() {
    }

    public Transition(String origin, String message, String target) {
      this.origin = origin;
      this.message = message;
      this.target = target;
    }

    // transient public UUID id;

    public String origin;
    public String message;
    public String target;
  }

  public List<Transition> transitions = new ArrayList<>();
  // public Set<Transition> transitions = new LinkedHashSet<>();

}
