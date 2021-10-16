package org.myrobotlab.service.data;

/**
 * This represents an utterance.  It is a represents a message from one user to another.
 * 
 */
public class Utterance {

  // The user that produced this utterance
  String username;
  // Where the utterance was heard/created.  (could be a private/direct message, or it could be to a channel / group)
  String channel;
  // The text of the utterance
  String text;

}
