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
package org.myrobotlab.fsm.api;


import java.util.List;
import java.util.Set;

/**
 * FSM interface. This is the main abstraction for a finite state machine.
 *
 * @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 */
public interface FiniteStateMachine {

    /**
     * Return current FSM state.
     * @return current FSM state
     */
    State getCurrentState();

    /**
     * Return FSM initial state.
     * @return FSM initial state
     */
    
    // State getInitialState();

    /**
     * Return FSM final states.
     * @return FSM final states
     */
    Set<State> getFinalStates();

    /**
     * Return FSM registered states.
     * @return FSM registered states
     */
    Set<State> getStates();

    /**
     * Return FSM registered transitions.
     * @return FSM registered transitions
     */
    Set<Transition> getTransitions();

    /**
     * Return the last triggered event.
     * @return the last triggered event
     */
    Event getLastEvent();

    /**
     * Return the last transition made.
     * @return the last transition made
     */
    Transition getLastTransition();

    /**
     * Fire an event. According to event type, the FSM will make the right transition.
     * @param event to fire
     * @return The next FSM state defined by the transition to make
     * @throws FiniteStateMachineException thrown if an exception occurs during event handling
     */
    // State fire(Event event) throws FiniteStateMachineException;
    
    List<String> fire(Event event) throws FiniteStateMachineException;

}
