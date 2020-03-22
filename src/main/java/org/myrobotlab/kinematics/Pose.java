package org.myrobotlab.kinematics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.service.interfaces.ServoControl;

/** represent a set of servo positions at a given point in time */
public class Pose {

  public final String name;
  public final Date createdDate;
  public HashMap<String, Double> positions = new HashMap<String, Double>();
  public HashMap<String, Double> speeds = new HashMap<String, Double>();

  public Pose(String name, List<ServoControl> servos) {
    this.name = name;
    this.createdDate = new Date();
    List<String> servoNames = new ArrayList<String>();
    for (ServoControl sc : servos) {
      positions.put(sc.getName(), sc.getPos());
      speeds.put(sc.getName(),  sc.getSpeed());
      servoNames.add(sc.getName());
    }
  }

  public HashMap<String, Double> getSpeeds() {
    return speeds;
  }
  
  public HashMap<String, Double> getPositions() {
    return positions;
  }

  public void savePose(String filename) throws IOException {
    String s = CodecUtils.toJson(this);
    FileOutputStream out = new FileOutputStream(new File(filename));
    out.write(s.getBytes());
    out.close();
  }

  public static Pose loadPose(String filename) throws IOException {
    String json = FileIO.toString(filename);
    Pose pose = (Pose) CodecUtils.fromJson(json, Pose.class);
    return pose;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((createdDate == null) ? 0 : createdDate.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((positions == null) ? 0 : positions.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Pose other = (Pose) obj;
    if (createdDate == null) {
      if (other.createdDate != null)
        return false;
    } else if (!createdDate.equals(other.createdDate))
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (positions == null) {
      if (other.positions != null)
        return false;
    } else if (!positions.equals(other.positions))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "Pose [positions=" + positions + ", speeds=\" + speeds + \", name=" + name + ", createdDate=" + createdDate + "]";
  }

}
