package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.MessageListener;
import org.myrobotlab.framework.interfaces.ServiceInterface;
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
 * Utilizing the excellent FSM implementation here
 * https://github.com/pnavais/state-machine
 * 
 * @author GroG
 */
public class FiniteStateMachine extends Service<FiniteStateMachineConfig> {

  public final static Logger log = LoggerFactory.getLogger(FiniteStateMachine.class);

  private static final long serialVersionUID = 1L;

  protected StateMachine stateMachine = StateMachine.newBuilder().build();

  protected State last = null;

  protected State current = null;

  protected String lastEvent = null;

  protected Set<String> messageListeners = new HashSet<>();

  // TODO - .from("A").to("B").on(Messages.ANY)
  // TODO - .from("A").to("B").on(Messages.EMPTY)

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
    transition.event = message.getPayload().get().toString();
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
   * for fsm event publishers
   * 
   * @param event
   * @return
   */
  public String onEvent(String event) {
    log.error("{} event arrived", event);
    fire(event);
    return event;
  }

  /**
   * fires a message type
   * 
   * @param event
   */
  public void fire(String event) {
    try {

      last = stateMachine.getCurrent();
      stateMachine.send(event);
      current = stateMachine.getCurrent();

      log.info("fired event ({}) -> ({}) moves to ({})", event, last == null ? null : last.getName(), current == null ? null : current.getName());

      if (last != null && !last.equals(current)) {
        invoke("publishNewState", current.getName());
      }
    } catch (Exception e) {
      log.error("fire threw", e);
    }
  }

  public String firedEvent(String event) {
    lastEvent = event;
    return event;
  }

  /**
   * gets the current state of this state machine
   * 
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
   * 
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
   * 
   * @param state
   * @return
   */
  public String publishNewState(String state) {
    log.error("publishNewState {}", state);
    for (String listener : messageListeners) {
      ServiceInterface service = Runtime.getService(listener);
      if (service != null) {
        org.myrobotlab.framework.Message msg = org.myrobotlab.framework.Message.createMessage(getName(), listener, CodecUtils.getCallbackTopicName(state), null);
        service.in(msg);
      }
    }
    return state;
  }

  @Override
  public FiniteStateMachineConfig getConfig() {
    super.getConfig();
    config.current = getCurrent();
    config.messageListeners = new ArrayList<>();
    config.messageListeners.addAll(messageListeners);
    return config;
  }

  @Override
  public FiniteStateMachineConfig apply(FiniteStateMachineConfig c) {
    super.apply(c);

    if (config.transitions != null) {

      // since this service operates directly from config
      // when config is "applied" we need to copy out and
      // re-apply the config using addTransition
      List<Transition> newTransistions = new ArrayList<>();
      newTransistions.addAll(config.transitions);
      clear();
      for (Transition t : newTransistions) {
        addTransition(t.from, t.event, t.to);
      }

      messageListeners = new HashSet<>();
      messageListeners.addAll(config.messageListeners);
      broadcastState();
    }

    // setCurrent
    if (config.current != null) {
      setCurrent(config.current);
    }

    return c;
  }

  public void attach(String name) {
    attachMessageListener(name);
  }

  public void attach(MessageListener listener) {
    attachMessageListener(listener.getName());
  }

  public void attachMessageListener(String listener) {
    messageListeners.add(listener);
  }

  public void detachMessageListener(String listener) {
    messageListeners.remove(listener);
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.WARN);

      // Runtime.startConfig("sub-04");

      // WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      // // webgui.setSsl(true);
      // webgui.autoStartBrowser(false);
      // webgui.startService();
      // FIXME !!! - setConfig and it operates on that config ... is that really
      // desired ?
      // Runtime.setConfig("fsm-test-01");
      // Runtime.startConfig("dewey-2");

      // Runtime.startConfig("worky");

      // YAMLImporter.builder().build().parseFile("docker-machine.yml");

      FiniteStateMachine fsm = (FiniteStateMachine) Runtime.start("i01.fsm", "FiniteStateMachine");
      // Runtime.start("servo", "Servo");
      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();

      Runtime.start("python", "Python");

      // TODO - need properties for each state ?

      fsm.addTransition("start", "starting", "initialize");

      fsm.addTransition("initialize", "initialized", "idle");

      fsm.addTransition("idle", "random", "random");
      fsm.addTransition("idle", "report", "report");
      fsm.addTransition("idle", "searching", "searching");
      fsm.addTransition("idle", "sleeping", "sleeping");
      fsm.addTransition("idle", "tracking", "tracking");

      fsm.addTransition("random", "idle", "idle");
      fsm.addTransition("report", "idle", "idle");
      fsm.addTransition("searching", "idle", "idle");
      fsm.addTransition("sleeping", "wake", "waking");
      fsm.addTransition("sleeping", "idle", "idle");
      fsm.addTransition("tracking", "idle", "idle");

      fsm.setCurrent("start");

      Runtime.start("i01", "InMoov2");

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

      boolean done = true;
      if (done) {
        return;
      }

      // fsm.send("clear-event", 1000);

      // fsm.removeScheduledEvents();

      log.info("state - {}", fsm.getCurrent());

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  public void setCurrent(String state) {
    try {
      if (state == null) {
        warn("attempting to set state to null");
        return;
      }
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