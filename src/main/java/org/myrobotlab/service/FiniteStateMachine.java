package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.FiniteStateMachineConfig;
import org.myrobotlab.service.config.FiniteStateMachineConfig.Transition;
import org.myrobotlab.service.config.ServiceConfig;
import org.slf4j.Logger;

import com.github.pnavais.machine.StateMachine;
import com.github.pnavais.machine.api.message.Message;
import com.github.pnavais.machine.model.State;
import com.github.pnavais.machine.model.StateTransition;
import com.github.pnavais.machine.model.StringMessage;

/**
 * 
 * @author GroG
 * 
 *         Utilizing the excellent FSM implementation here
 *         https://github.com/pnavais/state-machine
 *
 */
public class FiniteStateMachine extends Service {

  public final static Logger log = LoggerFactory.getLogger(FiniteStateMachine.class);

  private static final long serialVersionUID = 1L;

  protected StateMachine stateMachine = StateMachine.newBuilder().build();

  protected State last = null;

  protected State current;

  // must be transient since StateTransition is non serializable
  protected transient Map<String, Tuple> map = new HashMap<>();

  public class Tuple {
    public Transition transition;
    public StateTransition stateTransition;
  }

  private static Transition toFsmTransition(StateTransition state) {
    Transition transition = new Transition();
    com.github.pnavais.machine.model.State origin = state.getOrigin();
    Message message = state.getMessage();
    com.github.pnavais.machine.model.State target = state.getTarget();
    transition.from = origin.getName();
    // transition.id = state.getMessage().getMessageId();
    transition.on = message.getPayload().get().toString();
    transition.to = target.getName();
    return transition;
  }

  public FiniteStateMachine(String n, String id) {
    super(n, id);
  }

  public void clear() {
    ((FiniteStateMachineConfig) config).transitions.clear();
    map.clear();
    stateMachine.clear();
  }

  public String getNext(String key) {
    StringMessage msg = new StringMessage(key);
    Optional<State> s = stateMachine.getNext(msg);
    if (s.get() != null) {
      return s.get().getName();
    }    
    return null;
  }

  public void init() {
    stateMachine.init();
  }

  private String makeKey(String state0, String msgType, String state1) {
    return String.format("%s->%s->%s", state0, msgType, state1);
  }

  public boolean addTransition(String state0, String msgType, String state1) {
    String key = makeKey(state0, msgType, state1);

    if (map.containsKey(key)) {
      log.info("transition {} already exists", key);
      return false;
    }
    Tuple tuple = new Tuple();
    tuple.stateTransition = new StateTransition(state0, msgType, state1);
    tuple.transition = toFsmTransition(tuple.stateTransition);

    FiniteStateMachineConfig c = (FiniteStateMachineConfig) config;
    c.transitions.add(tuple.transition);
    stateMachine.add(tuple.stateTransition);
    map.put(key, tuple);
    return true;
  }

  public boolean removeTransition(String state0, String msgType, String state1) {
    String key = makeKey(state0, msgType, state1);
    if (!map.containsKey(key)) {
      log.info("transition %s does not exist", key);
      return false;
    }
    Tuple tuple = map.get(key);
    stateMachine.remove(tuple.stateTransition);
    stateMachine.prune();

    FiniteStateMachineConfig c = (FiniteStateMachineConfig) config;
    c.transitions.remove(tuple.transition);
    map.remove(key);

    return true;
  }

  /**
   * remove all orphaned states
   */
  public void prune() {
    stateMachine.prune();
  }

  /**
   * fires a message type
   * @param msg
   */
  public void fire(String msg) {
    try {

      last = stateMachine.getCurrent();
      stateMachine.send(msg);
      current = stateMachine.getCurrent();

      log.info("fired event ({}) -> ({}) moves to ({})", msg, last == null ? null : last.getName(), current == null ? null : current.getName());

      if (last != null && !last.equals(current)) {
        invoke("publishNewState", current.getName());
      }
    } catch (Exception e) {
      log.error("fire threw", e);
    }
  }

  /**
   * gets the current state of this state machine
   * @return
   */
  public String getCurrent() {
    if (current != null) {
      return current.getName();
    }
    return null;
  }

  /**
   * get the previous state of this state machine
   * @return
   */
  public String getLast() {
    if (last != null) {
      return last.getName();
    }
    return null;
  }

  public List<Transition> getTransitions() {
    FiniteStateMachineConfig c = (FiniteStateMachineConfig) config;
    return c.transitions;
  }

  /**
   * publishes state if changed here
   * @param state
   * @return
   */
  public String publishNewState(String state) {
    log.info("publishNewState {}", state);
    return state;
  }

  @Override
  public ServiceConfig getConfig() {
    FiniteStateMachineConfig c = (FiniteStateMachineConfig) config;
    return c;
  }

  @Override
  public ServiceConfig apply(ServiceConfig c) {
    FiniteStateMachineConfig newConfig = (FiniteStateMachineConfig) c;
    
    if (newConfig.transitions != null) {
      
      // since this service operates directly from config
      // when config is "applied" we need to copy out and
      // re-apply the config using addTransition
      List<Transition> newTransistions = new ArrayList<>();
      newTransistions.addAll(newConfig.transitions);
      broadcastState();
    }
    return c;
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      // webgui.setSsl(true);
      webgui.autoStartBrowser(false);
      webgui.startService();
      // Runtime.setConfig("dewey-2");
      Runtime.startConfig("dewey-2");

      boolean done = true;
      if (done) {
        return;
      }

      FiniteStateMachine fsm = (FiniteStateMachine) Runtime.start("fsm", "FiniteStateMachine");
      // Runtime.start("servo", "Servo");
      Runtime.start("webgui", "WebGui");


      // fsm.createFsm("emotional-state");

      // create a new fsm with 4 states
      // fsm.setStates("neutral", "ill", "sick", "vomiting");

      // add the ill-event transitions
      fsm.addTransition("neutral", "ill-event", "ill");
      fsm.addTransition("ill", "ill-event", "sick");
      fsm.addTransition("sick", "ill-event", "vomiting");

      // add the clear-event transitions
      fsm.addTransition("ill", "clear-event", "neutral");
      fsm.addTransition("sick", "clear-event", "ill");
      fsm.addTransition("vomiting", "clear-event", "sick");

      // fsm.subscribe("fsm", "publishState");

      log.info("state - {}", fsm.getCurrent());

      fsm.setCurrent("neutral");

      log.info("state - {}", fsm.getCurrent());

      fsm.fire("ill-event");

      log.info("state - {}", fsm.getCurrent());

      fsm.fire("ill-event");
      fsm.fire("ill-event");
      fsm.fire("ill-event");
      fsm.fire("ill-event");

      fsm.save();

      // fsm.send("clear-event", 1000);

      // fsm.removeScheduledEvents();

      log.info("state - {}", fsm.getCurrent());

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  public void setCurrent(String state) {
    try {
      last = stateMachine.getCurrent();
      stateMachine.setCurrent(state);
      current = stateMachine.getCurrent();
      if (last != null && !last.equals(current)) {
        invoke("publishNewState", current.getName());
      }
    } catch (Exception e) {
      log.error("setCurrent threw", e);
      error(e.getMessage());
    }
  }

}