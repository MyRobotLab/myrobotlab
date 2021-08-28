package org.myrobotlab.kinematics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.InMoov2;
import org.slf4j.Logger;

/** represent a set of servo positions at a given point in time */
public class Sequence {

  public final static Logger log = LoggerFactory.getLogger(InMoov2.class);

  public String name;

  /**
   * sequence of poses and offset times
   */
  public List<PoseSequence> poses = new ArrayList<>();

  public boolean cycle = false;

  public Sequence() {
  }

  public Sequence(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return "Sequence: " + name;
  }

  public static Sequence loadSequence(String filename) throws IOException {
    String json = FileIO.toString(filename);
    Sequence pose = (Sequence) CodecUtils.fromJson(json, Sequence.class);
    return pose;
  }

}
