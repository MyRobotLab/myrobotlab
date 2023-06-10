package org.myrobotlab.kinematics;

import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.InMoov2;
import org.slf4j.Logger;

/** represent a set of servo positions at a given point in time */
public class Sequence {

  public final static Logger log = LoggerFactory.getLogger(InMoov2.class);

  /**
   * sequence of poses and offset times
   */
  public List<SequencePart> parts = new ArrayList<>();

  public boolean repeat = false;

}
