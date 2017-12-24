/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.NeoPixelControl;
import org.myrobotlab.service.interfaces.NeoPixelController;
import org.slf4j.Logger;

public class NeoPixel extends Service implements NeoPixelControl {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(NeoPixel.class);

  transient NeoPixelController controller;

  public static class PixelColor {
    public int address;
    public int red;
    public int blue;
    public int green;
    public boolean changed;

    PixelColor(int address, int red, int green, int blue) {
      this.address = address;
      this.red = red;
      this.blue = blue;
      this.green = green;
      changed = true;
    }

    PixelColor() {
      address = 0;
      red = 0;
      blue = 0;
      green = 0;
      changed = true;
    }

    public boolean isEqual(PixelColor pix) {
      if (pix.red == red && pix.green == green && pix.blue == blue) {
        return true;
      }
      return false;
    }
  }

  public HashMap<Integer, PixelColor> pixelMatrix = new HashMap<Integer, PixelColor>();
  public List<PixelColor> savedPixelMatrix = new ArrayList<PixelColor>();

  public Integer numPixel = 0;

  /**
   * list of names of possible controllers
   */
  public List<String> controllers;
  public String controllerName;

  boolean isAttached = false;

  public Integer pin;
  public boolean off = false;

  public static transient final int NEOPIXEL_ANIMATION_NO_ANIMATION = 0;
  public static transient final int NEOPIXEL_ANIMATION_STOP = 1;
  public static transient final int NEOPIXEL_ANIMATION_COLOR_WIPE = 2;
  public static transient final int NEOPIXEL_ANIMATION_LARSON_SCANNER = 3;
  public static transient final int NEOPIXEL_ANIMATION_THEATER_CHASE = 4;
  public static transient final int NEOPIXEL_ANIMATION_THEATER_CHASE_RAINBOW = 5;
  public static transient final int NEOPIXEL_ANIMATION_RAINBOW = 6;
  public static transient final int NEOPIXEL_ANIMATION_RAINBOW_CYCLE = 7;
  public static transient final int NEOPIXEL_ANIMATION_FLASH_RANDOM = 8;
  public static transient final int NEOPIXEL_ANIMATION_IRONMAN = 9;

  public List<String> animations = Arrays.asList("No animation", "Stop", "Color Wipe", "Larson Scanner", "Theater Chase", "Theater Chase Rainbow", "Rainbow", "Rainbow Cycle",
      "Flash Random", "Ironman");
  public transient String animation = "No animation";
  public transient boolean[] animationSetting = { false, false }; // red,
                                  // green,
                                  // blue,
                                  // speed
  public transient boolean animationSettingColor = false;
  public transient boolean animationSettingSpeed = false;
  transient HashMap<Integer, boolean[]> animationSettings = new HashMap<Integer, boolean[]>();

  public NeoPixel(String n) {
    super(n);
    animationSettings.put(NEOPIXEL_ANIMATION_STOP, new boolean[] { false, false });
    animationSettings.put(NEOPIXEL_ANIMATION_COLOR_WIPE, new boolean[] { true, true });
    animationSettings.put(NEOPIXEL_ANIMATION_LARSON_SCANNER, new boolean[] { true, true });
    animationSettings.put(NEOPIXEL_ANIMATION_THEATER_CHASE, new boolean[] { true, true });
    animationSettings.put(NEOPIXEL_ANIMATION_THEATER_CHASE_RAINBOW, new boolean[] { false, true });
    animationSettings.put(NEOPIXEL_ANIMATION_RAINBOW, new boolean[] { false, true });
    animationSettings.put(NEOPIXEL_ANIMATION_RAINBOW_CYCLE, new boolean[] { false, true });
    animationSettings.put(NEOPIXEL_ANIMATION_FLASH_RANDOM, new boolean[] { true, true });
    animationSettings.put(NEOPIXEL_ANIMATION_IRONMAN, new boolean[] { true, true });
    subscribe(Runtime.getInstance().getName(), "registered", this.getName(), "onRegistered");
  }

  public void onRegistered(ServiceInterface s) {
    refreshControllers();
    broadcastState();
  }

  public List<String> refreshControllers() {
    controllers = Runtime.getServiceNamesFromInterface(NeoPixelController.class);
    return controllers;
  }

  // @Override
  public NeoPixelController getController() {
    return controller;
  }

  public String getControllerName() {
    String controlerName = null;
    if (controller != null) {
      controlerName = controller.getName();
    }
    return controlerName;
  }

  public boolean isAttached() {
    if (controller != null) {
      if (((Arduino) controller).getDeviceId((Attachable) this) != null) {
        isAttached = true;
        return true;
      }
      controller = null;
    }
    isAttached = false;
    return false;
  }

  public void setPixel(int address, int red, int green, int blue) {
    PixelColor pixel = new PixelColor(address, red, green, blue);
    setPixel(pixel);
  }

  public void setPixel(String address, String red, String green, String blue) {
    PixelColor pixel = new PixelColor(Integer.parseInt(address), Integer.parseInt(red), Integer.parseInt(green), Integer.parseInt(blue));
    setPixel(pixel);
  }

