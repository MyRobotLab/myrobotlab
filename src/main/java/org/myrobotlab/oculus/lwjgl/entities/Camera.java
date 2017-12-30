package org.myrobotlab.oculus.lwjgl.entities;

import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector3f;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class Camera {

  public final static Logger log = LoggerFactory.getLogger(Camera.class);

  private Vector3f position = new Vector3f(0, 0, 0);
  private float pitch;
  private float yaw;
  private float roll;

  public Camera() {

  }

  public void move() {
    // TODO: make some sort of proper joystick support here.
    // and a callback
    if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
      position.y -= 0.02;
    }
    if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
      position.y += 0.02;
    }
    if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
      position.x -= 0.02;
    }
    if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
      position.x += 0.02;
    }
    if (Keyboard.isKeyDown(Keyboard.KEY_L)) {
      position.z += 0.02;
    }
    if (Keyboard.isKeyDown(Keyboard.KEY_P)) {
      position.z -= 0.02;
    }
    log.info("Camera Position X:{} Y:{} Z:{}", position.x, position.y, position.z);
  }

  public Vector3f getPosition() {
    return position;
  }

  public void setPosition(Vector3f position) {
    this.position = position;
  }

  public float getPitch() {
    return pitch;
  }

  public void setPitch(float pitch) {
    this.pitch = pitch;
  }

  public float getYaw() {
    return yaw;
  }

  public void setYaw(float yaw) {
    this.yaw = yaw;
  }

  public float getRoll() {
    return roll;
  }

  public void setRoll(float roll) {
    this.roll = roll;
  }
}
