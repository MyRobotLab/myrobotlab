/**
 * The MIT License
 *
 *  Copyright (c) 2017, Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package org.myrobotlab.fsm.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.myrobotlab.fsm.api.Event;
import org.myrobotlab.fsm.api.FiniteStateMachine;
import org.myrobotlab.fsm.api.FiniteStateMachineException;
import org.myrobotlab.fsm.api.SimpleEvent;
import org.myrobotlab.fsm.api.State;
import org.myrobotlab.fsm.api.Transition;

public class SimpleFiniteStateMachine implements FiniteStateMachine {

  private static final Logger LOGGER = Logger.getLogger(SimpleFiniteStateMachine.class.getSimpleName());

  // private Set<State> currentStates;
  ArrayList<String> currentStates;
  private Set<State> finalStates;
  private Set<State> states;
  private Set<Transition> transitions;
  private Event lastEvent;
  private State lastChangedState;
  private Transition lastTransition;

  public SimpleFiniteStateMachine(String name) {
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

  /**
   * replaces states
   * 
   * @param states
   * @param initialState
   */
  /*
   * public void setStates(final Set<State> states, final State initialState) {
   * this.states = states; currentState = initialState; }
   */
  /*
   * public void addState(String state) { State s = new State(state); if
   * (states.size() == 0) { // first state will be current & initial unless
   * consumer changes it currentState = s; initialState = s; } states.add(s); }
   */

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

  /**
   * {@inheritDoc}
   */
  @Override
  public final synchronized List<String> fire(final Event inEvent) throws FiniteStateMachineException {
    
    // for (String currentState : currentStates) {
    for (int i = 0; i < currentStates.size(); ++i) {
      String currentState = currentStates.get(i);

    if (!finalStates.isEmpty() && finalStates.contains(currentState)) {
      LOGGER.log(Level.WARNING, "FSM is in final state '" + currentState + "', event " + inEvent + " is ignored.");
      // return currentStates;
    }

    if (inEvent == null) {
      LOGGER.log(Level.WARNING, "Null event fired, FSM state unchanged");
      // return currentStates;
    }
    for (Transition transition : transitions) { // FIXME - shouldn't have to iterate - just use a Map !!!      
      if (currentState.equals(transition.getSourceState().getId()) && // fsm is in the
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
          currentState.setId(newState.getId());
          currentState.setLastTransition(transition);
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
          LOGGER.log(Level.SEVERE, "An exception occurred during handling event " + inEvent + " of transition " + transition, e);
          throw new FiniteStateMachineException(transition, inEvent, e);
        }
      }
    }
    }
    return currentStates;
  }

  public void registerTransition(final Transition transition) {
    transitions.add(transition);
  }

  void registerFinalState(final State finalState) {
    finalStates.add(finalState);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public State getCurrentState() {
    return lastChangedState;
  }
  
  public List<String> getCurrentStates() {
    return currentStates;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public Set<State> getFinalStates() {
    return finalStates;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<State> getStates() {
    return states;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Transition> getTransitions() {
    return transitions;
  }

  @Override
  public Event getLastEvent() {
    return lastEvent;
  }

  @Override
  public Transition getLastTransition() {
    return lastTransition;
  }

  public void addState(String state) {
    State s = new State(state);
    if (states.size() == 0) {
      currentStates.add(s.getId());
    }
   states.add(s);
  }

  // TODO - setStates & addState
  /*
  public void setState(State state) {
    currentStates = state;
  }
  */

}