  public void setPixel(PixelColor pixel) {
    if (off)
      return;
    if (pixel.address <= getNumPixel()) {
      PixelColor pix = pixelMatrix.get(pixel.address);
      if (pix != null && !pix.isEqual(pixel)) {
        pixel.changed = true;
      } else {
        pixel.changed = false;
      }
      pixelMatrix.put(pixel.address, pixel);
    } else {
      log.info("Pixel address over the number of pixel");
    }
  }

  public void sendPixel(PixelColor pixel) {
    if (off)
      return;
    List<Integer> msg = new ArrayList<Integer>();
    msg.add(pixel.address);
    msg.add(pixel.red);
    msg.add(pixel.green);
    msg.add(pixel.blue);
    setPixel(pixel);
    controller.neoPixelWriteMatrix(this, msg);
    // savedPixelMatrix.clear();
    // savedPixelMatrix.add(pixel);
  }

  public void sendPixel(int address, int red, int green, int blue) {
    PixelColor pixel = new PixelColor(address, red, green, blue);
    sendPixel(pixel);
  }

  public void sendPixel(String address, String red, String green, String blue) {
    PixelColor pixel = new PixelColor(Integer.parseInt(address), Integer.parseInt(red), Integer.parseInt(green), Integer.parseInt(blue));
    sendPixel(pixel);
  }

  public void writeMatrix() {
    savedPixelMatrix.clear();
    Set<Entry<Integer, PixelColor>> set = pixelMatrix.entrySet();
    Iterator<Entry<Integer, PixelColor>> i = set.iterator();
    List<Integer> msg = new ArrayList<Integer>();
    while (i.hasNext()) {
      Map.Entry<Integer, PixelColor> me = (Map.Entry<Integer, PixelColor>) i.next();
      PixelColor pix = me.getValue();
      // will only send if the pixel value have changed
      if (pix.changed) {
        msg.add(pix.address);
        msg.add(pix.red);
        msg.add(pix.green);
        msg.add(pix.blue);
        pix.changed = false;
        me.setValue(pix);
      }
      savedPixelMatrix.add(me.getValue());
      if (msg.size() > 32) {
        if (!off && isAttached())
          controller.neoPixelWriteMatrix(this, msg);
        msg.clear();
      }
    }
    if (!off && isAttached())
      controller.neoPixelWriteMatrix(this, msg);
    broadcastState();
  }

  public Integer getPin() {
    return pin;
  }

  public int getNumPixel() {
    return numPixel;
  }

  public void turnOff() {
    for (int i = 1; i <= numPixel; i++) {
      PixelColor pixel = new PixelColor(i, 0, 0, 0);
      setPixel(pixel);
    }
    animationStop();
    writeMatrix();
    off = true;
  }

  public void turnOn() {
    off = false;
    broadcastState();
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(NeoPixel.class.getCanonicalName());
    meta.addDescription("Control a Neopixel hardware");
    meta.setAvailable(true); // false if you do not want it viewable in a    
    meta.addCategory("control","display");
    return meta;
  }

  public void attach(String controllerName, int pin, int numPixel) throws Exception {
    attach((NeoPixelController) Runtime.getService(controllerName), pin, numPixel);
  }

  public void attach(String controllerName, String pin, String numPixel) throws Exception {
    attach((NeoPixelController) Runtime.getService(controllerName), Integer.parseInt(pin), Integer.parseInt(numPixel));
  }

 
  @Override
  public void attach(NeoPixelController controller, int pin, int numPixel){
    if (controller == null) {
      error("setting null as controller");
      return;
    }
    if(isAttached) {
    	log.info("Neopixel already attached");
    	return;
    }
    
    this.pin = pin;
    this.numPixel = numPixel;

    // clear the old matrix
    pixelMatrix.clear();
    
    // create a new matrix
    for (int i = 1; i < numPixel + 1; i++) {
      setPixel(new PixelColor(i, 0, 0, 0));
    }

    controller.neoPixelAttach(this, pin, numPixel);
    
    log.info(String.format("%s setController %s", getName(), controller.getName()));
    this.controller = controller;
    controllerName = this.controller.getName();
    isAttached = true;
    // update gui with full pixels
    writeMatrix();
    broadcastState();
  }

  @Override
  public void detach(NeoPixelController controller) {
    // let the controller you want to detach this device
    if (controller != null) {
      controller.detach(this);
    }
    // setting controller reference to null
    this.controller = null;
    isAttached = false;
    refreshControllers();
    broadcastState();
  }

  public void refresh() {
    broadcastState();
  }

