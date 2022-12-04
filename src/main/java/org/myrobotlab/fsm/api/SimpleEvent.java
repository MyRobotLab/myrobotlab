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

import java.util.Date;

import org.myrobotlab.fsm.util.Utils;

/**
 * general event class for invoking transitions
 * 
 */
public class SimpleEvent extends Event {
  @Deprecated
  String source;
  private Transition transition;
  // private State lastState;

  public SimpleEvent(String name) {
    this.name = name;
    timestamp = System.currentTimeMillis();
  }

  public SimpleEvent() {
    this.name = Utils.DEFAULT_EVENT_NAME;
    timestamp = System.currentTimeMillis();
  }

  public SimpleEvent(String fsmName, String eventName) {
    this.source = fsmName;
    this.name = eventName;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Event");
    sb.append("{name='").append(name).append('\'');
    sb.append(", timestamp=").append(new Date(timestamp));
    sb.append('}');
    return sb.toString();
  }

  @Override
  public String getId() {
    return name;
  }

  public String getSource() {
    return source;
  }

  public void setTransition(Transition transition) {
    this.transition = transition;
  }

  public Transition getTransition() {
    return transition;
  }

}
