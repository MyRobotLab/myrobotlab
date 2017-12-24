/**
 * 
 */
package org.myrobotlab.jme3.interfaces;

import java.util.concurrent.ConcurrentHashMap;

import org.myrobotlab.framework.Service;
import org.myrobotlab.kinematics.CollisionItem;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.service.Servo.IKData;

import com.jme3.system.AppSettings;

/**
 * @author chris
 *
 */
public interface IntegratedMovementInterface {
  public void updatePosition(IKData event);
  public void setService(Service service);
  public void addObject(CollisionItem item);
  public void addObject(ConcurrentHashMap<String, CollisionItem> items);
  public void addPoint(Point point);
  public void setSettings(AppSettings settings);
  public void setShowSettings(boolean b);
  public void setPauseOnLostFocus(boolean b);
  public void start();
}
