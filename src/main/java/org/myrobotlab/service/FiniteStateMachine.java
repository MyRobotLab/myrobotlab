package org.myrobotlab.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.fsm.api.Event;
import org.myrobotlab.fsm.api.EventHandler;
import org.myrobotlab.fsm.api.SimpleEvent;
import org.myrobotlab.fsm.api.State;
import org.myrobotlab.fsm.api.StateHandler;
import org.myrobotlab.fsm.api.Transition;
import org.myrobotlab.fsm.core.SimpleFiniteStateMachine;
import org.myrobotlab.fsm.core.SimpleTransition;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class FiniteStateMachine extends Service implements EventHandler, StateHandler {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(FiniteStateMachine.class);

  SimpleFiniteStateMachine stateMachine;
  /**
   * callback handlers
   */
  Set<EventHandler> handlers = new HashSet<>();


  // FIXME - have "default" state !!!
  // FIXME emotionalState.addTransition("emotional-state", "ill", "ill-event", "ill") EXPLODES !!!
  public FiniteStateMachine(String n) {
    super(n);
    if (stateMachine == null) {
      stateMachine = new SimpleFiniteStateMachine(n);
    }
  }
  
  /*
  public void addState(String fsmName, String state) {
    if (!fsms.containsKey(fsmName)) {
      addStates(fsmName, state);
    }
    fsms.get(fsmName).addState(state);
  }
  */

  public void setStates(String... states) {    
    stateMachine.setStates(states);
  }

  public void addTransition(String state0, String eventType, String state1) {
    log.info("adding transition {}-({})->{}", state0, eventType, state1);
    // SimpleTransition transition = (SimpleTransition) new SimpleTransitionBuilder().name(String.format("%s-%s->%s", state0, eventType, state1)).sourceState(state0).targetState(state1).eventHandler(this).build();
    SimpleTransition transition = new SimpleTransition();
    State sourceState = new State(state0);
    State targetState = new State(state1);
    transition.setId(eventType);
    transition.setSourceState(sourceState);
    transition.setTargetState(targetState);
    transition.setEventHandler(this);
    transition.setNewStateHandler(this);
    stateMachine.registerTransition(transition);
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(FiniteStateMachine.class);
    meta.addDescription("general service which can create and maintaine multiple finite state machines");
    // meta.addDependency("orgId", "artifactId", "2.4.0");
    meta.addCategory("general", "ai");
    return meta;
  }

  public State publishState(State state) {
    log.info("publishState {}", state);
    return state;
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      FiniteStateMachine fsm = (FiniteStateMachine) Runtime.start("fsm", "FiniteStateMachine");
      // Runtime.start("servo", "Servo");
      // Runtime.start("gui", "SwingGui");
      
      // fsm.createFsm("emotional-state");

      // create a new fsm with 4 states
      fsm.setStates("neutral", "ill", "sick", "vomiting");

      // add the ill-event transitions
      fsm.addTransition("neutral", "ill-event", "ill");
      fsm.addTransition("ill", "ill-event", "sick");
      fsm.addTransition("sick", "ill-event", "vomiting");

      // add the clear-event transitions
      fsm.addTransition("ill", "clear-event", "neutral");
      fsm.addTransition("sick", "clear-event", "ill");
      fsm.addTransition("vomiting", "clear-event", "sick");

      fsm.subscribe("fsm", "publishState");

      fsm.fire("ill-event");
      fsm.fire("ill-event");
      fsm.fire("ill-event");
      fsm.fire("ill-event");
      fsm.fire("ill-event");
      
      fsm.addScheduledEvent("clear-event", 1000);
      
      fsm.removeScheduledEvents();
      
      fsm.getCurrentState();
      fsm.getCurrentStates();

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  public List<String> getCurrentStates() {
    return stateMachine.getCurrentStates();
  }

  public void removeScheduledEvents() {
    purgeTasks();
  }

  public void addScheduledEvent(String eventId, int millis) {
    putTask(millis, "fire", new Object[] {eventId});
  }

  public State getCurrentState() {
    return stateMachine.getCurrentState();
  }

  public void fire(String eventName) {
    try {
      log.info("firing event ({})", eventName);
      stateMachine.fire(new SimpleEvent(eventName));
    } catch (Exception e) {
      log.error("fire threw", e);
    }
  }

  // FIXME - have other event handlers - this one is "ONLY" for publishing new States
  @Override
  public void handleEvent(Event event) throws Exception {
    SimpleEvent se = (SimpleEvent)event;
    log.info("{} recieved fired event {} and is transitioning", se.getSource(), event);
    
    // for subscribers ...
    invoke("publishEvent", event);
    
    // for local attachers
    for (EventHandler eh: handlers) {
      eh.handleEvent(event);
    }
 
  }
  
  /**
   * FIXME - do we want to publish 2 channels ? events & states ? - states has a superset of info, state not as much
   * state change - new state is published
   * @param event
   * @throws Exception
   */
  @Override
  public void handleState(State state) throws Exception{
    // log.info("state {}-({})-> {}", state.getLastTransition().getSourceState(), state.getId(), state.getLastTransition().getId(), state.getId());
    log.info("handleState {} from {}", state.getId(), state.getLastTransition());
    invoke("publishState", state);
  }
  
  public void onState(State state){
    log.info("onState {}", state);
  }

  public String getFsmMap() {
    Set<Transition> transitions = stateMachine.getTransitions();
    Set<String> sort = new TreeSet<String>();
    StringBuilder sb = new StringBuilder();
    sb.append(getName() + ":\n");
    for (Transition transition : transitions) {
      sort.add(String.format("%s-(%s)->%s\n", transition.getSourceState(), transition.getId(), transition.getTargetState()));
    }
    for (String t : sort) {
      sb.append(t);
    }
    return sb.toString();
  }

  @Deprecated // UNSUPPORTED !!!
  /*
  public void setState(String fsmName, String state) {
    SimpleFiniteStateMachine fsm = stateMachine.get(fsmName);
    // fsm.setState(new State(state));
  }
  */

  public void attach(Attachable service) throws Exception {
    if (service instanceof EventHandler) {
      if (service.isLocal()) {
        handlers.add((EventHandler)service);
      } else {
        addListener("publishEvent",service.getName(),"onEvent");
      }
    } else {
      info(String.format("Service.attach does not know how to attach %s to a %s", service.getClass().getSimpleName(), this.getClass().getSimpleName()));
    }
  }

  public void addState(String state) {
    stateMachine.addState(state);
  }

}

// yum yum ...