  public static void main(String[] args) throws InterruptedException {
    LoggingFactory.init(Level.INFO);

    try {
      //WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      // webgui.autoStartBrowser(false);
      //webgui.startService();
      Runtime.start("gui", "SwingGui");
      Runtime.start("python", "Python");
      Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
      arduino.arduinoPath = "C:\\Program Files (x86)\\Arduino";
      arduino.setBoardMega();
      arduino.connect("COM15");
      arduino.setDebug(true);
      Arduino arduino1 = (Arduino) Runtime.start("arduino1", "Arduino");
      // arduino1.setBoardUno();
      arduino1.connect(arduino, "Serial2");
      // //arduino.setDebug(true);
      NeoPixel neopixel = (NeoPixel) Runtime.start("neopixel", "NeoPixel");
      // webgui.startBrowser("http://localhost:8888/#/service/neopixel");
      neopixel.attach(arduino1, 8, 16);
      // sleep(50);
      PixelColor pix = new NeoPixel.PixelColor(1, 255, 255, 0);
      //neopixel.setPixel(pix);
      neopixel.sendPixel(pix);
      //neopixel.setAnimation(NEOPIXEL_ANIMATION_LARSON_SCANNER, 255, 0, 0, 1);
      //arduino.enableBoardStatus(true);
      //neopixel.setAnimation(NEOPIXEL_ANIMATION_LARSON_SCANNER, 0, 255, 0, 1);
      Servo servo = (Servo) Runtime.start("servo", "Servo");
      servo.attach(arduino, 5);
      servo.moveTo(180);
      sleep(2000);
      //neopixel.setAnimation(NEOPIXEL_ANIMATION_LARSON_SCANNER, 200, 0, 0, 1);
    } catch (Exception e) {
      Logging.logError(e);
    }


  }

  @Override
  public void setAnimation(int animation, int red, int green, int blue, int speed) {
    // protect against 0 and negative speed
    if (speed < 1)
      speed = 1;
    controller.neoPixelSetAnimation(this, animation, red, green, blue, speed);
    this.animation = animationIntToString(animation);
    broadcastState();
  }

  String animationIntToString(int animation) {
    switch (animation) {
    case NEOPIXEL_ANIMATION_NO_ANIMATION:
      return "No animation";
    case NEOPIXEL_ANIMATION_STOP:
      return "Stop";
    case NEOPIXEL_ANIMATION_COLOR_WIPE:
      return "Color Wipe";
    case NEOPIXEL_ANIMATION_LARSON_SCANNER:
      return "Larson Scanner";
    case NEOPIXEL_ANIMATION_THEATER_CHASE:
      return "Theater Chase";
    case NEOPIXEL_ANIMATION_THEATER_CHASE_RAINBOW:
      return "Theater Chase Rainbow";
    case NEOPIXEL_ANIMATION_RAINBOW:
      return "Rainbow";
    case NEOPIXEL_ANIMATION_RAINBOW_CYCLE:
      return "Rainbow Cycle";
    case NEOPIXEL_ANIMATION_FLASH_RANDOM:
      return "Flash Random";
    case NEOPIXEL_ANIMATION_IRONMAN:
      return "Ironman";
    default:
      log.error("Unknow Animation type {}", animation);
      return "No Animation";
    }
  }

  @Override
  public void setAnimation(String animation, int red, int green, int blue, int speed) {
    setAnimation(animationStringToInt(animation), red, green, blue, speed);
  }

  @Override
  public void setAnimation(String animation, String red, String green, String blue, String speed) {
    setAnimation(animationStringToInt(animation), Integer.parseInt(red), Integer.parseInt(green), Integer.parseInt(blue), Integer.parseInt(speed));
  }

  int animationStringToInt(String animation) {
    switch (animation) {
    case "No animation":
      return NEOPIXEL_ANIMATION_STOP;
    case "Stop":
      return NEOPIXEL_ANIMATION_STOP;
    case "Color Wipe":
      return NEOPIXEL_ANIMATION_COLOR_WIPE;
    case "Larson Scanner":
      return NEOPIXEL_ANIMATION_LARSON_SCANNER;
    case "Theater Chase":
      return NEOPIXEL_ANIMATION_THEATER_CHASE;
    case "Theater Chase Rainbow":
      return NEOPIXEL_ANIMATION_THEATER_CHASE_RAINBOW;
    case "Rainbow":
      return NEOPIXEL_ANIMATION_RAINBOW;
    case "Rainbow Cycle":
      return NEOPIXEL_ANIMATION_RAINBOW_CYCLE;
    case "Flash Random":
      return NEOPIXEL_ANIMATION_FLASH_RANDOM;
    case "Ironman":
      return NEOPIXEL_ANIMATION_IRONMAN;
    default:
      log.error("Unknow Animation type {}", animation);
      return NEOPIXEL_ANIMATION_STOP;
    }

  }

  @Override
  public void setAnimationSetting(String animation) {
    // TODO Auto-generated method stub
    animationSetting = animationSettings.get(animationStringToInt(animation));
    animationSettingColor = animationSetting[0];
    animationSettingSpeed = animationSetting[1];
    broadcastState();
  }

  public void animationStop() {
    setAnimation(NEOPIXEL_ANIMATION_STOP, 0, 0, 0, 0);
  }

  @Override
  public void detach(String controllerName) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean isAttached(String name) {
    return controller != null && name.equals(controller.getName());
  }

  @Override
  public Set<String> getAttached() {
    Set<String> ret = new HashSet<String>();
    if (controller != null){
      ret.add(controller.getName());
    }
    return ret;
  }
}