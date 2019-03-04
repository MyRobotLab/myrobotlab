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

import org.myrobotlab.fsm.api.EventHandler;
import org.myrobotlab.fsm.api.State;
import org.myrobotlab.fsm.api.StateHandler;
import org.myrobotlab.fsm.api.Transition;
import org.myrobotlab.fsm.util.Utils;


public class SimpleTransition implements Transition {

    private String id;
    private State sourceState;
    private State targetState;
    // private Class eventType; 
    transient private EventHandler eventHandler;
    transient private StateHandler newStateHandler;
    // private String source;

    public SimpleTransition() {
        id = Utils.DEFAULT_TRANSITION_NAME;
    }

    public State getSourceState() {
        return sourceState;
    }

    public void setSourceState(State sourceState) {
        this.sourceState = sourceState;        
    }

    public State getTargetState() {
        return targetState;
    }

    public void setTargetState(State targetState) {
        this.targetState = targetState;      
    }

    public String getName() {
        return id;
    }

    public void setName(String name) {
        this.id = name;
    }

    public EventHandler getEventHandler() {
        return eventHandler;
    }

    public void setEventHandler(EventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    public void setNewStateHandler(StateHandler stateHandler) {
      this.newStateHandler = stateHandler;
  }

    /*
     * Transitions are unique according to source state and triggering event type
     */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        SimpleTransition that = (SimpleTransition) o;

        return id.equals(that.id) && sourceState.equals(that.sourceState);

    }

    @Override
    public int hashCode() {
        int result = sourceState.hashCode();
        // result = 31 * result + eventType.hashCode();
        result = 31 * result + id.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(sourceState.getId());
        sb.append("-(");
        sb.append(getId());
        sb.append(")->");
        sb.append(targetState.getId());
        /*
        sb.append("Transition");
        sb.append("{name='").append(id).append('\'');
        sb.append(", sourceState=").append(sourceState.getId());
        sb.append(", targetState=").append(targetState.getId());
        sb.append(", id=").append(id);
        if (eventHandler != null) {
            sb.append(", eventHandler=").append(eventHandler.getClass().getName());
        }
        sb.append('}');
        */
        return sb.toString();
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public StateHandler getNewStateHandler() {
      return newStateHandler;
    }

    public void setId(String id) {
      this.id = id;
    }
}
