/**
 * 
 */
package org.myrobotlab.jme3;

import java.util.concurrent.ConcurrentHashMap;

import org.myrobotlab.framework.Service;
import org.myrobotlab.kinematics.CollisionItem;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.service.interfaces.ServoData;

import com.jme3.system.AppSettings;

/**
 * @author chris
 *
 */
public interface IntegratedMovementInterface {
  public void updatePosition(ServoData event);

  public void setService(Service service);

  public void addObject(CollisionItem item);

  public void addObject(ConcurrentHashMap<String, CollisionItem> items);

  public void addPoint(Point point);

  public void setSettings(AppSettings settings);

  public void setShowSettings(boolean b);

  public void setPauseOnLostFocus(boolean b);

  public void start();
}
