package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.fsm.api.Event;
import org.myrobotlab.fsm.api.EventHandler;
import org.myrobotlab.fsm.api.FiniteStateMachineException;
import org.myrobotlab.fsm.api.SimpleEvent;
import org.myrobotlab.fsm.api.State;
import org.myrobotlab.fsm.api.StateHandler;
import org.myrobotlab.fsm.api.Transition;
import org.myrobotlab.fsm.core.SimpleTransition;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class FiniteStateMachine extends Service implements EventHandler, StateHandler, org.myrobotlab.fsm.api.FiniteStateMachine {

  public final static Logger log = LoggerFactory.getLogger(FiniteStateMachine.class);

  private static final long serialVersionUID = 1L;

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

  ArrayList<String> currentStates;
  Set<State> finalStates;
  Set<EventHandler> handlers = new HashSet<>();
  State lastChangedState;
  Event lastEvent;
  Transition lastTransition;
  Set<State> states;
  Set<Transition> transitions;

  // FIXME - have "default" state !!!
  // FIXME emotionalState.addTransition("emotional-state", "ill", "ill-event",
  // "ill") EXPLODES !!!
  public FiniteStateMachine(String n, String id) {
    super(n, id);
    // if conditions for json loads
    if (currentStates == null) {
      currentStates = new ArrayList<>();
    }
    if (finalStates == null) {
      finalStates = new HashSet<>();
    }
    if (states == null) {
      states = new HashSet<>();
    }
    if (transitions == null) {
      transitions = new HashSet<>();
    }
  }

  public void addScheduledEvent(String eventId, int millis) {
    addTask(millis, "fire", new Object[] { eventId });
  }

  public void addState(String state) {
    State s = new State(state);
    if (states.size() == 0) {
      currentStates.add(s.getId());
    }
    states.add(s);
  }

  public void addTransition(String state0, String eventType, String state1) {
    log.info("adding transition {}-({})->{}", state0, eventType, state1);
    // SimpleTransition transition = (SimpleTransition) new
    // SimpleTransitionBuilder().name(String.format("%s-%s->%s", state0,
    // eventType,
    // state1)).sourceState(state0).targetState(state1).eventHandler(this).build();
    SimpleTransition transition = new SimpleTransition();
    State sourceState = new State(state0);
    State targetState = new State(state1);
    transition.setId(eventType);
    transition.setSourceState(sourceState);
    transition.setTargetState(targetState);
    transition.setEventHandler(this);
    transition.setNewStateHandler(this);

    registerTransition(transition);
  }

  public void attach(Attachable service) throws Exception {
    if (service instanceof EventHandler) {
      if (service.isLocal()) {
        handlers.add((EventHandler) service);
      } else {
        addListener("publishEvent", service.getName(), "onEvent");
      }
    } else {
      info(String.format("Service.attach does not know how to attach %s to a %s", service.getClass().getSimpleName(), this.getClass().getSimpleName()));
    }
  }

  @Override
  public List<String> fire(Event inEvent) throws FiniteStateMachineException {

    // for (String currentState : currentStates) {
    for (int i = 0; i < currentStates.size(); ++i) {
      String currentState = currentStates.get(i);

      // FIXME !! CURRENTSTATE & FINALSTATES (MEBBE ALL NEEDS TO BE A MAP NOT A
      // SET !!!)
      if (!finalStates.isEmpty() && finalStates.contains(currentState)) {
        log.warn("FSM is in final state '" + currentState + "', event " + inEvent + " is ignored.");
      }

      if (inEvent == null) {
        log.warn("Null event fired, FSM state unchanged");
      }

      for (Transition transition : transitions) { // FIXME - shouldn't have to
                                                  // iterate - just use a Map
                                                  // !!!
        if (currentState.equals(transition.getSourceState().getId()) && // fsm
                                                                        // is in
                                                                        // the
        // right state as
        // expected by
        // transition
        // definition
            transition.getId().equals(inEvent.getId()) && // fired event
            // type is as
            // expected by
            // transition
            // definition
            states.contains(transition.getTargetState()) // target state is
                                                         // defined
        ) {
          try {
            SimpleEvent event = (SimpleEvent) inEvent;
            // event.setLastState(currentState);
            event.setTransition(transition);
            // perform action, if any on a transistion state handler
            if (transition.getEventHandler() != null) {
              transition.getEventHandler().handleEvent(event);
            }

            // FIXME - have transition return code e.g.
            // transition.execute(currentState, event, targetState) influence
            // transition
            // FIXME - registerTransitionExcecuteHandler

            // transition to target state
            State newState = transition.getTargetState();
            currentStates.set(i, newState.getId());
            /*
             * currentState.setId(newState.getId());
             * currentState.setLastTransition(transition);
             */
            lastChangedState = transition.getTargetState();

            if (transition.getNewStateHandler() != null) {
              transition.getNewStateHandler().handleState(transition.getTargetState());
            }

            // save last triggered event and transition
            lastEvent = event;
            lastTransition = transition;

            break;
          } catch (Exception e) {
            log.error("An exception occurred during handling event {} of transition {} ", inEvent, transition, e);
            throw new FiniteStateMachineException(transition, inEvent, e);
          }
        }
      }
    }
    return currentStates;
  }

  public void fire(String eventName) {
    try {
      log.info("firing event ({})", eventName);
      fire(new SimpleEvent(eventName));
    } catch (Exception e) {
      log.error("fire threw", e);
    }
  }

  public State getCurrentState() {
    return lastChangedState;
  }

  public List<String> getCurrentStates() {
    return currentStates;
  }

  @Override
  public Set<State> getFinalStates() {
    return finalStates;
  }

  public String getFsmMap() {
    Set<Transition> transitions = getTransitions();
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

  @Override
  public Event getLastEvent() {
    return lastEvent;
  }

  @Override
  public Transition getLastTransition() {
    return lastTransition;
  }

  @Override
  public Set<State> getStates() {
    return states;
  }

  @Override
  public Set<Transition> getTransitions() {
    return transitions;
  }

  // FIXME - have other event handlers - this one is "ONLY" for publishing new
  // States
  @Override
  public void handleEvent(Event event) throws Exception {
    SimpleEvent se = (SimpleEvent) event;
    log.info("{} recieved fired event {} and is transitioning", se.getSource(), event);

    // for subscribers ...
    invoke("publishEvent", event);

    // for local attachers
    for (EventHandler eh : handlers) {
      eh.handleEvent(event);
    }

  }

  /**
   * FIXME - do we want to publish 2 channels ? events and states ? - states has a
   * superset of info, state not as much state change - new state is published
   * 
   * @param state - incoming state to handle
   * @throws Exception - possible state exception
   */
  @Override
  public void handleState(State state) throws Exception {
    // log.info("state {}-({})-> {}",
    // state.getLastTransition().getSourceState(), state.getId(),
    // state.getLastTransition().getId(), state.getId());
    log.info("handleState {} from {}", state.getId(), state.getLastTransition());
    invoke("publishState", state);
  }

  public void onState(State state) {
    log.info("onState {}", state);
  }

  public State publishState(State state) {
    log.info("publishState {}", state);
    return state;
  }

  void registerFinalState(final State finalState) {
    finalStates.add(finalState);
  }

  public void registerTransition(final Transition transition) {
    transitions.add(transition);
  }

  public void removeScheduledEvents() {
    purgeTasks();
  }

  public void setStates(String... stateNames) {
    Set<State> states = new HashSet<>();
    State initialState = null;
    for (int i = 0; i < stateNames.length; ++i) {
      State state = new State(stateNames[i]);
      states.add(state);
      if (i == 0) {
        initialState = state;
      }
    }

    this.states = states;
    currentStates.add(initialState.getId());
  }
  
  public SimpleEvent publishEvent(SimpleEvent event) {
    return event;
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

}