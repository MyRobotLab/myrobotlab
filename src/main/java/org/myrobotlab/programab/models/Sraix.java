package org.myrobotlab.programab.models;

/**
 * Basic Sraix model, AIML 2.0 has more elements but these seemed like the most
 * relevant and ar actually used.
 * 
 * @author GroG
 *
 */
public class Sraix {

  /**
   * Search text when a query is sent to a remote system
   */
  public String search;

  /**
   * Oob is Out Of Band text which can be handled by internal processing
   */
  public Oob oob;

}
