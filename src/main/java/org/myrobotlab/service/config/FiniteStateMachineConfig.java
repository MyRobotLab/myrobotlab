package org.myrobotlab.service.config;

import java.util.ArrayList;
import java.util.List;

public class FiniteStateMachineConfig extends ServiceConfig {
  
  public static class Transition {
    public Transition() {
      
    }
    public Transition(String begin, String event, String end) {
      this.begin = begin;
      this.event = event;
      this.end = end;
    }
    public String begin;
    public String event;
    public String end;
  }

  public List<String> states = new ArrayList<>();
  public List<Transition> transitions = new ArrayList<>();

}
