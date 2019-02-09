package org.myrobotlab.jme3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.JMonkeyEngine;
import org.slf4j.Logger;

public class Interpolator {

  public final static Logger log = LoggerFactory.getLogger(JMonkeyEngine.class);

  public class Move {
    String name;
    String type;
    double newPos;
    double speed;
    long startTs;
    long nextMoveTs;
    long estimatedEndTs;
    public Float startPos;
    public int direction;

    public String toString() {
      return String.format("%s %s %.2f %.2f %d", name, type, newPos, speed, startTs);
    }
  }

  Map<String, Move> futureMoves = new ConcurrentHashMap<String, Move>();
  private JMonkeyEngine jme;

  // need to get FPS

  public Interpolator(JMonkeyEngine jMonkeyEngine) {
    this.jme = jMonkeyEngine;
  }

  // external thread does a request
  // processed into a data object which generates the next Msg
  public void addAnimation(String name, String type, double newPos, double speed) {
    
    // TODO Auto-generated method stub
    Move move = new Move();
    // thingy we are moving
    move.name = name;
    move.newPos = newPos; 
    move.startPos = jme.getAngle(name);
    if (move.startPos == null) {
      log.error("could not get angle for {} cannot create animation", name);
      return;
    }
    move.direction = (newPos - move.startPos > 0) ? 1 : -1;
    move.type = type;
    move.speed = speed;
    // FIXME - this needs to be the difference ??? - get
                          // current pos => find distance and direction
    // move.newPos = jme.getAngle(move.name) - newPos; // FIXME - this needs to
    // be the difference ??? - get current pos => find distance and direction

    move.startTs = System.currentTimeMillis();
    
    log.debug("addAnimation {} {} from {} to {} @ {} degrees/sec", name, type, move.startPos, newPos, speed);
    futureMoves.put(name, move);
  }

  public Jme3Msg getNext() {
    return null;
  }

  // FIXME - use ms for rate vs just seconds ?!
  public void generateMoves() {
    // for (Move move : futureMoves.values()) {
    for (String name : futureMoves.keySet()) {
      Move move = futureMoves.get(name);
      // find the time into our move (total) vs last ..
      long deltaTime = System.currentTimeMillis() - move.startTs; // milliseconds

      // total difference in our move - degrees per second
      double interPos = move.startPos + (move.direction * deltaTime * move.speed / 1000);

      log.debug(String.format("%s deltaTime %d ms new position %.2f", move.name, deltaTime, interPos));

      // instantaneous api to rotate - don't generate an animation from an
      // animation
      jme.rotateTo(move.name, interPos);

      if (Math.abs(interPos - move.newPos) < 0.5 /* if we're close enough */
          || /* or we overrun */ (move.direction < 0 && interPos < move.newPos)
          || /* or we overrun */ (move.direction > 0 && interPos > move.newPos)) {
        log.debug("removing animation {}", move);
        futureMoves.remove(move.name);
      }
    }
  }

}
