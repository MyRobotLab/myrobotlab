package org.myrobotlab.framework;

import java.util.Comparator;

import org.myrobotlab.service.config.ServiceConfig.Listener;

/**
 * This comparator is used to organize subscriptions/listeners in yml files so
 * that they are all nicely ordered by publishing method.
 * 
 * @author GroG
 */
public class MrlListenerComparator implements Comparator<Listener> {
  @Override
  public int compare(Listener listener1, Listener listener2) {
    return listener1.method.compareTo(listener2.method);
  }
}